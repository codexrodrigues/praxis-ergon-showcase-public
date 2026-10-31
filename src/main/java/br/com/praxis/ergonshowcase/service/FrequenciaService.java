package br.com.praxis.ergonshowcase.service;

import br.com.praxis.ergonshowcase.config.TenantContext;
import br.com.praxis.ergonshowcase.domain.Afastamento;
import br.com.praxis.ergonshowcase.domain.CodigoFrequencia;
import br.com.praxis.ergonshowcase.domain.CodigoFrequenciaId;
import br.com.praxis.ergonshowcase.domain.DocumentoLegal;
import br.com.praxis.ergonshowcase.domain.Frequencia;
import br.com.praxis.ergonshowcase.domain.RegraFrequenciaEstatutaria;
import br.com.praxis.ergonshowcase.domain.ServidorPessoa;
import br.com.praxis.ergonshowcase.domain.TipoFrequencia;
import br.com.praxis.ergonshowcase.domain.Vinculo;
import br.com.praxis.ergonshowcase.domain.VinculoId;
import br.com.praxis.ergonshowcase.dto.AbonarFaltaFrequenciaDTO;
import br.com.praxis.ergonshowcase.dto.AtualizarHorarioFrequenciaDTO;
import br.com.praxis.ergonshowcase.dto.AtualizarJustificativaFrequenciaDTO;
import br.com.praxis.ergonshowcase.dto.CancelarFrequenciaDTO;
import br.com.praxis.ergonshowcase.dto.CreateFrequenciaDTO;
import br.com.praxis.ergonshowcase.dto.FrequenciaFilterDTO;
import br.com.praxis.ergonshowcase.dto.FrequenciaResponseDTO;
import br.com.praxis.ergonshowcase.dto.HomologarFrequenciaDTO;
import br.com.praxis.ergonshowcase.dto.RejeitarFrequenciaDTO;
import br.com.praxis.ergonshowcase.dto.UpdateFrequenciaDTO;
import br.com.praxis.ergonshowcase.mapper.FrequenciaMapper;
import br.com.praxis.ergonshowcase.repository.FrequenciaRepository;
import br.com.praxis.ergonshowcase.repository.RegraFrequenciaEstatutariaRepository;
import br.com.praxis.ergonshowcase.rules.ErgonRuleEngineFacade;
import br.com.praxis.ergonshowcase.rules.ErgonRuleViolationException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
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

/** Operações transacionais, invariantes e workflow de homologação de ocorrências de frequência. */
@Service
public class FrequenciaService
    extends ErgonResourceService<
        Frequencia,
        FrequenciaResponseDTO,
        Long,
        FrequenciaFilterDTO,
        CreateFrequenciaDTO,
        UpdateFrequenciaDTO>
    implements VersionedCreateUpdateResourceService<
        FrequenciaResponseDTO,
        Long,
        FrequenciaFilterDTO,
        CreateFrequenciaDTO,
        UpdateFrequenciaDTO> {

  @Autowired(required = false)
  private ErgonInvariants invariants;

  @PersistenceContext protected EntityManager entityManager;
  protected final FrequenciaMapper mapper;
  private final TenantContext tenant;
  private final ErgonRuleEngineFacade ruleEngineFacade;
  private final RegraFrequenciaEstatutariaRepository regraRepository;

  public FrequenciaService(
      FrequenciaRepository repository, FrequenciaMapper mapper, TenantContext tenant) {
    this(repository, mapper, tenant, new ErgonRuleEngineFacade(), null);
  }

  @Autowired
  public FrequenciaService(
      FrequenciaRepository repository,
      FrequenciaMapper mapper,
      TenantContext tenant,
      ErgonRuleEngineFacade ruleEngineFacade,
      RegraFrequenciaEstatutariaRepository regraRepository) {
    super(repository, Frequencia.class, "id");
    this.mapper = mapper;
    this.tenant = tenant != null ? tenant : new TenantContext();
    this.ruleEngineFacade =
        ruleEngineFacade != null ? ruleEngineFacade : new ErgonRuleEngineFacade();
    this.regraRepository = regraRepository;
  }

  public ErgonRuleEngineFacade getRuleEngineFacade() {
    return ruleEngineFacade;
  }

  /**
   * Avalia deterministicamente o rascunho de {@link Frequencia} contra o
   * {@link br.com.praxis.ergonshowcase.rules.FrequenciaLancamentoRuleSet} do órgão, resolvendo a
   * {@link RegraFrequenciaEstatutaria} vigente do servidor como provedora dinâmica de fatos
   * ({@code effectiveStatutoryRule}).
   */
  public RuleEvaluationResult evaluateFrequenciaLancamento(Frequencia draft) {
    return evaluateFrequenciaLancamento(draft, null);
  }

  /**
   * Avalia deterministicamente o rascunho de {@link Frequencia} com data de referência explícita
   * para aferição de retroatividade ({@code ERG-00446}).
   */
  public RuleEvaluationResult evaluateFrequenciaLancamento(
      Frequencia draft, LocalDate referenceDate) {
    ErgonRuleEngineFacade.FrequenciaRuleContext ctx =
        buildFrequenciaRuleContext(draft, referenceDate);
    return ruleEngineFacade.evaluateFrequencia(ctx);
  }

  /**
   * Valida o rascunho de {@link Frequencia} em modo <i>fail-closed</i>, lançando
   * {@link ErgonRuleViolationException} (HTTP 422) caso qualquer slot resulte em {@code DENY},
   * {@code INCONCLUSIVE} ou {@code TECHNICAL_ERROR}.
   */
  public RuleEvaluationResult validateFrequenciaLancamento(Frequencia draft) {
    return validateFrequenciaLancamento(draft, null);
  }

  /**
   * Valida o rascunho de {@link Frequencia} com data de referência explícita em modo
   * <i>fail-closed</i>.
   */
  public RuleEvaluationResult validateFrequenciaLancamento(
      Frequencia draft, LocalDate referenceDate) {
    ErgonRuleEngineFacade.FrequenciaRuleContext ctx =
        buildFrequenciaRuleContext(draft, referenceDate);
    return ruleEngineFacade.enforceFrequencia(ctx);
  }

  /** Retorna o diagnóstico estruturado da avaliação determinística de lançamento de frequência. */
  public Map<String, Object> evaluateFrequenciaDiagnostic(
      Frequencia draft, LocalDate referenceDate) {
    Integer empCodigo =
        draft != null && draft.getEmpCodigo() != null ? draft.getEmpCodigo() : tenant.orgao();
    RuleEvaluationResult result = evaluateFrequenciaLancamento(draft, referenceDate);
    return ruleEngineFacade.toDiagnosticPayload(
        ruleEngineFacade.resolveFrequenciaPlan(empCodigo), result);
  }

  /**
   * Constrói o contexto completo de avaliação determinística de {@link Frequencia}, resolvendo o
   * vínculo, a pessoa, a modalidade {@link CodigoFrequencia}, a {@link RegraFrequenciaEstatutaria}
   * vigente ({@code effectiveStatutoryRule}), as ocorrências existentes e os afastamentos ativos.
   */
  public ErgonRuleEngineFacade.FrequenciaRuleContext buildFrequenciaRuleContext(
      Frequencia draft, LocalDate referenceDate) {
    if (draft == null) {
      return new ErgonRuleEngineFacade.FrequenciaRuleContext(
          null, null, null, null, null, List.of(), List.of(), referenceDate);
    }
    Vinculo vinculo = draft.getVinculo();
    if (vinculo == null
        && entityManager != null
        && draft.getNumfunc() != null
        && draft.getNumvinc() != null) {
      vinculo =
          entityManager.find(
              Vinculo.class, new VinculoId(draft.getNumfunc(), draft.getNumvinc()));
    }

    ServidorPessoa pessoa = draft.getPessoa();
    if (pessoa == null && entityManager != null && draft.getNumfunc() != null) {
      pessoa = entityManager.find(ServidorPessoa.class, draft.getNumfunc());
    }

    CodigoFrequencia codigo = draft.getCodigoFrequenciaEntidade();
    if (codigo == null
        && entityManager != null
        && draft.getTipofreq() != null
        && draft.getCodfreq() != null) {
      codigo =
          entityManager.find(
              CodigoFrequencia.class,
              new CodigoFrequenciaId(draft.getTipofreq(), draft.getCodfreq()));
    }

    if (entityManager != null && draft.getTipofreq() != null) {
      if (draft.getTipoFrequenciaEntidade() == null
          || !Objects.equals(draft.getTipoFrequenciaEntidade().getTipo(), draft.getTipofreq())) {
        draft.setTipoFrequenciaEntidade(
            entityManager.find(TipoFrequencia.class, draft.getTipofreq()));
      }
    }

    if (entityManager != null && draft.getDocumentoLegalId() != null) {
      if (draft.getDocumentoLegal() == null
          || !Objects.equals(draft.getDocumentoLegal().getId(), draft.getDocumentoLegalId())) {
        draft.setDocumentoLegal(
            entityManager.find(DocumentoLegal.class, draft.getDocumentoLegalId()));
      }
    } else if (draft.getDocumentoLegalId() == null) {
      draft.setDocumentoLegal(null);
    }

    List<RegraFrequenciaEstatutaria> ruleCandidates = loadStatutoryRuleCandidates(draft);
    RegraFrequenciaEstatutaria effectiveRule =
        ruleEngineFacade
            .resolveEffectiveStatutoryRule(
                ruleCandidates, vinculo, draft.getTipofreq(), draft.getCodfreq(), draft.getDtini())
            .orElse(null);

    List<Frequencia> existingFrequencias = loadExistingFrequencias(draft);
    List<Afastamento> activeAfastamentos = loadActiveAfastamentos(draft);

    return new ErgonRuleEngineFacade.FrequenciaRuleContext(
        draft,
        vinculo,
        pessoa,
        codigo,
        effectiveRule,
        existingFrequencias,
        activeAfastamentos,
        referenceDate);
  }

  private List<RegraFrequenciaEstatutaria> loadStatutoryRuleCandidates(Frequencia draft) {
    if (draft == null || draft.getTipofreq() == null || draft.getCodfreq() == null) {
      return List.of();
    }
    if (regraRepository != null) {
      List<RegraFrequenciaEstatutaria> candidates =
          regraRepository.findByTipofreqAndCodfreq(draft.getTipofreq(), draft.getCodfreq());
      return candidates != null ? candidates : List.of();
    }
    if (entityManager != null) {
      return entityManager
          .createQuery(
              "select r from RegraFrequenciaEstatutaria r where r.tipofreq=:t and r.codfreq=:c",
              RegraFrequenciaEstatutaria.class)
          .setParameter("t", draft.getTipofreq())
          .setParameter("c", draft.getCodfreq())
          .setFlushMode(FlushModeType.COMMIT)
          .getResultList();
    }
    return List.of();
  }

  private List<Frequencia> loadExistingFrequencias(Frequencia draft) {
    if (entityManager == null || draft.getNumfunc() == null || draft.getNumvinc() == null) {
      return List.of();
    }
    return entityManager
        .createQuery(
            "select x from Frequencia x where x.numfunc=:p and x.numvinc=:v and x.situacao<>'CANCELADO'",
            Frequencia.class)
        .setParameter("p", draft.getNumfunc())
        .setParameter("v", draft.getNumvinc())
        .setFlushMode(FlushModeType.COMMIT)
        .getResultList();
  }

  private List<Afastamento> loadActiveAfastamentos(Frequencia draft) {
    if (entityManager == null || draft.getNumfunc() == null || draft.getNumvinc() == null) {
      return List.of();
    }
    return entityManager
        .createQuery(
            "select a from Afastamento a where a.numfunc=:p and a.numvinc=:v and a.situacao not in ('CANCELADO','REJEITADO')",
            Afastamento.class)
        .setParameter("p", draft.getNumfunc())
        .setParameter("v", draft.getNumvinc())
        .setFlushMode(FlushModeType.COMMIT)
        .getResultList();
  }

  @Override
  protected List<CollectionExportField> exportFields() {
    var datePresentation =
        new CollectionExportFieldPresentation(
            "date", "dd/MM/yyyy", null, "pt-BR", "America/Sao_Paulo", null, null, "Não informado");
    var decimalPresentation =
        new CollectionExportFieldPresentation(
            "number", "0.00", null, "pt-BR", null, null, null, "Não informado");
    return List.of(
        field("id", "ID da frequência", "integer"),
        field("empCodigo", "Órgão", "integer"),
        field("numfunc", "Servidor", "integer"),
        field("numvinc", "Vínculo funcional", "integer"),
        field("vinculoMatricula", "Matrícula do vínculo", "string"),
        field("tipofreq", "Tipo de frequência", "string"),
        field("codfreq", "Código de frequência", "integer"),
        field("codigoFrequenciaNome", "Modalidade de frequência", "string"),
        presentedField("dtini", "Data de início", "date", datePresentation),
        presentedField("dtfim", "Data final", "date", datePresentation),
        presentedField("quantidade", "Quantidade apurada", "number", decimalPresentation),
        field("situacao", "Situação", "string"),
        field("obs", "Justificativa", "string"));
  }

  @Override
  protected FrequenciaMapper getResourceMapper() {
    return mapper;
  }

  @Override
  protected FrequenciaFilterDTO emptyFilter() {
    return new FrequenciaFilterDTO();
  }

  @Override
  protected OptionalLong getEntityResourceVersion(Frequencia entity) {
    return OptionalLong.of(entity.getVersion());
  }

  @Override
  public Optional<String> getDatasetVersion() {
    return Optional.empty();
  }

  @Override
  public StatsFieldRegistry getStatsFieldRegistry() {
    return StatsFieldRegistry.builder()
        .labeledGroupByBucket("id", "id", "obs", Set.of(StatsMetric.COUNT, StatsMetric.SUM))
        .categoricalGroupByBucket("situacao", "situacao")
        .categoricalGroupByBucket("tipofreq", "tipofreq")
        .temporalTimeSeriesField("dtini", "dtini")
        .numericHistogramMeasureField("quantidade", "quantidade")
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
  protected void beforeCreate(CreateFrequenciaDTO dto, Frequencia entity) {
    checkOrg(entity.getEmpCodigo());
    validateFrequenciaLancamento(entity);
    if (invariants != null) {
      invariants.frequencia(entity, true);
    }
  }

  @Override
  protected void beforeUpdate(Long id, Frequencia entity, UpdateFrequenciaDTO dto) {
    checkOrg(entity.getEmpCodigo());
  }

  @Override
  @Transactional
  public ResourceRepresentationResult<FrequenciaResponseDTO> update(
      Long id, UpdateFrequenciaDTO dto, ResourceVersionUpdatePrecondition<Long> precondition) {
    if (precondition == null) throw ResourceVersionPreconditionException.required();
    var readOnlyKeys = validateCustomFields(dto, "request");
    Frequencia entity = findEntityById(id);
    entityManager.lock(entity, LockModeType.PESSIMISTIC_WRITE);
    entityManager.refresh(entity);
    precondition.requireMatch(entity.getVersion());
    beforeUpdate(id, entity, dto);
    var old = CustomFieldSupport.snapshotReadOnlyValues(entity, readOnlyKeys);
    mapper.applyUpdate(entity, dto);
    validateFrequenciaLancamento(entity);
    if (invariants != null) {
      invariants.frequencia(entity, false);
    }
    CustomFieldSupport.restoreReadOnlyValues(entity, readOnlyKeys, old);
    var saved = getRepository().save(entity);
    afterUpdate(id, saved, dto);
    entityManager.flush();
    entityManager.refresh(saved);
    return toResourceRepresentation(saved);
  }

  private ResourceRepresentationResult<FrequenciaResponseDTO> command(
      Long id,
      ResourceVersionUpdatePrecondition<Long> precondition,
      Consumer<Frequencia> action) {
    if (precondition == null) throw ResourceVersionPreconditionException.required();
    Frequencia entity = findEntityById(id);
    entityManager.lock(entity, LockModeType.PESSIMISTIC_WRITE);
    entityManager.refresh(entity);
    precondition.requireMatch(entity.getVersion());
    checkOrg(entity.getEmpCodigo());
    action.accept(entity);
    if (!Set.of("CANCELADO", "REJEITADO").contains(entity.getSituacao())) {
      validateFrequenciaLancamento(entity);
      if (invariants != null) {
        invariants.frequencia(entity, false);
      }
    }
    var saved = getRepository().save(entity);
    entityManager.flush();
    entityManager.refresh(saved);
    return toResourceRepresentation(saved);
  }

  @Transactional
  public ResourceRepresentationResult<FrequenciaResponseDTO> atualizarHorario(
      Long id,
      AtualizarHorarioFrequenciaDTO dto,
      ResourceVersionUpdatePrecondition<Long> precondition) {
    return command(
        id,
        precondition,
        (Frequencia f) -> {
          if (dto.wasSupplied("horaEntrada")) f.setHoraEntrada(dto.getHoraEntrada());
          if (dto.wasSupplied("horaSaida")) f.setHoraSaida(dto.getHoraSaida());
          if (dto.wasSupplied("quantidade")) f.setQuantidade(dto.getQuantidade());
        });
  }

  @Transactional
  public ResourceRepresentationResult<FrequenciaResponseDTO> atualizarJustificativa(
      Long id,
      AtualizarJustificativaFrequenciaDTO dto,
      ResourceVersionUpdatePrecondition<Long> precondition) {
    return command(
        id,
        precondition,
        (Frequencia f) -> {
          if (dto.wasSupplied("obs")) f.setObs(dto.getObs());
          if (dto.wasSupplied("documentoLegalId")) f.setDocumentoLegalId(dto.getDocumentoLegalId());
          if (dto.wasSupplied("dtfim")) f.setDtfim(dto.getDtfim());
        });
  }

  @Transactional
  public ResourceRepresentationResult<FrequenciaResponseDTO> homologarFrequencia(
      Long id,
      HomologarFrequenciaDTO dto,
      ResourceVersionUpdatePrecondition<Long> precondition) {
    return command(
        id,
        precondition,
        (Frequencia f) -> {
          if (!"PENDENTE_HOMOLOGACAO".equals(f.getSituacao())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Somente ocorrências de frequência pendentes de homologação podem ser homologadas.");
          }
          if (dto.getDocumentoLegalId() != null) {
            f.setDocumentoLegalId(dto.getDocumentoLegalId());
          }
          if (dto.getObservacoes() != null && !dto.getObservacoes().isBlank()) {
            f.setObs(
                (f.getObs() == null ? "" : f.getObs() + " | ")
                    + "Homologação: "
                    + dto.getObservacoes());
          }
          f.setSituacao("HOMOLOGADO");
        });
  }

  @Transactional
  public ResourceRepresentationResult<FrequenciaResponseDTO> abonarFaltaFrequencia(
      Long id,
      AbonarFaltaFrequenciaDTO dto,
      ResourceVersionUpdatePrecondition<Long> precondition) {
    return command(
        id,
        precondition,
        (Frequencia f) -> {
          if (!Set.of("PENDENTE_HOMOLOGACAO", "HOMOLOGADO").contains(f.getSituacao())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Somente ocorrências pendentes ou homologadas podem receber abono formal.");
          }
          f.setDocumentoLegalId(dto.getDocumentoLegalId());
          f.setObs(
              (f.getObs() == null ? "" : f.getObs() + " | ")
                  + "Abono: "
                  + dto.getJustificativa());
          f.setSituacao("HOMOLOGADO");
        });
  }

  @Transactional
  public ResourceRepresentationResult<FrequenciaResponseDTO> rejeitarFrequencia(
      Long id,
      RejeitarFrequenciaDTO dto,
      ResourceVersionUpdatePrecondition<Long> precondition) {
    return command(
        id,
        precondition,
        (Frequencia f) -> {
          if (!"PENDENTE_HOMOLOGACAO".equals(f.getSituacao())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Somente ocorrências pendentes de homologação podem ser rejeitadas.");
          }
          f.setSituacao("REJEITADO");
          f.setObs(
              (f.getObs() == null ? "" : f.getObs() + " | ")
                  + "Rejeição: "
                  + dto.getMotivoRejeicao());
        });
  }

  @Transactional
  public ResourceRepresentationResult<FrequenciaResponseDTO> cancelarFrequencia(
      Long id,
      CancelarFrequenciaDTO dto,
      ResourceVersionUpdatePrecondition<Long> precondition) {
    return command(
        id,
        precondition,
        (Frequencia f) -> {
          if (!Set.of("PENDENTE_HOMOLOGACAO", "HOMOLOGADO").contains(f.getSituacao())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Somente ocorrências pendentes ou homologadas podem ser canceladas.");
          }
          f.setSituacao("CANCELADO");
          f.setObs(
              (f.getObs() == null ? "" : f.getObs() + " | ")
                  + "Cancelamento: "
                  + dto.getMotivoCancelamento());
        });
  }

  private void checkOrg(Integer orgao) {
    if (!Objects.equals(orgao, tenant.orgao())) {
      throw new ResponseStatusException(
          HttpStatus.UNPROCESSABLE_ENTITY, "Órgão da frequência diverge de X-Tenant.");
    }
  }
}
