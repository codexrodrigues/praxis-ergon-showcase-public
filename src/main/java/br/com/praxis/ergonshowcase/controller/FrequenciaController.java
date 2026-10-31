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

/** Publica o contrato de ocorrências operacionais de frequência através da hierarquia canônica Praxis. */
@ResourceFact(
    key = "pendenteHomologacao",
    resourceKey = "administracao-pessoal.frequencias",
    label = "Pendente de homologação",
    description = "Verdadeiro quando a ocorrência aguarda deliberação da chefia imediata ou RH.",
    dataType = "boolean",
    sourceKind = "computed")
@ResourceFact(
    key = "ocorrenciaHoraria",
    resourceKey = "administracao-pessoal.frequencias",
    label = "Ocorrência horária",
    description = "Verdadeiro quando há horários de entrada e saída apurados na ocorrência.",
    dataType = "boolean",
    sourceKind = "computed")
@ResourceFact(
    key = "comDocumentoComprobatorio",
    resourceKey = "administracao-pessoal.frequencias",
    label = "Com documento anexo",
    description = "Verdadeiro quando há documento comprobatório ou portaria vinculada.",
    dataType = "boolean",
    sourceKind = "computed")
@BulkResourceOperations(
    proposalOperationId = "ergon.frequencias.bulk.proposal",
    proposalResultsOperationId = "ergon.frequencias.bulk.proposal-results",
    executionOperationId = "ergon.frequencias.bulk.execution",
    executionResultsOperationId = "ergon.frequencias.bulk.results",
    cancelOperationId = "ergon.frequencias.bulk.cancel",
    updateSourceOperationId = "updateErgonFrequencia",
    protectedUpdateFields = {"tipofreq", "codfreq", "dtini"})
@ApiResource(
    value = ApiPaths.FREQUENCIAS,
    resourceKey = "administracao-pessoal.frequencias",
    title = "Ocorrências de frequência",
    description =
        "Ocorrências operacionais de frequência, faltas, abonos, atrasos e horas extraordinárias por vínculo e período (FREQUENCIAS).",
    icon = "schedule",
    identity =
        @ResourceIdentity(
            keyField = "id",
            titleField = "obs",
            displayLabelField = "obs",
            subtitleField = "pessoaNome",
            metadataFields = {"vinculoMatricula", "codigoFrequenciaNome", "situacao", "dtini"}),
    quickFilters = {
      @QuickFilter(
          id = "homologadas",
          label = "Homologadas",
          filter = "{\"situacoes\":[\"HOMOLOGADO\"]}"),
      @QuickFilter(
          id = "pendentes",
          label = "Pendentes de homologação",
          filter = "{\"situacoes\":[\"PENDENTE_HOMOLOGACAO\"]}"),
      @QuickFilter(
          id = "sem-documento",
          label = "Sem documento anexo",
          filter = "{\"semDocumentoLegal\":true}"),
      @QuickFilter(
          id = "ultimos-30-dias",
          label = "Últimos 30 dias",
          filter = "{\"iniciadasUltimosDias\":30}")
    })
@ApiGroup(
    value = "ergon",
    title = "Administração de pessoal",
    description = "Cadastros civis e assentamentos funcionais dos órgãos públicos.",
    icon = "groups")
public class FrequenciaController
    extends AbstractResourceController<
        FrequenciaResponseDTO,
        Long,
        FrequenciaFilterDTO,
        CreateFrequenciaDTO,
        UpdateFrequenciaDTO> {

  private final FrequenciaService service;
  private final ResourceVersionEtagService etags;
  private final ResourceVersionScopeProvider scopes;
  @Autowired private ErgonBulkOperations bulk;
  @Autowired private ErgonDeterminationService determinations;

  public FrequenciaController(
      FrequenciaService service,
      ResourceVersionEtagService etags,
      ResourceVersionScopeProvider scopes) {
    this.service = service;
    this.etags = etags;
    this.scopes = scopes;
  }

  @Override
  @PostMapping("/stats/group-by")
  @Operation(
      summary = "Ocorrências de frequência por situação e quantidade",
      description =
          "Agrega o conjunto autorizado e filtrado de ocorrências de frequência com abertura direta da ficha pela identidade real do bucket.")
  @UiAnalytics(
      projections = {
        @AnalyticsProjection(
            id = "frequencias-por-situacao",
            intent = AnalyticsIntent.COMPOSITION,
            sourceOperation = AnalyticsOperation.GROUP_BY,
            primaryDimension = @AnalyticsDimensionBinding(field = "situacao"),
            primaryMetrics =
                @AnalyticsMetricBinding(
                    field = "count",
                    aggregation = "count",
                    label = "Ocorrências"),
            defaultLimit = 20),
        @AnalyticsProjection(
            id = "quantidade-por-frequencia",
            intent = AnalyticsIntent.RANKING,
            sourceOperation = AnalyticsOperation.GROUP_BY,
            primaryDimension = @AnalyticsDimensionBinding(field = "id"),
            primaryMetrics =
                @AnalyticsMetricBinding(
                    field = "quantidade",
                    aggregation = "sum",
                    label = "Quantidade apurada"),
            defaultLimit = 20,
            recordOpen =
                @AnalyticsRecordOpen(
                    sourceIdentityField = "key",
                    target =
                        @AnalyticsSurfaceTarget(
                            resourceKey = "administracao-pessoal.frequencias",
                            surfaceId = "ficha-frequencia")))
      })
  public ResponseEntity<RestApiResponse<GroupByStatsResponse>> groupByStats(
      @RequestBody GroupByStatsRequest<FrequenciaFilterDTO> request) {
    return super.groupByStats(request);
  }

  @Override
  @PostMapping
  @Operation(
      operationId = "createErgonFrequencia",
      summary = "Registrar ocorrência de frequência",
      description =
          "Inclui novo registro de frequência, falta, abono ou hora extraordinária em estado PENDENTE_HOMOLOGACAO validando vínculo, órgão e modalidade.")
  public ResponseEntity<RestApiResponse<FrequenciaResponseDTO>> create(
      @Valid @RequestBody CreateFrequenciaDTO dto) {
    return super.create(dto);
  }

  @Override
  @PutMapping("/{id}")
  @BulkResourceOperation(BulkResourceOperation.Role.UPDATE_SOURCE)
  @Operation(
      operationId = "updateErgonFrequencia",
      summary = "Atualizar ocorrência de frequência",
      description =
          "Atualiza dados operacionais da ocorrência de frequência com controle otimista If-Match.")
  public ResponseEntity<RestApiResponse<FrequenciaResponseDTO>> update(
      @PathVariable Long id, @Valid @RequestBody UpdateFrequenciaDTO dto) {
    return super.update(id, dto);
  }

  @Override
  protected FrequenciaService getService() {
    return service;
  }

  @Override
  protected Long getResponseId(FrequenciaResponseDTO dto) {
    return dto.getId();
  }

  @GetMapping("/{id}/ficha-frequencia")
  @PreAuthorize("hasAuthority('ERGON_READ')")
  @Operation(
      summary = "Consultar ficha 360° da ocorrência de frequência",
      description =
          "Retorna a projeção completa de leitura da ocorrência de frequência para o drawer Quick View.")
  @UiSurface(
      id = "ficha-frequencia",
      kind = SurfaceKind.READ_PROJECTION,
      scope = SurfaceScope.ITEM,
      title = "Ficha da frequência",
      description =
          "Visão consolidada do período, horários, quantidade apurada, justificativa e documento comprobatório.",
      intent = "summary",
      requiredAuthorities = {"ERGON_READ"})
  public ResponseEntity<FrequenciaResponseDTO> ficha(@PathVariable Long id) {
    return withResourceVersion(ResponseEntity.ok(), id, service.findById(id));
  }

  @PatchMapping("/{id}/apuracao-horario")
  @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(
      operationId = "patchFrequenciaApuracaoHorario",
      summary = "Retificar horários e quantidade apurada",
      description =
          "Atualiza hora de entrada, hora de saída e quantidade apurada da ocorrência com If-Match.")
  @ResourceIntent(
      id = "apuracao-horario",
      title = "Apuração e horários",
      description = "Manutenção parcial dos horários de entrada/saída e quantidade apurada.")
  @UiSurface(
      id = "apuracao-horario",
      kind = SurfaceKind.PARTIAL_FORM,
      scope = SurfaceScope.ITEM,
      title = "Apuração e horários",
      description = "Retificação focalizada de horários e quantidade da ocorrência de frequência.",
      intent = "apuracao-horario",
      requiredAuthorities = {"ERGON_WRITE"})
  public ResponseEntity<FrequenciaResponseDTO> atualizarHorario(
      @PathVariable Long id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch,
      @Valid @RequestBody AtualizarHorarioFrequenciaDTO dto) {
    var p =
        new ResourceVersionUpdatePrecondition<Long>(
            etags, ifMatch, scopes.currentScope(), "administracao-pessoal.frequencias", id);
    return withResourceVersion(ResponseEntity.ok(), id, service.atualizarHorario(id, dto, p));
  }

  @PatchMapping("/{id}/justificativa-documento")
  @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(
      operationId = "patchFrequenciaJustificativaDocumento",
      summary = "Atualizar justificativa, documento e data final",
      description =
          "Atualiza a justificativa operacional, documento comprobatório e data final da ocorrência com If-Match.")
  @ResourceIntent(
      id = "justificativa-documento",
      title = "Justificativa e documento comprobatório",
      description = "Manutenção parcial da justificativa, documento comprobatório e data final.")
  @UiSurface(
      id = "justificativa-documento",
      kind = SurfaceKind.PARTIAL_FORM,
      scope = SurfaceScope.ITEM,
      title = "Justificativa e documento comprobatório",
      description = "Revisão focalizada de justificativa, documento legal e período final.",
      intent = "justificativa-documento",
      requiredAuthorities = {"ERGON_WRITE"})
  public ResponseEntity<FrequenciaResponseDTO> atualizarJustificativa(
      @PathVariable Long id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch,
      @Valid @RequestBody AtualizarJustificativaFrequenciaDTO dto) {
    var p =
        new ResourceVersionUpdatePrecondition<Long>(
            etags, ifMatch, scopes.currentScope(), "administracao-pessoal.frequencias", id);
    return withResourceVersion(ResponseEntity.ok(), id, service.atualizarJustificativa(id, dto, p));
  }

  @PostMapping("/{id}/actions/homologar-frequencia")
  @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(
      summary = "Homologar ocorrência de frequência",
      description =
          "Homologa ocorrência de frequência pendente mediante despacho da chefia ou RH. Exige If-Match.")
  @WorkflowAction(
      id = "homologar-frequencia",
      title = "Homologar frequência",
      description = "Aprova e homologa a ocorrência operacional de frequência pendente.",
      scope = ActionScope.ITEM,
      requiredAuthorities = {"ERGON_WRITE"},
      allowedStates = {"PENDENTE_HOMOLOGACAO"},
      confirmationRequired = true,
      resourceVersion = ActionRequirement.REQUIRED,
      resourceVersionTransport = ActionResourceVersionTransport.IF_MATCH)
  public ResponseEntity<FrequenciaResponseDTO> homologar(
      @PathVariable Long id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch,
      @Valid @RequestBody HomologarFrequenciaDTO dto) {
    var p =
        new ResourceVersionUpdatePrecondition<Long>(
            etags, ifMatch, scopes.currentScope(), "administracao-pessoal.frequencias", id);
    return withResourceVersion(ResponseEntity.ok(), id, service.homologarFrequencia(id, dto, p));
  }

  @PostMapping("/{id}/actions/abonar-falta-frequencia")
  @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(
      summary = "Abonar falta ou ausência mediante documento",
      description =
          "Registra o abono formal de ocorrência pendente mediante documento comprobatório e homologa o lançamento. Exige If-Match.")
  @WorkflowAction(
      id = "abonar-falta-frequencia",
      title = "Abonar falta",
      description = "Registra documento comprobatório e homologa o abono da ocorrência.",
      scope = ActionScope.ITEM,
      requiredAuthorities = {"ERGON_WRITE"},
      allowedStates = {"PENDENTE_HOMOLOGACAO", "HOMOLOGADO"},
      confirmationRequired = true,
      resourceVersion = ActionRequirement.REQUIRED,
      resourceVersionTransport = ActionResourceVersionTransport.IF_MATCH)
  public ResponseEntity<FrequenciaResponseDTO> abonarFalta(
      @PathVariable Long id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch,
      @Valid @RequestBody AbonarFaltaFrequenciaDTO dto) {
    var p =
        new ResourceVersionUpdatePrecondition<Long>(
            etags, ifMatch, scopes.currentScope(), "administracao-pessoal.frequencias", id);
    return withResourceVersion(ResponseEntity.ok(), id, service.abonarFaltaFrequencia(id, dto, p));
  }

  @PostMapping("/{id}/actions/rejeitar-frequencia")
  @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(
      summary = "Rejeitar ocorrência de frequência",
      description =
          "Rejeita ocorrência de frequência pendente de homologação registrando motivo fundamentado. Exige If-Match.")
  @WorkflowAction(
      id = "rejeitar-frequencia",
      title = "Rejeitar frequência",
      description = "Indefere a ocorrência de frequência pendente mediante justificativa.",
      scope = ActionScope.ITEM,
      requiredAuthorities = {"ERGON_WRITE"},
      allowedStates = {"PENDENTE_HOMOLOGACAO"},
      confirmationRequired = true,
      resourceVersion = ActionRequirement.REQUIRED,
      resourceVersionTransport = ActionResourceVersionTransport.IF_MATCH)
  public ResponseEntity<FrequenciaResponseDTO> rejeitar(
      @PathVariable Long id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch,
      @Valid @RequestBody RejeitarFrequenciaDTO dto) {
    var p =
        new ResourceVersionUpdatePrecondition<Long>(
            etags, ifMatch, scopes.currentScope(), "administracao-pessoal.frequencias", id);
    return withResourceVersion(ResponseEntity.ok(), id, service.rejeitarFrequencia(id, dto, p));
  }

  @PostMapping("/{id}/actions/cancelar-frequencia")
  @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(
      summary = "Cancelar ocorrência de frequência",
      description =
          "Torna sem efeito uma ocorrência de frequência pendente ou homologada mediante justificativa administrativa. Exige If-Match.")
  @WorkflowAction(
      id = "cancelar-frequencia",
      title = "Cancelar frequência",
      description = "Cancela administrativamente o lançamento de frequência.",
      scope = ActionScope.ITEM,
      requiredAuthorities = {"ERGON_WRITE"},
      allowedStates = {"PENDENTE_HOMOLOGACAO", "HOMOLOGADO"},
      riskLevel = ActionRiskLevel.HIGH,
      confirmationRequired = true,
      resourceVersion = ActionRequirement.REQUIRED,
      resourceVersionTransport = ActionResourceVersionTransport.IF_MATCH)
  public ResponseEntity<FrequenciaResponseDTO> cancelar(
      @PathVariable Long id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch,
      @Valid @RequestBody CancelarFrequenciaDTO dto) {
    var p =
        new ResourceVersionUpdatePrecondition<Long>(
            etags, ifMatch, scopes.currentScope(), "administracao-pessoal.frequencias", id);
    return withResourceVersion(ResponseEntity.ok(), id, service.cancelarFrequencia(id, dto, p));
  }

  @PostMapping("/determinations/previa")
  @PreAuthorize("hasAuthority('ERGON_READ')")
  @Operation(
      operationId = "determineErgonFrequenciaPrevia",
      summary = "Determinar modalidade, unidade e exigência de horário em rascunho",
      description =
          "Projeta a descrição do código de frequência, unidade de medida e obrigatoriedade de controle horário sem persistir.")
  public ResponseEntity<FrequenciaPreviaPreviewDTO> determinePrevia(
      @Valid @RequestBody FrequenciaPreviaDraftDTO draft) {
    return ResponseEntity.ok(determinations.previaFrequencia(draft));
  }

  @PostMapping("/bulk/update/evaluation")
  @Operation(
      operationId = "ergon.frequencias.bulk.uniform.evaluation",
      summary = "Avaliar edição uniforme de ocorrências de frequência",
      description =
          "Avalia seleção, versões e alterações de ocorrências de frequência sem modificar o cadastro; produz uma proposta protegida.")
  public RestApiResponse<BulkProposal> bulkEvaluate(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              content =
                  @io.swagger.v3.oas.annotations.media.Content(
                      schema =
                          @io.swagger.v3.oas.annotations.media.Schema(
                              implementation = ErgonBulkSchema.FrequenciaUniform.class)))
          @RequestBody
          BulkUniformEvaluationRequest<String, FrequenciaFilterDTO> request) {
    return RestApiResponse.success(bulk.uniform(Resource.FREQUENCIAS, request), null);
  }

  @PostMapping("/bulk/update-items/evaluation")
  @Operation(
      operationId = "ergon.frequencias.bulk.items.evaluation",
      summary = "Avaliar edições por ocorrência de frequência",
      description =
          "Avalia deltas distintos e a versão esperada de cada ocorrência de frequência, com impedimentos individualizados.")
  public RestApiResponse<BulkProposal> bulkEvaluateItems(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              content =
                  @io.swagger.v3.oas.annotations.media.Content(
                      schema =
                          @io.swagger.v3.oas.annotations.media.Schema(
                              implementation = ErgonBulkSchema.FrequenciaItems.class)))
          @RequestBody
          BulkItemEvaluationRequest<String> request) {
    return RestApiResponse.success(bulk.items(Resource.FREQUENCIAS, request), null);
  }

  @PostMapping("/bulk/update")
  @BulkOperation(
      mode = BulkMode.UNIFORM_UPDATE,
      evaluationOperationId = "ergon.frequencias.bulk.uniform.evaluation",
      atomicity = ActionCollectionAtomicity.PER_ITEM)
  @Operation(
      operationId = "ergon.frequencias.bulk.uniform",
      summary = "Confirmar edição uniforme de ocorrências de frequência",
      description =
          "Confirma uma proposta avaliada; cada ocorrência é revalidada e possui recibo durável, sem repetir efeitos no replay.")
  public RestApiResponse<BulkExecution> bulkConfirm(
      @RequestBody BulkConfirmationRequest request,
      @Parameter(
              description =
                  "Chave estável desta confirmação; repetir a mesma chave recupera a execução.")
          @RequestHeader("Idempotency-Key")
          String key) {
    return RestApiResponse.success(
        bulk.confirm(Resource.FREQUENCIAS, "ergon.frequencias.bulk.uniform", request, key), null);
  }

  @PostMapping("/bulk/update-items")
  @BulkOperation(
      mode = BulkMode.PER_ITEM_UPDATE,
      evaluationOperationId = "ergon.frequencias.bulk.items.evaluation",
      atomicity = ActionCollectionAtomicity.PER_ITEM)
  @Operation(
      operationId = "ergon.frequencias.bulk.items",
      summary = "Confirmar edições por ocorrência de frequência",
      description =
          "Aplica os deltas individualizados preservados na proposta, com versão e resultado próprios por ocorrência.")
  public RestApiResponse<BulkExecution> bulkConfirmItems(
      @RequestBody BulkConfirmationRequest request,
      @Parameter(
              description =
                  "Chave estável de confirmação para recuperar a mesma execução em caso de repetição.")
          @RequestHeader("Idempotency-Key")
          String key) {
    return RestApiResponse.success(
        bulk.confirm(Resource.FREQUENCIAS, "ergon.frequencias.bulk.items", request, key), null);
  }

  @GetMapping("/bulk/proposals/{proposalId}")
  @BulkResourceOperation(BulkResourceOperation.Role.PROPOSAL)
  @Operation(
      operationId = "ergon.frequencias.bulk.proposal",
      summary = "Consultar proposta de lote de frequências",
      description =
          "Consulta totais e intenção pública da proposta após conferir acesso ao conjunto completo de ocorrências.")
  public RestApiResponse<BulkProposal> bulkProposal(
      @Parameter(description = "Identidade da proposta avaliada.") @PathVariable UUID proposalId) {
    return RestApiResponse.success(
        bulk.proposal(
            Resource.FREQUENCIAS,
            bulk.locate(Resource.FREQUENCIAS, proposalId, false),
            proposalId),
        null);
  }

  @GetMapping("/bulk/proposals/{proposalId}/results")
  @BulkResourceOperation(BulkResourceOperation.Role.PROPOSAL_RESULTS)
  @Operation(
      operationId = "ergon.frequencias.bulk.proposal-results",
      summary = "Inspecionar avaliação do lote de frequências",
      description =
          "Pagina ocorrências executáveis e bloqueadas, com diagnósticos seguros e cursor vinculado ao solicitante.")
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
            Resource.FREQUENCIAS,
            bulk.locate(Resource.FREQUENCIAS, proposalId, false),
            proposalId,
            size,
            after),
        null);
  }

  @GetMapping("/bulk/executions/{executionId}")
  @BulkResourceOperation(BulkResourceOperation.Role.EXECUTION)
  @Operation(
      operationId = "ergon.frequencias.bulk.execution",
      summary = "Consultar execução do lote de frequências",
      description = "Retorna estado e contadores reconciliados a partir dos recibos persistidos.")
  public RestApiResponse<BulkExecution> bulkExecution(
      @Parameter(description = "Identidade da execução confirmada.")
          @PathVariable
          UUID executionId) {
    return RestApiResponse.success(
        bulk.execution(
            Resource.FREQUENCIAS,
            bulk.locate(Resource.FREQUENCIAS, executionId, true),
            executionId),
        null);
  }

  @GetMapping("/bulk/executions/{executionId}/results")
  @BulkResourceOperation(BulkResourceOperation.Role.EXECUTION_RESULTS)
  @Operation(
      operationId = "ergon.frequencias.bulk.results",
      summary = "Consultar recibos do lote de frequências",
      description = "Pagina resultados por ocorrência sem expor dados sensíveis da proposta.")
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
            Resource.FREQUENCIAS,
            bulk.locate(Resource.FREQUENCIAS, executionId, true),
            executionId,
            size,
            after),
        null);
  }

  @PostMapping("/bulk/executions/{executionId}/cancel")
  @BulkResourceOperation(BulkResourceOperation.Role.CANCEL)
  @Operation(
      operationId = "ergon.frequencias.bulk.cancel",
      summary = "Solicitar parada do lote de frequências",
      description =
          "Registra a solicitação de parada sem desfazer alterações já confirmadas e sem reabrir efeitos no replay.")
  public RestApiResponse<BulkExecution> bulkCancel(
      @Parameter(description = "Identidade da execução a interromper.")
          @PathVariable
          UUID executionId) {
    return RestApiResponse.success(
        bulk.cancel(
            Resource.FREQUENCIAS,
            bulk.locate(Resource.FREQUENCIAS, executionId, true),
            executionId),
        null);
  }

  @PostMapping("/bulk/homologar-frequencia-lote/evaluation")
  @Operation(
      operationId = "ergon.frequencias.homologar-frequencia-lote.evaluation",
      summary = "Avaliar homologação coletiva de ocorrências de frequência",
      description =
          "Avalia homologação coletiva de frequências pendentes do mesmo órgão, sem persistir alterações.")
  public RestApiResponse<BulkProposal> bulkHomologarEvaluate(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              content =
                  @io.swagger.v3.oas.annotations.media.Content(
                      schema =
                          @io.swagger.v3.oas.annotations.media.Schema(
                              implementation = ErgonBulkSchema.HomologarFrequenciaCommand.class)))
          @RequestBody
          BulkCommandEvaluationRequest<HomologarFrequenciaLoteDTO, String, FrequenciaFilterDTO>
              request) {
    return RestApiResponse.success(
        bulk.command(Resource.FREQUENCIAS, "homologar-frequencia-lote", request), null);
  }

  @PostMapping("/actions/homologar-frequencia-lote")
  @WorkflowAction(
      id = "homologar-frequencia-lote",
      title = "Homologar frequências em lote",
      description = "Homologa coletivamente ocorrências de frequência pendentes mediante despacho.",
      scope = ActionScope.COLLECTION,
      requiredAuthorities = {"ERGON_WRITE"},
      confirmationRequired = true,
      idempotencyKey = ActionRequirement.REQUIRED,
      atomicity = ActionCollectionAtomicity.PER_ITEM,
      outcomeMode = ActionOutcomeMode.PER_ITEM,
      maxSelection = 200)
  @BulkOperation(
      mode = BulkMode.DOMAIN_COMMAND,
      evaluationOperationId = "ergon.frequencias.homologar-frequencia-lote.evaluation",
      atomicity = ActionCollectionAtomicity.PER_ITEM)
  @Operation(
      operationId = "ergon.frequencias.homologar-frequencia-lote",
      summary = "Homologar frequências em lote",
      description = "Executa homologação coletiva governada de ocorrências de frequência pendentes.")
  public RestApiResponse<BulkExecution> bulkHomologarConfirm(
      @RequestBody BulkConfirmationRequest request,
      @Parameter(description = "Chave estável do ato coletivo para impedir efeitos duplicados.")
          @RequestHeader("Idempotency-Key")
          String key) {
    return RestApiResponse.success(
        bulk.confirm(
            Resource.FREQUENCIAS, "ergon.frequencias.homologar-frequencia-lote", request, key),
        null);
  }
}
