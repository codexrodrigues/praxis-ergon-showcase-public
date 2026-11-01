package br.com.praxis.ergonshowcase.service;

import br.com.praxis.ergonshowcase.config.TenantContext;
import br.com.praxis.ergonshowcase.domain.ParametroContagemFerias;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.mapper.ParametroContagemFeriasMapper;
import br.com.praxis.ergonshowcase.repository.ParametroContagemFeriasRepository;
import br.com.praxis.ergonshowcase.rules.ErgonRuleEngineFacade;
import br.com.praxis.ergonshowcase.rules.ErgonRuleViolationException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import java.util.*;
import java.util.function.Consumer;
import org.praxisplatform.rules.contract.RuleEvaluationResult;
import org.praxisplatform.uischema.concurrency.ResourceRepresentationResult;
import org.praxisplatform.uischema.concurrency.ResourceVersionPreconditionException;
import org.praxisplatform.uischema.concurrency.ResourceVersionUpdatePrecondition;
import org.praxisplatform.uischema.customfields.CustomFieldSupport;
import org.praxisplatform.uischema.exporting.CollectionExportField;
import org.praxisplatform.uischema.exporting.CollectionExportFieldPresentation;
import org.praxisplatform.uischema.service.base.VersionedCreateUpdateResourceService;
import org.praxisplatform.uischema.stats.StatsFieldRegistry;
import org.praxisplatform.uischema.stats.StatsMetric;
import org.praxisplatform.uischema.stats.StatsSupportMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Operações transacionais, invariantes e ciclo de vida da matriz estatutária de parâmetros de
 * contagem de férias ({@code PARAMETROS_CONTA}).
 */
@Service
public class ParametroContagemFeriasService
    extends ErgonResourceService<
        ParametroContagemFerias,
        ParametroContagemFeriasResponseDTO,
        Long,
        ParametroContagemFeriasFilterDTO,
        CreateParametroContagemFeriasDTO,
        UpdateParametroContagemFeriasDTO>
    implements VersionedCreateUpdateResourceService<
        ParametroContagemFeriasResponseDTO,
        Long,
        ParametroContagemFeriasFilterDTO,
        CreateParametroContagemFeriasDTO,
        UpdateParametroContagemFeriasDTO> {

  @Autowired(required = false)
  private ErgonInvariants invariants;

  @PersistenceContext protected EntityManager entityManager;
  protected final ParametroContagemFeriasMapper mapper;
  private final TenantContext tenant;
  private final ErgonRuleEngineFacade ruleEngineFacade;

  public ParametroContagemFeriasService(
      ParametroContagemFeriasRepository repository,
      ParametroContagemFeriasMapper mapper,
      TenantContext tenant) {
    this(repository, mapper, tenant, new ErgonRuleEngineFacade());
  }

  @Autowired
  public ParametroContagemFeriasService(
      ParametroContagemFeriasRepository repository,
      ParametroContagemFeriasMapper mapper,
      TenantContext tenant,
      ErgonRuleEngineFacade ruleEngineFacade) {
    super(repository, ParametroContagemFerias.class, "id");
    this.mapper = mapper;
    this.tenant = tenant != null ? tenant : new TenantContext();
    this.ruleEngineFacade =
        ruleEngineFacade != null ? ruleEngineFacade : new ErgonRuleEngineFacade();
  }

  public ErgonRuleEngineFacade getRuleEngineFacade() {
    return ruleEngineFacade;
  }

  /**
   * Avalia deterministicamente o rascunho de {@link ParametroContagemFerias} contra o plano
   * compilado do órgão ({@code PCK_PARAMETROS_CONTA.MAIN_PRE}/{@code MAIN_POS}) sem lançar exceção.
   */
  public RuleEvaluationResult evaluateParametroContagemFerias(ParametroContagemFerias draft) {
    return ruleEngineFacade.evaluateParametroContagemFerias(
        buildParametroContagemFeriasRuleContext(draft));
  }

  /**
   * Valida o rascunho de {@link ParametroContagemFerias} em modo <i>fail-closed</i>, lançando
   * {@link ErgonRuleViolationException} (HTTP 422) caso qualquer slot resulte em {@code DENY},
   * {@code INCONCLUSIVE} ou {@code TECHNICAL_ERROR}.
   */
  public RuleEvaluationResult validateParametroContagemFerias(ParametroContagemFerias draft) {
    return ruleEngineFacade.enforceParametroContagemFerias(
        buildParametroContagemFeriasRuleContext(draft));
  }

  /** Retorna o diagnóstico estruturado da avaliação determinística de {@link ParametroContagemFerias}. */
  public Map<String, Object> evaluateParametroContagemFeriasDiagnostic(
      ParametroContagemFerias draft) {
    Integer empCodigo =
        draft != null && draft.getEmpCodigo() != null ? draft.getEmpCodigo() : tenant.orgao();
    RuleEvaluationResult result = evaluateParametroContagemFerias(draft);
    return ruleEngineFacade.toDiagnosticPayload(
        ruleEngineFacade.resolveParametroContagemFeriasPlan(empCodigo), result);
  }

  public ErgonRuleEngineFacade.ParametroContagemFeriasRuleContext
      buildParametroContagemFeriasRuleContext(ParametroContagemFerias draft) {
    if (draft == null) {
      return new ErgonRuleEngineFacade.ParametroContagemFeriasRuleContext(null, List.of());
    }
    return new ErgonRuleEngineFacade.ParametroContagemFeriasRuleContext(
        draft, loadExistingParametroRules(draft));
  }

  private List<ParametroContagemFerias> loadExistingParametroRules(ParametroContagemFerias draft) {
    if (entityManager == null || draft == null || draft.getRegimeJuridico() == null) {
      return List.of();
    }
    var q =
        entityManager.createQuery(
            "select x from ParametroContagemFerias x where x.active=true and x.regimeJuridico=:rj and x.tipoVinculo=:tv and x.categoria=:cat",
            ParametroContagemFerias.class);
    if (q == null) {
      return List.of();
    }
    var r1 = q.setParameter("rj", draft.getRegimeJuridico());
    var r2 = r1 != null ? r1.setParameter("tv", draft.getTipoVinculo()) : null;
    var r3 = r2 != null ? r2.setParameter("cat", draft.getCategoria()) : null;
    var flushed = r3 != null ? r3.setFlushMode(FlushModeType.COMMIT) : null;
    var list =
        (flushed != null ? flushed : r3) != null
            ? (flushed != null ? flushed : r3).getResultList()
            : List.<ParametroContagemFerias>of();
    return list != null ? list : List.of();
  }

  @Override
  protected List<CollectionExportField> exportFields() {
    var datePresentation =
        new CollectionExportFieldPresentation(
            "date", "dd/MM/yyyy", null, "pt-BR", "America/Sao_Paulo", null, null, "Não informado");
    var boolPresentation =
        new CollectionExportFieldPresentation(
            "boolean", null, null, "pt-BR", null, "Sim", "Não", "Não informado");
    var intPresentation =
        new CollectionExportFieldPresentation(
            "number", "0", null, "pt-BR", null, null, null, "Não informado");
    return List.of(
        field("id", "ID da regra", "integer"),
        field("empCodigo", "Órgão", "integer"),
        field("orgaoNome", "Nome do órgão", "string"),
        field("regimeJuridico", "Regime jurídico", "string"),
        field("tipoVinculo", "Tipo de vínculo", "string"),
        field("categoria", "Categoria funcional", "string"),
        field("subcategoria", "Subcategoria", "string"),
        presentedField("dtini", "Início da vigência", "date", datePresentation),
        presentedField("dtfim", "Fim da vigência", "date", datePresentation),
        presentedField("temFerias", "Possui férias", "boolean", boolPresentation),
        presentedField("totDiasFer", "Dias de direito", "integer", intPresentation),
        presentedField("maxPerFer", "Máximo de parcelas", "integer", intPresentation),
        presentedField("maxDiasVendFer", "Máximo dias abono", "integer", intPresentation),
        presentedField("active", "Ativo", "boolean", boolPresentation));
  }

  @Override
  protected ParametroContagemFeriasMapper getResourceMapper() {
    return mapper;
  }

  @Override
  protected ParametroContagemFeriasFilterDTO emptyFilter() {
    return new ParametroContagemFeriasFilterDTO();
  }

  @Override
  protected OptionalLong getEntityResourceVersion(ParametroContagemFerias entity) {
    return OptionalLong.of(entity.getVersion());
  }

  @Override
  public Optional<String> getDatasetVersion() {
    return Optional.empty();
  }

  @Override
  protected org.praxisplatform.uischema.service.base.ResourceFilterAccessScope<
          ParametroContagemFerias>
      resolveResourceFilterAccessScope() {
    var explicit = tenant.explicitOrgao();
    if (explicit.isEmpty()) {
      return org.praxisplatform.uischema.service.base.ResourceFilterAccessScope.unrestricted();
    }
    int orgao = explicit.getAsInt();
    return org.praxisplatform.uischema.service.base.ResourceFilterAccessScope.restricted(
        (root, query, cb) ->
            cb.or(cb.isNull(root.get("empCodigo")), cb.equal(root.get("empCodigo"), orgao)));
  }

  @Override
  public StatsFieldRegistry getStatsFieldRegistry() {
    return StatsFieldRegistry.builder()
        .labeledGroupByBucket(
            "id", "id", "regimeJuridico", Set.of(StatsMetric.COUNT, StatsMetric.SUM))
        .categoricalGroupByBucket("regimeJuridico", "regimeJuridico")
        .categoricalGroupByBucket("tipoVinculo", "tipoVinculo")
        .categoricalGroupByBucket("categoria", "categoria")
        .categoricalGroupByBucket("active", "active")
        .temporalTimeSeriesField("dtini", "dtini")
        .numericHistogramMeasureField("totDiasFer", "totDiasFer")
        .numericHistogramMeasureField("maxDiasVendFer", "maxDiasVendFer")
        .build();
  }

  @Override
  public StatsSupportMode getGroupByStatsSupportMode() {
    return StatsSupportMode.AUTO;
  }

  @Override
  public StatsSupportMode getTimeSeriesStatsSupportMode() {
    return StatsSupportMode.AUTO;
  }

  @Override
  public StatsSupportMode getDistributionStatsSupportMode() {
    return StatsSupportMode.AUTO;
  }

  @Override
  public StatsSupportMode getComparisonStatsSupportMode() {
    return StatsSupportMode.AUTO;
  }

  @Override
  protected void beforeCreate(
      CreateParametroContagemFeriasDTO dto, ParametroContagemFerias entity) {
    checkOrg(entity.getEmpCodigo());
    validateParametroContagemFerias(entity);
    if (invariants != null) {
      invariants.parametroContagemFerias(entity, true);
    }
  }

  @Override
  protected void beforeUpdate(
      Long id, ParametroContagemFerias entity, UpdateParametroContagemFeriasDTO dto) {
    checkOrg(entity.getEmpCodigo());
  }

  @Override
  @Transactional
  public ResourceRepresentationResult<ParametroContagemFeriasResponseDTO> update(
      Long id,
      UpdateParametroContagemFeriasDTO dto,
      ResourceVersionUpdatePrecondition<Long> precondition) {
    if (precondition == null) throw ResourceVersionPreconditionException.required();
    var readOnlyKeys = validateCustomFields(dto, "request");
    ParametroContagemFerias entity = findEntityById(id);
    entityManager.lock(entity, LockModeType.PESSIMISTIC_WRITE);
    entityManager.refresh(entity);
    precondition.requireMatch(entity.getVersion());
    beforeUpdate(id, entity, dto);
    var old = CustomFieldSupport.snapshotReadOnlyValues(entity, readOnlyKeys);
    mapper.applyUpdate(entity, dto);
    validateParametroContagemFerias(entity);
    if (invariants != null) {
      invariants.parametroContagemFerias(entity, false);
    }
    CustomFieldSupport.restoreReadOnlyValues(entity, readOnlyKeys, old);
    var saved = getRepository().save(entity);
    afterUpdate(id, saved, dto);
    entityManager.flush();
    entityManager.refresh(saved);
    return readFacts != null
        ? enriched(toResourceRepresentation(saved))
        : toResourceRepresentation(saved);
  }

  private ResourceRepresentationResult<ParametroContagemFeriasResponseDTO> command(
      Long id,
      ResourceVersionUpdatePrecondition<Long> precondition,
      Consumer<ParametroContagemFerias> action) {
    if (precondition == null) throw ResourceVersionPreconditionException.required();
    ParametroContagemFerias entity = findEntityById(id);
    entityManager.lock(entity, LockModeType.PESSIMISTIC_WRITE);
    entityManager.refresh(entity);
    precondition.requireMatch(entity.getVersion());
    checkOrg(entity.getEmpCodigo());
    action.accept(entity);
    validateParametroContagemFerias(entity);
    if (invariants != null) {
      invariants.parametroContagemFerias(entity, false);
    }
    var saved = getRepository().save(entity);
    entityManager.flush();
    entityManager.refresh(saved);
    return readFacts != null
        ? enriched(toResourceRepresentation(saved))
        : toResourceRepresentation(saved);
  }

  @Transactional
  public ResourceRepresentationResult<ParametroContagemFeriasResponseDTO>
      atualizarParcelamentoAbono(
          Long id,
          AtualizarParcelamentoAbonoParametroFeriasDTO dto,
          ResourceVersionUpdatePrecondition<Long> precondition) {
    return command(
        id,
        precondition,
        (ParametroContagemFerias p) -> {
          p.setTemFerias(!Boolean.FALSE.equals(dto.getTemFerias()));
          p.setTotDiasFer(dto.getTotDiasFer());
          p.setMinPerFer(dto.getMinPerFer());
          p.setMaxPerFer(dto.getMaxPerFer());
          p.setMinDiasPerFer(dto.getMinDiasPerFer());
          p.setMaxDiasPerFer(dto.getMaxDiasPerFer());
          p.setMinDiasPerPrincFer(dto.getMinDiasPerPrincFer());
          p.setAbonoDirGozo(dto.getAbonoDirGozo());
          p.setNumDiasAbonoFer(dto.getNumDiasAbonoFer());
          p.setDenDiasAbonoFer(dto.getDenDiasAbonoFer());
          p.setMinDiasPvendFer(dto.getMinDiasPvendFer());
          p.setMaxDiasVendFer(dto.getMaxDiasVendFer());
          p.setAbonoConstFer(!Boolean.FALSE.equals(dto.getAbonoConstFer()));
          p.setAdiant13Fer(!Boolean.FALSE.equals(dto.getAdiant13Fer()));
          p.setAdiantSalFer(Boolean.TRUE.equals(dto.getAdiantSalFer()));
          p.setEmprestFer(Boolean.TRUE.equals(dto.getEmprestFer()));
        });
  }

  @Transactional
  public ResourceRepresentationResult<ParametroContagemFeriasResponseDTO>
      atualizarLicencaProgressao(
          Long id,
          AtualizarLicencaProgressaoParametroFeriasDTO dto,
          ResourceVersionUpdatePrecondition<Long> precondition) {
    return command(
        id,
        precondition,
        (ParametroContagemFerias p) -> {
          if (dto.getCriterioPrescricao() != null) {
            p.setCriterioPrescricao(dto.getCriterioPrescricao());
          }
          p.setPermiteGozoAposPrescricao(Boolean.TRUE.equals(dto.getPermiteGozoAposPrescricao()));
          p.setContDobroFerias(Boolean.TRUE.equals(dto.getContDobroFerias()));
          p.setMinDiasPeraqfer(dto.getMinDiasPeraqfer());
          p.setMaxDiasPeraqfer(dto.getMaxDiasPeraqfer());
          p.setTemLicEsp(Boolean.TRUE.equals(dto.getTemLicEsp()));
          p.setAnosLicEsp(dto.getAnosLicEsp());
          p.setMesesLicEspPer(dto.getMesesLicEspPer());
          p.setTemProgressao(Boolean.TRUE.equals(dto.getTemProgressao()));
          p.setAnosProgressao(dto.getAnosProgressao());
          p.setDocumentoLegalFeriasId(dto.getDocumentoLegalFeriasId());
          p.setDocumentoLegalProgressaoId(dto.getDocumentoLegalProgressaoId());
          p.setObservacoes(dto.getObservacoes());
        });
  }

  @Transactional
  public ResourceRepresentationResult<ParametroContagemFeriasResponseDTO> ativarParametro(
      Long id,
      AlterarVigenciaParametroFeriasDTO dto,
      ResourceVersionUpdatePrecondition<Long> precondition) {
    return command(
        id,
        precondition,
        (ParametroContagemFerias p) -> {
          if (Boolean.TRUE.equals(p.getActive())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "A matriz de parâmetros de contagem de férias já se encontra ativa.");
          }
          p.setActive(true);
          p.setDtfim(dto.getDtfim());
          if (dto.getDocumentoLegalFeriasId() != null) {
            p.setDocumentoLegalFeriasId(dto.getDocumentoLegalFeriasId());
          }
          p.setObservacoes(dto.getObservacoes());
        });
  }

  @Transactional
  public ResourceRepresentationResult<ParametroContagemFeriasResponseDTO> inativarParametro(
      Long id,
      AlterarVigenciaParametroFeriasDTO dto,
      ResourceVersionUpdatePrecondition<Long> precondition) {
    return command(
        id,
        precondition,
        (ParametroContagemFerias p) -> {
          if (!Boolean.TRUE.equals(p.getActive())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "A matriz de parâmetros de contagem de férias já se encontra inativa.");
          }
          p.setActive(false);
          if (dto.getDtfim() != null) {
            p.setDtfim(dto.getDtfim());
          }
          if (dto.getDocumentoLegalFeriasId() != null) {
            p.setDocumentoLegalFeriasId(dto.getDocumentoLegalFeriasId());
          }
          p.setObservacoes(dto.getObservacoes());
        });
  }

  @Transactional
  public ResourceRepresentationResult<ParametroContagemFeriasResponseDTO> encerrarVigenciaParametro(
      Long id,
      AlterarVigenciaParametroFeriasDTO dto,
      ResourceVersionUpdatePrecondition<Long> precondition) {
    return command(
        id,
        precondition,
        (ParametroContagemFerias p) -> {
          if (dto.getDtfim() == null) {
            throw new ResponseStatusException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "O encerramento de vigência exige informar a data final (dtfim).");
          }
          p.setDtfim(dto.getDtfim());
          if (dto.getDocumentoLegalFeriasId() != null) {
            p.setDocumentoLegalFeriasId(dto.getDocumentoLegalFeriasId());
          }
          p.setObservacoes(dto.getObservacoes());
        });
  }

  private void checkOrg(Integer orgao) {
    if (orgao != null && !Objects.equals(orgao, tenant.orgao())) {
      throw new ResponseStatusException(
          HttpStatus.UNPROCESSABLE_ENTITY,
          "Órgão da matriz de parâmetros de férias diverge de X-Tenant.");
    }
  }
}
