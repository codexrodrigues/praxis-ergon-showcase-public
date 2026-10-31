package br.com.praxis.ergonshowcase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
        "Resultado da determinação reativa da matriz de parâmetros de contagem de férias.")
public class ParametroContagemFeriasPreviaPreviewDTO {
  @Schema(description = "Teto calculado de dias conversíveis em abono pecuniário.")
  private Integer maxDiasVendFer;

  @Schema(description = "Duração mínima recomendada para a parcela principal.")
  private Integer minDiasPerPrincFer;
}
