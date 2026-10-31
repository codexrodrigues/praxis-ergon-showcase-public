package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
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
        "Critérios de pesquisa do catálogo de tipos macro de frequência e afastamento (TIPO_FREQ_).")
public class TipoFrequenciaFilterDTO implements GenericFilterDTO {
  @Schema(
      description =
          "Busca textual ampla por código identificador ou denominação do tipo macro de frequência.")
  @UISchema(label = "Busca rápida", group = "Pesquisa", order = 5, icon = "search")
  @Filterable(
      operation = FilterOperation.LIKE,
      relations = {"tipo", "nome"})
  private String buscaAmpla;

  @Schema(description = "Prefixo do código identificador do tipo macro de frequência.")
  @UISchema(label = "Código do tipo", group = "Pesquisa", order = 10)
  @Filterable(operation = FilterOperation.STARTS_WITH, relation = "tipo")
  private String tipo;

  @Schema(description = "Trecho da denominação oficial do tipo de frequência.")
  @UISchema(label = "Denominação", group = "Pesquisa", order = 20)
  @Filterable(operation = FilterOperation.LIKE, relation = "nome")
  private String nome;

  @Schema(description = "Restringe aos tipos que influenciam a contagem de tempo de serviço.")
  @UISchema(
      label = "Influi na contagem",
      group = "Reflexos",
      order = 30,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "schedule")
  @Filterable(operation = FilterOperation.IS_TRUE, relation = "influiConta")
  private Boolean somenteInfluiConta;

  @Schema(description = "Restringe aos tipos que caracterizam prestação de hora extraordinária.")
  @UISchema(
      label = "Caracteriza hora extra",
      group = "Reflexos",
      order = 40,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "more_time")
  @Filterable(operation = FilterOperation.IS_TRUE, relation = "horaExtra")
  private Boolean somenteHoraExtra;

  @Schema(description = "Faixa de percentual de acréscimo de hora extraordinária.")
  @UISchema(
      label = "Faixa de adicional (%)",
      group = "Reflexos",
      order = 50,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.RANGE_SLIDER,
      icon = "percent")
  @Filterable(operation = FilterOperation.BETWEEN, relation = "percentualExtra")
  private List<BigDecimal> percentualExtraEntre;

  @Schema(description = "Filtra pelo diploma legal regulamentador da natureza de frequência.")
  @UISchema(
      label = "Fundamento legal",
      group = "Normativo",
      order = 60,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "description",
      endpoint = ApiPaths.DOCUMENTO_OPTIONS,
      valueField = "id",
      displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL, relation = "documentoLegalId")
  private Long documentoLegalId;
}
