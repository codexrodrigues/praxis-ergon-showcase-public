package br.com.praxis.ergonshowcase.domain;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Formula;
import org.hibernate.type.SqlTypes;

/** Cargos e quadro por setor: contrato estrutural da Onda 1; comandos permanecem bloqueados. */
@Entity @Table(name = "cargos_setor") @Getter @Setter
public class CargoSetor {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(name = "emp_codigo")
  private Integer empCodigo;
  @Column(name = "dtini")
  private LocalDate dtini;
  @Column(name = "dtfim")
  private LocalDate dtfim;
  @Column(name = "documento_legal_id")
  private Long documentoLegalId;
  @Column(name = "observacao")
  private String observacao;
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "custom_fields")
  private Map<String, Object> customFields;
  @Column(name = "setor")
  private String setor;
  @Column(name = "cargo_id")
  private Integer cargoId;
  @Column(name = "abrange_subordinados")
  private Boolean abrangeSubordinados;
  @Column(name = "tipo_controle_quadro")
  private String tipoControleQuadro;
  @Column(name = "quadro_pai_id")
  private Long quadroPaiId;
  @Column(name = "lotacao_maxima_autorizada")
  private Integer lotacaoMaximaAutorizada;
  @Column(name = "lotacao_ocupada_atual")
  private Integer lotacaoOcupadaAtual;
  @Column(name = "perc_insalubridade")
  private BigDecimal percInsalubridade;
  @Column(name = "perc_periculosidade")
  private BigDecimal percPericulosidade;
  @Column(name = "exige_habilitacao_especifica")
  private Boolean exigeHabilitacaoEspecifica;
  @Version @Column(name = "version")
  private Long version = 0L;
  @Formula("(select h.nome from historico_setores h where h.emp_codigo=emp_codigo and h.setor=setor and h.dtini<=dtini and (h.dtfim is null or h.dtfim>=dtini) order by h.dtini desc limit 1)")
  private String setorNome;
  @Formula("(select c.nome from cargos c where c.emp_codigo=emp_codigo and c.cargo=cargo_id)")
  private String cargoNome;
  @Formula("case when dtini > current_date then 'FUTURO' when dtfim < current_date then 'ENCERRADO' else 'VIGENTE' end")
  private String situacaoLookup;
}
