package br.com.praxis.ergonshowcase.controller;
import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.service.SetorParametrizacaoErgonService;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.controller.base.AbstractResourceController;
import org.praxisplatform.uischema.surface.*;
import org.praxisplatform.uischema.stats.dto.GroupByStatsRequest;
import org.praxisplatform.uischema.stats.dto.GroupByStatsResponse;
import org.praxisplatform.uischema.fact.ResourceFact;
import org.praxisplatform.uischema.rest.response.RestApiResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.*;
@ResourceFact(key = "centroCusto", resourceKey = ApiPaths.KEY_SETORES_ERGON, label = "Centro de custo",
    description = "Classificação de apropriação de custo registrada na parametrização Ergon para esta vigência.",
    dataType = "string", sourceKind = "field")
@ApiResource(value=ApiPaths.SETORES_ERGON, resourceKey=ApiPaths.KEY_SETORES_ERGON, title="Parâmetros funcionais de setores",
 description="Inspeção temporal da estrutura organizacional. Alterações dependem da liberação dos comandos governados.", icon="account_tree",
 identity=@ResourceIdentity(keyField="id",titleField="setorNome",subtitleField="setor",metadataFields={"observacao"}),
 quickFilters={@QuickFilter(id="abertos",label="Sem encerramento",filter="{\"semEncerramento\":true}"),
 @QuickFilter(id="sem-ato",label="Sem fundamento legal",filter="{\"semFundamento\":true}")})
@ApiGroup(value="ergon",title="Administração de pessoal",description="Estrutura organizacional dos órgãos públicos.")
public class SetorParametrizacaoErgonController extends AbstractResourceController<SetorParametrizacaoErgonResponseDTO,Long,SetorParametrizacaoErgonFilterDTO,CreateSetorParametrizacaoErgonDTO,UpdateSetorParametrizacaoErgonDTO> {
 private final SetorParametrizacaoErgonService service;
 public SetorParametrizacaoErgonController(SetorParametrizacaoErgonService service) {this.service=service;}
 @Override protected SetorParametrizacaoErgonService getService() {return service;}
 @Override protected Long getResponseId(SetorParametrizacaoErgonResponseDTO dto) {return dto.getId();}
  @Override
  @PostMapping("/stats/group-by")
  @Operation(summary = "Agrupar parametrizações", description = "Apura registros do órgão autorizado no recorte filtrado.")
  @UiAnalytics(projections = {
      @AnalyticsProjection(id = "composicao", intent = AnalyticsIntent.COMPOSITION,
          sourceOperation = AnalyticsOperation.GROUP_BY,
          primaryDimension = @AnalyticsDimensionBinding(field = "tipoAcesso"),
          primaryMetrics = @AnalyticsMetricBinding(field = "count", aggregation = "count", label = "Parametrizações")),
      @AnalyticsProjection(id = "registros-nominais", intent = AnalyticsIntent.RANKING,
          sourceOperation = AnalyticsOperation.GROUP_BY,
          primaryDimension = @AnalyticsDimensionBinding(field = "id"),
          primaryMetrics = @AnalyticsMetricBinding(field = "count", aggregation = "count", label = "Parametrizações"),
          recordOpen = @AnalyticsRecordOpen(sourceIdentityField = "key",
              target = @AnalyticsSurfaceTarget(resourceKey = ApiPaths.KEY_SETORES_ERGON, surfaceId = "detail"))),
      @AnalyticsProjection(id = "inicios-de-vigencia", intent = AnalyticsIntent.TREND,
          sourceOperation = AnalyticsOperation.TIMESERIES,
          primaryDimension = @AnalyticsDimensionBinding(field = "dtini"),
          defaultGranularity = AnalyticsGranularity.MONTH, defaultTimezone = "America/Sao_Paulo",
          primaryMetrics = @AnalyticsMetricBinding(field = "count", aggregation = "count", label = "Parametrizações por início")),
      @AnalyticsProjection(id = "distribuicao", intent = AnalyticsIntent.DISTRIBUTION,
          sourceOperation = AnalyticsOperation.DISTRIBUTION,
          primaryDimension = @AnalyticsDimensionBinding(field = "grauRiscoAmbiental"),
          primaryMetrics = @AnalyticsMetricBinding(field = "grauRiscoAmbiental", aggregation = "terms", label = "Parametrizações por grau de risco")),
      @AnalyticsProjection(id = "comparativo-por-vigencia", intent = AnalyticsIntent.COMPARISON,
          sourceOperation = AnalyticsOperation.COMPARISON,
          primaryDimension = @AnalyticsDimensionBinding(field = "tipoAcesso"),
          comparisonPeriod = @AnalyticsComparisonPeriodBinding(field = "dtini", timezone = "America/Sao_Paulo"),
          primaryMetrics = @AnalyticsMetricBinding(field = "count", aggregation = "count", label = "Parametrizações"))
  })
  public ResponseEntity<RestApiResponse<GroupByStatsResponse>> groupByStats(
      @RequestBody GroupByStatsRequest<SetorParametrizacaoErgonFilterDTO> request) {
    return super.groupByStats(request);
  }

 @GetMapping("/{id}/detail") @PreAuthorize("hasAuthority('ERGON_READ')")
 @UiSurface(id="detail",kind=SurfaceKind.READ_PROJECTION,scope=SurfaceScope.ITEM,title="Ficha do período",description="Configuração organizacional e fundamento da vigência.",requiredAuthorities={"ERGON_READ"})
 @Operation(summary="Inspecionar período organizacional",description="Apresenta o assentamento do órgão autorizado com a sua fundamentação administrativa.")
 public ResponseEntity<SetorParametrizacaoErgonResponseDTO> detail(@Parameter(description="Identificador do período organizacional") @PathVariable Long id) {return withResourceVersion(ResponseEntity.ok(),id,service.findById(id));}
}
