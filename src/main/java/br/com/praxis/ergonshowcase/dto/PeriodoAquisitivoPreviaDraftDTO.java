package br.com.praxis.ergonshowcase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
        "Rascunho para determinação reativa de saldo disponível e data de prescrição do período aquisitivo.")
public class PeriodoAquisitivoPreviaDraftDTO {
  @Schema(description = "Data inicial do período aquisitivo.")
  @UISchema(
      label = "Início do período",
      helpText = "Data inicial de apuração do ciclo aquisitivo.",
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "event")
  private LocalDate dtini;

  @Schema(description = "Data final do período aquisitivo.")
  @UISchema(
      label = "Término do período",
      helpText = "Data final de apuração do ciclo aquisitivo.",
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "event_available")
  private LocalDate dtfim;

  @Schema(description = "Total de dias de direito no período aquisitivo.")
  @UISchema(
      label = "Dias de direito",
      helpText = "Total de dias de férias adquiridos no período.",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "beach_access")
  private Integer totDiasDireito;

  @Schema(description = "Total de dias já gozados.")
  @UISchema(
      label = "Dias gozados",
      helpText = "Total de dias já usufruídos pelo servidor.",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "check_circle")
  private Integer diasGozados;

  @Schema(description = "Total de dias vendidos em abono pecuniário.")
  @UISchema(
      label = "Dias em abono pecuniário",
      helpText = "Total de dias convertidos em abono pecuniário.",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "sell")
  private Integer diasVendidosAbono;

  @Schema(description = "Total de dias convertidos em contagem em dobro.")
  @UISchema(
      label = "Dias em contagem em dobro",
      helpText = "Total de dias convertidos para contagem em dobro.",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "trending_up")
  private Integer diasConvertidosDobro;
}
