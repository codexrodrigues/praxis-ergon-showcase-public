package br.com.praxis.ergonshowcase.controller;

import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.service.*;
import io.swagger.v3.oas.annotations.*;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.controller.base.AbstractReadOnlyResourceController;
import org.praxisplatform.uischema.surface.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Consulta auxiliar que sustenta opções e referências 360° dos pilotos. */
@ApiResource(
    value = "/api/ergon/setores",
    resourceKey = "administracao-pessoal.setores",
    title = "Unidades organizacionais",
    description = "Consulta do cadastro auxiliar para inspeção das referências funcionais.",
    identity =
        @ResourceIdentity(
            keyField = "id",
            titleField = "nome",
            subtitleField = "sigla",
            metadataFields = {"empCodigo", "tipoSetor"}),
    quickFilters = {
      @QuickFilter(id = "vigentes", label = "Unidades vigentes", filter = "{\"vigentes\":true}"),
      @QuickFilter(
          id = "encerrados",
          label = "Unidades encerradas",
          filter = "{\"encerrados\":true}")
    })
@ApiGroup(
    value = "ergon",
    title = "Administração de pessoal",
    description = "Cadastros civis e assentamentos funcionais dos órgãos públicos.")
public class SetorController
    extends AbstractReadOnlyResourceController<SetorResponseDTO, String, SetorFilterDTO> {
  private final SetorService service;

  public SetorController(SetorService service) {
    this.service = service;
  }

  @Override
  protected SetorService getService() {
    return service;
  }

  @Override
  protected String getResponseId(SetorResponseDTO dto) {
    return dto.getId();
  }

  @GetMapping("/{id}/ficha-unidade")
  @PreAuthorize("hasAuthority('ERGON_READ')")
  @UiSurface(
      id = "ficha-unidade",
      kind = SurfaceKind.READ_PROJECTION,
      scope = SurfaceScope.ITEM,
      title = "Ficha da unidade",
      description = "Consulta autenticada do registro referenciado e sua fundamentação.",
      requiredAuthorities = {"ERGON_READ"})
  @Operation(
      summary = "Consultar ficha auxiliar",
      description = "Apresenta o cadastro de origem sem disponibilizar operações de escrita.")
  public ResponseEntity<SetorResponseDTO> ficha(
      @Parameter(description = "Identidade do registro auxiliar") @PathVariable String id) {
    return withResourceVersion(ResponseEntity.ok(), id, service.findById(id));
  }
}
