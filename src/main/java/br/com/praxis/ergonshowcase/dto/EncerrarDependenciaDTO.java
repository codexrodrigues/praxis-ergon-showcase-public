package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import org.praxisplatform.uischema.*;
import org.praxisplatform.uischema.extension.annotation.UISchema;

@Schema(
    description =
        "Comando de encerramento da vigência de dependência por maioridade, emancipação, divórcio ou óbito.")
public record EncerrarDependenciaDTO(
    @NotNull
        @PastOrPresent
        @Schema(
            description =
                "Data de encerramento da condição de dependência; não pode ser anterior ao início de vigência.")
        @UISchema(
            label = "Data de encerramento",
            group = "Encerramento",
            order = 1,
            type = FieldDataType.DATE,
            controlType = FieldControlType.DATE_PICKER,
            icon = "event_busy")
        LocalDate dtfim,
    @Positive
        @Schema(description = "Documento comprobatório ou averbação que fundamenta o encerramento.")
        @UISchema(
            label = "Documento comprobatório",
            group = "Encerramento",
            order = 2,
            type = FieldDataType.NUMBER,
            controlType = FieldControlType.ENTITY_LOOKUP,
            icon = "description",
            endpoint = ApiPaths.DOCUMENTO_OPTIONS,
            valueField = "id",
            displayField = "label")
        Long documentoLegalId) {}
