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
import org.praxisplatform.uischema.fact.ResourceFact;

/** Publica o contrato de vínculos através da hierarquia canônica Praxis. */
@ResourceFact(
    key = "diasDesdeExercicio",
    resourceKey = "administracao-pessoal.vinculos",
    label = "Dias desde o exercício",
    description =
        "Dias corridos entre o início de exercício e a vacância ou a data atual; não equivale a tempo previdenciário certificado.",
    dataType = "integer",
    unit = "days",
    sourceKind = "computed")
@org.praxisplatform.uischema.fact.ResourceFact(
    key = "emEstagioProbatorio",
    resourceKey = "administracao-pessoal.vinculos",
    label =
        "Em estágio probatório",
    description =
        "Indica vínculo ativo em exercício cujo estágio ainda não foi homologado; não certifica elegibilidade jurídica.",
    dataType = "boolean",
    sourceKind = "computed")
@org.praxisplatform.uischema.fact.ResourceFact(
    key = "possuiCargoComissao",
    resourceKey = "administracao-pessoal.vinculos",
    label = "Cargo em comissão",
    description = "Indica cargo em comissão associado ao vínculo.",
    dataType = "boolean",
    sourceKind = "computed")
@org.praxisplatform.uischema.fact.ResourceFact(
    key = "emCessaoAtiva",
    resourceKey = "administracao-pessoal.vinculos",
    label = "Cessão ativa",
    description = "Indica estado funcional CEDIDO persistido.",
    dataType = "boolean",
    sourceKind = "computed")
@BulkResourceOperations(
    proposalOperationId="ergon.vinculos.bulk.proposal",
    proposalResultsOperationId="ergon.vinculos.bulk.proposal-results",
    executionOperationId="ergon.vinculos.bulk.execution",
    executionResultsOperationId="ergon.vinculos.bulk.results",
    cancelOperationId="ergon.vinculos.bulk.cancel",
    updateSourceOperationId="updateErgonVinculo", protectedUpdateFields={"cargo","dtnom","dtposse"})
@ApiResource(
    quickFilters = {
      @QuickFilter(id = "ativos", label = "ATIVO", filter = "{\"situacoes\":[\"ATIVO\"]}"),
      @QuickFilter(id = "cedidos", label = "CEDIDO", filter = "{\"situacoes\":[\"CEDIDO\"]}"),
      @QuickFilter(id = "afastados", label = "AFASTADO", filter = "{\"situacoes\":[\"AFASTADO\"]}"),
      @QuickFilter(
          id = "aposentados",
          label = "APOSENTADO",
          filter = "{\"situacoes\":[\"APOSENTADO\"]}"),
      @QuickFilter(id = "vacantes", label = "VACANTE", filter = "{\"situacoes\":[\"VACANTE\"]}"),
      @QuickFilter(id = "cota-pcd", label = "Informação de cota", filter = "{\"infoCota\":true}")
    },
    value = ApiPaths.VINCULOS,
    resourceKey = "administracao-pessoal.vinculos",
    title = "Vínculos",
    description =
        "Assentamentos funcionais por pessoa e número sequencial, com enquadramento e exercício.",
    icon = "badge",
    identity =
        @ResourceIdentity(
            keyField = "id",
            titleField = "matric",
            displayLabelField = "matric",
            subtitleField = "pessoaNome",
            metadataFields = {"orgaoNome", "cargoNome", "situacao"}))
@ApiGroup(
    value = "ergon",
    title = "Administração de pessoal",
    description = "Cadastros civis e assentamentos funcionais dos órgãos públicos.",
    icon = "groups")
public class VinculoController
    extends AbstractResourceController<
        VinculoResponseDTO, VinculoId, VinculoFilterDTO, CreateVinculoDTO, UpdateVinculoDTO> {
  private final VinculoService service;
  @org.springframework.beans.factory.annotation.Autowired private AfastamentoService afastamentosService;
  @org.springframework.beans.factory.annotation.Autowired private FrequenciaService frequenciasService;

  @Override
  @PostMapping("/stats/group-by")
  @Operation(summary = "Vínculos por situação",
      description = "Agrega o conjunto autorizado e filtrado. A projeção nominal abre a ficha pela identidade real do bucket; agrupamentos de categoria não abrem registros individuais.")
  @UiAnalytics(projections = {
      @AnalyticsProjection(id = "vinculos-composicao", intent = AnalyticsIntent.COMPOSITION,
          sourceOperation = AnalyticsOperation.GROUP_BY,
          primaryDimension = @AnalyticsDimensionBinding(field = "situacao"),
          primaryMetrics = @AnalyticsMetricBinding(field = "count", aggregation = "count", label = "Registros"),
          defaultLimit = 20),
      @AnalyticsProjection(id = "jornada-por-vinculo", intent = AnalyticsIntent.RANKING,
          sourceOperation = AnalyticsOperation.GROUP_BY,
          primaryDimension = @AnalyticsDimensionBinding(field = "id"),
          primaryMetrics = @AnalyticsMetricBinding(field = "jornadaSemanal", aggregation = "sum", label = "Horas semanais"),
          defaultLimit = 20,
          recordOpen = @AnalyticsRecordOpen(sourceIdentityField = "key",
              target = @AnalyticsSurfaceTarget(resourceKey = "administracao-pessoal.vinculos", surfaceId = "resumo-funcional")))
  })
  public org.springframework.http.ResponseEntity<RestApiResponse<org.praxisplatform.uischema.stats.dto.GroupByStatsResponse>> groupByStats(
      @RequestBody org.praxisplatform.uischema.stats.dto.GroupByStatsRequest<VinculoFilterDTO> request) {
    return super.groupByStats(request);
  }
  private final org.praxisplatform.uischema.concurrency.ResourceVersionEtagService etags;
  private final org.praxisplatform.uischema.concurrency.ResourceVersionScopeProvider scopes;

  public VinculoController(
      VinculoService service,
      org.praxisplatform.uischema.concurrency.ResourceVersionEtagService etags,
      org.praxisplatform.uischema.concurrency.ResourceVersionScopeProvider scopes) {
    this.service = service;
    this.etags = etags;
    this.scopes = scopes;
  }

  @org.springframework.web.bind.annotation.GetMapping("/{id}/resumo-funcional")
  @UiSurface(
      id = "resumo-funcional",
      kind = org.praxisplatform.uischema.surface.SurfaceKind.VIEW,
      scope = org.praxisplatform.uischema.surface.SurfaceScope.ITEM,
      title = "Resumo funcional",
      description = "Consulta do enquadramento e ciclo de vida do vínculo selecionado.",
      intent = "consultar-assentamento",
      icon = "badge")
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Consultar resumo funcional",
      description =
          "Apresenta a situação e o enquadramento do vínculo sem certificar tempo previdenciário.")
  public org.springframework.http.ResponseEntity<VinculoResponseDTO> resumo(
      @io.swagger.v3.oas.annotations.Parameter(
              description = "Identidade composta pessoa:número do vínculo",
              example = "1001:1")
          @org.springframework.web.bind.annotation.PathVariable
          VinculoId id) {
    return withResourceVersion(
        org.springframework.http.ResponseEntity.ok(), id, service.findById(id));
  }

  @org.springframework.web.bind.annotation.GetMapping("/{id}/afastamentos")
  @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ERGON_READ')")
  @UiSurface(
      id = "afastamentos-vinculo",
      kind = org.praxisplatform.uischema.surface.SurfaceKind.READ_PROJECTION,
      scope = org.praxisplatform.uischema.surface.SurfaceScope.ITEM,
      title = "Licenças e afastamentos",
      description =
          "Histórico e licenças em aberto associadas ao vínculo funcional selecionado.",
      relatedChildResourceKey = "administracao-pessoal.afastamentos",
      relatedChildResourcePath = ApiPaths.AFASTAMENTOS,
      relatedChildParentField = "numvinc",
      relatedChildOperations = {
        org.praxisplatform.uischema.surface.RelatedResourceChildOperation.FILTER,
        org.praxisplatform.uischema.surface.RelatedResourceChildOperation.LIST
      },
      requiredAuthorities = {"ERGON_READ"})
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Consultar afastamentos do vínculo",
      description =
          "Lista licenças e afastamentos vinculados ao assentamento funcional. A paginação é limitada a 200 registros por resposta.")
  public org.springframework.http.ResponseEntity<
          org.praxisplatform.uischema.rest.response.RestApiResponse<
              org.springframework.data.domain.Page<AfastamentoResponseDTO>>>
      afastamentos(
          @io.swagger.v3.oas.annotations.Parameter(
                  description = "Identidade composta pessoa:número do vínculo")
              @org.springframework.web.bind.annotation.PathVariable
              VinculoId id,
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
    var filter = new AfastamentoFilterDTO();
    filter.setNumfunc(id.numfunc());
    filter.setNumvinc(id.numero());
    var result =
        afastamentosService.filter(
            filter,
            org.springframework.data.domain.PageRequest.of(
                page, size, afastamentosService.getDefaultSort()),
            null);
    return org.springframework.http.ResponseEntity.ok(
        org.praxisplatform.uischema.rest.response.RestApiResponse.success(
            result,
            org.springframework.hateoas.Links.of(
                org.springframework.hateoas.Link.of(ApiPaths.AFASTAMENTOS, "collection"))));
  }

  @org.springframework.web.bind.annotation.GetMapping("/{id}/frequencias")
  @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ERGON_READ')")
  @UiSurface(
      id = "frequencias-vinculo",
      kind = org.praxisplatform.uischema.surface.SurfaceKind.READ_PROJECTION,
      scope = org.praxisplatform.uischema.surface.SurfaceScope.ITEM,
      title = "Ocorrências de frequência",
      description =
          "Histórico de ocorrências operacionais de frequência, faltas, abonos e horas extras do vínculo selecionado.",
      relatedChildResourceKey = "administracao-pessoal.frequencias",
      relatedChildResourcePath = ApiPaths.FREQUENCIAS,
      relatedChildParentField = "numvinc",
      relatedChildOperations = {
        org.praxisplatform.uischema.surface.RelatedResourceChildOperation.FILTER,
        org.praxisplatform.uischema.surface.RelatedResourceChildOperation.LIST
      },
      requiredAuthorities = {"ERGON_READ"})
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Consultar frequências do vínculo",
      description =
          "Lista ocorrências operacionais de frequência vinculadas ao assentamento funcional. A paginação é limitada a 200 registros por resposta.")
  public org.springframework.http.ResponseEntity<
          org.praxisplatform.uischema.rest.response.RestApiResponse<
              org.springframework.data.domain.Page<FrequenciaResponseDTO>>>
      frequencias(
          @io.swagger.v3.oas.annotations.Parameter(
                  description = "Identidade composta pessoa:número do vínculo")
              @org.springframework.web.bind.annotation.PathVariable
              VinculoId id,
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
    var filter = new FrequenciaFilterDTO();
    filter.setNumfunc(id.numfunc());
    filter.setNumvinc(id.numero());
    var result =
        frequenciasService.filter(
            filter,
            org.springframework.data.domain.PageRequest.of(
                page, size, frequenciasService.getDefaultSort()),
            null);
    return org.springframework.http.ResponseEntity.ok(
        org.praxisplatform.uischema.rest.response.RestApiResponse.success(
            result,
            org.springframework.hateoas.Links.of(
                org.springframework.hateoas.Link.of(ApiPaths.FREQUENCIAS, "collection"))));
  }

  @org.springframework.web.bind.annotation.PostMapping("/{id}/actions/registrar-vacancia")
  @WorkflowAction(
      id = "registrar-vacancia",
      title = "Registrar vacância",
      description =
          "Encerra um vínculo ativo mediante data, forma jurídica e fundamentação administrativa.",
      scope = org.praxisplatform.uischema.action.ActionScope.ITEM,
      requiredAuthorities = {"ERGON_WRITE"},
      allowedStates = {"ATIVO"},
      riskLevel = org.praxisplatform.uischema.action.ActionRiskLevel.HIGH,
      confirmationRequired = true,
      resourceVersion = org.praxisplatform.uischema.action.ActionRequirement.REQUIRED,
      resourceVersionTransport =
          org.praxisplatform.uischema.action.ActionResourceVersionTransport.IF_MATCH)
  @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ERGON_WRITE')")
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Registrar vacância",
      description =
          "Transita ATIVO para VACANTE e registra a fundamentação; exige ETag atual e órgão compatível com X-Tenant.")
  public org.springframework.http.ResponseEntity<VinculoResponseDTO> vacancia(
      @io.swagger.v3.oas.annotations.Parameter(
              description = "Identidade composta pessoa:número do vínculo",
              example = "1001:1")
          @org.springframework.web.bind.annotation.PathVariable
          VinculoId id,
      @io.swagger.v3.oas.annotations.Parameter(
              description = "ETag forte recebido na consulta do vínculo")
          @org.springframework.web.bind.annotation.RequestHeader(
              value = "If-Match",
              required = false)
          String ifMatch,
      @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody
          RegistrarVacanciaDTO dto) {
    var p =
        new org.praxisplatform.uischema.concurrency.ResourceVersionUpdatePrecondition<VinculoId>(
            etags, ifMatch, scopes.currentScope(), "administracao-pessoal.vinculos", id);
    return withResourceVersion(
        org.springframework.http.ResponseEntity.ok(), id, service.registrarVacancia(id, dto, p));
  }

  @Override
  protected VinculoService getService() {
    return service;
  }

  @Override
  protected VinculoId getResponseId(VinculoResponseDTO dto) {
    return dto.getId();
  }

  @org.springframework.beans.factory.annotation.Autowired private ErgonCommands commands;

  @org.springframework.beans.factory.annotation.Autowired
  private org.praxisplatform.uischema.concurrency.ResourceVersionEtagService commandEtags;

  @org.springframework.beans.factory.annotation.Autowired
  private org.praxisplatform.uischema.concurrency.ResourceVersionScopeProvider commandScopes;

  @org.springframework.web.bind.annotation.PostMapping("/{id}/actions/registrar-cessao")
  @WorkflowAction(
      id = "registrar-cessao",
      title = "Registrar cessão",
      description =
          "Registra o exercício temporário em outro órgão e a responsabilidade financeira da cessão.",
      scope = org.praxisplatform.uischema.action.ActionScope.ITEM,
      requiredAuthorities = {"ERGON_WRITE"},
      allowedStates = {"ATIVO"},
      resourceVersion = org.praxisplatform.uischema.action.ActionRequirement.REQUIRED,
      resourceVersionTransport =
          org.praxisplatform.uischema.action.ActionResourceVersionTransport.IF_MATCH)
  @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ERGON_WRITE')")
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Registrar cessão",
      description =
          "Registra o exercício temporário em outro órgão e a responsabilidade financeira da cessão. Exige If-Match atual.")
  public org.springframework.http.ResponseEntity<VinculoResponseDTO> registrarCessao(
      @org.springframework.web.bind.annotation.PathVariable VinculoId id,
      @org.springframework.web.bind.annotation.RequestHeader(value = "If-Match", required = false)
          String ifMatch,
      @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody
          RegistrarCessaoDTO dto) {
    var p =
        new org.praxisplatform.uischema.concurrency.ResourceVersionUpdatePrecondition<VinculoId>(
            commandEtags,
            ifMatch,
            commandScopes.currentScope(),
            "administracao-pessoal.vinculos",
            id);
    return withResourceVersion(
        org.springframework.http.ResponseEntity.ok(), id, commands.registrarCessao(id, dto, p));
  }

  @org.springframework.web.bind.annotation.PostMapping("/{id}/actions/encerrar-cessao")
  @WorkflowAction(
      id = "encerrar-cessao",
      title = "Encerrar cessão",
      description = "Registra o término da cessão e o retorno ao estado ativo.",
      scope = org.praxisplatform.uischema.action.ActionScope.ITEM,
      requiredAuthorities = {"ERGON_WRITE"},
      allowedStates = {"CEDIDO"},
      resourceVersion = org.praxisplatform.uischema.action.ActionRequirement.REQUIRED,
      resourceVersionTransport =
          org.praxisplatform.uischema.action.ActionResourceVersionTransport.IF_MATCH)
  @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ERGON_WRITE')")
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Encerrar cessão",
      description =
          "Registra o término da cessão e o retorno ao estado ativo. Exige If-Match atual.")
  public org.springframework.http.ResponseEntity<VinculoResponseDTO> encerrarCessao(
      @org.springframework.web.bind.annotation.PathVariable VinculoId id,
      @org.springframework.web.bind.annotation.RequestHeader(value = "If-Match", required = false)
          String ifMatch,
      @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody
          EncerrarCessaoDTO dto) {
    var p =
        new org.praxisplatform.uischema.concurrency.ResourceVersionUpdatePrecondition<VinculoId>(
            commandEtags,
            ifMatch,
            commandScopes.currentScope(),
            "administracao-pessoal.vinculos",
            id);
    return withResourceVersion(
        org.springframework.http.ResponseEntity.ok(), id, commands.encerrarCessao(id, dto, p));
  }

  @org.springframework.web.bind.annotation.PostMapping("/{id}/actions/conceder-aposentadoria")
  @WorkflowAction(
      id = "conceder-aposentadoria",
      title = "Conceder aposentadoria",
      description =
          "Registra a concessão administrativa fundamentada; não calcula elegibilidade ou benefício.",
      scope = org.praxisplatform.uischema.action.ActionScope.ITEM,
      requiredAuthorities = {"ERGON_WRITE"},
      allowedStates = {"ATIVO"},
      resourceVersion = org.praxisplatform.uischema.action.ActionRequirement.REQUIRED,
      resourceVersionTransport =
          org.praxisplatform.uischema.action.ActionResourceVersionTransport.IF_MATCH)
  @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ERGON_WRITE')")
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Conceder aposentadoria",
      description =
          "Registra a concessão administrativa fundamentada; não calcula elegibilidade ou benefício. Exige If-Match atual.")
  public org.springframework.http.ResponseEntity<VinculoResponseDTO> concederAposentadoria(
      @org.springframework.web.bind.annotation.PathVariable VinculoId id,
      @org.springframework.web.bind.annotation.RequestHeader(value = "If-Match", required = false)
          String ifMatch,
      @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody
          ConcederAposentadoriaDTO dto) {
    var p =
        new org.praxisplatform.uischema.concurrency.ResourceVersionUpdatePrecondition<VinculoId>(
            commandEtags,
            ifMatch,
            commandScopes.currentScope(),
            "administracao-pessoal.vinculos",
            id);
    return withResourceVersion(
        org.springframework.http.ResponseEntity.ok(),
        id,
        commands.concederAposentadoria(id, dto, p));
  }

  @org.springframework.web.bind.annotation.PostMapping("/{id}/actions/homologar-estagio-probatorio")
  @WorkflowAction(
      id = "homologar-estagio-probatorio",
      title = "Homologar estágio probatório",
      description = "Registra homologação fundamentada do estágio probatório.",
      scope = org.praxisplatform.uischema.action.ActionScope.ITEM,
      requiredAuthorities = {"ERGON_WRITE"},
      allowedStates = {"ATIVO"},
      resourceVersion = org.praxisplatform.uischema.action.ActionRequirement.REQUIRED,
      resourceVersionTransport =
          org.praxisplatform.uischema.action.ActionResourceVersionTransport.IF_MATCH)
  @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ERGON_WRITE')")
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Homologar estágio probatório",
      description =
          "Registra homologação fundamentada do estágio probatório. Exige If-Match atual.")
  public org.springframework.http.ResponseEntity<VinculoResponseDTO> homologarEstagio(
      @org.springframework.web.bind.annotation.PathVariable VinculoId id,
      @org.springframework.web.bind.annotation.RequestHeader(value = "If-Match", required = false)
          String ifMatch,
      @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody
          HomologarEstagioDTO dto) {
    var p =
        new org.praxisplatform.uischema.concurrency.ResourceVersionUpdatePrecondition<VinculoId>(
            commandEtags,
            ifMatch,
            commandScopes.currentScope(),
            "administracao-pessoal.vinculos",
            id);
    return withResourceVersion(
        org.springframework.http.ResponseEntity.ok(), id, commands.homologarEstagio(id, dto, p));
  }

  @org.springframework.web.bind.annotation.PatchMapping("/{id}/lotacao-exercicio")
  @ResourceIntent(
      id = "alterar-lotacao-exercicio",
      title = "Alterar lotação e exercício",
      description = "Mantém unidades, cargo em comissão e jornada do vínculo no mesmo órgão.")
  @UiSurface(
      id = "lotacao-exercicio",
      kind = org.praxisplatform.uischema.surface.SurfaceKind.PARTIAL_FORM,
      scope = org.praxisplatform.uischema.surface.SurfaceScope.ITEM,
      title = "Alterar lotação e exercício",
      description = "Mantém unidades, cargo em comissão e jornada do vínculo no mesmo órgão.",
      requiredAuthorities = {"ERGON_WRITE"})
  @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ERGON_WRITE')")
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Alterar lotação e exercício",
      description =
          "Mantém unidades, cargo em comissão e jornada do vínculo no mesmo órgão. Exige If-Match atual.")
  public org.springframework.http.ResponseEntity<VinculoResponseDTO> alterarLotacaoExercicio(
      @org.springframework.web.bind.annotation.PathVariable VinculoId id,
      @org.springframework.web.bind.annotation.RequestHeader(value = "If-Match", required = false)
          String ifMatch,
      @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody
          AlterarLotacaoExercicioDTO dto) {
    var p =
        new org.praxisplatform.uischema.concurrency.ResourceVersionUpdatePrecondition<VinculoId>(
            commandEtags,
            ifMatch,
            commandScopes.currentScope(),
            "administracao-pessoal.vinculos",
            id);
    return withResourceVersion(
        org.springframework.http.ResponseEntity.ok(),
        id,
        commands.alterarLotacaoExercicio(id, dto, p));
  }

  @org.springframework.web.bind.annotation.PatchMapping("/{id}/previdencia-pagamento")
  @ResourceIntent(
      id = "atualizar-previdencia-pagamento",
      title = "Atualizar previdência e pagamento",
      description = "Mantém o enquadramento previdenciário e a conta de pagamento do vínculo.")
  @UiSurface(
      id = "previdencia-pagamento",
      kind = org.praxisplatform.uischema.surface.SurfaceKind.PARTIAL_FORM,
      scope = org.praxisplatform.uischema.surface.SurfaceScope.ITEM,
      title = "Atualizar previdência e pagamento",
      description = "Mantém o enquadramento previdenciário e a conta de pagamento do vínculo.",
      requiredAuthorities = {"ERGON_WRITE"})
  @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ERGON_WRITE')")
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Atualizar previdência e pagamento",
      description =
          "Mantém o enquadramento previdenciário e a conta de pagamento do vínculo. Exige If-Match atual.")
  public org.springframework.http.ResponseEntity<VinculoResponseDTO> atualizarPrevidenciaPagamento(
      @org.springframework.web.bind.annotation.PathVariable VinculoId id,
      @org.springframework.web.bind.annotation.RequestHeader(value = "If-Match", required = false)
          String ifMatch,
      @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody
          AtualizarPrevidenciaPagamentoDTO dto) {
    var p =
        new org.praxisplatform.uischema.concurrency.ResourceVersionUpdatePrecondition<VinculoId>(
            commandEtags,
            ifMatch,
            commandScopes.currentScope(),
            "administracao-pessoal.vinculos",
            id);
    return withResourceVersion(
        org.springframework.http.ResponseEntity.ok(),
        id,
        commands.atualizarPrevidenciaPagamento(id, dto, p));
  }

  @org.springframework.web.bind.annotation.PostMapping("/{id}/actions/registrar-afastamento")
  @WorkflowAction(
      id = "registrar-afastamento",
      title = "Registrar afastamento",
      description =
          "Concede afastamento fundamentado, registra a licença homologada e altera o vínculo para AFASTADO.",
      scope = org.praxisplatform.uischema.action.ActionScope.ITEM,
      requiredAuthorities = {"ERGON_WRITE"},
      allowedStates = {"ATIVO"},
      confirmationRequired = true,
      resourceVersion = org.praxisplatform.uischema.action.ActionRequirement.REQUIRED,
      resourceVersionTransport =
          org.praxisplatform.uischema.action.ActionResourceVersionTransport.IF_MATCH)
  @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ERGON_WRITE')")
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Registrar afastamento",
      description =
          "Concede afastamento fundamentado, registra a licença homologada e altera o vínculo para AFASTADO. Exige If-Match atual.")
  public org.springframework.http.ResponseEntity<VinculoResponseDTO> registrarAfastamento(
      @io.swagger.v3.oas.annotations.Parameter(description = "Identidade imutável do assentamento")
          @org.springframework.web.bind.annotation.PathVariable
          VinculoId id,
      @io.swagger.v3.oas.annotations.Parameter(description = "ETag forte recebido na leitura atual")
          @org.springframework.web.bind.annotation.RequestHeader(
              value = "If-Match",
              required = false)
          String ifMatch,
      @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody
          RegistrarAfastamentoDTO dto) {
    var p =
        new org.praxisplatform.uischema.concurrency.ResourceVersionUpdatePrecondition<VinculoId>(
            commandEtags,
            ifMatch,
            commandScopes.currentScope(),
            "administracao-pessoal.vinculos",
            id);
    return withResourceVersion(
        org.springframework.http.ResponseEntity.ok(),
        id,
        commands.registrarAfastamento(id, dto, p));
  }

  @org.springframework.web.bind.annotation.PostMapping("/{id}/actions/encerrar-afastamento")
  @WorkflowAction(
      id = "encerrar-afastamento",
      title = "Registrar retorno",
      description =
          "Encerra a licença homologada aberta deste vínculo e registra seu retorno ao estado ATIVO.",
      scope = org.praxisplatform.uischema.action.ActionScope.ITEM,
      requiredAuthorities = {"ERGON_WRITE"},
      allowedStates = {"AFASTADO"},
      confirmationRequired = true,
      resourceVersion = org.praxisplatform.uischema.action.ActionRequirement.REQUIRED,
      resourceVersionTransport =
          org.praxisplatform.uischema.action.ActionResourceVersionTransport.IF_MATCH)
  @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ERGON_WRITE')")
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Registrar retorno",
      description =
          "Encerra a licença homologada aberta deste vínculo e registra seu retorno ao estado ATIVO. Exige If-Match atual.")
  public org.springframework.http.ResponseEntity<VinculoResponseDTO> encerrarAfastamento(
      @io.swagger.v3.oas.annotations.Parameter(description = "Identidade imutável do assentamento")
          @org.springframework.web.bind.annotation.PathVariable
          VinculoId id,
      @io.swagger.v3.oas.annotations.Parameter(description = "ETag forte recebido na leitura atual")
          @org.springframework.web.bind.annotation.RequestHeader(
              value = "If-Match",
              required = false)
          String ifMatch,
      @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody
          EncerrarAfastamentoDTO dto) {
    var p =
        new org.praxisplatform.uischema.concurrency.ResourceVersionUpdatePrecondition<VinculoId>(
            commandEtags,
            ifMatch,
            commandScopes.currentScope(),
            "administracao-pessoal.vinculos",
            id);
    return withResourceVersion(
        org.springframework.http.ResponseEntity.ok(), id, commands.encerrarAfastamento(id, dto, p));
  }

  @Override
  @org.springframework.web.bind.annotation.PostMapping
  @io.swagger.v3.oas.annotations.Operation(
      operationId = "createErgonVinculo",
      summary = "Cadastrar vinculo",
      description =
          "Inclui o cadastro validando identidade, órgão e invariantes de domínio; prévias do formulário não são autoridade de persistência.")
  public org.springframework.http.ResponseEntity<
          org.praxisplatform.uischema.rest.response.RestApiResponse<VinculoResponseDTO>>
      create(
          @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody
              CreateVinculoDTO dto) {
    return super.create(dto);
  }

  @Override
  @org.springframework.web.bind.annotation.PutMapping("/{id}")
  @BulkResourceOperation(BulkResourceOperation.Role.UPDATE_SOURCE)
  @io.swagger.v3.oas.annotations.Operation(
      operationId = "updateErgonVinculo",
      summary = "Manter vinculo",
      description =
          "Mantém os dados cadastrais com If-Match atual, preservando a identidade e as transições controladas por ações.")
  public org.springframework.http.ResponseEntity<
          org.praxisplatform.uischema.rest.response.RestApiResponse<VinculoResponseDTO>>
      update(
          @io.swagger.v3.oas.annotations.Parameter(description = "Identidade imutável do cadastro")
              @org.springframework.web.bind.annotation.PathVariable
              VinculoId id,
          @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody
              UpdateVinculoDTO dto) {
    return super.update(id, dto);
  }

  @org.springframework.beans.factory.annotation.Autowired
  private ErgonDeterminationService determinations;

  @org.springframework.web.bind.annotation.PostMapping("/determinations/provimento")
  @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ERGON_READ')")
  @io.swagger.v3.oas.annotations.Operation(
      operationId = "determineErgonProvimento",
      summary = "Consultar cargo e saldo",
      description =
          "Consulta fatos atuais do cargo no órgão do contexto, sem reservar vaga; o submit revalida sob bloqueio.")
  public org.springframework.http.ResponseEntity<ProvimentoPreviewDTO> determine(
      @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody
          CargoSelectionDTO draft) {
    return org.springframework.http.ResponseEntity.ok(determinations.provimento(draft));
  }

  @org.springframework.beans.factory.annotation.Autowired private ErgonBulkOperations bulk;

  @PostMapping("/bulk/update/evaluation")
  @Operation(operationId="ergon.vinculos.bulk.uniform.evaluation", summary="Avaliar edição uniforme",description="Avalia seleção, versões e alterações sem modificar o cadastro; produz uma proposta protegida.")
  public RestApiResponse<BulkProposal> bulkEvaluate(@io.swagger.v3.oas.annotations.parameters.RequestBody(content=@io.swagger.v3.oas.annotations.media.Content(schema=@io.swagger.v3.oas.annotations.media.Schema(implementation=ErgonBulkSchema.VinculoUniform.class))) @RequestBody BulkUniformEvaluationRequest<String,VinculoFilterDTO> request) {
    return RestApiResponse.success(bulk.uniform(Resource.VINCULOS,request),null);
  }
  @PostMapping("/bulk/update-items/evaluation")
  @Operation(operationId="ergon.vinculos.bulk.items.evaluation",summary="Avaliar edições por registro",description="Avalia deltas distintos e a versão esperada de cada registro, com impedimentos individualizados.")
  public RestApiResponse<BulkProposal> bulkEvaluateItems(@io.swagger.v3.oas.annotations.parameters.RequestBody(content=@io.swagger.v3.oas.annotations.media.Content(schema=@io.swagger.v3.oas.annotations.media.Schema(implementation=ErgonBulkSchema.VinculoItems.class))) @RequestBody BulkItemEvaluationRequest<String> request) {
    return RestApiResponse.success(bulk.items(Resource.VINCULOS,request),null);
  }
  @PostMapping("/bulk/update")
  @BulkOperation(mode=BulkMode.UNIFORM_UPDATE,evaluationOperationId="ergon.vinculos.bulk.uniform.evaluation",atomicity=ActionCollectionAtomicity.PER_ITEM)
  @Operation(operationId="ergon.vinculos.bulk.uniform",summary="Confirmar edição uniforme",description="Confirma uma proposta avaliada; cada registro é revalidado e possui recibo durável, sem repetir efeitos no replay.")
  public RestApiResponse<BulkExecution> bulkConfirm(@RequestBody BulkConfirmationRequest request,
      @Parameter(description="Chave estável desta confirmação; repetir a mesma chave recupera a execução.") @RequestHeader("Idempotency-Key") String key) {
    return RestApiResponse.success(bulk.confirm(Resource.VINCULOS,"ergon.vinculos.bulk.uniform",request,key),null);
  }
  @PostMapping("/bulk/update-items")
  @BulkOperation(mode=BulkMode.PER_ITEM_UPDATE,evaluationOperationId="ergon.vinculos.bulk.items.evaluation",atomicity=ActionCollectionAtomicity.PER_ITEM)
  @Operation(operationId="ergon.vinculos.bulk.items",summary="Confirmar edições por registro",description="Aplica os deltas individualizados preservados na proposta, com versão e resultado próprios por registro.")
  public RestApiResponse<BulkExecution> bulkConfirmItems(@RequestBody BulkConfirmationRequest request,
      @Parameter(description="Chave estável de confirmação para recuperar a mesma execução em caso de repetição.") @RequestHeader("Idempotency-Key") String key) {
    return RestApiResponse.success(bulk.confirm(Resource.VINCULOS,"ergon.vinculos.bulk.items",request,key),null);
  }
  @GetMapping("/bulk/proposals/{proposalId}")
  @BulkResourceOperation(BulkResourceOperation.Role.PROPOSAL)
  @Operation(operationId="ergon.vinculos.bulk.proposal",summary="Consultar proposta de lote",description="Consulta totais e intenção pública da proposta após conferir acesso ao conjunto completo de registros.")
  public RestApiResponse<BulkProposal> bulkProposal(@Parameter(description="Identidade da proposta avaliada.") @PathVariable UUID proposalId) {
    return RestApiResponse.success(bulk.proposal(Resource.VINCULOS,bulk.locate(Resource.VINCULOS,proposalId,false),proposalId),null);
  }
  @GetMapping("/bulk/proposals/{proposalId}/results")
  @BulkResourceOperation(BulkResourceOperation.Role.PROPOSAL_RESULTS)
  @Operation(operationId="ergon.vinculos.bulk.proposal-results",summary="Inspecionar avaliação do lote",description="Pagina registros executáveis e bloqueados, com diagnósticos seguros e cursor vinculado ao solicitante.")
  public RestApiResponse<CursorPage<BulkProposalItemResult<Object>>> bulkProposalResults(
      @Parameter(description="Identidade da proposta avaliada.") @PathVariable UUID proposalId,
      @Parameter(description="Quantidade de registros por página, de 1 a 200.") @RequestParam(defaultValue="50") int size,
      @Parameter(description="Cursor opaco emitido na página anterior; preserve o tamanho da página.") @RequestParam(required=false) String after) {
    return RestApiResponse.success(bulk.proposalResults(Resource.VINCULOS,bulk.locate(Resource.VINCULOS,proposalId,false),proposalId,size,after),null);
  }
  @GetMapping("/bulk/executions/{executionId}")
  @BulkResourceOperation(BulkResourceOperation.Role.EXECUTION)
  @Operation(operationId="ergon.vinculos.bulk.execution",summary="Consultar execução do lote",description="Retorna estado e contadores reconciliados a partir dos recibos persistidos.")
  public RestApiResponse<BulkExecution> bulkExecution(@Parameter(description="Identidade da execução confirmada.") @PathVariable UUID executionId) {
    return RestApiResponse.success(bulk.execution(Resource.VINCULOS,bulk.locate(Resource.VINCULOS,executionId,true),executionId),null);
  }
  @GetMapping("/bulk/executions/{executionId}/results")
  @BulkResourceOperation(BulkResourceOperation.Role.EXECUTION_RESULTS)
  @Operation(operationId="ergon.vinculos.bulk.results",summary="Consultar recibos do lote",description="Pagina resultados por registro sem expor fatos protegidos, versões ou dados bancários da proposta.")
  public RestApiResponse<CursorPage<BulkItemResult<Object>>> bulkResults(
      @Parameter(description="Identidade da execução confirmada.") @PathVariable UUID executionId,
      @Parameter(description="Quantidade de recibos por página, de 1 a 200.") @RequestParam(defaultValue="50") int size,
      @Parameter(description="Cursor opaco da página anterior de recibos.") @RequestParam(required=false) String after) {
    return RestApiResponse.success(bulk.results(Resource.VINCULOS,bulk.locate(Resource.VINCULOS,executionId,true),executionId,size,after),null);
  }
  @PostMapping("/bulk/executions/{executionId}/cancel")
  @BulkResourceOperation(BulkResourceOperation.Role.CANCEL)
  @Operation(operationId="ergon.vinculos.bulk.cancel",summary="Solicitar parada do lote",description="Registra a solicitação de parada sem desfazer alterações já confirmadas e sem reabrir efeitos no replay.")
  public RestApiResponse<BulkExecution> bulkCancel(@Parameter(description="Identidade da execução a interromper.") @PathVariable UUID executionId) {
    return RestApiResponse.success(bulk.cancel(Resource.VINCULOS,bulk.locate(Resource.VINCULOS,executionId,true),executionId),null);
  }

  @PostMapping("/bulk/realocar-setor-lote/evaluation")
  @Operation(operationId="ergon.vinculos.realocar-setor-lote.evaluation",summary="Avaliar ato coletivo",description="Realoca vínculos ativos ou cedidos por portaria do mesmo órgão, preservando versão e recibo por registro. A avaliação não grava alterações funcionais.")
  public RestApiResponse<BulkProposal> bulkRealocarEvaluate(@io.swagger.v3.oas.annotations.parameters.RequestBody(content=@io.swagger.v3.oas.annotations.media.Content(schema=@io.swagger.v3.oas.annotations.media.Schema(implementation=ErgonBulkSchema.RealocarCommand.class))) @RequestBody BulkCommandEvaluationRequest<RealocarSetorLoteDTO,String,VinculoFilterDTO> request) {
    return RestApiResponse.success(bulk.command("realocar-setor-lote",request),null);
  }
  @PostMapping("/actions/realocar-setor-lote")
  @WorkflowAction(id="realocar-setor-lote",title="Realocar setor em lote",description="Realoca vínculos ativos ou cedidos por portaria do mesmo órgão, preservando versão e recibo por registro.",scope=ActionScope.COLLECTION,
      requiredAuthorities={"ERGON_WRITE"},confirmationRequired=true,idempotencyKey=ActionRequirement.REQUIRED,
      atomicity=ActionCollectionAtomicity.PER_ITEM,outcomeMode=ActionOutcomeMode.PER_ITEM,maxSelection=200)
  @BulkOperation(mode=BulkMode.DOMAIN_COMMAND,evaluationOperationId="ergon.vinculos.realocar-setor-lote.evaluation",atomicity=ActionCollectionAtomicity.PER_ITEM)
  @Operation(operationId="ergon.vinculos.realocar-setor-lote",summary="Realocar setor em lote",description="Realoca vínculos ativos ou cedidos por portaria do mesmo órgão, preservando versão e recibo por registro.")
  public RestApiResponse<BulkExecution> bulkRealocarConfirm(@RequestBody BulkConfirmationRequest request,
      @Parameter(description="Chave estável do ato coletivo para impedir efeitos duplicados.") @RequestHeader("Idempotency-Key") String key) {
    return RestApiResponse.success(bulk.confirm(Resource.VINCULOS,"ergon.vinculos.realocar-setor-lote",request,key),null);
  }

  @PostMapping("/bulk/homologar-estagio-lote/evaluation")
  @Operation(operationId="ergon.vinculos.homologar-estagio-lote.evaluation",summary="Avaliar ato coletivo",description="Homologa estágios ainda pendentes de vínculos ativos, com data coerente e documento do órgão. A avaliação não grava alterações funcionais.")
  public RestApiResponse<BulkProposal> bulkHomologarEvaluate(@io.swagger.v3.oas.annotations.parameters.RequestBody(content=@io.swagger.v3.oas.annotations.media.Content(schema=@io.swagger.v3.oas.annotations.media.Schema(implementation=ErgonBulkSchema.HomologarCommand.class))) @RequestBody BulkCommandEvaluationRequest<HomologarEstagioLoteDTO,String,VinculoFilterDTO> request) {
    return RestApiResponse.success(bulk.command("homologar-estagio-lote",request),null);
  }
  @PostMapping("/actions/homologar-estagio-lote")
  @WorkflowAction(id="homologar-estagio-lote",title="Homologar estágio em lote",description="Homologa estágios ainda pendentes de vínculos ativos, com data coerente e documento do órgão.",scope=ActionScope.COLLECTION,
      requiredAuthorities={"ERGON_WRITE"},confirmationRequired=true,idempotencyKey=ActionRequirement.REQUIRED,
      atomicity=ActionCollectionAtomicity.PER_ITEM,outcomeMode=ActionOutcomeMode.PER_ITEM,maxSelection=200)
  @BulkOperation(mode=BulkMode.DOMAIN_COMMAND,evaluationOperationId="ergon.vinculos.homologar-estagio-lote.evaluation",atomicity=ActionCollectionAtomicity.PER_ITEM)
  @Operation(operationId="ergon.vinculos.homologar-estagio-lote",summary="Homologar estágio em lote",description="Homologa estágios ainda pendentes de vínculos ativos, com data coerente e documento do órgão.")
  public RestApiResponse<BulkExecution> bulkHomologarConfirm(@RequestBody BulkConfirmationRequest request,
      @Parameter(description="Chave estável do ato coletivo para impedir efeitos duplicados.") @RequestHeader("Idempotency-Key") String key) {
    return RestApiResponse.success(bulk.confirm(Resource.VINCULOS,"ergon.vinculos.homologar-estagio-lote",request,key),null);
  }
}
