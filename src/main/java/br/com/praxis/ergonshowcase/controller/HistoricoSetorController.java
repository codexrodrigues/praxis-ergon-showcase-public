package br.com.praxis.ergonshowcase.controller;
import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.service.HistoricoSetorService;
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
@ResourceHierarchy(parentField="setorPai", hierarchyPathField="caminhoHierarquico", levelField="nivelHierarquico", extinctField="extinto", auxiliaryParentFields={"setorPaiHier1","setorPaiHier2","setorPaiHier3","setorPaiHier4"})
@ResourceFact(key = "diagnosticoHierarquia", resourceKey = ApiPaths.KEY_HISTORICO_SETORES, label = "Integridade da hierarquia",
    description = "Indica se a composição temporal resolveu o caminho da unidade ou encontrou ciclo ou superior ausente.",
    dataType = "string", sourceKind = "field")
@ApiResource(value=ApiPaths.HISTORICO_SETORES, resourceKey=ApiPaths.KEY_HISTORICO_SETORES, title="Históricos de setores",
 description="Inspeção temporal da estrutura organizacional. Alterações dependem da liberação dos comandos governados.", icon="account_tree",
 identity=@ResourceIdentity(keyField="id",titleField="nome",subtitleField="sigla",metadataFields={"observacao"}),
 quickFilters={@QuickFilter(id="abertos",label="Sem encerramento",filter="{\"semEncerramento\":true}"),
 @QuickFilter(id="sem-ato",label="Sem fundamento legal",filter="{\"semFundamento\":true}")})
@ApiGroup(value="ergon",title="Administração de pessoal",description="Estrutura organizacional dos órgãos públicos.")
public class HistoricoSetorController extends AbstractResourceController<HistoricoSetorResponseDTO,Long,HistoricoSetorFilterDTO,CreateHistoricoSetorDTO,UpdateHistoricoSetorDTO> {
 private final HistoricoSetorService service;
 public HistoricoSetorController(HistoricoSetorService service) {this.service=service;}
 @Override protected HistoricoSetorService getService() {return service;}
 @Override protected Long getResponseId(HistoricoSetorResponseDTO dto) {return dto.getId();}
  @Override
  @PostMapping("/stats/group-by")
  @Operation(summary = "Agrupar vigências", description = "Apura regi