package br.com.praxis.ergonshowcase.controller;
import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.service.RadarOrganogramaChefiaOcupacaoService;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.controller.base.AbstractReadOnlyResourceController;
import org.praxisplatform.uischema.surface.*;
import org.praxisplatform.uischema.stats.dto.GroupByStatsRequest;
import org.praxisplatform.uischema.stats.dto.GroupByStatsResponse;
import org.praxisplatform.uischema.fact.ResourceFact;
import org.praxisplatform.uischema.rest.response.RestApiResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.*;
@ResourceHierarchy(parentField="setorPai",hierarchyPathField="caminhoHierarquico",levelField="nivelHierarquico",childrenCountField="quantidadeSubordinadosDiretos",extinctField="extinto",auxiliaryParentFields={"setorPaiHier1","setorPaiHier2","setorPaiHier3","setorPaiHier4"})
@ResourceFact(key = "situacaoComandoSetor", resourceKey = ApiPaths.KEY_RADAR_ORGANOGRAMA_CHEFIAS_OCUPACAO, label = "Situação do comando",
    description = "Diagnóstico calculado pela vigência da função, designação, exercício, impedimento e substituição. A apuração por cargo permanece indisponível.",
    dataType = "string", sourceKind = "field")
@ResourceFact(key = "vagasAutorizadasSetor", resourceKey = ApiPaths.KEY_RADAR_ORGANOGRAMA_CHEFIAS_OCUPACAO, label = "Vagas diretas autorizadas",
    description = "Total das autorizações vigentes que se restringem ao setor. Reservas para subordinados e ocupação ficam fora dessa soma.",
    dataType = "integer", unit = "vagas", sourceKind = "field")
@ResourceFact(key = "escopoApuracao", resourceKey = ApiPaths.KEY_RADAR_ORGANOGRAMA_CHEFIAS_OCUPACAO, label = "Escopo da apuração",
    description = "Identifica as fontes integradas ao segmento e as lacunas de provimento, exercício, folha e comando por cargo.",
    dataType = "string", sourceKind = "field")
@ApiResource(value=ApiPaths.RADAR_ORGANOGRAMA_CHEFIAS_OCUPACAO,resourceKey=ApiPaths.KEY_RADAR_ORGANOGRAMA_CHEFIAS_OCUPACAO,title="Organograma temporal",description="Estrutura histórica e escopo da apuração de comando e ocupação.",icon="account_tree",
 identity=@ResourceIdentity(keyField="id",titleField="nomeSetor",subtitleField="siglaSetor",metadataFields={"tipoHierarquia"}),
 quickFilters={@QuickFilter(id="extintos",label="Unidades extintas",filter="{\"extinto\":true}"),@QuickFilter(id="sem-apuracao",label="Comando não apurado",filter="{\"semComandoApurado\":true}")})
@ApiGroup(value="ergon",title="Administração de pessoal",description="Estrutura organizacional dos órgãos públicos.")
public class RadarOrganogramaChefiaOcupacaoController extends AbstractReadOnlyResourceController<RadarOrganogramaChefiaOcupacaoResponseDTO,String,RadarOrganogramaChefiaOcupacaoFilterDTO> {
 private final RadarOrganogramaChefiaOcupacaoService service;
 public RadarOrganogramaChefiaOcupacaoController(RadarOrganogramaChefiaOcupacaoService service) {this.service=service;}
 @Override protected RadarOrganogramaChefiaOcupacaoService getService() {return service;}
 @Override protected String getResponseId(RadarOrganogramaChefiaOcupacaoResponseDTO dto) {return dto.getId();}
  @Override
  @PostMapping("/stats/group-by")
  @Operation(summary = "Agrupar nós do organograma", description = "Apura registros do órgão autorizado no recorte filtrado.")
  @UiAnalytics(projections = {
      @AnalyticsProjection(id = "composicao", intent = AnalyticsIntent.COMPOSITION,
          sourceOperation = AnalyticsOperation.GROUP_BY,
          primaryDimension = @AnalyticsDimensionBinding(field = "situacaoComandoSetor"),
          primaryMetrics = @AnalyticsMetricBinding(field = "count", aggregation = "count", label = "Nós do organograma")),
      @AnalyticsProjection(id = "registros-nominais", intent = AnalyticsIntent.RANKING,
          sourceOperation = AnalyticsOperation.GROUP_BY,
          primaryDimension = @AnalyticsDimensionBinding(field = "id"),
          primaryMetrics = @AnalyticsMetricBinding(field = "count", aggregation = "count", label = "Nós do organograma"),
          recordOpen = @AnalyticsRecordOpen(sourceIdentityField = "key",
              target = @AnalyticsSurfaceTarget(resourceKey = ApiPaths.KEY_RADAR_ORGANOGRAMA_CHEFIAS_OCUPACAO, surfaceId = "detail"))),
      @AnalyticsProjection(id = "inicios-de-vigencia", intent = AnalyticsIntent.TREND,
          sourceOperation = AnalyticsOperation.TIMESERIES,
          primaryDimension = @AnalyticsDimensionBinding(field = "dtini"),
          defaultGranularity = AnalyticsGranularity.MONTH, defaultTimezone = "America/Sao_Paulo",
          primaryMetrics = @AnalyticsMetricBinding(field = "count", aggregation = "count", label = "Nós do organograma por início")),
      @AnalyticsProjection(id = "distribuicao", intent = AnalyticsIntent.DISTRIBUTION,
          sourceOperation = AnalyticsOperation.DISTRIBUTION,
          primaryDimension = @AnalyticsDimensionBinding(field = "tipoSetor"),
          primaryMetrics = @AnalyticsMetricBinding(field = "count", aggregation = "count", label = "Nós por classificação")),
      @AnalyticsProjection(id = "comparativo-por-vigencia", intent = AnalyticsIntent.COMPARISON,
          sourceOperation = AnalyticsOperation.COMPARISON,
          primaryDimension = @AnalyticsDimensionBinding(field = "situacaoComandoSetor"),
          comparisonPeriod = @AnalyticsComparisonPeriodBinding(field = "dtini", timezone = "America/Sao_Paulo"),
          primaryMetrics = @AnalyticsMetricBinding(field = "count", aggregation = "count", label = "Nós do organograma"))
  })
  public ResponseEntity<RestApiResponse<GroupByStatsResponse>> groupByStats(
      @RequestBody GroupByStatsRequest<RadarOrganogramaChefiaOcupacaoFilterDTO> request) {
    return super.groupByStats(request);
  }

 @GetMapping("/{id}/detail") @PreAuthorize("hasAuthority('ERGON_READ')")
 @UiSurface(id="detail",kind=SurfaceKind.READ_PROJECTION,scope=SurfaceScope.ITEM,title="Ficha temporal",description="Composição histórica da unidade e escopo dos indicadores.",requiredAuthorities={"ERGON_READ"})
 @Operation(summary="Inspecionar nó temporal",description="Consulta o período e a hierarquia identificados no órgão autorizado.")
 public ResponseEntity<RadarOrganogramaChefiaOcupacaoResponseDTO> detail(@Parameter(description="Id do período, segmento e hierarquia") @PathVariable String id) {return withResourceVersion(ResponseEntity.ok(),id,service.findById(id));}
}
