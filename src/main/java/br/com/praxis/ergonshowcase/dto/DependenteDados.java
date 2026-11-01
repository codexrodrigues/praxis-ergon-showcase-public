package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.*;
import lombok.Getter;
import lombok.Setter;
import org.praxisplatform.uischema.*;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.bulk.BulkMode;
import org.praxisplatform.uischema.customfields.CustomFields;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.extension.annotation.UISchemaPreset;

/** Dados cadastrais de dependentes vinculados ao servidor, compartilhados entre leitura e escrita. */
@Getter
@Setter
public abstract class DependenteDados {
  @NotBlank
  @Size(max = 300)
  @Schema(
      description =
          "Nome civil completo do dependente registrado no assentamento funcional do servidor.")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Identificação civil do dependente do servidor.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @UISchema(
      label = "Nome do dependente",
      preset = UISchemaPreset.ENTERPRISE_NAME,
      group = "Identificação civil",
      order = 10,
      icon = "person",
      tableHidden = false)
  @ResourceReference(
      resourceKey = "administracao-pessoal.dependentes",
      valueField = "id",
      surfaceId = "ficha-dependente",
      previewSurfaceId = "ficha-dependente",
      detailPresentation = "drawer")
  private String nome;

  @Size(max = 70)
  @Schema(
      nullable = true,
      description = "Nome social do dependente utilizado no atendimento administrativo e assistencial.")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Identificação social do dependente.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @UISchema(
      label = "Nome social",
      group = "Identificação civil",
      order = 20,
      icon = "person_outline",
      tableHidden = true)
  @BulkEditable(modes = {BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  private String nomeSocial;

  @Pattern(regexp = "[FM]")
  @Size(max = 1)
  @Schema(
      nullable = true,
      description = "Sexo registral do dependente no cadastro civil ([F]eminino ou [M]asculino).")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Dado biográfico individual do dependente.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @UISchema(
      label = "Sexo",
      group = "Identificação civil",
      order = 30,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      options =
          "[{\"value\":\"F\",\"label\":\"Feminino (F)\"},{\"value\":\"M\",\"label\":\"Masculino (M)\"}]",
      icon = "wc",
      tableHidden = true)
  @BulkEditable(modes = {BulkMode.UNIFORM_UPDATE, BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  private String sexo;

  @NotNull
  @Past
  @Schema(
      description =
          "Data de nascimento civil do dependente, base para apuração de limite etário de IRRF e salário-família.")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Data de nascimento individual do dependente.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @UISchema(
      label = "Nascimento",
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      group = "Identificação civil",
      order = 40,
      icon = "cake",
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 1)
  private LocalDate dtnasc;

  @NotBlank
  @Size(max = 20)
  @Schema(
      description =
          "Grau de parentesco ou vínculo civil com o servidor titular (ex.: FILHO, CONJUGE, ENTEADO, PAI/MAE).")
  @UISchema(
      label = "Parentesco",
      group = "Enquadramento da dependência",
      order = 50,
      icon = "family_restroom",
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 2)
  @BulkEditable(modes = {BulkMode.UNIFORM_UPDATE, BulkMode.PER_ITEM_UPDATE}, allowClear = false)
  private String parentesco;

  @Size(max = 20)
  @Schema(
      nullable = true,
      description =
          "Classificação legal do tipo de dependência no catálogo TIPO_DEPEN para enquadramento tributário e previdenciário.")
  @UISchema(
      label = "Tipo de dependência",
      group = "Enquadramento da dependência",
      order = 60,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "category",
      endpoint = ApiPaths.TIPO_DEPENDENTE_OPTIONS,
      valueField = "id",
      displayField = "label",
      tableHidden = true,
      quickViewVisible = true,
      quickViewOrder = 3)
  @ResourceReference(
      resourceKey = "administracao-pessoal.tipos-dependente",
      valueField = "tipodepen",
      surfaceId = "ficha-tipo-dependente",
      previewSurfaceId = "ficha-tipo-dependente",
      detailPresentation = "drawer")
  @BulkEditable(modes = {BulkMode.UNIFORM_UPDATE, BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  private String tipodepen;

  @Pattern(regexp = "[0-9]{11}")
  @Size(max = 11)
  @Schema(
      nullable = true,
      description =
          "CPF do dependente com onze dígitos numéricos sem formatação, obrigatório para dedução de IRRF e envio ao eSocial.")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      complianceTags = {"LGPD", "NON_EXPORTABLE"},
      reason = "Identificação fiscal individual do dependente protegida contra exportação em massa.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @UISchema(
      label = "CPF do dependente",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.CPF_CNPJ_INPUT,
      group = "Documentação civil",
      order = 70,
      icon = "badge",
      tableHidden = true,
      visibleForAuthorities = {"ERGON_READ", "ERGON_WRITE", "ERGON_PII_READ"},
      editableForAuthorities = {"ERGON_WRITE"},
      fieldAccessReason = "Dado fiscal pessoal de dependente (LGPD) governado para inspeção individual.")
  @BulkEditable(modes = {BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  private String cpf;

  @Size(max = 24)
  @Schema(
      nullable = true,
      description = "Número da cédula de identidade civil (RG) do dependente.")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Documento civil de identificação do dependente.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @UISchema(
      label = "Número do RG",
      group = "Documentação civil",
      order = 80,
      icon = "credit_card",
      tableHidden = true)
  @BulkEditable(modes = {BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  private String numrg;

  @Size(max = 20)
  @Schema(
      nullable = true,
      description = "Espécie do documento de identidade civil apresentado pelo dependente.")
  @UISchema(
      label = "Tipo de RG",
      group = "Documentação civil",
      order = 90,
      icon = "badge",
      tableHidden = true)
  @BulkEditable(modes = {BulkMode.UNIFORM_UPDATE, BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  private String tiporg;

  @Size(max = 20)
  @Schema(
      nullable = true,
      description = "Órgão expedidor da cédula de identidade do dependente (ex.: SSP, DETRAN, IFP).")
  @UISchema(
      label = "Órgão emissor do RG",
      group = "Documentação civil",
      order = 100,
      icon = "account_balance",
      tableHidden = true)
  @BulkEditable(modes = {BulkMode.UNIFORM_UPDATE, BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  private String orgaorg;

  @Size(max = 2)
  @Schema(
      nullable = true,
      description = "Sigla da Unidade da Federação emissora da cédula de identidade do dependente.")
  @UISchema(
      label = "UF do RG",
      group = "Documentação civil",
      order = 110,
      icon = "map",
      tableHidden = true)
  @BulkEditable(modes = {BulkMode.UNIFORM_UPDATE, BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  private String ufrg;

  @Schema(
      nullable = true,
      description = "Data de expedição do documento de identidade (RG) do dependente.")
  @UISchema(
      label = "Expedição do RG",
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      group = "Documentação civil",
      order = 120,
      icon = "event",
      tableHidden = true)
  @BulkEditable(modes = {BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  private LocalDate expedrg;

  @NotNull
  @Schema(
      description =
          "Indica se o dependente está habilitado para dedução legal de Imposto de Renda Retido na Fonte (IRRF).")
  @UISchema(
      label = "Deduz IRRF",
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      group = "Elegibilidade e benefícios",
      order = 130,
      icon = "receipt_long",
      labelTrue = "Sim",
      labelFalse = "Não",
      toneTrue = "success",
      toneFalse = "neutral",
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 4)
  @BulkEditable(modes = {BulkMode.UNIFORM_UPDATE, BulkMode.PER_ITEM_UPDATE}, allowClear = false)
  private Boolean depIr = true;

  @NotNull
  @Schema(
      description =
          "Indica se o dependente gera direito à percepção de cota de salário-família na folha de pagamento.")
  @UISchema(
      label = "Salário-família",
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      group = "Elegibilidade e benefícios",
      order = 140,
      icon = "savings",
      labelTrue = "Sim",
      labelFalse = "Não",
      toneTrue = "info",
      toneFalse = "neutral",
      tableHidden = true,
      quickViewVisible = true,
      quickViewOrder = 5)
  @BulkEditable(modes = {BulkMode.UNIFORM_UPDATE, BulkMode.PER_ITEM_UPDATE}, allowClear = false)
  private Boolean depSalarioFamilia = false;

  @NotNull
  @Schema(
      description =
          "Indica se o dependente é beneficiário ativo de assistência à saúde suplementar vinculada ao órgão.")
  @UISchema(
      label = "Plano de saúde",
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      group = "Elegibilidade e benefícios",
      order = 150,
      icon = "health_and_safety",
      labelTrue = "Ativo",
      labelFalse = "Inativo",
      toneTrue = "success",
      toneFalse = "neutral",
      tableHidden = true,
      quickViewVisible = true,
      quickViewOrder = 6)
  @BulkEditable(modes = {BulkMode.UNIFORM_UPDATE, BulkMode.PER_ITEM_UPDATE}, allowClear = false)
  private Boolean depPlanoSaude = false;

  @NotNull
  @Schema(
      description =
          "Indica se o dependente possui invalidez permanente atestada por junta médica oficial para isenção de teto etário.")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.RESTRICTED,
      dataCategory = DomainDataCategory.SENSITIVE_PERSONAL,
      reason = "Condição de saúde e invalidez atestada por junta médica.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @UISchema(
      label = "Invalidez atestada",
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      group = "Elegibilidade e benefícios",
      order = 160,
      icon = "medical_information",
      tableHidden = true)
  private Boolean invalido = false;

  @NotNull
  @Schema(
      description =
          "Indica se o dependente cursa ensino regular comprovado para manutenção de benefícios vinculados à escolaridade.")
  @UISchema(
      label = "Estudante regular",
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      group = "Elegibilidade e benefícios",
      order = 170,
      icon = "school",
      tableHidden = true)
  @BulkEditable(modes = {BulkMode.UNIFORM_UPDATE, BulkMode.PER_ITEM_UPDATE}, allowClear = false)
  private Boolean estudante = false;

  @NotNull
  @Schema(
      description =
          "Indica se o dependente cursa ensino superior ou escola técnica de 2º grau (extensão de dependência de IRRF até 24 anos).")
  @UISchema(
      label = "Universitário",
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      group = "Elegibilidade e benefícios",
      order = 180,
      icon = "account_balance",
      tableHidden = true)
  @BulkEditable(modes = {BulkMode.UNIFORM_UPDATE, BulkMode.PER_ITEM_UPDATE}, allowClear = false)
  private Boolean universitario = false;

  @NotNull
  @Schema(
      description =
          "Indica condição excepcional para concessão de benefício assistencial estatutário específico.")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.RESTRICTED,
      dataCategory = DomainDataCategory.SENSITIVE_PERSONAL,
      reason = "Condição clínica excepcional para benefício assistencial.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @UISchema(
      label = "Condição excepcional",
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      group = "Elegibilidade e benefícios",
      order = 190,
      icon = "accessible",
      tableHidden = true)
  @BulkEditable(modes = {BulkMode.UNIFORM_UPDATE, BulkMode.PER_ITEM_UPDATE}, allowClear = false)
  private Boolean excepcional = false;

  @NotNull
  @Schema(
      description =
          "Indica se o registro envolve curatela, tutela ou representação legal formalmente designada.")
  @UISchema(
      label = "Representante legal",
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      group = "Elegibilidade e benefícios",
      order = 200,
      icon = "gavel",
      tableHidden = true)
  @BulkEditable(modes = {BulkMode.UNIFORM_UPDATE, BulkMode.PER_ITEM_UPDATE}, allowClear = false)
  private Boolean representanteLegal = false;

  @Size(max = 30)
  @Schema(
      nullable = true,
      description =
          "Espécie da certidão de registro civil apresentada (ex.: NASCIMENTO, CASAMENTO, GUARDA JUDICIAL).")
  @UISchema(
      label = "Tipo de certidão",
      group = "Registro civil",
      order = 210,
      icon = "article",
      tableHidden = true)
  @BulkEditable(modes = {BulkMode.UNIFORM_UPDATE, BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  private String tipodocCert;

  @Size(max = 32)
  @Schema(
      nullable = true,
      description = "Número do termo da certidão civil lavrada em cartório.")
  @UISchema(
      label = "Termo da certidão",
      group = "Registro civil",
      order = 220,
      icon = "tag",
      tableHidden = true)
  @BulkEditable(modes = {BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  private String numCert;

  @Size(max = 15)
  @Schema(
      nullable = true,
      description = "Número ou código do livro de registro da certidão civil.")
  @UISchema(
      label = "Livro da certidão",
      group = "Registro civil",
      order = 230,
      icon = "menu_book",
      tableHidden = true)
  @BulkEditable(modes = {BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  private String livroACert;

  @Size(max = 15)
  @Schema(
      nullable = true,
      description = "Número da folha do livro de registro da certidão civil.")
  @UISchema(
      label = "Folha da certidão",
      group = "Registro civil",
      order = 240,
      icon = "description",
      tableHidden = true)
  @BulkEditable(modes = {BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  private String folhaCert;

  @Size(max = 100)
  @Schema(
      nullable = true,
      description = "Denominação oficial do cartório de registro civil emissor da certidão.")
  @UISchema(
      label = "Cartório de registro",
      group = "Registro civil",
      order = 250,
      icon = "assured_workload",
      tableHidden = true)
  @BulkEditable(modes = {BulkMode.UNIFORM_UPDATE, BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  private String cartorioCert;

  @Size(max = 2)
  @Schema(
      nullable = true,
      description = "Sigla da Unidade da Federação do cartório de registro civil.")
  @UISchema(
      label = "UF do cartório",
      group = "Registro civil",
      order = 260,
      icon = "map",
      tableHidden = true)
  @BulkEditable(modes = {BulkMode.UNIFORM_UPDATE, BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  private String ufCart;

  @Size(max = 60)
  @Schema(
      nullable = true,
      description = "Município de localização do cartório de registro civil.")
  @UISchema(
      label = "Município do cartório",
      group = "Registro civil",
      order = 270,
      icon = "location_city",
      tableHidden = true)
  @BulkEditable(modes = {BulkMode.UNIFORM_UPDATE, BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  private String municipioCart;

  @Size(max = 40)
  @Schema(
      nullable = true,
      description = "Matrícula nacional unificada de 32 dígitos da certidão de registro civil.")
  @UISchema(
      label = "Matrícula da certidão",
      group = "Registro civil",
      order = 280,
      icon = "pin",
      tableHidden = true)
  @BulkEditable(modes = {BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  private String matriculaCert;

  @Schema(
      nullable = true,
      description =
          "Referência ao documento comprobatório ou ato legal associado ao assentamento do dependente (substitui PONTPUBL/PONTARQS).")
  @UISchema(
      label = "Documento comprobatório",
      group = "Documentação comprobatória",
      order = 290,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "description",
      endpoint = ApiPaths.DOCUMENTO_OPTIONS,
      valueField = "id",
      displayField = "label",
      tableHidden = true)
  @ResourceReference(
      resourceKey = "administracao-pessoal.documentos-legais",
      valueField = "documentoLegalId",
      surfaceId = "ficha-documento",
      previewSurfaceId = "ficha-documento",
      detailPresentation = "drawer")
  @BulkEditable(modes = {BulkMode.UNIFORM_UPDATE, BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  private Long documentoLegalId;

  @NotNull
  @Schema(
      description =
          "Atributos complementares de dependentes definidos por órgão e governados pelo catálogo de campos customizados.")
  @UISchema(
      label = "Campos complementares",
      group = "Extensões do órgão",
      order = 900,
      tableHidden = true)
  @CustomFields(resourceKey = "administracao-pessoal.dependentes")
  private Map<String, Object> customFields = new LinkedHashMap<>();

  @Null
  @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
  @Schema(
      description =
          "Descrição oficial da modalidade de dependência projetada em rascunho; não pode ser enviada pelo cliente.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Modalidade prevista",
      group = "Enquadramento da dependência",
      order = 61,
      readOnly = true,
      tableHidden = true)
  private String tipoDependenteDescricaoPrevista;

  @Null
  @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
  @Schema(
      description =
          "Idade atual do dependente em anos completos projetada em rascunho; não pode ser enviada pelo cliente.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Idade calculada (rascunho)",
      group = "Elegibilidade e benefícios",
      order = 131,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.INPUT,
      icon = "cake",
      readOnly = true,
      tableHidden = true)
  private Integer idadeCalculadaAnos;

  @Null
  @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
  @Schema(
      description =
          "Prévia determinística de elegibilidade tributária a IRRF conforme tipo de dependência, idade, escolaridade e invalidez.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Elegibilidade IRRF prevista",
      group = "Elegibilidade e benefícios",
      order = 132,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "verified",
      readOnly = true,
      tableHidden = true)
  private Boolean elegivelIrrfPrevisto;
}
