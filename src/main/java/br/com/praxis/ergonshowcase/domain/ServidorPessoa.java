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
 * Cadastro civil, documental, de endereço, bancário e de acessibilidade dos servidores públicos
 * (equivalente a FUNCIONARIOS na tela ERGadm00104 do legado Ergon). Entidade raiz de pessoa
 * funcional com suporte a custom_fields JSONB.
 */
@Getter
@Setter
@Entity
@Table(name = "funcionarios")
@Comment(
    "Cadastro civil, documental, de endereço, bancário e de acessibilidade dos servidores públicos (equivalente a FUNCIONARIOS na tela ERGadm00104 do legado Ergon). Entidade raiz de pessoa funcional com suporte a custom_fields JSONB.")
public class ServidorPessoa {
  @Id
  /** Número funcional único identificador da pessoa/servidor no Ergon (NUMERO / NUMFUNC). */
  @Comment("Número funcional único identificador da pessoa/servidor no Ergon (NUMERO / NUMFUNC).")
  @Column(name = "numero", nullable = false)
  private Long numero;

  @org.praxisplatform.uischema.annotation.OptionLabel
  @org.praxisplatform.uischema.service.base.annotation.DefaultSortColumn
  /** Nome civil completo do servidor público. */
  @Comment("Nome civil completo do servidor público.")
  @Column(name = "nome", nullable = false, length = 300)
  private String nome;

  /** Nome social adotado pelo servidor público conforme legislação de identidade de gênero. */
  @Comment("Nome social adotado pelo servidor público conforme legislação de identidade de gênero.")
  @Column(name = "nome_social", nullable = true, length = 70)
  private String nomeSocial;

  /** Pronome ou forma de tratamento protocolar do servidor (ex.: Sr., Sra., Dr., Prof.). */
  @Comment("Pronome ou forma de tratamento protocolar do servidor (ex.: Sr., Sra., Dr., Prof.).")
  @Column(name = "tratamento", nullable = true, length = 20)
  private String tratamento;

  /** Sexo biológico registral do servidor ([F]eminino ou [M]asculino). */
  @Comment("Sexo biológico registral do servidor ([F]eminino ou [M]asculino).")
  @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.CHAR)
  @Column(name = "sexo", nullable = true, length = 1, columnDefinition = "character(1)")
  private String sexo;

  /** Identidade de gênero autodeclarada pelo servidor público. */
  @Comment("Identidade de gênero autodeclarada pelo servidor público.")
  @Column(name = "genero", nullable = true, length = 100)
  private String genero;

  /** Data de nascimento do servidor público. */
  @Comment("Data de nascimento do servidor público.")
  @Column(name = "dtnasc", nullable = false)
  private LocalDate dtnasc;

  /** Município de naturalidade (nascimento) do servidor. */
  @Comment("Município de naturalidade (nascimento) do servidor.")
  @Column(name = "cidnasc", nullable = true, length = 60)
  private String cidnasc;

  /** Sigla da Unidade da Federação de naturalidade do servidor. */
  @Comment("Sigla da Unidade da Federação de naturalidade do servidor.")
  @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.CHAR)
  @Column(name = "ufnasc", nullable = true, length = 2, columnDefinition = "character(2)")
  private String ufnasc;

  /**
   * Grupo sanguíneo e fator Rh do servidor (domínio ERG_G_SANG em itemtabelas, ex.: O+, A-, AB+).
   */
  @Comment(
      "Grupo sanguíneo e fator Rh do servidor (domínio ERG_G_SANG em itemtabelas, ex.: O+, A-, AB+).")
  @Column(name = "g_sanguineo", nullable = true, length = 3)
  private String gSanguineo;

  /**
   * Classificação de raça/cor autodeclarada conforme padrão IBGE/eSocial (domínio ERG_RACA em
   * itemtabelas).
   */
  @Comment(
      "Classificação de raça/cor autodeclarada conforme padrão IBGE/eSocial (domínio ERG_RACA em itemtabelas).")
  @Column(name = "raca_cor", nullable = true, length = 20)
  private String racaCor;

  /** Nome completo do pai na filiação civil do servidor. */
  @Comment("Nome completo do pai na filiação civil do servidor.")
  @Column(name = "pai", nullable = true, length = 300)
  private String pai;

  /** Nome completo da mãe na filiação civil do servidor. */
  @Comment("Nome completo da mãe na filiação civil do servidor.")
  @Column(name = "mae", nullable = true, length = 300)
  private String mae;

  /** Estado civil atual do servidor (domínio ERG_ESTCIVIL em itemtabelas). */
  @Comment("Estado civil atual do servidor (domínio ERG_ESTCIVIL em itemtabelas).")
  @Column(name = "estcivil", nullable = true, length = 20)
  private String estcivil;

  /** Indica se o servidor possui união estável formalmente declarada. */
  @Comment("Indica se o servidor possui união estável formalmente declarada.")
  @Column(name = "uniao_estavel", nullable = false)
  private Boolean uniaoEstavel = false;

  /** Grau de instrução formal do servidor (domínio ERG_ESCOLA em itemtabelas). */
  @Comment("Grau de instrução formal do servidor (domínio ERG_ESCOLA em itemtabelas).")
  @Column(name = "escolaridade", nullable = true, length = 20)
  private String escolaridade;

  /**
   * Código de nacionalidade do servidor (domínio ERG_NACIONALIDADE em itemtabelas; 10 =
   * Brasileira).
   */
  @Comment(
      "Código de nacionalidade do servidor (domínio ERG_NACIONALIDADE em itemtabelas; 10 = Brasileira).")
  @Column(name = "nacionalidade", nullable = true)
  private Integer nacionalidade;

  /** Ano de chegada ao Brasil para servidores estrangeiros ou naturalizados. */
  @Comment("Ano de chegada ao Brasil para servidores estrangeiros ou naturalizados.")
  @Column(name = "chegbrasil", nullable = true)
  private Integer chegbrasil;

  /** UF do primeiro emprego ou vínculo anterior para fins de histórico trabalhista/RAIS. */
  @Comment("UF do primeiro emprego ou vínculo anterior para fins de histórico trabalhista/RAIS.")
  @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.CHAR)
  @Column(name = "ufempant", nullable = true, length = 2, columnDefinition = "character(2)")
  private String ufempant;

  /** Ano do primeiro emprego do servidor para fins estatísticos e previdenciários. */
  @Comment("Ano do primeiro emprego do servidor para fins estatísticos e previdenciários.")
  @Column(name = "anoprimemp", nullable = true)
  private Integer anoprimemp;

  /** Cadastro de Pessoas Físicas (CPF) do servidor com 11 dígitos numéricos sem formatação. */
  @Comment("Cadastro de Pessoas Físicas (CPF) do servidor com 11 dígitos numéricos sem formatação.")
  @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.CHAR)
  @Column(name = "cpf", nullable = false, length = 11, columnDefinition = "character(11)")
  private String cpf;

  /** Número da Cédula de Identidade Civil (RG) do servidor. */
  @Comment("Número da Cédula de Identidade Civil (RG) do servidor.")
  @Column(name = "numrg", nullable = true, length = 24)
  private String numrg;

  /** Tipo do documento de identidade apresentado (domínio TIPO_RG em itemtabelas). */
  @Comment("Tipo do documento de identidade apresentado (domínio TIPO_RG em itemtabelas).")
  @Column(name = "tiporg", nullable = true, length = 20)
  private String tiporg;

  /**
   * Órgão expedidor da cédula de identidade (domínio ORGAO RG em itemtabelas, ex.: SSP, DETRAN,
   * IFP).
   */
  @Comment(
      "Órgão expedidor da cédula de identidade (domínio ORGAO RG em itemtabelas, ex.: SSP, DETRAN, IFP).")
  @Column(name = "orgaorg", nullable = true, length = 20)
  private String orgaorg;

  /** Sigla da UF do órgão expedidor do RG. */
  @Comment("Sigla da UF do órgão expedidor do RG.")
  @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.CHAR)
  @Column(name = "ufrg", nullable = true, length = 2, columnDefinition = "character(2)")
  private String ufrg;

  /** Data de expedição da cédula de identidade. */
  @Comment("Data de expedição da cédula de identidade.")
  @Column(name = "expedrg", nullable = true)
  private LocalDate expedrg;

  /** Número de inscrição no PIS/PASEP ou NIT do servidor público. */
  @Comment("Número de inscrição no PIS/PASEP ou NIT do servidor público.")
  @Column(name = "pispasep", nullable = true, length = 11)
  private String pispasep;

  /** Data de cadastramento original no programa PIS/PASEP. */
  @Comment("Data de cadastramento original no programa PIS/PASEP.")
  @Column(name = "datapis", nullable = true)
  private LocalDate datapis;

  /**
   * Código compensação do banco depositário/administrador do PIS/PASEP (ex.: 001 Banco do Brasil,
   * 104 CEF).
   */
  @Comment(
      "Código compensação do banco depositário/administrador do PIS/PASEP (ex.: 001 Banco do Brasil, 104 CEF).")
  @Column(name = "bancopis", nullable = true)
  private Integer bancopis;

  /** Número da Carteira de Trabalho e Previdência Social (CTPS). */
  @Comment("Número da Carteira de Trabalho e Previdência Social (CTPS).")
  @Column(name = "numcartpro", nullable = true, length = 12)
  private String numcartpro;

  /** Série da Carteira de Trabalho e Previdência Social (CTPS). */
  @Comment("Série da Carteira de Trabalho e Previdência Social (CTPS).")
  @Column(name = "sercartpro", nullable = true, length = 8)
  private String sercartpro;

  /** Sigla da UF de emissão da CTPS. */
  @Comment("Sigla da UF de emissão da CTPS.")
  @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.CHAR)
  @Column(name = "ufcartpro", nullable = true, length = 2, columnDefinition = "character(2)")
  private String ufcartpro;

  /** Número de inscrição do Título de Eleitor. */
  @Comment("Número de inscrição do Título de Eleitor.")
  @Column(name = "numtitel", nullable = true, length = 15)
  private String numtitel;

  /** Zona eleitoral do Título de Eleitor. */
  @Comment("Zona eleitoral do Título de Eleitor.")
  @Column(name = "zonatitel", nullable = true, length = 6)
  private String zonatitel;

  /** Seção eleitoral do Título de Eleitor. */
  @Comment("Seção eleitoral do Título de Eleitor.")
  @Column(name = "sectitel", nullable = true, length = 6)
  private String sectitel;

  /** Sigla da UF do domicílio eleitoral. */
  @Comment("Sigla da UF do domicílio eleitoral.")
  @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.CHAR)
  @Column(name = "uftitel", nullable = true, length = 2, columnDefinition = "character(2)")
  private String uftitel;

  /** Número do Certificado de Reservista ou documento militar. */
  @Comment("Número do Certificado de Reservista ou documento militar.")
  @Column(name = "numdocmili", nullable = true, length = 20)
  private String numdocmili;

  /** Série ou categoria do documento militar. */
  @Comment("Série ou categoria do documento militar.")
  @Column(name = "serdocmili", nullable = true, length = 10)
  private String serdocmili;

  /** Número de registro da Carteira Nacional de Habilitação (CNH). */
  @Comment("Número de registro da Carteira Nacional de Habilitação (CNH).")
  @Column(name = "cnh", nullable = true, length = 15)
  private String cnh;

  /** Categoria de habilitação da CNH (ex.: B, AB, D, E). */
  @Comment("Categoria de habilitação da CNH (ex.: B, AB, D, E).")
  @Column(name = "catcnh", nullable = true, length = 5)
  private String catcnh;

  /** Data de vencimento do exame de aptidão da CNH. */
  @Comment("Data de vencimento do exame de aptidão da CNH.")
  @Column(name = "validcnh", nullable = true)
  private LocalDate validcnh;

  /** Sigla da UF do DETRAN emissor da CNH. */
  @Comment("Sigla da UF do DETRAN emissor da CNH.")
  @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.CHAR)
  @Column(name = "ufcnh", nullable = true, length = 2, columnDefinition = "character(2)")
  private String ufcnh;

  /** Número de registro no conselho ou ordem de classe profissional. */
  @Comment("Número de registro no conselho ou ordem de classe profissional.")
  @Column(name = "identprof", nullable = true, length = 20)
  private String identprof;

  /** Sigla do conselho profissional (ex.: OAB, CRM, CREA, COREN, CRC). */
  @Comment("Sigla do conselho profissional (ex.: OAB, CRM, CREA, COREN, CRC).")
  @Column(name = "tipoidprof", nullable = true, length = 20)
  private String tipoidprof;

  /**
   * Tipo de logradouro do endereço residencial (domínio Logradouros em itemtabelas, ex.: RUA, AV,
   * AL).
   */
  @Comment(
      "Tipo de logradouro do endereço residencial (domínio Logradouros em itemtabelas, ex.: RUA, AV, AL).")
  @Column(name = "tipologender", nullable = true, length = 15)
  private String tipologender;

  /** Nome do logradouro residencial do servidor. */
  @Comment("Nome do logradouro residencial do servidor.")
  @Column(name = "nomelogender", nullable = true, length = 120)
  private String nomelogender;

  /** Número predial do endereço residencial. */
  @Comment("Número predial do endereço residencial.")
  @Column(name = "numender", nullable = true, length = 15)
  private String numender;

  /** Complemento do endereço residencial (bloco, apartamento, casa). */
  @Comment("Complemento do endereço residencial (bloco, apartamento, casa).")
  @Column(name = "complender", nullable = true, length = 80)
  private String complender;

  /** Bairro ou distrito do endereço residencial. */
  @Comment("Bairro ou distrito do endereço residencial.")
  @Column(name = "bairroender", nullable = true, length = 80)
  private String bairroender;

  /** Município de residência do servidor. */
  @Comment("Município de residência do servidor.")
  @Column(name = "cidadeender", nullable = true, length = 60)
  private String cidadeender;

  /** Sigla da UF de residência do servidor. */
  @Comment("Sigla da UF de residência do servidor.")
  @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.CHAR)
  @Column(name = "ufender", nullable = true, length = 2, columnDefinition = "character(2)")
  private String ufender;

  /** Código de Endereçamento Postal (CEP) residencial com 8 dígitos. */
  @Comment("Código de Endereçamento Postal (CEP) residencial com 8 dígitos.")
  @Column(name = "cepender", nullable = true, length = 8)
  private String cepender;

  /** Telefone fixo de contato residencial. */
  @Comment("Telefone fixo de contato residencial.")
  @Column(name = "telefone", nullable = true, length = 20)
  private String telefone;

  /** Número de telefone móvel (celular/WhatsApp) do servidor. */
  @Comment("Número de telefone móvel (celular/WhatsApp) do servidor.")
  @Column(name = "numtel_celular", nullable = true, length = 20)
  private String numtelCelular;

  /** Ramal telefônico institucional de contato interno. */
  @Comment("Ramal telefônico institucional de contato interno.")
  @Column(name = "ramal", nullable = true, length = 10)
  private String ramal;

  /** Endereço de e-mail institucional ou pessoal principal do servidor. */
  @Comment("Endereço de e-mail institucional ou pessoal principal do servidor.")
  @Column(name = "e_mail", nullable = true, length = 120)
  private String eMail;

  /** Código febaban do banco padrão para crédito de remuneração. */
  @Comment("Código febaban do banco padrão para crédito de remuneração.")
  @Column(name = "banco", nullable = true)
  private Integer banco;

  /** Código da agência bancária (com dígito) para crédito de remuneração. */
  @Comment("Código da agência bancária (com dígito) para crédito de remuneração.")
  @Column(name = "agencia", nullable = true, length = 10)
  private String agencia;

  /** Número da conta corrente ou salário (com dígito) do servidor. */
  @Comment("Número da conta corrente ou salário (com dígito) do servidor.")
  @Column(name = "conta", nullable = true, length = 25)
  private String conta;

  /** Modalidade de pagamento bancário (domínio ERG_TIPO_PAG em itemtabelas, ex.: CC, CS, TESOU). */
  @Comment(
      "Modalidade de pagamento bancário (domínio ERG_TIPO_PAG em itemtabelas, ex.: CC, CS, TESOU).")
  @Column(name = "tipopag", nullable = true, length = 10)
  private String tipopag;

  /** Indica se o servidor possui deficiência física atestada. */
  @Comment("Indica se o servidor possui deficiência física atestada.")
  @Column(name = "deffisica", nullable = false)
  private Boolean deffisica = false;

  /** Indica se o servidor possui deficiência visual atestada. */
  @Comment("Indica se o servidor possui deficiência visual atestada.")
  @Column(name = "defvisual", nullable = false)
  private Boolean defvisual = false;

  /** Indica se o servidor possui deficiência auditiva atestada. */
  @Comment("Indica se o servidor possui deficiência auditiva atestada.")
  @Column(name = "defauditiva", nullable = false)
  private Boolean defauditiva = false;

  /** Indica se o servidor possui deficiência mental/psicossocial atestada. */
  @Comment("Indica se o servidor possui deficiência mental/psicossocial atestada.")
  @Column(name = "defmental", nullable = false)
  private Boolean defmental = false;

  /** Indica se o servidor possui deficiência intelectual atestada. */
  @Comment("Indica se o servidor possui deficiência intelectual atestada.")
  @Column(name = "defintelectual", nullable = false)
  private Boolean defintelectual = false;

  /** Observações médicas ou de adaptação de acessibilidade relativas a deficiências. */
  @Comment("Observações médicas ou de adaptação de acessibilidade relativas a deficiências.")
  @Column(name = "obs_deficiencia", nullable = true, length = 255)
  private String obsDeficiencia;

  /** Data da última atualização ou recadastramento anual obrigatório do servidor. */
  @Comment("Data da última atualização ou recadastramento anual obrigatório do servidor.")
  @Column(name = "dt_recadast", nullable = true)
  private LocalDate dtRecadast;

  /**
   * Referência relacional explícita ao ato ou publicação associada ao cadastro civil (substitui
   * PONTPUBL/PONTARQS).
   */
  @Comment(
      "Referência relacional explícita ao ato ou publicação associada ao cadastro civil (substitui PONTPUBL/PONTARQS).")
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
      resourceKey = "administracao-pessoal.funcionarios")
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
