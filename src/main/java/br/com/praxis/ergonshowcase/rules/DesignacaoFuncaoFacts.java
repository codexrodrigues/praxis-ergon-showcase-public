package br.com.praxis.ergonshowcase.rules;

import br.com.praxis.ergonshowcase.domain.DesignacaoFuncao;
import br.com.praxis.ergonshowcase.domain.VagaFuncao;
import br.com.praxis.ergonshowcase.domain.VinculoId;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import org.praxisplatform.rules.facts.PraxisFactTreeBuilder;

/** Grafo ontológico de fatos de designação de função (rascunho, estado prévio, vaga 1-hop e vizinhança). */
public final class DesignacaoFuncaoFacts {
  private DesignacaoFuncaoFacts() {}

  public static ObjectNode load(EntityManager em, DesignacaoFuncao draft, String estadoAnterior) {
    VagaFuncao vaga = draft.getVagaFuncaoId() == null ? null
        : em.find(VagaFuncao.class, draft.getVagaFuncaoId());
    long outrosTitulares = vaga == null ? 0
        : em.createQuery("""
            select d.titularId from DesignacaoFuncao d
            where d.empCodigo = :orgao and d.vagaFuncaoId = :vaga and d.situacao = 'EM_EXERCICIO'
              and d.id <> :excluir
            """, VinculoId.class)
            .setParameter("orgao", draft.getEmpCodigo())
            .setParameter("vaga", vaga.getId())
            .setParameter("excluir", draft.getId() == null ? -1L : draft.getId())
            .setMaxResults(2)
            .getResultList()
            .size();
    return of(draft, estadoAnterior, vaga == null ? null : vaga.getSituacao(),
        vaga == null ? null : vaga.getFuncaoId(), outrosTitulares);
  }

  public static ObjectNode of(DesignacaoFuncao draft, String estadoAnterior, String estadoVaga,
      Long funcaoDaVaga, long outrosTitulares) {
    return PraxisFactTreeBuilder.create()
        .putDraft(node -> node
            .put("titular", draft.getTitularId() == null ? null : draft.getTitularId().numfunc())
            .put("primeiro", draft.getSubstituto1Id() == null ? null : draft.getSubstituto1Id().numfunc())
            .put("segundo", draft.getSubstituto2Id() == null ? null : draft.getSubstituto2Id().numfunc())
            .put("situacao", draft.getSituacao())
            .put("inicio", epochDay(draft.getDtini()))
            .put("fim", epochDay(draft.getDtfim()))
            .put("exercicio", epochDay(draft.getDtExercicio()))
            .put("vagaId", draft.getVagaFuncaoId())
            .put("funcao", draft.getFuncaoId())
            .put("atoPublicacaoId", draft.getAtoPublicacaoId()))
        .putCurrent(node -> node.put("situacao", estadoAnterior))
        .putConcept("vaga", node -> node.put("situacao", estadoVaga).put("funcao", funcaoDaVaga))
        .putNeighborhood(node -> node.put("outrosTitulares", outrosTitulares))
        .build();
  }

  private static Long epochDay(LocalDate value) {
    return value == null ? null : value.toEpochDay();
  }
}
