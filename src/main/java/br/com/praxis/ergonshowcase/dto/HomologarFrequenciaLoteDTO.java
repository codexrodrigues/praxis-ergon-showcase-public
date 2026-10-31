package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
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
        "Parâmetros do comando coletivo de homologação em lote de ocorrências de frequência.")
public class HomologarFrequenciaLoteDTO {

  @Schema(description = "Documento legal ou despacho coletivo de homologação da folha de ponto.")
  @UISchema(
      label = "Documento de homologação",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "MenuBook",
      group = "Homologação em Lote",
      order = 10,
      endpoint = ApiPaths.DOCUMENTO_OPTIONS,
      valueField = "id",
      displayField = "label",
      helpText = "Ato administrativo que fundamenta a homologação do lote.")
  private Long documentoLegalId;

  @NotBlank
  @Size(max = 1000)
  @Schema(description = "Observação ou despacho coletivo registrado em todas as ocorrências do lote.")
  @UISchema(
      label = "Despacho coletivo",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.TEXTAREA,
      icon = "done_all",
      group = "Homologação em Lote",
      order = 20,
      required = true,
      helpText = "Despacho registrado nas ocorrências homologadas em lote.")
  private String observacoes;
}
