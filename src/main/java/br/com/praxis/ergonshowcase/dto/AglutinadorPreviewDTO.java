package br.com.praxis.ergonshowcase.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Projeção do agregador elegível selecionado no draft. */
@Schema(description = "Identificação humana do cargo agregador consultado.")
public record AglutinadorPreviewDTO(
    @Schema(
            description = "Nomenclatura do cargo agregador habilitado do mesmo órgão.",
            nullable = true)
        String nome) {}
