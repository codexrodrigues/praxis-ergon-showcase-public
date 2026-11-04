package br.com.praxis.ergonshowcase.rules;

import br.com.praxis.ergonshowcase.rules.CadastroPessoalRuleSetSupport.Rule;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.praxisplatform.rules.contract.RuleSetDefinition;

/** Decisões governadas de vendas licenca especial. */
public final class VendaLicencaEspecialRuleSet {
  static final String RESOURCE = "administracao-pessoal.vendas-licenca-especial";

  private VendaLicencaEspecialRuleSet() {}

  static final List<Rule> PRODUCT = List.of(
      new Rule("ERG-03386", "diasVendidos",
          "Venda vedada pela política efetiva do período.",
          "PCK_ERG_VENDA_LICESP.MAIN_POS:185-192", "PCK_ERG_VENDA_LICESP.MAIN_POS:185-192",
          """
          {"and":[{"==":[{"var":"politica.permiteVenda"},true]},{"or":[{"==":[{"var":"politica.baseAbono"},"D"]},{"==":[{"var":"politica.baseAbono"},"G"]}]}]}
          """),
      new Rule("ERG-03383", "diasVendidos",
          "Dias vendidos abaixo do mínimo por evento.",
          "PCK_ERG_VENDA_LICESP.MAIN_POS:264-267", "PCK_ERG_VENDA_LICESP.MAIN_POS:264-267",
          """
          {"and":[{">":[{"var":"draft.diasVendidos"},0]},{"or":[{"==":[{"var":"politica.minimoVenda"},null]},{"==":[{"var":"draft.diasVendidos"},null]},{"<=":[{"var":"politica.minimoVenda"},{"var":"draft.diasVendidos"}]}]}]}
          """),
      new Rule("ERG-03382", "diasVendidos",
          "Venda e abono excedem o limite acumulado do período.",
          "PCK_ERG_VENDA_LICESP.MAIN_POS:250-260", "PCK_ERG_VENDA_LICESP.MAIN_POS:250-260",
          """
          {"or":[{"==":[{"var":"neighborhood.pecuniaAcumulada"},null]},{"==":[{"var":"politica.maximoVenda"},null]},{"<=":[{"var":"neighborhood.pecuniaAcumulada"},{"var":"politica.maximoVenda"}]}]}
          """),
      new Rule("ERG-03384", "diasVendidos",
          "Venda excede o saldo disponível.",
          "PCK_ERG_VENDA_LICESP.MAIN_POS:270-275", "PCK_ERG_VENDA_LICESP.MAIN_POS:270-275",
          """
          {"and":[{"!=":[{"var":"periodoAquisitivo.saldoDisponivel"},null]},{"<=":[{"var":"draft.consumo"},{"var":"periodoAquisitivo.saldoDisponivel"}]},{"<=":[{"var":"neighborhood.consumoAcumulado"},{"var":"periodoAquisitivo.diasDireito"}]}]}
          """),
      new Rule("CERG-06405", "dtPagamento",
          "Pagamento anterior à solicitação da venda.",
          "POLITICA_SHOWCASE.VENDA_CRONOLOGIA", "POLITICA_SHOWCASE.VENDA_CRONOLOGIA",
          """
          {"or":[{"==":[{"var":"draft.inicio"},null]},{"==":[{"var":"draft.pagamento"},null]},{"<=":[{"var":"draft.inicio"},{"var":"draft.pagamento"}]}]}
          """),
      new Rule("CERG-06406", "valorIndenizacao",
          "Valor de indenização da venda não pode ser negativo.",
          "POLITICA_SHOWCASE.VENDA_INDENIZACAO", "POLITICA_SHOWCASE.VENDA_INDENIZACAO",
          """
          {"or":[{"==":[0,null]},{"==":[{"var":"draft.valorIndenizacao"},null]},{"<=":[0,{"var":"draft.valorIndenizacao"}]}]}
          """),
      new Rule("CERG-06413", "diasVendidos", "Dias vendidos excedem a fração definida para o período.",
          "POLITICA_SHOWCASE.VENDA_FRACAO", "Limita a venda à fração do direito quando a matriz a define.",
          """
          {"or":[{"and":[{"==":[{"var":"politica.numeradorAbono"},null]},
                         {"==":[{"var":"politica.denominadorAbono"},null]}]},
                 {"and":[{"!=":[{"var":"politica.numeradorAbono"},null]},
                          {">":[{"var":"politica.denominadorAbono"},0]},
                          {"<=":[{"*":[{"var":"draft.diasVendidos"},{"var":"politica.denominadorAbono"}]},
                                 {"*":[{"var":"periodoAquisitivo.diasDireito"},{"var":"politica.numeradorAbono"}]}]}]}]}
          """),
      new Rule("CERG-06408", "diasVendidos",
          "Dias vendidos excedem o teto por operação.",
          "POLITICA_SHOWCASE.VENDA_TETO", "POLITICA_SHOWCASE.VENDA_TETO",
          """
          {"<=":[{"var":"draft.diasVendidos"},999]}
          """),
      new Rule("CERG-06407", "periodoAquisitivoLicencaId",
          "Período aquisitivo indisponível para venda.",
          "POLITICA_SHOWCASE.VENDA_PERIODO", "POLITICA_SHOWCASE.VENDA_PERIODO",
          """
          {"and":[{"!=":[{"var":"periodoAquisitivo.situacao"},"CANCELADO"]},{"!=":[{"var":"periodoAquisitivo.situacao"},"SUSPENSO"]},{"!=":[{"var":"periodoAquisitivo.situacao"},"PRESCRITO"]}]}
          """));

  static final List<Rule> ACTIONS = List.of(
      new Rule("CERG-06410", "valorIndenizacao", "Liquidação exige valor positivo.",
          "POLITICA_SHOWCASE.LICENCA_LIQUIDACAO", "POLITICA_SHOWCASE.LICENCA_LIQUIDACAO",
          """
          {"or":[{"and":[{"!=":[{"var":"draft.acao"},"pagar-venda-licenca-especial"]}]},{"and":[{"!=":[{"var":"draft.valorIndenizacao"},null]},{">":[{"var":"draft.valorIndenizacao"},0]}]}]}
          """),
      new Rule("CERG-06411", "mesAnoFolha", "Liquidação exige a identificação da folha.",
          "POLITICA_SHOWCASE.LICENCA_FOLHA", "POLITICA_SHOWCASE.LICENCA_FOLHA",
          """
          {"or":[{"and":[{"!=":[{"var":"draft.acao"},"pagar-venda-licenca-especial"]}]},{"and":[{"!=":[{"var":"draft.mesAnoFolha"},null]},{"!=":[{"var":"draft.numFolha"},null]}]}]}
          """),
      Rule.replaceable("CERG-06414", "atoPublicacaoId", "Liquidação de venda avulsa exige ato de publicação.",
          "POLITICA_SHOWCASE.VENDA_PUBLICACAO", "Na liquidação de venda avulsa, o ato de publicação deve estar registrado na venda.",
          """
          {"or":[{"!=":[{"var":"draft.acao"},"pagar-venda-licenca-especial"]},{"!=":[{"var":"draft.licencaEspecialGozoId"},null]},{"!=":[{"var":"draft.atoPublicacaoId"},null]}]}
          """),
      new Rule("CERG-06412", "dataEfeito", "Estorno anterior ao pagamento original.",
          "POLITICA_SHOWCASE.LICENCA_ESTORNO", "POLITICA_SHOWCASE.LICENCA_ESTORNO",
          """
          {"or":[{"and":[{"!=":[{"var":"draft.acao"},"estornar-venda-licenca-especial"]}]},{"or":[{"==":[{"var":"draft.pagamento"},null]},{"<=":[{"var":"draft.pagamento"},{"var":"draft.dataEfeito"}]}]}]}
          """));

  static final Rule CUSTOMER =
      new Rule("CERG-06409", "documentoLegalId",
          "Órgão 2 exige documento para venda de licença especial.",
          "POLITICA_ORGAO_2.VENDA_DOCUMENTO", "POLITICA_ORGAO_2.VENDA_DOCUMENTO",
          """
          {"!=":[{"var":"draft.documentoLegalId"},null]}
          """);

  public static RuleSetDefinition productRuleSet() {
    return definition(false);
  }

  public static RuleSetDefinition orgao2CustomizedRuleSet() {
    return CadastroPessoalRuleSetSupport.parameterize(definition(true), Map.of("CERG-06408", 20));
  }

  private static RuleSetDefinition definition(boolean orgao2) {
    Rule parameterized = PRODUCT.stream().filter(rule -> rule.code().equals("CERG-06408")).findFirst().orElseThrow();
    var bounds = CadastroPessoalRuleSetSupport.integerTemplate(
        CadastroPessoalRuleSetSupport.somenteCadastro(parameterized), "ergon:vendas-licenca-especial.limite",
        "limite", 999, 999, "/or/1/<=/1", "dias");
    List<Rule> rules = Stream.concat(ACTIONS.stream(),
        PRODUCT.stream().map(CadastroPessoalRuleSetSupport::somenteCadastro)).toList();
    return CadastroPessoalRuleSetSupport.definition(RESOURCE, rules,
        CadastroPessoalRuleSetSupport.somenteCadastro(CUSTOMER), orgao2,
        Map.of("CERG-06408", bounds));
  }
}
