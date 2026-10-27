package br.com.praxis.ergonshowcase.dto;

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
      icon = "person",
      quickViewVisible = true,
      quickViewOrder = 1)
  @ResourceReference(
      resourceKey = "administracao-pessoal.funcionarios",
      valueField = "id",
      surfaceId = "ficha-cadastral",
      previewSurfaceId = "ficha-cadastral",
      detailPresentation = "drawer")
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
      icon = "person_outline",
      quickViewVisible = true,
      quickViewOrder = 2)
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
      reason = "Identificação fiscal individual.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @UISchema(
      label = "CPF",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.CPF_CNPJ_INPUT,
      tableHidden = true,
      group = "Identificação civil",
      order = 30,
      icon = "badge")
  private String cpf;

  @NotNull
  @Past
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
      controlType = FieldControlType.DATE_PICKER,
      group = "Identificação civil",
      order = 40,
      icon = "cake",
      type = org.praxisplatform.uischema.FieldDataType.DATE)
  private LocalDate dtnasc;

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
      order = 50,
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
      order = 70,
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
      reason = "Identificador trabalhista e previdenciário individual.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @UISchema(
      label = "PIS/PASEP",
      type = FieldDataType.TEXT,
      tableHidden = true,
      group = "Documentação",
      order = 80,
      icon = "assignment_ind")
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
  private String tratamento;

  @Schema(description = "Sexo registral informado no cadastro civil.")
  @UISchema(
      label = "Sexo registral",
      group = "Identificação civil",
      tableHidden = true,
      order = 201)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Sexo registral informado no cadastro civil.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
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
  private String ufnasc;

  @Schema(description = "Grupo sanguíneo informado para o cadastro de saúde do servidor.")
  @UISchema(
      label = "Grupo sanguíneo",
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
  private String gSanguineo;

  @Schema(description = "Raça ou cor autodeclarada para identificação e políticas de pessoal.")
  @UISchema(label = "Raça/cor", group = "Identificação civil", tableHidden = true, order = 206)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.RESTRICTED,
      dataCategory = DomainDataCategory.SENSITIVE_PERSONAL,
      reason = "Raça ou cor autodeclarada para identificação e políticas de pessoal.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private String racaCor;

  @Schema(description = "Estado civil declarado pela pessoa.")
  @UISchema(label = "Estado civil", group = "Identificação civil", tableHidden = true, order = 207)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Estado civil declarado pela pessoa.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
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

  @Schema(description = "Grau de instrução concluído pela pessoa.")
  @UISchema(
      label = "Escolaridade",
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
  private String escolaridade;

  @Schema(description = "Código da nacionalidade no domínio cadastral Ergon.")
  @UISchema(label = "Nacionalidade", group = "Identificação civil", tableHidden = true, order = 210)
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
  private String mae;

  @Schema(description = "Número do documento de identidade civil, preservando zeros e letras.")
  @UISchema(
      label = "Número do RG",
      group = "Documentação civil e trabalhista",
      tableHidden = true,
      order = 216)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Número do documento de identidade civil, preservando zeros e letras.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
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
  private String ufrg;

  @Schema(description = "Data de expedição do documento de identidade civil.")
  @UISchema(
      label = "Expedição do RG",
      group = "Documentação civil e trabalhista",
      tableHidden = true,
      order = 220,
      controlType = org.praxisplatform.uischema.FieldControlType.DATE_PICKER,
      type = org.praxisplatform.uischema.FieldDataType.DATE,
      icon = "calendar_today")
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
      icon = "calendar_today")
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
  private String catcnh;

  @Schema(description = "Data limite de validade da habilitação.")
  @UISchema(
      label = "Validade da CNH",
      group = "Documentação civil e trabalhista",
      tableHidden = true,
      order = 234,
      controlType = org.praxisplatform.uischema.FieldControlType.DATE_PICKER,
      type = org.praxisplatform.uischema.FieldDataType.DATE,
      icon = "calendar_today")
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
  private String tipoidprof;

  @Schema(description = "Identidade do documento legal relacionado ao cadastro civil.")
  @UISchema(
      label = "Documento legal",
      group = "Documentação civil e trabalhista",
      tableHidden = true,
      order = 238)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Identidade do documento legal relacionado ao cadastro civil.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
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
  private String bairroender;

  @Schema(description = "Município de residência da pessoa.")
  @UISchema(
      label = "Município",
      group = "Endereço e contato",
      tableHidden = true,
      order = 244,
      quickViewVisible = true,
      quickViewOrder = 4)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Município de residência da pessoa.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private String cidadeender;

  @Schema(description = "UF de residência da pessoa.")
  @UISchema(
      label = "UF de residência",
      group = "Endereço e contato",
      tableHidden = true,
      order = 245,
      quickViewVisible = true,
      quickViewOrder = 5)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "UF de residência da pessoa.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
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
  private String ramal;

  @Schema(description = "Código da instituição bancária padrão para pagamento ao servidor.")
  @UISchema(label = "Banco", group = "Dados bancários padrão", tableHidden = true, order = 248)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.FINANCIAL,
      reason = "Código da instituição bancária padrão para pagamento ao servidor.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private Integer banco;

  @Schema(description = "Agência bancária padrão, incluindo eventual dígito verificador.")
  @UISchema(label = "Agência", group = "Dados bancários padrão", tableHidden = true, order = 249)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.FINANCIAL,
      reason = "Agência bancária padrão, incluindo eventual dígito verificador.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private String agencia;

  @Schema(description = "Conta bancária padrão, preservando zeros e dígito verificador.")
  @UISchema(label = "Conta", group = "Dados bancários padrão", tableHidden = true, order = 250)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.FINANCIAL,
      reason = "Conta bancária padrão, preservando zeros e dígito verificador.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private String conta;

  @Schema(description = "Modalidade de pagamento associada aos dados bancários padrão.")
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
  private String tipopag;

  @Schema(description = "Indica deficiência física registrada no cadastro da pessoa.")
  @UISchema(
      label = "Deficiência física",
      group = "Acessibilidade e PcD",
      tableHidden = true,
      order = 252)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.RESTRICTED,
      dataCategory = DomainDataCategory.SENSITIVE_PERSONAL,
      reason = "Indica deficiência física registrada no cadastro da pessoa.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private Boolean deffisica = false;

  @Schema(description = "Indica deficiência visual registrada no cadastro da pessoa.")
  @UISchema(
      label = "Deficiência visual",
      group = "Acessibilidade e PcD",
      tableHidden = true,
      order = 253)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.RESTRICTED,
      dataCategory = DomainDataCategory.SENSITIVE_PERSONAL,
      reason = "Indica deficiência visual registrada no cadastro da pessoa.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private Boolean defvisual = false;

  @Schema(description = "Indica deficiência auditiva registrada no cadastro da pessoa.")
  @UISchema(
      label = "Deficiência auditiva",
      group = "Acessibilidade e PcD",
      tableHidden = true,
      order = 254)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.RESTRICTED,
      dataCategory = DomainDataCategory.SENSITIVE_PERSONAL,
      reason = "Indica deficiência auditiva registrada no cadastro da pessoa.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private Boolean defauditiva = false;

  @Schema(description = "Indica deficiência mental registrada no cadastro da pessoa.")
  @UISchema(
      label = "Deficiência mental",
      group = "Acessibilidade e PcD",
      tableHidden = true,
      order = 255)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.RESTRICTED,
      dataCategory = DomainDataCategory.SENSITIVE_PERSONAL,
      reason = "Indica deficiência mental registrada no cadastro da pessoa.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private Boolean defmental = false;

  @Schema(description = "Indica deficiência intelectual registrada no cadastro da pessoa.")
  @UISchema(
      label = "Deficiência intelectual",
      group = "Acessibilidade e PcD",
      tableHidden = true,
      order = 256)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.RESTRICTED,
      dataCategory = DomainDataCategory.SENSITIVE_PERSONAL,
      reason = "Indica deficiência intelectual registrada no cadastro da pessoa.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private Boolean defintelectual = false;

  @Schema(description = "Informações complementares sobre acessibilidade e deficiência da pessoa.")
  @UISchema(
      label = "Observações de acessibilidade",
      group = "Acessibilidade e PcD",
      tableHidden = true,
      order = 257)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.RESTRICTED,
      dataCategory = DomainDataCategory.SENSITIVE_PERSONAL,
      reason = "Informações complementares sobre acessibilidade e deficiência da pessoa.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private String obsDeficiencia;

  @Schema(description = "Data da última conferência cadastral registrada pelo órgão.")
  @UISchema(
      label = "Último recadastramento",
      group = "Recadastramento",
      tableHidden = true,
      order = 258,
      controlType = org.praxisplatform.uischema.FieldControlType.DATE_PICKER,
      type = org.praxisplatform.uischema.FieldDataType.DATE,
      icon = "calendar_today")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Data da última conferência cadastral registrada pelo órgão.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private LocalDate dtRecadast;
}
