package br.com.praxis.ergonshowcase.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalTime;
import lombok.Getter;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.dto.PartialUpdateInput;
import org.praxisplatform.uischema.extension.annotation.UISchema;

@Getter
@Schema(
    description =
        "Manutenção parcial especializada para retificação de horários de entrada, saída e quantidade apurada na frequência.")
public class AtualizarHorarioFrequenciaDTO extends PartialUpdateInput {

  @Schema(description = "Horário de entrada retificado na ocorrência.")
  @UISchema(
      label = "Hora de entrada",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.TIME_PICKER,
      icon = "login",
      group = "Apuração e Horários",
      order = 10,
      helpText = "Horário inicial apurado na ocorrência.")
  private LocalTime horaEntrada;

  @Schema(description = "Horário de saída retificado na ocorrência.")
  @UISchema(
      label = "Hora de saída",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.TIME_PICKER,
      icon = "logout",
      group = "Apuração e Horários",
      order = 20,
      helpText = "Horário final apurado na ocorrência.")
  private LocalTime horaSaida;

  @Schema(description = "Quantidade apurada retificada em dias, horas ou minutos.")
  @UISchema(
      label = "Quantidade apurada",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "calculate",
      group = "Apuração e Horários",
      order = 30,
      helpText = "Quantidade total apurada após revisão de ponto.")
  private BigDecimal quantidade;

  @JsonSetter("horaEntrada")
  public void setHoraEntrada(LocalTime horaEntrada) {
    this.horaEntrada = horaEntrada;
    supplied("horaEntrada");
  }

  @JsonSetter("horaSaida")
  public void setHoraSaida(LocalTime horaSaida) {
    this.horaSaida = horaSaida;
    supplied("horaSaida");
  }

  @JsonSetter("quantidade")
  public void setQuantidade(BigDecimal quantidade) {
    this.quantidade = quantidade;
    supplied("quantidade");
  }
}
