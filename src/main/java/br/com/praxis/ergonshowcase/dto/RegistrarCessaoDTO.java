package br.com.praxis.ergonshowcase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import org.praxisplatform.uischema.extension.annotation.UISchema;

public record RegistrarCessaoDTO(
    @NotBlank
        @Schema(description = "Modalidade administrativa da cessão.")
        @UISchema(label = "Modalidade administrativa da cessão", group = "Fundamentação do ato")
        String tipoReq,
    @NotBlank
        @Schema(description = "Órgão para o qual o servidor será cedido.")
        @UISchema(
            label = "Órgão para o qual o servidor será cedido",
            group = "Fundamentação do ato")
        String orgaoReq,
    @NotBlank
        @Schema(description = "Função a exercer no órgão cessionário.")
        @UISchema(label = "Função a exercer no órgão cessionário", group = "Fundamentação do ato")
        String funcaoReq,
    @NotBlank
        @Schema(description = "Responsável pelo ônus financeiro da cessão.")
        @UISchema(
            label = "Responsável pelo ônus financeiro da cessão",
            group = "Fundamentação do ato")
        String tipoOnusReq,
    @Schema(description = "Forma de ressarcimento entre cedente e cessionário.")
        @UISchema(
            label = "Forma de ressarcimento entre cedente e cessionário",
            group = "Fundamentação do ato")
        String tipoRessarcReq,
    @NotNull
        @PastOrPresent
        @Schema(description = "Data inicial da cessão, igual ou posterior ao exercício.")
        @UISchema(
            label = "Data inicial da cessão, igual ou posterior ao exercício",
            group = "Fundamentação do ato")
        LocalDate dtiniCessao,
    @Schema(description = "Término previsto da cessão, igual ou posterior ao início.")
        @UISchema(
            label = "Término previsto da cessão, igual ou posterior ao início",
            group = "Fundamentação do ato")
        LocalDate dtfimCessao) {}
