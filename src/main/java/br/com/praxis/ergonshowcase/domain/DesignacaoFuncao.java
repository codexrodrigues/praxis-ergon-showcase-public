package br.com.praxis.ergonshowcase.domain;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.praxisplatform.uischema.annotation.OptionLabel;
import org.praxisplatform.uischema.customfields.CustomFields;

/** Atos individuais de designação, exercício e dispensa em função de confiança, incluindo opção remuneratória e ordem de substitutos. */
@Entity
@Table(name = "designacoes_funcao")
@Getter
@Setter
public class DesignacaoFuncao {
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

  @Embedded
  @AttributeOverrides({
      @AttributeOverride(name = "numfunc", column = @Column(name = "titular_numfunc", nullable = false)),
      @AttributeOverride(name = "numero", column = @Column(name = "titular_numvinc", nullable = false))})
  private VinculoId titularId;

  @Column(name = "funcao_id", nullable = false)
  private Long funcaoId;

  @Column(name = "vaga_funcao_id")
  private Long vagaFuncaoId;

  @Column(name = "setor_id", nullable = false, length = 30)
  private String setorId;

  @Column(name = "dt_exercicio")
  private LocalDate dtExercicio;

  @Embedded
  @AttributeOverrides({
      @AttributeOverride(name = "numfunc", column = @Column(name = "substituto1_numfunc", nullable = true)),
      @AttributeOverride(name = "numero", column = @Column(name = "substituto1_numvinc", nullable = true))})
  private VinculoId substituto1Id;

  @Embedded
  @AttributeOverrides({
      @AttributeOverride(name = "numfunc", column = @Column(name = "substituto2_numfunc", nullable = true)),
      @AttributeOverride(name = "numero", column = @Column(name = "substituto2_numvinc", nullable = true))})
  private VinculoId substituto2Id;

  @Column(name = "opcao_remuneracao", nullable = false, length = 24)
  private String opcaoRemuneracao;

  @Column(name = "valor_remuneracao", precision = 15, scale = 2)
  private BigDecimal valorRemuneracao;

  @Column(name = "ato_dispensa_id")
  private Long atoDispensaId;

  @Column(name = "motivo_dispensa", length = 500)
  private String motivoDispensa;

  @Column(name = "documento_legal_id")
  private Long documentoLegalId;

  @Column(name = "ato_publicacao_id")
  private Long atoPublicacaoId;

  @Column(name = "observacoes", length = 2000)
  private String observacoes;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "custom_fields")
  @CustomFields(resourceKey = ApiPaths.KEY_DESIGNACOES)
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
  @JoinColumns({
      @JoinColumn(name = "titular_numfunc", referencedColumnName = "numfunc", insertable = false, updatable = false),
      @JoinColumn(name = "titular_numvinc", referencedColumnName = "numero", insertable = false, updatable = false)})
  private Vinculo titular;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "funcao_id", referencedColumnName = "id", insertable = false, updatable = false)
  private FuncaoConfianca funcao;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "vaga_funcao_id", referencedColumnName = "id", insertable = false, updatable = false)
  private VagaFuncao vagaFuncao;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "setor_id", referencedColumnName = "setor", insertable = false, updatable = false)
  private Setor setor;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumns({
      @JoinColumn(name = "substituto1_numfunc", referencedColumnName = "numfunc", insertable = false, updatable = false),
      @JoinColumn(name = "substituto1_numvinc", referencedColumnName = "numero", insertable = false, updatable = false)})
  private Vinculo substituto1;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumns({
      @JoinColumn(name = "substituto2_numfunc", referencedColumnName = "numfunc", insertable = false, updatable = false),
      @JoinColumn(name = "substituto2_numvinc", referencedColumnName = "numero", insertable = false, updatable = false)})
  private Vinculo substituto2;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ato_dispensa_id", referencedColumnName = "id", insertable = false, updatable = false)
  private DocumentoLegal atoDispensa;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "documento_legal_id", referencedColumnName = "id", insertable = false, updatable = false)
  private DocumentoLegal documentoLegal;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ato_publicacao_id", referencedColumnName = "id", insertable = false, updatable = false)
  private DocumentoLegal atoPublicacao;
}
