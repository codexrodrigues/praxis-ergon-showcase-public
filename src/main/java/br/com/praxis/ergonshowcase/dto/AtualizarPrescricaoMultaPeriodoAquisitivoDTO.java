package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

@Getter
@Setter
@Schema(
    description =
        "Manutenção parcial da apuração de faltas, suspensões, prescrição, multa e fundamentação do período aquisitivo.")
public class AtualizarPrescricaoMultaPeriodoAquisitivoDTO {

  @NotNull
  @Min(0)
  @Max(90)
  @Schema(description = "Total de dias de direito apurado no período aquisitivo.")
  @UISchema(
      label = "Dias de direito",
      helpText = "Quantidade de dias de direito adquiridos no PA.",
      group = "Apuração e Prescrição",
      order = 10,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "beach_access")
  private Integer totDiasDireito;

  @NotNull
  @Min(0)
  @Max(366)
  @Schema(description = "Total de faltas injustificadas apuradas no período aquisitivo.")
  @UISchema(
      label = "Faltas apuradas",
      helpText = "Faltas não abonadas computadas no ciclo aquisitivo.",
      group = "Apuração e Prescrição",
      order = 20,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "event_busy")
  private Integer faltasApuradas;

  @NotNull
  @Min(0)
  @Max(3660)
  @Schema(description = "Total de dias de afastamento suspensivo no período aquisitivo.")
  @UISchema(
      label = "Dias de suspensão",
      helpText = "Dias de afastamento que postergaram o fechamento do PA.",
      group = "Apuração e Prescrição",
      order = 30,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "pause_circle")
  private Integer diasAfastamentoSuspensivo;

  @Schema(description = "Data limite de prescrição do período aquisitivo.")
  @UISchema(
      label = "Data de prescrição",
      helpText = "Data limite para início ou término do gozo.",
      group = "Apuração e Prescrição",
      order = 40,
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "warning")
  private LocalDate dtPrescricao;

  @Pattern(regexp = "I|F")
  @Schema(description = "Critério de avaliação da prescrição (I ou F).")
  @UISchema(
      label = "Critério de prescrição",
      helpText = "Informe I (início do gozo) ou F (término do gozo).",
      group = "Apuração e Prescrição",
      order = 50,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      icon = "warning")
  private String criterioPrescricao;

  @Pattern(regexp = "I|S|N")
  @Schema(description = "Permissão de gozo após a data de prescrição (I, S ou N).")
  @UISchema(
      label = "Gozo após prescrição (PA)",
      helpText = "Informe I (herda), S (permite) ou N (bloqueia).",
      group = "Apuração e Prescrição",
      order = 60,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      icon = "warning")
  private String permiteGozoPosPrescricao;

  @Schema(description = "Data limite para concessão sem incidência de multa.")
  @UISchema(
      label = "Data limite sem multa",
      helpText = "Data a partir da qual incide multa concessiva.",
      group = "Apuração e Prescrição",
      order = 70,
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "gavel")
  private LocalDate dtPgtoComMulta;

  @Pattern(regexp = "I|S|N")
  @Schema(description = "Dispensa de pagamento de multa concessiva (I, S ou N).")
  @UISchema(
      label = "Dispensa de multa (PA)",
      helpText = "Informe I (herda), S (dispensa) ou N (aplica multa).",
      group = "Apuração e Prescrição",
      order = 80,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      icon = "gavel")
  private String semPgtoMulta;

  @Schema(description = "Identificador do documento legal comprobatório.")
  @UISchema(
      label = "Documento comprobatório",
      helpText = "Portaria ou despacho que fundamenta a retificação.",
      group = "Apuração e Prescrição",
      order = 90,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "description",
      endpoint = ApiPaths.DOCUMENTO_OPTIONS,
      valueField = "id",
      displayField = "label")
  private Long documentoLegalId;

  @Size(max = 2000)
  @Schema(description = "Observações sobre a apuração e prescrição do período aquisitivo.")
  @UISchema(
      label = "Observações do período",
      helpText = "Anotações técnicas sobre a retificação do PA.",
      group = "Apuração e Prescrição",
      order = 100,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.TEXTAREA,
      icon = "notes")
  private String observacoes;
}
