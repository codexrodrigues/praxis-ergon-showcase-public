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
}
