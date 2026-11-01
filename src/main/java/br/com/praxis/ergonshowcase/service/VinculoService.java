package br.com.praxis.ergonshowcase.service;

import br.com.praxis.ergonshowcase.config.TenantContext;
import br.com.praxis.ergonshowcase.domain.*;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.mapper.*;
import br.com.praxis.ergonshowcase.repository.*;
import br.com.praxis.ergonshowcase.rules.ErgonRuleEngineFacade;
import jakarta.persistence.*;
import java.util.*;
import org.praxisplatform.rules.contract.RuleEvaluationResult;
import org.praxisplatform.uischema.concurrency.*;
import org.praxisplatform.uischema.customfields.CustomFieldSupport;
import org.praxisplatform.uischema.service.base.*;
import org.praxisplatform.uischema.stats.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Operações do cadastro com pré-condição de versão e preservação da identidade carregada. */
@Service
public class VinculoService
    extends ErgonResourceService<
        Vinculo,
        VinculoResponseDTO,
        VinculoId,
        VinculoFilterDTO,
        CreateVinculoDTO,
        UpdateVinculoDTO>
    implements VersionedCreateUpdateResourceService<
        VinculoResponseDTO, VinculoId, VinculoFilterDTO, CreateVinculoDTO, UpdateVinculoDTO> {
  @Autowired(required = false)
  private ErgonInvariants invariants;

  @PersistenceContext protected EntityManager entityManager;
  protected final VinculoMapper mapper;
  private final TenantContext tenant;
  private final ErgonRuleEngineFacade ruleEngineFacade;

  public VinculoService(VinculoRepository repository, VinculoMapper mapper, TenantContext tenant) {
    this(repository, mapper, tenant, new ErgonRuleEngineFacade());
  }

  @Autowired
  public VinculoService(
      VinculoRepository repository,
      VinculoMapper mapper,
      TenantContext tenant,
      ErgonRuleEngineFacade ruleEngineFacade) {
    super(repository, Vinculo.class, "id.numfunc", "id.numero");
    this.mapper = mapper;
    this.tenant = tenant != null ? tenant : new TenantContext();
    this.ruleEngineFacade =
        ruleEngineFacade != null ? ruleEngineFacade : new ErgonRuleEngineFacade();
  }

  public ErgonRuleEngineFacade getRuleEngineFacade() {
    return ruleEngineFacade;
  }

  /**
   * Avalia deterministicamente o rascunho de {@link Vinculo} contra o
   * {@link br.com.praxis.ergonshowcase.rules.VinculoProvimentoRuleSet} do órgão.
   */
  public RuleEvaluationResult evaluateVinculoProvimento(Vinculo draft, boolean creating) {
    ErgonRuleEngineFacade.VinculoRuleContext ctx = buildVinculoRuleContext(draft, creating);
    return ruleEngineFacade.evaluateVinculo(ctx);
  }

  /**
   * Valida o rascunho de {@link Vinculo} em modo <i>fail-closed</i>, lançando
   * {@link br.com.praxis.ergonshowcase.rules.ErgonRuleViolationException} (HTTP 422) se negado.
   */
  public RuleEvaluationResult validateVinculoProvimento(Vinculo draft, boolean creating) {
    ErgonRuleEngineFacade.VinculoRuleContext ctx = buildVinculoRuleContext(draft, creating);
    return ruleEngineFacade.enforceVinculo(ctx);
  }

  /** Retorna o diagnóstico estruturado da avaliação determinística de provimento de vínculo. */
  public Map<String, Object> evaluateVinculoDiagnostic(Vinculo draft, boolean creating) {
    Integer empCodigo =
        draft != null && draft.getEmpCodigo() != null ? draft.getEmpCodigo() : tenant.orgao();
    RuleEvaluationResult result = evaluateVinculoProvimento(draft, creating);
    return ruleEngineFacade.toDiagnosticPayload(
        ruleEngineFacade.resolveVinculoPlan(empCodigo), result);
  }

  /** Constrói o contexto completo de avaliação determinística de {@link Vinculo}. */
  public ErgonRuleEngineFacade.VinculoRuleContext buildVinculoRuleContext(
      Vinculo draft, boolean creating) {
    if (draft == null) {
      return new ErgonRuleEngineFacade.VinculoRuleContext(
          null, null, null, null, 0L, null, 0L, null, null, null, List.of(), 0L, creating);
    }
    Orgao orgao = draft.getOrgao();
    if (draft.getEmpCodigo() == null) {
      orgao = null;
    } else if (orgao != null && !Objects.equals(orgao.getEmpresa(), draft.getEmpCodigo())) {
      orgao = null;
    }
    if (orgao == null && entityManager != null && draft.getEmpCodigo() != null) {
      orgao = entityManager.find(Orgao.class, draft.getEmpCodigo());
    }
    ServidorPessoa pessoa = draft.getPessoa();
    if (draft.getId() == null || draft.getId().numfunc() == null) {
      pessoa = null;
    } else if (pessoa != null && !Objects.equals(pessoa.getNumero(), draft.getId().numfunc())) {
      pessoa = null;
    }
    if (pessoa == null
        && entityManager != null
        && draft.getId() != null
        && draft.getId().numfunc() != null) {
      pessoa = entityManager.find(ServidorPessoa.class, draft.getId().numfunc());
    }
    Cargo cargo = draft.getCargoEntidade();
    if (draft.getCargo() == null) {
      cargo = null;
    } else if (cargo != null && !Objects.equals(cargo.getCargo(), draft.getCargo())) {
      cargo = null;
    }
    if (cargo == null && entityManager != null && draft.getCargo() != null) {
      cargo = entityManager.find(Cargo.class, draft.getCargo());
    }
    Cargo cargoComissao = null;
    if (entityManager != null && draft.getCargoComissao() != null) {
      cargoComissao = entityManager.find(Cargo.class, draft.getCargoComissao());
    }
    Setor lotacao = draft.getLotacao();
    if (draft.getSetor() == null || draft.getSetor().isBlank()) {
      lotacao = null;
    } else if (lotacao != null && !Objects.equals(lotacao.getSetor(), draft.getSetor())) {
      lotacao = null;
    }
    if (lotacao == null
        && entityManager != null
        && draft.getSetor() != null
        && !draft.getSetor().isBlank()) {
      lotacao = entityManager.find(Setor.class, draft.getSetor());
    }
    Setor exercicio = draft.getExercicio();
    if (draft.getSetorExercicio() == null || draft.getSetorExercicio().isBlank()) {
      exercicio = null;
    } else if (exercicio != null
        && !Objects.equals(exercicio.getSetor(), draft.getSetorExercicio())) {
      exercicio = null;
    }
    if (exercicio == null
        && entityManager != null
        && draft.getSetorExercicio() != null
        && !draft.getSetorExercicio().isBlank()) {
      exercicio = entityManager.find(Setor.class, draft.getSetorExercicio());
    }
    DocumentoLegal doc = null;
    if (entityManager != null && draft.getDocumentoLegalId() != null) {
      doc = entityManager.find(DocumentoLegal.class, draft.getDocumentoLegalId());
    }
    long cargoOcupadas =
        cargo != null && invariants != null
            ? invariants.ocupadas(cargo.getCargo(), draft.getId())
            : 0L;
    long comissaoOcupadas =
        cargoComissao != null && invariants != null
            ? invariants.ocupadas(cargoComissao.getCargo(), draft.getId())
            : 0L;
    List<Vinculo> existingVinculos = List.of();
    int openAfastamentosCount = 0;
    if (entityManager != null && draft.getId() != null && draft.getId().numfunc() != null) {
      var qv =
          entityManager.createQuery(
              "select v from Vinculo v where v.id.numfunc=:p and (:n is null or v.id.numero<>:n) and v.situacao='ATIVO'",
              Vinculo.class);
      if (qv != null) {
        existingVinculos =
            qv.setParameter("p", draft.getId().numfunc())
                .setParameter("n", draft.getId().numero())
                .setFlushMode(FlushModeType.COMMIT)
                .getResultList();
      }
      if (draft.getId().numero() != null) {
        var qa =
            entityManager.createQuery(
                "select count(a) from Afastamento a where a.numfunc=:p and a.numvinc=:v and a.situacao='HOMOLOGADO' and a.dtfim is null",
                Long.class);
        if (qa != null) {
          Long cnt =
              qa.setParameter("p", draft.getId().numfunc())
                  .setParameter("v", draft.getId().numero())
                  .setFlushMode(FlushModeType.COMMIT)
                  .getSingleResult();
          openAfastamentosCount = cnt != null ? cnt.intValue() : 0;
        }
      }
    }
    return new ErgonRuleEngineFacade.VinculoRuleContext(
        draft,
        orgao,
        pessoa,
        cargo,
        cargoOcupadas,
        cargoComissao,
        comissaoOcupadas,
        lotacao,
        exercicio,
        doc,
        existingVinculos,
        openAfastamentosCount,
        creating);
  }

  @Override
  protected List<org.praxisplatform.uischema.exporting.CollectionExportField> exportFields() {
    return List.of(
        field("id", "Vínculo", "string"),
        field("empCodigo", "Órgão", "integer"),
        field("situacao", "Situação", "string"),
        presentedField(
            "dtexerc",
            "Exercício",
            "date",
            new org.praxisplatform.uischema.exporting.CollectionExportFieldPresentation(
                "date", "dd/MM/yyyy", null, "pt-BR", "America/Sao_Paulo", null, null, "Não informado")),
        presentedField(
            "jornadaSemanal",
            "Jornada semanal",
            "integer",
            new org.praxisplatform.uischema.exporting.CollectionExportFieldPresentation(
                "number", "0", null, "pt-BR", null, null, null, "Não informado")),
        field("customFields.regime_teletrabalho", "Regime de teletrabalho", "string"));
  }

  @Override
  protected VinculoMapper getResourceMapper() {
    return mapper;
  }

  @Override
  protected VinculoFilterDTO emptyFilter() {
    return new VinculoFilterDTO();
  }

  @Override
  protected OptionalLong getEntityResourceVersion(Vinculo e) {
    return OptionalLong.of(e.getVersion());
  }

  @Override
  public Optional<String> getDatasetVersion() {
    return Optional.empty();
  }

  @Override
  public StatsFieldRegistry getStatsFieldRegistry() {
    return StatsFieldRegistry.builder()
        .labeledGroupByBucket(
            "id",
            "id",
            "matric",
            Set.of(
                StatsMetric.COUNT,
                StatsMetric.SUM,
                StatsMetric.AVG,
                StatsMetric.MIN,
                StatsMetric.MAX,
                StatsMetric.MEDIAN,
                StatsMetric.PERCENTILE_90,
                StatsMetric.PERCENTILE_95))
        .groupByBucket(
            "situacao",
            "situacao",
            Set.of(
                StatsMetric.COUNT,
                StatsMetric.SUM,
                StatsMetric.AVG,
                StatsMetric.MEDIAN,
                StatsMetric.PERCENTILE_90))
        .temporalTimeSeriesField("dtexerc", "dtexerc")
        .numericHistogramMeasureField("jornadaSemanal", "jornadaSemanal")
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
  protected ResourceFilterAccessScope<Vinculo> resolveResourceFilterAccessScope() {
    var explicit = tenant != null ? tenant.explicitOrgao() : OptionalInt.empty();
    if (explicit.isEmpty()) {
      return ResourceFilterAccessScope.unrestricted();
    }
    int orgao = explicit.getAsInt();
    return ResourceFilterAccessScope.restricted(
        (root, query, cb) -> cb.equal(root.get("empCodigo"), orgao));
  }

  @Override
  @Transactional
  public ResourceRepresentationResult<VinculoResponseDTO> update(
      VinculoId id,
      UpdateVinculoDTO dto,
      ResourceVersionUpdatePrecondition<VinculoId> precondition) {
    if (precondition == null) throw ResourceVersionPreconditionException.required();
    var readOnlyKeys = validateCustomFields(dto, "request");
    var entity = findEntityById(id);
    entityManager.lock(entity, LockModeType.PESSIMISTIC_WRITE);
    entityManager.refresh(entity);
    precondition.requireMatch(entity.getVersion());
    beforeUpdate(id, entity, dto);
    var old = CustomFieldSupport.snapshotReadOnlyValues(entity, readOnlyKeys);
    mapper.applyUpdate(entity, dto);
    checkOrg(entity.getEmpCodigo());
    if (invariants != null) {
      invariants.vinculo(entity, false, false);
    }
    validateVinculoProvimento(entity, false);
    CustomFieldSupport.restoreReadOnlyValues(entity, readOnlyKeys, old);
    var saved = getRepository().save(entity);
    afterUpdate(id, saved, dto);
    entityManager.flush();
    entityManager.refresh(saved);
    return enriched(toResourceRepresentation(saved));
  }

  @Transactional
  public ResourceRepresentationResult<VinculoResponseDTO> registrarVacancia(
      VinculoId id,
      RegistrarVacanciaDTO dto,
      ResourceVersionUpdatePrecondition<VinculoId> precondition) {
    Vinculo e = findEntityById(id);
    entityManager.lock(e, LockModeType.PESSIMISTIC_WRITE);
    entityManager.refresh(e);
    precondition.requireMatch(e.getVersion());
    checkOrg(e.getEmpCodigo());
    if (!"ATIVO".equals(e.getSituacao()))
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Somente vínculo ativo pode receber esta ação.");
    if (e.getDtexerc() != null && dto.data().isBefore(e.getDtexerc()))
      throw new ResponseStatusException(
          HttpStatus.UNPROCESSABLE_ENTITY, "Vacância anterior ao exercício.");
    e.setDtvac(dto.data());
    e.setFormavac(dto.forma());
    e.setMotivovac(dto.motivo());
    ErgonInvariants.chronology(dto.data(), dto.dtPgtoAte(), "Pagamento final anterior à vacância.");
    e.setDtPgtoAte(dto.dtPgtoAte());
    e.setSituacao("VACANTE");
    validateVinculoProvimento(e, false);
    if (invariants != null) {
      invariants.vinculo(e, false, false);
    }
    entityManager.flush();
    return toResourceRepresentation(e);
  }

  @Override
  protected void beforeCreate(CreateVinculoDTO dto, Vinculo e) {
    if (invariants != null) {
      invariants.vinculo(e, true, false);
    }
    validateVinculoProvimento(e, true);
    checkOrg(e.getEmpCodigo());
    if (getRepository().existsById(e.getId()))
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Vínculo já cadastrado.");
    Orgao o = entityManager.find(Orgao.class, e.getEmpCodigo());
    if (o == null || !o.getAtiva())
      throw new ResponseStatusException(
          HttpStatus.UNPROCESSABLE_ENTITY, "Órgão indisponível para admissão.");
    if (entityManager.find(ServidorPessoa.class, e.getId().numfunc()) == null)
      throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Pessoa inexistente.");
  }

  @Override
  protected void beforeUpdate(VinculoId id, Vinculo e, UpdateVinculoDTO dto) {
    checkOrg(e.getEmpCodigo());
  }

  private void checkOrg(Integer orgao) {
    if (!java.util.Objects.equals(orgao, tenant.orgao()))
      throw new ResponseStatusException(
          HttpStatus.UNPROCESSABLE_ENTITY, "Órgão do vínculo diverge de X-Tenant.");
  }
}
