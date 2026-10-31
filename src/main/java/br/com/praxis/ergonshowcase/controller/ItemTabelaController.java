package br.com.praxis.ergonshowcase.controller;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.domain.ItemTabelaId;
import br.com.praxis.ergonshowcase.dto.ItemTabelaFilterDTO;
import br.com.praxis.ergonshowcase.dto.ItemTabelaResponseDTO;
import br.com.praxis.ergonshowcase.service.ItemTabelaService;
import io.swagger.v3.oas.annotations.Operation;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.controller.base.AbstractReadOnlyResourceController;
import org.praxisplatform.uischema.surface.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@ApiResource(
    value = ApiPaths.ITENS_TABELA,
    resourceKey = "administracao-pessoal.itens-tabela",
    title = "Itens de tabelas gerais",
    description =
        "Itens parametrizados das tabelas gerais de domínio do Ergon/Hades (HADES.ITEMTABELA_) utilizados em listas de valores, qualificações e campos customizados.",
    icon = "list_alt",
    identity =
        @ResourceIdentity(
            keyField = "id",
            titleField = "descr",
            displayLabelField = "descr",
            subtitleField = "tabelaDescricao",
            metadataFields = {"tab", "item", "ordem", "ativo"}),
    quickFilters = {
      @QuickFilter(
          id = "ativos",
          label = "Somente ativos",
          filter = "{\"somenteAtivos\":true}"),
      @QuickFilter(
          id = "globais",
          label = "Escopo global",
          filter = "{\"somenteGlobais\":true}"),
      @QuickFilter(
          id = "parentesco",
          label = "Tabela de parentesco",
          filter = "{\"tab\":\"Parentesco\"}")
    })
@ApiGroup(
    value = "ergon",
    title = "Administração de pessoal",
    description = "Cadastros civis e assentamentos funcionais dos órgãos públicos.",
    icon = "groups")
public class ItemTabelaController
    extends AbstractReadOnlyResourceController<
        ItemTabelaResponseDTO, ItemTabelaId, ItemTabelaFilterDTO> {
  @Autowired private ItemTabelaService service;

  @Override
  protected ItemTabelaService getService() {
    return service;
  }

  @Override
  protected ItemTabelaId getResponseId(ItemTabelaResponseDTO dto) {
    return dto.getId();
  }

  @GetMapping("/{id}/ficha-item-tabela")
  @PreAuthorize("hasAuthority('ERGON_READ')")
  @Operation(
      summary = "Consultar ficha do item de tabela geral",
      description =
          "Retorna a projeção de leitura do item de tabela geral com escopo organizacional, ordem e colunas complementares item1..4.")
  @UiSurface(
      id = "ficha-item-tabela",
      kind = SurfaceKind.READ_PROJECTION,
      scope = SurfaceScope.ITEM,
      title = "Ficha do item de tabela",
      description =
          "Detalhes do item de domínio, tabela proprietária, escopo por órgão e atributos complementares.",
      intent = "summary",
      requiredAuthorities = {"ERGON_READ"})
  public ResponseEntity<ItemTabelaResponseDTO> ficha(@PathVariable ItemTabelaId id) {
    return withResourceVersion(ResponseEntity.ok(), id, service.findById(id));
  }
}
