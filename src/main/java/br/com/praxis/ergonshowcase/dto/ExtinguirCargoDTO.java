package br.com.praxis.ergonshowcase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import org.praxisplatform.uischema.extension.annotation.UISchema;

public record ExtinguirCargoDTO(
    @NotNull
        @PastOrPresent
        @Schema(description = "Data de extinção legal do cargo desocupado.")
        @UISchema(
            label = "Data de extinção legal do cargo desocupado",
            group = "Fundamentação do ato")
        LocalDate dtExtincao) {}
