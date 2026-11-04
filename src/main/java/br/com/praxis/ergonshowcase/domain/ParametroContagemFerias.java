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
 * Matriz estatutária de parametrização de contagem de férias, abono pecuniário, prescrição,
 * contagem em dobro, licença especial e progressão funcional por órgão, regime jurídico, tipo de
 * vínculo, categoria, subcategoria e vigência ({@code PARAMETROS_CONTA} / {@code
 * PCK_PARAMETROS_CONTA}).
 */
@Entity
@Table(name = "parametros_contagem_ferias")
@Comment(
    "Matriz estatutária de parametrização de contagem de férias, abono pecuniário, prescrição, contagem em dobro, licença especial e progressão funcional (equivalente a PARAMETROS_CONTA no legado Ergon).")
@Getter
@Setter
@NoArgsConstructor
public class ParametroContagemFerias {
  @Column(name = "perm_gozo_lic_esp_ant_fim_pe", nullable = false)
  private Boolean permGozoLicEspAntFimPe = false;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Column(name = "emp_codigo")
  private Integer empCodigo;

  @org.praxisplatform.uischema.annotation.OptionLabel
  @Column(name = "regime_juridico", nullable = false, length = 30)
  private String regimeJuridico;

  @Column(name = "tipo_vinculo", nullable = false, length = 30)
  private String tipoVinculo;

  @Column(name = "categoria", nullable = false, length = 30)
  private String categoria;

  @Column(name = "subcategoria", length = 30)
  private String subcategoria;

  @org.praxisplatform.uischema.service.base.annotation.DefaultSortColumn
  @Column(name = "dtini", nullable = false)
  private LocalDate dtini;

  @Column(name = "dtfim")
  private LocalDate dtfim;

  @Column(name = "tem_ferias", nullable = false)
  private Boolean temFerias = true;

  @Column(name = "tot_dias_fer")
  private Integer totDiasFer = 30;

  @Column(name = "min_per_fer")
  private Integer minPerFer = 1;

  @Column(name = "max_per_fer")
  private Integer maxPerFer = 3;

  @Column(name = "min_dias_per_fer")
  private Integer minDiasPerFer = 10;

  @Column(name = "max_dias_per_fer")
  private Integer maxDiasPerFer = 30;

  @Column(name = "min_dias_per_princ_fer")
  private Integer minDiasPerPrincFer = 14;

  @Column(name = "abono_dir_gozo", length = 1)
  private String abonoDirGozo = "D";

  @Column(name = "num_dias_abono_fer")
  private Integer numDiasAbonoFer = 1;

  @Column(name = "den_dias_abono_fer")
  private Integer denDiasAbonoFer = 3;

  @Column(name = "min_dias_pvend_fer")
  private Integer minDiasPvendFer = 1;

  @Column(name = "max_dias_vend_fer")
  private Integer maxDiasVendFer = 10;

  @Column(name = "abono_const_fer", nullable = false)
  private Boolean abonoConstFer = true;

  @Column(name = "adiant_13_fer", nullable = false)
  private Boolean adiant13Fer = true;

  @Column(name = "adiant_sal_fer", nullable = false)
  private Boolean adiantSalFer = false;

  @Column(name = "emprest_fer", nullable = false)
  private Boolean emprestFer = false;

  @Column(name = "permite_gozo_antes_pa_fer", nullable = false)
  private Boolean permiteGozoAntesPaFer = false;

  @Column(name = "permite_fer_prim_per", nullable = false)
  private Boolean permiteFerPrimPer = false;

  @Column(name = "permite_fer_segun_per", nullable = false)
  private Boolean permiteFerSegunPer = false;

  @Column(name = "permite_fer_terc_per", nullable = false)
  private Boolean permiteFerTercPer = false;

  @Column(name = "permite_fer_per_inc", nullable = false)
  private Boolean permiteFerPerInc = false;

  @Column(name = "criterio_prescricao", length = 1)
  private String criterioPrescricao = "F";

  @Column(name = "permite_gozo_apos_prescricao", nullable = false)
  private Boolean permiteGozoAposPrescricao = false;

  @Column(name = "permite_lanc_conc_pa", nullable = false)
  private Boolean permiteLancConcPa = false;

  @Column(name = "exige_exercicio", nullable = false)
  private Boolean exigeExercicio = false;

  @Column(name = "sem_pgto_multa", nullable = false)
  private Boolean semPgtoMulta = false;

  @Column(name = "cont_dobro_ferias", nullable = false)
  private Boolean contDobroFerias = false;

  @Column(name = "min_dias_peraqfer")
  private Integer minDiasPeraqfer;

  @Column(name = "max_dias_peraqfer")
  private Integer maxDiasPeraqfer;

  @Column(name = "tem_lic_esp", nullable = false)
  private Boolean temLicEsp = false;

  @Column(name = "anos_lic_esp")
  private Integer anosLicEsp;

  @Column(name = "meses_lic_esp_per")
  private Integer mesesLicEspPer;

  @Column(name = "vend_lic_esp", nullable = false)
  private Boolean vendLicEsp = false;

  @Column(name = "abono_dir_gozo_lic_esp", length = 1)
  private String abonoDirGozoLicEsp;

  @Column(name = "min_dias_pvend_lic_esp")
  private Integer minDiasPvendLicEsp;

  @Column(name = "max_dias_vend_lic_esp")
  private Integer maxDiasVendLicEsp;

  @Column(name = "num_dias_abono_lic_esp")
  private Integer numDiasAbonoLicEsp;

  @Column(name = "den_dias_abono_lic_esp")
  private Integer denDiasAbonoLicEsp;

  @Column(name = "cont_dobro_lic_esp", nullable = false)
  private Boolean contDobroLicEsp = false;

  @Column(name = "min_dias_pcont_dob_lic_esp")
  private Integer minDiasPcontDobLicEsp;

  @Column(name = "max_dias_cont_dob_lic_esp")
  private Integer maxDiasContDobLicEsp;

  @Column(name = "tem_progressao", nullable = false)
  private Boolean temProgressao = false;

  @Column(name = "anos_progressao")
  private Integer anosProgressao;

  @Column(name = "tem_tempo_serv", nullable = false)
  private Boolean temTempoServ = false;

  @Column(name = "anos_1_tempo_serv")
  private Integer anos1TempoServ;

  @Column(name = "tem_sexta_parte", nullable = false)
  private Boolean temSextaParte = false;

  @Column(name = "anos_sexta_parte")
  private Integer anosSextaParte;

  @Column(name = "documento_legal_ferias_id")
  private Long documentoLegalFeriasId;

  @Column(name = "documento_legal_progressao_id")
  private Long documentoLegalProgressaoId;

  @Column(name = "observacoes", length = 2000)
  private String observacoes;

  @Column(name = "active", nullable = false)
  private Boolean active = true;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "custom_fields")
  @org.praxisplatform.uischema.customfields.CustomFields(
      resourceKey = "administracao-pessoal.parametros-contagem-ferias")
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
      name = "documento_legal_ferias_id",
      referencedColumnName = "id",
      insertable = false,
      updatable = false)
  private DocumentoLegal documentoLegalFerias;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "documento_legal_progressao_id",
      referencedColumnName = "id",
      insertable = false,
      updatable = false)
  private DocumentoLegal documentoLegalProgressao;
}
