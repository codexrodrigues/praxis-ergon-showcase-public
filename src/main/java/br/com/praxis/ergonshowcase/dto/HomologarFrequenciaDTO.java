package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
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
        "Comando de homologação formal de ocorrência operacional de frequência pela chefia imediata ou RH.")
public class HomologarFrequenciaDTO {

  @Schema(
      description =
          "Identificador do documento comprobatório ou despacho de homologação, quando exigido.")
  @UISchema(
      label = "Documento de homologação",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "MenuBook",
      group = "Homologação",
      order = 10,
      endpoint = ApiPaths.DOCUMENTO_OPTIONS,
      valueField = "id",
      displayField = "label",
      helpText = "Ato legal ou despacho que formaliza a homologação.")
  private Long documentoLegalId;

  @Size(max = 1000)
  @Schema(description = "Despacho ou observação complementar da homologação.")
  @UISchema(
      label = "Despacho de homologação",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.TEXTAREA,
      icon = "verified",
      group = "Homologação",
      order = 20,
      helpText = "Registre o parecer da chefia ou RH ao homologar a frequência.")
  private String observacoes;
}
