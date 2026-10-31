package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

@Getter
@Setter
@Schema(
    description =
        "Rascunho de entrada para determinação reativa de escopo global, obrigatoriedade de tabela de opções e descrição da tabela geral no cadastro de campo customizado.")
public class CampoCustomizadoPreviaDraftDTO {

  @Schema(description = "Código do órgão proprietário informado no formulário.")
  @UISchema(
      label = "Órgão proprietário",
      group = "Escopo e Alvo",
      order = 10,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "apartment",
      endpoint = ApiPaths.ORGAO_OPTIONS,
      valueField = "id",
      displayField = "label",
      helpText = "Órgão específico da customização ou vazio para campo global.")
  private Integer empCodigo;

  @Schema(
      description =
          "Tipo de dado primitivo informado no formulário (STRING, INTEGER, NUMBER, BOOLEAN, DATE, SELECT).")
  @UISchema(
      label = "Tipo de dado",
      group = "Tipagem e Componente",
      order = 20,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "data_object",
      helpText = "Tipo primitivo do atributo customizado.")
  private String fieldType;

  @Schema(description = "Controle visual selecionado no formulário.")
  @UISchema(
      label = "Controle visual",
      group = "Tipagem e Componente",
      order = 30,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "widgets",
      helpText = "Componente de formulário associado ao campo.")
  private String controlType;

  @Schema(description = "Tabela geral de domínio selecionada para lista de opções.")
  @UISchema(
      label = "Tabela geral de opções",
      group = "Validação e Governança",
      order = 40,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "table_chart",
      endpoint = ApiPaths.TABELA_OPTIONS,
      valueField = "id",
      displayField = "label",
      helpText = "Tabela geral opcional de domínio.")
  private String optionTabela;
}
