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
}
