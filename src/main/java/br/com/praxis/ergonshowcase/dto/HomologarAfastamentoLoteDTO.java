package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

/** Parâmetros do comando coletivo de homologação em lote de licenças e afastamentos pendentes. */
public record HomologarAfastamentoLoteDTO(
    @NotNull
        @Positive
        @Schema(
            description =
                "Portaria coletiva ou ata de junta médica do órgão que fundamenta a homologação em lote.")
        @UISchema(
            label = "Portaria ou ata homologatória",
            helpText = "Documento legal do órgão que ampara a homologação coletiva.",
            group = "Homologação em lote",
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
            description = "Ato de publicação oficial em Diário Oficial referente ao lote homologado.")
        @UISchema(
            label = "Ato de publicação oficial",
            helpText = "Publicação no Diário Oficial vinculada ao lote.",
            group = "Homologação em lote",
            order = 20,
            type = FieldDataType.NUMBER,
            controlType = FieldControlType.ENTITY_LOOKUP,
            icon = "newspaper",
            endpoint = ApiPaths.DOCUMENTO_OPTIONS,
            valueField = "id",
            displayField = "label")
        Long atoPublicacaoId,
    @NotBlank
        @Size(max = 500)
        @Schema(
            description =
                "Síntese do despacho homologatório coletivo registrada nos assentamentos selecionados.")
        @UISchema(
            label = "Despacho homologatório",
            helpText = "Texto de despacho anexado aos registros homologados.",
            group = "Homologação em lote",
            order = 30,
            icon = "fact_check")
        String despachoHomologatorio) {}
