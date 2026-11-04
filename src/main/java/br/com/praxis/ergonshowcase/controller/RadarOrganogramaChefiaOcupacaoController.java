package br.com.praxis.ergonshowcase.controller;
import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.service.RadarOrganogramaChefiaOcupacaoService;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.controller.base.AbstractReadOnlyResourceController;
import org.praxisplatform.uischema.surface.*;
import org.praxisplatform.uischema.stats.dto.GroupByStatsRequest;
import org.praxisplatform.uischema.stats.dto.GroupByStatsResponse;
import org.praxisplatform.uischema.fact.ResourceFact;
import org.praxisplatform.uischema.rest.response.RestApiResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.*;
@ResourceHierarchy(parentField="setorPai",hierarchyPathField="caminhoHierarquico",levelField="nivelHierarquico",childrenCountField="quantidadeSubordinadosDiretos",extinctField="extinto",auxiliaryParentFields={"setorPaiHier1","setorPaiHier2","setorPaiHier3","setorPaiHier4"})
@ResourceFact(key = "situacaoComandoSetor", resourceKey = ApiPaths.KEY_RADAR_ORGANOGRAMA_CHEFIAS_OCUPACAO, label = "Situação do comando",
    description = "Diagnóstico calculado pela vigência da função, designação, exercício, impedimento e substituição. A apuração por cargo permanece indisponível.",
    dataType = "string", sourceKind = "field")
@ResourceFact(key = "vagasAutorizadasSetor", resourceKey = ApiPaths.KEY_RADAR_ORGANOGRAMA_CHEFIAS_OCUPACAO, label = "Vagas diretas autorizadas",
    description = "Total das autorizações vigentes que se restringem ao setor. Reservas para subordinados e ocupação ficam fora dessa soma.",
    dataType = "integer", unit = "vagas", sourceKind = "field")
@ResourceFact(key = "escopoApuracao", resourceKey = ApiPaths.KEY_RADAR_ORGANOGRAMA_CHEFIAS_OCUPACAO, label = "Escopo da apuração",
    description = "Identifica as fontes integradas ao segmento e as lacunas de provimento, exercício, folha e comando por cargo.",
    dataType = "string", sourceKind = "field")
@ApiResource(value=ApiPaths.RADAR_ORGANOGRAMA_CHEFIAS_OCUPACAO,resourceKey=ApiPaths.KEY_RADAR_ORGANOGRAMA_CHEFIAS_OCUPACAO,title="Organograma temporal",description="Estrutura histórica e escopo da apuração de comando e ocupação.",icon="account_tree",
 identity=@ResourceIdentity(keyField="id",titleField="nomeSetor",subtitleField="siglaSetor",metadataFields={"tipoHierarquia"}),
 quickFilters={@QuickFilter(id="extintos",label="Unidades extintas",filter="{\"extinto\":true}"),@QuickFilter(id="sem-apuracao",label="Comando não apurado",filter="{\"semComandoApurado\":true}")})
@ApiGroup(value="ergon",title="Administração de pessoal",description=