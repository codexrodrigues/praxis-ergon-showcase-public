package br.com.praxis.ergonshowcase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import br.com.praxis.ergonshowcase.domain.DesignacaoFuncao;
import br.com.praxis.ergonshowcase.domain.FuncaoConfianca;
import br.com.praxis.ergonshowcase.domain.VagaFuncao;
import br.com.praxis.ergonshowcase.domain.Vinculo;
import br.com.praxis.ergonshowcase.domain.VinculoId;
import br.com.praxis.ergonshowcase.dto.HomologarDesignacaoLoteDTO;
import br.com.praxis.ergonshowcase.mapper.DesignacaoFuncaoMapper;
import br.com.praxis.ergonshowcase.repository.DesignacaoFuncaoRepository;
import br.com.praxis.ergonshowcase.rules.ErgonRuleEngineFacade;
import br.com.praxis.ergonshowcase.rules.ErgonRuleViolationException;
import br.com.praxis.ergonshowcase.service.DesignacaoFuncaoService;
import jakarta.persistence.TypedQuery;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;
import org.praxisplatform.rules.contract.DecisionSource;
import org.praxisplatform.rules.contract.RuleDecision;

class DesignacaoFuncaoRuleSetTest {
  private final ErgonRuleEngineFacade rules = RuleCatalogExecution.facade();

  @ParameterizedTest
  @ValueSource(ints = {1, 2})
  @SuppressWarnings("unchecked")
  void homologacaoValidatesManagedReferencesBeforeApplyingChanges(int orgao) {
    var em = FuncaoVagaRuleSetTest.scopedReferences(orgao);
    var candidate = draft(null, null);
    candidate.setId(9L);
    candidate.setEmpCodigo(orgao);
    candidate.setSituacao("DESIGNADA");
    candidate.setDtini(LocalDate.of(2026, 1, 1));
    candidate.setVagaFuncaoId(1L);
    candidate.setFuncaoId(2L);
    var vacancy = FuncaoVagaRuleSetTest.vaga();
    vacancy.setEmpCodigo(orgao);
    vacancy.setId(1L);
    var function = FuncaoVagaRuleSetTest.funcao();
    function.setEmpCodigo(orgao);
    var titular = new Vinculo();
    titular.setEmpCodigo(orgao);
    when(em.find(VagaFuncao.class, 1L)).thenReturn(vacancy);
    when(em.find(FuncaoConfianca.class, 2L)).thenReturn(function);
    when(em.find(Vinculo.class, candidate.getTitularId())).thenReturn(titular);
    TypedQuery<VinculoId> active = mock(TypedQuery.class, RETURNS_SELF);
    when(em.createQuery(contains("select d.titularId"), eq(VinculoId.class))).thenReturn(active);
    when(active.getResultList()).thenAnswer(call -> "EM_EXERCICIO".equals(candidate.getSituacao())
        ? List.of(candidate.getTitularId()) : List.of());
    var service = new DesignacaoFuncaoService(mock(DesignacaoFuncaoRepository.class),
        Mappers.getMapper(DesignacaoFuncaoMapper.class), rules);
    FuncaoVagaRuleSetTest.wireScopedService(service, em, orgao);
    var input = new HomologarDesignacaoLoteDTO(candidate.getDtini());

    if (orgao == 2) {
      ErgonFailureAssertions.assertViolation("CERG-07161", DecisionSource.CUSTOMER,
          () -> service.previewHomologacao(candidate, input));
      ErgonFailureAssertions.assertViolation("CERG-07161", DecisionSource.CUSTOMER,
          () -> service.applyHomologacao(candidate, input));
      assertThat(candidate.getSituacao()).isEqualTo("DESIGNADA");
      assertThat(vacancy.getSituacao()).isEqualTo("LIVRE");
      candidate.setAtoPublicacaoId(91L);
    }
    var preview = service.previewHomologacao(candidate, input);
    assertThat(preview.getSituacao()).isEqualTo("EM_EXERCICIO");
    assertThat(candidate.getSituacao()).isEqualTo("DESIGNADA");
    assertThat(vacancy.getSituacao()).isEqualTo("LIVRE");
    service.applyHomologacao(candidate, input);
    assertThat(candidate.getDtExercicio()).isEqualTo(input.dtExercicio());
    assertThat(vacancy.getSituacao()).isEqualTo("OCUPADA");
    verify(active, atLeastOnce()).setParameter("orgao", orgao);
  }

  @Test
  void allowsOptionalAndDistinctSubstitutes() {
    assertThat(rules.enforceDesignacaoFuncao(draft(null, null)).decision()).isEqualTo(RuleDecision.ALLOW);
    assertThat(rules.enforceDesignacaoFuncao(draft(2L, null)).decision()).isEqualTo(RuleDecision.ALLOW);
    assertThat(rules.enforceDesignacaoFuncao(draft(2L, 3L)).decision()).isEqualTo(RuleDecision.ALLOW);
  }

  @Test
  void orgao2ExigePublicacaoParaExercicio() {
    var candidate = draft(null, null);
    candidate.setFuncaoId(2L);
    candidate.setVagaFuncaoId(1L);
    candidate.setSituacao("EM_EXERCICIO");
    candidate.setDtini(LocalDate.of(2026, 1, 1));
    candidate.setDtExercicio(candidate.getDtini());
    assertThat(rules.enforceDesignacaoFuncao(candidate, "DESIGNADA", "LIVRE", 2L, 0).decision())
        .isEqualTo(RuleDecision.ALLOW);
    candidate.setEmpCodigo(2);
    assertThatThrownBy(() -> rules.enforceDesignacaoFuncao(candidate, "DESIGNADA", "LIVRE", 2L, 0))
        .isInstanceOfSatisfying(ErgonRuleViolationException.class,
            error -> assertThat(error.getReasonCodes()).containsExactly("CERG-07161"));
    candidate.setAtoPublicacaoId(20L);
    assertThat(rules.enforceDesignacaoFuncao(candidate, "DESIGNADA", "LIVRE", 2L, 0).decision())
        .isEqualTo(RuleDecision.ALLOW);
  }

  @ParameterizedTest
  @CsvSource(value = {
      "1, NULL, ERG-01203",
      "2, 1, ERG-01203",
      "2, 2, ERG-01204",
      "NULL, 2, ERG-01205"
  }, nullValues = "NULL")
  void mapsEachLegacyViolation(Long primeiro, Long segundo, String code) {
    assertThatThrownBy(() -> rules.enforceDesignacaoFuncao(draft(primeiro, segundo)))
        .isInstanceOfSatisfying(ErgonRuleViolationException.class,
            error -> assertThat(error.getReasonCodes()).containsExactly(code));
  }

  @Test
  void rejectsDesignationWhenEndDatePrecedesStartDate() {
    var candidate = draft(null, null);
    candidate.setDtini(LocalDate.of(2026, 5, 10));
    candidate.setDtfim(LocalDate.of(2026, 5, 9));
    assertThatThrownBy(() -> rules.enforceDesignacaoFuncao(candidate))
        .isInstanceOfSatisfying(ErgonRuleViolationException.class,
            error -> assertThat(error.getReasonCodes()).containsExactly("ERG-01200"));
  }

  static DesignacaoFuncao draft(Long primeiro, Long segundo) {
    var draft = new DesignacaoFuncao();
    draft.setEmpCodigo(1);
    draft.setTitularId(new VinculoId(1L, 1));
    if (primeiro != null) draft.setSubstituto1Id(new VinculoId(primeiro, 2));
    if (segundo != null) draft.setSubstituto2Id(new VinculoId(segundo, 3));
    return draft;
  }

  @ParameterizedTest
  @CsvSource({
      "DESIGNADA, OCUPADA, 1, 1, CERG-07002",
      "DISPENSADA, LIVRE, 0, 1, CERG-07003",
      "DESIGNADA, BLOQUEADA, 0, 1, CERG-07004",
      "DESIGNADA, LIVRE, 0, -1, CERG-07005"
  })
  void exerciseGuardsUseCurrentVacancyAndPreviousState(
      String estadoAnterior, String estadoVaga, long outrosTitulares, int dias, String code) {
    var candidate = draft(null, null);
    candidate.setVagaFuncaoId(1L);
    candidate.setFuncaoId(2L);
    candidate.setSituacao("EM_EXERCICIO");
    candidate.setDtini(LocalDate.of(2026, 1, 1));
    candidate.setDtExercicio(candidate.getDtini().plusDays(dias));
    assertThatThrownBy(() -> rules.enforceDesignacaoFuncao(candidate, estadoAnterior, estadoVaga, 2L, outrosTitulares))
        .isInstanceOfSatisfying(ErgonRuleViolationException.class,
            error -> assertThat(error.getReasonCodes()).containsExactly(code));
  }
}
