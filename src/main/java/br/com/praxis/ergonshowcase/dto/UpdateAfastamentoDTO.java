package br.com.praxis.ergonshowcase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(
    description =
        "Atualização cadastral dos dados operacionais, previsão de término, substituto e atos formais do afastamento, preservando vínculo, modalidade, data inicial e transições de homologação.")
public class UpdateAfastamentoDTO extends AfastamentoDados {}
