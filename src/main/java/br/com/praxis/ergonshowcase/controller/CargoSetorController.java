package br.com.praxis.ergonshowcase.controller;
import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.service.CargoSetorService;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.controller.base.AbstractResourceController;
import org.praxisplatform.uischema.surface.*;
import org.praxisplatform.uischema.stats.dto.GroupByStatsRequest;
import org.praxisplatform.uischema.stats.dto.GroupByStatsResponse;
import org.praxisplatform.uischema.fact.ResourceFact;
import org.praxisplatform.uischema.rest.response.RestApiResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.*;
@ResourceFact(key = "lotacaoMaximaAutorizada", resourceKey = ApiPaths.KEY_CARGOS_POR_SETOR, label = "Limite autorizado",
    description = "Limite de lotação para este cargo e vigência. O indicador abrange subordinados somente quando a autorização assim determina.",
    dataType = "integer", unit = "vagas", sourceKind = "field")
@ApiResource(value=ApiPaths.CARGOS_POR_SETOR, resourceKey=ApiPaths.KEY_CARGOS_POR_SETOR, title="Cargos e quadro por setor",
 description="Inspeção temporal da estrutura organizacional. Alterações dependem da liberação dos comandos governados.", icon="account_tree",
 identity=@ResourceIdentity(keyField="id",titleField="cargoNome",subtitleField="setor",metadataFields={"observacao"}),
 quickFilters={@QuickFilter(id="abertos",label="Sem encerramento",filter="{\"semEncerramento\":true}"),
 @QuickFilter(id="sem-ato",label="Sem fundamento legal",filter="{\"semFundamento\":true}")})
@ApiGroup(value="ergon",title="Administração de pessoal",description="Estrutura organizacional dos órgãos públicos.")
public class CargoSetorController extends AbstractResourceController<CargoSetorResponseDTO,Long,CargoSetorFilterDTO,CreateCargoSetorDTO,UpdateCargoSetorDTO> {
 private final CargoSetorService service;
 public CargoSetorController(CargoSetorService service) {this.service=service;}
 @Override protected CargoSetorService getService() {return service;}
 @Override protected Long getResponseId(CargoSetorResponseDTO dto) {return dto.getId();}
  @Override
  @PostMapping("/stats/group-by")
  @Operation(summary = "Agrupa