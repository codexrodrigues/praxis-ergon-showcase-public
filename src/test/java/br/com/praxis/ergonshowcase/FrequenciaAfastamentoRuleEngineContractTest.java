package br.com.praxis.ergonshowcase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.praxis.ergonshowcase.config.TenantContext;
import br.com.praxis.ergonshowcase.domain.Afastamento;
import br.com.praxis.ergonshowcase.domain.CodigoFrequencia;
import br.com.praxis.ergonshowcase.domain.CodigoFrequenciaId;
import br.com.praxis.ergonshowcase.domain.Frequencia;
import br.com.praxis.ergonshowcase.domain.RegraFrequenciaEstatutaria;
import br.com.praxis.ergonshowcase.domain.ServidorPessoa;
import br.com.praxis.ergonshowcase.domain.Vinculo;
import br.com.praxis.ergonshowcase.domain.VinculoId;
import br.com.praxis.ergonshowcase.mapper.AfastamentoMapper;
import br.com.praxis.ergonshowcase.mapper.FrequenciaMapper;
import br.com.praxis.ergonshowcase.repository.AfastamentoRepository;
import br.com.praxis.ergonshowcase.repository.FrequenciaRepository;
import br.com.praxis.ergonshowcase.repository.RegraFrequenciaEstatutariaRepository;
import br.com.praxis.ergonshowcase.rules.AfastamentoConcessaoRuleSet;
import br.com.praxis.ergonshowcase.rules.ErgonRuleEngineFacade;
import br.com.praxis.ergonshowcase.rules.ErgonRuleEngineFacade.AfastamentoRuleContext;
import br.com.praxis.ergonshowcase.rules.ErgonRuleEngineFacade.FrequenciaRuleContext;
import br.com.praxis.ergonshowcase.rules.ErgonRuleErrorCatalog;
import br.com.praxis.ergonshowcase.rules.ErgonRuleViolationException;
import br.com.praxis.ergonshowcase.rules.FrequenciaLancamentoRuleSet;
import br.com.praxis.ergonshowcase.service.AfastamentoService;
import br.com.praxis.ergonshowcase.service.FrequenciaService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
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
import org.springframework.http.HttpStatus;

/**
 * Suíte determinística de contrato para os RuleSets governados de Lançamento de Frequência
 * ({@link FrequenciaLancamentoRuleSet} — {@code PCK_FREQUENCIAS}) e Concessão de Licenças e
 * Afastamentos ({@link AfastamentoConcessaoRuleSet} — {@code PCK_LIC_AFAST}) sobre o
 * {@code praxis-rules-engine:0.1.0-beta.17}.
 *
 * <p>Prova:
 * <ol>
 *   <li>Paridade integral das 16 decisões de {@code FrequenciaLancamentoRuleSet} e das 15 decisões
 *       de {@code AfastamentoConcessaoRuleSet};
 *   <li>Ponte dinâmica de fatos com {@link RegraFrequenciaEstatutaria} ({@code effectiveStatutoryRule}),
 *       cobrindo vedação ({@code naoSeAplica}), durações mínima/máxima, interstício, janela de
 *       prorrogação, limites acumulados mensais/anuais, documento legal e concomitância cruzada
 *       entre {@link Frequencia} e {@link Afastamento};
 *   <li>Os 4 modos formais de customização por órgão ({@code empCodigo = 1} vs {@code empCodigo = 2}):
 *       {@code FORBIDDEN}, {@code PARAMETERIZABLE} (Contrato 1.6), {@code RESTRICT_ONLY} ({@code AUGMENT})
 *       e {@code REPLACEABLE} ({@code REPLACE_EXACT});
 *   <li>Integração determinística e <i>fail-closed</i> (HTTP 422) em {@link FrequenciaService} e
 *       {@link AfastamentoService}.
 * </ol>
 */
class FrequenciaAfastamentoRuleEngineContractTest {

  private ErgonRuleEngineFacade facade;

  @BeforeEach
  void setUp() {
    facade = RuleCatalogExecution.facade();
  }

  @Test
  void bothProductRuleSetsCompileCleanlyUnderContract16AndAllowValidBaselines() {
    RuleDecisionPlan freqPlan = facade.resolveFrequenciaPlan(1);
    assertThat(freqPlan.slotsByKey()).hasSize(16);
    assertThat(freqPlan.planDigest()).isNotBlank();

    RuleEvaluationResult freqResult = facade.evaluateFrequencia(validFrequenciaContext());
    assertThat(freqResult.decision()).isEqualTo(RuleDecision.ALLOW);
    assertThat(freqResult.reasonCodes()).isEmpty();
    assertThat(freqResult.bindingResults()).hasSize(17);

    RuleDecisionPlan afastPlan = facade.resolveAfastamentoPlan(1);
    assertThat(afastPlan.slotsByKey()).hasSize(15);
    assertThat(afastPlan.planDigest()).isNotBlank();

    RuleEvaluationResult afastResult = facade.evaluateAfastamento(validAfastamentoContext());
    assertThat(afastResult.decision()).isEqualTo(RuleDecision.ALLOW);
    assertThat(afastResult.reasonCodes()).isEmpty();
    assertThat(afastResult.bindingResults()).hasSize(17);
  }

  static Stream<Arguments> frequenciaProductDecisionViolations() {
    return Stream.of(
        Arguments.of(
            "ERG-00415",
            FrequenciaLancamentoRuleSet.SLOT_PERIODO_ORDEM_VALIDA,
            (Supplier<FrequenciaRuleContext>)
                () -> {
                  FrequenciaRuleContext base = validFrequenciaContext();
                  base.draft().setDtini(LocalDate.of(2026, 5, 10));
                  base.draft().setDtfim(LocalDate.of(2026, 5, 5));
                  return base;
                }),
        Arguments.of(
            "ERG-00400",
            FrequenciaLancamentoRuleSet.SLOT_CODIGO_HABILITADO_VIA_FREQUENCIA,
            (Supplier<FrequenciaRuleContext>)
                () -> {
                  FrequenciaRuleContext base = validFrequenciaContext();
                  base.codigoFrequencia().setViaFrequencia(false);
                  return base;
                }),
        Arguments.of(
            "ERG-00410",
            FrequenciaLancamentoRuleSet.SLOT_VINCULO_VIGENTE_NO_PERIODO,
            (Supplier<FrequenciaRuleContext>)
                () -> {
                  FrequenciaRuleContext base = validFrequenciaContext();
                  base.draft().setDtini(LocalDate.of(2019, 1, 1));
                  base.draft().setDtfim(LocalDate.of(2019, 1, 1));
                  return base;
                }),
        Arguments.of(
            "ERG-00411",
            FrequenciaLancamentoRuleSet.SLOT_VINCULO_VIGENTE_NO_PERIODO,
            (Supplier<FrequenciaRuleContext>)
                () -> {
                  FrequenciaRuleContext base = validFrequenciaContext();
                  base.vinculo().setDtvac(LocalDate.of(2026, 5, 1));
                  base.draft().setDtini(LocalDate.of(2026, 5, 10));
                  base.draft().setDtfim(LocalDate.of(2026, 5, 10));
                  return base;
                }),
        Arguments.of(
            "ERG-00436",
            FrequenciaLancamentoRuleSet.SLOT_HORARIO_ENTRADA_SAIDA_VALIDO,
            (Supplier<FrequenciaRuleContext>)
                () -> {
                  FrequenciaRuleContext base = validFrequenciaContext();
                  base.codigoFrequencia().setHoraEntradaSaida(true);
                  base.draft().setHoraEntrada(LocalTime.of(14, 0));
                  base.draft().setHoraSaida(LocalTime.of(12, 0));
                  return base;
                }),
        Arguments.of(
            "ERG-00443",
            FrequenciaLancamentoRuleSet.SLOT_COMPATIBILIDADE_SEXO_SERVIDOR,
            (Supplier<FrequenciaRuleContext>)
                () -> {
                  FrequenciaRuleContext base = validFrequenciaContext();
                  base.codigoFrequencia().setSexo("F");
                  base.pessoa().setSexo("M");
                  return base;
                }),
        Arguments.of(
            "ERG-00441",
            FrequenciaLancamentoRuleSet.SLOT_REGRA_ESTATUTARIA_OBRIGATORIA,
            (Supplier<FrequenciaRuleContext>)
                () -> {
                  FrequenciaRuleContext base = validFrequenciaContext();
                  base.codigoFrequencia().setRegrasFreq(true);
                  return new FrequenciaRuleContext(
                      base.draft(),
                      base.vinculo(),
                      base.pessoa(),
                      base.codigoFrequencia(),
                      null,
                      List.of(),
                      List.of());
                }),
        Arguments.of(
            "ERG-00430",
            FrequenciaLancamentoRuleSet.SLOT_VEDACAO_NAO_SE_APLICA,
            (Supplier<FrequenciaRuleContext>)
                () -> {
                  FrequenciaRuleContext base = validFrequenciaContext();
                  base.effectiveStatutoryRule().setNaoSeAplica(true);
                  return base;
                }),
        Arguments.of(
            "ERG-00427",
            FrequenciaLancamentoRuleSet.SLOT_DURACAO_MINIMA_ESTATUTARIA,
            (Supplier<FrequenciaRuleContext>)
                () -> {
                  FrequenciaRuleContext base = validFrequenciaContext();
                  base.effectiveStatutoryRule().setQtdMinDias(BigDecimal.valueOf(2));
                  base.draft().setDtini(LocalDate.of(2026, 5, 10));
                  base.draft().setDtfim(LocalDate.of(2026, 5, 10));
                  return base;
                }),
        Arguments.of(
            "ERG-00428",
            FrequenciaLancamentoRuleSet.SLOT_DURACAO_MAXIMA_ESTATUTARIA,
            (Supplier<FrequenciaRuleContext>)
                () -> {
                  FrequenciaRuleContext base = validFrequenciaContext();
                  base.effectiveStatutoryRule().setQtdMaxDias(BigDecimal.valueOf(2));
                  base.draft().setDtini(LocalDate.of(2026, 5, 10));
                  base.draft().setDtfim(LocalDate.of(2026, 5, 13));
                  base.draft().setQuantidade(BigDecimal.valueOf(4));
                  return base;
                }),
        Arguments.of(
            "ERG-00429",
            FrequenciaLancamentoRuleSet.SLOT_LIMITE_PRORROGACAO_ACUMULADA,
            (Supplier<FrequenciaRuleContext>)
                () -> {
                  FrequenciaRuleContext base = validFrequenciaContext();
                  base.effectiveStatutoryRule().setQtdMaxDias(BigDecimal.valueOf(3));
                  base.effectiveStatutoryRule().setIntProrrogDias(5);
                  base.draft().setDtini(LocalDate.of(2026, 5, 10));
                  base.draft().setDtfim(LocalDate.of(2026, 5, 11)); // 2 dias
                  base.draft().setQuantidade(BigDecimal.valueOf(2));
                  Frequencia prev = new Frequencia();
                  prev.setId(600L);
                  prev.setNumfunc(1001L);
                  prev.setNumvinc(1);
                  prev.setTipofreq("FALTA");
                  prev.setCodfreq(101);
                  prev.setDtini(LocalDate.of(2026, 5, 6));
                  prev.setDtfim(LocalDate.of(2026, 5, 7)); // 2 dias dentro da janela de 5 dias (2 + 2 = 4 > 3)
                  prev.setSituacao("HOMOLOGADO");
                  return new FrequenciaRuleContext(
                      base.draft(),
                      base.vinculo(),
                      base.pessoa(),
                      base.codigoFrequencia(),
                      base.effectiveStatutoryRule(),
                      List.of(prev),
                      List.of());
                }),
        Arguments.of(
            "ERG-00440",
            FrequenciaLancamentoRuleSet.SLOT_INTERSTICIO_MINIMO_ENTRE_OCORRENCIAS,
            (Supplier<FrequenciaRuleContext>)
                () -> {
                  FrequenciaRuleContext base = validFrequenciaContext();
                  base.effectiveStatutoryRule().setIntervMinDias(15);
                  base.effectiveStatutoryRule().setIntProrrogDias(null);
                  Frequencia prev = new Frequencia();
                  prev.setId(601L);
                  prev.setNumfunc(1001L);
                  prev.setNumvinc(1);
                  prev.setTipofreq("FALTA");
                  prev.setCodfreq(101);
                  prev.setDtini(LocalDate.of(2026, 5, 4));
                  prev.setDtfim(LocalDate.of(2026, 5, 4)); // 5 dias antes de 2026-05-10 (< 15)
                  prev.setSituacao("HOMOLOGADO");
                  return new FrequenciaRuleContext(
                      base.draft(),
                      base.vinculo(),
                      base.pessoa(),
                      base.codigoFrequencia(),
                      base.effectiveStatutoryRule(),
                      List.of(prev),
                      List.of());
                }),
        Arguments.of(
            "ERG-00444",
            FrequenciaLancamentoRuleSet.SLOT_LIMITES_ACUMULADOS_MES_ANO,
            (Supplier<FrequenciaRuleContext>)
                () -> {
                  FrequenciaRuleContext base = validFrequenciaContext();
                  base.effectiveStatutoryRule().setIntervMinDias(null);
                  base.effectiveStatutoryRule().setQtdMaxInterpMes(2);
                  Frequencia prev1 = new Frequencia();
                  prev1.setId(602L);
                  prev1.setNumfunc(1001L);
                  prev1.setNumvinc(1);
                  prev1.setTipofreq("FALTA");
                  prev1.setCodfreq(101);
                  prev1.setDtini(LocalDate.of(2026, 5, 2));
                  prev1.setDtfim(LocalDate.of(2026, 5, 3)); // 2 dias no mesmo mês + 1 dia atual = 3 > 2
                  prev1.setSituacao("HOMOLOGADO");
                  return new FrequenciaRuleContext(
                      base.draft(),
                      base.vinculo(),
                      base.pessoa(),
                      base.codigoFrequencia(),
                      base.effectiveStatutoryRule(),
                      List.of(prev1),
                      List.of());
                }),
        Arguments.of(
            "ERG-00445",
            FrequenciaLancamentoRuleSet.SLOT_EXIGENCIA_DOCUMENTO_LEGAL,
            (Supplier<FrequenciaRuleContext>)
                () -> {
                  FrequenciaRuleContext base = validFrequenciaContext();
                  base.effectiveStatutoryRule().setDocumentoLegalId(91L);
                  base.draft().setDocumentoLegalId(null);
                  return base;
                }),
        Arguments.of(
            "ERG-00446",
            FrequenciaLancamentoRuleSet.SLOT_LIMITE_RETROATIVIDADE_LANCAMENTO,
            (Supplier<FrequenciaRuleContext>)
                () -> {
                  FrequenciaRuleContext base = validFrequenciaContext();
                  // Referência 90 dias após dtini (2026-05-10 -> 2026-08-08 = 90 > default 60)
                  return new FrequenciaRuleContext(
                      base.draft(),
                      base.vinculo(),
                      base.pessoa(),
                      base.codigoFrequencia(),
                      base.effectiveStatutoryRule(),
                      List.of(),
                      List.of(),
                      LocalDate.of(2026, 8, 8));
                }),
        Arguments.of(
            "ERG-00417",
            FrequenciaLancamentoRuleSet.SLOT_CONCOMITANCIA_AFASTAMENTO_ATIVO,
            (Supplier<FrequenciaRuleContext>)
                () -> {
                  FrequenciaRuleContext base = validFrequenciaContext();
                  base.effectiveStatutoryRule().setPermiteConcomitancia(false);
                  Afastamento activeAfast = new Afastamento();
                  activeAfast.setId(500L);
                  activeAfast.setNumfunc(1001L);
                  activeAfast.setNumvinc(1);
                  activeAfast.setDtini(LocalDate.of(2026, 5, 1));
                  activeAfast.setDtprevfim(LocalDate.of(2026, 5, 20));
                  activeAfast.setSituacao("HOMOLOGADO");
                  return new FrequenciaRuleContext(
                      base.draft(),
                      base.vinculo(),
                      base.pessoa(),
                      base.codigoFrequencia(),
                      base.effectiveStatutoryRule(),
                      List.of(),
                      List.of(activeAfast));
                }),
        Arguments.of(
            "ERG-00424",
            FrequenciaLancamentoRuleSet.SLOT_SOBREPOSICAO_OCORRENCIA_MESMO_TIPO,
            (Supplier<FrequenciaRuleContext>)
                () -> {
                  FrequenciaRuleContext base = validFrequenciaContext();
                  Frequencia overlap = new Frequencia();
                  overlap.setId(605L);
                  overlap.setNumfunc(1001L);
                  overlap.setNumvinc(1);
                  overlap.setTipofreq("FALTA");
                  overlap.setCodfreq(101);
                  overlap.setDtini(LocalDate.of(2026, 5, 10));
                  overlap.setDtfim(LocalDate.of(2026, 5, 10));
                  overlap.setSituacao("HOMOLOGADO");
                  return new FrequenciaRuleContext(
                      base.draft(),
                      base.vinculo(),
                      base.pessoa(),
                      base.codigoFrequencia(),
                      base.effectiveStatutoryRule(),
                      List.of(overlap),
                      List.of());
                }));
  }

  @ParameterizedTest(name = "Frequencia: decisão {0} no slot {1} bloqueia violação determinística")
  @MethodSource("frequenciaProductDecisionViolations")
  void everyFrequenciaProductDecisionProducesCanonicalErgReasonCode(
      String expectedReasonCode,
      String expectedSlotKey,
      Supplier<FrequenciaRuleContext> contextSupplier) {
    RuleEvaluationResult result = facade.evaluateFrequencia(contextSupplier.get());

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

  static Stream<Arguments> afastamentoProductDecisionViolations() {
    return Stream.of(
        Arguments.of(
            "ERG-04312",
            AfastamentoConcessaoRuleSet.SLOT_PERIODO_ORDEM_VALIDA,
            (Supplier<AfastamentoRuleContext>)
                () -> {
                  AfastamentoRuleContext base = validAfastamentoContext();
                  base.draft().setDtini(LocalDate.of(2026, 3, 15));
                  base.draft().setDtfim(LocalDate.of(2026, 3, 1));
                  base.draft().setDtprevfim(LocalDate.of(2026, 3, 20));
                  return base;
                }),
        Arguments.of(
            "ERG-04313",
            AfastamentoConcessaoRuleSet.SLOT_PERIODO_ORDEM_VALIDA,
            (Supplier<AfastamentoRuleContext>)
                () -> {
                  AfastamentoRuleContext base = validAfastamentoContext();
                  base.draft().setDtini(LocalDate.of(2026, 3, 15));
                  base.draft().setDtprevfim(LocalDate.of(2026, 3, 1));
                  return base;
                }),
        Arguments.of(
            "ERG-04314",
            AfastamentoConcessaoRuleSet.SLOT_VINCULO_EM_EXERCICIO_NO_PERIODO,
            (Supplier<AfastamentoRuleContext>)
                () -> {
                  AfastamentoRuleContext base = validAfastamentoContext();
                  base.draft().setDtini(LocalDate.of(2018, 1, 1));
                  base.draft().setDtprevfim(LocalDate.of(2018, 1, 10));
                  return base;
                }),
        Arguments.of(
            "ERG-04306",
            AfastamentoConcessaoRuleSet.SLOT_MODALIDADE_HABILITADA_AFASTAMENTO,
            (Supplier<AfastamentoRuleContext>)
                () -> {
                  AfastamentoRuleContext base = validAfastamentoContext();
                  base.codigoFrequencia().setAfastamento(false);
                  return base;
                }),
        Arguments.of(
            "ERG-04310",
            AfastamentoConcessaoRuleSet.SLOT_COMPATIBILIDADE_SEXO_SERVIDOR,
            (Supplier<AfastamentoRuleContext>)
                () -> {
                  AfastamentoRuleContext base = validAfastamentoContext();
                  base.codigoFrequencia().setSexo("F");
                  base.pessoa().setSexo("M");
                  return base;
                }),
        Arguments.of(
            "ERG-04305",
            AfastamentoConcessaoRuleSet.SLOT_REGRA_ESTATUTARIA_OBRIGATORIA,
            (Supplier<AfastamentoRuleContext>)
                () -> {
                  AfastamentoRuleContext base = validAfastamentoContext();
                  base.codigoFrequencia().setRegrasFreq(true);
                  return new AfastamentoRuleContext(
                      base.draft(),
                      base.vinculo(),
                      base.pessoa(),
                      base.codigoFrequencia(),
                      null,
                      List.of(),
                      List.of(),
                      null,
                      null);
                }),
        Arguments.of(
            "ERG-04301",
            AfastamentoConcessaoRuleSet.SLOT_VEDACAO_NAO_SE_APLICA,
            (Supplier<AfastamentoRuleContext>)
                () -> {
                  AfastamentoRuleContext base = validAfastamentoContext();
                  base.effectiveStatutoryRule().setNaoSeAplica(true);
                  return base;
                }),
        Arguments.of(
            "ERG-04304",
            AfastamentoConcessaoRuleSet.SLOT_DURACAO_MINIMA_ESTATUTARIA,
            (Supplier<AfastamentoRuleContext>)
                () -> {
                  AfastamentoRuleContext base = validAfastamentoContext();
                  base.effectiveStatutoryRule().setQtdMinDias(BigDecimal.valueOf(5));
                  base.draft().setDtini(LocalDate.of(2026, 3, 1));
                  base.draft().setDtprevfim(LocalDate.of(2026, 3, 3)); // 3 dias < 5
                  base.draft().setQuantidadeDias(3);
                  return base;
                }),
        Arguments.of(
            "ERG-04303",
            AfastamentoConcessaoRuleSet.SLOT_DURACAO_MAXIMA_ESTATUTARIA,
            (Supplier<AfastamentoRuleContext>)
                () -> {
                  AfastamentoRuleContext base = validAfastamentoContext();
                  base.effectiveStatutoryRule().setQtdMaxDias(BigDecimal.valueOf(30));
                  base.draft().setDtini(LocalDate.of(2026, 3, 1));
                  base.draft().setDtprevfim(LocalDate.of(2026, 4, 15)); // 46 dias > 30
                  base.draft().setQuantidadeDias(46);
                  return base;
                }),
        Arguments.of(
            "ERG-04317",
            AfastamentoConcessaoRuleSet.SLOT_PRORROGACAO_ESTATUTARIA_VALIDA,
            (Supplier<AfastamentoRuleContext>)
                () -> {
                  AfastamentoRuleContext base = validAfastamentoContext();
                  base.draft().setProrrogacao(true);
                  base.draft().setAfastamentoOrigemId(null);
                  return base;
                }),
        Arguments.of(
            "ERG-04309",
            AfastamentoConcessaoRuleSet.SLOT_INTERSTICIO_MINIMO_ENTRE_CONCESSOES,
            (Supplier<AfastamentoRuleContext>)
                () -> {
                  AfastamentoRuleContext base = validAfastamentoContext();
                  base.effectiveStatutoryRule().setIntervMinDias(30);
                  Afastamento prev = new Afastamento();
                  prev.setId(490L);
                  prev.setNumfunc(1001L);
                  prev.setNumvinc(1);
                  prev.setTipofreq("LICENCA");
                  prev.setCodfreq(201);
                  prev.setDtini(LocalDate.of(2026, 2, 10));
                  prev.setDtfim(LocalDate.of(2026, 2, 20)); // Apenas 8 dias antes de 2026-03-01 (< 30)
                  prev.setSituacao("HOMOLOGADO");
                  return new AfastamentoRuleContext(
                      base.draft(),
                      base.vinculo(),
                      base.pessoa(),
                      base.codigoFrequencia(),
                      base.effectiveStatutoryRule(),
                      List.of(prev),
                      List.of(),
                      null,
                      null);
                }),
        Arguments.of(
            "ERG-04311",
            AfastamentoConcessaoRuleSet.SLOT_LIMITES_ACUMULADOS_MES_ANO,
            (Supplier<AfastamentoRuleContext>)
                () -> {
                  AfastamentoRuleContext base = validAfastamentoContext();
                  base.effectiveStatutoryRule().setIntervMinDias(null);
                  base.effectiveStatutoryRule().setQtdMaxInterpAno(20);
                  Afastamento prev = new Afastamento();
                  prev.setId(491L);
                  prev.setNumfunc(1001L);
                  prev.setNumvinc(1);
                  prev.setTipofreq("LICENCA");
                  prev.setCodfreq(201);
                  prev.setDtini(LocalDate.of(2026, 1, 5));
                  prev.setDtfim(LocalDate.of(2026, 1, 19)); // 15 dias + 10 dias atuais = 25 > 20
                  prev.setQuantidadeDias(15);
                  prev.setSituacao("HOMOLOGADO");
                  return new AfastamentoRuleContext(
                      base.draft(),
                      base.vinculo(),
                      base.pessoa(),
                      base.codigoFrequencia(),
                      base.effectiveStatutoryRule(),
                      List.of(prev),
                      List.of(),
                      null,
                      null);
                }),
        Arguments.of(
            "ERG-07513",
            AfastamentoConcessaoRuleSet.SLOT_DISPENSA_PERICIA_DOCUMENTO_LEGAL,
            (Supplier<AfastamentoRuleContext>)
                () -> {
                  AfastamentoRuleContext base = validAfastamentoContext();
                  base.draft().setDocumentoLegalId(null);
                  base.draft().setDtini(LocalDate.of(2026, 3, 1));
                  base.draft().setDtprevfim(LocalDate.of(2026, 3, 20)); // 20 dias > 15 sem documentoLegalId
                  base.draft().setQuantidadeDias(20);
                  return base;
                }),
        Arguments.of(
            "ERG-04318",
            AfastamentoConcessaoRuleSet.SLOT_SUBSTITUTO_DESIGNADO_VALIDO,
            (Supplier<AfastamentoRuleContext>)
                () -> {
                  AfastamentoRuleContext base = validAfastamentoContext();
                  base.draft().setNumfuncSubstituto(1001L); // Auto-substituição
                  base.draft().setNumvincSubstituto(1);
                  return base;
                }),
        Arguments.of(
            "ERG-04316",
            AfastamentoConcessaoRuleSet.SLOT_CONCOMITANCIA_FREQUENCIA_ATIVA,
            (Supplier<AfastamentoRuleContext>)
                () -> {
                  AfastamentoRuleContext base = validAfastamentoContext();
                  base.effectiveStatutoryRule().setPermiteConcomitancia(false);
                  Frequencia activeFreq = new Frequencia();
                  activeFreq.setId(700L);
                  activeFreq.setNumfunc(1001L);
                  activeFreq.setNumvinc(1);
                  activeFreq.setTipofreq("PONTO");
                  activeFreq.setCodfreq(301);
                  activeFreq.setDtini(LocalDate.of(2026, 3, 5));
                  activeFreq.setDtfim(LocalDate.of(2026, 3, 5));
                  activeFreq.setSituacao("HOMOLOGADO");
                  return new AfastamentoRuleContext(
                      base.draft(),
                      base.vinculo(),
                      base.pessoa(),
                      base.codigoFrequencia(),
                      base.effectiveStatutoryRule(),
                      List.of(),
                      List.of(activeFreq),
                      null,
                      null);
                }),
        Arguments.of(
            "ERG-04300",
            AfastamentoConcessaoRuleSet.SLOT_SOBREPOSICAO_AFASTAMENTO_ATIVO,
            (Supplier<AfastamentoRuleContext>)
                () -> {
                  AfastamentoRuleContext base = validAfastamentoContext();
                  Afastamento overlap = new Afastamento();
                  overlap.setId(499L);
                  overlap.setNumfunc(1001L);
                  overlap.setNumvinc(1);
                  overlap.setTipofreq("LICENCA");
                  overlap.setCodfreq(201);
                  overlap.setDtini(LocalDate.of(2026, 3, 5));
                  overlap.setDtprevfim(LocalDate.of(2026, 3, 15));
                  overlap.setSituacao("HOMOLOGADO");
                  return new AfastamentoRuleContext(
                      base.draft(),
                      base.vinculo(),
                      base.pessoa(),
                      base.codigoFrequencia(),
                      base.effectiveStatutoryRule(),
                      List.of(overlap),
                      List.of(),
                      null,
                      null);
                }));
  }

  @ParameterizedTest(name = "Afastamento: decisão {0} no slot {1} bloqueia violação determinística")
  @MethodSource("afastamentoProductDecisionViolations")
  void everyAfastamentoProductDecisionProducesCanonicalErgReasonCode(
      String expectedReasonCode,
      String expectedSlotKey,
      Supplier<AfastamentoRuleContext> contextSupplier) {
    RuleEvaluationResult result = facade.evaluateAfastamento(contextSupplier.get());

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
  void dynamicStatutoryRuleResolutionSelectsMostSpecificRuleForVinculoScope() {
    Vinculo vinculo = validVinculo();
    vinculo.setEmpCodigo(1);
    vinculo.setRegimejur("ESTATUTARIO");
    vinculo.setTipovinc("EFETIVO");
    vinculo.setCategoria("SAUDE");
    vinculo.setSubcategoria("MEDICO");

    // Regra 1: regra geral do ente público (empCodigo = null, subcategoria = null)
    RegraFrequenciaEstatutaria generalRule = validStatutoryRule("LICENCA", 201);
    generalRule.setId(1L);
    generalRule.setEmpCodigo(null);
    generalRule.setCategoria(null);
    generalRule.setSubcategoria(null);
    generalRule.setQtdMaxDias(BigDecimal.valueOf(60));

    // Regra 2: regra específica do Órgão 1 para SAUDE / MEDICO
    RegraFrequenciaEstatutaria specificRule = validStatutoryRule("LICENCA", 201);
    specificRule.setId(2L);
    specificRule.setEmpCodigo(1);
    specificRule.setCategoria("SAUDE");
    specificRule.setSubcategoria("MEDICO");
    specificRule.setQtdMaxDias(BigDecimal.valueOf(30));

    var resolved =
        facade.resolveEffectiveStatutoryRule(
            List.of(generalRule, specificRule),
            vinculo,
            "LICENCA",
            201,
            LocalDate.of(2026, 3, 1));
    assertThat(resolved).isPresent();
    assertThat(resolved.get().getId()).isEqualTo(2L);
    assertThat(resolved.get().getQtdMaxDias()).isEqualByComparingTo(BigDecimal.valueOf(30));

    // Quando o vínculo pertence a outra categoria sem regra específica, faz fallback para a regra geral
    vinculo.setCategoria("ADMINISTRATIVO");
    vinculo.setSubcategoria("ANALISTA");
    var fallback =
        facade.resolveEffectiveStatutoryRule(
            List.of(generalRule, specificRule),
            vinculo,
            "LICENCA",
            201,
            LocalDate.of(2026, 3, 1));
    assertThat(fallback).isPresent();
    assertThat(fallback.get().getId()).isEqualTo(1L);
  }

  @Test
  void mode1ForbiddenOverridesOnFrequenciaAndAfastamentoProtectedGuardsFailAtCompilation() {
    assertThatThrownBy(
            () ->
                facade.compilePlan(
                    FrequenciaLancamentoRuleSet.customerMode1ForbiddenOverrideAttempt()))
        .isInstanceOf(RulePlanException.class)
        .satisfies(
            ex -> {
              RulePlanException planEx = (RulePlanException) ex;
              assertThat(planEx.getCode()).isEqualTo(RulePlanIssueCode.PLAN_OVERRIDE_INVALID);
              assertThat(planEx.getMessage())
                  .contains(FrequenciaLancamentoRuleSet.SLOT_PERIODO_ORDEM_VALIDA);
            });

    assertThatThrownBy(
            () ->
                facade.compilePlan(
                    AfastamentoConcessaoRuleSet.customerMode1ForbiddenOverrideAttempt()))
        .isInstanceOf(RulePlanException.class)
        .satisfies(
            ex -> {
              RulePlanException planEx = (RulePlanException) ex;
              assertThat(planEx.getCode()).isEqualTo(RulePlanIssueCode.PLAN_OVERRIDE_INVALID);
              assertThat(planEx.getMessage())
                  .contains(AfastamentoConcessaoRuleSet.SLOT_PERIODO_ORDEM_VALIDA);
            });
  }

  @Test
  void mode2ParameterizableSlotsEnforceOrgaoSpecificThresholdsUnderContract16() {
    // 1. Frequência: lançamento com 30 dias de retroatividade (2026-05-10 avaliado em 2026-06-09)
    FrequenciaRuleContext baseFreq = validFrequenciaContext();
    LocalDate refDate30DaysLater = LocalDate.of(2026, 6, 9);

    // Órgão 1 (default maxRetroactiveDays = 60) -> ALLOW
    FrequenciaRuleContext orgao1Freq =
        new FrequenciaRuleContext(
            baseFreq.draft(),
            baseFreq.vinculo(),
            baseFreq.pessoa(),
            baseFreq.codigoFrequencia(),
            baseFreq.effectiveStatutoryRule(),
            List.of(),
            List.of(),
            refDate30DaysLater);
    assertThat(facade.evaluateFrequencia(orgao1Freq).decision()).isEqualTo(RuleDecision.ALLOW);

    // Órgão 2 (parametrizado maxRetroactiveDays = 15) -> DENY ERG-00446
    baseFreq.draft().setEmpCodigo(2);
    baseFreq.vinculo().setEmpCodigo(2);
    FrequenciaRuleContext orgao2Freq =
        new FrequenciaRuleContext(
            baseFreq.draft(),
            baseFreq.vinculo(),
            baseFreq.pessoa(),
            baseFreq.codigoFrequencia(),
            baseFreq.effectiveStatutoryRule(),
            List.of(),
            List.of(),
            refDate30DaysLater);
    RuleEvaluationResult freqDeny = facade.evaluateFrequencia(orgao2Freq);
    assertThat(freqDeny.decision()).isEqualTo(RuleDecision.DENY);
    assertThat(freqDeny.reasonCodes()).containsExactly("ERG-00446");

    // 2. Afastamento: 10 dias sem documentoLegalId (dispensa de perícia)
    AfastamentoRuleContext baseAfast = validAfastamentoContext();
    baseAfast.draft().setDocumentoLegalId(null); // 10 dias (2026-03-01 a 2026-03-10)

    // Órgão 1 (default maxDaysWithoutPericia = 15) -> ALLOW
    assertThat(facade.evaluateAfastamento(baseAfast).decision()).isEqualTo(RuleDecision.ALLOW);

    // Órgão 2 (parametrizado maxDaysWithoutPericia = 5) -> DENY ERG-07513
    baseAfast.draft().setEmpCodigo(2);
    baseAfast.vinculo().setEmpCodigo(2);
    RuleEvaluationResult afastDeny = facade.evaluateAfastamento(baseAfast);
    assertThat(afastDeny.decision()).isEqualTo(RuleDecision.DENY);
    assertThat(afastDeny.reasonCodes()).containsExactly("ERG-07513");

    // 3. Valores fora do envelope máximo do RuleParameterTemplate falham na compilação
    assertThatThrownBy(
            () ->
                facade.compilePlan(
                    FrequenciaLancamentoRuleSet.withCustomerBindings(
                        List.of(
                            FrequenciaLancamentoRuleSet.customerMode2ParameterizeBinding(0, 999)))))
        .isInstanceOf(RulePlanException.class)
        .satisfies(
            ex ->
                assertThat(((RulePlanException) ex).getCode())
                    .isEqualTo(RulePlanIssueCode.PLAN_PARAMETER_VALUES_INVALID));

    assertThatThrownBy(
            () ->
                facade.compilePlan(
                    AfastamentoConcessaoRuleSet.withCustomerBindings(
                        List.of(
                            AfastamentoConcessaoRuleSet.customerMode2ParameterizeBinding(1, 999)))))
        .isInstanceOf(RulePlanException.class)
        .satisfies(
            ex ->
                assertThat(((RulePlanException) ex).getCode())
                    .isEqualTo(RulePlanIssueCode.PLAN_PARAMETER_VALUES_INVALID));
  }

  @Test
  void mode3RestrictOnlyWithAugmentEnforcesCustomerAndProductRulesUnderDenyOverrides() {
    // 1. Frequência no Órgão 2: 4 dias sem documentoLegalId -> bloqueado por CERG-09101 (DecisionSource.CUSTOMER)
    FrequenciaRuleContext freqCtx = validFrequenciaContext();
    freqCtx.draft().setEmpCodigo(2);
    freqCtx.vinculo().setEmpCodigo(2);
    freqCtx.draft().setDtini(LocalDate.of(2026, 5, 10));
    freqCtx.draft().setDtfim(LocalDate.of(2026, 5, 13)); // 4 dias (> 3)
    freqCtx.draft().setQuantidade(BigDecimal.valueOf(4));
    freqCtx.draft().setDocumentoLegalId(null);

    RuleEvaluationResult freqAugmentDeny = facade.evaluateFrequencia(freqCtx);
    assertThat(freqAugmentDeny.decision()).isEqualTo(RuleDecision.DENY);
    assertThat(freqAugmentDeny.reasonCodes()).containsExactly("CERG-09101");

    // Quando documentoLegalId é informado, mas o acumulado mensal estoura qtdMaxInterpMes (ERG-00444), ambos coexistem!
    freqCtx.draft().setDocumentoLegalId(91L);
    freqCtx.effectiveStatutoryRule().setQtdMaxInterpMes(3);
    RuleEvaluationResult freqProductDeny = facade.evaluateFrequencia(freqCtx);
    assertThat(freqProductDeny.decision()).isEqualTo(RuleDecision.DENY);
    assertThat(freqProductDeny.reasonCodes()).containsExactly("ERG-00444");

    // 2. Afastamento no Órgão 2: 35 dias (31 em março + 4 em abril <= qtdMaxConsecMes 31) sem atoPublicacaoId -> bloqueado por CERG-09201
    AfastamentoRuleContext afastCtx = validAfastamentoContext();
    afastCtx.draft().setEmpCodigo(2);
    afastCtx.vinculo().setEmpCodigo(2);
    afastCtx.draft().setDtini(LocalDate.of(2026, 3, 1));
    afastCtx.draft().setDtprevfim(LocalDate.of(2026, 4, 4)); // 35 dias (> 30)
    afastCtx.draft().setQuantidadeDias(35);
    afastCtx.draft().setDocumentoLegalId(91L);
    afastCtx.draft().setAtoPublicacaoId(null);

    RuleEvaluationResult afastAugmentDeny = facade.evaluateAfastamento(afastCtx);
    assertThat(afastAugmentDeny.decision()).isEqualTo(RuleDecision.DENY);
    assertThat(afastAugmentDeny.reasonCodes()).containsExactly("CERG-09201");

    // Quando atoPublicacaoId é informado, mas ultrapassa qtdMaxInterpAno (ERG-04311), a regra do produto permanece ativa!
    afastCtx.draft().setAtoPublicacaoId(92L);
    afastCtx.effectiveStatutoryRule().setQtdMaxInterpAno(30);
    RuleEvaluationResult afastProductDeny = facade.evaluateAfastamento(afastCtx);
    assertThat(afastProductDeny.decision()).isEqualTo(RuleDecision.DENY);
    assertThat(afastProductDeny.reasonCodes()).containsExactly("ERG-04311");
  }

  @Test
  void mode4ReplaceExactReplacesConcomitancePolicyWithoutDisablingOtherProductSlots() {
    // 1. Frequência durante Afastamento ativo quando effectiveStatutoryRule.permiteConcomitancia == true
    FrequenciaRuleContext baseFreq = validFrequenciaContext();
    baseFreq.effectiveStatutoryRule().setPermiteConcomitancia(true);
    Afastamento activeAfast = new Afastamento();
    activeAfast.setId(500L);
    activeAfast.setNumfunc(1001L);
    activeAfast.setNumvinc(1);
    activeAfast.setDtini(LocalDate.of(2026, 5, 1));
    activeAfast.setDtprevfim(LocalDate.of(2026, 5, 20));
    activeAfast.setSituacao("HOMOLOGADO");

    FrequenciaRuleContext orgao1Freq =
        new FrequenciaRuleContext(
            baseFreq.draft(),
            baseFreq.vinculo(),
            baseFreq.pessoa(),
            baseFreq.codigoFrequencia(),
            baseFreq.effectiveStatutoryRule(),
            List.of(),
            List.of(activeAfast));
    // Órgão 1 permite porque permiteConcomitancia == true
    assertThat(facade.evaluateFrequencia(orgao1Freq).decision()).isEqualTo(RuleDecision.ALLOW);

    // Órgão 2 (REPLACE_EXACT -> CERG-00417) bloqueia mesmo com permiteConcomitancia == true
    baseFreq.draft().setEmpCodigo(2);
    baseFreq.vinculo().setEmpCodigo(2);
    RuleEvaluationResult orgao2FreqDeny = facade.evaluateFrequencia(orgao1Freq);
    assertThat(orgao2FreqDeny.decision()).isEqualTo(RuleDecision.DENY);
    assertThat(orgao2FreqDeny.reasonCodes()).containsExactly("CERG-00417");

    // 2. Afastamento sobre Frequência ativa quando effectiveStatutoryRule.permiteConcomitancia == true
    AfastamentoRuleContext baseAfast = validAfastamentoContext();
    baseAfast.effectiveStatutoryRule().setPermiteConcomitancia(true);
    Frequencia activeFreq = new Frequencia();
    activeFreq.setId(700L);
    activeFreq.setNumfunc(1001L);
    activeFreq.setNumvinc(1);
    activeFreq.setTipofreq("PONTO");
    activeFreq.setCodfreq(301);
    activeFreq.setDtini(LocalDate.of(2026, 3, 5));
    activeFreq.setDtfim(LocalDate.of(2026, 3, 5));
    activeFreq.setSituacao("HOMOLOGADO");

    AfastamentoRuleContext orgao1Afast =
        new AfastamentoRuleContext(
            baseAfast.draft(),
            baseAfast.vinculo(),
            baseAfast.pessoa(),
            baseAfast.codigoFrequencia(),
            baseAfast.effectiveStatutoryRule(),
            List.of(),
            List.of(activeFreq),
            null,
            null);
    // Órgão 1 permite porque permiteConcomitancia == true
    assertThat(facade.evaluateAfastamento(orgao1Afast).decision()).isEqualTo(RuleDecision.ALLOW);

    // Órgão 2 (REPLACE_EXACT -> CERG-04316) bloqueia mesmo com permiteConcomitancia == true
    baseAfast.draft().setEmpCodigo(2);
    baseAfast.vinculo().setEmpCodigo(2);
    RuleEvaluationResult orgao2AfastDeny = facade.evaluateAfastamento(orgao1Afast);
    assertThat(orgao2AfastDeny.decision()).isEqualTo(RuleDecision.DENY);
    assertThat(orgao2AfastDeny.reasonCodes()).containsExactly("CERG-04316");
  }

  @Test
  void frequenciaAndAfastamentoServicesEnforceFailClosedRulesWithDynamicStatutoryRuleBridge() {
    RegraFrequenciaEstatutariaRepository regraRepo =
        mock(RegraFrequenciaEstatutariaRepository.class);
    FrequenciaRepository freqRepo = mock(FrequenciaRepository.class);
    AfastamentoRepository afastRepo = mock(AfastamentoRepository.class);

    FrequenciaService freqService =
        new FrequenciaService(
            freqRepo, new FrequenciaMapper(), new TenantContext(), facade, regraRepo);
    AfastamentoService afastService =
        new AfastamentoService(
            afastRepo, new AfastamentoMapper(), new TenantContext(), facade, regraRepo);

    RegraFrequenciaEstatutaria faltaRule = validStatutoryRule("FALTA", 101);
    when(regraRepo.findByTipofreqAndCodfreq("FALTA", 101)).thenReturn(List.of(faltaRule));

    FrequenciaRuleContext validFreqCtx = validFrequenciaContext();
    Frequencia validFreq = validFreqCtx.draft();
    validFreq.setVinculo(validFreqCtx.vinculo());
    validFreq.setPessoa(validFreqCtx.pessoa());
    validFreq.setCodigoFrequenciaEntidade(validFreqCtx.codigoFrequencia());

    assertThat(freqService.validateFrequenciaLancamento(validFreq).decision())
        .isEqualTo(RuleDecision.ALLOW);

    // Violação de vedação estatutária (naoSeAplica = true -> HTTP 422 ERG-00430)
    faltaRule.setNaoSeAplica(true);
    assertThatThrownBy(() -> freqService.validateFrequenciaLancamento(validFreq))
        .isInstanceOf(ErgonRuleViolationException.class)
        .satisfies(
            ex -> {
              ErgonRuleViolationException ruleEx = (ErgonRuleViolationException) ex;
              assertThat(ruleEx.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
              assertThat(ruleEx.getReasonCodes()).containsExactly("ERG-00430");
              assertThat(ruleEx.getDecisionSources()).containsExactly(DecisionSource.PRODUCT);
              assertThat(ruleEx.getBody().getProperties())
                  .containsEntry("ruleSetKey", FrequenciaLancamentoRuleSet.RULESET_KEY);
            });

    // Afastamento no Órgão 2 violando CERG-09201 (CUSTOMER source)
    RegraFrequenciaEstatutaria licencaRule = validStatutoryRule("LICENCA", 201);
    licencaRule.setEmpCodigo(2);
    licencaRule.setQtdMaxDias(BigDecimal.valueOf(60));
    licencaRule.setQtdMaxConsecMes(31);
    licencaRule.setQtdMaxInterpMes(31);
    licencaRule.setQtdMaxConsecAno(90);
    licencaRule.setQtdMaxInterpAno(90);
    when(regraRepo.findByTipofreqAndCodfreq("LICENCA", 201)).thenReturn(List.of(licencaRule));

    AfastamentoRuleContext validAfastCtx = validAfastamentoContext();
    Afastamento afastOrgao2 = validAfastCtx.draft();
    afastOrgao2.setEmpCodigo(2);
    validAfastCtx.vinculo().setEmpCodigo(2);
    afastOrgao2.setVinculo(validAfastCtx.vinculo());
    afastOrgao2.setPessoa(validAfastCtx.pessoa());
    afastOrgao2.setCodigoFrequenciaEntidade(validAfastCtx.codigoFrequencia());
    afastOrgao2.setDtini(LocalDate.of(2026, 3, 1));
    afastOrgao2.setDtprevfim(LocalDate.of(2026, 4, 5)); // 36 dias (> 30) sem atoPublicacaoId
    afastOrgao2.setQuantidadeDias(36);
    afastOrgao2.setAtoPublicacaoId(null);

    assertThatThrownBy(() -> afastService.validateAfastamentoConcessao(afastOrgao2))
        .isInstanceOf(ErgonRuleViolationException.class)
        .satisfies(
            ex -> {
              ErgonRuleViolationException ruleEx = (ErgonRuleViolationException) ex;
              assertThat(ruleEx.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
              assertThat(ruleEx.getReasonCodes()).containsExactly("CERG-09201");
              assertThat(ruleEx.getDecisionSources()).containsExactly(DecisionSource.CUSTOMER);
              assertThat(ruleEx.getBody().getProperties())
                  .containsEntry("ruleSetKey", AfastamentoConcessaoRuleSet.RULESET_KEY);
            });
  }

  @Test
  void multiHopContiguousChainAndCrossOrganDocumentoLegalAreEnforcedDeterministically() {
    // 1. Cadeia contígua multi-hop: f1 (02..03/05 = 2d) -> f2 (04..05/05 = 2d) -> draft (06..07/05 = 2d) = 6d > qtdMaxConsecMes (5)
    FrequenciaRuleContext baseFreq = validFrequenciaContext();
    baseFreq.effectiveStatutoryRule().setIntervMinDias(null);
    baseFreq.effectiveStatutoryRule().setIntProrrogDias(null);
    baseFreq.effectiveStatutoryRule().setQtdMaxConsecMes(5);
    baseFreq.effectiveStatutoryRule().setQtdMaxInterpMes(10);
    baseFreq.draft().setDtini(LocalDate.of(2026, 5, 6));
    baseFreq.draft().setDtfim(LocalDate.of(2026, 5, 7));
    baseFreq.draft().setQuantidade(BigDecimal.valueOf(2));

    Frequencia f1 = new Frequencia();
    f1.setId(610L);
    f1.setNumfunc(1001L);
    f1.setNumvinc(1);
    f1.setTipofreq("FALTA");
    f1.setCodfreq(101);
    f1.setDtini(LocalDate.of(2026, 5, 2));
    f1.setDtfim(LocalDate.of(2026, 5, 3));
    f1.setSituacao("HOMOLOGADO");

    Frequencia f2 = new Frequencia();
    f2.setId(611L);
    f2.setNumfunc(1001L);
    f2.setNumvinc(1);
    f2.setTipofreq("FALTA");
    f2.setCodfreq(101);
    f2.setDtini(LocalDate.of(2026, 5, 4));
    f2.setDtfim(LocalDate.of(2026, 5, 5));
    f2.setSituacao("HOMOLOGADO");

    FrequenciaRuleContext multiHopCtx =
        new FrequenciaRuleContext(
            baseFreq.draft(),
            baseFreq.vinculo(),
            baseFreq.pessoa(),
            baseFreq.codigoFrequencia(),
            baseFreq.effectiveStatutoryRule(),
            List.of(f1, f2),
            List.of());
    RuleEvaluationResult multiHopResult = facade.evaluateFrequencia(multiHopCtx);
    assertThat(multiHopResult.decision()).isEqualTo(RuleDecision.DENY);
    assertThat(multiHopResult.reasonCodes()).contains("ERG-00444");

    // 2. DocumentoLegal de outro órgão (empCodigo = 2 em frequência do empCodigo = 1) -> DENY ERG-00445
    FrequenciaRuleContext crossOrgDocFreq = validFrequenciaContext();
    crossOrgDocFreq.effectiveStatutoryRule().setDocumentoLegalId(91L);
    br.com.praxis.ergonshowcase.domain.DocumentoLegal wrongOrgDoc =
        new br.com.praxis.ergonshowcase.domain.DocumentoLegal();
    wrongOrgDoc.setId(91L);
    wrongOrgDoc.setEmpCodigo(2);
    crossOrgDocFreq.draft().setDocumentoLegal(wrongOrgDoc);
    RuleEvaluationResult docFreqResult = facade.evaluateFrequencia(crossOrgDocFreq);
    assertThat(docFreqResult.decision()).isEqualTo(RuleDecision.DENY);
    assertThat(docFreqResult.reasonCodes()).containsExactly("ERG-00445");

    // 3. DocumentoLegal de outro órgão em Afastamento acima do limite de dispensa -> DENY ERG-07513
    AfastamentoRuleContext crossOrgDocAfast = validAfastamentoContext();
    crossOrgDocAfast.draft().setDtini(LocalDate.of(2026, 3, 1));
    crossOrgDocAfast.draft().setDtprevfim(LocalDate.of(2026, 3, 25)); // 25 dias > 15
    crossOrgDocAfast.draft().setQuantidadeDias(25);
    crossOrgDocAfast.draft().setDocumentoLegalId(91L);
    crossOrgDocAfast.draft().setDocumentoLegal(wrongOrgDoc);
    RuleEvaluationResult docAfastResult = facade.evaluateAfastamento(crossOrgDocAfast);
    assertThat(docAfastResult.decision()).isEqualTo(RuleDecision.DENY);
    assertThat(docAfastResult.reasonCodes()).containsExactly("ERG-07513");
  }

  static FrequenciaRuleContext validFrequenciaContext() {
    Vinculo vinculo = validVinculo();
    ServidorPessoa pessoa = validPessoa();

    CodigoFrequencia codigo = new CodigoFrequencia();
    codigo.setId(new CodigoFrequenciaId("FALTA", 101));
    codigo.setNome("Falta Abonada");
    codigo.setViaFrequencia(true);
    codigo.setAfastamento(false);
    codigo.setHoraEntradaSaida(false);
    codigo.setRegrasFreq(true);
    codigo.setSexo("A");
    codigo.setQtdmin(BigDecimal.ONE);
    codigo.setQtdmax(BigDecimal.valueOf(5));

    RegraFrequenciaEstatutaria rule = validStatutoryRule("FALTA", 101);
    rule.setDocumentoLegalId(null);

    Frequencia draft = new Frequencia();
    draft.setId(700L);
    draft.setEmpCodigo(1);
    draft.setNumfunc(1001L);
    draft.setNumvinc(1);
    draft.setTipofreq("FALTA");
    draft.setCodfreq(101);
    draft.setDtini(LocalDate.of(2026, 5, 10));
    draft.setDtfim(LocalDate.of(2026, 5, 10));
    draft.setQuantidade(BigDecimal.ONE);
    draft.setSituacao("PENDENTE_HOMOLOGACAO");
    draft.setDocumentoLegalId(91L);

    return new FrequenciaRuleContext(
        draft, vinculo, pessoa, codigo, rule, List.of(), List.of(), LocalDate.of(2026, 5, 15));
  }

  static AfastamentoRuleContext validAfastamentoContext() {
    Vinculo vinculo = validVinculo();
    ServidorPessoa pessoa = validPessoa();

    CodigoFrequencia codigo = new CodigoFrequencia();
    codigo.setId(new CodigoFrequenciaId("LICENCA", 201));
    codigo.setNome("Licença para Tratamento de Saúde");
    codigo.setViaFrequencia(false);
    codigo.setAfastamento(true);
    codigo.setHoraEntradaSaida(false);
    codigo.setRegrasFreq(true);
    codigo.setSexo("A");
    codigo.setQtdmin(BigDecimal.ONE);
    codigo.setQtdmax(BigDecimal.valueOf(90));

    RegraFrequenciaEstatutaria rule = validStatutoryRule("LICENCA", 201);
    rule.setQtdMaxDias(BigDecimal.valueOf(60));
    rule.setQtdMaxConsecMes(31);
    rule.setQtdMaxInterpMes(31);
    rule.setQtdMaxConsecAno(90);
    rule.setQtdMaxInterpAno(90);
    rule.setDocumentoLegalId(null);

    Afastamento draft = new Afastamento();
    draft.setId(500L);
    draft.setEmpCodigo(1);
    draft.setNumfunc(1001L);
    draft.setNumvinc(1);
    draft.setTipofreq("LICENCA");
    draft.setCodfreq(201);
    draft.setDtini(LocalDate.of(2026, 3, 1));
    draft.setDtprevfim(LocalDate.of(2026, 3, 10));
    draft.setQuantidadeDias(10);
    draft.setProrrogacao(false);
    draft.setSituacao("PENDENTE_HOMOLOGACAO");
    draft.setDocumentoLegalId(91L);
    draft.setAtoPublicacaoId(92L);
    draft.setMotivo("Licença médica homologada");

    return new AfastamentoRuleContext(
        draft, vinculo, pessoa, codigo, rule, List.of(), List.of(), null, null);
  }

  @Test
  void prorrogacaoNoMesmoRegistroPrecisaAmpliarAPrevisaoVigente() {
    var base = validAfastamentoContext();
    var draft = base.draft();
    draft.setSituacao("HOMOLOGADO");
    draft.setProrrogacao(true);
    var previsaoVigente = draft.getDtprevfim();
    var context = new AfastamentoRuleContext(draft, base.vinculo(), base.pessoa(),
        base.codigoFrequencia(), base.effectiveStatutoryRule(), base.existingAfastamentos(),
        base.activeFrequencias(), null, null, true, previsaoVigente);

    ErgonFailureAssertions.assertViolation("CERG-04319", DecisionSource.PRODUCT,
        () -> facade.enforceAfastamento(context));
    draft.setDtprevfim(previsaoVigente.plusDays(1));
    draft.setQuantidadeDias(11);
    assertThat(facade.enforceAfastamento(context).decision()).isEqualTo(RuleDecision.ALLOW);
  }

  private static Vinculo validVinculo() {
    Vinculo vinculo = new Vinculo();
    vinculo.setId(new VinculoId(1001L, 1));
    vinculo.setEmpCodigo(1);
    vinculo.setMatric("MAT-1001");
    vinculo.setRegimejur("ESTATUTARIO");
    vinculo.setTipovinc("EFETIVO");
    vinculo.setCategoria("GERAL");
    vinculo.setSubcategoria("PADRAO");
    vinculo.setDtexerc(LocalDate.of(2020, 1, 10));
    vinculo.setSituacao("ATIVO");
    return vinculo;
  }

  private static ServidorPessoa validPessoa() {
    ServidorPessoa pessoa = new ServidorPessoa();
    pessoa.setNumero(1001L);
    pessoa.setNome("Servidor Teste");
    pessoa.setSexo("M");
    return pessoa;
  }

  private static RegraFrequenciaEstatutaria validStatutoryRule(String tipofreq, Integer codfreq) {
    RegraFrequenciaEstatutaria rule = new RegraFrequenciaEstatutaria();
    rule.setId(10L);
    rule.setEmpCodigo(1);
    rule.setRegimeJuridico("ESTATUTARIO");
    rule.setTipoVinculo("EFETIVO");
    rule.setCategoria("GERAL");
    rule.setSubcategoria("PADRAO");
    rule.setTipofreq(tipofreq);
    rule.setCodfreq(codfreq);
    rule.setDtini(LocalDate.of(2024, 1, 1));
    rule.setDtfim(null);
    rule.setQtdMinDias(BigDecimal.ONE);
    rule.setQtdMaxDias(BigDecimal.valueOf(5));
    rule.setIntervMinDias(15);
    rule.setIntProrrogDias(10);
    rule.setQtdMaxConsecMes(5);
    rule.setQtdMaxInterpMes(5);
    rule.setQtdMaxConsecAno(30);
    rule.setQtdMaxInterpAno(30);
    rule.setExigeHomologacao(true);
    rule.setPermiteConcomitancia(false);
    rule.setNaoSeAplica(false);
    rule.setAtiva(true);
    rule.setRuleParams(Map.of("prorrogacao", "S"));
    return rule;
  }
}
