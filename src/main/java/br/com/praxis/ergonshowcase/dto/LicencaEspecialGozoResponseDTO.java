package br.com.praxis.ergonshowcase.dto;
import org.praxisplatform.uischema.annotation.Threshold;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.praxisplatform.uischema.annotation.ResourceIdentity;
import org.praxisplatform.uischema.extension.annotation.*;
@org.praxisplatform.uischema.fact.ResourceFact(key = "diasAbono", resourceKey = "administracao-pessoal.licencas-especiais-gozo", label = "Dias de abono", description = "Dias convertidos em abono na própria parcela, sem venda duplicada.", dataType = "integer", icon = "analytics", tone = "info", sourceKind = "property")
@org.praxisplatform.uischema.fact.ResourceFact(key = "diasGozo", resourceKey = "administracao-pessoal.licencas-especiais-gozo", label = "Dias de gozo", description = "Dias corridos de fruição na parcela de licença especial.", dataType = "integer", unit = "days", icon = "beach_access", tone = "info", sourceKind = "property", thresholds = {@Threshold(max = 30, tone = "info", label = "Até um mês"), @Threshold(min = 31, tone = "success", label = "Parcela extensa")})
@org.praxisplatform.uischema.fact.ResourceFact(key = "consumoDias",resourceKey = "administracao-pessoal.licencas-especiais-gozo",label = "Consumo da parcela",description = "Dias de fruição mais dias de abono debitados pela parcela ativa.",dataType = "integer",icon = "calculate",tone = "info",sourceKind = "property")
@Getter @Setter
@ResourceIdentity(keyField = "id", titleField = "descricao", subtitleField = "pessoaNome", metadataFields = {"chavePeraqlicesp", "abonoPecuniario"})
@Schema(description = "Parcela completa de gozo de licença especial com vínculo, abono e competência de folha.")
public class LicencaEspecialGozoResponseDTO extends LicencaEspecialGozoDados {
  @Schema(description = "Identidade imutável da parcela de fruição de licença especial.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Identidade da parcela", helpText = "Identificador estável da parcela de gozo.", readOnly = true, tableHidden = true, formHidden = true, icon = "tag", preset = UISchemaPreset.ENTERPRISE_ID)
  private Long id;
  @Schema(description = "Dias de fruição mais abono desta parcela; cancelamento libera o débito integral no período.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Consumo da parcela", readOnly = true, tableHidden = true, icon = "calculate")
  public Integer getConsumoDias() {return "CANCELADO".equals(getSituacao()) ? 0 : (getDiasGozo()==null?0:getDiasGozo())+(getDiasAbono()==null?0:getDiasAbono());}
  @Override
  @Schema(description = "Síntese da parcela de fruição de licença-prêmio ou especial para identificação na programação do vínculo.", accessMode = Schema.AccessMode.READ_ONLY)
  @org.praxisplatform.uischema.annotation.ResourceReference(resourceKey = "administracao-pessoal.licencas-especiais-gozo", valueField = "id", surfaceId = "detail", previewSurfaceId = "detail", detailPresentation = "drawer")
  public String getDescricao() { return super.getDescricao(); }

}
