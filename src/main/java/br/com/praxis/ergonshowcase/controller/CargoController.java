package br.com.praxis.ergonshowcase.controller;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.domain.*;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.service.*;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.controller.base.AbstractResourceController;

/** Publica o contrato de cargos através da hierarquia canônica Praxis. */
@ApiResource(
    value = ApiPaths.CARGOS,
    resourceKey = "administracao-pessoal.cargos",
    title = "Cargos",
    description = "Cargos e funções integrantes dos quadros de pessoal dos órgãos públicos.",
    icon = "badge",
    identity = @ResourceIdentity(keyField = "id", titleField = "nome", displayLabelField = "nome"))
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
}
