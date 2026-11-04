package br.com.praxis.ergonshowcase.controller;
import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.service.SetorParametrizacaoErgonService;
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
@ResourceFact(key = "centroCusto", resourceKey = ApiPaths.KEY_SETORES_ERGON, label = "Centro de custo",
    description = "Classificação de apropriação de custo registrada na parametrização Ergon para esta vigência.",
    dataType = "string", sourceKind = "field")
@ApiResource(value=ApiPaths.SETORES_ERGON, resourceKey=ApiPaths.KEY_SETORES_ERGON, title="Parâmetros funcionais de setores",
 description="Inspeção temporal da estrutura organizacional. Alterações dependem da liberação dos comandos governados.", icon="account_tree",
 identity=@ResourceIdentity(keyField="id",titleField="setorNome",subtitleField="setor",metadataFields={"observacao"}),
 quickFilters={@QuickFilter(id="abertos",label="Sem encerramento",filter="{\"semEncerramento\":true}"),
 @QuickFilter(id="sem-ato",label="Sem fundamento legal",filter="{\"semFundamento\":true}")})
@ApiGroup(value="ergon",title="Administração de pessoal",description="Estrutura organizacional dos órgãos públicos.")
public class SetorParametrizacaoErgonController extends AbstractResourceController<SetorParametrizacaoErgonResponseDTO,Long,SetorParametrizacaoErgonFilterDTO,CreateSetorParametrizacaoErgonDTO,UpdateSetorParametrizacaoErgonDTO> {
 private final SetorParametrizacaoErgonService service;
 public SetorParametrizacaoErgonController(SetorParametrizacaoErgonService service) {this.service=service;}
 @Override protected SetorParametrizacaoErgonService getService() {return service;}
 @Override protected Long getResponseId(SetorParametrizacaoErgonResponseDTO dto) {return dto.getId();}
  @Override
  @PostMapping("/stats/group-by")
  @Operation(summary = "Agrupar parametrizações", description = "Apura registros do órgão autorizado no 