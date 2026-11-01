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
        "Manutenção parcial dos overrides individuais do período aquisitivo (Tarefa 24610 do PCK_FERIAS).")
public class AtualizarOverridesPeriodoAquisitivoDTO {

  @Min(1)
  @Max(6)
  @Schema(description = "Override individual do máximo de parcelas neste período aquisitivo.")
  @UISchema(
      label = "Override máx. parcelas",
      helpText = "Sobrescreve o máximo de parcelas ou vazio para herdar.",
      group = "Overrides Individuais",
      order = 10,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "tune")
  private Integer maxPerFer;

  @Min(1)
  @Max(6)
  @Schema(description = "Override individual do mínimo de parcelas neste período aquisitivo.")
  @UISchema(
      label = "Override mín. parcelas",
      helpText = "Sobrescreve o mínimo de parcelas ou vazio para herdar.",
      group = "Overrides Individuais",
      order = 20,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "tune")
  private Integer minPerFer;

  @Min(1)
  @Max(90)
  @Schema(description = "Override individual do máximo de dias por parcela.")
  @UISchema(
      label = "Override máx. dias parcela",
      helpText = "Sobrescreve o teto de dias por parcela neste PA.",
      group = "Overrides Individuais",
      order = 30,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "tune")
  private Integer maxDiasPerFer;

  @Min(1)
  @Max(90)
  @Schema(description = "Override individual do mínimo de dias por parcela.")
  @UISchema(
      label = "Override mín. dias parcela",
      helpText = "Sobrescreve o mínimo de dias por parcela neste PA.",
      group = "Overrides Individuais",
      order = 40,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "tune")
  private Integer minDiasPerFer;

  @Min(1)
  @Max(90)
  @Schema(description = "Override individual do mínimo de dias na parcela principal.")
  @UISchema(
      label = "Override mín. principal",
      helpText = "Sobrescreve o mínimo de dias na parcela principal.",
      group = "Overrides Individuais",
      order = 50,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "tune")
  private Integer minDiasPerPrincFer;

  @Pattern(regexp = "I|D|G|N")
  @Schema(
      description = "Override da base de abono pecuniário: I (herda), D, G ou N.",
      allowableValues = {"I", "D", "G", "N"})
  @UISchema(
      label = "Override base de abono",
      helpText = "Informe I (herda), D (direito), G (gozo) ou N (sem abono).",
      group = "Overrides Individuais",
      order = 60,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      icon = "tune")
  private String abonoDirGozo;

  @Pattern(regexp = "I|S|N")
  @Schema(
      description = "Override do terço constitucional: I (herda), S ou N.",
      allowableValues = {"I", "S", "N"})
  @UISchema(
      label = "Override terço constitucional",
      helpText = "Informe I (herda), S (concede) ou N (veda).",
      group = "Overrides Individuais",
      order = 70,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      icon = "tune")
  private String abonoConstFer;

  @Pattern(regexp = "I|S|N")
  @Schema(
      description = "Override de adiantamento de 13º salário: I (herda), S ou N.",
      allowableValues = {"I", "S", "N"})
  @UISchema(
      label = "Override adiantamento 13º",
      helpText = "Informe I (herda), S (permite) ou N (bloqueia).",
      group = "Overrides Individuais",
      order = 80,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      icon = "tune")
  private String adiant13Fer;

  @Pattern(regexp = "I|S|N")
  @Schema(
      description = "Override de adiantamento salarial: I (herda), S ou N.",
      allowableValues = {"I", "S", "N"})
  @UISchema(
      label = "Override adiant. salarial",
      helpText = "Informe I (herda), S (permite) ou N (bloqueia).",
      group = "Overrides Individuais",
      order = 90,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      icon = "tune")
  private String adiantSalFer;

  @Pattern(regexp = "I|S|N")
  @Schema(
      description = "Override de empréstimo de férias: I (herda), S ou N.",
      allowableValues = {"I", "S", "N"})
  @UISchema(
      label = "Override empréstimo",
      helpText = "Informe I (herda), S (permite) ou N (bloqueia).",
      group = "Overrides Individuais",
      order = 100,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      icon = "tune")
  private String emprestFer;

  @Pattern(regexp = "I|S|N")
  @Schema(
      description = "Override de gozo antes do fim deste PA: I (herda), S ou N.",
      allowableValues = {"I", "S", "N"})
  @UISchema(
      label = "Override gozo antecipado",
      helpText = "Informe I (herda), S (permite) ou N (bloqueia).",
      group = "Overrides Individuais",
      order = 110,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      icon = "tune")
  private String permiteFerEstePer;

  @Min(0)
  @Max(90)
  @Schema(description = "Override do máximo de dias vendáveis em abono pecuniário.")
  @UISchema(
      label = "Override máx. venda abono",
      helpText = "Sobrescreve o teto de dias vendáveis em abono.",
      group = "Overrides Individuais",
      order = 120,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "tune")
  private Integer maxDiasVendFer;

  @Min(0)
  @Max(90)
  @Schema(description = "Override do mínimo de dias para venda de abono pecuniário.")
  @UISchema(
      label = "Override mín. venda abono",
      helpText = "Sobrescreve o mínimo de dias para venda de abono.",
      group = "Overrides Individuais",
      order = 130,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "tune")
  private Integer minDiasPvendFer;

  @Min(0)
  @Max(90)
  @Schema(description = "Override do numerador da fração de abono pecuniário.")
  @UISchema(
      label = "Override numerador abono",
      helpText = "Sobrescreve o numerador da fração de abono.",
      group = "Overrides Individuais",
      order = 140,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "tune")
  private Integer numDiasAbonoFer;

  @Min(1)
  @Max(90)
  @Schema(description = "Override do denominador da fração de abono pecuniário.")
  @UISchema(
      label = "Override denominador abono",
      helpText = "Sobrescreve o denominador da fração de abono.",
      group = "Overrides Individuais",
      order = 150,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "tune")
  private Integer denDiasAbonoFer;

  @Pattern(regexp = "I|S|N")
  @Schema(
      description = "Override de contagem em dobro de férias: I (herda), S ou N.",
      allowableValues = {"I", "S", "N"})
  @UISchema(
      label = "Override contagem em dobro",
      helpText = "Informe I (herda), S (permite) ou N (bloqueia).",
      group = "Overrides Individuais",
      order = 160,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      icon = "tune")
  private String contDobroFerias;

  @Min(0)
  @Max(90)
  @Schema(description = "Override do mínimo de dias para contagem em dobro.")
  @UISchema(
      label = "Override mín. dias dobro",
      helpText = "Sobrescreve o mínimo de dias para contagem em dobro.",
      group = "Overrides Individuais",
      order = 170,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "tune")
  private Integer minDiasPeraqfer;

  @Min(0)
  @Max(90)
  @Schema(description = "Override do máximo de dias para contagem em dobro.")
  @UISchema(
      label = "Override máx. dias dobro",
      helpText = "Sobrescreve o máximo de dias para contagem em dobro.",
      group = "Overrides Individuais",
      order = 180,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "tune")
  private Integer maxDiasPeraqfer;
}
