package br.com.praxis.ergonshowcase.service;

import br.com.praxis.ergonshowcase.config.TenantContext;
import br.com.praxis.ergonshowcase.domain.*;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.mapper.DependenteMapper;
import br.com.praxis.ergonshowcase.repository.DependenteRepository;
import br.com.praxis.ergonshowcase.rules.ErgonRuleEngineFacade;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Consumer;
import org.praxisplatform.rules.contract.RuleEvaluationResult;
import org.praxisplatform.uischema.concurrency.*;
import org.praxisplatform.uischema.customfields.CustomFieldSupport;
import org.praxisplatform.uischema.exporting.CollectionExportField;
import org.praxisplatform.uischema.service.base.*;
import org.praxisplatform.uischema.stats.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Operações transacionais e comandos de ciclo de vida do cadastro de dependentes. */
@Service
public class DependenteService
    extends ErgonResourceService<
        Dependente,
        DependenteResponseDTO,
        DependenteId,
        DependenteFilterDTO,
        CreateDependenteDTO,
        UpdateDependenteDTO>
    implements VersionedCreateUpdateResourceService<
        DependenteResponseDTO,
        DependenteId,
        DependenteFilterDTO,
        CreateDependenteDTO,
        UpdateDependenteDTO> {

  @Autowired(required = false)
  private ErgonInvariants invariants;
  protected final DependenteMapper mapper;
  private final TenantContext tenant;
  private final ErgonRuleEngineFacade ruleEngineFacade;

  public DependenteService(DependenteRepository repository, DependenteMapper mapper) {
    this(repository, mapper, new TenantContext(), new ErgonRuleEngineFacade());
  }

  @Autowired
  public DependenteService(
      DependenteRepository repository,
      DependenteMapper mapper,
      TenantContext tenant,
      ErgonRuleEngineFacade ruleEngineFacade) {
    super(repository, Dependente.class, "id.numfunc", "id.numdep");
    this.mapper = mapper;
    this.tenant = tenant != null ? tenant : new TenantContext();
    this.ruleEngineFacade =
        ruleEngineFacade != null ? ruleEngineFacade : new ErgonRuleEngineFacade();
  }

  public ErgonRuleEngineFacade getRuleEngineFacade() {
    return ruleEngineFacade;
  }

  /**
   * Avalia deterministicamente o rascunho de {@link Dependente} contra o
   * {@link br.com.praxis.ergonshowcase.rules.DependenteElegibilidadeRuleSet} do órgão corrente.
   */
  public RuleEvaluationResult evaluateDependenteElegibilidade(Dependente draft) {
    return evaluateDependenteElegibilidade(draft, null, null);
  }

  /**
   * Avalia deterministicamente o rascunho de {@link Dependente} informando o órgão e a data de
   * referência explícitos.
   */
  public RuleEvaluationResult evaluateDependenteElegibilidade(
      Dependente draft, Integer empCodigo, LocalDate referenceDate) {
    ErgonRuleEngineFacade.DependenteRuleContext ctx =
        buildDependenteRuleContext(draft, empCodigo, referenceDate);
    return ruleEngineFacade.evaluateDependente(ctx);
  }

  /**
   * Valida o rascunho de {@link Dependente} em modo <i>fail-closed</i>, lançando
   * {@link br.com.praxis.ergonshowcase.rules.ErgonRuleViolationException} (HTTP 422) se negado.
   */
  public RuleEvaluationResult validateDependenteElegibilidade(Dependente draft) {
    return validateDependenteElegibilidade(draft, null, null);
  }

  /**
   * Valida o rascunho de {@link Dependente} em modo <i>fail-closed</i> com órgão e data de
   * referência explícitos.
   */
  public RuleEvaluationResult validateDependenteElegibilidade(
      Dependente draft, Integer empCodigo, LocalDate referenceDate) {
    ErgonRuleEngineFacade.DependenteRuleContext ctx =
        buildDependenteRuleContext(draft, empCodigo, referenceDate);
    return ruleEngineFacade.enforceDependente(ctx);
  }

  /** Retorna o diagnóstico estruturado da avaliação determinística de elegibilidade de dependente. */
  public Map<String, Object> evaluateDependenteDiagnostic(
      Dependente draft, Integer empCodigo, LocalDate referenceDate) {
    Integer effectiveOrg = empCodigo != null ? empCodigo : (tenant != null ? tenant.orgao() : 1);
    RuleEvaluationResult result = evaluateDependenteElegibilidade(draft, effectiveOrg, referenceDate);
    return ruleEngineFacade.toDiagnosticPayload(
        ruleEngineFacade.resolveDependentePlan(effectiveOrg), result);
  }

  /** Constrói o contexto determinístico de avaliação de {@link Dependente}. */
  public ErgonRuleEngineFacade.DependenteRuleContext buildDependenteRuleContext(
      Dependente draft, Integer empCodigo, LocalDate referenceDate) {
    Integer effectiveOrg = empCodigo != null ? empCodigo : (tenant != null ? tenant.orgao() : 1);
    if (draft == null) {
      return new ErgonRuleEngineFacade.DependenteRuleContext(
          null, effectiveOrg, null, null, null, List.of(), referenceDate);
    }
    if (!Objects.equals(effectiveOrg, tenant.orgao()))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Órgão do dependente fora do escopo corrente.");
    if (getEntityManager() != null && draft.getId() != null && draft.getId().numfunc() != null)
      assertEntityAccessAllowed(draft);
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
    TipoDependente tipo = draft.getTipoDependenteEntidade();
    if (draft.getTipodepen() == null || draft.getTipodepen().isBlank()) {
      tipo = null;
    } else if (tipo != null && !Objects.equals(tipo.getTipodepen(), draft.getTipodepen())) {
      tipo = null;
    }
    if (tipo == null
        && getEntityManager() != null
        && draft.getTipodepen() != null
        && !draft.getTipodepen().isBlank()) {
      tipo = getEntityManager().find(TipoDependente.class, draft.getTipodepen());
    }
    DocumentoLegal doc = draft.getDocumentoLegal();
    if (draft.getDocumentoLegalId() == null) {
      doc = null;
    } else if (doc != null && !Objects.equals(doc.getId(), draft.getDocumentoLegalId())) {
      doc = null;
    }
    if (doc == null && getEntityManager() != null && draft.getDocumentoLegalId() != null) {
      doc = getEntityManager().find(DocumentoLegal.class, draft.getDocumentoLegalId());
    }
    ErgonTenantAccess.requireReferencesInOrganization(effectiveOrg, doc);
    List<Dependente> existing = List.of();
    if (getEntityManager() != null && draft.getId() != null && draft.getId().numfunc() != null) {
      var q =
          getEntityManager().createQuery(
              "select x from Dependente x where x.id.numfunc=:f and (:n is null or x.id.numdep<>:n) and exists (select 1 from Vinculo v where v.id.numfunc=x.id.numfunc and v.empCodigo=:org and v.situacao in ('ATIVO','AFASTADO','CEDIDO'))",
              Dependente.class);
      if (q != null) {
        existing =
            q.setParameter("org", effectiveOrg).setParameter("f", draft.getId().numfunc())
                .setParameter("n", draft.getId().numdep())
                .setFlushMode(FlushModeType.COMMIT)
                .getResultList();
      }
    }
    return new ErgonRuleEngineFacade.DependenteRuleContext(
        draft, effectiveOrg, pessoa, tipo, doc, existing, referenceDate);
  }

  @Override
  protected List<CollectionExportField> exportFields() {
    var datePresentation =
        new org.praxisplatform.uischema.exporting.CollectionExportFieldPresentation(
            "date", "dd/MM/yyyy", null, "pt-BR", "America/Sao_Paulo", null, null, "Não informado");
    var boolPresentation =
        new org.praxisplatform.uischema.exporting.CollectionExportFieldPresentation(
            "boolean", null, null, "pt-BR", null, "Sim", "Não", "Não informado");
    return List.of(
        field("id", "Identificador do dependente", "string"),
        field("numfunc", "Servidor titular", "integer"),
        field("numdep", "Sequencial", "integer"),
        field("nome", "Nome do dependente", "string"),
        field("parentesco", "Parentesco", "string"),
        field("tipodepen", "Tipo de dependência", "string"),
        presentedField("dtini", "Início da vigência", "date", datePresentation),
        presentedField("dtfim", "Fim da vigência", "date", datePresentation),
        presentedField("depIr", "Deduz IRRF", "boolean", boolPresentation),
        presentedField("depSalarioFamilia", "Salário-família", "boolean", boolPresentation),
        presentedField("depPlanoSaude", "Plano de saúde", "boolean", boolPresentation),
        presentedField("estudante", "Estudante regular", "boolean", boolPresentation),
        presentedField("universitario", "Universitário", "boolean", boolPresentation));
  }

  @Override
  protected DependenteMapper getResourceMapper() {
    return mapper;
  }

  @Override
  protected DependenteFilterDTO emptyFilter() {
    return new DependenteFilterDTO();
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
            "nome",
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
            "parentesco",
            "parentesco",
            Set.of(
                StatsMetric.COUNT,
                StatsMetric.MEDIAN,
                StatsMetric.PERCENTILE_90))
        .categoricalGroupByBucket("tipodepen", "tipodepen")
        .temporalTimeSeriesField("dtini", "dtini")
        .temporalTimeSeriesField("dtnasc", "dtnasc")
        .numericHistogramMeasureField("numdep", "id.numdep")
        .build();
  }

  @Override
  protected void applyResourceUpdate(DependenteId id, Dependente entity, UpdateDependenteDTO dto) {
    mapper.applyUpdate(entity, dto);
    if (invariants != null) {
      invariants.dependente(entity, false, false);
    }
    validateDependenteElegibilidade(entity);
  }

  @Override
  protected void beforeCreate(CreateDependenteDTO dto, Dependente entity) {
    assertEntityAccessAllowed(entity);
    if (invariants != null) {
      invariants.dependente(entity, true, false);
    }
    validateDependenteElegibilidade(entity);
    if (getRepository().existsById(entity.getId())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Dependente já cadastrado para este servidor e sequencial.");
    }
  }

  @Override
  protected void beforeDelete(DependenteId id, Dependente entity) {
    if (entity != null
        && entity.getDtfim() == null
        && (Boolean.TRUE.equals(entity.getDepIr())
            || Boolean.TRUE.equals(entity.getDepSalarioFamilia())
            || Boolean.TRUE.equals(entity.getDepPlanoSaude()))) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Não é permitido excluir dependente em vigência ativa com benefícios habilitados; encerre a dependência antes da exclusão.");
    }
  }

  private ResourceRepresentationResult<DependenteResponseDTO> command(
      DependenteId id,
      ResourceVersionUpdatePrecondition<DependenteId> precondition,
      Consumer<Dependente> action) {
    return executeVersionedMutation(id, precondition, entity -> {
      action.accept(entity);
    if (invariants != null) {
      invariants.dependente(entity, false, false);
    }
    validateDependenteElegibilidade(entity);
    });
  }

  @Transactional
  public ResourceRepresentationResult<DependenteResponseDTO> atualizarElegibilidade(
      DependenteId id,
      AtualizarElegibilidadeDependenteDTO dto,
      ResourceVersionUpdatePrecondition<DependenteId> precondition) {
    return command(
        id,
        precondition,
        (Dependente d) -> {
          if (dto.wasSupplied("tipodepen")) d.setTipodepen(dto.getTipodepen());
          if (dto.wasSupplied("depIr") && dto.getDepIr() != null) d.setDepIr(dto.getDepIr());
          if (dto.wasSupplied("depSalarioFamilia") && dto.getDepSalarioFamilia() != null)
            d.setDepSalarioFamilia(dto.getDepSalarioFamilia());
          if (dto.wasSupplied("depPlanoSaude") && dto.getDepPlanoSaude() != null)
            d.setDepPlanoSaude(dto.getDepPlanoSaude());
          if (dto.wasSupplied("estudante") && dto.getEstudante() != null)
            d.setEstudante(dto.getEstudante());
          if (dto.wasSupplied("universitario") && dto.getUniversitario() != null)
            d.setUniversitario(dto.getUniversitario());
          if (dto.wasSupplied("excepcional") && dto.getExcepcional() != null)
            d.setExcepcional(dto.getExcepcional());
          if (dto.wasSupplied("representanteLegal") && dto.getRepresentanteLegal() != null)
            d.setRepresentanteLegal(dto.getRepresentanteLegal());
          if (dto.wasSupplied("documentoLegalId")) d.setDocumentoLegalId(dto.getDocumentoLegalId());
        });
  }

  @Transactional
  public ResourceRepresentationResult<DependenteResponseDTO> atualizarCertidao(
      DependenteId id,
      AtualizarCertidaoDependenteDTO dto,
      ResourceVersionUpdatePrecondition<DependenteId> precondition) {
    return command(
        id,
        precondition,
        (Dependente d) -> {
          if (dto.wasSupplied("tipodocCert")) d.setTipodocCert(dto.getTipodocCert());
          if (dto.wasSupplied("numCert")) d.setNumCert(dto.getNumCert());
          if (dto.wasSupplied("livroACert")) d.setLivroACert(dto.getLivroACert());
          if (dto.wasSupplied("folhaCert")) d.setFolhaCert(dto.getFolhaCert());
          if (dto.wasSupplied("cartorioCert")) d.setCartorioCert(dto.getCartorioCert());
          if (dto.wasSupplied("ufCart")) d.setUfCart(dto.getUfCart());
          if (dto.wasSupplied("municipioCart")) d.setMunicipioCart(dto.getMunicipioCart());
          if (dto.wasSupplied("matriculaCert")) d.setMatriculaCert(dto.getMatriculaCert());
          if (dto.wasSupplied("documentoLegalId")) d.setDocumentoLegalId(dto.getDocumentoLegalId());
        });
  }

  @Transactional
  public ResourceRepresentationResult<DependenteResponseDTO> encerrarDependencia(
      DependenteId id,
      EncerrarDependenciaDTO dto,
      ResourceVersionUpdatePrecondition<DependenteId> precondition) {
    return command(
        id,
        precondition,
        (Dependente d) -> {
          if (d.getDtfim() != null) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "A vigência deste dependente já se encontra encerrada.");
          }
          d.setDtfim(dto.dtfim());
          d.setDepIr(false);
          d.setDepSalarioFamilia(false);
          d.setDepPlanoSaude(false);
          if (dto.documentoLegalId() != null) {
            d.setDocumentoLegalId(dto.documentoLegalId());
          }
        });
  }

  @Transactional
  public ResourceRepresentationResult<DependenteResponseDTO> reativarDependencia(
      DependenteId id,
      ReativarDependenciaDTO dto,
      ResourceVersionUpdatePrecondition<DependenteId> precondition) {
    return command(
        id,
        precondition,
        (Dependente d) -> {
          if (d.getDtfim() == null) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "A reativação exige que a vigência da dependência esteja encerrada.");
          }
          d.setDtfim(null);
          d.setDocumentoLegalId(dto.documentoLegalId());
          d.setUniversitario(dto.universitario());
          d.setDepIr(dto.depIr());
        });
  }

  @Transactional
  public ResourceRepresentationResult<DependenteResponseDTO> homologarInvalidez(
      DependenteId id,
      HomologarInvalidezDependenteDTO dto,
      ResourceVersionUpdatePrecondition<DependenteId> precondition) {
    return command(
        id,
        precondition,
        (Dependente d) -> {
          if (Boolean.TRUE.equals(d.getInvalido())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "A invalidez permanente já foi homologada para este dependente.");
          }
          d.setInvalido(true);
          d.setDtfim(null);
          d.setDocumentoLegalId(dto.documentoLegalId());
          d.setDepIr(dto.depIr());
        });
  }
  @Override protected java.util.Optional<org.praxisplatform.uischema.capability.ResourceStateSnapshot> toResourceStateSnapshot(Dependente e) {
    return java.util.Optional.of(snapshotState(e));
  }
  private org.praxisplatform.uischema.capability.ResourceStateSnapshot snapshotState(Dependente e) {
    return new org.praxisplatform.uischema.capability.ResourceStateSnapshot(e.getDtfim() == null ? "VIGENTE" : "ENCERRADO", java.util.Map.of("invalido",Boolean.TRUE.equals(e.getInvalido())));
  }

  @Override protected String resolveResourceKey() { return "administracao-pessoal.dependentes"; }

}
