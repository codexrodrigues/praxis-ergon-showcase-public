package br.com.praxis.ergonshowcase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.praxis.ergonshowcase.config.TenantContext;
import br.com.praxis.ergonshowcase.bulk.ErgonBulkDomain;
import br.com.praxis.ergonshowcase.domain.DesignacaoFuncao;
import br.com.praxis.ergonshowcase.domain.VagaFuncao;
import br.com.praxis.ergonshowcase.domain.RadarChefiaSubstituicao;
import br.com.praxis.ergonshowcase.domain.SubstituicaoFuncao;
import br.com.praxis.ergonshowcase.domain.VinculoId;
import br.com.praxis.ergonshowcase.dto.RadarChefiaSubstituicaoFilterDTO;
import br.com.praxis.ergonshowcase.dto.VagaFuncaoFilterDTO;
import br.com.praxis.ergonshowcase.dto.ChefiaDeterminationDTO.*;
import br.com.praxis.ergonshowcase.dto.UpdateDesignacaoFuncaoDTO;
import br.com.praxis.ergonshowcase.mapper.DesignacaoFuncaoMapper;
import br.com.praxis.ergonshowcase.rules.ErgonRuleEngineFacade;
import br.com.praxis.ergonshowcase.rules.ErgonRuleViolationException;
import br.com.praxis.ergonshowcase.rules.SubstituicaoFuncaoFacts;
import br.com.praxis.ergonshowcase.service.ErgonTenantAccess;
import br.com.praxis.ergonshowcase.service.DesignacaoFuncaoService;
import br.com.praxis.ergonshowcase.service.ErgonDeterminationService;
import br.com.praxis.ergonshowcase.service.VagaFuncaoService;
import br.com.praxis.ergonshowcase.repository.DesignacaoFuncaoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.persistence.Entity;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.junit.jupiter.api.Test;
import org.praxisplatform.rules.contract.RuleDecision;
import org.praxisplatform.uischema.concurrency.ResourceVersionPreconditionException;
import org.praxisplatform.uischema.concurrency.ResourceVersionUpdatePrecondition;
import org.praxisplatform.uischema.filter.specification.GenericSpecificationsBuilder;
import org.praxisplatform.uischema.stats.StatsEligibility;
import org.praxisplatform.uischema.stats.StatsMetric;
import org.praxisplatform.uischema.stats.StatsProperties;
import org.praxisplatform.uischema.stats.StatsSupportMode;
import org.praxisplatform.uischema.stats.dto.GroupByStatsRequest;
import org.praxisplatform.uischema.stats.dto.StatsMetricRequest;
import org.praxisplatform.uischema.stats.service.jpa.JpaStatsQueryExecutor;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.beans.BeanUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.util.ReflectionTestUtils;
import static org.mockito.Mockito.mock;

class ChefiaSlice7SqlTest {
  private static final String URL = "jdbc:h2:mem:slice7;MODE=PostgreSQL";

  @Test
  void schemaAndSeedProduceScopedCoverageAndRecalculateWhenExerciseChanges() throws SQLException {
    try (var connection = DriverManager.getConnection(URL)) {
      initialize(connection);
      seed(connection);

      for (int orgao : List.of(1, 2)) {
        assertThat(column(connection, "select categoria from funcoes_confianca where emp_codigo = " + orgao))
            .containsExactlyInAnyOrder("DIRECAO", "CHEFIA", "ASSESSORAMENTO");
        assertThat(column(connection, "select situacao from vagas_funcao where emp_codigo = " + orgao))
            .containsExactlyInAnyOrder("OCUPADA", "OCUPADA", "OCUPADA", "BLOQUEADA", "LIVRE");
        assertThat(column(connection, "select motivo from substituicoes_funcao where emp_codigo = " + orgao))
            .containsExactlyInAnyOrder("FERIAS", "AFASTAMENTO");
        assertThat(column(connection, """
            select situacao from radar_chefias_substituicoes where emp_codigo =
            """ + orgao)).containsExactlyInAnyOrder("COBERTA", "COBERTA", "DESCOBERTA");
        assertThat(column(connection, """
            select titular_numfunc from radar_chefias_substituicoes
            where situacao = 'DESCOBERTA' and emp_codigo =
            """ + orgao)).containsExactly(Integer.toString(orgao * 100 + 3));
        assertThat(column(connection, """
            select substituto2_numfunc from designacoes_funcao
            where substituto1_numfunc <> substituto2_numfunc and emp_codigo =
            """ + orgao)).hasSize(3);
      }
      assertThat(column(connection, """
          select id from radar_chefias_substituicoes where emp_codigo = 1
          and ferias_gozo_id is not null and afastamento_id is not null
          """)).isEmpty();
      assertScopedJpaQueries(connection);

      execute(connection, """
          update substituicoes_funcao set situacao = 'PENDENTE_FOLHA'
          where motivo = 'FERIAS' and emp_codigo = 1
          """);
      assertThat(column(connection, """
          select pendencias_folha from radar_chefias_substituicoes
          where ferias_gozo_id = 11 and situacao = 'COBERTA'
          """)).containsExactly("1");
      execute(connection, """
          update substituicoes_funcao set situacao = 'CANCELADA'
          where motivo = 'FERIAS' and emp_codigo = 1
          """);
      assertThat(column(connection, """
          select situacao from radar_chefias_substituicoes where ferias_gozo_id = 11
          """)).containsExactly("DESCOBERTA");
      assertThat(column(connection, """
          select situacao from radar_chefias_substituicoes where ferias_gozo_id = 12
          """)).containsExactly("COBERTA");

      assertThatThrownBy(() -> execute(connection, """
          update substituicoes_funcao set designacao_id =
            (select min(id) from designacoes_funcao where emp_codigo = 2)
          where emp_codigo = 1
          """)).isInstanceOf(SQLException.class);
    }
  }

  @Test
  void replacementMustStillCoverItsOwnImpedimentAndTheFunctionMustBeInForce() throws SQLException {
    try (var connection = DriverManager.getConnection(URL)) {
      initialize(connection);
      seed(connection);
      execute(connection, "update afastamentos set situacao = 'CANCELADO' where id = 21");
      execute(connection, """
          insert into afastamentos (id, emp_codigo, numfunc, numvinc, dtini, dtfim, situacao)
          values (29, 1, 102, 1, current_date - 1, current_date + 4, 'HOMOLOGADO')
          """);
      assertThat(column(connection, """
          select situacao from radar_chefias_substituicoes
          where afastamento_id = 29 and substituicao_id is null and qtd_coberturas = 0
          """)).containsExactly("DESCOBERTA");
      execute(connection, "update ferias_gozo set dt_interrupcao = current_date where id = 11");
      assertThat(column(connection, """
          select situacao from radar_chefias_substituicoes
          where emp_codigo = 1 and titular_numfunc = 101 and substituicao_id is null and qtd_coberturas = 0
          """)).containsExactly("TITULAR_EM_EXERCICIO");
      execute(connection, """
          update funcoes_confianca set situacao = 'EXTINTA', dtfim = current_date - 1
          where emp_codigo = 1 and codigo = 'S7-DIR'
          """);
      assertThat(column(connection, """
          select emp_codigo from radar_chefias_substituicoes where titular_numfunc in (101, 201)
          """)).containsExactly("2");
    }
  }

  @Test
  void seedCreatesOrdinaryAssignmentsWithoutCurrentImpediments() throws SQLException {
    try (var connection = DriverManager.getConnection(URL)) {
      initialize(connection);
      execute(connection, "update ferias_gozo set dtfim = current_date - 1");
      execute(connection, "update afastamentos set dtfim = current_date - 1");
      seed(connection);
      assertThat(column(connection, """
          select emp_codigo from radar_chefias_substituicoes
          where situacao = 'TITULAR_EM_EXERCICIO' and qtd_coberturas = 0
          """)).containsExactlyInAnyOrder("1", "2");
      assertThat(column(connection, "select id from substituicoes_funcao")).isEmpty();
      assertThat(column(connection, """
          select emp_codigo from vagas_funcao where numero_vaga = 705 and situacao = 'OCUPADA'
          """)).containsExactlyInAnyOrder("1", "2");
    }
  }

  private static void initialize(Connection connection) {
    executeScript(connection, "db/slice7/predecessores.sql");
    executeScript(connection, "db/migration/V8__funcoes_designacoes_substituicoes.sql");
  }

  private static void seed(Connection connection) {
    executeScript(connection, "db/migration/V9__seed_funcoes_designacoes_substituicoes.sql");
  }

  private static void assertScopedJpaQueries(Connection connection) throws SQLException {
    var registry = new StandardServiceRegistryBuilder()
        .applySetting(AvailableSettings.JAKARTA_JDBC_URL, URL)
        .applySetting(AvailableSettings.HBM2DDL_AUTO, "none")
        .build();
    try {
      var metadata = new MetadataSources(registry);
      var scanner = new ClassPathScanningCandidateComponentProvider(false);
      scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
      for (var candidate : scanner.findCandidateComponents("br.com.praxis.ergonshowcase.domain")) {
        metadata.addAnnotatedClassName(candidate.getBeanClassName());
      }
      try (var factory = metadata.buildMetadata().buildSessionFactory(); var session = factory.openSession()) {
        var filter = new RadarChefiaSubstituicaoFilterDTO();
        filter.setBuscaAmpla("sem cobertura");
        filter.setSituacao("DESCOBERTA");
        var builder = new GenericSpecificationsBuilder<RadarChefiaSubstituicao>();
        var scope = ErgonTenantAccess.scope(RadarChefiaSubstituicao.class, new TenantContext(), false);
        var specification = scope.specification().and(builder.buildSpecification(filter, Pageable.unpaged()).spec());
        var cb = session.getCriteriaBuilder();
        var criteria = cb.createQuery(RadarChefiaSubstituicao.class);
        var root = criteria.from(RadarChefiaSubstituicao.class);
        var rows = session.createQuery(criteria.select(root).where(specification.toPredicate(root, criteria, cb)))
            .getResultList();
        assertThat(rows).singleElement().satisfies(row -> {
          assertThat(row.getTitularId()).isEqualTo(new VinculoId(103L, 1));
          assertThat(row.getCustomFields()).isEmpty();
          assertThat(row.getEmpCodigo()).isEqualTo(1);
        });
        assertSubstitutionFacts(session, connection);
        assertScopedPreviewsAndAnalytics(session);
        assertDesignacaoBulkTransaction(session, connection);
        assertDeleteGuards(session);
      }
    } finally {
      StandardServiceRegistryBuilder.destroy(registry);
    }
  }

  private static void assertScopedPreviewsAndAnalytics(org.hibernate.Session session) {
    var tenant = new TenantContext();
    var previews = new ErgonDeterminationService(tenant, null);
    ReflectionTestUtils.setField(previews, "em", session);
    var dia = LocalDate.of(2026, 11, 3);
    assertThat(previews.previaFuncao(new FuncaoDraft(dia, dia)).diasVigenciaPrevistos()).isEqualTo(1);
    assertThatThrownBy(() -> previews.previaFuncao(new FuncaoDraft(dia, dia.minusDays(1))))
        .isInstanceOfSatisfying(br.com.praxis.ergonshowcase.rules.ErgonRuleViolationException.class,
            error -> assertThat(error.getViolations()).singleElement()
                .satisfies(violation -> assertThat(violation.code()).isEqualTo("ERG-01101")));
    var vagas = session.createQuery("from VagaFuncao v where v.situacao = 'OCUPADA' order by v.id",
        VagaFuncao.class).getResultList();
    var local = vagas.stream().filter(v -> v.getEmpCodigo() == 1).findFirst().orElseThrow();
    var externa = vagas.stream().filter(v -> v.getEmpCodigo() == 2).findFirst().orElseThrow();
    assertThat(previews.previaDesignacao(new DesignacaoDraft(local.getId())).situacaoVagaPrevista())
        .isEqualTo("OCUPADA");
    assertThatThrownBy(() -> previews.previaDesignacao(new DesignacaoDraft(externa.getId())))
        .isInstanceOfSatisfying(org.springframework.web.server.ResponseStatusException.class,
            error -> assertThat(error.getStatusCode().value()).isEqualTo(404));

    var funcao = session.find(br.com.praxis.ergonshowcase.domain.FuncaoConfianca.class, local.getFuncaoId());
    assertThat(previews.previaVaga(new VagaDraft(funcao.getId())).controleVagaPrevisto()).isTrue();
    funcao.setSituacao("EXTINTA");
    assertThatThrownBy(() -> previews.previaVaga(new VagaDraft(funcao.getId())))
        .isInstanceOfSatisfying(ErgonRuleViolationException.class,
            error -> assertThat(error.getReasonCodes()).containsExactly("ERG-01154"));
    funcao.setSituacao("ATIVA");

    var designacao = session.createQuery("""
        from DesignacaoFuncao d where d.empCodigo = 1 and d.titularId.numfunc = 103
        """, DesignacaoFuncao.class).getSingleResult();
    assertThatThrownBy(() -> previews.previaSubstituicao(new SubstituicaoDraft(
        designacao.getId(), designacao.getDtini().minusDays(1), designacao.getDtini().plusDays(2))))
        .isInstanceOfSatisfying(ErgonRuleViolationException.class,
            error -> assertThat(error.getReasonCodes()).containsExactly("ERG-01319"));
    var funcaoDesignacao = session.find(br.com.praxis.ergonshowcase.domain.FuncaoConfianca.class, designacao.getFuncaoId());
    funcaoDesignacao.setPermiteSubstituicao(false);
    assertThatThrownBy(() -> previews.previaSubstituicao(new SubstituicaoDraft(
        designacao.getId(), designacao.getDtini(), designacao.getDtini().plusDays(2))))
        .isInstanceOfSatisfying(ErgonRuleViolationException.class,
            error -> assertThat(error.getReasonCodes()).containsExactly("ERG-01315"));
    funcaoDesignacao.setPermiteSubstituicao(true);
    session.evict(funcao);
    session.evict(funcaoDesignacao);
    assertThat(session.isDirty()).isFalse();

    var service = new VagaFuncaoService(null, null);
    @SuppressWarnings("unchecked")
    var vagaSnapshot = ((java.util.Optional<org.praxisplatform.uischema.capability.ResourceStateSnapshot>)
        ReflectionTestUtils.invokeMethod(service, "toResourceStateSnapshot", local)).orElseThrow();
    assertThat(vagaSnapshot.state()).isEqualTo("OCUPADA");
    assertThat(vagaSnapshot.metadata())
        .containsEntry("disponivelParaDesignacao", false)
        .containsEntry("ocupada", true);
    ReflectionTestUtils.setField(service, "entityManager", session);
    ReflectionTestUtils.setField(service, "statsQueryExecutor", new JpaStatsQueryExecutor());
    ReflectionTestUtils.setField(service, "statsEligibility", new StatsEligibility());
    ReflectionTestUtils.setField(service, "statsProperties", new StatsProperties(true, 20, 100, StatsSupportMode.AUTO));
    var filtro = new VagaFuncaoFilterDTO();
    filtro.setSituacao("OCUPADA");
    var count = new StatsMetricRequest(StatsMetric.COUNT, null);
    var agrupado = service.groupByStats(new GroupByStatsRequest<>(filtro, "situacao", count, 10, null));
    assertThat(agrupado.buckets()).singleElement().satisfies(bucket -> {
      assertThat(bucket.key()).isEqualTo("OCUPADA");
      assertThat(bucket.count()).isEqualTo(3);
    });
    assertThatThrownBy(() -> service.groupByStats(new GroupByStatsRequest<>(filtro, "titularAtualId", count, 10, null)))
        .isInstanceOf(IllegalArgumentException.class);

    var radarService = new br.com.praxis.ergonshowcase.service.RadarChefiaSubstituicaoService(null, null);
    ReflectionTestUtils.setField(radarService, "entityManager", session);
    ReflectionTestUtils.setField(radarService, "statsQueryExecutor", new JpaStatsQueryExecutor());
    ReflectionTestUtils.setField(radarService, "statsEligibility", new StatsEligibility());
    ReflectionTestUtils.setField(radarService, "statsProperties", new StatsProperties(true, 20, 100, StatsSupportMode.AUTO));
    var radarFiltro = new RadarChefiaSubstituicaoFilterDTO();
    radarFiltro.setSetorNome("Gestão de Pessoas");
    var radarAgrupado = radarService.groupByStats(new GroupByStatsRequest<>(radarFiltro, "situacao", count, 10, null));
    assertThat(radarAgrupado.buckets())
        .extracting(b -> b.key())
        .contains("COBERTA", "DESCOBERTA");
  }

  private static void assertDeleteGuards(org.hibernate.Session session) {
    var tx = session.beginTransaction();
    try {
      var designacao = session.createQuery("""
          from DesignacaoFuncao d where d.empCodigo = 1 and d.titularId.numfunc = 103
          """, DesignacaoFuncao.class).getSingleResult();
      var funcao = session.find(br.com.praxis.ergonshowcase.domain.FuncaoConfianca.class, designacao.getFuncaoId());
      var vaga = session.find(VagaFuncao.class, designacao.getVagaFuncaoId());
      var substituicao = new SubstituicaoFuncao();
      substituicao.setEmpCodigo(1);
      substituicao.setSituacao("LIQUIDADA");
      var funcaoService = new br.com.praxis.ergonshowcase.service.FuncaoConfiancaService(null, null);
      var vagaService = new br.com.praxis.ergonshowcase.service.VagaFuncaoService(null, null);
      var substituicaoService = new br.com.praxis.ergonshowcase.service.SubstituicaoFuncaoService(null, null, null);
      assertDeleteConflict(funcaoService, session, funcao.getId(), funcao);
      assertDeleteConflict(vagaService, session, vaga.getId(), vaga);
      assertDeleteConflict(substituicaoService, session, 1L, substituicao);
    } finally {
      tx.rollback();
    }
  }

  private static void assertDeleteConflict(Object service, org.hibernate.Session session, Long id, Object entity) {
    ReflectionTestUtils.setField(service, "entityManager", session);
    assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(service, "beforeDelete", id, entity))
        .isInstanceOfSatisfying(org.springframework.web.server.ResponseStatusException.class,
            error -> assertThat(error.getStatusCode().value()).isEqualTo(409));
  }

  private static void assertDesignacaoBulkTransaction(org.hibernate.Session session, Connection connection)
      throws SQLException {
    // The predecessor SQL fixture only models the referenced tables' identifiers.
    var mapper = org.mapstruct.factory.Mappers.getMapper(DesignacaoFuncaoMapper.class);
    var service = new DesignacaoFuncaoService(mock(DesignacaoFuncaoRepository.class), mapper,
        new ErgonRuleEngineFacade()) {
      @Override protected void validateReferences(DesignacaoFuncao draft) {
        requireReference(draft.getEmpCodigo(), VagaFuncao.class, draft.getVagaFuncaoId());
      }
    };
    CanonicalResourceTestContext.wire(service, session);
    var bulk = new ErgonBulkDomain();
    var json = new ObjectMapper().findAndRegisterModules();
    ReflectionTestUtils.setField(bulk, "json", json);
    ReflectionTestUtils.setField(bulk, "designacaoFuncaoService", service);
    var parameters = json.createObjectNode().put("dtExercicio", java.time.LocalDate.now().toString());
    String operation = "ergon.designacoes.homologar-designacoes-lote";
    String vacancyFilter = " where emp_codigo = 1 and numero_vaga = 704";

    try (var validator = Validation.buildDefaultValidatorFactory()) {
      ReflectionTestUtils.setField(bulk, "validator", validator.getValidator());
      var setup = session.beginTransaction();
      var seed = session.createQuery("""
          from DesignacaoFuncao d where d.empCodigo = 1 and d.titularId.numfunc = 103
          """, DesignacaoFuncao.class).getSingleResult();
      var seedVaga = session.find(VagaFuncao.class, seed.getVagaFuncaoId());
      seed.setSituacao("DESIGNADA");
      seedVaga.setSituacao("LIVRE");
      seedVaga.setTitularAtualId(null);
      setup.commit();
      session.clear();

      var transaction = session.beginTransaction();
      var designacao = session.getReference(DesignacaoFuncao.class, seed.getId());
      assertThat(org.hibernate.Hibernate.isInitialized(designacao)).isFalse();
      long previousVersion = designacao.getVersion();
      var vaga = session.find(VagaFuncao.class, designacao.getVagaFuncaoId());
      var proposalCandidate = (DesignacaoFuncao) bulk.commandCandidate(designacao, operation, parameters);
      assertThat(proposalCandidate.getSituacao()).isEqualTo("EM_EXERCICIO");
      assertThat(designacao.getSituacao()).isEqualTo("DESIGNADA");
      assertThat(vaga.getSituacao()).isEqualTo("LIVRE");
      assertThat(vaga.getTitularAtualId()).isNull();
      assertThat(session.isDirty()).isFalse();

      assertThat(bulk.applyCommand(designacao, proposalCandidate)).isTrue();
      session.flush();
      assertThat(vaga.getSituacao()).isEqualTo("OCUPADA");
      assertThat(vaga.getTitularAtualId()).isEqualTo(designacao.getTitularId());
      assertThatThrownBy(() -> bulk.commandCandidate(designacao, operation, parameters))
          .isInstanceOfSatisfying(ErgonRuleViolationException.class,
              error -> assertThat(error.getReasonCodes()).contains("CERG-07003"));
      transaction.rollback();
      session.clear();
      assertThat(column(connection, "select situacao from vagas_funcao" + vacancyFilter)).containsExactly("LIVRE");
      assertThat(column(connection, "select situacao from designacoes_funcao where emp_codigo = 1 and titular_numfunc = 103"))
          .containsExactly("DESIGNADA");

      var confirmation = session.beginTransaction();
      var persisted = session.find(DesignacaoFuncao.class, designacao.getId());
      assertThat(bulk.applyCommand(persisted, bulk.commandCandidate(persisted, operation, parameters))).isTrue();
      confirmation.commit();
      assertThat(column(connection, "select situacao from vagas_funcao" + vacancyFilter)).containsExactly("OCUPADA");
      assertThat(column(connection, "select titular_atual_numfunc from vagas_funcao" + vacancyFilter)).containsExactly("103");

      var stale = session.beginTransaction();
      String key = "administracao-pessoal.designacoes";
      var etags = BulkCandidateFixture.ETAGS;
      Long designacaoId = persisted.getId();
      var precondition = new ResourceVersionUpdatePrecondition<>(
          etags, etags.create(key, designacaoId, previousVersion), key, designacaoId);
      assertThatThrownBy(() -> service.executeBulkDomainCommand(designacaoId, precondition,
          current -> { throw new AssertionError("Versão obsoleta não pode executar a homologação"); }))
          .isInstanceOf(ResourceVersionPreconditionException.class);
      stale.rollback();
      session.clear();

      var update = session.beginTransaction();
      persisted = session.find(DesignacaoFuncao.class, designacao.getId());
      var dto = new UpdateDesignacaoFuncaoDTO();
      BeanUtils.copyProperties(persisted, dto);
      dto.setVagaFuncaoId(null);
      ReflectionTestUtils.invokeMethod(service, "beforeUpdate", persisted.getId(), persisted, dto);
      mapper.applyUpdate(persisted, dto);
      session.flush();
      ReflectionTestUtils.invokeMethod(service, "afterUpdate", persisted.getId(), persisted, dto);
      assertThat(session.find(VagaFuncao.class, vaga.getId()).getSituacao()).isEqualTo("LIVRE");
      assertThat(session.find(VagaFuncao.class, vaga.getId()).getTitularAtualId()).isNull();
      update.rollback();
      session.clear();

      var deletion = session.beginTransaction();
      var toDelete = session.find(DesignacaoFuncao.class, designacao.getId());
      ReflectionTestUtils.invokeMethod(service, "beforeDelete", toDelete.getId(), toDelete);
      session.remove(toDelete);
      ReflectionTestUtils.invokeMethod(service, "afterDelete", toDelete.getId(), toDelete);
      assertThat(session.find(VagaFuncao.class, vaga.getId()).getSituacao()).isEqualTo("LIVRE");
      deletion.rollback();
      session.clear();
      assertThat(column(connection, "select titular_atual_numfunc from vagas_funcao" + vacancyFilter)).containsExactly("103");
    }
  }

  private static void assertSubstitutionFacts(org.hibernate.Session session, Connection connection)
      throws SQLException {
    var rules = new ErgonRuleEngineFacade();
    var draft = session.createQuery("""
        from SubstituicaoFuncao s where s.empCodigo = 1 and s.feriasGozoId = 11
        """, SubstituicaoFuncao.class).getSingleResult();
    session.detach(draft);

    var facts = SubstituicaoFuncaoFacts.load(session, draft);
    assertThat(facts.at("/impedimento/tipo").asText()).isEqualTo("FERIAS");
    assertThat(facts.at("/impedimento/situacao").asText()).isEqualTo("EM_GOZO");
    assertThat(facts.at("/impedimento/interrupcao").isNull()).isTrue();
    assertThat(facts.at("/neighborhood/outrasSubstituicoesTitular").intValue()).isZero();
    assertThat(facts.at("/neighborhood/outrasSubstituicoesSubstituto").intValue()).isZero();
    assertThat(facts.at("/neighborhood/feriasSubstituto").intValue()).isZero();
    assertThat(facts.at("/neighborhood/afastamentosSubstituto").intValue()).isZero();
    assertThat(rules.enforceSubstituicaoFuncao(1, facts).decision()).isEqualTo(RuleDecision.ALLOW);

    execute(connection, "update ferias_gozo set dt_interrupcao = dtini + 1 where id = 11");
    try {
      var interrupted = SubstituicaoFuncaoFacts.load(session, draft);
      assertThatThrownBy(() -> rules.enforceSubstituicaoFuncao(1, interrupted))
          .isInstanceOfSatisfying(ErgonRuleViolationException.class,
              failure -> assertThat(failure.getReasonCodes()).containsExactly("ERG-01311"));
    } finally {
      execute(connection, "update ferias_gozo set dt_interrupcao = null where id = 11");
    }

    draft.setFeriasGozoId(null);
    draft.setAfastamentoId(25L);
    draft.setDtini(draft.getDtini().plusDays(2));
    draft.setDtfim(draft.getDtini().plusDays(3));
    assertThat(SubstituicaoFuncaoFacts.load(session, draft).at("/impedimento/tipo").isNull()).isTrue();
    execute(connection, "update afastamentos set emp_codigo = 1 where id = 25");
    try {
      var loaded = SubstituicaoFuncaoFacts.load(session, draft);
      assertThat(loaded.at("/impedimento/tipo").asText()).isEqualTo("AFASTAMENTO");
      assertThat(loaded.at("/impedimento/situacao").asText()).isEqualTo("HOMOLOGADO");
      assertThat(rules.enforceSubstituicaoFuncao(1, loaded).decision()).isEqualTo(RuleDecision.ALLOW);
    } finally {
      execute(connection, "update afastamentos set emp_codigo = 2 where id = 25");
    }
  }

  private static void executeScript(Connection connection, String path) {
    ScriptUtils.executeSqlScript(connection, new ClassPathResource(path));
  }

  private static void execute(Connection connection, String sql) throws SQLException {
    try (var statement = connection.createStatement()) {
      statement.executeUpdate(sql);
    }
  }

  private static List<String> column(Connection connection, String sql) throws SQLException {
    try (var statement = connection.createStatement(); var rows = statement.executeQuery(sql)) {
      var values = new ArrayList<String>();
      while (rows.next()) values.add(rows.getString(1));
      return values;
    }
  }
}
