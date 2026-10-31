package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.domain.VinculoId;
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
    titleField = "periodoResumo",
    displayLabelField = "periodoResumo",
    subtitleField = "pessoaNome",
    metadataFields = {"vinculoMatricula", "exercicio", "saldoDiasDisponivel", "situacao"})
@Schema(
    description =
        "Projeção completa do período aquisitivo de férias do vínculo funcional, com contabilidade de direito, faltas, suspensões, saldo disponível, prescrição e overrides individuais.")
public class PeriodoAquisitivoFeriasResponseDTO {

  @Schema(
      description = "Identificador único surrogate do período aquisitivo de férias.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "ID do PA",
      helpText = "Chave surrogate imutável do período aquisitivo.",
      preset = UISchemaPreset.ENTERPRISE_ID,
      group = "Identificação e Vínculo",
      order = 1,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "tag",
      readOnly = true,
      tableHidden = true,
      formHidden = true)
  private Long id;

  @ResourceReference(
      resourceKey = "administracao-pessoal.periodos-aquisitivos-ferias",
      valueField = "id",
      surfaceId = "ficha-periodo-aquisitivo",
      previewSurfaceId = "ficha-periodo-aquisitivo",
      detailPresentation = "drawer")
  @Schema(
      description =
          "Síntese executiva do período aquisitivo combinando sequencial, exercício e datas de apuração.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Período aquisitivo",
      helpText = "Identificação resumida do ciclo aquisitivo de férias.",
      group = "Identificação e Vínculo",
      order = 5,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "date_range",
      readOnly = true,
      tableHidden = false,
      formHidden = true)
  private String periodoResumo;

  @ResourceReference(
      resourceKey = "administracao-pessoal.empresas",
      valueField = "empCodigo",
      surfaceId = "ficha-institucional",
      previewSurfaceId = "ficha-institucional",
      detailPresentation = "drawer")
  @Schema(
      description = "Código do órgão público de lotação do vínculo funcional.",
      accessMode = Schema.AccessMode.READ_ONLY)
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
      description = "Nome oficial do órgão público de lotação do vínculo.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Nome do órgão",
      helpText = "Denominação oficial do órgão público.",
      group = "Identificação e Vínculo",
      order = 15,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "domain",
      readOnly = true,
      tableHidden = true,
      formHidden = true)
  private String orgaoNome;

  @ResourceReference(
      resourceKey = "administracao-pessoal.funcionarios",
      valueField = "numfunc",
      surfaceId = "ficha-cadastral",
      previewSurfaceId = "ficha-cadastral",
      detailPresentation = "drawer")
  @Schema(
      description = "Número funcional do servidor titular do período aquisitivo.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Servidor",
      helpText = "Pessoa servidora titular do período aquisitivo.",
      group = "Identificação e Vínculo",
      order = 20,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "person",
      endpoint = ApiPaths.PESSOA_OPTIONS,
      valueField = "id",
      displayField = "label",
      readOnly = true,
      tableHidden = true)
  private Long numfunc;

  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Identificação civil do servidor titular do período aquisitivo.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @ResourceReference(
      resourceKey = "administracao-pessoal.funcionarios",
      valueField = "numfunc",
      surfaceId = "ficha-cadastral",
      previewSurfaceId = "ficha-cadastral",
      detailPresentation = "drawer")
  @Schema(
      description = "Nome civil completo do servidor titular do período aquisitivo.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Nome do servidor",
      helpText = "Nome civil da pessoa servidora titular.",
      group = "Identificação e Vínculo",
      order = 25,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "badge",
      readOnly = true,
      tableHidden = false,
      formHidden = true)
  private String pessoaNome;

  @Schema(
      description = "Número sequencial do vínculo funcional do servidor.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Vínculo funcional",
      helpText = "Sequencial do vínculo funcional do servidor.",
      group = "Identificação e Vínculo",
      order = 30,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "work",
      endpoint = ApiPaths.VINCULO_OPTIONS,
      valueField = "id",
      displayField = "label",
      readOnly = true,
      tableHidden = true)
  private Integer numvinc;

  @Schema(
      description = "Identidade composta do vínculo funcional titular.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Identidade do vínculo",
      helpText = "Chave composta do vínculo funcional.",
      group = "Identificação e Vínculo",
      order = 32,
      readOnly = true,
      tableHidden = true,
      formHidden = true)
  private VinculoId vinculoId;

  @ResourceReference(
      resourceKey = "administracao-pessoal.vinculos",
      valueField = "vinculoId",
      surfaceId = "resumo-funcional",
      previewSurfaceId = "resumo-funcional",
      detailPresentation = "drawer")
  @Schema(
      description = "Matrícula corporativa do vínculo funcional titular do período aquisitivo.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Matrícula do vínculo",
      helpText = "Matrícula funcional do vínculo associado.",
      group = "Identificação e Vínculo",
      order = 35,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "assignment_ind",
      readOnly = true,
      tableHidden = false,
      formHidden = true,
      quickViewVisible = true,
      quickViewOrder = 1)
  private String vinculoMatricula;

  @Schema(
      description =
          "Chave sequencial de negócio do período aquisitivo dentro do vínculo funcional (1, 2, 3...).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Sequencial do PA",
      helpText = "Ordem sequencial do período aquisitivo no vínculo.",
      group = "Identificação e Vínculo",
      order = 40,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "tag",
      readOnly = true,
      tableHidden = true)
  private Integer chave;

  @Schema(
      description = "Ano de exercício de referência orçamentária e funcional do período aquisitivo.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Ano de exercício",
      helpText = "Ano civil de referência para programação das férias.",
      group = "Identificação e Vínculo",
      order = 50,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "calendar_month",
      readOnly = true,
      tableHidden = true,
      quickViewVisible = true,
      quickViewOrder = 2)
  private Integer exercicio;

  @Schema(
      description = "Data de início de apuração do período aquisitivo de férias.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Início do PA",
      helpText = "Data inicial do ciclo aquisitivo de férias.",
      group = "Identificação e Vínculo",
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
      description = "Data de término do ciclo aquisitivo de férias.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Término do PA",
      helpText = "Data final de fechamento do período aquisitivo.",
      group = "Identificação e Vínculo",
      order = 70,
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "event_available",
      readOnly = true,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 4)
  private LocalDate dtfim;

  @Schema(
      description =
          "Total de dias de direito a férias apurado no período aquisitivo após desconto de faltas injustificadas.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Dias de direito",
      helpText = "Quantidade de dias de férias adquiridos neste período.",
      group = "Contabilidade de Dias e Saldo",
      order = 80,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "beach_access",
      readOnly = true,
      tableHidden = false)
  private Integer totDiasDireito;

  @Schema(
      description =
          "Quantidade de faltas injustificadas apuradas durante o ciclo aquisitivo que impactam a redução proporcional de dias.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Faltas apuradas",
      helpText = "Total de faltas não abonadas computadas no período aquisitivo.",
      group = "Contabilidade de Dias e Saldo",
      order = 90,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "event_busy",
      readOnly = true,
      tableHidden = true)
  private Integer faltasApuradas;

  @Schema(
      description =
          "Quantidade de dias de licenças ou afastamentos suspensivos que postergaram o fechamento do período aquisitivo.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Dias de suspensão",
      helpText = "Dias de afastamento sem contagem para férias que prorrogaram o PA.",
      group = "Contabilidade de Dias e Saldo",
      order = 100,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "pause_circle",
      readOnly = true,
      tableHidden = true)
  private Integer diasAfastamentoSuspensivo;

  @Schema(
      description = "Total acumulado de dias efetivamente usufruídos em parcelas de gozo.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Dias gozados",
      helpText = "Soma dos dias já programados ou usufruídos em parcelas.",
      group = "Contabilidade de Dias e Saldo",
      order = 110,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "check_circle",
      readOnly = true,
      tableHidden = true)
  private Integer diasGozados;

  @Schema(
      description =
          "Total de dias do período aquisitivo convertidos em abono pecuniário indenizatório.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Dias vendidos em abono",
      helpText = "Dias do período aquisitivo convertidos em pecúnia.",
      group = "Contabilidade de Dias e Saldo",
      order = 120,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "sell",
      readOnly = true,
      tableHidden = true)
  private Integer diasVendidosAbono;

  @Schema(
      description =
          "Total de dias do período aquisitivo averbados para contagem em dobro de tempo de serviço.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Dias em contagem em dobro",
      helpText = "Dias não gozados convertidos em contagem em dobro.",
      group = "Contabilidade de Dias e Saldo",
      order = 130,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "trending_up",
      readOnly = true,
      tableHidden = true)
  private Integer diasConvertidosDobro;

  @MicroVisualization(
      kind = MicroVisualizationKind.BULLET,
      max = 30,
      total = 30,
      fallbackText = "Quitado",
      ariaLabel = "Saldo de dias disponível para gozo no período aquisitivo",
      thresholds = {
        @Threshold(min = 0, max = 5, tone = "neutral", label = "Residual"),
        @Threshold(min = 5, max = 20, tone = "info", label = "Parcial"),
        @Threshold(min = 20, tone = "positive", label = "Integral")
      })
  @Schema(
      description =
          "Saldo remanescente de dias disponíveis para gozo ou abono (totDiasDireito - diasGozados - diasVendidosAbono - diasConvertidosDobro).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Saldo disponível",
      helpText = "Saldo remanescente de dias disponíveis para gozo ou conversão.",
      group = "Contabilidade de Dias e Saldo",
      order = 140,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "hourglass_bottom",
      readOnly = true,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 5)
  private Integer saldoDiasDisponivel;

  @Schema(
      description =
          "Situação operacional do período aquisitivo (EM_AQUISICAO, DISPONIVEL, PARCIALMENTE_GOZADO, QUITADO, PRESCRITO, SUSPENSO, CANCELADO).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Situação do PA",
      helpText = "Estado atual do ciclo aquisitivo de férias.",
      group = "Contabilidade de Dias e Saldo",
      order = 145,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      icon = "flag",
      readOnly = true,
      tableHidden = false)
  private String situacao;

  @Schema(
      description = "Data limite de prescrição para fruição das férias deste período aquisitivo.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Data de prescrição",
      helpText = "Data limite para início ou término do gozo antes da prescrição.",
      group = "Prescrição e Multa",
      order = 150,
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "warning",
      readOnly = true,
      tableHidden = true,
      quickViewVisible = true,
      quickViewOrder = 6)
  private LocalDate dtPrescricao;

  @Schema(
      description =
          "Critério de avaliação da prescrição no período aquisitivo: I (data inicial do gozo) ou F (data final do gozo).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Critério de prescrição",
      helpText = "Indica I (pelo início do gozo) ou F (pelo término do gozo).",
      group = "Prescrição e Multa",
      order = 160,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      icon = "warning",
      readOnly = true,
      tableHidden = true)
  private String criterioPrescricao;

  @Schema(
      description =
          "Override individual de permissão de gozo após a data de prescrição: I (herda do parâmetro), S (permite) ou N (bloqueia).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Gozo após prescrição (PA)",
      helpText = "Indica I (herda da regra), S (permite) ou N (bloqueia).",
      group = "Prescrição e Multa",
      order = 170,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      icon = "warning",
      readOnly = true,
      tableHidden = true)
  private String permiteGozoPosPrescricao;

  @Schema(
      description =
          "Data a partir da qual a concessão tardia das férias enseja pagamento de dobra ou multa concessiva.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Data limite sem multa",
      helpText = "Data a partir da qual incide multa por atraso concessivo.",
      group = "Prescrição e Multa",
      order = 180,
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "gavel",
      readOnly = true,
      tableHidden = true)
  private LocalDate dtPgtoComMulta;

  @Schema(
      description =
          "Indicador de dispensa do pagamento de multa concessiva para este período: I (herda), S (isenta multa) ou N (cobra multa).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Dispensa de multa (PA)",
      helpText = "Indica I (herda), S (dispensa multa) ou N (aplica multa).",
      group = "Prescrição e Multa",
      order = 190,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      icon = "gavel",
      readOnly = true,
      tableHidden = true)
  private String semPgtoMulta;

  @Schema(
      description =
          "Override individual do máximo de parcelas permitido para este período aquisitivo.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Override máx. parcelas",
      helpText = "Sobrescreve o máximo de parcelas para este PA.",
      group = "Overrides Individuais (Tarefa 24610)",
      order = 200,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "tune",
      readOnly = true,
      tableHidden = true)
  private Integer maxPerFer;

  @Schema(
      description =
          "Override individual do mínimo de parcelas exigido para este período aquisitivo.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Override mín. parcelas",
      helpText = "Sobrescreve o mínimo de parcelas para este PA.",
      group = "Overrides Individuais (Tarefa 24610)",
      order = 210,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "tune",
      readOnly = true,
      tableHidden = true)
  private Integer minPerFer;

  @Schema(
      description =
          "Override individual do máximo de dias por parcela neste período aquisitivo.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Override máx. dias parcela",
      helpText = "Sobrescreve o teto de dias por parcela neste PA.",
      group = "Overrides Individuais (Tarefa 24610)",
      order = 220,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "tune",
      readOnly = true,
      tableHidden = true)
  private Integer maxDiasPerFer;

  @Schema(
      description =
          "Override individual do mínimo de dias por parcela neste período aquisitivo.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Override mín. dias parcela",
      helpText = "Sobrescreve o mínimo de dias por parcela neste PA.",
      group = "Overrides Individuais (Tarefa 24610)",
      order = 230,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "tune",
      readOnly = true,
      tableHidden = true)
  private Integer minDiasPerFer;

  @Schema(
      description =
          "Override individual do mínimo de dias na parcela principal neste período aquisitivo.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Override mín. principal",
      helpText = "Sobrescreve a duração mínima da parcela principal neste PA.",
      group = "Overrides Individuais (Tarefa 24610)",
      order = 240,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "tune",
      readOnly = true,
      tableHidden = true)
  private Integer minDiasPerPrincFer;

  @Schema(
      description =
          "Override individual da base de abono pecuniário: I (herda), D (sobre direito), G (sobre gozo) ou N (sem abono).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Override base de abono",
      helpText = "Indica I (herda), D (direito), G (gozo) ou N (sem abono).",
      group = "Overrides Individuais (Tarefa 24610)",
      order = 250,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      icon = "tune",
      readOnly = true,
      tableHidden = true)
  private String abonoDirGozo;

  @Schema(
      description =
          "Override individual do terço constitucional neste PA: I (herda), S (concede) ou N (veda).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Override terço constitucional",
      helpText = "Indica I (herda), S (habilita 1/3) ou N (desabilita 1/3).",
      group = "Overrides Individuais (Tarefa 24610)",
      order = 260,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      icon = "tune",
      readOnly = true,
      tableHidden = true)
  private String abonoConstFer;

  @Schema(
      description =
          "Override individual de adiantamento de 13º salário neste PA: I (herda), S (permite) ou N (bloqueia).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Override adiantamento 13º",
      helpText = "Indica I (herda), S (permite) ou N (bloqueia).",
      group = "Overrides Individuais (Tarefa 24610)",
      order = 270,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      icon = "tune",
      readOnly = true,
      tableHidden = true)
  private String adiant13Fer;

  @Schema(
      description =
          "Override individual de adiantamento salarial neste PA: I (herda), S (permite) ou N (bloqueia).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Override adiant. salarial",
      helpText = "Indica I (herda), S (permite) ou N (bloqueia).",
      group = "Overrides Individuais (Tarefa 24610)",
      order = 280,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      icon = "tune",
      readOnly = true,
      tableHidden = true)
  private String adiantSalFer;

  @Schema(
      description =
          "Override individual de empréstimo de férias neste PA: I (herda), S (permite) ou N (bloqueia).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Override empréstimo",
      helpText = "Indica I (herda), S (permite) ou N (bloqueia).",
      group = "Overrides Individuais (Tarefa 24610)",
      order = 290,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      icon = "tune",
      readOnly = true,
      tableHidden = true)
  private String emprestFer;

  @Schema(
      description =
          "Override individual de permissão de gozo antes do fim deste PA: I (herda), S (permite) ou N (bloqueia).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Override gozo antecipado",
      helpText = "Indica I (herda), S (permite gozo no PA) ou N (bloqueia).",
      group = "Overrides Individuais (Tarefa 24610)",
      order = 300,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      icon = "tune",
      readOnly = true,
      tableHidden = true)
  private String permiteFerEstePer;

  @Schema(
      description =
          "Override individual do máximo de dias vendáveis em abono pecuniário neste PA.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Override máx. venda abono",
      helpText = "Sobrescreve o máximo de dias de abono pecuniário neste PA.",
      group = "Overrides Individuais (Tarefa 24610)",
      order = 310,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "tune",
      readOnly = true,
      tableHidden = true)
  private Integer maxDiasVendFer;

  @Schema(
      description =
          "Override individual do mínimo de dias para venda de abono pecuniário neste PA.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Override mín. venda abono",
      helpText = "Sobrescreve o mínimo de dias para venda de abono neste PA.",
      group = "Overrides Individuais (Tarefa 24610)",
      order = 320,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "tune",
      readOnly = true,
      tableHidden = true)
  private Integer minDiasPvendFer;

  @Schema(
      description =
          "Override individual do numerador da fração de abono pecuniário neste PA.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Override numerador abono",
      helpText = "Sobrescreve o numerador da fração de abono neste PA.",
      group = "Overrides Individuais (Tarefa 24610)",
      order = 330,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "tune",
      readOnly = true,
      tableHidden = true)
  private Integer numDiasAbonoFer;

  @Schema(
      description =
          "Override individual do denominador da fração de abono pecuniário neste PA.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Override denominador abono",
      helpText = "Sobrescreve o denominador da fração de abono neste PA.",
      group = "Overrides Individuais (Tarefa 24610)",
      order = 340,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "tune",
      readOnly = true,
      tableHidden = true)
  private Integer denDiasAbonoFer;

  @Schema(
      description =
          "Override individual de contagem em dobro de férias neste PA: I (herda), S (permite) ou N (bloqueia).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Override contagem em dobro",
      helpText = "Indica I (herda), S (permite dobro) ou N (bloqueia dobro).",
      group = "Overrides Individuais (Tarefa 24610)",
      order = 350,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      icon = "tune",
      readOnly = true,
      tableHidden = true)
  private String contDobroFerias;

  @Schema(
      description =
          "Override individual do mínimo de dias para contagem em dobro neste PA.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Override mín. dias dobro",
      helpText = "Sobrescreve o mínimo de dias para contagem em dobro neste PA.",
      group = "Overrides Individuais (Tarefa 24610)",
      order = 360,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "tune",
      readOnly = true,
      tableHidden = true)
  private Integer minDiasPeraqfer;

  @Schema(
      description =
          "Override individual do máximo de dias para contagem em dobro neste PA.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Override máx. dias dobro",
      helpText = "Sobrescreve o máximo de dias para contagem em dobro neste PA.",
      group = "Overrides Individuais (Tarefa 24610)",
      order = 370,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "tune",
      readOnly = true,
      tableHidden = true)
  private Integer maxDiasPeraqfer;

  @Schema(
      description =
          "Indica se o período aquisitivo foi gerado automaticamente por averbação de tempo externo.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Gerado por averbação",
      helpText = "Indica período originado por averbação com restrição de edição direta.",
      group = "Governança e Origem",
      order = 380,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "lock",
      readOnly = true,
      tableHidden = true)
  private Boolean geradoPorAverbacao;

  @ResourceReference(
      resourceKey = "administracao-pessoal.documentos-legais",
      valueField = "documentoLegalId",
      surfaceId = "ficha-documento",
      previewSurfaceId = "ficha-documento",
      detailPresentation = "drawer")
  @Schema(
      description =
          "Identificador do documento legal ou portaria que fundamenta o período aquisitivo.",
      accessMode = Schema.AccessMode.READ_ONLY)
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
      displayField = "label",
      readOnly = true,
      tableHidden = true)
  private Long documentoLegalId;

  @ResourceReference(
      resourceKey = "administracao-pessoal.documentos-legais",
      valueField = "documentoLegalId",
      surfaceId = "ficha-documento",
      previewSurfaceId = "ficha-documento",
      detailPresentation = "drawer")
  @Schema(
      description = "Número oficial do documento legal que fundamenta o período aquisitivo.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Número do documento",
      helpText = "Identificação oficial da portaria ou documento.",
      group = "Governança e Origem",
      order = 395,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "description",
      readOnly = true,
      tableHidden = true,
      formHidden = true)
  private String documentoLegalNumero;

  @ResourceReference(
      resourceKey = "administracao-pessoal.documentos-legais",
      valueField = "atoPublicacaoId",
      surfaceId = "ficha-documento",
      previewSurfaceId = "ficha-documento",
      detailPresentation = "drawer")
  @Schema(
      description =
          "Identificador do ato oficial de publicação da escala ou homologação do período aquisitivo.",
      accessMode = Schema.AccessMode.READ_ONLY)
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
      displayField = "label",
      readOnly = true,
      tableHidden = true)
  private Long atoPublicacaoId;

  @ResourceReference(
      resourceKey = "administracao-pessoal.documentos-legais",
      valueField = "atoPublicacaoId",
      surfaceId = "ficha-documento",
      previewSurfaceId = "ficha-documento",
      detailPresentation = "drawer")
  @Schema(
      description = "Número oficial do ato de publicação associado ao período aquisitivo.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Número da publicação",
      helpText = "Identificação oficial da publicação do período aquisitivo.",
      group = "Governança e Origem",
      order = 405,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "gavel",
      readOnly = true,
      tableHidden = true,
      formHidden = true)
  private String atoPublicacaoNumero;

  @Schema(
      description =
          "Observações funcionais, histórico de intercorrências e anotações de auditoria sobre o período aquisitivo.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Observações do período",
      helpText = "Anotações administrativas sobre o ciclo aquisitivo.",
      group = "Governança e Origem",
      order = 410,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.TEXTAREA,
      icon = "notes",
      readOnly = true,
      tableHidden = true)
  private String observacoes;

  @Schema(
      description =
          "Fato derivado que indica se o período aquisitivo possui saldo líquido de dias disponível para programação de férias.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Com saldo disponível",
      helpText = "Verdadeiro quando há saldo líquido positivo disponível para gozo.",
      group = "Contabilidade de Direito e Saldo",
      order = 145,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "event_available",
      readOnly = true,
      tableHidden = true)
  private Boolean possuiSaldoDisponivel;

  @Schema(
      description =
          "Fato derivado que indica se o período aquisitivo possui overrides individuais ativos sobre os parâmetros de contagem de férias.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Com override individual",
      helpText = "Verdadeiro quando o período aquisitivo sobrescreve regras estatutárias.",
      group = "Governança e Origem",
      order = 375,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "tune",
      readOnly = true,
      tableHidden = true)
  private Boolean possuiOverridesAtivos;

  @Schema(
      description =
          "Fato derivado que indica se o período aquisitivo possui saldo disponível próximo da prescrição ou já prescrito.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Em risco de prescrição",
      helpText = "Verdadeiro quando há prescrição registrada ou risco de prescrição com saldo.",
      group = "Prescrição e Dobro de Férias",
      order = 195,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "warning",
      readOnly = true,
      tableHidden = true)
  private Boolean emRiscoPrescricao;

  @Schema(
      description =
          "Atributos adicionais dinâmicos do período aquisitivo governados pelo catálogo de campos customizados.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @CustomFields(resourceKey = "administracao-pessoal.periodos-aquisitivos-ferias")
  @UISchema(
      label = "Campos customizados",
      helpText = "Atributos específicos configurados pelo órgão para períodos aquisitivos.",
      group = "Campos Customizados",
      order = 420,
      readOnly = true,
      tableHidden = true)
  private Map<String, Object> customFields = new LinkedHashMap<>();
}
