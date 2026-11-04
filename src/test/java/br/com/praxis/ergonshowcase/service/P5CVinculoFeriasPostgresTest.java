package br.com.praxis.ergonshowcase.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.praxis.ergonshowcase.config.TenantContext;
import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.controller.FeriasGozoController;
import br.com.praxis.ergonshowcase.controller.PeriodoAquisitivoFeriasController;
import br.com.praxis.ergonshowcase.domain.*;
import br.com.praxis.ergonshowcase.mapper.FeriasGozoMapper;
import br.com.praxis.ergonshowcase.mapper.PeriodoAquisitivoFeriasMapper;
import br.com.praxis.ergonshowcase.repository.FeriasGozoRepository;
import br.com.praxis.ergonshowcase.repository.PeriodoAquisitivoFeriasRepository;
import br.com.praxis.ergonshowcase.rules.ProvaHorariosAcumulacao;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.sql.SQLException;
import javax.sql.DataSource;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.praxisplatform.uischema.concurrency.ResourceVersionEtagService;
import org.praxisplatform.uischema.concurrency.ResourceVersionScope;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** PostgreSQL descartável, controllers, serviços, JPA e transações reais. */
class P5CVinculoFeriasPostgresTest {
  private final ObjectMapper json = new ObjectMapper().findAndRegisterModules()
      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  private final ResourceVersionEtagService etags = new ResourceVersionEtagService("p5c-persistence-test-secret");
  private final ResourceVersionScope scope = new ResourceVersionScope("ergon|org:1");

  @Test
  void persistsOfficialValuesAcrossPostAndPutAndReadsOnlyAuthorizedAccumulationFacts() throws Exception {
    try (var postgres = EmbeddedPostgres.builder().setServerConfig("listen_addresses", "127.0.0.1")
        .setPort(0).start()) {
      var factory = new LocalContainerEntityManagerFactoryBean();
      factory.setDataSource(postgres.getPostgresDatabase());
      factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
      factory.setPackagesToScan("br.com.praxis.ergonshowcase.domain");
      factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "create-drop",
          "hibernate.hbm2ddl.halt_on_error", "true"));
      factory.afterPropertiesSet();
      try {
        var emf = factory.getObject();
        var em = SharedEntityManagerCreator.createSharedEntityManager(emf);
        var tx = new TransactionTemplate(new JpaTransactionManager(emf));
        tx.executeWithoutResult(ignored -> seed(em));
        proveAccumulationRead(tx, em, postgres.getPostgresDatabase());
        proveFeriasWrites(tx, em);
      } finally {
        factory.destroy();
      }
    }
  }

  private void proveAccumulationRead(TransactionTemplate tx, EntityManager em, DataSource dataSource) {
    var context = SecurityContextHolder.createEmptyContext();
    var previous = SecurityContextHolder.getContext();
    SecurityContextHolder.setContext(context);
    try {
      var draft = vinculo(1, 1, "GERAL");
      tx.executeWithoutResult(ignored -> {
        context.setAuthentication(new UsernamePasswordAuthenticationToken("operator", "test",
            AuthorityUtils.createAuthorityList("ERGON_WRITE")));
        assertThat(new VinculoAccumulationFacts("1", Optional.empty()).load(em, draft)).isNull();
        context.setAuthentication(new UsernamePasswordAuthenticationToken("operator", "test",
            AuthorityUtils.createAuthorityList("ERGON_WRITE", "ERGON_ACCUMULATION_CHECK")));
        assertThat(new VinculoAccumulationFacts("", Optional.empty()).load(em, draft)).isNull();
        assertThat(new VinculoAccumulationFacts("2", Optional.empty()).load(em, draft)).isNull();
        var facts = new VinculoAccumulationFacts("1", Optional.empty()).load(em, draft);
        assertThat(facts).extracting(f -> f.categoria())
            .containsExactlyInAnyOrder("SAUDE", "MAGISTERIO");
        proveEffectiveSchedules(em, draft);
        assertThatThrownBy(() -> {
          try (var contender = dataSource.getConnection(); var statement = contender.createStatement()) {
            statement.executeQuery("select numero from funcionarios where numero = 1001 for update nowait");
          }
        }).isInstanceOfSatisfying(SQLException.class, error -> assertThat(error.getSQLState()).isEqualTo("55P03"));
      });
    } finally {
      SecurityContextHolder.setContext(previous);
    }
  }

  private void proveEffectiveSchedules(EntityManager em, Vinculo draft) {
    var dia = LocalDate.of(2026, 11, 3);
    draft.setDtexerc(dia);
    draft.setDtvac(dia);
    em.createNativeQuery("""
        create temporary table escala_prova (
          orgao integer, titular bigint, vinculo integer, dia date, inicio timestamptz, fim timestamptz
        ) on commit drop
        """).executeUpdate();
    em.createNativeQuery("""
        insert into escala_prova values
          (1,1001,1,'2026-11-03','2026-11-03T09:00Z','2026-11-03T12:00Z'),
          (2,1001,2,'2026-11-03','2026-11-03T13:00Z','2026-11-03T16:00Z'),
          (2,1001,3,'2026-11-03','2026-11-03T10:00Z','2026-11-03T11:00Z'),
          (2,1001,1,'2026-11-03','2026-11-03T10:00Z','2026-11-03T11:00Z'),
          (1,1001,1,'2026-11-02','2026-11-02T13:00Z','2026-11-02T16:00Z')
        """).executeUpdate();
    VinculoScheduleEvidenceSource fonte = (candidato, outro, inicio, fim) -> {
      assertThat(inicio).isEqualTo(dia);
      assertThat(fim).isEqualTo(dia);
      return Optional.of(new ProvaHorariosAcumulacao(inicio, fim, ZoneId.of("America/Sao_Paulo"),
          Duration.ofMinutes(30), readFixtureSchedule(em, candidato, dia), readFixtureSchedule(em, outro, dia)));
    };
    assertThat(new VinculoAccumulationFacts("1", Optional.of(fonte)).load(em, draft))
        .allSatisfy(fato -> assertThat(fato.horarios()).isNull());
    var fatos = new VinculoAccumulationFacts("1,2", Optional.of(fonte)).load(em, draft);
    assertThat(fatos).allSatisfy(fato -> assertThat(fato.horarios().candidato())
        .extracting(ProvaHorariosAcumulacao.Expediente::inicio).containsExactly(Instant.parse("2026-11-03T09:00:00Z")));
    assertThat(fatos).filteredOn(fato -> "SAUDE".equals(fato.categoria())).singleElement()
        .satisfies(fato -> assertThat(fato.horarios().concomitante())
            .extracting(ProvaHorariosAcumulacao.Expediente::inicio).containsExactly(Instant.parse("2026-11-03T13:00:00Z")));
  }

  private static java.util.List<ProvaHorariosAcumulacao.Expediente> readFixtureSchedule(EntityManager em,
      VinculoScheduleEvidenceSource.Assignment assignment, LocalDate dia) {
    var rows = em.createNativeQuery("""
        select extract(epoch from inicio)::bigint, extract(epoch from fim)::bigint from escala_prova
        where orgao=:orgao and titular=:titular and vinculo=:vinculo and dia=:dia order by inicio
        """).setParameter("orgao", assignment.orgao()).setParameter("titular", assignment.vinculo().numfunc())
        .setParameter("vinculo", assignment.vinculo().numero()).setParameter("dia", dia).getResultList();
    var result = new java.util.ArrayList<ProvaHorariosAcumulacao.Expediente>();
    for (Object row : rows) {
      var values = (Object[]) row;
      result.add(new ProvaHorariosAcumulacao.Expediente(Instant.ofEpochSecond(((Number) values[0]).longValue()),
          Instant.ofEpochSecond(((Number) values[1]).longValue())));
    }
    return result;
  }

  private void proveFeriasWrites(TransactionTemplate tx, EntityManager em) throws Exception {
    var repositories = new JpaRepositoryFactory(em);
    var tenant = new TenantContext();
    var pa = new PeriodoAquisitivoFeriasService(repositories.getRepository(PeriodoAquisitivoFeriasRepository.class),
        new PeriodoAquisitivoFeriasMapper(tenant), tenant);
    var gozo = new FeriasGozoService(repositories.getRepository(FeriasGozoRepository.class),
        new FeriasGozoMapper(tenant), tenant);
    var invariants = new ErgonInvariants();
    ReflectionTestUtils.setField(invariants, "em", em);
    for (Object service : new Object[]{pa, gozo}) {
      ReflectionTestUtils.setField(service, "entityManager", em);
      ReflectionTestUtils.setField(service, "invariants", invariants);
      ReflectionTestUtils.setField(service, "resourceObjectMapper", json);
    }
    var determinations = new ErgonDeterminationService(tenant, null);
    ReflectionTestUtils.setField(determinations, "em", em);
    var periodController = new PeriodoAquisitivoFeriasController(pa, etags, () -> scope);
    var gozoController = new FeriasGozoController(gozo, etags, () -> scope);
    var request = (HttpServletRequest) Proxy.newProxyInstance(HttpServletRequest.class.getClassLoader(),
        new Class<?>[]{HttpServletRequest.class}, (proxy, method, args) -> method.invoke(
            ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest(), args));
    for (Object controller : new Object[]{periodController, gozoController}) {
      ReflectionTestUtils.setField(controller, "determinations", determinations);
      ReflectionTestUtils.setField(controller, "request", request);
      ReflectionTestUtils.invokeMethod(controller, "initializeBasePath");
    }
    var mvc = MockMvcBuilders.standaloneSetup(periodController, gozoController)
        .setMessageConverters(new MappingJackson2HttpMessageConverter(json)).build();
    var periodPath = ApiPaths.PERIODOS_AQUISITIVOS_FERIAS;
    var period = json.createObjectNode().put("empCodigo", 1).put("numfunc", 1001).put("numvinc", 1)
        .put("dtini", "2025-01-01").put("dtfim", "2025-12-31");
    var periodPreview = write(tx, mvc, post(periodPath + "/determinations/previa"), period, status().isOk());
    assertThat(periodPreview.path("totDiasDireito").asInt()).isEqualTo(30);
    period.put("chave", 1).put("exercicio", 2026).put("dtPrescricao", "2028-01-31");
    period.set("diasDireitoPrevistos", periodPreview.get("totDiasDireito"));
    period.set("faltasApuradasPrevistas", periodPreview.get("faltasApuradas"));
    period.set("dataPrescricaoPrevista", periodPreview.get("dtPrescricao"));
    write(tx, mvc, post(periodPath), period, status().isCreated());
    var periodSaved = tx.execute(ignored -> em.createQuery("from PeriodoAquisitivoFerias", PeriodoAquisitivoFerias.class)
        .getSingleResult());
    assertThat(periodSaved.getTotDiasDireito()).isEqualTo(30);
    assertThat(periodSaved.getDtPrescricao()).isEqualTo(LocalDate.of(2028, 1, 31));
    period.put("dtPrescricao", "2028-02-29");
    period.put("diasDireitoPrevistos", 999).put("faltasApuradasPrevistas", 999).put("dataPrescricaoPrevista", "2099-01-01");
    write(tx, mvc, put(periodPath + "/" + periodSaved.getId()).header("If-Match",
        etags.create(scope, "administracao-pessoal.periodos-aquisitivos-ferias", periodSaved.getId(), periodSaved.getVersion())),
        period, status().isOk());
    tx.executeWithoutResult(ignored -> {
      var persisted = em.find(PeriodoAquisitivoFerias.class, periodSaved.getId());
      assertThat(persisted.getTotDiasDireito()).isEqualTo(30);
      assertThat(persisted.getFaltasApuradas()).isZero();
      assertThat(persisted.getDtPrescricao()).isEqualTo(LocalDate.of(2028, 2, 29));
      assertThat(persisted.getVersion()).isGreaterThan(periodSaved.getVersion());
    });

    var gozoPath = ApiPaths.FERIAS_GOZO;
    var parcel = json.createObjectNode().put("dtini", "2026-07-01").put("dtprevfim", "2026-07-10")
        .put("abonoPecuniario", false);
    var parcelPreview = write(tx, mvc, post(gozoPath + "/determinations/previa"), parcel, status().isOk());
    assertThat(parcelPreview.path("diasGozo").asInt()).isEqualTo(10);
    parcel.put("periodoAquisitivoId", periodSaved.getId()).put("ordemParcela", 1).put("diasGozo", 10);
    parcel.set("diasGozoPrevistos", parcelPreview.get("diasGozo"));
    parcel.set("diasAbonoPrevistos", parcelPreview.get("diasAbonoVendidos"));
    write(tx, mvc, post(gozoPath), parcel, status().isCreated());
    var parcelSaved = tx.execute(ignored -> em.createQuery("from FeriasGozo", FeriasGozo.class).getSingleResult());
    assertThat(parcelSaved.getDiasGozo()).isEqualTo(10);
    assertThat(parcelSaved.getDiasAbonoVendidos()).isZero();
    parcel.put("dtprevfim", "2026-07-11").put("diasGozo", 11);
    parcel.put("diasGozoPrevistos", 999).put("diasAbonoPrevistos", 999);
    write(tx, mvc, put(gozoPath + "/" + parcelSaved.getId()).header("If-Match",
        etags.create(scope, "administracao-pessoal.ferias-gozo", parcelSaved.getId(), parcelSaved.getVersion())),
        parcel, status().isOk());
    tx.executeWithoutResult(ignored -> {
      var persisted = em.find(FeriasGozo.class, parcelSaved.getId());
      assertThat(persisted.getDiasGozo()).isEqualTo(11);
      assertThat(persisted.getDiasAbonoVendidos()).isZero();
      assertThat(persisted.getVersion()).isGreaterThan(parcelSaved.getVersion());
    });
  }

  private ObjectNode write(TransactionTemplate tx, MockMvc mvc, MockHttpServletRequestBuilder request,
      ObjectNode body, ResultMatcher expected) {
    return tx.execute(ignored -> {
      try {
        return (ObjectNode) json.readTree(mvc.perform(request.contentType(MediaType.APPLICATION_JSON)
            .content(body.toString())).andExpect(expected).andReturn().getResponse().getContentAsString());
      } catch (Exception e) {
        throw new IllegalStateException("Falha no fluxo HTTP de férias", e);
      }
    });
  }

  private static void seed(EntityManager em) {
    for (int id : new int[]{1, 2}) {
      var org = new Orgao();
      org.setEmpresa(id);
      org.setNome("Órgão de teste " + id);
      org.setRazao(org.getNome());
      org.setCnpj("0000000000000" + id);
      org.setUfSigla("DF");
      org.setMunicipio("Brasília");
      org.setAtiva(true);
      em.persist(org);
    }
    var pessoa = new ServidorPessoa();
    pessoa.setNumero(1001L);
    pessoa.setNome("Titular sintético");
    pessoa.setCpf("00000000000");
    pessoa.setDtnasc(LocalDate.of(1980, 1, 1));
    em.persist(pessoa);
    em.persist(vinculo(1, 1, "GERAL"));
    em.persist(vinculo(2, 2, "SAUDE"));
    em.persist(vinculo(2, 3, "MAGISTERIO"));
  }

  private static Vinculo vinculo(int org, int numero, String categoria) {
    var vinculo = new Vinculo();
    vinculo.setId(new VinculoId(1001L, numero));
    vinculo.setEmpCodigo(org);
    vinculo.setMatric("TESTE-" + numero);
    vinculo.setRegimejur("ESTATUTARIO");
    vinculo.setTipovinc("EFETIVO");
    vinculo.setCategoria(categoria);
    vinculo.setDtexerc(LocalDate.of(2020, 1, 1));
    vinculo.setSituacao("ATIVO");
    return vinculo;
  }
}
