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
import org.praxisplatform.uischema.customfields.CustomFields;
import org.praxisplatform.uischema.extension.annotation.*;

@Getter @Setter
public abstract class LicencaEspecialGozoDados extends ParcelaLicencaEspecialDados {
  @Schema(description = "Servidor designado para substituir o titular durante a parcela de licença. A existência e a situação funcional são verificadas no banco; não é uma declaração do cliente de que o substituto está ativo.", nullable = true)
  @ResourceReference(resourceKey = "administracao-pessoal.funcionarios", valueField = "numfuncSubstituto", surfaceId = "ficha-cadastral", previewSurfaceId = "ficha-cadastral", detailPresentation = "drawer")
  @UISchema(label = "Servidor substituto", helpText = "Selecione o servidor designado para a substituição.", group = "Substituição", type = FieldDataType.NUMBER, controlType = FieldControlType.ENTITY_LOOKUP, endpoint = ApiPaths.PESSOA_OPTIONS, valueField = "id", displayField = "label", icon = "supervisor_account", tableHidden = true, preset = UISchemaPreset.ENTERPRISE_ID)
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  private Long numfuncSubstituto;
  public LicencaEspecialGozoDados() { setSituacao("PROGRAMADA"); }
  @Override
  @Schema(description = "Estado do ciclo de vida do assentamento. A criação inicia em PROGRAMADA; alterações de estado exigem a ação publicada, nunca retificação cadastral.", allowableValues = {"PROGRAMADA", "HOMOLOGADA", "EM_GOZO", "GOZADA", "INTERROMPIDA", "CANCELADO"})
  @UISchema(label = "Situação do direito", helpText = "Estado persistido, alterado exclusivamente por ações de negócio.", type = FieldDataType.TEXT, controlType = FieldControlType.SELECT, icon = "flag", readOnly = true, options = "[{\"value\":\"PROGRAMADA\",\"label\":\"Programada\"},{\"value\":\"HOMOLOGADA\",\"label\":\"Homologada\"},{\"value\":\"EM_GOZO\",\"label\":\"Em gozo\"},{\"value\":\"GOZADA\",\"label\":\"Gozada\"},{\"value\":\"INTERROMPIDA\",\"label\":\"Interrompida\"},{\"value\":\"CANCELADO\",\"label\":\"Cancelado\"}]", tableHidden = false, quickViewVisible = true, quickViewOrder = 3, preset = UISchemaPreset.ENTERPRISE_STATUS)
  public String getSituacao() { return super.getSituacao(); }

  @Override
  @Schema(description = "Primeiro dia da parcela de fruição de licença especial.")
  @UISchema(label = "Início do gozo", helpText = "Primeiro dia de fruição desta parcela.", group = "Programação do gozo", order = 50, type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, icon = "event", tableHidden = false, quickViewVisible = true, quickViewOrder = 1, preset = UISchemaPreset.DATE_RANGE)
  public LocalDate getDtini() { return super.getDtini(); }
  @Override
  @Schema(description = "Último dia efetivamente usufruído nesta parcela; permanece vazio enquanto o encerramento não estiver registrado.", nullable = true)
  @UISchema(label = "Término efetivo", helpText = "Último dia de fruição efetiva, distinto da previsão.", group = "Programação do gozo", order = 60, type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, icon = "event_available", tableHidden = false, preset = UISchemaPreset.DATE_RANGE)
  public LocalDate getDtfim() { return super.getDtfim(); }
  @NotBlank @Size(max = 200)
  @Schema(description = "Síntese da parcela de fruição de licença-prêmio ou especial para identificação na programação do vínculo.")
  
  @UISchema(label = "Parcela de licença", helpText = "Identifique a parcela de fruição.", group = "Programação do gozo", order = 1, type = FieldDataType.TEXT, controlType = FieldControlType.INPUT, icon = "beach_access", tableHidden = false, preset = UISchemaPreset.ENTERPRISE_NAME)
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE})
  private String descricao;
  @NotNull @Min(1)
  @Schema(description = "Ordem da parcela de fruição dentro do ciclo aquisitivo, distinguindo o fracionamento da mesma licença.")
  @UISchema(label = "Ordem da parcela", helpText = "Sequência de fruição neste período aquisitivo.", group = "Programação do gozo", order = 130, type = FieldDataType.NUMBER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "format_list_numbered", tableHidden = false, quickViewVisible = true, quickViewOrder = 4)
  private Integer ordemParcela;
  @Schema(description = "Término previsto da licença especial. Quando omitido, é calculado a partir do início e dos dias de gozo.")
  @UISchema(label = "Término previsto", helpText = "Último dia previsto para esta parcela.", group = "Programação do gozo", order = 140, type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, icon = "event_available", tableHidden = true, quickViewVisible = true, quickViewOrder = 2, preset = UISchemaPreset.DATE_RANGE)
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE})
  private LocalDate dtprevfim;
  @Min(1) @Max(999)
  @Schema(description = "Quantidade inclusiva entre início e término efetivo quando dtfim está preenchido; enquanto aberta, reserva os dias até dtprevfim. O encerramento preserva a previsão original e ajusta o débito do ciclo.")
  @MicroVisualization(kind = MicroVisualizationKind.BULLET, valueExpr = "=diasGozo", max = 90, fallbackText = "Dias de licença", ariaLabel = "Duração da parcela de licença especial", thresholds = {@Threshold(max = 30, tone = "info", label = "Até um mês"), @Threshold(min = 31, tone = "success", label = "Parcela extensa")})
  @UISchema(label = "Dias de gozo", helpText = "Conte pelo término efetivo; sem encerramento, use o término previsto.", group = "Programação do gozo", order = 150, type = FieldDataType.NUMBER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "beach_access", tableHidden = false, quickViewVisible = true, quickViewOrder = 5)
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE})
  private Integer diasGozo;
  @DomainGovernance(kind = DomainGovernanceKind.PRIVACY, classification = DomainClassification.CONFIDENTIAL, dataCategory = DomainDataCategory.FINANCIAL, complianceTags = {"LGPD", "FINANCIAL"}, reason = "Existência de abono indenizatório associado à fruição individual.", aiUsage = @AiUsagePolicy(visibility = AiVisibilityMode.SUMMARIZE_ONLY, trainingUse = AiTrainingUseMode.DENY))
  @NotNull
  @Schema(description = "Indica existência de abono pecuniário associado a esta parcela de licença especial, sem confundir o abono com dias efetivamente gozados.")
  @UISchema(label = "Com abono pecuniário", helpText = "Sinalize abono associado à parcela.", group = "Abono e folha", order = 160, type = FieldDataType.BOOLEAN, controlType = FieldControlType.CHECKBOX, icon = "payments", tableHidden = true, preset = UISchemaPreset.BOOLEAN_FLAG)
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE})
  private Boolean abonoPecuniario = false;
  @DomainGovernance(kind = DomainGovernanceKind.PRIVACY, classification = DomainClassification.CONFIDENTIAL, dataCategory = DomainDataCategory.FINANCIAL, complianceTags = {"LGPD", "FINANCIAL"}, reason = "Dias de direito destinados a abono pecuniário individual.", aiUsage = @AiUsagePolicy(visibility = AiVisibilityMode.SUMMARIZE_ONLY, trainingUse = AiTrainingUseMode.DENY))
  @NotNull @Min(0)
  @Schema(description = "Dias de licença destinados a abono pecuniário nesta parcela; não integram os dias de descanso efetivo.")
  @UISchema(label = "Dias de abono", helpText = "Dias de direito destinados ao abono associado.", group = "Abono e folha", order = 170, type = FieldDataType.NUMBER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "payments", tableHidden = true)
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE})
  private Integer diasAbono = 0;
  @DomainGovernance(kind = DomainGovernanceKind.PRIVACY, classification = DomainClassification.CONFIDENTIAL, dataCategory = DomainDataCategory.FINANCIAL, complianceTags = {"LGPD", "FINANCIAL"}, reason = "Posição do abono na parcela de fruição.", aiUsage = @AiUsagePolicy(visibility = AiVisibilityMode.SUMMARIZE_ONLY, trainingUse = AiTrainingUseMode.DENY))
  @Pattern(regexp = "I|F")
  @Schema(description = "Posição do abono em relação ao intervalo: I no início ou F no fim; nulo quando não houver abono.", nullable = true)
  @UISchema(options = "[{\"value\":\"I\",\"label\":\"No início\"},{\"value\":\"F\",\"label\":\"No fim\"}]", label = "Posição do abono", helpText = "Informe se o abono ocorre no início ou no fim.", group = "Abono e folha", order = 180, type = FieldDataType.TEXT, controlType = FieldControlType.SELECT, icon = "swap_horiz", tableHidden = true)
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  private String iniFimAbono;
  @DomainGovernance(kind = DomainGovernanceKind.PRIVACY, classification = DomainClassification.CONFIDENTIAL, dataCategory = DomainDataCategory.FINANCIAL, complianceTags = {"LGPD", "FINANCIAL"}, reason = "Competência do abono associado à licença individual.", aiUsage = @AiUsagePolicy(visibility = AiVisibilityMode.SUMMARIZE_ONLY, trainingUse = AiTrainingUseMode.DENY))
  @Schema(description = "Competência de referência da folha associada ao abono desta parcela, representada pelo primeiro dia do mês.", nullable = true)
  @UISchema(label = "Competência da folha", helpText = "Primeiro dia do mês de competência financeira.", group = "Abono e folha", order = 190, type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, icon = "calendar_month", tableHidden = true, preset = UISchemaPreset.DATE_RANGE)
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  private LocalDate mesAnoFolha;
  @DomainGovernance(kind = DomainGovernanceKind.PRIVACY, classification = DomainClassification.CONFIDENTIAL, dataCategory = DomainDataCategory.FINANCIAL, complianceTags = {"LGPD", "FINANCIAL"}, reason = "Folha de referência do abono individual.", aiUsage = @AiUsagePolicy(visibility = AiVisibilityMode.SUMMARIZE_ONLY, trainingUse = AiTrainingUseMode.DENY))
  @Min(1)
  @Schema(description = "Número da folha de pagamento dentro da competência de referência; é identificador operacional e não valor financeiro.", nullable = true)
  @UISchema(label = "Número da folha", helpText = "Identifique a folha da competência informada.", group = "Abono e folha", order = 200, type = FieldDataType.NUMBER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "receipt_long", tableHidden = true, preset = UISchemaPreset.ENTERPRISE_CODE)
  @BulkEditable(modes = {org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE, org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  private Integer numFolha;
  @CustomFields(resourceKey = "administracao-pessoal.licencas-especiais-gozo")
  @Schema(description = "Informações institucionais da parcela de gozo governadas pelo catálogo de campos do órgão.")
  @UISchema(label = "Campos customizados", helpText = "Atributos adicionais definidos para a fruição.", group = "Extensões institucionais", order = 900, tableHidden = true)
  private Map<String, Object> customFields = new LinkedHashMap<>();
  @Schema(description = "Dias de gozo previstos para um novo assentamento, sem reserva ou autorização. A escrita revalida os dados e a política de domínio.",accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Dias de gozo previstos", helpText = "Prévia não persistente; não certifica concessão nem disponibilidade na confirmação.", group = "Prévia de criação", readOnly = true, tableHidden = true, type = FieldDataType.NUMBER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "calculate")
  private Integer diasPrevistos;
}
