package br.com.praxis.ergonshowcase.service;

import br.com.praxis.ergonshowcase.config.TenantContext;
import br.com.praxis.ergonshowcase.domain.*;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.dto.ChefiaDeterminationDTO.*;
import br.com.praxis.ergonshowcase.rules.EffectiveVacationParameterResolver;
import br.com.praxis.ergonshowcase.rules.ErgonRuleEngineFacade;
import br.com.praxis.ergonshowcase.rules.OperationalRuleSet;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Avaliações HOST não persistentes; não substituem o enforcement transacional. */
@Service
@Transactional(readOnly = true)
public class ErgonDeterminationService {
  @PersistenceContext private EntityManager em;
  private final TenantContext tenant;
  private final ErgonReadFacts facts;
  private final ErgonRuleEngineFacade rules;
  private final EffectiveVacationParameterResolver vacationParameterResolver =
      new EffectiveVacationParameterResolver();

  public ErgonDeterminationService(TenantContext tenant, ErgonReadFacts facts) {
    this(tenant, facts, new ErgonRuleEngineFacade());
  }

  @org.springframework.beans.factory.annotation.Autowired
  public ErgonDeterminationService(TenantContext tenant, ErgonReadFacts facts, ErgonRuleEngineFacade rules) {
    this.tenant = tenant;
    this.facts = facts;
    this.rules = rules;
  }

  public FuncaoPreview previaFuncao(FuncaoDraft draft) {
    if (draft.dtini() != null && draft.dtfim() != null)
      rules.enforceFuncaoPreviewDates(tenant.orgao(), draft.dtini(), draft.dtfim());
    return new FuncaoPreview(diasInclusivos(draft.dtini(), draft.dtfim()));
  }

  public VagaPreview previaVaga(VagaDraft draft) {
    if (draft.funcaoId() == null) return new VagaPreview(null);
    var funcao = funcaoParaPrevia(draft.funcaoId());
    rules.enforceVagaFuncaoPreview(tenant.orgao(), funcao.getSituacao());
    return new VagaPreview(funcao.getControleVaga());
  }

  public DesignacaoPreview previaDesignacao(DesignacaoDraft draft) {
    if (draft.vagaFuncaoId() == null) return new DesignacaoPreview(null);
    var vaga = em.find(VagaFuncao.class, draft.vagaFuncaoId());
    exigirEscopoPrevia(vaga == null ? null : vaga.getEmpCodigo());
    return new DesignacaoPreview(vaga.getSituacao());
  }

  public SubstituicaoPreview previaSubstituicao(SubstituicaoDraft draft) {
    if (draft.dtini() != null && draft.dtfim() != null)
      rules.enforceSubstituicaoPreviewDates(tenant.orgao(), draft.dtini(), draft.dtfim());
    var dias = diasInclusivos(draft.dtini(), draft.dtfim());
    if (draft.designacaoId() == null) return new SubstituicaoPreview(dias, null);
    var designacao = em.find(DesignacaoFuncao.class, draft.designacaoId());
    exigirEscopoPrevia(designacao == null ? null : designacao.getEmpCodigo());
    var funcao = funcaoParaPrevia(designacao.getFuncaoId());
    rules.enforceSubstituicaoFuncaoPermitePreview(tenant.orgao(), funcao.getPermiteSubstituicao());
    if (draft.dtini() != null && draft.dtfim() != null) {
      rules.enforceSubstituicaoDesignacaoVigenciaPreview(
          tenant.orgao(), designacao, draft.dtini(), draft.dtfim());
    }
    return new SubstituicaoPreview(dias, Boolean.TRUE.equals(funcao.getPermiteSubstituicao())
        ? funcao.getDiasMinimosSubstituicao() : null);
  }

  private FuncaoConfianca funcaoParaPrevia(Long id) {
    var funcao = id == null ? null : em.find(FuncaoConfianca.class, id);
    exigirEscopoPrevia(funcao == null ? null : funcao.getEmpCodigo());
    return funcao;
  }

  private void exigirEscopoPrevia(Integer orgao) {
    if (orgao == null || !Objects.equals(tenant.orgao(), orgao)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Referência funcional indisponível no órgão.");
    }
  }

  private static Integer diasInclusivos(LocalDate inicio, LocalDate fim) {
    if (inicio == null || fim == null) return null;
    return Math.toIntExact(ChronoUnit.DAYS.between(inicio, fim) + 1);
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
    if (cargo == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cargo inexistente.");
    ErgonTenantAccess.requireReferencesInOrganization(tenant.orgao(), cargo);
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
    rules.enforceCargoAglutinadorPreview(tenant.orgao(), draft.cargo(), cargo.getEAglutinador());
    return new AglutinadorPreviewDTO(cargo.getNome());
  }

  public DependenteElegibilidadePreviewDTO elegibilidadeDependente(
      DependenteElegibilidadeDraftDTO draft) {
    String descricao = null;
    boolean geraIr = true;
    int tetoBase = 21;
    if (draft.tipodepen() != null && !draft.tipodepen().isBlank()) {
      TipoDependente tipo = em.find(TipoDependente.class, draft.tipodepen());
      if (tipo == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Modalidade de tipo de dependência inexistente.");
      descricao = tipo.getDescricao();
      geraIr = Boolean.TRUE.equals(tipo.getGeraIr());
      if (tipo.getIdadeLimite() != null) tetoBase = tipo.getIdadeLimite();
    }
    Integer idade = null;
    if (draft.dtnasc() != null) {
      LocalDate refDate = LocalDate.now(ZoneOffset.UTC);
      rules.enforceDependentePreviewBirthDate(tenant.orgao(), draft.dtnasc(), refDate);
      idade = Math.toIntExact(ChronoUnit.YEARS.between(draft.dtnasc(), refDate));
    }
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
      if (codigo == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Código de frequência inexistente.");
      rules.enforceCodigoAfastamentoPreview(tenant.orgao(), draft.tipofreq().trim(), draft.codfreq(), codigo.getAfastamento());
      nomeModalidade = codigo.getNome();
      comOnus = Boolean.TRUE.equals(codigo.getOnus());
    }
    Integer dias = null;
    if (draft.dtini() != null && draft.dtprevfim() != null) {
      rules.enforceAfastamentoPreviewDates(tenant.orgao(), draft.dtini(), draft.dtprevfim());
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
      if (codigo == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Código de frequência inexistente.");
      rules.enforceCodigoFrequenciaPreview(tenant.orgao(), draft.getTipofreq().trim(), draft.getCodfreq(), codigo.getViaFrequencia());
      preview.setCodigoFrequenciaNomePrevisto(codigo.getNome());
      preview.setFormatoQuantidadePrevisto(codigo.getFormatoQtd());
      preview.setExigeHorarioPrevisto(Boolean.TRUE.equals(codigo.getHoraEntradaSaida()));
    }
    if (draft.getDtini() != null && draft.getDtfim() != null) {
      rules.enforceFrequenciaPreviewDates(tenant.orgao(), draft.getDtini(), draft.getDtfim());
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
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tabela geral de opções (HADES.TABELA) inexistente.");
      }
      preview.setOptionTabelaDescricaoPrevista(tab.getDescr());
    }
    return preview;
  }

  public ParametroContagemFeriasPreviaPreviewDTO previaParametroContagemFerias(
      ParametroContagemFeriasPreviaDraftDTO draft) {
    if (Boolean.FALSE.equals(draft.getTemFerias())) {
      return new ParametroContagemFeriasPreviaPreviewDTO(0, 0);
    }
    int tot = draft.getTotDiasFer() != null ? draft.getTotDiasFer() : 30;
    int minPrinc = Math.max(10, Math.min(14, tot));
    if ("N".equalsIgnoreCase(draft.getAbonoDirGozo())) {
      return new ParametroContagemFeriasPreviaPreviewDTO(0, minPrinc);
    }
    int num = draft.getNumDiasAbonoFer() != null ? draft.getNumDiasAbonoFer() : 1;
    int den =
        draft.getDenDiasAbonoFer() != null && draft.getDenDiasAbonoFer() > 0
            ? draft.getDenDiasAbonoFer()
            : 3;
    rules.enforceOperational(OperationalRuleSet.PREVIA_FRACAO_ABONO, tenant.orgao(), num, den);
    int maxVend = (tot * num) / den;
    return new ParametroContagemFeriasPreviaPreviewDTO(maxVend, minPrinc);
  }

  public PeriodoAquisitivoPreviaPreviewDTO previaPeriodoAquisitivo(
      PeriodoAquisitivoPreviaDraftDTO draft) {
    if (draft.getEmpCodigo() != null && !Objects.equals(draft.getEmpCodigo(), tenant.orgao()))
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.FORBIDDEN, "Órgão do rascunho fora do escopo corrente.");
    if (draft.getDtini() != null && draft.getDtfim() != null) {
      rules.enforcePeriodoFeriasPreviewDates(tenant.orgao(), draft.getDtini(), draft.getDtfim());
    }
    Vinculo vinculo = null;
    if (em != null && draft.getNumfunc() != null && draft.getNumvinc() != null) {
      vinculo = em.find(Vinculo.class, new VinculoId(draft.getNumfunc(), draft.getNumvinc()));
      if (vinculo == null) throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.NOT_FOUND, "Vínculo selecionado não encontrado.");
      if (!Objects.equals(vinculo.getEmpCodigo(), tenant.orgao())) throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.FORBIDDEN, "Vínculo fora do órgão corrente.");
    }
    ParametroContagemFerias param =
        vacationParameterResolver.resolveActiveParameter(em, vinculo, draft.getDtini());
    LocalDate dtfimBase =
        draft.getDtfim() != null
            ? draft.getDtfim()
            : (draft.getDtini() != null ? draft.getDtini().plusYears(1).minusDays(1) : null);
    int suspensao =
        draft.getDiasAfastamentoSuspensivo() != null
            ? draft.getDiasAfastamentoSuspensivo()
            : querySuspensiveDaysInPeriod(
                draft.getNumfunc(), draft.getNumvinc(), draft.getDtini(), dtfimBase);
    LocalDate dtfimEfetivo =
        draft.getDtfim() != null
            ? draft.getDtfim()
            : (draft.getDtini() != null
                ? draft.getDtini().plusYears(1).minusDays(1).plusDays(suspensao)
                : null);
    int faltas =
        draft.getFaltasApuradas() != null
            ? draft.getFaltasApuradas()
            : queryUnjustifiedAbsencesInPeriod(
                draft.getNumfunc(), draft.getNumvinc(), draft.getDtini(), dtfimEfetivo);
    int baseDias =
        draft.getTotDiasDireito() != null
            ? draft.getTotDiasDireito()
            : (param != null && param.getTotDiasFer() != null ? param.getTotDiasFer() : 30);
    int direito =
        faltas > 0
            ? EffectiveVacationParameterResolver.computeRightDaysAfterAbsences(baseDias, faltas)
            : baseDias;
    if (suspensao > 180) {
      direito = 0;
    }
    int gozados = draft.getDiasGozados() != null ? draft.getDiasGozados() : 0;
    int vendidos = draft.getDiasVendidosAbono() != null ? draft.getDiasVendidosAbono() : 0;
    int dobro = draft.getDiasConvertidosDobro() != null ? draft.getDiasConvertidosDobro() : 0;
    int saldo = Math.max(0, direito - gozados - vendidos - dobro);
    LocalDate dtPrescricao = dtfimEfetivo != null ? dtfimEfetivo.plusYears(2) : null;
    return new PeriodoAquisitivoPreviaPreviewDTO(
        dtfimEfetivo, direito, saldo, dtPrescricao, faltas, suspensao);
  }

  private int queryUnjustifiedAbsencesInPeriod(
      Long numfunc, Integer numvinc, LocalDate iniPa, LocalDate fimPa) {
    if (em == null || numfunc == null || numvinc == null || iniPa == null || fimPa == null) {
      return 0;
    }
    var qFreq =
        em.createQuery(
            "select f from Frequencia f where f.empCodigo=:org and f.numfunc=:f and f.numvinc=:v and f.situacao='HOMOLOGADO'",
            Frequencia.class);
    if (qFreq == null) {
      return 0;
    }
    var p1 = qFreq.setParameter("f", numfunc);
    var p2 = p1 != null ? p1.setParameter("v", numvinc) : null;
    var p3 = p2 != null ? p2.setParameter("org", tenant.orgao()) : null;
    var freqs = p3 != null ? p3.getResultList() : List.<Frequencia>of();
    if (freqs == null) {
      return 0;
    }
    int faltas = 0;
    for (Frequencia f : freqs) {
      if (f == null || f.getDtini() == null || !Objects.equals(f.getEmpCodigo(), tenant.orgao())) continue;
      LocalDate fEnd = f.getDtfim() != null ? f.getDtfim() : f.getDtini();
      if (f.getDtini().isAfter(fimPa) || fEnd.isBefore(iniPa)) continue;
      boolean desconta =
          "FALTA".equalsIgnoreCase(f.getTipofreq())
              || (f.getCodigoFrequenciaEntidade() != null
                  && f.getCodigoFrequenciaEntidade().getQtdFaltas() != null
                  && f.getCodigoFrequenciaEntidade().getQtdFaltas().signum() > 0);
      if (desconta) {
        LocalDate overlapIni = f.getDtini().isBefore(iniPa) ? iniPa : f.getDtini();
        LocalDate overlapFim = fEnd.isAfter(fimPa) ? fimPa : fEnd;
        int qtd =
            f.getQuantidade() != null && f.getQuantidade().intValue() > 0
                ? f.getQuantidade().intValue()
                : Math.max(1, Math.toIntExact(ChronoUnit.DAYS.between(overlapIni, overlapFim) + 1));
        faltas += qtd;
      }
    }
    return faltas;
  }

  private int querySuspensiveDaysInPeriod(
      Long numfunc, Integer numvinc, LocalDate iniPa, LocalDate fimPa) {
    if (em == null || numfunc == null || numvinc == null || iniPa == null || fimPa == null) {
      return 0;
    }
    var qAfast =
        em.createQuery(
            "select a from Afastamento a where a.empCodigo=:org and a.numfunc=:f and a.numvinc=:v and a.situacao='HOMOLOGADO'",
            Afastamento.class);
    if (qAfast == null) {
      return 0;
    }
    var p1 = qAfast.setParameter("f", numfunc);
    var p2 = p1 != null ? p1.setParameter("v", numvinc) : null;
    var p3 = p2 != null ? p2.setParameter("org", tenant.orgao()) : null;
    var afasts = p3 != null ? p3.getResultList() : List.<Afastamento>of();
    if (afasts == null) {
      return 0;
    }
    int diasSuspensivos = 0;
    for (Afastamento a : afasts) {
      if (a == null || a.getDtini() == null || !Objects.equals(a.getEmpCodigo(), tenant.orgao())) continue;
      CodigoFrequencia cf = a.getCodigoFrequenciaEntidade();
      if (cf == null
          && a.getTipofreq() != null
          && !a.getTipofreq().isBlank()
          && a.getCodfreq() != null) {
        cf =
            em.find(
                CodigoFrequencia.class,
                new CodigoFrequenciaId(a.getTipofreq().trim(), a.getCodfreq()));
      }
      if (cf != null && Boolean.TRUE.equals(cf.getOnus())) {
        continue;
      }
      LocalDate aEnd =
          a.getDtfim() != null
              ? a.getDtfim()
              : (a.getDtprevfim() != null ? a.getDtprevfim() : fimPa);
      if (a.getDtini().isAfter(fimPa) || aEnd.isBefore(iniPa)) continue;
      LocalDate overlapIni = a.getDtini().isBefore(iniPa) ? iniPa : a.getDtini();
      LocalDate overlapFim = aEnd.isAfter(fimPa) ? fimPa : aEnd;
      diasSuspensivos +=
          Math.max(1, Math.toIntExact(ChronoUnit.DAYS.between(overlapIni, overlapFim) + 1));
    }
    return diasSuspensivos;
  }

  public FeriasGozoPreviaPreviewDTO previaFeriasGozo(FeriasGozoPreviaDraftDTO draft) {
    Integer chave = null;
    Integer totDiasPa = null;
    Integer ordemParcelaSugerida = 1;
    Integer diasAbonoSugeridos = draft.getDiasAbonoVendidos();
    LocalDate dtPagtoAbonoSugerida = null;
    PeriodoAquisitivoFerias pa = null;
    EffectiveVacationParameterResolver.EffectiveParametroFeriasFacts effective = null;
    int saldoAtualPa = 30;
    if (draft.getPeriodoAquisitivoId() != null) {
      pa = em != null ? em.find(PeriodoAquisitivoFerias.class, draft.getPeriodoAquisitivoId()) : null;
      if (pa == null) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Período aquisitivo selecionado não encontrado.");
      }
      if (!Objects.equals(pa.getEmpCodigo(), tenant.orgao())) throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.FORBIDDEN, "Período aquisitivo fora do órgão corrente.");
      chave = pa.getChave();
      totDiasPa = pa.getTotDiasDireito();
      int gozados = pa.getDiasGozados() != null ? pa.getDiasGozados() : 0;
      int vendidos = pa.getDiasVendidosAbono() != null ? pa.getDiasVendidosAbono() : 0;
      int dobro = pa.getDiasConvertidosDobro() != null ? pa.getDiasConvertidosDobro() : 0;
      saldoAtualPa =
          pa.getSaldoDiasDisponivel() != null
              ? pa.getSaldoDiasDisponivel()
              : Math.max(0, (totDiasPa != null ? totDiasPa : 30) - gozados - vendidos - dobro);
      effective =
          vacationParameterResolver.resolveAndMerge(
              em, pa, pa.getDtini() != null ? pa.getDtini() : draft.getDtini());
      if (em != null) {
        var q =
            em.createQuery(
                "select x from FeriasGozo x where x.empCodigo=:org and x.periodoAquisitivoId=:paId and x.situacao<>'CANCELADA'",
                FeriasGozo.class);
        if (q != null) {
          var r1 = q.setParameter("paId", pa.getId()).setParameter("org", tenant.orgao());
          var flushed = r1 != null ? r1.setFlushMode(FlushModeType.COMMIT) : null;
          var parcelas =
              (flushed != null ? flushed : r1) != null
                  ? (flushed != null ? flushed : r1).getResultList()
                  : null;
          if (parcelas != null) {
            ordemParcelaSugerida = parcelas.size() + 1;
          }
        }
      }
    }
    Integer dias = draft.getDiasGozo();
    LocalDate dtprevfim = draft.getDtprevfim();
    if (draft.getDtini() != null && dtprevfim != null) {
      dias = Math.toIntExact(ChronoUnit.DAYS.between(draft.getDtini(), dtprevfim) + 1);
      rules.enforceGozoFeriasPreviewDates(tenant.orgao(), draft.getDtini(), dtprevfim, dias);
    } else if (draft.getDtini() != null && dias != null && dias > 0) {
      dtprevfim = draft.getDtini().plusDays(dias - 1L);
    }
    if (Boolean.TRUE.equals(draft.getAbonoPecuniario())) {
      if (diasAbonoSugeridos == null || diasAbonoSugeridos <= 0) {
        diasAbonoSugeridos =
            effective != null
                ? EffectiveVacationParameterResolver.computeSuggestedAbonoDays(
                    effective, totDiasPa, dias)
                : ((totDiasPa != null ? totDiasPa : 30) / 3);
      }
      if (draft.getDtini() != null) {
        dtPagtoAbonoSugerida = draft.getDtini().minusDays(2);
      }
    } else if (diasAbonoSugeridos == null) {
      diasAbonoSugeridos = 0;
    }
    int debitGozo = dias != null ? dias : 0;
    int debitAbono =
        Boolean.TRUE.equals(draft.getAbonoPecuniario()) && diasAbonoSugeridos != null
            ? diasAbonoSugeridos
            : 0;
    Integer saldoPaAposParcela = Math.max(0, saldoAtualPa - debitGozo - debitAbono);
    return new FeriasGozoPreviaPreviewDTO(
        chave,
        totDiasPa,
        dias,
        dtprevfim,
        ordemParcelaSugerida,
        diasAbonoSugeridos,
        dtPagtoAbonoSugerida,
        saldoPaAposParcela);
  }
}
