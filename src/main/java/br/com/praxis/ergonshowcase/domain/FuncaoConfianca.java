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

/** Catálogo por órgão de funções de direção, chefia e assessoramento, com vigência, gratificação e política de vagas e substituição. */
@Entity
@Table(name = "funcoes_confianca")
@Getter
@Setter
public class FuncaoConfianca {
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

  @Column(name = "codigo", nullable = false, length = 24)
  private String codigo;

  @Column(name = "categoria", nullable = false, length = 24)
  private String categoria;

  @Column(name = "simbolo", nullable = false, length = 20)
  private String simbolo;

  @Column(name = "controle_vaga", nullable = false)
  private Boolean controleVaga;

  @Column(name = "dt_inicio_contr_vaga")
  private LocalDate dtInicioContrVaga;

  @Column(name = "permite_substituicao", nullable = false)
  private Boolean permiteSubstituicao;

  @Column(name = "tipo_substituicao", length = 50)
  private String tipoSubstituicao;

  @Column(name = "dias_minimos_substituicao")
  private Integer diasMinimosSubstituicao;

  @Column(name = "valor_gratificacao", nullable = false, precision = 15, scale = 2)
  private BigDecimal valorGratificacao;

  @Column(name = "percentual_opcao_efetivo", precision = 5, scale = 2)
  private BigDecimal percentualOpcaoEfetivo;

  @Column(name = "exige_vinculo_efetivo", nullable = false)
  private Boolean exigeVinculoEfetivo;

  @Column(name = "requisitos", length = 2000)
  private String requisitos;

  @Column(name = "documento_legal_id")
  private Long documentoLegalId;

  @Column(name = "ato_publicacao_id")
  private Long atoPublicacaoId;

  @Column(name = "observacoes", length = 2000)
  private String observacoes;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "custom_fields")
  @CustomFields(resourceKey = ApiPaths.KEY_FUNCOES)
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
  @JoinColumn(name = "documento_legal_id", referencedColumnName = "id", insertable = false, updatable = false)
  private DocumentoLegal documentoLegal;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ato_publicacao_id", referencedColumnName = "id", insertable = false, updatable = false)
  private DocumentoLegal atoPublicacao;
}
