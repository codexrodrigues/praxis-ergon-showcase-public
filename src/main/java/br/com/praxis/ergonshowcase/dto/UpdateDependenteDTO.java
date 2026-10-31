package br.com.praxis.ergonshowcase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(
    description =
        "Atualização cadastral dos dados civis, documentais e de elegibilidade do dependente, preservando a identidade natural composta e as datas de ciclo de vida.")
public class UpdateDependenteDTO extends DependenteDados {}
