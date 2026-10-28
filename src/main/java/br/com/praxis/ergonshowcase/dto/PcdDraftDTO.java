package br.com.praxis.ergonshowcase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.praxisplatform.uischema.annotation.*;

/** Indicadores do draft; a avaliação não lê nem persiste prontuário de pessoa. */
@Schema(
    description = "Indicadores específicos de deficiência apresentados no formulário cadastral.")
public record PcdDraftDTO(
    @DomainGovernance(
            kind = DomainGovernanceKind.PRIVACY,
            classification = DomainClassification.RESTRICTED,
            dataCategory = DomainDataCategory.SENSITIVE_PERSONAL,
            reason = "Indicador de deficiência em formulário não persistido.",
            aiUsage =
                @AiUsagePolicy(
                    visibility = AiVisibilityMode.MASK,
                    trainingUse = AiTrainingUseMode.DENY))
        @Schema(description = "Indica deficiência física declarada no draft.")
        @org.praxisplatform.uischema.extension.annotation.UISchema(
            label = "Deficiência física",
            group = "Acessibilidade",
            type = org.praxisplatform.uischema.FieldDataType.BOOLEAN,
            controlType = org.praxisplatform.uischema.FieldControlType.CHECKBOX,
            icon = "accessible")
        Boolean deffisica,
    @DomainGovernance(
            kind = DomainGovernanceKind.PRIVACY,
            classification = DomainClassification.RESTRICTED,
            dataCategory = DomainDataCategory.SENSITIVE_PERSONAL,
            reason = "Indicador de deficiência em formulário não persistido.",
            aiUsage =
                @AiUsagePolicy(
                    visibility = AiVisibilityMode.MASK,
                    trainingUse = AiTrainingUseMode.DENY))
        @Schema(description = "Indica deficiência visual declarada no draft.")
        @org.praxisplatform.uischema.extension.annotation.UISchema(
            label = "Deficiência visual",
            group = "Acessibilidade",
            type = org.praxisplatform.uischema.FieldDataType.BOOLEAN,
            controlType = org.praxisplatform.uischema.FieldControlType.CHECKBOX,
            icon = "accessible")
        Boolean defvisual,
    @DomainGovernance(
            kind = DomainGovernanceKind.PRIVACY,
            classification = DomainClassification.RESTRICTED,
            dataCategory = DomainDataCategory.SENSITIVE_PERSONAL,
            reason = "Indicador de deficiência em formulário não persistido.",
            aiUsage =
                @AiUsagePolicy(
                    visibility = AiVisibilityMode.MASK,
                    trainingUse = AiTrainingUseMode.DENY))
        @Schema(description = "Indica deficiência auditiva declarada no draft.")
        @org.praxisplatform.uischema.extension.annotation.UISchema(
            label = "Deficiência auditiva",
            group = "Acessibilidade",
            type = org.praxisplatform.uischema.FieldDataType.BOOLEAN,
            controlType = org.praxisplatform.uischema.FieldControlType.CHECKBOX,
            icon = "accessible")
        Boolean defauditiva,
    @DomainGovernance(
            kind = DomainGovernanceKind.PRIVACY,
            classification = DomainClassification.RESTRICTED,
            dataCategory = DomainDataCategory.SENSITIVE_PERSONAL,
            reason = "Indicador de deficiência em formulário não persistido.",
            aiUsage =
                @AiUsagePolicy(
                    visibility = AiVisibilityMode.MASK,
                    trainingUse = AiTrainingUseMode.DENY))
        @Schema(description = "Indica deficiência mental declarada no draft.")
        @org.praxisplatform.uischema.extension.annotation.UISchema(
            label = "Deficiência mental",
            group = "Acessibilidade",
            type = org.praxisplatform.uischema.FieldDataType.BOOLEAN,
            controlType = org.praxisplatform.uischema.FieldControlType.CHECKBOX,
            icon = "accessible")
        Boolean defmental,
    @DomainGovernance(
            kind = DomainGovernanceKind.PRIVACY,
            classification = DomainClassification.RESTRICTED,
            dataCategory = DomainDataCategory.SENSITIVE_PERSONAL,
            reason = "Indicador de deficiência em formulário não persistido.",
            aiUsage =
                @AiUsagePolicy(
                    visibility = AiVisibilityMode.MASK,
                    trainingUse = AiTrainingUseMode.DENY))
        @Schema(description = "Indica deficiência intelectual declarada no draft.")
        @org.praxisplatform.uischema.extension.annotation.UISchema(
            label = "Deficiência intelectual",
            group = "Acessibilidade",
            type = org.praxisplatform.uischema.FieldDataType.BOOLEAN,
            controlType = org.praxisplatform.uischema.FieldControlType.CHECKBOX,
            icon = "accessible")
        Boolean defintelectual) {}
