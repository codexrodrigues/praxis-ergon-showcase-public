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
        "Resultado da determinação reativa da parcela de férias com projeção do período aquisitivo, duração, abono sugerido e saldo remanescente.")
public class FeriasGozoPreviaPreviewDTO {
  @Schema(description = "Chave sequencial de negócio do período aquisitivo (CHAVEPERAQFER).")
  private Integer chavePeraqfer;

  @Schema(description = "Total de dias de direito no período aquisitivo (TOTDIAS).")
  private Integer totDiasDireitoPa;

  @Schema(description = "Quantidade calculada de dias de gozo na parcela.")
  private Integer diasGozo;

  @Schema(description = "Data de término previsto calculada.")
  private LocalDate dtprevfim;

  @Schema(description = "Ordem sequencial sugerida para a parcela dentro do período aquisitivo.")
  private Integer ordemParcela;

  @Schema(
      description =
          "Quantidade sugerida ou efetiva de dias de abono pecuniário segundo a fração parametrizada (NUM_DIAS_ABONO_FER / DEN_DIAS_ABONO_FER).")
  private Integer diasAbonoVendidos;

  @Schema(description = "Data sugerida de pagamento do abono pecuniário antecedendo o início do gozo.")
  private LocalDate dtPagtoAbono;

  @Schema(
      description =
          "Saldo projetado de dias remanescentes no período aquisitivo após o débito desta parcela e de seu abono.")
  private Integer saldoPaAposParcela;

  public FeriasGozoPreviaPreviewDTO(
      Integer chavePeraqfer, Integer totDiasDireitoPa, Integer diasGozo, LocalDate dtprevfim) {
    this(chavePeraqfer, totDiasDireitoPa, diasGozo, dtprevfim, null, null, null, null);
  }
}
