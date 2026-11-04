package br.com.praxis.ergonshowcase.controller;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.dto.DefinicaoCampoCustomizadoFilterDTO;
import br.com.praxis.ergonshowcase.dto.DefinicaoCampoCustomizadoResponseDTO;
import br.com.praxis.ergonshowcase.dto.ItemTabelaFilterDTO;
import br.com.praxis.ergonshowcase.dto.ItemTabelaResponseDTO;
import br.com.praxis.ergonshowcase.dto.TabelaFilterDTO;
import br.com.praxis.ergonshowcase.dto.TabelaResponseDTO;
import br.com.praxis.ergonshowcase.service.DefinicaoCampoCustomizadoService;
import br.com.praxis.ergonshowcase.service.ItemTabelaService;
import br.com.praxis.ergonshowcase.service.TabelaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.controller.base.AbstractReadOnlyResourceController;
import org.praxisplatform.uischema.fact.ResourceFact;
import org.praxisplatform.uischema.rest.response.RestApiResponse;
import org.praxisplatform.uischema.surface.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Links;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@ResourceFact(
    key = "multiEmp",
    resourceKey = "administracao-pessoal.tabelas",
    label = "Particionada por órgão",
    description =
        "Indica se os itens da tabela geral admitem escopo segregado por empresa/órgão (MULTI_EMP).",
    dataType = "boolean",
    icon = "domain",
    tone = "info",
    valuePresentation = "badge",
    sourceKind = "field")
@ResourceFact(
    key = "taminfo",
    resourceKey = "administracao-pessoal.tabelas",
    label = "Tamanho máximo do código",
    description =
        "Quantidade máxima de caracteres aceita para o código de item nesta tabela geral.",
    dataType = "integer",
    unit = "caracteres",
    icon = "straighten",
    tone = "neutral",
    valuePresentation = "integer",
    sourceKind = "field",
    thresholds = {
      @Threshold(min = 0, max = 5, tone = "success", label = "Código curto"),
      @Threshold(min = 5, max = 15, tone = "info", label = "Código médio"),
      @Threshold(min = 15, tone = "warning", label = "Código extenso")
    })
@ApiResource(
    value = ApiPaths.TABELAS,
    resourceKey = "administracao-pessoal.tabelas",
    title = "Tabelas gerais de domínio",
    description =
        "Catálogo mestre de tabelas gerais de domínio do Ergon/Hades (HADES.TABELA) que estrutura itens parametrizáveis e listas de opções de campos customizados.",
    icon = "table_chart",
    identity =
        @ResourceIdentity(
            keyField = "id",
            titleField = "descr",
            displayLabelField = "descr",
            subtitleField = "tab",
            metadataFields = {"updatedAt"}),
    quickFilters = {
      @QuickFilter(
          id = "multi-orgao",
          label = "Multi-órgão",
          filter = "{\"somenteMultiEmp\":true}"),
      @QuickFilter(
          id = "sistema-ergon",
          label = "Sistema Ergon",
          filter = "{\"sis\":\"Ergon\"}"),
      @QuickFilter(
          id = "com-colunas-auxiliares",
          label = "Com colunas auxiliares",
          filter = "{\"comColunaComplementar\":true}")
    })
@ApiGroup(
    value = "ergon",
    title = "Administração de pessoal",
    description = "Cadastros civis e assentamentos funcionais dos órgãos públicos.",
    icon = "groups")
public class TabelaController
    extends AbstractReadOnlyResourceController<TabelaResponseDTO, String, TabelaFilterDTO> {
  @Autowired private TabelaService service;
  @Autowired private ItemTabelaService itensService;
  @Autowired private DefinicaoCampoCustomizadoService camposCustomizadosService;

  @Override
  protected TabelaService getService() {
    return service;
  }

  @Override
  protected String getResponseId(TabelaResponseDTO dto) {
    return dto.getId();
  }

  @GetMapping("/{id}/ficha-tabela")
  @PreAuthorize("hasAuthority('ERGON_READ')")
  @Operation(
      summary = "Consultar ficha da tabela geral de domínio",
      description =
          "Retorna a projeção de leitura da tabela geral com sistema proprietário, natureza do código e rótulos de colunas complementares.")
  @UiSurface(
      id = "ficha-tabela",
      kind = SurfaceKind.READ_PROJECTION,
      scope = SurfaceScope.ITEM,
      title = "Ficha da tabela geral",
      description =
          "Estrutura de código, escopo multi-órgão e rótulos semânticos das colunas auxiliares da tabela geral.",
      intent = "summary",
      requiredAuthorities = {"ERGON_READ"})
  public ResponseEntity<TabelaResponseDTO> ficha(@PathVariable String id) {
    return withResourceVersion(ResponseEntity.ok(), id, service.findById(id));
  }

  @GetMapping("/{id}/itens")
  @PreAuthorize("hasAuthority('ERGON_READ')")
  @UiSurface(
      id = "itens-tabela",
      kind = SurfaceKind.READ_PROJECTION,
      scope = SurfaceScope.ITEM,
      title = "Itens da tabela geral",
      description = "Itens de domínio vinculados à tabela geral selecionada.",
      relatedChildResourceKey = "administracao-pessoal.itens-tabela",
      relatedChildResourcePath = ApiPaths.ITENS_TABELA,
      relatedChildParentField = "tab",
      relatedChildOperations = {
        RelatedResourceChildOperation.FILTER,
        RelatedResourceChildOperation.LIST
      },
      requiredAuthorities = {"ERGON_READ"})
  @Operation(
      summary = "Consultar itens da tabela geral",
      description =
          "Lista os itens parametrizados pertencentes à tabela geral de domínio. Paginação limitada a 200 registros.")
  public ResponseEntity<RestApiResponse<Page<ItemTabelaResponseDTO>>> itens(
      @Parameter(description = "Código identificador da tabela geral") @PathVariable String id,
      @Parameter(description = "Página a consultar, iniciando em zero")
          @RequestParam(defaultValue = "0")
          int page,
      @Parameter(description = "Quantidade de registros da página, de 1 a 200")
          @RequestParam(defaultValue = "20")
          int size) {
    ErgonHttpContract.childPage(page, size);
    return filterRelatedChildResource(id, itensService, ApiPaths.ITENS_TABELA, ItemTabelaFilterDTO::new,
        (child, parent) -> { child.setTab(parent); }, page, size);
  }

  @GetMapping("/{id}/campos-customizados")
  @PreAuthorize("hasAuthority('ERGON_READ')")
  @UiSurface(
      id = "campos-customizados-tabela",
      kind = SurfaceKind.READ_PROJECTION,
      scope = SurfaceScope.ITEM,
      title = "Campos customizados vinculados",
      description =
          "Definições de campos customizados que utilizam esta tabela geral como fonte de opções.",
      relatedChildResourceKey = "administracao-pessoal.campos-customizados",
      relatedChildResourcePath = ApiPaths.CAMPOS_CUSTOMIZADOS,
      relatedChildParentField = "optionTabela",
      relatedChildOperations = {
        RelatedResourceChildOperation.FILTER,
        RelatedResourceChildOperation.LIST
      },
      requiredAuthorities = {"ERGON_READ"})
  @Operation(
      summary = "Consultar campos customizados vinculados à tabela geral",
      description =
          "Lista as definições de campos customizados que consomem esta tabela geral como lista de opções.")
  public ResponseEntity<RestApiResponse<Page<DefinicaoCampoCustomizadoResponseDTO>>>
      camposCustomizados(
          @Parameter(description = "Código identificador da tabela geral") @PathVariable String id,
          @Parameter(description = "Página a consultar, iniciando em zero")
              @RequestParam(defaultValue = "0")
              int page,
          @Parameter(description = "Quantidade de registros da página, de 1 a 200")
              @RequestParam(defaultValue = "20")
              int size) {
    ErgonHttpContract.childPage(page, size);
    return filterRelatedChildResource(id, camposCustomizadosService, ApiPaths.CAMPOS_CUSTOMIZADOS, DefinicaoCampoCustomizadoFilterDTO::new,
        (child, parent) -> { child.setOptionTabela(parent); }, page, size);
  }
}
