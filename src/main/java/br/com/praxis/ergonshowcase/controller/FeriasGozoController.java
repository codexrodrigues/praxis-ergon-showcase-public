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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Publica o contrato de parcelas de programação e gozo de férias e abono pecuniário (FERIAS / ERG_ABONOS_FERIAS) através da hierarquia canônica Praxis. */
@ResourceFact(
    key = "comAbonoVendido",
    resourceKey = "administracao-pessoal.ferias-gozo",
    label = "Com abono pecuniário",
    description = "Verdadeiro quando a parcela de férias inclui venda de dias em abono pecuniário.",
    dataType = "boolean",
    icon = "payments",
    tone = "info",
    valuePresentation = "badge",
    sourceKind = "computed")
@ResourceFact(
    key = "comSubstitutoDesignado",
    resourceKey = "administracao-pessoal.ferias-gozo",
    label = "Com substituto designado",
    description = "Verdadeiro quando há servidor substituto formalmente vinculado à parcela de férias.",
    dataType = "boolean",
    icon = "supervisor_account",
    tone = "success",
    valuePresentation = "badge",
    sourceKind = "computed")
@ResourceFact(
    key = "parcelaInterrompida",
    resourceKey = "administracao-pessoal.ferias-gozo",
    label = "Parcela interrompida",
    description = "Verdadeiro quando o gozo da parcela sofreu interrupção administrativa formal.",
    dataType = "boolean",
    icon = "pause_circle",
    tone = "danger",
    valuePresentation = "badge",
    sourceKind = "computed")
@BulkResourceOperations(
    proposalOperationId = "ergon.ferias-gozo.bulk.proposal",
    proposalResultsOperationId = "ergon.ferias-gozo.bulk.proposal-results",
    executionOperationId = "ergon.ferias-gozo.bulk.execution",
    executionResultsOperationId = "ergon.ferias-gozo.bulk.results",
    cancelOperationId = "ergon.ferias-gozo.bulk.cancel",
    updateSourceOperationId = "updateErgonFeriasGozo",
    protectedUpdateFields = {
      "empCodigo",
      "numfunc",
      "numvinc",
      "periodoAquisitivoId",
      "chavePeraqfer",
      "totDiasDireitoPa",
      "ordemParcela",
      "dtini"
    })
@ApiResource(
    value = ApiPaths.FERIAS_GOZO,
    resourceKey = "administracao-pessoal.ferias-gozo",
    title = "Programação e gozo de férias",
    description =
        "Parcelas de programação e gozo de férias, abono pecuniário, adiantamentos financeiros, interrupção e substituição funcional por período aquisitivo (FERIAS / ERG_ABONOS_FERIAS).",
    icon = "beach_access",
    identity =
        @ResourceIdentity(
            keyField = "id",
            titleField = "parcelaResumo",
            displayLabelField = "parcelaResumo",
            subtitleField = "pessoaNome",
            metadataFields = {"orgaoNome", "situacao", "periodoAquisitivoExercicio"}),
    quickFilters = {
      @QuickFilter(
          id = "programadas",
          label = "Programadas",
          filter = "{\"situacoes\":[\"PROGRAMADA\"]}"),
      @QuickFilter(
          id = "homologadas",
          label = "Homologadas",
          filter = "{\"situacoes\":[\"HOMOLOGADA\"]}"),
      @QuickFilter(
          id = "em-gozo",
          label = "Em gozo",
          filter = "{\"situacoes\":[\"EM_GOZO\"]}"),
      @QuickFilter(
          id = "com-abono-pecuniario",
          label = "Com abono pecuniário",
          filter = "{\"abonoPecuniario\":true}"),
      @QuickFilter(
          id = "interrompidas",
          label = "Interrompidas",
          filter = "{\"situacoes\":[\"INTERROMPIDA\"]}")
    })
@ApiGroup(
    value = "ergon",
    title = "Administração de pessoal",
    description = "Cadastros civis e assentamentos funcionais dos órgãos públicos.",
    icon = "groups")
public class FeriasGozoController
    extends org.praxisplatform.uischema.controller.base.AbstractBulkGovernedResourceController<
        FeriasGozoResponseDTO,
        Long,
        FeriasGozoFilterDTO,
        CreateFeriasGozoDTO,
        UpdateFeriasGozoDTO> {

  private final FeriasGozoService service;
  @Autowired private ErgonBulkOperations bulk;
  @Autowired private ErgonDeterminationService determinations;

  public FeriasGozoController(
      FeriasGozoService service,
      ResourceVersionEtagService etags,
      ResourceVersionScopeProvider scopes) {
    this.service = service;
    configureResourceVersioning(etags, scopes);
  }

  @Override
  @PostMapping("/stats/group-by")
  @Operation(
      summary = "Parcelas de gozo de férias por situação e dias programados",
      description =
          "Agrega o conjunto autorizado e filtrado de parcelas de férias com abertura direta da ficha pela identidade real da parcela.")
  @UiAnalytics(
      projections = {
        @AnalyticsProjection(
            id = "ferias-gozo-por-situacao",
            intent = AnalyticsIntent.COMPOSITION,
            sourceOperation = AnalyticsOperation.GROUP_BY,
            primaryDimension = @AnalyticsDimensionBinding(field = "situacao"),
            primaryMetrics =
                @AnalyticsMetricBinding(
                    field = "count",
                    aggregation = "count",
                    label = "Parcelas de férias"),
            defaultLimit = 20),
        @AnalyticsProjection(
            id = "dias-gozo-por-parcela",
            intent = AnalyticsIntent.RANKING,
            sourceOperation = AnalyticsOperation.GROUP_BY,
            primaryDimension = @AnalyticsDimensionBinding(field = "id"),
            primaryMetrics =
                @AnalyticsMetricBinding(
                    field = "diasGozo",
                    aggregation = "sum",
                    label = "Dias de gozo"),
            defaultLimit = 20,
            recordOpen =
                @AnalyticsRecordOpen(
                    sourceIdentityField = "key",
                    target =
                        @AnalyticsSurfaceTarget(
                            resourceKey = "administracao-pessoal.ferias-gozo",
                            surfaceId = "ficha-ferias-gozo"))),
        @AnalyticsProjection(
            id = "evolucao-mensal-gozos-ferias",
            intent = AnalyticsIntent.TREND,
            sourceOperation = AnalyticsOperation.TIMESERIES,
            primaryDimension = @AnalyticsDimensionBinding(field = "dtini"),
            defaultGranularity = AnalyticsGranularity.MONTH,
            defaultTimezone = "America/Sao_Paulo",
            primaryMetrics =
                @AnalyticsMetricBinding(
                    field = "count",
                    aggregation = "count",
                    label = "Parcelas iniciadas no mês")),
        @AnalyticsProjection(
            id = "distribuicao-duracao-parcelas-gozo",
            intent = AnalyticsIntent.DISTRIBUTION,
            sourceOperation = AnalyticsOperation.DISTRIBUTION,
            primaryDimension = @AnalyticsDimensionBinding(field = "diasGozo"),
            primaryMetrics =
                @AnalyticsMetricBinding(
                    field = "diasGozo",
                    aggregation = "histogram",
                    label = "Distribuição de dias de gozo")),
        @AnalyticsProjection(
            id = "comparativo-parcelas-por-situacao",
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
                    label = "Variação de parcelas de férias"))
      })
  public ResponseEntity<RestApiResponse<GroupByStatsResponse>> groupByStats(
      @RequestBody GroupByStatsRequest<FeriasGozoFilterDTO> request) {
    return super.groupByStats(request);
  }

  @Override
  @PostMapping
  @Operation(
      operationId = "createErgonFeriasGozo",
      summary = "Programar parcela de gozo de férias",
      description =
          "Inclui nova parcela de programação de férias e abono pecuniário validando período aquisitivo, saldo disponível e limites estatutários.")
  public ResponseEntity<RestApiResponse<FeriasGozoResponseDTO>> create(
      @Valid @RequestBody CreateFeriasGozoDTO dto) {
    return super.create(dto);
  }

  @Override
  @PutMapping("/{id}")
  @BulkResourceOperation(BulkResourceOperation.Role.UPDATE_SOURCE)
  @Operation(
      operationId = "updateErgonFeriasGozo",
      summary = "Atualizar parcela de gozo de férias",
      description =
          "Atualiza período, abono pecuniário, efeitos financeiros, folha e substituição da parcela de férias com If-Match.")
  public ResponseEntity<RestApiResponse<FeriasGozoResponseDTO>> update(
      @PathVariable Long id, @Valid @RequestBody UpdateFeriasGozoDTO dto) {
    return super.update(id, dto);
  }

  @Override
  protected FeriasGozoService getService() {
    return service;
  }

  @Override
  protected Long getResponseId(FeriasGozoResponseDTO dto) {
    return dto.getId();
  }

  @GetMapping("/{id}/ficha-ferias-gozo")
  @PreAuthorize("hasAuthority('ERGON_READ')")
  @Operation(
      summary = "Consultar ficha 360° da parcela de gozo de férias",
      description =
          "Retorna a projeção completa de leitura da parcela de férias e abono pecuniário para o drawer Quick View.")
  @UiSurface(
      id = "ficha-ferias-gozo",
      kind = SurfaceKind.READ_PROJECTION,
      scope = SurfaceScope.ITEM,
      title = "Ficha da parcela de férias",
      description =
          "Visão consolidada do período de gozo, abono pecuniário, efeitos financeiros, substituição e atos formais.",
      intent = "summary",
      requiredAuthorities = {"ERGON_READ"})
  public ResponseEntity<FeriasGozoResponseDTO> ficha(@PathVariable Long id) {
    return withResourceVersion(ResponseEntity.ok(), id, service.findById(id));
  }

  @PatchMapping("/{id}/abono-financeiro")
  @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(
      operationId = "patchFeriasGozoAbonoFinanceiro",
      summary = "Atualizar abono pecuniário, terço constitucional e adiantamentos",
      description =
          "Atualiza venda de abono pecuniário, terço constitucional e adiantamentos financeiros da parcela com If-Match.")
  @ResourceIntent(
      id = "abono-financeiro",
      title = "Abono pecuniário e financeiro",
      description = "Manutenção parcial do abono pecuniário, terço constitucional e adiantamentos.")
  @UiSurface(
      id = "abono-financeiro",
      kind = SurfaceKind.PARTIAL_FORM,
      scope = SurfaceScope.ITEM,
      title = "Abono pecuniário e financeiro",
      description =
          "Revisão focalizada das opções de venda de abono pecuniário e adiantamentos de folha.",
      intent = "abono-financeiro",
      requiredAuthorities = {"ERGON_WRITE"})
  public ResponseEntity<FeriasGozoResponseDTO> atualizarAbonoFinanceiro(
      @PathVariable Long id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch,
      @Valid @RequestBody AtualizarAbonoFinanceiroFeriasGozoDTO dto) {
    return ErgonHttpContract.itemBody(executeVersionedItemPatch("abono-financeiro", id, ifMatch, condition -> service.atualizarAbonoFinanceiro(id, dto, condition)));
  }

  @PatchMapping("/{id}/substituto-folha")
  @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(
      operationId = "patchFeriasGozoSubstitutoFolha",
      summary = "Atualizar competência de folha, substituto e atos legais",
      description =
          "Atualiza competência/número da folha, servidor substituto e atos legais da parcela de férias com If-Match.")
  @ResourceIntent(
      id = "substituto-folha",
      title = "Folha, substituto e atos legais",
      description = "Manutenção parcial da competência de folha, substituição e portaria.")
  @UiSurface(
      id = "substituto-folha",
      kind = SurfaceKind.PARTIAL_FORM,
      scope = SurfaceScope.ITEM,
      title = "Folha, substituto e atos legais",
      description = "Revisão focalizada de folha de pagamento, substituto funcional e publicação.",
      intent = "substituto-folha",
      requiredAuthorities = {"ERGON_WRITE"})
  public ResponseEntity<FeriasGozoResponseDTO> atualizarSubstitutoFolha(
      @PathVariable Long id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch,
      @Valid @RequestBody AtualizarSubstitutoFolhaFeriasGozoDTO dto) {
    return ErgonHttpContract.itemBody(executeVersionedItemPatch("substituto-folha", id, ifMatch, condition -> service.atualizarSubstitutoFolha(id, dto, condition)));
  }

  @PostMapping("/{id}/actions/homologar-ferias-gozo")
  @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(
      summary = "Homologar programação de parcela de férias",
      description =
          "Aprova e homologa a parcela de férias programada mediante portaria ou escala oficial. Exige If-Match.")
  @WorkflowAction(
      id = "homologar-ferias-gozo",
      title = "Homologar parcela de férias",
      description = "Homologa a programação de gozo de férias na escala oficial do órgão.",
      scope = ActionScope.ITEM,
      requiredAuthorities = {"ERGON_WRITE"},
      allowedStates = {"PROGRAMADA"},
      confirmationRequired = true,
      resourceVersion = ActionRequirement.REQUIRED,
      resourceVersionTransport = ActionResourceVersionTransport.IF_MATCH)
  public ResponseEntity<FeriasGozoResponseDTO> homologar(
      @PathVariable Long id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch,
      @Valid @RequestBody TransicaoFeriasGozoDTO dto) {
    return ErgonHttpContract.itemBody(executeVersionedItemCommand("homologar-ferias-gozo", id, ifMatch, null, dto,
        condition -> service.homologarFeriasGozo(id, dto, condition)));
  }

  @PostMapping("/{id}/actions/iniciar-ferias-gozo")
  @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(
      summary = "Registrar início de gozo de férias",
      description =
          "Transita a parcela homologada para EM_GOZO no início do afastamento para descanso. Exige If-Match.")
  @WorkflowAction(
      id = "iniciar-ferias-gozo",
      title = "Iniciar gozo de férias",
      description = "Registra a entrada efetiva em gozo da parcela de férias homologada.",
      scope = ActionScope.ITEM,
      requiredAuthorities = {"ERGON_WRITE"},
      allowedStates = {"HOMOLOGADA"},
      confirmationRequired = true,
      resourceVersion = ActionRequirement.REQUIRED,
      resourceVersionTransport = ActionResourceVersionTransport.IF_MATCH)
  public ResponseEntity<FeriasGozoResponseDTO> iniciarGozo(
      @PathVariable Long id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch,
      @Valid @RequestBody TransicaoFeriasGozoDTO dto) {
    return ErgonHttpContract.itemBody(executeVersionedItemCommand("iniciar-ferias-gozo", id, ifMatch, null, dto,
        condition -> service.iniciarFeriasGozo(id, dto, condition)));
  }

  @PostMapping("/{id}/actions/concluir-ferias-gozo")
  @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(
      summary = "Concluir gozo da parcela de férias",
      description =
          "Encerra o gozo da parcela de férias na data final efetiva e consolida o retorno do servidor. Exige If-Match.")
  @WorkflowAction(
      id = "concluir-ferias-gozo",
      title = "Concluir gozo de férias",
      description = "Registra a conclusão integral da etapa de gozo de férias.",
      scope = ActionScope.ITEM,
      requiredAuthorities = {"ERGON_WRITE"},
      allowedStates = {"HOMOLOGADA", "EM_GOZO"},
      confirmationRequired = true,
      resourceVersion = ActionRequirement.REQUIRED,
      resourceVersionTransport = ActionResourceVersionTransport.IF_MATCH)
  public ResponseEntity<FeriasGozoResponseDTO> concluirGozo(
      @PathVariable Long id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch,
      @Valid @RequestBody TransicaoFeriasGozoDTO dto) {
    return ErgonHttpContract.itemBody(executeVersionedItemCommand("concluir-ferias-gozo", id, ifMatch, null, dto,
        condition -> service.concluirFeriasGozo(id, dto, condition)));
  }

  @PostMapping("/{id}/actions/interromper-ferias-gozo")
  @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(
      summary = "Interromper gozo de férias por necessidade do serviço",
      description =
          "Registra interrupção formal do gozo de férias mediante ato legal e justificativa administrativa. Exige If-Match.")
  @WorkflowAction(
      id = "interromper-ferias-gozo",
      title = "Interromper férias",
      description = "Suspende o gozo em curso por imperiosa necessidade do serviço público.",
      scope = ActionScope.ITEM,
      requiredAuthorities = {"ERGON_WRITE"},
      allowedStates = {"HOMOLOGADA", "EM_GOZO"},
      riskLevel = ActionRiskLevel.HIGH,
      confirmationRequired = true,
      resourceVersion = ActionRequirement.REQUIRED,
      resourceVersionTransport = ActionResourceVersionTransport.IF_MATCH)
  public ResponseEntity<FeriasGozoResponseDTO> interromper(
      @PathVariable Long id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch,
      @Valid @RequestBody InterromperFeriasGozoDTO dto) {
    return ErgonHttpContract.itemBody(executeVersionedItemCommand("interromper-ferias-gozo", id, ifMatch, null, dto,
        condition -> service.interromperFeriasGozo(id, dto, condition)));
  }

  @PostMapping("/{id}/actions/cancelar-ferias-gozo")
  @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(
      summary = "Cancelar programação de parcela de férias",
      description =
          "Torna sem efeito parcela de férias programada ou homologada ainda não gozada. Exige If-Match.")
  @WorkflowAction(
      id = "cancelar-ferias-gozo",
      title = "Cancelar parcela de férias",
      description = "Cancela administrativamente a programação da parcela de férias.",
      scope = ActionScope.ITEM,
      requiredAuthorities = {"ERGON_WRITE"},
      allowedStates = {"PROGRAMADA", "HOMOLOGADA"},
      riskLevel = ActionRiskLevel.HIGH,
      confirmationRequired = true,
      resourceVersion = ActionRequirement.REQUIRED,
      resourceVersionTransport = ActionResourceVersionTransport.IF_MATCH)
  public ResponseEntity<FeriasGozoResponseDTO> cancelar(
      @PathVariable Long id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch,
      @Valid @RequestBody TransicaoFeriasGozoDTO dto) {
    return ErgonHttpContract.itemBody(executeVersionedItemCommand("cancelar-ferias-gozo", id, ifMatch, null, dto,
        condition -> service.cancelarFeriasGozo(id, dto, condition)));
  }

  @PostMapping("/determinations/previa")
  @PreAuthorize("hasAuthority('ERGON_READ')")
  @Operation(
      operationId = "determineErgonFeriasGozoPrevia",
      summary = "Determinar término previsto, saldo remanescente do PA e elegibilidade de abono em rascunho",
      description =
          "Projeta a chave do período aquisitivo, data prevista de término, saldo remanescente do PA e elegibilidade para abono pecuniário sem persistir.")
  public ResponseEntity<FeriasGozoPreviaPreviewDTO> determinePrevia(
      @Valid @RequestBody FeriasGozoPreviaDraftDTO draft) {
    return ResponseEntity.ok(determinations.previaFeriasGozo(draft));
  }

  @Override
  @PostMapping("/bulk/update/evaluation")
  @Operation(
      operationId = "ergon.ferias-gozo.bulk.uniform.evaluation",
      summary = "Avaliar edição uniforme de parcelas de gozo de férias",
      description =
          "Avalia seleção, versões e alterações de parcelas de férias sem modificar o cadastro; produz uma proposta protegida.")
  @BulkResourceOperation(BulkResourceOperation.Role.UNIFORM_EVALUATION)
  public org.springframework.http.ResponseEntity<RestApiResponse<BulkProposal>> evaluateBulkUpdate(@RequestBody BulkUniformEvaluationRequest<?, FeriasGozoFilterDTO> request) {
    return super.evaluateBulkUpdate(request);
  }

  @Override
  @PostMapping("/bulk/update-items/evaluation")
  @Operation(
      operationId = "ergon.ferias-gozo.bulk.items.evaluation",
      summary = "Avaliar edições por parcela de gozo de férias",
      description =
          "Avalia deltas distintos e a versão esperada de cada parcela de férias, com impedimentos individualizados.")
  @BulkResourceOperation(BulkResourceOperation.Role.ITEM_EVALUATION)
  public org.springframework.http.ResponseEntity<RestApiResponse<BulkProposal>> evaluateBulkUpdateItems(@RequestBody BulkItemEvaluationRequest<?> request) {
    return super.evaluateBulkUpdateItems(request);
  }

  @Override
  @PostMapping("/bulk/update")
  @BulkOperation(
      mode = BulkMode.UNIFORM_UPDATE,
      evaluationOperationId = "ergon.ferias-gozo.bulk.uniform.evaluation",
      atomicity = ActionCollectionAtomicity.PER_ITEM)
  @Operation(
      operationId = "ergon.ferias-gozo.bulk.uniform",
      summary = "Confirmar edição uniforme de parcelas de gozo de férias",
      description =
          "Confirma uma proposta avaliada; cada parcela de férias é revalidada e possui recibo durável.")
  @BulkResourceOperation(BulkResourceOperation.Role.UNIFORM_CONFIRMATION)
  public org.springframework.http.ResponseEntity<RestApiResponse<BulkExecution>> confirmBulkUpdate(@RequestBody BulkConfirmationRequest request, @RequestHeader("Idempotency-Key") String key) {
    return super.confirmBulkUpdate(request, key);
  }

  @Override
  @PostMapping("/bulk/update-items")
  @BulkOperation(
      mode = BulkMode.PER_ITEM_UPDATE,
      evaluationOperationId = "ergon.ferias-gozo.bulk.items.evaluation",
      atomicity = ActionCollectionAtomicity.PER_ITEM)
  @Operation(
      operationId = "ergon.ferias-gozo.bulk.items",
      summary = "Confirmar edições por parcela de gozo de férias",
      description =
          "Aplica os deltas individualizados preservados na proposta, com versão e resultado próprios por parcela.")
  @BulkResourceOperation(BulkResourceOperation.Role.ITEM_CONFIRMATION)
  public org.springframework.http.ResponseEntity<RestApiResponse<BulkExecution>> confirmBulkUpdateItems(@RequestBody BulkConfirmationRequest request, @RequestHeader("Idempotency-Key") String key) {
    return super.confirmBulkUpdateItems(request, key);
  }











  @PostMapping("/bulk/homologar-ferias-gozo-lote/evaluation")
  @Operation(
      operationId = "ergon.ferias-gozo.homologar-ferias-gozo-lote.evaluation",
      summary = "Avaliar homologação coletiva de parcelas de gozo de férias",
      description =
          "Avalia homologação coletiva de parcelas de férias programadas do mesmo órgão, sem persistir alterações.")
  public org.springframework.http.ResponseEntity<RestApiResponse<BulkProposal>> bulkHomologarEvaluate(

          @RequestBody
          BulkCommandEvaluationRequest<TransicaoFeriasGozoDTO, ?, FeriasGozoFilterDTO>
              request) {
    return evaluateBulkDomainCommand("homologar-ferias-gozo-lote", request);
  }

  @PostMapping("/actions/homologar-ferias-gozo-lote")
  @WorkflowAction(
      id = "homologar-ferias-gozo-lote",
      title = "Homologar parcelas de férias em lote",
      description =
          "Homologa coletivamente parcelas de férias programadas mediante portaria ou escala oficial.",
      scope = ActionScope.COLLECTION,
      requiredAuthorities = {"ERGON_WRITE"},
      confirmationRequired = true,
      idempotencyKey = ActionRequirement.REQUIRED,
      atomicity = ActionCollectionAtomicity.PER_ITEM,
      outcomeMode = ActionOutcomeMode.PER_ITEM,
      maxSelection = 200)
  @BulkOperation(
      mode = BulkMode.DOMAIN_COMMAND,
      evaluationOperationId = "ergon.ferias-gozo.homologar-ferias-gozo-lote.evaluation",
      atomicity = ActionCollectionAtomicity.PER_ITEM)
  @Operation(
      operationId = "ergon.ferias-gozo.homologar-ferias-gozo-lote",
      summary = "Homologar parcelas de férias em lote",
      description = "Executa homologação coletiva governada de parcelas de férias programadas.")
  public org.springframework.http.ResponseEntity<RestApiResponse<BulkExecution>> bulkHomologarConfirm(
      @RequestBody BulkConfirmationRequest request,
      @Parameter(description = "Chave estável do ato coletivo para impedir efeitos duplicados.")
          @RequestHeader("Idempotency-Key")
          String key) {
    return confirmBulkDomainCommand("homologar-ferias-gozo-lote", request, key);
  }
  @Override @SuppressWarnings({"unchecked", "rawtypes"})
  protected org.praxisplatform.uischema.bulk.JpaResourceBulkEngine getBulkEngine() { return bulk.engine(Resource.FERIAS_GOZO); }
  @Override protected String bulkProposalOperationId(UUID id) { return bulk.locate(Resource.FERIAS_GOZO, id, false); }
  @Override protected String bulkExecutionOperationId(UUID id) { return bulk.locate(Resource.FERIAS_GOZO, id, true); }

}
