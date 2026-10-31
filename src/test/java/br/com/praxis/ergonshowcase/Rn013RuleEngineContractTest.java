package br.com.praxis.ergonshowcase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.praxis.ergonshowcase.domain.RegraFrequenciaEstatutaria;
import br.com.praxis.ergonshowcase.mapper.RegraFrequenciaEstatutariaMapper;
import br.com.praxis.ergonshowcase.repository.RegraFrequenciaEstatutariaRepository;
import br.com.praxis.ergonshowcase.rules.ErgonRuleEngineFacade;
import br.com.praxis.ergonshowcase.rules.ErgonRuleErrorCatalog;
import br.com.praxis.ergonshowcase.rules.ErgonRuleViolationException;
import br.com.praxis.ergonshowcase.rules.Rn013RegrasFrequenciaRuleSet;
import br.com.praxis.ergonshowcase.service.RegraFrequenciaEstatutariaService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.praxisplatform.rules.contract.DecisionSource;
import org.praxisplatform.rules.contract.RuleDecision;
import org.praxisplatform.rules.contract.RuleEvaluationResult;
import org.praxisplatform.rules.contract.RuleRuntimeCompatibility;
import org.praxisplatform.rules.plan.RuleDecisionPlan;
import org.praxisplatform.rules.plan.RulePlanException;
import org.praxisplatform.rules.plan.RulePlanIssueCode;
import org.springframework.http.HttpStatus;

/**
 * Suíte determinística de contrato da família {@code RN-013} ({@code PCK_REGRAS_FREQ.MAIN_PRE} e
 * {@code PCK_REGRAS_FREQ.MAIN_POS}) sobre o {@code praxis-rules-engine:0.1.0-beta.17}.
 *
 * <p>Prova:
 * <ol>
 *   <li>Paridade integral das decisões do produto ({@code ERG-00019} e {@code ERG-08380} a {@code ERG-08393});
 *   <li>Modo 1 ({@code OverridePolicy.FORBIDDEN}): bloqueio de bypass em tempo de compilação do plano;
 *   <li>Modo 2 ({@code OverridePolicy.PARAMETERIZABLE} + {@code RuleParameterTemplate} — Contrato 1.6);
 *   <li>Modo 3 ({@code OverridePolicy.RESTRICT_ONLY} + {@code CompositionPolicy.AUGMENT} sob {@code DENY_OVERRIDES});
 *   <li>Modo 4 ({@code OverridePolicy.REPLACEABLE} + {@code CompositionPolicy.REPLACE_EXACT}) substituindo
 *       apenas 1 slot sem desligar as demais decisões do produto;
 *   <li>Integração no {@link RegraFrequenciaEstatutariaService} com semântica <i>fail-closed</i> (HTTP 422).
 * </ol>
 */
class Rn013RuleEngineContractTest {

  private ErgonRuleEngineFacade facade;

  @BeforeEach
  void setUp() {
    facade = new ErgonRuleEngineFacade();
  }

  @Test
  void productRuleSetCompilesCleanlyUnderContract16AndAllowsValidDraft() {
    assertThat(RuleRuntimeCompatibility.current().engineContractVersion()).isEqualTo("1.6");
    RuleDecisionPlan plan = facade.resolveRn013Plan(1);
    assertThat(plan.slotsByKey()).hasSize(15);
    assertThat(plan.planDigest()).isNotBlank();

    RegraFrequenciaEstatutaria validDraft = validBaselineDraft();
    RuleEvaluationResult result = facade.evaluateRn013(validDraft, List.of());

    assertThat(result.decision()).isEqualTo(RuleDecision.ALLOW);
    assertThat(result.reasonCodes()).isEmpty();
    assertThat(result.bindingResults()).hasSize(15);
    assertThat(result.bindingResults())
        .allSatisfy(binding -> assertThat(binding.decision()).isEqualTo(RuleDecision.ALLOW));
  }

  static Stream<Arguments> productDecisionViolations() {
    return Stream.of(
        Arguments.of(
            "ERG-00019",
            Rn013RegrasFrequenciaRuleSet.SLOT_PERIODO_ORDEM_VALIDA,
            (Consumer<RegraFrequenciaEstatutaria>)
                d -> {
                  d.setDtini(LocalDate.of(2025, 6, 1));
                  d.setDtfim(LocalDate.of(2025, 1, 1));
                }),
        Arguments.of(
            "ERG-08380",
            Rn013RegrasFrequenciaRuleSet.SLOT_QTD_MAX_NON_NEGATIVE,
            (Consumer<RegraFrequenciaEstatutaria>) d -> d.setQtdMaxDias(BigDecimal.valueOf(-1))),
        Arguments.of(
            "ERG-08381",
            Rn013RegrasFrequenciaRuleSet.SLOT_QTD_MIN_NON_NEGATIVE,
            (Consumer<RegraFrequenciaEstatutaria>) d -> d.setQtdMinDias(BigDecimal.valueOf(-1))),
        Arguments.of(
            "ERG-08382",
            Rn013RegrasFrequenciaRuleSet.SLOT_QTD_MAX_COHERENT_WITH_MIN,
            (Consumer<RegraFrequenciaEstatutaria>)
                d -> {
                  d.setQtdMinDias(BigDecimal.TEN);
                  d.setQtdMaxDias(BigDecimal.valueOf(5));
                }),
        Arguments.of(
            "ERG-08383",
            Rn013RegrasFrequenciaRuleSet.SLOT_CONSEC_MES_NON_NEGATIVE,
            (Consumer<RegraFrequenciaEstatutaria>) d -> d.setQtdMaxConsecMes(-1)),
        Arguments.of(
            "ERG-08384",
            Rn013RegrasFrequenciaRuleSet.SLOT_CONSEC_ANO_NON_NEGATIVE,
            (Consumer<RegraFrequenciaEstatutaria>) d -> d.setQtdMaxConsecAno(-1)),
        Arguments.of(
            "ERG-08385",
            Rn013RegrasFrequenciaRuleSet.SLOT_INTERP_MES_NON_NEGATIVE,
            (Consumer<RegraFrequenciaEstatutaria>) d -> d.setQtdMaxInterpMes(-1)),
        Arguments.of(
            "ERG-08386",
            Rn013RegrasFrequenciaRuleSet.SLOT_INTERP_ANO_NON_NEGATIVE,
            (Consumer<RegraFrequenciaEstatutaria>) d -> d.setQtdMaxInterpAno(-1)),
        Arguments.of(
            "ERG-08387",
            Rn013RegrasFrequenciaRuleSet.SLOT_INT_PRORROG_NON_NEGATIVE,
            (Consumer<RegraFrequenciaEstatutaria>) d -> d.setIntProrrogDias(-1)),
        Arguments.of(
            "ERG-08388",
            Rn013RegrasFrequenciaRuleSet.SLOT_INTERV_MIN_NON_NEGATIVE,
            (Consumer<RegraFrequenciaEstatutaria>) d -> d.setIntervMinDias(-1)),
        Arguments.of(
            "ERG-08389",
            Rn013RegrasFrequenciaRuleSet.SLOT_CONSEC_MES_CALENDAR_LIMIT,
            (Consumer<RegraFrequenciaEstatutaria>) d -> d.setQtdMaxConsecMes(32)),
        Arguments.of(
            "ERG-08390",
            Rn013RegrasFrequenciaRuleSet.SLOT_INTERP_MES_CALENDAR_LIMIT,
            (Consumer<RegraFrequenciaEstatutaria>) d -> d.setQtdMaxInterpMes(32)),
        Arguments.of(
            "ERG-08391",
            Rn013RegrasFrequenciaRuleSet.SLOT_CONSEC_ANO_CALENDAR_LIMIT,
            (Consumer<RegraFrequenciaEstatutaria>) d -> d.setQtdMaxConsecAno(367)),
        Arguments.of(
            "ERG-08392",
            Rn013RegrasFrequenciaRuleSet.SLOT_INTERP_ANO_CALENDAR_LIMIT,
            (Consumer<RegraFrequenciaEstatutaria>) d -> d.setQtdMaxInterpAno(367)));
  }

  @ParameterizedTest(name = "Decisão {0} no slot {1} bloqueia violação determinística")
  @MethodSource("productDecisionViolations")
  void everyProductDecisionInMainPreProducesCanonicalErgReasonCode(
      String expectedReasonCode,
      String expectedSlotKey,
      Consumer<RegraFrequenciaEstatutaria> mutator) {
    RegraFrequenciaEstatutaria draft = validBaselineDraft();
    mutator.accept(draft);

    RuleEvaluationResult result = facade.evaluateRn013(draft, List.of());

    assertThat(result.decision()).isEqualTo(RuleDecision.DENY);
    assertThat(result.reasonCodes()).contains(expectedReasonCode);
    assertThat(result.bindingResults())
        .anySatisfy(
            binding -> {
              assertThat(binding.slotKey()).isEqualTo(expectedSlotKey);
              assertThat(binding.decision()).isEqualTo(RuleDecision.DENY);
              assertThat(binding.reasonCodes()).contains(expectedReasonCode);
            });
    assertThat(ErgonRuleErrorCatalog.find(expectedReasonCode))
        .isPresent()
        .get()
        .satisfies(desc -> assertThat(desc.slotKey()).isEqualTo(expectedSlotKey));
  }

  @Test
  void erg08393PostDecisionJavaExecutorDetectsOverlappingValidityInSameScope() {
    RegraFrequenciaEstatutaria existing = validBaselineDraft();
    existing.setId(100L);
    existing.setDtini(LocalDate.of(2024, 1, 1));
    existing.setDtfim(LocalDate.of(2024, 12, 31));

    // Caso 1: Período concomitante [2024-06-01 .. 2025-06-01] para a mesma chave de escopo -> DENY ERG-08393
    RegraFrequenciaEstatutaria overlappingDraft = validBaselineDraft();
    overlappingDraft.setId(200L);
    overlappingDraft.setDtini(LocalDate.of(2024, 6, 1));
    overlappingDraft.setDtfim(LocalDate.of(2025, 6, 1));

    RuleEvaluationResult denyResult = facade.evaluateRn013(overlappingDraft, List.of(existing));
    assertThat(denyResult.decision()).isEqualTo(RuleDecision.DENY);
    assertThat(denyResult.reasonCodes()).containsExactly("ERG-08393");

    // Caso 2: Período posterior sem interseção [2025-01-01 .. 2025-12-31] -> ALLOW
    RegraFrequenciaEstatutaria nonOverlappingDraft = validBaselineDraft();
    nonOverlappingDraft.setId(201L);
    nonOverlappingDraft.setDtini(LocalDate.of(2025, 1, 1));
    nonOverlappingDraft.setDtfim(LocalDate.of(2025, 12, 31));

    RuleEvaluationResult allowResult = facade.evaluateRn013(nonOverlappingDraft, List.of(existing));
    assertThat(allowResult.decision()).isEqualTo(RuleDecision.ALLOW);
  }

  @Test
  void mode1ForbiddenOverrideOnProtectedGuardFailsAtPlanCompilationTime() {
    assertThatThrownBy(
            () ->
                facade.compilePlan(
                    Rn013RegrasFrequenciaRuleSet.customerMode1ForbiddenOverrideAttempt()))
        .isInstanceOf(RulePlanException.class)
        .satisfies(
            ex -> {
              RulePlanException planEx = (RulePlanException) ex;
              assertThat(planEx.getCode()).isEqualTo(RulePlanIssueCode.PLAN_OVERRIDE_INVALID);
              assertThat(planEx.getMessage())
                  .contains(Rn013RegrasFrequenciaRuleSet.SLOT_PERIODO_ORDEM_VALIDA);
            });
  }

  @Test
  void mode2ParameterizableSlotAdjustsBoundsPerOrgaoWithoutChangingJsonLogic() {
    RuleDecisionPlan orgao2ParamPlan =
        facade.compilePlan(
            Rn013RegrasFrequenciaRuleSet.withCustomerBindings(
                List.of(Rn013RegrasFrequenciaRuleSet.customerMode2ParameterizeBinding(0, 12))));

    RegraFrequenciaEstatutaria draft30Days = validBaselineDraft();
    draft30Days.setQtdMaxDias(BigDecimal.valueOf(30));

    // Órgão 1 (default 0..3650) aprova 30 dias
    RuleEvaluationResult orgao1Result =
        facade.evaluate(
            facade.resolveRn013Plan(1), facade.buildRn013Facts(draft30Days, List.of(), 1));
    assertThat(orgao1Result.decision()).isEqualTo(RuleDecision.ALLOW);

    // Órgão 2 (parametrizado 0..12 via RuleParameterTemplate) bloqueia 30 dias com ERG-08380
    RuleEvaluationResult orgao2Deny =
        facade.evaluate(orgao2ParamPlan, facade.buildRn013Facts(draft30Days, List.of(), 2));
    assertThat(orgao2Deny.decision()).isEqualTo(RuleDecision.DENY);
    assertThat(orgao2Deny.reasonCodes()).containsExactly("ERG-08380");

    // Órgão 2 aprova 12 dias (dentro do limite parametrizado)
    draft30Days.setQtdMaxDias(BigDecimal.valueOf(12));
    RuleEvaluationResult orgao2Allow =
        facade.evaluate(orgao2ParamPlan, facade.buildRn013Facts(draft30Days, List.of(), 2));
    assertThat(orgao2Allow.decision()).isEqualTo(RuleDecision.ALLOW);

    // Tentativa de parametrizar fora do envelope máximo do template (9999 > 3650) falha na compilação
    assertThatThrownBy(
            () ->
                facade.compilePlan(
                    Rn013RegrasFrequenciaRuleSet.withCustomerBindings(
                        List.of(
                            Rn013RegrasFrequenciaRuleSet.customerMode2ParameterizeBinding(
                                0, 9999)))))
        .isInstanceOf(RulePlanException.class)
        .satisfies(
            ex ->
                assertThat(((RulePlanException) ex).getCode())
                    .isEqualTo(RulePlanIssueCode.PLAN_PARAMETER_VALUES_INVALID));
  }

  @Test
  void mode3RestrictOnlyWithAugmentEnforcesCustomerAndProductRulesUnderDenyOverrides() {
    RuleDecisionPlan orgao2AugmentPlan =
        facade.compilePlan(
            Rn013RegrasFrequenciaRuleSet.withCustomerBindings(
                List.of(Rn013RegrasFrequenciaRuleSet.customerMode3AugmentBinding())));

    // Rascunho com qtdMaxDias = 45 e sem documentoLegalId
    RegraFrequenciaEstatutaria draftWithoutDoc = validBaselineDraft();
    draftWithoutDoc.setQtdMaxDias(BigDecimal.valueOf(45));
    draftWithoutDoc.setDocumentoLegalId(null);

    // Órgão 1 permite (produto não exige documentoLegalId para qtdMaxDias > 30)
    RuleEvaluationResult orgao1Allow =
        facade.evaluate(
            facade.resolveRn013Plan(1), facade.buildRn013Facts(draftWithoutDoc, List.of(), 1));
    assertThat(orgao1Allow.decision()).isEqualTo(RuleDecision.ALLOW);

    // Órgão 2 bloqueia via regra aditiva CERG-09001 (DecisionSource.CUSTOMER)
    RuleEvaluationResult orgao2DenyCustomer =
        facade.evaluate(orgao2AugmentPlan, facade.buildRn013Facts(draftWithoutDoc, List.of(), 2));
    assertThat(orgao2DenyCustomer.decision()).isEqualTo(RuleDecision.DENY);
    assertThat(orgao2DenyCustomer.reasonCodes()).containsExactly("CERG-09001");
    assertThat(orgao2DenyCustomer.bindingResults())
        .anySatisfy(
            b -> {
              assertThat(b.bindingKey()).isEqualTo("customer.emp2.rn013.augment_cerg09001");
              assertThat(b.slotKey())
                  .isEqualTo(Rn013RegrasFrequenciaRuleSet.SLOT_CONSEC_ANO_CALENDAR_LIMIT);
              assertThat(b.decision()).isEqualTo(RuleDecision.DENY);
              assertThat(b.reasonCodes()).containsExactly("CERG-09001");
            });

    // Quando documentoLegalId é informado, mas qtdMaxConsecAno = 367, a regra do produto ERG-08391 continua ativa!
    draftWithoutDoc.setDocumentoLegalId(91L);
    draftWithoutDoc.setQtdMaxConsecAno(367);
    RuleEvaluationResult orgao2DenyProduct =
        facade.evaluate(orgao2AugmentPlan, facade.buildRn013Facts(draftWithoutDoc, List.of(), 2));
    assertThat(orgao2DenyProduct.decision()).isEqualTo(RuleDecision.DENY);
    assertThat(orgao2DenyProduct.reasonCodes()).containsExactly("ERG-08391");
  }

  @Test
  void mode4ReplaceExactReplacesSingleAuthorizedSlotWithoutDisablingOther14ProductDecisions() {
    RuleDecisionPlan orgao2ReplacePlan =
        facade.compilePlan(
            Rn013RegrasFrequenciaRuleSet.withCustomerBindings(
                List.of(Rn013RegrasFrequenciaRuleSet.customerMode4ReplaceExactBinding())));

    RegraFrequenciaEstatutaria draftZeroInterpMes = validBaselineDraft();
    draftZeroInterpMes.setQtdMaxInterpMes(0);
    draftZeroInterpMes.setNaoSeAplica(false);

    // No Órgão 1 (produto), qtdMaxInterpMes = 0 é aceito por ERG-08385 (>= 0)
    assertThat(
            facade
                .evaluate(
                    facade.resolveRn013Plan(1),
                    facade.buildRn013Facts(draftZeroInterpMes, List.of(), 1))
                .decision())
        .isEqualTo(RuleDecision.ALLOW);

    // No Órgão 2 (REPLACE_EXACT em rn013.interp_mes_non_negative), qtdMaxInterpMes = 0 é negado com CERG-08385
    RuleEvaluationResult replacedDeny =
        facade.evaluate(
            orgao2ReplacePlan, facade.buildRn013Facts(draftZeroInterpMes, List.of(), 2));
    assertThat(replacedDeny.decision()).isEqualTo(RuleDecision.DENY);
    assertThat(replacedDeny.reasonCodes()).containsExactly("CERG-08385");

    // Prova anti-regressão do legado (IF v_ep THEN): todas as outras 14 decisões do produto continuam ativas!
    List<String> otherProductCodes =
        List.of(
            "ERG-00019",
            "ERG-08380",
            "ERG-08381",
            "ERG-08382",
            "ERG-08383",
            "ERG-08384",
            "ERG-08386",
            "ERG-08387",
            "ERG-08388",
            "ERG-08389",
            "ERG-08390",
            "ERG-08391",
            "ERG-08392");

    for (String expectedCode : otherProductCodes) {
      RegraFrequenciaEstatutaria candidate = validBaselineDraft();
      candidate.setQtdMaxInterpMes(2); // Satisfaz CERG-08385
      productDecisionViolations()
          .filter(args -> expectedCode.equals(args.get()[0]))
          .findFirst()
          .ifPresent(
              args -> {
                @SuppressWarnings("unchecked")
                Consumer<RegraFrequenciaEstatutaria> mutator =
                    (Consumer<RegraFrequenciaEstatutaria>) args.get()[2];
                mutator.accept(candidate);
              });

      RuleEvaluationResult check =
          facade.evaluate(orgao2ReplacePlan, facade.buildRn013Facts(candidate, List.of(), 2));
      assertThat(check.decision())
          .as("Decisão do produto %s deve permanecer ativa após REPLACE_EXACT de ERG-08385", expectedCode)
          .isEqualTo(RuleDecision.DENY);
      assertThat(check.reasonCodes()).contains(expectedCode);
    }

    // E ERG-08393 (POST_DECISION Java executor) também permanece ativo no plano com REPLACE_EXACT
    RegraFrequenciaEstatutaria existing = validBaselineDraft();
    existing.setId(500L);
    RegraFrequenciaEstatutaria overlapCandidate = validBaselineDraft();
    overlapCandidate.setId(501L);
    overlapCandidate.setQtdMaxInterpMes(2);
    RuleEvaluationResult overlapCheck =
        facade.evaluate(
            orgao2ReplacePlan, facade.buildRn013Facts(overlapCandidate, List.of(existing), 2));
    assertThat(overlapCheck.decision()).isEqualTo(RuleDecision.DENY);
    assertThat(overlapCheck.reasonCodes()).contains("ERG-08393");
  }

  @Test
  void serviceIntegrationEnforcesFailClosedSemanticsForDenyAndInconclusive() {
    RegraFrequenciaEstatutariaRepository repository =
        mock(RegraFrequenciaEstatutariaRepository.class);
    RegraFrequenciaEstatutariaService service =
        new RegraFrequenciaEstatutariaService(
            repository, new RegraFrequenciaEstatutariaMapper(), facade);

    RegraFrequenciaEstatutaria validDraft = validBaselineDraft();
    when(repository.findByEmpCodigoAndRegimeJuridicoAndTipofreqAndCodfreq(
            1, "ESTATUTARIO", "LICENCA", 201))
        .thenReturn(List.of());

    RuleEvaluationResult ok = service.validateRn013(validDraft);
    assertThat(ok.decision()).isEqualTo(RuleDecision.ALLOW);

    // Violação de domínio (DENY -> HTTP 422 com código, origem PRODUCT, slotKey e ProblemDetail RFC 7807)
    validDraft.setQtdMaxDias(BigDecimal.valueOf(-5));
    assertThatThrownBy(() -> service.validateRn013(validDraft))
        .isInstanceOf(ErgonRuleViolationException.class)
        .satisfies(
            ex -> {
              ErgonRuleViolationException ruleEx = (ErgonRuleViolationException) ex;
              assertThat(ruleEx.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
              assertThat(ruleEx.getReasonCodes()).contains("ERG-08380");
              assertThat(ruleEx.getSlotKeys())
                  .contains(Rn013RegrasFrequenciaRuleSet.SLOT_QTD_MAX_NON_NEGATIVE);
              assertThat(ruleEx.getDecisionSources()).containsExactly(DecisionSource.PRODUCT);
              assertThat(ruleEx.getReason())
                  .contains("[ERG-08380 | PRODUCT | slot=rn013.qtd_max_non_negative]");
              assertThat(ruleEx.getBody().getProperties())
                  .containsEntry("ruleSetKey", Rn013RegrasFrequenciaRuleSet.RULESET_KEY)
                  .containsEntry("decision", "DENY");
            });

    // Fato obrigatório ausente (dtini == null -> INCONCLUSIVE -> fail-closed HTTP 422)
    RegraFrequenciaEstatutaria missingMandatoryFact = validBaselineDraft();
    missingMandatoryFact.setDtini(null);
    assertThatThrownBy(() -> service.validateRn013(missingMandatoryFact))
        .isInstanceOf(ErgonRuleViolationException.class)
        .satisfies(
            ex -> {
              ErgonRuleViolationException ruleEx = (ErgonRuleViolationException) ex;
              assertThat(ruleEx.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
              assertThat(ruleEx.getEvaluationResult().decision())
                  .isEqualTo(RuleDecision.INCONCLUSIVE);
              assertThat(ruleEx.getReasonCodes()).contains("FACT_REQUIRED_MISSING");
            });
  }

  @Test
  void generalRuleWithNullScopeDimensionsStillEnforcesOverlapDetectionErg08393() {
    RegraFrequenciaEstatutariaRepository repository =
        mock(RegraFrequenciaEstatutariaRepository.class);
    RegraFrequenciaEstatutariaService service =
        new RegraFrequenciaEstatutariaService(
            repository, new RegraFrequenciaEstatutariaMapper(), facade);

    // Regra geral do ente público (empCodigo = null, regimeJuridico = null, tipoVinculo = null)
    RegraFrequenciaEstatutaria existingGeneral = validBaselineDraft();
    existingGeneral.setId(300L);
    existingGeneral.setEmpCodigo(null);
    existingGeneral.setRegimeJuridico(null);
    existingGeneral.setTipoVinculo(null);

    RegraFrequenciaEstatutaria candidateGeneral = validBaselineDraft();
    candidateGeneral.setId(301L);
    candidateGeneral.setEmpCodigo(null);
    candidateGeneral.setRegimeJuridico(null);
    candidateGeneral.setTipoVinculo(null);
    candidateGeneral.setDtini(LocalDate.of(2024, 6, 1));
    candidateGeneral.setDtfim(LocalDate.of(2025, 6, 1));

    when(repository.findByTipofreqAndCodfreq("LICENCA", 201))
        .thenReturn(List.of(existingGeneral));

    assertThatThrownBy(() -> service.validateRn013(candidateGeneral))
        .isInstanceOf(ErgonRuleViolationException.class)
        .satisfies(
            ex -> {
              ErgonRuleViolationException ruleEx = (ErgonRuleViolationException) ex;
              assertThat(ruleEx.getReasonCodes()).containsExactly("ERG-08393");
              assertThat(ruleEx.getSlotKeys())
                  .containsExactly(Rn013RegrasFrequenciaRuleSet.SLOT_CONCOMITANCIA_VIGENCIA_OVERLAP);
            });
  }

  @Test
  void ruleParamsUnitPairsAndProrrogacaoDomainEnforceErg08383Erg08387AndErg08388() {
    // 1. ERG-08387: prorrogacao = 'N' com intProrrogDias = 10 -> DENY ERG-08387
    RegraFrequenciaEstatutaria prorrogacaoVedadaComIntervalo = validBaselineDraft();
    prorrogacaoVedadaComIntervalo.setIntProrrogDias(10);
    prorrogacaoVedadaComIntervalo.setRuleParams(java.util.Map.of("prorrogacao", "N"));
    RuleEvaluationResult denyProrrog =
        facade.evaluateRn013(prorrogacaoVedadaComIntervalo, List.of());
    assertThat(denyProrrog.decision()).isEqualTo(RuleDecision.DENY);
    assertThat(denyProrrog.reasonCodes()).containsExactly("ERG-08387");

    // 2. ERG-08387: prorrogacao = 'N' sem intProrrogDias (null) -> ALLOW
    prorrogacaoVedadaComIntervalo.setIntProrrogDias(null);
    assertThat(facade.evaluateRn013(prorrogacaoVedadaComIntervalo, List.of()).decision())
        .isEqualTo(RuleDecision.ALLOW);

    // 3. ERG-08383: prazo informado em ruleParams sem tipoUnidadePrazo -> DENY ERG-08383
    RegraFrequenciaEstatutaria prazoSemUnidade = validBaselineDraft();
    prazoSemUnidade.setRuleParams(java.util.Map.of("prazo", 30));
    RuleEvaluationResult denyPrazo = facade.evaluateRn013(prazoSemUnidade, List.of());
    assertThat(denyPrazo.decision()).isEqualTo(RuleDecision.DENY);
    assertThat(denyPrazo.reasonCodes()).containsExactly("ERG-08383");

    // 4. ERG-08388: intersticio informado em ruleParams sem tipoUnidadeInterst -> DENY ERG-08388
    RegraFrequenciaEstatutaria intersticioSemUnidade = validBaselineDraft();
    intersticioSemUnidade.setRuleParams(java.util.Map.of("intersticio", 60));
    RuleEvaluationResult denyInterst = facade.evaluateRn013(intersticioSemUnidade, List.of());
    assertThat(denyInterst.decision()).isEqualTo(RuleDecision.DENY);
    assertThat(denyInterst.reasonCodes()).containsExactly("ERG-08388");
  }

  @Test
  void customerViolationPreservesCustomerSourceProvenanceAndControllerDiagnosticEndpointsWork() {
    RegraFrequenciaEstatutariaRepository repository =
        mock(RegraFrequenciaEstatutariaRepository.class);
    RegraFrequenciaEstatutariaService service =
        new RegraFrequenciaEstatutariaService(
            repository, new RegraFrequenciaEstatutariaMapper(), facade);
    br.com.praxis.ergonshowcase.controller.RegraFrequenciaEstatutariaController controller =
        new br.com.praxis.ergonshowcase.controller.RegraFrequenciaEstatutariaController();
    org.springframework.test.util.ReflectionTestUtils.setField(controller, "service", service);

    // Consulta do plano do Órgão 2 expõe os 15 slots e bindings CUSTOMER + PRODUCT
    var planResponse = controller.rn013Plan(2);
    assertThat(planResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(planResponse.getBody())
        .containsEntry("empCodigo", 2)
        .containsEntry("slotCount", 15);

    // Violação de regra customizada do Órgão 2 (CERG-08385 em REPLACE_EXACT) preserva DecisionSource.CUSTOMER
    RegraFrequenciaEstatutaria orgao2Invalid = validBaselineDraft();
    orgao2Invalid.setEmpCodigo(2);
    orgao2Invalid.setQtdMaxDias(BigDecimal.valueOf(10));
    orgao2Invalid.setQtdMaxInterpMes(0);
    when(repository.findByEmpCodigoAndRegimeJuridicoAndTipofreqAndCodfreq(
            2, "ESTATUTARIO", "LICENCA", 201))
        .thenReturn(List.of());

    var evalDiag = controller.evaluateRn013(orgao2Invalid).getBody();
    assertThat(evalDiag)
        .containsEntry("decision", "DENY")
        .containsEntry("allowed", false)
        .containsEntry("decisionSources", List.of("CUSTOMER"));

    assertThatThrownBy(() -> controller.validateRn013(orgao2Invalid))
        .isInstanceOf(ErgonRuleViolationException.class)
        .satisfies(
            ex -> {
              ErgonRuleViolationException ruleEx = (ErgonRuleViolationException) ex;
              assertThat(ruleEx.getReasonCodes()).containsExactly("CERG-08385");
              assertThat(ruleEx.getDecisionSources()).containsExactly(DecisionSource.CUSTOMER);
              assertThat(ruleEx.getReason())
                  .contains("[CERG-08385 | CUSTOMER | slot=rn013.interp_mes_non_negative]");
            });
  }

  private static RegraFrequenciaEstatutaria validBaselineDraft() {
    RegraFrequenciaEstatutaria draft = new RegraFrequenciaEstatutaria();
    draft.setId(10L);
    draft.setEmpCodigo(1);
    draft.setRegimeJuridico("ESTATUTARIO");
    draft.setTipoVinculo("EFETIVO");
    draft.setCategoria("GERAL");
    draft.setSubcategoria("PADRAO");
    draft.setTipofreq("LICENCA");
    draft.setCodfreq(201);
    draft.setDtini(LocalDate.of(2024, 1, 1));
    draft.setDtfim(LocalDate.of(2024, 12, 31));
    draft.setQtdMinDias(BigDecimal.ONE);
    draft.setQtdMaxDias(BigDecimal.valueOf(15));
    draft.setIntervMinDias(30);
    draft.setIntProrrogDias(10);
    draft.setQtdMaxConsecMes(15);
    draft.setQtdMaxInterpMes(15);
    draft.setQtdMaxConsecAno(60);
    draft.setQtdMaxInterpAno(90);
    draft.setExigeHomologacao(true);
    draft.setPermiteConcomitancia(false);
    draft.setNaoSeAplica(false);
    draft.setDocumentoLegalId(91L);
    draft.setRuleKey(Rn013RegrasFrequenciaRuleSet.RULESET_KEY);
    draft.setAtiva(true);
    return draft;
  }
}
