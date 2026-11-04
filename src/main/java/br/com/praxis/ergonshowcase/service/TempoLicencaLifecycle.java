package br.com.praxis.ergonshowcase.service;

import br.com.praxis.ergonshowcase.config.TenantContext;
import br.com.praxis.ergonshowcase.domain.*;
import br.com.praxis.ergonshowcase.dto.TempoLicencaTransicaoDTO;
import br.com.praxis.ergonshowcase.rules.ErgonRuleEngineFacade;
import jakarta.persistence.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.praxisplatform.rules.facts.PraxisFactTreeBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Transições únicas para execução individual e lote. Não executa folha ou packages Oracle. */
public final class TempoLicencaLifecycle {
  private TempoLicencaLifecycle() {}

  public static Set<String> states(Class<?> type) {
    if (AverbacaoTempoServico.class.isAssignableFrom(type)) return Set.of("EM_ANALISE", "HOMOLOGADA", "PARCIALMENTE_DESAVERBADA", "DESAVERBADA_TOTAL", "CANCELADO");
    if (PeriodoAquisitivoLicencaEspecial.class.isAssignableFrom(type)) return Set.of("EM_AQUISICAO", "DISPONIVEL", "PARCIALMENTE_GOZADO", "CONVERTIDO_EM_DOBRO", "QUITADO", "SUSPENSO", "PRESCRITO", "CANCELADO");
    if (LicencaEspecialGozo.class.isAssignableFrom(type)) return Set.of("PROGRAMADA", "HOMOLOGADA", "EM_GOZO", "GOZADA", "INTERROMPIDA", "CANCELADO");
    if (VendaLicencaEspecial.class.isAssignableFrom(type)) return Set.of("SOLICITADA", "AUTORIZADA", "PAGA", "ESTORNADA");
    return Set.of();
  }
  public static String initial(Class<?> type) {
    if (AverbacaoTempoServico.class.isAssignableFrom(type)) return "EM_ANALISE";
    if (PeriodoAquisitivoLicencaEspecial.class.isAssignableFrom(type)) return "EM_AQUISICAO";
    if (LicencaEspecialGozo.class.isAssignableFrom(type)) return "PROGRAMADA";
    if (VendaLicencaEspecial.class.isAssignableFrom(type)) return "SOLICITADA";
    throw new IllegalArgumentException("Tipo de assentamento desconhecido.");
  }
  public static void requireState(AssentamentoTempoLicenca e, String... allowed) {
    if (!Set.of(allowed).contains(e.getSituacao())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Situação incompatível com este ato.");
  }
  private static PraxisFactTreeBuilder fatosDoAto(String action, TempoLicencaTransicaoDTO dto) {
    return PraxisFactTreeBuilder.create()
        .putDraft(draft -> draft
            .put("acao", action)
            .put("diasAto", dto.getDias())
            .put("dataEfeito", TempoLicencaRuleFacts.day(dto.getDataEfeito())))
        .putCurrent(c -> {})
        .putConcept("vinculo", v -> {})
        .putConcept("politica", p -> {})
        .putConcept("periodoAquisitivo", pa -> {})
        .putConcept("venda", v -> {})
        .putNeighborhood(n -> {});
  }
  public static void transition(AssentamentoTempoLicenca e, String action, TempoLicencaTransicaoDTO dto, TenantContext tenant, EntityManager em, ErgonRuleEngineFacade rules) {
    TempoLicencaValidation.orgao(tenant, e.getEmpCodigo());
    if (dto == null || dto.getDataEfeito() == null || dto.getJustificativa() == null || dto.getJustificativa().isBlank())
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data e justificativa do ato são obrigatórias.");
    if (e instanceof AverbacaoTempoServico a) averbacao(a, action, dto, em, rules);
    else if (e instanceof PeriodoAquisitivoLicencaEspecial pa) periodo(pa, action, dto, em, rules);
    else if (e instanceof LicencaEspecialGozo g) gozo(g, action, dto, tenant, em, rules);
    else if (e instanceof VendaLicencaEspecial v) venda(v, action, dto, rules);
    else throw new IllegalArgumentException("Tipo de assentamento desconhecido.");
    e.setObservacoes(dto.getJustificativa());
  }
  private static void averbacao(AverbacaoTempoServico a, String action, TempoLicencaTransicaoDTO d, EntityManager em, ErgonRuleEngineFacade rules) {
    switch (action) {
      case "homologar-averbacao" -> {
        requireState(a,"EM_ANALISE");
        validarAtoAverbacao(a, action, d, em, rules);
        a.setSituacao("HOMOLOGADA");
      }
      case "registrar-desaverbacao" -> {
        requireState(a,"HOMOLOGADA","PARCIALMENTE_DESAVERBADA");
        validarAtoAverbacao(a, action, d, em, rules);
        int total = Math.addExact(a.getDiasDesaverbados(), d.getDias());
        a.setDiasDesaverbados(total); a.setDataDesaverbacao(d.getDataEfeito()); a.setMotivoDesaverbacao(d.getJustificativa());
        a.setSituacao(total == a.getDiasFinalidade() ? "DESAVERBADA_TOTAL" : "PARCIALMENTE_DESAVERBADA");
      }
      case "reverter-desaverbacao" -> {
        requireState(a,"PARCIALMENTE_DESAVERBADA","DESAVERBADA_TOTAL");
        validarAtoAverbacao(a, action, d, em, rules);
        int rest = a.getDiasDesaverbados() - d.getDias();
        a.setDiasDesaverbados(rest); a.setDataDesaverbacao(rest == 0 ? null : d.getDataEfeito());
        a.setMotivoDesaverbacao(rest == 0 ? null : d.getJustificativa());
        a.setSituacao(rest == 0 ? "HOMOLOGADA" : "PARCIALMENTE_DESAVERBADA");
      }
      case "cancelar-averbacao" -> {
        requireState(a,"EM_ANALISE","HOMOLOGADA");
        validarAtoAverbacao(a, action, d, em, rules);
        a.setDataRemocao(d.getDataEfeito()); a.setMotivoRemocao(d.getJustificativa()); a.setSituacao("CANCELADO");
      }
      default -> throw new IllegalArgumentException("Ato não pertence à averbação.");
    }
  }
  private static void validarAtoAverbacao(AverbacaoTempoServico a, String action,
      TempoLicencaTransicaoDTO dto, EntityManager em, ErgonRuleEngineFacade rules) {
    long consumo = TempoLicencaValidation.consumoPeriodosGerados(a, em);
    rules.enforceAverbacaoTempoServico(a.getEmpCodigo(), fatosDoAto(action, dto)
        .putDraft(draft -> draft
            .put("diasDesaverbados", a.getDiasDesaverbados())
            .put("diasFinalidade", a.getDiasFinalidade()))
        .putNeighborhood(n -> n
            .put("consumoPeriodosGerados", consumo))
        .build());
  }

  private static void periodo(PeriodoAquisitivoLicencaEspecial p, String action, TempoLicencaTransicaoDTO d, EntityManager em, ErgonRuleEngineFacade rules) {
    TempoLicencaValidation.recalcularPeriodo(p,em, rules);
    switch(action) {
      case "homologar-periodo-licenca-especial" -> {
        requireState(p,"EM_AQUISICAO","SUSPENSO");
        validarAtoPeriodo(p, action, d, em, rules);
        p.setSituacao("DISPONIVEL");
      }
      case "suspender-periodo-licenca-especial" -> { requireState(p,"EM_AQUISICAO","DISPONIVEL","PARCIALMENTE_GOZADO"); p.setSituacao("SUSPENSO"); }
      case "cancelar-periodo-licenca-especial" -> {
        requireState(p,"EM_AQUISICAO","DISPONIVEL","SUSPENSO");
        validarAtoPeriodo(p, action, d, em, rules);
        p.setSituacao("CANCELADO");
      }
      case "converter-em-dobro" -> {
        requireState(p,"DISPONIVEL","PARCIALMENTE_GOZADO");
        validarAtoPeriodo(p, action, d, em, rules);
        int dias = d.getDias();
        p.setDiasConvertidosDobro(Math.addExact(p.getDiasConvertidosDobro(),dias)); p.setDtCntDobro(d.getDataEfeito()); p.setSituacao("CONVERTIDO_EM_DOBRO");
      }
      case "reverter-conversao-dobro" -> {
        requireState(p,"CONVERTIDO_EM_DOBRO","PARCIALMENTE_GOZADO","QUITADO");
        validarAtoPeriodo(p, action, d, em, rules);
        int rest = p.getDiasConvertidosDobro() - d.getDias();
        p.setDiasConvertidosDobro(rest); if(rest==0)p.setDtCntDobro(null); p.setSituacao("DISPONIVEL");
      }
      case "prescrever-periodo-licenca-especial" -> {
        requireState(p,"DISPONIVEL","PARCIALMENTE_GOZADO");
        validarAtoPeriodo(p, action, d, em, rules);
        p.setSituacao("PRESCRITO");
      }
      case "recalcular-saldo-licenca-especial" -> requireState(p,"EM_AQUISICAO","DISPONIVEL","PARCIALMENTE_GOZADO","CONVERTIDO_EM_DOBRO","QUITADO","SUSPENSO");
      default -> throw new IllegalArgumentException("Ato não pertence ao período.");
    }
  }

  private static void validarAtoPeriodo(PeriodoAquisitivoLicencaEspecial pa, String action,
      TempoLicencaTransicaoDTO dto, EntityManager em, ErgonRuleEngineFacade rules) {
    var builder = fatosDoAto(action, dto)
        .putDraft(draft -> draft
            .put("fim", TempoLicencaRuleFacts.day(pa.getDtfim()))
            .put("prescricao", TempoLicencaRuleFacts.day(pa.getDtPrescricao()))
            .put("diasConvertidosDobro", pa.getDiasConvertidosDobro())
            .put("saldoDisponivel", pa.getSaldoDias()));
    if ("converter-em-dobro".equals(action)) {
      var policy = EffectiveSpecialLeavePolicy.resolve(pa, null, em);
      builder.putConcept("politica", pol -> pol
          .put("permiteDobro", policy.permiteDobro())
          .put("minimoDobro", policy.minimoDobro()));
    }
    rules.enforcePeriodoAquisitivoLicencaEspecial(pa.getEmpCodigo(), builder.build());
  }

  private static void gozo(LicencaEspecialGozo g,String action,TempoLicencaTransicaoDTO d, TenantContext tenant, EntityManager em, ErgonRuleEngineFacade rules) {
    switch(action) {
      case "homologar-licenca-especial" -> { requireState(g,"PROGRAMADA");g.setSituacao("HOMOLOGADA"); }
      case "iniciar-gozo-licenca-especial" -> { requireState(g,"HOMOLOGADA"); validarAtoGozo(g, action, d, em, rules); g.setSituacao("EM_GOZO"); }
      case "encerrar-licenca-especial", "interromper-licenca-especial" -> {
        requireState(g,"EM_GOZO");
        validarAtoGozo(g, action, d, em, rules);
        g.setDtfim(d.getDataEfeito());g.setDiasGozo(Math.toIntExact(ChronoUnit.DAYS.between(g.getDtini(),d.getDataEfeito())+1));
        TempoLicencaValidation.recalcularAbonoEncerramento(g,tenant,em,rules);
        g.setSituacao(action.equals("encerrar-licenca-especial") ? "GOZADA" : "INTERROMPIDA");
      }
      case "cancelar-licenca-especial" -> {
        requireState(g,"PROGRAMADA","HOMOLOGADA");
        validarAtoGozo(g, action, d, em, rules);
        g.setSituacao("CANCELADO");
      }
      default -> throw new IllegalArgumentException("Ato não pertence ao gozo.");
    }
  }
  private static void validarAtoGozo(LicencaEspecialGozo g, String action,
      TempoLicencaTransicaoDTO dto, EntityManager em, ErgonRuleEngineFacade rules) {
    long substitutos = "cancelar-licenca-especial".equals(action) && g.getNumfuncSubstituto() != null
        ? em.createQuery("select count(v) from Vinculo v where v.id.numfunc=:pessoa and v.empCodigo=:org and v.situacao='ATIVO'", Long.class)
            .setParameter("pessoa", g.getNumfuncSubstituto())
            .setParameter("org", g.getEmpCodigo()).getSingleResult() : 0;
    rules.enforceLicencaEspecialGozo(g.getEmpCodigo(), fatosDoAto(action, dto)
        .putDraft(draft -> draft
            .put("inicio", TempoLicencaRuleFacts.day(g.getDtini()))
            .put("prevFim", TempoLicencaRuleFacts.day(g.getDtprevfim())))
        .putNeighborhood(n -> n
            .put("vinculosSubstituto", substitutos))
        .build());
  }

  private static void venda(VendaLicencaEspecial v,String action,TempoLicencaTransicaoDTO d, ErgonRuleEngineFacade rules) {
    switch(action) {
      case "autorizar-venda-licenca-especial" -> {
        requireState(v,"SOLICITADA");
        if(v.getDtPagamento()!=null) throw new ResponseStatusException(HttpStatus.CONFLICT, "Pagamento histórico já registrado.");
        v.setSituacao("AUTORIZADA");
      }
      case "pagar-venda-licenca-especial" -> {
        requireState(v,"AUTORIZADA");
        if(v.getDtPagamento()!=null) throw new ResponseStatusException(HttpStatus.CONFLICT, "Pagamento histórico já registrado.");
        validarAtoVenda(v, action, d, rules);
        v.setDtPagamento(d.getDataEfeito());v.setSituacao("PAGA");
      }
      case "estornar-venda-licenca-especial" -> {
        requireState(v,"SOLICITADA","AUTORIZADA","PAGA");
        validarAtoVenda(v, action, d, rules);
        v.setDtEstorno(d.getDataEfeito());v.setSituacao("ESTORNADA");
      }
      default -> throw new IllegalArgumentException("Ato não pertence à venda.");
    }
  }
  private static void validarAtoVenda(VendaLicencaEspecial v, String action,
      TempoLicencaTransicaoDTO dto, ErgonRuleEngineFacade rules) {
    rules.enforceVendaLicencaEspecial(v.getEmpCodigo(), fatosDoAto(action, dto)
        .putDraft(draft -> draft
            .put("pagamento", TempoLicencaRuleFacts.day(v.getDtPagamento()))
            .put("valorIndenizacao", v.getValorIndenizacao())
            .put("mesAnoFolha", TempoLicencaRuleFacts.day(v.getMesAnoFolha()))
            .put("numFolha", v.getNumFolha())
            .put("licencaEspecialGozoId", v.getLicencaEspecialGozoId())
            .put("atoPublicacaoId", v.getAtoPublicacaoId()))
        .build());
  }
}
