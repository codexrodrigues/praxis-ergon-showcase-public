package br.com.praxis.ergonshowcase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import br.com.praxis.ergonshowcase.domain.DesignacaoFuncao;
import br.com.praxis.ergonshowcase.domain.Afastamento;
import br.com.praxis.ergonshowcase.domain.Orgao;
import br.com.praxis.ergonshowcase.domain.SubstituicaoFuncao;
import br.com.praxis.ergonshowcase.domain.Vinculo;
import br.com.praxis.ergonshowcase.domain.VinculoId;
import br.com.praxis.ergonshowcase.dto.CreateSubstituicaoFuncaoDTO;
import br.com.praxis.ergonshowcase.mapper.SubstituicaoFuncaoMapper;
import br.com.praxis.ergonshowcase.repository.SubstituicaoFuncaoRepository;
import br.com.praxis.ergonshowcase.rules.ErgonRuleEngineFacade;
import br.com.praxis.ergonshowcase.rules.ErgonRuleErrorCatalog;
import br.com.praxis.ergonshowcase.rules.ErgonRuleViolationException;
import br.com.praxis.ergonshowcase.service.SubstituicaoFuncaoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.praxisplatform.rules.contract.DecisionSource;
import org.praxisplatform.rules.contract.RuleDecision;
import org.mapstruct.factory.Mappers;
import org.springframework.web.server.ResponseStatusException;

class SubstituicaoFuncaoRuleSetTest {
  private final ErgonRuleEngineFacade rules = RuleCatalogExecution.facade();

  @Test
  void allowsCoveredSubstitution() {
    assertThat(rules.enforceSubstituicaoFuncao(1, validFacts()).decision()).isEqualTo(RuleDecision.ALLOW);
  }

  @Test
  void orgao2ExigeDocumentoNaSubstituicaoRemunerada() {
    var facts = validFacts();
    var draft = (ObjectNode) facts.get("draft");
    draft.put("fim", 24).put("inicioRemunerado", 15).put("fimRemunerado", 20);
    assertThat(rules.enforceSubstituicaoFuncao(1, facts).decision()).isEqualTo(RuleDecision.ALLOW);
    assertThatThrownBy(() -> rules.enforceSubstituicaoFuncao(2, facts))
        .isInstanceOfSatisfying(ErgonRuleViolationException.class,
            error -> assertThat(error.getReasonCodes()).containsExactly("CERG-07171"));
    draft.put("documentoLegalId", 20L);
    assertThat(rules.enforceSubstituicaoFuncao(2, facts).decision()).isEqualTo(RuleDecision.ALLOW);
  }

  @Test
  void appliesTheMinimumToInclusiveExerciseDaysOnlyWhenRemunerationIsRequested() {
    ObjectNode facts = validFacts();
    var draft = (ObjectNode) facts.get("draft");
    var funcao = (ObjectNode) facts.get("funcao");
    draft.put("fim", 24).put("inicioRemunerado", 15).put("fimRemunerado", 20);
    funcao.put("diasMinimosSubstituicao", 15);
    assertThat(rules.enforceSubstituicaoFuncao(1, facts).decision()).isEqualTo(RuleDecision.ALLOW);
    funcao.put("diasMinimosSubstituicao", 16);
    assertThatThrownBy(() -> rules.enforceSubstituicaoFuncao(1, facts))
        .isInstanceOfSatisfying(ErgonRuleViolationException.class,
            error -> assertThat(error.getReasonCodes()).containsExactly("CERG-07001"));
    draft.putNull("inicioRemunerado").putNull("fimRemunerado");
    assertThat(rules.enforceSubstituicaoFuncao(1, facts).decision()).isEqualTo(RuleDecision.ALLOW);
  }

  @Test
  void pisoInstitucionalUsaParametroDoOrgaoMesmoSemMinimoDaFuncao() {
    var facts = validFacts();
    var draft = (ObjectNode) facts.get("draft");
    draft.put("fim", 19).put("inicioRemunerado", 15).put("fimRemunerado", 19).put("documentoLegalId", 20L);
    ErgonFailureAssertions.assertViolation("CERG-07001",
        org.praxisplatform.rules.contract.DecisionSource.PRODUCT,
        () -> rules.enforceSubstituicaoFuncao(1, facts));
    assertThat(rules.enforceSubstituicaoFuncao(2, facts).decision()).isEqualTo(RuleDecision.ALLOW);
    draft.put("fim", 18).put("fimRemunerado", 18);
    ErgonFailureAssertions.assertViolation("CERG-07001",
        org.praxisplatform.rules.contract.DecisionSource.CUSTOMER,
        () -> rules.enforceSubstituicaoFuncao(2, facts));
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2})
  @SuppressWarnings("unchecked")
  void createUsesTheRealFactProviderAndScopedQueryResults(int orgao) {
    var em = FuncaoVagaRuleSetTest.scopedReferences(orgao);
    var repository = mock(SubstituicaoFuncaoRepository.class);
    var service = new SubstituicaoFuncaoService(repository, Mappers.getMapper(SubstituicaoFuncaoMapper.class), rules);
    FuncaoVagaRuleSetTest.wireScopedService(service, em, orgao);
    var inicio = LocalDate.of(2026, 1, 1);
    var titularId = new VinculoId(1L, 1);
    var substitutoId = new VinculoId(2L, 1);

    var designacao = new DesignacaoFuncao();
    designacao.setEmpCodigo(orgao);
    when(em.find(DesignacaoFuncao.class, 20L)).thenReturn(designacao);
    var substituto = new Vinculo();
    substituto.setEmpCodigo(orgao);
    when(em.find(Vinculo.class, substitutoId)).thenReturn(substituto);
    var afastamento = new Afastamento();
    afastamento.setEmpCodigo(orgao);
    when(em.find(Afastamento.class, 21L)).thenReturn(afastamento);

    var designacaoFacts = mock(Tuple.class);
    when(designacaoFacts.get("vinculo", VinculoId.class)).thenReturn(titularId);
    when(designacaoFacts.get("funcao", Long.class)).thenReturn(22L);
    when(designacaoFacts.get("inicio", LocalDate.class)).thenReturn(inicio);
    when(designacaoFacts.get("situacao", String.class)).thenReturn("EM_EXERCICIO");
    var substitutoFacts = mock(Tuple.class);
    when(substitutoFacts.get("exercicio", LocalDate.class)).thenReturn(inicio);
    var funcaoFacts = mock(Tuple.class);
    when(funcaoFacts.get("permite", Boolean.class)).thenReturn(true);
    when(funcaoFacts.get("minimo", Integer.class)).thenReturn(14);
    var titularQuery = tupleQuery(em, "from DesignacaoFuncao", designacaoFacts);
    var substitutoQuery = tupleQuery(em, "from Vinculo", substitutoFacts);
    var funcaoQuery = tupleQuery(em, "from FuncaoConfianca", funcaoFacts);
    TypedQuery<Tuple> impedimentos = mock(TypedQuery.class, RETURNS_SELF);
    when(em.createQuery(contains("from Afastamento"), eq(Tuple.class))).thenReturn(impedimentos);
    when(impedimentos.getResultList()).thenReturn(List.of());
    TypedQuery<Long> concomitancias = mock(TypedQuery.class, RETURNS_SELF);
    when(em.createQuery(anyString(), eq(Long.class))).thenReturn(concomitancias);
    when(concomitancias.getSingleResult()).thenReturn(0L);
    when(repository.save(any())).thenAnswer(call -> {
      var entity = call.<SubstituicaoFuncao>getArgument(0);
      entity.setId(10L);
      return entity;
    });

    var input = new CreateSubstituicaoFuncaoDTO();
    input.setEmpCodigo(orgao);
    input.setDesignacaoId(20L);
    input.setSubstitutoId(substitutoId);
    input.setAfastamentoId(21L);
    input.setDtini(inicio);
    input.setDtfim(inicio.plusDays(14));
    input.setDtiniRemunerada(inicio);
    input.setDtfimRemunerada(input.getDtfim());
    input.setDocumentoLegalId(91L);
    ErgonFailureAssertions.assertViolation("ERG-01311", DecisionSource.PRODUCT,
        () -> service.create(input));
    var afastamentoFacts = mock(Tuple.class);
    when(afastamentoFacts.get("situacao", String.class)).thenReturn("HOMOLOGADO");
    when(afastamentoFacts.get("inicio", LocalDate.class)).thenReturn(inicio);
    when(afastamentoFacts.get("fim", LocalDate.class)).thenReturn(input.getDtfim());
    when(impedimentos.getResultList()).thenReturn(List.of(afastamentoFacts));
    if (orgao == 2) {
      input.setDocumentoLegalId(null);
      ErgonFailureAssertions.assertViolation("CERG-07171", DecisionSource.CUSTOMER,
          () -> service.create(input));
      input.setDocumentoLegalId(91L);
    }
    verify(repository, never()).save(any());
    assertThat(service.create(input).id()).isEqualTo(10L);
    verify(titularQuery, atLeastOnce()).setParameter("orgao", orgao);
    verify(substitutoQuery, atLeastOnce()).setParameter("orgao", orgao);
    verify(funcaoQuery, atLeastOnce()).setParameter("orgao", orgao);
    verify(impedimentos, atLeastOnce()).setParameter("orgao", orgao);
    verify(concomitancias, atLeastOnce()).setParameter("orgao", orgao);
  }

  @SuppressWarnings("unchecked")
  private static TypedQuery<Tuple> tupleQuery(EntityManager em, String from, Tuple result) {
    TypedQuery<Tuple> query = mock(TypedQuery.class, RETURNS_SELF);
    when(em.createQuery(contains(from), eq(Tuple.class))).thenReturn(query);
    when(query.getSingleResult()).thenReturn(result);
    return query;
  }

  @Test
  void rejectsForeignDesignationBeforeLoadingRuleFacts() {
    var repository = mock(SubstituicaoFuncaoRepository.class);
    var service = new SubstituicaoFuncaoService(repository, Mappers.getMapper(SubstituicaoFuncaoMapper.class), rules);
    var em = mock(EntityManager.class);
    CanonicalResourceTestContext.wire(service, em);
    var orgao = new Orgao();
    orgao.setEmpresa(1);
    when(em.find(Orgao.class, 1)).thenReturn(orgao);
    var foreign = new DesignacaoFuncao();
    foreign.setEmpCodigo(2);
    when(em.find(DesignacaoFuncao.class, 20L)).thenReturn(foreign);
    var input = new CreateSubstituicaoFuncaoDTO();
    input.setEmpCodigo(1);
    input.setDesignacaoId(20L);

    assertThatThrownBy(() -> service.create(input))
        .isInstanceOfSatisfying(ResponseStatusException.class,
            error -> assertThat(error.getStatusCode().value()).isEqualTo(403));
    verify(em, never()).createQuery(anyString(), any(Class.class));
    verifyNoInteractions(repository);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("violations")
  void projectsReasonFromCatalog(String code, Consumer<ObjectNode> mutate) {
    ObjectNode facts = validFacts();
    mutate.accept(facts);
    assertThat(ErgonRuleErrorCatalog.find(code)).isPresent();
    assertThatThrownBy(() -> rules.enforceSubstituicaoFuncao(1, facts))
        .isInstanceOfSatisfying(ErgonRuleViolationException.class, error ->
            assertThat(error.getReasonCodes()).contains(code));
  }

  private static Stream<Arguments> violations() {
    return Stream.of(
        violation("ERG-01300", facts -> ((ObjectNode) facts.get("draft")).put("fim", 9)),
        violation("ERG-01301", facts -> ((ObjectNode) facts.get("draft")).put("substituto", 1)),
        violation("ERG-01302", facts -> ((ObjectNode) facts.get("draft")).put("fimRemunerado", 20)),
        violation("ERG-01302", facts -> ((ObjectNode) facts.get("draft")).put("inicioRemunerado", 10)),
        violation("ERG-01303", facts -> ((ObjectNode) facts.get("draft")).put("inicioRemunerado", 9).put("fimRemunerado", 12)),
        violation("ERG-01304", facts -> ((ObjectNode) facts.get("substituto")).put("exercicio", 11)),
        violation("ERG-01305", facts -> ((ObjectNode) facts.get("substituto")).put("vacancia", 19)),
        violation("ERG-01306", facts -> ((ObjectNode) facts.get("substituto")).put("inicioCessao", 20)),
        violation("ERG-01308", facts -> ((ObjectNode) facts.get("neighborhood")).put("feriasSubstituto", 1)),
        violation("ERG-01309", facts -> ((ObjectNode) facts.get("neighborhood")).put("afastamentosSubstituto", 1)),
        violation("ERG-01311", facts -> ((ObjectNode) facts.get("impedimento")).put("fim", 19)),
        violation("ERG-01311", facts -> ((ObjectNode) facts.get("impedimento")).put("situacao", "CANCELADO")),
        violation("ERG-01311", facts -> ((ObjectNode) facts.get("impedimento"))
            .put("tipo", "FERIAS").put("situacao", "INTERROMPIDA").put("interrupcao", 20)),
        violation("ERG-01315", facts -> ((ObjectNode) facts.get("funcao")).put("permiteSubstituicao", false)),
        violation("ERG-01319", facts -> ((ObjectNode) facts.get("designacao")).put("fim", 19)),
        violation("ERG-01323", facts -> ((ObjectNode) facts.get("neighborhood")).put("outrasSubstituicoesSubstituto", 1)),
        violation("ERG-01324", facts -> ((ObjectNode) facts.get("neighborhood")).put("outrasSubstituicoesTitular", 1)));
  }

  private static Arguments violation(String code, Consumer<ObjectNode> mutate) {
    return Arguments.of(code, mutate);
  }

  static ObjectNode validFacts() {
    var root = new ObjectMapper().createObjectNode();
    root.putObject("draft")
        .put("inicio", 10).put("fim", 20)
        .putNull("inicioRemunerado").putNull("fimRemunerado")
        .put("substituto", 2).putNull("documentoLegalId");
    root.putObject("designacao")
        .put("titular", 1).put("inicio", 1).putNull("fim").put("situacao", "EM_EXERCICIO");
    root.putObject("substituto")
        .put("exercicio", 1).putNull("vacancia").putNull("inicioCessao").putNull("fimCessao");
    root.putObject("funcao")
        .put("permiteSubstituicao", true).putNull("diasMinimosSubstituicao");
    root.putObject("impedimento")
        .put("tipo", "AFASTAMENTO").put("situacao", "HOMOLOGADO")
        .put("inicio", 10).put("fim", 25).putNull("interrupcao");
    root.putObject("neighborhood")
        .put("outrasSubstituicoesTitular", 0).put("outrasSubstituicoesSubstituto", 0)
        .put("feriasSubstituto", 0).put("afastamentosSubstituto", 0);
    return root;
  }
}
