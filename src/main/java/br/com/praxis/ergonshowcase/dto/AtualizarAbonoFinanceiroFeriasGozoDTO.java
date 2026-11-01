package br.com.praxis.ergonshowcase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

@Getter
@Setter
@Schema(
    description =
        "Manutenção parcial de venda de abono pecuniário, datas financeiras e opções de adiantamento da parcela de férias.")
public class AtualizarAbonoFinanceiroFeriasGozoDTO {

  @NotNull
  @Schema(description = "Indica se há venda de abono pecuniário nesta parcela.")
  @UISchema(
      label = "Com abono pecuniário",
      helpText = "Habilita conversão de dias em abono pecuniário.",
      group = "Abono e Financeiro",
      order = 10,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "sell")
  private Boolean abonoPecuniario = false;

  @Pattern(regexp = "I|F")
  @Schema(
      description = "Posição cronológica do abono pecuniário: I (início) ou F (final).",
      allowableValues = {"I", "F"})
  @UISchema(
      label = "Posição do abono (I/F)",
      helpText = "Informe I (início das férias) ou F (final das férias).",
      group = "Abono e Financeiro",
      order = 20,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      icon = "swap_horiz")
  private String iniFimAbono;

  @NotNull
  @Min(0)
  @Max(90)
  @Schema(description = "Quantidade de dias vendidos em abono pecuniário.")
  @UISchema(
      label = "Dias de abono vendidos",
      helpText = "Quantidade de dias convertidos em pecúnia.",
      group = "Abono e Financeiro",
      order = 30,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "payments")
  private Integer diasAbonoVendidos = 0;

  @Schema(description = "Data de protocolo ou requerimento da venda do abono.")
  @UISchema(
      label = "Data da venda do abono",
      helpText = "Data da opção formal pelo abono pecuniário.",
      group = "Abono e Financeiro",
      order = 40,
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "event")
  private LocalDate dtVendaAbono;

  @Schema(description = "Data programada de pagamento do abono pecuniário.")
  @UISchema(
      label = "Pagamento do abono",
      helpText = "Data de liquidação financeira do abono pecuniário.",
      group = "Abono e Financeiro",
      order = 50,
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "paid")
  private LocalDate dtPagtoAbono;

  @NotNull
  @Schema(description = "Indica pagamento do terço constitucional nesta parcela.")
  @UISchema(
      label = "Paga terço constitucional",
      helpText = "Comanda o pagamento do adicional de 1/3.",
      group = "Abono e Financeiro",
      order = 60,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "account_balance")
  private Boolean abonoConstitucional = true;

  @NotNull
  @Schema(description = "Indica opção por adiantamento de 13º salário.")
  @UISchema(
      label = "Adiantamento de 13º",
      helpText = "Comanda antecipação de 13º salário.",
      group = "Abono e Financeiro",
      order = 70,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "payments")
  private Boolean adiantamento13 = false;

  @NotNull
  @Schema(description = "Indica opção por adiantamento salarial de férias.")
  @UISchema(
      label = "Adiantamento salarial",
      helpText = "Comanda antecipação salarial de férias.",
      group = "Abono e Financeiro",
      order = 80,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "payments")
  private Boolean adiantamentoSalario = false;

  @NotNull
  @Schema(description = "Indica opção por empréstimo financeiro de férias.")
  @UISchema(
      label = "Empréstimo de férias",
      helpText = "Comanda rubrica de empréstimo de férias.",
      group = "Abono e Financeiro",
      order = 90,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "account_balance")
  private Boolean emprestimoFerias = false;
}
