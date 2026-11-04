package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
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
@ResourceIdentity(keyField = "id", titleField = "descricao", subtitleField = "substitutoNome",
    metadataFields = {"empCodigo", "competenciaFolha"})
@Schema(description = "Exercícios interinos vinculados à designação titular e ao impedimento, com vigências funcional e remuneratória independentes.")
public class SubstituicaoFuncaoResponseDTO extends SubstituicaoFuncaoDados {
  @Schema(description = "Identidade estável usada para abrir a ficha deste registro.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Identidade", helpText = "Identificador usado na abertura da ficha.", type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "tag", preset = UISchemaPreset.ENTERPRISE_ID,
      tableHidden = true, formHidden = true, readOnly = true)
  private Long id;

  @Schema(description = "Nome civil do servidor que exerce interinamente a função.", accessMode = Schema.AccessMode.READ_ONLY)
  @ResourceReference(resourceKey = "administracao-pessoal.funcionarios", valueField = "substitutoPessoaId",
      surfaceId = "ficha-cadastral", previewSurfaceId = "ficha-cadastral", detailPresentation = "drawer")
  @DomainGovernance(kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL, exportable = false)
  @UISchema(label = "Nome do substituto", helpText = "Abra o vínculo de quem está em exercício.", group = "Identificação e situação",
      type = FieldDataType.TEXT, controlType = FieldControlType.INPUT, icon = "person",
      preset = UISchemaPreset.ENTERPRISE_NAME, tableHidden = false, readOnly = true, editable = false, formHidden = true)
  private String substitutoNome;

  @Schema(description = "Número funcional da pessoa identificada no vínculo de substituto.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @DomainGovernance(kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL, dataCategory = DomainDataCategory.PERSONAL, exportable = false)
  @UISchema(label = "Número funcional", helpText = "Identifica a pessoa na ficha cadastral.",
      type = FieldDataType.NUMBER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "badge",
      preset = UISchemaPreset.ENTERPRISE_ID, tableHidden = true, formHidden = true, readOnly = true)
  private Long substitutoPessoaId;


  @Schema(description = "Quantidade de dias civis do exercício interino, incluindo início e fim.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @MicroVisualization(kind = MicroVisualizationKind.BULLET, valueExpr = "=diasSubstituicao", targetExpr = "=diasSubstituicao",
      ariaLabel = "Dias de exercício", fallbackText = "Duração do exercício", thresholds = {
          @Threshold(max = 0, tone = "neutral", label = "Sem dias"),
          @Threshold(min = 1, tone = "info", label = "Com dias")})
  @UISchema(label = "Dias de exercício", helpText = "Confira a duração do exercício interino.",
      type = FieldDataType.NUMBER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "timer",
      group = "Remuneração", tableHidden = false, readOnly = true, editable = false)
  public Integer getDiasSubstituicao() {
    return inclusiveDays(getDtini(), getDtfim());
  }

  @Schema(description = "Quantidade inclusiva de dias do intervalo remunerado, quando ambas as datas estão registradas.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @MicroVisualization(kind = MicroVisualizationKind.BULLET, valueExpr = "=diasRemunerados", targetExpr = "=diasSubstituicao",
      ariaLabel = "Dias remunerados", fallbackText = "Duração remunerada", thresholds = {
          @Threshold(max = 0, tone = "neutral", label = "Sem dias"),
          @Threshold(min = 1, tone = "info", label = "Com dias")})
  @UISchema(label = "Dias remunerados", helpText = "Compare com a duração total do exercício.",
      type = FieldDataType.NUMBER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "timer",
      group = "Remuneração", tableHidden = true, readOnly = true, editable = false, quickViewVisible = true, quickViewOrder = 5)
  public Integer getDiasRemunerados() {
    return inclusiveDays(getDtiniRemunerada(), getDtfimRemunerada());
  }

  private static Integer inclusiveDays(LocalDate start, LocalDate end) {
    return start == null || end == null ? null : Math.toIntExact(ChronoUnit.DAYS.between(start, end) + 1);
  }

  @Override
  @ResourceReference(resourceKey = ApiPaths.KEY_SUBSTITUICOES, valueField = "id", surfaceId = "detail",
      previewSurfaceId = "detail", detailPresentation = "drawer")
  public String getDescricao() { return super.getDescricao(); }
}
