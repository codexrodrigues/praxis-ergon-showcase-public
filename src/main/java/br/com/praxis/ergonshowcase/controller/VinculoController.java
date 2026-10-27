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
@org.praxisplatform.uischema.fact.ResourceFact(
    key = "emEstagioProbatorio",
    resourceKey = "administracao-pessoal.vinculos",
    label =
        "Indica vínculo ativo em exercício cujo estágio ainda não foi homologado; não certifica elegibilidade jurídica",
    description =
        "Indica vínculo ativo em exercício cujo estágio ainda não foi homologado; não certifica elegibilidade jurídica.",
    dataType = "boolean",
    sourceKind = "computed")
@org.praxisplatform.uischema.fact.ResourceFact(
    key = "possuiCargoComissao",
    resourceKey = "administracao-pessoal.vinculos",
    label = "Indica cargo em comissão associado ao vínculo",
    description = "Indica cargo em comissão associado ao vínculo.",
    dataType = "boolean",
    sourceKind = "computed")
@org.praxisplatform.uischema.fact.ResourceFact(
    key = "emCessaoAtiva",
    resourceKey = "administracao-pessoal.vinculos",
    label = "Indica estado funcional CEDIDO persistido",
    description = "Indica estado funcional CEDIDO persistido.",
    dataType = "boolean",
    sourceKind = "computed")
@ApiResource(
    quickFilters = {
      @QuickFilter(id = "ativos", label = "ATIVO", filter = "{\"situacoes\":[\"ATIVO\"]}"),
      @QuickFilter(id = "cedidos", label = "CEDIDO", filter = "{\"situacoes\":[\"CEDIDO\"]}"),
      @QuickFilter(id = "afastados", label = "AFASTADO", filter = "{\"situacoes\":[\"AFASTADO\"]}"),
      @QuickFilter(
          id = "aposentados",
          label = "APOSENTADO",
          filter = "{\"situacoes\":[\"APOSENTADO\"]}"),
      @QuickFilter(id = "vacantes", label = "VACANTE", filter = "{\"situacoes\":[\"VACANTE\"]}"),
      @QuickFilter(id = "cota-pcd", label = "Informação de cota", filter = "{\"infoCota\":true}")
    },
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

  @org.springframework.beans.factory.annotation.Autowired private ErgonCommands commands;

  @org.springframework.beans.factory.annotation.Autowired
  private org.praxisplatform.uischema.concurrency.ResourceVersionEtagService commandEtags;

  @org.springframework.beans.factory.annotation.Autowired
  private org.praxisplatform.uischema.concurrency.ResourceVersionScopeProvider commandScopes;

  @org.springframework.web.bind.annotation.PostMapping("/{id}/actions/registrar-cessao")
  @WorkflowAction(
      id = "registrar-cessao",
      title = "Registrar cessão",
      description =
          "Registra o exercício temporário em outro órgão e a responsabilidade financeira da cessão.",
      scope = org.praxisplatform.uischema.action.ActionScope.ITEM,
      requiredAuthorities = {"ERGON_WRITE"},
      allowedStates = {"ATIVO"},
      resourceVersion = org.praxisplatform.uischema.action.ActionRequirement.REQUIRED,
      resourceVersionTransport =
          org.praxisplatform.uischema.action.ActionResourceVersionTransport.IF_MATCH)
  @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ERGON_WRITE')")
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Registrar cessão",
      description =
          "Registra o exercício temporário em outro órgão e a responsabilidade financeira da cessão. Exige If-Match atual.")
  public org.springframework.http.ResponseEntity<VinculoResponseDTO> registrarCessao(
      @org.springframework.web.bind.annotation.PathVariable VinculoId id,
      @org.springframework.web.bind.annotation.RequestHeader(value = "If-Match", required = false)
          String ifMatch,
      @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody
          RegistrarCessaoDTO dto) {
    var p =
        new org.praxisplatform.uischema.concurrency.ResourceVersionUpdatePrecondition<VinculoId>(
            commandEtags,
            ifMatch,
            commandScopes.currentScope(),
            "administracao-pessoal.vinculos",
            id);
    return withResourceVersion(
        org.springframework.http.ResponseEntity.ok(), id, commands.registrarCessao(id, dto, p));
  }

  @org.springframework.web.bind.annotation.PostMapping("/{id}/actions/encerrar-cessao")
  @WorkflowAction(
      id = "encerrar-cessao",
      title = "Encerrar cessão",
      description = "Registra o término da cessão e o retorno ao estado ativo.",
      scope = org.praxisplatform.uischema.action.ActionScope.ITEM,
      requiredAuthorities = {"ERGON_WRITE"},
      allowedStates = {"CEDIDO"},
      resourceVersion = org.praxisplatform.uischema.action.ActionRequirement.REQUIRED,
      resourceVersionTransport =
          org.praxisplatform.uischema.action.ActionResourceVersionTransport.IF_MATCH)
  @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ERGON_WRITE')")
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Encerrar cessão",
      description =
          "Registra o término da cessão e o retorno ao estado ativo. Exige If-Match atual.")
  public org.springframework.http.ResponseEntity<VinculoResponseDTO> encerrarCessao(
      @org.springframework.web.bind.annotation.PathVariable VinculoId id,
      @org.springframework.web.bind.annotation.RequestHeader(value = "If-Match", required = false)
          String ifMatch,
      @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody
          EncerrarCessaoDTO dto) {
    var p =
        new org.praxisplatform.uischema.concurrency.ResourceVersionUpdatePrecondition<VinculoId>(
            commandEtags,
            ifMatch,
            commandScopes.currentScope(),
            "administracao-pessoal.vinculos",
            id);
    return withResourceVersion(
        org.springframework.http.ResponseEntity.ok(), id, commands.encerrarCessao(id, dto, p));
  }

  @org.springframework.web.bind.annotation.PostMapping("/{id}/actions/conceder-aposentadoria")
  @WorkflowAction(
      id = "conceder-aposentadoria",
      title = "Conceder aposentadoria",
      description =
          "Registra a concessão administrativa fundamentada; não calcula elegibilidade ou benefício.",
      scope = org.praxisplatform.uischema.action.ActionScope.ITEM,
      requiredAuthorities = {"ERGON_WRITE"},
      allowedStates = {"ATIVO"},
      resourceVersion = org.praxisplatform.uischema.action.ActionRequirement.REQUIRED,
      resourceVersionTransport =
          org.praxisplatform.uischema.action.ActionResourceVersionTransport.IF_MATCH)
  @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ERGON_WRITE')")
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Conceder aposentadoria",
      description =
          "Registra a concessão administrativa fundamentada; não calcula elegibilidade ou benefício. Exige If-Match atual.")
  public org.springframework.http.ResponseEntity<VinculoResponseDTO> concederAposentadoria(
      @org.springframework.web.bind.annotation.PathVariable VinculoId id,
      @org.springframework.web.bind.annotation.RequestHeader(value = "If-Match", required = false)
          String ifMatch,
      @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody
          ConcederAposentadoriaDTO dto) {
    var p =
        new org.praxisplatform.uischema.concurrency.ResourceVersionUpdatePrecondition<VinculoId>(
            commandEtags,
            ifMatch,
            commandScopes.currentScope(),
            "administracao-pessoal.vinculos",
            id);
    return withResourceVersion(
        org.springframework.http.ResponseEntity.ok(),
        id,
        commands.concederAposentadoria(id, dto, p));
  }

  @org.springframework.web.bind.annotation.PostMapping("/{id}/actions/homologar-estagio-probatorio")
  @WorkflowAction(
      id = "homologar-estagio-probatorio",
      title = "Homologar estágio probatório",
      description = "Registra homologação fundamentada do estágio probatório.",
      scope = org.praxisplatform.uischema.action.ActionScope.ITEM,
      requiredAuthorities = {"ERGON_WRITE"},
      allowedStates = {"ATIVO"},
      resourceVersion = org.praxisplatform.uischema.action.ActionRequirement.REQUIRED,
      resourceVersionTransport =
          org.praxisplatform.uischema.action.ActionResourceVersionTransport.IF_MATCH)
  @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ERGON_WRITE')")
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Homologar estágio probatório",
      description =
          "Registra homologação fundamentada do estágio probatório. Exige If-Match atual.")
  public org.springframework.http.ResponseEntity<VinculoResponseDTO> homologarEstagio(
      @org.springframework.web.bind.annotation.PathVariable VinculoId id,
      @org.springframework.web.bind.annotation.RequestHeader(value = "If-Match", required = false)
          String ifMatch,
      @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody
          HomologarEstagioDTO dto) {
    var p =
        new org.praxisplatform.uischema.concurrency.ResourceVersionUpdatePrecondition<VinculoId>(
            commandEtags,
            ifMatch,
            commandScopes.currentScope(),
            "administracao-pessoal.vinculos",
            id);
    return withResourceVersion(
        org.springframework.http.ResponseEntity.ok(), id, commands.homologarEstagio(id, dto, p));
  }

  @org.springframework.web.bind.annotation.PatchMapping("/{id}/lotacao-exercicio")
  @ResourceIntent(
      id = "alterar-lotacao-exercicio",
      title = "Alterar lotação e exercício",
      description = "Mantém unidades, cargo em comissão e jornada do vínculo no mesmo órgão.")
  @UiSurface(
      id = "lotacao-exercicio",
      kind = org.praxisplatform.uischema.surface.SurfaceKind.PARTIAL_FORM,
      scope = org.praxisplatform.uischema.surface.SurfaceScope.ITEM,
      title = "Alterar lotação e exercício",
      description = "Mantém unidades, cargo em comissão e jornada do vínculo no mesmo órgão.",
      requiredAuthorities = {"ERGON_WRITE"})
  @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ERGON_WRITE')")
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Alterar lotação e exercício",
      description =
          "Mantém unidades, cargo em comissão e jornada do vínculo no mesmo órgão. Exige If-Match atual.")
  public org.springframework.http.ResponseEntity<VinculoResponseDTO> alterarLotacaoExercicio(
      @org.springframework.web.bind.annotation.PathVariable VinculoId id,
      @org.springframework.web.bind.annotation.RequestHeader(value = "If-Match", required = false)
          String ifMatch,
      @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody
          AlterarLotacaoExercicioDTO dto) {
    var p =
        new org.praxisplatform.uischema.concurrency.ResourceVersionUpdatePrecondition<VinculoId>(
            commandEtags,
            ifMatch,
            commandScopes.currentScope(),
            "administracao-pessoal.vinculos",
            id);
    return withResourceVersion(
        org.springframework.http.ResponseEntity.ok(),
        id,
        commands.alterarLotacaoExercicio(id, dto, p));
  }

  @org.springframework.web.bind.annotation.PatchMapping("/{id}/previdencia-pagamento")
  @ResourceIntent(
      id = "atualizar-previdencia-pagamento",
      title = "Atualizar previdência e pagamento",
      description = "Mantém o enquadramento previdenciário e a conta de pagamento do vínculo.")
  @UiSurface(
      id = "previdencia-pagamento",
      kind = org.praxisplatform.uischema.surface.SurfaceKind.PARTIAL_FORM,
      scope = org.praxisplatform.uischema.surface.SurfaceScope.ITEM,
      title = "Atualizar previdência e pagamento",
      description = "Mantém o enquadramento previdenciário e a conta de pagamento do vínculo.",
      requiredAuthorities = {"ERGON_WRITE"})
  @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ERGON_WRITE')")
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Atualizar previdência e pagamento",
      description =
          "Mantém o enquadramento previdenciário e a conta de pagamento do vínculo. Exige If-Match atual.")
  public org.springframework.http.ResponseEntity<VinculoResponseDTO> atualizarPrevidenciaPagamento(
      @org.springframework.web.bind.annotation.PathVariable VinculoId id,
      @org.springframework.web.bind.annotation.RequestHeader(value = "If-Match", required = false)
          String ifMatch,
      @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody
          AtualizarPrevidenciaPagamentoDTO dto) {
    var p =
        new org.praxisplatform.uischema.concurrency.ResourceVersionUpdatePrecondition<VinculoId>(
            commandEtags,
            ifMatch,
            commandScopes.currentScope(),
            "administracao-pessoal.vinculos",
            id);
    return withResourceVersion(
        org.springframework.http.ResponseEntity.ok(),
        id,
        commands.atualizarPrevidenciaPagamento(id, dto, p));
  }
}
