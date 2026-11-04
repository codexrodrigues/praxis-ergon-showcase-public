package br.com.praxis.ergonshowcase.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.praxisplatform.uischema.bulk.BulkMode;
import br.com.praxis.ergonshowcase.domain.VinculoId;
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
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.customfields.CustomFields;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.extension.annotation.UISchemaPreset;
import jakarta.validation.constraints.*;

@Getter
@Setter
@Schema(description = "Exercícios interinos vinculados à designação titular e ao impedimento, com vigências funcional e remuneratória independentes.")
public abstract class SubstituicaoFuncaoDados {
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

  @Schema(description = "Identificação do exercício interino registrado para a designação titular.")
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

  @Schema(description = "Primeiro dia de exercício interino das atribuições do titular.")
  @DomainGovernance(kind = DomainGovernanceKind.SECURITY,
      classification = DomainClassification.INTERNAL,
      dataCategory = DomainDataCategory.OPERATIONAL)
  @UISchema(label = "Início da substituição",
      helpText = "Use a data inicial que consta no ato.",
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "event",
      preset = UISchemaPreset.DATE_RANGE,
      group = "Vigência",
      order = 30,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 3,
      visibleForAuthorities = {"ERGON_READ"},
      editableForAuthorities = {"ERGON_WRITE"})
  @NotNull
  private LocalDate dtini;

  @Schema(description = "Último dia de exercício interino, inclusive.")
  @DomainGovernance(kind = DomainGovernanceKind.SECURITY,
      classification = DomainClassification.INTERNAL,
      dataCategory = DomainDataCategory.OPERATIONAL)
  @UISchema(label = "Fim da substituição",
      helpText = "Informe o último dia do exercício interino.",
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "event",
      preset = UISchemaPreset.DATE_RANGE,
      group = "Vigência",
      order = 40,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 4,
      visibleForAuthorities = {"ERGON_READ"},
      editableForAuthorities = {"ERGON_WRITE"})
  @NotNull
  private LocalDate dtfim;

  @Schema(description = "Posição do exercício interino e do respectivo processamento em folha.", allowableValues = {"PROGRAMADA", "EM_EXERCICIO", "PENDENTE_FOLHA", "LIQUIDADA", "CANCELADA"})
  @DomainGovernance(kind = DomainGovernanceKind.SECURITY,
      classification = DomainClassification.INTERNAL,
      dataCategory = DomainDataCategory.OPERATIONAL)
  @UISchema(label = "Situação",
      helpText = "Selecione a posição cadastral documentada.",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      icon = "flag",
      preset = UISchemaPreset.ENTERPRISE_STATUS,
      options = "[{\"value\":\"PROGRAMADA\",\"label\":\"Programada\"},{\"value\":\"EM_EXERCICIO\",\"label\":\"Em exercício\"},{\"value\":\"PENDENTE_FOLHA\",\"label\":\"Pendente de folha\"},{\"value\":\"LIQUIDADA\",\"label\":\"Liquidada\"},{\"value\":\"CANCELADA\",\"label\":\"Cancelada\"}]",
      group = "Identificação e situação",
      order = 50,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 6,
      visibleForAuthorities = {"ERGON_READ"},
      editableForAuthorities = {"ERGON_WRITE"})
  @NotBlank
  @Size(max = 30)
  @Pattern(regexp = "PROGRAMADA|EM_EXERCICIO|PENDENTE_FOLHA|LIQUIDADA|CANCELADA")
  private String situacao;

  @Schema(description = "Evento precursor que identifica a função, o titular substituído e a vigência de sua designação.")
  @ResourceReference(resourceKey = ApiPaths.KEY_DESIGNACOES, valueField = "designacaoId",
      surfaceId = "detail", previewSurfaceId = "detail", detailPresentation = "drawer")
  @DomainGovernance(kind = DomainGovernanceKind.SECURITY,
      classification = DomainClassification.INTERNAL,
      dataCategory = DomainDataCategory.OPERATIONAL)
  @UISchema(label = "Designação titular",
      helpText = "Selecione o ato do titular impedido.",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "link",
      preset = UISchemaPreset.ENTERPRISE_NAME,
      endpoint = ApiPaths.DESIGNACAO_OPTIONS,
      valueField = "id",
      displayField = "label",
      group = "Vínculos e unidades",
      order = 60,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 1,
      visibleForAuthorities = {"ERGON_READ"},
      editableForAuthorities = {"ERGON_WRITE"})
  @NotNull
  private Long designacaoId;

  @Schema(description = "Vínculo do servidor que assume temporariamente as atribuições da função.")
  @ResourceReference(resourceKey = "administracao-pessoal.vinculos", valueField = "substitutoId",
      surfaceId = "resumo-funcional", previewSurfaceId = "resumo-funcional", detailPresentation = "drawer")
  @DomainGovernance(kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL, exportable = false)
  @UISchema(label = "Vínculo substituto",
      helpText = "Escolha a matrícula de quem entra em exercício.",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "link",
      preset = UISchemaPreset.ENTERPRISE_ID,
      endpoint = ApiPaths.VINCULO_OPTIONS,
      valueField = "id",
      displayField = "label",
      group = "Vínculos e unidades",
      order = 70,
      tableHidden = true,
      visibleForAuthorities = {"ERGON_READ"},
      editableForAuthorities = {"ERGON_WRITE"})
  @NotNull
  private VinculoId substitutoId;

  @Schema(description = "Natureza do impedimento do titular que originou o exercício interino.", allowableValues = {"FERIAS", "AFASTAMENTO", "AUSENCIA_EVENTUAL"})
  @DomainGovernance(kind = DomainGovernanceKind.SECURITY,
      classification = DomainClassification.INTERNAL,
      dataCategory = DomainDataCategory.OPERATIONAL)
  @UISchema(label = "Motivo do exercício",
      helpText = "Selecione o evento que justifica a substituição.",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      icon = "flag",
      preset = UISchemaPreset.ENTERPRISE_STATUS,
      options = "[{\"value\":\"FERIAS\",\"label\":\"Férias\"},{\"value\":\"AFASTAMENTO\",\"label\":\"Afastamento\"},{\"value\":\"AUSENCIA_EVENTUAL\",\"label\":\"Ausência eventual\"}]",
      group = "Identificação e situação",
      order = 80,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 2,
      visibleForAuthorities = {"ERGON_READ"},
      editableForAuthorities = {"ERGON_WRITE"})
  @NotBlank
  @Size(max = 24)
  @Pattern(regexp = "FERIAS|AFASTAMENTO|AUSENCIA_EVENTUAL")
  private String motivo;

  @Schema(description = "Evento funcional de afastamento que fundamenta o impedimento, quando esta é a origem da substituição.", nullable = true)
  @ResourceReference(resourceKey = "administracao-pessoal.afastamentos", valueField = "afastamentoId",
      surfaceId = "ficha-afastamento", previewSurfaceId = "ficha-afastamento", detailPresentation = "drawer")
  @DomainGovernance(kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL, exportable = false)
  @UISchema(label = "Afastamento do titular",
      helpText = "Vincule o afastamento registrado para o titular.",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "link",
      preset = UISchemaPreset.ENTERPRISE_ID,
      endpoint = ApiPaths.AFASTAMENTO_OPTIONS,
      valueField = "id",
      displayField = "label",
      group = "Vínculos e unidades",
      order = 90,
      tableHidden = true,
      visibleForAuthorities = {"ERGON_READ"},
      editableForAuthorities = {"ERGON_WRITE"})
  private Long afastamentoId;

  @Schema(description = "Parcela de gozo de férias do titular que fundamenta o exercício interino.", nullable = true)
  @ResourceReference(resourceKey = "administracao-pessoal.ferias-gozo", valueField = "feriasGozoId",
      surfaceId = "ficha-ferias-gozo", previewSurfaceId = "ficha-ferias-gozo", detailPresentation = "drawer")
  @DomainGovernance(kind = DomainGovernanceKind.SECURITY,
      classification = DomainClassification.INTERNAL,
      dataCategory = DomainDataCategory.OPERATIONAL)
  @UISchema(label = "Férias do titular",
      helpText = "Vincule a parcela de férias correspondente.",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "link",
      preset = UISchemaPreset.ENTERPRISE_ID,
      endpoint = ApiPaths.FERIAS_GOZO_OPTIONS,
      valueField = "id",
      displayField = "label",
      group = "Vínculos e unidades",
      order = 100,
      tableHidden = true,
      visibleForAuthorities = {"ERGON_READ"},
      editableForAuthorities = {"ERGON_WRITE"})
  private Long feriasGozoId;

  @Schema(description = "Primeiro dia remunerado do exercício interino, distinto do início funcional quando a regra de pagamento assim definir.", nullable = true)
  @DomainGovernance(kind = DomainGovernanceKind.SECURITY,
      classification = DomainClassification.INTERNAL,
      dataCategory = DomainDataCategory.OPERATIONAL)
  @UISchema(label = "Início remunerado",
      helpText = "Use o primeiro dia homologado para folha.",
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "event",
      preset = UISchemaPreset.DATE_RANGE,
      group = "Remuneração",
      order = 110,
      tableHidden = true,
      visibleForAuthorities = {"ERGON_READ"},
      editableForAuthorities = {"ERGON_WRITE"})
  private LocalDate dtiniRemunerada;

  @Schema(description = "Último dia remunerado do exercício interino, contado inclusive no período pago.", nullable = true)
  @DomainGovernance(kind = DomainGovernanceKind.SECURITY,
      classification = DomainClassification.INTERNAL,
      dataCategory = DomainDataCategory.OPERATIONAL)
  @UISchema(label = "Fim remunerado",
      helpText = "Use o último dia homologado para folha.",
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "event",
      preset = UISchemaPreset.DATE_RANGE,
      group = "Remuneração",
      order = 120,
      tableHidden = true,
      visibleForAuthorities = {"ERGON_READ"},
      editableForAuthorities = {"ERGON_WRITE"})
  private LocalDate dtfimRemunerada;

  @Schema(description = "Mês de referência em que o pagamento da substituição é processado, representado pelo primeiro dia do mês.", nullable = true)
  @DomainGovernance(kind = DomainGovernanceKind.SECURITY,
      classification = DomainClassification.INTERNAL,
      dataCategory = DomainDataCategory.OPERATIONAL)
  @UISchema(label = "Competência de folha",
      helpText = "Informe o primeiro dia da competência.",
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "event",
      preset = UISchemaPreset.DATE_RANGE,
      group = "Remuneração",
      order = 130,
      tableHidden = true,
      visibleForAuthorities = {"ERGON_READ"},
      editableForAuthorities = {"ERGON_WRITE"})
  private LocalDate competenciaFolha;

  @Schema(description = "Valor bruto apurado para a substituição no período remunerado informado.", nullable = true)
  @DomainGovernance(kind = DomainGovernanceKind.SECURITY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.FINANCIAL, exportable = false)
  @UISchema(label = "Valor apurado",
      helpText = "Informe o valor apurado para a folha.",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.CURRENCY_INPUT,
      icon = "payments",
      preset = UISchemaPreset.MONETARY_AMOUNT,
      group = "Remuneração",
      order = 140,
      tableHidden = true,
      visibleForAuthorities = {"ERGON_READ"},
      editableForAuthorities = {"ERGON_WRITE"})
  @PositiveOrZero
  @Digits(integer = 13, fraction = 2)
  private BigDecimal valorSubstituicao;

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
      order = 150,
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
      order = 160,
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
      order = 170,
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
  @CustomFields(resourceKey = ApiPaths.KEY_SUBSTITUICOES)
  @UISchema(label = "Campos customizados",
      helpText = "Preencha somente os campos habilitados para o órgão.",
      type = FieldDataType.JSON,
      controlType = FieldControlType.INPUT,
      icon = "extension",
      preset = UISchemaPreset.NONE,
      group = "Extensões institucionais",
      order = 180,
      tableHidden = true,
      visibleForAuthorities = {"ERGON_READ"},
      editableForAuthorities = {"ERGON_WRITE"})
  private Map<String, Object> customFields = new LinkedHashMap<>();

  @Schema(description = "Duração inclusiva do exercício interino proposto.", accessMode = Schema.AccessMode.READ_ONLY, nullable = true)
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  @UISchema(label = "Dias de substituição", helpText = "Duração inclusiva do exercício interino proposto.", group = "Prévia da operação",
      type = FieldDataType.NUMBER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "timer",
      tableHidden = true, readOnly = true, editable = false)
  private Integer diasSubstituicaoPrevistos;

  @Schema(description = "Mínimo de dias cadastrado na função para remuneração da substituição.", accessMode = Schema.AccessMode.READ_ONLY, nullable = true)
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  @UISchema(label = "Mínimo remunerado", helpText = "Mínimo de dias cadastrado na função para remuneração da substituição.", group = "Prévia da operação",
      type = FieldDataType.NUMBER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "timer",
      tableHidden = true, readOnly = true, editable = false)
  private Integer minimoRemuneradoPrevisto;
}
