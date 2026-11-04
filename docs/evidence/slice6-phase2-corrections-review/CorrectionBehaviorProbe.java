package br.com.praxis.ergonshowcase;

import br.com.praxis.ergonshowcase.domain.*;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.service.*;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Production services/mappers with existing mocked storage; no SQL/transaction proof. */
public class CorrectionBehaviorProbe {
  public static void main(String[] args) {
    TempoLicencaIssue10RegressionTest.validator();
    try {
      var h=new TempoLicencaIssue10RegressionTest();
      var j=h.homologated("G");h.pay(j);var v=j.abono();
      assertThat(v.getSituacao()).isEqualTo("PAGA");
      assertThatThrownBy(()->j.venda().transition(v.getId(),"estornar-venda-licenca-especial",h.h.command(),h.version(v.getId(),0)))
          .hasMessageContaining("ERG-03387");
      assertThatThrownBy(()->j.gozo().transition(91L,"cancelar-licenca-especial",h.h.command(),h.version(91L,0)))
          .hasMessageContaining("Abono liquidado");
      var g=j.f().gozos.getFirst();
      when(j.f().em.find(LicencaEspecialGozo.class,91L,LockModeType.PESSIMISTIC_WRITE)).thenReturn(g);
      j.venda().transition(v.getId(),"reconciliar-abono-vinculado",h.h.command(),h.version(v.getId(),0));
      assertThat(v.getSituacao()).isEqualTo("PAGA");
      System.out.println("CONFIRMED: paid linked abono rejects estorno; gozo rejects cancellation; reconciliation preserves PAGA");

      var f=new TempoLicencaCrudContractTest.Fixture();var helper=new TempoLicencaCrudContractTest();
      var a=(AverbacaoTempoServicoService)f.service(0)[0];
      var ad=(CreateAverbacaoTempoServicoDTO)helper.create(0);ad.setDtini(LocalDate.of(2000,1,1));ad.setDtfim(LocalDate.of(2004,12,31));a.create(ad);
      var gs=(LicencaEspecialGozoService)f.service(2)[0];gs.create((CreateLicencaEspecialGozoDTO)helper.create(2));
      gs.transition(91L,"homologar-licenca-especial",helper.command(),helper.precondition(0));
      gs.transition(91L,"iniciar-gozo-licenca-especial",helper.command(),helper.precondition(0));
      var end=helper.command();end.setDataEfeito(LocalDate.of(2026,1,10));
      gs.transition(91L,"encerrar-licenca-especial",end,helper.precondition(0));
      // Valid graph admitted by V5: recognized origin, referenced period and consumed leave.
      // V6 leaves that origin EM_ANALISE even while its period becomes PARCIALMENTE_GOZADO.
      f.pa.setAverbacaoOrigemId(91L);f.periodos.add(f.pa);
      assertThat(f.pa.getDiasGozados()).isEqualTo(10);
      assertThatThrownBy(()->a.transition(91L,"homologar-averbacao",helper.command(),helper.precondition(0)))
          .hasMessageContaining("Regularize o consumo");
      assertThatThrownBy(()->a.transition(91L,"cancelar-averbacao",helper.command(),helper.precondition(0)))
          .hasMessageContaining("Regularize o consumo");
      System.out.println("CONFIRMED: migrated EM_ANALISE origin with consumed linked period cannot homologate or cancel");
    } finally { TempoLicencaIssue10RegressionTest.closeValidator(); }
  }
}
