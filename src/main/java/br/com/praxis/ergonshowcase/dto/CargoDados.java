package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.*;
import lombok.Getter;
import lombok.Setter;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.NumericFormat;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.customfields.CustomFields;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.extension.annotation.UISchemaPreset;

/** Definição administrativa do cargo, preservando seu órgão proprietário no update. */
@Getter
@Setter
public abstract class CargoDados {
  @NotBlank
  @Size(max = 120)
  @Schema(
      description =
          "Nomenclatura oficial do cargo público ou função de confiança no quadro de pessoal.")
  @UISchema(
      label = "Nome do cargo",
      preset = UISchemaPreset.ENTERPRISE_NAME,
      group = "Identificação",
      order = 10,
      icon = "work")
  @ResourceReference(
      resourceKey = "administracao-pessoal.cargos",
      valueField = "id",
      surfaceId = "ficha-cargo",
      previewSurfaceId = "ficha-cargo",
      detailPresentation = "drawer")
  private String nome;

  @NotBlank
  @Size(max = 20)
  @Schema(
      description =
          "Categoria funcional do cargo, utilizada no enquadramento do servidor e na aplicação das regras estatutárias.")
  @UISchema(label = "Categoria", group = "Enquadramento", order = 20, icon = "category")
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE}, allowClear = false)
  private String categoria;

  @Size(max = 40)
  @Schema(nullable = true, description = "Especialidade ou classe funcional que detalha a categoria do cargo.")
  @UISchema(label = "Subcategoria", group = "Enquadramento", order = 30, icon = "label")
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  private String subcategoria;

  @Size(max = 40)
  @Schema(nullable = true,
      description =
          "Classificação estrutural do provimento, como cargo efetivo, comissionado ou função gratificada.")
  @UISchema(
      label = "Tipo de cargo",
      group = "Enquadramento",
      order = 40,
      icon = "work_outline",
      quickViewVisible = true,
      quickViewOrder = 1)
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE}, allowClear = false)
  private String tipoCargo;

  @NotBlank
  @Pattern(regexp = "[CF]")
  @Schema(
      description =
          "Distingue cargo público (C) de função de confiança (F) no quadro institucional.")
  @UISchema(label = "Cargo ou função", group = "Enquadramento", order = 50, icon = "badge")
  private String cargoFuncao = "C";

  @Size(max = 20)
  @Schema(nullable = true,
      description =
          "Grau de instrução mínimo exigido para provimento no cargo, conforme o domínio de escolaridade do Ergon.")
  @UISchema(
      label = "Escolaridade exigida",
      group = "Enquadramento",
      order = 60,
      icon = "school",
      quickViewVisible = true,
      quickViewOrder = 2)
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  private String escolaridade;

  @Size(max = 10)
  @Schema(nullable = true,
      description =
          "Código da Classificação Brasileira de Ocupações associado ao cargo; tratado como identificador textual.")
  @UISchema(
      label = "Prefixo CBO",
      type = FieldDataType.TEXT,
      group = "Enquadramento",
      order = 70,
      icon = "tag")
  private String cbo;

  @PositiveOrZero
  @Schema(nullable = true,
      description = "Total de vagas legalmente autorizadas para provimento neste cargo ou função.")
  @UISchema(
      label = "Vagas autorizadas",
      numericFormat = NumericFormat.INTEGER,
      group = "Quadro de vagas",
      order = 80,
      icon = "groups")
  private Integer vagasAutorizadas;

  @NotNull
  @Schema(
      description = "Indica que o provimento requer controle do saldo de vagas autorizadas em lei.")
  @UISchema(
      label = "Controle de vagas",
      type = FieldDataType.BOOLEAN,
      controlType = org.praxisplatform.uischema.FieldControlType.SELECT,
      options = "[{\"value\":true,\"label\":\"Sim\"},{\"value\":false,\"label\":\"Não\"}]",
      helpText = "Selecione sim ou não; no novo cadastro esta escolha é obrigatória.",
      group = "Quadro de vagas",
      order = 90,
      icon = "rule",
      quickViewVisible = true,
      quickViewOrder = 3)
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE}, allowClear = false)
  private Boolean controleVaga;

  @NotNull
  @Schema(
      description =
          "Indica remuneração estruturada por tabela de vencimentos e referências salariais.")
  @UISchema(
      label = "Por referência salarial",
      type = FieldDataType.BOOLEAN,
      controlType = org.praxisplatform.uischema.FieldControlType.SELECT,
      options = "[{\"value\":true,\"label\":\"Sim\"},{\"value\":false,\"label\":\"Não\"}]",
      helpText = "Selecione sim ou não; no novo cadastro esta escolha é obrigatória.",
      group = "Quadro de vagas",
      order = 100,
      icon = "payments")
  private Boolean porReferencia;

  @CustomFields(resourceKey = "administracao-pessoal.cargos")
  @Schema(
      description =
          "Atributos adicionais do cargo definidos pelo catálogo global e pelo órgão selecionado, incluindo requisitos profissionais.")
  @UISchema(label = "Características do órgão", group = "Características adicionais", order = 110)
  private Map<String, Object> customFields = new LinkedHashMap<>();

  @NotNull
  @Schema(description = "Indica cargo que reúne outros cargos para controle agregado do quadro.")
  @UISchema(
      label = "Cargo aglutinador",
      type = FieldDataType.BOOLEAN,
      controlType = org.praxisplatform.uischema.FieldControlType.SELECT,
      options = "[{\"value\":true,\"label\":\"Sim\"},{\"value\":false,\"label\":\"Não\"}]",
      icon = "hub",
      helpText = "Selecione sim ou não; a condição da origem não é transferida ao novo cargo.",
      group = "Quadro de Vagas e Vigência",
      tableHidden = true,
      order = 200)
  @Getter(onMethod_ = @com.fasterxml.jackson.annotation.JsonProperty("eAglutinador"))
  @Setter(onMethod_ = @com.fasterxml.jackson.annotation.JsonProperty("eAglutinador"))
  @com.fasterxml.jackson.annotation.JsonProperty("eAglutinador")
  private Boolean eAglutinador;

  @Schema(nullable = true, description = "Cargo agregador do mesmo órgão ao qual este cargo está vinculado.")
  @UISchema(
      label = "Cargo agregador",
      group = "Quadro de Vagas e Vigência",
      tableHidden = true,
      order = 201,
      type = org.praxisplatform.uischema.FieldDataType.TEXT,
      controlType = org.praxisplatform.uischema.FieldControlType.ENTITY_LOOKUP,
      icon = "hub",
      endpoint = ApiPaths.CARGO_OPTIONS,
      valueField = "id",
      displayField = "label")
  @ResourceReference(
      resourceKey = "administracao-pessoal.cargos",
      valueField = "cargoAglutinador",
      surfaceId = "ficha-cargo",
      previewSurfaceId = "ficha-cargo",
      detailPresentation = "drawer")
  private Integer cargoAglutinador;

  @Schema(nullable = true, description = "Data inicial da vigência do controle de vagas deste cargo.")
  @UISchema(
      label = "Início do controle de vagas",
      group = "Quadro de Vagas e Vigência",
      tableHidden = true,
      order = 202,
      controlType = org.praxisplatform.uischema.FieldControlType.DATE_PICKER,
      type = org.praxisplatform.uischema.FieldDataType.DATE,
      icon = "event")
  private LocalDate dtInicioContrVaga;

  @Schema(nullable = true, description = "Documento legal que fundamenta a definição do cargo.")
  @UISchema(
      label = "Documento legal",
      group = "Quadro de Vagas e Vigência",
      tableHidden = true,
      order = 203,
      type = org.praxisplatform.uischema.FieldDataType.TEXT,
      controlType = org.praxisplatform.uischema.FieldControlType.ENTITY_LOOKUP,
      icon = "gavel",
      endpoint = ApiPaths.DOCUMENTO_OPTIONS,
      valueField = "id",
      displayField = "label")
  @ResourceReference(
      resourceKey = "administracao-pessoal.documentos-legais",
      valueField = "documentoLegalId",
      surfaceId = "ficha-documento",
      previewSurfaceId = "ficha-documento",
      detailPresentation = "drawer")
  private Long documentoLegalId;

  @jakarta.validation.constraints.Null(
      message = "Prévia derivada não pode ser informada no submit.")
  @Schema(
      description =
          "Denominação consultada do cargo agregador; a manutenção definitiva verifica novamente sua existência e habilitação.")
  @UISchema(
      label = "Cargo agregador selecionado",
      group = "Quadro de Vagas e Vigência",
      readOnly = true,
      tableHidden = true,
      icon = "info")
  @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
  private String cargoAglutinadorNomePrevisto;
}
