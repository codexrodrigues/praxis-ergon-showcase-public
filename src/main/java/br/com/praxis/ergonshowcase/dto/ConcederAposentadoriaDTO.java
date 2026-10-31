package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import org.praxisplatform.uischema.extension.annotation.UISchema;

public record ConcederAposentadoriaDTO(
    @NotNull
        @PastOrPresent
        @Schema(description = "Data de concessão da aposentadoria.")
        @UISchema(label = "Concessão", group = "Fundamentação do ato", type = org.praxisplatform.uischema.FieldDataType.DATE,
            controlType = org.praxisplatform.uischema.FieldControlType.DATE_PICKER, icon = "event")
        LocalDate dtaposent,
    @NotBlank
        @Schema(description = "Modalidade jurídica da aposentadoria concedida.")
        @UISchema(
            label = "Modalidade de aposentadoria",
            group = "Fundamentação do ato")
        String tipoapos,
    @NotNull
        @DecimalMin("0")
        @DecimalMax("1")
        @Schema(description = "Fração proporcional do benefício, entre zero e um.")
        @UISchema(
            label = "Fator de proporcionalidade",
            type = org.praxisplatform.uischema.FieldDataType.NUMBER,
            controlType = org.praxisplatform.uischema.FieldControlType.INPUT, icon = "percent",
            group = "Fundamentação do ato")
        java.math.BigDecimal fatorProporcAposent,
    @NotNull
        @Schema(description = "Documento legal que fundamenta a concessão.")
        @UISchema(
            label = "Ato de concessão",
            type = org.praxisplatform.uischema.FieldDataType.TEXT,
            controlType = org.praxisplatform.uischema.FieldControlType.ENTITY_LOOKUP, icon = "gavel",
            endpoint = ApiPaths.DOCUMENTO_OPTIONS, valueField = "id", displayField = "label",
            group = "Fundamentação do ato")
        Long documentoLegalId) {}
