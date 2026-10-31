package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
        "Comando de interrupção de férias em gozo por imperiosa necessidade do serviço público, recompondo o saldo do período aquisitivo.")
public class InterromperFeriasGozoDTO {

  @NotNull
  @Schema(description = "Data de retorno antecipado e interrupção efetiva do gozo das férias.")
  @UISchema(
      label = "Data de interrupção",
      helpText = "Data em que o servidor reassumiu o exercício por interrupção.",
      group = "Interrupção de Férias",
      order = 10,
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "warning")
  private LocalDate dtInterrupcao;

  @NotBlank
  @Size(max = 500)
  @Schema(description = "Motivo de interesse público que determinou a interrupção das férias.")
  @UISchema(
      label = "Motivo da interrupção",
      helpText = "Justificativa administrativa da necessidade do serviço.",
      group = "Interrupção de Férias",
      order = 20,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.TEXTAREA,
      icon = "report")
  private String motivoInterrupcao;

  @Schema(
      description = "Identificador da portaria que determinou a interrupção das férias.",
      nullable = true)
  @UISchema(
      label = "Documento de concessão",
      helpText = "Portaria de interrupção de férias.",
      group = "Interrupção de Férias",
      order = 30,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "description",
      endpoint = ApiPaths.DOCUMENTO_OPTIONS,
      valueField = "id",
      displayField = "label")
  private Long documentoLegalId;

  @Size(max = 2000)
  @Schema(description = "Observações complementares sobre o saldo remanescente.")
  @UISchema(
      label = "Observações da parcela",
      helpText = "Anotações complementares da interrupção.",
      group = "Interrupção de Férias",
      order = 40,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.TEXTAREA,
      icon = "notes")
  private String observacoes;
}
