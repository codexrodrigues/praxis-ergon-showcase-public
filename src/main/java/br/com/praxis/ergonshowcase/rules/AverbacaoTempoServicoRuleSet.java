package br.com.praxis.ergonshowcase.rules;

import br.com.praxis.ergonshowcase.rules.CadastroPessoalRuleSetSupport.Rule;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.praxisplatform.rules.contract.RuleSetDefinition;

/** Decisões governadas de averbacoes tempo servico. */
public final class AverbacaoTempoServicoRuleSet {
  static final String RESOURCE = "administracao-pessoal.averbacoes-tempo-servico";

  private AverbacaoTempoServicoRuleSet() {}

  static final List<Rule> PRODUCT = List.of(
      new Rule("ERG-00006", "dtfim",
          "Término da averbação anterior ao início.",
          "PCK_AVERBACOES_CONTA.MAIN_PRE:118-122", "PCK_AVERBACOES_CONTA.MAIN_PRE:118-122",
          """
          {"and":[{"!=":[{"var":"draft.fim"},null]},{"<=":[{"var":"draft.inicio"},{"var":"draft.fim"}]}]}
          """),
      new Rule("CERG-06102", "dataAContar",
          "Data a contar anterior ao início da averbação.",
          "POLITICA_SHOWCASE.DATA_A_CONTAR", "Data a contar não antecede o início certificado.",
          """
          {"or":[{"==":[{"var":"draft.inicio"},null]},{"==":[{"var":"draft.dataAContar"},null]},{"<=":[{"var":"draft.inicio"},{"var":"draft.dataAContar"}]}]}
          """),
      new Rule("CERG-06103", "totalDias",
          "Dias reconhecidos devem ser positivos e caber na certidão.",
          "PCK_AVERBACOES_CONTA.MAIN_PRE:89-107", "PCK_AVERBACOES_CONTA.MAIN_PRE:89-107",
          """
          {"and":[{"!=":[{"var":"draft.totalDias"},null]},{"!=":[{"var":"draft.diasCorridos"},null]},{">":[{"var":"draft.totalDias"},0]},{"<=":[{"var":"draft.totalDias"},{"var":"draft.diasCorridos"}]}]}
          """),
      new Rule("CERG-06104", "diasFinalidade",
          "Dias da finalidade excedem o tempo reconhecido.",
          "PCK_AVERB_OQUE_CONTA.MAIN_PRE:53-62", "PCK_AVERB_OQUE_CONTA.MAIN_PRE:53-62",
          """
          {"and":[{"!=":[{"var":"draft.diasFinalidade"},null]},{"!=":[{"var":"draft.totalDias"},null]},{">=":[{"var":"draft.diasFinalidade"},0]},{"<=":[{"var":"draft.diasFinalidade"},{"var":"draft.totalDias"}]}]}
          """),
      new Rule("CERG-06105", "diasDesaverbados",
          "Dias desaverbados fora do saldo da finalidade.",
          "PCK_DESAVERBACOES_CONTA.MAIN_PRE:71-95", "PCK_DESAVERBACOES_CONTA.MAIN_PRE:71-95",
          """
          {"and":[{"!=":[{"var":"draft.diasDesaverbados"},null]},{"!=":[{"var":"draft.diasFinalidade"},null]},{">=":[{"var":"draft.diasDesaverbados"},0]},{"<=":[{"var":"draft.diasDesaverbados"},{"var":"draft.diasFinalidade"}]}]}
          """),
      Rule.replaceable("CERG-06106", "complemento",
          "Informe o período do complemento como DDMMYYYY-DDMMYYYY, com datas válidas e em ordem.",
          "PCK_AVERB_OQUE_CONTA.MAIN_PRE:53-60 (Tarefa 13239)", "PCK_AVERB_OQUE_CONTA.MAIN_PRE:53-60 (Tarefa 13239)",
          """
          {"or":[{"!=":[{"var":"draft.finalidade"},"LICENCA_ESPECIAL"]},{"and":[{"!=":[{"var":"draft.complementoInicio"},null]},{"!=":[{"var":"draft.complementoFim"},null]},{"<=":[{"var":"draft.complementoInicio"},{"var":"draft.complementoFim"}]}]}]}
          """),
      Rule.replaceable("CERG-06107", "dtfim",
          "A averbação alcança o exercício do próprio vínculo.",
          "POLITICA_SHOWCASE.AVERBACAO_EXERCICIO", "Tempo externo só alcança o exercício quando a concomitância é admitida.",
          """
          {"or":[{"==":[{"var":"draft.sobrepoe"},true]},{"==":[{"var":"vinculo.exercicio"},null]},{"==":[{"var":"draft.fim"},null]},{"<":[{"var":"draft.fim"},{"var":"vinculo.exercicio"}]}]}
          """),
      Rule.replaceable("CERG-06108", "dtini",
          "Já existe averbação ativa da mesma finalidade no intervalo.",
          "POLITICA_SHOWCASE.AVERBACAO_CONCOMITANCIA", "POLITICA_SHOWCASE.AVERBACAO_CONCOMITANCIA",
          """
          {"or":[{"==":[{"var":"draft.sobrepoe"},true]},{"==":[{"var":"neighborhood.sobreposicoes"},0]}]}
          """),
      Rule.replaceable("ERG-02202", "dataAContar", "Data a contar anterior ao exercício do vínculo.",
          "PCK_AVERBACOES_CONTA.MAIN_PRE:135-138",
          "IF V_ROW_NEW.data_a_contar < v_dtexerc THEN trata_erro(2202).",
          """
          {"or":[{"==":[{"var":"draft.dataAContar"},null]},
                 {"==":[{"var":"vinculo.exercicio"},null]},
                 {">=":[{"var":"draft.dataAContar"},{"var":"vinculo.exercicio"}]}]}
          """),
      new Rule("CERG-06109", "diasFinalidade",
          "Direito de licença por averbação excede o limite.",
          "PCK_AVERB_OQUE_CONTA:150-156", "PCK_AVERB_OQUE_CONTA:150-156",
          """
          {"or":[{"!=":[{"var":"draft.finalidade"},"LICENCA_ESPECIAL"]},{"<=":[{"var":"draft.diasFinalidade"},999]}]}
          """),
      new Rule("CERG-06110", "diasDesaverbados",
          "Regularize o consumo do período gerado antes de alterar a averbação.",
          "PCK_PERAQLICESP.MAIN_PRE:120-130", "PCK_PERAQLICESP.MAIN_PRE:120-130",
          """
          {"==":[{"var":"neighborhood.consumoPeriodosGerados"},0]}
          """));

  static final List<Rule> ACTIONS = List.of(
      new Rule("CERG-06112", "dias", "Informe os dias da desaverbação ou reversão.",
          "PCK_DESAVERBACOES_CONTA.MAIN_PRE:71-75", "PCK_DESAVERBACOES_CONTA.MAIN_PRE:71-75",
          """
          {"or":[{"and":[{"!=":[{"var":"draft.acao"},"registrar-desaverbacao"]},{"!=":[{"var":"draft.acao"},"reverter-desaverbacao"]}]},{"and":[{"!=":[{"var":"draft.diasAto"},null]},{">":[{"var":"draft.diasAto"},0]}]}]}
          """),
      new Rule("CERG-06113", "dias", "Desaverbação excede o tempo destinado à finalidade.",
          "PCK_DESAVERBACOES_CONTA.MAIN_PRE:83-115", "PCK_DESAVERBACOES_CONTA.MAIN_PRE:83-115",
          """
          {"or":[{"and":[{"!=":[{"var":"draft.acao"},"registrar-desaverbacao"]}]},{"==":[{"var":"draft.diasAto"},null]},{"<=":[{"+":[{"var":"draft.diasDesaverbados"},{"var":"draft.diasAto"}]},{"var":"draft.diasFinalidade"}]}]}
          """),
      new Rule("CERG-06114", "dias", "Reversão excede os dias desaverbados.",
          "POLITICA_SHOWCASE.REVERSAO_DESAVERBACAO", "POLITICA_SHOWCASE.REVERSAO_DESAVERBACAO",
          """
          {"or":[{"and":[{"!=":[{"var":"draft.acao"},"reverter-desaverbacao"]}]},{"==":[{"var":"draft.diasAto"},null]},{"<=":[{"var":"draft.diasAto"},{"var":"draft.diasDesaverbados"}]}]}
          """),
      new Rule("CERG-06115", "dias", "Período gerado já possui consumo registrado.",
          "PCK_PERAQLICESP.MAIN_PRE:120-130", "PCK_PERAQLICESP.MAIN_PRE:120-130",
          """
          {"or":[{"and":[{"!=":[{"var":"draft.acao"},"registrar-desaverbacao"]},{"!=":[{"var":"draft.acao"},"reverter-desaverbacao"]},{"!=":[{"var":"draft.acao"},"cancelar-averbacao"]},{"!=":[{"var":"draft.acao"},"homologar-averbacao"]}]},{"==":[{"var":"neighborhood.consumoPeriodosGerados"},0]}]}
          """));

  static final Rule CUSTOMER =
      new Rule("CERG-06111", "documentoLegalId",
          "Órgão 2 exige documento em averbação RGPS acima de 365 dias.",
          "POLITICA_ORGAO_2.AVERBACAO_RGPS", "POLITICA_ORGAO_2.AVERBACAO_RGPS",
          """
          {"or":[{"!=":[{"var":"draft.regimePrevidenciario"},"RGPS"]},{"<=":[{"var":"draft.totalDias"},365]},{"!=":[{"var":"draft.documentoLegalId"},null]}]}
          """);

  public static RuleSetDefinition productRuleSet() {
    return definition(false);
  }

  public static RuleSetDefinition orgao2CustomizedRuleSet() {
    return CadastroPessoalRuleSetSupport.parameterize(definition(true), Map.of("CERG-06109", 180));
  }

  private static RuleSetDefinition definition(boolean orgao2) {
    Rule parameterized = PRODUCT.stream().filter(rule -> rule.code().equals("CERG-06109")).findFirst().orElseThrow();
    var bounds = CadastroPessoalRuleSetSupport.integerTemplate(
        CadastroPessoalRuleSetSupport.somenteCadastro(parameterized), "ergon:averbacoes-tempo-servico.limite",
        "limite", 999, 999, "/or/1/or/1/<=/1", "dias");
    List<Rule> rules = Stream.concat(ACTIONS.stream(),
        PRODUCT.stream().map(CadastroPessoalRuleSetSupport::somenteCadastro)).toList();
    return CadastroPessoalRuleSetSupport.definition(RESOURCE, rules,
        CadastroPessoalRuleSetSupport.somenteCadastro(CUSTOMER), orgao2,
        Map.of("CERG-06109", bounds));
  }
}
