package br.com.praxis.ergonshowcase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import org.praxisplatform.uischema.extension.annotation.UISchema;

public record ConcederAposentadoriaDTO(
    @NotNull
        @PastOrPresent
        @Schema(description = "Data de concessão da aposentadoria.")
        @UISchema(label = "Data de concessão da aposentadoria", group = "Fundamentação do ato")
        LocalDate dtaposent,
    @NotBlank
        @Schema(description = "Modalidade jurídica da aposentadoria concedida.")
        @UISchema(
            label = "Modalidade jurídica da aposentadoria concedida",
            group = "Fundamentação do ato")
        String tipoapos,
    @NotNull
        @DecimalMin("0")
        @DecimalMax("1")
        @Schema(description = "Fração proporcional do benefício, entre zero e um.")
        @UISchema(
            label = "Fração proporcional do benefício, entre zero e um",
            group = "Fundamentação do ato")
        java.math.BigDecimal fatorProporcAposent,
    @NotNull
        @Schema(description = "Documento legal que fundamenta a concessão.")
        @UISchema(
            label = "Documento legal que fundamenta a concessão",
            group = "Fundamentação do ato")
        Long documentoLegalId) {}
