package br.com.praxis.ergonshowcase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.extension.annotation.UISchema;

/** Manutenção especializada; propriedades ausentes preservam o valor persistido. */
@Getter
@Setter
public class AtualizarDadosBancariosContatoDTO {
  @Schema(description = "Endereço eletrônico para contato com a pessoa.")
  @UISchema(label = "E-mail", group = "Endereço, contato e dados bancários")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Endereço eletrônico para contato com a pessoa.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @Getter(onMethod_ = @com.fasterxml.jackson.annotation.JsonProperty("eMail"))
  @Setter(onMethod_ = @com.fasterxml.jackson.annotation.JsonProperty("eMail"))
  @jakarta.validation.constraints.Email
  private String eMail;

  @Schema(description = "Telefone de contato administrativo da pessoa.")
  @UISchema(label = "Telefone", group = "Endereço, contato e dados bancários")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Telefone de contato administrativo da pessoa.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private String telefone;

  @Schema(description = "Número de telefone celular da pessoa.")
  @UISchema(label = "Celular", group = "Endereço, contato e dados bancários")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Número de telefone celular da pessoa.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private String numtelCelular;

  @Schema(description = "Tipo do logradouro residencial, como rua ou avenida.")
  @UISchema(label = "Tipo de logradouro", group = "Endereço, contato e dados bancários")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Tipo do logradouro residencial, como rua ou avenida.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private String tipologender;

  @Schema(description = "Nome do logradouro do endereço residencial.")
  @UISchema(label = "Logradouro", group = "Endereço, contato e dados bancários")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Nome do logradouro do endereço residencial.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private String nomelogender;

  @Schema(description = "Número do imóvel residencial, mantido como texto.")
  @UISchema(label = "Número", group = "Endereço, contato e dados bancários")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Número do imóvel residencial, mantido como texto.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private String numender;

  @Schema(description = "Complemento do endereço residencial.")
  @UISchema(label = "Complemento", group = "Endereço, contato e dados bancários")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Complemento do endereço residencial.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private String complender;

  @Schema(description = "Bairro de residência da pessoa.")
  @UISchema(label = "Bairro", group = "Endereço, contato e dados bancários")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Bairro de residência da pessoa.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private String bairroender;

  @Schema(description = "Município de residência da pessoa.")
  @UISchema(label = "Município", group = "Endereço, contato e dados bancários")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Município de residência da pessoa.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private String cidadeender;

  @Schema(description = "UF de residência da pessoa.")
  @UISchema(label = "UF", group = "Endereço, contato e dados bancários")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "UF de residência da pessoa.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private String ufender;

  @Schema(description = "Código postal do endereço residencial, preservando zeros.")
  @UISchema(label = "CEP", group = "Endereço, contato e dados bancários")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Código postal do endereço residencial, preservando zeros.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private String cepender;

  @Schema(description = "Código da instituição bancária padrão para pagamento ao servidor.")
  @UISchema(label = "Banco", group = "Endereço, contato e dados bancários")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.FINANCIAL,
      reason = "Código da instituição bancária padrão para pagamento ao servidor.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private Integer banco;

  @Schema(description = "Agência bancária padrão, incluindo eventual dígito verificador.")
  @UISchema(label = "Agência", group = "Endereço, contato e dados bancários")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.FINANCIAL,
      reason = "Agência bancária padrão, incluindo eventual dígito verificador.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private String agencia;

  @Schema(description = "Conta bancária padrão, preservando zeros e dígito verificador.")
  @UISchema(label = "Conta", group = "Endereço, contato e dados bancários")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.FINANCIAL,
      reason = "Conta bancária padrão, preservando zeros e dígito verificador.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private String conta;

  @Schema(description = "Modalidade de pagamento associada aos dados bancários padrão.")
  @UISchema(label = "Tipo de pagamento", group = "Endereço, contato e dados bancários")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.FINANCIAL,
      reason = "Modalidade de pagamento associada aos dados bancários padrão.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private String tipopag;
}
