package br.com.praxis.ergonshowcase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.com.praxis.ergonshowcase.config.TenantContext;
import br.com.praxis.ergonshowcase.config.OptionSourcesConfig;
import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.controller.SetorController;
import br.com.praxis.ergonshowcase.controller.CargoSetorController;
import br.com.praxis.ergonshowcase.controller.DesignacaoFuncaoController;
import br.com.praxis.ergonshowcase.controller.GrupoSetorController;
import br.com.praxis.ergonshowcase.controller.HistoricoSetorController;
import br.com.praxis.ergonshowcase.controller.RadarOrganogramaChefiaOcupacaoController;
import br.com.praxis.ergonshowcase.controller.SetorParametrizacaoErgonController;
import br.com.praxis.ergonshowcase.controller.SubempresaLocalidadeController;
import br.com.praxis.ergonshowcase.domain.*;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.mapper.*;
import br.com.praxis.ergonshowcase.repository.*;
import br.com.praxis.ergonshowcase.service.*;
import jakarta.persistence.Entity;
import java.time.LocalDate;
import java.util.*;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mapstruct.factory.Mappers;
import org.praxisplatform.uischema.annotation.ResourceReference;
import org.praxisplatform.uischema.dto.OptionDTO;
import org.praxisplatform.uischema.openapi.OpenApiCanonicalOperationResolver;
import org.praxisplatform.uischema.openapi.OpenApiDocumentService;
import org.praxisplatform.uischema.options.OptionSourceFilterRequest;
import org.praxisplatform.uischema.options.OptionSourceRegistry;
import org.praxisplatform.uischema.options.service.CompositeOptionSourceQueryExecutor;
import org.praxisplatform.uischema.options.service.DefaultOptionSourceProviderRegistry;
import org.praxisplatform.uischema.options.service.jpa.JpaOptionSourceProvider;
import org.praxisplatform.uischema.options.service.jpa.JpaOptionSourceQueryExecutor;
import org.praxisplatform.uischema.rest.exceptionhandler.GlobalExceptionHandler;
import org.praxisplatform.uischema.schema.FilteredSchemaReferenceResolver;
import org.praxisplatform.uischema.stats.StatsEligibility;
import org.praxisplatform.uischema.stats.StatsProperties;
import org.praxisplatform.uischema.stats.StatsSupportMode;
import org.praxisplatform.uischema.stats.service.jpa.JpaStatsQueryExecutor;
import org.praxisplatform.uischema.surface.AnnotationDrivenSurfaceDefinitionRegistry;
import org.praxisplatform.uischema.surface.DefaultSurfaceAvailabilityContextResolver;
import org.praxisplatform.uischema.surface.DefaultSurfaceAvailabilityEvaluator;
import org.praxisplatform.uischema.surface.SurfaceCatalogService;
import org.praxisplatform.uischema.validation.AnnotationConflictMode;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Exige V11/V12 na cópia PostgreSQL 17 explicitamente admitida. Todas as fixtures fazem rollback. */
@EnabledIfEnvironmentVariable(named="SLICE8_JDBC_URL",matches="jdbc:postgresql:.*")
class OrganogramaSlice8PostgresTest {
  static StandardServiceRegistry registry;
  static SessionFactory factory;
  Session session;
  TenantContext tenant;
  HistoricoSetorService history;
  SetorService sectors;

  @BeforeAll static void openFactory() {
    registry=new StandardServiceRegistryBuilder()
        .applySetting(AvailableSettings.JAKARTA_JDBC_URL,System.getenv("SLICE8_JDBC_URL"))
        .applySetting(AvailableSettings.JAKARTA_JDBC_USER,System.getenv("SLICE8_DB_USER"))
        .applySetting(AvailableSettings.JAKARTA_JDBC_PASSWORD,System.getenv("SLICE8_DB_PASSWORD"))
        .applySetting(AvailableSettings.HBM2DDL_AUTO,"none")
        .applySetting(AvailableSettings.SHOW_SQL,false).build();
    var metadata=new MetadataSources(registry);
    var scanner=new ClassPathScanningCandidateComponentProvider(false);
    scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
    for(var candidate:scanner.findCandidateComponents("br.com.praxis.ergonshowcase.domain"))metadata.addAnnotatedClassName(candidate.getBeanClassName());
    factory=metadata.buildMetadata().buildSessionFactory();
  }
  @AfterAll static void closeFactory() {if(factory!=null)factory.close();if(registry!=null)StandardServiceRegistryBuilder.destroy(registry);}
  @BeforeEach void open() {
    session=factory.openSession();session.beginTransaction();
    assertThat(session.createNativeQuery("show server_version",String.class).getSingleResult()).startsWith("17.");
    tenant=mock(TenantContext.class);when(tenant.orgao()).thenReturn(1);when(tenant.resolveTenant()).thenReturn(Optional.of("1"));
    var repositories=new JpaRepositoryFactory(session);
    history=new HistoricoSetorService(repositories.getRepository(HistoricoSetorRepository.class),Mappers.getMapper(HistoricoSetorMapper.class));
    ReflectionTestUtils.setField(history,"entityManager",session);
    ReflectionTestUtils.setField(history,"resourceTenant",tenant);
    ReflectionTestUtils.setField(history,"temporalTenant",tenant);
    sectors=new SetorService(repositories.getRepository(SetorRepository.class),Mappers.getMapper(SetorMapper.class),tenant);
    ReflectionTestUtils.setField(sectors,"entityManager",session);
    ReflectionTestUtils.setField(sectors,"resourceTenant",tenant);
    ReflectionTestUtils.setField(sectors,"history",history);
  }
  @AfterEach void close() {
    SecurityContextHolder.clearContext();
    if(session!=null){if(session.getTransaction().isActive())session.getTransaction().rollback();session.close();}
  }

  @Test void directoryKeepsStringIdentityAndHistoricalQuerySelectsPastCurrentAndFuturePeriods() {
    var f=new SetorFilterDTO();f.setSetor("S8-1-DIR");
    assertThat(sectors.filter(f,Pageable.unpaged(),List.of()).getContent()).singleElement().satisfies(d->assertThat(d.getNome()).isEqualTo("Diretoria atual 1"));
    for(var scenario:Map.of("2021-06-01","histórica","2026-06-01","atual","2031-06-01","futura").entrySet()) {
      f.setDataReferencia(LocalDate.parse(scenario.getKey()));f.setNome(scenario.getValue());
      assertThat(sectors.filter(f,Pageable.unpaged(),List.of()).getContent()).singleElement().satisfies(d->{
        assertThat(d.getId()).isEqualTo("S8-1-DIR");assertThat(d.getNome()).contains(scenario.getValue());assertThat(d.getEmpCodigo()).isEqualTo(1);
      });
      assertThat(sectors.filterOptions(f,Pageable.unpaged()).getContent()).singleElement().satisfies(o->assertThat(o.label()).contains(scenario.getValue()));
    }
  }

  @Test void historicalOnlyUnitsSameCodesAndIncludeIdsCannotCrossOrganizationOrDate() {
    insert(1,"X8-HIST","Antiga um",null,"2000-01-01","2001-12-31",null);
    insert(2,"X8-HIST","Antiga dois",null,"2000-01-01","2001-12-31",null);
    var f=new SetorFilterDTO();f.setDataReferencia(LocalDate.parse("2001-01-01"));f.setSetor("X8-HIST");
    assertThat(sectors.filter(f,Pageable.unpaged(),List.of()).getContent()).singleElement().satisfies(d->assertThat(d.getNome()).isEqualTo("Antiga um"));
    f.setEmpCodigo(2);assertThat(sectors.filter(f,Pageable.unpaged(),List.of()).getContent()).isEmpty();
    f.setEmpCodigo(null);f.setDataReferencia(LocalDate.parse("2031-01-01"));
    assertThatThrownBy(()->sectors.filter(f,Pageable.unpaged(),List.of("X8-HIST"))).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
  }

  @Test void unresolvableExplicitIncludedIdsNeverBecomeAnUnrestrictedHistoricalPage() {
    var f=new SetorFilterDTO();f.setDataReferencia(LocalDate.parse("2026-11-04"));
    for(var id:List.of("INEXISTENTE","S8-2-DIR"))assertThatThrownBy(()->sectors.filter(f,Pageable.unpaged(),List.of(id)))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class).hasMessageContaining("Referências indisponíveis");
    assertThatThrownBy(()->sectors.filter(f,Pageable.unpaged(),List.of("S8-1-DIR","S8-2-DIR"))).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
  }

  @Test void upgradeKeepsThePreviouslyNullableClassificationWithoutInventingBusinessData() {
    var f=new SetorFilterDTO();f.setSetor("S8-UPGRADE-NULL");
    assertThat(sectors.filter(f,Pageable.unpaged(),List.of()).getContent()).singleElement().satisfies(d->assertThat(d.getTipoSetor()).isNull());
    f.setDataReferencia(LocalDate.parse("2021-01-01"));
    assertThat(sectors.filter(f,Pageable.unpaged(),List.of()).getContent()).singleElement().satisfies(d->assertThat(d.getTipoSetor()).isNull());
  }

  @Test void currentOnlyQuerySurfacesRejectTemporalCriteriaInsteadOfFilteringTheCurrentRow() {
    var f=new SetorFilterDTO();f.setDataReferencia(LocalDate.parse("2021-01-01"));
    assertThatThrownBy(()->sectors.getSpecificationsBuilder().buildSpecification(f,Pageable.unpaged(),
        new org.praxisplatform.uischema.customfields.NoOpCustomFieldDefinitionProvider(),Optional::empty,"administracao-pessoal.setores"))
      .isInstanceOf(org.praxisplatform.uischema.rest.exceptionhandler.exception.InvalidFilterPayloadException.class);
  }

  @Test void temporalCursorAndLocatePreserveStableStringOrderingAndTenant() {
    var f=new SetorFilterDTO();f.setDataReferencia(LocalDate.parse("2026-11-04"));f.setBuscaAmpla("S8-1");
    var expected=sectors.filter(f,Pageable.unpaged(Sort.by("id")),List.of()).getContent().stream().map(SetorResponseDTO::getId).toList();
    var actual=new ArrayList<String>();String after=null;int pages=0;
    do {var page=sectors.filterByCursor(f,Sort.by("id"),after,null,2);page.content().forEach(d->actual.add(d.getId()));after=page.next();assertThat(++pages).isLessThan(10);}while(after!=null);
    assertThat(actual).containsExactlyElementsOf(expected);
    for(int i=0;i<expected.size();i++)assertThat(sectors.locate(f,Sort.by("id"),expected.get(i))).hasValue(i);
  }

  @Test void currentDirectoryReadsHistoryChangesWithoutMutatingPhysicalForeignKeyTargets() {
    session.createNativeMutationQuery("update historico_setores set nome='Nome corrente reestruturado' where setor='S8-1-DIR' and dtini=date '2024-01-01'").executeUpdate();
    var f=new SetorFilterDTO();f.setSetor("S8-1-DIR");
    assertThat(sectors.filter(f,Pageable.unpaged(),List.of()).getContent()).singleElement().satisfies(d->assertThat(d.getNome()).isEqualTo("Nome corrente reestruturado"));
    assertThat(session.createNativeQuery("select nome from setores where setor='S8-1-DIR'",String.class).getSingleResult()).isEqualTo("Diretoria atual 1");
  }

  @Test void directoryRehydratesLastKnownPeriodInAGapAndFirstKnownPeriodBeforeItStarts() {
    session.createNativeMutationQuery("""
        update historico_setores set nome='Diretoria encerrada', dtfim=current_date-1, extinto=true
        where emp_codigo=1 and setor='S8-1-DIR' and dtini=date '2024-01-01'
        """).executeUpdate();
    session.createNativeMutationQuery("""
        update historico_setores set nome='Primeira vigência futura', dtini=current_date+1
        where emp_codigo=1 and setor='S8-1-RAIZ'
        """).executeUpdate();
    var filter = new SetorFilterDTO();
    filter.setSetor("S8-1-DIR");
    var yesterday = session.createNativeQuery("select current_date-1", LocalDate.class).getSingleResult();
    assertThat(sectors.filter(filter, Pageable.unpaged(), List.of()).getContent()).singleElement().satisfies(row -> {
      assertThat(row.getNome()).isEqualTo("Diretoria encerrada");
      assertThat(row.getDtfim()).isEqualTo(yesterday);
      assertThat(row.getExtinto()).isTrue();
      assertThat(row.getCaminhoHierarquico()).isNull();
    });
    assertThat(sectors.findById("S8-1-DIR").body().getNome()).isEqualTo("Diretoria encerrada");
    assertThat(sectors.findById("S8-1-RAIZ").body().getNome()).isEqualTo("Primeira vigência futura");
    session.createNativeMutationQuery("""
        delete from historico_setores where emp_codigo=1 and setor='S8-1-DIR' and dtini>current_date
        """).executeUpdate();
    assertThat(sectors.findById("S8-1-DIR").body().getDtfim()).isEqualTo(yesterday);
    assertThat(session.createNativeQuery("select nome from setores where setor='S8-1-DIR'", String.class)
        .getSingleResult()).isEqualTo("Diretoria atual 1");
  }

  @Test void optionSourcePublishesExtinctStatusAndRetainsExistingCodesByIds() {
    session.createNativeMutationQuery("""
        update historico_setores set extinto=true
        where emp_codigo=1 and setor='S8-1-RAIZ'
        """).executeUpdate();
    var registry = new OptionSourcesConfig().options();
    @SuppressWarnings("unchecked")
    ObjectProvider<OptionSourceRegistry> provider = mock(ObjectProvider.class);
    when(provider.getIfAvailable()).thenReturn(registry);
    ReflectionTestUtils.setField(sectors, "optionSourceRegistryProvider", provider);
    var jpa = new JpaOptionSourceProvider(new JpaOptionSourceQueryExecutor());
    ReflectionTestUtils.setField(sectors, "optionSourceQueryExecutor",
        new CompositeOptionSourceQueryExecutor(new DefaultOptionSourceProviderRegistry(List.of(jpa))));

    var request = new OptionSourceFilterRequest<SetorFilterDTO>(
        null, List.of(), "Secretaria demonstrativa 1", "text", null, List.of());
    assertThat(sectors.filterOptionSourceOptions(ApiPaths.SETOR_SOURCE, request, PageRequest.of(0, 10)).getContent())
        .singleElement().satisfies(option -> assertLookup(option, "INATIVO", false));
    var rehydrated = sectors.byIdsOptionSourceOptions(ApiPaths.SETOR_SOURCE, List.of("S8-1-RAIZ", "S8-1-DIR", "S8-2-RAIZ"));
    assertThat(rehydrated).extracting(OptionDTO::id).containsExactly("S8-1-RAIZ", "S8-1-DIR");
    assertLookup(rehydrated.get(0), "INATIVO", false);
    assertLookup(rehydrated.get(1), "VIGENTE", true);
    assertThat(sectors.resolveOptionSource(ApiPaths.SETOR_SOURCE).entityLookup().selectionPolicy()
        .allowRetainInvalidExistingValue()).isTrue();
  }

  private static void assertLookup(OptionDTO<?> option, String status, boolean selectable) {
    assertThat(option.extra()).containsEntry("status", status).containsEntry("selectable", selectable);
  }

  @Test void radarSegmentsFunctionStartAndInclusiveEndAndClearsCommandOutsideItsValidity() {
    session.createNativeMutationQuery("""
        update funcoes_confianca set dtini=date '2026-07-02', dtfim=date '2026-07-19', situacao='EXTINTA'
        where emp_codigo=1 and codigo='S8-TIT'
        """).executeUpdate();
    for (String date : List.of("2026-07-01", "2026-07-02", "2026-07-19", "2026-07-20")) {
      boolean valid = date.equals("2026-07-02") || date.equals("2026-07-19");
      var snapshot = session.createNativeQuery("""
          select dtini,dtfim,situacao_comando_setor,chefe_titular_numfunc,chefe_em_exercicio_numfunc
          from radar_organograma_chefias_ocupacao where emp_codigo=1 and setor='S8-1-TIT' and tipo_hierarquia=0
          and cast(:date as date) between dtini and coalesce(dtfim,date '9999-12-31')
          """, Object[].class).setParameter("date", date).getSingleResult();
      assertThat(snapshot[2]).isEqualTo(valid ? "TITULAR_EM_EXERCICIO" : null);
      assertThat(snapshot[3]).isEqualTo(valid ? 800011L : null);
      assertThat(snapshot[4]).isEqualTo(valid ? 800011L : null);
      if (valid) {
        assertThat(snapshot[0].toString()).isEqualTo("2026-07-02");
        assertThat(snapshot[1].toString()).isEqualTo("2026-07-19");
      }
    }
  }

  @Test void radarRecalculatesAtIndependentExerciseAbsenceAndSubstitutionBoundaries() {
    session.createNativeMutationQuery("""
        update designacoes_funcao set dt_exercicio=date '2026-10-27', dtfim=date '2026-12-04', situacao='DISPENSADA'
        where emp_codigo=1 and setor_id='S8-1-SUB'
        """).executeUpdate();
    session.createNativeMutationQuery("""
        update substituicoes_funcao set dtini=date '2026-11-04', dtfim=date '2026-11-26'
        where descricao='Exercício interino demonstrativo S8' and emp_codigo=1
        """).executeUpdate();
    String vacant = "VACANCIA_CHEFIA_SEM_TITULAR";
    String present = "TITULAR_EM_EXERCICIO";
    String uncovered = "DESCOBERTURA_CRITICA_TITULAR_AUSENTE_SEM_SUBSTITUTO";
    String covered = "SUBSTITUTO_EM_EXERCICIO_TITULAR_AUSENTE";
    for (var expected : Map.of(
        "2026-10-26", vacant, "2026-10-27", present,
        "2026-10-31", present, "2026-11-01", uncovered,
        "2026-11-03", uncovered, "2026-11-04", covered,
        "2026-11-26", covered, "2026-11-27", uncovered,
        "2026-12-01", present, "2026-12-05", vacant).entrySet()) {
      assertThat(command("S8-1-SUB", expected.getKey())).as(expected.getKey()).isEqualTo(expected.getValue());
    }
  }

  @Test void temporalHttpResultOpensThePublishedReferenceAndItsExactPeriod() throws Exception {
    insert(1, "X8-PAI", "Superior auxiliar", null, "2000-01-01", "2003-12-31", null);
    insert(1, "X8-LEG", "Unidade antiga", null, "2000-01-01", "2001-12-31", "X8-PAI");
    insert(1, "X8-LEG", "Unidade posterior", null, "2002-01-01", "2003-12-31", "X8-PAI");
    insert(2, "X8-LEG", "Unidade de outro órgão", null, "2000-01-01", "2001-12-31", null);
    var controller = new SetorController(sectors, history, mock(SetorParametrizacaoErgonService.class),
        mock(CargoSetorService.class), mock(DesignacaoFuncaoService.class),
        mock(RadarOrganogramaChefiaOcupacaoService.class));
    ReflectionTestUtils.setField(controller, "paginationMaxSize", 100);
    ReflectionTestUtils.invokeMethod(controller, "initializeBasePath");
    var mvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler()).build();
    var webContext = mvc.getDispatcherServlet().getWebApplicationContext();
    var mappings = webContext.getBean(RequestMappingHandlerMapping.class);
    var documents = mock(OpenApiDocumentService.class);
    when(documents.resolveGroupFromPath(anyString())).thenReturn("ergon");
    var operations = new OpenApiCanonicalOperationResolver(documents, mappings);
    var surfaces = new AnnotationDrivenSurfaceDefinitionRegistry(mappings, webContext, operations,
        new FilteredSchemaReferenceResolver(), AnnotationConflictMode.FAIL);
    var catalog = new SurfaceCatalogService(surfaces, new DefaultSurfaceAvailabilityEvaluator(),
        new DefaultSurfaceAvailabilityContextResolver());
    ReflectionTestUtils.setField(controller, "surfaceCatalogService", catalog);
    SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
        "leitor", "", List.of(new SimpleGrantedAuthority("ERGON_READ"))));
    var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
    var reference = SetorResponseDTO.class.getDeclaredField("nome").getAnnotation(ResourceReference.class);
    assertThat(reference.previewSurfaceId()).isEqualTo(reference.surfaceId()).isEqualTo("ficha-unidade");
    assertThat(catalog.findByResourceKey(reference.resourceKey()).surfaces())
        .anySatisfy(surface -> assertThat(surface.id()).isEqualTo(reference.previewSurfaceId()));

    var references = new ArrayList<String>();
    for (var scenario : List.of(List.of("2001-06-01", "Unidade antiga"), List.of("2003-06-01", "Unidade posterior"))) {
      var result = mvc.perform(post(ApiPaths.SETORES + "/filter").contentType("application/json")
              .content("""
                  {"setor":"X8-LEG","dataReferencia":"%s","tipoHierarquia":4,"nome":"%s",
                   "setorPai":"X8-PAI","encerrados":true,"semFundamento":true,"nivelHierarquico":2}
                  """.formatted(scenario.get(0), scenario.get(1))))
          .andExpect(status().isOk()).andExpect(jsonPath("$.data.content[0].id").value("X8-LEG"))
          .andReturn();
      var row = mapper.readTree(result.getResponse().getContentAsByteArray()).at("/data/content/0");
      String ref = row.path(reference.valueField()).asText();
      assertThat(ref).isNotBlank().isNotEqualTo(row.path("id").asText());
      references.add(ref);
      var item = mvc.perform(get(ApiPaths.SETORES + "/{id}", ref)).andExpect(status().isOk())
          .andExpect(jsonPath("$.data.nome").value(scenario.get(1))).andReturn();
      assertThat(java.net.URI.create(row.at("/_links/self/href").asText()).getPath())
          .isEqualTo(ApiPaths.SETORES + "/" + ref);
      var surfacesHref = mapper.readTree(item.getResponse().getContentAsByteArray()).at("/_links/surfaces/href").asText();
      var discovery = mvc.perform(get(java.net.URI.create(surfacesHref))).andExpect(status().isOk())
          .andExpect(jsonPath("$.resourceId").value(ref)).andReturn();
      var preview = mapper.readTree(discovery.getResponse().getContentAsByteArray()).path("surfaces").valueStream()
          .filter(surface -> surface.path("id").asText().equals(reference.previewSurfaceId()))
          .findFirst().orElseThrow();
      assertThat(preview.path("kind").asText()).isEqualTo("READ_PROJECTION");
      assertThat(preview.path("availability").path("allowed").asBoolean()).isTrue();
      assertThat(preview.path("scope").asText()).isEqualTo("ITEM");
      assertThat(preview.path("method").asText()).isEqualTo("GET");
      assertThat(preview.path("schemaUrl").asText()).contains("/schemas/filtered");
      mvc.perform(get(preview.path("path").asText(), ref))
          .andExpect(status().isOk()).andExpect(jsonPath("$.nome").value(scenario.get(1)))
          .andExpect(jsonPath("$.id").value("X8-LEG"))
          .andExpect(jsonPath("$.caminhoHierarquico").value(path("X8-PAI", "X8-LEG")));
    }
    assertThat(references).doesNotHaveDuplicates();
    mvc.perform(post(ApiPaths.SETORES + "/filter").param("includeIds", "X8-LEG,S8-2-DIR")
        .contentType("application/json").content("{\"dataReferencia\":\"2001-06-01\"}"))
        .andExpect(status().isBadRequest());
    mvc.perform(get(ApiPaths.SETORES + "/{id}/ficha-unidade", "S8-1-DIR"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("Diretoria atual 1"));
    mvc.perform(get(ApiPaths.SETORES + "/{id}", references.getFirst().replace("2001-06-01", "2003-06-01")))
        .andExpect(status().isNotFound());
    when(tenant.orgao()).thenReturn(2);
    when(tenant.resolveTenant()).thenReturn(Optional.of("2"));
    mvc.perform(get(ApiPaths.SETORES + "/{id}/ficha-unidade", references.getFirst()))
        .andExpect(status().isNotFound());
  }

  @Test void sixCommandStatesComeFromSourceRowsAndRecalculateAfterCancellation() {
    var expected=Set.of("TITULAR_EM_EXERCICIO","SUBSTITUTO_EM_EXERCICIO_TITULAR_AUSENTE","DESCOBERTURA_CRITICA_TITULAR_AUSENTE_SEM_SUBSTITUTO",
        "VACANCIA_CHEFIA_SEM_TITULAR","SEM_CHEFIA_PARAMETRIZADA","CONFLITO_DUPLA_TITULARIDADE");
    for(int org:List.of(1,2)) {
      var states=session.createNativeQuery("select distinct situacao_comando_setor from radar_organograma_chefias_ocupacao where emp_codigo=:o and setor like 'S8-%' and tipo_hierarquia=0 and date '2026-11-04' between dtini and coalesce(dtfim,date '9999-12-31')",String.class).setParameter("o",org).getResultList();
      assertThat(states).containsExactlyInAnyOrderElementsOf(expected);
    }
    session.createNativeMutationQuery("update substituicoes_funcao set situacao='CANCELADA' where descricao='Exercício interino demonstrativo S8' and emp_codigo=1").executeUpdate();
    assertThat(command("S8-1-SUB","2026-11-04")).isEqualTo("DESCOBERTURA_CRITICA_TITULAR_AUSENTE_SEM_SUBSTITUTO");
    assertThat(command("S8-1-SUB","2026-12-01")).isEqualTo("TITULAR_EM_EXERCICIO");
  }

  @Test void siblingEmploymentAndAbsentSubstituteDoNotProduceFalseCoverage() {
    session.createNativeMutationQuery("update afastamentos set numfunc=800014 where id=800013").executeUpdate();
    assertThat(command("S8-1-SUB","2026-11-04")).isEqualTo("DESCOBERTURA_CRITICA_TITULAR_AUSENTE_SEM_SUBSTITUTO");
    session.createNativeMutationQuery("insert into vinculos(numfunc,numero,emp_codigo,situacao,regimejur,tipovinc,categoria,dtexerc) values(800012,2,1,'ATIVO','ESTATUTARIO','EFETIVO','SERVIDOR',date '2024-01-01')").executeUpdate();
    session.createNativeMutationQuery("update afastamentos set numvinc=2 where id=800012").executeUpdate();
    assertThat(command("S8-1-SUB","2026-11-04")).isEqualTo("TITULAR_EM_EXERCICIO");
  }

  @Test void approvedSpecialLeaveAndRemovingFrequencyAreActualAbsenceSources() {
    var period=((Number)session.createNativeQuery("""
      insert into periodos_aquisitivos_licenca_especial(emp_codigo,numfunc,numvinc,descricao,dtini,dtfim,chave,situacao)
      values(1,800011,1,'Período de prova',date '2020-01-01',date '2024-12-31',1,'DISPONIVEL') returning id
      """,Long.class).getSingleResult()).longValue();
    var leave=((Number)session.createNativeQuery("""
      insert into licencas_especiais_gozo(emp_codigo,numfunc,numvinc,descricao,dtini,dtfim,situacao,
      periodo_aquisitivo_licenca_id,chave_peraqlicesp,ordem_parcela,dtprevfim,dias_gozo)
      values(1,800011,1,'Gozo de prova',date '2026-11-01',date '2026-11-30','HOMOLOGADA',:p,1,1,date '2026-11-30',30) returning id
      """,Long.class).setParameter("p",period).getSingleResult()).longValue();
    assertThat(command("S8-1-TIT","2026-11-04")).isEqualTo("DESCOBERTURA_CRITICA_TITULAR_AUSENTE_SEM_SUBSTITUTO");
    session.createNativeMutationQuery("update licencas_especiais_gozo set situacao='PROGRAMADA' where id=:id").setParameter("id",leave).executeUpdate();
    assertThat(command("S8-1-TIT","2026-11-04")).isEqualTo("TITULAR_EM_EXERCICIO");
    session.createNativeMutationQuery("insert into frequencias(id,numfunc,numvinc,emp_codigo,dtini,dtfim,tipofreq,codfreq,situacao) values(899999,800011,1,1,date '2026-11-01',date '2026-11-30','LICENCA',201,'HOMOLOGADO')").executeUpdate();
    assertThat(command("S8-1-TIT","2026-11-04")).isEqualTo("DESCOBERTURA_CRITICA_TITULAR_AUSENTE_SEM_SUBSTITUTO");
    session.createNativeMutationQuery("update frequencias set situacao='CANCELADO' where id=899999").executeUpdate();
    assertThat(command("S8-1-TIT","2026-11-04")).isEqualTo("TITULAR_EM_EXERCICIO");
  }

  @Test void temporalOverlapAndCrossOrganizationReferencesAreRejectedByPostgres() {
    session.doWork(c->{
      for(String sql:List.of(
          "insert into historico_setores(emp_codigo,setor,nome,dtini,tipo_setor,caminho_hierarquico) values(1,'S8-1-DIR','Sobreposição',date '2026-01-01','DIRETORIA','/x/')",
          "update historico_setores set grupo_setor_id=(select id from grupos_setores where emp_codigo=2 limit 1) where setor='S8-1-RAIZ'")) {
        var sp=c.setSavepoint();
        try(var st=c.createStatement()) {assertThatThrownBy(()->st.execute(sql)).isInstanceOf(java.sql.SQLException.class);}
        finally {c.rollback(sp);c.releaseSavepoint(sp);}
      }
    });
  }

  private String command(String sector,String day) {
    return session.createNativeQuery("select situacao_comando_setor from radar_organograma_chefias_ocupacao where setor=:s and tipo_hierarquia=0 and cast(:d as date) between dtini and coalesce(dtfim,date '9999-12-31')",String.class).setParameter("s",sector).setParameter("d",day).getSingleResult();
  }

  private MockMvc phase2Mvc() {
    var repo = new JpaRepositoryFactory(session);
    var params = new SetorParametrizacaoErgonService(repo.getRepository(SetorParametrizacaoErgonRepository.class),
        Mappers.getMapper(SetorParametrizacaoErgonMapper.class));
    var quadro = new CargoSetorService(repo.getRepository(CargoSetorRepository.class), Mappers.getMapper(CargoSetorMapper.class));
    var designacoes = new DesignacaoFuncaoService(repo.getRepository(DesignacaoFuncaoRepository.class),
        Mappers.getMapper(DesignacaoFuncaoMapper.class), mock(br.com.praxis.ergonshowcase.rules.ErgonRuleEngineFacade.class));
    var radar = new RadarOrganogramaChefiaOcupacaoService(repo.getRepository(RadarOrganogramaChefiaOcupacaoRepository.class),
        Mappers.getMapper(RadarOrganogramaChefiaOcupacaoMapper.class));
    var grupos = new GrupoSetorService(repo.getRepository(GrupoSetorRepository.class), Mappers.getMapper(GrupoSetorMapper.class));
    var localidades = new SubempresaLocalidadeService(repo.getRepository(SubempresaLocalidadeRepository.class),
        Mappers.getMapper(SubempresaLocalidadeMapper.class));
    for (Object resource : List.of(sectors, history, params, quadro, designacoes, radar, grupos, localidades)) {
      ReflectionTestUtils.setField(resource, "entityManager", session);
      ReflectionTestUtils.setField(resource, "resourceTenant", tenant);
      CanonicalResourceTestContext.exports(resource);
      ReflectionTestUtils.setField(resource, "statsProperties", new StatsProperties(true, 20, 100, StatsSupportMode.AUTO));
      ReflectionTestUtils.setField(resource, "statsEligibility", new StatsEligibility());
      ReflectionTestUtils.setField(resource, "statsQueryExecutor", new JpaStatsQueryExecutor());
    }
    var setorController = new SetorController(sectors, history, params, quadro, designacoes, radar);
    var controllers = List.of(setorController, new HistoricoSetorController(history),
        new SetorParametrizacaoErgonController(params), new CargoSetorController(quadro),
        new DesignacaoFuncaoController(designacoes, null, null), new RadarOrganogramaChefiaOcupacaoController(radar),
        new GrupoSetorController(grupos), new SubempresaLocalidadeController(localidades));
    for (Object controller : controllers) {
      ReflectionTestUtils.setField(controller, "paginationMaxSize", 200);
      ReflectionTestUtils.invokeMethod(controller, "initializeBasePath");
    }
    var mvc = MockMvcBuilders.standaloneSetup(controllers.toArray())
        .setControllerAdvice(new GlobalExceptionHandler()).build();
    var context = mvc.getDispatcherServlet().getWebApplicationContext();
    var mappings = context.getBean(RequestMappingHandlerMapping.class);
    var documents = mock(OpenApiDocumentService.class);
    when(documents.resolveGroupFromPath(anyString())).thenReturn("ergon");
    var operations = new OpenApiCanonicalOperationResolver(documents, mappings);
    var definitions = new AnnotationDrivenSurfaceDefinitionRegistry(mappings, context, operations,
        new FilteredSchemaReferenceResolver(), AnnotationConflictMode.FAIL);
    var catalog = new SurfaceCatalogService(definitions, new DefaultSurfaceAvailabilityEvaluator(),
        new DefaultSurfaceAvailabilityContextResolver());
    for (Object controller : controllers) {
      ReflectionTestUtils.setField(controller, "surfaceCatalogService", catalog);
    }
    SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
        "leitor", "", List.of(new SimpleGrantedAuthority("ERGON_READ"))));
    return mvc;
  }

  @Test void treeAndRelatedSurfacesUseTheAuthorizedCompositionAndTheWholeBranch() throws Exception {
    insert(1, "X8-GRAND", "Descendente de terceiro nível", "S8-1-DIR", "2020-01-01", null, "S8-1-DIR");
    insert(2, "X8-GRAND", "Descendente de outro órgão", "S8-2-DIR", "2020-01-01", null, "S8-2-DIR");
    var mvc = phase2Mvc();
    var tree = mvc.perform(get("/api/ergon/setores/arvore-organograma")
            .param("dataReferencia", "2021-06-01").param("tipoHierarquia", "4")
            .param("buscaAmpla", "S8-1").param("sort", "id,desc").param("page", "1").param("size", "1"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(2))
        .andExpect(jsonPath("$._links.surfaces.href").value(org.hamcrest.Matchers.containsString("/schemas/surfaces")))
        .andExpect(jsonPath("$.data.number").value(1))
        .andExpect(jsonPath("$.data.content[0].id").value("S8-1-DIR"))
        .andExpect(jsonPath("$.data.content[0].nome").value("Diretoria histórica 1"))
        .andExpect(jsonPath("$.data.content[0].setorPai").value("S8-1-RAIZ")).andReturn();
    var json = new com.fasterxml.jackson.databind.ObjectMapper();
    var ref = json.readTree(tree.getResponse().getContentAsByteArray()).at("/data/content/0/referenciaFicha").asText();
    mvc.perform(get(ApiPaths.SETORES + "/{id}/ficha-unidade", ref))
        .andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("Diretoria histórica 1"));
    mvc.perform(get(ApiPaths.SETORES + "/{id}/radar-subarvore", ref))
        .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(2))
        .andExpect(jsonPath("$.data.content[*].setor").value(org.hamcrest.Matchers.containsInAnyOrder("S8-1-DIR", "X8-GRAND")))
        .andExpect(jsonPath("$.data.content[*].tipoHierarquia").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is(4))));
    mvc.perform(get(ApiPaths.SETORES + "/{id}/surfaces", ref))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.surfaces[?(@.id=='radar-subarvore')].relatedResource.childParentField").value("raizSubarvore"))
        .andExpect(jsonPath("$.surfaces[?(@.id=='designacoes-chefia')].relatedResource.childParentField").value("setorId"));
    mvc.perform(post(ApiPaths.RADAR_ORGANOGRAMA_CHEFIAS_OCUPACAO + "/filter").contentType("application/json")
            .content("""
                {"raizSubarvore":"S8-1-RAIZ","dataReferencia":"2021-06-01","tipoHierarquia":4}
                """))
        .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(3))
        .andExpect(jsonPath("$.data.content[*].setor").value(
            org.hamcrest.Matchers.containsInAnyOrder("S8-1-RAIZ", "S8-1-DIR", "X8-GRAND")));
    // O outlet envia o identificador explícito de discovery no campo de filtro do filho.
    mvc.perform(post(ApiPaths.RADAR_ORGANOGRAMA_CHEFIAS_OCUPACAO + "/filter").contentType("application/json")
            .content("{\"raizSubarvore\":\"" + ref + "\"}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(2))
        .andExpect(jsonPath("$.data.content[*].tipoHierarquia").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is(4))));
    mvc.perform(post(ApiPaths.HISTORICO_SETORES + "/filter").contentType("application/json")
            .content("{\"setor\":\"" + ref + "\"}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(3));
    mvc.perform(post(ApiPaths.CARGOS_POR_SETOR + "/filter").contentType("application/json")
            .content("{\"setor\":\"" + ref + "\"}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.data.content[0].lotacaoMaximaAutorizada").value(8));
    mvc.perform(get(ApiPaths.SETORES + "/S8-1-DIR/historico-vigencias"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(3));
    mvc.perform(get(ApiPaths.SETORES + "/S8-1-SUB/parametrizacoes-ergon"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.data.content[0].setor").value("S8-1-SUB"));
    mvc.perform(get(ApiPaths.SETORES + "/S8-1-DIR/cargos-autorizados"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.data.content[0].lotacaoMaximaAutorizada").value(8));
    mvc.perform(get(ApiPaths.SETORES + "/S8-1-SUB/designacoes-chefia"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1))
        .andExpect(jsonPath("$.data.content[0].setorId").value("S8-1-SUB"))
        .andExpect(jsonPath("$.data.content[0].empCodigo").value(1));
    var sub = mvc.perform(get("/api/ergon/setores/arvore-organograma")
            .param("setor", "S8-1-SUB").param("dataReferencia", "2026-11-04"))
        .andExpect(status().isOk()).andReturn();
    String subRef = json.readTree(sub.getResponse().getContentAsByteArray()).at("/data/content/0/referenciaFicha").asText();
    mvc.perform(post(ApiPaths.DESIGNACOES + "/filter").contentType("application/json")
            .content("{\"setorId\":\"" + subRef + "\"}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1))
        .andExpect(jsonPath("$.data.content[0].setorId").value("S8-1-SUB"));
    mvc.perform(post(ApiPaths.SETORES_ERGON + "/filter").contentType("application/json")
            .content("{\"setor\":\"" + subRef + "\"}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1));
    when(tenant.orgao()).thenReturn(2);
    when(tenant.resolveTenant()).thenReturn(Optional.of("2"));
    mvc.perform(post(ApiPaths.DESIGNACOES + "/filter").contentType("application/json")
            .content("{\"setorId\":\"" + subRef + "\"}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(0));
    when(tenant.orgao()).thenReturn(1);
    when(tenant.resolveTenant()).thenReturn(Optional.of("1"));
    mvc.perform(get(ApiPaths.SETORES + "/S8-2-SUB/designacoes-chefia")).andExpect(status().isForbidden());
    mvc.perform(get("/api/ergon/setores/arvore-organograma").param("tipoHierarquia", "5"))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/api/ergon/setores/arvore-organograma").param("size", "201"))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test void everyOrganizationResourceExecutesScopedStatsExportsAndItsPublishedDetail() throws Exception {
    var mvc = phase2Mvc();
    var json = new com.fasterxml.jackson.databind.ObjectMapper();
    record Dataset(String path, String filter, String dimension, String distribution, String mode, String detail, int count) {}
    for (var data : List.of(
        new Dataset(ApiPaths.SETORES, "\"setor\":\"S8-1-DIR\"", "tipoSetor", "quantidadeSubordinadosDiretos", "HISTOGRAM", "ficha-unidade", 1),
        new Dataset(ApiPaths.HISTORICO_SETORES, "\"setor\":\"S8-1-DIR\"", "tipoSetor", "ufLotacao", "TERMS", "detail", 3),
        new Dataset(ApiPaths.SETORES_ERGON, "\"setor\":\"S8-1-RAIZ\"", "tipoAcesso", "grauRiscoAmbiental", "TERMS", "detail", 1),
        new Dataset(ApiPaths.CARGOS_POR_SETOR, "\"setor\":\"S8-1-DIR\"", "tipoControleQuadro", "lotacaoMaximaAutorizada", "HISTOGRAM", "detail", 1),
        new Dataset(ApiPaths.SUBEMPRESAS_LOCALIDADES, "\"codigo\":\"S8-UG\"", "naturezaUnidade", "uf", "TERMS", "detail", 1),
        new Dataset(ApiPaths.GRUPOS_SETORES, "\"sigla\":\"S8-REG\"", "tipoAgrupamento", "ufAbrangencia", "TERMS", "detail", 1),
        new Dataset(ApiPaths.RADAR_ORGANOGRAMA_CHEFIAS_OCUPACAO, "\"setor\":\"S8-1-DIR\",\"dataReferencia\":\"2026-11-04\"",
            "situacaoComandoSetor", "vagasAutorizadasSetor", "HISTOGRAM", "detail", 1))) {
      String filter = "{" + data.filter() + "}";
      var list = mvc.perform(post(data.path() + "/filter").contentType("application/json").content(filter))
          .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(data.count())).andReturn();
      String id = json.readTree(list.getResponse().getContentAsByteArray()).at("/data/content/0/id").asText();
      mvc.perform(get(data.path() + "/{id}/" + data.detail(), id))
          .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id)).andExpect(jsonPath("$.empCodigo").value(1));
      var nominal = mvc.perform(post(data.path() + "/stats/group-by").contentType("application/json")
              .content("{\"filter\":" + filter + ",\"field\":\"id\",\"metric\":{\"operation\":\"COUNT\"}}"))
          .andExpect(status().isOk()).andReturn();
      assertThat(json.readTree(nominal.getResponse().getContentAsByteArray()).at("/data/buckets").size())
          .as(data.path()).isEqualTo(data.count());
      mvc.perform(post(data.path() + "/stats/group-by").contentType("application/json")
              .content("{\"filter\":{\"empCodigo\":2," + data.filter() + "},\"field\":\"id\",\"metric\":{\"operation\":\"COUNT\"}}"))
          .andExpect(status().isOk()).andExpect(jsonPath("$.data.buckets").isEmpty());
      mvc.perform(post(data.path() + "/stats/timeseries").contentType("application/json")
              .content("""
                  {"filter":%s,"field":"dtini","granularity":"MONTH","metric":{"operation":"COUNT"},
                   "from":"2020-01-01","to":"2026-12-31"}
                  """.formatted(filter)))
          .andExpect(status().isOk()).andExpect(jsonPath("$.data.points").isNotEmpty());
      mvc.perform(post(data.path() + "/stats/distribution").contentType("application/json")
              .content("""
                  {"filter":%s,"field":"%s","mode":"%s","metric":{"operation":"COUNT"},"bucketSize":5}
                  """.formatted(filter, data.distribution(), data.mode())))
          .andExpect(status().isOk()).andExpect(jsonPath("$.data.mode").value(data.mode()));
      mvc.perform(post(data.path() + "/stats/comparison").contentType("application/json")
              .content("""
                  {"filter":%s,"field":"%s","periodField":"dtini","metrics":[{"operation":"COUNT"}],
                   "period":{"from":"2026-01-01","to":"2026-12-31","timezone":"America/Sao_Paulo"}}
                  """.formatted(filter, data.dimension())))
          .andExpect(status().isOk()).andExpect(jsonPath("$.data.periodField").value("dtini"));
      for (var format : List.of("CSV", "JSON", "EXCEL")) {
        var export = mvc.perform(post(data.path() + "/export").contentType("application/json")
                .content("""
                    {"format":"%s","scope":"FILTERED","filters":%s,"applyFormatting":true}
                    """.formatted(format, filter)))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Export-Row-Count", Integer.toString(data.count())))
            .andReturn();
        assertThat(export.getResponse().getContentAsByteArray()).isNotEmpty();
      }
    }
    String radar = ApiPaths.RADAR_ORGANOGRAMA_CHEFIAS_OCUPACAO;
    mvc.perform(post(radar + "/filter").contentType("application/json")
            .content("{\"setor\":\"S8-1-DIR\",\"dataReferencia\":\"2026-11-04\"}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.data.content[0].vagasAutorizadasSetor").value(8))
        .andExpect(jsonPath("$.data.content[0].vagasOcupadasSetor").doesNotExist())
        .andExpect(jsonPath("$.data.content[0].escopoApuracao").value(
            org.hamcrest.Matchers.containsString("PROVIMENTO_EXERCICIO_FOLHA_E_COMANDO_POR_CARGO_NAO_INTEGRADOS")));
    mvc.perform(post(radar + "/export").contentType("application/json")
            .content("""
                {"format":"JSON","fields":[{"key":"chefeTitularNome","label":"Titular"}]}
                """))
        .andExpect(status().isBadRequest());
    mvc.perform(post(radar + "/export").contentType("application/json")
            .content("{\"format\":\"CSV\",\"maxRows\":1001}"))
        .andExpect(status().isBadRequest());
    mvc.perform(post(ApiPaths.SETORES + "/export").contentType("application/json")
            .content("""
                {"format":"JSON","scope":"SELECTED","selection":{"keyField":"id","selectedKeys":["S8-1-DIR","S8-2-DIR"]}}
                """))
        .andExpect(status().isOk()).andExpect(header().string("X-Export-Row-Count", "1"));
  }

  @Test void parentChangeRebuildsChildPathInAllFiveHierarchiesWithoutChangingChildPeriod() {
    insert(1,"X8-R1","Raiz um",null,"2020-01-01",null,null);
    insert(1,"X8-R2","Raiz dois",null,"2020-01-01",null,null);
    insert(1,"X8-P","Pai antigo","X8-R1","2020-01-01","2024-12-31","X8-R1");
    insert(1,"X8-P","Pai novo","X8-R2","2025-01-01",null,"X8-R2");
    insert(1,"X8-C","Filho","X8-P","2020-01-01",null,"X8-P");
    for(int kind=0;kind<=4;kind++)for(var entry:Map.of("2024-12-31","X8-R1","2025-01-01","X8-R2").entrySet()) {
      var f=new HistoricoSetorFilterDTO();f.setDataReferencia(LocalDate.parse(entry.getKey()));f.setTipoHierarquia(kind);f.setSetor("X8-C");
      assertThat(history.filter(f,Pageable.unpaged(),List.of()).getContent()).singleElement().satisfies(d->{
        assertThat(d.getNivelHierarquico()).isEqualTo(3);assertThat(d.getCaminhoHierarquico()).isEqualTo(path(entry.getValue(),"X8-P","X8-C"));
      });
      var rows=session.createNativeQuery("select caminho_hierarquico from radar_organograma_chefias_ocupacao where emp_codigo=1 and setor='X8-C' and tipo_hierarquia=:k and dtini<=:d and (dtfim is null or dtfim>=:d)",String.class)
          .setParameter("k",kind).setParameter("d",f.getDataReferencia()).getResultList();
      assertThat(rows).containsExactly(path(entry.getValue(),"X8-P","X8-C"));
    }
  }

  @Test void auxiliaryParentSelectionAndDelimitedPathsRejectSiblingPrefixesAndMalformedInput() {
    insert(1,"X8-A","A",null,"2020-01-01",null,null);
    insert(1,"X8-AB","AB",null,"2020-01-01",null,null);
    insert(1,"X8/%_","Especial","X8-A","2020-01-01",null,"X8-AB");
    var f=new HistoricoSetorFilterDTO();f.setDataReferencia(LocalDate.parse("2026-01-01"));f.setCaminhoSubarvore(path("X8-A"));
    assertThat(history.filter(f,Pageable.unpaged(),List.of()).getContent()).extracting(HistoricoSetorResponseDTO::getSetor).containsExactlyInAnyOrder("X8-A","X8/%_");
    f.setIncluirSubordinados(false);assertThat(history.filter(f,Pageable.unpaged(),List.of()).getContent()).hasSize(1);
    f.setCaminhoSubarvore(null);f.setTipoHierarquia(4);f.setSetorPai("X8-AB");
    assertThat(history.filter(f,Pageable.unpaged(),List.of()).getContent()).extracting(HistoricoSetorResponseDTO::getSetor).containsExactly("X8/%_");
    f.setCaminhoSubarvore("/58382d41");assertThatThrownBy(()->history.filter(f,Pageable.unpaged(),List.of())).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
  }

  @Test void cyclesOrMissingParentsAreNotReinterpretedAsRoots() {
    insert(1,"X8-C1","Ciclo um","X8-C2","2020-01-01",null,null);
    insert(1,"X8-C2","Ciclo dois","X8-C1","2020-01-01",null,null);
    insert(1,"X8-O","Orfao","X8-MISSING","2020-01-01",null,null);
    var f=new HistoricoSetorFilterDTO();f.setDataReferencia(LocalDate.parse("2026-01-01"));f.setBuscaAmpla("X8-");
    assertThat(history.filter(f,Pageable.unpaged(),List.of()).getContent()).isEmpty();
    f.setDataReferencia(null);assertThat(history.filter(f,Pageable.unpaged(),List.of()).getContent()).hasSize(3);
  }

  @Test void correlatedDisplayNamesUseTheOwningOrganizationAndPeriod() {
    var rows=session.createQuery("from CargoSetor where setor like 'S8-%'",CargoSetor.class).getResultList();
    assertThat(rows).hasSize(4);
    for(var r:rows) {
      assertThat(r.getSetorNome()).endsWith(" "+r.getEmpCodigo());
      if(r.getSetor().endsWith("DIR"))assertThat(r.getSetorNome()).contains("atual");
      assertThat(r.getCargoNome()).isNotBlank();
    }
    assertThat(session.createQuery("from SetorParametrizacaoErgon where setor like 'S8-%'",SetorParametrizacaoErgon.class).getResultList()).hasSize(12).allSatisfy(r->assertThat(r.getSetorNome()).endsWith(" "+r.getEmpCodigo()));
  }

  @Test void radarHasOneSnapshotAndKeepsUnavailableMetricsUnknown() {
    var f=new RadarOrganogramaChefiaOcupacaoFilterDTO();f.setSetor("S8-1-DIR");f.setDataReferencia(LocalDate.parse("2026-01-01"));
    var spec=new OrganogramaRadarQuery().buildSpecification(f,Pageable.unpaged()).spec();
    var cb=session.getCriteriaBuilder();var q=cb.createQuery(RadarOrganogramaChefiaOcupacao.class);var root=q.from(RadarOrganogramaChefiaOcupacao.class);
    assertThat(session.createQuery(q.where(spec.toPredicate(root,q,cb))).getResultList()).singleElement().satisfies(r->{
      assertThat(r.getNomeSetor()).isEqualTo("Diretoria atual 1");assertThat(r.getVagasAutorizadasSetor()).isEqualTo(8L);
      assertThat(r.getVagasOcupadasSetor()).isNull();assertThat(r.getApropriadosFolhaSetorCount()).isNull();
    });
  }

  private void insert(int org,String code,String name,String parent,String start,String end,String auxiliary) {
    session.createNativeMutationQuery("""
      insert into historico_setores(emp_codigo,setor,nome,setor_pai,dtini,dtfim,tipo_setor,caminho_hierarquico,
      setor_pai_hier1,setor_pai_hier2,setor_pai_hier3,setor_pai_hier4)
      values(:o,:c,:n,:p,cast(:s as date),cast(:e as date),'DIRETORIA','/stale/',:a,:a,:a,:a)
      """).setParameter("o",org).setParameter("c",code).setParameter("n",name).setParameter("p",parent)
        .setParameter("s",start).setParameter("e",end).setParameter("a",auxiliary).executeUpdate();
  }
  private static String path(String... codes) {return "/"+String.join("/",Arrays.stream(codes).map(c->HexFormat.of().formatHex(c.getBytes(java.nio.charset.StandardCharsets.UTF_8))).toList())+"/";}
}
