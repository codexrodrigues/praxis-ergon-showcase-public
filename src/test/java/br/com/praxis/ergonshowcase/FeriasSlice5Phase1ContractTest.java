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
import br.com.praxis.ergonshowcase.mapper.FeriasGozoMapper;
import br.com.praxis.ergonshowcase.mapper.ParametroContagemFeriasMapper;
import br.com.praxis.ergonshowcase.mapper.PeriodoAquisitivoFeriasMapper;
import br.com.praxis.ergonshowcase.repository.FeriasGozoRepository;
import br.com.praxis.ergonshowcase.repository.ParametroContagemFeriasRepository;
import br.com.praxis.ergonshowcase.repository.PeriodoAquisitivoFeriasRepository;
import br.com.praxis.ergonshowcase.repository.VinculoRepository;
import br.com.praxis.ergonshowcase.service.ErgonDeterminationService;
import br.com.praxis.ergonshowcase.service.ErgonInvariants;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TypedQuery;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * Provas determinísticas de contrato, mapeamento 100%, padrão de precedência (Tarefa 24610 do
 * PCK_FERIAS.PKB), determinações reativas, invariantes e estados do Slice 5 (Férias e Períodos
 * Aquisitivos).
 */
class FeriasSlice5Phase1ContractTest {

  @Test
  void parametroContagemFeriasMapperPreservesAllColumnsAndNormalizesCodes() {
    var tenant = new TenantContext();
    var mapper = new ParametroContagemFeriasMapper(tenant);
    var create = new CreateParametroContagemFeriasDTO();
    create.setEmpCodigo(1);
    create.setRegimeJuridico(" estatutario ");
    create.setTipoVinculo(" efetivo ");
    create.setCategoria(" magisterio ");
    create.setSubcategoria(" docente ");
    create.setDtini(LocalDate.of(2020, 1, 1));
    create.setTemFerias(true);
    create.setTotDiasFer(45);
    create.setMinPerFer(1);
    create.setMaxPerFer(3);
    create.setMinDiasPerFer(10);
    create.setMaxDiasPerFer(45);
    create.setMinDiasPerPrincFer(15);
    create.setAbonoDirGozo(" d ");
    create.setNumDiasAbonoFer(1);
    create.setDenDiasAbonoFer(3);
    create.setMinDiasPvendFer(5);
    create.setMaxDiasVendFer(15);
    create.setAbonoConstFer(true);
    create.setAdiant13Fer(true);
    create.setAdiantSalFer(true);
    create.setEmprestFer(false);
    create.setPermiteGozoAntesPaFer(true);
    create.setPermiteFerPrimPer(false);
    create.setPermiteFerSegunPer(true);
    create.setPermiteFerTercPer(true);
    create.setPermiteFerPerInc(false);
    create.setCriterioPrescricao(" f ");
    create.setPermiteGozoAposPrescricao(false);
    create.setPermiteLancConcPa(false);
    create.setExigeExercicio(true);
    create.setSemPgtoMulta(true);
    create.setContDobroFerias(true);
    create.setMinDiasPeraqfer(10);
    create.setMaxDiasPeraqfer(30);
    create.setTemLicEsp(true);
    create.setAnosLicEsp(5);
    create.setMesesLicEspPer(3);
    create.setVendLicEsp(true);
    create.setAbonoDirGozoLicEsp(" d ");
    create.setMinDiasPvendLicEsp(15);
    create.setMaxDiasVendLicEsp(45);
    create.setNumDiasAbonoLicEsp(1);
    create.setDenDiasAbonoLicEsp(2);
    create.setContDobroLicEsp(true);
    create.setMinDiasPcontDobLicEsp(30);
    create.setMaxDiasContDobLicEsp(90);
    create.setTemProgressao(true);
    create.setAnosProgressao(2);
    create.setTemTempoServ(true);
    create.setAnos1TempoServ(1);
    create.setTemSextaParte(true);
    create.setAnosSextaParte(20);
    create.setDocumentoLegalFeriasId(10L);
    create.setDocumentoLegalProgressaoId(11L);
    create.setObservacoes("  Estatuto do Magistério  ");
    create.setCustomFields(Map.of("portariaRegulamentadora", "PRT-2020"));

    ParametroContagemFerias entity = mapper.newEntity(create);
    entity.setId(501L);
    assertThat(entity.getRegimeJuridico()).isEqualTo("ESTATUTARIO");
    assertThat(entity.getTipoVinculo()).isEqualTo("EFETIVO");
    assertThat(entity.getCategoria()).isEqualTo("MAGISTERIO");
    assertThat(entity.getSubcategoria()).isEqualTo("DOCENTE");
    assertThat(entity.getAbonoDirGozo()).isEqualTo("D");
    assertThat(entity.getCriterioPrescricao()).isEqualTo("F");
    assertThat(entity.getAbonoDirGozoLicEsp()).isEqualTo("D");
    assertThat(entity.getObservacoes()).isEqualTo("Estatuto do Magistério");
    assertThat(entity.getActive()).isTrue();

    var resp = mapper.toResponse(entity);
    assertThat(resp.getId()).isEqualTo(501L);
    assertThat(resp.getTotDiasFer()).isEqualTo(45);
    assertThat(resp.getMaxPerFer()).isEqualTo(3);
    assertThat(resp.getAnosLicEsp()).isEqualTo(5);
    assertThat(resp.getAnosSextaParte()).isEqualTo(20);
    assertThat(resp.getEscopoGlobal()).isFalse();
    assertThat(resp.getPermiteParcelamento()).isTrue();
    assertThat(resp.getPermiteAbonoPecuniario()).isTrue();
    assertThat(resp.getCustomFields()).containsEntry("portariaRegulamentadora", "PRT-2020");
  }

  @Test
  void periodoAquisitivoAndFeriasGozoMappersHonorPrecedencePatternAndAccounting() {
    var tenant = new TenantContext();
    var paMapper = new PeriodoAquisitivoFeriasMapper(tenant);
    var createPa = new CreatePeriodoAquisitivoFeriasDTO();
    createPa.setEmpCodigo(1);
    createPa.setNumfunc(100L);
    createPa.setNumvinc(1);
    createPa.setChave(2);
    createPa.setExercicio(2025);
    createPa.setDtini(LocalDate.of(2024, 1, 1));
    createPa.setDtfim(LocalDate.of(2024, 12, 31));
    createPa.setTotDiasDireito(30);
    createPa.setFaltasApuradas(2);
    createPa.setDiasAfastamentoSuspensivo(0);
    createPa.setDiasGozados(10);
    createPa.setDiasVendidosAbono(10);
    createPa.setDiasConvertidosDobro(0);
    // Padrão de Precedência (Tarefa 24610 do PCK_FERIAS.PKB):
    // 'I' = Indiferente (herda de PARAMETROS_CONTA); 'S'/'N' ou numérico = sobrescreve
    createPa.setAbonoDirGozo(null);
    createPa.setAbonoConstFer(" s ");
    createPa.setPermiteFerEstePer(" n ");
    createPa.setMaxPerFer(2);

    PeriodoAquisitivoFerias pa = paMapper.newEntity(createPa);
    pa.setId(700L);
    assertThat(pa.getAbonoDirGozo()).isEqualTo("I");
    assertThat(pa.getAbonoConstFer()).isEqualTo("S");
    assertThat(pa.getPermiteFerEstePer()).isEqualTo("N");
    assertThat(pa.getMaxPerFer()).isEqualTo(2);
    assertThat(pa.getSaldoDiasDisponivel()).isEqualTo(10);
    assertThat(pa.getSituacao()).isEqualTo("PARCIALMENTE_GOZADO");

    var paResp = paMapper.toResponse(pa);
    assertThat(paResp.getPeriodoResumo()).contains("PA #2 (2025)");
    assertThat(paResp.getVinculoId()).isEqualTo(new VinculoId(100L, 1));
    assertThat(paResp.getPossuiSaldoDisponivel()).isTrue();
    assertThat(paResp.getPossuiOverridesAtivos()).isTrue();
    assertThat(paResp.getEmRiscoPrescricao()).isFalse();

    // Quando a soma quita 100% dos dias de direito, a situação passa a QUITADO
    var updatePa = new UpdatePeriodoAquisitivoFeriasDTO();
    updatePa.setEmpCodigo(1);
    updatePa.setNumfunc(100L);
    updatePa.setNumvinc(1);
    updatePa.setChave(2);
    updatePa.setExercicio(2025);
    updatePa.setDtini(LocalDate.of(2024, 1, 1));
    updatePa.setDtfim(LocalDate.of(2024, 12, 31));
    updatePa.setTotDiasDireito(30);
    updatePa.setDiasGozados(20);
    updatePa.setDiasVendidosAbono(10);
    updatePa.setDiasConvertidosDobro(0);
    paMapper.applyUpdate(pa, updatePa);
    assertThat(pa.getSaldoDiasDisponivel()).isZero();
    assertThat(pa.getSituacao()).isEqualTo("QUITADO");

    // FeriasGozoMapper calcula diasGozo automaticamente pelo intervalo inclusivo dtini..dtprevfim
    var fgMapper = new FeriasGozoMapper(tenant);
    var createFg = new CreateFeriasGozoDTO();
    createFg.setEmpCodigo(1);
    createFg.setNumfunc(100L);
    createFg.setNumvinc(1);
    createFg.setPeriodoAquisitivoId(700L);
    createFg.setChavePeraqfer(2);
    createFg.setOrdemParcela(1);
    createFg.setDtini(LocalDate.of(2025, 7, 1));
    createFg.setDtprevfim(LocalDate.of(2025, 7, 15));
    createFg.setAbonoPecuniario(true);
    createFg.setIniFimAbono(" f ");
    createFg.setDiasAbonoVendidos(10);

    FeriasGozo fg = fgMapper.newEntity(createFg);
    fg.setId(900L);
    fg.setPeriodoAquisitivo(pa);
    assertThat(fg.getDiasGozo()).isEqualTo(15);
    assertThat(fg.getSituacao()).isEqualTo("PROGRAMADA");
    assertThat(fg.getIniFimAbono()).isEqualTo("F");
    var fgResp = fgMapper.toResponse(fg);
    assertThat(fgResp.getParcelaResumo()).contains("1ª Parcela (PA #2)");
    assertThat(fgResp.getPeriodoAquisitivoResumo()).contains("PA #2 (2025)");
    assertThat(fgResp.getComAbonoVendido()).isTrue();
    assertThat(fgResp.getComSubstitutoDesignado()).isFalse();
    assertThat(fgResp.getParcelaInterrompida()).isFalse();
  }

  @Test
  void reactiveDeterminationsAndDefinitionsCoverAllThreeSlice5Resources() {
    var config = new ErgonDeterminationConfig();
    var definitions = config.ergonDeterminations().definitions();
    assertThat(definitions)
        .extracting(d -> d.id())
        .contains(
            "ergon.parametroContagemFerias.previa",
            "ergon.periodoAquisitivoFerias.previa",
            "ergon.feriasGozo.previa");

    EntityManager em = mock(EntityManager.class);
    var pa = new PeriodoAquisitivoFerias();
    pa.setId(700L);
    pa.setEmpCodigo(1);
    pa.setChave(3);
    pa.setTotDiasDireito(30);
    when(em.find(PeriodoAquisitivoFerias.class, 700L)).thenReturn(pa);

    var service = new ErgonDeterminationService(new TenantContext(), null);
    ReflectionTestUtils.setField(service, "em", em);

    var draftParam = new ParametroContagemFeriasPreviaDraftDTO();
    draftParam.setTemFerias(true);
    draftParam.setTotDiasFer(30);
    draftParam.setAbonoDirGozo("D");
    draftParam.setNumDiasAbonoFer(1);
    draftParam.setDenDiasAbonoFer(3);
    var previewParam = service.previaParametroContagemFerias(draftParam);
    assertThat(previewParam.getMaxDiasVendFer()).isEqualTo(10);
    assertThat(previewParam.getMinDiasPerPrincFer()).isEqualTo(14);

    var draftPa = new PeriodoAquisitivoPreviaDraftDTO();
    draftPa.setDtini(LocalDate.of(2024, 1, 1));
    draftPa.setDtfim(LocalDate.of(2024, 12, 31));
    draftPa.setTotDiasDireito(30);
    draftPa.setDiasGozados(10);
    draftPa.setDiasVendidosAbono(5);
    draftPa.setDiasConvertidosDobro(0);
    var previewPa = service.previaPeriodoAquisitivo(draftPa);
    assertThat(previewPa.getSaldoDiasDisponivel()).isEqualTo(15);
    assertThat(previewPa.getDtPrescricao()).isEqualTo(LocalDate.of(2026, 12, 31));

    var draftFg = new FeriasGozoPreviaDraftDTO();
    draftFg.setPeriodoAquisitivoId(700L);
    draftFg.setDtini(LocalDate.of(2025, 9, 1));
    draftFg.setDiasGozo(10);
    var previewFg = service.previaFeriasGozo(draftFg);
    assertThat(previewFg.getChavePeraqfer()).isEqualTo(3);
    assertThat(previewFg.getTotDiasDireitoPa()).isEqualTo(30);
    assertThat(previewFg.getDiasGozo()).isEqualTo(10);
    assertThat(previewFg.getDtprevfim()).isEqualTo(LocalDate.of(2025, 9, 10));
  }

  @Test
  @SuppressWarnings("unchecked")
  void invariantsEnforceStatutoryRulesAndPrecedenceAcrossAllThreeResources() {
    EntityManager em = mock(EntityManager.class);
    var tenant = new TenantContext();
    var invariants = new ErgonInvariants();
    ReflectionTestUtils.setField(invariants, "em", em);
    ReflectionTestUtils.setField(invariants, "tenant", tenant);

    var orgao = new Orgao();
    orgao.setEmpresa(1);
    orgao.setAtiva(true);
    when(em.find(eq(Orgao.class), eq(1), any(LockModeType.class))).thenReturn(orgao);

    var pessoa = new ServidorPessoa();
    pessoa.setNumero(100L);
    when(em.find(eq(ServidorPessoa.class), eq(100L), any(LockModeType.class))).thenReturn(pessoa);

    var vinculo = new Vinculo();
    vinculo.setId(new VinculoId(100L, 1));
    vinculo.setEmpCodigo(1);
    vinculo.setSituacao("ATIVO");
    vinculo.setDtexerc(LocalDate.of(2020, 1, 1));
    when(em.find(eq(Vinculo.class), eq(new VinculoId(100L, 1)), any(LockModeType.class)))
        .thenReturn(vinculo);

    TypedQuery<DefinicaoCampoCustomizado> cfQuery = mock(TypedQuery.class);
    when(cfQuery.setParameter(anyString(), any())).thenReturn(cfQuery);
    when(cfQuery.setFlushMode(any(FlushModeType.class))).thenReturn(cfQuery);
    when(cfQuery.getResultList()).thenReturn(List.of());
    when(em.createQuery(
            anyString(), eq(DefinicaoCampoCustomizado.class)))
        .thenReturn(cfQuery);

    // 1. ParametroContagemFerias: minPerFer > maxPerFer -> 422
    var param = new ParametroContagemFerias();
    param.setEmpCodigo(1);
    param.setRegimeJuridico("ESTATUTARIO");
    param.setTipoVinculo("EFETIVO");
    param.setCategoria("GERAL");
    param.setDtini(LocalDate.of(2020, 1, 1));
    param.setTemFerias(true);
    param.setTotDiasFer(30);
    param.setMinPerFer(4);
    param.setMaxPerFer(2);
    param.setMinDiasPerFer(10);
    param.setMaxDiasPerFer(30);
    assertThatThrownBy(() -> invariants.parametroContagemFerias(param, true))
        .isInstanceOfAny(ResponseStatusException.class, org.praxisplatform.rules.violation.PraxisRuleViolationException.class)
        .extracting(ex -> ErgonFailureAssertions.status(ex))
        .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

    param.setMinPerFer(1);
    param.setMaxPerFer(3);
    invariants.parametroContagemFerias(param, true);

    // 1b. ParametroContagemFerias: sobreposição de vigência ativa -> 409 CONFLICT
    var existingParam = new ParametroContagemFerias();
    existingParam.setId(99L);
    existingParam.setEmpCodigo(1);
    existingParam.setRegimeJuridico("ESTATUTARIO");
    existingParam.setTipoVinculo("EFETIVO");
    existingParam.setCategoria("GERAL");
    existingParam.setDtini(LocalDate.of(2019, 1, 1));
    existingParam.setDtfim(null);
    existingParam.setActive(true);
    TypedQuery<ParametroContagemFerias> paramQuery = mock(TypedQuery.class);
    when(paramQuery.setParameter(anyString(), any())).thenReturn(paramQuery);
    when(paramQuery.setFlushMode(any(FlushModeType.class))).thenReturn(paramQuery);
    when(paramQuery.getResultList()).thenReturn(List.of(existingParam));
    when(em.createQuery(anyString(), eq(ParametroContagemFerias.class))).thenReturn(paramQuery);

    assertThatThrownBy(() -> invariants.parametroContagemFerias(param, true))
        .isInstanceOfAny(ResponseStatusException.class, org.praxisplatform.rules.violation.PraxisRuleViolationException.class)
        .extracting(ex -> ErgonFailureAssertions.status(ex))
        .isEqualTo(HttpStatus.CONFLICT);
    when(paramQuery.getResultList()).thenReturn(List.of());

    // A fronteira estrutural encaminha inclusive um período prescrito à decisão governada.
    var paService = mock(br.com.praxis.ergonshowcase.service.PeriodoAquisitivoFeriasService.class);
    var fgService = mock(br.com.praxis.ergonshowcase.service.FeriasGozoService.class);
    ReflectionTestUtils.setField(invariants, "periodoAquisitivoFeriasService", paService);
    ReflectionTestUtils.setField(invariants, "feriasGozoService", fgService);
    var pa = new PeriodoAquisitivoFerias();
    pa.setId(50L);
    pa.setEmpCodigo(1);
    pa.setNumfunc(100L);
    pa.setNumvinc(1);
    pa.setChave(1);
    pa.setSituacao("PRESCRITO");
    when(em.find(eq(PeriodoAquisitivoFerias.class), eq(50L), any(LockModeType.class))).thenReturn(pa);
    invariants.periodoAquisitivoFerias(pa, true);
    org.mockito.Mockito.verify(paService).validatePeriodoAquisitivoFerias(pa);

    var fg = new FeriasGozo();
    fg.setEmpCodigo(1);
    fg.setNumfunc(100L);
    fg.setNumvinc(1);
    fg.setPeriodoAquisitivoId(50L);
    fg.setChavePeraqfer(99);
    invariants.feriasGozo(fg, true);
    var chamadas = org.mockito.Mockito.inOrder(fgService);
    chamadas.verify(fgService).hydrateParentPeriodoAquisitivo(fg);
    chamadas.verify(fgService).validateFeriasGozo(fg);

  }

  @Test
  void vinculoStateProviderPublishesStateAndFactsForAllThreeSlice5Resources() {
    var paramRepo = mock(ParametroContagemFeriasRepository.class);
    var paRepo = mock(PeriodoAquisitivoFeriasRepository.class);
    var fgRepo = mock(FeriasGozoRepository.class);
    var provider = StateSnapshotFixture.provider(StateSnapshotFixture.service(br.com.praxis.ergonshowcase.service.ParametroContagemFeriasService.class, paramRepo),
        StateSnapshotFixture.service(br.com.praxis.ergonshowcase.service.PeriodoAquisitivoFeriasService.class, paRepo),
        StateSnapshotFixture.service(br.com.praxis.ergonshowcase.service.FeriasGozoService.class, fgRepo));


    var param = new ParametroContagemFerias();
    param.setId(10L);
    param.setEmpCodigo(null);
    param.setActive(true);
    param.setTemFerias(true);
    param.setMaxPerFer(3);
    param.setAbonoDirGozo("D");
    param.setDtfim(null);
    when(paramRepo.findById(10L)).thenReturn(Optional.of(param));

    var paramSnap =
        provider.resolve("administracao-pessoal.parametros-contagem-ferias", "10").orElseThrow();
    assertThat(paramSnap.state()).isEqualTo("ATIVO");
    assertThat(paramSnap.metadata())
        .containsEntry("temFerias", true)
        .containsEntry("vigenciaAberta", true)
        .containsEntry("escopoGlobal", true)
        .containsEntry("permiteParcelamento", true)
        .containsEntry("permiteAbonoPecuniario", true);

    var pa = new PeriodoAquisitivoFerias();
    pa.setId(20L);
    pa.setEmpCodigo(1);
    pa.setSituacao("DISPONIVEL");
    pa.setSaldoDiasDisponivel(20);
    pa.setMaxPerFer(2);
    pa.setGeradoPorAverbacao(true);
    when(paRepo.findById(20L)).thenReturn(Optional.of(pa));

    var paSnap =
        provider
            .resolve("administracao-pessoal.periodos-aquisitivos-ferias", "20")
            .orElseThrow();
    assertThat(paSnap.state()).isEqualTo("DISPONIVEL");
    assertThat(paSnap.metadata())
        .containsEntry("saldoPositivo", true)
        .containsEntry("possuiSaldoDisponivel", true)
        .containsEntry("possuiOverridesAtivos", true)
        .containsEntry("geradoPorAverbacao", true);

    var fg = new FeriasGozo();
    fg.setId(30L);
    fg.setEmpCodigo(1);
    fg.setSituacao("INTERROMPIDA");
    fg.setAbonoPecuniario(true);
    fg.setNumfuncSubstituto(200L);
    fg.setNumvincSubstituto(1);
    fg.setDtInterrupcao(LocalDate.of(2025, 2, 10));
    when(fgRepo.findById(30L)).thenReturn(Optional.of(fg));

    var fgSnap = provider.resolve("administracao-pessoal.ferias-gozo", "30").orElseThrow();
    assertThat(fgSnap.state()).isEqualTo("INTERROMPIDA");
    assertThat(fgSnap.metadata())
        .containsEntry("abonoPecuniario", true)
        .containsEntry("comAbonoVendido", true)
        .containsEntry("comSubstitutoDesignado", true)
        .containsEntry("interrompida", true)
        .containsEntry("parcelaInterrompida", true);
  }

  @Test
  void quickFiltersDeserializeCleanlyAndGlobalMapperPreservesNullEmpCodigo() throws Exception {
    var tenant = new TenantContext();
    var paramMapper = new ParametroContagemFeriasMapper(tenant);
    var createGlobal = new CreateParametroContagemFeriasDTO();
    createGlobal.setEmpCodigo(null);
    createGlobal.setRegimeJuridico("ESTATUTARIO");
    createGlobal.setTipoVinculo("EFETIVO");
    createGlobal.setCategoria("GERAL");
    createGlobal.setDtini(LocalDate.of(2020, 1, 1));
    var mappedGlobal = paramMapper.newEntity(createGlobal);
    assertThat(mappedGlobal.getEmpCodigo()).isNull();

    var json =
        new com.fasterxml.jackson.databind.ObjectMapper()
            .configure(
                com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                true);
    for (var qf :
        br.com.praxis.ergonshowcase.controller.ParametroContagemFeriasController.class
            .getAnnotation(org.praxisplatform.uischema.annotation.ApiResource.class)
            .quickFilters()) {
      assertThat(json.readValue(qf.filter(), ParametroContagemFeriasFilterDTO.class)).isNotNull();
    }
    for (var qf :
        br.com.praxis.ergonshowcase.controller.PeriodoAquisitivoFeriasController.class
            .getAnnotation(org.praxisplatform.uischema.annotation.ApiResource.class)
            .quickFilters()) {
      assertThat(json.readValue(qf.filter(), PeriodoAquisitivoFeriasFilterDTO.class)).isNotNull();
    }
    for (var qf :
        br.com.praxis.ergonshowcase.controller.FeriasGozoController.class
            .getAnnotation(org.praxisplatform.uischema.annotation.ApiResource.class)
            .quickFilters()) {
      assertThat(json.readValue(qf.filter(), FeriasGozoFilterDTO.class)).isNotNull();
    }
  }
}
