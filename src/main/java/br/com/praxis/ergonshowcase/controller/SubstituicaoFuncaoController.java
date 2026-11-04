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
import br.com.praxis.ergonshowcase.service.SubstituicaoFuncaoService;
import io.swagger.v3.oas.annotations.Operation;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.controller.base.AbstractBulkGovernedDuplicateDraftResourceController;
import org.praxisplatform.uischema.surface.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

@ResourceFact(key = "diasSubstituicao", resourceKey = ApiPaths.KEY_SUBSTITUICOES, label = "Dias de substituição",
    description = "Dias civis inclusivos da vigência do exercício interino registrada no assentamento.",
    dataType = "integer", icon = "timer", tone = "info",
    sourceKind = "computed")
@BulkResourceOperations(proposalOperationId = "ergon.substituicoes.bulk.proposal",
    proposalResultsOperationId = "ergon.substituicoes.bulk.proposal-results",
    executionOperationId = "ergon.substituicoes.bulk.execution",
    executionResultsOperationId = "ergon.substituicoes.bulk.results",
    cancelOperationId = "ergon.substituicoes.bulk.cancel",
    updateSourceOperationId = "updateErgonSubstituicaoFuncao",
    protectedUpdateFields = {"empCodigo", "designacaoId", "substitutoId", "situacao", "dtini", "dtfim", "motivo", "afastamentoId", "feriasGozoId", "dtiniRemunerada", "dtfimRemunerada", "competenciaFolha", "valorSubstituicao", "atoPublicacaoId"})
@ApiResource(value = ApiPaths.SUBSTITUICOES, resourceKey = ApiPaths.KEY_SUBSTITUICOES, title = "Substituições",
    description = "Exercícios interinos vinculados à designação titular e ao impedimento, com vigências funcional e remuneratória independentes.", icon = "swap_horiz",
    identity = @ResourceIdentity(keyField = "id", titleField = "descricao", subtitleField = "substitutoNome",
        metadataFields = {"empCodigo", "competenciaFolha"}),
    quickFilters = {
        @QuickFilter(id = "exercicio", label = "Em exercício", filter = "{\"situacao\":\"EM_EXERCICIO\"}"),
        @QuickFilter(id = "folha", label = "Pendentes de folha", filter = "{\"situacao\":\"PENDENTE_FOLHA\"}"),
        @QuickFilter(id = "ferias", label = "Por férias", filter = "{\"motivo\":\"FERIAS\"}")})
@ApiGroup(value = "ergon", title = "Administração de pessoal",
    description = "Assentamentos funcionais de órgãos públicos.", icon = "groups")
public class SubstituicaoFuncaoController extends AbstractBulkGovernedDuplicateDraftResourceController<SubstituicaoFuncaoResponseDTO, Long, SubstituicaoFuncaoFilterDTO, CreateSubstituicaoFuncaoDTO, UpdateSubstituicaoFuncaoDTO, SubstituicaoFuncaoDuplicateDraftDTO> {
  private final SubstituicaoFuncaoService service;
  private final ErgonDeterminationService determinations;
  private final ErgonBulkOperations bulk;

  public SubstituicaoFuncaoController(SubstituicaoFuncaoService service, ErgonBulkOperations bulk, ErgonDeterminationService determinations) {
    this.service = service;
    this.determinations = determinations;
    this.bulk = bulk;
  }

  @Override
  protected SubstituicaoFuncaoService getService() { return service; }

  @Override
  protected Long getResponseId(SubstituicaoFuncaoResponseDTO dto) { return dto.getId(); }

  @GetMapping("/{id}/detail")
  @PreAuthorize("hasAuthority('ERGON_READ')")
  @Operation(operationId = "detalharSubstituicaoFuncao", summary = "Consultar ficha de substituições",
      description = "Exercícios interinos vinculados à designação titular e ao impedimento, com vigências funcional e remuneratória independentes.")
  @UiSurface(id = "detail", kind = SurfaceKind.READ_PROJECTION, scope = SurfaceScope.ITEM, title = "Substituições",
      description = "Exercícios interinos vinculados à designação titular e ao impedimento, com vigências funcional e remuneratória independentes.", requiredAuthorities = {"ERGON_READ"})
  public ResponseEntity<SubstituicaoFuncaoResponseDTO> detail(@PathVariable Long id) {
    return withResourceVersion(ResponseEntity.ok(), id, service.findById(id));
  }

  @Override
  @PostMapping("/stats/group-by")
  @Operation(operationId = "statsErgonSubstituicaoFuncao", summary = "Exercícios interinos por impedimento",
      description = "Conta somente registros do órgão efetivo dentro do recorte solicitado. Cada grupo reúne múltiplos registros e não representa uma ficha individual.")
  @UiAnalytics(projections = {
      @AnalyticsProjection(id = "substituicoes-por-impedimento", intent = AnalyticsIntent.COMPOSITION,
          sourceOperation = AnalyticsOperation.GROUP_BY,
          primaryDimension = @AnalyticsDimensionBinding(field = "motivo"),
          primaryMetrics = @AnalyticsMetricBinding(field = "count", aggregation = "count", label = "Exercícios interinos por impedimento")),
      @AnalyticsProjection(id = "vigencia-de-substituicoes", intent = AnalyticsIntent.TREND,
          sourceOperation = AnalyticsOperation.TIMESERIES,
          primaryDimension = @AnalyticsDimensionBinding(field = "dtini"),
          defaultGranularity = AnalyticsGranularity.MONTH, defaultTimezone = "America/Sao_Paulo",
          primaryMetrics = @AnalyticsMetricBinding(field = "count", aggregation = "count", label = "Substituições por início de vigência")),
      @AnalyticsProjection(id = "substituicoes-por-impedimento-comparativo", intent = AnalyticsIntent.COMPARISON,
          sourceOperation = AnalyticsOperation.COMPARISON,
          primaryDimension = @AnalyticsDimensionBinding(field = "motivo"),
          comparisonPeriod = @AnalyticsComparisonPeriodBinding(field = "dtini", timezone = "America/Sao_Paulo"),
          primaryMetrics = @AnalyticsMetricBinding(field = "count", aggregation = "count", label = "Exercícios interinos por impedimento"))
  })
  public ResponseEntity<RestApiResponse<GroupByStatsResponse>> groupByStats(
      @RequestBody GroupByStatsRequest<SubstituicaoFuncaoFilterDTO> request) {
    return super.groupByStats(request);
  }

  @Override
  @SuppressWarnings("unchecked")
  protected JpaResourceBulkEngine<Long, SubstituicaoFuncaoFilterDTO> getBulkEngine() {
    return bulk.engine(Resource.SUBSTITUICOES);
  }

  @Override
  @PutMapping("/{id}")
  @BulkResourceOperation(BulkResourceOperation.Role.UPDATE_SOURCE)
  @Operation(operationId = "updateErgonSubstituicaoFuncao", summary = "Retificar cadastro",
      description = "Mantém o registro no órgão efetivo com controle de versão e validação integral do cadastro.")
  public ResponseEntity<RestApiResponse<SubstituicaoFuncaoResponseDTO>> update(
      @PathVariable Long id, @Valid @RequestBody UpdateSubstituicaoFuncaoDTO input) {
    return super.update(id, input);
  }

  @Override
  @PostMapping("/bulk/update/evaluation")
  @BulkResourceOperation(BulkResourceOperation.Role.UNIFORM_EVALUATION)
  @Operation(operationId = "ergon.substituicoes.bulk.uniform.evaluation", summary = "Avaliar retificação uniforme",
      description = "Avalia a mesma retificação cadastral nos registros selecionados do órgão e prepara proposta por versão.")
  public ResponseEntity<RestApiResponse<BulkProposal>> evaluateBulkUpdate(
      @RequestBody BulkUniformEvaluationRequest<?, SubstituicaoFuncaoFilterDTO> request) {
    return super.evaluateBulkUpdate(request);
  }

  @Override
  @PostMapping("/bulk/update-items/evaluation")
  @BulkResourceOperation(BulkResourceOperation.Role.ITEM_EVALUATION)
  @Operation(operationId = "ergon.substituicoes.bulk.items.evaluation", summary = "Avaliar retificações individuais",
      description = "Avalia campos e versões de cada retificação individual sem gravar os cadastros.")
  public ResponseEntity<RestApiResponse<BulkProposal>> evaluateBulkUpdateItems(
      @RequestBody BulkItemEvaluationRequest<?> request) {
    return super.evaluateBulkUpdateItems(request);
  }

  @Override
  @PostMapping("/bulk/update")
  @BulkResourceOperation(BulkResourceOperation.Role.UNIFORM_CONFIRMATION)
  @BulkOperation(mode = BulkMode.UNIFORM_UPDATE, evaluationOperationId = "ergon.substituicoes.bulk.uniform.evaluation",
      atomicity = ActionCollectionAtomicity.PER_ITEM)
  @Operation(operationId = "ergon.substituicoes.bulk.uniform", summary = "Confirmar retificação uniforme",
      description = "Confirma a proposta uniforme com revalidação e recibo de resultado por registro.")
  public ResponseEntity<RestApiResponse<BulkExecution>> confirmBulkUpdate(
      @RequestBody BulkConfirmationRequest request, @RequestHeader("Idempotency-Key") String key) {
    return super.confirmBulkUpdate(request, key);
  }

  @Override
  @PostMapping("/bulk/update-items")
  @BulkResourceOperation(BulkResourceOperation.Role.ITEM_CONFIRMATION)
  @BulkOperation(mode = BulkMode.PER_ITEM_UPDATE, evaluationOperationId = "ergon.substituicoes.bulk.items.evaluation",
      atomicity = ActionCollectionAtomicity.PER_ITEM)
  @Operation(operationId = "ergon.substituicoes.bulk.items", summary = "Confirmar retificações individuais",
      description = "Aplica cada retificação preservada na proposta com controle de versão e idempotência.")
  public ResponseEntity<RestApiResponse<BulkExecution>> confirmBulkUpdateItems(
      @RequestBody BulkConfirmationRequest request, @RequestHeader("Idempotency-Key") String key) {
    return super.confirmBulkUpdateItems(request, key);
  }

  @Override
  @PostMapping
  @Operation(operationId = "createErgonSubstituicaoFuncao", summary = "Incluir registro",
      description = "Inclui o registro no órgão efetivo com validação de referências e das regras do cadastro.")
  public ResponseEntity<RestApiResponse<SubstituicaoFuncaoResponseDTO>> create(@Valid @RequestBody CreateSubstituicaoFuncaoDTO input) {
    return super.create(input);
  }

  @PostMapping("/determinations/previa")
  @PreAuthorize("hasAuthority('ERGON_READ')")
  @Operation(operationId = "determineErgonSubstituicaoFuncaoPrevia", summary = "Duração e mínimo remunerado",
      description = "Calcula a duração proposta e consulta o mínimo remunerado cadastrado na função do titular.")
  public ResponseEntity<SubstituicaoPreview> determinePrevia(@Valid @RequestBody SubstituicaoDraft draft) {
    return ResponseEntity.ok(determinations.previaSubstituicao(draft));
  }
}
