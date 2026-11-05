package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.domain.ItemTabelaId;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.*;
import org.praxisplatform.uischema.*;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.extension.annotation.UISchemaPreset;

@Getter
@Setter
@Schema(
    description =
        "Itens das tabelas gerais de domínio do Ergon/Hades (equivalente a HADES.ITEMTABELA_), com chave composta natural (tab, item), escopo opcional por órgão e colunas auxiliares item1..4.")
@ResourceIdentity(
    keyField = "id",
    titleField = "descr",
    displayLabelField = "descr",
    subtitleField = "tabelaDescricao",
    metadataFields = {"orgaoNome", "updatedAt"})
public class ItemTabelaResponseDTO {
  @Schema(
      description =
          "Identidade composta natural formada pelo código da tabela geral e pelo código do item (ex.: ERG_ESTCIVIL:CASADO).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Identificador composto",
      helpText = "Chave composta natural do item da tabela geral.",
      preset = UISchemaPreset.ENTERPRISE_ID,
      group = "Identificação",
      order = 5,
      readOnly = true,
      tableHidden = true,
      formHidden = true)
  private ItemTabelaId id;

  @Schema(
      description =
          "Código da tabela geral de domínio à qual o item pertence (componente 1 da chave composta natural).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Tabela geral",
      helpText = "Tabela geral de domínio proprietária deste item.",
      group = "Identificação",
      order = 10,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "table_chart",
      endpoint = ApiPaths.TABELA_OPTIONS,
      valueField = "id",
      displayField = "label",
      readOnly = true,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 1)
  private String tab;

  @Schema(
      description =
          "Finalidade descritiva da tabela geral de domínio à qual o item pertence.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Finalidade da tabela",
      helpText = "Denominação da tabela geral proprietária.",
      group = "Identificação",
      order = 15,
      readOnly = true,
      tableHidden = true,
      formHidden = true)
  @ResourceReference(
      resourceKey = "administracao-pessoal.tabelas",
      valueField = "tab",
      surfaceId = "ficha-tabela",
      previewSurfaceId = "ficha-tabela",
      detailPresentation = "drawer")
  private String tabelaDescricao;

  @Schema(
      description =
          "Código identificador do item dentro da tabela geral de domínio (componente 2 da chave composta natural).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Código do item", icon = "tag",
      helpText = "Valor armazenado quando o item é selecionado.",
      group = "Identificação",
      order = 20,
      readOnly = true,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 2)
  private String item;

  @Schema(
      description =
          "Descrição legível do item apresentada em caixas de seleção, grades e relatórios.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Descrição do item",
      helpText = "Denominação oficial exibida para o item de domínio.",
      group = "Identificação",
      order = 30,
      readOnly = true,
      tableHidden = false)
  @ResourceReference(
      resourceKey = "administracao-pessoal.itens-tabela",
      valueField = "id",
      surfaceId = "ficha-item-tabela",
      previewSurfaceId = "ficha-item-tabela",
      detailPresentation = "drawer")
  private String descr;

  @Schema(
      description =
          "Órgão público específico ao qual o item se restringe quando a tabela geral é multi-órgão (nulo para itens globais).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Órgão específico",
      helpText = "Órgão proprietário do item quando restrito; vazio para item global.",
      group = "Escopo e ordenação",
      order = 40,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "apartment",
      endpoint = ApiPaths.ORGAO_OPTIONS,
      valueField = "id",
      displayField = "label",
      readOnly = true,
      tableHidden = false)
  private Integer empCodigo;

  @Schema(
      description = "Nome do órgão público quando o item possui restrição multi-órgão.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Nome do órgão", icon = "apartment",
      helpText = "Denominação do órgão público associado ao item.",
      group = "Escopo e ordenação",
      order = 45,
      readOnly = true,
      tableHidden = true,
      formHidden = true)
  @ResourceReference(
      resourceKey = "administracao-pessoal.empresas",
      valueField = "empCodigo",
      surfaceId = "ficha-institucional",
      previewSurfaceId = "ficha-institucional",
      detailPresentation = "drawer")
  private String orgaoNome;

  @Schema(
      description = "Ordem numérica de exibição do item em listas de seleção e formulários.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Ordem de exibição", numericFormat = org.praxisplatform.uischema.NumericFormat.INTEGER,
      helpText = "Posição sequencial do item nas listas de seleção.",
      group = "Escopo e ordenação",
      order = 50,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "format_list_numbered",
      readOnly = true,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 3)
  private Integer ordem;

  @Schema(
      description = "Indica se o item de domínio está ativo para seleção em novos cadastros.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Item ativo",
      helpText = "Define se o item pode ser selecionado em novos lançamentos.",
      group = "Escopo e ordenação",
      order = 60,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "verified",
      labelTrue = "Ativo",
      labelFalse = "Inativo",
      toneTrue = "success",
      toneFalse = "neutral",
      readOnly = true,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 4)
  private Boolean ativo;

  @Schema(
      description =
          "Atributo complementar 1 do item (ex.: ordem de escolaridade, código de equivalência eSocial ou agrupador).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Atributo item1", icon = "tune",
      helpText = "Primeiro valor complementar parametrizado para o item.",
      group = "Atributos complementares",
      order = 70,
      readOnly = true,
      tableHidden = true,
      quickViewVisible = true,
      quickViewOrder = 5)
  private String item1;

  @Schema(
      description = "Atributo complementar 2 do item (ex.: sigla auxiliar ou equivalência legal).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Atributo item2",
      helpText = "Segundo valor complementar parametrizado para o item.",
      group = "Atributos complementares",
      order = 80,
      readOnly = true,
      tableHidden = true)
  private String item2;

  @Schema(
      description =
          "Atributo complementar 3 em texto livre ou expressão de parametrização associada ao item.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Atributo item3",
      helpText = "Terceiro valor complementar ou regra textual associada ao item.",
      group = "Atributos complementares",
      order = 90,
      controlType = FieldControlType.TEXTAREA,
      readOnly = true,
      tableHidden = true)
  private String item3;

  @Schema(
      description = "Atributo complementar 4 para metadados adicionais de integração externa.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Atributo item4",
      helpText = "Quarto valor complementar para integração externa.",
      group = "Atributos complementares",
      order = 100,
      readOnly = true,
      tableHidden = true)
  private String item4;

  @Schema(
      description = "Instante UTC da última atualização cadastral registrada no item de domínio.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Última atualização",
      helpText = "Data e hora UTC da última revisão do registro.",
      group = "Auditoria",
      order = 110,
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_TIME_PICKER,
      icon = "history",
      readOnly = true,
      tableHidden = true,
      formHidden = true)
  private java.time.OffsetDateTime updatedAt;
}
