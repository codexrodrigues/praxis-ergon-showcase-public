package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.util.Map;
import lombok.Getter;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.dto.PartialUpdateInput;
import org.praxisplatform.uischema.extension.annotation.UISchema;

@Getter
@Schema(
    description =
        "Manutenção parcial especializada para obrigatoriedade, tabela de opções, regras de validação e política de governança do campo customizado.")
public class AtualizarGovernancaCampoCustomizadoDTO extends PartialUpdateInput {

  @Schema(description = "Indica se o preenchimento do campo customizado é obrigatório.")
  @UISchema(
      label = "Obrigatório",
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "priority_high",
      group = "Validação e Governança",
      order = 10,
      helpText = "Torna o campo obrigatório no recurso alvo.")
  private Boolean required;

  @Size(max = 30)
  @Schema(description = "Tabela geral de domínio (HADES.TABELA) que fornece opções ao campo.")
  @UISchema(
      label = "Tabela de opções",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "table_view",
      group = "Validação e Governança",
      order = 20,
      endpoint = ApiPaths.TABELA_OPTIONS,
      valueField = "id",
      displayField = "label",
      helpText = "Tabela geral que alimenta a lista de opções.")
  private String optionTabela;

  @Schema(description = "Mapa estruturado de restrições de validação (minLength, maxLength, pattern).")
  @UISchema(
      label = "Regras de validação",
      type = FieldDataType.JSON,
      controlType = FieldControlType.TEXTAREA,
      icon = "rule",
      group = "Validação e Governança",
      order = 30,
      helpText = "Restrições determinísticas de validação.")
  private Map<String, Object> validationRules;

  @Schema(description = "Política de governança semântica, LGPD e elegibilidade de exportação/IA.")
  @UISchema(
      label = "Política de governança",
      type = FieldDataType.JSON,
      controlType = FieldControlType.TEXTAREA,
      icon = "policy",
      group = "Validação e Governança",
      order = 40,
      helpText = "Classificação de sensibilidade e mascaramento.")
  private Map<String, Object> governancePolicy;

  @JsonSetter("required")
  public void setRequired(Boolean required) {
    this.required = required;
    supplied("required");
  }

  @JsonSetter("optionTabela")
  public void setOptionTabela(String optionTabela) {
    this.optionTabela = optionTabela;
    supplied("optionTabela");
  }

  @JsonSetter("validationRules")
  public void setValidationRules(Map<String, Object> validationRules) {
    this.validationRules = validationRules;
    supplied("validationRules");
  }

  @JsonSetter("governancePolicy")
  public void setGovernancePolicy(Map<String, Object> governancePolicy) {
    this.governancePolicy = governancePolicy;
    supplied("governancePolicy");
  }
}
