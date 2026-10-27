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
      icon = "cake")
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
}
