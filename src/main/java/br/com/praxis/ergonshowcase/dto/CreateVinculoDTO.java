package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.*;
import lombok.Getter;
import lombok.Setter;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.extension.annotation.UISchemaPreset;

@Getter
@Setter
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"id"})
@Schema(description = "Inclusão de vínculos no cadastro de administração de pessoal.")
public class CreateVinculoDTO extends VinculoDados {

  @NotNull
  @Positive
  @Schema(
      description =
          "Número funcional da pessoa titular do novo vínculo; integra a identidade e permanece imutável.")
  @UISchema(
      label = "Pessoa servidora",
      controlType = org.praxisplatform.uischema.FieldControlType.ENTITY_LOOKUP,
      endpoint = ApiPaths.PESSOA_OPTIONS,
      valueField = "id",
      displayField = "label",
      group = "Identificação funcional",
      order = 1,
      icon = "person",
      type = org.praxisplatform.uischema.FieldDataType.TEXT)
  @ResourceReference(
      resourceKey = "administracao-pessoal.funcionarios",
      valueField = "numfunc",
      surfaceId = "ficha-cadastral",
      previewSurfaceId = "ficha-cadastral",
      detailPresentation = "drawer")
  private Long numfunc;

  @NotNull
  @Positive
  @Schema(
      description =
          "Sequencial do novo vínculo da pessoa; permite representar acumulação de vínculos distintos.")
  @UISchema(
      label = "Número do vínculo",
      preset = UISchemaPreset.ENTERPRISE_CODE,
      group = "Identificação funcional",
      order = 2,
      icon = "tag")
  private Integer numero;

  @NotNull
  @Schema(
      description =
          "Órgão público de origem do vínculo; deve corresponder ao contexto X-Tenant usado na criação.")
  @UISchema(
      label = "Órgão",
      controlType = org.praxisplatform.uischema.FieldControlType.ENTITY_LOOKUP,
      endpoint = ApiPaths.ORGAO_OPTIONS,
      valueField = "id",
      displayField = "label",
      group = "Lotação",
      order = 3,
      icon = "apartment",
      type = org.praxisplatform.uischema.FieldDataType.TEXT)
  @ResourceReference(
      resourceKey = "administracao-pessoal.empresas",
      valueField = "empCodigo",
      surfaceId = "ficha-institucional",
      previewSurfaceId = "ficha-institucional",
      detailPresentation = "drawer")
  private Integer empCodigo;

  @NotNull
  @Schema(
      description =
          "Data registrada ou programada para início do exercício. Não pode anteceder a posse quando esta foi informada.")
  @UISchema(
      label = "Início de exercício",
      controlType = org.praxisplatform.uischema.FieldControlType.DATE_PICKER,
      group = "Exercício",
      order = 4,
      icon = "event",
      type = org.praxisplatform.uischema.FieldDataType.DATE)
  private LocalDate dtexerc;
}
