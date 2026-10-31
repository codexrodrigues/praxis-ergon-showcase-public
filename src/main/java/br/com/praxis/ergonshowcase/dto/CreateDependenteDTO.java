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
        "Comando de inclusão de dependente vinculado ao servidor público, fixando a chave composta natural (numfunc, numdep) e o início de vigência.")
public class CreateDependenteDTO extends DependenteDados {
  @NotNull
  @Positive
  @Schema(description = "Número funcional do servidor público titular ao qual o dependente pertence.")
  @UISchema(
      label = "Servidor titular",
      group = "Identificação do vínculo familiar",
      order = 1,
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
          "Sequencial numérico do dependente dentro do prontuário funcional do servidor (NUMDEP).")
  @UISchema(
      label = "Sequencial do dependente",
      group = "Identificação do vínculo familiar",
      order = 2,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.INPUT,
      icon = "format_list_numbered")
  private Integer numdep;

  @NotNull
  @Schema(description = "Data de início da vigência da condição de dependência legal ou econômica.")
  @UISchema(
      label = "Início da dependência",
      group = "Vigência da dependência",
      order = 3,
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "event_available")
  private LocalDate dtini;
}
