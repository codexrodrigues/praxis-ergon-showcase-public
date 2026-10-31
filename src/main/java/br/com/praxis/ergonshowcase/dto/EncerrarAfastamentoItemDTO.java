package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

/** Comando de encerramento ou retorno antecipado diretamente no registro de afastamento. */
public record EncerrarAfastamentoItemDTO(
    @NotNull
        @Schema(
            description =
                "Último dia efetivo do período de licença ou afastamento; não pode anteceder a data de início.")
        @UISchema(
            label = "Data de término efetivo",
            helpText = "Último dia efetivo da licença antes do retorno ao exercício.",
            group = "Encerramento",
            order = 10,
            type = FieldDataType.DATE,
            controlType = FieldControlType.DATE_PICKER,
            icon = "event_busy")
        LocalDate dtfim,
    @NotNull
        @Positive
        @Schema(
            description =
                "Documento legal ou despacho que formaliza o encerramento ou retorno ao exercício.")
        @UISchema(
            label = "Documento legal de retorno",
            helpText = "Ato legal do mesmo órgão que registra o término do afastamento.",
            group = "Encerramento",
            order = 20,
            type = FieldDataType.NUMBER,
            controlType = FieldControlType.ENTITY_LOOKUP,
            icon = "description",
            endpoint = ApiPaths.DOCUMENTO_OPTIONS,
            valueField = "id",
            displayField = "label")
        Long documentoLegalId,
    @Positive
        @Schema(
            nullable = true,
            description = "Ato de publicação oficial do encerramento ou retorno.")
        @UISchema(
            label = "Ato de publicação oficial",
            helpText = "Publicação em Diário Oficial referente ao retorno.",
            group = "Encerramento",
            order = 30,
            type = FieldDataType.NUMBER,
            controlType = FieldControlType.ENTITY_LOOKUP,
            icon = "newspaper",
            endpoint = ApiPaths.DOCUMENTO_OPTIONS,
            valueField = "id",
            displayField = "label")
        Long atoPublicacaoId) {}
