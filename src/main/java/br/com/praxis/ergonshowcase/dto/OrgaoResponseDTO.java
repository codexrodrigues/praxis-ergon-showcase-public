package br.com.praxis.ergonshowcase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.util.*;
import lombok.Getter;
import lombok.Setter;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.extension.annotation.UISchemaPreset;

@Getter
@Setter
@Schema(
    description =
        "Entes públicos responsáveis pelos quadros funcionais e pela administração de pessoal.")
@ResourceIdentity(keyField = "id", titleField = "nome", displayLabelField = "nome")
public class OrgaoResponseDTO extends OrgaoDados {
  @Schema(
      description = "Código imutável do órgão público no cadastro multi-órgão do Ergon.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Identificação",
      preset = UISchemaPreset.ENTERPRISE_ID,
      readOnly = true,
      formHidden = true,
      group = "Identificação",
      order = 0)
  private Integer id;
}
