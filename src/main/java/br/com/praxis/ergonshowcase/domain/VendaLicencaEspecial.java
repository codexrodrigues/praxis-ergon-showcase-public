package br.com.praxis.ergonshowcase.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** ERG_VENDA_LICESP: conversão de dias de licença especial em pecúnia. */
@Entity @Table(name = "vendas_licenca_especial") @Getter @Setter
public class VendaLicencaEspecial extends ParcelaLicencaEspecial {
  @Column(name = "licenca_especial_gozo_id") private Long licencaEspecialGozoId;
  @com.fasterxml.jackson.annotation.JsonIgnore @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "licenca_especial_gozo_id", insertable = false, updatable = false)
  private LicencaEspecialGozo licencaEspecialGozo;
  public VendaLicencaEspecial() { setSituacao("SOLICITADA"); }
  @Column(name = "dias_vendidos", nullable = false) private Integer diasVendidos;
  @Column(name = "dt_pagamento") private LocalDate dtPagamento;
  @Column(name = "dt_estorno") private LocalDate dtEstorno;
  @Column(name = "valor_indenizacao", precision = 15, scale = 2) private BigDecimal valorIndenizacao;
  @Column(nullable = false, length = 20) private String modalidade = "PECUNIA";
  @Column(name = "mes_ano_folha") private LocalDate mesAnoFolha;
  @Column(name = "num_folha") private Integer numFolha;
  @JdbcTypeCode(SqlTypes.JSON) @Column(name = "custom_fields")
  @org.praxisplatform.uischema.customfields.CustomFields(resourceKey = "administracao-pessoal.vendas-licenca-especial")
  private Map<String, Object> customFields = new LinkedHashMap<>();
}
