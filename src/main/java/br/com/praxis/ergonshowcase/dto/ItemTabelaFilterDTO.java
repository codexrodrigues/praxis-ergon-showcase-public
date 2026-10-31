package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
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
        "Critérios de pesquisa dos itens de tabelas gerais de domínio do Ergon/Hades (HADES.ITEMTABELA_) por tabela, código do item, descrição, órgão, atributos auxiliares e situação.")
public class ItemTabelaFilterDTO implements GenericFilterDTO {
  @Size(max = 200)
  @Schema(
      description =
          "Busca ampla textual por código da tabela geral, código do item, descrição do item, atributo complementar item1 ou finalidade da tabela.")
  @UISchema(
      label = "Busca ampla de item",
      helpText = "Pesquisa simultaneamente por tabela, código, descrição ou atributo auxiliar.",
      group = "Busca rápida",
      order = 1,
      icon = "search")
  @Filterable(
      operation = FilterOperation.LIKE,
      relations = {"id.tab", "id.item", "descr", "item1", "tabelaEntidade.descr"})
  private String buscaAmpla;

  @Size(max = 40)
  @Schema(
      description =
          "Filtra pelo código identificador exato da tabela geral proprietária do item (componente 1 da chave composta).")
  @UISchema(
      label = "Tabela geral",
      helpText = "Seleciona a tabela geral proprietária dos itens.",
      group = "Identificação",
      order = 10,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "table_chart",
      endpoint = ApiPaths.TABELA_OPTIONS,
      valueField = "id",
      displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL, relation = "id.tab")
  private String tab;

  @Size(max = 99)
  @Schema(
      description =
          "Filtra pelo código identificador exato do item dentro da tabela geral (componente 2 da chave composta).")
  @UISchema(
      label = "Código do item",
      helpText = "Código exato do item de domínio.",
      group = "Identificação",
      order = 20,
      icon = "tag")
  @Filterable(operation = FilterOperation.EQUAL, relation = "id.item")
  private String item;

  @Size(max = 200)
  @Schema(description = "Filtra por trecho da descrição legível do item da tabela geral.")
  @UISchema(
      label = "Descrição contém",
      helpText = "Texto parcial da descrição do item.",
      group = "Identificação",
      order = 30,
      icon = "search")
  @Filterable(operation = FilterOperation.LIKE, relation = "descr")
  private String descr;

  @Schema(
      description =
          "Filtra pelo órgão público específico ao qual o item se aplica em tabelas multi-órgão.")
  @UISchema(
      label = "Órgão específico",
      helpText = "Órgão público proprietário do item.",
      group = "Escopo e ordenação",
      order = 40,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "apartment",
      endpoint = ApiPaths.ORGAO_OPTIONS,
      valueField = "id",
      displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL, relation = "empCodigo")
  private Integer empCodigo;

  @Schema(description = "Filtra pelo indicador de item ativo ou inativo para seleção.")
  @UISchema(
      label = "Item ativo",
      helpText = "Indica se o item está ativo para seleção.",
      group = "Escopo e ordenação",
      order = 50,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "verified")
  @Filterable(operation = FilterOperation.EQUAL, relation = "ativo")
  private Boolean ativo;

  @Size(max = 120)
  @Schema(description = "Filtra pelo valor do primeiro atributo complementar (item1).")
  @UISchema(
      label = "Atributo item1",
      helpText = "Valor do primeiro atributo auxiliar do item.",
      group = "Atributos complementares",
      order = 60,
      icon = "tune")
  @Filterable(operation = FilterOperation.LIKE, relation = "item1")
  private String item1;

  @Schema(description = "Filtra apenas itens ativos para seleção em novos cadastros.")
  @UISchema(
      label = "Somente ativos",
      helpText = "Restringe a itens ativos no catálogo.",
      group = "Filtros rápidos",
      order = 70,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "check_circle")
  @Filterable(operation = FilterOperation.IS_TRUE, relation = "ativo")
  private Boolean somenteAtivos;

  @Schema(
      description =
          "Filtra apenas itens globais compartilhados por todos os órgãos (empCodigo nulo).")
  @UISchema(
      label = "Somente itens globais",
      helpText = "Restringe a itens sem restrição por órgão público.",
      group = "Filtros rápidos",
      order = 80,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "public")
  @Filterable(operation = FilterOperation.IS_NULL, relation = "empCodigo")
  private Boolean somenteGlobais;
}
