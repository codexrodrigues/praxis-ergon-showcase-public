package br.com.praxis.ergonshowcase.controller;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.domain.*;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.service.*;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.controller.base.AbstractResourceController;

/** Publica o contrato de órgãos através da hierarquia canônica Praxis. */
@ApiResource(
    quickFilters = {
      @QuickFilter(id = "ativos", label = "Órgãos ativos", filter = "{\"ativa\":true}"),
      @QuickFilter(id = "inativos", label = "Órgãos inativos", filter = "{\"ativa\":false}")
    },
    value = ApiPaths.ORGAOS,
    resourceKey = "administracao-pessoal.empresas",
    title = "Órgãos",
    description =
        "Entes públicos responsáveis pelos quadros funcionais e pela administração de pessoal.",
    icon = "apartment",
    identity = @ResourceIdentity(keyField = "id", titleField = "nome", displayLabelField = "nome"))
@ApiGroup(
    value = "ergon",
    title = "Administração de pessoal",
    description = "Cadastros civis e assentamentos funcionais dos órgãos públicos.",
    icon = "groups")
public class OrgaoController
    extends AbstractResourceController<
        OrgaoResponseDTO, Integer, OrgaoFilterDTO, CreateOrgaoDTO, UpdateOrgaoDTO> {
  private final OrgaoService service;

  public OrgaoController(OrgaoService service) {
    this.service = service;
  }

  @Override
  protected OrgaoService getService() {
    return service;
  }

  @Override
  protected Integer getResponseId(OrgaoResponseDTO dto) {
    return dto.getId();
  }
}
