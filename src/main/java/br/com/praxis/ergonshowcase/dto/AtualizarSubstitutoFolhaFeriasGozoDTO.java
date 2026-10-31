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
        "Manutenção parcial da competência de folha de pagamento, designação de substituto e atos legais da parcela de férias.")
public class AtualizarSubstitutoFolhaFeriasGozoDTO {

  @Schema(description = "Competência (mês/ano) da folha de pagamento.")
  @UISchema(
      label = "Competência da folha",
      helpText = "Data de competência mensal da folha de pagamento.",
      group = "Folha e Substituto",
      order = 10,
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "receipt_long")
  private LocalDate mesAnoFolha;

  @Min(1)
  @Max(99)
  @Schema(description = "Número sequencial da folha na competência.")
  @UISchema(
      label = "Número da folha",
      helpText = "Sequencial da folha de pagamento.",
      group = "Folha e Substituto",
      order = 20,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "tag")
  private Integer numFolha;

  @Schema(description = "Número funcional do servidor substituto.")
  @UISchema(
      label = "Servidor substituto",
      helpText = "Servidor designado para substituir o titular.",
      group = "Folha e Substituto",
      order = 30,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "supervisor_account",
      endpoint = ApiPaths.PESSOA_OPTIONS,
      valueField = "id",
      displayField = "label")
  private Long numfuncSubstituto;

  @Schema(description = "Número sequencial do vínculo ativo do substituto.")
  @UISchema(
      label = "Vínculo do substituto",
      helpText = "Sequencial do vínculo funcional do substituto.",
      group = "Folha e Substituto",
      order = 40,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "badge")
  private Integer numvincSubstituto;

  @Schema(description = "Identificador da portaria de concessão das férias.")
  @UISchema(
      label = "Documento de concessão",
      helpText = "Portaria de concessão das férias.",
      group = "Folha e Substituto",
      order = 50,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "description",
      endpoint = ApiPaths.DOCUMENTO_OPTIONS,
      valueField = "id",
      displayField = "label")
  private Long documentoLegalId;

  @Schema(description = "Identificador do ato de publicação oficial.")
  @UISchema(
      label = "Ato de publicação",
      helpText = "Publicação em diário oficial.",
      group = "Folha e Substituto",
      order = 60,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "gavel",
      endpoint = ApiPaths.DOCUMENTO_OPTIONS,
      valueField = "id",
      displayField = "label")
  private Long atoPublicacaoId;

  @Size(max = 2000)
  @Schema(description = "Observações operacionais sobre a folha e substituição.")
  @UISchema(
      label = "Observações da parcela",
      helpText = "Anotações administrativas sobre a parcela.",
      group = "Folha e Substituto",
      order = 70,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.TEXTAREA,
      icon = "notes")
  private String observacoes;
}
