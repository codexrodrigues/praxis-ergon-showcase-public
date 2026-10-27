package br.com.praxis.ergonshowcase.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Ação sem parâmetros cadastrais; o estado de destino pertence à operação selecionada. */
@Schema(
    description =
        "Comando institucional sem parâmetros; identidade e estado de destino pertencem à rota.")
public record AlterarSituacaoOrgaoDTO() {}
