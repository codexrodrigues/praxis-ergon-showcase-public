package br.com.praxis.ergonshowcase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

@Getter
@Setter
@Schema(
    description =
        "Resultado da determinação reativa para preenchimento assistido do formulário de campo customizado.")
public class CampoCustomizadoPreviaPreviewDTO {

  @Schema(description = "Descrição da tabela geral de domínio selecionada, quando informada.")
  @UISchema(
      label = "Finalidade da tabela prevista",
      group = "Prévia Reativa",
      order = 10,
      readOnly = true,
      helpText = "Descrição oficial da tabela geral vinculada.")
  private String optionTabelaDescricaoPrevista;

  @Schema(
      description =
          "Indica se a combinação de tipo e controle exige informar tabela geral de opções.")
  @UISchema(
      label = "Exige tabela de opções",
      group = "Prévia Reativa",
      order = 20,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "playlist_add_check",
      readOnly = true,
      helpText = "Verdadeiro quando o tipo ou controle exige uma tabela geral de opções.")
  private Boolean requerTabelaOpcoesPrevisto;

  @Schema(
      description =
          "Indica se o campo customizado terá escopo global para todos os órgãos (empCodigo nulo).")
  @UISchema(
      label = "Escopo global previsto",
      group = "Prévia Reativa",
      order = 30,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "public",
      readOnly = true,
      helpText = "Verdadeiro quando nenhum órgão específico é selecionado.")
  private Boolean escopoGlobalPrevisto;
}
