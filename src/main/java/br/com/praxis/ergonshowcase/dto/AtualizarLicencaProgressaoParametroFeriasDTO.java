package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

@Getter
@Setter
@Schema(
    description =
        "Manutenção parcial das regras de prescrição, contagem em dobro, licença especial, progressão funcional e fundamentação legal.")
public class AtualizarLicencaProgressaoParametroFeriasDTO {

  @Pattern(regexp = "I|F")
  @Schema(
      description = "Critério de prescrição: I (data inicial do gozo) ou F (data final do gozo).",
      allowableValues = {"I", "F"})
  @UISchema(
      label = "Critério de prescrição",
      helpText = "Use I para validar pelo início do gozo ou F pelo término.",
      group = "Prescrição e Dobro",
      order = 10,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      options =
          "[{\"value\":\"I\",\"label\":\"Data inicial do gozo\"},{\"value\":\"F\",\"label\":\"Data final do gozo\"}]",
      icon = "warning")
  private String criterioPrescricao;

  @NotNull
  @Schema(description = "Indica se permite registrar gozo de férias após a data de prescrição.")
  @UISchema(
      label = "Permite gozo após prescrição",
      helpText = "Autoriza gozo mesmo após vencida a data de prescrição.",
      group = "Prescrição e Dobro",
      order = 20,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "warning")
  private Boolean permiteGozoAposPrescricao = false;

  @NotNull
  @Schema(description = "Indica se permite converter férias não gozadas em contagem em dobro.")
  @UISchema(
      label = "Permite contagem em dobro",
      helpText = "Autoriza averbar dias de férias não gozados em dobro.",
      group = "Prescrição e Dobro",
      order = 30,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "trending_up")
  private Boolean contDobroFerias = false;

  @Min(0)
  @Max(90)
  @Schema(description = "Mínimo de dias do PA para conversão em contagem em dobro.")
  @UISchema(
      label = "Mínimo para contagem em dobro",
      helpText = "Menor saldo de dias do PA conversível em dobro.",
      group = "Prescrição e Dobro",
      order = 40,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "trending_up")
  private Integer minDiasPeraqfer;

  @Min(0)
  @Max(90)
  @Schema(description = "Máximo de dias do PA para conversão em contagem em dobro.")
  @UISchema(
      label = "Máximo para contagem em dobro",
      helpText = "Maior saldo de dias do PA conversível em dobro.",
      group = "Prescrição e Dobro",
      order = 50,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "trending_up")
  private Integer maxDiasPeraqfer;

  @NotNull
  @Schema(description = "Indica se o enquadramento confere direito a licença especial.")
  @UISchema(
      label = "Possui licença especial",
      helpText = "Habilita apuração de períodos de licença especial.",
      group = "Licença Especial e Progressão",
      order = 60,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "workspace_premium")
  private Boolean temLicEsp = false;

  @Min(1)
  @Max(30)
  @Schema(description = "Quantidade de anos exigida por período de licença especial.")
  @UISchema(
      label = "Anos por licença especial",
      helpText = "Interstício em anos para aquisição de licença especial.",
      group = "Licença Especial e Progressão",
      order = 70,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "schedule")
  private Integer anosLicEsp;

  @Min(1)
  @Max(24)
  @Schema(description = "Quantidade de meses de direito por período de licença especial.")
  @UISchema(
      label = "Meses de direito da licença",
      helpText = "Duração em meses concedida por licença especial.",
      group = "Licença Especial e Progressão",
      order = 80,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "calendar_month")
  private Integer mesesLicEspPer;

  @NotNull
  @Schema(description = "Indica se o enquadramento habilita apuração de progressão funcional.")
  @UISchema(
      label = "Possui progressão funcional",
      helpText = "Habilita contagem de tempo para progressão na carreira.",
      group = "Licença Especial e Progressão",
      order = 90,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "trending_up")
  private Boolean temProgressao = false;

  @Min(1)
  @Max(30)
  @Schema(description = "Interstício em anos exigido para progressão funcional.")
  @UISchema(
      label = "Anos para progressão",
      helpText = "Quantidade de anos exigida para progressão funcional.",
      group = "Licença Especial e Progressão",
      order = 100,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "schedule")
  private Integer anosProgressao;

  @Schema(description = "Identificador do ato legal que fundamenta a regra de férias.")
  @UISchema(
      label = "Ato legal de férias",
      helpText = "Legislação ou estatuto das regras de férias.",
      group = "Fundamentação",
      order = 110,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "description",
      endpoint = ApiPaths.DOCUMENTO_OPTIONS,
      valueField = "id",
      displayField = "label")
  private Long documentoLegalFeriasId;

  @Schema(description = "Identificador do ato legal que fundamenta a progressão funcional.")
  @UISchema(
      label = "Ato legal de progressão",
      helpText = "Legislação que regulamenta a progressão funcional.",
      group = "Fundamentação",
      order = 120,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "gavel",
      endpoint = ApiPaths.DOCUMENTO_OPTIONS,
      valueField = "id",
      displayField = "label")
  private Long documentoLegalProgressaoId;

  @Size(max = 2000)
  @Schema(description = "Observações técnicas e diretrizes normativas sobre a regra.")
  @UISchema(
      label = "Observações normativas",
      helpText = "Anotações interpretativas sobre a parametrização.",
      group = "Fundamentação",
      order = 130,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.TEXTAREA,
      icon = "notes")
  private String observacoes;
}
