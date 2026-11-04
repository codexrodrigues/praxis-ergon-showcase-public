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
@Schema(description = "Certidão e finalidades: propriedades ausentes preservam o valor persistido; nulo explícito só é aceito quando o domínio permitir.")
public class AverbacaoCertidaoFinalidadesPatchDTO extends org.praxisplatform.uischema.dto.PartialUpdateInput {
  @Size(max = 200)
  @Schema(description = "Instituição onde o tempo foi prestado. O legado tornou seu preenchimento opcional na Tarefa 37486; a ausência não invalida por si a certidão.", nullable = true)
  @UISchema(label = "Instituição de origem", helpText = "Órgão ou empregador que certificou o serviço anterior.", group = "Tempo reconhecido", order = 120, type = FieldDataType.TEXT, controlType = FieldControlType.INPUT, icon = "account_balance", tableHidden = false)
  private String instituicao;
  @com.fasterxml.jackson.annotation.JsonSetter("instituicao")
  public void setInstituicao(String value) { supplied("instituicao"); this.instituicao=value; }
   @Pattern(regexp = "PUBLICO|RGPS")
  @Schema(description = "Natureza do tempo admitida pelo showcase: serviço público ou tempo vinculado ao Regime Geral. A finalidade define em que direito será aproveitado.", allowableValues = {"PUBLICO", "RGPS"})
  @UISchema(label = "Natureza do tempo", helpText = "Selecione a origem pública ou RGPS da certidão.", group = "Tempo reconhecido", order = 130, type = FieldDataType.TEXT, controlType = FieldControlType.SELECT, options = "[{\"value\":\"PUBLICO\",\"label\":\"Público\"},{\"value\":\"RGPS\",\"label\":\"RGPS\"}]", icon = "category", tableHidden = false, quickViewVisible = true, quickViewOrder = 4)
  private String tipoTempo;
  @com.fasterxml.jackson.annotation.JsonSetter("tipoTempo")
  public void setTipoTempo(String value) { supplied("tipoTempo"); this.tipoTempo=value; }
  @Size(max = 20)
  @Schema(description = "Sigla do regime previdenciário certificador do tempo. É referência textual do assentamento, sem inferir elegibilidade para aposentadoria.", nullable = true)
  @UISchema(label = "Regime previdenciário", helpText = "Informe o regime constante da certidão.", group = "Tempo reconhecido", order = 140, type = FieldDataType.TEXT, controlType = FieldControlType.INPUT, icon = "shield", tableHidden = true)
  private String regimePrevidenciario;
  @com.fasterxml.jackson.annotation.JsonSetter("regimePrevidenciario")
  public void setRegimePrevidenciario(String value) { supplied("regimePrevidenciario"); this.regimePrevidenciario=value; }
   @Pattern(regexp = "APOSENTADORIA|LICENCA_ESPECIAL|TEMPO_SERVICO")
  @Schema(description = "Direito para o qual se reconhece o tempo, consolidando AVERB_OQUE_CONTA. Uma certidão não conta automaticamente para todas as finalidades.", allowableValues = {"APOSENTADORIA", "LICENCA_ESPECIAL", "TEMPO_SERVICO"})
  @UISchema(label = "Finalidade da contagem", helpText = "Informe qual direito recebe o tempo reconhecido.", group = "Efeitos da contagem", order = 150, type = FieldDataType.TEXT, controlType = FieldControlType.SELECT, options = "[{\"value\":\"APOSENTADORIA\",\"label\":\"Aposentadoria\"},{\"value\":\"LICENCA_ESPECIAL\",\"label\":\"Licença especial\"},{\"value\":\"TEMPO_SERVICO\",\"label\":\"Tempo de serviço\"}]", icon = "rule", tableHidden = true)
  private String finalidade;
  @com.fasterxml.jackson.annotation.JsonSetter("finalidade")
  public void setFinalidade(String value) { supplied("finalidade"); this.finalidade=value; }
  @Size(max = 20)
  @Schema(description = "Complemento da finalidade de contagem. Para dias de licença especial que originam um ciclo, o legado exige o intervalo aquisitivo no formato DDMMYYYY-DDMMYYYY (Tarefa 13239 de PCK_AVERB_OQUE_CONTA).", nullable = true, example = "01012006-31122010")
  @UISchema(label = "Complemento da finalidade", helpText = "Para formar um ciclo de licença, informe DDMMYYYY-DDMMYYYY.", group = "Efeitos da contagem", order = 160, type = FieldDataType.TEXT, controlType = FieldControlType.INPUT, icon = "post_add", tableHidden = true)
  private String complemento;
  @com.fasterxml.jackson.annotation.JsonSetter("complemento")
  public void setComplemento(String value) { supplied("complemento"); this.complemento=value; }
  @Schema(description = "Data a partir da qual os dias reconhecidos produzem efeito na contagem funcional, distinta do início do serviço prestado.", nullable = true)
  @UISchema(label = "Efeito na contagem", helpText = "Data inicial de aproveitamento do tempo certificado.", group = "Efeitos da contagem", order = 170, type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, icon = "event_available", tableHidden = true, preset = UISchemaPreset.DATE_RANGE)
  private LocalDate dataAContar;
  @com.fasterxml.jackson.annotation.JsonSetter("dataAContar")
  public void setDataAContar(LocalDate value) { supplied("dataAContar"); this.dataAContar=value; }
   @Min(0)
  @Schema(description = "Dias reconhecidos na certidão antes da desaverbação parcial. Não corresponde necessariamente à diferença entre datas por existirem critérios próprios de contagem.")
  @MicroVisualization(kind = MicroVisualizationKind.BULLET, valueExpr = "=totalDias", max = 3650, fallbackText = "Dias reconhecidos", ariaLabel = "Dias certificados antes de desaverbações", thresholds = {@Threshold(max = 365, tone = "info", label = "Até um ano"), @Threshold(min = 366, tone = "success", label = "Acima de um ano")})
  @UISchema(label = "Dias reconhecidos", helpText = "Total certificado antes dos descontos por desaverbação.", group = "Efeitos da contagem", order = 180, type = FieldDataType.NUMBER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "timelapse", tableHidden = false, quickViewVisible = true, quickViewOrder = 5)
  private Integer totalDias;
  @com.fasterxml.jackson.annotation.JsonSetter("totalDias")
  public void setTotalDias(Integer value) { supplied("totalDias"); this.totalDias=value; }
   @Min(0)
  @Schema(description = "Dias aproveitados para a finalidade específica de AVERB_OQUE_CONTA.DIAS. Para formação de licença especial representam os dias de direito do ciclo, distintos do total de serviço certificado.")
  @UISchema(label = "Dias para a finalidade", helpText = "Quantidade destinada ao direito; não confunda com duração do serviço.", group = "Efeitos da contagem", order = 185, type = FieldDataType.NUMBER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "rule", tableHidden = true)
  private Integer diasFinalidade;
  @com.fasterxml.jackson.annotation.JsonSetter("diasFinalidade")
  public void setDiasFinalidade(Integer value) { supplied("diasFinalidade"); this.diasFinalidade=value; }

  @Schema(description = "Indica se o assentamento registra sobreposição de períodos; a marcação não concede aproveitamento em duplicidade.")
  @UISchema(label = "Tempo sobreposto", helpText = "Sinalize coincidência temporal identificada na análise.", group = "Efeitos da contagem", order = 190, type = FieldDataType.BOOLEAN, controlType = FieldControlType.CHECKBOX, icon = "layers", tableHidden = true, preset = UISchemaPreset.BOOLEAN_FLAG)
  private Boolean sobrepoe;
  @com.fasterxml.jackson.annotation.JsonSetter("sobrepoe")
  public void setSobrepoe(Boolean value) { supplied("sobrepoe"); this.sobrepoe=value; }
}
