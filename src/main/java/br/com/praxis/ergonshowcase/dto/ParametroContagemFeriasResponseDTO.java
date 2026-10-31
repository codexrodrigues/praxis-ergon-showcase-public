package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.praxisplatform.uischema.*;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.customfields.CustomFields;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.extension.annotation.UISchemaPreset;

@Getter
@Setter
@ResourceIdentity(
    keyField = "id",
    titleField = "regimeJuridico",
    displayLabelField = "regimeJuridico",
    subtitleField = "categoria",
    metadataFields = {"tipoVinculo", "dtini", "totDiasFer", "active"})
@Schema(
    description =
        "Projeção completa da matriz estatutária de parâmetros de contagem de férias, abono pecuniário, prescrição, contagem em dobro, licença especial e progressão funcional.")
public class ParametroContagemFeriasResponseDTO {

  @Schema(
      description = "Identificador único surrogate da regra de contagem de férias.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "ID da regra",
      helpText = "Chave surrogate imutável do parâmetro de contagem de férias.",
      preset = UISchemaPreset.ENTERPRISE_ID,
      group = "Escopo e Vigência",
      order = 1,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "tag",
      readOnly = true,
      tableHidden = true,
      formHidden = true)
  private Long id;

  @ResourceReference(
      resourceKey = "administracao-pessoal.empresas",
      valueField = "empCodigo",
      surfaceId = "ficha-institucional",
      previewSurfaceId = "ficha-institucional",
      detailPresentation = "drawer")
  @Schema(
      description =
          "Código do órgão público ao qual a regra se aplica; nulo quando for regra geral multi-órgão.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Órgão de aplicação",
      helpText = "Órgão público destinatário ou vazio para regra geral.",
      group = "Escopo e Vigência",
      order = 10,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "apartment",
      endpoint = ApiPaths.ORGAO_OPTIONS,
      valueField = "id",
      displayField = "label",
      readOnly = true,
      tableHidden = true)
  private Integer empCodigo;

  @ResourceReference(
      resourceKey = "administracao-pessoal.empresas",
      valueField = "empCodigo",
      surfaceId = "ficha-institucional",
      previewSurfaceId = "ficha-institucional",
      detailPresentation = "drawer")
  @Schema(
      description = "Nome oficial do órgão público quando a regra é específica de um órgão.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Nome do órgão",
      helpText = "Denominação do órgão público de aplicação da regra.",
      group = "Escopo e Vigência",
      order = 15,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "domain",
      readOnly = true,
      tableHidden = true,
      formHidden = true,
      quickViewVisible = true,
      quickViewOrder = 1)
  private String orgaoNome;

  @ResourceReference(
      resourceKey = "administracao-pessoal.parametros-contagem-ferias",
      valueField = "id",
      surfaceId = "ficha-parametro-contagem-ferias",
      previewSurfaceId = "ficha-parametro-contagem-ferias",
      detailPresentation = "drawer")
  @Schema(
      description =
          "Regime jurídico funcional abrangido pela parametrização de férias (ex.: ESTATUTARIO, CLT, TEMPORARIO).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Regime jurídico",
      helpText = "Regime jurídico funcional regido por esta matriz de férias.",
      group = "Escopo e Vigência",
      order = 20,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "gavel",
      readOnly = true,
      tableHidden = false)
  private String regimeJuridico;

  @Schema(
      description =
          "Tipo de vínculo funcional abrangido pela regra (ex.: EFETIVO, COMISSIONADO, CONTRATADO).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Tipo de vínculo",
      helpText = "Modalidade do vínculo funcional a que se aplica a contagem.",
      group = "Escopo e Vigência",
      order = 30,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "work",
      readOnly = true,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 2)
  private String tipoVinculo;

  @Schema(
      description =
          "Categoria funcional do servidor abrangida pela regra (ex.: ADMINISTRATIVO, MAGISTERIO, SAUDE, POLICIAL).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Categoria funcional",
      helpText = "Categoria funcional de carreira para aplicação dos limites de férias.",
      group = "Escopo e Vigência",
      order = 40,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "category",
      readOnly = true,
      tableHidden = false)
  private String categoria;

  @Schema(
      description =
          "Subcategoria funcional opcional para diferenciação fina de regras de férias dentro da mesma carreira.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Subcategoria funcional",
      helpText = "Subdivisão opcional da categoria funcional.",
      group = "Escopo e Vigência",
      order = 50,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "layers",
      readOnly = true,
      tableHidden = true)
  private String subcategoria;

  @Schema(
      description =
          "Data de início de vigência normativa da matriz de parâmetros de contagem de férias.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Início da vigência",
      helpText = "Data inicial a partir da qual esta regra estatutária produz efeitos.",
      group = "Escopo e Vigência",
      order = 60,
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "event",
      readOnly = true,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 3)
  private LocalDate dtini;

  @Schema(
      description =
          "Data de término de vigência da regra; quando nula, indica parametrização vigente por prazo indeterminado.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Fim da vigência",
      helpText = "Data final de vigência ou vazio para regra ativa sem término.",
      group = "Escopo e Vigência",
      order = 70,
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "event_busy",
      readOnly = true,
      tableHidden = true)
  private LocalDate dtfim;

  @Schema(
      description =
          "Indica se a combinação de regime, vínculo e categoria confere direito a férias periódicas remuneradas.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Possui direito a férias",
      helpText = "Habilita a aquisição e o gozo de férias para este enquadramento.",
      group = "Direito e Parcelamento",
      order = 80,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "beach_access",
      readOnly = true,
      tableHidden = true)
  private Boolean temFerias;

  @MicroVisualization(
      kind = MicroVisualizationKind.BULLET,
      max = 60,
      total = 60,
      fallbackText = "Sem direito",
      ariaLabel = "Total de dias de direito a férias por período aquisitivo",
      thresholds = {
        @Threshold(min = 0, max = 20, tone = "warning", label = "Reduzido"),
        @Threshold(min = 20, max = 35, tone = "positive", label = "Ordinário"),
        @Threshold(min = 35, tone = "info", label = "Especial")
      })
  @Schema(
      description =
          "Total padrão de dias de direito a férias por período aquisitivo completo (ex.: 30 dias ordinários ou 45 dias para magistério).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Total de dias de direito",
      helpText = "Quantidade padrão de dias de férias adquiridos em cada ciclo completo.",
      group = "Direito e Parcelamento",
      order = 90,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "calendar_month",
      readOnly = true,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 4)
  private Integer totDiasFer;

  @Schema(
      description =
          "Quantidade mínima de parcelas em que o período aquisitivo de férias deve ser usufruído.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Mínimo de parcelas",
      helpText = "Menor número de etapas de gozo exigido pela regra estatutária.",
      group = "Direito e Parcelamento",
      order = 100,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "splitscreen",
      readOnly = true,
      tableHidden = true)
  private Integer minPerFer;

  @Schema(
      description =
          "Quantidade máxima de parcelas permitida para fracionamento do gozo de um mesmo período aquisitivo.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Máximo de parcelas",
      helpText = "Limite máximo de etapas para fracionamento das férias.",
      group = "Direito e Parcelamento",
      order = 110,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "splitscreen",
      readOnly = true,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 5)
  private Integer maxPerFer;

  @Schema(
      description =
          "Quantidade mínima de dias exigida em cada parcela individual de gozo de férias.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Mínimo de dias por parcela",
      helpText = "Menor duração permitida para qualquer fração de gozo de férias.",
      group = "Direito e Parcelamento",
      order = 120,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "timer",
      readOnly = true,
      tableHidden = true)
  private Integer minDiasPerFer;

  @Schema(
      description =
          "Quantidade máxima de dias permitida em uma única parcela de gozo de férias.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Máximo de dias por parcela",
      helpText = "Maior duração permitida para uma parcela individual de gozo.",
      group = "Direito e Parcelamento",
      order = 130,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "timer",
      readOnly = true,
      tableHidden = true)
  private Integer maxDiasPerFer;

  @Schema(
      description =
          "Quantidade mínima de dias exigida em pelo menos uma parcela principal quando houver fracionamento.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Mínimo na parcela principal",
      helpText = "Duração mínima obrigatória na parcela principal do período aquisitivo.",
      group = "Direito e Parcelamento",
      order = 140,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "verified",
      readOnly = true,
      tableHidden = true)
  private Integer minDiasPerPrincFer;

  @Schema(
      description =
          "Base de cálculo do abono pecuniário de férias: D (sobre dias de direito do PA), G (sobre dias de gozo da parcela) ou N (não permite abono).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Base do abono pecuniário",
      helpText = "Indica D (dias de direito), G (dias de gozo) ou N (sem abono pecuniário).",
      group = "Abono e Financeiro",
      order = 150,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      icon = "sell",
      readOnly = true,
      tableHidden = true)
  private String abonoDirGozo;

  @Schema(
      description =
          "Numerador da fração regulamentar de conversão de férias em abono pecuniário.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Numerador da fração de abono",
      helpText = "Numerador da proporção máxima conversível em pecúnia.",
      group = "Abono e Financeiro",
      order = 160,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "percent",
      readOnly = true,
      tableHidden = true)
  private Integer numDiasAbonoFer;

  @Schema(
      description =
          "Denominador da fração regulamentar de conversão de férias em abono pecuniário.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Denominador da fração de abono",
      helpText = "Denominador da proporção máxima conversível em pecúnia.",
      group = "Abono e Financeiro",
      order = 170,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "percent",
      readOnly = true,
      tableHidden = true)
  private Integer denDiasAbonoFer;

  @Schema(
      description =
          "Quantidade mínima de dias de férias exigida para permitir venda de abono pecuniário.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Mínimo de dias para venda",
      helpText = "Menor quantidade de dias que pode ser convertida em abono pecuniário.",
      group = "Abono e Financeiro",
      order = 180,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "payments",
      readOnly = true,
      tableHidden = true)
  private Integer minDiasPvendFer;

  @Schema(
      description =
          "Quantidade máxima de dias de férias que pode ser convertida em abono pecuniário no período aquisitivo.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Máximo de dias vendidos",
      helpText = "Teto de dias conversíveis em abono pecuniário por período aquisitivo.",
      group = "Abono e Financeiro",
      order = 190,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "payments",
      readOnly = true,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 6)
  private Integer maxDiasVendFer;

  @Schema(
      description =
          "Indica se o enquadramento faz jus ao pagamento do terço constitucional de férias.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Terço constitucional",
      helpText = "Habilita o pagamento do adicional constitucional de 1/3 de férias.",
      group = "Abono e Financeiro",
      order = 200,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "account_balance",
      readOnly = true,
      tableHidden = true)
  private Boolean abonoConstFer;

  @Schema(
      description =
          "Indica se o servidor pode solicitar adiantamento de décimo terceiro salário por ocasião das férias.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Permite adiantamento de 13º",
      helpText = "Autoriza antecipação de parcela da gratificação natalina nas férias.",
      group = "Abono e Financeiro",
      order = 210,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "payments",
      readOnly = true,
      tableHidden = true)
  private Boolean adiant13Fer;

  @Schema(
      description =
          "Indica se o servidor pode solicitar adiantamento salarial de férias no processamento da folha.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Permite adiantamento salarial",
      helpText = "Autoriza antecipação salarial de férias na folha de pagamento.",
      group = "Abono e Financeiro",
      order = 220,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "payments",
      readOnly = true,
      tableHidden = true)
  private Boolean adiantSalFer;

  @Schema(
      description =
          "Indica se o servidor pode contratar rubrica de empréstimo ou antecipação restituível de férias.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Permite empréstimo de férias",
      helpText = "Autoriza concessão de empréstimo financeiro vinculado ao gozo de férias.",
      group = "Abono e Financeiro",
      order = 230,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "account_balance",
      readOnly = true,
      tableHidden = true)
  private Boolean emprestFer;

  @Schema(
      description =
          "Indica se é permitido programar ou usufruir férias antes do término do respectivo período aquisitivo.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Gozo antes do fim do PA",
      helpText = "Autoriza fruição de férias com período aquisitivo ainda em aquisição.",
      group = "Gozo Antecipado e Prescrição",
      order = 240,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "history",
      readOnly = true,
      tableHidden = true)
  private Boolean permiteGozoAntesPaFer;

  @Schema(
      description =
          "Indica se é permitido usufruir férias ainda no primeiro período aquisitivo do vínculo funcional.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Permite no 1º período",
      helpText = "Autoriza gozo antecipado durante o primeiro período aquisitivo.",
      group = "Gozo Antecipado e Prescrição",
      order = 250,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "history",
      readOnly = true,
      tableHidden = true)
  private Boolean permiteFerPrimPer;

  @Schema(
      description =
          "Indica se é permitido usufruir férias antecipadas durante o segundo período aquisitivo do vínculo.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Permite no 2º período",
      helpText = "Autoriza gozo antecipado durante o segundo período aquisitivo.",
      group = "Gozo Antecipado e Prescrição",
      order = 260,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "history",
      readOnly = true,
      tableHidden = true)
  private Boolean permiteFerSegunPer;

  @Schema(
      description =
          "Indica se é permitido usufruir férias antecipadas a partir do terceiro período aquisitivo do vínculo.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Permite no 3º período",
      helpText = "Autoriza gozo antecipado no terceiro período aquisitivo e seguintes.",
      group = "Gozo Antecipado e Prescrição",
      order = 270,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "history",
      readOnly = true,
      tableHidden = true)
  private Boolean permiteFerTercPer;

  @Schema(
      description =
          "Indica se é permitido programar férias em período aquisitivo incompleto ou proporcional.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Permite período incompleto",
      helpText = "Autoriza concessão de férias proporcionais em período aquisitivo incompleto.",
      group = "Gozo Antecipado e Prescrição",
      order = 280,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "history",
      readOnly = true,
      tableHidden = true)
  private Boolean permiteFerPerInc;

  @Schema(
      description =
          "Critério de comparação da prescrição do período aquisitivo: I (data inicial do gozo) ou F (data final do gozo).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Critério de prescrição",
      helpText = "Indica I para validar pelo início do gozo ou F pelo término do gozo.",
      group = "Gozo Antecipado e Prescrição",
      order = 290,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      icon = "warning",
      readOnly = true,
      tableHidden = true)
  private String criterioPrescricao;

  @Schema(
      description =
          "Indica se é permitido registrar gozo de férias após a data de prescrição do período aquisitivo.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Permite gozo após prescrição",
      helpText = "Autoriza agendamento de férias mesmo após vencida a data de prescrição.",
      group = "Gozo Antecipado e Prescrição",
      order = 300,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "warning",
      readOnly = true,
      tableHidden = true)
  private Boolean permiteGozoAposPrescricao;

  @Schema(
      description =
          "Indica se é permitido lançar períodos aquisitivos concomitantes ou sobrepostos para o mesmo vínculo.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Permite PA concomitante",
      helpText = "Autoriza coexistência de períodos aquisitivos com interseção de datas.",
      group = "Gozo Antecipado e Prescrição",
      order = 310,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "layers",
      readOnly = true,
      tableHidden = true)
  private Boolean permiteLancConcPa;

  @Schema(
      description =
          "Indica se o preenchimento do ano de exercício de referência é obrigatório no período aquisitivo.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Exige ano de exercício",
      helpText = "Torna obrigatório informar o exercício de referência na abertura do PA.",
      group = "Gozo Antecipado e Prescrição",
      order = 320,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "calendar_month",
      readOnly = true,
      tableHidden = true)
  private Boolean exigeExercicio;

  @Schema(
      description =
          "Indica isenção de pagamento de dobra/multa por concessão de férias fora do prazo concessivo.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Isenta multa concessiva",
      helpText = "Suprime o pagamento de multa por atraso na concessão das férias.",
      group = "Gozo Antecipado e Prescrição",
      order = 330,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "gavel",
      readOnly = true,
      tableHidden = true)
  private Boolean semPgtoMulta;

  @Schema(
      description =
          "Indica se o saldo de férias não gozado pode ser averbado para contagem em dobro de tempo de serviço.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Permite contagem em dobro",
      helpText = "Autoriza converter dias de férias não gozados em contagem em dobro.",
      group = "Gozo Antecipado e Prescrição",
      order = 340,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "trending_up",
      readOnly = true,
      tableHidden = true)
  private Boolean contDobroFerias;

  @Schema(
      description =
          "Quantidade mínima de dias do período aquisitivo passível de conversão para contagem em dobro.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Mínimo para contagem em dobro",
      helpText = "Menor saldo de dias do PA que pode ser convertido em dobro.",
      group = "Gozo Antecipado e Prescrição",
      order = 350,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "trending_up",
      readOnly = true,
      tableHidden = true)
  private Integer minDiasPeraqfer;

  @Schema(
      description =
          "Quantidade máxima de dias do período aquisitivo passível de conversão para contagem em dobro.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Máximo para contagem em dobro",
      helpText = "Maior saldo de dias do PA que pode ser convertido em dobro.",
      group = "Gozo Antecipado e Prescrição",
      order = 360,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "trending_up",
      readOnly = true,
      tableHidden = true)
  private Integer maxDiasPeraqfer;

  @Schema(
      description =
          "Indica se o enquadramento estatutário faz jus à aquisição de licença-prêmio ou licença especial por assiduidade.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Possui licença especial",
      helpText = "Habilita apuração de quinquênios/períodos de licença especial.",
      group = "Licença Especial e Progressão",
      order = 370,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "workspace_premium",
      readOnly = true,
      tableHidden = true)
  private Boolean temLicEsp;

  @Schema(
      description =
          "Quantidade de anos de efetivo exercício exigida para adquirir cada período de licença especial.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Anos por licença especial",
      helpText = "Interstício em anos para aquisição de licença especial.",
      group = "Licença Especial e Progressão",
      order = 380,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "schedule",
      readOnly = true,
      tableHidden = true)
  private Integer anosLicEsp;

  @Schema(
      description =
          "Quantidade de meses de direito concedida a cada período adquirido de licença especial.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Meses de direito da licença",
      helpText = "Duração em meses concedida por ciclo de licença especial.",
      group = "Licença Especial e Progressão",
      order = 390,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "calendar_month",
      readOnly = true,
      tableHidden = true)
  private Integer mesesLicEspPer;

  @Schema(
      description =
          "Indica se os dias de licença especial adquiridos podem ser convertidos em pecúnia indenizatória.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Permite venda de licença",
      helpText = "Autoriza conversão de saldo de licença especial em abono pecuniário.",
      group = "Licença Especial e Progressão",
      order = 400,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "sell",
      readOnly = true,
      tableHidden = true)
  private Boolean vendLicEsp;

  @Schema(
      description =
          "Base de cálculo para conversão de licença especial em pecúnia: D (dias de direito), G (dias de gozo) ou N (vedado).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Base de abono da licença",
      helpText = "Indica D (direito), G (gozo) ou N (sem conversão da licença).",
      group = "Licença Especial e Progressão",
      order = 410,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      icon = "sell",
      readOnly = true,
      tableHidden = true)
  private String abonoDirGozoLicEsp;

  @Schema(
      description =
          "Quantidade mínima de dias de licença especial que pode ser convertida em pecúnia.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Mínimo venda licença especial",
      helpText = "Menor quantidade de dias de licença especial conversível em pecúnia.",
      group = "Licença Especial e Progressão",
      order = 420,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "payments",
      readOnly = true,
      tableHidden = true)
  private Integer minDiasPvendLicEsp;

  @Schema(
      description =
          "Quantidade máxima de dias de licença especial que pode ser convertida em pecúnia.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Máximo venda licença especial",
      helpText = "Teto de dias de licença especial conversível em pecúnia.",
      group = "Licença Especial e Progressão",
      order = 430,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "payments",
      readOnly = true,
      tableHidden = true)
  private Integer maxDiasVendLicEsp;

  @Schema(
      description =
          "Numerador da fração regulamentar de conversão da licença especial em abono pecuniário.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Numerador abono da licença",
      helpText = "Numerador da fração máxima de conversão da licença especial.",
      group = "Licença Especial e Progressão",
      order = 440,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "percent",
      readOnly = true,
      tableHidden = true)
  private Integer numDiasAbonoLicEsp;

  @Schema(
      description =
          "Denominador da fração regulamentar de conversão da licença especial em abono pecuniário.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Denominador abono da licença",
      helpText = "Denominador da fração máxima de conversão da licença especial.",
      group = "Licença Especial e Progressão",
      order = 450,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "percent",
      readOnly = true,
      tableHidden = true)
  private Integer denDiasAbonoLicEsp;

  @Schema(
      description =
          "Indica se o saldo não usufruído de licença especial pode ser contado em dobro para tempo de serviço.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Contagem em dobro da licença",
      helpText = "Autoriza averbar licença especial não gozada em dobro.",
      group = "Licença Especial e Progressão",
      order = 460,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "trending_up",
      readOnly = true,
      tableHidden = true)
  private Boolean contDobroLicEsp;

  @Schema(
      description =
          "Quantidade mínima de dias de licença especial exigida para averbação em dobro.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Mínimo dobro licença especial",
      helpText = "Menor saldo de dias de licença especial para contagem em dobro.",
      group = "Licença Especial e Progressão",
      order = 470,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "trending_up",
      readOnly = true,
      tableHidden = true)
  private Integer minDiasPcontDobLicEsp;

  @Schema(
      description =
          "Quantidade máxima de dias de licença especial permitida para averbação em dobro.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Máximo dobro licença especial",
      helpText = "Teto de dias de licença especial para contagem em dobro.",
      group = "Licença Especial e Progressão",
      order = 480,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "trending_up",
      readOnly = true,
      tableHidden = true)
  private Integer maxDiasContDobLicEsp;

  @Schema(
      description =
          "Indica se o enquadramento estatutário habilita apuração automática de interstício de progressão funcional.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Possui progressão funcional",
      helpText = "Habilita contagem de tempo para progressão na carreira.",
      group = "Licença Especial e Progressão",
      order = 490,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "trending_up",
      readOnly = true,
      tableHidden = true)
  private Boolean temProgressao;

  @Schema(
      description =
          "Interstício em anos exigido para cada ciclo de progressão funcional na carreira.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Anos para progressão",
      helpText = "Quantidade de anos exigida para progressão funcional.",
      group = "Licença Especial e Progressão",
      order = 500,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "schedule",
      readOnly = true,
      tableHidden = true)
  private Integer anosProgressao;

  @Schema(
      description =
          "Indica se o enquadramento estatutário habilita contagem de adicional por tempo de serviço.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Possui adicional de tempo",
      helpText = "Habilita apuração de adicional por tempo de serviço.",
      group = "Licença Especial e Progressão",
      order = 510,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "schedule",
      readOnly = true,
      tableHidden = true)
  private Boolean temTempoServ;

  @Schema(
      description =
          "Quantidade de anos exigida para o primeiro ciclo de adicional por tempo de serviço.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Anos para 1º adicional",
      helpText = "Interstício em anos para aquisição do primeiro adicional de tempo.",
      group = "Licença Especial e Progressão",
      order = 520,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "schedule",
      readOnly = true,
      tableHidden = true)
  private Integer anos1TempoServ;

  @Schema(
      description =
          "Indica se o enquadramento estatutário habilita apuração do benefício constitucional de sexta-parte.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Possui sexta-parte",
      helpText = "Habilita contagem de tempo para concessão de sexta-parte.",
      group = "Licença Especial e Progressão",
      order = 530,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "workspace_premium",
      readOnly = true,
      tableHidden = true)
  private Boolean temSextaParte;

  @Schema(
      description =
          "Quantidade de anos de efetivo exercício exigida para aquisição da sexta-parte.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Anos para sexta-parte",
      helpText = "Interstício em anos para concessão de sexta-parte.",
      group = "Licença Especial e Progressão",
      order = 540,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "schedule",
      readOnly = true,
      tableHidden = true)
  private Integer anosSextaParte;

  @ResourceReference(
      resourceKey = "administracao-pessoal.documentos-legais",
      valueField = "documentoLegalFeriasId",
      surfaceId = "ficha-documento",
      previewSurfaceId = "ficha-documento",
      detailPresentation = "drawer")
  @Schema(
      description =
          "Identificador do documento legal ou estatuto que fundamenta a regra de contagem de férias.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Ato legal de férias",
      helpText = "Legislação ou estatuto que regulamenta a contagem de férias.",
      group = "Fundamentação e Governança",
      order = 550,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "description",
      endpoint = ApiPaths.DOCUMENTO_OPTIONS,
      valueField = "id",
      displayField = "label",
      readOnly = true,
      tableHidden = true)
  private Long documentoLegalFeriasId;

  @ResourceReference(
      resourceKey = "administracao-pessoal.documentos-legais",
      valueField = "documentoLegalFeriasId",
      surfaceId = "ficha-documento",
      previewSurfaceId = "ficha-documento",
      detailPresentation = "drawer")
  @Schema(
      description = "Número oficial do ato legal que fundamenta a regra de contagem de férias.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Número do ato de férias",
      helpText = "Identificação oficial da lei ou portaria das regras de férias.",
      group = "Fundamentação e Governança",
      order = 555,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "description",
      readOnly = true,
      tableHidden = true,
      formHidden = true)
  private String documentoLegalFeriasNumero;

  @ResourceReference(
      resourceKey = "administracao-pessoal.documentos-legais",
      valueField = "documentoLegalProgressaoId",
      surfaceId = "ficha-documento",
      previewSurfaceId = "ficha-documento",
      detailPresentation = "drawer")
  @Schema(
      description =
          "Identificador do documento legal que fundamenta os interstícios de progressão e tempo de serviço.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Ato legal de progressão",
      helpText = "Legislação que regulamenta progressão funcional e adicionais de tempo.",
      group = "Fundamentação e Governança",
      order = 560,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "gavel",
      endpoint = ApiPaths.DOCUMENTO_OPTIONS,
      valueField = "id",
      displayField = "label",
      readOnly = true,
      tableHidden = true)
  private Long documentoLegalProgressaoId;

  @ResourceReference(
      resourceKey = "administracao-pessoal.documentos-legais",
      valueField = "documentoLegalProgressaoId",
      surfaceId = "ficha-documento",
      previewSurfaceId = "ficha-documento",
      detailPresentation = "drawer")
  @Schema(
      description = "Número oficial do ato legal que fundamenta a progressão e o tempo de serviço.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Número do ato de progressão",
      helpText = "Identificação oficial da norma de progressão funcional.",
      group = "Fundamentação e Governança",
      order = 565,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "gavel",
      readOnly = true,
      tableHidden = true,
      formHidden = true)
  private String documentoLegalProgressaoNumero;

  @Schema(
      description =
          "Observações técnicas, diretrizes de aplicação e notas normativas sobre a parametrização estatutária.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Observações normativas",
      helpText = "Anotações interpretativas e diretrizes de RH sobre esta regra.",
      group = "Fundamentação e Governança",
      order = 570,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.TEXTAREA,
      icon = "notes",
      readOnly = true,
      tableHidden = true)
  private String observacoes;

  @Schema(
      description = "Indica se a matriz de parâmetros de contagem de férias está ativa.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Ativo",
      helpText = "Indica se a parametrização encontra-se ativa para aplicação.",
      group = "Fundamentação e Governança",
      order = 575,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "toggle_on",
      readOnly = true,
      tableHidden = false)
  private Boolean active;

  @Schema(
      description =
          "Fato derivado que indica se a regra estatutária vale globalmente para todos os órgãos por não restringir empCodigo.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Regra de escopo global",
      helpText = "Verdadeiro quando a regra vale para todos os órgãos.",
      group = "Enquadramento Estatutário e Vigência",
      order = 18,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "public",
      readOnly = true,
      tableHidden = true)
  private Boolean escopoGlobal;

  @Schema(
      description =
          "Fato derivado que indica se a regra estatutária autoriza o parcelamento das férias em duas ou mais etapas.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Permite parcelamento",
      helpText = "Verdadeiro quando o máximo de parcelas autoriza mais de uma etapa.",
      group = "Direito e Parcelamento de Férias",
      order = 95,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "splitscreen",
      readOnly = true,
      tableHidden = true)
  private Boolean permiteParcelamento;

  @Schema(
      description =
          "Fato derivado que indica se a regra estatutária permite converter parte das férias em abono pecuniário.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Permite abono pecuniário",
      helpText = "Verdadeiro quando a regra habilita conversão em abono pecuniário.",
      group = "Abono Pecuniário e Opções Financeiras",
      order = 135,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "sell",
      readOnly = true,
      tableHidden = true)
  private Boolean permiteAbonoPecuniario;

  @Schema(
      description =
          "Atributos adicionais dinâmicos da parametrização de férias governados pelo catálogo de campos customizados.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @CustomFields(resourceKey = "administracao-pessoal.parametros-contagem-ferias")
  @UISchema(
      label = "Campos customizados",
      helpText = "Atributos específicos configurados pelo órgão para regras de férias.",
      group = "Campos Customizados",
      order = 580,
      readOnly = true,
      tableHidden = true)
  private Map<String, Object> customFields = new LinkedHashMap<>();
}
