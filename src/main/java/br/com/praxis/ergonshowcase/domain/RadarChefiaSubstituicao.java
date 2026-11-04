package br.com.praxis.ergonshowcase.domain;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Immutable;
import org.hibernate.type.SqlTypes;
import org.praxisplatform.uischema.annotation.OptionLabel;
import org.praxisplatform.uischema.customfields.CustomFields;

/** Posição de cobertura da chefia na data de referência. Distingue titular em exercício, impedimento coberto e chefia descoberta. */
@Entity
@Table(name = "radar_chefias_substituicoes")
@Immutable
@Getter
@Setter
public class RadarChefiaSubstituicao {
  @Id
  private Long id;

  @Column(name = "emp_codigo", nullable = false)
  private Integer empCodigo;

  @OptionLabel
  @Column(nullable = false, length = 200)
  private String descricao;

  @Column(name = "dtini", nullable = false)
  private LocalDate dtini;

  @Column(name = "dtfim")
  private LocalDate dtfim;

  @Column(name = "situacao", nullable = false, length = 30)
  private String situacao;

  @Column(name = "funcao_id", nullable = false)
  private Long funcaoId;

  @Column(name = "setor_id", nullable = false, length = 30)
  private String setorId;

  @Column(name = "designacao_id", nullable = false)
  private Long designacaoId;

  @Column(name = "vaga_funcao_id")
  private Long vagaFuncaoId;

  @Embedded
  @AttributeOverrides({
      @AttributeOverride(name = "numfunc", column = @Column(name = "titular_numfunc", nullable = false)),
      @AttributeOverride(name = "numero", column = @Column(name = "titular_numvinc", nullable = false))})
  private VinculoId titularId;

  @Column(name = "substituicao_id")
  private Long substituicaoId;

  @Column(name = "afastamento_id")
  private Long afastamentoId;

  @Column(name = "ferias_gozo_id")
  private Long feriasGozoId;

  @Column(name = "data_referencia", nullable = false)
  private LocalDate dataReferencia;

  @Column(name = "qtd_coberturas", nullable = false)
  private Integer qtdCoberturas;

  @Column(name = "pendencias_folha", nullable = false)
  private Integer pendenciasFolha;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "custom_fields")
  @CustomFields(resourceKey = ApiPaths.KEY_DESIGNACOES)
  private Map<String, Object> customFields;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "emp_codigo", referencedColumnName = "empresa", insertable = false, updatable = false)
  private Orgao orgao;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "funcao_id", referencedColumnName = "id", insertable = false, updatable = false)
  private FuncaoConfianca funcao;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "setor_id", referencedColumnName = "setor", insertable = false, updatable = false)
  private Setor setor;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "designacao_id", referencedColumnName = "id", insertable = false, updatable = false)
  private DesignacaoFuncao designacao;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "vaga_funcao_id", referencedColumnName = "id", insertable = false, updatable = false)
  private VagaFuncao vagaFuncao;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumns({
      @JoinColumn(name = "titular_numfunc", referencedColumnName = "numfunc", insertable = false, updatable = false),
      @JoinColumn(name = "titular_numvinc", referencedColumnName = "numero", insertable = false, updatable = false)})
  private Vinculo titular;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "substituicao_id", referencedColumnName = "id", insertable = false, updatable = false)
  private SubstituicaoFuncao substituicao;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "afastamento_id", referencedColumnName = "id", insertable = false, updatable = false)
  private Afastamento afastamento;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ferias_gozo_id", referencedColumnName = "id", insertable = false, updatable = false)
  private FeriasGozo feriasGozo;
}
