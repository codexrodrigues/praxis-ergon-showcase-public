package br.com.praxis.ergonshowcase.controller;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.domain.*;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.service.*;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.controller.base.AbstractResourceController;
import org.praxisplatform.uischema.fact.ResourceFact;

/** Publica o contrato de vínculos através da hierarquia canônica Praxis. */
@ResourceFact(
    key = "diasDesdeExercicio",
    resourceKey = "administracao-pessoal.vinculos",
    label = "Dias desde o exercício",
    description =
        "Dias corridos entre o início de exercício e a vacância ou a data atual; não equivale a tempo previdenciário certificado.",
    dataType = "integer",
    unit = "days",
    sourceKind = "computed")
@ApiResource(
    value = ApiPaths.VINCULOS,
    resourceKey = "administracao-pessoal.vinculos",
    title = "Vínculos",
    description =
        "Assentamentos funcionais por pessoa e número sequencial, com enquadramento e exercício.",
    icon = "badge",
    identity =
        @ResourceIdentity(keyField = "id", titleField = "matric", displayLabelField = "matric"))
@ApiGroup(
    value = "ergon",
    title = "Administração de pessoal",
    description = "Cadastros civis e assentamentos funcionais dos órgãos públicos.",
    icon = "groups")
public class VinculoController
    extends AbstractResourceController<
        VinculoResponseDTO, VinculoId, VinculoFilterDTO, CreateVinculoDTO, UpdateVinculoDTO> {
  private final VinculoService service;
  private final org.praxisplatform.uischema.concurrency.ResourceVersionEtagService etags;
  private final org.praxisplatform.uischema.concurrency.ResourceVersionScopeProvider scopes;

  public VinculoController(
      VinculoService service,
      org.praxisplatform.uischema.concurrency.ResourceVersionEtagService etags,
      org.praxisplatform.uischema.concurrency.ResourceVersionScopeProvider scopes) {
    this.service = service;
    this.etags = etags;
    this.scopes = scopes;
  }

  @org.springframework.web.bind.annotation.GetMapping("/{id}/resumo-funcional")
  @UiSurface(
      id = "resumo-funcional",
      kind = org.praxisplatform.uischema.surface.SurfaceKind.VIEW,
      scope = org.praxisplatform.uischema.surface.SurfaceScope.ITEM,
      title = "Resumo funcional",
      description = "Consulta do enquadramento e ciclo de vida do vínculo selecionado.",
      intent = "consultar-assentamento",
      icon = "badge")
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Consultar resumo funcional",
      description =
          "Apresenta a situação e o enquadramento do vínculo sem certificar tempo previdenciário.")
  public org.springframework.http.ResponseEntity<VinculoResponseDTO> resumo(
      @io.swagger.v3.oas.annotations.Parameter(
              description = "Identidade composta pessoa:número do vínculo",
              example = "1001:1")
          @org.springframework.web.bind.annotation.PathVariable
          VinculoId id) {
    return withResourceVersion(
        org.springframework.http.ResponseEntity.ok(), id, service.findById(id));
  }

  @org.springframework.web.bind.annotation.PostMapping("/{id}/actions/registrar-vacancia")
  @WorkflowAction(
      id = "registrar-vacancia",
      title = "Registrar vacância",
      description =
          "Encerra um vínculo ativo mediante data, forma jurídica e fundamentação administrativa.",
      scope = org.praxisplatform.uischema.action.ActionScope.ITEM,
      requiredAuthorities = {"ERGON_WRITE"},
      allowedStates = {"ATIVO"},
      riskLevel = org.praxisplatform.uischema.action.ActionRiskLevel.HIGH,
      confirmationRequired = true,
      resourceVersion = org.praxisplatform.uischema.action.ActionRequirement.REQUIRED,
      resourceVersionTransport =
          org.praxisplatform.uischema.action.ActionResourceVersionTransport.IF_MATCH)
  @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ERGON_WRITE')")
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Registrar vacância",
      description =
          "Transita ATIVO para VACANTE e registra a fundamentação; exige ETag atual e órgão compatível com X-Tenant.")
  public org.springframework.http.ResponseEntity<VinculoResponseDTO> vacancia(
      @io.swagger.v3.oas.annotations.Parameter(
              description = "Identidade composta pessoa:número do vínculo",
              example = "1001:1")
          @org.springframework.web.bind.annotation.PathVariable
          VinculoId id,
      @io.swagger.v3.oas.annotations.Parameter(
              description = "ETag forte recebido na consulta do vínculo")
          @org.springframework.web.bind.annotation.RequestHeader(
              value = "If-Match",
              required = false)
          String ifMatch,
      @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody
          RegistrarVacanciaDTO dto) {
    var p =
        new org.praxisplatform.uischema.concurrency.ResourceVersionUpdatePrecondition<VinculoId>(
            etags, ifMatch, scopes.currentScope(), "administracao-pessoal.vinculos", id);
    return withResourceVersion(
        org.springframework.http.ResponseEntity.ok(), id, service.registrarVacancia(id, dto, p));
  }

  @Override
  protected VinculoService getService() {
    return service;
  }

  @Override
  protected VinculoId getResponseId(VinculoResponseDTO dto) {
    return dto.getId();
  }
}
