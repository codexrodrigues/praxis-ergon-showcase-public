package br.com.praxis.ergonshowcase.controller;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.service.*;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import java.util.List;
import java.util.function.BiConsumer;
import org.springdoc.core.annotations.ParameterObject;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.controller.base.AbstractReadOnlyResourceController;
import org.praxisplatform.uischema.fact.ResourceFact;
import org.praxisplatform.uischema.stats.dto.GroupByStatsRequest;
import org.praxisplatform.uischema.stats.dto.GroupByStatsResponse;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;
import org.praxisplatform.uischema.rest.response.RestApiResponse;
import org.praxisplatform.uischema.service.base.BaseResourceQueryService;
import org.praxisplatform.uischema.surface.*;
import org.praxisplatform.uischema.util.PageableBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Links;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Consulta auxiliar que sustenta opções e referências 360° dos pilotos. */
@ResourceFact(
    key = "tipoSetor",
    resourceKey = ApiPaths.KEY_SETORES,
    label = "Classificação da unidade",
    description =
        "Natureza organizacional ou administrativa da unidade de lotação no organograma do órgão.",
    dataType = "string",
    icon = "account_tree",
    tone = "info",
    valuePresentation = "badge",
    sourceKind = "field")
@ResourceFact(key = "quantidadeSubordinadosDiretos", resourceKey = ApiPaths.KEY_SETORES, label = "Subordinados diretos",
    description = "Quantidade de unidades diretamente ligadas ao setor na composição disponível. O valor é desconhecido quando a composição não resolve o nó.",
    dataType = "integer", unit = "unidades", sourceKind = "field")
@ApiResource(
    value = ApiPaths.SETORES,
    resourceKey = ApiPaths.KEY_SETORES,
    title = "Unidades organizacionais",
    description = "Diretório de unidades e composição temporal do organograma para inspeção de lotações, quadro e comando.",
    identity =
        @ResourceIdentity(
            keyField = "id",
            titleField = "nome",
            subtitleField = "sigla",
            metadataFields = {"setorPai", "dtfim"}),
    quickFilters = {
      @QuickFilter(id = "vigentes", label = "Unidades vigentes", filter = "{\"vigentes\":true}"),
      @QuickFilter(
          id = "encerrados",
          label = "Unidades encerradas",
          filter = "{\"encerrados\":true}")
    })
@ApiGroup(
    value = "ergon",
    title = "Administração de pessoal",
    description = "Cadastros civis e assentamentos funcionais dos órgãos públicos.")
@ResourceHierarchy(parentField="setorPai",hierarchyPathField="caminhoHierarquico",levelField="nivelHierarquico",
    childrenCountField="quantidadeSubordinadosDiretos",extinctField="extinto",
    auxiliaryParentFields={"setorPaiHier1","setorPaiHier2","setorPaiHier3","setorPaiHier4"})
public class SetorController
    extends AbstractReadOnlyResourceController<SetorResponseDTO, String, SetorFilterDTO> {
  private final SetorService service;
  private final HistoricoSetorService historicos;
  private final SetorParametrizacaoErgonService parametrizacoes;
  private final CargoSetorService cargos;
  private final DesignacaoFuncaoService designacoes;
  private final RadarOrganogramaChefiaOcupacaoService radar;

  public SetorController(SetorService service, HistoricoSetorService historicos,
      SetorParametrizacaoErgonService parametrizacoes, CargoSetorService cargos,
      DesignacaoFuncaoService designacoes, RadarOrganogramaChefiaOcupacaoService radar) {
    this.service = service;
    this.historicos = historicos;
    this.parametrizacoes = parametrizacoes;
    this.cargos = cargos;
    this.designacoes = designacoes;
    this.radar = radar;
  }

  @Override
  protected SetorService getService() {
    return service;
  }

  @Override
  protected String getResponseId(SetorResponseDTO dto) {
    return dto.getReferenciaFicha();
  }

  @Override
  @PostMapping("/stats/group-by")
  @Operation(summary = "Agrupar unidades", description = "Apura registros do órgão autorizado no recorte filtrado.")
  @UiAnalytics(projections = {
      @AnalyticsProjection(id = "composicao", intent = AnalyticsIntent.COMPOSITION,
          sourceOperation = AnalyticsOperation.GROUP_BY,
          primaryDimension = @AnalyticsDimensionBinding(field = "tipoSetor"),
          primaryMetrics = @AnalyticsMetricBinding(field = "count", aggregation = "count", label = "Unidades")),
      @AnalyticsProjection(id = "registros-nominais", intent = AnalyticsIntent.RANKING,
          sourceOperation = AnalyticsOperation.GROUP_BY,
          primaryDimension = @AnalyticsDimensionBinding(field = "id"),
          primaryMetrics = @AnalyticsMetricBinding(field = "count", aggregation = "count", label = "Unidades"),
          recordOpen = @AnalyticsRecordOpen(sourceIdentityField = "key",
              target = @AnalyticsSurfaceTarget(resourceKey = ApiPaths.KEY_SETORES, surfaceId = "ficha-unidade"))),
      @AnalyticsProjection(id = "inicios-de-vigencia", intent = AnalyticsIntent.TREND,
          sourceOperation = AnalyticsOperation.TIMESERIES,
          primaryDimension = @AnalyticsDimensionBinding(field = "dtini"),
          defaultGranularity = AnalyticsGranularity.MONTH, defaultTimezone = "America/Sao_Paulo",
          primaryMetrics = @AnalyticsMetricBinding(field = "count", aggregation = "count", label = "Unidades por início")),
      @AnalyticsProjection(id = "distribuicao", intent = AnalyticsIntent.DISTRIBUTION,
          sourceOperation = AnalyticsOperation.DISTRIBUTION,
          primaryDimension = @AnalyticsDimensionBinding(field = "ufLotacao"),
          primaryMetrics = @AnalyticsMetricBinding(field = "count", aggregation = "count", label = "Unidades por UF de lotação")),
      @AnalyticsProjection(id = "comparativo-por-vigencia", intent = AnalyticsIntent.COMPARISON,
          sourceOperation = AnalyticsOperation.COMPARISON,
          primaryDimension = @AnalyticsDimensionBinding(field = "tipoSetor"),
          comparisonPeriod = @AnalyticsComparisonPeriodBinding(field = "dtini", timezone = "America/Sao_Paulo"),
          primaryMetrics = @AnalyticsMetricBinding(field = "count", aggregation = "count", label = "Unidades"))
  })
  public ResponseEntity<RestApiResponse<GroupByStatsResponse>> groupByStats(
      @RequestBody GroupByStatsRequest<SetorFilterDTO> request) {
    return super.groupByStats(request);
  }

  @GetMapping("/{id}/ficha-unidade")
  @PreAuthorize("hasAuthority('ERGON_READ')")
  @UiSurface(
      id = "ficha-unidade",
      kind = SurfaceKind.READ_PROJECTION,
      scope = SurfaceScope.ITEM,
      title = "Ficha da unidade",
      description = "Consulta autenticada do registro referenciado e sua fundamentação.",
      requiredAuthorities = {"ERGON_READ"})
  @Operation(
      summary = "Consultar ficha auxiliar",
      description = "Apresenta a unidade corrente pelo código ou a composição temporal indicada pela referência da ficha.")
  public ResponseEntity<SetorResponseDTO> ficha(
      @Parameter(description = "Código da unidade ou referência temporal publicada na consulta") @PathVariable String id) {
    return withResourceVersion(ResponseEntity.ok(), id, service.findById(id));
  }

  @GetMapping(ApiPaths.SETOR_ARVORE)
  @PreAuthorize("hasAuthority('ERGON_READ')")
  @UiSurface(id = "organograma-tree", kind = SurfaceKind.HIERARCHY_TREE,
      scope = SurfaceScope.COLLECTION, title = "Árvore do organograma",
      description = "Composição paginada das unidades vigentes na data e na hierarquia selecionadas.",
      requiredAuthorities = {"ERGON_READ"})
  @Operation(operationId = "consultarArvoreOrganograma", summary = "Consultar árvore do organograma",
      description = "Aplica filtros de unidade à composição do órgão autorizado. A data padrão é hoje e a hierarquia padrão é a oficial.",
      parameters = @Parameter(name = "sort", in = ParameterIn.QUERY,
          description = "Ordenação no formato campo,direção. Repita o parâmetro para ordenar por mais de um campo."))
  public ResponseEntity<RestApiResponse<Page<SetorResponseDTO>>> arvore(
      @ParameterObject @ModelAttribute SetorFilterDTO filter,
      @Parameter(description = "Número da página, a partir de zero")
      @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "Quantidade de unidades por página, de 1 a 200")
      @RequestParam(defaultValue = "20") int size,
      @Parameter(description = "Códigos de unidade que restringem a página")
      @RequestParam(required = false) List<String> includeIds,
      @Parameter(hidden = true) @RequestParam MultiValueMap<String, String> queryParams) {
    ErgonHttpContract.childPage(page, size);
    if (filter.getTipoHierarquia() == null) {
      filter.setTipoHierarquia(0);
    }
    var pageable = treePage(page, size, queryParams.get("sort"));
    var rows = service.filter(filter, pageable, includeIds);
    var links = buildCollectionDiscoveryLinks();
    links.add(linkToUiSchema(ApiPaths.SETOR_ARVORE, "get", "response"));
    return successEnvelope(ResponseEntity.ok(), rows, hateoasOrNull(Links.of(links)));
  }

  private Pageable treePage(int page, int size, List<String> sort) {
    try {
      return PageableBuilder.from(page, size, sort, service.getDefaultSort());
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage(), e);
    }
  }

  @GetMapping("/{id}/historico-vigencias")
  @PreAuthorize("hasAuthority('ERGON_READ')")
  @UiSurface(id = "historico-vigencias", kind = SurfaceKind.READ_PROJECTION, scope = SurfaceScope.ITEM,
      title = "Histórico de vigências", description = "Períodos organizacionais registrados para a unidade.",
      relatedChildResourceKey = ApiPaths.KEY_HISTORICO_SETORES, relatedChildResourcePath = ApiPaths.HISTORICO_SETORES,
      relatedChildParentField = "setor", relatedChildOperations = RelatedResourceChildOperation.FILTER,
      requiredAuthorities = {"ERGON_READ"})
  @Operation(operationId = "historicoVigenciasDoSetor", summary = "Histórico de vigências", description = "Períodos organizacionais registrados para a unidade.")
  public ResponseEntity<RestApiResponse<Page<HistoricoSetorResponseDTO>>> historicoVigencias(
      @PathVariable String id, @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return related(id, page, size, historicos, new HistoricoSetorFilterDTO(), (filter, unidade) -> filter.setSetor(unidade.getId()));
  }

  @GetMapping("/{id}/parametrizacoes-ergon")
  @PreAuthorize("hasAuthority('ERGON_READ')")
  @UiSurface(id = "parametrizacoes-ergon", kind = SurfaceKind.READ_PROJECTION, scope = SurfaceScope.ITEM,
      title = "Parametrizações Ergon", description = "Histórico de comando, classificação funcional e custo da unidade.",
      relatedChildResourceKey = ApiPaths.KEY_SETORES_ERGON, relatedChildResourcePath = ApiPaths.SETORES_ERGON,
      relatedChildParentField = "setor", relatedChildOperations = RelatedResourceChildOperation.FILTER,
      requiredAuthorities = {"ERGON_READ"})
  @Operation(operationId = "parametrizacoesErgonDoSetor", summary = "Parametrizações Ergon", description = "Histórico de comando, classificação funcional e custo da unidade.")
  public ResponseEntity<RestApiResponse<Page<SetorParametrizacaoErgonResponseDTO>>> parametrizacoesErgon(
      @PathVariable String id, @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return related(id, page, size, parametrizacoes, new SetorParametrizacaoErgonFilterDTO(), (filter, unidade) -> filter.setSetor(unidade.getId()));
  }

  @GetMapping("/{id}/cargos-autorizados")
  @PreAuthorize("hasAuthority('ERGON_READ')")
  @UiSurface(id = "cargos-autorizados", kind = SurfaceKind.READ_PROJECTION, scope = SurfaceScope.ITEM,
      title = "Cargos autorizados", description = "Autorizações de quadro diretamente registradas para a unidade.",
      relatedChildResourceKey = ApiPaths.KEY_CARGOS_POR_SETOR, relatedChildResourcePath = ApiPaths.CARGOS_POR_SETOR,
      relatedChildParentField = "setor", relatedChildOperations = RelatedResourceChildOperation.FILTER,
      requiredAuthorities = {"ERGON_READ"})
  @Operation(operationId = "cargosAutorizadosDoSetor", summary = "Cargos autorizados", description = "Autorizações de quadro diretamente registradas para a unidade.")
  public ResponseEntity<RestApiResponse<Page<CargoSetorResponseDTO>>> cargosAutorizados(
      @PathVariable String id, @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return related(id, page, size, cargos, new CargoSetorFilterDTO(), (filter, unidade) -> filter.setSetor(unidade.getId()));
  }

  @GetMapping("/{id}/designacoes-chefia")
  @PreAuthorize("hasAuthority('ERGON_READ')")
  @UiSurface(id = "designacoes-chefia", kind = SurfaceKind.READ_PROJECTION, scope = SurfaceScope.ITEM,
      title = "Designações de chefia", description = "Atos de designação funcional registrados para a unidade.",
      relatedChildResourceKey = ApiPaths.KEY_DESIGNACOES, relatedChildResourcePath = ApiPaths.DESIGNACOES,
      relatedChildParentField = "setorId", relatedChildOperations = RelatedResourceChildOperation.FILTER,
      requiredAuthorities = {"ERGON_READ"})
  @Operation(operationId = "designacoesChefiaDoSetor", summary = "Designações de chefia", description = "Atos de designação funcional registrados para a unidade.")
  public ResponseEntity<RestApiResponse<Page<DesignacaoFuncaoResponseDTO>>> designacoesChefia(
      @PathVariable String id, @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return related(id, page, size, designacoes, new DesignacaoFuncaoFilterDTO(), (filter, unidade) -> filter.setSetorId(unidade.getId()));
  }

  @GetMapping("/{id}/radar-subarvore")
  @PreAuthorize("hasAuthority('ERGON_READ')")
  @UiSurface(id = "radar-subarvore", kind = SurfaceKind.READ_PROJECTION, scope = SurfaceScope.ITEM,
      title = "Radar da subárvore", description = "Apuração da unidade e de todos os seus descendentes na composição consultada.",
      relatedChildResourceKey = ApiPaths.KEY_RADAR_ORGANOGRAMA_CHEFIAS_OCUPACAO, relatedChildResourcePath = ApiPaths.RADAR_ORGANOGRAMA_CHEFIAS_OCUPACAO,
      relatedChildParentField = "raizSubarvore", relatedChildOperations = RelatedResourceChildOperation.FILTER,
      requiredAuthorities = {"ERGON_READ"})
  @Operation(operationId = "radarSubarvoreDoSetor", summary = "Radar da subárvore", description = "Apuração da unidade e de todos os seus descendentes na composição consultada.")
  public ResponseEntity<RestApiResponse<Page<RadarOrganogramaChefiaOcupacaoResponseDTO>>> radarSubarvore(
      @PathVariable String id, @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return related(id, page, size, radar, new RadarOrganogramaChefiaOcupacaoFilterDTO(), service::bindRadarSubtree);
  }

  private <R, I, F extends GenericFilterDTO> ResponseEntity<RestApiResponse<Page<R>>> related(
      String id, int page, int size, BaseResourceQueryService<R, I, F> child, F filter,
      BiConsumer<F, SetorResponseDTO> bindParent) {
    ErgonHttpContract.childPage(page, size);
    bindParent.accept(filter, service.findById(id).body());
    var rows = child.filter(filter, PageRequest.of(page, size, child.getDefaultSort()), List.of());
    return successEnvelope(ResponseEntity.ok(), rows, null);
  }
}
