package br.com.praxis.ergonshowcase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import org.praxisplatform.uischema.extension.annotation.UISchema;

public record EncerrarCessaoDTO(
    @NotNull
        @PastOrPresent
        @Schema(description = "Data efetiva do encerramento da cessão.")
        @UISchema(label = "Data efetiva do encerramento da cessão", group = "Fundamentação do ato")
        LocalDate dtfimCessao) {}
