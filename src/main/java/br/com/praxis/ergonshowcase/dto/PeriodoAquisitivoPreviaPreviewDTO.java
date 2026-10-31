package br.com.praxis.ergonshowcase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    description =
        "Resultado da determinação reativa de saldo disponível e prescrição do período aquisitivo.")
public class PeriodoAquisitivoPreviaPreviewDTO {
  @Schema(description = "Saldo de dias disponível calculado.")
  private Integer saldoDiasDisponivel;

  @Schema(description = "Data de prescrição sugerida após o término do período aquisitivo.")
  private LocalDate dtPrescricao;
}
