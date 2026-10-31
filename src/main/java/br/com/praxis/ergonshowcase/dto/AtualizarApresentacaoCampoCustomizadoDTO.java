package br.com.praxis.ergonshowcase.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.dto.PartialUpdateInput;
import org.praxisplatform.uischema.extension.annotation.UISchema;

@Getter
@Schema(
    description =
        "Manutenção parcial especializada para apresentação visual, agrupamento e controle do campo customizado.")
public class AtualizarApresentacaoCampoCustomizadoDTO extends PartialUpdateInput {

  @Size(max = 120)
  @Schema(description = "Rótulo editorial do campo customizado exibido no formulário e tabela.")
  @UISchema(
      label = "Rótulo de exibição",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "label",
      group = "Apresentação",
      order = 10,
      helpText = "Rótulo curto apresentado ao usuário.")
  private String label;

  @Size(max = 500)
  @Schema(description = "Texto de ajuda contextual apresentado abaixo do controle.")
  @UISchema(
      label = "Texto de ajuda",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.TEXTAREA,
      icon = "help_outline",
      group = "Apresentação",
      order = 20,
      helpText = "Orientação operacional para preenchimento.")
  private String helpText;

  @Size(max = 120)
  @Schema(description = "Grupo semântico de organização no formulário e na ficha 360°.")
  @UISchema(
      label = "Grupo do formulário",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "folder",
      group = "Apresentação",
      order = 30,
      helpText = "Seção visual de agrupamento do campo.")
  private String groupName;

  @Schema(description = "Ordem de apresentação do campo dentro da seção.")
  @UISchema(
      label = "Ordem no grupo",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "sort",
      group = "Apresentação",
      order = 40,
      helpText = "Ordem relativa de apresentação.")
  private Integer fieldOrder;

  @Size(max = 40)
  @Schema(description = "Tipo de controle visual utilizado na renderização do formulário.")
  @UISchema(
      label = "Controle visual",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "widgets",
      group = "Apresentação",
      order = 50,
      helpText = "Componente de UI para edição do atributo.")
  private String controlType;

  @JsonSetter("label")
  public void setLabel(String label) {
    this.label = label;
    supplied("label");
  }

  @JsonSetter("helpText")
  public void setHelpText(String helpText) {
    this.helpText = helpText;
    supplied("helpText");
  }

  @JsonSetter("groupName")
  public void setGroupName(String groupName) {
    this.groupName = groupName;
    supplied("groupName");
  }

  @JsonSetter("fieldOrder")
  public void setFieldOrder(Integer fieldOrder) {
    this.fieldOrder = fieldOrder;
    supplied("fieldOrder");
  }

  @JsonSetter("controlType")
  public void setControlType(String controlType) {
    this.controlType = controlType;
    supplied("controlType");
  }
}
