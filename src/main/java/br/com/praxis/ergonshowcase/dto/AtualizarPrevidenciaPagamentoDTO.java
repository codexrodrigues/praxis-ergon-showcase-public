package br.com.praxis.ergonshowcase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.extension.annotation.UISchema;

/** Manutenção especializada; propriedades ausentes preservam o valor persistido. */
@Getter
@Setter
public class AtualizarPrevidenciaPagamentoDTO {
  @Schema(description = "Regime previdenciário ao qual o vínculo está filiado.")
  @UISchema(label = "Regime previdenciário", group = "Previdência e pagamento")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Regime previdenciário ao qual o vínculo está filiado.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private String regimeprev;

  @Schema(description = "Plano previdenciário de enquadramento do vínculo.")
  @UISchema(label = "Plano previdenciário", group = "Previdência e pagamento")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Plano previdenciário de enquadramento do vínculo.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
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

  @Schema(description = "Banco destinatário do pagamento deste vínculo.")
  @UISchema(label = "Banco", group = "Previdência e pagamento")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.FINANCIAL,
      reason = "Banco destinatário do pagamento deste vínculo.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private Integer banco;

  @Schema(description = "Agência bancária para pagamento deste vínculo.")
  @UISchema(label = "Agência", group = "Previdência e pagamento")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.FINANCIAL,
      reason = "Agência bancária para pagamento deste vínculo.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private String agencia;

  @Schema(description = "Conta bancária para pagamento deste vínculo.")
  @UISchema(label = "Conta", group = "Previdência e pagamento")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.FINANCIAL,
      reason = "Conta bancária para pagamento deste vínculo.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private String conta;

  @Schema(description = "Modalidade de pagamento deste vínculo.")
  @UISchema(label = "Tipo de pagamento", group = "Previdência e pagamento")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.FINANCIAL,
      reason = "Modalidade de pagamento deste vínculo.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private String tipopag;
}
