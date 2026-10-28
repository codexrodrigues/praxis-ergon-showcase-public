package br.com.praxis.ergonshowcase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.extension.annotation.UISchema;

/** Manutenção especializada; propriedades ausentes preservam o valor persistido. */
@Getter
public class AlterarLotacaoExercicioDTO extends org.praxisplatform.uischema.dto.PartialUpdateInput {
  @Schema(nullable = true, description = "Código da unidade de lotação do vínculo no mesmo órgão.")
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

  @Schema(nullable = true, description = "Unidade de efetivo exercício do vínculo no mesmo órgão.")
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

  @Schema(nullable = true, description = "Cargo em comissão ocupado no mesmo órgão do vínculo.")
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

  @Schema(nullable = true, description = "Quantidade de horas semanais de trabalho do vínculo.")
  @UISchema(label = "Jornada semanal", group = "Enquadramento e lotação")
  @jakarta.validation.constraints.Positive
  @jakarta.validation.constraints.Max(168)
  private Integer jornadaSemanal;

  @Schema(nullable = true, description = "Documento legal que fundamenta o assentamento funcional.")
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

  @com.fasterxml.jackson.annotation.JsonSetter(value = "setor", nulls = com.fasterxml.jackson.annotation.Nulls.SET)
  public void setSetor(String value) {
    supplied("setor");
    this.setor = value;
  }

  @com.fasterxml.jackson.annotation.JsonSetter(value = "setorExercicio", nulls = com.fasterxml.jackson.annotation.Nulls.SET)
  public void setSetorExercicio(String value) {
    supplied("setorExercicio");
    this.setorExercicio = value;
  }

  @com.fasterxml.jackson.annotation.JsonSetter(value = "cargoComissao", nulls = com.fasterxml.jackson.annotation.Nulls.SET)
  public void setCargoComissao(Integer value) {
    supplied("cargoComissao");
    this.cargoComissao = value;
  }

  @com.fasterxml.jackson.annotation.JsonSetter(value = "jornadaSemanal", nulls = com.fasterxml.jackson.annotation.Nulls.SET)
  public void setJornadaSemanal(Integer value) {
    supplied("jornadaSemanal");
    this.jornadaSemanal = value;
  }

  @com.fasterxml.jackson.annotation.JsonSetter(value = "documentoLegalId", nulls = com.fasterxml.jackson.annotation.Nulls.SET)
  public void setDocumentoLegalId(Long value) {
    supplied("documentoLegalId");
    this.documentoLegalId = value;
  }
}
