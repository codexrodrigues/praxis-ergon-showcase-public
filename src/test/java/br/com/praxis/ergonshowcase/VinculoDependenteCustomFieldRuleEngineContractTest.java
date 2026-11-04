package br.com.praxis.ergonshowcase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.praxis.ergonshowcase.config.TenantContext;
import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.controller.DependenteController;
import br.com.praxis.ergonshowcase.controller.VinculoController;
import br.com.praxis.ergonshowcase.domain.Afastamento;
import br.com.praxis.ergonshowcase.domain.Cargo;
import br.com.praxis.ergonshowcase.domain.CodigoFrequencia;
import br.com.praxis.ergonshowcase.domain.CodigoFrequenciaId;
import br.com.praxis.ergonshowcase.domain.DefinicaoCampoCustomizado;
import br.com.praxis.ergonshowcase.domain.Dependente;
import br.com.praxis.ergonshowcase.domain.DependenteId;
import br.com.praxis.ergonshowcase.domain.DocumentoLegal;
import br.com.praxis.ergonshowcase.domain.Frequencia;
import br.com.praxis.ergonshowcase.domain.Orgao;
import br.com.praxis.ergonshowcase.domain.ServidorPessoa;
import br.com.praxis.ergonshowcase.domain.Setor;
import br.com.praxis.ergonshowcase.domain.TipoDependente;
import br.com.praxis.ergonshowcase.domain.Vinculo;
import br.com.praxis.ergonshowcase.domain.VinculoId;
import br.com.praxis.ergonshowcase.dto.CreateDependenteDTO;
import br.com.praxis.ergonshowcase.dto.CreateVinculoDTO;
import br.com.praxis.ergonshowcase.mapper.DefinicaoCampoCustomizadoMapper;
import br.com.praxis.ergonshowcase.mapper.DependenteMapper;
import br.com.praxis.ergonshowcase.mapper.VinculoMapper;
import br.com.praxis.ergonshowcase.repository.DefinicaoCampoCustomizadoRepository;
import br.com.praxis.ergonshowcase.repository.DependenteRepository;
import br.com.praxis.ergonshowcase.repository.VinculoRepository;
import br.com.praxis.ergonshowcase.rules.DependenteElegibilidadeRuleSet;
import br.com.praxis.ergonshowcase.rules.ErgonRuleEngineFacade;
import br.com.praxis.ergonshowcase.rules.ErgonRuleEngineFacade.DependenteRuleContext;
import br.com.praxis.ergonshowcase.rules.ErgonRuleEngineFacade.VinculoRuleContext;
import br.com.praxis.ergonshowcase.rules.ErgonRuleErrorCatalog;
import br.com.praxis.ergonshowcase.rules.ErgonRuleViolationException;
import br.com.praxis.ergonshowcase.rules.VinculoProvimentoRuleSet;
import br.com.praxis.ergonshowcase.service.DefinicaoCampoCustomizadoService;
import br.com.praxis.ergonshowcase.service.DependenteService;
import br.com.praxis.ergonshowcase.service.ErgonInvariants;
import br.com.praxis.ergonshowcase.service.VinculoService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TypedQuery;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.praxisplatform.rules.contract.DecisionSource;
import org.praxisplatform.rules.contract.RuleDecision;
import org.praxisplatform.rules.contract.RuleEvaluationResult;
import org.praxisplatform.rules.plan.RuleDecisionPlan;
import org.praxisplatform.rules.plan.RulePlanException;
import org.praxisplatform.rules.plan.RulePlanIssueCode;
import org.praxisplatform.uischema.rest.exceptionhandler.GlobalExceptionHandler;
import org.praxisplatform.uischema.service.base.BaseCreateUpdateResourceCommandService.SavedResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Suíte determinística de contrato para os RuleSets governados de Elegibilidade de Dependentes
 * ({@link DependenteElegibilidadeRuleSet} — {@code PCK_DEPENDENTES}), Provimento e Vacância de
 * Vínculos ({@link VinculoProvimentoRuleSet} — {@code PCK_VINCULOS}) e Validação/Execução Real de
 * JSON Logic em {@link DefinicaoCampoCustomizado#getValidationRules()} sobre o
 * {@code praxis-rules-engine:0.1.0-beta.17}.
 */
class VinculoDependenteCustomFieldRuleEngineContractTest {

  private ErgonRuleEngineFacade facade;

  @BeforeEach
  void setUp() {
    facade = RuleCatalogExecution.facade();
  }

  @Test
  void bothProductRuleSetsCompileCleanlyUnderContract16AndAllowValidBaselines() {
    RuleDecisionPlan depPlan = facade.resolveDependentePlan(1);
    assertThat(depPlan.slotsByKey()).hasSize(10);
    assertThat(depPlan.planDigest()).isNotBlank();

    RuleEvaluationResult depResult = facade.evaluateDependente(validDependenteContext());
    assertThat(depResult.decision()).isEqualTo(RuleDecision.ALLOW);
    assertThat(depResult.reasonCodes()).isEmpty();
    assertThat(depResult.bindingResults()).hasSize(12);

    RuleDecisionPlan vincPlan = facade.resolveVinculoPlan(1);
    assertThat(vincPlan.slotsByKey()).hasSize(8);
    assertThat(vincPlan.planDigest()).isNotBlank();

    RuleEvaluationResult vincResult = facade.evaluateVinculo(validVinculoContext());
    assertThat(vincResult.decision()).isEqualTo(RuleDecision.ALLOW);
    assertThat(vincResult.reasonCodes()).isEmpty();
    assertThat(vincResult.bindingResults()).hasSize(8);
  }

  @Test
  void entradaDeDependenteAceitaNascimentoHojeEDelegaDataFuturaAoMotor() {
    var ctx = validDependenteContext();
    LocalDate hoje = ctx.referenceDate();
    try (var validation = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
      var validator = validation.getValidator();
      for (LocalDate nascimento : List.of(hoje, hoje.plusDays(1))) {
        assertThat(validator.validateValue(
            br.com.praxis.ergonshowcase.dto.CreateDependenteDTO.class, "dtnasc", nascimento)).isEmpty();
      }
    }
    ctx.draft().setDtnasc(hoje);
    ctx.draft().setDtini(hoje);
    assertThat(facade.enforceDependente(ctx).decision()).isEqualTo(RuleDecision.ALLOW);

    ctx.draft().setDtnasc(hoje.plusDays(1));
    ctx.draft().setDtini(hoje.plusDays(1));
    assertThatThrownBy(() -> facade.enforceDependente(ctx))
        .isInstanceOfSatisfying(ErgonRuleViolationException.class, rejection -> {
          assertThat(rejection.getHttpStatus()).isEqualTo(422);
          assertThat(rejection.getReasonCodes()).containsExactly("ERG-03182");
          assertThat(rejection.getDescriptors().getFirst().targetField()).isEqualTo("dtnasc");
        });
  }

  @Test
  void entradaDeVinculoAceitaExercicioProgramadoMasPreservaOrdemDaPosse() {
    var ctx = validVinculoContext();
    var dataProgramada = LocalDate.now().plusDays(5);
    try (var validation = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
      assertThat(validation.getValidator().validateValue(
          br.com.praxis.ergonshowcase.dto.CreateVinculoDTO.class, "dtexerc", dataProgramada)).isEmpty();
    }
    ctx.draft().setDtnom(dataProgramada.minusDays(7));
    ctx.draft().setDtposse(dataProgramada.minusDays(6));
    ctx.draft().setDtexerc(dataProgramada);
    assertThat(facade.enforceVinculo(ctx).decision()).isEqualTo(RuleDecision.ALLOW);
    ctx.draft().setDtposse(dataProgramada.plusDays(1));
    assertThatThrownBy(() -> facade.enforceVinculo(ctx))
        .isInstanceOfSatisfying(ErgonRuleViolationException.class,
            rejection -> assertThat(rejection.getReasonCodes()).contains("ERG-00324"));
  }

  @Test
  void postsDeDependenteEVinculoPreservamStatusNaFronteiraHttp() throws Exception {
    var dep = validDependenteContext();
    var depService = mock(DependenteService.class);
    var depMapper = new DependenteMapper();
    var hoje = LocalDate.now(java.time.ZoneId.of(ErgonRuleEngineFacade.DEFAULT_TIME_ZONE));
    when(depService.create(any(CreateDependenteDTO.class))).thenAnswer(call -> {
      var draft = depMapper.newEntity(call.getArgument(0));
      facade.enforceDependente(new DependenteRuleContext(
          draft, 1, dep.pessoa(), dep.tipoDependente(), dep.documentoLegal(), List.of(), hoje));
      return new SavedResult<>(draft.getId(), depMapper.toResponse(draft));
    });
    var vinc = validVinculoContext();
    var vincService = mock(VinculoService.class);
    var vincMapper = new VinculoMapper();
    when(vincService.create(any(CreateVinculoDTO.class))).thenAnswer(call -> {
      var dto = call.<CreateVinculoDTO>getArgument(0);
      vinc.draft().setDtnom(dto.getDtnom());
      vinc.draft().setDtposse(dto.getDtposse());
      vinc.draft().setDtexerc(dto.getDtexerc());
      facade.enforceVinculo(vinc);
      vinc.draft().setPessoa(vinc.pessoa());
      return new SavedResult<>(vinc.draft().getId(), vincMapper.toResponse(vinc.draft()));
    });
    var etags = new org.praxisplatform.uischema.concurrency.ResourceVersionEtagService("test");
    org.praxisplatform.uischema.concurrency.ResourceVersionScopeProvider scopes =
        () -> org.praxisplatform.uischema.concurrency.ResourceVersionScope.GLOBAL;
    var mvc = MockMvcBuilders.standaloneSetup(
            new DependenteController(depService, etags, scopes),
            new VinculoController(vincService, etags, scopes))
        .setControllerAdvice(new GlobalExceptionHandler()).build();
    String dependente = """
        {"numfunc":1001,"numdep":1,"nome":"Recém-nascido","parentesco":"FILHO",
         "sexo":"M","tipodepen":"FILHO_MENOR","cpf":"99988877766","documentoLegalId":77,
         "dtnasc":"%1$s","dtini":"%1$s"}
        """;
    mvc.perform(post(ApiPaths.DEPENDENTES).contentType(MediaType.APPLICATION_JSON)
            .content(dependente.formatted(hoje)))
        .andExpect(status().isCreated());
    mvc.perform(post(ApiPaths.DEPENDENTES).contentType(MediaType.APPLICATION_JSON)
            .content(dependente.formatted(hoje.plusDays(1))))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.errors[0].code").value("ERG-03182"))
        .andExpect(jsonPath("$.errors[0].violations[0].targetField").value("dtnasc"))
        .andExpect(jsonPath("$.errors[0].violations[0].slotKey")
            .value(DependenteElegibilidadeRuleSet.SLOT_DATA_NASCIMENTO))
        .andExpect(jsonPath("$.errors[0].violations[0].lineage").isNotEmpty());
    mvc.perform(post(ApiPaths.VINCULOS).contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"numfunc":1001,"numero":1,"empCodigo":1,"regimejur":"ESTATUTARIO",
                 "tipovinc":"EFETIVO","categoria":"ADMINISTRATIVO","dtnom":"%s",
                 "dtposse":"%s","dtexerc":"%s"}
                """.formatted(hoje.minusDays(2), hoje, hoje.plusDays(5))))
        .andExpect(status().isCreated());
  }

  @ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"ceder", "retornar", "aposentar", "estagio"})
  void comandosDeVinculoEncaminhamDatasInvertidasAoMotor(String acao) {
    var ctx = validVinculoContext();
    Vinculo draft = ctx.draft();
    draft.setSituacao(acao.equals("retornar") ? "CEDIDO" : "ATIVO");
    draft.setDtiniCessao(draft.getDtexerc().plusDays(5));
    var anterior = draft.getDtexerc().minusDays(1);
    var em = mock(EntityManager.class);
    when(em.find(Vinculo.class, draft.getId())).thenReturn(draft);
    var invariants = mock(ErgonInvariants.class);
    org.mockito.Mockito.doAnswer(invocation -> {
      facade.enforceVinculo(ctx);
      return null;
    }).when(invariants).vinculo(draft, false);
    var commands = new br.com.praxis.ergonshowcase.service.ErgonCommands();
    ReflectionTestUtils.setField(commands, "em", em);
    ReflectionTestUtils.setField(commands, "rules", invariants);
    ReflectionTestUtils.setField(commands, "tenant", new TenantContext());
    @SuppressWarnings("unchecked")
    org.praxisplatform.uischema.concurrency.ResourceVersionUpdatePrecondition<VinculoId> precondition =
        mock(org.praxisplatform.uischema.concurrency.ResourceVersionUpdatePrecondition.class);

    assertThatThrownBy(() -> {
      switch (acao) {
        case "ceder" -> commands.registrarCessao(draft.getId(),
            new br.com.praxis.ergonshowcase.dto.RegistrarCessaoDTO(
                "CESSAO", "Órgão", null, null, null, anterior, draft.getDtexerc()), precondition);
        case "retornar" -> commands.encerrarCessao(draft.getId(),
            new br.com.praxis.ergonshowcase.dto.EncerrarCessaoDTO(anterior), precondition);
        case "aposentar" -> commands.concederAposentadoria(draft.getId(),
            new br.com.praxis.ergonshowcase.dto.ConcederAposentadoriaDTO(
                anterior, "VOLUNTARIA", null, null), precondition);
        case "estagio" -> commands.homologarEstagio(draft.getId(),
            new br.com.praxis.ergonshowcase.dto.HomologarEstagioDTO(anterior, null), precondition);
        default -> throw new AssertionError(acao);
      }
    }).isInstanceOfSatisfying(ErgonRuleViolationException.class, rejection -> {
      assertThat(rejection.getHttpStatus()).isEqualTo(422);
      assertThat(rejection.getReasonCodes()).contains("ERG-00324");
      assertThat(rejection.getViolations()).anySatisfy(violation -> {
        assertThat(violation.code()).isEqualTo("ERG-00324");
        assertThat(violation.slotKey()).isNotBlank();
        assertThat(violation.targetField()).isNotBlank();
        assertThat(violation.lineage()).contains("PCK_VINCULOS");
      });
    });
    org.mockito.Mockito.verify(em, org.mockito.Mockito.never()).flush();
  }

  static Stream<Arguments> dependenteProductDecisionViolations() {
    return Stream.of(
        Arguments.of(
            "ERG-03162",
            DependenteElegibilidadeRuleSet.SLOT_PERIODO_VIGENCIA,
            (Supplier<DependenteRuleContext>)
                () -> {
                  DependenteRuleContext base = validDependenteContext();
                  base.draft().setDtini(LocalDate.of(2025, 5, 10));
                  base.draft().setDtfim(LocalDate.of(2025, 5, 1));
                  return base;
                }),
        Arguments.of(
            "ERG-03182",
            DependenteElegibilidadeRuleSet.SLOT_DATA_NASCIMENTO,
            (Supplier<DependenteRuleContext>)
                () -> {
                  DependenteRuleContext base = validDependenteContext();
                  base.draft().setDtnasc(LocalDate.of(2027, 1, 1));
                  base.draft().setDtini(LocalDate.of(2027, 1, 1));
                  return base;
                }),
        Arguments.of(
            "ERG-03181",
            DependenteElegibilidadeRuleSet.SLOT_DATA_NASCIMENTO,
            (Supplier<DependenteRuleContext>)
                () -> {
                  DependenteRuleContext base = validDependenteContext();
                  base.draft().setDtnasc(LocalDate.of(1890, 1, 1));
                  base.draft().setDepIr(false);
                  base.draft().setDepSalarioFamilia(false);
                  base.draft().setDepPlanoSaude(false);
                  return base;
                }),
        Arguments.of(
            "ERG-03183",
            DependenteElegibilidadeRuleSet.SLOT_COERENCIA_PARENTESCO,
            (Supplier<DependenteRuleContext>)
                () -> {
                  DependenteRuleContext base = validDependenteContext();
                  base.draft().setParentesco("FILHO");
                  base.draft().setDtnasc(LocalDate.of(1980, 1, 1)); // anterior ao nascimento do titular (1985)
                  base.draft().setTipodepen(null);
                  base.draft().setDepIr(false);
                  base.draft().setDepSalarioFamilia(false);
                  base.draft().setDepPlanoSaude(false);
                  return base;
                }),
        Arguments.of(
            "ERG-03184",
            DependenteElegibilidadeRuleSet.SLOT_COERENCIA_PARENTESCO,
            (Supplier<DependenteRuleContext>)
                () -> {
                  DependenteRuleContext base = validDependenteContext();
                  base.draft().setParentesco("PAI");
                  base.draft().setDtnasc(LocalDate.of(2005, 1, 1)); // posterior ao nascimento do titular (1985)
                  base.draft().setTipodepen(null);
                  base.draft().setDepIr(false);
                  base.draft().setDepSalarioFamilia(false);
                  base.draft().setDepPlanoSaude(false);
                  return base;
                }),
        Arguments.of(
            "ERG-03185",
            DependenteElegibilidadeRuleSet.SLOT_CPF_OBRIGATORIO_IRRF,
            (Supplier<DependenteRuleContext>)
                () -> {
                  DependenteRuleContext base = validDependenteContext();
                  base.draft().setDepIr(true);
                  base.draft().setCpf(null); // IRRF sem CPF
                  return base;
                }),
        Arguments.of(
            "ERG-03171",
            DependenteElegibilidadeRuleSet.SLOT_COMPATIBILIDADE_TIPO,
            (Supplier<DependenteRuleContext>)
                () -> {
                  DependenteRuleContext base = validDependenteContext();
                  base.tipoDependente().setGeraSalarioFamilia(false);
                  base.draft().setDepSalarioFamilia(true);
                  return base;
                }),
        Arguments.of(
            "ERG-03177",
            DependenteElegibilidadeRuleSet.SLOT_ELEGIBILIDADE_ETARIA,
            (Supplier<DependenteRuleContext>)
                () -> {
                  DependenteRuleContext base = validDependenteContext();
                  // 22 anos na data de referência (2026-06-01), não universitário, não inválido, com depIr = true
                  base.tipoDependente().setIdadeLimite(null);
                  base.draft().setDtnasc(LocalDate.of(2004, 1, 1));
                  base.draft().setDepIr(true);
                  base.draft().setDepSalarioFamilia(false);
                  base.draft().setUniversitario(false);
                  base.draft().setInvalido(false);
                  return base;
                }),
        Arguments.of(
            "ERG-03188",
            DependenteElegibilidadeRuleSet.SLOT_ENCERRAMENTO_SEM_BENEFICIOS,
            (Supplier<DependenteRuleContext>)
                () -> {
                  DependenteRuleContext base = validDependenteContext();
                  base.draft().setDtfim(LocalDate.of(2025, 12, 31));
                  base.draft().setDepIr(true); // encerrado mantendo benefício ativo
                  return base;
                }),
        Arguments.of(
            "ERG-21400",
            DependenteElegibilidadeRuleSet.SLOT_EXIGENCIA_DOCUMENTO,
            (Supplier<DependenteRuleContext>)
                () -> {
                  DependenteRuleContext base = validDependenteContext();
                  base.draft().setInvalido(true);
                  base.draft().setDocumentoLegalId(null); // inválido sem laudo/documento legal
                  return new DependenteRuleContext(
                      base.draft(),
                      base.empCodigo(),
                      base.pessoa(),
                      base.tipoDependente(),
                      null,
                      base.existingDependentes(),
                      base.referenceDate());
                }),
        Arguments.of(
            "ERG-03198",
            DependenteElegibilidadeRuleSet.SLOT_POLITICA_PLANO_SAUDE,
            (Supplier<DependenteRuleContext>)
                () -> {
                  DependenteRuleContext base = validDependenteContext();
                  base.draft().setParentesco("AGREGADO");
                  base.draft().setTipodepen(null);
                  base.draft().setDepIr(false);
                  base.draft().setDepSalarioFamilia(false);
                  base.draft().setDepPlanoSaude(true); // agregado em plano de saúde sem documentoLegalId
                  base.draft().setDocumentoLegalId(null);
                  return new DependenteRuleContext(
                      base.draft(),
                      base.empCodigo(),
                      base.pessoa(),
                      null,
                      null,
                      base.existingDependentes(),
                      base.referenceDate());
                }),
        Arguments.of(
            "ERG-03193",
            DependenteElegibilidadeRuleSet.SLOT_UNICIDADE_CPF,
            (Supplier<DependenteRuleContext>)
                () -> {
                  DependenteRuleContext base = validDependenteContext();
                  Dependente sibling = new Dependente();
                  sibling.setId(new DependenteId(1001L, 2));
                  sibling.setCpf(base.draft().getCpf());
                  sibling.setDtini(LocalDate.of(2020, 1, 1));
                  sibling.setDtfim(null); // dependente ativo no mesmo servidor com mesmo CPF
                  return new DependenteRuleContext(
                      base.draft(),
                      base.empCodigo(),
                      base.pessoa(),
                      base.tipoDependente(),
                      base.documentoLegal(),
                      List.of(sibling),
                      base.referenceDate());
                }));
  }

  @ParameterizedTest
  @MethodSource("dependenteProductDecisionViolations")
  void dependenteProductRuleSetDeniesEveryInvalidDecisionWithExactReasonCodeAndSlot(
      String expectedCode, String expectedSlot, Supplier<DependenteRuleContext> contextSupplier) {
    DependenteRuleContext ctx = contextSupplier.get();
    RuleEvaluationResult result = facade.evaluateDependente(ctx);
    assertThat(result.decision()).isEqualTo(RuleDecision.DENY);
    assertThat(result.reasonCodes()).contains(expectedCode);

    assertThatThrownBy(() -> facade.enforceDependente(ctx))
        .isInstanceOfSatisfying(
            ErgonRuleViolationException.class,
            ex -> {
              assertThat(ex.getStatusCode()).isEqualTo(java.util.Set.of("ERG-03193", "ERG-00300").contains(expectedCode)
                  ? HttpStatus.CONFLICT : HttpStatus.UNPROCESSABLE_ENTITY);
              assertThat(ex.getReasonCodes()).contains(expectedCode);
              assertThat(ex.getSlotKeys()).contains(expectedSlot);
            });
  }

  @Test
  void dependenteBenefitAgeCeilingsEnforceIrrfFamilyAllowanceHealthPlanAndExemptions() {
    // 1. Salário-família acima de 14 anos (15 anos) sem invalidez -> DENY ERG-03177
    DependenteRuleContext over14SalFam = validDependenteContext();
    over14SalFam.draft().setDtnasc(LocalDate.of(2011, 1, 1)); // 15 anos em 2026-06-01
    over14SalFam.draft().setDepSalarioFamilia(true);
    RuleEvaluationResult resSalFam = facade.evaluateDependente(over14SalFam);
    assertThat(resSalFam.decision()).isEqualTo(RuleDecision.DENY);
    assertThat(resSalFam.reasonCodes()).contains("ERG-03177");

    // 2. Universitário de 23 anos com estudante = true -> ALLOW para IRRF e Plano de Saúde
    DependenteRuleContext college23 = validDependenteContext();
    college23.draft().setDtnasc(LocalDate.of(2003, 1, 1)); // 23 anos em 2026-06-01
    college23.draft().setDepIr(true);
    college23.draft().setDepSalarioFamilia(false);
    college23.draft().setDepPlanoSaude(true);
    college23.draft().setEstudante(true);
    college23.draft().setUniversitario(true);
    assertThat(facade.evaluateDependente(college23).decision()).isEqualTo(RuleDecision.ALLOW);

    // 3. Universitário de 25 anos (> 24) sem invalidez -> DENY ERG-03177
    DependenteRuleContext college25 = validDependenteContext();
    college25.draft().setDtnasc(LocalDate.of(2001, 1, 1)); // 25 anos em 2026-06-01
    college25.draft().setDepIr(true);
    college25.draft().setDepSalarioFamilia(false);
    college25.draft().setDepPlanoSaude(true);
    college25.draft().setEstudante(true);
    college25.draft().setUniversitario(true);
    assertThat(facade.evaluateDependente(college25).decision()).isEqualTo(RuleDecision.DENY);
    assertThat(facade.evaluateDependente(college25).reasonCodes()).contains("ERG-03177");

    // 4. Isenção automática de teto etário quando invalido == true (35 anos, com laudo legal) -> ALLOW
    DependenteRuleContext invalido35 = validDependenteContext();
    invalido35.draft().setDtnasc(LocalDate.of(1991, 1, 1)); // 35 anos
    invalido35.draft().setInvalido(true);
    invalido35.draft().setDepIr(true);
    invalido35.draft().setDepSalarioFamilia(true);
    invalido35.draft().setDepPlanoSaude(true);
    assertThat(facade.evaluateDependente(invalido35).decision()).isEqualTo(RuleDecision.ALLOW);

    // 5. Isenção automática de teto etário quando excepcional == true (30 anos, com laudo legal) -> ALLOW
    DependenteRuleContext excepcional30 = validDependenteContext();
    excepcional30.draft().setDtnasc(LocalDate.of(1996, 1, 1)); // 30 anos
    excepcional30.draft().setExcepcional(true);
    excepcional30.draft().setDepIr(true);
    excepcional30.draft().setDepSalarioFamilia(true);
    excepcional30.draft().setDepPlanoSaude(true);
    assertThat(facade.evaluateDependente(excepcional30).decision()).isEqualTo(RuleDecision.ALLOW);

    // 6. Dependente anterior já encerrado (dtfim < dtini) com mesmo CPF não bloqueia novo registro
    Dependente closedSibling = new Dependente();
    closedSibling.setId(new DependenteId(1001L, 2));
    closedSibling.setCpf(validDependenteContext().draft().getCpf());
    closedSibling.setDtini(LocalDate.of(2015, 1, 1));
    closedSibling.setDtfim(LocalDate.of(2019, 12, 31));
    DependenteRuleContext withClosedSibling =
        new DependenteRuleContext(
            validDependenteContext().draft(),
            1,
            validDependenteContext().pessoa(),
            validDependenteContext().tipoDependente(),
            validDependenteContext().documentoLegal(),
            List.of(closedSibling),
            LocalDate.of(2026, 6, 1));
    assertThat(facade.evaluateDependente(withClosedSibling).decision()).isEqualTo(RuleDecision.ALLOW);
  }

  @Test
  void dependenteAllFourCustomizationModesEnforcedBetweenOrgao1AndOrgao2() {
    // 1. FORBIDDEN: tentativa de sobrescrever guarda protegida (cronologia/genealogia/CPF) falha na compilação
    assertThatThrownBy(
            () ->
                facade.compilePlan(
                    DependenteElegibilidadeRuleSet.customerMode1ForbiddenOverrideAttempt()))
        .isInstanceOfSatisfying(
            RulePlanException.class,
            ex -> {
              assertThat(ex.getCode()).isEqualTo(RulePlanIssueCode.PLAN_OVERRIDE_INVALID);
              assertThat(ex.getMessage())
                  .contains(DependenteElegibilidadeRuleSet.SLOT_PERIODO_VIGENCIA);
            });

    // 2. PARAMETERIZABLE (Contrato 1.6): parâmetro fora da faixa (maxAgeIrrfStandard = 5 < 14) falha na compilação
    assertThatThrownBy(
            () ->
                facade.compilePlan(
                    DependenteElegibilidadeRuleSet.withCustomerBindings(
                        List.of(
                            DependenteElegibilidadeRuleSet.customerMode2ParameterizeBinding(
                                5, 24, 14, 24)))))
        .isInstanceOfSatisfying(
            RulePlanException.class,
            ex ->
                assertThat(ex.getCode())
                    .isEqualTo(RulePlanIssueCode.PLAN_PARAMETER_VALUES_INVALID));

    // No Órgão 2 (empCodigo = 2), maxAgeHealthPlan é parametrizado para 21 anos (vs 24 no Órgão 1)
    // E AUGMENT CERG-09301 exige documentoLegalId para universitario == true
    DependenteRuleContext dep22Health = validDependenteContext();
    dep22Health.draft().setDtnasc(LocalDate.of(2004, 1, 1)); // 22 anos
    dep22Health.draft().setDepIr(true);
    dep22Health.draft().setDepSalarioFamilia(false);
    dep22Health.draft().setDepPlanoSaude(true);
    dep22Health.draft().setEstudante(true);
    dep22Health.draft().setUniversitario(true);

    // Órgão 1 permite 22 anos no plano de saúde
    assertThat(facade.evaluateDependente(dep22Health).decision()).isEqualTo(RuleDecision.ALLOW);

    // Órgão 2 bloqueia 22 anos no plano de saúde por PARAMETERIZE (ERG-03177, DecisionSource.CUSTOMER)
    DocumentoLegal docOrgao2 = new DocumentoLegal();
    docOrgao2.setId(77L);
    docOrgao2.setEmpCodigo(2);
    DependenteRuleContext dep22HealthOrgao2 =
        new DependenteRuleContext(
            dep22Health.draft(),
            2,
            dep22Health.pessoa(),
            dep22Health.tipoDependente(),
            docOrgao2,
            List.of(),
            LocalDate.of(2026, 6, 1));
    RuleEvaluationResult resParamOrgao2 = facade.evaluateDependente(dep22HealthOrgao2);
    assertThat(resParamOrgao2.decision()).isEqualTo(RuleDecision.DENY);
    assertThat(resParamOrgao2.reasonCodes()).contains("ERG-03177");

    // 3. RESTRICT_ONLY (AUGMENT CERG-09301): universitário de 20 anos sem documentoLegalId
    // Passa no Órgão 1, mas bloqueia no Órgão 2 com CERG-09301 (DecisionSource.CUSTOMER)
    DependenteRuleContext uniSemDocOrgao1 = validDependenteContext();
    uniSemDocOrgao1.draft().setDtnasc(LocalDate.of(2006, 1, 1)); // 20 anos
    uniSemDocOrgao1.draft().setDepSalarioFamilia(false);
    uniSemDocOrgao1.draft().setDepPlanoSaude(false);
    uniSemDocOrgao1.draft().setEstudante(true);
    uniSemDocOrgao1.draft().setUniversitario(true);
    uniSemDocOrgao1.draft().setDocumentoLegalId(null);
    uniSemDocOrgao1 =
        new DependenteRuleContext(
            uniSemDocOrgao1.draft(),
            1,
            uniSemDocOrgao1.pessoa(),
            uniSemDocOrgao1.tipoDependente(),
            null,
            List.of(),
            LocalDate.of(2026, 6, 1));
    assertThat(facade.evaluateDependente(uniSemDocOrgao1).decision()).isEqualTo(RuleDecision.ALLOW);

    DependenteRuleContext uniSemDocOrgao2 =
        new DependenteRuleContext(
            uniSemDocOrgao1.draft(),
            2,
            uniSemDocOrgao1.pessoa(),
            uniSemDocOrgao1.tipoDependente(),
            null,
            List.of(),
            LocalDate.of(2026, 6, 1));
    RuleEvaluationResult resAugmentOrgao2 = facade.evaluateDependente(uniSemDocOrgao2);
    assertThat(resAugmentOrgao2.decision()).isEqualTo(RuleDecision.DENY);
    assertThat(resAugmentOrgao2.reasonCodes()).contains("CERG-09301");
    assertThatThrownBy(() -> facade.enforceDependente(uniSemDocOrgao2))
        .isInstanceOfSatisfying(
            ErgonRuleViolationException.class,
            ex ->
                assertThat(ex.getViolations())
                    .anySatisfy(
                        v -> {
                          assertThat(v.code()).isEqualTo("CERG-09301");
                          assertThat(v.source()).isEqualTo(DecisionSource.CUSTOMER);
                        }));

    // 4. REPLACEABLE (REPLACE_EXACT CERG-03198): cônjuge com depPlanoSaude = true sem documentoLegalId
    // Passa no Órgão 1, mas bloqueia no Órgão 2 com CERG-03198
    DependenteRuleContext conjugeSaudeSemDoc = validDependenteContext();
    conjugeSaudeSemDoc.draft().setParentesco("CONJUGE");
    conjugeSaudeSemDoc.draft().setTipodepen(null);
    conjugeSaudeSemDoc.draft().setDtnasc(LocalDate.of(1990, 1, 1));
    conjugeSaudeSemDoc.draft().setDepSalarioFamilia(false);
    conjugeSaudeSemDoc.draft().setDepPlanoSaude(true);
    conjugeSaudeSemDoc.draft().setDocumentoLegalId(null);
    DependenteRuleContext conjugeOrgao1 =
        new DependenteRuleContext(
            conjugeSaudeSemDoc.draft(),
            1,
            conjugeSaudeSemDoc.pessoa(),
            null,
            null,
            List.of(),
            LocalDate.of(2026, 6, 1));
    assertThat(facade.evaluateDependente(conjugeOrgao1).decision()).isEqualTo(RuleDecision.ALLOW);

    DependenteRuleContext conjugeOrgao2 =
        new DependenteRuleContext(
            conjugeSaudeSemDoc.draft(),
            2,
            conjugeSaudeSemDoc.pessoa(),
            null,
            null,
            List.of(),
            LocalDate.of(2026, 6, 1));
    RuleEvaluationResult resReplaceOrgao2 = facade.evaluateDependente(conjugeOrgao2);
    assertThat(resReplaceOrgao2.decision()).isEqualTo(RuleDecision.DENY);
    assertThat(resReplaceOrgao2.reasonCodes()).contains("CERG-03198");
  }

  static Stream<Arguments> vinculoProductDecisionViolations() {
    return Stream.of(
        Arguments.of(
            "ERG-00324",
            VinculoProvimentoRuleSet.SLOT_CADEIA_CRONOLOGICA,
            (Supplier<VinculoRuleContext>)
                () -> {
                  VinculoRuleContext base = validVinculoContext();
                  base.draft().setDtnom(LocalDate.of(2020, 2, 1));
                  base.draft().setDtposse(LocalDate.of(2020, 1, 15));
                  return base;
                }),
        Arguments.of(
            "ERG-00325",
            VinculoProvimentoRuleSet.SLOT_IDADE_MINIMA_INGRESSO,
            (Supplier<VinculoRuleContext>)
                () -> {
                  VinculoRuleContext base = validVinculoContext();
                  base.pessoa().setDtnasc(LocalDate.of(2005, 6, 1)); // 14 anos em 2020-01-15 (< 18)
                  return base;
                }),
        Arguments.of(
            "ERG-00354",
            VinculoProvimentoRuleSet.SLOT_COMPATIBILIDADE_REGIME_CARGO,
            (Supplier<VinculoRuleContext>)
                () -> {
                  VinculoRuleContext base = validVinculoContext();
                  base.draft().setRegimejur("CLT");
                  base.draft().setTipovinc("EFETIVO"); // CLT incompatível com provimento EFETIVO
                  return base;
                }),
        Arguments.of(
            "ERG-00359",
            VinculoProvimentoRuleSet.SLOT_VIGENCIA_CARGO_VAGAS,
            (Supplier<VinculoRuleContext>)
                () -> {
                  VinculoRuleContext base = validVinculoContext();
                  base.cargo().setControleVaga(true);
                  base.cargo().setVagasAutorizadas(2);
                  return new VinculoRuleContext(
                      base.draft(),
                      base.orgao(),
                      base.pessoa(),
                      base.cargo(),
                      2L, // vagas esgotadas (2 ocupadas de 2 autorizadas)
                      base.cargoComissao(),
                      base.cargoComissaoOcupadas(),
                      base.setor(),
                      base.setorExercicio(),
                      base.documentoLegal(),
                      base.existingVinculos(),
                      base.openAfastamentosCount(),
                      base.creating());
                }),
        Arguments.of(
            "ERG-00315",
            VinculoProvimentoRuleSet.SLOT_LIMITES_JORNADA_EXERCICIO,
            (Supplier<VinculoRuleContext>)
                () -> {
                  VinculoRuleContext base = validVinculoContext();
                  base.draft().setDtposse(LocalDate.of(2020, 1, 10));
                  base.draft().setDtexerc(LocalDate.of(2020, 2, 25)); // 46 dias (> 30 dias regulamentares)
                  return base;
                }),
        Arguments.of(
            "ERG-00342",
            VinculoProvimentoRuleSet.SLOT_VACANCIA_SEM_AFASTAMENTO,
            (Supplier<VinculoRuleContext>)
                () -> {
                  VinculoRuleContext base = validVinculoContext();
                  base.draft().setDtvac(LocalDate.of(2026, 5, 1));
                  base.draft().setFormavac("EXONERACAO");
                  return new VinculoRuleContext(
                      base.draft(),
                      base.orgao(),
                      base.pessoa(),
                      base.cargo(),
                      base.cargoOcupadas(),
                      base.cargoComissao(),
                      base.cargoComissaoOcupadas(),
                      base.setor(),
                      base.setorExercicio(),
                      base.documentoLegal(),
                      base.existingVinculos(),
                      1L, // 1 afastamento aberto impede vacância
                      false);
                }),
        Arguments.of(
            "ERG-00330",
            VinculoProvimentoRuleSet.SLOT_POLITICA_CONTRATO_TEMPORARIO,
            (Supplier<VinculoRuleContext>)
                () -> {
                  VinculoRuleContext base = validVinculoContext();
                  base.draft().setRegimejur("ADMINISTRATIVO");
                  base.draft().setTipovinc("TEMPORARIO");
                  base.draft().setDtinicontr(null); // contrato temporário sem vigência contratual
                  base.draft().setDtfimcontr(null);
                  return base;
                }),
        Arguments.of(
            "ERG-00300",
            VinculoProvimentoRuleSet.SLOT_ACUMULACAO_CONSTITUCIONAL,
            (Supplier<VinculoRuleContext>)
                () -> {
                  VinculoRuleContext base = validVinculoContext();
                  base.draft().setCategoria("ADMINISTRATIVO");
                  Vinculo activeOther = new Vinculo();
                  activeOther.setId(new VinculoId(1001L, 2));
                  activeOther.setEmpCodigo(1);
                  activeOther.setSituacao("ATIVO");
                  activeOther.setCategoria("SAUDE");
                  activeOther.setDtexerc(LocalDate.of(2018, 1, 1));
                  return new VinculoRuleContext(
                      base.draft(),
                      base.orgao(),
                      base.pessoa(),
                      base.cargo(),
                      base.cargoOcupadas(),
                      base.cargoComissao(),
                      base.cargoComissaoOcupadas(),
                      base.setor(),
                      base.setorExercicio(),
                      base.documentoLegal(),
                      List.of(concomitante(activeOther)),
                      0L,
                      true);
                }));
  }

  @ParameterizedTest
  @MethodSource("vinculoProductDecisionViolations")
  void vinculoProductRuleSetDeniesEveryInvalidDecisionWithExactReasonCodeAndSlot(
      String expectedCode, String expectedSlot, Supplier<VinculoRuleContext> contextSupplier) {
    VinculoRuleContext ctx = contextSupplier.get();
    RuleEvaluationResult result = facade.evaluateVinculo(ctx);
    assertThat(result.decision()).isEqualTo(RuleDecision.DENY);
    assertThat(result.reasonCodes()).contains(expectedCode);

    assertThatThrownBy(() -> facade.enforceVinculo(ctx))
        .isInstanceOfSatisfying(
            ErgonRuleViolationException.class,
            ex -> {
              assertThat(ex.getStatusCode()).isEqualTo(java.util.Set.of("ERG-03193", "ERG-00300").contains(expectedCode)
                  ? HttpStatus.CONFLICT : HttpStatus.UNPROCESSABLE_ENTITY);
              assertThat(ex.getReasonCodes()).contains(expectedCode);
              assertThat(ex.getSlotKeys()).contains(expectedSlot);
            });
  }

  @Test
  void vinculoConstitutionalAccumulationRequiresSchedulesForEligiblePairsAndRejectsThird() {
    Vinculo activeMagisterio = new Vinculo();
    activeMagisterio.setId(new VinculoId(1001L, 2));
    activeMagisterio.setEmpCodigo(1);
    activeMagisterio.setSituacao("ATIVO");
    activeMagisterio.setCategoria("MAGISTERIO");
    activeMagisterio.setDtexerc(LocalDate.of(2018, 1, 1));

    // 1. Segundo vínculo ativo em SAUDE com primeiro em MAGISTERIO exige prova de horários
    VinculoRuleContext base = validVinculoContext();
    base.draft().setCategoria("SAUDE");
    VinculoRuleContext twoValid =
        new VinculoRuleContext(
            base.draft(),
            base.orgao(),
            base.pessoa(),
            base.cargo(),
            base.cargoOcupadas(),
            base.cargoComissao(),
            base.cargoComissaoOcupadas(),
            base.setor(),
            base.setorExercicio(),
            base.documentoLegal(),
            List.of(concomitante(activeMagisterio)),
            0L,
            true);
    assertThat(facade.evaluateVinculo(twoValid).decision()).isEqualTo(RuleDecision.INCONCLUSIVE);

    // 2. 1 cargo técnico (TECNICO) + 1 cargo de professor (MAGISTERIO) -> requer comprovação de horários (CF/88 art. 37, XVI, "b")
    VinculoRuleContext baseTecnico = validVinculoContext();
    baseTecnico.draft().setCategoria("TECNICO");
    baseTecnico.cargo().setCategoria("TECNICO");
    VinculoRuleContext tecnicoPlusMagisterio =
        new VinculoRuleContext(
            baseTecnico.draft(),
            baseTecnico.orgao(),
            baseTecnico.pessoa(),
            baseTecnico.cargo(),
            baseTecnico.cargoOcupadas(),
            baseTecnico.cargoComissao(),
            baseTecnico.cargoComissaoOcupadas(),
            baseTecnico.setor(),
            baseTecnico.setorExercicio(),
            baseTecnico.documentoLegal(),
            List.of(concomitante(activeMagisterio)),
            0L,
            true);
    assertThat(facade.evaluateVinculo(tecnicoPlusMagisterio).decision())
        .isEqualTo(RuleDecision.INCONCLUSIVE);

    // 3. 2 cargos técnicos (TECNICO + TECNICO) -> DENY ERG-00300
    Vinculo activeTecnico2 = new Vinculo();
    activeTecnico2.setId(new VinculoId(1001L, 4));
    activeTecnico2.setEmpCodigo(1);
    activeTecnico2.setSituacao("ATIVO");
    activeTecnico2.setCategoria("TECNICO");
    activeTecnico2.setDtexerc(LocalDate.of(2019, 1, 1));
    VinculoRuleContext twoTecnicos =
        new VinculoRuleContext(
            baseTecnico.draft(),
            baseTecnico.orgao(),
            baseTecnico.pessoa(),
            baseTecnico.cargo(),
            baseTecnico.cargoOcupadas(),
            baseTecnico.cargoComissao(),
            baseTecnico.cargoComissaoOcupadas(),
            baseTecnico.setor(),
            baseTecnico.setorExercicio(),
            baseTecnico.documentoLegal(),
            List.of(concomitante(activeTecnico2)),
            0L,
            true);
    RuleEvaluationResult twoTecnicosRes = facade.evaluateVinculo(twoTecnicos);
    assertThat(twoTecnicosRes.decision()).isEqualTo(RuleDecision.DENY);
    assertThat(twoTecnicosRes.reasonCodes()).contains("ERG-00300");

    // 4. Terceiro vínculo ativo mesmo em SAUDE/MAGISTERIO -> DENY ERG-00300
    Vinculo activeSaude2 = new Vinculo();
    activeSaude2.setId(new VinculoId(1001L, 3));
    activeSaude2.setEmpCodigo(1);
    activeSaude2.setSituacao("ATIVO");
    activeSaude2.setCategoria("SAUDE");
    activeSaude2.setDtexerc(LocalDate.of(2019, 1, 1));

    VinculoRuleContext threeActive =
        new VinculoRuleContext(
            base.draft(),
            base.orgao(),
            base.pessoa(),
            base.cargo(),
            base.cargoOcupadas(),
            base.cargoComissao(),
            base.cargoComissaoOcupadas(),
            base.setor(),
            base.setorExercicio(),
            base.documentoLegal(),
            List.of(concomitante(activeMagisterio), concomitante(activeSaude2)),
            0L,
            true);
    RuleEvaluationResult thirdRes = facade.evaluateVinculo(threeActive);
    assertThat(thirdRes.decision()).isEqualTo(RuleDecision.DENY);
    assertThat(thirdRes.reasonCodes()).contains("ERG-00300");
  }

  @Test
  void vinculoAllFourCustomizationModesEnforcedBetweenOrgao1AndOrgao2() {
    // 1. FORBIDDEN: tentativa de sobrescrever guarda protegida de vínculo falha na compilação
    assertThatThrownBy(
            () ->
                facade.compilePlan(
                    VinculoProvimentoRuleSet.customerMode1ForbiddenOverrideAttempt()))
        .isInstanceOfSatisfying(
            RulePlanException.class,
            ex -> {
              assertThat(ex.getCode()).isEqualTo(RulePlanIssueCode.PLAN_OVERRIDE_INVALID);
              assertThat(ex.getMessage())
                  .contains(VinculoProvimentoRuleSet.SLOT_CADEIA_CRONOLOGICA);
            });

    // 2. PARAMETERIZABLE (Contrato 1.6): parâmetro fora da faixa (maxWeeklyHours = 200 > 168) falha na compilação
    assertThatThrownBy(
            () ->
                facade.compilePlan(
                    VinculoProvimentoRuleSet.withCustomerBindings(
                        List.of(
                            VinculoProvimentoRuleSet.customerMode2ParameterizeBinding(200, 15)))))
        .isInstanceOfSatisfying(
            RulePlanException.class,
            ex ->
                assertThat(ex.getCode())
                    .isEqualTo(RulePlanIssueCode.PLAN_PARAMETER_VALUES_INVALID));

    // No Órgão 2 (empCodigo = 2), maxWeeklyHours = 44 e maxDaysPosseToExercicio = 15
    VinculoRuleContext vincJornada60 = validVinculoContext();
    vincJornada60.draft().setJornadaSemanal(60);
    vincJornada60.draft().setDtposse(LocalDate.of(2020, 1, 10));
    vincJornada60.draft().setDtexerc(LocalDate.of(2020, 1, 29)); // 19 dias

    // Órgão 1 permite (<= 168h e <= 30 dias)
    assertThat(facade.evaluateVinculo(vincJornada60).decision()).isEqualTo(RuleDecision.ALLOW);

    // Órgão 2 bloqueia por PARAMETERIZE (ERG-00315, DecisionSource.CUSTOMER)
    VinculoRuleContext vincJornada60Orgao2 = toOrgao2VinculoContext(vincJornada60);
    RuleEvaluationResult resParamOrgao2 = facade.evaluateVinculo(vincJornada60Orgao2);
    assertThat(resParamOrgao2.decision()).isEqualTo(RuleDecision.DENY);
    assertThat(resParamOrgao2.reasonCodes()).contains("ERG-00315");

    // 3. RESTRICT_ONLY (AUGMENT CERG-09401): vacância ou cargo em comissão sem documentoLegalId no Órgão 2
    VinculoRuleContext vacSemDocOrgao1 = validVinculoContext();
    vacSemDocOrgao1.draft().setSituacao("VACANTE");
    vacSemDocOrgao1.draft().setDtvac(LocalDate.of(2026, 5, 1));
    vacSemDocOrgao1.draft().setFormavac("EXONERACAO");
    vacSemDocOrgao1.draft().setDocumentoLegalId(null);
    vacSemDocOrgao1 =
        new VinculoRuleContext(
            vacSemDocOrgao1.draft(),
            vacSemDocOrgao1.orgao(),
            vacSemDocOrgao1.pessoa(),
            vacSemDocOrgao1.cargo(),
            0L,
            null,
            0L,
            vacSemDocOrgao1.setor(),
            vacSemDocOrgao1.setorExercicio(),
            null,
            List.of(),
            0L,
            false);
    assertThat(facade.evaluateVinculo(vacSemDocOrgao1).decision()).isEqualTo(RuleDecision.ALLOW);

    VinculoRuleContext vacSemDocOrgao2 = toOrgao2VinculoContext(vacSemDocOrgao1);
    RuleEvaluationResult resAugmentOrgao2 = facade.evaluateVinculo(vacSemDocOrgao2);
    assertThat(resAugmentOrgao2.decision()).isEqualTo(RuleDecision.DENY);
    assertThat(resAugmentOrgao2.reasonCodes()).contains("CERG-09401");

    // 4. REPLACEABLE (REPLACE_EXACT CERG-00330): contrato temporário de 900 dias (> 730 dias)
    // Passa no Órgão 1, mas bloqueia no Órgão 2 com CERG-00330
    VinculoRuleContext temp900Orgao1 = validVinculoContext();
    temp900Orgao1.draft().setRegimejur("ADMINISTRATIVO");
    temp900Orgao1.draft().setTipovinc("TEMPORARIO");
    temp900Orgao1.draft().setDtinicontr(LocalDate.of(2025, 1, 1));
    temp900Orgao1.draft().setDtfimcontr(LocalDate.of(2027, 6, 20)); // ~900 dias
    assertThat(facade.evaluateVinculo(temp900Orgao1).decision()).isEqualTo(RuleDecision.ALLOW);

    VinculoRuleContext temp900Orgao2 = toOrgao2VinculoContext(temp900Orgao1);
    RuleEvaluationResult resReplaceOrgao2 = facade.evaluateVinculo(temp900Orgao2);
    assertThat(resReplaceOrgao2.decision()).isEqualTo(RuleDecision.DENY);
    assertThat(resReplaceOrgao2.reasonCodes()).contains("CERG-00330");
  }

  @Test
  @SuppressWarnings("unchecked")
  void customFieldValidationRulesSyntacticValidationWithPraxisJsonLogicEngine() {
    EntityManager em = mock(EntityManager.class);
    ErgonInvariants invariants = new ErgonInvariants();
    ReflectionTestUtils.setField(invariants, "em", em);
    ReflectionTestUtils.setField(invariants, "ruleEngineFacade", facade);

    Orgao orgao = new Orgao();
    orgao.setEmpresa(1);
    orgao.setAtiva(true);
    when(em.find(eq(Orgao.class), eq(1), any(LockModeType.class))).thenReturn(orgao);

    TypedQuery<DefinicaoCampoCustomizado> dupQuery = mock(TypedQuery.class);
    when(em.createQuery(anyString(), eq(DefinicaoCampoCustomizado.class))).thenReturn(dupQuery);
    when(dupQuery.setParameter(anyString(), any())).thenReturn(dupQuery);
    when(dupQuery.setFlushMode(any(FlushModeType.class))).thenReturn(dupQuery);
    when(dupQuery.getResultList()).thenReturn(List.of());

    DefinicaoCampoCustomizado def = new DefinicaoCampoCustomizado();
    def.setId(950L);
    def.setEmpCodigo(1);
    def.setResourceKey("administracao-pessoal.vinculos");
    def.setFieldKey("processo_sei_nomeacao");
    def.setFieldType("STRING");
    def.setControlType("INPUT");
    def.setLabel("Processo SEI de Nomeação");
    def.setFieldOrder(10);
    def.setActive(true);

    // 1. Expressão JSON Logic válida (usando value, customFields e draft) é aceita
    def.setValidationRules(
        Map.of(
            "and",
            List.of(
                Map.of("!=", List.of(Map.of("var", "value"), "")),
                Map.of(
                    "or",
                    List.of(
                        Map.of("!=", List.of(Map.of("var", "draft.regimejur"), "ESTATUTARIO")),
                        Map.of("!=", java.util.Arrays.asList(Map.of("var", "value"), null)))))));
    invariants.campoCustomizado(def, true);

    DefinicaoCampoCustomizadoService ccService =
        new DefinicaoCampoCustomizadoService(
            mock(DefinicaoCampoCustomizadoRepository.class),
            new DefinicaoCampoCustomizadoMapper(),
            new TenantContext(),
            facade);
    ccService.validateCampoCustomizadoValidationRules(def);

    // 2. Operador JSON Logic inexistente -> rejeitado com HTTP 422 (ERG-21501)
    def.setValidationRules(
        Map.of("operador_inexistente_xyz", List.of(Map.of("var", "value"), "SEI")));
    assertThatThrownBy(() -> invariants.campoCustomizado(def, false))
        .isInstanceOfSatisfying(
            ErgonRuleViolationException.class,
            ex -> {
              assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
              assertThat(ex.getReasonCodes()).contains(ErgonRuleErrorCatalog.ERG_21501.code());
              assertThat(ex.getSlotKeys()).contains(ErgonRuleErrorCatalog.ERG_21501.slotKey());
            });

    // 3. Variável raiz fora do escopo permitido (ex.: secret.token) -> rejeitado com HTTP 422 (ERG-21501)
    def.setValidationRules(
        Map.of("==", List.of(Map.of("var", "secretRoot.token"), "admin")));
    assertThatThrownBy(() -> ccService.validateCampoCustomizadoValidationRules(def))
        .isInstanceOfSatisfying(
            ErgonRuleViolationException.class,
            ex -> {
              assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
              assertThat(ex.getReasonCodes()).contains(ErgonRuleErrorCatalog.ERG_21501.code());
            });
  }

  @Test
  void customFieldValidationRulesRuntimeEvaluationAcrossVinculoDependenteFrequenciaAndAfastamento() {
    ErgonInvariants invariants = new ErgonInvariants();
    ReflectionTestUtils.setField(invariants, "ruleEngineFacade", facade);

    // 1. Vinculo: regra JSON Logic exigindo que jornada_teletrabalho_max <= draft.jornadaSemanal
    DefinicaoCampoCustomizado defVinculo = new DefinicaoCampoCustomizado();
    defVinculo.setId(951L);
    defVinculo.setEmpCodigo(1);
    defVinculo.setResourceKey("administracao-pessoal.vinculos");
    defVinculo.setFieldKey("horas_teletrabalho");
    defVinculo.setFieldType("INTEGER");
    defVinculo.setControlType("NUMERIC_TEXT_BOX");
    defVinculo.setRequired(true);
    defVinculo.setActive(true);
    defVinculo.setValidationRules(
        Map.of(
            "and",
            List.of(
                Map.of(">=", List.of(Map.of("var", "value"), 4)),
                Map.of("<=", List.of(Map.of("var", "value"), Map.of("var", "draft.jornadaSemanal"))))));

    Vinculo vinculo = validVinculoContext().draft();
    vinculo.setJornadaSemanal(40);

    // Caso positivo: 20 <= 40 -> passa
    invariants.customFields(
        "administracao-pessoal.vinculos",
        1,
        Map.of("horas_teletrabalho", 20),
        vinculo,
        List.of(defVinculo));

    // Caso negativo: 44 > 40 -> bloqueia com HTTP 422 (ERG-21502)
    assertThatThrownBy(
            () ->
                invariants.customFields(
                    "administracao-pessoal.vinculos",
                    1,
                    Map.of("horas_teletrabalho", 44),
                    vinculo,
                    List.of(defVinculo)))
        .isInstanceOfSatisfying(
            ErgonRuleViolationException.class,
            ex -> {
              assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
              assertThat(ex.getReasonCodes()).contains(ErgonRuleErrorCatalog.ERG_21502.code());
              assertThat(ex.getViolations().get(0).targetField())
                  .isEqualTo("customFields.horas_teletrabalho");
            });

    // 2. Dependente: regra JSON Logic exigindo numero_carteira_plano quando draft.depPlanoSaude == true
    DefinicaoCampoCustomizado defDependente = new DefinicaoCampoCustomizado();
    defDependente.setId(952L);
    defDependente.setEmpCodigo(1);
    defDependente.setResourceKey("administracao-pessoal.dependentes");
    defDependente.setFieldKey("numero_carteira_plano");
    defDependente.setFieldType("STRING");
    defDependente.setControlType("INPUT");
    defDependente.setRequired(false);
    defDependente.setActive(true);
    defDependente.setValidationRules(
        Map.of(
            "or",
            List.of(
                Map.of("!=", List.of(Map.of("var", "draft.depPlanoSaude"), true)),
                Map.of(
                    "and",
                    List.of(
                        Map.of("!=", java.util.Arrays.asList(Map.of("var", "value"), null)),
                        Map.of("!=", List.of(Map.of("var", "value"), "")))))));

    Dependente dep = validDependenteContext().draft();
    dep.setDepPlanoSaude(true);

    // Positivo: carteira informada quando depPlanoSaude == true
    invariants.customFields(
        "administracao-pessoal.dependentes",
        1,
        Map.of("numero_carteira_plano", "CARTEIRA-2026-99"),
        dep,
        List.of(defDependente));

    // Negativo: carteira omitida quando depPlanoSaude == true -> bloqueia com HTTP 422 (ERG-21502)
    assertThatThrownBy(
            () ->
                invariants.customFields(
                    "administracao-pessoal.dependentes",
                    1,
                    Map.of(),
                    dep,
                    List.of(defDependente)))
        .isInstanceOfSatisfying(
            ErgonRuleViolationException.class,
            ex -> {
              assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
              assertThat(ex.getReasonCodes()).contains(ErgonRuleErrorCatalog.ERG_21502.code());
            });

    // 3. Frequencia e Afastamento: avaliação determinística sobre customFields e draft
    DefinicaoCampoCustomizado defFreq = new DefinicaoCampoCustomizado();
    defFreq.setId(953L);
    defFreq.setEmpCodigo(1);
    defFreq.setResourceKey("administracao-pessoal.frequencias");
    defFreq.setFieldKey("codigo_ponto_biometrico");
    defFreq.setFieldType("STRING");
    defFreq.setControlType("INPUT");
    defFreq.setRequired(true);
    defFreq.setActive(true);
    defFreq.setValidationRules(
        Map.of("in", List.of(Map.of("var", "value"), List.of("BIO-OK", "BIO-JUSTIFICADO"))));

    Frequencia freq = new Frequencia();
    freq.setId(701L);
    freq.setEmpCodigo(1);
    freq.setSituacao("PENDENTE_HOMOLOGACAO");
    invariants.customFields(
        "administracao-pessoal.frequencias",
        1,
        Map.of("codigo_ponto_biometrico", "BIO-OK"),
        freq,
        List.of(defFreq));
    assertThatThrownBy(
            () ->
                invariants.customFields(
                    "administracao-pessoal.frequencias",
                    1,
                    Map.of("codigo_ponto_biometrico", "INVALIDO"),
                    freq,
                    List.of(defFreq)))
        .isInstanceOf(ErgonRuleViolationException.class);

    DefinicaoCampoCustomizado defAfast = new DefinicaoCampoCustomizado();
    defAfast.setId(954L);
    defAfast.setEmpCodigo(1);
    defAfast.setResourceKey("administracao-pessoal.afastamentos");
    defAfast.setFieldKey("cid_homologado_junta");
    defAfast.setFieldType("BOOLEAN");
    defAfast.setControlType("CHECKBOX");
    defAfast.setRequired(true);
    defAfast.setActive(true);
    defAfast.setValidationRules(Map.of("==", List.of(Map.of("var", "value"), true)));

    Afastamento afast = new Afastamento();
    afast.setId(801L);
    afast.setEmpCodigo(1);
    afast.setSituacao("HOMOLOGADO");
    invariants.customFields(
        "administracao-pessoal.afastamentos",
        1,
        Map.of("cid_homologado_junta", true),
        afast,
        List.of(defAfast));
    assertThatThrownBy(
            () ->
                invariants.customFields(
                    "administracao-pessoal.afastamentos",
                    1,
                    Map.of("cid_homologado_junta", false),
                    afast,
                    List.of(defAfast)))
        .isInstanceOf(ErgonRuleViolationException.class);
  }

  @Test
  void dependenteAndVinculoServicesEnforceFailClosed422AndProduceStructuredDiagnostics() {
    TenantContext tenant = new TenantContext();
    DependenteService depService =
        new DependenteService(
            mock(DependenteRepository.class), new DependenteMapper(), tenant, facade);
    VinculoService vincService =
        new VinculoService(mock(VinculoRepository.class), new VinculoMapper(), tenant, facade);

    DependenteRuleContext validDepCtx = validDependenteContext();
    validDepCtx.draft().setPessoa(validDepCtx.pessoa());
    validDepCtx.draft().setTipoDependenteEntidade(validDepCtx.tipoDependente());
    validDepCtx.draft().setDocumentoLegal(validDepCtx.documentoLegal());

    Map<String, Object> depDiag =
        depService.evaluateDependenteDiagnostic(
            validDepCtx.draft(), 1, LocalDate.of(2026, 6, 1));
    assertThat(depDiag)
        .containsEntry("allowed", true)
        .containsEntry("decision", "ALLOW")
        .containsEntry("ruleSetKey", DependenteElegibilidadeRuleSet.RULESET_KEY);

    validDepCtx.draft().setDtnasc(LocalDate.of(2000, 1, 1)); // 26 anos -> estoura teto etário
    assertThatThrownBy(
            () ->
                depService.validateDependenteElegibilidade(
                    validDepCtx.draft(), 1, LocalDate.of(2026, 6, 1)))
        .isInstanceOfSatisfying(
            ErgonRuleViolationException.class,
            ex -> assertThat(ex.getReasonCodes()).contains("ERG-03177"));

    VinculoRuleContext validVincCtx = validVinculoContext();
    validVincCtx.draft().setOrgao(validVincCtx.orgao());
    validVincCtx.draft().setPessoa(validVincCtx.pessoa());
    validVincCtx.draft().setCargoEntidade(validVincCtx.cargo());
    validVincCtx.draft().setLotacao(validVincCtx.setor());
    validVincCtx.draft().setExercicio(validVincCtx.setorExercicio());

    Map<String, Object> vincDiag =
        vincService.evaluateVinculoDiagnostic(validVincCtx.draft(), true);
    assertThat(vincDiag)
        .containsEntry("allowed", false)
        .containsEntry("decision", "INCONCLUSIVE")
        .containsEntry("ruleSetKey", VinculoProvimentoRuleSet.RULESET_KEY);

    validVincCtx.draft().setJornadaSemanal(180); // > 168h -> bloqueia ERG-00315
    assertThatThrownBy(() -> vincService.validateVinculoProvimento(validVincCtx.draft(), true))
        .isInstanceOfSatisfying(
            ErgonRuleViolationException.class,
            ex -> assertThat(ex.getReasonCodes()).contains("ERG-00315"));
  }

  static DependenteRuleContext validDependenteContext() {
    ServidorPessoa titular = new ServidorPessoa();
    titular.setNumero(1001L);
    titular.setNome("Servidor Titular");
    titular.setCpf("11122233344");
    titular.setDtnasc(LocalDate.of(1985, 3, 15));
    titular.setSexo("F");

    TipoDependente tipo = new TipoDependente();
    tipo.setTipodepen("FILHO_MENOR");
    tipo.setDescricao("Filho(a) até 21 anos");
    tipo.setGeraIr(true);
    tipo.setGeraSalarioFamilia(true);
    tipo.setIdadeLimite(21);

    DocumentoLegal doc = new DocumentoLegal();
    doc.setId(77L);
    doc.setEmpCodigo(1);
    doc.setNumeroDocumento("CERT-2016/77");

    Dependente draft = new Dependente();
    draft.setId(new DependenteId(1001L, 1));
    draft.setNome("Dependente Filho Menor");
    draft.setSexo("M");
    draft.setParentesco("FILHO");
    draft.setTipodepen("FILHO_MENOR");
    draft.setCpf("99988877766");
    draft.setDtnasc(LocalDate.of(2016, 5, 10)); // 10 anos em 2026-06-01
    draft.setDtini(LocalDate.of(2016, 5, 10));
    draft.setDepIr(true);
    draft.setDepSalarioFamilia(true);
    draft.setDepPlanoSaude(true);
    draft.setEstudante(true);
    draft.setUniversitario(false);
    draft.setInvalido(false);
    draft.setExcepcional(false);
    draft.setRepresentanteLegal(false);
    draft.setDocumentoLegalId(77L);

    return new DependenteRuleContext(
        draft, 1, titular, tipo, doc, List.of(), LocalDate.of(2026, 6, 1));
  }

  @Test
  void ec138AppliesAtPublicationAndAnEligiblePairStillNeedsScheduleEvidence() {
    var base = validVinculoContext();
    var priorTeaching = new ErgonRuleEngineFacade.VinculoConcomitante(
        LocalDate.of(2020, 1, 1), null, "MAGISTERIO");
    var context = new VinculoRuleContext(base.draft(), base.orgao(), base.pessoa(), base.cargo(),
        base.cargoOcupadas(), base.cargoComissao(), base.cargoComissaoOcupadas(), base.setor(),
        base.setorExercicio(), base.documentoLegal(), List.of(priorTeaching), 0L, true);
    base.draft().setDtnom(LocalDate.of(2025, 12, 19));
    base.draft().setDtposse(LocalDate.of(2025, 12, 20));
    base.draft().setDtexerc(LocalDate.of(2025, 12, 21));
    assertThat(facade.evaluateVinculo(context).reasonCodes()).contains("ERG-00300");
    base.draft().setDtexerc(LocalDate.of(2025, 12, 22));
    var result = facade.evaluateVinculo(context);
    assertThat(result.decision()).isEqualTo(RuleDecision.INCONCLUSIVE);
    assertThat(result.reasonCodes()).doesNotContain("ERG-00300");
  }

  @Test
  void accumulationEvaluatesConcreteSchedulesOnlyWithFullCoverageAndTravelTime() {
    var base = validVinculoContext();
    var date = LocalDate.of(2026, 1, 15);
    base.draft().setDtnom(date.minusDays(2));
    base.draft().setDtposse(date.minusDays(1));
    base.draft().setDtexerc(date);
    var tz = java.time.ZoneId.of("America/Sao_Paulo");
    var first = expediente("2026-01-15T11:00:00Z", "2026-01-15T15:00:00Z");
    var nonOverlapping = expediente("2026-01-15T15:30:00Z", "2026-01-15T19:30:00Z");
    var conflicting = expediente("2026-01-15T14:59:00Z", "2026-01-15T18:59:00Z");
    var insufficientTravel = expediente("2026-01-15T15:29:00Z", "2026-01-15T19:29:00Z");
    var travel = java.time.Duration.ofMinutes(30);
    for (var window : List.of(nonOverlapping, conflicting, insufficientTravel)) {
      var proof = new br.com.praxis.ergonshowcase.rules.ProvaHorariosAcumulacao(
          date, date, tz, travel, List.of(first), List.of(window));
      var fact = new ErgonRuleEngineFacade.VinculoConcomitante(date, date, "MAGISTERIO", proof);
      var result = facade.evaluateVinculo(withConcomitant(base, fact));
      if (window == nonOverlapping) assertThat(result.decision()).isEqualTo(RuleDecision.ALLOW);
      else assertThat(result.reasonCodes()).contains("ERG-00300");
      if (window == nonOverlapping) {
        var partial = new ErgonRuleEngineFacade.VinculoConcomitante(date, date.plusDays(1), "MAGISTERIO", proof);
        assertThat(facade.evaluateVinculo(withConcomitant(base, partial)).decision())
            .isEqualTo(RuleDecision.INCONCLUSIVE);
      }
    }
  }

  private static br.com.praxis.ergonshowcase.rules.ProvaHorariosAcumulacao.Expediente expediente(
      String inicio, String fim) {
    return new br.com.praxis.ergonshowcase.rules.ProvaHorariosAcumulacao.Expediente(
        java.time.Instant.parse(inicio), java.time.Instant.parse(fim));
  }

  private static VinculoRuleContext withConcomitant(
      VinculoRuleContext base, ErgonRuleEngineFacade.VinculoConcomitante fact) {
    return new VinculoRuleContext(base.draft(), base.orgao(), base.pessoa(), base.cargo(),
        base.cargoOcupadas(), base.cargoComissao(), base.cargoComissaoOcupadas(), base.setor(),
        base.setorExercicio(), base.documentoLegal(), List.of(fact), 0L, true);
  }

  private static ErgonRuleEngineFacade.VinculoConcomitante concomitante(Vinculo vinculo) {
    return new ErgonRuleEngineFacade.VinculoConcomitante(
        vinculo.getDtexerc(), vinculo.getDtvac(), vinculo.getCategoria());
  }

  static VinculoRuleContext validVinculoContext() {
    Orgao orgao = new Orgao();
    orgao.setEmpresa(1);
    orgao.setNome("Secretaria de Administração");
    orgao.setAtiva(true);

    ServidorPessoa pessoa = new ServidorPessoa();
    pessoa.setNumero(1001L);
    pessoa.setNome("Servidor Titular");
    pessoa.setCpf("11122233344");
    pessoa.setDtnasc(LocalDate.of(1990, 1, 1)); // 30 anos em 2020-01-15
    pessoa.setSexo("F");

    Cargo cargo = new Cargo();
    cargo.setCargo(101);
    cargo.setEmpCodigo(1);
    cargo.setNome("Analista em Gestão Pública");
    cargo.setCategoria("ADMINISTRATIVO");
    cargo.setTipoCargo("EFETIVO");
    cargo.setControleVaga(true);
    cargo.setVagasAutorizadas(10);

    Setor setor = new Setor();
    setor.setSetor("SETOR-01");
    setor.setEmpCodigo(1);
    setor.setNome("Diretoria de Gestão de Pessoas");

    DocumentoLegal doc = new DocumentoLegal();
    doc.setId(50L);
    doc.setEmpCodigo(1);
    doc.setNumeroDocumento("PORT-2020/50");

    Vinculo draft = new Vinculo();
    draft.setId(new VinculoId(1001L, 1));
    draft.setEmpCodigo(1);
    draft.setMatric("MAT-1001-1");
    draft.setRegimejur("ESTATUTARIO");
    draft.setTipovinc("EFETIVO");
    draft.setCategoria("ADMINISTRATIVO");
    draft.setSituacao("ATIVO");
    draft.setCargo(101);
    draft.setSetor("SETOR-01");
    draft.setSetorExercicio("SETOR-01");
    draft.setJornadaSemanal(40);
    draft.setDtnom(LocalDate.of(2020, 1, 5));
    draft.setDtposse(LocalDate.of(2020, 1, 10));
    draft.setDtexerc(LocalDate.of(2020, 1, 15));
    draft.setDocumentoLegalId(50L);

    return new VinculoRuleContext(
        draft, orgao, pessoa, cargo, 3L, null, 0L, setor, setor, doc, List.of(), 0L, true);
  }

  private static VinculoRuleContext toOrgao2VinculoContext(VinculoRuleContext base) {
    base.draft().setEmpCodigo(2);
    Orgao orgao2 = new Orgao();
    orgao2.setEmpresa(2);
    orgao2.setAtiva(true);
    Cargo cargo2 = new Cargo();
    cargo2.setCargo(101);
    cargo2.setEmpCodigo(2);
    cargo2.setCategoria(base.cargo().getCategoria());
    cargo2.setTipoCargo(base.cargo().getTipoCargo());
    cargo2.setControleVaga(true);
    cargo2.setVagasAutorizadas(10);
    Setor setor2 = new Setor();
    setor2.setSetor("SETOR-01");
    setor2.setEmpCodigo(2);
    DocumentoLegal doc2 = null;
    if (base.documentoLegal() != null) {
      doc2 = new DocumentoLegal();
      doc2.setId(base.documentoLegal().getId());
      doc2.setEmpCodigo(2);
    }
    return new VinculoRuleContext(
        base.draft(),
        orgao2,
        base.pessoa(),
        cargo2,
        base.cargoOcupadas(),
        null,
        0L,
        setor2,
        setor2,
        doc2,
        base.existingVinculos(),
        base.openAfastamentosCount(),
        base.creating());
  }
}
