package br.com.praxis.ergonshowcase.controller;
import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.service.RadarTempoServicoLicencaEspecialService;
import io.swagger.v3.oas.annotations.Operation;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.controller.base.AbstractReadOnlyResourceController;
import org.praxisplatform.uischema.fact.ResourceFact;
import org.praxisplatform.uischema.surface.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@ApiResource(value = ApiPaths.RADAR_TEMPO_SERVICO_LICENCA_ESPECIAL, resourceKey = "administracao-pessoal.radar-tempo-servico-licenca-especial", title = "Radar de tempo e licença especial", description = "Visão consolidada por ciclo de direito, tempo averbado do vínculo, destinações de dias e prescrição.", icon = "radar",
    identity = @ResourceIdentity(keyField = "id", titleField = "descricao", subtitleField = "pessoaNome", metadataFields = {"dtPrescricao", "diasProgramados", "diasVendidosRegistrados"}),
    quickFilters = {@QuickFilter(id = "saldo", label = "Com saldo disponível", filter = "{\"saldoAcima\":0}"), @QuickFilter(id = "prescricao", label = "Prescrição em até 90 dias", filter = "{\"prescricaoProximosDias\":90}"), @QuickFilter(id = "dobro", label = "Com conversão em dobro", filter = "{\"conversaoAcima\":0}")})
@ApiGroup(value = "ergon", title = "Administração de pessoal", description = "Assentamentos funcionais de órgãos públicos.", icon = "groups")
public class RadarTempoServicoLicencaEspecialController extends AbstractReadOnlyResourceController<RadarTempoServicoLicencaEspecialResponseDTO, Long, RadarTempoServicoLicencaEspecialFilterDTO> {
  private final RadarTempoServicoLicencaEspecialService service;
  public RadarTempoServicoLicencaEspecialController(RadarTempoServicoLicencaEspecialService service) { this.service = service; }
  @Override protected RadarTempoServicoLicencaEspecialService getService() { return service; }
  @Override protected Long getResponseId(RadarTempoServicoLicencaEspecialResponseDTO dto) { return dto.getId(); }
  @GetMapping("/{id}/detail") @PreAuthorize("hasAuthority('ERGON_READ')")
  @Operation(summary = "Inspecionar radar de tempo e licença", description = "Consulta os indicadores consolidados do ciclo sem executar concessões ou conversões.")
  @UiSurface(id = "detail", kind = SurfaceKind.READ_PROJECTION, scope = SurfaceScope.ITEM, title = "Radar de tempo e licença", description = "Saldo, tempo reconhecido, programação e prescrição do ciclo monitorado.", requiredAuthorities = {"ERGON_READ"})
  public ResponseEntity<RadarTempoServicoLicencaEspecialResponseDTO> detail(@PathVariable Long id) { return withResourceVersion(ResponseEntity.ok(), id, service.findById(id)); }

  @Override @PostMapping("/stats/group-by")
  @Operation(operationId = "statsRadarTempoServicoLicencaEspecial", summary = "Distribuição e ranking do direito", description = "Agrega somente registros autorizados pelo filtro oficial; buckets nominais abrem a ficha 360° pela identidade persistida.")
  @UiAnalytics(projections = {
    @AnalyticsProjection(id = "situacoes", intent = AnalyticsIntent.COMPOSITION, sourceOperation = AnalyticsOperation.GROUP_BY,
      primaryDimension = @AnalyticsDimensionBinding(field = "situacao"), primaryMetrics = @AnalyticsMetricBinding(field = "count", aggregation = "count", label = "Registros")),
    @AnalyticsProjection(id = "dias-por-registro", intent = AnalyticsIntent.RANKING, sourceOperation = AnalyticsOperation.GROUP_BY,
      primaryDimension = @AnalyticsDimensionBinding(field = "id"), primaryMetrics = @AnalyticsMetricBinding(field = "saldoDias", aggregation = "sum", label = "Dias"),
      recordOpen = @AnalyticsRecordOpen(sourceIdentityField = "key", target = @AnalyticsSurfaceTarget(resourceKey = "administracao-pessoal.radar-tempo-servico-licenca-especial", surfaceId = "detail")))
  })
  public ResponseEntity<org.praxisplatform.uischema.rest.response.RestApiResponse<org.praxisplatform.uischema.stats.dto.GroupByStatsResponse>> groupByStats(
      @RequestBody org.praxisplatform.uischema.stats.dto.GroupByStatsRequest<RadarTempoServicoLicencaEspecialFilterDTO> request) { return super.groupByStats(request); }
}
