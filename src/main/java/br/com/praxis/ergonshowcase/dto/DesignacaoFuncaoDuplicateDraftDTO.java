package br.com.praxis.ergonshowcase.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Sugestões para a inclusão de um novo cadastro, sujeitas às validações da criação.")
public record DesignacaoFuncaoDuplicateDraftDTO(
    @Schema(description = "Função de confiança sugerida para o novo ato de designação.") Long funcaoId) {}
