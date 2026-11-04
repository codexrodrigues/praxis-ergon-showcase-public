package br.com.praxis.ergonshowcase.policystudio;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.praxisplatform.config.dto.DomainRuleCompositionManifestResponse;
import org.praxisplatform.config.service.DomainRuleGovernancePrincipalResolver;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPaths.POLICY_STUDIO)
@org.praxisplatform.uischema.annotation.ApiGroup("ergon")
public class ErgonPolicyCompositionController {
  private final ErgonPolicyComposition composition;
  private final DomainRuleGovernancePrincipalResolver principals;

  public ErgonPolicyCompositionController(ErgonPolicyComposition composition,
      DomainRuleGovernancePrincipalResolver principals) {
    this.composition = composition;
    this.principals = principals;
  }

  @PostMapping(ApiPaths.POLICY_STUDIO_COMPOSITION)
  @Operation(operationId = "prepareErgonGovernedComposition", summary = "Preparar manifesto de regras do órgão",
      description = "Compõe uma versão candidata a partir de fontes aprovadas e devolve o manifesto canônico para revisão.")
  public ResponseEntity<DomainRuleCompositionManifestResponse> prepare(@PathVariable String ruleSetKey,
      @PathVariable int empCodigo, @RequestBody Request candidate, HttpServletRequest request) {
    var principal = principals.resolve(request, request.getHeader("X-Tenant-ID"),
        request.getHeader("X-Env"), "RULE_SNAPSHOT_PUBLISHER");
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(composition.prepare(
        ruleSetKey, empCodigo, candidate.sourceDefinitionIds(), candidate.validFromUtc(),
        candidate.validUntilUtc(), principal));
  }

  @ExceptionHandler({IllegalArgumentException.class, ErgonPolicyStudioService.InvalidPolicy.class,
      org.praxisplatform.rules.plan.RulePlanException.class,
      org.praxisplatform.config.exception.ConfigurationIngestionException.class})
  ProblemDetail invalidCandidate(RuntimeException failure) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, failure.getMessage());
  }

  public record Request(
      @Schema(description = "IDs das definições que autorizam os bindings do órgão.") List<UUID> sourceDefinitionIds,
      @Schema(description = "Início UTC da vigência proposta.") String validFromUtc,
      @Schema(description = "Limite UTC exclusivo da vigência proposta, quando houver.") String validUntilUtc) {}
}
