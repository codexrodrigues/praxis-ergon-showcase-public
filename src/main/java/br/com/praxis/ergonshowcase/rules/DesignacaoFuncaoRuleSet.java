package br.com.praxis.ergonshowcase.rules;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import java.util.List;
import org.praxisplatform.rules.contract.RuleSetDefinition;

/** Regras de indicação de substitutos eventuais no ato de designação. */
public final class DesignacaoFuncaoRuleSet {
  private DesignacaoFuncaoRuleSet() {}

  static final List<ChefiaRuleSetSupport.Rule> RULES = List.of(
      new ChefiaRuleSetSupport.Rule("ERG-01200", "dtfim",
          "O término da designação não pode anteceder o início.",
          "PCK_DESIGNACOES_EV.VALID_DESIGNACOES:254-256,274-276",
          "p_desig.DTINI > p_desig.DTFIM",
          """
          {"or":[
            {"===":[{"var":"draft.fim"},null]},
            {"and":[
              {"!==":[{"var":"draft.inicio"},null]},
              {"<=":[{"var":"draft.inicio"},{"var":"draft.fim"}]}
            ]}
          ]}
          """),
      new ChefiaRuleSetSupport.Rule("ERG-01203", "substituto1Id",
          "O titular não pode ser indicado como seu próprio substituto.",
          "PCK_DESIGNACOES_EV.VALID_DESIGNACOES:287-291",
          "NUMFUNCSUBS1 IS NOT NULL AND (NUMFUNCSUBS1 = NUMFUNC OR NUMFUNCSUBS2 = NUMFUNC)",
          """
          {"or":[
            {"===":[{"var":"draft.primeiro"},null]},
            {"and":[
              {"!==":[{"var":"draft.primeiro"},{"var":"draft.titular"}]},
              {"!==":[{"var":"draft.segundo"},{"var":"draft.titular"}]}
            ]}
          ]}
          """),
      new ChefiaRuleSetSupport.Rule("ERG-01204", "substituto2Id",
          "O primeiro e o segundo substituto devem ser pessoas diferentes.",
          "PCK_DESIGNACOES_EV.VALID_DESIGNACOES:293-298",
          "NUMFUNCSUBS2 IS NOT NULL AND NUMFUNCSUBS2 = NUMFUNCSUBS1",
          """
          {"or":[
            {"===":[{"var":"draft.segundo"},null]},
            {"!==":[{"var":"draft.segundo"},{"var":"draft.primeiro"}]}
          ]}
          """),
      new ChefiaRuleSetSupport.Rule("ERG-01205", "substituto1Id",
          "Indique o primeiro substituto antes de indicar o segundo.",
          "PCK_DESIGNACOES_EV.VALID_DESIGNACOES:301-303",
          "ELSIF NUMFUNCSUBS2 IS NOT NULL (NUMFUNCSUBS1 IS NULL)",
          """
          {"or":[
            {"===":[{"var":"draft.segundo"},null]},
            {"!==":[{"var":"draft.primeiro"},null]}
          ]}
          """),
      new ChefiaRuleSetSupport.Rule("CERG-07002", "vagaFuncaoId",
          "A vaga já possui outro titular em exercício.",
          "VALID_EXCL_VAGA_FUN:423-449", "count_desig > 0 (uma posição numerada só pode ter um titular em exercício no período)",
          """
          {"or":[
            {"!==":[{"var":"draft.situacao"},"EM_EXERCICIO"]},
            {"===":[{"var":"neighborhood.outrosTitulares"},0]}
          ]}
          """),
      new ChefiaRuleSetSupport.Rule("CERG-07003", "situacao",
          "A homologação coletiva exige uma designação aguardando exercício.",
          "POLITICA_FUNCIONAL_SLICE_7", "Origem DESIGNADA para homologação coletiva.",
          """
          {"or":[
            {"===":[{"var":"current.situacao"},null]},
            {"===":[{"var":"current.situacao"},"DESIGNADA"]}
          ]}
          """),
      new ChefiaRuleSetSupport.Rule("CERG-07004", "vagaFuncaoId",
          "O exercício exige vaga da mesma função, livre ou já ocupada pelo próprio ato.",
          "VALID_CONTROLE_VAGA:200-206", "hvagcar.cargo <> v_cargo e disponibilidade da posição numerada",
          """
          {"or":[
            {"!==":[{"var":"draft.situacao"},"EM_EXERCICIO"]},
            {"===":[{"var":"draft.vagaId"},null]},
            {"and":[
              {"===":[{"var":"draft.funcao"},{"var":"vaga.funcao"}]},
              {"in":[{"var":"vaga.situacao"},["LIVRE","OCUPADA"]]}
            ]}
          ]}
          """),
      ChefiaRuleSetSupport.Rule.replaceable("CERG-07005", "dtExercicio",
          "A data de exercício deve estar dentro da vigência da designação.",
          "PCK_DESIGNACOES_EV.VALID_DESIGNACOES:246-248",
          "Política do ato de exercício no host, com intervalo inclusivo da designação.",
          """
          {"or":[
            {"!==":[{"var":"draft.situacao"},"EM_EXERCICIO"]},
            {"and":[
              {"!==":[{"var":"draft.inicio"},null]},
              {"!==":[{"var":"draft.exercicio"},null]},
              {">=":[{"var":"draft.exercicio"},{"var":"draft.inicio"}]},
              {"or":[{"===":[{"var":"draft.fim"},null]},
                     {"<=":[{"var":"draft.exercicio"},{"var":"draft.fim"}]}]}
            ]}
          ]}
          """));

  static final ChefiaRuleSetSupport.Rule CUSTOMER = new ChefiaRuleSetSupport.Rule(
      "CERG-07161", "atoPublicacaoId", "O Órgão 2 exige publicação para iniciar exercício.",
      "POLITICA_ORGAO_2.DESIGNACAOFUNCAO", "O Órgão 2 exige publicação para iniciar exercício.",
      """
      {"or":[{"!=":[{"var":"draft.situacao"},"EM_EXERCICIO"]},
             {"!=":[{"var":"draft.atoPublicacaoId"},null]}]}
      """);

  public static RuleSetDefinition productRuleSet() {
    return ChefiaRuleSetSupport.definition(ApiPaths.KEY_DESIGNACOES, RULES, CUSTOMER, false);
  }

  public static RuleSetDefinition orgao2CustomizedRuleSet() {
    return ChefiaRuleSetSupport.orgao2CustomizedRuleSet(ApiPaths.KEY_DESIGNACOES, RULES, CUSTOMER);
  }
}
