package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
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
        "Rascunho de entrada para determinação reativa de modalidade, unidade e obrigatoriedade de horário na frequência.")
public class FrequenciaPreviaDraftDTO {
  @Schema(description = "Tipo macro de frequência informado no formulário.")
  @UISchema(
      label = "Tipo de frequência",
      group = "Modalidade",
      order = 10,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "category",
      endpoint = ApiPaths.TIPO_FREQUENCIA_OPTIONS,
      valueField = "id",
      displayField = "label",
      helpText = "Tipo macro da ocorrência de frequência.")
  private String tipofreq;

  @Schema(description = "Código numérico da ocorrência de frequência informado no formulário.")
  @UISchema(
      label = "Código de frequência",
      group = "Modalidade",
      order = 20,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.INPUT,
      icon = "pin",
      helpText = "Código numérico detalhado da ocorrência.")
  private Integer codfreq;

  @Schema(description = "Data de início da ocorrência informada no formulário.")
  @UISchema(
      label = "Data de início",
      group = "Período",
      order = 30,
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "event",
      helpText = "Data inicial da ocorrência de frequência.")
  private LocalDate dtini;

  @Schema(description = "Data final da ocorrência informada no formulário.")
  @UISchema(
      label = "Data final",
      group = "Período",
      order = 40,
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "event_available",
      helpText = "Data final da ocorrência de frequência.")
  private LocalDate dtfim;
}
