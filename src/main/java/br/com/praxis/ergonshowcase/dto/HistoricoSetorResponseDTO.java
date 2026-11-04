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
@Schema(description = "Históricos de setores: inspeção da configuração e da vigência no órgão.")
@ResourceIdentity(keyField = "id", titleField = "nome", subtitleField = "sigla", metadataFields = {"observacao"})
public class HistoricoSetorResponseDTO extends HistoricoSetorDados {
  @Schema(description="Informa se o período participa de uma árvore íntegra na data consultada, ou se há ciclo ou superior ausente; na inspeção sem data usa o início do período.",accessMode=Schema.AccessMode.READ_ONLY)
  @UISchema(label="Integridade da hierarquia",group="Organograma",tableHidden=true,readOnly=true,icon="account_tree",preset=UISchemaPreset.ENTERPRISE_STATUS)
  private String diagnosticoHierarquia;
  @Schema(description="Quantidade de filhos imediatos na hierarquia e data selecionadas; ausente quando a consulta não seleciona uma composição temporal.",accessMode=Schema.AccessMode.READ_ONLY)
  @UISchema(label="Subordinados diretos",group="Organograma",tableHidden=true,readOnly=true,type=FieldDataType.NUMBER,icon="account_tree")
  private Integer quantidadeSubordinadosDiretos;
  @Schema(description = "Identificador imutável deste período; não substitui o código administrativo da unidade.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Período", group = "Configuração organizacional", tableHidden = true, readOnly = true, type = FieldDataType.NUMBER, icon = "numbers")
  private Long id;
  @Override
  @ResourceReference(resourceKey=ApiPaths.KEY_HISTORICO_SETORES,valueField="id",surfaceId="detail",previewSurfaceId="detail",detailPresentation="drawer")
  public String getNome() {return super.getNome();}
  @Override
  @MicroVisualization(kind=MicroVisualizationKind.HARVEY_BALL,valueExpr="=extinto ? 1 : 0",totalExpr="=1",min=0,max=1,thresholds=@Threshold(min=1,tone="neutral",label="Unidade extinta"),fallbackText="Estado administrativo não informado",ariaLabel="Extinção administrativa na vigência")
  public Boolean getExtinto() {return super.getExtinto();}
}
