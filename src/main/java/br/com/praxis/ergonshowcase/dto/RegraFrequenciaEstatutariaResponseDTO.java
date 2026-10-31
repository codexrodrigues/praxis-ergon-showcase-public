package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.domain.CodigoFrequenciaId;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.praxisplatform.uischema.*;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.extension.annotation.UISchemaPreset;

@Getter
@Setter
@ResourceIdentity(
    keyField = "id",
    titleField = "ruleKey",
    displayLabelField = "ruleKey",
    subtitleField = "codigoFrequenciaNome",
    metadataFields = {"regimeJuridico", "tipoVinculo", "categoria", "ativa"})
@Schema(
    description =
        "Projeção completa da matriz parametrizada de regras e limites estatutários de frequência e afastamento por órgão, regime jurídico, tipo de vínculo e categoria.")
public class RegraFrequenciaEstatutariaResponseDTO {

  @Schema(
      description = "Identificador único surrogate da regra estatutária de frequência.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "ID da regra",
      preset = UISchemaPreset.ENTERPRISE_ID,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "tag",
      group = "Identificação",
      order = 1,
      readOnly = true,
      tableHidden = true,
      formHidden = true,
      filterable = true,
      sortable = true,
      helpText = "Chave técnica surrogate imutável da regra estatutária.")
  private Long id;

  @OptionLabel
  @ResourceReference(
      resourceKey = "administracao-pessoal.regras-frequencia-estatutaria",
      valueField = "id",
      surfaceId = "ficha-regra-frequencia-estatutaria",
      previewSurfaceId = "ficha-regra-frequencia-estatutaria",
      detailPresentation = "drawer")
  @Schema(
      description =
          "Chave canônica do RuleSet no motor de regras que consome esta parametrização estatutária.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Chave da regra",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "gavel",
      group = "Identificação",
      order = 10,
      readOnly = true,
      tableHidden = false,
      filterable = true,
      sortable = true,
      helpText = "Identificador canônico do RuleSet avaliado pelo motor de regras.")
  private String ruleKey;

  @ResourceReference(
      resourceKey = "administracao-pessoal.empresas",
      valueField = "empCodigo",
      surfaceId = "ficha-institucional",
      previewSurfaceId = "ficha-institucional",
      detailPresentation = "drawer")
  @Schema(
      description =
          "Código do órgão público ao qual a regra se aplica ou nulo quando vale para todos os órgãos.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Órgão",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "apartment",
      group = "Abrangência Estatutária",
      order = 20,
      readOnly = true,
      tableHidden = true,
      filterable = true,
      sortable = true,
      endpoint = ApiPaths.ORGAO_OPTIONS,
      valueField = "id",
      displayField = "label",
      helpText = "Órgão público específico da regra ou vazio quando aplicável a todo o ente.")
  private Integer empCodigo;

  @ResourceReference(
      resourceKey = "administracao-pessoal.empresas",
      valueField = "empCodigo",
      surfaceId = "ficha-institucional",
      previewSurfaceId = "ficha-institucional",
      detailPresentation = "drawer")
  @Schema(
      description = "Nome do órgão público ao qual a regra estatutária se aplica.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Nome do órgão",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "domain",
      group = "Abrangência Estatutária",
      order = 25,
      readOnly = true,
      tableHidden = true,
      quickViewVisible = true,
      quickViewOrder = 10,
      sortable = true,
      helpText = "Denominação oficial do órgão público abrangido pela regra.")
  private String orgaoNome;

  @Schema(
      description =
          "Regime jurídico alvo da regra estatutária de frequência, como ESTATUTARIO ou CLT.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Regime jurídico",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      icon = "balance",
      group = "Abrangência Estatutária",
      order = 30,
      readOnly = true,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 20,
      filterable = true,
      sortable = true,
      options =
          "[{\"id\":\"ESTATUTARIO\",\"label\":\"Estatutário\"},{\"id\":\"CLT\",\"label\":\"Celetista\"},{\"id\":\"ESPECIAL\",\"label\":\"Regime Especial\"}]",
      valueField = "id",
      displayField = "label",
      helpText = "Regime jurídico ao qual se aplicam os limites de concessão.")
  private String regimeJuridico;

  @Schema(
      description =
          "Tipo de vínculo funcional alvo da regra estatutária, como EFETIVO, COMISSAO ou TEMPORARIO.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Tipo de vínculo",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      icon = "badge",
      group = "Abrangência Estatutária",
      order = 40,
      readOnly = true,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 30,
      filterable = true,
      sortable = true,
      options =
          "[{\"id\":\"EFETIVO\",\"label\":\"Efetivo\"},{\"id\":\"COMISSAO\",\"label\":\"Comissionado\"},{\"id\":\"TEMPORARIO\",\"label\":\"Temporário\"},{\"id\":\"TODOS\",\"label\":\"Todos os vínculos\"}]",
      valueField = "id",
      displayField = "label",
      helpText = "Natureza do vínculo funcional regulado pelo parâmetro.")
  private String tipoVinculo;

  @Schema(
      description =
          "Categoria funcional alvo da regra estatutária, como GERAL, MAGISTERIO ou GUARDA_MUNICIPAL.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Categoria funcional",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      icon = "groups",
      group = "Abrangência Estatutária",
      order = 50,
      readOnly = true,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 40,
      filterable = true,
      sortable = true,
      options =
          "[{\"id\":\"GERAL\",\"label\":\"Quadro Geral\"},{\"id\":\"MAGISTERIO\",\"label\":\"Magistério\"},{\"id\":\"GUARDA_MUNICIPAL\",\"label\":\"Guarda Municipal\"},{\"id\":\"SAUDE\",\"label\":\"Saúde\"},{\"id\":\"TODAS\",\"label\":\"Todas as categorias\"}]",
      valueField = "id",
      displayField = "label",
      helpText = "Categoria funcional abrangida pelo limite estatutário.")
  private String categoria;

  @Schema(
      description =
          "Subcategoria funcional específica quando a regra distingue especialidades de carreira.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Subcategoria",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "label",
      group = "Abrangência Estatutária",
      order = 60,
      readOnly = true,
      tableHidden = true,
      filterable = true,
      sortable = true,
      helpText = "Subcategoria específica de carreira, quando aplicável.")
  private String subcategoria;

  @ResourceReference(
      resourceKey = "administracao-pessoal.tipos-frequencia",
      valueField = "tipofreq",
      surfaceId = "ficha-tipo-frequencia",
      previewSurfaceId = "ficha-tipo-frequencia",
      detailPresentation = "drawer")
  @Schema(
      description = "Tipo macro de frequência regulado pela regra estatutária.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Tipo de frequência",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "category",
      group = "Tipificação e Vigência",
      order = 70,
      readOnly = true,
      tableHidden = true,
      filterable = true,
      sortable = true,
      endpoint = ApiPaths.TIPO_FREQUENCIA_OPTIONS,
      valueField = "id",
      displayField = "label",
      helpText = "Agrupador macro da ocorrência ou licença regulada.")
  private String tipofreq;

  @ResourceReference(
      resourceKey = "administracao-pessoal.tipos-frequencia",
      valueField = "tipofreq",
      surfaceId = "ficha-tipo-frequencia",
      previewSurfaceId = "ficha-tipo-frequencia",
      detailPresentation = "drawer")
  @Schema(
      description = "Descrição do tipo macro de frequência regulado pela regra.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Descrição do tipo",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "description",
      group = "Tipificação e Vigência",
      order = 75,
      readOnly = true,
      tableHidden = true,
      sortable = true,
      helpText = "Denominação do tipo macro de frequência.")
  private String tipoFrequenciaDescricao;

  @Schema(
      description = "Código específico de frequência ou afastamento regulado pela regra.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Código de frequência",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "pin",
      group = "Tipificação e Vigência",
      order = 80,
      readOnly = true,
      tableHidden = true,
      filterable = true,
      sortable = true,
      endpoint = ApiPaths.CODIGO_FREQUENCIA_OPTIONS,
      valueField = "id",
      displayField = "label",
      helpText = "Código numérico da ocorrência ou licença no catálogo de frequência.")
  private Integer codfreq;

  @Schema(
      description = "Identidade composta do código de frequência regulado pela regra.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Chave do código de frequência",
      group = "Tipificação e Vigência",
      order = 82,
      readOnly = true,
      tableHidden = true,
      formHidden = true,
      helpText = "Identificador composto no catálogo de códigos de frequência.")
  private CodigoFrequenciaId codigoFrequenciaId;

  @ResourceReference(
      resourceKey = "administracao-pessoal.codigos-frequencia",
      valueField = "codigoFrequenciaId",
      surfaceId = "ficha-codigo-frequencia",
      previewSurfaceId = "ficha-codigo-frequencia",
      detailPresentation = "drawer")
  @Schema(
      description = "Denominação oficial do código de frequência ou afastamento regulado.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Modalidade regulada",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "event_note",
      group = "Tipificação e Vigência",
      order = 85,
      readOnly = true,
      tableHidden = false,
      sortable = true,
      helpText = "Nome oficial da modalidade de frequência ou licença.")
  private String codigoFrequenciaNome;

  @Schema(
      description = "Data de início de vigência legal do parâmetro estatutário.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Início da vigência",
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "event_available",
      group = "Tipificação e Vigência",
      order = 90,
      readOnly = true,
      tableHidden = true,
      filterable = true,
      sortable = true,
      helpText = "Data a partir da qual o limite estatutário produz efeitos.")
  private LocalDate dtini;

  @Schema(
      description =
          "Data de encerramento da vigência legal do parâmetro estatutário, quando revogado ou substituído.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Fim da vigência",
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "event_busy",
      group = "Tipificação e Vigência",
      order = 100,
      readOnly = true,
      tableHidden = true,
      filterable = true,
      sortable = true,
      helpText = "Data final de vigência da regra ou vazio quando em vigor.")
  private LocalDate dtfim;

  @Schema(
      description = "Duração mínima exigida em dias por evento de concessão.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Mínimo por evento em dias",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "vertical_align_bottom",
      group = "Limites Quantitativos",
      order = 110,
      readOnly = true,
      tableHidden = true,
      sortable = true,
      helpText = "Quantidade mínima de dias exigida para concessão do evento.")
  private BigDecimal qtdMinDias;

  @MicroVisualization(
      kind = MicroVisualizationKind.BULLET,
      valueExpr = "qtdMaxDias",
      min = 0,
      max = 365,
      total = 365,
      valueSuffix = " d",
      compactValue = true,
      fallbackText = "Sem teto por evento",
      ariaLabel = "Teto máximo de dias por evento em relação ao ciclo anual",
      thresholds = {
        @Threshold(min = 1, max = 30, tone = "success", label = "Curta duração"),
        @Threshold(min = 30, max = 180, tone = "warning", label = "Média duração"),
        @Threshold(min = 180, tone = "danger", label = "Longa duração")
      })
  @Schema(
      description = "Duração máxima permitida em dias por evento de concessão.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Máximo por evento em dias",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "vertical_align_top",
      group = "Limites Quantitativos",
      order = 120,
      readOnly = true,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 50,
      filterable = true,
      sortable = true,
      helpText = "Teto máximo de dias permitido em cada ato individual de concessão.")
  private BigDecimal qtdMaxDias;

  @Schema(
      description =
          "Interstício mínimo obrigatório em dias entre duas concessões da mesma espécie.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Interstício mínimo em dias",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "timelapse",
      group = "Limites Quantitativos",
      order = 130,
      readOnly = true,
      tableHidden = true,
      filterable = true,
      sortable = true,
      helpText = "Intervalo mínimo em dias exigido entre o término de uma concessão e a seguinte.")
  private Integer intervMinDias;

  @Schema(
      description =
          "Prazo limite em dias após o término para que novo lançamento seja caracterizado como prorrogação.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Janela de prorrogação em dias",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "update",
      group = "Limites Quantitativos",
      order = 140,
      readOnly = true,
      tableHidden = true,
      sortable = true,
      helpText = "Quantidade máxima de dias entre eventos para caracterizar prorrogação contínua.")
  private Integer intProrrogDias;

  @Schema(
      description =
          "Limite máximo de dias consecutivos permitidos dentro do mesmo mês competência.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Máximo consecutivo no mês",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "date_range",
      group = "Limites Mensais e Anuais",
      order = 150,
      readOnly = true,
      tableHidden = true,
      sortable = true,
      helpText = "Teto de dias ininterruptos permitido dentro da mesma competência mensal.")
  private Integer qtdMaxConsecMes;

  @Schema(
      description =
          "Limite máximo de dias interpolados ou acumulados permitidos dentro do mesmo mês competência.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Máximo interpolado no mês",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "calendar_view_month",
      group = "Limites Mensais e Anuais",
      order = 160,
      readOnly = true,
      tableHidden = true,
      sortable = true,
      helpText = "Teto de dias somados na mesma competência mensal.")
  private Integer qtdMaxInterpMes;

  @Schema(
      description = "Limite máximo de dias consecutivos permitidos dentro do exercício anual.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Máximo consecutivo no ano",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "calendar_today",
      group = "Limites Mensais e Anuais",
      order = 170,
      readOnly = true,
      tableHidden = true,
      sortable = true,
      helpText = "Teto de dias consecutivos permitido dentro do mesmo exercício civil.")
  private Integer qtdMaxConsecAno;

  @Schema(
      description =
          "Limite máximo de dias interpolados ou acumulados permitidos dentro do exercício anual.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Máximo interpolado no ano",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "event_repeat",
      group = "Limites Mensais e Anuais",
      order = 180,
      readOnly = true,
      tableHidden = true,
      sortable = true,
      helpText = "Teto acumulado de dias permitido ao longo do exercício civil.")
  private Integer qtdMaxInterpAno;

  @Schema(
      description =
          "Indica se o lançamento deste código exige transição inicial para pendente de homologação.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Exige homologação",
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "verified",
      group = "Governança e Motor de Regras",
      order = 190,
      readOnly = true,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 60,
      filterable = true,
      sortable = true,
      labelTrue = "Exige homologação",
      labelFalse = "Direto",
      toneTrue = "warning",
      toneFalse = "neutral",
      helpText = "Define se o registro nasce pendente de homologação da chefia ou RH.")
  private Boolean exigeHomologacao;

  @Schema(
      description =
          "Indica se a regra admite sobreposição de período com outra ocorrência ou afastamento ativo.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Permite concomitância",
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "layers",
      group = "Governança e Motor de Regras",
      order = 200,
      readOnly = true,
      tableHidden = true,
      filterable = true,
      sortable = true,
      labelTrue = "Permite",
      labelFalse = "Veda",
      toneTrue = "info",
      toneFalse = "neutral",
      helpText = "Indica se pode coexistir no mesmo período de outro afastamento ativo.")
  private Boolean permiteConcomitancia;

  @Schema(
      description =
          "Indica vedação expressa do código de frequência ou afastamento para a combinação de regime, vínculo e categoria.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Vedado para o perfil",
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "block",
      group = "Governança e Motor de Regras",
      order = 210,
      readOnly = true,
      tableHidden = true,
      filterable = true,
      sortable = true,
      labelTrue = "Vedado",
      labelFalse = "Permitido",
      toneTrue = "danger",
      toneFalse = "success",
      helpText = "Sinaliza que a modalidade não se aplica ao regime, vínculo e categoria.")
  private Boolean naoSeAplica;

  @Schema(
      description =
          "Parâmetros estruturados em JSONB consumidos pelos Fact Providers do motor de regras.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Parâmetros da regra",
      type = FieldDataType.JSON,
      controlType = FieldControlType.TEXTAREA,
      icon = "data_object",
      group = "Governança e Motor de Regras",
      order = 220,
      readOnly = true,
      tableHidden = true,
      helpText = "Mapa estruturado de parâmetros adicionais avaliados pelo motor de regras.")
  private Map<String, Object> ruleParams;

  @ResourceReference(
      resourceKey = "administracao-pessoal.documentos-legais",
      valueField = "documentoLegalId",
      surfaceId = "ficha-documento",
      previewSurfaceId = "ficha-documento",
      detailPresentation = "drawer")
  @Schema(
      description =
          "Identificador do documento legal ou artigo estatutário que instituiu o parâmetro.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Documento legal",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "menu_book",
      group = "Governança e Motor de Regras",
      order = 230,
      readOnly = true,
      tableHidden = true,
      filterable = true,
      sortable = true,
      endpoint = ApiPaths.DOCUMENTO_OPTIONS,
      valueField = "id",
      displayField = "label",
      helpText = "Ato normativo ou dispositivo estatutário que fundamenta a regra.")
  private Long documentoLegalId;

  @ResourceReference(
      resourceKey = "administracao-pessoal.documentos-legais",
      valueField = "documentoLegalId",
      surfaceId = "ficha-documento",
      previewSurfaceId = "ficha-documento",
      detailPresentation = "drawer")
  @Schema(
      description = "Número e ementa do documento legal que fundamenta a regra estatutária.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Norma fundamentadora",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "policy",
      group = "Governança e Motor de Regras",
      order = 235,
      readOnly = true,
      tableHidden = true,
      sortable = true,
      helpText = "Identificação do ato legal de amparo da regra.")
  private String documentoLegalNumero;

  @Schema(
      description = "Indica se a regra parametrizada está ativa para avaliação pelo motor de regras.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Regra ativa",
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "check_circle",
      group = "Governança e Motor de Regras",
      order = 240,
      readOnly = true,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 70,
      filterable = true,
      sortable = true,
      labelTrue = "Ativa",
      labelFalse = "Inativa",
      toneTrue = "success",
      toneFalse = "neutral",
      helpText = "Define se o parâmetro está habilitado para execução determinística.")
  private Boolean ativa;
}
