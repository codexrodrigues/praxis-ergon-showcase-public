package br.com.praxis.ergonshowcase;

import br.com.praxis.ergonshowcase.domain.*;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.service.*;
import java.lang.reflect.Method;
import java.util.*;
import org.praxisplatform.uischema.concurrency.*;

/** Counterexamples on the same mocked storage as the delivered contract tests. No database. */
public class CrudReviewProbe {
  static final TempoLicencaCrudContractTest helpers = new TempoLicencaCrudContractTest();
  static Object invoke(String name, Class<?>[] signature, Object... args) throws Exception {
    Method m = TempoLicencaCrudContractTest.class.getDeclaredMethod(name, signature);
    m.setAccessible(true); return m.invoke(helpers, args);
  }
  static Object create(int kind) throws Exception { return invoke("create", new Class[]{int.class}, kind); }
  static Object update(int kind, Object dto) throws Exception { return invoke("update", new Class[]{int.class,Object.class}, kind,dto); }
  static ResourceVersionUpdatePrecondition<Long> token() throws Exception {
    return (ResourceVersionUpdatePrecondition<Long>) invoke("precondition",new Class[]{long.class},0L);
  }
  static void check(boolean condition, String name) { if (!condition) throw new AssertionError(name); }
  public static void main(String[] args) throws Exception {
    var f = new TempoLicencaCrudContractTest.Fixture();
    var p = new ParametroContagemFerias(); p.setVendLicEsp(false); p.setActive(true);
    p.setEmpCodigo(1); p.setRegimeJuridico("EST"); p.setTipoVinculo("EFETIVO");
    p.setDtini(java.time.LocalDate.of(2020,1,1)); f.parametros.add(p);
    var sale = (VendaLicencaEspecialService) f.service(3)[0];
    sale.create((CreateVendaLicencaEspecialDTO) create(3));
    check(f.pa.getSaldoDias()==80,"inherited sale");
    System.out.println("C1: vendLicEsp=I, general=false -> SALE ACCEPTED, saldo=80");

    f = new TempoLicencaCrudContractTest.Fixture();
    f.pa.setVendLicEsp("N"); f.pa.setAbonoDirGozoLicEsp("N"); f.pa.setMaxDiasVendLicEsp(0);
    var gozo = (LicencaEspecialGozoService) f.service(2)[0];
    var g = (CreateLicencaEspecialGozoDTO) create(2);
    g.setAbonoPecuniario(true); g.setDiasAbono(5); g.setIniFimAbono("F"); gozo.create(g);
    check(f.pa.getDiasVendidos()==5,"abono");
    System.out.println("C2: abono=N, venda=N, maxVenda=0 -> ABONO ACCEPTED, diasVendidos=5");

    f = new TempoLicencaCrudContractTest.Fixture(); f.pa.setMaxDiasVendLicEsp(10);
    gozo = (LicencaEspecialGozoService) f.service(2)[0]; gozo.create(g);
    sale = (VendaLicencaEspecialService) f.service(3)[0]; sale.create((CreateVendaLicencaEspecialDTO) create(3));
    check(f.pa.getDiasVendidos()==15,"combined maximum");
    System.out.println("C3: maxVenda=10, abono5+sale10 -> ACCEPTED, diasVendidos=15");

    f = new TempoLicencaCrudContractTest.Fixture(); sale=(VendaLicencaEspecialService) f.service(3)[0];
    var v=(CreateVendaLicencaEspecialDTO) create(3); sale.create(v); f.pa.setVendLicEsp("N");
    var u=(UpdateVendaLicencaEspecialDTO) update(3,v); u.setSituacao("CANCELADO");
    try { sale.update(91L,u,token()); throw new AssertionError("cancel should reproduce rejection"); }
    catch (org.springframework.web.server.ResponseStatusException e) {
      check(e.getStatusCode().value()==422,"cancel status");
      System.out.println("C4: existing sale, policy changed N, CANCEL -> "+e.getMessage()+", saldo="+f.pa.getSaldoDias());
    }
    f = new TempoLicencaCrudContractTest.Fixture(); gozo=(LicencaEspecialGozoService) f.service(2)[0];
    g=(CreateLicencaEspecialGozoDTO) create(2); gozo.create(g); f.pa.setSituacao("CANCELADO");
    var gu=(UpdateLicencaEspecialGozoDTO) update(2,g); gu.setSituacao("CANCELADO");
    try { gozo.update(91L,gu,token()); throw new AssertionError("cancel should reproduce rejection"); }
    catch (org.springframework.web.server.ResponseStatusException e) {
      check(e.getStatusCode().value()==422,"parent cancel status");
      System.out.println("C5: parent CANCELADO, child CANCEL -> "+e.getMessage()+", saldo="+f.pa.getSaldoDias());
    }
    f = new TempoLicencaCrudContractTest.Fixture();
    var origin = new AverbacaoTempoServico(); origin.setId(30L); origin.setEmpCodigo(1);
    origin.setNumfunc(1001L); origin.setNumvinc(1); origin.setSituacao("CANCELADO");
    origin.setFinalidade("APOSENTADORIA"); origin.setDataRemocao(java.time.LocalDate.of(2020,1,1));
    org.mockito.Mockito.when(f.em.find(AverbacaoTempoServico.class,30L)).thenReturn(origin);
    var ps=(PeriodoAquisitivoLicencaEspecialService) f.service(1)[0];
    var pc=(CreatePeriodoAquisitivoLicencaEspecialDTO) create(1); pc.setAverbacaoOrigemId(30L); ps.create(pc);
    System.out.println("C6: origin CANCELADO+removed+APOSENTADORIA -> LICENSE PERIOD ACCEPTED");

    f = new TempoLicencaCrudContractTest.Fixture(); gozo=(LicencaEspecialGozoService) f.service(2)[0];
    g=(CreateLicencaEspecialGozoDTO) create(2); g.setDtfim(java.time.LocalDate.of(2026,1,5)); gozo.create(g);
    check(f.pa.getDiasGozados()==10,"actual duration");
    System.out.println("C7: actual Jan1..Jan5, planned Jan1..Jan10 -> diasGozados=10, saldo=80");
    gu=(UpdateLicencaEspecialGozoDTO) update(2,g); gu.setDiasGozo(5);
    try { gozo.update(91L,gu,token()); throw new AssertionError("actual duration should reproduce rejection"); }
    catch (org.springframework.web.server.ResponseStatusException e) {
      System.out.println("C7b: set diasGozo=5 retaining original forecast -> "+e.getMessage());
    }

    f = new TempoLicencaCrudContractTest.Fixture(); ps=(PeriodoAquisitivoLicencaEspecialService) f.service(1)[0];
    pc=(CreatePeriodoAquisitivoLicencaEspecialDTO) create(1); pc.setDiasDireito(30);
    try { ps.create(pc); throw new AssertionError("default saldo should reproduce rejection"); }
    catch (org.springframework.web.server.ResponseStatusException e) {
      System.out.println("C8: create diasDireito=30, unchanged default saldo=90 -> "+e.getMessage());
    }
  }
}
