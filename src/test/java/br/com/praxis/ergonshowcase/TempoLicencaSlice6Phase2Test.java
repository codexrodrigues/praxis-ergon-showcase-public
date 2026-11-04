package br.com.praxis.ergonshowcase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import br.com.praxis.ergonshowcase.config.*;
import br.com.praxis.ergonshowcase.controller.*;
import br.com.praxis.ergonshowcase.domain.*;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.praxisplatform.uischema.action.*;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.bulk.*;
import org.praxisplatform.uischema.concurrency.ResourceVersionPreconditionException;
import org.praxisplatform.uischema.exporting.*;
import org.praxisplatform.uischema.fact.ResourceFact;
import org.praxisplatform.uischema.service.base.BaseResourceService;
import org.praxisplatform.uischema.stats.*;
import org.praxisplatform.uischema.surface.*;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.*;

/** Contratos executáveis locais; mocks não certificam isolamento PostgreSQL nem transporte HTTP. */
@SuppressWarnings({"rawtypes","unchecked"})
class TempoLicencaSlice6Phase2Test {
  final TempoLicencaCrudContractTest h=new TempoLicencaCrudContractTest();
  final ObjectMapper json=new ObjectMapper().findAndRegisterModules();
  static final List<Class<?>> CONTROLLERS=List.of(AverbacaoTempoServicoController.class,PeriodoAquisitivoLicencaEspecialController.class,LicencaEspecialGozoController.class,VendaLicencaEspecialController.class,RadarTempoServicoLicencaEspecialController.class);
  static final List<Class<?>> RESPONSES=List.of(AverbacaoTempoServicoResponseDTO.class,PeriodoAquisitivoLicencaEspecialResponseDTO.class,LicencaEspecialGozoResponseDTO.class,VendaLicencaEspecialResponseDTO.class,RadarTempoServicoLicencaEspecialResponseDTO.class);

  @TestFactory Stream<DynamicTest> eachResourcePublishesResolvableDetailAnalyticsAndUniqueSafeFacts() {
    return java.util.stream.IntStream.range(0,5).mapToObj(index->DynamicTest.dynamicTest(CONTROLLERS.get(index).getSimpleName(),()->{
      var ctrl=CONTROLLERS.get(index);var detail=ctrl.getMethod("detail",Long.class).getAnnotation(UiSurface.class);
      assertThat(detail.id()).isEqualTo("detail");assertThat(detail.kind()).isEqualTo(SurfaceKind.READ_PROJECTION);assertThat(detail.scope()).isEqualTo(SurfaceScope.ITEM);
      var analytics=Arrays.stream(ctrl.getDeclaredMethods()).map(m->m.getAnnotation(UiAnalytics.class)).filter(Objects::nonNull).findFirst().orElseThrow();
      assertThat(analytics.projections()).hasSizeBetween(2,3);
      var nominal=Arrays.stream(analytics.projections()).filter(p->!p.recordOpen().target().surfaceId().isBlank()).findFirst().orElseThrow();
      assertThat(nominal.primaryDimension().field()).isEqualTo("id");assertThat(nominal.recordOpen().sourceIdentityField()).isEqualTo("key");
      assertThat(nominal.recordOpen().target().resourceKey()).isEqualTo(ctrl.getAnnotation(ApiResource.class).resourceKey());
      var facts=RESPONSES.get(index).getAnnotationsByType(ResourceFact.class);assertThat(facts).hasSizeBetween(3,4);
      assertThat(Arrays.stream(facts).map(ResourceFact::key)).doesNotHaveDuplicates();
      var bean=new BeanWrapperImpl(RESPONSES.get(index).getConstructor().newInstance());
      for(var fact:facts){assertThat(fact.label().length()).isLessThanOrEqualTo(35);assertThat(fact.label()).doesNotEndWith(".");assertThat(bean.isReadableProperty(fact.key())).isTrue();assertThat(fact.key()).isNotEqualTo("valorIndenizacao");}
    }));
  }

  @TestFactory Stream<DynamicTest> itemActionsAndAllThreeBulkModesDeclareConcurrencyAndRealHandlers() {
    return CONTROLLERS.subList(0,4).stream().map(ctrl->DynamicTest.dynamicTest(ctrl.getSimpleName(),()->{
      var bulk=ctrl.getAnnotation(BulkResourceOperations.class);assertThat(bulk.protectedUpdateFields()).contains("situacao","empCodigo","numfunc","numvinc");
      var modes=new HashSet<BulkMode>();int item=0,collection=0,intents=0;
      for(var method:ctrl.getDeclaredMethods()){
        var action=method.getAnnotation(WorkflowAction.class);
        if(action!=null){assertThat(method.isAnnotationPresent(PostMapping.class)).isTrue();assertThat(action.idempotencyKey()).isEqualTo(ActionRequirement.REQUIRED);
          if(action.scope()==ActionScope.ITEM){item++;assertThat(action.allowedStates()).isNotEmpty();assertThat(action.resourceVersionTransport()).isEqualTo(ActionResourceVersionTransport.IF_MATCH);assertThat(action.resourceVersion()).isEqualTo(ActionRequirement.REQUIRED);}
          else{collection++;assertThat(method.getAnnotation(BulkOperation.class).mode()).isEqualTo(BulkMode.DOMAIN_COMMAND);}}
        var op=method.getAnnotation(BulkOperation.class);if(op!=null){modes.add(op.mode());assertThat(Arrays.stream(ctrl.getDeclaredMethods()).map(m->m.getAnnotation(io.swagger.v3.oas.annotations.Operation.class)).filter(Objects::nonNull).map(io.swagger.v3.oas.annotations.Operation::operationId)).contains(op.evaluationOperationId());}
        if(method.isAnnotationPresent(ResourceIntent.class)){intents++;assertThat(method.isAnnotationPresent(PatchMapping.class)).isTrue();assertThat(method.getAnnotation(UiSurface.class).kind()).isEqualTo(SurfaceKind.PARTIAL_FORM);}
      }
      assertThat(item).isGreaterThanOrEqualTo(3);assertThat(collection).isPositive();assertThat(intents).isPositive();assertThat(modes).containsExactlyInAnyOrder(BulkMode.UNIFORM_UPDATE,BulkMode.PER_ITEM_UPDATE,BulkMode.DOMAIN_COMMAND);
    }));
  }

  @Test void childSurfacesUseActualForeignKeysAndFullVinculoIdentity() throws Exception {
    assertThat(PeriodoAquisitivoLicencaEspecialController.class.getMethod("childrenLicencaEspecialGozo",Long.class,int.class,int.class).getAnnotation(UiSurface.class).relatedChildParentField()).isEqualTo("periodoAquisitivoLicencaId");
    assertThat(AverbacaoTempoServicoController.class.getMethod("childrenPeriodoAquisitivoLicencaEspecial",Long.class,int.class,int.class).getAnnotation(UiSurface.class).relatedChildParentField()).isEqualTo("averbacaoOrigemId");
    assertThat(LicencaEspecialGozoController.class.getMethod("childrenVendaLicencaEspecial",Long.class,int.class,int.class).getAnnotation(UiSurface.class).relatedChildParentField()).isEqualTo("licencaEspecialGozoId");
    assertThat(VinculoController.class.getMethod("childrenAverbacaoTempoServico",VinculoId.class,int.class,int.class).getAnnotation(UiSurface.class).relatedChildParentField()).isEqualTo("vinculoId");
    var service=mock(LicencaEspecialGozoService.class);var parent=mock(PeriodoAquisitivoLicencaEspecialService.class);var ctrl=new PeriodoAquisitivoLicencaEspecialController(parent);
    ReflectionTestUtils.setField(ctrl,"licencaEspecialGozoChildren",service);
    when(service.getDefaultSort()).thenReturn(org.springframework.data.domain.Sort.by("id"));
    ctrl.childrenLicencaEspecialGozo(88L,0,20);
    var filter=org.mockito.ArgumentCaptor.forClass(LicencaEspecialGozoFilterDTO.class);verify(service).filter(filter.capture(),any(),eq(List.of()));assertThat(filter.getValue().getPeriodoAquisitivoLicencaId()).isEqualTo(88L);
    assertThatThrownBy(()->ctrl.childrenLicencaEspecialGozo(88L,0,201)).hasMessageContaining("Paginação");
  }

  @Test void partialIntentPreservesOmissionAndAcceptsExplicitNullableClear() throws Exception {
    var f=new TempoLicencaCrudContractTest.Fixture();var s=(AverbacaoTempoServicoService)f.service(0)[0];s.create((CreateAverbacaoTempoServicoDTO)h.create(0));
    try(var factory=jakarta.validation.Validation.buildDefaultValidatorFactory()){
      ReflectionTestUtils.setField(s,"beanValidator",factory.getValidator());
      var patch=json.readValue("{\"observacoes\":\"CTC revisada\",\"documentoLegalId\":null}",AverbacaoGovernancaPatchDTO.class);
      assertThat(patch.wasSupplied("documentoLegalId")).isTrue();assertThat(patch.wasSupplied("atoPublicacaoId")).isFalse();
      var result=s.patch(91L,patch,h.precondition(0));assertThat(result.body().getObservacoes()).isEqualTo("CTC revisada");assertThat(result.body().getTotalDias()).isEqualTo(365);assertThat(result.body().getTipoTempo()).isEqualTo("PUBLICO");
      assertThatThrownBy(()->s.patch(91L,patch,h.precondition(7))).isInstanceOf(ResourceVersionPreconditionException.class);
    }
  }

  @Test void ordinaryWritersCannotBypassLifecycle() {
    for(int kind=0;kind<4;kind++){
      var f=new TempoLicencaCrudContractTest.Fixture();var s=(BaseResourceService)f.service(kind)[0];var create=h.create(kind);s.create(create);var update=h.update(kind,create);new BeanWrapperImpl(update).setPropertyValue("situacao","CANCELADO");
      int k=kind;assertThatThrownBy(()->((org.praxisplatform.uischema.service.base.VersionedCreateUpdateResourceService)s).update(91L,update,h.precondition(0))).as("resource %s",k).hasMessageContaining("ciclo de vida");
    }
  }

  @Test void gozoHomologationSynchronizesOneAbonoWithoutDoubleDebitAndCancellationRestoresBalance() {
    var f=new TempoLicencaCrudContractTest.Fixture();var s=(LicencaEspecialGozoService)f.service(2)[0];var d=(CreateLicencaEspecialGozoDTO)h.create(2);d.setAbonoPecuniario(true);d.setDiasAbono(5);d.setIniFimAbono("F");s.create(d);
    assertThat(f.pa.getSaldoDias()).isEqualTo(75);assertThat(f.vendas).isEmpty();
    s.transition(91L,"homologar-licenca-especial",h.command(),h.precondition(0));
    assertThat(f.vendas).hasSize(1);assertThat(f.vendas.getFirst().getLicencaEspecialGozoId()).isEqualTo(91L);assertThat(f.pa.getDiasVendidos()).isEqualTo(5);assertThat(f.pa.getSaldoDias()).isEqualTo(75);
    s.transition(91L,"cancelar-licenca-especial",h.command(),h.precondition(0));assertThat(f.vendas.getFirst().getSituacao()).isEqualTo("ESTORNADA");assertThat(f.pa.getSaldoDias()).isEqualTo(90);
    assertThatThrownBy(()->s.transition(91L,"homologar-licenca-especial",h.command(),h.precondition(0))).hasMessageContaining("409");
  }

  @Test void interruptedGozoReleasesUnusedReservationAndPreservesOriginalForecast() {
    var f=new TempoLicencaCrudContractTest.Fixture();var s=(LicencaEspecialGozoService)f.service(2)[0];s.create((CreateLicencaEspecialGozoDTO)h.create(2));
    s.transition(91L,"homologar-licenca-especial",h.command(),h.precondition(0));s.transition(91L,"iniciar-gozo-licenca-especial",h.command(),h.precondition(0));
    var pair=s.transition(91L,"interromper-licenca-especial",h.command(),h.precondition(0));assertThat(pair.body().getSituacao()).isEqualTo("INTERROMPIDA");assertThat(pair.body().getDiasGozo()).isEqualTo(5);assertThat(pair.body().getDtprevfim()).isEqualTo(LocalDate.of(2026,1,10));assertThat(f.pa.getSaldoDias()).isEqualTo(85);
  }

  @Test void saleRequiresAuthorizationPaymentDataAndPermitsReversalAfterPolicyRevocation() {
    var f=new TempoLicencaCrudContractTest.Fixture();var s=(VendaLicencaEspecialService)f.service(3)[0];s.create((CreateVendaLicencaEspecialDTO)h.create(3));
    assertThatThrownBy(()->s.transition(91L,"pagar-venda-licenca-especial",h.command(),h.precondition(0))).hasMessageContaining("409");
    s.transition(91L,"autorizar-venda-licenca-especial",h.command(),h.precondition(0));
    assertThatThrownBy(()->s.transition(91L,"pagar-venda-licenca-especial",h.command(),h.precondition(0))) .isInstanceOfSatisfying(br.com.praxis.ergonshowcase.rules.ErgonRuleViolationException.class,
        rejection -> assertThat(rejection.getReasonCodes()).containsExactly("CERG-06410"));
    var entity = f.vendas.getFirst();
    entity.setValorIndenizacao(new BigDecimal("1234.50"));
    assertThatThrownBy(() -> s.transition(91L, "pagar-venda-licenca-especial", h.command(), h.precondition(0)))
        .isInstanceOfSatisfying(br.com.praxis.ergonshowcase.rules.ErgonRuleViolationException.class,
            rejection -> assertThat(rejection.getReasonCodes()).containsExactly("CERG-06411"));
    entity.setMesAnoFolha(LocalDate.of(2026,1,1));entity.setNumFolha(1);
    assertThatThrownBy(() -> s.transition(91L, "pagar-venda-licenca-especial", h.command(), h.precondition(0)))
        .isInstanceOfSatisfying(br.com.praxis.ergonshowcase.rules.ErgonRuleViolationException.class,
            rejection -> assertThat(rejection.getReasonCodes()).containsExactly("CERG-06414"));
    var ato = new br.com.praxis.ergonshowcase.domain.DocumentoLegal();
    org.mockito.Mockito.when(f.em.find(br.com.praxis.ergonshowcase.domain.DocumentoLegal.class, 901L)).thenReturn(ato);
    entity.setAtoPublicacaoId(901L);
    var paid=s.transition(91L,"pagar-venda-licenca-especial",h.command(),h.precondition(0));assertThat(paid.body().isLiquidada()).isTrue();assertThat(paid.body().getDtPagamento()).isEqualTo(h.command().getDataEfeito());
    f.pa.setVendLicEsp("N");s.transition(91L,"estornar-venda-licenca-especial",h.command(),h.precondition(0));assertThat(f.pa.getSaldoDias()).isEqualTo(90);
  }

  @Test void periodReconciliationControlsAllThreeReadonlyCountersAndDoubleConversionPolicy() {
    var f=new TempoLicencaCrudContractTest.Fixture();var parts=f.service(1);var s=(PeriodoAquisitivoLicencaEspecialService)parts[0];var d=(CreatePeriodoAquisitivoLicencaEspecialDTO)h.create(1);d.setDtfim(LocalDate.of(2026,1,1));s.create(d);
    s.transition(91L,"homologar-periodo-licenca-especial",h.command(),h.precondition(0));
    var input=h.command();input.setDias(10);
    assertThatThrownBy(()->s.transition(91L,"converter-em-dobro",input,h.precondition(0))).hasMessageContaining("permissão");
    // The existing managed record is read from the service repository, never supplied as a command quantity.
    var repo=(org.praxisplatform.uischema.repository.base.BaseCrudRepository)parts[1];
    var entity=(PeriodoAquisitivoLicencaEspecial)repo.findById(91L).orElseThrow();entity.setContDobroLicEsp("S");
    var converted=s.transition(91L,"converter-em-dobro",input,h.precondition(0));assertThat(converted.body().getSaldoDias()).isEqualTo(80);assertThat(converted.body().getDiasConvertidosDobro()).isEqualTo(10);
    var restored=s.transition(91L,"reverter-conversao-dobro",input,h.precondition(0));assertThat(restored.body().getSaldoDias()).isEqualTo(90);assertThat(restored.body().getDiasGozados()).isZero();assertThat(restored.body().getDiasVendidos()).isZero();
  }

  @Test void duplicateDraftCopiesOnlyReviewedDescriptiveSuggestionsAndNeverWrites() {
    var f=new TempoLicencaCrudContractTest.Fixture();var parts=f.service(0);var s=(AverbacaoTempoServicoService)parts[0];s.create((CreateAverbacaoTempoServicoDTO)h.create(0));var repo=(org.praxisplatform.uischema.repository.base.BaseCrudRepository)parts[1];clearInvocations(repo,f.em);
    var draft=s.duplicateDraft(91L);assertThat(draft.getDescricao()).isNotBlank();assertThat(draft.getChave()).isNull();assertThat(draft.getEmpCodigo()).isNull();assertThat(draft.getNumfunc()).isNull();assertThat(draft.getDtini()).isNull();assertThat(draft.getDocumentoLegalId()).isNull();assertThat(draft.getDiasDesaverbados()).isZero();assertThat(draft.getSituacao()).isEqualTo("EM_ANALISE");verify(repo,never()).save(any());verify(f.em,never()).persist(any());verify(f.em,never()).flush();
  }

  @Test void previewsAreDeterministicAndRejectInvalidDraftArithmetic() {
    var service=new TempoLicencaPreviewService(mock(TenantContext.class));
    assertThat(service.averbacao(new TempoLicencaPrevia.Averbacao(100,25)).diasPrevistos()).isEqualTo(75);
    assertThat(service.periodo(new TempoLicencaPrevia.Periodo(90,10)).diasPrevistos()).isEqualTo(80);
    assertThat(service.gozo(new TempoLicencaPrevia.Gozo(LocalDate.of(2026,1,1),LocalDate.of(2026,1,10))).diasPrevistos()).isEqualTo(10);
    assertThatThrownBy(() -> service.averbacao(new TempoLicencaPrevia.Averbacao(10, 11)))
        .isInstanceOfSatisfying(br.com.praxis.ergonshowcase.rules.ErgonRuleViolationException.class,
            failure -> assertThat(failure.getReasonCodes()).containsExactly("CERG-09038"));
    assertThatThrownBy(() -> service.periodo(new TempoLicencaPrevia.Periodo(90, 91)))
        .isInstanceOfSatisfying(br.com.praxis.ergonshowcase.rules.ErgonRuleViolationException.class,
            failure -> assertThat(failure.getReasonCodes()).containsExactly("CERG-09039"));
  }

  @Test void homologationGeneratesPeriodAndReversalsReconcileItsUnconsumedRight() {
    var f=new TempoLicencaCrudContractTest.Fixture();var s=(AverbacaoTempoServicoService)f.service(0)[0];s.create((CreateAverbacaoTempoServicoDTO)h.create(0));
    s.transition(91L,"homologar-averbacao",h.command(),h.precondition(0));assertThat(f.periodos).hasSize(1);var pa=f.periodos.getFirst();assertThat(pa.getAverbacaoOrigemId()).isEqualTo(91L);assertThat(pa.getDiasDireito()).isEqualTo(90);
    var dto=h.command();dto.setDias(25);s.transition(91L,"registrar-desaverbacao",dto,h.precondition(0));assertThat(pa.getDiasDireito()).isEqualTo(65);assertThat(pa.getSaldoDias()).isEqualTo(65);
    s.transition(91L,"reverter-desaverbacao",dto,h.precondition(0));assertThat(pa.getDiasDireito()).isEqualTo(90);
    s.transition(91L,"cancelar-averbacao",h.command(),h.precondition(0));assertThat(pa.getSituacao()).isEqualTo("CANCELADO");assertThat(f.periodos).hasSize(1);
  }

  @Test void periodSuspensionPrescriptionAndCancellationHaveExplicitNegativePaths() {
    var f=new TempoLicencaCrudContractTest.Fixture();var parts=f.service(1);var s=(PeriodoAquisitivoLicencaEspecialService)parts[0];var d=(CreatePeriodoAquisitivoLicencaEspecialDTO)h.create(1);d.setDtfim(LocalDate.of(2026,1,1));d.setDtPrescricao(LocalDate.of(2026,1,4));s.create(d);
    s.transition(91L,"homologar-periodo-licenca-especial",h.command(),h.precondition(0));
    assertThat(s.transition(91L,"suspender-periodo-licenca-especial",h.command(),h.precondition(0)).body().getSituacao()).isEqualTo("SUSPENSO");
    s.transition(91L,"homologar-periodo-licenca-especial",h.command(),h.precondition(0));
    var premature=h.command();premature.setDataEfeito(LocalDate.of(2026,1,3));assertThatThrownBy(()->s.transition(91L,"prescrever-periodo-licenca-especial",premature,h.precondition(0))).hasMessageContaining("Prazo prescricional");
    assertThat(s.transition(91L,"prescrever-periodo-licenca-especial",h.command(),h.precondition(0)).body().getSituacao()).isEqualTo("PRESCRITO");
    assertThatThrownBy(()->s.transition(91L,"cancelar-periodo-licenca-especial",h.command(),h.precondition(0))).hasMessageContaining("409");
    var other=new TempoLicencaCrudContractTest.Fixture();var os=(PeriodoAquisitivoLicencaEspecialService)other.service(1)[0];os.create((CreatePeriodoAquisitivoLicencaEspecialDTO)h.create(1));assertThat(os.transition(91L,"cancelar-periodo-licenca-especial",h.command(),h.precondition(0)).body().getSituacao()).isEqualTo("CANCELADO");
  }

  @Test void newRecordsCannotSmuggleWorkflowEffectsThroughCreate() {
    var f=new TempoLicencaCrudContractTest.Fixture();var a=(CreateAverbacaoTempoServicoDTO)h.create(0);a.setDiasDesaverbados(1);assertThatThrownBy(()->((AverbacaoTempoServicoService)f.service(0)[0]).create(a)).hasMessageContaining("não pode nascer desaverbado");
    var p=(CreatePeriodoAquisitivoLicencaEspecialDTO)h.create(1);p.setDiasConvertidosDobro(1);assertThatThrownBy(()->((PeriodoAquisitivoLicencaEspecialService)f.service(1)[0]).create(p)).hasMessageContaining("ação própria");
    var v=(CreateVendaLicencaEspecialDTO)h.create(3);v.setDtPagamento(LocalDate.of(2026,1,5));assertThatThrownBy(()->((VendaLicencaEspecialService)f.service(3)[0]).create(v)).hasMessageContaining("não pode nascer liquidada");
  }

  @Test void canonicalStateProviderResolvesPersistedStateAndHidesForeignOrganization() {
    var f=new TempoLicencaCrudContractTest.Fixture();
    var components=f.service(2);var snapshotService=(LicencaEspecialGozoService)components[0];
    var repo=(br.com.praxis.ergonshowcase.repository.LicencaEspecialGozoRepository)components[1];
    var provider=StateSnapshotFixture.provider(snapshotService);
    var gozo=new LicencaEspecialGozo();gozo.setId(91L);gozo.setEmpCodigo(1);gozo.setSituacao("EM_GOZO");
    when(repo.findById(91L)).thenReturn(java.util.Optional.of(gozo));
    var key="administracao-pessoal.licencas-especiais-gozo";assertThat(provider.resolve(key,"91").orElseThrow().state()).isEqualTo("EM_GOZO");
    gozo.setEmpCodigo(2);assertThat(provider.resolve(key,91L)).isEmpty();assertThat(provider.resolve(key,"invalid")).isEmpty();
  }
}
