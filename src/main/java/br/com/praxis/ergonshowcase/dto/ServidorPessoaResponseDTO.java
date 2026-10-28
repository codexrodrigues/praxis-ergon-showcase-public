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
@ResourceIdentity(
    keyField = "id",
    titleField = "nome",
    displayLabelField = "nome",
    subtitleField = "nomeSocial",
    metadataFields = {"id", "escolaridade", "cidadeender", "ufender"})
public class ServidorPessoaResponseDTO extends ServidorPessoaDados {
  @Schema(description = "Quantidade de canais preenchidos entre telefone, celular e e-mail (zero a três). Mede presença cadastral, não validade, consentimento ou possibilidade de contato.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Canais de contato", group = "Endereço e contato", readOnly = true,
      formHidden = true, icon = "contact_phone", helpText = "Presença de telefone, celular e e-mail; os valores não são exibidos neste indicador.")
  @DomainGovernance(kind = DomainGovernanceKind.PRIVACY, classification = DomainClassification.RESTRICTED,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Indicador derivado dos meios de contato pessoal.",
      aiUsage = @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @MicroVisualization(kind = MicroVisualizationKind.HARVEY_BALL, valueExpr = "canaisContatoPreenchidos",
      total = 3, min = 0, max = 3, compactValue = true,
      thresholds = {@Threshold(equalsValue = "0", tone = "warning", label = "Sem canal informado"),
          @Threshold(min = 1, max = 3, tone = "info", label = "Canal informado")},
      fallbackText = "Quantidade de canais de contato preenchidos, de zero a três",
      ariaLabel = "Presença cadastral de canais de contato")
  public Integer getCanaisContatoPreenchidos() {
    return (getTelefone() == null || getTelefone().isBlank() ? 0 : 1)
        + (getNumtelCelular() == null || getNumtelCelular().isBlank() ? 0 : 1)
        + (getEMail() == null || getEMail().isBlank() ? 0 : 1);
  }
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
      label = "Idade",
      group = "Identificação civil",
      tableHidden = true,
      readOnly = true,
      formHidden = true)
  private Integer idadeAnos;

  @Schema(
      description = "Indica ao menos uma das cinco deficiências específicas registrada na pessoa.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Pessoa com deficiência",
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
