package br.com.praxis.ergonshowcase.controller;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.dto.TipoDependenteFilterDTO;
import br.com.praxis.ergonshowcase.dto.TipoDependenteResponseDTO;
import br.com.praxis.ergonshowcase.service.TipoDependenteService;
import io.swagger.v3.oas.annotations.Operation;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.controller.base.AbstractReadOnlyResourceController;
import org.praxisplatform.uischema.surface.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@ApiResource(
    value = ApiPaths.TIPOS_DEPENDENTE,
    resourceKey = "administracao-pessoal.tipos-dependente",
    title = "Tipos de dependente",
    description =
        "Catálogo de classificações legais de dependência funcional e previdenciária (TIPO_DEPEN).",
    icon = "category",
    identity =
        @ResourceIdentity(
            keyField = "id",
            titleField = "descricao",
            displayLabelField = "descricao",
            subtitleField = "id",
            metadataFields = {"geraIr", "idadeLimite"}),
    quickFilters = {
      @QuickFilter(
          id = "deduz-irrf",
          label = "Deduz IRRF",
          filter = "{\"somenteGeraIr\":true}"),
      @QuickFilter(
          id = "salario-familia",
          label = "Salário-família",
          filter = "{\"somenteSalarioFamilia\":true}"),
      @QuickFilter(
          id = "sem-limite-etario",
          label = "Sem limite etário",
          filter = "{\"semIdadeLimite\":true}")
    })
@ApiGroup(
    value = "ergon",
    title = "Administração de pessoal",
    description = "Cadastros civis e assentamentos funcionais dos órgãos públicos.",
    icon = "groups")
public class TipoDependenteController
    extends AbstractReadOnlyResourceController<
        TipoDependenteResponseDTO, String, TipoDependenteFilterDTO> {
  @Autowired private TipoDependenteService service;

  @Override
  protected TipoDependenteService getService() {
    return service;
  }

  @Override
  protected String getResponseId(TipoDependenteResponseDTO dto) {
    return dto.getId();
  }

  @GetMapping("/{id}/ficha-tipo-dependente")
  @PreAuthorize("hasAuthority('ERGON_READ')")
  @Operation(
      summary = "Consultar ficha da modalidade de dependência",
      description =
          "Retorna a projeção de leitura do tipo de dependência com regras de IRRF, salário-família e teto etário.")
  @UiSurface(
      id = "ficha-tipo-dependente",
      kind = SurfaceKind.READ_PROJECTION,
      scope = SurfaceScope.ITEM,
      title = "Ficha da modalidade",
      description = "Regras fiscais, previdenciárias e limite etário da modalidade de dependência.",
      intent = "summary",
      requiredAuthorities = {"ERGON_READ"})
  public ResponseEntity<TipoDependenteResponseDTO> ficha(@PathVariable String id) {
    return withResourceVersion(ResponseEntity.ok(), id, service.findById(id));
  }
}
