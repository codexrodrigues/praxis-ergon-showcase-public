package br.com.praxis.ergonshowcase.rules;

import br.com.praxis.ergonshowcase.rules.CadastroPessoalRuleSetSupport.Rule;
import java.util.List;
import org.praxisplatform.rules.contract.RuleSetDefinition;

/** Regras do quadro de cargos na manutenção e na extinção. */
public final class CargoQuadroRuleSet {
  static final String RESOURCE = "administracao-pessoal.cargos";

  private CargoQuadroRuleSet() {}

  static final List<Rule> PRODUCT = List.of(
      new Rule("CERG-02101", "vagasAutorizadas", "Controle de vagas exige quantidade autorizada positiva.",
          "POLITICA_QUADRO_SHOWCASE", "Controle de vaga booleano exige quantidade maior que zero.",
          """
          {"or":[{"!=":[{"var":"draft.controle"},true]},
            {"and":[{"!=":[{"var":"draft.autorizadas"},null]},{">":[{"var":"draft.autorizadas"},0]}]}]}
          """),
      new Rule("CERG-02102", "vagasAutorizadas", "Vagas autorizadas inferiores à ocupação existente.",
          "POLITICA_QUADRO_SHOWCASE", "A quantidade autorizada não pode ser menor que os vínculos ocupantes.",
          """
          {"or":[{"==":[{"var":"draft.autorizadas"},null]},
            {">=":[{"var":"draft.autorizadas"},{"var":"neighborhood.ocupadas"}]}]}
          """),
      new Rule("CERG-02103", "eAglutinador", "Cargo com dependentes não pode deixar de ser aglutinador.",
          "POLITICA_QUADRO_SHOWCASE", "Cargo referenciado por outros cargos deve permanecer aglutinador.",
          """
          {"or":[{"==":[{"var":"draft.aglutinador"},true]},{\"==\":[{\"var\":\"neighborhood.dependentes\"},0]}]}
          """),
      new Rule("CERG-02104", "cargoAglutinador", "Cargo aglutinador não pode aglutinar a si próprio.",
          "POLITICA_QUADRO_SHOWCASE", "O código de cargo aglutinador não pode ser o código do próprio cargo.",
          """
          {"!==":[{"var":"draft.pai"},{"var":"draft.cargo"}]}
          """),
      new Rule("CERG-02105", "cargoAglutinador", "O cargo aglutinador selecionado deve estar habilitado.",
          "POLITICA_QUADRO_SHOWCASE", "A referência deve apontar para um cargo habilitado como aglutinador.",
          """
          {"or":[{"==":[{"var":"draft.pai"},null]},{"==":[{"var":"cargoAglutinador.aglutinador"},true]}]}
          """),
      new Rule("CERG-02106", "dtExtincao", "Extinção anterior ao início do controle de vagas.",
          "PCK_CARGOS.MAIN_PRE:204-218",
          "IF v_row_new.dt_inicio_contr_vaga > v_row_new.dt_extincao THEN trata_erro(99, ...);",
          """
          {"or":[{"==":[{"var":"draft.inicio"},null]},{"==":[{"var":"draft.extincao"},null]},
            {"<=":[{"var":"draft.inicio"},{"var":"draft.extincao"}]}]}
          """),
      Rule.replaceable("CERG-02107", "dtExtincao", "Cargo ocupado por vínculo ativo, afastado ou cedido.",
          "POLITICA_QUADRO_SHOWCASE", "O ato de extinção exige cargo sem vínculo ocupante.",
          """
          {"or":[{"!=":[{"var":"draft.extinguindo"},true]},{"==":[{"var":"neighborhood.ocupadas"},0]}]}
          """),
      Rule.replaceable("CERG-02109", "cargoAglutinador", "Cargo aglutinador não pode apontar para outro aglutinador.",
          "POLITICA_QUADRO_SHOWCASE", "A aglutinação forma uma hierarquia de um nível.",
          """
          {"or":[{"!=":[{"var":"draft.aglutinador"},true]},{"==":[{"var":"draft.pai"},null]}]}
          """));

  static final Rule CUSTOMER = new Rule(
      "CERG-02108", "documentoLegalId", "O Órgão 2 exige documento legal para controle de vagas ou extinção.",
      "POLITICA_ORGAO_2", "Documento legal do órgão exigido para ato de controle de vaga ou extinção.",
      """
      {"or":[{"and":[{"!=":[{"var":"draft.controle"},true]},
                    {"!=":[{"var":"draft.extinguindo"},true]}]},
             {"!=":[{"var":"draft.documentoLegalId"},null]}]}
      """);

  public static RuleSetDefinition productRuleSet() {
    return CadastroPessoalRuleSetSupport.definition(RESOURCE, PRODUCT, CUSTOMER, false);
  }

  public static RuleSetDefinition orgao2CustomizedRuleSet() {
    return CadastroPessoalRuleSetSupport.definition(RESOURCE, PRODUCT, CUSTOMER, true);
  }
}
