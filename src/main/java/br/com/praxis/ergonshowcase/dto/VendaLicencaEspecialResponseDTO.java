package br.com.praxis.ergonshowcase.dto;
import org.praxisplatform.uischema.annotation.Threshold;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.praxisplatform.uischema.annotation.ResourceIdentity;
import org.praxisplatform.uischema.extension.annotation.*;
@org.praxisplatform.uischema.fact.ResourceFact(key = "diasVendidos", resourceKey = "administracao-pessoal.vendas-licenca-especial", label = "Dias vendidos", description = "Dias originais de direito destinados à conversão pecuniária neste evento.", dataType = "integer", unit = "days", icon = "sell", tone = "info", sourceKind = "property", thresholds = {@Threshold(max = 30, tone = "info", label = "Até um mês"), @Threshold(min = 31, tone = "warning", label = "Conversão extensa")})
@org.praxisplatform.uischema.fact.ResourceFact(key = "liquidada",resourceKey = "administracao-pessoal.vendas-licenca-especial",label = "Pagamento registrado",description = "Indica venda no estado PAGA, sem expor o valor indenizatório restrito.",dataType = "boolean",icon = "paid",tone = "success",sourceKind = "property")
@org.praxisplatform.uischema.fact.ResourceFact(key = "abonoVinculado",resourceKey = "administracao-pessoal.vendas-licenca-especial",label = "Abono vinculado ao gozo",description = "Identifica espelho de abono da parcela, excluído da soma de vendas avulsas.",dataType = "boolean",icon = "link",tone = "info",sourceKind = "property")
@Getter @Setter
@ResourceIdentity(keyField = "id", titleField = "descricao", subtitleField = "pessoaNome", metadataFields = {"mesAnoFolha", "numFolha"})
@Schema(description = "Venda completa de licença especial com dias convertidos, modalidade e liquidação financeira.")
public class VendaLicencaEspecialResponseDTO extends VendaLicencaEspecialDados {
  @Schema(description = "Identidade da autorização financeira ativa encontrada ou emitida pelo ato de reconciliação. Preenchida no resultado desse ato; a identidade e a revisão deste registro histórico permanecem inalteradas. Permite seguir para a nova apuração sem inferir identidades.", accessMode = Schema.AccessMode.READ_ONLY, nullable = true)
  @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
  @org.praxisplatform.uischema.annotation.ResourceReference(resourceKey = "administracao-pessoal.vendas-licenca-especial", valueField = "autorizacaoAtualId", surfaceId = "detail", previewSurfaceId = "detail", detailPresentation = "drawer")
  @UISchema(label = "Autorização atual", helpText = "Abra a autorização encontrada ou emitida pela reconciliação.", group = "Conciliação financeira", readOnly = true, tableHidden = true, type = org.praxisplatform.uischema.FieldDataType.NUMBER, controlType = org.praxisplatform.uischema.FieldControlType.ENTITY_LOOKUP, endpoint = br.com.praxis.ergonshowcase.constants.ApiPaths.VENDA_LICENCA_ESPECIAL_OPTIONS, valueField = "id", displayField = "label", icon = "link", preset = UISchemaPreset.ENTERPRISE_ID)
  private Long autorizacaoAtualId;
  @Schema(description = "Verdadeiro apenas quando a liquidação foi registrada pela ação de pagamento.",accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Pagamento registrado",readOnly = true,tableHidden = true,icon = "paid")
  public boolean isLiquidada() {return "PAGA".equals(getSituacao());}
  @Schema(description = "Verdadeiro quando o evento é um espelho do abono gerado pelo gozo, sem novo débito no período.",accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Abono vinculado",readOnly = true,tableHidden = true,icon = "link")
  public boolean isAbonoVinculado() {return getLicencaEspecialGozoId()!=null;}
  @Override
  @Schema(description = "Identificação administrativa da conversão de licença especial em pecúnia ou abono, vinculada a um ciclo aquisitivo específico.", accessMode = Schema.AccessMode.READ_ONLY)
  @org.praxisplatform.uischema.annotation.ResourceReference(resourceKey = "administracao-pessoal.vendas-licenca-especial", valueField = "id", surfaceId = "detail", previewSurfaceId = "detail", detailPresentation = "drawer")
  public String getDescricao() { return super.getDescricao(); }

}
