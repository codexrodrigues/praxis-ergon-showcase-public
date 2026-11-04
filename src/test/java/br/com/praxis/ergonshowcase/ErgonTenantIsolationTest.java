package br.com.praxis.ergonshowcase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import br.com.praxis.ergonshowcase.bulk.ErgonBulkDomain;
import br.com.praxis.ergonshowcase.bulk.ErgonBulkDomain.Resource;
import br.com.praxis.ergonshowcase.config.TenantContext;
import br.com.praxis.ergonshowcase.domain.*;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.mapper.*;
import br.com.praxis.ergonshowcase.repository.*;
import br.com.praxis.ergonshowcase.service.*;
import jakarta.persistence.*;
import jakarta.persistence.criteria.*;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.praxisplatform.uischema.concurrency.ResourceVersionUpdatePrecondition;
import org.praxisplatform.uischema.exporting.*;
import org.praxisplatform.uischema.service.base.ResourceFilterAccessScope;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.server.ResponseStatusException;

/** Regressões C-04/C-05/H-05 com serviços reais e persistência simulada. */
@SuppressWarnings({"unchecked", "rawtypes"})
class ErgonTenantIsolationTest {
  final TenantContext tenant = new TenantContext();
  @AfterEach void cleanup() { RequestContextHolder.resetRequestAttributes(); }

  static void forbidden(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
    assertThatThrownBy(call).isInstanceOfSatisfying(ResponseStatusException.class,
        failure -> assertThat(failure.getStatusCode().value()).isEqualTo(403));
  }
  static Orgao organization(int id) { var row = new Orgao(); row.setEmpresa(id); row.setNome("Órgão " + id); return row; }

  @Test void aForeignEmploymentCannotSupplyFactsForAnAbsence() {
    var em = mock(EntityManager.class);
    var service = new AfastamentoService(mock(AfastamentoRepository.class), new AfastamentoMapper(), tenant);
    CanonicalResourceTestContext.wire(service, em);
    var draft = new Afastamento();
    draft.setEmpCodigo(1);
    draft.setNumfunc(1001L);
    draft.setNumvinc(2);
    var id = new VinculoId(1001L, 2);
    var foreign = new Vinculo();
    foreign.setId(id);
    foreign.setEmpCodigo(2);
    when(em.find(Vinculo.class, id)).thenReturn(foreign);
    forbidden(() -> service.buildAfastamentoRuleContext(draft, false));
    verify(em, never()).find(eq(ServidorPessoa.class), any());
    verify(em, never()).createQuery(anyString(), eq(Afastamento.class));
  }

  @Test void documentOptionsAreOrganizationScopedAndGeneralTablesDoNotAdvertiseAnUnsupportedFilter() {
    var options = new br.com.praxis.ergonshowcase.config.OptionSourcesConfig().options();
    var documents = options.resolve(DocumentoLegal.class, br.com.praxis.ergonshowcase.constants.ApiPaths.DOCUMENTO_SOURCE).orElseThrow();
    var tables = options.resolve(Tabela.class, br.com.praxis.ergonshowcase.constants.ApiPaths.TABELA_SOURCE).orElseThrow();
    assertThat(documents.dependsOn()).containsExactly("empCodigo");
    assertThat(documents.dependencyFilterMap()).containsExactlyEntriesOf(Map.of("empCodigo", "empCodigo"));
    assertThat(tables.dependsOn()).isEmpty();
    assertThat(tables.dependencyFilterMap()).isEmpty();
  }

  @Test void c04DefaultOrganizationProtectsGetLocateExportAndExplicitIncludes() {
    assertThat(tenant.explicitOrgao()).isEmpty();
    var repository = mock(OrgaoRepository.class);
    var service = new OrgaoService(repository, org.mapstruct.factory.Mappers.getMapper(OrgaoMapper.class), tenant);
    var local = organization(1); var foreign = organization(2);
    when(repository.findById(1)).thenReturn(Optional.of(local));
    when(repository.findById(2)).thenReturn(Optional.of(foreign));
    forbidden(() -> service.findById(2));
    forbidden(() -> service.locate(new OrgaoFilterDTO(), Sort.by("empresa"), 2));
    verify(repository, never()).count(any(Specification.class));

    when(repository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(foreign));
    when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(foreign)));
    CanonicalResourceTestContext.exports(service);
    var export = new CollectionExportRequest<OrgaoFilterDTO>(null, null, "/api/ergon/orgaos",
        CollectionExportFormat.JSON, CollectionExportScope.ALL, null, List.of(), new OrgaoFilterDTO(),
        null, null, Map.of(), true, false, 10, null, Map.of());
    forbidden(() -> service.exportCollection(export));

    when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());
    when(repository.findAll(any(Specification.class))).thenReturn(List.of(foreign));
    forbidden(() -> service.filter(new OrgaoFilterDTO(), PageRequest.of(0, 10), List.of(2)));
    assertThat(service.findById(1).body().getId()).isEqualTo(1);
  }

  @Test void c04NormalAndBulkSelectionsAlwaysContainTheSameEffectiveOrganizationPredicate() {
    var repository = mock(OrgaoRepository.class);
    var service = new OrgaoService(repository, org.mapstruct.factory.Mappers.getMapper(OrgaoMapper.class), tenant);
    when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());
    service.filter(new OrgaoFilterDTO(), PageRequest.of(0, 10), List.of());
    service.selectBulkIdentities(new OrgaoFilterDTO(), List.of(1, 2), List.of(), 10);
    var captor = ArgumentCaptor.forClass(Specification.class);
    verify(repository, times(2)).findAll(captor.capture(), any(Pageable.class));
    for (var spec : captor.getAllValues()) {
      Root root = mock(Root.class, RETURNS_DEEP_STUBS);
      when(root.getJavaType()).thenReturn(Orgao.class);
      var query = mock(CriteriaQuery.class);
      var cb = mock(CriteriaBuilder.class);
      spec.toPredicate(root, query, cb);
      verify(cb, atLeastOnce()).equal(root.get("empresa"), 1);
    }
    ResourceFilterAccessScope<Orgao> normal = ReflectionTestUtils.invokeMethod(service, "resolveResourceFilterAccessScope");
    ResourceFilterAccessScope<Orgao> bulk = ReflectionTestUtils.invokeMethod(service, "resolveBulkResourceFilterAccessScope");
    assertThat(normal.mode()).isEqualTo(ResourceFilterAccessScope.Mode.RESTRICTED);
    assertThat(bulk.mode()).isEqualTo(normal.mode());
  }

  @Test void c04ReadOnlyAndGlobalRecordPoliciesUseTheEffectiveTenantToo() {
    var repository = mock(RadarTempoServicoLicencaEspecialRepository.class);
    var service = new RadarTempoServicoLicencaEspecialService(repository, org.mapstruct.factory.Mappers.getMapper(RadarTempoServicoLicencaEspecialMapper.class), tenant);
    var row = new RadarTempoServicoLicencaEspecial(); row.setId(8L); row.setEmpCodigo(2);
    when(repository.findById(8L)).thenReturn(Optional.of(row));
    forbidden(() -> service.findById(8L));
    ResourceFilterAccessScope<?> scope = ReflectionTestUtils.invokeMethod(service, "resolveResourceFilterAccessScope");
    assertThat(scope.mode()).isEqualTo(ResourceFilterAccessScope.Mode.RESTRICTED);
    var global = new DefinicaoCampoCustomizado();
    assertThatCode(() -> ErgonTenantAccess.assertAllowed(global, tenant, null, true)).doesNotThrowAnyException();
    forbidden(() -> ErgonTenantAccess.assertAllowed(global, tenant, null, false));
    global.setEmpCodigo(2);
    forbidden(() -> ErgonTenantAccess.assertAllowed(global, tenant, null, true));
  }

  @Test void c04DependentsRequireAPersonWithAnEmploymentInTheCurrentOrganizationIncludingBulk() {
    var em = mock(EntityManager.class);
    TypedQuery<Long> membership = mock(TypedQuery.class, RETURNS_SELF);
    when(em.createQuery(contains("v.id.numfunc=:person and v.empCodigo=:org"), eq(Long.class))).thenReturn(membership);
    var dependent = new Dependente(); dependent.setId(new DependenteId(1001L, 1));
    var repository = mock(DependenteRepository.class);
    when(repository.findById(dependent.getId())).thenReturn(Optional.of(dependent));
    var service = new DependenteService(repository, new DependenteMapper());
    ReflectionTestUtils.setField(service, "entityManager", em);
    var bulk = new ErgonBulkDomain(); ReflectionTestUtils.setField(bulk, "em", em); ReflectionTestUtils.setField(bulk, "tenant", tenant);
    when(membership.getSingleResult()).thenReturn(0L);
    forbidden(() -> service.findById(dependent.getId()));
    assertThat(bulk.scope(Resource.DEPENDENTES, dependent)).isFalse();
    when(membership.getSingleResult()).thenReturn(1L);
    assertThatCode(() -> service.findById(dependent.getId())).doesNotThrowAnyException();
    assertThat(bulk.scope(Resource.DEPENDENTES, dependent)).isTrue();
    verify(membership, atLeastOnce()).setParameter("org", 1);
    ResourceFilterAccessScope<Dependente> scope = ReflectionTestUtils.invokeMethod(service, "resolveResourceFilterAccessScope");
    Root<Dependente> root = mock(Root.class, RETURNS_DEEP_STUBS);
    CriteriaQuery query = mock(CriteriaQuery.class);
    Subquery subquery = mock(Subquery.class, RETURNS_SELF);
    Root<Vinculo> vinculos = mock(Root.class, RETURNS_DEEP_STUBS);
    when(query.subquery(Integer.class)).thenReturn(subquery);
    when(subquery.from(Vinculo.class)).thenReturn(vinculos);
    var cb = mock(CriteriaBuilder.class);
    scope.specification().toPredicate(root, query, cb);
    verify(cb).equal(vinculos.get("empCodigo"), 1);
    verify(cb).exists(subquery);
  }

  @Test void c04CreationCannotWriteAForeignOrganizationOrItsDependents() {
    var organizationRepository = mock(OrgaoRepository.class);
    var organizations = new OrgaoService(organizationRepository,
        org.mapstruct.factory.Mappers.getMapper(OrgaoMapper.class), tenant);
    var organization = new CreateOrgaoDTO(); organization.setEmpresa(2);
    forbidden(() -> organizations.create(organization));
    verify(organizationRepository, never()).save(any());

    var repository = mock(DependenteRepository.class);
    var dependents = new DependenteService(repository, new DependenteMapper());
    var em = mock(EntityManager.class);
    TypedQuery<Long> membership = mock(TypedQuery.class, RETURNS_SELF);
    when(em.createQuery(anyString(), eq(Long.class))).thenReturn(membership);
    when(membership.getSingleResult()).thenReturn(0L);
    ReflectionTestUtils.setField(dependents, "entityManager", em);
    var input = new CreateDependenteDTO(); input.setNumfunc(2002L); input.setNumdep(1);
    forbidden(() -> dependents.create(input));
    verify(repository, never()).save(any());
  }

  @Test void c04BulkReadAuthorizationRequiresTheDependentOwnerInTheCurrentOrganization() throws Exception {
    var operations = new br.com.praxis.ergonshowcase.bulk.ErgonBulkOperations();
    var infrastructure = mock(org.praxisplatform.uischema.bulk.BulkExecutionInfrastructure.class);
    var source = mock(javax.sql.DataSource.class);
    var connection = mock(java.sql.Connection.class);
    var statement = mock(java.sql.PreparedStatement.class);
    var rows = mock(java.sql.ResultSet.class);
    when(infrastructure.dataSource()).thenReturn(source);
    when(connection.prepareStatement(anyString())).thenReturn(statement);
    when(statement.executeQuery()).thenReturn(rows); when(rows.next()).thenReturn(true);
    ReflectionTestUtils.setField(operations, "infrastructure", infrastructure);
    ReflectionTestUtils.setField(operations, "tenant", tenant);
    org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
        new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("operator", "unused",
            List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ERGON_READ"))));
    var provider = (org.praxisplatform.uischema.bulk.BulkReadAuthorizationProvider)
        ReflectionTestUtils.invokeMethod(operations, "authorization", Resource.DEPENDENTES, "ergon.dependentes.bulk.uniform");
    var context = new org.praxisplatform.uischema.bulk.BulkReadAuthorizationProvider.Context("p5-test",
        "administracao-pessoal.dependentes", "ergon.dependentes.bulk.uniform", "operator|orgao=1");
    var targets = List.of(new org.praxisplatform.uischema.bulk.BulkReadAuthorizationProvider.Target(0, "1001:1",
        new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode().put("orgao", 1)));
    org.springframework.transaction.support.TransactionSynchronizationManager.bindResource(source,
        new org.springframework.jdbc.datasource.ConnectionHolder(connection));
    try {
      when(rows.getInt(1)).thenReturn(0);
      assertThat(provider.authorize(context, "operator|orgao=1", targets, java.time.Duration.ofSeconds(2)).state())
          .isEqualTo(org.praxisplatform.uischema.bulk.BulkReadAuthorizationProvider.State.DENIED_OR_REDUCED);
      when(rows.getInt(1)).thenReturn(1);
      assertThat(provider.authorize(context, "operator|orgao=1", targets, java.time.Duration.ofSeconds(2)).state())
          .isEqualTo(org.praxisplatform.uischema.bulk.BulkReadAuthorizationProvider.State.AUTHORIZED_ALL);
      var sql = ArgumentCaptor.forClass(String.class);
      verify(connection, times(2)).prepareStatement(sql.capture());
      assertThat(sql.getValue()).contains("exists (select 1 from public.vinculos v", "v.numfunc=t.numfunc",
          "v.emp_codigo=?", "v.situacao in ('ATIVO','AFASTADO','CEDIDO')");
      verify(statement, times(2)).setInt(2, 1);
    } finally {
      org.springframework.transaction.support.TransactionSynchronizationManager.unbindResource(source);
      org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }
  }

  @Test void h05ForeignCommandAndBulkTargetsAreRejectedBeforeAnyExclusiveLock() {
    var em = mock(EntityManager.class);
    var commands = new ErgonCommands();
    ReflectionTestUtils.setField(commands, "em", em); ReflectionTestUtils.setField(commands, "tenant", tenant);
    var employment = new Vinculo(); employment.setId(new VinculoId(1001L, 1)); employment.setEmpCodigo(2);
    when(em.find(Vinculo.class, employment.getId())).thenReturn(employment);
    forbidden(() -> ReflectionTestUtils.invokeMethod(commands, "vinculo", employment.getId(), mock(ResourceVersionUpdatePrecondition.class), "ATIVO"));
    when(em.find(Orgao.class, 2)).thenReturn(organization(2));
    forbidden(() -> commands.ativarOrgao(2, null, mock(ResourceVersionUpdatePrecondition.class)));
    var bulk = new ErgonBulkDomain(); ReflectionTestUtils.setField(bulk, "em", em); ReflectionTestUtils.setField(bulk, "tenant", tenant);
    when(em.unwrap(org.hibernate.Session.class)).thenReturn(mock(org.hibernate.Session.class));
    assertThatThrownBy(() -> bulk.load(Resource.VINCULOS,
        org.praxisplatform.uischema.id.CompositeIdSupport.encode(employment.getId()), true)).isInstanceOf(SecurityException.class);
    verify(em, never()).lock(any(), any());
    verify(em, never()).lock(any(), any(), anyMap());
    verify(em, never()).refresh(any());
  }

  @Test void c05VacationPreviewsDenyForeignEmploymentAndAcquisitionBeforeReadingTheirFacts() {
    var em = mock(EntityManager.class);
    var service = new ErgonDeterminationService(tenant, null); ReflectionTestUtils.setField(service, "em", em);
    var employment = new Vinculo(); employment.setEmpCodigo(2);
    when(em.find(eq(Vinculo.class), any())).thenReturn(employment);
    var period = new PeriodoAquisitivoPreviaDraftDTO(); period.setNumfunc(1001L); period.setNumvinc(1);
    forbidden(() -> service.previaPeriodoAquisitivo(period));
    var parent = new PeriodoAquisitivoFerias(); parent.setEmpCodigo(2);
    when(em.find(PeriodoAquisitivoFerias.class, 7L)).thenReturn(parent);
    var parcel = new FeriasGozoPreviaDraftDTO(); parcel.setPeriodoAquisitivoId(7L);
    forbidden(() -> service.previaFeriasGozo(parcel));
    verify(em, never()).createQuery(anyString(), any(Class.class));
  }

  @Test void c05AbsencesAndSuspensionsAreQueriedAndCountedOnlyWithinTheCurrentOrganization() {
    var em = mock(EntityManager.class);
    var service = new ErgonDeterminationService(tenant, null); ReflectionTestUtils.setField(service, "em", em);
    var employment = new Vinculo(); employment.setEmpCodigo(1);
    when(em.find(eq(Vinculo.class), any())).thenReturn(employment);
    TypedQuery<Frequencia> frequency = mock(TypedQuery.class, RETURNS_SELF);
    TypedQuery<Afastamento> leave = mock(TypedQuery.class, RETURNS_SELF);
    when(em.createQuery(anyString(), eq(Frequencia.class))).thenReturn(frequency);
    when(em.createQuery(anyString(), eq(Afastamento.class))).thenReturn(leave);
    var local = new Frequencia(); local.setEmpCodigo(1); local.setTipofreq("FALTA"); local.setDtini(LocalDate.of(2024, 2, 2));
    var foreign = new Frequencia(); foreign.setEmpCodigo(2); foreign.setTipofreq("FALTA"); foreign.setDtini(LocalDate.of(2024, 2, 3));
    var localLeave = new Afastamento(); localLeave.setEmpCodigo(1); localLeave.setDtini(LocalDate.of(2024, 3, 1)); localLeave.setDtfim(LocalDate.of(2024, 3, 2));
    var foreignLeave = new Afastamento(); foreignLeave.setEmpCodigo(2); foreignLeave.setDtini(LocalDate.of(2024, 4, 1)); foreignLeave.setDtfim(LocalDate.of(2024, 4, 20));
    when(frequency.getResultList()).thenReturn(List.of(local, foreign)); when(leave.getResultList()).thenReturn(List.of(localLeave, foreignLeave));
    var draft = new PeriodoAquisitivoPreviaDraftDTO(); draft.setNumfunc(1001L); draft.setNumvinc(1);
    draft.setDtini(LocalDate.of(2024, 1, 1)); draft.setDtfim(LocalDate.of(2024, 12, 31));
    var preview = service.previaPeriodoAquisitivo(draft);
    assertThat(preview.getFaltasApuradas()).isEqualTo(1);
    assertThat(preview.getDiasAfastamentoSuspensivo()).isEqualTo(2);
    verify(em).createQuery(contains("f.empCodigo=:org"), eq(Frequencia.class));
    verify(em).createQuery(contains("a.empCodigo=:org"), eq(Afastamento.class));
    verify(frequency).setParameter("org", 1); verify(leave).setParameter("org", 1);
  }
}
