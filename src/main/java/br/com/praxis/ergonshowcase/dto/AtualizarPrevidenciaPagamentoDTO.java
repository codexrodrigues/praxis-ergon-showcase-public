package br.com.praxis.ergonshowcase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.extension.annotation.UISchema;

/** Manutenção especializada; propriedades ausentes preservam o valor persistido. */
@Getter
public class AtualizarPrevidenciaPagamentoDTO extends org.praxisplatform.uischema.dto.PartialUpdateInput {
  @Schema(nullable = true, description = "Regime previdenciário ao qual o vínculo está filiado.")
  @UISchema(label = "Regime previdenciário", group = "Previdência e pagamento")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Regime previdenciário ao qual o vínculo está filiado.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @jakarta.validation.constraints.Size(max = 30)
  private String regimeprev;

  @Schema(nullable = true, description = "Plano previdenciário de enquadramento do vínculo.")
  @UISchema(label = "Plano previdenciário", group = "Previdência e pagamento")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Plano previdenciário de enquadramento do vínculo.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @jakarta.validation.constraints.Size(max = 40)
  private String planoprev;

  @Schema(description = "Indica incidência do desconto de imposto de renda na folha do vínculo.")
  @UISchema(label = "Descontar IR", group = "Previdência e pagamento")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Indica incidência do desconto de imposto de renda na folha do vínculo.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private Boolean descontaIr;

  @Schema(description = "Indica opção de envio de comunicação relativa ao vínculo.")
  @UISchema(label = "Comunicação por correio", group = "Previdência e pagamento")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Indica opção de envio de comunicação relativa ao vínculo.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private Boolean correio;

  @Schema(nullable = true, description = "Banco destinatário do pagamento deste vínculo.")
  @UISchema(
      label = "Banco",
      group = "Previdência e pagamento",
      visibleForAuthorities = {"ERGON_READ", "ERGON_WRITE", "ERGON_FINANCIAL_READ"},
      editableForAuthorities = {"ERGON_WRITE"},
      fieldAccessReason = "Dado bancário de pagamento do vínculo governado para perfis autorizados.")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.FINANCIAL,
      complianceTags = {"LGPD", "FINANCIAL_DATA", "NON_EXPORTABLE"},
      reason = "Banco destinatário do pagamento deste vínculo.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private Integer banco;

  @Schema(nullable = true, description = "Agência bancária para pagamento deste vínculo.")
  @UISchema(
      label = "Agência",
      group = "Previdência e pagamento",
      visibleForAuthorities = {"ERGON_READ", "ERGON_WRITE", "ERGON_FINANCIAL_READ"},
      editableForAuthorities = {"ERGON_WRITE"},
      fieldAccessReason = "Agência bancária de pagamento do vínculo governada para perfis autorizados.")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.FINANCIAL,
      complianceTags = {"LGPD", "FINANCIAL_DATA", "NON_EXPORTABLE"},
      reason = "Agência bancária para pagamento deste vínculo.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @jakarta.validation.constraints.Size(max = 10)
  private String agencia;

  @Schema(nullable = true, description = "Conta bancária para pagamento deste vínculo.")
  @UISchema(
      label = "Conta",
      group = "Previdência e pagamento",
      visibleForAuthorities = {"ERGON_READ", "ERGON_WRITE", "ERGON_FINANCIAL_READ"},
      editableForAuthorities = {"ERGON_WRITE"},
      fieldAccessReason = "Conta bancária de pagamento do vínculo protegida contra exportação em massa.")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.FINANCIAL,
      complianceTags = {"LGPD", "FINANCIAL_DATA", "NON_EXPORTABLE"},
      reason = "Conta bancária para pagamento deste vínculo.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @jakarta.validation.constraints.Size(max = 25)
  private String conta;

  @Schema(nullable = true, description = "Modalidade de pagamento deste vínculo.")
  @UISchema(label = "Tipo de pagamento", group = "Previdência e pagamento")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.FINANCIAL,
      reason = "Modalidade de pagamento deste vínculo.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @jakarta.validation.constraints.Size(max = 10)
  private String tipopag;

  @com.fasterxml.jackson.annotation.JsonSetter(value = "regimeprev", nulls = com.fasterxml.jackson.annotation.Nulls.SET)
  public void setRegimeprev(String value) {
    supplied("regimeprev");
    this.regimeprev = value;
  }

  @com.fasterxml.jackson.annotation.JsonSetter(value = "planoprev", nulls = com.fasterxml.jackson.annotation.Nulls.SET)
  public void setPlanoprev(String value) {
    supplied("planoprev");
    this.planoprev = value;
  }

  @com.fasterxml.jackson.annotation.JsonSetter(value = "descontaIr", nulls = com.fasterxml.jackson.annotation.Nulls.FAIL)
  public void setDescontaIr(Boolean value) {
    supplied("descontaIr");
    this.descontaIr = value;
  }

  @com.fasterxml.jackson.annotation.JsonSetter(value = "correio", nulls = com.fasterxml.jackson.annotation.Nulls.FAIL)
  public void setCorreio(Boolean value) {
    supplied("correio");
    this.correio = value;
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
