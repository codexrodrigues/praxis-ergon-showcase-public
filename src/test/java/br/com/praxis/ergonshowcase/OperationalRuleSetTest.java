package br.com.praxis.ergonshowcase;

import static br.com.praxis.ergonshowcase.rules.OperationalRuleSet.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.praxis.ergonshowcase.rules.ErgonRuleEngineFacade;
import br.com.praxis.ergonshowcase.rules.ErgonRuleErrorCatalog;
import br.com.praxis.ergonshowcase.rules.ErgonRuleViolationException;
import br.com.praxis.ergonshowcase.rules.FeriasGozoRuleSet;
import br.com.praxis.ergonshowcase.rules.OperationalRuleSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.praxisplatform.rules.contract.RuleDecision;
import org.praxisplatform.rules.contract.RuleSetDefinition;
import org.springframework.test.util.ReflectionTestUtils;

class OperationalRuleSetTest {
  private static final LocalDate INICIO = LocalDate.of(2026, 1, 1);
  private static final LocalDate FIM = LocalDate.of(2026, 1, 31);
  private final ErgonRuleEngineFacade rules = RuleCatalogExecution.facade();

  record Case(OperationalRuleSet rule, String code, Object[] invalid, Object[] valid) {
    @Override public String toString() { return code; }
  }

  @Test
  void everyOperationalRuleHasOneContractCase() {
    assertThat(cases().map(Case::rule).toList()).containsExactlyInAnyOrder(OperationalRuleSet.values());
    assertThat(cases().map(Case::code).toList()).doesNotHaveDuplicates();
  }

  @Test
  void previewsReuseCanonicalProductCodes() {
    rejects("CERG-02105", () -> rules.enforceCargoAglutinadorPreview(1, 42, false));
    rejects("ERG-04306", () -> rules.enforceCodigoAfastamentoPreview(1, "F", 1, false));
    rejects("ERG-00400", () -> rules.enforceCodigoFrequenciaPreview(1, "F", 1, false));
    rejects("ERG-04313", () -> rules.enforceAfastamentoPreviewDates(1, FIM, INICIO));
    rejects("ERG-00415", () -> rules.enforceFrequenciaPreviewDates(1, FIM, INICIO));
    rejects("ERG-01851", () -> rules.enforcePeriodoFeriasPreviewDates(1, FIM, INICIO));
    rules.enforceCargoAglutinadorPreview(1, 42, true);
    rules.enforceCodigoAfastamentoPreview(1, "F", 1, true);
    rules.enforceCodigoFrequenciaPreview(1, "F", 1, true);
    rules.enforceAfastamentoPreviewDates(1, INICIO, FIM);
    rules.enforceFrequenciaPreviewDates(1, INICIO, FIM);
    rules.enforcePeriodoFeriasPreviewDates(1, INICIO, FIM);
  }

  @Test
  void vacationPreviewSelectsChronologyWhenEmploymentBindingComesFirst() {
    var product = FeriasGozoRuleSet.productRuleSet();
    var bindings = new ArrayList<>(product.bindings());
    var employmentAndFrequency = bindings.stream()
        .filter(binding -> "product.ferias-gozo.valid-ferias-vinculo-frequencia".equals(binding.bindingKey()))
        .findFirst().orElseThrow();
    assertThat(employmentAndFrequency.falseReasonCode()).isEqualTo("VALID_FERIAS");
    bindings.remove(employmentAndFrequency);
    bindings.addFirst(employmentAndFrequency);

    var facade = RuleCatalogExecution.facade();
    ReflectionTestUtils.setField(facade, "defaultProductFeriasGozoPlan",
        new RuleSetDefinition(product.ref(), product.availableRoots(), product.slots(), bindings,
            product.compatibility(), product.failPolicy()));

    assertThatThrownBy(() -> facade.enforceGozoFeriasPreviewDates(1, FIM, INICIO, -29))
        .isInstanceOfSatisfying(ErgonRuleViolationException.class, ex ->
            assertThat(ex.getViolations()).singleElement().satisfies(violation -> {
              assertThat(violation.bindingKey()).isEqualTo("product.ferias-gozo.valid-ferias-cronologia");
              assertThat(violation.code()).isEqualTo("VALID_FERIAS");
            }));
    facade.enforceGozoFeriasPreviewDates(1, INICIO, FIM, 31);
  }

  private static void rejects(String code, Runnable evaluate) {
    assertThatThrownBy(evaluate::run).isInstanceOfSatisfying(ErgonRuleViolationException.class,
        ex -> assertThat(ex.getReasonCodes()).containsExactly(code));
  }

  @ParameterizedTest
  @MethodSource("cases")
  void enforcesExactlyOneCataloguedDecision(Case c) {
    var descriptor = ErgonRuleErrorCatalog.find(c.code()).orElseThrow();
    assertThatThrownBy(() -> rules.enforceOperational(c.rule(), 1, c.invalid()))
        .isInstanceOfSatisfying(ErgonRuleViolationException.class, ex ->
            assertThat(ex.getViolations()).singleElement().satisfies(violation -> {
              assertThat(violation.code()).isEqualTo(c.code());
              assertThat(violation.slotKey()).isEqualTo(descriptor.slotKey());
              assertThat(violation.decision()).isEqualTo(RuleDecision.DENY);
            }));
    assertThat(rules.enforceOperational(c.rule(), 2, c.valid()).decision())
        .isEqualTo(RuleDecision.ALLOW);
  }

  static Stream<Case> cases() {
    return Stream.of(
        new Case(CAMPO_SELECT, "CERG-09051",
            new Object[] {"SELECT", null},
            new Object[] {"SELECT", "TABELA"}),
        new Case(CAMPO_ORDEM, "CERG-09002",
            new Object[] {0},
            new Object[] {999}),
        new Case(FERIAS_VINCULO, "CERG-09003",
            new Object[] {1L, 2L, 1, 1},
            new Object[] {1L, 1L, 1, 1}),
        new Case(PREVIA_FRACAO_ABONO, "CERG-09009",
            new Object[] {4, 3},
            new Object[] {3, 3}),
        new Case(LICENCA_ESTADO_EDICAO, "CERG-09012",
            new Object[] {"PROGRAMADA", "GOZADA"},
            new Object[] {"PROGRAMADA", "PROGRAMADA"}),
        new Case(LICENCA_PERIODO_EDICAO, "CERG-09013",
            new Object[] {1L, 2L},
            new Object[] {1L, 1L}),
        new Case(AVERBACAO_CRIAR, "CERG-09014",
            new Object[] {0, INICIO, null, null, null},
            new Object[] {0, null, null, null, null}),
        new Case(AVERBACAO_ATO_EDICAO, "CERG-09015",
            new Object[] {0, 1, null, null, null, null, null, null, null, null},
            new Object[] {0, 0, null, null, null, null, null, null, null, null}),
        new Case(AVERBACAO_RETIFICACAO, "CERG-09016",
            new Object[] {"HOMOLOGADA", 20, 21, 20, 20, "L", "L", "T", "T", "I", "I", INICIO, INICIO, FIM, FIM},
            new Object[] {"EM_ANALISE", 20, 21, 20, 20, "L", "L", "T", "T", "I", "I", INICIO, INICIO, FIM, FIM}),
        new Case(GOZO_CRIAR, "CERG-09017",
            new Object[] {FIM},
            new Object[] {null}),
        new Case(GOZO_ENCERRAMENTO_EDICAO, "CERG-09018",
            new Object[] {null, FIM},
            new Object[] {null, null}),
        new Case(GOZO_RETIFICACAO, "CERG-09019",
            new Object[] {"GOZADA", INICIO, INICIO, FIM, FIM, 10, 11, false, false, 0, 0, null, null},
            new Object[] {"PROGRAMADA", INICIO, INICIO, FIM, FIM, 10, 11, false, false, 0, 0, null, null}),
        new Case(PERIODO_CRIAR, "CERG-09020",
            new Object[] {1, null},
            new Object[] {0, null}),
        new Case(PERIODO_CONVERSAO_EDICAO, "CERG-09021",
            new Object[] {0, 1, null, null},
            new Object[] {0, 0, null, null}),
        new Case(PERIODO_VINCULO_ORIGEM, "CERG-09022",
            new Object[] {1L, 1L, 1, 2},
            new Object[] {1L, 1L, 1, 1}),
        new Case(PERIODO_NOVA_ORIGEM, "CERG-09023",
            new Object[] {null, 1L, "EM_ANALISE", null, "LICENCA_ESPECIAL"},
            new Object[] {null, 1L, "HOMOLOGADA", null, "LICENCA_ESPECIAL"}),
        new Case(VENDA_CRIAR_DATAS, "CERG-09024",
            new Object[] {null, FIM},
            new Object[] {null, null}),
        new Case(VENDA_CRIAR_MODALIDADE, "CERG-09025",
            new Object[] {1L, "ABONO_JUNTO_GOZO"},
            new Object[] {null, "PECUNIA"}),
        new Case(VENDA_DATAS_EDICAO, "CERG-09026",
            new Object[] {null, INICIO, null, null},
            new Object[] {null, null, null, null}),
        new Case(VENDA_ABONO_ORIGEM, "ERG-03387",
            new Object[] {1L, "excluir", null, null, null, null, null, null, null, null},
            new Object[] {1L, "liquidacao", INICIO, INICIO, 2, 2, 7L, 7L, null, null}),
        new Case(VENDA_PERIODO_ORIGEM, "CERG-09027",
            new Object[] {1L, 2L},
            new Object[] {1L, 1L}),
        new Case(VENDA_MODALIDADE_ORIGEM, "CERG-09028",
            new Object[] {"PECUNIA"},
            new Object[] {"ABONO_JUNTO_GOZO"}),
        new Case(VENDA_DIAS_VALOR, "CERG-09029",
            new Object[] {1L, "AUTORIZADA", 1, -1},
            new Object[] {1L, "AUTORIZADA", 1, 0}),
        new Case(VENDA_PATCH_ESTORNO, "CERG-09030",
            new Object[] {"ESTORNADA"},
            new Object[] {"AUTORIZADA"}),
        new Case(VENDA_RECONCILIAR_ORIGEM, "CERG-09031",
            new Object[] {null},
            new Object[] {1L}),
        new Case(VENDA_RECONCILIAR_DATA, "CERG-09032",
            new Object[] {FIM, INICIO},
            new Object[] {FIM, FIM}),
        new Case(VENDA_RECONCILIAR_GOZO, "CERG-09033",
            new Object[] {"CANCELADO", true},
            new Object[] {"HOMOLOGADA", true}),
        new Case(VENDA_RETIFICACAO, "CERG-09034",
            new Object[] {"PAGA", 1, 2, null, null, null, null, null, null},
            new Object[] {"AUTORIZADA", 1, 2, null, null, null, null, null, null}),
        new Case(PERIODO_SALDO_CONTABIL, "CERG-09035",
            new Object[] {30, 20L, 10L, 1},
            new Object[] {30, 20L, 10L, 0}),
        new Case(GOZO_ABONO_UNICO, "CERG-09036",
            new Object[] {2L},
            new Object[] {1L}),
        new Case(AVERBACAO_REDISTRIBUICAO, "CERG-09037",
            new Object[] {2},
            new Object[] {1}),
        new Case(PREVIA_AVERBACAO, "CERG-09038",
            new Object[] {20, 21},
            new Object[] {20, 20}),
        new Case(PREVIA_CONVERSAO, "CERG-09039",
            new Object[] {20, 21},
            new Object[] {20, 20}),
        new Case(PREVIA_GOZO_INTERVALO, "CERG-09040",
            new Object[] {FIM, INICIO},
            new Object[] {INICIO, FIM}),
        new Case(PREVIA_GOZO_LIMITE, "CERG-09041",
            new Object[] {1000L},
            new Object[] {999L}),
        new Case(PREVIA_VENDA_SALDO_APURADO, "CERG-09042",
            new Object[] {null},
            new Object[] {0}),
        new Case(PREVIA_VENDA_CONSUMO, "CERG-09043",
            new Object[] {0},
            new Object[] {1}),
        new Case(PREVIA_VENDA_LIMITE, "CERG-09044",
            new Object[] {11, 10},
            new Object[] {10, 10}));
  }
}
