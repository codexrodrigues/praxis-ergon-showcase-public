package br.com.praxis.ergonshowcase.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Período aquisitivo de férias do vínculo funcional, com contabilidade de dias de direito, faltas,
 * afastamentos suspensivos, saldo disponível, controle de prescrição/multa e overrides individuais
 * sobre os parâmetros estatutários ({@code PERAQFER} / {@code PCK_PERAQFER} / Tarefa 24610).
 */
@Entity
@Table(name = "periodos_aquisitivos_ferias")
@Comment(
    "Período aquisitivo de férias do vínculo funcional com contabilidade de direito, saldo, prescrição e overrides individuais da Tarefa 24610 (equivalente a PERAQFER no legado Ergon).")
@Getter
@Setter
@NoArgsConstructor
public class PeriodoAquisitivoFerias {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Column(name = "emp_codigo", nullable = false)
  private Integer empCodigo;

  @Column(name = "numfunc", nullable = false)
  private Long numfunc;

  @Column(name = "numvinc", nullable = false)
  private Integer numvinc;

  @org.praxisplatform.uischema.annotation.OptionLabel
  @Column(name = "chave", nullable = false)
  private Integer chave;

  @Column(name = "exercicio")
  private Integer exercicio;

  @org.praxisplatform.uischema.service.base.annotation.DefaultSortColumn
  @Column(name = "dtini", nullable = false)
  private LocalDate dtini;

  @Column(name = "dtfim", nullable = false)
  private LocalDate dtfim;

  @Column(name = "tot_dias_direito", nullable = false)
  private Integer totDiasDireito = 30;

  @Column(name = "faltas_apuradas", nullable = false)
  private Integer faltasApuradas = 0;

  @Column(name = "dias_afastamento_suspensivo", nullable = false)
  private Integer diasAfastamentoSuspensivo = 0;

  @Column(name = "dias_gozados", nullable = false)
  private Integer diasGozados = 0;

  @Column(name = "dias_vendidos_abono", nullable = false)
  private Integer diasVendidosAbono = 0;

  @Column(name = "dias_convertidos_dobro", nullable = false)
  private Integer diasConvertidosDobro = 0;

  @Column(name = "saldo_dias_disponivel", nullable = false)
  private Integer saldoDiasDisponivel = 30;

  @Column(name = "situacao", nullable = false, length = 30)
  private String situacao = "DISPONIVEL";

  @Column(name = "dt_prescricao")
  private LocalDate dtPrescricao;

  @Column(name = "criterio_prescricao", length = 1)
  private String criterioPrescricao = "I";

  @Column(name = "permite_gozo_pos_prescricao", length = 1)
  private String permiteGozoPosPrescricao = "I";

  @Column(name = "dt_pgto_com_multa")
  private LocalDate dtPgtoComMulta;

  @Column(name = "sem_pgto_multa", length = 1)
  private String semPgtoMulta = "N";

  @Column(name = "max_per_fer")
  private Integer maxPerFer;

  @Column(name = "min_per_fer")
  private Integer minPerFer;

  @Column(name = "max_dias_per_fer")
  private Integer maxDiasPerFer;

  @Column(name = "min_dias_per_fer")
  private Integer minDiasPerFer;

  @Column(name = "min_dias_per_princ_fer")
  private Integer minDiasPerPrincFer;

  @Column(name = "abono_dir_gozo", length = 1)
  private String abonoDirGozo = "I";

  @Column(name = "abono_const_fer", length = 1)
  private String abonoConstFer = "I";

  @Column(name = "adiant_13_fer", length = 1)
  private String adiant13Fer = "I";

  @Column(name = "adiant_sal_fer", length = 1)
  private String adiantSalFer = "I";

  @Column(name = "emprest_fer", length = 1)
  private String emprestFer = "I";

  @Column(name = "permite_fer_este_per", length = 1)
  private String permiteFerEstePer = "I";

  @Column(name = "max_dias_vend_fer")
  private Integer maxDiasVendFer;

  @Column(name = "min_dias_pvend_fer")
  private Integer minDiasPvendFer;

  @Column(name = "num_dias_abono_fer")
  private Integer numDiasAbonoFer;

  @Column(name = "den_dias_abono_fer")
  private Integer denDiasAbonoFer;

  @Column(name = "cont_dobro_ferias", length = 1)
  private String contDobroFerias = "I";

  @Column(name = "min_dias_peraqfer")
  private Integer minDiasPeraqfer;

  @Column(name = "max_dias_peraqfer")
  private Integer maxDiasPeraqfer;

  @Column(name = "gerado_por_averbacao", nullable = false)
  private Boolean geradoPorAverbacao = false;

  @Column(name = "documento_legal_id")
  private Long documentoLegalId;

  @Column(name = "ato_publicacao_id")
  private Long atoPublicacaoId;

  @Column(name = "observacoes", length = 2000)
  private String observacoes;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "custom_fields")
  @org.praxisplatform.uischema.customfields.CustomFields(
      resourceKey = "administracao-pessoal.periodos-aquisitivos-ferias")
  private Map<String, Object> customFields = new LinkedHashMap<>();

  @Version
  @Column(name = "version", nullable = false)
  private Long version = 0L;

  @Embedded private AuditoriaErgon auditoria = new AuditoriaErgon();

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "emp_codigo",
      referencedColumnName = "empresa",
      insertable = false,
      updatable = false)
  private Orgao orgao;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "numfunc",
      referencedColumnName = "numero",
      insertable = false,
      updatable = false)
  private ServidorPessoa pessoa;

  @JsonIgnore
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

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "documento_legal_id",
      referencedColumnName = "id",
      insertable = false,
      updatable = false)
  private DocumentoLegal documentoLegal;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "ato_publicacao_id",
      referencedColumnName = "id",
      insertable = false,
      updatable = false)
  private DocumentoLegal atoPublicacao;
}
