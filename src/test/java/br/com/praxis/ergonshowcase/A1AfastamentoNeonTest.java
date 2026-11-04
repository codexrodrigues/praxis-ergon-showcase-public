package br.com.praxis.ergonshowcase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.praxis.ergonshowcase.domain.VinculoId;
import br.com.praxis.ergonshowcase.dto.EncerrarAfastamentoDTO;
import br.com.praxis.ergonshowcase.dto.EncerrarAfastamentoItemDTO;
import br.com.praxis.ergonshowcase.dto.ProrrogarAfastamentoDTO;
import br.com.praxis.ergonshowcase.rules.ErgonRuleViolationException;
import br.com.praxis.ergonshowcase.service.AfastamentoService;
import br.com.praxis.ergonshowcase.service.ErgonCommands;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.praxisplatform.uischema.concurrency.ResourceVersionEtagService;
import org.praxisplatform.uischema.concurrency.ResourceVersionPreconditionException;
import org.praxisplatform.uischema.concurrency.ResourceVersionUpdatePrecondition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(properties = {"spring.jpa.hibernate.ddl-auto=none",
    "showcase.security.accumulation-orgs=1"})
@WithMockUser(authorities = {"ERGON_READ", "ERGON_WRITE", "ERGON_ACCUMULATION_CHECK"})
@EnabledIfEnvironmentVariable(named = "A1_NEON_PROOF", matches = "true")
class A1AfastamentoNeonTest {
  private static final long AFASTAMENTO = 1L;
  private static final VinculoId VINCULO = new VinculoId(1003L, 1);
  private static final long DOCUMENTO = 7L;
  private static final LocalDate INICIO = LocalDate.of(2026, 3, 10);
  private static final String AFASTAMENTO_KEY = "administracao-pessoal.afastamentos";
  private static final String VINCULO_KEY = "administracao-pessoal.vinculos";

  @Autowired AfastamentoService afastamentos;
  @Autowired ErgonCommands comandos;
  @Autowired JdbcTemplate jdbc;
  @Autowired DataSource dataSource;
  @Autowired PlatformTransactionManager transactions;
  @Autowired ResourceVersionEtagService etags;

  @Test
  void prorrogaEEncerraComDuasTransacoesCompetindoPeloMesmoVinculo() throws Exception {
    String branch = System.getenv("PROOF_NEON_BRANCH");
    String host = System.getenv("BULK_PROOF_EXPECTED_HOST");
    assertThat(branch).matches("br-[a-z0-9-]+");
    assertThat(branch).isNotEqualTo("br-super-sound-b7urizmd");
    assertThat(branch).isEqualTo(System.getenv("BULK_PROOF_DISPOSABLE_BRANCH"));
    assertThat(host).isNotBlank();
    try (var connection = dataSource.getConnection()) {
      assertThat(connection.getMetaData().getURL()).startsWith("jdbc:postgresql://" + host + "/");
    }

    TransactionTemplate tx = new TransactionTemplate(transactions);
    tx.executeWithoutResult(status -> {
      assertThat(jdbc.update("""
          update afastamentos set dtfim=null, dtprevfim=?, quantidade_dias=30, prorrogacao=false,
            afastamento_origem_id=null, situacao='HOMOLOGADO'
          where id=? and numfunc=1003 and numvinc=1 and emp_codigo=1
          """, INICIO.plusDays(29), AFASTAMENTO)).isEqualTo(1);
      assertThat(jdbc.update("""
          update vinculos set situacao='AFASTADO' where numfunc=1003 and numero=1 and emp_codigo=1
          """)).isEqualTo(1);
    });

    assertThatThrownBy(() -> afastamentos.prorrogarAfastamento(AFASTAMENTO,
        new ProrrogarAfastamentoDTO(INICIO.minusDays(1), DOCUMENTO, null), condicaoAfastamento()))
        .isInstanceOfSatisfying(ErgonRuleViolationException.class,
            error -> assertThat(error.getReasonCodes()).contains("ERG-04313"));
    assertThatThrownBy(() -> afastamentos.prorrogarAfastamento(AFASTAMENTO,
        new ProrrogarAfastamentoDTO(INICIO.plusDays(29), DOCUMENTO, null), condicaoAfastamento()))
        .isInstanceOfSatisfying(ErgonRuleViolationException.class,
            error -> assertThat(error.getReasonCodes()).contains("CERG-04319"));

    LocalDate fim = INICIO.plusDays(30);
    afastamentos.prorrogarAfastamento(AFASTAMENTO,
        new ProrrogarAfastamentoDTO(fim, DOCUMENTO, null), condicaoAfastamento());
    assertThatThrownBy(() -> afastamentos.encerrarAfastamento(AFASTAMENTO,
        new EncerrarAfastamentoItemDTO(INICIO.minusDays(1), DOCUMENTO, null), condicaoAfastamento()))
        .isInstanceOfSatisfying(ErgonRuleViolationException.class,
            error -> assertThat(error.getReasonCodes()).contains("ERG-04312"));

    var condicaoAfastamento = condicaoAfastamento();
    Long versaoVinculo = jdbc.queryForObject(
        "select version from vinculos where numfunc=1003 and numero=1", Long.class);
    var condicaoVinculo = condicao(VINCULO_KEY, VINCULO, versaoVinculo);
    var pidConcorrente = new CompletableFuture<Integer>();
    try (var executor = Executors.newSingleThreadExecutor()) {
      var resultadoConcorrente = tx.execute(status -> {
        jdbc.execute("set local lock_timeout='10s'");
        int pidVinculo = jdbc.queryForObject("select pg_backend_pid()", Integer.class);
        jdbc.queryForObject(
            "select numero from vinculos where numfunc=1003 and numero=1 for update", Integer.class);
        var concorrente = executor.submit(() -> {
          try {
            tx.executeWithoutResult(other -> {
              jdbc.execute("set local lock_timeout='10s'");
              pidConcorrente.complete(jdbc.queryForObject("select pg_backend_pid()", Integer.class));
              afastamentos.encerrarAfastamento(AFASTAMENTO,
                  new EncerrarAfastamentoItemDTO(fim, DOCUMENTO, null), condicaoAfastamento);
            });
            return null;
          } catch (RuntimeException rejection) {
            return rejection;
          }
        });
        try {
          aguardaBloqueio(pidConcorrente.get(5, TimeUnit.SECONDS), pidVinculo);
        } catch (Exception failure) {
          throw new AssertionError("Não foi possível observar a disputa entre as transações.", failure);
        }
        try {
          comandos.encerrarAfastamento(VINCULO,
              new EncerrarAfastamentoDTO(AFASTAMENTO, fim, DOCUMENTO), condicaoVinculo);
        } catch (ErgonRuleViolationException rejection) {
          throw new AssertionError("O encerramento pelo vínculo foi rejeitado: "
              + rejection.getReasonCodes(), rejection);
        }
        return concorrente;
      });
      assertThat(resultadoConcorrente.get(15, TimeUnit.SECONDS))
          .isInstanceOf(ResourceVersionPreconditionException.class);
    }
    assertThat(jdbc.queryForObject("select dtfim from afastamentos where id=?",
        LocalDate.class, AFASTAMENTO)).isEqualTo(fim);
    assertThat(jdbc.queryForObject("select situacao from vinculos where numfunc=1003 and numero=1",
        String.class)).isEqualTo("ATIVO");
  }

  private void aguardaBloqueio(int concorrente, int detentor) throws Exception {
    try (var observer = dataSource.getConnection();
         var query = observer.prepareStatement("select ? = any(pg_blocking_pids(?))")) {
      query.setInt(1, detentor);
      query.setInt(2, concorrente);
      long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
      while (System.nanoTime() < limite) {
        try (var rs = query.executeQuery()) {
          if (rs.next() && rs.getBoolean(1)) return;
        }
        Thread.sleep(50);
      }
    }
    throw new AssertionError("O PostgreSQL não registrou a transação concorrente aguardando o lock.");
  }

  private ResourceVersionUpdatePrecondition<Long> condicaoAfastamento() {
    return condicao(AFASTAMENTO_KEY, AFASTAMENTO, jdbc.queryForObject(
        "select version from afastamentos where id=?", Long.class, AFASTAMENTO));
  }

  private <ID> ResourceVersionUpdatePrecondition<ID> condicao(String recurso, ID id, long versao) {
    return new ResourceVersionUpdatePrecondition<>(etags, etags.create(recurso, id, versao), recurso, id);
  }
}
