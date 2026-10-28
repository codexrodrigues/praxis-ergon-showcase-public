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
    identity =
        @ResourceIdentity(
            keyField = "id",
            titleField = "nome",
            displayLabelField = "nome",
            subtitleField = "fantasia",
            metadataFields = {"municipio", "ufSigla", "ativa"}))
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

  @org.springframework.beans.factory.annotation.Autowired private ErgonCommands commands;

  @org.springframework.beans.factory.annotation.Autowired
  private org.praxisplatform.uischema.concurrency.ResourceVersionEtagService commandEtags;

  @org.springframework.beans.factory.annotation.Autowired
  private org.praxisplatform.uischema.concurrency.ResourceVersionScopeProvider commandScopes;

  @org.springframework.web.bind.annotation.PostMapping("/{id}/actions/ativar-orgao")
  @WorkflowAction(
      id = "ativar-orgao",
      title = "Ativar órgão",
      description = "Habilita o órgão para novas admissões e operações cadastrais.",
      scope = org.praxisplatform.uischema.action.ActionScope.ITEM,
      requiredAuthorities = {"ERGON_WRITE"},
      allowedStates = {"INATIVO"},
      confirmationRequired = true,
      resourceVersion = org.praxisplatform.uischema.action.ActionRequirement.REQUIRED,
      resourceVersionTransport =
          org.praxisplatform.uischema.action.ActionResourceVersionTransport.IF_MATCH)
  @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ERGON_WRITE')")
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Ativar órgão",
      description =
          "Habilita o órgão para novas admissões e operações cadastrais. Exige If-Match atual.")
  public org.springframework.http.ResponseEntity<OrgaoResponseDTO> ativarOrgao(
      @io.swagger.v3.oas.annotations.Parameter(description = "Identidade imutável do assentamento")
          @org.springframework.web.bind.annotation.PathVariable
          Integer id,
      @io.swagger.v3.oas.annotations.Parameter(description = "ETag forte recebido na leitura atual")
          @org.springframework.web.bind.annotation.RequestHeader(
              value = "If-Match",
              required = false)
          String ifMatch,
      @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody
          AlterarSituacaoOrgaoDTO dto) {
    var p =
        new org.praxisplatform.uischema.concurrency.ResourceVersionUpdatePrecondition<Integer>(
            commandEtags,
            ifMatch,
            commandScopes.currentScope(),
            "administracao-pessoal.empresas",
            id);
    return withResourceVersion(
        org.springframework.http.ResponseEntity.ok(), id, commands.ativarOrgao(id, dto, p));
  }

  @org.springframework.web.bind.annotation.PostMapping("/{id}/actions/inativar-orgao")
  @WorkflowAction(
      id = "inativar-orgao",
      title = "Inativar órgão",
      description = "Desabilita novas admissões sem remover os assentamentos existentes.",
      scope = org.praxisplatform.uischema.action.ActionScope.ITEM,
      requiredAuthorities = {"ERGON_WRITE"},
      allowedStates = {"ATIVO"},
      confirmationRequired = true,
      resourceVersion = org.praxisplatform.uischema.action.ActionRequirement.REQUIRED,
      resourceVersionTransport =
          org.praxisplatform.uischema.action.ActionResourceVersionTransport.IF_MATCH)
  @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ERGON_WRITE')")
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Inativar órgão",
      description =
          "Desabilita novas admissões sem remover os assentamentos existentes. Exige If-Match atual.")
  public org.springframework.http.ResponseEntity<OrgaoResponseDTO> inativarOrgao(
      @io.swagger.v3.oas.annotations.Parameter(description = "Identidade imutável do assentamento")
          @org.springframework.web.bind.annotation.PathVariable
          Integer id,
      @io.swagger.v3.oas.annotations.Parameter(description = "ETag forte recebido na leitura atual")
          @org.springframework.web.bind.annotation.RequestHeader(
              value = "If-Match",
              required = false)
          String ifMatch,
      @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody
          AlterarSituacaoOrgaoDTO dto) {
    var p =
        new org.praxisplatform.uischema.concurrency.ResourceVersionUpdatePrecondition<Integer>(
            commandEtags,
            ifMatch,
            commandScopes.currentScope(),
            "administracao-pessoal.empresas",
            id);
    return withResourceVersion(
        org.springframework.http.ResponseEntity.ok(), id, commands.inativarOrgao(id, dto, p));
  }

  @org.springframework.web.bind.annotation.GetMapping("/{id}/ficha-institucional")
  @UiSurface(
      id = "ficha-institucional",
      kind = org.praxisplatform.uischema.surface.SurfaceKind.READ_PROJECTION,
      scope = org.praxisplatform.uischema.surface.SurfaceScope.ITEM,
      title = "Ficha institucional",
      description = "Consulta identificação, endereço e situação administrativa do órgão.",
      requiredAuthorities = {"ERGON_READ"})
  @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ERGON_READ')")
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Consultar ficha institucional",
      description = "Consulta identificação, endereço e situação administrativa do órgão.")
  public org.springframework.http.ResponseEntity<OrgaoResponseDTO> ficha(
      @io.swagger.v3.oas.annotations.Parameter(description = "Identidade imutável do cadastro")
          @org.springframework.web.bind.annotation.PathVariable
          Integer id) {
    return withResourceVersion(
        org.springframework.http.ResponseEntity.ok(), id, service.findById(id));
  }

  @org.springframework.beans.factory.annotation.Autowired private CargoService relatedService;

  @org.springframework.web.bind.annotation.GetMapping("/{id}/quadro-cargos")
  @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ERGON_READ')")
  @UiSurface(
      id = "quadro-cargos",
      kind = org.praxisplatform.uischema.surface.SurfaceKind.READ_PROJECTION,
      scope = org.praxisplatform.uischema.surface.SurfaceScope.ITEM,
      title = "Quadro de cargos",
      description = "Cargos e funções pertencentes ao quadro do órgão.",
      relatedChildResourceKey = "administracao-pessoal.cargos",
      relatedChildResourcePath = "/api/ergon/cargos",
      relatedChildParentField = "empCodigo",
      relatedChildOperations = {
        org.praxisplatform.uischema.surface.RelatedResourceChildOperation.FILTER,
        org.praxisplatform.uischema.surface.RelatedResourceChildOperation.LIST
      },
      requiredAuthorities = {"ERGON_READ"})
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Consultar quadro de cargos",
      description =
          "Cargos e funções pertencentes ao quadro do órgão. A paginação é limitada a 200 registros por resposta.")
  public org.springframework.http.ResponseEntity<
          org.praxisplatform.uischema.rest.response.RestApiResponse<
              org.springframework.data.domain.Page<CargoResponseDTO>>>
      quadro_cargos(
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
    var filter = new CargoFilterDTO();
    filter.setEmpCodigo(id);
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
                org.springframework.hateoas.Link.of("/api/ergon/cargos", "collection"))));
  }
}
