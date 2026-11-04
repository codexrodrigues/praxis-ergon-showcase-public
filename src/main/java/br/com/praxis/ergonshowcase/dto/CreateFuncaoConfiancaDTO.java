package br.com.praxis.ergonshowcase.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Inclusão de registro no recurso funções de confiança. Catálogo por órgão de funções de direção, chefia e assessoramento, com vigência, gratificação e política de vagas e substituição.")
public class CreateFuncaoConfiancaDTO extends FuncaoConfiancaDados {
}
