package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
        "Comando de abono formal de falta ou ausência justificada mediante documento comprobatório.")
public class AbonarFaltaFrequenciaDTO {

  @NotNull
  @Schema(description = "Identificador do documento comprobatório ou portaria que ampara o abono.")
  @UISchema(
      label = "Documento comprobatório",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "menu_book",
      group = "Abono de Falta",
      order = 10,
      required = true,
      endpoint = ApiPaths.DOCUMENTO_OPTIONS,
      valueField = "id",
      displayField = "label",
      helpText = "Selecione o atestado, declaração ou portaria que ampara o abono.")
  private Long documentoLegalId;

  @NotBlank
  @Size(max = 1000)
  @Schema(description = "Fundamentação formal para o abono da falta ou ausência.")
  @UISchema(
      label = "Fundamentação do abono",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.TEXTAREA,
      icon = "task_alt",
      group = "Abono de Falta",
      order = 20,
      required = true,
      helpText = "Descreva o motivo formal para conversão e homologação do abono.")
  private String justificativa;
}
