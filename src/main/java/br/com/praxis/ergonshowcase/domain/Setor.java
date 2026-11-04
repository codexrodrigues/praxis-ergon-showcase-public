package br.com.praxis.ergonshowcase.domain;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Estrutura organizacional e unidades de lotação funcional/financeira por órgão público
 * (equivalente a SETORES_ / SETORES_ERGON / HSETOR no legado Ergon). Possui suporte a campos
 * customizados multi-órgão em JSONB indexado via GIN.
 */
@Getter
@Setter
@Entity
@Table(name = "setores")
@org.hibernate.annotations.Immutable
@org.hibernate.annotations.Subselect("select * from diretorio_setores_correntes")
@org.hibernate.annotations.Synchronize({"setores", "historico_setores", "setores_ergon"})
@Comment(
    "Estrutura organizacional e unidades de lotação funcional/financeira por órgão público (equivalente a SETORES_ / SETORES_ERGON / HSETOR no legado Ergon). Possui suporte a campos customizados multi-órgão em JSONB indexado via GIN.")
public class Setor {
  /** Vigência para novas seleções; referências históricas continuam reidratáveis por identidade. */
  @org.hibernate.annotations.Formula("case when dtini > current_date then 'FUTURO' when dtfim is not null and dtfim < current_date then 'ENCERRADO' when extinto then 'INATIVO' else 'VIGENTE' end")
  private String situacaoLookup;
  @Id
  /** Código alfanumérico estruturado identificador único do setor ou unidade de lotação. */
  @Comment("Código alfanumérico estruturado identificador único do setor ou unidade de lotação.")
  @Column(name = "setor", nullable = false, length = 30)
  private String setor;

  /** Código do órgão público ao qual pertence a unidade organizacional. */
  @Comment("Código do órgão público ao qual pertence a unidade organizacional.")
  @Column(name = "emp_codigo", nullable = false)
  private Integer empCodigo;

  @org.praxisplatform.uischema.annotation.OptionLabel
  @org.praxisplatform.uischema.service.base.annotation.DefaultSortColumn
  /** Denominação oficial completa da secretaria, diretoria, coordenadoria ou setor. */
  @Comment("Denominação oficial completa da secretaria, diretoria, coordenadoria ou setor.")
  @Column(name = "nome", nullable = false, length = 120)
  private String nome;

  /** Sigla administrativa abreviada do setor para exibição em organogramas e listagens. */
  @Comment("Sigla administrativa abreviada do setor para exibição em organogramas e listagens.")
  @Column(name = "sigla", nullable = true, length = 30)
  private String sigla;

  /** Código do setor hierarquicamente superior no organograma do órgão. */
  @Comment("Código do setor hierarquicamente superior no organograma do órgão.")
  @Column(name = "setor_pai", nullable = true, length = 30)
  private String setorPai;

  /**
   * Classificação funcional/financeira do setor (ex.: SECRETARIA, DIRETORIA, COORDENADORIA,
   * UNIDADE_OPERACIONAL).
   */
  @Comment(
      "Classificação funcional/financeira do setor (ex.: SECRETARIA, DIRETORIA, COORDENADORIA, UNIDADE_OPERACIONAL).")
  @Column(name = "tipo_setor", nullable = true, length = 20)
  private String tipoSetor;

  /** Data de início de vigência ou criação da unidade organizacional. */
  @Comment("Data de início de vigência ou criação da unidade organizacional.")
  @Column(name = "dtini", nullable = false)
  private LocalDate dtini;

  /** Data de extinção ou encerramento de vigência da unidade organizacional (NULL quando ativa). */
  @Comment(
      "Data de extinção ou encerramento de vigência da unidade organizacional (NULL quando ativa).")
  @Column(name = "dtfim", nullable = true)
  private LocalDate dtfim;

  /**
   * CNPJ específico da unidade de lotação tributária para apuração previdenciária e eSocial, quando
   * distinto do órgão.
   */
  @Comment(
      "CNPJ específico da unidade de lotação tributária para apuração previdenciária e eSocial, quando distinto do órgão.")
  @Column(name = "cgc_lotacao", nullable = true, length = 18)
  private String cgcLotacao;

  /** Município de localização física da unidade organizacional. */
  @Comment("Município de localização física da unidade organizacional.")
  @Column(name = "municipio_lotacao", nullable = true, length = 60)
  private String municipioLotacao;

  /** Sigla da UF de localização física da unidade organizacional. */
  @Comment("Sigla da UF de localização física da unidade organizacional.")
  @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.CHAR)
  @Column(name = "uf_lotacao", nullable = true, length = 2, columnDefinition = "character(2)")
  private String ufLotacao;

  /**
   * Referência relacional explícita ao ato legal de criação ou reestruturação do setor (substitui
   * PONTLEI/PONTPUBL/PONTPROC).
   */
  @Comment(
      "Referência relacional explícita ao ato legal de criação ou reestruturação do setor (substitui PONTLEI/PONTPUBL/PONTPROC).")
  @Column(name = "documento_legal_id", nullable = true)
  private Long documentoLegalId;

  /**
   * Atributos de negócio customizados por órgão em formato JSONB governado por
   * custom_field_definitions (substitui FLEX_CAMPO_01..40).
   */
  @Comment(
      "Atributos de negócio customizados por órgão em formato JSONB governado por custom_field_definitions (substitui FLEX_CAMPO_01..40).")
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "custom_fields", nullable = false)
  @org.praxisplatform.uischema.customfields.CustomFields(
      resourceKey = "administracao-pessoal.setores")
  private Map<String, Object> customFields = new LinkedHashMap<>();

  /** Versão do registro para controle de concorrência otimista (@Version / ETag / If-Match). */
  @Comment(
      "Versão do registro para controle de concorrência otimista (@Version / ETag / If-Match).")
  @Version
  @Column(name = "version", nullable = false)
  private Long version = 0L;

  /** Proveniência temporal do assentamento funcional ou cadastral. */
  @Embedded private AuditoriaErgon auditoria = new AuditoriaErgon();

  @Column(name="nome_longo")
  private String nomeLongo;

  @Column(name="data_criacao_original")
  private LocalDate dataCriacaoOriginal;

  @Column(name="quantidade_subordinados_diretos")
  private Integer quantidadeSubordinadosDiretos;

  @Column(name="centro_custo_padrao")
  private String centroCustoPadrao;

  @Column(name="cargo_chefe_id")
  private Integer cargoChefeId;

  @Column(name="funcao_chefe_id")
  private Long funcaoChefeId;

  @Column(name="observacao")
  private String observacao;

  @Column(name="setor_pai_hier1")
  private String setorPaiHier1;

  @Column(name="setor_pai_hier2")
  private String setorPaiHier2;

  @Column(name="setor_pai_hier3")
  private String setorPaiHier3;

  @Column(name="setor_pai_hier4")
  private String setorPaiHier4;

  @Column(name="caminho_hierarquico")
  private String caminhoHierarquico;

  @Column(name="nivel_hierarquico")
  private Integer nivelHierarquico;

  @Column(name="subemp_codigo")
  private String subempCodigo;

  @Column(name="localidade_codigo")
  private String localidadeCodigo;

  @Column(name="grupo_setor_id")
  private Long grupoSetorId;

  @Column(name="extinto")
  private Boolean extinto;

  @Column(name="data_extincao")
  private LocalDate dataExtincao;

  @Column(name="setor_substituto")
  private String setorSubstituto;

  @Column(name="tipo_logradouro")
  private String tipoLogradouro;

  @Column(name="logradouro")
  private String logradouro;

  @Column(name="numero_endereco")
  private String numeroEndereco;

  @Column(name="complemento_endereco")
  private String complementoEndereco;

  @Column(name="bairro")
  private String bairro;

  @Column(name="cep")
  private String cep;

  @Column(name="telefone")
  private String telefone;

  @Column(name="fax")
  private String fax;

  @Column(name="email_unidade")
  private String emailUnidade;
}
