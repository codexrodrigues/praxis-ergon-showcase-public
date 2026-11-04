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
import br.com.praxis.ergonshowcase.service.AverbacaoTempoServicoService;
import io.swagger.v3.oas.annotations.Operation;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.controller.base.AbstractDuplicateDraftResourceController;
import org.praxisplatform.uischema.fact.ResourceFact;
import org.praxisplatform.uischema.surface.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@BulkResourceOperations(proposalOperationId = "ergon.averbacoes-tempo-servico.bulk.proposal", proposalResultsOperationId = "ergon.averbacoes-tempo-servico.bulk.proposal-results",
    executionOperationId = "ergon.averbacoes-tempo-servico.bulk.execution", executionResultsOperationId = "ergon.averbacoes-tempo-servico.bulk.results", cancelOperationId = "ergon.averbacoes-tempo-servico.bulk.cancel",
    updateSourceOperationId = "updateErgonAverbacaoTempoServico", protectedUpdateFields = {"empCodigo", "numfunc", "numvinc", "chave", "situacao", "diasDesaverbados", "dataDesaverbacao", "motivoDesaverbacao", "dataRemocao", "motivoRemocao"})
@ApiResource(value = ApiPaths.AVERBACOES_TEMPO_SERVICO, resourceKey = "administracao-pessoal.averbacoes-tempo-servico", title = "Averbações de tempo de serviço", description = "Reconhecimento de serviço anterior por finalidade, com tempo certificado e desaverbação parcial consolidada.", icon = "history_edu",
    identity = @ResourceIdentity(keyField = "id", titleField = "descricao", subtitleField = "pessoaNome", metadataFields = {"instituicao", "finalidade"}),
    quickFilters = {@QuickFilter(id = "publico", label = "Tempo público", filter = "{\"tipoTempo\":\"PUBLICO\"}"), @QuickFilter(id = "rgps", label = "Tempo RGPS", filter = "{\"tipoTempo\":\"RGPS\"}"), @QuickFilter(id = "desaverbados", label = "Desaverbação parcial", filter = "{\"diasDesaverbadosAcima\":0}")})
@ApiGroup(value = "ergon", title = "Administração de pessoal", description = "Assentamentos funcionais de órgãos públicos.", icon = "groups")
public class AverbacaoTempoServicoController extends org.praxisplatform.uischema.controller.base.AbstractBulkGovernedDuplicateDraftResourceController<AverbacaoTempoServicoResponseDTO, Long, AverbacaoTempoServicoFilterDTO, CreateAverbacaoTempoServicoDTO, UpdateAverbacaoTempoServicoDTO, AverbacaoTempoServicoDuplicateDraftDTO> {
  @org.springframework.beans.factory.annotation.Autowired private ErgonBulkOperations bulk;
  @org.springframework.beans.factory.annotation.Autowired private br.com.praxis.ergonshowcase.service.PeriodoAquisitivoLicencaEspecialService periodoAquisitivoLicencaEspecialChildren;
  private final AverbacaoTempoServicoService service;
  public AverbacaoTempoServicoController(AverbacaoTempoServicoService service) { this.service = service; }
  @Override protected AverbacaoTempoServicoService getService() { return service; }
  @Override protected Long getResponseId(AverbacaoTempoServicoResponseDTO dto) { return dto.getId(); }
  @GetMapping("/{id}/detail") @PreAuthorize("hasAuthority('ERGON_READ')")
  @Operation(summary = "Inspecionar averbação de tempo", description = "Retorna o assentamento completo e seu lastro para inspeção em drawer.")
  @UiSurface(id = "detail", kind = SurfaceKind.READ_PROJECTION, scope = SurfaceScope.ITEM, title = "Averbação de tempo", description = "Certidão, finalidade, descontos e lastro documental do tempo reconhecido.", requiredAuthorities = {"ERGON_READ"})
  public ResponseEntity<AverbacaoTempoServicoResponseDTO> detail(@PathVariable Long id) { return withResourceVersion(ResponseEntity.ok(), id, service.findById(id)); }

  @Override @PostMapping("/stats/group-by")
  @Operation(operationId = "statsAverbacaoTempoServico", summary = "Distribuição e ranking do direito", description = "Agrega somente registros autorizados pelo filtro oficial; buckets nominais abrem a ficha 360° pela identidade persistida.")
  @UiAnalytics(projections = {
    @AnalyticsProjection(id = "situacoes", intent = AnalyticsIntent.COMPOSITION, sourceOperation = AnalyticsOperation.GROUP_BY,
      primaryDimension = @AnalyticsDimensionBinding(field = "situacao"), primaryMetrics = @AnalyticsMetricBinding(field = "count", aggregation = "count", label = "Registros")),
    @AnalyticsProjection(id = "dias-por-registro", intent = AnalyticsIntent.RANKING, sourceOperation = AnalyticsOperation.GROUP_BY,
      primaryDimension = @AnalyticsDimensionBinding(field = "id"), primaryMetrics = @AnalyticsMetricBinding(field = "totalDias", aggregation = "sum", label = "Dias"),
      recordOpen = @AnalyticsRecordOpen(sourceIdentityField = "key", target = @AnalyticsSurfaceTarget(resourceKey = "administracao-pessoal.averbacoes-tempo-servico", surfaceId = "detail")))
  })
  public ResponseEntity<org.praxisplatform.uischema.rest.response.RestApiResponse<org.praxisplatform.uischema.stats.dto.GroupByStatsResponse>> groupByStats(
      @RequestBody org.praxisplatform.uischema.stats.dto.GroupByStatsRequest<AverbacaoTempoServicoFilterDTO> request) { return super.groupByStats(request); }

  @PatchMapping("/{id}/atualizar-certidao-finalidades") @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(operationId = "patchAverbacaoCertidaoFinalidadesPatchDTO", summary = "Certidão e finalidades", description = "Atualiza somente campos explicitamente enviados, sob lock, no órgão autorizado e com If-Match obrigatório.")
  @ResourceIntent(id = "atualizar-certidao-finalidades", title = "Certidão e finalidades")
  @UiSurface(id = "atualizar-certidao-finalidades", kind = SurfaceKind.PARTIAL_FORM, scope = SurfaceScope.ITEM, title = "Certidão e finalidades", requiredAuthorities = {"ERGON_WRITE"})
  public ResponseEntity<AverbacaoTempoServicoResponseDTO> patchAverbacaoCertidaoFinalidadesPatchDTO(@PathVariable Long id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch, @jakarta.validation.Valid @RequestBody AverbacaoCertidaoFinalidadesPatchDTO dto) {
    return ErgonHttpContract.itemBody(executeVersionedItemPatch("atualizar-certidao-finalidades", id, ifMatch, condition -> service.patch(id, dto, condition)));
  }

  @PatchMapping("/{id}/atualizar-observacoes-governanca") @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(operationId = "patchAverbacaoGovernancaPatchDTO", summary = "Documentos e observações", description = "Atualiza somente campos explicitamente enviados, sob lock, no órgão autorizado e com If-Match obrigatório.")
  @ResourceIntent(id = "atualizar-observacoes-governanca", title = "Documentos e observações")
  @UiSurface(id = "atualizar-observacoes-governanca", kind = SurfaceKind.PARTIAL_FORM, scope = SurfaceScope.ITEM, title = "Documentos e observações", requiredAuthorities = {"ERGON_WRITE"})
  public ResponseEntity<AverbacaoTempoServicoResponseDTO> patchAverbacaoGovernancaPatchDTO(@PathVariable Long id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch, @jakarta.validation.Valid @RequestBody AverbacaoGovernancaPatchDTO dto) {
    return ErgonHttpContract.itemBody(executeVersionedItemPatch("atualizar-observacoes-governanca", id, ifMatch, condition -> service.patch(id, dto, condition)));
  }

  @Override @PutMapping("/{id}") @BulkResourceOperation(BulkResourceOperation.Role.UPDATE_SOURCE)
  @Operation(operationId = "updateErgonAverbacaoTempoServico", summary = "Retificar assentamento", description = "Mantém os dados cadastrais preservando a identidade e o estado de ciclo de vida, com If-Match obrigatório.")
  public ResponseEntity<RestApiResponse<AverbacaoTempoServicoResponseDTO>> update(@PathVariable Long id, @jakarta.validation.Valid @RequestBody UpdateAverbacaoTempoServicoDTO dto) { return super.update(id,dto); }

  @PostMapping("/{id}/actions/homologar-averbacao") @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(operationId = "actionAverbacaoTempoServicoHomologarAverbacao", summary = "Homologar averbação", description = "Executa o ato no órgão autorizado, validando estado e versão persistidos. A mesma chave e conteúdo recuperam o recibo original sem repetir efeitos.")
  @WorkflowAction(id = "homologar-averbacao", title = "Homologar averbação", scope = ActionScope.ITEM, requiredAuthorities = {"ERGON_WRITE"},
    allowedStates = {"EM_ANALISE"}, confirmationRequired = true, idempotencyKey = ActionRequirement.REQUIRED,
    resourceVersion = ActionRequirement.REQUIRED, resourceVersionTransport = ActionResourceVersionTransport.IF_MATCH)
  public ResponseEntity<AverbacaoTempoServicoResponseDTO> actionHomologarAverbacao(@PathVariable Long id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch, @RequestHeader("Idempotency-Key") String key,
      @jakarta.validation.Valid @RequestBody TempoLicencaTransicaoDTO dto) {
    return ErgonHttpContract.itemBody(executeVersionedItemCommand("homologar-averbacao", id, ifMatch, key, dto,
        condition -> service.transition(id, "homologar-averbacao", dto, condition)));
  }

  @PostMapping("/{id}/actions/registrar-desaverbacao") @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(operationId = "actionAverbacaoTempoServicoRegistrarDesaverbacao", summary = "Registrar desaverbação", description = "Executa o ato no órgão autorizado, validando estado e versão persistidos. A mesma chave e conteúdo recuperam o recibo original sem repetir efeitos.")
  @WorkflowAction(id = "registrar-desaverbacao", title = "Registrar desaverbação", scope = ActionScope.ITEM, requiredAuthorities = {"ERGON_WRITE"},
    allowedStates = {"HOMOLOGADA", "PARCIALMENTE_DESAVERBADA"}, confirmationRequired = true, idempotencyKey = ActionRequirement.REQUIRED,
    resourceVersion = ActionRequirement.REQUIRED, resourceVersionTransport = ActionResourceVersionTransport.IF_MATCH)
  public ResponseEntity<AverbacaoTempoServicoResponseDTO> actionRegistrarDesaverbacao(@PathVariable Long id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch, @RequestHeader("Idempotency-Key") String key,
      @jakarta.validation.Valid @RequestBody TempoLicencaTransicaoDTO dto) {
    return ErgonHttpContract.itemBody(executeVersionedItemCommand("registrar-desaverbacao", id, ifMatch, key, dto,
        condition -> service.transition(id, "registrar-desaverbacao", dto, condition)));
  }

  @PostMapping("/{id}/actions/reverter-desaverbacao") @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(operationId = "actionAverbacaoTempoServicoReverterDesaverbacao", summary = "Reverter desaverbação", description = "Executa o ato no órgão autorizado, validando estado e versão persistidos. A mesma chave e conteúdo recuperam o recibo original sem repetir efeitos.")
  @WorkflowAction(id = "reverter-desaverbacao", title = "Reverter desaverbação", scope = ActionScope.ITEM, requiredAuthorities = {"ERGON_WRITE"},
    allowedStates = {"PARCIALMENTE_DESAVERBADA", "DESAVERBADA_TOTAL"}, confirmationRequired = true, idempotencyKey = ActionRequirement.REQUIRED,
    resourceVersion = ActionRequirement.REQUIRED, resourceVersionTransport = ActionResourceVersionTransport.IF_MATCH)
  public ResponseEntity<AverbacaoTempoServicoResponseDTO> actionReverterDesaverbacao(@PathVariable Long id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch, @RequestHeader("Idempotency-Key") String key,
      @jakarta.validation.Valid @RequestBody TempoLicencaTransicaoDTO dto) {
    return ErgonHttpContract.itemBody(executeVersionedItemCommand("reverter-desaverbacao", id, ifMatch, key, dto,
        condition -> service.transition(id, "reverter-desaverbacao", dto, condition)));
  }

  @PostMapping("/{id}/actions/cancelar-averbacao") @PreAuthorize("hasAuthority('ERGON_WRITE')")
  @Operation(operationId = "actionAverbacaoTempoServicoCancelarAverbacao", summary = "Cancelar averbação", description = "Executa o ato no órgão autorizado, validando estado e versão persistidos. A mesma chave e conteúdo recuperam o recibo original sem repetir efeitos.")
  @WorkflowAction(id = "cancelar-averbacao", title = "Cancelar averbação", scope = ActionScope.ITEM, requiredAuthorities = {"ERGON_WRITE"},
    allowedStates = {"EM_ANALISE", "HOMOLOGADA"}, confirmationRequired = true, idempotencyKey = ActionRequirement.REQUIRED,
    resourceVersion = ActionRequirement.REQUIRED, resourceVersionTransport = ActionResourceVersionTransport.IF_MATCH)
  public ResponseEntity<AverbacaoTempoServicoResponseDTO> actionCancelarAverbacao(@PathVariable Long id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch, @RequestHeader("Idempotency-Key") String key,
      @jakarta.validation.Valid @RequestBody TempoLicencaTransicaoDTO dto) {
    return ErgonHttpContract.itemBody(executeVersionedItemCommand("cancelar-averbacao", id, ifMatch, key, dto,
        condition -> service.transition(id, "cancelar-averbacao", dto, condition)));
  }
  @Override
  @PostMapping("/bulk/update/evaluation")
  @Operation(
      operationId = "ergon.averbacoes-tempo-servico.bulk.uniform.evaluation",
      summary = "Avaliar edição uniforme de assentamentos de tempo e licença",
      description =
          "Avalia seleção, versões e alterações de assentamentos selecionados sem modificar o cadastro; produz uma proposta protegida.")
  @BulkResourceOperation(BulkResourceOperation.Role.UNIFORM_EVALUATION)
  public org.springframework.http.ResponseEntity<RestApiResponse<BulkProposal>> evaluateBulkUpdate(@RequestBody BulkUniformEvaluationRequest<?, AverbacaoTempoServicoFilterDTO> request) {
    return super.evaluateBulkUpdate(request);
  }

  @Override
  @PostMapping("/bulk/update-items/evaluation")
  @Operation(
      operationId = "ergon.averbacoes-tempo-servico.bulk.items.evaluation",
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
      evaluationOperationId = "ergon.averbacoes-tempo-servico.bulk.uniform.evaluation",
      atomicity = ActionCollectionAtomicity.PER_ITEM)
  @Operation(
      operationId = "ergon.averbacoes-tempo-servico.bulk.uniform",
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
      evaluationOperationId = "ergon.averbacoes-tempo-servico.bulk.items.evaluation",
      atomicity = ActionCollectionAtomicity.PER_ITEM)
  @Operation(
      operationId = "ergon.averbacoes-tempo-servico.bulk.items",
      summary = "Confirmar edições por parcela de gozo de férias",
      description =
          "Aplica os deltas individualizados preservados na proposta, com versão e resultado próprios por parcela.")
  @BulkResourceOperation(BulkResourceOperation.Role.ITEM_CONFIRMATION)
  public org.springframework.http.ResponseEntity<RestApiResponse<BulkExecution>> confirmBulkUpdateItems(@RequestBody BulkConfirmationRequest request, @RequestHeader("Idempotency-Key") String key) {
    return super.confirmBulkUpdateItems(request, key);
  }











  @PostMapping("/bulk/homologar-averbacoes-lote/evaluation")
  @Operation(
      operationId = "ergon.averbacoes-tempo-servico.homologar-averbacoes-lote.evaluation",
      summary = "Avaliar homologação de averbações",
      description =
          "Avalia o ato coletivo no mesmo órgão, sem persistir efeitos; cada alvo conserva sua versão e seu impedimento individual.")
  public org.springframework.http.ResponseEntity<RestApiResponse<BulkProposal>> bulkCommandEvaluate(
      
          @RequestBody
          BulkCommandEvaluationRequest<TempoLicencaTransicaoDTO, ?, AverbacaoTempoServicoFilterDTO>
              request) {
    return evaluateBulkDomainCommand("homologar-averbacoes-lote", request);
  }

  @PostMapping("/actions/homologar-averbacoes-lote")
  @WorkflowAction(
      id = "homologar-averbacoes-lote",
      title = "Homologar averbações em lote",
      description =
          "Homologa certidões em análise e gera o direito de licença especial quando a finalidade o exigir.",
      scope = ActionScope.COLLECTION,
      requiredAuthorities = {"ERGON_WRITE"},
      confirmationRequired = true,
      idempotencyKey = ActionRequirement.REQUIRED,
      atomicity = ActionCollectionAtomicity.PER_ITEM,
      outcomeMode = ActionOutcomeMode.PER_ITEM,
      maxSelection = 200)
  @BulkOperation(
      mode = BulkMode.DOMAIN_COMMAND,
      evaluationOperationId = "ergon.averbacoes-tempo-servico.homologar-averbacoes-lote.evaluation",
      atomicity = ActionCollectionAtomicity.PER_ITEM)
  @Operation(
      operationId = "ergon.averbacoes-tempo-servico.homologar-averbacoes-lote",
      summary = "Homologar averbações em lote",
      description = "Homologa certidões em análise e gera o direito de licença especial quando a finalidade o exigir.")
  public org.springframework.http.ResponseEntity<RestApiResponse<BulkExecution>> bulkCommandConfirm(
      @RequestBody BulkConfirmationRequest request,
      @Parameter(description = "Chave estável do ato coletivo para impedir efeitos duplicados.")
          @RequestHeader("Idempotency-Key")
          String key) {
    return confirmBulkDomainCommand("homologar-averbacoes-lote", request, key);
  }

  @GetMapping("/{id}/periodos-licenca-especial-gerados") @PreAuthorize("hasAuthority('ERGON_READ')")
  @Operation(operationId = "childrenAverbacaoTempoServicoPeriodoAquisitivoLicencaEspecial", summary = "Períodos gerados", description = "Consulta paginada dos registros relacionados, após validar o pai no órgão atual, sem aceitar substituição do filtro de parentesco.")
  @UiSurface(id = "periodos-licenca-especial-gerados", kind = SurfaceKind.READ_PROJECTION, scope = SurfaceScope.ITEM, title = "Períodos gerados", relatedChildResourceKey = ApiPaths.KEY_PERIODOS_AQUISITIVOS_LICENCA_ESPECIAL, relatedChildResourcePath = ApiPaths.PERIODOS_AQUISITIVOS_LICENCA_ESPECIAL, relatedChildParentField = "averbacaoOrigemId", relatedChildOperations = {RelatedResourceChildOperation.FILTER, RelatedResourceChildOperation.LIST}, requiredAuthorities = {"ERGON_READ"})
  public ResponseEntity<RestApiResponse<org.springframework.data.domain.Page<PeriodoAquisitivoLicencaEspecialResponseDTO>>> childrenPeriodoAquisitivoLicencaEspecial(@PathVariable Long id,
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    ErgonHttpContract.childPage(page, size);
    return filterRelatedChildResource(id, periodoAquisitivoLicencaEspecialChildren, ApiPaths.PERIODOS_AQUISITIVOS_LICENCA_ESPECIAL, PeriodoAquisitivoLicencaEspecialFilterDTO::new,
        (child, parent) -> { child.setAverbacaoOrigemId(parent); }, page, size);
  }

  @org.springframework.beans.factory.annotation.Autowired private br.com.praxis.ergonshowcase.service.TempoLicencaPreviewService previews;
  @PostMapping("/determinations/preview") @PreAuthorize("hasAuthority('ERGON_READ')")
  @Operation(operationId = "determineAverbacaoTempoServico", summary = "Tempo líquido previsto", description = "Calcula prévia de criação sem persistir, reservar saldo ou autorizar o ato.")
  public TempoLicencaPrevia.Resultado preview(@jakarta.validation.Valid @RequestBody TempoLicencaPrevia.Averbacao input) {return previews.averbacao(input);}
  @Override @PostMapping
  @Operation(operationId = "createErgonAverbacaoTempoServico", summary = "Registrar assentamento", description = "Cria no estado inicial do domínio, revalidando vínculo, política e contabilidade no servidor.")
  public ResponseEntity<RestApiResponse<AverbacaoTempoServicoResponseDTO>> create(@jakarta.validation.Valid @RequestBody CreateAverbacaoTempoServicoDTO input) {return super.create(input);}
  @Override @SuppressWarnings({"unchecked", "rawtypes"})
  protected org.praxisplatform.uischema.bulk.JpaResourceBulkEngine getBulkEngine() { return bulk.engine(Resource.AVERBACOES_TEMPO_SERVICO); }
  @Override protected String bulkProposalOperationId(UUID id) { return bulk.locate(Resource.AVERBACOES_TEMPO_SERVICO, id, false); }
  @Override protected String bulkExecutionOperationId(UUID id) { return bulk.locate(Resource.AVERBACOES_TEMPO_SERVICO, id, true); }

}
