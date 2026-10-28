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
    identity =
        @ResourceIdentity(
            keyField = "id",
            titleField = "nome",
            displayLabelField = "nome",
            subtitleField = "nomeSocial",
            metadataFields = {"id", "escolaridade", "cidadeender", "ufender"}))
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

  @org.springframework.web.bind.annotation.GetMapping("/{id}/vinculos-funcionais")
  @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ERGON_READ')")
  @UiSurface(
      id = "vinculos-funcionais",
      kind = org.praxisplatform.uischema.surface.SurfaceKind.READ_PROJECTION,
      scope = org.praxisplatform.uischema.surface.SurfaceScope.ITEM,
      title = "Vínculos funcionais",
      description = "Assentamentos funcionais da pessoa, incluindo vínculos históricos.",
      relatedChildResourceKey = "administracao-pessoal.vinculos",
      relatedChildResourcePath = "/api/ergon/vinculos",
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
    service.findById(id);
    if (page < 0 || size < 1 || size > 200)
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY,
          "Paginação fora do limite permitido.");
    var filter = new VinculoFilterDTO();
    filter.setNumfunc(id);
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
}
