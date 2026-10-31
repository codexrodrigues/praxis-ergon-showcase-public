package br.com.praxis.ergonshowcase.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Comment;

/**
 * Catálogo detalhado de códigos de ocorrência de frequência, faltas, abonos e licenças/afastamentos
 * (equivalente a CODIGOS_FREQ_ na tela ERGadm00034 do legado Ergon). Preserva a chave composta
 * natural (tipofreq, codigo).
 */
@Getter
@Setter
@Entity
@Table(name = "codigos_frequencia")
@Comment(
    "Catálogo detalhado de códigos de ocorrência de frequência, faltas, abonos e licenças/afastamentos (equivalente a CODIGOS_FREQ_ na tela ERGadm00034 do legado Ergon). Preserva a chave composta natural (tipofreq, codigo).")
public class CodigoFrequencia {
  /** Referência relacional para leitura do tipo macro de frequência no catálogo de códigos. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "tipofreq",
      referencedColumnName = "tipo",
      insertable = false,
      updatable = false)
  private TipoFrequencia tipoFrequenciaEntidade;

  /** Referência relacional para leitura do diploma legal no catálogo de códigos de frequência. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "documento_legal_id",
      referencedColumnName = "id",
      insertable = false,
      updatable = false)
  private DocumentoLegal documentoLegal;

  /** Identidade natural composta, imutável após inclusão. */
  @EmbeddedId private CodigoFrequenciaId id;

  @org.praxisplatform.uischema.annotation.OptionLabel
  @org.praxisplatform.uischema.service.base.annotation.DefaultSortColumn
  /** Descrição completa da ocorrência de frequência ou modalidade de licença/afastamento. */
  @Comment("Descrição completa da ocorrência de frequência ou modalidade de licença/afastamento.")
  @Column(name = "nome", nullable = false, length = 120)
  private String nome;

  /**
   * Sigla mnemônica curta utilizada em espelhos de ponto e mapas de frequência (ex.: FJ, FI, LTS,
   * LMA).
   */
  @Comment(
      "Sigla mnemônica curta utilizada em espelhos de ponto e mapas de frequência (ex.: FJ, FI, LTS, LMA).")
  @Column(name = "mnemonico", nullable = true, length = 6)
  private String mnemonico;

  /** Nome abreviado para exibição compacta em colunas de grade e relatórios. */
  @Comment("Nome abreviado para exibição compacta em colunas de grade e relatórios.")
  @Column(name = "nome_abrev", nullable = true, length = 20)
  private String nomeAbrev;

  /** Indica se o código pode ser lançado diretamente no módulo operacional de frequência. */
  @Comment("Indica se o código pode ser lançado diretamente no módulo operacional de frequência.")
  @Column(name = "via_frequencia", nullable = false)
  private Boolean viaFrequencia = true;

  /**
   * Indica se o afastamento ou ocorrência implica ônus financeiro ou desconto remuneratório para o
   * servidor/órgão.
   */
  @Comment(
      "Indica se o afastamento ou ocorrência implica ônus financeiro ou desconto remuneratório para o servidor/órgão.")
  @Column(name = "onus", nullable = false)
  private Boolean onus = false;

  /**
   * Indica se o código caracteriza um afastamento formal ou licença que suspende/interrompe o
   * exercício regular.
   */
  @Comment(
      "Indica se o código caracteriza um afastamento formal ou licença que suspende/interrompe o exercício regular.")
  @Column(name = "afastamento", nullable = false)
  private Boolean afastamento = false;

  /**
   * Indica se o código está sujeito à validação parametrizada de limites estatutários em
   * regras_frequencia_estatutaria.
   */
  @Comment(
      "Indica se o código está sujeito à validação parametrizada de limites estatutários em regras_frequencia_estatutaria.")
  @Column(name = "regras_freq", nullable = false)
  private Boolean regrasFreq = false;

  /** Indica se o lançamento exige preenchimento explícito dos horários de entrada e saída. */
  @Comment("Indica se o lançamento exige preenchimento explícito dos horários de entrada e saída.")
  @Column(name = "hora_entrada_saida", nullable = false)
  private Boolean horaEntradaSaida = false;

  /**
   * Restrição de aplicabilidade por sexo do servidor: [A]mbos, [F]eminino (ex.:
   * licença-maternidade) ou [M]asculino (ex.: licença-paternidade).
   */
  @Comment(
      "Restrição de aplicabilidade por sexo do servidor: [A]mbos, [F]eminino (ex.: licença-maternidade) ou [M]asculino (ex.: licença-paternidade).")
  @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.CHAR)
  @Column(name = "sexo", nullable = false, length = 1, columnDefinition = "character(1)")
  private String sexo;

  /** Modo de informe da ocorrência (ex.: PERIODO, QUANTIDADE, HORARIO). */
  @Comment("Modo de informe da ocorrência (ex.: PERIODO, QUANTIDADE, HORARIO).")
  @Column(name = "tipo_preenchimento", nullable = true, length = 20)
  private String tipoPreenchimento;

  /** Unidade de medida da quantidade informada (ex.: DIAS, HORAS, OCORRENCIAS). */
  @Comment("Unidade de medida da quantidade informada (ex.: DIAS, HORAS, OCORRENCIAS).")
  @Column(name = "formato_qtd", nullable = true, length = 15)
  private String formatoQtd;

  /** Fator de equivalência em faltas para fins de desconto ou perda de avos de férias/13º. */
  @Comment("Fator de equivalência em faltas para fins de desconto ou perda de avos de férias/13º.")
  @Column(name = "qtd_faltas", nullable = true, precision = 6, scale = 2)
  private BigDecimal qtdFaltas;

  /** Quantidade mínima permitida por lançamento individual deste código. */
  @Comment("Quantidade mínima permitida por lançamento individual deste código.")
  @Column(name = "qtdmin", nullable = true, precision = 8, scale = 2)
  private BigDecimal qtdmin;

  /** Quantidade máxima permitida por lançamento individual deste código. */
  @Comment("Quantidade máxima permitida por lançamento individual deste código.")
  @Column(name = "qtdmax", nullable = true, precision = 8, scale = 2)
  private BigDecimal qtdmax;

  /**
   * Referência relacional explícita ao artigo ou diploma legal que fundamenta o código (substitui
   * PONTLEI).
   */
  @Comment(
      "Referência relacional explícita ao artigo ou diploma legal que fundamenta o código (substitui PONTLEI).")
  @Column(name = "documento_legal_id", nullable = true)
  private Long documentoLegalId;

  /** Versão do registro para controle de concorrência otimista (@Version / ETag / If-Match). */
  @Comment(
      "Versão do registro para controle de concorrência otimista (@Version / ETag / If-Match).")
  @Version
  @Column(name = "version", nullable = false)
  private Long version = 0L;

  /** Proveniência temporal do assentamento funcional ou cadastral. */
  @Embedded private AuditoriaErgon auditoria = new AuditoriaErgon();
}
