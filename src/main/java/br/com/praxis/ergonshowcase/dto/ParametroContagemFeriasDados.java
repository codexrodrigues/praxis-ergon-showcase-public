package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.annotation.BulkEditable;
import org.praxisplatform.uischema.customfields.CustomFields;
import org.praxisplatform.uischema.extension.annotation.UISchema;

@Getter
@Setter
public abstract class ParametroContagemFeriasDados {
  @NotNull
  @Schema(description = "Permite fruição de licença-prêmio antes do encerramento do período aquisitivo, conforme a Tarefa 103339. Desabilitado por padrão; não antecipa automaticamente a aquisição do direito.", defaultValue = "false")
  @UISchema(label = "Gozo antecipado de licença", helpText = "Habilite somente quando o regime admitir gozo antes do fim da aquisição.", group = "Licença Especial", order = 475, type = FieldDataType.BOOLEAN, controlType = FieldControlType.CHECKBOX, icon = "event_available", tableHidden = true, preset = org.praxisplatform.uischema.extension.annotation.UISchemaPreset.BOOLEAN_FLAG)
  private Boolean permGozoLicEspAntFimPe = false;

  @Schema(
      description =
          "Código do órgão público ao qual a regra se aplica; quando nulo, define regra geral para todos os órgãos.",
      nullable = true)
  @BulkEditable(allowClear = true)
  @UISchema(
      label = "Órgão de aplicação",
      helpText = "Selecione o órgão específico ou deixe em branco para regra geral multi-órgão.",
      group = "Escopo e Vigência",
      order = 10,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "apartment",
      endpoint = ApiPaths.ORGAO_OPTIONS,
      valueField = "id",
      displayField = "label")
  private Integer empCodigo;

  @NotBlank
  @Size(max = 30)
  @Schema(
      description =
          "Regime jurídico funcional abrangido pela parametrização de férias (ex.: ESTATUTARIO, CLT, TEMPORARIO).")
  @BulkEditable
  @UISchema(
      label = "Regime jurídico",
      helpText = "Regime jurídico funcional regido por esta matriz de férias.",
      group = "Escopo e Vigência",
      order = 20,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "gavel")
  private String regimeJuridico;

  @NotBlank
  @Size(max = 30)
  @Schema(
      description =
          "Tipo de vínculo funcional abrangido pela regra (ex.: EFETIVO, COMISSIONADO, CONTRATADO).")
  @BulkEditable
  @UISchema(
      label = "Tipo de vínculo",
      helpText = "Modalidade do vínculo funcional a que se aplica a contagem.",
      group = "Escopo e Vigência",
      order = 30,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "work")
  private String tipoVinculo;

  @NotBlank
  @Size(max = 30)
  @Schema(
      description =
          "Categoria funcional do servidor abrangida pela regra (ex.: ADMINISTRATIVO, MAGISTERIO, SAUDE, POLICIAL).")
  @BulkEditable
  @UISchema(
      label = "Categoria funcional",
      helpText = "Categoria funcional de carreira para aplicação dos limites de férias.",
      group = "Escopo e Vigência",
      order = 40,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "category")
  private String categoria;

  @Size(max = 30)
  @Schema(
      description =
          "Subcategoria funcional opcional para diferenciação fina de regras de férias dentro da mesma carreira.",
      nullable = true)
  @BulkEditable(allowClear = true)
  @UISchema(
      label = "Subcategoria funcional",
      helpText = "Subdivisão opcional da categoria funcional.",
      group = "Escopo e Vigência",
      order = 50,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "layers")
  private String subcategoria;

  @NotNull
  @Schema(
      description =
          "Data de início de vigência normativa da matriz de parâmetros de contagem de férias.")
  @UISchema(
      label = "Início da vigência",
      helpText = "Data inicial a partir da qual esta regra estatutária produz efeitos.",
      group = "Escopo e Vigência",
      order = 60,
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "event")
  private LocalDate dtini;

  @Schema(
      description =
          "Data de término de vigência da regra; quando nula, indica parametrização vigente por prazo indeterminado.",
      nullable = true)
  @BulkEditable(allowClear = true)
  @UISchema(
      label = "Fim da vigência",
      helpText = "Data final de vigência ou em branco para regra ativa sem término.",
      group = "Escopo e Vigência",
      order = 70,
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "event_busy")
  private LocalDate dtfim;

  @NotNull
  @Schema(
      description =
          "Indica se a combinação de regime, vínculo e categoria confere direito a férias periódicas remuneradas.")
  @BulkEditable
  @UISchema(
      label = "Possui direito a férias",
      helpText = "Habilita a aquisição e o gozo de férias para este enquadramento.",
      group = "Direito e Parcelamento",
      order = 80,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "beach_access")
  private Boolean temFerias = true;

  @Min(0)
  @Max(90)
  @Schema(
      description =
          "Total padrão de dias de direito a férias por período aquisitivo completo (ex.: 30 dias ordinários ou 45 dias para magistério).",
      nullable = true)
  @BulkEditable(allowClear = true)
  @UISchema(
      label = "Total de dias de direito",
      helpText = "Quantidade padrão de dias de férias adquiridos em cada ciclo completo.",
      group = "Direito e Parcelamento",
      order = 90,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "calendar_month")
  private Integer totDiasFer = 30;

  @Min(1)
  @Max(6)
  @Schema(
      description =
          "Quantidade mínima de parcelas em que o período aquisitivo de férias deve ser usufruído.",
      nullable = true)
  @BulkEditable(allowClear = true)
  @UISchema(
      label = "Mínimo de parcelas",
      helpText = "Menor número de etapas de gozo exigido pela regra estatutária.",
      group = "Direito e Parcelamento",
      order = 100,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "splitscreen")
  private Integer minPerFer = 1;

  @Min(1)
  @Max(6)
  @Schema(
      description =
          "Quantidade máxima de parcelas permitida para fracionamento do gozo de um mesmo período aquisitivo.",
      nullable = true)
  @BulkEditable(allowClear = true)
  @UISchema(
      label = "Máximo de parcelas",
      helpText = "Limite máximo de etapas para fracionamento das férias.",
      group = "Direito e Parcelamento",
      order = 110,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "splitscreen")
  private Integer maxPerFer = 3;

  @Min(1)
  @Max(90)
  @Schema(
      description =
          "Quantidade mínima de dias exigida em cada parcela individual de gozo de férias.",
      nullable = true)
  @BulkEditable(allowClear = true)
  @UISchema(
      label = "Mínimo de dias por parcela",
      helpText = "Menor duração permitida para qualquer fração de gozo de férias.",
      group = "Direito e Parcelamento",
      order = 120,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "timer")
  private Integer minDiasPerFer = 10;

  @Min(1)
  @Max(90)
  @Schema(
      description =
          "Quantidade máxima de dias permitida em uma única parcela de gozo de férias.",
      nullable = true)
  @BulkEditable(allowClear = true)
  @UISchema(
      label = "Máximo de dias por parcela",
      helpText = "Maior duração permitida para uma parcela individual de gozo.",
      group = "Direito e Parcelamento",
      order = 130,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "timer")
  private Integer maxDiasPerFer = 30;

  @Min(1)
  @Max(90)
  @Schema(
      description =
          "Quantidade mínima de dias exigida em pelo menos uma parcela principal quando houver fracionamento (ex.: 14 dias).",
      nullable = true)
  @BulkEditable(allowClear = true)
  @UISchema(
      label = "Mínimo na parcela principal",
      helpText = "Duração mínima obrigatória na parcela principal do período aquisitivo.",
      group = "Direito e Parcelamento",
      order = 140,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "verified")
  private Integer minDiasPerPrincFer = 14;

  @Pattern(regexp = "D|G|N")
  @Schema(
      description =
          "Base de cálculo do abono pecuniário de férias: D (sobre dias de direito do PA), G (sobre dias de gozo da parcela) ou N (não permite abono).",
      allowableValues = {"D", "G", "N"},
      nullable = true)
  @BulkEditable
  @UISchema(
      label = "Base do abono pecuniário",
      helpText = "Informe D (dias de direito), G (dias de gozo) ou N (sem abono pecuniário).",
      group = "Abono e Financeiro",
      order = 150,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      options =
          "[{\"value\":\"D\",\"label\":\"Dias de direito (D)\"},{\"value\":\"G\",\"label\":\"Dias de gozo (G)\"},{\"value\":\"N\",\"label\":\"Sem abono pecuniário (N)\"}]",
      icon = "sell")
  private String abonoDirGozo = "D";

  @Min(0)
  @Max(90)
  @Schema(
      description =
          "Numerador da fração regulamentar de conversão de férias em abono pecuniário (ex.: 1 em 1/3).",
      nullable = true)
  @BulkEditable(allowClear = true)
  @UISchema(
      label = "Numerador da fração de abono",
      helpText = "Numerador da proporção máxima conversível em pecúnia.",
      group = "Abono e Financeiro",
      order = 160,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "percent")
  private Integer numDiasAbonoFer = 1;

  @Min(1)
  @Max(90)
  @Schema(
      description =
          "Denominador da fração regulamentar de conversão de férias em abono pecuniário (ex.: 3 em 1/3).",
      nullable = true)
  @BulkEditable(allowClear = true)
  @UISchema(
      label = "Denominador da fração de abono",
      helpText = "Denominador da proporção máxima conversível em pecúnia.",
      group = "Abono e Financeiro",
      order = 170,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "percent")
  private Integer denDiasAbonoFer = 3;

  @Min(0)
  @Max(90)
  @Schema(
      description =
          "Quantidade mínima de dias de férias exigida para permitir venda de abono pecuniário.",
      nullable = true)
  @BulkEditable(allowClear = true)
  @UISchema(
      label = "Mínimo de dias para venda",
      helpText = "Menor quantidade de dias que pode ser convertida em abono pecuniário.",
      group = "Abono e Financeiro",
      order = 180,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "payments")
  private Integer minDiasPvendFer = 1;

  @Min(0)
  @Max(90)
  @Schema(
      description =
          "Quantidade máxima de dias de férias que pode ser convertida em abono pecuniário no período aquisitivo.",
      nullable = true)
  @BulkEditable(allowClear = true)
  @UISchema(
      label = "Máximo de dias vendidos",
      helpText = "Teto de dias conversíveis em abono pecuniário por período aquisitivo.",
      group = "Abono e Financeiro",
      order = 190,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "payments")
  private Integer maxDiasVendFer = 10;

  @NotNull
  @Schema(
      description =
          "Indica se o enquadramento faz jus ao pagamento do terço constitucional de férias.")
  @BulkEditable
  @UISchema(
      label = "Terço constitucional",
      helpText = "Habilita o pagamento do adicional constitucional de 1/3 de férias.",
      group = "Abono e Financeiro",
      order = 200,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "account_balance")
  private Boolean abonoConstFer = true;

  @NotNull
  @Schema(
      description =
          "Indica se o servidor pode solicitar adiantamento de décimo terceiro salário por ocasião das férias.")
  @BulkEditable
  @UISchema(
      label = "Permite adiantamento de 13º",
      helpText = "Autoriza antecipação de parcela da gratificação natalina nas férias.",
      group = "Abono e Financeiro",
      order = 210,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "payments")
  private Boolean adiant13Fer = true;

  @NotNull
  @Schema(
      description =
          "Indica se o servidor pode solicitar adiantamento salarial de férias no processamento da folha.")
  @BulkEditable
  @UISchema(
      label = "Permite adiantamento salarial",
      helpText = "Autoriza antecipação salarial de férias na folha de pagamento.",
      group = "Abono e Financeiro",
      order = 220,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "payments")
  private Boolean adiantSalFer = false;

  @NotNull
  @Schema(
      description =
          "Indica se o servidor pode contratar rubrica de empréstimo ou antecipação restituível de férias.")
  @BulkEditable
  @UISchema(
      label = "Permite empréstimo de férias",
      helpText = "Autoriza concessão de empréstimo financeiro vinculado ao gozo de férias.",
      group = "Abono e Financeiro",
      order = 230,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "account_balance")
  private Boolean emprestFer = false;

  @NotNull
  @Schema(
      description =
          "Indica se é permitido programar ou usufruir férias antes do término do respectivo período aquisitivo.")
  @BulkEditable
  @UISchema(
      label = "Gozo antes do fim do PA",
      helpText = "Autoriza fruição de férias com período aquisitivo ainda em aquisição.",
      group = "Gozo Antecipado e Prescrição",
      order = 240,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "history")
  private Boolean permiteGozoAntesPaFer = false;

  @NotNull
  @Schema(
      description =
          "Indica se é permitido usufruir férias ainda no primeiro período aquisitivo do vínculo funcional.")
  @BulkEditable
  @UISchema(
      label = "Permite no 1º período",
      helpText = "Autoriza gozo antecipado durante o primeiro período aquisitivo.",
      group = "Gozo Antecipado e Prescrição",
      order = 250,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "history")
  private Boolean permiteFerPrimPer = false;

  @NotNull
  @Schema(
      description =
          "Indica se é permitido usufruir férias antecipadas durante o segundo período aquisitivo do vínculo.")
  @BulkEditable
  @UISchema(
      label = "Permite no 2º período",
      helpText = "Autoriza gozo antecipado durante o segundo período aquisitivo.",
      group = "Gozo Antecipado e Prescrição",
      order = 260,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "history")
  private Boolean permiteFerSegunPer = false;

  @NotNull
  @Schema(
      description =
          "Indica se é permitido usufruir férias antecipadas a partir do terceiro período aquisitivo do vínculo.")
  @BulkEditable
  @UISchema(
      label = "Permite no 3º período",
      helpText = "Autoriza gozo antecipado no terceiro período aquisitivo e seguintes.",
      group = "Gozo Antecipado e Prescrição",
      order = 270,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "history")
  private Boolean permiteFerTercPer = false;

  @NotNull
  @Schema(
      description =
          "Indica se é permitido programar férias em período aquisitivo incompleto ou proporcional.")
  @BulkEditable
  @UISchema(
      label = "Permite período incompleto",
      helpText = "Autoriza concessão de férias proporcionais em período aquisitivo incompleto.",
      group = "Gozo Antecipado e Prescrição",
      order = 280,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "history")
  private Boolean permiteFerPerInc = false;

  @Pattern(regexp = "I|F")
  @Schema(
      description =
          "Critério de comparação da prescrição do período aquisitivo: I (data inicial do gozo até a prescrição) ou F (data final do gozo até a prescrição).",
      allowableValues = {"I", "F"},
      nullable = true)
  @BulkEditable
  @UISchema(
      label = "Critério de prescrição",
      helpText = "Use I para validar pelo início do gozo ou F pelo término do gozo.",
      group = "Gozo Antecipado e Prescrição",
      order = 290,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      options =
          "[{\"value\":\"I\",\"label\":\"Pelo início do gozo (I)\"},{\"value\":\"F\",\"label\":\"Pelo término do gozo (F)\"}]",
      icon = "warning")
  private String criterioPrescricao = "F";

  @NotNull
  @Schema(
      description =
          "Indica se é permitido registrar gozo de férias após a data de prescrição do período aquisitivo.")
  @BulkEditable
  @UISchema(
      label = "Permite gozo após prescrição",
      helpText = "Autoriza agendamento de férias mesmo após vencida a data de prescrição.",
      group = "Gozo Antecipado e Prescrição",
      order = 300,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "warning")
  private Boolean permiteGozoAposPrescricao = false;

  @NotNull
  @Schema(
      description =
          "Indica se é permitido lançar períodos aquisitivos concomitantes ou sobrepostos para o mesmo vínculo.")
  @BulkEditable
  @UISchema(
      label = "Permite PA concomitante",
      helpText = "Autoriza coexistência de períodos aquisitivos com interseção de datas.",
      group = "Gozo Antecipado e Prescrição",
      order = 310,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "layers")
  private Boolean permiteLancConcPa = false;

  @NotNull
  @Schema(
      description =
          "Indica se o preenchimento do ano de exercício de referência é obrigatório no período aquisitivo.")
  @BulkEditable
  @UISchema(
      label = "Exige ano de exercício",
      helpText = "Torna obrigatório informar o exercício de referência na abertura do PA.",
      group = "Gozo Antecipado e Prescrição",
      order = 320,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "calendar_month")
  private Boolean exigeExercicio = false;

  @NotNull
  @Schema(
      description =
          "Indica isenção de pagamento de dobra/multa por concessão de férias fora do prazo concessivo.")
  @BulkEditable
  @UISchema(
      label = "Isenta multa concessiva",
      helpText = "Suprime o pagamento de multa por atraso na concessão das férias.",
      group = "Gozo Antecipado e Prescrição",
      order = 330,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "gavel")
  private Boolean semPgtoMulta = false;

  @NotNull
  @Schema(
      description =
          "Indica se o saldo de férias não gozado pode ser averbado para contagem em dobro de tempo de serviço.")
  @BulkEditable
  @UISchema(
      label = "Permite contagem em dobro",
      helpText = "Autoriza converter dias de férias não gozados em contagem em dobro.",
      group = "Gozo Antecipado e Prescrição",
      order = 340,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "trending_up")
  private Boolean contDobroFerias = false;

  @Min(0)
  @Max(90)
  @Schema(
      description =
          "Quantidade mínima de dias do período aquisitivo passível de conversão para contagem em dobro.",
      nullable = true)
  @BulkEditable(allowClear = true)
  @UISchema(
      label = "Mínimo para contagem em dobro",
      helpText = "Menor saldo de dias do PA que pode ser convertido em dobro.",
      group = "Gozo Antecipado e Prescrição",
      order = 350,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "trending_up")
  private Integer minDiasPeraqfer;

  @Min(0)
  @Max(90)
  @Schema(
      description =
          "Quantidade máxima de dias do período aquisitivo passível de conversão para contagem em dobro.",
      nullable = true)
  @BulkEditable(allowClear = true)
  @UISchema(
      label = "Máximo para contagem em dobro",
      helpText = "Maior saldo de dias do PA que pode ser convertido em dobro.",
      group = "Gozo Antecipado e Prescrição",
      order = 360,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "trending_up")
  private Integer maxDiasPeraqfer;

  @NotNull
  @Schema(
      description =
          "Indica se o enquadramento estatutário faz jus à aquisição de licença-prêmio ou licença especial por assiduidade.")
  @BulkEditable
  @UISchema(
      label = "Possui licença especial",
      helpText = "Habilita apuração de quinquênios/períodos de licença especial.",
      group = "Licença Especial e Progressão",
      order = 370,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "workspace_premium")
  private Boolean temLicEsp = false;

  @Min(1)
  @Max(30)
  @Schema(
      description =
          "Quantidade de anos de efetivo exercício exigida para adquirir cada período de licença especial (ex.: 5 anos).",
      nullable = true)
  @BulkEditable(allowClear = true)
  @UISchema(
      label = "Anos por licença especial",
      helpText = "Interstício em anos para aquisição de licença especial.",
      group = "Licença Especial e Progressão",
      order = 380,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "schedule")
  private Integer anosLicEsp;

  @Min(1)
  @Max(24)
  @Schema(
      description =
          "Quantidade de meses de direito concedida a cada período adquirido de licença especial (ex.: 3 meses).",
      nullable = true)
  @BulkEditable(allowClear = true)
  @UISchema(
      label = "Meses de direito da licença",
      helpText = "Duração em meses concedida por ciclo de licença especial.",
      group = "Licença Especial e Progressão",
      order = 390,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "calendar_month")
  private Integer mesesLicEspPer;

  @NotNull
  @Schema(
      description =
          "Indica se os dias de licença especial adquiridos podem ser convertidos em pecúnia indenizatória.")
  @BulkEditable
  @UISchema(
      label = "Permite venda de licença",
      helpText = "Autoriza conversão de saldo de licença especial em abono pecuniário.",
      group = "Licença Especial e Progressão",
      order = 400,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "sell")
  private Boolean vendLicEsp = false;

  @Pattern(regexp = "D|G|N")
  @Schema(
      description =
          "Base de cálculo para conversão de licença especial em pecúnia: D (dias de direito), G (dias de gozo) ou N (vedado).",
      nullable = true)
  @BulkEditable(allowClear = true)
  @UISchema(
      label = "Base de abono da licença",
      helpText = "Informe D (direito), G (gozo) ou N (sem conversão da licença).",
      group = "Licença Especial e Progressão",
      order = 410,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      options =
          "[{\"value\":\"D\",\"label\":\"Dias de direito (D)\"},{\"value\":\"G\",\"label\":\"Dias de gozo (G)\"},{\"value\":\"N\",\"label\":\"Sem conversão (N)\"}]",
      icon = "sell")
  private String abonoDirGozoLicEsp;

  @Min(0)
  @Max(365)
  @Schema(
      description =
          "Quantidade mínima de dias de licença especial que pode ser convertida em pecúnia.",
      nullable = true)
  @BulkEditable(allowClear = true)
  @UISchema(
      label = "Mínimo venda licença especial",
      helpText = "Menor quantidade de dias de licença especial conversível em pecúnia.",
      group = "Licença Especial e Progressão",
      order = 420,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "payments")
  private Integer minDiasPvendLicEsp;

  @Min(0)
  @Max(365)
  @Schema(
      description =
          "Quantidade máxima de dias de licença especial que pode ser convertida em pecúnia.",
      nullable = true)
  @BulkEditable(allowClear = true)
  @UISchema(
      label = "Máximo venda licença especial",
      helpText = "Teto de dias de licença especial conversível em pecúnia.",
      group = "Licença Especial e Progressão",
      order = 430,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "payments")
  private Integer maxDiasVendLicEsp;

  @Min(0)
  @Max(365)
  @Schema(
      description =
          "Numerador da fração regulamentar de conversão da licença especial em abono pecuniário.",
      nullable = true)
  @BulkEditable(allowClear = true)
  @UISchema(
      label = "Numerador abono da licença",
      helpText = "Numerador da fração máxima de conversão da licença especial.",
      group = "Licença Especial e Progressão",
      order = 440,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "percent")
  private Integer numDiasAbonoLicEsp;

  @Min(1)
  @Max(365)
  @Schema(
      description =
          "Denominador da fração regulamentar de conversão da licença especial em abono pecuniário.",
      nullable = true)
  @BulkEditable(allowClear = true)
  @UISchema(
      label = "Denominador abono da licença",
      helpText = "Denominador da fração máxima de conversão da licença especial.",
      group = "Licença Especial e Progressão",
      order = 450,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "percent")
  private Integer denDiasAbonoLicEsp;

  @NotNull
  @Schema(
      description =
          "Indica se o saldo não usufruído de licença especial pode ser contado em dobro para tempo de serviço.")
  @BulkEditable
  @UISchema(
      label = "Contagem em dobro da licença",
      helpText = "Autoriza averbar licença especial não gozada em dobro.",
      group = "Licença Especial e Progressão",
      order = 460,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "trending_up")
  private Boolean contDobroLicEsp = false;

  @Min(0)
  @Max(365)
  @Schema(
      description =
          "Quantidade mínima de dias de licença especial exigida para averbação em dobro.",
      nullable = true)
  @BulkEditable(allowClear = true)
  @UISchema(
      label = "Mínimo dobro licença especial",
      helpText = "Menor saldo de dias de licença especial para contagem em dobro.",
      group = "Licença Especial e Progressão",
      order = 470,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "trending_up")
  private Integer minDiasPcontDobLicEsp;

  @Min(0)
  @Max(365)
  @Schema(
      description =
          "Quantidade máxima de dias de licença especial permitida para averbação em dobro.",
      nullable = true)
  @BulkEditable(allowClear = true)
  @UISchema(
      label = "Máximo dobro licença especial",
      helpText = "Teto de dias de licença especial para contagem em dobro.",
      group = "Licença Especial e Progressão",
      order = 480,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "trending_up")
  private Integer maxDiasContDobLicEsp;

  @NotNull
  @Schema(
      description =
          "Indica se o enquadramento estatutário habilita apuração automática de interstício de progressão funcional.")
  @BulkEditable
  @UISchema(
      label = "Possui progressão funcional",
      helpText = "Habilita contagem de tempo para progressão na carreira.",
      group = "Licença Especial e Progressão",
      order = 490,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "trending_up")
  private Boolean temProgressao = false;

  @Min(1)
  @Max(30)
  @Schema(
      description =
          "Interstício em anos exigido para cada ciclo de progressão funcional na carreira.",
      nullable = true)
  @BulkEditable(allowClear = true)
  @UISchema(
      label = "Anos para progressão",
      helpText = "Quantidade de anos exigida para progressão funcional.",
      group = "Licença Especial e Progressão",
      order = 500,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "schedule")
  private Integer anosProgressao;

  @NotNull
  @Schema(
      description =
          "Indica se o enquadramento estatutário habilita contagem de adicional por tempo de serviço (anuênio/quinquênio).")
  @BulkEditable
  @UISchema(
      label = "Possui adicional de tempo",
      helpText = "Habilita apuração de adicional por tempo de serviço.",
      group = "Licença Especial e Progressão",
      order = 510,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "schedule")
  private Boolean temTempoServ = false;

  @Min(1)
  @Max(35)
  @Schema(
      description =
          "Quantidade de anos exigida para o primeiro ciclo de adicional por tempo de serviço.",
      nullable = true)
  @BulkEditable(allowClear = true)
  @UISchema(
      label = "Anos para 1º adicional",
      helpText = "Interstício em anos para aquisição do primeiro adicional de tempo.",
      group = "Licença Especial e Progressão",
      order = 520,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "schedule")
  private Integer anos1TempoServ;

  @NotNull
  @Schema(
      description =
          "Indica se o enquadramento estatutário habilita apuração do benefício constitucional de sexta-parte.")
  @BulkEditable
  @UISchema(
      label = "Possui sexta-parte",
      helpText = "Habilita contagem de tempo para concessão de sexta-parte.",
      group = "Licença Especial e Progressão",
      order = 530,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "workspace_premium")
  private Boolean temSextaParte = false;

  @Min(1)
  @Max(40)
  @Schema(
      description =
          "Quantidade de anos de efetivo exercício exigida para aquisição da sexta-parte (ex.: 20 anos).",
      nullable = true)
  @BulkEditable(allowClear = true)
  @UISchema(
      label = "Anos para sexta-parte",
      helpText = "Interstício em anos para concessão de sexta-parte.",
      group = "Licença Especial e Progressão",
      order = 540,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "schedule")
  private Integer anosSextaParte;

  @Schema(
      description =
          "Identificador do documento legal ou estatuto que fundamenta a regra de contagem de férias.",
      nullable = true)
  @BulkEditable(allowClear = true)
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
      displayField = "label")
  private Long documentoLegalFeriasId;

  @Schema(
      description =
          "Identificador do documento legal que fundamenta os interstícios de progressão e tempo de serviço.",
      nullable = true)
  @BulkEditable(allowClear = true)
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
      displayField = "label")
  private Long documentoLegalProgressaoId;

  @Size(max = 2000)
  @Schema(
      description =
          "Observações técnicas, diretrizes de aplicação e notas normativas sobre a parametrização estatutária.",
      nullable = true)
  @BulkEditable(allowClear = true)
  @UISchema(
      label = "Observações normativas",
      helpText = "Anotações interpretativas e diretrizes de RH sobre esta regra.",
      group = "Fundamentação e Governança",
      order = 570,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.TEXTAREA,
      icon = "notes")
  private String observacoes;

  @Schema(
      description =
          "Atributos adicionais dinâmicos da parametrização de férias governados pelo catálogo de campos customizados.")
  @CustomFields(resourceKey = "administracao-pessoal.parametros-contagem-ferias")
  @UISchema(
      label = "Campos customizados",
      helpText = "Atributos específicos configurados pelo órgão para regras de férias.",
      group = "Campos Customizados",
      order = 580)
  private Map<String, Object> customFields = new LinkedHashMap<>();
}
