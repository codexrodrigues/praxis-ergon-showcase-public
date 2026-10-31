package br.com.praxis.ergonshowcase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.praxis.ergonshowcase.bulk.ErgonBulkDomain;
import br.com.praxis.ergonshowcase.bulk.ErgonBulkDomain.Resource;
import br.com.praxis.ergonshowcase.config.TenantContext;
import br.com.praxis.ergonshowcase.config.VinculoStateProvider;
import br.com.praxis.ergonshowcase.controller.FeriasGozoController;
import br.com.praxis.ergonshowcase.controller.ParametroContagemFeriasController;
import br.com.praxis.ergonshowcase.controller.PeriodoAquisitivoFeriasController;
import br.com.praxis.ergonshowcase.controller.RadarFeriasPrescricaoController;
import br.com.praxis.ergonshowcase.controller.VinculoController;
import br.com.praxis.ergonshowcase.domain.Afastamento;
import br.com.praxis.ergonshowcase.domain.DefinicaoCampoCustomizado;
import br.com.praxis.ergonshowcase.domain.DocumentoLegal;
import br.com.praxis.ergonshowcase.domain.FeriasGozo;
import br.com.praxis.ergonshowcase.domain.Frequencia;
import br.com.praxis.ergonshowcase.domain.Orgao;
import br.com.praxis.ergonshowcase.domain.ParametroContagemFerias;
import br.com.praxis.ergonshowcase.domain.PeriodoAquisitivoFerias;
import br.com.praxis.ergonshowcase.domain.RadarFeriasPrescricao;
import br.com.praxis.ergonshowcase.domain.ServidorPessoa;
import br.com.praxis.ergonshowcase.domain.Vinculo;
import br.com.praxis.ergonshowcase.domain.VinculoId;
import br.com.praxis.ergonshowcase.dto.AlterarVigenciaParametroFeriasDTO;
import br.com.praxis.ergonshowcase.dto.AtualizarAbonoFinanceiroFeriasGozoDTO;
import br.com.praxis.ergonshowcase.dto.AtualizarLicencaProgressaoParametroFeriasDTO;
import br.com.praxis.ergonshowcase.dto.AtualizarOverridesPeriodoAquisitivoDTO;
import br.com.praxis.ergonshowcase.dto.AtualizarParcelamentoAbonoParametroFeriasDTO;
import br.com.praxis.ergonshowcase.dto.AtualizarPrescricaoMultaPeriodoAquisitivoDTO;
import br.com.praxis.ergonshowcase.dto.AtualizarSubstitutoFolhaFeriasGozoDTO;
import br.com.praxis.ergonshowcase.dto.ConverterDobroPeriodoAquisitivoDTO;
import br.com.praxis.ergonshowcase.dto.CreateFeriasGozoDTO;
import br.com.praxis.ergonshowcase.dto.FeriasGozoDados;
import br.com.praxis.ergonshowcase.dto.InterromperFeriasGozoDTO;
import br.com.praxis.ergonshowcase.dto.ParametroContagemFeriasDados;
import br.com.praxis.ergonshowcase.dto.PeriodoAquisitivoFeriasDados;
import br.com.praxis.ergonshowcase.dto.RadarFeriasPrescricaoFilterDTO;
import br.com.praxis.ergonshowcase.dto.TransicaoFeriasGozoDTO;
import br.com.praxis.ergonshowcase.dto.TransicaoPeriodoAquisitivoFeriasDTO;
import br.com.praxis.ergonshowcase.mapper.FeriasGozoMapper;
import br.com.praxis.ergonshowcase.mapper.ParametroContagemFeriasMapper;
import br.com.praxis.ergonshowcase.mapper.PeriodoAquisitivoFeriasMapper;
import br.com.praxis.ergonshowcase.mapper.RadarFeriasPrescricaoMapper;
import br.com.praxis.ergonshowcase.repository.FeriasGozoRepository;
import br.com.praxis.ergonshowcase.repository.ParametroContagemFeriasRepository;
import br.com.praxis.ergonshowcase.repository.PeriodoAquisitivoFeriasRepository;
import br.com.praxis.ergonshowcase.repository.RadarFeriasPrescricaoRepository;
import br.com.praxis.ergonshowcase.repository.VinculoRepository;
import br.com.praxis.ergonshowcase.service.ErgonInvariants;
import br.com.praxis.ergonshowcase.service.ErgonReadFacts;
import br.com.praxis.ergonshowcase.service.FeriasGozoService;
import br.com.praxis.ergonshowcase.service.ParametroContagemFeriasService;
import br.com.praxis.ergonshowcase.service.PeriodoAquisitivoFeriasService;
import br.com.praxis.ergonshowcase.service.RadarFeriasPrescricaoService;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TypedQuery;
import jakarta.validation.Validation;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.praxisplatform.uischema.annotation.ApiResource;
import org.praxisplatform.uischema.annotation.BulkEditable;
import org.praxisplatform.uischema.annotation.UiAnalytics;
import org.praxisplatform.uischema.annotation.UiSurface;
import org.praxisplatform.uischema.annotation.WorkflowAction;
import org.praxisplatform.uischema.bulk.BulkChangeOperator;
import org.praxisplatform.uischema.bulk.BulkEditableFields;
import org.praxisplatform.uischema.bulk.BulkFieldChange;
import org.praxisplatform.uischema.bulk.BulkMode;
import org.praxisplatform.uischema.concurrency.ResourceVersionEtagService;
import org.praxisplatform.uischema.concurrency.ResourceVersionPreconditionException;
import org.praxisplatform.uischema.concurrency.ResourceVersionScope;
import org.praxisplatform.uischema.concurrency.ResourceVersionScopeProvider;
import org.praxisplatform.uischema.concurrency.ResourceVersionUpdatePrecondition;
import org.praxisplatform.uischema.exporting.CollectionExportFormat;
import org.praxisplatform.uischema.exporting.CollectionExportRequest;
import org.praxisplatform.uischema.exporting.CollectionExportScope;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * Suíte de contrato da Fase 2 do Slice 5 (Issue #7): ciclo de vida completo com {@code
 * @WorkflowAction} ({@code 200 OK}, {@code 409 Conflict} em estado inválido e {@code 412
 * Precondition Failed} em {@code If-Match} divergente), {@code @ResourceIntent} ({@code PATCH}),
 * recálculo automático de {@code saldoDiasDisponivel} e {@code situacao} em {@code
 * PeriodoAquisitivoFerias}, Bulking nos 3 modos ({@code UNIFORM_UPDATE}, {@code PER_ITEM_UPDATE},
 * {@code DOMAIN_COMMAND}) e contrato completo do {@code RadarFeriasPrescricao}.
 */
class FeriasSlice5Phase2ContractTest {

  private static final ResourceVersionScope SCOPE = new ResourceVersionScope("1");

  private static <I> ResourceVersionUpdatePrecondition<I> precondition(
      ResourceVersionEtagService etags, String resourceKey, I id, long version) {
    String etag = etags.create(SCOPE, resourceKey, id, version);
    return new ResourceVersionUpdatePrecondition<>(etags, etag, SCOPE, resourceKey, id);
  }

  private static <I> ResourceVersionUpdatePrecondition<I> stalePrecondition(
      ResourceVersionEtagService etags, String resourceKey, I id, long staleVersion) {
    String etag = etags.create(SCOPE, resourceKey, id, staleVersion);
    return new ResourceVersionUpdatePrecondition<>(etags, etag, SCOPE, resourceKey, id);
  }

  @SuppressWarnings("unchecked")
  private static ErgonInvariants createMockedInvariants(EntityManager em, TenantContext tenant) {
    var invariants = new ErgonInvariants();
    ReflectionTestUtils.setField(invariants, "em", em);
    ReflectionTestUtils.setField(invariants, "tenant", tenant);

    var orgao = new Orgao();
    orgao.setEmpresa(1);
    orgao.setNome("Prefeitura Municipal");
    orgao.setAtiva(true);
    when(em.find(eq(Orgao.class), eq(1), any(LockModeType.class))).thenReturn(orgao);
    when(em.find(eq(Orgao.class), eq(1))).thenReturn(orgao);

    var pessoa = new ServidorPessoa();
    pessoa.setNumero(100L);
    pessoa.setNome("Maria da Silva");
    when(em.find(eq(ServidorPessoa.class), eq(100L), any(LockModeType.class))).thenReturn(pessoa);
    when(em.find(eq(ServidorPessoa.class), eq(100L))).thenReturn(pessoa);

    var vinculo = new Vinculo();
    vinculo.setId(new VinculoId(100L, 1));
    vinculo.setEmpCodigo(1);
    vinculo.setMatric("MAT-100-1");
    vinculo.setSituacao("ATIVO");
    vinculo.setRegimejur("ESTATUTARIO");
    vinculo.setTipovinc("EFETIVO");
    vinculo.setCategoria("GERAL");
    vinculo.setDtexerc(LocalDate.of(2020, 1, 1));
    when(em.find(eq(Vinculo.class), eq(new VinculoId(100L, 1)), any(LockModeType.class)))
        .thenReturn(vinculo);
    when(em.find(eq(Vinculo.class), eq(new VinculoId(100L, 1)))).thenReturn(vinculo);

    var doc = new DocumentoLegal();
    doc.setId(10L);
    doc.setEmpCodigo(1);
    doc.setNumeroDocumento("PORT-2025/10");
    when(em.find(eq(DocumentoLegal.class), eq(10L), any(LockModeType.class))).thenReturn(doc);
    when(em.find(eq(DocumentoLegal.class), eq(10L))).thenReturn(doc);

    TypedQuery<DefinicaoCampoCustomizado> cfQuery = mock(TypedQuery.class);
    when(cfQuery.setParameter(anyString(), any())).thenReturn(cfQuery);
    when(cfQuery.setFlushMode(any(FlushModeType.class))).thenReturn(cfQuery);
    when(cfQuery.getResultList()).thenReturn(List.of());
    when(em.createQuery(anyString(), eq(DefinicaoCampoCustomizado.class))).thenReturn(cfQuery);

    TypedQuery<ParametroContagemFerias> paramQuery = mock(TypedQuery.class);
    when(paramQuery.setParameter(anyString(), any())).thenReturn(paramQuery);
    when(paramQuery.setFlushMode(any(FlushModeType.class))).thenReturn(paramQuery);
    when(paramQuery.getResultList()).thenReturn(List.of());
    when(em.createQuery(anyString(), eq(ParametroContagemFerias.class))).thenReturn(paramQuery);

    TypedQuery<PeriodoAquisitivoFerias> paQuery = mock(TypedQuery.class);
    when(paQuery.setParameter(anyString(), any())).thenReturn(paQuery);
    when(paQuery.setFlushMode(any(FlushModeType.class))).thenReturn(paQuery);
    when(paQuery.getResultList()).thenReturn(List.of());
    when(em.createQuery(anyString(), eq(PeriodoAquisitivoFerias.class))).thenReturn(paQuery);

    TypedQuery<FeriasGozo> fgQuery = mock(TypedQuery.class);
    when(fgQuery.setParameter(anyString(), any())).thenReturn(fgQuery);
    when(fgQuery.setFlushMode(any(FlushModeType.class))).thenReturn(fgQuery);
    when(fgQuery.getResultList()).thenReturn(List.of());
    when(em.createQuery(anyString(), eq(FeriasGozo.class))).thenReturn(fgQuery);

    return invariants;
  }

  @Test
  void workflowActionsEnforceStateTransitionsConflict409AndIfMatchPrecondition412AcrossAllSlice5Resources() {
    EntityManager em = mock(EntityManager.class);
    var tenant = new TenantContext();
    var invariants = createMockedInvariants(em, tenant);
    var etags = new ResourceVersionEtagService("slice5-secret");
    var readFacts = mock(ErgonReadFacts.class);

    // 1. ParametroContagemFerias: ativar / encerrarVigencia / desativar
    var paramRepo = mock(ParametroContagemFeriasRepository.class);
    var paramMapper = new ParametroContagemFeriasMapper(tenant);
    var paramService = new ParametroContagemFeriasService(paramRepo, paramMapper, tenant);
    ReflectionTestUtils.setField(paramService, "entityManager", em);
    ReflectionTestUtils.setField(paramService, "invariants", invariants);
    ReflectionTestUtils.setField(paramService, "readFacts", readFacts);

    var param = new ParametroContagemFerias();
    param.setId(101L);
    param.setEmpCodigo(1);
    param.setRegimeJuridico("ESTATUTARIO");
    param.setTipoVinculo("EFETIVO");
    param.setCategoria("GERAL");
    param.setDtini(LocalDate.of(2022, 1, 1));
    param.setTemFerias(true);
    param.setTotDiasFer(30);
    param.setMinPerFer(1);
    param.setMaxPerFer(3);
    param.setMinDiasPerFer(10);
    param.setMaxDiasPerFer(30);
    param.setActive(true);
    param.setVersion(1L);
    when(em.find(eq(ParametroContagemFerias.class), eq(101L), any(LockModeType.class)))
        .thenReturn(param);
    when(paramRepo.findById(101L)).thenReturn(Optional.of(param));
    when(paramRepo.save(any(ParametroContagemFerias.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    var vigenciaDto = new AlterarVigenciaParametroFeriasDTO();
    vigenciaDto.setDtfim(LocalDate.of(2025, 12, 31));
    vigenciaDto.setDocumentoLegalFeriasId(10L);
    vigenciaDto.setObservacoes("Encerramento normativo");

    // 409 ao tentar ativar parâmetro já ativo
    assertThatThrownBy(
            () ->
                paramService.ativarParametro(
                    101L,
                    vigenciaDto,
                    precondition(
                        etags, "administracao-pessoal.parametros-contagem-ferias", 101L, 1L)))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
        .isEqualTo(HttpStatus.CONFLICT);

    // 412 ao enviar If-Match divergente
    assertThatThrownBy(
            () ->
                paramService.encerrarVigenciaParametro(
                    101L,
                    vigenciaDto,
                    stalePrecondition(
                        etags, "administracao-pessoal.parametros-contagem-ferias", 101L, 99L)))
        .isInstanceOf(ResourceVersionPreconditionException.class);

    // 200 ao encerrar vigência e desativar
    var encerrado =
        paramService.encerrarVigenciaParametro(
            101L,
            vigenciaDto,
            precondition(etags, "administracao-pessoal.parametros-contagem-ferias", 101L, 1L));
    assertThat(encerrado.body().getDtfim()).isEqualTo(LocalDate.of(2025, 12, 31));

    var desativado =
        paramService.inativarParametro(
            101L,
            vigenciaDto,
            precondition(etags, "administracao-pessoal.parametros-contagem-ferias", 101L, 1L));
    assertThat(desativado.body().getActive()).isFalse();

    // 200 ao reativar parâmetro inativo
    vigenciaDto.setDtfim(null);
    var reativadoParam =
        paramService.ativarParametro(
            101L,
            vigenciaDto,
            precondition(etags, "administracao-pessoal.parametros-contagem-ferias", 101L, 1L));
    assertThat(reativadoParam.body().getActive()).isTrue();

    // 2. PeriodoAquisitivoFerias: homologar, suspender, reativar, converterDobro, prescrever, cancelar
    var paRepo = mock(PeriodoAquisitivoFeriasRepository.class);
    var paMapper = new PeriodoAquisitivoFeriasMapper(tenant);
    var paService = new PeriodoAquisitivoFeriasService(paRepo, paMapper, tenant);
    ReflectionTestUtils.setField(paService, "entityManager", em);
    ReflectionTestUtils.setField(paService, "invariants", invariants);
    ReflectionTestUtils.setField(paService, "readFacts", readFacts);

    var pa = new PeriodoAquisitivoFerias();
    pa.setId(202L);
    pa.setEmpCodigo(1);
    pa.setNumfunc(100L);
    pa.setNumvinc(1);
    pa.setChave(1);
    pa.setExercicio(2025);
    pa.setDtini(LocalDate.of(2024, 1, 1));
    pa.setDtfim(LocalDate.of(2024, 12, 31));
    pa.setTotDiasDireito(30);
    pa.setSaldoDiasDisponivel(30);
    pa.setSituacao("EM_AQUISICAO");
    pa.setVersion(2L);
    when(em.find(eq(PeriodoAquisitivoFerias.class), eq(202L), any(LockModeType.class)))
        .thenReturn(pa);
    when(paRepo.findById(202L)).thenReturn(Optional.of(pa));
    when(paRepo.save(any(PeriodoAquisitivoFerias.class))).thenAnswer(inv -> inv.getArgument(0));

    var transicaoPa = new TransicaoPeriodoAquisitivoFeriasDTO();
    transicaoPa.setDocumentoLegalId(10L);
    transicaoPa.setObservacoes("Ato de transição do PA");

    // 409 ao tentar reativar quando não está SUSPENSO
    assertThatThrownBy(
            () ->
                paService.reativarPeriodoAquisitivo(
                    202L,
                    transicaoPa,
                    precondition(
                        etags, "administracao-pessoal.periodos-aquisitivos-ferias", 202L, 2L)))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
        .isEqualTo(HttpStatus.CONFLICT);

    // 412 em If-Match divergente no PA
    assertThatThrownBy(
            () ->
                paService.homologarPeriodoAquisitivo(
                    202L,
                    transicaoPa,
                    stalePrecondition(
                        etags, "administracao-pessoal.periodos-aquisitivos-ferias", 202L, 5L)))
        .isInstanceOf(ResourceVersionPreconditionException.class);

    // Homologar: EM_AQUISICAO -> DISPONIVEL
    var paHomologado =
        paService.homologarPeriodoAquisitivo(
            202L,
            transicaoPa,
            precondition(etags, "administracao-pessoal.periodos-aquisitivos-ferias", 202L, 2L));
    assertThat(paHomologado.body().getSituacao()).isEqualTo("DISPONIVEL");

    // Suspender: DISPONIVEL -> SUSPENSO
    var paSuspenso =
        paService.suspenderPeriodoAquisitivo(
            202L,
            transicaoPa,
            precondition(etags, "administracao-pessoal.periodos-aquisitivos-ferias", 202L, 2L));
    assertThat(paSuspenso.body().getSituacao()).isEqualTo("SUSPENSO");

    // Reativar: SUSPENSO -> DISPONIVEL
    var paReativado =
        paService.reativarPeriodoAquisitivo(
            202L,
            transicaoPa,
            precondition(etags, "administracao-pessoal.periodos-aquisitivos-ferias", 202L, 2L));
    assertThat(paReativado.body().getSituacao()).isEqualTo("DISPONIVEL");

    // Converter em dobro: 10 dias -> PARCIALMENTE_GOZADO, saldo = 20
    var dobroDto = new ConverterDobroPeriodoAquisitivoDTO();
    dobroDto.setDiasConvertidosDobro(10);
    dobroDto.setDocumentoLegalId(10L);
    dobroDto.setObservacoes("Averbação em dobro");
    var paDobro =
        paService.converterDobroPeriodoAquisitivo(
            202L,
            dobroDto,
            precondition(etags, "administracao-pessoal.periodos-aquisitivos-ferias", 202L, 2L));
    assertThat(paDobro.body().getDiasConvertidosDobro()).isEqualTo(10);
    assertThat(paDobro.body().getSaldoDiasDisponivel()).isEqualTo(20);
    assertThat(paDobro.body().getSituacao()).isEqualTo("PARCIALMENTE_GOZADO");

    // Prescrever: PARCIALMENTE_GOZADO -> PRESCRITO, saldo = 0
    var paPrescrito =
        paService.prescreverPeriodoAquisitivo(
            202L,
            transicaoPa,
            precondition(etags, "administracao-pessoal.periodos-aquisitivos-ferias", 202L, 2L));
    assertThat(paPrescrito.body().getSituacao()).isEqualTo("PRESCRITO");
    assertThat(paPrescrito.body().getSaldoDiasDisponivel()).isZero();

    // Cancelar a partir de DISPONIVEL -> CANCELADO
    pa.setSituacao("DISPONIVEL");
    pa.setSaldoDiasDisponivel(20);
    var paCancelado =
        paService.cancelarPeriodoAquisitivo(
            202L,
            transicaoPa,
            precondition(etags, "administracao-pessoal.periodos-aquisitivos-ferias", 202L, 2L));
    assertThat(paCancelado.body().getSituacao()).isEqualTo("CANCELADO");
    assertThat(paCancelado.body().getSaldoDiasDisponivel()).isZero();

    // 409 ao tentar cancelar novamente um PA já CANCELADO
    assertThatThrownBy(
            () ->
                paService.cancelarPeriodoAquisitivo(
                    202L,
                    transicaoPa,
                    precondition(
                        etags, "administracao-pessoal.periodos-aquisitivos-ferias", 202L, 2L)))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
        .isEqualTo(HttpStatus.CONFLICT);

    // Verificar exposição de todos os 7 @WorkflowAction de item em PeriodoAquisitivoFeriasController (incluindo recalcular-faltas-direito-periodo)
    ResourceVersionScopeProvider scopes = () -> SCOPE;
    var paController = new PeriodoAquisitivoFeriasController(paService, etags, scopes);
    pa.setSituacao("SUSPENSO");
    pa.setSaldoDiasDisponivel(20);
    String validPaEtag =
        etags.create(SCOPE, "administracao-pessoal.periodos-aquisitivos-ferias", 202L, 2L);
    assertThat(paController.reativar(202L, validPaEtag, transicaoPa).getBody().getSituacao())
        .isEqualTo("PARCIALMENTE_GOZADO");

    // Recalcular faltas e direito do PA apurando Frequencia (8 faltas -> 24 dias de direito) e Afastamento (5 dias suspensivos)
    @SuppressWarnings("unchecked")
    TypedQuery<Frequencia> freqQuery = mock(TypedQuery.class);
    when(freqQuery.setParameter(anyString(), any())).thenReturn(freqQuery);
    var falta = new Frequencia();
    falta.setNumfunc(100L);
    falta.setNumvinc(1);
    falta.setDtini(LocalDate.of(2024, 3, 1));
    falta.setDtfim(LocalDate.of(2024, 3, 8));
    falta.setTipofreq("FALTA");
    falta.setSituacao("HOMOLOGADO");
    when(freqQuery.getResultList()).thenReturn(List.of(falta));
    when(em.createQuery(anyString(), eq(Frequencia.class))).thenReturn(freqQuery);

    @SuppressWarnings("unchecked")
    TypedQuery<Afastamento> afastQuery = mock(TypedQuery.class);
    when(afastQuery.setParameter(anyString(), any())).thenReturn(afastQuery);
    var afast = new Afastamento();
    afast.setNumfunc(100L);
    afast.setNumvinc(1);
    afast.setDtini(LocalDate.of(2024, 5, 1));
    afast.setDtfim(LocalDate.of(2024, 5, 5));
    afast.setSituacao("HOMOLOGADO");
    when(afastQuery.getResultList()).thenReturn(List.of(afast));
    when(em.createQuery(anyString(), eq(Afastamento.class))).thenReturn(afastQuery);

    var paRecalculado =
        paController.recalcularFaltasDireito(202L, validPaEtag, transicaoPa).getBody();
    assertThat(paRecalculado.getFaltasApuradas()).isEqualTo(8);
    assertThat(paRecalculado.getDiasAfastamentoSuspensivo()).isEqualTo(5);
    assertThat(paRecalculado.getTotDiasDireito()).isEqualTo(24);
    assertThat(paRecalculado.getSaldoDiasDisponivel()).isEqualTo(14);

    assertThat(paController.cancelar(202L, validPaEtag, transicaoPa).getBody().getSituacao())
        .isEqualTo("CANCELADO");
  }

  @Test
  @SuppressWarnings("unchecked")
  void feriasGozoLifecycleAutomaticallyRecalculatesPeriodoAquisitivoBalanceAndSituacao() {
    EntityManager em = mock(EntityManager.class);
    var tenant = new TenantContext();
    var invariants = createMockedInvariants(em, tenant);
    var etags = new ResourceVersionEtagService("slice5-secret");
    var readFacts = mock(ErgonReadFacts.class);

    var pa = new PeriodoAquisitivoFerias();
    pa.setId(500L);
    pa.setEmpCodigo(1);
    pa.setNumfunc(100L);
    pa.setNumvinc(1);
    pa.setChave(1);
    pa.setExercicio(2025);
    pa.setDtini(LocalDate.of(2024, 1, 1));
    pa.setDtfim(LocalDate.of(2024, 12, 31));
    pa.setTotDiasDireito(30);
    pa.setDiasGozados(0);
    pa.setDiasVendidosAbono(0);
    pa.setDiasConvertidosDobro(0);
    pa.setSaldoDiasDisponivel(30);
    pa.setSituacao("DISPONIVEL");
    pa.setVersion(1L);
    when(em.find(eq(PeriodoAquisitivoFerias.class), eq(500L), any(LockModeType.class)))
        .thenReturn(pa);
    when(em.find(eq(PeriodoAquisitivoFerias.class), eq(500L))).thenReturn(pa);

    List<FeriasGozo> activeParcelas = new ArrayList<>();
    TypedQuery<FeriasGozo> fgQuery = mock(TypedQuery.class);
    when(fgQuery.setParameter(anyString(), any())).thenReturn(fgQuery);
    when(fgQuery.setFlushMode(any(FlushModeType.class))).thenReturn(fgQuery);
    when(fgQuery.getResultList())
        .thenAnswer(
            inv ->
                activeParcelas.stream()
                    .filter(p -> !"CANCELADA".equals(p.getSituacao()))
                    .toList());
    when(em.createQuery(anyString(), eq(FeriasGozo.class))).thenReturn(fgQuery);

    var fgRepo = mock(FeriasGozoRepository.class);
    var fgMapper = new FeriasGozoMapper(tenant);
    var fgService = new FeriasGozoService(fgRepo, fgMapper, tenant);
    ReflectionTestUtils.setField(fgService, "entityManager", em);
    ReflectionTestUtils.setField(fgService, "invariants", invariants);
    ReflectionTestUtils.setField(fgService, "readFacts", readFacts);

    when(fgRepo.save(any(FeriasGozo.class)))
        .thenAnswer(
            inv -> {
              FeriasGozo saved = inv.getArgument(0);
              if (saved.getId() == null) {
                saved.setId(901L);
              }
              saved.setVersion(1L);
              activeParcelas.removeIf(p -> p.getId().equals(saved.getId()));
              activeParcelas.add(saved);
              return saved;
            });

    // 1. Criar 1ª parcela (20 dias de gozo + 10 dias de abono = 30 dias) -> PA fica QUITADO com saldo 0
    var createDto = new CreateFeriasGozoDTO();
    createDto.setEmpCodigo(1);
    createDto.setNumfunc(100L);
    createDto.setNumvinc(1);
    createDto.setPeriodoAquisitivoId(500L);
    createDto.setChavePeraqfer(1);
    createDto.setOrdemParcela(1);
    createDto.setDtini(LocalDate.of(2025, 7, 1));
    createDto.setDtprevfim(LocalDate.of(2025, 7, 20));
    createDto.setDiasGozo(20);
    createDto.setAbonoPecuniario(true);
    createDto.setIniFimAbono("F");
    createDto.setDiasAbonoVendidos(10);

    var created = fgService.create(createDto);
    assertThat(created.body().getSituacao()).isEqualTo("PROGRAMADA");
    assertThat(pa.getDiasGozados()).isEqualTo(20);
    assertThat(pa.getDiasVendidosAbono()).isEqualTo(10);
    assertThat(pa.getSaldoDiasDisponivel()).isZero();
    assertThat(pa.getSituacao()).isEqualTo("QUITADO");

    FeriasGozo parcel = activeParcelas.getFirst();
    when(em.find(eq(FeriasGozo.class), eq(901L), any(LockModeType.class))).thenReturn(parcel);
    when(fgRepo.findById(901L)).thenReturn(Optional.of(parcel));

    // 2. Transições de ciclo de vida: PROGRAMADA -> HOMOLOGADA -> EM_GOZO -> INTERROMPIDA
    var transicao = new TransicaoFeriasGozoDTO();
    transicao.setDocumentoLegalId(10L);
    transicao.setObservacoes("Despacho de férias");

    // 409 ao tentar iniciarGozo antes de homologar
    assertThatThrownBy(
            () ->
                fgService.iniciarGozo(
                    901L,
                    transicao,
                    precondition(etags, "administracao-pessoal.ferias-gozo", 901L, 1L)))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
        .isEqualTo(HttpStatus.CONFLICT);

    // Homologar: PROGRAMADA -> HOMOLOGADA
    var homologada =
        fgService.homologarFeriasGozo(
            901L, transicao, precondition(etags, "administracao-pessoal.ferias-gozo", 901L, 1L));
    assertThat(homologada.body().getSituacao()).isEqualTo("HOMOLOGADA");

    // Iniciar gozo: HOMOLOGADA -> EM_GOZO
    var emGozo =
        fgService.iniciarGozo(
            901L, transicao, precondition(etags, "administracao-pessoal.ferias-gozo", 901L, 1L));
    assertThat(emGozo.body().getSituacao()).isEqualTo("EM_GOZO");

    // Interromper com dtInterrupcao > dtprevfim -> 422 UNPROCESSABLE_ENTITY
    var interromperInvalidoDto = new InterromperFeriasGozoDTO();
    interromperInvalidoDto.setDtInterrupcao(LocalDate.of(2025, 7, 25));
    interromperInvalidoDto.setMotivoInterrupcao("Data posterior ao término previsto");
    assertThatThrownBy(
            () ->
                fgService.interromperFeriasGozo(
                    901L,
                    interromperInvalidoDto,
                    precondition(etags, "administracao-pessoal.ferias-gozo", 901L, 1L)))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
        .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

    // Interromper após 8 dias (01/07 a 09/07 = 8 dias efetivos) -> PA volta a PARCIALMENTE_GOZADO com saldo 12
    var interromperDto = new InterromperFeriasGozoDTO();
    interromperDto.setDtInterrupcao(LocalDate.of(2025, 7, 9));
    interromperDto.setMotivoInterrupcao("Convocação por necessidade imperiosa do serviço");
    interromperDto.setDocumentoLegalId(10L);
    interromperDto.setObservacoes("Portaria de interrupção");

    var interrompida =
        fgService.interromperFeriasGozo(
            901L,
            interromperDto,
            precondition(etags, "administracao-pessoal.ferias-gozo", 901L, 1L));
    assertThat(interrompida.body().getSituacao()).isEqualTo("INTERROMPIDA");
    assertThat(interrompida.body().getDiasGozo()).isEqualTo(8);
    assertThat(pa.getDiasGozados()).isEqualTo(8);
    assertThat(pa.getDiasVendidosAbono()).isEqualTo(10);
    assertThat(pa.getSaldoDiasDisponivel()).isEqualTo(12);
    assertThat(pa.getSituacao()).isEqualTo("PARCIALMENTE_GOZADO");

    // 3. Cancelar parcela programada devolve 100% do saldo ao PA (DISPONIVEL, saldo = 30)
    parcel.setSituacao("PROGRAMADA");
    var cancelada =
        fgService.cancelarFeriasGozo(
            901L, transicao, precondition(etags, "administracao-pessoal.ferias-gozo", 901L, 1L));
    assertThat(cancelada.body().getSituacao()).isEqualTo("CANCELADA");
    assertThat(pa.getDiasGozados()).isZero();
    assertThat(pa.getDiasVendidosAbono()).isZero();
    assertThat(pa.getSaldoDiasDisponivel()).isEqualTo(30);
    assertThat(pa.getSituacao()).isEqualTo("DISPONIVEL");

    // 4. Concluir gozo a partir de EM_GOZO -> GOZADA
    parcel.setSituacao("EM_GOZO");
    parcel.setDiasGozo(15);
    parcel.setAbonoPecuniario(false);
    parcel.setDiasAbonoVendidos(0);
    transicao.setDtfim(LocalDate.of(2025, 7, 15));
    var concluida =
        fgService.concluirGozo(
            901L, transicao, precondition(etags, "administracao-pessoal.ferias-gozo", 901L, 1L));
    assertThat(concluida.body().getSituacao()).isEqualTo("GOZADA");
    assertThat(pa.getDiasGozados()).isEqualTo(15);
    assertThat(pa.getSaldoDiasDisponivel()).isEqualTo(15);
    assertThat(pa.getSituacao()).isEqualTo("PARCIALMENTE_GOZADO");
  }

  @Test
  void partialFormResourceIntentsUpdateTargetedSubdomainsWithIfMatchAcrossAllSlice5Resources() {
    EntityManager em = mock(EntityManager.class);
    var tenant = new TenantContext();
    var invariants = createMockedInvariants(em, tenant);
    var etags = new ResourceVersionEtagService("slice5-secret");
    var readFacts = mock(ErgonReadFacts.class);

    // 1. ParametroContagemFerias intents
    var paramRepo = mock(ParametroContagemFeriasRepository.class);
    var paramMapper = new ParametroContagemFeriasMapper(tenant);
    var paramService = new ParametroContagemFeriasService(paramRepo, paramMapper, tenant);
    ReflectionTestUtils.setField(paramService, "entityManager", em);
    ReflectionTestUtils.setField(paramService, "invariants", invariants);
    ReflectionTestUtils.setField(paramService, "readFacts", readFacts);

    var param = new ParametroContagemFerias();
    param.setId(11L);
    param.setEmpCodigo(1);
    param.setRegimeJuridico("ESTATUTARIO");
    param.setTipoVinculo("EFETIVO");
    param.setCategoria("GERAL");
    param.setDtini(LocalDate.of(2021, 1, 1));
    param.setTemFerias(true);
    param.setTotDiasFer(30);
    param.setMinPerFer(1);
    param.setMaxPerFer(3);
    param.setMinDiasPerFer(10);
    param.setMaxDiasPerFer(30);
    param.setActive(true);
    param.setVersion(1L);
    when(em.find(eq(ParametroContagemFerias.class), eq(11L), any(LockModeType.class)))
        .thenReturn(param);
    when(paramRepo.findById(11L)).thenReturn(Optional.of(param));
    when(paramRepo.save(any(ParametroContagemFerias.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    var regrasDto = new AtualizarParcelamentoAbonoParametroFeriasDTO();
    regrasDto.setTemFerias(true);
    regrasDto.setTotDiasFer(30);
    regrasDto.setMinPerFer(1);
    regrasDto.setMaxPerFer(2);
    regrasDto.setMinDiasPerFer(10);
    regrasDto.setMaxDiasPerFer(30);
    regrasDto.setMinDiasPerPrincFer(15);
    regrasDto.setAbonoDirGozo("D");
    regrasDto.setNumDiasAbonoFer(1);
    regrasDto.setDenDiasAbonoFer(3);
    regrasDto.setMaxDiasVendFer(10);
    regrasDto.setAbonoConstFer(true);
    var resRegras =
        paramService.atualizarParcelamentoAbono(
            11L,
            regrasDto,
            precondition(etags, "administracao-pessoal.parametros-contagem-ferias", 11L, 1L));
    assertThat(resRegras.body().getMaxPerFer()).isEqualTo(2);
    assertThat(resRegras.body().getMinDiasPerPrincFer()).isEqualTo(15);

    var licDto = new AtualizarLicencaProgressaoParametroFeriasDTO();
    licDto.setCriterioPrescricao("F");
    licDto.setPermiteGozoAposPrescricao(true);
    licDto.setContDobroFerias(true);
    licDto.setMinDiasPeraqfer(5);
    licDto.setMaxDiasPeraqfer(30);
    licDto.setTemLicEsp(true);
    licDto.setAnosLicEsp(5);
    licDto.setMesesLicEspPer(3);
    licDto.setTemProgressao(true);
    licDto.setAnosProgressao(2);
    var resLic =
        paramService.atualizarLicencaProgressao(
            11L,
            licDto,
            precondition(etags, "administracao-pessoal.parametros-contagem-ferias", 11L, 1L));
    assertThat(resLic.body().getAnosLicEsp()).isEqualTo(5);
    assertThat(resLic.body().getAnosProgressao()).isEqualTo(2);
    assertThat(resLic.body().getCriterioPrescricao()).isEqualTo("F");
    assertThat(resLic.body().getPermiteGozoAposPrescricao()).isTrue();

    // 2. PeriodoAquisitivoFerias intents
    var paRepo = mock(PeriodoAquisitivoFeriasRepository.class);
    var paMapper = new PeriodoAquisitivoFeriasMapper(tenant);
    var paService = new PeriodoAquisitivoFeriasService(paRepo, paMapper, tenant);
    ReflectionTestUtils.setField(paService, "entityManager", em);
    ReflectionTestUtils.setField(paService, "invariants", invariants);
    ReflectionTestUtils.setField(paService, "readFacts", readFacts);

    var pa = new PeriodoAquisitivoFerias();
    pa.setId(22L);
    pa.setEmpCodigo(1);
    pa.setNumfunc(100L);
    pa.setNumvinc(1);
    pa.setChave(1);
    pa.setExercicio(2025);
    pa.setDtini(LocalDate.of(2024, 1, 1));
    pa.setDtfim(LocalDate.of(2024, 12, 31));
    pa.setTotDiasDireito(30);
    pa.setSaldoDiasDisponivel(30);
    pa.setSituacao("DISPONIVEL");
    pa.setVersion(1L);
    when(em.find(eq(PeriodoAquisitivoFerias.class), eq(22L), any(LockModeType.class)))
        .thenReturn(pa);
    when(em.find(eq(PeriodoAquisitivoFerias.class), eq(22L))).thenReturn(pa);
    when(paRepo.findById(22L)).thenReturn(Optional.of(pa));
    when(paRepo.save(any(PeriodoAquisitivoFerias.class))).thenAnswer(inv -> inv.getArgument(0));

    var overridesDto = new AtualizarOverridesPeriodoAquisitivoDTO();
    overridesDto.setMaxPerFer(2);
    overridesDto.setAbonoConstFer("S");
    var resOverrides =
        paService.atualizarOverrides(
            22L,
            overridesDto,
            precondition(etags, "administracao-pessoal.periodos-aquisitivos-ferias", 22L, 1L));
    assertThat(resOverrides.body().getMaxPerFer()).isEqualTo(2);
    assertThat(resOverrides.body().getPossuiOverridesAtivos()).isTrue();

    var prescPaDto = new AtualizarPrescricaoMultaPeriodoAquisitivoDTO();
    prescPaDto.setTotDiasDireito(24);
    prescPaDto.setFaltasApuradas(6);
    prescPaDto.setDiasAfastamentoSuspensivo(0);
    prescPaDto.setDtPrescricao(LocalDate.of(2026, 12, 31));
    prescPaDto.setCriterioPrescricao("I");
    prescPaDto.setPermiteGozoPosPrescricao("S");
    prescPaDto.setSemPgtoMulta("S");
    var resPrescPa =
        paService.atualizarPrescricaoMulta(
            22L,
            prescPaDto,
            precondition(etags, "administracao-pessoal.periodos-aquisitivos-ferias", 22L, 1L));
    assertThat(resPrescPa.body().getTotDiasDireito()).isEqualTo(24);
    assertThat(resPrescPa.body().getSaldoDiasDisponivel()).isEqualTo(24);
    assertThat(resPrescPa.body().getDtPrescricao()).isEqualTo(LocalDate.of(2026, 12, 31));

    // 3. FeriasGozo intents
    var fgRepo = mock(FeriasGozoRepository.class);
    var fgMapper = new FeriasGozoMapper(tenant);
    var fgService = new FeriasGozoService(fgRepo, fgMapper, tenant);
    ReflectionTestUtils.setField(fgService, "entityManager", em);
    ReflectionTestUtils.setField(fgService, "invariants", invariants);
    ReflectionTestUtils.setField(fgService, "readFacts", readFacts);

    var fg = new FeriasGozo();
    fg.setId(33L);
    fg.setEmpCodigo(1);
    fg.setNumfunc(100L);
    fg.setNumvinc(1);
    fg.setPeriodoAquisitivoId(22L);
    fg.setChavePeraqfer(1);
    fg.setOrdemParcela(1);
    fg.setDtini(LocalDate.of(2025, 8, 1));
    fg.setDtprevfim(LocalDate.of(2025, 8, 15));
    fg.setDiasGozo(15);
    fg.setSituacao("PROGRAMADA");
    fg.setVersion(1L);
    when(em.find(eq(FeriasGozo.class), eq(33L), any(LockModeType.class))).thenReturn(fg);
    when(fgRepo.findById(33L)).thenReturn(Optional.of(fg));
    when(fgRepo.save(any(FeriasGozo.class))).thenAnswer(inv -> inv.getArgument(0));

    var abonoDto = new AtualizarAbonoFinanceiroFeriasGozoDTO();
    abonoDto.setAbonoPecuniario(true);
    abonoDto.setIniFimAbono("I");
    abonoDto.setDiasAbonoVendidos(8);
    abonoDto.setAbonoConstitucional(true);
    abonoDto.setAdiantamento13(true);
    var resAbono =
        fgService.atualizarAbonoFinanceiro(
            33L, abonoDto, precondition(etags, "administracao-pessoal.ferias-gozo", 33L, 1L));
    assertThat(resAbono.body().getDiasAbonoVendidos()).isEqualTo(8);
    assertThat(resAbono.body().getAdiantamento13()).isTrue();

    var folhaDto = new AtualizarSubstitutoFolhaFeriasGozoDTO();
    folhaDto.setMesAnoFolha(LocalDate.of(2025, 8, 1));
    folhaDto.setNumFolha(1);
    folhaDto.setDocumentoLegalId(10L);
    folhaDto.setObservacoes("Processado na folha mensal");
    var resFolha =
        fgService.atualizarSubstitutoFolha(
            33L, folhaDto, precondition(etags, "administracao-pessoal.ferias-gozo", 33L, 1L));
    assertThat(resFolha.body().getNumFolha()).isEqualTo(1);
  }

  @Test
  @SuppressWarnings("unchecked")
  void bulkingSupportsUniformPerItemAndDomainCommandAcrossAllSlice5Resources() throws Exception {
    EntityManager em = mock(EntityManager.class);
    var tenant = new TenantContext();
    var invariants = createMockedInvariants(em, tenant);
    var json = new ObjectMapper().findAndRegisterModules();
    var validator = Validation.buildDefaultValidatorFactory().getValidator();

    var pa = new PeriodoAquisitivoFerias();
    pa.setId(77L);
    pa.setEmpCodigo(1);
    pa.setNumfunc(100L);
    pa.setNumvinc(1);
    pa.setChave(1);
    pa.setExercicio(2025);
    pa.setDtini(LocalDate.of(2024, 1, 1));
    pa.setDtfim(LocalDate.of(2024, 12, 31));
    pa.setTotDiasDireito(30);
    pa.setSaldoDiasDisponivel(30);
    pa.setSituacao("EM_AQUISICAO");
    pa.setVersion(1L);
    when(em.find(eq(PeriodoAquisitivoFerias.class), eq(77L), any(LockModeType.class)))
        .thenReturn(pa);
    when(em.find(eq(PeriodoAquisitivoFerias.class), eq(77L))).thenReturn(pa);

    var fg = new FeriasGozo();
    fg.setId(88L);
    fg.setEmpCodigo(1);
    fg.setNumfunc(100L);
    fg.setNumvinc(1);
    fg.setPeriodoAquisitivoId(77L);
    fg.setChavePeraqfer(1);
    fg.setOrdemParcela(1);
    fg.setDtini(LocalDate.of(2025, 10, 1));
    fg.setDtprevfim(LocalDate.of(2025, 10, 15));
    fg.setDiasGozo(15);
    fg.setAbonoPecuniario(false);
    fg.setDiasAbonoVendidos(0);
    fg.setSituacao("PROGRAMADA");
    fg.setVersion(1L);

    TypedQuery<FeriasGozo> fgQuery = mock(TypedQuery.class);
    when(fgQuery.setParameter(anyString(), any())).thenReturn(fgQuery);
    when(fgQuery.setFlushMode(any(FlushModeType.class))).thenReturn(fgQuery);
    when(fgQuery.getResultList()).thenReturn(List.of(fg));
    when(em.createQuery(anyString(), eq(FeriasGozo.class))).thenReturn(fgQuery);

    var fgRepo = mock(FeriasGozoRepository.class);
    var fgService = new FeriasGozoService(fgRepo, new FeriasGozoMapper(tenant), tenant);
    ReflectionTestUtils.setField(fgService, "entityManager", em);
    ReflectionTestUtils.setField(fgService, "invariants", invariants);

    var bulkDomain = new ErgonBulkDomain();
    ReflectionTestUtils.setField(bulkDomain, "em", em);
    ReflectionTestUtils.setField(bulkDomain, "json", json);
    ReflectionTestUtils.setField(bulkDomain, "validator", validator);
    ReflectionTestUtils.setField(bulkDomain, "tenant", tenant);
    ReflectionTestUtils.setField(bulkDomain, "invariants", invariants);
    ReflectionTestUtils.setField(
        bulkDomain, "parametrosContagemFerias", new ParametroContagemFeriasMapper(tenant));
    ReflectionTestUtils.setField(
        bulkDomain, "periodosAquisitivosFerias", new PeriodoAquisitivoFeriasMapper(tenant));
    ReflectionTestUtils.setField(bulkDomain, "feriasGozo", new FeriasGozoMapper(tenant));
    ReflectionTestUtils.setField(bulkDomain, "feriasGozoService", fgService);

    var fgFields = mock(BulkEditableFields.class);
    when(fgFields.writableFields(BulkMode.UNIFORM_UPDATE))
        .thenReturn(Set.of("numFolha", "adiantamento13"));
    when(fgFields.writableFields(BulkMode.PER_ITEM_UPDATE))
        .thenReturn(Set.of("diasGozo", "dtprevfim"));
    when(fgFields.clearableFields(any())).thenReturn(Set.of());
    assertThat(FeriasGozoDados.class.getDeclaredField("numFolha").getAnnotation(BulkEditable.class))
        .isNotNull();
    assertThat(
            PeriodoAquisitivoFeriasDados.class
                .getDeclaredField("observacoes")
                .getAnnotation(BulkEditable.class))
        .isNotNull();
    assertThat(
            ParametroContagemFeriasDados.class
                .getDeclaredField("totDiasFer")
                .getAnnotation(BulkEditable.class))
        .isNotNull();

    // 1. UNIFORM_UPDATE em FeriasGozo
    var uniformChanges =
        List.of(
            new BulkFieldChange("numFolha", BulkChangeOperator.SET, json.valueToTree(2)),
            new BulkFieldChange(
                "adiantamento13", BulkChangeOperator.SET, json.valueToTree(true)));
    Object uniformDto =
        bulkDomain.candidate(
            Resource.FERIAS_GOZO, fg, uniformChanges, fgFields, BulkMode.UNIFORM_UPDATE);
    assertThat(bulkDomain.apply(Resource.FERIAS_GOZO, fg, uniformDto)).isTrue();
    assertThat(fg.getNumFolha()).isEqualTo(2);
    assertThat(fg.getAdiantamento13()).isTrue();

    // 2. PER_ITEM_UPDATE em FeriasGozo (alterando diasGozo de 15 para 20 -> recalcula PA para saldo 10)
    var itemChanges =
        List.of(
            new BulkFieldChange("diasGozo", BulkChangeOperator.SET, json.valueToTree(20)),
            new BulkFieldChange(
                "dtprevfim", BulkChangeOperator.SET, json.valueToTree("2025-10-20")));
    Object itemDto =
        bulkDomain.candidate(
            Resource.FERIAS_GOZO, fg, itemChanges, fgFields, BulkMode.PER_ITEM_UPDATE);
    assertThat(bulkDomain.apply(Resource.FERIAS_GOZO, fg, itemDto)).isTrue();
    assertThat(fg.getDiasGozo()).isEqualTo(20);
    assertThat(pa.getDiasGozados()).isEqualTo(20);
    assertThat(pa.getSaldoDiasDisponivel()).isEqualTo(10);

    // 3. DOMAIN_COMMAND em PeriodoAquisitivoFerias (homologar-periodo-aquisitivo-lote)
    pa.setSituacao("SUSPENSO");
    var cmdPaDto = new TransicaoPeriodoAquisitivoFeriasDTO();
    cmdPaDto.setDocumentoLegalId(10L);
    cmdPaDto.setObservacoes("Homologação coletiva anual");
    Object paCandidate =
        bulkDomain.commandCandidate(
            pa, "homologar-periodo-aquisitivo-lote", json.valueToTree(cmdPaDto));
    assertThat(bulkDomain.applyCommand(pa, paCandidate)).isTrue();
    assertThat(pa.getSituacao()).isEqualTo("PARCIALMENTE_GOZADO");

    // 4. DOMAIN_COMMAND em FeriasGozo (homologar-ferias-gozo-lote) sincroniza o PeriodoAquisitivoFerias
    var cmdFgDto = new TransicaoFeriasGozoDTO();
    cmdFgDto.setDocumentoLegalId(10L);
    cmdFgDto.setObservacoes("Escala coletiva homologada");
    Object fgCandidate =
        bulkDomain.commandCandidate(fg, "homologar-ferias-gozo-lote", json.valueToTree(cmdFgDto));
    assertThat(bulkDomain.applyCommand(fg, fgCandidate)).isTrue();
    assertThat(fg.getSituacao()).isEqualTo("HOMOLOGADA");
    assertThat(pa.getDiasGozados()).isEqualTo(20);
    assertThat(pa.getSaldoDiasDisponivel()).isEqualTo(10);
    assertThat(pa.getSituacao()).isEqualTo("PARCIALMENTE_GOZADO");
  }

  @Test
  @SuppressWarnings("unchecked")
  void radarFeriasPrescricaoContractUrgencyBandsQuickFiltersAnalyticsAndSurfacesAreComplete()
      throws Exception {
    LocalDate ref = LocalDate.of(2026, 6, 1);

    // 1. Teste determinístico das 5 faixas de urgência prescricional
    assertThat(
            RadarFeriasPrescricaoMapper.calcularFaixaUrgencia(
                "DISPONIVEL", 30, LocalDate.of(2026, 5, 31), ref))
        .isEqualTo("VENCIDO_PRESCRITO");
    assertThat(
            RadarFeriasPrescricaoMapper.calcularFaixaUrgencia(
                "PRESCRITO", 0, LocalDate.of(2026, 12, 31), ref))
        .isEqualTo("VENCIDO_PRESCRITO");
    assertThat(
            RadarFeriasPrescricaoMapper.calcularFaixaUrgencia(
                "DISPONIVEL", 30, LocalDate.of(2026, 6, 25), ref))
        .isEqualTo("CRITICO_30D");
    assertThat(
            RadarFeriasPrescricaoMapper.calcularFaixaUrgencia(
                "DISPONIVEL", 30, LocalDate.of(2026, 7, 20), ref))
        .isEqualTo("ALERTA_60D");
    assertThat(
            RadarFeriasPrescricaoMapper.calcularFaixaUrgencia(
                "DISPONIVEL", 30, LocalDate.of(2026, 8, 25), ref))
        .isEqualTo("ATENCAO_90D");
    assertThat(
            RadarFeriasPrescricaoMapper.calcularFaixaUrgencia(
                "DISPONIVEL", 30, LocalDate.of(2026, 12, 31), ref))
        .isEqualTo("REGULAR");

    // 2. Projeção completa de PeriodoAquisitivoFerias + parcelas FeriasGozo -> RadarFeriasPrescricaoResponseDTO
    var pa = new PeriodoAquisitivoFerias();
    pa.setId(800L);
    pa.setEmpCodigo(1);
    pa.setNumfunc(100L);
    pa.setNumvinc(1);
    pa.setChave(2);
    pa.setExercicio(2026);
    pa.setDtini(LocalDate.of(2024, 6, 1));
    pa.setDtfim(LocalDate.of(2025, 5, 31));
    pa.setSituacao("DISPONIVEL");
    pa.setTotDiasDireito(30);
    pa.setFaltasApuradas(0);
    pa.setDiasAfastamentoSuspensivo(0);
    pa.setDiasGozados(0);
    pa.setDiasVendidosAbono(0);
    pa.setSaldoDiasDisponivel(30);
    pa.setDtPrescricao(LocalDate.of(2026, 6, 20));

    var fgProg = new FeriasGozo();
    fgProg.setId(950L);
    fgProg.setPeriodoAquisitivoId(800L);
    fgProg.setDtini(LocalDate.of(2026, 6, 10));
    fgProg.setDiasGozo(10);
    fgProg.setDiasAbonoVendidos(0);
    fgProg.setSituacao("PROGRAMADA");

    var radarEntity = RadarFeriasPrescricaoMapper.projetar(pa, List.of(fgProg), 2, ref);
    var radarMapper = new RadarFeriasPrescricaoMapper();
    var radarDto = radarMapper.toResponse(radarEntity);

    assertThat(radarDto.getId()).isEqualTo(800L);
    assertThat(radarDto.getVinculoId()).isEqualTo(new VinculoId(100L, 1));
    assertThat(radarDto.getDiasProgramadosFuturos()).isEqualTo(10);
    assertThat(radarDto.getSaldoDiasDisponivel()).isEqualTo(30);
    assertThat(radarDto.getSaldoSemProgramacao()).isEqualTo(20);
    assertThat(radarDto.getDiasAtePrescricao()).isEqualTo(19);
    assertThat(radarDto.getFaixaUrgenciaPrescricao()).isEqualTo("CRITICO_30D");
    assertThat(radarDto.getEmRiscoAcumuloPeriodos()).isTrue();
    assertThat(radarDto.getPossuiParcelaProgramada()).isTrue();
    assertThat(radarDto.getProximaParcelaInicio()).isEqualTo(LocalDate.of(2026, 6, 10));
    assertThat(radarDto.getProximaParcelaSituacao()).isEqualTo("PROGRAMADA");
    assertThat(radarDto.getUrgenciaCriticaOuVencida()).isTrue();
    assertThat(radarDto.getPossuiSaldoDescoberto()).isTrue();
    assertThat(radarDto.getEmRiscoImediatoGovernanca()).isTrue();

    // 3. Deserialização estrita (FAIL_ON_UNKNOWN_PROPERTIES) dos 5 @QuickFilter de RadarFeriasPrescricaoController
    var json =
        new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
    var apiResource = RadarFeriasPrescricaoController.class.getAnnotation(ApiResource.class);
    assertThat(apiResource.quickFilters()).hasSize(5);
    for (var qf : apiResource.quickFilters()) {
      var parsed = json.readValue(qf.filter(), RadarFeriasPrescricaoFilterDTO.class);
      assertThat(parsed).as("QuickFilter " + qf.id()).isNotNull();
    }

    // 4. StatsFieldRegistry, CollectionExport e Surfaces do RadarFeriasPrescricaoService / Controller
    var radarRepo = mock(RadarFeriasPrescricaoRepository.class);
    when(radarRepo.findById(800L)).thenReturn(Optional.of(radarEntity));
    when(radarRepo.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(radarEntity)));

    var radarService = new RadarFeriasPrescricaoService(radarRepo, radarMapper, new TenantContext());
    var exportExecutor = mock(org.praxisplatform.uischema.exporting.CollectionExportExecutor.class);
    when(exportExecutor.export(any(), any(), any(), any(), any()))
        .thenReturn(mock(org.praxisplatform.uischema.exporting.CollectionExportResult.class));
    ReflectionTestUtils.setField(radarService, "exports", exportExecutor);
    assertThat(radarService.getStatsFieldRegistry()).isNotNull();
    assertThat(radarService.supportsCollectionExport()).isTrue();
    CollectionExportRequest<RadarFeriasPrescricaoFilterDTO> exportReq =
        mock(CollectionExportRequest.class);
    when(exportReq.format()).thenReturn(CollectionExportFormat.CSV);
    when(exportReq.scope()).thenReturn(CollectionExportScope.FILTERED);
    when(exportReq.filters()).thenReturn(new RadarFeriasPrescricaoFilterDTO());
    when(exportReq.maxRows()).thenReturn(100);
    var exportRes = radarService.exportCollection(exportReq);
    assertThat(exportRes).isNotNull();

    var fgRepo = mock(FeriasGozoRepository.class);
    when(fgRepo.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(fgProg)));
    var fgService =
        new FeriasGozoService(fgRepo, new FeriasGozoMapper(new TenantContext()), new TenantContext());
    ReflectionTestUtils.setField(fgService, "readFacts", mock(ErgonReadFacts.class));

    var radarController = new RadarFeriasPrescricaoController(radarService, fgService);
    assertThat(radarController.detail(800L).getBody().getFaixaUrgenciaPrescricao())
        .isEqualTo("CRITICO_30D");
    assertThat(radarController.parcelasGozo(800L, 0, 20).getBody().getData().getContent())
        .hasSize(1);

    // 5. VinculoStateProvider resolve administracao-pessoal.radar-ferias-prescricao
    var stateProvider = new VinculoStateProvider(mock(VinculoRepository.class));
    ReflectionTestUtils.setField(stateProvider, "radarFeriasPrescricao", radarRepo);
    ReflectionTestUtils.setField(stateProvider, "tenant", new TenantContext());
    var snap =
        stateProvider
            .resolve("administracao-pessoal.radar-ferias-prescricao", 800L)
            .orElseThrow();
    assertThat(snap.state()).isEqualTo("CRITICO_30D");
    assertThat(snap.metadata())
        .containsEntry("urgenciaCriticaOuVencida", true)
        .containsEntry("possuiSaldoDescoberto", true)
        .containsEntry("emRiscoImediatoGovernanca", true);

    // 6. Verificar anotações @UiAnalytics e @UiSurface nos controllers do Slice 5 e VinculoController
    assertThat(
            RadarFeriasPrescricaoController.class
                .getMethod("groupByStats", org.praxisplatform.uischema.stats.dto.GroupByStatsRequest.class)
                .getAnnotation(UiAnalytics.class)
                .projections())
        .hasSize(2);
    assertThat(
            VinculoController.class
                .getMethod("periodosAquisitivosFerias", VinculoId.class, int.class, int.class)
                .getAnnotation(UiSurface.class)
                .relatedChildResourceKey())
        .isEqualTo("administracao-pessoal.periodos-aquisitivos-ferias");
    assertThat(
            VinculoController.class
                .getMethod("feriasGozo", VinculoId.class, int.class, int.class)
                .getAnnotation(UiSurface.class)
                .relatedChildResourceKey())
        .isEqualTo("administracao-pessoal.ferias-gozo");
    assertThat(
            ParametroContagemFeriasController.class
                .getMethod(
                    "encerrarVigencia",
                    Long.class,
                    String.class,
                    AlterarVigenciaParametroFeriasDTO.class)
                .getAnnotation(WorkflowAction.class)
                .id())
        .isEqualTo("encerrar-vigencia-parametro-ferias");
    assertThat(
            FeriasGozoController.class
                .getMethod("iniciarGozo", Long.class, String.class, TransicaoFeriasGozoDTO.class)
                .getAnnotation(WorkflowAction.class)
                .id())
        .isEqualTo("iniciar-ferias-gozo");
    assertThat(
            FeriasGozoController.class
                .getMethod("concluirGozo", Long.class, String.class, TransicaoFeriasGozoDTO.class)
                .getAnnotation(WorkflowAction.class)
                .id())
        .isEqualTo("concluir-ferias-gozo");
  }
}
