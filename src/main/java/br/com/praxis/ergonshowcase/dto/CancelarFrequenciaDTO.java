package br.com.praxis.ergonshowcase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

@Getter
@Setter
@Schema(description = "Comando de cancelamento administrativo de ocorrência de frequência.")
public class CancelarFrequenciaDTO {

  @NotBlank
  @Size(max = 1000)
  @Schema(description = "Motivo administrativo do cancelamento da ocorrência de frequência.")
  @UISchema(
      label = "Motivo do cancelamento",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.TEXTAREA,
      icon = "block",
      group = "Cancelamento",
      order = 10,
      required = true,
      helpText = "Justifique o cancelamento administrativo do lançamento.")
  private String motivoCancelamento;
}
