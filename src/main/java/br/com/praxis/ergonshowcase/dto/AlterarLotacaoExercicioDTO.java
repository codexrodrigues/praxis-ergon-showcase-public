package br.com.praxis.ergonshowcase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.extension.annotation.UISchema;

/** Manutenção especializada; propriedades ausentes preservam o valor persistido. */
@Getter
@Setter
public class AlterarLotacaoExercicioDTO {
  @Schema(description = "Código da unidade de lotação do vínculo no mesmo órgão.")
  @UISchema(
      label = "Lotação",
      group = "Enquadramento e lotação",
      type = org.praxisplatform.uischema.FieldDataType.TEXT,
      controlType = org.praxisplatform.uischema.FieldControlType.ENTITY_LOOKUP,
      icon = "account_tree",
      endpoint = "/api/ergon/setores/option-sources/setor/options/filter",
      valueField = "id",
      displayField = "label")
  @jakarta.validation.constraints.Size(max = 30)
  private String setor;

  @Schema(description = "Unidade de efetivo exercício do vínculo no mesmo órgão.")
  @UISchema(
      label = "Exercício",
      group = "Enquadramento e lotação",
      type = org.praxisplatform.uischema.FieldDataType.TEXT,
      controlType = org.praxisplatform.uischema.FieldControlType.ENTITY_LOOKUP,
      icon = "business",
      endpoint = "/api/ergon/setores/option-sources/setor/options/filter",
      valueField = "id",
      displayField = "label")
  @jakarta.validation.constraints.Size(max = 30)
  private String setorExercicio;

  @Schema(description = "Cargo em comissão ocupado no mesmo órgão do vínculo.")
  @UISchema(
      label = "Cargo em comissão",
      group = "Enquadramento e lotação",
      type = org.praxisplatform.uischema.FieldDataType.TEXT,
      controlType = org.praxisplatform.uischema.FieldControlType.ENTITY_LOOKUP,
      icon = "badge",
      endpoint = "/api/ergon/cargos/option-sources/cargo/options/filter",
      valueField = "id",
      displayField = "label")
  private Integer cargoComissao;

  @Schema(description = "Quantidade de horas semanais de trabalho do vínculo.")
  @UISchema(label = "Jornada semanal", group = "Enquadramento e lotação")
  @jakarta.validation.constraints.Positive
  @jakarta.validation.constraints.Max(168)
  private Integer jornadaSemanal;

  @Schema(description = "Documento legal que fundamenta o assentamento funcional.")
  @UISchema(
      label = "Documento legal",
      group = "Enquadramento e lotação",
      type = org.praxisplatform.uischema.FieldDataType.TEXT,
      controlType = org.praxisplatform.uischema.FieldControlType.ENTITY_LOOKUP,
      icon = "gavel",
      endpoint = "/api/ergon/documentos-legais/option-sources/documento/options/filter",
      valueField = "id",
      displayField = "label")
  private Long documentoLegalId;
}
