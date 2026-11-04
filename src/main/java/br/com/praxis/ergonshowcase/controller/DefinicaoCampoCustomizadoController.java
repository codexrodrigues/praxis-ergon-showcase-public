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
@ResourceFact(
    key = "fieldOrder",
    resourceKey = "administracao-pessoal.campos-customizados",
    label = "Ordem no grupo",
    description = "Posição numérica do campo customizado dentro da seção do formulário dinâmico.",
    dataType = "integer",
    icon = "format_list_numbered",
    tone = "neutral",
    valuePresentation = "metric",
    sourceKind = "field",
    thresholds = {
      @Threshold(min = 0, max = 30, tone = "info", label = "Topo"),
      @Threshold(min = 30, max = 70, tone = "neutral", label = "Intermediário"),
      @Threshold(min = 70, tone = "warning", label = "Final")
    })
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
            metadataFields = {"empCodigo", "active"}),
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
    extends org.praxisplatform.uischema.controller.base.AbstractBulkGovernedResourceController<
        DefinicaoCampoCustomizadoResponseDTO,
        Long,
        DefinicaoCampoCustomizadoFilterDTO,
        CreateDefinicaoCampoCustomizadoDTO,
        UpdateDefinicaoCampoCustomizadoDTO> {

  private final DefinicaoCampoCustomizadoService service;
  @Autowired private ErgonBulkOperations bulk;
  @Autowired private ErgonDeterminationService determinations;

  public DefinicaoCampoCustomizadoController(
      DefinicaoCampoCustomizadoService service,
      ResourceVersionEtagService etags,
      ResourceVersionScopeProvider scopes) {
    this.service = service;
    configureResourceVersioning(etags, scopes);
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
                            surfaceId = "ficha-campo-customizado"))),
        @AnalyticsProjection(
            id = "evolucao-atualizacao-campos-customizados",
            intent = AnalyticsIntent.TREND,
            sourceOperation = AnalyticsOperation.TIMESERIES,
            primaryDimension = @AnalyticsDimensionBinding(field = "updatedAt"),
            defaultGranularity = AnalyticsGranularity.MONTH,
            defaultTimezone = "America/Sao_Paulo",
            primaryMetrics =
                @AnalyticsMetricBinding(
                    field = "count",
                    aggregation = "count",
                    label = "Definições atualizadas no mês")),
        @AnalyticsProjection(
            id = "distribuicao-ordem-campos-customizados",
            intent = AnalyticsIntent.DISTRIBUTION,
            sourceOperation = AnalyticsOperation.DISTRIBUTION,
            primaryDimension = @AnalyticsDimensionBinding(field = "fieldOrder"),
            primaryMetrics =
                @AnalyticsMetricBinding(
                    field = "fieldOrder",
                    aggregation = "histogram",
                    label = "Distribuição de ordem no grupo")),
        @AnalyticsProjection(
            id = "comparativo-campos-por-recurso",
            intent = AnalyticsIntent.COMPARISON,
            sourceOperation = AnalyticsOperation.COMPARISON,
            primaryDimension = @AnalyticsDimensionBinding(field = "resourceKey"),
            comparisonPeriod =
                @AnalyticsComparisonPeriodBinding(
                    field = "updatedAt",
                    timezone = "America/Sao_Paulo"),
            primaryMetrics =
                @AnalyticsMetricBinding(
                    field = "count",
                    aggregation = "count",
                    label = "Variação por recurso alvo"))
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
    return ErgonHttpContract.itemBody(executeVersionedItemPatch("apresentacao-formulario", id, ifMatch, condition -> service.atualizarApresentacao(id, dto, condition)));
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
    return ErgonHttpContract.itemBody(executeVersionedItemPatch("regras-governanca", id, ifMatch, condition -> service.atualizarGovernanca(id, dto, condition)));
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
    return ErgonHttpContract.itemBody(executeVersionedItemCommand("ativar-campo-customizado", id, ifMatch, null, dto,
        condition -> service.ativarCampoCustomizado(id, dto, condition)));
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
    return ErgonHttpContract.itemBody(executeVersionedItemCommand("desativar-campo-customizado", id, ifMatch, null, dto,
        condition -> service.desativarCampoCustomizado(id, dto, condition)));
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

  @Override
  @PostMapping("/bulk/update/evaluation")
  @Operation(
      operationId = "ergon.campos-customizados.bulk.uniform.evaluation",
      summary = "Avaliar edição uniforme de campos customizados",
      description =
          "Avalia seleção, versões e alterações de campos customizados sem modificar o catálogo; produz uma proposta protegida.")
  @BulkResourceOperation(BulkResourceOperation.Role.UNIFORM_EVALUATION)
  public org.springframework.http.ResponseEntity<RestApiResponse<BulkProposal>> evaluateBulkUpdate(@RequestBody BulkUniformEvaluationRequest<?, DefinicaoCampoCustomizadoFilterDTO> request) {
    return super.evaluateBulkUpdate(request);
  }

  @Override
  @PostMapping("/bulk/update-items/evaluation")
  @Operation(
      operationId = "ergon.campos-customizados.bulk.items.evaluation",
      summary = "Avaliar edições por campo customizado",
      description =
          "Avalia deltas distintos e a versão esperada de cada definição de campo customizado, com impedimentos individualizados.")
  @BulkResourceOperation(BulkResourceOperation.Role.ITEM_EVALUATION)
  public org.springframework.http.ResponseEntity<RestApiResponse<BulkProposal>> evaluateBulkUpdateItems(@RequestBody BulkItemEvaluationRequest<?> request) {
    return super.evaluateBulkUpdateItems(request);
  }

  @Override
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
  @BulkResourceOperation(BulkResourceOperation.Role.UNIFORM_CONFIRMATION)
  public org.springframework.http.ResponseEntity<RestApiResponse<BulkExecution>> confirmBulkUpdate(@RequestBody BulkConfirmationRequest request, @RequestHeader("Idempotency-Key") String key) {
    return super.confirmBulkUpdate(request, key);
  }

  @Override
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
  @BulkResourceOperation(BulkResourceOperation.Role.ITEM_CONFIRMATION)
  public org.springframework.http.ResponseEntity<RestApiResponse<BulkExecution>> confirmBulkUpdateItems(@RequestBody BulkConfirmationRequest request, @RequestHeader("Idempotency-Key") String key) {
    return super.confirmBulkUpdateItems(request, key);
  }











  @PostMapping("/bulk/ativar-campo-customizado-lote/evaluation")
  @Operation(
      operationId = "ergon.campos-customizados.ativar-campo-customizado-lote.evaluation",
      summary = "Avaliar ativação coletiva de campos customizados",
      description =
          "Avalia ativação coletiva de definições de campos customizados inativas do mesmo órgão, sem persistir alterações.")
  public org.springframework.http.ResponseEntity<RestApiResponse<BulkProposal>> bulkAtivarEvaluate(

          @RequestBody
          BulkCommandEvaluationRequest<
                  AtivarCampoCustomizadoLoteDTO, ?, DefinicaoCampoCustomizadoFilterDTO>
              request) {
    return evaluateBulkDomainCommand("ativar-campo-customizado-lote", request);
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
  public org.springframework.http.ResponseEntity<RestApiResponse<BulkExecution>> bulkAtivarConfirm(
      @RequestBody BulkConfirmationRequest request,
      @Parameter(description = "Chave estável do ato coletivo para impedir efeitos duplicados.")
          @RequestHeader("Idempotency-Key")
          String key) {
    return confirmBulkDomainCommand("ativar-campo-customizado-lote", request, key);
  }
  @Override @SuppressWarnings({"unchecked", "rawtypes"})
  protected org.praxisplatform.uischema.bulk.JpaResourceBulkEngine getBulkEngine() { return bulk.engine(Resource.CAMPOS_CUSTOMIZADOS); }
  @Override protected String bulkProposalOperationId(UUID id) { return bulk.locate(Resource.CAMPOS_CUSTOMIZADOS, id, false); }
  @Override protected String bulkExecutionOperationId(UUID id) { return bulk.locate(Resource.CAMPOS_CUSTOMIZADOS, id, true); }

}
