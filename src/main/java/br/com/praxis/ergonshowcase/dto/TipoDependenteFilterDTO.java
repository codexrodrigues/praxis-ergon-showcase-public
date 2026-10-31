package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.*;
import org.praxisplatform.uischema.*;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.filter.annotation.Filterable;
import org.praxisplatform.uischema.filter.annotation.Filterable.FilterOperation;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;

@Getter
@Setter
@Schema(
    description =
        "Critérios de pesquisa do catálogo de tipos de dependente para consulta de regras de IRRF, salário-família e limites etários.")
public class TipoDependenteFilterDTO implements GenericFilterDTO {
  @Schema(description = "Busca textual ampla por código ou descrição da modalidade de dependência.")
  @UISchema(label = "Busca rápida", group = "Pesquisa", order = 5, icon = "search")
  @Filterable(
      operation = FilterOperation.LIKE,
      relations = {"tipodepen", "descricao"})
  private String buscaAmpla;

  @Schema(description = "Trecho da descrição oficial da modalidade de dependência.")
  @UISchema(label = "Descrição", group = "Pesquisa", order = 10)
  @Filterable(operation = FilterOperation.LIKE, relation = "descricao")
  private String descricao;

  @Schema(description = "Prefixo do código identificador do tipo de dependência.")
  @UISchema(label = "Código", group = "Pesquisa", order = 20)
  @Filterable(operation = FilterOperation.STARTS_WITH, relation = "tipodepen")
  private String tipodepen;

  @Schema(description = "Restringe às modalidades elegíveis para dedução de IRRF.")
  @UISchema(
      label = "Elegível a IRRF",
      group = "Elegibilidade",
      order = 30,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "receipt_long")
  @Filterable(operation = FilterOperation.IS_TRUE, relation = "geraIr")
  private Boolean somenteGeraIr;

  @Schema(description = "Restringe às modalidades que geram cota de salário-família.")
  @UISchema(
      label = "Com salário-família",
      group = "Elegibilidade",
      order = 40,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "family_restroom")
  @Filterable(operation = FilterOperation.IS_TRUE, relation = "geraSalarioFamilia")
  private Boolean somenteSalarioFamilia;

  @Schema(description = "Faixa de idade limite padrão em anos configurada na modalidade.")
  @UISchema(
      label = "Faixa de idade limite",
      group = "Elegibilidade",
      order = 50,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.RANGE_SLIDER,
      icon = "cake")
  @Filterable(operation = FilterOperation.BETWEEN, relation = "idadeLimite")
  private List<Integer> idadeLimiteEntre;

  @Schema(description = "Restringe às modalidades vitalícias ou sem limite etário cadastrado.")
  @UISchema(
      label = "Sem limite etário",
      group = "Elegibilidade",
      order = 60,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "all_inclusive")
  @Filterable(operation = FilterOperation.IS_NULL, relation = "idadeLimite")
  private Boolean semIdadeLimite;

  @Schema(description = "Filtra pelo diploma legal regulamentador da modalidade.")
  @UISchema(
      label = "Fundamento legal",
      group = "Normativo",
      order = 70,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "description",
      endpoint = ApiPaths.DOCUMENTO_OPTIONS,
      valueField = "id",
      displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL, relation = "documentoLegalId")
  private Long documentoLegalId;
}
