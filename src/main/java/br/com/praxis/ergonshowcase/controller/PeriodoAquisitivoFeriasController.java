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
    sourceKind = "computed")
@ResourceFact(
    key = "possuiOverridesAtivos",
    resourceKey = "administracao-pessoal.periodos-aquisitivos-ferias",
    label = "Com override individual",
    description = "Verdadeiro quando o período aquisitivo sobrescreve parâmetros herdados de PARAMETROS_CONTA.",
    dataType = "boolean",
    sourceKind = "computed")
@ResourceFact(
    key = "emRiscoPrescricao",
    resourceKey = "administracao-pessoal.periodos-aquisitivos-ferias",
    label = "Em risco de prescrição",
    description = "Verdadeiro quando há saldo disponível e data de prescrição apurada ou prescrição efetivada.",
    dataType = "boolean",
    sourceKind = "computed")
@BulkResourceOperations(
    proposalOperationId = "ergon.periodos-aquisitivos-ferias.bulk.proposal",
    proposalResultsOperationId = "ergon.periodos-aquisitivos-ferias.bulk.proposal-results",
    executionOperationId = "ergon.periodos-aquisitivos-ferias.bulk.execution",
    executionResultsOperationId = "ergon.periodos-aquisitivos-ferias.bulk.results",
    cancelOperationId = "ergon.periodos-aquisitivos-ferias.bulk.cancel",
    updateSourceOperationId = "updateErgonPeriodoAquisitivoFerias",
    protectedUpdateFields = {
      "empCodigo",
      "numfunc",
      "numvinc",
      "chave",
      "dtini",
      "saldoDiasDisponivel",
      "situacao"
    })
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
            metadataFields = {
              "vinculoMatricula",
              "exercicio",
              "situacao",
              "saldoDiasDisponivel"
            }),
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
    extends AbstractResourceController<
        PeriodoAquisitivoFeriasResponseDTO,
        Long,
        PeriodoAquisitivoFeriasFilterDTO,
        CreatePeriodoAquisitivoFeriasDTO,
        UpdatePeriodoAquisitivoFeriasDTO> {

  private final PeriodoAquisitivoFeriasService service;
  private final ResourceVersionEtagService etags;
  private final ResourceVersionScopeProvider scopes;
  @Autowired private FeriasGozoService feriasGozoService;
  @Autowired private ErgonBulkOperations bulk;
  @Autowired private ErgonDeterminationService determinations;

  public PeriodoAquisitivoFeriasController(
      PeriodoAquisitivoFeriasService service,
      ResourceVersionEtagService etags,
      ResourceVersionScopeProvider scopes) {
    this.service = service;
    this.etags = etags;
    this.scopes = scopes;
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
                            surfaceId = "ficha-periodo-aquisitivo")))
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
    service.findById(id);
    if (page < 0 || size < 1 || size > 200)
      throw new ResponseStatusException(
          HttpStatus.UNPROCESSABLE_ENTITY, "Paginação fora do limite permitido.");
    var filter = new FeriasGozoFilterDTO();
    filter.setPeriodoAquisitivoId(id);
    var result =
        feriasGozoService.filter(
            filter, PageRequest.of(page, size, feriasGozoService.getDefaultSort()), null);
    return ResponseEntity.ok(
        RestApiResponse.success(
            result, Links.of(Link.of(ApiPaths.FERIAS_GOZO, "collection"))));
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
    var p =
        new ResourceVersionUpdatePrecondition<Long>(
            etags,
            ifMatch,
            scopes.currentScope(),
            "administracao-pessoal.periodos-aquisitivos-ferias",
            id);
    return withResourceVersion(ResponseEntity.ok(), id, service.atualizarOverrides(id, dto, p));
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
    var p =
        new ResourceVersionUpdatePrecondition<Long>(
            etags,
            ifMatch,
            scopes.currentScope(),
            "administracao-pessoal.periodos-aquisitivos-ferias",
            id);
    return withResourceVersion(
        ResponseEntity.ok(), id, service.atualizarPrescricaoMulta(id, dto, p));
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
    var p =
        new ResourceVersionUpdatePrecondition<Long>(
            etags,
            ifMatch,
            scopes.currentScope(),
            "administracao-pessoal.periodos-aquisitivos-ferias",
            id);
    return withResourceVersion(
        ResponseEntity.ok(), id, service.homologarPeriodoAquisitivo(id, dto, p));
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
    var p =
        new ResourceVersionUpdatePrecondition<Long>(
            etags,
            ifMatch,
            scopes.currentScope(),
            "administracao-pessoal.periodos-aquisitivos-ferias",
            id);
    return withResourceVersion(
        ResponseEntity.ok(), id, service.suspenderPeriodoAquisitivo(id, dto, p));
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
    var p =
        new ResourceVersionUpdatePrecondition<Long>(
            etags,
            ifMatch,
            scopes.currentScope(),
            "administracao-pessoal.periodos-aquisitivos-ferias",
            id);
    return withResourceVersion(
        ResponseEntity.ok(), id, service.converterDobroPeriodoAquisitivo(id, dto, p));
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
    var p =
        new ResourceVersionUpdatePrecondition<Long>(
            etags,
            ifMatch,
            scopes.currentScope(),
            "administracao-pessoal.periodos-aquisitivos-ferias",
            id);
    return withResourceVersion(
        ResponseEntity.ok(), id, service.prescreverPeriodoAquisitivo(id, dto, p));
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

  @PostMapping("/bulk/update/evaluation")
  @Operation(
      operationId = "ergon.periodos-aquisitivos-ferias.bulk.uniform.evaluation",
      summary = "Avaliar edição uniforme de períodos aquisitivos de férias",
      description =
          "Avalia seleção, versões e alterações de períodos aquisitivos sem modificar o cadastro; produz uma proposta protegida.")
  public RestApiResponse<BulkProposal> bulkEvaluate(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              content =
                  @io.swagger.v3.oas.annotations.media.Content(
                      schema =
                          @io.swagger.v3.oas.annotations.media.Schema(
                              implementation =
                                  ErgonBulkSchema.PeriodoAquisitivoFeriasUniform.class)))
          @RequestBody
          BulkUniformEvaluationRequest<String, PeriodoAquisitivoFeriasFilterDTO> request) {
    return RestApiResponse.success(
        bulk.uniform(Resource.PERIODOS_AQUISITIVOS_FERIAS, request), null);
  }

  @PostMapping("/bulk/update-items/evaluation")
  @Operation(
      operationId = "ergon.periodos-aquisitivos-ferias.bulk.items.evaluation",
      summary = "Avaliar edições por período aquisitivo de férias",
      description =
          "Avalia deltas distintos e a versão esperada de cada período aquisitivo, com impedimentos individualizados.")
  public RestApiResponse<BulkProposal> bulkEvaluateItems(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              content =
                  @io.swagger.v3.oas.annotations.media.Content(
                      schema =
                          @io.swagger.v3.oas.annotations.media.Schema(
                              implementation =
                                  ErgonBulkSchema.PeriodoAquisitivoFeriasItems.class)))
          @RequestBody
          BulkItemEvaluationRequest<String> request) {
    return RestApiResponse.success(
        bulk.items(Resource.PERIODOS_AQUISITIVOS_FERIAS, request), null);
  }

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
  public RestApiResponse<BulkExecution> bulkConfirm(
      @RequestBody BulkConfirmationRequest request,
      @Parameter(
              description =
                  "Chave estável desta confirmação; repetir a mesma chave recupera a execução.")
          @RequestHeader("Idempotency-Key")
          String key) {
    return RestApiResponse.success(
        bulk.confirm(
            Resource.PERIODOS_AQUISITIVOS_FERIAS,
            "ergon.periodos-aquisitivos-ferias.bulk.uniform",
            request,
            key),
        null);
  }

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
  public RestApiResponse<BulkExecution> bulkConfirmItems(
      @RequestBody BulkConfirmationRequest request,
      @Parameter(
              description =
                  "Chave estável de confirmação para recuperar a mesma execução em caso de repetição.")
          @RequestHeader("Idempotency-Key")
          String key) {
    return RestApiResponse.success(
        bulk.confirm(
            Resource.PERIODOS_AQUISITIVOS_FERIAS,
            "ergon.periodos-aquisitivos-ferias.bulk.items",
            request,
            key),
        null);
  }

  @GetMapping("/bulk/proposals/{proposalId}")
  @BulkResourceOperation(BulkResourceOperation.Role.PROPOSAL)
  @Operation(
      operationId = "ergon.periodos-aquisitivos-ferias.bulk.proposal",
      summary = "Consultar proposta de lote de períodos aquisitivos",
      description =
          "Consulta totais e intenção pública da proposta após conferir acesso ao conjunto completo de períodos aquisitivos.")
  public RestApiResponse<BulkProposal> bulkProposal(
      @Parameter(description = "Identidade da proposta avaliada.") @PathVariable UUID proposalId) {
    return RestApiResponse.success(
        bulk.proposal(
            Resource.PERIODOS_AQUISITIVOS_FERIAS,
            bulk.locate(Resource.PERIODOS_AQUISITIVOS_FERIAS, proposalId, false),
            proposalId),
        null);
  }

  @GetMapping("/bulk/proposals/{proposalId}/results")
  @BulkResourceOperation(BulkResourceOperation.Role.PROPOSAL_RESULTS)
  @Operation(
      operationId = "ergon.periodos-aquisitivos-ferias.bulk.proposal-results",
      summary = "Inspecionar avaliação do lote de períodos aquisitivos",
      description =
          "Pagina períodos aquisitivos executáveis e bloqueados, com diagnósticos seguros e cursor vinculado ao solicitante.")
  public RestApiResponse<CursorPage<BulkProposalItemResult<Object>>> bulkProposalResults(
      @Parameter(description = "Identidade da proposta avaliada.") @PathVariable UUID proposalId,
      @Parameter(description = "Quantidade de registros por página, de 1 a 200.")
          @RequestParam(defaultValue = "50")
          int size,
      @Parameter(description = "Cursor opaco emitido na página anterior.")
          @RequestParam(required = false)
          String after) {
    return RestApiResponse.success(
        bulk.proposalResults(
            Resource.PERIODOS_AQUISITIVOS_FERIAS,
            bulk.locate(Resource.PERIODOS_AQUISITIVOS_FERIAS, proposalId, false),
            proposalId,
            size,
            after),
        null);
  }

  @GetMapping("/bulk/executions/{executionId}")
  @BulkResourceOperation(BulkResourceOperation.Role.EXECUTION)
  @Operation(
      operationId = "ergon.periodos-aquisitivos-ferias.bulk.execution",
      summary = "Consultar execução do lote de períodos aquisitivos",
      description = "Retorna estado e contadores reconciliados a partir dos recibos persistidos.")
  public RestApiResponse<BulkExecution> bulkExecution(
      @Parameter(description = "Identidade da execução confirmada.")
          @PathVariable
          UUID executionId) {
    return RestApiResponse.success(
        bulk.execution(
            Resource.PERIODOS_AQUISITIVOS_FERIAS,
            bulk.locate(Resource.PERIODOS_AQUISITIVOS_FERIAS, executionId, true),
            executionId),
        null);
  }

  @GetMapping("/bulk/executions/{executionId}/results")
  @BulkResourceOperation(BulkResourceOperation.Role.EXECUTION_RESULTS)
  @Operation(
      operationId = "ergon.periodos-aquisitivos-ferias.bulk.results",
      summary = "Consultar recibos do lote de períodos aquisitivos",
      description =
          "Pagina resultados por período aquisitivo sem expor dados sensíveis da proposta.")
  public RestApiResponse<CursorPage<BulkItemResult<Object>>> bulkResults(
      @Parameter(description = "Identidade da execução confirmada.")
          @PathVariable
          UUID executionId,
      @Parameter(description = "Quantidade de recibos por página, de 1 a 200.")
          @RequestParam(defaultValue = "50")
          int size,
      @Parameter(description = "Cursor opaco da página anterior de recibos.")
          @RequestParam(required = false)
          String after) {
    return RestApiResponse.success(
        bulk.results(
            Resource.PERIODOS_AQUISITIVOS_FERIAS,
            bulk.locate(Resource.PERIODOS_AQUISITIVOS_FERIAS, executionId, true),
            executionId,
            size,
            after),
        null);
  }

  @PostMapping("/bulk/executions/{executionId}/cancel")
  @BulkResourceOperation(BulkResourceOperation.Role.CANCEL)
  @Operation(
      operationId = "ergon.periodos-aquisitivos-ferias.bulk.cancel",
      summary = "Solicitar parada do lote de períodos aquisitivos",
      description =
          "Registra a solicitação de parada sem desfazer alterações já confirmadas e sem reabrir efeitos no replay.")
  public RestApiResponse<BulkExecution> bulkCancel(
      @Parameter(description = "Identidade da execução a interromper.")
          @PathVariable
          UUID executionId) {
    return RestApiResponse.success(
        bulk.cancel(
            Resource.PERIODOS_AQUISITIVOS_FERIAS,
            bulk.locate(Resource.PERIODOS_AQUISITIVOS_FERIAS, executionId, true),
            executionId),
        null);
  }

  @PostMapping("/bulk/homologar-periodo-aquisitivo-lote/evaluation")
  @Operation(
      operationId =
          "ergon.periodos-aquisitivos-ferias.homologar-periodo-aquisitivo-lote.evaluation",
      summary = "Avaliar homologação coletiva de períodos aquisitivos de férias",
      description =
          "Avalia homologação coletiva de períodos aquisitivos em aquisição ou suspensos do mesmo órgão, sem persistir alterações.")
  public RestApiResponse<BulkProposal> bulkHomologarEvaluate(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              content =
                  @io.swagger.v3.oas.annotations.media.Content(
                      schema =
                          @io.swagger.v3.oas.annotations.media.Schema(
                              implementation =
                                  ErgonBulkSchema.HomologarPeriodoAquisitivoFeriasCommand.class)))
          @RequestBody
          BulkCommandEvaluationRequest<
                  TransicaoPeriodoAquisitivoFeriasDTO, String, PeriodoAquisitivoFeriasFilterDTO>
              request) {
    return RestApiResponse.success(
        bulk.command(
            Resource.PERIODOS_AQUISITIVOS_FERIAS, "homologar-periodo-aquisitivo-lote", request),
        null);
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
  public RestApiResponse<BulkExecution> bulkHomologarConfirm(
      @RequestBody BulkConfirmationRequest request,
      @Parameter(description = "Chave estável do ato coletivo para impedir efeitos duplicados.")
          @RequestHeader("Idempotency-Key")
          String key) {
    return RestApiResponse.success(
        bulk.confirm(
            Resource.PERIODOS_AQUISITIVOS_FERIAS,
            "ergon.periodos-aquisitivos-ferias.homologar-periodo-aquisitivo-lote",
            request,
            key),
        null);
  }
}
