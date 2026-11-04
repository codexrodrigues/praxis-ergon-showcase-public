package br.com.praxis.ergonshowcase.rules;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.rules.ChefiaRuleSetSupport.Rule;
import java.util.List;
import org.praxisplatform.rules.contract.RuleSetDefinition;

/** Política de manutenção cadastral de chefias. */
public final class VagaFuncaoRuleSet {
  private VagaFuncaoRuleSet() {}

  static final List<Rule> RULES = List.of(
      new Rule("ERG-01151", "dtfim",
          "O término da vaga não pode anteceder a abertura.",
          "PCK_VAGAS_FUNCAO.MAIN_PRE:47-55", "v_row_new.data_extincao <= max_dtfim (dtfim < dtini no cadastro da vaga)",
          """
          {"or":[{"==":[{"var":"draft.fim"},null]},{"and":[{"!=":[{"var":"draft.inicio"},null]},{"<=":[{"var":"draft.inicio"},{"var":"draft.fim"}]}]}]}
          """),
      new Rule("ERG-01152", "situacao",
          "A situação da vaga deve refletir a designação do titular.",
          "PCK_VAGAS_FUNCAO.MAIN_PRE:57-66", "SELECT COUNT(*) INTO v_count FROM designacoes_ev WHERE numero_vaga = v_row_new.numero (coerência entre situacao = OCUPADA e titularAtualId)",
          """
          {"==":[{"==":[{"var":"draft.situacao"},"OCUPADA"]},{"!=":[{"var":"current.titular"},null]}]}
          """),
      new Rule("ERG-01153", "funcaoId",
          "Uma vaga ocupada não pode mudar de função.",
          "PCK_VAGAS_FUNCAO.MAIN_PRE:57-66", "SELECT COUNT(*) INTO v_count FROM designacoes_ev WHERE numero_vaga = v_row_new.numero (vaga ocupada não pode alterar funcaoId)",
          """
          {"or":[{"==":[{"var":"current.titular"},null]},{"==":[{"var":"draft.funcao"},{"var":"current.funcao"}]}]}
          """),
      Rule.replaceable("ERG-01154", "funcaoId",
          "Uma vaga vigente exige função ativa.",
          "POLITICA_SHOWCASE.VAGA_FUNCAO_ATIVA", "A vaga vigente exige funcao.situacao = 'ATIVA'; a vaga extinta preserva sua função histórica.",
          """
          {"or":[{"==":[{"var":"draft.situacao"},"EXTINTA"]},{"==":[{"var":"funcao.situacao"},"ATIVA"]}]}
          """));

  static final Rule CUSTOMER =
      new Rule("CERG-07151", "documentoLegalId",
          "O Órgão 2 exige documento legal para abrir vaga.",
          "POLITICA_ORGAO_2.VAGA_ABERTURA", "A abertura da vaga exige documento legal.",
          """
          {"or":[{"!=":[{"var":"current.criacao"},true]},{"!=":[{"var":"draft.documentoLegalId"},null]}]}
          """);

  public static RuleSetDefinition productRuleSet() {
    return ChefiaRuleSetSupport.definition(ApiPaths.KEY_VAGAS_FUNCAO, RULES, CUSTOMER, false);
  }

  public static RuleSetDefinition orgao2CustomizedRuleSet() {
    return ChefiaRuleSetSupport.orgao2CustomizedRuleSet(ApiPaths.KEY_VAGAS_FUNCAO, RULES, CUSTOMER);
  }
}
