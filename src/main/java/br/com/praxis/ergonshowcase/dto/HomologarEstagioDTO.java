package br.com.praxis.ergonshowcase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import org.praxisplatform.uischema.extension.annotation.UISchema;

public record HomologarEstagioDTO(
    @NotNull
        @PastOrPresent
        @Schema(description = "Data de homologação do estágio probatório.")
        @UISchema(
            label = "Homologação",
            type = org.praxisplatform.uischema.FieldDataType.DATE,
            controlType = org.praxisplatform.uischema.FieldControlType.DATE_PICKER, icon = "event",
            group = "Fundamentação do ato")
        LocalDate dtHomologEstagio,
    @NotNull
        @Schema(description = "Documento legal que homologa o estágio.")
        @UISchema(label = "Ato de homologação", group = "Fundamentação do ato",
            type = org.praxisplatform.uischema.FieldDataType.TEXT,
            controlType = org.praxisplatform.uischema.FieldControlType.ENTITY_LOOKUP, icon = "gavel",
            endpoint = "/api/ergon/documentos-legais/option-sources/documento/options/filter", valueField = "id", displayField = "label")
        Long documentoLegalId) {}
