package br.com.praxis.ergonshowcase.controller;

import br.com.praxis.ergonshowcase.bulk.*;
import br.com.praxis.ergonshowcase.bulk.ErgonBulkDomain.Resource;
import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.domain.DependenteId;
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

/** Publica o contrato de dependentes através da hierarquia canônica Praxis. */
@ResourceFact(
    key = "dependenteVigente",
    resourceKey = "administracao-pessoal.dependentes",
    label = "Dependência vigente",
    description = "Verdadeiro quando a vigência da dependência está ativa na data atual.",
    dataType = "boolean",
    icon = "verified",
    tone = "success",
    valuePresentation = "badge",
    sourceKind = "computed")
@ResourceFact(
    key = "idadeAnos",
    resourceKey = "administracao-pessoal.dependentes",
    label = "Idade atual (anos)",
    description = "Idade completa em anos calculada a partir da data de nascimento.",
    dataType = "integer",
    unit = "years",
    icon = "cake",
    tone = "info",
    valuePresentation = "metric",
    thresholds = {
      @Threshold(min = 0, max = 21, tone = "success", label = "Faixa etária padrão (até 21)"),
      @Threshold(min = 22, max = 24, tone = "warning", label = "Faixa universitária (22 a 24)"),
      @Threshold(min = 25, max = 130, tone = "neutral", label = "Acima do limite etário padrão")
    },
    sourceKind = "computed")
@ResourceFact(
    key = "elegivelIrrf",
    resourceKey = "administracao-pessoal.dependentes",
    label = "Elegível a IRRF ativo",
    description =
        "Verdadeiro quando vigente, habilitado em depIr e dentro do limite etário ou invalidez.",
    dataType = "boolean",
    icon = "receipt_long",
    tone = "info",
    valuePresentation = "badge",
    sourceKind = "computed")
@BulkResourceOperations(
    proposalOperationId = "ergon.dependentes.bulk.proposal",
    proposalResultsOperationId = "ergon.dependentes.bulk.proposal-results",
    executionOperationId = "ergon.dependentes.bulk.execution",
    executionResultsOperationId = "ergon.dependentes.bulk.results",
    cancelOperationId = "ergon.dependentes.bulk.cancel",
    updateSourceOperationId = "updateErgonDependente",
    protectedUpdateFields = {"nome", "dtnasc", "invalido"})
@ApiResource(
    value = ApiPaths.DEPENDENTES,
    resourceKey = "administracao-pessoal.dependentes",
    title = "Dependentes",
    description =
        "Dependentes legais e econômicos vinculados aos servidores públicos para fins tributários, previdenciários e assistenciais.",
    icon = "family_restroom",
    identity =
        @ResourceIdentity(
            keyField = "id",
            titleField = "nome",
            displayLabelField = "nome",
            subtitleField = "pessoaNome",
            metadataFields = {"parentesco", "depIr", "dtini"}),
    quickFilters = {
      @QuickFilter(
          id = "vigentes",
          label = "Somente vigentes",
          filter = "{\"somenteVigentes\":true}"),
      @QuickFilter(
          id = "deduz-irrf",
          label = "Deduz IRRF",
          filter = "{\"somenteDepIr\":true}"),
      @QuickFilter(
          id = "salario-familia",
          label = "Salário-família",
          filter = "{\"somenteSalarioFamilia\":true}"),
      @QuickFilter(
          id = "universitarios",
          label = "Universitários",
          filter = "{\"somenteUniversitarios\":true}"),
      @QuickFilter(
          id = "sem-cpf",
          label = "Sem CPF (eSocial)",
          filter = "{\"semCpf\":true}")
    })
@ApiGroup(
    value = "ergon",
    title = "Administração de pessoal",
    description = "Cadastros civis e assentamentos funcionais dos órgãos públicos.",
    icon = "groups")
public class DependenteController
    extends AbstractResourceController<
        DependenteResponseDTO,
        DependenteId,
        DependenteFilterDTO,
        CreateDependenteDTO,
        UpdateDependenteDTO> {

  private final DependenteService service;
  private final ResourceVersionEtagService etags;
  private final ResourceVersionScopeProvider scopes;
  @Autowired private ErgonBulkOperations bulk;
  @Autowired private ErgonDeterminationService determinations;

  public DependenteController(
      DependenteService service,
      ResourceVersionEtagService etags,
      ResourceVersionScopeProvider scopes) {
    this.service = service;
    this.etags = etags;
    this.scopes = scopes;
  }

  @Override
  @PostMapping("/stats/group-by")
  @Operation(
      summary = "Dependentes por parentesco e modalidade",
      description =
          "Agrega o conjunto filtrado de dependentes com abertura direta da ficha pela identidade real do bucket.")
  @UiAnalytics(
      projections = {
        @AnalyticsProjection(
            id = "dependentes-por-parentesco",
            intent = AnalyticsIntent.COMPOSITION,
            sourceOperation = AnalyticsOperation.GROUP_BY,
            primaryDimension = @AnalyticsDimensionBinding(field = "parentesco"),
            primaryMetrics =
                @AnalyticsMetricBinding(
                    field = "count",
                    aggregation = "count",
                    label = "Dependentes"),
            defaultLimit = 20),
        @AnalyticsProjection(
            id = "dependentes-nominais",
            intent = AnalyticsIntent.RANKING,
            sourceOperation = AnalyticsOperation.GROUP_BY,
            primaryDimension = @AnalyticsDimensionBinding(field = "id"),
            primaryMetrics =
                @AnalyticsMetricBinding(
                    field = "count",
                    aggregation = "count",
                    label = "Registros"),
            defaultLimit = 20,
            recordOpen =
                @AnalyticsRecordOpen(
                    sourceIdentityField = "key",
                    target =
                        @AnalyticsSurfaceTarget(
                            resourceKey = "administracao-pessoal.dependentes",
                            surfaceId = "ficha-dependente")))
      })
  public ResponseEntity<RestApiResponse<GroupByStatsResponse>> groupByStats(
      @RequestBody GroupByStatsRequest<DependenteFilterDTO> request) {
    return super.groupByStats(request);
  }

  @Override
  @PostMapping
  @Operation(
      operationId = "createErgonDependente",
      summary = "Incluir dependente",
      description = "Registra novo dependente vinculado ao prontuário funcional do servidor.")
  public ResponseEntity<RestApiResponse<DependenteResponseDTO>> create(
      @Valid @RequestBody CreateDependenteDTO dto) {
    return super.create(dto);
  }

  @Override
  @PutMapping("/{id}")
  @BulkResourceOperation(BulkResourceOperation.Role.UPDATE_SOURCE)
  @Operation(
      operationId = "updateErgonDependente",
      summary = "Atualizar dependente",
      description = "Atualiza dados cadastrais do dependente com controle otimista If-Match.")
  public ResponseEntity<RestApiResponse<DependenteResponseDTO>> update(
      @PathVariable DependenteId id, @Valid @RequestBody UpdateDependenteDTO dto) {
    return super.update(id, dto);
  }

  @Override
  protected DependenteService getService() {
    return service;
  }

  @Override
  protected DependenteId getResponseId(DependenteResponseDTO dto) {
    return dto.getId();
  }

  @GetMapping("/{id}/ficha-dependente")
  @PreAuthorize("hasAuthority('ERGON_READ')")
  @Operation(
      summary = "Consultar ficha 360° do dependente",
      description = "Retorna a projeção completa de leitura do dependente para o drawer Quick View.")
  @UiSurface(
      id = "ficha-dependente",
      kind = SurfaceKind.READ_PROJECTION,
      scope = SurfaceScope.ITEM,
      title = "Ficha do dependente",
      description = "Visão consolidada de elegibilidade, documentos civis e vigência do dependente.",
      intent = "summary",
      requiredAuthorities = {"ERGON_READ"})
  public ResponseEntity<DependenteResponseDTO> ficha(@PathVariable DependenteId id) {
    return withResourceVersion(ResponseEntity.ok(), id, service.findById(id));
  }

  @PatchMapping("/{id}/elegibilidade-beneficios")
  @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(
      operationId = "patchDependenteElegibilidade",
      summary = "Revisar elegibilidade e benefícios",
      description =
          "Atualiza indicadores de IRRF, salário-família, saúde e escolaridade do dependente com If-Match.")
  @ResourceIntent(
      id = "elegibilidade-beneficios",
      title = "Elegibilidade e benefícios",
      description = "Manutenção parcial dos indicadores tributários, previdenciários e estudantis.")
  @UiSurface(
      id = "elegibilidade-beneficios",
      kind = SurfaceKind.PARTIAL_FORM,
      scope = SurfaceScope.ITEM,
      title = "Elegibilidade e benefícios",
      description = "Revisão focalizada de IRRF, salário-família, plano de saúde e escolaridade.",
      intent = "elegibilidade-beneficios",
      requiredAuthorities = {"ERGON_WRITE"})
  public ResponseEntity<DependenteResponseDTO> elegibilidade(
      @PathVariable DependenteId id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch,
      @Valid @RequestBody AtualizarElegibilidadeDependenteDTO dto) {
    var p =
        new ResourceVersionUpdatePrecondition<DependenteId>(
            etags, ifMatch, scopes.currentScope(), "administracao-pessoal.dependentes", id);
    return withResourceVersion(ResponseEntity.ok(), id, service.atualizarElegibilidade(id, dto, p));
  }

  @PatchMapping("/{id}/certidao-civil")
  @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(
      operationId = "patchDependenteCertidao",
      summary = "Atualizar certidão de registro civil",
      description =
          "Atualiza termo, livro, folha, cartório e matrícula nacional da certidão civil com If-Match.")
  @ResourceIntent(
      id = "certidao-civil",
      title = "Certidão de registro civil",
      description = "Manutenção parcial dos dados cartoriais e certidão civil do dependente.")
  @UiSurface(
      id = "certidao-civil",
      kind = SurfaceKind.PARTIAL_FORM,
      scope = SurfaceScope.ITEM,
      title = "Certidão de registro civil",
      description = "Atualização dos dados de certidão de nascimento, casamento ou guarda.",
      intent = "certidao-civil",
      requiredAuthorities = {"ERGON_WRITE"})
  public ResponseEntity<DependenteResponseDTO> certidao(
      @PathVariable DependenteId id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch,
      @Valid @RequestBody AtualizarCertidaoDependenteDTO dto) {
    var p =
        new ResourceVersionUpdatePrecondition<DependenteId>(
            etags, ifMatch, scopes.currentScope(), "administracao-pessoal.dependentes", id);
    return withResourceVersion(ResponseEntity.ok(), id, service.atualizarCertidao(id, dto, p));
  }

  @PostMapping("/{id}/actions/encerrar-dependencia")
  @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(
      summary = "Encerrar vigência da dependência",
      description = "Registra a data de término da dependência e cessa deduções ativas. Exige If-Match.")
  @WorkflowAction(
      id = "encerrar-dependencia",
      title = "Encerrar dependência",
      description = "Encerra a vigência da dependência por maioridade, emancipação ou averbação.",
      scope = ActionScope.ITEM,
      requiredAuthorities = {"ERGON_WRITE"},
      allowedStates = {"VIGENTE"},
      confirmationRequired = true,
      resourceVersion = ActionRequirement.REQUIRED,
      resourceVersionTransport = ActionResourceVersionTransport.IF_MATCH)
  public ResponseEntity<DependenteResponseDTO> encerrar(
      @PathVariable DependenteId id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch,
      @Valid @RequestBody EncerrarDependenciaDTO dto) {
    var p =
        new ResourceVersionUpdatePrecondition<DependenteId>(
            etags, ifMatch, scopes.currentScope(), "administracao-pessoal.dependentes", id);
    return withResourceVersion(ResponseEntity.ok(), id, service.encerrarDependencia(id, dto, p));
  }

  @PostMapping("/{id}/actions/reativar-dependencia")
  @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(
      summary = "Reativar vigência da dependência",
      description = "Reativa dependência encerrada mediante documento comprobatório obrigatório. Exige If-Match.")
  @WorkflowAction(
      id = "reativar-dependencia",
      title = "Reativar dependência",
      description = "Restabelece a vigência da dependência mediante comprovação documental.",
      scope = ActionScope.ITEM,
      requiredAuthorities = {"ERGON_WRITE"},
      allowedStates = {"ENCERRADO"},
      confirmationRequired = true,
      resourceVersion = ActionRequirement.REQUIRED,
      resourceVersionTransport = ActionResourceVersionTransport.IF_MATCH)
  public ResponseEntity<DependenteResponseDTO> reativar(
      @PathVariable DependenteId id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch,
      @Valid @RequestBody ReativarDependenciaDTO dto) {
    var p =
        new ResourceVersionUpdatePrecondition<DependenteId>(
            etags, ifMatch, scopes.currentScope(), "administracao-pessoal.dependentes", id);
    return withResourceVersion(ResponseEntity.ok(), id, service.reativarDependencia(id, dto, p));
  }

  @PostMapping("/{id}/actions/homologar-invalidez")
  @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(
      summary = "Homologar invalidez permanente",
      description =
          "Registra invalidez atestada por junta médica oficial e isenta de teto etário. Exige If-Match.")
  @WorkflowAction(
      id = "homologar-invalidez",
      title = "Homologar invalidez",
      description = "Homologa laudo de invalidez permanente e assegura manutenção vitalícia.",
      scope = ActionScope.ITEM,
      requiredAuthorities = {"ERGON_WRITE"},
      allowedStates = {"VIGENTE", "ENCERRADO"},
      confirmationRequired = true,
      resourceVersion = ActionRequirement.REQUIRED,
      resourceVersionTransport = ActionResourceVersionTransport.IF_MATCH)
  public ResponseEntity<DependenteResponseDTO> homologarInvalidez(
      @PathVariable DependenteId id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch,
      @Valid @RequestBody HomologarInvalidezDependenteDTO dto) {
    var p =
        new ResourceVersionUpdatePrecondition<DependenteId>(
            etags, ifMatch, scopes.currentScope(), "administracao-pessoal.dependentes", id);
    return withResourceVersion(ResponseEntity.ok(), id, service.homologarInvalidez(id, dto, p));
  }

  @PostMapping("/determinations/elegibilidade")
  @PreAuthorize("hasAuthority('ERGON_READ')")
  @Operation(
      operationId = "determineErgonDependenteElegibilidade",
      summary = "Determinar idade e elegibilidade a IRRF em rascunho",
      description =
          "Projeta a descrição da modalidade, calcula a idade atual e avalia a elegibilidade a IRRF sem persistir.")
  public ResponseEntity<DependenteElegibilidadePreviewDTO> determineElegibilidade(
      @Valid @RequestBody DependenteElegibilidadeDraftDTO draft) {
    return ResponseEntity.ok(determinations.elegibilidadeDependente(draft));
  }

  @PostMapping("/bulk/update/evaluation")
  @Operation(
      operationId = "ergon.dependentes.bulk.uniform.evaluation",
      summary = "Avaliar edição uniforme de dependentes",
      description =
          "Avalia seleção, versões e alterações de dependentes sem modificar o cadastro; produz uma proposta protegida.")
  public RestApiResponse<BulkProposal> bulkEvaluate(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              content =
                  @io.swagger.v3.oas.annotations.media.Content(
                      schema =
                          @io.swagger.v3.oas.annotations.media.Schema(
                              implementation = ErgonBulkSchema.DependenteUniform.class)))
          @RequestBody
          BulkUniformEvaluationRequest<String, DependenteFilterDTO> request) {
    return RestApiResponse.success(bulk.uniform(Resource.DEPENDENTES, request), null);
  }

  @PostMapping("/bulk/update-items/evaluation")
  @Operation(
      operationId = "ergon.dependentes.bulk.items.evaluation",
      summary = "Avaliar edições por dependente",
      description =
          "Avalia deltas distintos e a versão esperada de cada dependente, com impedimentos individualizados.")
  public RestApiResponse<BulkProposal> bulkEvaluateItems(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              content =
                  @io.swagger.v3.oas.annotations.media.Content(
                      schema =
                          @io.swagger.v3.oas.annotations.media.Schema(
                              implementation = ErgonBulkSchema.DependenteItems.class)))
          @RequestBody
          BulkItemEvaluationRequest<String> request) {
    return RestApiResponse.success(bulk.items(Resource.DEPENDENTES, request), null);
  }

  @PostMapping("/bulk/update")
  @BulkOperation(
      mode = BulkMode.UNIFORM_UPDATE,
      evaluationOperationId = "ergon.dependentes.bulk.uniform.evaluation",
      atomicity = ActionCollectionAtomicity.PER_ITEM)
  @Operation(
      operationId = "ergon.dependentes.bulk.uniform",
      summary = "Confirmar edição uniforme de dependentes",
      description =
          "Confirma uma proposta avaliada; cada dependente é revalidado e possui recibo durável, sem repetir efeitos no replay.")
  public RestApiResponse<BulkExecution> bulkConfirm(
      @RequestBody BulkConfirmationRequest request,
      @Parameter(description = "Chave estável desta confirmação; repetir a mesma chave recupera a execução.")
          @RequestHeader("Idempotency-Key")
          String key) {
    return RestApiResponse.success(
        bulk.confirm(Resource.DEPENDENTES, "ergon.dependentes.bulk.uniform", request, key), null);
  }

  @PostMapping("/bulk/update-items")
  @BulkOperation(
      mode = BulkMode.PER_ITEM_UPDATE,
      evaluationOperationId = "ergon.dependentes.bulk.items.evaluation",
      atomicity = ActionCollectionAtomicity.PER_ITEM)
  @Operation(
      operationId = "ergon.dependentes.bulk.items",
      summary = "Confirmar edições por dependente",
      description =
          "Aplica os deltas individualizados preservados na proposta, com versão e resultado próprios por dependente.")
  public RestApiResponse<BulkExecution> bulkConfirmItems(
      @RequestBody BulkConfirmationRequest request,
      @Parameter(description = "Chave estável de confirmação para recuperar a mesma execução em caso de repetição.")
          @RequestHeader("Idempotency-Key")
          String key) {
    return RestApiResponse.success(
        bulk.confirm(Resource.DEPENDENTES, "ergon.dependentes.bulk.items", request, key), null);
  }

  @GetMapping("/bulk/proposals/{proposalId}")
  @BulkResourceOperation(BulkResourceOperation.Role.PROPOSAL)
  @Operation(
      operationId = "ergon.dependentes.bulk.proposal",
      summary = "Consultar proposta de lote de dependentes",
      description =
          "Consulta totais e intenção pública da proposta após conferir acesso ao conjunto completo de dependentes.")
  public RestApiResponse<BulkProposal> bulkProposal(
      @Parameter(description = "Identidade da proposta avaliada.") @PathVariable UUID proposalId) {
    return RestApiResponse.success(
        bulk.proposal(
            Resource.DEPENDENTES, bulk.locate(Resource.DEPENDENTES, proposalId, false), proposalId),
        null);
  }

  @GetMapping("/bulk/proposals/{proposalId}/results")
  @BulkResourceOperation(BulkResourceOperation.Role.PROPOSAL_RESULTS)
  @Operation(
      operationId = "ergon.dependentes.bulk.proposal-results",
      summary = "Inspecionar avaliação do lote de dependentes",
      description =
          "Pagina dependentes executáveis e bloqueados, com diagnósticos seguros e cursor vinculado ao solicitante.")
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
            Resource.DEPENDENTES,
            bulk.locate(Resource.DEPENDENTES, proposalId, false),
            proposalId,
            size,
            after),
        null);
  }

  @GetMapping("/bulk/executions/{executionId}")
  @BulkResourceOperation(BulkResourceOperation.Role.EXECUTION)
  @Operation(
      operationId = "ergon.dependentes.bulk.execution",
      summary = "Consultar execução do lote de dependentes",
      description = "Retorna estado e contadores reconciliados a partir dos recibos persistidos.")
  public RestApiResponse<BulkExecution> bulkExecution(
      @Parameter(description = "Identidade da execução confirmada.")
          @PathVariable
          UUID executionId) {
    return RestApiResponse.success(
        bulk.execution(
            Resource.DEPENDENTES,
            bulk.locate(Resource.DEPENDENTES, executionId, true),
            executionId),
        null);
  }

  @GetMapping("/bulk/executions/{executionId}/results")
  @BulkResourceOperation(BulkResourceOperation.Role.EXECUTION_RESULTS)
  @Operation(
      operationId = "ergon.dependentes.bulk.results",
      summary = "Consultar recibos do lote de dependentes",
      description = "Pagina resultados por dependente sem expor dados sensíveis da proposta.")
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
            Resource.DEPENDENTES,
            bulk.locate(Resource.DEPENDENTES, executionId, true),
            executionId,
            size,
            after),
        null);
  }

  @PostMapping("/bulk/executions/{executionId}/cancel")
  @BulkResourceOperation(BulkResourceOperation.Role.CANCEL)
  @Operation(
      operationId = "ergon.dependentes.bulk.cancel",
      summary = "Solicitar parada do lote de dependentes",
      description =
          "Registra a solicitação de parada sem desfazer alterações já confirmadas e sem reabrir efeitos no replay.")
  public RestApiResponse<BulkExecution> bulkCancel(
      @Parameter(description = "Identidade da execução a interromper.")
          @PathVariable
          UUID executionId) {
    return RestApiResponse.success(
        bulk.cancel(
            Resource.DEPENDENTES,
            bulk.locate(Resource.DEPENDENTES, executionId, true),
            executionId),
        null);
  }

  @PostMapping("/bulk/encerrar-dependencia-lote/evaluation")
  @Operation(
      operationId = "ergon.dependentes.encerrar-dependencia-lote.evaluation",
      summary = "Avaliar encerramento coletivo de dependências",
      description =
          "Avalia encerramento coletivo de dependências vigentes com ato fundamentador, sem persistir alterações.")
  public RestApiResponse<BulkProposal> bulkEncerrarEvaluate(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              content =
                  @io.swagger.v3.oas.annotations.media.Content(
                      schema =
                          @io.swagger.v3.oas.annotations.media.Schema(
                              implementation = ErgonBulkSchema.EncerrarDependenciaCommand.class)))
          @RequestBody
          BulkCommandEvaluationRequest<EncerrarDependenciaLoteDTO, String, DependenteFilterDTO>
              request) {
    return RestApiResponse.success(
        bulk.command(Resource.DEPENDENTES, "encerrar-dependencia-lote", request), null);
  }

  @PostMapping("/actions/encerrar-dependencia-lote")
  @WorkflowAction(
      id = "encerrar-dependencia-lote",
      title = "Encerrar dependências em lote",
      description = "Encerra coletivamente dependências vigentes com registro de ato fundamentador.",
      scope = ActionScope.COLLECTION,
      requiredAuthorities = {"ERGON_WRITE"},
      confirmationRequired = true,
      idempotencyKey = ActionRequirement.REQUIRED,
      atomicity = ActionCollectionAtomicity.PER_ITEM,
      outcomeMode = ActionOutcomeMode.PER_ITEM,
      maxSelection = 200)
  @BulkOperation(
      mode = BulkMode.DOMAIN_COMMAND,
      evaluationOperationId = "ergon.dependentes.encerrar-dependencia-lote.evaluation",
      atomicity = ActionCollectionAtomicity.PER_ITEM)
  @Operation(
      operationId = "ergon.dependentes.encerrar-dependencia-lote",
      summary = "Encerrar dependências em lote",
      description = "Executa encerramento coletivo governado de dependências vigentes.")
  public RestApiResponse<BulkExecution> bulkEncerrarConfirm(
      @RequestBody BulkConfirmationRequest request,
      @Parameter(description = "Chave estável do ato coletivo para impedir efeitos duplicados.")
          @RequestHeader("Idempotency-Key")
          String key) {
    return RestApiResponse.success(
        bulk.confirm(
            Resource.DEPENDENTES, "ergon.dependentes.encerrar-dependencia-lote", request, key),
        null);
  }
}
