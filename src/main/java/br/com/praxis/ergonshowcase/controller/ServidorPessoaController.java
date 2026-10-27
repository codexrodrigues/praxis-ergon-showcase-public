package br.com.praxis.ergonshowcase.controller;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.domain.*;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.service.*;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.controller.base.AbstractResourceController;

/** Publica o contrato de pessoas servidoras através da hierarquia canônica Praxis. */
@ApiResource(
    value = ApiPaths.PESSOAS,
    resourceKey = "administracao-pessoal.funcionarios",
    title = "Pessoas servidoras",
    description =
        "Cadastro civil único das pessoas titulares de vínculos funcionais com órgãos públicos.",
    icon = "badge",
    identity = @ResourceIdentity(keyField = "id", titleField = "nome", displayLabelField = "nome"))
@ApiGroup(
    value = "ergon",
    title = "Administração de pessoal",
    description = "Cadastros civis e assentamentos funcionais dos órgãos públicos.",
    icon = "groups")
public class ServidorPessoaController
    extends AbstractResourceController<
        ServidorPessoaResponseDTO,
        Long,
        ServidorPessoaFilterDTO,
        CreateServidorPessoaDTO,
        UpdateServidorPessoaDTO> {
  private final ServidorPessoaService service;

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
}
