package br.com.praxis.ergonshowcase.rules;

import br.com.praxis.ergonshowcase.domain.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import java.time.LocalDate;
import org.praxisplatform.rules.facts.PraxisFactTreeBuilder;

/** Grafo ontológico de fatos de substituição (rascunho, conceitos 1-hop e vizinhança do órgão). */
public final class SubstituicaoFuncaoFacts {
  private SubstituicaoFuncaoFacts() {}

  public static ObjectNode load(EntityManager em, SubstituicaoFuncao draft) {
    Integer orgao = draft.getEmpCodigo();
    Tuple titular = em.createQuery("""
        select d.titularId as vinculo, d.funcaoId as funcao, d.dtExercicio as inicio,
               d.dtfim as fim, d.situacao as situacao
        from DesignacaoFuncao d where d.empCodigo = :orgao and d.id = :id
        """, Tuple.class)
        .setParameter("orgao", orgao).setParameter("id", draft.getDesignacaoId()).getSingleResult();
    Tuple substituto = em.createQuery("""
        select v.dtexerc as exercicio, v.dtvac as vacancia,
               v.dtiniCessao as inicioCessao, v.dtfimCessao as fimCessao
        from Vinculo v where v.empCodigo = :orgao and v.id = :id
        """, Tuple.class)
        .setParameter("orgao", orgao).setParameter("id", draft.getSubstitutoId()).getSingleResult();
    Tuple politica = em.createQuery("""
        select f.permiteSubstituicao as permite, f.diasMinimosSubstituicao as minimo
        from FuncaoConfianca f where f.empCodigo = :orgao and f.id = :id
        """, Tuple.class)
        .setParameter("orgao", orgao).setParameter("id", titular.get("funcao", Long.class)).getSingleResult();
    VinculoId vinculoTitular = titular.get("vinculo", VinculoId.class);

    return PraxisFactTreeBuilder.create()
        .putDraft(node -> node
            .put("inicio", epochDay(draft.getDtini()))
            .put("fim", epochDay(draft.getDtfim()))
            .put("inicioRemunerado", epochDay(draft.getDtiniRemunerada()))
            .put("fimRemunerado", epochDay(draft.getDtfimRemunerada()))
            .put("substituto", draft.getSubstitutoId().numfunc())
            .put("documentoLegalId", draft.getDocumentoLegalId()))
        .putConcept("designacao", node -> node
            .put("titular", vinculoTitular.numfunc())
            .put("inicio", epochDay(titular.get("inicio", LocalDate.class)))
            .put("fim", epochDay(titular.get("fim", LocalDate.class)))
            .put("situacao", titular.get("situacao", String.class)))
        .putConcept("substituto", node -> node
            .put("exercicio", epochDay(substituto.get("exercicio", LocalDate.class)))
            .put("vacancia", epochDay(substituto.get("vacancia", LocalDate.class)))
            .put("inicioCessao", epochDay(substituto.get("inicioCessao", LocalDate.class)))
            .put("fimCessao", epochDay(substituto.get("fimCessao", LocalDate.class))))
        .putConcept("funcao", node -> node
            .put("permiteSubstituicao", politica.get("permite", Boolean.class))
            .put("diasMinimosSubstituicao", politica.get("minimo", Integer.class)))
        .putConcept("impedimento", node -> impedimento(em, draft, vinculoTitular, node))
        .putNeighborhood(node -> node
            .put("outrasSubstituicoesTitular", concomitancias(em, draft, "designacaoId", draft.getDesignacaoId()))
            .put("outrasSubstituicoesSubstituto", concomitancias(em, draft, "substitutoId", draft.getSubstitutoId()))
            .put("feriasSubstituto", feriasSubstituto(em, draft))
            .put("afastamentosSubstituto", afastamentosSubstituto(em, draft)))
        .build();
  }

  private static long feriasSubstituto(EntityManager em, SubstituicaoFuncao draft) {
    VinculoId substituto = draft.getSubstitutoId();
    return em.createQuery("""
        select count(f) from FeriasGozo f
        where f.empCodigo = :orgao and f.numfunc = :numfunc and f.numvinc = :numvinc
          and f.situacao in ('HOMOLOGADA', 'EM_GOZO', 'GOZADA', 'INTERROMPIDA')
          and (f.dtInterrupcao is null or f.dtInterrupcao > :inicio)
          and f.dtini <= :fim and coalesce(f.dtfim, f.dtprevfim) >= :inicio
        """, Long.class)
        .setParameter("orgao", draft.getEmpCodigo())
        .setParameter("numfunc", substituto.numfunc())
        .setParameter("numvinc", substituto.numero())
        .setParameter("inicio", draft.getDtini())
        .setParameter("fim", draft.getDtfim())
        .getSingleResult();
  }

  private static long afastamentosSubstituto(EntityManager em, SubstituicaoFuncao draft) {
    VinculoId substituto = draft.getSubstitutoId();
    return em.createQuery("""
        select count(a) from Afastamento a
        where a.empCodigo = :orgao and a.numfunc = :numfunc and a.numvinc = :numvinc
          and a.situacao = 'HOMOLOGADO'
          and a.dtini <= :fim and coalesce(a.dtfim, a.dtprevfim, :fim) >= :inicio
        """, Long.class)
        .setParameter("orgao", draft.getEmpCodigo())
        .setParameter("numfunc", substituto.numfunc())
        .setParameter("numvinc", substituto.numero())
        .setParameter("inicio", draft.getDtini())
        .setParameter("fim", draft.getDtfim())
        .getSingleResult();
  }

  private static long concomitancias(EntityManager em, SubstituicaoFuncao draft,
      String field, Object key) {
    return em.createQuery("""
        select count(s) from SubstituicaoFuncao s
        where s.empCodigo = :orgao and s.%s = :key and s.situacao <> 'CANCELADA'
          and s.id <> :id and s.dtini <= :fim and s.dtfim >= :inicio
        """.formatted(field), Long.class).setParameter("orgao", draft.getEmpCodigo()).setParameter("key", key)
        .setParameter("id", draft.getId() == null ? -1L : draft.getId())
        .setParameter("inicio", draft.getDtini()).setParameter("fim", draft.getDtfim())
        .getSingleResult();
  }

  private static void impedimento(EntityManager em, SubstituicaoFuncao draft, VinculoId titular,
      PraxisFactTreeBuilder node) {
    boolean ambos = draft.getAfastamentoId() != null && draft.getFeriasGozoId() != null;
    boolean ferias = !ambos && draft.getFeriasGozoId() != null;
    Long id = ambos ? null : (ferias ? draft.getFeriasGozoId() : draft.getAfastamentoId());
    Tuple row = id == null ? null : em.createQuery(ferias ? """
        select a.situacao as situacao, a.dtini as inicio,
               coalesce(a.dtfim, a.dtprevfim) as fim, a.dtInterrupcao as interrupcao
        from FeriasGozo a
        where a.empCodigo = :orgao and a.id = :id and a.numfunc = :numfunc and a.numvinc = :numvinc
        """ : """
        select a.situacao as situacao, a.dtini as inicio,
               coalesce(a.dtfim, a.dtprevfim) as fim
        from Afastamento a
        where a.empCodigo = :orgao and a.id = :id and a.numfunc = :numfunc and a.numvinc = :numvinc
        """, Tuple.class)
        .setParameter("orgao", draft.getEmpCodigo()).setParameter("id", id)
        .setParameter("numfunc", titular.numfunc()).setParameter("numvinc", titular.numero())
        .getResultList().stream().findFirst().orElse(null);
    node.put("tipo", row == null ? null : (ferias ? "FERIAS" : "AFASTAMENTO"))
        .put("situacao", row == null ? null : row.get("situacao", String.class))
        .put("inicio", row == null ? null : epochDay(row.get("inicio", LocalDate.class)))
        .put("fim", row == null ? null : epochDay(row.get("fim", LocalDate.class)))
        .put("interrupcao", row == null || !ferias ? null : epochDay(row.get("interrupcao", LocalDate.class)));
  }

  private static Long epochDay(LocalDate value) {
    return value == null ? null : value.toEpochDay();
  }
}
