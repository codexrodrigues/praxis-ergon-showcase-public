package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.praxisplatform.uischema.*;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.bulk.BulkMode;
import org.praxisplatform.uischema.customfields.CustomFields;
import org.praxisplatform.uischema.extension.annotation.UISchema;

@Getter
@Setter
@Schema(
    description =
        "Atributos mutáveis da ocorrência operacional de frequência, falta, abono, atraso ou hora extraordinária.")
public class FrequenciaDados {

  @NotBlank
  @Size(max = 20)
  @ResourceReference(
      resourceKey = "administracao-pessoal.tipos-frequencia",
      valueField = "tipofreq",
      surfaceId = "ficha-tipo-frequencia",
      previewSurfaceId = "ficha-tipo-frequencia",
      detailPresentation = "drawer")
  @Schema(description = "Tipo macro da ocorrência de frequência no catálogo funcional.")
  @UISchema(
      label = "Tipo de frequência",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "category",
      group = "Tipificação e Período",
      order = 40,
      required = true,
      endpoint = ApiPaths.TIPO_FREQUENCIA_OPTIONS,
      valueField = "id",
      displayField = "label",
      helpText = "Selecione o tipo macro da ocorrência de frequência.")
  private String tipofreq;

  @NotNull
  @Schema(description = "Código específico da ocorrência de frequência ou falta.")
  @UISchema(
      label = "Código de frequência",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "pin",
      group = "Tipificação e Período",
      order = 50,
      required = true,
      endpoint = ApiPaths.CODIGO_FREQUENCIA_OPTIONS,
      valueField = "id",
      displayField = "label",
      helpText = "Selecione o código detalhado da ocorrência de frequência.")
  private Integer codfreq;

  @NotNull
  @Schema(description = "Data de início da ocorrência de frequência ou falta.")
  @UISchema(
      label = "Data de início",
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "event",
      group = "Tipificação e Período",
      order = 60,
      required = true,
      helpText = "Data inicial da ocorrência operacional de frequência.")
  private LocalDate dtini;

  @BulkEditable(
      modes = {BulkMode.UNIFORM_UPDATE, BulkMode.PER_ITEM_UPDATE},
      allowClear = true)
  @Schema(
      description =
          "Data final da ocorrência de frequência para eventos contínuos ou igual à data inicial.",
      nullable = true)
  @UISchema(
      label = "Data final",
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "event_available",
      group = "Tipificação e Período",
      order = 70,
      helpText = "Data final da ocorrência ou vazio para evento pontual no mesmo dia.")
  private LocalDate dtfim;

  @BulkEditable(
      modes = {BulkMode.UNIFORM_UPDATE, BulkMode.PER_ITEM_UPDATE},
      allowClear = true)
  @Schema(
      description =
          "Quantidade apurada na ocorrência em dias, horas ou minutos conforme o formato do código.",
      nullable = true)
  @UISchema(
      label = "Quantidade apurada",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "calculate",
      group = "Apuração e Horários",
      order = 80,
      helpText = "Quantidade de dias, horas ou minutos registrada na ocorrência.")
  private BigDecimal quantidade;

  @BulkEditable(
      modes = {BulkMode.UNIFORM_UPDATE, BulkMode.PER_ITEM_UPDATE},
      allowClear = true)
  @Schema(
      description =
          "Horário de entrada registrado na ocorrência quando o código exige controle horário.",
      nullable = true)
  @UISchema(
      label = "Hora de entrada",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.TIME_PICKER,
      icon = "login",
      group = "Apuração e Horários",
      order = 90,
      helpText = "Horário de início do expediente ou ocorrência horária.")
  private LocalTime horaEntrada;

  @BulkEditable(
      modes = {BulkMode.UNIFORM_UPDATE, BulkMode.PER_ITEM_UPDATE},
      allowClear = true)
  @Schema(
      description =
          "Horário de saída registrado na ocorrência quando o código exige controle horário.",
      nullable = true)
  @UISchema(
      label = "Hora de saída",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.TIME_PICKER,
      icon = "logout",
      group = "Apuração e Horários",
      order = 100,
      helpText = "Horário de encerramento do expediente ou ocorrência horária.")
  private LocalTime horaSaida;

  @BulkEditable(
      modes = {BulkMode.UNIFORM_UPDATE, BulkMode.PER_ITEM_UPDATE},
      allowClear = true)
  @Size(max = 2000)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Justificativa de frequência pode conter informação pessoal ou médica do servidor.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @Schema(
      description =
          "Justificativa, motivo de abono ou observação operacional registrada para a ocorrência.",
      nullable = true)
  @UISchema(
      label = "Justificativa e observações",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.TEXTAREA,
      icon = "notes",
      group = "Justificativa e Amparo Legal",
      order = 110,
      helpText = "Descreva a justificativa operacional, motivo de abono ou observação.")
  private String obs;

  @BulkEditable(
      modes = {BulkMode.UNIFORM_UPDATE, BulkMode.PER_ITEM_UPDATE},
      allowClear = true)
  @ResourceReference(
      resourceKey = "administracao-pessoal.documentos-legais",
      valueField = "documentoLegalId",
      surfaceId = "ficha-documento",
      previewSurfaceId = "ficha-documento",
      detailPresentation = "drawer")
  @Schema(
      description =
          "Identificador do documento comprobatório, portaria de abono ou processo anexo.",
      nullable = true)
  @UISchema(
      label = "Documento comprobatório",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "MenuBook",
      group = "Justificativa e Amparo Legal",
      order = 120,
      endpoint = ApiPaths.DOCUMENTO_OPTIONS,
      valueField = "id",
      displayField = "label",
      helpText = "Ato legal, atestado ou processo administrativo que fundamenta o registro.")
  private Long documentoLegalId;

  @Schema(
      description =
          "Denominação prevista da modalidade de frequência calculada reativamente em tempo de formulário.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Modalidade prevista",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "auto_awesome",
      group = "Prévia Reativa",
      order = 200,
      readOnly = true,
      tableHidden = true,
      helpText = "Nome oficial da modalidade resolvido automaticamente ao selecionar o código.")
  private String codigoFrequenciaNomePrevisto;

  @Schema(
      description =
          "Unidade de medida prevista do código selecionado calculada reativamente no formulário.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Unidade de apuração prevista",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "straighten",
      group = "Prévia Reativa",
      order = 210,
      readOnly = true,
      tableHidden = true,
      helpText = "Unidade de medida do código, como DIAS, HORAS ou MINUTOS.")
  private String formatoQuantidadePrevisto;

  @Schema(
      description =
          "Indica reativamente se o código selecionado exige preenchimento de hora de entrada e saída.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Exige horário previsto",
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "schedule",
      group = "Prévia Reativa",
      order = 220,
      readOnly = true,
      tableHidden = true,
      labelTrue = "Exige horário",
      labelFalse = "Dispensa horário",
      toneTrue = "warning",
      toneFalse = "neutral",
      helpText = "Sinaliza se os horários de entrada e saída são obrigatórios para o código.")
  private Boolean exigeHorarioPrevisto;

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
}
