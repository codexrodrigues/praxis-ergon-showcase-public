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
        "Resultado da determinação reativa de término, dias de direito ajustados por faltas, saldo disponível e prescrição do período aquisitivo.")
public class PeriodoAquisitivoPreviaPreviewDTO {
  @Schema(description = "Data final calculada do período aquisitivo após cômputo de prorrogações por suspensão.")
  private LocalDate dtfim;

  @Schema(
      description =
          "Total de dias de direito calculado após aplicação da tabela de redução progressiva por faltas injustificadas (30/24/18/12/0).")
  private Integer totDiasDireito;

  @Schema(description = "Saldo de dias disponível calculado.")
  private Integer saldoDiasDisponivel;

  @Schema(description = "Data de prescrição sugerida após o término do período aquisitivo.")
  private LocalDate dtPrescricao;

  @Schema(
      description =
          "Quantidade de faltas injustificadas apuradas na frequência homologada dentro da janela do período aquisitivo.")
  private Integer faltasApuradas;

  @Schema(
      description =
          "Quantidade de dias de afastamento suspensivo homologados dentro da janela do período aquisitivo.")
  private Integer diasAfastamentoSuspensivo;

  public PeriodoAquisitivoPreviaPreviewDTO(
      LocalDate dtfim,
      Integer totDiasDireito,
      Integer saldoDiasDisponivel,
      LocalDate dtPrescricao) {
    this(dtfim, totDiasDireito, saldoDiasDisponivel, dtPrescricao, null, null);
  }

  public PeriodoAquisitivoPreviaPreviewDTO(Integer saldoDiasDisponivel, LocalDate dtPrescricao) {
    this(null, null, saldoDiasDisponivel, dtPrescricao, null, null);
  }
}
