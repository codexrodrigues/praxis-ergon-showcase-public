package br.com.praxis.ergonshowcase.rules;

import java.util.List;
import java.util.Map;
import org.praxisplatform.rules.contract.OverridePolicy;
import org.praxisplatform.rules.contract.RuleParameterTemplate;
import org.praxisplatform.rules.contract.RuleSetDefinition;
import org.praxisplatform.rules.contract.RuleSetRef;

/** Composição das validações cadastrais e da restrição adicional por órgão. */
final class CadastroPessoalRuleSetSupport {
  private CadastroPessoalRuleSetSupport() {}

  record Rule(
      String code,
      String field,
      String message,
      String origin,
      String lineage,
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

    String slot(String resource) {
      return toOntologyRule().slot(resource);
    }

    ErgonRuleErrorCatalog.RuleErrorDescriptor descriptor(String slot) {
      return toOntologyRule().descriptor(slot);
    }

    ErgonOntologyRuleSetSupport.Rule toOntologyRule() {
      return new ErgonOntologyRuleSetSupport.Rule(
          code, field, message, origin, lineage, expression, policy);
    }
  }

  static RuleSetDefinition definition(
      String resource, List<Rule> product, Rule customer, boolean orgao2) {
    return definition(resource, product, customer, orgao2, Map.of());
  }

  static RuleSetDefinition definition(
      String resource,
      List<Rule> product,
      Rule customer,
      boolean orgao2,
      Map<String, RuleParameterTemplate> templates) {
    return ErgonOntologyRuleSetSupport.definition(
        new RuleSetRef(
            "administracao-pessoal", resource, "ergon." + resource + ".cadastro", "validate", 1),
        resource,
        "customer.emp2.",
        product.stream().map(Rule::toOntologyRule).toList(),
        customer == null ? null : customer.toOntologyRule(),
        orgao2,
        templates);
  }

  static RuleParameterTemplate integerTemplate(
      Rule rule,
      String key,
      String parameter,
      int defaultValue,
      int maximum,
      String pointer,
      String unit) {
    return integerTemplate(
        rule.code(), rule.expression(), key, parameter, defaultValue, maximum, pointer, unit);
  }

  static RuleParameterTemplate integerTemplate(
      String code,
      String expression,
      String key,
      String parameter,
      int defaultValue,
      int maximum,
      String pointer,
      String unit) {
    return ErgonOntologyRuleSetSupport.integerTemplate(
        code, expression, key, parameter, defaultValue, maximum, pointer, unit);
  }

  static RuleSetDefinition parameterize(
      RuleSetDefinition base, Map<String, Integer> values) {
    return ErgonOntologyRuleSetSupport.parameterize(base, values);
  }

  static Rule somenteCadastro(Rule rule) {
    return new Rule(
        rule.code(),
        rule.field(),
        rule.message(),
        rule.origin(),
        rule.lineage(),
        """
        {"or":[{"!=":[{"var":"draft.acao"},null]},%s]}
        """
            .formatted(rule.expression()),
        rule.policy());
  }
}
