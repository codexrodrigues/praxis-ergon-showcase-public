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
  @Autowired private VinculoAccumulationFacts accumulationFacts;
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
    checkOrg(draft.getEmpCodigo());
    Orgao orgao = draft.getOrgao();
    if (draft.getEmpCodigo() == null) {
      orgao = null;
    } else if (orgao != null && !Objects.equals(orgao.getEmpresa(), draft.getEmpCodigo())) {
      orgao = null;
    }
    if (orgao == null && getEntityManager() != null && draft.getEmpCodigo() != null) {
      orgao = getEntityManager().find(Orgao.class, draft.getEmpCodigo());
    }
    ServidorPessoa pessoa = draft.getPessoa();
    if (draft.getId() == null || draft.getId().numfunc() == null) {
      pessoa = null;
    } else if (pessoa != null && !Objects.equals(pessoa.getNumero(), draft.getId().numfunc())) {
      pessoa = null;
    }
    if (pessoa == null
        && getEntityManager() != null
        && draft.getId() != null
        && draft.getId().numfunc() != null) {
      pessoa = getEntityManager().find(ServidorPessoa.class, draft.getId().numfunc());
    }
    Cargo cargo = draft.getCargoEntidade();
    if (draft.getCargo() == null) {
      cargo = null;
    } else if (cargo != null && !Objects.equals(cargo.getCargo(), draft.getCargo())) {
      cargo = null;
    }
    if (cargo == null && getEntityManager() != null && draft.getCargo() != null) {
      cargo = getEntityManager().find(Cargo.class, draft.getCargo());
    }
    Cargo cargoComissao = null;
    if (getEntityManager() != null && draft.getCargoComissao() != null) {
      cargoComissao = getEntityManager().find(Cargo.class, draft.getCargoComissao());
    }
    Setor lotacao = draft.getLotacao();
    if (draft.getSetor() == null || draft.getSetor().isBlank()) {
      lotacao = null;
    } else if (lotacao != null && !Objects.equals(lotacao.getSetor(), draft.getSetor())) {
      lotacao = null;
    }
    if (lotacao == null
        && getEntityManager() != null
        && draft.getSetor() != null
        && !draft.getSetor().isBlank()) {
      lotacao = getEntityManager().find(Setor.class, draft.getSetor());
    }
    Setor exercicio = draft.getExercicio();
    if (draft.getSetorExercicio() == null || draft.getSetorExercicio().isBlank()) {
      exercicio = null;
    } else if (exercicio != null
        && !Objects.equals(exercicio.getSetor(), draft.getSetorExercicio())) {
      exercicio = null;
    }
    if (exercicio == null
        && getEntityManager() != null
        && draft.getSetorExercicio() != null
        && !draft.getSetorExercicio().isBlank()) {
      exercicio = getEntityManager().find(Setor.class, draft.getSetorExercicio());
    }
    DocumentoLegal doc = null;
    if (getEntityManager() != null && draft.getDocumentoLegalId() != null) {
      doc = getEntityManager().find(DocumentoLegal.class, draft.getDocumentoLegalId());
    }
    ErgonTenantAccess.requireReferencesInOrganization(draft.getEmpCodigo(), cargo, cargoComissao, lotacao, exercicio, doc);
    long cargoOcupadas =
        cargo != null && invariants != null
            ? invariants.ocupadas(cargo.getCargo(), draft.getId(), draft.getEmpCodigo())
            : 0L;
    long comissaoOcupadas =
        cargoComissao != null && invariants != null
            ? invariants.ocupadas(cargoComissao.getCargo(), draft.getId(), draft.getEmpCodigo())
            : 0L;
    var existingVinculos = accumulationFacts == null ? null
        : accumulationFacts.load(getEntityManager(), draft);
    int openAfastamentosCount = 0;
    if (getEntityManager() != null && draft.getId() != null && draft.getId().numfunc() != null) {
      if (draft.getId().numero() != null) {
        var qa =
            getEntityManager().createQuery(
                "select count(a) from Afastamento a where a.empCodigo=:org and a.numfunc=:p and a.numvinc=:v and a.situacao='HOMOLOGADO' and a.dtfim is null",
                Long.class);
        if (qa != null) {
          Long cnt =
              qa.setParameter("org", draft.getEmpCodigo()).setParameter("p", draft.getId().numfunc())
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
  protected void applyResourceUpdate(VinculoId id, Vinculo entity, UpdateVinculoDTO dto) {
    mapper.applyUpdate(entity, dto);
    checkOrg(entity.getEmpCodigo());
    if (invariants != null) {
      invariants.vinculo(entity, false, false);
    }
    validateVinculoProvimento(entity, false);
  }

  @Transactional
  public ResourceRepresentationResult<VinculoResponseDTO> registrarVacancia(
      VinculoId id,
      RegistrarVacanciaDTO dto,
      ResourceVersionUpdatePrecondition<VinculoId> precondition) {
    return executeVersionedMutation(id, precondition, (Vinculo e) -> {
    checkOrg(e.getEmpCodigo());
    if (!"ATIVO".equals(e.getSituacao()))
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Somente vínculo ativo pode receber esta ação.");
    e.setDtvac(dto.data());
    e.setFormavac(dto.forma());
    e.setMotivovac(dto.motivo());
    e.setDtPgtoAte(dto.dtPgtoAte());
    e.setSituacao("VACANTE");
    validateVinculoProvimento(e, false);
    if (invariants != null) {
      invariants.vinculo(e, false, false);
    }
    });
  }

  @Override
  @Transactional
  public BaseResourceCommandService.SavedResult<VinculoId, VinculoResponseDTO> create(CreateVinculoDTO dto) {
    checkOrg(dto.getEmpCodigo());
    return super.create(dto);
  }

  @Override
  protected void beforeCreate(CreateVinculoDTO dto, Vinculo e) {
    checkOrg(e.getEmpCodigo());
    if (invariants != null) {
      invariants.vinculo(e, true, false);
    }
    validateVinculoProvimento(e, true);
    if (getRepository().existsById(e.getId()))
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Vínculo já cadastrado.");
  }

  @Override
  protected void beforeUpdate(VinculoId id, Vinculo e, UpdateVinculoDTO dto) {
    checkOrg(e.getEmpCodigo());
  }

  @Override
  protected void beforeDelete(VinculoId id, Vinculo entity) {
    if (entity != null) {
      checkOrg(entity.getEmpCodigo());
    }
    if (getEntityManager() != null && id != null && entity != null) {
      long afastamentos =
          getEntityManager()
              .createQuery(
                  "select count(a) from Afastamento a where a.empCodigo=:org and a.numfunc=:p and a.numvinc=:v",
                  Long.class)
              .setParameter("org", entity.getEmpCodigo()).setParameter("p", id.numfunc())
              .setParameter("v", id.numero())
              .getSingleResult();
      if (afastamentos > 0) {
        throw new ResponseStatusException(
            HttpStatus.CONFLICT,
            "Não é permitido excluir vínculo funcional que possui históricos de afastamento.");
      }
    }
  }

  private void checkOrg(Integer orgao) {
    if (!java.util.Objects.equals(orgao, tenant.orgao()))
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Órgão do vínculo diverge de X-Tenant.");
  }
  @Override protected java.util.Optional<org.praxisplatform.uischema.capability.ResourceStateSnapshot> toResourceStateSnapshot(Vinculo e) {
    return java.util.Optional.of(snapshotState(e));
  }
  private org.praxisplatform.uischema.capability.ResourceStateSnapshot snapshotState(Vinculo e) {
    return org.praxisplatform.uischema.capability.ResourceStateSnapshot.of(e.getSituacao());
  }

  @Override protected String resolveResourceKey() { return "administracao-pessoal.vinculos"; }

}
