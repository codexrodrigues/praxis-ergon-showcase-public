package br.com.praxis.ergonshowcase;

import static br.com.praxis.ergonshowcase.ErgonFailureAssertions.assertViolation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import br.com.praxis.ergonshowcase.domain.DocumentoLegal;
import br.com.praxis.ergonshowcase.rules.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.praxisplatform.rules.contract.DecisionBinding;
import org.praxisplatform.rules.contract.DecisionSource;
import org.praxisplatform.rules.contract.RuleDecision;
import org.praxisplatform.rules.contract.RuleEvaluationResult;
import org.praxisplatform.rules.contract.RuleSetDefinition;
import org.praxisplatform.uischema.repository.base.BaseCrudRepository;
import org.praxisplatform.uischema.service.base.BaseResourceService;
import org.springframework.beans.BeanWrapperImpl;

class TempoLicencaRuleSetContractTest {
  private static final ObjectMapper JSON = new ObjectMapper();
  private final ErgonRuleEngineFacade rules = RuleCatalogExecution.facade();

  enum Resource {
    AVERBACAO(AverbacaoTempoServicoRuleSet::productRuleSet,
        AverbacaoTempoServicoRuleSet::orgao2CustomizedRuleSet,
        """
        {"draft":{"inicio":100,
        "fim":1199,
        "diasCorridos":1100,
        "totalDias":1100,
        "diasFinalidade":100,
        "diasDesaverbados":1,
        "finalidade":"LICENCA_ESPECIAL",
        "complementoInicio":100,
        "complementoFim":1199,
        "sobrepoe":false,
        "documentoLegalId":1},
        "vinculo":{},
        "neighborhood":{"sobreposicoes":0,"consumoPeriodosGerados":0}}
        """),
    PERIODO(PeriodoAquisitivoLicencaEspecialRuleSet::productRuleSet,
        PeriodoAquisitivoLicencaEspecialRuleSet::orgao2CustomizedRuleSet,
        """
        {"draft":{"inicio":100,
        "fim":200,
        "diasDireito":100,
        "diasConvertidosDobro":0,
        "vendLicEsp":"I",
        "contDobroLicEsp":"I",
        "saldoDisponivel":100,
        "documentoLegalId":1},
        "current":{},
        "vinculo":{},
        "politica":{"temLicEsp":true,"permiteDobro":true},
        "neighborhood":{"sobreposicoes":0,"diasGozados":0,"diasVendidos":0,"gozoAtivo":0,"vendaAtiva":0}}
        """),
    GOZO(LicencaEspecialGozoRuleSet::productRuleSet,
        LicencaEspecialGozoRuleSet::orgao2CustomizedRuleSet,
        """
        {"draft":{"inicio":100,
        "prevFim":109,
        "fimEfetivo":109,
        "diasGozo":10,
        "diasCorridos":10,
        "numfunc":1,
        "consumo":10,
        "abono":false,
        "diasAbono":0,
        "documentoLegalId":1},
        "vinculo":{},
        "periodoAquisitivo":{"situacao":"DISPONIVEL","saldoDisponivel":100,"diasDireito":100},
        "politica":{"permiteVenda":true,"baseAbono":"D"},
        "venda":{},
        "neighborhood":{"vinculosSubstituto":0,"sobreposicoes":0,"consumoAcumulado":10,"outrosAbonos":0,"maiorOrdemOuQuantidade":1}}
        """),
    VENDA(VendaLicencaEspecialRuleSet::productRuleSet,
        VendaLicencaEspecialRuleSet::orgao2CustomizedRuleSet,
        """
        {"draft":{"inicio":100,
        "diasVendidos":10,
        "consumo":10,
        "documentoLegalId":1},
        "periodoAquisitivo":{"situacao":"DISPONIVEL","saldoDisponivel":100,"diasDireito":100},
        "politica":{"permiteVenda":true,"baseAbono":"D"},
        "neighborhood":{"pecuniaAcumulada":10,"consumoAcumulado":10}}
        """);

    final Supplier<RuleSetDefinition> product;
    final Supplier<RuleSetDefinition> customized;
    final String baseline;

    Resource(Supplier<RuleSetDefinition> product, Supplier<RuleSetDefinition> customized, String baseline) {
      this.product = product;
      this.customized = customized;
      this.baseline = baseline;
    }

    ObjectNode facts(String overrides) {
      return merge(parse(baseline), parse(overrides));
    }

    BiFunction<Integer, ObjectNode, RuleEvaluationResult> enforcer(ErgonRuleEngineFacade rules) {
      return switch (this) {
        case AVERBACAO -> rules::enforceAverbacaoTempoServico;
        case PERIODO -> rules::enforcePeriodoAquisitivoLicencaEspecial;
        case GOZO -> rules::enforceLicencaEspecialGozo;
        case VENDA -> rules::enforceVendaLicencaEspecial;
      };
    }
  }

  record Case(Resource resource, String code, String setup, String mutation) {
    @Override public String toString() { return code; }
  }

  static Stream<Case> productCases() {
    return Stream.of(
        new Case(Resource.AVERBACAO, "ERG-00006", "{}", "{\"draft\":{\"fim\":99}}"),
        new Case(Resource.AVERBACAO, "CERG-06102", "{}", "{\"draft\":{\"dataAContar\":99}}"),
        new Case(Resource.AVERBACAO, "CERG-06103", "{}", "{\"draft\":{\"totalDias\":1101}}"),
        new Case(Resource.AVERBACAO, "CERG-06104", "{}", "{\"draft\":{\"diasFinalidade\":1101}}"),
        new Case(Resource.AVERBACAO, "CERG-06105", "{}", "{\"draft\":{\"diasDesaverbados\":101}}"),
        new Case(Resource.AVERBACAO, "CERG-06106", "{}", "{\"draft\":{\"complementoFim\":null}}"),
        new Case(Resource.AVERBACAO, "CERG-06107", "{}", "{\"vinculo\":{\"exercicio\":1100}}"),
        new Case(Resource.AVERBACAO, "CERG-06108", "{}", "{\"neighborhood\":{\"sobreposicoes\":1}}"),
        new Case(Resource.AVERBACAO, "ERG-02202", "{\"vinculo\":{\"exercicio\":1201}}", "{\"draft\":{\"dataAContar\":1200}}"),
        new Case(Resource.AVERBACAO, "CERG-06109", "{}", "{\"draft\":{\"diasFinalidade\":1000}}"),
        new Case(Resource.AVERBACAO, "CERG-06110", "{}", "{\"neighborhood\":{\"consumoPeriodosGerados\":1}}"),
        new Case(Resource.AVERBACAO, "CERG-06112", "{\"draft\":{\"acao\":\"registrar-desaverbacao\",\"diasAto\":1}}", "{\"draft\":{\"diasAto\":null}}"),
        new Case(Resource.AVERBACAO, "CERG-06113", "{\"draft\":{\"acao\":\"registrar-desaverbacao\",\"diasAto\":1}}", "{\"draft\":{\"diasAto\":100}}"),
        new Case(Resource.AVERBACAO, "CERG-06114", "{\"draft\":{\"acao\":\"reverter-desaverbacao\",\"diasAto\":1}}", "{\"draft\":{\"diasAto\":2}}"),
        new Case(Resource.AVERBACAO, "CERG-06115", "{\"draft\":{\"acao\":\"cancelar-averbacao\"}}", "{\"neighborhood\":{\"consumoPeriodosGerados\":1}}"),
        new Case(Resource.PERIODO, "CERG-06201", "{}", "{\"draft\":{\"fim\":99}}"),
        new Case(Resource.PERIODO, "CERG-06202", "{}", "{\"draft\":{\"prescricao\":199}}"),
        new Case(Resource.PERIODO, "ERG-00803", "{}", "{\"vinculo\":{\"exercicio\":101}}"),
        new Case(Resource.PERIODO, "ERG-00804", "{}", "{\"vinculo\":{\"vacancia\":199}}"),
        new Case(Resource.PERIODO, "CERG-06205", "{}", "{\"neighborhood\":{\"sobreposicoes\":1}}"),
        new Case(Resource.PERIODO, "CERG-06206", "{}", "{\"draft\":{\"diasDireito\":1000}}"),
        new Case(Resource.PERIODO, "ERG-02102", "{}", "{\"neighborhood\":{\"diasGozados\":101}}"),
        new Case(Resource.PERIODO, "CERG-06208", "{}", "{\"draft\":{\"contDobroLicEsp\":\"X\"}}"),
        new Case(Resource.PERIODO, "ERG-02100", "{\"draft\":{\"diasConvertidosDobro\":1}}", "{\"politica\":{\"maximoDobro\":0}}"),
        new Case(Resource.PERIODO, "ERG-00800", "{}", "{\"politica\":{\"mesesPeriodo\":3}}"),
        new Case(Resource.PERIODO, "CERG-06220", "{}", "{\"politica\":{\"temLicEsp\":false}}"),
        new Case(Resource.PERIODO, "CERG-06221", "{\"current\":{\"edicaoManual\":true,\"inicio\":100,\"fim\":200,\"diasDireito\":100},\"neighborhood\":{\"gozoAtivo\":1}}", "{\"draft\":{\"diasDireito\":101}}"),
        new Case(Resource.PERIODO, "CERG-06222", "{\"draft\":{\"situacao\":\"CANCELADO\"}}", "{\"neighborhood\":{\"gozoAtivo\":1}}"),
        new Case(Resource.PERIODO, "ERG-02104", "{\"draft\":{\"diasConvertidosDobro\":1}}", "{\"politica\":{\"permiteDobro\":false}}"),
        new Case(Resource.PERIODO, "CERG-06212", "{\"draft\":{\"acao\":\"homologar-periodo-licenca-especial\",\"dataEfeito\":200}}", "{\"draft\":{\"dataEfeito\":199}}"),
        new Case(Resource.PERIODO, "CERG-06213", "{\"draft\":{\"acao\":\"prescrever-periodo-licenca-especial\",\"prescricao\":300,\"dataEfeito\":300}}", "{\"draft\":{\"dataEfeito\":299}}"),
        new Case(Resource.PERIODO, "CERG-06214", "{\"draft\":{\"acao\":\"converter-em-dobro\",\"diasAto\":1}}", "{\"draft\":{\"diasAto\":null}}"),
        new Case(Resource.PERIODO, "CERG-06215", "{\"draft\":{\"acao\":\"reverter-conversao-dobro\",\"diasAto\":1,\"diasConvertidosDobro\":1}}", "{\"draft\":{\"diasAto\":2}}"),
        new Case(Resource.PERIODO, "CERG-06216", "{\"draft\":{\"acao\":\"converter-em-dobro\",\"diasAto\":1}}", "{\"politica\":{\"permiteDobro\":false}}"),
        new Case(Resource.PERIODO, "CERG-06217", "{\"draft\":{\"acao\":\"converter-em-dobro\",\"diasAto\":1}}", "{\"politica\":{\"minimoDobro\":2}}"),
        new Case(Resource.PERIODO, "CERG-06218", "{\"draft\":{\"acao\":\"converter-em-dobro\",\"diasAto\":1}}", "{\"draft\":{\"saldoDisponivel\":0}}"),
        new Case(Resource.PERIODO, "CERG-06219", "{\"draft\":{\"acao\":\"cancelar-periodo-licenca-especial\"}}", "{\"draft\":{\"diasConvertidosDobro\":1}}"),
        new Case(Resource.GOZO, "CERG-06301", "{}", "{\"draft\":{\"prevFim\":99}}"),
        new Case(Resource.GOZO, "CERG-06302", "{}", "{\"draft\":{\"diasGozo\":0}}"),
        new Case(Resource.GOZO, "CERG-06303", "{}", "{\"periodoAquisitivo\":{\"situacao\":\"SUSPENSO\"}}"),
        new Case(Resource.GOZO, "ERG-00701", "{}", "{\"periodoAquisitivo\":{\"fim\":100}}"),
        new Case(Resource.GOZO, "CERG-06305", "{}", "{\"draft\":{\"substituto\":2}}"),
        new Case(Resource.GOZO, "CERG-06306", "{}", "{\"neighborhood\":{\"sobreposicoes\":1}}"),
        new Case(Resource.GOZO, "ERG-00705", "{}", "{\"periodoAquisitivo\":{\"saldoDisponivel\":9}}"),
        new Case(Resource.GOZO, "CERG-06308", "{}", "{\"draft\":{\"iniFimAbono\":\"I\"}}"),
        new Case(Resource.GOZO, "CERG-06309", "{\"draft\":{\"abono\":true,\"diasAbono\":2,\"iniFimAbono\":\"I\",\"baseDiasAbono\":10},\"politica\":{\"numeradorAbono\":2,\"denominadorAbono\":10}}", "{\"politica\":{\"denominadorAbono\":11}}"),
        new Case(Resource.GOZO, "ERG-00703", "{\"draft\":{\"abono\":true,\"diasAbono\":2,\"iniFimAbono\":\"I\",\"baseDiasAbono\":10},\"politica\":{\"numeradorAbono\":2,\"denominadorAbono\":10}}", "{\"neighborhood\":{\"outrosAbonos\":1}}"),
        new Case(Resource.GOZO, "ERG-00704", "{\"draft\":{\"abono\":true,\"diasAbono\":2,\"iniFimAbono\":\"I\",\"baseDiasAbono\":10},\"politica\":{\"numeradorAbono\":2,\"denominadorAbono\":10},\"neighborhood\":{\"pecuniaAcumulada\":2}}", "{\"politica\":{\"maximoVenda\":1}}"),
        new Case(Resource.GOZO, "CERG-06312", "{}", "{\"neighborhood\":{\"maiorOrdemOuQuantidade\":1000}}"),
        new Case(Resource.GOZO, "ERG-00702", "{}", "{\"periodoAquisitivo\":{\"prescricao\":100}}"),
        new Case(Resource.GOZO, "CERG-06313", "{}", "{\"vinculo\":{\"vacancia\":108}}"),
        new Case(Resource.GOZO, "CERG-06319", "{\"draft\":{\"acao\":\"recalcular-abono-encerramento\"},\"politica\":{\"numeradorAbono\":2,\"denominadorAbono\":10}}", "{\"politica\":{\"denominadorAbono\":0}}"),
        new Case(Resource.GOZO, "CERG-06320", "{\"draft\":{\"acao\":\"preservar-abono-liquidado\",\"abono\":true,\"diasAbono\":2},\"venda\":{\"estado\":\"PAGA\",\"diasVendidos\":2}}", "{\"venda\":{\"diasVendidos\":3}}"),
        new Case(Resource.GOZO, "CERG-06315", "{\"draft\":{\"acao\":\"iniciar-gozo-licenca-especial\",\"dataEfeito\":100}}", "{\"draft\":{\"dataEfeito\":99}}"),
        new Case(Resource.GOZO, "CERG-06316", "{\"draft\":{\"acao\":\"encerrar-licenca-especial\",\"dataEfeito\":109}}", "{\"draft\":{\"dataEfeito\":110}}"),
        new Case(Resource.GOZO, "CERG-06317", "{\"draft\":{\"acao\":\"cancelar-licenca-especial\"}}", "{\"neighborhood\":{\"vinculosSubstituto\":1}}"),
        new Case(Resource.VENDA, "ERG-03386", "{}", "{\"politica\":{\"permiteVenda\":false}}"),
        new Case(Resource.VENDA, "ERG-03383", "{}", "{\"draft\":{\"diasVendidos\":0}}"),
        new Case(Resource.VENDA, "ERG-03382", "{}", "{\"politica\":{\"maximoVenda\":9}}"),
        new Case(Resource.VENDA, "ERG-03384", "{}", "{\"periodoAquisitivo\":{\"saldoDisponivel\":9}}"),
        new Case(Resource.VENDA, "CERG-06405", "{}", "{\"draft\":{\"pagamento\":99}}"),
        new Case(Resource.VENDA, "CERG-06406", "{}", "{\"draft\":{\"valorIndenizacao\":-1}}"),
        new Case(Resource.VENDA, "CERG-06413", "{\"politica\":{\"numeradorAbono\":1,\"denominadorAbono\":10}}", "{\"politica\":{\"numeradorAbono\":0}}"),
        new Case(Resource.VENDA, "CERG-06408", "{}", "{\"draft\":{\"diasVendidos\":1000}}"),
        new Case(Resource.VENDA, "CERG-06407", "{}", "{\"periodoAquisitivo\":{\"situacao\":\"PRESCRITO\"}}"),
        new Case(Resource.VENDA, "CERG-06410", "{\"draft\":{\"acao\":\"pagar-venda-licenca-especial\",\"valorIndenizacao\":1,\"mesAnoFolha\":202601,\"numFolha\":1,\"atoPublicacaoId\":91}}", "{\"draft\":{\"valorIndenizacao\":0}}"),
        new Case(Resource.VENDA, "CERG-06411", "{\"draft\":{\"acao\":\"pagar-venda-licenca-especial\",\"valorIndenizacao\":1,\"mesAnoFolha\":202601,\"numFolha\":1,\"atoPublicacaoId\":91}}", "{\"draft\":{\"numFolha\":null}}"),
        new Case(Resource.VENDA, "CERG-06414", "{\"draft\":{\"acao\":\"pagar-venda-licenca-especial\",\"valorIndenizacao\":1,\"mesAnoFolha\":202601,\"numFolha\":1,\"atoPublicacaoId\":91}}", "{\"draft\":{\"atoPublicacaoId\":null}}"),
        new Case(Resource.VENDA, "CERG-06412", "{\"draft\":{\"acao\":\"estornar-venda-licenca-especial\",\"pagamento\":200,\"dataEfeito\":200}}", "{\"draft\":{\"dataEfeito\":199}}"));
  }

  static Stream<Case> tenantCases() {
    return Stream.of(
        new Case(Resource.AVERBACAO, "CERG-06109", "{\"draft\":{\"diasFinalidade\":181}}", "{\"draft\":{\"diasFinalidade\":180}}"),
        new Case(Resource.AVERBACAO, "CERG-06111", "{\"draft\":{\"regimePrevidenciario\":\"RGPS\",\"documentoLegalId\":null}}", "{\"draft\":{\"documentoLegalId\":1}}"),
        new Case(Resource.PERIODO, "CERG-06206", "{\"draft\":{\"diasDireito\":181}}", "{\"draft\":{\"diasDireito\":180}}"),
        new Case(Resource.PERIODO, "CERG-06211", "{\"draft\":{\"diasConvertidosDobro\":1,\"documentoLegalId\":null}}", "{\"draft\":{\"documentoLegalId\":1}}"),
        new Case(Resource.GOZO, "CERG-06312", "{\"neighborhood\":{\"maiorOrdemOuQuantidade\":4}}", "{\"neighborhood\":{\"maiorOrdemOuQuantidade\":3}}"),
        new Case(Resource.GOZO, "CERG-06314", "{\"draft\":{\"abono\":true,\"diasAbono\":2,\"iniFimAbono\":\"I\",\"baseDiasAbono\":10,\"documentoLegalId\":null},\"politica\":{\"numeradorAbono\":2,\"denominadorAbono\":10}}", "{\"draft\":{\"documentoLegalId\":1}}"),
        new Case(Resource.VENDA, "CERG-06408", "{\"draft\":{\"diasVendidos\":21}}", "{\"draft\":{\"diasVendidos\":20}}"),
        new Case(Resource.VENDA, "CERG-06409", "{\"draft\":{\"documentoLegalId\":null}}", "{\"draft\":{\"documentoLegalId\":1}}"));
  }

  @Test
  void everyDeclaredCodeAndEveryTenantBindingHasAnExecutionCase() {
    for (Resource resource : Resource.values()) {
      var productCodes = resource.product.get().bindings().stream()
          .map(DecisionBinding::falseReasonCode).toList();
      assertThat(productCases().filter(c -> c.resource == resource).map(Case::code).toList())
          .containsExactlyInAnyOrderElementsOf(productCodes);

      var customized = resource.customized.get();
      var customerCodes = customized.bindings().stream()
          .filter(b -> b.source() == DecisionSource.CUSTOMER)
          .map(b -> b.parameterization() == null ? b.falseReasonCode()
              : customized.slots().stream().filter(s -> s.slotKey().equals(b.slotKey()))
                  .findFirst().orElseThrow().parameterTemplate().falseReasonCode())
          .toList();
      assertThat(tenantCases().filter(c -> c.resource == resource).map(Case::code).toList())
          .containsExactlyInAnyOrderElementsOf(customerCodes);
    }
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("productCases")
  void productRuleRejectsOnlyItsOwnInvalidFact(Case c) {
    var enforce = c.resource.enforcer(rules);
    ObjectNode baseline = c.resource.facts(c.setup);
    assertThat(enforce.apply(1, baseline).decision()).isEqualTo(RuleDecision.ALLOW);
    assertViolation(c.code, DecisionSource.PRODUCT,
        () -> enforce.apply(1, merge(baseline.deepCopy(), parse(c.mutation))));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("tenantCases")
  void sameFactsUseTheOrgSpecificPolicy(Case c) {
    var enforce = c.resource.enforcer(rules);
    ObjectNode facts = c.resource.facts(c.setup);
    assertThat(enforce.apply(1, facts).decision()).isEqualTo(RuleDecision.ALLOW);
    assertViolation(c.code, DecisionSource.CUSTOMER, () -> enforce.apply(2, facts));
    assertThat(enforce.apply(2, merge(facts.deepCopy(), parse(c.mutation))).decision())
        .isEqualTo(RuleDecision.ALLOW);
  }

  @ParameterizedTest
  @CsvSource({
      "0, diasFinalidade, 181, 180, CERG-06109",
      "1, diasDireito, 181, 180, CERG-06206",
      "2, ordemParcela, 4, 3, CERG-06312",
      "3, diasVendidos, 21, 20, CERG-06408"
  })
  @SuppressWarnings({"rawtypes", "unchecked"})
  void createBuildsRuleFactsBeforeSaving(int kind, String field, int boundary, int accepted, String code) {
    for (int orgao : new int[] {1, 2}) {
      var fixture = new TempoLicencaCrudContractTest.Fixture();
      when(fixture.tenant.orgao()).thenReturn(orgao);
      fixture.vinculo.setEmpCodigo(orgao);
      fixture.pa.setEmpCodigo(orgao);
      var document = new DocumentoLegal();
      document.setEmpCodigo(orgao);
      when(fixture.em.find(DocumentoLegal.class, 91L)).thenReturn(document);
      var parts = fixture.service(kind);
      var service = (BaseResourceService) parts[0];
      var repository = (BaseCrudRepository) parts[1];
      var dto = new TempoLicencaCrudContractTest().create(kind);
      var input = new BeanWrapperImpl(dto);
      input.setPropertyValue("empCodigo", orgao);
      input.setPropertyValue("documentoLegalId", 91L);
      input.setPropertyValue(field, boundary);
      if (orgao == 2) {
        assertViolation(code, DecisionSource.CUSTOMER, () -> service.create(dto));
        verify(repository, never()).save(any());
        input.setPropertyValue(field, accepted);
      }
      assertThat(service.create(dto).id()).isEqualTo(91L);
    }
  }

  static ObjectNode merge(ObjectNode target, ObjectNode overrides) {
    overrides.properties().forEach(entry -> {
      if (entry.getValue() instanceof ObjectNode childOverride
          && target.get(entry.getKey()) instanceof ObjectNode childTarget) {
        childTarget.setAll(childOverride);
      } else {
        target.set(entry.getKey(), entry.getValue());
      }
    });
    return target;
  }

  static ObjectNode parse(String input) {
    try {
      return (ObjectNode) JSON.readTree(input);
    } catch (JsonProcessingException error) {
      throw new IllegalArgumentException("Fixture JSON inválida", error);
    }
  }
}
