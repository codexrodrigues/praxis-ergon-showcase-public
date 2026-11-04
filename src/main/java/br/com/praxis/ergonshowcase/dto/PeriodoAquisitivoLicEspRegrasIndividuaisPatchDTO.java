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
@Schema(description = "Política individual da licença: propriedades ausentes preservam o valor persistido; nulo explícito só é aceito quando o domínio permitir.")
public class PeriodoAquisitivoLicEspRegrasIndividuaisPatchDTO extends org.praxisplatform.uischema.dto.PartialUpdateInput {
  @Min(1) @Max(99)
  @Schema(description = "Anos trabalhados para aquisição neste ciclo. Quando ausente, a apuração posterior deverá consultar o parâmetro estatutário do vínculo.", nullable = true)
  @UISchema(label = "Anos para aquisição", helpText = "Override individual do tempo necessário à aquisição.", group = "Parâmetros individuais", order = 190, type = FieldDataType.NUMBER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "tune", tableHidden = true)
  private Integer anosLicEsp;
  @com.fasterxml.jackson.annotation.JsonSetter("anosLicEsp")
  public void setAnosLicEsp(Integer value) { supplied("anosLicEsp"); this.anosLicEsp=value; }
  @Min(1) @Max(99)
  @Schema(description = "Meses de gozo previstos para o direito deste ciclo, priorizando a definição individual de PERAQLICESP sobre a matriz geral.", nullable = true)
  @UISchema(label = "Meses de direito", helpText = "Override individual da extensão da licença concedida.", group = "Parâmetros individuais", order = 200, type = FieldDataType.NUMBER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "tune", tableHidden = true)
  private Integer mesesLicEspPer;
  @com.fasterxml.jackson.annotation.JsonSetter("mesesLicEspPer")
  public void setMesesLicEspPer(Integer value) { supplied("mesesLicEspPer"); this.mesesLicEspPer=value; }
  @Pattern(regexp = "I|S|N")
  @Schema(description = "Permissão individual de venda: I herda a matriz, S permite e N veda; a precedência por chave do período é documentada na Tarefa 82601.", allowableValues = {"I", "S", "N"})
  @UISchema(label = "Permissão de venda", helpText = "Herdar, permitir ou vedar venda neste ciclo.", group = "Parâmetros individuais", order = 240, type = FieldDataType.TEXT, controlType = FieldControlType.SELECT, icon = "sell", tableHidden = true)
  private String vendLicEsp;
  @com.fasterxml.jackson.annotation.JsonSetter("vendLicEsp")
  public void setVendLicEsp(String value) { supplied("vendLicEsp"); this.vendLicEsp=value; }
  @Pattern(regexp = "D|G|N")
  @Schema(description = "Base de cálculo do abono: D aplica a razão aos dias de direito; G aos dias de gozo; N exclui abono. Ausência preserva a herança do parâmetro geral.", allowableValues = {"D", "G", "N"}, nullable = true)
  @UISchema(label = "Base do abono", helpText = "Direito, gozo ou sem abono; vazio mantém a herança.", group = "Parâmetros individuais", order = 210, type = FieldDataType.TEXT, controlType = FieldControlType.SELECT, icon = "calculate", tableHidden = true)
  private String abonoDirGozoLicEsp;
  @com.fasterxml.jackson.annotation.JsonSetter("abonoDirGozoLicEsp")
  public void setAbonoDirGozoLicEsp(String value) { supplied("abonoDirGozoLicEsp"); this.abonoDirGozoLicEsp=value; }
  @Min(1)
  @Schema(description = "Mínimo de dias que podem ser vendidos por evento neste ciclo, quando houver override individual.", nullable = true)
  @UISchema(label = "Mínimo por venda", helpText = "Piso de dias por evento de venda.", group = "Parâmetros individuais", order = 250, type = FieldDataType.NUMBER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "sell", tableHidden = true)
  private Integer minDiasPvendLicEsp;
  @com.fasterxml.jackson.annotation.JsonSetter("minDiasPvendLicEsp")
  public void setMinDiasPvendLicEsp(Integer value) { supplied("minDiasPvendLicEsp"); this.minDiasPvendLicEsp=value; }
  @Min(0)
  @Schema(description = "Teto de dias vendáveis no período aquisitivo inteiro, distinto do mínimo por evento.", nullable = true)
  @UISchema(label = "Teto de dias vendidos", helpText = "Limite acumulado de vendas deste ciclo.", group = "Parâmetros individuais", order = 260, type = FieldDataType.NUMBER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "sell", tableHidden = true)
  private Integer maxDiasVendLicEsp;
  @com.fasterxml.jackson.annotation.JsonSetter("maxDiasVendLicEsp")
  public void setMaxDiasVendLicEsp(Integer value) { supplied("maxDiasVendLicEsp"); this.maxDiasVendLicEsp=value; }
  @Min(0)
  @Schema(description = "Numerador da razão dias de abono por dias de licença especial, aplicado à base D ou G selecionada.", nullable = true)
  @UISchema(label = "Numerador do abono", helpText = "Parcela superior da proporção de abono.", group = "Parâmetros individuais", order = 220, type = FieldDataType.NUMBER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "calculate", tableHidden = true)
  private Integer numDiasAbonoLicEsp;
  @com.fasterxml.jackson.annotation.JsonSetter("numDiasAbonoLicEsp")
  public void setNumDiasAbonoLicEsp(Integer value) { supplied("numDiasAbonoLicEsp"); this.numDiasAbonoLicEsp=value; }
  @Min(1)
  @Schema(description = "Denominador positivo da razão do abono; zero não constitui proporção válida.", nullable = true)
  @UISchema(label = "Denominador do abono", helpText = "Parcela inferior positiva da proporção de abono.", group = "Parâmetros individuais", order = 230, type = FieldDataType.NUMBER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "calculate", tableHidden = true)
  private Integer denDiasAbonoLicEsp;
  @com.fasterxml.jackson.annotation.JsonSetter("denDiasAbonoLicEsp")
  public void setDenDiasAbonoLicEsp(Integer value) { supplied("denDiasAbonoLicEsp"); this.denDiasAbonoLicEsp=value; }
  @Pattern(regexp = "I|S|N")
  @Schema(description = "Permissão individual para conversão em dobro: I herda, S permite e N veda, conforme funções da Tarefa 58924 e precedência da Tarefa 82601.", allowableValues = {"I", "S", "N"})
  @UISchema(label = "Permissão de contagem em dobro", helpText = "Herdar, permitir ou vedar conversão neste ciclo.", group = "Parâmetros individuais", order = 270, type = FieldDataType.TEXT, controlType = FieldControlType.SELECT, icon = "more_time", tableHidden = true)
  private String contDobroLicEsp;
  @com.fasterxml.jackson.annotation.JsonSetter("contDobroLicEsp")
  public void setContDobroLicEsp(String value) { supplied("contDobroLicEsp"); this.contDobroLicEsp=value; }
  @Min(1)
  @Schema(description = "Mínimo de dias originais de licença convertíveis em dobro por evento neste ciclo.", nullable = true)
  @UISchema(label = "Mínimo por conversão", helpText = "Piso de dias originais por conversão.", group = "Parâmetros individuais", order = 280, type = FieldDataType.NUMBER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "more_time", tableHidden = true)
  private Integer minDiasPcontDobLicEsp;
  @com.fasterxml.jackson.annotation.JsonSetter("minDiasPcontDobLicEsp")
  public void setMinDiasPcontDobLicEsp(Integer value) { supplied("minDiasPcontDobLicEsp"); this.minDiasPcontDobLicEsp=value; }
  @Min(0)
  @Schema(description = "Máximo acumulado de dias originais convertíveis em dobro por período aquisitivo.", nullable = true)
  @UISchema(label = "Teto de conversão em dobro", helpText = "Teto de dias originais convertidos no ciclo.", group = "Parâmetros individuais", order = 290, type = FieldDataType.NUMBER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "more_time", tableHidden = true)
  private Integer maxDiasContDobLicEsp;
  @com.fasterxml.jackson.annotation.JsonSetter("maxDiasContDobLicEsp")
  public void setMaxDiasContDobLicEsp(Integer value) { supplied("maxDiasContDobLicEsp"); this.maxDiasContDobLicEsp=value; }

  @Schema(description = "Política individual de fruição antes do término da aquisição: HERDAR usa a matriz estatutária, PERMITIR autoriza e VEDAR impede antecipação. A escolha não executa a concessão.", type = "string", implementation = String.class, allowableValues = {"HERDAR", "PERMITIR", "VEDAR"}, defaultValue = "HERDAR")
  @UISchema(label = "Gozo antes da aquisição", helpText = "Escolha herdar a regra, permitir ou vedar antecipação.", group = "Parâmetros individuais", order = 300, type = FieldDataType.TEXT, controlType = FieldControlType.SELECT, options = "[{\"value\":\"HERDAR\",\"label\":\"Herdar a regra\"},{\"value\":\"PERMITIR\",\"label\":\"Permitir\"},{\"value\":\"VEDAR\",\"label\":\"Vedar\"}]", icon = "event_available", tableHidden = true, preset = UISchemaPreset.ENTERPRISE_STATUS)
  private br.com.praxis.ergonshowcase.domain.PoliticaAntecipacaoLicenca permGozoLicEspAntFimPe;
  @com.fasterxml.jackson.annotation.JsonSetter("permGozoLicEspAntFimPe")
  public void setPermGozoLicEspAntFimPe(br.com.praxis.ergonshowcase.domain.PoliticaAntecipacaoLicenca value) { supplied("permGozoLicEspAntFimPe"); this.permGozoLicEspAntFimPe=value; }
}
