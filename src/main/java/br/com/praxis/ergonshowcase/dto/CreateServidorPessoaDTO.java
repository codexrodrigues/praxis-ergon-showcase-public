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
@Schema(description = "Inclusão de pessoas servidoras no cadastro de administração de pessoal.")
public class CreateServidorPessoaDTO extends ServidorPessoaDados {
  @NotNull
  @Positive
  @Schema(
      description =
          "Número funcional imutável do cadastro civil, compartilhado pelos vínculos da pessoa.")
  @UISchema(
      label = "Código",
      preset = UISchemaPreset.ENTERPRISE_ID,
      group = "Identificação civil",
      order = 1,
      icon = "tag")
  private Long numero;
}
