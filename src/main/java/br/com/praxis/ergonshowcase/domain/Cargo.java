package br.com.praxis.ergonshowcase.domain;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Catálogo de cargos efetivos, cargos em comissão e funções gratificadas do quadro de pessoal
 * (equivalente a CARGOS_ no legado Ergon). Inclui exigência de escolaridade, categoria funcional,
 * controle de vagas e extensibilidade via JSONB.
 */
@Getter
@Setter
@Entity
@Table(name = "cargos")
@Comment(
    "Catálogo de cargos efetivos, cargos em comissão e funções gratificadas do quadro de pessoal (equivalente a CARGOS_ no legado Ergon). Inclui exigência de escolaridade, categoria funcional, controle de vagas e extensibilidade via JSONB.")
public class Cargo {
  /** Situação de seleção derivada da extinção; não cria coluna nem altera o cadastro. */
  @org.hibernate.annotations.Formula("case when dt_extincao is null then 'VIGENTE' else 'EXTINTO' end")
  private String situacaoLookup;
  /** Referência relacional para leitura de orgao no assentamento. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "emp_codigo",
      referencedColumnName = "empresa",
      insertable = false,
      updatable = false)
  private Orgao orgao;

  @Id
  /** Código numérico identificador único do cargo ou função no quadro de pessoal. */
  @Comment("Código numérico identificador único do cargo ou função no quadro de pessoal.")
  @Column(name = "cargo", nullable = false)
  private Integer cargo;

  /** Código do órgão público proprietário do quadro de cargos. */
  @Comment("Código do órgão público proprietário do quadro de cargos.")
  @Column(name = "emp_codigo", nullable = false)
  private Integer empCodigo;

  @org.praxisplatform.uischema.annotation.OptionLabel
  @org.praxisplatform.uischema.service.base.annotation.DefaultSortColumn
  /** Nomenclatura oficial do cargo público ou função de confiança. */
  @Comment("Nomenclatura oficial do cargo público ou função de confiança.")
  @Column(name = "nome", nullable = false, length = 120)
  private String nome;

  /**
   * Categoria funcional associada ao cargo (ex.: GERAL, MAGISTERIO, SAUDE, GUARDA_MUNICIPAL,
   * PROCURADORIA).
   */
  @Comment(
      "Categoria funcional associada ao cargo (ex.: GERAL, MAGISTERIO, SAUDE, GUARDA_MUNICIPAL, PROCURADORIA).")
  @Column(name = "categoria", nullable = false, length = 20)
  private String categoria;

  /** Subcategoria ou especialidade funcional vinculada à categoria do cargo. */
  @Comment("Subcategoria ou especialidade funcional vinculada à categoria do cargo.")
  @Column(name = "subcategoria", nullable = true, length = 40)
  private String subcategoria;

  /**
   * Classificação estrutural do cargo (ex.: EFETIVO, COMISSIONADO, FUNCAO_GRATIFICADA, TEMPORARIO).
   */
  @Comment(
      "Classificação estrutural do cargo (ex.: EFETIVO, COMISSIONADO, FUNCAO_GRATIFICADA, TEMPORARIO).")
  @Column(name = "tipo_cargo", nullable = true, length = 40)
  private String tipoCargo;

  /**
   * Indicador se o registro refere-se a [C]argo ou [F]unção de confiança (equivalente a
   * CARGO_FUNCAO no legado).
   */
  @Comment(
      "Indicador se o registro refere-se a [C]argo ou [F]unção de confiança (equivalente a CARGO_FUNCAO no legado).")
  @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.CHAR)
  @Column(name = "cargo_funcao", nullable = false, length = 1, columnDefinition = "character(1)")
  private String cargoFuncao;

  /**
   * Grau de instrução mínimo exigido para provimento no cargo (referência ao domínio ERG_ESCOLA em
   * itemtabelas).
   */
  @Comment(
      "Grau de instrução mínimo exigido para provimento no cargo (referência ao domínio ERG_ESCOLA em itemtabelas).")
  @Column(name = "escolaridade", nullable = true, length = 20)
  private String escolaridade;

  /** Indica se o provimento neste cargo exige verificação de saldo de vagas autorizadas em lei. */
  @Comment(
      "Indica se o provimento neste cargo exige verificação de saldo de vagas autorizadas em lei.")
  @Column(name = "controle_vaga", nullable = false)
  private Boolean controleVaga = false;

  /**
   * Indica se a remuneração do cargo é estruturada por tabela de vencimentos/referências salariais.
   */
  @Comment(
      "Indica se a remuneração do cargo é estruturada por tabela de vencimentos/referências salariais.")
  @Column(name = "por_referencia", nullable = false)
  private Boolean porReferencia = false;

  /** Indica se o cargo atua como aglutinador para fins de controle global de quadro de vagas. */
  @Comment(
      "Indica se o cargo atua como aglutinador para fins de controle global de quadro de vagas.")
  @Column(name = "e_aglutinador", nullable = false)
  private Boolean eAglutinador = false;

  /** Código do cargo pai aglutinador quando o controle de vagas é compartilhado. */
  @Comment("Código do cargo pai aglutinador quando o controle de vagas é compartilhado.")
  @Column(name = "cargo_aglutinador", nullable = true)
  private Integer cargoAglutinador;

  /** Data de início da vigência do controle legal de vagas para este cargo. */
  @Comment("Data de início da vigência do controle legal de vagas para este cargo.")
  @Column(name = "dt_inicio_contr_vaga", nullable = true)
  private LocalDate dtInicioContrVaga;

  /**
   * Data de extinção legal do cargo (NULL para cargos vigentes; impede novos provimentos após a
   * data).
   */
  @Comment(
      "Data de extinção legal do cargo (NULL para cargos vigentes; impede novos provimentos após a data).")
  @Column(name = "dt_extincao", nullable = true)
  private LocalDate dtExtincao;

  /**
   * Referência relacional explícita à lei de criação ou reestruturação do cargo (substitui
   * PONTLEI/PONTPUBL).
   */
  @Comment(
      "Referência relacional explícita à lei de criação ou reestruturação do cargo (substitui PONTLEI/PONTPUBL).")
  @Column(name = "documento_legal_id", nullable = true)
  private Long documentoLegalId;

  /**
   * Atributos de negócio customizados por órgão em formato JSONB governado por
   * custom_field_definitions (substitui FLEX_CAMPO_01..40).
   */
  @Comment(
      "Atributos de negócio customizados por órgão em formato JSONB governado por custom_field_definitions (substitui FLEX_CAMPO_01..40).")
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "custom_fields", nullable = false)
  @org.praxisplatform.uischema.customfields.CustomFields(
      resourceKey = "administracao-pessoal.cargos")
  private Map<String, Object> customFields = new LinkedHashMap<>();

  /** Versão do registro para controle de concorrência otimista (@Version / ETag / If-Match). */
  @Comment(
      "Versão do registro para controle de concorrência otimista (@Version / ETag / If-Match).")
  @Version
  @Column(name = "version", nullable = false)
  // Versão nula identifica entidade nova com código atribuído; JPA inicializa ao persistir.
  private Long version;

  /**
   * Código na Classificação Brasileira de Ocupações (CBO/MTE) de 6 dígitos associado ao cargo para
   * apuração da RAIS e eventos do eSocial (S-1030/S-2200).
   */
  @Comment(
      "Código na Classificação Brasileira de Ocupações (CBO/MTE) de 6 dígitos associado ao cargo para apuração da RAIS e eventos do eSocial (S-1030/S-2200).")
  @Column(name = "cbo", nullable = true, length = 10)
  private String cbo;

  /**
   * Quantidade total de vagas legalmente criadas e autorizadas em lei para provimento no cargo ou
   * função pública.
   */
  @Comment(
      "Quantidade total de vagas legalmente criadas e autorizadas em lei para provimento no cargo ou função pública.")
  @Column(name = "vagas_autorizadas", nullable = true)
  private Integer vagasAutorizadas;

  /** Proveniência temporal do assentamento funcional ou cadastral. */
  @Embedded private AuditoriaErgon auditoria = new AuditoriaErgon();
}
