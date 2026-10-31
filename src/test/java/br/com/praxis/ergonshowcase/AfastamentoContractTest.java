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
import br.com.praxis.ergonshowcase.repository.AfastamentoRepository;
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
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * Testes determinísticos de contrato, invariantes, mapeamento, atualização parcial e determinações
 * reativas de TiposFrequencia, CodigosFrequencia e Afastamentos.
 */
class AfastamentoContractTest {

  @Test
  void tipoECodigoFrequenciaMappersStayReadOnlyAndMapAllColumns() {
    var doc = new DocumentoLegal();
    doc.setId(88L);
    doc.setNumeroDocumento("LEI-8112/1990");

    var tipo = new TipoFrequencia();
    tipo.setTipo("LICENCA");
    tipo.setNome("Licenças Estatutárias");
    tipo.setInfluiConta(true);
    tipo.setHoraExtra(false);
    tipo.setPercentualExtra(BigDecimal.ZERO);
    tipo.setDocumentoLegalId(88L);
    tipo.setDocumentoLegal(doc);

    var tipoMapper = new TipoFrequenciaMapper();
    TipoFrequenciaResponseDTO tipoDto = tipoMapper.toResponse(tipo);
    assertThat(tipoDto.getId()).isEqualTo("LICENCA");
    assertThat(tipoDto.getTipo()).isEqualTo("LICENCA");
    assertThat(tipoDto.getNome()).isEqualTo("Licenças Estatutárias");
    assertThat(tipoDto.getInfluiConta()).isTrue();
    assertThat(tipoDto.getHoraExtra()).isFalse();
    assertThat(tipoDto.getPercentualExtra()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(tipoDto.getDocumentoLegalId()).isEqualTo(88L);
    assertThat(tipoDto.getDocumentoLegalNumero()).isEqualTo("LEI-8112/1990");
    assertThatThrownBy(() -> tipoMapper.newEntity(null))
        .isInstanceOf(UnsupportedOperationException.class);

    var codigo = new CodigoFrequencia();
    codigo.setId(new CodigoFrequenciaId("LICENCA", 201));
    codigo.setTipoFrequenciaEntidade(tipo);
    codigo.setDocumentoLegal(doc);
    codigo.setMnemonico("LTS");
    codigo.setNome("Licença para Tratamento de Saúde");
    codigo.setNomeAbrev("Lic. Saúde");
    codigo.setTipoPreenchimento("PERIODO");
    codigo.setFormatoQtd("DIAS");
    codigo.setViaFrequencia(false);
    codigo.setOnus(true);
    codigo.setAfastamento(true);
    codigo.setSexo("A");
    codigo.setQtdmin(BigDecimal.ONE);
    codigo.setQtdmax(BigDecimal.valueOf(730));
    codigo.setRegrasFreq(true);
    codigo.setHoraEntradaSaida(false);
    codigo.setQtdFaltas(BigDecimal.ZERO);
    codigo.setDocumentoLegalId(88L);

    var codigoMapper = new CodigoFrequenciaMapper();
    CodigoFrequenciaResponseDTO codigoDto = codigoMapper.toResponse(codigo);
    assertThat(codigoDto.getId()).isEqualTo(new CodigoFrequenciaId("LICENCA", 201));
    assertThat(codigoDto.getTipofreq()).isEqualTo("LICENCA");
    assertThat(codigoDto.getTipoFrequenciaNome()).isEqualTo("Licenças Estatutárias");
    assertThat(codigoDto.getCodigo()).isEqualTo(201);
    assertThat(codigoDto.getMnemonico()).isEqualTo("LTS");
    assertThat(codigoDto.getNome()).isEqualTo("Licença para Tratamento de Saúde");
    assertThat(codigoDto.getAfastamento()).isTrue();
    assertThat(codigoDto.getOnus()).isTrue();
    assertThat(codigoDto.getQtdmin()).isEqualByComparingTo(BigDecimal.ONE);
    assertThat(codigoDto.getQtdmax()).isEqualByComparingTo(BigDecimal.valueOf(730));
    assertThat(codigoDto.getDocumentoLegalNumero()).isEqualTo("LEI-8112/1990");
    assertThatThrownBy(() -> codigoMapper.newEntity(null))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void afastamentoMapperDerivesFactsAndDurationDeterministically() {
    var mapper = new AfastamentoMapper();
    var pessoa = new ServidorPessoa();
    pessoa.setNumero(1001L);
    pessoa.setNome("Servidor Afastado Teste");

    var substituto = new ServidorPessoa();
    substituto.setNumero(1002L);
    substituto.setNome("Servidor Substituto Teste");

    var vinculo = new Vinculo();
    vinculo.setId(new VinculoId(1001L, 1));
    vinculo.setMatric("MAT-1001");

    var entity = new Afastamento();
    entity.setId(500L);
    entity.setEmpCodigo(1);
    entity.setNumfunc(1001L);
    entity.setNumvinc(1);
    entity.setPessoa(pessoa);
    entity.setVinculo(vinculo);
    entity.setPessoaSubstituta(substituto);
    entity.setTipofreq("LICENCA");
    entity.setCodfreq(201);
    entity.setDtini(LocalDate.of(2026, 3, 1));
    entity.setDtprevfim(LocalDate.of(2026, 3, 15));
    entity.setSituacao("PENDENTE_HOMOLOGACAO");
    entity.setMotivo("Atestado médico pericial.");
    entity.setNumfuncSubstituto(1002L);
    entity.setNumvincSubstituto(1);

    AfastamentoResponseDTO pendente = mapper.toResponse(entity);
    assertThat(pendente.getId()).isEqualTo(500L);
    assertThat(pendente.getVinculoId()).isEqualTo(new VinculoId(1001L, 1));
    assertThat(pendente.getCodigoFrequenciaId()).isEqualTo(new CodigoFrequenciaId("LICENCA", 201));
    assertThat(pendente.getQuantidadeDias()).isEqualTo(15);
    assertThat(pendente.getPendenteHomologacao()).isTrue();
    assertThat(pendente.getAfastamentoEmCurso()).isFalse();
    assertThat(pendente.getComSubstitutoDesignado()).isTrue();
    assertThat(pendente.getSubstitutoNome()).isEqualTo("Servidor Substituto Teste");

    entity.setSituacao("HOMOLOGADO");
    AfastamentoResponseDTO homologado = mapper.toResponse(entity);
    assertThat(homologado.getPendenteHomologacao()).isFalse();
    assertThat(homologado.getAfastamentoEmCurso()).isTrue();

    entity.setDtfim(LocalDate.of(2026, 3, 10));
    entity.setQuantidadeDias(10);
    AfastamentoResponseDTO encerrado = mapper.toResponse(entity);
    assertThat(encerrado.getAfastamentoEmCurso()).isFalse();
    assertThat(encerrado.getQuantidadeDias()).isEqualTo(10);
  }

  @Test
  void reactiveDeterminationConfigAndServiceComputeAfastamentoPreview() {
    var defs = new ErgonDeterminationConfig().ergonDeterminations().definitions();
    var afastDef =
        defs.stream()
            .filter(d -> "ergon.afastamento.previa".equals(d.id()))
            .findFirst()
            .orElseThrow();
    assertThat(afastDef.operationId()).isEqualTo("determineErgonAfastamentoPrevia");
    assertThat(afastDef.sourcePaths())
        .containsExactly("/tipofreq", "/codfreq", "/dtini", "/dtprevfim");

    EntityManager em = mock(EntityManager.class);
    var codigo = new CodigoFrequencia();
    codigo.setId(new CodigoFrequenciaId("LICENCA", 201));
    codigo.setNome("Licença para Tratamento de Saúde");
    codigo.setAfastamento(true);
    codigo.setOnus(true);
    when(em.find(CodigoFrequencia.class, new CodigoFrequenciaId("LICENCA", 201)))
        .thenReturn(codigo);

    var service = new ErgonDeterminationService(new TenantContext(), new ErgonReadFacts());
    ReflectionTestUtils.setField(service, "em", em);

    var preview =
        service.previaAfastamento(
            new AfastamentoPreviaDraftDTO(
                "LICENCA", 201, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 10)));
    assertThat(preview.codigoFrequenciaNomePrevisto())
        .isEqualTo("Licença para Tratamento de Saúde");
    assertThat(preview.quantidadeDiasPrevista()).isEqualTo(10);
    assertThat(preview.comOnusPrevisto()).isTrue();

    assertThatThrownBy(
            () ->
                service.previaAfastamento(
                    new AfastamentoPreviaDraftDTO(
                        "LICENCA", 201, LocalDate.of(2026, 4, 10), LocalDate.of(2026, 4, 1))))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("término previsto");
  }

  @Test
  @SuppressWarnings("unchecked")
  void ergonInvariantsEnforcesChronologyModalitySexSubstituteAndOverlapRules() {
    EntityManager em = mock(EntityManager.class);
    var invariants = new ErgonInvariants();
    ReflectionTestUtils.setField(invariants, "em", em);

    var titular = new Vinculo();
    titular.setId(new VinculoId(1001L, 1));
    titular.setEmpCodigo(1);
    titular.setDtexerc(LocalDate.of(2020, 1, 10));
    titular.setSituacao("ATIVO");
    when(em.find(eq(Vinculo.class), eq(new VinculoId(1001L, 1)), any(LockModeType.class)))
        .thenReturn(titular);

    var pessoa = new ServidorPessoa();
    pessoa.setNumero(1001L);
    pessoa.setSexo("M");
    when(em.find(eq(ServidorPessoa.class), eq(1001L), any(LockModeType.class))).thenReturn(pessoa);

    var codigo = new CodigoFrequencia();
    codigo.setId(new CodigoFrequenciaId("LICENCA", 201));
    codigo.setAfastamento(true);
    codigo.setSexo("A");
    codigo.setQtdmin(BigDecimal.valueOf(5));
    codigo.setQtdmax(BigDecimal.valueOf(60));
    when(em.find(CodigoFrequencia.class, new CodigoFrequenciaId("LICENCA", 201)))
        .thenReturn(codigo);

    TypedQuery<Afastamento> overlapQuery = mock(TypedQuery.class);
    when(em.createQuery(anyString(), eq(Afastamento.class))).thenReturn(overlapQuery);
    when(overlapQuery.setParameter(anyString(), any())).thenReturn(overlapQuery);
    when(overlapQuery.setFlushMode(any(FlushModeType.class))).thenReturn(overlapQuery);
    when(overlapQuery.getResultList()).thenReturn(List.of());

    var afast = new Afastamento();
    afast.setId(500L);
    afast.setEmpCodigo(1);
    afast.setNumfunc(1001L);
    afast.setNumvinc(1);
    afast.setTipofreq("LICENCA");
    afast.setCodfreq(201);
    afast.setDtini(LocalDate.of(2026, 3, 1));
    afast.setDtprevfim(LocalDate.of(2026, 3, 15));
    afast.setSituacao("HOMOLOGADO");
    afast.setMotivo("Licença médica");

    // Caso válido passa
    invariants.afastamento(afast, true);

    // 1. Duração inferior a qtdmin (5 dias) -> 422
    afast.setDtprevfim(LocalDate.of(2026, 3, 2));
    assertThatThrownBy(() -> invariants.afastamento(afast, true))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("inferior ao mínimo regulamentar");
    afast.setDtprevfim(LocalDate.of(2026, 3, 15));

    // 2. Restrição de sexo (modalidade exclusiva para sexo F) -> 422
    codigo.setSexo("F");
    assertThatThrownBy(() -> invariants.afastamento(afast, true))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("exclusiva para servidores do sexo F");
    codigo.setSexo("I");

    // 3. Auto-substituição -> 422
    afast.setNumfuncSubstituto(1001L);
    afast.setNumvincSubstituto(1);
    assertThatThrownBy(() -> invariants.afastamento(afast, false))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("substituto de si próprio");
    afast.setNumfuncSubstituto(null);
    afast.setNumvincSubstituto(null);

    // 4. Prorrogação sem afastamentoOrigemId -> 422
    afast.setProrrogacao(true);
    assertThatThrownBy(() -> invariants.afastamento(afast, false))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("afastamento de origem");
    afast.setProrrogacao(false);

    // 5. Concomitância com outro afastamento HOMOLOGADO -> 409 CONFLICT
    var existente = new Afastamento();
    existente.setId(499L);
    existente.setNumfunc(1001L);
    existente.setNumvinc(1);
    existente.setDtini(LocalDate.of(2026, 3, 10));
    existente.setDtprevfim(LocalDate.of(2026, 3, 20));
    existente.setSituacao("HOMOLOGADO");
    when(overlapQuery.getResultList()).thenReturn(List.of(existente));

    assertThatThrownBy(() -> invariants.afastamento(afast, false))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(
            ex ->
                assertThat(((ResponseStatusException) ex).getStatusCode())
                    .isEqualTo(HttpStatus.CONFLICT));
  }

  @Test
  void partialUpdateDtosAndStateProviderTrackAfastamentoLifecycle() {
    var substDto = new AtualizarSubstitutoAfastamentoDTO();
    substDto.setNumfuncSubstituto(1002L);
    assertThat(substDto.wasSupplied("numfuncSubstituto")).isTrue();
    assertThat(substDto.wasSupplied("numvincSubstituto")).isFalse();

    var atosDto = new AtualizarAtosPublicacaoAfastamentoDTO();
    atosDto.setDocumentoLegalId(99L);
    assertThat(atosDto.wasSupplied("documentoLegalId")).isTrue();
    assertThat(atosDto.wasSupplied("atoPublicacaoId")).isFalse();

    AfastamentoRepository afastRepo = mock(AfastamentoRepository.class);
    var afast = new Afastamento();
    afast.setId(500L);
    afast.setEmpCodigo(1);
    afast.setSituacao("PENDENTE_HOMOLOGACAO");
    afast.setProrrogacao(false);
    when(afastRepo.findById(500L)).thenReturn(Optional.of(afast));

    var stateProvider = new VinculoStateProvider(mock(VinculoRepository.class));
    ReflectionTestUtils.setField(stateProvider, "afastamentos", afastRepo);
    ReflectionTestUtils.setField(stateProvider, "tenant", new TenantContext());

    var pendenteSnap =
        stateProvider.resolve("administracao-pessoal.afastamentos", "500").orElseThrow();
    assertThat(pendenteSnap.state()).isEqualTo("PENDENTE_HOMOLOGACAO");
    assertThat(pendenteSnap.metadata()).containsEntry("emAberto", true);

    afast.setSituacao("HOMOLOGADO");
    afast.setDtfim(LocalDate.of(2026, 3, 15));
    var encerradoSnap =
        stateProvider.resolve("administracao-pessoal.afastamentos", "500").orElseThrow();
    assertThat(encerradoSnap.state()).isEqualTo("ENCERRADO");
    assertThat(encerradoSnap.metadata()).containsEntry("emAberto", false);
  }
}
