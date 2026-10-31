package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.annotation.ResourceReference;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.extension.annotation.UISchemaPreset;

/** Sugestões independentes da origem, sem autorização de vagas ou obrigação de persistência. */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.ALWAYS)
@Schema(description = "Rascunho parcial de novo cargo. Revise as sugestões e complete o contrato de criação; preparar não reserva código nem autoriza provimento.")
public class CargoDuplicateDraftDTO {
  @Schema(nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      description = "Código do novo cargo, deixado sem preenchimento. A criação exige código positivo ainda disponível.")
  @UISchema(label = "Novo código", group = "Identificação", icon = "badge")
  private Integer cargo;

  @Schema(nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      description = "Órgão proprietário a selecionar para o novo quadro; a criação confere sua correspondência com o contexto corrente.")
  @UISchema(label = "Órgão", group = "Identificação", icon = "apartment",
      preset = UISchemaPreset.ENTERPRISE_ID,
      type = FieldDataType.NUMBER, controlType = FieldControlType.ENTITY_LOOKUP,
      endpoint = ApiPaths.ORGAO_OPTIONS, valueField = "id", displayField = "label",
      helpText = "Selecione o órgão do novo cadastro; o órgão da origem não é copiado.")
  @ResourceReference(resourceKey = "administracao-pessoal.empresas", valueField = "empCodigo",
      surfaceId = "ficha-institucional", previewSurfaceId = "ficha-institucional", detailPresentation = "drawer")
  private Integer empCodigo;

  @Schema(nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      description = "Nomenclatura do cargo de origem sugerida para revisão; o novo cadastro pode ter a mesma denominação com outro código.")
  @UISchema(label = "Nome do cargo", group = "Identificação", icon = "work")
  private String nome;

  @Schema(nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      description = "Categoria funcional sugerida a partir da origem; sua cópia não certifica enquadramento legal do novo cargo.")
  @UISchema(label = "Categoria", group = "Enquadramento", icon = "category")
  private String categoria;

  @Schema(nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      description = "Especialidade ou classe da origem, quando informada, para revisão no novo enquadramento.")
  @UISchema(label = "Subcategoria", group = "Enquadramento", icon = "label")
  private String subcategoria;

  @Schema(nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      description = "Classificação do cargo ou função sugerida pela origem, sem inferir regras adicionais de provimento.")
  @UISchema(label = "Tipo de cargo", group = "Enquadramento", icon = "work_outline")
  private String tipoCargo;

  @Schema(nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      description = "Distinção da origem entre cargo (C) e função (F), preservada sem converter funções em cargos.")
  @UISchema(label = "Cargo ou função", group = "Enquadramento", icon = "badge")
  private String cargoFuncao;

  @Schema(nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      description = "Escolaridade mínima da origem sugerida para revisão dos requisitos do novo cargo.")
  @UISchema(label = "Escolaridade exigida", group = "Enquadramento", icon = "school")
  private String escolaridade;

  @Schema(nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      description = "Identificador textual CBO da origem, preservando zeros à esquerda; não representa validação contra catálogo externo.")
  @UISchema(label = "CBO", group = "Enquadramento", icon = "tag")
  private String cbo;

  @Schema(nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      description = "Quantidade de vagas do novo cargo, não herdada da origem. Controle habilitado exige quantidade positiva no cadastro final.")
  @UISchema(label = "Vagas autorizadas", group = "Quadro de vagas", icon = "groups")
  private Integer vagasAutorizadas;

  @Schema(nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      description = "Escolha pendente sobre controle de vagas no novo cargo. A criação exige informar sim ou não explicitamente.")
  @UISchema(label = "Controle de vagas", group = "Quadro de vagas", icon = "rule",
      preset = UISchemaPreset.BOOLEAN_FLAG,
      type = FieldDataType.BOOLEAN, controlType = FieldControlType.SELECT,
      options = "[{\"value\":true,\"label\":\"Sim\"},{\"value\":false,\"label\":\"Não\"}]",
      labelTrue = "Sim", labelFalse = "Não", toneTrue = "success", toneFalse = "neutral",
      helpText = "Escolha sim ou não no novo cadastro; o rascunho não antecipa a resposta.")
  private Boolean controleVaga;

  @Schema(nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      description = "Escolha pendente do indicador de referência salarial do host; não reproduz integralmente o regime POR_REFER do legado.")
  @UISchema(label = "Por referência salarial", group = "Quadro de vagas", icon = "payments",
      preset = UISchemaPreset.BOOLEAN_FLAG,
      type = FieldDataType.BOOLEAN, controlType = FieldControlType.SELECT,
      options = "[{\"value\":true,\"label\":\"Sim\"},{\"value\":false,\"label\":\"Não\"}]",
      labelTrue = "Sim", labelFalse = "Não", toneTrue = "success", toneFalse = "neutral",
      helpText = "Defina explicitamente se o novo cargo usa referência salarial.")
  private Boolean porReferencia;

  @Schema(nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      description = "Escolha pendente sobre reunir outros cargos no controle agregado; não é copiada da origem.")
  @UISchema(label = "Cargo aglutinador", group = "Quadro de vagas", icon = "hub",
      preset = UISchemaPreset.BOOLEAN_FLAG,
      type = FieldDataType.BOOLEAN, controlType = FieldControlType.SELECT,
      options = "[{\"value\":true,\"label\":\"Sim\"},{\"value\":false,\"label\":\"Não\"}]",
      labelTrue = "Sim", labelFalse = "Não", toneTrue = "success", toneFalse = "neutral",
      helpText = "Defina explicitamente se o novo cargo reúne outros cargos no controle agregado.")
  @Getter(onMethod_ = @JsonProperty("eAglutinador"))
  @Setter(onMethod_ = @JsonProperty("eAglutinador"))
  @com.fasterxml.jackson.annotation.JsonProperty("eAglutinador")
  private Boolean eAglutinador;

  @Schema(nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      description = "Cargo agregador a escolher novamente, se aplicável; o cadastro final verifica órgão e habilitação do agregador.")
  @UISchema(label = "Cargo agregador", group = "Quadro de vagas", icon = "hub",
      preset = UISchemaPreset.ENTERPRISE_ID,
      type = FieldDataType.NUMBER, controlType = FieldControlType.ENTITY_LOOKUP,
      endpoint = ApiPaths.CARGO_OPTIONS, valueField = "id", displayField = "label",
      helpText = "Selecione novamente o agregador, se aplicável; a criação confere órgão e habilitação.")
  @ResourceReference(resourceKey = "administracao-pessoal.cargos", valueField = "cargoAglutinador",
      surfaceId = "ficha-cargo", previewSurfaceId = "ficha-cargo", detailPresentation = "drawer")
  private Integer cargoAglutinador;

  @Schema(nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      description = "Início do controle de vagas do novo cargo, sem reaproveitar a vigência da origem.")
  @UISchema(label = "Início do controle", group = "Quadro de vagas", icon = "event",
      type = org.praxisplatform.uischema.FieldDataType.DATE,
      controlType = org.praxisplatform.uischema.FieldControlType.DATE_PICKER)
  private LocalDate dtInicioContrVaga;

  @Schema(nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      description = "Documento de embasamento legal a revisar e selecionar para o novo cargo; a referência da origem não é herdada.")
  @UISchema(label = "Documento legal", group = "Quadro de vagas", icon = "gavel",
      preset = UISchemaPreset.LEGAL_DOCUMENT_REFERENCE,
      type = FieldDataType.NUMBER, controlType = FieldControlType.ENTITY_LOOKUP,
      endpoint = ApiPaths.DOCUMENTO_OPTIONS, valueField = "id", displayField = "label",
      helpText = "Selecione o documento que fundamenta o novo cargo; a referência anterior não é herdada.")
  @ResourceReference(resourceKey = "administracao-pessoal.documentos-legais", valueField = "documentoLegalId",
      surfaceId = "ficha-documento", previewSurfaceId = "ficha-documento", detailPresentation = "drawer")
  private Long documentoLegalId;

  @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      description = "Mapa vazio: nenhum atributo adicional da origem é copiado. O preenchimento final usa o catálogo vigente do órgão de criação.")
  @UISchema(label = "Características do órgão", group = "Características adicionais", icon = "tune")
  private Map<String, Object> customFields = new LinkedHashMap<>();
}
