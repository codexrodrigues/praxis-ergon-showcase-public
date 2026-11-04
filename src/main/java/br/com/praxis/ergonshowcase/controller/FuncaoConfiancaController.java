package br.com.praxis.ergonshowcase.controller;

import org.praxisplatform.uischema.fact.ResourceFact;
import org.praxisplatform.uischema.stats.dto.GroupByStatsRequest;
import org.praxisplatform.uischema.stats.dto.GroupByStatsResponse;
import org.praxisplatform.uischema.rest.response.RestApiResponse;
import br.com.praxis.ergonshowcase.bulk.ErgonBulkDomain.Resource;
import br.com.praxis.ergonshowcase.bulk.ErgonBulkOperations;
import br.com.praxis.ergonshowcase.constants.ApiPaths;
import jakarta.validation.Valid;
import org.praxisplatform.uischema.action.ActionCollectionAtomicity;
import org.praxisplatform.uischema.bulk.*;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.dto.ChefiaDeterminationDTO.*;
import br.com.praxis.ergonshowcase.service.ErgonDeterminationService;
import br.com.praxis.ergonshowcase.service.FuncaoConfiancaService;
import io.swagger.v3.oas.annotations.Operation;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.controller.base.AbstractBulkGovernedDuplicateDraftResourceController;
import org.praxisplatform.uischema.surface.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

@ResourceFact(key = "permiteSubstituicao", resourceKey = ApiPaths.KEY_FUNCOES, label = "Admite substituição",
    description = "Indica se a política da função admite exercício interino.",
    dataType = "boolean", icon = "swap_horiz", tone = "info",
    sourceKind = "field")
@BulkResourceOperations(proposalOperationId = "ergon.funcoes.bulk.proposal",
    proposalResultsOperationId = "ergon.funcoes.bulk.proposal-results",
    executionOperationId = "ergon.funcoes.bulk.execution",
    executionResultsOperationId = "ergon.funcoes.bulk.results",
    cancelOperationId = "ergon.funcoes.bulk.cancel",
    updateSourceOperationId = "updateErgonFuncaoConfianca",
    protectedUpdateFields = {"empCodigo", "codigo", "situacao", "dtini", "dtfim", "controleVaga", "dtInicioContrVaga", "permiteSubstituicao", "tipoSubstituicao", "diasMinimosSubstituicao", "valorGratificacao", "percentualOpcaoEfetivo", "exigeVinculoEfetivo", "atoPublicacaoId"})
@ApiResource(value = ApiPaths.FUNCOES, resourceKey = ApiPaths.KEY_FUNCOES, title = "Funções de confiança",
    description = "Catálogo por órgão de funções de direção, chefia e assessoramento, com vigência, gratificação e política de vagas e substituição.", icon = "badge",
    identity = @ResourceIdentity(keyField = "id", titleField = "descricao", subtitleField = "codigo",
        metadataFields = {"situacao", "dtini", "empCodigo"}),
    quickFilters = {
        @QuickFilter(id = "ativas", label = "Ativas", filter = "{\"situacao\":\"ATIVA\"}"),
        @QuickFilter(id = "controladas", label = "Com controle de vagas", filter = "{\"controleVaga\":true}"),
        @QuickFilter(id = "extintas", label = "Extintas", filter = "{\"situacao\":\"EXTINTA\"}")})
@ApiGroup(value = "ergon", title = "Administração de pessoal",
    description = "Assentamentos funcionais de órgãos públicos.", icon = "groups")
public class FuncaoConfiancaController extends AbstractBulkGovernedDuplicateDraftResourceController<FuncaoConfiancaResponseDTO, Long, FuncaoConfiancaFilterDTO, CreateFuncaoConfiancaDTO, UpdateFuncaoConfiancaDTO, FuncaoConfiancaDuplicateDraftDTO> {
  private final FuncaoConfiancaService service;
  private final ErgonDeterminationService determinations;
  private final ErgonBulkOperations bulk;

  public FuncaoConfiancaController(FuncaoConfiancaService service, ErgonBulkOperations bulk, ErgonDeterminationService determinations) {
    this.service = service;
    this.determinations = determinations;
    this.bulk = bulk;
  }

  @Override
  protected FuncaoConfiancaService getService() { return service; }

  @Override
  protected Long getResponseId(FuncaoConfiancaResponseDTO dto) { return dto.getId(); }

  @GetMapping("/{id}/detail")
  @PreAuthorize("hasAuthority('ERGON_READ')")
  @Operation(operationId = "detalharFuncaoConfianca", summary = "Consultar ficha de funções de confiança",
      description = "Catálogo por órgão de funções de direção, chefia e assessoramento, com vigência, gratificação e política de vagas e substituição.")
  @UiSurface(id = "detail", kind = SurfaceKind.READ_PROJECTION, scope = SurfaceScope.ITEM, title = "Funções de confiança",
      description = "Catálogo por órgão de funções de direção, chefia e assessoramento, com vigência, gratificação e política de vagas e substituição.", requiredAuthorities = {"ERGON_READ"})
  public ResponseEntity<FuncaoConfiancaResponseDTO> detail(@PathVariable Long id) {
    return withResourceVersion(ResponseEntity.ok(), id, service.findById(id));
  }

  @Override
  @PostMapping("/stats/group-by")
  @Operation(operationId = "statsErgonFuncaoConfianca", summary = "Funções por categoria institucional",
      description = "Conta somente registros do órgão efetivo dentro do recorte solicitado. Cada grupo reúne múltiplos registros e não representa uma ficha individual.")
  @UiAnalytics(projections = {
      @AnalyticsProjection(id = "funcoes-por-categoria", intent = AnalyticsIntent.COMPOSITION,
          sourceOperation = AnalyticsOperation.GROUP_BY,
          primaryDimension = @AnalyticsDimensionBinding(field = "categoria"),
          primaryMetrics = @AnalyticsMetricBinding(field = "count", aggregation = "count", label = "Funções por categoria institucional")),
      @AnalyticsProjection(id = "vigencia-de-funcoes", intent = AnalyticsIntent.TREND,
          sourceOperation = AnalyticsOperation.TIMESERIES,
          primaryDimension = @AnalyticsDimensionBinding(field = "dtini"),
          defaultGranularity = AnalyticsGranularity.MONTH, defaultTimezone = "America/Sao_Paulo",
          primaryMetrics = @AnalyticsMetricBinding(field = "count", aggregation = "count", label = "Funções por início de vigência")),
      @AnalyticsProjection(id = "funcoes-por-categoria-comparativo", intent = AnalyticsIntent.COMPARISON,
          sourceOperation = AnalyticsOperation.COMPARISON,
          primaryDimension = @AnalyticsDimensionBinding(field = "categoria"),
          comparisonPeriod = @AnalyticsComparisonPeriodBinding(field = "dtini", timezone = "America/Sao_Paulo"),
          primaryMetrics = @AnalyticsMetricBinding(field = "count", aggregation = "count", label = "Funções por categoria institucional")),
      @AnalyticsProjection(id = "minimo-para-substituicao", intent = AnalyticsIntent.DISTRIBUTION,
          sourceOperation = AnalyticsOperation.DISTRIBUTION,
          primaryDimension = @AnalyticsDimensionBinding(field = "diasMinimosSubstituicao"),
          primaryMetrics = @AnalyticsMetricBinding(field = "diasMinimosSubstituicao", aggregation = "histogram", label = "Mínimo de dias"))
  })
  public ResponseEntity<RestApiResponse<GroupByStatsResponse>> groupByStats(
      @RequestBody GroupByStatsRequest<FuncaoConfiancaFilterDTO> request) {
    return super.groupByStats(request);
  }

  @Override
  @SuppressWarnings("unchecked")
  protected JpaResourceBulkEngine<Long, FuncaoConfiancaFilterDTO> getBulkEngine() {
    return bulk.engine(Resource.FUNCOES);
  }

  @Override
  @PutMapping("/{id}")
  @BulkResourceOperation(BulkResourceOperation.Role.UPDATE_SOURCE)
  @Operation(operationId = "updateErgonFuncaoConfianca", summary = "Retificar cadastro",
      description = "Mantém o registro no órgão efetivo com controle de versão e validação integral do cadastro.")
  public ResponseEntity<RestApiResponse<FuncaoConfiancaResponseDTO>> update(
      @PathVariable Long id, @Valid @RequestBody UpdateFuncaoConfiancaDTO input) {
    return super.update(id, input);
  }

  @Override
  @PostMapping("/bulk/update/evaluation")
  @BulkResourceOperation(BulkResourceOperation.Role.UNIFORM_EVALUATION)
  @Operation(operationId = "ergon.funcoes.bulk.uniform.evaluation", summary = "Avaliar retificação uniforme",
      description = "Avalia a mesma retificação cadastral nos registros selecionados do órgão e prepara proposta por versão.")
  public ResponseEntity<RestApiResponse<BulkProposal>> evaluateBulkUpdate(
      @RequestBody BulkUniformEvaluationRequest<?, FuncaoConfiancaFilterDTO> request) {
    return super.evaluateBulkUpdate(request);
  }

  @Override
  @PostMapping("/bulk/update-items/evaluation")
  @BulkResourceOperation(BulkResourceOperation.Role.ITEM_EVALUATION)
  @Operation(operationId = "ergon.funcoes.bulk.items.evaluation", summary = "Avaliar retificações individuais",
      description = "Avalia campos e versões de cada retificação individual sem gravar os cadastros.")
  public ResponseEntity<RestApiResponse<BulkProposal>> evaluateBulkUpdateItems(
      @RequestBody BulkItemEvaluationRequest<?> request) {
    return super.evaluateBulkUpdateItems(request);
  }

  @Override
  @PostMapping("/bulk/update")
  @BulkResourceOperation(BulkResourceOperation.Role.UNIFORM_CONFIRMATION)
  @BulkOperation(mode = BulkMode.UNIFORM_UPDATE, evaluationOperationId = "ergon.funcoes.bulk.uniform.evaluation",
      atomicity = ActionCollectionAtomicity.PER_ITEM)
  @Operation(operationId = "ergon.funcoes.bulk.uniform", summary = "Confirmar retificação uniforme",
      description = "Confirma a proposta uniforme com revalidação e recibo de resultado por registro.")
  public ResponseEntity<RestApiResponse<BulkExecution>> confirmBulkUpdate(
      @RequestBody BulkConfirmationRequest request, @RequestHeader("Idempotency-Key") String key) {
    return super.confirmBulkUpdate(request, key);
  }

  @Override
  @PostMapping("/bulk/update-items")
  @BulkResourceOperation(BulkResourceOperation.Role.ITEM_CONFIRMATION)
  @BulkOperation(mode = BulkMode.PER_ITEM_UPDATE, evaluationOperationId = "ergon.funcoes.bulk.items.evaluation",
      atomicity = ActionCollectionAtomicity.PER_ITEM)
  @Operation(operationId = "ergon.funcoes.bulk.items", summary = "Confirmar retificações individuais",
      description = "Aplica cada retificação preservada na proposta com controle de versão e idempotência.")
  public ResponseEntity<RestApiResponse<BulkExecution>> confirmBulkUpdateItems(
      @RequestBody BulkConfirmationRequest request, @RequestHeader("Idempotency-Key") String key) {
    return super.confirmBulkUpdateItems(request, key);
  }

  @Override
  @PostMapping
  @Operation(operationId = "createErgonFuncaoConfianca", summary = "Incluir registro",
      description = "Inclui o registro no órgão efetivo com validação de referências e das regras do cadastro.")
  public ResponseEntity<RestApiResponse<FuncaoConfiancaResponseDTO>> create(@Valid @RequestBody CreateFuncaoConfiancaDTO input) {
    return super.create(input);
  }

  @PostMapping("/determinations/previa")
  @PreAuthorize("hasAuthority('ERGON_READ')")
  @Operation(operationId = "determineErgonFuncaoConfiancaPrevia", summary = "Vigência da função",
      description = "Calcula a duração civil inclusiva da vigência informada.")
  public ResponseEntity<FuncaoPreview> determinePrevia(@Valid @RequestBody FuncaoDraft draft) {
    return ResponseEntity.ok(determinations.previaFuncao(draft));
  }
}
