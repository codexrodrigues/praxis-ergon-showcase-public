package br.com.praxis.ergonshowcase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import org.praxisplatform.uischema.extension.annotation.UISchema;

/** Retorno vinculado a uma licença homologada aberta do assentamento selecionado. */
public record EncerrarAfastamentoDTO(
    @NotNull
        @Positive
        @Schema(
            description =
                "Identidade da licença homologada aberta deste vínculo que será encerrada.")
        @UISchema(label = "Afastamento", group = "Retorno")
        Long afastamentoId,
    @NotNull
        @PastOrPresent
        @Schema(
            description =
                "Último dia efetivo do afastamento, inclusive; não pode anteceder o início.")
        @UISchema(
            type = org.praxisplatform.uischema.FieldDataType.DATE,
            controlType = org.praxisplatform.uischema.FieldControlType.DATE_PICKER,
            icon = "calendar_today",
            label = "Último dia de afastamento",
            group = "Retorno")
        LocalDate dtfim,
    @NotNull
        @Positive
        @Schema(description = "Documento legal do mesmo órgão que fundamenta o retorno.")
        @UISchema(label = "Documento legal de retorno", group = "Retorno")
        Long documentoLegalId) {}
