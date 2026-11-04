package br.com.praxis.ergonshowcase.controller;
import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.service.GrupoSetorService;
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
@ResourceFact(key = "tipoAgrupamento", resourceKey = ApiPaths.KEY_GRUPOS_SETORES, label = "Tipo de agrupamento",
    description = "Finalidade administrativa da classificação que reúne unidades sob o mesmo grupo na vigência.",
    dataType = "string", sourceKind = "field")
@ApiResource(value=ApiPaths.GRUPOS_SETORES, resourceKey=ApiPaths.KEY_GRUPOS_SETORES, title="Grupos de setores",
 description="Inspeção temporal da estrutura organizacional. Alterações dependem da liberação dos comandos governados.", icon="account_tree",
 identity=@ResourceIdentity(keyField="id",titleField="nome",subtitleField="sigla",metadataFields={"observacao"}),
 quickFilters={@QuickFilter(id="abertos",label="Sem encerramento",filter="{\"semEncerramento\":true}"),
 @QuickFilter(id="sem-ato",label="Sem fundamento legal",filter="{\"semFundamento\":true}")})
@ApiGroup(value="ergon",title="Administração de pessoal",description="Estrutura organizacional dos órgãos públicos.")
public class GrupoSetorController extends AbstractResourceController<GrupoSetorResponseDTO,Long,GrupoSetorFilterDTO,CreateGrupoSetorDTO,UpdateGrupoSetorDTO> {
 private final GrupoSetorService service;
 public GrupoSetorController(GrupoSetorService service) {this.service=service;}
 @Override protected GrupoSetorService getService() {return service;}
 @Override protected Long getResponseId(GrupoSetorResponseDTO dto) {return dto.getId();}
  @Override
  @PostMapping("/stats/group-by")
  @Operation(summary = "Agrupar grupos", description = "Apura registros do órgão autorizado no recorte filtrado.")
  @UiAnalytics(projections = {
      @AnalyticsProjection(id = "composicao", intent = AnalyticsIntent.COMPOSITION,
          sourceOperation = AnalyticsOperation.GROUP_BY,
          primaryDimension = @AnalyticsDimensionBinding(field = "tipoAgrupamento"),
          primaryMetrics = @AnalyticsMetricBinding(field = "count", aggregation = "count", label = "Grupos")),
      @AnalyticsProjection(id = "registros-nominais", intent = AnalyticsIntent.RANKING,
          sourceOperation = AnalyticsOperation.GROUP_BY,
          primaryDimension = @AnalyticsDimensionBinding(field = "id"),
          primaryMetrics = @AnalyticsMetricBinding(field = "count", aggregation = "count", label = "Grupos"),
          recordOpen = @AnalyticsRecordOpen(sourceIdentityField = "key",
              target = @AnalyticsSurfaceTarget(resourceKey = ApiPaths.KEY_GRUPOS_SETORES, surfaceId = "detail"))),
      @AnalyticsProjection(id = "inicios-de-vigencia", intent = AnalyticsIntent.TREND,
          sourceOperation = AnalyticsOperation.TIMESERIES,
          primaryDimension = @AnalyticsDimensionBinding(field = "dtini"),
          defaultGranularity = AnalyticsGranularity.MONTH, defaultTimezone = "America/Sao_Paulo",
          primaryMetrics = @AnalyticsMetricBinding(field = "count", aggregation = "count", label = "Grupos por início")),
      @AnalyticsProjection(id = "distribuicao", intent = AnalyticsIntent.DISTRIBUTION,
          sourceOperation = AnalyticsOperation.DISTRIBUTION,
          primaryDimension = @AnalyticsDimensionBinding(field = "ufAbrangencia"),
          primaryMetrics = @AnalyticsMetricBinding(field = "count", aggregation = "count", label = "Grupos por UF de abrangência")),
      @AnalyticsProjection(id = "comparativo-por-vigencia", intent = AnalyticsIntent.COMPARISON,
          sourceOperation = AnalyticsOperation.COMPARISON,
          primaryDimension = @AnalyticsDimensionBinding(field = "tipoAgrupamento"),
          comparisonPeriod = @AnalyticsComparisonPeriodBinding(field = "dtini", timezone = "America/Sao_Paulo"),
          primaryMetrics = @AnalyticsMetricBinding(field = "count", aggregation = "count", label = "Grupos"))
  })
  public ResponseEntity<RestApiResponse<GroupByStatsResponse>> groupByStats(
      @RequestBody GroupByStatsRequest<GrupoSetorFilterDTO> request) {
    return super.groupByStats(request);
  }

 @GetMapping("/{id}/detail") @PreAuthorize("hasAuthority('ERGON_READ')")
 @UiSurface(id="detail",kind=SurfaceKind.READ_PROJECTION,scope=SurfaceScope.ITEM,title="Ficha do período",description="Configuração organizacional e fundamento da vigência.",requiredAuthorities={"ERGON_READ"})
 @Operation(summary="Inspecionar período organizacional",description="Apresenta o assentamento do órgão autorizado com a sua fundamentação administrativa.")
 public ResponseEntity<GrupoSetorResponseDTO> detail(@Parameter(description="Identificador do período organizacional") @PathVariable Long id) {return withResourceVersion(ResponseEntity.ok(),id,service.findById(id));}
}
