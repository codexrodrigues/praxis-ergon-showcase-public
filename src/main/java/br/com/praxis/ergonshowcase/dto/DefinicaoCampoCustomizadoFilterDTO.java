package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.*;
import org.praxisplatform.uischema.*;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.filter.annotation.Filterable;
import org.praxisplatform.uischema.filter.annotation.Filterable.FilterOperation;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;

@Getter
@Setter
@Schema(
    description =
        "Critérios de pesquisa do catálogo governado de definições de campos customizados multi-órgão por recurso, chave técnica, tipo, controle, tabela de opções e situação.")
public class DefinicaoCampoCustomizadoFilterDTO implements GenericFilterDTO {
  @Size(max = 200)
  @Schema(
      description =
          "Busca ampla textual por rótulo de exibição, chave técnica (fieldKey), chave do recurso estendido, grupo do formulário ou tabela de opções.")
  @UISchema(
      label = "Busca ampla de campo",
      helpText = "Pesquisa simultaneamente por rótulo, chave técnica, recurso ou seção.",
      group = "Busca rápida",
      order = 1,
      icon = "search")
  @Filterable(
      operation = FilterOperation.LIKE,
      relations = {"label", "fieldKey", "resourceKey", "groupName", "optionTabela"})
  private String buscaAmpla;

  @Schema(
      description =
          "Filtra pelo órgão público proprietário da definição de campo customizado.")
  @UISchema(
      label = "Órgão proprietário",
      helpText = "Órgão público proprietário da customização.",
      group = "Escopo e Alvo",
      order = 10,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "apartment",
      endpoint = ApiPaths.ORGAO_OPTIONS,
      valueField = "id",
      displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL, relation = "empCodigo")
  private Integer empCodigo;

  @Size(max = 120)
  @Schema(
      description =
          "Filtra pela chave exata do recurso Praxis estendido (ex.: administracao-pessoal.funcionarios).")
  @UISchema(
      label = "Recurso estendido",
      helpText = "Chave semântica exata do recurso estendido.",
      group = "Escopo e Alvo",
      order = 20,
      icon = "hub")
  @Filterable(operation = FilterOperation.EQUAL, relation = "resourceKey")
  private String resourceKey;

  @Size(max = 80)
  @Schema(description = "Filtra por prefixo ou trecho da chave técnica em snake_case (fieldKey).")
  @UISchema(
      label = "Chave técnica contém",
      helpText = "Trecho da chave técnica em snake_case.",
      group = "Escopo e Alvo",
      order = 30,
      icon = "key")
  @Filterable(operation = FilterOperation.LIKE, relation = "fieldKey")
  private String fieldKey;

  @Size(max = 120)
  @Schema(description = "Filtra por trecho do rótulo de exibição do campo customizado.")
  @UISchema(
      label = "Rótulo contém",
      helpText = "Texto parcial do rótulo exibido no formulário.",
      group = "Apresentação Editorial",
      order = 40,
      icon = "label")
  @Filterable(operation = FilterOperation.LIKE, relation = "label")
  private String label;

  @Size(max = 30)
  @Schema(
      description =
          "Filtra pelo tipo de dado primitivo do campo customizado (STRING, INTEGER, NUMBER, BOOLEAN, DATE, SELECT).")
  @UISchema(
      label = "Tipo de dado",
      helpText = "Tipo primitivo do campo customizado.",
      group = "Tipagem e Componente",
      order = 50,
      icon = "data_object")
  @Filterable(operation = FilterOperation.EQUAL, relation = "fieldType")
  private String fieldType;

  @Schema(description = "Filtra por lista de tipos de dado primitivos do campo customizado.")
  @UISchema(
      label = "Tipos de dado",
      helpText = "Conjunto de tipos primitivos a incluir.",
      group = "Tipagem e Componente",
      order = 55,
      controlType = FieldControlType.MULTI_SELECT,
      icon = "checklist")
  @Filterable(operation = FilterOperation.IN, relation = "fieldType")
  private List<String> fieldTypes;

  @Size(max = 40)
  @Schema(description = "Filtra pelo componente visual canônico (controlType).")
  @UISchema(
      label = "Controle visual",
      helpText = "Componente de formulário associado ao campo.",
      group = "Tipagem e Componente",
      order = 60,
      icon = "widgets")
  @Filterable(operation = FilterOperation.EQUAL, relation = "controlType")
  private String controlType;

  @Size(max = 40)
  @Schema(
      description =
          "Filtra pela tabela geral de domínio vinculada como fonte de opções (optionTabela).")
  @UISchema(
      label = "Tabela geral de opções",
      helpText = "Tabela geral que abastece o campo quando SELECT.",
      group = "Validação e Governança",
      order = 70,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "table_chart",
      endpoint = ApiPaths.TABELA_OPTIONS,
      valueField = "id",
      displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL, relation = "optionTabela")
  private String optionTabela;

  @Schema(description = "Filtra pelo indicador de preenchimento obrigatório.")
  @UISchema(
      label = "Obrigatório",
      helpText = "Indica se o campo customizado é obrigatório.",
      group = "Validação e Governança",
      order = 80,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "rule")
  @Filterable(operation = FilterOperation.EQUAL, relation = "required")
  private Boolean required;

  @Schema(description = "Filtra pelo indicador de definição ativa ou inativa.")
  @UISchema(
      label = "Definição ativa",
      helpText = "Indica se o campo está ativo no schema.",
      group = "Validação e Governança",
      order = 90,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "verified")
  @Filterable(operation = FilterOperation.EQUAL, relation = "active")
  private Boolean active;

  @Schema(description = "Filtra por ordem mínima de posicionamento no grupo do formulário.")
  @UISchema(
      label = "Ordem mínima",
      helpText = "Ordem mínima de posicionamento no grupo.",
      group = "Apresentação Editorial",
      order = 100,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "vertical_align_bottom")
  @Filterable(operation = FilterOperation.GREATER_OR_EQUAL, relation = "fieldOrder")
  private Integer ordemMinima;

  @Schema(description = "Filtra por ordem máxima de posicionamento no grupo do formulário.")
  @UISchema(
      label = "Ordem máxima",
      helpText = "Ordem máxima de posicionamento no grupo.",
      group = "Apresentação Editorial",
      order = 110,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "vertical_align_top")
  @Filterable(operation = FilterOperation.LESS_OR_EQUAL, relation = "fieldOrder")
  private Integer ordemMaxima;

  @Schema(description = "Filtra apenas definições ativas de campos customizados.")
  @UISchema(
      label = "Somente ativos",
      helpText = "Restringe a definições ativas em /schemas/filtered.",
      group = "Filtros rápidos",
      order = 120,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "check_circle")
  @Filterable(operation = FilterOperation.IS_TRUE, relation = "active")
  private Boolean somenteAtivos;

  @Schema(description = "Filtra apenas definições com preenchimento obrigatório.")
  @UISchema(
      label = "Somente obrigatórios",
      helpText = "Restringe a campos customizados obrigatórios.",
      group = "Filtros rápidos",
      order = 130,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "priority_high")
  @Filterable(operation = FilterOperation.IS_TRUE, relation = "required")
  private Boolean somenteObrigatorios;

  @Schema(description = "Filtra apenas definições globais aplicáveis a todos os órgãos (empCodigo nulo).")
  @UISchema(
      label = "Somente globais",
      helpText = "Restringe a definições globais sem órgão específico.",
      group = "Filtros rápidos",
      order = 140,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "public")
  @Filterable(operation = FilterOperation.IS_NULL, relation = "empCodigo")
  private Boolean somenteGlobais;

  @Schema(description = "Filtra apenas definições associadas a uma tabela geral de opções (optionTabela não nulo).")
  @UISchema(
      label = "Com tabela de opções",
      helpText = "Restringe a campos abastecidos por tabela geral.",
      group = "Filtros rápidos",
      order = 150,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "list_alt")
  @Filterable(operation = FilterOperation.IS_NOT_NULL, relation = "optionTabela")
  private Boolean comTabelaOpcoes;
}
