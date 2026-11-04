package br.com.praxis.ergonshowcase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.com.praxis.ergonshowcase.domain.*;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.service.*;
import br.com.praxis.ergonshowcase.rules.ErgonRuleViolationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.converter.*;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.praxisplatform.uischema.extension.CustomOpenApiResolver;
import org.praxisplatform.uischema.repository.base.BaseCrudRepository;

/** Regressões C1–C8 em serviços e mappers reais. Não substituem a prova PostgreSQL/HTTP. */
@SuppressWarnings({"rawtypes", "unchecked"})
class TempoLicencaSevenFixesRegressionTest {
  final TempoLicencaCrudContractTest helpers = new TempoLicencaCrudContractTest();
  TempoLicencaCrudContractTest.Fixture fixture() { return new TempoLicencaCrudContractTest.Fixture(); }
  CreateVendaLicencaEspecialDTO sale() { return (CreateVendaLicencaEspecialDTO) helpers.create(3); }
  CreateLicencaEspecialGozoDTO gozo(boolean abono) {
    var g = (CreateLicencaEspecialGozoDTO) helpers.create(2);
    if (abono) { g.setAbonoPecuniario(true); g.setDiasAbono(5); g.setIniFimAbono("F"); }
    return g;
  }
  ParametroContagemFerias matrix(TempoLicencaCrudContractTest.Fixture f, boolean permits) {
    var p = new ParametroContagemFerias(); p.setEmpCodigo(1); p.setRegimeJuridico("EST"); p.setTipoVinculo("EFETIVO");
    p.setActive(true); p.setDtini(LocalDate.of(2010,1,1)); p.setVendLicEsp(permits);
    p.setAbonoDirGozoLicEsp("G"); p.setNumDiasAbonoLicEsp(1); p.setDenDiasAbonoLicEsp(2);
    f.parametros.add(p); return p;
  }

  @Test void c1InheritedPermissionDeniesFalseMissingInactiveAndForeignMatrixBeforeSave() {
    for (String scenario : List.of("false", "missing", "inactive", "foreign", "expired")) {
      var f=fixture(); f.pa.setVendLicEsp("I");
      if (!scenario.equals("missing")) {
        var p=matrix(f,!scenario.equals("false"));
        if (scenario.equals("inactive")) p.setActive(false);
        if (scenario.equals("foreign")) p.setEmpCodigo(2);
        if (scenario.equals("expired")) p.setDtfim(LocalDate.of(2011,1,1));
      }
      var parts=f.service(3); var s=(VendaLicencaEspecialService) parts[0];
      assertThatThrownBy(() -> s.create(sale())).as(scenario)
          .isInstanceOfSatisfying(ErgonRuleViolationException.class,
              violation -> assertThat(violation.getReasonCodes()).contains("ERG-03386"));
      verify((BaseCrudRepository) parts[1],never()).save(any());
      assertThat(f.pa.getSaldoDias()).isEqualTo(90);
    }
  }

  @Test void c1InheritedLimitsAndExplicitOverridesRespectAcquisitionAndExerciseDates() {
    var f=fixture(); f.pa.setVendLicEsp("I");
    var past=matrix(f,true); past.setDtfim(LocalDate.of(2020,12,31)); past.setMinDiasPvendLicEsp(15); past.setMaxDiasVendLicEsp(20);
    var recent=matrix(f,false); recent.setDtini(LocalDate.of(2021,1,1));
    var s=(VendaLicencaEspecialService) f.service(3)[0];
    // Aquisição termina em 2019: a regra de 2021 não substitui retroativamente a matriz.
    assertThatThrownBy(() -> s.create(sale())).hasMessageContaining("mínimo");
    var v=sale(); v.setDiasVendidos(25);
    assertThatThrownBy(() -> s.create(v)).hasMessageContaining("limite");
    v.setDiasVendidos(15); s.create(v); assertThat(f.pa.getSaldoDias()).isEqualTo(75);
    f.vinculo.setDtexerc(LocalDate.of(2022,1,1));
    assertThatThrownBy(() -> s.create(sale())).hasMessageContaining("vedada");
    f.pa.setVendLicEsp("S"); f.pa.setMinDiasPvendLicEsp(1); f.pa.setMaxDiasVendLicEsp(90);
    s.create(sale()); assertThat(f.pa.getSaldoDias()).isEqualTo(65);
    f.pa.setVendLicEsp("N");
    assertThatThrownBy(() -> s.create(sale())).hasMessageContaining("vedada");
  }

  @Test void c2AbonoCannotBypassItsOwnPolicySalePermissionOrRatio() {
    for (String scenario : List.of("abonoN", "saleN", "inheritFalse", "missingRatio", "zeroDenominator", "wrongRatio")) {
      var f=fixture();
      switch(scenario) {
        case "abonoN" -> f.pa.setAbonoDirGozoLicEsp("N");
        case "saleN" -> f.pa.setVendLicEsp("N");
        case "inheritFalse" -> { f.pa.setVendLicEsp("I"); matrix(f,false); }
        case "missingRatio" -> f.pa.setNumDiasAbonoLicEsp(null);
        case "zeroDenominator" -> f.pa.setDenDiasAbonoLicEsp(0);
        case "wrongRatio" -> f.pa.setDenDiasAbonoLicEsp(3);
      }
      var parts=f.service(2); var s=(LicencaEspecialGozoService) parts[0];
      assertThatThrownBy(() -> s.create(gozo(true))).as(scenario)
          .isInstanceOfSatisfying(ErgonRuleViolationException.class,
              violation -> assertThat(violation.getReasonCodes()).contains("CERG-06309"));
      verify((BaseCrudRepository) parts[1],never()).save(any());
      assertThat(f.pa.getDiasVendidos()).isZero();
    }
  }

  @Test void c2InheritedAbonoAndRightsBaseAreAppliedAndOnlyOneRightsAbonoIsAllowed() {
    var f=fixture(); f.pa.setVendLicEsp("I"); f.pa.setAbonoDirGozoLicEsp(null);
    f.pa.setNumDiasAbonoLicEsp(null); f.pa.setDenDiasAbonoLicEsp(null);
    var p=matrix(f,true); p.setAbonoDirGozoLicEsp("D"); p.setDenDiasAbonoLicEsp(3);
    var s=(LicencaEspecialGozoService) f.service(2)[0];
    var g=gozo(true); g.setDiasAbono(30); s.create(g);
    assertThat(f.pa.getDiasVendidos()).isEqualTo(30); assertThat(f.pa.getSaldoDias()).isEqualTo(50);
    var other=gozo(true); other.setOrdemParcela(2); other.setDiasAbono(30);
    other.setDtini(LocalDate.of(2026, 2, 1));
    other.setDtprevfim(LocalDate.of(2026, 2, 10));
    assertThatThrownBy(() -> s.create(other))
        .isInstanceOfSatisfying(ErgonRuleViolationException.class,
            violation -> assertThat(violation.getReasonCodes()).contains("ERG-00703"));
  }

  @Test void c3BothCreationOrdersAndUpdatesShareOnePecuniaryLimit() {
    for(boolean gozoFirst: List.of(true,false)) {
      var f=fixture(); f.pa.setMaxDiasVendLicEsp(10);
      var gs=(LicencaEspecialGozoService) f.service(2)[0]; var vs=(VendaLicencaEspecialService) f.service(3)[0];
      if(gozoFirst) {
        var g=gozo(true); gs.create(g);
        assertThatThrownBy(() -> vs.create(sale())).hasMessageContaining("limite");
        var v=sale(); v.setDiasVendidos(5); vs.create(v);
        var u=(UpdateVendaLicencaEspecialDTO) helpers.update(3,v);
        vs.update(91L,u,helpers.precondition(0)); // exclui o próprio consumo anterior
        u.setDiasVendidos(6);
        assertThatThrownBy(() -> vs.update(91L,u,helpers.precondition(0))).hasMessageContaining("limite");
        var gu=(UpdateLicencaEspecialGozoDTO) helpers.update(2,g);
        gs.update(91L,gu,helpers.precondition(0)); // abono próprio também não é contado duas vezes
        gu.setDiasGozo(12);
        gu.setDtprevfim(LocalDate.of(2026,1,12)); gu.setDiasAbono(6);
        assertThatThrownBy(() -> gs.update(91L,gu,helpers.precondition(0))).hasMessageContaining("limite");
      } else {
        vs.create(sale());
        assertThatThrownBy(() -> gs.create(gozo(true))).hasMessageContaining("limite");
      }
      assertThat(f.pa.getDiasVendidos()).isLessThanOrEqualTo(10);
    }
  }

  @Test void c4SaleCanBeCancelledAfterPermissionAndLimitsChangeWithoutDeletingHistory() {
    var f=fixture(); var s=(VendaLicencaEspecialService) f.service(3)[0]; var v=sale(); s.create(v);
    f.pa.setVendLicEsp("N"); f.pa.setMinDiasPvendLicEsp(20); f.pa.setMaxDiasVendLicEsp(0);
    var u=(UpdateVendaLicencaEspecialDTO) helpers.update(3,v);
    s.transition(91L,"estornar-venda-licenca-especial",helpers.command(),helpers.precondition(0));
    assertThat(f.pa.getSaldoDias()).isEqualTo(90); assertThat(f.vendas).hasSize(1);
    assertThat(f.vendas.getFirst().getSituacao()).isEqualTo("ESTORNADA");
    u.setSituacao("SOLICITADA");
    assertThatThrownBy(() -> s.update(91L,u,helpers.precondition(0))).hasMessageContaining("ciclo de vida");
  }

  @Test void c5LegacyCancelledParentStillAllowsChildEstornoButNotReactivation() {
    var f=fixture(); var s=(LicencaEspecialGozoService) f.service(2)[0]; var g=gozo(true); s.create(g);
    f.pa.setSituacao("CANCELADO"); f.pa.setAbonoDirGozoLicEsp("N");
    var u=(UpdateLicencaEspecialGozoDTO) helpers.update(2,g);
    s.transition(91L,"cancelar-licenca-especial",helpers.command(),helpers.precondition(0)); assertThat(f.pa.getSaldoDias()).isEqualTo(90);
    u.setSituacao("PROGRAMADA");
    assertThatThrownBy(() -> s.update(91L,u,helpers.precondition(0))).hasMessageContaining("ciclo de vida");
  }

  @Test void c5NewParentCancellationRequiresChildrenToBeCancelledFirst() {
    var f=fixture(); var parts=f.service(1); var ps=(PeriodoAquisitivoLicencaEspecialService) parts[0];
    var pc=(CreatePeriodoAquisitivoLicencaEspecialDTO) helpers.create(1); ps.create(pc);
    var saved=(PeriodoAquisitivoLicencaEspecial)((BaseCrudRepository)parts[1]).findById(91L).orElseThrow();
    when(f.em.find(PeriodoAquisitivoLicencaEspecial.class,91L)).thenReturn(saved);
    var gs=(LicencaEspecialGozoService) f.service(2)[0]; var g=gozo(false);
    g.setPeriodoAquisitivoLicencaId(91L);
    g.setDtini(pc.getDtfim().plusDays(1));
    g.setDtprevfim(g.getDtini().plusDays(9));
    gs.create(g);
    var u=(UpdatePeriodoAquisitivoLicencaEspecialDTO) helpers.update(1,pc); u.setSituacao("CANCELADO");
    clearInvocations((BaseCrudRepository)parts[1]);
    assertThatThrownBy(() -> ps.transition(91L,"cancelar-periodo-licenca-especial",helpers.command(),helpers.precondition(0))).hasMessageContaining("parcelas ativas");
    verify((BaseCrudRepository)parts[1],never()).save(any());
  }

  AverbacaoTempoServico origin(TempoLicencaCrudContractTest.Fixture f) {
    var a=new AverbacaoTempoServico(); a.setId(30L); a.setEmpCodigo(1); a.setNumfunc(1001L); a.setNumvinc(1);
    a.setFinalidade("LICENCA_ESPECIAL"); a.setSituacao("HOMOLOGADA"); when(f.em.find(AverbacaoTempoServico.class,30L)).thenReturn(a); return a;
  }
  @Test void c6NewOriginRejectsCancellationRemovalAndWrongPurposeSeparately() {
    for(String scenario:List.of("cancelled","removed","purpose")) {
      var f=fixture(); var a=origin(f);
      if(scenario.equals("cancelled")) a.setSituacao("CANCELADO");
      if(scenario.equals("removed")) a.setDataRemocao(LocalDate.of(2020,1,1));
      if(scenario.equals("purpose")) a.setFinalidade("APOSENTADORIA");
      var s=(PeriodoAquisitivoLicencaEspecialService) f.service(1)[0];
      var dto=(CreatePeriodoAquisitivoLicencaEspecialDTO) helpers.create(1); dto.setAverbacaoOrigemId(30L);
      assertThatThrownBy(() -> s.create(dto)).as(scenario).hasMessageContaining("Nova origem");
    }
  }

  @Test void c6ExistingHistoricalOriginCanBeRetainedButNewAssociationCannotUseIt() {
    var f=fixture(); var a=origin(f); var s=(PeriodoAquisitivoLicencaEspecialService) f.service(1)[0];
    var dto=(CreatePeriodoAquisitivoLicencaEspecialDTO) helpers.create(1); dto.setAverbacaoOrigemId(30L); s.create(dto);
    a.setSituacao("CANCELADO"); a.setDataRemocao(LocalDate.of(2020,1,1));
    var u=(UpdatePeriodoAquisitivoLicencaEspecialDTO) helpers.update(1,dto);
    s.update(91L,u,helpers.precondition(0));
    assertThatThrownBy(() -> s.create(dto)).hasMessageContaining("Nova origem");
  }

  @Test void c6UpdateCannotIntroduceAnIneligibleOrigin() {
    var f=fixture(); var a=origin(f); a.setSituacao("CANCELADO");
    var parts=f.service(1); var s=(PeriodoAquisitivoLicencaEspecialService) parts[0];
    var dto=(CreatePeriodoAquisitivoLicencaEspecialDTO) helpers.create(1); s.create(dto);
    var u=(UpdatePeriodoAquisitivoLicencaEspecialDTO) helpers.update(1,dto); u.setAverbacaoOrigemId(30L);
    clearInvocations((BaseCrudRepository)parts[1]);
    assertThatThrownBy(() -> s.update(91L,u,helpers.precondition(0))).hasMessageContaining("Nova origem");
    verify((BaseCrudRepository)parts[1],never()).save(any());
  }

  @Test void c7ClosedCreateUsesActualDurationInsteadOfForecast() {
    var f=fixture();var s=(LicencaEspecialGozoService)f.service(2)[0];var g=gozo(false);g.setDtfim(LocalDate.of(2026,1,5));g.setDiasGozo(5);
    assertThatThrownBy(()->s.create(g)).hasMessageContaining("não pode nascer encerrada");
  }
  @Test void c7ActualClosureReleasesReservationWhileOriginalForecastIsRetained() {
    var f=fixture();var s=(LicencaEspecialGozoService)f.service(2)[0];s.create(gozo(false));
    s.transition(91L,"homologar-licenca-especial",helpers.command(),helpers.precondition(0));
    s.transition(91L,"iniciar-gozo-licenca-especial",helpers.command(),helpers.precondition(0));
    var result=s.transition(91L,"encerrar-licenca-especial",helpers.command(),helpers.precondition(0));
    assertThat(result.body().getDtprevfim()).isEqualTo(LocalDate.of(2026,1,10));
    assertThat(result.body().getSituacao()).isEqualTo("GOZADA");
    assertThat(f.pa.getDiasGozados()).isEqualTo(5);assertThat(f.pa.getSaldoDias()).isEqualTo(85);
  }

  @Test void c8NonDefaultRightAndSubsequentEditsDeriveTotalsWithoutClientArithmetic() {
    var f=fixture(); var parts=f.service(1); var s=(PeriodoAquisitivoLicencaEspecialService) parts[0];
    var dto=(CreatePeriodoAquisitivoLicencaEspecialDTO) helpers.create(1); dto.setDiasDireito(30);
    var created=s.create(dto);
    var saved=(PeriodoAquisitivoLicencaEspecial)((BaseCrudRepository)parts[1]).findById(91L).orElseThrow();
    assertThat(saved.getSaldoDias()).isEqualTo(30);
    var u=(UpdatePeriodoAquisitivoLicencaEspecialDTO) helpers.update(1,dto); u.setDiasDireito(40);
    assertThat(s.update(91L,u,helpers.precondition(0)).body().getSaldoDias()).isEqualTo(40);
    var child=new LicencaEspecialGozo(); child.setPeriodoAquisitivoLicencaId(91L); child.setDiasGozo(5); f.gozos.add(child);
    var foreign=new LicencaEspecialGozo(); foreign.setPeriodoAquisitivoLicencaId(20L); foreign.setDiasGozo(20); f.gozos.add(foreign);
    u.setDiasDireito(50);
    assertThatThrownBy(() -> s.update(91L,u,helpers.precondition(0)))
        .isInstanceOfSatisfying(ErgonRuleViolationException.class,
            violation -> assertThat(violation.getReasonCodes()).contains("CERG-06221"));
    u.setDiasDireito(40);
    var result=s.update(91L,u,helpers.precondition(0));
    assertThat(result.body().getSaldoDias()).isEqualTo(35);
    assertThat(result.body().getDiasGozados()).isEqualTo(5);
    assertThat(result.body().getDiasVendidos()).isZero();
    u.setDiasDireito(4);
    assertThatThrownBy(() -> s.update(91L,u,helpers.precondition(0)))
        .isInstanceOfSatisfying(ErgonRuleViolationException.class,
            violation -> assertThat(violation.getReasonCodes()).contains("ERG-02102"));
    assertThat(saved.getDiasDireito()).isEqualTo(40);
  }

  @Test void c8RequestSchemasOmitDerivedTotalsAndResponsePublishesReadonlyMicroVisualization() {
    var converters=new ModelConverters(); converters.addConverter(new CustomOpenApiResolver(new ObjectMapper()));
    for(var type:List.of(CreatePeriodoAquisitivoLicencaEspecialDTO.class,UpdatePeriodoAquisitivoLicencaEspecialDTO.class)) {
      var schema=converters.resolveAsResolvedSchema(new AnnotatedType(type).resolveAsRef(false)).schema;
      assertThat(schema.getProperties()).doesNotContainKeys("saldoDias","diasGozados","diasVendidos");
    }
    var schema=converters.resolveAsResolvedSchema(new AnnotatedType(PeriodoAquisitivoLicencaEspecialResponseDTO.class).resolveAsRef(false)).schema;
    for(String name:List.of("saldoDias","diasGozados","diasVendidos"))
      assertThat(((io.swagger.v3.oas.models.media.Schema)schema.getProperties().get(name)).getReadOnly()).isTrue();
    var saldo=(io.swagger.v3.oas.models.media.Schema)schema.getProperties().get("saldoDias");
    assertThat(saldo.getExtensions().get("x-ui").toString()).contains("microVisualization");
  }
}
