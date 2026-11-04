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
import br.com.praxis.ergonshowcase.service.VagaFuncaoService;
import io.swagger.v3.oas.annotations.Operation;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.controller.base.AbstractBulkGovernedResourceController;
import org.praxisplatform.uischema.surface.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

@ResourceFact(key = "ocupacao", resourceKey = ApiPaths.KEY_VAGAS_FUNCAO, label = "Ocupação da vaga",
    description = "Indicador de posição ocupada na situação corrente, com valor um para vaga ocupada e zero nos demais estados.",
    dataType = "integer", icon = "event_seat", tone = "info",
    sourceKind = "computed")
@BulkResourceOperations(proposalOperationId = "ergon.vagas-funcao.bulk.proposal",
    proposalResultsOperationId = "ergon.vagas-funcao.bulk.proposal-results",
    executionOperationId = "ergon.vagas-funcao.bulk.execution",
    executionResultsOperationId = "ergon.vagas-funcao.bulk.results",
    cancelOperationId = "ergon.vagas-funcao.bulk.cancel",
    updateSourceOperationId = "updateErgonVagaFuncao",
    protectedUpdateFields = {"empCodigo", "numeroVaga", "funcaoId", "situacao", "dtini", "dtfim", "setorId", "origemVaga", "motivoBloqueio", "atoPublicacaoId"})
@ApiResource(value = ApiPaths.VAGAS_FUNCAO, resourceKey = ApiPaths.KEY_VAGAS_FUNCAO, title = "Vagas de função",
    description = "Posições numeradas autorizadas por função, órgão e unidade, com origem e situação de ocupação.", icon = "event_seat",
    identity = @ResourceIdentity(keyField = "id", titleField = "descricao", subtitleField = "numeroVaga",
        metadataFields = {"empCodigo", "titularAtualId"}),
    quickFilters = {
        @QuickFilter(id = "livres", label = "Livres", filter = "{\"situacao\":\"LIVRE\"}"),
        @QuickFilter(id = "ocupadas", label = "Ocupadas", filter = "{\"situacao\":\"OCUPADA\"}"),
        @QuickFilter(id = "bloqueadas", label = "Bloqueadas", filter = "{\"situacao\":\"BLOQUEADA\"}")})
@ApiGroup(value = "ergon", title = "Administração de pessoal",
    description = "Assentamentos funcionais de órgãos públicos.", icon = "groups")
public class VagaFuncaoController extends AbstractBulkGovernedResourceController<VagaFuncaoResponseDTO, Long, VagaFuncaoFilterDTO, CreateVagaFuncaoDTO, UpdateVagaFuncaoDTO> {
  private final VagaFuncaoService service;
  private final ErgonDeterminationService determinations;
  private final ErgonBulkOperations bulk;

  public VagaFuncaoController(VagaFuncaoService service, ErgonBulkOperations bulk, ErgonDeterminationService determinations) {
    this.service = service;
    this.determinations = determinations;
    this.bulk = bulk;
  }

  @Override
  protected VagaFuncaoService getService() { return service; }

  @Override
  protected Long getResponseId(VagaFuncaoResponseDTO dto) { return dto.getId(); }

  @GetMapping("/{id}/detail")
  @PreAuthorize("hasAuthority('ERGON_READ')")
  @Operation(operationId = "detalharVagaFuncao", summary = "Consultar ficha de vagas de função",
      description = "Posições numeradas autorizadas por função, órgão e unidade, com origem e situação de ocupação.")
  @UiSurface(id = "detail", kind = SurfaceKind.READ_PROJECTION, scope = SurfaceScope.ITEM, title = "Vagas de função",
      description = "Posições numeradas autorizadas por função, órgão e unidade, com origem e situação de ocupação.", requiredAuthorities = {"ERGON_READ"})
  public ResponseEntity<VagaFuncaoResponseDTO> detail(@PathVariable Long id) {
    return withResourceVersion(ResponseEntity.ok(), id, service.findById(id));
  }

  @Override
  @PostMapping("/stats/group-by")
  @Operation(operationId = "statsErgonVagaFuncao", summary = "Posições por situação de ocupação",
      description = "Conta somente registros do órgão efetivo dentro do recorte solicitado. Cada grupo reúne múltiplos registros e não representa uma ficha individual.")
  @UiAnalytics(projections = {
      @AnalyticsProjection(id = "vagas-por-situacao", intent = AnalyticsIntent.COMPOSITION,
          sourceOperation = AnalyticsOperation.GROUP_BY,
          primaryDimension = @AnalyticsDimensionBinding(field = "situacao"),
          primaryMetrics = @AnalyticsMetricBinding(field = "count", aggregation = "count", label = "Posições por situação de ocupação")),
      @AnalyticsProjection(id = "vigencia-de-vagas", intent = AnalyticsIntent.TREND,
          sourceOperation = AnalyticsOperation.TIMESERIES,
          primaryDimension = @AnalyticsDimensionBinding(field = "dtini"),
          defaultGranularity = AnalyticsGranularity.MONTH, defaultTimezone = "America/Sao_Paulo",
          primaryMetrics = @AnalyticsMetricBinding(field = "count", aggregation = "count", label = "Vagas por início de vigência")),
      @AnalyticsProjection(id = "vagas-por-situacao-comparativo", intent = AnalyticsIntent.COMPARISON,
          sourceOperation = AnalyticsOperation.COMPARISON,
          primaryDimension = @AnalyticsDimensionBinding(field = "situacao"),
          comparisonPeriod = @AnalyticsComparisonPeriodBinding(field = "dtini", timezone = "America/Sao_Paulo"),
          primaryMetrics = @AnalyticsMetricBinding(field = "count", aggregation = "count", label = "Posições por situação de ocupação"))
  })
  public ResponseEntity<RestApiResponse<GroupByStatsResponse>> groupByStats(
      @RequestBody GroupByStatsRequest<VagaFuncaoFilterDTO> request) {
    return super.groupByStats(request);
  }

  @Override
  @SuppressWarnings("unchecked")
  protected JpaResourceBulkEngine<Long, VagaFuncaoFilterDTO> getBulkEngine() {
    return bulk.engine(Resource.VAGAS_FUNCAO);
  }

  @Override
  @PutMapping("/{id}")
  @BulkResourceOperation(BulkResourceOperation.Role.UPDATE_SOURCE)
  @Operation(operationId = "updateErgonVagaFuncao", summary = "Retificar cadastro",
      description = "Mantém o registro no órgão efetivo com controle de versão e validação integral do cadastro.")
  public ResponseEntity<RestApiResponse<VagaFuncaoResponseDTO>> update(
      @PathVariable Long id, @Valid @RequestBody UpdateVagaFuncaoDTO input) {
    return super.update(id, input);
  }

  @Override
  @PostMapping("/bulk/update/evaluation")
  @BulkResourceOperation(BulkResourceOperation.Role.UNIFORM_EVALUATION)
  @Operation(operationId = "ergon.vagas-funcao.bulk.uniform.evaluation", summary = "Avaliar retificação uniforme",
      description = "Avalia a mesma retificação cadastral nos registros selecionados do órgão e prepara proposta por versão.")
  public ResponseEntity<RestApiResponse<BulkProposal>> evaluateBulkUpdate(
      @RequestBody BulkUniformEvaluationRequest<?, VagaFuncaoFilterDTO> request) {
    return super.evaluateBulkUpdate(request);
  }

  @Override
  @PostMapping("/bulk/update-items/evaluation")
  @BulkResourceOperation(BulkResourceOperation.Role.ITEM_EVALUATION)
  @Operation(operationId = "ergon.vagas-funcao.bulk.items.evaluation", summary = "Avaliar retificações individuais",
      description = "Avalia campos e versões de cada retificação individual sem gravar os cadastros.")
  public ResponseEntity<RestApiResponse<BulkProposal>> evaluateBulkUpdateItems(
      @RequestBody BulkItemEvaluationRequest<?> request) {
    return super.evaluateBulkUpdateItems(request);
  }

  @Override
  @PostMapping("/bulk/update")
  @BulkResourceOperation(BulkResourceOperation.Role.UNIFORM_CONFIRMATION)
  @BulkOperation(mode = BulkMode.UNIFORM_UPDATE, evaluationOperationId = "ergon.vagas-funcao.bulk.uniform.evaluation",
      atomicity = ActionCollectionAtomicity.PER_ITEM)
  @Operation(operationId = "ergon.vagas-funcao.bulk.uniform", summary = "Confirmar retificação uniforme",
      description = "Confirma a proposta uniforme com revalidação e recibo de resultado por registro.")
  public ResponseEntity<RestApiResponse<BulkExecution>> confirmBulkUpdate(
      @RequestBody BulkConfirmationRequest request, @RequestHeader("Idempotency-Key") String key) {
    return super.confirmBulkUpdate(request, key);
  }

  @Override
  @PostMapping("/bulk/update-items")
  @BulkResourceOperation(BulkResourceOperation.Role.ITEM_CONFIRMATION)
  @BulkOperation(mode = BulkMode.PER_ITEM_UPDATE, evaluationOperationId = "ergon.vagas-funcao.bulk.items.evaluation",
      atomicity = ActionCollectionAtomicity.PER_ITEM)
  @Operation(operationId = "ergon.vagas-funcao.bulk.items", summary = "Confirmar retificações individuais",
      description = "Aplica cada retificação preservada na proposta com controle de versão e idempotência.")
  public ResponseEntity<RestApiResponse<BulkExecution>> confirmBulkUpdateItems(
      @RequestBody BulkConfirmationRequest request, @RequestHeader("Idempotency-Key") String key) {
    return super.confirmBulkUpdateItems(request, key);
  }

  @Override
  @PostMapping
  @Operation(operationId = "createErgonVagaFuncao", summary = "Incluir registro",
      description = "Inclui o registro no órgão efetivo com validação de referências e das regras do cadastro.")
  public ResponseEntity<RestApiResponse<VagaFuncaoResponseDTO>> create(@Valid @RequestBody CreateVagaFuncaoDTO input) {
    return super.create(input);
  }

  @PostMapping("/determinations/previa")
  @PreAuthorize("hasAuthority('ERGON_READ')")
  @Operation(operationId = "determineErgonVagaFuncaoPrevia", summary = "Controle da função",
      description = "Consulta se a função selecionada exige controle de vagas no órgão efetivo.")
  public ResponseEntity<VagaPreview> determinePrevia(@Valid @RequestBody VagaDraft draft) {
    return ResponseEntity.ok(determinations.previaVaga(draft));
  }
}
