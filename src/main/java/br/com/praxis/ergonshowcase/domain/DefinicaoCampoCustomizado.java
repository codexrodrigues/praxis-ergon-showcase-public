package br.com.praxis.ergonshowcase.domain;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.praxisplatform.uischema.annotation.OptionLabel;
import org.praxisplatform.uischema.service.base.annotation.DefaultSortColumn;

/**
 * Catálogo governado de metadados de campos customizados multi-órgão (inspirado em Workday Custom
 * Fields / SAP SuccessFactors MDF). Substitui as 40 colunas físicas FLEX_CAMPO_01..40 e projeta
 * dinamicamente propriedades tipadas, controles x-ui, opções de tabelas gerais, validações e
 * políticas LGPD/IA em /schemas/filtered do praxis-metadata-starter.
 */
@Getter
@Setter
@Entity
@Table(name = "custom_field_definitions")
@Comment(
    "Catálogo governado de metadados de campos customizados multi-órgão (inspirado em Workday Custom Fields / SAP SuccessFactors MDF). Substitui as 40 colunas físicas FLEX_CAMPO_01..40 e projeta dinamicamente propriedades tipadas, controles x-ui, opções de tabelas gerais, validações e políticas LGPD/IA em /schemas/filtered do praxis-metadata-starter.")
public class DefinicaoCampoCustomizado {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  /** Identificador único surrogate da definição de campo customizado. */
  @Comment("Identificador único surrogate da definição de campo customizado.")
  @Column(name = "id", nullable = false)
  private Long id;

  /**
   * Código do órgão público proprietário da customização (NULL quando o campo customizado é global
   * para todos os órgãos do tenant).
   */
  @Comment(
      "Código do órgão público proprietário da customização (NULL quando o campo customizado é global para todos os órgãos do tenant).")
  @Column(name = "emp_codigo", nullable = true)
  private Integer empCodigo;

  /**
   * Chave canônica do recurso Praxis estendido (ex.: administracao-pessoal.funcionarios,
   * administracao-pessoal.vinculos, administracao-pessoal.frequencias,
   * administracao-pessoal.afastamentos).
   */
  @Comment(
      "Chave canônica do recurso Praxis estendido (ex.: administracao-pessoal.funcionarios, administracao-pessoal.vinculos, administracao-pessoal.frequencias, administracao-pessoal.afastamentos).")
  @Column(name = "resource_key", nullable = false, length = 120)
  private String resourceKey;

  /**
   * Chave semântica em snake_case armazenada dentro do objeto custom_fields JSONB da tabela alvo
   * (ex.: numero_calcado, processo_sei_nomeacao, cid_atestado).
   */
  @Comment(
      "Chave semântica em snake_case armazenada dentro do objeto custom_fields JSONB da tabela alvo (ex.: numero_calcado, processo_sei_nomeacao, cid_atestado).")
  @Column(name = "field_key", nullable = false, length = 80)
  private String fieldKey;

  /**
   * Tipo de dado primitivo do campo customizado (STRING, INTEGER, NUMBER, BOOLEAN, DATE, SELECT).
   */
  @Comment(
      "Tipo de dado primitivo do campo customizado (STRING, INTEGER, NUMBER, BOOLEAN, DATE, SELECT).")
  @Column(name = "field_type", nullable = false, length = 30)
  private String fieldType;

  /**
   * Componente visual canônico do @praxisui/dynamic-fields (INPUT, TEXTAREA, NUMERIC_TEXT_BOX,
   * DATE_PICKER, SELECT, SEARCHABLE_SELECT, CHECKBOX, SWITCH).
   */
  @Comment(
      "Componente visual canônico do @praxisui/dynamic-fields (INPUT, TEXTAREA, NUMERIC_TEXT_BOX, DATE_PICKER, SELECT, SEARCHABLE_SELECT, CHECKBOX, SWITCH).")
  @Column(name = "control_type", nullable = false, length = 40)
  private String controlType;

  /**
   * Rótulo de negócio em português apresentado nos formulários dinâmicos e cabeçalhos de tabela.
   */
  @Comment(
      "Rótulo de negócio em português apresentado nos formulários dinâmicos e cabeçalhos de tabela.")
  @OptionLabel
  @Column(name = "label", nullable = false, length = 120)
  private String label;

  /**
   * Texto de ajuda contextual explicando a finalidade de negócio e o preenchimento esperado do
   * campo.
   */
  @Comment(
      "Texto de ajuda contextual explicando a finalidade de negócio e o preenchimento esperado do campo.")
  @Column(name = "help_text", nullable = true, length = 500)
  private String helpText;

  /** Nome da seção ou agrupamento visual do formulário dinâmico onde o campo será renderizado. */
  @Comment(
      "Nome da seção ou agrupamento visual do formulário dinâmico onde o campo será renderizado.")
  @Column(name = "group_name", nullable = true, length = 120)
  private String groupName;

  /** Ordem numérica de posicionamento do campo dentro do grupo visual. */
  @Comment("Ordem numérica de posicionamento do campo dentro do grupo visual.")
  @DefaultSortColumn
  @Column(name = "field_order", nullable = false)
  private Integer fieldOrder;

  /**
   * Indica se o preenchimento do campo customizado é obrigatório nas operações de criação e edição.
   */
  @Comment(
      "Indica se o preenchimento do campo customizado é obrigatório nas operações de criação e edição.")
  @Column(name = "required", nullable = false)
  private Boolean required = false;

  /**
   * Referência opcional à tabela geral (tabelas.tab) que fornece a lista de opções quando
   * field_type = SELECT.
   */
  @Comment(
      "Referência opcional à tabela geral (tabelas.tab) que fornece a lista de opções quando field_type = SELECT.")
  @Column(name = "option_tabela", nullable = true, length = 40)
  private String optionTabela;

  /**
   * Regras declarativas de validação em JSONB (ex.: min, max, minLength, maxLength, pattern,
   * customMessage) consumidas pelo backend e pelo @praxisui/dynamic-form.
   */
  @Comment(
      "Regras declarativas de validação em JSONB (ex.: min, max, minLength, maxLength, pattern, customMessage) consumidas pelo backend e pelo @praxisui/dynamic-form.")
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "validation_rules", nullable = false)
  private Map<String, Object> validationRules = new LinkedHashMap<>();

  /**
   * Política de governança de domínio (@DomainGovernance / LGPD / AiUsagePolicy) em JSONB definindo
   * classificação de sensibilidade, mascaramento e permissão de uso por IA.
   */
  @Comment(
      "Política de governança de domínio (@DomainGovernance / LGPD / AiUsagePolicy) em JSONB definindo classificação de sensibilidade, mascaramento e permissão de uso por IA.")
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "governance_policy", nullable = false)
  private Map<String, Object> governancePolicy = new LinkedHashMap<>();

  /**
   * Indica se a definição do campo customizado está ativa para publicação no schema e validação de
   * payload.
   */
  @Comment(
      "Indica se a definição do campo customizado está ativa para publicação no schema e validação de payload.")
  @Column(name = "active", nullable = false)
  private Boolean active = true;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "emp_codigo",
      referencedColumnName = "empresa",
      insertable = false,
      updatable = false)
  private Orgao orgao;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "option_tabela",
      referencedColumnName = "tab",
      insertable = false,
      updatable = false)
  private Tabela optionTabelaEntidade;

  /** Versão do registro para controle de concorrência otimista (@Version / ETag / If-Match). */
  @Comment(
      "Versão do registro para controle de concorrência otimista (@Version / ETag / If-Match).")
  @Version
  @Column(name = "version", nullable = false)
  private Long version = 0L;

  /** Proveniência temporal do assentamento funcional ou cadastral. */
  @Embedded private AuditoriaErgon auditoria = new AuditoriaErgon();
}
