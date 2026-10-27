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
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"id"})
@Schema(description = "Inclusão de órgãos no cadastro de administração de pessoal.")
public class CreateOrgaoDTO extends OrgaoDados {
  @NotNull
  @Schema(description = "Estado inicial de habilitação do órgão para novas admissões.")
  @UISchema(label = "Órgão inicialmente ativo", group = "Situação")
  private Boolean ativa = true;

  @NotNull
  @Positive
  @Schema(description = "Código imutável do órgão público no cadastro multi-órgão do Ergon.")
  @UISchema(
      label = "Código",
      preset = UISchemaPreset.ENTERPRISE_ID,
      group = "Identificação",
      order = 1,
      icon = "tag")
  private Integer empresa;
}
