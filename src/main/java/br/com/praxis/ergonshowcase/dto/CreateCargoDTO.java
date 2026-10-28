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
      icon = "badge",
      type = org.praxisplatform.uischema.FieldDataType.TEXT,
      controlType = org.praxisplatform.uischema.FieldControlType.ENTITY_LOOKUP,
      endpoint = "/api/ergon/cargos/option-sources/cargo/options/filter",
      valueField = "id",
      displayField = "label")
  @ResourceReference(
      resourceKey = "administracao-pessoal.cargos",
      valueField = "cargo",
      surfaceId = "ficha-cargo",
      previewSurfaceId = "ficha-cargo",
      detailPresentation = "drawer")
  private Integer cargo;

  @NotNull
  @Schema(
      description =
          "Órgão proprietário do quadro ao qual o cargo pertence; permanece fixo após a inclusão.")
  @UISchema(
      label = "Órgão",
      controlType = org.praxisplatform.uischema.FieldControlType.ENTITY_LOOKUP,
      endpoint = "/api/ergon/orgaos/option-sources/orgao/options/filter",
      valueField = "id",
      displayField = "label",
      group = "Identificação",
      order = 5,
      icon = "apartment",
      type = org.praxisplatform.uischema.FieldDataType.TEXT)
  @ResourceReference(
      resourceKey = "administracao-pessoal.empresas",
      valueField = "empCodigo",
      surfaceId = "ficha-institucional",
      previewSurfaceId = "ficha-institucional",
      detailPresentation = "drawer")
  private Integer empCodigo;
}
