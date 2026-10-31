package br.com.praxis.ergonshowcase.service;

import br.com.praxis.ergonshowcase.config.TenantContext;
import br.com.praxis.ergonshowcase.domain.*;
import br.com.praxis.ergonshowcase.dto.*;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Avaliações HOST não persistentes; não substituem o enforcement transacional. */
@Service
@Transactional(readOnly = true)
public class ErgonDeterminationService {
  @PersistenceContext private EntityManager em;
  private final TenantContext tenant;
  private final ErgonReadFacts facts;

  public ErgonDeterminationService(TenantContext tenant, ErgonReadFacts facts) {
    this.tenant = tenant;
    this.facts = facts;
  }

  public PcdPreviewDTO pcd(PcdDraftDTO draft) {
    return new PcdPreviewDTO(
        ErgonDerivedFacts.possuiDeficiencia(
            draft.deffisica(),
            draft.defvisual(),
            draft.defauditiva(),
            draft.defmental(),
            draft.defintelectual()));
  }

  private Cargo selected(Integer id) {
    Cargo cargo = em.find(Cargo.class, id);
    if (cargo == null || !java.util.Objects.equals(tenant.orgao(), cargo.getEmpCodigo()))
      ErgonInvariants.fail("Cargo inexistente ou de outro órgão.");
    return cargo;
  }

  public ProvimentoPreviewDTO provimento(CargoSelectionDTO draft) {
    if (draft.cargo() == null) return new ProvimentoPreviewDTO(null, null);
    Cargo cargo = selected(draft.cargo());
    var dto = new CargoResponseDTO();
    dto.setId(cargo.getCargo());
    dto.setVagasAutorizadas(cargo.getVagasAutorizadas());
    facts.enrich(List.of(dto));
    return new ProvimentoPreviewDTO(cargo.getNome(), dto.getSaldoVagasDisponiveis());
  }

  public AglutinadorPreviewDTO aglutinador(CargoSelectionDTO draft) {
    if (draft.cargo() == null) return new AglutinadorPreviewDTO(null);
    Cargo cargo = selected(draft.cargo());
    if (!Boolean.TRUE.equals(cargo.getEAglutinador()))
      ErgonInvariants.fail("Cargo selecionado não está habilitado como agregador.");
    return new AglutinadorPreviewDTO(cargo.getNome());
  }

  public DependenteElegibilidadePreviewDTO elegibilidadeDependente(
      DependenteElegibilidadeDraftDTO draft) {
    String descricao = null;
    boolean geraIr = true;
    int tetoBase = 21;
    if (draft.tipodepen() != null && !draft.tipodepen().isBlank()) {
      TipoDependente tipo = em.find(TipoDependente.class, draft.tipodepen());
      if (tipo == null) ErgonInvariants.fail("Modalidade de tipo de dependência inexistente.");
      descricao = tipo.getDescricao();
      geraIr = Boolean.TRUE.equals(tipo.getGeraIr());
      if (tipo.getIdadeLimite() != null) tetoBase = tipo.getIdadeLimite();
    }
    Integer idade =
        draft.dtnasc() == null
            ? null
            : Math.toIntExact(
                ChronoUnit.YEARS.between(draft.dtnasc(), LocalDate.now(ZoneOffset.UTC)));
    int tetoEfetivo = Boolean.TRUE.equals(draft.universitario()) ? Math.max(tetoBase, 24) : tetoBase;
    boolean dentroDoLimite =
        Boolean.TRUE.equals(draft.invalido()) || (idade != null && idade <= tetoEfetivo);
    return new DependenteElegibilidadePreviewDTO(descricao, idade, geraIr && dentroDoLimite);
  }

  public AfastamentoPreviaPreviewDTO previaAfastamento(AfastamentoPreviaDraftDTO draft) {
    String nomeModalidade = null;
    Boolean comOnus = null;
    if (draft.tipofreq() != null && !draft.tipofreq().isBlank() && draft.codfreq() != null) {
      CodigoFrequencia codigo =
          em.find(
              CodigoFrequencia.class,
              new CodigoFrequenciaId(draft.tipofreq().trim(), draft.codfreq()));
      if (codigo == null || !Boolean.TRUE.equals(codigo.getAfastamento())) {
        ErgonInvariants.fail(
            "Modalidade de frequência inexistente ou não habilitada para afastamento.");
      }
      nomeModalidade = codigo.getNome();
      comOnus = Boolean.TRUE.equals(codigo.getOnus());
    }
    Integer dias = null;
    if (draft.dtini() != null && draft.dtprevfim() != null) {
      if (draft.dtprevfim().isBefore(draft.dtini())) {
        ErgonInvariants.fail(
            "O término previsto do afastamento não pode anteceder a data de início.");
      }
      dias = Math.toIntExact(ChronoUnit.DAYS.between(draft.dtini(), draft.dtprevfim()) + 1);
    }
    return new AfastamentoPreviaPreviewDTO(nomeModalidade, dias, comOnus);
  }

  public FrequenciaPreviaPreviewDTO previaFrequencia(FrequenciaPreviaDraftDTO draft) {
    FrequenciaPreviaPreviewDTO preview = new FrequenciaPreviaPreviewDTO();
    if (draft.getTipofreq() != null
        && !draft.getTipofreq().isBlank()
        && draft.getCodfreq() != null) {
      CodigoFrequencia codigo =
          em.find(
              CodigoFrequencia.class,
              new CodigoFrequenciaId(draft.getTipofreq().trim(), draft.getCodfreq()));
      if (codigo == null || !Boolean.TRUE.equals(codigo.getViaFrequencia())) {
        ErgonInvariants.fail(
            "Código de frequência inexistente ou não habilitado para lançamento via frequência.");
      }
      preview.setCodigoFrequenciaNomePrevisto(codigo.getNome());
      preview.setFormatoQuantidadePrevisto(codigo.getFormatoQtd());
      preview.setExigeHorarioPrevisto(Boolean.TRUE.equals(codigo.getHoraEntradaSaida()));
    }
    if (draft.getDtini() != null
        && draft.getDtfim() != null
        && draft.getDtfim().isBefore(draft.getDtini())) {
      ErgonInvariants.fail(
          "A data final da ocorrência de frequência não pode anteceder a data de início.");
    }
    return preview;
  }

  public CampoCustomizadoPreviaPreviewDTO previaCampoCustomizado(
      CampoCustomizadoPreviaDraftDTO draft) {
    CampoCustomizadoPreviaPreviewDTO preview = new CampoCustomizadoPreviaPreviewDTO();
    preview.setEscopoGlobalPrevisto(draft.getEmpCodigo() == null);
    boolean requiresTabela =
        "SELECT".equalsIgnoreCase(draft.getFieldType())
            || "SELECT".equalsIgnoreCase(draft.getControlType())
            || "SEARCHABLE_SELECT".equalsIgnoreCase(draft.getControlType());
    preview.setRequerTabelaOpcoesPrevisto(requiresTabela);
    if (draft.getOptionTabela() != null && !draft.getOptionTabela().isBlank()) {
      Tabela tab = em.find(Tabela.class, draft.getOptionTabela().trim());
      if (tab == null) {
        ErgonInvariants.fail("Tabela geral de opções (HADES.TABELA) inexistente.");
      }
      preview.setOptionTabelaDescricaoPrevista(tab.getDescr());
    }
    return preview;
  }
}
