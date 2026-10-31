package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.domain.CodigoFrequenciaId;
import br.com.praxis.ergonshowcase.domain.VinculoId;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
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
    titleField = "obs",
    displayLabelField = "obs",
    subtitleField = "pessoaNome",
    metadataFields = {"vinculoMatricula", "codigoFrequenciaNome", "situacao", "dtini"})
@Schema(
    description =
        "Projeção completa da ocorrência operacional de frequência, falta, abono, atraso ou hora extraordinária do vínculo funcional.")
public class FrequenciaResponseDTO {

  @Schema(
      description = "Chave primária surrogate imutável da ocorrência de frequência.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "ID da frequência",
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
      helpText = "Identificador técnico imutável da ocorrência.")
  private Long id;

  @ResourceReference(
      resourceKey = "administracao-pessoal.empresas",
      valueField = "empCodigo",
      surfaceId = "ficha-institucional",
      previewSurfaceId = "ficha-institucional",
      detailPresentation = "drawer")
  @Schema(
      description = "Código do órgão público de lotação do vínculo no momento da ocorrência.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Órgão",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "apartment",
      group = "Vínculo Funcional",
      order = 10,
      readOnly = true,
      tableHidden = true,
      filterable = true,
      sortable = true,
      endpoint = ApiPaths.ORGAO_OPTIONS,
      valueField = "id",
      displayField = "label",
      helpText = "Órgão público de lotação do vínculo.")
  private Integer empCodigo;

  @ResourceReference(
      resourceKey = "administracao-pessoal.empresas",
      valueField = "empCodigo",
      surfaceId = "ficha-institucional",
      previewSurfaceId = "ficha-institucional",
      detailPresentation = "drawer")
  @Schema(
      description = "Nome do órgão público de lotação do vínculo no momento da ocorrência.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Nome do órgão",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "domain",
      group = "Vínculo Funcional",
      order = 15,
      readOnly = true,
      tableHidden = true,
      quickViewVisible = true,
      quickViewOrder = 10,
      sortable = true,
      helpText = "Denominação oficial do órgão de lotação.")
  private String orgaoNome;

  @ResourceReference(
      resourceKey = "administracao-pessoal.funcionarios",
      valueField = "numfunc",
      surfaceId = "ficha-cadastral",
      previewSurfaceId = "ficha-cadastral",
      detailPresentation = "drawer")
  @Schema(
      description = "Número funcional do servidor titular da ocorrência de frequência.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Servidor",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "person",
      group = "Vínculo Funcional",
      order = 20,
      readOnly = true,
      tableHidden = true,
      filterable = true,
      sortable = true,
      endpoint = ApiPaths.PESSOA_OPTIONS,
      valueField = "id",
      displayField = "label",
      helpText = "Identificador funcional do servidor.")
  private Long numfunc;

  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Identificação civil do servidor associado à ocorrência de frequência.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @ResourceReference(
      resourceKey = "administracao-pessoal.funcionarios",
      valueField = "numfunc",
      surfaceId = "ficha-cadastral",
      previewSurfaceId = "ficha-cadastral",
      detailPresentation = "drawer")
  @Schema(
      description = "Nome civil completo do servidor titular da ocorrência de frequência.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Nome do servidor",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "badge",
      group = "Vínculo Funcional",
      order = 25,
      readOnly = true,
      tableHidden = false,
      sortable = true,
      helpText = "Nome civil do servidor associado à ocorrência.")
  private String pessoaNome;

  @Schema(
      description = "Número sequencial do vínculo funcional do servidor.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Vínculo funcional",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "work",
      group = "Vínculo Funcional",
      order = 30,
      readOnly = true,
      tableHidden = true,
      filterable = true,
      sortable = true,
      endpoint = ApiPaths.VINCULO_OPTIONS,
      valueField = "id",
      displayField = "label",
      helpText = "Sequencial do vínculo funcional do servidor.")
  private Integer numvinc;

  @Schema(
      description = "Identidade composta do vínculo funcional associado à ocorrência.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Identidade do vínculo",
      group = "Vínculo Funcional",
      order = 32,
      readOnly = true,
      tableHidden = true,
      formHidden = true,
      helpText = "Chave composta do vínculo funcional titular.")
  private VinculoId vinculoId;

  @ResourceReference(
      resourceKey = "administracao-pessoal.vinculos",
      valueField = "vinculoId",
      surfaceId = "resumo-funcional",
      previewSurfaceId = "resumo-funcional",
      detailPresentation = "drawer")
  @Schema(
      description = "Matrícula corporativa do vínculo funcional no momento da ocorrência.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Matrícula do vínculo",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "assignment_ind",
      group = "Vínculo Funcional",
      order = 35,
      readOnly = true,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 20,
      sortable = true,
      helpText = "Matrícula funcional do vínculo associado.")
  private String vinculoMatricula;

  @ResourceReference(
      resourceKey = "administracao-pessoal.tipos-frequencia",
      valueField = "tipofreq",
      surfaceId = "ficha-tipo-frequencia",
      previewSurfaceId = "ficha-tipo-frequencia",
      detailPresentation = "drawer")
  @Schema(
      description = "Tipo macro da ocorrência de frequência.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Tipo de frequência",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "category",
      group = "Tipificação e Período",
      order = 40,
      readOnly = true,
      tableHidden = true,
      filterable = true,
      sortable = true,
      endpoint = ApiPaths.TIPO_FREQUENCIA_OPTIONS,
      valueField = "id",
      displayField = "label",
      helpText = "Agrupador macro da ocorrência de frequência.")
  private String tipofreq;

  @ResourceReference(
      resourceKey = "administracao-pessoal.tipos-frequencia",
      valueField = "tipofreq",
      surfaceId = "ficha-tipo-frequencia",
      previewSurfaceId = "ficha-tipo-frequencia",
      detailPresentation = "drawer")
  @Schema(
      description = "Descrição do tipo macro da ocorrência de frequência.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Descrição do tipo",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "description",
      group = "Tipificação e Período",
      order = 45,
      readOnly = true,
      tableHidden = true,
      sortable = true,
      helpText = "Denominação do tipo macro de frequência.")
  private String tipoFrequenciaDescricao;

  @Schema(
      description = "Código detalhado da ocorrência de frequência ou falta.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Código de frequência",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "pin",
      group = "Tipificação e Período",
      order = 50,
      readOnly = true,
      tableHidden = true,
      filterable = true,
      sortable = true,
      endpoint = ApiPaths.CODIGO_FREQUENCIA_OPTIONS,
      valueField = "id",
      displayField = "label",
      helpText = "Código numérico detalhado no catálogo de frequência.")
  private Integer codfreq;

  @Schema(
      description = "Identidade composta do código de frequência da ocorrência.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Chave do código de frequência",
      group = "Tipificação e Período",
      order = 52,
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
      description = "Denominação oficial do código de frequência registrado na ocorrência.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Modalidade de frequência",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "event_note",
      group = "Tipificação e Período",
      order = 55,
      readOnly = true,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 30,
      sortable = true,
      helpText = "Nome oficial da ocorrência no catálogo de frequência.")
  private String codigoFrequenciaNome;

  @Schema(
      description = "Data de início da ocorrência de frequência ou falta.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Data de início",
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "event",
      group = "Tipificação e Período",
      order = 60,
      readOnly = true,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 40,
      filterable = true,
      sortable = true,
      helpText = "Data inicial da ocorrência operacional.")
  private LocalDate dtini;

  @Schema(
      description =
          "Data final da ocorrência de frequência para período contínuo ou igual à data inicial.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Data final",
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "event_available",
      group = "Tipificação e Período",
      order = 70,
      readOnly = true,
      tableHidden = false,
      filterable = true,
      sortable = true,
      helpText = "Data final da ocorrência de frequência.")
  private LocalDate dtfim;

  @MicroVisualization(
      kind = MicroVisualizationKind.BULLET,
      valueExpr = "quantidade",
      min = 0,
      max = 24,
      total = 24,
      valueSuffix = " u",
      compactValue = true,
      fallbackText = "Sem quantidade apurada",
      ariaLabel = "Quantidade apurada na ocorrência de frequência",
      thresholds = {
        @Threshold(min = 0, max = 2, tone = "success", label = "Curta duração"),
        @Threshold(min = 2, max = 8, tone = "warning", label = "Turno regular"),
        @Threshold(min = 8, tone = "danger", label = "Acima da jornada")
      })
  @Schema(
      description = "Quantidade apurada na ocorrência em dias, horas ou minutos.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Quantidade apurada",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "calculate",
      group = "Apuração e Horários",
      order = 80,
      readOnly = true,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 50,
      filterable = true,
      sortable = true,
      helpText = "Quantidade de dias, horas ou minutos apurada.")
  private BigDecimal quantidade;

  @Schema(
      description = "Horário de entrada registrado na ocorrência.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Hora de entrada",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.TIME_PICKER,
      icon = "login",
      group = "Apuração e Horários",
      order = 90,
      readOnly = true,
      tableHidden = true,
      sortable = true,
      helpText = "Horário inicial registrado na ocorrência.")
  private LocalTime horaEntrada;

  @Schema(
      description = "Horário de saída registrado na ocorrência.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Hora de saída",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.TIME_PICKER,
      icon = "logout",
      group = "Apuração e Horários",
      order = 100,
      readOnly = true,
      tableHidden = true,
      sortable = true,
      helpText = "Horário final registrado na ocorrência.")
  private LocalTime horaSaida;

  @Schema(
      description =
          "Estado do workflow de aprovação da ocorrência: PENDENTE_HOMOLOGACAO, HOMOLOGADO, REJEITADO ou CANCELADO.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Situação da frequência",
      preset = UISchemaPreset.ENTERPRISE_STATUS,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      icon = "verified_user",
      group = "Workflow e Governança",
      order = 110,
      readOnly = true,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 60,
      filterable = true,
      sortable = true,
      options =
          "[{\"id\":\"PENDENTE_HOMOLOGACAO\",\"label\":\"Pendente de Homologação\"},{\"id\":\"HOMOLOGADO\",\"label\":\"Homologado\"},{\"id\":\"REJEITADO\",\"label\":\"Rejeitado\"},{\"id\":\"CANCELADO\",\"label\":\"Cancelado\"}]",
      valueField = "id",
      displayField = "label",
      helpText = "Situação atual do registro no ciclo de homologação.")
  private String situacao;

  @OptionLabel
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Justificativa de frequência pode conter informação pessoal ou médica do servidor.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @ResourceReference(
      resourceKey = "administracao-pessoal.frequencias",
      valueField = "id",
      surfaceId = "ficha-frequencia",
      previewSurfaceId = "ficha-frequencia",
      detailPresentation = "drawer")
  @Schema(
      description =
          "Justificativa, motivo de abono ou observação operacional registrada para a ocorrência.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Justificativa",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.TEXTAREA,
      icon = "notes",
      group = "Justificativa e Amparo Legal",
      order = 120,
      readOnly = true,
      tableHidden = false,
      filterable = true,
      sortable = true,
      helpText = "Justificativa operacional ou motivo de abono da ocorrência.")
  private String obs;

  @ResourceReference(
      resourceKey = "administracao-pessoal.documentos-legais",
      valueField = "documentoLegalId",
      surfaceId = "ficha-documento",
      previewSurfaceId = "ficha-documento",
      detailPresentation = "drawer")
  @Schema(
      description =
          "Identificador do documento comprobatório, portaria de abono ou processo anexo.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Documento comprobatório",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "menu_book",
      group = "Justificativa e Amparo Legal",
      order = 130,
      readOnly = true,
      tableHidden = true,
      filterable = true,
      sortable = true,
      endpoint = ApiPaths.DOCUMENTO_OPTIONS,
      valueField = "id",
      displayField = "label",
      helpText = "Documento legal ou processo anexo à ocorrência.")
  private Long documentoLegalId;

  @ResourceReference(
      resourceKey = "administracao-pessoal.documentos-legais",
      valueField = "documentoLegalId",
      surfaceId = "ficha-documento",
      previewSurfaceId = "ficha-documento",
      detailPresentation = "drawer")
  @Schema(
      description = "Número do documento comprobatório ou portaria de abono vinculada.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Número do documento",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "policy",
      group = "Justificativa e Amparo Legal",
      order = 135,
      readOnly = true,
      tableHidden = true,
      sortable = true,
      helpText = "Identificação do documento legal de suporte.")
  private String documentoLegalNumero;

  @CustomFields(resourceKey = "administracao-pessoal.frequencias")
  @Schema(description = "Atributos complementares de frequência parametrizados por órgão.")
  @UISchema(
      label = "Campos complementares do órgão",
      helpText =
          "Atributos customizados de frequência governados pelo catálogo dinâmico do órgão.",
      group = "Campos customizados",
      order = 250,
      tableHidden = true)
  private Map<String, Object> customFields = new LinkedHashMap<>();

  @Schema(
      description =
          "Fato derivado que indica se a ocorrência aguarda homologação da chefia ou RH.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Pendente de homologação",
      helpText = "Verdadeiro quando a frequência aguarda despacho homologatório.",
      group = "Workflow e Governança",
      order = 140,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "pending_actions",
      readOnly = true,
      tableHidden = true)
  private Boolean pendenteHomologacao;

  @Schema(
      description =
          "Fato derivado que indica se a ocorrência possui horários de entrada e saída apurados.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Ocorrência horária",
      helpText = "Verdadeiro quando há registro de hora de entrada e saída.",
      group = "Apuração e Horários",
      order = 145,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "schedule",
      readOnly = true,
      tableHidden = true)
  private Boolean ocorrenciaHoraria;

  @Schema(
      description =
          "Fato derivado que indica se há documento comprobatório ou portaria vinculada.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Com documento anexo",
      helpText = "Verdadeiro quando há documento comprobatório associado.",
      group = "Justificativa e Amparo Legal",
      order = 150,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "fact_check",
      readOnly = true,
      tableHidden = true)
  private Boolean comDocumentoComprobatorio;
}
