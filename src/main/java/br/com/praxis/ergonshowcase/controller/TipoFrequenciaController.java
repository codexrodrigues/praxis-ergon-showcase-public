package br.com.praxis.ergonshowcase.controller;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.dto.TipoFrequenciaFilterDTO;
import br.com.praxis.ergonshowcase.dto.TipoFrequenciaResponseDTO;
import br.com.praxis.ergonshowcase.service.TipoFrequenciaService;
import io.swagger.v3.oas.annotations.Operation;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.controller.base.AbstractReadOnlyResourceController;
import org.praxisplatform.uischema.surface.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
}
