package br.com.praxis.ergonshowcase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.controller.AverbacaoTempoServicoController;
import br.com.praxis.ergonshowcase.controller.LicencaEspecialGozoController;
import br.com.praxis.ergonshowcase.config.*;
import br.com.praxis.ergonshowcase.domain.*;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.mapper.*;
import br.com.praxis.ergonshowcase.repository.*;
import br.com.praxis.ergonshowcase.service.*;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.praxisplatform.uischema.concurrency.*;
import org.praxisplatform.uischema.repository.base.BaseCrudRepository;
import org.praxisplatform.uischema.service.base.*;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.test.util.ReflectionTestUtils;

/** Fluxo real de serviço e hooks com armazenamento simulado; não prova locks PostgreSQL. */
@SuppressWarnings({"rawtypes", "unchecked"})
class TempoLicencaCrudContractTest {
  static class Fixture {
    final EntityManager em = mock(EntityManager.class);
    final TenantContext tenant = mock(TenantContext.class);
    final PeriodoAquisitivoLicencaEspecial pa = new PeriodoAquisitivoLicencaEspecial();
    final Vinculo vinculo = new Vinculo();
    final List<LicencaEspecialGozo> gozos = new ArrayList<>();
    final List<VendaLicencaEspecial> vendas = new ArrayList<>();
    final List<PeriodoAquisitivoLicencaEspecial> periodos = new ArrayList<>();
    final List<ParametroContagemFerias> parametros = new ArrayList<>();
    Fixture() {
      when(tenant.orgao()).thenReturn(1);
      when(em.unwrap(org.hibernate.Session.class)).thenReturn(mock(org.hibernate.Session.class));
      when(em.find(eq(Vinculo.class),any(VinculoId.class),eq(LockModeType.PESSIMISTIC_WRITE))).thenAnswer(x->vinculo);
      var generated = new java.util.concurrent.atomic.AtomicLong(10000L);
      doAnswer(inv -> {
        var e=(AssentamentoTempoLicenca)inv.getArgument(0);if(e.getId()==null)e.setId(generated.getAndIncrement());
        if(e instanceof VendaLicencaEspecial v && !vendas.contains(v))vendas.add(v);
        if(e instanceof PeriodoAquisitivoLicencaEspecial p && !periodos.contains(p))periodos.add(p);
        return null;
      }).when(em).persist(any());
      pa.setSituacao("DISPONIVEL"); pa.setId(20L); pa.setEmpCodigo(1); pa.setNumfunc(1001L); pa.setNumvinc(1); pa.setChave(1);
      pa.setDtini(LocalDate.of(2015, 1, 1)); pa.setDtfim(LocalDate.of(2019, 12, 31));
      pa.setPermGozoLicEspAntFimPe(PoliticaAntecipacaoLicenca.VEDAR);
      pa.setVendLicEsp("S"); pa.setAbonoDirGozoLicEsp("G");
      pa.setNumDiasAbonoLicEsp(1); pa.setDenDiasAbonoLicEsp(2);
      vinculo.setEmpCodigo(1); vinculo.setRegimejur("EST"); vinculo.setTipovinc("EFETIVO");
      when(em.find(PeriodoAquisitivoLicencaEspecial.class, 20L)).thenReturn(pa);
      when(em.find(eq(Vinculo.class), any(VinculoId.class))).thenReturn(vinculo);
      when(em.createQuery(anyString(), any(Class.class))).thenAnswer(inv -> {
        String sql = inv.getArgument(0); Class type = inv.getArgument(1);
        TypedQuery query = mock(TypedQuery.class, RETURNS_SELF);
        var params = new HashMap<String,Object>();
        doAnswer(x -> { params.put(x.getArgument(0), x.getArgument(1)); return query; })
            .when(query).setParameter(anyString(), any());
        if(type == Integer.class) { when(query.getSingleResult()).thenReturn(1); } else if (type == Long.class) {
          when(query.getSingleResult()).thenAnswer(x -> sql.contains("LicencaEspecialGozo")
              ? gozos.stream()
                  .filter(g -> !"CANCELADO".equals(g.getSituacao()))
                  .filter(g -> !sql.contains("x.dtini") || (
                      Objects.equals(g.getEmpCodigo(), params.get("org"))
                      && Objects.equals(g.getNumfunc(), params.get("pessoa"))
                      && Objects.equals(g.getNumvinc(), params.get("vinculo"))
                      && !Objects.equals(g.getId(), params.get("id"))
                      && !g.getDtini().isAfter((LocalDate) params.get("fim"))
                      && !(g.getDtfim() == null ? g.getDtprevfim() : g.getDtfim())
                          .isBefore((LocalDate) params.get("inicio"))))
                  .count()
              : sql.contains("VendaLicencaEspecial") ? (long) vendas.size()
                  : sql.contains("averbacaoOrigemId") ? (long) periodos.size() : 0L);
        } else {
          when(query.getResultList()).thenAnswer(x -> type == LicencaEspecialGozo.class ? gozos.stream().filter(g -> !"CANCELADO".equals(g.getSituacao()) && Objects.equals(g.getPeriodoAquisitivoLicencaId(),params.get("pa"))).toList()
              : type == VendaLicencaEspecial.class ? vendas.stream().filter(v -> (params.containsKey("gozo") ? Objects.equals(v.getLicencaEspecialGozoId(),params.get("gozo")) : !Set.of("ESTORNADA","CANCELADO").contains(v.getSituacao()) && v.getLicencaEspecialGozoId()==null && Objects.equals(v.getPeriodoAquisitivoLicencaId(),params.get("pa")))).toList()
              : type == ParametroContagemFerias.class ? parametros
              : type == PeriodoAquisitivoLicencaEspecial.class ? periodos : List.of());
        }
        return query;
      });
    }
    Object[] service(int kind) {
      BaseCrudRepository repo; Object service;
      switch (kind) {
        case 0 -> { repo = mock(AverbacaoTempoServicoRepository.class); service = new AverbacaoTempoServicoService((AverbacaoTempoServicoRepository) repo, Mappers.getMapper(AverbacaoTempoServicoMapper.class), tenant); }
        case 1 -> { repo = mock(PeriodoAquisitivoLicencaEspecialRepository.class); service = new PeriodoAquisitivoLicencaEspecialService((PeriodoAquisitivoLicencaEspecialRepository) repo, Mappers.getMapper(PeriodoAquisitivoLicencaEspecialMapper.class), tenant); }
        case 2 -> { repo = mock(LicencaEspecialGozoRepository.class); service = new LicencaEspecialGozoService((LicencaEspecialGozoRepository) repo, Mappers.getMapper(LicencaEspecialGozoMapper.class), tenant); }
        default -> { repo = mock(VendaLicencaEspecialRepository.class); service = new VendaLicencaEspecialService((VendaLicencaEspecialRepository) repo, Mappers.getMapper(VendaLicencaEspecialMapper.class), tenant); }
      }
      CanonicalResourceTestContext.wire(service, em);
      var nextId = new java.util.concurrent.atomic.AtomicLong(91L);
      when(repo.save(any())).thenAnswer(inv -> {
        var e = (AssentamentoTempoLicenca) inv.getArgument(0);
        if (e.getId() == null) e.setId(nextId.getAndIncrement());
        if (e instanceof LicencaEspecialGozo g && !gozos.contains(g)) gozos.add(g);
        if (e instanceof VendaLicencaEspecial v && !vendas.contains(v)) vendas.add(v);
        when(repo.findById(e.getId())).thenReturn(Optional.of(e));
        if(e instanceof LicencaEspecialGozo g)when(em.find(LicencaEspecialGozo.class,g.getId())).thenReturn(g);
        if(e instanceof VendaLicencaEspecial v)when(em.find(VendaLicencaEspecial.class,v.getId())).thenReturn(v);
        return e;
      });
      doAnswer(inv -> { Object e = inv.getArgument(0); gozos.remove(e); vendas.remove(e); return null; }).when(repo).delete(any(AssentamentoTempoLicenca.class));
      return new Object[]{service, repo};
    }
  }

  Object create(int kind) {
    Object dto = switch (kind) {
      case 0 -> new CreateAverbacaoTempoServicoDTO(); case 1 -> new CreatePeriodoAquisitivoLicencaEspecialDTO();
      case 2 -> new CreateLicencaEspecialGozoDTO(); default -> new CreateVendaLicencaEspecialDTO();
    };
    var b = new BeanWrapperImpl(dto);
    b.setPropertyValue("descricao", "Assentamento de teste"); b.setPropertyValue("empCodigo", 1);
    b.setPropertyValue("numfunc", 1001L); b.setPropertyValue("numvinc", 1);
    b.setPropertyValue("dtini", LocalDate.of(2026, 1, 1));
    if (kind < 2) b.setPropertyValue("chave", 1);
    if (kind == 0) {
      b.setPropertyValue("dtfim", LocalDate.of(2026, 12, 31));
      b.setPropertyValue("complemento", "01012026-31122026");
      b.setPropertyValue("totalDias", 365);
      b.setPropertyValue("diasFinalidade", 90);
      b.setPropertyValue("tipoTempo", "PUBLICO");
      b.setPropertyValue("finalidade", "LICENCA_ESPECIAL");
    }
    if (kind == 1) {
      b.setPropertyValue("dtfim", LocalDate.of(2030, 12, 31));
      b.setPropertyValue("anosLicEsp", 5);
    }
    if (kind >= 2) b.setPropertyValue("periodoAquisitivoLicencaId", 20L);
    if (kind == 2) { b.setPropertyValue("ordemParcela", 1); b.setPropertyValue("diasGozo", 10); b.setPropertyValue("dtprevfim", LocalDate.of(2026, 1, 10)); }
    if (kind == 3) b.setPropertyValue("diasVendidos", 10);
    return dto;
  }
  Object update(int kind, Object create) {
    Object dto = switch (kind) {
      case 0 -> new UpdateAverbacaoTempoServicoDTO(); case 1 -> new UpdatePeriodoAquisitivoLicencaEspecialDTO();
      case 2 -> new UpdateLicencaEspecialGozoDTO(); default -> new UpdateVendaLicencaEspecialDTO();
    };
    org.springframework.beans.BeanUtils.copyProperties(create, dto);
    new BeanWrapperImpl(dto).setPropertyValue("descricao", "Assentamento revisado"); return dto;
  }
  TempoLicencaTransicaoDTO command() {var d=new TempoLicencaTransicaoDTO();d.setDataEfeito(LocalDate.of(2026,1,5));d.setJustificativa("Ato administrativo de teste");return d;}
  ResourceVersionUpdatePrecondition<Long> precondition(long version) {
    var etags = new ResourceVersionEtagService("synthetic-test-secret");
    return new ResourceVersionUpdatePrecondition<>(etags, etags.create("test.resource", 91L, version), "test.resource", 91L);
  }

  @Test void postHidrataDiasEChaveEProjetaCronologiaGovernada() throws Exception {
    var fixture = new Fixture();
    var service = (AverbacaoTempoServicoService) fixture.service(0)[0];
    var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders
        .standaloneSetup(new AverbacaoTempoServicoController(service))
        .setControllerAdvice(new org.praxisplatform.uischema.rest.exceptionhandler.GlobalExceptionHandler())
        .build();
    String payload = """
        {"empCodigo":1,"numfunc":1001,"numvinc":1,"descricao":"Certidão de tempo",
         "tipoTempo":"PUBLICO","finalidade":"LICENCA_ESPECIAL","complemento":"01012026-05012026",
         "dtini":"2026-01-01","dtfim":"%s"}
        """;
    mvc.perform(post(ApiPaths.AVERBACOES_TEMPO_SERVICO).contentType("application/json")
            .content(payload.formatted("2026-01-05")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.chave").value(2))
        .andExpect(jsonPath("$.data.totalDias").value(5))
        .andExpect(jsonPath("$.data.diasFinalidade").value(5));
    mvc.perform(post(ApiPaths.AVERBACOES_TEMPO_SERVICO).contentType("application/json")
            .content(payload.formatted("2025-12-31")))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.errors[0].violations[0].code").value("ERG-00006"))
        .andExpect(jsonPath("$.errors[0].violations[0].slotKey").isNotEmpty())
        .andExpect(jsonPath("$.errors[0].violations[0].targetField").value("dtfim"))
        .andExpect(jsonPath("$.errors[0].violations[0].lineage").isNotEmpty());
  }

  @Test void postDeGozoAceitaDiasOuTerminoPrevisto() throws Exception {
    for (String periodo : List.of("\"diasGozo\":10", "\"dtprevfim\":\"2026-01-10\"")) {
      var service = (LicencaEspecialGozoService) new Fixture().service(2)[0];
      var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders
          .standaloneSetup(new LicencaEspecialGozoController(service))
          .setControllerAdvice(new org.praxisplatform.uischema.rest.exceptionhandler.GlobalExceptionHandler())
          .build();
      mvc.perform(post(ApiPaths.LICENCAS_ESPECIAIS_GOZO).contentType("application/json")
              .content("""
                  {"periodoAquisitivoLicencaId":20,"descricao":"Licença programada",
                   "ordemParcela":1,"dtini":"2026-01-01",%s}
                  """.formatted(periodo)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.data.diasGozo").value(10))
          .andExpect(jsonPath("$.data.chavePeraqlicesp").value(1));
    }
  }

  @Test void fourServicesExecuteCanonicalCreateVersionedUpdateAndDelete() {
    for (int kind = 0; kind < 4; kind++) {
      var f = new Fixture(); var parts = f.service(kind);
      BaseResourceService service = (BaseResourceService) parts[0]; var repo = (BaseCrudRepository) parts[1];
      Object dto = create(kind);
      var created = service.create(dto); assertThat(created.id()).isEqualTo(91L);
      var changed = update(kind, dto);
      assertThatThrownBy(() -> service.update(91L, changed)).isInstanceOfSatisfying(ResourceVersionPreconditionException.class, e -> assertThat(e.status().value()).isEqualTo(428));
      var versioned = (VersionedCreateUpdateResourceService) service;
      assertThatThrownBy(() -> versioned.update(91L, changed, null)).isInstanceOfSatisfying(ResourceVersionPreconditionException.class, e -> assertThat(e.status().value()).isEqualTo(428));
      clearInvocations(repo);
      assertThatThrownBy(() -> versioned.update(91L, changed, precondition(7))).isInstanceOfSatisfying(ResourceVersionPreconditionException.class, e -> assertThat(e.status().value()).isEqualTo(412));
      verify(repo, never()).save(any());
      var result = versioned.update(91L, changed, precondition(0));
      assertThat(new BeanWrapperImpl(result.body()).getPropertyValue("descricao")).isEqualTo("Assentamento revisado");
      assertThat(result.persistedVersion()).isPresent();
      service.deleteById(91L); verify(repo).delete(any(AssentamentoTempoLicenca.class));
      if (kind >= 2) assertThat(f.pa.getSaldoDias()).isEqualTo(90);
    }
  }

  private void rejected(int kind, java.util.function.Consumer<Fixture> setup, java.util.function.Consumer<BeanWrapperImpl> change, String message) {
    var f = new Fixture(); setup.accept(f); var service = (BaseResourceService) f.service(kind)[0]; var dto = create(kind); change.accept(new BeanWrapperImpl(dto));
    assertThatThrownBy(() -> service.create(dto)).as("kind=" + kind + " parentSaldo=" + f.pa.getSaldoDias()) .isInstanceOfSatisfying(br.com.praxis.ergonshowcase.rules.ErgonRuleViolationException.class, rejection -> {
      assertThat(rejection.getHttpStatus()).isEqualTo(422);
      assertThat(rejection.getReasonCodes()).contains(message);
    });
  }
  @Test void parentEligibilitySaldoAndAbonoAreEnforcedBeforeSave() {
    for (int kind : List.of(2, 3)) {
      rejected(kind, f -> f.pa.setSituacao("CANCELADO"), b -> {}, kind == 2 ? "CERG-06303" : "CERG-06407");
      rejected(kind, f -> f.pa.setSaldoDias(0), b -> {}, kind == 2 ? "ERG-00705" : "ERG-03384");
    }
    rejected(3, f -> f.pa.setVendLicEsp("N"), b -> {}, "ERG-03386");
    rejected(3, f -> f.pa.setMinDiasPvendLicEsp(15), b -> {}, "ERG-03383");
    rejected(3, f -> f.pa.setMaxDiasVendLicEsp(5), b -> {}, "ERG-03382");
    rejected(2, f -> f.pa.setDtfim(LocalDate.of(2029, 1, 1)), b -> {}, "ERG-00701");
    rejected(2, f -> {}, b -> b.setPropertyValue("diasAbono", 1), "CERG-06308");
    rejected(2, f -> {}, b -> b.setPropertyValue("iniFimAbono", "F"), "CERG-06308");
    rejected(2, f -> {}, b -> b.setPropertyValue("diasGozo", 9), "CERG-06302");
    rejected(2, f -> { f.pa.setDtfim(LocalDate.of(2029, 1, 1)); f.pa.setPermGozoLicEspAntFimPe(PoliticaAntecipacaoLicenca.HERDAR);
      var p = new ParametroContagemFerias(); p.setEmpCodigo(1); p.setRegimeJuridico("EST"); p.setTipoVinculo("EFETIVO"); p.setDtini(LocalDate.of(2020, 1, 1)); p.setPermGozoLicEspAntFimPe(false); f.parametros.add(p);
    }, b -> {}, "ERG-00701");
  }

  @Test void consumptionIsRecomputedAcrossCreateUpdateCancelAndDelete() {
    var f = new Fixture(); var gs = (LicencaEspecialGozoService) f.service(2)[0]; var vs = (VendaLicencaEspecialService) f.service(3)[0];
    var g = (CreateLicencaEspecialGozoDTO) create(2); g.setAbonoPecuniario(true); g.setDiasAbono(5); g.setIniFimAbono("F");
    gs.create(g); assertThat(f.pa.getDiasGozados()).isEqualTo(10); assertThat(f.pa.getDiasVendidos()).isEqualTo(5); assertThat(f.pa.getSaldoDias()).isEqualTo(75);
    var v = (CreateVendaLicencaEspecialDTO) create(3); vs.create(v); assertThat(f.pa.getSaldoDias()).isEqualTo(65);
    var u = (UpdateVendaLicencaEspecialDTO) update(3, v); u.setDiasVendidos(20);
    vs.update(91L, u, precondition(0)); assertThat(f.pa.getSaldoDias()).isEqualTo(55);
    vs.transition(91L,"estornar-venda-licenca-especial",command(),precondition(0)); assertThat(f.pa.getSaldoDias()).isEqualTo(75);
    gs.deleteAllById(List.of(91L)); assertThat(f.pa.getSaldoDias()).isEqualTo(90);
  }

  @Test void updatingGozoReleasesItsOldConsumptionAndSecondSaleCannotOverdraw() {
    var f = new Fixture(); var gs = (LicencaEspecialGozoService) f.service(2)[0];
    var g = (CreateLicencaEspecialGozoDTO) create(2); gs.create(g);
    var u = (UpdateLicencaEspecialGozoDTO) update(2, g); u.setDiasGozo(20); u.setDtprevfim(LocalDate.of(2026, 1, 20));
    gs.update(91L, u, precondition(0)); assertThat(f.pa.getSaldoDias()).isEqualTo(70);
    var vs = (VendaLicencaEspecialService) f.service(3)[0]; var v = (CreateVendaLicencaEspecialDTO) create(3);
    f.pa.setNumDiasAbonoLicEsp(1); f.pa.setDenDiasAbonoLicEsp(1);
    v.setDiasVendidos(65); vs.create(v); assertThat(f.pa.getSaldoDias()).isEqualTo(5);
    v.setDiasVendidos(10);
    assertThatThrownBy(() -> vs.create(v)).hasMessageContaining("saldo");
    gs.transition(91L,"cancelar-licenca-especial",command(),precondition(0)); assertThat(f.pa.getSaldoDias()).isEqualTo(25);
  }

  @Test void explicitAndInheritedPermissionPermitEarlyGozoAndPeriodTotalsAreComputed() {
    for (var policy : List.of(PoliticaAntecipacaoLicenca.PERMITIR, PoliticaAntecipacaoLicenca.HERDAR)) {
      var f = new Fixture(); f.pa.setDtfim(LocalDate.of(2029, 1, 1)); f.pa.setPermGozoLicEspAntFimPe(policy);
      var p = new ParametroContagemFerias(); p.setEmpCodigo(1); p.setRegimeJuridico("EST"); p.setTipoVinculo("EFETIVO");
      p.setDtini(LocalDate.of(2020, 1, 1)); p.setPermGozoLicEspAntFimPe(true); f.parametros.add(p);
      var gs = (LicencaEspecialGozoService) f.service(2)[0]; gs.create((CreateLicencaEspecialGozoDTO) create(2));
      assertThat(f.pa.getSaldoDias()).isEqualTo(80);
    }
    var f = new Fixture(); var ps = (PeriodoAquisitivoLicencaEspecialService) f.service(1)[0];
    var dto = (CreatePeriodoAquisitivoLicencaEspecialDTO) create(1); dto.setDiasDireito(30);
    assertThat(ps.create(dto).body().getSaldoDias()).isEqualTo(30);
  }

  @Test void parentsWithLinkedRecordsCannotBeDeleted() {
    var f = new Fixture(); var parts = f.service(1); var repo = (BaseCrudRepository) parts[1];
    when(repo.findById(20L)).thenReturn(Optional.of(f.pa)); f.gozos.add(new LicencaEspecialGozo());
    BaseResourceService ps = (BaseResourceService) parts[0];
    assertThatThrownBy(() -> ps.deleteById(20L)).hasMessageContaining("409");
    verify(repo, never()).delete(any(AssentamentoTempoLicenca.class));
    var a = new AverbacaoTempoServico(); a.setEmpCodigo(1); a.setId(30L); f.periodos.add(f.pa);
    parts = f.service(0); var ar = (BaseCrudRepository) parts[1]; when(ar.findById(30L)).thenReturn(Optional.of(a));
    BaseResourceService as = (BaseResourceService) parts[0];
    assertThatThrownBy(() -> as.deleteById(30L)).hasMessageContaining("409");
  }
}
