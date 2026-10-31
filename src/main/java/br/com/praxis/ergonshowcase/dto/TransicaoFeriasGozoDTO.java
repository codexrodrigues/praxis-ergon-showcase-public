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
        "Comando de transição de estado da parcela de gozo de férias (homologação, início de fruição, conclusão ou cancelamento).")
public class TransicaoFeriasGozoDTO {

  @Schema(
      description = "Data de término efetivo do gozo das férias quando da conclusão.",
      nullable = true)
  @UISchema(
      label = "Término efetivo",
      helpText = "Data efetiva de encerramento do gozo das férias.",
      group = "Transição da Parcela",
      order = 10,
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "event_available")
  private LocalDate dtfim;

  @Schema(
      description = "Identificador da portaria ou ato legal que ampara a transição.",
      nullable = true)
  @UISchema(
      label = "Documento de concessão",
      helpText = "Portaria ou despacho que formaliza o ato.",
      group = "Transição da Parcela",
      order = 20,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "description",
      endpoint = ApiPaths.DOCUMENTO_OPTIONS,
      valueField = "id",
      displayField = "label")
  private Long documentoLegalId;

  @Schema(
      description = "Identificador do ato de publicação no diário oficial relativo à parcela.",
      nullable = true)
  @UISchema(
      label = "Ato de publicação",
      helpText = "Ato oficial de publicação da escala ou alteração de férias.",
      group = "Transição da Parcela",
      order = 25,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "campaign",
      endpoint = ApiPaths.DOCUMENTO_OPTIONS,
      valueField = "id",
      displayField = "label")
  private Long atoPublicacaoId;

  @NotBlank
  @Size(max = 2000)
  @Schema(description = "Despacho ou justificativa administrativa para a transição da parcela.")
  @UISchema(
      label = "Observações da parcela",
      helpText = "Justificativa formal registrada na parcela.",
      group = "Transição da Parcela",
      order = 30,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.TEXTAREA,
      icon = "notes")
  private String observacoes;
}
