package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import org.praxisplatform.uischema.*;
import org.praxisplatform.uischema.extension.annotation.UISchema;

@Schema(
    description =
        "Comando de homologação de invalidez permanente atestada por junta médica oficial para isenção de teto etário.")
public record HomologarInvalidezDependenteDTO(
    @NotNull
        @Positive
        @Schema(description = "Laudo médico oficial ou ato normativo que comprova a invalidez permanente.")
        @UISchema(
            label = "Laudo ou ato oficial",
            group = "Homologação médica",
            order = 1,
            type = FieldDataType.NUMBER,
            controlType = FieldControlType.ENTITY_LOOKUP,
            icon = "medical_information",
            endpoint = ApiPaths.DOCUMENTO_OPTIONS,
            valueField = "id",
            displayField = "label")
        Long documentoLegalId,
    @NotNull
        @Schema(description = "Habilita dedução de IRRF vitalícia após homologação da invalidez.")
        @UISchema(
            label = "Habilitar dedução IRRF",
            group = "Homologação médica",
            order = 2,
            type = FieldDataType.BOOLEAN,
            controlType = FieldControlType.CHECKBOX,
            icon = "receipt_long")
        Boolean depIr) {}
