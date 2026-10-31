package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import org.praxisplatform.uischema.*;
import org.praxisplatform.uischema.extension.annotation.UISchema;

@Schema(
    description =
        "Comando de reativação da vigência de dependência mediante comprovação documental (ex.: matrícula universitária ou decisão judicial).")
public record ReativarDependenciaDTO(
    @NotNull
        @Positive
        @Schema(description = "Documento comprobatório obrigatório que fundamenta a reativação da dependência.")
        @UISchema(
            label = "Documento comprobatório",
            group = "Reativação",
            order = 1,
            type = FieldDataType.NUMBER,
            controlType = FieldControlType.ENTITY_LOOKUP,
            icon = "description",
            endpoint = ApiPaths.DOCUMENTO_OPTIONS,
            valueField = "id",
            displayField = "label")
        Long documentoLegalId,
    @NotNull
        @Schema(description = "Indica se a reativação decorre de matrícula em ensino superior ou técnico.")
        @UISchema(
            label = "Universitário",
            group = "Reativação",
            order = 2,
            type = FieldDataType.BOOLEAN,
            controlType = FieldControlType.CHECKBOX,
            icon = "school")
        Boolean universitario,
    @NotNull
        @Schema(description = "Restabelece a dedução de IRRF para o dependente reativado.")
        @UISchema(
            label = "Deduz IRRF",
            group = "Reativação",
            order = 3,
            type = FieldDataType.BOOLEAN,
            controlType = FieldControlType.CHECKBOX,
            icon = "receipt_long")
        Boolean depIr) {}
