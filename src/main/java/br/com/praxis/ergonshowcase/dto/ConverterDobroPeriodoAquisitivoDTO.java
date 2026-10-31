package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
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
        "Comando de conversão de saldo disponível do período aquisitivo para contagem em dobro de tempo de serviço.")
public class ConverterDobroPeriodoAquisitivoDTO {

  @NotNull
  @Min(1)
  @Max(90)
  @Schema(description = "Quantidade de dias do saldo disponível a converter em contagem em dobro.")
  @UISchema(
      label = "Dias em contagem em dobro",
      helpText = "Quantidade de dias do saldo do PA a converter em dobro.",
      group = "Conversão em Dobro",
      order = 10,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "trending_up")
  private Integer diasConvertidosDobro;

  @Schema(description = "Identificador do ato legal que autoriza a averbação em dobro.")
  @UISchema(
      label = "Documento comprobatório",
      helpText = "Portaria ou despacho que autoriza a contagem em dobro.",
      group = "Conversão em Dobro",
      order = 20,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "description",
      endpoint = ApiPaths.DOCUMENTO_OPTIONS,
      valueField = "id",
      displayField = "label")
  private Long documentoLegalId;

  @NotBlank
  @Size(max = 2000)
  @Schema(description = "Fundamentação administrativa da conversão em dobro.")
  @UISchema(
      label = "Observações do período",
      helpText = "Justificativa formal para a conversão de saldo em dobro.",
      group = "Conversão em Dobro",
      order = 30,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.TEXTAREA,
      icon = "notes")
  private String observacoes;
}
