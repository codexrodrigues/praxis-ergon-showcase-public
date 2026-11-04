package br.com.praxis.ergonshowcase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import br.com.praxis.ergonshowcase.config.TenantContext;
import br.com.praxis.ergonshowcase.domain.DocumentoLegal;
import br.com.praxis.ergonshowcase.domain.FuncaoConfianca;
import br.com.praxis.ergonshowcase.domain.Orgao;
import br.com.praxis.ergonshowcase.domain.VagaFuncao;
import br.com.praxis.ergonshowcase.domain.VinculoId;
import br.com.praxis.ergonshowcase.dto.CreateFuncaoConfiancaDTO;
import br.com.praxis.ergonshowcase.dto.CreateVagaFuncaoDTO;
import br.com.praxis.ergonshowcase.mapper.FuncaoConfiancaMapper;
import br.com.praxis.ergonshowcase.mapper.VagaFuncaoMapper;
import br.com.praxis.ergonshowcase.repository.FuncaoConfiancaRepository;
import br.com.praxis.ergonshowcase.repository.VagaFuncaoRepository;
import br.com.praxis.ergonshowcase.rules.ErgonRuleEngineFacade;
import br.com.praxis.ergonshowcase.rules.ErgonRuleViolationException;
import br.com.praxis.ergonshowcase.service.FuncaoConfiancaService;
import br.com.praxis.ergonshowcase.service.VagaFuncaoService;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;
import org.praxisplatform.rules.contract.DecisionSource;
import org.praxisplatform.rules.contract.RuleDecision;
import org.springframework.test.util.ReflectionTestUtils;

class FuncaoVagaRuleSetTest {
  private static final LocalDate INICIO = LocalDate.of(2026, 1, 1);
  private final ErgonRuleEngineFacade rules = RuleCatalogExecution.facade();

  @ParameterizedTest
  @ValueSource(ints = {1, 2})
  void funcaoCreateUsesPublicationFromTheCandidateEntity(int orgao) {
    var em = scopedReferences(orgao);
    var repository = mock(FuncaoConfiancaRepository.class);
    var service = new FuncaoConfiancaService(repository, Mappers.getMapper(FuncaoConfiancaMapper.class));
    wireScopedService(service, em, orgao);
    ReflectionTestUtils.setField(service, "rules", rules);
    when(repository.save(any())).thenAnswer(call -> {
      var entity = call.<FuncaoConfianca>getArgument(0);
      entity.setId(10L);
      return entity;
    });
    var input = new CreateFuncaoConfiancaDTO();
    input.setEmpCodigo(orgao);
    input.setDtini(INICIO);
    input.setSituacao("EXTINTA");
    input.setDocumentoLegalId(91L);
    if (orgao == 2) {
      ErgonFailureAssertions.assertViolation("CERG-07102", DecisionSource.CUSTOMER,
          () -> service.create(input));
      verify(repository, never()).save(any());
      input.setAtoPublicacaoId(91L);
    }
    assertThat(service.create(input).id()).isEqualTo(10L);
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2})
  void vagaCreateUsesTheReferencedFunctionStateAndTenantDocument(int orgao) {
    var em = scopedReferences(orgao);
    var parent = funcao();
    parent.setEmpCodigo(orgao);
    when(em.find(FuncaoConfianca.class, 2L)).thenReturn(parent);
    var repository = mock(VagaFuncaoRepository.class);
    var service = new VagaFuncaoService(repository, Mappers.getMapper(VagaFuncaoMapper.class));
    wireScopedService(service, em, orgao);
    ReflectionTestUtils.setField(service, "rules", rules);
    when(repository.save(any())).thenAnswer(call -> {
      var entity = call.<VagaFuncao>getArgument(0);
      entity.setId(10L);
      return entity;
    });
    var input = new CreateVagaFuncaoDTO();
    input.setEmpCodigo(orgao);
    input.setDtini(INICIO);
    input.setFuncaoId(2L);
    input.setSituacao("LIVRE");
    if (orgao == 2) {
      ErgonFailureAssertions.assertViolation("CERG-07151", DecisionSource.CUSTOMER,
          () -> service.create(input));
      verify(repository, never()).save(any());
      input.setDocumentoLegalId(91L);
    }
    parent.setSituacao("SUSPENSA");
    denied("ERG-01154", () -> service.create(input));
    verify(repository, never()).save(any());
    parent.setSituacao("ATIVA");
    assertThat(service.create(input).id()).isEqualTo(10L);
  }

  static EntityManager scopedReferences(int orgao) {
    var em = mock(EntityManager.class);
    var organization = new Orgao();
    organization.setEmpresa(orgao);
    when(em.find(Orgao.class, orgao)).thenReturn(organization);
    var document = new DocumentoLegal();
    document.setEmpCodigo(orgao);
    when(em.find(DocumentoLegal.class, 91L)).thenReturn(document);
    return em;
  }

  static void wireScopedService(Object service, EntityManager em, int orgao) {
    CanonicalResourceTestContext.wire(service, em);
    var tenant = mock(TenantContext.class);
    when(tenant.orgao()).thenReturn(orgao);
    ReflectionTestUtils.setField(service, "resourceTenant", tenant);
  }

  @ParameterizedTest
  @ValueSource(strings = {"ERG-01101", "ERG-01102", "CERG-07101"})
  void funcaoRejeitaCadaRegraDeProduto(String code) {
    var draft = funcao();
    switch (code) {
      case "ERG-01101" -> draft.setDtfim(INICIO.minusDays(1));
      case "ERG-01102" -> draft.setPermiteSubstituicao(true);
      case "CERG-07101" -> draft.setSituacao("EXTINTA");
      default -> throw new AssertionError(code);
    }
    denied(code, () -> rules.enforceFuncaoConfianca(draft));
  }

  @Test
  void orgao2ExigeAtoDePublicacaoNaExtincao() {
    var draft = funcao();
    draft.setSituacao("EXTINTA");
    draft.setDocumentoLegalId(5L);
    assertThat(rules.enforceFuncaoConfianca(draft).decision()).isEqualTo(RuleDecision.ALLOW);
    draft.setEmpCodigo(2);
    denied("CERG-07102", () -> rules.enforceFuncaoConfianca(draft));
    draft.setAtoPublicacaoId(7L);
    assertThat(rules.enforceFuncaoConfianca(draft).decision()).isEqualTo(RuleDecision.ALLOW);
  }

  @ParameterizedTest
  @ValueSource(strings = {"ERG-01151", "ERG-01152", "ERG-01153", "ERG-01154"})
  void vagaRejeitaCadaRegraDeProduto(String code) {
    var draft = vaga();
    var funcao = funcao();
    var anterior = vaga();
    switch (code) {
      case "ERG-01151" -> draft.setDtfim(INICIO.minusDays(1));
      case "ERG-01152" -> draft.setSituacao("OCUPADA");
      case "ERG-01153" -> {
        anterior.setTitularAtualId(new VinculoId(1L, 1));
        draft.setSituacao("OCUPADA");
        draft.setFuncaoId(3L);
      }
      case "ERG-01154" -> funcao.setSituacao("SUSPENSA");
      default -> throw new AssertionError(code);
    }
    denied(code, () -> rules.enforceVagaFuncao(draft, anterior, funcao));
  }

  @Test
  void orgao2ExigeDocumentoSomenteNaAbertura() {
    var draft = vaga();
    var funcao = funcao();
    assertThat(rules.enforceVagaFuncao(draft, null, funcao).decision()).isEqualTo(RuleDecision.ALLOW);
    draft.setEmpCodigo(2);
    denied("CERG-07151", () -> rules.enforceVagaFuncao(draft, null, funcao));
    assertThat(rules.enforceVagaFuncao(draft, vaga(), funcao).decision()).isEqualTo(RuleDecision.ALLOW);
    draft.setDocumentoLegalId(5L);
    assertThat(rules.enforceVagaFuncao(draft, null, funcao).decision()).isEqualTo(RuleDecision.ALLOW);
  }

  private static void denied(String code, Runnable write) {
    assertThatThrownBy(write::run).isInstanceOfSatisfying(ErgonRuleViolationException.class, error -> {
      assertThat(error.getHttpStatus()).isEqualTo(422);
      assertThat(error.getReasonCodes()).containsExactly(code);
      var violation = error.getViolations().getFirst();
      assertThat(violation.slotKey()).isNotBlank();
      assertThat(violation.targetField()).isNotBlank();
      assertThat(violation.lineage()).isNotBlank();
    });
  }

  static FuncaoConfianca funcao() {
    var draft = new FuncaoConfianca();
    draft.setEmpCodigo(1);
    draft.setSituacao("ATIVA");
    draft.setDtini(INICIO);
    return draft;
  }

  static VagaFuncao vaga() {
    var draft = new VagaFuncao();
    draft.setEmpCodigo(1);
    draft.setFuncaoId(2L);
    draft.setDtini(INICIO);
    draft.setSituacao("LIVRE");
    return draft;
  }
}
