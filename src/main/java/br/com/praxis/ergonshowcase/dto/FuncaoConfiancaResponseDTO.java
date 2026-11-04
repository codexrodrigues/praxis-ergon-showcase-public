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
@ResourceIdentity(keyField = "id", titleField = "descricao", subtitleField = "codigo",
    metadataFields = {"situacao", "dtini", "empCodigo"})
@Schema(description = "Catálogo por órgão de funções de direção, chefia e assessoramento, com vigência, gratificação e política de vagas e substituição.")
public class FuncaoConfiancaResponseDTO extends FuncaoConfiancaDados {
  @Schema(description = "Identidade estável usada para abrir a ficha deste registro.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Identidade", helpText = "Identificador usado na abertura da ficha.", type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "tag", preset = UISchemaPreset.ENTERPRISE_ID,
      tableHidden = true, formHidden = true, readOnly = true)
  private Long id;

  @Override
  @MicroVisualization(kind = MicroVisualizationKind.BULLET, valueExpr = "=diasMinimosSubstituicao",
      ariaLabel = "Dias mínimos de substituição", fallbackText = "Mínimo para remuneração",
      thresholds = {@Threshold(max = 0, tone = "neutral", label = "Sem mínimo"),
          @Threshold(min = 1, tone = "info", label = "Mínimo definido")})
  public Integer getDiasMinimosSubstituicao() { return super.getDiasMinimosSubstituicao(); }

  @Override
  @ResourceReference(resourceKey = ApiPaths.KEY_FUNCOES, valueField = "id", surfaceId = "detail",
      previewSurfaceId = "detail", detailPresentation = "drawer")
  public String getDescricao() { return super.getDescricao(); }
}
