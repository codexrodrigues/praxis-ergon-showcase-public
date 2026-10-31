package br.com.praxis.ergonshowcase.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    description =
        "Resultado determinístico da avaliação reativa de modalidade, idade e elegibilidade a IRRF do dependente.")
public record DependenteElegibilidadePreviewDTO(
    @Schema(description = "Descrição oficial da modalidade de dependência selecionada.")
        String tipoDependenteDescricaoPrevista,
    @Schema(description = "Idade atual do dependente em anos completos calculada a partir de dtnasc.")
        Integer idadeCalculadaAnos,
    @Schema(
            description =
                "Verdadeiro quando a modalidade habilita IRRF e o dependente atende ao limite etário ou invalidez.")
        Boolean elegivelIrrfPrevisto) {}
