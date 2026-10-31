package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.praxisplatform.uischema.*;
import org.praxisplatform.uischema.annotation.ResourceReference;
import org.praxisplatform.uischema.extension.annotation.UISchema;

@Getter
@Setter
@Schema(
    description =
        "Comando de abertura de licença ou afastamento funcional, fixando o vínculo titular, a modalidade de frequência e a data inicial do período.")
public class CreateAfastamentoDTO extends AfastamentoDados {
  @NotNull
  @Positive
  @Schema(
      description =
          "Código do órgão público ao qual pertence o vínculo funcional afastado (deve coincidir com X-Tenant).")
  @UISchema(
      label = "Órgão",
      helpText = "Órgão público proprietário do assentamento funcional.",
      group = "Identificação funcional",
      order = 1,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "apartment",
      endpoint = ApiPaths.ORGAO_OPTIONS,
      valueField = "id",
      displayField = "label")
  @ResourceReference(
      resourceKey = "administracao-pessoal.empresas",
      valueField = "empCodigo",
      surfaceId = "ficha-institucional",
      previewSurfaceId = "ficha-institucional",
      detailPresentation = "drawer")
  private Integer empCodigo;

  @NotNull
  @Positive
  @Schema(description = "Número funcional do servidor público afastado (componente 1 do vínculo).")
  @UISchema(
      label = "Servidor afastado",
      helpText = "Pessoa servidora titular do vínculo funcional.",
      group = "Identificação funcional",
      order = 2,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "person",
      endpoint = ApiPaths.PESSOA_OPTIONS,
      valueField = "id",
      displayField = "label")
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
          "Número sequencial do vínculo funcional afastado (componente 2 da FK composta para vínculos).")
  @UISchema(
      label = "Sequencial do vínculo",
      helpText = "Número do vínculo funcional do servidor no órgão.",
      group = "Identificação funcional",
      order = 3,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "badge",
      endpoint = ApiPaths.VINCULO_OPTIONS,
      valueField = "id",
      displayField = "label")
  private Integer numvinc;
}
