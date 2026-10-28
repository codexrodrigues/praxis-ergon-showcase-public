package br.com.praxis.ergonshowcase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;

/** Identidade selecionada no draft; ausência limpa a prévia, sem reservar vaga. */
@Schema(description = "Seleção de cargo para consulta não persistente de fatos do quadro.")
public record CargoSelectionDTO(
    @Positive
        @Schema(
            description = "Cargo selecionado; deve existir no órgão do contexto de catálogo.",
            nullable = true)
        @org.praxisplatform.uischema.extension.annotation.UISchema(
            label = "Cargo selecionado",
            group = "Quadro de pessoal",
            type = org.praxisplatform.uischema.FieldDataType.TEXT,
            controlType = org.praxisplatform.uischema.FieldControlType.ENTITY_LOOKUP,
            icon = "badge",
            endpoint = "/api/ergon/cargos/option-sources/cargo/options/filter",
            valueField = "id",
            displayField = "label")
        Integer cargo) {}
