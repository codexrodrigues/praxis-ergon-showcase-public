package br.com.praxis.ergonshowcase;

import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.rules.ErgonRuleViolationException;
import br.com.praxis.ergonshowcase.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

class P5BSalePreviewRegressionTest {
  record Fixture(TempoLicencaCrudContractTest.Fixture data, TempoLicencaPreviewService preview) { }
  Fixture fixture() {
    var f = new TempoLicencaCrudContractTest.Fixture();
    var service = (VendaLicencaEspecialService) f.service(3)[0];
    var input = (CreateVendaLicencaEspecialDTO) new TempoLicencaCrudContractTest().create(3);
    f.pa.setDenDiasAbonoLicEsp(1);
    input.setDiasVendidos(60); service.create(input);
    var preview = new TempoLicencaPreviewService(f.tenant);
    ReflectionTestUtils.setField(preview, "em", f.em);
    return new Fixture(f, preview);
  }
  @ParameterizedTest @CsvSource({"60,30", "70,20", "30,60"})
  void editingReplacesTheExistingConsumptionInsteadOfDebitingItAgain(int days, int remaining) {
    var f = fixture();
    assertEquals(remaining, f.preview.venda(new TempoLicencaPrevia.Venda(20L, days, 91L)).diasPrevistos());
    assertEquals(30, f.data.pa.getSaldoDias()); assertEquals(60, f.data.vendas.getFirst().getDiasVendidos());
  }
  @Test void creationDoesNotBorrowAnExistingSalesCreditAndEditCannotExceedTheRecomposedBalance() {
    var f = fixture();
    assertEquals(10, f.preview.venda(new TempoLicencaPrevia.Venda(20L, 20, null)).diasPrevistos());
    rejects("CERG-09044", () -> f.preview.venda(new TempoLicencaPrevia.Venda(20L, 60, null)));
    rejects("CERG-09044", () -> f.preview.venda(new TempoLicencaPrevia.Venda(20L, 91, 91L)));
  }
  @Test void canceledSalesDoNotRestoreCreditTwice() {
    var f = fixture(); f.data.vendas.getFirst().setSituacao("CANCELADO"); f.data.pa.setSaldoDias(90);
    assertEquals(30, f.preview.venda(new TempoLicencaPrevia.Venda(20L, 60, 91L)).diasPrevistos());
    rejects("CERG-09044", () -> f.preview.venda(new TempoLicencaPrevia.Venda(20L, 91, 91L)));
  }
  @Test void missingOtherOrganizationOtherPeriodAndLinkedAbonoAreRejectedBeforeAnyCredit() {
    var f = fixture(); var sale = f.data.vendas.getFirst();
    assertEquals(404, assertThrows(ResponseStatusException.class,
        () -> f.preview.venda(new TempoLicencaPrevia.Venda(20L, 60, 999L))).getStatusCode().value());
    sale.setEmpCodigo(2);
    assertEquals(403, assertThrows(ResponseStatusException.class,
        () -> f.preview.venda(new TempoLicencaPrevia.Venda(20L, 60, 91L))).getStatusCode().value());
    sale.setEmpCodigo(1); sale.setPeriodoAquisitivoLicencaId(21L);
    rejects("CERG-09013", () -> f.preview.venda(new TempoLicencaPrevia.Venda(20L, 60, 91L)));
    sale.setPeriodoAquisitivoLicencaId(20L); sale.setLicencaEspecialGozoId(5L);
    rejects("ERG-03387", () -> f.preview.venda(new TempoLicencaPrevia.Venda(20L, 60, 91L)));
  }

  private static void rejects(String code, Runnable preview) {
    var violation = assertThrows(ErgonRuleViolationException.class, preview::run);
    assertEquals(422, violation.getHttpStatus());
    assertEquals(java.util.List.of(code), violation.getReasonCodes());
  }
}
