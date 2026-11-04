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
import br.com.praxis.ergonshowcase.domain.*;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.mapper.*;
import br.com.praxis.ergonshowcase.repository.FrequenciaRepository;
import br.com.praxis.ergonshowcase.repository.VinculoRepository;
import br.com.praxis.ergonshowcase.service.ErgonDeterminationService;
import br.com.praxis.ergonshowcase.service.ErgonInvariants;
import br.com.praxis.ergonshowcase.service.ErgonReadFacts;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TypedQuery;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * Testes determinísticos de contrato, invariantes, mapeamento, atualização parcial e determinações
 * reativas de RegrasFrequenciaEstatutaria e Frequencias.
 */
class FrequenciaContractTest {

  @Test
  void regraFrequenciaEstatutariaMapperStaysReadOnlyAndMapsAllColumns() {
    var orgao = new Orgao();
    orgao.setEmpresa(1);
    orgao.setNome("Secretaria de Administração");

    var doc = new DocumentoLegal();
    doc.setId(91L);
    doc.setNumeroDocumento("DEC-4500/2024");

    var tipo = new TipoFrequencia();
    tipo.setTipo("FALTA");
    tipo.setNome("Faltas e Ausências");

    var codigo = new CodigoFrequencia();
    codigo.setId(new CodigoFrequenciaId("FALTA", 101));
    codigo.setNome("Falta Abonada");

    var regra = new RegraFrequenciaEstatutaria();
    regra.setId(10L);
    regra.setEmpCodigo(1);
    regra.setOrgao(orgao);
    regra.setTipofreq("FALTA");
    regra.setCodfreq(101);
    regra.setTipoFrequenciaEntidade(tipo);
    regra.setCodigoFrequenciaEntidade(codigo);
    regra.setRegimeJuridico("ESTATUTARIO");
    regra.setTipoVinculo("EFETIVO");
    regra.setCategoria("GERAL");
    regra.setSubcategoria("PADRAO");
    regra.setDtini(LocalDate.of(2024, 1, 1));
    regra.setDtfim(null);
    regra.setQtdMinDias(BigDecimal.ONE);
    regra.setQtdMaxDias(BigDecimal.valueOf(6));
    regra.setIntervMinDias(30);
    regra.setIntProrrogDias(15);
    regra.setQtdMaxConsecMes(2);
    regra.setQtdMaxInterpMes(3);
    regra.setQtdMaxConsecAno(6);
    regra.setQtdMaxInterpAno(6);
    regra.setExigeHomologacao(true);
    regra.setPermiteConcomitancia(false);
    regra.setNaoSeAplica(false);
    regra.setRuleKey("ergon.frequencia.falta.abonada");
    regra.setRuleParams(java.util.Map.of("limiteAnual", 6));
    regra.setDocumentoLegalId(91L);
    regra.setDocumentoLegal(doc);
    regra.setAtiva(true);

    var mapper = new RegraFrequenciaEstatutariaMapper();
    RegraFrequenciaEstatutariaResponseDTO dto = mapper.toResponse(regra);

    assertThat(dto.getId()).isEqualTo(10L);
    assertThat(dto.getOrgaoNome()).isEqualTo("Secretaria de Administração");
    assertThat(dto.getCodigoFrequenciaId()).isEqualTo(new CodigoFrequenciaId("FALTA", 101));
    assertThat(dto.getCodigoFrequenciaNome()).isEqualTo("Falta Abonada");
    assertThat(dto.getRuleKey()).isEqualTo("ergon.frequencia.falta.abonada");
    assertThat(dto.getQtdMaxDias()).isEqualByComparingTo(BigDecimal.valueOf(6));
    assertThat(dto.getDocumentoLegalNumero()).isEqualTo("DEC-4500/2024");
    assertThat(dto.getAtiva()).isTrue();
    assertThatThrownBy(() -> mapper.newEntity(null))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void frequenciaMapperDerivesFactsAndMapsMutableFields() {
    var mapper = new FrequenciaMapper();
    var pessoa = new ServidorPessoa();
    pessoa.setNumero(1001L);
    pessoa.setNome("Servidor Frequência Teste");

    var vinculo = new Vinculo();
    vinculo.setId(new VinculoId(1001L, 1));
    vinculo.setMatric("MAT-1001");

    var codigo = new CodigoFrequencia();
    codigo.setId(new CodigoFrequenciaId("PONTO", 301));
    codigo.setNome("Hora Extra 50%");

    var entity = new Frequencia();
    entity.setId(700L);
    entity.setEmpCodigo(1);
    entity.setNumfunc(1001L);
    entity.setNumvinc(1);
    entity.setPessoa(pessoa);
    entity.setVinculo(vinculo);
    entity.setTipofreq("PONTO");
    entity.setCodfreq(301);
    entity.setCodigoFrequenciaEntidade(codigo);
    entity.setDtini(LocalDate.of(2026, 5, 10));
    entity.setDtfim(LocalDate.of(2026, 5, 10));
    entity.setHoraEntrada(LocalTime.of(18, 0));
    entity.setHoraSaida(LocalTime.of(20, 0));
    entity.setQuantidade(BigDecimal.valueOf(2));
    entity.setSituacao("PENDENTE_HOMOLOGACAO");
    entity.setObs("Serviço extraordinário de fechamento da folha.");
    entity.setDocumentoLegalId(91L);

    FrequenciaResponseDTO dto = mapper.toResponse(entity);
    assertThat(dto.getId()).isEqualTo(700L);
    assertThat(dto.getVinculoId()).isEqualTo(new VinculoId(1001L, 1));
    assertThat(dto.getCodigoFrequenciaId()).isEqualTo(new CodigoFrequenciaId("PONTO", 301));
    assertThat(dto.getCodigoFrequenciaNome()).isEqualTo("Hora Extra 50%");
    assertThat(dto.getQuantidade()).isEqualByComparingTo(BigDecimal.valueOf(2));
    assertThat(dto.getPendenteHomologacao()).isTrue();
    assertThat(dto.getOcorrenciaHoraria()).isTrue();
    assertThat(dto.getComDocumentoComprobatorio()).isTrue();

    entity.setSituacao("HOMOLOGADO");
    entity.setHoraEntrada(null);
    entity.setHoraSaida(null);
    entity.setDocumentoLegalId(null);
    FrequenciaResponseDTO homologado = mapper.toResponse(entity);
    assertThat(homologado.getPendenteHomologacao()).isFalse();
    assertThat(homologado.getOcorrenciaHoraria()).isFalse();
    assertThat(homologado.getComDocumentoComprobatorio()).isFalse();
  }

  @Test
  void reactiveDeterminationConfigAndServiceComputeFrequenciaPreview() {
    var defs = new ErgonDeterminationConfig().ergonDeterminations().definitions();
    var freqDef =
        defs.stream()
            .filter(d -> "ergon.frequencia.previa".equals(d.id()))
            .findFirst()
            .orElseThrow();
    assertThat(freqDef.operationId()).isEqualTo("determineErgonFrequenciaPrevia");
    assertThat(freqDef.sourcePaths())
        .containsExactly("/tipofreq", "/codfreq", "/dtini", "/dtfim");

    EntityManager em = mock(EntityManager.class);
    var codigo = new CodigoFrequencia();
    codigo.setId(new CodigoFrequenciaId("PONTO", 301));
    codigo.setNome("Hora Extra 50%");
    codigo.setViaFrequencia(true);
    codigo.setFormatoQtd("HORAS");
    codigo.setHoraEntradaSaida(true);
    when(em.find(CodigoFrequencia.class, new CodigoFrequenciaId("PONTO", 301)))
        .thenReturn(codigo);

    var service = new ErgonDeterminationService(new TenantContext(), new ErgonReadFacts());
    ReflectionTestUtils.setField(service, "em", em);

    var draft = new FrequenciaPreviaDraftDTO();
    draft.setTipofreq("PONTO");
    draft.setCodfreq(301);
    draft.setDtini(LocalDate.of(2026, 5, 10));
    draft.setDtfim(LocalDate.of(2026, 5, 10));

    var preview = service.previaFrequencia(draft);
    assertThat(preview.getCodigoFrequenciaNomePrevisto()).isEqualTo("Hora Extra 50%");
    assertThat(preview.getFormatoQuantidadePrevisto()).isEqualTo("HORAS");
    assertThat(preview.getExigeHorarioPrevisto()).isTrue();

    var invalidDraft = new FrequenciaPreviaDraftDTO();
    invalidDraft.setTipofreq("PONTO");
    invalidDraft.setCodfreq(301);
    invalidDraft.setDtini(LocalDate.of(2026, 5, 10));
    invalidDraft.setDtfim(LocalDate.of(2026, 5, 1));
    assertThatThrownBy(() -> service.previaFrequencia(invalidDraft))
        .isInstanceOfAny(ResponseStatusException.class, org.praxisplatform.rules.violation.PraxisRuleViolationException.class)
        .hasMessageContaining("não pode anteceder a data de início");
  }

  @Test
  @SuppressWarnings("unchecked")
  void ergonInvariantsEnforcesFrequenciaModalityHoursBoundsAndOverlapRules() {
    EntityManager em = mock(EntityManager.class);
    var invariants = new ErgonInvariants();
    ReflectionTestUtils.setField(invariants, "em", em);
    var governed = new br.com.praxis.ergonshowcase.service.FrequenciaService(
        null, null, mock(br.com.praxis.ergonshowcase.config.TenantContext.class));
    ReflectionTestUtils.setField(governed, "entityManager", em);
    ReflectionTestUtils.setField(invariants, "frequenciaService", governed);

    TypedQuery<br.com.praxis.ergonshowcase.domain.RegraFrequenciaEstatutaria> statutory =
        mock(TypedQuery.class, org.mockito.Mockito.RETURNS_SELF);
    when(em.createQuery(anyString(), eq(br.com.praxis.ergonshowcase.domain.RegraFrequenciaEstatutaria.class)))
        .thenReturn(statutory);
    when(statutory.getResultList()).thenReturn(List.of());
    TypedQuery<br.com.praxis.ergonshowcase.domain.Afastamento> crossResource =
        mock(TypedQuery.class, org.mockito.Mockito.RETURNS_SELF);
    when(em.createQuery(anyString(), eq(br.com.praxis.ergonshowcase.domain.Afastamento.class)))
        .thenReturn(crossResource);
    when(crossResource.getResultList()).thenReturn(List.of());



    var titular = new Vinculo();
    titular.setId(new VinculoId(1001L, 1));
    titular.setEmpCodigo(1);
    titular.setDtexerc(LocalDate.of(2020, 1, 10));
    titular.setSituacao("ATIVO");
    when(em.find(eq(Vinculo.class), eq(new VinculoId(1001L, 1)), any(LockModeType.class)))
        .thenReturn(titular);

    when(em.find(Vinculo.class, titular.getId())).thenReturn(titular);
    var pessoa = new ServidorPessoa();
    pessoa.setNumero(1001L);
    pessoa.setSexo("M");
    when(em.find(ServidorPessoa.class, 1001L)).thenReturn(pessoa);
    when(em.find(eq(ServidorPessoa.class), eq(1001L), any(LockModeType.class))).thenReturn(pessoa);

    var codigo = new CodigoFrequencia();
    codigo.setId(new CodigoFrequenciaId("PONTO", 301));
    codigo.setViaFrequencia(true);
    codigo.setFormatoQtd("HORAS");
    codigo.setHoraEntradaSaida(true);
    codigo.setSexo("A");
    codigo.setQtdmin(BigDecimal.valueOf(1));
    codigo.setQtdmax(BigDecimal.valueOf(4));
    when(em.find(CodigoFrequencia.class, new CodigoFrequenciaId("PONTO", 301)))
        .thenReturn(codigo);

    TypedQuery<Frequencia> overlapQuery = mock(TypedQuery.class);
    when(em.createQuery(anyString(), eq(Frequencia.class))).thenReturn(overlapQuery);
    when(overlapQuery.setParameter(anyString(), any())).thenReturn(overlapQuery);
    when(overlapQuery.setFlushMode(any(FlushModeType.class))).thenReturn(overlapQuery);
    when(overlapQuery.getResultList()).thenReturn(List.of());

    var freq = new Frequencia();
    freq.setId(700L);
    freq.setEmpCodigo(1);
    freq.setNumfunc(1001L);
    freq.setNumvinc(1);
    freq.setTipofreq("PONTO");
    freq.setCodfreq(301);
    freq.setDtini(LocalDate.of(2026, 5, 10));
    freq.setDtfim(LocalDate.of(2026, 5, 10));
    freq.setHoraEntrada(LocalTime.of(18, 0));
    freq.setHoraSaida(LocalTime.of(20, 0));
    freq.setQuantidade(new BigDecimal("2.00"));
    freq.setSituacao("HOMOLOGADO");
    freq.setObs("Serviço extraordinário");

    // Caso válido passa
    invariants.frequencia(freq, true);

    // 1. Falta de horário quando horaEntradaSaida = true -> 422
    freq.setHoraSaida(null);
    assertThatThrownBy(() -> invariants.frequencia(freq, false))
        .isInstanceOfAny(ResponseStatusException.class, org.praxisplatform.rules.violation.PraxisRuleViolationException.class)
        .satisfies(ex -> assertThat(((org.praxisplatform.rules.violation.PraxisRuleViolationException) ex).getReasonCodes()).contains("ERG-00436"));
    freq.setHoraSaida(LocalTime.of(20, 0));

    // 2. Quantidade superior a qtdmax (4h) -> 422
    freq.setQuantidade(new BigDecimal("6.00"));
    assertThatThrownBy(() -> invariants.frequencia(freq, false))
        .isInstanceOfAny(ResponseStatusException.class, org.praxisplatform.rules.violation.PraxisRuleViolationException.class)
        .satisfies(ex -> assertThat(((org.praxisplatform.rules.violation.PraxisRuleViolationException) ex).getReasonCodes()).contains("ERG-00428"));
    freq.setQuantidade(new BigDecimal("2.00"));

    // 3. Código não habilitado para lançamento via frequência -> 422
    codigo.setViaFrequencia(false);
    assertThatThrownBy(() -> invariants.frequencia(freq, false))
        .isInstanceOfAny(ResponseStatusException.class, org.praxisplatform.rules.violation.PraxisRuleViolationException.class)
        .satisfies(ex -> assertThat(((org.praxisplatform.rules.violation.PraxisRuleViolationException) ex).getReasonCodes()).contains("ERG-00400"));
    codigo.setViaFrequencia(true);

    // 4. Sobreposição com outra ocorrência ativa do mesmo código -> 409 CONFLICT
    var existente = new Frequencia();
    existente.setEmpCodigo(1);
    existente.setId(699L);
    existente.setNumfunc(1001L);
    existente.setNumvinc(1);
    existente.setTipofreq("PONTO");
    existente.setCodfreq(301);
    existente.setDtini(LocalDate.of(2026, 5, 10));
    existente.setDtfim(LocalDate.of(2026, 5, 10));
    existente.setSituacao("HOMOLOGADO");
    when(overlapQuery.getResultList()).thenReturn(List.of(existente));

    assertThatThrownBy(() -> invariants.frequencia(freq, false))
        .isInstanceOfAny(ResponseStatusException.class, org.praxisplatform.rules.violation.PraxisRuleViolationException.class)
        .satisfies(
            ex ->
                assertThat(ErgonFailureAssertions.status(ex))
                    .isEqualTo(HttpStatus.CONFLICT));
  }

  @Test
  void partialUpdateDtosAndStateProviderTrackFrequenciaLifecycle() {
    var horarioDto = new AtualizarHorarioFrequenciaDTO();
    horarioDto.setHoraEntrada(LocalTime.of(8, 0));
    assertThat(horarioDto.wasSupplied("horaEntrada")).isTrue();
    assertThat(horarioDto.wasSupplied("horaSaida")).isFalse();

    var justDto = new AtualizarJustificativaFrequenciaDTO();
    justDto.setObs("Atestado médico de comparecimento.");
    assertThat(justDto.wasSupplied("obs")).isTrue();
    assertThat(justDto.wasSupplied("documentoLegalId")).isFalse();

    FrequenciaRepository freqRepo = mock(FrequenciaRepository.class);
    var freq = new Frequencia();
    freq.setId(700L);
    freq.setEmpCodigo(1);
    freq.setSituacao("PENDENTE_HOMOLOGACAO");
    freq.setHoraEntrada(LocalTime.of(18, 0));
    freq.setHoraSaida(LocalTime.of(20, 0));
    when(freqRepo.findById(700L)).thenReturn(Optional.of(freq));

    var stateProvider = StateSnapshotFixture.provider(StateSnapshotFixture.service(br.com.praxis.ergonshowcase.service.FrequenciaService.class, freqRepo));


    var pendenteSnap =
        stateProvider.resolve("administracao-pessoal.frequencias", "700").orElseThrow();
    assertThat(pendenteSnap.state()).isEqualTo("PENDENTE_HOMOLOGACAO");
    assertThat(pendenteSnap.metadata()).containsEntry("possuiDocumento", false);

    freq.setSituacao("HOMOLOGADO");
    freq.setDocumentoLegalId(91L);
    var homologadoSnap =
        stateProvider.resolve("administracao-pessoal.frequencias", "700").orElseThrow();
    assertThat(homologadoSnap.state()).isEqualTo("HOMOLOGADO");
    assertThat(homologadoSnap.metadata()).containsEntry("possuiDocumento", true);
  }
}
