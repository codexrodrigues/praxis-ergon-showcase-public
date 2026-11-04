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

/** Históricos de setores: contrato estrutural da Onda 1; comandos permanecem bloqueados. */
@Entity @Table(name = "historico_setores") @Getter @Setter
public class HistoricoSetor {
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
  @org.praxisplatform.uischema.annotation.OptionLabel
  @Column(name = "nome")
  private String nome;
  @Column(name = "nome_longo")
  private String nomeLongo;
  @Column(name = "sigla")
  private String sigla;
  @Column(name = "tipo_setor")
  private String tipoSetor;
  @Column(name = "cgc_lotacao")
  private String cgcLotacao;
  @Column(name = "motivo_reestruturacao")
  private String motivoReestruturacao;
  @Column(name = "setor_pai")
  private String setorPai;
  @Column(name = "setor_pai_hier1")
  private String setorPaiHier1;
  @Column(name = "setor_pai_hier2")
  private String setorPaiHier2;
  @Column(name = "setor_pai_hier3")
  private String setorPaiHier3;
  @Column(name = "setor_pai_hier4")
  private String setorPaiHier4;
  @Column(name = "caminho_hierarquico")
  private String caminhoHierarquico;
  @Column(name = "nivel_hierarquico")
  private Integer nivelHierarquico;
  @Column(name = "subemp_codigo")
  private String subempCodigo;
  @Column(name = "localidade_codigo")
  private String localidadeCodigo;
  @Column(name = "grupo_setor_id")
  private Long grupoSetorId;
  @Column(name = "extinto")
  private Boolean extinto;
  @Column(name = "data_extincao")
  private LocalDate dataExtincao;
  @Column(name = "setor_substituto")
  private String setorSubstituto;
  @Column(name = "tipo_logradouro")
  private String tipoLogradouro;
  @Column(name = "logradouro")
  private String logradouro;
  @Column(name = "numero_endereco")
  private String numeroEndereco;
  @Column(name = "complemento_endereco")
  private String complementoEndereco;
  @Column(name = "bairro")
  private String bairro;
  @Column(name = "municipio_lotacao")
  private String municipioLotacao;
  @Column(name = "uf_lotacao")
  private String ufLotacao;
  @Column(name = "cep")
  private String cep;
  @Column(name = "telefone")
  private String telefone;
  @Column(name = "fax")
  private String fax;
  @Column(name = "email_unidade")
  private String emailUnidade;
  @Version @Column(name = "version")
  private Long version = 0L;
  @Formula("case when dtini > current_date then 'FUTURO' when dtfim < current_date then 'ENCERRADO' when extinto then 'INATIVO' else 'VIGENTE' end")
  private String situacaoLookup;
}
