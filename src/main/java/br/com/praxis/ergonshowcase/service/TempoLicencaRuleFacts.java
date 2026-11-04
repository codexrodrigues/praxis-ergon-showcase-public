package br.com.praxis.ergonshowcase.service;

import br.com.praxis.ergonshowcase.domain.*;
import br.com.praxis.ergonshowcase.rules.EffectiveVacationParameterResolver;
import br.com.praxis.ergonshowcase.rules.ErgonRuleEngineFacade;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoUnit;
import org.praxisplatform.rules.facts.PraxisFactTreeBuilder;

/** Datas e contagens da transação para as decisões de tempo e licença especial. */
final class TempoLicencaRuleFacts {
  private static final DateTimeFormatter COMPLEMENTO =
      DateTimeFormatter.ofPattern("ddMMuuuu").withResolverStyle(ResolverStyle.STRICT);

  private TempoLicencaRuleFacts() {}

  static PraxisFactTreeBuilder common(AssentamentoTempoLicenca entity, Vinculo vinculo) {
    return PraxisFactTreeBuilder.create()
        .putDraft(draft -> draft
            .put("inicio", day(entity.getDtini()))
            .put("fim", day(entity.getDtfim()))
            .put("numfunc", entity.getNumfunc())
            .put("situacao", entity.getSituacao())
            .put("documentoLegalId", entity.getDocumentoLegalId()))
        .putConcept("vinculo", v -> v
            .put("exercicio", vinculo == null ? null : day(vinculo.getDtexerc()))
            .put("vacancia", vinculo == null ? null : day(vinculo.getDtvac())));
  }

  static ObjectNode averbacao(AverbacaoTempoServico a, Vinculo vinculo, EntityManager em,
      long consumoPeriodosGerados) {
    String[] partes = a.getComplemento() == null ? new String[0] : a.getComplemento().split("-", -1);
    long sobreposicoes = a.getDtini() == null || a.getDtfim() == null ? 0 :
        em.createQuery("""
            select count(a) from AverbacaoTempoServico a
             where a.empCodigo=:org and a.numfunc=:pessoa and a.numvinc=:vinculo
               and (:id is null or a.id<>:id) and a.finalidade=:finalidade
               and a.situacao not in ('CANCELADO','DESAVERBADA_TOTAL')
               and a.dtini<=:fim and a.dtfim>=:inicio
            """, Long.class)
            .setParameter("org", a.getEmpCodigo()).setParameter("pessoa", a.getNumfunc())
            .setParameter("vinculo", a.getNumvinc()).setParameter("id", a.getId())
            .setParameter("finalidade", a.getFinalidade()).setParameter("inicio", a.getDtini())
            .setParameter("fim", a.getDtfim()).setFlushMode(FlushModeType.COMMIT).getSingleResult();
    return common(a, vinculo)
        .putDraft(draft -> draft
            .put("dataAContar", day(a.getDataAContar()))
            .put("diasCorridos", inclusive(a.getDtini(), a.getDtfim()))
            .put("totalDias", a.getTotalDias())
            .put("diasFinalidade", a.getDiasFinalidade())
            .put("diasDesaverbados", a.getDiasDesaverbados())
            .put("finalidade", a.getFinalidade())
            .put("regimePrevidenciario", a.getRegimePrevidenciario())
            .put("sobrepoe", a.getSobrepoe())
            .put("complementoInicio", partes.length == 2 ? parse(partes[0]) : null)
            .put("complementoFim", partes.length == 2 ? parse(partes[1]) : null))
        .putNeighborhood(n -> n
            .put("consumoPeriodosGerados", consumoPeriodosGerados)
            .put("sobreposicoes", sobreposicoes))
        .build();
  }

  static ObjectNode periodo(PeriodoAquisitivoLicencaEspecial pa,
      PeriodoAquisitivoLicencaEspecial anterior, boolean edicaoManual, Vinculo vinculo, EntityManager em) {
    var policy = EffectiveSpecialLeavePolicy.resolve(pa, null, em);
    var consumo = TempoLicencaValidation.apurarConsumo(pa, em);
    LocalDate referencia = pa.getDtfim();
    if (vinculo.getDtexerc() != null && (referencia == null || referencia.isBefore(vinculo.getDtexerc())))
      referencia = vinculo.getDtexerc();
    var matriz = new EffectiveVacationParameterResolver().resolveActiveParameter(em, vinculo, referencia);
    long sobreposicoes = "CANCELADO".equals(pa.getSituacao()) ? 0 :
        em.createQuery("""
            select count(p) from PeriodoAquisitivoLicencaEspecial p
             where p.empCodigo=:org and p.numfunc=:pessoa and p.numvinc=:vinculo
               and (:id is null or p.id<>:id) and p.situacao<>'CANCELADO'
               and (p.chave=:chave or (p.dtini<=:fim and p.dtfim>=:inicio))
            """, Long.class)
            .setParameter("org", pa.getEmpCodigo()).setParameter("pessoa", pa.getNumfunc())
            .setParameter("vinculo", pa.getNumvinc()).setParameter("id", pa.getId())
            .setParameter("chave", pa.getChave()).setParameter("inicio", pa.getDtini())
            .setParameter("fim", pa.getDtfim()).setFlushMode(FlushModeType.COMMIT).getSingleResult();
    return common(pa, vinculo)
        .putDraft(draft -> draft
            .put("prescricao", day(pa.getDtPrescricao()))
            .put("origem", pa.getAverbacaoOrigemId())
            .put("anosIndividuais", pa.getAnosLicEsp())
            .put("mesesIndividuais", pa.getMesesLicEspPer())
            .put("diasDireito", pa.getDiasDireito())
            .put("diasConvertidosDobro", pa.getDiasConvertidosDobro())
            .put("vendLicEsp", pa.getVendLicEsp())
            .put("contDobroLicEsp", pa.getContDobroLicEsp()))
        .putCurrent(current -> current
            .put("edicaoManual", edicaoManual)
            .put("origem", anterior == null ? null : anterior.getAverbacaoOrigemId())
            .put("inicio", anterior == null ? null : day(anterior.getDtini()))
            .put("fim", anterior == null ? null : day(anterior.getDtfim()))
            .put("diasDireito", anterior == null ? null : anterior.getDiasDireito()))
        .putConcept("politica", pol -> pol
            .put("temLicEsp", matriz == null ? null : matriz.getTemLicEsp())
            .put("mesesPeriodo", pa.getMesesLicEspPer() != null
                ? pa.getMesesLicEspPer() : matriz == null ? null : matriz.getMesesLicEspPer())
            .put("permiteDobro", policy.permiteDobro())
            .put("minimoDobro", policy.minimoDobro())
            .put("maximoDobro", policy.maximoDobro()))
        .putNeighborhood(n -> n
            .put("gozoAtivo", consumo.parcelas())
            .put("vendaAtiva", consumo.vendas())
            .put("diasGozados", consumo.gozo())
            .put("diasVendidos", consumo.venda())
            .put("sobreposicoes", sobreposicoes))
        .build();
  }

  static int proximaChave(Class<? extends AssentamentoTempoLicenca> type,
      AssentamentoTempoLicenca entity, Vinculo vinculo, EntityManager em) {
    em.lock(vinculo, LockModeType.PESSIMISTIC_WRITE);
    Integer maximo = em.createQuery("select coalesce(max(a.chave),0) from " + type.getSimpleName()
            + " a where a.empCodigo=:org and a.numfunc=:pessoa and a.numvinc=:vinculo", Integer.class)
        .setParameter("org", entity.getEmpCodigo()).setParameter("pessoa", entity.getNumfunc())
        .setParameter("vinculo", entity.getNumvinc()).setFlushMode(FlushModeType.COMMIT)
        .getSingleResult();
    return Math.addExact(maximo, 1);
  }

  static long sobreposicoesGozo(LicencaEspecialGozo g, LocalDate fim, Vinculo vinculo,
      EntityManager em, ErgonRuleEngineFacade rules) {
    if (g.getDtini() == null || fim == null || fim.isBefore(g.getDtini())) return 0;
    long licencas = em.createQuery("""
        select count(x) from LicencaEspecialGozo x
         where x.empCodigo=:org and x.numfunc=:pessoa and x.numvinc=:vinculo
           and (:id is null or x.id<>:id) and x.situacao<>'CANCELADO'
           and x.dtini<=:fim and coalesce(x.dtfim,x.dtprevfim)>=:inicio
        """, Long.class)
        .setParameter("org", g.getEmpCodigo()).setParameter("pessoa", g.getNumfunc())
        .setParameter("vinculo", g.getNumvinc()).setParameter("id", g.getId())
        .setParameter("inicio", g.getDtini()).setParameter("fim", fim)
        .setFlushMode(FlushModeType.COMMIT).getSingleResult();
    long ferias = em.createQuery("""
        select count(x) from FeriasGozo x
         where x.empCodigo=:org and x.numfunc=:pessoa and x.numvinc=:vinculo
           and x.situacao<>'CANCELADA' and x.dtini<=:fim
           and coalesce(x.dtfim,x.dtprevfim)>=:inicio
        """, Long.class)
        .setParameter("org", g.getEmpCodigo()).setParameter("pessoa", g.getNumfunc())
        .setParameter("vinculo", g.getNumvinc()).setParameter("inicio", g.getDtini())
        .setParameter("fim", fim).setFlushMode(FlushModeType.COMMIT).getSingleResult();
    var afastamentos = em.createQuery("""
        select x from Afastamento x
         where x.empCodigo=:org and x.numfunc=:pessoa and x.numvinc=:vinculo
           and x.situacao='HOMOLOGADO' and x.dtini<=:fim
           and (coalesce(x.dtfim,x.dtprevfim) is null or coalesce(x.dtfim,x.dtprevfim)>=:inicio)
        """, Afastamento.class)
        .setParameter("org", g.getEmpCodigo()).setParameter("pessoa", g.getNumfunc())
        .setParameter("vinculo", g.getNumvinc()).setParameter("inicio", g.getDtini())
        .setParameter("fim", fim).setFlushMode(FlushModeType.COMMIT).getResultList();
    long incompatíveis = afastamentos.stream().filter(a -> {
      var candidatos = em.createQuery("""
          select r from RegraFrequenciaEstatutaria r where r.tipofreq=:tipo and r.codfreq=:codigo
          """, RegraFrequenciaEstatutaria.class)
          .setParameter("tipo", a.getTipofreq()).setParameter("codigo", a.getCodfreq())
          .setFlushMode(FlushModeType.COMMIT).getResultList();
      return rules.resolveEffectiveStatutoryRule(candidatos, vinculo, a.getTipofreq(),
          a.getCodfreq(), a.getDtini()).map(r -> !Boolean.TRUE.equals(r.getPermiteConcomitancia()))
          .orElse(true);
    }).count();
    return licencas + ferias + incompatíveis;
  }

  static Long day(LocalDate date) {
    return date == null ? null : date.toEpochDay();
  }

  static Long inclusive(LocalDate start, LocalDate end) {
    return start == null || end == null ? null : ChronoUnit.DAYS.between(start, end) + 1;
  }

  private static Long parse(String text) {
    if (!text.matches("[0-9]{8}")) return null;
    try {
      return day(LocalDate.parse(text, COMPLEMENTO));
    } catch (DateTimeParseException invalid) {
      return null;
    }
  }
}
