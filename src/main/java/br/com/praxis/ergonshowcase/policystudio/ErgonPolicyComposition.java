package br.com.praxis.ergonshowcase.policystudio;

import br.com.praxis.ergonshowcase.rules.ErgonRuleEngineFacade;
import br.com.praxis.ergonshowcase.rules.ErgonRuleErrorCatalog.RuleErrorDescriptor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.praxisplatform.config.dto.DomainRuleCompositionManifestRequest;
import org.praxisplatform.config.dto.DomainRuleCompositionManifestResponse;
import org.praxisplatform.config.dto.DomainRuleDefinitionResponse;
import org.praxisplatform.config.service.DomainRuleGovernancePrincipal;
import org.praxisplatform.config.service.DomainRuleService;
import org.praxisplatform.config.service.DomainRuleSnapshotService;
import org.praxisplatform.config.tx.ConfigTransactionManagerNames;
import org.praxisplatform.rules.contract.DecisionBinding;
import org.praxisplatform.rules.contract.DecisionSource;
import org.praxisplatform.rules.contract.RuleSetDefinition;
import org.praxisplatform.rules.contract.RuleSetRef;
import org.praxisplatform.rules.parameters.RuleParameterResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ErgonPolicyComposition {
  public static final String OWNER = "praxis-ergon-showcase";
  public static final String HOST_CONTRACT = "ergon-policy-studio/1.0";

  private final ErgonRuleEngineFacade rules;
  private final ErgonPolicyStudioService studio;
  private final DomainRuleService definitions;
  private final DomainRuleSnapshotService snapshots;
  private final ObjectMapper mapper;

  public ErgonPolicyComposition(ErgonRuleEngineFacade rules, ErgonPolicyStudioService studio,
      DomainRuleService definitions, DomainRuleSnapshotService snapshots, ObjectMapper mapper) {
    this.rules = rules;
    this.studio = studio;
    this.definitions = definitions;
    this.snapshots = snapshots;
    this.mapper = mapper;
  }

  @Transactional(transactionManager = ConfigTransactionManagerNames.CONFIG, readOnly = true,
      isolation = Isolation.REPEATABLE_READ)
  public DomainRuleCompositionManifestResponse prepare(String key, int organization,
      List<UUID> sourceIds, String validFrom, String validUntil, DomainRuleGovernancePrincipal principal) {
    if (!Integer.toString(organization).equals(principal.tenantId())) {
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.FORBIDDEN, "Órgão fora do escopo Config autenticado.");
    }
    if (sourceIds == null || sourceIds.isEmpty() || sourceIds.stream().anyMatch(java.util.Objects::isNull)
        || new HashSet<>(sourceIds).size() != sourceIds.size()) {
      throw new IllegalArgumentException("Informe IDs distintos de definições aprovadas.");
    }
    var plan = rules.factoryPlan(key, organization);
    var ref = plan.definition().ref();
    var sources = sourceIds.stream().map(id -> definitions.definition(id, principal))
        .sorted(Comparator.comparing(DomainRuleDefinitionResponse::ruleKey)).toList();
    Map<String, RuleErrorDescriptor> errors = Map.of();
    Set<String> composed = new HashSet<>();
    for (var source : sources) {
      if (!composed.add(source.ruleKey())) {
        throw new IllegalArgumentException("Cada binding deve ter uma única fonte aprovada.");
      }
      var request = readCustomization(source, ref);
      var candidate = studio.prepareCandidate(plan, organization, request, errors);
      var binding = candidate.definition().bindings().stream()
          .filter(item -> item.bindingKey().equals(source.ruleKey()))
          .filter(item -> item.enabled() && item.source() == DecisionSource.CUSTOMER)
          .filter(item -> item.slotKey().equals(request.slotKey()))
          .findFirst().orElseThrow(() -> new IllegalArgumentException(
              "A fonte deve identificar o binding habilitado produzido pela customização."));
      verifyApprovedInput(candidate.definition(), source, binding);
      plan = rules.compilePlan(candidate.definition());
      errors = candidate.errors();
    }
    var definition = plan.definition();
    var enabled = definition.bindings().stream().filter(DecisionBinding::enabled)
        .map(DecisionBinding::bindingKey).toList();
    if (!enabled.containsAll(composed)) {
      throw new IllegalArgumentException("As fontes aprovadas substituem ou desabilitam umas às outras.");
    }
    int maximum = snapshots.findMaximumRuleSetVersion(principal.tenantId(), principal.environment(), key);
    var versioned = new RuleSetRef(ref.domainKey(), ref.boundedContextKey(), ref.ruleSetKey(),
        ref.operationKey(), Math.addExact(maximum, 1));
    var descriptors = new HashMap<String, org.praxisplatform.rules.violation.RuleErrorDescriptor>();
    for (var binding : definition.bindings()) {
      if (!binding.enabled() || binding.source() != DecisionSource.CUSTOMER || binding.falseReasonCode() == null) continue;
      var error = errors.getOrDefault(binding.bindingKey(), errors.get(binding.falseReasonCode()));
      if (error != null) {
        descriptors.put(binding.bindingKey(), new org.praxisplatform.rules.violation.RuleErrorDescriptor(
            error.code(), error.targetField(), error.messagePtBr(), error.plsqlOrigin(), "CUSTOMER"));
      }
    }
    var ruleSet = new RuleSetDefinition(versioned, definition.availableRoots(), definition.slots(),
        definition.bindings(), definition.compatibility(), definition.failPolicy(), descriptors);
    rules.compileGovernedCandidate(key, ruleSet);
    return snapshots.prepareCompositionManifest(new DomainRuleCompositionManifestRequest(
        ruleSet, sourceIds, OWNER, HOST_CONTRACT, validFrom, validUntil),
        principal.tenantId(), principal.environment());
  }

  private PolicyStudioDtos.CustomizationRequest readCustomization(DomainRuleDefinitionResponse source, RuleSetRef ref) {
    if (!Set.of("approved", "active").contains(source.status()) || !"form_rule".equals(source.ruleType())
        || !OWNER.equals(source.serviceKey()) || !ref.ruleSetKey().equals(source.resourceKey())
        || !ref.boundedContextKey().equals(source.contextKey())
        || source.definition() == null || source.parameters() == null
        || !HOST_CONTRACT.equals(source.parameters().path("hostContractVersion").asText())
        || !"REFERENCE_DRAFT_ONLY".equals(source.governance() == null ? null
            : source.governance().path("lifecycleBoundary").asText())) {
      throw new IllegalArgumentException("Fonte fora do contrato de composição Ergon ou sem aprovação.");
    }
    try {
      return mapper.treeToValue(source.definition().required("ergonCustomization"),
          PolicyStudioDtos.CustomizationRequest.class);
    } catch (JsonProcessingException failure) {
      throw new IllegalArgumentException("Customização aprovada inválida.");
    }
  }

  private void verifyApprovedInput(RuleSetDefinition definition, DomainRuleDefinitionResponse source,
      DecisionBinding binding) {
    var executor = binding.executor();
    if (binding.parameterization() != null) {
      var template = definition.slots().stream().filter(slot -> slot.slotKey().equals(binding.slotKey()))
          .findFirst().orElseThrow().parameterTemplate();
      if (!mapper.valueToTree(template).equals(source.definition().path("parameterTemplate"))
          || !binding.parameterization().values().equals(source.parameters().path("parameterValues"))) {
        throw new IllegalArgumentException("Template ou valores declarados divergem da fonte aprovada.");
      }
      executor = RuleParameterResolver.resolve(template, binding.parameterization().values()).executor();
    }
    if (executor == null || executor.expression() == null || !executor.expression().equals(source.condition())) {
      throw new IllegalArgumentException("O predicado materializado diverge da condição aprovada.");
    }
  }
}
