package br.com.praxis.ergonshowcase.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Matriz parametrizada de regras e limites estatutários de frequência e afastamento por órgão,
 * regime jurídico, tipo de vínculo e categoria (evolução Praxis-Native de REGRAS_FREQ na tela
 * ERGadm00036). Alimenta os Fact Providers do praxis-rules-engine para avaliação determinística de
 * durações, interstícios, limites mensais/anuais, concomitância e necessidade de homologação.
 */
@Getter
@Setter
@Entity
@Table(name = "regras_frequencia_estatutaria")
@Comment(
    "Matriz parametrizada de regras e limites estatutários de frequência e afastamento por órgão, regime jurídico, tipo de vínculo e categoria (evolução Praxis-Native de REGRAS_FREQ na tela ERGadm00036). Alimenta os Fact Providers do praxis-rules-engine para avaliação determinística de durações, interstícios, limites mensais/anuais, concomitância e necessidade de homologação.")
public class RegraFrequenciaEstatutaria {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  /** Identificador único surrogate da regra estatutária. */
  @Comment("Identificador único surrogate da regra estatutária.")
  @Column(name = "id", nullable = false)
  private Long id;

  /**
   * Código do órgão público ao qual a regra se aplica (NULL para regra estatutária padrão de todos
   * os órgãos do tenant).
   */
  @Comment(
      "Código do órgão público ao qual a regra se aplica (NULL para regra estatutária padrão de todos os órgãos do tenant).")
  @Column(name = "emp_codigo", nullable = true)
  private Integer empCodigo;

  /** Regime jurídico alvo da regra (ex.: ESTATUTARIO, CLT; equivale a REG_JUR no legado). */
  @Comment("Regime jurídico alvo da regra (ex.: ESTATUTARIO, CLT; equivale a REG_JUR no legado).")
  @Column(name = "regime_juridico", nullable = false, length = 20)
  private String regimeJuridico;

  /**
   * Tipo de vínculo funcional alvo da regra (ex.: EFETIVO, COMISSAO, TEMPORARIO; equivale a
   * TIPO_VINC no legado).
   */
  @Comment(
      "Tipo de vínculo funcional alvo da regra (ex.: EFETIVO, COMISSAO, TEMPORARIO; equivale a TIPO_VINC no legado).")
  @Column(name = "tipo_vinculo", nullable = false, length = 20)
  private String tipoVinculo;

  /**
   * Categoria funcional alvo da regra (ex.: GERAL, MAGISTERIO, GUARDA_MUNICIPAL ou TODAS; equivale
   * a CATEG no legado).
   */
  @Comment(
      "Categoria funcional alvo da regra (ex.: GERAL, MAGISTERIO, GUARDA_MUNICIPAL ou TODAS; equivale a CATEG no legado).")
  @Column(name = "categoria", nullable = false, length = 20)
  private String categoria;

  /**
   * Subcategoria funcional específica quando a regra distingue especialidades (equivale a SUBCATEG
   * no legado).
   */
  @Comment(
      "Subcategoria funcional específica quando a regra distingue especialidades (equivale a SUBCATEG no legado).")
  @Column(name = "subcategoria", nullable = true, length = 40)
  private String subcategoria;

  /** Tipo de frequência regulado pela regra (equivale a TIPOFREQ no legado). */
  @Comment("Tipo de frequência regulado pela regra (equivale a TIPOFREQ no legado).")
  @Column(name = "tipofreq", nullable = false, length = 20)
  private String tipofreq;

  /** Código específico de frequência ou afastamento regulado (equivale a CODIGO no legado). */
  @Comment("Código específico de frequência ou afastamento regulado (equivale a CODIGO no legado).")
  @Column(name = "codfreq", nullable = false)
  private Integer codfreq;

  /** Data de início de vigência legal do parâmetro estatutário. */
  @Comment("Data de início de vigência legal do parâmetro estatutário.")
  @Column(name = "dtini", nullable = false)
  private LocalDate dtini;

  /** Data de fim de vigência legal do parâmetro estatutário (NULL quando vigente). */
  @Comment("Data de fim de vigência legal do parâmetro estatutário (NULL quando vigente).")
  @Column(name = "dtfim", nullable = true)
  private LocalDate dtfim;

  /** Duração mínima exigida em dias por evento de concessão (equivale a QTD_MIN em REGRAS_FREQ). */
  @Comment(
      "Duração mínima exigida em dias por evento de concessão (equivale a QTD_MIN em REGRAS_FREQ).")
  @Column(name = "qtd_min_dias", nullable = true, precision = 8, scale = 2)
  private BigDecimal qtdMinDias;

  /**
   * Duração máxima permitida em dias por evento de concessão (equivale a QTD_MAX em REGRAS_FREQ).
   */
  @Comment(
      "Duração máxima permitida em dias por evento de concessão (equivale a QTD_MAX em REGRAS_FREQ).")
  @Column(name = "qtd_max_dias", nullable = true, precision = 8, scale = 2)
  private BigDecimal qtdMaxDias;

  /**
   * Interstício mínimo obrigatório em dias entre duas concessões da mesma espécie (equivale a
   * INTERV_MIN).
   */
  @Comment(
      "Interstício mínimo obrigatório em dias entre duas concessões da mesma espécie (equivale a INTERV_MIN).")
  @Column(name = "interv_min_dias", nullable = true)
  private Integer intervMinDias;

  /**
   * Prazo limite em dias após o término para que novo lançamento seja caracterizado como
   * prorrogação (equivale a INT_PRORROG).
   */
  @Comment(
      "Prazo limite em dias após o término para que novo lançamento seja caracterizado como prorrogação (equivale a INT_PRORROG).")
  @Column(name = "int_prorrog_dias", nullable = true)
  private Integer intProrrogDias;

  /**
   * Limite máximo de dias consecutivos permitidos dentro do mesmo mês competência
   * (QTD_MAX_CONSEC_MES).
   */
  @Comment(
      "Limite máximo de dias consecutivos permitidos dentro do mesmo mês competência (QTD_MAX_CONSEC_MES).")
  @Column(name = "qtd_max_consec_mes", nullable = true)
  private Integer qtdMaxConsecMes;

  /**
   * Limite máximo de dias interpolados/acumulados permitidos dentro do mesmo mês competência
   * (QTD_MAX_INTERP_MES).
   */
  @Comment(
      "Limite máximo de dias interpolados/acumulados permitidos dentro do mesmo mês competência (QTD_MAX_INTERP_MES).")
  @Column(name = "qtd_max_interp_mes", nullable = true)
  private Integer qtdMaxInterpMes;

  /**
   * Limite máximo de dias consecutivos permitidos dentro do exercício anual (QTD_MAX_CONSEC_ANO).
   */
  @Comment(
      "Limite máximo de dias consecutivos permitidos dentro do exercício anual (QTD_MAX_CONSEC_ANO).")
  @Column(name = "qtd_max_consec_ano", nullable = true)
  private Integer qtdMaxConsecAno;

  /**
   * Limite máximo de dias interpolados/acumulados permitidos dentro do exercício anual
   * (QTD_MAX_INTERP_ANO).
   */
  @Comment(
      "Limite máximo de dias interpolados/acumulados permitidos dentro do exercício anual (QTD_MAX_INTERP_ANO).")
  @Column(name = "qtd_max_interp_ano", nullable = true)
  private Integer qtdMaxInterpAno;

  /**
   * Indica se o lançamento deste código exige transição inicial para PENDENTE_HOMOLOGACAO e
   * aprovação via @WorkflowAction.
   */
  @Comment(
      "Indica se o lançamento deste código exige transição inicial para PENDENTE_HOMOLOGACAO e aprovação via @WorkflowAction.")
  @Column(name = "exige_homologacao", nullable = false)
  private Boolean exigeHomologacao = false;

  /**
   * Indica se a regra admite sobreposição de período com outra ocorrência ou afastamento ativo do
   * mesmo vínculo.
   */
  @Comment(
      "Indica se a regra admite sobreposição de período com outra ocorrência ou afastamento ativo do mesmo vínculo.")
  @Column(name = "permite_concomitancia", nullable = false)
  private Boolean permiteConcomitancia = false;

  /**
   * Indica vedação expressa do código de frequência/afastamento para a combinação de regime,
   * vínculo e categoria (NAO_SE_APLICA).
   */
  @Comment(
      "Indica vedação expressa do código de frequência/afastamento para a combinação de regime, vínculo e categoria (NAO_SE_APLICA).")
  @Column(name = "nao_se_aplica", nullable = false)
  private Boolean naoSeAplica = false;

  /**
   * Chave canônica do RuleSet no praxis-rules-engine que consome esta parametrização (ex.:
   * administracao-pessoal.afastamentos.validacao-estatutaria).
   */
  @Comment(
      "Chave canônica do RuleSet no praxis-rules-engine que consome esta parametrização (ex.: administracao-pessoal.afastamentos.validacao-estatutaria).")
  @Column(name = "rule_key", nullable = false, length = 120)
  private String ruleKey;

  /**
   * Parâmetros extras estruturados em JSONB (ex.: exige_laudo_junta_apos_dias, cid_obrigatorio,
   * expressao_json_logic) para Fact Providers.
   */
  @Comment(
      "Parâmetros extras estruturados em JSONB (ex.: exige_laudo_junta_apos_dias, cid_obrigatorio, expressao_json_logic) para Fact Providers.")
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "rule_params", nullable = false)
  private Map<String, Object> ruleParams = new LinkedHashMap<>();

  /**
   * Referência relacional ao artigo do Estatuto dos Servidores ou decreto que instituiu o limite.
   */
  @Comment(
      "Referência relacional ao artigo do Estatuto dos Servidores ou decreto que instituiu o limite.")
  @Column(name = "documento_legal_id", nullable = true)
  private Long documentoLegalId;

  /** Indica se a regra parametrizada está ativa para avaliação pelo motor de regras. */
  @Comment("Indica se a regra parametrizada está ativa para avaliação pelo motor de regras.")
  @Column(name = "ativa", nullable = false)
  private Boolean ativa = true;

  /** Versão do registro para controle de concorrência otimista (@Version / ETag / If-Match). */
  @Comment(
      "Versão do registro para controle de concorrência otimista (@Version / ETag / If-Match).")
  @Version
  @Column(name = "version", nullable = false)
  private Long version = 0L;

  /** Proveniência temporal do assentamento funcional ou cadastral. */
  @Embedded private AuditoriaErgon auditoria = new AuditoriaErgon();
}
