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
    icon = "cake",
    tone = "info",
    valuePresentation = "metric",
    thresholds = {
      @Threshold(min = 0, max = 64, tone = "info", label = "Faixa regular"),
      @Threshold(min = 65, max = 74, tone = "warning", label = "Elegibilidade etária"),
      @Threshold(min = 75, max = 130, tone = "danger", label = "Compulsória (75+ anos)")
    },
    sourceKind = "computed")
@org.praxisplatform.uischema.fact.ResourceFact(
    key = "possuiDeficienciaPcD",
    resourceKey = "administracao-pessoal.funcionarios",
    label = "Pessoa com deficiência",
    description = "Indica ao menos uma das cinco deficiências específicas registrada na pessoa.",
    dataType = "boolean",
    icon = "accessible",
    tone = "info",
    valuePresentation = "badge",
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
            metadataFields = {"cpf", "idadeAnos"}))
@ApiGroup(
    value = "ergon",
    title = "Administração de pessoal",
    description = "Cadastros civis e assentamentos funcionais dos órgãos públicos.",
    icon = "groups")
public class ServidorPessoaController
    extends org.praxisplatform.uischema.controller.base.AbstractBulkGovernedResourceController<
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
              target = @AnalyticsSurfaceTarget(resourceKey = "administracao-pessoal.funcionarios", surfaceId = "ficha-cadastral"))),
      @AnalyticsProjection(id = "evolucao-nascimento-pessoas", intent = AnalyticsIntent.TREND,
          sourceOperation = AnalyticsOperation.TIMESERIES,
          primaryDimension = @AnalyticsDimensionBinding(field = "dtnasc"),
          defaultGranularity = AnalyticsGranularity.MONTH,
          defaultTimezone = "America/Sao_Paulo",
          primaryMetrics = @AnalyticsMetricBinding(field = "count", aggregation = "count", label = "Servidores por mês de nascimento")),
      @AnalyticsProjection(id = "distribuicao-numero-funcional", intent = AnalyticsIntent.DISTRIBUTION,
          sourceOperation = AnalyticsOperation.DISTRIBUTION,
          primaryDimension = @AnalyticsDimensionBinding(field = "id"),
          primaryMetrics = @AnalyticsMetricBinding(field = "id", aggregation = "histogram", label = "Distribuição de número funcional")),
      @AnalyticsProjection(id = "comparativo-pessoas-por-escolaridade", intent = AnalyticsIntent.COMPARISON,
          sourceOperation = AnalyticsOperation.COMPARISON,
          primaryDimension = @AnalyticsDimensionBinding(field = "escolaridade"),
          comparisonPeriod = @AnalyticsComparisonPeriodBinding(field = "dtnasc", timezone = "America/Sao_Paulo"),
          primaryMetrics = @AnalyticsMetricBinding(field = "count", aggregation = "count", label = "Variação por escolaridade"))
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
    return ErgonHttpContract.itemBody(executeVersionedItemPatch("atualizar-dados-bancarios-contato", id, ifMatch, condition -> commands.atualizarDadosBancariosContato(id, dto, condition)));
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
  @org.springframework.beans.factory.annotation.Autowired private DependenteService dependentesService;
  @org.springframework.beans.factory.annotation.Autowired private AfastamentoService afastamentosService;
  @org.springframework.beans.factory.annotation.Autowired private FrequenciaService frequenciasService;

  @org.springframework.web.bind.annotation.GetMapping("/{id}/afastamentos")
  @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ERGON_READ')")
  @UiSurface(
      id = "afastamentos-pessoa",
      kind = org.praxisplatform.uischema.surface.SurfaceKind.READ_PROJECTION,
      scope = org.praxisplatform.uischema.surface.SurfaceScope.ITEM,
      title = "Licenças e afastamentos",
      description = "Licenças e afastamentos funcionais consolidados em todos os vínculos da pessoa.",
      relatedChildResourceKey = "administracao-pessoal.afastamentos",
      relatedChildResourcePath = ApiPaths.AFASTAMENTOS,
      relatedChildParentField = "numfunc",
      relatedChildOperations = {
        org.praxisplatform.uischema.surface.RelatedResourceChildOperation.FILTER,
        org.praxisplatform.uischema.surface.RelatedResourceChildOperation.LIST
      },
      requiredAuthorities = {"ERGON_READ"})
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Consultar afastamentos da pessoa servidora",
      description =
          "Lista licenças e afastamentos de todos os vínculos da pessoa servidora. Paginação limitada a 200 registros.")
  public org.springframework.http.ResponseEntity<
          org.praxisplatform.uischema.rest.response.RestApiResponse<
              org.springframework.data.domain.Page<AfastamentoResponseDTO>>>
      afastamentos(
          @io.swagger.v3.oas.annotations.Parameter(description = "Identidade do servidor titular")
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
    ErgonHttpContract.childPage(page, size);
    return filterRelatedChildResource(id, afastamentosService, ApiPaths.AFASTAMENTOS, AfastamentoFilterDTO::new,
        (child, parent) -> { child.setNumfunc(parent); }, page, size);
  }

  @org.springframework.web.bind.annotation.GetMapping("/{id}/frequencias")
  @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ERGON_READ')")
  @UiSurface(
      id = "frequencias-pessoa",
      kind = org.praxisplatform.uischema.surface.SurfaceKind.READ_PROJECTION,
      scope = org.praxisplatform.uischema.surface.SurfaceScope.ITEM,
      title = "Ocorrências de frequência",
      description = "Ocorrências de frequência, faltas, abonos e horas extras em todos os vínculos da pessoa.",
      relatedChildResourceKey = "administracao-pessoal.frequencias",
      relatedChildResourcePath = ApiPaths.FREQUENCIAS,
      relatedChildParentField = "numfunc",
      relatedChildOperations = {
        org.praxisplatform.uischema.surface.RelatedResourceChildOperation.FILTER,
        org.praxisplatform.uischema.surface.RelatedResourceChildOperation.LIST
      },
      requiredAuthorities = {"ERGON_READ"})
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Consultar frequências da pessoa servidora",
      description =
          "Lista ocorrências operacionais de frequência de todos os vínculos da pessoa servidora. Paginação limitada a 200 registros.")
  public org.springframework.http.ResponseEntity<
          org.praxisplatform.uischema.rest.response.RestApiResponse<
              org.springframework.data.domain.Page<FrequenciaResponseDTO>>>
      frequencias(
          @io.swagger.v3.oas.annotations.Parameter(description = "Identidade do servidor titular")
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
    ErgonHttpContract.childPage(page, size);
    return filterRelatedChildResource(id, frequenciasService, ApiPaths.FREQUENCIAS, FrequenciaFilterDTO::new,
        (child, parent) -> { child.setNumfunc(parent); }, page, size);
  }

  @org.springframework.web.bind.annotation.GetMapping("/{id}/dependentes")
  @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ERGON_READ')")
  @UiSurface(
      id = "dependentes",
      kind = org.praxisplatform.uischema.surface.SurfaceKind.READ_PROJECTION,
      scope = org.praxisplatform.uischema.surface.SurfaceScope.ITEM,
      title = "Dependentes",
      description = "Dependentes legais e econômicos vinculados ao servidor para fins de IRRF, salário-família e saúde.",
      relatedChildResourceKey = "administracao-pessoal.dependentes",
      relatedChildResourcePath = ApiPaths.DEPENDENTES,
      relatedChildParentField = "numfunc",
      relatedChildOperations = {
        org.praxisplatform.uischema.surface.RelatedResourceChildOperation.FILTER,
        org.praxisplatform.uischema.surface.RelatedResourceChildOperation.LIST
      },
      requiredAuthorities = {"ERGON_READ"})
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Consultar dependentes do servidor",
      description =
          "Dependentes legais e econômicos vinculados ao servidor. A paginação é limitada a 200 registros por resposta.")
  public org.springframework.http.ResponseEntity<
          org.praxisplatform.uischema.rest.response.RestApiResponse<
              org.springframework.data.domain.Page<DependenteResponseDTO>>>
      dependentes(
          @io.swagger.v3.oas.annotations.Parameter(description = "Identidade do servidor titular")
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
    ErgonHttpContract.childPage(page, size);
    return filterRelatedChildResource(id, dependentesService, ApiPaths.DEPENDENTES, DependenteFilterDTO::new,
        (child, parent) -> { child.setNumfunc(parent); }, page, size);
  }

  @org.springframework.web.bind.annotation.GetMapping("/{id}/vinculos-funcionais")
  @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ERGON_READ')")
  @UiSurface(
      id = "vinculos-funcionais",
      kind = org.praxisplatform.uischema.surface.SurfaceKind.READ_PROJECTION,
      scope = org.praxisplatform.uischema.surface.SurfaceScope.ITEM,
      title = "Vínculos funcionais",
      description = "Assentamentos funcionais da pessoa, incluindo vínculos históricos.",
      relatedChildResourceKey = "administracao-pessoal.vinculos",
      relatedChildResourcePath = ApiPaths.VINCULOS,
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
    ErgonHttpContract.childPage(page, size);
    return filterRelatedChildResource(id, relatedService, ApiPaths.VINCULOS, VinculoFilterDTO::new,
        (child, parent) -> { child.setNumfunc(parent); }, page, size);
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

  @Override
  @PostMapping("/bulk/update/evaluation")
  @Operation(operationId="ergon.funcionarios.bulk.uniform.evaluation", summary="Avaliar edição uniforme",description="Avalia seleção, versões e alterações sem modificar o cadastro; produz uma proposta protegida.")
  @BulkResourceOperation(BulkResourceOperation.Role.UNIFORM_EVALUATION)
  public org.springframework.http.ResponseEntity<RestApiResponse<BulkProposal>> evaluateBulkUpdate(@RequestBody BulkUniformEvaluationRequest<?, ServidorPessoaFilterDTO> request) {
    return super.evaluateBulkUpdate(request);
  }
  @Override
  @PostMapping("/bulk/update-items/evaluation")
  @Operation(operationId="ergon.funcionarios.bulk.items.evaluation",summary="Avaliar edições por registro",description="Avalia deltas distintos e a versão esperada de cada registro, com impedimentos individualizados.")
  @BulkResourceOperation(BulkResourceOperation.Role.ITEM_EVALUATION)
  public org.springframework.http.ResponseEntity<RestApiResponse<BulkProposal>> evaluateBulkUpdateItems(@RequestBody BulkItemEvaluationRequest<?> request) {
    return super.evaluateBulkUpdateItems(request);
  }
  @Override
  @PostMapping("/bulk/update")
  @BulkOperation(mode=BulkMode.UNIFORM_UPDATE,evaluationOperationId="ergon.funcionarios.bulk.uniform.evaluation",atomicity=ActionCollectionAtomicity.PER_ITEM)
  @Operation(operationId="ergon.funcionarios.bulk.uniform",summary="Confirmar edição uniforme",description="Confirma uma proposta avaliada; cada registro é revalidado e possui recibo durável, sem repetir efeitos no replay.")
  @BulkResourceOperation(BulkResourceOperation.Role.UNIFORM_CONFIRMATION)
  public org.springframework.http.ResponseEntity<RestApiResponse<BulkExecution>> confirmBulkUpdate(@RequestBody BulkConfirmationRequest request, @RequestHeader("Idempotency-Key") String key) {
    return super.confirmBulkUpdate(request, key);
  }
  @Override
  @PostMapping("/bulk/update-items")
  @BulkOperation(mode=BulkMode.PER_ITEM_UPDATE,evaluationOperationId="ergon.funcionarios.bulk.items.evaluation",atomicity=ActionCollectionAtomicity.PER_ITEM)
  @Operation(operationId="ergon.funcionarios.bulk.items",summary="Confirmar edições por registro",description="Aplica os deltas individualizados preservados na proposta, com versão e resultado próprios por registro.")
  @BulkResourceOperation(BulkResourceOperation.Role.ITEM_CONFIRMATION)
  public org.springframework.http.ResponseEntity<RestApiResponse<BulkExecution>> confirmBulkUpdateItems(@RequestBody BulkConfirmationRequest request, @RequestHeader("Idempotency-Key") String key) {
    return super.confirmBulkUpdateItems(request, key);
  }





  @Override @SuppressWarnings({"unchecked", "rawtypes"})
  protected org.praxisplatform.uischema.bulk.JpaResourceBulkEngine getBulkEngine() { return bulk.engine(Resource.PESSOAS); }
  @Override protected String bulkProposalOperationId(UUID id) { return bulk.locate(Resource.PESSOAS, id, false); }
  @Override protected String bulkExecutionOperationId(UUID id) { return bulk.locate(Resource.PESSOAS, id, true); }

}
