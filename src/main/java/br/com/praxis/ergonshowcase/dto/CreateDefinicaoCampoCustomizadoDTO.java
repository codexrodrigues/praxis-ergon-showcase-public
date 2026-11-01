package br.com.praxis.ergonshowcase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

@Getter
@Setter
@Schema(
    description =
        "Payload de criação de definição de campo customizado multi-órgão no catálogo governado (substituto de FLEX_CAMPO_01..40).")
public class CreateDefinicaoCampoCustomizadoDTO extends DefinicaoCampoCustomizadoDados {
  @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
  @Schema(
      description =
          "Descrição prevista da tabela geral selecionada para abastecer opções (prévia reativa não persistida).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Finalidade da tabela prevista",
      helpText = "Denominação da tabela geral selecionada em optionTabela.",
      group = "Validação e Governança",
      order = 112,
      readOnly = true,
      tableHidden = true)
  private String optionTabelaDescricaoPrevista;

  @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
  @Schema(
      description =
          "Indica em tempo de formulário se a combinação de tipo e controle exige informar tabela geral de opções.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Exige tabela de opções",
      helpText = "Verdadeiro quando o tipo ou controle exige uma tabela geral de opções.",
      group = "Tipagem e Componente",
      order = 52,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "playlist_add_check",
      readOnly = true,
      tableHidden = true)
  private Boolean requerTabelaOpcoesPrevisto;

  @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
  @Schema(
      description =
          "Indica em tempo de formulário se o campo customizado terá escopo global para todos os órgãos (empCodigo nulo).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Escopo global previsto",
      helpText = "Verdadeiro quando nenhum órgão específico é selecionado.",
      group = "Escopo e Alvo",
      order = 12,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "public",
      readOnly = true,
      tableHidden = true)
  private Boolean escopoGlobalPrevisto;
}
