package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.domain.VinculoId;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.praxisplatform.uischema.*;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.extension.annotation.*;

/** Campos comuns; parcelas admitem as chaves funcionais omitidas e hidratadas pelo período. */
@Getter @Setter
public abstract class AssentamentoTempoLicencaDados {
  @Schema(description = "Órgão responsável pelo assentamento de tempo ou licença do vínculo. Nas parcelas, é preenchido pelo período aquisitivo selecionado.")
  @ResourceReference(resourceKey = "administracao-pessoal.empresas", valueField = "empCodigo", surfaceId = "ficha-institucional", previewSurfaceId = "ficha-institucional", detailPresentation = "drawer")
  @UISchema(label = "Órgão", helpText = "Selecione o órgão do vínculo; nas parcelas, herda do período.", group = "Identificação funcional", order = 10, type = FieldDataType.NUMBER, controlType = FieldControlType.ENTITY_LOOKUP, endpoint = ApiPaths.ORGAO_OPTIONS, valueField = "id", displayField = "label", icon = "apartment", tableHidden = true, preset = UISchemaPreset.TENANT_LABEL)
  private Integer empCodigo;

  @Schema(description = "Denominação oficial do órgão público responsável pelo vínculo do assentamento.", accessMode = Schema.AccessMode.READ_ONLY)
  @ResourceReference(resourceKey = "administracao-pessoal.empresas", valueField = "empCodigo", surfaceId = "ficha-institucional", previewSurfaceId = "ficha-institucional", detailPresentation = "drawer")
  @UISchema(label = "Nome do órgão", helpText = "Órgão responsável pelo vínculo funcional.", group = "Identificação funcional", order = 15, type = FieldDataType.TEXT, controlType = FieldControlType.INPUT, icon = "apartment", readOnly = true, tableHidden = true, formHidden = true, preset = UISchemaPreset.TENANT_LABEL)
  private String orgaoNome;

  @DomainGovernance(kind = DomainGovernanceKind.PRIVACY, classification = DomainClassification.CONFIDENTIAL, dataCategory = DomainDataCategory.PERSONAL, complianceTags = {"LGPD", "PII"}, reason = "Identificação funcional para manutenção de direitos do servidor.", aiUsage = @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @Schema(description = "Número funcional da pessoa titular do tempo reconhecido ou da licença. Não é uma quantidade e não deve compor medidas estatísticas.")
  @ResourceReference(resourceKey = "administracao-pessoal.funcionarios", valueField = "numfunc", surfaceId = "ficha-cadastral", previewSurfaceId = "ficha-cadastral", detailPresentation = "drawer")
  @UISchema(label = "Servidor", helpText = "Pessoa titular do assentamento; nas parcelas, herda do período.", group = "Identificação funcional", order = 20, type = FieldDataType.NUMBER, controlType = FieldControlType.ENTITY_LOOKUP, endpoint = ApiPaths.PESSOA_OPTIONS, valueField = "id", displayField = "label", icon = "person", tableHidden = true, preset = UISchemaPreset.ENTERPRISE_ID)
  private Long numfunc;

  @Schema(description = "Sequencial do vínculo da pessoa titular. Deve corresponder ao número funcional e ao órgão; nas parcelas é hidratado pelo período.")
  @UISchema(label = "Sequencial do vínculo", helpText = "Informe o número do vínculo dentro do cadastro da pessoa.", group = "Identificação funcional", order = 30, type = FieldDataType.NUMBER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "badge", tableHidden = true, preset = UISchemaPreset.ENTERPRISE_CODE)
  private Integer numvinc;

  @Schema(description = "Identidade composta do vínculo, apresentada para navegação ao resumo funcional.", accessMode = Schema.AccessMode.READ_ONLY)
  @ResourceReference(resourceKey = "administracao-pessoal.vinculos", valueField = "vinculoId", surfaceId = "resumo-funcional", previewSurfaceId = "resumo-funcional", detailPresentation = "drawer")
  @UISchema(label = "Vínculo funcional", helpText = "Abra o resumo funcional do titular.", group = "Identificação funcional", order = 35, type = FieldDataType.TEXT, controlType = FieldControlType.INPUT, icon = "work", tableHidden = true, readOnly = true, preset = UISchemaPreset.ENTERPRISE_ID)
  private VinculoId vinculoId;

  @Schema(description = "Matrícula institucional do vínculo funcional titular do assentamento.", accessMode = Schema.AccessMode.READ_ONLY)
  @ResourceReference(resourceKey = "administracao-pessoal.vinculos", valueField = "vinculoId", surfaceId = "resumo-funcional", previewSurfaceId = "resumo-funcional", detailPresentation = "drawer")
  @UISchema(label = "Matrícula do vínculo", helpText = "Matrícula funcional do vínculo titular.", group = "Identificação funcional", order = 38, type = FieldDataType.TEXT, controlType = FieldControlType.INPUT, icon = "badge", readOnly = true, tableHidden = false, formHidden = true, preset = UISchemaPreset.ENTERPRISE_CODE)
  private String vinculoMatricula;

  @Schema(description = "Nome civil do titular, resolvido pelo cadastro de pessoas para identificação humana do assentamento.", accessMode = Schema.AccessMode.READ_ONLY)
  @ResourceReference(resourceKey = "administracao-pessoal.funcionarios", valueField = "numfunc", surfaceId = "ficha-cadastral", previewSurfaceId = "ficha-cadastral", detailPresentation = "drawer")
  @DomainGovernance(kind = DomainGovernanceKind.PRIVACY, classification = DomainClassification.CONFIDENTIAL, dataCategory = DomainDataCategory.PERSONAL, complianceTags = {"LGPD", "PII"}, reason = "Identificação do titular do direito funcional.", aiUsage = @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @UISchema(label = "Nome do servidor", helpText = "Titular do tempo reconhecido ou da licença.", group = "Identificação funcional", order = 40, type = FieldDataType.TEXT, controlType = FieldControlType.INPUT, icon = "person", readOnly = true, tableHidden = false, formHidden = true, preset = UISchemaPreset.ENTERPRISE_NAME)
  private String pessoaNome;

  @NotNull
  @Schema(description = "Data inicial do intervalo reconhecido: serviço averbado, aquisição do direito, parcela de gozo ou registro da venda, conforme o recurso.")
  @UISchema(label = "Início do intervalo", helpText = "Data inicial do evento ou período registrado.", group = "Período e situação", order = 50, type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, icon = "event", tableHidden = false, quickViewVisible = true, quickViewOrder = 1, preset = UISchemaPreset.DATE_RANGE)
  private LocalDate dtini;

  @Schema(description = "Data final do intervalo reconhecido. No gozo representa encerramento efetivo; na venda pode permanecer vazia por ser evento pontual.", nullable = true)
  @UISchema(label = "Término do intervalo", helpText = "Informe o encerramento do período, quando houver.", group = "Período e situação", order = 60, type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, icon = "event_available", tableHidden = false, preset = UISchemaPreset.DATE_RANGE)
  private LocalDate dtfim;

  @NotBlank
  @Schema(description = "Estado persistido de ciclo de vida; consulte as ações publicadas pelo recurso para transições permitidas.")
  @UISchema(label = "Situação cadastral", helpText = "Classificação do lançamento para consulta e pesquisa.", group = "Período e situação", order = 70, type = FieldDataType.TEXT, controlType = FieldControlType.SELECT, options = "[{\"value\":\"REGISTRADO\",\"label\":\"Registrado\"},{\"value\":\"CANCELADO\",\"label\":\"Cancelado\"}]", icon = "flag", tableHidden = false, quickViewVisible = true, quickViewOrder = 3, preset = UISchemaPreset.ENTERPRISE_STATUS)
  private String situacao;

  @Schema(description = "Documento que fundamenta juridicamente o reconhecimento do tempo ou a concessão da licença.", nullable = true)
  @ResourceReference(resourceKey = "administracao-pessoal.documentos-legais", valueField = "documentoLegalId", surfaceId = "ficha-documento", previewSurfaceId = "ficha-documento", detailPresentation = "drawer")
  @UISchema(label = "Fundamento legal", helpText = "Selecione o documento de embasamento do direito.", group = "Lastro documental", order = 80, type = FieldDataType.NUMBER, controlType = FieldControlType.ENTITY_LOOKUP, endpoint = ApiPaths.DOCUMENTO_OPTIONS, valueField = "id", displayField = "label", icon = "gavel", tableHidden = true, preset = UISchemaPreset.LEGAL_DOCUMENT_REFERENCE)
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  private Long documentoLegalId;

  @Schema(description = "Número oficial do ato ou documento legal que fundamenta o assentamento.", accessMode = Schema.AccessMode.READ_ONLY)
  @ResourceReference(resourceKey = "administracao-pessoal.documentos-legais", valueField = "documentoLegalId", surfaceId = "ficha-documento", previewSurfaceId = "ficha-documento", detailPresentation = "drawer")
  @UISchema(label = "Número do fundamento", helpText = "Identificação oficial do documento legal vinculado.", group = "Lastro documental", order = 85, type = FieldDataType.TEXT, controlType = FieldControlType.INPUT, icon = "gavel", readOnly = true, tableHidden = true, formHidden = true, preset = UISchemaPreset.LEGAL_DOCUMENT_REFERENCE)
  private String documentoLegalNumero;

  @Schema(description = "Ato de publicação que dá publicidade ao lançamento, distinto do documento de embasamento legal.", nullable = true)
  @ResourceReference(resourceKey = "administracao-pessoal.documentos-legais", valueField = "atoPublicacaoId", surfaceId = "ficha-documento", previewSurfaceId = "ficha-documento", detailPresentation = "drawer")
  @UISchema(label = "Ato de publicação", helpText = "Selecione a publicação correspondente ao assentamento.", group = "Lastro documental", order = 90, type = FieldDataType.NUMBER, controlType = FieldControlType.ENTITY_LOOKUP, endpoint = ApiPaths.DOCUMENTO_OPTIONS, valueField = "id", displayField = "label", icon = "description", tableHidden = true, preset = UISchemaPreset.LEGAL_DOCUMENT_REFERENCE)
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  private Long atoPublicacaoId;

  @Schema(description = "Número oficial do ato de publicação associado ao assentamento.", accessMode = Schema.AccessMode.READ_ONLY)
  @ResourceReference(resourceKey = "administracao-pessoal.documentos-legais", valueField = "atoPublicacaoId", surfaceId = "ficha-documento", previewSurfaceId = "ficha-documento", detailPresentation = "drawer")
  @UISchema(label = "Número da publicação", helpText = "Identificação oficial do ato de publicação vinculado.", group = "Lastro documental", order = 95, type = FieldDataType.TEXT, controlType = FieldControlType.INPUT, icon = "description", readOnly = true, tableHidden = true, formHidden = true, preset = UISchemaPreset.LEGAL_DOCUMENT_REFERENCE)
  private String atoPublicacaoNumero;

  @Size(max = 2000)
  @Schema(description = "Justificativa complementar do lançamento para análise funcional; não substitui documento comprobatório nem pode conter credenciais ou dados de saúde desnecessários.", nullable = true)
  @DomainGovernance(kind = DomainGovernanceKind.PRIVACY, classification = DomainClassification.RESTRICTED, dataCategory = DomainDataCategory.PERSONAL, complianceTags = {"LGPD"}, reason = "Texto livre de assentamento funcional requer revisão antes de divulgação.", aiUsage = @AiUsagePolicy(visibility = AiVisibilityMode.DENY, trainingUse = AiTrainingUseMode.DENY, reasoningUse = AiControlledUseMode.DENY))
  @UISchema(label = "Observações", helpText = "Registre apenas informações necessárias à análise do direito.", group = "Lastro documental", order = 100, type = FieldDataType.TEXT, controlType = FieldControlType.TEXTAREA, icon = "notes", tableHidden = true, visibleForAuthorities = {"ERGON_READ"}, editableForAuthorities = {"ERGON_WRITE"}, fieldAccessReason = "Acesso restrito à manutenção de assentamentos funcionais")
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  private String observacoes;
}
