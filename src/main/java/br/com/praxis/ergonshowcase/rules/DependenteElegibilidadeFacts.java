package br.com.praxis.ergonshowcase.rules;

import br.com.praxis.ergonshowcase.domain.Dependente;
import br.com.praxis.ergonshowcase.domain.DocumentoLegal;
import br.com.praxis.ergonshowcase.domain.ServidorPessoa;
import br.com.praxis.ergonshowcase.domain.TipoDependente;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import org.praxisplatform.rules.facts.PraxisFactTreeBuilder;

/** Grafo ontológico de fatos de elegibilidade e cadastro de dependentes ({@code DEPENDENTES}). */
public final class DependenteElegibilidadeFacts {
  private DependenteElegibilidadeFacts() {}

  public static ObjectNode of(
      ObjectMapper mapper,
      ErgonRuleEngineFacade.DependenteRuleContext ctx,
      Integer empCodigo) {
    Objects.requireNonNull(ctx, "ctx must not be null");
    Dependente draft = ctx.draft();
    LocalDate refDate = ctx.referenceDate() != null ? ctx.referenceDate() : LocalDate.now();

    ObjectNode root = PraxisFactTreeBuilder.create()
        .putDraft(node -> populateDraft(node, draft, empCodigo, refDate))
        .putConcept("pessoa", node -> populatePessoa(node, ctx.pessoa()))
        .putConcept("tipoDependente", node -> populateTipoDependente(node, draft, ctx.tipoDependente()))
        .putConcept(
            "documentoLegal",
            node -> populateDocumentoLegal(
                node,
                ctx.documentoLegal(),
                draft != null ? draft.getDocumentoLegalId() : null,
                empCodigo))
        .putConcept("tenant", node -> node.put("empCodigo", empCodigo))
        .putContext(node -> node
            .put("resourceKey", DependenteElegibilidadeRuleSet.RESOURCE_KEY)
            .put("ruleSetKey", DependenteElegibilidadeRuleSet.RULESET_KEY)
            .put("referenceDate", refDate))
        .build();

    ArrayNode existingArray = mapper.createArrayNode();
    if (ctx.existingDependentes() != null) {
      for (Dependente existing : ctx.existingDependentes()) {
        if (existing != null) {
          Integer exNumdep = existing.getId() != null ? existing.getId().numdep() : null;
          Long exNumfunc = existing.getId() != null ? existing.getId().numfunc() : null;
          existingArray.add(
              PraxisFactTreeBuilder.create()
                  .put("id", exNumdep)
                  .put("numfunc", exNumfunc)
                  .put("numero", exNumdep)
                  .put("cpf", blankToNull(existing.getCpf()))
                  .put("dtini", existing.getDtini())
                  .put("dtfim", existing.getDtfim())
                  .build());
        }
      }
    }
    root.set("existingDependentes", existingArray);
    return root;
  }

  private static void populateDraft(
      PraxisFactTreeBuilder draftNode, Dependente draft, Integer empCodigo, LocalDate refDate) {
    if (draft == null) {
      return;
    }
    Long numfunc = draft.getId() != null ? draft.getId().numfunc() : null;
    Integer numdep = draft.getId() != null ? draft.getId().numdep() : null;
    String cleanCpf = blankToNull(draft.getCpf());
    draftNode
        .put("id", numdep)
        .put("empCodigo", empCodigo)
        .put("numfunc", numfunc)
        .put("numero", numdep)
        .put("numdep", numdep)
        .put("nome", draft.getNome())
        .put("dtnasc", draft.getDtnasc())
        .put("sexo", draft.getSexo())
        .put("cpf", cleanCpf)
        .put("cpfValidFormat", cleanCpf != null && cleanCpf.matches("\\d{11}"))
        .put("parentesco", blankToNull(draft.getParentesco()))
        .put("tipodepen", blankToNull(draft.getTipodepen()))
        .put("dtini", draft.getDtini())
        .put("dtfim", draft.getDtfim())
        .put("depIr", Boolean.TRUE.equals(draft.getDepIr()))
        .put("depSalarioFamilia", Boolean.TRUE.equals(draft.getDepSalarioFamilia()))
        .put("depPlanoSaude", Boolean.TRUE.equals(draft.getDepPlanoSaude()))
        .put("estudante", Boolean.TRUE.equals(draft.getEstudante()))
        .put("universitario", Boolean.TRUE.equals(draft.getUniversitario()))
        .put("invalido", Boolean.TRUE.equals(draft.getInvalido()))
        .put("excepcional", Boolean.TRUE.equals(draft.getExcepcional()))
        .put("representanteLegal", Boolean.TRUE.equals(draft.getRepresentanteLegal()))
        .put("documentoLegalId", draft.getDocumentoLegalId())
        .put("numrg", blankToNull(draft.getNumrg()))
        .put("tiporg", blankToNull(draft.getTiporg()))
        .put("orgaorg", blankToNull(draft.getOrgaorg()))
        .put("ufrg", blankToNull(draft.getUfrg()))
        .put("expedrg", draft.getExpedrg())
        .put("numCert", blankToNull(draft.getNumCert()))
        .put("livroCert", blankToNull(draft.getLivroACert()))
        .put("folhaCert", blankToNull(draft.getFolhaCert()))
        .put("cartorioCert", blankToNull(draft.getCartorioCert()))
        .put("tipodocCert", blankToNull(draft.getTipodocCert()))
        .put("referenceDate", refDate);
    if (draft.getDtnasc() != null) {
      int age =
          draft.getDtnasc().isAfter(refDate)
              ? -1
              : Math.toIntExact(ChronoUnit.YEARS.between(draft.getDtnasc(), refDate));
      draftNode.put("ageYears", age);
    } else {
      draftNode.put("ageYears", null);
    }
  }

  private static void populatePessoa(PraxisFactTreeBuilder pessoaNode, ServidorPessoa pessoa) {
    if (pessoa == null) {
      pessoaNode
          .put("present", false)
          .put("id", null)
          .put("cpf", null)
          .put("dtnasc", null)
          .put("sexo", null);
    } else {
      pessoaNode
          .put("present", true)
          .put("id", pessoa.getNumero())
          .put("cpf", blankToNull(pessoa.getCpf()))
          .put("dtnasc", pessoa.getDtnasc())
          .put("sexo", pessoa.getSexo());
    }
  }

  private static void populateTipoDependente(
      PraxisFactTreeBuilder tipoNode, Dependente draft, TipoDependente tipo) {
    if (tipo == null) {
      tipoNode
          .put("present", false)
          .put("codigo", null)
          .put("geraIr", false)
          .put("geraSalarioFamilia", false)
          .put("effectiveMaxAge", null);
    } else {
      Integer effectiveMaxAge =
          draft != null
                  && Boolean.TRUE.equals(draft.getUniversitario())
                  && tipo.getIdadeLimite() != null
                  && tipo.getIdadeLimite() < 24
              ? Integer.valueOf(24)
              : tipo.getIdadeLimite();
      tipoNode
          .put("present", true)
          .put("codigo", tipo.getTipodepen())
          .put("geraIr", Boolean.TRUE.equals(tipo.getGeraIr()))
          .put("geraSalarioFamilia", Boolean.TRUE.equals(tipo.getGeraSalarioFamilia()))
          .put("effectiveMaxAge", effectiveMaxAge);
    }
  }

  public static void populateDocumentoLegal(
      PraxisFactTreeBuilder node,
      DocumentoLegal doc,
      Long documentoLegalId,
      Integer expectedEmpCodigo) {
    if (doc == null) {
      node.put("present", false)
          .put("id", documentoLegalId)
          .put("empCodigo", null)
          .put("sameOrgao", documentoLegalId != null)
          .put("tipoDocumento", null);
      return;
    }
    node.put("present", true)
        .put("id", doc.getId())
        .put("empCodigo", doc.getEmpCodigo())
        .put("sameOrgao", expectedEmpCodigo == null || Objects.equals(doc.getEmpCodigo(), expectedEmpCodigo))
        .put("tipoDocumento", doc.getTipoDocumento());
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }
}
