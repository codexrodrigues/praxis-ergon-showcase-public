package br.com.praxis.ergonshowcase.controller;
import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.service.CargoSetorService;
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
@ResourceFact(key = "lotacaoMaximaAutorizada", resourceKey = ApiPaths.KEY_CARGOS_POR_SETOR, label = "Limite autorizado",
    description = "Limite de lotação para este cargo e vigência. O indicador abrange subordinados somente quando a autorização assim determina.",
    dataType = "integer", unit = "vagas", sourceKind = "field")
@ApiResource(value=ApiPaths.CARGOS_POR_SETOR, resourceKey=ApiPaths.KEY_CARGOS_POR_SETOR, title="Cargos e quadro por setor",
 description="Inspeção temporal da estrutura organizacional. Alterações dependem da liberação dos comandos governados.", icon="account_tree",
 identity=@ResourceIdentity(keyField="id",titleField="cargoNome",subtitleField="setor",metadataFields={"observacao"}),
 quickFilters={@QuickFilter(id="abertos",label="Sem encerramento",filter="{\"semEncerramento\":true}"),
 @QuickFilter(id="sem-ato",label="Sem fundamento legal",filter="{\"semFundamento\":true}")})
@ApiGroup(value="ergon",title="Administração de pessoal",description="Estrutura organizacional dos órgãos públicos.")
public class CargoSetorController extends AbstractResourceController<CargoSetorResponseDTO,Long,CargoSetorFilterDTO,CreateCargoSetorDTO,UpdateCargoSetorDTO> {
 private final CargoSetorService service;
 public CargoSetorController(CargoSetorService service) {this.service=service;}
 @Override protected CargoSetorService getService() {return service;}
 @Override protected Long getResponseId(CargoSetorResponseDTO dto) {return dto.getId();}
  @Override
  @PostMapping("/stats/group-by")
  @Operation(summary = "Agrupar autorizações", description = "Apura registros do órgão autorizado no recorte filtrado.")
  @UiAnalytics(projections = {
      @AnalyticsProjection(id = "composicao", intent = AnalyticsIntent.COMPOSITION,
          sourceOperation = AnalyticsOperation.GROUP_BY,
          primaryDimension = @AnalyticsDimensionBinding(field = "tipoControleQuadro"),
          primaryMetrics = @AnalyticsMetricBinding(field = "count", aggregation = "count", label = "Autorizações")),
      @AnalyticsProjection(id = "registros-nominais", intent = AnalyticsIntent.RANKING,
          sourceOperation = AnalyticsOperation.GROUP_BY,
          primaryDimension = @AnalyticsDimensionBinding(field = "id"),
          primaryMetrics = @AnalyticsMetricBinding(field = "count", aggregation = "count", label = "Autorizações"),
          recordOpen = @AnalyticsRecordOpen(sourceIdentityField = "key",
              target = @AnalyticsSurfaceTarget(resourceKey = ApiPaths.KEY_CARGOS_POR_SETOR, surfaceId = "detail"))),
      @AnalyticsProjection(id = "inicios-de-vigencia", intent = AnalyticsIntent.TREND,
          sourceOperation = AnalyticsOperation.TIMESERIES,
          primaryDimension = @AnalyticsDimensionBinding(field = "dtini"),
          defaultGranularity = AnalyticsGranularity.MONTH, defaultTimezone = "America/Sao_Paulo",
          primaryMetrics = @AnalyticsMetricBinding(field = "count", aggregation = "count", label = "Autorizações por início")),
      @AnalyticsProjection(id = "distribuicao", intent = AnalyticsIntent.DISTRIBUTION,
          sourceOperation = AnalyticsOperation.DISTRIBUTION,
          primaryDimension = @AnalyticsDimensionBinding(field = "lotacaoMaximaAutorizada"),
          primaryMetrics = @AnalyticsMetricBinding(field = "lotacaoMaximaAutorizada", aggregation = "histogram", label = "Faixas de lotação autorizada")),
      @AnalyticsProjection(id = "comparativo-por-vigencia", intent = AnalyticsIntent.COMPARISON,
          sourceOperation = AnalyticsOperation.COMPARISON,
          primaryDimension = @AnalyticsDimensionBinding(field = "tipoControleQuadro"),
          comparisonPeriod = @AnalyticsComparisonPeriodBinding(field = "dtini", timezone = "America/Sao_Paulo"),
          primaryMetrics = @AnalyticsMetricBinding(field = "count", aggregation = "count", label = "Autorizações"))
  })
  public ResponseEntity<RestApiResponse<GroupByStatsResponse>> groupByStats(
      @RequestBody GroupByStatsRequest<CargoSetorFilterDTO> request) {
    return super.groupByStats(request);
  }

 @GetMapping("/{id}/detail") @PreAuthorize("hasAuthority('ERGON_READ')")
 @UiSurface(id="detail",kind=SurfaceKind.READ_PROJECTION,scope=SurfaceScope.ITEM,title="Ficha do período",description="Configuração organizacional e fundamento da vigência.",requiredAuthorities={"ERGON_READ"})
 @Operation(summary="Inspecionar período organizacional",description="Apresenta o assentamento do órgão autorizado com a sua fundamentação administrativa.")
 public ResponseEntity<CargoSetorResponseDTO> detail(@Parameter(description="Identificador do período organizacional") @PathVariable Long id) {return withResourceVersion(ResponseEntity.ok(),id,service.findById(id));}
}
