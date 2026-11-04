package br.com.praxis.ergonshowcase.policystudio;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.policystudio.PolicyStudioDtos.*;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import org.praxisplatform.uischema.annotation.ApiGroup;
import org.praxisplatform.uischema.rest.response.RestApiResponse;
import org.springframework.http.ProblemDetail;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPaths.POLICY_STUDIO)
@ApiGroup("ergon")
public class ErgonPolicyStudioController {
  private final ErgonPolicyStudioService studio;

  public ErgonPolicyStudioController(ErgonPolicyStudioService studio) {
    this.studio = studio;
  }

  @GetMapping("/organizations")
  @Operation(operationId = "listErgonPolicyStudioOrganizations", summary = "Listar órgãos autorizados",
      description = "Projeta os órgãos acessíveis à identidade atual e a capacidade de publicar em cada um.")
  public RestApiResponse<List<Organization>> organizations() {
    return RestApiResponse.success(PolicyStudioAccess.organizations(), null);
  }

  @GetMapping("/rule-sets")
  @Operation(operationId = "listErgonPolicyStudioRuleSets", summary = "Listar conjuntos de regras",
      description = "Consulta o catálogo e as capacidades de composição dos planos ativos do órgão.")
  public RestApiResponse<List<RuleSetSummary>> list(@RequestParam(defaultValue = "1") int empCodigo) {
    PolicyStudioAccess.requireRead(empCodigo);
    return RestApiResponse.success(studio.list(empCodigo), null);
  }

  @GetMapping("/rule-sets/{ruleSetKey}")
  @Operation(operationId = "getErgonPolicyStudioRuleSetDetail", summary = "Inspecionar plano de regras",
      description = "Apresenta a ordem de execução, as dependências, a linhagem e os parâmetros do plano.")
  public ResponseEntity<RestApiResponse<RuleSetDetail>> detail(@PathVariable String ruleSetKey,
      @RequestParam(defaultValue = "1") int empCodigo) {
    PolicyStudioAccess.requireRead(empCodigo);
    return plan(studio.detail(ruleSetKey, empCodigo));
  }

  @PostMapping("/rule-sets/{ruleSetKey}/orgaos/{empCodigo}/customize")
  @Operation(operationId = "customizeErgonPolicyStudioRuleSet", summary = "Customizar política do órgão",
      description = "Compila e ativa a alteração admitida pelo slot para as próximas operações do órgão.")
  public ResponseEntity<RestApiResponse<RuleSetDetail>> customize(@PathVariable String ruleSetKey,
      @PathVariable int empCodigo, @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
      @RequestBody CustomizationRequest request) {
    PolicyStudioAccess.requirePublish(empCodigo);
    return plan(studio.customize(ruleSetKey, empCodigo, ifMatch, request));
  }

  @PostMapping("/rule-sets/{ruleSetKey}/orgaos/{empCodigo}/preview")
  @Operation(operationId = "previewErgonPolicyStudioCandidate", summary = "Simular proposta do órgão",
      description = "Compara o plano ativo e a proposta para os mesmos fatos, preservando a ativação atual.")
  public RestApiResponse<SimulationResponse> preview(@PathVariable String ruleSetKey, @PathVariable int empCodigo,
      @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
      @RequestBody CandidateRequest request) {
    PolicyStudioAccess.requirePublish(empCodigo);
    return RestApiResponse.success(studio.preview(ruleSetKey, empCodigo, ifMatch, request), null);
  }

  @PostMapping("/rule-sets/{ruleSetKey}/orgaos/{empCodigo}/reset")
  @Operation(operationId = "resetErgonPolicyStudioRuleSet", summary = "Restaurar política do órgão",
      description = "Descarta as alterações publicadas e resolve novamente o plano inicial do órgão.")
  public ResponseEntity<RestApiResponse<RuleSetDetail>> reset(@PathVariable String ruleSetKey,
      @PathVariable int empCodigo, @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch) {
    PolicyStudioAccess.requirePublish(empCodigo);
    return plan(studio.reset(ruleSetKey, empCodigo, ifMatch));
  }

  private static ResponseEntity<RestApiResponse<RuleSetDetail>> plan(RuleSetDetail detail) {
    return ResponseEntity.ok().eTag(detail.activationTag()).body(RestApiResponse.success(detail, null));
  }

  @PostMapping(ApiPaths.POLICY_STUDIO_SIMULATE_SEGMENT)
  @Operation(operationId = "simulateErgonPolicyStudioRuleSet", summary = "Simular políticas lado a lado",
      description = "Avalia os mesmos fatos nos planos ativos do órgão de referência e do órgão alvo, sem aplicar efeitos.")
  public RestApiResponse<SimulationResponse> simulate(@RequestBody SimulationRequest request) {
    PolicyStudioAccess.requireRead(request.baselineOrgao());
    PolicyStudioAccess.requireRead(request.targetOrgao());
    return RestApiResponse.success(studio.simulate(request), null);
  }

  @ExceptionHandler(ErgonPolicyStudioService.InvalidPolicy.class)
  ProblemDetail invalidPolicy(ErgonPolicyStudioService.InvalidPolicy error) {
    var problem = ProblemDetail.forStatusAndDetail(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY, error.getMessage());
    problem.setTitle("Customização de política inválida");
    problem.setProperty("code", error.code());
    return problem;
  }
}
