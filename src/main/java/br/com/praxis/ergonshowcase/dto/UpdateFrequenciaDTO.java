package br.com.praxis.ergonshowcase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(
    description =
        "Contrato de atualização cadastral de ocorrência operacional de frequência, falta, abono ou hora extraordinária.")
public class UpdateFrequenciaDTO extends FrequenciaDados {}
