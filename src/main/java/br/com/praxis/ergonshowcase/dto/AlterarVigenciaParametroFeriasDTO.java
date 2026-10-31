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
        "Comando de ativação, inativação ou encerramento de vigência da matriz estatutária de parâmetros de contagem de férias.")
public class AlterarVigenciaParametroFeriasDTO {

  @Schema(
      description =
          "Data de término de vigência normativa da matriz de parâmetros de contagem de férias.",
      nullable = true)
  @UISchema(
      label = "Fim da vigência",
      helpText = "Data de encerramento da vigência normativa.",
      group = "Vigência e Fundamentação",
      order = 10,
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "event_busy")
  private LocalDate dtfim;

  @Schema(
      description = "Identificador do ato legal que fundamenta a alteração de vigência.",
      nullable = true)
  @UISchema(
      label = "Ato legal de férias",
      helpText = "Documento normativo que ampara a revisão da regra.",
      group = "Vigência e Fundamentação",
      order = 20,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "description",
      endpoint = ApiPaths.DOCUMENTO_OPTIONS,
      valueField = "id",
      displayField = "label")
  private Long documentoLegalFeriasId;

  @NotBlank
  @Size(max = 2000)
  @Schema(
      description =
          "Justificativa administrativa ou parecer normativo para a alteração de estado ou vigência da regra.")
  @UISchema(
      label = "Observações normativas",
      helpText = "Justificativa formal para o ato de vigência.",
      group = "Vigência e Fundamentação",
      order = 30,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.TEXTAREA,
      icon = "notes")
  private String observacoes;
}
