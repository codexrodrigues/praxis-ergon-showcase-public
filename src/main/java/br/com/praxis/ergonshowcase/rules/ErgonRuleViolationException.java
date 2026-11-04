package br.com.praxis.ergonshowcase.rules;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.praxisplatform.rules.contract.DecisionSource;
import org.praxisplatform.rules.contract.DecisionStage;
import org.praxisplatform.rules.contract.RuleDecision;
import org.praxisplatform.rules.contract.RuleEvaluationResult;
import org.praxisplatform.rules.plan.RuleDecisionPlan;
import org.praxisplatform.rules.violation.PraxisRuleViolationException;
import org.praxisplatform.rules.violation.SlotViolationDetail;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

/** Adapta o catálogo Ergon à rejeição canônica, compartilhada por REST, comandos e lotes. */
public class ErgonRuleViolationException extends PraxisRuleViolationException {
  private static final long serialVersionUID = 1L;
  private final transient RuleDecisionPlan plan;
  private final transient RuleEvaluationResult evaluationResult;

  private final transient Map<String, ErgonRuleErrorCatalog.RuleErrorDescriptor> localErrors;

  public ErgonRuleViolationException(RuleDecisionPlan plan, RuleEvaluationResult result) {
    this(plan, result, Map.of());
  }

  ErgonRuleViolationException(RuleDecisionPlan plan, RuleEvaluationResult result,
      Map<String, ErgonRuleErrorCatalog.RuleErrorDescriptor> localErrors) {
    super(plan, result, ErgonRuleErrorCatalog.canonical(localErrors));
    this.localErrors = localErrors;
    this.plan = plan;
    this.evaluationResult = result;
  }

  public ErgonRuleViolationException(List<SlotViolationDetail> violations) {
    super(violations);
    this.localErrors = Map.of();
    this.plan = null;
    this.evaluationResult = null;
  }

  public ErgonRuleViolationException(String code, String slotKey, String bindingKey,
      DecisionSource source, DecisionStage stage, String targetField, String messagePtBr) {
    this(List.of(detail(code, slotKey, bindingKey, source, stage, targetField, messagePtBr)));
  }

  public static SlotViolationDetail detail(String code, String slotKey, String bindingKey,
      DecisionSource source, DecisionStage stage, String targetField, String messagePtBr) {
    var descriptor = ErgonRuleErrorCatalog.canonical().find(code);
    return new SlotViolationDetail(code, slotKey, bindingKey, source, stage, RuleDecision.DENY,
        targetField, messagePtBr,
        descriptor.map(org.praxisplatform.rules.violation.RuleErrorDescriptor::lineage).orElse(null),
        descriptor.map(org.praxisplatform.rules.violation.RuleErrorDescriptor::category).orElse("BUSINESS_LOGIC"));
  }

  public RuleDecisionPlan getPlan() { return plan; }
  public RuleEvaluationResult getEvaluationResult() { return evaluationResult; }
  public List<ErgonRuleErrorCatalog.RuleErrorDescriptor> getDescriptors() {
    return getReasonCodes().stream().map(code -> java.util.Optional.ofNullable(localErrors.get(code)).or(() -> ErgonRuleErrorCatalog.find(code))).flatMap(java.util.Optional::stream).toList();
  }

  /** Adaptadores para consumidores diretos; a classificação HTTP permanece no engine. */
  public HttpStatusCode getStatusCode() { return HttpStatusCode.valueOf(getHttpStatus()); }
  public String getReason() { return getMessage(); }
  public ProblemDetail getBody() {
    var body = ProblemDetail.forStatusAndDetail(getStatusCode(), getMessage());
    body.setTitle("Violação determinística de regra de negócio");
    toDiagnosticProperties().forEach(body::setProperty);
    if (evaluationResult != null) {
      body.setProperty("ruleSetKey", evaluationResult.ruleSetRef().ruleSetKey());
      body.setProperty("resourceKey", evaluationResult.ruleSetRef().boundedContextKey());
      body.setProperty("planDigest", evaluationResult.planDigest());
      body.setProperty("factsDigest", evaluationResult.factsDigest());
    }
    return body;
  }

  public static List<SlotViolationDetail> buildViolations(RuleDecisionPlan plan, RuleEvaluationResult result) {
    Objects.requireNonNull(result, "result");
    return result.decision() == RuleDecision.ALLOW ? List.of()
        : new PraxisRuleViolationException(plan, result, ErgonRuleErrorCatalog.canonical()).getViolations();
  }
}
