package br.com.praxis.ergonshowcase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

import br.com.praxis.ergonshowcase.domain.DefinicaoCampoCustomizado;
import br.com.praxis.ergonshowcase.rules.ErgonRuleEngineFacade;
import br.com.praxis.ergonshowcase.rules.ErgonRuleErrorCatalog;
import br.com.praxis.ergonshowcase.rules.ErgonRuleViolationException;
import br.com.praxis.ergonshowcase.rules.OperationalRuleSet;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.praxisplatform.rules.contract.RuleDecision;
import org.praxisplatform.rules.contract.RuleEvaluationResult;
import org.praxisplatform.rules.plan.RuleDecisionPlan;

/** Replays the domain fixtures in isolation and checks the denials actually returned by the engine. */
final class RuleCatalogExecution {
  private static final ThreadLocal<RuleCatalogExecution> ACTIVE = new ThreadLocal<>();
  private final Set<Rejection> observed = new HashSet<>();
  private final Set<String> customFieldCodes = new HashSet<>();

  private record Rejection(String family, String code) {}

  static ErgonRuleEngineFacade facade() {
    RuleCatalogExecution execution = ACTIVE.get();
    return execution == null ? new ErgonRuleEngineFacade() : execution.new RecordingFacade();
  }

  static void verify() {
    var execution = new RuleCatalogExecution();
    execution.runFixtures();
    execution.assertComplete();
  }

  private void runFixtures() {
    var request = LauncherDiscoveryRequestBuilder.request()
        .configurationParameter("junit.jupiter.execution.parallel.enabled", "false")
        .selectors(
            selectClass(CadastroPessoalRuleSetTest.class),
            selectClass(VinculoDependenteCustomFieldRuleEngineContractTest.class),
            selectClass(Rn013RuleEngineContractTest.class),
            selectClass(FrequenciaAfastamentoRuleEngineContractTest.class),
            selectClass(FeriasRuleEngineAndDeterminationsContractTest.class),
            selectClass(TempoLicencaRuleSetContractTest.class),
            selectClass(FuncaoVagaRuleSetTest.class),
            selectClass(DesignacaoFuncaoRuleSetTest.class),
            selectClass(SubstituicaoFuncaoRuleSetTest.class),
            selectClass(OperationalRuleSetTest.class),
            selectClass(ErgonCrossSliceCustomizationMatrixTest.class))
        .build();
    var listener = new SummaryGeneratingListener();
    ACTIVE.set(this);
    try {
      LauncherFactory.create().execute(request, listener);
    } finally {
      ACTIVE.remove();
    }
    var summary = listener.getSummary();
    assertThat(summary.getTestsSucceededCount()).isPositive();
    assertThat(summary.getFailures().stream()
        .map(failure -> failure.getTestIdentifier().getDisplayName() + " - " + failure.getException())
        .toList())
        .as("Falhas nas fixtures de execução do catálogo")
        .isEmpty();
  }

  private void assertComplete() {
    var rules = new ErgonRuleEngineFacade();
    Set<String> missing = new TreeSet<>();
    Set<String> governedCodes = new HashSet<>();
    for (var family : ErgonCrossSliceCustomizationMatrixTest.families().toList()) {
      for (var definition : List.of(family.product().get(), family.customized().get())) {
        governedCodes.addAll(checkPlan(rules.compilePlan(definition), missing));
      }
    }
    var mapper = new ObjectMapper();
    var operationalCodes = new ArrayList<String>();
    for (var rule : OperationalRuleSet.values()) {
      operationalCodes.addAll(checkPlan(rules.compilePlan(rule.productRuleSet(mapper)), missing));
    }
    assertThat(operationalCodes).doesNotHaveDuplicates().doesNotContainAnyElementsOf(governedCodes);
    Set<String> catalogMissing = new TreeSet<>(ErgonRuleErrorCatalog.all().keySet());
    observed.forEach(rejection -> catalogMissing.remove(rejection.code));
    catalogMissing.removeAll(customFieldCodes);
    assertThat(missing).as("Bindings sem DENY observado. Códigos ausentes no catálogo executado: %s",
        catalogMissing).isEmpty();
    assertThat(catalogMissing).as("Códigos do catálogo sem DENY observado").isEmpty();
  }

  private List<String> checkPlan(RuleDecisionPlan plan, Set<String> missing) {
    var key = plan.definition().ref().ruleSetKey();
    var codes = new ArrayList<String>();
    for (var binding : plan.orderedBindings()) {
      String code = binding.falseReasonCode();
      if (code == null) continue;
      codes.add(code);
      if (!observed.contains(new Rejection(key, code))) missing.add(key + " / " + code);
    }
    return codes;
  }

  private void observe(RuleEvaluationResult result) {
    if (result == null) return;
    String family = result.ruleSetRef().ruleSetKey();
    for (var binding : result.bindingResults()) {
      if (binding.decision() == RuleDecision.DENY) {
        binding.reasonCodes().forEach(code -> observed.add(new Rejection(family, code)));
      }
    }
  }

  private final class RecordingFacade extends ErgonRuleEngineFacade {
    @Override
    public RuleEvaluationResult evaluate(RuleDecisionPlan plan, JsonNode facts, String nowUtc) {
      var result = super.evaluate(plan, facts, nowUtc);
      observe(result);
      return result;
    }

    @Override
    public RuleEvaluationResult enforce(RuleDecisionPlan plan, JsonNode facts) {
      try {
        return super.enforce(plan, facts);
      } catch (ErgonRuleViolationException denial) {
        observe(denial.getEvaluationResult());
        throw denial;
      }
    }

    @Override
    public void validateCustomFieldValidationRules(Map<String, Object> validationRules) {
      try {
        super.validateCustomFieldValidationRules(validationRules);
      } catch (ErgonRuleViolationException denial) {
        customFieldCodes.addAll(denial.getReasonCodes());
        throw denial;
      }
    }

    @Override
    public void evaluateCustomFieldsJsonLogic(String resourceKey, Integer orgao,
        Map<String, Object> values, Object draft,
        List<DefinicaoCampoCustomizado> definitions) {
      try {
        super.evaluateCustomFieldsJsonLogic(resourceKey, orgao, values, draft, definitions);
      } catch (ErgonRuleViolationException denial) {
        customFieldCodes.addAll(denial.getReasonCodes());
        throw denial;
      }
    }
  }
}

