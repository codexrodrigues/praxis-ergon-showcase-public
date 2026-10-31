package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.domain.VinculoId;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
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
    titleField = "periodoResumo",
    displayLabelField = "periodoResumo",
    subtitleField = "pessoaNome",
    metadataFields = {
      "vinculoMatricula",
      "exercicio",
      "faixaUrgenciaPrescricao",
      "saldoDiasDisponivel"
    })
@Schema(
    description =
        "Projeção analítica executiva de saldos de períodos aquisitivos de férias, queima de direito, cobertura de programação futura, risco de prescrição e acúmulo de ciclos vencidos.")
public class RadarFeriasPrescricaoResponseDTO {

  @Schema(
      description =
          "Identificador único do período aquisitivo monitorado no radar de prescrição de férias.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "ID do período",
      helpText = "Chave surrogate do período aquisitivo monitorado.",
      preset = UISchemaPreset.ENTERPRISE_ID,
      group = "Identificação e Lotação",
      order = 1,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "tag",
      readOnly = true,
      tableHidden = true,
      formHidden = true)
  private Long id;

  @ResourceReference(
      resourceKey = "administracao-pessoal.radar-ferias-prescricao",
      valueField = "id",
      surfaceId = "detail",
      previewSurfaceId = "detail",
      detailPresentation = "drawer")
  @Schema(
      description =
          "Síntese executiva do período aquisitivo combinando sequencial, exercício, intervalo aquisitivo e faixa de urgência.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Período monitorado",
      helpText = "Identificação resumida do ciclo aquisitivo no radar de prescrição.",
      group = "Identificação e Lotação",
      order = 5,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "radar",
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
      description = "Código do órgão público de lotação do vínculo funcional monitorado.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Órgão",
      helpText = "Órgão público titular do vínculo funcional.",
      group = "Identificação e Lotação",
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
      description = "Denominação oficial do órgão público de lotação do vínculo funcional.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Nome do órgão",
      helpText = "Nome oficial do órgão público do vínculo.",
      group = "Identificação e Lotação",
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
      description = "Número funcional único do servidor público titular do período aquisitivo.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Servidor",
      helpText = "Pessoa servidora titular do período aquisitivo.",
      group = "Identificação e Lotação",
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
      reason = "Identificação civil do servidor titular no radar de prescrição de férias.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @ResourceReference(
      resourceKey = "administracao-pessoal.funcionarios",
      valueField = "numfunc",
      surfaceId = "ficha-cadastral",
      previewSurfaceId = "ficha-cadastral",
      detailPresentation = "drawer")
  @Schema(
      description = "Nome civil completo do servidor público titular do período aquisitivo.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Nome do servidor",
      helpText = "Nome civil da pessoa servidora titular.",
      group = "Identificação e Lotação",
      order = 25,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "badge",
      readOnly = true,
      tableHidden = false,
      formHidden = true)
  private String pessoaNome;

  @Schema(
      description = "Número sequencial do vínculo funcional do servidor público.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Vínculo funcional",
      helpText = "Sequencial do vínculo funcional do servidor.",
      group = "Identificação e Lotação",
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
      description = "Identidade composta do vínculo funcional monitorado.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Identidade do vínculo",
      helpText = "Chave composta do vínculo funcional.",
      group = "Identificação e Lotação",
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
      description = "Matrícula institucional do vínculo funcional titular do período aquisitivo.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Matrícula do vínculo",
      helpText = "Matrícula funcional institucional do vínculo.",
      group = "Identificação e Lotação",
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

  @ResourceReference(
      resourceKey = "administracao-pessoal.setores",
      valueField = "setorCodigo",
      surfaceId = "ficha-unidade",
      previewSurfaceId = "ficha-unidade",
      detailPresentation = "drawer")
  @Schema(
      description =
          "Código estruturado da unidade organizacional (setor) de lotação do vínculo funcional.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Código do setor",
      helpText = "Código da unidade organizacional de lotação.",
      group = "Identificação e Lotação",
      order = 40,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "account_tree",
      endpoint = ApiPaths.SETOR_OPTIONS,
      valueField = "id",
      displayField = "label",
      readOnly = true,
      tableHidden = true)
  private String setorCodigo;

  @ResourceReference(
      resourceKey = "administracao-pessoal.setores",
      valueField = "setorCodigo",
      surfaceId = "ficha-unidade",
      previewSurfaceId = "ficha-unidade",
      detailPresentation = "drawer")
  @Schema(
      description =
          "Denominação oficial da unidade organizacional (setor) de lotação do vínculo funcional.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Setor de lotação",
      helpText = "Unidade organizacional de lotação atual do vínculo.",
      group = "Identificação e Lotação",
      order = 45,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "corporate_fare",
      readOnly = true,
      tableHidden = false,
      formHidden = true,
      quickViewVisible = true,
      quickViewOrder = 2)
  private String setorNome;

  @ResourceReference(
      resourceKey = "administracao-pessoal.cargos",
      valueField = "cargoCodigo",
      surfaceId = "ficha-cargo",
      previewSurfaceId = "ficha-cargo",
      detailPresentation = "drawer")
  @Schema(
      description = "Código numérico identificador do cargo público ocupado pelo vínculo funcional.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Código do cargo",
      helpText = "Código identificador do cargo público.",
      group = "Identificação e Lotação",
      order = 50,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "work_outline",
      endpoint = ApiPaths.CARGO_OPTIONS,
      valueField = "id",
      displayField = "label",
      readOnly = true,
      tableHidden = true)
  private Integer cargoCodigo;

  @ResourceReference(
      resourceKey = "administracao-pessoal.cargos",
      valueField = "cargoCodigo",
      surfaceId = "ficha-cargo",
      previewSurfaceId = "ficha-cargo",
      detailPresentation = "drawer")
  @Schema(
      description = "Nomenclatura oficial do cargo público ocupado pelo vínculo funcional.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Cargo público",
      helpText = "Nomenclatura oficial do cargo ocupado.",
      group = "Identificação e Lotação",
      order = 55,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "badge",
      readOnly = true,
      tableHidden = true,
      formHidden = true)
  private String cargoNome;

  @Schema(
      description =
          "Regime jurídico do vínculo funcional (ex.: ESTATUTARIO, CLT, ESPECIAL, COMISSIONADO).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Regime jurídico",
      helpText = "Regime jurídico de regência do vínculo funcional.",
      group = "Identificação e Lotação",
      order = 60,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      icon = "gavel",
      readOnly = true,
      tableHidden = true)
  private String regimeJuridico;

  @Schema(
      description =
          "Tipo de vínculo funcional com a administração pública (ex.: EFETIVO, COMISSAO, TEMPORARIO).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Tipo de vínculo",
      helpText = "Classificação funcional do vínculo no quadro.",
      group = "Identificação e Lotação",
      order = 65,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      icon = "work_history",
      readOnly = true,
      tableHidden = true)
  private String tipoVinculo;

  @Schema(
      description =
          "Categoria funcional de enquadramento do vínculo (ex.: GERAL, MAGISTERIO, SAUDE, GUARDA_MUNICIPAL).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Categoria funcional",
      helpText = "Carreira ou segmento funcional do vínculo.",
      group = "Identificação e Lotação",
      order = 70,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      icon = "category",
      readOnly = true,
      tableHidden = true)
  private String categoria;

  @ResourceReference(
      resourceKey = "administracao-pessoal.periodos-aquisitivos-ferias",
      valueField = "id",
      surfaceId = "ficha-periodo-aquisitivo",
      previewSurfaceId = "ficha-periodo-aquisitivo",
      detailPresentation = "drawer")
  @Schema(
      description =
          "Chave sequencial de negócio do período aquisitivo dentro do vínculo funcional (1, 2, 3...).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Sequencial do PA",
      helpText = "Ordem sequencial do período aquisitivo no vínculo.",
      group = "Ciclo e Saldos de Férias",
      order = 80,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "format_list_numbered",
      readOnly = true,
      tableHidden = true)
  private Integer chave;

  @Schema(
      description = "Data inicial de apuração do período aquisitivo de férias.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Início do PA",
      helpText = "Data inicial do ciclo aquisitivo de férias.",
      group = "Ciclo e Saldos de Férias",
      order = 85,
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "event",
      readOnly = true,
      tableHidden = true)
  private LocalDate dtini;

  @Schema(
      description = "Data final de fechamento do período aquisitivo de férias.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Término do PA",
      helpText = "Data final de fechamento do ciclo aquisitivo.",
      group = "Ciclo e Saldos de Férias",
      order = 90,
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "event_available",
      readOnly = true,
      tableHidden = true)
  private LocalDate dtfim;

  @Schema(
      description = "Ano de exercício de referência orçamentária e funcional do período aquisitivo.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Ano de exercício",
      helpText = "Ano civil de referência para programação das férias.",
      group = "Ciclo e Saldos de Férias",
      order = 95,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "calendar_month",
      readOnly = true,
      tableHidden = true)
  private Integer exercicio;

  @Schema(
      description =
          "Situação operacional do período aquisitivo (EM_AQUISICAO, DISPONIVEL, PARCIALMENTE_GOZADO, QUITADO, PRESCRITO, CONVERTIDO_DOBRO, SUSPENSO, CANCELADO).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Situação do PA",
      helpText = "Estado operacional atual do ciclo aquisitivo.",
      group = "Ciclo e Saldos de Férias",
      order = 100,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      icon = "flag",
      readOnly = true,
      tableHidden = true)
  private String situacao;

  @Schema(
      description =
          "Total de dias de direito a férias apurado no ciclo após desconto de faltas injustificadas.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Dias de direito",
      helpText = "Quantidade de dias de férias adquiridos no ciclo.",
      group = "Ciclo e Saldos de Férias",
      order = 105,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "beach_access",
      readOnly = true,
      tableHidden = true)
  private Integer totDiasDireito;

  @Schema(
      description = "Quantidade de faltas injustificadas apuradas no ciclo aquisitivo.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Faltas apuradas",
      helpText = "Faltas não abonadas computadas no período aquisitivo.",
      group = "Ciclo e Saldos de Férias",
      order = 110,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "event_busy",
      readOnly = true,
      tableHidden = true)
  private Integer faltasApuradas;

  @Schema(
      description =
          "Quantidade de dias de afastamentos suspensivos que prorrogaram o fechamento do período aquisitivo.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Dias de suspensão",
      helpText = "Dias de afastamento sem contagem para férias.",
      group = "Ciclo e Saldos de Férias",
      order = 115,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "pause_circle",
      readOnly = true,
      tableHidden = true)
  private Integer diasAfastamentoSuspensivo;

  @Schema(
      description =
          "Total acumulado de dias debitados em parcelas de gozo para este período aquisitivo.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Dias gozados total",
      helpText = "Soma dos dias debitados em parcelas de gozo.",
      group = "Ciclo e Saldos de Férias",
      order = 120,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "check_circle",
      readOnly = true,
      tableHidden = true)
  private Integer diasGozadosTotal;

  @Schema(
      description =
          "Total acumulado de dias convertidos em abono pecuniário para este período aquisitivo.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Dias em abono total",
      helpText = "Soma dos dias vendidos em abono pecuniário.",
      group = "Ciclo e Saldos de Férias",
      order = 125,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "sell",
      readOnly = true,
      tableHidden = true)
  private Integer diasAbonoTotal;

  @Schema(
      description =
          "Quantidade de dias já reservados em parcelas futuras programadas ou homologadas para este período aquisitivo.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Dias programados futuros",
      helpText = "Dias reservados em parcelas programadas ou homologadas.",
      group = "Ciclo e Saldos de Férias",
      order = 130,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "upcoming",
      readOnly = true,
      tableHidden = true)
  private Integer diasProgramadosFuturos;

  @MicroVisualization(
      kind = MicroVisualizationKind.BULLET,
      max = 30,
      total = 30,
      fallbackText = "Quitado",
      ariaLabel = "Saldo líquido de dias disponível no período aquisitivo",
      thresholds = {
        @Threshold(min = 0, max = 5, tone = "neutral", label = "Residual"),
        @Threshold(min = 5, max = 20, tone = "info", label = "Parcial"),
        @Threshold(min = 20, tone = "positive", label = "Integral")
      })
  @Schema(
      description =
          "Saldo líquido remanescente de dias disponíveis para gozo ou abono no período aquisitivo.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Saldo disponível",
      helpText = "Saldo líquido de dias disponível no período aquisitivo.",
      group = "Ciclo e Saldos de Férias",
      order = 135,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "hourglass_bottom",
      readOnly = true,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 3)
  private Integer saldoDiasDisponivel;

  @MicroVisualization(
      kind = MicroVisualizationKind.HARVEY_BALL,
      max = 30,
      total = 30,
      fallbackText = "Coberto",
      ariaLabel = "Saldo de dias descoberto sem programação futura de gozo",
      thresholds = {
        @Threshold(min = 0, max = 1, tone = "positive", label = "Coberto"),
        @Threshold(min = 1, max = 15, tone = "warning", label = "Parcial"),
        @Threshold(min = 15, tone = "negative", label = "Descoberto")
      })
  @Schema(
      description =
          "Parcela do saldo disponível que ainda não possui programação futura de gozo registrada.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Saldo sem programação",
      helpText = "Dias disponíveis ainda descobertos de escala futura.",
      group = "Ciclo e Saldos de Férias",
      order = 140,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "event_repeat",
      readOnly = true,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 4)
  private Integer saldoSemProgramacao;

  @Schema(
      description = "Data limite de prescrição para fruição das férias deste período aquisitivo.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Data de prescrição",
      helpText = "Data limite para fruição antes da prescrição.",
      group = "Prescrição e Risco Operacional",
      order = 150,
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "warning",
      readOnly = true,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 5)
  private LocalDate dtPrescricao;

  @MicroVisualization(
      kind = MicroVisualizationKind.BULLET,
      max = 180,
      total = 180,
      fallbackText = "Sem prescrição",
      ariaLabel = "Dias restantes até a data limite de prescrição do período aquisitivo",
      thresholds = {
        @Threshold(min = -999, max = 0, tone = "negative", label = "Prescrito"),
        @Threshold(min = 0, max = 30, tone = "negative", label = "Crítico 30d"),
        @Threshold(min = 30, max = 60, tone = "warning", label = "Alerta 60d"),
        @Threshold(min = 60, max = 90, tone = "info", label = "Atenção 90d"),
        @Threshold(min = 90, tone = "positive", label = "Regular")
      })
  @Schema(
      description =
          "Quantidade de dias corridos restantes até a data limite de prescrição (negativo quando vencido).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Dias até prescrição",
      helpText = "Contagem regressiva em dias até o limite prescricional.",
      group = "Prescrição e Risco Operacional",
      order = 155,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "timer",
      readOnly = true,
      tableHidden = true)
  private Integer diasAtePrescricao;

  @Schema(
      description =
          "Faixa executiva de urgência prescricional (VENCIDO_PRESCRITO, CRITICO_30D, ALERTA_60D, ATENCAO_90D, REGULAR).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Faixa de urgência",
      helpText = "Classificação de risco de prescrição do período aquisitivo.",
      group = "Prescrição e Risco Operacional",
      order = 160,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      icon = "notification_important",
      readOnly = true,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 6)
  private String faixaUrgenciaPrescricao;

  @Schema(
      description =
          "Indica se o vínculo acumula dois ou mais períodos aquisitivos abertos com saldo disponível.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Risco de acúmulo de PAs",
      helpText = "Verdadeiro quando o vínculo possui 2+ períodos com saldo aberto.",
      group = "Prescrição e Risco Operacional",
      order = 165,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "layers_clear",
      readOnly = true,
      tableHidden = true)
  private Boolean emRiscoAcumuloPeriodos;

  @Schema(
      description =
          "Indica se existe ao menos uma parcela de gozo programada, homologada ou em curso para o período.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Possui parcela programada",
      helpText = "Verdadeiro quando há programação ativa para este período.",
      group = "Prescrição e Risco Operacional",
      order = 170,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "event_upcoming",
      readOnly = true,
      tableHidden = true)
  private Boolean possuiParcelaProgramada;

  @Schema(
      description =
          "Data de início da próxima parcela de gozo programada, homologada ou em fruição no período.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Início próxima parcela",
      helpText = "Data inicial da próxima parcela ativa vinculada ao PA.",
      group = "Prescrição e Risco Operacional",
      order = 175,
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "flight_takeoff",
      readOnly = true,
      tableHidden = true)
  private LocalDate proximaParcelaInicio;

  @Schema(
      description =
          "Situação operacional da próxima parcela de gozo associada ao período aquisitivo.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Situação próxima parcela",
      helpText = "Estado da próxima parcela ativa vinculada ao PA.",
      group = "Prescrição e Risco Operacional",
      order = 180,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      icon = "flag_circle",
      readOnly = true,
      tableHidden = true)
  private String proximaParcelaSituacao;

  @Schema(
      description =
          "Fato determinístico que destaca períodos vencidos/prescritos ou em janela crítica de até 30 dias.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Urgência crítica ou vencida",
      helpText = "Verdadeiro quando a faixa é VENCIDO_PRESCRITO ou CRITICO_30D.",
      group = "Prescrição e Risco Operacional",
      order = 185,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "crisis_alert",
      readOnly = true,
      tableHidden = true)
  private Boolean urgenciaCriticaOuVencida;

  @Schema(
      description =
          "Fato determinístico que destaca períodos com saldo de dias ainda descoberto de programação futura.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Com saldo descoberto",
      helpText = "Verdadeiro quando saldoSemProgramacao é maior que zero.",
      group = "Prescrição e Risco Operacional",
      order = 190,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "warning_amber",
      readOnly = true,
      tableHidden = true)
  private Boolean possuiSaldoDescoberto;

  @Schema(
      description =
          "Fato determinístico que consolida risco imediato por prescrição crítica/vencida ou acúmulo de períodos.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Risco imediato de governança",
      helpText = "Verdadeiro quando há acúmulo de PAs ou urgência crítica.",
      group = "Prescrição e Risco Operacional",
      order = 195,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "gpp_maybe",
      readOnly = true,
      tableHidden = true)
  private Boolean emRiscoImediatoGovernanca;
}
