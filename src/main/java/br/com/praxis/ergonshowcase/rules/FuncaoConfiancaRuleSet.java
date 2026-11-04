package br.com.praxis.ergonshowcase.rules;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.rules.ChefiaRuleSetSupport.Rule;
import java.util.List;
import org.praxisplatform.rules.contract.RuleSetDefinition;

/** Política de manutenção cadastral de chefias. */
public final class FuncaoConfiancaRuleSet {
  private FuncaoConfiancaRuleSet() {}

  static final List<Rule> RULES = List.of(
      new Rule("ERG-01101", "dtfim",
          "O término da função não pode anteceder o início.",
          "PCK_HIST_FUNCAO_EV.MAIN_PRE:77-81", "V_ROW_NEW.dtfim IS NOT NULL AND V_ROW_NEW.dtfim < V_ROW_NEW.dtini",
          """
          {"or":[{"==":[{"var":"draft.fim"},null]},{"and":[{"!=":[{"var":"draft.inicio"},null]},{"<=":[{"var":"draft.inicio"},{"var":"draft.fim"}]}]}]}
          """),
      Rule.replaceable("ERG-01102", "diasMinimosSubstituicao",
          "Função que admite substituição exige mínimo de um dia.",
          "HIST_FUNCAO_EV.CKC_HFUNCAO_PERMITE_SUBST:12-14", "PERMITE_SUBST = 'S' exige diasMinimosSubstituicao >= 1 no cadastro da função",
          """
          {"or":[{"!=":[{"var":"draft.permiteSubstituicao"},true]},{"and":[{"!=":[{"var":"draft.minimo"},null]},{">=":[{"var":"draft.minimo"},1]}]}]}
          """),
      Rule.replaceable("CERG-07101", "documentoLegalId",
          "A extinção da função exige documento legal.",
          "POLITICA_SHOWCASE.FUNCAO_EXTINCAO", "A situação EXTINTA requer lastro documental (documentoLegalId != null).",
          """
          {"or":[{"!=":[{"var":"draft.situacao"},"EXTINTA"]},{"!=":[{"var":"draft.documentoLegalId"},null]}]}
          """));

  static final Rule CUSTOMER =
      new Rule("CERG-07102", "atoPublicacaoId",
          "O Órgão 2 exige ato de publicação na extinção da função.",
          "POLITICA_ORGAO_2.FUNCAO_EXTINCAO", "A extinção deve registrar a publicação oficial.",
          """
          {"or":[{"!=":[{"var":"draft.situacao"},"EXTINTA"]},{"!=":[{"var":"draft.atoPublicacaoId"},null]}]}
          """);

  public static RuleSetDefinition productRuleSet() {
    return ChefiaRuleSetSupport.definition(ApiPaths.KEY_FUNCOES, RULES, CUSTOMER, false);
  }

  public static RuleSetDefinition orgao2CustomizedRuleSet() {
    return ChefiaRuleSetSupport.orgao2CustomizedRuleSet(ApiPaths.KEY_FUNCOES, RULES, CUSTOMER);
  }
}
