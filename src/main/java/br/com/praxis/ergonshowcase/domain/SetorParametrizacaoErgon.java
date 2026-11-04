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

/** Parâmetros funcionais de setores: contrato estrutural da Onda 1; comandos permanecem bloqueados. */
@Entity @Table(name = "setores_ergon") @Getter @Setter
public class SetorParametrizacaoErgon {
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
  @Column(name = "cargo_chefe_id")
  private Integer cargoChefeId;
  @Column(name = "funcao_chefe_id")
  private Long funcaoChefeId;
  @Column(name = "tipo_setor")
  private String tipoSetor;
  @Column(name = "tipo_acesso")
  private String tipoAcesso;
  @Column(name = "tipo_classif_folha")
  private String tipoClassifFolha;
  @Column(name = "classif_folha")
  private String classifFolha;
  @Column(name = "centro_custo")
  private String centroCusto;
  @Column(name = "sindicato")
  private String sindicato;
  @Column(name = "cep")
  private String cep;
  @Column(name = "codigo_logradouro")
  private String codigoLogradouro;
  @Column(name = "perc_insalubridade")
  private BigDecimal percInsalubridade;
  @Column(name = "perc_periculosidade")
  private BigDecimal percPericulosidade;
  @Column(name = "grau_risco_ambiental")
  private String grauRiscoAmbiental;
  @Column(name = "agente_nocivo_principal")
  private String agenteNocivoPrincipal;
  @Column(name = "exige_epi_obrigatorio")
  private Boolean exigeEpiObrigatorio;
  @Column(name = "numero_processo")
  private String numeroProcesso;
  @Version @Column(name = "version")
  private Long version = 0L;
  @Formula("(select h.nome from historico_setores h where h.emp_codigo=emp_codigo and h.setor=setor and h.dtini<=dtini and (h.dtfim is null or h.dtfim>=dtini) order by h.dtini desc limit 1)")
  private String setorNome;
  @Formula("case when dtini > current_date then 'FUTURO' when dtfim < current_date then 'ENCERRADO' else 'VIGENTE' end")
  private String situacaoLookup;
}
