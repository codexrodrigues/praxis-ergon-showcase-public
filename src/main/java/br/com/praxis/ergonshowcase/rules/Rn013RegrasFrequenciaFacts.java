package br.com.praxis.ergonshowcase.rules;

import br.com.praxis.ergonshowcase.domain.RegraFrequenciaEstatutaria;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import org.praxisplatform.rules.facts.PraxisFactTreeBuilder;

/** Grafo ontológico de fatos de regras estatutárias de frequência ({@code RN-013}). */
public final class Rn013RegrasFrequenciaFacts {
  private Rn013RegrasFrequenciaFacts() {}

  public static ObjectNode of(
      ObjectMapper mapper,
      RegraFrequenciaEstatutaria draft,
      List<RegraFrequenciaEstatutaria> existingOverlaps,
      Integer empCodigo) {
    ObjectNode root = PraxisFactTreeBuilder.create()
        .putDraft(node -> populateDraftNode(mapper, node, draft))
        .putConcept("tenant", node -> node.put("empCodigo", empCodigo != null ? empCodigo : 1))
        .putContext(node -> node
            .put("resourceKey", Rn013RegrasFrequenciaRuleSet.RESOURCE_KEY)
            .put("ruleSetKey", Rn013RegrasFrequenciaRuleSet.RULESET_KEY))
        .build();

    ArrayNode overlapsArray = mapper.createArrayNode();
    if (existingOverlaps != null) {
      for (RegraFrequenciaEstatutaria existing : existingOverlaps) {
        if (existing != null) {
          overlapsArray.add(toDraftNode(mapper, existing));
        }
      }
    }
    root.set("existingOverlaps", overlapsArray);
    return root;
  }

  public static ObjectNode toDraftNode(ObjectMapper mapper, RegraFrequenciaEstatutaria entity) {
    return PraxisFactTreeBuilder.create()
        .putDraft(node -> populateDraftNode(mapper, node, entity))
        .build()
        .withObject("/draft");
  }

  private static void populateDraftNode(
      ObjectMapper mapper, PraxisFactTreeBuilder node, RegraFrequenciaEstatutaria entity) {
    node.put("id", entity != null ? entity.getId() : null)
        .put("empCodigo", entity != null ? entity.getEmpCodigo() : null)
        .put("regimeJuridico", entity != null ? entity.getRegimeJuridico() : null)
        .put("tipoVinculo", entity != null ? entity.getTipoVinculo() : null)
        .put("categoria", entity != null ? entity.getCategoria() : null)
        .put("subcategoria", entity != null ? entity.getSubcategoria() : null)
        .put("tipofreq", entity != null ? entity.getTipofreq() : null)
        .put("codfreq", entity != null ? entity.getCodfreq() : null);
    if (entity != null && entity.getDtini() != null) {
      node.put("dtini", entity.getDtini());
    }
    node.put("dtfim", entity != null ? entity.getDtfim() : null)
        .put("qtdMinDias", entity != null ? entity.getQtdMinDias() : null)
        .put("qtdMaxDias", entity != null ? entity.getQtdMaxDias() : null)
        .put("intervMinDias", entity != null ? entity.getIntervMinDias() : null)
        .put("intProrrogDias", entity != null ? entity.getIntProrrogDias() : null)
        .put("qtdMaxConsecMes", entity != null ? entity.getQtdMaxConsecMes() : null)
        .put("qtdMaxInterpMes", entity != null ? entity.getQtdMaxInterpMes() : null)
        .put("qtdMaxConsecAno", entity != null ? entity.getQtdMaxConsecAno() : null)
        .put("qtdMaxInterpAno", entity != null ? entity.getQtdMaxInterpAno() : null)
        .put("exigeHomologacao", entity != null ? entity.getExigeHomologacao() : null)
        .put("permiteConcomitancia", entity != null ? entity.getPermiteConcomitancia() : null)
        .put("naoSeAplica", entity != null ? entity.getNaoSeAplica() : null)
        .put("ativa", entity != null ? entity.getAtiva() : null)
        .put("documentoLegalId", entity != null ? entity.getDocumentoLegalId() : null)
        .put("ruleKey", entity != null ? entity.getRuleKey() : null);

    ObjectNode paramsNode = mapper.createObjectNode();
    paramsNode.putNull("prorrogacao");
    paramsNode.putNull("prazo");
    paramsNode.putNull("tipoUnidadePrazo");
    paramsNode.putNull("intersticio");
    paramsNode.putNull("tipoUnidadeInterst");
    paramsNode.putNull("tipoUnidade");
    paramsNode.putNull("pontlei");
    if (entity != null && entity.getRuleParams() != null) {
      entity.getRuleParams().forEach((k, v) -> paramsNode.set(k, mapper.valueToTree(v)));
    }
    node.put("ruleParams", paramsNode);
  }
}
