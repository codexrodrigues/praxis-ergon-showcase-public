package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

@Getter
@Setter
@Schema(
    description =
        "Contrato de abertura de nova ocorrência operacional de frequência, falta, abono ou hora extraordinária.")
public class CreateFrequenciaDTO extends FrequenciaDados {

  @NotNull
  @Schema(description = "Código do órgão público de lotação do vínculo no momento da ocorrência.")
  @UISchema(
      label = "Órgão",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "apartment",
      group = "Vínculo Funcional",
      order = 10,
      required = true,
      endpoint = ApiPaths.ORGAO_OPTIONS,
      valueField = "id",
      displayField = "label",
      helpText = "Selecione o órgão público de lotação do vínculo.")
  private Integer empCodigo;

  @NotNull
  @Schema(description = "Número funcional do servidor titular da ocorrência de frequência.")
  @UISchema(
      label = "Servidor",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "person",
      group = "Vínculo Funcional",
      order = 20,
      required = true,
      endpoint = ApiPaths.PESSOA_OPTIONS,
      valueField = "id",
      displayField = "label",
      helpText = "Selecione o servidor titular do vínculo funcional.")
  private Long numfunc;

  @NotNull
  @Schema(description = "Número sequencial do vínculo funcional do servidor.")
  @UISchema(
      label = "Vínculo funcional",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "badge",
      group = "Vínculo Funcional",
      order = 30,
      required = true,
      endpoint = ApiPaths.VINCULO_OPTIONS,
      valueField = "id",
      displayField = "label",
      helpText = "Selecione o vínculo funcional associado à ocorrência.")
  private Integer numvinc;
}
