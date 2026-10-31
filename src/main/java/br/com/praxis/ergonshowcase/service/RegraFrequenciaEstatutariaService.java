package br.com.praxis.ergonshowcase.service;

import br.com.praxis.ergonshowcase.config.TenantContext;
import br.com.praxis.ergonshowcase.domain.RegraFrequenciaEstatutaria;
import br.com.praxis.ergonshowcase.dto.RegraFrequenciaEstatutariaFilterDTO;
import br.com.praxis.ergonshowcase.dto.RegraFrequenciaEstatutariaResponseDTO;
import br.com.praxis.ergonshowcase.mapper.RegraFrequenciaEstatutariaMapper;
import br.com.praxis.ergonshowcase.repository.RegraFrequenciaEstatutariaRepository;
import br.com.praxis.ergonshowcase.rules.ErgonRuleEngineFacade;
import br.com.praxis.ergonshowcase.rules.ErgonRuleViolationException;
import java.util.List;
import java.util.OptionalLong;
import org.praxisplatform.rules.contract.RuleEvaluationResult;
import org.praxisplatform.uischema.service.base.AbstractReadOnlyResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Serviço de catálogo e governança determinística de regras estatutárias de frequência
 * ({@code REGRAS_FREQ}), integrando a avaliação da {@code RN-013} via {@link ErgonRuleEngineFacade}.
 */
@Service
public class RegraFrequenciaEstatutariaService
    extends AbstractReadOnlyResourceService<
        RegraFrequenciaEstatutaria,
        RegraFrequenciaEstatutariaResponseDTO,
        Long,
        RegraFrequenciaEstatutariaFilterDTO> {
  private final RegraFrequenciaEstatutariaRepository regraRepository;
  private final RegraFrequenciaEstatutariaMapper mapper;
  private final ErgonRuleEngineFacade ruleEngineFacade;
  private final TenantContext tenantContext;

  public RegraFrequenciaEstatutariaService(
      RegraFrequenciaEstatutariaRepository repository, RegraFrequenciaEstatutariaMapper mapper) {
    this(repository, mapper, new ErgonRuleEngineFacade(), new TenantContext());
  }

  public RegraFrequenciaEstatutariaService(
      RegraFrequenciaEstatutariaRepository repository,
      RegraFrequenciaEstatutariaMapper mapper,
      ErgonRuleEngineFacade ruleEngineFacade) {
    this(repository, mapper, ruleEngineFacade, new TenantContext());
  }

  @Autowired
  public RegraFrequenciaEstatutariaService(
      RegraFrequenciaEstatutariaRepository repository,
      RegraFrequenciaEstatutariaMapper mapper,
      ErgonRuleEngineFacade ruleEngineFacade,
      TenantContext tenantContext) {
    super(repository, RegraFrequenciaEstatutaria.class);
    this.regraRepository = repository;
    this.mapper = mapper;
    this.ruleEngineFacade = ruleEngineFacade;
    this.tenantContext = tenantContext != null ? tenantContext : new TenantContext();
  }

  @Override
  protected RegraFrequenciaEstatutariaMapper getResourceMapper() {
    return mapper;
  }

  @Override
  protected OptionalLong getEntityResourceVersion(RegraFrequenciaEstatutaria entity) {
    return OptionalLong.of(entity.getVersion());
  }

  public ErgonRuleEngineFacade getRuleEngineFacade() {
    return ruleEngineFacade;
  }

  /**
   * Avalia deterministicamente o rascunho de {@link RegraFrequenciaEstatutaria} contra o plano
   * compilado da {@code RN-013} para o órgão correspondente, pré-carregando os registros do mesmo
   * escopo funcional para aferição de concomitância ({@code ERG-08393}).
   */
  public RuleEvaluationResult evaluateRn013(RegraFrequenciaEstatutaria draft) {
    List<RegraFrequenciaEstatutaria> existingCandidates = loadExistingScopeCandidates(draft);
    return ruleEngineFacade.evaluateRn013(draft, existingCandidates);
  }

  /**
   * Valida o rascunho de {@link RegraFrequenciaEstatutaria} contra o plano compilado da
   * {@code RN-013} em modo <i>fail-closed</i>, lançando {@link ErgonRuleViolationException}
   * (HTTP 422) caso qualquer slot resulte em {@code DENY}, {@code INCONCLUSIVE} ou
   * {@code TECHNICAL_ERROR}.
   */
  public RuleEvaluationResult validateRn013(RegraFrequenciaEstatutaria draft) {
    List<RegraFrequenciaEstatutaria> existingCandidates = loadExistingScopeCandidates(draft);
    return ruleEngineFacade.enforceRn013(draft, existingCandidates);
  }

  /**
   * Simula a avaliação determinística da {@code RN-013} sem lançar exceção e retorna o payload
   * estruturado de diagnóstico com códigos {@code ERG-*}, {@code CERG-*}, {@code slotKey}s e
   * origem {@code PRODUCT}/{@code CUSTOMER}.
   */
  public java.util.Map<String, Object> evaluateRn013Diagnostic(RegraFrequenciaEstatutaria draft) {
    Integer empCodigo = resolveEffectiveOrgao(draft != null ? draft.getEmpCodigo() : null);
    RuleEvaluationResult result = evaluateRn013(draft);
    return ruleEngineFacade.toDiagnosticPayload(
        ruleEngineFacade.resolveRn013Plan(empCodigo), result);
  }

  /**
   * Executa validação <i>fail-closed</i> da {@code RN-013} e retorna o payload estruturado de
   * confirmação quando {@code RuleDecision.ALLOW} (ou lança {@link ErgonRuleViolationException}
   * com RFC 7807 {@code ProblemDetail} em caso de violação).
   */
  public java.util.Map<String, Object> validateRn013Diagnostic(RegraFrequenciaEstatutaria draft) {
    Integer empCodigo = resolveEffectiveOrgao(draft != null ? draft.getEmpCodigo() : null);
    RuleEvaluationResult result = validateRn013(draft);
    return ruleEngineFacade.toDiagnosticPayload(
        ruleEngineFacade.resolveRn013Plan(empCodigo), result);
  }

  /** Retorna o resumo do plano compilado da {@code RN-013} e catálogo de erros para o órgão. */
  public java.util.Map<String, Object> getRn013PlanSummary(Integer empCodigo) {
    return ruleEngineFacade.toPlanSummaryPayload(resolveEffectiveOrgao(empCodigo));
  }

  private Integer resolveEffectiveOrgao(Integer explicitEmpCodigo) {
    return explicitEmpCodigo != null ? explicitEmpCodigo : tenantContext.orgao();
  }

  private List<RegraFrequenciaEstatutaria> loadExistingScopeCandidates(
      RegraFrequenciaEstatutaria draft) {
    if (draft == null || draft.getTipofreq() == null || draft.getCodfreq() == null) {
      return List.of();
    }
    if (draft.getEmpCodigo() != null && draft.getRegimeJuridico() != null) {
      List<RegraFrequenciaEstatutaria> scoped =
          regraRepository.findByEmpCodigoAndRegimeJuridicoAndTipofreqAndCodfreq(
              draft.getEmpCodigo(),
              draft.getRegimeJuridico(),
              draft.getTipofreq(),
              draft.getCodfreq());
      return scoped != null ? scoped : List.of();
    }
    List<RegraFrequenciaEstatutaria> generalCandidates =
        regraRepository.findByTipofreqAndCodfreq(draft.getTipofreq(), draft.getCodfreq());
    return generalCandidates != null ? generalCandidates : List.of();
  }
}
