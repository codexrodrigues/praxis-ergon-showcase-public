package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.extension.annotation.UISchema;

/** Comando de prorrogação de prazo previsto de licença ou afastamento homologado aberto. */
public record ProrrogarAfastamentoDTO(
    @NotNull
        @Schema(
            description =
                "Nova data prevista para o término do afastamento; deve ser posterior à previsão atual e à data de início.")
        @UISchema(
            label = "Novo término previsto",
            helpText = "Nova data estimada de término após a prorrogação.",
            group = "Prorrogação",
            order = 10,
            type = FieldDataType.DATE,
            controlType = FieldControlType.DATE_PICKER,
            icon = "update")
        LocalDate novaDataPrevisaoFim,
    @NotNull
        @Positive
        @Schema(
            description =
                "Portaria ou laudo pericial que autoriza a prorrogação do período de afastamento.")
        @UISchema(
            label = "Ato autorizador da prorrogação",
            helpText = "Documento legal ou laudo que ampara a extensão do prazo.",
            group = "Prorrogação",
            order = 20,
            type = FieldDataType.NUMBER,
            controlType = FieldControlType.ENTITY_LOOKUP,
            icon = "description",
            endpoint = ApiPaths.DOCUMENTO_OPTIONS,
            valueField = "id",
            displayField = "label")
        Long documentoLegalId,
    @Size(max = 2000)
        @Schema(
            nullable = true,
            description = "Justificativa complementar anexada à fundamentação da prorrogação.")
        @DomainGovernance(
            kind = DomainGovernanceKind.PRIVACY,
            classification = DomainClassification.RESTRICTED,
            dataCategory = DomainDataCategory.SENSITIVE_PERSONAL,
            reason = "Justificativa de prorrogação pode conter informações médicas.",
            aiUsage =
                @AiUsagePolicy(
                    visibility = AiVisibilityMode.MASK,
                    trainingUse = AiTrainingUseMode.DENY))
        @UISchema(
            label = "Justificativa da prorrogação",
            helpText = "Complemento à fundamentação original para o período prorrogado.",
            group = "Prorrogação",
            order = 30,
            icon = "notes")
        String motivoComplementar) {}
