package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import org.praxisplatform.uischema.*;
import org.praxisplatform.uischema.extension.annotation.UISchema;

@Schema(
    description =
        "Comando coletivo governado para encerramento em lote de dependências vigentes com registro de ato normativo.")
public record EncerrarDependenciaLoteDTO(
    @NotNull
        @PastOrPresent
        @Schema(description = "Data de encerramento aplicada aos dependentes vigentes selecionados.")
        @UISchema(
            label = "Data de encerramento",
            group = "Encerramento coletivo",
            order = 1,
            type = FieldDataType.DATE,
            controlType = FieldControlType.DATE_PICKER,
            icon = "event_busy")
        LocalDate dtfim,
    @NotNull
        @Positive
        @Schema(description = "Ato administrativo ou despacho de auditoria que fundamenta o encerramento coletivo.")
        @UISchema(
            label = "Ato fundamentador",
            group = "Encerramento coletivo",
            order = 2,
            type = FieldDataType.NUMBER,
            controlType = FieldControlType.ENTITY_LOOKUP,
            icon = "gavel",
            endpoint = ApiPaths.DOCUMENTO_OPTIONS,
            valueField = "id",
            displayField = "label")
        Long documentoLegalId) {}
