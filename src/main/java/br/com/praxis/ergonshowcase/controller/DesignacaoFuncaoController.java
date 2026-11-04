package br.com.praxis.ergonshowcase.controller;

import org.praxisplatform.uischema.fact.ResourceFact;
import org.praxisplatform.uischema.stats.dto.GroupByStatsRequest;
import org.praxisplatform.uischema.stats.dto.GroupByStatsResponse;
import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.bulk.ErgonBulkOperations;
import br.com.praxis.ergonshowcase.bulk.ErgonBulkDomain.Resource;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.dto.ChefiaDeterminationDTO.*;
import br.com.praxis.ergonshowcase.service.ErgonDeterminationService;
import br.com.praxis.ergonshowcase.service.DesignacaoFuncaoService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.praxisplatform.uischema.action.*;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.bulk.*;
import org.praxisplatform.uischema.controller.base.AbstractBulkGovernedDuplicateDraftResourceController;
import org.praxisplatform.uischema.rest.response.RestApiResponse;
import org.praxisplatform.uischema.surface.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@BulkResourceOperations(proposalOperationId = "ergon.designacoes.bulk.proposal",
    proposalResultsOperationId = "ergon.designacoes.bulk.proposal-results",
    executionOperationId = "ergon.designacoes.bulk.execution",
    executionResultsOperationId = "ergon.designacoes.bulk.results",
    cancelOperationId = "ergon.designacoes.bulk.cancel",
    updateSourceOperationId = "updateErgonDesignacaoFuncao",
    protectedUpdateFields = {"empCodigo", "titularId", "funcaoId", "vagaFuncaoId", "situacao",
        "dtini", "dtfim", "dtExercicio", "atoDispensaId", "motivoDispensa"})
@ResourceFact(key = "diasVigencia", resourceKey = ApiPaths.KEY_DESIGNACOES, label = "Dias de vigência",
    description = "Dias civis inclusivos entre início e fim registrados. Vigências abertas não têm duração fechada.",
    dataType = "integer", icon = "date_range", tone = "info",
    sourceKind = "computed")
@ApiResource(value = ApiPaths.DESIGNACOES, resourceKey = ApiPaths.KEY_DESIGNACOES, title = "Designações",
    description = "Atos individuais de designação, exercício e dispensa em função de confiança, incluindo opção remuneratória e ordem de substitutos.", icon = "assignment_ind",
    identity = @ResourceIdentity(keyField = "id", titleField = "descricao", subtitleField = "titularNome",
        metadataFields = {"empCodigo", "dtini", "dtfim"}),
    quickFilters = {
        @QuickFilter(id = "exercicio", label = "Em exercício", filter = "{\"situacao\":\"EM_EXERCICIO\"}"),
        @QuickFilter(id = "pendentes", label = "Aguardando exercício", filter = "{\"situacao\":\"DESIGNADA\"}"),
        @QuickFilter(id = "dispensas", label = "Dispensadas", filter = "{\"situacao\":\"DISPENSADA\"}")})
@ApiGroup(value = "ergon", title = "Administração de pessoal",
    description = "Assentamentos funcionais de órgãos públicos.", icon = "groups")
public class DesignacaoFuncaoController extends AbstractBulkGovernedDuplicateDraftResourceController<DesignacaoFuncaoResponseDTO, Long, DesignacaoFuncaoFilterDTO, CreateDesignacaoFuncaoDTO, UpdateDesignacaoFuncaoDTO, DesignacaoFuncaoDuplicateDraftDTO> {
  private static final String HOMOLOGAR = "homologar-designacoes-lote";
  private final DesignacaoFuncaoService service;
  private final ErgonDeterminationService determinations;
  private final ErgonBulkOperations bulk;

  public DesignacaoFuncaoController(DesignacaoFuncaoService service, ErgonBulkOperations bulk, ErgonDeterminationService determinations) {
    this.service = service;
    this.determinations = determinations;
    this.bulk = bulk;
  }

  @Override
  protected DesignacaoFuncaoService getService() { return service; }

  @Override
  protected Long getResponseId(DesignacaoFuncaoResponseDTO dto) { return dto.getId(); }

  @Override
  @SuppressWarnings("unchecked")
  protected JpaResourceBulkEngine<Long, DesignacaoFuncaoFilterDTO> getBulkEngine() {
    return bulk.engine(Resource.DESIGNACOES);
  }

  @Override
  @PutMapping("/{id}")
  @BulkResourceOperation(BulkResourceOperation.Role.UPDATE_SOURCE)
  @Operation(operationId = "updateErgonDesignacaoFuncao", summary = "Retificar designação",
      description = "Atualiza o ato no órgão corrente mediante If-Match e sincroniza a titularidade das vagas afetadas.")
  public ResponseEntity<RestApiResponse<DesignacaoFuncaoResponseDTO>> update(
      @PathVariable Long id, @Valid @RequestBody UpdateDesignacaoFuncaoDTO input) {
    return super.update(id, input);
  }

  @Override
  @PostMapping("/bulk/update/evaluation")
  @Operation(operationId="administracao-pessoal.designacoes.bulk.update.evaluation", summary="Avaliar edição uniforme",description="Avalia seleção, versões e alterações sem modificar o cadastro; produz uma proposta protegida.")
  @BulkResourceOperation(BulkResourceOperation.Role.UNIFORM_EVALUATION)
  public ResponseEntity<RestApiResponse<BulkProposal>> evaluateBulkUpdate(@RequestBody BulkUniformEvaluationRequest<?, DesignacaoFuncaoFilterDTO> request) {
    return super.evaluateBulkUpdate(request);
  }
  @Override
  @PostMapping("/bulk/update-items/evaluation")
  @Operation(operationId="administracao-pessoal.designacoes.bulk.update-items.evaluation",summary="Avaliar edições por registro",description="Avalia deltas distintos e a versão esperada de cada registro, com impedimentos individualizados.")
  @BulkResourceOperation(BulkResourceOperation.Role.ITEM_EVALUATION)
  public ResponseEntity<RestApiResponse<BulkProposal>> evaluateBulkUpdateItems(@RequestBody BulkItemEvaluationRequest<?> request) {
    return super.evaluateBulkUpdateItems(request);
  }
  @Override
  @PostMapping("/bulk/update")
  @BulkOperation(mode=BulkMode.UNIFORM_UPDATE,evaluationOperationId="administracao-pessoal.designacoes.bulk.update.evaluation",atomicity=ActionCollectionAtomicity.PER_ITEM)
  @Operation(operationId="administracao-pessoal.designacoes.bulk.update",summary="Confirmar edição uniforme",description="Confirma uma proposta avaliada; cada registro é revalidado e possui recibo durável, sem repetir efeitos no replay.")
  @BulkResourceOperation(BulkResourceOperation.Role.UNIFORM_CONFIRMATION)
  public ResponseEntity<RestApiResponse<BulkExecution>> confirmBulkUpdate(@RequestBody BulkConfirmationRequest request, @RequestHeader("Idempotency-Key") String key) {
    return super.confirmBulkUpdate(request, key);
  }
  @Override
  @PostMapping("/bulk/update-items")
  @BulkOperation(mode=BulkMode.PER_ITEM_UPDATE,evaluationOperationId="administracao-pessoal.designacoes.bulk.update-items.evaluation",atomicity=ActionCollectionAtomicity.PER_ITEM)
  @Operation(operationId="administracao-pessoal.designacoes.bulk.update-items",summary="Confirmar edições por registro",description="Aplica os deltas individualizados preservados na proposta, com versão e resultado próprios por registro.")
  @BulkResourceOperation(BulkResourceOperation.Role.ITEM_CONFIRMATION)
  public ResponseEntity<RestApiResponse<BulkExecution>> confirmBulkUpdateItems(@RequestBody BulkConfirmationRequest request, @RequestHeader("Idempotency-Key") String key) {
    return super.confirmBulkUpdateItems(request, key);
  }

  @PostMapping("/bulk/" + HOMOLOGAR + "/evaluation")
  @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(operationId = "ergon.designacoes.homologar-designacoes-lote.evaluation", summary = "Avaliar homologação de designações",
      description = "Avalia versões e disponibilidade das vagas e retém uma proposta sem iniciar exercício.")
  public ResponseEntity<RestApiResponse<BulkProposal>> evaluateHomologacao(
      @RequestBody BulkCommandEvaluationRequest<HomologarDesignacaoLoteDTO, ?, DesignacaoFuncaoFilterDTO> request) {
    return evaluateBulkDomainCommand(HOMOLOGAR, request);
  }

  @PostMapping("/actions/" + HOMOLOGAR)
  @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @WorkflowAction(id = HOMOLOGAR, title = "Homologar designações em lote", scope = ActionScope.COLLECTION,
      requiredAuthorities = {"ERGON_WRITE"}, confirmationRequired = true, idempotencyKey = ActionRequirement.REQUIRED,
      atomicity = ActionCollectionAtomicity.PER_ITEM, outcomeMode = ActionOutcomeMode.PER_ITEM, maxSelection = 200)
  @BulkOperation(mode = BulkMode.DOMAIN_COMMAND, evaluationOperationId = "ergon.designacoes.homologar-designacoes-lote.evaluation",
      atomicity = ActionCollectionAtomicity.PER_ITEM)
  @Operation(operationId = "ergon.designacoes.homologar-designacoes-lote", summary = "Confirmar homologação de designações",
      description = "Inicia exercício e atualiza a vaga em uma transação por ato aprovado na proposta.")
  public ResponseEntity<RestApiResponse<BulkExecution>> confirmHomologacao(
      @RequestBody BulkConfirmationRequest request, @RequestHeader("Idempotency-Key") String key) {
    return confirmBulkDomainCommand(HOMOLOGAR, request, key);
  }

  @GetMapping("/{id}/detail")
  @PreAuthorize("hasAuthority('ERGON_READ')")
  @Operation(operationId = "detalharDesignacaoFuncao", summary = "Consultar ficha de designações",
      description = "Atos individuais de designação, exercício e dispensa em função de confiança, incluindo opção remuneratória e ordem de substitutos.")
  @UiSurface(id = "detail", kind = SurfaceKind.READ_PROJECTION, scope = SurfaceScope.ITEM, title = "Designações",
      description = "Atos individuais de designação, exercício e dispensa em função de confiança, incluindo opção remuneratória e ordem de substitutos.", requiredAuthorities = {"ERGON_READ"})
  public ResponseEntity<DesignacaoFuncaoResponseDTO> detail(@PathVariable Long id) {
    return withResourceVersion(ResponseEntity.ok(), id, service.findById(id));
  }

  @Override
  @PostMapping("/stats/group-by")
  @Operation(operationId = "statsErgonDesignacaoFuncao", summary = "Atos por situação de exercício",
      description = "Conta somente registros do órgão efetivo dentro do recorte solicitado. Cada grupo reúne múltiplos registros e não representa uma ficha individual.")
  @UiAnalytics(projections = {
      @AnalyticsProjection(id = "designacoes-por-situacao", intent = AnalyticsIntent.COMPOSITION,
          sourceOperation = AnalyticsOperation.GROUP_BY,
          primaryDimension = @AnalyticsDimensionBinding(field = "situacao"),
          primaryMetrics = @AnalyticsMetricBinding(field = "count", aggregation = "count", label = "Atos por situação de exercício")),
      @AnalyticsProjection(id = "vigencia-de-designacoes", intent = AnalyticsIntent.TREND,
          sourceOperation = AnalyticsOperation.TIMESERIES,
          primaryDimension = @AnalyticsDimensionBinding(field = "dtini"),
          defaultGranularity = AnalyticsGranularity.MONTH, defaultTimezone = "America/Sao_Paulo",
          primaryMetrics = @AnalyticsMetricBinding(field = "count", aggregation = "count", label = "Designações por início de vigência")),
      @AnalyticsProjection(id = "designacoes-por-situacao-comparativo", intent = AnalyticsIntent.COMPARISON,
          sourceOperation = AnalyticsOperation.COMPARISON,
          primaryDimension = @AnalyticsDimensionBinding(field = "situacao"),
          comparisonPeriod = @AnalyticsComparisonPeriodBinding(field = "dtini", timezone = "America/Sao_Paulo"),
          primaryMetrics = @AnalyticsMetricBinding(field = "count", aggregation = "count", label = "Atos por situação de exercício"))
  })
  public ResponseEntity<RestApiResponse<GroupByStatsResponse>> groupByStats(
      @RequestBody GroupByStatsRequest<DesignacaoFuncaoFilterDTO> request) {
    return super.groupByStats(request);
  }

  @Override
  @PostMapping
  @Operation(operationId = "createErgonDesignacaoFuncao", summary = "Incluir registro",
      description = "Inclui o registro no órgão efetivo com validação de referências e das regras do cadastro.")
  public ResponseEntity<RestApiResponse<DesignacaoFuncaoResponseDTO>> create(@Valid @RequestBody CreateDesignacaoFuncaoDTO input) {
    return super.create(input);
  }

  @PostMapping("/determinations/previa")
  @PreAuthorize("hasAuthority('ERGON_READ')")
  @Operation(operationId = "determineErgonDesignacaoFuncaoPrevia", summary = "Situação da vaga",
      description = "Consulta a situação cadastrada da vaga no órgão efetivo, sem reservá-la.")
  public ResponseEntity<DesignacaoPreview> determinePrevia(@Valid @RequestBody DesignacaoDraft draft) {
    return ResponseEntity.ok(determinations.previaDesignacao(draft));
  }
}
