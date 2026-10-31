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

/** Publica a matriz estatutária de parâmetros de contagem de férias (PARAMETROS_CONTA) através da hierarquia canônica Praxis. */
@ResourceFact(
    key = "escopoGlobal",
    resourceKey = "administracao-pessoal.parametros-contagem-ferias",
    label = "Regra de escopo global",
    description = "Verdadeiro quando a regra vale para todos os órgãos por não restringir empCodigo.",
    dataType = "boolean",
    sourceKind = "computed")
@ResourceFact(
    key = "permiteParcelamento",
    resourceKey = "administracao-pessoal.parametros-contagem-ferias",
    label = "Permite parcelamento",
    description = "Verdadeiro quando maxParcelasFerias autoriza mais de uma etapa de gozo por período aquisitivo.",
    dataType = "boolean",
    sourceKind = "computed")
@ResourceFact(
    key = "permiteAbonoPecuniario",
    resourceKey = "administracao-pessoal.parametros-contagem-ferias",
    label = "Permite abono pecuniário",
    description = "Verdadeiro quando a regra habilita venda de dias em abono pecuniário.",
    dataType = "boolean",
    sourceKind = "computed")
@BulkResourceOperations(
    proposalOperationId = "ergon.parametros-contagem-ferias.bulk.proposal",
    proposalResultsOperationId = "ergon.parametros-contagem-ferias.bulk.proposal-results",
    executionOperationId = "ergon.parametros-contagem-ferias.bulk.execution",
    executionResultsOperationId = "ergon.parametros-contagem-ferias.bulk.results",
    cancelOperationId = "ergon.parametros-contagem-ferias.bulk.cancel",
    updateSourceOperationId = "updateErgonParametroContagemFerias",
    protectedUpdateFields = {"dtini", "active"})
@ApiResource(
    value = ApiPaths.PARAMETROS_CONTAGEM_FERIAS,
    resourceKey = "administracao-pessoal.parametros-contagem-ferias",
    title = "Parâmetros de contagem de férias",
    description =
        "Matriz estatutária de parâmetros de contagem de férias, abono pecuniário, gozo antes do fim do PA, prescrição, contagem em dobro, licença especial e progressão funcional (PARAMETROS_CONTA).",
    icon = "rule_settings",
    identity =
        @ResourceIdentity(
            keyField = "id",
            titleField = "regimeJuridico",
            displayLabelField = "regimeJuridico",
            subtitleField = "categoria",
            metadataFields = {"tipoVinculo", "subcategoria", "dtini", "active"}),
    quickFilters = {
      @QuickFilter(
          id = "ativos",
          label = "Ativos",
          filter = "{\"active\":true}"),
      @QuickFilter(
          id = "vigentes",
          label = "Vigência aberta",
          filter = "{\"vigenciaEmAberto\":true}"),
      @QuickFilter(
          id = "globais",
          label = "Escopo global",
          filter = "{\"regraGeralMultiOrgao\":true}"),
      @QuickFilter(
          id = "com-abono-pecuniario",
          label = "Com abono pecuniário",
          filter = "{\"maxDiasVendFerMinimo\":1}"),
      @QuickFilter(
          id = "com-licenca-especial",
          label = "Com licença especial",
          filter = "{\"temLicEsp\":true}")
    })
@ApiGroup(
    value = "ergon",
    title = "Administração de pessoal",
    description = "Cadastros civis e assentamentos funcionais dos órgãos públicos.",
    icon = "groups")
public class ParametroContagemFeriasController
    extends AbstractResourceController<
        ParametroContagemFeriasResponseDTO,
        Long,
        ParametroContagemFeriasFilterDTO,
        CreateParametroContagemFeriasDTO,
        UpdateParametroContagemFeriasDTO> {

  private final ParametroContagemFeriasService service;
  private final ResourceVersionEtagService etags;
  private final ResourceVersionScopeProvider scopes;
  @Autowired private ErgonBulkOperations bulk;
  @Autowired private ErgonDeterminationService determinations;

  public ParametroContagemFeriasController(
      ParametroContagemFeriasService service,
      ResourceVersionEtagService etags,
      ResourceVersionScopeProvider scopes) {
    this.service = service;
    this.etags = etags;
    this.scopes = scopes;
  }

  @Override
  @PostMapping("/stats/group-by")
  @Operation(
      summary = "Parâmetros de contagem de férias por regime jurídico e máximo de parcelas",
      description =
          "Agrega o catálogo autorizado de parâmetros de contagem de férias com abertura direta da ficha pela identidade real do registro.")
  @UiAnalytics(
      projections = {
        @AnalyticsProjection(
            id = "parametros-por-regime",
            intent = AnalyticsIntent.COMPOSITION,
            sourceOperation = AnalyticsOperation.GROUP_BY,
            primaryDimension = @AnalyticsDimensionBinding(field = "regimeJuridico"),
            primaryMetrics =
                @AnalyticsMetricBinding(
                    field = "count",
                    aggregation = "count",
                    label = "Parâmetros"),
            defaultLimit = 20),
        @AnalyticsProjection(
            id = "parcelas-por-parametro",
            intent = AnalyticsIntent.RANKING,
            sourceOperation = AnalyticsOperation.GROUP_BY,
            primaryDimension = @AnalyticsDimensionBinding(field = "id"),
            primaryMetrics =
                @AnalyticsMetricBinding(
                    field = "maxPerFer",
                    aggregation = "sum",
                    label = "Máximo de parcelas"),
            defaultLimit = 20,
            recordOpen =
                @AnalyticsRecordOpen(
                    sourceIdentityField = "key",
                    target =
                        @AnalyticsSurfaceTarget(
                            resourceKey = "administracao-pessoal.parametros-contagem-ferias",
                            surfaceId = "ficha-parametro-contagem-ferias")))
      })
  public ResponseEntity<RestApiResponse<GroupByStatsResponse>> groupByStats(
      @RequestBody GroupByStatsRequest<ParametroContagemFeriasFilterDTO> request) {
    return super.groupByStats(request);
  }

  @Override
  @PostMapping
  @Operation(
      operationId = "createErgonParametroContagemFerias",
      summary = "Cadastrar parâmetro estatutário de contagem de férias",
      description =
          "Inclui nova matriz estatutária de contagem de férias, abono pecuniário, prescrição e licença especial validando vigência e sobreposição de escopo.")
  public ResponseEntity<RestApiResponse<ParametroContagemFeriasResponseDTO>> create(
      @Valid @RequestBody CreateParametroContagemFeriasDTO dto) {
    return super.create(dto);
  }

  @Override
  @PutMapping("/{id}")
  @BulkResourceOperation(BulkResourceOperation.Role.UPDATE_SOURCE)
  @Operation(
      operationId = "updateErgonParametroContagemFerias",
      summary = "Atualizar parâmetro estatutário de contagem de férias",
      description =
          "Atualiza regras de parcelamento, abono pecuniário, prescrição, contagem em dobro e licença especial com If-Match.")
  public ResponseEntity<RestApiResponse<ParametroContagemFeriasResponseDTO>> update(
      @PathVariable Long id, @Valid @RequestBody UpdateParametroContagemFeriasDTO dto) {
    return super.update(id, dto);
  }

  @Override
  protected ParametroContagemFeriasService getService() {
    return service;
  }

  @Override
  protected Long getResponseId(ParametroContagemFeriasResponseDTO dto) {
    return dto.getId();
  }

  @GetMapping("/{id}/ficha-parametro-contagem-ferias")
  @PreAuthorize("hasAuthority('ERGON_READ')")
  @Operation(
      summary = "Consultar ficha 360° do parâmetro de contagem de férias",
      description =
          "Retorna a projeção completa de leitura da matriz estatutária de férias para o drawer Quick View.")
  @UiSurface(
      id = "ficha-parametro-contagem-ferias",
      kind = SurfaceKind.READ_PROJECTION,
      scope = SurfaceScope.ITEM,
      title = "Ficha do parâmetro de férias",
      description =
          "Visão consolidada do escopo estatutário, parcelamento, abono pecuniário, prescrição e licença especial.",
      intent = "summary",
      requiredAuthorities = {"ERGON_READ"})
  public ResponseEntity<ParametroContagemFeriasResponseDTO> ficha(@PathVariable Long id) {
    return withResourceVersion(ResponseEntity.ok(), id, service.findById(id));
  }

  @PatchMapping("/{id}/parcelamento-abono")
  @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(
      operationId = "patchParametroFeriasParcelamentoAbono",
      summary = "Atualizar parcelamento, abono pecuniário e prescrição",
      description =
          "Atualiza limites de parcelamento, abono pecuniário, gozo antes do fim do PA e prescrição com If-Match.")
  @ResourceIntent(
      id = "parcelamento-abono",
      title = "Parcelamento, abono e prescrição",
      description = "Manutenção parcial das regras de parcelamento, abono pecuniário e prescrição.")
  @UiSurface(
      id = "parcelamento-abono",
      kind = SurfaceKind.PARTIAL_FORM,
      scope = SurfaceScope.ITEM,
      title = "Parcelamento, abono e prescrição",
      description = "Revisão focalizada de etapas de gozo, abono pecuniário e prescrição de férias.",
      intent = "parcelamento-abono",
      requiredAuthorities = {"ERGON_WRITE"})
  public ResponseEntity<ParametroContagemFeriasResponseDTO> atualizarParcelamentoAbono(
      @PathVariable Long id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch,
      @Valid @RequestBody AtualizarParcelamentoAbonoParametroFeriasDTO dto) {
    var p =
        new ResourceVersionUpdatePrecondition<Long>(
            etags,
            ifMatch,
            scopes.currentScope(),
            "administracao-pessoal.parametros-contagem-ferias",
            id);
    return withResourceVersion(
        ResponseEntity.ok(), id, service.atualizarParcelamentoAbono(id, dto, p));
  }

  @PatchMapping("/{id}/licenca-progressao")
  @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(
      operationId = "patchParametroFeriasLicencaProgressao",
      summary = "Atualizar licença especial e progressão funcional",
      description =
          "Atualiza parâmetros de licença especial, contagem em dobro e progressão funcional com If-Match.")
  @ResourceIntent(
      id = "licenca-progressao",
      title = "Licença especial e progressão",
      description = "Manutenção parcial dos parâmetros de licença especial e progressão funcional.")
  @UiSurface(
      id = "licenca-progressao",
      kind = SurfaceKind.PARTIAL_FORM,
      scope = SurfaceScope.ITEM,
      title = "Licença especial e progressão",
      description = "Revisão focalizada de licença especial, conversão em dobro e progressão.",
      intent = "licenca-progressao",
      requiredAuthorities = {"ERGON_WRITE"})
  public ResponseEntity<ParametroContagemFeriasResponseDTO> atualizarLicencaProgressao(
      @PathVariable Long id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch,
      @Valid @RequestBody AtualizarLicencaProgressaoParametroFeriasDTO dto) {
    var p =
        new ResourceVersionUpdatePrecondition<Long>(
            etags,
            ifMatch,
            scopes.currentScope(),
            "administracao-pessoal.parametros-contagem-ferias",
            id);
    return withResourceVersion(
        ResponseEntity.ok(), id, service.atualizarLicencaProgressao(id, dto, p));
  }

  @PostMapping("/{id}/actions/ativar-parametro-ferias")
  @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(
      summary = "Ativar parâmetro estatutário de contagem de férias",
      description =
          "Reativa matriz estatutária de contagem de férias validando ausência de sobreposição de vigência. Exige If-Match.")
  @WorkflowAction(
      id = "ativar-parametro-ferias",
      title = "Ativar parâmetro de férias",
      description = "Reativa a matriz estatutária de contagem de férias no catálogo do órgão.",
      scope = ActionScope.ITEM,
      requiredAuthorities = {"ERGON_WRITE"},
      allowedStates = {"INATIVO"},
      confirmationRequired = true,
      resourceVersion = ActionRequirement.REQUIRED,
      resourceVersionTransport = ActionResourceVersionTransport.IF_MATCH)
  public ResponseEntity<ParametroContagemFeriasResponseDTO> ativar(
      @PathVariable Long id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch,
      @Valid @RequestBody AlterarVigenciaParametroFeriasDTO dto) {
    var p =
        new ResourceVersionUpdatePrecondition<Long>(
            etags,
            ifMatch,
            scopes.currentScope(),
            "administracao-pessoal.parametros-contagem-ferias",
            id);
    return withResourceVersion(ResponseEntity.ok(), id, service.ativarParametro(id, dto, p));
  }

  @PostMapping("/{id}/actions/encerrar-vigencia-parametro-ferias")
  @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(
      summary = "Encerrar vigência de parâmetro estatutário de férias",
      description =
          "Registra data final de vigência da matriz estatutária de contagem de férias ativa. Exige If-Match.")
  @WorkflowAction(
      id = "encerrar-vigencia-parametro-ferias",
      title = "Encerrar vigência",
      description = "Define a data de término de vigência do parâmetro estatutário ativo.",
      scope = ActionScope.ITEM,
      requiredAuthorities = {"ERGON_WRITE"},
      allowedStates = {"ATIVO"},
      confirmationRequired = true,
      resourceVersion = ActionRequirement.REQUIRED,
      resourceVersionTransport = ActionResourceVersionTransport.IF_MATCH)
  public ResponseEntity<ParametroContagemFeriasResponseDTO> encerrarVigencia(
      @PathVariable Long id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch,
      @Valid @RequestBody AlterarVigenciaParametroFeriasDTO dto) {
    var p =
        new ResourceVersionUpdatePrecondition<Long>(
            etags,
            ifMatch,
            scopes.currentScope(),
            "administracao-pessoal.parametros-contagem-ferias",
            id);
    return withResourceVersion(
        ResponseEntity.ok(), id, service.encerrarVigenciaParametro(id, dto, p));
  }

  @PostMapping("/{id}/actions/desativar-parametro-ferias")
  @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(
      summary = "Desativar parâmetro estatutário de contagem de férias",
      description =
          "Suspende a aplicação da matriz estatutária de contagem de férias no catálogo ativo. Exige If-Match.")
  @WorkflowAction(
      id = "desativar-parametro-ferias",
      title = "Desativar parâmetro de férias",
      description = "Suspende o parâmetro estatutário de contagem de férias no órgão.",
      scope = ActionScope.ITEM,
      requiredAuthorities = {"ERGON_WRITE"},
      allowedStates = {"ATIVO"},
      riskLevel = ActionRiskLevel.HIGH,
      confirmationRequired = true,
      resourceVersion = ActionRequirement.REQUIRED,
      resourceVersionTransport = ActionResourceVersionTransport.IF_MATCH)
  public ResponseEntity<ParametroContagemFeriasResponseDTO> desativar(
      @PathVariable Long id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch,
      @Valid @RequestBody AlterarVigenciaParametroFeriasDTO dto) {
    var p =
        new ResourceVersionUpdatePrecondition<Long>(
            etags,
            ifMatch,
            scopes.currentScope(),
            "administracao-pessoal.parametros-contagem-ferias",
            id);
    return withResourceVersion(
        ResponseEntity.ok(), id, service.inativarParametro(id, dto, p));
  }

  @PostMapping("/determinations/previa")
  @PreAuthorize("hasAuthority('ERGON_READ')")
  @Operation(
      operationId = "determineErgonParametroContagemFeriasPrevia",
      summary = "Determinar órgão, descrição do regime e síntese da regra estatutária em rascunho",
      description =
          "Projeta o nome do órgão, a descrição do regime jurídico e o resumo executivo da regra de férias sem persistir.")
  public ResponseEntity<ParametroContagemFeriasPreviaPreviewDTO> determinePrevia(
      @Valid @RequestBody ParametroContagemFeriasPreviaDraftDTO draft) {
    return ResponseEntity.ok(determinations.previaParametroContagemFerias(draft));
  }

  @PostMapping("/bulk/update/evaluation")
  @Operation(
      operationId = "ergon.parametros-contagem-ferias.bulk.uniform.evaluation",
      summary = "Avaliar edição uniforme de parâmetros de contagem de férias",
      description =
          "Avalia seleção, versões e alterações de parâmetros de contagem de férias sem modificar o catálogo; produz uma proposta protegida.")
  public RestApiResponse<BulkProposal> bulkEvaluate(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              content =
                  @io.swagger.v3.oas.annotations.media.Content(
                      schema =
                          @io.swagger.v3.oas.annotations.media.Schema(
                              implementation =
                                  ErgonBulkSchema.ParametroContagemFeriasUniform.class)))
          @RequestBody
          BulkUniformEvaluationRequest<String, ParametroContagemFeriasFilterDTO> request) {
    return RestApiResponse.success(
        bulk.uniform(Resource.PARAMETROS_CONTAGEM_FERIAS, request), null);
  }

  @PostMapping("/bulk/update-items/evaluation")
  @Operation(
      operationId = "ergon.parametros-contagem-ferias.bulk.items.evaluation",
      summary = "Avaliar edições por parâmetro de contagem de férias",
      description =
          "Avalia deltas distintos e a versão esperada de cada parâmetro estatutário de férias, com impedimentos individualizados.")
  public RestApiResponse<BulkProposal> bulkEvaluateItems(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              content =
                  @io.swagger.v3.oas.annotations.media.Content(
                      schema =
                          @io.swagger.v3.oas.annotations.media.Schema(
                              implementation =
                                  ErgonBulkSchema.ParametroContagemFeriasItems.class)))
          @RequestBody
          BulkItemEvaluationRequest<String> request) {
    return RestApiResponse.success(bulk.items(Resource.PARAMETROS_CONTAGEM_FERIAS, request), null);
  }

  @PostMapping("/bulk/update")
  @BulkOperation(
      mode = BulkMode.UNIFORM_UPDATE,
      evaluationOperationId = "ergon.parametros-contagem-ferias.bulk.uniform.evaluation",
      atomicity = ActionCollectionAtomicity.PER_ITEM)
  @Operation(
      operationId = "ergon.parametros-contagem-ferias.bulk.uniform",
      summary = "Confirmar edição uniforme de parâmetros de contagem de férias",
      description =
          "Confirma uma proposta avaliada; cada parâmetro de contagem de férias é revalidado e possui recibo durável.")
  public RestApiResponse<BulkExecution> bulkConfirm(
      @RequestBody BulkConfirmationRequest request,
      @Parameter(
              description =
                  "Chave estável desta confirmação; repetir a mesma chave recupera a execução.")
          @RequestHeader("Idempotency-Key")
          String key) {
    return RestApiResponse.success(
        bulk.confirm(
            Resource.PARAMETROS_CONTAGEM_FERIAS,
            "ergon.parametros-contagem-ferias.bulk.uniform",
            request,
            key),
        null);
  }

  @PostMapping("/bulk/update-items")
  @BulkOperation(
      mode = BulkMode.PER_ITEM_UPDATE,
      evaluationOperationId = "ergon.parametros-contagem-ferias.bulk.items.evaluation",
      atomicity = ActionCollectionAtomicity.PER_ITEM)
  @Operation(
      operationId = "ergon.parametros-contagem-ferias.bulk.items",
      summary = "Confirmar edições por parâmetro de contagem de férias",
      description =
          "Aplica os deltas individualizados preservados na proposta, com versão e resultado próprios por parâmetro.")
  public RestApiResponse<BulkExecution> bulkConfirmItems(
      @RequestBody BulkConfirmationRequest request,
      @Parameter(
              description =
                  "Chave estável de confirmação para recuperar a mesma execução em caso de repetição.")
          @RequestHeader("Idempotency-Key")
          String key) {
    return RestApiResponse.success(
        bulk.confirm(
            Resource.PARAMETROS_CONTAGEM_FERIAS,
            "ergon.parametros-contagem-ferias.bulk.items",
            request,
            key),
        null);
  }

  @GetMapping("/bulk/proposals/{proposalId}")
  @BulkResourceOperation(BulkResourceOperation.Role.PROPOSAL)
  @Operation(
      operationId = "ergon.parametros-contagem-ferias.bulk.proposal",
      summary = "Consultar proposta de lote de parâmetros de férias",
      description =
          "Consulta totais e intenção pública da proposta após conferir acesso ao conjunto completo de parâmetros.")
  public RestApiResponse<BulkProposal> bulkProposal(
      @Parameter(description = "Identidade da proposta avaliada.") @PathVariable UUID proposalId) {
    return RestApiResponse.success(
        bulk.proposal(
            Resource.PARAMETROS_CONTAGEM_FERIAS,
            bulk.locate(Resource.PARAMETROS_CONTAGEM_FERIAS, proposalId, false),
            proposalId),
        null);
  }

  @GetMapping("/bulk/proposals/{proposalId}/results")
  @BulkResourceOperation(BulkResourceOperation.Role.PROPOSAL_RESULTS)
  @Operation(
      operationId = "ergon.parametros-contagem-ferias.bulk.proposal-results",
      summary = "Inspecionar avaliação do lote de parâmetros de férias",
      description =
          "Pagina parâmetros executáveis e bloqueados, com diagnósticos seguros e cursor vinculado ao solicitante.")
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
            Resource.PARAMETROS_CONTAGEM_FERIAS,
            bulk.locate(Resource.PARAMETROS_CONTAGEM_FERIAS, proposalId, false),
            proposalId,
            size,
            after),
        null);
  }

  @GetMapping("/bulk/executions/{executionId}")
  @BulkResourceOperation(BulkResourceOperation.Role.EXECUTION)
  @Operation(
      operationId = "ergon.parametros-contagem-ferias.bulk.execution",
      summary = "Consultar execução do lote de parâmetros de férias",
      description = "Retorna estado e contadores reconciliados a partir dos recibos persistidos.")
  public RestApiResponse<BulkExecution> bulkExecution(
      @Parameter(description = "Identidade da execução confirmada.")
          @PathVariable
          UUID executionId) {
    return RestApiResponse.success(
        bulk.execution(
            Resource.PARAMETROS_CONTAGEM_FERIAS,
            bulk.locate(Resource.PARAMETROS_CONTAGEM_FERIAS, executionId, true),
            executionId),
        null);
  }

  @GetMapping("/bulk/executions/{executionId}/results")
  @BulkResourceOperation(BulkResourceOperation.Role.EXECUTION_RESULTS)
  @Operation(
      operationId = "ergon.parametros-contagem-ferias.bulk.results",
      summary = "Consultar recibos do lote de parâmetros de férias",
      description = "Pagina resultados por parâmetro sem expor dados sensíveis da proposta.")
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
            Resource.PARAMETROS_CONTAGEM_FERIAS,
            bulk.locate(Resource.PARAMETROS_CONTAGEM_FERIAS, executionId, true),
            executionId,
            size,
            after),
        null);
  }

  @PostMapping("/bulk/executions/{executionId}/cancel")
  @BulkResourceOperation(BulkResourceOperation.Role.CANCEL)
  @Operation(
      operationId = "ergon.parametros-contagem-ferias.bulk.cancel",
      summary = "Solicitar parada do lote de parâmetros de férias",
      description =
          "Registra a solicitação de parada sem desfazer alterações já confirmadas e sem reabrir efeitos no replay.")
  public RestApiResponse<BulkExecution> bulkCancel(
      @Parameter(description = "Identidade da execução a interromper.")
          @PathVariable
          UUID executionId) {
    return RestApiResponse.success(
        bulk.cancel(
            Resource.PARAMETROS_CONTAGEM_FERIAS,
            bulk.locate(Resource.PARAMETROS_CONTAGEM_FERIAS, executionId, true),
            executionId),
        null);
  }

  @PostMapping("/bulk/ativar-parametro-ferias-lote/evaluation")
  @Operation(
      operationId = "ergon.parametros-contagem-ferias.ativar-parametro-ferias-lote.evaluation",
      summary = "Avaliar ativação coletiva de parâmetros de contagem de férias",
      description =
          "Avalia ativação coletiva de parâmetros de contagem de férias inativos do mesmo órgão, sem persistir alterações.")
  public RestApiResponse<BulkProposal> bulkAtivarEvaluate(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              content =
                  @io.swagger.v3.oas.annotations.media.Content(
                      schema =
                          @io.swagger.v3.oas.annotations.media.Schema(
                              implementation =
                                  ErgonBulkSchema.AtivarParametroContagemFeriasCommand.class)))
          @RequestBody
          BulkCommandEvaluationRequest<
                  AlterarVigenciaParametroFeriasDTO, String, ParametroContagemFeriasFilterDTO>
              request) {
    return RestApiResponse.success(
        bulk.command(
            Resource.PARAMETROS_CONTAGEM_FERIAS, "ativar-parametro-ferias-lote", request),
        null);
  }

  @PostMapping("/actions/ativar-parametro-ferias-lote")
  @WorkflowAction(
      id = "ativar-parametro-ferias-lote",
      title = "Ativar parâmetros de férias em lote",
      description = "Ativa coletivamente parâmetros de contagem de férias inativos no órgão.",
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
          "ergon.parametros-contagem-ferias.ativar-parametro-ferias-lote.evaluation",
      atomicity = ActionCollectionAtomicity.PER_ITEM)
  @Operation(
      operationId = "ergon.parametros-contagem-ferias.ativar-parametro-ferias-lote",
      summary = "Ativar parâmetros de férias em lote",
      description =
          "Executa ativação coletiva governada de parâmetros de contagem de férias inativos.")
  public RestApiResponse<BulkExecution> bulkAtivarConfirm(
      @RequestBody BulkConfirmationRequest request,
      @Parameter(description = "Chave estável do ato coletivo para impedir efeitos duplicados.")
          @RequestHeader("Idempotency-Key")
          String key) {
    return RestApiResponse.success(
        bulk.confirm(
            Resource.PARAMETROS_CONTAGEM_FERIAS,
            "ergon.parametros-contagem-ferias.ativar-parametro-ferias-lote",
            request,
            key),
        null);
  }
}
