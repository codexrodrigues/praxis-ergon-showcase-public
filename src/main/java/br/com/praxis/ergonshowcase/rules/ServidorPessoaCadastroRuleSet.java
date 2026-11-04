package br.com.praxis.ergonshowcase.rules;

import br.com.praxis.ergonshowcase.rules.CadastroPessoalRuleSetSupport.Rule;
import java.util.List;
import java.util.Map;
import org.praxisplatform.rules.contract.RuleParameterTemplate;
import org.praxisplatform.rules.contract.RuleSetDefinition;

/** Regras do cadastro civil e restrições do órgão efetivo da operação. */
public final class ServidorPessoaCadastroRuleSet {
  static final String RESOURCE = "administracao-pessoal.funcionarios";

  private ServidorPessoaCadastroRuleSet() {}

  static final List<Rule> PRODUCT = List.of(
      new Rule("CERG-02201", "dtnasc", "A data de nascimento não pode ser posterior à data de hoje.",
          "PCK_FUNCIONARIOS.MAIN_PRE:205-209", "IF V_ROW_NEW.DTNASC > SYSDATE THEN trata_erro(99, ...);",
          """
          {"or":[{"==":[{"var":"draft.nascimento"},null]},
                 {"<=":[{"var":"draft.nascimento"},{"var":"context.hoje"}]}]}
          """),
      new Rule("ERG-00235", "dtnasc", "A pessoa está abaixo da idade mínima de cadastro.",
          "PCK_FUNCIONARIOS.VALID_B_I_FUNC:729-733", "ADD_MONTHS(dtnasc, NVL(GET_OPCAO('IDADE_MINIMA'),10)*12) > SYSDATE.",
          """
          {"or":[{"==":[{"var":"draft.nascimento"},null]},
                 {">=":[{"var":"draft.anosCompletosHoje"},10]}]}
          """),
      new Rule("ERG-00236", "dtnasc", "A pessoa está acima da idade máxima de cadastro.",
          "PCK_FUNCIONARIOS.VALID_B_I_FUNC:735-737",
          "ADD_MONTHS(dtnasc, NVL(GET_OPCAO('IDADE_MAXIMA_FUNC'),100)*12) < NVL(dt_cert_fim,SYSDATE).",
          """
          {"or":[{"==":[{"var":"draft.nascimento"},null]},
                 {"<=":[{"var":"draft.anosArredondadosNaReferencia"},100]}]}
          """),
      new Rule("ERG-00241", "anoprimemp", "O ano do primeiro emprego não pode anteceder o ano de nascimento.",
          "PCK_FUNCIONARIOS.VALID_B_I_FUNC:761-763", "TO_CHAR(anoprimemp) < TO_CHAR(dtnasc, 'YYYY').",
          """
          {"or":[{"==":[{"var":"draft.anoPrimeiroEmprego"},null]},{"==":[{"var":"draft.anoNascimento"},null]},
                 {">=":[{"var":"draft.anoPrimeiroEmprego"},{"var":"draft.anoNascimento"}]}]}
          """),
      new Rule("ERG-00242", "anoprimemp", "O ano do primeiro emprego não pode ser posterior ao ano atual.",
          "PCK_FUNCIONARIOS.VALID_B_I_FUNC:764-766", "TO_CHAR(anoprimemp) > TO_CHAR(SYSDATE, 'YYYY').",
          """
          {"or":[{"==":[{"var":"draft.anoPrimeiroEmprego"},null]},
                 {"<=":[{"var":"draft.anoPrimeiroEmprego"},{"var":"context.anoAtual"}]}]}
          """),
      new Rule("CERG-02203", "expedrg", "A expedição do RG não pode anteceder o nascimento.",
          "POLITICA_CADASTRAL_SHOWCASE", "A expedição do documento pressupõe pessoa já nascida.",
          """
          {"or":[{"==":[{"var":"draft.expedicaoRg"},null]},{"==":[{"var":"draft.nascimento"},null]},
                 {">=":[{"var":"draft.expedicaoRg"},{"var":"draft.nascimento"}]}]}
          """),
      Rule.replaceable("CERG-02204", "identprof", "Registro profissional exige número e tipo correspondentes.",
          "POLITICA_CADASTRAL_SHOWCASE", "IDENTPROF e TIPOIDPROF são informados em conjunto.",
          """
          {"or":[{"and":[{"==":[{"var":"draft.registroProfissional"},null]},
                          {"==":[{"var":"draft.tipoRegistro"},null]}]},
                 {"and":[{"!=":[{"var":"draft.registroProfissional"},null]},
                          {"!=":[{"var":"draft.tipoRegistro"},null]}]}]}
          """),
      new Rule("CERG-02205", "cpf", "CPF já utilizado por outra pessoa.",
          "POLITICA_CADASTRAL_SHOWCASE", "O CPF identifica uma única pessoa no cadastro civil compartilhado.",
          """
          {"==":[{"var":"neighborhood.outrosComCpf"},0]}
          """));

  static final Rule CUSTOMER = new Rule(
      "CERG-02206", "pispasep", "O Órgão 2 exige PIS/PASEP no cadastro.",
      "POLITICA_ORGAO_2", "PISPASEP obrigatório no cadastro mantido pelo órgão.",
      """
      {"!=":[{"var":"draft.pispasep"},null]}
      """);

  public static RuleSetDefinition productRuleSet() {
    return CadastroPessoalRuleSetSupport.definition(RESOURCE, PRODUCT, CUSTOMER, false, ageTemplates());
  }

  public static RuleSetDefinition orgao2CustomizedRuleSet() {
    return CadastroPessoalRuleSetSupport.definition(RESOURCE, PRODUCT, CUSTOMER, true, ageTemplates());
  }

  private static Map<String, RuleParameterTemplate> ageTemplates() {
    return Map.of(
        "ERG-00235", CadastroPessoalRuleSetSupport.integerTemplate(PRODUCT.get(1),
            "ergon:cadastro-servidor.idade-minima", "idadeMinima", 10, Integer.MAX_VALUE, "/or/1/>=/1", "years"),
        "ERG-00236", CadastroPessoalRuleSetSupport.integerTemplate(PRODUCT.get(2),
            "ergon:cadastro-servidor.idade-maxima", "idadeMaxima", 100, Integer.MAX_VALUE, "/or/1/<=/1", "years"));
  }
}
