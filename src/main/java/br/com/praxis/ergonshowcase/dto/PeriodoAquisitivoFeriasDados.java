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
public abstract class PeriodoAquisitivoFeriasDados {

  @NotNull
  @Schema(description = "Código do órgão público de lotação do vínculo funcional.")
  @UISchema(
      label = "Órgão",
      helpText = "Órgão público titular do vínculo funcional.",
      group = "Identificação e Vínculo",
      order = 10,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "apartment",
      endpoint = ApiPaths.ORGAO_OPTIONS,
      valueField = "id",
      displayField = "label")
  private Integer empCodigo;

  @NotNull
  @Schema(description = "Número funcional do servidor titular do período aquisitivo.")
  @UISchema(
      label = "Servidor",
      helpText = "Pessoa servidora titular do período aquisitivo de férias.",
      group = "Identificação e Vínculo",
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
      helpText = "Sequencial do vínculo funcional a que pertence o período aquisitivo.",
      group = "Identificação e Vínculo",
      order = 30,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "work",
      endpoint = ApiPaths.VINCULO_OPTIONS,
      valueField = "id",
      displayField = "label")
  private Integer numvinc;

  @NotNull
  @Min(1)
  @Schema(
      description =
          "Chave sequencial de negócio do período aquisitivo dentro do vínculo funcional (1, 2, 3...).")
  @UISchema(
      label = "Sequencial do PA",
      helpText = "Ordem sequencial do período aquisitivo no vínculo (CHAVE).",
      group = "Identificação e Vínculo",
      order = 40,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "tag")
  private Integer chave;

  @Min(1950)
  @Max(2100)
  @Schema(
      description = "Ano de exercício de referência orçamentária e funcional do período aquisitivo.",
      nullable = true)
  @BulkEditable(allowClear = true)
  @UISchema(
      label = "Ano de exercício",
      helpText = "Ano civil de referência para programação das férias.",
      group = "Identificação e Vínculo",
      order = 50,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "calendar_month")
  private Integer exercicio;

  @NotNull
  @Schema(description = "Data de início de apuração do período aquisitivo de férias.")
  @UISchema(
      label = "Início do PA",
      helpText = "Data inicial do ciclo aquisitivo de férias.",
      group = "Identificação e Vínculo",
      order = 60,
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "event")
  private LocalDate dtini;

  @NotNull
  @Schema(description = "Data de término do ciclo aquisitivo de férias.")
  @BulkEditable
  @UISchema(
      label = "Término do PA",
      helpText = "Data final de fechamento do período aquisitivo.",
      group = "Identificação e Vínculo",
      order = 70,
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "event_available")
  private LocalDate dtfim;

  @NotNull
  @Min(0)
  @Max(90)
  @Schema(
      description =
          "Total de dias de direito a férias apurado no período aquisitivo após desconto de faltas injustificadas.")
  @BulkEditable
  @UISchema(
      label = "Dias de direito",
      helpText = "Quantidade de dias de férias adquiridos neste período.",
      group = "Contabilidade de Dias e Saldo",
      order = 80,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "beach_access")
  private Integer totDiasDireito = 30;

  @NotNull
  @Min(0)
  @Max(366)
  @Schema(
      description =
          "Quantidade de faltas injustificadas apuradas durante o ciclo aquisitivo que impactam a redução proporcional de dias.")
  @BulkEditable
  @UISchema(
      label = "Faltas apuradas",
      helpText = "Total de faltas não abonadas computadas no período aquisitivo.",
      group = "Contabilidade de Dias e Saldo",
      order = 90,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "event_busy")
  private Integer faltasApuradas = 0;

  @NotNull
  @Min(0)
  @Max(3660)
  @Schema(
      description =
          "Quantidade de dias de licenças ou afastamentos suspensivos que postergaram o fechamento do período aquisitivo.")
  @BulkEditable
  @UISchema(
      label = "Dias de suspensão",
      helpText = "Dias de afastamento sem contagem para férias que prorrogaram o PA.",
      group = "Contabilidade de Dias e Saldo",
      order = 100,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "pause_circle")
  private Integer diasAfastamentoSuspensivo = 0;

  @NotNull
  @Min(0)
  @Max(90)
  @Schema(description = "Total acumulado de dias efetivamente usufruídos em parcelas de gozo.")
  @BulkEditable
  @UISchema(
      label = "Dias gozados",
      helpText = "Soma dos dias já programados ou usufruídos em parcelas.",
      group = "Contabilidade de Dias e Saldo",
      order = 110,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "check_circle")
  private Integer diasGozados = 0;

  @NotNull
  @Min(0)
  @Max(90)
  @Schema(
      description =
          "Total de dias do período aquisitivo convertidos em abono pecuniário indenizatório.")
  @BulkEditable
  @UISchema(
      label = "Dias vendidos em abono",
      helpText = "Dias do período aquisitivo convertidos em pecúnia.",
      group = "Contabilidade de Dias e Saldo",
      order = 120,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "sell")
  private Integer diasVendidosAbono = 0;

  @NotNull
  @Min(0)
  @Max(90)
  @Schema(
      description =
          "Total de dias do período aquisitivo averbados para contagem em dobro de tempo de serviço.")
  @BulkEditable
  @UISchema(
      label = "Dias em contagem em dobro",
      helpText = "Dias não gozados convertidos em contagem em dobro.",
      group = "Contabilidade de Dias e Saldo",
      order = 130,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "trending_up")
  private Integer diasConvertidosDobro = 0;

  @Schema(
      description = "Data limite de prescrição para fruição das férias deste período aquisitivo.",
      nullable = true)
  @BulkEditable(allowClear = true)
  @UISchema(
      label = "Data de prescrição",
      helpText = "Data limite para início ou término do gozo antes da prescrição.",
      group = "Prescrição e Multa",
      order = 150,
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "warning")
  private LocalDate dtPrescricao;

  @Pattern(regexp = "I|F")
  @Schema(
      description =
          "Critério de avaliação da prescrição no período aquisitivo: I (data inicial do gozo) ou F (data final do gozo).",
      nullable = true)
  @BulkEditable
  @UISchema(
      label = "Critério de prescrição",
      helpText = "Informe I (pelo início do gozo) ou F (pelo término do gozo).",
      group = "Prescrição e Multa",
      order = 160,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      icon = "warning")
  private String criterioPrescricao = "I";

  @Pattern(regexp = "I|S|N")
  @Schema(
      description =
          "Override individual de permissão de gozo após a data de prescrição: I (herda do parâmetro), S (permite) ou N (bloqueia).",
      nullable = true)
  @BulkEditable
  @UISchema(
      label = "Gozo após prescrição (PA)",
      helpText = "Use I (herda da regra), S (permite) ou N (bloqueia).",
      group = "Prescrição e Multa",
      order = 170,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      icon = "warning")
  private String permiteGozoPosPrescricao = "I";

  @Schema(
      description =
          "Data a partir da qual a concessão tardia das férias enseja pagamento de dobra ou multa concessiva.",
      nullable = true)
  @BulkEditable(allowClear = true)
  @UISchema(
      label = "Data limite sem multa",
      helpText = "Data a partir da qual incide multa por atraso concessivo.",
      group = "Prescrição e Multa",
      order = 180,
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "gavel")
  private LocalDate dtPgtoComMulta;

  @Pattern(regexp = "I|S|N")
  @Schema(
      description =
          "Indicador de dispensa do pagamento de multa concessiva para este período: I (herda), S (isenta multa) ou N (cobra multa).",
      nullable = true)
  @BulkEditable
  @UISchema(
      label = "Dispensa de multa (PA)",
      helpText = "Use I (herda), S (dispensa multa) ou N (aplica multa).",
      group = "Prescrição e Multa",
      order = 190,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      icon = "gavel")
  private String semPgtoMulta = "N";

  @Min(1)
  @Max(6)
  @Schema(
      description =
          "Override individual do máximo de parcelas permitido para este período aquisitivo (nulo = herda de ParametroContagemFerias).",
      nullable = true)
  @BulkEditable(allowClear = true)
  @UISchema(
      label = "Override máx. parcelas",
      helpText = "Sobrescreve o máximo de parcelas para este PA ou vazio para herdar.",
      group = "Overrides Individuais (Tarefa 24610)",
      order = 200,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "tune")
  private Integer maxPerFer;

  @Min(1)
  @Max(6)
  @Schema(
      description =
          "Override individual do mínimo de parcelas exigido para este período aquisitivo (nulo = herda de ParametroContagemFerias).",
      nullable = true)
  @BulkEditable(allowClear = true)
  @UISchema(
      label = "Override mín. parcelas",
      helpText = "Sobrescreve o mínimo de parcelas para este PA ou vazio para herdar.",
      group = "Overrides Individuais (Tarefa 24610)",
      order = 210,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "tune")
  private Integer minPerFer;

  @Min(1)
  @Max(90)
  @Schema(
      description =
          "Override individual do máximo de dias por parcela neste período aquisitivo (nulo = herda da regra).",
      nullable = true)
  @BulkEditable(allowClear = true)
  @UISchema(
      label = "Override máx. dias parcela",
      helpText = "Sobrescreve o teto de dias por parcela neste PA.",
      group = "Overrides Individuais (Tarefa 24610)",
      order = 220,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "tune")
  private Integer maxDiasPerFer;

  @Min(1)
  @Max(90)
  @Schema(
      description =
          "Override individual do mínimo de dias por parcela neste período aquisitivo (nulo = herda da regra).",
      nullable = true)
  @BulkEditable(allowClear = true)
  @UISchema(
      label = "Override mín. dias parcela",
      helpText = "Sobrescreve o mínimo de dias por parcela neste PA.",
      group = "Overrides Individuais (Tarefa 24610)",
      order = 230,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "tune")
  private Integer minDiasPerFer;

  @Min(1)
  @Max(90)
  @Schema(
      description =
          "Override individual do mínimo de dias na parcela principal neste período aquisitivo (nulo = herda da regra).",
      nullable = true)
  @BulkEditable(allowClear = true)
  @UISchema(
      label = "Override mín. principal",
      helpText = "Sobrescreve a duração mínima da parcela principal neste PA.",
      group = "Overrides Individuais (Tarefa 24610)",
      order = 240,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "tune")
  private Integer minDiasPerPrincFer;

  @Pattern(regexp = "I|D|G|N")
  @Schema(
      description =
          "Override individual da base de abono pecuniário: I (herda), D (sobre direito), G (sobre gozo) ou N (sem abono).",
      nullable = true)
  @BulkEditable
  @UISchema(
      label = "Override base de abono",
      helpText = "Use I (herda), D (direito), G (gozo) ou N (sem abono).",
      group = "Overrides Individuais (Tarefa 24610)",
      order = 250,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      icon = "tune")
  private String abonoDirGozo = "I";

  @Pattern(regexp = "I|S|N")
  @Schema(
      description =
          "Override individual do terço constitucional neste PA: I (herda), S (concede) ou N (veda).",
      nullable = true)
  @BulkEditable
  @UISchema(
      label = "Override terço constitucional",
      helpText = "Use I (herda), S (habilita 1/3) ou N (desabilita 1/3).",
      group = "Overrides Individuais (Tarefa 24610)",
      order = 260,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      icon = "tune")
  private String abonoConstFer = "I";

  @Pattern(regexp = "I|S|N")
  @Schema(
      description =
          "Override individual de adiantamento de 13º salário neste PA: I (herda), S (permite) ou N (bloqueia).",
      nullable = true)
  @BulkEditable
  @UISchema(
      label = "Override adiantamento 13º",
      helpText = "Use I (herda), S (permite) ou N (bloqueia).",
      group = "Overrides Individuais (Tarefa 24610)",
      order = 270,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      icon = "tune")
  private String adiant13Fer = "I";

  @Pattern(regexp = "I|S|N")
  @Schema(
      description =
          "Override individual de adiantamento salarial neste PA: I (herda), S (permite) ou N (bloqueia).",
      nullable = true)
  @BulkEditable
  @UISchema(
      label = "Override adiant. salarial",
      helpText = "Use I (herda), S (permite) ou N (bloqueia).",
      group = "Overrides Individuais (Tarefa 24610)",
      order = 280,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      icon = "tune")
  private String adiantSalFer = "I";

  @Pattern(regexp = "I|S|N")
  @Schema(
      description =
          "Override individual de empréstimo de férias neste PA: I (herda), S (permite) ou N (bloqueia).",
      nullable = true)
  @BulkEditable
  @UISchema(
      label = "Override empréstimo",
      helpText = "Use I (herda), S (permite) ou N (bloqueia).",
      group = "Overrides Individuais (Tarefa 24610)",
      order = 290,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      icon = "tune")
  private String emprestFer = "I";

  @Pattern(regexp = "I|S|N")
  @Schema(
      description =
          "Override individual de permissão de gozo antes do fim deste PA: I (herda), S (permite) ou N (bloqueia).",
      nullable = true)
  @BulkEditable
  @UISchema(
      label = "Override gozo antecipado",
      helpText = "Use I (herda), S (permite gozo no PA) ou N (bloqueia).",
      group = "Overrides Individuais (Tarefa 24610)",
      order = 300,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      icon = "tune")
  private String permiteFerEstePer = "I";

  @Min(0)
  @Max(90)
  @Schema(
      description =
          "Override individual do máximo de dias vendáveis em abono pecuniário neste PA (nulo = herda).",
      nullable = true)
  @BulkEditable(allowClear = true)
  @UISchema(
      label = "Override máx. venda abono",
      helpText = "Sobrescreve o máximo de dias de abono pecuniário neste PA.",
      group = "Overrides Individuais (Tarefa 24610)",
      order = 310,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "tune")
  private Integer maxDiasVendFer;

  @Min(0)
  @Max(90)
  @Schema(
      description =
          "Override individual do mínimo de dias para venda de abono pecuniário neste PA (nulo = herda).",
      nullable = true)
  @BulkEditable(allowClear = true)
  @UISchema(
      label = "Override mín. venda abono",
      helpText = "Sobrescreve o mínimo de dias para venda de abono neste PA.",
      group = "Overrides Individuais (Tarefa 24610)",
      order = 320,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "tune")
  private Integer minDiasPvendFer;

  @Min(0)
  @Max(90)
  @Schema(
      description =
          "Override individual do numerador da fração de abono pecuniário neste PA (nulo = herda).",
      nullable = true)
  @BulkEditable(allowClear = true)
  @UISchema(
      label = "Override numerador abono",
      helpText = "Sobrescreve o numerador da fração de abono neste PA.",
      group = "Overrides Individuais (Tarefa 24610)",
      order = 330,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "tune")
  private Integer numDiasAbonoFer;

  @Min(1)
  @Max(90)
  @Schema(
      description =
          "Override individual do denominador da fração de abono pecuniário neste PA (nulo = herda).",
      nullable = true)
  @BulkEditable(allowClear = true)
  @UISchema(
      label = "Override denominador abono",
      helpText = "Sobrescreve o denominador da fração de abono neste PA.",
      group = "Overrides Individuais (Tarefa 24610)",
      order = 340,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "tune")
  private Integer denDiasAbonoFer;

  @Pattern(regexp = "I|S|N")
  @Schema(
      description =
          "Override individual de contagem em dobro de férias neste PA: I (herda), S (permite) ou N (bloqueia).",
      nullable = true)
  @BulkEditable
  @UISchema(
      label = "Override contagem em dobro",
      helpText = "Use I (herda), S (permite dobro) ou N (bloqueia dobro).",
      group = "Overrides Individuais (Tarefa 24610)",
      order = 350,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      icon = "tune")
  private String contDobroFerias = "I";

  @Min(0)
  @Max(90)
  @Schema(
      description =
          "Override individual do mínimo de dias para contagem em dobro neste PA (nulo = herda).",
      nullable = true)
  @BulkEditable(allowClear = true)
  @UISchema(
      label = "Override mín. dias dobro",
      helpText = "Sobrescreve o mínimo de dias para contagem em dobro neste PA.",
      group = "Overrides Individuais (Tarefa 24610)",
      order = 360,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "tune")
  private Integer minDiasPeraqfer;

  @Min(0)
  @Max(90)
  @Schema(
      description =
          "Override individual do máximo de dias para contagem em dobro neste PA (nulo = herda).",
      nullable = true)
  @BulkEditable(allowClear = true)
  @UISchema(
      label = "Override máx. dias dobro",
      helpText = "Sobrescreve o máximo de dias para contagem em dobro neste PA.",
      group = "Overrides Individuais (Tarefa 24610)",
      order = 370,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "tune")
  private Integer maxDiasPeraqfer;

  @NotNull
  @Schema(
      description =
          "Indica se o período aquisitivo foi gerado automaticamente por averbação de tempo externo (bloqueio ERG-00099).")
  @BulkEditable
  @UISchema(
      label = "Gerado por averbação",
      helpText = "Indica período originado por averbação com restrição de edição direta.",
      group = "Governança e Origem",
      order = 380,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "lock")
  private Boolean geradoPorAverbacao = false;

  @Schema(
      description =
          "Identificador do documento legal ou portaria que fundamenta o período aquisitivo.",
      nullable = true)
  @BulkEditable(allowClear = true)
  @UISchema(
      label = "Documento comprobatório",
      helpText = "Portaria ou despacho que fundamenta o período aquisitivo.",
      group = "Governança e Origem",
      order = 390,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "description",
      endpoint = ApiPaths.DOCUMENTO_OPTIONS,
      valueField = "id",
      displayField = "label")
  private Long documentoLegalId;

  @Schema(
      description =
          "Identificador do ato oficial de publicação da escala ou homologação do período aquisitivo.",
      nullable = true)
  @BulkEditable(allowClear = true)
  @UISchema(
      label = "Ato de publicação",
      helpText = "Publicação em diário oficial referente ao período aquisitivo.",
      group = "Governança e Origem",
      order = 400,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "gavel",
      endpoint = ApiPaths.DOCUMENTO_OPTIONS,
      valueField = "id",
      displayField = "label")
  private Long atoPublicacaoId;

  @Size(max = 2000)
  @Schema(
      description =
          "Observações funcionais, histórico de intercorrências e anotações de auditoria sobre o período aquisitivo.",
      nullable = true)
  @BulkEditable(allowClear = true)
  @UISchema(
      label = "Observações do período",
      helpText = "Anotações administrativas sobre o ciclo aquisitivo.",
      group = "Governança e Origem",
      order = 410,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.TEXTAREA,
      icon = "notes")
  private String observacoes;

  @Schema(
      description =
          "Atributos adicionais dinâmicos do período aquisitivo governados pelo catálogo de campos customizados.")
  @CustomFields(resourceKey = "administracao-pessoal.periodos-aquisitivos-ferias")
  @UISchema(
      label = "Campos customizados",
      helpText = "Atributos específicos configurados pelo órgão para períodos aquisitivos.",
      group = "Campos Customizados",
      order = 420)
  private Map<String, Object> customFields = new LinkedHashMap<>();
}
