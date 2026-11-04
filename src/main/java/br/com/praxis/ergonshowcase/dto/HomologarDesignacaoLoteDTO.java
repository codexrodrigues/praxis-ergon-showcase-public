package br.com.praxis.ergonshowcase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDate;

@Schema(description = "Registra o início efetivo de exercício das designações selecionadas no mesmo órgão.")
public record HomologarDesignacaoLoteDTO(
    @NotNull @PastOrPresent
    @Schema(description = "Primeiro dia em que os titulares assumiram a função. A data é aplicada aos atos aprovados no lote.")
    LocalDate dtExercicio) {}
