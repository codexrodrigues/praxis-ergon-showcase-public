package br.com.praxis.ergonshowcase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.praxis.ergonshowcase.config.ErgonDeterminationConfig;
import br.com.praxis.ergonshowcase.config.TenantContext;
import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.controller.AfastamentoController;
import br.com.praxis.ergonshowcase.controller.VinculoController;
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
import org.praxisplatform.uischema.concurrency.ResourceVersionEtagService;
import org.praxisplatform.uischema.concurrency.ResourceVersionScope;
import org.praxisplatform.uischema.concurrency.ResourceVersionScopeProvider;
import org.praxisplatform.uischema.id.CompositeIdConverter;
import org.praxisplatform.uischema.id.CompositeIdSupport;
import org.praxisplatform.uischema.rest.exceptionhandler.GlobalExceptionHandler;
import org.praxisplatform.uischema.service.base.BaseCreateUpdateResourceCommandService.SavedResult;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
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
        .isInstanceOfAny(ResponseStatusException.class, org.praxisplatform.rules.violation.PraxisRuleViolationException.class)
        .hasMessageContaining("término previsto");
  }

  @Test
  @SuppressWarnings("unchecked")
  void ergonInvariantsEnforcesChronologyModalitySexSubstituteAndOverlapRules() {
    EntityManager em = mock(EntityManager.class);
    var invariants = new ErgonInvariants();
    ReflectionTestUtils.setField(invariants, "em", em);
    var governed = new br.com.praxis.ergonshowcase.service.AfastamentoService(
        null, null, mock(br.com.praxis.ergonshowcase.config.TenantContext.class));
    ReflectionTestUtils.setField(governed, "entityManager", em);
    ReflectionTestUtils.setField(invariants, "afastamentoService", governed);

    TypedQuery<br.com.praxis.ergonshowcase.domain.RegraFrequenciaEstatutaria> statutory =
        mock(TypedQuery.class, org.mockito.Mockito.RETURNS_SELF);
    when(em.createQuery(anyString(), eq(br.com.praxis.ergonshowcase.domain.RegraFrequenciaEstatutaria.class)))
        .thenReturn(statutory);
    when(statutory.getResultList()).thenReturn(List.of());
    TypedQuery<br.com.praxis.ergonshowcase.domain.Frequencia> crossResource =
        mock(TypedQuery.class, org.mockito.Mockito.RETURNS_SELF);
    when(em.createQuery(anyString(), eq(br.com.praxis.ergonshowcase.domain.Frequencia.class)))
        .thenReturn(crossResource);
    when(crossResource.getResultList()).thenReturn(List.of());



    var titular = new Vinculo();
    titular.setId(new VinculoId(1001L, 1));
    titular.setEmpCodigo(1);
    titular.setDtexerc(LocalDate.of(2020, 1, 10));
    titular.setSituacao("ATIVO");
    when(em.find(Vinculo.class, titular.getId())).thenReturn(titular);
    when(em.find(eq(Vinculo.class), eq(new VinculoId(1001L, 1)), any(LockModeType.class)))
        .thenReturn(titular);

    var pessoa = new ServidorPessoa();
    pessoa.setNumero(1001L);
    pessoa.setSexo("M");
    when(em.find(ServidorPessoa.class, 1001L)).thenReturn(pessoa);
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
        .isInstanceOfAny(ResponseStatusException.class, org.praxisplatform.rules.violation.PraxisRuleViolationException.class)
        .hasMessageContaining("inferior ao mínimo regulamentar");
    afast.setDtprevfim(LocalDate.of(2026, 3, 15));

    // 2. Restrição de sexo (modalidade exclusiva para sexo F) -> 422
    codigo.setSexo("F");
    assertThatThrownBy(() -> invariants.afastamento(afast, true))
        .isInstanceOfAny(ResponseStatusException.class, org.praxisplatform.rules.violation.PraxisRuleViolationException.class)
        .hasMessageContaining("exclusiva para servidores do sexo");
    codigo.setSexo("I");

    // 3. Auto-substituição -> 422
    afast.setNumfuncSubstituto(1001L);
    afast.setNumvincSubstituto(1);
    assertThatThrownBy(() -> invariants.afastamento(afast, false))
        .isInstanceOfAny(ResponseStatusException.class, org.praxisplatform.rules.violation.PraxisRuleViolationException.class)
        .satisfies(ex -> assertThat(((org.praxisplatform.rules.violation.PraxisRuleViolationException) ex).getReasonCodes()).contains("ERG-04318"));
    afast.setNumfuncSubstituto(null);
    afast.setNumvincSubstituto(null);

    // 4. Prorrogação sem afastamentoOrigemId -> 422
    afast.setProrrogacao(true);
    assertThatThrownBy(() -> invariants.afastamento(afast, false))
        .isInstanceOfAny(ResponseStatusException.class, org.praxisplatform.rules.violation.PraxisRuleViolationException.class)
        .hasMessageContaining("afastamento de origem");
    afast.setProrrogacao(false);

    // 5. Concomitância com outro afastamento HOMOLOGADO -> 409 CONFLICT
    var existente = new Afastamento();
    existente.setEmpCodigo(1);
    existente.setId(499L);
    existente.setNumfunc(1001L);
    existente.setNumvinc(1);
    existente.setDtini(LocalDate.of(2026, 3, 10));
    existente.setDtprevfim(LocalDate.of(2026, 3, 20));
    existente.setSituacao("HOMOLOGADO");
    when(overlapQuery.getResultList()).thenReturn(List.of(existente));

    assertThatThrownBy(() -> invariants.afastamento(afast, false))
        .isInstanceOfAny(ResponseStatusException.class, org.praxisplatform.rules.violation.PraxisRuleViolationException.class)
        .satisfies(
            ex ->
                assertThat(ErgonFailureAssertions.status(ex))
                    .isEqualTo(HttpStatus.CONFLICT));
  }

  @Test
  void comandosDeAfastamentoUsamRegrasDeCronologiaEAdmitamMotivoOmitido() throws Exception {
    EntityManager em = mock(EntityManager.class);
    TenantContext tenant = org.mockito.Mockito.spy(new TenantContext());
    var commands = new br.com.praxis.ergonshowcase.service.ErgonCommands();
    ReflectionTestUtils.setField(commands, "em", em);
    ReflectionTestUtils.setField(commands, "tenant", tenant);
    ReflectionTestUtils.setField(commands, "rules", mock(ErgonInvariants.class));
    var vinculoMapper = mock(VinculoMapper.class);
    when(vinculoMapper.toResponse(any(Vinculo.class))).thenReturn(new VinculoResponseDTO());
    ReflectionTestUtils.setField(commands, "vinculos", vinculoMapper);
    var readFacts = mock(ErgonReadFacts.class);
    when(readFacts.enrichOne(any(VinculoResponseDTO.class))).thenAnswer(invocation -> invocation.getArgument(0));
    ReflectionTestUtils.setField(commands, "readFacts", readFacts);

    var vinculo = new Vinculo();
    var id = new VinculoId(1001L, 1);
    vinculo.setId(id);
    vinculo.setVersion(1L);
    vinculo.setEmpCodigo(1);
    vinculo.setSituacao("ATIVO");
    var inicio = LocalDate.of(2026, 6, 1);
    vinculo.setDtexerc(inicio.minusYears(2));
    when(em.find(Vinculo.class, id)).thenReturn(vinculo);
    var codigo = new CodigoFrequencia();
    codigo.setId(new CodigoFrequenciaId("LICENCA", 201));
    codigo.setAfastamento(true);
    codigo.setSexo("A");
    when(em.find(CodigoFrequencia.class, codigo.getId())).thenReturn(codigo);
    var pessoa = new ServidorPessoa();
    pessoa.setSexo("M");
    var documento = new DocumentoLegal();
    documento.setId(77L);
    documento.setEmpCodigo(1);
    when(em.find(DocumentoLegal.class, 77L)).thenReturn(documento);

    var service = new br.com.praxis.ergonshowcase.service.AfastamentoService(null, null, tenant) {
      @Override
      public br.com.praxis.ergonshowcase.rules.ErgonRuleEngineFacade.AfastamentoRuleContext
          buildAfastamentoRuleContext(Afastamento draft, boolean inPlaceProrrogacao) {
        assertThat(inPlaceProrrogacao).isFalse();
        return new br.com.praxis.ergonshowcase.rules.ErgonRuleEngineFacade.AfastamentoRuleContext(
            draft, vinculo, pessoa, codigo, null, List.of(), List.of(), null, null, false);
      }
    };
    ReflectionTestUtils.setField(commands, "afastamentoService", service);
    @SuppressWarnings("unchecked")
    org.praxisplatform.uischema.concurrency.ResourceVersionUpdatePrecondition<VinculoId> condition =
        mock(org.praxisplatform.uischema.concurrency.ResourceVersionUpdatePrecondition.class);
    assertThatThrownBy(() -> commands.registrarAfastamento(id,
        new RegistrarAfastamentoDTO("LICENCA", 201, inicio, inicio.minusDays(1), null, 77L, null), condition))
        .isInstanceOfSatisfying(br.com.praxis.ergonshowcase.rules.ErgonRuleViolationException.class,
            error -> assertThat(error.getReasonCodes()).contains("ERG-04313"));
    org.mockito.Mockito.verify(em, org.mockito.Mockito.never()).persist(any());

    commands.registrarAfastamento(id,
        new RegistrarAfastamentoDTO("LICENCA", 201, inicio, inicio.plusDays(2), "  ", 77L, null), condition);
    var saved = org.mockito.ArgumentCaptor.forClass(Afastamento.class);
    org.mockito.Mockito.verify(em).persist(saved.capture());
    var afastamento = saved.getValue();
    assertThat(afastamento.getMotivo()).isNull();
    assertThat(afastamento.getQuantidadeDias()).isEqualTo(3);
    afastamento.setId(500L);
    when(em.find(Afastamento.class, 500L)).thenReturn(afastamento);
    @SuppressWarnings("unchecked")
    TypedQuery<Long> abertos = mock(TypedQuery.class, org.mockito.Mockito.RETURNS_SELF);
    when(em.createQuery(anyString(), eq(Long.class))).thenReturn(abertos);
    when(abertos.getSingleResult()).thenReturn(1L);

    assertThatThrownBy(() -> commands.encerrarAfastamento(id,
        new EncerrarAfastamentoDTO(500L, inicio.minusDays(1), 77L), condition))
        .isInstanceOfSatisfying(br.com.praxis.ergonshowcase.rules.ErgonRuleViolationException.class,
            error -> assertThat(error.getViolations()).anySatisfy(violation -> {
              assertThat(violation.code()).isEqualTo("ERG-04312");
              assertThat(violation.targetField()).isEqualTo("dtfim");
              assertThat(violation.slotKey()).isNotBlank();
              assertThat(violation.lineage()).contains("PCK_LIC_AFAST");
            }));

    // O mesmo comando concede 35 dias no Órgão 1, mas exige publicação no Órgão 2.
    org.mockito.Mockito.clearInvocations(em);
    vinculo.setSituacao("ATIVO");
    var longa = new RegistrarAfastamentoDTO("LICENCA", 201, inicio, inicio.plusDays(34), null, 77L, null);
    commands.registrarAfastamento(id, longa, condition);
    org.mockito.Mockito.verify(em).persist(any(Afastamento.class));
    org.mockito.Mockito.clearInvocations(em);
    org.mockito.Mockito.doReturn(2).when(tenant).orgao();
    documento.setEmpCodigo(2);
    vinculo.setEmpCodigo(2);
    vinculo.setSituacao("ATIVO");
    assertThatThrownBy(() -> commands.registrarAfastamento(id, longa, condition))
        .isInstanceOfSatisfying(br.com.praxis.ergonshowcase.rules.ErgonRuleViolationException.class,
            error -> assertThat(error.getReasonCodes()).contains("CERG-09201"));
    org.mockito.Mockito.verify(em, org.mockito.Mockito.never()).persist(any());
    commands.registrarAfastamento(id,
        new RegistrarAfastamentoDTO("LICENCA", 201, inicio, inicio.plusDays(34), null, 77L, 77L), condition);
    org.mockito.Mockito.verify(em).persist(org.mockito.ArgumentMatchers.argThat(
        value -> value instanceof Afastamento item && item.getAtoPublicacaoId().equals(77L)));

    vinculo.setSituacao("ATIVO");
    var etags = new ResourceVersionEtagService("test");
    ResourceVersionScopeProvider scopes = () -> ResourceVersionScope.GLOBAL;
    var vinculoController = new VinculoController(
        mock(br.com.praxis.ergonshowcase.service.VinculoService.class), etags, scopes);
    ReflectionTestUtils.invokeMethod(vinculoController, "configureResourceVersioning", etags, scopes);
    ReflectionTestUtils.invokeMethod(vinculoController, "initializeBasePath");
    ReflectionTestUtils.setField(vinculoController, "commands", commands);
    var afastamentoWrites = mock(br.com.praxis.ergonshowcase.service.AfastamentoService.class);
    var mapper = new AfastamentoMapper();
    when(afastamentoWrites.create(any(CreateAfastamentoDTO.class))).thenAnswer(call -> {
      var draft = mapper.newEntity(call.getArgument(0));
      service.validateAfastamentoConcessao(draft);
      assertThat(draft.getMotivo()).isNull();
      draft.setId(501L);
      return new SavedResult<>(draft.getId(), mapper.toResponse(draft));
    });
    var conversion = new DefaultFormattingConversionService();
    conversion.addConverter(new CompositeIdConverter());
    var mvc = MockMvcBuilders.standaloneSetup(
            new AfastamentoController(afastamentoWrites, etags, scopes), vinculoController)
        .setConversionService(conversion).setControllerAdvice(new GlobalExceptionHandler()).build();
    var inicioProgramado = LocalDate.now().plusDays(1);
    mvc.perform(post(ApiPaths.AFASTAMENTOS).contentType(MediaType.APPLICATION_JSON).content("""
        {"numfunc":1001,"numvinc":1,"empCodigo":2,"tipofreq":"LICENCA","codfreq":201,
         "dtini":"%s","dtprevfim":"%s","documentoLegalId":77}
        """.formatted(inicioProgramado, inicioProgramado.plusDays(2))))
        .andExpect(status().isCreated());
    mvc.perform(post(ApiPaths.VINCULOS + "/{id}/actions/registrar-afastamento", CompositeIdSupport.stringify(id))
            .header("If-Match", etags.create("administracao-pessoal.vinculos", id, 1L))
            .contentType(MediaType.APPLICATION_JSON).content("""
                {"tipofreq":"LICENCA","codfreq":201,"dtini":"%s","dtprevfim":"%s","documentoLegalId":77}
                """.formatted(inicio, inicio.plusDays(2))))
        .andExpect(status().isOk());

    // Um retorno não encerra a outra licença e não apaga o estado de cessão.
    afastamento.setEmpCodigo(2);
    afastamento.setDtfim(null);
    when(abertos.getSingleResult()).thenReturn(2L);
    commands.encerrarAfastamento(id, new EncerrarAfastamentoDTO(500L, inicio.plusDays(1), 77L), condition);
    assertThat(vinculo.getSituacao()).isEqualTo("AFASTADO");
    afastamento.setDtfim(null);
    vinculo.setSituacao("CEDIDO");
    when(abertos.getSingleResult()).thenReturn(1L);
    commands.encerrarAfastamento(id, new EncerrarAfastamentoDTO(500L, inicio.plusDays(1), 77L), condition);
    assertThat(vinculo.getSituacao()).isEqualTo("CEDIDO");
  }

  @Test
  void entradaDeAfastamentoAdmiteMotivoOmitidoEDerivaProrrogacaoDaOrigem() {
    var dto = new br.com.praxis.ergonshowcase.dto.CreateAfastamentoDTO();
    var mapper = new br.com.praxis.ergonshowcase.mapper.AfastamentoMapper();
    try (var validation = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
      var validator = validation.getValidator();
      assertThat(validator.validateProperty(dto, "motivo")).isEmpty();
      assertThat(validator.validateProperty(dto, "prorrogacao")).isEmpty();
      assertThat(validator.validateValue(
          br.com.praxis.ergonshowcase.dto.RegistrarAfastamentoDTO.class, "motivo", null)).isEmpty();
    }
    assertThat(mapper.newEntity(dto).getMotivo()).isNull();
    assertThat(mapper.newEntity(dto).getProrrogacao()).isFalse();
    dto.setAfastamentoOrigemId(100L);
    assertThat(mapper.newEntity(dto).getProrrogacao()).isTrue();
    dto.setProrrogacao(false);
    assertThat(mapper.newEntity(dto).getProrrogacao()).isFalse();
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

    var stateProvider = StateSnapshotFixture.provider(StateSnapshotFixture.service(br.com.praxis.ergonshowcase.service.AfastamentoService.class, afastRepo));


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
