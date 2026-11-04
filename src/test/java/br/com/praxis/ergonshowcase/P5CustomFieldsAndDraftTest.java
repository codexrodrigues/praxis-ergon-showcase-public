package br.com.praxis.ergonshowcase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.com.praxis.ergonshowcase.config.TenantContext;
import br.com.praxis.ergonshowcase.controller.LicencaEspecialGozoController;
import br.com.praxis.ergonshowcase.domain.DefinicaoCampoCustomizado;
import br.com.praxis.ergonshowcase.domain.DesignacaoFuncao;
import br.com.praxis.ergonshowcase.domain.FuncaoConfianca;
import br.com.praxis.ergonshowcase.domain.SubstituicaoFuncao;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.mapper.DefinicaoCampoCustomizadoMapper;
import br.com.praxis.ergonshowcase.repository.DefinicaoCampoCustomizadoRepository;
import br.com.praxis.ergonshowcase.repository.DesignacaoFuncaoRepository;
import br.com.praxis.ergonshowcase.repository.FuncaoConfiancaRepository;
import br.com.praxis.ergonshowcase.repository.SubstituicaoFuncaoRepository;
import br.com.praxis.ergonshowcase.rules.*;
import br.com.praxis.ergonshowcase.service.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.praxisplatform.uischema.bulk.*;
import org.praxisplatform.uischema.concurrency.*;
import org.praxisplatform.uischema.customfields.*;
import org.praxisplatform.uischema.service.base.BaseResourceService;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.test.util.ReflectionTestUtils;

/** R1/R2/R3 e C-02 permanentes, compondo serviços do host com o validador e adapter nativos. */
@SuppressWarnings({"unchecked", "rawtypes"})
class P5CustomFieldsAndDraftTest {
  @Test void chefiaDraftsProjectOnlyReviewedSuggestionsAndShareEligibilityWithSnapshots() {
    var funcao = new FuncaoConfianca();
    funcao.setEmpCodigo(1);
    funcao.setSituacao("ATIVA");
    funcao.setCategoria("CHEFIA");
    funcao.setCodigo("Original");
    var designacao = new DesignacaoFuncao();
    designacao.setEmpCodigo(1);
    designacao.setSituacao("DESIGNADA");
    designacao.setFuncaoId(17L);
    designacao.setAtoPublicacaoId(18L);
    var substituicao = new SubstituicaoFuncao();
    substituicao.setEmpCodigo(1);
    substituicao.setSituacao("PROGRAMADA");
    substituicao.setMotivo("FERIAS");
    substituicao.setFeriasGozoId(19L);
    var funcoes = mock(FuncaoConfiancaRepository.class);
    var designacoes = mock(DesignacaoFuncaoRepository.class);
    var substituicoes = mock(SubstituicaoFuncaoRepository.class);
    when(funcoes.findById(1L)).thenReturn(Optional.of(funcao));
    when(designacoes.findById(2L)).thenReturn(Optional.of(designacao));
    when(substituicoes.findById(3L)).thenReturn(Optional.of(substituicao));
    var functionService = new FuncaoConfiancaService(funcoes, null);
    var designationService = new DesignacaoFuncaoService(designacoes, null, null);
    var substitutionService = new SubstituicaoFuncaoService(substituicoes, null, null);
    assertThat(functionService.duplicateDraft(1L)).isEqualTo(new FuncaoConfiancaDuplicateDraftDTO("CHEFIA"));
    assertThat(designationService.duplicateDraft(2L)).isEqualTo(new DesignacaoFuncaoDuplicateDraftDTO(17L));
    assertThat(substitutionService.duplicateDraft(3L)).isEqualTo(new SubstituicaoFuncaoDuplicateDraftDTO("FERIAS"));
    assertThat(designationService.findResourceStateSnapshot(2L).orElseThrow().metadata())
        .containsEntry("duplicateDraftEligible", true);
    designacao.setSituacao("DISPENSADA");
    assertThat(designationService.findResourceStateSnapshot(2L).orElseThrow().metadata())
        .containsEntry("duplicateDraftEligible", false);
    assertThatThrownBy(() -> designationService.duplicateDraft(2L))
        .isInstanceOfSatisfying(org.springframework.web.server.ResponseStatusException.class,
            error -> assertThat(error.getStatusCode().value()).isEqualTo(403));
    funcao.setEmpCodigo(2);
    assertThatThrownBy(() -> functionService.duplicateDraft(1L))
        .isInstanceOfSatisfying(org.springframework.web.server.ResponseStatusException.class,
            error -> assertThat(error.getStatusCode().value()).isEqualTo(403));
    verify(funcoes, times(2)).findById(1L);
    verify(designacoes, times(4)).findById(2L);
    verify(substituicoes).findById(3L);
    verifyNoMoreInteractions(funcoes, designacoes, substituicoes);
  }

  static CustomFieldDescriptor readonly() {
    return new CustomFieldDescriptor("audited", "Valor auditado", "Valor mantido exclusivamente pelo servidor.",
        CustomFieldDataType.INTEGER, null, null, 0, false, true, false, false, false,
        null, null, null, null, null, null, null, null, null, null, null, null, null);
  }

  @Test void r1OptionalOmissionAndExplicitNullAreAcceptedByTheActualCreateLifecycle() {
    for (var values : List.of(Map.<String, Object>of(), Collections.<String, Object>singletonMap("extra", null))) {
      var fixture = new TempoLicencaCrudContractTest.Fixture();
      var service = (LicencaEspecialGozoService) fixture.service(2)[0];
      var input = (CreateLicencaEspecialGozoDTO) new TempoLicencaCrudContractTest().create(2);
      input.setCustomFields(values);
      var descriptor = P4CustomFieldAdoptionRegressionTest.descriptor(Map.of(">=", List.of(Map.of("var", "value"), 4)));
      ReflectionTestUtils.setField(service, "customFieldValidator", new CustomFieldValidator((r, t, s) -> List.of(descriptor)));
      assertThatCode(() -> service.create(input)).doesNotThrowAnyException();
    }
  }

  @Test void c02BothBulkModesRetainServerOwnedCustomFieldsWithoutRejectingAnUnrelatedEdit() {
    var fixture = new TempoLicencaCrudContractTest.Fixture();
    var service = (LicencaEspecialGozoService) fixture.service(2)[0];
    service.create((CreateLicencaEspecialGozoDTO) new TempoLicencaCrudContractTest().create(2));
    var row = fixture.gozos.getFirst(); row.setCustomFields(new LinkedHashMap<>(Map.of("audited", 99)));
    ReflectionTestUtils.setField(service, "customFieldValidator", new CustomFieldValidator((r, t, s) -> List.of(readonly())));
    var compiled = new TempoLicencaBulkAnalyticsExportTest().fields(UpdateLicencaEspecialGozoDTO.class, LicencaEspecialGozoController.class);
    for (var mode : List.of(BulkMode.UNIFORM_UPDATE, BulkMode.PER_ITEM_UPDATE)) {
      var candidate = (UpdateLicencaEspecialGozoDTO) BulkCandidateFixture.candidate(service, row,
          List.of(BulkFieldChange.set("observacoes", BulkCandidateFixture.JSON.valueToTree("Revisão coletiva"))), compiled, mode);
      assertThat(((Number) candidate.getCustomFields().get("audited")).intValue()).isEqualTo(99);
      assertThat(row.getObservacoes()).isNull();
      assertThat(((Number) row.getCustomFields().get("audited")).intValue()).isEqualTo(99);
      BulkCandidateFixture.apply(service, row, candidate);
      assertThat(row.getObservacoes()).isEqualTo("Revisão coletiva");
      assertThat(((Number) row.getCustomFields().get("audited")).intValue()).isEqualTo(99);
      row.setObservacoes(null);
    }
  }

  @ParameterizedTest @ValueSource(strings = {"tenant.orgao", "system.user", "nowUtc", "unknown", "$['tenant'].orgao"})
  void r2UnknownRootsAreRejectedBeforeCreateOrUpdateCanSaveADefinition(String path) {
    var expression = Map.<String, Object>of("==", List.of(Map.of("var", path), 1));
    var repository = mock(DefinicaoCampoCustomizadoRepository.class);
    var service = new DefinicaoCampoCustomizadoService(repository, new DefinicaoCampoCustomizadoMapper(), tenant(), new ErgonRuleEngineFacade());
    var create = definition(); create.setValidationRules(expression);
    assertInvalidDefinition(() -> service.create(create));
    var row = new DefinicaoCampoCustomizado(); row.setId(1L); row.setEmpCodigo(1); row.setVersion(0L);
    when(repository.findById(1L)).thenReturn(Optional.of(row));
    var update = new UpdateDefinicaoCampoCustomizadoDTO(); org.springframework.beans.BeanUtils.copyProperties(create, update);
    var etags = new ResourceVersionEtagService("p5-definition-validation-test-secret");
    var scope = new ResourceVersionScope("org=1");
    var condition = new ResourceVersionUpdatePrecondition<>(etags,
        etags.create(scope, service.getResourceKey(), 1L, 0), scope, service.getResourceKey(), 1L);
    assertInvalidDefinition(() -> service.update(1L, update, condition));
    verify(repository, never()).save(any());
  }

  @Test void r2AllFourCanonicalRootsAdmitPureExpressionsButNoTemporalOperator() {
    var facade = new ErgonRuleEngineFacade();
    for (String path : List.of("value", "draft.empCodigo", "entity.empCodigo", "customFields.extra"))
      assertThatCode(() -> facade.validateCustomFieldValidationRules(Map.of("==", List.of(Map.of("var", path), 1))))
          .doesNotThrowAnyException();
    assertInvalidDefinition(() -> facade.validateCustomFieldValidationRules(Map.of("now", List.of())));
    assertInvalidDefinition(() -> facade.validateCustomFieldValidationRules(Map.of("<", List.of(Map.of("var", "value"), Map.of("now", List.of())))));
  }

  @Test void duplicateDraftCopiesReviewedOrganizationAndQuantitiesWithoutPersistingOrCopyingIdentity() {
    var harness = new TempoLicencaCrudContractTest();
    for (int kind : List.of(1, 2, 3)) {
      var fixture = new TempoLicencaCrudContractTest.Fixture();
      var parts = fixture.service(kind); var service = (BaseResourceService) parts[0];
      service.create(harness.create(kind));
      var repository = (org.praxisplatform.uischema.repository.base.BaseCrudRepository) parts[1];
      clearInvocations(repository, fixture.em);
      Object draft = switch (kind) {
        case 1 -> ((PeriodoAquisitivoLicencaEspecialService) service).duplicateDraft(91L);
        case 2 -> ((LicencaEspecialGozoService) service).duplicateDraft(91L);
        default -> ((VendaLicencaEspecialService) service).duplicateDraft(91L);
      };
      var bean = new BeanWrapperImpl(draft);
      assertThat(bean.getPropertyValue("empCodigo")).isEqualTo(1);
      assertThat(bean.getPropertyValue(kind == 1 ? "diasDireito" : kind == 2 ? "diasGozo" : "diasVendidos")).isNotNull();
      assertThat(bean.getPropertyValue("numfunc")).isNull();
      assertThat(bean.getPropertyValue("numvinc")).isNull();
      assertThat(bean.getPropertyValue("dtini")).isNull();
      verify(repository, never()).save(any()); verify(fixture.em, never()).persist(any()); verify(fixture.em, never()).flush();
    }
  }

  @ParameterizedTest @CsvSource({"D,I", "D,F", "G,I", "G,F"})
  void specialLeaveDraftRetainsAbonoProposalTogetherWithoutCopyingItsAuthorization(String base, String position) {
    var fixture = new TempoLicencaCrudContractTest.Fixture();
    fixture.pa.setAbonoDirGozoLicEsp(base);
    var parts = fixture.service(2);
    var service = (LicencaEspecialGozoService) parts[0];
    var repository = (org.praxisplatform.uischema.repository.base.BaseCrudRepository) parts[1];
    var harness = new TempoLicencaCrudContractTest();
    var input = (CreateLicencaEspecialGozoDTO) harness.create(2);
    input.setAbonoPecuniario(true);
    input.setDiasAbono("D".equals(base) ? 45 : 5);
    input.setIniFimAbono(position);
    input.setMesAnoFolha(java.time.LocalDate.of(2026, 1, 1));
    input.setNumFolha(2);
    service.create(input);
    service.transition(91L, "homologar-licenca-especial", harness.command(), harness.precondition(0));
    var source = fixture.gozos.getFirst();
    var json = new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules();
    var sourceBefore = json.valueToTree(source);
    int balanceBefore = fixture.pa.getSaldoDias();
    var authorizationsBefore = fixture.vendas.stream().map(json::valueToTree).toList();
    clearInvocations(repository, fixture.em);

    var draft = service.duplicateDraft(91L);

    assertThat(draft.getAbonoPecuniario()).isTrue();
    assertThat(draft.getDiasAbono()).isEqualTo(input.getDiasAbono());
    assertThat(draft.getIniFimAbono()).isEqualTo(position);
    assertThat(draft.getSituacao()).isEqualTo("PROGRAMADA");
    assertThat(draft.getPeriodoAquisitivoLicencaId()).isNull();
    assertThat(draft.getNumfunc()).isNull(); assertThat(draft.getNumvinc()).isNull();
    assertThat(draft.getDtini()).isNull(); assertThat(draft.getDtprevfim()).isNull();
    assertThat(draft.getDtfim()).isNull(); assertThat(draft.getOrdemParcela()).isNull();
    assertThat(draft.getDocumentoLegalId()).isNull(); assertThat(draft.getAtoPublicacaoId()).isNull();
    assertThat(draft.getMesAnoFolha()).isNull(); assertThat(draft.getNumFolha()).isNull();
    assertThat(json.valueToTree(draft).has("abonoConstitucional")).isFalse();
    assertThat(json.<com.fasterxml.jackson.databind.JsonNode>valueToTree(source)).isEqualTo(sourceBefore);
    assertThat(fixture.pa.getSaldoDias()).isEqualTo(balanceBefore);
    assertThat(fixture.vendas.stream().map(json::valueToTree).toList()).isEqualTo(authorizationsBefore);
    verify(repository, never()).save(any());
    verify(fixture.em, never()).persist(any()); verify(fixture.em, never()).flush();
  }

  @Test void specialLeaveDraftRetainsAnExplicitNoAbonoChoice() {
    var fixture = new TempoLicencaCrudContractTest.Fixture();
    var service = (LicencaEspecialGozoService) fixture.service(2)[0];
    service.create((CreateLicencaEspecialGozoDTO) new TempoLicencaCrudContractTest().create(2));
    var draft = service.duplicateDraft(91L);
    assertThat(draft.getAbonoPecuniario()).isFalse();
    assertThat(draft.getDiasAbono()).isZero(); assertThat(draft.getIniFimAbono()).isNull();
  }

  @Test void finalCreateRechecksTheAbonoProposalAgainstTheSelectedPeriodsCurrentPolicy() {
    var fixture = new TempoLicencaCrudContractTest.Fixture();
    var parts = fixture.service(2);
    var service = (LicencaEspecialGozoService) parts[0];
    var repository = (org.praxisplatform.uischema.repository.base.BaseCrudRepository) parts[1];
    var input = (CreateLicencaEspecialGozoDTO) new TempoLicencaCrudContractTest().create(2);
    input.setAbonoPecuniario(true); input.setDiasAbono(5); input.setIniFimAbono("F");
    service.create(input);
    var draft = service.duplicateDraft(91L);
    draft.setPeriodoAquisitivoLicencaId(20L); draft.setOrdemParcela(2);
    draft.setDtini(java.time.LocalDate.of(2026, 2, 1));
    draft.setDtprevfim(java.time.LocalDate.of(2026, 2, 10));
    fixture.pa.setAbonoDirGozoLicEsp("N");
    int balanceBefore = fixture.pa.getSaldoDias();
    clearInvocations(repository, fixture.em);

    assertThatThrownBy(() -> service.create(draft))
        .isInstanceOfSatisfying(br.com.praxis.ergonshowcase.rules.ErgonRuleViolationException.class,
            violation -> assertThat(violation.getReasonCodes()).contains("CERG-06309"));
    assertThat(fixture.gozos).hasSize(1); assertThat(fixture.vendas).isEmpty();
    assertThat(fixture.pa.getSaldoDias()).isEqualTo(balanceBefore);
    verify(repository, never()).save(any());
    verify(fixture.em, never()).persist(any()); verify(fixture.em, never()).flush();
  }

  static TenantContext tenant() { return new TenantContext(); }
  static CreateDefinicaoCampoCustomizadoDTO definition() {
    var input = new CreateDefinicaoCampoCustomizadoDTO(); input.setEmpCodigo(1);
    input.setResourceKey("administracao-pessoal.vinculos"); input.setFieldKey("extra"); input.setFieldType("INTEGER");
    input.setControlType("NUMERIC_TEXT_BOX"); input.setLabel("Extra"); return input;
  }
  static void assertInvalidDefinition(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
    assertThatThrownBy(call).isInstanceOfSatisfying(ErgonRuleViolationException.class, failure -> {
      assertThat(failure.getHttpStatus()).isEqualTo(422); assertThat(failure.getReasonCodes()).containsExactly("ERG-21501");
    });
  }
}
