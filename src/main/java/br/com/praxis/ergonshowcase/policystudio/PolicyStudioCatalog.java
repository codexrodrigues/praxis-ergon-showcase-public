package br.com.praxis.ergonshowcase.policystudio;

import static br.com.praxis.ergonshowcase.policystudio.PolicyStudioDtos.PresetScenario;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

final class PolicyStudioCatalog {
  private static final String CADASTRO = "Quadro, Pessoas, Vínculos e Dependentes";
  private static final String FREQUENCIA = "Frequência e Afastamentos";
  private static final String FERIAS = "Férias e Prescrição";
  private static final String LICENCA = "Tempo de Serviço e Licenças Especiais";
  private static final String CHEFIA = "Funções de Confiança, Vagas e Substituições";

  record Entry(String group, String title, String description) {}

  private static final Map<String, Entry> ENTRIES = Map.ofEntries(
      Map.entry("cargos", new Entry(CADASTRO, "Quadro de cargos",
          "Valida capacidade de vagas, hierarquia de aglutinação e requisitos para extinguir um cargo.")),
      Map.entry("funcionarios", new Entry(CADASTRO, "Cadastro de servidores",
          "Valida unicidade do CPF, cronologia de documentos, limites de idade e registro profissional.")),
      Map.entry("dependentes", new Entry(CADASTRO, "Elegibilidade de dependentes",
          "Avalia parentesco, idade, vínculo estudantil e documentação para os benefícios do dependente.")),
      Map.entry("vinculos", new Entry(CADASTRO, "Provimento de vínculos",
          "Avalia ocupação de vagas, acumulação de vínculos e requisitos para provimento e vacância.")),
      Map.entry("regras-frequencia", new Entry(FREQUENCIA, "Regras estatutárias RN-013",
          "Valida a vigência, a concomitância e os limites que podem ser cadastrados para frequência e afastamento.")),
      Map.entry("frequencias", new Entry(FREQUENCIA, "Lançamento de frequência",
          "Avalia datas, retroatividade, limites estatutários e conflitos com registros funcionais.")),
      Map.entry("afastamentos", new Entry(FREQUENCIA, "Concessão de afastamentos",
          "Avalia o prazo de afastamento, a documentação, a perícia e conflitos no período solicitado.")),
      Map.entry("parametros-contagem-ferias", new Entry(FERIAS, "Parâmetros de contagem de férias",
          "Valida combinações de vigência, quantidade de parcelas e condições para abono pecuniário.")),
      Map.entry("periodos-aquisitivos-ferias", new Entry(FERIAS, "Aquisição de férias",
          "Avalia datas de aquisição, prescrição, saldo e conversões de dias do vínculo.")),
      Map.entry("ferias-gozo", new Entry(FERIAS, "Concessão de férias",
          "Avalia programação, fracionamento, saldo, substituição e pedidos de abono ou adiantamento.")),
      Map.entry("averbacoes-tempo-servico", new Entry(LICENCA, "Averbação de tempo de serviço",
          "Valida o período certificado, as finalidades, as concomitâncias e o limite de dias a averbar.")),
      Map.entry("periodos-aquisitivos-licenca-especial", new Entry(LICENCA, "Aquisição de licença especial",
          "Avalia elegibilidade, formação e prescrição do período, bem como saldo para conversão em dobro.")),
      Map.entry("licencas-especiais-gozo", new Entry(LICENCA, "Gozo de licença especial",
          "Avalia a programação das parcelas, o saldo disponível, a substituição e o abono do gozo.")),
      Map.entry("vendas-licenca-especial", new Entry(LICENCA, "Venda de licença especial",
          "Avalia dias indenizáveis, lastro da liquidação e condições para estornar o pagamento.")),
      Map.entry("funcoes", new Entry(CHEFIA, "Funções de confiança",
          "Valida a vigência da função, os critérios de substituição e o lastro documental da extinção.")),
      Map.entry("vagas-funcao", new Entry(CHEFIA, "Vagas de função",
          "Valida a vigência da vaga, a correspondência com a função e a ocupação por titular.")),
      Map.entry("designacoes", new Entry(CHEFIA, "Designações para função",
          "Avalia a vaga, o início do exercício, a indicação de substitutos e a publicação do ato.")),
      Map.entry("substituicoes", new Entry(CHEFIA, "Substituições em função",
          "Avalia a designação, o período de substituição, a ausência do titular e as condições de remuneração.")));

  private final JsonNode fixtures;

  PolicyStudioCatalog(ObjectMapper mapper) {
    try (var input = PolicyStudioCatalog.class.getResourceAsStream("/policy-studio/scenarios.json")) {
      if (input == null) throw new IllegalStateException("Cenários do Policy Studio ausentes.");
      fixtures = mapper.readTree(input);
    } catch (IOException error) {
      throw new IllegalStateException("Cenários do Policy Studio inválidos.", error);
    }
  }

  Entry entry(String resourceKey) {
    var entry = ENTRIES.get(resourceKey.substring(resourceKey.lastIndexOf('.') + 1));
    if (entry == null) throw new IllegalStateException("Descrição de RuleSet ausente: " + resourceKey);
    return entry;
  }

  List<PresetScenario> scenarios(String key) {
    return StreamSupport.stream(fixtures.path(key).spliterator(), false)
        .map(node -> new PresetScenario(node.path("presetScenarioKey").asText(),
            node.path("descriptionPtBr").asText(), node.path("facts").deepCopy())).toList();
  }
}
