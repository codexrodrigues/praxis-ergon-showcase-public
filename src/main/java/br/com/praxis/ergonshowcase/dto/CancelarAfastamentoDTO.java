package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

/** Comando de cancelamento administrativo de afastamento pendente ou homologado sem efeito consumado. */
public record CancelarAfastamentoDTO(
    @NotBlank
        @Size(max = 2000)
        @Schema(
            description =
                "Justificativa administrativa formal para o cancelamento da licença ou afastamento.")
        @UISchema(
            label = "Justificativa do cancelamento",
            helpText = "Motivo administrativo que determina a tornar sem efeito o registro.",
            group = "Cancelamento",
            order = 10,
            icon = "cancel")
        String motivoCancelamento,
    @NotNull
        @Positive
        @Schema(
            description =
                "Portaria ou despacho administrativo que torna sem efeito ou cancela o afastamento.")
        @UISchema(
            label = "Ato de cancelamento",
            helpText = "Documento legal do órgão que fundamenta o cancelamento.",
            group = "Cancelamento",
            order = 20,
            type = FieldDataType.NUMBER,
            controlType = FieldControlType.ENTITY_LOOKUP,
            icon = "description",
            endpoint = ApiPaths.DOCUMENTO_OPTIONS,
            valueField = "id",
            displayField = "label")
        Long documentoLegalId) {}
