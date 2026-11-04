package br.com.praxis.ergonshowcase.service;


import br.com.praxis.ergonshowcase.config.TenantContext;
import br.com.praxis.ergonshowcase.domain.*;
import br.com.praxis.ergonshowcase.rules.ErgonRuleEngineFacade;
import br.com.praxis.ergonshowcase.rules.OperationalRuleSet;
import br.com.praxis.ergonshowcase.rules.EffectiveVacationParameterResolver;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.FlushModeType;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Integridade cadastral e contabilidade serializada por período aquisitivo. */
final class TempoLicencaValidation {
  private TempoLicencaValidation() {}

  static void orgao(TenantContext tenant, Integer orgao) {
    if (orgao == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Órgão do assentamento é obrigatório.");
    if (tenant.orgao() != orgao)
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Assentamento fora do órgão da requisição.");
  }

  /** Mesmo contexto em GET by-ids, POST by-ids e filter; payload não amplia o órgão. */
  static <F extends br.com.praxis.ergonshowcase.dto.TempoLicencaFilterDTO> F scopeFilter(
      F filter, TenantContext tenant) {
    if (filter.getEmpCodigo() != null) orgao(tenant, filter.getEmpCodigo());
    filter.setEmpCodigo(tenant.orgao());
    return filter;
  }

  static Vinculo cadastro(AssentamentoTempoLicenca entity, TenantContext tenant, EntityManager em) {
    orgao(tenant, entity.getEmpCodigo());
    if (entity.getNumfunc() == null || entity.getNumvinc() == null)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pessoa e vínculo são obrigatórios.");
    if (entity.getDescricao() == null || entity.getDescricao().isBlank())
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identifique o assentamento.");
    if (!TempoLicencaLifecycle.states(entity.getClass()).contains(entity.getSituacao()))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Situação cadastral inválida.");
    Vinculo vinculo = em.find(Vinculo.class, new VinculoId(entity.getNumfunc(), entity.getNumvinc()));
    if (vinculo == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Vínculo inexistente.");
    ErgonTenantAccess.requireReferencesInOrganization(entity.getEmpCodigo(), vinculo);
    documento(entity.getDocumentoLegalId(), entity.getEmpCodigo(), em);
    documento(entity.getAtoPublicacaoId(), entity.getEmpCodigo(), em);
    return vinculo;
  }

  private static void documento(Long id, Integer orgao, EntityManager em) {
    if (id == null) return;
    var documento = em.find(DocumentoLegal.class, id);
    if (documento == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento legal inexistente.");
    ErgonTenantAccess.requireReferencesInOrganization(orgao, documento);
  }

  static void hydrate(ParcelaLicencaEspecial entity, TenantContext tenant, EntityManager em) {
    if (entity.getPeriodoAquisitivoLicencaId() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione o período aquisitivo.");
    var parent = em.find(PeriodoAquisitivoLicencaEspecial.class, entity.getPeriodoAquisitivoLicencaId());
    if (parent == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Período aquisitivo não encontrado.");
    orgao(tenant, parent.getEmpCodigo());
    if (entity.getEmpCodigo() != null) orgao(tenant, entity.getEmpCodigo());
    compatible(entity.getNumfunc(), parent.getNumfunc(), "Pessoa");
    compatible(entity.getNumvinc(), parent.getNumvinc(), "Vínculo");
    compatible(entity.getChavePeraqlicesp(), parent.getChave(), "Chave do período");
    entity.setEmpCodigo(parent.getEmpCodigo());
    entity.setNumfunc(parent.getNumfunc());
    entity.setNumvinc(parent.getNumvinc());
    entity.setChavePeraqlicesp(parent.getChave());
  }

  private static void compatible(Object supplied, Object expected, String label) {
    if (supplied != null && !Objects.equals(supplied, expected))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " diverge do período aquisitivo selecionado.");
  }

  static PeriodoAquisitivoLicencaEspecial parent(Long id, TenantContext tenant, EntityManager em) {
    var pa = em.find(PeriodoAquisitivoLicencaEspecial.class, id);
    if (pa == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Período aquisitivo não encontrado.");
    orgao(tenant, pa.getEmpCodigo());
    em.lock(pa, LockModeType.PESSIMISTIC_WRITE);
    em.refresh(pa);
    orgao(tenant, pa.getEmpCodigo());
    return pa;
  }

  private static List<LicencaEspecialGozo> gozos(Long id, Integer org, EntityManager em) {
    return em.createQuery("select g from LicencaEspecialGozo g where g.empCodigo=:org and g.periodoAquisitivoLicencaId=:pa and g.situacao in ('PROGRAMADA','HOMOLOGADA','EM_GOZO','GOZADA','INTERROMPIDA')", LicencaEspecialGozo.class)
        .setParameter("org", org).setParameter("pa", id).setFlushMode(FlushModeType.COMMIT).getResultList();
  }

  private static List<VendaLicencaEspecial> vendas(Long id, Integer org, EntityManager em) {
    return em.createQuery("select v from VendaLicencaEspecial v where v.empCodigo=:org and v.periodoAquisitivoLicencaId=:pa and v.situacao in ('SOLICITADA','AUTORIZADA','PAGA') and v.licencaEspecialGozoId is null", VendaLicencaEspecial.class)
        .setParameter("org", org).setParameter("pa", id).setFlushMode(FlushModeType.COMMIT).getResultList();
  }

  static void validarConsumo(ParcelaLicencaEspecial entity, Vinculo vinculo, TenantContext tenant,
      EntityManager em, ErgonRuleEngineFacade rules) {
    var pa = parent(entity.getPeriodoAquisitivoLicencaId(), tenant, em);
    // Estorno não concede novo direito. A política atual não pode impedir a liberação do saldo.
    if ("CANCELADO".equals(entity.getSituacao()) || "ESTORNADA".equals(entity.getSituacao())) return;
    var gs = gozos(pa.getId(), pa.getEmpCodigo(), em);
    var vs = vendas(pa.getId(), pa.getEmpCodigo(), em);
    var outrosGozos = gs.stream().filter(g -> !(entity instanceof LicencaEspecialGozo) || !Objects.equals(g.getId(), entity.getId())).toList();
    long gozo = outrosGozos.stream().mapToLong(LicencaEspecialGozo::getDiasGozo).sum();
    long abono = outrosGozos.stream().mapToLong(LicencaEspecialGozo::getDiasAbono).sum();
    long vendido = vs.stream().filter(v -> !(entity instanceof VendaLicencaEspecial) || !Objects.equals(v.getId(), entity.getId()))
        .mapToLong(VendaLicencaEspecial::getDiasVendidos).sum();
    long anterior = entity instanceof LicencaEspecialGozo
        ? gs.stream().filter(g -> Objects.equals(g.getId(), entity.getId())).mapToLong(g -> g.getDiasGozo().longValue() + g.getDiasAbono()).sum()
        : vs.stream().filter(v -> Objects.equals(v.getId(), entity.getId())).mapToLong(VendaLicencaEspecial::getDiasVendidos).sum();
    var politica = EffectiveSpecialLeavePolicy.resolve(pa, entity, em);
    long consumo;
    long pecunia;
    var builder = TempoLicencaRuleFacts.common(entity, vinculo)
        .putConcept("periodoAquisitivo", paNode -> paNode
            .put("situacao", pa.getSituacao())
            .put("fim", TempoLicencaRuleFacts.day(pa.getDtfim()))
            .put("prescricao", TempoLicencaRuleFacts.day(pa.getDtPrescricao()))
            .put("origem", pa.getAverbacaoOrigemId())
            .put("diasDireito", pa.getDiasDireito())
            .put("saldoDisponivel", pa.getSaldoDias() == null ? null : pa.getSaldoDias().longValue() + anterior))
        .putConcept("politica", polNode -> polNode
            .put("permiteVenda", politica.permiteVenda())
            .put("baseAbono", politica.baseAbono())
            .put("numeradorAbono", politica.numeradorAbono())
            .put("denominadorAbono", politica.denominadorAbono())
            .put("minimoVenda", politica.minimoVenda())
            .put("maximoVenda", politica.maximoVenda())
            .put("permiteAntecipacao", politica.permiteAntecipacao()));
    if (entity instanceof LicencaEspecialGozo g) {
      var fim = g.getDtfim() == null ? g.getDtprevfim() : g.getDtfim();
      var matriz = new EffectiveVacationParameterResolver().resolveActiveParameter(em, vinculo, g.getDtini());
      consumo = zero(g.getDiasGozo()) + (long) zero(g.getDiasAbono());
      pecunia = zero(g.getDiasAbono());
      long vinculosSubstituto = g.getNumfuncSubstituto() == null ? 0 :
          em.createQuery("select count(v) from Vinculo v where v.id.numfunc=:pessoa and v.empCodigo=:org and v.situacao='ATIVO'", Long.class)
              .setParameter("pessoa", g.getNumfuncSubstituto())
              .setParameter("org", g.getEmpCodigo()).getSingleResult();
      long sobreposicoes = TempoLicencaRuleFacts.sobreposicoesGozo(g, fim, vinculo, em, rules);
      builder.putDraft(draft -> draft
              .put("prevFim", TempoLicencaRuleFacts.day(g.getDtprevfim()))
              .put("fimEfetivo", TempoLicencaRuleFacts.day(fim))
              .put("diasCorridos", TempoLicencaRuleFacts.inclusive(g.getDtini(), fim))
              .put("diasGozo", g.getDiasGozo())
              .put("diasAbono", g.getDiasAbono())
              .put("iniFimAbono", g.getIniFimAbono())
              .put("abono", g.getAbonoPecuniario())
              .put("baseDiasAbono", "D".equals(politica.baseAbono()) ? pa.getDiasDireito() : g.getDiasGozo())
              .put("substituto", g.getNumfuncSubstituto())
              .put("consumo", consumo))
          .putConcept("politica", polNode -> polNode
              .put("permiteAposPrescricao", matriz == null ? null : matriz.getPermiteGozoAposPrescricao()))
          .putConcept("venda", vNode -> {})
          .putNeighborhood(n -> n
              .put("outrosAbonos", outrosGozos.stream().filter(x -> x.getDiasAbono() > 0).count())
              .put("maiorOrdemOuQuantidade", Math.max(zero(g.getOrdemParcela()), outrosGozos.size() + 1))
              .put("vinculosSubstituto", vinculosSubstituto)
              .put("sobreposicoes", sobreposicoes));
    } else {
      var v = (VendaLicencaEspecial) entity;
      consumo = zero(v.getDiasVendidos());
      pecunia = consumo;
      builder.putDraft(draft -> draft
          .put("diasVendidos", v.getDiasVendidos())
          .put("pagamento", TempoLicencaRuleFacts.day(v.getDtPagamento()))
          .put("valorIndenizacao", v.getValorIndenizacao())
          .put("consumo", consumo));
    }
    long consumoAcumulado = gozo + abono + vendido + consumo + pa.getDiasConvertidosDobro();
    long pecuniaAcumulada = vendido + abono + pecunia;
    var fatos = builder.putNeighborhood(n -> n
            .put("consumoAcumulado", consumoAcumulado)
            .put("pecuniaAcumulada", pecuniaAcumulada))
        .build();
    if (entity instanceof LicencaEspecialGozo) rules.enforceLicencaEspecialGozo(entity.getEmpCodigo(), fatos);
    else rules.enforceVendaLicencaEspecial(entity.getEmpCodigo(), fatos);
  }

  private static int zero(Integer value) {
    return value == null ? 0 : value;
  }

  public static void recalcularPeriodo(PeriodoAquisitivoLicencaEspecial pa, EntityManager em, ErgonRuleEngineFacade rules) {
    var consumo = apurarConsumo(pa, em);
    long gozo = consumo.gozo();
    long vendido = consumo.venda();
    long saldo = pa.getDiasDireito().longValue() - gozo - vendido - pa.getDiasConvertidosDobro();
    rules.enforceOperational(OperationalRuleSet.PERIODO_SALDO_CONTABIL, pa.getEmpCodigo(), pa.getDiasDireito(), gozo, vendido, pa.getDiasConvertidosDobro());
    pa.setDiasGozados(Math.toIntExact(gozo));
    pa.setDiasVendidos(Math.toIntExact(vendido));
    pa.setSaldoDias(Math.toIntExact(saldo));
    if(java.util.Set.of("DISPONIVEL","PARCIALMENTE_GOZADO","CONVERTIDO_EM_DOBRO","QUITADO").contains(pa.getSituacao())) {
      if(saldo==0) pa.setSituacao(Objects.equals(pa.getDiasConvertidosDobro(),pa.getDiasDireito()) ? "CONVERTIDO_EM_DOBRO" : "QUITADO");
      else if(gozo+vendido>0) pa.setSituacao("PARCIALMENTE_GOZADO");
      else pa.setSituacao(pa.getDiasConvertidosDobro()>0 ? "CONVERTIDO_EM_DOBRO" : "DISPONIVEL");
    }
  }

  record ConsumoPeriodo(long gozo, long venda, int parcelas, int vendas) {}

  static ConsumoPeriodo apurarConsumo(PeriodoAquisitivoLicencaEspecial pa, EntityManager em) {
    var gs = pa.getId() == null ? List.<LicencaEspecialGozo>of() : gozos(pa.getId(), pa.getEmpCodigo(), em);
    long gozo = gs.stream().mapToLong(LicencaEspecialGozo::getDiasGozo).sum();
    var vs = pa.getId() == null ? List.<VendaLicencaEspecial>of() : vendas(pa.getId(), pa.getEmpCodigo(), em);
    long vendido = vs.stream().mapToLong(VendaLicencaEspecial::getDiasVendidos).sum()
        + gs.stream().mapToLong(LicencaEspecialGozo::getDiasAbono).sum();
    return new ConsumoPeriodo(gozo, vendido, gs.size(), vs.size());
  }

  public static void sincronizar(Long id, TenantContext tenant, EntityManager em, ErgonRuleEngineFacade rules) {
    // O lock já foi obtido na validação/exclusão, antes da alteração da parcela.
    em.flush();
    var pa = parent(id, tenant, em);
    recalcularPeriodo(pa, em, rules);
    em.flush();
  }

  private static List<VendaLicencaEspecial> abonos(LicencaEspecialGozo g, EntityManager em, ErgonRuleEngineFacade rules) {
    if(g.getId()==null) return List.of();
    var linked = em.createQuery("select v from VendaLicencaEspecial v where v.empCodigo=:org and v.licencaEspecialGozoId=:gozo order by v.id", VendaLicencaEspecial.class)
        .setParameter("org", g.getEmpCodigo()).setParameter("gozo", g.getId()).setFlushMode(FlushModeType.COMMIT)
        .setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();
    linked.forEach(em::refresh);
    rules.enforceOperational(OperationalRuleSet.GOZO_ABONO_UNICO, g.getEmpCodigo(), linked.stream().filter(v -> !"ESTORNADA".equals(v.getSituacao())).count());
    return linked;
  }

  /** Guarda anterior ao merge, compartilhada por PATCH, PUT, lote e transições. */
  static void validarAbonoLiquidado(LicencaEspecialGozo g, EntityManager em, ErgonRuleEngineFacade rules) {
    abonos(g, em, rules).forEach(v -> preservarLiquidacao(v, g, rules));
  }

  private static void preservarLiquidacao(VendaLicencaEspecial v, LicencaEspecialGozo g,
      ErgonRuleEngineFacade rules) {
    if (!"PAGA".equals(v.getSituacao())) return;
    var facts = TempoLicencaRuleFacts.common(g, null)
        .putDraft(draft -> draft
            .put("acao", "preservar-abono-liquidado")
            .put("abono", g.getAbonoPecuniario())
            .put("diasAbono", g.getDiasAbono())
            .put("mesAnoFolha", TempoLicencaRuleFacts.day(g.getMesAnoFolha()))
            .put("numFolha", g.getNumFolha()))
        .putConcept("periodoAquisitivo", pa -> {})
        .putConcept("politica", pol -> {})
        .putConcept("venda", venda -> venda
            .put("estado", v.getSituacao())
            .put("diasVendidos", v.getDiasVendidos())
            .put("mesAnoFolha", TempoLicencaRuleFacts.day(v.getMesAnoFolha()))
            .put("numFolha", v.getNumFolha()))
        .putNeighborhood(n -> {})
        .build();
    rules.enforceLicencaEspecialGozo(g.getEmpCodigo(), facts);
  }

  /** PCK_LIC_ESP MAIN_POS, linhas 305–333: proporção calculada sobre o intervalo efetivo. */
  static void recalcularAbonoEncerramento(LicencaEspecialGozo g, TenantContext tenant, EntityManager em, ErgonRuleEngineFacade rules) {
    if(!Boolean.TRUE.equals(g.getAbonoPecuniario())) return;
    var pa=parent(g.getPeriodoAquisitivoLicencaId(),tenant,em);
    var policy=EffectiveSpecialLeavePolicy.resolve(pa,g,em);
    rules.enforceLicencaEspecialGozo(g.getEmpCodigo(), TempoLicencaRuleFacts.common(g, null)
        .putDraft(draft -> draft.put("acao", "recalcular-abono-encerramento"))
        .putConcept("periodoAquisitivo", pNode -> {})
        .putConcept("politica", pol -> pol
            .put("baseAbono", policy.baseAbono())
            .put("numeradorAbono", policy.numeradorAbono())
            .put("denominadorAbono", policy.denominadorAbono()))
        .putConcept("venda", vNode -> {})
        .putNeighborhood(n -> {})
        .build());
    long base="G".equals(policy.baseAbono()) ? g.getDiasGozo() : pa.getDiasDireito();
    int dias=Math.toIntExact(base*policy.numeradorAbono()/policy.denominadorAbono());
    g.setDiasAbono(dias);
    if(dias==0) { g.setAbonoPecuniario(false);g.setIniFimAbono(null); }

  }

  /** O abono espelha a própria parcela; a consulta de vendas avulsas exclui esse espelho. */
  public static void sincronizarAbono(LicencaEspecialGozo g, EntityManager em, ErgonRuleEngineFacade rules) {
    manterRegistroAbono(g,em,false,rules);
  }

  /** Reemissão após estorno depende do ato explícito de reconciliação. */
  public static VendaLicencaEspecial reconciliarAbono(LicencaEspecialGozo g, EntityManager em, ErgonRuleEngineFacade rules) {
    return manterRegistroAbono(g,em,true,rules);
  }

  private static VendaLicencaEspecial manterRegistroAbono(LicencaEspecialGozo g, EntityManager em, boolean reemitir, ErgonRuleEngineFacade rules) {
    if ("PROGRAMADA".equals(g.getSituacao())) return null;
    var linked=abonos(g,em,rules);
    var active=linked.stream().filter(v -> !"ESTORNADA".equals(v.getSituacao())).toList();
    active.forEach(v -> preservarLiquidacao(v,g,rules));
    boolean ativo = Boolean.TRUE.equals(g.getAbonoPecuniario()) && !"CANCELADO".equals(g.getSituacao());
    if (!ativo) {
      active.forEach(v -> v.setSituacao("ESTORNADA"));
      return null;
    }
    if(active.isEmpty() && !linked.isEmpty() && !reemitir) return null;
    VendaLicencaEspecial v = active.isEmpty() ? new VendaLicencaEspecial() : active.getFirst();
    // Uma liquidação é histórica; nem documentos ou descrição são reescritos pelo espelho.
    if("PAGA".equals(v.getSituacao())) return v;
    if(!Objects.equals(v.getDiasVendidos(),g.getDiasAbono())) v.setValorIndenizacao(null);
    v.setEmpCodigo(g.getEmpCodigo()); v.setNumfunc(g.getNumfunc()); v.setNumvinc(g.getNumvinc());
    v.setPeriodoAquisitivoLicencaId(g.getPeriodoAquisitivoLicencaId()); v.setChavePeraqlicesp(g.getChavePeraqlicesp());
    v.setLicencaEspecialGozoId(g.getId()); v.setModalidade("ABONO_JUNTO_GOZO");
    v.setDescricao("Abono da parcela " + g.getOrdemParcela()); v.setDtini(g.getDtini());
    v.setDiasVendidos(g.getDiasAbono()); v.setMesAnoFolha(g.getMesAnoFolha()); v.setNumFolha(g.getNumFolha());
    v.setDocumentoLegalId(g.getDocumentoLegalId()); v.setAtoPublicacaoId(g.getAtoPublicacaoId());
    if (!"PAGA".equals(v.getSituacao())) v.setSituacao("AUTORIZADA");
    if (active.isEmpty()) em.persist(v);
    return v;
  }

  static void semParcelas(Long id, Integer org, EntityManager em) {
    long count = em.createQuery("select count(g) from LicencaEspecialGozo g where g.empCodigo=:org and g.periodoAquisitivoLicencaId=:pa", Long.class).setParameter("org", org).setParameter("pa", id).getSingleResult()
        + em.createQuery("select count(v) from VendaLicencaEspecial v where v.empCodigo=:org and v.periodoAquisitivoLicencaId=:pa", Long.class).setParameter("org", org).setParameter("pa", id).getSingleResult();
    if (count > 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "Período possui parcelas de gozo ou venda vinculadas.");
  }

  static void semPeriodos(Long id, Integer org, EntityManager em) {
    if (em.createQuery("select count(p) from PeriodoAquisitivoLicencaEspecial p where p.empCodigo=:org and p.averbacaoOrigemId=:origem", Long.class)
        .setParameter("org", org).setParameter("origem", id).getSingleResult() > 0)
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Averbação possui períodos aquisitivos vinculados.");
  }

  public static void validarImpactoOrigem(AverbacaoTempoServico a, EntityManager em) {
    if (consumoPeriodosGerados(a, em) > 0)
      throw new ResponseStatusException(HttpStatus.CONFLICT,
          "Períodos de origem possuem consumo ou conversão em dobro.");
  }

  static long consumoPeriodosGerados(AverbacaoTempoServico a, EntityManager em) {
    if (a.getId() == null) return 0;
    var periods=em.createQuery("select p from PeriodoAquisitivoLicencaEspecial p where p.empCodigo=:org and p.averbacaoOrigemId=:origem",PeriodoAquisitivoLicencaEspecial.class)
        .setParameter("org",a.getEmpCodigo()).setParameter("origem",a.getId()).setFlushMode(FlushModeType.COMMIT).getResultList();
    long consumo = 0;
    for(var pa:periods) {
      em.lock(pa,LockModeType.PESSIMISTIC_WRITE);em.refresh(pa);
      var saldo = apurarConsumo(pa, em);
      consumo += saldo.gozo() + saldo.venda() + pa.getDiasConvertidosDobro();
    }
    return consumo;
  }

  public static void reconciliarOrigem(AverbacaoTempoServico a, EntityManager em, ErgonRuleEngineFacade rules) {
    if(!"LICENCA_ESPECIAL".equals(a.getFinalidade()) || "EM_ANALISE".equals(a.getSituacao())) return;
    var periods=em.createQuery("select p from PeriodoAquisitivoLicencaEspecial p where p.empCodigo=:org and p.averbacaoOrigemId=:origem",PeriodoAquisitivoLicencaEspecial.class)
        .setParameter("org",a.getEmpCodigo()).setParameter("origem",a.getId()).setFlushMode(FlushModeType.COMMIT).getResultList();
    if("CANCELADO".equals(a.getSituacao())) {
      validarImpactoOrigem(a,em); periods.forEach(pa->pa.setSituacao("CANCELADO"));return;
    }
    int direito=Math.subtractExact(a.getDiasFinalidade(), a.getDiasDesaverbados());
    if(periods.isEmpty()) {
      // A chave é alocada com o vínculo bloqueado, sem copiar a chave única da averbação.
      var owner=em.find(Vinculo.class,new VinculoId(a.getNumfunc(),a.getNumvinc()),LockModeType.PESSIMISTIC_WRITE);
      if (owner == null)
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Vínculo de origem inexistente.");
      ErgonTenantAccess.requireReferencesInOrganization(a.getEmpCodigo(), owner);
      Integer max=em.createQuery("select coalesce(max(p.chave),0) from PeriodoAquisitivoLicencaEspecial p where p.empCodigo=:org and p.numfunc=:pessoa and p.numvinc=:vinculo",Integer.class)
          .setParameter("org",a.getEmpCodigo()).setParameter("pessoa",a.getNumfunc()).setParameter("vinculo",a.getNumvinc()).getSingleResult();
      var pa=new PeriodoAquisitivoLicencaEspecial();pa.setEmpCodigo(a.getEmpCodigo());pa.setNumfunc(a.getNumfunc());pa.setNumvinc(a.getNumvinc());
      pa.setChave(Math.addExact(max,1));pa.setDescricao("Licença especial por averbação " + a.getChave());
      pa.setDtini(a.getDtini());pa.setDtfim(a.getDtfim());pa.setDiasDireito(direito);pa.setSaldoDias(direito);
      pa.setAverbacaoOrigemId(a.getId());pa.setDocumentoLegalId(a.getDocumentoLegalId());pa.setAtoPublicacaoId(a.getAtoPublicacaoId());
      pa.setObservacoes("Período gerado pela homologação da averbação de tempo de serviço.");
      rules.enforcePeriodoAquisitivoLicencaEspecial(pa.getEmpCodigo(),
          TempoLicencaRuleFacts.periodo(pa, null, false, owner, em));
      em.persist(pa);
    } else if(periods.size()!=1 || !Objects.equals(periods.getFirst().getDiasDireito(),direito)) {
      validarImpactoOrigem(a,em);
      rules.enforceOperational(OperationalRuleSet.AVERBACAO_REDISTRIBUICAO, a.getEmpCodigo(), periods.size());
      var pa=periods.getFirst();pa.setDiasDireito(direito);recalcularPeriodo(pa,em,rules);
    }
  }

}
