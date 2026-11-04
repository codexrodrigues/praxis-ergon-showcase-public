package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.temporal.ChronoUnit;
import lombok.Getter;
import lombok.Setter;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.extension.annotation.UISchemaPreset;

@Getter
@Setter
@ResourceIdentity(keyField = "id", titleField = "descricao", subtitleField = "titularNome",
    metadataFields = {"empCodigo", "dtini", "dtfim"})
@Schema(description = "Atos individuais de designação, exercício e dispensa em função de confiança, incluindo opção remuneratória e ordem de substitutos.")
public class DesignacaoFuncaoResponseDTO extends DesignacaoFuncaoDados {
  @Schema(description = "Identidade estável usada para abrir a ficha deste registro.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Identidade", helpText = "Identificador usado na abertura da ficha.", type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "tag", preset = UISchemaPreset.ENTERPRISE_ID,
      tableHidden = true, formHidden = true, readOnly = true)
  private Long id;

  @Schema(description = "Nome civil do servidor vinculado ao ato de designação.", accessMode = Schema.AccessMode.READ_ONLY)
  @ResourceReference(resourceKey = "administracao-pessoal.funcionarios", valueField = "titularPessoaId",
      surfaceId = "ficha-cadastral", previewSurfaceId = "ficha-cadastral", detailPresentation = "drawer")
  @DomainGovernance(kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL, exportable = false)
  @UISchema(label = "Nome do titular", helpText = "Abra o resumo funcional do titular.", group = "Identificação e situação",
      type = FieldDataType.TEXT, controlType = FieldControlType.INPUT, icon = "person",
      preset = UISchemaPreset.ENTERPRISE_NAME, tableHidden = false, readOnly = true, editable = false, formHidden = true)
  private String titularNome;

  @Schema(description = "Número funcional da pessoa identificada no vínculo de titular.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @DomainGovernance(kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL, dataCategory = DomainDataCategory.PERSONAL, exportable = false)
  @UISchema(label = "Número funcional", helpText = "Identifica a pessoa na ficha cadastral.",
      type = FieldDataType.NUMBER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "badge",
      preset = UISchemaPreset.ENTERPRISE_ID, tableHidden = true, formHidden = true, readOnly = true)
  private Long titularPessoaId;

  @Schema(description = "Dias civis inclusivos entre início e fim do ato. Fica sem valor para vigência aberta.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Dias de vigência", helpText = "Consulte a duração da vigência registrada no ato.",
      group = "Identificação e situação", type = FieldDataType.NUMBER, controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "date_range", tableHidden = false, readOnly = true, editable = false, formHidden = true)
  @MicroVisualization(kind = MicroVisualizationKind.BULLET, valueExpr = "=diasVigencia",
      ariaLabel = "Dias de vigência da designação", fallbackText = "Vigência sem duração fechada",
      thresholds = @Threshold(min = 1, tone = "info", label = "Vigência delimitada"))
  public Integer getDiasVigencia() {
    if (getDtini() == null || getDtfim() == null || getDtfim().isBefore(getDtini())) return null;
    return Math.toIntExact(ChronoUnit.DAYS.between(getDtini(), getDtfim()) + 1);
  }

  @Override
  @ResourceReference(resourceKey = ApiPaths.KEY_DESIGNACOES, valueField = "id", surfaceId = "detail",
      previewSurfaceId = "detail", detailPresentation = "drawer")
  public String getDescricao() { return super.getDescricao(); }
}
