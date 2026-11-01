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

/** Publica o contrato do catálogo de definições de campos customizados por órgão e recurso. */
@ResourceFact(
    key = "possuiTabelaOpcoes",
    resourceKey = "administracao-pessoal.campos-customizados",
    label = "Com tabela de opções",
    description = "Verdadeiro quando o campo customizado é alimentado por uma tabela geral HADES.TABELA.",
    dataType = "boolean",
    icon = "table_View",
    tone = "info",
    valuePresentation = "badge",
    sourceKind = "computed")
@ResourceFact(
    key = "possuiRegrasValidacao",
    resourceKey = "administracao-pessoal.campos-customizados",
    label = "Com regras de validação",
    description = "Verdadeiro quando o campo customizado possui regras estruturadas de validação declaradas.",
    dataType = "boolean",
    icon = "rule",
    tone = "info",
    valuePresentation = "badge",
    sourceKind = "computed")
@ResourceFact(
    key = "obrigatorioAtivo",
    resourceKey = "administracao-pessoal.campos-customizados",
    label = "Obrigatório e ativo",
    description = "Verdadeiro quando o campo customizado é simultaneamente obrigatório e ativo no órgão.",
    dataType = "boolean",
    icon = "verified",
    tone = "success",
    valuePresentation = "badge",
    sourceKind = "computed")
@BulkResourceOperations(
    proposalOperationId = "ergon.campos-customizados.bulk.proposal",
    proposalResultsOperationId = "ergon.campos-customizados.bulk.proposal-results",
    executionOperationId = "ergon.campos-customizados.bulk.execution",
    executionResultsOperationId = "ergon.campos-customizados.bulk.results",
    cancelOperationId = "ergon.campos-customizados.bulk.cancel",
    updateSourceOperationId = "updateErgonCampoCustomizado",
    protectedUpdateFields = {
      "empCodigo",
      "resourceKey",
      "fieldKey",
      "fieldType",
      "validationRules",
      "governancePolicy"
    })
@ApiResource(
    value = ApiPaths.CAMPOS_CUSTOMIZADOS,
    resourceKey = "administracao-pessoal.campos-customizados",
    title = "Definições de campos customizados",
    description =
        "Catálogo de definições de campos customizados JSONB por órgão e recurso com governança, validação e tabelas de opções (custom_field_definitions).",
    icon = "tune",
    identity =
        @ResourceIdentity(
            keyField = "id",
            titleField = "label",
            displayLabelField = "label",
            subtitleField = "resourceKey",
            metadataFields = {"fieldKey", "fieldType", "controlType", "active"}),
    quickFilters = {
      @QuickFilter(
          id = "ativos",
          label = "Ativos",
          filter = "{\"somenteAtivos\":true}"),
      @QuickFilter(
          id = "obrigatorios",
          label = "Obrigatórios",
          filter = "{\"somenteObrigatorios\":true}"),
      @QuickFilter(
          id = "com-tabela-opcoes",
          label = "Com tabela de opções",
          filter = "{\"comTabelaOpcoes\":true}"),
      @QuickFilter(
          id = "globais",
          label = "Escopo global",
          filter = "{\"somenteGlobais\":true}"),
      @QuickFilter(
          id = "inativos",
          label = "Inativos",
          filter = "{\"active\":false}")
    })
@ApiGroup(
    value = "ergon",
    title = "Administração de pessoal",
    description = "Cadastros civis e assentamentos funcionais dos órgãos públicos.",
    icon = "groups")
public class DefinicaoCampoCustomizadoController
    extends AbstractResourceController<
        DefinicaoCampoCustomizadoResponseDTO,
        Long,
        DefinicaoCampoCustomizadoFilterDTO,
        CreateDefinicaoCampoCustomizadoDTO,
        UpdateDefinicaoCampoCustomizadoDTO> {

  private final DefinicaoCampoCustomizadoService service;
  private final ResourceVersionEtagService etags;
  private final ResourceVersionScopeProvider scopes;
  @Autowired private ErgonBulkOperations bulk;
  @Autowired private ErgonDeterminationService determinations;

  public DefinicaoCampoCustomizadoController(
      DefinicaoCampoCustomizadoService service,
      ResourceVersionEtagService etags,
      ResourceVersionScopeProvider scopes) {
    this.service = service;
    this.etags = etags;
    this.scopes = scopes;
  }

  @Override
  @PostMapping("/stats/group-by")
  @Operation(
      summary = "Campos customizados por recurso alvo e tipo de dado",
      description =
          "Agrega o catálogo autorizado de campos customizados por recurso e ordem, permitindo abrir a ficha pela identidade do item.")
  @UiAnalytics(
      projections = {
        @AnalyticsProjection(
            id = "campos-por-recurso",
            intent = AnalyticsIntent.COMPOSITION,
            sourceOperation = AnalyticsOperation.GROUP_BY,
            primaryDimension = @AnalyticsDimensionBinding(field = "resourceKey"),
            primaryMetrics =
                @AnalyticsMetricBinding(
                    field = "count",
                    aggregation = "count",
                    label = "Campos customizados"),
            defaultLimit = 20),
        @AnalyticsProjection(
            id = "ordem-por-campo",
            intent = AnalyticsIntent.RANKING,
            sourceOperation = AnalyticsOperation.GROUP_BY,
            primaryDimension = @AnalyticsDimensionBinding(field = "id"),
            primaryMetrics =
                @AnalyticsMetricBinding(
                    field = "fieldOrder",
                    aggregation = "sum",
                    label = "Ordem no grupo"),
            defaultLimit = 20,
            recordOpen =
                @AnalyticsRecordOpen(
                    sourceIdentityField = "key",
                    target =
                        @AnalyticsSurfaceTarget(
                            resourceKey = "administracao-pessoal.campos-customizados",
                            surfaceId = "ficha-campo-customizado")))
      })
  public ResponseEntity<RestApiResponse<GroupByStatsResponse>> groupByStats(
      @RequestBody GroupByStatsRequest<DefinicaoCampoCustomizadoFilterDTO> request) {
    return super.groupByStats(request);
  }

  @Override
  @PostMapping
  @Operation(
      operationId = "createErgonCampoCustomizado",
      summary = "Cadastrar definição de campo customizado",
      description =
          "Inclui novo campo customizado ativo no catálogo do órgão e recurso alvo validando tipagem, controle e tabela de opções.")
  public ResponseEntity<RestApiResponse<DefinicaoCampoCustomizadoResponseDTO>> create(
      @Valid @RequestBody CreateDefinicaoCampoCustomizadoDTO dto) {
    return super.create(dto);
  }

  @Override
  @PutMapping("/{id}")
  @BulkResourceOperation(BulkResourceOperation.Role.UPDATE_SOURCE)
  @Operation(
      operationId = "updateErgonCampoCustomizado",
      summary = "Atualizar definição de campo customizado",
      description =
          "Atualiza apresentação, tipagem, tabela de opções e governança do campo customizado com If-Match.")
  public ResponseEntity<RestApiResponse<DefinicaoCampoCustomizadoResponseDTO>> update(
      @PathVariable Long id, @Valid @RequestBody UpdateDefinicaoCampoCustomizadoDTO dto) {
    return super.update(id, dto);
  }

  @Override
  protected DefinicaoCampoCustomizadoService getService() {
    return service;
  }

  @Override
  protected Long getResponseId(DefinicaoCampoCustomizadoResponseDTO dto) {
    return dto.getId();
  }

  @GetMapping("/{id}/ficha-campo-customizado")
  @PreAuthorize("hasAuthority('ERGON_READ')")
  @Operation(
      summary = "Consultar ficha 360° da definição de campo customizado",
      description =
          "Retorna a projeção completa de leitura da definição de campo customizado para o drawer Quick View.")
  @UiSurface(
      id = "ficha-campo-customizado",
      kind = SurfaceKind.READ_PROJECTION,
      scope = SurfaceScope.ITEM,
      title = "Ficha do campo customizado",
      description =
          "Visão consolidada de tipagem, controle visual, tabela de opções, regras de validação e política de governança.",
      intent = "summary",
      requiredAuthorities = {"ERGON_READ"})
  public ResponseEntity<DefinicaoCampoCustomizadoResponseDTO> ficha(@PathVariable Long id) {
    return withResourceVersion(ResponseEntity.ok(), id, service.findById(id));
  }

  @PatchMapping("/{id}/apresentacao-formulario")
  @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(
      operationId = "patchCampoCustomizadoApresentacao",
      summary = "Atualizar apresentação, grupo e controle visual",
      description =
          "Atualiza rótulo, texto de ajuda, grupo, ordem e controle visual do campo customizado com If-Match.")
  @ResourceIntent(
      id = "apresentacao-formulario",
      title = "Apresentação e controle visual",
      description = "Manutenção parcial do rótulo, ajuda, agrupamento, ordem e controle visual.")
  @UiSurface(
      id = "apresentacao-formulario",
      kind = SurfaceKind.PARTIAL_FORM,
      scope = SurfaceScope.ITEM,
      title = "Apresentação e controle visual",
      description = "Edição focalizada da experiência de formulário do campo customizado.",
      intent = "apresentacao-formulario",
      requiredAuthorities = {"ERGON_WRITE"})
  public ResponseEntity<DefinicaoCampoCustomizadoResponseDTO> atualizarApresentacao(
      @PathVariable Long id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch,
      @Valid @RequestBody AtualizarApresentacaoCampoCustomizadoDTO dto) {
    var p =
        new ResourceVersionUpdatePrecondition<Long>(
            etags, ifMatch, scopes.currentScope(), "administracao-pessoal.campos-customizados", id);
    return withResourceVersion(
        ResponseEntity.ok(), id, service.atualizarApresentacao(id, dto, p));
  }

  @PatchMapping("/{id}/regras-governanca")
  @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(
      operationId = "patchCampoCustomizadoGovernanca",
      summary = "Atualizar obrigatoriedade, tabela de opções, validação e governança",
      description =
          "Atualiza obrigatoriedade, tabela geral de opções, regras de validação e política de governança com If-Match.")
  @ResourceIntent(
      id = "regras-governanca",
      title = "Validação e governança",
      description = "Manutenção parcial de obrigatoriedade, tabela de opções, validação e LGPD.")
  @UiSurface(
      id = "regras-governanca",
      kind = SurfaceKind.PARTIAL_FORM,
      scope = SurfaceScope.ITEM,
      title = "Validação e governança",
      description = "Revisão focalizada das regras de validação e governança semântica do campo.",
      intent = "regras-governanca",
      requiredAuthorities = {"ERGON_WRITE"})
  public ResponseEntity<DefinicaoCampoCustomizadoResponseDTO> atualizarGovernanca(
      @PathVariable Long id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch,
      @Valid @RequestBody AtualizarGovernancaCampoCustomizadoDTO dto) {
    var p =
        new ResourceVersionUpdatePrecondition<Long>(
            etags, ifMatch, scopes.currentScope(), "administracao-pessoal.campos-customizados", id);
    return withResourceVersion(ResponseEntity.ok(), id, service.atualizarGovernanca(id, dto, p));
  }

  @PostMapping("/{id}/actions/ativar-campo-customizado")
  @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(
      summary = "Ativar definição de campo customizado",
      description =
          "Reativa definição de campo customizado inativa para publicação imediata em /schemas/filtered do órgão. Exige If-Match.")
  @WorkflowAction(
      id = "ativar-campo-customizado",
      title = "Ativar campo customizado",
      description = "Ativa o campo customizado para exibição e validação no recurso alvo.",
      scope = ActionScope.ITEM,
      requiredAuthorities = {"ERGON_WRITE"},
      allowedStates = {"INATIVO"},
      confirmationRequired = true,
      resourceVersion = ActionRequirement.REQUIRED,
      resourceVersionTransport = ActionResourceVersionTransport.IF_MATCH)
  public ResponseEntity<DefinicaoCampoCustomizadoResponseDTO> ativar(
      @PathVariable Long id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch,
      @Valid @RequestBody AlterarSituacaoCampoCustomizadoDTO dto) {
    var p =
        new ResourceVersionUpdatePrecondition<Long>(
            etags, ifMatch, scopes.currentScope(), "administracao-pessoal.campos-customizados", id);
    return withResourceVersion(
        ResponseEntity.ok(), id, service.ativarCampoCustomizado(id, dto, p));
  }

  @PostMapping("/{id}/actions/desativar-campo-customizado")
  @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(
      summary = "Desativar definição de campo customizado",
      description =
          "Suspende a publicação da definição de campo customizado sem excluir dados históricos em JSONB. Exige If-Match.")
  @WorkflowAction(
      id = "desativar-campo-customizado",
      title = "Desativar campo customizado",
      description = "Suspende o campo customizado no catálogo ativo do órgão.",
      scope = ActionScope.ITEM,
      requiredAuthorities = {"ERGON_WRITE"},
      allowedStates = {"ATIVO"},
      riskLevel = ActionRiskLevel.HIGH,
      confirmationRequired = true,
      resourceVersion = ActionRequirement.REQUIRED,
      resourceVersionTransport = ActionResourceVersionTransport.IF_MATCH)
  public ResponseEntity<DefinicaoCampoCustomizadoResponseDTO> desativar(
      @PathVariable Long id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch,
      @Valid @RequestBody AlterarSituacaoCampoCustomizadoDTO dto) {
    var p =
        new ResourceVersionUpdatePrecondition<Long>(
            etags, ifMatch, scopes.currentScope(), "administracao-pessoal.campos-customizados", id);
    return withResourceVersion(
        ResponseEntity.ok(), id, service.desativarCampoCustomizado(id, dto, p));
  }

  @PostMapping("/determinations/previa")
  @PreAuthorize("hasAuthority('ERGON_READ')")
  @Operation(
      operationId = "determineErgonCampoCustomizadoPrevia",
      summary = "Determinar controle visual sugerido e descrição da tabela de opções em rascunho",
      description =
          "Projeta o controle visual compatível com o tipo de dado e a descrição da tabela geral HADES.TABELA sem persistir.")
  public ResponseEntity<CampoCustomizadoPreviaPreviewDTO> determinePrevia(
      @Valid @RequestBody CampoCustomizadoPreviaDraftDTO draft) {
    return ResponseEntity.ok(determinations.previaCampoCustomizado(draft));
  }

  @PostMapping("/bulk/update/evaluation")
  @Operation(
      operationId = "ergon.campos-customizados.bulk.uniform.evaluation",
      summary = "Avaliar edição uniforme de campos customizados",
      description =
          "Avalia seleção, versões e alterações de campos customizados sem modificar o catálogo; produz uma proposta protegida.")
  public RestApiResponse<BulkProposal> bulkEvaluate(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              content =
                  @io.swagger.v3.oas.annotations.media.Content(
                      schema =
                          @io.swagger.v3.oas.annotations.media.Schema(
                              implementation =
                                  ErgonBulkSchema.CampoCustomizadoUniform.class)))
          @RequestBody
          BulkUniformEvaluationRequest<String, DefinicaoCampoCustomizadoFilterDTO> request) {
    return RestApiResponse.success(bulk.uniform(Resource.CAMPOS_CUSTOMIZADOS, request), null);
  }

  @PostMapping("/bulk/update-items/evaluation")
  @Operation(
      operationId = "ergon.campos-customizados.bulk.items.evaluation",
      summary = "Avaliar edições por campo customizado",
      description =
          "Avalia deltas distintos e a versão esperada de cada definição de campo customizado, com impedimentos individualizados.")
  public RestApiResponse<BulkProposal> bulkEvaluateItems(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              content =
                  @io.swagger.v3.oas.annotations.media.Content(
                      schema =
                          @io.swagger.v3.oas.annotations.media.Schema(
                              implementation =
                                  ErgonBulkSchema.CampoCustomizadoItems.class)))
          @RequestBody
          BulkItemEvaluationRequest<String> request) {
    return RestApiResponse.success(bulk.items(Resource.CAMPOS_CUSTOMIZADOS, request), null);
  }

  @PostMapping("/bulk/update")
  @BulkOperation(
      mode = BulkMode.UNIFORM_UPDATE,
      evaluationOperationId = "ergon.campos-customizados.bulk.uniform.evaluation",
      atomicity = ActionCollectionAtomicity.PER_ITEM)
  @Operation(
      operationId = "ergon.campos-customizados.bulk.uniform",
      summary = "Confirmar edição uniforme de campos customizados",
      description =
          "Confirma uma proposta avaliada; cada definição de campo customizado é revalidada e possui recibo durável.")
  public RestApiResponse<BulkExecution> bulkConfirm(
      @RequestBody BulkConfirmationRequest request,
      @Parameter(
              description =
                  "Chave estável desta confirmação; repetir a mesma chave recupera a execução.")
          @RequestHeader("Idempotency-Key")
          String key) {
    return RestApiResponse.success(
        bulk.confirm(
            Resource.CAMPOS_CUSTOMIZADOS, "ergon.campos-customizados.bulk.uniform", request, key),
        null);
  }

  @PostMapping("/bulk/update-items")
  @BulkOperation(
      mode = BulkMode.PER_ITEM_UPDATE,
      evaluationOperationId = "ergon.campos-customizados.bulk.items.evaluation",
      atomicity = ActionCollectionAtomicity.PER_ITEM)
  @Operation(
      operationId = "ergon.campos-customizados.bulk.items",
      summary = "Confirmar edições por campo customizado",
      description =
          "Aplica os deltas individualizados preservados na proposta, com versão e resultado próprios por definição.")
  public RestApiResponse<BulkExecution> bulkConfirmItems(
      @RequestBody BulkConfirmationRequest request,
      @Parameter(
              description =
                  "Chave estável de confirmação para recuperar a mesma execução em caso de repetição.")
          @RequestHeader("Idempotency-Key")
          String key) {
    return RestApiResponse.success(
        bulk.confirm(
            Resource.CAMPOS_CUSTOMIZADOS, "ergon.campos-customizados.bulk.items", request, key),
        null);
  }

  @GetMapping("/bulk/proposals/{proposalId}")
  @BulkResourceOperation(BulkResourceOperation.Role.PROPOSAL)
  @Operation(
      operationId = "ergon.campos-customizados.bulk.proposal",
      summary = "Consultar proposta de lote de campos customizados",
      description =
          "Consulta totais e intenção pública da proposta após conferir acesso ao conjunto completo de definições.")
  public RestApiResponse<BulkProposal> bulkProposal(
      @Parameter(description = "Identidade da proposta avaliada.") @PathVariable UUID proposalId) {
    return RestApiResponse.success(
        bulk.proposal(
            Resource.CAMPOS_CUSTOMIZADOS,
            bulk.locate(Resource.CAMPOS_CUSTOMIZADOS, proposalId, false),
            proposalId),
        null);
  }

  @GetMapping("/bulk/proposals/{proposalId}/results")
  @BulkResourceOperation(BulkResourceOperation.Role.PROPOSAL_RESULTS)
  @Operation(
      operationId = "ergon.campos-customizados.bulk.proposal-results",
      summary = "Inspecionar avaliação do lote de campos customizados",
      description =
          "Pagina definições executáveis e bloqueadas, com diagnósticos seguros e cursor vinculado ao solicitante.")
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
            Resource.CAMPOS_CUSTOMIZADOS,
            bulk.locate(Resource.CAMPOS_CUSTOMIZADOS, proposalId, false),
            proposalId,
            size,
            after),
        null);
  }

  @GetMapping("/bulk/executions/{executionId}")
  @BulkResourceOperation(BulkResourceOperation.Role.EXECUTION)
  @Operation(
      operationId = "ergon.campos-customizados.bulk.execution",
      summary = "Consultar execução do lote de campos customizados",
      description = "Retorna estado e contadores reconciliados a partir dos recibos persistidos.")
  public RestApiResponse<BulkExecution> bulkExecution(
      @Parameter(description = "Identidade da execução confirmada.")
          @PathVariable
          UUID executionId) {
    return RestApiResponse.success(
        bulk.execution(
            Resource.CAMPOS_CUSTOMIZADOS,
            bulk.locate(Resource.CAMPOS_CUSTOMIZADOS, executionId, true),
            executionId),
        null);
  }

  @GetMapping("/bulk/executions/{executionId}/results")
  @BulkResourceOperation(BulkResourceOperation.Role.EXECUTION_RESULTS)
  @Operation(
      operationId = "ergon.campos-customizados.bulk.results",
      summary = "Consultar recibos do lote de campos customizados",
      description = "Pagina resultados por definição sem expor dados sensíveis da proposta.")
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
            Resource.CAMPOS_CUSTOMIZADOS,
            bulk.locate(Resource.CAMPOS_CUSTOMIZADOS, executionId, true),
            executionId,
            size,
            after),
        null);
  }

  @PostMapping("/bulk/executions/{executionId}/cancel")
  @BulkResourceOperation(BulkResourceOperation.Role.CANCEL)
  @Operation(
      operationId = "ergon.campos-customizados.bulk.cancel",
      summary = "Solicitar parada do lote de campos customizados",
      description =
          "Registra a solicitação de parada sem desfazer alterações já confirmadas e sem reabrir efeitos no replay.")
  public RestApiResponse<BulkExecution> bulkCancel(
      @Parameter(description = "Identidade da execução a interromper.")
          @PathVariable
          UUID executionId) {
    return RestApiResponse.success(
        bulk.cancel(
            Resource.CAMPOS_CUSTOMIZADOS,
            bulk.locate(Resource.CAMPOS_CUSTOMIZADOS, executionId, true),
            executionId),
        null);
  }

  @PostMapping("/bulk/ativar-campo-customizado-lote/evaluation")
  @Operation(
      operationId = "ergon.campos-customizados.ativar-campo-customizado-lote.evaluation",
      summary = "Avaliar ativação coletiva de campos customizados",
      description =
          "Avalia ativação coletiva de definições de campos customizados inativas do mesmo órgão, sem persistir alterações.")
  public RestApiResponse<BulkProposal> bulkAtivarEvaluate(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              content =
                  @io.swagger.v3.oas.annotations.media.Content(
                      schema =
                          @io.swagger.v3.oas.annotations.media.Schema(
                              implementation =
                                  ErgonBulkSchema.AtivarCampoCustomizadoCommand.class)))
          @RequestBody
          BulkCommandEvaluationRequest<
                  AtivarCampoCustomizadoLoteDTO, String, DefinicaoCampoCustomizadoFilterDTO>
              request) {
    return RestApiResponse.success(
        bulk.command(Resource.CAMPOS_CUSTOMIZADOS, "ativar-campo-customizado-lote", request), null);
  }

  @PostMapping("/actions/ativar-campo-customizado-lote")
  @WorkflowAction(
      id = "ativar-campo-customizado-lote",
      title = "Ativar campos customizados em lote",
      description = "Ativa coletivamente definições de campos customizados inativas no órgão.",
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
          "ergon.campos-customizados.ativar-campo-customizado-lote.evaluation",
      atomicity = ActionCollectionAtomicity.PER_ITEM)
  @Operation(
      operationId = "ergon.campos-customizados.ativar-campo-customizado-lote",
      summary = "Ativar campos customizados em lote",
      description =
          "Executa ativação coletiva governada de definições de campos customizados inativas.")
  public RestApiResponse<BulkExecution> bulkAtivarConfirm(
      @RequestBody BulkConfirmationRequest request,
      @Parameter(description = "Chave estável do ato coletivo para impedir efeitos duplicados.")
          @RequestHeader("Idempotency-Key")
          String key) {
    return RestApiResponse.success(
        bulk.confirm(
            Resource.CAMPOS_CUSTOMIZADOS,
            "ergon.campos-customizados.ativar-campo-customizado-lote",
            request,
            key),
        null);
  }
}
