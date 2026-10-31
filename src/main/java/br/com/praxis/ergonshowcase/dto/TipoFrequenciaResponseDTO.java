package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.*;
import org.praxisplatform.uischema.*;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.extension.annotation.UISchema;

@Getter
@Setter
@Schema(
    description =
        "Catálogo de naturezas macro de ocorrências de frequência e afastamento que agrupa os códigos de frequência e define reflexos em contagem de tempo e sobrejornada (equivalente a TIPO_FREQ_ na tela ERGadm00033 do legado Ergon).")
@ResourceIdentity(
    keyField = "id",
    titleField = "nome",
    displayLabelField = "nome",
    subtitleField = "id",
    metadataFields = {"influiConta", "horaExtra"})
public class TipoFrequenciaResponseDTO {
  @Schema(
      description =
          "Identificador único do tipo macro de frequência no catálogo normativo (ex.: NORMAL, FALTA, LICENCA, AFASTAMENTO, HORA_EXTRA).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Código do tipo",
      helpText = "Identificador alfanumérico estável da natureza de frequência.",
      group = "Identificação",
      order = 10,
      readOnly = true,
      tableHidden = false)
  private String id;

  @Schema(
      description =
          "Sigla ou código natural do tipo macro de frequência (TIPO_FREQ_.TIPO).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Sigla do tipo",
      helpText = "Chave natural do tipo macro de frequência.",
      group = "Identificação",
      order = 15,
      readOnly = true,
      tableHidden = true,
      formHidden = true)
  private String tipo;

  @Schema(
      description =
          "Denominação descritiva oficial do tipo de frequência apresentada em consultas, espelhos de ponto e relatórios.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Denominação do tipo",
      helpText = "Nome oficial da natureza de frequência ou afastamento.",
      group = "Identificação",
      order = 20,
      readOnly = true,
      tableHidden = false)
  @ResourceReference(
      resourceKey = "administracao-pessoal.tipos-frequencia",
      valueField = "id",
      surfaceId = "ficha-tipo-frequencia",
      previewSurfaceId = "ficha-tipo-frequencia",
      detailPresentation = "drawer")
  private String nome;

  @Schema(
      description =
          "Indica se ocorrências vinculadas a este tipo impactam a apuração de tempo de serviço, aposentadoria ou adicionais temporais (INFLUI_CONTA).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Influi na contagem",
      helpText = "Define se ocorrências desta natureza repercutem na contagem de tempo.",
      group = "Regras e reflexos",
      order = 30,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "schedule",
      labelTrue = "Impacta contagem",
      labelFalse = "Neutro",
      toneTrue = "warning",
      toneFalse = "neutral",
      readOnly = true,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 1)
  private Boolean influiConta;

  @Schema(
      description =
          "Indica se ocorrências vinculadas a este tipo caracterizam prestação de serviço extraordinário (HORA_EXTRA).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Caracteriza hora extra",
      helpText = "Define se o tipo de frequência representa sobrejornada remunerada ou compensável.",
      group = "Regras e reflexos",
      order = 40,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "more_time",
      labelTrue = "Hora extra",
      labelFalse = "Jornada regular",
      toneTrue = "info",
      toneFalse = "neutral",
      readOnly = true,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 2)
  private Boolean horaExtra;

  @Schema(
      description =
          "Percentual padrão de acréscimo remuneratório incidente sobre a hora normal quando o tipo caracteriza serviço extraordinário.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Adicional de hora extra",
      helpText = "Percentual de acréscimo remuneratório aplicável à sobrejornada.",
      group = "Regras e reflexos",
      order = 50,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "percent",
      readOnly = true,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 3)
  private BigDecimal percentualExtra;

  @Schema(
      description =
          "Referência ao diploma legal ou ato normativo que regulamenta o tipo de frequência (substitui PONTLEI).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Fundamento legal",
      helpText = "Dispositivo normativo que disciplina esta natureza de frequência.",
      group = "Normativo",
      order = 60,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "description",
      endpoint = ApiPaths.DOCUMENTO_OPTIONS,
      valueField = "id",
      displayField = "label",
      readOnly = true,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 4)
  @ResourceReference(
      resourceKey = "administracao-pessoal.documentos-legais",
      valueField = "documentoLegalId",
      surfaceId = "ficha-documento",
      previewSurfaceId = "ficha-documento",
      detailPresentation = "drawer")
  private Long documentoLegalId;

  @Schema(
      description =
          "Número oficial do ato ou diploma legal associado à regulamentação do tipo de frequência.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Número do ato legal",
      helpText = "Identificação oficial do ato normativo vinculado.",
      group = "Normativo",
      order = 65,
      readOnly = true,
      tableHidden = false)
  @ResourceReference(
      resourceKey = "administracao-pessoal.documentos-legais",
      valueField = "documentoLegalId",
      surfaceId = "ficha-documento",
      previewSurfaceId = "ficha-documento",
      detailPresentation = "drawer")
  private String documentoLegalNumero;
}
