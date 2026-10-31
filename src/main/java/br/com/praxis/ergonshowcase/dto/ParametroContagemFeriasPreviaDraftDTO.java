package br.com.praxis.ergonshowcase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

@Getter
@Setter
@Schema(
    description =
        "Rascunho para determinação reativa de limites de abono pecuniário e parcelamento da matriz de férias.")
public class ParametroContagemFeriasPreviaDraftDTO {
  @Schema(description = "Indica se o enquadramento confere direito a férias.")
  @UISchema(
      label = "Possui direito a férias",
      helpText = "Indica se o enquadramento confere direito a férias.",
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "beach_access")
  private Boolean temFerias;

  @Schema(description = "Total padrão de dias de direito a férias.")
  @UISchema(
      label = "Total de dias de férias",
      helpText = "Quantidade padrão de dias de direito por período aquisitivo.",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "calendar_month")
  private Integer totDiasFer;

  @Schema(description = "Base de cálculo do abono pecuniário (D, G ou N).")
  @UISchema(
      label = "Base do abono pecuniário",
      helpText = "Indica D, G ou N para cálculo do abono pecuniário.",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      icon = "sell")
  private String abonoDirGozo;

  @Schema(description = "Numerador da fração de abono pecuniário.")
  @UISchema(
      label = "Numerador da fração abono",
      helpText = "Numerador da fração máxima de conversão em abono.",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "percent")
  private Integer numDiasAbonoFer;

  @Schema(description = "Denominador da fração de abono pecuniário.")
  @UISchema(
      label = "Denominador da fração abono",
      helpText = "Denominador da fração máxima de conversão em abono.",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "percent")
  private Integer denDiasAbonoFer;
}
