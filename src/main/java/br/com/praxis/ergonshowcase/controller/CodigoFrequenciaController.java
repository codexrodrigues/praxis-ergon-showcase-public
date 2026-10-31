package br.com.praxis.ergonshowcase.controller;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.domain.CodigoFrequenciaId;
import br.com.praxis.ergonshowcase.dto.CodigoFrequenciaFilterDTO;
import br.com.praxis.ergonshowcase.dto.CodigoFrequenciaResponseDTO;
import br.com.praxis.ergonshowcase.service.CodigoFrequenciaService;
import io.swagger.v3.oas.annotations.Operation;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.controller.base.AbstractReadOnlyResourceController;
import org.praxisplatform.uischema.surface.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@ApiResource(
    value = ApiPaths.CODIGOS_FREQUENCIA,
    resourceKey = "administracao-pessoal.codigos-frequencia",
    title = "Códigos de frequência",
    description =
        "Catálogo detalhado de ocorrências de frequência, faltas, abonos e modalidades de licença/afastamento (CODIGOS_FREQ_).",
    icon = "event_note",
    identity =
        @ResourceIdentity(
            keyField = "id",
            titleField = "nome",
            displayLabelField = "nome",
            subtitleField = "tipoFrequenciaNome",
            metadataFields = {"tipofreq", "codigo", "mnemonico", "afastamento"}),
    quickFilters = {
      @QuickFilter(
          id = "afastamentos",
          label = "Licenças e afastamentos",
          filter = "{\"somenteAfastamento\":true}"),
      @QuickFilter(
          id = "via-frequencia",
          label = "Via frequência",
          filter = "{\"somenteViaFrequencia\":true}"),
      @QuickFilter(
          id = "com-onus",
          label = "Com ônus",
          filter = "{\"somenteComOnus\":true}")
    })
@ApiGroup(
    value = "ergon",
    title = "Administração de pessoal",
    description = "Cadastros civis e assentamentos funcionais dos órgãos públicos.",
    icon = "groups")
public class CodigoFrequenciaController
    extends AbstractReadOnlyResourceController<
        CodigoFrequenciaResponseDTO, CodigoFrequenciaId, CodigoFrequenciaFilterDTO> {
  @Autowired private CodigoFrequenciaService service;
  @Autowired private br.com.praxis.ergonshowcase.service.RegraFrequenciaEstatutariaService regrasService;

  @Override
  protected CodigoFrequenciaService getService() {
    return service;
  }

  @Override
  protected CodigoFrequenciaId getResponseId(CodigoFrequenciaResponseDTO dto) {
    return dto.getId();
  }

  @GetMapping("/{id}/ficha-codigo-frequencia")
  @PreAuthorize("hasAuthority('ERGON_READ')")
  @Operation(
      summary = "Consultar ficha do código de frequência",
      description =
          "Retorna a projeção de leitura do código de frequência ou licença com regras operacionais, ônus e limites.")
  @UiSurface(
      id = "ficha-codigo-frequencia",
      kind = SurfaceKind.READ_PROJECTION,
      scope = SurfaceScope.ITEM,
      title = "Ficha do código de frequência",
      description = "Regras de lançamento, ônus, limites estatutários e fundamento legal do código.",
      intent = "summary",
      requiredAuthorities = {"ERGON_READ"})
  public ResponseEntity<CodigoFrequenciaResponseDTO> ficha(@PathVariable CodigoFrequenciaId id) {
    return withResourceVersion(ResponseEntity.ok(), id, service.findById(id));
  }

  @GetMapping("/{id}/regras")
  @PreAuthorize("hasAuthority('ERGON_READ')")
  @UiSurface(
      id = "regras-codigo-frequencia",
      kind = SurfaceKind.READ_PROJECTION,
      scope = SurfaceScope.ITEM,
      title = "Regras estatutárias aplicáveis",
      description = "Parâmetros e limites estatutários configurados para este código de frequência ou afastamento.",
      relatedChildResourceKey = "administracao-pessoal.regras-frequencia-estatutaria",
      relatedChildResourcePath = ApiPaths.REGRAS_FREQUENCIA_ESTATUTARIA,
      relatedChildParentField = "codigoFrequenciaId",
      relatedChildOperations = {
        RelatedResourceChildOperation.FILTER,
        RelatedResourceChildOperation.LIST
      },
      requiredAuthorities = {"ERGON_READ"})
  @Operation(
      summary = "Consultar regras estatutárias do código de frequência",
      description =
          "Lista as regras estatutárias de duração, interstício e limite configuradas para este código. Paginação limitada a 200 registros.")
  public ResponseEntity<
          org.praxisplatform.uischema.rest.response.RestApiResponse<
              org.springframework.data.domain.Page<
                  br.com.praxis.ergonshowcase.dto.RegraFrequenciaEstatutariaResponseDTO>>>
      regras(
          @PathVariable CodigoFrequenciaId id,
          @RequestParam(defaultValue = "0") int page,
          @RequestParam(defaultValue = "20") int size) {
    service.findById(id);
    if (page < 0 || size < 1 || size > 200)
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY,
          "Paginação fora do limite permitido.");
    var filter = new br.com.praxis.ergonshowcase.dto.RegraFrequenciaEstatutariaFilterDTO();
    filter.setCodigoFrequenciaId(id);
    var result =
        regrasService.filter(
            filter,
            org.springframework.data.domain.PageRequest.of(
                page, size, regrasService.getDefaultSort()),
            null);
    return ResponseEntity.ok(
        org.praxisplatform.uischema.rest.response.RestApiResponse.success(
            result,
            org.springframework.hateoas.Links.of(
                org.springframework.hateoas.Link.of(
                    ApiPaths.REGRAS_FREQUENCIA_ESTATUTARIA, "collection"))));
  }
}
