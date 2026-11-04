package br.com.praxis.ergonshowcase.controller;
import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.bulk.*;
import br.com.praxis.ergonshowcase.bulk.ErgonBulkDomain.Resource;
import java.util.UUID;
import org.praxisplatform.uischema.action.*;
import org.praxisplatform.uischema.bulk.*;
import org.praxisplatform.uischema.dto.CursorPage;
import org.praxisplatform.uischema.rest.response.RestApiResponse;
import io.swagger.v3.oas.annotations.Parameter;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.service.VendaLicencaEspecialService;
import io.swagger.v3.oas.annotations.Operation;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.controller.base.AbstractDuplicateDraftResourceController;
import org.praxisplatform.uischema.fact.ResourceFact;
import org.praxisplatform.uischema.surface.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@BulkResourceOperations(proposalOperationId = "ergon.vendas-licenca-especial.bulk.proposal", proposalResultsOperationId = "ergon.vendas-licenca-especial.bulk.proposal-results",
    executionOperationId = "ergon.vendas-licenca-especial.bulk.execution", executionResultsOperationId = "ergon.vendas-licenca-especial.bulk.results", cancelOperationId = "ergon.vendas-licenca-especial.bulk.cancel",
    updateSourceOperationId = "updateErgonVendaLicencaEspecial", protectedUpdateFields = {"empCodigo", "numfunc", "numvinc", "periodoAquisitivoLicencaId", "chavePeraqlicesp", "situacao", "dtPagamento", "dtEstorno", "modalidade"})
@ApiResource(value = ApiPaths.VENDAS_LICENCA_ESPECIAL, resourceKey = "administracao-pessoal.vendas-licenca-especial", title = "Vendas de licença especial", description = "Registro de conversão de dias de licença em pecúnia ou abono e sua referência de liquidação.", icon = "sell",
    identity = @ResourceIdentity(keyField = "id", titleField = "descricao", subtitleField = "pessoaNome", metadataFields = {"mesAnoFolha", "numFolha"}),
    quickFilters = {@QuickFilter(id = "pecunia", label = "Venda em pecúnia", filter = "{\"modalidade\":\"PECUNIA\"}"), @QuickFilter(id = "abono", label = "Abono pecuniário", filter = "{\"modalidade\":\"ABONO_JUNTO_GOZO\"}"), @QuickFilter(id = "sem-pagamento", label = "Sem pagamento registrado", filter = "{\"semPagamento\":true}")})
@ApiGroup(value = "ergon", title = "Administração de pessoal", description = "Assentamentos funcionais de órgãos públicos.", icon = "groups")
public class VendaLicencaEspecialController extends org.praxisplatform.uischema.controller.base.AbstractBulkGovernedDuplicateDraftResourceController<VendaLicencaEspecialResponseDTO, Long, VendaLicencaEspecialFilterDTO, CreateVendaLicencaEspecialDTO, UpdateVendaLicencaEspecialDTO, VendaLicencaEspecialDuplicateDraftDTO> {
  @org.springframework.beans.factory.annotation.Autowired private ErgonBulkOperations bulk;
  private final VendaLicencaEspecialService service;
  public VendaLicencaEspecialController(VendaLicencaEspecialService service) { this.service = service; }
  @Override protected VendaLicencaEspecialService getService() { return service; }
  @Override protected Long getResponseId(VendaLicencaEspecialResponseDTO dto) { return dto.getId(); }
  @GetMapping("/{id}/detail") @PreAuthorize("hasAuthority('ERGON_READ')")
  @Operation(summary = "Inspecionar venda de licença especial", description = "Consulta o evento de conversão, dias vendidos e posição de pagamento registrada.")
  @UiSurface(id = "detail", kind = SurfaceKind.READ_PROJECTION, scope = SurfaceScope.ITEM, title = "Venda de licença especial", description = "Conversão pecuniária, período de origem e referência de liquidação.", requiredAuthorities = {"ERGON_READ"})
  public ResponseEntity<VendaLicencaEspecialResponseDTO> detail(@PathVariable Long id) { return withResourceVersion(ResponseEntity.ok(), id, service.findById(id)); }

  @Override @PostMapping("/stats/group-by")
  @Operation(operationId = "statsVendaLicencaEspecial", summary = "Distribuição e ranking do direito", description = "Agrega somente registros autorizados pelo filtro oficial; buckets nominais abrem a ficha 360° pela identidade persistida.")
  @UiAnalytics(projections = {
    @AnalyticsProjection(id = "situacoes", intent = AnalyticsIntent.COMPOSITION, sourceOperation = AnalyticsOperation.GROUP_BY,
      primaryDimension = @AnalyticsDimensionBinding(field = "situacao"), primaryMetrics = @AnalyticsMetricBinding(field = "count", aggregation = "count", label = "Registros")),
    @AnalyticsProjection(id = "dias-por-registro", intent = AnalyticsIntent.RANKING, sourceOperation = AnalyticsOperation.GROUP_BY,
      primaryDimension = @AnalyticsDimensionBinding(field = "id"), primaryMetrics = @AnalyticsMetricBinding(field = "diasVendidos", aggregation = "sum", label = "Dias"),
      recordOpen = @AnalyticsRecordOpen(sourceIdentityField = "key", target = @AnalyticsSurfaceTarget(resourceKey = "administracao-pessoal.vendas-licenca-especial", surfaceId = "detail")))
  })
  public ResponseEntity<org.praxisplatform.uischema.rest.response.RestApiResponse<org.praxisplatform.uischema.stats.dto.GroupByStatsResponse>> groupByStats(
      @RequestBody org.praxisplatform.uischema.stats.dto.GroupByStatsRequest<VendaLicencaEspecialFilterDTO> request) { return super.groupByStats(request); }

  @PatchMapping("/{id}/atualizar-liquidacao-folha-venda-licesp") @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(operationId = "patchVendaLicencaEspecialLiquidacaoPatchDTO", summary = "Apuração financeira da venda", description = "Apura valor e observações com If-Match obrigatório. Em abono vinculado, origem, dias, folha e documentos são mantidos pelo gozo. Valores pagos não podem ser retificados; a liquidação exige a ação de pagamento.")
  @ResourceIntent(id = "atualizar-liquidacao-folha-venda-licesp", title = "Apuração financeira da venda")
  @UiSurface(id = "atualizar-liquidacao-folha-venda-licesp", kind = SurfaceKind.PARTIAL_FORM, scope = SurfaceScope.ITEM, title = "Apuração financeira da venda", requiredAuthorities = {"ERGON_WRITE"})
  public ResponseEntity<VendaLicencaEspecialResponseDTO> patchVendaLicencaEspecialLiquidacaoPatchDTO(@PathVariable Long id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch, @jakarta.validation.Valid @RequestBody VendaLicencaEspecialLiquidacaoPatchDTO dto) {
    return ErgonHttpContract.itemBody(executeVersionedItemPatch("atualizar-liquidacao-folha-venda-licesp", id, ifMatch, condition -> service.patch(id, dto, condition)));
  }

  @Override @PutMapping("/{id}") @BulkResourceOperation(BulkResourceOperation.Role.UPDATE_SOURCE)
  @Operation(operationId = "updateErgonVendaLicencaEspecial", summary = "Retificar assentamento", description = "Mantém os dados cadastrais preservando a identidade e o estado de ciclo de vida, com If-Match obrigatório.")
  public ResponseEntity<RestApiResponse<VendaLicencaEspecialResponseDTO>> update(@PathVariable Long id, @jakarta.validation.Valid @RequestBody UpdateVendaLicencaEspecialDTO dto) { return super.update(id,dto); }

  @PostMapping("/{id}/actions/autorizar-venda-licenca-especial") @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(operationId = "actionVendaLicencaEspecialAutorizarVendaLicencaEspecial", summary = "Autorizar venda", description = "Executa o ato no órgão autorizado, validando estado e versão persistidos. A mesma chave e conteúdo recuperam o recibo original sem repetir efeitos.")
  @WorkflowAction(id = "autorizar-venda-licenca-especial", title = "Autorizar venda", scope = ActionScope.ITEM, requiredAuthorities = {"ERGON_WRITE"},
    allowedStates = {"SOLICITADA"}, confirmationRequired = true, idempotencyKey = ActionRequirement.REQUIRED,
    resourceVersion = ActionRequirement.REQUIRED, resourceVersionTransport = ActionResourceVersionTransport.IF_MATCH)
  public ResponseEntity<VendaLicencaEspecialResponseDTO> actionAutorizarVendaLicencaEspecial(@PathVariable Long id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch, @RequestHeader("Idempotency-Key") String key,
      @jakarta.validation.Valid @RequestBody TempoLicencaTransicaoDTO dto) {
    return ErgonHttpContract.itemBody(executeVersionedItemCommand("autorizar-venda-licenca-especial", id, ifMatch, key, dto,
        condition -> service.transition(id, "autorizar-venda-licenca-especial", dto, condition)));
  }

  @PostMapping("/{id}/actions/pagar-venda-licenca-especial") @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(operationId = "actionVendaLicencaEspecialPagarVendaLicencaEspecial", summary = "Liquidar venda", description = "Executa o ato no órgão autorizado, validando estado e versão persistidos. A mesma chave e conteúdo recuperam o recibo original sem repetir efeitos.")
  @WorkflowAction(id = "pagar-venda-licenca-especial", title = "Liquidar venda", scope = ActionScope.ITEM, requiredAuthorities = {"ERGON_WRITE"},
    allowedStates = {"AUTORIZADA"}, confirmationRequired = true, idempotencyKey = ActionRequirement.REQUIRED,
    resourceVersion = ActionRequirement.REQUIRED, resourceVersionTransport = ActionResourceVersionTransport.IF_MATCH)
  public ResponseEntity<VendaLicencaEspecialResponseDTO> actionPagarVendaLicencaEspecial(@PathVariable Long id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch, @RequestHeader("Idempotency-Key") String key,
      @jakarta.validation.Valid @RequestBody TempoLicencaTransicaoDTO dto) {
    return ErgonHttpContract.itemBody(executeVersionedItemCommand("pagar-venda-licenca-especial", id, ifMatch, key, dto,
        condition -> service.transition(id, "pagar-venda-licenca-especial", dto, condition)));
  }

  @PostMapping("/{id}/actions/estornar-venda-licenca-especial") @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(operationId = "actionVendaLicencaEspecialEstornarVendaLicencaEspecial", summary = "Estornar venda ou abono", description = "Compensa o pagamento ou retira a autorização, preservando data e valor originais e registrando a data de estorno. No abono vinculado, não altera os dias reservados pelo gozo. If-Match e Idempotency-Key obrigatórios; não executa transferência bancária externa.")
  @WorkflowAction(id = "estornar-venda-licenca-especial", title = "Estornar venda ou abono", description = "Registra a compensação sem apagar o pagamento original. No abono, preserva a reserva de dias do gozo; reconciliação explícita pode emitir nova autorização.", scope = ActionScope.ITEM, requiredAuthorities = {"ERGON_WRITE"},
    allowedStates = {"SOLICITADA", "AUTORIZADA", "PAGA"}, confirmationRequired = true, idempotencyKey = ActionRequirement.REQUIRED,
    resourceVersion = ActionRequirement.REQUIRED, resourceVersionTransport = ActionResourceVersionTransport.IF_MATCH)
  public ResponseEntity<VendaLicencaEspecialResponseDTO> actionEstornarVendaLicencaEspecial(@PathVariable Long id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch, @RequestHeader("Idempotency-Key") String key,
      @jakarta.validation.Valid @RequestBody TempoLicencaTransicaoDTO dto) {
    return ErgonHttpContract.itemBody(executeVersionedItemCommand("estornar-venda-licenca-especial", id, ifMatch, key, dto,
        condition -> service.transition(id, "estornar-venda-licenca-especial", dto, condition)));
  }
  @Override
  @PostMapping("/bulk/update/evaluation")
  @Operation(
      operationId = "ergon.vendas-licenca-especial.bulk.uniform.evaluation",
      summary = "Avaliar edição uniforme de assentamentos de tempo e licença",
      description =
          "Avalia seleção, versões e alterações de assentamentos selecionados sem modificar o cadastro; produz uma proposta protegida.")
  @BulkResourceOperation(BulkResourceOperation.Role.UNIFORM_EVALUATION)
  public org.springframework.http.ResponseEntity<RestApiResponse<BulkProposal>> evaluateBulkUpdate(@RequestBody BulkUniformEvaluationRequest<?, VendaLicencaEspecialFilterDTO> request) {
    return super.evaluateBulkUpdate(request);
  }

  @Override
  @PostMapping("/bulk/update-items/evaluation")
  @Operation(
      operationId = "ergon.vendas-licenca-especial.bulk.items.evaluation",
      summary = "Avaliar edições por parcela de gozo de férias",
      description =
          "Avalia deltas distintos e a versão esperada de cada assentamento, com impedimentos individualizados.")
  @BulkResourceOperation(BulkResourceOperation.Role.ITEM_EVALUATION)
  public org.springframework.http.ResponseEntity<RestApiResponse<BulkProposal>> evaluateBulkUpdateItems(@RequestBody BulkItemEvaluationRequest<?> request) {
    return super.evaluateBulkUpdateItems(request);
  }

  @Override
  @PostMapping("/bulk/update")
  @BulkOperation(
      mode = BulkMode.UNIFORM_UPDATE,
      evaluationOperationId = "ergon.vendas-licenca-especial.bulk.uniform.evaluation",
      atomicity = ActionCollectionAtomicity.PER_ITEM)
  @Operation(
      operationId = "ergon.vendas-licenca-especial.bulk.uniform",
      summary = "Confirmar edição uniforme de assentamentos de tempo e licença",
      description =
          "Confirma uma proposta avaliada; cada assentamento é revalidada e possui recibo durável.")
  @BulkResourceOperation(BulkResourceOperation.Role.UNIFORM_CONFIRMATION)
  public org.springframework.http.ResponseEntity<RestApiResponse<BulkExecution>> confirmBulkUpdate(@RequestBody BulkConfirmationRequest request, @RequestHeader("Idempotency-Key") String key) {
    return super.confirmBulkUpdate(request, key);
  }

  @Override
  @PostMapping("/bulk/update-items")
  @BulkOperation(
      mode = BulkMode.PER_ITEM_UPDATE,
      evaluationOperationId = "ergon.vendas-licenca-especial.bulk.items.evaluation",
      atomicity = ActionCollectionAtomicity.PER_ITEM)
  @Operation(
      operationId = "ergon.vendas-licenca-especial.bulk.items",
      summary = "Confirmar edições por parcela de gozo de férias",
      description =
          "Aplica os deltas individualizados preservados na proposta, com versão e resultado próprios por parcela.")
  @BulkResourceOperation(BulkResourceOperation.Role.ITEM_CONFIRMATION)
  public org.springframework.http.ResponseEntity<RestApiResponse<BulkExecution>> confirmBulkUpdateItems(@RequestBody BulkConfirmationRequest request, @RequestHeader("Idempotency-Key") String key) {
    return super.confirmBulkUpdateItems(request, key);
  }











  @PostMapping("/bulk/autorizar-vendas-licenca-especial-lote/evaluation")
  @Operation(
      operationId = "ergon.vendas-licenca-especial.autorizar-vendas-licenca-especial-lote.evaluation",
      summary = "Avaliar autorização de vendas",
      description =
          "Avalia o ato coletivo no mesmo órgão, sem persistir efeitos; cada alvo conserva sua versão e seu impedimento individual.")
  public org.springframework.http.ResponseEntity<RestApiResponse<BulkProposal>> bulkCommandEvaluate(
      
          @RequestBody
          BulkCommandEvaluationRequest<TempoLicencaTransicaoDTO, ?, VendaLicencaEspecialFilterDTO>
              request) {
    return evaluateBulkDomainCommand("autorizar-vendas-licenca-especial-lote", request);
  }

  @PostMapping("/actions/autorizar-vendas-licenca-especial-lote")
  @WorkflowAction(
      id = "autorizar-vendas-licenca-especial-lote",
      title = "Autorizar vendas em lote",
      description =
          "Autoriza vendas solicitadas após conferir a política efetiva e o saldo do período aquisitivo, com resultado individual por venda.",
      scope = ActionScope.COLLECTION,
      requiredAuthorities = {"ERGON_WRITE"},
      confirmationRequired = true,
      idempotencyKey = ActionRequirement.REQUIRED,
      atomicity = ActionCollectionAtomicity.PER_ITEM,
      outcomeMode = ActionOutcomeMode.PER_ITEM,
      maxSelection = 200)
  @BulkOperation(
      mode = BulkMode.DOMAIN_COMMAND,
      evaluationOperationId = "ergon.vendas-licenca-especial.autorizar-vendas-licenca-especial-lote.evaluation",
      atomicity = ActionCollectionAtomicity.PER_ITEM)
  @Operation(
      operationId = "ergon.vendas-licenca-especial.autorizar-vendas-licenca-especial-lote",
      summary = "Autorizar vendas em lote",
      description = "Autoriza vendas solicitadas após conferir a política efetiva e o saldo do período aquisitivo, com resultado individual por venda.")
  public org.springframework.http.ResponseEntity<RestApiResponse<BulkExecution>> bulkCommandConfirm(
      @RequestBody BulkConfirmationRequest request,
      @Parameter(description = "Chave estável do ato coletivo para impedir efeitos duplicados.")
          @RequestHeader("Idempotency-Key")
          String key) {
    return confirmBulkDomainCommand("autorizar-vendas-licenca-especial-lote", request, key);
  }

  @org.springframework.beans.factory.annotation.Autowired private br.com.praxis.ergonshowcase.service.TempoLicencaPreviewService previews;
  @PostMapping("/determinations/preview") @PreAuthorize("hasAuthority('ERGON_READ')")
  @Operation(operationId = "determineVendaLicencaEspecial", summary = "Saldo após venda previsto", description = "Calcula prévia de criação sem persistir, reservar saldo ou autorizar o ato.")
  public TempoLicencaPrevia.Resultado preview(@jakarta.validation.Valid @RequestBody TempoLicencaPrevia.Venda input) {return previews.venda(input);}
  @Override @PostMapping
  @Operation(operationId = "createErgonVendaLicencaEspecial", summary = "Registrar assentamento", description = "Cria no estado inicial do domínio, revalidando vínculo, política e contabilidade no servidor.")
  public ResponseEntity<RestApiResponse<VendaLicencaEspecialResponseDTO>> create(@jakarta.validation.Valid @RequestBody CreateVendaLicencaEspecialDTO input) {return super.create(input);}

  @PostMapping("/{id}/actions/reconciliar-abono-vinculado") @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(operationId = "reconcileVendaLicencaEspecialAbono",summary = "Reconciliar abono vinculado",description = "Após estorno, cria nova autorização na coleção financeira do gozo sem alterar o registro histórico retornado. Uma autorização já ativa é reutilizada. Não copia valor ou pagamento anterior nem duplica o débito de dias; a nova autorização exige apuração própria.")
  @WorkflowAction(id = "reconciliar-abono-vinculado",title = "Reconciliar abono vinculado",description = "Após estorno, disponibiliza nova autorização na coleção financeira do gozo. A resposta conserva a identidade e a revisão do registro histórico de origem.",scope = ActionScope.ITEM,requiredAuthorities = {"ERGON_WRITE"},
      allowedStates = {"AUTORIZADA","PAGA","ESTORNADA"},confirmationRequired = true,idempotencyKey = ActionRequirement.REQUIRED,resourceVersion = ActionRequirement.REQUIRED,resourceVersionTransport = ActionResourceVersionTransport.IF_MATCH)
  public ResponseEntity<VendaLicencaEspecialResponseDTO> reconcileLinkedAbono(@PathVariable Long id,
      @RequestHeader(value = "If-Match",required = false) String ifMatch,@RequestHeader("Idempotency-Key") String key,
      @jakarta.validation.Valid @RequestBody TempoLicencaTransicaoDTO dto) {
    return ErgonHttpContract.itemBody(executeVersionedItemCommand("reconciliar-abono-vinculado", id, ifMatch, key, dto,
        condition -> service.transition(id,"reconciliar-abono-vinculado",dto,condition)));
  }
  @Override @SuppressWarnings({"unchecked", "rawtypes"})
  protected org.praxisplatform.uischema.bulk.JpaResourceBulkEngine getBulkEngine() { return bulk.engine(Resource.VENDAS_LICENCA_ESPECIAL); }
  @Override protected String bulkProposalOperationId(UUID id) { return bulk.locate(Resource.VENDAS_LICENCA_ESPECIAL, id, false); }
  @Override protected String bulkExecutionOperationId(UUID id) { return bulk.locate(Resource.VENDAS_LICENCA_ESPECIAL, id, true); }

}
