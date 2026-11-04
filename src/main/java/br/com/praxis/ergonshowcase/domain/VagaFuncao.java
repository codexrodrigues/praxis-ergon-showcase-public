package br.com.praxis.ergonshowcase.domain;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.praxisplatform.uischema.annotation.OptionLabel;
import org.praxisplatform.uischema.customfields.CustomFields;

/** Posições numeradas autorizadas por função, órgão e unidade, com origem e situação de ocupação. */
@Entity
@Table(name = "vagas_funcao")
@Getter
@Setter
public class VagaFuncao {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
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

  @Column(name = "numero_vaga", nullable = false)
  private Long numeroVaga;

  @Column(name = "funcao_id", nullable = false)
  private Long funcaoId;

  @Column(name = "setor_id", nullable = false, length = 30)
  private String setorId;

  @Column(name = "origem_vaga", nullable = false, length = 50)
  private String origemVaga;

  @Embedded
  @AttributeOverrides({
      @AttributeOverride(name = "numfunc", column = @Column(name = "titular_atual_numfunc", nullable = true)),
      @AttributeOverride(name = "numero", column = @Column(name = "titular_atual_numvinc", nullable = true))})
  private VinculoId titularAtualId;

  @Column(name = "motivo_bloqueio", length = 500)
  private String motivoBloqueio;

  @Column(name = "documento_legal_id")
  private Long documentoLegalId;

  @Column(name = "ato_publicacao_id")
  private Long atoPublicacaoId;

  @Column(name = "observacoes", length = 2000)
  private String observacoes;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "custom_fields")
  @CustomFields(resourceKey = ApiPaths.KEY_VAGAS_FUNCAO)
  private Map<String, Object> customFields;

  @Version
  private Long version;

  @Embedded
  private AuditoriaErgon auditoria = new AuditoriaErgon();

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
  @JoinColumns({
      @JoinColumn(name = "titular_atual_numfunc", referencedColumnName = "numfunc", insertable = false, updatable = false),
      @JoinColumn(name = "titular_atual_numvinc", referencedColumnName = "numero", insertable = false, updatable = false)})
  private Vinculo titularAtual;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "documento_legal_id", referencedColumnName = "id", insertable = false, updatable = false)
  private DocumentoLegal documentoLegal;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ato_publicacao_id", referencedColumnName = "id", insertable = false, updatable = false)
  private DocumentoLegal atoPublicacao;
}
