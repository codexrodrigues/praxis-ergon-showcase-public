package br.com.praxis.ergonshowcase.policystudio;

import static br.com.praxis.ergonshowcase.policystudio.PolicyStudioDtos.*;

import br.com.praxis.ergonshowcase.rules.ErgonRuleErrorCatalog;
import br.com.praxis.ergonshowcase.rules.ErgonRuleErrorCatalog.RuleErrorDescriptor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.praxisplatform.rules.contract.*;
import org.praxisplatform.rules.facts.PraxisFactTreeBuilder;
import org.praxisplatform.rules.parameters.RuleParameterResolver;
import org.praxisplatform.rules.plan.RuleDecisionPlan;

final class PolicyStudioProjection {
  private PolicyStudioProjection() {}

  static RuleSetDetail detail(RuleDecisionPlan plan, RuleDecisionPlan product, int orgao, String activationTag,
      PolicyStudioCatalog.Entry entry, List<PresetScenario> scenarios, Map<String, RuleErrorDescriptor> errors,
      JsonNode bypassExpression, ObjectMapper mapper) {
    var resolved = bindingIndex(plan);
    var positions = new HashMap<String, Integer>();
    for (int index = 0; index < plan.orderedBindings().size(); index++) {
      positions.putIfAbsent(plan.orderedBindings().get(index).slotKey(), index);
    }
    var bindings = plan.definition().bindings().stream()
        .collect(Collectors.groupingBy(DecisionBinding::slotKey));
    var slots = plan.definition().slots().stream()
        .sorted(Comparator.comparing(DecisionSlot::stage)
            .thenComparingInt(slot -> positions.getOrDefault(slot.slotKey(), Integer.MAX_VALUE)))
        .map(slot -> {
          var sourceBindings = bindings.getOrDefault(slot.slotKey(), List.of());
          return new Slot(slot.slotKey(), slot.stage(), slot.cardinality(), slot.overridePolicy(),
              slot.aggregationPolicy(), slot.overridePolicy() == OverridePolicy.REPLACEABLE,
              slot.overridePolicy() == OverridePolicy.PARAMETERIZABLE,
              slot.overridePolicy() == OverridePolicy.RESTRICT_ONLY && slot.cardinality() == SlotCardinality.MULTIPLE,
              sourceBindings.stream().anyMatch(binding -> isBypass(binding, bypassExpression)),
              template(slot, sourceBindings, mapper),
              sourceBindings.stream().map(binding -> projectBinding(slot, binding,
                  resolved.getOrDefault(binding.bindingKey(), binding), errors)).toList());
        }).toList();
    var definition = plan.definition();
    return new RuleSetDetail(definition.ref().ruleSetKey(), entry.title(), entry.description(),
        definition.availableRoots(), definition.conceptRoots(), definition.referencedFactPaths(),
        definition.factPathsByRoot(), definition.failPolicy(),
        product.planDigest(), plan.planDigest(), activationTag, orgao,
        !product.planDigest().equals(plan.planDigest()), slots, edges(plan, resolved), scenarios);
  }

  static Map<String, DecisionBinding> bindingIndex(RuleDecisionPlan plan) {
    return plan.orderedBindings().stream().collect(Collectors.toMap(
        DecisionBinding::bindingKey, binding -> binding));
  }

  private static ParameterTemplate template(DecisionSlot slot, List<DecisionBinding> bindings,
      ObjectMapper mapper) {
    var template = slot.parameterTemplate();
    if (template == null) return null;
    var values = bindings.stream().filter(DecisionBinding::enabled).map(DecisionBinding::parameterization)
        .filter(java.util.Objects::nonNull).findFirst().map(RuleParameterization::values).orElse(null);
    var effective = values == null ? mapper.createObjectNode()
        : RuleParameterResolver.resolve(template, values).effectiveValues();
    return new ParameterTemplate(template.templateKey(), template.templateVersion(), template.falseReasonCode(),
        template.parameters().stream().map(parameter -> new Parameter(parameter.name(), parameter.type(),
            parameter.unit(), parameter.minimum(), parameter.maximum(), parameter.defaultValue(),
            effective.get(parameter.name()) == null ? parameter.defaultValue() : effective.get(parameter.name())))
            .toList());
  }

  private static Binding projectBinding(DecisionSlot slot, DecisionBinding binding, DecisionBinding resolved,
      Map<String, RuleErrorDescriptor> errors) {
    var template = slot == null ? null : slot.parameterTemplate();
    var executor = resolved.executor() != null ? resolved.executor()
        : (template != null ? template.executor() : null);
    var expression = executor == null ? null : executor.expression();
    var requiredPaths = !resolved.requiredFactPaths().isEmpty() ? List.copyOf(resolved.requiredFactPaths())
        : (template != null && template.requiredFactPaths() != null ? List.copyOf(template.requiredFactPaths()) : List.<String>of());
    var facts = new LinkedHashSet<String>(PraxisFactTreeBuilder.extractReferencedFactPaths(expression));
    for (String reqPath : requiredPaths) {
      if (reqPath != null && reqPath.contains(".")) {
        facts.add(reqPath);
      }
    }
    var falseDecision = resolved.falseDecision() != null ? resolved.falseDecision()
        : (template != null ? template.falseDecision() : null);
    var falseReasonCode = resolved.falseReasonCode() != null ? resolved.falseReasonCode()
        : (template != null ? template.falseReasonCode() : null);
    var descriptor = binding.source() == DecisionSource.CUSTOMER ? errors.get(binding.bindingKey()) : null;
    if (descriptor == null) {
      descriptor = error(binding.source() == DecisionSource.CUSTOMER ? errors : Map.of(), falseReasonCode);
    }
    return new Binding(binding.bindingKey(), binding.source(), binding.compositionPolicy(), binding.enabled(),
        binding.order(), binding.dependsOn(), executor == null ? null : executor.type(),
        executor == null ? null : executor.implementationKey(), expression, falseDecision,
        falseReasonCode, requiredPaths, List.copyOf(facts), descriptor == null ? null : descriptor.messagePtBr(),
        descriptor == null ? null : descriptor.targetField(), descriptor == null ? null : descriptor.plsqlOrigin(),
        descriptor == null ? null : descriptor.plsqlLineage());
  }

  static RuleErrorDescriptor error(Map<String, RuleErrorDescriptor> local, String code) {
    if (code == null) return null;
    var descriptor = local.get(code);
    return descriptor == null ? ErgonRuleErrorCatalog.find(code).orElse(null) : descriptor;
  }

  static boolean isBypass(DecisionBinding binding, JsonNode bypassExpression) {
    if (!binding.enabled() || binding.source() != DecisionSource.CUSTOMER
        || binding.compositionPolicy() != CompositionPolicy.REPLACE_EXACT) return false;
    var expression = binding.executor().expression();
    return bypassExpression.equals(expression) || (expression != null && expression.isBoolean() && expression.booleanValue());
  }

  private static List<DagEdge> edges(RuleDecisionPlan plan, Map<String, DecisionBinding> bindings) {
    var result = new ArrayList<DagEdge>();
    DecisionBinding previous = null;
    for (var binding : plan.orderedBindings()) {
      for (String dependency : binding.dependsOn()) {
        result.add(edge(bindings.get(dependency), binding, EdgeKind.DEPENDS_ON));
      }
      // A transição visual entre estágios é distinta das dependências do compilador.
      if (previous != null && plan.slotsByKey().get(previous.slotKey()).stage()
          != plan.slotsByKey().get(binding.slotKey()).stage()
          && !binding.dependsOn().contains(previous.bindingKey())) {
        result.add(edge(previous, binding, EdgeKind.STAGE_SEQUENCE));
      }
      previous = binding;
    }
    return List.copyOf(result);
  }

  private static DagEdge edge(DecisionBinding from, DecisionBinding to, EdgeKind kind) {
    return new DagEdge(from.bindingKey(), to.bindingKey(), from.slotKey(), to.slotKey(), kind);
  }

  record Outcome(DecisionSource source, RuleDecision decision, boolean bypassed) {}

  static Map<String, List<Outcome>> outcomes(SimulationResult result) {
    return result.slotTraces().stream().collect(Collectors.groupingBy(SlotTrace::slotKey,
        LinkedHashMap::new, Collectors.mapping(trace ->
            new Outcome(trace.source(), trace.decision(), trace.bypassed()), Collectors.toList())));
  }
}
