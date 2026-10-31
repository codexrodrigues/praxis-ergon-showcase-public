package br.com.praxis.ergonshowcase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

@Getter
@Setter
@Schema(
    description =
        "Manutenção parcial dos limites de parcelamento, venda de abono pecuniário e rubricas financeiras da regra de férias.")
public class AtualizarParcelamentoAbonoParametroFeriasDTO {

  @NotNull
  @Schema(description = "Indica se o enquadramento confere direito a férias periódicas.")
  @UISchema(
      label = "Possui direito a férias",
      helpText = "Habilita aquisição e gozo de férias para este enquadramento.",
      group = "Direito e Parcelamento",
      order = 10,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "beach_access")
  private Boolean temFerias = true;

  @Min(0)
  @Max(90)
  @Schema(description = "Total padrão de dias de direito a férias por período aquisitivo.")
  @UISchema(
      label = "Total de dias de direito",
      helpText = "Quantidade padrão de dias de férias por ciclo completo.",
      group = "Direito e Parcelamento",
      order = 20,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "calendar_month")
  private Integer totDiasFer;

  @Min(1)
  @Max(6)
  @Schema(description = "Quantidade mínima de parcelas para fruição das férias.")
  @UISchema(
      label = "Mínimo de parcelas",
      helpText = "Menor número de etapas de gozo exigido.",
      group = "Direito e Parcelamento",
      order = 30,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "splitscreen")
  private Integer minPerFer;

  @Min(1)
  @Max(6)
  @Schema(description = "Quantidade máxima de parcelas permitida no período aquisitivo.")
  @UISchema(
      label = "Máximo de parcelas",
      helpText = "Limite máximo de etapas para fracionamento das férias.",
      group = "Direito e Parcelamento",
      order = 40,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "splitscreen")
  private Integer maxPerFer;

  @Min(1)
  @Max(90)
  @Schema(description = "Quantidade mínima de dias exigida em cada parcela de férias.")
  @UISchema(
      label = "Mínimo de dias por parcela",
      helpText = "Menor duração permitida para qualquer fração de férias.",
      group = "Direito e Parcelamento",
      order = 50,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "timer")
  private Integer minDiasPerFer;

  @Min(1)
  @Max(90)
  @Schema(description = "Quantidade máxima de dias permitida em uma parcela de férias.")
  @UISchema(
      label = "Máximo de dias por parcela",
      helpText = "Maior duração permitida para uma parcela individual.",
      group = "Direito e Parcelamento",
      order = 60,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "timer")
  private Integer maxDiasPerFer;

  @Min(1)
  @Max(90)
  @Schema(description = "Quantidade mínima de dias exigida na parcela principal.")
  @UISchema(
      label = "Mínimo na parcela principal",
      helpText = "Duração mínima obrigatória na parcela principal.",
      group = "Direito e Parcelamento",
      order = 70,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "verified")
  private Integer minDiasPerPrincFer;

  @Pattern(regexp = "D|G|N")
  @Schema(description = "Base de cálculo do abono pecuniário: D (direito), G (gozo) ou N (vedado).")
  @UISchema(
      label = "Base do abono pecuniário",
      helpText = "Informe D (direito), G (gozo) ou N (sem abono pecuniário).",
      group = "Abono e Financeiro",
      order = 80,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      icon = "sell")
  private String abonoDirGozo;

  @Min(0)
  @Max(90)
  @Schema(description = "Numerador da fração regulamentar de abono pecuniário.")
  @UISchema(
      label = "Numerador da fração de abono",
      helpText = "Numerador da proporção máxima conversível em pecúnia.",
      group = "Abono e Financeiro",
      order = 90,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "percent")
  private Integer numDiasAbonoFer;

  @Min(1)
  @Max(90)
  @Schema(description = "Denominador da fração regulamentar de abono pecuniário.")
  @UISchema(
      label = "Denominador da fração de abono",
      helpText = "Denominador da proporção máxima conversível em pecúnia.",
      group = "Abono e Financeiro",
      order = 100,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "percent")
  private Integer denDiasAbonoFer;

  @Min(0)
  @Max(90)
  @Schema(description = "Quantidade mínima de dias para venda de abono pecuniário.")
  @UISchema(
      label = "Mínimo de dias para venda",
      helpText = "Menor quantidade de dias conversível em abono pecuniário.",
      group = "Abono e Financeiro",
      order = 110,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "payments")
  private Integer minDiasPvendFer;

  @Min(0)
  @Max(90)
  @Schema(description = "Quantidade máxima de dias vendidos em abono pecuniário.")
  @UISchema(
      label = "Máximo de dias vendidos",
      helpText = "Teto de dias conversíveis em abono pecuniário.",
      group = "Abono e Financeiro",
      order = 120,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "payments")
  private Integer maxDiasVendFer;

  @NotNull
  @Schema(description = "Indica se faz jus ao terço constitucional de férias.")
  @UISchema(
      label = "Terço constitucional",
      helpText = "Habilita pagamento do adicional constitucional de 1/3.",
      group = "Abono e Financeiro",
      order = 130,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "account_balance")
  private Boolean abonoConstFer = true;

  @NotNull
  @Schema(description = "Indica se permite adiantamento de décimo terceiro salário.")
  @UISchema(
      label = "Permite adiantamento de 13º",
      helpText = "Autoriza antecipação da gratificação natalina nas férias.",
      group = "Abono e Financeiro",
      order = 140,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "payments")
  private Boolean adiant13Fer = true;

  @NotNull
  @Schema(description = "Indica se permite adiantamento salarial de férias.")
  @UISchema(
      label = "Permite adiantamento salarial",
      helpText = "Autoriza antecipação salarial de férias na folha.",
      group = "Abono e Financeiro",
      order = 150,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "payments")
  private Boolean adiantSalFer = false;

  @NotNull
  @Schema(description = "Indica se permite contratação de empréstimo de férias.")
  @UISchema(
      label = "Permite empréstimo de férias",
      helpText = "Autoriza empréstimo restituível vinculado às férias.",
      group = "Abono e Financeiro",
      order = 160,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "account_balance")
  private Boolean emprestFer = false;
}
