package br.com.praxis.ergonshowcase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

/** Entradas de cálculo não persistente; a identidade opcional de edição é resolvida e autorizada no servidor. */
public final class TempoLicencaPrevia {
  private TempoLicencaPrevia() {}
  public record Averbacao(
      @NotNull @Min(0) @Schema(description="Dias certificados antes da desaverbação.") Integer totalDias,
      @NotNull @Min(0) @Schema(description="Dias retirados do aproveitamento da certidão.") Integer diasDesaverbados) {}
  public record Periodo(
      @NotNull @Min(0) @Max(999) @Schema(description="Direito original do novo ciclo de licença, antes de parcelas.") Integer diasDireito,
      @NotNull @Min(0) @Schema(description="Dias originais que se pretende converter em dobro, sem duplicar o débito no saldo.") Integer diasConvertidosDobro) {}
  public record Gozo(
      @NotNull @Schema(description="Primeiro dia da parcela de fruição.") LocalDate dtini,
      @NotNull @Schema(description="Último dia previsto da parcela de fruição.") LocalDate dtprevfim) {}
  public record Venda(
      @NotNull @Schema(description="Período aquisitivo que custeia a venda, consultado no órgão autorizado.") Long periodoAquisitivoLicencaId,
      @NotNull @Min(1) @Max(999) @Schema(description="Dias originais pretendidos para a venda avulsa.") Integer diasVendidos,
      @Schema(description="Identidade da venda em edição; nula na criação. O servidor verifica órgão, período e modalidade e recupera seu consumo anterior, sem aceitar crédito informado pelo cliente.", nullable=true) Long vendaId) {}
  public record Resultado(@Schema(description="Resultado em dias originais: tempo líquido, saldo inicial, duração inclusiva ou saldo após venda, conforme a operação de prévia.",accessMode=Schema.AccessMode.READ_ONLY) Integer diasPrevistos) {}
}
