package br.com.praxis.ergonshowcase.controller;

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
    label = "Idade civil em anos completos na data atual",
    description = "Idade civil em anos completos na data atual.",
    dataType = "integer",
    sourceKind = "computed")
@org.praxisplatform.uischema.fact.ResourceFact(
    key = "possuiDeficienciaPcD",
    resourceKey = "administracao-pessoal.funcionarios",
    label = "Indica ao menos uma das cinco deficiências específicas registrada na pessoa",
    description = "Indica ao menos uma das cinco deficiências específicas registrada na pessoa.",
    dataType = "boolean",
    sourceKind = "computed")
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
    var p =
        new org.praxisplatform.uischema.concurrency.ResourceVersionUpdatePrecondition<Long>(
            commandEtags,
            ifMatch,
            commandScopes.currentScope(),
            "administracao-pessoal.funcionarios",
            id);
    return withResourceVersion(
        org.springframework.http.ResponseEntity.ok(),
        id,
        commands.atualizarDadosBancariosContato(id, dto, p));
  }
}
