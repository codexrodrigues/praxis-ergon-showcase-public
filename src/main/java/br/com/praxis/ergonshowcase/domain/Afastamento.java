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
 * Licenças e afastamentos funcionais do servidor por vínculo e período (equivalente a LIC_AFAST no
 * legado Ergon, sem a tabela sombra LIC_AFAST_PND). Adota Surrogate PK id imutável + UNIQUE natural
 * (numfunc, numvinc, dtini, codfreq) e integra-se ao praxis-rules-engine para controle de
 * concomitância, limites estatutários e workflow de homologação.
 */
@Getter
@Setter
@Entity
@Table(name = "afastamentos")
@Comment(
    "Licenças e afastamentos funcionais do servidor por vínculo e período (equivalente a LIC_AFAST no legado Ergon, sem a tabela sombra LIC_AFAST_PND). Adota Surrogate PK id imutável + UNIQUE natural (numfunc, numvinc, dtini, codfreq) e integra-se ao praxis-rules-engine para controle de concomitância, limites estatutários e workflow de homologação.")
public class Afastamento {
  /** Referência relacional para leitura do órgão público do vínculo afastado. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "emp_codigo",
      referencedColumnName = "empresa",
      insertable = false,
      updatable = false)
  private Orgao orgao;

  /** Referência relacional para leitura da pessoa servidora titular do afastamento. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "numfunc",
      referencedColumnName = "numero",
      insertable = false,
      updatable = false)
  private ServidorPessoa pessoa;

  /** Referência relacional para leitura do vínculo funcional titular do afastamento. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumns({
    @JoinColumn(
        name = "numfunc",
        referencedColumnName = "numfunc",
        insertable = false,
        updatable = false),
    @JoinColumn(
        name = "numvinc",
        referencedColumnName = "numero",
        insertable = false,
        updatable = false)
  })
  private Vinculo vinculo;

  /** Referência relacional para leitura do tipo macro de frequência do afastamento. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "tipofreq",
      referencedColumnName = "tipo",
      insertable = false,
      updatable = false)
  private TipoFrequencia tipoFrequenciaEntidade;

  /** Referência relacional para leitura do código de frequência específico do afastamento. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumns({
    @JoinColumn(
        name = "tipofreq",
        referencedColumnName = "tipofreq",
        insertable = false,
        updatable = false),
    @JoinColumn(
        name = "codfreq",
        referencedColumnName = "codigo",
        insertable = false,
        updatable = false)
  })
  private CodigoFrequencia codigoFrequenciaEntidade;

  /** Referência relacional para leitura do afastamento original prorrogado. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "afastamento_origem_id",
      referencedColumnName = "id",
      insertable = false,
      updatable = false)
  private Afastamento afastamentoOrigem;

  /** Referência relacional para leitura do servidor designado como substituto. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "numfunc_substituto",
      referencedColumnName = "numero",
      insertable = false,
      updatable = false)
  private ServidorPessoa pessoaSubstituta;

  /** Referência relacional para leitura da portaria ou laudo pericial de concessão. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "documento_legal_id",
      referencedColumnName = "id",
      insertable = false,
      updatable = false)
  private DocumentoLegal documentoLegal;

  /** Referência relacional para leitura do ato de publicação oficial do afastamento. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "ato_publicacao_id",
      referencedColumnName = "id",
      insertable = false,
      updatable = false)
  private DocumentoLegal atoPublicacao;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  /** Chave primária surrogate imutável do registro de licença ou afastamento. */
  @Comment("Chave primária surrogate imutável do registro de licença ou afastamento.")
  @Column(name = "id", nullable = false)
  private Long id;

  /** Número funcional do servidor afastado (componente 1 da FK composta para vinculos). */
  @Comment("Número funcional do servidor afastado (componente 1 da FK composta para vinculos).")
  @Column(name = "numfunc", nullable = false)
  private Long numfunc;

  /**
   * Número sequencial do vínculo funcional afastado (componente 2 da FK composta para vinculos).
   */
  @Comment(
      "Número sequencial do vínculo funcional afastado (componente 2 da FK composta para vinculos).")
  @Column(name = "numvinc", nullable = false)
  private Integer numvinc;

  /** Código do órgão público do vínculo funcional. */
  @Comment("Código do órgão público do vínculo funcional.")
  @Column(name = "emp_codigo", nullable = false)
  private Integer empCodigo;

  /** Data de início efetivo da licença ou afastamento. */
  @Comment("Data de início efetivo da licença ou afastamento.")
  @Column(name = "dtini", nullable = false)
  private LocalDate dtini;

  /** Data originalmente prevista para o término da licença ou afastamento (DTPREVFIM). */
  @Comment("Data originalmente prevista para o término da licença ou afastamento (DTPREVFIM).")
  @Column(name = "dtprevfim", nullable = true)
  private LocalDate dtprevfim;

  /** Data de término efetivo ou retorno antecipado do afastamento. */
  @Comment("Data de término efetivo ou retorno antecipado do afastamento.")
  @Column(name = "dtfim", nullable = true)
  private LocalDate dtfim;

  /** Tipo de frequência associado à modalidade de afastamento (ex.: LICENCA, AFASTAMENTO). */
  @Comment("Tipo de frequência associado à modalidade de afastamento (ex.: LICENCA, AFASTAMENTO).")
  @Column(name = "tipofreq", nullable = false, length = 20)
  private String tipofreq;

  /**
   * Código específico da licença ou afastamento (componente da chave natural UNIQUE e FK para
   * codigos_frequencia).
   */
  @Comment(
      "Código específico da licença ou afastamento (componente da chave natural UNIQUE e FK para codigos_frequencia).")
  @Column(name = "codfreq", nullable = false)
  private Integer codfreq;

  /** Duração apurada em dias corridos do período de afastamento. */
  @Comment("Duração apurada em dias corridos do período de afastamento.")
  @Column(name = "quantidade_dias", nullable = true)
  private Integer quantidadeDias;

  /**
   * Estado do ciclo de vida e aprovação da licença (PENDENTE_HOMOLOGACAO, HOMOLOGADO, REJEITADO,
   * CANCELADO), substituindo LIC_AFAST_PND.
   */
  @Comment(
      "Estado do ciclo de vida e aprovação da licença (PENDENTE_HOMOLOGACAO, HOMOLOGADO, REJEITADO, CANCELADO), substituindo LIC_AFAST_PND.")
  @Column(name = "situacao", nullable = false, length = 30)
  private String situacao;

  @org.praxisplatform.uischema.annotation.OptionLabel
  @org.praxisplatform.uischema.service.base.annotation.DefaultSortColumn
  /** Fundamentação clínica, administrativa ou jurídica detalhada do afastamento (MOTIVO). */
  @Comment("Fundamentação clínica, administrativa ou jurídica detalhada do afastamento (MOTIVO).")
  @Column(name = "motivo", nullable = true, length = 4000)
  private String motivo;

  /**
   * Indica se este lançamento representa prorrogação contínua de licença imediatamente anterior.
   */
  @Comment(
      "Indica se este lançamento representa prorrogação contínua de licença imediatamente anterior.")
  @Column(name = "prorrogacao", nullable = false)
  private Boolean prorrogacao = false;

  /** Referência ao afastamento original quando o registro atual é uma prorrogação. */
  @Comment("Referência ao afastamento original quando o registro atual é uma prorrogação.")
  @Column(name = "afastamento_origem_id", nullable = true)
  private Long afastamentoOrigemId;

  /**
   * Número funcional do servidor designado para substituição durante o afastamento, quando
   * aplicável.
   */
  @Comment(
      "Número funcional do servidor designado para substituição durante o afastamento, quando aplicável.")
  @Column(name = "numfunc_substituto", nullable = true)
  private Long numfuncSubstituto;

  /** Número do vínculo do servidor substituto designado. */
  @Comment("Número do vínculo do servidor substituto designado.")
  @Column(name = "numvinc_substituto", nullable = true)
  private Integer numvincSubstituto;

  /**
   * Referência relacional explícita à portaria de concessão ou laudo pericial oficial (substitui
   * PONTLEI/RESULTPRONT).
   */
  @Comment(
      "Referência relacional explícita à portaria de concessão ou laudo pericial oficial (substitui PONTLEI/RESULTPRONT).")
  @Column(name = "documento_legal_id", nullable = true)
  private Long documentoLegalId;

  /**
   * Referência relacional explícita à publicação do afastamento em Diário Oficial (substitui
   * PONTPUBL/PONTPROC/PONTARQS).
   */
  @Comment(
      "Referência relacional explícita à publicação do afastamento em Diário Oficial (substitui PONTPUBL/PONTPROC/PONTARQS).")
  @Column(name = "ato_publicacao_id", nullable = true)
  private Long atoPublicacaoId;

  /**
   * Atributos de negócio customizados por órgão em formato JSONB governado por
   * custom_field_definitions (ex.: cid_atestado, numero_laudo_pericial, processo_sei_licenca).
   */
  @Comment(
      "Atributos de negócio customizados por órgão em formato JSONB governado por custom_field_definitions (ex.: cid_atestado, numero_laudo_pericial, processo_sei_licenca).")
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "custom_fields", nullable = false)
  @org.praxisplatform.uischema.customfields.CustomFields(
      resourceKey = "administracao-pessoal.afastamentos")
  private Map<String, Object> customFields = new LinkedHashMap<>();

  /** Versão do registro para controle de concorrência otimista (@Version / ETag / If-Match). */
  @Comment(
      "Versão do registro para controle de concorrência otimista (@Version / ETag / If-Match).")
  @Version
  @Column(name = "version", nullable = false)
  private Long version = 0L;

  /** Proveniência temporal do assentamento funcional ou cadastral. */
  @Embedded private AuditoriaErgon auditoria = new AuditoriaErgon();
}
