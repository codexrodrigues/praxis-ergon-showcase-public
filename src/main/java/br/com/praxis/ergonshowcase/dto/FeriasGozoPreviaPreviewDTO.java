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
        "Resultado da determinação reativa da parcela de férias com projeção do período aquisitivo e duração calculada.")
public class FeriasGozoPreviaPreviewDTO {
  @Schema(description = "Chave sequencial de negócio do período aquisitivo (CHAVEPERAQFER).")
  private Integer chavePeraqfer;

  @Schema(description = "Total de dias de direito no período aquisitivo (TOTDIAS).")
  private Integer totDiasDireitoPa;

  @Schema(description = "Quantidade calculada de dias de gozo na parcela.")
  private Integer diasGozo;

  @Schema(description = "Data de término previsto calculada.")
  private LocalDate dtprevfim;
}
