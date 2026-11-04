package br.com.praxis.ergonshowcase.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** AVERBACOES_CONTA + finalidade de AVERB_OQUE_CONTA e desaverbação parcial consolidada. */
@Entity @Table(name = "averbacoes_tempo_servico") @Getter @Setter
public class AverbacaoTempoServico extends AssentamentoTempoLicenca {
  public AverbacaoTempoServico() { setSituacao("EM_ANALISE"); }
  @Column(nullable = false) private Integer chave;
  @Column(length = 200) private String instituicao;
  @Column(name = "tipo_tempo", nullable = false, length = 30) private String tipoTempo;
  @Column(name = "regime_previdenciario", length = 20) private String regimePrevidenciario;
  @Column(nullable = false, length = 30) private String finalidade;
  @Column(length = 20) private String complemento;
  @Column(name = "data_a_contar") private LocalDate dataAContar;
  @Column(name = "total_dias", nullable = false) private Integer totalDias;
  @Column(name = "dias_finalidade", nullable = false) private Integer diasFinalidade;
  @Column(nullable = false) private Boolean sobrepoe = false;
  @Column(length = 2000) private String motivo;
  @Column(name = "data_remocao") private LocalDate dataRemocao;
  @Column(name = "motivo_remocao", length = 2000) private String motivoRemocao;
  @Column(name = "dias_desaverbados", nullable = false) private Integer diasDesaverbados = 0;
  @Column(name = "data_desaverbacao") private LocalDate dataDesaverbacao;
  @Column(name = "motivo_desaverbacao", length = 2000) private String motivoDesaverbacao;
  @Column(name = "info1", length = 2000) private String info1;
  @Column(name = "info2", length = 2000) private String info2;
  @Column(name = "referencia_anexo", length = 255) private String referenciaAnexo;
  @JdbcTypeCode(SqlTypes.JSON) @Column(name = "custom_fields")
  @org.praxisplatform.uischema.customfields.CustomFields(resourceKey = "administracao-pessoal.averbacoes-tempo-servico")
  private Map<String, Object> customFields = new LinkedHashMap<>();
}
