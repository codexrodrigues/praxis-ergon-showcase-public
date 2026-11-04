package br.com.praxis.ergonshowcase.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

/** Identificação funcional e lastro documental comuns aos assentamentos do Slice 6. */
@MappedSuperclass
@Getter
@Setter
public abstract class AssentamentoTempoLicenca {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  @org.praxisplatform.uischema.service.base.annotation.DefaultSortColumn
  private Long id;
  @Column(name = "emp_codigo", nullable = false) private Integer empCodigo;
  @Column(nullable = false) private Long numfunc;
  @Column(nullable = false) private Integer numvinc;
  @org.praxisplatform.uischema.annotation.OptionLabel
  @Column(nullable = false, length = 200) private String descricao;
  @Column(nullable = false) private LocalDate dtini;
  private LocalDate dtfim;
  @Column(nullable = false, length = 30) private String situacao;
  @Column(name = "documento_legal_id") private Long documentoLegalId;
  @Column(name = "ato_publicacao_id") private Long atoPublicacaoId;
  @Column(length = 2000) private String observacoes;
  @Version private Long version = 0L;
  @Embedded private AuditoriaErgon auditoria = new AuditoriaErgon();
  @JsonIgnore @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "emp_codigo", referencedColumnName = "empresa", insertable = false, updatable = false)
  private Orgao orgao;
  @JsonIgnore @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "numfunc", referencedColumnName = "numero", insertable = false, updatable = false)
  private ServidorPessoa pessoa;
  @JsonIgnore @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumns({@JoinColumn(name = "numfunc", referencedColumnName = "numfunc", insertable = false, updatable = false),
      @JoinColumn(name = "numvinc", referencedColumnName = "numero", insertable = false, updatable = false)})
  private Vinculo vinculo;
  @JsonIgnore @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "documento_legal_id", insertable = false, updatable = false)
  private DocumentoLegal documentoLegal;
  @JsonIgnore @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ato_publicacao_id", insertable = false, updatable = false)
  private DocumentoLegal atoPublicacao;
}
