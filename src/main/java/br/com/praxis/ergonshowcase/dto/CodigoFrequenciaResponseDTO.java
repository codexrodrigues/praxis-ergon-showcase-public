package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.domain.CodigoFrequenciaId;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.*;
import org.praxisplatform.uischema.*;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.extension.annotation.UISchemaPreset;

@Getter
@Setter
@Schema(
    description =
        "Catálogo detalhado de códigos de ocorrência de frequência, faltas, abonos e modalidades de licença/afastamento com regras operacionais e limites de lançamento (equivalente a CODIGOS_FREQ_ na tela ERGadm00034 do legado Ergon).")
@ResourceIdentity(
    keyField = "id",
    titleField = "nome",
    displayLabelField = "nome",
    subtitleField = "tipoFrequenciaNome",
    metadataFields = {"tipofreq", "codigo", "mnemonico", "afastamento"})
public class CodigoFrequenciaResponseDTO {
  @Schema(
      description =
          "Identidade composta natural formada pelo tipo macro de frequência e pelo código numérico da ocorrência (ex.: LICENCA:201).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Identificador composto",
      helpText = "Chave composta natural do código de frequência.",
      preset = UISchemaPreset.ENTERPRISE_ID,
      group = "Identificação",
      order = 5,
      readOnly = true,
      tableHidden = true,
      formHidden = true)
  private CodigoFrequenciaId id;

  @Schema(
      description =
          "Tipo macro de frequência ao qual o código de ocorrência pertence (componente 1 da chave composta natural).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Tipo de frequência",
      helpText = "Natureza macro à qual este código de ocorrência está vinculado.",
      group = "Identificação",
      order = 10,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "category",
      endpoint = ApiPaths.TIPO_FREQUENCIA_OPTIONS,
      valueField = "id",
      displayField = "label",
      readOnly = true,
      tableHidden = false)
  private String tipofreq;

  @Schema(
      description =
          "Denominação oficial do tipo macro de frequência ao qual o código pertence.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Natureza da frequência",
      helpText = "Descrição da natureza macro do código de frequência.",
      group = "Identificação",
      order = 15,
      readOnly = true,
      tableHidden = true,
      formHidden = true)
  @ResourceReference(
      resourceKey = "administracao-pessoal.tipos-frequencia",
      valueField = "tipofreq",
      surfaceId = "ficha-tipo-frequencia",
      previewSurfaceId = "ficha-tipo-frequencia",
      detailPresentation = "drawer")
  private String tipoFrequenciaNome;

  @Schema(
      description =
          "Código numérico identificador da ocorrência de frequência ou modalidade de afastamento (componente 2 da chave composta natural).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Código numérico",
      helpText = "Número identificador da modalidade dentro do tipo de frequência.",
      group = "Identificação",
      order = 20,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.INPUT,
      icon = "tag",
      readOnly = true,
      tableHidden = false)
  private Integer codigo;

  @Schema(
      description =
          "Descrição completa da ocorrência de frequência, falta, abono ou modalidade de licença/afastamento.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Descrição da ocorrência",
      helpText = "Denominação oficial do código de frequência ou licença.",
      group = "Identificação",
      order = 30,
      readOnly = true,
      tableHidden = false)
  @ResourceReference(
      resourceKey = "administracao-pessoal.codigos-frequencia",
      valueField = "id",
      surfaceId = "ficha-codigo-frequencia",
      previewSurfaceId = "ficha-codigo-frequencia",
      detailPresentation = "drawer")
  private String nome;

  @Schema(
      description =
          "Sigla mnemônica curta utilizada em espelhos de ponto e mapas de frequência (ex.: FJ, FI, LTS, LMA).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Sigla mnemônica",
      helpText = "Sigla curta de até 6 caracteres para espelhos de frequência.",
      group = "Identificação",
      order = 40,
      readOnly = true,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 1)
  private String mnemonico;

  @Schema(
      description =
          "Nome abreviado para exibição compacta em colunas de grade e relatórios operacionais.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Nome abreviado",
      helpText = "Denominação reduzida de até 20 caracteres para relatórios compactos.",
      group = "Identificação",
      order = 50,
      readOnly = true,
      tableHidden = true)
  private String nomeAbrev;

  @Schema(
      description =
          "Indica se o código caracteriza um afastamento formal ou licença que suspende ou interrompe o exercício regular (LIC_AFAST).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Habilita afastamento",
      helpText = "Define se o código pode ser utilizado em concessões de licença e afastamento.",
      group = "Regras de lançamento",
      order = 60,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "event_busy",
      labelTrue = "Licença / afastamento",
      labelFalse = "Ocorrência de ponto",
      toneTrue = "info",
      toneFalse = "neutral",
      readOnly = true,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 2)
  private Boolean afastamento;

  @Schema(
      description =
          "Indica se o código pode ser lançado diretamente na apuração operacional diária/mensal de frequência.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Lançável via frequência",
      helpText = "Define se permite lançamento direto no boletim de frequência.",
      group = "Regras de lançamento",
      order = 70,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "edit_calendar",
      labelTrue = "Via frequência",
      labelFalse = "Ato formal",
      toneTrue = "success",
      toneFalse = "neutral",
      readOnly = true,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 3)
  private Boolean viaFrequencia;

  @Schema(
      description =
          "Indica se a ocorrência ou licença implica ônus financeiro ou desconto remuneratório para o servidor/órgão.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Com ônus ou desconto",
      helpText = "Define se há repercussão financeira ou ônus na ocorrência.",
      group = "Regras de lançamento",
      order = 80,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "payments",
      labelTrue = "Com ônus",
      labelFalse = "Sem ônus",
      toneTrue = "warning",
      toneFalse = "neutral",
      readOnly = true,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 4)
  private Boolean onus;

  @Schema(
      description =
          "Indica se o código está sujeito à validação parametrizada de limites estatutários em regras_frequencia_estatutaria.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Sujeito a teto estatutário",
      helpText = "Define se aplica limites máximos por interstício estatutário.",
      group = "Regras de lançamento",
      order = 90,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "rule",
      labelTrue = "Com regra",
      labelFalse = "Livre",
      toneTrue = "info",
      toneFalse = "neutral",
      readOnly = true,
      tableHidden = true)
  private Boolean regrasFreq;

  @Schema(
      description =
          "Indica se o lançamento exige preenchimento explícito dos horários de entrada e saída.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Exige horário início/fim",
      helpText = "Define se o registro requer hora de entrada e saída.",
      group = "Regras de lançamento",
      order = 100,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "schedule",
      labelTrue = "Exige horário",
      labelFalse = "Dispensa horário",
      toneTrue = "info",
      toneFalse = "neutral",
      readOnly = true,
      tableHidden = true)
  private Boolean horaEntradaSaida;

  @Schema(
      description =
          "Restrição de aplicabilidade por sexo do servidor: [A]mbos, [F]eminino (ex.: licença-maternidade) ou [M]asculino (ex.: licença-paternidade).",
      allowableValues = {"A", "F", "M"},
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Aplicabilidade por sexo",
      helpText = "Restrição de elegibilidade por sexo ([A]mbos, [F]eminino ou [M]asculino).",
      group = "Parâmetros de quantidade",
      order = 110,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      options =
          "[{\"value\":\"A\",\"label\":\"Ambos (A)\"},{\"value\":\"F\",\"label\":\"Feminino (F)\"},{\"value\":\"M\",\"label\":\"Masculino (M)\"}]",
      icon = "wc",
      readOnly = true,
      tableHidden = true)
  private String sexo;

  @Schema(
      description =
          "Modo de informe da ocorrência no lançamento (ex.: PERIODO, QUANTIDADE, HORARIO).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Modo de preenchimento",
      helpText = "Forma operacional de apuração (período, quantidade ou horário).",
      group = "Parâmetros de quantidade",
      order = 120,
      readOnly = true,
      tableHidden = true)
  private String tipoPreenchimento;

  @Schema(
      description =
          "Unidade de medida da quantidade informada (ex.: DIAS, HORAS, OCORRENCIAS).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Unidade de medida",
      helpText = "Grandeza utilizada na contabilização da ocorrência.",
      group = "Parâmetros de quantidade",
      order = 130,
      readOnly = true,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 5)
  private String formatoQtd;

  @Schema(
      description =
          "Fator de equivalência em faltas para fins de desconto remuneratório ou perda de avos de férias e 13º salário.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Equivalência em faltas",
      helpText = "Peso da ocorrência na apuração de faltas para férias e 13º.",
      group = "Parâmetros de quantidade",
      order = 140,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "calculate",
      readOnly = true,
      tableHidden = true)
  private BigDecimal qtdFaltas;

  @Schema(
      description =
          "Quantidade mínima permitida por lançamento individual deste código de frequência.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Quantidade mínima",
      helpText = "Piso mínimo admitido em cada lançamento da ocorrência.",
      group = "Parâmetros de quantidade",
      order = 150,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "vertical_align_bottom",
      readOnly = true,
      tableHidden = true)
  private BigDecimal qtdmin;

  @Schema(
      description =
          "Quantidade máxima permitida por lançamento individual deste código de frequência.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Quantidade máxima",
      helpText = "Teto máximo admitido em cada lançamento da ocorrência.",
      group = "Parâmetros de quantidade",
      order = 160,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "vertical_align_top",
      readOnly = true,
      tableHidden = true)
  private BigDecimal qtdmax;

  @Schema(
      description =
          "Referência ao artigo ou diploma legal que fundamenta o código de frequência ou licença (substitui PONTLEI).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Fundamento legal",
      helpText = "Diploma legal ou artigo estatutário que ampara a ocorrência.",
      group = "Normativo",
      order = 170,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "description",
      endpoint = ApiPaths.DOCUMENTO_OPTIONS,
      valueField = "id",
      displayField = "label",
      readOnly = true,
      tableHidden = true,
      quickViewVisible = true,
      quickViewOrder = 6)
  @ResourceReference(
      resourceKey = "administracao-pessoal.documentos-legais",
      valueField = "documentoLegalId",
      surfaceId = "ficha-documento",
      previewSurfaceId = "ficha-documento",
      detailPresentation = "drawer")
  private Long documentoLegalId;

  @Schema(
      description =
          "Número oficial do ato ou diploma legal associado ao código de frequência.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Número do ato legal",
      helpText = "Identificação oficial do diploma legal vinculado.",
      group = "Normativo",
      order = 175,
      readOnly = true,
      tableHidden = true)
  @ResourceReference(
      resourceKey = "administracao-pessoal.documentos-legais",
      valueField = "documentoLegalId",
      surfaceId = "ficha-documento",
      previewSurfaceId = "ficha-documento",
      detailPresentation = "drawer")
  private String documentoLegalNumero;
}
