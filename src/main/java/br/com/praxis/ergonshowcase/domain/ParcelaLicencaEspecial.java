package br.com.praxis.ergonshowcase.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Chaves espelhadas do período, preenchidas pelo serviço antes de persistir a parcela. */
@MappedSuperclass @Getter @Setter
public abstract class ParcelaLicencaEspecial extends AssentamentoTempoLicenca {
  @Column(name = "periodo_aquisitivo_licenca_id", nullable = false)
  private Long periodoAquisitivoLicencaId;
  @Column(name = "chave_peraqlicesp", nullable = false) private Integer chavePeraqlicesp;
  @JsonIgnore @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "periodo_aquisitivo_licenca_id", insertable = false, updatable = false)
  private PeriodoAquisitivoLicencaEspecial periodoAquisitivoLicenca;
}
