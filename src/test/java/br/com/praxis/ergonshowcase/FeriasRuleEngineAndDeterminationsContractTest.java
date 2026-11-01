package br.com.praxis.ergonshowcase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.praxis.ergonshowcase.config.TenantContext;
import br.com.praxis.ergonshowcase.domain.Afastamento;
import br.com.praxis.ergonshowcase.domain.CodigoFrequencia;
import br.com.praxis.ergonshowcase.domain.CodigoFrequenciaId;
import br.com.praxis.ergonshowcase.domain.FeriasGozo;
import br.com.praxis.ergonshowcase.domain.Frequencia;
import br.com.praxis.ergonshowcase.domain.ParametroContagemFerias;
import br.com.praxis.ergonshowcase.domain.PeriodoAquisitivoFerias;
import br.com.praxis.ergonshowcase.domain.Vinculo;
import br.com.praxis.ergonshowcase.domain.VinculoId;
import br.com.praxis.ergonshowcase.dto.FeriasGozoPreviaDraftDTO;
import br.com.praxis.ergonshowcase.dto.PeriodoAquisitivoPreviaDraftDTO;
import br.com.praxis.ergonshowcase.dto.TransicaoPeriodoAquisitivoFeriasDTO;
import br.com.praxis.ergonshowcase.mapper.FeriasGozoMapper;
import br.com.praxis.ergonshowcase.mapper.ParametroContagemFeriasMapper;
import br.com.praxis.ergonshowcase.mapper.PeriodoAquisitivoFeriasMapper;
import br.com.praxis.ergonshowcase.repository.FeriasGozoRepository;
import br.com.praxis.ergonshowcase.repository.ParametroContagemFeriasRepository;
import br.com.praxis.ergonshowcase.repository.PeriodoAquisitivoFeriasRepository;
import br.com.praxis.ergonshowcase.rules.EffectiveVacationParameterResolver;
import br.com.praxis.ergonshowcase.rules.EffectiveVacationParameterResolver.EffectiveParametroFeriasFacts;
import br.com.praxis.ergonshowcase.rules.ErgonRuleEngineFacade;
import br.com.praxis.ergonshowcase.rules.ErgonRuleEngineFacade.FeriasGozoRuleContext;
import br.com.praxis.ergonshowcase.rules.ErgonRuleEngineFacade.ParametroContagemFeriasRuleContext;
import br.com.praxis.ergonshowcase.rules.ErgonRuleEngineFacade.PeriodoAquisitivoFeriasRuleContext;
import br.com.praxis.ergonshowcase.rules.ErgonRuleErrorCatalog;
import br.com.praxis.ergonshowcase.rules.ErgonRuleViolationException;
import br.com.praxis.ergonshowcase.rules.FeriasGozoRuleSet;
import br.com.praxis.ergonshowcase.rules.ParametroContagemFeriasRuleSet;
import br.com.praxis.ergonshowcase.rules.PeriodoAquisitivoFeriasRuleSet;
import br.com.praxis.ergonshowcase.service.ErgonDeterminationService;
import br.com.praxis.ergonshowcase.service.ErgonReadFacts;
import br.com.praxis.ergonshowcase.service.FeriasGozoService;
import br.com.praxis.ergonshowcase.service.ParametroContagemFeriasService;
import br.com.praxis.ergonshowcase.service.PeriodoAquisitivoFeriasService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TypedQuery;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.praxisplatform.rules.contract.DecisionSource;
import org.praxisplatform.rules.contract.RuleDecision;
import org.praxisplatform.rules.contract.RuleEvaluationResult;
import org.praxisplatform.rules.plan.RuleDecisionPlan;
import org.praxisplatform.rules.plan.RulePlanException;
import org.praxisplatform.rules.plan.RulePlanIssueCode;
import org.praxisplatform.uischema.concurrency.ResourceVersionEtagService;
import org.praxisplatform.uischema.concurrency.ResourceVersionScope;
import org.praxisplatform.uischema.concurrency.ResourceVersionUpdatePrecondition;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Suíte determinística de contrato da Fase 3 do Slice 5 (Issue #8):
 * <ol>
 *   <li>Extração PL/SQL 4-Hops e cobertura integral dos códigos {@code ERG-*} em
 *       {@link ParametroContagemFeriasRuleSet}, {@link PeriodoAquisitivoFeriasRuleSet} e
 *       {@link FeriasGozoRuleSet} sobre o {@code praxis-rules-engine:0.1.0-beta.17} (Contrato 1.6);
 *   <li>Merge com Precedência ({@code PARAMETROS_CONTA (+) PERAQFER -> FERIAS}, Tarefa 24610 de
 *       {@code PCK_FERIAS.PKB}) via {@link EffectiveVacationParameterResolver};
 *   <li>Os 4 Modos Formais de Customização por Órgão ({@code empCodigo = 1} vs {@code empCodigo = 2}):
 *       {@code FORBIDDEN}, {@code PARAMETERIZABLE}, {@code RESTRICT_ONLY}/{@code AUGMENT} e
 *       {@code REPLACEABLE}/{@code REPLACE_EXACT};
 *   <li>Reflexo de {@link Frequencia} (redução progressiva de faltas {@code 30/24/18/12/0}) e
 *       {@link Afastamento} sobre o período aquisitivo, Determinações Reativas enriquecidas e
 *       enforcement <i>fail-closed</i> (HTTP 422) nos 3 serviços.
 * </ol>
 */
class FeriasRuleEngineAndDeterminationsContractTest {

  private ErgonRuleEngineFacade facade;

  @BeforeEach
  void setUp() {
    facade = new ErgonRuleEngineFacade();
  }

  @Test
  void allThreeProductRuleSetsCompileCleanlyUnderContract16AndAllowValidBaselines() {
    RuleDecisionPlan paramPlan = facade.resolveParametroContagemFeriasPlan(1);
    assertThat(paramPlan.slotsByKey()).hasSize(12);
    assertThat(paramPlan.planDigest()).isNotBlank();

    RuleEvaluationResult paramResult =
        facade.evaluateParametroContagemFerias(validParametroContext());
    assertThat(paramResult.decision()).isEqualTo(RuleDecision.ALLOW);
    assertThat(paramResult.reasonCodes()).isEmpty();

    RuleDecisionPlan paPlan = facade.resolvePeriodoAquisitivoFeriasPlan(1);
    assertThat(paPlan.slotsByKey()).hasSize(11);
    assertThat(paPlan.planDigest()).isNotBlank();

    RuleEvaluationResult paResult = facade.evaluatePeriodoAquisitivoFerias(validPaContext());
    assertThat(paResult.decision()).isEqualTo(RuleDecision.ALLOW);
    assertThat(paResult.reasonCodes()).isEmpty();

    RuleDecisionPlan fgPlan = facade.resolveFeriasGozoPlan(1);
    assertThat(fgPlan.slotsByKey()).hasSize(12);
    assertThat(fgPlan.planDigest()).isNotBlank();

    RuleEvaluationResult fgResult = facade.evaluateFeriasGozo(validFeriasGozoContext());
    assertThat(fgResult.decision()).isEqualTo(RuleDecision.ALLOW);
    assertThat(fgResult.reasonCodes()).isEmpty();
  }

  static Stream<Arguments> parametroContagemViolations() {
    return Stream.of(
        Arguments.of(
            "ERG-01873",
            ParametroContagemFeriasRuleSet.SLOT_VIGENCIA_ORDEM_VALIDA,
            (Supplier<ParametroContagemFeriasRuleContext>)
                () -> {
                  ParametroContagemFerias p = validParametro();
                  p.setDtini(LocalDate.of(2025, 6, 1));
                  p.setDtfim(LocalDate.of(2025, 5, 1));
                  return new ParametroContagemFeriasRuleContext(p, List.of());
                }),
        Arguments.of(
            "ERG-01870",
            ParametroContagemFeriasRuleSet.SLOT_PRESCRICAO_E_FLAGS_GOZO,
            (Supplier<ParametroContagemFeriasRuleContext>)
                () -> {
                  ParametroContagemFerias p = validParametro();
                  p.setCriterioPrescricao("X");
                  return new ParametroContagemFeriasRuleContext(p, List.of());
                }),
        Arguments.of(
            "ERG-01800",
            ParametroContagemFeriasRuleSet.SLOT_FERIAS_HABILITADAS_OBRIGATORIOS,
            (Supplier<ParametroContagemFeriasRuleContext>)
                () -> {
                  ParametroContagemFerias p = validParametro();
                  p.setTemFerias(true);
                  p.setTotDiasFer(0);
                  return new ParametroContagemFeriasRuleContext(p, List.of());
                }),
        Arguments.of(
            "ERG-01806",
            ParametroContagemFeriasRuleSet.SLOT_FERIAS_LIMITES_DIAS_TEMPLATE,
            (Supplier<ParametroContagemFeriasRuleContext>)
                () -> {
                  ParametroContagemFerias p = validParametro();
                  p.setTotDiasFer(100);
                  p.setMaxDiasPerFer(100);
                  return new ParametroContagemFeriasRuleContext(p, List.of());
                }),
        Arguments.of(
            "ERG-01809",
            ParametroContagemFeriasRuleSet.SLOT_PARCELAMENTO_COERENCIA,
            (Supplier<ParametroContagemFeriasRuleContext>)
                () -> {
                  ParametroContagemFerias p = validParametro();
                  p.setMinPerFer(4);
                  p.setMaxPerFer(2);
                  return new ParametroContagemFeriasRuleContext(p, List.of());
                }),
        Arguments.of(
            "ERG-01810",
            ParametroContagemFeriasRuleSet.SLOT_PARCELAMENTO_COERENCIA,
            (Supplier<ParametroContagemFeriasRuleContext>)
                () -> {
                  ParametroContagemFerias p = validParametro();
                  p.setMinDiasPerFer(15);
                  p.setMaxDiasPerFer(10);
                  return new ParametroContagemFeriasRuleContext(p, List.of());
                }),
        Arguments.of(
            "ERG-01851",
            ParametroContagemFeriasRuleSet.SLOT_PARCELAMENTO_COERENCIA,
            (Supplier<ParametroContagemFeriasRuleContext>)
                () -> {
                  ParametroContagemFerias p = validParametro();
                  p.setTotDiasFer(3);
                  p.setMaxDiasPerFer(3);
                  p.setMinDiasPerFer(1);
                  p.setMinDiasPerPrincFer(1);
                  p.setMaxPerFer(5);
                  p.setMinPerFer(1);
                  return new ParametroContagemFeriasRuleContext(p, List.of());
                }),
        Arguments.of(
            "ERG-01852",
            ParametroContagemFeriasRuleSet.SLOT_PARCELAMENTO_COERENCIA,
            (Supplier<ParametroContagemFeriasRuleContext>)
                () -> {
                  ParametroContagemFerias p = validParametro();
                  p.setTotDiasFer(20);
                  p.setMaxDiasPerFer(30);
                  return new ParametroContagemFeriasRuleContext(p, List.of());
                }),
        Arguments.of(
            "ERG-01802",
            ParametroContagemFeriasRuleSet.SLOT_ABONO_PECUNIARIO_FRACAO_LIMITES,
            (Supplier<ParametroContagemFeriasRuleContext>)
                () -> {
                  ParametroContagemFerias p = validParametro();
                  p.setAbonoDirGozo("D");
                  p.setNumDiasAbonoFer(4);
                  p.setDenDiasAbonoFer(3);
                  return new ParametroContagemFeriasRuleContext(p, List.of());
                }),
        Arguments.of(
            "ERG-01811",
            ParametroContagemFeriasRuleSet.SLOT_FERIAS_DESABILITADAS_SEM_LIMITES,
            (Supplier<ParametroContagemFeriasRuleContext>)
                () -> {
                  ParametroContagemFerias p = validParametro();
                  p.setTemFerias(false);
                  p.setTotDiasFer(30);
                  return new ParametroContagemFeriasRuleContext(p, List.of());
                }),
        Arguments.of(
            "ERG-01853",
            ParametroContagemFeriasRuleSet.SLOT_CONTAGEM_DOBRO_FERIAS_COERENCIA,
            (Supplier<ParametroContagemFeriasRuleContext>)
                () -> {
                  ParametroContagemFerias p = validParametro();
                  p.setContDobroFerias(true);
                  p.setMinDiasPeraqfer(null);
                  p.setMaxDiasPeraqfer(30);
                  return new ParametroContagemFeriasRuleContext(p, List.of());
                }),
        Arguments.of(
            "ERG-01854",
            ParametroContagemFeriasRuleSet.SLOT_CONTAGEM_DOBRO_FERIAS_COERENCIA,
            (Supplier<ParametroContagemFeriasRuleContext>)
                () -> {
                  ParametroContagemFerias p = validParametro();
                  p.setContDobroFerias(true);
                  p.setMinDiasPeraqfer(10);
                  p.setMaxDiasPeraqfer(null);
                  return new ParametroContagemFeriasRuleContext(p, List.of());
                }),
        Arguments.of(
            "ERG-01855",
            ParametroContagemFeriasRuleSet.SLOT_CONTAGEM_DOBRO_FERIAS_COERENCIA,
            (Supplier<ParametroContagemFeriasRuleContext>)
                () -> {
                  ParametroContagemFerias p = validParametro();
                  p.setContDobroFerias(false);
                  p.setMinDiasPeraqfer(10);
                  p.setMaxDiasPeraqfer(null);
                  return new ParametroContagemFeriasRuleContext(p, List.of());
                }),
        Arguments.of(
            "ERG-01856",
            ParametroContagemFeriasRuleSet.SLOT_CONTAGEM_DOBRO_FERIAS_COERENCIA,
            (Supplier<ParametroContagemFeriasRuleContext>)
                () -> {
                  ParametroContagemFerias p = validParametro();
                  p.setContDobroFerias(false);
                  p.setMinDiasPeraqfer(null);
                  p.setMaxDiasPeraqfer(20);
                  return new ParametroContagemFeriasRuleContext(p, List.of());
                }),
        Arguments.of(
            "ERG-00606",
            ParametroContagemFeriasRuleSet.SLOT_CONTAGEM_DOBRO_FERIAS_COERENCIA,
            (Supplier<ParametroContagemFeriasRuleContext>)
                () -> {
                  ParametroContagemFerias p = validParametro();
                  p.setContDobroFerias(true);
                  p.setMinDiasPeraqfer(25);
                  p.setMaxDiasPeraqfer(10);
                  return new ParametroContagemFeriasRuleContext(p, List.of());
                }),
        Arguments.of(
            "ERG-01813",
            ParametroContagemFeriasRuleSet.SLOT_LICENCA_ESPECIAL_PROGRESSAO_COERENCIA,
            (Supplier<ParametroContagemFeriasRuleContext>)
                () -> {
                  ParametroContagemFerias p = validParametro();
                  p.setTemLicEsp(true);
                  p.setAnosLicEsp(null);
                  p.setMesesLicEspPer(3);
                  return new ParametroContagemFeriasRuleContext(p, List.of());
                }),
        Arguments.of(
            "ERG-01850",
            ParametroContagemFeriasRuleSet.SLOT_CONCOMITANCIA_VIGENCIA_ESCOPO,
            (Supplier<ParametroContagemFeriasRuleContext>)
                () -> {
                  ParametroContagemFerias p = validParametro();
                  ParametroContagemFerias existing = validParametro();
                  existing.setId(999L);
                  existing.setDtini(LocalDate.of(2020, 1, 1));
                  existing.setDtfim(null);
                  return new ParametroContagemFeriasRuleContext(p, List.of(existing));
                }));
  }

  @ParameterizedTest(name = "ParametroContagemFerias [{index}] -> {0} no slot {1}")
  @MethodSource("parametroContagemViolations")
  void parametroContagemFeriasProductViolationsAreDeniedWithOfficialErgonCodes(
      String expectedCode,
      String expectedSlot,
      Supplier<ParametroContagemFeriasRuleContext> ctxSupplier) {
    RuleEvaluationResult result = facade.evaluateParametroContagemFerias(ctxSupplier.get());
    assertThat(result.decision()).isEqualTo(RuleDecision.DENY);
    assertThat(result.reasonCodes()).contains(expectedCode);
    assertThat(ErgonRuleErrorCatalog.find(expectedCode))
        .isPresent()
        .get()
        .satisfies(
            desc -> {
              assertThat(desc.targetField()).isNotBlank().doesNotContain("PCK_");
              assertThat(desc.messagePtBr())
                  .isNotBlank()
                  .isNotIn("PRODUCT_DECISION", "DB_INVARIANT", "CUSTOMER_EXTENSION_HOOK");
              assertThat(desc.plsqlOrigin()).contains("PCK_PARAMETROS_CONTA");
            });
    assertThat(result.bindingResults())
        .anySatisfy(
            b -> {
              assertThat(b.slotKey()).isEqualTo(expectedSlot);
              assertThat(b.decision()).isEqualTo(RuleDecision.DENY);
              assertThat(b.reasonCodes()).contains(expectedCode);
            });
  }

  static Stream<Arguments> periodoAquisitivoViolations() {
    return Stream.of(
        Arguments.of(
            "ERG-01851",
            PeriodoAquisitivoFeriasRuleSet.SLOT_CRONOLOGIA_DATAS_VALIDA,
            (Supplier<PeriodoAquisitivoFeriasRuleContext>)
                () -> {
                  PeriodoAquisitivoFeriasRuleContext base = validPaContext();
                  base.draft().setDtini(LocalDate.of(2024, 12, 31));
                  base.draft().setDtfim(LocalDate.of(2024, 1, 1));
                  return base;
                }),
        Arguments.of(
            "ERG-01850",
            PeriodoAquisitivoFeriasRuleSet.SLOT_VINCULO_EXERCICIO_VACANCIA,
            (Supplier<PeriodoAquisitivoFeriasRuleContext>)
                () -> {
                  PeriodoAquisitivoFeriasRuleContext base = validPaContext();
                  base.draft().setDtini(LocalDate.of(2018, 1, 1));
                  base.draft().setDtfim(LocalDate.of(2018, 12, 31));
                  return base;
                }),
        Arguments.of(
            "ERG-00099",
            PeriodoAquisitivoFeriasRuleSet.SLOT_BLOQUEIO_AVERBACAO_E_CRITERIO_PRESCRICAO,
            (Supplier<PeriodoAquisitivoFeriasRuleContext>)
                () -> {
                  PeriodoAquisitivoFeriasRuleContext base = validPaContext();
                  base.draft().setGeradoPorAverbacao(true);
                  return new PeriodoAquisitivoFeriasRuleContext(
                      base.draft(),
                      base.vinculo(),
                      base.resolvedParametroBase(),
                      base.parametroCandidates(),
                      base.existingPeriodos(),
                      base.parcelasDoPeriodo(),
                      true);
                }),
        Arguments.of(
            "ERG-00600",
            PeriodoAquisitivoFeriasRuleSet.SLOT_DIREITO_TETO_E_REDUCAO_FALTAS,
            (Supplier<PeriodoAquisitivoFeriasRuleContext>)
                () -> {
                  PeriodoAquisitivoFeriasRuleContext base = validPaContext();
                  // 10 faltas injustificadas reduz o teto legal de 30 para 24 dias
                  base.draft().setFaltasApuradas(10);
                  base.draft().setTotDiasDireito(30);
                  return base;
                }),
        Arguments.of(
            "ERG-00612",
            PeriodoAquisitivoFeriasRuleSet.SLOT_ACUMULO_E_FALTAS_LIMITES_TEMPLATE,
            (Supplier<PeriodoAquisitivoFeriasRuleContext>)
                () -> {
                  PeriodoAquisitivoFeriasRuleContext base = validPaContext();
                  PeriodoAquisitivoFerias open1 = validPa();
                  open1.setId(701L);
                  open1.setChave(1);
                  open1.setDtini(LocalDate.of(2022, 1, 1));
                  open1.setDtfim(LocalDate.of(2022, 12, 31));
                  open1.setSituacao("DISPONIVEL");
                  open1.setSaldoDiasDisponivel(30);

                  PeriodoAquisitivoFerias open2 = validPa();
                  open2.setId(702L);
                  open2.setChave(2);
                  open2.setDtini(LocalDate.of(2023, 1, 1));
                  open2.setDtfim(LocalDate.of(2023, 12, 31));
                  open2.setSituacao("DISPONIVEL");
                  open2.setSaldoDiasDisponivel(30);

                  base.draft().setChave(3);
                  return new PeriodoAquisitivoFeriasRuleContext(
                      base.draft(),
                      base.vinculo(),
                      base.parametroCandidates(),
                      List.of(open1, open2),
                      List.of());
                }),
        Arguments.of(
            "ERG-00604",
            PeriodoAquisitivoFeriasRuleSet.SLOT_OVERRIDES_CONTAGEM_DOBRO,
            (Supplier<PeriodoAquisitivoFeriasRuleContext>)
                () -> {
                  PeriodoAquisitivoFeriasRuleContext base = validPaContext();
                  base.draft().setContDobroFerias("S");
                  base.draft().setMinDiasPeraqfer(null);
                  base.draft().setMaxDiasPeraqfer(null);
                  return base;
                }),
        Arguments.of(
            "ERG-01809",
            PeriodoAquisitivoFeriasRuleSet.SLOT_OVERRIDES_PARCELAMENTO_E_ABONO,
            (Supplier<PeriodoAquisitivoFeriasRuleContext>)
                () -> {
                  PeriodoAquisitivoFeriasRuleContext base = validPaContext();
                  base.draft().setAbonoDirGozo("N");
                  base.draft().setMaxDiasVendFer(10);
                  return base;
                }),
        Arguments.of(
            "ERG-00615",
            PeriodoAquisitivoFeriasRuleSet.SLOT_PRESCRICAO_PARCELAS_ATIVAS,
            (Supplier<PeriodoAquisitivoFeriasRuleContext>)
                () -> {
                  PeriodoAquisitivoFeriasRuleContext base = validPaContext();
                  base.draft().setDtPrescricao(LocalDate.of(2025, 6, 30));
                  FeriasGozo parcela = validFeriasGozo();
                  parcela.setDtini(LocalDate.of(2025, 7, 10));
                  parcela.setDtprevfim(LocalDate.of(2025, 7, 24));
                  return new PeriodoAquisitivoFeriasRuleContext(
                      base.draft(),
                      base.vinculo(),
                      base.parametroCandidates(),
                      List.of(),
                      List.of(parcela));
                }),
        Arguments.of(
            "ERG-00601",
            PeriodoAquisitivoFeriasRuleSet.SLOT_SOBREPOSICAO_E_ACUMULO_PERIODOS,
            (Supplier<PeriodoAquisitivoFeriasRuleContext>)
                () -> {
                  PeriodoAquisitivoFeriasRuleContext base = validPaContext();
                  PeriodoAquisitivoFerias overlapping = validPa();
                  overlapping.setId(799L);
                  overlapping.setChave(9);
                  overlapping.setDtini(LocalDate.of(2024, 6, 1));
                  overlapping.setDtfim(LocalDate.of(2025, 5, 31));
                  return new PeriodoAquisitivoFeriasRuleContext(
                      base.draft(),
                      base.vinculo(),
                      base.parametroCandidates(),
                      List.of(overlapping),
                      List.of());
                }));
  }

  @ParameterizedTest(name = "PeriodoAquisitivoFerias [{index}] -> {0} no slot {1}")
  @MethodSource("periodoAquisitivoViolations")
  void periodoAquisitivoFeriasProductViolationsAreDeniedWithOfficialErgonCodes(
      String expectedCode,
      String expectedSlot,
      Supplier<PeriodoAquisitivoFeriasRuleContext> ctxSupplier) {
    RuleEvaluationResult result = facade.evaluatePeriodoAquisitivoFerias(ctxSupplier.get());
    assertThat(result.decision()).isEqualTo(RuleDecision.DENY);
    assertThat(result.reasonCodes()).contains(expectedCode);
    assertThat(ErgonRuleErrorCatalog.find(expectedCode))
        .isPresent()
        .get()
        .satisfies(
            desc -> {
              assertThat(desc.targetField()).isNotBlank().doesNotContain("PCK_");
              assertThat(desc.messagePtBr())
                  .isNotBlank()
                  .isNotIn("PRODUCT_DECISION", "DB_INVARIANT", "CUSTOMER_EXTENSION_HOOK");
              assertThat(desc.plsqlOrigin()).contains("PCK_");
            });
    assertThat(result.bindingResults())
        .anySatisfy(
            b -> {
              assertThat(b.slotKey()).isEqualTo(expectedSlot);
              assertThat(b.decision()).isEqualTo(RuleDecision.DENY);
              assertThat(b.reasonCodes()).contains(expectedCode);
            });
  }

  static Stream<Arguments> feriasGozoViolations() {
    return Stream.of(
        Arguments.of(
            "VALID_FERIAS",
            FeriasGozoRuleSet.SLOT_CRONOLOGIA_E_INTERRUPCAO_VALIDA,
            (Supplier<FeriasGozoRuleContext>)
                () -> {
                  FeriasGozoRuleContext base = validFeriasGozoContext();
                  base.draft().setDtini(LocalDate.of(2025, 7, 20));
                  base.draft().setDtprevfim(LocalDate.of(2025, 7, 10));
                  return base;
                }),
        Arguments.of(
            "VALID_FERIAS",
            FeriasGozoRuleSet.SLOT_VINCULO_EXERCICIO_VACANCIA_FREQUENCIA,
            (Supplier<FeriasGozoRuleContext>)
                () -> {
                  FeriasGozoRuleContext base = validFeriasGozoContext();
                  Afastamento af = new Afastamento();
                  af.setId(401L);
                  af.setNumfunc(1001L);
                  af.setNumvinc(1);
                  af.setDtini(LocalDate.of(2025, 7, 5));
                  af.setDtprevfim(LocalDate.of(2025, 7, 12));
                  af.setSituacao("HOMOLOGADO");
                  return new FeriasGozoRuleContext(
                      base.draft(),
                      base.periodoAquisitivo(),
                      base.vinculo(),
                      null,
                      base.parametroCandidates(),
                      base.existingPeriodosDoVinculo(),
                      List.of(),
                      List.of(),
                      List.of(af));
                }),
        Arguments.of(
            "ERG-00506",
            FeriasGozoRuleSet.SLOT_DIREITO_FERIAS_HABILITADO,
            (Supplier<FeriasGozoRuleContext>)
                () -> {
                  FeriasGozoRuleContext base = validFeriasGozoContext();
                  base.periodoAquisitivo().setSituacao("PRESCRITO");
                  return base;
                }),
        Arguments.of(
            "ERG-00502",
            FeriasGozoRuleSet.SLOT_SUBSTITUTO_FUNCIONAL_VALIDO,
            (Supplier<FeriasGozoRuleContext>)
                () -> {
                  FeriasGozoRuleContext base = validFeriasGozoContext();
                  base.draft().setNumfuncSubstituto(1001L);
                  base.draft().setNumvincSubstituto(1);
                  return base;
                }),
        Arguments.of(
            "ERG-00503",
            FeriasGozoRuleSet.SLOT_SUBSTITUTO_FUNCIONAL_VALIDO,
            (Supplier<FeriasGozoRuleContext>)
                () -> {
                  FeriasGozoRuleContext base = validFeriasGozoContext();
                  base.draft().setNumfuncSubstituto(2002L);
                  base.draft().setNumvincSubstituto(null);
                  return base;
                }),
        Arguments.of(
            "ERG-00504",
            FeriasGozoRuleSet.SLOT_SUBSTITUTO_FUNCIONAL_VALIDO,
            (Supplier<FeriasGozoRuleContext>)
                () -> {
                  FeriasGozoRuleContext base = validFeriasGozoContext();
                  Vinculo subst = validVinculo();
                  subst.setId(new VinculoId(2002L, 1));
                  base.draft().setNumfuncSubstituto(2002L);
                  base.draft().setNumvincSubstituto(1);
                  return new FeriasGozoRuleContext(
                      base.draft(),
                      base.periodoAquisitivo(),
                      base.vinculo(),
                      subst,
                      true,
                      null,
                      base.parametroCandidates(),
                      base.existingPeriodosDoVinculo(),
                      1,
                      List.of(),
                      List.of(),
                      List.of(),
                      LocalDate.of(2025, 6, 1),
                      true,
                      true);
                }),
        Arguments.of(
            "ERG-00507",
            FeriasGozoRuleSet.SLOT_BENEFICIOS_FINANCEIROS_ELEGIBILIDADE,
            (Supplier<FeriasGozoRuleContext>)
                () -> {
                  FeriasGozoRuleContext base = validFeriasGozoContext();
                  base.parametroCandidates().get(0).setAdiant13Fer(false);
                  base.draft().setAdiantamento13(true);
                  return base;
                }),
        Arguments.of(
            "ERG-00508",
            FeriasGozoRuleSet.SLOT_BENEFICIOS_FINANCEIROS_ELEGIBILIDADE,
            (Supplier<FeriasGozoRuleContext>)
                () -> {
                  FeriasGozoRuleContext base = validFeriasGozoContext();
                  base.parametroCandidates().get(0).setAdiantSalFer(false);
                  base.draft().setAdiantamentoSalario(true);
                  return base;
                }),
        Arguments.of(
            "ERG-00509",
            FeriasGozoRuleSet.SLOT_BENEFICIOS_FINANCEIROS_ELEGIBILIDADE,
            (Supplier<FeriasGozoRuleContext>)
                () -> {
                  FeriasGozoRuleContext base = validFeriasGozoContext();
                  base.parametroCandidates().get(0).setEmprestFer(false);
                  base.draft().setEmprestimoFerias(true);
                  return base;
                }),
        Arguments.of(
            "ERG-00510",
            FeriasGozoRuleSet.SLOT_BENEFICIOS_FINANCEIROS_ELEGIBILIDADE,
            (Supplier<FeriasGozoRuleContext>)
                () -> {
                  FeriasGozoRuleContext base = validFeriasGozoContext();
                  base.parametroCandidates().get(0).setAbonoConstFer(false);
                  base.draft().setAbonoConstitucional(true);
                  return base;
                }),
        Arguments.of(
            "ERG-00512",
            FeriasGozoRuleSet.SLOT_BENEFICIOS_FINANCEIROS_ELEGIBILIDADE,
            (Supplier<FeriasGozoRuleContext>)
                () -> {
                  FeriasGozoRuleContext base = validFeriasGozoContext();
                  base.draft().setOrdemParcela(2);
                  base.draft().setDiasGozo(15);
                  base.draft().setDtini(LocalDate.of(2025, 8, 1));
                  base.draft().setDtprevfim(LocalDate.of(2025, 8, 15));
                  base.draft().setAbonoConstitucional(true);
                  FeriasGozo p1 = validFeriasGozo();
                  p1.setId(801L);
                  p1.setOrdemParcela(1);
                  p1.setDiasGozo(15);
                  p1.setAbonoConstitucional(true);
                  return new FeriasGozoRuleContext(
                      base.draft(),
                      base.periodoAquisitivo(),
                      base.vinculo(),
                      null,
                      base.parametroCandidates(),
                      base.existingPeriodosDoVinculo(),
                      List.of(p1),
                      List.of(),
                      List.of());
                }),
        Arguments.of(
            "ERG-00513",
            FeriasGozoRuleSet.SLOT_BENEFICIOS_FINANCEIROS_ELEGIBILIDADE,
            (Supplier<FeriasGozoRuleContext>)
                () -> {
                  FeriasGozoRuleContext base = validFeriasGozoContext();
                  base.draft().setOrdemParcela(2);
                  base.draft().setDiasGozo(15);
                  base.draft().setDtini(LocalDate.of(2025, 8, 1));
                  base.draft().setDtprevfim(LocalDate.of(2025, 8, 15));
                  base.draft().setAdiantamento13(true);
                  base.draft().setAbonoConstitucional(false);
                  FeriasGozo p1 = validFeriasGozo();
                  p1.setId(801L);
                  p1.setOrdemParcela(1);
                  p1.setDiasGozo(15);
                  p1.setAdiantamento13(true);
                  return new FeriasGozoRuleContext(
                      base.draft(),
                      base.periodoAquisitivo(),
                      base.vinculo(),
                      null,
                      base.parametroCandidates(),
                      base.existingPeriodosDoVinculo(),
                      List.of(p1),
                      List.of(),
                      List.of());
                }),
        Arguments.of(
            "ERG-00521",
            FeriasGozoRuleSet.SLOT_BENEFICIOS_FINANCEIROS_ELEGIBILIDADE,
            (Supplier<FeriasGozoRuleContext>)
                () -> {
                  FeriasGozoRuleContext base = validFeriasGozoContext();
                  base.parametroCandidates().get(0).setPermiteLancConcPa(false);
                  base.draft().setOrdemParcela(2);
                  base.draft().setDiasGozo(15);
                  base.draft().setDtini(LocalDate.of(2025, 8, 1));
                  base.draft().setDtprevfim(LocalDate.of(2025, 8, 15));
                  base.draft().setAbonoConstitucional(false);
                  base.draft().setAbonoPecuniario(true);
                  base.draft().setIniFimAbono("F");
                  base.draft().setDiasAbonoVendidos(5);
                  FeriasGozo p1 = validFeriasGozo();
                  p1.setId(801L);
                  p1.setOrdemParcela(1);
                  p1.setDiasGozo(10);
                  p1.setAbonoPecuniario(true);
                  p1.setIniFimAbono("F");
                  p1.setDiasAbonoVendidos(5);
                  return new FeriasGozoRuleContext(
                      base.draft(),
                      base.periodoAquisitivo(),
                      base.vinculo(),
                      null,
                      base.parametroCandidates(),
                      base.existingPeriodosDoVinculo(),
                      List.of(p1),
                      List.of(),
                      List.of());
                }),
        Arguments.of(
            "ERG-00516",
            FeriasGozoRuleSet.SLOT_GOZO_ANTES_TERMINO_PA_POR_ORDEM,
            (Supplier<FeriasGozoRuleContext>)
                () -> {
                  FeriasGozoRuleContext base = validFeriasGozoContext();
                  base.parametroCandidates().get(0).setPermiteGozoAntesPaFer(false);
                  base.draft().setDtini(LocalDate.of(2023, 12, 1));
                  base.draft().setDtprevfim(LocalDate.of(2023, 12, 30));
                  return base;
                }),
        Arguments.of(
            "ERG-00527",
            FeriasGozoRuleSet.SLOT_GOZO_ANTES_TERMINO_PA_POR_ORDEM,
            (Supplier<FeriasGozoRuleContext>)
                () -> {
                  FeriasGozoRuleContext base = validFeriasGozoContext();
                  base.parametroCandidates().get(0).setPermiteGozoAntesPaFer(false);
                  base.parametroCandidates().get(0).setPermiteFerPrimPer(false);
                  base.periodoAquisitivo().setChave(1);
                  base.draft().setDtini(LocalDate.of(2024, 6, 1));
                  base.draft().setDtprevfim(LocalDate.of(2024, 6, 30));
                  return base;
                }),
        Arguments.of(
            "ERG-00528",
            FeriasGozoRuleSet.SLOT_GOZO_ANTES_TERMINO_PA_POR_ORDEM,
            (Supplier<FeriasGozoRuleContext>)
                () -> {
                  FeriasGozoRuleContext base = validFeriasGozoContext();
                  base.parametroCandidates().get(0).setPermiteGozoAntesPaFer(false);
                  base.parametroCandidates().get(0).setPermiteFerSegunPer(false);
                  base.periodoAquisitivo().setChave(2);
                  base.draft().setChavePeraqfer(2);
                  base.draft().setDtini(LocalDate.of(2024, 6, 1));
                  base.draft().setDtprevfim(LocalDate.of(2024, 6, 30));
                  return base;
                }),
        Arguments.of(
            "ERG-00530",
            FeriasGozoRuleSet.SLOT_GOZO_ANTES_TERMINO_PA_POR_ORDEM,
            (Supplier<FeriasGozoRuleContext>)
                () -> {
                  FeriasGozoRuleContext base = validFeriasGozoContext();
                  base.parametroCandidates().get(0).setPermiteGozoAntesPaFer(false);
                  base.parametroCandidates().get(0).setPermiteFerTercPer(false);
                  base.periodoAquisitivo().setChave(3);
                  base.draft().setChavePeraqfer(3);
                  base.draft().setDtini(LocalDate.of(2024, 6, 1));
                  base.draft().setDtprevfim(LocalDate.of(2024, 6, 30));
                  return base;
                }),
        Arguments.of(
            "ERG-00517",
            FeriasGozoRuleSet.SLOT_GOZO_ANTES_TERMINO_PA_POR_ORDEM,
            (Supplier<FeriasGozoRuleContext>)
                () -> {
                  FeriasGozoRuleContext base = validFeriasGozoContext();
                  base.parametroCandidates().get(0).setPermiteGozoAntesPaFer(false);
                  base.parametroCandidates().get(0).setPermiteFerPerInc(false);
                  base.periodoAquisitivo().setChave(4);
                  base.draft().setChavePeraqfer(4);
                  base.draft().setDtini(LocalDate.of(2024, 6, 1));
                  base.draft().setDtprevfim(LocalDate.of(2024, 6, 30));
                  return base;
                }),
        Arguments.of(
            "ERG-01880",
            FeriasGozoRuleSet.SLOT_GOZO_APOS_PRESCRICAO_PA,
            (Supplier<FeriasGozoRuleContext>)
                () -> {
                  FeriasGozoRuleContext base = validFeriasGozoContext();
                  base.periodoAquisitivo().setDtPrescricao(LocalDate.of(2025, 6, 30));
                  return base;
                }),
        Arguments.of(
            "ERG-00519",
            FeriasGozoRuleSet.SLOT_FRACIONAMENTO_LIMITES_TEMPLATE,
            (Supplier<FeriasGozoRuleContext>)
                () -> {
                  FeriasGozoRuleContext base = validFeriasGozoContext();
                  base.draft().setDiasGozo(5);
                  base.draft().setDtprevfim(LocalDate.of(2025, 7, 5));
                  return base;
                }),
        Arguments.of(
            "ERG-00522",
            FeriasGozoRuleSet.SLOT_ABONO_PECUNIARIO_ELEGIBILIDADE_E_LIMITES,
            (Supplier<FeriasGozoRuleContext>)
                () -> {
                  FeriasGozoRuleContext base = validFeriasGozoContext();
                  base.draft().setDiasGozo(20);
                  base.draft().setDtprevfim(LocalDate.of(2025, 7, 20));
                  base.draft().setAbonoPecuniario(true);
                  base.draft().setIniFimAbono("F");
                  // Fração 1/3 sobre 30 dias permite no máximo 10 dias de abono
                  base.draft().setDiasAbonoVendidos(15);
                  return base;
                }),
        Arguments.of(
            "ERG-00514",
            FeriasGozoRuleSet.SLOT_CONCOMITANCIA_E_SALDO_FRACIONAMENTO,
            (Supplier<FeriasGozoRuleContext>)
                () -> {
                  FeriasGozoRuleContext base = validFeriasGozoContext();
                  FeriasGozo concomitante = validFeriasGozo();
                  concomitante.setId(899L);
                  concomitante.setOrdemParcela(2);
                  concomitante.setAbonoConstitucional(false);
                  concomitante.setDtini(LocalDate.of(2025, 7, 10));
                  concomitante.setDtprevfim(LocalDate.of(2025, 7, 25));
                  return new FeriasGozoRuleContext(
                      base.draft(),
                      base.periodoAquisitivo(),
                      base.vinculo(),
                      null,
                      base.parametroCandidates(),
                      base.existingPeriodosDoVinculo(),
                      List.of(concomitante),
                      List.of(),
                      List.of());
                }),
        Arguments.of(
            "ERG-00518",
            FeriasGozoRuleSet.SLOT_CONCOMITANCIA_E_SALDO_FRACIONAMENTO,
            (Supplier<FeriasGozoRuleContext>)
                () -> {
                  FeriasGozoRuleContext base = validFeriasGozoContext();
                  base.parametroCandidates().get(0).setMaxDiasPerFer(20);
                  return base;
                }),
        Arguments.of(
            "ERG-00520",
            FeriasGozoRuleSet.SLOT_CONCOMITANCIA_E_SALDO_FRACIONAMENTO,
            (Supplier<FeriasGozoRuleContext>)
                () -> {
                  FeriasGozoRuleContext base = validFeriasGozoContext();
                  base.parametroCandidates().get(0).setMaxPerFer(2);
                  base.draft().setOrdemParcela(3);
                  return base;
                }),
        Arguments.of(
            "ERG-00523",
            FeriasGozoRuleSet.SLOT_CONCOMITANCIA_E_SALDO_FRACIONAMENTO,
            (Supplier<FeriasGozoRuleContext>)
                () -> {
                  FeriasGozoRuleContext base = validFeriasGozoContext();
                  base.draft().setAbonoPecuniario(true);
                  base.draft().setIniFimAbono("F");
                  base.draft().setDiasAbonoVendidos(10);
                  // 30 dias de gozo + 10 dias de abono = 40 > 30
                  return base;
                }),
        Arguments.of(
            "ERG-00531",
            FeriasGozoRuleSet.SLOT_CONCOMITANCIA_E_SALDO_FRACIONAMENTO,
            (Supplier<FeriasGozoRuleContext>)
                () -> {
                  FeriasGozoRuleContext base = validFeriasGozoContext();
                  // Exige no mínimo 2 parcelas, mas a 1ª parcela já consome os 30 dias
                  base.parametroCandidates().get(0).setMinPerFer(2);
                  return base;
                }),
        Arguments.of(
            "ERG-00532",
            FeriasGozoRuleSet.SLOT_CONCOMITANCIA_E_SALDO_FRACIONAMENTO,
            (Supplier<FeriasGozoRuleContext>)
                () -> {
                  FeriasGozoRuleContext base = validFeriasGozoContext();
                  base.parametroCandidates().get(0).setMaxPerFer(2);
                  base.draft().setOrdemParcela(2);
                  base.draft().setDiasGozo(10);
                  base.draft().setDtini(LocalDate.of(2025, 8, 1));
                  base.draft().setDtprevfim(LocalDate.of(2025, 8, 10));
                  base.draft().setAbonoConstitucional(false);
                  FeriasGozo p1 = validFeriasGozo();
                  p1.setId(801L);
                  p1.setOrdemParcela(1);
                  p1.setDiasGozo(15);
                  // 15 + 10 = 25 na última parcela (2 de 2), sobrando 5 dias sem poder criar 3ª parcela
                  return new FeriasGozoRuleContext(
                      base.draft(),
                      base.periodoAquisitivo(),
                      base.vinculo(),
                      null,
                      base.parametroCandidates(),
                      base.existingPeriodosDoVinculo(),
                      List.of(p1),
                      List.of(),
                      List.of());
                }),
        Arguments.of(
            "ERG-00525",
            FeriasGozoRuleSet.SLOT_CONCOMITANCIA_E_SALDO_FRACIONAMENTO,
            (Supplier<FeriasGozoRuleContext>)
                () -> {
                  FeriasGozoRuleContext base = validFeriasGozoContext();
                  base.parametroCandidates().get(0).setMaxPerFer(2);
                  base.parametroCandidates().get(0).setMaxDiasPerFer(15);
                  base.parametroCandidates().get(0).setMinDiasPerPrincFer(10);
                  base.draft().setOrdemParcela(1);
                  base.draft().setDiasGozo(10);
                  base.draft().setDtprevfim(LocalDate.of(2025, 7, 10));
                  // 1ª parcela de 10 dias deixa saldo de 20 dias, impossível quitar em 1 parcela restante com maxDiasPerFer=15
                  return base;
                }),
        Arguments.of(
            "ERG-00526",
            FeriasGozoRuleSet.SLOT_CONCOMITANCIA_E_SALDO_FRACIONAMENTO,
            (Supplier<FeriasGozoRuleContext>)
                () -> {
                  FeriasGozoRuleContext base = validFeriasGozoContext();
                  // 1ª parcela de 25 dias deixa saldo residual de 5 dias (< minDiasPerFer = 10)
                  base.draft().setOrdemParcela(1);
                  base.draft().setDiasGozo(25);
                  base.draft().setDtprevfim(LocalDate.of(2025, 7, 25));
                  return base;
                }));
  }

  @ParameterizedTest(name = "FeriasGozo [{index}] -> {0} no slot {1}")
  @MethodSource("feriasGozoViolations")
  void feriasGozoProductViolationsAreDeniedWithOfficialErgonCodes(
      String expectedCode,
      String expectedSlot,
      Supplier<FeriasGozoRuleContext> ctxSupplier) {
    RuleEvaluationResult result = facade.evaluateFeriasGozo(ctxSupplier.get());
    assertThat(result.decision()).isEqualTo(RuleDecision.DENY);
    assertThat(result.reasonCodes()).contains(expectedCode);
    assertThat(ErgonRuleErrorCatalog.find(expectedCode))
        .isPresent()
        .get()
        .satisfies(
            desc -> {
              assertThat(desc.targetField()).isNotBlank().doesNotContain("PCK_");
              assertThat(desc.messagePtBr())
                  .isNotBlank()
                  .isNotIn("PRODUCT_DECISION", "DB_INVARIANT", "CUSTOMER_EXTENSION_HOOK");
              assertThat(desc.plsqlOrigin()).contains("PCK_");
            });
    assertThat(result.bindingResults())
        .anySatisfy(
            b -> {
              assertThat(b.slotKey()).isEqualTo(expectedSlot);
              assertThat(b.decision()).isEqualTo(RuleDecision.DENY);
              assertThat(b.reasonCodes()).contains(expectedCode);
            });
  }

  @Test
  void effectiveVacationParameterResolverHonorsPrecedenceMergeTarefa24610() {
    EffectiveVacationParameterResolver resolver = facade.getVacationParameterResolver();
    Vinculo vinculo = validVinculo();

    // 1. Especificidade: regra do Órgão 1 + categoria GERAL vence regra global (empCodigo == null)
    ParametroContagemFerias globalParam = validParametro();
    globalParam.setId(1L);
    globalParam.setEmpCodigo(null);
    globalParam.setCategoria(null);
    globalParam.setSubcategoria(null);
    globalParam.setTotDiasFer(30);
    globalParam.setMaxPerFer(3);

    ParametroContagemFerias specificParam = validParametro();
    specificParam.setId(2L);
    specificParam.setEmpCodigo(1);
    specificParam.setCategoria("GERAL");
    specificParam.setSubcategoria("PADRAO");
    specificParam.setTotDiasFer(45);
    specificParam.setMaxPerFer(3);
    specificParam.setMinDiasPerFer(10);
    specificParam.setMinDiasPerPrincFer(15);
    specificParam.setAbonoDirGozo("D");
    specificParam.setNumDiasAbonoFer(1);
    specificParam.setDenDiasAbonoFer(3);
    specificParam.setAbonoConstFer(true);
    specificParam.setPermiteGozoAntesPaFer(false);
    specificParam.setPermiteFerPrimPer(false);
    specificParam.setPermiteFerSegunPer(true);
    specificParam.setContDobroFerias(false);
    specificParam.setSemPgtoMulta(true);

    Optional<ParametroContagemFerias> resolved =
        resolver.resolveActiveParameter(
            List.of(globalParam, specificParam), 1, vinculo, LocalDate.of(2025, 1, 1));
    assertThat(resolved).isPresent();
    assertThat(resolved.get().getId()).isEqualTo(2L);

    // 2. Quando PA tem todos os overrides em 'I' / null -> herda 100% de specificParam
    PeriodoAquisitivoFerias paInherit = validPa();
    EffectiveParametroFeriasFacts inherited =
        resolver.resolveEffectiveFacts(
            List.of(globalParam, specificParam),
            null,
            paInherit,
            vinculo,
            LocalDate.of(2025, 1, 1));
    assertThat(inherited.parametroContagemId()).isEqualTo(2L);
    assertThat(inherited.totDiasFer()).isEqualTo(45);
    assertThat(inherited.maxPerFer()).isEqualTo(3);
    assertThat(inherited.minDiasPerFer()).isEqualTo(10);
    assertThat(inherited.minDiasPerPrincFer()).isEqualTo(15);
    assertThat(inherited.abonoDirGozo()).isEqualTo("D");
    assertThat(inherited.abonoConstFer()).isTrue();
    assertThat(inherited.permiteGozoAntesPaFer()).isFalse();
    assertThat(inherited.permiteFerPrimPer()).isFalse();
    assertThat(inherited.permiteFerSegunPer()).isTrue();
    assertThat(inherited.contDobroFerias()).isFalse();
    assertThat(inherited.semPgtoMulta()).isTrue();
    assertThat(inherited.possuiOverridesIndividuais()).isFalse();

    // 3. Quando PA define overrides ('S'/'N' ou numérico) -> sobrescreve specificParam (Tarefa 24610)
    PeriodoAquisitivoFerias paOverride = validPa();
    paOverride.setMaxPerFer(2);
    paOverride.setMinDiasPerFer(12);
    paOverride.setMinDiasPerPrincFer(18);
    paOverride.setAbonoDirGozo("G");
    paOverride.setNumDiasAbonoFer(1);
    paOverride.setDenDiasAbonoFer(2);
    paOverride.setAbonoConstFer("N");
    paOverride.setPermiteFerEstePer("S");
    paOverride.setContDobroFerias("S");
    paOverride.setMinDiasPeraqfer(5);
    paOverride.setMaxDiasPeraqfer(15);

    EffectiveParametroFeriasFacts merged =
        resolver.resolveEffectiveFacts(
            List.of(globalParam, specificParam),
            null,
            paOverride,
            vinculo,
            LocalDate.of(2025, 1, 1));
    assertThat(merged.maxPerFer()).isEqualTo(2);
    assertThat(merged.minDiasPerFer()).isEqualTo(12);
    assertThat(merged.minDiasPerPrincFer()).isEqualTo(18);
    assertThat(merged.abonoDirGozo()).isEqualTo("G");
    assertThat(merged.numDiasAbonoFer()).isEqualTo(1);
    assertThat(merged.denDiasAbonoFer()).isEqualTo(2);
    assertThat(merged.abonoConstFer()).isFalse();
    assertThat(merged.permiteGozoAntesPaFer()).isTrue();
    assertThat(merged.permiteFerPrimPer()).isTrue();
    assertThat(merged.contDobroFerias()).isTrue();
    assertThat(merged.minDiasPeraqfer()).isEqualTo(5);
    assertThat(merged.maxDiasPeraqfer()).isEqualTo(15);
    assertThat(merged.possuiOverridesIndividuais()).isTrue();
  }

  @Test
  void mode1ForbiddenSlotsRejectCustomerOverridesAtCompileTimeAcrossAllThreeRuleSets() {
    assertThatThrownBy(
            () ->
                facade.compilePlan(
                    ParametroContagemFeriasRuleSet.customerMode1ForbiddenOverrideAttempt()))
        .isInstanceOf(RulePlanException.class)
        .satisfies(
            ex -> {
              RulePlanException planEx = (RulePlanException) ex;
              assertThat(planEx.getCode()).isEqualTo(RulePlanIssueCode.PLAN_OVERRIDE_INVALID);
              assertThat(planEx.getMessage())
                  .contains(ParametroContagemFeriasRuleSet.SLOT_VIGENCIA_ORDEM_VALIDA);
            });

    assertThatThrownBy(
            () ->
                facade.compilePlan(
                    PeriodoAquisitivoFeriasRuleSet.customerMode1ForbiddenOverrideAttempt()))
        .isInstanceOf(RulePlanException.class)
        .satisfies(
            ex -> {
              RulePlanException planEx = (RulePlanException) ex;
              assertThat(planEx.getCode()).isEqualTo(RulePlanIssueCode.PLAN_OVERRIDE_INVALID);
              assertThat(planEx.getMessage())
                  .contains(PeriodoAquisitivoFeriasRuleSet.SLOT_CRONOLOGIA_DATAS_VALIDA);
            });

    assertThatThrownBy(
            () ->
                facade.compilePlan(
                    FeriasGozoRuleSet.customerMode1ForbiddenOverrideAttempt()))
        .isInstanceOf(RulePlanException.class)
        .satisfies(
            ex -> {
              RulePlanException planEx = (RulePlanException) ex;
              assertThat(planEx.getCode()).isEqualTo(RulePlanIssueCode.PLAN_OVERRIDE_INVALID);
              assertThat(planEx.getMessage())
                  .contains(FeriasGozoRuleSet.SLOT_CONCOMITANCIA_E_SALDO_FRACIONAMENTO);
            });
  }

  @Test
  void mode2ParameterizableSlotsEnforceOrgaoSpecificThresholdsAndRejectInvalidBounds() {
    // 1. ParametroContagemFerias: maxPerFer = 3 é permitido no Órgão 1 (teto 6), mas negado no Órgão 2 (teto 2)
    ParametroContagemFerias param = validParametro();
    param.setMaxPerFer(3);
    param.setDocumentoLegalFeriasId(10L);
    param.setAbonoDirGozo("N");
    param.setNumDiasAbonoFer(null);
    param.setDenDiasAbonoFer(null);
    param.setMinDiasPvendFer(null);
    param.setMaxDiasVendFer(null);

    assertThat(
            facade
                .evaluateParametroContagemFerias(
                    new ParametroContagemFeriasRuleContext(param, List.of()))
                .decision())
        .isEqualTo(RuleDecision.ALLOW);

    param.setEmpCodigo(2);
    RuleEvaluationResult paramDeny =
        facade.evaluateParametroContagemFerias(
            new ParametroContagemFeriasRuleContext(param, List.of()));
    assertThat(paramDeny.decision()).isEqualTo(RuleDecision.DENY);
    assertThat(paramDeny.reasonCodes()).containsExactly("ERG-01806");

    // 2. PeriodoAquisitivoFerias: 2º PA em aberto é permitido no Órgão 1 (teto 2), mas negado no Órgão 2 (teto 1)
    PeriodoAquisitivoFerias open1 = validPa();
    open1.setId(701L);
    open1.setChave(1);
    open1.setDtini(LocalDate.of(2023, 1, 1));
    open1.setDtfim(LocalDate.of(2023, 12, 31));
    open1.setSituacao("DISPONIVEL");
    open1.setSaldoDiasDisponivel(30);

    PeriodoAquisitivoFeriasRuleContext basePa = validPaContext();
    basePa.draft().setChave(2);
    PeriodoAquisitivoFeriasRuleContext orgao1Pa =
        new PeriodoAquisitivoFeriasRuleContext(
            basePa.draft(),
            basePa.vinculo(),
            basePa.parametroCandidates(),
            List.of(open1),
            List.of());
    assertThat(facade.evaluatePeriodoAquisitivoFerias(orgao1Pa).decision())
        .isEqualTo(RuleDecision.ALLOW);

    basePa.draft().setEmpCodigo(2);
    basePa.vinculo().setEmpCodigo(2);
    open1.setEmpCodigo(2);
    RuleEvaluationResult paDeny = facade.evaluatePeriodoAquisitivoFerias(orgao1Pa);
    assertThat(paDeny.decision()).isEqualTo(RuleDecision.DENY);
    assertThat(paDeny.reasonCodes()).containsExactly("ERG-00612");

    // 3. FeriasGozo: antecedência de venda de abono de 5 dias é permitida no Órgão 1 (mín 0), mas negada no Órgão 2 (mín 15)
    FeriasGozoRuleContext baseFg = validFeriasGozoContext();
    baseFg.draft().setDiasGozo(20);
    baseFg.draft().setDtprevfim(LocalDate.of(2025, 7, 20));
    baseFg.draft().setAbonoPecuniario(true);
    baseFg.draft().setIniFimAbono("F");
    baseFg.draft().setDiasAbonoVendidos(10);
    baseFg.draft().setDtVendaAbono(LocalDate.of(2025, 6, 26)); // 5 dias antes de 2025-07-01
    baseFg.draft().setDocumentoLegalId(10L);

    assertThat(facade.evaluateFeriasGozo(baseFg).decision()).isEqualTo(RuleDecision.ALLOW);

    baseFg.draft().setEmpCodigo(2);
    baseFg.vinculo().setEmpCodigo(2);
    baseFg.periodoAquisitivo().setEmpCodigo(2);
    baseFg.parametroCandidates().get(0).setEmpCodigo(2);
    RuleEvaluationResult fgDeny = facade.evaluateFeriasGozo(baseFg);
    assertThat(fgDeny.decision()).isEqualTo(RuleDecision.DENY);
    assertThat(fgDeny.reasonCodes()).containsExactly("ERG-00519");

    // 4. Parâmetros fora dos limites do RuleParameterTemplate são rejeitados pelo compilador
    assertThatThrownBy(
            () ->
                facade.compilePlan(
                    ParametroContagemFeriasRuleSet.withCustomerBindings(
                        List.of(
                            ParametroContagemFeriasRuleSet.customerMode2ParameterizeBinding(
                                10, 999, 2)))))
        .isInstanceOf(RulePlanException.class)
        .satisfies(
            ex ->
                assertThat(((RulePlanException) ex).getCode())
                    .isEqualTo(RulePlanIssueCode.PLAN_PARAMETER_VALUES_INVALID));

    assertThatThrownBy(
            () ->
                facade.compilePlan(
                    PeriodoAquisitivoFeriasRuleSet.withCustomerBindings(
                        List.of(
                            PeriodoAquisitivoFeriasRuleSet.customerMode2ParameterizeBinding(
                                99, 20)))))
        .isInstanceOf(RulePlanException.class)
        .satisfies(
            ex ->
                assertThat(((RulePlanException) ex).getCode())
                    .isEqualTo(RulePlanIssueCode.PLAN_PARAMETER_VALUES_INVALID));

    assertThatThrownBy(
            () ->
                facade.compilePlan(
                    FeriasGozoRuleSet.withCustomerBindings(
                        List.of(
                            FeriasGozoRuleSet.customerMode2ParameterizeBinding(99, 10, 10, 15)))))
        .isInstanceOf(RulePlanException.class)
        .satisfies(
            ex ->
                assertThat(((RulePlanException) ex).getCode())
                    .isEqualTo(RulePlanIssueCode.PLAN_PARAMETER_VALUES_INVALID));
  }

  @Test
  void mode3RestrictOnlyWithAugmentAndMode4ReplaceExactCoexistWithProductSlotsAcrossOrgaos() {
    // 1. ParametroContagemFerias no Órgão 2:
    // - Mode 3 (AUGMENT -> CERG-PARAM-301): exige documentoLegalFeriasId != null
    // - Mode 4 (REPLACE_EXACT -> CERG-PARAM-401): exige abonoDirGozo == 'N'
    ParametroContagemFerias paramOrgao2 = validParametro();
    paramOrgao2.setEmpCodigo(2);
    paramOrgao2.setMaxPerFer(2);
    paramOrgao2.setDocumentoLegalFeriasId(null);
    paramOrgao2.setAbonoDirGozo("N");
    paramOrgao2.setNumDiasAbonoFer(null);
    paramOrgao2.setDenDiasAbonoFer(null);
    paramOrgao2.setMinDiasPvendFer(null);
    paramOrgao2.setMaxDiasVendFer(null);

    RuleEvaluationResult paramAugmentDeny =
        facade.evaluateParametroContagemFerias(
            new ParametroContagemFeriasRuleContext(paramOrgao2, List.of()));
    assertThat(paramAugmentDeny.decision()).isEqualTo(RuleDecision.DENY);
    assertThat(paramAugmentDeny.reasonCodes()).containsExactly("CERG-PARAM-301");

    paramOrgao2.setDocumentoLegalFeriasId(10L);
    paramOrgao2.setAbonoDirGozo("D");
    paramOrgao2.setNumDiasAbonoFer(1);
    paramOrgao2.setDenDiasAbonoFer(3);
    RuleEvaluationResult paramReplaceDeny =
        facade.evaluateParametroContagemFerias(
            new ParametroContagemFeriasRuleContext(paramOrgao2, List.of()));
    assertThat(paramReplaceDeny.decision()).isEqualTo(RuleDecision.DENY);
    assertThat(paramReplaceDeny.reasonCodes()).containsExactly("CERG-PARAM-401");

    // 2. PeriodoAquisitivoFerias no Órgão 2:
    // - Mode 3 (AUGMENT -> CERG-PA-301): exige documentoLegalId != null quando diasConvertidosDobro > 0
    // - Mode 4 (REPLACE_EXACT -> CERG-PA-401): exige documentoLegalId != null para autorizar gozo após dtPrescricao
    PeriodoAquisitivoFeriasRuleContext paOrgao2 = validPaContext();
    paOrgao2.draft().setEmpCodigo(2);
    paOrgao2.vinculo().setEmpCodigo(2);
    paOrgao2.parametroCandidates().get(0).setEmpCodigo(2);
    paOrgao2.draft().setDiasConvertidosDobro(10);
    paOrgao2.draft().setSaldoDiasDisponivel(20);
    paOrgao2.draft().setDocumentoLegalId(null);

    RuleEvaluationResult paAugmentDeny = facade.evaluatePeriodoAquisitivoFerias(paOrgao2);
    assertThat(paAugmentDeny.decision()).isEqualTo(RuleDecision.DENY);
    assertThat(paAugmentDeny.reasonCodes()).containsExactly("CERG-PA-301");

    paOrgao2.draft().setDiasConvertidosDobro(0);
    paOrgao2.draft().setSaldoDiasDisponivel(30);
    paOrgao2.draft().setDtPrescricao(LocalDate.of(2025, 6, 30));
    paOrgao2.parametroCandidates().get(0).setPermiteGozoAposPrescricao(true);
    FeriasGozo parcelaPosPrescricao = validFeriasGozo();
    parcelaPosPrescricao.setDtini(LocalDate.of(2025, 7, 10));
    parcelaPosPrescricao.setDtprevfim(LocalDate.of(2025, 7, 24));
    PeriodoAquisitivoFeriasRuleContext paReplaceCtx =
        new PeriodoAquisitivoFeriasRuleContext(
            paOrgao2.draft(),
            paOrgao2.vinculo(),
            paOrgao2.parametroCandidates(),
            List.of(),
            List.of(parcelaPosPrescricao));
    RuleEvaluationResult paReplaceDeny = facade.evaluatePeriodoAquisitivoFerias(paReplaceCtx);
    assertThat(paReplaceDeny.decision()).isEqualTo(RuleDecision.DENY);
    assertThat(paReplaceDeny.reasonCodes()).containsExactly("CERG-PA-401");

    // 3. FeriasGozo no Órgão 2:
    // - Mode 3 (AUGMENT -> CERG-FER-301): exige documentoLegalId != null quando abonoPecuniario ou adiantamento13 == true
    // - Mode 4 (REPLACE_EXACT -> CERG-FER-401): abono pecuniário permitido somente na 1ª parcela e com iniFimAbono == 'F'
    FeriasGozoRuleContext fgOrgao2 = validFeriasGozoContext();
    fgOrgao2.draft().setEmpCodigo(2);
    fgOrgao2.vinculo().setEmpCodigo(2);
    fgOrgao2.periodoAquisitivo().setEmpCodigo(2);
    fgOrgao2.parametroCandidates().get(0).setEmpCodigo(2);
    fgOrgao2.draft().setAdiantamento13(true);
    fgOrgao2.draft().setDocumentoLegalId(null);

    RuleEvaluationResult fgAugmentDeny = facade.evaluateFeriasGozo(fgOrgao2);
    assertThat(fgAugmentDeny.decision()).isEqualTo(RuleDecision.DENY);
    assertThat(fgAugmentDeny.reasonCodes()).containsExactly("CERG-FER-301");

    fgOrgao2.draft().setDocumentoLegalId(10L);
    fgOrgao2.draft().setDiasGozo(20);
    fgOrgao2.draft().setDtprevfim(LocalDate.of(2025, 7, 20));
    fgOrgao2.draft().setAbonoPecuniario(true);
    fgOrgao2.draft().setIniFimAbono("I"); // Órgão 1 aceita 'I', mas Órgão 2 exige 'F'
    fgOrgao2.draft().setDiasAbonoVendidos(10);
    fgOrgao2.draft().setDtVendaAbono(LocalDate.of(2025, 6, 1));

    RuleEvaluationResult fgReplaceDeny = facade.evaluateFeriasGozo(fgOrgao2);
    assertThat(fgReplaceDeny.decision()).isEqualTo(RuleDecision.DENY);
    assertThat(fgReplaceDeny.reasonCodes()).containsExactly("CERG-FER-401");
  }

  @Test
  @SuppressWarnings("unchecked")
  void servicesAndReactiveDeterminationsEnforceFailClosed422AndReflectAbsencesSuspensionsAndMergedParameters() {
    EntityManager em = mock(EntityManager.class);
    TenantContext tenant = new TenantContext();
    ResourceVersionEtagService etags = new ResourceVersionEtagService("slice5-phase3-secret");
    ResourceVersionScope scope = new ResourceVersionScope("1");

    Vinculo vinculo = validVinculo();
    when(em.find(eq(Vinculo.class), eq(new VinculoId(1001L, 1)))).thenReturn(vinculo);
    when(em.find(eq(Vinculo.class), eq(new VinculoId(1001L, 1)), any(LockModeType.class)))
        .thenReturn(vinculo);

    ParametroContagemFerias param = validParametro();
    TypedQuery<ParametroContagemFerias> paramQuery = mock(TypedQuery.class);
    when(paramQuery.setParameter(anyString(), any())).thenReturn(paramQuery);
    when(paramQuery.setFlushMode(any(FlushModeType.class))).thenReturn(paramQuery);
    when(paramQuery.getResultList()).thenReturn(List.of(param));
    when(em.createQuery(anyString(), eq(ParametroContagemFerias.class))).thenReturn(paramQuery);

    // 1. ParametroContagemFeriasService falha com HTTP 422 ErgonRuleViolationException (ERG-01850) em concomitância
    ParametroContagemFeriasRepository paramRepo = mock(ParametroContagemFeriasRepository.class);
    ParametroContagemFeriasService paramService =
        new ParametroContagemFeriasService(
            paramRepo, new ParametroContagemFeriasMapper(tenant), tenant, facade);
    ReflectionTestUtils.setField(paramService, "entityManager", em);

    ParametroContagemFerias conflictingParam = validParametro();
    conflictingParam.setId(null);
    conflictingParam.setDtini(LocalDate.of(2024, 6, 1));
    assertThatThrownBy(() -> paramService.validateParametroContagemFerias(conflictingParam))
        .isInstanceOf(ErgonRuleViolationException.class)
        .satisfies(
            ex -> {
              ErgonRuleViolationException ruleEx = (ErgonRuleViolationException) ex;
              assertThat(ruleEx.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
              assertThat(ruleEx.getReasonCodes()).containsExactly("ERG-01850");
              assertThat(ruleEx.getDecisionSources()).containsExactly(DecisionSource.PRODUCT);
            });

    // 2. PeriodoAquisitivoFeriasService recalcula faltas (Frequencia -> 16 faltas -> 18 dias de direito) e suspensão (Afastamento -> 12 dias)
    PeriodoAquisitivoFeriasRepository paRepo = mock(PeriodoAquisitivoFeriasRepository.class);
    PeriodoAquisitivoFeriasService paService =
        new PeriodoAquisitivoFeriasService(
            paRepo, new PeriodoAquisitivoFeriasMapper(tenant), tenant, facade);
    ReflectionTestUtils.setField(paService, "entityManager", em);
    ReflectionTestUtils.setField(paService, "readFacts", mock(ErgonReadFacts.class));

    PeriodoAquisitivoFerias pa = validPa();
    pa.setId(700L);
    pa.setVersion(1L);
    when(paRepo.findById(700L)).thenReturn(Optional.of(pa));
    when(paRepo.save(any(PeriodoAquisitivoFerias.class))).thenAnswer(inv -> inv.getArgument(0));
    when(em.find(eq(PeriodoAquisitivoFerias.class), eq(700L))).thenReturn(pa);
    when(em.find(eq(PeriodoAquisitivoFerias.class), eq(700L), any(LockModeType.class)))
        .thenReturn(pa);

    CodigoFrequencia codFalta = new CodigoFrequencia();
    codFalta.setId(new CodigoFrequenciaId("FALTA", 101));
    codFalta.setQtdFaltas(BigDecimal.ONE);

    Frequencia falta = new Frequencia();
    falta.setId(501L);
    falta.setNumfunc(1001L);
    falta.setNumvinc(1);
    falta.setTipofreq("FALTA");
    falta.setCodfreq(101);
    falta.setCodigoFrequenciaEntidade(codFalta);
    falta.setDtini(LocalDate.of(2024, 3, 1));
    falta.setDtfim(LocalDate.of(2024, 3, 16));
    falta.setQuantidade(BigDecimal.valueOf(16)); // 16 faltas -> faixa 15..23 -> 18 dias de direito
    falta.setSituacao("HOMOLOGADO");

    TypedQuery<Frequencia> freqQuery = mock(TypedQuery.class);
    when(freqQuery.setParameter(anyString(), any())).thenReturn(freqQuery);
    when(freqQuery.setFlushMode(any(FlushModeType.class))).thenReturn(freqQuery);
    when(freqQuery.getResultList()).thenReturn(List.of(falta));
    when(em.createQuery(anyString(), eq(Frequencia.class))).thenReturn(freqQuery);

    Afastamento afastSusp = new Afastamento();
    afastSusp.setId(601L);
    afastSusp.setNumfunc(1001L);
    afastSusp.setNumvinc(1);
    afastSusp.setDtini(LocalDate.of(2024, 5, 1));
    afastSusp.setDtfim(LocalDate.of(2024, 5, 12)); // 12 dias suspensivos
    afastSusp.setSituacao("HOMOLOGADO");

    TypedQuery<Afastamento> afastQuery = mock(TypedQuery.class);
    when(afastQuery.setParameter(anyString(), any())).thenReturn(afastQuery);
    when(afastQuery.setFlushMode(any(FlushModeType.class))).thenReturn(afastQuery);
    when(afastQuery.getResultList()).thenReturn(List.of(afastSusp));
    when(em.createQuery(anyString(), eq(Afastamento.class))).thenReturn(afastQuery);

    TypedQuery<PeriodoAquisitivoFerias> paQuery = mock(TypedQuery.class);
    when(paQuery.setParameter(anyString(), any())).thenReturn(paQuery);
    when(paQuery.setFlushMode(any(FlushModeType.class))).thenReturn(paQuery);
    when(paQuery.getResultList()).thenReturn(List.of(pa));
    when(em.createQuery(anyString(), eq(PeriodoAquisitivoFerias.class))).thenReturn(paQuery);

    TypedQuery<FeriasGozo> fgQuery = mock(TypedQuery.class);
    when(fgQuery.setParameter(anyString(), any())).thenReturn(fgQuery);
    when(fgQuery.setFlushMode(any(FlushModeType.class))).thenReturn(fgQuery);
    when(fgQuery.getResultList()).thenReturn(List.of());
    when(em.createQuery(anyString(), eq(FeriasGozo.class))).thenReturn(fgQuery);

    String paEtag =
        etags.create(scope, "administracao-pessoal.periodos-aquisitivos-ferias", 700L, 1L);
    var recalcResult =
        paService.recalcularFaltasDireitoPeriodo(
            700L,
            new TransicaoPeriodoAquisitivoFeriasDTO(),
            new ResourceVersionUpdatePrecondition<>(
                etags, paEtag, scope, "administracao-pessoal.periodos-aquisitivos-ferias", 700L));
    assertThat(recalcResult.body().getFaltasApuradas()).isEqualTo(16);
    assertThat(recalcResult.body().getDiasAfastamentoSuspensivo()).isEqualTo(12);
    assertThat(recalcResult.body().getTotDiasDireito()).isEqualTo(18);
    assertThat(recalcResult.body().getSaldoDiasDisponivel()).isEqualTo(18);

    // 3. FeriasGozoService falha com HTTP 422 ErgonRuleViolationException (ERG-00523) ao exceder os 18 dias disponíveis no PA
    FeriasGozoRepository fgRepo = mock(FeriasGozoRepository.class);
    FeriasGozoService fgService =
        new FeriasGozoService(fgRepo, new FeriasGozoMapper(tenant), tenant, facade);
    ReflectionTestUtils.setField(fgService, "entityManager", em);

    FeriasGozo fgExcesso = validFeriasGozo();
    fgExcesso.setDiasGozo(20); // 20 > 18 dias de direito após faltas
    fgExcesso.setDtprevfim(LocalDate.of(2025, 7, 20));
    assertThatThrownBy(() -> fgService.validateFeriasGozo(fgExcesso))
        .isInstanceOf(ErgonRuleViolationException.class)
        .satisfies(
            ex -> {
              ErgonRuleViolationException ruleEx = (ErgonRuleViolationException) ex;
              assertThat(ruleEx.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
              assertThat(ruleEx.getReasonCodes()).containsExactly("ERG-00523");
            });

    // 4. Determinações Reativas enriquecidas (ErgonDeterminationService) com merge PARAMETROS_CONTA (+) PERAQFER
    ErgonDeterminationService determinationService = new ErgonDeterminationService(tenant, null);
    ReflectionTestUtils.setField(determinationService, "em", em);

    PeriodoAquisitivoPreviaDraftDTO paDraft = new PeriodoAquisitivoPreviaDraftDTO();
    paDraft.setEmpCodigo(1);
    paDraft.setNumfunc(1001L);
    paDraft.setNumvinc(1);
    paDraft.setDtini(LocalDate.of(2024, 1, 1));
    paDraft.setFaltasApuradas(8); // 6..14 faltas -> reduz de 30 para 24 dias
    paDraft.setDiasAfastamentoSuspensivo(10); // prorroga dtfim em 10 dias
    var paPreview = determinationService.previaPeriodoAquisitivo(paDraft);
    assertThat(paPreview.getTotDiasDireito()).isEqualTo(24);
    assertThat(paPreview.getDtfim()).isEqualTo(LocalDate.of(2025, 1, 10));
    assertThat(paPreview.getSaldoDiasDisponivel()).isEqualTo(24);
    assertThat( paPreview.getDtPrescricao()).isEqualTo(LocalDate.of(2027, 1, 10));

    // Quando faltasApuradas e diasAfastamentoSuspensivo não são informados no rascunho, apura automaticamente de Frequencia e Afastamento
    PeriodoAquisitivoPreviaDraftDTO paAutoDraft = new PeriodoAquisitivoPreviaDraftDTO();
    paAutoDraft.setEmpCodigo(1);
    paAutoDraft.setNumfunc(1001L);
    paAutoDraft.setNumvinc(1);
    paAutoDraft.setDtini(LocalDate.of(2024, 1, 1));
    var paAutoPreview = determinationService.previaPeriodoAquisitivo(paAutoDraft);
    assertThat(paAutoPreview.getFaltasApuradas()).isEqualTo(16);
    assertThat(paAutoPreview.getDiasAfastamentoSuspensivo()).isEqualTo(12);
    assertThat(paAutoPreview.getTotDiasDireito()).isEqualTo(18);
    assertThat(paAutoPreview.getDtfim()).isEqualTo(LocalDate.of(2025, 1, 12));
    assertThat(paAutoPreview.getSaldoDiasDisponivel()).isEqualTo(18);

    // Override no PA mudando fração de abono para 1/2 sobre os 18 dias de direito -> sugere 9 dias de abono
    pa.setNumDiasAbonoFer(1);
    pa.setDenDiasAbonoFer(2);
    FeriasGozoPreviaDraftDTO fgDraft = new FeriasGozoPreviaDraftDTO();
    fgDraft.setPeriodoAquisitivoId(700L);
    fgDraft.setDtini(LocalDate.of(2025, 7, 1));
    fgDraft.setDiasGozo(9);
    fgDraft.setAbonoPecuniario(true);
    var fgPreview = determinationService.previaFeriasGozo(fgDraft);
    assertThat(fgPreview.getChavePeraqfer()).isEqualTo(1);
    assertThat(fgPreview.getTotDiasDireitoPa()).isEqualTo(18);
    assertThat(fgPreview.getOrdemParcela()).isEqualTo(1);
    assertThat(fgPreview.getDiasGozo()).isEqualTo(9);
    assertThat(fgPreview.getDtprevfim()).isEqualTo(LocalDate.of(2025, 7, 9));
    assertThat(fgPreview.getDiasAbonoVendidos()).isEqualTo(9);
    assertThat(fgPreview.getDtPagtoAbono()).isEqualTo(LocalDate.of(2025, 6, 29));
    assertThat(fgPreview.getSaldoPaAposParcela()).isZero();
  }

  @Test
  void granularReasonCodesAreEmittedIndividuallyAcrossAllThreeVacationRuleSets() {
    Vinculo vinculo = validVinculo();
    ParametroContagemFerias param = validParametro();

    // 1. ParametroContagemFeriasRuleSet granular codes ERG-01814..ERG-01820
    parametroContagemFeriasProductViolationsAreDeniedWithOfficialErgonCodes(
        "ERG-01814",
        ParametroContagemFeriasRuleSet.SLOT_LICENCA_ESPECIAL_PROGRESSAO_COERENCIA,
        () -> {
          var p = validParametro();
          p.setTemLicEsp(true);
          p.setAnosLicEsp(5);
          p.setMesesLicEspPer(3);
          p.setVendLicEsp(true);
          p.setAbonoDirGozoLicEsp(null);
          return new ParametroContagemFeriasRuleContext(p, List.of());
        });

    parametroContagemFeriasProductViolationsAreDeniedWithOfficialErgonCodes(
        "ERG-01815",
        ParametroContagemFeriasRuleSet.SLOT_LICENCA_ESPECIAL_PROGRESSAO_COERENCIA,
        () -> {
          var p = validParametro();
          p.setTemLicEsp(true);
          p.setAnosLicEsp(5);
          p.setMesesLicEspPer(3);
          p.setVendLicEsp(false);
          p.setMinDiasPvendLicEsp(10);
          return new ParametroContagemFeriasRuleContext(p, List.of());
        });

    parametroContagemFeriasProductViolationsAreDeniedWithOfficialErgonCodes(
        "ERG-01816",
        ParametroContagemFeriasRuleSet.SLOT_LICENCA_ESPECIAL_PROGRESSAO_COERENCIA,
        () -> {
          var p = validParametro();
          p.setTemLicEsp(true);
          p.setAnosLicEsp(5);
          p.setMesesLicEspPer(3);
          p.setContDobroLicEsp(true);
          p.setMinDiasPcontDobLicEsp(30);
          p.setMaxDiasContDobLicEsp(15);
          return new ParametroContagemFeriasRuleContext(p, List.of());
        });

    parametroContagemFeriasProductViolationsAreDeniedWithOfficialErgonCodes(
        "ERG-01817",
        ParametroContagemFeriasRuleSet.SLOT_LICENCA_ESPECIAL_PROGRESSAO_COERENCIA,
        () -> {
          var p = validParametro();
          p.setTemLicEsp(false);
          p.setAnosLicEsp(5);
          return new ParametroContagemFeriasRuleContext(p, List.of());
        });

    parametroContagemFeriasProductViolationsAreDeniedWithOfficialErgonCodes(
        "ERG-01818",
        ParametroContagemFeriasRuleSet.SLOT_LICENCA_ESPECIAL_PROGRESSAO_COERENCIA,
        () -> {
          var p = validParametro();
          p.setTemProgressao(true);
          p.setAnosProgressao(null);
          return new ParametroContagemFeriasRuleContext(p, List.of());
        });

    parametroContagemFeriasProductViolationsAreDeniedWithOfficialErgonCodes(
        "ERG-01819",
        ParametroContagemFeriasRuleSet.SLOT_LICENCA_ESPECIAL_PROGRESSAO_COERENCIA,
        () -> {
          var p = validParametro();
          p.setTemTempoServ(true);
          p.setAnos1TempoServ(null);
          return new ParametroContagemFeriasRuleContext(p, List.of());
        });

    parametroContagemFeriasProductViolationsAreDeniedWithOfficialErgonCodes(
        "ERG-01820",
        ParametroContagemFeriasRuleSet.SLOT_LICENCA_ESPECIAL_PROGRESSAO_COERENCIA,
        () -> {
          var p = validParametro();
          p.setTemSextaParte(true);
          p.setAnosSextaParte(null);
          return new ParametroContagemFeriasRuleContext(p, List.of());
        });

    // 2. PeriodoAquisitivoFeriasRuleSet granular codes ERG-00605, ERG-00606, ERG-01810, ERG-01802
    periodoAquisitivoFeriasProductViolationsAreDeniedWithOfficialErgonCodes(
        "ERG-00605",
        PeriodoAquisitivoFeriasRuleSet.SLOT_OVERRIDES_CONTAGEM_DOBRO,
        () -> {
          var pa = validPa();
          pa.setMinDiasPeraqfer(-5);
          return new PeriodoAquisitivoFeriasRuleContext(
              pa, vinculo, List.of(param), List.of(), List.of());
        });

    periodoAquisitivoFeriasProductViolationsAreDeniedWithOfficialErgonCodes(
        "ERG-00606",
        PeriodoAquisitivoFeriasRuleSet.SLOT_OVERRIDES_CONTAGEM_DOBRO,
        () -> {
          var pa = validPa();
          pa.setMinDiasPeraqfer(20);
          pa.setMaxDiasPeraqfer(10);
          return new PeriodoAquisitivoFeriasRuleContext(
              pa, vinculo, List.of(param), List.of(), List.of());
        });

    periodoAquisitivoFeriasProductViolationsAreDeniedWithOfficialErgonCodes(
        "ERG-01810",
        PeriodoAquisitivoFeriasRuleSet.SLOT_OVERRIDES_PARCELAMENTO_E_ABONO,
        () -> {
          var pa = validPa();
          pa.setMinDiasPerPrincFer(35);
          pa.setMaxDiasPerFer(30);
          return new PeriodoAquisitivoFeriasRuleContext(
              pa, vinculo, List.of(param), List.of(), List.of());
        });

    periodoAquisitivoFeriasProductViolationsAreDeniedWithOfficialErgonCodes(
        "ERG-01802",
        PeriodoAquisitivoFeriasRuleSet.SLOT_OVERRIDES_PARCELAMENTO_E_ABONO,
        () -> {
          var pa = validPa();
          pa.setNumDiasAbonoFer(5);
          pa.setDenDiasAbonoFer(3);
          return new PeriodoAquisitivoFeriasRuleContext(
              pa, vinculo, List.of(param), List.of(), List.of());
        });

    // 3. FeriasGozoRuleSet granular codes ERG-03311..03315
    feriasGozoProductViolationsAreDeniedWithOfficialErgonCodes(
        "ERG-03311",
        FeriasGozoRuleSet.SLOT_ABONO_PECUNIARIO_ELEGIBILIDADE_E_LIMITES,
        () -> {
          var pa = validPa();
          var fg = validFeriasGozo();
          fg.setAbonoPecuniario(true);
          fg.setDiasAbonoVendidos(10);
          fg.setIniFimAbono(null);
          return new FeriasGozoRuleContext(
              fg, pa, vinculo, null, List.of(param), List.of(pa), List.of(), List.of(), List.of());
        });

    feriasGozoProductViolationsAreDeniedWithOfficialErgonCodes(
        "ERG-03312",
        FeriasGozoRuleSet.SLOT_ABONO_PECUNIARIO_ELEGIBILIDADE_E_LIMITES,
        () -> {
          var pa = validPa();
          var fg = validFeriasGozo();
          fg.setAbonoPecuniario(true);
          fg.setDiasAbonoVendidos(3);
          fg.setIniFimAbono("I");
          return new FeriasGozoRuleContext(
              fg, pa, vinculo, null, List.of(param), List.of(pa), List.of(), List.of(), List.of());
        });

    feriasGozoProductViolationsAreDeniedWithOfficialErgonCodes(
        "ERG-03313",
        FeriasGozoRuleSet.SLOT_ABONO_PECUNIARIO_ELEGIBILIDADE_E_LIMITES,
        () -> {
          var pa = validPa();
          var pMax8 = validParametro();
          pMax8.setMaxDiasVendFer(8);
          var fg = validFeriasGozo();
          fg.setAbonoPecuniario(true);
          fg.setDiasAbonoVendidos(9);
          fg.setIniFimAbono("I");
          return new FeriasGozoRuleContext(
              fg, pa, vinculo, null, List.of(pMax8), List.of(pa), List.of(), List.of(), List.of());
        });

    feriasGozoProductViolationsAreDeniedWithOfficialErgonCodes(
        "ERG-03314",
        FeriasGozoRuleSet.SLOT_ABONO_PECUNIARIO_ELEGIBILIDADE_E_LIMITES,
        () -> {
          var pa = validPa();
          var pStrict = validParametro();
          pStrict.setMaxDiasVendFer(20);
          pStrict.setNumDiasAbonoFer(1);
          pStrict.setDenDiasAbonoFer(3);
          var fg = validFeriasGozo();
          fg.setAbonoPecuniario(true);
          fg.setDiasAbonoVendidos(12);
          fg.setIniFimAbono("I");
          return new FeriasGozoRuleContext(
              fg, pa, vinculo, null, List.of(pStrict), List.of(pa), List.of(), List.of(), List.of());
        });

    feriasGozoProductViolationsAreDeniedWithOfficialErgonCodes(
        "ERG-03315",
        FeriasGozoRuleSet.SLOT_ABONO_PECUNIARIO_ELEGIBILIDADE_E_LIMITES,
        () -> {
          var pa = validPa();
          var fg = validFeriasGozo();
          fg.setAbonoPecuniario(true);
          fg.setDiasAbonoVendidos(10);
          fg.setIniFimAbono("I");
          fg.setDtVendaAbono(LocalDate.of(2025, 6, 15));
          fg.setDtPagtoAbono(LocalDate.of(2025, 6, 10));
          return new FeriasGozoRuleContext(
              fg, pa, vinculo, null, List.of(param), List.of(pa), List.of(), List.of(), List.of());
        });
  }

  private static ParametroContagemFeriasRuleContext validParametroContext() {
    return new ParametroContagemFeriasRuleContext(validParametro(), List.of());
  }

  private static PeriodoAquisitivoFeriasRuleContext validPaContext() {
    return new PeriodoAquisitivoFeriasRuleContext(
        validPa(), validVinculo(), List.of(validParametro()), List.of(), List.of());
  }

  private static FeriasGozoRuleContext validFeriasGozoContext() {
    PeriodoAquisitivoFerias pa = validPa();
    return new FeriasGozoRuleContext(
        validFeriasGozo(),
        pa,
        validVinculo(),
        null,
        List.of(validParametro()),
        List.of(pa),
        List.of(),
        List.of(),
        List.of());
  }

  private static Vinculo validVinculo() {
    Vinculo v = new Vinculo();
    v.setId(new VinculoId(1001L, 1));
    v.setEmpCodigo(1);
    v.setMatric("MAT-1001");
    v.setRegimejur("ESTATUTARIO");
    v.setTipovinc("EFETIVO");
    v.setCategoria("GERAL");
    v.setSubcategoria("PADRAO");
    v.setDtexerc(LocalDate.of(2020, 1, 1));
    v.setSituacao("ATIVO");
    return v;
  }

  private static ParametroContagemFerias validParametro() {
    ParametroContagemFerias p = new ParametroContagemFerias();
    p.setId(100L);
    p.setEmpCodigo(1);
    p.setRegimeJuridico("ESTATUTARIO");
    p.setTipoVinculo("EFETIVO");
    p.setCategoria("GERAL");
    p.setSubcategoria("PADRAO");
    p.setDtini(LocalDate.of(2020, 1, 1));
    p.setDtfim(null);
    p.setTemFerias(true);
    p.setTotDiasFer(30);
    p.setMinPerFer(1);
    p.setMaxPerFer(3);
    p.setMinDiasPerFer(10);
    p.setMaxDiasPerFer(30);
    p.setMinDiasPerPrincFer(14);
    p.setAbonoDirGozo("D");
    p.setNumDiasAbonoFer(1);
    p.setDenDiasAbonoFer(3);
    p.setMinDiasPvendFer(5);
    p.setMaxDiasVendFer(10);
    p.setAbonoConstFer(true);
    p.setAdiant13Fer(true);
    p.setAdiantSalFer(true);
    p.setEmprestFer(true);
    p.setPermiteGozoAntesPaFer(true);
    p.setPermiteFerPrimPer(true);
    p.setPermiteFerSegunPer(true);
    p.setPermiteFerTercPer(true);
    p.setPermiteFerPerInc(true);
    p.setCriterioPrescricao("F");
    p.setPermiteGozoAposPrescricao(false);
    p.setPermiteLancConcPa(true);
    p.setExigeExercicio(true);
    p.setSemPgtoMulta(false);
    p.setContDobroFerias(true);
    p.setMinDiasPeraqfer(5);
    p.setMaxDiasPeraqfer(30);
    p.setTemLicEsp(false);
    p.setVendLicEsp(false);
    p.setContDobroLicEsp(false);
    p.setTemProgressao(false);
    p.setTemTempoServ(false);
    p.setTemSextaParte(false);
    p.setActive(true);
    return p;
  }

  private static PeriodoAquisitivoFerias validPa() {
    PeriodoAquisitivoFerias pa = new PeriodoAquisitivoFerias();
    pa.setId(700L);
    pa.setEmpCodigo(1);
    pa.setNumfunc(1001L);
    pa.setNumvinc(1);
    pa.setChave(1);
    pa.setExercicio(2025);
    pa.setDtini(LocalDate.of(2024, 1, 1));
    pa.setDtfim(LocalDate.of(2024, 12, 31));
    pa.setTotDiasDireito(30);
    pa.setFaltasApuradas(0);
    pa.setDiasAfastamentoSuspensivo(0);
    pa.setDiasGozados(0);
    pa.setDiasVendidosAbono(0);
    pa.setDiasConvertidosDobro(0);
    pa.setSaldoDiasDisponivel(30);
    pa.setSituacao("DISPONIVEL");
    pa.setDtPrescricao(LocalDate.of(2026, 12, 31));
    pa.setCriterioPrescricao("F");
    pa.setPermiteGozoPosPrescricao("I");
    pa.setSemPgtoMulta("I");
    pa.setAbonoDirGozo("I");
    pa.setAbonoConstFer("I");
    pa.setAdiant13Fer("I");
    pa.setAdiantSalFer("I");
    pa.setEmprestFer("I");
    pa.setPermiteFerEstePer("I");
    pa.setContDobroFerias("I");
    pa.setGeradoPorAverbacao(false);
    return pa;
  }

  private static FeriasGozo validFeriasGozo() {
    FeriasGozo fg = new FeriasGozo();
    fg.setId(900L);
    fg.setEmpCodigo(1);
    fg.setNumfunc(1001L);
    fg.setNumvinc(1);
    fg.setPeriodoAquisitivoId(700L);
    fg.setChavePeraqfer(1);
    fg.setOrdemParcela(1);
    fg.setDtini(LocalDate.of(2025, 7, 1));
    fg.setDtprevfim(LocalDate.of(2025, 7, 30));
    fg.setDiasGozo(30);
    fg.setTotDiasDireitoPa(30);
    fg.setAbonoPecuniario(false);
    fg.setDiasAbonoVendidos(0);
    fg.setAbonoConstitucional(true);
    fg.setAdiantamento13(false);
    fg.setAdiantamentoSalario(false);
    fg.setEmprestimoFerias(false);
    fg.setSituacao("PROGRAMADA");
    return fg;
  }
}
