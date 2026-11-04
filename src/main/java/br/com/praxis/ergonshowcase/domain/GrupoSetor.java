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

/** Grupos de setores: contrato estrutural da Onda 1; comandos permanecem bloqueados. */
@Entity @Table(name = "grupos_setores") @Getter @Setter
public class GrupoSetor {
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
  @Column(name = "sigla")
  private String sigla;
  @org.praxisplatform.uischema.annotation.OptionLabel
  @Column(name = "nome")
  private String nome;
  @Column(name = "tipo_agrupamento")
  private String tipoAgrupamento;
  @Column(name = "setor_sede")
  private String setorSede;
  @Column(name = "subemp_codigo_vinculada")
  private String subempCodigoVinculada;
  @Column(name = "uf_abrangencia")
  private String ufAbrangencia;
  @Column(name = "municipio_sede")
  private String municipioSede;
  @Column(name = "ativo")
  private Boolean ativo;
  @Version @Column(name = "version")
  private Long version = 0L;
  @Formula("case when dtini > current_date then 'FUTURO' when dtfim < current_date then 'ENCERRADO' when not ativo then 'INATIVO' else 'VIGENTE' end")
  private String situacaoLookup;
}
