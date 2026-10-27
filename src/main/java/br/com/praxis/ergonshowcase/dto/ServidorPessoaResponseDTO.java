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
        "Cadastro civil único das pessoas titulares de vínculos funcionais com órgãos públicos.")
@ResourceIdentity(keyField = "id", titleField = "nome", displayLabelField = "nome")
public class ServidorPessoaResponseDTO extends ServidorPessoaDados {
  @Schema(
      description =
          "Número funcional imutável do cadastro civil, compartilhado pelos vínculos da pessoa.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Identificação",
      preset = UISchemaPreset.ENTERPRISE_ID,
      readOnly = true,
      formHidden = true,
      group = "Identificação",
      order = 0)
  private Long id;
}
