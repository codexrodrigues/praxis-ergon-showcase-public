package br.com.praxis.ergonshowcase;

import br.com.praxis.ergonshowcase.domain.*;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.service.*;
import br.com.praxis.ergonshowcase.repository.*;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.beans.BeanUtils;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Independent counterexamples. Real services/mappers, mocked storage; no transaction/SQL proof. */
public class Phase2ReviewProbe {
  static final TempoLicencaCrudContractTest h = new TempoLicencaCrudContractTest();
  static int passed;
  static org.praxisplatform.uischema.concurrency.ResourceVersionUpdatePrecondition<Long> precondition(Long id) {
    var etags=new org.praxisplatform.uischema.concurrency.ResourceVersionEtagService("synthetic-test-secret");
    return new org.praxisplatform.uischema.concurrency.ResourceVersionUpdatePrecondition<>(etags,etags.create("test.resource",id,0),"test.resource",id);
  }
  static CreateLicencaEspecialGozoDTO gozo() {
    var d=(CreateLicencaEspecialGozoDTO)h.create(2);
    d.setAbonoPecuniario(true);d.setDiasAbono(5);d.setIniFimAbono("F");
    d.setMesAnoFolha(LocalDate.of(2026,1,1));d.setNumFolha(1);return d;
  }
  static void run(String name, Runnable proof) {
    try { proof.run();passed++;System.out.println("CONFIRMED " + name); }
    catch(Throwable t) { System.out.println("PROBE_FAILED " + name);t.printStackTrace(); }
  }
  public static void main(String[] args) {
    run("C: non-license 5000-day averbação homologates but cancellation fails with license cap",()->{
      var f=new TempoLicencaCrudContractTest.Fixture();var s=(AverbacaoTempoServicoService)f.service(0)[0];
      var d=(CreateAverbacaoTempoServicoDTO)h.create(0);d.setFinalidade("APOSENTADORIA");d.setTotalDias(5000);d.setDiasFinalidade(5000);
      s.create(d);assertThat(s.transition(91L,"homologar-averbacao",h.command(),h.precondition(0)).body().getSituacao()).isEqualTo("HOMOLOGADA");
      assertThat(f.periodos).isEmpty();
      assertThatThrownBy(()->s.transition(91L,"cancelar-averbacao",h.command(),h.precondition(0))).hasMessageContaining("999 dias");
    });
    run("D: valid 10-day gozo + 5-day abono cannot be interrupted on day 5",()->{
      var f=new TempoLicencaCrudContractTest.Fixture();var s=(LicencaEspecialGozoService)f.service(2)[0];s.create(gozo());
      s.transition(91L,"homologar-licenca-especial",h.command(),h.precondition(0));
      s.transition(91L,"iniciar-gozo-licenca-especial",h.command(),h.precondition(0));
      assertThatThrownBy(()->s.transition(91L,"interromper-licenca-especial",h.command(),h.precondition(0))).hasMessageContaining("Dias de abono divergem");
    });
    run("E: generated authorized abono has no value, cannot be paid, and cannot accept liquidation PATCH",()->{
      var f=new TempoLicencaCrudContractTest.Fixture();var gs=(LicencaEspecialGozoService)f.service(2)[0];gs.create(gozo());
      gs.transition(91L,"homologar-licenca-especial",h.command(),h.precondition(0));
      var v=f.vendas.getFirst();assertThat(v.getSituacao()).isEqualTo("AUTORIZADA");assertThat(v.getValorIndenizacao()).isNull();
      var p=f.service(3);var vs=(VendaLicencaEspecialService)p[0];var repo=(VendaLicencaEspecialRepository)p[1];
      when(repo.findById(v.getId())).thenReturn(Optional.of(v));
      assertThatThrownBy(()->vs.transition(v.getId(),"pagar-venda-licenca-especial",h.command(),precondition(v.getId()))).hasMessageContaining("Liquidação exige");
      try(var factory=jakarta.validation.Validation.buildDefaultValidatorFactory()) {
        ReflectionTestUtils.setField(vs,"beanValidator",factory.getValidator());
        var patch=new VendaLicencaEspecialLiquidacaoPatchDTO();patch.setValorIndenizacao(new BigDecimal("1234.56"));
        assertThatThrownBy(()->vs.patch(v.getId(),patch,precondition(v.getId()))).hasMessageContaining("ERG-03387");
      }
    });
    run("B: gozo PATCH silently overwrites payroll month/number of existing paid abono",()->{
      var f=new TempoLicencaCrudContractTest.Fixture();var s=(LicencaEspecialGozoService)f.service(2)[0];s.create(gozo());
      s.transition(91L,"homologar-licenca-especial",h.command(),h.precondition(0));
      var v=f.vendas.getFirst();
      // Existing paid historical record is a valid domain state; no SQL/payment simulation claimed.
      v.setSituacao("PAGA");v.setDtPagamento(LocalDate.of(2026,1,5));v.setValorIndenizacao(new BigDecimal("1234.56"));
      try(var factory=jakarta.validation.Validation.buildDefaultValidatorFactory()) {
        ReflectionTestUtils.setField(s,"beanValidator",factory.getValidator());
        var patch=new LicencaEspecialGozoReagendamentoPatchDTO();patch.setMesAnoFolha(LocalDate.of(2026,2,1));patch.setNumFolha(2);
        s.patch(91L,patch,h.precondition(0));
      }
      assertThat(v.getSituacao()).isEqualTo("PAGA");assertThat(v.getMesAnoFolha()).isEqualTo(LocalDate.of(2026,2,1));assertThat(v.getNumFolha()).isEqualTo(2);
      assertThat(v.getDtPagamento()).isEqualTo(LocalDate.of(2026,1,5));
    });
    run("F: generic PUT clears effective date of completed 10-day conversion while preserving converted debit",()->{
      var f=new TempoLicencaCrudContractTest.Fixture();var parts=f.service(1);var s=(PeriodoAquisitivoLicencaEspecialService)parts[0];
      var d=(CreatePeriodoAquisitivoLicencaEspecialDTO)h.create(1);d.setDtfim(LocalDate.of(2026,1,1));s.create(d);
      s.transition(91L,"homologar-periodo-licenca-especial",h.command(),h.precondition(0));
      var repo=(PeriodoAquisitivoLicencaEspecialRepository)parts[1];repo.findById(91L).orElseThrow().setContDobroLicEsp("S");
      var command=h.command();command.setDias(10);
      var converted=s.transition(91L,"converter-em-dobro",command,h.precondition(0)).body();
      assertThat(converted.getDtCntDobro()).isEqualTo(command.getDataEfeito());
      var update=new UpdatePeriodoAquisitivoLicencaEspecialDTO();BeanUtils.copyProperties(converted,update);update.setDtCntDobro(null);
      try {
        var json=new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules();
        var acceptsDate=json.readValue("{\"dtCntDobro\":\"2026-02-01\"}",UpdatePeriodoAquisitivoLicencaEspecialDTO.class);
        assertThat(acceptsDate.getDtCntDobro()).isEqualTo(LocalDate.of(2026,2,1));
        update=json.readValue(json.writeValueAsBytes(update),UpdatePeriodoAquisitivoLicencaEspecialDTO.class);
      } catch(java.io.IOException e) { throw new AssertionError(e); }
      var response=s.update(91L,update,h.precondition(0)).body();
      assertThat(response.getDiasConvertidosDobro()).isEqualTo(10);assertThat(response.getSaldoDias()).isEqualTo(80);
      assertThat(response.getDtCntDobro()).isNull();assertThat(response.getSituacao()).isEqualTo("CONVERTIDO_EM_DOBRO");
    });
    run("G: migrated SOLICITADA sale with historical payment date can be authorized and paid again, overwriting original date",()->{
      var f=new TempoLicencaCrudContractTest.Fixture();var parts=f.service(3);var s=(VendaLicencaEspecialService)parts[0];
      var d=(CreateVendaLicencaEspecialDTO)h.create(3);d.setValorIndenizacao(new BigDecimal("1234.56"));d.setMesAnoFolha(LocalDate.of(2026,1,1));d.setNumFolha(1);s.create(d);
      // This combination is produced by the unmodified V6, independently verified against PostgreSQL.
      var repo=(VendaLicencaEspecialRepository)parts[1];repo.findById(91L).orElseThrow().setDtPagamento(LocalDate.of(2026,1,5));
      s.transition(91L,"autorizar-venda-licenca-especial",h.command(),h.precondition(0));
      var payment=h.command();payment.setDataEfeito(LocalDate.of(2026,2,5));
      var response=s.transition(91L,"pagar-venda-licenca-especial",payment,h.precondition(0)).body();
      assertThat(response.getSituacao()).isEqualTo("PAGA");assertThat(response.getDtPagamento()).isEqualTo(LocalDate.of(2026,2,5));
    });
    System.out.println("COUNTEREXAMPLES_CONFIRMED="+passed+"/6");
    if(passed!=6)throw new AssertionError("One or more independent probes failed");
  }
}
