package br.com.praxis.ergonshowcase.rules;

import br.com.praxis.ergonshowcase.rules.CadastroPessoalRuleSetSupport.Rule;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.praxisplatform.rules.contract.RuleSetDefinition;

/** Decisões governadas de licencas especiais gozo. */
public final class LicencaEspecialGozoRuleSet {
  static final String RESOURCE = "administracao-pessoal.licencas-especiais-gozo";

  private LicencaEspecialGozoRuleSet() {}

  static final List<Rule> PRODUCT = List.of(
      new Rule("CERG-06301", "dtprevfim",
          "Término previsto anterior ao início da licença.",
          "PCK_LIC_ESP.MAIN_PRE:65-85", "PCK_LIC_ESP.MAIN_PRE:65-85",
          """
          {"and":[{"!=":[{"var":"draft.prevFim"},null]},{"<=":[{"var":"draft.inicio"},{"var":"draft.prevFim"}]}]}
          """),
      new Rule("CERG-06302", "diasGozo",
          "Dias de gozo devem corresponder ao intervalo inclusivo.",
          "PCK_LIC_ESP.MAIN_POS:305-333", "PCK_LIC_ESP.MAIN_POS:305-333",
          """
          {"and":[{"!=":[{"var":"draft.diasGozo"},null]},{">":[{"var":"draft.diasGozo"},0]},{"==":[{"var":"draft.diasGozo"},{"var":"draft.diasCorridos"}]}]}
          """),
      new Rule("CERG-06303", "periodoAquisitivoLicencaId",
          "Período aquisitivo indisponível para gozo.",
          "POLITICA_SHOWCASE.LICENCA_PERIODO", "POLITICA_SHOWCASE.LICENCA_PERIODO",
          """
          {"and":[{"!=":[{"var":"periodoAquisitivo.situacao"},"CANCELADO"]},{"!=":[{"var":"periodoAquisitivo.situacao"},"SUSPENSO"]},{"or":[{"!=":[{"var":"periodoAquisitivo.situacao"},"PRESCRITO"]},{"==":[{"var":"politica.permiteAposPrescricao"},true]}]}]}
          """),
      new Rule("ERG-00701", "dtini",
          "Gozo antes do fim da aquisição não permitido.",
          "PCK_LIC_ESP.MAIN_POS:274-288", "PCK_LIC_ESP.MAIN_POS:274-288",
          """
          {"or":[{"!=":[{"var":"periodoAquisitivo.origem"},null]},{"==":[{"var":"politica.permiteAntecipacao"},true]},{"==":[{"var":"periodoAquisitivo.fim"},null]},{">":[{"var":"draft.inicio"},{"var":"periodoAquisitivo.fim"}]}]}
          """),
      new Rule("CERG-06305", "numfuncSubstituto",
          "Substituto deve ser outra pessoa com vínculo ativo no órgão.",
          "PCK_LIC_ESP.MAIN_PRE:88-130", "PCK_LIC_ESP.MAIN_PRE:88-130",
          """
          {"or":[{"==":[{"var":"draft.substituto"},null]},{"and":[{"!=":[{"var":"draft.substituto"},{"var":"draft.numfunc"}]},{">":[{"var":"neighborhood.vinculosSubstituto"},0]}]}]}
          """),
      new Rule("CERG-06306", "dtini",
          "Licença concomitante com outro gozo, férias ou afastamento.",
          "PCK_LIC_ESP.MAIN_POS:249-262", "PCK_LIC_ESP.MAIN_POS:249-262",
          """
          {"==":[{"var":"neighborhood.sobreposicoes"},0]}
          """),
      new Rule("ERG-00705", "diasGozo",
          "Consumo excede o saldo do período aquisitivo.",
          "PCK_LIC_ESP.MAIN_POS:356-365", "PCK_LIC_ESP.MAIN_POS:356-365",
          """
          {"and":[{"!=":[{"var":"periodoAquisitivo.saldoDisponivel"},null]},{"<=":[{"var":"draft.consumo"},{"var":"periodoAquisitivo.saldoDisponivel"}]},{"<=":[{"var":"neighborhood.consumoAcumulado"},{"var":"periodoAquisitivo.diasDireito"}]}]}
          """),
      Rule.replaceable("CERG-06308", "abonoPecuniario",
          "Dados de abono inconsistentes com a parcela.",
          "PCK_LIC_ESP.MAIN_POS:305-333", "PCK_LIC_ESP.MAIN_POS:305-333",
          """
          {"or":[{"and":[{"!=":[{"var":"draft.abono"},true]},{"==":[{"var":"draft.diasAbono"},0]},{"==":[{"var":"draft.iniFimAbono"},null]}]},{"and":[{"==":[{"var":"draft.abono"},true]},{"!=":[{"var":"draft.diasAbono"},null]},{">":[{"var":"draft.diasAbono"},0]},{"or":[{"==":[{"var":"draft.iniFimAbono"},"I"]},{"==":[{"var":"draft.iniFimAbono"},"F"]}]}]}]}
          """),
      new Rule("CERG-06309", "diasAbono",
          "Abono não permitido ou incompatível com a proporção vigente.",
          "PCK_LIC_ESP.MAIN_POS:305-333", "PCK_LIC_ESP.MAIN_POS:305-333",
          """
          {"or":[{"!=":[{"var":"draft.abono"},true]},{"and":[{"==":[{"var":"politica.permiteVenda"},true]},{"or":[{"==":[{"var":"politica.baseAbono"},"D"]},{"==":[{"var":"politica.baseAbono"},"G"]}]},{"!=":[{"var":"politica.numeradorAbono"},null]},{"!=":[{"var":"politica.denominadorAbono"},null]},{">=":[{"var":"politica.numeradorAbono"},0]},{">":[{"var":"politica.denominadorAbono"},0]},{"<=":[{"*":[{"var":"draft.diasAbono"},{"var":"politica.denominadorAbono"}]},{"*":[{"var":"draft.baseDiasAbono"},{"var":"politica.numeradorAbono"}]}]},{">":[{"*":[{"+":[{"var":"draft.diasAbono"},1]},{"var":"politica.denominadorAbono"}]},{"*":[{"var":"draft.baseDiasAbono"},{"var":"politica.numeradorAbono"}]}]}]}]}
          """),
      new Rule("ERG-00703", "diasAbono",
          "Abono sobre direito já usado em outra parcela.",
          "PCK_LIC_ESP.MAIN_POS:309-321", "IF num_abonos > 1 THEN trata_erro(703).",
          """
          {"or":[{"!=":[{"var":"draft.abono"},true]},{"!=":[{"var":"politica.baseAbono"},"D"]},{"==":[{"var":"neighborhood.outrosAbonos"},0]}]}
          """),
      new Rule("ERG-00704", "diasAbono",
          "Venda e abono excedem o limite acumulado do período.",
          "PCK_LIC_ESP.MAIN_POS:348-361", "PCK_LIC_ESP.MAIN_POS:348-361",
          """
          {"or":[{"<=":[{"var":"draft.diasAbono"},0]},{"or":[{"==":[{"var":"neighborhood.pecuniaAcumulada"},null]},{"==":[{"var":"politica.maximoVenda"},null]},{"<=":[{"var":"neighborhood.pecuniaAcumulada"},{"var":"politica.maximoVenda"}]}]}]}
          """),
      new Rule("CERG-06312", "ordemParcela",
          "Quantidade ou ordem de parcelas excede o limite.",
          "POLITICA_SHOWCASE.LICENCA_PARCELAS", "POLITICA_SHOWCASE.LICENCA_PARCELAS",
          """
          {"<=":[{"var":"neighborhood.maiorOrdemOuQuantidade"},999]}
          """),
      new Rule("ERG-00702", "dtini", "Início da licença alcança ou ultrapassa a prescrição.",
          "PCK_LIC_ESP.MAIN_POS:289-293",
          "ELSIF TRUNC(v_dt_prescricao) <= TRUNC(dtini) THEN trata_erro(702).",
          """
          {"or":[{"==":[{"var":"politica.permiteAposPrescricao"},true]},
                 {"==":[{"var":"periodoAquisitivo.prescricao"},null]},
                 {"<":[{"var":"draft.inicio"},{"var":"periodoAquisitivo.prescricao"}]}]}
          """),
      new Rule("CERG-06313", "dtfim",
          "Gozo fora do exercício do vínculo.",
          "PCK_LIC_ESP.MAIN_POS:263-292", "PCK_LIC_ESP.MAIN_POS:263-292",
          """
          {"and":[{"or":[{"==":[{"var":"vinculo.exercicio"},null]},{"==":[{"var":"draft.inicio"},null]},{"<=":[{"var":"vinculo.exercicio"},{"var":"draft.inicio"}]}]},{"or":[{"==":[{"var":"draft.fimEfetivo"},null]},{"==":[{"var":"vinculo.vacancia"},null]},{"<=":[{"var":"draft.fimEfetivo"},{"var":"vinculo.vacancia"}]}]}]}
          """));

  static final List<Rule> ACTIONS = List.of(
      new Rule("CERG-06319", "diasAbono", "Proporção de abono não definida ou inválida na política efetiva.",
          "PCK_LIC_ESP.MAIN_POS:305-333", "A proporção de abono usa uma base e um divisor válidos.",
          """
          {"or":[{"!=":[{"var":"draft.acao"},"recalcular-abono-encerramento"]},
                 {"and":[{"or":[{"==":[{"var":"politica.baseAbono"},"D"]},
                                {"==":[{"var":"politica.baseAbono"},"G"]}]},
                          {"!=":[{"var":"politica.numeradorAbono"},null]},
                          {"!=":[{"var":"politica.denominadorAbono"},null]},
                          {">=":[{"var":"politica.numeradorAbono"},0]},
                          {">":[{"var":"politica.denominadorAbono"},0]}]}]}
          """),
      new Rule("CERG-06320", "diasAbono", "Abono liquidado: dias e folha não podem ser alterados pelo gozo.",
          "POLITICA_SHOWCASE.ABONO_LIQUIDADO", "A parcela não modifica um lançamento financeiro histórico.",
          """
          {"or":[{"!=":[{"var":"draft.acao"},"preservar-abono-liquidado"]},
                 {"!=":[{"var":"venda.estado"},"PAGA"]},
                 {"and":[{"==":[{"var":"draft.abono"},true]},
                          {"!=":[{"var":"draft.situacao"},"CANCELADO"]},
                          {"==":[{"var":"draft.diasAbono"},{"var":"venda.diasVendidos"}]},
                          {"==":[{"var":"draft.mesAnoFolha"},{"var":"venda.mesAnoFolha"}]},
                          {"==":[{"var":"draft.numFolha"},{"var":"venda.numFolha"}]}]}]}
          """),
      new Rule("CERG-06315", "dataEfeito", "Gozo ainda não iniciado na data do ato.",
          "POLITICA_SHOWCASE.LICENCA_INICIO", "POLITICA_SHOWCASE.LICENCA_INICIO",
          """
          {"or":[{"and":[{"!=":[{"var":"draft.acao"},"iniciar-gozo-licenca-especial"]}]},{"<=":[{"var":"draft.inicio"},{"var":"draft.dataEfeito"}]}]}
          """),
      new Rule("CERG-06316", "dataEfeito", "Data efetiva fora do intervalo programado.",
          "PCK_LIC_ESP.MAIN_PRE:65-85", "PCK_LIC_ESP.MAIN_PRE:65-85",
          """
          {"or":[{"and":[{"!=":[{"var":"draft.acao"},"encerrar-licenca-especial"]},{"!=":[{"var":"draft.acao"},"interromper-licenca-especial"]}]},{"and":[{"<=":[{"var":"draft.inicio"},{"var":"draft.dataEfeito"}]},{"<=":[{"var":"draft.dataEfeito"},{"var":"draft.prevFim"}]}]}]}
          """),
      new Rule("CERG-06317", "numfuncSubstituto", "Regularize a substituição ativa antes de cancelar a licença.",
          "PCK_LIC_ESP.MAIN_PRE:116-132", "PCK_LIC_ESP.MAIN_PRE:116-132",
          """
          {"or":[{"and":[{"!=":[{"var":"draft.acao"},"cancelar-licenca-especial"]}]},{"==":[{"var":"neighborhood.vinculosSubstituto"},0]}]}
          """));

  static final Rule CUSTOMER =
      new Rule("CERG-06314", "documentoLegalId",
          "Órgão 2 exige documento para parcela com abono.",
          "POLITICA_ORGAO_2.LICENCA_ABONO", "POLITICA_ORGAO_2.LICENCA_ABONO",
          """
          {"or":[{"!=":[{"var":"draft.abono"},true]},{"!=":[{"var":"draft.documentoLegalId"},null]}]}
          """);

  public static RuleSetDefinition productRuleSet() {
    return definition(false);
  }

  public static RuleSetDefinition orgao2CustomizedRuleSet() {
    return CadastroPessoalRuleSetSupport.parameterize(definition(true), Map.of("CERG-06312", 3));
  }

  private static RuleSetDefinition definition(boolean orgao2) {
    Rule parameterized = PRODUCT.stream().filter(rule -> rule.code().equals("CERG-06312")).findFirst().orElseThrow();
    var bounds = CadastroPessoalRuleSetSupport.integerTemplate(
        CadastroPessoalRuleSetSupport.somenteCadastro(parameterized), "ergon:licencas-especiais-gozo.limite",
        "limite", 999, 999, "/or/1/<=/1", "parcelas");
    List<Rule> rules = Stream.concat(ACTIONS.stream(),
        PRODUCT.stream().map(CadastroPessoalRuleSetSupport::somenteCadastro)).toList();
    return CadastroPessoalRuleSetSupport.definition(RESOURCE, rules,
        CadastroPessoalRuleSetSupport.somenteCadastro(CUSTOMER), orgao2,
        Map.of("CERG-06312", bounds));
  }
}
