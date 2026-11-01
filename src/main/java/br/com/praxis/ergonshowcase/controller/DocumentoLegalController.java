package br.com.praxis.ergonshowcase.controller;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.service.*;
import io.swagger.v3.oas.annotations.*;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.controller.base.AbstractReadOnlyResourceController;
import org.praxisplatform.uischema.fact.ResourceFact;
import org.praxisplatform.uischema.surface.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Consulta auxiliar que sustenta opções e referências 360° dos pilotos. */
@ResourceFact(
    key = "anoDocumento",
    resourceKey = "administracao-pessoal.documentos-legais",
    label = "Ano de edição do ato",
    description =
        "Exercício oficial de edição ou numeração do ato administrativo ou diploma legal.",
    dataType = "integer",
    icon = "gavel",
    tone = "neutral",
    valuePresentation = "badge",
    sourceKind = "field")
@ApiResource(
    value = ApiPaths.DOCUMENTOS_LEGAIS,
    resourceKey = "administracao-pessoal.documentos-legais",
    title = "Documentos legais",
    description = "Consulta do cadastro auxiliar para inspeção das referências funcionais.",
    identity =
        @ResourceIdentity(
            keyField = "id",
            titleField = "ementa",
            subtitleField = "tipoDocumento",
            metadataFields = {"empCodigo", "numeroDocumento"}),
    quickFilters = {
      @QuickFilter(id = "publicados", label = "Atos publicados", filter = "{\"publicados\":true}"),
      @QuickFilter(id = "sem-anexo", label = "Sem anexo digital", filter = "{\"semAnexo\":true}")
    })
@ApiGroup(
    value = "ergon",
    title = "Administração de pessoal",
    description = "Cadastros civis e assentamentos funcionais dos órgãos públicos.")
public class DocumentoLegalController
    extends AbstractReadOnlyResourceController<
        DocumentoLegalResponseDTO, Long, DocumentoLegalFilterDTO> {
  private final DocumentoLegalService service;

  public DocumentoLegalController(DocumentoLegalService service) {
    this.service = service;
  }

  @Override
  protected DocumentoLegalService getService() {
    return service;
  }

  @Override
  protected Long getResponseId(DocumentoLegalResponseDTO dto) {
    return dto.getId();
  }

  @GetMapping("/{id}/ficha-documento")
  @PreAuthorize("hasAuthority('ERGON_READ')")
  @UiSurface(
      id = "ficha-documento",
      kind = SurfaceKind.READ_PROJECTION,
      scope = SurfaceScope.ITEM,
      title = "Ficha do documento",
      description = "Consulta autenticada do registro referenciado e sua fundamentação.",
      requiredAuthorities = {"ERGON_READ"})
  @Operation(
      summary = "Consultar ficha auxiliar",
      description = "Apresenta o cadastro de origem sem disponibilizar operações de escrita.")
  public ResponseEntity<DocumentoLegalResponseDTO> ficha(
      @Parameter(description = "Identidade do registro auxiliar") @PathVariable Long id) {
    return withResourceVersion(ResponseEntity.ok(), id, service.findById(id));
  }
}
