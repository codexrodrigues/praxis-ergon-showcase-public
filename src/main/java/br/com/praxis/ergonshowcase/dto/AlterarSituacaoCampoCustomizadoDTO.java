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
        "Comando de ativação ou desativação governada da definição de campo customizado no catálogo do órgão.")
public class AlterarSituacaoCampoCustomizadoDTO {

  @NotBlank
  @Size(max = 500)
  @Schema(
      description =
          "Justificativa técnica ou normativa para ativar ou desativar o campo customizado.")
  @UISchema(
      label = "Justificativa",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.TEXTAREA,
      icon = "notes",
      group = "Governança",
      order = 10,
      required = true,
      helpText = "Fundamente a alteração de vigência do campo customizado.")
  private String justificativa;
}
