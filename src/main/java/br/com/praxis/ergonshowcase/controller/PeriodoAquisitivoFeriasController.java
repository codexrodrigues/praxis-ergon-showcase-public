package br.com.praxis.ergonshowcase.controller;

import br.com.praxis.ergonshowcase.bulk.*;
import br.com.praxis.ergonshowcase.bulk.ErgonBulkDomain.Resource;
import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import java.util.UUID;
import org.praxisplatform.uischema.action.*;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.bulk.*;
import org.praxisplatform.uischema.concurrency.*;
import org.praxisplatform.uischema.controller.base.AbstractResourceController;
import org.praxisplatform.uischema.dto.CursorPage;
import org.praxisplatform.uischema.fact.ResourceFact;
import org.praxisplatform.uischema.rest.response.RestApiResponse;
import org.praxisplatform.uischema.stats.dto.GroupByStatsRequest;
import org.praxisplatform.uischema.stats.dto.GroupByStatsResponse;
import org.praxisplatform.uischema.surface.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Links;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Publica o contrato de períodos aquisitivos de férias (PERAQFER) através da hierarquia canônica Praxis. */
@ResourceFact(
    key = "possuiSaldoDisponivel",
    resourceKey = "administracao-pessoal.periodos-aquisitivos-ferias",
    label = "Com saldo disponível",
    description = "Verdadeiro quando o período aquisitivo possui saldo líquido de dias disponível para programação.",
    dataType = "boolean",
    icon = "event_available",
    tone = "success",
    valuePresentation = "badge",
    sourceKind = "computed")
@ResourceFact(
    key = "possuiOverridesAtivos",
    resourceKey = "administracao-pessoal.periodos-aquisitivos-ferias",
    label = "Com override individual",
    description = "Verdadeiro quando o período aquisitivo sobrescreve parâmetros herdados de PARAMETROS_CONTA.",
    dataType = "boolean",
    icon = "tune",
    tone = "warning",
    valuePresentation = "badge",
    sourceKind = "computed")
@ResourceFact(
    key = "emRiscoPrescricao",
    resourceKey = "administracao-pessoal.periodos-aquisitivos-ferias",
    label = "Em risco de prescrição",
    description = "Verdadeiro quando há saldo disponível e data de prescrição apurada ou prescrição efetivada.",
    dataType = "boolean",
    icon = "warning_amber",
    tone = "danger",
    valuePresentation = "badge",
    sourceKind = "computed")
@ResourceFact(
    key = "saldoDiasDisponivel",
    resourceKey = "administracao-pessoal.periodos-aquisitivos-ferias",
    label = "Saldo disponível",
    description = "Saldo líquido de dias de férias disponível para programação ou conversão.",
    dataType = "integer",
    unit = "days",
    icon = "event_available",
    tone = "info",
    valuePresentation = "metric",
    thresholds = {
      @Threshold(max = 0, tone = "neutral", label = "Quitado"),
      @Threshold(min = 1, max = 10, tone = "warning", label = "Saldo residual"),
      @Threshold(min = 11, max = 60, tone = "success", label = "Saldo regular")
    },
    sourceKind = "computed")
@BulkResourceOperations(
    proposalOperationId = "ergon.periodos-aquisitivos-ferias.bulk.proposal",
    proposalResultsOperationId = "ergon.periodos-aquisitivos-ferias.bulk.proposal-results",
    executionOperationId = "ergon.periodos-aquisitivos-ferias.bulk.execution",
    executionResultsOperationId = "ergon.periodos-aquisitivos-ferias.bulk.results",
    cancelOperationId = "ergon.periodos-aquisitivos-ferias.bulk.cancel",
    updateSourceOperationId = "updateErgonPeriodoAquisitivoFerias",
    protectedUpdateFields = {"empCodigo", "numfunc", "numvinc", "chave", "dtini", "diasGozados", "diasVendidosAbono", "diasConvertidosDobro", "geradoPorAverbacao"})
@ApiResource(
    value = ApiPaths.PERIODOS_AQUISITIVOS_FERIAS,
    resourceKey = "administracao-pessoal.periodos-aquisitivos-ferias",
    title = "Períodos aquisitivos de férias",
    description =
        "Ciclos aquisitivos de férias por vínculo funcional, contabilidade de direito/faltas/gozo/abono/saldo, prescrição e overrides individuais sobre PARAMETROS_CONTA (PERAQFER).",
    icon = "date_range",
    identity =
        @ResourceIdentity(
            keyField = "id",
            titleField = "periodoResumo",
            displayLabelField = "periodoResumo",
            subtitleField = "pessoaNome",
            metadataFields = {"situacao", "chave", "geradoPorAverbacao"}),
    quickFilters = {
      @QuickFilter(
          id = "disponiveis",
          label = "Com saldo disponível",
          filter = "{\"saldoMinimoDisponivel\":1}"),
      @QuickFilter(
          id = "em-aquisicao",
          label = "Em aquisição",
          filter = "{\"situacoes\":[\"EM_AQUISICAO\"]}"),
      @QuickFilter(
          id = "parcialmente-gozados",
          label = "Parcialmente gozados",
          filter = "{\"situacoes\":[\"PARCIALMENTE_GOZADO\"]}"),
      @QuickFilter(
          id = "prescritos",
          label = "Prescritos",
          filter = "{\"situacoes\":[\"PRESCRITO\"]}"),
      @QuickFilter(
          id = "com-dobro",
          label = "Com contagem em dobro",
          filter = "{\"diasConvertidosDobroMinimo\":1}")
    })
@ApiGroup(
    value = "ergon",
    title = "Administração de pessoal",
    description = "Cadastros civis e assentamentos funcionais dos órgãos públicos.",
    icon = "groups")
public class PeriodoAquisitivoFeriasController
    extends org.praxisplatform.uischema.controller.base.AbstractBulkGovernedResourceController<
        PeriodoAquisitivoFeriasResponseDTO,
        Long,
        PeriodoAquisitivoFeriasFilterDTO,
        CreatePeriodoAquisitivoFeriasDTO,
        UpdatePeriodoAquisitivoFeriasDTO> {

  private final PeriodoAquisitivoFeriasService service;
  @Autowired private FeriasGozoService feriasGozoService;
  @Autowired private ErgonBulkOperations bulk;
  @Autowired private ErgonDeterminationService determinations;

  public PeriodoAquisitivoFeriasController(
      PeriodoAquisitivoFeriasService service,
      ResourceVersionEtagService etags,
      ResourceVersionScopeProvider scopes) {
    this.service = service;
    configureResourceVersioning(etags, scopes);
  }

  @Override
  @PostMapping("/stats/group-by")
  @Operation(
      summary = "Períodos aquisitivos por situação e saldo de dias disponível",
      description =
          "Agrega o conjunto autorizado e filtrado de períodos aquisitivos de férias com abertura direta da ficha pela identidade real do ciclo.")
  @UiAnalytics(
      projections = {
        @AnalyticsProjection(
            id = "periodos-aquisitivos-por-situacao",
            intent = AnalyticsIntent.COMPOSITION,
            sourceOperation = AnalyticsOperation.GROUP_BY,
            primaryDimension = @AnalyticsDimensionBinding(field = "situacao"),
            primaryMetrics =
                @AnalyticsMetricBinding(
                    field = "count",
                    aggregation = "count",
                    label = "Períodos aquisitivos"),
            defaultLimit = 20),
        @AnalyticsProjection(
            id = "saldo-disponivel-por-periodo",
            intent = AnalyticsIntent.RANKING,
            sourceOperation = AnalyticsOperation.GROUP_BY,
            primaryDimension = @AnalyticsDimensionBinding(field = "id"),
            primaryMetrics =
                @AnalyticsMetricBinding(
                    field = "saldoDiasDisponivel",
                    aggregation = "sum",
                    label = "Saldo disponível (dias)"),
            defaultLimit = 20,
            recordOpen =
                @AnalyticsRecordOpen(
                    sourceIdentityField = "key",
                    target =
                        @AnalyticsSurfaceTarget(
                            resourceKey = "administracao-pessoal.periodos-aquisitivos-ferias",
                            surfaceId = "ficha-periodo-aquisitivo"))),
        @AnalyticsProjection(
            id = "evolucao-abertura-periodos-aquisitivos",
            intent = AnalyticsIntent.TREND,
            sourceOperation = AnalyticsOperation.TIMESERIES,
            primaryDimension = @AnalyticsDimensionBinding(field = "dtini"),
            defaultGranularity = AnalyticsGranularity.MONTH,
            defaultTimezone = "America/Sao_Paulo",
            primaryMetrics =
                @AnalyticsMetricBinding(
                    field = "count",
                    aggregation = "count",
                    label = "Períodos iniciados no mês")),
        @AnalyticsProjection(
            id = "distribuicao-saldo-dias-disponivel",
            intent = AnalyticsIntent.DISTRIBUTION,
            sourceOperation = AnalyticsOperation.DISTRIBUTION,
            primaryDimension = @AnalyticsDimensionBinding(field = "saldoDiasDisponivel"),
            primaryMetrics =
                @AnalyticsMetricBinding(
                    field = "saldoDiasDisponivel",
                    aggregation = "histogram",
                    label = "Distribuição de saldo disponível (dias)")),
        @AnalyticsProjection(
            id = "comparativo-periodos-por-situacao",
            intent = AnalyticsIntent.COMPARISON,
            sourceOperation = AnalyticsOperation.COMPARISON,
            primaryDimension = @AnalyticsDimensionBinding(field = "situacao"),
            comparisonPeriod =
                @AnalyticsComparisonPeriodBinding(
                    field = "dtini",
                    timezone = "America/Sao_Paulo"),
            primaryMetrics =
                @AnalyticsMetricBinding(
                    field = "count",
                    aggregation = "count",
                    label = "Variação de períodos aquisitivos"))
      })
  public ResponseEntity<RestApiResponse<GroupByStatsResponse>> groupByStats(
      @RequestBody GroupByStatsRequest<PeriodoAquisitivoFeriasFilterDTO> request) {
    return super.groupByStats(request);
  }

  @Override
  @PostMapping
  @Operation(
      operationId = "createErgonPeriodoAquisitivoFerias",
      summary = "Cadastrar período aquisitivo de férias",
      description =
          "Inclui novo ciclo aquisitivo de férias do vínculo validando órgão, cronologia, contabilidade de saldo e overrides individuais.")
  public ResponseEntity<RestApiResponse<PeriodoAquisitivoFeriasResponseDTO>> create(
      @Valid @RequestBody CreatePeriodoAquisitivoFeriasDTO dto) {
    return super.create(dto);
  }

  @Override
  @PutMapping("/{id}")
  @BulkResourceOperation(BulkResourceOperation.Role.UPDATE_SOURCE)
  @Operation(
      operationId = "updateErgonPeriodoAquisitivoFerias",
      summary = "Atualizar período aquisitivo de férias",
      description =
          "Atualiza contabilidade de dias, prescrição e overrides individuais do período aquisitivo com If-Match.")
  public ResponseEntity<RestApiResponse<PeriodoAquisitivoFeriasResponseDTO>> update(
      @PathVariable Long id, @Valid @RequestBody UpdatePeriodoAquisitivoFeriasDTO dto) {
    return super.update(id, dto);
  }

  @Override
  protected PeriodoAquisitivoFeriasService getService() {
    return service;
  }

  @Override
  protected Long getResponseId(PeriodoAquisitivoFeriasResponseDTO dto) {
    return dto.getId();
  }

  @GetMapping("/{id}/ficha-periodo-aquisitivo")
  @PreAuthorize("hasAuthority('ERGON_READ')")
  @Operation(
      summary = "Consultar ficha 360° do período aquisitivo de férias",
      description =
          "Retorna a projeção completa de leitura do período aquisitivo de férias para o drawer Quick View.")
  @UiSurface(
      id = "ficha-periodo-aquisitivo",
      kind = SurfaceKind.READ_PROJECTION,
      scope = SurfaceScope.ITEM,
      title = "Ficha do período aquisitivo",
      description =
          "Visão consolidada de direito adquirido, faltas, saldo disponível, prescrição e overrides do ciclo aquisitivo.",
      intent = "summary",
      requiredAuthorities = {"ERGON_READ"})
  public ResponseEntity<PeriodoAquisitivoFeriasResponseDTO> ficha(@PathVariable Long id) {
    return withResourceVersion(ResponseEntity.ok(), id, service.findById(id));
  }

  @GetMapping("/{id}/ferias-gozo")
  @PreAuthorize("hasAuthority('ERGON_READ')")
  @UiSurface(
      id = "ferias-gozo-periodo",
      kind = SurfaceKind.READ_PROJECTION,
      scope = SurfaceScope.ITEM,
      title = "Parcelas de gozo de férias",
      description =
          "Parcelas programadas, homologadas ou gozadas associadas ao período aquisitivo selecionado.",
      relatedChildResourceKey = "administracao-pessoal.ferias-gozo",
      relatedChildResourcePath = ApiPaths.FERIAS_GOZO,
      relatedChildParentField = "periodoAquisitivoId",
      relatedChildOperations = {
        RelatedResourceChildOperation.FILTER,
        RelatedResourceChildOperation.LIST
      },
      requiredAuthorities = {"ERGON_READ"})
  @Operation(
      summary = "Consultar parcelas de gozo do período aquisitivo",
      description =
          "Lista as parcelas de programação e gozo de férias vinculadas ao período aquisitivo. A paginação é limitada a 200 registros por resposta.")
  public ResponseEntity<RestApiResponse<Page<FeriasGozoResponseDTO>>> feriasGozo(
      @Parameter(description = "Identificador único do período aquisitivo")
          @PathVariable
          Long id,
      @Parameter(description = "Página a consultar, iniciando em zero")
          @RequestParam(defaultValue = "0")
          int page,
      @Parameter(description = "Quantidade de registros da página, de 1 a 200")
          @RequestParam(defaultValue = "20")
          int size) {
    ErgonHttpContract.childPage(page, size);
    return filterRelatedChildResource(id, feriasGozoService, ApiPaths.FERIAS_GOZO, FeriasGozoFilterDTO::new,
        (child, parent) -> { child.setPeriodoAquisitivoId(parent); }, page, size);
  }

  @PatchMapping("/{id}/overrides-parametros")
  @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(
      operationId = "patchPeriodoAquisitivoOverrides",
      summary = "Atualizar overrides individuais sobre os parâmetros de contagem",
      description =
          "Atualiza os campos de exceção individual ('I'=Herda de PARAMETROS_CONTA, 'S'/'N' ou numérico=Sobrescreve) do período aquisitivo com If-Match.")
  @ResourceIntent(
      id = "overrides-parametros",
      title = "Overrides individuais (PERAQFER)",
      description = "Manutenção parcial das exceções individuais sobre PARAMETROS_CONTA.")
  @UiSurface(
      id = "overrides-parametros",
      kind = SurfaceKind.PARTIAL_FORM,
      scope = SurfaceScope.ITEM,
      title = "Overrides individuais (PERAQFER)",
      description =
          "Revisão focalizada das regras de parcelamento, abono e gozo específicas deste período aquisitivo.",
      intent = "overrides-parametros",
      requiredAuthorities = {"ERGON_WRITE"})
  public ResponseEntity<PeriodoAquisitivoFeriasResponseDTO> atualizarOverrides(
      @PathVariable Long id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch,
      @Valid @RequestBody AtualizarOverridesPeriodoAquisitivoDTO dto) {
    return ErgonHttpContract.itemBody(executeVersionedItemPatch("overrides-parametros", id, ifMatch, condition -> service.atualizarOverrides(id, dto, condition)));
  }

  @PatchMapping("/{id}/prescricao-multa")
  @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(
      operationId = "patchPeriodoAquisitivoPrescricaoMulta",
      summary = "Atualizar prescrição, multa e documento legal",
      description =
          "Atualiza datas de prescrição, indicador de prescrição, multa por concessão fora do prazo e ato legal com If-Match.")
  @ResourceIntent(
      id = "prescricao-multa",
      title = "Prescrição, multa e ato legal",
      description = "Manutenção parcial da prescrição, multa administrativa e ato legal do PA.")
  @UiSurface(
      id = "prescricao-multa",
      kind = SurfaceKind.PARTIAL_FORM,
      scope = SurfaceScope.ITEM,
      title = "Prescrição, multa e ato legal",
      description = "Revisão focalizada do controle prescricional e fundamentação formal do PA.",
      intent = "prescricao-multa",
      requiredAuthorities = {"ERGON_WRITE"})
  public ResponseEntity<PeriodoAquisitivoFeriasResponseDTO> atualizarPrescricaoMulta(
      @PathVariable Long id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch,
      @Valid @RequestBody AtualizarPrescricaoMultaPeriodoAquisitivoDTO dto) {
    return ErgonHttpContract.itemBody(executeVersionedItemPatch("prescricao-multa", id, ifMatch, condition -> service.atualizarPrescricaoMulta(id, dto, condition)));
  }

  @PostMapping("/{id}/actions/homologar-periodo-aquisitivo")
  @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(
      summary = "Homologar fechamento do período aquisitivo",
      description =
          "Conclui a apuração do ciclo aquisitivo e libera o saldo para programação de gozo. Exige If-Match.")
  @WorkflowAction(
      id = "homologar-periodo-aquisitivo",
      title = "Homologar período aquisitivo",
      description = "Libera o período aquisitivo apurado para programação de parcelas de férias.",
      scope = ActionScope.ITEM,
      requiredAuthorities = {"ERGON_WRITE"},
      allowedStates = {"EM_AQUISICAO", "SUSPENSO"},
      confirmationRequired = true,
      resourceVersion = ActionRequirement.REQUIRED,
      resourceVersionTransport = ActionResourceVersionTransport.IF_MATCH)
  public ResponseEntity<PeriodoAquisitivoFeriasResponseDTO> homologar(
      @PathVariable Long id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch,
      @Valid @RequestBody TransicaoPeriodoAquisitivoFeriasDTO dto) {
    return ErgonHttpContract.itemBody(executeVersionedItemCommand("homologar-periodo-aquisitivo", id, ifMatch, null, dto,
        condition -> service.homologarPeriodoAquisitivo(id, dto, condition)));
  }

  @PostMapping("/{id}/actions/suspender-periodo-aquisitivo")
  @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(
      summary = "Suspender contagem do período aquisitivo",
      description =
          "Registra suspensão administrativa do ciclo aquisitivo em razão de licença ou afastamento suspensivo. Exige If-Match.")
  @WorkflowAction(
      id = "suspender-periodo-aquisitivo",
      title = "Suspender período aquisitivo",
      description = "Suspende o ciclo aquisitivo por afastamento ou ato administrativo.",
      scope = ActionScope.ITEM,
      requiredAuthorities = {"ERGON_WRITE"},
      allowedStates = {"EM_AQUISICAO", "DISPONIVEL", "PARCIALMENTE_GOZADO"},
      confirmationRequired = true,
      resourceVersion = ActionRequirement.REQUIRED,
      resourceVersionTransport = ActionResourceVersionTransport.IF_MATCH)
  public ResponseEntity<PeriodoAquisitivoFeriasResponseDTO> suspender(
      @PathVariable Long id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch,
      @Valid @RequestBody TransicaoPeriodoAquisitivoFeriasDTO dto) {
    return ErgonHttpContract.itemBody(executeVersionedItemCommand("suspender-periodo-aquisitivo", id, ifMatch, null, dto,
        condition -> service.suspenderPeriodoAquisitivo(id, dto, condition)));
  }

  @PostMapping("/{id}/actions/converter-dobro-periodo-aquisitivo")
  @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(
      summary = "Converter saldo em contagem em dobro",
      description =
          "Converte dias disponíveis do período aquisitivo para contagem em dobro mediante ato legal. Exige If-Match.")
  @WorkflowAction(
      id = "converter-dobro-periodo-aquisitivo",
      title = "Converter em contagem em dobro",
      description = "Destina dias do saldo disponível para averbação em dobro.",
      scope = ActionScope.ITEM,
      requiredAuthorities = {"ERGON_WRITE"},
      allowedStates = {"DISPONIVEL", "PARCIALMENTE_GOZADO"},
      confirmationRequired = true,
      resourceVersion = ActionRequirement.REQUIRED,
      resourceVersionTransport = ActionResourceVersionTransport.IF_MATCH)
  public ResponseEntity<PeriodoAquisitivoFeriasResponseDTO> converterDobro(
      @PathVariable Long id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch,
      @Valid @RequestBody ConverterDobroPeriodoAquisitivoDTO dto) {
    return ErgonHttpContract.itemBody(executeVersionedItemCommand("converter-dobro-periodo-aquisitivo", id, ifMatch, null, dto,
        condition -> service.converterDobroPeriodoAquisitivo(id, dto, condition)));
  }

  @PostMapping("/{id}/actions/prescrever-periodo-aquisitivo")
  @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(
      summary = "Registrar prescrição do período aquisitivo",
      description =
          "Reconhece administrativamente a prescrição do período aquisitivo e zera o saldo disponível. Exige If-Match.")
  @WorkflowAction(
      id = "prescrever-periodo-aquisitivo",
      title = "Registrar prescrição",
      description = "Declara prescrito o período aquisitivo não gozado no prazo estatutário.",
      scope = ActionScope.ITEM,
      requiredAuthorities = {"ERGON_WRITE"},
      allowedStates = {"DISPONIVEL", "PARCIALMENTE_GOZADO"},
      riskLevel = ActionRiskLevel.HIGH,
      confirmationRequired = true,
      resourceVersion = ActionRequirement.REQUIRED,
      resourceVersionTransport = ActionResourceVersionTransport.IF_MATCH)
  public ResponseEntity<PeriodoAquisitivoFeriasResponseDTO> prescrever(
      @PathVariable Long id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch,
      @Valid @RequestBody TransicaoPeriodoAquisitivoFeriasDTO dto) {
    return ErgonHttpContract.itemBody(executeVersionedItemCommand("prescrever-periodo-aquisitivo", id, ifMatch, null, dto,
        condition -> service.prescreverPeriodoAquisitivo(id, dto, condition)));
  }

  @PostMapping("/{id}/actions/reativar-periodo-aquisitivo")
  @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(
      summary = "Reativar período aquisitivo suspenso",
      description =
          "Restabelece a contagem e o saldo disponível de um período aquisitivo anteriormente suspenso. Exige If-Match.")
  @WorkflowAction(
      id = "reativar-periodo-aquisitivo",
      title = "Reativar período aquisitivo",
      description = "Retoma a vigência e recalcula a situação operacional do período suspenso.",
      scope = ActionScope.ITEM,
      requiredAuthorities = {"ERGON_WRITE"},
      allowedStates = {"SUSPENSO"},
      confirmationRequired = true,
      resourceVersion = ActionRequirement.REQUIRED,
      resourceVersionTransport = ActionResourceVersionTransport.IF_MATCH)
  public ResponseEntity<PeriodoAquisitivoFeriasResponseDTO> reativar(
      @PathVariable Long id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch,
      @Valid @RequestBody TransicaoPeriodoAquisitivoFeriasDTO dto) {
    return ErgonHttpContract.itemBody(executeVersionedItemCommand("reativar-periodo-aquisitivo", id, ifMatch, null, dto,
        condition -> service.reativarPeriodoAquisitivo(id, dto, condition)));
  }

  @PostMapping("/{id}/actions/cancelar-periodo-aquisitivo")
  @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(
      summary = "Cancelar período aquisitivo de férias",
      description =
          "Cancela administrativamente o período aquisitivo em razão de revisão cadastral ou vacância e zera o saldo disponível. Exige If-Match.")
  @WorkflowAction(
      id = "cancelar-periodo-aquisitivo",
      title = "Cancelar período aquisitivo",
      description = "Inativa administrativamente o ciclo aquisitivo e zera seu saldo disponível.",
      scope = ActionScope.ITEM,
      requiredAuthorities = {"ERGON_WRITE"},
      allowedStates = {"EM_AQUISICAO", "DISPONIVEL", "PARCIALMENTE_GOZADO", "SUSPENSO"},
      riskLevel = ActionRiskLevel.HIGH,
      confirmationRequired = true,
      resourceVersion = ActionRequirement.REQUIRED,
      resourceVersionTransport = ActionResourceVersionTransport.IF_MATCH)
  public ResponseEntity<PeriodoAquisitivoFeriasResponseDTO> cancelar(
      @PathVariable Long id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch,
      @Valid @RequestBody TransicaoPeriodoAquisitivoFeriasDTO dto) {
    return ErgonHttpContract.itemBody(executeVersionedItemCommand("cancelar-periodo-aquisitivo", id, ifMatch, null, dto,
        condition -> service.cancelarPeriodoAquisitivo(id, dto, condition)));
  }

  @PostMapping("/{id}/actions/recalcular-faltas-direito-periodo")
  @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(
      summary = "Recalcular faltas apuradas, suspensões e dias de direito do período aquisitivo",
      description =
          "Apura ocorrências homologadas de Frequencia e Afastamento no intervalo aquisitivo (dtini..dtfim), aplica a redução progressiva de direito e recalcula o saldo disponível. Exige If-Match.")
  @WorkflowAction(
      id = "recalcular-faltas-direito-periodo",
      title = "Recalcular faltas e direito",
      description =
          "Apura faltas injustificadas e afastamentos suspensivos do ciclo e recalcula o direito e o saldo de férias.",
      scope = ActionScope.ITEM,
      requiredAuthorities = {"ERGON_WRITE"},
      allowedStates = {"EM_AQUISICAO", "DISPONIVEL", "PARCIALMENTE_GOZADO", "SUSPENSO"},
      confirmationRequired = true,
      resourceVersion = ActionRequirement.REQUIRED,
      resourceVersionTransport = ActionResourceVersionTransport.IF_MATCH)
  public ResponseEntity<PeriodoAquisitivoFeriasResponseDTO> recalcularFaltasDireito(
      @PathVariable Long id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch,
      @Valid @RequestBody TransicaoPeriodoAquisitivoFeriasDTO dto) {
    return ErgonHttpContract.itemBody(executeVersionedItemCommand("recalcular-faltas-direito-periodo", id, ifMatch, null, dto,
        condition -> service.recalcularFaltasDireitoPeriodo(id, dto, condition)));
  }

  @PostMapping("/determinations/previa")
  @PreAuthorize("hasAuthority('ERGON_READ')")
  @Operation(
      operationId = "determineErgonPeriodoAquisitivoPrevia",
      summary = "Determinar servidor, saldo líquido previsto e herança de parâmetros em rascunho",
      description =
          "Projeta o nome do servidor, situação do vínculo, saldo líquido disponível e status de herança de PARAMETROS_CONTA sem persistir.")
  public ResponseEntity<PeriodoAquisitivoPreviaPreviewDTO> determinePrevia(
      @Valid @RequestBody PeriodoAquisitivoPreviaDraftDTO draft) {
    return ResponseEntity.ok(determinations.previaPeriodoAquisitivo(draft));
  }

  @Override
  @PostMapping("/bulk/update/evaluation")
  @Operation(
      operationId = "ergon.periodos-aquisitivos-ferias.bulk.uniform.evaluation",
      summary = "Avaliar edição uniforme de períodos aquisitivos de férias",
      description =
          "Avalia seleção, versões e alterações de períodos aquisitivos sem modificar o cadastro; produz uma proposta protegida.")
  @BulkResourceOperation(BulkResourceOperation.Role.UNIFORM_EVALUATION)
  public org.springframework.http.ResponseEntity<RestApiResponse<BulkProposal>> evaluateBulkUpdate(@RequestBody BulkUniformEvaluationRequest<?, PeriodoAquisitivoFeriasFilterDTO> request) {
    return super.evaluateBulkUpdate(request);
  }

  @Override
  @PostMapping("/bulk/update-items/evaluation")
  @Operation(
      operationId = "ergon.periodos-aquisitivos-ferias.bulk.items.evaluation",
      summary = "Avaliar edições por período aquisitivo de férias",
      description =
          "Avalia deltas distintos e a versão esperada de cada período aquisitivo, com impedimentos individualizados.")
  @BulkResourceOperation(BulkResourceOperation.Role.ITEM_EVALUATION)
  public org.springframework.http.ResponseEntity<RestApiResponse<BulkProposal>> evaluateBulkUpdateItems(@RequestBody BulkItemEvaluationRequest<?> request) {
    return super.evaluateBulkUpdateItems(request);
  }

  @Override
  @PostMapping("/bulk/update")
  @BulkOperation(
      mode = BulkMode.UNIFORM_UPDATE,
      evaluationOperationId = "ergon.periodos-aquisitivos-ferias.bulk.uniform.evaluation",
      atomicity = ActionCollectionAtomicity.PER_ITEM)
  @Operation(
      operationId = "ergon.periodos-aquisitivos-ferias.bulk.uniform",
      summary = "Confirmar edição uniforme de períodos aquisitivos de férias",
      description =
          "Confirma uma proposta avaliada; cada período aquisitivo é revalidado e possui recibo durável.")
  @BulkResourceOperation(BulkResourceOperation.Role.UNIFORM_CONFIRMATION)
  public org.springframework.http.ResponseEntity<RestApiResponse<BulkExecution>> confirmBulkUpdate(@RequestBody BulkConfirmationRequest request, @RequestHeader("Idempotency-Key") String key) {
    return super.confirmBulkUpdate(request, key);
  }

  @Override
  @PostMapping("/bulk/update-items")
  @BulkOperation(
      mode = BulkMode.PER_ITEM_UPDATE,
      evaluationOperationId = "ergon.periodos-aquisitivos-ferias.bulk.items.evaluation",
      atomicity = ActionCollectionAtomicity.PER_ITEM)
  @Operation(
      operationId = "ergon.periodos-aquisitivos-ferias.bulk.items",
      summary = "Confirmar edições por período aquisitivo de férias",
      description =
          "Aplica os deltas individualizados preservados na proposta, com versão e resultado próprios por período aquisitivo.")
  @BulkResourceOperation(BulkResourceOperation.Role.ITEM_CONFIRMATION)
  public org.springframework.http.ResponseEntity<RestApiResponse<BulkExecution>> confirmBulkUpdateItems(@RequestBody BulkConfirmationRequest request, @RequestHeader("Idempotency-Key") String key) {
    return super.confirmBulkUpdateItems(request, key);
  }











  @PostMapping("/bulk/homologar-periodo-aquisitivo-lote/evaluation")
  @Operation(
      operationId =
          "ergon.periodos-aquisitivos-ferias.homologar-periodo-aquisitivo-lote.evaluation",
      summary = "Avaliar homologação coletiva de períodos aquisitivos de férias",
      description =
          "Avalia homologação coletiva de períodos aquisitivos em aquisição ou suspensos do mesmo órgão, sem persistir alterações.")
  public org.springframework.http.ResponseEntity<RestApiResponse<BulkProposal>> bulkHomologarEvaluate(

          @RequestBody
          BulkCommandEvaluationRequest<
                  TransicaoPeriodoAquisitivoFeriasDTO, ?, PeriodoAquisitivoFeriasFilterDTO>
              request) {
    return evaluateBulkDomainCommand("homologar-periodo-aquisitivo-lote", request);
  }

  @PostMapping("/actions/homologar-periodo-aquisitivo-lote")
  @WorkflowAction(
      id = "homologar-periodo-aquisitivo-lote",
      title = "Homologar períodos aquisitivos em lote",
      description =
          "Homologa coletivamente ciclos aquisitivos concluídos liberando saldo para programação.",
      scope = ActionScope.COLLECTION,
      requiredAuthorities = {"ERGON_WRITE"},
      confirmationRequired = true,
      idempotencyKey = ActionRequirement.REQUIRED,
      atomicity = ActionCollectionAtomicity.PER_ITEM,
      outcomeMode = ActionOutcomeMode.PER_ITEM,
      maxSelection = 200)
  @BulkOperation(
      mode = BulkMode.DOMAIN_COMMAND,
      evaluationOperationId =
          "ergon.periodos-aquisitivos-ferias.homologar-periodo-aquisitivo-lote.evaluation",
      atomicity = ActionCollectionAtomicity.PER_ITEM)
  @Operation(
      operationId = "ergon.periodos-aquisitivos-ferias.homologar-periodo-aquisitivo-lote",
      summary = "Homologar períodos aquisitivos em lote",
      description =
          "Executa homologação coletiva governada de períodos aquisitivos de férias.")
  public org.springframework.http.ResponseEntity<RestApiResponse<BulkExecution>> bulkHomologarConfirm(
      @RequestBody BulkConfirmationRequest request,
      @Parameter(description = "Chave estável do ato coletivo para impedir efeitos duplicados.")
          @RequestHeader("Idempotency-Key")
          String key) {
    return confirmBulkDomainCommand("homologar-periodo-aquisitivo-lote", request, key);
  }
  @Override @SuppressWarnings({"unchecked", "rawtypes"})
  protected org.praxisplatform.uischema.bulk.JpaResourceBulkEngine getBulkEngine() { return bulk.engine(Resource.PERIODOS_AQUISITIVOS_FERIAS); }
  @Override protected String bulkProposalOperationId(UUID id) { return bulk.locate(Resource.PERIODOS_AQUISITIVOS_FERIAS, id, false); }
  @Override protected String bulkExecutionOperationId(UUID id) { return bulk.locate(Resource.PERIODOS_AQUISITIVOS_FERIAS, id, true); }

}
