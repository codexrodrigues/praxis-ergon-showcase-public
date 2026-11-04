package br.com.praxis.ergonshowcase.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Sugestões para a inclusão de um novo cadastro, sujeitas às validações da criação.")
public record FuncaoConfiancaDuplicateDraftDTO(
    @Schema(description = "Categoria institucional sugerida para classificar a nova função.") String categoria) {}
