package br.com.praxis.ergonshowcase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.extension.annotation.UISchema;

/** Manutenção especializada; propriedades ausentes preservam o valor persistido. */
@Getter
public class AtualizarDadosBancariosContatoDTO extends org.praxisplatform.uischema.dto.PartialUpdateInput {
  @Schema(nullable = true, description = "Endereço eletrônico para contato com a pessoa.")
  @UISchema(label = "E-mail", group = "Endereço, contato e dados bancários")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Endereço eletrônico para contato com a pessoa.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @Getter(onMethod_ = @com.fasterxml.jackson.annotation.JsonProperty("eMail"))
  @jakarta.validation.constraints.Email
  @jakarta.validation.constraints.Size(max = 120)
  private String eMail;

  @Schema(nullable = true, description = "Telefone de contato administrativo da pessoa.")
  @UISchema(label = "Telefone", group = "Endereço, contato e dados bancários")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Telefone de contato administrativo da pessoa.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @jakarta.validation.constraints.Size(max = 20)
  private String telefone;

  @Schema(nullable = true, description = "Número de telefone celular da pessoa.")
  @UISchema(label = "Celular", group = "Endereço, contato e dados bancários")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Número de telefone celular da pessoa.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @jakarta.validation.constraints.Size(max = 20)
  private String numtelCelular;

  @Schema(nullable = true, description = "Tipo do logradouro residencial, como rua ou avenida.")
  @UISchema(label = "Tipo de logradouro", group = "Endereço, contato e dados bancários")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Tipo do logradouro residencial, como rua ou avenida.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @jakarta.validation.constraints.Size(max = 15)
  private String tipologender;

  @Schema(nullable = true, description = "Nome do logradouro do endereço residencial.")
  @UISchema(label = "Logradouro", group = "Endereço, contato e dados bancários")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Nome do logradouro do endereço residencial.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @jakarta.validation.constraints.Size(max = 120)
  private String nomelogender;

  @Schema(nullable = true, description = "Número do imóvel residencial, mantido como texto.")
  @UISchema(label = "Número", group = "Endereço, contato e dados bancários")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Número do imóvel residencial, mantido como texto.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @jakarta.validation.constraints.Size(max = 15)
  private String numender;

  @Schema(nullable = true, description = "Complemento do endereço residencial.")
  @UISchema(label = "Complemento", group = "Endereço, contato e dados bancários")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Complemento do endereço residencial.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @jakarta.validation.constraints.Size(max = 80)
  private String complender;

  @Schema(nullable = true, description = "Bairro de residência da pessoa.")
  @UISchema(label = "Bairro", group = "Endereço, contato e dados bancários")
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
  @UISchema(label = "Município", group = "Endereço, contato e dados bancários")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Município de residência da pessoa.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @jakarta.validation.constraints.Size(max = 60)
  private String cidadeender;

  @Schema(nullable = true, description = "UF de residência da pessoa.")
  @UISchema(label = "UF", group = "Endereço, contato e dados bancários")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "UF de residência da pessoa.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @jakarta.validation.constraints.Size(max = 2)
  private String ufender;

  @Schema(nullable = true, description = "Código postal do endereço residencial, preservando zeros.")
  @UISchema(label = "CEP", group = "Endereço, contato e dados bancários")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Código postal do endereço residencial, preservando zeros.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @jakarta.validation.constraints.Size(max = 8)
  private String cepender;

  @Schema(nullable = true, description = "Código da instituição bancária padrão para pagamento ao servidor.")
  @UISchema(label = "Banco", group = "Endereço, contato e dados bancários")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.FINANCIAL,
      reason = "Código da instituição bancária padrão para pagamento ao servidor.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private Integer banco;

  @Schema(nullable = true, description = "Agência bancária padrão, incluindo eventual dígito verificador.")
  @UISchema(label = "Agência", group = "Endereço, contato e dados bancários")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.FINANCIAL,
      reason = "Agência bancária padrão, incluindo eventual dígito verificador.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @jakarta.validation.constraints.Size(max = 10)
  private String agencia;

  @Schema(nullable = true, description = "Conta bancária padrão, preservando zeros e dígito verificador.")
  @UISchema(label = "Conta", group = "Endereço, contato e dados bancários")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.FINANCIAL,
      reason = "Conta bancária padrão, preservando zeros e dígito verificador.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @jakarta.validation.constraints.Size(max = 25)
  private String conta;

  @Schema(nullable = true, description = "Modalidade de pagamento associada aos dados bancários padrão.")
  @UISchema(label = "Tipo de pagamento", group = "Endereço, contato e dados bancários")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.FINANCIAL,
      reason = "Modalidade de pagamento associada aos dados bancários padrão.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @jakarta.validation.constraints.Size(max = 10)
  private String tipopag;

  @com.fasterxml.jackson.annotation.JsonSetter(value = "eMail", nulls = com.fasterxml.jackson.annotation.Nulls.SET)
  public void setEMail(String value) {
    supplied("eMail");
    this.eMail = value;
  }

  @com.fasterxml.jackson.annotation.JsonSetter(value = "telefone", nulls = com.fasterxml.jackson.annotation.Nulls.SET)
  public void setTelefone(String value) {
    supplied("telefone");
    this.telefone = value;
  }

  @com.fasterxml.jackson.annotation.JsonSetter(value = "numtelCelular", nulls = com.fasterxml.jackson.annotation.Nulls.SET)
  public void setNumtelCelular(String value) {
    supplied("numtelCelular");
    this.numtelCelular = value;
  }

  @com.fasterxml.jackson.annotation.JsonSetter(value = "tipologender", nulls = com.fasterxml.jackson.annotation.Nulls.SET)
  public void setTipologender(String value) {
    supplied("tipologender");
    this.tipologender = value;
  }

  @com.fasterxml.jackson.annotation.JsonSetter(value = "nomelogender", nulls = com.fasterxml.jackson.annotation.Nulls.SET)
  public void setNomelogender(String value) {
    supplied("nomelogender");
    this.nomelogender = value;
  }

  @com.fasterxml.jackson.annotation.JsonSetter(value = "numender", nulls = com.fasterxml.jackson.annotation.Nulls.SET)
  public void setNumender(String value) {
    supplied("numender");
    this.numender = value;
  }

  @com.fasterxml.jackson.annotation.JsonSetter(value = "complender", nulls = com.fasterxml.jackson.annotation.Nulls.SET)
  public void setComplender(String value) {
    supplied("complender");
    this.complender = value;
  }

  @com.fasterxml.jackson.annotation.JsonSetter(value = "bairroender", nulls = com.fasterxml.jackson.annotation.Nulls.SET)
  public void setBairroender(String value) {
    supplied("bairroender");
    this.bairroender = value;
  }

  @com.fasterxml.jackson.annotation.JsonSetter(value = "cidadeender", nulls = com.fasterxml.jackson.annotation.Nulls.SET)
  public void setCidadeender(String value) {
    supplied("cidadeender");
    this.cidadeender = value;
  }

  @com.fasterxml.jackson.annotation.JsonSetter(value = "ufender", nulls = com.fasterxml.jackson.annotation.Nulls.SET)
  public void setUfender(String value) {
    supplied("ufender");
    this.ufender = value;
  }

  @com.fasterxml.jackson.annotation.JsonSetter(value = "cepender", nulls = com.fasterxml.jackson.annotation.Nulls.SET)
  public void setCepender(String value) {
    supplied("cepender");
    this.cepender = value;
  }

  @com.fasterxml.jackson.annotation.JsonSetter(value = "banco", nulls = com.fasterxml.jackson.annotation.Nulls.SET)
  public void setBanco(Integer value) {
    supplied("banco");
    this.banco = value;
  }

  @com.fasterxml.jackson.annotation.JsonSetter(value = "agencia", nulls = com.fasterxml.jackson.annotation.Nulls.SET)
  public void setAgencia(String value) {
    supplied("agencia");
    this.agencia = value;
  }

  @com.fasterxml.jackson.annotation.JsonSetter(value = "conta", nulls = com.fasterxml.jackson.annotation.Nulls.SET)
  public void setConta(String value) {
    supplied("conta");
    this.conta = value;
  }

  @com.fasterxml.jackson.annotation.JsonSetter(value = "tipopag", nulls = com.fasterxml.jackson.annotation.Nulls.SET)
  public void setTipopag(String value) {
    supplied("tipopag");
    this.tipopag = value;
  }
}
