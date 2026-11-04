package br.com.praxis.ergonshowcase.controller;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.service.RadarChefiaSubstituicaoService;
import io.swagger.v3.oas.annotations.Operation;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.controller.base.AbstractReadOnlyResourceController;
import org.praxisplatform.uischema.fact.ResourceFact;
import org.praxisplatform.uischema.surface.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@ResourceFact(
    key = "chefiasCobertas",
    resourceKey = ApiPaths.KEY_RADAR_CHEFIAS_SUBSTITUICOES,
    label = "Substituições vigentes",
    description = "Quantidade de exercícios interinos vigentes na data de referência para cobrir impedimentos do titular.",
    dataType = "integer",
    unit = "days",
    icon = "swap_horiz",
    tone = "info",
    valuePresentation = "badge",
    thresholds = {
      @Threshold(max = 0, tone = "warning", label = "Sem cobertura"),
      @Threshold(min = 1, tone = "info", label = "Com substituto")
    },
    sourceKind = "computed")
@ResourceFact(
    key = "pendenciasFolhaAtivas",
    resourceKey = ApiPaths.KEY_RADAR_CHEFIAS_SUBSTITUICOES,
    label = "Pendências de folha",
    description = "Quantidade de substituições com apuração remuneratória pendente de processamento em folha.",
    dataType = "integer",
    unit = "days",
    icon = "payments",
    tone = "warning",
    valuePresentation = "badge",
    thresholds = {
      @Threshold(max = 0, tone = "success", label = "Regular"),
      @Threshold(min = 1, tone = "danger", label = "Pendente")
    },
    sourceKind = "computed")
@ResourceFact(
    key = "chefiaDescobertaNaData",
    resourceKey = ApiPaths.KEY_RADAR_CHEFIAS_SUBSTITUICOES,
    label = "Chefia descoberta",
    description = "Verdadeiro quando o titular está impedido na data de referência e não há exercício interino vigente.",
    dataType = "boolean",
    icon = "warning",
    tone = "danger",
    valuePresentation = "badge",
    sourceKind = "computed")
@ApiResource(value = ApiPaths.RADAR_CHEFIAS_SUBSTITUICOES, resourceKey = ApiPaths.KEY_RADAR_CHEFIAS_SUBSTITUICOES, title = "Radar de chefias",
    description = "Posição de cobertura da chefia na data de referência. Distingue titular em exercício, impedimento coberto e chefia descoberta.", icon = "radar",
    identity = @ResourceIdentity(keyField = "id", titleField = "descricao", subtitleField = "titularNome",
        metadataFields = {"empCodigo", "dataReferencia"}),
    quickFilters = {
        @QuickFilter(id = "descobertas", label = "Chefias descobertas", filter = "{\"situacao\":\"DESCOBERTA\"}"),
        @QuickFilter(id = "cobertas", label = "Com substituição", filter = "{\"situacao\":\"COBERTA\"}"),
        @QuickFilter(id = "folha", label = "Com pendência de folha", filter = "{\"pendenciasFolhaMin\":1}")})
@ApiGroup(value = "ergon", title = "Administração de pessoal",
    description = "Assentamentos funcionais de órgãos públicos.", icon = "groups")
public class RadarChefiaSubstituicaoController extends AbstractReadOnlyResourceController<RadarChefiaSubstituicaoResponseDTO, Long, RadarChefiaSubstituicaoFilterDTO> {
  private final RadarChefiaSubstituicaoService service;

  public RadarChefiaSubstituicaoController(RadarChefiaSubstituicaoService service) {
    this.service = service;
  }
  @Override
  protected RadarChefiaSubstituicaoService getService() { return service; }
  @Override
  protected Long getResponseId(RadarChefiaSubstituicaoResponseDTO dto) { return dto.getId(); }

  @GetMapping("/{id}/detail")
  @PreAuthorize("hasAuthority('ERGON_READ')")
  @Operation(operationId = "detalharRadarChefiaSubstituicao", summary = "Consultar ficha de radar de chefias",
      description = "Posição de cobertura da chefia na data de referência. Distingue titular em exercício, impedimento coberto e chefia descoberta.")
  @UiSurface(id = "detail", kind = SurfaceKind.READ_PROJECTION, scope = SurfaceScope.ITEM, title = "Radar de chefias",
      description = "Posição de cobertura da chefia na data de referência. Distingue titular em exercício, impedimento coberto e chefia descoberta.", requiredAuthorities = {"ERGON_READ"})
  public ResponseEntity<RadarChefiaSubstituicaoResponseDTO> detail(@PathVariable Long id) {
    return withResourceVersion(ResponseEntity.ok(), id, service.findById(id));
  }

  @Override
  @PostMapping("/stats/group-by")
  @Operation(
      operationId = "statsRadarChefiaSubstituicao",
      summary = "Distribuição e ranking de cobertura de chefias",
      description =
          "Agrega somente registros autorizados pelo filtro oficial; buckets nominais abrem a ficha 360° pela identidade persistida.")
  @UiAnalytics(
      projections = {
        @AnalyticsProjection(
            id = "cobertura-por-situacao",
            intent = AnalyticsIntent.COMPOSITION,
            sourceOperation = AnalyticsOperation.GROUP_BY,
            primaryDimension = @AnalyticsDimensionBinding(field = "situacao"),
            primaryMetrics =
                @AnalyticsMetricBinding(
                    field = "count",
                    aggregation = "count",
                    label = "Chefias")),
        @AnalyticsProjection(
            id = "ranking-pendencias-folha",
            intent = AnalyticsIntent.RANKING,
            sourceOperation = AnalyticsOperation.GROUP_BY,
            primaryDimension = @AnalyticsDimensionBinding(field = "id"),
            primaryMetrics =
                @AnalyticsMetricBinding(
                    field = "pendenciasFolha",
                    aggregation = "sum",
                    label = "Pendências"),
            recordOpen =
                @AnalyticsRecordOpen(
                    sourceIdentityField = "key",
                    target =
                        @AnalyticsSurfaceTarget(
                            resourceKey = ApiPaths.KEY_RADAR_CHEFIAS_SUBSTITUICOES,
                            surfaceId = "detail"))),
        @AnalyticsProjection(
            id = "chefias-por-unidade",
            intent = AnalyticsIntent.RANKING,
            sourceOperation = AnalyticsOperation.GROUP_BY,
            primaryDimension = @AnalyticsDimensionBinding(field = "setorId"),
            primaryMetrics =
                @AnalyticsMetricBinding(
                    field = "qtdCoberturas",
                    aggregation = "sum",
                    label = "Coberturas"))
      })
  public ResponseEntity<
          org.praxisplatform.uischema.rest.response.RestApiResponse<
              org.praxisplatform.uischema.stats.dto.GroupByStatsResponse>>
      groupByStats(
          @RequestBody
              org.praxisplatform.uischema.stats.dto.GroupByStatsRequest<
                      RadarChefiaSubstituicaoFilterDTO>
                  request) {
    return super.groupByStats(request);
  }
}
