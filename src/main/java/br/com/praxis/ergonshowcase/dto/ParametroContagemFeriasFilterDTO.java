package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.customfields.CustomFields;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.filter.annotation.Filterable;
import org.praxisplatform.uischema.filter.annotation.Filterable.FilterOperation;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;

@Getter
@Setter
@NoArgsConstructor
@Schema(
    description =
        "Critérios ricos de filtragem da matriz estatutária de parâmetros de contagem de férias, abono pecuniário, prescrição e licença especial.")
public class ParametroContagemFeriasFilterDTO implements GenericFilterDTO {

  @Schema(
      description =
          "Busca ampla textual por regime jurídico, tipo de vínculo, categoria, subcategoria, órgão ou observações normativas.")
  @UISchema(
      label = "Busca ampla",
      helpText = "Pesquise por regime jurídico, vínculo, categoria, órgão ou observação.",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "search")
  @Filterable(
      operation = FilterOperation.LIKE,
      relations = {
        "regimeJuridico",
        "tipoVinculo",
        "categoria",
        "subcategoria",
        "observacoes",
        "orgao.nome"
      })
  private String searchTerm;

  @Schema(description = "Filtra regras pertencentes a um órgão público específico.")
  @UISchema(
      label = "Órgão",
      helpText = "Selecione o órgão público de aplicação da regra.",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "apartment",
      endpoint = ApiPaths.ORGAO_OPTIONS,
      valueField = "id",
      displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL, relation = "empCodigo")
  private Integer empCodigo;

  @Schema(description = "Filtra regras gerais multi-órgão (quando empCodigo é nulo).")
  @UISchema(
      label = "Apenas regras gerais",
      helpText = "Exibe somente regras globais aplicáveis a todos os órgãos.",
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "public")
  @Filterable(operation = FilterOperation.IS_NULL, relation = "empCodigo")
  private Boolean regraGeralMultiOrgao;

  @Schema(description = "Filtra pelo regime jurídico exato (ex.: ESTATUTARIO, CLT).")
  @UISchema(
      label = "Regime jurídico",
      helpText = "Filtra pelo código exato do regime jurídico.",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "gavel")
  @Filterable(operation = FilterOperation.EQUAL, relation = "regimeJuridico")
  private String regimeJuridico;

  @Schema(description = "Filtra por múltiplos regimes jurídicos simultaneamente.")
  @UISchema(
      label = "Lista de regimes",
      helpText = "Filtra por um conjunto de regimes jurídicos.",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.MULTI_SELECT,
      icon = "gavel")
  @Filterable(operation = FilterOperation.IN, relation = "regimeJuridico")
  private List<String> regimesJuridicos;

  @Schema(description = "Filtra pelo tipo de vínculo funcional (ex.: EFETIVO, COMISSIONADO).")
  @UISchema(
      label = "Tipo de vínculo",
      helpText = "Filtra pela modalidade do vínculo funcional.",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "work")
  @Filterable(operation = FilterOperation.EQUAL, relation = "tipoVinculo")
  private String tipoVinculo;

  @Schema(description = "Filtra por múltiplos tipos de vínculo funcional.")
  @UISchema(
      label = "Lista de tipos de vínculo",
      helpText = "Seleciona múltiplos tipos de vínculo funcional.",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.MULTI_SELECT,
      icon = "work")
  @Filterable(operation = FilterOperation.IN, relation = "tipoVinculo")
  private List<String> tiposVinculo;

  @Schema(description = "Filtra pela categoria funcional (ex.: ADMINISTRATIVO, MAGISTERIO, SAUDE).")
  @UISchema(
      label = "Categoria funcional",
      helpText = "Filtra pela categoria funcional de carreira.",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "category")
  @Filterable(operation = FilterOperation.EQUAL, relation = "categoria")
  private String categoria;

  @Schema(description = "Filtra por múltiplas categorias funcionais.")
  @UISchema(
      label = "Lista de categorias",
      helpText = "Seleciona múltiplas categorias funcionais.",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.MULTI_SELECT,
      icon = "category")
  @Filterable(operation = FilterOperation.IN, relation = "categoria")
  private List<String> categorias;

  @Schema(description = "Filtra pela subcategoria funcional.")
  @UISchema(
      label = "Subcategoria",
      helpText = "Filtra pela subcategoria funcional.",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "layers")
  @Filterable(operation = FilterOperation.LIKE, relation = "subcategoria")
  private String subcategoria;

  @Schema(description = "Filtra pelo intervalo da data de início de vigência (de/até).")
  @UISchema(
      label = "Período de início",
      helpText = "Intervalo da data de início de vigência.",
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_RANGE,
      icon = "date_range")
  @Filterable(operation = FilterOperation.BETWEEN, relation = "dtini")
  private List<LocalDate> dtiniBetween;

  @Schema(description = "Filtra pelo intervalo da data de término de vigência (de/até).")
  @UISchema(
      label = "Período de término",
      helpText = "Intervalo da data final de vigência.",
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_RANGE,
      icon = "event_busy")
  @Filterable(operation = FilterOperation.BETWEEN, relation = "dtfim")
  private List<LocalDate> dtfimBetween;

  @Schema(description = "Filtra apenas regras com vigência em aberto (sem data de término).")
  @UISchema(
      label = "Vigência em aberto",
      helpText = "Exibe apenas regras sem data final de vigência.",
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "check_circle")
  @Filterable(operation = FilterOperation.IS_NULL, relation = "dtfim")
  private Boolean vigenciaEmAberto;

  @Schema(description = "Filtra pelo indicador de direito a férias.")
  @UISchema(
      label = "Possui direito a férias",
      helpText = "Filtra enquadramentos que conferem direito a férias.",
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "beach_access")
  @Filterable(operation = FilterOperation.EQUAL, relation = "temFerias")
  private Boolean temFerias;

  @Schema(description = "Filtra pela base de abono pecuniário (D, G ou N).")
  @UISchema(
      label = "Base do abono pecuniário",
      helpText = "Filtra por D (direito), G (gozo) ou N (sem abono).",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      icon = "sell")
  @Filterable(operation = FilterOperation.EQUAL, relation = "abonoDirGozo")
  private String abonoDirGozo;

  @Schema(description = "Filtra regras que permitem gozo antes do fim do período aquisitivo.")
  @UISchema(
      label = "Gozo antes do fim do PA",
      helpText = "Filtra regras com permissão de gozo antecipado.",
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "history")
  @Filterable(operation = FilterOperation.EQUAL, relation = "permiteGozoAntesPaFer")
  private Boolean permiteGozoAntesPaFer;

  @Schema(description = "Filtra regras que permitem contagem em dobro de férias.")
  @UISchema(
      label = "Permite contagem em dobro",
      helpText = "Filtra regras que admitem averbação de férias em dobro.",
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "trending_up")
  @Filterable(operation = FilterOperation.EQUAL, relation = "contDobroFerias")
  private Boolean contDobroFerias;

  @Schema(description = "Filtra regras que contemplam direito a licença especial.")
  @UISchema(
      label = "Possui licença especial",
      helpText = "Filtra enquadramentos com direito a licença-prêmio/especial.",
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "workspace_premium")
  @Filterable(operation = FilterOperation.EQUAL, relation = "temLicEsp")
  private Boolean temLicEsp;

  @Schema(description = "Filtra regras que habilitam progressão funcional.")
  @UISchema(
      label = "Possui progressão funcional",
      helpText = "Filtra enquadramentos com apuração de progressão funcional.",
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "trending_up")
  @Filterable(operation = FilterOperation.EQUAL, relation = "temProgressao")
  private Boolean temProgressao;

  @Schema(description = "Filtra pelo intervalo do total de dias de direito a férias.")
  @UISchema(
      label = "Faixa de dias de direito",
      helpText = "Intervalo do total de dias de direito a férias.",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.RANGE_SLIDER,
      icon = "calendar_month")
  @Filterable(operation = FilterOperation.BETWEEN, relation = "totDiasFer")
  private List<Integer> totDiasFerBetween;

  @Schema(description = "Filtra pelo status ativo ou inativo da parametrização.")
  @UISchema(
      label = "Ativo",
      helpText = "Filtra por regras ativas ou inativas.",
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "toggle_on")
  @Filterable(operation = FilterOperation.EQUAL, relation = "active")
  private Boolean active;

  @Schema(description = "Filtra regras iniciadas nos últimos N dias.")
  @UISchema(
      label = "Iniciadas nos últimos dias",
      helpText = "Quantidade de dias recentes para início de vigência.",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "history")
  @Filterable(operation = FilterOperation.IN_LAST_DAYS, relation = "dtini")
  private Integer iniciadasUltimosDias;

  @Schema(description = "Filtra regras com término previsto nos próximos N dias.")
  @UISchema(
      label = "Expirando nos próximos dias",
      helpText = "Radar de regras cuja vigência se encerra nos próximos dias.",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "upcoming")
  @Filterable(operation = FilterOperation.IN_NEXT_DAYS, relation = "dtfim")
  private Integer expirandoProximosDias;

  @Schema(
      description =
          "Filtra regras cujo limite máximo de dias vendáveis em abono pecuniário seja igual ou superior ao valor informado.")
  @UISchema(
      label = "Máximo de abono mínimo",
      helpText = "Informe o mínimo de dias vendáveis permitido pela regra.",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "payments")
  @Filterable(operation = FilterOperation.GREATER_OR_EQUAL, relation = "maxDiasVendFer")
  private Integer maxDiasVendFerMinimo;

  @Schema(description = "Critérios de filtragem sobre atributos dinâmicos customizados.")
  @CustomFields(resourceKey = "administracao-pessoal.parametros-contagem-ferias")
  @UISchema(
      label = "Campos customizados",
      helpText = "Filtros adicionais sobre propriedades dinâmicas da regra.",
      group = "Campos Customizados",
      order = 900)
  private Map<String, Object> customFields = new LinkedHashMap<>();
}
