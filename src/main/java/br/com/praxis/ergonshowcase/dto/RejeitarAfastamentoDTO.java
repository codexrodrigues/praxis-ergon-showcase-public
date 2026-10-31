package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.extension.annotation.UISchema;

/** Comando de indeferimento/rejeição de solicitação de licença ou afastamento pendente. */
public record RejeitarAfastamentoDTO(
    @NotBlank
        @Size(max = 2000)
        @Schema(
            description =
                "Fundamentação administrativa ou parecer pericial que motiva o indeferimento da licença.")
        @DomainGovernance(
            kind = DomainGovernanceKind.PRIVACY,
            classification = DomainClassification.RESTRICTED,
            dataCategory = DomainDataCategory.SENSITIVE_PERSONAL,
            reason = "Parecer de indeferimento pode conter avaliação clínica pericial.",
            aiUsage =
                @AiUsagePolicy(
                    visibility = AiVisibilityMode.MASK,
                    trainingUse = AiTrainingUseMode.DENY))
        @UISchema(
            label = "Parecer de indeferimento",
            helpText = "Razões administrativas ou técnicas para rejeição do pedido.",
            group = "Indeferimento",
            order = 10,
            icon = "gavel")
        String motivoRejeicao,
    @Positive
        @Schema(
            nullable = true,
            description = "Despacho ou documento legal que formaliza o indeferimento.")
        @UISchema(
            label = "Despacho decisório",
            helpText = "Documento legal que registra o indeferimento.",
            group = "Indeferimento",
            order = 20,
            type = FieldDataType.NUMBER,
            controlType = FieldControlType.ENTITY_LOOKUP,
            icon = "description",
            endpoint = ApiPaths.DOCUMENTO_OPTIONS,
            valueField = "id",
            displayField = "label")
        Long documentoLegalId) {}
