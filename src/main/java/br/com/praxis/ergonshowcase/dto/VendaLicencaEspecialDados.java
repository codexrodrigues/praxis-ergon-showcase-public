package br.com.praxis.ergonshowcase.dto;
import br.com.praxis.ergonshowcase.constants.ApiPaths;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import lombok.Getter;
import lombok.Setter;
import org.praxisplatform.uischema.*;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.customfields.CustomFields;
import org.praxisplatform.uischema.extension.annotation.*;

@Getter @Setter
public abstract class VendaLicencaEspecialDados extends ParcelaLicencaEspecialDados {
  @Schema(description = "Identidade imutável da venda carregada para edição e para sua prévia. Nunca é atribuída por POST ou PUT; o servidor usa a identidade da rota na escrita.", accessMode = Schema.AccessMode.READ_ONLY, nullable = true)
  @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
  @UISchema(label = "Identidade da venda", helpText = "Identificador do evento em edição; preenchido pelo servidor.", readOnly = true, tableHidden = true, formHidden = true, type = FieldDataType.NUMBER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "tag", preset = UISchemaPreset.ENTERPRISE_ID)
  private Long id;
  @Schema(description = "Parcela de gozo que originou o abono vinculado. Preenchida apenas pelo servidor ao homologar o gozo; o débito já integra diasAbono e não é contado novamente como venda avulsa.", accessMode = Schema.AccessMode.READ_ONLY, nullable = true)
  @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
  @ResourceReference(resourceKey = "administracao-pessoal.licencas-especiais-gozo", valueField = "licencaEspecialGozoId", surfaceId = "detail", previewSurfaceId = "detail", detailPresentation = "drawer")
  @UISchema(label = "Gozo de origem", helpText = "Origem do abono pecuniário, quando vinculado à fruição.", group = "Origem da pecúnia", type = FieldDataType.NUMBER, controlType = FieldControlType.ENTITY_LOOKUP, endpoint = ApiPaths.LICENCA_ESPECIAL_GOZO_OPTIONS, valueField = "id", displayField = "label", icon = "beach_access", tableHidden = true, readOnly = true, preset = UISchemaPreset.ENTERPRISE_ID)
  private Long licencaEspecialGozoId;
  public VendaLicencaEspecialDados() { setSituacao("SOLICITADA"); }
  @Schema(description = "Data de efeito da compensação financeira ou retirada da autorização, registrada pela ação de estorno. O pagamento original permanece preservado; nulo indica ausência de estorno datado ou histórico anterior sem essa informação.", nullable = true)
  @DomainGovernance(kind = DomainGovernanceKind.PRIVACY, classification = DomainClassification.CONFIDENTIAL, dataCategory = DomainDataCategory.FINANCIAL, complianceTags = {"LGPD", "FINANCIAL"}, reason = "Data funcional da compensação financeira individual.", aiUsage = @AiUsagePolicy(visibility = AiVisibilityMode.SUMMARIZE_ONLY, trainingUse = AiTrainingUseMode.DENY))
  @UISchema(label = "Efeito do estorno", helpText = "Data do ato compensatório; o pagamento original não é apagado.", group = "Liquidação financeira", order = 145, type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, icon = "undo", tableHidden = true, readOnly = true, preset = UISchemaPreset.DATE_RANGE)
  private LocalDate dtEstorno;
  @Override
  @Schema(description = "Estado do ciclo de vida do assentamento. A criação inicia em SOLICITADA; alterações de estado exigem a ação publicada, nunca retificação cadastral.", allowableValues = {"SOLICITADA", "AUTORIZADA", "PAGA", "ESTORNADA"})
  @UISchema(label = "Situação do direito", helpText = "Estado persistido, alterado exclusivamente por ações de negócio.", type = FieldDataType.TEXT, controlType = FieldControlType.SELECT, icon = "flag", readOnly = true, options = "[{\"value\":\"SOLICITADA\",\"label\":\"Solicitada\"},{\"value\":\"AUTORIZADA\",\"label\":\"Autorizada\"},{\"value\":\"PAGA\",\"label\":\"Paga\"},{\"value\":\"ESTORNADA\",\"label\":\"Estornada\"}]", tableHidden = false, quickViewVisible = true, quickViewOrder = 3, preset = UISchemaPreset.ENTERPRISE_STATUS)
  public String getSituacao() { return super.getSituacao(); }

  @Override
  @Schema(description = "Data de registro da conversão de licença especial em pecúnia ou abono, distinta da data de pagamento.")
  @UISchema(label = "Data da venda", helpText = "Data do evento de conversão; não indica pagamento.", group = "Conversão pecuniária", order = 50, type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, icon = "event", tableHidden = false, quickViewVisible = true, quickViewOrder = 1, preset = UISchemaPreset.DATE_RANGE)
  public LocalDate getDtini() { return super.getDtini(); }
  @Override
  @Schema(description = "Término cadastrado do assentamento de venda, quando sua origem utiliza um intervalo; não é a data de pagamento.", nullable = true, accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Término cadastrado", helpText = "Informação histórica do intervalo, quando existente.", group = "Lastro documental", order = 60, type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, icon = "event_available", tableHidden = true, formHidden = true, readOnly = true, preset = UISchemaPreset.DATE_RANGE)
  public LocalDate getDtfim() { return super.getDtfim(); }
  @NotBlank @Size(max = 200)
  @Schema(description = "Identificação administrativa da conversão de licença especial em pecúnia ou abono, vinculada a um ciclo aquisitivo específico.")
  
  @UISchema(label = "Venda de licença", helpText = "Identifique o evento de conversão pecuniária.", group = "Conversão pecuniária", order = 1, type = FieldDataType.TEXT, controlType = FieldControlType.INPUT, icon = "sell", tableHidden = false, preset = UISchemaPreset.ENTERPRISE_NAME)
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE})
  private String descricao;
  @DomainGovernance(kind = DomainGovernanceKind.PRIVACY, classification = DomainClassification.CONFIDENTIAL, dataCategory = DomainDataCategory.FINANCIAL, complianceTags = {"LGPD", "FINANCIAL"}, reason = "Quantidade de direito convertida em indenização individual.", aiUsage = @AiUsagePolicy(visibility = AiVisibilityMode.SUMMARIZE_ONLY, trainingUse = AiTrainingUseMode.DENY))
  @NotNull @Min(1) @Max(999)
  @Schema(description = "Dias originais de licença convertidos neste evento de venda; o débito no direito é em dias, independentemente do valor indenizatório.")
  @MicroVisualization(kind = MicroVisualizationKind.BULLET, valueExpr = "=diasVendidos", max = 90, fallbackText = "Dias vendidos", ariaLabel = "Dias de licença destinados à venda", thresholds = {@Threshold(max = 30, tone = "info", label = "Até um mês"), @Threshold(min = 31, tone = "warning", label = "Conversão extensa")})
  @UISchema(label = "Dias vendidos", helpText = "Dias de licença destinados à conversão neste evento.", group = "Conversão pecuniária", order = 130, type = FieldDataType.NUMBER, numericFormat = org.praxisplatform.uischema.NumericFormat.INTEGER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "sell", tableHidden = false, quickViewVisible = true, quickViewOrder = 4)
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE})
  private Integer diasVendidos;
  @DomainGovernance(kind = DomainGovernanceKind.PRIVACY, classification = DomainClassification.CONFIDENTIAL, dataCategory = DomainDataCategory.FINANCIAL, complianceTags = {"LGPD", "FINANCIAL"}, reason = "Data de liquidação da indenização individual.", aiUsage = @AiUsagePolicy(visibility = AiVisibilityMode.SUMMARIZE_ONLY, trainingUse = AiTrainingUseMode.DENY))
  @Schema(description = "Data de pagamento da indenização, quando liquidada; não se presume pagamento a partir da data do registro.", nullable = true)
  @UISchema(readOnly = true, label = "Pagamento da indenização", helpText = "Informe apenas quando o pagamento estiver identificado.", group = "Liquidação financeira", order = 140, type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, icon = "event_available", tableHidden = true, quickViewVisible = true, quickViewOrder = 2, preset = UISchemaPreset.DATE_RANGE)
  private LocalDate dtPagamento;
  @DecimalMin("0.00") @Digits(integer = 13, fraction = 2)
  @Schema(description = "Valor bruto em reais da indenização por venda da licença especial. Nulo significa ainda não informado, preservando a distinção de valor zero.", nullable = true)
  @DomainGovernance(kind = DomainGovernanceKind.PRIVACY, classification = DomainClassification.RESTRICTED, dataCategory = DomainDataCategory.FINANCIAL, complianceTags = {"LGPD", "FINANCIAL", "NON_EXPORTABLE"}, reason = "Indenização individual do servidor; excluir da exportação cadastral e das medidas públicas.", aiUsage = @AiUsagePolicy(visibility = AiVisibilityMode.DENY, trainingUse = AiTrainingUseMode.DENY, ruleAuthoring = AiControlledUseMode.REVIEW_REQUIRED, reasoningUse = AiControlledUseMode.DENY))
  @UISchema(label = "Valor da indenização", helpText = "Valor bruto em reais, quando apurado.", group = "Liquidação financeira", order = 150, type = FieldDataType.NUMBER, controlType = FieldControlType.CURRENCY_INPUT, icon = "payments", tableHidden = true, preset = UISchemaPreset.MONETARY_AMOUNT, visibleForAuthorities = {"ERGON_READ"}, editableForAuthorities = {"ERGON_WRITE"}, fieldAccessReason = "Valor indenizatório individual de acesso funcional restrito")
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  private BigDecimal valorIndenizacao;
  @DomainGovernance(kind = DomainGovernanceKind.PRIVACY, classification = DomainClassification.CONFIDENTIAL, dataCategory = DomainDataCategory.FINANCIAL, complianceTags = {"LGPD", "FINANCIAL"}, reason = "Modalidade de conversão pecuniária do direito individual.", aiUsage = @AiUsagePolicy(visibility = AiVisibilityMode.SUMMARIZE_ONLY, trainingUse = AiTrainingUseMode.DENY))
  @NotBlank @Pattern(regexp = "PECUNIA|ABONO_JUNTO_GOZO")
  @Schema(description = "Modalidade de destinação dos dias: PECUNIA para venda indenizatória ou ABONO_JUNTO_GOZO para registro vinculado gerado pela homologação do gozo.", allowableValues = {"PECUNIA", "ABONO_JUNTO_GOZO"})
  @UISchema(label = "Modalidade da conversão", helpText = "Selecione venda em pecúnia ou abono.", group = "Conversão pecuniária", order = 160, type = FieldDataType.TEXT, controlType = FieldControlType.SELECT, icon = "category", tableHidden = false, quickViewVisible = true, quickViewOrder = 5)
  private String modalidade = "PECUNIA";
  @DomainGovernance(kind = DomainGovernanceKind.PRIVACY, classification = DomainClassification.CONFIDENTIAL, dataCategory = DomainDataCategory.FINANCIAL, complianceTags = {"LGPD", "FINANCIAL"}, reason = "Competência financeira da indenização do titular.", aiUsage = @AiUsagePolicy(visibility = AiVisibilityMode.SUMMARIZE_ONLY, trainingUse = AiTrainingUseMode.DENY))
  @Schema(description = "Competência da folha que processa a indenização, representada pelo primeiro dia do mês.", nullable = true)
  @UISchema(label = "Competência da folha", helpText = "Primeiro dia do mês de processamento financeiro.", group = "Liquidação financeira", order = 170, type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, icon = "calendar_month", tableHidden = true, preset = UISchemaPreset.DATE_RANGE)
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  private LocalDate mesAnoFolha;
  @DomainGovernance(kind = DomainGovernanceKind.PRIVACY, classification = DomainClassification.CONFIDENTIAL, dataCategory = DomainDataCategory.FINANCIAL, complianceTags = {"LGPD", "FINANCIAL"}, reason = "Referência de processamento da indenização individual.", aiUsage = @AiUsagePolicy(visibility = AiVisibilityMode.SUMMARIZE_ONLY, trainingUse = AiTrainingUseMode.DENY))
  @Min(1)
  @Schema(description = "Identificador da folha de processamento dentro da competência, sem semântica de quantidade ou medida.", nullable = true)
  @UISchema(label = "Número da folha", helpText = "Informe a folha da competência de pagamento.", group = "Liquidação financeira", order = 180, type = FieldDataType.NUMBER, numericFormat = org.praxisplatform.uischema.NumericFormat.INTEGER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "receipt_long", tableHidden = true, preset = UISchemaPreset.ENTERPRISE_CODE)
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  private Integer numFolha;
  @CustomFields(resourceKey = "administracao-pessoal.vendas-licenca-especial")
  @Schema(description = "Extensões institucionais da venda de licença especial governadas pelo catálogo do órgão.")
  @UISchema(label = "Campos customizados", helpText = "Atributos adicionais definidos para a conversão pecuniária.", group = "Extensões institucionais", order = 900, tableHidden = true)
  private Map<String, Object> customFields = new LinkedHashMap<>();
  @Schema(description = "Saldo após venda previsto para um novo assentamento, sem reserva ou autorização. A escrita revalida os dados e a política de domínio.",accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Saldo após venda previsto", helpText = "Prévia não persistente; não certifica concessão nem disponibilidade na confirmação.", group = "Prévia de criação", readOnly = true, tableHidden = true, type = FieldDataType.NUMBER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "calculate")
  private Integer diasPrevistos;
}
