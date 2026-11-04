package br.com.praxis.ergonshowcase.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.praxisplatform.uischema.bulk.BulkMode;
import br.com.praxis.ergonshowcase.domain.VinculoId;
import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.customfields.CustomFields;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.extension.annotation.UISchemaPreset;
import jakarta.validation.constraints.*;

@Getter
@Setter
@Schema(description = "Posições numeradas autorizadas por função, órgão e unidade, com origem e situação de ocupação.")
public abstract class VagaFuncaoDados {
  @Schema(description = "Órgão responsável pelo cadastro e pela gestão funcional deste registro.")
  @ResourceReference(resourceKey = "administracao-pessoal.empresas", valueField = "empCodigo",
      surfaceId = "ficha-institucional", previewSurfaceId = "ficha-institucional", detailPresentation = "drawer")
  @DomainGovernance(kind = DomainGovernanceKind.SECURITY,
      classification = DomainClassification.INTERNAL,
      dataCategory = DomainDataCategory.OPERATIONAL)
  @UISchema(label = "Órgão",
      helpText = "Selecione o órgão ao qual pertence o registro.",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "apartment",
      preset = UISchemaPreset.ENTERPRISE_ID,
      endpoint = ApiPaths.ORGAO_OPTIONS,
      valueField = "id",
      displayField = "label",
      group = "Identificação e situação",
      order = 10,
      tableHidden = true,
      visibleForAuthorities = {"ERGON_READ"},
      editableForAuthorities = {"ERGON_WRITE"})
  @NotNull
  private Integer empCodigo;

  @Schema(description = "Identificação da posição numerada no quadro de distribuição do órgão.")
  @DomainGovernance(kind = DomainGovernanceKind.SECURITY,
      classification = DomainClassification.INTERNAL,
      dataCategory = DomainDataCategory.OPERATIONAL)
  @UISchema(label = "Descrição",
      helpText = "Use uma identificação curta para localizar este registro.",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "notes",
      preset = UISchemaPreset.NONE,
      group = "Identificação e situação",
      order = 20,
      tableHidden = false,
      visibleForAuthorities = {"ERGON_READ"},
      editableForAuthorities = {"ERGON_WRITE"})
  @NotBlank
  @Size(max = 200)
  @BulkEditable(modes = {BulkMode.PER_ITEM_UPDATE}, allowClear = false)
  private String descricao;

  @Schema(description = "Data em que a posição numerada passa a compor o quadro autorizado.")
  @DomainGovernance(kind = DomainGovernanceKind.SECURITY,
      classification = DomainClassification.INTERNAL,
      dataCategory = DomainDataCategory.OPERATIONAL)
  @UISchema(label = "Início da vigência",
      helpText = "Use a data inicial que consta no ato.",
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "event",
      preset = UISchemaPreset.DATE_RANGE,
      group = "Vigência",
      order = 30,
      tableHidden = true,
      quickViewVisible = true,
      quickViewOrder = 5,
      visibleForAuthorities = {"ERGON_READ"},
      editableForAuthorities = {"ERGON_WRITE"})
  @NotNull
  private LocalDate dtini;

  @Schema(description = "Último dia em que a posição integra o quadro da função, inclusive.", nullable = true)
  @DomainGovernance(kind = DomainGovernanceKind.SECURITY,
      classification = DomainClassification.INTERNAL,
      dataCategory = DomainDataCategory.OPERATIONAL)
  @UISchema(label = "Fim da vigência",
      helpText = "Deixe em branco enquanto a vigência estiver aberta.",
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "event",
      preset = UISchemaPreset.DATE_RANGE,
      group = "Vigência",
      order = 40,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 6,
      visibleForAuthorities = {"ERGON_READ"},
      editableForAuthorities = {"ERGON_WRITE"})
  private LocalDate dtfim;

  @Schema(description = "Posição cadastral da vaga quanto à ocupação, bloqueio ou extinção.", allowableValues = {"LIVRE", "OCUPADA", "BLOQUEADA", "EXTINTA"})
  @DomainGovernance(kind = DomainGovernanceKind.SECURITY,
      classification = DomainClassification.INTERNAL,
      dataCategory = DomainDataCategory.OPERATIONAL)
  @UISchema(label = "Situação",
      helpText = "Selecione a posição cadastral documentada.",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      icon = "flag",
      preset = UISchemaPreset.ENTERPRISE_STATUS,
      options = "[{\"value\":\"LIVRE\",\"label\":\"Livre\"},{\"value\":\"OCUPADA\",\"label\":\"Ocupada\"},{\"value\":\"BLOQUEADA\",\"label\":\"Bloqueada\"},{\"value\":\"EXTINTA\",\"label\":\"Extinta\"}]",
      group = "Identificação e situação",
      order = 50,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 3,
      visibleForAuthorities = {"ERGON_READ"},
      editableForAuthorities = {"ERGON_WRITE"})
  @NotBlank
  @Size(max = 30)
  @Pattern(regexp = "LIVRE|OCUPADA|BLOQUEADA|EXTINTA")
  private String situacao;

  @Schema(description = "Número de até doze dígitos atribuído à posição autorizada para ocupação da função no órgão.")
  @DomainGovernance(kind = DomainGovernanceKind.SECURITY,
      classification = DomainClassification.INTERNAL,
      dataCategory = DomainDataCategory.OPERATIONAL)
  @UISchema(label = "Número da vaga",
      helpText = "Use o número publicado na distribuição de vagas.",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "tag",
      preset = UISchemaPreset.NONE,
      group = "Identificação e situação",
      order = 60,
      tableHidden = false,
      visibleForAuthorities = {"ERGON_READ"},
      editableForAuthorities = {"ERGON_WRITE"})
  @NotNull
  @Positive @Max(999999999999L)
  private Long numeroVaga;

  @Schema(description = "Função de confiança para a qual a vaga numerada foi autorizada.")
  @ResourceReference(resourceKey = ApiPaths.KEY_FUNCOES, valueField = "funcaoId",
      surfaceId = "detail", previewSurfaceId = "detail", detailPresentation = "drawer")
  @DomainGovernance(kind = DomainGovernanceKind.SECURITY,
      classification = DomainClassification.INTERNAL,
      dataCategory = DomainDataCategory.OPERATIONAL)
  @UISchema(label = "Função autorizada",
      helpText = "Escolha a função que consta no ato.",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "link",
      preset = UISchemaPreset.ENTERPRISE_NAME,
      endpoint = ApiPaths.FUNCAO_OPTIONS,
      valueField = "id",
      displayField = "label",
      group = "Vínculos e unidades",
      order = 70,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 1,
      visibleForAuthorities = {"ERGON_READ"},
      editableForAuthorities = {"ERGON_WRITE"})
  @NotNull
  private Long funcaoId;

  @Schema(description = "Unidade organizacional à qual a posição foi distribuída.")
  @ResourceReference(resourceKey = "administracao-pessoal.setores", valueField = "setorId",
      surfaceId = "ficha-unidade", previewSurfaceId = "ficha-unidade", detailPresentation = "drawer")
  @DomainGovernance(kind = DomainGovernanceKind.SECURITY,
      classification = DomainClassification.INTERNAL,
      dataCategory = DomainDataCategory.OPERATIONAL)
  @UISchema(label = "Unidade da vaga",
      helpText = "Selecione a unidade beneficiária da distribuição.",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "link",
      preset = UISchemaPreset.ENTERPRISE_NAME,
      endpoint = ApiPaths.SETOR_OPTIONS,
      valueField = "id",
      displayField = "label",
      group = "Vínculos e unidades",
      order = 80,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 2,
      visibleForAuthorities = {"ERGON_READ"},
      editableForAuthorities = {"ERGON_WRITE"})
  @NotBlank
  @Size(max = 30)
  private String setorId;

  @Schema(description = "Classificação administrativa da origem da vaga, como criação por lei ou redistribuição.")
  @DomainGovernance(kind = DomainGovernanceKind.SECURITY,
      classification = DomainClassification.INTERNAL,
      dataCategory = DomainDataCategory.OPERATIONAL)
  @UISchema(label = "Origem da vaga",
      helpText = "Identifique a origem prevista no ato.",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "notes",
      preset = UISchemaPreset.NONE,
      group = "Identificação e situação",
      order = 90,
      tableHidden = true,
      quickViewVisible = true,
      quickViewOrder = 4,
      visibleForAuthorities = {"ERGON_READ"},
      editableForAuthorities = {"ERGON_WRITE"})
  @NotBlank
  @Size(max = 50)
  private String origemVaga;

  @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
  @Schema(description = "Vínculo em exercício na posição numerada, atualizado pelo ciclo de vida da designação.",
      accessMode = Schema.AccessMode.READ_ONLY, nullable = true)
  @ResourceReference(resourceKey = "administracao-pessoal.vinculos", valueField = "titularAtualId",
      surfaceId = "resumo-funcional", previewSurfaceId = "resumo-funcional", detailPresentation = "drawer")
  @DomainGovernance(kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL, exportable = false)
  @UISchema(label = "Titular atual",
      helpText = "Consulte o vínculo que está em exercício na vaga.",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "link",
      preset = UISchemaPreset.ENTERPRISE_ID,
      endpoint = ApiPaths.VINCULO_OPTIONS,
      valueField = "id",
      displayField = "label",
      group = "Vínculos e unidades",
      order = 100,
      tableHidden = true,
      readOnly = true, editable = false,
      visibleForAuthorities = {"ERGON_READ"})
  private VinculoId titularAtualId;

  @Schema(description = "Fundamento administrativo que impede o uso de uma vaga ainda vigente.", nullable = true)
  @DomainGovernance(kind = DomainGovernanceKind.SECURITY,
      classification = DomainClassification.INTERNAL,
      dataCategory = DomainDataCategory.OPERATIONAL)
  @UISchema(label = "Motivo do bloqueio",
      helpText = "Detalhe a restrição de ocupação.",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "notes",
      preset = UISchemaPreset.NONE,
      group = "Identificação e situação",
      order = 110,
      tableHidden = true,
      visibleForAuthorities = {"ERGON_READ"},
      editableForAuthorities = {"ERGON_WRITE"})
  @Size(max = 500)
  private String motivoBloqueio;

  @Schema(description = "Documento que fornece o fundamento jurídico para o registro.", nullable = true)
  @ResourceReference(resourceKey = "administracao-pessoal.documentos-legais", valueField = "documentoLegalId",
      surfaceId = "ficha-documento", previewSurfaceId = "ficha-documento", detailPresentation = "drawer")
  @DomainGovernance(kind = DomainGovernanceKind.SECURITY,
      classification = DomainClassification.INTERNAL,
      dataCategory = DomainDataCategory.LEGAL)
  @UISchema(label = "Fundamento legal",
      helpText = "Selecione o ato legal de referência.",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "link",
      preset = UISchemaPreset.LEGAL_DOCUMENT_REFERENCE,
      endpoint = ApiPaths.DOCUMENTO_OPTIONS,
      valueField = "id",
      displayField = "label",
      group = "Atos e fundamento",
      order = 120,
      tableHidden = true,
      visibleForAuthorities = {"ERGON_READ"},
      editableForAuthorities = {"ERGON_WRITE"})
  @BulkEditable(modes = {BulkMode.UNIFORM_UPDATE, BulkMode.PER_ITEM_UPDATE}, allowClear = false)
  private Long documentoLegalId;

  @Schema(description = "Documento que comprova a publicação oficial deste registro.", nullable = true)
  @ResourceReference(resourceKey = "administracao-pessoal.documentos-legais", valueField = "atoPublicacaoId",
      surfaceId = "ficha-documento", previewSurfaceId = "ficha-documento", detailPresentation = "drawer")
  @DomainGovernance(kind = DomainGovernanceKind.SECURITY,
      classification = DomainClassification.INTERNAL,
      dataCategory = DomainDataCategory.LEGAL)
  @UISchema(label = "Ato publicado",
      helpText = "Selecione a publicação correspondente.",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "link",
      preset = UISchemaPreset.LEGAL_DOCUMENT_REFERENCE,
      endpoint = ApiPaths.DOCUMENTO_OPTIONS,
      valueField = "id",
      displayField = "label",
      group = "Atos e fundamento",
      order = 130,
      tableHidden = true,
      visibleForAuthorities = {"ERGON_READ"},
      editableForAuthorities = {"ERGON_WRITE"})
  private Long atoPublicacaoId;

  @Schema(description = "Informações administrativas complementares do assentamento, sem substituir seu ato legal.", nullable = true)
  @DomainGovernance(kind = DomainGovernanceKind.SECURITY,
      classification = DomainClassification.INTERNAL,
      dataCategory = DomainDataCategory.OPERATIONAL)
  @UISchema(label = "Observações",
      helpText = "Registre apenas informações necessárias à gestão da função.",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "notes",
      preset = UISchemaPreset.NONE,
      group = "Registro administrativo",
      order = 140,
      tableHidden = true,
      visibleForAuthorities = {"ERGON_READ"},
      editableForAuthorities = {"ERGON_WRITE"})
  @Size(max = 2000)
  @BulkEditable(modes = {BulkMode.UNIFORM_UPDATE, BulkMode.PER_ITEM_UPDATE}, allowClear = false)
  private String observacoes;

  @Schema(description = "Atributos institucionais definidos pelo órgão para este tipo de registro e armazenados em JSON.", nullable = true)
  @DomainGovernance(kind = DomainGovernanceKind.SECURITY,
      classification = DomainClassification.INTERNAL,
      dataCategory = DomainDataCategory.OPERATIONAL)
  @CustomFields(resourceKey = ApiPaths.KEY_VAGAS_FUNCAO)
  @UISchema(label = "Campos customizados",
      helpText = "Preencha somente os campos habilitados para o órgão.",
      type = FieldDataType.JSON,
      controlType = FieldControlType.INPUT,
      icon = "extension",
      preset = UISchemaPreset.NONE,
      group = "Extensões institucionais",
      order = 150,
      tableHidden = true,
      visibleForAuthorities = {"ERGON_READ"},
      editableForAuthorities = {"ERGON_WRITE"})
  private Map<String, Object> customFields = new LinkedHashMap<>();

  @Schema(description = "Indica se a função da posição está sujeita a controle de vagas.", accessMode = Schema.AccessMode.READ_ONLY, nullable = true)
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  @UISchema(label = "Controle de vagas", helpText = "Indica se a função da posição está sujeita a controle de vagas.", group = "Prévia da operação",
      type = FieldDataType.BOOLEAN, controlType = FieldControlType.CHECKBOX, icon = "event_seat",
      tableHidden = true, readOnly = true, editable = false)
  private Boolean controleVagaPrevisto;
}
