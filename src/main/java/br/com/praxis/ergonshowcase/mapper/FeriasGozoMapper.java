package br.com.praxis.ergonshowcase.mapper;

import br.com.praxis.ergonshowcase.config.TenantContext;
import br.com.praxis.ergonshowcase.domain.FeriasGozo;
import br.com.praxis.ergonshowcase.domain.VinculoId;
import br.com.praxis.ergonshowcase.dto.*;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import org.praxisplatform.uischema.mapper.base.ResourceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FeriasGozoMapper
    implements ResourceMapper<
        FeriasGozo, FeriasGozoResponseDTO, CreateFeriasGozoDTO, UpdateFeriasGozoDTO, Long> {

  private final TenantContext tenant;

  public FeriasGozoMapper() {
    this(new TenantContext());
  }

  @Autowired
  public FeriasGozoMapper(TenantContext tenant) {
    this.tenant = tenant != null ? tenant : new TenantContext();
  }

  @Override
  public FeriasGozoResponseDTO toResponse(FeriasGozo entity) {
    var dto = new FeriasGozoResponseDTO();
    dto.setId(entity.getId());
    dto.setParcelaResumo(resumo(entity));
    dto.setEmpCodigo(entity.getEmpCodigo());
    dto.setOrgaoNome(entity.getOrgao() != null ? entity.getOrgao().getNome() : null);
    dto.setNumfunc(entity.getNumfunc());
    dto.setPessoaNome(entity.getPessoa() != null ? entity.getPessoa().getNome() : null);
    dto.setNumvinc(entity.getNumvinc());
    if (entity.getNumfunc() != null && entity.getNumvinc() != null) {
      dto.setVinculoId(new VinculoId(entity.getNumfunc(), entity.getNumvinc()));
    }
    dto.setVinculoMatricula(entity.getVinculo() != null ? entity.getVinculo().getMatric() : null);
    dto.setPeriodoAquisitivoId(entity.getPeriodoAquisitivoId());
    dto.setPeriodoAquisitivoResumo(
        entity.getPeriodoAquisitivo() != null
            ? PeriodoAquisitivoFeriasMapper.resumo(entity.getPeriodoAquisitivo())
            : entity.getChavePeraqfer() != null ? "PA #" + entity.getChavePeraqfer() : null);
    dto.setPeriodoAquisitivoExercicio(
        entity.getPeriodoAquisitivo() != null ? entity.getPeriodoAquisitivo().getExercicio() : null);
    dto.setChavePeraqfer(entity.getChavePeraqfer());
    dto.setOrdemParcela(entity.getOrdemParcela());
    dto.setDtini(entity.getDtini());
    dto.setDtprevfim(entity.getDtprevfim());
    dto.setDtfim(entity.getDtfim());
    dto.setDiasGozo(entity.getDiasGozo());
    dto.setTotDiasDireitoPa(entity.getTotDiasDireitoPa());
    dto.setAbonoPecuniario(entity.getAbonoPecuniario());
    dto.setIniFimAbono(entity.getIniFimAbono());
    dto.setDiasAbonoVendidos(entity.getDiasAbonoVendidos());
    dto.setDtVendaAbono(entity.getDtVendaAbono());
    dto.setDtPagtoAbono(entity.getDtPagtoAbono());
    dto.setAbonoConstitucional(entity.getAbonoConstitucional());
    dto.setAdiantamento13(entity.getAdiantamento13());
    dto.setAdiantamentoSalario(entity.getAdiantamentoSalario());
    dto.setEmprestimoFerias(entity.getEmprestimoFerias());
    dto.setMesAnoFolha(entity.getMesAnoFolha());
    dto.setNumFolha(entity.getNumFolha());
    dto.setSituacao(entity.getSituacao());
    dto.setDtInterrupcao(entity.getDtInterrupcao());
    dto.setMotivoInterrupcao(entity.getMotivoInterrupcao());
    dto.setNumfuncSubstituto(entity.getNumfuncSubstituto());
    dto.setSubstitutoNome(
        entity.getPessoaSubstituta() != null ? entity.getPessoaSubstituta().getNome() : null);
    dto.setNumvincSubstituto(entity.getNumvincSubstituto());
    dto.setDocumentoLegalId(entity.getDocumentoLegalId());
    dto.setDocumentoLegalNumero(
        entity.getDocumentoLegal() != null ? entity.getDocumentoLegal().getNumeroDocumento() : null);
    dto.setAtoPublicacaoId(entity.getAtoPublicacaoId());
    dto.setAtoPublicacaoNumero(
        entity.getAtoPublicacao() != null ? entity.getAtoPublicacao().getNumeroDocumento() : null);
    dto.setObservacoes(entity.getObservacoes());
    dto.setComAbonoVendido(
        Boolean.TRUE.equals(entity.getAbonoPecuniario())
            || (entity.getDiasAbonoVendidos() != null && entity.getDiasAbonoVendidos() > 0));
    dto.setComSubstitutoDesignado(
        entity.getNumfuncSubstituto() != null && entity.getNumvincSubstituto() != null);
    dto.setParcelaInterrompida(
        "INTERROMPIDA".equals(entity.getSituacao()) || entity.getDtInterrupcao() != null);
    dto.setCustomFields(
        entity.getCustomFields() != null
            ? new LinkedHashMap<>(entity.getCustomFields())
            : new LinkedHashMap<>());
    return dto;
  }

  @Override
  public FeriasGozo newEntity(CreateFeriasGozoDTO dto) {
    var entity = new FeriasGozo();
    apply(dto, entity);
    entity.setSituacao("PROGRAMADA");
    return entity;
  }

  @Override
  public void applyUpdate(FeriasGozo entity, UpdateFeriasGozoDTO dto) {
    Integer chaveAtual = entity.getChavePeraqfer();
    Integer direitoAtual = entity.getTotDiasDireitoPa();
    apply(dto, entity);
    if (dto.getChavePeraqfer() == null && chaveAtual != null) {
      entity.setChavePeraqfer(chaveAtual);
    }
    if (dto.getTotDiasDireitoPa() == null && direitoAtual != null) {
      entity.setTotDiasDireitoPa(direitoAtual);
    }
  }

  @Override
  public Long extractId(FeriasGozo entity) {
    return entity.getId();
  }

  private void apply(FeriasGozoDados dto, FeriasGozo entity) {
    entity.setEmpCodigo(dto.getEmpCodigo());
    if (entity.getEmpCodigo() == null) {
      entity.setEmpCodigo(tenant.orgao());
    }
    entity.setNumfunc(dto.getNumfunc());
    entity.setNumvinc(dto.getNumvinc());
    entity.setPeriodoAquisitivoId(dto.getPeriodoAquisitivoId());
    entity.setChavePeraqfer(dto.getChavePeraqfer());
    entity.setOrdemParcela(dto.getOrdemParcela());
    if (entity.getOrdemParcela() == null) {
      entity.setOrdemParcela(1);
    }
    entity.setDtini(dto.getDtini());
    entity.setDtprevfim(dto.getDtprevfim());
    entity.setDtfim(dto.getDtfim());
    entity.setDiasGozo(dto.getDiasGozo());
    if (entity.getDiasGozo() == null
        && dto.getDtini() != null
        && dto.getDtprevfim() != null
        && !dto.getDtprevfim().isBefore(dto.getDtini())) {
      entity.setDiasGozo(
          Math.toIntExact(ChronoUnit.DAYS.between(dto.getDtini(), dto.getDtprevfim()) + 1));
    }
    entity.setTotDiasDireitoPa(dto.getTotDiasDireitoPa());
    if (entity.getTotDiasDireitoPa() == null) {
      entity.setTotDiasDireitoPa(30);
    }
    entity.setAbonoPecuniario(dto.getAbonoPecuniario());
    entity.setAbonoPecuniario(Boolean.TRUE.equals(entity.getAbonoPecuniario()));
    entity.setIniFimAbono(dto.getIniFimAbono());
    entity.setIniFimAbono(normUpper(entity.getIniFimAbono()));
    entity.setDiasAbonoVendidos(dto.getDiasAbonoVendidos());
    if (entity.getDiasAbonoVendidos() == null) {
      entity.setDiasAbonoVendidos(0);
    }
    entity.setDtVendaAbono(dto.getDtVendaAbono());
    entity.setDtPagtoAbono(dto.getDtPagtoAbono());
    entity.setAbonoConstitucional(dto.getAbonoConstitucional());
    entity.setAbonoConstitucional(!Boolean.FALSE.equals(entity.getAbonoConstitucional()));
    entity.setAdiantamento13(dto.getAdiantamento13());
    entity.setAdiantamento13(Boolean.TRUE.equals(entity.getAdiantamento13()));
    entity.setAdiantamentoSalario(dto.getAdiantamentoSalario());
    entity.setAdiantamentoSalario(Boolean.TRUE.equals(entity.getAdiantamentoSalario()));
    entity.setEmprestimoFerias(dto.getEmprestimoFerias());
    entity.setEmprestimoFerias(Boolean.TRUE.equals(entity.getEmprestimoFerias()));
    entity.setMesAnoFolha(dto.getMesAnoFolha());
    entity.setNumFolha(dto.getNumFolha());
    entity.setDtInterrupcao(dto.getDtInterrupcao());
    entity.setMotivoInterrupcao(dto.getMotivoInterrupcao());
    entity.setMotivoInterrupcao(norm(entity.getMotivoInterrupcao()));
    entity.setNumfuncSubstituto(dto.getNumfuncSubstituto());
    entity.setNumvincSubstituto(dto.getNumvincSubstituto());
    entity.setDocumentoLegalId(dto.getDocumentoLegalId());
    entity.setAtoPublicacaoId(dto.getAtoPublicacaoId());
    entity.setObservacoes(dto.getObservacoes());
    entity.setObservacoes(norm(entity.getObservacoes()));
    entity.setCustomFields(dto.getCustomFields());
    entity.setCustomFields(
        entity.getCustomFields() != null
            ? new LinkedHashMap<>(entity.getCustomFields())
            : new LinkedHashMap<>());
  }

  private static String resumo(FeriasGozo e) {
    String ord = e.getOrdemParcela() != null ? e.getOrdemParcela() + "ª Parcela" : "Parcela";
    String pa = e.getChavePeraqfer() != null ? " (PA #" + e.getChavePeraqfer() + ")" : "";
    String ini = e.getDtini() != null ? e.getDtini().toString() : "?";
    String fim =
        e.getDtfim() != null
            ? e.getDtfim().toString()
            : e.getDtprevfim() != null ? e.getDtprevfim().toString() : "?";
    return ord + pa + ": " + ini + " a " + fim;
  }

  private static String norm(String s) {
    return s == null || s.isBlank() ? null : s.trim();
  }

  private static String normUpper(String s) {
    return s == null || s.isBlank() ? null : s.trim().toUpperCase();
  }
}
