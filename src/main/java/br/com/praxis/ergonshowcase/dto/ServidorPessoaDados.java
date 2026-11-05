package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.*;
import lombok.Getter;
import lombok.Setter;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.customfields.CustomFields;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.extension.annotation.UISchemaPreset;

/** Cadastro civil compartilhado pelos vínculos da mesma pessoa. */
@Getter
@Setter
public abstract class ServidorPessoaDados {
  @NotBlank
  @Size(max = 300)
  @Schema(
      description =
          "Nome civil completo que identifica a pessoa titular de um ou mais vínculos funcionais.")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Identifica a pessoa no cadastro civil.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @UISchema(
      label = "Nome civil",
      preset = UISchemaPreset.ENTERPRISE_NAME,
      group = "Identificação civil",
      order = 10,
      icon = "person")

  private String nome;

  @Size(max = 70)
  @Schema(
      description =
          "Nome social adotado pela pessoa para identificação no atendimento administrativo.")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Identificação pessoal escolhida para atendimento administrativo.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @UISchema(
      label = "Nome social",
      group = "Identificação civil",
      order = 20,
      icon = "person_outline")
  private String nomeSocial;

  @NotBlank
  @Pattern(regexp = "[0-9]{11}")
  @Schema(
      description =
          "CPF da pessoa, com onze dígitos sem formatação; identifica o cadastro civil único e preserva zeros à esquerda.")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      complianceTags = {"LGPD", "NON_EXPORTABLE"},
      reason = "Identificação fiscal individual protegida contra exportação em massa.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @UISchema(
      label = "CPF",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.CPF_CNPJ_INPUT,
      tableHidden = true,
      group = "Identificação civil",
      order = 30,
      icon = "badge",
      visibleForAuthorities = {"ERGON_READ", "ERGON_WRITE", "ERGON_PII_READ"},
      editableForAuthorities = {"ERGON_WRITE"},
      fieldAccessReason = "Dado fiscal pessoal (LGPD) governado para inspeção individual.")
  @jakarta.validation.constraints.Size(max = 11)
  private String cpf;

  @NotNull
  @Schema(
      description =
          "Data de nascimento civil da pessoa, utilizada no cadastro funcional e na conferência de identificação.")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Dado biográfico individual.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @UISchema(
      label = "Nascimento",
      controlType = org.praxisplatform.uischema.FieldControlType.DATE_PICKER,
      group = "Identificação civil",
      order = 40,
      icon = "event",
      type = org.praxisplatform.uischema.FieldDataType.DATE, quickViewVisible = true, quickViewOrder = 1)
  private LocalDate dtnasc;

  @Schema(nullable = true, description = "Data de óbito ou desaparecimento informada no registro civil. Quando preenchida, é a referência para validar a idade máxima do servidor.")
  @DomainGovernance(kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Data de evento do registro civil individual.",
      aiUsage = @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @UISchema(label = "Óbito ou desaparecimento", group = "Identificação civil", order = 42,
      icon = "event", tableHidden = true, helpText = "Informe a data registrada na certidão ou no ato.",
      type = org.praxisplatform.uischema.FieldDataType.DATE,
      controlType = org.praxisplatform.uischema.FieldControlType.DATE_PICKER)
  private LocalDate dtCertFim;

  @Email
  @Size(max = 120)
  @Schema(
      description =
          "Endereço eletrônico principal, institucional ou pessoal, informado para contato com o servidor.")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Canal de contato individual.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @UISchema(
      label = "E-mail",
      controlType = FieldControlType.EMAIL_INPUT,
      group = "Contato",
      order = 50, quickViewVisible = true, quickViewOrder = 4,
      icon = "email")
  @Getter(onMethod_ = @com.fasterxml.jackson.annotation.JsonProperty("eMail"))
  @Setter(onMethod_ = @com.fasterxml.jackson.annotation.JsonProperty("eMail"))
  private String eMail;

  @Size(max = 20)
  @Schema(
      description = "Telefone fixo residencial informado pela pessoa para contato administrativo.")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Contato residencial do servidor.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @UISchema(
      label = "Telefone",
      type = FieldDataType.TEXT,
      group = "Contato",
      order = 60,
      icon = "phone")
  private String telefone;

  @Size(max = 20)
  @Schema(
      description =
          "Telefone móvel da pessoa para contato; mantém código de área e formatação informada.")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Contato móvel individual.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @UISchema(
      label = "Celular",
      type = FieldDataType.TEXT,
      group = "Contato",
      order = 70, quickViewVisible = true, quickViewOrder = 5,
      icon = "smartphone")
  private String numtelCelular;

  @Pattern(regexp = "[0-9]{11}")
  @Schema(
      description =
          "Inscrição do servidor no PIS/PASEP ou NIT, utilizada na identificação trabalhista e previdenciária.")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      complianceTags = {"LGPD", "NON_EXPORTABLE"},
      reason = "Identificador trabalhista e previdenciário individual.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @UISchema(
      label = "PIS/PASEP",
      type = FieldDataType.TEXT,
      tableHidden = true,
      group = "Documentação",
      order = 80,
      icon = "assignment_ind",
      visibleForAuthorities = {"ERGON_READ", "ERGON_WRITE", "ERGON_PII_READ"},
      editableForAuthorities = {"ERGON_WRITE"},
      fieldAccessReason = "Inscrição previdenciária individual (LGPD) governada para inspeção autorizada.")
  @jakarta.validation.constraints.Size(max = 11)
  private String pispasep;

  @CustomFields(resourceKey = "administracao-pessoal.funcionarios")
  @Schema(
      description =
          "Dados adicionais do cadastro civil previstos no catálogo de campos do órgão, com validação e governança próprias.")
  @UISchema(label = "Características do órgão", group = "Características adicionais", order = 90)
  private Map<String, Object> customFields = new LinkedHashMap<>();

  @Schema(description = "Tratamento protocolar usado na identificação do servidor.")
  @UISchema(label = "Tratamento", group = "Identificação civil", tableHidden = true, order = 200)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Tratamento protocolar usado na identificação do servidor.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @jakarta.validation.constraints.Size(max = 20)
  private String tratamento;

  @Schema(
      description = "Sexo registral informado no cadastro civil ([F]eminino ou [M]asculino).",
      allowableValues = {"F", "M"},
      nullable = true)
  @UISchema(
      label = "Sexo registral",
      group = "Identificação civil",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      options =
          "[{\"value\":\"F\",\"label\":\"Feminino (F)\"},{\"value\":\"M\",\"label\":\"Masculino (M)\"}]",
      icon = "wc",
      tableHidden = true,
      order = 201)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Sexo registral informado no cadastro civil.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @jakarta.validation.constraints.Size(max = 1)
  private String sexo;

  @Schema(description = "Identidade de gênero autodeclarada pela pessoa.")
  @UISchema(
      label = "Identidade de gênero",
      group = "Identificação civil",
      tableHidden = true,
      order = 202)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Identidade de gênero autodeclarada pela pessoa.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @jakarta.validation.constraints.Size(max = 100)
  private String genero;

  @Schema(description = "Município de nascimento da pessoa.")
  @UISchema(label = "Naturalidade", group = "Identificação civil", tableHidden = true, order = 203)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Município de nascimento da pessoa.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @jakarta.validation.constraints.Size(max = 60)
  private String cidnasc;

  @Schema(description = "Unidade federativa de nascimento da pessoa.")
  @UISchema(
      label = "UF de nascimento",
      group = "Identificação civil",
      tableHidden = true,
      order = 204)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Unidade federativa de nascimento da pessoa.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @jakarta.validation.constraints.Size(max = 2)
  private String ufnasc;

  @Schema(description = "Grupo sanguíneo informado para o cadastro de saúde do servidor.")
  @UISchema(
      label = "Grupo sanguíneo",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      options =
          "[{\"value\":\"A+\",\"label\":\"Tipo A Positivo\"},{\"value\":\"A-\",\"label\":\"Tipo A Negativo\"},{\"value\":\"B+\",\"label\":\"Tipo B Positivo\"},{\"value\":\"B-\",\"label\":\"Tipo B Negativo\"},{\"value\":\"AB+\",\"label\":\"Tipo AB Positivo\"},{\"value\":\"AB-\",\"label\":\"Tipo AB Negativo\"},{\"value\":\"O+\",\"label\":\"Tipo O Positivo\"},{\"value\":\"O-\",\"label\":\"Tipo O Negativo\"}]",
      icon = "bloodtype",
      group = "Identificação civil",
      tableHidden = true,
      order = 205)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.RESTRICTED,
      dataCategory = DomainDataCategory.SENSITIVE_PERSONAL,
      reason = "Grupo sanguíneo informado para o cadastro de saúde do servidor.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @Getter(onMethod_ = @com.fasterxml.jackson.annotation.JsonProperty("gSanguineo"))
  @Setter(onMethod_ = @com.fasterxml.jackson.annotation.JsonProperty("gSanguineo"))
  @jakarta.validation.constraints.Size(max = 3)
  private String gSanguineo;

  @Schema(nullable = true, description = "Raça ou cor autodeclarada para identificação e políticas de pessoal.")
  @UISchema(
      label = "Raça/cor",
      group = "Identificação civil",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      options =
          "[{\"value\":\"1\",\"label\":\"Indígena\"},{\"value\":\"2\",\"label\":\"Branca\"},{\"value\":\"4\",\"label\":\"Preta\"},{\"value\":\"6\",\"label\":\"Amarela\"},{\"value\":\"8\",\"label\":\"Parda\"},{\"value\":\"9\",\"label\":\"Não informado\"}]",
      icon = "diversity_3",
      tableHidden = true,
      order = 206)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.RESTRICTED,
      dataCategory = DomainDataCategory.SENSITIVE_PERSONAL,
      reason = "Raça ou cor autodeclarada para identificação e políticas de pessoal.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @jakarta.validation.constraints.Size(max = 20)
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  private String racaCor;

  @Schema(description = "Estado civil declarado pela pessoa.")
  @UISchema(
      label = "Estado civil",
      group = "Identificação civil",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      options =
          "[{\"value\":\"SOLTEIRO\",\"label\":\"Solteiro(a)\"},{\"value\":\"CASADO\",\"label\":\"Casado(a)\"},{\"value\":\"DIVORCIADO\",\"label\":\"Divorciado(a)\"},{\"value\":\"SEPARADO\",\"label\":\"Separado(a) Judicialmente ou Extrajudicialmente\"},{\"value\":\"UNIAO_ESTAVEL\",\"label\":\"Companheiro(a) em União Estável\"},{\"value\":\"VIUVO\",\"label\":\"Viúvo(a)\"}]",
      icon = "favorite",
      tableHidden = true,
      order = 207, quickViewVisible = true, quickViewOrder = 2)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Estado civil declarado pela pessoa.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @jakarta.validation.constraints.Size(max = 20)
  private String estcivil;

  @Schema(description = "Indica união estável declarada para fins cadastrais.")
  @UISchema(label = "União estável", group = "Identificação civil", tableHidden = true, order = 208)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Indica união estável declarada para fins cadastrais.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private Boolean uniaoEstavel = false;

  @Schema(nullable = true, description = "Grau de instrução concluído pela pessoa.")
  @UISchema(
      label = "Escolaridade",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      options =
          "[{\"value\":\"FUND_INCOMP\",\"label\":\"Ensino Fundamental Incompleto\"},{\"value\":\"FUND_COMP\",\"label\":\"Ensino Fundamental Completo\"},{\"value\":\"MEDIO_COMP\",\"label\":\"Ensino Médio Completo / Técnico\"},{\"value\":\"SUPERIOR_COMP\",\"label\":\"Ensino Superior Completo (Graduação)\"},{\"value\":\"ESPECIALIZACAO\",\"label\":\"Pós-Graduação Lato Sensu (Especialização / MBA)\"},{\"value\":\"MESTRADO\",\"label\":\"Pós-Graduação Stricto Sensu (Mestrado)\"},{\"value\":\"DOUTORADO\",\"label\":\"Pós-Graduação Stricto Sensu (Doutorado)\"}]",
      icon = "school",
      group = "Identificação civil",
      tableHidden = true,
      order = 209,
      quickViewVisible = true,
      quickViewOrder = 3)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Grau de instrução concluído pela pessoa.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @jakarta.validation.constraints.Size(max = 20)
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  private String escolaridade;

  @Schema(description = "Código da nacionalidade no domínio cadastral Ergon.")
  @UISchema(
      label = "Nacionalidade",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.SELECT,
      options =
          "[{\"value\":10,\"label\":\"Brasileira Nata\"},{\"value\":20,\"label\":\"Brasileira Naturalizada\"},{\"value\":21,\"label\":\"Argentina\"},{\"value\":22,\"label\":\"Boliviana\"},{\"value\":23,\"label\":\"Chilena\"},{\"value\":24,\"label\":\"Paraguaia\"},{\"value\":25,\"label\":\"Uruguaia\"},{\"value\":30,\"label\":\"Alemã\"},{\"value\":35,\"label\":\"Espanhola\"},{\"value\":37,\"label\":\"Francesa\"},{\"value\":39,\"label\":\"Italiana\"},{\"value\":41,\"label\":\"Japonesa\"},{\"value\":45,\"label\":\"Portuguesa\"},{\"value\":50,\"label\":\"Outras\"}]",
      icon = "public",
      group = "Identificação civil",
      tableHidden = true,
      order = 210)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Código da nacionalidade no domínio cadastral Ergon.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private Integer nacionalidade;

  @Schema(description = "Ano de chegada ao Brasil da pessoa estrangeira.")
  @UISchema(
      label = "Chegada ao Brasil",
      group = "Identificação civil",
      tableHidden = true,
      order = 211)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Ano de chegada ao Brasil da pessoa estrangeira.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private Integer chegbrasil;

  @Schema(description = "UF do emprego anterior para informações trabalhistas.")
  @UISchema(
      label = "UF do emprego anterior",
      group = "Identificação civil",
      tableHidden = true,
      order = 212)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "UF do emprego anterior para informações trabalhistas.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @jakarta.validation.constraints.Size(max = 2)
  private String ufempant;

  @Schema(description = "Ano de ingresso no primeiro emprego informado pela pessoa.")
  @UISchema(
      label = "Ano do primeiro emprego",
      group = "Identificação civil",
      tableHidden = true,
      order = 213)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Ano de ingresso no primeiro emprego informado pela pessoa.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private Integer anoprimemp;

  @Schema(description = "Nome civil do pai constante do assentamento de filiação.")
  @UISchema(label = "Nome do pai", group = "Filiação", tableHidden = true, order = 214)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Nome civil do pai constante do assentamento de filiação.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @jakarta.validation.constraints.Size(max = 300)
  private String pai;

  @Schema(description = "Nome civil da mãe constante do assentamento de filiação.")
  @UISchema(label = "Nome da mãe", group = "Filiação", tableHidden = true, order = 215)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Nome civil da mãe constante do assentamento de filiação.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @jakarta.validation.constraints.Size(max = 300)
  private String mae;

  @Schema(description = "Número do documento de identidade civil, preservando zeros e letras.")
  @UISchema(
      label = "Número do RG",
      group = "Documentação civil e trabalhista",
      tableHidden = true,
      order = 216,
      visibleForAuthorities = {"ERGON_READ", "ERGON_WRITE", "ERGON_PII_READ"},
      editableForAuthorities = {"ERGON_WRITE"},
      fieldAccessReason = "Documento civil de identificação (LGPD) restrito a perfis autorizados.")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      complianceTags = {"LGPD", "NON_EXPORTABLE"},
      reason = "Número do documento de identidade civil, preservando zeros e letras.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @jakarta.validation.constraints.Size(max = 24)
  private String numrg;

  @Schema(description = "Tipo do documento civil apresentado como identidade.")
  @UISchema(
      label = "Tipo de identidade",
      group = "Documentação civil e trabalhista",
      tableHidden = true,
      order = 217)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Tipo do documento civil apresentado como identidade.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @jakarta.validation.constraints.Size(max = 20)
  private String tiporg;

  @Schema(description = "Órgão emissor do documento de identidade civil.")
  @UISchema(
      label = "Órgão emissor",
      group = "Documentação civil e trabalhista",
      tableHidden = true,
      order = 218)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Órgão emissor do documento de identidade civil.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @jakarta.validation.constraints.Size(max = 20)
  private String orgaorg;

  @Schema(description = "UF de emissão do documento de identidade.")
  @UISchema(
      label = "UF de emissão",
      group = "Documentação civil e trabalhista",
      tableHidden = true,
      order = 219)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "UF de emissão do documento de identidade.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @jakarta.validation.constraints.Size(max = 2)
  private String ufrg;

  @Schema(description = "Data de expedição do documento de identidade civil.")
  @UISchema(
      label = "Expedição do RG",
      group = "Documentação civil e trabalhista",
      tableHidden = true,
      order = 220,
      controlType = org.praxisplatform.uischema.FieldControlType.DATE_PICKER,
      type = org.praxisplatform.uischema.FieldDataType.DATE,
      icon = "event")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Data de expedição do documento de identidade civil.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private LocalDate expedrg;

  @Schema(description = "Data de inscrição da pessoa no PIS ou PASEP.")
  @UISchema(
      label = "Inscrição PIS/PASEP",
      group = "Documentação civil e trabalhista",
      tableHidden = true,
      order = 221,
      controlType = org.praxisplatform.uischema.FieldControlType.DATE_PICKER,
      type = org.praxisplatform.uischema.FieldDataType.DATE,
      icon = "event")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Data de inscrição da pessoa no PIS ou PASEP.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private LocalDate datapis;

  @Schema(description = "Banco vinculado à inscrição PIS/PASEP.")
  @UISchema(
      label = "Banco do PIS/PASEP",
      group = "Documentação civil e trabalhista",
      tableHidden = true,
      order = 222)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Banco vinculado à inscrição PIS/PASEP.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private Integer bancopis;

  @Schema(description = "Número da Carteira de Trabalho e Previdência Social.")
  @UISchema(
      label = "Número da CTPS",
      group = "Documentação civil e trabalhista",
      tableHidden = true,
      order = 223)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Número da Carteira de Trabalho e Previdência Social.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @jakarta.validation.constraints.Size(max = 12)
  private String numcartpro;

  @Schema(description = "Série da Carteira de Trabalho, que complementa sua identificação.")
  @UISchema(
      label = "Série da CTPS",
      group = "Documentação civil e trabalhista",
      tableHidden = true,
      order = 224)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Série da Carteira de Trabalho, que complementa sua identificação.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @jakarta.validation.constraints.Size(max = 8)
  private String sercartpro;

  @Schema(description = "UF de emissão da Carteira de Trabalho.")
  @UISchema(
      label = "UF da CTPS",
      group = "Documentação civil e trabalhista",
      tableHidden = true,
      order = 225)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "UF de emissão da Carteira de Trabalho.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @jakarta.validation.constraints.Size(max = 2)
  private String ufcartpro;

  @Schema(description = "Número da inscrição eleitoral da pessoa.")
  @UISchema(
      label = "Título eleitoral",
      group = "Documentação civil e trabalhista",
      tableHidden = true,
      order = 226)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Número da inscrição eleitoral da pessoa.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @jakarta.validation.constraints.Size(max = 15)
  private String numtitel;

  @Schema(description = "Zona eleitoral da inscrição da pessoa.")
  @UISchema(
      label = "Zona eleitoral",
      group = "Documentação civil e trabalhista",
      tableHidden = true,
      order = 227)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Zona eleitoral da inscrição da pessoa.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @jakarta.validation.constraints.Size(max = 6)
  private String zonatitel;

  @Schema(description = "Seção eleitoral da inscrição da pessoa.")
  @UISchema(
      label = "Seção eleitoral",
      group = "Documentação civil e trabalhista",
      tableHidden = true,
      order = 228)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Seção eleitoral da inscrição da pessoa.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @jakarta.validation.constraints.Size(max = 6)
  private String sectitel;

  @Schema(description = "UF da inscrição eleitoral.")
  @UISchema(
      label = "UF eleitoral",
      group = "Documentação civil e trabalhista",
      tableHidden = true,
      order = 229)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "UF da inscrição eleitoral.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @jakarta.validation.constraints.Size(max = 2)
  private String uftitel;

  @Schema(description = "Número do documento de situação militar apresentado.")
  @UISchema(
      label = "Documento militar",
      group = "Documentação civil e trabalhista",
      tableHidden = true,
      order = 230)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Número do documento de situação militar apresentado.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @jakarta.validation.constraints.Size(max = 20)
  private String numdocmili;

  @Schema(description = "Série do documento militar apresentado.")
  @UISchema(
      label = "Série militar",
      group = "Documentação civil e trabalhista",
      tableHidden = true,
      order = 231)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Série do documento militar apresentado.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @jakarta.validation.constraints.Size(max = 10)
  private String serdocmili;

  @Schema(description = "Número da Carteira Nacional de Habilitação.")
  @UISchema(
      label = "Número da CNH",
      group = "Documentação civil e trabalhista",
      tableHidden = true,
      order = 232)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Número da Carteira Nacional de Habilitação.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @jakarta.validation.constraints.Size(max = 15)
  private String cnh;

  @Schema(description = "Categoria de habilitação para condução de veículos.")
  @UISchema(
      label = "Categoria da CNH",
      group = "Documentação civil e trabalhista",
      tableHidden = true,
      order = 233)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Categoria de habilitação para condução de veículos.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @jakarta.validation.constraints.Size(max = 5)
  private String catcnh;

  @Schema(description = "Data limite de validade da habilitação.")
  @UISchema(
      label = "Validade da CNH",
      group = "Documentação civil e trabalhista",
      tableHidden = true,
      order = 234,
      controlType = org.praxisplatform.uischema.FieldControlType.DATE_PICKER,
      type = org.praxisplatform.uischema.FieldDataType.DATE,
      icon = "event")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Data limite de validade da habilitação.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private LocalDate validcnh;

  @Schema(description = "UF emissora da habilitação.")
  @UISchema(
      label = "UF da CNH",
      group = "Documentação civil e trabalhista",
      tableHidden = true,
      order = 235)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "UF emissora da habilitação.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @jakarta.validation.constraints.Size(max = 2)
  private String ufcnh;

  @Schema(description = "Registro profissional apresentado pela pessoa; exige o respectivo tipo.")
  @UISchema(
      label = "Registro profissional",
      group = "Documentação civil e trabalhista",
      tableHidden = true,
      order = 236)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Registro profissional apresentado pela pessoa; exige o respectivo tipo.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @jakarta.validation.constraints.Size(max = 20)
  private String identprof;

  @Schema(description = "Tipo ou conselho do registro profissional; exige o número correspondente.")
  @UISchema(
      label = "Conselho ou tipo de registro",
      group = "Documentação civil e trabalhista",
      tableHidden = true,
      order = 237)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Tipo ou conselho do registro profissional; exige o número correspondente.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @jakarta.validation.constraints.Size(max = 20)
  private String tipoidprof;

  @Schema(description = "Identidade do documento legal relacionado ao cadastro civil.")
  @UISchema(
      label = "Documento legal",
      group = "Documentação civil e trabalhista",
      tableHidden = true,
      order = 238,
      type = org.praxisplatform.uischema.FieldDataType.TEXT,
      controlType = org.praxisplatform.uischema.FieldControlType.ENTITY_LOOKUP,
      icon = "gavel",
      endpoint = ApiPaths.DOCUMENTO_OPTIONS,
      valueField = "id",
      displayField = "label")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Identidade do documento legal relacionado ao cadastro civil.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @ResourceReference(
      resourceKey = "administracao-pessoal.documentos-legais",
      valueField = "documentoLegalId",
      surfaceId = "ficha-documento",
      previewSurfaceId = "ficha-documento",
      detailPresentation = "drawer")
  private Long documentoLegalId;

  @Schema(description = "Tipo do logradouro residencial, como rua ou avenida.")
  @UISchema(
      label = "Tipo de logradouro",
      group = "Endereço e contato",
      tableHidden = true,
      order = 239)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Tipo do logradouro residencial, como rua ou avenida.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @jakarta.validation.constraints.Size(max = 15)
  private String tipologender;

  @Schema(description = "Nome do logradouro do endereço residencial.")
  @UISchema(label = "Logradouro", group = "Endereço e contato", tableHidden = true, order = 240)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Nome do logradouro do endereço residencial.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @jakarta.validation.constraints.Size(max = 120)
  private String nomelogender;

  @Schema(description = "Número do imóvel residencial, mantido como texto.")
  @UISchema(label = "Número", group = "Endereço e contato", tableHidden = true, order = 241)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Número do imóvel residencial, mantido como texto.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @jakarta.validation.constraints.Size(max = 15)
  private String numender;

  @Schema(description = "Complemento do endereço residencial.")
  @UISchema(label = "Complemento", group = "Endereço e contato", tableHidden = true, order = 242)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Complemento do endereço residencial.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @jakarta.validation.constraints.Size(max = 80)
  private String complender;

  @Schema(description = "Bairro de residência da pessoa.")
  @UISchema(label = "Bairro", group = "Endereço e contato", tableHidden = true, order = 243)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Bairro de residência da pessoa.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @jakarta.validation.constraints.Size(max = 80)
  private String bairroender;

  @Schema(nullable = true, description = "Município de residência da pessoa.")
  @UISchema(
      label = "Município",
      icon = "location_city",
      group = "Endereço e contato",
      tableHidden = true,
      order = 244,
      quickViewVisible = true,
      quickViewOrder = 6)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Município de residência da pessoa.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @jakarta.validation.constraints.Size(max = 60)
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  private String cidadeender;

  @Schema(nullable = true, description = "UF de residência da pessoa.")
  @UISchema(
      label = "UF de residência",
      icon = "map",
      group = "Endereço e contato",
      tableHidden = true,
      order = 245)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "UF de residência da pessoa.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @jakarta.validation.constraints.Size(max = 2)
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  private String ufender;

  @Schema(description = "Código postal do endereço residencial, preservando zeros.")
  @UISchema(label = "CEP", group = "Endereço e contato", tableHidden = true, order = 246)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Código postal do endereço residencial, preservando zeros.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @jakarta.validation.constraints.Size(max = 8)
  private String cepender;

  @Schema(description = "Ramal de contato administrativo da pessoa.")
  @UISchema(label = "Ramal", group = "Endereço e contato", tableHidden = true, order = 247)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Ramal de contato administrativo da pessoa.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @jakarta.validation.constraints.Size(max = 10)
  private String ramal;

  @Schema(nullable = true, description = "Código da instituição bancária padrão para pagamento ao servidor.")
  @UISchema(
      label = "Banco",
      group = "Dados bancários padrão",
      tableHidden = true,
      order = 248,
      visibleForAuthorities = {"ERGON_READ", "ERGON_WRITE", "ERGON_FINANCIAL_READ"},
      editableForAuthorities = {"ERGON_WRITE"},
      fieldAccessReason = "Dado bancário individual protegido contra exposição não autorizada.")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.FINANCIAL,
      complianceTags = {"LGPD", "FINANCIAL_DATA", "NON_EXPORTABLE"},
      reason = "Código da instituição bancária padrão para pagamento ao servidor.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  private Integer banco;

  @Schema(nullable = true, description = "Agência bancária padrão, incluindo eventual dígito verificador.")
  @UISchema(
      label = "Agência",
      group = "Dados bancários padrão",
      tableHidden = true,
      order = 249,
      visibleForAuthorities = {"ERGON_READ", "ERGON_WRITE", "ERGON_FINANCIAL_READ"},
      editableForAuthorities = {"ERGON_WRITE"},
      fieldAccessReason = "Agência bancária individual protegida contra exposição não autorizada.")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.FINANCIAL,
      complianceTags = {"LGPD", "FINANCIAL_DATA", "NON_EXPORTABLE"},
      reason = "Agência bancária padrão, incluindo eventual dígito verificador.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @jakarta.validation.constraints.Size(max = 10)
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  private String agencia;

  @Schema(description = "Conta bancária padrão, preservando zeros e dígito verificador.")
  @UISchema(
      label = "Conta",
      group = "Dados bancários padrão",
      tableHidden = true,
      order = 250,
      visibleForAuthorities = {"ERGON_READ", "ERGON_WRITE", "ERGON_FINANCIAL_READ"},
      editableForAuthorities = {"ERGON_WRITE"},
      fieldAccessReason = "Conta bancária individual protegida contra exposição e exportação em massa.")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.FINANCIAL,
      complianceTags = {"LGPD", "FINANCIAL_DATA", "NON_EXPORTABLE"},
      reason = "Conta bancária padrão, preservando zeros e dígito verificador.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @jakarta.validation.constraints.Size(max = 25)
  private String conta;

  @Schema(nullable = true, description = "Modalidade de pagamento associada aos dados bancários padrão.")
  @UISchema(
      label = "Tipo de pagamento",
      group = "Dados bancários padrão",
      tableHidden = true,
      order = 251)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.FINANCIAL,
      reason = "Modalidade de pagamento associada aos dados bancários padrão.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @jakarta.validation.constraints.Size(max = 10)
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  private String tipopag;

  @Schema(description = "Indica deficiência física registrada no cadastro da pessoa.")
  @UISchema(
      label = "Deficiência física",
      group = "Acessibilidade e PcD",
      tableHidden = true,
      order = 252,
      visibleForAuthorities = {"ERGON_READ", "ERGON_WRITE", "ERGON_HEALTH_READ"},
      editableForAuthorities = {"ERGON_WRITE"},
      fieldAccessReason = "Dado pessoal sensível de saúde/PcD (LGPD Art. 11).")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.RESTRICTED,
      dataCategory = DomainDataCategory.SENSITIVE_PERSONAL,
      complianceTags = {"LGPD", "HEALTH_DATA", "NON_EXPORTABLE"},
      reason = "Indica deficiência física registrada no cadastro da pessoa.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private Boolean deffisica = false;

  @Schema(description = "Indica deficiência visual registrada no cadastro da pessoa.")
  @UISchema(
      label = "Deficiência visual",
      group = "Acessibilidade e PcD",
      tableHidden = true,
      order = 253,
      visibleForAuthorities = {"ERGON_READ", "ERGON_WRITE", "ERGON_HEALTH_READ"},
      editableForAuthorities = {"ERGON_WRITE"},
      fieldAccessReason = "Dado pessoal sensível de saúde/PcD (LGPD Art. 11).")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.RESTRICTED,
      dataCategory = DomainDataCategory.SENSITIVE_PERSONAL,
      complianceTags = {"LGPD", "HEALTH_DATA", "NON_EXPORTABLE"},
      reason = "Indica deficiência visual registrada no cadastro da pessoa.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private Boolean defvisual = false;

  @Schema(description = "Indica deficiência auditiva registrada no cadastro da pessoa.")
  @UISchema(
      label = "Deficiência auditiva",
      group = "Acessibilidade e PcD",
      tableHidden = true,
      order = 254,
      visibleForAuthorities = {"ERGON_READ", "ERGON_WRITE", "ERGON_HEALTH_READ"},
      editableForAuthorities = {"ERGON_WRITE"},
      fieldAccessReason = "Dado pessoal sensível de saúde/PcD (LGPD Art. 11).")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.RESTRICTED,
      dataCategory = DomainDataCategory.SENSITIVE_PERSONAL,
      complianceTags = {"LGPD", "HEALTH_DATA", "NON_EXPORTABLE"},
      reason = "Indica deficiência auditiva registrada no cadastro da pessoa.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private Boolean defauditiva = false;

  @Schema(description = "Indica deficiência mental registrada no cadastro da pessoa.")
  @UISchema(
      label = "Deficiência mental",
      group = "Acessibilidade e PcD",
      tableHidden = true,
      order = 255,
      visibleForAuthorities = {"ERGON_READ", "ERGON_WRITE", "ERGON_HEALTH_READ"},
      editableForAuthorities = {"ERGON_WRITE"},
      fieldAccessReason = "Dado pessoal sensível de saúde/PcD (LGPD Art. 11).")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.RESTRICTED,
      dataCategory = DomainDataCategory.SENSITIVE_PERSONAL,
      complianceTags = {"LGPD", "HEALTH_DATA", "NON_EXPORTABLE"},
      reason = "Indica deficiência mental registrada no cadastro da pessoa.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private Boolean defmental = false;

  @Schema(description = "Indica deficiência intelectual registrada no cadastro da pessoa.")
  @UISchema(
      label = "Deficiência intelectual",
      group = "Acessibilidade e PcD",
      tableHidden = true,
      order = 256,
      visibleForAuthorities = {"ERGON_READ", "ERGON_WRITE", "ERGON_HEALTH_READ"},
      editableForAuthorities = {"ERGON_WRITE"},
      fieldAccessReason = "Dado pessoal sensível de saúde/PcD (LGPD Art. 11).")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.RESTRICTED,
      dataCategory = DomainDataCategory.SENSITIVE_PERSONAL,
      complianceTags = {"LGPD", "HEALTH_DATA", "NON_EXPORTABLE"},
      reason = "Indica deficiência intelectual registrada no cadastro da pessoa.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private Boolean defintelectual = false;

  @Schema(description = "Informações complementares sobre acessibilidade e deficiência da pessoa.")
  @UISchema(
      label = "Observações de acessibilidade",
      group = "Acessibilidade e PcD",
      tableHidden = true,
      order = 257,
      visibleForAuthorities = {"ERGON_READ", "ERGON_WRITE", "ERGON_HEALTH_READ"},
      editableForAuthorities = {"ERGON_WRITE"},
      fieldAccessReason = "Laudo e observações clínicas de PcD protegidos por sigilo (LGPD Art. 11).")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.RESTRICTED,
      dataCategory = DomainDataCategory.SENSITIVE_PERSONAL,
      complianceTags = {"LGPD", "HEALTH_DATA", "NON_EXPORTABLE"},
      reason = "Informações complementares sobre acessibilidade e deficiência da pessoa.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @jakarta.validation.constraints.Size(max = 255)
  private String obsDeficiencia;

  @Schema(description = "Data da última conferência cadastral registrada pelo órgão.")
  @UISchema(
      label = "Último recadastramento",
      group = "Recadastramento",
      tableHidden = true,
      order = 258,
      controlType = org.praxisplatform.uischema.FieldControlType.DATE_PICKER,
      type = org.praxisplatform.uischema.FieldDataType.DATE,
      icon = "event",
      quickViewVisible = true,
      quickViewOrder = 7)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Data da última conferência cadastral registrada pelo órgão.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private LocalDate dtRecadast;

  @jakarta.validation.constraints.Null(
      message = "Prévia derivada não pode ser informada no submit.")
  @Schema(
      description =
          "Disjunção informativa dos cinco indicadores específicos; o cadastro definitivo recalcula o fato e não aceita esta prévia como autoridade.")
  @UISchema(
      label = "PcD no formulário",
      group = "Acessibilidade e PcD",
      readOnly = true,
      tableHidden = true,
      icon = "info",
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.RESTRICTED,
      dataCategory = DomainDataCategory.SENSITIVE_PERSONAL,
      reason = "Prévia de indicador de deficiência no formulário.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
  private Boolean possuiDeficienciaPcDPrevisto;
}
