package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.extension.annotation.UISchemaPreset;

@Getter
@Setter
@ResourceIdentity(keyField = "id", titleField = "descricao", subtitleField = "numeroVaga",
    metadataFields = {"empCodigo", "titularAtualId"})
@Schema(description = "Posições numeradas autorizadas por função, órgão e unidade, com origem e situação de ocupação.")
public class VagaFuncaoResponseDTO extends VagaFuncaoDados {
  @Schema(description = "Identidade estável usada para abrir a ficha deste registro.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Identidade", helpText = "Identificador usado na abertura da ficha.", type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "tag", preset = UISchemaPreset.ENTERPRISE_ID,
      tableHidden = true, formHidden = true, readOnly = true)
  private Long id;

  @Schema(description = "Ocupação cadastral da posição. Vale 1 quando a vaga está ocupada e 0 quando não há ocupação registrada.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @MicroVisualization(kind = MicroVisualizationKind.BULLET, valueExpr = "=ocupacao", targetExpr = "=1",
      ariaLabel = "Ocupação da vaga", fallbackText = "Posição ocupada", thresholds = {
          @Threshold(max = 0, tone = "neutral", label = "Sem ocupação"),
          @Threshold(min = 1, tone = "info", label = "Ocupada")})
  @UISchema(label = "Ocupação", helpText = "Confira a ocupação cadastrada da posição.",
      type = FieldDataType.NUMBER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "event_seat",
      group = "Identificação e situação", tableHidden = false, readOnly = true, editable = false)
  public int getOcupacao() { return "OCUPADA".equals(getSituacao()) ? 1 : 0; }

  @Override
  @ResourceReference(resourceKey = ApiPaths.KEY_VAGAS_FUNCAO, valueField = "id", surfaceId = "detail",
      previewSurfaceId = "detail", detailPresentation = "drawer")
  public String getDescricao() { return super.getDescricao(); }
}
