package br.com.praxis.ergonshowcase.controller;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.dto.TipoFrequenciaFilterDTO;
import br.com.praxis.ergonshowcase.dto.TipoFrequenciaResponseDTO;
import br.com.praxis.ergonshowcase.service.TipoFrequenciaService;
import io.swagger.v3.oas.annotations.Operation;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.controller.base.AbstractReadOnlyResourceController;
import org.praxisplatform.uischema.fact.ResourceFact;
import org.praxisplatform.uischema.surface.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@ResourceFact(
    key = "influiConta",
    resourceKey = "administracao-pessoal.tipos-frequencia",
    label = "Impacta contagem de tempo",
    description =
        "Indica se as ocorrências desta natureza repercutem na apuração de tempo de serviço e férias.",
    dataType = "boolean",
    icon = "calculate",
    tone = "warning",
    valuePresentation = "badge",
    sourceKind = "field")
@ResourceFact(
    key = "horaExtra",
    resourceKey = "administracao-pessoal.tipos-frequencia",
    label = "Natureza de sobrejornada",
    description =
        "Indica se o tipo de frequência classifica eventos de serviço extraordinário ou banco de horas.",
    dataType = "boolean",
    icon = "more_time",
    tone = "info",
    valuePresentation = "badge",
    sourceKind = "field")
@ApiResource(
    value = ApiPaths.TIPOS_FREQUENCIA,
    resourceKey = "administracao-pessoal.tipos-frequencia",
    title = "Tipos de frequência",
    description =
        "Catálogo de naturezas macro de ocorrências de frequência e afastamento (TIPO_FREQ_).",
    icon = "schedule",
    identity =
        @ResourceIdentity(
            keyField = "id",
            titleField = "nome",
            displayLabelField = "nome",
            subtitleField = "id",
            metadataFields = {"influiConta", "horaExtra"}),
    quickFilters = {
      @QuickFilter(
          id = "influi-contagem",
          label = "Influi na contagem",
          filter = "{\"somenteInfluiConta\":true}"),
      @QuickFilter(
          id = "hora-extra",
          label = "Hora extra",
          filter = "{\"somenteHoraExtra\":true}")
    })
@ApiGroup(
    value = "ergon",
    title = "Administração de pessoal",
    description = "Cadastros civis e assentamentos funcionais dos órgãos públicos.",
    icon = "groups")
public class TipoFrequenciaController
    extends AbstractReadOnlyResourceController<
        TipoFrequenciaResponseDTO, String, TipoFrequenciaFilterDTO> {
  @Autowired private TipoFrequenciaService service;
  @Autowired private br.com.praxis.ergonshowcase.service.CodigoFrequenciaService codigosService;
  @Autowired private br.com.praxis.ergonshowcase.service.RegraFrequenciaEstatutariaService regrasService;

  @Override
  protected TipoFrequenciaService getService() {
    return service;
  }

  @Override
  protected String getResponseId(TipoFrequenciaResponseDTO dto) {
    return dto.getId();
  }

  @GetMapping("/{id}/ficha-tipo-frequencia")
  @PreAuthorize("hasAuthority('ERGON_READ')")
  @Operation(
      summary = "Consultar ficha da natureza de frequência",
      description =
          "Retorna a projeção de leitura do tipo macro de frequência com reflexos em contagem de tempo e sobrejornada.")
  @UiSurface(
      id = "ficha-tipo-frequencia",
      kind = SurfaceKind.READ_PROJECTION,
      scope = SurfaceScope.ITEM,
      title = "Ficha do tipo de frequência",
      description = "Regras de contagem de tempo, sobrejornada e fundamento legal da natureza.",
      intent = "summary",
      requiredAuthorities = {"ERGON_READ"})
  public ResponseEntity<TipoFrequenciaResponseDTO> ficha(@PathVariable String id) {
    return withResourceVersion(ResponseEntity.ok(), id, service.findById(id));
  }

  @GetMapping("/{id}/codigos")
  @PreAuthorize("hasAuthority('ERGON_READ')")
  @UiSurface(
      id = "codigos-tipo-frequencia",
      kind = SurfaceKind.READ_PROJECTION,
      scope = SurfaceScope.ITEM,
      title = "Códigos de frequência vinculados",
      description = "Códigos específicos de frequência, falta, abono ou afastamento pertencentes a esta natureza.",
      relatedChildResourceKey = "administracao-pessoal.codigos-frequencia",
      relatedChildResourcePath = ApiPaths.CODIGOS_FREQUENCIA,
      relatedChildParentField = "tipofreq",
      relatedChildOperations = {
        RelatedResourceChildOperation.FILTER,
        RelatedResourceChildOperation.LIST
      },
      requiredAuthorities = {"ERGON_READ"})
  @Operation(
      summary = "Consultar códigos de frequência vinculados ao tipo",
      description =
          "Lista os códigos de frequência ou afastamento subordinados a esta natureza macro. Paginação limitada a 200 registros.")
  public ResponseEntity<
          org.praxisplatform.uischema.rest.response.RestApiResponse<
              org.springframework.data.domain.Page<
                  br.com.praxis.ergonshowcase.dto.CodigoFrequenciaResponseDTO>>>
      codigos(
          @PathVariable String id,
          @RequestParam(defaultValue = "0") int page,
          @RequestParam(defaultValue = "20") int size) {
    service.findById(id);
    if (page < 0 || size < 1 || size > 200)
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY,
          "Paginação fora do limite permitido.");
    var filter = new br.com.praxis.ergonshowcase.dto.CodigoFrequenciaFilterDTO();
    filter.setTipofreq(id);
    var result =
        codigosService.filter(
            filter,
            org.springframework.data.domain.PageRequest.of(
                page, size, codigosService.getDefaultSort()),
            null);
    return ResponseEntity.ok(
        org.praxisplatform.uischema.rest.response.RestApiResponse.success(
            result,
            org.springframework.hateoas.Links.of(
                org.springframework.hateoas.Link.of(ApiPaths.CODIGOS_FREQUENCIA, "collection"))));
  }

  @GetMapping("/{id}/regras")
  @PreAuthorize("hasAuthority('ERGON_READ')")
  @UiSurface(
      id = "regras-tipo-frequencia",
      kind = SurfaceKind.READ_PROJECTION,
      scope = SurfaceScope.ITEM,
      title = "Regras estatutárias vinculadas",
      description = "Parâmetros e limites estatutários configurados para códigos desta natureza de frequência.",
      relatedChildResourceKey = "administracao-pessoal.regras-frequencia-estatutaria",
      relatedChildResourcePath = ApiPaths.REGRAS_FREQUENCIA_ESTATUTARIA,
      relatedChildParentField = "tipofreq",
      relatedChildOperations = {
        RelatedResourceChildOperation.FILTER,
        RelatedResourceChildOperation.LIST
      },
      requiredAuthorities = {"ERGON_READ"})
  @Operation(
      summary = "Consultar regras estatutárias vinculadas ao tipo de frequência",
      description =
          "Lista as regras estatutárias de frequência e afastamento associadas a esta natureza. Paginação limitada a 200 registros.")
  public ResponseEntity<
          org.praxisplatform.uischema.rest.response.RestApiResponse<
              org.springframework.data.domain.Page<
                  br.com.praxis.ergonshowcase.dto.RegraFrequenciaEstatutariaResponseDTO>>>
      regras(
          @PathVariable String id,
          @RequestParam(defaultValue = "0") int page,
          @RequestParam(defaultValue = "20") int size) {
    service.findById(id);
    if (page < 0 || size < 1 || size > 200)
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY,
          "Paginação fora do limite permitido.");
    var filter = new br.com.praxis.ergonshowcase.dto.RegraFrequenciaEstatutariaFilterDTO();
    filter.setTipofreq(id);
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
