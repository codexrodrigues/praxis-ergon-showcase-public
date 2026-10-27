package br.com.praxis.ergonshowcase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.util.*;
import lombok.Getter;
import lombok.Setter;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.extension.annotation.UISchemaPreset;

@Getter
@Setter
@Schema(
    description =
        "Cadastro civil único das pessoas titulares de vínculos funcionais com órgãos públicos.")
@ResourceIdentity(keyField = "id", titleField = "nome", displayLabelField = "nome")
public class ServidorPessoaResponseDTO extends ServidorPessoaDados {
  @Schema(
      description =
          "Número funcional imutável do cadastro civil, compartilhado pelos vínculos da pessoa.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Identificação",
      preset = UISchemaPreset.ENTERPRISE_ID,
      readOnly = true,
      formHidden = true,
      group = "Identificação",
      order = 0)
  private Long id;

  @Schema(
      description = "Idade civil em anos completos na data atual.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Idade civil em anos completos na data atual",
      group = "Identificação civil",
      tableHidden = true,
      readOnly = true,
      formHidden = true)
  private Integer idadeAnos;

  @Schema(
      description = "Indica ao menos uma das cinco deficiências específicas registrada na pessoa.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Indica ao menos uma das cinco deficiências específicas registrada na pessoa",
      group = "Acessibilidade e PcD",
      tableHidden = true,
      readOnly = true,
      formHidden = true)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.RESTRICTED,
      dataCategory = DomainDataCategory.SENSITIVE_PERSONAL,
      reason = "Indicador derivado de condição de saúde da pessoa.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private Boolean possuiDeficienciaPcD;
}
