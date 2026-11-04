package br.com.praxis.ergonshowcase.dto;
import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.domain.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.*;
import lombok.Getter;
import org.praxisplatform.uischema.*;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.extension.annotation.*;
@Getter
@Schema(description = "Apuração financeira e observações da venda, com preservação de propriedades ausentes. No abono vinculado, permite apurar o valor e registrar observações; folha, documentos, origem e dias continuam controlados pelo gozo. Valores liquidados não podem ser retificados.")
public class VendaLicencaEspecialLiquidacaoPatchDTO extends org.praxisplatform.uischema.dto.PartialUpdateInput {
  @DomainGovernance(kind = DomainGovernanceKind.PRIVACY, classification = DomainClassification.CONFIDENTIAL, dataCategory = DomainDataCategory.FINANCIAL, complianceTags = {"LGPD", "FINANCIAL"}, reason = "Competência do abono associado à licença individual.", aiUsage = @AiUsagePolicy(visibility = AiVisibilityMode.SUMMARIZE_ONLY, trainingUse = AiTrainingUseMode.DENY))
  @Schema(description = "Competência de referência da folha associada ao abono desta parcela, representada pelo primeiro dia do mês.", nullable = true)
  @UISchema(label = "Competência da folha", helpText = "Primeiro dia do mês de competência financeira.", group = "Abono e folha", order = 190, type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, icon = "calendar_month", tableHidden = true, preset = UISchemaPreset.DATE_RANGE)
  private LocalDate mesAnoFolha;
  @com.fasterxml.jackson.annotation.JsonSetter("mesAnoFolha")
  public void setMesAnoFolha(LocalDate value) { supplied("mesAnoFolha"); this.mesAnoFolha=value; }
  @DomainGovernance(kind = DomainGovernanceKind.PRIVACY, classification = DomainClassification.CONFIDENTIAL, dataCategory = DomainDataCategory.FINANCIAL, complianceTags = {"LGPD", "FINANCIAL"}, reason = "Folha de referência do abono individual.", aiUsage = @AiUsagePolicy(visibility = AiVisibilityMode.SUMMARIZE_ONLY, trainingUse = AiTrainingUseMode.DENY))
  @Min(1)
  @Schema(description = "Número da folha de pagamento dentro da competência de referência; é identificador operacional e não valor financeiro.", nullable = true)
  @UISchema(label = "Número da folha", helpText = "Identifique a folha da competência informada.", group = "Abono e folha", order = 200, type = FieldDataType.NUMBER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "receipt_long", tableHidden = true, preset = UISchemaPreset.ENTERPRISE_CODE)
  private Integer numFolha;
  @com.fasterxml.jackson.annotation.JsonSetter("numFolha")
  public void setNumFolha(Integer value) { supplied("numFolha"); this.numFolha=value; }
  @Size(max = 2000)
  @Schema(description = "Justificativa complementar do lançamento para análise funcional; não substitui documento comprobatório nem pode conter credenciais ou dados de saúde desnecessários.", nullable = true)
  @DomainGovernance(kind = DomainGovernanceKind.PRIVACY, classification = DomainClassification.RESTRICTED, dataCategory = DomainDataCategory.PERSONAL, complianceTags = {"LGPD"}, reason = "Texto livre de assentamento funcional requer revisão antes de divulgação.", aiUsage = @AiUsagePolicy(visibility = AiVisibilityMode.DENY, trainingUse = AiTrainingUseMode.DENY, reasoningUse = AiControlledUseMode.DENY))
  @UISchema(label = "Observações", helpText = "Registre apenas informações necessárias à análise do direito.", group = "Lastro documental", order = 100, type = FieldDataType.TEXT, controlType = FieldControlType.TEXTAREA, icon = "notes", tableHidden = true, visibleForAuthorities = {"ERGON_READ"}, editableForAuthorities = {"ERGON_WRITE"}, fieldAccessReason = "Acesso restrito à manutenção de assentamentos funcionais")
  private String observacoes;
  @com.fasterxml.jackson.annotation.JsonSetter("observacoes")
  public void setObservacoes(String value) { supplied("observacoes"); this.observacoes=value; }
  @Schema(description = "Documento que fundamenta juridicamente o reconhecimento do tempo ou a concessão da licença.", nullable = true)
  @ResourceReference(resourceKey = "administracao-pessoal.documentos-legais", valueField = "documentoLegalId", surfaceId = "ficha-documento", previewSurfaceId = "ficha-documento", detailPresentation = "drawer")
  @UISchema(label = "Fundamento legal", helpText = "Selecione o documento de embasamento do direito.", group = "Lastro documental", order = 80, type = FieldDataType.NUMBER, controlType = FieldControlType.ENTITY_LOOKUP, endpoint = ApiPaths.DOCUMENTO_OPTIONS, valueField = "id", displayField = "label", icon = "gavel", tableHidden = true, preset = UISchemaPreset.LEGAL_DOCUMENT_REFERENCE)
  private Long documentoLegalId;
  @com.fasterxml.jackson.annotation.JsonSetter("documentoLegalId")
  public void setDocumentoLegalId(Long value) { supplied("documentoLegalId"); this.documentoLegalId=value; }
  @Schema(description = "Ato de publicação que dá publicidade ao lançamento, distinto do documento de embasamento legal.", nullable = true)
  @ResourceReference(resourceKey = "administracao-pessoal.documentos-legais", valueField = "atoPublicacaoId", surfaceId = "ficha-documento", previewSurfaceId = "ficha-documento", detailPresentation = "drawer")
  @UISchema(label = "Ato de publicação", helpText = "Selecione a publicação correspondente ao assentamento.", group = "Lastro documental", order = 90, type = FieldDataType.NUMBER, controlType = FieldControlType.ENTITY_LOOKUP, endpoint = ApiPaths.DOCUMENTO_OPTIONS, valueField = "id", displayField = "label", icon = "description", tableHidden = true, preset = UISchemaPreset.LEGAL_DOCUMENT_REFERENCE)
  private Long atoPublicacaoId;
  @com.fasterxml.jackson.annotation.JsonSetter("atoPublicacaoId")
  public void setAtoPublicacaoId(Long value) { supplied("atoPublicacaoId"); this.atoPublicacaoId=value; }
  @DecimalMin("0.00") @Digits(integer = 13, fraction = 2)
  @Schema(description = "Valor bruto em reais da indenização por venda da licença especial. Nulo significa ainda não informado, preservando a distinção de valor zero.", nullable = true)
  @DomainGovernance(kind = DomainGovernanceKind.PRIVACY, classification = DomainClassification.RESTRICTED, dataCategory = DomainDataCategory.FINANCIAL, complianceTags = {"LGPD", "FINANCIAL", "NON_EXPORTABLE"}, reason = "Indenização individual do servidor; excluir da exportação cadastral e das medidas públicas.", aiUsage = @AiUsagePolicy(visibility = AiVisibilityMode.DENY, trainingUse = AiTrainingUseMode.DENY, ruleAuthoring = AiControlledUseMode.REVIEW_REQUIRED, reasoningUse = AiControlledUseMode.DENY))
  @UISchema(label = "Valor da indenização", helpText = "Valor bruto em reais, quando apurado.", group = "Liquidação financeira", order = 150, type = FieldDataType.NUMBER, controlType = FieldControlType.CURRENCY_INPUT, icon = "payments", tableHidden = true, preset = UISchemaPreset.MONETARY_AMOUNT, visibleForAuthorities = {"ERGON_READ"}, editableForAuthorities = {"ERGON_WRITE"}, fieldAccessReason = "Valor indenizatório individual de acesso funcional restrito")
  private BigDecimal valorIndenizacao;
  @com.fasterxml.jackson.annotation.JsonSetter("valorIndenizacao")
  public void setValorIndenizacao(BigDecimal value) {supplied("valorIndenizacao");this.valorIndenizacao=value;}
}
