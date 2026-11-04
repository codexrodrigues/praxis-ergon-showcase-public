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
import br.com.praxis.ergonshowcase.service.PeriodoAquisitivoLicencaEspecialService;
import io.swagger.v3.oas.annotations.Operation;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.controller.base.AbstractDuplicateDraftResourceController;
import org.praxisplatform.uischema.fact.ResourceFact;
import org.praxisplatform.uischema.surface.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@BulkResourceOperations(proposalOperationId = "ergon.periodos-aquisitivos-licenca-especial.bulk.proposal", proposalResultsOperationId = "ergon.periodos-aquisitivos-licenca-especial.bulk.proposal-results",
    executionOperationId = "ergon.periodos-aquisitivos-licenca-especial.bulk.execution", executionResultsOperationId = "ergon.periodos-aquisitivos-licenca-especial.bulk.results", cancelOperationId = "ergon.periodos-aquisitivos-licenca-especial.bulk.cancel",
    updateSourceOperationId = "updateErgonPeriodoAquisitivoLicencaEspecial", protectedUpdateFields = {"empCodigo", "numfunc", "numvinc", "chave", "situacao", "diasConvertidosDobro", "dtCntDobro", "averbacaoOrigemId"})
@ApiResource(value = ApiPaths.PERIODOS_AQUISITIVOS_LICENCA_ESPECIAL, resourceKey = "administracao-pessoal.periodos-aquisitivos-licenca-especial", title = "Períodos de licença especial", description = "Ciclos de aquisição, destinações de dias e parâmetros individuais de licença-prêmio ou especial.", icon = "date_range",
    identity = @ResourceIdentity(keyField = "id", titleField = "descricao", subtitleField = "pessoaNome", metadataFields = {"chave", "anosLicEsp", "mesesLicEspPer"}),
    quickFilters = {@QuickFilter(id = "saldo", label = "Com saldo disponível", filter = "{\"saldoAcima\":0}"), @QuickFilter(id = "prescricao", label = "Prescrição em até 90 dias", filter = "{\"prescricaoProximosDias\":90}"), @QuickFilter(id = "dobro", label = "Com conversão em dobro", filter = "{\"comContagemDobro\":true}")})
@ApiGroup(value = "ergon", title = "Administração de pessoal", description = "Assentamentos funcionais de órgãos públicos.", icon = "groups")
public class PeriodoAquisitivoLicencaEspecialController extends org.praxisplatform.uischema.controller.base.AbstractBulkGovernedDuplicateDraftResourceController<PeriodoAquisitivoLicencaEspecialResponseDTO, Long, PeriodoAquisitivoLicencaEspecialFilterDTO, CreatePeriodoAquisitivoLicencaEspecialDTO, UpdatePeriodoAquisitivoLicencaEspecialDTO, PeriodoAquisitivoLicencaEspecialDuplicateDraftDTO> {
  @org.springframework.beans.factory.annotation.Autowired private ErgonBulkOperations bulk;
  @org.springframework.beans.factory.annotation.Autowired private br.com.praxis.ergonshowcase.service.LicencaEspecialGozoService licencaEspecialGozoChildren;
  @org.springframework.beans.factory.annotation.Autowired private br.com.praxis.ergonshowcase.service.VendaLicencaEspecialService vendaLicencaEspecialChildren;
  private final PeriodoAquisitivoLicencaEspecialService service;
  public PeriodoAquisitivoLicencaEspecialController(PeriodoAquisitivoLicencaEspecialService service) { this.service = service; }
  @Override protected PeriodoAquisitivoLicencaEspecialService getService() { return service; }
  @Override protected Long getResponseId(PeriodoAquisitivoLicencaEspecialResponseDTO dto) { return dto.getId(); }
  @GetMapping("/{id}/detail") @PreAuthorize("hasAuthority('ERGON_READ')")
  @Operation(summary = "Inspecionar ciclo de licença especial", description = "Consulta integral do direito, saldo e parâmetros individuais do período aquisitivo.")
  @UiSurface(id = "detail", kind = SurfaceKind.READ_PROJECTION, scope = SurfaceScope.ITEM, title = "Ciclo de licença especial", description = "Direito, destinações de dias, origem e overrides individuais do ciclo.", requiredAuthorities = {"ERGON_READ"})
  public ResponseEntity<PeriodoAquisitivoLicencaEspecialResponseDTO> detail(@PathVariable Long id) { return withResourceVersion(ResponseEntity.ok(), id, service.findById(id)); }

  @Override @PostMapping("/stats/group-by")
  @Operation(operationId = "statsPeriodoAquisitivoLicencaEspecial", summary = "Distribuição e ranking do direito", description = "Agrega somente registros autorizados pelo filtro oficial; buckets nominais abrem a ficha 360° pela identidade persistida.")
  @UiAnalytics(projections = {
    @AnalyticsProjection(id = "situacoes", intent = AnalyticsIntent.COMPOSITION, sourceOperation = AnalyticsOperation.GROUP_BY,
      primaryDimension = @AnalyticsDimensionBinding(field = "situacao"), primaryMetrics = @AnalyticsMetricBinding(field = "count", aggregation = "count", label = "Registros")),
    @AnalyticsProjection(id = "dias-por-registro", intent = AnalyticsIntent.RANKING, sourceOperation = AnalyticsOperation.GROUP_BY,
      primaryDimension = @AnalyticsDimensionBinding(field = "id"), primaryMetrics = @AnalyticsMetricBinding(field = "saldoDias", aggregation = "sum", label = "Dias"),
      recordOpen = @AnalyticsRecordOpen(sourceIdentityField = "key", target = @AnalyticsSurfaceTarget(resourceKey = "administracao-pessoal.periodos-aquisitivos-licenca-especial", surfaceId = "detail")))
  })
  public ResponseEntity<org.praxisplatform.uischema.rest.response.RestApiResponse<org.praxisplatform.uischema.stats.dto.GroupByStatsResponse>> groupByStats(
      @RequestBody org.praxisplatform.uischema.stats.dto.GroupByStatsRequest<PeriodoAquisitivoLicencaEspecialFilterDTO> request) { return super.groupByStats(request); }

  @PatchMapping("/{id}/parametrizar-regras-individuais-licenca-especial") @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(operationId = "patchPeriodoAquisitivoLicEspRegrasIndividuaisPatchDTO", summary = "Política individual da licença", description = "Atualiza somente campos explicitamente enviados, sob lock, no órgão autorizado e com If-Match obrigatório.")
  @ResourceIntent(id = "parametrizar-regras-individuais-licenca-especial", title = "Política individual da licença")
  @UiSurface(id = "parametrizar-regras-individuais-licenca-especial", kind = SurfaceKind.PARTIAL_FORM, scope = SurfaceScope.ITEM, title = "Política individual da licença", requiredAuthorities = {"ERGON_WRITE"})
  public ResponseEntity<PeriodoAquisitivoLicencaEspecialResponseDTO> patchPeriodoAquisitivoLicEspRegrasIndividuaisPatchDTO(@PathVariable Long id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch, @jakarta.validation.Valid @RequestBody PeriodoAquisitivoLicEspRegrasIndividuaisPatchDTO dto) {
    return ErgonHttpContract.itemBody(executeVersionedItemPatch("parametrizar-regras-individuais-licenca-especial", id, ifMatch, condition -> service.patch(id, dto, condition)));
  }

  @PatchMapping("/{id}/ajustar-prescricao-governanca-licenca-especial") @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(operationId = "patchPeriodoAquisitivoLicEspPrescricaoPatchDTO", summary = "Prescrição e lastro legal", description = "Atualiza somente campos explicitamente enviados, sob lock, no órgão autorizado e com If-Match obrigatório.")
  @ResourceIntent(id = "ajustar-prescricao-governanca-licenca-especial", title = "Prescrição e lastro legal")
  @UiSurface(id = "ajustar-prescricao-governanca-licenca-especial", kind = SurfaceKind.PARTIAL_FORM, scope = SurfaceScope.ITEM, title = "Prescrição e lastro legal", requiredAuthorities = {"ERGON_WRITE"})
  public ResponseEntity<PeriodoAquisitivoLicencaEspecialResponseDTO> patchPeriodoAquisitivoLicEspPrescricaoPatchDTO(@PathVariable Long id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch, @jakarta.validation.Valid @RequestBody PeriodoAquisitivoLicEspPrescricaoPatchDTO dto) {
    return ErgonHttpContract.itemBody(executeVersionedItemPatch("ajustar-prescricao-governanca-licenca-especial", id, ifMatch, condition -> service.patch(id, dto, condition)));
  }

  @Override @PutMapping("/{id}") @BulkResourceOperation(BulkResourceOperation.Role.UPDATE_SOURCE)
  @Operation(operationId = "updateErgonPeriodoAquisitivoLicencaEspecial", summary = "Retificar assentamento", description = "Mantém os dados cadastrais preservando a identidade e o estado de ciclo de vida, com If-Match obrigatório.")
  public ResponseEntity<RestApiResponse<PeriodoAquisitivoLicencaEspecialResponseDTO>> update(@PathVariable Long id, @jakarta.validation.Valid @RequestBody UpdatePeriodoAquisitivoLicencaEspecialDTO dto) { return super.update(id,dto); }

  @PostMapping("/{id}/actions/homologar-periodo-licenca-especial") @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(operationId = "actionPeriodoAquisitivoLicencaEspecialHomologarPeriodoLicencaEspecial", summary = "Homologar período", description = "Executa o ato no órgão autorizado, validando estado e versão persistidos. A mesma chave e conteúdo recuperam o recibo original sem repetir efeitos.")
  @WorkflowAction(id = "homologar-periodo-licenca-especial", title = "Homologar período", scope = ActionScope.ITEM, requiredAuthorities = {"ERGON_WRITE"},
    allowedStates = {"EM_AQUISICAO", "SUSPENSO"}, confirmationRequired = true, idempotencyKey = ActionRequirement.REQUIRED,
    resourceVersion = ActionRequirement.REQUIRED, resourceVersionTransport = ActionResourceVersionTransport.IF_MATCH)
  public ResponseEntity<PeriodoAquisitivoLicencaEspecialResponseDTO> actionHomologarPeriodoLicencaEspecial(@PathVariable Long id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch, @RequestHeader("Idempotency-Key") String key,
      @jakarta.validation.Valid @RequestBody TempoLicencaTransicaoDTO dto) {
    return ErgonHttpContract.itemBody(executeVersionedItemCommand("homologar-periodo-licenca-especial", id, ifMatch, key, dto,
        condition -> service.transition(id, "homologar-periodo-licenca-especial", dto, condition)));
  }

  @PostMapping("/{id}/actions/suspender-periodo-licenca-especial") @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(operationId = "actionPeriodoAquisitivoLicencaEspecialSuspenderPeriodoLicencaEspecial", summary = "Suspender período", description = "Executa o ato no órgão autorizado, validando estado e versão persistidos. A mesma chave e conteúdo recuperam o recibo original sem repetir efeitos.")
  @WorkflowAction(id = "suspender-periodo-licenca-especial", title = "Suspender período", scope = ActionScope.ITEM, requiredAuthorities = {"ERGON_WRITE"},
    allowedStates = {"EM_AQUISICAO", "DISPONIVEL", "PARCIALMENTE_GOZADO"}, confirmationRequired = true, idempotencyKey = ActionRequirement.REQUIRED,
    resourceVersion = ActionRequirement.REQUIRED, resourceVersionTransport = ActionResourceVersionTransport.IF_MATCH)
  public ResponseEntity<PeriodoAquisitivoLicencaEspecialResponseDTO> actionSuspenderPeriodoLicencaEspecial(@PathVariable Long id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch, @RequestHeader("Idempotency-Key") String key,
      @jakarta.validation.Valid @RequestBody TempoLicencaTransicaoDTO dto) {
    return ErgonHttpContract.itemBody(executeVersionedItemCommand("suspender-periodo-licenca-especial", id, ifMatch, key, dto,
        condition -> service.transition(id, "suspender-periodo-licenca-especial", dto, condition)));
  }

  @PostMapping("/{id}/actions/cancelar-periodo-licenca-especial") @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(operationId = "actionPeriodoAquisitivoLicencaEspecialCancelarPeriodoLicencaEspecial", summary = "Cancelar período", description = "Executa o ato no órgão autorizado, validando estado e versão persistidos. A mesma chave e conteúdo recuperam o recibo original sem repetir efeitos.")
  @WorkflowAction(id = "cancelar-periodo-licenca-especial", title = "Cancelar período", scope = ActionScope.ITEM, requiredAuthorities = {"ERGON_WRITE"},
    allowedStates = {"EM_AQUISICAO", "DISPONIVEL", "SUSPENSO"}, confirmationRequired = true, idempotencyKey = ActionRequirement.REQUIRED,
    resourceVersion = ActionRequirement.REQUIRED, resourceVersionTransport = ActionResourceVersionTransport.IF_MATCH)
  public ResponseEntity<PeriodoAquisitivoLicencaEspecialResponseDTO> actionCancelarPeriodoLicencaEspecial(@PathVariable Long id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch, @RequestHeader("Idempotency-Key") String key,
      @jakarta.validation.Valid @RequestBody TempoLicencaTransicaoDTO dto) {
    return ErgonHttpContract.itemBody(executeVersionedItemCommand("cancelar-periodo-licenca-especial", id, ifMatch, key, dto,
        condition -> service.transition(id, "cancelar-periodo-licenca-especial", dto, condition)));
  }

  @PostMapping("/{id}/actions/converter-em-dobro") @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(operationId = "actionPeriodoAquisitivoLicencaEspecialConverterEmDobro", summary = "Converter em dobro", description = "Executa o ato no órgão autorizado, validando estado e versão persistidos. A mesma chave e conteúdo recuperam o recibo original sem repetir efeitos.")
  @WorkflowAction(id = "converter-em-dobro", title = "Converter em dobro", scope = ActionScope.ITEM, requiredAuthorities = {"ERGON_WRITE"},
    allowedStates = {"DISPONIVEL", "PARCIALMENTE_GOZADO"}, confirmationRequired = true, idempotencyKey = ActionRequirement.REQUIRED,
    resourceVersion = ActionRequirement.REQUIRED, resourceVersionTransport = ActionResourceVersionTransport.IF_MATCH)
  public ResponseEntity<PeriodoAquisitivoLicencaEspecialResponseDTO> actionConverterEmDobro(@PathVariable Long id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch, @RequestHeader("Idempotency-Key") String key,
      @jakarta.validation.Valid @RequestBody TempoLicencaTransicaoDTO dto) {
    return ErgonHttpContract.itemBody(executeVersionedItemCommand("converter-em-dobro", id, ifMatch, key, dto,
        condition -> service.transition(id, "converter-em-dobro", dto, condition)));
  }

  @PostMapping("/{id}/actions/reverter-conversao-dobro") @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(operationId = "actionPeriodoAquisitivoLicencaEspecialReverterConversaoDobro", summary = "Reverter conversão em dobro", description = "Executa o ato no órgão autorizado, validando estado e versão persistidos. A mesma chave e conteúdo recuperam o recibo original sem repetir efeitos.")
  @WorkflowAction(id = "reverter-conversao-dobro", title = "Reverter conversão em dobro", scope = ActionScope.ITEM, requiredAuthorities = {"ERGON_WRITE"},
    allowedStates = {"CONVERTIDO_EM_DOBRO", "PARCIALMENTE_GOZADO", "QUITADO"}, confirmationRequired = true, idempotencyKey = ActionRequirement.REQUIRED,
    resourceVersion = ActionRequirement.REQUIRED, resourceVersionTransport = ActionResourceVersionTransport.IF_MATCH)
  public ResponseEntity<PeriodoAquisitivoLicencaEspecialResponseDTO> actionReverterConversaoDobro(@PathVariable Long id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch, @RequestHeader("Idempotency-Key") String key,
      @jakarta.validation.Valid @RequestBody TempoLicencaTransicaoDTO dto) {
    return ErgonHttpContract.itemBody(executeVersionedItemCommand("reverter-conversao-dobro", id, ifMatch, key, dto,
        condition -> service.transition(id, "reverter-conversao-dobro", dto, condition)));
  }

  @PostMapping("/{id}/actions/prescrever-periodo-licenca-especial") @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(operationId = "actionPeriodoAquisitivoLicencaEspecialPrescreverPeriodoLicencaEspecial", summary = "Prescrever período", description = "Executa o ato no órgão autorizado, validando estado e versão persistidos. A mesma chave e conteúdo recuperam o recibo original sem repetir efeitos.")
  @WorkflowAction(id = "prescrever-periodo-licenca-especial", title = "Prescrever período", scope = ActionScope.ITEM, requiredAuthorities = {"ERGON_WRITE"},
    allowedStates = {"DISPONIVEL", "PARCIALMENTE_GOZADO"}, confirmationRequired = true, idempotencyKey = ActionRequirement.REQUIRED,
    resourceVersion = ActionRequirement.REQUIRED, resourceVersionTransport = ActionResourceVersionTransport.IF_MATCH)
  public ResponseEntity<PeriodoAquisitivoLicencaEspecialResponseDTO> actionPrescreverPeriodoLicencaEspecial(@PathVariable Long id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch, @RequestHeader("Idempotency-Key") String key,
      @jakarta.validation.Valid @RequestBody TempoLicencaTransicaoDTO dto) {
    return ErgonHttpContract.itemBody(executeVersionedItemCommand("prescrever-periodo-licenca-especial", id, ifMatch, key, dto,
        condition -> service.transition(id, "prescrever-periodo-licenca-especial", dto, condition)));
  }

  @PostMapping("/{id}/actions/recalcular-saldo-licenca-especial") @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(operationId = "actionPeriodoAquisitivoLicencaEspecialRecalcularSaldoLicencaEspecial", summary = "Reconciliar saldo", description = "Executa o ato no órgão autorizado, validando estado e versão persistidos. A mesma chave e conteúdo recuperam o recibo original sem repetir efeitos.")
  @WorkflowAction(id = "recalcular-saldo-licenca-especial", title = "Reconciliar saldo", scope = ActionScope.ITEM, requiredAuthorities = {"ERGON_WRITE"},
    allowedStates = {"EM_AQUISICAO", "DISPONIVEL", "PARCIALMENTE_GOZADO", "CONVERTIDO_EM_DOBRO", "QUITADO", "SUSPENSO"}, confirmationRequired = true, idempotencyKey = ActionRequirement.REQUIRED,
    resourceVersion = ActionRequirement.REQUIRED, resourceVersionTransport = ActionResourceVersionTransport.IF_MATCH)
  public ResponseEntity<PeriodoAquisitivoLicencaEspecialResponseDTO> actionRecalcularSaldoLicencaEspecial(@PathVariable Long id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch, @RequestHeader("Idempotency-Key") String key,
      @jakarta.validation.Valid @RequestBody TempoLicencaTransicaoDTO dto) {
    return ErgonHttpContract.itemBody(executeVersionedItemCommand("recalcular-saldo-licenca-especial", id, ifMatch, key, dto,
        condition -> service.transition(id, "recalcular-saldo-licenca-especial", dto, condition)));
  }
  @Override
  @PostMapping("/bulk/update/evaluation")
  @Operation(
      operationId = "ergon.periodos-aquisitivos-licenca-especial.bulk.uniform.evaluation",
      summary = "Avaliar edição uniforme de assentamentos de tempo e licença",
      description =
          "Avalia seleção, versões e alterações de assentamentos selecionados sem modificar o cadastro; produz uma proposta protegida.")
  @BulkResourceOperation(BulkResourceOperation.Role.UNIFORM_EVALUATION)
  public org.springframework.http.ResponseEntity<RestApiResponse<BulkProposal>> evaluateBulkUpdate(@RequestBody BulkUniformEvaluationRequest<?, PeriodoAquisitivoLicencaEspecialFilterDTO> request) {
    return super.evaluateBulkUpdate(request);
  }

  @Override
  @PostMapping("/bulk/update-items/evaluation")
  @Operation(
      operationId = "ergon.periodos-aquisitivos-licenca-especial.bulk.items.evaluation",
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
      evaluationOperationId = "ergon.periodos-aquisitivos-licenca-especial.bulk.uniform.evaluation",
      atomicity = ActionCollectionAtomicity.PER_ITEM)
  @Operation(
      operationId = "ergon.periodos-aquisitivos-licenca-especial.bulk.uniform",
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
      evaluationOperationId = "ergon.periodos-aquisitivos-licenca-especial.bulk.items.evaluation",
      atomicity = ActionCollectionAtomicity.PER_ITEM)
  @Operation(
      operationId = "ergon.periodos-aquisitivos-licenca-especial.bulk.items",
      summary = "Confirmar edições por parcela de gozo de férias",
      description =
          "Aplica os deltas individualizados preservados na proposta, com versão e resultado próprios por parcela.")
  @BulkResourceOperation(BulkResourceOperation.Role.ITEM_CONFIRMATION)
  public org.springframework.http.ResponseEntity<RestApiResponse<BulkExecution>> confirmBulkUpdateItems(@RequestBody BulkConfirmationRequest request, @RequestHeader("Idempotency-Key") String key) {
    return super.confirmBulkUpdateItems(request, key);
  }











  @PostMapping("/bulk/recalcular-saldos-licenca-especial-lote/evaluation")
  @Operation(
      operationId = "ergon.periodos-aquisitivos-licenca-especial.recalcular-saldos-licenca-especial-lote.evaluation",
      summary = "Avaliar recálculo de saldos",
      description =
          "Avalia o ato coletivo no mesmo órgão, sem persistir efeitos; cada alvo conserva sua versão e seu impedimento individual.")
  public org.springframework.http.ResponseEntity<RestApiResponse<BulkProposal>> bulkCommandEvaluate(
      
          @RequestBody
          BulkCommandEvaluationRequest<TempoLicencaTransicaoDTO, ?, PeriodoAquisitivoLicencaEspecialFilterDTO>
              request) {
    return evaluateBulkDomainCommand("recalcular-saldos-licenca-especial-lote", request);
  }

  @PostMapping("/actions/recalcular-saldos-licenca-especial-lote")
  @WorkflowAction(
      id = "recalcular-saldos-licenca-especial-lote",
      title = "Reconciliar saldos em lote",
      description =
          "Recalcula gozo, vendas, abonos, conversão e saldo a partir das parcelas persistidas, sem aceitar contabilidade fornecida pelo cliente.",
      scope = ActionScope.COLLECTION,
      requiredAuthorities = {"ERGON_WRITE"},
      confirmationRequired = true,
      idempotencyKey = ActionRequirement.REQUIRED,
      atomicity = ActionCollectionAtomicity.PER_ITEM,
      outcomeMode = ActionOutcomeMode.PER_ITEM,
      maxSelection = 200)
  @BulkOperation(
      mode = BulkMode.DOMAIN_COMMAND,
      evaluationOperationId = "ergon.periodos-aquisitivos-licenca-especial.recalcular-saldos-licenca-especial-lote.evaluation",
      atomicity = ActionCollectionAtomicity.PER_ITEM)
  @Operation(
      operationId = "ergon.periodos-aquisitivos-licenca-especial.recalcular-saldos-licenca-especial-lote",
      summary = "Reconciliar saldos em lote",
      description = "Recalcula gozo, vendas, abonos, conversão e saldo a partir das parcelas persistidas, sem aceitar contabilidade fornecida pelo cliente.")
  public org.springframework.http.ResponseEntity<RestApiResponse<BulkExecution>> bulkCommandConfirm(
      @RequestBody BulkConfirmationRequest request,
      @Parameter(description = "Chave estável do ato coletivo para impedir efeitos duplicados.")
          @RequestHeader("Idempotency-Key")
          String key) {
    return confirmBulkDomainCommand("recalcular-saldos-licenca-especial-lote", request, key);
  }

  @GetMapping("/{id}/parcelas-gozo-licenca-especial") @PreAuthorize("hasAuthority('ERGON_READ')")
  @Operation(operationId = "childrenPeriodoAquisitivoLicencaEspecialLicencaEspecialGozo", summary = "Parcelas de gozo", description = "Consulta paginada dos registros relacionados, após validar o pai no órgão atual, sem aceitar substituição do filtro de parentesco.")
  @UiSurface(id = "parcelas-gozo-licenca-especial", kind = SurfaceKind.READ_PROJECTION, scope = SurfaceScope.ITEM, title = "Parcelas de gozo", relatedChildResourceKey = ApiPaths.KEY_LICENCAS_ESPECIAIS_GOZO, relatedChildResourcePath = ApiPaths.LICENCAS_ESPECIAIS_GOZO, relatedChildParentField = "periodoAquisitivoLicencaId", relatedChildOperations = {RelatedResourceChildOperation.FILTER, RelatedResourceChildOperation.LIST}, requiredAuthorities = {"ERGON_READ"})
  public ResponseEntity<RestApiResponse<org.springframework.data.domain.Page<LicencaEspecialGozoResponseDTO>>> childrenLicencaEspecialGozo(@PathVariable Long id,
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    ErgonHttpContract.childPage(page, size);
    return filterRelatedChildResource(id, licencaEspecialGozoChildren, ApiPaths.LICENCAS_ESPECIAIS_GOZO, LicencaEspecialGozoFilterDTO::new,
        (child, parent) -> { child.setPeriodoAquisitivoLicencaId(parent); }, page, size);
  }

  @GetMapping("/{id}/vendas-pecunia-licenca-especial") @PreAuthorize("hasAuthority('ERGON_READ')")
  @Operation(operationId = "childrenPeriodoAquisitivoLicencaEspecialVendaLicencaEspecial", summary = "Vendas e abonos", description = "Consulta paginada dos registros relacionados, após validar o pai no órgão atual, sem aceitar substituição do filtro de parentesco.")
  @UiSurface(id = "vendas-pecunia-licenca-especial", kind = SurfaceKind.READ_PROJECTION, scope = SurfaceScope.ITEM, title = "Vendas e abonos", relatedChildResourceKey = ApiPaths.KEY_VENDAS_LICENCA_ESPECIAL, relatedChildResourcePath = ApiPaths.VENDAS_LICENCA_ESPECIAL, relatedChildParentField = "periodoAquisitivoLicencaId", relatedChildOperations = {RelatedResourceChildOperation.FILTER, RelatedResourceChildOperation.LIST}, requiredAuthorities = {"ERGON_READ"})
  public ResponseEntity<RestApiResponse<org.springframework.data.domain.Page<VendaLicencaEspecialResponseDTO>>> childrenVendaLicencaEspecial(@PathVariable Long id,
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    ErgonHttpContract.childPage(page, size);
    return filterRelatedChildResource(id, vendaLicencaEspecialChildren, ApiPaths.VENDAS_LICENCA_ESPECIAL, VendaLicencaEspecialFilterDTO::new,
        (child, parent) -> { child.setPeriodoAquisitivoLicencaId(parent); }, page, size);
  }

  @org.springframework.beans.factory.annotation.Autowired private br.com.praxis.ergonshowcase.service.TempoLicencaPreviewService previews;
  @PostMapping("/determinations/preview") @PreAuthorize("hasAuthority('ERGON_READ')")
  @Operation(operationId = "determinePeriodoAquisitivoLicencaEspecial", summary = "Saldo inicial previsto", description = "Calcula prévia de criação sem persistir, reservar saldo ou autorizar o ato.")
  public TempoLicencaPrevia.Resultado preview(@jakarta.validation.Valid @RequestBody TempoLicencaPrevia.Periodo input) {return previews.periodo(input);}
  @Override @PostMapping
  @Operation(operationId = "createErgonPeriodoAquisitivoLicencaEspecial", summary = "Registrar assentamento", description = "Cria no estado inicial do domínio, revalidando vínculo, política e contabilidade no servidor.")
  public ResponseEntity<RestApiResponse<PeriodoAquisitivoLicencaEspecialResponseDTO>> create(@jakarta.validation.Valid @RequestBody CreatePeriodoAquisitivoLicencaEspecialDTO input) {return super.create(input);}
  @Override @SuppressWarnings({"unchecked", "rawtypes"})
  protected org.praxisplatform.uischema.bulk.JpaResourceBulkEngine getBulkEngine() { return bulk.engine(Resource.PERIODOS_AQUISITIVOS_LICENCA_ESPECIAL); }
  @Override protected String bulkProposalOperationId(UUID id) { return bulk.locate(Resource.PERIODOS_AQUISITIVOS_LICENCA_ESPECIAL, id, false); }
  @Override protected String bulkExecutionOperationId(UUID id) { return bulk.locate(Resource.PERIODOS_AQUISITIVOS_LICENCA_ESPECIAL, id, true); }

}
