package br.com.praxis.ergonshowcase.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.praxisplatform.uischema.bulk.BulkMode;
import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.NumericFormat;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.customfields.CustomFields;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.extension.annotation.UISchemaPreset;
import jakarta.validation.constraints.*;

@Getter
@Setter
@Schema(description = "Catálogo por órgão de funções de direção, chefia e assessoramento, com vigência, gratificação e política de vagas e substituição.")
public abstract class FuncaoConfiancaDados {
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

  @Schema(description = "Nome oficial da função sob o qual a unidade publica atos de provimento.")
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

  @Schema(description = "Primeiro dia de validade do cadastro da função no órgão.")
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
      tableHidden = false,
      visibleForAuthorities = {"ERGON_READ"},
      editableForAuthorities = {"ERGON_WRITE"})
  @NotNull
  private LocalDate dtini;

  @Schema(description = "Último dia de vigência da função, inclusive, quando houver extinção ou término previsto.", nullable = true)
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
      tableHidden = true,
      quickViewVisible = true,
      quickViewOrder = 6,
      visibleForAuthorities = {"ERGON_READ"},
      editableForAuthorities = {"ERGON_WRITE"})
  private LocalDate dtfim;

  @Schema(description = "Posição cadastral da função quanto à disponibilidade institucional para designação.", allowableValues = {"ATIVA", "SUSPENSA", "EXTINTA"})
  @DomainGovernance(kind = DomainGovernanceKind.SECURITY,
      classification = DomainClassification.INTERNAL,
      dataCategory = DomainDataCategory.OPERATIONAL)
  @UISchema(label = "Situação",
      helpText = "Selecione a posição cadastral documentada.",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      icon = "flag",
      preset = UISchemaPreset.ENTERPRISE_STATUS,
      options = "[{\"value\":\"ATIVA\",\"label\":\"Ativa\"},{\"value\":\"SUSPENSA\",\"label\":\"Suspensa\"},{\"value\":\"EXTINTA\",\"label\":\"Extinta\"}]",
      group = "Identificação e situação",
      order = 50,
      tableHidden = false,
      visibleForAuthorities = {"ERGON_READ"},
      editableForAuthorities = {"ERGON_WRITE"})
  @NotBlank
  @Size(max = 30)
  @Pattern(regexp = "ATIVA|SUSPENSA|EXTINTA")
  private String situacao;

  @Schema(description = "Identificador funcional usado nos atos de designação e no cadastro de vagas do órgão.")
  @DomainGovernance(kind = DomainGovernanceKind.SECURITY,
      classification = DomainClassification.INTERNAL,
      dataCategory = DomainDataCategory.OPERATIONAL)
  @UISchema(label = "Código da função",
      helpText = "Informe o código registrado no ato.",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "notes",
      preset = UISchemaPreset.NONE,
      group = "Identificação e situação",
      order = 60,
      tableHidden = false,
      visibleForAuthorities = {"ERGON_READ"},
      editableForAuthorities = {"ERGON_WRITE"})
  @NotBlank
  @Size(max = 24)
  private String codigo;

  @Schema(description = "Natureza da função comissionada conforme sua finalidade institucional de direção, chefia ou assessoramento.", allowableValues = {"DIRECAO", "CHEFIA", "ASSESSORAMENTO"})
  @DomainGovernance(kind = DomainGovernanceKind.SECURITY,
      classification = DomainClassification.INTERNAL,
      dataCategory = DomainDataCategory.OPERATIONAL)
  @UISchema(label = "Categoria",
      helpText = "Selecione a finalidade da função.",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      icon = "flag",
      preset = UISchemaPreset.ENTERPRISE_STATUS,
      options = "[{\"value\":\"DIRECAO\",\"label\":\"Direção\"},{\"value\":\"CHEFIA\",\"label\":\"Chefia\"},{\"value\":\"ASSESSORAMENTO\",\"label\":\"Assessoramento\"}]",
      group = "Identificação e situação",
      order = 70,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 1,
      visibleForAuthorities = {"ERGON_READ"},
      editableForAuthorities = {"ERGON_WRITE"})
  @NotBlank
  @Size(max = 24)
  @Pattern(regexp = "DIRECAO|CHEFIA|ASSESSORAMENTO")
  @BulkEditable(modes = {BulkMode.UNIFORM_UPDATE, BulkMode.PER_ITEM_UPDATE}, allowClear = false)
  private String categoria;

  @Schema(description = "Símbolo da tabela remuneratória associado à função, como FG-1 ou DAS-3.")
  @DomainGovernance(kind = DomainGovernanceKind.SECURITY,
      classification = DomainClassification.INTERNAL,
      dataCategory = DomainDataCategory.OPERATIONAL)
  @UISchema(label = "Símbolo remuneratório",
      helpText = "Use o símbolo vigente no ato.",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "notes",
      preset = UISchemaPreset.NONE,
      group = "Identificação e situação",
      order = 80,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 2,
      visibleForAuthorities = {"ERGON_READ"},
      editableForAuthorities = {"ERGON_WRITE"})
  @NotBlank
  @Size(max = 20)
  @BulkEditable(modes = {BulkMode.UNIFORM_UPDATE, BulkMode.PER_ITEM_UPDATE}, allowClear = false)
  private String simbolo;

  @Schema(description = "Indicador de que a designação passa a exigir vaga numerada a partir do marco de controle cadastrado.")
  @DomainGovernance(kind = DomainGovernanceKind.SECURITY,
      classification = DomainClassification.INTERNAL,
      dataCategory = DomainDataCategory.OPERATIONAL)
  @UISchema(label = "Controle de vagas",
      helpText = "Marque quando houver distribuição formal de vagas.",
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "fact_check",
      preset = UISchemaPreset.BOOLEAN_FLAG,
      labelTrue = "Sim",
      labelFalse = "Não",
      toneTrue = "info",
      toneFalse = "neutral",
      group = "Identificação e situação",
      order = 90,
      tableHidden = true,
      quickViewVisible = true,
      quickViewOrder = 3,
      visibleForAuthorities = {"ERGON_READ"},
      editableForAuthorities = {"ERGON_WRITE"})
  @NotNull
  private Boolean controleVaga;

  @Schema(description = "Primeiro dia em que a designação nesta função passa a exigir número de vaga.", nullable = true)
  @DomainGovernance(kind = DomainGovernanceKind.SECURITY,
      classification = DomainClassification.INTERNAL,
      dataCategory = DomainDataCategory.OPERATIONAL)
  @UISchema(label = "Início do controle",
      helpText = "Use a data definida para implantação do controle.",
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "event",
      preset = UISchemaPreset.DATE_RANGE,
      group = "Vigência",
      order = 100,
      tableHidden = true,
      visibleForAuthorities = {"ERGON_READ"},
      editableForAuthorities = {"ERGON_WRITE"})
  private LocalDate dtInicioContrVaga;

  @Schema(description = "Indicador da política da função que permite exercício interino durante impedimento do titular.")
  @DomainGovernance(kind = DomainGovernanceKind.SECURITY,
      classification = DomainClassification.INTERNAL,
      dataCategory = DomainDataCategory.OPERATIONAL)
  @UISchema(label = "Admite substituição",
      helpText = "Confira a previsão de substituição da função.",
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "fact_check",
      preset = UISchemaPreset.BOOLEAN_FLAG,
      labelTrue = "Sim",
      labelFalse = "Não",
      toneTrue = "info",
      toneFalse = "neutral",
      group = "Identificação e situação",
      order = 110,
      tableHidden = true,
      quickViewVisible = true,
      quickViewOrder = 4,
      visibleForAuthorities = {"ERGON_READ"},
      editableForAuthorities = {"ERGON_WRITE"})
  @NotNull
  private Boolean permiteSubstituicao;

  @Schema(description = "Modalidade de substituição prevista no histórico da função e utilizada na identificação do evento funcional.", nullable = true)
  @DomainGovernance(kind = DomainGovernanceKind.SECURITY,
      classification = DomainClassification.INTERNAL,
      dataCategory = DomainDataCategory.OPERATIONAL)
  @UISchema(label = "Tipo de substituição",
      helpText = "Use o tipo definido no cadastro da função.",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "notes",
      preset = UISchemaPreset.NONE,
      group = "Identificação e situação",
      order = 120,
      tableHidden = true,
      visibleForAuthorities = {"ERGON_READ"},
      editableForAuthorities = {"ERGON_WRITE"})
  @Size(max = 50)
  private String tipoSubstituicao;

  @Schema(description = "Quantidade mínima de dias de exercício interino para enquadramento na política de remuneração da função.", nullable = true)
  @DomainGovernance(kind = DomainGovernanceKind.SECURITY,
      classification = DomainClassification.INTERNAL,
      dataCategory = DomainDataCategory.OPERATIONAL)
  @UISchema(label = "Dias mínimos",
      helpText = "Informe o mínimo previsto para pagamento.",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "tag",
      preset = UISchemaPreset.NONE,
      group = "Identificação e situação",
      order = 130,
      tableHidden = true,
      visibleForAuthorities = {"ERGON_READ"},
      editableForAuthorities = {"ERGON_WRITE"})
  @PositiveOrZero
  private Integer diasMinimosSubstituicao;

  @Schema(description = "Valor mensal integral da gratificação correspondente ao símbolo da função.")
  @DomainGovernance(kind = DomainGovernanceKind.SECURITY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.FINANCIAL, exportable = false)
  @UISchema(label = "Valor da gratificação",
      helpText = "Informe o valor nominal mensal em reais.",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.CURRENCY_INPUT,
      icon = "payments",
      preset = UISchemaPreset.MONETARY_AMOUNT,
      group = "Remuneração",
      order = 140,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 5,
      visibleForAuthorities = {"ERGON_READ"},
      editableForAuthorities = {"ERGON_WRITE"})
  @NotNull
  @PositiveOrZero
  @Digits(integer = 13, fraction = 2)
  private BigDecimal valorGratificacao;

  @Schema(description = "Parcela percentual da gratificação aplicável quando o servidor opta pela remuneração do vínculo efetivo.", nullable = true)
  @DomainGovernance(kind = DomainGovernanceKind.SECURITY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.FINANCIAL, exportable = false)
  @UISchema(label = "Percentual de opção",
      helpText = "Informe de 0 a 100.",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "percent",
      preset = UISchemaPreset.NONE, numericFormat = NumericFormat.PERCENT,
      group = "Remuneração",
      order = 150,
      tableHidden = true,
      visibleForAuthorities = {"ERGON_READ"},
      editableForAuthorities = {"ERGON_WRITE"})
  @PositiveOrZero
  @Digits(integer = 3, fraction = 2)
  @DecimalMax("100")
  private BigDecimal percentualOpcaoEfetivo;

  @Schema(description = "Indicação de reserva da função a ocupantes de vínculo efetivo, conforme o ato de instituição.")
  @DomainGovernance(kind = DomainGovernanceKind.SECURITY,
      classification = DomainClassification.INTERNAL,
      dataCategory = DomainDataCategory.OPERATIONAL)
  @UISchema(label = "Exige vínculo efetivo",
      helpText = "Confira os requisitos de provimento.",
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "fact_check",
      preset = UISchemaPreset.BOOLEAN_FLAG,
      labelTrue = "Sim",
      labelFalse = "Não",
      toneTrue = "info",
      toneFalse = "neutral",
      group = "Identificação e situação",
      order = 160,
      tableHidden = true,
      visibleForAuthorities = {"ERGON_READ"},
      editableForAuthorities = {"ERGON_WRITE"})
  @NotNull
  private Boolean exigeVinculoEfetivo;

  @Schema(description = "Requisitos de carreira, formação ou experiência exigidos para exercício da função.", nullable = true)
  @DomainGovernance(kind = DomainGovernanceKind.SECURITY,
      classification = DomainClassification.INTERNAL,
      dataCategory = DomainDataCategory.OPERATIONAL)
  @UISchema(label = "Requisitos de provimento",
      helpText = "Registre os critérios do ato de instituição.",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "notes",
      preset = UISchemaPreset.NONE,
      group = "Identificação e situação",
      order = 170,
      tableHidden = true,
      visibleForAuthorities = {"ERGON_READ"},
      editableForAuthorities = {"ERGON_WRITE"})
  @Size(max = 2000)
  @BulkEditable(modes = {BulkMode.UNIFORM_UPDATE, BulkMode.PER_ITEM_UPDATE}, allowClear = false)
  private String requisitos;

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
      order = 180,
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
      order = 190,
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
      order = 200,
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
  @CustomFields(resourceKey = ApiPaths.KEY_FUNCOES)
  @UISchema(label = "Campos customizados",
      helpText = "Preencha somente os campos habilitados para o órgão.",
      type = FieldDataType.JSON,
      controlType = FieldControlType.INPUT,
      icon = "extension",
      preset = UISchemaPreset.NONE,
      group = "Extensões institucionais",
      order = 210,
      tableHidden = true,
      visibleForAuthorities = {"ERGON_READ"},
      editableForAuthorities = {"ERGON_WRITE"})
  private Map<String, Object> customFields = new LinkedHashMap<>();

  @Schema(description = "Duração da vigência delimitada do catálogo.", accessMode = Schema.AccessMode.READ_ONLY, nullable = true)
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  @UISchema(label = "Dias de vigência", helpText = "Duração da vigência delimitada do catálogo.", group = "Prévia da operação",
      type = FieldDataType.NUMBER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "date_range",
      tableHidden = true, readOnly = true, editable = false)
  private Integer diasVigenciaPrevistos;
}
