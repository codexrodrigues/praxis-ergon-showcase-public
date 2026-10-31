package br.com.praxis.ergonshowcase.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    description =
        "Comando de abertura e registro de período aquisitivo de férias de vínculo funcional.")
public class CreatePeriodoAquisitivoFeriasDTO extends PeriodoAquisitivoFeriasDados {}
