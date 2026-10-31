package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.annotation.AiTrainingUseMode;
import org.praxisplatform.uischema.annotation.AiUsagePolicy;
import org.praxisplatform.uischema.annotation.AiVisibilityMode;
import org.praxisplatform.uischema.annotation.DomainClassification;
import org.praxisplatform.uischema.annotation.DomainDataCategory;
import org.praxisplatform.uischema.annotation.DomainGovernance;
import org.praxisplatform.uischema.annotation.DomainGovernanceKind;
import org.praxisplatform.uischema.dto.PartialUpdateInput;
import org.praxisplatform.uischema.extension.annotation.UISchema;

/**
 * Manutenção parcial dos indicadores de elegibilidade tributária, previdenciária e assistencial do
 * dependente; propriedades ausentes preservam o valor persistido.
 */
@Getter
@Schema(
    description =
        "Manutenção parcial dos indicadores de elegibilidade tributária, previdenciária e assistencial do dependente.")
public class AtualizarElegibilidadeDependenteDTO extends PartialUpdateInput {
  @Size(max = 20)
  @Schema(
      nullable = true,
      description = "Classificação legal do tipo de dependência no catálogo TIPO_DEPEN.")
  @UISchema(
      label = "Tipo de dependência",
      group = "Elegibilidade",
      order = 1,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "category",
      endpoint = ApiPaths.TIPO_DEPENDENTE_OPTIONS,
      valueField = "id",
      displayField = "label")
  private String tipodepen;

  @Schema(description = "Indica se o dependente está habilitado para dedução de IRRF.")
  @UISchema(
      label = "Deduz IRRF",
      group = "Elegibilidade",
      order = 2,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "receipt_long")
  private Boolean depIr;

  @Schema(description = "Indica se o dependente gera direito à cota de salário-família.")
  @UISchema(
      label = "Salário-família",
      group = "Elegibilidade",
      order = 3,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "savings")
  private Boolean depSalarioFamilia;

  @Schema(description = "Indica se o dependente é beneficiário do plano de saúde suplementar.")
  @UISchema(
      label = "Plano de saúde",
      group = "Elegibilidade",
      order = 4,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "health_and_safety")
  private Boolean depPlanoSaude;

  @Schema(description = "Indica se o dependente cursa ensino regular comprovado.")
  @UISchema(
      label = "Estudante regular",
      group = "Elegibilidade",
      order = 5,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "school")
  private Boolean estudante;

  @Schema(
      description =
          "Indica se o dependente cursa ensino superior ou técnico (extensão até 24 anos).")
  @UISchema(
      label = "Universitário",
      group = "Elegibilidade",
      order = 6,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "account_balance")
  private Boolean universitario;

  @Schema(description = "Indica condição excepcional para benefício assistencial estatutário.")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.RESTRICTED,
      dataCategory = DomainDataCategory.SENSITIVE_PERSONAL,
      reason = "Condição clínica excepcional do dependente.",
      aiUsage =
          @AiUsagePolicy(
              visibility = AiVisibilityMode.MASK,
              trainingUse = AiTrainingUseMode.DENY))
  @UISchema(
      label = "Condição excepcional",
      group = "Elegibilidade",
      order = 7,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "accessible")
  private Boolean excepcional;

  @Schema(description = "Indica se o registro envolve tutela, curatela ou representante legal.")
  @UISchema(
      label = "Representante legal",
      group = "Elegibilidade",
      order = 8,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "gavel")
  private Boolean representanteLegal;

  @Positive
  @Schema(
      nullable = true,
      description = "Documento comprobatório ou ato legal que fundamenta a revisão.")
  @UISchema(
      label = "Documento comprobatório",
      group = "Fundamentação",
      order = 9,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "description",
      endpoint = ApiPaths.DOCUMENTO_OPTIONS,
      valueField = "id",
      displayField = "label")
  private Long documentoLegalId;

  @JsonSetter(value = "tipodepen", nulls = Nulls.SET)
  public void setTipodepen(String value) {
    supplied("tipodepen");
    this.tipodepen = value;
  }

  @JsonSetter(value = "depIr", nulls = Nulls.SET)
  public void setDepIr(Boolean value) {
    supplied("depIr");
    this.depIr = value;
  }

  @JsonSetter(value = "depSalarioFamilia", nulls = Nulls.SET)
  public void setDepSalarioFamilia(Boolean value) {
    supplied("depSalarioFamilia");
    this.depSalarioFamilia = value;
  }

  @JsonSetter(value = "depPlanoSaude", nulls = Nulls.SET)
  public void setDepPlanoSaude(Boolean value) {
    supplied("depPlanoSaude");
    this.depPlanoSaude = value;
  }

  @JsonSetter(value = "estudante", nulls = Nulls.SET)
  public void setEstudante(Boolean value) {
    supplied("estudante");
    this.estudante = value;
  }

  @JsonSetter(value = "universitario", nulls = Nulls.SET)
  public void setUniversitario(Boolean value) {
    supplied("universitario");
    this.universitario = value;
  }

  @JsonSetter(value = "excepcional", nulls = Nulls.SET)
  public void setExcepcional(Boolean value) {
    supplied("excepcional");
    this.excepcional = value;
  }

  @JsonSetter(value = "representanteLegal", nulls = Nulls.SET)
  public void setRepresentanteLegal(Boolean value) {
    supplied("representanteLegal");
    this.representanteLegal = value;
  }

  @JsonSetter(value = "documentoLegalId", nulls = Nulls.SET)
  public void setDocumentoLegalId(Long value) {
    supplied("documentoLegalId");
    this.documentoLegalId = value;
  }
}
