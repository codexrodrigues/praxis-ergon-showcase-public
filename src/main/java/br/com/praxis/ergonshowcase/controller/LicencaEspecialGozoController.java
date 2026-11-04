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
import br.com.praxis.ergonshowcase.service.LicencaEspecialGozoService;
import io.swagger.v3.oas.annotations.Operation;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.controller.base.AbstractDuplicateDraftResourceController;
import org.praxisplatform.uischema.fact.ResourceFact;
import org.praxisplatform.uischema.surface.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@BulkResourceOperations(proposalOperationId = "ergon.licencas-especiais-gozo.bulk.proposal", proposalResultsOperationId = "ergon.licencas-especiais-gozo.bulk.proposal-results",
    executionOperationId = "ergon.licencas-especiais-gozo.bulk.execution", executionResultsOperationId = "ergon.licencas-especiais-gozo.bulk.results", cancelOperationId = "ergon.licencas-especiais-gozo.bulk.cancel",
    updateSourceOperationId = "updateErgonLicencaEspecialGozo", protectedUpdateFields = {"empCodigo", "numfunc", "numvinc", "periodoAquisitivoLicencaId", "chavePeraqlicesp", "ordemParcela", "situacao", "dtfim"})
@ApiResource(value = ApiPaths.LICENCAS_ESPECIAIS_GOZO, resourceKey = "administracao-pessoal.licencas-especiais-gozo", title = "Gozo de licença especial", description = "Parcelas de fruição de licença especial associadas ao ciclo aquisitivo e ao vínculo funcional.", icon = "beach_access",
    identity = @ResourceIdentity(keyField = "id", titleField = "descricao", subtitleField = "pessoaNome", metadataFields = {"chavePeraqlicesp", "abonoPecuniario"}),
    quickFilters = {@QuickFilter(id = "abono", label = "Com abono pecuniário", filter = "{\"abonoPecuniario\":true}"), @QuickFilter(id = "sem-termino", label = "Sem término efetivo", filter = "{\"semTerminoEfetivo\":true}"), @QuickFilter(id = "recentes", label = "Iniciadas nos últimos 30 dias", filter = "{\"inicioUltimosDias\":30}")})
@ApiGroup(value = "ergon", title = "Administração de pessoal", description = "Assentamentos funcionais de órgãos públicos.", icon = "groups")
public class LicencaEspecialGozoController extends org.praxisplatform.uischema.controller.base.AbstractBulkGovernedDuplicateDraftResourceController<LicencaEspecialGozoResponseDTO, Long, LicencaEspecialGozoFilterDTO, CreateLicencaEspecialGozoDTO, UpdateLicencaEspecialGozoDTO, LicencaEspecialGozoDuplicateDraftDTO> {
  @org.springframework.beans.factory.annotation.Autowired private ErgonBulkOperations bulk;
  @org.springframework.beans.factory.annotation.Autowired private br.com.praxis.ergonshowcase.service.VendaLicencaEspecialService vendaLicencaEspecialChildren;
  private final LicencaEspecialGozoService service;
  public LicencaEspecialGozoController(LicencaEspecialGozoService service) { this.service = service; }
  @Override protected LicencaEspecialGozoService getService() { return service; }
  @Override protected Long getResponseId(LicencaEspecialGozoResponseDTO dto) { return dto.getId(); }
  @GetMapping("/{id}/detail") @PreAuthorize("hasAuthority('ERGON_READ')")
  @Operation(summary = "Inspecionar parcela de licença especial", description = "Consulta completa da fruição, período de origem e referências de abono e folha.")
  @UiSurface(id = "detail", kind = SurfaceKind.READ_PROJECTION, scope = SurfaceScope.ITEM, title = "Parcela de licença especial", description = "Programação, duração, abono e vínculo da parcela de fruição.", requiredAuthorities = {"ERGON_READ"})
  public ResponseEntity<LicencaEspecialGozoResponseDTO> detail(@PathVariable Long id) { return withResourceVersion(ResponseEntity.ok(), id, service.findById(id)); }

  @Override @PostMapping("/stats/group-by")
  @Operation(operationId = "statsLicencaEspecialGozo", summary = "Distribuição e ranking do direito", description = "Agrega somente registros autorizados pelo filtro oficial; buckets nominais abrem a ficha 360° pela identidade persistida.")
  @UiAnalytics(projections = {
    @AnalyticsProjection(id = "situacoes", intent = AnalyticsIntent.COMPOSITION, sourceOperation = AnalyticsOperation.GROUP_BY,
      primaryDimension = @AnalyticsDimensionBinding(field = "situacao"), primaryMetrics = @AnalyticsMetricBinding(field = "count", aggregation = "count", label = "Registros")),
    @AnalyticsProjection(id = "dias-por-registro", intent = AnalyticsIntent.RANKING, sourceOperation = AnalyticsOperation.GROUP_BY,
      primaryDimension = @AnalyticsDimensionBinding(field = "id"), primaryMetrics = @AnalyticsMetricBinding(field = "diasGozo", aggregation = "sum", label = "Dias"),
      recordOpen = @AnalyticsRecordOpen(sourceIdentityField = "key", target = @AnalyticsSurfaceTarget(resourceKey = "administracao-pessoal.licencas-especiais-gozo", surfaceId = "detail")))
  })
  public ResponseEntity<org.praxisplatform.uischema.rest.response.RestApiResponse<org.praxisplatform.uischema.stats.dto.GroupByStatsResponse>> groupByStats(
      @RequestBody org.praxisplatform.uischema.stats.dto.GroupByStatsRequest<LicencaEspecialGozoFilterDTO> request) { return super.groupByStats(request); }

  @PatchMapping("/{id}/reagendar-gozo-licenca-especial") @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(operationId = "patchLicencaEspecialGozoReagendamentoPatchDTO", summary = "Programação e abono", description = "Atualiza somente campos explicitamente enviados, sob lock, no órgão autorizado e com If-Match obrigatório.")
  @ResourceIntent(id = "reagendar-gozo-licenca-especial", title = "Programação e abono")
  @UiSurface(id = "reagendar-gozo-licenca-especial", kind = SurfaceKind.PARTIAL_FORM, scope = SurfaceScope.ITEM, title = "Programação e abono", requiredAuthorities = {"ERGON_WRITE"})
  public ResponseEntity<LicencaEspecialGozoResponseDTO> patchLicencaEspecialGozoReagendamentoPatchDTO(@PathVariable Long id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch, @jakarta.validation.Valid @RequestBody LicencaEspecialGozoReagendamentoPatchDTO dto) {
    return ErgonHttpContract.itemBody(executeVersionedItemPatch("reagendar-gozo-licenca-especial", id, ifMatch, condition -> service.patch(id, dto, condition)));
  }

  @PatchMapping("/{id}/atualizar-substituto-processo-licenca-especial") @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(operationId = "patchLicencaEspecialGozoSubstitutoPatchDTO", summary = "Documentação da parcela", description = "Atualiza somente campos explicitamente enviados, sob lock, no órgão autorizado e com If-Match obrigatório.")
  @ResourceIntent(id = "atualizar-substituto-processo-licenca-especial", title = "Documentação da parcela")
  @UiSurface(id = "atualizar-substituto-processo-licenca-especial", kind = SurfaceKind.PARTIAL_FORM, scope = SurfaceScope.ITEM, title = "Documentação da parcela", requiredAuthorities = {"ERGON_WRITE"})
  public ResponseEntity<LicencaEspecialGozoResponseDTO> patchLicencaEspecialGozoSubstitutoPatchDTO(@PathVariable Long id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch, @jakarta.validation.Valid @RequestBody LicencaEspecialGozoSubstitutoPatchDTO dto) {
    return ErgonHttpContract.itemBody(executeVersionedItemPatch("atualizar-substituto-processo-licenca-especial", id, ifMatch, condition -> service.patch(id, dto, condition)));
  }

  @Override @PutMapping("/{id}") @BulkResourceOperation(BulkResourceOperation.Role.UPDATE_SOURCE)
  @Operation(operationId = "updateErgonLicencaEspecialGozo", summary = "Retificar assentamento", description = "Mantém os dados cadastrais preservando a identidade e o estado de ciclo de vida, com If-Match obrigatório.")
  public ResponseEntity<RestApiResponse<LicencaEspecialGozoResponseDTO>> update(@PathVariable Long id, @jakarta.validation.Valid @RequestBody UpdateLicencaEspecialGozoDTO dto) { return super.update(id,dto); }

  @PostMapping("/{id}/actions/homologar-licenca-especial") @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(operationId = "actionLicencaEspecialGozoHomologarLicencaEspecial", summary = "Homologar parcela", description = "Executa o ato no órgão autorizado, validando estado e versão persistidos. A mesma chave e conteúdo recuperam o recibo original sem repetir efeitos.")
  @WorkflowAction(id = "homologar-licenca-especial", title = "Homologar parcela", scope = ActionScope.ITEM, requiredAuthorities = {"ERGON_WRITE"},
    allowedStates = {"PROGRAMADA"}, confirmationRequired = true, idempotencyKey = ActionRequirement.REQUIRED,
    resourceVersion = ActionRequirement.REQUIRED, resourceVersionTransport = ActionResourceVersionTransport.IF_MATCH)
  public ResponseEntity<LicencaEspecialGozoResponseDTO> actionHomologarLicencaEspecial(@PathVariable Long id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch, @RequestHeader("Idempotency-Key") String key,
      @jakarta.validation.Valid @RequestBody TempoLicencaTransicaoDTO dto) {
    return ErgonHttpContract.itemBody(executeVersionedItemCommand("homologar-licenca-especial", id, ifMatch, key, dto,
        condition -> service.transition(id, "homologar-licenca-especial", dto, condition)));
  }

  @PostMapping("/{id}/actions/iniciar-gozo-licenca-especial") @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(operationId = "actionLicencaEspecialGozoIniciarGozoLicencaEspecial", summary = "Iniciar gozo", description = "Executa o ato no órgão autorizado, validando estado e versão persistidos. A mesma chave e conteúdo recuperam o recibo original sem repetir efeitos.")
  @WorkflowAction(id = "iniciar-gozo-licenca-especial", title = "Iniciar gozo", scope = ActionScope.ITEM, requiredAuthorities = {"ERGON_WRITE"},
    allowedStates = {"HOMOLOGADA"}, confirmationRequired = true, idempotencyKey = ActionRequirement.REQUIRED,
    resourceVersion = ActionRequirement.REQUIRED, resourceVersionTransport = ActionResourceVersionTransport.IF_MATCH)
  public ResponseEntity<LicencaEspecialGozoResponseDTO> actionIniciarGozoLicencaEspecial(@PathVariable Long id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch, @RequestHeader("Idempotency-Key") String key,
      @jakarta.validation.Valid @RequestBody TempoLicencaTransicaoDTO dto) {
    return ErgonHttpContract.itemBody(executeVersionedItemCommand("iniciar-gozo-licenca-especial", id, ifMatch, key, dto,
        condition -> service.transition(id, "iniciar-gozo-licenca-especial", dto, condition)));
  }

  @PostMapping("/{id}/actions/interromper-licenca-especial") @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(operationId = "actionLicencaEspecialGozoInterromperLicencaEspecial", summary = "Interromper gozo", description = "Executa o ato no órgão autorizado, validando estado e versão persistidos. A mesma chave e conteúdo recuperam o recibo original sem repetir efeitos.")
  @WorkflowAction(id = "interromper-licenca-especial", title = "Interromper gozo", scope = ActionScope.ITEM, requiredAuthorities = {"ERGON_WRITE"},
    allowedStates = {"EM_GOZO"}, confirmationRequired = true, idempotencyKey = ActionRequirement.REQUIRED,
    resourceVersion = ActionRequirement.REQUIRED, resourceVersionTransport = ActionResourceVersionTransport.IF_MATCH)
  public ResponseEntity<LicencaEspecialGozoResponseDTO> actionInterromperLicencaEspecial(@PathVariable Long id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch, @RequestHeader("Idempotency-Key") String key,
      @jakarta.validation.Valid @RequestBody TempoLicencaTransicaoDTO dto) {
    return ErgonHttpContract.itemBody(executeVersionedItemCommand("interromper-licenca-especial", id, ifMatch, key, dto,
        condition -> service.transition(id, "interromper-licenca-especial", dto, condition)));
  }

  @PostMapping("/{id}/actions/encerrar-licenca-especial") @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(operationId = "actionLicencaEspecialGozoEncerrarLicencaEspecial", summary = "Encerrar gozo", description = "Executa o ato no órgão autorizado, validando estado e versão persistidos. A mesma chave e conteúdo recuperam o recibo original sem repetir efeitos.")
  @WorkflowAction(id = "encerrar-licenca-especial", title = "Encerrar gozo", scope = ActionScope.ITEM, requiredAuthorities = {"ERGON_WRITE"},
    allowedStates = {"EM_GOZO"}, confirmationRequired = true, idempotencyKey = ActionRequirement.REQUIRED,
    resourceVersion = ActionRequirement.REQUIRED, resourceVersionTransport = ActionResourceVersionTransport.IF_MATCH)
  public ResponseEntity<LicencaEspecialGozoResponseDTO> actionEncerrarLicencaEspecial(@PathVariable Long id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch, @RequestHeader("Idempotency-Key") String key,
      @jakarta.validation.Valid @RequestBody TempoLicencaTransicaoDTO dto) {
    return ErgonHttpContract.itemBody(executeVersionedItemCommand("encerrar-licenca-especial", id, ifMatch, key, dto,
        condition -> service.transition(id, "encerrar-licenca-especial", dto, condition)));
  }

  @PostMapping("/{id}/actions/cancelar-licenca-especial") @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(operationId = "actionLicencaEspecialGozoCancelarLicencaEspecial", summary = "Cancelar parcela", description = "Executa o ato no órgão autorizado, validando estado e versão persistidos. A mesma chave e conteúdo recuperam o recibo original sem repetir efeitos.")
  @WorkflowAction(id = "cancelar-licenca-especial", title = "Cancelar parcela", scope = ActionScope.ITEM, requiredAuthorities = {"ERGON_WRITE"},
    allowedStates = {"PROGRAMADA", "HOMOLOGADA"}, confirmationRequired = true, idempotencyKey = ActionRequirement.REQUIRED,
    resourceVersion = ActionRequirement.REQUIRED, resourceVersionTransport = ActionResourceVersionTransport.IF_MATCH)
  public ResponseEntity<LicencaEspecialGozoResponseDTO> actionCancelarLicencaEspecial(@PathVariable Long id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch, @RequestHeader("Idempotency-Key") String key,
      @jakarta.validation.Valid @RequestBody TempoLicencaTransicaoDTO dto) {
    return ErgonHttpContract.itemBody(executeVersionedItemCommand("cancelar-licenca-especial", id, ifMatch, key, dto,
        condition -> service.transition(id, "cancelar-licenca-especial", dto, condition)));
  }
  @Override
  @PostMapping("/bulk/update/evaluation")
  @Operation(
      operationId = "ergon.licencas-especiais-gozo.bulk.uniform.evaluation",
      summary = "Avaliar edição uniforme de assentamentos de tempo e licença",
      description =
          "Avalia seleção, versões e alterações de assentamentos selecionados sem modificar o cadastro; produz uma proposta protegida.")
  @BulkResourceOperation(BulkResourceOperation.Role.UNIFORM_EVALUATION)
  public org.springframework.http.ResponseEntity<RestApiResponse<BulkProposal>> evaluateBulkUpdate(@RequestBody BulkUniformEvaluationRequest<?, LicencaEspecialGozoFilterDTO> request) {
    return super.evaluateBulkUpdate(request);
  }

  @Override
  @PostMapping("/bulk/update-items/evaluation")
  @Operation(
      operationId = "ergon.licencas-especiais-gozo.bulk.items.evaluation",
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
      evaluationOperationId = "ergon.licencas-especiais-gozo.bulk.uniform.evaluation",
      atomicity = ActionCollectionAtomicity.PER_ITEM)
  @Operation(
      operationId = "ergon.licencas-especiais-gozo.bulk.uniform",
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
      evaluationOperationId = "ergon.licencas-especiais-gozo.bulk.items.evaluation",
      atomicity = ActionCollectionAtomicity.PER_ITEM)
  @Operation(
      operationId = "ergon.licencas-especiais-gozo.bulk.items",
      summary = "Confirmar edições por parcela de gozo de férias",
      description =
          "Aplica os deltas individualizados preservados na proposta, com versão e resultado próprios por parcela.")
  @BulkResourceOperation(BulkResourceOperation.Role.ITEM_CONFIRMATION)
  public org.springframework.http.ResponseEntity<RestApiResponse<BulkExecution>> confirmBulkUpdateItems(@RequestBody BulkConfirmationRequest request, @RequestHeader("Idempotency-Key") String key) {
    return super.confirmBulkUpdateItems(request, key);
  }











  @PostMapping("/bulk/homologar-licencas-especiais-lote/evaluation")
  @Operation(
      operationId = "ergon.licencas-especiais-gozo.homologar-licencas-especiais-lote.evaluation",
      summary = "Avaliar homologação de licenças",
      description =
          "Avalia o ato coletivo no mesmo órgão, sem persistir efeitos; cada alvo conserva sua versão e seu impedimento individual.")
  public org.springframework.http.ResponseEntity<RestApiResponse<BulkProposal>> bulkCommandEvaluate(
      
          @RequestBody
          BulkCommandEvaluationRequest<TempoLicencaTransicaoDTO, ?, LicencaEspecialGozoFilterDTO>
              request) {
    return evaluateBulkDomainCommand("homologar-licencas-especiais-lote", request);
  }

  @PostMapping("/actions/homologar-licencas-especiais-lote")
  @WorkflowAction(
      id = "homologar-licencas-especiais-lote",
      title = "Homologar licenças em lote",
      description =
          "Homologa parcelas programadas e sincroniza o espelho do abono sem duplicar o débito no período aquisitivo.",
      scope = ActionScope.COLLECTION,
      requiredAuthorities = {"ERGON_WRITE"},
      confirmationRequired = true,
      idempotencyKey = ActionRequirement.REQUIRED,
      atomicity = ActionCollectionAtomicity.PER_ITEM,
      outcomeMode = ActionOutcomeMode.PER_ITEM,
      maxSelection = 200)
  @BulkOperation(
      mode = BulkMode.DOMAIN_COMMAND,
      evaluationOperationId = "ergon.licencas-especiais-gozo.homologar-licencas-especiais-lote.evaluation",
      atomicity = ActionCollectionAtomicity.PER_ITEM)
  @Operation(
      operationId = "ergon.licencas-especiais-gozo.homologar-licencas-especiais-lote",
      summary = "Homologar licenças em lote",
      description = "Homologa parcelas programadas e sincroniza o espelho do abono sem duplicar o débito no período aquisitivo.")
  public org.springframework.http.ResponseEntity<RestApiResponse<BulkExecution>> bulkCommandConfirm(
      @RequestBody BulkConfirmationRequest request,
      @Parameter(description = "Chave estável do ato coletivo para impedir efeitos duplicados.")
          @RequestHeader("Idempotency-Key")
          String key) {
    return confirmBulkDomainCommand("homologar-licencas-especiais-lote", request, key);
  }

  @GetMapping("/{id}/vendas-originadas-gozo") @PreAuthorize("hasAuthority('ERGON_READ')")
  @Operation(operationId = "childrenLicencaEspecialGozoVendaLicencaEspecial", summary = "Pecúnia do abono", description = "Consulta paginada dos registros relacionados, após validar o pai no órgão atual, sem aceitar substituição do filtro de parentesco.")
  @UiSurface(id = "vendas-originadas-gozo", kind = SurfaceKind.READ_PROJECTION, scope = SurfaceScope.ITEM, title = "Pecúnia do abono", relatedChildResourceKey = ApiPaths.KEY_VENDAS_LICENCA_ESPECIAL, relatedChildResourcePath = ApiPaths.VENDAS_LICENCA_ESPECIAL, relatedChildParentField = "licencaEspecialGozoId", relatedChildOperations = {RelatedResourceChildOperation.FILTER, RelatedResourceChildOperation.LIST}, requiredAuthorities = {"ERGON_READ"})
  public ResponseEntity<RestApiResponse<org.springframework.data.domain.Page<VendaLicencaEspecialResponseDTO>>> childrenVendaLicencaEspecial(@PathVariable Long id,
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    ErgonHttpContract.childPage(page, size);
    return filterRelatedChildResource(id, vendaLicencaEspecialChildren, ApiPaths.VENDAS_LICENCA_ESPECIAL, VendaLicencaEspecialFilterDTO::new,
        (child, parent) -> { child.setLicencaEspecialGozoId(parent); }, page, size);
  }

  @org.springframework.beans.factory.annotation.Autowired private br.com.praxis.ergonshowcase.service.TempoLicencaPreviewService previews;
  @PostMapping("/determinations/preview") @PreAuthorize("hasAuthority('ERGON_READ')")
  @Operation(operationId = "determineLicencaEspecialGozo", summary = "Dias de gozo previstos", description = "Calcula prévia de criação sem persistir, reservar saldo ou autorizar o ato.")
  public TempoLicencaPrevia.Resultado preview(@jakarta.validation.Valid @RequestBody TempoLicencaPrevia.Gozo input) {return previews.gozo(input);}
  @Override @PostMapping
  @Operation(operationId = "createErgonLicencaEspecialGozo", summary = "Registrar assentamento", description = "Cria no estado inicial do domínio, revalidando vínculo, política e contabilidade no servidor.")
  public ResponseEntity<RestApiResponse<LicencaEspecialGozoResponseDTO>> create(@jakarta.validation.Valid @RequestBody CreateLicencaEspecialGozoDTO input) {return super.create(input);}
  @Override @SuppressWarnings({"unchecked", "rawtypes"})
  protected org.praxisplatform.uischema.bulk.JpaResourceBulkEngine getBulkEngine() { return bulk.engine(Resource.LICENCAS_ESPECIAIS_GOZO); }
  @Override protected String bulkProposalOperationId(UUID id) { return bulk.locate(Resource.LICENCAS_ESPECIAIS_GOZO, id, false); }
  @Override protected String bulkExecutionOperationId(UUID id) { return bulk.locate(Resource.LICENCAS_ESPECIAIS_GOZO, id, true); }

}
