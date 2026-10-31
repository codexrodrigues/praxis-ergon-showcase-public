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
@Schema(description = "Comando de rejeição fundamentada de ocorrência de frequência pendente.")
public class RejeitarFrequenciaDTO {

  @NotBlank
  @Size(max = 1000)
  @Schema(description = "Motivo formal da rejeição da ocorrência de frequência.")
  @UISchema(
      label = "Motivo da rejeição",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.TEXTAREA,
      icon = "cancel",
      group = "Rejeição",
      order = 10,
      required = true,
      helpText = "Informe o motivo do indeferimento ou rejeição da ocorrência.")
  private String motivoRejeicao;
}
