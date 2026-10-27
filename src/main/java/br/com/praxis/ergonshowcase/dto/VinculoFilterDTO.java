package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
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
@Schema(description = "Critérios de pesquisa de vínculos na administração de pessoal.")
public class VinculoFilterDTO implements GenericFilterDTO {
  @Schema(description = "Número funcional exato da pessoa cujos vínculos serão consultados.")
  @UISchema(
      label = "Pessoa servidora",
      controlType = FieldControlType.ENTITY_LOOKUP,
      endpoint = ApiPaths.PESSOA_OPTIONS,
      valueField = "id",
      displayField = "label",
      group = "Pesquisa",
      order = 10)
  @Filterable(operation = FilterOperation.EQUAL, relation = "id.numfunc")
  private Long numfunc;

  @Schema(description = "Número sequencial exato do vínculo dentro do cadastro da pessoa.")
  @UISchema(label = "Vínculo", group = "Pesquisa", order = 20)
  @Filterable(operation = FilterOperation.EQUAL, relation = "id.numero")
  private Integer numero;

  @Schema(description = "Órgão de origem dos assentamentos funcionais pesquisados.")
  @UISchema(
      label = "Órgão",
      controlType = FieldControlType.ENTITY_LOOKUP,
      endpoint = ApiPaths.ORGAO_OPTIONS,
      valueField = "id",
      displayField = "label",
      group = "Pesquisa",
      order = 30)
  @Filterable(operation = FilterOperation.EQUAL)
  private Integer empCodigo;

  @Schema(
      description = "Estados funcionais incluídos no resultado, como ATIVO, AFASTADO ou CEDIDO.")
  @UISchema(label = "Situações", group = "Pesquisa", order = 40)
  @Filterable(operation = FilterOperation.IN, relation = "situacao")
  private List<String> situacoes;

  @Schema(description = "Intervalo inclusivo de início do efetivo exercício do vínculo.")
  @UISchema(
      label = "Período de exercício",
      controlType = FieldControlType.DATE_RANGE,
      group = "Pesquisa",
      order = 50)
  @Filterable(operation = FilterOperation.BETWEEN, relation = "dtexerc")
  private List<LocalDate> exercicio;

  @Schema(
      description =
          "Janela móvel em dias para localizar vínculos que iniciaram exercício recentemente.")
  @UISchema(label = "Exercício nos últimos dias", group = "Pesquisa", order = 60)
  @Filterable(operation = FilterOperation.IN_LAST_DAYS, relation = "dtexerc")
  private Integer exercicioRecente;

  @Schema(description = "Trecho da matrícula institucional do vínculo.")
  @UISchema(label = "Matrícula", group = "Pesquisa", order = 70)
  @Filterable(operation = FilterOperation.LIKE)
  private String matric;

  @CustomFields(resourceKey = "administracao-pessoal.vinculos")
  @Schema(
      description =
          "Predicados sobre atributos funcionais dinâmicos, validados pelo catálogo do órgão.")
  @UISchema(label = "Características adicionais", group = "Pesquisa", order = 80)
  private Map<String, Object> customFields;
}
