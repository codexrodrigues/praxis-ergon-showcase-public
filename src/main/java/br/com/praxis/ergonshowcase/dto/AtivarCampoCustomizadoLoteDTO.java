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
@Schema(
    description =
        "Parâmetros do comando coletivo de ativação em lote de definições de campos customizados.")
public class AtivarCampoCustomizadoLoteDTO {

  @NotBlank
  @Size(max = 500)
  @Schema(
      description =
          "Justificativa administrativa registrada na ativação coletiva dos campos customizados.")
  @UISchema(
      label = "Justificativa do lote",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.TEXTAREA,
      icon = "done_all",
      group = "Ativação em Lote",
      order = 10,
      required = true,
      helpText = "Justificativa da publicação coletiva dos campos customizados.")
  private String justificativa;
}
