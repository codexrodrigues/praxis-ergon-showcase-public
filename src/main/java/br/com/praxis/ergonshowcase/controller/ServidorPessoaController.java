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
import org.praxisplatform.uischema.controller.base.AbstractResourceController;

/** Publica o contrato de pessoas servidoras através da hierarquia canônica Praxis. */
@org.praxisplatform.uischema.fact.ResourceFact(
    key = "idadeAnos",
    unit = "years",
    resourceKey = "administracao-pessoal.funcionarios",
    label = "Idade em anos",
    description = "Idade civil em anos completos na data atual.",
    dataType = "integer",
    sourceKind = "computed")
@org.praxisplatform.uischema.fact.ResourceFact(
    key = "possuiDeficienciaPcD",
    resourceKey = "administracao-pessoal.funcionarios",
    label = "Pessoa com deficiência",
    description = "Indica ao menos uma das cinco deficiências específicas registrada na pessoa.",
    dataType = "boolean",
    sourceKind = "computed")
@BulkResourceOperations(
    proposalOperationId="ergon.funcionarios.bulk.proposal",
    proposalResultsOperationId="ergon.funcionarios.bulk.proposal-results",
    executionOperationId="ergon.funcionarios.bulk.execution",
    executionResultsOperationId="ergon.funcionarios.bulk.results",
    cancelOperationId="ergon.funcionarios.bulk.cancel",
    updateSourceOperationId="updateErgonServidorPessoa", protectedUpdateFields={"cpf","nome","dtnasc"})
@ApiResource(
    quickFilters = {
      @QuickFilter(id = "pcd", label = "Pessoas com deficiência", filter = "{\"somentePcD\":true}"),
      @QuickFilter(
          id = "com-nome-social",
          label = "Com nome social",
          filter = "{\"possuiNomeSocial\":true}")
    },
    value = ApiPaths.PESSOAS,
    resourceKey = "administracao-pessoal.funcionarios",
    title = "Pessoas servidoras",
    description =
        "Cadastro civil único das pessoas titulares de vínculos funcionais com órgãos públicos.",
    icon = "badge",
    identity =
        @ResourceIdentity(
            keyField = "id",
            titleField = "nome",
            displayLabelField = "nome",
            subtitleField = "nomeSocial",
            metadataFields = {"id", "escolaridade", "cidadeender", "ufender"}))
@ApiGroup(
    value = "ergon",
    title = "Administração de pessoal",
    description = "Cadastros civis e assentamentos funcionais dos órgãos públicos.",
    icon = "groups")
public class ServidorPessoaController
    extends AbstractResourceController<
        ServidorPessoaResponseDTO,
        Long,
        ServidorPessoaFilterDTO,
        CreateServidorPessoaDTO,
        UpdateServidorPessoaDTO> {
  private final ServidorPessoaService service;
  @Override
  @PostMapping("/stats/group-by")
  @Operation(summary = "Pessoas por escolaridade",
      description = "Agrega o conjunto autorizado e filtrado. A projeção nominal abre a ficha pela identidade real do bucket; agrupamentos de categoria não abrem registros individuais.")
  @UiAnalytics(projections = {
      @AnalyticsProjection(id = "funcionarios-composicao", intent = AnalyticsIntent.COMPOSITION,
          sourceOperation = AnalyticsOperation.GROUP_BY,
          primaryDimension = @AnalyticsDimensionBinding(field = "escolaridade"),
          primaryMetrics = @AnalyticsMetricBinding(field = "count", aggregation = "count", label = "Registros"),
          defaultLimit = 20),
      @AnalyticsProjection(id = "cadastros-por-pessoa", intent = AnalyticsIntent.RANKING,
          sourceOperation = AnalyticsOperation.GROUP_BY,
          primaryDimension = @AnalyticsDimensionBinding(field = "id"),
          primaryMetrics = @AnalyticsMetricBinding(field = "count", aggregation = "count", label = "Cadastros civis"),
          defaultLimit = 20,
          recordOpen = @AnalyticsRecordOpen(sourceIdentityField = "key",
              target = @AnalyticsSurfaceTarget(resourceKey = "administracao-pessoal.funcionarios", surfaceId = "ficha-cadastral")))
  })
  public org.springframework.http.ResponseEntity<RestApiResponse<org.praxisplatform.uischema.stats.dto.GroupByStatsResponse>> groupByStats(
      @RequestBody org.praxisplatform.uischema.stats.dto.GroupByStatsRequest<ServidorPessoaFilterDTO> request) {
    return super.groupByStats(request);
  }

  public ServidorPessoaController(ServidorPessoaService service) {
    this.service = service;
  }

  @Override
  protected ServidorPessoaService getService() {
    return service;
  }

  @Override
  protected Long getResponseId(ServidorPessoaResponseDTO dto) {
    return dto.getId();
  }

  @org.springframework.beans.factory.annotation.Autowired private ErgonCommands commands;

  @org.springframework.beans.factory.annotation.Autowired
  private org.praxisplatform.uischema.concurrency.ResourceVersionEtagService commandEtags;

  @org.springframework.beans.factory.annotation.Autowired
  private org.praxisplatform.uischema.concurrency.ResourceVersionScopeProvider commandScopes;

  @org.springframework.web.bind.annotation.PatchMapping("/{id}/dados-bancarios-contato")
  @ResourceIntent(
      id = "atualizar-dados-bancarios-contato",
      title = "Atualizar dados bancários e contato",
      description = "Mantém endereço, canais de contato e conta bancária padrão da pessoa.")
  @UiSurface(
      id = "dados-bancarios-contato",
      kind = org.praxisplatform.uischema.surface.SurfaceKind.PARTIAL_FORM,
      scope = org.praxisplatform.uischema.surface.SurfaceScope.ITEM,
      title = "Atualizar dados bancários e contato",
      description = "Mantém endereço, canais de contato e conta bancária padrão da pessoa.",
      requiredAuthorities = {"ERGON_WRITE"})
  @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ERGON_WRITE')")
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Atualizar dados bancários e contato",
      description =
          "Mantém endereço, canais de contato e conta bancária padrão da pessoa. Exige If-Match atual.")
  public org.springframework.http.ResponseEntity<ServidorPessoaResponseDTO>
      atualizarDadosBancariosContato(
          @org.springframework.web.bind.annotation.PathVariable Long id,
          @org.springframework.web.bind.annotation.RequestHeader(
                  value = "If-Match",
                  required = false)
              String ifMatch,
          @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody
              AtualizarDadosBancariosContatoDTO dto) {
    var p =
        new org.praxisplatform.uischema.concurrency.ResourceVersionUpdatePrecondition<Long>(
            commandEtags,
            ifMatch,
            commandScopes.currentScope(),
            "administracao-pessoal.funcionarios",
            id);
    return withResourceVersion(
        org.springframework.http.ResponseEntity.ok(),
        id,
        commands.atualizarDadosBancariosContato(id, dto, p));
  }

  @org.springframework.web.bind.annotation.GetMapping("/{id}/ficha-cadastral")
  @UiSurface(
      id = "ficha-cadastral",
      kind = org.praxisplatform.uischema.surface.SurfaceKind.READ_PROJECTION,
      scope = org.praxisplatform.uischema.surface.SurfaceScope.ITEM,
      title = "Ficha cadastral",
      description =
          "Consulta dados cadastrais da pessoa conforme as políticas de acesso e privacidade.",
      requiredAuthorities = {"ERGON_READ"})
  @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ERGON_READ')")
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Consultar ficha cadastral",
      description =
          "Consulta dados cadastrais da pessoa conforme as políticas de acesso e privacidade.")
  public org.springframework.http.ResponseEntity<ServidorPessoaResponseDTO> ficha(
      @io.swagger.v3.oas.annotations.Parameter(description = "Identidade imutável do cadastro")
          @org.springframework.web.bind.annotation.PathVariable
          Long id) {
    return withResourceVersion(
        org.springframework.http.ResponseEntity.ok(), id, service.findById(id));
  }

  @org.springframework.beans.factory.annotation.Autowired private VinculoService relatedService;

  @org.springframework.web.bind.annotation.GetMapping("/{id}/vinculos-funcionais")
  @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ERGON_READ')")
  @UiSurface(
      id = "vinculos-funcionais",
      kind = org.praxisplatform.uischema.surface.SurfaceKind.READ_PROJECTION,
      scope = org.praxisplatform.uischema.surface.SurfaceScope.ITEM,
      title = "Vínculos funcionais",
      description = "Assentamentos funcionais da pessoa, incluindo vínculos históricos.",
      relatedChildResourceKey = "administracao-pessoal.vinculos",
      relatedChildResourcePath = "/api/ergon/vinculos",
      relatedChildParentField = "numfunc",
      relatedChildOperations = {
        org.praxisplatform.uischema.surface.RelatedResourceChildOperation.FILTER,
        org.praxisplatform.uischema.surface.RelatedResourceChildOperation.LIST
      },
      requiredAuthorities = {"ERGON_READ"})
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Consultar vínculos funcionais",
      description =
          "Assentamentos funcionais da pessoa, incluindo vínculos históricos. A paginação é limitada a 200 registros por resposta.")
  public org.springframework.http.ResponseEntity<
          org.praxisplatform.uischema.rest.response.RestApiResponse<
              org.springframework.data.domain.Page<VinculoResponseDTO>>>
      vinculos_funcionais(
          @io.swagger.v3.oas.annotations.Parameter(description = "Identidade do cadastro pai")
              @org.springframework.web.bind.annotation.PathVariable
              Long id,
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
    filter.setNumfunc(id);
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
                org.springframework.hateoas.Link.of("/api/ergon/vinculos", "collection"))));
  }

  @Override
  @org.springframework.web.bind.annotation.PostMapping
  @io.swagger.v3.oas.annotations.Operation(
      operationId = "createErgonServidorPessoa",
      summary = "Cadastrar pessoa servidora",
      description =
          "Inclui o cadastro validando identidade, órgão e invariantes de domínio; prévias do formulário não são autoridade de persistência.")
  public org.springframework.http.ResponseEntity<
          org.praxisplatform.uischema.rest.response.RestApiResponse<ServidorPessoaResponseDTO>>
      create(
          @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody
              CreateServidorPessoaDTO dto) {
    return super.create(dto);
  }

  @Override
  @org.springframework.web.bind.annotation.PutMapping("/{id}")
  @BulkResourceOperation(BulkResourceOperation.Role.UPDATE_SOURCE)
  @io.swagger.v3.oas.annotations.Operation(
      operationId = "updateErgonServidorPessoa",
      summary = "Manter pessoa servidora",
      description =
          "Mantém os dados cadastrais com If-Match atual, preservando a identidade e as transições controladas por ações.")
  public org.springframework.http.ResponseEntity<
          org.praxisplatform.uischema.rest.response.RestApiResponse<ServidorPessoaResponseDTO>>
      update(
          @io.swagger.v3.oas.annotations.Parameter(description = "Identidade imutável do cadastro")
              @org.springframework.web.bind.annotation.PathVariable
              Long id,
          @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody
              UpdateServidorPessoaDTO dto) {
    return super.update(id, dto);
  }

  @org.springframework.beans.factory.annotation.Autowired
  private ErgonDeterminationService determinations;

  @org.springframework.web.bind.annotation.PostMapping("/determinations/pcd")
  @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ERGON_READ')")
  @io.swagger.v3.oas.annotations.Operation(
      operationId = "determineErgonPcd",
      summary = "Consultar prévia de PcD",
      description =
          "Calcula a disjunção dos indicadores do draft, sem persistir ou alterar dados pessoais.")
  public org.springframework.http.ResponseEntity<PcdPreviewDTO> determine(
      @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody
          PcdDraftDTO draft) {
    return org.springframework.http.ResponseEntity.ok(determinations.pcd(draft));
  }

  @org.springframework.beans.factory.annotation.Autowired private ErgonBulkOperations bulk;

  @PostMapping("/bulk/update/evaluation")
  @Operation(operationId="ergon.funcionarios.bulk.uniform.evaluation", summary="Avaliar edição uniforme",description="Avalia seleção, versões e alterações sem modificar o cadastro; produz uma proposta protegida.")
  public RestApiResponse<BulkProposal> bulkEvaluate(@io.swagger.v3.oas.annotations.parameters.RequestBody(content=@io.swagger.v3.oas.annotations.media.Content(schema=@io.swagger.v3.oas.annotations.media.Schema(implementation=ErgonBulkSchema.PessoaUniform.class))) @RequestBody BulkUniformEvaluationRequest<String,ServidorPessoaFilterDTO> request) {
    return RestApiResponse.success(bulk.uniform(Resource.PESSOAS,request),null);
  }
  @PostMapping("/bulk/update-items/evaluation")
  @Operation(operationId="ergon.funcionarios.bulk.items.evaluation",summary="Avaliar edições por registro",description="Avalia deltas distintos e a versão esperada de cada registro, com impedimentos individualizados.")
  public RestApiResponse<BulkProposal> bulkEvaluateItems(@io.swagger.v3.oas.annotations.parameters.RequestBody(content=@io.swagger.v3.oas.annotations.media.Content(schema=@io.swagger.v3.oas.annotations.media.Schema(implementation=ErgonBulkSchema.PessoaItems.class))) @RequestBody BulkItemEvaluationRequest<String> request) {
    return RestApiResponse.success(bulk.items(Resource.PESSOAS,request),null);
  }
  @PostMapping("/bulk/update")
  @BulkOperation(mode=BulkMode.UNIFORM_UPDATE,evaluationOperationId="ergon.funcionarios.bulk.uniform.evaluation",atomicity=ActionCollectionAtomicity.PER_ITEM)
  @Operation(operationId="ergon.funcionarios.bulk.uniform",summary="Confirmar edição uniforme",description="Confirma uma proposta avaliada; cada registro é revalidado e possui recibo durável, sem repetir efeitos no replay.")
  public RestApiResponse<BulkExecution> bulkConfirm(@RequestBody BulkConfirmationRequest request,
      @Parameter(description="Chave estável desta confirmação; repetir a mesma chave recupera a execução.") @RequestHeader("Idempotency-Key") String key) {
    return RestApiResponse.success(bulk.confirm(Resource.PESSOAS,"ergon.funcionarios.bulk.uniform",request,key),null);
  }
  @PostMapping("/bulk/update-items")
  @BulkOperation(mode=BulkMode.PER_ITEM_UPDATE,evaluationOperationId="ergon.funcionarios.bulk.items.evaluation",atomicity=ActionCollectionAtomicity.PER_ITEM)
  @Operation(operationId="ergon.funcionarios.bulk.items",summary="Confirmar edições por registro",description="Aplica os deltas individualizados preservados na proposta, com versão e resultado próprios por registro.")
  public RestApiResponse<BulkExecution> bulkConfirmItems(@RequestBody BulkConfirmationRequest request,
      @Parameter(description="Chave estável de confirmação para recuperar a mesma execução em caso de repetição.") @RequestHeader("Idempotency-Key") String key) {
    return RestApiResponse.success(bulk.confirm(Resource.PESSOAS,"ergon.funcionarios.bulk.items",request,key),null);
  }
  @GetMapping("/bulk/proposals/{proposalId}")
  @BulkResourceOperation(BulkResourceOperation.Role.PROPOSAL)
  @Operation(operationId="ergon.funcionarios.bulk.proposal",summary="Consultar proposta de lote",description="Consulta totais e intenção pública da proposta após conferir acesso ao conjunto completo de registros.")
  public RestApiResponse<BulkProposal> bulkProposal(@Parameter(description="Identidade da proposta avaliada.") @PathVariable UUID proposalId) {
    return RestApiResponse.success(bulk.proposal(Resource.PESSOAS,bulk.locate(Resource.PESSOAS,proposalId,false),proposalId),null);
  }
  @GetMapping("/bulk/proposals/{proposalId}/results")
  @BulkResourceOperation(BulkResourceOperation.Role.PROPOSAL_RESULTS)
  @Operation(operationId="ergon.funcionarios.bulk.proposal-results",summary="Inspecionar avaliação do lote",description="Pagina registros executáveis e bloqueados, com diagnósticos seguros e cursor vinculado ao solicitante.")
  public RestApiResponse<CursorPage<BulkProposalItemResult<Object>>> bulkProposalResults(
      @Parameter(description="Identidade da proposta avaliada.") @PathVariable UUID proposalId,
      @Parameter(description="Quantidade de registros por página, de 1 a 200.") @RequestParam(defaultValue="50") int size,
      @Parameter(description="Cursor opaco emitido na página anterior; preserve o tamanho da página.") @RequestParam(required=false) String after) {
    return RestApiResponse.success(bulk.proposalResults(Resource.PESSOAS,bulk.locate(Resource.PESSOAS,proposalId,false),proposalId,size,after),null);
  }
  @GetMapping("/bulk/executions/{executionId}")
  @BulkResourceOperation(BulkResourceOperation.Role.EXECUTION)
  @Operation(operationId="ergon.funcionarios.bulk.execution",summary="Consultar execução do lote",description="Retorna estado e contadores reconciliados a partir dos recibos persistidos.")
  public RestApiResponse<BulkExecution> bulkExecution(@Parameter(description="Identidade da execução confirmada.") @PathVariable UUID executionId) {
    return RestApiResponse.success(bulk.execution(Resource.PESSOAS,bulk.locate(Resource.PESSOAS,executionId,true),executionId),null);
  }
  @GetMapping("/bulk/executions/{executionId}/results")
  @BulkResourceOperation(BulkResourceOperation.Role.EXECUTION_RESULTS)
  @Operation(operationId="ergon.funcionarios.bulk.results",summary="Consultar recibos do lote",description="Pagina resultados por registro sem expor fatos protegidos, versões ou dados bancários da proposta.")
  public RestApiResponse<CursorPage<BulkItemResult<Object>>> bulkResults(
      @Parameter(description="Identidade da execução confirmada.") @PathVariable UUID executionId,
      @Parameter(description="Quantidade de recibos por página, de 1 a 200.") @RequestParam(defaultValue="50") int size,
      @Parameter(description="Cursor opaco da página anterior de recibos.") @RequestParam(required=false) String after) {
    return RestApiResponse.success(bulk.results(Resource.PESSOAS,bulk.locate(Resource.PESSOAS,executionId,true),executionId,size,after),null);
  }
  @PostMapping("/bulk/executions/{executionId}/cancel")
  @BulkResourceOperation(BulkResourceOperation.Role.CANCEL)
  @Operation(operationId="ergon.funcionarios.bulk.cancel",summary="Solicitar parada do lote",description="Registra a solicitação de parada sem desfazer alterações já confirmadas e sem reabrir efeitos no replay.")
  public RestApiResponse<BulkExecution> bulkCancel(@Parameter(description="Identidade da execução a interromper.") @PathVariable UUID executionId) {
    return RestApiResponse.success(bulk.cancel(Resource.PESSOAS,bulk.locate(Resource.PESSOAS,executionId,true),executionId),null);
  }
}
