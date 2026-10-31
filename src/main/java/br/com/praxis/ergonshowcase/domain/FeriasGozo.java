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
 * Parcela de programação e gozo de férias vinculada ao período aquisitivo, com venda de abono
 * pecuniário, opções financeiras (terço constitucional, adiantamentos de 13º e salário, empréstimo),
 * competência de folha, substituição funcional e interrupção ({@code FERIAS} + {@code
 * ERG_ABONOS_FERIAS} / {@code PCK_FERIAS}).
 */
@Entity
@Table(name = "ferias_gozo")
@Comment(
    "Parcela de programação e gozo de férias e abono pecuniário vinculada ao período aquisitivo (equivalente a FERIAS + ERG_ABONOS_FERIAS no legado Ergon).")
@Getter
@Setter
@NoArgsConstructor
public class FeriasGozo {
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

  @Column(name = "periodo_aquisitivo_id", nullable = false)
  private Long periodoAquisitivoId;

  @org.praxisplatform.uischema.annotation.OptionLabel
  @Column(name = "chave_peraqfer", nullable = false)
  private Integer chavePeraqfer;

  @Column(name = "ordem_parcela", nullable = false)
  private Integer ordemParcela = 1;

  @org.praxisplatform.uischema.service.base.annotation.DefaultSortColumn
  @Column(name = "dtini", nullable = false)
  private LocalDate dtini;

  @Column(name = "dtprevfim", nullable = false)
  private LocalDate dtprevfim;

  @Column(name = "dtfim")
  private LocalDate dtfim;

  @Column(name = "dias_gozo", nullable = false)
  private Integer diasGozo;

  @Column(name = "tot_dias_direito_pa", nullable = false)
  private Integer totDiasDireitoPa = 30;

  @Column(name = "abono_pecuniario", nullable = false)
  private Boolean abonoPecuniario = false;

  @Column(name = "ini_fim_abono", length = 1)
  private String iniFimAbono;

  @Column(name = "dias_abono_vendidos", nullable = false)
  private Integer diasAbonoVendidos = 0;

  @Column(name = "dt_venda_abono")
  private LocalDate dtVendaAbono;

  @Column(name = "dt_pagto_abono")
  private LocalDate dtPagtoAbono;

  @Column(name = "abono_constitucional", nullable = false)
  private Boolean abonoConstitucional = true;

  @Column(name = "adiantamento_13", nullable = false)
  private Boolean adiantamento13 = false;

  @Column(name = "adiantamento_salario", nullable = false)
  private Boolean adiantamentoSalario = false;

  @Column(name = "emprestimo_ferias", nullable = false)
  private Boolean emprestimoFerias = false;

  @Column(name = "mes_ano_folha")
  private LocalDate mesAnoFolha;

  @Column(name = "num_folha")
  private Integer numFolha;

  @Column(name = "situacao", nullable = false, length = 30)
  private String situacao = "PROGRAMADA";

  @Column(name = "dt_interrupcao")
  private LocalDate dtInterrupcao;

  @Column(name = "motivo_interrupcao", length = 500)
  private String motivoInterrupcao;

  @Column(name = "numfunc_substituto")
  private Long numfuncSubstituto;

  @Column(name = "numvinc_substituto")
  private Integer numvincSubstituto;

  @Column(name = "documento_legal_id")
  private Long documentoLegalId;

  @Column(name = "ato_publicacao_id")
  private Long atoPublicacaoId;

  @Column(name = "observacoes", length = 2000)
  private String observacoes;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "custom_fields")
  @org.praxisplatform.uischema.customfields.CustomFields(
      resourceKey = "administracao-pessoal.ferias-gozo")
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
      name = "periodo_aquisitivo_id",
      referencedColumnName = "id",
      insertable = false,
      updatable = false)
  private PeriodoAquisitivoFerias periodoAquisitivo;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "numfunc_substituto",
      referencedColumnName = "numero",
      insertable = false,
      updatable = false)
  private ServidorPessoa pessoaSubstituta;

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
