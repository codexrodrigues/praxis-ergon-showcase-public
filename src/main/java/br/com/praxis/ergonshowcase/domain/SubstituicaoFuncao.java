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

/** Exercícios interinos vinculados à designação titular e ao impedimento, com vigências funcional e remuneratória independentes. */
@Entity
@Table(name = "substituicoes_funcao")
@Getter
@Setter
public class SubstituicaoFuncao {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "emp_codigo", nullable = false)
  private Integer empCodigo;

  @OptionLabel
  @Column(nullable = false, length = 200)
  private String descricao;

  @Column(name = "dtini", nullable = false)
  private LocalDate dtini;

  @Column(name = "dtfim", nullable = false)
  private LocalDate dtfim;

  @Column(name = "situacao", nullable = false, length = 30)
  private String situacao;

  @Column(name = "designacao_id", nullable = false)
  private Long designacaoId;

  @Embedded
  @AttributeOverrides({
      @AttributeOverride(name = "numfunc", column = @Column(name = "substituto_numfunc", nullable = false)),
      @AttributeOverride(name = "numero", column = @Column(name = "substituto_numvinc", nullable = false))})
  private VinculoId substitutoId;

  @Column(name = "motivo", nullable = false, length = 24)
  private String motivo;

  @Column(name = "afastamento_id")
  private Long afastamentoId;

  @Column(name = "ferias_gozo_id")
  private Long feriasGozoId;

  @Column(name = "dtini_remunerada")
  private LocalDate dtiniRemunerada;

  @Column(name = "dtfim_remunerada")
  private LocalDate dtfimRemunerada;

  @Column(name = "competencia_folha")
  private LocalDate competenciaFolha;

  @Column(name = "valor_substituicao", precision = 15, scale = 2)
  private BigDecimal valorSubstituicao;

  @Column(name = "documento_legal_id")
  private Long documentoLegalId;

  @Column(name = "ato_publicacao_id")
  private Long atoPublicacaoId;

  @Column(name = "observacoes", length = 2000)
  private String observacoes;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "custom_fields")
  @CustomFields(resourceKey = ApiPaths.KEY_SUBSTITUICOES)
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
  @JoinColumn(name = "designacao_id", referencedColumnName = "id", insertable = false, updatable = false)
  private DesignacaoFuncao designacao;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumns({
      @JoinColumn(name = "substituto_numfunc", referencedColumnName = "numfunc", insertable = false, updatable = false),
      @JoinColumn(name = "substituto_numvinc", referencedColumnName = "numero", insertable = false, updatable = false)})
  private Vinculo substituto;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "afastamento_id", referencedColumnName = "id", insertable = false, updatable = false)
  private Afastamento afastamento;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ferias_gozo_id", referencedColumnName = "id", insertable = false, updatable = false)
  private FeriasGozo feriasGozo;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "documento_legal_id", referencedColumnName = "id", insertable = false, updatable = false)
  private DocumentoLegal documentoLegal;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ato_publicacao_id", referencedColumnName = "id", insertable = false, updatable = false)
  private DocumentoLegal atoPublicacao;
}
