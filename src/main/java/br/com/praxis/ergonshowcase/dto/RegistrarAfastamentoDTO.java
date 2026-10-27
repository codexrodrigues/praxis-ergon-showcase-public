package br.com.praxis.ergonshowcase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.extension.annotation.UISchema;

/** Concessão administrativa de afastamento com código de frequência elegível e período. */
public record RegistrarAfastamentoDTO(
    @NotBlank
        @Size(max = 20)
        @Schema(description = "Tipo de frequência do domínio de licenças e afastamentos.")
        @UISchema(label = "Tipo de frequência", group = "Modalidade")
        String tipofreq,
    @NotNull
        @PositiveOrZero
        @Schema(
            description =
                "Código específico habilitado para afastamento no catálogo de frequência.")
        @UISchema(label = "Código de frequência", group = "Modalidade")
        Integer codfreq,
    @NotNull
        @PastOrPresent
        @Schema(description = "Primeiro dia do afastamento; não pode anteceder o exercício.")
        @UISchema(
            type = org.praxisplatform.uischema.FieldDataType.DATE,
            controlType = org.praxisplatform.uischema.FieldControlType.DATE_PICKER,
            icon = "calendar_today",
            label = "Início",
            group = "Período")
        LocalDate dtini,
    @Schema(
            description =
                "Último dia previsto da licença; quando preenchido, não pode anteceder o início.")
        @UISchema(
            type = org.praxisplatform.uischema.FieldDataType.DATE,
            controlType = org.praxisplatform.uischema.FieldControlType.DATE_PICKER,
            icon = "calendar_today",
            label = "Término previsto",
            group = "Período")
        LocalDate dtprevfim,
    @NotBlank
        @Size(max = 4000)
        @Schema(
            description =
                "Fundamentação administrativa, clínica ou jurídica da concessão de afastamento.")
        @DomainGovernance(
            kind = DomainGovernanceKind.PRIVACY,
            classification = DomainClassification.RESTRICTED,
            dataCategory = DomainDataCategory.SENSITIVE_PERSONAL,
            reason = "Fundamentação pode conter informações de saúde.",
            aiUsage =
                @AiUsagePolicy(
                    visibility = AiVisibilityMode.MASK,
                    trainingUse = AiTrainingUseMode.DENY))
        @UISchema(label = "Fundamentação", group = "Ato")
        String motivo,
    @NotNull
        @Positive
        @Schema(description = "Documento legal do mesmo órgão que fundamenta a concessão.")
        @UISchema(label = "Documento legal", group = "Ato")
        Long documentoLegalId) {}
