package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.util.*;
import lombok.Getter;
import lombok.Setter;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.extension.annotation.UISchemaPreset;

@Getter
@Setter
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"id"})
@Schema(description = "Inclusão de cargos no cadastro de administração de pessoal.")
public class CreateCargoDTO extends CargoDados {
  @NotNull
  @Positive
  @Schema(description = "Código imutável que identifica o cargo ou função no quadro de pessoal.")
  @UISchema(
      label = "Código",
      preset = UISchemaPreset.ENTERPRISE_ID,
      group = "Identificação",
      order = 1,
      icon = "tag")
  private Integer cargo;

  @NotNull
  @Schema(
      description =
          "Órgão proprietário do quadro ao qual o cargo pertence; permanece fixo após a inclusão.")
  @UISchema(
      label = "Órgão",
      controlType = FieldControlType.ENTITY_LOOKUP,
      endpoint = ApiPaths.ORGAO_OPTIONS,
      valueField = "id",
      displayField = "label",
      group = "Identificação",
      order = 5,
      icon = "apartment")
  private Integer empCodigo;
}
