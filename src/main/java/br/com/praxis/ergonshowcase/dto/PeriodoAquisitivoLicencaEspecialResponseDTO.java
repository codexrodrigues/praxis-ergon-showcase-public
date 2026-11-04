package br.com.praxis.ergonshowcase.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.praxisplatform.uischema.annotation.ResourceIdentity;
import org.praxisplatform.uischema.extension.annotation.*;
import org.praxisplatform.uischema.*;
import org.praxisplatform.uischema.annotation.*;
@org.praxisplatform.uischema.fact.ResourceFact(key = "diasDireito", resourceKey = "administracao-pessoal.periodos-aquisitivos-licenca-especial", label = "Dias de direito", description = "Dias atribuídos ao ciclo antes das destinações.", dataType = "integer", icon = "analytics", tone = "info", sourceKind = "property")
@org.praxisplatform.uischema.fact.ResourceFact(key = "diasGozados", resourceKey = "administracao-pessoal.periodos-aquisitivos-licenca-especial", label = "Dias de gozo", description = "Reserva ou fruição apurada das parcelas ativas.", dataType = "integer", icon = "analytics", tone = "info", sourceKind = "property")
@org.praxisplatform.uischema.fact.ResourceFact(key = "diasVendidos", resourceKey = "administracao-pessoal.periodos-aquisitivos-licenca-especial", label = "Dias em pecúnia", description = "Venda avulsa mais abonos ativos do ciclo.", dataType = "integer", icon = "analytics", tone = "info", sourceKind = "property")
@org.praxisplatform.uischema.fact.ResourceFact(key = "saldoDias", resourceKey = "administracao-pessoal.periodos-aquisitivos-licenca-especial", label = "Saldo de licença", description = "Dias de direito restantes após gozo, venda e conversão em dobro.", dataType = "integer", unit = "days", icon = "donut_large", tone = "success", sourceKind = "property", thresholds = {@Threshold(max = 0, tone = "warning", label = "Sem saldo"), @Threshold(min = 1, tone = "success", label = "Disponível")})
@Getter @Setter
@ResourceIdentity(keyField = "id", titleField = "descricao", subtitleField = "pessoaNome", metadataFields = {"chave", "anosLicEsp", "mesesLicEspPer"})
@Schema(description = "Ciclo completo de licença especial com direito, destinações de dias e overrides individuais.")
public class PeriodoAquisitivoLicencaEspecialResponseDTO extends PeriodoAquisitivoLicencaEspecialDados {
  @Schema(description = "Total calculado de dias das parcelas registradas: reserva pela previsão enquanto abertas e fruição efetiva após encerramento. Parcelas canceladas não consomem o direito.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Dias de gozo", helpText = "Inclui reserva de parcelas abertas e fruição encerrada; calculado pelo servidor.", group = "Contabilidade do direito", order = 130, type = FieldDataType.NUMBER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "beach_access", readOnly = true, tableHidden = true)
  private Integer diasGozados;
  @Schema(description = "Total calculado de dias vendidos e de abono das parcelas registradas, excluindo cancelamentos; compartilha o teto pecuniário do ciclo.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Dias vendidos", helpText = "Soma de venda avulsa e abono; calculado pelo servidor.", group = "Contabilidade do direito", order = 140, type = FieldDataType.NUMBER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "sell", readOnly = true, tableHidden = true)
  private Integer diasVendidos;
  @Schema(description = "Saldo calculado pelo servidor: direito menos gozo reservado/efetivo, vendas/abonos e dias originais convertidos em dobro. Não é entrada cadastral.", accessMode = Schema.AccessMode.READ_ONLY)
  @MicroVisualization(kind = MicroVisualizationKind.BULLET, valueExpr = "=saldoDias", targetExpr = "=diasDireito", totalExpr = "=diasDireito", fallbackText = "Saldo de licença", ariaLabel = "Saldo em relação ao direito do período", thresholds = {@Threshold(max = 0, tone = "warning", label = "Sem saldo"), @Threshold(min = 1, tone = "success", label = "Disponível")})
  @UISchema(label = "Saldo de dias", helpText = "Saldo após todas as destinações ativas; calculado pelo servidor.", group = "Contabilidade do direito", order = 160, type = FieldDataType.NUMBER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "donut_large", readOnly = true, tableHidden = false, quickViewVisible = true, quickViewOrder = 5)
  private Integer saldoDias;
  @Schema(description = "Identidade imutável do ciclo de aquisição utilizada pelas parcelas de gozo e venda.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Identidade do período", helpText = "Identificador estável do ciclo aquisitivo.", readOnly = true, tableHidden = true, formHidden = true, icon = "tag", preset = UISchemaPreset.ENTERPRISE_ID)
  private Long id;
  @Override
  @Schema(description = "Identificação do ciclo de aquisição de licença-prêmio ou especial do vínculo, permitindo distinguir quinquênios sucessivos.", accessMode = Schema.AccessMode.READ_ONLY)
  @org.praxisplatform.uischema.annotation.ResourceReference(resourceKey = "administracao-pessoal.periodos-aquisitivos-licenca-especial", valueField = "id", surfaceId = "detail", previewSurfaceId = "detail", detailPresentation = "drawer")
  public String getDescricao() { return super.getDescricao(); }

}
