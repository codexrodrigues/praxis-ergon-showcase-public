package br.com.praxis.ergonshowcase.dto;

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
        "Critérios de pesquisa do catálogo mestre de tabelas gerais de domínio do Ergon/Hades (HADES.TABELA) por código, finalidade, sistema proprietário e escopo multi-órgão.")
public class TabelaFilterDTO implements GenericFilterDTO {
  @Size(max = 180)
  @Schema(
      description =
          "Busca ampla textual pelo código identificador da tabela geral, finalidade funcional ou sistema proprietário.")
  @UISchema(
      label = "Busca ampla de tabela",
      helpText = "Pesquisa simultaneamente por código, finalidade ou sistema proprietário.",
      group = "Busca rápida",
      order = 1,
      icon = "search")
  @Filterable(
      operation = FilterOperation.LIKE,
      relations = {"tab", "descr", "sis"})
  private String buscaAmpla;

  @Size(max = 40)
  @Schema(
      description =
          "Filtra pelo código identificador exato da tabela geral de domínio (ex.: ERG_ESTCIVIL, ERG_ESCOLA).")
  @UISchema(
      label = "Código da tabela",
      helpText = "Identificador exato da tabela geral.",
      group = "Identificação",
      order = 10,
      icon = "tag")
  @Filterable(operation = FilterOperation.EQUAL, relation = "tab")
  private String tab;

  @Size(max = 180)
  @Schema(description = "Filtra por trecho da descrição funcional da tabela geral de domínio.")
  @UISchema(
      label = "Finalidade contém",
      helpText = "Texto parcial da finalidade da tabela geral.",
      group = "Identificação",
      order = 20,
      icon = "search")
  @Filterable(operation = FilterOperation.LIKE, relation = "descr")
  private String descr;

  @Size(max = 20)
  @Schema(
      description =
          "Filtra pelo sistema proprietário da tabela geral no ecossistema (ex.: Ergon, Hades).")
  @UISchema(
      label = "Sistema proprietário",
      helpText = "Subsistema responsável pela tabela geral.",
      group = "Identificação",
      order = 30,
      icon = "category")
  @Filterable(operation = FilterOperation.EQUAL, relation = "sis")
  private String sis;

  @Schema(
      description =
          "Filtra pelo indicador de escopo multi-órgão (MULTI_EMP) dos itens da tabela geral.")
  @UISchema(
      label = "Escopo multi-órgão",
      helpText = "Marca tabelas cujos itens admitem parametrização por órgão.",
      group = "Estrutura do item",
      order = 40,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "apartment")
  @Filterable(operation = FilterOperation.EQUAL, relation = "multiEmp")
  private Boolean multiEmp;

  @Size(max = 1)
  @Schema(
      description =
          "Filtra pela natureza do código dos itens da tabela geral ([A]lfanumérico ou [N]umérico).")
  @UISchema(
      label = "Natureza do código",
      helpText = "Tipo de código do item: A (alfanumérico) ou N (numérico).",
      group = "Estrutura do item",
      order = 50,
      icon = "code")
  @Filterable(operation = FilterOperation.EQUAL, relation = "tipo")
  private String tipo;

  @Schema(
      description =
          "Filtra apenas tabelas gerais que permitem variação de itens por órgão público (MULTI_EMP = verdadeiro).")
  @UISchema(
      label = "Somente multi-órgão",
      helpText = "Restringe a tabelas gerais multi-órgão.",
      group = "Filtros rápidos",
      order = 60,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "domain")
  @Filterable(operation = FilterOperation.IS_TRUE, relation = "multiEmp")
  private Boolean somenteMultiEmp;

  @Schema(
      description =
          "Filtra apenas tabelas gerais que definem rótulo semântico para a primeira coluna complementar (labelItem1).")
  @UISchema(
      label = "Com coluna complementar",
      helpText = "Restringe a tabelas gerais que utilizam colunas auxiliares item1..3.",
      group = "Filtros rápidos",
      order = 70,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "view_column")
  @Filterable(operation = FilterOperation.IS_NOT_NULL, relation = "labelItem1")
  private Boolean comColunaComplementar;
}
