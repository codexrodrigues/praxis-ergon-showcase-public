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

/** Publica o contrato de licenças e afastamentos através da hierarquia canônica Praxis. */
@ResourceFact(
    key = "afastamentoEmCurso",
    resourceKey = "administracao-pessoal.afastamentos",
    label = "Afastamento em curso",
    description = "Verdadeiro quando o afastamento está homologado e sem data de término efetivo.",
    dataType = "boolean",
    icon = "event_busy",
    tone = "warning",
    valuePresentation = "badge",
    sourceKind = "computed")
@ResourceFact(
    key = "comSubstitutoDesignado",
    resourceKey = "administracao-pessoal.afastamentos",
    label = "Com substituto designado",
    description = "Verdadeiro quando há servidor substituto formalmente vinculado ao período.",
    dataType = "boolean",
    icon = "swap_horiz",
    tone = "info",
    valuePresentation = "badge",
    sourceKind = "computed")
@ResourceFact(
    key = "pendenteHomologacao",
    resourceKey = "administracao-pessoal.afastamentos",
    label = "Pendente de homologação",
    description = "Verdadeiro quando o registro aguarda deliberação da chefia ou junta médica.",
    dataType = "boolean",
    icon = "pending_actions",
    tone = "warning",
    valuePresentation = "badge",
    sourceKind = "computed")
@BulkResourceOperations(
    proposalOperationId = "ergon.afastamentos.bulk.proposal",
    proposalResultsOperationId = "ergon.afastamentos.bulk.proposal-results",
    executionOperationId = "ergon.afastamentos.bulk.execution",
    executionResultsOperationId = "ergon.afastamentos.bulk.results",
    cancelOperationId = "ergon.afastamentos.bulk.cancel",
    updateSourceOperationId = "updateErgonAfastamento",
    protectedUpdateFields = {"tipofreq", "codfreq", "dtini"})
@ApiResource(
    value = ApiPaths.AFASTAMENTOS,
    resourceKey = "administracao-pessoal.afastamentos",
    title = "Licenças e afastamentos",
    description =
        "Licenças e afastamentos funcionais por vínculo e período, com workflow de homologação, substituição e atos formais (LIC_AFAST).",
    icon = "event_busy",
    identity =
        @ResourceIdentity(
            keyField = "id",
            titleField = "motivo",
            displayLabelField = "motivo",
            subtitleField = "pessoaNome",
            metadataFields = {"orgaoNome", "codigoFrequenciaNome"}),
    quickFilters = {
      @QuickFilter(
          id = "homologados",
          label = "Homologados",
          filter = "{\"situacoes\":[\"HOMOLOGADO\"]}"),
      @QuickFilter(
          id = "pendentes",
          label = "Pendentes de homologação",
          filter = "{\"situacoes\":[\"PENDENTE_HOMOLOGACAO\"]}"),
      @QuickFilter(
          id = "em-aberto",
          label = "Em aberto",
          filter = "{\"somenteEmAberto\":true}"),
      @QuickFilter(
          id = "prorrogacoes",
          label = "Prorrogações",
          filter = "{\"somenteProrrogacoes\":true}"),
      @QuickFilter(
          id = "com-substituto",
          label = "Com substituto",
          filter = "{\"comSubstituto\":true}")
    })
@ApiGroup(
    value = "ergon",
    title = "Administração de pessoal",
    description = "Cadastros civis e assentamentos funcionais dos órgãos públicos.",
    icon = "groups")
public class AfastamentoController
    extends org.praxisplatform.uischema.controller.base.AbstractBulkGovernedResourceController<
        AfastamentoResponseDTO,
        Long,
        AfastamentoFilterDTO,
        CreateAfastamentoDTO,
        UpdateAfastamentoDTO> {

  private final AfastamentoService service;
  @Autowired private ErgonBulkOperations bulk;
  @Autowired private ErgonDeterminationService determinations;

  public AfastamentoController(
      AfastamentoService service,
      ResourceVersionEtagService etags,
      ResourceVersionScopeProvider scopes) {
    this.service = service;
    configureResourceVersioning(etags, scopes);
  }

  @Override
  @PostMapping("/stats/group-by")
  @Operation(
      summary = "Licenças e afastamentos por situação e duração",
      description =
          "Agrega o conjunto autorizado e filtrado de afastamentos com abertura direta da ficha pela identidade real do bucket.")
  @UiAnalytics(
      projections = {
        @AnalyticsProjection(
            id = "afastamentos-por-situacao",
            intent = AnalyticsIntent.COMPOSITION,
            sourceOperation = AnalyticsOperation.GROUP_BY,
            primaryDimension = @AnalyticsDimensionBinding(field = "situacao"),
            primaryMetrics =
                @AnalyticsMetricBinding(
                    field = "count",
                    aggregation = "count",
                    label = "Afastamentos"),
            defaultLimit = 20),
        @AnalyticsProjection(
            id = "dias-por-afastamento",
            intent = AnalyticsIntent.RANKING,
            sourceOperation = AnalyticsOperation.GROUP_BY,
            primaryDimension = @AnalyticsDimensionBinding(field = "id"),
            primaryMetrics =
                @AnalyticsMetricBinding(
                    field = "quantidadeDias",
                    aggregation = "sum",
                    label = "Dias de afastamento"),
            defaultLimit = 20,
            recordOpen =
                @AnalyticsRecordOpen(
                    sourceIdentityField = "key",
                    target =
                        @AnalyticsSurfaceTarget(
                            resourceKey = "administracao-pessoal.afastamentos",
                            surfaceId = "ficha-afastamento"))),
        @AnalyticsProjection(
            id = "evolucao-mensal-afastamentos",
            intent = AnalyticsIntent.TREND,
            sourceOperation = AnalyticsOperation.TIMESERIES,
            primaryDimension = @AnalyticsDimensionBinding(field = "dtini"),
            defaultGranularity = AnalyticsGranularity.MONTH,
            defaultTimezone = "America/Sao_Paulo",
            primaryMetrics =
                @AnalyticsMetricBinding(
                    field = "count",
                    aggregation = "count",
                    label = "Afastamentos iniciados no mês")),
        @AnalyticsProjection(
            id = "distribuicao-duracao-afastamentos",
            intent = AnalyticsIntent.DISTRIBUTION,
            sourceOperation = AnalyticsOperation.DISTRIBUTION,
            primaryDimension = @AnalyticsDimensionBinding(field = "quantidadeDias"),
            primaryMetrics =
                @AnalyticsMetricBinding(
                    field = "quantidadeDias",
                    aggregation = "histogram",
                    label = "Distribuição de duração (dias)")),
        @AnalyticsProjection(
            id = "comparativo-afastamentos-por-situacao",
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
                    label = "Variação por situação"))
      })
  public ResponseEntity<RestApiResponse<GroupByStatsResponse>> groupByStats(
      @RequestBody GroupByStatsRequest<AfastamentoFilterDTO> request) {
    return super.groupByStats(request);
  }

  @Override
  @PostMapping
  @Operation(
      operationId = "createErgonAfastamento",
      summary = "Registrar solicitação de licença ou afastamento",
      description =
          "Inclui novo registro de licença ou afastamento em estado PENDENTE_HOMOLOGACAO validando vínculo, órgão e modalidade.")
  public ResponseEntity<RestApiResponse<AfastamentoResponseDTO>> create(
      @Valid @RequestBody CreateAfastamentoDTO dto) {
    return super.create(dto);
  }

  @Override
  @PutMapping("/{id}")
  @BulkResourceOperation(BulkResourceOperation.Role.UPDATE_SOURCE)
  @Operation(
      operationId = "updateErgonAfastamento",
      summary = "Atualizar licença ou afastamento",
      description = "Atualiza dados operacionais do afastamento com controle otimista If-Match.")
  public ResponseEntity<RestApiResponse<AfastamentoResponseDTO>> update(
      @PathVariable Long id, @Valid @RequestBody UpdateAfastamentoDTO dto) {
    return super.update(id, dto);
  }

  @Override
  protected AfastamentoService getService() {
    return service;
  }

  @Override
  protected Long getResponseId(AfastamentoResponseDTO dto) {
    return dto.getId();
  }

  @GetMapping("/{id}/ficha-afastamento")
  @PreAuthorize("hasAuthority('ERGON_READ')")
  @Operation(
      summary = "Consultar ficha 360° do afastamento",
      description =
          "Retorna a projeção completa de leitura da licença ou afastamento para o drawer Quick View.")
  @UiSurface(
      id = "ficha-afastamento",
      kind = SurfaceKind.READ_PROJECTION,
      scope = SurfaceScope.ITEM,
      title = "Ficha do afastamento",
      description =
          "Visão consolidada do período, enquadramento, substituição funcional e atos legais da licença.",
      intent = "summary",
      requiredAuthorities = {"ERGON_READ"})
  public ResponseEntity<AfastamentoResponseDTO> ficha(@PathVariable Long id) {
    return withResourceVersion(ResponseEntity.ok(), id, service.findById(id));
  }

  @PatchMapping("/{id}/substituto-funcional")
  @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(
      operationId = "patchAfastamentoSubstituto",
      summary = "Designar ou revisar substituto funcional",
      description =
          "Atualiza o servidor e vínculo designados para substituição durante o afastamento com If-Match.")
  @ResourceIntent(
      id = "substituto-funcional",
      title = "Substituto funcional",
      description = "Manutenção parcial da designação de servidor substituto no período.")
  @UiSurface(
      id = "substituto-funcional",
      kind = SurfaceKind.PARTIAL_FORM,
      scope = SurfaceScope.ITEM,
      title = "Substituto funcional",
      description = "Designação ou dispensa de substituto funcional durante a licença.",
      intent = "substituto-funcional",
      requiredAuthorities = {"ERGON_WRITE"})
  public ResponseEntity<AfastamentoResponseDTO> atualizarSubstituto(
      @PathVariable Long id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch,
      @Valid @RequestBody AtualizarSubstitutoAfastamentoDTO dto) {
    return ErgonHttpContract.itemBody(executeVersionedItemPatch("substituto-funcional", id, ifMatch, condition -> service.atualizarSubstituto(id, dto, condition)));
  }

  @PatchMapping("/{id}/atos-publicacao")
  @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(
      operationId = "patchAfastamentoAtosPublicacao",
      summary = "Atualizar portaria, publicação e prorrogação",
      description =
          "Atualiza portaria de concessão, ato de publicação oficial e referência de prorrogação com If-Match.")
  @ResourceIntent(
      id = "atos-publicacao",
      title = "Atos e publicação oficial",
      description = "Manutenção parcial dos documentos legais, publicação e origem de prorrogação.")
  @UiSurface(
      id = "atos-publicacao",
      kind = SurfaceKind.PARTIAL_FORM,
      scope = SurfaceScope.ITEM,
      title = "Atos e publicação oficial",
      description = "Revisão focalizada de portaria, publicação oficial e referência de origem.",
      intent = "atos-publicacao",
      requiredAuthorities = {"ERGON_WRITE"})
  public ResponseEntity<AfastamentoResponseDTO> atualizarAtosPublicacao(
      @PathVariable Long id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch,
      @Valid @RequestBody AtualizarAtosPublicacaoAfastamentoDTO dto) {
    return ErgonHttpContract.itemBody(executeVersionedItemPatch("atos-publicacao", id, ifMatch, condition -> service.atualizarAtosPublicacao(id, dto, condition)));
  }

  @PostMapping("/{id}/actions/homologar-afastamento")
  @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(
      summary = "Homologar licença ou afastamento",
      description =
          "Homologa afastamento pendente mediante portaria/laudo do órgão e atualiza o vínculo para AFASTADO. Exige If-Match.")
  @WorkflowAction(
      id = "homologar-afastamento",
      title = "Homologar afastamento",
      description = "Aprova e homologa a concessão de licença ou afastamento pendente.",
      scope = ActionScope.ITEM,
      requiredAuthorities = {"ERGON_WRITE"},
      allowedStates = {"PENDENTE_HOMOLOGACAO"},
      confirmationRequired = true,
      resourceVersion = ActionRequirement.REQUIRED,
      resourceVersionTransport = ActionResourceVersionTransport.IF_MATCH)
  public ResponseEntity<AfastamentoResponseDTO> homologar(
      @PathVariable Long id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch,
      @Valid @RequestBody HomologarAfastamentoDTO dto) {
    return ErgonHttpContract.itemBody(executeVersionedItemCommand("homologar-afastamento", id, ifMatch, null, dto,
        condition -> service.homologarAfastamento(id, dto, condition)));
  }

  @PostMapping("/{id}/actions/rejeitar-afastamento")
  @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(
      summary = "Indeferir solicitação de afastamento",
      description =
          "Rejeita afastamento pendente de homologação registrando parecer fundamentado. Exige If-Match.")
  @WorkflowAction(
      id = "rejeitar-afastamento",
      title = "Rejeitar afastamento",
      description = "Indefere a solicitação de licença pendente mediante parecer técnico.",
      scope = ActionScope.ITEM,
      requiredAuthorities = {"ERGON_WRITE"},
      allowedStates = {"PENDENTE_HOMOLOGACAO"},
      confirmationRequired = true,
      resourceVersion = ActionRequirement.REQUIRED,
      resourceVersionTransport = ActionResourceVersionTransport.IF_MATCH)
  public ResponseEntity<AfastamentoResponseDTO> rejeitar(
      @PathVariable Long id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch,
      @Valid @RequestBody RejeitarAfastamentoDTO dto) {
    return ErgonHttpContract.itemBody(executeVersionedItemCommand("rejeitar-afastamento", id, ifMatch, null, dto,
        condition -> service.rejeitarAfastamento(id, dto, condition)));
  }

  @PostMapping("/{id}/actions/prorrogar-afastamento")
  @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(
      summary = "Prorrogar término previsto do afastamento",
      description =
          "Estende a previsão de término de um afastamento homologado aberto mediante novo documento legal. Exige If-Match.")
  @WorkflowAction(
      id = "prorrogar-afastamento",
      title = "Prorrogar afastamento",
      description = "Estende a data de término previsto da licença homologada em curso.",
      scope = ActionScope.ITEM,
      requiredAuthorities = {"ERGON_WRITE"},
      allowedStates = {"HOMOLOGADO"},
      confirmationRequired = true,
      resourceVersion = ActionRequirement.REQUIRED,
      resourceVersionTransport = ActionResourceVersionTransport.IF_MATCH)
  public ResponseEntity<AfastamentoResponseDTO> prorrogar(
      @PathVariable Long id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch,
      @Valid @RequestBody ProrrogarAfastamentoDTO dto) {
    return ErgonHttpContract.itemBody(executeVersionedItemCommand("prorrogar-afastamento", id, ifMatch, null, dto,
        condition -> service.prorrogarAfastamento(id, dto, condition)));
  }

  @PostMapping("/{id}/actions/encerrar-afastamento")
  @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(
      summary = "Encerrar afastamento e registrar retorno",
      description =
          "Registra a data de término efetivo da licença homologada e restabelece o vínculo para ATIVO quando não houver outra licença aberta. Exige If-Match.")
  @WorkflowAction(
      id = "encerrar-afastamento",
      title = "Encerrar afastamento",
      description = "Registra o último dia efetivo da licença e apura a duração total em dias.",
      scope = ActionScope.ITEM,
      requiredAuthorities = {"ERGON_WRITE"},
      allowedStates = {"HOMOLOGADO"},
      confirmationRequired = true,
      resourceVersion = ActionRequirement.REQUIRED,
      resourceVersionTransport = ActionResourceVersionTransport.IF_MATCH)
  public ResponseEntity<AfastamentoResponseDTO> encerrar(
      @PathVariable Long id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch,
      @Valid @RequestBody EncerrarAfastamentoItemDTO dto) {
    return ErgonHttpContract.itemBody(executeVersionedItemCommand("encerrar-afastamento", id, ifMatch, null, dto,
        condition -> service.encerrarAfastamento(id, dto, condition)));
  }

  @PostMapping("/{id}/actions/cancelar-afastamento")
  @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(
      summary = "Cancelar licença ou afastamento",
      description =
          "Torna sem efeito um afastamento pendente ou homologado em aberto mediante ato administrativo. Exige If-Match.")
  @WorkflowAction(
      id = "cancelar-afastamento",
      title = "Cancelar afastamento",
      description = "Cancela administrativamente o registro de afastamento não encerrado.",
      scope = ActionScope.ITEM,
      requiredAuthorities = {"ERGON_WRITE"},
      allowedStates = {"PENDENTE_HOMOLOGACAO", "HOMOLOGADO"},
      riskLevel = ActionRiskLevel.HIGH,
      confirmationRequired = true,
      resourceVersion = ActionRequirement.REQUIRED,
      resourceVersionTransport = ActionResourceVersionTransport.IF_MATCH)
  public ResponseEntity<AfastamentoResponseDTO> cancelar(
      @PathVariable Long id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch,
      @Valid @RequestBody CancelarAfastamentoDTO dto) {
    return ErgonHttpContract.itemBody(executeVersionedItemCommand("cancelar-afastamento", id, ifMatch, null, dto,
        condition -> service.cancelarAfastamento(id, dto, condition)));
  }

  @PostMapping("/determinations/previa")
  @PreAuthorize("hasAuthority('ERGON_READ')")
  @Operation(
      operationId = "determineErgonAfastamentoPrevia",
      summary = "Determinar modalidade, duração prevista e ônus em rascunho",
      description =
          "Projeta a descrição do código de frequência, calcula os dias corridos previstos e sinaliza ônus sem persistir.")
  public ResponseEntity<AfastamentoPreviaPreviewDTO> determinePrevia(
      @Valid @RequestBody AfastamentoPreviaDraftDTO draft) {
    return ResponseEntity.ok(determinations.previaAfastamento(draft));
  }

  @Override
  @PostMapping("/bulk/update/evaluation")
  @Operation(
      operationId = "ergon.afastamentos.bulk.uniform.evaluation",
      summary = "Avaliar edição uniforme de afastamentos",
      description =
          "Avalia seleção, versões e alterações de afastamentos sem modificar o cadastro; produz uma proposta protegida.")
  @BulkResourceOperation(BulkResourceOperation.Role.UNIFORM_EVALUATION)
  public org.springframework.http.ResponseEntity<RestApiResponse<BulkProposal>> evaluateBulkUpdate(@RequestBody BulkUniformEvaluationRequest<?, AfastamentoFilterDTO> request) {
    return super.evaluateBulkUpdate(request);
  }

  @Override
  @PostMapping("/bulk/update-items/evaluation")
  @Operation(
      operationId = "ergon.afastamentos.bulk.items.evaluation",
      summary = "Avaliar edições por afastamento",
      description =
          "Avalia deltas distintos e a versão esperada de cada afastamento, com impedimentos individualizados.")
  @BulkResourceOperation(BulkResourceOperation.Role.ITEM_EVALUATION)
  public org.springframework.http.ResponseEntity<RestApiResponse<BulkProposal>> evaluateBulkUpdateItems(@RequestBody BulkItemEvaluationRequest<?> request) {
    return super.evaluateBulkUpdateItems(request);
  }

  @Override
  @PostMapping("/bulk/update")
  @BulkOperation(
      mode = BulkMode.UNIFORM_UPDATE,
      evaluationOperationId = "ergon.afastamentos.bulk.uniform.evaluation",
      atomicity = ActionCollectionAtomicity.PER_ITEM)
  @Operation(
      operationId = "ergon.afastamentos.bulk.uniform",
      summary = "Confirmar edição uniforme de afastamentos",
      description =
          "Confirma uma proposta avaliada; cada afastamento é revalidado e possui recibo durável, sem repetir efeitos no replay.")
  @BulkResourceOperation(BulkResourceOperation.Role.UNIFORM_CONFIRMATION)
  public org.springframework.http.ResponseEntity<RestApiResponse<BulkExecution>> confirmBulkUpdate(@RequestBody BulkConfirmationRequest request, @RequestHeader("Idempotency-Key") String key) {
    return super.confirmBulkUpdate(request, key);
  }

  @Override
  @PostMapping("/bulk/update-items")
  @BulkOperation(
      mode = BulkMode.PER_ITEM_UPDATE,
      evaluationOperationId = "ergon.afastamentos.bulk.items.evaluation",
      atomicity = ActionCollectionAtomicity.PER_ITEM)
  @Operation(
      operationId = "ergon.afastamentos.bulk.items",
      summary = "Confirmar edições por afastamento",
      description =
          "Aplica os deltas individualizados preservados na proposta, com versão e resultado próprios por afastamento.")
  @BulkResourceOperation(BulkResourceOperation.Role.ITEM_CONFIRMATION)
  public org.springframework.http.ResponseEntity<RestApiResponse<BulkExecution>> confirmBulkUpdateItems(@RequestBody BulkConfirmationRequest request, @RequestHeader("Idempotency-Key") String key) {
    return super.confirmBulkUpdateItems(request, key);
  }











  @PostMapping("/bulk/homologar-afastamento-lote/evaluation")
  @Operation(
      operationId = "ergon.afastamentos.homologar-afastamento-lote.evaluation",
      summary = "Avaliar homologação coletiva de afastamentos",
      description =
          "Avalia homologação coletiva de licenças pendentes com portaria/ata do mesmo órgão, sem persistir alterações.")
  public org.springframework.http.ResponseEntity<RestApiResponse<BulkProposal>> bulkHomologarEvaluate(

          @RequestBody
          BulkCommandEvaluationRequest<HomologarAfastamentoLoteDTO, ?, AfastamentoFilterDTO>
              request) {
    return evaluateBulkDomainCommand("homologar-afastamento-lote", request);
  }

  @PostMapping("/actions/homologar-afastamento-lote")
  @WorkflowAction(
      id = "homologar-afastamento-lote",
      title = "Homologar afastamentos em lote",
      description = "Homologa coletivamente licenças pendentes mediante portaria ou ata oficial.",
      scope = ActionScope.COLLECTION,
      requiredAuthorities = {"ERGON_WRITE"},
      confirmationRequired = true,
      idempotencyKey = ActionRequirement.REQUIRED,
      atomicity = ActionCollectionAtomicity.PER_ITEM,
      outcomeMode = ActionOutcomeMode.PER_ITEM,
      maxSelection = 200)
  @BulkOperation(
      mode = BulkMode.DOMAIN_COMMAND,
      evaluationOperationId = "ergon.afastamentos.homologar-afastamento-lote.evaluation",
      atomicity = ActionCollectionAtomicity.PER_ITEM)
  @Operation(
      operationId = "ergon.afastamentos.homologar-afastamento-lote",
      summary = "Homologar afastamentos em lote",
      description = "Executa homologação coletiva governada de licenças pendentes.")
  public org.springframework.http.ResponseEntity<RestApiResponse<BulkExecution>> bulkHomologarConfirm(
      @RequestBody BulkConfirmationRequest request,
      @Parameter(description = "Chave estável do ato coletivo para impedir efeitos duplicados.")
          @RequestHeader("Idempotency-Key")
          String key) {
    return confirmBulkDomainCommand("homologar-afastamento-lote", request, key);
  }
  @Override @SuppressWarnings({"unchecked", "rawtypes"})
  protected org.praxisplatform.uischema.bulk.JpaResourceBulkEngine getBulkEngine() { return bulk.engine(Resource.AFASTAMENTOS); }
  @Override protected String bulkProposalOperationId(UUID id) { return bulk.locate(Resource.AFASTAMENTOS, id, false); }
  @Override protected String bulkExecutionOperationId(UUID id) { return bulk.locate(Resource.AFASTAMENTOS, id, true); }

}
