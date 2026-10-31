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
        "Resultado da determinação reativa para preenchimento assistido do formulário de frequência.")
public class FrequenciaPreviaPreviewDTO {
  @Schema(
      description =
          "Denominação prevista da modalidade de frequência localizada no catálogo normativo.")
  @UISchema(
      label = "Modalidade prevista",
      group = "Prévia Reativa",
      order = 10,
      readOnly = true,
      helpText = "Denominação oficial da modalidade selecionada.")
  private String codigoFrequenciaNomePrevisto;

  @Schema(
      description =
          "Unidade de medida prevista para apuração da quantidade (DIAS, HORAS ou MINUTOS).")
  @UISchema(
      label = "Unidade de apuração prevista",
      group = "Prévia Reativa",
      order = 20,
      readOnly = true,
      helpText = "Unidade regulamentar de apuração do código.")
  private String formatoQuantidadePrevisto;

  @Schema(
      description =
          "Indica se o código de frequência selecionado exige o preenchimento de hora de entrada e saída.")
  @UISchema(
      label = "Exige horário previsto",
      group = "Prévia Reativa",
      order = 30,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "schedule",
      readOnly = true,
      helpText = "Sinaliza a obrigatoriedade de informar horários de entrada e saída.")
  private Boolean exigeHorarioPrevisto;
}
