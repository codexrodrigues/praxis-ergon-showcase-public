package br.com.praxis.ergonshowcase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.com.praxis.ergonshowcase.bulk.*;
import br.com.praxis.ergonshowcase.config.*;
import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.controller.LicencaEspecialGozoController;
import br.com.praxis.ergonshowcase.domain.*;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.mapper.LicencaEspecialGozoMapper;
import br.com.praxis.ergonshowcase.repository.*;
import br.com.praxis.ergonshowcase.rules.ErgonRuleViolationException;
import br.com.praxis.ergonshowcase.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mapstruct.factory.Mappers;
import org.praxisplatform.uischema.bulk.*;
import org.praxisplatform.uischema.capability.ResourceOperationAvailabilityContext;
import org.praxisplatform.uischema.concurrency.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.test.util.ReflectionTestUtils;

/** R2–R7: serviços/mappers reais; armazenamento simulado não prova transações PostgreSQL. */
class TempoLicencaIssue10RegressionTest {
  static ValidatorFactory validators;
  final TempoLicencaCrudContractTest h=new TempoLicencaCrudContractTest();
  final ObjectMapper json=new ObjectMapper().findAndRegisterModules();
  @BeforeAll static void validator() { validators=Validation.buildDefaultValidatorFactory(); }
  @AfterAll static void closeValidator() { validators.close(); }

  ResourceVersionUpdatePrecondition<Long> version(Long id,long revision) {
    var etags=new ResourceVersionEtagService("regression-test-secret");
    return new ResourceVersionUpdatePrecondition<>(etags,etags.create("test.resource",id,revision),"test.resource",id);
  }
  record Journey(TempoLicencaCrudContractTest.Fixture f, LicencaEspecialGozoService gozo,
                 VendaLicencaEspecialService venda, VendaLicencaEspecialRepository repository) {
    VendaLicencaEspecial abono() { return f.vendas.getFirst(); }
  }
  Journey homologated(String base) {
    var f=new TempoLicencaCrudContractTest.Fixture();f.pa.setAbonoDirGozoLicEsp(base);
    var gs=(LicencaEspecialGozoService)f.service(2)[0];var sale=f.service(3);
    var vs=(VendaLicencaEspecialService)sale[0];var repo=(VendaLicencaEspecialRepository)sale[1];
    ReflectionTestUtils.setField(gs,"beanValidator",validators.getValidator());
    ReflectionTestUtils.setField(vs,"beanValidator",validators.getValidator());
    var d=(CreateLicencaEspecialGozoDTO)h.create(2);
    d.setAbonoPecuniario(true);d.setDiasAbono("D".equals(base)?45:5);d.setIniFimAbono("F");
    d.setMesAnoFolha(LocalDate.of(2026,1,1));d.setNumFolha(1);gs.create(d);
    gs.transition(91L,"homologar-licenca-especial",h.command(),version(91L,0));
    var v=f.vendas.getFirst();when(repo.findById(v.getId())).thenReturn(Optional.of(v));
    return new Journey(f,gs,vs,repo);
  }
  void appraise(Journey j) {
    var patch=new VendaLicencaEspecialLiquidacaoPatchDTO();patch.setValorIndenizacao(new BigDecimal("1234.56"));
    j.venda.patch(j.abono().getId(),patch,version(j.abono().getId(),0));
  }
  void pay(Journey j) {
    appraise(j);j.venda.transition(j.abono().getId(),"pagar-venda-licenca-especial",h.command(),version(j.abono().getId(),0));
  }
  void start(Journey j) { j.gozo.transition(91L,"iniciar-gozo-licenca-especial",h.command(),version(91L,0)); }

  @Test void linkedAbonoIsReadableButCannotSeedAStandaloneSale() {
    var journey = homologated("G");
    var id = journey.abono().getId();
    var availability = new CargoDraftAvailability(mock(CargoRepository.class), journey.f().tenant);
    var security = org.springframework.security.core.context.SecurityContextHolder.getContext();
    var previous = security.getAuthentication();
    security.setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
        "operator", "test", org.springframework.security.core.authority.AuthorityUtils.createAuthorityList("ERGON_READ", "ERGON_WRITE")));
    try {
      var snapshot = journey.venda().findResourceStateSnapshot(id).orElseThrow();
      var key = ApiPaths.KEY_VENDAS_LICENCA_ESPECIAL;
      var path = ApiPaths.VENDAS_LICENCA_ESPECIAL;
      assertThat(availability.evaluate(ResourceOperationAvailabilityContext.item(key, path, "view", id, snapshot)).allowed()).isTrue();
      assertThat(availability.evaluate(ResourceOperationAvailabilityContext.item(key, path, "duplicate-draft", id, snapshot)).allowed()).isFalse();
      assertThatThrownBy(() -> journey.venda().duplicateDraft(id))
          .isInstanceOfSatisfying(org.springframework.web.server.ResponseStatusException.class,
              failure -> assertThat(failure.getStatusCode().value()).isEqualTo(403));

      var fixture = new TempoLicencaCrudContractTest.Fixture();
      var standalone = (VendaLicencaEspecialService) fixture.service(3)[0];
      var saleId = standalone.create((CreateVendaLicencaEspecialDTO) h.create(3)).id();
      var saleSnapshot = standalone.findResourceStateSnapshot(saleId).orElseThrow();
      assertThat(availability.evaluate(ResourceOperationAvailabilityContext.item(
          key, path, "duplicate-draft", saleId, saleSnapshot)).allowed()).isTrue();
      assertThat(standalone.duplicateDraft(saleId).getModalidade()).isEqualTo("PECUNIA");
    } finally {
      security.setAuthentication(previous);
    }
  }

  @Test void r3GeneratedAbonoCanBeAppraisedAndPaidThroughPublishedPatchAndAction() throws Exception {
    var j=homologated("G");var id=j.abono().getId();
    assertThat(j.abono().getValorIndenizacao()).isNull();
    assertThatThrownBy(()->j.venda.transition(id,"pagar-venda-licenca-especial",h.command(),version(id,0)))
        .hasMessageContaining("Liquidação exige");
    var patch=json.readValue("{\"valorIndenizacao\":1234.56,\"observacoes\":\"Apuração da indenização\"}",VendaLicencaEspecialLiquidacaoPatchDTO.class);
    var result=j.venda.patch(id,patch,version(id,0));
    assertThat(result.body().getValorIndenizacao()).isEqualByComparingTo("1234.56");
    assertThat(result.body().getMesAnoFolha()).isEqualTo(LocalDate.of(2026,1,1));
    assertThat(result.body().getLicencaEspecialGozoId()).isEqualTo(91L);
    var paid=j.venda.transition(id,"pagar-venda-licenca-especial",h.command(),version(id,0)).body();
    assertThat(paid.getSituacao()).isEqualTo("PAGA");assertThat(paid.getDtPagamento()).isEqualTo(h.command().getDataEfeito());
    assertThat(j.f.vendas).hasSize(1);assertThat(j.f.pa.getDiasVendidos()).isEqualTo(5);assertThat(j.f.pa.getSaldoDias()).isEqualTo(75);
    assertThatThrownBy(()->j.venda.transition(id,"pagar-venda-licenca-especial",h.command(),version(id,0))).hasMessageContaining("409");
  }
  @Test void r3LinkedFinancialPatchDoesNotPermitOriginPayrollOrPaidValueEditsAndChecksVersion() {
    var j=homologated("G");var id=j.abono().getId();var patch=new VendaLicencaEspecialLiquidacaoPatchDTO();
    patch.setNumFolha(2);patch.setValorIndenizacao(BigDecimal.TEN);
    assertThatThrownBy(()->j.venda.patch(id,patch,version(id,0))).isInstanceOfSatisfying(ErgonRuleViolationException.class, failure -> {
      assertThat(failure.getHttpStatus()).isEqualTo(422);
      assertThat(failure.getReasonCodes()).contains("ERG-03387");
    });
    assertThat(j.abono().getValorIndenizacao()).isNull();assertThat(j.abono().getNumFolha()).isEqualTo(1);
    var valueOnly=new VendaLicencaEspecialLiquidacaoPatchDTO();valueOnly.setValorIndenizacao(BigDecimal.TEN);
    assertThatThrownBy(()->j.venda.patch(id,valueOnly,version(id,7))).isInstanceOf(ResourceVersionPreconditionException.class);
    assertThatThrownBy(()->j.venda.patch(id,valueOnly,null)).isInstanceOf(ResourceVersionPreconditionException.class);
    pay(j);
    assertThatThrownBy(()->j.venda.patch(id,valueOnly,version(id,0))).hasMessageContaining("não podem ser retificados");
    assertThat(j.abono().getValorIndenizacao()).isEqualByComparingTo("1234.56");
    var full=new UpdateVendaLicencaEspecialDTO();BeanUtils.copyProperties(j.abono(),full);full.setDiasVendidos(6);
    assertThatThrownBy(()->j.venda.update(id,full,version(id,0))).isInstanceOfSatisfying(ErgonRuleViolationException.class, failure -> {
      assertThat(failure.getHttpStatus()).isEqualTo(422);
      assertThat(failure.getReasonCodes()).contains("ERG-03387");
    });
  }
  @ParameterizedTest @ValueSource(strings={"{\"mesAnoFolha\":\"2026-02-01\"}","{\"numFolha\":2}","{\"mesAnoFolha\":null}","{\"numFolha\":null}"})
  void r4PaidAbonoRejectsGozoPayrollPatchBeforeMerge(String body) throws Exception {
    var j=homologated("G");pay(j);
    var patch=json.readValue(body,LicencaEspecialGozoReagendamentoPatchDTO.class);
    assertThatThrownBy(()->j.gozo.patch(91L,patch,version(91L,0))).hasMessageContaining("Abono liquidado");
    assertThat(j.f.gozos.getFirst().getMesAnoFolha()).isEqualTo(LocalDate.of(2026,1,1));
    assertThat(j.f.gozos.getFirst().getNumFolha()).isEqualTo(1);
    assertThat(j.abono().getMesAnoFolha()).isEqualTo(LocalDate.of(2026,1,1));assertThat(j.abono().getNumFolha()).isEqualTo(1);
    assertThat(j.abono().getSituacao()).isEqualTo("PAGA");assertThat(j.abono().getDtPagamento()).isEqualTo(h.command().getDataEfeito());
  }
  @ParameterizedTest @EnumSource(value=BulkMode.class,names={"UNIFORM_UPDATE","PER_ITEM_UPDATE"})
  void r4PaidAbonoRejectsBulkPayrollChangeAndAllowsUnrelatedEdit(BulkMode mode) {
    var j=homologated("G");pay(j);
    var domain=new ErgonBulkDomain();ReflectionTestUtils.setField(domain,"em",j.f.em);
    ReflectionTestUtils.setField(domain,"tenant",j.f.tenant);ReflectionTestUtils.setField(domain,"json",json);
    ReflectionTestUtils.setField(domain,"validator",validators.getValidator());
    ReflectionTestUtils.setField(domain,"licencaEspecialGozo",Mappers.getMapper(LicencaEspecialGozoMapper.class));
    ReflectionTestUtils.setField(domain,"licencaEspecialGozoService",j.gozo);
    var fields=new TempoLicencaBulkAnalyticsExportTest().fields(UpdateLicencaEspecialGozoDTO.class,LicencaEspecialGozoController.class);
    assertThatThrownBy(()->BulkCandidateFixture.candidate(j.gozo,j.f.gozos.getFirst(),
        List.of(BulkFieldChange.set("numFolha",json.valueToTree(2))),fields,mode)).hasMessageContaining("Abono liquidado");
    var dto=(UpdateLicencaEspecialGozoDTO)BulkCandidateFixture.candidate(j.gozo,j.f.gozos.getFirst(),
        List.of(BulkFieldChange.set("observacoes",json.valueToTree("Revisão documental"))),fields,mode);
    j.gozo.update(91L,dto,version(91L,0));
    assertThat(j.abono().getNumFolha()).isEqualTo(1);assertThat(j.abono().getSituacao()).isEqualTo("PAGA");
  }
  @ParameterizedTest @ValueSource(strings={"APOSENTADORIA","TEMPO_SERVICO"})
  void r5LongNonLicenseAverbacaoSupportsCancellationAndDesaverbacao(String finalidade) {
    var f=new TempoLicencaCrudContractTest.Fixture();var s=(AverbacaoTempoServicoService)f.service(0)[0];
    var d=(CreateAverbacaoTempoServicoDTO)h.create(0);d.setFinalidade(finalidade);d.setTotalDias(5000);d.setDiasFinalidade(5000);
    d.setDtini(LocalDate.of(2000,1,1));d.setDtfim(d.getDtini().plusDays(4999));s.create(d);
    s.transition(91L,"homologar-averbacao",h.command(),version(91L,0));
    var quantity=h.command();quantity.setDias(10);
    assertThat(s.transition(91L,"registrar-desaverbacao",quantity,version(91L,0)).body().getDiasDesaverbados()).isEqualTo(10);
    assertThat(s.transition(91L,"reverter-desaverbacao",quantity,version(91L,0)).body().getDiasDesaverbados()).isZero();
    assertThat(s.transition(91L,"cancelar-averbacao",h.command(),version(91L,0)).body().getSituacao()).isEqualTo("CANCELADO");
    assertThat(f.periodos).isEmpty();
  }
  @Test void r5LicenseRightStillRejectsMoreThan999Days() {
    var f=new TempoLicencaCrudContractTest.Fixture();var s=(AverbacaoTempoServicoService)f.service(0)[0];
    var d=(CreateAverbacaoTempoServicoDTO)h.create(0);
    d.setDtfim(d.getDtini().plusDays(999));
    d.setTotalDias(1000); d.setDiasFinalidade(1000);
    assertThatThrownBy(() -> s.create(d))
        .isInstanceOfSatisfying(ErgonRuleViolationException.class,
            violation -> assertThat(violation.getReasonCodes()).contains("CERG-06109"));
    assertThat(f.periodos).isEmpty();
  }
  @ParameterizedTest @ValueSource(strings={"interromper-licenca-especial","encerrar-licenca-especial"})
  void r6EarlyEndRecalculatesUnpaidAbonoAndReleasesBalance(String action) {
    var j=homologated("G");appraise(j);start(j);
    var ended=j.gozo.transition(91L,action,h.command(),version(91L,0)).body();
    assertThat(ended.getDiasGozo()).isEqualTo(5);assertThat(ended.getDiasAbono()).isEqualTo(2);
    assertThat(ended.getDtprevfim()).isEqualTo(LocalDate.of(2026,1,10));
    assertThat(j.abono().getDiasVendidos()).isEqualTo(2);assertThat(j.abono().getValorIndenizacao()).isNull();
    assertThat(j.abono().getSituacao()).isEqualTo("AUTORIZADA");
    assertThat(j.f.pa.getDiasGozados()).isEqualTo(5);assertThat(j.f.pa.getDiasVendidos()).isEqualTo(2);assertThat(j.f.pa.getSaldoDias()).isEqualTo(83);
  }
  @Test void r6TruncationToZeroAbonoReversesOnlyUnpaidMirror() {
    var j=homologated("G");start(j);var d=h.command();d.setDataEfeito(LocalDate.of(2026,1,1));
    var ended=j.gozo.transition(91L,"interromper-licenca-especial",d,version(91L,0)).body();
    assertThat(ended.getDiasAbono()).isZero();assertThat(ended.getAbonoPecuniario()).isFalse();assertThat(ended.getIniFimAbono()).isNull();
    assertThat(j.abono().getSituacao()).isEqualTo("ESTORNADA");assertThat(j.f.pa.getSaldoDias()).isEqualTo(89);
  }
  @Test void r6PaidProportionalAbonoBlocksReductionWithoutChangingManagedGozo() {
    var j=homologated("G");pay(j);start(j);
    assertThatThrownBy(()->j.gozo.transition(91L,"interromper-licenca-especial",h.command(),version(91L,0))).hasMessageContaining("Abono liquidado");
    assertThat(j.f.gozos.getFirst().getSituacao()).isEqualTo("EM_GOZO");assertThat(j.f.gozos.getFirst().getDiasGozo()).isEqualTo(10);
    assertThat(j.abono().getDiasVendidos()).isEqualTo(5);assertThat(j.abono().getSituacao()).isEqualTo("PAGA");assertThat(j.f.pa.getSaldoDias()).isEqualTo(75);
  }
  @Test void r6RightBasedPaidAbonoIsPreservedWhenOnlyRestDaysChange() {
    var j=homologated("D");pay(j);start(j);
    var ended=j.gozo.transition(91L,"interromper-licenca-especial",h.command(),version(91L,0)).body();
    assertThat(ended.getDiasGozo()).isEqualTo(5);assertThat(ended.getDiasAbono()).isEqualTo(45);
    assertThat(j.abono().getSituacao()).isEqualTo("PAGA");assertThat(j.abono().getValorIndenizacao()).isEqualByComparingTo("1234.56");
    assertThat(j.f.pa.getSaldoDias()).isEqualTo(40);
  }
  @Test void r7GenericPutCannotClearOrReplaceConversionDateButCanMaintainOtherFields() throws Exception {
    var f=new TempoLicencaCrudContractTest.Fixture();var parts=f.service(1);var s=(PeriodoAquisitivoLicencaEspecialService)parts[0];
    var d=(CreatePeriodoAquisitivoLicencaEspecialDTO)h.create(1);d.setDtini(LocalDate.of(2015,1,1));d.setDtfim(LocalDate.of(2019,12,31));s.create(d);
    s.transition(91L,"homologar-periodo-licenca-especial",h.command(),version(91L,0));
    ((PeriodoAquisitivoLicencaEspecialRepository)parts[1]).findById(91L).orElseThrow().setContDobroLicEsp("S");
    var c=h.command();c.setDias(10);var response=s.transition(91L,"converter-em-dobro",c,version(91L,0)).body();
    var input=new UpdatePeriodoAquisitivoLicencaEspecialDTO();BeanUtils.copyProperties(response,input);
    for(var date:Arrays.asList(null,LocalDate.of(2026,2,1))) {
      var payload=json.valueToTree(input);
      if(date==null) ((com.fasterxml.jackson.databind.node.ObjectNode)payload).putNull("dtCntDobro");
      else ((com.fasterxml.jackson.databind.node.ObjectNode)payload).put("dtCntDobro",date.toString());
      var dto=json.treeToValue(payload,UpdatePeriodoAquisitivoLicencaEspecialDTO.class);
      assertThatThrownBy(()->s.update(91L,dto,version(91L,0))).hasMessageContaining("ação própria");
    }
    var dto=json.readValue(json.writeValueAsBytes(input),UpdatePeriodoAquisitivoLicencaEspecialDTO.class);dto.setObservacoes("Retificação documental");
    var maintained=s.update(91L,dto,version(91L,0)).body();
    assertThat(maintained.getDtCntDobro()).isEqualTo(c.getDataEfeito());assertThat(maintained.getDiasConvertidosDobro()).isEqualTo(10);assertThat(maintained.getSaldoDias()).isEqualTo(80);
  }
  @Test void r3FinancialPatchLocksGozoAndPeriodBeforeLinkedSale() {
    var j=homologated("G");clearInvocations(j.f.em);appraise(j);
    var ordered=inOrder(j.f.em);
    ordered.verify(j.f.em).lock(eq(j.f.gozos.getFirst()),eq(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE),anyMap());
    ordered.verify(j.f.em).refresh(j.f.gozos.getFirst());
    ordered.verify(j.f.em).lock(j.f.pa,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
    ordered.verify(j.f.em).refresh(j.f.pa);
    ordered.verify(j.f.em).lock(eq(j.abono()),eq(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE),anyMap());
    ordered.verify(j.f.em).refresh(j.abono());
  }
  @ParameterizedTest @ValueSource(strings={"dataDesaverbacao","motivoDesaverbacao","motivoRemocao"})
  void r7AverbacaoWorkflowEvidenceCannotBeChangedByPut(String field) {
    var f=new TempoLicencaCrudContractTest.Fixture();var s=(AverbacaoTempoServicoService)f.service(0)[0];
    var d=(CreateAverbacaoTempoServicoDTO)h.create(0);d.setFinalidade("APOSENTADORIA");s.create(d);
    s.transition(91L,"homologar-averbacao",h.command(),version(91L,0));var quantity=h.command();quantity.setDias(5);
    var response=s.transition(91L,"registrar-desaverbacao",quantity,version(91L,0)).body();
    var dto=new UpdateAverbacaoTempoServicoDTO();BeanUtils.copyProperties(response,dto);
    new BeanWrapperImpl(dto).setPropertyValue(field,field.startsWith("data")?LocalDate.of(2026,2,1):"Substituição do lastro");
    assertThatThrownBy(()->s.update(91L,dto,version(91L,0))).hasMessageContaining("ação própria");
  }
  @ParameterizedTest @ValueSource(strings={"SOLICITADA","AUTORIZADA"})
  void r2HistoricalPaymentCannotBeAuthorizedOrRecordedAgainEvenIfStateIsInconsistent(String state) {
    var f=new TempoLicencaCrudContractTest.Fixture();var s=(VendaLicencaEspecialService)f.service(3)[0];
    var d=(CreateVendaLicencaEspecialDTO)h.create(3);d.setValorIndenizacao(BigDecimal.TEN);d.setMesAnoFolha(LocalDate.of(2026,1,1));d.setNumFolha(1);s.create(d);
    var v=f.vendas.getFirst();v.setSituacao(state);v.setDtPagamento(LocalDate.of(2026,1,5));
    String action="SOLICITADA".equals(state)?"autorizar-venda-licenca-especial":"pagar-venda-licenca-especial";
    assertThatThrownBy(()->s.transition(91L,action,h.command(),version(91L,0))).hasMessageContaining("Pagamento histórico");
    assertThat(v.getDtPagamento()).isEqualTo(LocalDate.of(2026,1,5));assertThat(v.getSituacao()).isEqualTo(state);
  }
}
