package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.*;
import lombok.*;
import org.praxisplatform.uischema.*;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.extension.annotation.UISchemaPreset;

@Getter
@Setter
@Schema(
    description =
        "Catálogo governado de metadados de campos customizados multi-órgão que substitui as colunas legadas FLEX_CAMPO_01..40 e publica propriedades dinâmicas tipadas em /schemas/filtered.")
@ResourceIdentity(
    keyField = "id",
    titleField = "label",
    displayLabelField = "label",
    subtitleField = "resourceKey",
    metadataFields = {"empCodigo", "active"})
public class DefinicaoCampoCustomizadoResponseDTO {
  @Schema(
      description = "Identificador único surrogate da definição de campo customizado.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "ID da definição",
      helpText = "Chave surrogate imutável da definição de campo customizado.",
      preset = UISchemaPreset.ENTERPRISE_ID,
      group = "Escopo e Alvo",
      order = 1,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "tag",
      readOnly = true,
      tableHidden = true,
      formHidden = true)
  private Long id;

  @Schema(
      description =
          "Código do órgão público proprietário da customização (nulo quando o campo é global para todos os órgãos).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Órgão proprietário",
      helpText = "Órgão público ao qual o campo customizado pertence ou vazio para global.",
      group = "Escopo e Alvo",
      order = 10,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "apartment",
      endpoint = ApiPaths.ORGAO_OPTIONS,
      valueField = "id",
      displayField = "label",
      readOnly = true,
      tableHidden = false)
  private Integer empCodigo;

  @Schema(
      description = "Nome do órgão público proprietário quando a customização é específica.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Nome do órgão",
      helpText = "Denominação oficial do órgão público proprietário.",
      group = "Escopo e Alvo",
      order = 15,
      readOnly = true,
      tableHidden = true,
      formHidden = true)
  @ResourceReference(
      resourceKey = "administracao-pessoal.empresas",
      valueField = "empCodigo",
      surfaceId = "ficha-institucional",
      previewSurfaceId = "ficha-institucional",
      detailPresentation = "drawer")
  private String orgaoNome;

  @Schema(
      description =
          "Chave canônica do recurso Praxis estendido (ex.: administracao-pessoal.funcionarios, administracao-pessoal.vinculos).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Recurso estendido",
      helpText = "Recurso Praxis que recebe este campo customizado em customFields.",
      group = "Escopo e Alvo",
      order = 20,
      readOnly = true,
      tableHidden = false)
  private String resourceKey;

  @Schema(
      description =
          "Chave semântica em snake_case armazenada dentro do objeto custom_fields JSONB da tabela alvo.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Chave técnica",
      helpText = "Identificador snake_case da propriedade dentro de customFields.",
      group = "Escopo e Alvo",
      order = 30,
      readOnly = true,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 1)
  private String fieldKey;

  @Schema(
      description =
          "Rótulo de negócio em português apresentado nos formulários dinâmicos e cabeçalhos de tabela.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Rótulo de exibição",
      helpText = "Título amigável apresentado nos formulários e tabelas.",
      group = "Apresentação Editorial",
      order = 40,
      readOnly = true,
      tableHidden = false)
  @ResourceReference(
      resourceKey = "administracao-pessoal.campos-customizados",
      valueField = "id",
      surfaceId = "ficha-campo-customizado",
      previewSurfaceId = "ficha-campo-customizado",
      detailPresentation = "drawer")
  private String label;

  @Schema(
      description =
          "Tipo de dado primitivo do campo customizado (STRING, INTEGER, NUMBER, BOOLEAN, DATE, SELECT).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Tipo de dado",
      helpText = "Natureza primitiva do valor armazenado no JSONB.",
      group = "Tipagem e Componente",
      order = 50,
      readOnly = true,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 2)
  private String fieldType;

  @Schema(
      description =
          "Componente visual canônico do @praxisui/dynamic-fields (INPUT, TEXTAREA, NUMERIC_TEXT_BOX, DATE_PICKER, SELECT, SEARCHABLE_SELECT, CHECKBOX, SWITCH).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Controle visual",
      helpText = "Componente de interface dinâmica associado ao campo.",
      group = "Tipagem e Componente",
      order = 60,
      readOnly = true,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 3)
  private String controlType;

  @Schema(
      description =
          "Texto de ajuda contextual explicando a finalidade de negócio e o preenchimento esperado do campo.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Texto de ajuda",
      helpText = "Orientação contextual exibida no formulário dinâmico.",
      group = "Apresentação Editorial",
      order = 70,
      controlType = FieldControlType.TEXTAREA,
      readOnly = true,
      tableHidden = true)
  private String helpText;

  @Schema(
      description =
          "Nome da seção ou agrupamento visual do formulário dinâmico onde o campo será renderizado.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Grupo do formulário",
      helpText = "Seção do formulário dinâmico onde o campo é exibido.",
      group = "Apresentação Editorial",
      order = 80,
      readOnly = true,
      tableHidden = true,
      quickViewVisible = true,
      quickViewOrder = 4)
  private String groupName;

  @Schema(
      description = "Ordem numérica de posicionamento do campo dentro do grupo visual.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @MicroVisualization(
      kind = MicroVisualizationKind.BULLET,
      max = 100,
      total = 100,
      fallbackText = "Sem ordem",
      ariaLabel = "Ordem no grupo do campo customizado",
      thresholds = {
        @Threshold(min = 0, max = 30, tone = "info", label = "Topo"),
        @Threshold(min = 30, max = 70, tone = "neutral", label = "Intermediário"),
        @Threshold(min = 70, tone = "warning", label = "Final")
      })
  @UISchema(
      label = "Ordem no grupo",
      helpText = "Posição numérica do campo dentro da seção do formulário.",
      group = "Apresentação Editorial",
      order = 90,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "format_list_numbered",
      readOnly = true,
      tableHidden = false)
  private Integer fieldOrder;

  @Schema(
      description =
          "Indica se o preenchimento do campo customizado é obrigatório nas operações de criação e edição.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Preenchimento obrigatório",
      helpText = "Define se o campo exige valor nas submissões de escrita.",
      group = "Validação e Governança",
      order = 100,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "rule",
      labelTrue = "Obrigatório",
      labelFalse = "Opcional",
      toneTrue = "warning",
      toneFalse = "neutral",
      readOnly = true,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 5)
  private Boolean required;

  @Schema(
      description =
          "Referência opcional à tabela geral (tabelas.tab) que fornece a lista de opções quando fieldType = SELECT.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Tabela geral de opções",
      helpText = "Tabela geral que abastece os itens selecionáveis do campo.",
      group = "Validação e Governança",
      order = 110,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "table_chart",
      endpoint = ApiPaths.TABELA_OPTIONS,
      valueField = "id",
      displayField = "label",
      readOnly = true,
      tableHidden = true,
      quickViewVisible = true,
      quickViewOrder = 6)
  @ResourceReference(
      resourceKey = "administracao-pessoal.tabelas",
      valueField = "optionTabela",
      surfaceId = "ficha-tabela",
      previewSurfaceId = "ficha-tabela",
      detailPresentation = "drawer")
  private String optionTabela;

  @Schema(
      description =
          "Descrição funcional da tabela geral associada como fonte de opções do campo customizado.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Finalidade da tabela",
      helpText = "Denominação da tabela geral associada ao campo.",
      group = "Validação e Governança",
      order = 115,
      readOnly = true,
      tableHidden = true,
      formHidden = true)
  @ResourceReference(
      resourceKey = "administracao-pessoal.tabelas",
      valueField = "optionTabela",
      surfaceId = "ficha-tabela",
      previewSurfaceId = "ficha-tabela",
      detailPresentation = "drawer")
  private String optionTabelaDescricao;

  @Schema(
      description =
          "Regras declarativas de validação em JSONB (ex.: min, max, minLength, maxLength, pattern, customMessage).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Regras de validação",
      helpText = "Parâmetros declarativos de validação aplicados ao campo.",
      group = "Validação e Governança",
      order = 120,
      readOnly = true,
      tableHidden = true)
  private Map<String, Object> validationRules = new LinkedHashMap<>();

  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Política de governança LGPD e visibilidade de IA do campo customizado.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.ALLOW, trainingUse = AiTrainingUseMode.DENY))
  @Schema(
      description =
          "Política de governança de domínio (@DomainGovernance / LGPD / AiUsagePolicy) em JSONB.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Política de governança",
      helpText = "Política LGPD e de uso por IA associada ao campo customizado.",
      group = "Validação e Governança",
      order = 130,
      readOnly = true,
      tableHidden = true)
  private Map<String, Object> governancePolicy = new LinkedHashMap<>();

  @Schema(
      description =
          "Indica se a definição do campo customizado está ativa para publicação no schema e validação de payload.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Definição ativa",
      helpText = "Define se o campo customizado é publicado em /schemas/filtered.",
      group = "Validação e Governança",
      order = 140,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "verified",
      labelTrue = "Ativo",
      labelFalse = "Inativo",
      toneTrue = "success",
      toneFalse = "neutral",
      readOnly = true,
      tableHidden = false)
  private Boolean active;

  @Schema(
      description =
          "Instante UTC da última atualização registrada na definição de campo customizado.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Última atualização",
      helpText = "Data e hora UTC da última revisão do registro.",
      group = "Auditoria",
      order = 150,
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_TIME_PICKER,
      icon = "history",
      readOnly = true,
      tableHidden = true,
      formHidden = true)
  private java.time.OffsetDateTime updatedAt;

  @Schema(
      description =
          "Indicador calculado: verdadeiro quando o campo customizado é abastecido por uma tabela geral de opções.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Com tabela de opções",
      helpText = "Fato determinístico derivado de optionTabela.",
      group = "Indicadores",
      order = 160,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "list_alt",
      readOnly = true,
      tableHidden = true,
      formHidden = true)
  private Boolean possuiTabelaOpcoes;

  @Schema(
      description =
          "Indicador calculado: verdadeiro quando o campo customizado possui regras de validação declaradas.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Com regras de validação",
      helpText = "Fato determinístico derivado de validationRules.",
      group = "Indicadores",
      order = 170,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "fact_check",
      readOnly = true,
      tableHidden = true,
      formHidden = true)
  private Boolean possuiRegrasValidacao;

  @Schema(
      description =
          "Indicador calculado: verdadeiro quando o campo customizado é simultaneamente obrigatório e ativo.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Obrigatório e ativo",
      helpText = "Fato determinístico derivado de required e active.",
      group = "Indicadores",
      order = 180,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "verified_user",
      readOnly = true,
      tableHidden = true,
      formHidden = true)
  private Boolean obrigatorioAtivo;
}
