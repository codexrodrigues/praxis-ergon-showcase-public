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
import org.praxisplatform.uischema.annotation.OptionLabel;
import org.praxisplatform.uischema.service.base.annotation.DefaultSortColumn;

/**
 * Ocorrências operacionais de frequência, faltas, abonos, atrasos e horas extraordinárias por
 * vínculo e período (equivalente a FREQUENCIAS na tela ERGadm00189 do legado Ergon, sem a tabela
 * sombra FREQUENCIAS_PND). Adota Surrogate PK id imutável + UNIQUE natural (numfunc, numvinc,
 * dtini, tipofreq) para permitir edição segura de datas e tipos em PUT /{id}.
 */
@Getter
@Setter
@Entity
@Table(name = "frequencias")
@Comment(
    "Ocorrências operacionais de frequência, faltas, abonos, atrasos e horas extraordinárias por vínculo e período (equivalente a FREQUENCIAS na tela ERGadm00189 do legado Ergon, sem a tabela sombra FREQUENCIAS_PND). Adota Surrogate PK id imutável + UNIQUE natural (numfunc, numvinc, dtini, tipofreq) para permitir edição segura de datas e tipos em PUT /{id}.")
public class Frequencia {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  /** Chave primária surrogate imutável da ocorrência de frequência. */
  @Comment("Chave primária surrogate imutável da ocorrência de frequência.")
  @Column(name = "id", nullable = false)
  private Long id;

  /** Número funcional do servidor (componente 1 da FK composta para vinculos). */
  @Comment("Número funcional do servidor (componente 1 da FK composta para vinculos).")
  @Column(name = "numfunc", nullable = false)
  private Long numfunc;

  /** Número sequencial do vínculo funcional (componente 2 da FK composta para vinculos). */
  @Comment("Número sequencial do vínculo funcional (componente 2 da FK composta para vinculos).")
  @Column(name = "numvinc", nullable = false)
  private Integer numvinc;

  /** Código do órgão público de lotação do vínculo no momento da ocorrência. */
  @Comment("Código do órgão público de lotação do vínculo no momento da ocorrência.")
  @Column(name = "emp_codigo", nullable = false)
  private Integer empCodigo;

  /** Data de início da ocorrência de frequência ou falta. */
  @Comment("Data de início da ocorrência de frequência ou falta.")
  @DefaultSortColumn(ascending = false)
  @Column(name = "dtini", nullable = false)
  private LocalDate dtini;

  /**
   * Data final da ocorrência de frequência (igual a dtini para evento diário ou superior para
   * período contínuo).
   */
  @Comment(
      "Data final da ocorrência de frequência (igual a dtini para evento diário ou superior para período contínuo).")
  @Column(name = "dtfim", nullable = true)
  private LocalDate dtfim;

  /** Tipo macro da ocorrência de frequência (referência a tipos_frequencia). */
  @Comment("Tipo macro da ocorrência de frequência (referência a tipos_frequencia).")
  @Column(name = "tipofreq", nullable = false, length = 20)
  private String tipofreq;

  /** Código detalhado da ocorrência de frequência (referência composta a codigos_frequencia). */
  @Comment(
      "Código detalhado da ocorrência de frequência (referência composta a codigos_frequencia).")
  @Column(name = "codfreq", nullable = false)
  private Integer codfreq;

  /**
   * Quantidade apurada na ocorrência (em dias, horas ou minutos conforme formato_qtd do código).
   */
  @Comment(
      "Quantidade apurada na ocorrência (em dias, horas ou minutos conforme formato_qtd do código).")
  @Column(name = "quantidade", nullable = true, precision = 8, scale = 2)
  private BigDecimal quantidade;

  /** Horário de entrada registrado na ocorrência, quando o código exige controle horário. */
  @Comment("Horário de entrada registrado na ocorrência, quando o código exige controle horário.")
  @Column(name = "hora_entrada", nullable = true)
  private LocalTime horaEntrada;

  /** Horário de saída registrado na ocorrência, quando o código exige controle horário. */
  @Comment("Horário de saída registrado na ocorrência, quando o código exige controle horário.")
  @Column(name = "hora_saida", nullable = true)
  private LocalTime horaSaida;

  /**
   * Estado do workflow de aprovação da ocorrência (PENDENTE_HOMOLOGACAO, HOMOLOGADO, REJEITADO,
   * CANCELADO), substituindo FREQUENCIAS_PND e alimentando @WorkflowAction no Praxis.
   */
  @Comment(
      "Estado do workflow de aprovação da ocorrência (PENDENTE_HOMOLOGACAO, HOMOLOGADO, REJEITADO, CANCELADO), substituindo FREQUENCIAS_PND e alimentando @WorkflowAction no Praxis.")
  @Column(name = "situacao", nullable = false, length = 30)
  private String situacao;

  /** Justificativa, motivo de abono ou observação operacional registrada para a ocorrência. */
  @Comment("Justificativa, motivo de abono ou observação operacional registrada para a ocorrência.")
  @OptionLabel
  @Column(name = "obs", nullable = true, length = 2000)
  private String obs;

  /**
   * Referência relacional explícita ao documento comprobatório, portaria de abono ou processo
   * anexo.
   */
  @Comment(
      "Referência relacional explícita ao documento comprobatório, portaria de abono ou processo anexo.")
  @Column(name = "documento_legal_id", nullable = true)
  private Long documentoLegalId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "emp_codigo",
      referencedColumnName = "empresa",
      insertable = false,
      updatable = false)
  private Orgao orgao;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "numfunc",
      referencedColumnName = "numero",
      insertable = false,
      updatable = false)
  private ServidorPessoa pessoa;

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

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "tipofreq",
      referencedColumnName = "tipo",
      insertable = false,
      updatable = false)
  private TipoFrequencia tipoFrequenciaEntidade;

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

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "documento_legal_id",
      referencedColumnName = "id",
      insertable = false,
      updatable = false)
  private DocumentoLegal documentoLegal;

  /**
   * Atributos de negócio customizados por órgão em formato JSONB governado por
   * custom_field_definitions (substitui FLEX_CAMPO_01..40).
   */
  @Comment(
      "Atributos de negócio customizados por órgão em formato JSONB governado por custom_field_definitions (substitui FLEX_CAMPO_01..40).")
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "custom_fields", nullable = false)
  @org.praxisplatform.uischema.customfields.CustomFields(
      resourceKey = "administracao-pessoal.frequencias")
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
