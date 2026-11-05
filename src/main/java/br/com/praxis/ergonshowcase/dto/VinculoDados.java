package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.*;
import lombok.Getter;
import lombok.Setter;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.NumericFormat;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.customfields.CustomFields;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.extension.annotation.UISchemaPreset;

/** Campos cadastrais do vínculo; transições funcionais pertencem aos comandos próprios. */
@Getter
@Setter
public abstract class VinculoDados {
  @Size(max = 20)
  @Schema(
      description =
          "Matrícula institucional atribuída ao vínculo funcional dentro do órgão; distingue assentamentos da mesma pessoa.")
  @UISchema(
      label = "Matrícula",
      preset = UISchemaPreset.ENTERPRISE_CODE,
      group = "Identificação funcional",
      order = 10,
      icon = "badge")
  private String matric;

  @NotBlank
  @Size(max = 20)
  @Schema(
      description =
          "Regime jurídico que fundamenta a relação funcional, como estatutário ou celetista.")
  @UISchema(label = "Regime jurídico", group = "Enquadramento", order = 20, icon = "gavel")
  private String regimejur;

  @NotBlank
  @Size(max = 20)
  @Schema(
      description =
          "Modalidade do vínculo funcional, como efetivo, comissão ou temporário, usada no enquadramento estatutário.")
  @UISchema(
      label = "Tipo de vínculo",
      group = "Enquadramento",
      order = 30,
      icon = "work",
      quickViewVisible = true,
      quickViewOrder = 3)
  private String tipovinc;

  @NotBlank
  @Size(max = 20)
  @Schema(
      description =
          "Categoria funcional do assentamento, utilizada no recorte de regras de pessoal.")
  @UISchema(label = "Categoria", group = "Enquadramento", order = 40, icon = "category")
  private String categoria;

  @Schema(
      description =
          "Cargo de provimento ocupado pelo vínculo; deve pertencer ao mesmo órgão do assentamento.")
  @UISchema(
      label = "Cargo",
      controlType = org.praxisplatform.uischema.FieldControlType.ENTITY_LOOKUP,
      endpoint = ApiPaths.CARGO_OPTIONS,
      valueField = "id",
      displayField = "label",
      tableHidden = true,
      group = "Lotação",
      order = 50,
      icon = "badge",
      type = org.praxisplatform.uischema.FieldDataType.TEXT)
  @ResourceReference(
      resourceKey = "administracao-pessoal.cargos",
      valueField = "cargo",
      surfaceId = "ficha-cargo",
      previewSurfaceId = "ficha-cargo",
      detailPresentation = "drawer")
  private Integer cargo;

  @Size(max = 20)
  @Schema(
      description = "Referência ou padrão salarial vigente na tabela de vencimentos do vínculo.")
  @UISchema(label = "Referência salarial", group = "Enquadramento", order = 60, icon = "payments")
  private String referencia;

  @Positive
  @Max(168)
  @Schema(
      description =
          "Carga horária semanal contratual ou estatutária do vínculo, expressa em horas.")
  @UISchema(
      label = "Jornada semanal",
      numericFormat = NumericFormat.INTEGER,
      group = "Enquadramento",
      order = 70,
      icon = "schedule",
      quickViewVisible = true,
      quickViewOrder = 5)
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE}, allowClear = false)
  private Integer jornadaSemanal;

  @CustomFields(resourceKey = "administracao-pessoal.vinculos")
  @Schema(
      description =
          "Atributos funcionais definidos no catálogo do órgão, incluindo o processo SEI de nomeação e o regime de teletrabalho.")
  @UISchema(label = "Características do órgão", group = "Características adicionais", order = 80)
  private Map<String, Object> customFields = new LinkedHashMap<>();

  @Schema(description = "Matrícula do vínculo em sistema externo para conciliação cadastral.")
  @UISchema(
      label = "Matrícula externa",
      group = "Identificação funcional",
      tableHidden = true,
      order = 200)
  @jakarta.validation.constraints.Size(max = 20)
  private String matriculaExterna;

  @Schema(description = "Especialidade ou subcategoria funcional do vínculo.")
  @UISchema(
      label = "Subcategoria",
      group = "Enquadramento e lotação",
      tableHidden = true,
      order = 201)
  @jakarta.validation.constraints.Size(max = 40)
  private String subcategoria;

  @Schema(nullable = true, description = "Código da unidade de lotação do vínculo no mesmo órgão.")
  @UISchema(
      label = "Lotação",
      group = "Enquadramento e lotação",
      tableHidden = true,
      order = 202,
      type = org.praxisplatform.uischema.FieldDataType.TEXT,
      controlType = org.praxisplatform.uischema.FieldControlType.ENTITY_LOOKUP,
      icon = "account_tree",
      endpoint = ApiPaths.SETOR_OPTIONS,
      valueField = "id",
      displayField = "label")
  @ResourceReference(
      resourceKey = "administracao-pessoal.setores",
      valueField = "setor",
      surfaceId = "ficha-unidade",
      previewSurfaceId = "ficha-unidade",
      detailPresentation = "drawer")
  @jakarta.validation.constraints.Size(max = 30)
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  private String setor;

  @Schema(nullable = true, description = "Unidade de efetivo exercício do vínculo no mesmo órgão.")
  @UISchema(
      label = "Exercício",
      group = "Enquadramento e lotação",
      tableHidden = true,
      order = 203,
      type = org.praxisplatform.uischema.FieldDataType.TEXT,
      controlType = org.praxisplatform.uischema.FieldControlType.ENTITY_LOOKUP,
      icon = "business",
      endpoint = ApiPaths.SETOR_OPTIONS,
      valueField = "id",
      displayField = "label")
  @ResourceReference(
      resourceKey = "administracao-pessoal.setores",
      valueField = "setorExercicio",
      surfaceId = "ficha-unidade",
      previewSurfaceId = "ficha-unidade",
      detailPresentation = "drawer")
  @jakarta.validation.constraints.Size(max = 30)
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  private String setorExercicio;

  @Schema(description = "Cargo em comissão ocupado no mesmo órgão do vínculo.")
  @UISchema(
      label = "Cargo em comissão",
      group = "Enquadramento e lotação",
      tableHidden = true,
      order = 204,
      type = org.praxisplatform.uischema.FieldDataType.TEXT,
      controlType = org.praxisplatform.uischema.FieldControlType.ENTITY_LOOKUP,
      icon = "badge",
      endpoint = ApiPaths.CARGO_OPTIONS,
      valueField = "id",
      displayField = "label")
  @ResourceReference(
      resourceKey = "administracao-pessoal.cargos",
      valueField = "cargoComissao",
      surfaceId = "ficha-cargo",
      previewSurfaceId = "ficha-cargo",
      detailPresentation = "drawer")
  private Integer cargoComissao;

  @Schema(description = "Documento legal que fundamenta o assentamento funcional.")
  @UISchema(
      label = "Documento legal",
      group = "Enquadramento e lotação",
      tableHidden = true,
      order = 205,
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

  @Schema(description = "Referência à publicação oficial do ato funcional.")
  @UISchema(
      label = "Publicação do ato",
      group = "Enquadramento e lotação",
      tableHidden = true,
      order = 206,
      type = org.praxisplatform.uischema.FieldDataType.TEXT,
      controlType = org.praxisplatform.uischema.FieldControlType.ENTITY_LOOKUP,
      icon = "article",
      endpoint = ApiPaths.DOCUMENTO_OPTIONS,
      valueField = "id",
      displayField = "label")
  @ResourceReference(
      resourceKey = "administracao-pessoal.documentos-legais",
      valueField = "atoPublicacaoId",
      surfaceId = "ficha-documento",
      previewSurfaceId = "ficha-documento",
      detailPresentation = "drawer")
  private Long atoPublicacaoId;

  @Schema(description = "Data de nomeação para o provimento do vínculo.")
  @UISchema(
      label = "Nomeação",
      group = "Ingresso, concurso e estágio probatório",
      tableHidden = true,
      order = 207,
      controlType = org.praxisplatform.uischema.FieldControlType.DATE_PICKER,
      type = org.praxisplatform.uischema.FieldDataType.DATE,
      icon = "event")
  private LocalDate dtnom;

  @Schema(description = "Data de posse; deve ocorrer entre nomeação e exercício.")
  @UISchema(
      label = "Posse",
      group = "Ingresso, concurso e estágio probatório",
      tableHidden = true,
      order = 208,
      controlType = org.praxisplatform.uischema.FieldControlType.DATE_PICKER,
      type = org.praxisplatform.uischema.FieldDataType.DATE,
      icon = "event")
  private LocalDate dtposse;

  @Schema(description = "Data de admissão informada nas obrigações trabalhistas.")
  @UISchema(
      label = "Admissão RAIS",
      group = "Ingresso, concurso e estágio probatório",
      tableHidden = true,
      order = 209,
      controlType = org.praxisplatform.uischema.FieldControlType.DATE_PICKER,
      type = org.praxisplatform.uischema.FieldDataType.DATE,
      icon = "event")
  private LocalDate dtadmRais;

  @Schema(description = "Classificação obtida no concurso que fundamenta a admissão.")
  @UISchema(
      label = "Classificação no concurso",
      group = "Ingresso, concurso e estágio probatório",
      tableHidden = true,
      order = 210)
  private Integer classifconc;

  @Schema(description = "Data do concurso de ingresso do vínculo.")
  @UISchema(
      label = "Concurso",
      group = "Ingresso, concurso e estágio probatório",
      tableHidden = true,
      order = 211,
      controlType = org.praxisplatform.uischema.FieldControlType.DATE_PICKER,
      type = org.praxisplatform.uischema.FieldDataType.DATE,
      icon = "event")
  private LocalDate dtconc;

  @Schema(description = "Número do vínculo anterior na cadeia de continuidade funcional.")
  @UISchema(
      label = "Vínculo anterior",
      group = "Ingresso, concurso e estágio probatório",
      tableHidden = true,
      order = 212)
  private Integer numvincant;

  @Schema(description = "Número do vínculo posterior na cadeia de continuidade funcional.")
  @UISchema(
      label = "Vínculo posterior",
      group = "Ingresso, concurso e estágio probatório",
      tableHidden = true,
      order = 213)
  private Integer numvincpos;

  @Schema(description = "Motivo administrativo associado ao assentamento funcional.")
  @UISchema(
      label = "Motivo administrativo",
      group = "Ingresso, concurso e estágio probatório",
      tableHidden = true,
      order = 214)
  @jakarta.validation.constraints.Size(max = 2000)
  private String motivo;

  @Schema(description = "Data inicial do contrato temporário.")
  @UISchema(
      label = "Início do contrato",
      group = "Contrato temporário",
      tableHidden = true,
      order = 215,
      controlType = org.praxisplatform.uischema.FieldControlType.DATE_PICKER,
      type = org.praxisplatform.uischema.FieldDataType.DATE,
      icon = "event")
  private LocalDate dtinicontr;

  @Schema(description = "Data final originalmente contratada.")
  @UISchema(
      label = "Término do contrato",
      group = "Contrato temporário",
      tableHidden = true,
      order = 216,
      controlType = org.praxisplatform.uischema.FieldControlType.DATE_PICKER,
      type = org.praxisplatform.uischema.FieldDataType.DATE,
      icon = "event")
  private LocalDate dtfimcontr;

  @Schema(
      description = "Data final da prorrogação contratual; não pode anteceder o término original.")
  @UISchema(
      label = "Prorrogação do contrato",
      group = "Contrato temporário",
      tableHidden = true,
      order = 217,
      controlType = org.praxisplatform.uischema.FieldControlType.DATE_PICKER,
      type = org.praxisplatform.uischema.FieldDataType.DATE,
      icon = "event")
  private LocalDate dtprorrogcontr;

  @Schema(nullable = true, description = "Regime previdenciário ao qual o vínculo está filiado.")
  @UISchema(
      label = "Regime previdenciário",
      group = "Previdência, folha e FGTS",
      tableHidden = true,
      order = 218)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.FINANCIAL,
      reason = "Regime previdenciário ao qual o vínculo está filiado.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @jakarta.validation.constraints.Size(max = 30)
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  private String regimeprev;

  @Schema(nullable = true, description = "Plano previdenciário de enquadramento do vínculo.")
  @UISchema(
      label = "Plano previdenciário",
      group = "Previdência, folha e FGTS",
      tableHidden = true,
      order = 219)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.FINANCIAL,
      reason = "Plano previdenciário de enquadramento do vínculo.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @jakarta.validation.constraints.Size(max = 40)
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  private String planoprev;

  @Schema(description = "Indica incidência do desconto de imposto de renda na folha do vínculo.")
  @UISchema(
      label = "Desconta IR",
      group = "Previdência, folha e FGTS",
      tableHidden = true,
      order = 220)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.FINANCIAL,
      reason = "Indica incidência do desconto de imposto de renda na folha do vínculo.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE}, allowClear = false)
  private Boolean descontaIr = false;

  @Schema(description = "Indica opção de envio de comunicação relativa ao vínculo.")
  @UISchema(
      label = "Comunicação por correio",
      group = "Previdência, folha e FGTS",
      tableHidden = true,
      order = 221)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.FINANCIAL,
      reason = "Indica opção de envio de comunicação relativa ao vínculo.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE}, allowClear = false)
  private Boolean correio = false;

  @Schema(description = "Indica enquadramento do vínculo em informação de cota.")
  @UISchema(
      label = "Informação de cota",
      group = "Previdência, folha e FGTS",
      tableHidden = true,
      order = 222)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.FINANCIAL,
      reason = "Indica enquadramento do vínculo em informação de cota.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private Boolean infoCota = false;

  @Schema(nullable = true, description = "Banco destinatário do pagamento deste vínculo.")
  @UISchema(
      label = "Banco",
      group = "Previdência, folha e FGTS",
      tableHidden = true,
      order = 223,
      visibleForAuthorities = {"ERGON_READ", "ERGON_WRITE", "ERGON_FINANCIAL_READ"},
      editableForAuthorities = {"ERGON_WRITE"},
      fieldAccessReason = "Dado bancário de pagamento do vínculo governado para perfis autorizados.")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.FINANCIAL,
      complianceTags = {"LGPD", "FINANCIAL_DATA", "NON_EXPORTABLE"},
      reason = "Banco destinatário do pagamento deste vínculo.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  private Integer banco;

  @Schema(nullable = true, description = "Agência bancária para pagamento deste vínculo.")
  @UISchema(
      label = "Agência",
      group = "Previdência, folha e FGTS",
      tableHidden = true,
      order = 224,
      visibleForAuthorities = {"ERGON_READ", "ERGON_WRITE", "ERGON_FINANCIAL_READ"},
      editableForAuthorities = {"ERGON_WRITE"},
      fieldAccessReason = "Agência bancária de pagamento do vínculo governada para perfis autorizados.")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.FINANCIAL,
      complianceTags = {"LGPD", "FINANCIAL_DATA", "NON_EXPORTABLE"},
      reason = "Agência bancária para pagamento deste vínculo.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @jakarta.validation.constraints.Size(max = 10)
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  private String agencia;

  @Schema(description = "Conta bancária para pagamento deste vínculo.")
  @UISchema(
      label = "Conta",
      group = "Previdência, folha e FGTS",
      tableHidden = true,
      order = 225,
      visibleForAuthorities = {"ERGON_READ", "ERGON_WRITE", "ERGON_FINANCIAL_READ"},
      editableForAuthorities = {"ERGON_WRITE"},
      fieldAccessReason = "Conta bancária de pagamento do vínculo protegida contra exportação em massa.")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.FINANCIAL,
      complianceTags = {"LGPD", "FINANCIAL_DATA", "NON_EXPORTABLE"},
      reason = "Conta bancária para pagamento deste vínculo.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @jakarta.validation.constraints.Size(max = 25)
  private String conta;

  @Schema(nullable = true, description = "Modalidade de pagamento deste vínculo.")
  @UISchema(
      label = "Tipo de pagamento",
      group = "Previdência, folha e FGTS",
      tableHidden = true,
      order = 226)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.FINANCIAL,
      reason = "Modalidade de pagamento deste vínculo.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @jakarta.validation.constraints.Size(max = 10)
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  private String tipopag;

  @Schema(description = "Data de opção pelo regime do FGTS.")
  @UISchema(
      label = "Opção FGTS",
      group = "Previdência, folha e FGTS",
      tableHidden = true,
      order = 227,
      controlType = org.praxisplatform.uischema.FieldControlType.DATE_PICKER,
      type = org.praxisplatform.uischema.FieldDataType.DATE,
      icon = "event")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.FINANCIAL,
      reason = "Data de opção pelo regime do FGTS.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private LocalDate dtopfgts;

  @Schema(description = "Data de retroação da opção pelo FGTS.")
  @UISchema(
      label = "Retroação FGTS",
      group = "Previdência, folha e FGTS",
      tableHidden = true,
      order = 228,
      controlType = org.praxisplatform.uischema.FieldControlType.DATE_PICKER,
      type = org.praxisplatform.uischema.FieldDataType.DATE,
      icon = "event")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.FINANCIAL,
      reason = "Data de retroação da opção pelo FGTS.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private LocalDate dtretrfgts;

  @Schema(description = "Banco depositário da conta vinculada de FGTS.")
  @UISchema(
      label = "Banco FGTS",
      group = "Previdência, folha e FGTS",
      tableHidden = true,
      order = 229)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.FINANCIAL,
      reason = "Banco depositário da conta vinculada de FGTS.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private Integer bancofgts;

  @Schema(description = "Agência da conta vinculada do FGTS.")
  @UISchema(
      label = "Agência FGTS",
      group = "Previdência, folha e FGTS",
      tableHidden = true,
      order = 230)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.FINANCIAL,
      reason = "Agência da conta vinculada do FGTS.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @jakarta.validation.constraints.Size(max = 10)
  private String agenciafgts;

  @Schema(description = "Número da conta vinculada do FGTS.")
  @UISchema(
      label = "Conta FGTS",
      group = "Previdência, folha e FGTS",
      tableHidden = true,
      order = 231)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.FINANCIAL,
      reason = "Número da conta vinculada do FGTS.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @jakarta.validation.constraints.Size(max = 25)
  private String contafgts;

  @Schema(description = "Valor monetário de depósito do FGTS registrado no assentamento.")
  @UISchema(
      label = "Depósito FGTS",
      group = "Previdência, folha e FGTS",
      tableHidden = true,
      order = 232,
      type = org.praxisplatform.uischema.FieldDataType.NUMBER,
      controlType = org.praxisplatform.uischema.FieldControlType.CURRENCY_INPUT,
      icon = "payments")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.FINANCIAL,
      reason = "Valor monetário de depósito do FGTS registrado no assentamento.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private java.math.BigDecimal valorDepFgts;

  @jakarta.validation.constraints.Null(
      message = "Prévia derivada não pode ser informada no submit.")
  @Schema(
      description =
          "Denominação consultada para o cargo escolhido no formulário; não é gravada no assentamento.")
  @UISchema(
      label = "Cargo selecionado",
      group = "Enquadramento e lotação",
      readOnly = true,
      tableHidden = true,
      icon = "info")
  @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
  private String cargoNomePrevisto;

  @jakarta.validation.constraints.Null(
      message = "Prévia derivada não pode ser informada no submit.")
  @Schema(
      description =
          "Saldo informativo do cargo selecionado, sem reserva de vaga; o submit verifica novamente a ocupação sob bloqueio.")
  @UISchema(
      label = "Saldo atual de vagas",
      group = "Enquadramento e lotação",
      readOnly = true,
      tableHidden = true,
      icon = "info",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX)
  @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
  private Long saldoVagasPrevisto;
}
