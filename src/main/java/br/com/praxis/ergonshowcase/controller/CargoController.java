package br.com.praxis.ergonshowcase.controller;

import br.com.praxis.ergonshowcase.bulk.*;
import br.com.praxis.ergonshowcase.bulk.ErgonBulkDomain.Resource;
import org.praxisplatform.uischema.bulk.*;
import org.praxisplatform.uischema.action.*;
import org.praxisplatform.uischema.dto.CursorPage;
import org.praxisplatform.uischema.rest.response.RestApiResponse;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.UUID;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.domain.*;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.service.*;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.controller.base.AbstractDuplicateDraftResourceController;

/** Publica o contrato de cargos através da hierarquia canônica Praxis. */
@org.praxisplatform.uischema.fact.ResourceFact(
    key = "cargoExtinto",
    resourceKey = "administracao-pessoal.cargos",
    label = "Cargo extinto",
    description = "Indica existência de extinção registrada para o cargo.",
    dataType = "boolean",
    icon = "block",
    tone = "danger",
    valuePresentation = "badge",
    sourceKind = "computed")
@org.praxisplatform.uischema.fact.ResourceFact(
    key = "saldoVagasDisponiveis",
    unit = "positions",
    resourceKey = "administracao-pessoal.cargos",
    label =
        "Saldo de vagas",
    description =
        "Vagas autorizadas menos vínculos ativos, afastados ou cedidos; nulo quando não há quantitativo autorizado.",
    dataType = "integer",
    icon = "groups",
    tone = "info",
    valuePresentation = "metric",
    thresholds = {
      @Threshold(max = 0, tone = "danger", label = "Quadro lotado ou excedente"),
      @Threshold(min = 1, max = 5, tone = "warning", label = "Saldo reduzido"),
      @Threshold(min = 6, max = 99999, tone = "success", label = "Vagas disponíveis")
    },
    sourceKind = "computed")
@BulkResourceOperations(
    proposalOperationId="ergon.cargos.bulk.proposal",
    proposalResultsOperationId="ergon.cargos.bulk.proposal-results",
    executionOperationId="ergon.cargos.bulk.execution",
    executionResultsOperationId="ergon.cargos.bulk.results",
    cancelOperationId="ergon.cargos.bulk.cancel",
    updateSourceOperationId="updateErgonCargo", protectedUpdateFields={})
@ApiResource(
    quickFilters = {
      @QuickFilter(
          id = "cargos-provimento",
          label = "Cargos de provimento",
          filter = "{\"cargoFuncao\":\"C\"}"),
      @QuickFilter(
          id = "funcoes-confianca",
          label = "Funções de confiança",
          filter = "{\"cargoFuncao\":\"F\"}"),
      @QuickFilter(
          id = "com-controle-vagas",
          label = "Com controle de vagas",
          filter = "{\"controleVaga\":true}"),
      @QuickFilter(id = "vigentes", label = "Cargos vigentes", filter = "{\"apenasVigentes\":true}")
    },
    value = ApiPaths.CARGOS,
    resourceKey = "administracao-pessoal.cargos",
    title = "Cargos",
    description = "Cargos e funções integrantes dos quadros de pessoal dos órgãos públicos.",
    icon = "badge",
    identity =
        @ResourceIdentity(
            keyField = "id",
            titleField = "nome",
            displayLabelField = "nome",
            subtitleField = "categoria",
            metadataFields = {"id", "orgaoNome", "tipoCargo", "saldoVagasDisponiveis"}))
@ApiGroup(
    value = "ergon",
    title = "Administração de pessoal",
    description = "Cadastros civis e assentamentos funcionais dos órgãos públicos.",
    icon = "groups")
public class CargoController
    extends AbstractDuplicateDraftResourceController<
        CargoResponseDTO, Integer, CargoFilterDTO, CreateCargoDTO, UpdateCargoDTO, CargoDuplicateDraftDTO> {
  private final CargoService service;
  @Override
  @PostMapping("/stats/group-by")
  @Operation(summary = "Cargos por categoria",
      description = "Agrega o conjunto autorizado e filtrado. A projeção nominal abre a ficha pela identidade real do bucket; agrupamentos de categoria não abrem registros individuais.")
  @UiAnalytics(projections = {
      @AnalyticsProjection(id = "cargos-composicao", intent = AnalyticsIntent.COMPOSITION,
          sourceOperation = AnalyticsOperation.GROUP_BY,
          primaryDimension = @AnalyticsDimensionBinding(field = "categoria"),
          primaryMetrics = @AnalyticsMetricBinding(field = "count", aggregation = "count", label = "Registros"),
          defaultLimit = 20),
      @AnalyticsProjection(id = "vagas-por-cargo", intent = AnalyticsIntent.RANKING,
          sourceOperation = AnalyticsOperation.GROUP_BY,
          primaryDimension = @AnalyticsDimensionBinding(field = "id"),
          primaryMetrics = @AnalyticsMetricBinding(field = "vagasAutorizadas", aggregation = "sum", label = "Vagas autorizadas"),
          defaultLimit = 20,
          recordOpen = @AnalyticsRecordOpen(sourceIdentityField = "key",
              target = @AnalyticsSurfaceTarget(resourceKey = "administracao-pessoal.cargos", surfaceId = "ficha-cargo")))
  })
  public org.springframework.http.ResponseEntity<RestApiResponse<org.praxisplatform.uischema.stats.dto.GroupByStatsResponse>> groupByStats(
      @RequestBody org.praxisplatform.uischema.stats.dto.GroupByStatsRequest<CargoFilterDTO> request) {
    return super.groupByStats(request);
  }

  public CargoController(CargoService service) {
    this.service = service;
  }

  @Override
  protected CargoService getService() {
    return service;
  }

  @Override
  @PostMapping("/{id}/duplicate-draft")
  @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ERGON_READ') and hasAuthority('ERGON_WRITE')")
  @Operation(operationId = "prepareErgonCargoDraft", summary = "Preparar novo cargo",
      description = "Prepara um novo cadastro a partir da classificação de um cargo vigente do órgão corrente, sem gravar. "
          + "Não copia identidade, órgão, autorização de vagas, documento legal, datas, agregação ou atributos adicionais. "
          + "Complete e revise o rascunho no POST de criação, que revalida todas as invariantes; descartar não altera a origem.")
  @WorkflowAction(id = "duplicate-draft", title = "Preparar novo cargo",
      description = "Preenche nome e enquadramento a partir de um cargo acessível, sem persistência. "
          + "A identidade e o órgão devem ser informados no novo cadastro.",
      requiredAuthorities = {"ERGON_READ", "ERGON_WRITE"},
      allowedStates = {"VIGENTE"},
      tags = {"duplicate-draft", "draft", "business-command"}, order = 90)
  public org.springframework.http.ResponseEntity<RestApiResponse<CargoDuplicateDraftDTO>> duplicateDraft(
      @Parameter(description = "Código do cargo de referência classificatória no órgão corrente.")
      @PathVariable Integer id) {
    return super.duplicateDraft(id);
  }

  @Override
  protected Integer getResponseId(CargoResponseDTO dto) {
    return dto.getId();
  }

  @org.springframework.beans.factory.annotation.Autowired private ErgonCommands commands;

  @org.springframework.beans.factory.annotation.Autowired
  private org.praxisplatform.uischema.concurrency.ResourceVersionEtagService commandEtags;

  @org.springframework.beans.factory.annotation.Autowired
  private org.praxisplatform.uischema.concurrency.ResourceVersionScopeProvider commandScopes;

  @org.springframework.web.bind.annotation.PostMapping("/{id}/actions/extinguir-cargo")
  @WorkflowAction(
      id = "extinguir-cargo",
      title = "Extinguir cargo",
      description =
          "Registra extinção do cargo quando inexistem ocupantes ativos, afastados ou cedidos.",
      scope = org.praxisplatform.uischema.action.ActionScope.ITEM,
      requiredAuthorities = {"ERGON_WRITE"},
      allowedStates = {"VIGENTE"},
      resourceVersion = org.praxisplatform.uischema.action.ActionRequirement.REQUIRED,
      resourceVersionTransport =
          org.praxisplatform.uischema.action.ActionResourceVersionTransport.IF_MATCH)
  @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ERGON_WRITE')")
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Extinguir cargo",
      description =
          "Registra extinção do cargo quando inexistem ocupantes ativos, afastados ou cedidos. Exige If-Match atual.")
  public org.springframework.http.ResponseEntity<CargoResponseDTO> extinguirCargo(
      @org.springframework.web.bind.annotation.PathVariable Integer id,
      @org.springframework.web.bind.annotation.RequestHeader(value = "If-Match", required = false)
          String ifMatch,
      @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody
          ExtinguirCargoDTO dto) {
    var p =
        new org.praxisplatform.uischema.concurrency.ResourceVersionUpdatePrecondition<Integer>(
            commandEtags,
            ifMatch,
            commandScopes.currentScope(),
            "administracao-pessoal.cargos",
            id);
    return withResourceVersion(
        org.springframework.http.ResponseEntity.ok(), id, commands.extinguirCargo(id, dto, p));
  }

  @org.springframework.web.bind.annotation.GetMapping("/{id}/ficha-cargo")
  @UiSurface(
      id = "ficha-cargo",
      kind = org.praxisplatform.uischema.surface.SurfaceKind.READ_PROJECTION,
      scope = org.praxisplatform.uischema.surface.SurfaceScope.ITEM,
      title = "Ficha do cargo",
      description = "Consulta enquadramento, controle de vagas e extinção do cargo.",
      requiredAuthorities = {"ERGON_READ"})
  @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ERGON_READ')")
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Consultar ficha do cargo",
      description = "Consulta enquadramento, controle de vagas e extinção do cargo.")
  public org.springframework.http.ResponseEntity<CargoResponseDTO> ficha(
      @io.swagger.v3.oas.annotations.Parameter(description = "Identidade imutável do cadastro")
          @org.springframework.web.bind.annotation.PathVariable
          Integer id) {
    return withResourceVersion(
        org.springframework.http.ResponseEntity.ok(), id, service.findById(id));
  }

  @org.springframework.beans.factory.annotation.Autowired private VinculoService relatedService;

  @org.springframework.web.bind.annotation.GetMapping("/{id}/vinculos-titulares")
  @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ERGON_READ')")
  @UiSurface(
      id = "vinculos-titulares",
      kind = org.praxisplatform.uischema.surface.SurfaceKind.READ_PROJECTION,
      scope = org.praxisplatform.uischema.surface.SurfaceScope.ITEM,
      title = "Vínculos titulares",
      description = "Assentamentos cujo cargo de provimento é o cargo consultado.",
      relatedChildResourceKey = "administracao-pessoal.vinculos",
      relatedChildResourcePath = ApiPaths.VINCULOS,
      relatedChildParentField = "cargo",
      relatedChildOperations = {
        org.praxisplatform.uischema.surface.RelatedResourceChildOperation.FILTER,
        org.praxisplatform.uischema.surface.RelatedResourceChildOperation.LIST
      },
      requiredAuthorities = {"ERGON_READ"})
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Consultar vínculos titulares",
      description =
          "Assentamentos cujo cargo de provimento é o cargo consultado. A paginação é limitada a 200 registros por resposta.")
  public org.springframework.http.ResponseEntity<
          org.praxisplatform.uischema.rest.response.RestApiResponse<
              org.springframework.data.domain.Page<VinculoResponseDTO>>>
      vinculos_titulares(
          @io.swagger.v3.oas.annotations.Parameter(description = "Identidade do cadastro pai")
              @org.springframework.web.bind.annotation.PathVariable
              Integer id,
          @io.swagger.v3.oas.annotations.Parameter(
                  description = "Página a consultar, iniciando em zero")
              @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0")
              int page,
          @io.swagger.v3.oas.annotations.Parameter(
                  description = "Quantidade de registros da página, de 1 a 200")
              @org.springframework.web.bind.annotation.RequestParam(defaultValue = "20")
              int size) {
    service.findById(id);
    if (page < 0 || size < 1 || size > 200)
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY,
          "Paginação fora do limite permitido.");
    var filter = new VinculoFilterDTO();
    filter.setCargo(id);
    var result =
        relatedService.filter(
            filter,
            org.springframework.data.domain.PageRequest.of(
                page, size, relatedService.getDefaultSort()),
            null);
    return org.springframework.http.ResponseEntity.ok(
        org.praxisplatform.uischema.rest.response.RestApiResponse.success(
            result,
            org.springframework.hateoas.Links.of(
                org.springframework.hateoas.Link.of(ApiPaths.VINCULOS, "collection"))));
  }

  @org.springframework.web.bind.annotation.GetMapping("/{id}/vinculos-comissao")
  @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ERGON_READ')")
  @UiSurface(
      id = "vinculos-comissao",
      kind = org.praxisplatform.uischema.surface.SurfaceKind.READ_PROJECTION,
      scope = org.praxisplatform.uischema.surface.SurfaceScope.ITEM,
      title = "Vínculos em comissão",
      description = "Assentamentos que exercem este cargo em comissão.",
      relatedChildResourceKey = "administracao-pessoal.vinculos",
      relatedChildResourcePath = ApiPaths.VINCULOS,
      relatedChildParentField = "cargoComissao",
      relatedChildOperations = {
        org.praxisplatform.uischema.surface.RelatedResourceChildOperation.FILTER,
        org.praxisplatform.uischema.surface.RelatedResourceChildOperation.LIST
      },
      requiredAuthorities = {"ERGON_READ"})
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Consultar vínculos em comissão",
      description =
          "Assentamentos que exercem este cargo em comissão. A paginação é limitada a 200 registros por resposta.")
  public org.springframework.http.ResponseEntity<
          org.praxisplatform.uischema.rest.response.RestApiResponse<
              org.springframework.data.domain.Page<VinculoResponseDTO>>>
      vinculos_comissao(
          @io.swagger.v3.oas.annotations.Parameter(description = "Identidade do cadastro pai")
              @org.springframework.web.bind.annotation.PathVariable
              Integer id,
          @io.swagger.v3.oas.annotations.Parameter(
                  description = "Página a consultar, iniciando em zero")
              @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0")
              int page,
          @io.swagger.v3.oas.annotations.Parameter(
                  description = "Quantidade de registros da página, de 1 a 200")
              @org.springframework.web.bind.annotation.RequestParam(defaultValue = "20")
              int size) {
    service.findById(id);
    if (page < 0 || size < 1 || size > 200)
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY,
          "Paginação fora do limite permitido.");
    var filter = new VinculoFilterDTO();
    filter.setCargoComissao(id);
    var result =
        relatedService.filter(
            filter,
            org.springframework.data.domain.PageRequest.of(
                page, size, relatedService.getDefaultSort()),
            null);
    return org.springframework.http.ResponseEntity.ok(
        org.praxisplatform.uischema.rest.response.RestApiResponse.success(
            result,
            org.springframework.hateoas.Links.of(
                org.springframework.hateoas.Link.of(ApiPaths.VINCULOS, "collection"))));
  }

  @Override
  @org.springframework.web.bind.annotation.PostMapping
  @io.swagger.v3.oas.annotations.Operation(
      operationId = "createErgonCargo",
      summary = "Cadastrar cargo",
      description =
          "Inclui o cadastro validando identidade, órgão e invariantes de domínio; prévias do formulário não são autoridade de persistência.")
  public org.springframework.http.ResponseEntity<
          org.praxisplatform.uischema.rest.response.RestApiResponse<CargoResponseDTO>>
      create(
          @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody
              CreateCargoDTO dto) {
    return super.create(dto);
  }

  @Override
  @org.springframework.web.bind.annotation.PutMapping("/{id}")
  @BulkResourceOperation(BulkResourceOperation.Role.UPDATE_SOURCE)
  @io.swagger.v3.oas.annotations.Operation(
      operationId = "updateErgonCargo",
      summary = "Manter cargo",
      description =
          "Mantém os dados cadastrais com If-Match atual, preservando a identidade e as transições controladas por ações.")
  public org.springframework.http.ResponseEntity<
          org.praxisplatform.uischema.rest.response.RestApiResponse<CargoResponseDTO>>
      update(
          @io.swagger.v3.oas.annotations.Parameter(description = "Identidade imutável do cadastro")
              @org.springframework.web.bind.annotation.PathVariable
              Integer id,
          @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody
              UpdateCargoDTO dto) {
    return super.update(id, dto);
  }

  @org.springframework.beans.factory.annotation.Autowired
  private ErgonDeterminationService determinations;

  @org.springframework.web.bind.annotation.PostMapping("/determinations/aglutinador")
  @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ERGON_READ')")
  @io.swagger.v3.oas.annotations.Operation(
      operationId = "determineErgonAglutinador",
      summary = "Consultar cargo agregador",
      description =
          "Identifica o cargo agregador habilitado do mesmo órgão, sem alterar o quadro de pessoal.")
  public org.springframework.http.ResponseEntity<AglutinadorPreviewDTO> determine(
      @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody
          CargoSelectionDTO draft) {
    return org.springframework.http.ResponseEntity.ok(determinations.aglutinador(draft));
  }

  @org.springframework.beans.factory.annotation.Autowired private ErgonBulkOperations bulk;

  @PostMapping("/bulk/update/evaluation")
  @Operation(operationId="ergon.cargos.bulk.uniform.evaluation", summary="Avaliar edição uniforme",description="Avalia seleção, versões e alterações sem modificar o cadastro; produz uma proposta protegida.")
  public RestApiResponse<BulkProposal> bulkEvaluate(@io.swagger.v3.oas.annotations.parameters.RequestBody(content=@io.swagger.v3.oas.annotations.media.Content(schema=@io.swagger.v3.oas.annotations.media.Schema(implementation=ErgonBulkSchema.CargoUniform.class))) @RequestBody BulkUniformEvaluationRequest<Integer,CargoFilterDTO> request) {
    return RestApiResponse.success(bulk.uniform(Resource.CARGOS,request),null);
  }
  @PostMapping("/bulk/update-items/evaluation")
  @Operation(operationId="ergon.cargos.bulk.items.evaluation",summary="Avaliar edições por registro",description="Avalia deltas distintos e a versão esperada de cada registro, com impedimentos individualizados.")
  public RestApiResponse<BulkProposal> bulkEvaluateItems(@io.swagger.v3.oas.annotations.parameters.RequestBody(content=@io.swagger.v3.oas.annotations.media.Content(schema=@io.swagger.v3.oas.annotations.media.Schema(implementation=ErgonBulkSchema.CargoItems.class))) @RequestBody BulkItemEvaluationRequest<Integer> request) {
    return RestApiResponse.success(bulk.items(Resource.CARGOS,request),null);
  }
  @PostMapping("/bulk/update")
  @BulkOperation(mode=BulkMode.UNIFORM_UPDATE,evaluationOperationId="ergon.cargos.bulk.uniform.evaluation",atomicity=ActionCollectionAtomicity.PER_ITEM)
  @Operation(operationId="ergon.cargos.bulk.uniform",summary="Confirmar edição uniforme",description="Confirma uma proposta avaliada; cada registro é revalidado e possui recibo durável, sem repetir efeitos no replay.")
  public RestApiResponse<BulkExecution> bulkConfirm(@RequestBody BulkConfirmationRequest request,
      @Parameter(description="Chave estável desta confirmação; repetir a mesma chave recupera a execução.") @RequestHeader("Idempotency-Key") String key) {
    return RestApiResponse.success(bulk.confirm(Resource.CARGOS,"ergon.cargos.bulk.uniform",request,key),null);
  }
  @PostMapping("/bulk/update-items")
  @BulkOperation(mode=BulkMode.PER_ITEM_UPDATE,evaluationOperationId="ergon.cargos.bulk.items.evaluation",atomicity=ActionCollectionAtomicity.PER_ITEM)
  @Operation(operationId="ergon.cargos.bulk.items",summary="Confirmar edições por registro",description="Aplica os deltas individualizados preservados na proposta, com versão e resultado próprios por registro.")
  public RestApiResponse<BulkExecution> bulkConfirmItems(@RequestBody BulkConfirmationRequest request,
      @Parameter(description="Chave estável de confirmação para recuperar a mesma execução em caso de repetição.") @RequestHeader("Idempotency-Key") String key) {
    return RestApiResponse.success(bulk.confirm(Resource.CARGOS,"ergon.cargos.bulk.items",request,key),null);
  }
  @GetMapping("/bulk/proposals/{proposalId}")
  @BulkResourceOperation(BulkResourceOperation.Role.PROPOSAL)
  @Operation(operationId="ergon.cargos.bulk.proposal",summary="Consultar proposta de lote",description="Consulta totais e intenção pública da proposta após conferir acesso ao conjunto completo de registros.")
  public RestApiResponse<BulkProposal> bulkProposal(@Parameter(description="Identidade da proposta avaliada.") @PathVariable UUID proposalId) {
    return RestApiResponse.success(bulk.proposal(Resource.CARGOS,bulk.locate(Resource.CARGOS,proposalId,false),proposalId),null);
  }
  @GetMapping("/bulk/proposals/{proposalId}/results")
  @BulkResourceOperation(BulkResourceOperation.Role.PROPOSAL_RESULTS)
  @Operation(operationId="ergon.cargos.bulk.proposal-results",summary="Inspecionar avaliação do lote",description="Pagina registros executáveis e bloqueados, com diagnósticos seguros e cursor vinculado ao solicitante.")
  public RestApiResponse<CursorPage<BulkProposalItemResult<Object>>> bulkProposalResults(
      @Parameter(description="Identidade da proposta avaliada.") @PathVariable UUID proposalId,
      @Parameter(description="Quantidade de registros por página, de 1 a 200.") @RequestParam(defaultValue="50") int size,
      @Parameter(description="Cursor opaco emitido na página anterior; preserve o tamanho da página.") @RequestParam(required=false) String after) {
    return RestApiResponse.success(bulk.proposalResults(Resource.CARGOS,bulk.locate(Resource.CARGOS,proposalId,false),proposalId,size,after),null);
  }
  @GetMapping("/bulk/executions/{executionId}")
  @BulkResourceOperation(BulkResourceOperation.Role.EXECUTION)
  @Operation(operationId="ergon.cargos.bulk.execution",summary="Consultar execução do lote",description="Retorna estado e contadores reconciliados a partir dos recibos persistidos.")
  public RestApiResponse<BulkExecution> bulkExecution(@Parameter(description="Identidade da execução confirmada.") @PathVariable UUID executionId) {
    return RestApiResponse.success(bulk.execution(Resource.CARGOS,bulk.locate(Resource.CARGOS,executionId,true),executionId),null);
  }
  @GetMapping("/bulk/executions/{executionId}/results")
  @BulkResourceOperation(BulkResourceOperation.Role.EXECUTION_RESULTS)
  @Operation(operationId="ergon.cargos.bulk.results",summary="Consultar recibos do lote",description="Pagina resultados por registro sem expor fatos protegidos, versões ou dados bancários da proposta.")
  public RestApiResponse<CursorPage<BulkItemResult<Object>>> bulkResults(
      @Parameter(description="Identidade da execução confirmada.") @PathVariable UUID executionId,
      @Parameter(description="Quantidade de recibos por página, de 1 a 200.") @RequestParam(defaultValue="50") int size,
      @Parameter(description="Cursor opaco da página anterior de recibos.") @RequestParam(required=false) String after) {
    return RestApiResponse.success(bulk.results(Resource.CARGOS,bulk.locate(Resource.CARGOS,executionId,true),executionId,size,after),null);
  }
  @PostMapping("/bulk/executions/{executionId}/cancel")
  @BulkResourceOperation(BulkResourceOperation.Role.CANCEL)
  @Operation(operationId="ergon.cargos.bulk.cancel",summary="Solicitar parada do lote",description="Registra a solicitação de parada sem desfazer alterações já confirmadas e sem reabrir efeitos no replay.")
  public RestApiResponse<BulkExecution> bulkCancel(@Parameter(description="Identidade da execução a interromper.") @PathVariable UUID executionId) {
    return RestApiResponse.success(bulk.cancel(Resource.CARGOS,bulk.locate(Resource.CARGOS,executionId,true),executionId),null);
  }
}
