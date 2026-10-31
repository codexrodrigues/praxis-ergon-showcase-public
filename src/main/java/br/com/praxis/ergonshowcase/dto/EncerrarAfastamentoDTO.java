package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

/** Retorno vinculado a uma licença homologada aberta do assentamento selecionado. */
public record EncerrarAfastamentoDTO(
    @NotNull
        @Positive
        @Schema(
            description =
                "Identidade da licença homologada aberta deste vínculo que será encerrada.")
        @UISchema(
            label = "Afastamento",
            group = "Retorno",
            type = FieldDataType.NUMBER,
            controlType = FieldControlType.ENTITY_LOOKUP,
            icon = "event_busy",
            endpoint = ApiPaths.AFASTAMENTO_OPTIONS,
            valueField = "id",
            displayField = "label")
        Long afastamentoId,
    @NotNull
        @PastOrPresent
        @Schema(
            description =
                "Último dia efetivo do afastamento, inclusive; não pode anteceder o início.")
        @UISchema(
            type = FieldDataType.DATE,
            controlType = FieldControlType.DATE_PICKER,
            icon = "calendar_today",
            label = "Último dia de afastamento",
            group = "Retorno")
        LocalDate dtfim,
    @NotNull
        @Positive
        @Schema(description = "Documento legal do mesmo órgão que fundamenta o retorno.")
        @UISchema(
            label = "Documento legal de retorno",
            group = "Retorno",
            type = FieldDataType.NUMBER,
            controlType = FieldControlType.ENTITY_LOOKUP,
            icon = "description",
            endpoint = ApiPaths.DOCUMENTO_OPTIONS,
            valueField = "id",
            displayField = "label")
        Long documentoLegalId) {}
