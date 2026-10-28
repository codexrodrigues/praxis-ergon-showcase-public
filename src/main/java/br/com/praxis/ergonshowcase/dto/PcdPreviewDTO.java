package br.com.praxis.ergonshowcase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.praxisplatform.uischema.annotation.*;

/** Fato de acessibilidade derivado, sem efeito sobre o cadastro persistido. */
@Schema(description = "Prévia determinística do indicador de deficiência do formulário.")
public record PcdPreviewDTO(
    @Schema(
            description =
                "Verdadeiro quando ao menos um dos cinco indicadores específicos está marcado.")
        @DomainGovernance(
            kind = DomainGovernanceKind.PRIVACY,
            classification = DomainClassification.RESTRICTED,
            dataCategory = DomainDataCategory.SENSITIVE_PERSONAL,
            reason = "Indicador derivado de deficiência.",
            aiUsage =
                @AiUsagePolicy(
                    visibility = AiVisibilityMode.MASK,
                    trainingUse = AiTrainingUseMode.DENY))
        boolean possuiDeficienciaPcD) {}
