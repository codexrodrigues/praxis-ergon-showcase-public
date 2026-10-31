package br.com.praxis.ergonshowcase.service;

import br.com.praxis.ergonshowcase.config.TenantContext;
import br.com.praxis.ergonshowcase.domain.*;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.mapper.AfastamentoMapper;
import br.com.praxis.ergonshowcase.repository.AfastamentoRepository;
import br.com.praxis.ergonshowcase.repository.RegraFrequenciaEstatutariaRepository;
import br.com.praxis.ergonshowcase.rules.ErgonRuleEngineFacade;
import br.com.praxis.ergonshowcase.rules.ErgonRuleViolationException;
import jakarta.persistence.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Consumer;
import org.praxisplatform.rules.contract.RuleEvaluationResult;
import org.praxisplatform.uischema.concurrency.*;
import org.praxisplatform.uischema.customfields.CustomFieldSupport;
import org.praxisplatform.uischema.exporting.CollectionExportField;
import org.praxisplatform.uischema.exporting.CollectionExportFieldPresentation;
import org.praxisplatform.uischema.service.base.*;
import org.praxisplatform.uischema.stats.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Operações transacionais, invariantes e workflow de homologação de licenças e afastamentos. */
@Service
public class AfastamentoService
    extends ErgonResourceService<
        Afastamento,
        AfastamentoResponseDTO,
        Long,
        AfastamentoFilterDTO,
        CreateAfastamentoDTO,
        UpdateAfastamentoDTO>
    implements VersionedCreateUpdateResourceService<
        AfastamentoResponseDTO,
        Long,
        AfastamentoFilterDTO,
        CreateAfastamentoDTO,
        UpdateAfastamentoDTO> {

  @Autowired(required = false)
  private ErgonInvariants invariants;

  @PersistenceContext protected EntityManager entityManager;
  protected final AfastamentoMapper mapper;
  private final TenantContext tenant;
  private final ErgonRuleEngineFacade ruleEngineFacade;
  private final RegraFrequenciaEstatutariaRepository regraRepository;

  public AfastamentoService(
      AfastamentoRepository repository, AfastamentoMapper mapper, TenantContext tenant) {
    this(repository, mapper, tenant, new ErgonRuleEngineFacade(), null);
  }

  @Autowired
  public AfastamentoService(
      AfastamentoRepository repository,
      AfastamentoMapper mapper,
      TenantContext tenant,
      ErgonRuleEngineFacade ruleEngineFacade,
      RegraFrequenciaEstatutariaRepository regraRepository) {
    super(repository, Afastamento.class, "id");
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
   * Avalia deterministicamente o rascunho de {@link Afastamento} contra o
   * {@link br.com.praxis.ergonshowcase.rules.AfastamentoConcessaoRuleSet} do órgão, resolvendo a
   * {@link RegraFrequenciaEstatutaria} vigente do servidor como provedora dinâmica de fatos
   * ({@code effectiveStatutoryRule}).
   */
  public RuleEvaluationResult evaluateAfastamentoConcessao(Afastamento draft) {
    return evaluateAfastamentoConcessao(draft, false);
  }

  /**
   * Avalia deterministicamente o rascunho de {@link Afastamento} informando se se trata de
   * prorrogação <i>in-place</i>.
   */
  public RuleEvaluationResult evaluateAfastamentoConcessao(
      Afastamento draft, boolean inPlaceProrrogacao) {
    ErgonRuleEngineFacade.AfastamentoRuleContext ctx =
        buildAfastamentoRuleContext(draft, inPlaceProrrogacao);
    return ruleEngineFacade.evaluateAfastamento(ctx);
  }

  /**
   * Valida o rascunho de {@link Afastamento} em modo <i>fail-closed</i>, lançando
   * {@link ErgonRuleViolationException} (HTTP 422) caso qualquer slot resulte em {@code DENY},
   * {@code INCONCLUSIVE} ou {@code TECHNICAL_ERROR}.
   */
  public RuleEvaluationResult validateAfastamentoConcessao(Afastamento draft) {
    return validateAfastamentoConcessao(draft, false);
  }

  /**
   * Valida o rascunho de {@link Afastamento} em modo <i>fail-closed</i> informando se se trata de
   * prorrogação <i>in-place</i>.
   */
  public RuleEvaluationResult validateAfastamentoConcessao(
      Afastamento draft, boolean inPlaceProrrogacao) {
    ErgonRuleEngineFacade.AfastamentoRuleContext ctx =
        buildAfastamentoRuleContext(draft, inPlaceProrrogacao);
    return ruleEngineFacade.enforceAfastamento(ctx);
  }

  /** Retorna o diagnóstico estruturado da avaliação determinística de concessão de afastamento. */
  public Map<String, Object> evaluateAfastamentoDiagnostic(Afastamento draft) {
    Integer empCodigo =
        draft != null && draft.getEmpCodigo() != null ? draft.getEmpCodigo() : tenant.orgao();
    RuleEvaluationResult result = evaluateAfastamentoConcessao(draft);
    return ruleEngineFacade.toDiagnosticPayload(
        ruleEngineFacade.resolveAfastamentoPlan(empCodigo), result);
  }

  /**
   * Constrói o contexto completo de avaliação determinística de {@link Afastamento}, resolvendo o
   * vínculo, a pessoa, a modalidade {@link CodigoFrequencia}, a {@link RegraFrequenciaEstatutaria}
   * vigente ({@code effectiveStatutoryRule}), os afastamentos existentes, as frequências ativas, o
   * afastamento de origem e o vínculo do substituto.
   */
  public ErgonRuleEngineFacade.AfastamentoRuleContext buildAfastamentoRuleContext(
      Afastamento draft, boolean inPlaceProrrogacao) {
    if (draft == null) {
      return new ErgonRuleEngineFacade.AfastamentoRuleContext(
          null, null, null, null, null, List.of(), List.of(), null, null, inPlaceProrrogacao);
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

    List<Afastamento> existingAfastamentos = loadExistingAfastamentos(draft);
    List<Frequencia> activeFrequencias = loadActiveFrequencias(draft);

    Afastamento origem = draft.getAfastamentoOrigem();
    if (origem == null && entityManager != null && draft.getAfastamentoOrigemId() != null) {
      origem = entityManager.find(Afastamento.class, draft.getAfastamentoOrigemId());
    }

    Vinculo substitutoVinculo = null;
    if (draft.getNumfuncSubstituto() != null && draft.getNumvincSubstituto() != null) {
      if (entityManager != null) {
        substitutoVinculo =
            entityManager.find(
                Vinculo.class,
                new VinculoId(draft.getNumfuncSubstituto(), draft.getNumvincSubstituto()));
      } else if (draft.getPessoaSubstituta() != null) {
        substitutoVinculo = new Vinculo();
        substitutoVinculo.setId(
            new VinculoId(draft.getNumfuncSubstituto(), draft.getNumvincSubstituto()));
        substitutoVinculo.setEmpCodigo(draft.getEmpCodigo());
        substitutoVinculo.setSituacao("ATIVO");
      }
    }

    return new ErgonRuleEngineFacade.AfastamentoRuleContext(
        draft,
        vinculo,
        pessoa,
        codigo,
        effectiveRule,
        existingAfastamentos,
        activeFrequencias,
        origem,
        substitutoVinculo,
        inPlaceProrrogacao);
  }

  private List<RegraFrequenciaEstatutaria> loadStatutoryRuleCandidates(Afastamento draft) {
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

  private List<Afastamento> loadExistingAfastamentos(Afastamento draft) {
    if (entityManager == null || draft.getNumfunc() == null || draft.getNumvinc() == null) {
      return List.of();
    }
    return entityManager
        .createQuery(
            "select x from Afastamento x where x.numfunc=:f and x.numvinc=:v and x.situacao not in ('CANCELADO','REJEITADO')",
            Afastamento.class)
        .setParameter("f", draft.getNumfunc())
        .setParameter("v", draft.getNumvinc())
        .setFlushMode(FlushModeType.COMMIT)
        .getResultList();
  }

  private List<Frequencia> loadActiveFrequencias(Afastamento draft) {
    if (entityManager == null || draft.getNumfunc() == null || draft.getNumvinc() == null) {
      return List.of();
    }
    return entityManager
        .createQuery(
            "select f from Frequencia f where f.numfunc=:f and f.numvinc=:v and f.situacao not in ('CANCELADO','REJEITADO')",
            Frequencia.class)
        .setParameter("f", draft.getNumfunc())
        .setParameter("v", draft.getNumvinc())
        .setFlushMode(FlushModeType.COMMIT)
        .getResultList();
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
        field("id", "Código do afastamento", "integer"),
        field("empCodigo", "Órgão", "integer"),
        field("numfunc", "Servidor afastado", "integer"),
        field("numvinc", "Sequencial do vínculo", "integer"),
        field("vinculoMatricula", "Matrícula do vínculo", "string"),
        field("tipofreq", "Tipo de frequência", "string"),
        field("codfreq", "Código da licença", "integer"),
        field("codigoFrequenciaNome", "Modalidade de licença", "string"),
        presentedField("dtini", "Início do afastamento", "date", datePresentation),
        presentedField("dtprevfim", "Término previsto", "date", datePresentation),
        presentedField("dtfim", "Término efetivo", "date", datePresentation),
        presentedField("quantidadeDias", "Duração (dias)", "integer", intPresentation),
        field("situacao", "Situação", "string"),
        presentedField("prorrogacao", "É prorrogação", "boolean", boolPresentation));
  }

  @Override
  protected AfastamentoMapper getResourceMapper() {
    return mapper;
  }

  @Override
  protected AfastamentoFilterDTO emptyFilter() {
    return new AfastamentoFilterDTO();
  }

  @Override
  protected OptionalLong getEntityResourceVersion(Afastamento entity) {
    return OptionalLong.of(entity.getVersion());
  }

  @Override
  public Optional<String> getDatasetVersion() {
    return Optional.empty();
  }

  @Override
  public StatsFieldRegistry getStatsFieldRegistry() {
    return StatsFieldRegistry.builder()
        .labeledGroupByBucket("id", "id", "motivo", Set.of(StatsMetric.COUNT, StatsMetric.SUM))
        .categoricalGroupByBucket("situacao", "situacao")
        .categoricalGroupByBucket("tipofreq", "tipofreq")
        .temporalTimeSeriesField("dtini", "dtini")
        .numericHistogramMeasureField("quantidadeDias", "quantidadeDias")
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
  protected void beforeCreate(CreateAfastamentoDTO dto, Afastamento entity) {
    checkOrg(entity.getEmpCodigo());
    validateAfastamentoConcessao(entity, false);
    if (invariants != null) {
      invariants.afastamento(entity, true);
    }
  }

  @Override
  protected void beforeUpdate(Long id, Afastamento entity, UpdateAfastamentoDTO dto) {
    checkOrg(entity.getEmpCodigo());
  }

  @Override
  @Transactional
  public ResourceRepresentationResult<AfastamentoResponseDTO> update(
      Long id, UpdateAfastamentoDTO dto, ResourceVersionUpdatePrecondition<Long> precondition) {
    if (precondition == null) throw ResourceVersionPreconditionException.required();
    var readOnlyKeys = validateCustomFields(dto, "request");
    Afastamento entity = findEntityById(id);
    entityManager.lock(entity, LockModeType.PESSIMISTIC_WRITE);
    entityManager.refresh(entity);
    precondition.requireMatch(entity.getVersion());
    beforeUpdate(id, entity, dto);
    var old = CustomFieldSupport.snapshotReadOnlyValues(entity, readOnlyKeys);
    mapper.applyUpdate(entity, dto);
    validateAfastamentoConcessao(entity, false);
    if (invariants != null) {
      invariants.afastamento(entity, false);
    }
    CustomFieldSupport.restoreReadOnlyValues(entity, readOnlyKeys, old);
    var saved = getRepository().save(entity);
    afterUpdate(id, saved, dto);
    entityManager.flush();
    entityManager.refresh(saved);
    return toResourceRepresentation(saved);
  }

  private ResourceRepresentationResult<AfastamentoResponseDTO> command(
      Long id,
      ResourceVersionUpdatePrecondition<Long> precondition,
      Consumer<Afastamento> action) {
    return command(id, precondition, false, action);
  }

  private ResourceRepresentationResult<AfastamentoResponseDTO> command(
      Long id,
      ResourceVersionUpdatePrecondition<Long> precondition,
      boolean inPlaceProrrogacao,
      Consumer<Afastamento> action) {
    if (precondition == null) throw ResourceVersionPreconditionException.required();
    Afastamento entity = findEntityById(id);
    entityManager.lock(entity, LockModeType.PESSIMISTIC_WRITE);
    entityManager.refresh(entity);
    precondition.requireMatch(entity.getVersion());
    checkOrg(entity.getEmpCodigo());
    action.accept(entity);
    if (!Set.of("CANCELADO", "REJEITADO").contains(entity.getSituacao())) {
      validateAfastamentoConcessao(entity, inPlaceProrrogacao);
      if (invariants != null && !inPlaceProrrogacao) {
        invariants.afastamento(entity, false);
      }
    }
    var saved = getRepository().save(entity);
    entityManager.flush();
    entityManager.refresh(saved);
    return toResourceRepresentation(saved);
  }

  @Transactional
  public ResourceRepresentationResult<AfastamentoResponseDTO> atualizarSubstituto(
      Long id,
      AtualizarSubstitutoAfastamentoDTO dto,
      ResourceVersionUpdatePrecondition<Long> precondition) {
    return command(
        id,
        precondition,
        (Afastamento a) -> {
          if (dto.wasSupplied("numfuncSubstituto"))
            a.setNumfuncSubstituto(dto.getNumfuncSubstituto());
          if (dto.wasSupplied("numvincSubstituto"))
            a.setNumvincSubstituto(dto.getNumvincSubstituto());
          if (dto.wasSupplied("documentoLegalId"))
            a.setDocumentoLegalId(dto.getDocumentoLegalId());
        });
  }

  @Transactional
  public ResourceRepresentationResult<AfastamentoResponseDTO> atualizarAtosPublicacao(
      Long id,
      AtualizarAtosPublicacaoAfastamentoDTO dto,
      ResourceVersionUpdatePrecondition<Long> precondition) {
    return command(
        id,
        precondition,
        (Afastamento a) -> {
          if (dto.wasSupplied("documentoLegalId"))
            a.setDocumentoLegalId(dto.getDocumentoLegalId());
          if (dto.wasSupplied("atoPublicacaoId"))
            a.setAtoPublicacaoId(dto.getAtoPublicacaoId());
          if (dto.wasSupplied("prorrogacao") && dto.getProrrogacao() != null)
            a.setProrrogacao(dto.getProrrogacao());
          if (dto.wasSupplied("afastamentoOrigemId"))
            a.setAfastamentoOrigemId(dto.getAfastamentoOrigemId());
        });
  }

  @Transactional
  public ResourceRepresentationResult<AfastamentoResponseDTO> homologarAfastamento(
      Long id,
      HomologarAfastamentoDTO dto,
      ResourceVersionUpdatePrecondition<Long> precondition) {
    return command(
        id,
        precondition,
        (Afastamento a) -> {
          if (!"PENDENTE_HOMOLOGACAO".equals(a.getSituacao())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Somente afastamentos pendentes de homologação podem ser homologados.");
          }
          a.setDocumentoLegalId(dto.documentoLegalId());
          if (dto.atoPublicacaoId() != null) {
            a.setAtoPublicacaoId(dto.atoPublicacaoId());
          }
          if (dto.dtprevfim() != null) {
            a.setDtprevfim(dto.dtprevfim());
            if (!dto.dtprevfim().isBefore(a.getDtini())) {
              a.setQuantidadeDias(
                  Math.toIntExact(ChronoUnit.DAYS.between(a.getDtini(), dto.dtprevfim()) + 1));
            }
          }
          a.setSituacao("HOMOLOGADO");
          Vinculo v =
              entityManager.find(
                  Vinculo.class,
                  new VinculoId(a.getNumfunc(), a.getNumvinc()),
                  LockModeType.PESSIMISTIC_WRITE);
          if (v != null && a.getDtfim() == null && "ATIVO".equals(v.getSituacao())) {
            v.setSituacao("AFASTADO");
            v.setDocumentoLegalId(dto.documentoLegalId());
          }
        });
  }

  @Transactional
  public ResourceRepresentationResult<AfastamentoResponseDTO> rejeitarAfastamento(
      Long id,
      RejeitarAfastamentoDTO dto,
      ResourceVersionUpdatePrecondition<Long> precondition) {
    return command(
        id,
        precondition,
        (Afastamento a) -> {
          if (!"PENDENTE_HOMOLOGACAO".equals(a.getSituacao())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Somente afastamentos pendentes de homologação podem ser rejeitados.");
          }
          a.setSituacao("REJEITADO");
          a.setMotivo(
              (a.getMotivo() == null ? "" : a.getMotivo() + " | ")
                  + "Indeferimento: "
                  + dto.motivoRejeicao());
          if (dto.documentoLegalId() != null) {
            a.setDocumentoLegalId(dto.documentoLegalId());
          }
        });
  }

  @Transactional
  public ResourceRepresentationResult<AfastamentoResponseDTO> prorrogarAfastamento(
      Long id,
      ProrrogarAfastamentoDTO dto,
      ResourceVersionUpdatePrecondition<Long> precondition) {
    return command(
        id,
        precondition,
        true,
        (Afastamento a) -> {
          if (!"HOMOLOGADO".equals(a.getSituacao()) || a.getDtfim() != null) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "A prorrogação exige um afastamento homologado e ainda em aberto.");
          }
          ErgonInvariants.chronology(
              a.getDtini(),
              dto.novaDataPrevisaoFim(),
              "A nova data de término previsto não pode anteceder a data de início do afastamento.");
          if (a.getDtprevfim() != null && !dto.novaDataPrevisaoFim().isAfter(a.getDtprevfim())) {
            ErgonInvariants.fail(
                "A nova data de término previsto deve ser posterior à previsão atual.");
          }
          a.setDtprevfim(dto.novaDataPrevisaoFim());
          a.setQuantidadeDias(
              Math.toIntExact(
                  ChronoUnit.DAYS.between(a.getDtini(), dto.novaDataPrevisaoFim()) + 1));
          a.setProrrogacao(true);
          a.setDocumentoLegalId(dto.documentoLegalId());
          if (dto.motivoComplementar() != null && !dto.motivoComplementar().isBlank()) {
            a.setMotivo(
                (a.getMotivo() == null ? "" : a.getMotivo() + " | ")
                    + "Prorrogação: "
                    + dto.motivoComplementar());
          }
        });
  }

  @Transactional
  public ResourceRepresentationResult<AfastamentoResponseDTO> encerrarAfastamento(
      Long id,
      EncerrarAfastamentoItemDTO dto,
      ResourceVersionUpdatePrecondition<Long> precondition) {
    return command(
        id,
        precondition,
        (Afastamento a) -> {
          if (!"HOMOLOGADO".equals(a.getSituacao()) || a.getDtfim() != null) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "O encerramento exige um afastamento homologado e ainda sem término efetivo.");
          }
          ErgonInvariants.chronology(
              a.getDtini(),
              dto.dtfim(),
              "A data de término efetivo não pode anteceder o início do afastamento.");
          a.setDtfim(dto.dtfim());
          a.setQuantidadeDias(
              Math.toIntExact(ChronoUnit.DAYS.between(a.getDtini(), dto.dtfim()) + 1));
          a.setDocumentoLegalId(dto.documentoLegalId());
          if (dto.atoPublicacaoId() != null) {
            a.setAtoPublicacaoId(dto.atoPublicacaoId());
          }
          reconciliarSituacaoVinculoAoEncerrar(a, dto.documentoLegalId());
        });
  }

  @Transactional
  public ResourceRepresentationResult<AfastamentoResponseDTO> cancelarAfastamento(
      Long id,
      CancelarAfastamentoDTO dto,
      ResourceVersionUpdatePrecondition<Long> precondition) {
    return command(
        id,
        precondition,
        (Afastamento a) -> {
          if (!Set.of("PENDENTE_HOMOLOGACAO", "HOMOLOGADO").contains(a.getSituacao())
              || a.getDtfim() != null) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Somente afastamentos pendentes ou homologados em aberto podem ser cancelados.");
          }
          a.setSituacao("CANCELADO");
          a.setDocumentoLegalId(dto.documentoLegalId());
          a.setMotivo(
              (a.getMotivo() == null ? "" : a.getMotivo() + " | ")
                  + "Cancelado: "
                  + dto.motivoCancelamento());
          reconciliarSituacaoVinculoAoEncerrar(a, dto.documentoLegalId());
        });
  }

  private void reconciliarSituacaoVinculoAoEncerrar(Afastamento a, Long documentoLegalId) {
    long outrosAbertos =
        entityManager
            .createQuery(
                "select count(x) from Afastamento x where x.numfunc=:p and x.numvinc=:v and x.id<>:id and x.situacao='HOMOLOGADO' and x.dtfim is null",
                Long.class)
            .setParameter("p", a.getNumfunc())
            .setParameter("v", a.getNumvinc())
            .setParameter("id", a.getId())
            .setFlushMode(FlushModeType.COMMIT)
            .getSingleResult();
    if (outrosAbertos == 0) {
      Vinculo v =
          entityManager.find(
              Vinculo.class,
              new VinculoId(a.getNumfunc(), a.getNumvinc()),
              LockModeType.PESSIMISTIC_WRITE);
      if (v != null && "AFASTADO".equals(v.getSituacao())) {
        v.setSituacao("ATIVO");
        if (documentoLegalId != null) {
          v.setDocumentoLegalId(documentoLegalId);
        }
      }
    }
  }

  private void checkOrg(Integer orgao) {
    if (!Objects.equals(orgao, tenant.orgao())) {
      throw new ResponseStatusException(
          HttpStatus.UNPROCESSABLE_ENTITY, "Órgão do afastamento diverge de X-Tenant.");
    }
  }
}
