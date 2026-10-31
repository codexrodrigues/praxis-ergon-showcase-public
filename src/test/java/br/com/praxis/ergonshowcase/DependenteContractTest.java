package br.com.praxis.ergonshowcase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.praxis.ergonshowcase.config.ErgonDeterminationConfig;
import br.com.praxis.ergonshowcase.config.TenantContext;
import br.com.praxis.ergonshowcase.config.VinculoStateProvider;
import br.com.praxis.ergonshowcase.domain.*;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.mapper.*;
import br.com.praxis.ergonshowcase.repository.DependenteRepository;
import br.com.praxis.ergonshowcase.repository.VinculoRepository;
import br.com.praxis.ergonshowcase.service.ErgonDeterminationService;
import br.com.praxis.ergonshowcase.service.ErgonInvariants;
import br.com.praxis.ergonshowcase.service.ErgonReadFacts;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TypedQuery;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * Testes determinísticos de contrato, invariantes, mapeamento, atualização parcial e determinações
 * reativas de Dependentes e TiposDependente.
 */
class DependenteContractTest {

  @Test
  void dependenteMapperDerivesAgeVigenciaAndIrrfEligibilityDeterministically() {
    var mapper = new DependenteMapper();
    var pessoa = new ServidorPessoa();
    pessoa.setNumero(1001L);
    pessoa.setNome("Servidor Titular Teste");

    var tipo = new TipoDependente();
    tipo.setTipodepen("FILHO_MENOR");
    tipo.setDescricao("Filho(a) até 21 anos");
    tipo.setGeraIr(true);
    tipo.setGeraSalarioFamilia(true);
    tipo.setIdadeLimite(21);

    var entity = new Dependente();
    entity.setId(new DependenteId(1001L, 1));
    entity.setPessoa(pessoa);
    entity.setTipoDependenteEntidade(tipo);
    entity.setNome("Dependente Teste");
    entity.setParentesco("FILHO");
    entity.setTipodepen("FILHO_MENOR");
    entity.setCpf("12345678901");
    entity.setDtnasc(LocalDate.now(ZoneOffset.UTC).minusYears(19));
    entity.setDtini(LocalDate.now(ZoneOffset.UTC).minusYears(19));
    entity.setDepIr(true);
    entity.setUniversitario(false);
    entity.setInvalido(false);

    DependenteResponseDTO active = mapper.toResponse(entity);
    assertThat(active.getId()).isEqualTo(new DependenteId(1001L, 1));
    assertThat(active.getNumfunc()).isEqualTo(1001L);
    assertThat(active.getNumdep()).isEqualTo(1);
    assertThat(active.getPessoaNome()).isEqualTo("Servidor Titular Teste");
    assertThat(active.getTipoDependenteDescricao()).isEqualTo("Filho(a) até 21 anos");
    assertThat(active.getIdadeAnos()).isEqualTo(19L);
    assertThat(active.getDependenteVigente()).isTrue();
    assertThat(active.getElegivelIrrf()).isTrue();

    // Acima de 21 anos sem curso universitário nem invalidez perde elegibilidade automática
    entity.setDtnasc(LocalDate.now(ZoneOffset.UTC).minusYears(23));
    DependenteResponseDTO over21 = mapper.toResponse(entity);
    assertThat(over21.getIdadeAnos()).isEqualTo(23L);
    assertThat(over21.getElegivelIrrf()).isFalse();

    // Universitário até 24 anos recupera elegibilidade
    entity.setUniversitario(true);
    DependenteResponseDTO college = mapper.toResponse(entity);
    assertThat(college.getElegivelIrrf()).isTrue();

    // Encerrado (mesmo na data atual) perde vigência e elegibilidade imediatamente
    entity.setDtfim(LocalDate.now(ZoneOffset.UTC));
    DependenteResponseDTO closed = mapper.toResponse(entity);
    assertThat(closed.getDependenteVigente()).isFalse();
    assertThat(closed.getElegivelIrrf()).isFalse();
  }

  @Test
  void tipoDependenteMapperStaysReadOnlyAndMapsAllColumns() {
    var mapper = new TipoDependenteMapper();
    var doc = new DocumentoLegal();
    doc.setId(77L);
    doc.setNumeroDocumento("LEI-8112/1990");

    var tipo = new TipoDependente();
    tipo.setTipodepen("CONJUGE");
    tipo.setDescricao("Cônjuge ou companheiro(a)");
    tipo.setGeraIr(true);
    tipo.setGeraSalarioFamilia(false);
    tipo.setIdadeLimite(null);
    tipo.setDocumentoLegalId(77L);
    tipo.setDocumentoLegal(doc);

    TipoDependenteResponseDTO dto = mapper.toResponse(tipo);
    assertThat(dto.getId()).isEqualTo("CONJUGE");
    assertThat(dto.getDescricao()).isEqualTo("Cônjuge ou companheiro(a)");
    assertThat(dto.getGeraIr()).isTrue();
    assertThat(dto.getGeraSalarioFamilia()).isFalse();
    assertThat(dto.getIdadeLimite()).isNull();
    assertThat(dto.getDocumentoLegalId()).isEqualTo(77L);
    assertThat(dto.getDocumentoLegalNumero()).isEqualTo("LEI-8112/1990");
    assertThatThrownBy(() -> mapper.newEntity(null))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void reactiveDeterminationConfigPublishesDependenteEligibilityBinding() {
    var defs = new ErgonDeterminationConfig().ergonDeterminations().definitions();
    assertThat(defs)
        .extracting(d -> d.id())
        .contains("ergon.dependente.elegibilidade");
    var depDef =
        defs.stream()
            .filter(d -> "ergon.dependente.elegibilidade".equals(d.id()))
            .findFirst()
            .orElseThrow();
    assertThat(depDef.operationId()).isEqualTo("determineErgonDependenteElegibilidade");
    assertThat(depDef.sourcePaths())
        .containsExactly("/tipodepen", "/dtnasc", "/universitario", "/invalido");
  }

  @Test
  @SuppressWarnings("unchecked")
  void ergonInvariantsEnforcesChronologyCpfCertidaoAndBenefitRules() {
    EntityManager em = mock(EntityManager.class);
    var invariants = new ErgonInvariants();
    ReflectionTestUtils.setField(invariants, "em", em);

    var titular = new ServidorPessoa();
    titular.setNumero(1001L);
    titular.setCpf("11122233344");
    when(em.find(eq(ServidorPessoa.class), eq(1001L), any(LockModeType.class))).thenReturn(titular);

    var tipo = new TipoDependente();
    tipo.setTipodepen("CONJUGE");
    tipo.setGeraIr(true);
    tipo.setGeraSalarioFamilia(false);
    when(em.find(TipoDependente.class, "CONJUGE")).thenReturn(tipo);

    TypedQuery<Long> countQuery = mock(TypedQuery.class);
    when(em.createQuery(anyString(), eq(Long.class))).thenReturn(countQuery);
    when(countQuery.setParameter(anyString(), any())).thenReturn(countQuery);
    when(countQuery.setFlushMode(any(FlushModeType.class))).thenReturn(countQuery);
    when(countQuery.getSingleResult()).thenReturn(0L);

    var dep = new Dependente();
    dep.setId(new DependenteId(1001L, 1));
    dep.setNome("Dependente Válido");
    dep.setParentesco("CONJUGE");
    dep.setTipodepen("CONJUGE");
    dep.setCpf("99988877766");
    dep.setDtnasc(LocalDate.of(1990, 5, 10));
    dep.setDtini(LocalDate.of(2020, 1, 1));
    dep.setDepIr(true);
    dep.setDepSalarioFamilia(false);
    dep.setDepPlanoSaude(true);

    // Caso válido passa sem exceção
    invariants.dependente(dep, true);

    // 1. CPF igual ao titular -> 422
    dep.setCpf("11122233344");
    assertThatThrownBy(() -> invariants.dependente(dep, true))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("CPF do servidor titular");
    dep.setCpf("99988877766");

    // 2. depSalarioFamilia = true em modalidade que não gera salário-família -> 422
    dep.setDepSalarioFamilia(true);
    assertThatThrownBy(() -> invariants.dependente(dep, false))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("salário-família");
    dep.setDepSalarioFamilia(false);

    // 3. dtfim preenchido com benefício ativo -> 422
    dep.setDtfim(LocalDate.of(2024, 1, 1));
    assertThatThrownBy(() -> invariants.dependente(dep, false))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Dependência encerrada não pode manter benefícios ativos");
  }

  @Test
  void ergonDeterminationServiceComputesEligibilityPreview() {
    EntityManager em = mock(EntityManager.class);
    var tipo = new TipoDependente();
    tipo.setTipodepen("FILHO_MENOR");
    tipo.setDescricao("Filho(a) até 21 anos");
    tipo.setGeraIr(true);
    tipo.setIdadeLimite(21);
    when(em.find(TipoDependente.class, "FILHO_MENOR")).thenReturn(tipo);

    var service = new ErgonDeterminationService(new TenantContext(), new ErgonReadFacts());
    ReflectionTestUtils.setField(service, "em", em);

    var previewOver21 =
        service.elegibilidadeDependente(
            new DependenteElegibilidadeDraftDTO(
                "FILHO_MENOR", LocalDate.now(ZoneOffset.UTC).minusYears(22), false, false));
    assertThat(previewOver21.tipoDependenteDescricaoPrevista()).isEqualTo("Filho(a) até 21 anos");
    assertThat(previewOver21.idadeCalculadaAnos()).isEqualTo(22);
    assertThat(previewOver21.elegivelIrrfPrevisto()).isFalse();

    var previewUniversitario =
        service.elegibilidadeDependente(
            new DependenteElegibilidadeDraftDTO(
                "FILHO_MENOR", LocalDate.now(ZoneOffset.UTC).minusYears(22), true, false));
    assertThat(previewUniversitario.elegivelIrrfPrevisto()).isTrue();
  }

  @Test
  void partialUpdateDtosTrackSuppliedPropertiesAndStateProviderAlignsWithDtfim() {
    var certDto = new AtualizarCertidaoDependenteDTO();
    certDto.setMatriculaCert("12345678901234567890123456789012");
    assertThat(certDto.wasSupplied("matriculaCert")).isTrue();
    assertThat(certDto.wasSupplied("livroACert")).isFalse();

    var elegDto = new AtualizarElegibilidadeDependenteDTO();
    elegDto.setUniversitario(true);
    assertThat(elegDto.wasSupplied("universitario")).isTrue();
    assertThat(elegDto.wasSupplied("depIr")).isFalse();

    DependenteRepository depRepo = mock(DependenteRepository.class);
    var dep = new Dependente();
    dep.setId(new DependenteId(1001L, 1));
    dep.setDtini(LocalDate.now(ZoneOffset.UTC).minusYears(5));
    dep.setDtfim(null);
    dep.setInvalido(true);
    when(depRepo.findById(new DependenteId(1001L, 1))).thenReturn(Optional.of(dep));

    var stateProvider = new VinculoStateProvider(mock(VinculoRepository.class));
    ReflectionTestUtils.setField(stateProvider, "dependentes", depRepo);

    var vigenteSnap = stateProvider.resolve("administracao-pessoal.dependentes", "1001:1").orElseThrow();
    assertThat(vigenteSnap.state()).isEqualTo("VIGENTE");
    assertThat(vigenteSnap.metadata()).containsEntry("invalido", true);

    dep.setDtfim(LocalDate.now(ZoneOffset.UTC));
    var encerradoSnap = stateProvider.resolve("administracao-pessoal.dependentes", "1001:1").orElseThrow();
    assertThat(encerradoSnap.state()).isEqualTo("ENCERRADO");
  }
}
