package br.com.praxis.ergonshowcase;

import static br.com.praxis.ergonshowcase.ErgonFailureAssertions.assertViolation;
import static br.com.praxis.ergonshowcase.TempoLicencaRuleSetContractTest.parse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.com.praxis.ergonshowcase.domain.DefinicaoCampoCustomizado;
import br.com.praxis.ergonshowcase.domain.Vinculo;
import br.com.praxis.ergonshowcase.rules.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.praxisplatform.rules.contract.DecisionSource;
import org.praxisplatform.rules.contract.RuleDecision;
import org.praxisplatform.rules.contract.RuleEvaluationResult;
import org.praxisplatform.rules.contract.RuleSetDefinition;
import org.praxisplatform.rules.plan.RuleDecisionPlan;

class ErgonCrossSliceCustomizationMatrixTest {
  private static final LocalDate DATE = LocalDate.of(2026, 5, 10);

  record Family(Supplier<RuleSetDefinition> product, Supplier<RuleSetDefinition> customized) {
    @Override public String toString() { return product.get().ref().boundedContextKey(); }
  }

  private record Scenario(Family family, String code,
      BiFunction<Integer, Boolean, RuleEvaluationResult> enforce, Consumer<Integer> register) {
    @Override public String toString() { return family + " " + code; }
  }

  static Stream<Family> families() {
    var families = new java.util.LinkedHashMap<String, Family>();
    scenarios(RuleCatalogExecution.facade()).map(Scenario::family)
        .forEach(family -> families.putIfAbsent(family.product.get().ref().ruleSetKey(), family));
    return families.values().stream();
  }

  static Stream<Scenario> matrix() {
    return scenarios(RuleCatalogExecution.facade());
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("matrix")
  void sameFactsUseTheActivePlanAndRegistrationChangesOnlyOrgao7(Scenario row) {
    allows(row, 1, false);
    allows(row, 7, false);
    rejects(row, 2);
    allows(row, 2, true);

    row.register.accept(7);
    rejects(row, 7);
    allows(row, 7, true);
    allows(row, 1, false);
    rejects(row, 2);
    allows(row, 2, true);
  }

  private static void allows(Scenario row, int orgao, boolean meetsRequirement) {
    assertThat(row.enforce.apply(orgao, meetsRequirement).decision()).isEqualTo(RuleDecision.ALLOW);
  }

  private static void rejects(Scenario row, int orgao) {
    assertViolation(row.code, DecisionSource.CUSTOMER, () -> row.enforce.apply(orgao, false));
  }

  @Test
  void cronologiaDeFeriasDisparaCodigoLegado() {
    var rules = RuleCatalogExecution.facade();
    var ctx = FeriasRuleEngineAndDeterminationsContractTest.validFeriasGozoContext();
    var plan = rules.resolveFeriasGozoPlan(1);
    assertThat(rules.enforce(plan, rules.buildFeriasGozoFacts(ctx)).decision())
        .isEqualTo(RuleDecision.ALLOW);
    ctx.draft().setDtprevfim(ctx.draft().getDtini().minusDays(1));
    assertViolation("VALID_FERIAS", DecisionSource.PRODUCT,
        () -> rules.enforce(plan, rules.buildFeriasGozoFacts(ctx)));
  }

  @Test
  void customFieldAdmissionRejectsContextDependentJsonLogic() {
    var rules = RuleCatalogExecution.facade();
    assertViolation("ERG-21501", DecisionSource.PRODUCT, () -> rules.validateCustomFieldValidationRules(
        Map.of("<", List.of(Map.of("var", "value"), Map.of("now", List.of())))));
  }

  @Test
  void dynamicFieldUsesTheDefinitionOfTheCurrentOrganization() {
    var rules = RuleCatalogExecution.facade();
    var base = customField(null, 10);
    var override = customField(2, 20);
    var org7 = customField(7, 30);
    var definitions = List.of(base, override, org7);
    var draft = new Vinculo();
    String resource = base.getResourceKey();
    rules.evaluateCustomFieldsJsonLogic(resource, 1, Map.of("limite", 10), draft, definitions);
    var error = assertThrows(ErgonRuleViolationException.class, () ->
        rules.evaluateCustomFieldsJsonLogic(resource, 2, Map.of("limite", 10), draft, definitions));
    assertThat(error.getHttpStatus()).isEqualTo(422);
    assertThat(error.getReasonCodes()).containsExactly("ERG-21502");
    assertThat(error.getViolations()).singleElement().satisfies(detail ->
        assertThat(detail.targetField()).isEqualTo("customFields.limite"));
    rules.evaluateCustomFieldsJsonLogic(resource, 2, Map.of("limite", 20), draft, definitions);
    assertThrows(ErgonRuleViolationException.class, () ->
        rules.evaluateCustomFieldsJsonLogic(resource, 7, Map.of("limite", 20), draft, definitions));
    rules.evaluateCustomFieldsJsonLogic(resource, 7, Map.of("limite", 30), draft, definitions);
  }

  private static DefinicaoCampoCustomizado customField(Integer orgao, int minimum) {
    var field = new DefinicaoCampoCustomizado();
    field.setEmpCodigo(orgao);
    field.setResourceKey("administracao-pessoal.vinculos");
    field.setFieldKey("limite");
    field.setFieldType("INTEGER");
    field.setLabel("Limite");
    field.setActive(true);
    field.setValidationRules(Map.of(">=", List.of(Map.of("var", "value"), minimum)));
    RuleCatalogExecution.facade().validateCustomFieldValidationRules(field.getValidationRules());
    return field;
  }

  private static Stream<Scenario> scenarios(ErgonRuleEngineFacade rules) {
    var rn013AnnualRules = RuleCatalogExecution.facade();
    var cadastro = Stream.of(
        row(CargoQuadroRuleSet::productRuleSet, CargoQuadroRuleSet::orgao2CustomizedRuleSet,
            "CERG-02108", rules::registerOrgaoCargoQuadroPlan, (orgao, compliant) -> {
              var cargo = CadastroPessoalRuleSetTest.cargoValido();
              cargo.setEmpCodigo(orgao);
              if (compliant) cargo.setDocumentoLegalId(91L);
              return rules.enforceCargoQuadro(cargo, null, 0, 0, false);
            }),
        row(ServidorPessoaCadastroRuleSet::productRuleSet, ServidorPessoaCadastroRuleSet::orgao2CustomizedRuleSet,
            "CERG-02206", rules::registerOrgaoServidorPessoaCadastroPlan, (orgao, compliant) -> {
              var pessoa = CadastroPessoalRuleSetTest.pessoaValida();
              if (compliant) pessoa.setPispasep("12345678901");
              return rules.enforceServidorPessoaCadastro(pessoa, orgao, 0, DATE);
            }),
        planned(rules, DependenteElegibilidadeRuleSet::productRuleSet, DependenteElegibilidadeRuleSet::orgao2CustomizedRuleSet,
            "CERG-09301", rules::registerOrgaoDependentePlan, rules::resolveDependentePlan,
            dependenteFacts(rules), "documentoLegalId"),
        planned(rules, VinculoProvimentoRuleSet::productRuleSet, VinculoProvimentoRuleSet::orgao2CustomizedRuleSet,
            "CERG-09401", rules::registerOrgaoVinculoPlan, rules::resolveVinculoPlan,
            vinculoFacts(rules), "documentoLegalId"),
        row(Rn013RegrasFrequenciaRuleSet::productRuleSet, Rn013RegrasFrequenciaRuleSet::orgao2CustomizedRuleSet,
            "ERG-08380", rules::registerOrgaoRn013Plan, (orgao, compliant) -> {
              var draft = Rn013RuleEngineContractTest.validBaselineDraft();
              draft.setQtdMaxDias(BigDecimal.valueOf(compliant ? 12 : 13));
              return rules.enforce(rules.resolveRn013Plan(orgao), rules.buildRn013Facts(draft, List.of(), orgao));
            }),
        row(Rn013RegrasFrequenciaRuleSet::productRuleSet, Rn013RegrasFrequenciaRuleSet::orgao2CustomizedRuleSet,
            "CERG-09001", rn013AnnualRules::registerOrgaoRn013Plan, (orgao, compliant) -> {
              var draft = Rn013RuleEngineContractTest.validBaselineDraft();
              draft.setQtdMaxDias(BigDecimal.valueOf(12));
              draft.setQtdMaxConsecAno(31);
              draft.setDocumentoLegalId(compliant ? 91L : null);
              return rn013AnnualRules.enforce(rn013AnnualRules.resolveRn013Plan(orgao),
                  rn013AnnualRules.buildRn013Facts(draft, List.of(), orgao));
            }),
        planned(rules, FrequenciaLancamentoRuleSet::productRuleSet, FrequenciaLancamentoRuleSet::orgao2CustomizedRuleSet,
            "CERG-09101", rules::registerOrgaoFrequenciaPlan, rules::resolveFrequenciaPlan,
            frequenciaFacts(rules), "documentoLegalId"),
        planned(rules, AfastamentoConcessaoRuleSet::productRuleSet, AfastamentoConcessaoRuleSet::orgao2CustomizedRuleSet,
            "CERG-09201", rules::registerOrgaoAfastamentoPlan, rules::resolveAfastamentoPlan,
            afastamentoFacts(rules), "atoPublicacaoId"),
        planned(rules, ParametroContagemFeriasRuleSet::productRuleSet, ParametroContagemFeriasRuleSet::orgao2CustomizedRuleSet,
            "CERG-PARAM-301", rules::registerOrgaoParametroContagemFeriasPlan, rules::resolveParametroContagemFeriasPlan,
            parametroFacts(rules), "documentoLegalFeriasId"),
        planned(rules, PeriodoAquisitivoFeriasRuleSet::productRuleSet, PeriodoAquisitivoFeriasRuleSet::orgao2CustomizedRuleSet,
            "CERG-PA-301", rules::registerOrgaoPeriodoAquisitivoFeriasPlan, rules::resolvePeriodoAquisitivoFeriasPlan,
            periodoFacts(rules), "documentoLegalId"),
        planned(rules, FeriasGozoRuleSet::productRuleSet, FeriasGozoRuleSet::orgao2CustomizedRuleSet,
            "CERG-FER-301", rules::registerOrgaoFeriasGozoPlan, rules::resolveFeriasGozoPlan,
            feriasFacts(rules), "documentoLegalId"));

    var tempoLicenca = TempoLicencaRuleSetContractTest.tenantCases().map(c -> {
      var tempoRules = RuleCatalogExecution.facade();
      BiFunction<Integer, RuleSetDefinition, RuleDecisionPlan> register = switch (c.resource()) {
        case AVERBACAO -> tempoRules::registerOrgaoAverbacaoTempoServicoPlan;
        case PERIODO -> tempoRules::registerOrgaoPeriodoAquisitivoLicencaEspecialPlan;
        case GOZO -> tempoRules::registerOrgaoLicencaEspecialGozoPlan;
        case VENDA -> tempoRules::registerOrgaoVendaLicencaEspecialPlan;
      };
      return row(c.resource().product, c.resource().customized, c.code(), register, (orgao, compliant) -> {
        var facts = c.resource().facts(c.setup());
        if (compliant) TempoLicencaRuleSetContractTest.merge(facts, parse(c.mutation()));
        return c.resource().enforcer(tempoRules).apply(orgao, facts);
      });
    });

    var chefia = Stream.of(
        row(FuncaoConfiancaRuleSet::productRuleSet, FuncaoConfiancaRuleSet::orgao2CustomizedRuleSet,
            "CERG-07102", rules::registerOrgaoFuncaoConfiancaPlan, (orgao, compliant) -> {
              var draft = FuncaoVagaRuleSetTest.funcao();
              draft.setEmpCodigo(orgao);
              draft.setSituacao("EXTINTA");
              draft.setDocumentoLegalId(5L);
              if (compliant) draft.setAtoPublicacaoId(7L);
              return rules.enforceFuncaoConfianca(draft);
            }),
        row(VagaFuncaoRuleSet::productRuleSet, VagaFuncaoRuleSet::orgao2CustomizedRuleSet,
            "CERG-07151", rules::registerOrgaoVagaFuncaoPlan, (orgao, compliant) -> {
              var draft = FuncaoVagaRuleSetTest.vaga();
              draft.setEmpCodigo(orgao);
              if (compliant) draft.setDocumentoLegalId(5L);
              return rules.enforceVagaFuncao(draft, null, FuncaoVagaRuleSetTest.funcao());
            }),
        row(DesignacaoFuncaoRuleSet::productRuleSet, DesignacaoFuncaoRuleSet::orgao2CustomizedRuleSet,
            "CERG-07161", rules::registerOrgaoDesignacaoFuncaoPlan, (orgao, compliant) -> {
              var draft = DesignacaoFuncaoRuleSetTest.draft(null, null);
              draft.setEmpCodigo(orgao);
              draft.setSituacao("EM_EXERCICIO");
              draft.setDtini(DATE);
              draft.setDtExercicio(DATE);
              draft.setVagaFuncaoId(1L);
              draft.setFuncaoId(2L);
              if (compliant) draft.setAtoPublicacaoId(20L);
              return rules.enforceDesignacaoFuncao(draft, "DESIGNADA", "LIVRE", 2L, 0);
            }),
        row(SubstituicaoFuncaoRuleSet::productRuleSet, SubstituicaoFuncaoRuleSet::orgao2CustomizedRuleSet,
            "CERG-07171", rules::registerOrgaoSubstituicaoFuncaoPlan, (orgao, compliant) -> {
              var facts = SubstituicaoFuncaoRuleSetTest.validFacts();
              var draft = (ObjectNode) facts.get("draft");
              draft.put("fim", 24).put("inicioRemunerado", 15).put("fimRemunerado", 20);
              if (compliant) draft.put("documentoLegalId", 20L);
              return rules.enforceSubstituicaoFuncao(orgao, facts);
            }));
    return Stream.of(cadastro, tempoLicenca, chefia).flatMap(stream -> stream);
  }

  private static Scenario row(Supplier<RuleSetDefinition> product, Supplier<RuleSetDefinition> customized,
      String code, BiFunction<Integer, RuleSetDefinition, RuleDecisionPlan> register,
      BiFunction<Integer, Boolean, RuleEvaluationResult> enforce) {
    return new Scenario(new Family(product, customized), code, enforce,
        orgao -> register.apply(orgao, customized.get()));
  }

  private static Scenario planned(ErgonRuleEngineFacade rules, Supplier<RuleSetDefinition> product,
      Supplier<RuleSetDefinition> customized, String code,
      BiFunction<Integer, RuleSetDefinition, RuleDecisionPlan> register, IntFunction<RuleDecisionPlan> resolve,
      ObjectNode baseline, String documentField) {
    return row(product, customized, code, register, (orgao, compliant) -> {
      var facts = baseline.deepCopy();
      var draft = (ObjectNode) facts.get("draft");
      if (compliant) {
        draft.put(documentField, 91L);
        if (facts.get("documentoLegal") instanceof ObjectNode document) {
          document.put("present", true).put("sameOrgao", true);
        }
      } else {
        draft.putNull(documentField);
      }
      return rules.enforce(resolve.apply(orgao), facts);
    });
  }

  private static ObjectNode dependenteFacts(ErgonRuleEngineFacade rules) {
    var ctx = VinculoDependenteCustomFieldRuleEngineContractTest.validDependenteContext();
    ctx.draft().setDtnasc(LocalDate.of(2006, 1, 1));
    ctx.draft().setDepSalarioFamilia(false);
    ctx.draft().setDepPlanoSaude(false);
    ctx.draft().setEstudante(true);
    ctx.draft().setUniversitario(true);
    return rules.buildDependenteFacts(ctx);
  }

  private static ObjectNode vinculoFacts(ErgonRuleEngineFacade rules) {
    var ctx = VinculoDependenteCustomFieldRuleEngineContractTest.validVinculoContext();
    ctx.draft().setSituacao("VACANTE");
    ctx.draft().setDtvac(LocalDate.of(2026, 5, 1));
    ctx.draft().setFormavac("EXONERACAO");
    return rules.buildVinculoFacts(ctx);
  }

  private static ObjectNode frequenciaFacts(ErgonRuleEngineFacade rules) {
    var ctx = FrequenciaAfastamentoRuleEngineContractTest.validFrequenciaContext();
    ctx.draft().setDtini(DATE);
    ctx.draft().setDtfim(DATE.plusDays(3));
    ctx.draft().setQuantidade(BigDecimal.valueOf(4));
    return rules.buildFrequenciaFacts(ctx);
  }

  private static ObjectNode afastamentoFacts(ErgonRuleEngineFacade rules) {
    var ctx = FrequenciaAfastamentoRuleEngineContractTest.validAfastamentoContext();
    ctx.draft().setDtini(LocalDate.of(2026, 3, 1));
    ctx.draft().setDtprevfim(LocalDate.of(2026, 4, 4));
    ctx.draft().setQuantidadeDias(35);
    ctx.draft().setDocumentoLegalId(91L);
    return rules.buildAfastamentoFacts(ctx);
  }

  private static ObjectNode parametroFacts(ErgonRuleEngineFacade rules) {
    var ctx = FeriasRuleEngineAndDeterminationsContractTest.validParametroContext();
    ctx.draft().setMaxPerFer(2);
    ctx.draft().setAbonoDirGozo("N");
    ctx.draft().setNumDiasAbonoFer(null);
    ctx.draft().setDenDiasAbonoFer(null);
    ctx.draft().setMinDiasPvendFer(null);
    ctx.draft().setMaxDiasVendFer(null);
    return rules.buildParametroContagemFeriasFacts(ctx);
  }

  private static ObjectNode periodoFacts(ErgonRuleEngineFacade rules) {
    var ctx = FeriasRuleEngineAndDeterminationsContractTest.validPaContext();
    ctx.draft().setDiasConvertidosDobro(10);
    ctx.draft().setSaldoDiasDisponivel(20);
    return rules.buildPeriodoAquisitivoFeriasFacts(ctx);
  }

  private static ObjectNode feriasFacts(ErgonRuleEngineFacade rules) {
    var ctx = FeriasRuleEngineAndDeterminationsContractTest.validFeriasGozoContext();
    ctx.draft().setAdiantamento13(true);
    return rules.buildFeriasGozoFacts(ctx);
  }
}

