package br.com.praxis.ergonshowcase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.util.*;
import lombok.Getter;
import lombok.Setter;
import org.praxisplatform.uischema.annotation.*;

@Getter
@Setter
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"id"})
@Schema(
    description =
        "Manutenção cadastral de cargos, com identidade preservada e versão exigida em If-Match.")
public class UpdateCargoDTO extends CargoDados {
  /** Defaults históricos exclusivos da substituição cadastral; CREATE exige escolhas explícitas. */
  public UpdateCargoDTO() {
    setControleVaga(false);
    setPorReferencia(false);
    setEAglutinador(false);
  }
}
