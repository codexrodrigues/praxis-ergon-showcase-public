package br.com.praxis.ergonshowcase.dto;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import org.praxisplatform.uischema.*;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.extension.annotation.*;

@Getter @Setter
@Schema(description = "Parâmetros funcionais de setores: inspeção da configuração e da vigência no órgão.")
@ResourceIdentity(keyField = "id", titleField = "setorNome", subtitleField = "setor", metadataFields = {"observacao"})
public class SetorParametrizacaoErgonResponseDTO extends SetorParametrizacaoErgonDados {
  @Schema(description = "Identificador imutável deste período; não substitui o código administrativo da unidade.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Período", group = "Configuração organizacional", tableHidden = true, readOnly = true, type = FieldDataType.NUMBER, icon = "numbers")
  private Long id;
  @ResourceReference(resourceKey=ApiPaths.KEY_SETORES_ERGON,valueField="id",surfaceId="detail",previewSurfaceId="detail",detailPresentation="drawer")
  @Schema(description = "Denominação obtida da referência do mesmo órgão para identificação humana do assentamento.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Unidade", group = "Configuração organizacional", tableHidden = false, readOnly = true, type = FieldDataType.TEXT, icon = "description")
  private String setorNome;
  @Override
  @MicroVisualization(kind=MicroVisualizationKind.BULLET,valueExpr="percInsalubridade",totalExpr="=100",min=0,max=100,thresholds=@Threshold(min=0,tone="warning",label="Percentual parametrizado"),fallbackText="Adicional não informado",ariaLabel="Percentual de insalubridade da unidade")
  public BigDecimal getPercInsalubridade() {return super.getPercInsalubridade();}
}
