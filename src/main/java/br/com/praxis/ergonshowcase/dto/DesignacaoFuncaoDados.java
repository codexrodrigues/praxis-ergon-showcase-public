package br.com.praxis.ergonshowcase.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
@Schema(description = "Atos individuais de designação, exercício e dispensa em função de confiança, incluindo opção remuneratória e ordem de substitutos.")
public abstract class DesignacaoFuncaoDados {
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

  @Schema(description = "Identificação do ato individual que atribui a função de confiança ao servidor.")
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
  @BulkEditable
  private String descricao;

  @Schema(description = "Data de início dos efeitos do ato de designação, independente da entrada em exercício.")
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
      visibleForAuthorities = {"ERGON_READ"},
      editableForAuthorities = {"ERGON_WRITE"})
  @NotNull
  private LocalDate dtini;

  @Schema(description = "Último dia abrangido pela designação, inclusive, quando houver dispensa ou prazo determinado.", nullable = true)
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
      visibleForAuthorities = {"ERGON_READ"},
      editableForAuthorities = {"ERGON_WRITE"})
  private LocalDate dtfim;

  @Schema(description = "Posição do ato entre designação publicada, exercício, suspensão e dispensa.", allowableValues = {"DESIGNADA", "EM_EXERCICIO", "SUSPENSA", "DISPENSADA"})
  @DomainGovernance(kind = DomainGovernanceKind.SECURITY,
      classification = DomainClassification.INTERNAL,
      dataCategory = DomainDataCategory.OPERATIONAL)
  @UISchema(label = "Situação",
      helpText = "Selecione a posição cadastral documentada.",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      icon = "flag",
      preset = UISchemaPreset.ENTERPRISE_STATUS,
      options = "[{\"value\":\"DESIGNADA\",\"label\":\"Designada\"},{\"value\":\"EM_EXERCICIO\",\"label\":\"Em exercício\"},{\"value\":\"SUSPENSA\",\"label\":\"Suspensa\"},{\"value\":\"DISPENSADA\",\"label\":\"Dispensada\"}]",
      group = "Identificação e situação",
      order = 50,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 4,
      visibleForAuthorities = {"ERGON_READ"},
      editableForAuthorities = {"ERGON_WRITE"})
  @NotBlank
  @Size(max = 30)
  @Pattern(regexp = "DESIGNADA|EM_EXERCICIO|SUSPENSA|DISPENSADA")
  private String situacao;

  @Schema(description = "Vínculo em que o servidor recebeu o ato de designação para a função.")
  @ResourceReference(resourceKey = "administracao-pessoal.vinculos", valueField = "titularId",
      surfaceId = "resumo-funcional", previewSurfaceId = "resumo-funcional", detailPresentation = "drawer")
  @DomainGovernance(kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL, exportable = false)
  @UISchema(label = "Vínculo do titular",
      helpText = "Escolha a matrícula e o vínculo do titular.",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "link",
      preset = UISchemaPreset.ENTERPRISE_ID,
      endpoint = ApiPaths.VINCULO_OPTIONS,
      valueField = "id",
      displayField = "label",
      group = "Vínculos e unidades",
      order = 60,
      tableHidden = true,
      visibleForAuthorities = {"ERGON_READ"},
      editableForAuthorities = {"ERGON_WRITE"})
  @NotNull
  private VinculoId titularId;

  @Schema(description = "Função de confiança objeto do ato de designação.")
  @ResourceReference(resourceKey = ApiPaths.KEY_FUNCOES, valueField = "funcaoId",
      surfaceId = "detail", previewSurfaceId = "detail", detailPresentation = "drawer")
  @DomainGovernance(kind = DomainGovernanceKind.SECURITY,
      classification = DomainClassification.INTERNAL,
      dataCategory = DomainDataCategory.OPERATIONAL)
  @UISchema(label = "Função designada",
      helpText = "Selecione a função indicada no ato.",
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

  @Schema(description = "Posição numerada ocupada pelo ato quando a função usa controle de vagas.", nullable = true)
  @ResourceReference(resourceKey = ApiPaths.KEY_VAGAS_FUNCAO, valueField = "vagaFuncaoId",
      surfaceId = "detail", previewSurfaceId = "detail", detailPresentation = "drawer")
  @DomainGovernance(kind = DomainGovernanceKind.SECURITY,
      classification = DomainClassification.INTERNAL,
      dataCategory = DomainDataCategory.OPERATIONAL)
  @UISchema(label = "Vaga vinculada",
      helpText = "Filtre pela função e selecione a vaga do ato.",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "link",
      preset = UISchemaPreset.ENTERPRISE_ID,
      endpoint = ApiPaths.VAGA_FUNCAO_OPTIONS,
      valueField = "id",
      displayField = "label",
      group = "Vínculos e unidades",
      order = 80,
      tableHidden = true,
      quickViewVisible = true,
      quickViewOrder = 2,
      visibleForAuthorities = {"ERGON_READ"},
      editableForAuthorities = {"ERGON_WRITE"})
  private Long vagaFuncaoId;

  @Schema(description = "Setor onde o titular responde pelas atribuições da função designada.")
  @ResourceReference(resourceKey = "administracao-pessoal.setores", valueField = "setorId",
      surfaceId = "ficha-unidade", previewSurfaceId = "ficha-unidade", detailPresentation = "drawer")
  @DomainGovernance(kind = DomainGovernanceKind.SECURITY,
      classification = DomainClassification.INTERNAL,
      dataCategory = DomainDataCategory.OPERATIONAL)
  @UISchema(label = "Unidade de exercício",
      helpText = "Selecione a unidade que recebe o titular.",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "link",
      preset = UISchemaPreset.ENTERPRISE_NAME,
      endpoint = ApiPaths.SETOR_OPTIONS,
      valueField = "id",
      displayField = "label",
      group = "Vínculos e unidades",
      order = 90,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 3,
      visibleForAuthorities = {"ERGON_READ"},
      editableForAuthorities = {"ERGON_WRITE"})
  @NotBlank
  @Size(max = 30)
  @BulkEditable
  private String setorId;

  @Schema(description = "Primeiro dia de efetivo exercício na função, que pode ser posterior à publicação da designação.", nullable = true)
  @DomainGovernance(kind = DomainGovernanceKind.SECURITY,
      classification = DomainClassification.INTERNAL,
      dataCategory = DomainDataCategory.OPERATIONAL)
  @UISchema(label = "Início do exercício",
      helpText = "Informe a data do registro de entrada em exercício.",
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "event",
      preset = UISchemaPreset.DATE_RANGE,
      group = "Vigência",
      order = 100,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 5,
      visibleForAuthorities = {"ERGON_READ"},
      editableForAuthorities = {"ERGON_WRITE"})
  private LocalDate dtExercicio;

  @Schema(description = "Primeiro vínculo indicado no ato para responder durante impedimento do titular.", nullable = true)
  @ResourceReference(resourceKey = "administracao-pessoal.vinculos", valueField = "substituto1Id",
      surfaceId = "resumo-funcional", previewSurfaceId = "resumo-funcional", detailPresentation = "drawer")
  @DomainGovernance(kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL, exportable = false)
  @UISchema(label = "Primeiro substituto",
      helpText = "Selecione o vínculo com precedência na substituição.",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "link",
      preset = UISchemaPreset.ENTERPRISE_ID,
      endpoint = ApiPaths.VINCULO_OPTIONS,
      valueField = "id",
      displayField = "label",
      group = "Vínculos e unidades",
      order = 110,
      tableHidden = true,
      quickViewVisible = true,
      quickViewOrder = 6,
      visibleForAuthorities = {"ERGON_READ"},
      editableForAuthorities = {"ERGON_WRITE"})
  private VinculoId substituto1Id;

  @Schema(description = "Vínculo indicado em segunda ordem para responder na ausência do titular e do primeiro substituto.", nullable = true)
  @ResourceReference(resourceKey = "administracao-pessoal.vinculos", valueField = "substituto2Id",
      surfaceId = "resumo-funcional", previewSurfaceId = "resumo-funcional", detailPresentation = "drawer")
  @DomainGovernance(kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL, exportable = false)
  @UISchema(label = "Segundo substituto",
      helpText = "Selecione o vínculo de segunda ordem.",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "link",
      preset = UISchemaPreset.ENTERPRISE_ID,
      endpoint = ApiPaths.VINCULO_OPTIONS,
      valueField = "id",
      displayField = "label",
      group = "Vínculos e unidades",
      order = 120,
      tableHidden = true,
      quickViewVisible = true,
      quickViewOrder = 7,
      visibleForAuthorities = {"ERGON_READ"},
      editableForAuthorities = {"ERGON_WRITE"})
  private VinculoId substituto2Id;

  @Schema(description = "Escolha registrada entre remuneração integral da função e remuneração efetiva acrescida da opção percentual.", allowableValues = {"INTEGRAL", "VINCULO_EFETIVO"})
  @DomainGovernance(kind = DomainGovernanceKind.SECURITY,
      classification = DomainClassification.INTERNAL,
      dataCategory = DomainDataCategory.OPERATIONAL)
  @UISchema(label = "Opção remuneratória",
      helpText = "Registre a escolha formal do servidor.",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      icon = "flag",
      preset = UISchemaPreset.ENTERPRISE_STATUS,
      options = "[{\"value\":\"INTEGRAL\",\"label\":\"Integral da função\"},{\"value\":\"VINCULO_EFETIVO\",\"label\":\"Opção pelo efetivo\"}]",
      group = "Remuneração",
      order = 130,
      tableHidden = true,
      visibleForAuthorities = {"ERGON_READ"},
      editableForAuthorities = {"ERGON_WRITE"})
  @NotBlank
  @Size(max = 24)
  @Pattern(regexp = "INTEGRAL|VINCULO_EFETIVO")
  @BulkEditable
  private String opcaoRemuneracao;

  @Schema(description = "Valor mensal registrado para a função conforme a opção remuneratória do titular.", nullable = true)
  @DomainGovernance(kind = DomainGovernanceKind.SECURITY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.FINANCIAL, exportable = false)
  @UISchema(label = "Remuneração registrada",
      helpText = "Confira o valor que consta no assentamento.",
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
  private BigDecimal valorRemuneracao;

  @Schema(description = "Documento publicado que formaliza a dispensa do titular da função.", nullable = true)
  @ResourceReference(resourceKey = "administracao-pessoal.documentos-legais", valueField = "atoDispensaId",
      surfaceId = "ficha-documento", previewSurfaceId = "ficha-documento", detailPresentation = "drawer")
  @DomainGovernance(kind = DomainGovernanceKind.SECURITY,
      classification = DomainClassification.INTERNAL,
      dataCategory = DomainDataCategory.LEGAL)
  @UISchema(label = "Ato de dispensa",
      helpText = "Selecione o documento de encerramento.",
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
  private Long atoDispensaId;

  @Schema(description = "Fundamento administrativo registrado para a cessação da designação.", nullable = true)
  @DomainGovernance(kind = DomainGovernanceKind.SECURITY,
      classification = DomainClassification.INTERNAL,
      dataCategory = DomainDataCategory.OPERATIONAL)
  @UISchema(label = "Motivo da dispensa",
      helpText = "Use o fundamento do ato de dispensa.",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "notes",
      preset = UISchemaPreset.NONE,
      group = "Identificação e situação",
      order = 160,
      tableHidden = true,
      visibleForAuthorities = {"ERGON_READ"},
      editableForAuthorities = {"ERGON_WRITE"})
  @Size(max = 500)
  private String motivoDispensa;

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
      order = 170,
      tableHidden = true,
      visibleForAuthorities = {"ERGON_READ"},
      editableForAuthorities = {"ERGON_WRITE"})
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
      order = 180,
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
      order = 190,
      tableHidden = true,
      visibleForAuthorities = {"ERGON_READ"},
      editableForAuthorities = {"ERGON_WRITE"})
  @Size(max = 2000)
  @BulkEditable(allowClear = true)
  private String observacoes;

  @Schema(description = "Atributos institucionais definidos pelo órgão para este tipo de registro e armazenados em JSON.", nullable = true)
  @DomainGovernance(kind = DomainGovernanceKind.SECURITY,
      classification = DomainClassification.INTERNAL,
      dataCategory = DomainDataCategory.OPERATIONAL)
  @CustomFields(resourceKey = ApiPaths.KEY_DESIGNACOES)
  @UISchema(label = "Campos customizados",
      helpText = "Preencha somente os campos habilitados para o órgão.",
      type = FieldDataType.JSON,
      controlType = FieldControlType.INPUT,
      icon = "extension",
      preset = UISchemaPreset.NONE,
      group = "Extensões institucionais",
      order = 200,
      tableHidden = true,
      visibleForAuthorities = {"ERGON_READ"},
      editableForAuthorities = {"ERGON_WRITE"})
  private Map<String, Object> customFields = new LinkedHashMap<>();

  @Schema(description = "Situação corrente da vaga selecionada no momento da consulta.", accessMode = Schema.AccessMode.READ_ONLY, nullable = true)
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  @UISchema(label = "Situação da vaga", helpText = "Situação corrente da vaga selecionada no momento da consulta.", group = "Prévia da operação",
      type = FieldDataType.TEXT, controlType = FieldControlType.INPUT, icon = "event_seat",
      tableHidden = true, readOnly = true, editable = false)
  private String situacaoVagaPrevista;
}
