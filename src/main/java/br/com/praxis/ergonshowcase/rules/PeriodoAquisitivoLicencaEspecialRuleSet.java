package br.com.praxis.ergonshowcase.rules;

import br.com.praxis.ergonshowcase.rules.CadastroPessoalRuleSetSupport.Rule;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.praxisplatform.rules.contract.RuleSetDefinition;

/** Decisões governadas de periodos aquisitivos licenca especial. */
public final class PeriodoAquisitivoLicencaEspecialRuleSet {
  static final String RESOURCE = "administracao-pessoal.periodos-aquisitivos-licenca-especial";

  private PeriodoAquisitivoLicencaEspecialRuleSet() {}

  static final List<Rule> PRODUCT = List.of(
      new Rule("CERG-06201", "dtfim",
          "Término da aquisição anterior ao início.",
          "PCK_PERAQLICESP.MAIN_POS:287-290", "PCK_PERAQLICESP.MAIN_POS:287-290",
          """
          {"and":[{"!=":[{"var":"draft.fim"},null]},{"<=":[{"var":"draft.inicio"},{"var":"draft.fim"}]}]}
          """),
      new Rule("CERG-06202", "dtPrescricao",
          "Prescrição anterior ao término da aquisição.",
          "PCK_PERAQLICESP.MAIN_POS:289-293", "PCK_PERAQLICESP.MAIN_POS:289-293",
          """
          {"or":[{"==":[{"var":"draft.fim"},null]},{"==":[{"var":"draft.prescricao"},null]},{"<=":[{"var":"draft.fim"},{"var":"draft.prescricao"}]}]}
          """),
      new Rule("ERG-00803", "dtini",
          "Aquisição anterior ao exercício do vínculo.",
          "PCK_PERAQLICESP.MAIN_POS:317-319", "PCK_PERAQLICESP.MAIN_POS:317-319",
          """
          {"or":[{"!=":[{"var":"draft.origem"},null]},{"or":[{"==":[{"var":"vinculo.exercicio"},null]},{"==":[{"var":"draft.inicio"},null]},{"<=":[{"var":"vinculo.exercicio"},{"var":"draft.inicio"}]}]}]}
          """),
      new Rule("ERG-00804", "dtfim",
          "Aquisição ultrapassa a vacância do vínculo.",
          "PCK_PERAQLICESP.MAIN_POS:320-328", "PCK_PERAQLICESP.MAIN_POS:320-328",
          """
          {"or":[{"==":[{"var":"draft.fim"},null]},{"==":[{"var":"vinculo.vacancia"},null]},{"<=":[{"var":"draft.fim"},{"var":"vinculo.vacancia"}]}]}
          """),
      new Rule("CERG-06205", "chave",
          "Período ativo conflita por chave ou intervalo no vínculo.",
          "PCK_PERAQLICESP.MAIN_POS:273-284", "PCK_PERAQLICESP.MAIN_POS:273-284",
          """
          {"==":[{"var":"neighborhood.sobreposicoes"},0]}
          """),
      new Rule("CERG-06206", "diasDireito",
          "Dias de direito fora dos limites do período aquisitivo.",
          "PCK_PERAQLICESP.MAIN_PRE:58-61", "PCK_PERAQLICESP.MAIN_PRE:58-61",
          """
          {"and":[{">=":[{"var":"draft.diasDireito"},0]},{"<=":[{"var":"draft.diasDireito"},999]}]}
          """),
      new Rule("ERG-02102", "diasDireito",
          "Dias de direito inferiores ao total consumido.",
          "PCK_ERG_CONTDOBRO_LICESP.MAIN_POS:151-156", "PCK_ERG_CONTDOBRO_LICESP.MAIN_POS:151-156",
          """
          {">=":[{"var":"draft.diasDireito"},{"+":[{"var":"neighborhood.diasGozados"},{"var":"neighborhood.diasVendidos"},{"var":"draft.diasConvertidosDobro"}]}]}
          """),
      Rule.replaceable("CERG-06208", "contDobroLicEsp",
          "Override de conversão e venda deve ser I, S ou N.",
          "POLITICA_SHOWCASE.LICENCA_OVERRIDE", "POLITICA_SHOWCASE.LICENCA_OVERRIDE",
          """
          {"and":[{"or":[{"==":[{"var":"draft.vendLicEsp"},"I"]},{"==":[{"var":"draft.vendLicEsp"},"S"]},{"==":[{"var":"draft.vendLicEsp"},"N"]}]},{"or":[{"==":[{"var":"draft.contDobroLicEsp"},"I"]},{"==":[{"var":"draft.contDobroLicEsp"},"S"]},{"==":[{"var":"draft.contDobroLicEsp"},"N"]}]}]}
          """),
      new Rule("ERG-02100", "diasConvertidosDobro",
          "Conversão em dobro excede o limite aplicável.",
          "PCK_ERG_CONTDOBRO_LICESP.MAIN_POS:139-145", "PCK_ERG_CONTDOBRO_LICESP.MAIN_POS:139-145",
          """
          {"or":[{"==":[{"var":"politica.maximoDobro"},null]},{"<=":[{"var":"draft.diasConvertidosDobro"},{"var":"politica.maximoDobro"}]}]}
          """),
      new Rule("ERG-00800", "diasDireito", "Direito excede os dias correspondentes aos meses da política.",
          "PCK_PERAQLICESP.MAIN_PRE:210-214",
          "IF 30 * NVL(MESESLICESPPER, v_pc.meseslicespper) < dias_direito THEN trata_erro(800).",
          """
          {"or":[{"==":[{"var":"politica.mesesPeriodo"},null]},
                 {"<=":[{"var":"draft.diasDireito"},{"*":[30,{"var":"politica.mesesPeriodo"}]}]}]}
          """),
      new Rule("CERG-06220", "numvinc", "O vínculo não possui política habilitada para licença especial.",
          "PCK_PERAQLICESP.MAIN_PRE:183-198",
          "O legado exige temlicesp quando o período depende integralmente da matriz.",
          """
          {"or":[{"==":[{"var":"politica.temLicEsp"},true]},
                 {"!=":[{"var":"draft.anosIndividuais"},null]},
                 {"!=":[{"var":"draft.mesesIndividuais"},null]},
                 {"!=":[{"var":"draft.origem"},null]}]}
          """),
      new Rule("CERG-06221", "diasDireito",
          "Datas e direito base ficam bloqueados após consumo ou geração por averbação.",
          "PCK_PERAQLICESP.MAIN_PRE:112-130", "Preserva o ciclo e a base de direito já utilizados.",
          """
          {"or":[{"!=":[{"var":"current.edicaoManual"},true]},
                 {"and":[{"==":[{"var":"current.origem"},null]},
                          {"==":[{"var":"neighborhood.gozoAtivo"},0]},
                          {"==":[{"var":"neighborhood.vendaAtiva"},0]}]},
                 {"and":[{"==":[{"var":"draft.inicio"},{"var":"current.inicio"}]},
                          {"==":[{"var":"draft.fim"},{"var":"current.fim"}]},
                          {"==":[{"var":"draft.diasDireito"},{"var":"current.diasDireito"}]}]}]}
          """),
      new Rule("CERG-06222", "situacao", "Cancele as parcelas ativas antes de cancelar o período aquisitivo.",
          "POLITICA_SHOWCASE.LICENCA_CANCELAMENTO",
          "O cancelamento não pode invalidar parcelas ou vendas ativas do ciclo.",
          """
          {"or":[{"!=":[{"var":"draft.situacao"},"CANCELADO"]},
                 {"and":[{"==":[{"var":"neighborhood.gozoAtivo"},0]},
                          {"==":[{"var":"neighborhood.vendaAtiva"},0]}]}]}
          """),
      new Rule("ERG-02104", "diasConvertidosDobro",
          "Conversão em dobro não permitida para o vínculo.",
          "PCK_ERG_CONTDOBRO_LICESP.MAIN_POS:105-113", "PCK_ERG_CONTDOBRO_LICESP.MAIN_POS:105-113",
          """
          {"or":[{"<=":[{"var":"draft.diasConvertidosDobro"},0]},{"==":[{"var":"politica.permiteDobro"},true]}]}
          """));

  static final List<Rule> ACTIONS = List.of(
      new Rule("CERG-06212", "dataEfeito", "Aquisição ainda não concluída na data do ato.",
          "POLITICA_SHOWCASE.LICENCA_HOMOLOGACAO", "POLITICA_SHOWCASE.LICENCA_HOMOLOGACAO",
          """
          {"or":[{"and":[{"!=":[{"var":"draft.acao"},"homologar-periodo-licenca-especial"]}]},{"and":[{"!=":[{"var":"draft.fim"},null]},{"<=":[{"var":"draft.fim"},{"var":"draft.dataEfeito"}]}]}]}
          """),
      new Rule("CERG-06213", "dataEfeito", "Prazo prescricional ainda não atingido.",
          "PCK_PERAQLICESP.MAIN_POS:332-356", "PCK_PERAQLICESP.MAIN_POS:332-356",
          """
          {"or":[{"and":[{"!=":[{"var":"draft.acao"},"prescrever-periodo-licenca-especial"]}]},{"and":[{"!=":[{"var":"draft.prescricao"},null]},{"<=":[{"var":"draft.prescricao"},{"var":"draft.dataEfeito"}]}]}]}
          """),
      new Rule("CERG-06214", "dias", "Informe os dias da conversão ou reversão.",
          "PCK_ERG_CONTDOBRO_LICESP.MAIN_POS:137-155", "PCK_ERG_CONTDOBRO_LICESP.MAIN_POS:137-155",
          """
          {"or":[{"and":[{"!=":[{"var":"draft.acao"},"converter-em-dobro"]},{"!=":[{"var":"draft.acao"},"reverter-conversao-dobro"]}]},{"and":[{"!=":[{"var":"draft.diasAto"},null]},{">":[{"var":"draft.diasAto"},0]}]}]}
          """),
      new Rule("CERG-06215", "dias", "Reversão excede os dias convertidos.",
          "POLITICA_SHOWCASE.REVERSAO_CONTAGEM_DOBRO", "POLITICA_SHOWCASE.REVERSAO_CONTAGEM_DOBRO",
          """
          {"or":[{"and":[{"!=":[{"var":"draft.acao"},"reverter-conversao-dobro"]}]},{"==":[{"var":"draft.diasAto"},null]},{"<=":[{"var":"draft.diasAto"},{"var":"draft.diasConvertidosDobro"}]}]}
          """),
      new Rule("CERG-06216", "dias", "Conversão exige permissão da política efetiva.",
          "PCK_ERG_CONTDOBRO_LICESP.MAIN_POS:105-113", "PCK_ERG_CONTDOBRO_LICESP.MAIN_POS:105-113",
          """
          {"or":[{"and":[{"!=":[{"var":"draft.acao"},"converter-em-dobro"]}]},{"==":[{"var":"politica.permiteDobro"},true]}]}
          """),
      new Rule("CERG-06217", "dias", "Dias da conversão abaixo do mínimo por evento.",
          "PCK_ERG_CONTDOBRO_LICESP.MAIN_POS:147-149", "PCK_ERG_CONTDOBRO_LICESP.MAIN_POS:147-149",
          """
          {"or":[{"and":[{"!=":[{"var":"draft.acao"},"converter-em-dobro"]}]},{"or":[{"==":[{"var":"politica.minimoDobro"},null]},{"==":[{"var":"draft.diasAto"},null]},{"<=":[{"var":"politica.minimoDobro"},{"var":"draft.diasAto"}]}]}]}
          """),
      new Rule("CERG-06218", "dias", "Conversão excede o saldo disponível.",
          "PCK_ERG_CONTDOBRO_LICESP.MAIN_POS:151-156", "PCK_ERG_CONTDOBRO_LICESP.MAIN_POS:151-156",
          """
          {"or":[{"and":[{"!=":[{"var":"draft.acao"},"converter-em-dobro"]}]},{"==":[{"var":"draft.diasAto"},null]},{"and":[{"!=":[{"var":"draft.saldoDisponivel"},null]},{"<=":[{"var":"draft.diasAto"},{"var":"draft.saldoDisponivel"}]}]}]}
          """),
      new Rule("CERG-06219", "diasConvertidosDobro", "Reverta a conversão em dobro antes de cancelar.",
          "POLITICA_SHOWCASE.LICENCA_CANCELAMENTO", "POLITICA_SHOWCASE.LICENCA_CANCELAMENTO",
          """
          {"or":[{"and":[{"!=":[{"var":"draft.acao"},"cancelar-periodo-licenca-especial"]}]},{"==":[{"var":"draft.diasConvertidosDobro"},0]}]}
          """));

  static final Rule CUSTOMER =
      new Rule("CERG-06211", "documentoLegalId",
          "Órgão 2 exige documento para conversão em dobro.",
          "POLITICA_ORGAO_2.LICENCA_DOBRO", "POLITICA_ORGAO_2.LICENCA_DOBRO",
          """
          {"or":[{"<=":[{"var":"draft.diasConvertidosDobro"},0]},{"!=":[{"var":"draft.documentoLegalId"},null]}]}
          """);

  public static RuleSetDefinition productRuleSet() {
    return definition(false);
  }

  public static RuleSetDefinition orgao2CustomizedRuleSet() {
    return CadastroPessoalRuleSetSupport.parameterize(definition(true), Map.of("CERG-06206", 180));
  }

  private static RuleSetDefinition definition(boolean orgao2) {
    Rule parameterized = PRODUCT.stream().filter(rule -> rule.code().equals("CERG-06206")).findFirst().orElseThrow();
    var bounds = CadastroPessoalRuleSetSupport.integerTemplate(
        CadastroPessoalRuleSetSupport.somenteCadastro(parameterized), "ergon:periodos-aquisitivos-licenca-especial.limite",
        "limite", 999, 999, "/or/1/and/1/<=/1", "dias");
    List<Rule> rules = Stream.concat(ACTIONS.stream(),
        PRODUCT.stream().map(CadastroPessoalRuleSetSupport::somenteCadastro)).toList();
    return CadastroPessoalRuleSetSupport.definition(RESOURCE, rules,
        CadastroPessoalRuleSetSupport.somenteCadastro(CUSTOMER), orgao2,
        Map.of("CERG-06206", bounds));
  }
}
