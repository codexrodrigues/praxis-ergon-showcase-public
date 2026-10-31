package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
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
        "Comando de transição de estado do período aquisitivo de férias (suspensão, reativação, prescrição ou cancelamento).")
public class TransicaoPeriodoAquisitivoFeriasDTO {

  @Schema(
      description = "Data de prescrição ou data de referência da transição do período aquisitivo.",
      nullable = true)
  @UISchema(
      label = "Data de prescrição",
      helpText = "Data de prescrição ou marco temporal do ato.",
      group = "Transição e Fundamentação",
      order = 10,
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "event")
  private LocalDate dtPrescricao;

  @Schema(
      description = "Identificador do documento legal que fundamenta a transição de estado.",
      nullable = true)
  @UISchema(
      label = "Documento comprobatório",
      helpText = "Portaria ou despacho que ampara a transição do PA.",
      group = "Transição e Fundamentação",
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
  @Schema(description = "Justificativa formal para a mudança de situação do período aquisitivo.")
  @UISchema(
      label = "Observações do período",
      helpText = "Justificativa administrativa registrada no histórico do PA.",
      group = "Transição e Fundamentação",
      order = 30,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.TEXTAREA,
      icon = "notes")
  private String observacoes;
}
