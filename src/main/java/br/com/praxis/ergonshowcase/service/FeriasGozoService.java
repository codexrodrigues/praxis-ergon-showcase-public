package br.com.praxis.ergonshowcase.service;

import br.com.praxis.ergonshowcase.config.TenantContext;
import br.com.praxis.ergonshowcase.domain.Afastamento;
import br.com.praxis.ergonshowcase.domain.FeriasGozo;
import br.com.praxis.ergonshowcase.domain.Frequencia;
import br.com.praxis.ergonshowcase.domain.ParametroContagemFerias;
import br.com.praxis.ergonshowcase.domain.PeriodoAquisitivoFerias;
import br.com.praxis.ergonshowcase.domain.Vinculo;
import br.com.praxis.ergonshowcase.domain.VinculoId;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.mapper.FeriasGozoMapper;
import br.com.praxis.ergonshowcase.mapper.PeriodoAquisitivoFeriasMapper;
import br.com.praxis.ergonshowcase.repository.FeriasGozoRepository;
import br.com.praxis.ergonshowcase.rules.ErgonRuleEngineFacade;
import br.com.praxis.ergonshowcase.rules.ErgonRuleViolationException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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
 * Operações transacionais, invariantes e ciclo de vida das parcelas de programação e gozo de férias
 * ({@code FERIAS} + {@code ERG_ABONOS_FERIAS} / {@code PCK_FERIAS}).
 */
@Service
public class FeriasGozoService
    extends ErgonResourceService<
        FeriasGozo,
        FeriasGozoResponseDTO,
        Long,
        FeriasGozoFilterDTO,
        CreateFeriasGozoDTO,
        UpdateFeriasGozoDTO>
    implements VersionedCreateUpdateResourceService<
        FeriasGozoResponseDTO,
        Long,
        FeriasGozoFilterDTO,
        CreateFeriasGozoDTO,
        UpdateFeriasGozoDTO> {

  @Autowired(required = false)
  private ErgonInvariants invariants;

  @PersistenceContext protected EntityManager entityManager;
  protected final FeriasGozoMapper mapper;
  private final TenantContext tenant;
  private final ErgonRuleEngineFacade ruleEngineFacade;

  public FeriasGozoService(
      FeriasGozoRepository repository, FeriasGozoMapper mapper, TenantContext tenant) {
    this(repository, mapper, tenant, new ErgonRuleEngineFacade());
  }

  @Autowired
  public FeriasGozoService(
      FeriasGozoRepository repository,
      FeriasGozoMapper mapper,
      TenantContext tenant,
      ErgonRuleEngineFacade ruleEngineFacade) {
    super(repository, FeriasGozo.class, "id");
    this.mapper = mapper;
    this.tenant = tenant != null ? tenant : new TenantContext();
    this.ruleEngineFacade =
        ruleEngineFacade != null ? ruleEngineFacade : new ErgonRuleEngineFacade();
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
        field("id", "ID da parcela", "integer"),
        field("empCodigo", "Órgão", "integer"),
        field("numfunc", "Servidor", "integer"),
        field("numvinc", "Vínculo", "integer"),
        field("vinculoMatricula", "Matrícula", "string"),
        field("periodoAquisitivoId", "ID do PA", "integer"),
        presentedField("chavePeraqfer", "Chave do PA", "integer", intPresentation),
        presentedField("ordemParcela", "Ordem da parcela", "integer", intPresentation),
        presentedField("dtini", "Início do gozo", "date", datePresentation),
        presentedField("dtprevfim", "Término previsto", "date", datePresentation),
        presentedField("dtfim", "Término efetivo", "date", datePresentation),
        presentedField("diasGozo", "Dias de gozo", "integer", intPresentation),
        presentedField("abonoPecuniario", "Com abono pecuniário", "boolean", boolPresentation),
        presentedField("diasAbonoVendidos", "Dias abono vendidos", "integer", intPresentation),
        presentedField("adiantamento13", "Adiantamento 13º", "boolean", boolPresentation),
        field("situacao", "Situação", "string"));
  }

  @Override
  protected FeriasGozoMapper getResourceMapper() {
    return mapper;
  }

  @Override
  protected FeriasGozoFilterDTO emptyFilter() {
    return new FeriasGozoFilterDTO();
  }

  @Override
  protected OptionalLong getEntityResourceVersion(FeriasGozo entity) {
    return OptionalLong.of(entity.getVersion());
  }

  @Override
  public Optional<String> getDatasetVersion() {
    return Optional.empty();
  }

  @Override
  protected org.praxisplatform.uischema.service.base.ResourceFilterAccessScope<FeriasGozo>
      resolveResourceFilterAccessScope() {
    var explicitOrgao = tenant.explicitOrgao();
    if (explicitOrgao.isEmpty()) {
      return org.praxisplatform.uischema.service.base.ResourceFilterAccessScope.unrestricted();
    }
    int orgao = explicitOrgao.getAsInt();
    return org.praxisplatform.uischema.service.base.ResourceFilterAccessScope.restricted(
        (root, query, cb) -> cb.equal(root.get("empCodigo"), orgao));
  }

  @Override
  public StatsFieldRegistry getStatsFieldRegistry() {
    return StatsFieldRegistry.builder()
        .labeledGroupByBucket(
            "id",
            "id",
            "situacao",
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
        .categoricalGroupByBucket("ordemParcela", "ordemParcela")
        .categoricalGroupByBucket("abonoPecuniario", "abonoPecuniario")
        .temporalTimeSeriesField("dtini", "dtini")
        .numericHistogramMeasureField("diasGozo", "diasGozo")
        .numericHistogramMeasureField("diasAbonoVendidos", "diasAbonoVendidos")
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
  protected void beforeCreate(CreateFeriasGozoDTO dto, FeriasGozo entity) {
    hydrateParentPeriodoAquisitivo(entity);
    checkOrg(entity.getEmpCodigo());
    validateFeriasGozo(entity);
    if (invariants != null) {
      invariants.feriasGozo(entity, true);
    }
  }

  public void hydrateParentPeriodoAquisitivo(FeriasGozo entity) {
    hydrateParentPeriodoAquisitivo(entity, false);
  }

  public void hydrateParentPeriodoAquisitivo(FeriasGozo entity, boolean reparented) {
    if (entity == null) {
      return;
    }
    if (entityManager != null && entity.getPeriodoAquisitivoId() != null) {
      PeriodoAquisitivoFerias pa =
          entityManager.find(PeriodoAquisitivoFerias.class, entity.getPeriodoAquisitivoId());
      if (pa != null) {
        if (reparented || entity.getEmpCodigo() == null) {
          entity.setEmpCodigo(pa.getEmpCodigo());
        }
        if (reparented || entity.getNumfunc() == null) {
          entity.setNumfunc(pa.getNumfunc());
        }
        if (reparented || entity.getNumvinc() == null) {
          entity.setNumvinc(pa.getNumvinc());
        }
        if (pa.getChave() != null) {
          entity.setChavePeraqfer(pa.getChave());
        }
        if (pa.getTotDiasDireito() != null) {
          entity.setTotDiasDireitoPa(pa.getTotDiasDireito());
        }
      }
    }
    if (entity.getEmpCodigo() == null && tenant != null) {
      entity.setEmpCodigo(tenant.orgao());
    }
    if (entity.getTotDiasDireitoPa() == null) {
      entity.setTotDiasDireitoPa(30);
    }
  }

  @Override
  protected void afterCreate(CreateFeriasGozoDTO dto, FeriasGozo saved) {
    sincronizarContabilidadePeriodoAquisitivo(saved.getPeriodoAquisitivoId());
  }

  @Override
  protected void beforeUpdate(Long id, FeriasGozo entity, UpdateFeriasGozoDTO dto) {
    checkOrg(entity.getEmpCodigo());
  }

  @Override
  protected void afterUpdate(Long id, FeriasGozo saved, UpdateFeriasGozoDTO dto) {
    sincronizarContabilidadePeriodoAquisitivo(saved.getPeriodoAquisitivoId());
  }

  @Override
  @Transactional
  public ResourceRepresentationResult<FeriasGozoResponseDTO> update(
      Long id, UpdateFeriasGozoDTO dto, ResourceVersionUpdatePrecondition<Long> precondition) {
    if (precondition == null) throw ResourceVersionPreconditionException.required();
    var readOnlyKeys = validateCustomFields(dto, "request");
    FeriasGozo entity = findEntityById(id);
    entityManager.lock(entity, LockModeType.PESSIMISTIC_WRITE);
    entityManager.refresh(entity);
    precondition.requireMatch(entity.getVersion());
    beforeUpdate(id, entity, dto);
    var old = CustomFieldSupport.snapshotReadOnlyValues(entity, readOnlyKeys);
    FeriasGozo previousSnapshot = snapshotBeforeMutation(entity);
    boolean reparented =
        previousSnapshot != null
            && previousSnapshot.getPeriodoAquisitivoId() != null
            && dto.getPeriodoAquisitivoId() != null
            && !Objects.equals(
                previousSnapshot.getPeriodoAquisitivoId(), dto.getPeriodoAquisitivoId());
    if (reparented && !"PROGRAMADA".equals(previousSnapshot.getSituacao())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Somente parcelas programadas podem ser realocadas para outro período aquisitivo.");
    }
    mapper.applyUpdate(entity, dto);
    hydrateParentPeriodoAquisitivo(entity, reparented);
    checkOrg(entity.getEmpCodigo());
    validateFeriasGozo(entity, previousSnapshot);
    if (invariants != null) {
      invariants.feriasGozo(entity, false);
    }
    CustomFieldSupport.restoreReadOnlyValues(entity, readOnlyKeys, old);
    var saved = getRepository().save(entity);
    if (previousSnapshot != null
        && previousSnapshot.getPeriodoAquisitivoId() != null
        && !Objects.equals(
            previousSnapshot.getPeriodoAquisitivoId(), saved.getPeriodoAquisitivoId())) {
      sincronizarContabilidadePeriodoAquisitivo(previousSnapshot.getPeriodoAquisitivoId());
    }
    afterUpdate(id, saved, dto);
    entityManager.flush();
    entityManager.refresh(saved);
    return readFacts != null
        ? enriched(toResourceRepresentation(saved))
        : toResourceRepresentation(saved);
  }

  private ResourceRepresentationResult<FeriasGozoResponseDTO> command(
      Long id, ResourceVersionUpdatePrecondition<Long> precondition, Consumer<FeriasGozo> action) {
    if (precondition == null) throw ResourceVersionPreconditionException.required();
    FeriasGozo entity = findEntityById(id);
    entityManager.lock(entity, LockModeType.PESSIMISTIC_WRITE);
    entityManager.refresh(entity);
    precondition.requireMatch(entity.getVersion());
    checkOrg(entity.getEmpCodigo());
    FeriasGozo previousSnapshot = snapshotBeforeMutation(entity);
    action.accept(entity);
    if (!"CANCELADA".equals(entity.getSituacao())) {
      validateFeriasGozo(entity, previousSnapshot);
    }
    if (invariants != null && !"CANCELADA".equals(entity.getSituacao())) {
      invariants.feriasGozo(entity, false);
    }
    var saved = getRepository().save(entity);
    sincronizarContabilidadePeriodoAquisitivo(saved.getPeriodoAquisitivoId());
    entityManager.flush();
    entityManager.refresh(saved);
    return readFacts != null
        ? enriched(toResourceRepresentation(saved))
        : toResourceRepresentation(saved);
  }

  @Transactional
  public ResourceRepresentationResult<FeriasGozoResponseDTO> atualizarAbonoFinanceiro(
      Long id,
      AtualizarAbonoFinanceiroFeriasGozoDTO dto,
      ResourceVersionUpdatePrecondition<Long> precondition) {
    return command(
        id,
        precondition,
        (FeriasGozo fg) -> {
          fg.setAbonoPecuniario(Boolean.TRUE.equals(dto.getAbonoPecuniario()));
          fg.setIniFimAbono(dto.getIniFimAbono());
          fg.setDiasAbonoVendidos(
              dto.getDiasAbonoVendidos() != null ? dto.getDiasAbonoVendidos() : 0);
          fg.setDtVendaAbono(dto.getDtVendaAbono());
          fg.setDtPagtoAbono(dto.getDtPagtoAbono());
          fg.setAbonoConstitucional(!Boolean.FALSE.equals(dto.getAbonoConstitucional()));
          fg.setAdiantamento13(Boolean.TRUE.equals(dto.getAdiantamento13()));
          fg.setAdiantamentoSalario(Boolean.TRUE.equals(dto.getAdiantamentoSalario()));
          fg.setEmprestimoFerias(Boolean.TRUE.equals(dto.getEmprestimoFerias()));
        });
  }

  @Transactional
  public ResourceRepresentationResult<FeriasGozoResponseDTO> atualizarSubstitutoFolha(
      Long id,
      AtualizarSubstitutoFolhaFeriasGozoDTO dto,
      ResourceVersionUpdatePrecondition<Long> precondition) {
    return command(
        id,
        precondition,
        (FeriasGozo fg) -> {
          fg.setMesAnoFolha(dto.getMesAnoFolha());
          fg.setNumFolha(dto.getNumFolha());
          fg.setNumfuncSubstituto(dto.getNumfuncSubstituto());
          fg.setNumvincSubstituto(dto.getNumvincSubstituto());
          if (dto.getDocumentoLegalId() != null) fg.setDocumentoLegalId(dto.getDocumentoLegalId());
          if (dto.getAtoPublicacaoId() != null) fg.setAtoPublicacaoId(dto.getAtoPublicacaoId());
          if (dto.getObservacoes() != null) fg.setObservacoes(dto.getObservacoes());
        });
  }

  @Transactional
  public ResourceRepresentationResult<FeriasGozoResponseDTO> homologarFeriasGozo(
      Long id,
      TransicaoFeriasGozoDTO dto,
      ResourceVersionUpdatePrecondition<Long> precondition) {
    return command(
        id,
        precondition,
        (FeriasGozo fg) -> {
          if (!"PROGRAMADA".equals(fg.getSituacao())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "Somente parcelas programadas podem ser homologadas.");
          }
          fg.setSituacao("HOMOLOGADA");
          if (dto.getDocumentoLegalId() != null) fg.setDocumentoLegalId(dto.getDocumentoLegalId());
          if (dto.getAtoPublicacaoId() != null) fg.setAtoPublicacaoId(dto.getAtoPublicacaoId());
          fg.setObservacoes(dto.getObservacoes());
        });
  }

  @Transactional
  public ResourceRepresentationResult<FeriasGozoResponseDTO> iniciarFeriasGozo(
      Long id,
      TransicaoFeriasGozoDTO dto,
      ResourceVersionUpdatePrecondition<Long> precondition) {
    return command(
        id,
        precondition,
        (FeriasGozo fg) -> {
          if (!"HOMOLOGADA".equals(fg.getSituacao())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "Somente parcelas homologadas podem entrar em gozo.");
          }
          fg.setSituacao("EM_GOZO");
          if (dto.getDocumentoLegalId() != null) fg.setDocumentoLegalId(dto.getDocumentoLegalId());
          if (dto.getAtoPublicacaoId() != null) fg.setAtoPublicacaoId(dto.getAtoPublicacaoId());
          fg.setObservacoes(dto.getObservacoes());
        });
  }

  @Transactional
  public ResourceRepresentationResult<FeriasGozoResponseDTO> concluirFeriasGozo(
      Long id,
      TransicaoFeriasGozoDTO dto,
      ResourceVersionUpdatePrecondition<Long> precondition) {
    return command(
        id,
        precondition,
        (FeriasGozo fg) -> {
          if (!Set.of("HOMOLOGADA", "EM_GOZO").contains(fg.getSituacao())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Somente parcelas homologadas ou em gozo podem ser concluídas.");
          }
          fg.setDtfim(dto.getDtfim() != null ? dto.getDtfim() : fg.getDtprevfim());
          fg.setSituacao("GOZADA");
          if (dto.getDocumentoLegalId() != null) fg.setDocumentoLegalId(dto.getDocumentoLegalId());
          if (dto.getAtoPublicacaoId() != null) fg.setAtoPublicacaoId(dto.getAtoPublicacaoId());
          fg.setObservacoes(dto.getObservacoes());
        });
  }

  @Transactional
  public ResourceRepresentationResult<FeriasGozoResponseDTO> interromperFeriasGozo(
      Long id,
      InterromperFeriasGozoDTO dto,
      ResourceVersionUpdatePrecondition<Long> precondition) {
    return command(
        id,
        precondition,
        (FeriasGozo fg) -> {
          if (!Set.of("HOMOLOGADA", "EM_GOZO").contains(fg.getSituacao())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Somente parcelas homologadas ou efetivamente em gozo podem ser interrompidas.");
          }
          ErgonInvariants.chronology(
              fg.getDtini(),
              dto.getDtInterrupcao(),
              "A data de interrupção não pode anteceder a data de início do gozo.");
          if (fg.getDtprevfim() != null) {
            ErgonInvariants.chronology(
                dto.getDtInterrupcao(),
                fg.getDtprevfim(),
                "A data de interrupção não pode ultrapassar o término previsto da parcela de férias.");
          }
          fg.setDtInterrupcao(dto.getDtInterrupcao());
          fg.setDtfim(dto.getDtInterrupcao());
          fg.setMotivoInterrupcao(dto.getMotivoInterrupcao());
          int diasOriginais = fg.getDiasGozo() != null ? fg.getDiasGozo() : Integer.MAX_VALUE;
          int diasEfetivos =
              Math.min(
                  diasOriginais,
                  Math.max(
                      1,
                      Math.toIntExact(
                          ChronoUnit.DAYS.between(fg.getDtini(), dto.getDtInterrupcao()))));
          fg.setDiasGozo(diasEfetivos);
          fg.setSituacao("INTERROMPIDA");
          if (dto.getDocumentoLegalId() != null) fg.setDocumentoLegalId(dto.getDocumentoLegalId());
          if (dto.getObservacoes() != null) fg.setObservacoes(dto.getObservacoes());
        });
  }

  @Transactional
  public ResourceRepresentationResult<FeriasGozoResponseDTO> cancelarFeriasGozo(
      Long id,
      TransicaoFeriasGozoDTO dto,
      ResourceVersionUpdatePrecondition<Long> precondition) {
    return command(
        id,
        precondition,
        (FeriasGozo fg) -> {
          if (!Set.of("PROGRAMADA", "HOMOLOGADA").contains(fg.getSituacao())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Somente parcelas programadas ou homologadas ainda não usufruídas podem ser canceladas.");
          }
          fg.setSituacao("CANCELADA");
          if (dto.getDocumentoLegalId() != null) fg.setDocumentoLegalId(dto.getDocumentoLegalId());
          if (dto.getAtoPublicacaoId() != null) fg.setAtoPublicacaoId(dto.getAtoPublicacaoId());
          fg.setObservacoes(dto.getObservacoes());
        });
  }

  @Transactional
  public ResourceRepresentationResult<FeriasGozoResponseDTO> iniciarGozo(
      Long id,
      TransicaoFeriasGozoDTO dto,
      ResourceVersionUpdatePrecondition<Long> precondition) {
    return iniciarFeriasGozo(id, dto, precondition);
  }

  @Transactional
  public ResourceRepresentationResult<FeriasGozoResponseDTO> concluirGozo(
      Long id,
      TransicaoFeriasGozoDTO dto,
      ResourceVersionUpdatePrecondition<Long> precondition) {
    return concluirFeriasGozo(id, dto, precondition);
  }

  public void sincronizarContabilidadePeriodoAquisitivo(Long periodoAquisitivoId) {
    if (periodoAquisitivoId == null || entityManager == null) return;
    PeriodoAquisitivoFerias pa =
        entityManager.find(
            PeriodoAquisitivoFerias.class, periodoAquisitivoId, LockModeType.PESSIMISTIC_WRITE);
    if (pa == null) return;
    var q =
        entityManager.createQuery(
            "select x from FeriasGozo x where x.periodoAquisitivoId=:paId and x.situacao<>'CANCELADA'",
            FeriasGozo.class);
    if (q != null) {
      var p1 = q.setParameter("paId", periodoAquisitivoId);
      var parcelas = p1 != null ? p1.getResultList() : List.<FeriasGozo>of();
      if (parcelas != null) {
        int totalGozo =
            parcelas.stream()
                .mapToInt(p -> p.getDiasGozo() != null ? p.getDiasGozo() : 0)
                .sum();
        int totalAbono =
            parcelas.stream()
                .mapToInt(
                    p ->
                        Boolean.TRUE.equals(p.getAbonoPecuniario())
                                && p.getDiasAbonoVendidos() != null
                            ? p.getDiasAbonoVendidos()
                            : 0)
                .sum();
        pa.setDiasGozados(totalGozo);
        pa.setDiasVendidosAbono(totalAbono);
      }
    }
    PeriodoAquisitivoFeriasMapper.recomputeSaldoAndSituacao(pa);
  }

  public RuleEvaluationResult evaluateFeriasGozo(FeriasGozo entity) {
    return ruleEngineFacade.evaluateFeriasGozo(buildFeriasGozoRuleContext(entity, null));
  }

  public void validateFeriasGozo(FeriasGozo entity) {
    validateFeriasGozo(entity, null);
  }

  public void validateFeriasGozo(FeriasGozo entity, FeriasGozo previousSnapshot) {
    ruleEngineFacade.enforceFeriasGozo(buildFeriasGozoRuleContext(entity, previousSnapshot));
  }

  private static FeriasGozo snapshotBeforeMutation(FeriasGozo src) {
    if (src == null) {
      return null;
    }
    FeriasGozo copy = new FeriasGozo();
    copy.setId(src.getId());
    copy.setPeriodoAquisitivoId(src.getPeriodoAquisitivoId());
    copy.setDtini(src.getDtini());
    copy.setDtprevfim(src.getDtprevfim());
    copy.setDtfim(src.getDtfim());
    copy.setSituacao(src.getSituacao());
    copy.setNumfuncSubstituto(src.getNumfuncSubstituto());
    copy.setNumvincSubstituto(src.getNumvincSubstituto());
    return copy;
  }

  private static boolean detectActiveSubstitutionConflict(
      FeriasGozo entity, FeriasGozo previousSnapshot) {
    if (entity == null || previousSnapshot == null) {
      return false;
    }
    if (previousSnapshot.getNumfuncSubstituto() == null
        || previousSnapshot.getNumvincSubstituto() == null) {
      return false;
    }
    if (!"EM_GOZO".equalsIgnoreCase(previousSnapshot.getSituacao())) {
      return false;
    }
    if ("INTERROMPIDA".equalsIgnoreCase(entity.getSituacao())
        || "GOZADA".equalsIgnoreCase(entity.getSituacao())) {
      return false;
    }
    if (previousSnapshot.getDtini() != null
        && entity.getDtini() != null
        && entity.getDtini().isAfter(previousSnapshot.getDtini())) {
      return true;
    }
    LocalDate prevEnd =
        previousSnapshot.getDtfim() != null
            ? previousSnapshot.getDtfim()
            : previousSnapshot.getDtprevfim();
    LocalDate currEnd = entity.getDtfim() != null ? entity.getDtfim() : entity.getDtprevfim();
    return prevEnd != null && currEnd != null && currEnd.isBefore(prevEnd);
  }

  private ErgonRuleEngineFacade.FeriasGozoRuleContext buildFeriasGozoRuleContext(
      FeriasGozo entity, FeriasGozo previousSnapshot) {
    boolean activeSubstConflict = detectActiveSubstitutionConflict(entity, previousSnapshot);
    if (entityManager == null) {
      return new ErgonRuleEngineFacade.FeriasGozoRuleContext(
          entity,
          null,
          null,
          null,
          null,
          List.of(),
          List.of(),
          List.of(),
          List.of(),
          List.of(),
          null,
          activeSubstConflict,
          true);
    }
    Vinculo vinculo = null;
    if (entity.getNumfunc() != null && entity.getNumvinc() != null) {
      vinculo =
          entityManager.find(
              Vinculo.class,
              new VinculoId(entity.getNumfunc(), entity.getNumvinc()),
              LockModeType.PESSIMISTIC_READ);
      if (vinculo == null) {
        vinculo =
            entityManager.find(
                Vinculo.class, new VinculoId(entity.getNumfunc(), entity.getNumvinc()));
      }
    }
    Vinculo substituto = null;
    if (entity.getNumfuncSubstituto() != null && entity.getNumvincSubstituto() != null) {
      substituto =
          entityManager.find(
              Vinculo.class,
              new VinculoId(entity.getNumfuncSubstituto(), entity.getNumvincSubstituto()),
              LockModeType.PESSIMISTIC_READ);
      if (substituto == null) {
        substituto =
            entityManager.find(
                Vinculo.class,
                new VinculoId(entity.getNumfuncSubstituto(), entity.getNumvincSubstituto()));
      }
    }
    PeriodoAquisitivoFerias pa = null;
    if (entity.getPeriodoAquisitivoId() != null) {
      pa =
          entityManager.find(
              PeriodoAquisitivoFerias.class,
              entity.getPeriodoAquisitivoId(),
              LockModeType.PESSIMISTIC_READ);
      if (pa == null) {
        pa = entityManager.find(PeriodoAquisitivoFerias.class, entity.getPeriodoAquisitivoId());
      }
    }
    List<ParametroContagemFerias> paramCandidates =
        ruleEngineFacade.getVacationParameterResolver().loadCandidates(entityManager, vinculo);
    List<FeriasGozo> parcelasVinculo = List.of();
    List<PeriodoAquisitivoFerias> todosPas = List.of();
    List<Afastamento> afastamentos = List.of();
    List<Frequencia> frequencias = List.of();
    if (entity.getNumfunc() != null && entity.getNumvinc() != null) {
      var qFg =
          entityManager.createQuery(
              "select x from FeriasGozo x where x.numfunc=:f and x.numvinc=:v and x.situacao<>'CANCELADA'",
              FeriasGozo.class);
      if (qFg != null) {
        var r1 = qFg.setParameter("f", entity.getNumfunc());
        var r2 = r1 != null ? r1.setParameter("v", entity.getNumvinc()) : null;
        var flushed = r2 != null ? r2.setFlushMode(FlushModeType.COMMIT) : null;
        var list = (flushed != null ? flushed : r2) != null ? (flushed != null ? flushed : r2).getResultList() : null;
        if (list != null) parcelasVinculo = list;
      }
      var qPa =
          entityManager.createQuery(
              "select x from PeriodoAquisitivoFerias x where x.numfunc=:f and x.numvinc=:v and x.situacao<>'CANCELADO'",
              PeriodoAquisitivoFerias.class);
      if (qPa != null) {
        var r1 = qPa.setParameter("f", entity.getNumfunc());
        var r2 = r1 != null ? r1.setParameter("v", entity.getNumvinc()) : null;
        var flushed = r2 != null ? r2.setFlushMode(FlushModeType.COMMIT) : null;
        var list = (flushed != null ? flushed : r2) != null ? (flushed != null ? flushed : r2).getResultList() : null;
        if (list != null) todosPas = list;
      }
      var qAf =
          entityManager.createQuery(
              "select x from Afastamento x where x.numfunc=:f and x.numvinc=:v", Afastamento.class);
      if (qAf != null) {
        var r1 = qAf.setParameter("f", entity.getNumfunc());
        var r2 = r1 != null ? r1.setParameter("v", entity.getNumvinc()) : null;
        var flushed = r2 != null ? r2.setFlushMode(FlushModeType.COMMIT) : null;
        var list = (flushed != null ? flushed : r2) != null ? (flushed != null ? flushed : r2).getResultList() : null;
        if (list != null) afastamentos = list;
      }
      var qFr =
          entityManager.createQuery(
              "select x from Frequencia x where x.numfunc=:f and x.numvinc=:v", Frequencia.class);
      if (qFr != null) {
        var r1 = qFr.setParameter("f", entity.getNumfunc());
        var r2 = r1 != null ? r1.setParameter("v", entity.getNumvinc()) : null;
        var flushed = r2 != null ? r2.setFlushMode(FlushModeType.COMMIT) : null;
        var list = (flushed != null ? flushed : r2) != null ? (flushed != null ? flushed : r2).getResultList() : null;
        if (list != null) frequencias = list;
      }
    }
    return new ErgonRuleEngineFacade.FeriasGozoRuleContext(
        entity,
        pa,
        vinculo,
        substituto,
        null,
        paramCandidates,
        todosPas,
        parcelasVinculo,
        frequencias,
        afastamentos,
        null,
        activeSubstConflict,
        true);
  }

  private void checkOrg(Integer orgao) {
    if (!Objects.equals(orgao, tenant.orgao())) {
      throw new ResponseStatusException(
          HttpStatus.UNPROCESSABLE_ENTITY, "Órgão da parcela de férias diverge de X-Tenant.");
    }
  }
}
