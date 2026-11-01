package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.annotation.ResourceReference;
import org.praxisplatform.uischema.extension.annotation.UISchema;

@Getter
@Setter
@Schema(
    description =
        "Rascunho para determinação reativa de duração, término previsto e dados do período aquisitivo na programação de férias.")
public class FeriasGozoPreviaDraftDTO {
  @ResourceReference(
      resourceKey = "administracao-pessoal.periodos-aquisitivos-ferias",
      valueField = "periodoAquisitivoId",
      surfaceId = "ficha-periodo-aquisitivo",
      previewSurfaceId = "ficha-periodo-aquisitivo",
      detailPresentation = "drawer")
  @Schema(description = "Identificador do período aquisitivo selecionado.")
  @UISchema(
      label = "Período aquisitivo",
      helpText = "Período aquisitivo de férias selecionado.",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "date_range",
      endpoint = ApiPaths.PERIODO_AQUISITIVO_FERIAS_OPTIONS,
      valueField = "id",
      displayField = "label")
  private Long periodoAquisitivoId;

  @Schema(description = "Data de início do gozo.")
  @UISchema(
      label = "Início do gozo",
      helpText = "Data inicial de afastamento em gozo de férias.",
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "event")
  private LocalDate dtini;

  @Schema(description = "Data de término previsto do gozo.")
  @UISchema(
      label = "Término previsto",
      helpText = "Data final programada da parcela de férias.",
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "event_available")
  private LocalDate dtprevfim;

  @Schema(description = "Quantidade de dias de gozo informada.")
  @UISchema(
      label = "Dias de gozo",
      helpText = "Quantidade de dias corridos de descanso nesta parcela.",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "beach_access")
  private Integer diasGozo;

  @Schema(
      description =
          "Indica se há opção por conversão parcial em abono pecuniário nesta parcela.")
  @UISchema(
      label = "Com abono pecuniário",
      helpText = "Indica venda de dias em abono pecuniário nesta parcela.",
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "sell")
  private Boolean abonoPecuniario;

  @Schema(
      description =
          "Quantidade de dias vendidos em abono pecuniário informada na parcela.")
  @UISchema(
      label = "Dias de abono vendidos",
      helpText = "Quantidade de dias convertidos em pecúnia nesta parcela.",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "payments")
  private Integer diasAbonoVendidos;
}
