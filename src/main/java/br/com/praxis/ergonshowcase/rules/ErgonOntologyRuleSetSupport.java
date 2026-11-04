package br.com.praxis.ergonshowcase.rules;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.praxisplatform.rules.contract.CompositionPolicy;
import org.praxisplatform.rules.contract.DecisionAggregationPolicy;
import org.praxisplatform.rules.contract.DecisionBinding;
import org.praxisplatform.rules.contract.DecisionSlot;
import org.praxisplatform.rules.contract.DecisionSource;
import org.praxisplatform.rules.contract.DecisionStage;
import org.praxisplatform.rules.contract.OverridePolicy;
import org.praxisplatform.rules.contract.RuleDecision;
import org.praxisplatform.rules.contract.RuleExecutorRef;
import org.praxisplatform.rules.contract.RuleFailPolicy;
import org.praxisplatform.rules.contract.RuleParameterDefinition;
import org.praxisplatform.rules.contract.RuleParameterTemplate;
import org.praxisplatform.rules.contract.RuleParameterType;
import org.praxisplatform.rules.contract.RuleParameterization;
import org.praxisplatform.rules.contract.RuleRuntimeCompatibility;
import org.praxisplatform.rules.contract.RuleSetDefinition;
import org.praxisplatform.rules.contract.RuleSetRef;
import org.praxisplatform.rules.contract.SlotCardinality;
import org.praxisplatform.rules.facts.PraxisFactTreeBuilder;

/**
 * Suporte declarativo canônico para construção de {@link RuleSetDefinition}s governados
 * sobre as 5 raízes ontológicas canônicas ({@code draft}, {@code current}, {@code <conceito>},
 * {@code neighborhood}, {@code context}), inferindo automaticamente {@code availableRoots} e
 * {@code requiredFactPaths} a partir da AST JSON Logic e rejeitando o saco escalar {@code facts.*}.
 */
public final class ErgonOntologyRuleSetSupport {
  private static final ObjectMapper JSON = new ObjectMapper();

  private ErgonOntologyRuleSetSupport() {}

  public record Rule(
      String code,
      String field,
      String message,
      String origin,
      String lineage,
      String expression,
      OverridePolicy policy) {

    public Rule(
        String code,
        String field,
        String message,
        String origin,
        String lineage,
        String expression) {
      this(code, field, message, origin, lineage, expression, OverridePolicy.FORBIDDEN);
    }

    public static Rule replaceable(
        String code,
        String field,
        String message,
        String origin,
        String lineage,
        String expression) {
      return new Rule(
          code, field, message, origin, lineage, expression, OverridePolicy.REPLACEABLE);
    }

    public String slot(String prefix) {
      return prefix + "." + code.toLowerCase(Locale.ROOT);
    }

    public ErgonRuleErrorCatalog.RuleErrorDescriptor descriptor(String slot) {
      return new ErgonRuleErrorCatalog.RuleErrorDescriptor(
          code, slot, field, message, origin, lineage);
    }
  }

  public static RuleSetDefinition definition(
      RuleSetRef ref,
      String slotPrefix,
      String customerKeyPrefix,
      List<Rule> product,
      Rule customer,
      boolean orgao2,
      Map<String, RuleParameterTemplate> templates) {
    List<DecisionSlot> slots = new ArrayList<>();
    List<DecisionBinding> bindings = new ArrayList<>();
    for (Rule rule : product) {
      String slot = rule.slot(slotPrefix);
      var template = templates.get(rule.code());
      OverridePolicy policy = template == null ? rule.policy() : OverridePolicy.PARAMETERIZABLE;
      slots.add(
          new DecisionSlot(
              slot,
              policy == OverridePolicy.FORBIDDEN
                  ? DecisionStage.PROTECTED_GUARD
                  : DecisionStage.DOMAIN_DECISION,
              SlotCardinality.SINGLE,
              policy,
              DecisionAggregationPolicy.SINGLE_RESULT,
              template));
      bindings.add(
          binding(
              "product." + slot,
              slot,
              DecisionSource.PRODUCT,
              null,
              rule.expression(),
              rule.code(),
              bindings.size() + 1));
    }
    if (customer != null) {
      String customerSlot = customer.slot(slotPrefix);
      slots.add(
          new DecisionSlot(
              customerSlot,
              DecisionStage.DOMAIN_DECISION,
              SlotCardinality.MULTIPLE,
              OverridePolicy.RESTRICT_ONLY,
              DecisionAggregationPolicy.DENY_OVERRIDES,
              null));
      if (orgao2) {
        bindings.add(
            binding(
                customerKeyPrefix + customerSlot,
                customerSlot,
                DecisionSource.CUSTOMER,
                CompositionPolicy.AUGMENT,
                customer.expression(),
                customer.code(),
                bindings.size() + 1));
      }
    }
    return RuleSetDefinition.withInferredRoots(
        ref, slots, bindings, RuleRuntimeCompatibility.current(), RuleFailPolicy.FAIL_CLOSED);
  }

  public static RuleParameterTemplate integerTemplate(
      String code,
      String expression,
      String key,
      String parameter,
      int defaultValue,
      int maximum,
      String pointer,
      String unit) {
    var product =
        binding("product.template", "template", DecisionSource.PRODUCT, null, expression, code, 0);
    return new RuleParameterTemplate(
        key,
        "1.0",
        product.executor(),
        RuleDecision.DENY,
        code,
        product.requiredFactPaths(),
        List.of(
            new RuleParameterDefinition(
                parameter,
                RuleParameterType.INTEGER,
                true,
                false,
                false,
                unit,
                JSON.getNodeFactory().numberNode(defaultValue),
                BigDecimal.ZERO,
                BigDecimal.valueOf(maximum),
                List.of(),
                List.of(pointer))));
  }

  public static RuleSetDefinition parameterize(
      RuleSetDefinition base, Map<String, Integer> values) {
    List<DecisionBinding> bindings = new ArrayList<>();
    for (var binding : base.bindings()) {
      var slot =
          base.slots().stream()
              .filter(s -> s.slotKey().equals(binding.slotKey()))
              .findFirst()
              .orElseThrow();
      Integer value =
          values.get(binding.falseReasonCode() == null ? "" : binding.falseReasonCode());
      var template = slot.parameterTemplate();
      if (binding.source() == DecisionSource.PRODUCT
          && value != null
          && template != null
          && base.bindings().stream()
              .noneMatch(
                  b ->
                      b.slotKey().equals(slot.slotKey())
                          && b.source() == DecisionSource.CUSTOMER
                          && b.enabled())) {
        bindings.add(
            new DecisionBinding(
                binding.bindingKey(),
                binding.slotKey(),
                binding.source(),
                binding.compositionPolicy(),
                binding.executor(),
                binding.dependsOn(),
                binding.order(),
                false,
                binding.falseDecision(),
                binding.falseReasonCode(),
                binding.requiredFactPaths(),
                binding.parameterization()));
        var parameters =
            JSON.createObjectNode().put(template.parameters().getFirst().name(), value);
        bindings.add(
            new DecisionBinding(
                "customer.config." + slot.slotKey(),
                slot.slotKey(),
                DecisionSource.CUSTOMER,
                CompositionPolicy.PARAMETERIZE,
                null,
                List.of(),
                binding.order(),
                true,
                null,
                null,
                List.of(),
                new RuleParameterization(
                    template.templateKey(), template.templateVersion(), parameters)));
      } else {
        bindings.add(binding);
      }
    }
    return new RuleSetDefinition(
        base.ref(),
        base.availableRoots(),
        base.slots(),
        bindings,
        base.compatibility(),
        base.failPolicy());
  }

  public static DecisionBinding binding(
      String key,
      String slot,
      DecisionSource source,
      CompositionPolicy policy,
      String expression,
      String code,
      int order) {
    try {
      var tree = JSON.readTree(expression);
      var requiredRoots = roots(tree);
      if (requiredRoots.contains("facts")) {
        throw new IllegalStateException(
            "Raiz escalar achatada 'facts' proibida na ontologia Praxis para " + key);
      }
      return new DecisionBinding(
          key,
          slot,
          source,
          policy,
          RuleExecutorRef.jsonLogic(tree),
          List.of(),
          order,
          true,
          RuleDecision.DENY,
          code,
          requiredRoots,
          null);
    } catch (JsonProcessingException invalid) {
      throw new IllegalStateException("Expressão JSON Logic inválida: " + key, invalid);
    }
  }

  public static List<String> roots(JsonNode node) {
    return PraxisFactTreeBuilder.extractReferencedRoots(node);
  }

  public static List<String> factPaths(JsonNode node) {
    return PraxisFactTreeBuilder.extractReferencedFactPaths(node);
  }

  public static List<String> mergeRequiredRoots(
      List<String> baseRequiredFactPaths, JsonNode expression) {
    List<String> merged =
        new ArrayList<>(baseRequiredFactPaths == null ? List.of() : baseRequiredFactPaths);
    for (String root : roots(expression)) {
      boolean covered =
          merged.stream().anyMatch(path -> path.equals(root) || path.startsWith(root + "."));
      if (!covered) {
        merged.add(root);
      }
    }
    return List.copyOf(merged);
  }
}
