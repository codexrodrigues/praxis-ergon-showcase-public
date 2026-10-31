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
    sourceKind = "computed")
@ResourceFact(
    key = "comSubstitutoDesignado",
    resourceKey = "administracao-pessoal.afastamentos",
    label = "Com substituto designado",
    description = "Verdadeiro quando há servidor substituto formalmente vinculado ao período.",
    dataType = "boolean",
    sourceKind = "computed")
@ResourceFact(
    key = "pendenteHomologacao",
    resourceKey = "administracao-pessoal.afastamentos",
    label = "Pendente de homologação",
    description = "Verdadeiro quando o registro aguarda deliberação da chefia ou junta médica.",
    dataType = "boolean",
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
            metadataFields = {"vinculoMatricula", "codigoFrequenciaNome", "situacao", "dtini"}),
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
    extends AbstractResourceController<
        AfastamentoResponseDTO,
        Long,
        AfastamentoFilterDTO,
        CreateAfastamentoDTO,
        UpdateAfastamentoDTO> {

  private final AfastamentoService service;
  private final ResourceVersionEtagService etags;
  private final ResourceVersionScopeProvider scopes;
  @Autowired private ErgonBulkOperations bulk;
  @Autowired private ErgonDeterminationService determinations;

  public AfastamentoController(
      AfastamentoService service,
      ResourceVersionEtagService etags,
      ResourceVersionScopeProvider scopes) {
    this.service = service;
    this.etags = etags;
    this.scopes = scopes;
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
                            surfaceId = "ficha-afastamento")))
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
    var p =
        new ResourceVersionUpdatePrecondition<Long>(
            etags, ifMatch, scopes.currentScope(), "administracao-pessoal.afastamentos", id);
    return withResourceVersion(ResponseEntity.ok(), id, service.atualizarSubstituto(id, dto, p));
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
    var p =
        new ResourceVersionUpdatePrecondition<Long>(
            etags, ifMatch, scopes.currentScope(), "administracao-pessoal.afastamentos", id);
    return withResourceVersion(
        ResponseEntity.ok(), id, service.atualizarAtosPublicacao(id, dto, p));
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
    var p =
        new ResourceVersionUpdatePrecondition<Long>(
            etags, ifMatch, scopes.currentScope(), "administracao-pessoal.afastamentos", id);
    return withResourceVersion(ResponseEntity.ok(), id, service.homologarAfastamento(id, dto, p));
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
    var p =
        new ResourceVersionUpdatePrecondition<Long>(
            etags, ifMatch, scopes.currentScope(), "administracao-pessoal.afastamentos", id);
    return withResourceVersion(ResponseEntity.ok(), id, service.rejeitarAfastamento(id, dto, p));
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
    var p =
        new ResourceVersionUpdatePrecondition<Long>(
            etags, ifMatch, scopes.currentScope(), "administracao-pessoal.afastamentos", id);
    return withResourceVersion(ResponseEntity.ok(), id, service.prorrogarAfastamento(id, dto, p));
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
    var p =
        new ResourceVersionUpdatePrecondition<Long>(
            etags, ifMatch, scopes.currentScope(), "administracao-pessoal.afastamentos", id);
    return withResourceVersion(ResponseEntity.ok(), id, service.encerrarAfastamento(id, dto, p));
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
    var p =
        new ResourceVersionUpdatePrecondition<Long>(
            etags, ifMatch, scopes.currentScope(), "administracao-pessoal.afastamentos", id);
    return withResourceVersion(ResponseEntity.ok(), id, service.cancelarAfastamento(id, dto, p));
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

  @PostMapping("/bulk/update/evaluation")
  @Operation(
      operationId = "ergon.afastamentos.bulk.uniform.evaluation",
      summary = "Avaliar edição uniforme de afastamentos",
      description =
          "Avalia seleção, versões e alterações de afastamentos sem modificar o cadastro; produz uma proposta protegida.")
  public RestApiResponse<BulkProposal> bulkEvaluate(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              content =
                  @io.swagger.v3.oas.annotations.media.Content(
                      schema =
                          @io.swagger.v3.oas.annotations.media.Schema(
                              implementation = ErgonBulkSchema.AfastamentoUniform.class)))
          @RequestBody
          BulkUniformEvaluationRequest<String, AfastamentoFilterDTO> request) {
    return RestApiResponse.success(bulk.uniform(Resource.AFASTAMENTOS, request), null);
  }

  @PostMapping("/bulk/update-items/evaluation")
  @Operation(
      operationId = "ergon.afastamentos.bulk.items.evaluation",
      summary = "Avaliar edições por afastamento",
      description =
          "Avalia deltas distintos e a versão esperada de cada afastamento, com impedimentos individualizados.")
  public RestApiResponse<BulkProposal> bulkEvaluateItems(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              content =
                  @io.swagger.v3.oas.annotations.media.Content(
                      schema =
                          @io.swagger.v3.oas.annotations.media.Schema(
                              implementation = ErgonBulkSchema.AfastamentoItems.class)))
          @RequestBody
          BulkItemEvaluationRequest<String> request) {
    return RestApiResponse.success(bulk.items(Resource.AFASTAMENTOS, request), null);
  }

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
  public RestApiResponse<BulkExecution> bulkConfirm(
      @RequestBody BulkConfirmationRequest request,
      @Parameter(
              description =
                  "Chave estável desta confirmação; repetir a mesma chave recupera a execução.")
          @RequestHeader("Idempotency-Key")
          String key) {
    return RestApiResponse.success(
        bulk.confirm(Resource.AFASTAMENTOS, "ergon.afastamentos.bulk.uniform", request, key), null);
  }

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
  public RestApiResponse<BulkExecution> bulkConfirmItems(
      @RequestBody BulkConfirmationRequest request,
      @Parameter(
              description =
                  "Chave estável de confirmação para recuperar a mesma execução em caso de repetição.")
          @RequestHeader("Idempotency-Key")
          String key) {
    return RestApiResponse.success(
        bulk.confirm(Resource.AFASTAMENTOS, "ergon.afastamentos.bulk.items", request, key), null);
  }

  @GetMapping("/bulk/proposals/{proposalId}")
  @BulkResourceOperation(BulkResourceOperation.Role.PROPOSAL)
  @Operation(
      operationId = "ergon.afastamentos.bulk.proposal",
      summary = "Consultar proposta de lote de afastamentos",
      description =
          "Consulta totais e intenção pública da proposta após conferir acesso ao conjunto completo de afastamentos.")
  public RestApiResponse<BulkProposal> bulkProposal(
      @Parameter(description = "Identidade da proposta avaliada.") @PathVariable UUID proposalId) {
    return RestApiResponse.success(
        bulk.proposal(
            Resource.AFASTAMENTOS,
            bulk.locate(Resource.AFASTAMENTOS, proposalId, false),
            proposalId),
        null);
  }

  @GetMapping("/bulk/proposals/{proposalId}/results")
  @BulkResourceOperation(BulkResourceOperation.Role.PROPOSAL_RESULTS)
  @Operation(
      operationId = "ergon.afastamentos.bulk.proposal-results",
      summary = "Inspecionar avaliação do lote de afastamentos",
      description =
          "Pagina afastamentos executáveis e bloqueados, com diagnósticos seguros e cursor vinculado ao solicitante.")
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
            Resource.AFASTAMENTOS,
            bulk.locate(Resource.AFASTAMENTOS, proposalId, false),
            proposalId,
            size,
            after),
        null);
  }

  @GetMapping("/bulk/executions/{executionId}")
  @BulkResourceOperation(BulkResourceOperation.Role.EXECUTION)
  @Operation(
      operationId = "ergon.afastamentos.bulk.execution",
      summary = "Consultar execução do lote de afastamentos",
      description = "Retorna estado e contadores reconciliados a partir dos recibos persistidos.")
  public RestApiResponse<BulkExecution> bulkExecution(
      @Parameter(description = "Identidade da execução confirmada.")
          @PathVariable
          UUID executionId) {
    return RestApiResponse.success(
        bulk.execution(
            Resource.AFASTAMENTOS,
            bulk.locate(Resource.AFASTAMENTOS, executionId, true),
            executionId),
        null);
  }

  @GetMapping("/bulk/executions/{executionId}/results")
  @BulkResourceOperation(BulkResourceOperation.Role.EXECUTION_RESULTS)
  @Operation(
      operationId = "ergon.afastamentos.bulk.results",
      summary = "Consultar recibos do lote de afastamentos",
      description = "Pagina resultados por afastamento sem expor dados sensíveis da proposta.")
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
            Resource.AFASTAMENTOS,
            bulk.locate(Resource.AFASTAMENTOS, executionId, true),
            executionId,
            size,
            after),
        null);
  }

  @PostMapping("/bulk/executions/{executionId}/cancel")
  @BulkResourceOperation(BulkResourceOperation.Role.CANCEL)
  @Operation(
      operationId = "ergon.afastamentos.bulk.cancel",
      summary = "Solicitar parada do lote de afastamentos",
      description =
          "Registra a solicitação de parada sem desfazer alterações já confirmadas e sem reabrir efeitos no replay.")
  public RestApiResponse<BulkExecution> bulkCancel(
      @Parameter(description = "Identidade da execução a interromper.")
          @PathVariable
          UUID executionId) {
    return RestApiResponse.success(
        bulk.cancel(
            Resource.AFASTAMENTOS,
            bulk.locate(Resource.AFASTAMENTOS, executionId, true),
            executionId),
        null);
  }

  @PostMapping("/bulk/homologar-afastamento-lote/evaluation")
  @Operation(
      operationId = "ergon.afastamentos.homologar-afastamento-lote.evaluation",
      summary = "Avaliar homologação coletiva de afastamentos",
      description =
          "Avalia homologação coletiva de licenças pendentes com portaria/ata do mesmo órgão, sem persistir alterações.")
  public RestApiResponse<BulkProposal> bulkHomologarEvaluate(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              content =
                  @io.swagger.v3.oas.annotations.media.Content(
                      schema =
                          @io.swagger.v3.oas.annotations.media.Schema(
                              implementation = ErgonBulkSchema.HomologarAfastamentoCommand.class)))
          @RequestBody
          BulkCommandEvaluationRequest<HomologarAfastamentoLoteDTO, String, AfastamentoFilterDTO>
              request) {
    return RestApiResponse.success(
        bulk.command(Resource.AFASTAMENTOS, "homologar-afastamento-lote", request), null);
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
  public RestApiResponse<BulkExecution> bulkHomologarConfirm(
      @RequestBody BulkConfirmationRequest request,
      @Parameter(description = "Chave estável do ato coletivo para impedir efeitos duplicados.")
          @RequestHeader("Idempotency-Key")
          String key) {
    return RestApiResponse.success(
        bulk.confirm(
            Resource.AFASTAMENTOS, "ergon.afastamentos.homologar-afastamento-lote", request, key),
        null);
  }
}
