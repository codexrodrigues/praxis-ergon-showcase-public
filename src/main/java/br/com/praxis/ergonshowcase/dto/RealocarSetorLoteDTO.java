package br.com.praxis.ergonshowcase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import org.praxisplatform.uischema.*;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.annotation.*;

/** Parâmetros da portaria coletiva; seleção e versões pertencem ao protocolo canônico. */
@Schema(description = "Portaria de realocação de vínculos ativos ou cedidos entre unidades do mesmo órgão.")
public record RealocarSetorLoteDTO(
    @NotBlank @Size(max=30)
    @Schema(description="Unidade de lotação de destino, existente no órgão de cada vínculo selecionado.")
    @UISchema(label="Lotação de destino", group="Destino", type=FieldDataType.TEXT, controlType=FieldControlType.ENTITY_LOOKUP, icon="account_tree", endpoint="/api/ergon/setores/option-sources/setor/options/filter",valueField="id",displayField="label")
    String setor,
    @Size(max=30)
    @Schema(description="Unidade de exercício de destino no mesmo órgão; quando ausente, preserva o exercício atual.")
    @UISchema(label="Exercício de destino", group="Destino", type=FieldDataType.TEXT, controlType=FieldControlType.ENTITY_LOOKUP, icon="account_tree", endpoint="/api/ergon/setores/option-sources/setor/options/filter",valueField="id",displayField="label")
    String setorExercicio,
    @NotNull @Positive
    @Schema(description="Portaria ou ato coletivo emitido pelo órgão responsável pelos vínculos; fundamenta a realocação.")
    @UISchema(label="Portaria coletiva", group="Fundamentação", type=FieldDataType.TEXT, controlType=FieldControlType.ENTITY_LOOKUP, icon="gavel", endpoint="/api/ergon/documentos-legais/option-sources/documento/options/filter",valueField="id",displayField="label")
    Long documentoLegalId,
    @Size(max=2000)
    @DomainGovernance(kind=DomainGovernanceKind.PRIVACY, classification=DomainClassification.CONFIDENTIAL,
        dataCategory=DomainDataCategory.PERSONAL,
        reason="Justificativa administrativa de movimentação pode identificar circunstâncias pessoais dos servidores selecionados.",
        aiUsage=@AiUsagePolicy(visibility=AiVisibilityMode.MASK,trainingUse=AiTrainingUseMode.DENY))
    @Schema(description="Justificativa complementar preservada na intenção protegida do ato coletivo; não sobrescreve o motivo de ingresso do vínculo.")
    @UISchema(label="Justificativa",group="Fundamentação",controlType=FieldControlType.TEXTAREA,tableHidden=true)
    String observacao) {}
