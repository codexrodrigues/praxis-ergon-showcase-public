package br.com.praxis.ergonshowcase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import org.praxisplatform.uischema.extension.annotation.UISchema;

public record HomologarEstagioDTO(
    @NotNull
        @PastOrPresent
        @Schema(description = "Data de homologação do estágio probatório.")
        @UISchema(
            label = "Data de homologação do estágio probatório",
            group = "Fundamentação do ato")
        LocalDate dtHomologEstagio,
    @NotNull
        @Schema(description = "Documento legal que homologa o estágio.")
        @UISchema(label = "Documento legal que homologa o estágio", group = "Fundamentação do ato")
        Long documentoLegalId) {}
