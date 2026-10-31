package br.com.praxis.ergonshowcase.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    description =
        "Comando de criação de matriz estatutária de parâmetros de contagem de férias, abono, prescrição e licença especial.")
public class CreateParametroContagemFeriasDTO extends ParametroContagemFeriasDados {}
