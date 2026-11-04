package br.com.praxis.ergonshowcase.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Inclusão de registro no recurso vagas de função. Posições numeradas autorizadas por função, órgão e unidade, com origem e situação de ocupação.")
public class CreateVagaFuncaoDTO extends VagaFuncaoDados {
}
