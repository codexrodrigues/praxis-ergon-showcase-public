package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.util.*;
import lombok.*;
import org.praxisplatform.uischema.*;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.bulk.*;
import org.praxisplatform.uischema.extension.annotation.UISchema;

@Getter
@Setter
public abstract class DefinicaoCampoCustomizadoDados {
  @Positive
  @BulkEditable(modes = {BulkMode.UNIFORM_UPDATE, BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  @Schema(
      nullable = true,
      description =
          "Código do órgão público proprietário da customização (nulo quando o campo customizado é global para todos os órgãos do ente).")
  @UISchema(
      label = "Órgão proprietário",
      helpText = "Órgão específico da customização ou vazio para campo global.",
      group = "Escopo e Alvo",
      order = 10,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "apartment",
      endpoint = ApiPaths.ORGAO_OPTIONS,
      valueField = "id",
      displayField = "label")
  private Integer empCodigo;

  @NotBlank
  @Size(max = 120)
  @Schema(
      description =
          "Chave canônica do recurso Praxis estendido (ex.: administracao-pessoal.funcionarios, administracao-pessoal.vinculos, administracao-pessoal.frequencias).")
  @UISchema(
      label = "Recurso estendido",
      helpText = "Chave semântica do recurso Praxis que receberá o campo customizado.",
      group = "Escopo e Alvo",
      order = 20,
      icon = "hub")
  private String resourceKey;

  @NotBlank
  @Size(max = 80)
  @Pattern(regexp = "^[a-z][a-z0-9_]*$")
  @Schema(
      description =
          "Chave semântica em snake_case armazenada dentro do objeto custom_fields JSONB da tabela alvo (ex.: numero_calcado, processo_sei_nomeacao).")
  @UISchema(
      label = "Chave técnica",
      helpText = "Identificador único da propriedade dentro de customFields.",
      group = "Escopo e Alvo",
      order = 30,
      icon = "key")
  private String fieldKey;

  @NotBlank
  @Size(max = 30)
  @Schema(
      description =
          "Tipo de dado primitivo do campo customizado (STRING, INTEGER, NUMBER, BOOLEAN, DATE, SELECT).")
  @UISchema(
      label = "Tipo de dado",
      helpText = "Natureza primitiva do valor: STRING, INTEGER, NUMBER, BOOLEAN, DATE ou SELECT.",
      group = "Tipagem e Componente",
      order = 40,
      icon = "data_object")
  private String fieldType;

  @NotBlank
  @Size(max = 40)
  @BulkEditable(modes = {BulkMode.UNIFORM_UPDATE, BulkMode.PER_ITEM_UPDATE})
  @Schema(
      description =
          "Componente visual canônico do @praxisui/dynamic-fields (INPUT, TEXTAREA, NUMERIC_TEXT_BOX, DATE_PICKER, SELECT, SEARCHABLE_SELECT, CHECKBOX, SWITCH).")
  @UISchema(
      label = "Controle visual",
      helpText = "Componente de formulário dinâmico que renderiza o campo.",
      group = "Tipagem e Componente",
      order = 50,
      icon = "widgets")
  private String controlType;

  @NotBlank
  @Size(max = 120)
  @BulkEditable(modes = {BulkMode.PER_ITEM_UPDATE})
  @Schema(
      description =
          "Rótulo de negócio em português apresentado nos formulários dinâmicos e cabeçalhos de tabela.")
  @UISchema(
      label = "Rótulo de exibição",
      helpText = "Título curto apresentado ao usuário no formulário e na grade.",
      group = "Apresentação Editorial",
      order = 60,
      icon = "label")
  private String label;

  @Size(max = 500)
  @BulkEditable(modes = {BulkMode.UNIFORM_UPDATE, BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  @Schema(
      nullable = true,
      description =
          "Texto de ajuda contextual explicando a finalidade de negócio e o preenchimento esperado do campo.")
  @UISchema(
      label = "Texto de ajuda",
      helpText = "Orientação de preenchimento exibida sob o controle ou em dica contextual.",
      group = "Apresentação Editorial",
      order = 70,
      controlType = FieldControlType.TEXTAREA,
      icon = "help_outline")
  private String helpText;

  @Size(max = 120)
  @BulkEditable(modes = {BulkMode.UNIFORM_UPDATE, BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  @Schema(
      nullable = true,
      description =
          "Nome da seção ou agrupamento visual do formulário dinâmico onde o campo será renderizado.")
  @UISchema(
      label = "Grupo do formulário",
      helpText = "Seção visual do formulário dinâmico onde o campo é posicionado.",
      group = "Apresentação Editorial",
      order = 80,
      icon = "view_agenda")
  private String groupName;

  @NotNull
  @Min(1)
  @Max(999)
  @BulkEditable(modes = {BulkMode.PER_ITEM_UPDATE})
  @Schema(
      description = "Ordem numérica de posicionamento do campo dentro do grupo visual (1 a 999).")
  @UISchema(
      label = "Ordem no grupo",
      helpText = "Posição numérica do campo dentro da seção do formulário.",
      group = "Apresentação Editorial",
      order = 90,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "format_list_numbered")
  private Integer fieldOrder;

  @NotNull
  @BulkEditable(modes = {BulkMode.UNIFORM_UPDATE, BulkMode.PER_ITEM_UPDATE})
  @Schema(
      description =
          "Indica se o preenchimento do campo customizado é obrigatório nas operações de criação e edição.")
  @UISchema(
      label = "Preenchimento obrigatório",
      helpText = "Exige valor informado nas submissões de criação e edição.",
      group = "Validação e Governança",
      order = 100,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "rule")
  private Boolean required = false;

  @Size(max = 40)
  @BulkEditable(modes = {BulkMode.UNIFORM_UPDATE, BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  @Schema(
      nullable = true,
      description =
          "Referência opcional à tabela geral (tabelas.tab) que fornece a lista de opções quando fieldType = SELECT.")
  @UISchema(
      label = "Tabela geral de opções",
      helpText = "Tabela geral que abastece as opções quando o tipo é SELECT.",
      group = "Validação e Governança",
      order = 110,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "table_chart",
      endpoint = ApiPaths.TABELA_OPTIONS,
      valueField = "id",
      displayField = "label")
  private String optionTabela;

  @NotNull
  @Schema(
      description =
          "Regras declarativas de validação em JSONB (ex.: min, max, minLength, maxLength, pattern, customMessage) consumidas pelo backend e pelo @praxisui/dynamic-form.")
  @UISchema(
      label = "Regras de validação",
      helpText = "Mapa declarativo de limites como min, max, minLength, maxLength e pattern.",
      group = "Validação e Governança",
      order = 120,
      icon = "fact_check")
  private Map<String, Object> validationRules = new LinkedHashMap<>();

  @NotNull
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Política de governança LGPD e visibilidade de IA do campo customizado.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.ALLOW, trainingUse = AiTrainingUseMode.DENY))
  @Schema(
      description =
          "Política de governança de domínio (@DomainGovernance / LGPD / AiUsagePolicy) em JSONB definindo classificação de sensibilidade, mascaramento e permissão de uso por IA.")
  @UISchema(
      label = "Política de governança",
      helpText = "Metadados de conformidade LGPD, sensibilidade e política de uso por IA.",
      group = "Validação e Governança",
      order = 130,
      icon = "policy")
  private Map<String, Object> governancePolicy = new LinkedHashMap<>();
}
