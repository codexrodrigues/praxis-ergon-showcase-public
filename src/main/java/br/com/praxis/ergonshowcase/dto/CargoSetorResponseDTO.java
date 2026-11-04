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
@Schema(description = "Cargos e quadro por setor: inspeção da configuração e da vigência no órgão.")
@ResourceIdentity(keyField = "id", titleField = "cargoNome", subtitleField = "setor", metadataFields = {"observacao"})
public class CargoSetorResponseDTO extends CargoSetorDados {
  @Schema(description = "Identificador imutável deste período; não substitui o código administrativo da unidade.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Período", group = "Configuração organizacional", tableHidden = true, readOnly = true, type = FieldDataType.NUMBER, icon = "numbers")
  private Long id;
  @ResourceReference(resourceKey = "administracao-pessoal.setores", valueField = "setor", surfaceId = "ficha-unidade", previewSurfaceId = "ficha-unidade", detailPresentation = "drawer")
  @Schema(description = "Denominação obtida da referência do mesmo órgão para identificação humana do assentamento.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Unidade", group = "Configuração organizacional", tableHidden = false, readOnly = true, type = FieldDataType.TEXT, icon = "description")
  private String setorNome;
  @ResourceReference(resourceKey=ApiPaths.KEY_CARGOS_POR_SETOR,valueField="id",surfaceId="detail",previewSurfaceId="detail",detailPresentation="drawer")
  @Schema(description = "Denominação obtida da referência do mesmo órgão para identificação humana do assentamento.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Cargo", group = "Configuração organizacional", tableHidden = false, readOnly = true, type = FieldDataType.TEXT, icon = "description")
  private String cargoNome;
  @Override
  @MicroVisualization(kind=MicroVisualizationKind.BULLET,valueExpr="lotacaoOcupadaAtual",targetExpr="lotacaoMaximaAutorizada",totalExpr="lotacaoMaximaAutorizada",min=0,thresholds=@Threshold(min=0,tone="info",label="Ocupação apurada"),fallbackText="Ocupação ainda não apurada",ariaLabel="Ocupação em relação ao quadro autorizado")
  public Integer getLotacaoOcupadaAtual() {return super.getLotacaoOcupadaAtual();}
}
