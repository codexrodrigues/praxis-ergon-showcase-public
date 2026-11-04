package br.com.praxis.ergonshowcase.policystudio;

import static br.com.praxis.ergonshowcase.policystudio.PolicyStudioDtos.*;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.rules.AfastamentoConcessaoRuleSet;
import br.com.praxis.ergonshowcase.rules.ErgonOntologyRuleSetSupport;
import br.com.praxis.ergonshowcase.rules.ErgonRuleEngineFacade;
import br.com.praxis.ergonshowcase.rules.ErgonRuleErrorCatalog;
import br.com.praxis.ergonshowcase.rules.ErgonRuleErrorCatalog.RuleErrorDescriptor;
import br.com.praxis.ergonshowcase.rules.FrequenciaLancamentoRuleSet;
import br.com.praxis.ergonshowcase.rules.ParametroContagemFeriasRuleSet;
import br.com.praxis.ergonshowcase.rules.Rn013RegrasFrequenciaRuleSet;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.praxisplatform.rules.contract.*;
import org.praxisplatform.rules.parameters.RuleParameterResolver;
import org.praxisplatform.rules.plan.RuleDecisionPlan;
import org.praxisplatform.rules.plan.RulePlanException;
import org.praxisplatform.rules.violation.PraxisRuleViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ErgonPolicyStudioService {
  private record ParameterRange(String minimum, String maximum) {}
  private static final Map<String, ParameterRange> PARAMETER_RANGES = Map.of(
      Rn013RegrasFrequenciaRuleSet.TEMPLATE_QTD_MAX_BOUNDS_KEY, new ParameterRange("minAllowedQty", "maxAllowedQty"),
      AfastamentoConcessaoRuleSet.TEMPLATE_PERICIA_WAIVER_BOUNDS_KEY, new ParameterRange("minDaysWithoutPericia", "maxDaysWithoutPericia"),
      FrequenciaLancamentoRuleSet.TEMPLATE_RETROACTIVE_BOUNDS_KEY, new ParameterRange("minRetroactiveDays", "maxRetroactiveDays"),
      ParametroContagemFeriasRuleSet.TEMPLATE_VACATION_DAYS_BOUNDS_KEY, new ParameterRange("minDiasDireitoPadrao", "maxDiasDireitoPadrao"));

  private final ErgonRuleEngineFacade rules;
  private final ObjectMapper mapper;
  private final PolicyStudioCatalog catalog;
  private final JsonNode bypassExpression;

  public ErgonPolicyStudioService(ErgonRuleEngineFacade rules, ObjectMapper mapper) {
    this.rules = rules;
    this.mapper = mapper;
    this.catalog = new PolicyStudioCatalog(mapper);
    this.bypassExpression = mapper.createObjectNode().set("==", mapper.createArrayNode().add(1).add(1));
  }

  private static String canonicalResourceKey(RuleSetRef ref) {
    return Rn013RegrasFrequenciaRuleSet.RESOURCE_KEY.equals(ref.boundedContextKey())
        ? ApiPaths.KEY_REGRAS_FREQUENCIA_ESTATUTARIA : ref.boundedContextKey();
  }

  List<RuleSetSummary> list(int orgao) {
    requireOrgao(orgao);
    return rules.ruleSetKeys().stream().map(key -> {
      var product = rules.productRuleSet(key);
      var active = rules.resolvePlan(key, orgao);
      var entry = catalog.entry(product.ref().boundedContextKey());
      var ref = product.ref();
      var slots = product.slots();
      long customizations = active.orderedBindings().stream()
          .filter(binding -> binding.source() == DecisionSource.CUSTOMER)
          .map(DecisionBinding::slotKey).distinct().count();
      String resourceKey = canonicalResourceKey(ref);
      var activeDef = active.definition();
      return new RuleSetSummary(key, resourceKey, ref.boundedContextKey(),
          ref.operationKey(), ref.version(), entry.title(), entry.group(), entry.description(),
          activeDef.availableRoots(), activeDef.conceptRoots(),
          activeDef.referencedFactPaths(), activeDef.factPathsByRoot(),
          slots.size(), count(slots, OverridePolicy.FORBIDDEN), count(slots, OverridePolicy.PARAMETERIZABLE),
          count(slots, OverridePolicy.REPLACEABLE), count(slots, OverridePolicy.RESTRICT_ONLY),
          customizations, active.planDigest(), rules.resolvePlan(key, null).planDigest());
    }).toList();
  }

  RuleSetDetail detail(String key, int orgao) {
    requireRuleSet(key);
    requireOrgao(orgao);
    synchronized (rules) {
      return detail(key, orgao, rules.resolvePlan(key, orgao));
    }
  }

  private RuleSetDetail detail(String key, int orgao, RuleDecisionPlan plan) {
    return PolicyStudioProjection.detail(plan, rules.resolvePlan(key, null), orgao, rules.activationTag(key, orgao),
        catalog.entry(plan.definition().ref().boundedContextKey()), catalog.scenarios(key),
        rules.localErrors(plan), bypassExpression, mapper);
  }

  record Candidate(RuleSetDefinition definition, Map<String, RuleErrorDescriptor> errors) {}

  RuleSetDetail customize(String key, int orgao, String ifMatch, CustomizationRequest request) {
    requireRuleSet(key);
    requireOrgao(orgao);
    synchronized (rules) {
      requireRevision(key, orgao, ifMatch);
      var plan = rules.resolvePlan(key, orgao);
      var candidate = prepareCandidate(plan, orgao, request);
      if (plan.definition().equals(candidate.definition()) && rules.localErrors(plan).equals(candidate.errors())) {
        return detail(key, orgao, plan);
      }
      try {
        return detail(key, orgao, rules.registerPlan(key, orgao, candidate.definition(), candidate.errors()));
      } catch (RulePlanException error) {
        throw new InvalidPolicy(error.getCode().name(), error.getMessage());
      } catch (IllegalArgumentException error) {
        throw invalid(error.getMessage());
      }
    }
  }

  private Candidate prepareCandidate(RuleDecisionPlan plan, int orgao, CustomizationRequest request) {
    return prepareCandidate(plan, orgao, request, rules.localErrors(plan));
  }

  Candidate prepareCandidate(RuleDecisionPlan plan, int orgao, CustomizationRequest request,
      Map<String, RuleErrorDescriptor> previousErrors) {
    if (request == null || request.mode() == null || request.slotKey() == null || request.slotKey().isBlank()) {
      throw invalid("Informe a modalidade e o slot da alteração.");
    }
    var slot = plan.slotsByKey().get(request.slotKey());
    if (slot == null) throw invalid("Slot desconhecido: " + request.slotKey());
    var errors = new HashMap<>(previousErrors);
    var definition = plan.definition();
    try {
      var bindings = switch (request.mode()) {
        case BYPASS_SLOT, REPLACE_SLOT_EXPRESSION, UPDATE_PARAMETERS -> replace(definition, slot, orgao, request, errors);
        case AUGMENT_SLOT -> augment(definition, slot, orgao, request, errors);
      };
      errors.keySet().removeIf(code -> bindings.stream().noneMatch(binding ->
          code.equals(binding.falseReasonCode()) || code.equals(binding.bindingKey())));
      return new Candidate(withBindings(definition, bindings), Map.copyOf(errors));
    } catch (RulePlanException error) {
      throw new InvalidPolicy(error.getCode().name(), error.getMessage());
    } catch (IllegalArgumentException error) {
      throw invalid(error.getMessage());
    }
  }

  SimulationResponse preview(String key, int orgao, String ifMatch, CandidateRequest request) {
    requireRuleSet(key);
    requireOrgao(orgao);
    JsonNode facts = resolveFacts(key, request.facts(), request.presetScenarioKey());
    RuleDecisionPlan active;
    RuleDecisionPlan proposed;
    Map<String, RuleErrorDescriptor> proposedErrors;
    synchronized (rules) {
      requireRevision(key, orgao, ifMatch);
      active = rules.resolvePlan(key, orgao);
      var candidate = prepareCandidate(active, orgao, request.customization());
      try {
        proposed = rules.compilePlan(candidate.definition());
      } catch (RulePlanException error) {
        throw new InvalidPolicy(error.getCode().name(), error.getMessage());
      }
      proposedErrors = candidate.errors();
    }
    return compare(active, orgao, proposed, orgao, facts, proposedErrors);
  }

  RuleSetDetail reset(String key, int orgao, String ifMatch) {
    requireRuleSet(key);
    requireOrgao(orgao);
    synchronized (rules) {
      requireRevision(key, orgao, ifMatch);
      return detail(key, orgao, rules.resetOrgaoPlan(key, orgao));
    }
  }

  private void requireRevision(String key, int orgao, String ifMatch) {
    if (ifMatch == null) {
      throw new ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED, "Informe o If-Match do plano inspecionado.");
    }
    if (!rules.activationTag(key, orgao).equals(ifMatch)) {
      throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED, "O plano foi alterado. Recarregue antes de publicar.");
    }
  }

  private List<DecisionBinding> replace(RuleSetDefinition definition, DecisionSlot slot, int orgao,
      CustomizationRequest request, Map<String, RuleErrorDescriptor> errors) {
    boolean parameterize = request.mode() == CustomizationMode.UPDATE_PARAMETERS;
    boolean bypass = request.mode() == CustomizationMode.BYPASS_SLOT;
    requirePolicy(slot, parameterize ? OverridePolicy.PARAMETERIZABLE : OverridePolicy.REPLACEABLE);
    if (bypass && request.bypass() == null) throw invalid("Informe se o bypass deve ser ativado.");

    var slotBindings = definition.bindings().stream()
        .filter(binding -> binding.slotKey().equals(slot.slotKey())).toList();
    var product = slotBindings.stream().filter(binding -> binding.source() == DecisionSource.PRODUCT)
        .findFirst().orElseThrow(() -> invalid("O slot não possui uma decisão de produto."));
    var active = slotBindings.stream().filter(DecisionBinding::enabled).findFirst()
        .orElseThrow(() -> invalid("O slot não possui uma decisão ativa."));
    if (active.source() != DecisionSource.PRODUCT && active.source() != DecisionSource.CUSTOMER) {
      throw invalid("O slot é controlado por uma fonte protegida.");
    }
    boolean restore = bypass && !request.bypass();
    String bypassKey = customerKey(orgao, "bypass", slot.slotKey());
    boolean hasStudioBypass = active.bindingKey().equals(bypassKey);
    if (bypass && (restore != hasStudioBypass)) return definition.bindings();

    DecisionBinding replacement;
    if (restore) {
      var displaced = slotBindings.stream().filter(binding -> binding.source() == DecisionSource.CUSTOMER
          && !binding.enabled()).findFirst().orElse(product);
      replacement = copy(displaced, active.dependsOn(), true);
    } else if (parameterize) {
      var values = request.parameterValues();
      if (values == null || values.isEmpty()) throw invalid("Informe os valores do template.");
      var template = slot.parameterTemplate();
      var parameters = active.parameterization() == null
          ? mapper.createObjectNode() : active.parameterization().values();
      values.forEach(parameters::set);
      var effective = RuleParameterResolver.resolve(template, parameters).effectiveValues();
      var range = PARAMETER_RANGES.get(template.templateKey());
      if (range != null && effective.get(range.minimum()).decimalValue()
          .compareTo(effective.get(range.maximum()).decimalValue()) > 0) {
        throw invalid("O valor de " + range.minimum() + " deve ser menor ou igual a " + range.maximum() + ".");
      }
      replacement = new DecisionBinding(customerKey(orgao, "parameters", slot.slotKey()), slot.slotKey(),
          DecisionSource.CUSTOMER, CompositionPolicy.PARAMETERIZE, null, active.dependsOn(),
          active.order(), true, null, null, List.of(),
          new RuleParameterization(template.templateKey(), template.templateVersion(), parameters));
    } else {
      String code = bypass || request.falseReasonCode() == null
          ? product.falseReasonCode() : request.falseReasonCode();
      if (code == null) throw invalid("Informe o código de razão da regra substituta.");
      String message = bypass ? request.customReasonMessagePtBr() : request.messagePtBr();
      var error = ErgonRuleErrorCatalog.find(code).orElse(null);
      if (error != null && !code.equals(product.falseReasonCode()) && !slot.slotKey().equals(error.slotKey())) {
        throw invalid("O código pertence a uma decisão de outro slot ou recurso.");
      }
      if (definition.bindings().stream().anyMatch(binding -> !binding.slotKey().equals(slot.slotKey())
          && code.equals(binding.falseReasonCode()))) {
        throw invalid("O código já identifica uma decisão de outro slot.");
      }
      if (message != null || error == null) {
        var original = ErgonRuleErrorCatalog.find(product.falseReasonCode()).orElse(null);
        if (error == null && !isCustomerCode(code)) throw invalid("Código de razão local inválido.");
        errors.put(bypass ? bypassKey : code, new RuleErrorDescriptor(code, slot.slotKey(),
            original == null ? null : original.targetField(), requiredText(message, "Mensagem", 1000),
            "POLITICA_LOCAL_ORGAO_" + orgao, null));
      }
      if (!bypass && error != null && message == null) errors.remove(code);
      var expression = bypass ? bypassExpression : requireExpression(request.expressionJson());
      replacement = new DecisionBinding(customerKey(orgao, bypass ? "bypass" : "replace", slot.slotKey()),
          slot.slotKey(), DecisionSource.CUSTOMER, CompositionPolicy.REPLACE_EXACT,
          RuleExecutorRef.jsonLogic(expression),
          active.dependsOn(), active.order(), true, RuleDecision.DENY, code,
          bypass ? product.requiredFactPaths() : extractValidatedRoots(definition, expression), null);
    }

    var bindings = new ArrayList<DecisionBinding>();
    for (var binding : definition.bindings()) {
      if (binding.slotKey().equals(slot.slotKey())) {
        if (binding.source() == DecisionSource.PRODUCT) {
          if (!binding.bindingKey().equals(replacement.bindingKey())) {
            bindings.add(copy(binding, binding.dependsOn(), false));
          }
        } else if (bypass && !restore && binding.bindingKey().equals(active.bindingKey())) {
          bindings.add(copy(binding, binding.dependsOn(), false));
        }
        continue;
      }
      var dependencies = binding.dependsOn().stream()
          .map(dep -> dep.equals(active.bindingKey()) || dep.equals(product.bindingKey())
              ? replacement.bindingKey() : dep)
          .distinct().toList();
      bindings.add(copy(binding, dependencies, binding.enabled()));
    }
    bindings.add(replacement);
    return bindings;
  }

  private List<DecisionBinding> augment(RuleSetDefinition definition, DecisionSlot slot, int orgao,
      CustomizationRequest request, Map<String, RuleErrorDescriptor> errors) {
    requirePolicy(slot, OverridePolicy.RESTRICT_ONLY);
    if (slot.cardinality() != SlotCardinality.MULTIPLE) throw invalid("O slot admite somente um binding.");
    String code = request.ruleCode();
    if (!isCustomerCode(code) || ErgonRuleErrorCatalog.find(code).isPresent()) {
      throw invalid("Informe um código CERG local, distinto dos códigos do produto.");
    }
    String bindingKey = customerKey(orgao, "augment", slot.slotKey() + "." + code);
    if (definition.bindings().stream().anyMatch(binding -> code.equals(binding.falseReasonCode())
        && !bindingKey.equals(binding.bindingKey()))) {
      throw invalid("O código já identifica outro binding neste plano.");
    }
    var descriptor = new RuleErrorDescriptor(code, slot.slotKey(),
        requiredText(request.targetField(), "Campo alvo", 160), requiredText(request.messagePtBr(), "Mensagem", 1000),
        requiredText(request.origin(), "Origem", 300), null);
    var bindings = new ArrayList<>(definition.bindings());
    var previous = bindings.stream().filter(binding -> bindingKey.equals(binding.bindingKey())).findFirst();
    int order = previous.map(DecisionBinding::order)
        .orElseGet(() -> bindings.stream().mapToInt(DecisionBinding::order).max().orElse(0) + 1);
    var expression = requireExpression(request.expressionJson());
    var binding = new DecisionBinding(bindingKey, slot.slotKey(), DecisionSource.CUSTOMER,
        CompositionPolicy.AUGMENT, RuleExecutorRef.jsonLogic(expression),
        List.of(), order, true, RuleDecision.DENY, code, extractValidatedRoots(definition, expression), null);
    previous.ifPresent(bindings::remove);
    bindings.add(binding);
    if (Boolean.FALSE.equals(request.enabled())) {
      rules.compilePlan(withBindings(definition, bindings));
      bindings.set(bindings.size() - 1, copy(binding, binding.dependsOn(), false));
    }
    errors.put(code, descriptor);
    return bindings;
  }

  private static boolean isCustomerCode(String code) {
    return code != null && code.matches("CERG-[A-Z0-9]+(?:-[A-Z0-9]+)*") && code.length() <= 64;
  }

  private static String customerKey(int orgao, String mode, String identity) {
    return "customer.orgao" + orgao + "." + mode + "." + identity;
  }

  private static String requiredText(String value, String label, int maximum) {
    if (value == null || value.isBlank() || value.length() > maximum) {
      throw invalid(label + " deve conter de 1 a " + maximum + " caracteres.");
    }
    return value;
  }

  private static JsonNode requireExpression(JsonNode expression) {
    if (expression == null || (!expression.isObject() && !expression.isBoolean())) {
      throw invalid("Informe um predicado JSON Logic como objeto ou booleano.");
    }
    return expression;
  }

  private static List<String> extractValidatedRoots(RuleSetDefinition definition, JsonNode expression) {
    final List<String> roots;
    try {
      roots = ErgonOntologyRuleSetSupport.roots(expression);
    } catch (IllegalStateException ex) {
      throw new InvalidPolicy(org.praxisplatform.rules.plan.RulePlanIssueCode.PLAN_REFERENCE_MISSING.name(),
          ex.getMessage());
    }
    for (String root : roots) {
      if (!definition.availableRoots().contains(root)) {
        throw new InvalidPolicy(org.praxisplatform.rules.plan.RulePlanIssueCode.PLAN_REFERENCE_MISSING.name(),
            "A expressão referencia raiz ontológica indisponível neste RuleSet: " + root
                + " (raízes permitidas: " + definition.availableRoots() + ").");
      }
    }
    return roots;
  }

  private static void requirePolicy(DecisionSlot slot, OverridePolicy expected) {
    if (slot.overridePolicy() != expected) {
      throw invalid("O slot " + slot.slotKey() + " usa " + slot.overridePolicy()
          + " e a operação exige " + expected + ".");
    }
  }

  private static DecisionBinding copy(DecisionBinding binding, List<String> dependencies, boolean enabled) {
    return new DecisionBinding(binding.bindingKey(), binding.slotKey(), binding.source(),
        binding.compositionPolicy(), binding.executor(), dependencies, binding.order(), enabled,
        binding.falseDecision(), binding.falseReasonCode(), binding.requiredFactPaths(), binding.parameterization());
  }

  private static RuleSetDefinition withBindings(RuleSetDefinition definition, List<DecisionBinding> bindings) {
    return new RuleSetDefinition(definition.ref(), definition.availableRoots(), definition.slots(), bindings,
        definition.compatibility(), definition.failPolicy());
  }

  private static long count(List<DecisionSlot> slots, OverridePolicy policy) {
    return slots.stream().filter(slot -> slot.overridePolicy() == policy).count();
  }

  SimulationResponse simulate(SimulationRequest request) {
    String key = request.ruleSetKey();
    requireRuleSet(key);
    int baseline = request.baselineOrgao();
    int target = request.targetOrgao();
    requireOrgao(baseline);
    requireOrgao(target);
    JsonNode facts = resolveFacts(key, request.facts(), request.presetScenarioKey());
    RuleDecisionPlan baselinePlan;
    RuleDecisionPlan targetPlan;
    synchronized (rules) {
      baselinePlan = rules.resolvePlan(key, baseline);
      targetPlan = rules.resolvePlan(key, target);
    }
    return compare(baselinePlan, baseline, targetPlan, target, facts, rules.localErrors(targetPlan));
  }

  private JsonNode resolveFacts(String key, JsonNode facts, String preset) {
    if (facts != null && preset != null) throw invalid("Escolha fatos ou um cenário.");
    if (facts == null && preset != null) {
      facts = catalog.scenarios(key).stream().filter(scenario -> scenario.presetScenarioKey().equals(preset))
          .findFirst().orElseThrow(() -> invalid("Cenário desconhecido.")).facts();
    }
    if (facts == null || !facts.isObject()) throw invalid("Informe uma árvore de fatos.");
    return facts;
  }

  private SimulationResponse compare(RuleDecisionPlan baselinePlan, Integer baseline, RuleDecisionPlan targetPlan,
      int target, JsonNode facts, Map<String, RuleErrorDescriptor> targetErrors) {
    String now = Instant.now().toString();
    var baselineResult = evaluate(baselinePlan, baseline, facts, now, rules.localErrors(baselinePlan));
    var targetResult = evaluate(targetPlan, target, facts, now, targetErrors);
    var baselineSlots = PolicyStudioProjection.outcomes(baselineResult);
    var targetSlots = PolicyStudioProjection.outcomes(targetResult);
    var slots = new LinkedHashSet<>(baselineSlots.keySet());
    slots.addAll(targetSlots.keySet());
    return new SimulationResponse(baselineResult, targetResult,
        slots.stream().filter(slot -> !Objects.equals(baselineSlots.get(slot), targetSlots.get(slot))).toList(), now);
  }

  private SimulationResult evaluate(RuleDecisionPlan plan, Integer orgao, JsonNode facts, String now,
      Map<String, RuleErrorDescriptor> localErrors) {
    var result = rules.evaluate(plan, facts, now);
    var violations = result.decision() == RuleDecision.ALLOW ? List.<Violation>of()
        : new PraxisRuleViolationException(plan, result, ErgonRuleErrorCatalog.canonical(localErrors))
            .getViolations().stream().map(violation -> {
              var error = PolicyStudioProjection.error(localErrors, violation.code());
              return new Violation(violation.code(), violation.slotKey(), violation.bindingKey(),
                  violation.source(), violation.stage(), violation.decision(), violation.targetField(),
                  violation.messagePtBr(), error == null ? null : error.plsqlOrigin(),
                  error == null ? null : error.plsqlLineage());
            }).toList();
    var bindings = PolicyStudioProjection.bindingIndex(plan);
    var traces = result.bindingResults().stream().map(trace -> {
      var binding = bindings.get(trace.bindingKey());
      return new SlotTrace(trace.slotKey(), trace.bindingKey(), trace.stage(), binding.source(),
          binding.compositionPolicy(), trace.decision(), trace.reasonCodes().stream().findFirst().orElse(null),
          PolicyStudioProjection.isBypass(binding, bypassExpression));
    }).toList();
    return new SimulationResult(orgao, result.planDigest(), result.factsDigest(), result.decision(), result.reasonCodes(), violations, traces);
  }

  private void requireRuleSet(String key) {
    if (key == null || !rules.ruleSetKeys().contains(key)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Conjunto de regras desconhecido.");
    }
  }

  private static void requireOrgao(int orgao) {
    if (orgao <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Órgão inválido.");
  }

  private static InvalidPolicy invalid(String message) {
    return new InvalidPolicy("PLAN_OVERRIDE_INVALID", message);
  }

  static final class InvalidPolicy extends RuntimeException {
    private final String code;

    InvalidPolicy(String code, String message) {
      super(message);
      this.code = code;
    }

    String code() {
      return code;
    }
  }
}
