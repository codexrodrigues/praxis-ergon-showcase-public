package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import org.praxisplatform.uischema.annotation.ResourceReference;
import org.praxisplatform.uischema.extension.annotation.UISchema;

public record ExtinguirCargoDTO(
    @NotNull
        @PastOrPresent
        @Schema(description = "Data de extinção legal do cargo desocupado.")
        @UISchema(
            label = "Extinção",
            type = org.praxisplatform.uischema.FieldDataType.DATE,
            controlType = org.praxisplatform.uischema.FieldControlType.DATE_PICKER, icon = "event",
            group = "Fundamentação do ato")
        LocalDate dtExtincao,
    @Positive
        @Schema(nullable = true, description = "Ato do mesmo órgão que fundamenta a extinção. Quando omitido, o comando conserva o ato já vinculado ao cargo.")
        @UISchema(label = "Ato de extinção", icon = "description", group = "Fundamentação do ato",
            type = org.praxisplatform.uischema.FieldDataType.NUMBER,
            controlType = org.praxisplatform.uischema.FieldControlType.ENTITY_LOOKUP,
            endpoint = ApiPaths.DOCUMENTO_OPTIONS, valueField = "id", displayField = "label")
        @ResourceReference(resourceKey = "administracao-pessoal.documentos-legais", valueField = "documentoLegalId",
            surfaceId = "ficha-documento", previewSurfaceId = "ficha-documento", detailPresentation = "drawer")
        Long documentoLegalId) {
  public ExtinguirCargoDTO(LocalDate dtExtincao) {
    this(dtExtincao, null);
  }
}
