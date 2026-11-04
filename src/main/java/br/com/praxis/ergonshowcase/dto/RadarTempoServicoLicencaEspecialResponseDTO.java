package br.com.praxis.ergonshowcase.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.*;
import lombok.*;
import org.praxisplatform.uischema.*;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.customfields.CustomFields;
import org.praxisplatform.uischema.extension.annotation.*;
@org.praxisplatform.uischema.fact.ResourceFact(key = "diasDireito", resourceKey = "administracao-pessoal.radar-tempo-servico-licenca-especial", label = "Dias de direito", description = "Dias de direito do período monitorado.", dataType = "integer", icon = "analytics", tone = "info", sourceKind = "property")
@org.praxisplatform.uischema.fact.ResourceFact(key = "diasProgramados", resourceKey = "administracao-pessoal.radar-tempo-servico-licenca-especial", label = "Dias programados", description = "Reserva e fruição consolidadas do período monitorado.", dataType = "integer", icon = "analytics", tone = "info", sourceKind = "property")
@org.praxisplatform.uischema.fact.ResourceFact(key = "saldoDias", resourceKey = "administracao-pessoal.radar-tempo-servico-licenca-especial", label = "Saldo de licença", description = "Posição cadastral dos dias de direito disponíveis no ciclo monitorado.", dataType = "integer", unit = "days", icon = "donut_large", tone = "success", sourceKind = "property", thresholds = {@Threshold(max = 0, tone = "warning", label = "Sem saldo"), @Threshold(min = 1, tone = "success", label = "Disponível")})
@Getter @Setter
@ResourceIdentity(keyField = "id", titleField = "descricao", subtitleField = "pessoaNome", metadataFields = {"dtPrescricao", "diasProgramados", "diasVendidosRegistrados"})
@Schema(description = "Radar por ciclo aquisitivo: saldo cadastral, tempo averbado do vínculo, parcelas futuras e vendas registradas. Não substitui a apuração legal dos direitos.")
public class RadarTempoServicoLicencaEspecialResponseDTO extends AssentamentoTempoLicencaDados {
  @Schema(description = "Identidade do ciclo aquisitivo monitorado pelo radar.", accessMode = Schema.AccessMode.READ_ONLY)
  @ResourceReference(resourceKey = "administracao-pessoal.periodos-aquisitivos-licenca-especial", valueField = "id", surfaceId = "detail", previewSurfaceId = "detail", detailPresentation = "drawer")
  @UISchema(label = "Identidade do ciclo", helpText = "Abra o ciclo aquisitivo de origem.", tableHidden = true, readOnly = true, icon = "tag", preset = UISchemaPreset.ENTERPRISE_ID)
  private Long id;
  @Schema(description = "Descrição do ciclo aquisitivo monitorado, preservada para identificação executiva.", accessMode = Schema.AccessMode.READ_ONLY)
  @ResourceReference(resourceKey = "administracao-pessoal.radar-tempo-servico-licenca-especial", valueField = "id", surfaceId = "detail", previewSurfaceId = "detail", detailPresentation = "drawer")
  @UISchema(label = "Ciclo monitorado", helpText = "Identificação do ciclo de licença especial.", group = "Direito monitorado", order = 1, type = FieldDataType.TEXT, controlType = FieldControlType.INPUT, icon = "radar", tableHidden = false, readOnly = true)
  private String descricao;
  @Schema(description = "Dias de direito atribuídos ao ciclo na posição cadastral.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Dias de direito", helpText = "Direito cadastrado para este ciclo.", group = "Direito monitorado", order = 110, type = FieldDataType.NUMBER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "verified", tableHidden = false, quickViewVisible = true, quickViewOrder = 4, readOnly = true)
  private Integer diasDireito;
  @Schema(description = "Saldo cadastral de dias após as destinações já contabilizadas no ciclo, sem deduzir novamente as parcelas futuras.", accessMode = Schema.AccessMode.READ_ONLY)
  @MicroVisualization(kind = MicroVisualizationKind.BULLET, valueExpr = "=saldoDias", targetExpr = "=diasDireito", totalExpr = "=diasDireito", fallbackText = "Saldo disponível", ariaLabel = "Saldo do ciclo em relação ao direito", thresholds = {@Threshold(max = 0, tone = "warning", label = "Sem saldo"), @Threshold(min = 1, tone = "success", label = "Com saldo")})
  @UISchema(label = "Saldo de licença", helpText = "Posição cadastral de dias disponíveis.", group = "Direito monitorado", order = 120, type = FieldDataType.NUMBER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "donut_large", tableHidden = false, quickViewVisible = true, quickViewOrder = 5, readOnly = true)
  private Integer saldoDias;
  @Schema(description = "Dias originais de licença consumidos pela conversão em dobro no ciclo; não representa o dobro creditado no tempo de serviço.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Dias convertidos em dobro", helpText = "Dias originais destinados à contagem em dobro.", group = "Destinações do direito", order = 130, type = FieldDataType.NUMBER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "more_time", tableHidden = true, readOnly = true)
  private Integer diasConvertidosDobro;
  @Schema(description = "Prescrição cadastrada para o direito; nula quando não há prazo informado.", accessMode = Schema.AccessMode.READ_ONLY, nullable = true)
  @UISchema(label = "Prescrição do ciclo", helpText = "Prazo cadastrado; ausência não significa direito sem prescrição.", group = "Prazos", order = 140, type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, icon = "warning", tableHidden = true, readOnly = true, preset = UISchemaPreset.DATE_RANGE)
  private LocalDate dtPrescricao;
  @Schema(description = "Soma do tempo líquido averbado para licença especial no vínculo inteiro, descontando desaverbações parciais e removidos. Repete-se em cada ciclo do mesmo vínculo e não deve ser somada entre ciclos.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Tempo averbado do vínculo", helpText = "Total do vínculo, repetido por ciclo; não some as linhas.", group = "Tempo e destinações", order = 150, type = FieldDataType.NUMBER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "history_edu", tableHidden = false, readOnly = true)
  private Long totalAverbadoVinculo;
  @Schema(description = "Dias em parcelas registradas com início a partir da data corrente; cancelamentos não participam da soma.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Gozo futuro registrado", helpText = "Dias em parcelas futuras ainda registradas.", group = "Tempo e destinações", order = 160, type = FieldDataType.NUMBER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "event", tableHidden = true, readOnly = true)
  private Long diasProgramados;
  @Schema(description = "Dias em vendas não canceladas registradas para o ciclo, independentemente de pagamento; não representa liquidação financeira.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Vendas registradas em dias", helpText = "Dias registrados em eventos de venda deste ciclo.", group = "Tempo e destinações", order = 170, type = FieldDataType.NUMBER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "sell", tableHidden = true, readOnly = true)
  private Long diasVendidosRegistrados;
  @Schema(description = "Diferença em dias civis entre a prescrição e a data corrente; valores negativos indicam prazo ultrapassado e nulo preserva a ausência de prazo.", accessMode = Schema.AccessMode.READ_ONLY, nullable = true)
  @UISchema(label = "Dias até prescrição", helpText = "Negativo indica prazo ultrapassado; vazio indica prazo desconhecido.", group = "Prazos", order = 180, type = FieldDataType.NUMBER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "timer", tableHidden = true, readOnly = true)
  private Integer diasAtePrescricao;
  @CustomFields(resourceKey = "administracao-pessoal.periodos-aquisitivos-licenca-especial")
  @Schema(description = "Extensões institucionais do ciclo de origem projetadas para inspeção do radar.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Campos customizados", helpText = "Atributos institucionais do ciclo monitorado.", group = "Extensões institucionais", order = 900, tableHidden = true, readOnly = true)
  private Map<String, Object> customFields;
  @Override
  @Schema(description = "Estado do ciclo de vida do assentamento. A criação inicia em EM_AQUISICAO; alterações de estado exigem a ação publicada, nunca retificação cadastral.", allowableValues = {"EM_AQUISICAO", "DISPONIVEL", "PARCIALMENTE_GOZADO", "CONVERTIDO_EM_DOBRO", "QUITADO", "SUSPENSO", "PRESCRITO", "CANCELADO"})
  @UISchema(label = "Situação do direito", helpText = "Estado persistido, alterado exclusivamente por ações de negócio.", type = FieldDataType.TEXT, controlType = FieldControlType.SELECT, icon = "flag", readOnly = true, options = "[{\"value\":\"EM_AQUISICAO\",\"label\":\"Em aquisição\"},{\"value\":\"DISPONIVEL\",\"label\":\"Disponível\"},{\"value\":\"PARCIALMENTE_GOZADO\",\"label\":\"Parcialmente gozado\"},{\"value\":\"CONVERTIDO_EM_DOBRO\",\"label\":\"Convertido em dobro\"},{\"value\":\"QUITADO\",\"label\":\"Quitado\"},{\"value\":\"SUSPENSO\",\"label\":\"Suspenso\"},{\"value\":\"PRESCRITO\",\"label\":\"Prescrito\"},{\"value\":\"CANCELADO\",\"label\":\"Cancelado\"}]", tableHidden = false, quickViewVisible = true, quickViewOrder = 3, preset = UISchemaPreset.ENTERPRISE_STATUS)
  public String getSituacao() { return super.getSituacao(); }
}
