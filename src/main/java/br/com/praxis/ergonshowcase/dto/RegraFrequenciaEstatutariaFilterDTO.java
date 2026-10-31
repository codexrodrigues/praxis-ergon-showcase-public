package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.filter.annotation.Filterable;
import org.praxisplatform.uischema.filter.annotation.Filterable.FilterOperation;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;

@Getter
@Setter
@Schema(
    description =
        "Filtros avançados para consulta e auditoria da matriz de regras estatutárias de frequência e afastamento.")
public class RegraFrequenciaEstatutariaFilterDTO implements GenericFilterDTO {

  @Filterable(
      operation = FilterOperation.LIKE,
      relations = {
        "ruleKey",
        "regimeJuridico",
        "tipoVinculo",
        "categoria",
        "subcategoria",
        "codigoFrequenciaEntidade.nome",
        "documentoLegal.numeroDocumento"
      })
  @Schema(
      description =
          "Busca ampla textual por chave da regra, regime jurídico, vínculo, categoria, modalidade de frequência ou norma legal.")
  @UISchema(
      label = "Busca ampla de regras",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "search",
      group = "Pesquisa Rápida",
      order = 1,
      helpText = "Pesquise por chave do RuleSet, modalidade, regime, categoria ou norma legal.")
  private String searchTerm;

  @Filterable(operation = FilterOperation.EQUAL, relation = "empCodigo")
  @Schema(description = "Filtra regras aplicáveis a um órgão público específico.")
  @UISchema(
      label = "Órgão",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "apartment",
      group = "Abrangência Estatutária",
      order = 10,
      endpoint = ApiPaths.ORGAO_OPTIONS,
      valueField = "id",
      displayField = "label",
      helpText = "Selecione o órgão público para restringir a matriz.")
  private Integer empCodigo;

  @Filterable(operation = FilterOperation.EQUAL, relation = "regimeJuridico")
  @Schema(description = "Filtra regras pelo regime jurídico alvo.")
  @UISchema(
      label = "Regime jurídico",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      icon = "balance",
      group = "Abrangência Estatutária",
      order = 20,
      options =
          "[{\"id\":\"ESTATUTARIO\",\"label\":\"Estatutário\"},{\"id\":\"CLT\",\"label\":\"Celetista\"},{\"id\":\"ESPECIAL\",\"label\":\"Regime Especial\"}]",
      valueField = "id",
      displayField = "label",
      helpText = "Filtra pelo regime jurídico da regra.")
  private String regimeJuridico;

  @Filterable(operation = FilterOperation.EQUAL, relation = "tipoVinculo")
  @Schema(description = "Filtra regras pelo tipo de vínculo funcional alvo.")
  @UISchema(
      label = "Tipo de vínculo",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      icon = "badge",
      group = "Abrangência Estatutária",
      order = 30,
      options =
          "[{\"id\":\"EFETIVO\",\"label\":\"Efetivo\"},{\"id\":\"COMISSAO\",\"label\":\"Comissionado\"},{\"id\":\"TEMPORARIO\",\"label\":\"Temporário\"},{\"id\":\"TODOS\",\"label\":\"Todos os vínculos\"}]",
      valueField = "id",
      displayField = "label",
      helpText = "Filtra pela natureza do vínculo funcional.")
  private String tipoVinculo;

  @Filterable(operation = FilterOperation.EQUAL, relation = "categoria")
  @Schema(description = "Filtra regras pela categoria funcional alvo.")
  @UISchema(
      label = "Categoria funcional",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      icon = "groups",
      group = "Abrangência Estatutária",
      order = 40,
      options =
          "[{\"id\":\"GERAL\",\"label\":\"Quadro Geral\"},{\"id\":\"MAGISTERIO\",\"label\":\"Magistério\"},{\"id\":\"GUARDA_MUNICIPAL\",\"label\":\"Guarda Municipal\"},{\"id\":\"SAUDE\",\"label\":\"Saúde\"},{\"id\":\"TODAS\",\"label\":\"Todas as categorias\"}]",
      valueField = "id",
      displayField = "label",
      helpText = "Filtra pela categoria de carreira.")
  private String categoria;

  @Filterable(operation = FilterOperation.EQUAL, relation = "tipofreq")
  @Schema(description = "Filtra regras pelo tipo macro de frequência.")
  @UISchema(
      label = "Tipo de frequência",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "category",
      group = "Modalidade e Vigência",
      order = 50,
      endpoint = ApiPaths.TIPO_FREQUENCIA_OPTIONS,
      valueField = "id",
      displayField = "label",
      helpText = "Selecione o agrupador macro de frequência.")
  private String tipofreq;

  @Filterable(operation = FilterOperation.EQUAL, relation = "codfreq")
  @Schema(description = "Filtra regras pelo código específico de frequência ou afastamento.")
  @UISchema(
      label = "Código de frequência",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "pin",
      group = "Modalidade e Vigência",
      order = 60,
      endpoint = ApiPaths.CODIGO_FREQUENCIA_OPTIONS,
      valueField = "id",
      displayField = "label",
      helpText = "Selecione a modalidade específica de frequência ou afastamento.")
  private Integer codfreq;

  @Filterable(operation = FilterOperation.LIKE, relation = "ruleKey")
  @Schema(description = "Filtra pela chave canônica do RuleSet no motor de regras.")
  @UISchema(
      label = "Chave da regra",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "gavel",
      group = "Modalidade e Vigência",
      order = 70,
      helpText = "Trecho da chave canônica do RuleSet.")
  private String ruleKey;

  @Filterable(operation = FilterOperation.BETWEEN, relation = "dtini")
  @Schema(description = "Intervalo de datas de início de vigência da regra estatutária.")
  @UISchema(
      label = "Período de início de vigência",
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_RANGE,
      icon = "date_range",
      group = "Modalidade e Vigência",
      order = 80,
      helpText = "Faixa de datas de início de vigência legal.")
  private List<LocalDate> dtiniBetween;

  @Filterable(operation = FilterOperation.IS_NULL, relation = "dtfim")
  @Schema(description = "Restringe apenas regras estatutárias com vigência em aberto.")
  @UISchema(
      label = "Vigência em aberto",
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "verified",
      group = "Modalidade e Vigência",
      order = 90,
      helpText = "Quando marcado, exibe apenas regras sem data final de vigência.")
  private Boolean semDtfim;

  @Filterable(operation = FilterOperation.GREATER_OR_EQUAL, relation = "qtdMaxDias")
  @Schema(description = "Limite mínimo para o teto de dias por evento.")
  @UISchema(
      label = "Teto mínimo em dias",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "vertical_align_bottom",
      group = "Limites e Governança",
      order = 100,
      helpText = "Filtra regras cujo teto por evento seja igual ou superior ao valor.")
  private BigDecimal qtdMaxDiasMin;

  @Filterable(operation = FilterOperation.LESS_OR_EQUAL, relation = "qtdMaxDias")
  @Schema(description = "Limite máximo para o teto de dias por evento.")
  @UISchema(
      label = "Teto máximo em dias",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "vertical_align_top",
      group = "Limites e Governança",
      order = 110,
      helpText = "Filtra regras cujo teto por evento seja igual ou inferior ao valor.")
  private BigDecimal qtdMaxDiasMax;

  @Filterable(operation = FilterOperation.EQUAL, relation = "exigeHomologacao")
  @Schema(description = "Filtra regras conforme exigência de homologação.")
  @UISchema(
      label = "Exige homologação",
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "fact_check",
      group = "Limites e Governança",
      order = 120,
      helpText = "Filtra regras que exigem aprovação formal.")
  private Boolean exigeHomologacao;

  @Filterable(operation = FilterOperation.EQUAL, relation = "permiteConcomitancia")
  @Schema(description = "Filtra regras conforme permissão de concomitância.")
  @UISchema(
      label = "Permite concomitância",
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "layers",
      group = "Limites e Governança",
      order = 130,
      helpText = "Filtra regras que admitem sobreposição com outro afastamento.")
  private Boolean permiteConcomitancia;

  @Filterable(operation = FilterOperation.EQUAL, relation = "naoSeAplica")
  @Schema(description = "Filtra regras de vedação expressa para o perfil funcional.")
  @UISchema(
      label = "Vedado para o perfil",
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "block",
      group = "Limites e Governança",
      order = 140,
      helpText = "Filtra regras que vedam o código para o perfil selecionado.")
  private Boolean naoSeAplica;

  @Filterable(operation = FilterOperation.EQUAL, relation = "documentoLegalId")
  @Schema(description = "Filtra regras pelo documento legal fundamentador.")
  @UISchema(
      label = "Documento legal",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "MenuBook",
      group = "Limites e Governança",
      order = 150,
      endpoint = ApiPaths.DOCUMENTO_OPTIONS,
      valueField = "id",
      displayField = "label",
      helpText = "Filtra pelo ato normativo que instituiu a regra.")
  private Long documentoLegalId;

  @Filterable(operation = FilterOperation.EQUAL, relation = "ativa")
  @Schema(description = "Filtra regras ativas ou inativas no motor de regras.")
  @UISchema(
      label = "Regra ativa",
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "check_circle",
      group = "Limites e Governança",
      order = 160,
      helpText = "Filtra pelo status de ativação da regra.")
  private Boolean ativa;
}
