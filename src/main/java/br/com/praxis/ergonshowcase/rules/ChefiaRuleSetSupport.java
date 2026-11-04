package br.com.praxis.ergonshowcase.rules;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import org.praxisplatform.rules.contract.OverridePolicy;
import org.praxisplatform.rules.contract.RuleParameterTemplate;
import org.praxisplatform.rules.contract.RuleSetDefinition;
import org.praxisplatform.rules.contract.RuleSetRef;

public final class ChefiaRuleSetSupport {
  private ChefiaRuleSetSupport() {}

  record Rule(
      String code,
      String field,
      String message,
      String source,
      String predicate,
      String expression,
      OverridePolicy policy) {
    Rule(
        String code,
        String field,
        String message,
        String origin,
        String lineage,
        String expression) {
      this(code, field, message, origin, lineage, expression, OverridePolicy.FORBIDDEN);
    }

    static Rule replaceable(
        String code,
        String field,
        String message,
        String origin,
        String lineage,
        String expression) {
      return new Rule(
          code, field, message, origin, lineage, expression, OverridePolicy.REPLACEABLE);
    }

    String slot() {
      return toOntologyRule().slot("chefia");
    }

    ErgonRuleErrorCatalog.RuleErrorDescriptor descriptor() {
      return descriptor(slot());
    }

    ErgonRuleErrorCatalog.RuleErrorDescriptor descriptor(String slot) {
      return toOntologyRule().descriptor(slot);
    }

    ErgonOntologyRuleSetSupport.Rule toOntologyRule() {
      return new ErgonOntologyRuleSetSupport.Rule(
          code, field, message, source, predicate, expression, policy);
    }
  }

  static RuleSetDefinition definition(String resource, List<Rule> rules) {
    return definition(resource, rules, null, false);
  }

  static RuleSetDefinition orgao2CustomizedRuleSet(
      String resource, List<Rule> rules, Rule customer) {
    return definition(resource, rules, customer, true);
  }

  static RuleSetDefinition definition(
      String resource, List<Rule> rules, Rule customer, boolean orgao2) {
    return definition(resource, rules, customer, orgao2, Map.of());
  }

  static RuleSetDefinition definition(
      String resource,
      List<Rule> rules,
      Rule customer,
      boolean orgao2,
      Map<String, RuleParameterTemplate> templates) {
    return ErgonOntologyRuleSetSupport.definition(
        new RuleSetRef(
            "workforce.leadership", resource, "ergon." + resource + ".chefia", "validate", 1),
        "chefia",
        "customer.orgao2.",
        rules.stream().map(Rule::toOntologyRule).toList(),
        customer == null ? null : customer.toOntologyRule(),
        orgao2,
        templates);
  }

  public static List<String> roots(JsonNode node) {
    return ErgonOntologyRuleSetSupport.roots(node);
  }
}
