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
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.praxisplatform.uischema.surface.RelatedResourceChildOperation;
import org.praxisplatform.uischema.surface.SurfaceKind;
import org.praxisplatform.uischema.surface.SurfaceScope;

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
    icon = "calendar_month",
    tone = "info",
    valuePresentation = "metric",
    thresholds = {
      @Threshold(min = 0, max = 1094, tone = "warning", label = "Período inicial (< 3 anos)"),
      @Threshold(min = 1095, max = 36500, tone = "success", label = "Carreira consolidada")
    },
    sourceKind = "computed")
@org.praxisplatform.uischema.fact.ResourceFact(
    key = "emEstagioProbatorio",
    resourceKey = "administracao-pessoal.vinculos",
    label =
        "Em estágio probatório",
    description =
        "Indica vínculo ativo em exercício cujo estágio ainda não foi homologado; não certifica elegibilidade jurídica.",
    dataType = "boolean",
    icon = "hourglass_top",
    tone = "warning",
    valuePresentation = "badge",
    sourceKind = "computed")
@org.praxisplatform.uischema.fact.ResourceFact(
    key = "possuiCargoComissao",
    resourceKey = "administracao-pessoal.vinculos",
    label = "Cargo em comissão",
    description = "Indica cargo em comissão associado ao vínculo.",
    dataType = "boolean",
    icon = "workspace_premium",
    tone = "info",
    valuePresentation = "badge",
    sourceKind = "computed")
@org.praxisplatform.uischema.fact.ResourceFact(
    key = "emCessaoAtiva",
    resourceKey = "administracao-pessoal.vinculos",
    label = "Cessão ativa",
    description = "Indica estado funcional CEDIDO persistido.",
    dataType = "boolean",
    icon = "swap_horiz",
    tone = "warning",
    valuePresentation = "badge",
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
            metadataFields = {"setorNome", "regimejur", "categoria"}))
@ApiGroup(
    value = "ergon",
    title = "Administração de pessoal",
    description = "Cadastros civis e assentamentos funcionais dos órgãos públicos.",
    icon = "groups")
public class VinculoController
    extends org.praxisplatform.uischema.controller.base.AbstractBulkGovernedResourceController<
        VinculoResponseDTO, VinculoId, VinculoFilterDTO, CreateVinculoDTO, UpdateVinculoDTO> {
  private final VinculoService service;
  @org.springframework.beans.factory.annotation.Autowired private AfastamentoService afastamentosService;
  @org.springframework.beans.factory.annotation.Autowired private FrequenciaService frequenciasService;
  @org.springframework.beans.factory.annotation.Autowired private PeriodoAquisitivoFeriasService periodosAquisitivosFeriasService;
  @org.springframework.beans.factory.annotation.Autowired private FeriasGozoService feriasGozoService;

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
              target = @AnalyticsSurfaceTarget(resourceKey = "administracao-pessoal.vinculos", surfaceId = "resumo-funcional"))),
      @AnalyticsProjection(id = "evolucao-exercicio-vinculos", intent = AnalyticsIntent.TREND,
          sourceOperation = AnalyticsOperation.TIMESERIES,
          primaryDimension = @AnalyticsDimensionBinding(field = "dtexerc"),
          defaultGranularity = AnalyticsGranularity.MONTH,
          defaultTimezone = "America/Sao_Paulo",
          primaryMetrics = @AnalyticsMetricBinding(field = "count", aggregation = "count", label = "Vínculos iniciados no mês")),
      @AnalyticsProjection(id = "distribuicao-jornada-semanal", intent = AnalyticsIntent.DISTRIBUTION,
          sourceOperation = AnalyticsOperation.DISTRIBUTION,
          primaryDimension = @AnalyticsDimensionBinding(field = "jornadaSemanal"),
          primaryMetrics = @AnalyticsMetricBinding(field = "jornadaSemanal", aggregation = "histogram", label = "Distribuição de jornada semanal")),
      @AnalyticsProjection(id = "comparativo-vinculos-por-situacao", intent = AnalyticsIntent.COMPARISON,
          sourceOperation = AnalyticsOperation.COMPARISON,
          primaryDimension = @AnalyticsDimensionBinding(field = "situacao"),
          comparisonPeriod = @AnalyticsComparisonPeriodBinding(field = "dtexerc", timezone = "America/Sao_Paulo"),
          primaryMetrics = @AnalyticsMetricBinding(field = "count", aggregation = "count", label = "Variação por situação"))
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
  @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ERGON_READ')")
  @UiSurface(
      id = "resumo-funcional",
      kind = org.praxisplatform.uischema.surface.SurfaceKind.READ_PROJECTION,
      scope = org.praxisplatform.uischema.surface.SurfaceScope.ITEM,
      title = "Resumo funcional",
      description = "Consulta do enquadramento e ciclo de vida do vínculo selecionado.",
      intent = "consultar-assentamento",
      icon = "badge",
      requiredAuthorities = {"ERGON_READ"})
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
      relatedChildParentField = "vinculoId",
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
    ErgonHttpContract.childPage(page, size);
    return filterRelatedChildResource(id, afastamentosService, ApiPaths.AFASTAMENTOS, AfastamentoFilterDTO::new,
        (child, parent) -> { child.setVinculoId(parent); }, page, size);
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
      relatedChildParentField = "vinculoId",
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
    ErgonHttpContract.childPage(page, size);
    return filterRelatedChildResource(id, frequenciasService, ApiPaths.FREQUENCIAS, FrequenciaFilterDTO::new,
        (child, parent) -> { child.setVinculoId(parent); }, page, size);
  }

  @org.springframework.web.bind.annotation.GetMapping("/{id}/periodos-aquisitivos-ferias")
  @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ERGON_READ')")
  @UiSurface(
      id = "periodos-aquisitivos-vinculo",
      kind = org.praxisplatform.uischema.surface.SurfaceKind.READ_PROJECTION,
      scope = org.praxisplatform.uischema.surface.SurfaceScope.ITEM,
      title = "Períodos aquisitivos de férias",
      description =
          "Ciclos aquisitivos de férias apurados e saldos disponíveis do vínculo funcional selecionado.",
      relatedChildResourceKey = "administracao-pessoal.periodos-aquisitivos-ferias",
      relatedChildResourcePath = ApiPaths.PERIODOS_AQUISITIVOS_FERIAS,
      relatedChildParentField = "vinculoId",
      relatedChildOperations = {
        org.praxisplatform.uischema.surface.RelatedResourceChildOperation.FILTER,
        org.praxisplatform.uischema.surface.RelatedResourceChildOperation.LIST
      },
      requiredAuthorities = {"ERGON_READ"})
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Consultar períodos aquisitivos de férias do vínculo",
      description =
          "Lista períodos aquisitivos de férias vinculados ao assentamento funcional. A paginação é limitada a 200 registros por resposta.")
  public org.springframework.http.ResponseEntity<
          org.praxisplatform.uischema.rest.response.RestApiResponse<
              org.springframework.data.domain.Page<PeriodoAquisitivoFeriasResponseDTO>>>
      periodosAquisitivosFerias(
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
    ErgonHttpContract.childPage(page, size);
    return filterRelatedChildResource(id, periodosAquisitivosFeriasService, ApiPaths.PERIODOS_AQUISITIVOS_FERIAS, PeriodoAquisitivoFeriasFilterDTO::new,
        (child, parent) -> { child.setVinculoId(parent); }, page, size);
  }

  @org.springframework.web.bind.annotation.GetMapping("/{id}/ferias-gozo")
  @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ERGON_READ')")
  @UiSurface(
      id = "ferias-gozo-vinculo",
      kind = org.praxisplatform.uischema.surface.SurfaceKind.READ_PROJECTION,
      scope = org.praxisplatform.uischema.surface.SurfaceScope.ITEM,
      title = "Programação e gozo de férias",
      description =
          "Parcelas de programação e gozo de férias e abono pecuniário associadas ao vínculo funcional selecionado.",
      relatedChildResourceKey = "administracao-pessoal.ferias-gozo",
      relatedChildResourcePath = ApiPaths.FERIAS_GOZO,
      relatedChildParentField = "vinculoId",
      relatedChildOperations = {
        org.praxisplatform.uischema.surface.RelatedResourceChildOperation.FILTER,
        org.praxisplatform.uischema.surface.RelatedResourceChildOperation.LIST
      },
      requiredAuthorities = {"ERGON_READ"})
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Consultar parcelas de gozo de férias do vínculo",
      description =
          "Lista parcelas de programação e gozo de férias vinculadas ao assentamento funcional. A paginação é limitada a 200 registros por resposta.")
  public org.springframework.http.ResponseEntity<
          org.praxisplatform.uischema.rest.response.RestApiResponse<
              org.springframework.data.domain.Page<FeriasGozoResponseDTO>>>
      feriasGozo(
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
    ErgonHttpContract.childPage(page, size);
    return filterRelatedChildResource(id, feriasGozoService, ApiPaths.FERIAS_GOZO, FeriasGozoFilterDTO::new,
        (child, parent) -> { child.setVinculoId(parent); }, page, size);
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
    return ErgonHttpContract.itemBody(executeVersionedItemCommand("registrar-vacancia", id, ifMatch, null, dto,
        condition -> service.registrarVacancia(id, dto, condition)));
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
    return ErgonHttpContract.itemBody(executeVersionedItemCommand("registrar-cessao", id, ifMatch, null, dto,
        condition -> commands.registrarCessao(id, dto, condition)));
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
    return ErgonHttpContract.itemBody(executeVersionedItemCommand("encerrar-cessao", id, ifMatch, null, dto,
        condition -> commands.encerrarCessao(id, dto, condition)));
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
    return ErgonHttpContract.itemBody(executeVersionedItemCommand("conceder-aposentadoria", id, ifMatch, null, dto,
        condition -> commands.concederAposentadoria(id, dto, condition)));
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
    return ErgonHttpContract.itemBody(executeVersionedItemCommand("homologar-estagio-probatorio", id, ifMatch, null, dto,
        condition -> commands.homologarEstagio(id, dto, condition)));
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
    return ErgonHttpContract.itemBody(executeVersionedItemPatch("alterar-lotacao-exercicio", id, ifMatch, condition -> commands.alterarLotacaoExercicio(id, dto, condition)));
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
    return ErgonHttpContract.itemBody(executeVersionedItemPatch("atualizar-previdencia-pagamento", id, ifMatch, condition -> commands.atualizarPrevidenciaPagamento(id, dto, condition)));
  }

  @org.springframework.web.bind.annotation.PostMapping("/{id}/actions/registrar-afastamento")
  @WorkflowAction(
      id = "registrar-afastamento",
      title = "Registrar afastamento",
      description =
          "Concede licença homologada conforme as regras de concomitância do órgão. Vínculo ativo passa a afastado; cessão vigente é preservada.",
      scope = org.praxisplatform.uischema.action.ActionScope.ITEM,
      requiredAuthorities = {"ERGON_WRITE"},
      allowedStates = {"ATIVO", "AFASTADO", "CEDIDO"},
      confirmationRequired = true,
      resourceVersion = org.praxisplatform.uischema.action.ActionRequirement.REQUIRED,
      resourceVersionTransport =
          org.praxisplatform.uischema.action.ActionResourceVersionTransport.IF_MATCH)
  @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ERGON_WRITE')")
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Registrar afastamento",
      description =
          "Concede licença homologada conforme as regras de concomitância do órgão. Vínculo ativo passa a afastado; cessão vigente é preservada. Exige If-Match atual.")
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
    return ErgonHttpContract.itemBody(executeVersionedItemCommand("registrar-afastamento", id, ifMatch, null, dto,
        condition -> commands.registrarAfastamento(id, dto, condition)));
  }

  @org.springframework.web.bind.annotation.PostMapping("/{id}/actions/encerrar-afastamento")
  @WorkflowAction(
      id = "encerrar-afastamento",
      title = "Registrar retorno",
      description =
          "Encerra a licença homologada selecionada. O vínculo afastado volta a ativo quando não resta licença aberta; cessão vigente é preservada.",
      scope = org.praxisplatform.uischema.action.ActionScope.ITEM,
      requiredAuthorities = {"ERGON_WRITE"},
      allowedStates = {"AFASTADO", "CEDIDO"},
      confirmationRequired = true,
      resourceVersion = org.praxisplatform.uischema.action.ActionRequirement.REQUIRED,
      resourceVersionTransport =
          org.praxisplatform.uischema.action.ActionResourceVersionTransport.IF_MATCH)
  @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ERGON_WRITE')")
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Registrar retorno",
      description =
          "Encerra a licença homologada selecionada. O vínculo afastado volta a ativo quando não resta licença aberta; cessão vigente é preservada. Exige If-Match atual.")
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
    return ErgonHttpContract.itemBody(executeVersionedItemCommand("encerrar-afastamento", id, ifMatch, null, dto,
        condition -> commands.encerrarAfastamento(id, dto, condition)));
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

  @Override
  @PostMapping("/bulk/update/evaluation")
  @Operation(operationId="ergon.vinculos.bulk.uniform.evaluation", summary="Avaliar edição uniforme",description="Avalia seleção, versões e alterações sem modificar o cadastro; produz uma proposta protegida.")
  @BulkResourceOperation(BulkResourceOperation.Role.UNIFORM_EVALUATION)
  public org.springframework.http.ResponseEntity<RestApiResponse<BulkProposal>> evaluateBulkUpdate(@RequestBody BulkUniformEvaluationRequest<?, VinculoFilterDTO> request) {
    return super.evaluateBulkUpdate(request);
  }
  @Override
  @PostMapping("/bulk/update-items/evaluation")
  @Operation(operationId="ergon.vinculos.bulk.items.evaluation",summary="Avaliar edições por registro",description="Avalia deltas distintos e a versão esperada de cada registro, com impedimentos individualizados.")
  @BulkResourceOperation(BulkResourceOperation.Role.ITEM_EVALUATION)
  public org.springframework.http.ResponseEntity<RestApiResponse<BulkProposal>> evaluateBulkUpdateItems(@RequestBody BulkItemEvaluationRequest<?> request) {
    return super.evaluateBulkUpdateItems(request);
  }
  @Override
  @PostMapping("/bulk/update")
  @BulkOperation(mode=BulkMode.UNIFORM_UPDATE,evaluationOperationId="ergon.vinculos.bulk.uniform.evaluation",atomicity=ActionCollectionAtomicity.PER_ITEM)
  @Operation(operationId="ergon.vinculos.bulk.uniform",summary="Confirmar edição uniforme",description="Confirma uma proposta avaliada; cada registro é revalidado e possui recibo durável, sem repetir efeitos no replay.")
  @BulkResourceOperation(BulkResourceOperation.Role.UNIFORM_CONFIRMATION)
  public org.springframework.http.ResponseEntity<RestApiResponse<BulkExecution>> confirmBulkUpdate(@RequestBody BulkConfirmationRequest request, @RequestHeader("Idempotency-Key") String key) {
    return super.confirmBulkUpdate(request, key);
  }
  @Override
  @PostMapping("/bulk/update-items")
  @BulkOperation(mode=BulkMode.PER_ITEM_UPDATE,evaluationOperationId="ergon.vinculos.bulk.items.evaluation",atomicity=ActionCollectionAtomicity.PER_ITEM)
  @Operation(operationId="ergon.vinculos.bulk.items",summary="Confirmar edições por registro",description="Aplica os deltas individualizados preservados na proposta, com versão e resultado próprios por registro.")
  @BulkResourceOperation(BulkResourceOperation.Role.ITEM_CONFIRMATION)
  public org.springframework.http.ResponseEntity<RestApiResponse<BulkExecution>> confirmBulkUpdateItems(@RequestBody BulkConfirmationRequest request, @RequestHeader("Idempotency-Key") String key) {
    return super.confirmBulkUpdateItems(request, key);
  }






  @PostMapping("/bulk/realocar-setor-lote/evaluation")
  @Operation(operationId="ergon.vinculos.realocar-setor-lote.evaluation",summary="Avaliar ato coletivo",description="Realoca vínculos ativos ou cedidos por portaria do mesmo órgão, preservando versão e recibo por registro. A avaliação não grava alterações funcionais.")
  public org.springframework.http.ResponseEntity<RestApiResponse<BulkProposal>> bulkRealocarEvaluate( @RequestBody BulkCommandEvaluationRequest<RealocarSetorLoteDTO, ?,VinculoFilterDTO> request) {
    return evaluateBulkDomainCommand("realocar-setor-lote", request);
  }
  @PostMapping("/actions/realocar-setor-lote")
  @WorkflowAction(id="realocar-setor-lote",title="Realocar setor em lote",description="Realoca vínculos ativos ou cedidos por portaria do mesmo órgão, preservando versão e recibo por registro.",scope=ActionScope.COLLECTION,
      requiredAuthorities={"ERGON_WRITE"},confirmationRequired=true,idempotencyKey=ActionRequirement.REQUIRED,
      atomicity=ActionCollectionAtomicity.PER_ITEM,outcomeMode=ActionOutcomeMode.PER_ITEM,maxSelection=200)
  @BulkOperation(mode=BulkMode.DOMAIN_COMMAND,evaluationOperationId="ergon.vinculos.realocar-setor-lote.evaluation",atomicity=ActionCollectionAtomicity.PER_ITEM)
  @Operation(operationId="ergon.vinculos.realocar-setor-lote",summary="Realocar setor em lote",description="Realoca vínculos ativos ou cedidos por portaria do mesmo órgão, preservando versão e recibo por registro.")
  public org.springframework.http.ResponseEntity<RestApiResponse<BulkExecution>> bulkRealocarConfirm(@RequestBody BulkConfirmationRequest request,
      @Parameter(description="Chave estável do ato coletivo para impedir efeitos duplicados.") @RequestHeader("Idempotency-Key") String key) {
    return confirmBulkDomainCommand("realocar-setor-lote", request, key);
  }

  @PostMapping("/bulk/homologar-estagio-lote/evaluation")
  @Operation(operationId="ergon.vinculos.homologar-estagio-lote.evaluation",summary="Avaliar ato coletivo",description="Homologa estágios ainda pendentes de vínculos ativos, com data coerente e documento do órgão. A avaliação não grava alterações funcionais.")
  public org.springframework.http.ResponseEntity<RestApiResponse<BulkProposal>> bulkHomologarEvaluate( @RequestBody BulkCommandEvaluationRequest<HomologarEstagioLoteDTO, ?,VinculoFilterDTO> request) {
    return evaluateBulkDomainCommand("homologar-estagio-lote", request);
  }
  @PostMapping("/actions/homologar-estagio-lote")
  @WorkflowAction(id="homologar-estagio-lote",title="Homologar estágio em lote",description="Homologa estágios ainda pendentes de vínculos ativos, com data coerente e documento do órgão.",scope=ActionScope.COLLECTION,
      requiredAuthorities={"ERGON_WRITE"},confirmationRequired=true,idempotencyKey=ActionRequirement.REQUIRED,
      atomicity=ActionCollectionAtomicity.PER_ITEM,outcomeMode=ActionOutcomeMode.PER_ITEM,maxSelection=200)
  @BulkOperation(mode=BulkMode.DOMAIN_COMMAND,evaluationOperationId="ergon.vinculos.homologar-estagio-lote.evaluation",atomicity=ActionCollectionAtomicity.PER_ITEM)
  @Operation(operationId="ergon.vinculos.homologar-estagio-lote",summary="Homologar estágio em lote",description="Homologa estágios ainda pendentes de vínculos ativos, com data coerente e documento do órgão.")
  public org.springframework.http.ResponseEntity<RestApiResponse<BulkExecution>> bulkHomologarConfirm(@RequestBody BulkConfirmationRequest request,
      @Parameter(description="Chave estável do ato coletivo para impedir efeitos duplicados.") @RequestHeader("Idempotency-Key") String key) {
    return confirmBulkDomainCommand("homologar-estagio-lote", request, key);
  }

  @org.springframework.beans.factory.annotation.Autowired private AverbacaoTempoServicoService averbacaoTempoServicoChildren;
  @GetMapping("/{id}/averbacoes-tempo-servico-vinculo") @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ERGON_READ')")
  @Operation(operationId = "childrenVinculoAverbacaoTempoServico", summary = "Averbações do vínculo", description = "Consulta pelo par completo pessoa e vínculo, validado no escopo de órgão; número de vínculo isolado não identifica o pai.")
  @UiSurface(id = "averbacoes-tempo-servico-vinculo", kind = org.praxisplatform.uischema.surface.SurfaceKind.READ_PROJECTION, scope = org.praxisplatform.uischema.surface.SurfaceScope.ITEM, title = "Averbações do vínculo", relatedChildResourceKey = ApiPaths.KEY_AVERBACOES_TEMPO_SERVICO, relatedChildResourcePath = ApiPaths.AVERBACOES_TEMPO_SERVICO, relatedChildParentField = "vinculoId", relatedChildOperations = {org.praxisplatform.uischema.surface.RelatedResourceChildOperation.FILTER, org.praxisplatform.uischema.surface.RelatedResourceChildOperation.LIST}, requiredAuthorities = {"ERGON_READ"})
  public org.springframework.http.ResponseEntity<RestApiResponse<org.springframework.data.domain.Page<AverbacaoTempoServicoResponseDTO>>> childrenAverbacaoTempoServico(@PathVariable VinculoId id,
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    ErgonHttpContract.childPage(page, size);
    return filterRelatedChildResource(id, averbacaoTempoServicoChildren, ApiPaths.AVERBACOES_TEMPO_SERVICO, AverbacaoTempoServicoFilterDTO::new,
        (child, parent) -> { child.setVinculoId(parent); child.setNumfunc(parent.numfunc()); child.setNumvinc(parent.numero()); }, page, size);
  }

  @org.springframework.beans.factory.annotation.Autowired private PeriodoAquisitivoLicencaEspecialService periodoAquisitivoLicencaEspecialChildren;
  @GetMapping("/{id}/periodos-aquisitivos-licenca-especial-vinculo") @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ERGON_READ')")
  @Operation(operationId = "childrenVinculoPeriodoAquisitivoLicencaEspecial", summary = "Licença especial do vínculo", description = "Consulta pelo par completo pessoa e vínculo, validado no escopo de órgão; número de vínculo isolado não identifica o pai.")
  @UiSurface(id = "periodos-aquisitivos-licenca-especial-vinculo", kind = org.praxisplatform.uischema.surface.SurfaceKind.READ_PROJECTION, scope = org.praxisplatform.uischema.surface.SurfaceScope.ITEM, title = "Licença especial do vínculo", relatedChildResourceKey = ApiPaths.KEY_PERIODOS_AQUISITIVOS_LICENCA_ESPECIAL, relatedChildResourcePath = ApiPaths.PERIODOS_AQUISITIVOS_LICENCA_ESPECIAL, relatedChildParentField = "vinculoId", relatedChildOperations = {org.praxisplatform.uischema.surface.RelatedResourceChildOperation.FILTER, org.praxisplatform.uischema.surface.RelatedResourceChildOperation.LIST}, requiredAuthorities = {"ERGON_READ"})
  public org.springframework.http.ResponseEntity<RestApiResponse<org.springframework.data.domain.Page<PeriodoAquisitivoLicencaEspecialResponseDTO>>> childrenPeriodoAquisitivoLicencaEspecial(@PathVariable VinculoId id,
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    ErgonHttpContract.childPage(page, size);
    return filterRelatedChildResource(id, periodoAquisitivoLicencaEspecialChildren, ApiPaths.PERIODOS_AQUISITIVOS_LICENCA_ESPECIAL, PeriodoAquisitivoLicencaEspecialFilterDTO::new,
        (child, parent) -> { child.setVinculoId(parent); child.setNumfunc(parent.numfunc()); child.setNumvinc(parent.numero()); }, page, size);
  }
  @Autowired private DesignacaoFuncaoService designacoesChildren;

  @GetMapping("/{id}/designacoes-vinculo")
  @PreAuthorize("hasAuthority('ERGON_READ')")
  @Operation(operationId = "childrenVinculoDesignacaoFuncao", summary = "Designações do vínculo",
      description = "Atos em que o vínculo identificado pelo par de número funcional e número do vínculo é o titular da função. A consulta respeita o órgão corrente.")
  @UiSurface(id = "designacoes-vinculo", kind = SurfaceKind.READ_PROJECTION, scope = SurfaceScope.ITEM,
      title = "Designações do vínculo", relatedChildResourceKey = ApiPaths.KEY_DESIGNACOES,
      relatedChildResourcePath = ApiPaths.DESIGNACOES,
      relatedChildParentField = "titularId",
      relatedChildOperations = {RelatedResourceChildOperation.FILTER, RelatedResourceChildOperation.LIST},
      requiredAuthorities = {"ERGON_READ"})
  public ResponseEntity<RestApiResponse<Page<DesignacaoFuncaoResponseDTO>>> childrenDesignacaoFuncao(
      @Parameter(description = "Identidade composta pelo número funcional e pelo número do vínculo.")
      @PathVariable VinculoId id,
      @Parameter(description = "Índice da página, começando em zero.")
      @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "Quantidade de assentamentos por página, de 1 a 200.")
      @RequestParam(defaultValue = "20") int size) {
    ErgonHttpContract.childPage(page, size);
    return filterRelatedChildResource(id, designacoesChildren, ApiPaths.DESIGNACOES,
        DesignacaoFuncaoFilterDTO::new, DesignacaoFuncaoFilterDTO::setTitularId, page, size);
  }

  @Autowired private SubstituicaoFuncaoService substituicoesChildren;

  @GetMapping("/{id}/substituicoes-vinculo")
  @PreAuthorize("hasAuthority('ERGON_READ')")
  @Operation(operationId = "childrenVinculoSubstituicaoFuncao", summary = "Substituições do vínculo",
      description = "Períodos em que o vínculo identificado pelo par de número funcional e número do vínculo assume a função interinamente. A consulta respeita o órgão corrente.")
  @UiSurface(id = "substituicoes-vinculo", kind = SurfaceKind.READ_PROJECTION, scope = SurfaceScope.ITEM,
      title = "Substituições do vínculo", relatedChildResourceKey = ApiPaths.KEY_SUBSTITUICOES,
      relatedChildResourcePath = ApiPaths.SUBSTITUICOES,
      relatedChildParentField = "substitutoId",
      relatedChildOperations = {RelatedResourceChildOperation.FILTER, RelatedResourceChildOperation.LIST},
      requiredAuthorities = {"ERGON_READ"})
  public ResponseEntity<RestApiResponse<Page<SubstituicaoFuncaoResponseDTO>>> childrenSubstituicaoFuncao(
      @Parameter(description = "Identidade composta pelo número funcional e pelo número do vínculo.")
      @PathVariable VinculoId id,
      @Parameter(description = "Índice da página, começando em zero.")
      @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "Quantidade de assentamentos por página, de 1 a 200.")
      @RequestParam(defaultValue = "20") int size) {
    ErgonHttpContract.childPage(page, size);
    return filterRelatedChildResource(id, substituicoesChildren, ApiPaths.SUBSTITUICOES,
        SubstituicaoFuncaoFilterDTO::new, SubstituicaoFuncaoFilterDTO::setSubstitutoId, page, size);
  }

  @Override @SuppressWarnings({"unchecked", "rawtypes"})
  protected org.praxisplatform.uischema.bulk.JpaResourceBulkEngine getBulkEngine() { return bulk.engine(Resource.VINCULOS); }
  @Override protected String bulkProposalOperationId(UUID id) { return bulk.locate(Resource.VINCULOS, id, false); }
  @Override protected String bulkExecutionOperationId(UUID id) { return bulk.locate(Resource.VINCULOS, id, true); }

}
