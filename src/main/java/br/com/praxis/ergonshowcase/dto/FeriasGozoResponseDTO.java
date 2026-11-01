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
    titleField = "parcelaResumo",
    displayLabelField = "parcelaResumo",
    subtitleField = "pessoaNome",
    metadataFields = {"vinculoMatricula", "ordemParcela", "diasGozo", "situacao"})
@Schema(
    description =
        "Projeção completa da parcela de programação e gozo de férias, com venda de abono pecuniário, opções financeiras, competência de folha, substituto e situação operacional.")
public class FeriasGozoResponseDTO {

  @Schema(
      description = "Identificador único surrogate da parcela de gozo de férias.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "ID da parcela",
      helpText = "Chave surrogate imutável da parcela de férias.",
      preset = UISchemaPreset.ENTERPRISE_ID,
      group = "Vínculo e Período Aquisitivo",
      order = 1,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "tag",
      readOnly = true,
      tableHidden = true,
      formHidden = true)
  private Long id;

  @ResourceReference(
      resourceKey = "administracao-pessoal.ferias-gozo",
      valueField = "id",
      surfaceId = "ficha-ferias-gozo",
      previewSurfaceId = "ficha-ferias-gozo",
      detailPresentation = "drawer")
  @Schema(
      description =
          "Síntese executiva da parcela combinando ordem, período aquisitivo e datas de fruição.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Parcela de férias",
      helpText = "Identificação resumida da etapa de gozo de férias.",
      group = "Vínculo e Período Aquisitivo",
      order = 5,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "beach_access",
      readOnly = true,
      tableHidden = false,
      formHidden = true)
  private String parcelaResumo;

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
      group = "Vínculo e Período Aquisitivo",
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
      group = "Vínculo e Período Aquisitivo",
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
      description = "Número funcional do servidor titular da programação de férias.",
      accessMode = Schema.AccessMode.READ_ONLY)
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
      displayField = "label",
      readOnly = true,
      tableHidden = true)
  private Long numfunc;

  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Identificação civil do servidor titular do gozo de férias.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @ResourceReference(
      resourceKey = "administracao-pessoal.funcionarios",
      valueField = "numfunc",
      surfaceId = "ficha-cadastral",
      previewSurfaceId = "ficha-cadastral",
      detailPresentation = "drawer")
  @Schema(
      description = "Nome civil completo do servidor titular do gozo de férias.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Nome do servidor",
      helpText = "Nome civil da pessoa servidora titular.",
      group = "Vínculo e Período Aquisitivo",
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
      group = "Vínculo e Período Aquisitivo",
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
      group = "Vínculo e Período Aquisitivo",
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
      description = "Matrícula corporativa do vínculo funcional titular da parcela de férias.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Matrícula do vínculo",
      helpText = "Matrícula funcional do vínculo associado.",
      group = "Vínculo e Período Aquisitivo",
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
      resourceKey = "administracao-pessoal.periodos-aquisitivos-ferias",
      valueField = "periodoAquisitivoId",
      surfaceId = "ficha-periodo-aquisitivo",
      previewSurfaceId = "ficha-periodo-aquisitivo",
      detailPresentation = "drawer")
  @Schema(
      description =
          "Identificador surrogate do período aquisitivo de férias debitado por esta parcela.",
      accessMode = Schema.AccessMode.READ_ONLY)
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
      displayField = "label",
      readOnly = true,
      tableHidden = true)
  private Long periodoAquisitivoId;

  @ResourceReference(
      resourceKey = "administracao-pessoal.periodos-aquisitivos-ferias",
      valueField = "periodoAquisitivoId",
      surfaceId = "ficha-periodo-aquisitivo",
      previewSurfaceId = "ficha-periodo-aquisitivo",
      detailPresentation = "drawer")
  @Schema(
      description =
          "Síntese de identificação do período aquisitivo de férias vinculado à parcela de gozo.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Resumo do período aquisitivo",
      helpText = "Identificação sintética do ciclo aquisitivo debitado pela parcela.",
      group = "Vínculo e Período Aquisitivo",
      order = 43,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "date_range",
      readOnly = true,
      tableHidden = true,
      formHidden = true)
  private String periodoAquisitivoResumo;

  @ResourceReference(
      resourceKey = "administracao-pessoal.periodos-aquisitivos-ferias",
      valueField = "periodoAquisitivoId",
      surfaceId = "ficha-periodo-aquisitivo",
      previewSurfaceId = "ficha-periodo-aquisitivo",
      detailPresentation = "drawer")
  @Schema(
      description = "Ano de exercício do período aquisitivo vinculado à parcela.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Exercício do PA",
      helpText = "Ano de exercício do período aquisitivo debitado.",
      group = "Vínculo e Período Aquisitivo",
      order = 45,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "calendar_month",
      readOnly = true,
      tableHidden = true,
      formHidden = true)
  private Integer periodoAquisitivoExercicio;

  @Schema(
      description =
          "Chave sequencial de negócio do período aquisitivo no vínculo (CHAVEPERAQFER).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Chave do PA",
      helpText = "Sequencial de negócio do período aquisitivo no vínculo.",
      group = "Vínculo e Período Aquisitivo",
      order = 50,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "tag",
      readOnly = true,
      tableHidden = true,
      quickViewVisible = true,
      quickViewOrder = 2)
  private Integer chavePeraqfer;

  @Schema(
      description =
          "Ordem sequencial da parcela de gozo dentro do período aquisitivo (1ª, 2ª, 3ª parcela).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Ordem da parcela",
      helpText = "Número sequencial da etapa de gozo dentro do PA.",
      group = "Período de Gozo e Duração",
      order = 60,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "splitscreen",
      readOnly = true,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 3)
  private Integer ordemParcela;

  @Schema(
      description = "Data de início da fruição da parcela de férias.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Início do gozo",
      helpText = "Data inicial de afastamento em gozo de férias.",
      group = "Período de Gozo e Duração",
      order = 70,
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "event",
      readOnly = true,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 4)
  private LocalDate dtini;

  @Schema(
      description = "Data de término previsto da parcela de férias programada.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Término previsto",
      helpText = "Data final programada para encerramento da parcela.",
      group = "Período de Gozo e Duração",
      order = 80,
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "event_available",
      readOnly = true,
      tableHidden = false)
  private LocalDate dtprevfim;

  @Schema(
      description =
          "Data de término efetivo do gozo da parcela (ajustada quando há interrupção ou conclusão).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Término efetivo",
      helpText = "Data final efetiva do gozo das férias.",
      group = "Período de Gozo e Duração",
      order = 90,
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "event_busy",
      readOnly = true,
      tableHidden = true)
  private LocalDate dtfim;

  @MicroVisualization(
      kind = MicroVisualizationKind.BULLET,
      max = 30,
      total = 30,
      fallbackText = "Sem dias",
      ariaLabel = "Quantidade de dias de gozo na parcela de férias",
      thresholds = {
        @Threshold(min = 0, max = 10, tone = "warning", label = "Curta"),
        @Threshold(min = 10, max = 20, tone = "info", label = "Fracionada"),
        @Threshold(min = 20, tone = "positive", label = "Integral")
      })
  @Schema(
      description = "Quantidade de dias corridos de gozo nesta parcela de férias.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Dias de gozo",
      helpText = "Quantidade de dias de descanso nesta parcela.",
      group = "Período de Gozo e Duração",
      order = 100,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "beach_access",
      readOnly = true,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 5)
  private Integer diasGozo;

  @Schema(
      description =
          "Espelho do total de dias de direito do período aquisitivo no momento da concessão (TOTDIAS).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Dias de direito no PA",
      helpText = "Total de dias de direito do período aquisitivo vinculado.",
      group = "Período de Gozo e Duração",
      order = 110,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "calendar_month",
      readOnly = true,
      tableHidden = true)
  private Integer totDiasDireitoPa;

  @Schema(
      description =
          "Indica se há conversão parcial de férias em abono pecuniário vinculada a esta parcela.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Com abono pecuniário",
      helpText = "Indica venda de dias em abono pecuniário nesta parcela.",
      group = "Abono Pecuniário e Financeiro",
      order = 120,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "sell",
      readOnly = true,
      tableHidden = true)
  private Boolean abonoPecuniario;

  @Schema(
      description =
          "Posição cronológica do abono pecuniário em relação ao gozo: I (no início das férias) ou F (ao final das férias).",
      allowableValues = {"I", "F"},
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Posição do abono (I/F)",
      helpText = "Indica I (início das férias) ou F (final das férias).",
      group = "Abono Pecuniário e Financeiro",
      order = 130,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      options =
          "[{\"value\":\"I\",\"label\":\"Início das férias\"},{\"value\":\"F\",\"label\":\"Final das férias\"}]",
      icon = "swap_horiz",
      readOnly = true,
      tableHidden = true)
  private String iniFimAbono;

  @Schema(
      description =
          "Quantidade de dias vendidos como abono pecuniário nesta parcela (ERG_ABONOS_FERIAS).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Dias de abono vendidos",
      helpText = "Quantidade de dias convertidos em pecúnia nesta parcela.",
      group = "Abono Pecuniário e Financeiro",
      order = 140,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "payments",
      readOnly = true,
      tableHidden = true,
      quickViewVisible = true,
      quickViewOrder = 6)
  private Integer diasAbonoVendidos;

  @Schema(
      description = "Data de protocolo ou requerimento da venda do abono pecuniário.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Data da venda do abono",
      helpText = "Data da opção formal pela conversão em abono pecuniário.",
      group = "Abono Pecuniário e Financeiro",
      order = 150,
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "event",
      readOnly = true,
      tableHidden = true)
  private LocalDate dtVendaAbono;

  @Schema(
      description = "Data programada ou efetiva de pagamento do abono pecuniário.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Pagamento do abono",
      helpText = "Data de liquidação financeira do abono pecuniário.",
      group = "Abono Pecuniário e Financeiro",
      order = 160,
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "paid",
      readOnly = true,
      tableHidden = true)
  private LocalDate dtPagtoAbono;

  @Schema(
      description =
          "Indica se esta parcela comanda o pagamento do terço constitucional de férias na folha.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Paga terço constitucional",
      helpText = "Comanda o pagamento do adicional de 1/3 nesta parcela.",
      group = "Abono Pecuniário e Financeiro",
      order = 170,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "account_balance",
      readOnly = true,
      tableHidden = true)
  private Boolean abonoConstitucional;

  @Schema(
      description =
          "Indica se o servidor optou pelo adiantamento de décimo terceiro salário nesta parcela.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Adiantamento de 13º",
      helpText = "Comanda antecipação de 13º salário junto às férias.",
      group = "Abono Pecuniário e Financeiro",
      order = 180,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "payments",
      readOnly = true,
      tableHidden = true)
  private Boolean adiantamento13;

  @Schema(
      description =
          "Indica se o servidor optou pelo adiantamento salarial de férias nesta parcela.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Adiantamento salarial",
      helpText = "Comanda antecipação salarial de férias na folha.",
      group = "Abono Pecuniário e Financeiro",
      order = 190,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "payments",
      readOnly = true,
      tableHidden = true)
  private Boolean adiantamentoSalario;

  @Schema(
      description =
          "Indica se o servidor contratou empréstimo financeiro restituível vinculado a esta parcela.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Empréstimo de férias",
      helpText = "Comanda rubrica de empréstimo de férias na folha.",
      group = "Abono Pecuniário e Financeiro",
      order = 200,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "account_balance",
      readOnly = true,
      tableHidden = true)
  private Boolean emprestimoFerias;

  @Schema(
      description = "Competência (mês/ano) da folha de pagamento de processamento das férias.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Competência da folha",
      helpText = "Data de competência mensal da folha de pagamento.",
      group = "Folha, Substituição e Interrupção",
      order = 210,
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "receipt_long",
      readOnly = true,
      tableHidden = true)
  private LocalDate mesAnoFolha;

  @Schema(
      description = "Número sequencial da folha de pagamento na competência de processamento.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Número da folha",
      helpText = "Sequencial da folha de pagamento na competência.",
      group = "Folha, Substituição e Interrupção",
      order = 220,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "tag",
      readOnly = true,
      tableHidden = true)
  private Integer numFolha;

  @Schema(
      description =
          "Situação operacional da parcela de gozo (PROGRAMADA, HOMOLOGADA, EM_GOZO, GOZADA, INTERROMPIDA, CANCELADA).",
      allowableValues = {
        "PROGRAMADA",
        "HOMOLOGADA",
        "EM_GOZO",
        "GOZADA",
        "INTERROMPIDA",
        "CANCELADA"
      },
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Situação da parcela",
      helpText = "Estado operacional da parcela de férias.",
      group = "Folha, Substituição e Interrupção",
      order = 230,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      options =
          "[{\"value\":\"PROGRAMADA\",\"label\":\"Programada\"},{\"value\":\"HOMOLOGADA\",\"label\":\"Homologada\"},{\"value\":\"EM_GOZO\",\"label\":\"Em gozo\"},{\"value\":\"GOZADA\",\"label\":\"Gozada\"},{\"value\":\"INTERROMPIDA\",\"label\":\"Interrompida\"},{\"value\":\"CANCELADA\",\"label\":\"Cancelada\"}]",
      icon = "flag",
      readOnly = true,
      tableHidden = false)
  private String situacao;

  @Schema(
      description =
          "Data a partir da qual o gozo de férias foi interrompido por necessidade do serviço.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Data de interrupção",
      helpText = "Data de retorno antecipado por interrupção das férias.",
      group = "Folha, Substituição e Interrupção",
      order = 240,
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "warning",
      readOnly = true,
      tableHidden = true)
  private LocalDate dtInterrupcao;

  @Schema(
      description = "Motivo administrativo ou fundamentação da interrupção do gozo de férias.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Motivo da interrupção",
      helpText = "Justificativa formal para a interrupção das férias.",
      group = "Folha, Substituição e Interrupção",
      order = 250,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.TEXTAREA,
      icon = "report",
      readOnly = true,
      tableHidden = true)
  private String motivoInterrupcao;

  @ResourceReference(
      resourceKey = "administracao-pessoal.funcionarios",
      valueField = "numfuncSubstituto",
      surfaceId = "ficha-cadastral",
      previewSurfaceId = "ficha-cadastral",
      detailPresentation = "drawer")
  @Schema(
      description =
          "Número funcional do servidor designado como substituto durante o gozo das férias.",
      accessMode = Schema.AccessMode.READ_ONLY)
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
      displayField = "label",
      readOnly = true,
      tableHidden = true)
  private Long numfuncSubstituto;

  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Identificação civil do servidor substituto designado.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @ResourceReference(
      resourceKey = "administracao-pessoal.funcionarios",
      valueField = "numfuncSubstituto",
      surfaceId = "ficha-cadastral",
      previewSurfaceId = "ficha-cadastral",
      detailPresentation = "drawer")
  @Schema(
      description = "Nome civil completo do servidor substituto designado.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Nome do substituto",
      helpText = "Nome civil do servidor substituto.",
      group = "Folha, Substituição e Interrupção",
      order = 265,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "supervisor_account",
      readOnly = true,
      tableHidden = true,
      formHidden = true)
  private String substitutoNome;

  @Schema(
      description = "Número sequencial do vínculo funcional do servidor substituto.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Vínculo do substituto",
      helpText = "Sequencial do vínculo ativo do servidor substituto.",
      group = "Folha, Substituição e Interrupção",
      order = 270,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "badge",
      readOnly = true,
      tableHidden = true)
  private Integer numvincSubstituto;

  @ResourceReference(
      resourceKey = "administracao-pessoal.documentos-legais",
      valueField = "documentoLegalId",
      surfaceId = "ficha-documento",
      previewSurfaceId = "ficha-documento",
      detailPresentation = "drawer")
  @Schema(
      description = "Identificador da portaria ou documento legal de concessão das férias.",
      accessMode = Schema.AccessMode.READ_ONLY)
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
      description = "Número oficial do documento legal de concessão das férias.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Número da portaria",
      helpText = "Identificação oficial da portaria de férias.",
      group = "Governança e Fundamentação",
      order = 285,
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
      description = "Identificador do ato oficial de publicação da escala ou portaria de férias.",
      accessMode = Schema.AccessMode.READ_ONLY)
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
      description = "Número oficial do ato de publicação da escala ou portaria de férias.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Número da publicação",
      helpText = "Identificação oficial da publicação das férias.",
      group = "Governança e Fundamentação",
      order = 295,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "gavel",
      readOnly = true,
      tableHidden = true,
      formHidden = true)
  private String atoPublicacaoNumero;

  @Schema(
      description = "Observações operacionais e despachos referentes à parcela de gozo de férias.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Observações da parcela",
      helpText = "Anotações administrativas sobre o gozo das férias.",
      group = "Governança e Fundamentação",
      order = 300,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.TEXTAREA,
      icon = "notes",
      readOnly = true,
      tableHidden = true)
  private String observacoes;

  @Schema(
      description =
          "Fato derivado que indica se a parcela de férias possui venda efetiva de dias em abono pecuniário.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Fato abono pecuniário",
      helpText = "Verdadeiro quando há venda de dias em abono pecuniário nesta parcela.",
      group = "Abono Pecuniário e Financeiro",
      order = 145,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "sell",
      readOnly = true,
      tableHidden = true)
  private Boolean comAbonoVendido;

  @Schema(
      description =
          "Fato derivado que indica se há servidor substituto formalmente designado para cobrir o período da parcela.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Com substituto designado",
      helpText = "Verdadeiro quando há servidor substituto vinculado à parcela.",
      group = "Folha, Substituição e Interrupção",
      order = 275,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "how_to_reg",
      readOnly = true,
      tableHidden = true)
  private Boolean comSubstitutoDesignado;

  @Schema(
      description =
          "Fato derivado que indica se o gozo desta parcela de férias sofreu interrupção administrativa formal.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Parcela interrompida",
      helpText = "Verdadeiro quando a parcela foi interrompida por necessidade do serviço.",
      group = "Folha, Substituição e Interrupção",
      order = 255,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "warning",
      readOnly = true,
      tableHidden = true)
  private Boolean parcelaInterrompida;

  @Schema(
      description =
          "Atributos adicionais dinâmicos da parcela de férias governados pelo catálogo de campos customizados.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @CustomFields(resourceKey = "administracao-pessoal.ferias-gozo")
  @UISchema(
      label = "Campos customizados",
      helpText = "Atributos específicos configurados pelo órgão para gozo de férias.",
      group = "Campos Customizados",
      order = 310,
      readOnly = true,
      tableHidden = true)
  private Map<String, Object> customFields = new LinkedHashMap<>();
}
