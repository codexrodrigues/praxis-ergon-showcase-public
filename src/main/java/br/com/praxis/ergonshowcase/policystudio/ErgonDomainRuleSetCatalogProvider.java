package br.com.praxis.ergonshowcase.policystudio;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.rules.ErgonRuleEngineFacade;
import br.com.praxis.ergonshowcase.rules.ErgonRuleErrorCatalog;
import br.com.praxis.ergonshowcase.rules.Rn013RegrasFrequenciaRuleSet;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.praxisplatform.rules.contract.DecisionBinding;
import org.praxisplatform.rules.contract.DecisionSlot;
import org.praxisplatform.rules.contract.RuleSetDefinition;
import org.praxisplatform.rules.plan.RuleDecisionPlan;
import org.praxisplatform.uischema.domain.DomainRuleSetCatalogProvider;
import org.praxisplatform.uischema.domain.DomainRuleSetDescriptor;
import org.springframework.stereotype.Component;

/**
 * Publica os 18 {@link RuleSetDefinition}s estatutários e operacionais do Ergon RH Público
 * (incluindo customizações de órgão ativas e linhagem PL/SQL do {@link ErgonRuleErrorCatalog})
 * diretamente no catálogo semântico de domínio ({@code /schemas/domain}).
 */
@Component
public class ErgonDomainRuleSetCatalogProvider implements DomainRuleSetCatalogProvider {

  private final ErgonRuleEngineFacade rules;
  private final PolicyStudioCatalog catalog;

  public ErgonDomainRuleSetCatalogProvider(ErgonRuleEngineFacade rules, ObjectMapper mapper) {
    this.rules = rules;
    this.catalog = new PolicyStudioCatalog(mapper);
  }

  @Override
  public List<DomainRuleSetDescriptor> findRuleSets(String resourceKey, String tenantId) {
    if (resourceKey == null || resourceKey.isBlank()) {
      return List.of();
    }
    Integer orgao = parseOrgao(tenantId);
    List<DomainRuleSetDescriptor> result = new ArrayList<>();
    for (String key : rules.ruleSetKeys()) {
      var product = rules.productRuleSet(key);
      var ref = product.ref();
      if (ref == null) {
        continue;
      }
      String canonicalResourceKey =
          Rn013RegrasFrequenciaRuleSet.RESOURCE_KEY.equals(ref.boundedContextKey())
              ? ApiPaths.KEY_REGRAS_FREQUENCIA_ESTATUTARIA
              : ref.boundedContextKey();
      String normalizedResourceKey = resourceKey.trim();
      if (!normalizedResourceKey.equals(canonicalResourceKey)
          && !normalizedResourceKey.equals(ref.boundedContextKey())) {
        continue;
      }
      RuleDecisionPlan plan = rules.resolvePlan(key, orgao);
      PolicyStudioCatalog.Entry entry = catalog.entry(ref.boundedContextKey());
      Map<String, ErgonRuleErrorCatalog.RuleErrorDescriptor> localErrors = rules.localErrors(plan);
      result.add(
          DomainRuleSetDescriptor.of(
              normalizedResourceKey,
              plan.definition(),
              entry.title(),
              entry.description(),
              buildReasons(plan.definition(), localErrors)));
    }
    return List.copyOf(result);
  }

  private static List<DomainRuleSetDescriptor.RuleReasonDescriptor> buildReasons(
      RuleSetDefinition definition,
      Map<String, ErgonRuleErrorCatalog.RuleErrorDescriptor> localErrors) {
    Map<String, DomainRuleSetDescriptor.RuleReasonDescriptor> reasons = new LinkedHashMap<>();
    if (definition.slots() != null) {
      for (DecisionSlot slot : definition.slots()) {
        if (slot != null
            && slot.parameterTemplate() != null
            && slot.parameterTemplate().falseReasonCode() != null) {
          resolveError(slot.parameterTemplate().falseReasonCode(), slot.slotKey(), localErrors)
              .ifPresent(r -> reasons.putIfAbsent(r.code(), r));
        }
      }
    }
    if (definition.bindings() != null) {
      for (DecisionBinding binding : definition.bindings()) {
        if (binding != null && binding.enabled() && binding.falseReasonCode() != null) {
          resolveError(binding.falseReasonCode(), binding.slotKey(), localErrors)
              .ifPresent(r -> reasons.putIfAbsent(r.code(), r));
        }
      }
    }
    return List.copyOf(reasons.values());
  }

  private static Optional<DomainRuleSetDescriptor.RuleReasonDescriptor> resolveError(
      String code,
      String fallbackSlotKey,
      Map<String, ErgonRuleErrorCatalog.RuleErrorDescriptor> localErrors) {
    if (code == null || code.isBlank()) {
      return Optional.empty();
    }
    return Optional.ofNullable(localErrors.get(code))
        .or(() -> ErgonRuleErrorCatalog.find(code))
        .map(
            err ->
                new DomainRuleSetDescriptor.RuleReasonDescriptor(
                    err.code(),
                    err.slotKey() != null && !err.slotKey().isBlank()
                        ? err.slotKey()
                        : fallbackSlotKey,
                    err.targetField(),
                    err.messagePtBr(),
                    err.plsqlOrigin(),
                    err.plsqlLineage()));
  }

  private static Integer parseOrgao(String tenantId) {
    if (tenantId == null || tenantId.isBlank()) {
      return 1;
    }
    try {
      int parsed = Integer.parseInt(tenantId.trim());
      return parsed > 0 ? parsed : 1;
    } catch (NumberFormatException ignored) {
      return 1;
    }
  }
}
