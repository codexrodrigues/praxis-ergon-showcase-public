package br.com.praxis.ergonshowcase.controller;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.dto.FeriasGozoFilterDTO;
import br.com.praxis.ergonshowcase.dto.FeriasGozoResponseDTO;
import br.com.praxis.ergonshowcase.dto.RadarFeriasPrescricaoFilterDTO;
import br.com.praxis.ergonshowcase.dto.RadarFeriasPrescricaoResponseDTO;
import br.com.praxis.ergonshowcase.service.FeriasGozoService;
import br.com.praxis.ergonshowcase.service.RadarFeriasPrescricaoService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.praxisplatform.uischema.annotation.AnalyticsDimensionBinding;
import org.praxisplatform.uischema.annotation.AnalyticsIntent;
import org.praxisplatform.uischema.annotation.AnalyticsMetricBinding;
import org.praxisplatform.uischema.annotation.AnalyticsOperation;
import org.praxisplatform.uischema.annotation.AnalyticsProjection;
import org.praxisplatform.uischema.annotation.AnalyticsRecordOpen;
import org.praxisplatform.uischema.annotation.AnalyticsSurfaceTarget;
import org.praxisplatform.uischema.annotation.ApiGroup;
import org.praxisplatform.uischema.annotation.ApiResource;
import org.praxisplatform.uischema.annotation.QuickFilter;
import org.praxisplatform.uischema.annotation.ResourceIdentity;
import org.praxisplatform.uischema.annotation.UiAnalytics;
import org.praxisplatform.uischema.annotation.UiSurface;
import org.praxisplatform.uischema.controller.base.AbstractReadOnlyResourceController;
import org.praxisplatform.uischema.fact.ResourceFact;
import org.praxisplatform.uischema.rest.response.RestApiResponse;
import org.praxisplatform.uischema.stats.dto.GroupByStatsRequest;
import org.praxisplatform.uischema.stats.dto.GroupByStatsResponse;
import org.praxisplatform.uischema.surface.RelatedResourceChildOperation;
import org.praxisplatform.uischema.surface.SurfaceKind;
import org.praxisplatform.uischema.surface.SurfaceScope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Links;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

/**
 * Publica a projeção analítica executiva somente-leitura para governança de prescrição, queima de
 * saldo, risco de acúmulo de períodos aquisitivos e cobertura de programação futura de férias.
 */
@ResourceFact(
    key = "urgenciaCriticaOuVencida",
    resourceKey = "administracao-pessoal.radar-ferias-prescricao",
    label = "Urgência crítica ou vencida",
    description =
        "Verdadeiro quando o período aquisitivo está vencido/prescrito ou em janela crítica de até 30 dias.",
    dataType = "boolean",
    sourceKind = "computed")
@ResourceFact(
    key = "possuiSaldoDescoberto",
    resourceKey = "administracao-pessoal.radar-ferias-prescricao",
    label = "Com saldo descoberto",
    description =
        "Verdadeiro quando há saldo disponível de dias ainda não coberto por parcelas futuras programadas.",
    dataType = "boolean",
    sourceKind = "computed")
@ResourceFact(
    key = "emRiscoImediatoGovernanca",
    resourceKey = "administracao-pessoal.radar-ferias-prescricao",
    label = "Risco imediato de governança",
    description =
        "Verdadeiro quando o vínculo acumula 2+ períodos com saldo aberto ou possui prescrição crítica/vencida.",
    dataType = "boolean",
    sourceKind = "computed")
@ApiResource(
    value = ApiPaths.RADAR_FERIAS_PRESCRICAO,
    resourceKey = "administracao-pessoal.radar-ferias-prescricao",
    title = "Radar de prescrição de férias",
    description =
        "Projeção analítica executiva de saldos de períodos aquisitivos de férias, queima de direito, cobertura de programação futura, urgência de prescrição e acúmulo de ciclos (PERAQFER + FERIAS).",
    icon = "radar",
    identity =
        @ResourceIdentity(
            keyField = "id",
            titleField = "periodoResumo",
            displayLabelField = "periodoResumo",
            subtitleField = "pessoaNome",
            metadataFields = {
              "vinculoMatricula",
              "exercicio",
              "faixaUrgenciaPrescricao",
              "saldoDiasDisponivel"
            }),
    quickFilters = {
      @QuickFilter(
          id = "vencidos-ou-criticos",
          label = "Vencidos ou críticos (30d)",
          filter = "{\"faixasUrgenciaPrescricao\":[\"VENCIDO_PRESCRITO\",\"CRITICO_30D\"]}"),
      @QuickFilter(
          id = "risco-acumulo-periodos",
          label = "Risco acúmulo de períodos",
          filter = "{\"emRiscoAcumuloPeriodos\":true}"),
      @QuickFilter(
          id = "saldo-descoberto",
          label = "Saldo sem programação",
          filter = "{\"saldoSemProgramacaoMinimo\":1}"),
      @QuickFilter(
          id = "prescrevendo-60d",
          label = "Prescrevendo em 60 dias",
          filter = "{\"prescrevendoProximosDias\":60}"),
      @QuickFilter(
          id = "com-programacao-futura",
          label = "Com parcela programada",
          filter = "{\"possuiParcelaProgramada\":true}")
    })
@ApiGroup(
    value = "ergon",
    title = "Administração de pessoal",
    description = "Cadastros civis e assentamentos funcionais dos órgãos públicos.",
    icon = "groups")
public class RadarFeriasPrescricaoController
    extends AbstractReadOnlyResourceController<
        RadarFeriasPrescricaoResponseDTO, Long, RadarFeriasPrescricaoFilterDTO> {

  private final RadarFeriasPrescricaoService service;
  @Autowired(required = false) private FeriasGozoService feriasGozoService;

  @Autowired
  public RadarFeriasPrescricaoController(RadarFeriasPrescricaoService service) {
    this.service = service;
  }

  public RadarFeriasPrescricaoController(
      RadarFeriasPrescricaoService service, FeriasGozoService feriasGozoService) {
    this.service = service;
    this.feriasGozoService = feriasGozoService;
  }

  @Override
  protected RadarFeriasPrescricaoService getService() {
    return service;
  }

  @Override
  protected Long getResponseId(RadarFeriasPrescricaoResponseDTO dto) {
    return dto.getId();
  }

  @Override
  @PostMapping("/stats/group-by")
  @Operation(
      summary = "Radar de férias por faixa de urgência e saldo descoberto",
      description =
          "Agrega o conjunto autorizado e filtrado do radar de prescrição por faixa de urgência ou saldo descoberto com abertura direta da visão 360°.")
  @UiAnalytics(
      projections = {
        @AnalyticsProjection(
            id = "periodos-por-faixa-urgencia",
            intent = AnalyticsIntent.COMPOSITION,
            sourceOperation = AnalyticsOperation.GROUP_BY,
            primaryDimension = @AnalyticsDimensionBinding(field = "faixaUrgenciaPrescricao"),
            primaryMetrics =
                @AnalyticsMetricBinding(
                    field = "count",
                    aggregation = "count",
                    label = "Períodos monitorados"),
            defaultLimit = 20),
        @AnalyticsProjection(
            id = "saldo-descoberto-por-periodo",
            intent = AnalyticsIntent.RANKING,
            sourceOperation = AnalyticsOperation.GROUP_BY,
            primaryDimension = @AnalyticsDimensionBinding(field = "id"),
            primaryMetrics =
                @AnalyticsMetricBinding(
                    field = "saldoSemProgramacao",
                    aggregation = "sum",
                    label = "Saldo descoberto (dias)"),
            defaultLimit = 20,
            recordOpen =
                @AnalyticsRecordOpen(
                    sourceIdentityField = "key",
                    target =
                        @AnalyticsSurfaceTarget(
                            resourceKey = "administracao-pessoal.radar-ferias-prescricao",
                            surfaceId = "detail")))
      })
  public ResponseEntity<RestApiResponse<GroupByStatsResponse>> groupByStats(
      @Valid @RequestBody GroupByStatsRequest<RadarFeriasPrescricaoFilterDTO> request) {
    return super.groupByStats(request);
  }

  @GetMapping("/{id}/detail")
  @PreAuthorize("hasAuthority('ERGON_READ')")
  @Operation(
      summary = "Consultar visão 360° do radar de prescrição de férias",
      description =
          "Retorna a projeção analítica completa do período aquisitivo monitorado para o drawer Quick View 360°.")
  @UiSurface(
      id = "detail",
      kind = SurfaceKind.READ_PROJECTION,
      scope = SurfaceScope.ITEM,
      title = "Visão 360° do radar de férias",
      description =
          "Indicadores executivos de queima de saldo, cobertura de programação futura, faixa de urgência prescricional e acúmulo de períodos.",
      intent = "summary",
      requiredAuthorities = {"ERGON_READ"})
  public ResponseEntity<RadarFeriasPrescricaoResponseDTO> detail(@PathVariable Long id) {
    return withResourceVersion(ResponseEntity.ok(), id, service.findById(id));
  }

  @GetMapping("/{id}/parcelas-gozo")
  @PreAuthorize("hasAuthority('ERGON_READ')")
  @UiSurface(
      id = "parcelas-gozo-radar",
      kind = SurfaceKind.READ_PROJECTION,
      scope = SurfaceScope.ITEM,
      title = "Parcelas de gozo do período",
      description =
          "Parcelas de programação e gozo vinculadas ao período aquisitivo monitorado no radar.",
      relatedChildResourceKey = "administracao-pessoal.ferias-gozo",
      relatedChildResourcePath = ApiPaths.FERIAS_GOZO,
      relatedChildParentField = "periodoAquisitivoId",
      relatedChildOperations = {
        RelatedResourceChildOperation.FILTER,
        RelatedResourceChildOperation.LIST
      },
      requiredAuthorities = {"ERGON_READ"})
  @Operation(
      summary = "Consultar parcelas de gozo a partir do radar de prescrição",
      description =
          "Lista as parcelas de gozo e abono associadas ao período aquisitivo monitorado no radar. Paginação limitada a 200 registros.")
  public ResponseEntity<RestApiResponse<Page<FeriasGozoResponseDTO>>> parcelasGozo(
      @PathVariable Long id,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    service.findById(id);
    if (page < 0 || size < 1 || size > 200) {
      throw new ResponseStatusException(
          HttpStatus.UNPROCESSABLE_ENTITY, "Paginação fora do limite permitido.");
    }
    var filter = new FeriasGozoFilterDTO();
    filter.setPeriodoAquisitivoId(id);
    var result =
        feriasGozoService.filter(
            filter, PageRequest.of(page, size, feriasGozoService.getDefaultSort()), null);
    return ResponseEntity.ok(
        RestApiResponse.success(result, Links.of(Link.of(ApiPaths.FERIAS_GOZO, "collection"))));
  }
}
