package br.com.praxis.ergonshowcase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.praxis.ergonshowcase.config.TenantContext;
import br.com.praxis.ergonshowcase.domain.Cargo;
import br.com.praxis.ergonshowcase.domain.ServidorPessoa;
import br.com.praxis.ergonshowcase.rules.ErgonRuleEngineFacade;
import br.com.praxis.ergonshowcase.rules.ErgonRuleViolationException;
import br.com.praxis.ergonshowcase.dto.CargoResponseDTO;
import br.com.praxis.ergonshowcase.dto.ExtinguirCargoDTO;
import br.com.praxis.ergonshowcase.mapper.CargoMapper;
import br.com.praxis.ergonshowcase.service.ErgonCommands;
import br.com.praxis.ergonshowcase.service.ErgonInvariants;
import br.com.praxis.ergonshowcase.service.ErgonReadFacts;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.time.LocalDate;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.praxisplatform.rules.contract.DecisionSource;
import org.praxisplatform.rules.contract.RuleDecision;
import org.praxisplatform.uischema.concurrency.ResourceVersionUpdatePrecondition;
import org.springframework.test.util.ReflectionTestUtils;

class CadastroPessoalRuleSetTest {
  private static final LocalDate HOJE = LocalDate.of(2026, 11, 3);
  private final ErgonRuleEngineFacade rules = RuleCatalogExecution.facade();

  @ParameterizedTest
  @MethodSource("rejeicoesCargo")
  void cargoRejeitaCadaViolacaoComCodigoProprio(String code, Consumer<Cargo> invalid,
      Cargo parent, long ocupadas, long dependentes, boolean extinguindo) {
    Cargo cargo = cargoValido();
    invalid.accept(cargo);
    assertThatThrownBy(() -> rules.enforceCargoQuadro(cargo, parent, ocupadas, dependentes, extinguindo))
        .isInstanceOfSatisfying(ErgonRuleViolationException.class,
            error -> assertThat(error.getReasonCodes()).contains(code));
  }

  static Stream<Arguments> rejeicoesCargo() {
    Cargo paiInativo = new Cargo();
    Cargo paiAtivo = new Cargo();
    paiAtivo.setEAglutinador(true);
    return Stream.of(
        Arguments.of("CERG-02101", (Consumer<Cargo>) c -> c.setVagasAutorizadas(0),
            null, 0L, 0L, false),
        Arguments.of("CERG-02102", (Consumer<Cargo>) c -> c.setVagasAutorizadas(1),
            null, 2L, 0L, false),
        Arguments.of("CERG-02103", (Consumer<Cargo>) c -> c.setEAglutinador(false),
            null, 0L, 1L, false),
        Arguments.of("CERG-02104", (Consumer<Cargo>) c -> c.setCargoAglutinador(c.getCargo()),
            null, 0L, 0L, false),
        Arguments.of("CERG-02105", (Consumer<Cargo>) c -> c.setCargoAglutinador(2),
            paiInativo, 0L, 0L, false),
        Arguments.of("CERG-02106", (Consumer<Cargo>) c -> c.setDtExtincao(HOJE.minusDays(1)),
            null, 0L, 0L, false),
        Arguments.of("CERG-02107", (Consumer<Cargo>) c -> c.setDtExtincao(HOJE),
            null, 1L, 0L, true),
        Arguments.of("CERG-02109", (Consumer<Cargo>) c -> {
          c.setEAglutinador(true);
          c.setCargoAglutinador(2);
        }, paiAtivo, 0L, 0L, false));
  }

  @Test
  void orgao2ExigeDocumentoNaManutencaoEExtincaoDoCargo() {
    Cargo cargo = cargoValido();
    for (boolean extinguindo : new boolean[] {false, true}) {
      cargo.setDocumentoLegalId(null);
      cargo.setEmpCodigo(1);
      assertThat(rules.enforceCargoQuadro(cargo, null, 0, 0, extinguindo).decision())
          .isEqualTo(RuleDecision.ALLOW);
      cargo.setEmpCodigo(2);
      assertThatThrownBy(() -> rules.enforceCargoQuadro(cargo, null, 0, 0, extinguindo))
          .isInstanceOfSatisfying(ErgonRuleViolationException.class, error -> {
            assertThat(error.getReasonCodes()).containsExactly("CERG-02108");
            assertThat(error.getViolations().getFirst().source()).isEqualTo(DecisionSource.CUSTOMER);
          });
      cargo.setDocumentoLegalId(12L);
      assertThat(rules.enforceCargoQuadro(cargo, null, 0, 0, extinguindo).decision())
          .isEqualTo(RuleDecision.ALLOW);
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  void comandoDeExtincaoUsaOPlanoDoOrgao() {
    var em = mock(EntityManager.class);
    TypedQuery<Long> contagem = mock(TypedQuery.class, org.mockito.Mockito.RETURNS_SELF);
    when(em.createQuery(anyString(), eq(Long.class))).thenReturn(contagem);
    when(contagem.getSingleResult()).thenReturn(0L);
    var tenant = mock(TenantContext.class);
    var invariants = new ErgonInvariants();
    ReflectionTestUtils.setField(invariants, "em", em);
    ReflectionTestUtils.setField(invariants, "ruleEngineFacade", rules);
    var commands = new ErgonCommands();
    ReflectionTestUtils.setField(commands, "em", em);
    ReflectionTestUtils.setField(commands, "tenant", tenant);
    ReflectionTestUtils.setField(commands, "rules", invariants);
    ReflectionTestUtils.setField(commands, "cargos", new CargoMapper());
    var readFacts = mock(ErgonReadFacts.class);
    when(readFacts.enrichOne(any(CargoResponseDTO.class))).thenAnswer(call -> call.getArgument(0));
    ReflectionTestUtils.setField(commands, "readFacts", readFacts);
    ResourceVersionUpdatePrecondition<Integer> condition = mock(ResourceVersionUpdatePrecondition.class);

    Cargo cargo = cargoValido();
    cargo.setEmpCodigo(2);
    when(em.find(Cargo.class, cargo.getCargo())).thenReturn(cargo);
    when(tenant.orgao()).thenReturn(2);
    var dto = new ExtinguirCargoDTO(HOJE);
    assertThatThrownBy(() -> commands.extinguirCargo(cargo.getCargo(), dto, condition))
        .isInstanceOfSatisfying(ErgonRuleViolationException.class,
            error -> assertThat(error.getReasonCodes()).containsExactly("CERG-02108"));

    cargo.setDtExtincao(null);
    var documento = new br.com.praxis.ergonshowcase.domain.DocumentoLegal();
    documento.setId(12L);
    documento.setEmpCodigo(2);
    when(em.find(br.com.praxis.ergonshowcase.domain.DocumentoLegal.class, 12L)).thenReturn(documento);
    commands.extinguirCargo(cargo.getCargo(), new ExtinguirCargoDTO(HOJE, 12L), condition);
    assertThat(cargo.getDocumentoLegalId()).isEqualTo(12L);
    assertThat(cargo.getDtExtincao()).isEqualTo(HOJE);
  }

  @ParameterizedTest
  @MethodSource("rejeicoesPessoa")
  void pessoaRejeitaCadaViolacaoComCodigoProprio(String code, Consumer<ServidorPessoa> invalid,
      long outrosComCpf) {
    ServidorPessoa pessoa = pessoaValida();
    invalid.accept(pessoa);
    assertThatThrownBy(() -> rules.enforceServidorPessoaCadastro(pessoa, 1, outrosComCpf, HOJE))
        .isInstanceOfSatisfying(ErgonRuleViolationException.class,
            error -> assertThat(error.getReasonCodes()).contains(code));
  }

  static Stream<Arguments> rejeicoesPessoa() {
    return Stream.of(
        Arguments.of("CERG-02201", (Consumer<ServidorPessoa>) p -> p.setDtnasc(HOJE.plusDays(1)), 0L),
        Arguments.of("ERG-00235", (Consumer<ServidorPessoa>) p -> p.setDtnasc(HOJE.minusYears(10).plusDays(1)), 0L),
        Arguments.of("ERG-00236", (Consumer<ServidorPessoa>) p -> p.setDtnasc(HOJE.minusYears(100).minusDays(1)), 0L),
        Arguments.of("ERG-00241", (Consumer<ServidorPessoa>) p -> p.setAnoprimemp(1970), 0L),
        Arguments.of("ERG-00242", (Consumer<ServidorPessoa>) p -> p.setAnoprimemp(HOJE.getYear() + 1), 0L),
        Arguments.of("CERG-02203", (Consumer<ServidorPessoa>) p -> p.setExpedrg(p.getDtnasc().minusDays(1)), 0L),
        Arguments.of("CERG-02204", (Consumer<ServidorPessoa>) p -> p.setIdentprof("REG-1"), 0L),
        Arguments.of("CERG-02205", (Consumer<ServidorPessoa>) p -> {}, 1L));
  }

  @Test
  void orgao2ExigePisPasepEProdutoPreservaAsIdadesLimiteInclusivas() {
    ServidorPessoa pessoa = pessoaValida();
    pessoa.setDtnasc(HOJE.minusYears(10));
    assertThat(rules.enforceServidorPessoaCadastro(pessoa, 1, 0, HOJE).decision())
        .isEqualTo(RuleDecision.ALLOW);
    pessoa.setDtnasc(HOJE.minusYears(100));
    assertThat(rules.enforceServidorPessoaCadastro(pessoa, 1, 0, HOJE).decision())
        .isEqualTo(RuleDecision.ALLOW);
    assertThatThrownBy(() -> rules.enforceServidorPessoaCadastro(pessoa, 2, 0, HOJE))
        .isInstanceOfSatisfying(ErgonRuleViolationException.class, error -> {
          assertThat(error.getReasonCodes()).containsExactly("CERG-02206");
          assertThat(error.getViolations().getFirst().source()).isEqualTo(DecisionSource.CUSTOMER);
        });
    pessoa.setPispasep("12345678901");
    assertThat(rules.enforceServidorPessoaCadastro(pessoa, 2, 0, HOJE).decision())
        .isEqualTo(RuleDecision.ALLOW);
  }

  @Test
  void idadeNoUltimoDiaDeFevereiroSegueAddMonthsDoOracle() {
    ServidorPessoa pessoa = pessoaValida();
    pessoa.setDtnasc(LocalDate.of(2014, 2, 28));
    assertThatThrownBy(() ->
        rules.enforceServidorPessoaCadastro(pessoa, 1, 0, LocalDate.of(2024, 2, 28)))
        .isInstanceOfSatisfying(ErgonRuleViolationException.class,
            error -> assertThat(error.getReasonCodes()).containsExactly("ERG-00235"));
    assertThat(rules.enforceServidorPessoaCadastro(pessoa, 1, 0, LocalDate.of(2024, 2, 29)).decision())
        .isEqualTo(RuleDecision.ALLOW);
  }

  @Test
  void aceitaPlanosConfiguradosEmOutrosOrgaosEUsaDataDaCertidao() {
    rules.registerOrgaoCargoQuadroPlan(7,
        br.com.praxis.ergonshowcase.rules.CargoQuadroRuleSet.orgao2CustomizedRuleSet());
    rules.registerOrgaoServidorPessoaCadastroPlan(7,
        br.com.praxis.ergonshowcase.rules.ServidorPessoaCadastroRuleSet.orgao2CustomizedRuleSet());
    Cargo cargo = cargoValido();
    cargo.setEmpCodigo(7);
    assertThatThrownBy(() -> rules.enforceCargoQuadro(cargo, null, 0, 0, true))
        .isInstanceOfSatisfying(ErgonRuleViolationException.class,
            error -> assertThat(error.getReasonCodes()).containsExactly("CERG-02108"));

    var options = new br.com.praxis.ergonshowcase.config.ServidorPessoaAgeOptions();
    options.setPorOrgao(java.util.Map.of(7,
        new br.com.praxis.ergonshowcase.config.ServidorPessoaAgeOptions.Limites(18, 90)));
    ReflectionTestUtils.setField(rules, "servidorAgeOptions", options);
    ServidorPessoa pessoa = pessoaValida();
    pessoa.setDtnasc(HOJE.minusYears(17));
    assertThatThrownBy(() -> rules.enforceServidorPessoaCadastro(pessoa, 7, 0, HOJE))
        .isInstanceOfSatisfying(ErgonRuleViolationException.class,
            error -> assertThat(error.getReasonCodes()).containsExactly("ERG-00235"));
    pessoa.setDtnasc(HOJE.minusYears(95));
    pessoa.setDtCertFim(HOJE.minusYears(10));
    assertThatThrownBy(() -> rules.enforceServidorPessoaCadastro(pessoa, 7, 0, HOJE))
        .isInstanceOfSatisfying(ErgonRuleViolationException.class,
            error -> assertThat(error.getReasonCodes()).containsExactly("CERG-02206"));
    pessoa.setPispasep("12345678901");
    assertThat(rules.enforceServidorPessoaCadastro(pessoa, 7, 0, HOJE).decision())
        .isEqualTo(RuleDecision.ALLOW);
    pessoa.setDtCertFim(null);
    assertThatThrownBy(() -> rules.enforceServidorPessoaCadastro(pessoa, 7, 0, HOJE))
        .isInstanceOfSatisfying(ErgonRuleViolationException.class,
            error -> assertThat(error.getReasonCodes()).containsExactly("ERG-00236"));
  }

  static Cargo cargoValido() {
    Cargo cargo = new Cargo();
    cargo.setCargo(1);
    cargo.setVersion(1L);
    cargo.setEmpCodigo(1);
    cargo.setControleVaga(true);
    cargo.setVagasAutorizadas(5);
    cargo.setDtInicioContrVaga(HOJE);
    return cargo;
  }

  static ServidorPessoa pessoaValida() {
    ServidorPessoa pessoa = new ServidorPessoa();
    pessoa.setDtnasc(HOJE.minusYears(40));
    return pessoa;
  }
}
