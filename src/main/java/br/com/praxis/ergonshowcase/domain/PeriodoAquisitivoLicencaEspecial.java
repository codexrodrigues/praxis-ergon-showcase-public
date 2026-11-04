package br.com.praxis.ergonshowcase.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** PERAQLICESP: direito, saldo e parâmetros individuais prioritários ao parâmetro geral. */
@Entity @Table(name = "periodos_aquisitivos_licenca_especial") @Getter @Setter
public class PeriodoAquisitivoLicencaEspecial extends AssentamentoTempoLicenca {
  public PeriodoAquisitivoLicencaEspecial() { setSituacao("EM_AQUISICAO"); }
  @Column(nullable = false) private Integer chave;
  @Column(name = "dias_direito", nullable = false) private Integer diasDireito = 90;
  @Column(name = "dias_gozados", nullable = false) private Integer diasGozados = 0;
  @Column(name = "dias_vendidos", nullable = false) private Integer diasVendidos = 0;
  @Column(name = "dias_convertidos_dobro", nullable = false) private Integer diasConvertidosDobro = 0;
  @Column(name = "saldo_dias", nullable = false) private Integer saldoDias = 90;
  @Column(name = "dt_prescricao") private LocalDate dtPrescricao;
  @Column(name = "dt_cnt_dobro") private LocalDate dtCntDobro;
  @Column(name = "anos_lic_esp") private Integer anosLicEsp;
  @Column(name = "meses_lic_esp_per") private Integer mesesLicEspPer;
  @Column(name = "abono_dir_gozo_lic_esp", length = 1) private String abonoDirGozoLicEsp;
  @Column(name = "num_dias_abono_lic_esp") private Integer numDiasAbonoLicEsp;
  @Column(name = "den_dias_abono_lic_esp") private Integer denDiasAbonoLicEsp;
  @Column(name = "vend_lic_esp", length = 1) private String vendLicEsp = "I";
  @Column(name = "min_dias_pvend_lic_esp") private Integer minDiasPvendLicEsp;
  @Column(name = "max_dias_vend_lic_esp") private Integer maxDiasVendLicEsp;
  @Column(name = "cont_dobro_lic_esp", length = 1) private String contDobroLicEsp = "I";
  @Column(name = "min_dias_pcont_dob_lic_esp") private Integer minDiasPcontDobLicEsp;
  @Column(name = "max_dias_cont_dob_lic_esp") private Integer maxDiasContDobLicEsp;
  @Convert(converter = PoliticaAntecipacaoLicencaConverter.class)
  @Column(name = "perm_gozo_lic_esp_ant_fim_pe")
  private PoliticaAntecipacaoLicenca permGozoLicEspAntFimPe = PoliticaAntecipacaoLicenca.HERDAR;
  @Column(name = "averbacao_origem_id") private Long averbacaoOrigemId;
  @Column(name = "referencia_anexo", length = 255) private String referenciaAnexo;
  @JdbcTypeCode(SqlTypes.JSON) @Column(name = "custom_fields")
  @org.praxisplatform.uischema.customfields.CustomFields(resourceKey = "administracao-pessoal.periodos-aquisitivos-licenca-especial")
  private Map<String, Object> customFields = new LinkedHashMap<>();
  @JsonIgnore @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "averbacao_origem_id", insertable = false, updatable = false)
  private AverbacaoTempoServico averbacaoOrigem;
}
