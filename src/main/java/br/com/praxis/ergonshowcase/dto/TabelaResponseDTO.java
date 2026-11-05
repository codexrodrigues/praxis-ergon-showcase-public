package br.com.praxis.ergonshowcase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.*;
import org.praxisplatform.uischema.*;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.extension.annotation.UISchema;

@Getter
@Setter
@Schema(
    description =
        "Catálogo mestre de tabelas gerais de domínio do Ergon/Hades (equivalente a HADES.TABELA) que organiza códigos parametrizáveis como estado civil, escolaridade, regimes jurídicos, categorias, formas de vacância e listas de opções de campos customizados.")
@ResourceIdentity(
    keyField = "id",
    titleField = "descr",
    displayLabelField = "descr",
    subtitleField = "tab",
    metadataFields = {"updatedAt"})
public class TabelaResponseDTO {
  @Schema(
      description =
          "Identificador canônico da tabela geral de domínio correspondente à chave primária (ex.: ERG_ESTCIVIL, ERG_ESCOLA, ERG_REGIME_JUR, Parentesco).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Código da tabela",
      helpText = "Identificador alfanumérico único da tabela geral no catálogo.",
      group = "Identificação",
      order = 10,
      readOnly = true,
      tableHidden = false)
  private String id;

  @Schema(
      description =
          "Chave natural da tabela geral de domínio (HADES.TABELA.TAB) utilizada nas referências de itens e campos customizados.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Sigla da tabela",
      helpText = "Chave natural da tabela geral de domínio.",
      group = "Identificação",
      order = 15,
      readOnly = true,
      tableHidden = true,
      formHidden = true)
  private String tab;

  @Schema(
      description =
          "Descrição funcional da finalidade da tabela geral de domínio apresentada em catálogos e editores de parametrização.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Finalidade da tabela",
      helpText = "Denominação oficial da tabela geral de domínio.",
      group = "Identificação",
      order = 20,
      readOnly = true,
      tableHidden = false)
  @ResourceReference(
      resourceKey = "administracao-pessoal.tabelas",
      valueField = "id",
      surfaceId = "ficha-tabela",
      previewSurfaceId = "ficha-tabela",
      detailPresentation = "drawer")
  private String descr;

  @Schema(
      description =
          "Sistema proprietário da tabela geral no ecossistema corporativo (ex.: Ergon, Hades).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Sistema proprietário", icon = "dns",
      helpText = "Módulo ou subsistema responsável pela governança da tabela geral.",
      group = "Identificação",
      order = 30,
      readOnly = true,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 1)
  private String sis;

  @Schema(
      description =
          "Indica se os itens da tabela geral admitem recorte específico por órgão/empresa (equivalente a MULTI_EMP S/N no legado).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Escopo multi-órgão",
      helpText = "Define se os itens da tabela podem variar por órgão público.",
      group = "Estrutura do item",
      order = 40,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "apartment",
      labelTrue = "Multi-órgão",
      labelFalse = "Global",
      toneTrue = "info",
      toneFalse = "neutral",
      readOnly = true,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 2)
  private Boolean multiEmp;

  @Schema(
      description =
          "Tipo de dado predominante do código do item dentro desta tabela geral ([A]lfanumérico ou [N]umérico).",
      allowableValues = {"A", "N"},
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Natureza do código",
      helpText = "Classificação do código dos itens: Alfanumérico (A) ou Numérico (N).",
      group = "Estrutura do item",
      order = 50,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      options =
          "[{\"value\":\"A\",\"label\":\"Alfanumérico (A)\"},{\"value\":\"N\",\"label\":\"Numérico (N)\"}]",
      icon = "data_object",
      readOnly = true,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 3)
  private String tipo;

  @Schema(
      description =
          "Tamanho máximo recomendado em caracteres para o código identificador dos itens desta tabela geral.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Tamanho máx. do código", numericFormat = org.praxisplatform.uischema.NumericFormat.INTEGER,
      helpText = "Limite máximo de caracteres para os códigos de item da tabela.",
      group = "Estrutura do item",
      order = 60,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "straighten",
      readOnly = true,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 4)
  private Integer taminfo;

  @Schema(
      description =
          "Rótulo semântico atribuído à coluna complementar item1 para os itens pertencentes a esta tabela geral.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Rótulo de item1", icon = "label",
      helpText = "Significado negocial da primeira coluna complementar dos itens.",
      group = "Colunas complementares",
      order = 70,
      readOnly = true,
      tableHidden = true,
      quickViewVisible = true,
      quickViewOrder = 5)
  private String labelItem1;

  @Schema(
      description =
          "Rótulo semântico atribuído à coluna complementar item2 para os itens pertencentes a esta tabela geral.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Rótulo de item2",
      helpText = "Significado negocial da segunda coluna complementar dos itens.",
      group = "Colunas complementares",
      order = 80,
      readOnly = true,
      tableHidden = true)
  private String labelItem2;

  @Schema(
      description =
          "Rótulo semântico atribuído à coluna complementar item3 para os itens pertencentes a esta tabela geral.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Rótulo de item3",
      helpText = "Significado negocial da terceira coluna complementar dos itens.",
      group = "Colunas complementares",
      order = 90,
      readOnly = true,
      tableHidden = true)
  private String labelItem3;

  @Schema(
      description =
          "Instante UTC da última atualização cadastral registrada na tabela geral de domínio.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Última atualização",
      helpText = "Data e hora UTC da última revisão do registro.",
      group = "Auditoria",
      order = 100,
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_TIME_PICKER,
      icon = "history",
      readOnly = true,
      tableHidden = true,
      formHidden = true)
  private java.time.OffsetDateTime updatedAt;
}
