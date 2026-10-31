package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.dto.PartialUpdateInput;
import org.praxisplatform.uischema.extension.annotation.UISchema;

/** Manutenção parcial da designação de servidor substituto durante o período de afastamento. */
@Getter
@NoArgsConstructor
@Schema(
    description =
        "Comando de manutenção parcial da designação de substituto funcional durante o período de licença ou afastamento.")
public class AtualizarSubstitutoAfastamentoDTO extends PartialUpdateInput {
  @Positive
  @Schema(
      nullable = true,
      description =
          "Número funcional do servidor público designado para substituição durante o afastamento.")
  @UISchema(
      label = "Servidor substituto",
      helpText = "Servidor designado para responder pelo posto durante a licença.",
      group = "Substituição funcional",
      order = 10,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "person_add",
      endpoint = ApiPaths.PESSOA_OPTIONS,
      valueField = "id",
      displayField = "label")
  private Long numfuncSubstituto;

  @Positive
  @Schema(
      nullable = true,
      description = "Número sequencial do vínculo funcional do servidor substituto designado.")
  @UISchema(
      label = "Vínculo do substituto",
      helpText = "Sequencial do vínculo ativo do servidor substituto.",
      group = "Substituição funcional",
      order = 20,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.INPUT,
      icon = "badge")
  private Integer numvincSubstituto;

  @Positive
  @Schema(
      nullable = true,
      description = "Portaria ou despacho que designa ou dispensa o servidor substituto.")
  @UISchema(
      label = "Portaria de substituição",
      helpText = "Ato legal que formaliza a substituição no período.",
      group = "Substituição funcional",
      order = 30,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "description",
      endpoint = ApiPaths.DOCUMENTO_OPTIONS,
      valueField = "id",
      displayField = "label")
  private Long documentoLegalId;

  @JsonSetter(value = "numfuncSubstituto", nulls = Nulls.SET)
  public void setNumfuncSubstituto(Long v) {
    supplied("numfuncSubstituto");
    this.numfuncSubstituto = v;
  }

  @JsonSetter(value = "numvincSubstituto", nulls = Nulls.SET)
  public void setNumvincSubstituto(Integer v) {
    supplied("numvincSubstituto");
    this.numvincSubstituto = v;
  }

  @JsonSetter(value = "documentoLegalId", nulls = Nulls.SET)
  public void setDocumentoLegalId(Long v) {
    supplied("documentoLegalId");
    this.documentoLegalId = v;
  }
}
