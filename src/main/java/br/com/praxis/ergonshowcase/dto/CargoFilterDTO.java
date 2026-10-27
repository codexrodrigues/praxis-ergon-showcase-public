package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.util.*;
import lombok.Getter;
import lombok.Setter;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.customfields.CustomFields;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.filter.annotation.Filterable;
import org.praxisplatform.uischema.filter.annotation.Filterable.FilterOperation;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;

@Getter
@Setter
@Schema(description = "Critérios de pesquisa de cargos na administração de pessoal.")
public class CargoFilterDTO implements GenericFilterDTO {
  @Schema(description = "Trecho da nomenclatura do cargo ou função.")
  @UISchema(label = "Nome do cargo", group = "Pesquisa", order = 10)
  @Filterable(operation = FilterOperation.LIKE)
  private String nome;

  @Schema(description = "Órgão proprietário do quadro de cargos a consultar.")
  @UISchema(
      label = "Órgão",
      controlType = FieldControlType.ENTITY_LOOKUP,
      endpoint = ApiPaths.ORGAO_OPTIONS,
      valueField = "id",
      displayField = "label",
      group = "Pesquisa",
      order = 20)
  @Filterable(operation = FilterOperation.EQUAL)
  private Integer empCodigo;

  @Schema(description = "Categorias funcionais que devem integrar o resultado da pesquisa.")
  @UISchema(label = "Categorias", group = "Pesquisa", order = 30)
  @Filterable(operation = FilterOperation.IN, relation = "categoria")
  private List<String> categorias;

  @CustomFields(resourceKey = "administracao-pessoal.cargos")
  @Schema(
      description = "Predicados sobre características adicionais dos cargos definidas pelo órgão.")
  @UISchema(label = "Características adicionais", group = "Pesquisa", order = 40)
  private Map<String, Object> customFields;
}
