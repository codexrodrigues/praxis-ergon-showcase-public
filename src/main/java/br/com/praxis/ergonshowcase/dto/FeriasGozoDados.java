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
public abstract class FeriasGozoDados {

  @NotNull
  @Schema(description = "Código do órgão público de lotação do vínculo funcional.")
  @UISchema(
      label = "Órgão",
      helpText = "Órgão público titular do vínculo funcional.",
      group = "Vínculo e Período Aquisitivo",
      order = 10,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "apartment",
      endpoint = ApiPaths.ORGAO_OPTIONS,
      valueField = "id",
      displayField = "label")
  private Integer empCodigo;

  @NotNull
  @Schema(description = "Número funcional do servidor titular da programação de férias.")
  @UISchema(
      label = "Servidor",
      helpText = "Pessoa servidora titular da parcela de férias.",
      group = "Vínculo e Período Aquisitivo",
      order = 20,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "person",
      endpoint = ApiPaths.PESSOA_OPTIONS,
      valueField = "id",
      displayField = "label")
  private Long numfunc;

  @NotNull
  @Schema(description = "Número sequencial do vínculo funcional do servidor.")
  @UISchema(
      label = "Vínculo funcional",
      helpText = "Sequencial do vínculo funcional do servidor.",
      group = "Vínculo e Período Aquisitivo",
      order = 30,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "work",
      endpoint = ApiPaths.VINCULO_OPTIONS,
      valueField = "id",
      displayField = "label")
  private Integer numvinc;

  @NotNull
  @Schema(
      description =
          "Identificador surrogate do período aquisitivo de férias debitado por esta parcela.")
  @UISchema(
      label = "Período aquisitivo",
      helpText = "Período aquisitivo de férias ao qual a parcela está vinculada.",
      group = "Vínculo e Período Aquisitivo",
      order = 40,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "date_range",
      endpoint = ApiPaths.PERIODO_AQUISITIVO_FERIAS_OPTIONS,
      valueField = "id",
      displayField = "label")
  private Long periodoAquisitivoId;

  @NotNull
  @Min(1)
  @Schema(
      description =
          "Chave sequencial de negócio do período aquisitivo no vínculo (CHAVEPERAQFER).")
  @UISchema(
      label = "Chave do PA",
      helpText = "Sequencial de negócio do período aquisitivo no vínculo.",
      group = "Vínculo e Período Aquisitivo",
      order = 50,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "tag")
  private Integer chavePeraqfer;

  @NotNull
  @Min(1)
  @Max(6)
  @Schema(
      description =
          "Ordem sequencial da parcela de gozo dentro do período aquisitivo (1ª, 2ª, 3ª parcela).")
  @UISchema(
      label = "Ordem da parcela",
      helpText = "Número sequencial da etapa de gozo dentro do PA.",
      group = "Período de Gozo e Duração",
      order = 60,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "splitscreen")
  private Integer ordemParcela = 1;

  @NotNull
  @Schema(description = "Data de início da fruição da parcela de férias.")
  @UISchema(
      label = "Início do gozo",
      helpText = "Data inicial de afastamento em gozo de férias.",
      group = "Período de Gozo e Duração",
      order = 70,
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "event")
  private LocalDate dtini;

  @NotNull
  @Schema(description = "Data de término previsto da parcela de férias programada.")
  @BulkEditable
  @UISchema(
      label = "Término previsto",
      helpText = "Data final programada para encerramento da parcela.",
      group = "Período de Gozo e Duração",
      order = 80,
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "event_available")
  private LocalDate dtprevfim;

  @Schema(
      description =
          "Data de término efetivo do gozo da parcela (ajustada quando há interrupção ou conclusão).",
      nullable = true)
  @BulkEditable(allowClear = true)
  @UISchema(
      label = "Término efetivo",
      helpText = "Data final efetiva do gozo das férias.",
      group = "Período de Gozo e Duração",
      order = 90,
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "event_busy")
  private LocalDate dtfim;

  @NotNull
  @Min(1)
  @Max(90)
  @Schema(description = "Quantidade de dias corridos de gozo nesta parcela de férias.")
  @BulkEditable
  @UISchema(
      label = "Dias de gozo",
      helpText = "Quantidade de dias de descanso nesta parcela.",
      group = "Período de Gozo e Duração",
      order = 100,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "beach_access")
  private Integer diasGozo;

  @NotNull
  @Min(0)
  @Max(90)
  @Schema(
      description =
          "Espelho do total de dias de direito do período aquisitivo no momento da concessão (TOTDIAS).")
  @UISchema(
      label = "Dias de direito no PA",
      helpText = "Total de dias de direito do período aquisitivo vinculado.",
      group = "Período de Gozo e Duração",
      order = 110,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "calendar_month")
  private Integer totDiasDireitoPa = 30;

  @NotNull
  @Schema(
      description =
          "Indica se há conversão parcial de férias em abono pecuniário vinculada a esta parcela.")
  @BulkEditable
  @UISchema(
      label = "Com abono pecuniário",
      helpText = "Indica venda de dias em abono pecuniário nesta parcela.",
      group = "Abono Pecuniário e Financeiro",
      order = 120,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "sell")
  private Boolean abonoPecuniario = false;

  @Pattern(regexp = "I|F")
  @Schema(
      description =
          "Posição cronológica do abono pecuniário em relação ao gozo: I (no início das férias) ou F (ao final das férias).",
      allowableValues = {"I", "F"},
      nullable = true)
  @BulkEditable(allowClear = true)
  @UISchema(
      label = "Posição do abono (I/F)",
      helpText = "Informe I (início das férias) ou F (final das férias).",
      group = "Abono Pecuniário e Financeiro",
      order = 130,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      icon = "swap_horiz")
  private String iniFimAbono;

  @NotNull
  @Min(0)
  @Max(90)
  @Schema(
      description =
          "Quantidade de dias vendidos como abono pecuniário nesta parcela (ERG_ABONOS_FERIAS).")
  @BulkEditable
  @UISchema(
      label = "Dias de abono vendidos",
      helpText = "Quantidade de dias convertidos em pecúnia nesta parcela.",
      group = "Abono Pecuniário e Financeiro",
      order = 140,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "payments")
  private Integer diasAbonoVendidos = 0;

  @Schema(
      description = "Data de protocolo ou requerimento da venda do abono pecuniário.",
      nullable = true)
  @BulkEditable(allowClear = true)
  @UISchema(
      label = "Data da venda do abono",
      helpText = "Data da opção formal pela conversão em abono pecuniário.",
      group = "Abono Pecuniário e Financeiro",
      order = 150,
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "event")
  private LocalDate dtVendaAbono;

  @Schema(
      description = "Data programada ou efetiva de pagamento do abono pecuniário.",
      nullable = true)
  @BulkEditable(allowClear = true)
  @UISchema(
      label = "Pagamento do abono",
      helpText = "Data de liquidação financeira do abono pecuniário.",
      group = "Abono Pecuniário e Financeiro",
      order = 160,
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "paid")
  private LocalDate dtPagtoAbono;

  @NotNull
  @Schema(
      description =
          "Indica se esta parcela comanda o pagamento do terço constitucional de férias na folha.")
  @BulkEditable
  @UISchema(
      label = "Paga terço constitucional",
      helpText = "Comanda o pagamento do adicional de 1/3 nesta parcela.",
      group = "Abono Pecuniário e Financeiro",
      order = 170,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "account_balance")
  private Boolean abonoConstitucional = true;

  @NotNull
  @Schema(
      description =
          "Indica se o servidor optou pelo adiantamento de décimo terceiro salário nesta parcela.")
  @BulkEditable
  @UISchema(
      label = "Adiantamento de 13º",
      helpText = "Comanda antecipação de 13º salário junto às férias.",
      group = "Abono Pecuniário e Financeiro",
      order = 180,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "payments")
  private Boolean adiantamento13 = false;

  @NotNull
  @Schema(
      description =
          "Indica se o servidor optou pelo adiantamento salarial de férias nesta parcela.")
  @BulkEditable
  @UISchema(
      label = "Adiantamento salarial",
      helpText = "Comanda antecipação salarial de férias na folha.",
      group = "Abono Pecuniário e Financeiro",
      order = 190,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "payments")
  private Boolean adiantamentoSalario = false;

  @NotNull
  @Schema(
      description =
          "Indica se o servidor contratou empréstimo financeiro restituível vinculado a esta parcela.")
  @BulkEditable
  @UISchema(
      label = "Empréstimo de férias",
      helpText = "Comanda rubrica de empréstimo de férias na folha.",
      group = "Abono Pecuniário e Financeiro",
      order = 200,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "account_balance")
  private Boolean emprestimoFerias = false;

  @Schema(
      description = "Competência (mês/ano) da folha de pagamento de processamento das férias.",
      nullable = true)
  @BulkEditable(allowClear = true)
  @UISchema(
      label = "Competência da folha",
      helpText = "Data de competência mensal da folha de pagamento.",
      group = "Folha, Substituição e Interrupção",
      order = 210,
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "receipt_long")
  private LocalDate mesAnoFolha;

  @Min(1)
  @Max(99)
  @Schema(
      description = "Número sequencial da folha de pagamento na competência de processamento.",
      nullable = true)
  @BulkEditable(allowClear = true)
  @UISchema(
      label = "Número da folha",
      helpText = "Sequencial da folha de pagamento na competência.",
      group = "Folha, Substituição e Interrupção",
      order = 220,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "tag")
  private Integer numFolha;

  @Schema(
      description =
          "Data a partir da qual o gozo de férias foi interrompido por necessidade do serviço.",
      nullable = true)
  @BulkEditable(allowClear = true)
  @UISchema(
      label = "Data de interrupção",
      helpText = "Data de retorno antecipado por interrupção das férias.",
      group = "Folha, Substituição e Interrupção",
      order = 240,
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "warning")
  private LocalDate dtInterrupcao;

  @Size(max = 500)
  @Schema(
      description = "Motivo administrativo ou fundamentação da interrupção do gozo de férias.",
      nullable = true)
  @BulkEditable(allowClear = true)
  @UISchema(
      label = "Motivo da interrupção",
      helpText = "Justificativa formal para a interrupção das férias.",
      group = "Folha, Substituição e Interrupção",
      order = 250,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.TEXTAREA,
      icon = "report")
  private String motivoInterrupcao;

  @Schema(
      description =
          "Número funcional do servidor designado como substituto durante o gozo das férias (ERG-00502).",
      nullable = true)
  @BulkEditable(allowClear = true)
  @UISchema(
      label = "Servidor substituto",
      helpText = "Servidor designado para responder durante as férias.",
      group = "Folha, Substituição e Interrupção",
      order = 260,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "supervisor_account",
      endpoint = ApiPaths.PESSOA_OPTIONS,
      valueField = "id",
      displayField = "label")
  private Long numfuncSubstituto;

  @Schema(
      description =
          "Número sequencial do vínculo funcional do servidor substituto (ERG-00503).",
      nullable = true)
  @BulkEditable(allowClear = true)
  @UISchema(
      label = "Vínculo do substituto",
      helpText = "Sequencial do vínculo ativo do servidor substituto.",
      group = "Folha, Substituição e Interrupção",
      order = 270,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "badge")
  private Integer numvincSubstituto;

  @Schema(
      description = "Identificador da portaria ou documento legal de concessão das férias.",
      nullable = true)
  @BulkEditable(allowClear = true)
  @UISchema(
      label = "Documento de concessão",
      helpText = "Portaria ou ato administrativo de concessão das férias.",
      group = "Governança e Fundamentação",
      order = 280,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "description",
      endpoint = ApiPaths.DOCUMENTO_OPTIONS,
      valueField = "id",
      displayField = "label")
  private Long documentoLegalId;

  @Schema(
      description = "Identificador do ato oficial de publicação da escala ou portaria de férias.",
      nullable = true)
  @BulkEditable(allowClear = true)
  @UISchema(
      label = "Ato de publicação",
      helpText = "Publicação oficial referente à parcela de férias.",
      group = "Governança e Fundamentação",
      order = 290,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "gavel",
      endpoint = ApiPaths.DOCUMENTO_OPTIONS,
      valueField = "id",
      displayField = "label")
  private Long atoPublicacaoId;

  @Size(max = 2000)
  @Schema(
      description = "Observações operacionais e despachos referentes à parcela de gozo de férias.",
      nullable = true)
  @BulkEditable(allowClear = true)
  @UISchema(
      label = "Observações da parcela",
      helpText = "Anotações administrativas sobre o gozo das férias.",
      group = "Governança e Fundamentação",
      order = 300,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.TEXTAREA,
      icon = "notes")
  private String observacoes;

  @Schema(
      description =
          "Atributos adicionais dinâmicos da parcela de férias governados pelo catálogo de campos customizados.")
  @CustomFields(resourceKey = "administracao-pessoal.ferias-gozo")
  @UISchema(
      label = "Campos customizados",
      helpText = "Atributos específicos configurados pelo órgão para gozo de férias.",
      group = "Campos Customizados",
      order = 310)
  private Map<String, Object> customFields = new LinkedHashMap<>();
}
