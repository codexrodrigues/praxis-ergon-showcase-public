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
@Schema(description = "Programação e abono: propriedades ausentes preservam o valor persistido; nulo explícito só é aceito quando o domínio permitir.")
public class LicencaEspecialGozoReagendamentoPatchDTO extends org.praxisplatform.uischema.dto.PartialUpdateInput {

  @Schema(description = "Data inicial do intervalo reconhecido: serviço averbado, aquisição do direito, parcela de gozo ou registro da venda, conforme o recurso.")
  @UISchema(label = "Início do intervalo", helpText = "Data inicial do evento ou período registrado.", group = "Período e situação", order = 50, type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, icon = "event", tableHidden = false, quickViewVisible = true, quickViewOrder = 1, preset = UISchemaPreset.DATE_RANGE)
  private LocalDate dtini;
  @com.fasterxml.jackson.annotation.JsonSetter("dtini")
  public void setDtini(LocalDate value) { supplied("dtini"); this.dtini=value; }

  @Schema(description = "Término previsto da parcela de licença especial; o término efetivo permanece separado para registrar interrupções ou encerramento.")
  @UISchema(label = "Término previsto", helpText = "Último dia previsto para esta parcela.", group = "Programação do gozo", order = 140, type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, icon = "event_available", tableHidden = true, quickViewVisible = true, quickViewOrder = 2, preset = UISchemaPreset.DATE_RANGE)
  private LocalDate dtprevfim;
  @com.fasterxml.jackson.annotation.JsonSetter("dtprevfim")
  public void setDtprevfim(LocalDate value) { supplied("dtprevfim"); this.dtprevfim=value; }
   @Min(1) @Max(999)
  @Schema(description = "Quantidade inclusiva entre início e término efetivo quando dtfim está preenchido; enquanto aberta, reserva os dias até dtprevfim. O encerramento preserva a previsão original e ajusta o débito do ciclo.")
  @MicroVisualization(kind = MicroVisualizationKind.BULLET, valueExpr = "=diasGozo", max = 90, fallbackText = "Dias de licença", ariaLabel = "Duração da parcela de licença especial", thresholds = {@Threshold(max = 30, tone = "info", label = "Até um mês"), @Threshold(min = 31, tone = "success", label = "Parcela extensa")})
  @UISchema(label = "Dias de gozo", helpText = "Conte pelo término efetivo; sem encerramento, use o término previsto.", group = "Programação do gozo", order = 150, type = FieldDataType.NUMBER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "beach_access", tableHidden = false, quickViewVisible = true, quickViewOrder = 5)
  private Integer diasGozo;
  @com.fasterxml.jackson.annotation.JsonSetter("diasGozo")
  public void setDiasGozo(Integer value) { supplied("diasGozo"); this.diasGozo=value; }
  @DomainGovernance(kind = DomainGovernanceKind.PRIVACY, classification = DomainClassification.CONFIDENTIAL, dataCategory = DomainDataCategory.FINANCIAL, complianceTags = {"LGPD", "FINANCIAL"}, reason = "Existência de abono indenizatório associado à fruição individual.", aiUsage = @AiUsagePolicy(visibility = AiVisibilityMode.SUMMARIZE_ONLY, trainingUse = AiTrainingUseMode.DENY))

  @Schema(description = "Indica existência de abono pecuniário associado a esta parcela de licença especial, sem confundir o abono com dias efetivamente gozados.")
  @UISchema(label = "Com abono pecuniário", helpText = "Sinalize abono associado à parcela.", group = "Abono e folha", order = 160, type = FieldDataType.BOOLEAN, controlType = FieldControlType.CHECKBOX, icon = "payments", tableHidden = true, preset = UISchemaPreset.BOOLEAN_FLAG)
  private Boolean abonoPecuniario;
  @com.fasterxml.jackson.annotation.JsonSetter("abonoPecuniario")
  public void setAbonoPecuniario(Boolean value) { supplied("abonoPecuniario"); this.abonoPecuniario=value; }
  @DomainGovernance(kind = DomainGovernanceKind.PRIVACY, classification = DomainClassification.CONFIDENTIAL, dataCategory = DomainDataCategory.FINANCIAL, complianceTags = {"LGPD", "FINANCIAL"}, reason = "Dias de direito destinados a abono pecuniário individual.", aiUsage = @AiUsagePolicy(visibility = AiVisibilityMode.SUMMARIZE_ONLY, trainingUse = AiTrainingUseMode.DENY))
   @Min(0)
  @Schema(description = "Dias de licença destinados a abono pecuniário nesta parcela; não integram os dias de descanso efetivo.")
  @UISchema(label = "Dias de abono", helpText = "Dias de direito destinados ao abono associado.", group = "Abono e folha", order = 170, type = FieldDataType.NUMBER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "payments", tableHidden = true)
  private Integer diasAbono;
  @com.fasterxml.jackson.annotation.JsonSetter("diasAbono")
  public void setDiasAbono(Integer value) { supplied("diasAbono"); this.diasAbono=value; }
  @DomainGovernance(kind = DomainGovernanceKind.PRIVACY, classification = DomainClassification.CONFIDENTIAL, dataCategory = DomainDataCategory.FINANCIAL, complianceTags = {"LGPD", "FINANCIAL"}, reason = "Posição do abono na parcela de fruição.", aiUsage = @AiUsagePolicy(visibility = AiVisibilityMode.SUMMARIZE_ONLY, trainingUse = AiTrainingUseMode.DENY))
  @Pattern(regexp = "I|F")
  @Schema(description = "Posição do abono em relação ao intervalo: I no início ou F no fim; nulo quando não houver abono.", nullable = true)
  @UISchema(options = "[{\"value\":\"I\",\"label\":\"No início\"},{\"value\":\"F\",\"label\":\"No fim\"}]", label = "Posição do abono", helpText = "Informe se o abono ocorre no início ou no fim.", group = "Abono e folha", order = 180, type = FieldDataType.TEXT, controlType = FieldControlType.SELECT, icon = "swap_horiz", tableHidden = true)
  private String iniFimAbono;
  @com.fasterxml.jackson.annotation.JsonSetter("iniFimAbono")
  public void setIniFimAbono(String value) { supplied("iniFimAbono"); this.iniFimAbono=value; }
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
}
