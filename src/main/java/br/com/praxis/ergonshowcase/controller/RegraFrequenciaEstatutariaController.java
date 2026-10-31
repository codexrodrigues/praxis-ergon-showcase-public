package br.com.praxis.ergonshowcase.controller;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.dto.RegraFrequenciaEstatutariaFilterDTO;
import br.com.praxis.ergonshowcase.dto.RegraFrequenciaEstatutariaResponseDTO;
import br.com.praxis.ergonshowcase.service.RegraFrequenciaEstatutariaService;
import io.swagger.v3.oas.annotations.Operation;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.controller.base.AbstractReadOnlyResourceController;
import org.praxisplatform.uischema.surface.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@ApiResource(
    value = ApiPaths.REGRAS_FREQUENCIA_ESTATUTARIA,
    resourceKey = "administracao-pessoal.regras-frequencia-estatutaria",
    title = "Regras estatutárias de frequência",
    description =
        "Matriz parametrizada de regras, durações, interstícios e limites mensais/anuais de frequência e afastamento por regime, vínculo e categoria (REGRAS_FREQ).",
    icon = "gavel",
    identity =
        @ResourceIdentity(
            keyField = "id",
            titleField = "ruleKey",
            displayLabelField = "ruleKey",
            subtitleField = "codigoFrequenciaNome",
            metadataFields = {"regimeJuridico", "tipoVinculo", "categoria", "ativa"}),
    quickFilters = {
      @QuickFilter(
          id = "ativas",
          label = "Regras ativas",
          filter = "{\"ativa\":true}"),
      @QuickFilter(
          id = "exigem-homologacao",
          label = "Exigem homologação",
          filter = "{\"exigeHomologacao\":true}"),
      @QuickFilter(
          id = "vedadas",
          label = "Vedadas para o perfil",
          filter = "{\"naoSeAplica\":true}"),
      @QuickFilter(
          id = "estatutarias",
          label = "Regime estatutário",
          filter = "{\"regimeJuridico\":\"ESTATUTARIO\"}")
    })
@ApiGroup(
    value = "ergon",
    title = "Administração de pessoal",
    description = "Cadastros civis e assentamentos funcionais dos órgãos públicos.",
    icon = "groups")
public class RegraFrequenciaEstatutariaController
    extends AbstractReadOnlyResourceController<
        RegraFrequenciaEstatutariaResponseDTO, Long, RegraFrequenciaEstatutariaFilterDTO> {
  @Autowired private RegraFrequenciaEstatutariaService service;

  @Override
  protected RegraFrequenciaEstatutariaService getService() {
    return service;
  }

  @Override
  protected Long getResponseId(RegraFrequenciaEstatutariaResponseDTO dto) {
    return dto.getId();
  }

  @GetMapping("/{id}/ficha-regra-frequencia-estatutaria")
  @PreAuthorize("hasAuthority('ERGON_READ')")
  @Operation(
      summary = "Consultar ficha da regra estatutária de frequência",
      description =
          "Retorna a projeção de leitura da regra estatutária com durações, interstícios, limites mensais e anuais e parâmetros do motor de regras.")
  @UiSurface(
      id = "ficha-regra-frequencia-estatutaria",
      kind = SurfaceKind.READ_PROJECTION,
      scope = SurfaceScope.ITEM,
      title = "Ficha da regra estatutária",
      description =
          "Matriz de limites quantitativos, interstícios, vedação e chave do RuleSet da regra estatutária.",
      intent = "summary",
      requiredAuthorities = {"ERGON_READ"})
  public ResponseEntity<RegraFrequenciaEstatutariaResponseDTO> ficha(@PathVariable Long id) {
    return withResourceVersion(ResponseEntity.ok(), id, service.findById(id));
  }

  @GetMapping("/rn013/plan")
  @PreAuthorize("hasAuthority('ERGON_READ')")
  @Operation(
      operationId = "getRn013RuleDecisionPlan",
      summary = "Consultar plano compilado e catálogo de erros da RN-013 (PCK_REGRAS_FREQ)",
      description =
          "Retorna o RuleDecisionPlan compilado para o órgão (slots, bindings ativos PRODUCT vs CUSTOMER e catálogo ERG-08380..ERG-08393 / ERG-00019).")
  public ResponseEntity<java.util.Map<String, Object>> rn013Plan(
      @RequestParam(name = "empCodigo", required = false) Integer empCodigo) {
    return ResponseEntity.ok(service.getRn013PlanSummary(empCodigo));
  }

  @PostMapping("/rn013/evaluate")
  @PreAuthorize("hasAuthority('ERGON_READ')")
  @Operation(
      operationId = "evaluateRn013RegraFrequencia",
      summary = "Simular avaliação determinística da RN-013 sobre rascunho de regra estatutária",
      description =
          "Avalia todas as 15 decisões da RN-013 para o órgão sem lançar exceção, retornando o diagnóstico por slot, códigos ERG-*/CERG-* e rastreabilidade PRODUCT vs CUSTOMER.")
  public ResponseEntity<java.util.Map<String, Object>> evaluateRn013(
      @RequestBody br.com.praxis.ergonshowcase.domain.RegraFrequenciaEstatutaria draft) {
    return ResponseEntity.ok(service.evaluateRn013Diagnostic(draft));
  }

  @PostMapping("/rn013/validate")
  @PreAuthorize("hasAuthority('ERGON_READ')")
  @Operation(
      operationId = "validateRn013RegraFrequencia",
      summary = "Validar rascunho de regra estatutária na RN-013 em modo fail-closed",
      description =
          "Executa validação determinística fail-closed da RN-013, retornando HTTP 200 em caso de ALLOW ou HTTP 422 (ProblemDetail com violações estruturadas) quando DENY/INCONCLUSIVE.")
  public ResponseEntity<java.util.Map<String, Object>> validateRn013(
      @RequestBody br.com.praxis.ergonshowcase.domain.RegraFrequenciaEstatutaria draft) {
    return ResponseEntity.ok(service.validateRn013Diagnostic(draft));
  }
}
