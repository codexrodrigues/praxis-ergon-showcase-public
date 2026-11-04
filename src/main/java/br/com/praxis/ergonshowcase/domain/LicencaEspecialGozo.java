package br.com.praxis.ergonshowcase.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Parcela de LIC_ESP; datas de gozo e abono mantidas separadas. */
@Entity @Table(name = "licencas_especiais_gozo") @Getter @Setter
public class LicencaEspecialGozo extends ParcelaLicencaEspecial {
  @Column(name = "numfunc_substituto") private Long numfuncSubstituto;
  public LicencaEspecialGozo() { setSituacao("PROGRAMADA"); }
  @Column(name = "ordem_parcela", nullable = false) private Integer ordemParcela;
  @Column(name = "dtprevfim", nullable = false) private LocalDate dtprevfim;
  @Column(name = "dias_gozo", nullable = false) private Integer diasGozo;
  @Column(name = "abono_pecuniario", nullable = false) private Boolean abonoPecuniario = false;
  @Column(name = "dias_abono", nullable = false) private Integer diasAbono = 0;
  @Column(name = "ini_fim_abono", length = 1) private String iniFimAbono;
  @Column(name = "mes_ano_folha") private LocalDate mesAnoFolha;
  @Column(name = "num_folha") private Integer numFolha;
  @JdbcTypeCode(SqlTypes.JSON) @Column(name = "custom_fields")
  @org.praxisplatform.uischema.customfields.CustomFields(resourceKey = "administracao-pessoal.licencas-especiais-gozo")
  private Map<String, Object> customFields = new LinkedHashMap<>();
}
