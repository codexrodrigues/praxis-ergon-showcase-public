package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.filter.annotation.Filterable;
import org.praxisplatform.uischema.filter.annotation.Filterable.FilterOperation;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;

@Getter
@Setter
@Schema(
    description =
        "Critérios de consulta do cadastro auxiliar para inspeção de referências funcionais.")
public class SetorFilterDTO implements GenericFilterDTO {
  @Schema(description = "Trecho da denominação oficial da unidade organizacional.")
  @UISchema(label = "Nome da unidade", group = "Pesquisa")
  @Filterable(operation = FilterOperation.LIKE, relation = "nome")
  private String nome;

  @Schema(description = "Prefixo da sigla administrativa da unidade.")
  @UISchema(label = "Sigla", group = "Pesquisa")
  @Filterable(operation = FilterOperation.STARTS_WITH, relation = "sigla")
  private String sigla;

  @Schema(description = "Órgão proprietário das unidades a consultar.")
  @UISchema(
      label = "Órgão",
      group = "Pesquisa",
      type = org.praxisplatform.uischema.FieldDataType.TEXT,
      controlType = org.praxisplatform.uischema.FieldControlType.ENTITY_LOOKUP,
      icon = "apartment",
      endpoint = ApiPaths.ORGAO_OPTIONS,
      valueField = "id",
      displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL, relation = "empCodigo")
  private Integer empCodigo;

  @Schema(description = "Unidade imediatamente superior no organograma pesquisado.")
  @UISchema(
      label = "Unidade superior",
      group = "Pesquisa",
      type = org.praxisplatform.uischema.FieldDataType.TEXT,
      controlType = org.praxisplatform.uischema.FieldControlType.ENTITY_LOOKUP,
      icon = "account_tree",
      endpoint = ApiPaths.SETOR_OPTIONS,
      valueField = "id",
      displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL, relation = "setorPai")
  private String setorPai;

  @Schema(description = "Classificação administrativa da unidade organizacional.")
  @UISchema(label = "Tipo de unidade", group = "Pesquisa")
  @Filterable(operation = FilterOperation.EQUAL, relation = "tipoSetor")
  private String tipoSetor;

  @Schema(description = "Restringe a unidades sem data de encerramento registrada.")
  @UISchema(label = "Sem encerramento", group = "Pesquisa")
  @Filterable(operation = FilterOperation.IS_NULL, relation = "dtfim")
  private Boolean vigentes;

  @Schema(description = "Restringe a unidades com encerramento de vigência registrado.")
  @UISchema(label = "Com encerramento", group = "Pesquisa")
  @Filterable(operation = FilterOperation.IS_NOT_NULL, relation = "dtfim")
  private Boolean encerrados;
}
