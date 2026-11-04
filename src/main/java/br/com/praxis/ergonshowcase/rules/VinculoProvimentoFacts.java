package br.com.praxis.ergonshowcase.rules;

import br.com.praxis.ergonshowcase.domain.Cargo;
import br.com.praxis.ergonshowcase.domain.Orgao;
import br.com.praxis.ergonshowcase.domain.ServidorPessoa;
import br.com.praxis.ergonshowcase.domain.Setor;
import br.com.praxis.ergonshowcase.domain.Vinculo;
import br.com.praxis.ergonshowcase.rules.ErgonRuleEngineFacade.VinculoConcomitante;
import br.com.praxis.ergonshowcase.rules.ErgonRuleEngineFacade.VinculoRuleContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import org.praxisplatform.rules.facts.PraxisFactTreeBuilder;

/** Grafo ontológico de fatos de provimento, acumulação e ciclo de vida de {@code VINCULOS}. */
public final class VinculoProvimentoFacts {
  private VinculoProvimentoFacts() {}

  public static ObjectNode of(ObjectMapper mapper, VinculoRuleContext ctx) {
    Objects.requireNonNull(ctx, "ctx must not be null");
    Vinculo draft = ctx.draft();
    Integer empCodigo = draft != null && draft.getEmpCodigo() != null ? draft.getEmpCodigo() : 1;

    ObjectNode root = PraxisFactTreeBuilder.create()
        .putDraft(node -> populateDraft(node, draft, ctx))
        .putConcept("orgao", node -> populateOrgao(node, ctx.orgao()))
        .putConcept("pessoa", node -> populatePessoa(node, ctx.pessoa()))
        .putConcept("setor", node -> populateSetor(node, ctx.setor(), empCodigo))
        .putConcept("setorExercicio", node -> populateSetor(node, ctx.setorExercicio(), empCodigo))
        .putConcept("cargo", node -> populateCargo(node, ctx.cargo(), ctx.cargoOcupadas(), empCodigo))
        .putConcept(
            "cargoComissao",
            node -> populateCargo(node, ctx.cargoComissao(), ctx.cargoComissaoOcupadas(), empCodigo))
        .putConcept(
            "documentoLegal",
            node -> DependenteElegibilidadeFacts.populateDocumentoLegal(
                node,
                ctx.documentoLegal(),
                draft != null ? draft.getDocumentoLegalId() : null,
                empCodigo))
        .putConcept("tenant", node -> node.put("empCodigo", empCodigo))
        .putContext(node -> node
            .put("resourceKey", VinculoProvimentoRuleSet.RESOURCE_KEY)
            .put("ruleSetKey", VinculoProvimentoRuleSet.RULESET_KEY)
            .put("creating", ctx.creating()))
        .build();

    if (ctx.existingVinculos() != null) {
      ArrayNode existingArray = root.putArray("existingVinculos");
      for (VinculoConcomitante existing : ctx.existingVinculos()) {
        ObjectNode item = PraxisFactTreeBuilder.create()
            .put("inicio", existing.inicio())
            .put("fim", existing.fim())
            .put("categoria", existing.categoria())
            .build();
        if (existing.horarios() != null) {
          existing.horarios().writeTo(item.putObject("horarios"));
        }
        existingArray.add(item);
      }
    }

    return root;
  }

  private static void populateDraft(
      PraxisFactTreeBuilder draftNode, Vinculo draft, VinculoRuleContext ctx) {
    if (draft == null) {
      return;
    }
    Long numfunc = draft.getId() != null ? draft.getId().numfunc() : null;
    Integer numero = draft.getId() != null ? draft.getId().numero() : null;
    Integer contractDurationDays =
        draft.getDtinicontr() != null && draft.getDtfimcontr() != null
            ? Math.toIntExact(ChronoUnit.DAYS.between(draft.getDtinicontr(), draft.getDtfimcontr()))
            : null;
    Integer daysPosseToExercicio =
        draft.getDtposse() != null && draft.getDtexerc() != null
            ? Math.toIntExact(ChronoUnit.DAYS.between(draft.getDtposse(), draft.getDtexerc()))
            : null;
    LocalDate entryDate = draft.getDtposse() != null ? draft.getDtposse() : draft.getDtexerc();
    Integer ageAtIngressoYears = null;
    if (ctx.pessoa() != null && ctx.pessoa().getDtnasc() != null && entryDate != null) {
      ageAtIngressoYears =
          entryDate.isBefore(ctx.pessoa().getDtnasc())
              ? -1
              : Math.toIntExact(ChronoUnit.YEARS.between(ctx.pessoa().getDtnasc(), entryDate));
    }

    draftNode
        .put("id", numero)
        .put("empCodigo", draft.getEmpCodigo())
        .put("numfunc", numfunc)
        .put("numero", numero)
        .put("regimejur", blankToNull(draft.getRegimejur()))
        .put("tipovinc", blankToNull(draft.getTipovinc()))
        .put("categoria", blankToNull(draft.getCategoria()))
        .put("subcategoria", blankToNull(draft.getSubcategoria()))
        .put("situacao", blankToNull(draft.getSituacao()))
        .put("creating", ctx.creating())
        .put(
            "occupyingPosition",
            draft.getSituacao() != null
                && ErgonRuleEngineFacade.VINCULO_EM_EXERCICIO_OU_AFASTADO.contains(draft.getSituacao()))
        .put("dtnom", draft.getDtnom())
        .put("dtposse", draft.getDtposse())
        .put("dtexerc", draft.getDtexerc())
        .put("dtvac", draft.getDtvac())
        .put("formavac", blankToNull(draft.getFormavac()))
        .put("motivovac", blankToNull(draft.getMotivovac()))
        .put("dtaposent", draft.getDtaposent())
        .put("tipoapos", blankToNull(draft.getTipoapos()))
        .put("dtinicontr", draft.getDtinicontr())
        .put("dtfimcontr", draft.getDtfimcontr())
        .put("dtprorrogcontr", draft.getDtprorrogcontr())
        .put("contractDurationDays", contractDurationDays)
        .put("dtHomologEstagio", draft.getDtHomologEstagio())
        .put("dtinihomologestag", null)
        .put("dtfimhomologestag", null)
        .put("dtiniCessao", draft.getDtiniCessao())
        .put("dtfimCessao", draft.getDtfimCessao())
        .put("tipoReq", blankToNull(draft.getTipoReq()))
        .put("orgaoReq", blankToNull(draft.getOrgaoReq()))
        .put("dtinipagto", null)
        .put("dtfimpagto", null)
        .put("dtPgtoAte", draft.getDtPgtoAte())
        .put("jornadaSemanal", draft.getJornadaSemanal())
        .put("daysPosseToExercicio", daysPosseToExercicio)
        .put("ageAtIngressoYears", ageAtIngressoYears)
        .put("cargo", draft.getCargo())
        .put("cargoComissao", draft.getCargoComissao())
        .put("setor", blankToNull(draft.getSetor()))
        .put("setorExercicio", blankToNull(draft.getSetorExercicio()))
        .put("documentoLegalId", draft.getDocumentoLegalId())
        .put("openAfastamentosCount", ctx.openAfastamentosCount());
  }

  private static void populateOrgao(PraxisFactTreeBuilder node, Orgao orgao) {
    if (orgao == null) {
      node.put("present", false).put("ativa", false);
    } else {
      node.put("present", true).put("ativa", Boolean.TRUE.equals(orgao.getAtiva()));
    }
  }

  private static void populatePessoa(PraxisFactTreeBuilder node, ServidorPessoa pessoa) {
    if (pessoa == null) {
      node.put("present", false).put("id", null).put("dtnasc", null);
    } else {
      node.put("present", true).put("id", pessoa.getNumero()).put("dtnasc", pessoa.getDtnasc());
    }
  }

  private static void populateSetor(PraxisFactTreeBuilder node, Setor setor, Integer empCodigo) {
    node.put("present", setor != null)
        .put("sameOrgao", setor != null && Objects.equals(setor.getEmpCodigo(), empCodigo));
  }

  private static void populateCargo(
      PraxisFactTreeBuilder node, Cargo cargo, long ocupadas, Integer empCodigo) {
    if (cargo == null) {
      node.put("present", false)
          .put("sameOrgao", false)
          .put("extinct", false)
          .put("tipoCargo", null)
          .put("controleVaga", false)
          .put("vagasAutorizadas", null)
          .put("ocupadas", ocupadas);
      return;
    }
    node.put("present", true)
        .put("sameOrgao", Objects.equals(cargo.getEmpCodigo(), empCodigo))
        .put("extinct", cargo.getDtExtincao() != null)
        .put("tipoCargo", blankToNull(cargo.getTipoCargo()))
        .put("controleVaga", Boolean.TRUE.equals(cargo.getControleVaga()))
        .put("vagasAutorizadas", cargo.getVagasAutorizadas())
        .put("ocupadas", ocupadas);
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }
}
