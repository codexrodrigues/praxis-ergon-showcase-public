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
 * Dependentes legais e econômicos vinculados ao servidor público para fins de Imposto de Renda,
 * salário-família, pensão e benefícios assistenciais (equivalente a DEPENDENTES e HIST_DEPEN na
 * tela ERGadm00217 do legado Ergon). Preserva a chave composta natural (numfunc, numdep).
 */
@Getter
@Setter
@Entity
@Table(name = "dependentes")
@Comment(
    "Dependentes legais e econômicos vinculados ao servidor público para fins de Imposto de Renda, salário-família, pensão e benefícios assistenciais (equivalente a DEPENDENTES e HIST_DEPEN na tela ERGadm00217 do legado Ergon). Preserva a chave composta natural (numfunc, numdep).")
public class Dependente {
  /** Identidade natural composta, imutável após inclusão. */
  @EmbeddedId private DependenteId id;

  @org.praxisplatform.uischema.annotation.OptionLabel
  @org.praxisplatform.uischema.service.base.annotation.DefaultSortColumn
  /** Nome civil completo do dependente. */
  @Comment("Nome civil completo do dependente.")
  @Column(name = "nome", nullable = false, length = 300)
  private String nome;

  /** Nome social do dependente, quando aplicável. */
  @Comment("Nome social do dependente, quando aplicável.")
  @Column(name = "nome_social", nullable = true, length = 70)
  private String nomeSocial;

  /** Sexo do dependente ([F]eminino ou [M]asculino). */
  @Comment("Sexo do dependente ([F]eminino ou [M]asculino).")
  @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.CHAR)
  @Column(name = "sexo", nullable = true, length = 1, columnDefinition = "character(1)")
  private String sexo;

  /**
   * Data de nascimento do dependente, utilizada no controle etário de elegibilidade de benefícios.
   */
  @Comment(
      "Data de nascimento do dependente, utilizada no controle etário de elegibilidade de benefícios.")
  @Column(name = "dtnasc", nullable = false)
  private LocalDate dtnasc;

  /**
   * Grau de parentesco ou relação de dependência com o servidor (domínio Parentesco em itemtabelas,
   * ex.: FILHO, CONJUGE, ENTEADO, PAI/MAE).
   */
  @Comment(
      "Grau de parentesco ou relação de dependência com o servidor (domínio Parentesco em itemtabelas, ex.: FILHO, CONJUGE, ENTEADO, PAI/MAE).")
  @Column(name = "parentesco", nullable = false, length = 20)
  private String parentesco;

  /** Classificação funcional do tipo de dependência (referência a tipos_dependente). */
  @Comment("Classificação funcional do tipo de dependência (referência a tipos_dependente).")
  @Column(name = "tipodepen", nullable = true, length = 20)
  private String tipodepen;

  /** CPF do dependente (obrigatório para declaração de IRRF e eSocial). */
  @Comment("CPF do dependente (obrigatório para declaração de IRRF e eSocial).")
  @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.CHAR)
  @Column(name = "cpf", nullable = true, length = 11, columnDefinition = "character(11)")
  private String cpf;

  /** Número do documento de identidade (RG) do dependente. */
  @Comment("Número do documento de identidade (RG) do dependente.")
  @Column(name = "numrg", nullable = true, length = 24)
  private String numrg;

  /** Tipo de documento de identidade do dependente. */
  @Comment("Tipo de documento de identidade do dependente.")
  @Column(name = "tiporg", nullable = true, length = 20)
  private String tiporg;

  /** Órgão emissor do RG do dependente. */
  @Comment("Órgão emissor do RG do dependente.")
  @Column(name = "orgaorg", nullable = true, length = 20)
  private String orgaorg;

  /** Sigla da UF emissora do RG do dependente. */
  @Comment("Sigla da UF emissora do RG do dependente.")
  @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.CHAR)
  @Column(name = "ufrg", nullable = true, length = 2, columnDefinition = "character(2)")
  private String ufrg;

  /** Data de expedição do RG do dependente. */
  @Comment("Data de expedição do RG do dependente.")
  @Column(name = "expedrg", nullable = true)
  private LocalDate expedrg;

  /** Data de início da vigência da condição de dependência. */
  @Comment("Data de início da vigência da condição de dependência.")
  @Column(name = "dtini", nullable = false)
  private LocalDate dtini;

  /**
   * Data de encerramento da condição de dependência (por maioridade, emancipação, óbito ou
   * divórcio).
   */
  @Comment(
      "Data de encerramento da condição de dependência (por maioridade, emancipação, óbito ou divórcio).")
  @Column(name = "dtfim", nullable = true)
  private LocalDate dtfim;

  /**
   * Indica se o dependente é elegível e ativo para dedução de Imposto de Renda Retido na Fonte
   * (IRRF).
   */
  @Comment(
      "Indica se o dependente é elegível e ativo para dedução de Imposto de Renda Retido na Fonte (IRRF).")
  @Column(name = "dep_ir", nullable = false)
  private Boolean depIr = true;

  /** Indica se o dependente gera direito à cota de salário-família. */
  @Comment("Indica se o dependente gera direito à cota de salário-família.")
  @Column(name = "dep_salario_familia", nullable = false)
  private Boolean depSalarioFamilia = false;

  /**
   * Indica se o dependente é beneficiário de assistência à saúde suplementar vinculada ao órgão.
   */
  @Comment(
      "Indica se o dependente é beneficiário de assistência à saúde suplementar vinculada ao órgão.")
  @Column(name = "dep_plano_saude", nullable = false)
  private Boolean depPlanoSaude = false;

  /** Indica se o dependente possui invalidez permanente atestada por junta médica oficial. */
  @Comment("Indica se o dependente possui invalidez permanente atestada por junta médica oficial.")
  @Column(name = "invalido", nullable = false)
  private Boolean invalido = false;

  /** Indica se o dependente cursa ensino regular para fins de extensão de elegibilidade. */
  @Comment("Indica se o dependente cursa ensino regular para fins de extensão de elegibilidade.")
  @Column(name = "estudante", nullable = false)
  private Boolean estudante = false;

  /**
   * Indica se o dependente cursa ensino superior ou técnico (extensão de dependência até 24 anos).
   */
  @Comment(
      "Indica se o dependente cursa ensino superior ou técnico (extensão de dependência até 24 anos).")
  @Column(name = "universitario", nullable = false)
  private Boolean universitario = false;

  /** Indica condição excepcional para fins de benefício assistencial estatutário. */
  @Comment("Indica condição excepcional para fins de benefício assistencial estatutário.")
  @Column(name = "excepcional", nullable = false)
  private Boolean excepcional = false;

  /** Indica se o registro atua como ou exige representante legal/tutor. */
  @Comment("Indica se o registro atua como ou exige representante legal/tutor.")
  @Column(name = "representante_legal", nullable = false)
  private Boolean representanteLegal = false;

  /** Tipo de certidão civil apresentada (ex.: NASCIMENTO, CASAMENTO, GUARDA JUDICIAL). */
  @Comment("Tipo de certidão civil apresentada (ex.: NASCIMENTO, CASAMENTO, GUARDA JUDICIAL).")
  @Column(name = "tipodoc_cert", nullable = true, length = 30)
  private String tipodocCert;

  /** Número do termo da certidão civil do dependente. */
  @Comment("Número do termo da certidão civil do dependente.")
  @Column(name = "num_cert", nullable = true, length = 32)
  private String numCert;

  /** Número ou código do livro de registro da certidão civil. */
  @Comment("Número ou código do livro de registro da certidão civil.")
  @Column(name = "livro_a_cert", nullable = true, length = 15)
  private String livroACert;

  /** Número da folha do livro da certidão civil. */
  @Comment("Número da folha do livro da certidão civil.")
  @Column(name = "folha_cert", nullable = true, length = 15)
  private String folhaCert;

  /** Denominação do cartório de registro civil emissor da certidão. */
  @Comment("Denominação do cartório de registro civil emissor da certidão.")
  @Column(name = "cartorio_cert", nullable = true, length = 100)
  private String cartorioCert;

  /** Sigla da UF do cartório de registro civil. */
  @Comment("Sigla da UF do cartório de registro civil.")
  @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.CHAR)
  @Column(name = "uf_cart", nullable = true, length = 2, columnDefinition = "character(2)")
  private String ufCart;

  /** Município de localização do cartório de registro civil. */
  @Comment("Município de localização do cartório de registro civil.")
  @Column(name = "municipio_cart", nullable = true, length = 60)
  private String municipioCart;

  /** Matrícula nacional unificada de 32 dígitos da certidão de registro civil. */
  @Comment("Matrícula nacional unificada de 32 dígitos da certidão de registro civil.")
  @Column(name = "matricula_cert", nullable = true, length = 40)
  private String matriculaCert;

  /**
   * Referência relacional explícita ao documento comprobatório ou publicação associada ao
   * dependente (substitui PONTPUBL/PONTARQS).
   */
  @Comment(
      "Referência relacional explícita ao documento comprobatório ou publicação associada ao dependente (substitui PONTPUBL/PONTARQS).")
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
      resourceKey = "administracao-pessoal.dependentes")
  private Map<String, Object> customFields = new LinkedHashMap<>();

  /** Versão do registro para controle de concorrência otimista (@Version / ETag / If-Match). */
  @Comment(
      "Versão do registro para controle de concorrência otimista (@Version / ETag / If-Match).")
  @Version
  @Column(name = "version", nullable = false)
  private Long version = 0L;

  /** Proveniência temporal do assentamento funcional ou cadastral. */
  @Embedded private AuditoriaErgon auditoria = new AuditoriaErgon();
}
