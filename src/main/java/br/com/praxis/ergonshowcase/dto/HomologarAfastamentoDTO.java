package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

/** Comando de homologação formal de licença ou afastamento pendente. */
public record HomologarAfastamentoDTO(
    @NotNull
        @Positive
        @Schema(
            description =
                "Portaria de concessão ou laudo pericial oficial que fundamenta a homologação do afastamento.")
        @UISchema(
            label = "Portaria ou laudo legal",
            helpText = "Ato legal do mesmo órgão que homologa a concessão.",
            group = "Homologação",
            order = 10,
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
            description = "Ato de publicação oficial no Diário Oficial referente à homologação.")
        @UISchema(
            label = "Ato de publicação oficial",
            helpText = "Publicação em Diário Oficial vinculada à homologação.",
            group = "Homologação",
            order = 20,
            type = FieldDataType.NUMBER,
            controlType = FieldControlType.ENTITY_LOOKUP,
            icon = "newspaper",
            endpoint = ApiPaths.DOCUMENTO_OPTIONS,
            valueField = "id",
            displayField = "label")
        Long atoPublicacaoId,
    @Schema(
            nullable = true,
            description =
                "Data de término previsto ratificada no despacho de homologação; quando informada, atualiza a previsão do período.")
        @UISchema(
            label = "Término previsto homologado",
            helpText = "Data final prevista ratificada pela junta médica ou chefia.",
            group = "Homologação",
            order = 30,
            type = FieldDataType.DATE,
            controlType = FieldControlType.DATE_PICKER,
            icon = "event")
        LocalDate dtprevfim) {}
