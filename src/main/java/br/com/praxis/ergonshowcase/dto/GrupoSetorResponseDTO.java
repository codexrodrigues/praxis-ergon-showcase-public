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
@Schema(description = "Grupos de setores: inspeção da configuração e da vigência no órgão.")
@ResourceIdentity(keyField = "id", titleField = "nome", subtitleField = "sigla", metadataFields = {"observacao"})
public class GrupoSetorResponseDTO extends GrupoSetorDados {
  @Schema(description = "Identificador imutável deste período; não substitui o código administrativo da unidade.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Período", group = "Configuração organizacional", tableHidden = true, readOnly = true, type = FieldDataType.NUMBER, icon = "numbers")
  private Long id;
  @Override
  @ResourceReference(resourceKey=ApiPaths.KEY_GRUPOS_SETORES,valueField="id",surfaceId="detail",previewSurfaceId="detail",detailPresentation="drawer")
  public String getNome() {return super.getNome();}
  @Override
  @MicroVisualization(kind=MicroVisualizationKind.HARVEY_BALL,valueExpr="=ativo ? 1 : 0",totalExpr="=1",min=0,max=1,thresholds=@Threshold(min=1,tone="success",label="Grupo habilitado"),fallbackText="Habilitação não informada",ariaLabel="Habilitação administrativa do grupo")
  public Boolean getAtivo() {return super.getAtivo();}
}
