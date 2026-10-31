package br.com.praxis.ergonshowcase.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    description =
        "Comando de programação e registro de parcela de gozo de férias e abono pecuniário.")
public class CreateFeriasGozoDTO extends FeriasGozoDados {}
