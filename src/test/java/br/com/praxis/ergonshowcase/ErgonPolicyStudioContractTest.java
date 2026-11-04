package br.com.praxis.ergonshowcase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.praxis.ergonshowcase.config.SecurityConfig;
import br.com.praxis.ergonshowcase.policystudio.ErgonDomainRuleSetCatalogProvider;
import br.com.praxis.ergonshowcase.policystudio.ErgonPolicyStudioController;
import br.com.praxis.ergonshowcase.policystudio.ErgonPolicyStudioService;
import br.com.praxis.ergonshowcase.rules.ErgonRuleEngineFacade;
import br.com.praxis.ergonshowcase.rules.ErgonRuleViolationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.praxisplatform.rules.contract.DecisionBinding;
import org.praxisplatform.rules.contract.RuleDecision;
import org.praxisplatform.rules.contract.RuleSetDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@SpringJUnitWebConfig(ErgonPolicyStudioContractTest.WebConfig.class)
@TestPropertySource(properties = {"showcase.security.username=studio-test", "showcase.security.password=test-only"})
class ErgonPolicyStudioContractTest {
  private static final String STUDIO = "/api/ergon/policy-studio";
  private static final String AVERBACAO = "ergon.administracao-pessoal.averbacoes-tempo-servico.cadastro";
  private static final String RN013 = "ergon.administracao-pessoal.regras-frequencia.rn013";
  private static final String GUARD = "administracao-pessoal.averbacoes-tempo-servico.erg-00006";
  private static final String POLICY = "administracao-pessoal.averbacoes-tempo-servico.cerg-06107";
  private static final String LIMIT = "administracao-pessoal.averbacoes-tempo-servico.cerg-06109";
  private static final String EXTENSION = "administracao-pessoal.averbacoes-tempo-servico.cerg-06111";

  @Configuration(proxyBeanMethods = false)
  @EnableWebMvc
  @EnableWebSecurity
  @Import({SecurityConfig.class, ErgonPolicyStudioController.class, ErgonPolicyStudioService.class,
      ErgonDomainRuleSetCatalogProvider.class, ErgonRuleEngineFacade.class,
      br.com.praxis.ergonshowcase.config.ServidorPessoaAgeOptions.class})
  static class WebConfig {
    @Bean ObjectMapper objectMapper() {
      return new ObjectMapper();
    }

    @Bean org.springframework.core.convert.ConversionService conversionService() {
      return new org.springframework.format.support.DefaultFormattingConversionService();
    }
  }

  @Autowired WebApplicationContext context;
  @Autowired ObjectMapper json;
  @Autowired ErgonRuleEngineFacade rules;
  @Autowired ErgonPolicyStudioService studioService;
  @Autowired ErgonDomainRuleSetCatalogProvider domainRuleSetCatalogProvider;
  private MockMvc mvc;

  @BeforeEach
  void setUp() {
    rules.resetOrgaoPlan(AVERBACAO, 3);
    rules.resetOrgaoPlan(RN013, 3);
    rules.resetOrgaoPlan(RN013, 2);
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @Test
  void listsAndInspectsAllRuleSetsAndReplaysTheirThreeScenarios() throws Exception {
    var rows = json.readTree(mvc.perform(get(STUDIO + "/rule-sets").param("empCodigo", "2").with(reader()))
        .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(18))
        .andReturn().getResponse().getContentAsString()).path("data");
    assertThat(StreamSupport.stream(rows.spliterator(), false).map(row -> row.path("ruleSetKey").asText()))
        .containsExactlyElementsOf(rules.ruleSetKeys());
    assertThat(rows.valueStream().filter(row -> RN013.equals(row.path("ruleSetKey").asText()))
        .findFirst().orElseThrow().path("resourceKey").asText())
        .isEqualTo("administracao-pessoal.regras-frequencia-estatutaria");
    for (var row : rows) {
      String key = row.path("ruleSetKey").asText();
      assertThat(row.path("totalSlots").asInt()).isEqualTo(row.path("protectedGuardSlotsCount").asInt()
          + row.path("parameterizableSlotsCount").asInt() + row.path("replaceableSlotsCount").asInt()
          + row.path("restrictOnlySlotsCount").asInt());
      assertThat(row.path("activeCustomerOverridesCount").asInt()).isPositive();
      assertThat(row.path("availableRoots")).isNotEmpty();
      assertThat(row.path("referencedFactPaths")).isNotEmpty();
      assertThat(row.path("factPathsByRoot").isObject()).isTrue();
      assertThat(row.path("factPathsByRoot").path("draft")).isNotEmpty();
      var detail = detail(key, 2);
      assertThat(detail.path("availableRoots")).isEqualTo(row.path("availableRoots"));
      assertThat(detail.path("conceptRoots")).isEqualTo(row.path("conceptRoots"));
      assertThat(detail.path("referencedFactPaths")).isEqualTo(row.path("referencedFactPaths"));
      assertThat(detail.path("factPathsByRoot")).isEqualTo(row.path("factPathsByRoot"));
      assertThat(StreamSupport.stream(detail.path("availableRoots").spliterator(), false).map(JsonNode::asText))
          .as(key).doesNotContain("facts");
      assertThat(StreamSupport.stream(detail.path("referencedFactPaths").spliterator(), false).map(JsonNode::asText))
          .as(key).noneMatch(path -> path.equals("facts") || path.startsWith("facts."));
      assertThat(detail.path("slots").size()).isEqualTo(row.path("totalSlots").asInt());
      assertThat(detail.path("presetScenarios").size()).isEqualTo(3);
      var compliant = simulate(key, 2, "CONFORMIDADE_PADRAO");
      assertThat(compliant.at("/baseline/decision").asText()).as("%s: %s", key, compliant.path("baseline").path("violations"))
          .isEqualTo("ALLOW");
      assertThat(compliant.at("/target/decision").asText()).as("%s: %s", key, compliant.path("target").path("violations"))
          .isEqualTo("ALLOW");
      var customized = simulate(key, 2, "CUSTOMIZACAO_ORGAO_2");
      assertThat(customized.at("/baseline/decision").asText()).as(key).isEqualTo("ALLOW");
      assertThat(customized.at("/target/decision").asText()).as(key).isEqualTo("DENY");
      assertThat(customized.path("target").path("violations").findValuesAsText("source"))
          .as(key).contains("CUSTOMER");
      var guard = simulate(key, 2, "GUARD_ESTRUTURAL_OU_BYPASS");
      assertThat(guard.at("/baseline/decision").asText()).as(key).isEqualTo("DENY");
      var ruleSlots = rules.resolvePlan(key, null).slotsByKey();
      assertThat(StreamSupport.stream(guard.at("/baseline/violations").spliterator(), false)
          .filter(violation -> violation.hasNonNull("slotKey"))
          .map(violation -> ruleSlots.get(violation.path("slotKey").asText()).overridePolicy().name()))
          .as(key).containsAnyOf("FORBIDDEN", "REPLACEABLE");
    }
    var detail = detail(RN013, 2);
    assertThat(detail.path("dagEdges").findValuesAsText("kind")).contains("STAGE_SEQUENCE");
    assertThat(detail.path("slots").findValues("requiredFactPaths")).isNotEmpty();
    assertThat(detail.path("slots").findValues("referencedFactPaths")).isNotEmpty();
    assertThat(detail.path("slots").findValuesAsText("plsqlOrigin")).anyMatch(origin -> !origin.isBlank());
    assertThat(detail.path("slots").findValuesAsText("plsqlLineage")).anyMatch(lineage -> !lineage.isBlank());
  }

  @Test
  void defaultsToActiveOrganizationsOneAndTwoAndRequiresReadAccessToBoth() throws Exception {
    var payload = json.createObjectNode().put("ruleSetKey", AVERBACAO)
        .put("presetScenarioKey", "CONFORMIDADE_PADRAO");
    mvc.perform(postJson(STUDIO + "/simulate", payload).with(reader()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.baseline.empCodigo").value(1))
        .andExpect(jsonPath("$.data.target.empCodigo").value(2));
    mvc.perform(postJson(STUDIO + "/simulate", payload).with(user("reader-org2").authorities(
        new SimpleGrantedAuthority("ERGON_READ"), new SimpleGrantedAuthority("ERGON_POLICY_READ:2"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void rejectsProtectedBypassAndRequiresWriteAuthority() throws Exception {
    var payload = json.createObjectNode().put("mode", "BYPASS_SLOT").put("slotKey", GUARD).put("bypass", true);
    mvc.perform(postJson(mutation(AVERBACAO, "customize"), payload).with(reader()))
        .andExpect(status().isForbidden());
    mvc.perform(postJson(mutation(AVERBACAO, "customize"), payload).header("If-Match", rules.activationTag(AVERBACAO, 3)).with(writer()))
        .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("PLAN_OVERRIDE_INVALID"));
    assertThat(rules.resolvePlan(AVERBACAO, 3).planDigest()).isEqualTo(rules.resolvePlan(AVERBACAO, 1).planDigest());
  }

  @Test
  void publishesBypassForGdfAndRestoresProductWithoutChangingOtherOrganizations() throws Exception {
    var scenario = "GUARD_ESTRUTURAL_OU_BYPASS";
    var denied = simulate(AVERBACAO, 3, scenario);
    assertThat(denied.path("target").path("violations").findValuesAsText("code")).contains("CERG-06107");
    var payload = json.createObjectNode().put("mode", "BYPASS_SLOT").put("slotKey", POLICY).put("bypass", true);
    var active = customize(AVERBACAO, payload);
    assertThat(slot(active, POLICY).path("isBypassedInOrgao").asBoolean()).isTrue();
    var allowed = simulate(AVERBACAO, 3, scenario);
    assertThat(allowed.at("/baseline/decision").asText()).isEqualTo("DENY");
    assertThat(allowed.at("/target/decision").asText()).isEqualTo("ALLOW");
    assertThat(allowed.path("target").path("slotTraces").findValuesAsText("compositionPolicy")).contains("REPLACE_EXACT");
    assertThat(allowed.path("divergentSlots").valueStream().map(JsonNode::asText)).contains(POLICY);
    var facts = scenarioFacts(active, scenario);
    assertThat(rules.enforceAverbacaoTempoServico(3, (ObjectNode) facts).decision())
        .isEqualTo(RuleDecision.ALLOW);
    payload.put("bypass", false);
    customize(AVERBACAO, payload);
    assertThat(simulate(AVERBACAO, 3, scenario).at("/target/decision").asText()).isEqualTo("DENY");
  }

  @Test
  void exactReplacementRelinksDependenciesToTheNewBinding() throws Exception {
    var product = rules.productRuleSet(AVERBACAO);
    String dependentSlot = "administracao-pessoal.averbacoes-tempo-servico.cerg-06108";
    String dependency = "product." + POLICY;
    var bindings = product.bindings().stream().map(binding -> {
      if (!dependentSlot.equals(binding.slotKey())) return binding;
      return new DecisionBinding(binding.bindingKey(), binding.slotKey(), binding.source(),
          binding.compositionPolicy(), binding.executor(), List.of(dependency), binding.order(),
          binding.enabled(), binding.falseDecision(), binding.falseReasonCode(), binding.requiredFactPaths(),
          binding.parameterization());
    }).toList();
    rules.registerPlan(AVERBACAO, 3, new RuleSetDefinition(product.ref(), product.availableRoots(),
        product.slots(), bindings, product.compatibility(), product.failPolicy()));
    assertThat(detail(AVERBACAO, 3).path("dagEdges").findValuesAsText("kind")).contains("DEPENDS_ON");

    var replacement = json.createObjectNode().put("mode", "REPLACE_SLOT_EXPRESSION").put("slotKey", POLICY);
    replacement.set("expressionJson", json.readTree("""
        {"==":[{"var":"draft.documentoLegalId"},1]}
        """));
    var updated = customize(AVERBACAO, replacement);
    String customerKey = "customer.orgao3.replace." + POLICY;
    assertThat(updated.path("dagEdges").valueStream().filter(edge -> "DEPENDS_ON".equals(edge.path("kind").asText())))
        .anyMatch(edge -> customerKey.equals(edge.path("fromBindingKey").asText())
            && dependentSlot.equals(edge.path("toSlotKey").asText()));
    assertThat(slot(updated, POLICY).path("isBypassedInOrgao").asBoolean()).isFalse();
    assertThat(simulate(AVERBACAO, 3, "GUARD_ESTRUTURAL_OU_BYPASS").at("/target/decision").asText())
        .isEqualTo("ALLOW");
  }

  @Test
  void parameterAndAugmentAreScopedAndRejectedCandidatesKeepTheActivePlan() throws Exception {
    var parameters = json.createObjectNode().put("mode", "UPDATE_PARAMETERS").put("slotKey", LIMIT);
    parameters.putObject("parameterValues").put("limite", 170);
    var active = customize(AVERBACAO, parameters);
    assertThat(slot(active, LIMIT).at("/parameterTemplate/parameters/0/effectiveValue").asInt()).isEqualTo(170);
    assertThat(slot(detail(AVERBACAO, 1), LIMIT).at("/parameterTemplate/parameters/0/effectiveValue").asInt()).isEqualTo(999);
    parameters.withObject("/parameterValues").put("limite", 1000);
    mvc.perform(postJson(mutation(AVERBACAO, "customize"), parameters).header("If-Match", rules.activationTag(AVERBACAO, 3)).with(writer()))
        .andExpect(status().isUnprocessableEntity());
    assertThat(detail(AVERBACAO, 3).path("activePlanDigest")).isEqualTo(active.path("activePlanDigest"));

    var extension = json.createObjectNode().put("mode", "AUGMENT_SLOT").put("slotKey", EXTENSION)
        .put("ruleCode", "CERG-GDF-901").put("targetField", "documentoLegalId")
        .put("messagePtBr", "A política do GDF exige conferência documental.")
        .put("origin", "POLITICA_GDF").put("enabled", true);
    extension.set("expressionJson", json.readTree("""
        {"==":[{"var":"draft.documentoLegalId"},91]}
        """));
    var augmented = customize(AVERBACAO, extension);
    var result = simulate(AVERBACAO, 3, "CONFORMIDADE_PADRAO");
    assertThat(result.at("/baseline/decision").asText()).isEqualTo("ALLOW");
    assertThat(result.at("/target/decision").asText()).isEqualTo("DENY");
    assertThat(result.path("target").path("violations").findValuesAsText("code")).contains("CERG-GDF-901");
    assertThat(result.path("target").path("violations").findValuesAsText("source")).contains("CUSTOMER");
    assertThat(result.path("target").path("violations").findValuesAsText("plsqlOrigin")).contains("POLITICA_GDF");
    var facts = scenarioFacts(augmented, "CONFORMIDADE_PADRAO");
    var error = org.junit.jupiter.api.Assertions.assertThrows(ErgonRuleViolationException.class,
        () -> rules.enforceAverbacaoTempoServico(3, (ObjectNode) facts));
    assertThat(error.getViolations()).anyMatch(v -> v.code().equals("CERG-GDF-901")
        && v.messagePtBr().equals(extension.path("messagePtBr").asText()));
    mvc.perform(post(mutation(AVERBACAO, "reset")).header("If-Match", rules.activationTag(AVERBACAO, 3)).with(writer())).andExpect(status().isOk())
        .andExpect(jsonPath("$.data.customizedForOrgao").value(false));
    assertThat(simulate(AVERBACAO, 3, "CONFORMIDADE_PADRAO").at("/target/decision").asText()).isEqualTo("ALLOW");
  }

  @Test
  void scopesWritesAndRejectsStaleActivationEvenAfterReset() throws Exception {
    var path = STUDIO + "/rule-sets/" + AVERBACAO + "/orgaos/";
    var request = json.createObjectNode().put("mode", "BYPASS_SLOT").put("slotKey", POLICY).put("bypass", true);
    mvc.perform(get(STUDIO + "/rule-sets/" + AVERBACAO).param("empCodigo", "7").with(reader()))
        .andExpect(status().isForbidden());
    mvc.perform(postJson(path + "7/customize", request).header("X-Tenant", "3").with(writer()))
        .andExpect(status().isForbidden());
    mvc.perform(postJson(path + "3/customize", request).with(writer()))
        .andExpect(status().isPreconditionRequired());
    String initial = detail(AVERBACAO, 3).path("activationTag").asText();
    customize(AVERBACAO, request);
    mvc.perform(post(path + "3/reset").header("If-Match", initial).with(writer()))
        .andExpect(status().isPreconditionFailed());
    mvc.perform(post(path + "3/reset").header("If-Match", rules.activationTag(AVERBACAO, 3)).with(writer()))
        .andExpect(status().isOk());
    mvc.perform(postJson(path + "3/customize", request).header("If-Match", initial).with(writer()))
        .andExpect(status().isPreconditionFailed());
  }

  @Test
  void restoresFactoryReplacementAndPreservesUnspecifiedUrnValues() throws Exception {
    String slotKey = br.com.praxis.ergonshowcase.rules.Rn013RegrasFrequenciaRuleSet.SLOT_INTERP_MES_NON_NEGATIVE;
    var path = STUDIO + "/rule-sets/" + RN013 + "/orgaos/2/customize";
    var bypass = json.createObjectNode().put("mode", "BYPASS_SLOT").put("slotKey", slotKey).put("bypass", true);
    data(mvc.perform(postJson(path, bypass).header("If-Match", rules.activationTag(RN013, 2)).with(writer())));
    var restored = data(mvc.perform(postJson(path, bypass.put("bypass", false))
        .header("If-Match", rules.activationTag(RN013, 2)).with(writer())));
    assertThat(slot(restored, slotKey).path("bindings").valueStream()
        .filter(binding -> binding.path("enabled").asBoolean()).map(binding -> binding.path("falseReasonCode").asText()))
        .containsExactly("CERG-08385");

    String urnSlot = br.com.praxis.ergonshowcase.rules.Rn013RegrasFrequenciaRuleSet.SLOT_QTD_MAX_NON_NEGATIVE;
    var change = json.createObjectNode().put("mode", "UPDATE_PARAMETERS").put("slotKey", urnSlot);
    change.putObject("parameterValues").put("minAllowedQty", 1);
    var updated = data(mvc.perform(postJson(path, change).header("If-Match", rules.activationTag(RN013, 2)).with(writer())));
    assertThat(slot(updated, urnSlot).at("/parameterTemplate/parameters/1/effectiveValue").asInt()).isEqualTo(12);
    change.withObject("/parameterValues").put("minAllowedQty", 30).put("maxAllowedQty", 20);
    mvc.perform(postJson(path, change).header("If-Match", rules.activationTag(RN013, 2)).with(writer()))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void rejectsForeignReasonCodesAndProtectsTheSalePeriod() throws Exception {
    var expression = json.createObjectNode().put("mode", "REPLACE_SLOT_EXPRESSION")
        .put("slotKey", POLICY).put("falseReasonCode", "CERG-02205");
    expression.set("expressionJson", json.createObjectNode().set("==", json.createArrayNode().add(1).add(0)));
    mvc.perform(postJson(mutation(AVERBACAO, "customize"), expression)
        .header("If-Match", rules.activationTag(AVERBACAO, 3)).with(writer()))
        .andExpect(status().isUnprocessableEntity());
    String sale = "ergon.administracao-pessoal.vendas-licenca-especial.cadastro";
    var bypass = json.createObjectNode().put("mode", "BYPASS_SLOT")
        .put("slotKey", "administracao-pessoal.vendas-licenca-especial.cerg-06407").put("bypass", true);
    mvc.perform(postJson(mutation(sale, "customize"), bypass)
        .header("If-Match", rules.activationTag(sale, 3)).with(writer()))
        .andExpect(status().isUnprocessableEntity());
    assertThat(simulate(AVERBACAO, 3, "CONFORMIDADE_PADRAO").at("/baseline/planDigest"))
        .isEqualTo(detail(AVERBACAO, 3).path("productPlanDigest"));
  }

  @Test
  void comparesAnUnpublishedCandidateWithoutChangingTheHead() throws Exception {
    var head = detail(AVERBACAO, 3);
    var request = json.createObjectNode().put("presetScenarioKey", "GUARD_ESTRUTURAL_OU_BYPASS");
    request.putObject("customization").put("mode", "BYPASS_SLOT").put("slotKey", POLICY).put("bypass", true);
    var result = data(mvc.perform(postJson(mutation(AVERBACAO, "preview"), request)
        .header("If-Match", head.path("activationTag").asText()).with(writer())));
    assertThat(result.at("/baseline/decision").asText()).isEqualTo("DENY");
    assertThat(result.at("/target/decision").asText()).isEqualTo("ALLOW");
    assertThat(result.at("/target/factsDigest")).isEqualTo(result.at("/baseline/factsDigest"));
    assertThat(detail(AVERBACAO, 3).path("activationTag")).isEqualTo(head.path("activationTag"));
    assertThat(simulate(AVERBACAO, 3, "GUARD_ESTRUTURAL_OU_BYPASS").at("/target/decision").asText())
        .isEqualTo("DENY");
  }

  @Test
  void rejectsInvertedRangesAcrossTheOtherGovernedBounds() throws Exception {
    for (var key : java.util.List.of(
        "ergon.administracao-pessoal.afastamentos.concessao",
        "ergon.administracao-pessoal.frequencias.lancamento",
        "ergon.administracao-pessoal.parametros-contagem-ferias.matriz")) {
      var slot = rules.productRuleSet(key).slots().stream()
          .filter(candidate -> candidate.parameterTemplate() != null).findFirst().orElseThrow();
      var parameters = slot.parameterTemplate().parameters();
      var request = json.createObjectNode().put("mode", "UPDATE_PARAMETERS").put("slotKey", slot.slotKey());
      request.putObject("parameterValues").put(parameters.get(0).name(), 20).put(parameters.get(1).name(), 10);
      String revision = detail(key, 3).path("activationTag").asText();
      mvc.perform(postJson(mutation(key, "customize"), request).header("If-Match", revision).with(writer()))
          .andExpect(status().isUnprocessableEntity())
          .andExpect(jsonPath("$.code").value("PLAN_OVERRIDE_INVALID"));
      assertThat(detail(key, 3).path("activationTag").asText()).isEqualTo(revision);
    }
  }

  @Test
  void projectsAll18GovernedRuleSetsIntoDomainRuleSetCatalogProviderAndSemanticDomainCatalog() throws Exception {
    var rows = json.readTree(mvc.perform(get(STUDIO + "/rule-sets").param("empCodigo", "2").with(reader()))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("data");
    var tenantHolder = new java.util.concurrent.atomic.AtomicReference<>("1");
    var domainCatalog = new org.praxisplatform.uischema.domain.SemanticDomainCatalogService(
        new org.praxisplatform.uischema.action.ActionDefinitionRegistry() {
          @Override public List<org.praxisplatform.uischema.action.ActionDefinition> findByResourceKey(String rk) { return List.of(); }
          @Override public List<org.praxisplatform.uischema.action.ActionDefinition> findByGroup(String g) { return List.of(); }
        },
        new org.praxisplatform.uischema.surface.SurfaceDefinitionRegistry() {
          @Override public List<org.praxisplatform.uischema.surface.SurfaceDefinition> findByResourceKey(String rk) { return List.of(); }
          @Override public List<org.praxisplatform.uischema.surface.SurfaceDefinition> findByGroup(String g) { return List.of(); }
        },
        org.praxisplatform.uischema.options.OptionSourceRegistry.empty(),
        null,
        null,
        java.time.Clock.systemUTC(),
        "praxis-ergon-showcase",
        "Praxis Ergon Showcase",
        "1.0.0",
        null,
        null,
        () -> java.util.Optional.ofNullable(tenantHolder.get()),
        domainRuleSetCatalogProvider);

    for (var row : rows) {
      String resourceKey = row.path("resourceKey").asText();
      String ruleSetKey = row.path("ruleSetKey").asText();
      var org1Descriptors = domainRuleSetCatalogProvider.findRuleSets(resourceKey, "1");
      var org2Descriptors = domainRuleSetCatalogProvider.findRuleSets(resourceKey, "2");
      assertThat(org1Descriptors).hasSize(1);
      assertThat(org2Descriptors).hasSize(1);
      assertThat(org1Descriptors.get(0).ruleSetKey()).isEqualTo(ruleSetKey);
      assertThat(org1Descriptors.get(0).availableRoots()).doesNotContain("facts");
      assertThat(org1Descriptors.get(0).referencedFactPaths())
          .noneMatch(path -> path.equals("facts") || path.startsWith("facts."));
      assertThat(org1Descriptors.get(0).factPathsByRoot().keySet())
          .containsExactlyElementsOf(org1Descriptors.get(0).availableRoots());
      assertThat(org1Descriptors.get(0).reasonDescriptors()).isNotEmpty();

      tenantHolder.set("1");
      var org1Catalog = domainCatalog.findByResourceKey(resourceKey);
      tenantHolder.set("2");
      var org2Catalog = domainCatalog.findByResourceKey(resourceKey);

      assertThat(org1Catalog.nodes())
          .anyMatch(node -> "policy_hint".equals(node.nodeType())
              && ruleSetKey.equals(node.metadata().get("ruleSetKey"))
              && node.metadata().containsKey("factPathsByRoot"));
      assertThat(org1Catalog.edges())
          .anyMatch(edge -> "governed_by".equals(edge.edgeType()) && resourceKey.equals(edge.sourceNodeKey()));
      assertThat(org1Catalog.bindings())
          .anyMatch(binding -> "rule_definition".equals(binding.bindingType())
              && ruleSetKey.equals(binding.target().get("ruleSetKey")));
      assertThat(org2Catalog.nodes())
          .anyMatch(node -> "policy_hint".equals(node.nodeType())
              && ruleSetKey.equals(node.metadata().get("ruleSetKey")));
    }

    String tag = rules.activationTag(AVERBACAO, 3);
    ObjectNode legacyFactsAttempt = json.createObjectNode()
        .put("mode", "REPLACE_SLOT_EXPRESSION")
        .put("slotKey", POLICY);
    legacyFactsAttempt.set("expressionJson", json.readTree("{\"==\":[{\"var\":\"facts.foo\"},1]}"));
    mvc.perform(postJson(mutation(AVERBACAO, "customize"), legacyFactsAttempt).header("If-Match", tag).with(writer()))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("PLAN_REFERENCE_MISSING"));

    ObjectNode unknownRootAttempt = json.createObjectNode()
        .put("mode", "REPLACE_SLOT_EXPRESSION")
        .put("slotKey", POLICY);
    unknownRootAttempt.set("expressionJson", json.readTree("{\"==\":[{\"var\":\"impedimento.tipo\"},\"FERIAS\"]}"));
    mvc.perform(postJson(mutation(AVERBACAO, "customize"), unknownRootAttempt).header("If-Match", tag).with(writer()))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("PLAN_REFERENCE_MISSING"));
  }

  private JsonNode detail(String key, int orgao) throws Exception {
    return data(mvc.perform(get(STUDIO + "/rule-sets/" + key).param("empCodigo", String.valueOf(orgao))
        .with(reader())));
  }

  private JsonNode simulate(String key, int target, String preset) throws Exception {
    var payload = json.createObjectNode().put("ruleSetKey", key)
        .put("targetEmpCodigo", target).put("presetScenarioKey", preset);
    return data(mvc.perform(postJson(STUDIO + "/simulate", payload).with(reader())));
  }

  private JsonNode customize(String key, JsonNode payload) throws Exception {
    return data(mvc.perform(postJson(mutation(key, "customize"), payload).header("If-Match", rules.activationTag(key, 3)).with(writer())));
  }

  private JsonNode data(org.springframework.test.web.servlet.ResultActions result) throws Exception {
    var response = result.andReturn().getResponse();
    String body = response.getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    assertThat(response.getStatus()).as(body).isEqualTo(200);
    return json.readTree(body).path("data");
  }

  private static JsonNode slot(JsonNode detail, String key) {
    return detail.path("slots").valueStream().filter(slot -> key.equals(slot.path("slotKey").asText()))
        .findFirst().orElseThrow();
  }

  private static JsonNode scenarioFacts(JsonNode detail, String key) {
    return detail.path("presetScenarios").valueStream()
        .filter(scenario -> key.equals(scenario.path("presetScenarioKey").asText()))
        .findFirst().orElseThrow().path("facts");
  }

  private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder postJson(
      String path, JsonNode body) {
    return post(path).contentType("application/json").content(body.toString());
  }

  private static String mutation(String key, String operation) {
    return STUDIO + "/rule-sets/" + key + "/orgaos/3/" + operation;
  }

  private static RequestPostProcessor reader() {
    return user("reader").authorities(new SimpleGrantedAuthority("ERGON_READ"),
        new SimpleGrantedAuthority("ERGON_POLICY_READ:1"), new SimpleGrantedAuthority("ERGON_POLICY_READ:2"),
        new SimpleGrantedAuthority("ERGON_POLICY_READ:3"));
  }

  private static RequestPostProcessor writer() {
    return user("writer").authorities(new SimpleGrantedAuthority("ERGON_WRITE"),
        new SimpleGrantedAuthority("ERGON_POLICY_READ:2"), new SimpleGrantedAuthority("ERGON_POLICY_PUBLISH:2"),
        new SimpleGrantedAuthority("ERGON_POLICY_READ:3"), new SimpleGrantedAuthority("ERGON_POLICY_PUBLISH:3"));
  }
}
