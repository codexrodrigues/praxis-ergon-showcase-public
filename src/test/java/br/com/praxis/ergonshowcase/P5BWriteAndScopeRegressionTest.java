package br.com.praxis.ergonshowcase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import br.com.praxis.ergonshowcase.config.*;
import br.com.praxis.ergonshowcase.domain.*;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.mapper.DependenteMapper;
import br.com.praxis.ergonshowcase.repository.DependenteRepository;
import br.com.praxis.ergonshowcase.service.*;
import jakarta.persistence.*;
import jakarta.validation.Validation;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.praxisplatform.uischema.customfields.*;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

@SuppressWarnings({"rawtypes", "unchecked"})
class P5BWriteAndScopeRegressionTest {
  @Test void crossOrganizationVinculoCreateFailsBeforeCustomFieldsOrDomainReads() {
    var repository = mock(br.com.praxis.ergonshowcase.repository.VinculoRepository.class);
    var mapper = mock(br.com.praxis.ergonshowcase.mapper.VinculoMapper.class);
    var validator = mock(CustomFieldValidator.class);
    var em = mock(EntityManager.class);
    var service = new VinculoService(repository, mapper, new TenantContext());
    ReflectionTestUtils.setField(service, "customFieldValidator", validator);
    ReflectionTestUtils.setField(service, "entityManager", em);
    var input = new CreateVinculoDTO();
    input.setEmpCodigo(2);

    var denied = assertThrows(org.springframework.web.server.ResponseStatusException.class,
        () -> service.create(input));
    assertEquals(403, denied.getStatusCode().value());
    verifyNoInteractions(validator, mapper, em, repository);
  }

  @Test void editingAnExistingSaleMustNotDebitItsOwnConsumptionAgainInThePreview() {
    var fixture = new TempoLicencaCrudContractTest.Fixture();
    var service = (VendaLicencaEspecialService) fixture.service(3)[0];
    var harness = new TempoLicencaCrudContractTest();
    var input = (CreateVendaLicencaEspecialDTO) harness.create(3);
    fixture.pa.setDenDiasAbonoLicEsp(1);
    input.setDiasVendidos(60);
    var created = service.create(input).body();
    assertEquals(30, fixture.pa.getSaldoDias());
    var update = new UpdateVendaLicencaEspecialDTO(); BeanUtils.copyProperties(created, update);
    assertDoesNotThrow(() -> service.update(91L, update, harness.precondition(0)));
    var preview = new TempoLicencaPreviewService(fixture.tenant);
    ReflectionTestUtils.setField(preview, "em", fixture.em);
    var result = assertDoesNotThrow(() -> preview.venda(new TempoLicencaPrevia.Venda(20L, 60, 91L)),
        "EDIT unchanged 60/90 is a valid write; preview must not reject it against remaining 30.");
    assertEquals(30, result.diasPrevistos());
  }

  @Test void linkedAbonoFinancialPatchMustRunTheNativeCrossFieldRuleBeforeSaving() {
    TempoLicencaIssue10RegressionTest.validator();
    try {
      var harness = new TempoLicencaIssue10RegressionTest();
      var journey = harness.homologated("G");
      journey.abono().setValorIndenizacao(new BigDecimal("50"));
      journey.abono().setCustomFields(new LinkedHashMap<>(Map.of("extra", 1)));
      var descriptor = P4CustomFieldAdoptionRegressionTest.descriptor(Map.of("<=",
          List.of(Map.of("var", "draft.valorIndenizacao"), 100)));
      var reads = new AtomicInteger();
      var validator = new CustomFieldValidator((r, t, s) -> { reads.incrementAndGet(); return List.of(descriptor); });
      ReflectionTestUtils.setField(journey.venda(), "customFieldValidator", validator);
      var fullCandidate = new UpdateVendaLicencaEspecialDTO();
      BeanUtils.copyProperties(journey.abono(), fullCandidate); fullCandidate.setValorIndenizacao(new BigDecimal("200"));
      assertThrows(CustomFieldValidationException.class, () -> validator.prepare(fullCandidate,
          "administracao-pessoal.vendas-licenca-especial", "1", "request")
          .validateForUpdate(journey.abono(), fullCandidate));
      reads.set(0); clearInvocations(journey.repository());
      var patch = new VendaLicencaEspecialLiquidacaoPatchDTO(); patch.setValorIndenizacao(new BigDecimal("200"));
      assertThrows(CustomFieldValidationException.class,
          () -> {
            var accepted = journey.venda().patch(journey.abono().getId(), patch, harness.version(journey.abono().getId(), 0));
            long saves = mockingDetails(journey.repository()).getInvocations().stream()
                .filter(call -> call.getMethod().getName().equals("save")).count();
            System.out.println("Linked abono financial PATCH accepted value=" + accepted.body().getValorIndenizacao()
                + "; native definition reads=" + reads.get() + "; saves=" + saves);
          },
          "The specialized PATCH must reject the same forbidden financial draft as the native validator.");
      verify(journey.repository(), never()).save(any());
      assertTrue(reads.get() > 0);
      assertEquals(new BigDecimal("50"), journey.abono().getValorIndenizacao());
    } finally { TempoLicencaIssue10RegressionTest.closeValidator(); }
  }

  @Test void dependentPageMustNotRecheckTheSameMembershipOncePerRow() {
    var repository = mock(DependenteRepository.class);
    var service = new DependenteService(repository, new DependenteMapper());
    var em = mock(EntityManager.class);
    TypedQuery<Long> membership = mock(TypedQuery.class, RETURNS_SELF);
    when(em.createQuery(anyString(), eq(Long.class))).thenReturn(membership);
    when(membership.getSingleResult()).thenReturn(1L);
    when(membership.getResultList()).thenReturn(List.of(1001L));
    ReflectionTestUtils.setField(service, "entityManager", em);
    var rows = new ArrayList<Dependente>();
    for (int index = 1; index <= 100; index++) {
      var row = new Dependente(); row.setId(new DependenteId(1001L, index)); row.setNome("Dependent " + index); rows.add(row);
    }
    when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(rows));
    assertEquals(100, service.filter(new DependenteFilterDTO(), PageRequest.of(0, 100), List.of()).getContent().size());
    long count = mockingDetails(em).getInvocations().stream().filter(call -> call.getMethod().getName().equals("createQuery")).count();
    System.out.println("Membership SELECTs for 100 rows belonging to one person: " + count);
    assertTrue(count <= 1, "The scoped page must not add 100 repeated membership queries for the same owner.");
  }

  @Test void membershipAuthorizationIsNotCachedAcrossOrganizationsAndStillProtectsIndividualReads() {
    var em = mock(EntityManager.class);
    TypedQuery<Long> query = mock(TypedQuery.class, RETURNS_SELF);
    when(em.createQuery(anyString(), eq(Long.class))).thenReturn(query);
    when(query.getResultList()).thenReturn(List.of(1001L), List.of());
    when(query.getSingleResult()).thenReturn(0L);
    var tenant = mock(TenantContext.class);
    var dependent = new Dependente(); dependent.setId(new DependenteId(1001L, 1));
    when(tenant.orgao()).thenReturn(1);
    assertDoesNotThrow(() -> ErgonTenantAccess.assertAllAllowed(List.of(dependent), tenant, em, false));
    when(tenant.orgao()).thenReturn(2);
    var denied = assertThrows(org.springframework.web.server.ResponseStatusException.class,
        () -> ErgonTenantAccess.assertAllAllowed(List.of(dependent), tenant, em, false));
    assertEquals(403, denied.getStatusCode().value());
    assertThrows(org.springframework.web.server.ResponseStatusException.class,
        () -> ErgonTenantAccess.assertAllowed(dependent, tenant, em, false));
    verify(query).setParameter("org", 1); verify(query, times(2)).setParameter("org", 2);
  }

  @Test void financialPatchUsesTheCanonicalHttpErrorAndAllowsAValidAppraisal() throws Exception {
    TempoLicencaIssue10RegressionTest.validator();
    try {
      var harness = new TempoLicencaIssue10RegressionTest(); var journey = harness.homologated("G");
      journey.abono().setValorIndenizacao(new BigDecimal("50"));
      journey.abono().setCustomFields(new LinkedHashMap<>(Map.of("extra", 1)));
      var definition = P4CustomFieldAdoptionRegressionTest.descriptor(Map.of("<=", List.of(Map.of("var", "draft.valorIndenizacao"), 100)));
      ReflectionTestUtils.setField(journey.venda(), "customFieldValidator", new CustomFieldValidator((r,t,s) -> List.of(definition)));
      var canonical = new org.praxisplatform.uischema.rest.exceptionhandler.GlobalExceptionHandler();
      var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(
          new FinancialBoundary(journey.venda(), harness.version(journey.abono().getId(), 0)))
          .setControllerAdvice(new br.com.praxis.ergonshowcase.controller.ErgonCustomFieldExceptionHandler(canonical), canonical).build();
      clearInvocations(journey.repository());
      mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/financial/" + journey.abono().getId())
          .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content("{\"valorIndenizacao\":200}"))
          .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isUnprocessableEntity())
          .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.errors[0].code").value("ERG-21502"))
          .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.errors[0].target").value("customFields.extra"));
      verify(journey.repository(), never()).save(any());
      mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/financial/" + journey.abono().getId())
          .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content("{\"valorIndenizacao\":75}"))
          .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
          .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.valorIndenizacao").value(75));
      verify(journey.repository()).save(any());
    } finally { TempoLicencaIssue10RegressionTest.closeValidator(); }
  }

  @org.springframework.web.bind.annotation.RestController
  static class FinancialBoundary {
    final VendaLicencaEspecialService service;
    final org.praxisplatform.uischema.concurrency.ResourceVersionUpdatePrecondition<Long> precondition;
    FinancialBoundary(VendaLicencaEspecialService service, org.praxisplatform.uischema.concurrency.ResourceVersionUpdatePrecondition<Long> precondition) {
      this.service=service; this.precondition=precondition;
    }
    @org.springframework.web.bind.annotation.PatchMapping("/financial/{id}")
    VendaLicencaEspecialResponseDTO patch(@org.springframework.web.bind.annotation.PathVariable Long id,
        @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody VendaLicencaEspecialLiquidacaoPatchDTO dto) {
      return service.patch(id, dto, precondition).body();
    }
  }

}
