package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.*;
import lombok.Getter;
import lombok.Setter;
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
  @Schema(description = "Localiza cargos por trecho da denominação, categoria funcional, escolaridade exigida ou tipo de cargo. A correspondência em qualquer desses atributos atende à busca; órgão e demais critérios continuam cumulativos.", example = "SAUDE")
  @UISchema(label = "Busca ampla", group = "Pesquisa", order = 1, icon = "search", type = org.praxisplatform.uischema.FieldDataType.TEXT, controlType = org.praxisplatform.uischema.FieldControlType.INPUT)
  @Filterable(operation = FilterOperation.LIKE, relations = {"nome", "categoria", "escolaridade", "tipoCargo"})
  private String buscaAmpla;

  @Schema(description = "Trecho da nomenclatura do cargo ou função.")
  @UISchema(label = "Nome do cargo", group = "Pesquisa", order = 10)
  @Filterable(operation = FilterOperation.LIKE)
  private String nome;

  @Schema(description = "Órgão proprietário do quadro de cargos a consultar.")
  @UISchema(
      label = "Órgão",
      controlType = org.praxisplatform.uischema.FieldControlType.ENTITY_LOOKUP,
      endpoint = ApiPaths.ORGAO_OPTIONS,
      valueField = "id",
      displayField = "label",
      group = "Pesquisa",
      order = 20,
      type = org.praxisplatform.uischema.FieldDataType.TEXT,
      icon = "apartment")
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

  @Schema(description = "Distingue cargo de provimento e função de confiança.")
  @UISchema(label = "Cargo ou função", group = "Pesquisa avançada", order = 200)
  @Filterable(operation = FilterOperation.EQUAL, relation = "cargoFuncao")
  private String cargoFuncao;

  @Schema(description = "Trecho da especialidade funcional do cargo.")
  @UISchema(label = "Subcategoria", group = "Pesquisa avançada", order = 201)
  @Filterable(operation = FilterOperation.LIKE, relation = "subcategoria")
  private String subcategoria;

  @Schema(description = "Modalidades de provimento incluídas.")
  @UISchema(label = "Tipos de cargo", group = "Pesquisa avançada", order = 202)
  @Filterable(operation = FilterOperation.IN, relation = "tipoCargo")
  private List<String> tiposCargo;

  @Schema(description = "Graus de instrução exigidos para o cargo.")
  @UISchema(label = "Escolaridades", group = "Pesquisa avançada", order = 203)
  @Filterable(operation = FilterOperation.IN, relation = "escolaridade")
  private List<String> escolaridades;

  @Schema(description = "Prefixo da classificação brasileira de ocupações.")
  @UISchema(label = "Prefixo CBO", group = "Pesquisa avançada", order = 204)
  @Filterable(operation = FilterOperation.STARTS_WITH, relation = "cbo")
  private String cbo;

  @Schema(description = "Pesquisa cargos sujeitos ao controle de vagas.")
  @UISchema(label = "Controle de vagas", group = "Pesquisa avançada", order = 205)
  @Filterable(operation = FilterOperation.EQUAL, relation = "controleVaga")
  private Boolean controleVaga;

  @Schema(description = "Pesquisa cargos remunerados por referência salarial.")
  @UISchema(label = "Por referência salarial", group = "Pesquisa avançada", order = 206)
  @Filterable(operation = FilterOperation.EQUAL, relation = "porReferencia")
  private Boolean porReferencia;

  @Schema(description = "Intervalo inclusivo de vagas legalmente autorizadas.")
  @UISchema(label = "Intervalo de vagas", group = "Pesquisa avançada", order = 207)
  @Filterable(operation = FilterOperation.BETWEEN, relation = "vagasAutorizadas")
  private List<Integer> vagasAutorizadasRange;

  @Schema(description = "Quantidade mínima de vagas autorizadas.")
  @UISchema(label = "Vagas mínimas", group = "Pesquisa avançada", order = 208)
  @Filterable(operation = FilterOperation.GREATER_OR_EQUAL, relation = "vagasAutorizadas")
  private Integer vagasMinimas;

  @Schema(description = "Intervalo de início do controle de vagas.")
  @UISchema(
      label = "Início do controle de vagas",
      group = "Pesquisa avançada",
      order = 209,
      controlType = org.praxisplatform.uischema.FieldControlType.DATE_RANGE,
      type = org.praxisplatform.uischema.FieldDataType.DATE,
      icon = "event")
  @Filterable(operation = FilterOperation.BETWEEN, relation = "dtInicioContrVaga")
  private List<LocalDate> vigenciaControleVagaRange;

  @Schema(description = "Seleciona cargos sem ato de extinção registrado.")
  @UISchema(label = "Somente vigentes", group = "Pesquisa avançada", order = 210)
  @Filterable(operation = FilterOperation.IS_NULL, relation = "dtExtincao")
  private Boolean apenasVigentes;

  @Schema(description = "Seleciona cargos com extinção registrada.")
  @UISchema(label = "Somente extintos", group = "Pesquisa avançada", order = 211)
  @Filterable(operation = FilterOperation.IS_NOT_NULL, relation = "dtExtincao")
  private Boolean apenasExtintos;
}
