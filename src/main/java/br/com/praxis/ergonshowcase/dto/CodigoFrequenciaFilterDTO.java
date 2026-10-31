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
        "Critérios de pesquisa do catálogo de códigos de ocorrência de frequência e modalidades de licença/afastamento (CODIGOS_FREQ_).")
public class CodigoFrequenciaFilterDTO implements GenericFilterDTO {
  @Schema(
      description =
          "Busca textual ampla por descrição, sigla mnemônica, nome abreviado ou denominação do tipo macro de frequência.")
  @UISchema(label = "Busca rápida", group = "Pesquisa", order = 5, icon = "search")
  @Filterable(
      operation = FilterOperation.LIKE,
      relations = {"nome", "mnemonico", "nomeAbrev", "id.tipofreq", "tipoFrequenciaEntidade.nome"})
  private String buscaAmpla;

  @Schema(description = "Filtra pelo tipo macro de frequência ao qual o código pertence.")
  @UISchema(
      label = "Tipo de frequência",
      group = "Pesquisa",
      order = 10,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "category",
      endpoint = ApiPaths.TIPO_FREQUENCIA_OPTIONS,
      valueField = "id",
      displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL, relation = "id.tipofreq")
  private String tipofreq;

  @Schema(description = "Filtra pelo código numérico exato da ocorrência.")
  @UISchema(
      label = "Código numérico",
      group = "Pesquisa",
      order = 20,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.INPUT,
      icon = "tag")
  @Filterable(operation = FilterOperation.EQUAL, relation = "id.codigo")
  private Integer codigo;

  @Schema(description = "Trecho da descrição completa do código de frequência ou licença.")
  @UISchema(label = "Descrição", group = "Pesquisa", order = 30)
  @Filterable(operation = FilterOperation.LIKE, relation = "nome")
  private String nome;

  @Schema(description = "Prefixo da sigla mnemônica do código de frequência.")
  @UISchema(label = "Sigla mnemônica", group = "Pesquisa", order = 40)
  @Filterable(operation = FilterOperation.STARTS_WITH, relation = "mnemonico")
  private String mnemonico;

  @Schema(description = "Restringe aos códigos habilitados para concessão formal de afastamento ou licença.")
  @UISchema(
      label = "Somente licenças/afastamentos",
      group = "Regras",
      order = 50,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "event_busy")
  @Filterable(operation = FilterOperation.IS_TRUE, relation = "afastamento")
  private Boolean somenteAfastamento;

  @Schema(description = "Restringe aos códigos lançáveis diretamente via boletim de frequência.")
  @UISchema(
      label = "Lançável via frequência",
      group = "Regras",
      order = 60,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "edit_calendar")
  @Filterable(operation = FilterOperation.IS_TRUE, relation = "viaFrequencia")
  private Boolean somenteViaFrequencia;

  @Schema(description = "Restringe aos códigos com ônus financeiro ou desconto remuneratório.")
  @UISchema(
      label = "Com ônus financeiro",
      group = "Regras",
      order = 70,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "payments")
  @Filterable(operation = FilterOperation.IS_TRUE, relation = "onus")
  private Boolean somenteComOnus;

  @Schema(description = "Restringe aos códigos sujeitos a regras de limite estatutário.")
  @UISchema(
      label = "Com teto estatutário",
      group = "Regras",
      order = 80,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "rule")
  @Filterable(operation = FilterOperation.IS_TRUE, relation = "regrasFreq")
  private Boolean somenteComRegrasFreq;

  @Schema(description = "Filtra pela restrição de sexo aplicável ([A], [F] ou [M]).")
  @UISchema(label = "Sexo aplicável", group = "Parâmetros", order = 90)
  @Filterable(operation = FilterOperation.EQUAL, relation = "sexo")
  private String sexo;

  @Schema(description = "Filtra pela unidade de medida configurada (DIAS, HORAS, OCORRENCIAS).")
  @UISchema(label = "Unidade de medida", group = "Parâmetros", order = 100)
  @Filterable(operation = FilterOperation.EQUAL, relation = "formatoQtd")
  private String formatoQtd;

  @Schema(description = "Faixa de quantidade máxima permitida por lançamento.")
  @UISchema(
      label = "Faixa de teto máximo",
      group = "Parâmetros",
      order = 110,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.RANGE_SLIDER,
      icon = "vertical_align_top")
  @Filterable(operation = FilterOperation.BETWEEN, relation = "qtdmax")
  private List<BigDecimal> qtdmaxEntre;

  @Schema(description = "Filtra pelo diploma legal que fundamenta o código de ocorrência.")
  @UISchema(
      label = "Fundamento legal",
      group = "Normativo",
      order = 120,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "description",
      endpoint = ApiPaths.DOCUMENTO_OPTIONS,
      valueField = "id",
      displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL, relation = "documentoLegalId")
  private Long documentoLegalId;
}
