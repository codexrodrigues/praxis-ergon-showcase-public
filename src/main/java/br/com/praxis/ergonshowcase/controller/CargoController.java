package br.com.praxis.ergonshowcase.controller;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.domain.*;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.service.*;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.controller.base.AbstractResourceController;

/** Publica o contrato de cargos através da hierarquia canônica Praxis. */
@org.praxisplatform.uischema.fact.ResourceFact(
    key = "cargoExtinto",
    resourceKey = "administracao-pessoal.cargos",
    label = "Indica existência de extinção registrada para o cargo",
    description = "Indica existência de extinção registrada para o cargo.",
    dataType = "boolean",
    sourceKind = "computed")
@org.praxisplatform.uischema.fact.ResourceFact(
    key = "saldoVagasDisponiveis",
    unit = "positions",
    resourceKey = "administracao-pessoal.cargos",
    label =
        "Vagas autorizadas menos vínculos ativos, afastados ou cedidos; nulo quando não há quantitativo autorizado",
    description =
        "Vagas autorizadas menos vínculos ativos, afastados ou cedidos; nulo quando não há quantitativo autorizado.",
    dataType = "integer",
    sourceKind = "computed")
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
    extends AbstractResourceController<
        CargoResponseDTO, Integer, CargoFilterDTO, CreateCargoDTO, UpdateCargoDTO> {
  private final CargoService service;

  public CargoController(CargoService service) {
    this.service = service;
  }

  @Override
  protected CargoService getService() {
    return service;
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
      relatedChildResourcePath = "/api/ergon/vinculos",
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
                org.springframework.hateoas.Link.of("/api/ergon/vinculos", "collection"))));
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
      relatedChildResourcePath = "/api/ergon/vinculos",
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
                org.springframework.hateoas.Link.of("/api/ergon/vinculos", "collection"))));
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
}
