package br.com.praxis.ergonshowcase.rules;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import java.util.List;
import java.util.Map;
import org.praxisplatform.rules.contract.RuleSetDefinition;

/** Decisões de substituição funcional avaliadas sobre o grafo ontológico do órgão. */
public final class SubstituicaoFuncaoRuleSet {
  private SubstituicaoFuncaoRuleSet() {}

  private static final String MINIMO_REMUNERADO = "CERG-07001";
  private static final int PISO_PRODUTO = 15;
  private static final int PISO_ORGAO_2 = 10;

  static final List<ChefiaRuleSetSupport.Rule> RULES = List.of(
      new ChefiaRuleSetSupport.Rule("ERG-01300", "dtfim",
          "O fim da substituição deve ser igual ou posterior ao início.",
          "PCK_SUBSTITUICOES_EV.VERIFICA_DATAS_SUBSTITUICAO:166", "p_subst.dtfim < p_subst.dtini",
          """
          {">=":[{"var":"draft.fim"},{"var":"draft.inicio"}]}
          """),
      new ChefiaRuleSetSupport.Rule("ERG-01301", "substitutoId",
          "O substituto deve ser uma pessoa diferente do titular.",
          "PCK_SUBSTITUICOES_EV.VERIFICA_DATAS_SUBSTITUICAO:162", "p_subst.numfunc = p_subst.numfuncsubs",
          """
          {"!==":[{"var":"draft.substituto"},{"var":"designacao.titular"}]}
          """),
      new ChefiaRuleSetSupport.Rule("ERG-01302", "dtiniRemunerada",
          "Informe início e fim da remuneração em ordem cronológica.",
          "PCK_SUBSTITUICOES_EV.VERIFICA_DATAS_SUBSTITUICAO:174", "p_subst.dtfimrem < p_subst.dtinirem OR p_subst.dtinirem IS NULL",
          """
          {"or":[{"and":[{"===":[{"var":"draft.inicioRemunerado"},null]},{"===":[{"var":"draft.fimRemunerado"},null]}]},{"and":[{"!==":[{"var":"draft.inicioRemunerado"},null]},{"!==":[{"var":"draft.fimRemunerado"},null]},{">=":[{"var":"draft.fimRemunerado"},{"var":"draft.inicioRemunerado"}]}]}]}
          """),
      new ChefiaRuleSetSupport.Rule("ERG-01303", "dtfimRemunerada",
          "O intervalo remunerado deve caber no período da substituição.",
          "PCK_SUBSTITUICOES_EV.VERIFICA_DATAS_SUBSTITUICAO:179", "NOT (p_subst.dtinirem >= p_subst.dtini AND (NVL(p_subst.dtfimrem, p_subst.dtfim + 1) <= p_subst.dtfim OR p_subst.dtfim IS NULL))",
          """
          {"or":[{"===":[{"var":"draft.inicioRemunerado"},null]},{"===":[{"var":"draft.fimRemunerado"},null]},{"and":[{">=":[{"var":"draft.inicioRemunerado"},{"var":"draft.inicio"}]},{"<=":[{"var":"draft.fimRemunerado"},{"var":"draft.fim"}]}]}]}
          """),
      new ChefiaRuleSetSupport.Rule("ERG-01304", "substitutoId",
          "O vínculo do substituto deve estar em exercício no início da substituição.",
          "PCK_SUBSTITUICOES_EV.VERIFICA_FUNCIONARIO_ATIVO:57", "P_DTINI < v_dtexerc",
          """
          {"and":[{"!==":[{"var":"substituto.exercicio"},null]},{">=":[{"var":"draft.inicio"},{"var":"substituto.exercicio"}]}]}
          """),
      new ChefiaRuleSetSupport.Rule("ERG-01305", "substitutoId",
          "O período de substituição não pode ultrapassar a vacância do vínculo.",
          "PCK_SUBSTITUICOES_EV.VERIFICA_FUNCIONARIO_ATIVO:61", "((v_dtvac IS NOT NULL) AND (P_DTFIM IS NULL)) OR (P_DTFIM > v_dtvac)",
          """
          {"or":[{"===":[{"var":"substituto.vacancia"},null]},{"<=":[{"var":"draft.fim"},{"var":"substituto.vacancia"}]}]}
          """),
      new ChefiaRuleSetSupport.Rule("ERG-01306", "substitutoId",
          "O substituto não pode estar cedido no período da substituição.",
          "PCK_SUBSTITUICOES_EV.VERIFICA_CESSAO_SUBSTITUTO:85", "tot > 0 (cessões concomitantes do substituto)",
          """
          {"or":[{"===":[{"var":"substituto.inicioCessao"},null]},{"or":[{">":[{"var":"substituto.inicioCessao"},{"var":"draft.fim"}]},{"and":[{"!==":[{"var":"substituto.fimCessao"},null]},{"<":[{"var":"substituto.fimCessao"},{"var":"draft.inicio"}]}]}]}]}
          """),
      new ChefiaRuleSetSupport.Rule("ERG-01308", "substitutoId",
          "O substituto não pode estar em gozo de férias no período da substituição.",
          "PCK_SUBSTITUICOES_EV.VERIFICA_FERIAS_SUBSTITUTO:131", "v_cont > 0 (férias concomitantes do vínculo substituto no período)",
          """
          {"===":[{"var":"neighborhood.feriasSubstituto"},0]}
          """),
      new ChefiaRuleSetSupport.Rule("ERG-01309", "substitutoId",
          "O substituto não pode estar em licença ou afastamento no período da substituição.",
          "PCK_SUBSTITUICOES_EV.VERIFICA_LIC_AFAST_SUBSTITUTO:342", "v_cont > 0 (licenças ou afastamentos concomitantes do vínculo substituto)",
          """
          {"===":[{"var":"neighborhood.afastamentosSubstituto"},0]}
          """),
      new ChefiaRuleSetSupport.Rule("ERG-01311", "afastamentoId",
          "O impedimento do titular deve cobrir todo o período da substituição.",
          "PCK_SUBSTITUICOES_EV.VERIFICA_TITULAR_AFASTADO:263", "v_tot_dias < NVL(p_subst.dtfim, PACK_ERGON.DATA_MAXIMA) - p_subst.dtini + 1",
          """
          {"and":[
            {"!==":[{"var":"impedimento.inicio"},null]},
            {"!==":[{"var":"impedimento.fim"},null]},
            {"<=":[{"var":"impedimento.inicio"},{"var":"draft.inicio"}]},
            {">=":[{"var":"impedimento.fim"},{"var":"draft.fim"}]},
            {"or":[
              {"and":[{"===":[{"var":"impedimento.tipo"},"AFASTAMENTO"]},{"===":[{"var":"impedimento.situacao"},"HOMOLOGADO"]}]},
              {"and":[{"===":[{"var":"impedimento.tipo"},"FERIAS"]},{"in":[{"var":"impedimento.situacao"},["HOMOLOGADA","EM_GOZO","GOZADA","INTERROMPIDA"]]},{"or":[{"===":[{"var":"impedimento.interrupcao"},null]},{">":[{"var":"impedimento.interrupcao"},{"var":"draft.fim"}]}]}]}
            ]}
          ]}
          """),
      new ChefiaRuleSetSupport.Rule("ERG-01315", "designacaoId",
          "A função do titular não permite substituição.",
          "PCK_SUBSTITUICOES_EV.VERIFICA_HISTORICO_FUNCAO:643", "NVL(rec_hfun.permite_subst,'N') = 'N'",
          """
          {"===":[{"var":"funcao.permiteSubstituicao"},true]}
          """),
      new ChefiaRuleSetSupport.Rule("ERG-01319", "designacaoId",
          "A designação do titular deve estar vigente durante a substituição.",
          "PCK_SUBSTITUICOES_EV.PEGA_DTFIM_FUNCAO_DESIGNACAO:523,553", "v_funcao_sa IS NULL AND v_funcao_d IS NULL ou ausência de concomitância com a designação do titular",
          """
          {"and":[{"===":[{"var":"designacao.situacao"},"EM_EXERCICIO"]},{"!==":[{"var":"designacao.inicio"},null]},{"<=":[{"var":"designacao.inicio"},{"var":"draft.inicio"}]},{"or":[{"===":[{"var":"designacao.fim"},null]},{">=":[{"var":"designacao.fim"},{"var":"draft.fim"}]}]}]}
          """),
      new ChefiaRuleSetSupport.Rule("ERG-01323", "dtini",
          "Já existe exercício concomitante para o vínculo substituto.",
          "PCK_SUBSTITUICOES_EV.VALIDA_CONCOMITANCIA:578", "v_cont > 1 (substituições concomitantes por numfunc e numvinc)",
          """
          {"===":[{"var":"neighborhood.outrasSubstituicoesSubstituto"},0]}
          """),
      new ChefiaRuleSetSupport.Rule(MINIMO_REMUNERADO, "dtiniRemunerada",
          "O exercício interino deve atingir o mínimo de dias da função e do órgão para ser remunerado.",
          "POLITICA_FUNCIONAL_SLICE_7", "S7-F3: mínimo de exercício para elegibilidade remuneratória",
          """
          {"or":[
            {"===":[{"var":"draft.inicioRemunerado"},null]},
            {">=":[{"+":[{"-":[{"var":"draft.fim"},{"var":"draft.inicio"}]},1]},
                   {"max":[{"if":[{"==":[{"var":"funcao.diasMinimosSubstituicao"},null]},0,
                                  {"var":"funcao.diasMinimosSubstituicao"}]},%d]}]}
          ]}
          """.formatted(PISO_PRODUTO)),
      new ChefiaRuleSetSupport.Rule("ERG-01324", "dtini",
          "Já existe substituição concomitante para esta designação.",
          "PCK_SUBSTITUICOES_EV.VALIDA_TITULAR_JA_SUBSTITUIDO:615", "v_cont > 1 (concomitâncias por numfuncsubs, numvincsubs e dtinidesigsubs)",
          """
          {"===":[{"var":"neighborhood.outrasSubstituicoesTitular"},0]}
          """));

  static final ChefiaRuleSetSupport.Rule CUSTOMER = new ChefiaRuleSetSupport.Rule(
      "CERG-07171", "documentoLegalId", "O Órgão 2 exige documento legal na substituição remunerada.",
      "POLITICA_ORGAO_2.SUBSTITUICAOFUNCAO", "O Órgão 2 exige documento legal na substituição remunerada.",
      """
      {"or":[{"==":[{"var":"draft.inicioRemunerado"},null]},
             {"!=":[{"var":"draft.documentoLegalId"},null]}]}
      """);

  public static RuleSetDefinition productRuleSet() {
    return definition(false);
  }

  public static RuleSetDefinition orgao2CustomizedRuleSet() {
    return CadastroPessoalRuleSetSupport.parameterize(definition(true),
        Map.of(MINIMO_REMUNERADO, PISO_ORGAO_2));
  }

  private static RuleSetDefinition definition(boolean orgao2) {
    var rule = RULES.stream().filter(candidate -> MINIMO_REMUNERADO.equals(candidate.code()))
        .findFirst().orElseThrow();
    var template = CadastroPessoalRuleSetSupport.integerTemplate(rule.code(), rule.expression(),
        "ergon:substituicoes.diasMinimosRemunerados", "diasMinimosRemunerados",
        PISO_PRODUTO, 999, "/or/1/>=/1/max/1", "dias");
    return ChefiaRuleSetSupport.definition(ApiPaths.KEY_SUBSTITUICOES, RULES, CUSTOMER, orgao2,
        Map.of(MINIMO_REMUNERADO, template));
  }
}
