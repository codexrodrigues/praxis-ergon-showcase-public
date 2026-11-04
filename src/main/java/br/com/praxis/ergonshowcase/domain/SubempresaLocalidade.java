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

/** Subempresas e localidades: contrato estrutural da Onda 1; comandos permanecem bloqueados. */
@Entity @Table(name = "subempresas_localidades") @Getter @Setter
public class SubempresaLocalidade {
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
  @Column(name = "codigo")
  private String codigo;
  @Column(name = "natureza_unidade")
  private String naturezaUnidade;
  @org.praxisplatform.uischema.annotation.OptionLabel
  @Column(name = "nome")
  private String nome;
  @Column(name = "nome_fantasia")
  private String nomeFantasia;
  @Column(name = "razao_social")
  private String razaoSocial;
  @Column(name = "cnpj")
  private String cnpj;
  @Column(name = "cnpj_declarante_esocial")
  private String cnpjDeclaranteEsocial;
  @Column(name = "cnae_preponderante")
  private String cnaePreponderante;
  @Column(name = "natureza_juridica")
  private String naturezaJuridica;
  @Column(name = "subempresa_pai_codigo")
  private String subempresaPaiCodigo;
  @Column(name = "setor_sede")
  private String setorSede;
  @Column(name = "tipo_localidade")
  private String tipoLocalidade;
  @Column(name = "regiao_geografica")
  private String regiaoGeografica;
  @Column(name = "entrancia_comarca")
  private String entranciaComarca;
  @Column(name = "uf")
  private String uf;
  @Column(name = "municipio")
  private String municipio;
  @Column(name = "cep")
  private String cep;
  @Column(name = "logradouro")
  private String logradouro;
  @Column(name = "ativo")
  private Boolean ativo;
  @Version @Column(name = "version")
  private Long version = 0L;
  @Formula("case when dtini > current_date then 'FUTURO' when dtfim < current_date then 'ENCERRADO' when not ativo then 'INATIVO' else 'VIGENTE' end")
  private String situacaoLookup;
}
