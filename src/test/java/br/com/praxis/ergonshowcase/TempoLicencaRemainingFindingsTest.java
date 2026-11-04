package br.com.praxis.ergonshowcase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import br.com.praxis.ergonshowcase.controller.VendaLicencaEspecialController;
import br.com.praxis.ergonshowcase.domain.*;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.repository.*;
import br.com.praxis.ergonshowcase.service.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.concurrency.*;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

/** Serviços/mappers reais; os testes SQL complementares exercitam migração e unicidade em PostgreSQL. */
class TempoLicencaRemainingFindingsTest {
  final TempoLicencaIssue10RegressionTest h=new TempoLicencaIssue10RegressionTest();
  @BeforeAll static void setupValidator() { TempoLicencaIssue10RegressionTest.validator(); }
  @AfterAll static void closeValidator() { TempoLicencaIssue10RegressionTest.closeValidator(); }
  TempoLicencaTransicaoDTO date(int day) { var d=h.h.command();d.setDataEfeito(LocalDate.of(2026,1,day));return d; }
  void reverse(TempoLicencaIssue10RegressionTest.Journey j) {
    j.venda().transition(j.abono().getId(),"estornar-venda-licenca-especial",date(6),h.version(j.abono().getId(),0));
  }
  @Test void s3PaidAbonoEstornoPreservesOriginalPaymentAndDoesNotReleaseGozoDays() {
    var j=h.homologated("G");h.pay(j);var id=j.abono().getId();
    var r=j.venda().transition(id,"estornar-venda-licenca-especial",date(6),h.version(id,0)).body();
    assertThat(r.getSituacao()).isEqualTo("ESTORNADA");assertThat(r.getDtEstorno()).isEqualTo(LocalDate.of(2026,1,6));
    assertThat(r.getDtPagamento()).isEqualTo(LocalDate.of(2026,1,5));assertThat(r.getValorIndenizacao()).isEqualByComparingTo("1234.56");
    assertThat(r.isLiquidada()).isFalse();assertThat(j.f().pa.getSaldoDias()).isEqualTo(75);
    assertThat(j.f().gozos.getFirst().getDiasAbono()).isEqualTo(5);
    assertThatThrownBy(()->j.venda().transition(id,"estornar-venda-licenca-especial",date(6),h.version(id,0))).hasMessageContaining("409");
  }
  @Test void s3PaidAbonoCanBeReversedThenItsGozoCanceledWithoutDeletingFinancialHistory() {
    var j=h.homologated("G");h.pay(j);reverse(j);var old=j.abono();
    j.gozo().transition(91L,"cancelar-licenca-especial",date(7),h.version(91L,0));
    assertThat(j.f().pa.getSaldoDias()).isEqualTo(90);assertThat(j.f().pa.getDiasVendidos()).isZero();
    assertThat(j.f().vendas).containsExactly(old);assertThat(old.getSituacao()).isEqualTo("ESTORNADA");
    assertThat(old.getDtPagamento()).isEqualTo(LocalDate.of(2026,1,5));assertThat(old.getDtEstorno()).isEqualTo(LocalDate.of(2026,1,6));
    assertThatThrownBy(()->j.venda().transition(old.getId(),"reconciliar-abono-vinculado",date(8),h.version(old.getId(),0))).hasMessageContaining("abono homologado ativo");
  }
  @Test void s3AfterInterruptionReconciliationCreatesOneNewAuthorizationAndNeverRewritesTheReversal() {
    var j=h.homologated("G");h.start(j);h.pay(j);reverse(j);var old=j.abono();
    j.gozo().transition(91L,"interromper-licenca-especial",date(7),h.version(91L,0));
    assertThat(j.f().pa.getSaldoDias()).isEqualTo(80);assertThat(j.f().vendas).hasSize(1);
    var result=j.venda().transition(old.getId(),"reconciliar-abono-vinculado",date(8),h.version(old.getId(),0));
    assertThat(result.body().getId()).isEqualTo(old.getId());assertThat(result.body().getSituacao()).isEqualTo("ESTORNADA");
    assertThat(j.f().vendas).hasSize(2);var fresh=j.f().vendas.getLast();
    assertThat(result.body().getAutorizacaoAtualId()).isEqualTo(fresh.getId());
    assertThat(fresh.getId()).isNotEqualTo(old.getId());assertThat(fresh.getSituacao()).isEqualTo("AUTORIZADA");
    assertThat(fresh.getDiasVendidos()).isEqualTo(3);assertThat(fresh.getLicencaEspecialGozoId()).isEqualTo(91L);
    assertThat(fresh.getDtPagamento()).isNull();assertThat(fresh.getDtEstorno()).isNull();assertThat(fresh.getValorIndenizacao()).isNull();
    j.venda().transition(old.getId(),"reconciliar-abono-vinculado",date(8),h.version(old.getId(),0));
    assertThat(j.f().vendas).hasSize(2);assertThat(j.f().pa.getSaldoDias()).isEqualTo(80);
    when(j.repository().findById(fresh.getId())).thenReturn(Optional.of(fresh));
    var patch=new VendaLicencaEspecialLiquidacaoPatchDTO();patch.setValorIndenizacao(new BigDecimal("300.00"));
    j.venda().patch(fresh.getId(),patch,h.version(fresh.getId(),0));
    j.venda().transition(fresh.getId(),"pagar-venda-licenca-especial",date(9),h.version(fresh.getId(),0));
    assertThat(fresh.getSituacao()).isEqualTo("PAGA");assertThat(old.getSituacao()).isEqualTo("ESTORNADA");
    assertThat(old.getDiasVendidos()).isEqualTo(5);assertThat(old.getValorIndenizacao()).isEqualByComparingTo("1234.56");
    assertThat(old.getDtPagamento()).isEqualTo(LocalDate.of(2026,1,5));assertThat(old.getDtEstorno()).isEqualTo(LocalDate.of(2026,1,6));
    assertThat(j.f().pa.getDiasVendidos()).isEqualTo(3);assertThat(j.f().pa.getSaldoDias()).isEqualTo(80);
  }
  @Test void s3UnrelatedGozoMaintenanceDoesNotReactivateAnEstornadoAbono() {
    var j=h.homologated("G");h.pay(j);reverse(j);
    var u=new UpdateLicencaEspecialGozoDTO();BeanUtils.copyProperties(j.f().gozos.getFirst(),u);u.setObservacoes("Retificação documental");
    j.gozo().update(91L,u,h.version(91L,0));
    assertThat(j.f().vendas).hasSize(1);assertThat(j.abono().getSituacao()).isEqualTo("ESTORNADA");
    var patch=new VendaLicencaEspecialLiquidacaoPatchDTO();patch.setValorIndenizacao(BigDecimal.TEN);
    assertThatThrownBy(()->j.venda().patch(j.abono().getId(),patch,h.version(j.abono().getId(),0))).hasMessageContaining("histórico");
  }
  @Test void s3EstornoRejectsBackdatedStaleAndMissingPreconditionWithoutChangingPayment() {
    var j=h.homologated("G");h.pay(j);var id=j.abono().getId();
    assertThatThrownBy(()->j.venda().transition(id,"estornar-venda-licenca-especial",date(4),h.version(id,0)))
        .isInstanceOfSatisfying(br.com.praxis.ergonshowcase.rules.ErgonRuleViolationException.class,
            rejection -> assertThat(rejection.getReasonCodes()).containsExactly("CERG-06412"));
    assertThatThrownBy(()->j.venda().transition(id,"estornar-venda-licenca-especial",date(6),h.version(id,7))).isInstanceOf(ResourceVersionPreconditionException.class);
    assertThatThrownBy(()->j.venda().transition(id,"estornar-venda-licenca-especial",date(6),null)).isInstanceOf(ResourceVersionPreconditionException.class);
    assertThat(j.abono().getSituacao()).isEqualTo("PAGA");assertThat(j.abono().getDtEstorno()).isNull();
  }
  @Test void s3EstornoAndReconciliationReplayDoNotRepeatEffectsAndRetainHistoricalDates() throws Exception {
    var j=h.homologated("G");h.pay(j);var old=j.abono();var commands=new TempoLicencaCommandReceiptTest();commands.setup();
    try {
      var count=new AtomicInteger();var input=date(6);
      for(int i=0;i<2;i++) {
        var result=commands.execute("test.resource","/test",old.getId(),"estornar-venda-licenca-especial","estorno-key","etag",input,VendaLicencaEspecialResponseDTO.class,
            ()->{count.incrementAndGet();return j.venda().transition(old.getId(),"estornar-venda-licenca-especial",input,h.version(old.getId(),0));});
        assertThat(result.body().getDtEstorno()).isEqualTo(input.getDataEfeito());assertThat(result.body().getDtPagamento()).isEqualTo(LocalDate.of(2026,1,5));
      }
      assertThat(count.get()).isEqualTo(1);
      var reissue=date(8);var reissues=new AtomicInteger();
      for(int i=0;i<2;i++) {
        var result=commands.execute("test.resource","/test",old.getId(),"reconciliar-abono-vinculado","reissue-key","etag",reissue,VendaLicencaEspecialResponseDTO.class,
            ()->{reissues.incrementAndGet();return j.venda().transition(old.getId(),"reconciliar-abono-vinculado",reissue,h.version(old.getId(),0));});
        assertThat(result.body().getId()).isEqualTo(old.getId());
        assertThat(result.body().getAutorizacaoAtualId()).isEqualTo(j.f().vendas.getLast().getId());
      }
      assertThat(reissues.get()).isEqualTo(1);assertThat(j.f().vendas).hasSize(2);assertThat(commands.receipts).hasSize(2);
    } finally { commands.cleanup(); }
  }
  @Test void s3CompensationDateCannotBeSetByCreatePutOrBulkAndDiscoveryUsesCurrentModality() {
    var f=new TempoLicencaCrudContractTest.Fixture();var service=(VendaLicencaEspecialService)f.service(3)[0];
    var d=(CreateVendaLicencaEspecialDTO)h.h.create(3);d.setDtEstorno(LocalDate.of(2026,1,6));
    assertThatThrownBy(()->service.create(d)).hasMessageContaining("não pode nascer");
    d.setDtEstorno(null);service.create(d);var u=(UpdateVendaLicencaEspecialDTO)h.h.update(3,d);u.setDtEstorno(LocalDate.of(2026,1,6));
    assertThatThrownBy(()->service.update(91L,u,h.version(91L,0))).hasMessageContaining("ação própria");
    var fields=new TempoLicencaBulkAnalyticsExportTest().fields(UpdateVendaLicencaEspecialDTO.class,VendaLicencaEspecialController.class);
    for(var mode:List.of(org.praxisplatform.uischema.bulk.BulkMode.UNIFORM_UPDATE,org.praxisplatform.uischema.bulk.BulkMode.PER_ITEM_UPDATE))
      assertThat(fields.writableFields(mode)).doesNotContain("dtEstorno");
    assertThat(Arrays.stream(VendaLicencaEspecialController.class.getAnnotation(ApiResource.class).quickFilters()).filter(q->q.id().equals("abono")).findFirst().orElseThrow().filter())
        .contains("ABONO_JUNTO_GOZO");
  }
  @Test void s2MigratedHomologatedOriginWithConsumptionAcceptsDocumentaryMaintenanceAndProtectsItsRight() {
    var f=new TempoLicencaCrudContractTest.Fixture();var parts=f.service(0);var a=(AverbacaoTempoServicoService)parts[0];
    var d=(CreateAverbacaoTempoServicoDTO)h.h.create(0);a.create(d);
    var source=((AverbacaoTempoServicoRepository)parts[1]).findById(91L).orElseThrow();source.setSituacao("HOMOLOGADA");
    f.pa.setAverbacaoOrigemId(91L);f.periodos.add(f.pa);
    var gs=(LicencaEspecialGozoService)f.service(2)[0];gs.create((CreateLicencaEspecialGozoDTO)h.h.create(2));
    gs.transition(91L,"homologar-licenca-especial",date(5),h.version(91L,0));
    gs.transition(91L,"iniciar-gozo-licenca-especial",date(5),h.version(91L,0));
    gs.transition(91L,"encerrar-licenca-especial",date(10),h.version(91L,0));
    var u=new UpdateAverbacaoTempoServicoDTO();BeanUtils.copyProperties(source,u);u.setObservacoes("Lastro revisado");
    assertThat(a.update(91L,u,h.version(91L,0)).body().getSituacao()).isEqualTo("HOMOLOGADA");
    assertThat(f.pa.getDiasDireito()).isEqualTo(90);assertThat(f.pa.getSaldoDias()).isEqualTo(80);
    var desaverbar=date(6);desaverbar.setDias(1);
    assertThatThrownBy(()->a.transition(91L,"registrar-desaverbacao",desaverbar,h.version(91L,0)))
        .isInstanceOfSatisfying(br.com.praxis.ergonshowcase.rules.ErgonRuleViolationException.class,
            rejection -> assertThat(rejection.getReasonCodes()).containsExactly("CERG-06115"));
    assertThat(source.getDiasDesaverbados()).isZero();
  }
  @Test @SuppressWarnings({"unchecked","rawtypes"})
  void s3CompensationFiltersBuildPredicatesOnTheRealJpaDate() {
    var filter=new VendaLicencaEspecialFilterDTO();filter.setComEstorno(true);
    var root=mock(jakarta.persistence.criteria.Root.class,RETURNS_DEEP_STUBS);
    var cb=mock(jakarta.persistence.criteria.CriteriaBuilder.class);
    new org.praxisplatform.uischema.filter.specification.GenericSpecificationsBuilder<>().buildSpecification(filter,org.springframework.data.domain.Pageable.unpaged())
        .spec().toPredicate(root,mock(jakarta.persistence.criteria.CriteriaQuery.class),cb);
    verify(cb).isNotNull(root.get("dtEstorno"));
  }
  @Test void s3ReissueCannotBeBackdatedBeforeTheCompensation() {
    var j=h.homologated("G");h.pay(j);reverse(j);var old=j.abono();
    assertThatThrownBy(()->j.venda().transition(old.getId(),"reconciliar-abono-vinculado",date(5),h.version(old.getId(),0)))
        .hasMessageContaining("anteceder o estorno");
    assertThat(j.f().vendas).hasSize(1);
  }
  @Test void s3NewPayrollBelongsToNewAuthorizationAndDoesNotRewriteTheOldPayment() {
    var j=h.homologated("G");h.pay(j);reverse(j);var old=j.abono();
    var patch=new LicencaEspecialGozoReagendamentoPatchDTO();patch.setMesAnoFolha(LocalDate.of(2026,2,1));patch.setNumFolha(2);
    j.gozo().patch(91L,patch,h.version(91L,0));
    assertThat(j.f().vendas).hasSize(1);assertThat(old.getNumFolha()).isEqualTo(1);assertThat(old.getMesAnoFolha()).isEqualTo(LocalDate.of(2026,1,1));
    j.venda().transition(old.getId(),"reconciliar-abono-vinculado",date(8),h.version(old.getId(),0));
    var fresh=j.f().vendas.getLast();assertThat(fresh.getNumFolha()).isEqualTo(2);assertThat(fresh.getMesAnoFolha()).isEqualTo(LocalDate.of(2026,2,1));
    assertThat(old.getNumFolha()).isEqualTo(1);assertThat(old.getDtPagamento()).isEqualTo(LocalDate.of(2026,1,5));
  }
  @Test void operationalFilterSchemasStayAlignedWithEverySlice6Lifecycle() throws Exception {
    var types=Map.of(AverbacaoTempoServicoFilterDTO.class,AverbacaoTempoServico.class,
        PeriodoAquisitivoLicencaEspecialFilterDTO.class,PeriodoAquisitivoLicencaEspecial.class,
        LicencaEspecialGozoFilterDTO.class,LicencaEspecialGozo.class,
        VendaLicencaEspecialFilterDTO.class,VendaLicencaEspecial.class,
        RadarTempoServicoLicencaEspecialFilterDTO.class,PeriodoAquisitivoLicencaEspecial.class);
    for(var entry:types.entrySet()) for(var field:List.of("situacao","situacoes","situacoesExcluidas")) {
      var schema=entry.getKey().getDeclaredField(field).getAnnotation(io.swagger.v3.oas.annotations.media.Schema.class);
      assertThat(schema.allowableValues()).as("%s.%s",entry.getKey().getSimpleName(),field)
          .containsExactlyInAnyOrderElementsOf(TempoLicencaLifecycle.states(entry.getValue()));
    }
  }
}
