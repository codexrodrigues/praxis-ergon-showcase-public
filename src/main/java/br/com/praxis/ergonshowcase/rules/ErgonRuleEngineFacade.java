package br.com.praxis.ergonshowcase.rules;

import br.com.praxis.ergonshowcase.config.ServidorPessoaAgeOptions;
import br.com.praxis.ergonshowcase.domain.Afastamento;
import br.com.praxis.ergonshowcase.domain.Cargo;
import br.com.praxis.ergonshowcase.domain.CodigoFrequencia;
import br.com.praxis.ergonshowcase.domain.DefinicaoCampoCustomizado;
import br.com.praxis.ergonshowcase.domain.Dependente;
import br.com.praxis.ergonshowcase.domain.DesignacaoFuncao;
import br.com.praxis.ergonshowcase.domain.DocumentoLegal;
import br.com.praxis.ergonshowcase.domain.FeriasGozo;
import br.com.praxis.ergonshowcase.domain.Frequencia;
import br.com.praxis.ergonshowcase.domain.FuncaoConfianca;
import br.com.praxis.ergonshowcase.domain.VagaFuncao;
import br.com.praxis.ergonshowcase.domain.Orgao;
import br.com.praxis.ergonshowcase.domain.ParametroContagemFerias;
import br.com.praxis.ergonshowcase.domain.PeriodoAquisitivoFerias;
import br.com.praxis.ergonshowcase.domain.RegraFrequenciaEstatutaria;
import br.com.praxis.ergonshowcase.domain.ServidorPessoa;
import br.com.praxis.ergonshowcase.domain.Setor;
import br.com.praxis.ergonshowcase.domain.TipoDependente;
import br.com.praxis.ergonshowcase.domain.TipoFrequencia;
import br.com.praxis.ergonshowcase.domain.Vinculo;
import br.com.praxis.ergonshowcase.rules.EffectiveVacationParameterResolver.EffectiveParametroFeriasFacts;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.praxisplatform.rules.contract.DecisionBinding;
import org.praxisplatform.rules.contract.DecisionSource;
import org.praxisplatform.rules.contract.DecisionStage;
import org.praxisplatform.rules.contract.RuleDecision;
import org.praxisplatform.rules.contract.RuleEvaluationResult;
import org.praxisplatform.rules.contract.RuleExecutorResult;
import org.praxisplatform.rules.contract.RuleSetDefinition;
import org.praxisplatform.rules.contract.RuleSetRef;
import org.praxisplatform.rules.facts.PraxisFactTreeBuilder;
import org.praxisplatform.rules.jsonlogic.PraxisJsonLogicEngine;
import org.praxisplatform.rules.jsonlogic.PraxisJsonLogicException;
import org.praxisplatform.rules.jsonlogic.model.JsonLogicEvaluationOptions;
import org.praxisplatform.rules.plan.PraxisRulePlanCompiler;
import org.praxisplatform.rules.plan.RuleDecisionPlan;
import org.praxisplatform.rules.runtime.PraxisRuleSetEngine;
import org.praxisplatform.rules.runtime.RuleBindingExecutor;
import org.praxisplatform.rules.runtime.RuleBindingExecutorRegistry;
import org.praxisplatform.rules.runtime.RuleExecutorContext;
import org.springframework.stereotype.Component;

/**
 * Fachada determinística e <i>fail-closed</i> sobre o {@code praxis-rules-engine} para compilação
 * de {@link RuleDecisionPlan} via {@link PraxisRulePlanCompiler} e avaliação em memória via
 * {@link PraxisRuleSetEngine}.
 *
 * <p>Reúne as famílias de regras de administração de pessoal e os planos ativos por órgão.
 *
 * <p>Toda consulta JPA/SQL ocorre na camada de serviço antes de invocar o motor; os executores
 * JSON Logic e Java registrados nesta fachada operam exclusivamente sobre a árvore imutável de
 * fatos ({@link JsonNode}), garantindo execução pura, determinística e auditável por slot.
 */
@Component
public class ErgonRuleEngineFacade {

  public static final String DEFAULT_TIME_ZONE = "America/Sao_Paulo";
  public static final List<String> CUSTOM_FIELD_JSON_LOGIC_ROOTS =
      List.of("value", "customFields", "draft", "entity");
  private static final LocalDate OPEN_ENDED_DATE = LocalDate.of(9999, 12, 31);
  private static final Set<String> INACTIVE_SITUACOES = Set.of("CANCELADO", "REJEITADO");
  private static final Set<String> PROFESSOR_CATEGORIAS =
      Set.of("MAGISTERIO", "DOCENTE", "PROFESSOR");
  private static final Set<String> SAUDE_CATEGORIAS = Set.of("SAUDE", "PROFISSIONAL_SAUDE");
  private static final Set<String> TECNICO_CIENTIFICO_CATEGORIAS =
      Set.of("TECNICO", "TECNICO_CIENTIFICO", "CIENTIFICO");

  /** Contexto pré-carregado para avaliação determinística de elegibilidade de {@link Dependente}. */
  public record DependenteRuleContext(
      Dependente draft,
      Integer empCodigo,
      ServidorPessoa pessoa,
      TipoDependente tipoDependente,
      DocumentoLegal documentoLegal,
      List<Dependente> existingDependentes,
      LocalDate referenceDate) {

    public DependenteRuleContext(
        Dependente draft,
        ServidorPessoa pessoa,
        TipoDependente tipoDependente,
        DocumentoLegal documentoLegal,
        List<Dependente> existingDependentes,
        LocalDate referenceDate) {
      this(draft, null, pessoa, tipoDependente, documentoLegal, existingDependentes, referenceDate);
    }

    public DependenteRuleContext(
        Dependente draft,
        ServidorPessoa pessoa,
        TipoDependente tipoDependente,
        DocumentoLegal documentoLegal,
        List<Dependente> existingDependentes) {
      this(draft, null, pessoa, tipoDependente, documentoLegal, existingDependentes, null);
    }
  }

  public static final Set<String> VINCULO_EM_EXERCICIO_OU_AFASTADO =
      Set.of("ATIVO", "AFASTADO", "CEDIDO");
  private static final LocalDate EC_138_VIGENCIA = LocalDate.of(2025, 12, 22);

  /** Dados mínimos do vínculo alheio, exclusivos da aferição de acumulação. */
  public record VinculoConcomitante(
      LocalDate inicio, LocalDate fim, String categoria, ProvaHorariosAcumulacao horarios) {
    public VinculoConcomitante(LocalDate inicio, LocalDate fim, String categoria) {
      this(inicio, fim, categoria, null);
    }
  }

  /** Contexto pré-carregado para avaliação determinística de provimento/manutenção de {@link Vinculo}. */
  public record VinculoRuleContext(
      Vinculo draft,
      Orgao orgao,
      ServidorPessoa pessoa,
      Cargo cargo,
      long cargoOcupadas,
      Cargo cargoComissao,
      long cargoComissaoOcupadas,
      Setor setor,
      Setor setorExercicio,
      DocumentoLegal documentoLegal,
      List<VinculoConcomitante> existingVinculos,
      long openAfastamentosCount,
      boolean creating) {}

  /** Contexto pré-carregado para avaliação determinística de lançamento/manutenção de {@link Frequencia}. */
  public record FrequenciaRuleContext(
      Frequencia draft,
      Vinculo vinculo,
      ServidorPessoa pessoa,
      CodigoFrequencia codigoFrequencia,
      RegraFrequenciaEstatutaria effectiveStatutoryRule,
      List<Frequencia> existingFrequencias,
      List<Afastamento> activeAfastamentos,
      LocalDate referenceDate) {

    public FrequenciaRuleContext(
        Frequencia draft,
        Vinculo vinculo,
        ServidorPessoa pessoa,
        CodigoFrequencia codigoFrequencia,
        RegraFrequenciaEstatutaria effectiveStatutoryRule,
        List<Frequencia> existingFrequencias,
        List<Afastamento> activeAfastamentos) {
      this(
          draft,
          vinculo,
          pessoa,
          codigoFrequencia,
          effectiveStatutoryRule,
          existingFrequencias,
          activeAfastamentos,
          null);
    }
  }

  /** Contexto pré-carregado para avaliação determinística de concessão/manutenção de {@link Afastamento}. */
  public record AfastamentoRuleContext(
      Afastamento draft,
      Vinculo vinculo,
      ServidorPessoa pessoa,
      CodigoFrequencia codigoFrequencia,
      RegraFrequenciaEstatutaria effectiveStatutoryRule,
      List<Afastamento> existingAfastamentos,
      List<Frequencia> activeFrequencias,
      Afastamento afastamentoOrigem,
      Vinculo substitutoVinculo,
      boolean inPlaceProrrogacao,
      LocalDate previsaoAnterior) {

    public AfastamentoRuleContext(Afastamento draft, Vinculo vinculo, ServidorPessoa pessoa,
        CodigoFrequencia codigoFrequencia, RegraFrequenciaEstatutaria effectiveStatutoryRule,
        List<Afastamento> existingAfastamentos, List<Frequencia> activeFrequencias,
        Afastamento afastamentoOrigem, Vinculo substitutoVinculo, boolean inPlaceProrrogacao) {
      this(draft, vinculo, pessoa, codigoFrequencia, effectiveStatutoryRule, existingAfastamentos,
          activeFrequencias, afastamentoOrigem, substitutoVinculo, inPlaceProrrogacao, null);
    }

    public AfastamentoRuleContext comPrevisaoAnterior(LocalDate previsaoAnterior) {
      return new AfastamentoRuleContext(draft, vinculo, pessoa, codigoFrequencia,
          effectiveStatutoryRule, existingAfastamentos, activeFrequencias, afastamentoOrigem,
          substitutoVinculo, inPlaceProrrogacao, previsaoAnterior);
    }

    public AfastamentoRuleContext(
        Afastamento draft,
        Vinculo vinculo,
        ServidorPessoa pessoa,
        CodigoFrequencia codigoFrequencia,
        RegraFrequenciaEstatutaria effectiveStatutoryRule,
        List<Afastamento> existingAfastamentos,
        List<Frequencia> activeFrequencias,
        Afastamento afastamentoOrigem,
        Vinculo substitutoVinculo) {
      this(
          draft,
          vinculo,
          pessoa,
          codigoFrequencia,
          effectiveStatutoryRule,
          existingAfastamentos,
          activeFrequencias,
          afastamentoOrigem,
          substitutoVinculo,
          false);
    }
  }

  /** Contexto pré-carregado para avaliação determinística de {@link ParametroContagemFerias}. */
  public record ParametroContagemFeriasRuleContext(
      ParametroContagemFerias draft, List<ParametroContagemFerias> existingRules) {}

  /** Contexto pré-carregado para avaliação determinística de {@link PeriodoAquisitivoFerias}. */
  public record PeriodoAquisitivoFeriasRuleContext(
      PeriodoAquisitivoFerias draft,
      PeriodoAquisitivoFerias previousSnapshot,
      Vinculo vinculo,
      ParametroContagemFerias resolvedParametroBase,
      List<ParametroContagemFerias> parametroCandidates,
      List<PeriodoAquisitivoFerias> existingPeriodos,
      List<FeriasGozo> parcelasDoPeriodo,
      List<Frequencia> activeFrequencias,
      List<Afastamento> activeAfastamentos,
      boolean lockedAverbacaoMutation) {

    public PeriodoAquisitivoFeriasRuleContext(
        PeriodoAquisitivoFerias draft,
        Vinculo vinculo,
        List<ParametroContagemFerias> parametroCandidates,
        List<PeriodoAquisitivoFerias> existingPeriodos,
        List<FeriasGozo> parcelasDoPeriodo) {
      this(
          draft,
          null,
          vinculo,
          null,
          parametroCandidates,
          existingPeriodos,
          parcelasDoPeriodo,
          List.of(),
          List.of(),
          false);
    }

    public PeriodoAquisitivoFeriasRuleContext(
        PeriodoAquisitivoFerias draft,
        Vinculo vinculo,
        ParametroContagemFerias resolvedParametroBase,
        List<ParametroContagemFerias> parametroCandidates,
        List<PeriodoAquisitivoFerias> existingPeriodos,
        List<FeriasGozo> parcelasDoPeriodo,
        boolean lockedAverbacaoMutation) {
      this(
          draft,
          null,
          vinculo,
          resolvedParametroBase,
          parametroCandidates,
          existingPeriodos,
          parcelasDoPeriodo,
          List.of(),
          List.of(),
          lockedAverbacaoMutation);
    }
  }

  /** Contexto pré-carregado para avaliação determinística de {@link FeriasGozo}. */
  public record FeriasGozoRuleContext(
      FeriasGozo draft,
      PeriodoAquisitivoFerias periodoAquisitivo,
      Vinculo vinculo,
      Vinculo substitutoVinculo,
      Boolean substitutoVinculoValidoOverride,
      ParametroContagemFerias resolvedParametroBase,
      List<ParametroContagemFerias> parametroCandidates,
      List<PeriodoAquisitivoFerias> existingPeriodosDoVinculo,
      Integer ordemPaNoVinculoOverride,
      List<FeriasGozo> existingParcelasDoVinculo,
      List<Frequencia> activeFrequencias,
      List<Afastamento> activeAfastamentos,
      LocalDate referenceDate,
      boolean activeSubstitutionConflict,
      boolean enforceStrictResidualBalance) {

    public FeriasGozoRuleContext(
        FeriasGozo draft,
        PeriodoAquisitivoFerias periodoAquisitivo,
        Vinculo vinculo,
        Vinculo substitutoVinculo,
        List<ParametroContagemFerias> parametroCandidates,
        List<PeriodoAquisitivoFerias> existingPeriodosDoVinculo,
        List<FeriasGozo> existingParcelasDoVinculo,
        List<Frequencia> activeFrequencias,
        List<Afastamento> activeAfastamentos) {
      this(
          draft,
          periodoAquisitivo,
          vinculo,
          substitutoVinculo,
          null,
          null,
          parametroCandidates,
          existingPeriodosDoVinculo,
          null,
          existingParcelasDoVinculo,
          activeFrequencias,
          activeAfastamentos,
          null,
          false,
          true);
    }

    public FeriasGozoRuleContext(
        FeriasGozo draft,
        PeriodoAquisitivoFerias periodoAquisitivo,
        Vinculo vinculo,
        Vinculo substitutoVinculo,
        ParametroContagemFerias resolvedParametroBase,
        List<ParametroContagemFerias> parametroCandidates,
        List<PeriodoAquisitivoFerias> existingPeriodosDoVinculo,
        List<FeriasGozo> existingParcelasDoVinculo,
        List<Frequencia> activeFrequencias,
        List<Afastamento> activeAfastamentos,
        LocalDate referenceDate,
        boolean activeSubstitutionConflict,
        boolean enforceStrictResidualBalance) {
      this(
          draft,
          periodoAquisitivo,
          vinculo,
          substitutoVinculo,
          null,
          resolvedParametroBase,
          parametroCandidates,
          existingPeriodosDoVinculo,
          null,
          existingParcelasDoVinculo,
          activeFrequencias,
          activeAfastamentos,
          referenceDate,
          activeSubstitutionConflict,
          enforceStrictResidualBalance);
    }
  }

  private final ObjectMapper mapper;
  private final PraxisJsonLogicEngine jsonLogicEngine;
  private final RuleBindingExecutorRegistry executorRegistry;
  private final org.praxisplatform.rules.runtime.GovernedRuleEngineTemplate template;
  private final EffectiveVacationParameterResolver vacationParameterResolver =
      new EffectiveVacationParameterResolver();


  private record RuleFamily(RuleSetDefinition product, RuleSetDefinition orgao2) {}
  private record OrganizationRuleSet(String key, int empCodigo) {}

  private final Map<String, RuleFamily> ruleFamilies = new LinkedHashMap<>();
  private final String activationEpoch = java.util.UUID.randomUUID().toString();
  private final Map<OrganizationRuleSet, java.util.UUID> activationRevisions = new HashMap<>();

  /** Token da ativação em memória. O mesmo digest pode reaparecer após um reset. */
  public synchronized String activationTag(String key, int orgao) {
    var scope = new OrganizationRuleSet(key, orgao);
    var revision = activationRevisions.get(scope);
    if (revision == null) {
      revision = java.util.UUID.nameUUIDFromBytes(
          (activationEpoch + ":" + key + ":" + orgao).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
    return "\"" + revision + "\"";
  }

  private final Map<OrganizationRuleSet, RuleDecisionPlan> activePlans = new ConcurrentHashMap<>();
  private final ReferenceQueue<RuleDecisionPlan> retiredPlans = new ReferenceQueue<>();
  private final Map<PlanReference, Map<String, ErgonRuleErrorCatalog.RuleErrorDescriptor>> planErrors =
      new HashMap<>();

  // Descritores acompanham a identidade da publicação, inclusive durante avaliações em curso.
  private static final class PlanReference extends WeakReference<RuleDecisionPlan> {
    private final int hash;

    private PlanReference(RuleDecisionPlan plan, ReferenceQueue<RuleDecisionPlan> queue) {
      super(plan, queue);
      hash = System.identityHashCode(plan);
    }

    @Override public int hashCode() {
      return hash;
    }

    @Override public boolean equals(Object other) {
      return this == other || (other instanceof PlanReference ref && get() != null && get() == ref.get());
    }
  }

  private void addFamily(RuleSetDefinition product, RuleSetDefinition orgao2) {
    ruleFamilies.put(product.ref().ruleSetKey(), new RuleFamily(product, orgao2));
  }

  public List<String> ruleSetKeys() {
    return List.copyOf(ruleFamilies.keySet());
  }

  public RuleSetDefinition productRuleSet(String ruleSetKey) {
    return family(ruleSetKey).product();
  }

  private RuleFamily family(String ruleSetKey) {
    var family = ruleFamilies.get(ruleSetKey);
    if (family == null) throw new IllegalArgumentException("RuleSet desconhecido: " + ruleSetKey);
    return family;
  }

  public RuleSetDefinition effectiveRuleSet(String ruleSetKey, Integer empCodigo) {
    return resolvePlan(ruleSetKey, empCodigo).definition();
  }

  public RuleDecisionPlan resolvePlan(String ruleSetKey, Integer empCodigo) {
    var family = family(ruleSetKey);
    var active = empCodigo == null ? null : activePlans.get(new OrganizationRuleSet(ruleSetKey, empCodigo));
    return resolvePlan(family, empCodigo, active);
  }

  public RuleDecisionPlan factoryPlan(String ruleSetKey, int empCodigo) {
    return resolvePlan(family(ruleSetKey), empCodigo, null);
  }

  private RuleDecisionPlan resolvePlan(RuleFamily family, Integer empCodigo, RuleDecisionPlan active) {
    var definition = active != null ? active.definition()
        : Objects.equals(empCodigo, 2) ? family.orgao2() : family.product();
    if (empCodigo != null && family.product() == servidorProduto) {
      var limits = servidorAgeOptions.getPorOrgao().get(empCodigo);
      if (limits != null) {
        var configured = CadastroPessoalRuleSetSupport.parameterize(definition,
            Map.of("ERG-00235", limits.idadeMinima(), "ERG-00236", limits.idadeMaximaFunc()));
        if (!definition.equals(configured)) {
          var configuredPlan = template.compile(configured, "orgao:" + empCodigo);
          if (active != null) rememberErrors(configuredPlan, localErrors(active));
          return configuredPlan;
        }
      }
    }
    if (active != null) return active;
    return template.compile(definition, Objects.equals(empCodigo, 2) ? "factory:2" : "product");
  }

  public RuleDecisionPlan registerPlan(String ruleSetKey, int empCodigo, RuleSetDefinition definition) {
    return registerPlan(ruleSetKey, empCodigo, definition, Map.of());
  }

  /** Compila o candidato e publica seus descritores locais junto com o plano ativo. */
  public synchronized RuleDecisionPlan registerPlan(String ruleSetKey, int empCodigo,
      RuleSetDefinition definition, Map<String, ErgonRuleErrorCatalog.RuleErrorDescriptor> errors) {
    if (empCodigo <= 0) throw new IllegalArgumentException("Órgão inválido.");
    validateProductIdentity(ruleSetKey, definition);
    var plan = new PraxisRulePlanCompiler(executorRegistry).compile(definition);
    rememberErrors(plan, errors);
    var scope = new OrganizationRuleSet(ruleSetKey, empCodigo);
    activePlans.put(scope, plan);
    activationRevisions.put(scope, java.util.UUID.randomUUID());
    return plan;
  }

  /**
   * Compila uma publicação previamente verificada pelo Config para o escopo autorizado pelo host.
   * A admissão não altera a seleção ativa. Ambiente e órgão vêm do contexto validado do servidor.
   */
  public org.praxisplatform.rules.snapshot.CompiledRuleSnapshot admitGovernedSnapshot(
      String ruleSetKey, int authorizedOrganization, String authorizedEnvironment, String hostContractVersion,
      org.praxisplatform.rules.contract.PublishedRuleSnapshot snapshot) {
    if (authorizedOrganization <= 0 || !Integer.toString(authorizedOrganization).equals(snapshot.tenantId())
        || !snapshot.environment().equals(authorizedEnvironment)
        || !"praxis-ergon-showcase".equals(snapshot.ownerServiceKey())) {
      throw new IllegalArgumentException("Publicação fora do escopo autorizado do Ergon.");
    }
    validateProductIdentity(ruleSetKey, snapshot.ruleSet());
    return new org.praxisplatform.rules.snapshot.PraxisRuleSnapshotCompiler(executorRegistry)
        .compile(snapshot, hostContractVersion);
  }

  private void validateProductIdentity(String ruleSetKey, RuleSetDefinition definition) {
    var product = productRuleSet(ruleSetKey);
    if (!product.ref().sameIdentity(definition.ref())) {
      throw new IllegalArgumentException("Identidade do RuleSet inválida.");
    }
    if (!product.slots().equals(definition.slots()) || !product.availableRoots().equals(definition.availableRoots())
        || product.failPolicy() != definition.failPolicy()) {
      throw new IllegalArgumentException("Os slots, fatos e política de falha pertencem ao produto.");
    }
    var protectedBindings = product.bindings().stream()
        .filter(binding -> product.slots().stream().anyMatch(slot -> slot.slotKey().equals(binding.slotKey())
            && slot.overridePolicy() == org.praxisplatform.rules.contract.OverridePolicy.FORBIDDEN))
        .toList();
    if (!definition.bindings().containsAll(protectedBindings)) {
      throw new IllegalArgumentException("O candidato alterou decisões protegidas do produto.");
    }
    for (var descriptor : definition.errorDescriptors().values()) {
      if (ErgonRuleErrorCatalog.find(descriptor.code()).isPresent()) {
        throw new IllegalArgumentException("O descritor do cliente usa um código reservado do produto.");
      }
    }
    for (var binding : definition.bindings()) {
      if (binding.enabled()
          && binding.executor() != null
          && binding.executor().type() == org.praxisplatform.rules.contract.RuleExecutorType.JSON_LOGIC
          && binding.executor().expression() != null) {
        final List<String> referencedRoots;
        try {
          referencedRoots = ErgonOntologyRuleSetSupport.roots(binding.executor().expression());
        } catch (IllegalStateException ex) {
          throw new IllegalArgumentException(ex.getMessage(), ex);
        }
        if (!definition.availableRoots().containsAll(referencedRoots)) {
          throw new IllegalArgumentException("O binding customizado referencia raiz ontológica fora de availableRoots.");
        }
        boolean coversAllRoots = referencedRoots.stream().allMatch(root ->
            binding.requiredFactPaths().stream()
                .anyMatch(path -> path.equals(root) || path.startsWith(root + ".")));
        if (!coversAllRoots) {
          throw new IllegalArgumentException("O binding customizado omite raízes ontológicas obrigatórias em requiredFactPaths.");
        }
      }
    }
  }

  public RuleDecisionPlan compileGovernedCandidate(String ruleSetKey, RuleSetDefinition definition) {
    validateProductIdentity(ruleSetKey, definition);
    return compilePlan(definition);
  }

  public List<org.praxisplatform.rules.contract.RuleImplementationRef> governedImplementations() {
    return ruleSetKeys().stream().map(this::productRuleSet)
        .flatMap(definition -> definition.bindings().stream())
        .map(org.praxisplatform.rules.contract.DecisionBinding::executor)
        .filter(java.util.Objects::nonNull)
        .map(org.praxisplatform.rules.contract.RuleExecutorRef::implementationKey)
        .filter(java.util.Objects::nonNull).distinct().sorted()
        .map(executorRegistry::implementationRef).toList();
  }

  public synchronized RuleDecisionPlan resetOrgaoPlan(String ruleSetKey, int empCodigo) {
    family(ruleSetKey);
    var scope = new OrganizationRuleSet(ruleSetKey, empCodigo);
    activePlans.remove(scope);
    activationRevisions.put(scope, java.util.UUID.randomUUID());
    return resolvePlan(ruleSetKey, empCodigo);
  }

  public synchronized Map<String, ErgonRuleErrorCatalog.RuleErrorDescriptor> localErrors(RuleDecisionPlan plan) {
    clearRetiredErrors();
    return planErrors.getOrDefault(new PlanReference(plan, null), Map.of());
  }

  private synchronized void rememberErrors(RuleDecisionPlan plan,
      Map<String, ErgonRuleErrorCatalog.RuleErrorDescriptor> errors) {
    clearRetiredErrors();
    if (!errors.isEmpty()) planErrors.put(new PlanReference(plan, retiredPlans), Map.copyOf(errors));
  }

  private void clearRetiredErrors() {
    for (var reference = retiredPlans.poll(); reference != null; reference = retiredPlans.poll()) {
      planErrors.remove(reference);
    }
  }

  private final RuleSetDefinition cargoProduto = CargoQuadroRuleSet.productRuleSet();
  private final RuleSetDefinition servidorProduto = ServidorPessoaCadastroRuleSet.productRuleSet();
  private final RuleSetDefinition defaultProductDependentePlan = DependenteElegibilidadeRuleSet.productRuleSet();
  private final RuleSetDefinition defaultProductVinculoPlan = VinculoProvimentoRuleSet.productRuleSet();
  private final RuleSetDefinition defaultProductRn013Plan = Rn013RegrasFrequenciaRuleSet.productRuleSet();
  private final RuleSetDefinition defaultProductFrequenciaPlan = FrequenciaLancamentoRuleSet.productRuleSet();
  private final RuleSetDefinition defaultProductAfastamentoPlan = AfastamentoConcessaoRuleSet.productRuleSet();
  private final RuleSetDefinition defaultProductParametroContagemFeriasPlan = ParametroContagemFeriasRuleSet.productRuleSet();
  private final RuleSetDefinition defaultProductPeriodoAquisitivoFeriasPlan = PeriodoAquisitivoFeriasRuleSet.productRuleSet();
  private final RuleSetDefinition defaultProductFeriasGozoPlan = FeriasGozoRuleSet.productRuleSet();
  private final RuleSetDefinition defaultProductAverbacaoTempoServicoPlan = AverbacaoTempoServicoRuleSet.productRuleSet();
  private final RuleSetDefinition defaultProductPeriodoAquisitivoLicencaEspecialPlan = PeriodoAquisitivoLicencaEspecialRuleSet.productRuleSet();
  private final RuleSetDefinition defaultProductLicencaEspecialGozoPlan = LicencaEspecialGozoRuleSet.productRuleSet();
  private final RuleSetDefinition defaultProductVendaLicencaEspecialPlan = VendaLicencaEspecialRuleSet.productRuleSet();
  private final RuleSetDefinition defaultProductFuncaoConfiancaPlan = FuncaoConfiancaRuleSet.productRuleSet();
  private final RuleSetDefinition defaultProductVagaFuncaoPlan = VagaFuncaoRuleSet.productRuleSet();
  private final RuleSetDefinition defaultProductDesignacaoFuncaoPlan = DesignacaoFuncaoRuleSet.productRuleSet();
  private final RuleSetDefinition defaultProductSubstituicaoFuncaoPlan = SubstituicaoFuncaoRuleSet.productRuleSet();

  public RuleEvaluationResult evaluateAverbacaoTempoServico(int orgao, ObjectNode facts) {
    return evaluate(resolveAverbacaoTempoServicoPlan(orgao), facts);
  }

  public RuleEvaluationResult enforceAverbacaoTempoServico(int orgao, ObjectNode facts) {
    return enforce(resolveAverbacaoTempoServicoPlan(orgao), facts);
  }

  public RuleDecisionPlan resolveAverbacaoTempoServicoPlan(int orgao) {
    return resolvePlan(defaultProductAverbacaoTempoServicoPlan.ref().ruleSetKey(), orgao);
  }

  public RuleEvaluationResult evaluatePeriodoAquisitivoLicencaEspecial(int orgao, ObjectNode facts) {
    return evaluate(resolvePeriodoAquisitivoLicencaEspecialPlan(orgao), facts);
  }

  public RuleEvaluationResult enforcePeriodoAquisitivoLicencaEspecial(int orgao, ObjectNode facts) {
    return enforce(resolvePeriodoAquisitivoLicencaEspecialPlan(orgao), facts);
  }

  public RuleDecisionPlan resolvePeriodoAquisitivoLicencaEspecialPlan(int orgao) {
    return resolvePlan(defaultProductPeriodoAquisitivoLicencaEspecialPlan.ref().ruleSetKey(), orgao);
  }

  public RuleEvaluationResult evaluateLicencaEspecialGozo(int orgao, ObjectNode facts) {
    return evaluate(resolveLicencaEspecialGozoPlan(orgao), facts);
  }

  public RuleEvaluationResult enforceLicencaEspecialGozo(int orgao, ObjectNode facts) {
    return enforce(resolveLicencaEspecialGozoPlan(orgao), facts);
  }

  public RuleDecisionPlan resolveLicencaEspecialGozoPlan(int orgao) {
    return resolvePlan(defaultProductLicencaEspecialGozoPlan.ref().ruleSetKey(), orgao);
  }

  public RuleEvaluationResult evaluateVendaLicencaEspecial(int orgao, ObjectNode facts) {
    return evaluate(resolveVendaLicencaEspecialPlan(orgao), facts);
  }

  public RuleEvaluationResult enforceVendaLicencaEspecial(int orgao, ObjectNode facts) {
    return enforce(resolveVendaLicencaEspecialPlan(orgao), facts);
  }

  public RuleDecisionPlan resolveVendaLicencaEspecialPlan(int orgao) {
    return resolvePlan(defaultProductVendaLicencaEspecialPlan.ref().ruleSetKey(), orgao);
  }

  public RuleDecisionPlan registerOrgaoAverbacaoTempoServicoPlan(int orgao, RuleSetDefinition definition) {
    return registerPlan(defaultProductAverbacaoTempoServicoPlan.ref().ruleSetKey(), orgao, definition);
  }

  public RuleDecisionPlan registerOrgaoPeriodoAquisitivoLicencaEspecialPlan(int orgao, RuleSetDefinition definition) {
    return registerPlan(defaultProductPeriodoAquisitivoLicencaEspecialPlan.ref().ruleSetKey(), orgao, definition);
  }

  public RuleDecisionPlan registerOrgaoLicencaEspecialGozoPlan(int orgao, RuleSetDefinition definition) {
    return registerPlan(defaultProductLicencaEspecialGozoPlan.ref().ruleSetKey(), orgao, definition);
  }

  public RuleDecisionPlan registerOrgaoVendaLicencaEspecialPlan(int orgao, RuleSetDefinition definition) {
    return registerPlan(defaultProductVendaLicencaEspecialPlan.ref().ruleSetKey(), orgao, definition);
  }

  public ErgonRuleEngineFacade() {
    this.mapper = new ObjectMapper();
    this.jsonLogicEngine = new PraxisJsonLogicEngine();
    this.executorRegistry =
        new RuleBindingExecutorRegistry(
            List.of(
                concomitanciaJavaExecutor(),
                frequenciaSobreposicaoJavaExecutor(),
                afastamentoSobreposicaoJavaExecutor(),
                dependenteCpfUnicidadeJavaExecutor(),
                vinculoAcumulacaoConstitucionalJavaExecutor(),
                parametroContagemConcomitanciaJavaExecutor()));
    this.template = new org.praxisplatform.rules.runtime.GovernedRuleEngineTemplate(this.executorRegistry, ErgonRuleErrorCatalog.canonical());

    addFamily(cargoProduto, CargoQuadroRuleSet.orgao2CustomizedRuleSet());
    addFamily(servidorProduto, ServidorPessoaCadastroRuleSet.orgao2CustomizedRuleSet());
    addFamily(defaultProductDependentePlan, DependenteElegibilidadeRuleSet.orgao2CustomizedRuleSet());
    addFamily(defaultProductVinculoPlan, VinculoProvimentoRuleSet.orgao2CustomizedRuleSet());
    addFamily(defaultProductRn013Plan, Rn013RegrasFrequenciaRuleSet.orgao2CustomizedRuleSet());
    addFamily(defaultProductFrequenciaPlan, FrequenciaLancamentoRuleSet.orgao2CustomizedRuleSet());
    addFamily(defaultProductAfastamentoPlan, AfastamentoConcessaoRuleSet.orgao2CustomizedRuleSet());
    addFamily(defaultProductParametroContagemFeriasPlan, ParametroContagemFeriasRuleSet.orgao2CustomizedRuleSet());
    addFamily(defaultProductPeriodoAquisitivoFeriasPlan, PeriodoAquisitivoFeriasRuleSet.orgao2CustomizedRuleSet());
    addFamily(defaultProductFeriasGozoPlan, FeriasGozoRuleSet.orgao2CustomizedRuleSet());
    addFamily(defaultProductAverbacaoTempoServicoPlan, AverbacaoTempoServicoRuleSet.orgao2CustomizedRuleSet());
    addFamily(defaultProductPeriodoAquisitivoLicencaEspecialPlan, PeriodoAquisitivoLicencaEspecialRuleSet.orgao2CustomizedRuleSet());
    addFamily(defaultProductLicencaEspecialGozoPlan, LicencaEspecialGozoRuleSet.orgao2CustomizedRuleSet());
    addFamily(defaultProductVendaLicencaEspecialPlan, VendaLicencaEspecialRuleSet.orgao2CustomizedRuleSet());
    addFamily(defaultProductFuncaoConfiancaPlan, FuncaoConfiancaRuleSet.orgao2CustomizedRuleSet());
    addFamily(defaultProductVagaFuncaoPlan, VagaFuncaoRuleSet.orgao2CustomizedRuleSet());
    addFamily(defaultProductDesignacaoFuncaoPlan, DesignacaoFuncaoRuleSet.orgao2CustomizedRuleSet());
    addFamily(defaultProductSubstituicaoFuncaoPlan, SubstituicaoFuncaoRuleSet.orgao2CustomizedRuleSet());
  }

  public EffectiveVacationParameterResolver getVacationParameterResolver() {
    return vacationParameterResolver;
  }

  public PraxisJsonLogicEngine getJsonLogicEngine() {
    return jsonLogicEngine;
  }

  /** Compila e valida estaticamente um {@link RuleSetDefinition} contra o registro de executores. */
  public RuleDecisionPlan compilePlan(RuleSetDefinition definition) {
    return template.compile(Objects.requireNonNull(definition), "ad-hoc");
  }

  /** Retorna o {@link RuleDecisionPlan} compilado da {@code RN-013} para o órgão informado. */
  public RuleDecisionPlan resolveRn013Plan(Integer empCodigo) {
    return resolvePlan(defaultProductRn013Plan.ref().ruleSetKey(), empCodigo);
  }

  /** Registra ou atualiza em memória um plano customizado da {@code RN-013} para um órgão específico. */
  public RuleDecisionPlan registerOrgaoRn013Plan(Integer empCodigo, RuleSetDefinition definition) {
    return registerPlan(defaultProductRn013Plan.ref().ruleSetKey(), empCodigo, definition);
  }

  /** Retorna o {@link RuleDecisionPlan} compilado de lançamento de frequência para o órgão informado. */
  public RuleDecisionPlan resolveFrequenciaPlan(Integer empCodigo) {
    return resolvePlan(defaultProductFrequenciaPlan.ref().ruleSetKey(), empCodigo);
  }

  /** Registra ou atualiza em memória um plano customizado de frequência para um órgão específico. */
  public RuleDecisionPlan registerOrgaoFrequenciaPlan(
      Integer empCodigo, RuleSetDefinition definition) {
    return registerPlan(defaultProductFrequenciaPlan.ref().ruleSetKey(), empCodigo, definition);
  }

  /** Retorna o {@link RuleDecisionPlan} compilado de concessão de afastamento para o órgão informado. */
  public RuleDecisionPlan resolveAfastamentoPlan(Integer empCodigo) {
    return resolvePlan(defaultProductAfastamentoPlan.ref().ruleSetKey(), empCodigo);
  }

  /** Registra ou atualiza em memória um plano customizado de afastamento para um órgão específico. */
  public RuleDecisionPlan registerOrgaoAfastamentoPlan(
      Integer empCodigo, RuleSetDefinition definition) {
    return registerPlan(defaultProductAfastamentoPlan.ref().ruleSetKey(), empCodigo, definition);
  }

  /** Retorna o {@link RuleDecisionPlan} compilado de elegibilidade de dependente para o órgão informado. */
  public RuleDecisionPlan resolveDependentePlan(Integer empCodigo) {
    return resolvePlan(defaultProductDependentePlan.ref().ruleSetKey(), empCodigo);
  }

  /** Registra ou atualiza em memória um plano customizado de dependente para um órgão específico. */
  public RuleDecisionPlan registerOrgaoDependentePlan(
      Integer empCodigo, RuleSetDefinition definition) {
    return registerPlan(defaultProductDependentePlan.ref().ruleSetKey(), empCodigo, definition);
  }

  /** Retorna o {@link RuleDecisionPlan} compilado de provimento de vínculo para o órgão informado. */
  public RuleDecisionPlan resolveVinculoPlan(Integer empCodigo) {
    return resolvePlan(defaultProductVinculoPlan.ref().ruleSetKey(), empCodigo);
  }

  /** Registra ou atualiza em memória um plano customizado de vínculo para um órgão específico. */
  public RuleDecisionPlan registerOrgaoVinculoPlan(
      Integer empCodigo, RuleSetDefinition definition) {
    return registerPlan(defaultProductVinculoPlan.ref().ruleSetKey(), empCodigo, definition);
  }

  /** Avalia um {@link RuleDecisionPlan} compilado sobre os fatos normalizados. */
  public RuleEvaluationResult evaluate(RuleDecisionPlan plan, JsonNode facts) {
    return evaluate(plan, facts, Instant.now().toString());
  }

  /** Avalia um {@link RuleDecisionPlan} compilado com instante UTC explícito para reprodutibilidade. */
  public RuleEvaluationResult evaluate(RuleDecisionPlan plan, JsonNode facts, String nowUtc) {
    Objects.requireNonNull(plan, "plan must not be null");
    Objects.requireNonNull(facts, "facts must not be null");
    return template.evaluate(plan, facts, nowUtc, DEFAULT_TIME_ZONE);
  }

  /**
   * Avalia um {@link RuleDecisionPlan} e impõe semântica <i>fail-closed</i>: qualquer decisão final
   * diferente de {@link RuleDecision#ALLOW} lança {@link ErgonRuleViolationException} (HTTP 422).
   */
  public RuleEvaluationResult enforce(RuleDecisionPlan plan, JsonNode facts) {
    RuleEvaluationResult result = template.evaluate(plan, facts, Instant.now().toString(), DEFAULT_TIME_ZONE);
    if (result.decision() != RuleDecision.ALLOW) throw new ErgonRuleViolationException(plan, result, localErrors(plan));
    return result;
  }

  private final Map<OperationalRuleSet, RuleSetDefinition> operationalDefinitions = new ConcurrentHashMap<>();

  public RuleEvaluationResult enforceOperational(OperationalRuleSet rule, Integer orgao, Object... values) {
    var definition = operationalDefinitions.computeIfAbsent(rule, key -> key.productRuleSet(mapper));
    String tenantKey = orgao == null ? "global" : "orgao:" + orgao;
    return enforce(template.compile(definition, tenantKey), rule.facts(mapper, values));
  }

  public void enforceCargoAglutinadorPreview(int orgao, Integer cargo, Boolean habilitado) {
    enforcePreviewBinding(orgao, cargoProduto, "CERG-02105",
        PraxisFactTreeBuilder.create()
            .putDraft(draft -> draft.put("pai", cargo))
            .putConcept("cargoAglutinador", c -> c.put("aglutinador", habilitado))
            .build());
  }

  public void enforceCodigoAfastamentoPreview(int orgao, String tipo, Integer codigo, Boolean habilitado) {
    enforcePreviewBinding(orgao, defaultProductAfastamentoPlan, "ERG-04306",
        ocorrenciaPreviewFacts(tipo, codigo, "afastamento", habilitado));
  }

  public void enforceCodigoFrequenciaPreview(int orgao, String tipo, Integer codigo, Boolean habilitado) {
    enforcePreviewBinding(orgao, defaultProductFrequenciaPlan, "ERG-00400",
        ocorrenciaPreviewFacts(tipo, codigo, "viaFrequencia", habilitado));
  }

  private ObjectNode ocorrenciaPreviewFacts(String tipo, Integer codigo, String flag, Boolean habilitado) {
    return PraxisFactTreeBuilder.create()
        .putDraft(draft -> draft.put("tipofreq", tipo).put("codfreq", codigo).put("dtini", null))
        .putConcept("codigoFrequencia", c -> c.put("present", true).put("tipofreq", tipo).put(flag, habilitado))
        .putConcept("tipoFrequencia", c -> c.put("present", false))
        .build();
  }

  public void enforceAfastamentoPreviewDates(int orgao, LocalDate inicio, LocalDate fim) {
    var facts = PraxisFactTreeBuilder.create()
        .putDraft(draft -> draft.put("dtini", inicio).put("dtprevfim", fim).put("tipofreq", null).put("codfreq", null))
        .build();
    enforcePreviewBinding(orgao, defaultProductAfastamentoPlan, "ERG-04313", facts);
  }

  public void enforceFrequenciaPreviewDates(int orgao, LocalDate inicio, LocalDate fim) {
    var facts = PraxisFactTreeBuilder.create()
        .putDraft(draft -> draft.put("dtini", inicio).put("dtfim", fim).put("tipofreq", null).put("codfreq", null))
        .build();
    enforcePreviewBinding(orgao, defaultProductFrequenciaPlan, "ERG-00415", facts);
  }

  public void enforcePeriodoFeriasPreviewDates(int orgao, LocalDate inicio, LocalDate fim) {
    var facts = PraxisFactTreeBuilder.create()
        .putDraft(draft -> draft.put("dtini", inicio).put("dtfim", fim))
        .build();
    enforcePreviewBinding(orgao, defaultProductPeriodoAquisitivoFeriasPlan, "ERG-01851", facts);
  }

  public void enforceGozoFeriasPreviewDates(int orgao, LocalDate inicio, LocalDate fim, int dias) {
    var facts = PraxisFactTreeBuilder.create()
        .putDraft(draft -> draft.put("dtini", inicio).put("dtprevfim", fim).put("diasGozo", dias))
        .build();
    var binding = defaultProductFeriasGozoPlan.bindings().stream()
        .filter(candidate -> FeriasGozoRuleSet.BINDING_CRONOLOGIA.equals(candidate.bindingKey()))
        .findFirst().orElseThrow();
    enforcePreviewBinding(orgao, defaultProductFeriasGozoPlan, binding, facts);
  }

  public void enforceDependentePreviewBirthDate(int orgao, LocalDate dtnasc, LocalDate referenceDate) {
    if (dtnasc == null) {
      return;
    }
    LocalDate refDate = referenceDate != null ? referenceDate : LocalDate.now();
    int age = dtnasc.isAfter(refDate) ? -1 : Math.toIntExact(ChronoUnit.YEARS.between(dtnasc, refDate));
    ObjectNode facts = PraxisFactTreeBuilder.create()
        .putDraft(draft -> draft
            .put("dtnasc", dtnasc)
            .put("dtini", refDate)
            .put("parentesco", "FILHO")
            .put("referenceDate", refDate)
            .put("ageYears", age))
        .build();
    enforcePreviewBinding(orgao, defaultProductDependentePlan, "ERG-03182", facts);
    enforcePreviewBinding(orgao, defaultProductDependentePlan, "ERG-03181", facts);
  }

  @org.springframework.beans.factory.annotation.Autowired
  private ServidorPessoaAgeOptions servidorAgeOptions = new ServidorPessoaAgeOptions();

  public RuleDecisionPlan registerOrgaoCargoQuadroPlan(int orgao, RuleSetDefinition definition) {
    return registerPlan(cargoProduto.ref().ruleSetKey(), orgao, definition);
  }

  public RuleDecisionPlan registerOrgaoServidorPessoaCadastroPlan(int orgao, RuleSetDefinition definition) {
    return registerPlan(servidorProduto.ref().ruleSetKey(), orgao, definition);
  }

  public RuleEvaluationResult enforceCargoQuadro(Cargo cargo, Cargo parent,
      long ocupadas, long dependentes, boolean extinguindo) {
    Integer orgao = Objects.requireNonNull(cargo.getEmpCodigo());
    ObjectNode facts = PraxisFactTreeBuilder.create()
        .putDraft(draft -> draft
            .put("cargo", cargo.getCargo())
            .put("controle", cargo.getControleVaga())
            .put("autorizadas", cargo.getVagasAutorizadas())
            .put("aglutinador", cargo.getEAglutinador())
            .put("pai", cargo.getCargoAglutinador())
            .put("inicio", epochDay(cargo.getDtInicioContrVaga()))
            .put("extincao", epochDay(cargo.getDtExtincao()))
            .put("extinguindo", extinguindo)
            .put("documentoLegalId", cargo.getDocumentoLegalId()))
        .putConcept("cargoAglutinador", c -> c
            .put("aglutinador", parent == null ? null : parent.getEAglutinador()))
        .putNeighborhood(n -> n
            .put("ocupadas", ocupadas)
            .put("dependentes", dependentes))
        .build();
    return enforce(resolvePlan(cargoProduto.ref().ruleSetKey(), orgao), facts);
  }

  public RuleEvaluationResult enforceServidorPessoaCadastro(ServidorPessoa pessoa, int orgao,
      long outrosComCpf, LocalDate hoje) {
    LocalDate nascimento = pessoa.getDtnasc();
    LocalDate referencia = pessoa.getDtCertFim() == null ? hoje : pessoa.getDtCertFim();
    ObjectNode facts = PraxisFactTreeBuilder.create()
        .putDraft(draft -> draft
            .put("nascimento", epochDay(nascimento))
            .put("anosCompletosHoje", idadeOracle(nascimento, hoje, false))
            .put("anosArredondadosNaReferencia", idadeOracle(nascimento, referencia, true))
            .put("anoNascimento", nascimento == null ? null : nascimento.getYear())
            .put("anoPrimeiroEmprego", pessoa.getAnoprimemp())
            .put("expedicaoRg", epochDay(pessoa.getExpedrg()))
            .put("registroProfissional", blankToNull(pessoa.getIdentprof()))
            .put("tipoRegistro", blankToNull(pessoa.getTipoidprof()))
            .put("pispasep", blankToNull(pessoa.getPispasep())))
        .putNeighborhood(n -> n
            .put("outrosComCpf", outrosComCpf))
        .putContext(ctx -> ctx
            .put("hoje", hoje.toEpochDay())
            .put("anoAtual", hoje.getYear()))
        .build();
    return enforce(resolvePlan(servidorProduto.ref().ruleSetKey(), orgao), facts);
  }

  private static Integer idadeOracle(LocalDate nascimento, LocalDate referencia, boolean arredondarParaCima) {
    if (nascimento == null) return null;
    int anos = referencia.getYear() - nascimento.getYear();
    int comparacao = referencia.compareTo(aniversarioOracle(nascimento, anos));
    // ADD_MONTHS pode levar o aniversário ao último dia do mês de destino.
    return arredondarParaCima ? anos + (comparacao > 0 ? 1 : 0) : anos - (comparacao < 0 ? 1 : 0);
  }

  private static LocalDate aniversarioOracle(LocalDate nascimento, int anos) {
    if (nascimento == null) return null;
    LocalDate aniversario = nascimento.plusYears(anos);
    // ADD_MONTHS preserva o último dia do mês de origem, inclusive em fevereiro.
    return nascimento.getDayOfMonth() == nascimento.lengthOfMonth()
        ? aniversario.withDayOfMonth(aniversario.lengthOfMonth())
        : aniversario;
  }

  private static Long epochDay(LocalDate date) {
    return date == null ? null : date.toEpochDay();
  }

  public RuleDecisionPlan registerOrgaoFuncaoConfiancaPlan(int orgao, RuleSetDefinition definition) {
    return registerPlan(defaultProductFuncaoConfiancaPlan.ref().ruleSetKey(), orgao, definition);
  }

  public RuleDecisionPlan registerOrgaoVagaFuncaoPlan(int orgao, RuleSetDefinition definition) {
    return registerPlan(defaultProductVagaFuncaoPlan.ref().ruleSetKey(), orgao, definition);
  }

  public RuleDecisionPlan registerOrgaoDesignacaoFuncaoPlan(int orgao, RuleSetDefinition definition) {
    return registerPlan(defaultProductDesignacaoFuncaoPlan.ref().ruleSetKey(), orgao, definition);
  }

  public RuleDecisionPlan registerOrgaoSubstituicaoFuncaoPlan(int orgao, RuleSetDefinition definition) {
    return registerPlan(defaultProductSubstituicaoFuncaoPlan.ref().ruleSetKey(), orgao, definition);
  }

  public RuleEvaluationResult enforceFuncaoConfianca(FuncaoConfianca draft) {
    ObjectNode root = PraxisFactTreeBuilder.create()
        .putDraft(node -> node
            .put("inicio", epochDay(draft.getDtini()))
            .put("fim", epochDay(draft.getDtfim()))
            .put("situacao", draft.getSituacao())
            .put("permiteSubstituicao", draft.getPermiteSubstituicao())
            .put("minimo", draft.getDiasMinimosSubstituicao())
            .put("documentoLegalId", draft.getDocumentoLegalId())
            .put("atoPublicacaoId", draft.getAtoPublicacaoId()))
        .build();
    int orgao = draft.getEmpCodigo();
    return enforce(resolvePlan(defaultProductFuncaoConfiancaPlan.ref().ruleSetKey(), orgao), root);
  }

  public void enforceFuncaoPreviewDates(int orgao, LocalDate inicio, LocalDate fim) {
    enforceChefiaPreviewDates(orgao, inicio, fim, defaultProductFuncaoConfiancaPlan, "ERG-01101");
  }

  public void enforceSubstituicaoPreviewDates(int orgao, LocalDate inicio, LocalDate fim) {
    enforceChefiaPreviewDates(orgao, inicio, fim, defaultProductSubstituicaoFuncaoPlan, "ERG-01300");
  }

  public void enforceVagaFuncaoPreview(int orgao, String situacaoFuncao) {
    enforcePreviewBinding(orgao, defaultProductVagaFuncaoPlan, "ERG-01154",
        PraxisFactTreeBuilder.create()
            .putDraft(node -> node.put("situacao", "LIVRE"))
            .putConcept("funcao", node -> node.put("situacao", situacaoFuncao))
            .build());
  }

  public void enforceSubstituicaoFuncaoPermitePreview(int orgao, Boolean permiteSubstituicao) {
    enforcePreviewBinding(orgao, defaultProductSubstituicaoFuncaoPlan, "ERG-01315",
        PraxisFactTreeBuilder.create()
            .putConcept("funcao", node -> node.put("permiteSubstituicao", permiteSubstituicao))
            .build());
  }

  public void enforceSubstituicaoDesignacaoVigenciaPreview(
      int orgao, DesignacaoFuncao designacao, LocalDate inicio, LocalDate fim) {
    enforcePreviewBinding(orgao, defaultProductSubstituicaoFuncaoPlan, "ERG-01319",
        PraxisFactTreeBuilder.create()
            .putDraft(node -> node.put("inicio", epochDay(inicio)).put("fim", epochDay(fim)))
            .putConcept("designacao", node -> node
                .put("situacao", designacao == null ? null : designacao.getSituacao())
                .put("inicio", designacao == null ? null : epochDay(designacao.getDtExercicio()))
                .put("fim", designacao == null ? null : epochDay(designacao.getDtfim())))
            .build());
  }

  private void enforceChefiaPreviewDates(int orgao, LocalDate inicio, LocalDate fim,
      RuleSetDefinition product, String code) {
    enforcePreviewBinding(orgao, product, code,
        PraxisFactTreeBuilder.create()
            .putDraft(node -> node.put("inicio", epochDay(inicio)).put("fim", epochDay(fim)))
            .build());
  }

  private void enforcePreviewBinding(int orgao, RuleSetDefinition product, String code, ObjectNode facts) {
    var binding = product.bindings().stream().filter(b -> code.equals(b.falseReasonCode()))
        .findFirst().orElseThrow();
    enforcePreviewBinding(orgao, product, binding, facts);
  }

  private void enforcePreviewBinding(int orgao, RuleSetDefinition product, DecisionBinding binding,
      ObjectNode facts) {
    var activePlan = resolvePlan(product.ref().ruleSetKey(), orgao);
    var slot = activePlan.slotsByKey().get(binding.slotKey());
    if (slot == null) {
      throw new IllegalStateException("Slot ausente no plano ativo: " + binding.slotKey());
    }
    List<DecisionBinding> previewBindings = activePlan.definition().bindings().stream()
        .filter(candidate -> candidate.slotKey().equals(slot.slotKey())
            && candidate.enabled()
            && (slot.cardinality() == org.praxisplatform.rules.contract.SlotCardinality.SINGLE
                || candidate.bindingKey().equals(binding.bindingKey())
                || Objects.equals(candidate.falseReasonCode(), binding.falseReasonCode())))
        .map(candidate -> candidate.dependsOn().isEmpty()
            ? candidate
            : new DecisionBinding(
                candidate.bindingKey(), candidate.slotKey(), candidate.source(),
                candidate.compositionPolicy(), candidate.executor(), List.of(),
                candidate.order(), candidate.enabled(), candidate.falseDecision(),
                candidate.falseReasonCode(), candidate.requiredFactPaths(),
                candidate.parameterization()))
        .toList();
    if (previewBindings.isEmpty()) {
      return;
    }
    var ref = activePlan.definition().ref();
    var preview = new RuleSetDefinition(
        new RuleSetRef(ref.domainKey(), ref.boundedContextKey(), ref.ruleSetKey(),
            "preview-" + binding.bindingKey(), ref.version()),
        activePlan.definition().availableRoots(), List.of(slot), previewBindings,
        activePlan.definition().compatibility(), activePlan.definition().failPolicy());
    var previewPlan = template.compile(preview, "preview:" + binding.bindingKey() + ":orgao:" + orgao);
    rememberErrors(previewPlan, localErrors(activePlan));
    enforce(previewPlan, facts);
  }

  public RuleEvaluationResult enforceVagaFuncao(VagaFuncao draft, VagaFuncao previous, FuncaoConfianca funcao) {
    ObjectNode root = PraxisFactTreeBuilder.create()
        .putDraft(node -> node
            .put("inicio", epochDay(draft.getDtini()))
            .put("fim", epochDay(draft.getDtfim()))
            .put("situacao", draft.getSituacao())
            .put("funcao", draft.getFuncaoId())
            .put("documentoLegalId", draft.getDocumentoLegalId()))
        .putCurrent(node -> node
            .put("criacao", previous == null)
            .put("titular", previous == null || previous.getTitularAtualId() == null
                ? null : previous.getTitularAtualId().numfunc())
            .put("funcao", previous == null ? null : previous.getFuncaoId()))
        .putConcept("funcao", node -> node
            .put("situacao", funcao == null ? null : funcao.getSituacao()))
        .build();
    int orgao = draft.getEmpCodigo();
    return enforce(resolvePlan(defaultProductVagaFuncaoPlan.ref().ruleSetKey(), orgao), root);
  }

  public RuleEvaluationResult enforceDesignacaoFuncao(DesignacaoFuncao draft) {
    return enforceDesignacaoFuncao(draft, null, null, null, 0);
  }

  public RuleEvaluationResult enforceDesignacaoFuncao(DesignacaoFuncao draft, String estadoAnterior,
      String estadoVaga, Long funcaoDaVaga, long outrosTitulares) {
    return enforceDesignacaoFuncao(
        draft.getEmpCodigo(),
        DesignacaoFuncaoFacts.of(draft, estadoAnterior, estadoVaga, funcaoDaVaga, outrosTitulares));
  }

  public RuleEvaluationResult enforceDesignacaoFuncao(Integer empCodigo, ObjectNode facts) {
    RuleDecisionPlan plan = resolvePlan(defaultProductDesignacaoFuncaoPlan.ref().ruleSetKey(), empCodigo);
    return enforce(plan, facts);
  }

  public RuleEvaluationResult enforceSubstituicaoFuncao(Integer empCodigo, ObjectNode facts) {
    RuleDecisionPlan plan = resolvePlan(defaultProductSubstituicaoFuncaoPlan.ref().ruleSetKey(), empCodigo);
    return enforce(plan, facts);
  }

  /** Avalia a {@code RN-013} sobre um rascunho de {@link RegraFrequenciaEstatutaria} e seus registros existentes. */
  public RuleEvaluationResult evaluateRn013(
      RegraFrequenciaEstatutaria draft, List<RegraFrequenciaEstatutaria> existingOverlaps) {
    Integer empCodigo = draft != null && draft.getEmpCodigo() != null ? draft.getEmpCodigo() : 1;
    RuleDecisionPlan plan = resolveRn013Plan(empCodigo);
    ObjectNode facts = buildRn013Facts(draft, existingOverlaps, empCodigo);
    return evaluate(plan, facts);
  }

  /**
   * Avalia a {@code RN-013} e lança {@link ErgonRuleViolationException} (HTTP 422) se o resultado
   * não for {@link RuleDecision#ALLOW}.
   */
  public RuleEvaluationResult enforceRn013(
      RegraFrequenciaEstatutaria draft, List<RegraFrequenciaEstatutaria> existingOverlaps) {
    Integer empCodigo = draft != null && draft.getEmpCodigo() != null ? draft.getEmpCodigo() : 1;
    RuleDecisionPlan plan = resolveRn013Plan(empCodigo);
    ObjectNode facts = buildRn013Facts(draft, existingOverlaps, empCodigo);
    return enforce(plan, facts);
  }

  /** Avalia deterministicamente o lançamento/manutenção de {@link Frequencia} contra o plano do órgão. */
  public RuleEvaluationResult evaluateFrequencia(FrequenciaRuleContext ctx) {
    Objects.requireNonNull(ctx, "ctx must not be null");
    Integer empCodigo =
        ctx.draft() != null && ctx.draft().getEmpCodigo() != null ? ctx.draft().getEmpCodigo() : 1;
    RuleDecisionPlan plan = resolveFrequenciaPlan(empCodigo);
    ObjectNode facts = buildFrequenciaFacts(ctx);
    return evaluate(plan, facts);
  }

  /**
   * Avalia o lançamento/manutenção de {@link Frequencia} em modo <i>fail-closed</i>, lançando
   * {@link ErgonRuleViolationException} (HTTP 422) se o resultado não for {@link RuleDecision#ALLOW}.
   */
  public RuleEvaluationResult enforceFrequencia(FrequenciaRuleContext ctx) {
    Objects.requireNonNull(ctx, "ctx must not be null");
    Integer empCodigo =
        ctx.draft() != null && ctx.draft().getEmpCodigo() != null ? ctx.draft().getEmpCodigo() : 1;
    RuleDecisionPlan plan = resolveFrequenciaPlan(empCodigo);
    ObjectNode facts = buildFrequenciaFacts(ctx);
    return enforce(plan, facts);
  }

  /** Avalia deterministicamente a concessão/manutenção de {@link Afastamento} contra o plano do órgão. */
  public RuleEvaluationResult evaluateAfastamento(AfastamentoRuleContext ctx) {
    Objects.requireNonNull(ctx, "ctx must not be null");
    Integer empCodigo =
        ctx.draft() != null && ctx.draft().getEmpCodigo() != null ? ctx.draft().getEmpCodigo() : 1;
    RuleDecisionPlan plan = resolveAfastamentoPlan(empCodigo);
    ObjectNode facts = buildAfastamentoFacts(ctx);
    return evaluate(plan, facts);
  }

  /**
   * Avalia a concessão/manutenção de {@link Afastamento} em modo <i>fail-closed</i>, lançando
   * {@link ErgonRuleViolationException} (HTTP 422) se o resultado não for {@link RuleDecision#ALLOW}.
   */
  public RuleEvaluationResult enforceAfastamento(AfastamentoRuleContext ctx) {
    Objects.requireNonNull(ctx, "ctx must not be null");
    Integer empCodigo =
        ctx.draft() != null && ctx.draft().getEmpCodigo() != null ? ctx.draft().getEmpCodigo() : 1;
    RuleDecisionPlan plan = resolveAfastamentoPlan(empCodigo);
    ObjectNode facts = buildAfastamentoFacts(ctx);
    return enforce(plan, facts);
  }

  /** Avalia deterministicamente a elegibilidade de {@link Dependente} contra o plano do órgão. */
  public RuleEvaluationResult evaluateDependente(DependenteRuleContext ctx) {
    Objects.requireNonNull(ctx, "ctx must not be null");
    Integer empCodigo = resolveDependenteEmpCodigo(ctx);
    RuleDecisionPlan plan = resolveDependentePlan(empCodigo);
    ObjectNode facts = buildDependenteFacts(ctx);
    return evaluate(plan, facts);
  }

  /**
   * Avalia a elegibilidade de {@link Dependente} em modo <i>fail-closed</i>, lançando
   * {@link ErgonRuleViolationException} (HTTP 422) se o resultado não for {@link RuleDecision#ALLOW}.
   */
  public RuleEvaluationResult enforceDependente(DependenteRuleContext ctx) {
    Objects.requireNonNull(ctx, "ctx must not be null");
    Integer empCodigo = resolveDependenteEmpCodigo(ctx);
    RuleDecisionPlan plan = resolveDependentePlan(empCodigo);
    ObjectNode facts = buildDependenteFacts(ctx);
    return enforce(plan, facts);
  }

  private static Integer resolveDependenteEmpCodigo(DependenteRuleContext ctx) {
    if (ctx.empCodigo() != null) {
      return ctx.empCodigo();
    }
    if (ctx.draft() != null
        && ctx.draft().getCustomFields() != null
        && ctx.draft().getCustomFields().get("empCodigo") instanceof Number n) {
      return n.intValue();
    }
    if (ctx.documentoLegal() != null && ctx.documentoLegal().getEmpCodigo() != null) {
      return ctx.documentoLegal().getEmpCodigo();
    }
    return 1;
  }

  /** Avalia deterministicamente o provimento/manutenção de {@link Vinculo} contra o plano do órgão. */
  public RuleEvaluationResult evaluateVinculo(VinculoRuleContext ctx) {
    Objects.requireNonNull(ctx, "ctx must not be null");
    Integer empCodigo =
        ctx.draft() != null && ctx.draft().getEmpCodigo() != null ? ctx.draft().getEmpCodigo() : 1;
    RuleDecisionPlan plan = resolveVinculoPlan(empCodigo);
    ObjectNode facts = buildVinculoFacts(ctx);
    return evaluate(plan, facts);
  }

  /**
   * Avalia o provimento/manutenção de {@link Vinculo} em modo <i>fail-closed</i>, lançando
   * {@link ErgonRuleViolationException} (HTTP 422) se o resultado não for {@link RuleDecision#ALLOW}.
   */
  public RuleEvaluationResult enforceVinculo(VinculoRuleContext ctx) {
    Objects.requireNonNull(ctx, "ctx must not be null");
    Integer empCodigo =
        ctx.draft() != null && ctx.draft().getEmpCodigo() != null ? ctx.draft().getEmpCodigo() : 1;
    RuleDecisionPlan plan = resolveVinculoPlan(empCodigo);
    ObjectNode facts = buildVinculoFacts(ctx);
    return enforce(plan, facts);
  }

  /**
   * Valida sintática e estruturalmente {@code DefinicaoCampoCustomizado.validationRules}, incluindo
   * limites escalares e qualquer árvore JSON Logic via {@link PraxisJsonLogicEngine#validate}.
   * Rejeita expressões inválidas com HTTP 422 ({@link ErgonRuleViolationException} {@code ERG-21501}).
   */
  public void validateCustomFieldValidationRules(Map<String, Object> validationRules) {
    if (validationRules == null || validationRules.isEmpty()) {
      return;
    }
    if (validationRules.get("min") instanceof Number minN
        && validationRules.get("max") instanceof Number maxN
        && minN.doubleValue() > maxN.doubleValue()) {
      throw new ErgonRuleViolationException(
          ErgonRuleErrorCatalog.ERG_21501.code(),
          ErgonRuleErrorCatalog.ERG_21501.slotKey(),
          "product.ergon.customField.syntax",
          DecisionSource.PRODUCT,
          DecisionStage.PROTECTED_GUARD,
          "validationRules",
          "validationRules inválido: min não pode ser maior que max.");
    }
    if (validationRules.get("minLength") instanceof Number minL
        && validationRules.get("maxLength") instanceof Number maxL
        && minL.intValue() > maxL.intValue()) {
      throw new ErgonRuleViolationException(
          ErgonRuleErrorCatalog.ERG_21501.code(),
          ErgonRuleErrorCatalog.ERG_21501.slotKey(),
          "product.ergon.customField.syntax",
          DecisionSource.PRODUCT,
          DecisionStage.PROTECTED_GUARD,
          "validationRules",
          "validationRules inválido: minLength não pode ser maior que maxLength.");
    }
    Object patternObj = validationRules.get("pattern");
    if (patternObj != null) {
      try {
        Pattern.compile(String.valueOf(patternObj));
      } catch (PatternSyntaxException ex) {
        throw new ErgonRuleViolationException(
            ErgonRuleErrorCatalog.ERG_21501.code(),
            ErgonRuleErrorCatalog.ERG_21501.slotKey(),
            "product.ergon.customField.syntax",
            DecisionSource.PRODUCT,
            DecisionStage.PROTECTED_GUARD,
            "validationRules",
            "Expressão regular inválida em validationRules.pattern.");
      }
    }

    try {
      org.praxisplatform.uischema.customfields.CustomFieldValidationRules.validate(
          br.com.praxis.ergonshowcase.config.CamposCustomizadosProvider.jsonLogicRules(validationRules));
    } catch (PraxisJsonLogicException | IllegalArgumentException ex) {
      throw new ErgonRuleViolationException(
          ErgonRuleErrorCatalog.ERG_21501.code(),
          ErgonRuleErrorCatalog.ERG_21501.slotKey(),
          "product.ergon.customField.syntax",
          DecisionSource.PRODUCT,
          DecisionStage.PROTECTED_GUARD,
          "validationRules",
          ErgonRuleErrorCatalog.ERG_21501.messagePtBr() + " Detalhe: " + ex.getMessage());
    }
  }

  /**
   * Avalia em runtime as expressões JSON Logic definidas nas {@link DefinicaoCampoCustomizado}
   * ativas sobre o contexto determinístico {@code {"value": <valorDoCampo>, "customFields": <mapa>, "draft": <entidade>}},
   * lançando {@link ErgonRuleViolationException} (HTTP 422 {@code ERG-21502}) quando uma regra retornar {@code false}.
   */
  public void evaluateCustomFieldsJsonLogic(
      String resourceKey,
      Integer empCodigo,
      Map<String, Object> customFields,
      Object draftEntity,
      List<DefinicaoCampoCustomizado> activeDefinitions) {
    if (activeDefinitions == null || activeDefinitions.isEmpty()) return;
    Map<String,Object> values = customFields == null ? Map.of() : customFields;
    Map<String,DefinicaoCampoCustomizado> effective = new LinkedHashMap<>();
    for (var definition : activeDefinitions) {
      if (definition == null || Boolean.FALSE.equals(definition.getActive()) || definition.getFieldKey() == null
          || definition.getEmpCodigo() != null && empCodigo != null && !Objects.equals(definition.getEmpCodigo(),empCodigo)) continue;
      if (!effective.containsKey(definition.getFieldKey()) || definition.getEmpCodigo() != null) effective.put(definition.getFieldKey(),definition);
    }
    var descriptors = new ArrayList<org.praxisplatform.uischema.customfields.CustomFieldDescriptor>();
    for (var definition : effective.values()) {
      var rules = br.com.praxis.ergonshowcase.config.CamposCustomizadosProvider.jsonLogicRules(definition.getValidationRules());
      String type = definition.getFieldType();
      var dataType = org.praxisplatform.uischema.customfields.CustomFieldDataType.valueOf(
          type == null || "SELECT".equals(type) ? "STRING" : "NUMBER".equals(type) ? "DECIMAL" : type);
      descriptors.add(new org.praxisplatform.uischema.customfields.CustomFieldDescriptor(
          definition.getFieldKey(), definition.getLabel(), definition.getHelpText(), dataType, null, null, 0,
          Boolean.TRUE.equals(definition.getRequired()), false, false, false, false, null, null, null, null, null, null, null, null,
          List.of(), List.of(), null, null, Map.of(), rules));
    }
    try {
      new org.praxisplatform.uischema.customfields.CustomFieldValidator((key,tenant,schema) -> descriptors)
          .validateValues(values, descriptors, toSafeEntityNode(draftEntity), toSafeEntityNode(draftEntity));
    } catch (org.praxisplatform.uischema.customfields.CustomFieldValidationException failure) {
      var error = failure.getFieldErrors().getFirst();
      String key = error.getField().replace("customFields.", "");
      throw new ErgonRuleViolationException(ErgonRuleErrorCatalog.ERG_21502.code(), ErgonRuleErrorCatalog.ERG_21502.slotKey(),
          "product.ergon.customField." + key, DecisionSource.PRODUCT, DecisionStage.DOMAIN_DECISION, error.getField(),
          error.getDefaultMessage() == null ? ErgonRuleErrorCatalog.ERG_21502.messagePtBr() : error.getDefaultMessage());
    }
  }

  /**
   * Resolve deterministicamente a {@link RegraFrequenciaEstatutaria} vigente mais específica para o
   * escopo funcional do {@link Vinculo} ({@code empCodigo}, {@code regimejur}, {@code tipovinc},
   * {@code categoria}, {@code subcategoria}, {@code tipofreq}, {@code codfreq}) na data do evento,
   * espelhando {@code PCK_FREQUENCIAS.valid_regras_freq} e {@code PCK_LIC_AFAST.valid_reg_licafast}.
   */
  public Optional<RegraFrequenciaEstatutaria> resolveEffectiveStatutoryRule(
      List<RegraFrequenciaEstatutaria> candidates,
      Vinculo vinculo,
      String tipofreq,
      Integer codfreq,
      LocalDate eventDate) {
    if (candidates == null || candidates.isEmpty() || tipofreq == null || codfreq == null) {
      return Optional.empty();
    }
    return candidates.stream()
        .filter(Objects::nonNull)
        .filter(r -> r.getAtiva() == null || Boolean.TRUE.equals(r.getAtiva()))
        .filter(
            r ->
                Objects.equals(r.getTipofreq(), tipofreq)
                    && Objects.equals(r.getCodfreq(), codfreq))
        .filter(r -> matchesScopeDimension(r.getEmpCodigo(), vinculo != null ? vinculo.getEmpCodigo() : null))
        .filter(r -> matchesScopeDimension(r.getRegimeJuridico(), vinculo != null ? vinculo.getRegimejur() : null))
        .filter(r -> matchesScopeDimension(r.getTipoVinculo(), vinculo != null ? vinculo.getTipovinc() : null))
        .filter(r -> matchesScopeDimension(r.getCategoria(), vinculo != null ? vinculo.getCategoria() : null))
        .filter(r -> matchesScopeDimension(r.getSubcategoria(), vinculo != null ? vinculo.getSubcategoria() : null))
        .filter(
            r ->
                eventDate == null
                    || ((r.getDtini() == null || !eventDate.isBefore(r.getDtini()))
                        && (r.getDtfim() == null || !eventDate.isAfter(r.getDtfim()))))
        .max(
            Comparator.comparingInt(ErgonRuleEngineFacade::specificityScore)
                .thenComparing(
                    r -> r.getDtini() != null ? r.getDtini() : LocalDate.MIN));
  }

  private static <T> boolean matchesScopeDimension(T ruleValue, T vinculoValue) {
    return ruleValue == null || Objects.equals(ruleValue, vinculoValue);
  }

  private static int specificityScore(RegraFrequenciaEstatutaria r) {
    int score = 0;
    if (r.getEmpCodigo() != null) score += 16;
    if (r.getRegimeJuridico() != null) score += 8;
    if (r.getTipoVinculo() != null) score += 4;
    if (r.getCategoria() != null) score += 2;
    if (r.getSubcategoria() != null) score += 1;
    return score;
  }

  /**
   * Constrói um payload estruturado de diagnóstico e auditoria para uma avaliação determinística,
   * preservando rastreabilidade de slot, binding e origem ({@code PRODUCT} vs {@code CUSTOMER}).
   */
  public Map<String, Object> toDiagnosticPayload(
      RuleDecisionPlan plan, RuleEvaluationResult result) {
    Objects.requireNonNull(result, "result must not be null");
    List<org.praxisplatform.rules.violation.SlotViolationDetail> violations =
        ErgonRuleViolationException.buildViolations(plan, result);
    Map<String, Object> payload = new java.util.LinkedHashMap<>(template.toDiagnosticPayload(plan, result));
    if (result.ruleSetRef() != null) {
      payload.put("ruleSetKey", result.ruleSetRef().ruleSetKey());
      payload.put("resourceKey", result.ruleSetRef().boundedContextKey());
      payload.put("domainScope", result.ruleSetRef().domainKey());
      payload.put("version", result.ruleSetRef().version());
    }
    payload.put("planDigest", result.planDigest());
    payload.put("factsDigest", result.factsDigest());
    payload.put("decision", result.decision().name());
    payload.put("allowed", result.decision() == RuleDecision.ALLOW);
    payload.put("reasonCodes", result.reasonCodes());
    payload.put(
        "slotKeys",
        violations.stream()
            .map(org.praxisplatform.rules.violation.SlotViolationDetail::slotKey)
            .distinct()
            .toList());
    payload.put(
        "decisionSources",
        violations.stream()
            .filter(v -> v.source() != null)
            .map(v -> v.source().name())
            .distinct()
            .toList());
    payload.put("violations", violations);
    payload.put("bindingResults", result.bindingResults());
    return payload;
  }

  /**
   * Retorna o resumo de governança do {@link RuleDecisionPlan} compilado da {@code RN-013} para o
   * órgão informado.
   */
  public Map<String, Object> toPlanSummaryPayload(Integer empCodigo) {
    int resolvedOrgao = empCodigo != null ? empCodigo : 1;
    return buildPlanSummaryPayload(resolvedOrgao, resolveRn013Plan(resolvedOrgao));
  }

  /** Retorna o resumo de governança de um {@link RuleDecisionPlan} arbitrário para o órgão informado. */
  public Map<String, Object> buildPlanSummaryPayload(int resolvedOrgao, RuleDecisionPlan plan) {
    Map<String, Object> payload = new java.util.LinkedHashMap<>();
    payload.put("empCodigo", resolvedOrgao);
    payload.put("ruleSet", plan.definition().ref());
    payload.put("planDigest", plan.planDigest());
    payload.put("failPolicy", plan.definition().failPolicy().name());
    payload.put("slotCount", plan.slotsByKey().size());
    payload.put("slots", plan.slotsByKey().values());
    payload.put(
        "activeBindings",
        plan.orderedBindings().stream()
            .map(
                b ->
                    Map.of(
                        "bindingKey", b.bindingKey(),
                        "slotKey", b.slotKey(),
                        "source", b.source().name(),
                        "compositionPolicy",
                            b.compositionPolicy() != null
                                ? b.compositionPolicy().name()
                                : "DEFAULT",
                        "order", b.order()))
            .toList());
    payload.put("errorCatalog", ErgonRuleErrorCatalog.all());
    return payload;
  }

  /**
   * Constrói a árvore JSON imutável de fatos da {@code RN-013} ({@code draft},
   * {@code existingOverlaps}, {@code tenant}, {@code context}) sem {@code POJONode}s.
   */
  public ObjectNode buildRn013Facts(
      RegraFrequenciaEstatutaria draft,
      List<RegraFrequenciaEstatutaria> existingOverlaps,
      Integer empCodigo) {
    return Rn013RegrasFrequenciaFacts.of(mapper, draft, existingOverlaps, empCodigo);
  }

  /**
   * Constrói a árvore JSON imutável de fatos para {@link FrequenciaLancamentoRuleSet}, incluindo a
   * ponte dinâmica {@code effectiveStatutoryRule} derivada de {@link RegraFrequenciaEstatutaria}.
   */
  public ObjectNode buildFrequenciaFacts(FrequenciaRuleContext ctx) {
    return FrequenciaAfastamentoFacts.ofFrequencia(mapper, ctx);
  }

  /**
   * Constrói a árvore JSON imutável de fatos para {@link AfastamentoConcessaoRuleSet}, incluindo a
   * ponte dinâmica {@code effectiveStatutoryRule} derivada de {@link RegraFrequenciaEstatutaria}.
   */
  public ObjectNode buildAfastamentoFacts(AfastamentoRuleContext ctx) {
    return FrequenciaAfastamentoFacts.ofAfastamento(mapper, ctx);
  }

  /**
   * Constrói a árvore JSON imutável de fatos para {@link DependenteElegibilidadeRuleSet}.
   */
  public ObjectNode buildDependenteFacts(DependenteRuleContext ctx) {
    Objects.requireNonNull(ctx, "ctx must not be null");
    return DependenteElegibilidadeFacts.of(mapper, ctx, resolveDependenteEmpCodigo(ctx));
  }

  /**
   * Constrói a árvore JSON imutável de fatos para {@link VinculoProvimentoRuleSet}.
   */
  public ObjectNode buildVinculoFacts(VinculoRuleContext ctx) {
    return VinculoProvimentoFacts.of(mapper, ctx);
  }





  private ObjectNode toSafeEntityNode(Object draftEntity) {
    ObjectNode node = mapper.createObjectNode();
    if (draftEntity == null) {
      return node;
    }
    if (draftEntity instanceof Dependente d) {
      Long numfunc = d.getId() != null ? d.getId().numfunc() : null;
      Integer numdep = d.getId() != null ? d.getId().numdep() : null;
      putIntOrNull(node, "id", numdep);
      putLongOrNull(node, "numfunc", numfunc);
      putIntOrNull(node, "numero", numdep);
      putIntOrNull(node, "numdep", numdep);
      putTextOrNull(node, "nome", d.getNome());
      putDateOrNull(node, "dtnasc", d.getDtnasc());
      putTextOrNull(node, "sexo", d.getSexo());
      putTextOrNull(node, "cpf", d.getCpf());
      putTextOrNull(node, "parentesco", d.getParentesco());
      putTextOrNull(node, "tipodepen", d.getTipodepen());
      putDateOrNull(node, "dtini", d.getDtini());
      putDateOrNull(node, "dtfim", d.getDtfim());
      putBooleanOrNull(node, "depIr", d.getDepIr());
      putBooleanOrNull(node, "depSalarioFamilia", d.getDepSalarioFamilia());
      putBooleanOrNull(node, "depPlanoSaude", d.getDepPlanoSaude());
      putBooleanOrNull(node, "estudante", d.getEstudante());
      putBooleanOrNull(node, "universitario", d.getUniversitario());
      putBooleanOrNull(node, "invalido", d.getInvalido());
      putBooleanOrNull(node, "excepcional", d.getExcepcional());
      putBooleanOrNull(node, "representanteLegal", d.getRepresentanteLegal());
      putLongOrNull(node, "documentoLegalId", d.getDocumentoLegalId());
      return node;
    }
    if (draftEntity instanceof Vinculo v) {
      Long numfunc = v.getId() != null ? v.getId().numfunc() : null;
      Integer numero = v.getId() != null ? v.getId().numero() : null;
      putIntOrNull(node, "id", numero);
      putIntOrNull(node, "empCodigo", v.getEmpCodigo());
      putLongOrNull(node, "numfunc", numfunc);
      putIntOrNull(node, "numero", numero);
      putTextOrNull(node, "regimejur", v.getRegimejur());
      putTextOrNull(node, "tipovinc", v.getTipovinc());
      putTextOrNull(node, "categoria", v.getCategoria());
      putTextOrNull(node, "subcategoria", v.getSubcategoria());
      putTextOrNull(node, "situacao", v.getSituacao());
      putDateOrNull(node, "dtnom", v.getDtnom());
      putDateOrNull(node, "dtposse", v.getDtposse());
      putDateOrNull(node, "dtexerc", v.getDtexerc());
      putDateOrNull(node, "dtvac", v.getDtvac());
      putTextOrNull(node, "formavac", v.getFormavac());
      putDateOrNull(node, "dtaposent", v.getDtaposent());
      putIntOrNull(node, "jornadaSemanal", v.getJornadaSemanal());
      putIntOrNull(node, "cargo", v.getCargo());
      putIntOrNull(node, "cargoComissao", v.getCargoComissao());
      putTextOrNull(node, "setor", v.getSetor());
      putTextOrNull(node, "setorExercicio", v.getSetorExercicio());
      putLongOrNull(node, "documentoLegalId", v.getDocumentoLegalId());
      return node;
    }
    if (draftEntity instanceof Frequencia f) {
      putLongOrNull(node, "id", f.getId());
      putIntOrNull(node, "empCodigo", f.getEmpCodigo());
      putLongOrNull(node, "numfunc", f.getNumfunc());
      putIntOrNull(node, "numvinc", f.getNumvinc());
      putTextOrNull(node, "tipofreq", f.getTipofreq());
      putIntOrNull(node, "codfreq", f.getCodfreq());
      putDateOrNull(node, "dtini", f.getDtini());
      putDateOrNull(node, "dtfim", f.getDtfim());
      if (f.getQuantidade() != null) {
        node.put("quantidade", f.getQuantidade().stripTrailingZeros());
      } else {
        node.putNull("quantidade");
      }
      putTextOrNull(node, "situacao", f.getSituacao());
      putLongOrNull(node, "documentoLegalId", f.getDocumentoLegalId());
      return node;
    }
    if (draftEntity instanceof Afastamento a) {
      putLongOrNull(node, "id", a.getId());
      putIntOrNull(node, "empCodigo", a.getEmpCodigo());
      putLongOrNull(node, "numfunc", a.getNumfunc());
      putIntOrNull(node, "numvinc", a.getNumvinc());
      putTextOrNull(node, "tipofreq", a.getTipofreq());
      putIntOrNull(node, "codfreq", a.getCodfreq());
      putDateOrNull(node, "dtini", a.getDtini());
      putDateOrNull(node, "dtprevfim", a.getDtprevfim());
      putDateOrNull(node, "dtfim", a.getDtfim());
      putIntOrNull(node, "quantidadeDias", a.getQuantidadeDias());
      putBooleanOrNull(node, "prorrogacao", a.getProrrogacao());
      putTextOrNull(node, "situacao", a.getSituacao());
      putLongOrNull(node, "documentoLegalId", a.getDocumentoLegalId());
      return node;
    }
    if (draftEntity instanceof Cargo c) {
      putIntOrNull(node, "id", c.getCargo());
      putIntOrNull(node, "empCodigo", c.getEmpCodigo());
      putIntOrNull(node, "cargo", c.getCargo());
      putTextOrNull(node, "nome", c.getNome());
      putTextOrNull(node, "categoria", c.getCategoria());
      putTextOrNull(node, "tipoCargo", c.getTipoCargo());
      putBooleanOrNull(node, "controleVaga", c.getControleVaga());
      putIntOrNull(node, "vagasAutorizadas", c.getVagasAutorizadas());
      putIntOrNull(node, "qtdVagasAutorizadas", c.getVagasAutorizadas());
      putDateOrNull(node, "dtExtincao", c.getDtExtincao());
      return node;
    }
    if (draftEntity instanceof ServidorPessoa p) {
      putLongOrNull(node, "id", p.getNumero());
      putLongOrNull(node, "numero", p.getNumero());
      putTextOrNull(node, "nome", p.getNome());
      putTextOrNull(node, "cpf", p.getCpf());
      putDateOrNull(node, "dtnasc", p.getDtnasc());
      putTextOrNull(node, "sexo", p.getSexo());
      return node;
    }
    if (draftEntity instanceof ParametroContagemFerias pcf) {
      putLongOrNull(node, "id", pcf.getId());
      putIntOrNull(node, "empCodigo", pcf.getEmpCodigo());
      putTextOrNull(node, "regimeJuridico", pcf.getRegimeJuridico());
      putTextOrNull(node, "tipoVinculo", pcf.getTipoVinculo());
      putTextOrNull(node, "categoria", pcf.getCategoria());
      putTextOrNull(node, "subcategoria", pcf.getSubcategoria());
      putDateOrNull(node, "dtini", pcf.getDtini());
      putDateOrNull(node, "dtfim", pcf.getDtfim());
      putBooleanOrNull(node, "temFerias", pcf.getTemFerias());
      putIntOrNull(node, "totDiasFer", pcf.getTotDiasFer());
      putIntOrNull(node, "minPerFer", pcf.getMinPerFer());
      putIntOrNull(node, "maxPerFer", pcf.getMaxPerFer());
      putIntOrNull(node, "minDiasPerFer", pcf.getMinDiasPerFer());
      putIntOrNull(node, "maxDiasPerFer", pcf.getMaxDiasPerFer());
      putIntOrNull(node, "minDiasPerPrincFer", pcf.getMinDiasPerPrincFer());
      putTextOrNull(node, "abonoDirGozo", pcf.getAbonoDirGozo());
      putBooleanOrNull(node, "contDobroFerias", pcf.getContDobroFerias());
      putLongOrNull(node, "documentoLegalFeriasId", pcf.getDocumentoLegalFeriasId());
      return node;
    }
    if (draftEntity instanceof PeriodoAquisitivoFerias paf) {
      putLongOrNull(node, "id", paf.getId());
      putIntOrNull(node, "empCodigo", paf.getEmpCodigo());
      putLongOrNull(node, "numfunc", paf.getNumfunc());
      putIntOrNull(node, "numvinc", paf.getNumvinc());
      putIntOrNull(node, "chave", paf.getChave());
      putIntOrNull(node, "exercicio", paf.getExercicio());
      putDateOrNull(node, "dtini", paf.getDtini());
      putDateOrNull(node, "dtfim", paf.getDtfim());
      putIntOrNull(node, "totDiasDireito", paf.getTotDiasDireito());
      putIntOrNull(node, "faltasApuradas", paf.getFaltasApuradas());
      putIntOrNull(node, "diasAfastamentoSuspensivo", paf.getDiasAfastamentoSuspensivo());
      putIntOrNull(node, "diasGozados", paf.getDiasGozados());
      putIntOrNull(node, "diasVendidosAbono", paf.getDiasVendidosAbono());
      putIntOrNull(node, "diasConvertidosDobro", paf.getDiasConvertidosDobro());
      putIntOrNull(node, "saldoDiasDisponivel", paf.getSaldoDiasDisponivel());
      putTextOrNull(node, "situacao", paf.getSituacao());
      putDateOrNull(node, "dtPrescricao", paf.getDtPrescricao());
      putLongOrNull(node, "documentoLegalId", paf.getDocumentoLegalId());
      return node;
    }
    if (draftEntity instanceof FeriasGozo fg) {
      putLongOrNull(node, "id", fg.getId());
      putIntOrNull(node, "empCodigo", fg.getEmpCodigo());
      putLongOrNull(node, "numfunc", fg.getNumfunc());
      putIntOrNull(node, "numvinc", fg.getNumvinc());
      putLongOrNull(node, "periodoAquisitivoId", fg.getPeriodoAquisitivoId());
      putIntOrNull(node, "chavePeraqfer", fg.getChavePeraqfer());
      putIntOrNull(node, "ordemParcela", fg.getOrdemParcela());
      putDateOrNull(node, "dtini", fg.getDtini());
      putDateOrNull(node, "dtprevfim", fg.getDtprevfim());
      putDateOrNull(node, "dtfim", fg.getDtfim());
      putIntOrNull(node, "diasGozo", fg.getDiasGozo());
      putIntOrNull(node, "totDiasDireitoPa", fg.getTotDiasDireitoPa());
      putBooleanOrNull(node, "abonoPecuniario", fg.getAbonoPecuniario());
      putTextOrNull(node, "iniFimAbono", fg.getIniFimAbono());
      putIntOrNull(node, "diasAbonoVendidos", fg.getDiasAbonoVendidos());
      putBooleanOrNull(node, "abonoConstitucional", fg.getAbonoConstitucional());
      putBooleanOrNull(node, "adiantamento13", fg.getAdiantamento13());
      putBooleanOrNull(node, "adiantamentoSalario", fg.getAdiantamentoSalario());
      putBooleanOrNull(node, "emprestimoFerias", fg.getEmprestimoFerias());
      putTextOrNull(node, "situacao", fg.getSituacao());
      putLongOrNull(node, "documentoLegalId", fg.getDocumentoLegalId());
      return node;
    }
    if (draftEntity instanceof Map<?, ?> map) {
      for (Map.Entry<?, ?> entry : map.entrySet()) {
        if (entry.getKey() != null) {
          node.set(String.valueOf(entry.getKey()), toSafeJsonValue(entry.getValue()));
        }
      }
      return node;
    }
    return node;
  }

  private JsonNode toSafeJsonValue(Object value) {
    if (value == null) {
      return mapper.nullNode();
    }
    if (value instanceof JsonNode jn) {
      return jn;
    }
    if (value instanceof String s) {
      return mapper.getNodeFactory().textNode(s);
    }
    if (value instanceof Boolean b) {
      return mapper.getNodeFactory().booleanNode(b);
    }
    if (value instanceof Integer i) {
      return mapper.getNodeFactory().numberNode(i);
    }
    if (value instanceof Long l) {
      return mapper.getNodeFactory().numberNode(l);
    }
    if (value instanceof BigDecimal bd) {
      return mapper.getNodeFactory().numberNode(bd.stripTrailingZeros());
    }
    if (value instanceof Number n) {
      return mapper.getNodeFactory().numberNode(new BigDecimal(n.toString()).stripTrailingZeros());
    }
    if (value instanceof LocalDate
        || value instanceof java.time.LocalTime
        || value instanceof java.time.LocalDateTime
        || value instanceof Instant) {
      return mapper.getNodeFactory().textNode(value.toString());
    }
    if (value instanceof Map<?, ?> map) {
      ObjectNode obj = mapper.createObjectNode();
      for (Map.Entry<?, ?> entry : map.entrySet()) {
        if (entry.getKey() != null) {
          obj.set(String.valueOf(entry.getKey()), toSafeJsonValue(entry.getValue()));
        }
      }
      return obj;
    }
    if (value instanceof Iterable<?> iterable) {
      ArrayNode arr = mapper.createArrayNode();
      for (Object item : iterable) {
        arr.add(toSafeJsonValue(item));
      }
      return arr;
    }
    return mapper.getNodeFactory().textNode(String.valueOf(value));
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }

  private static boolean isInactive(String situacao) {
    return situacao != null && INACTIVE_SITUACOES.contains(situacao);
  }

  private static RuleBindingExecutor concomitanciaJavaExecutor() {
    return new RuleBindingExecutor() {
      @Override
      public String implementationKey() {
        return Rn013RegrasFrequenciaRuleSet.JAVA_EXECUTOR_CONCOMITANCIA_KEY;
      }

      @Override
      public String implementationVersion() {
        return Rn013RegrasFrequenciaRuleSet.JAVA_EXECUTOR_VERSION;
      }

      @Override
      public RuleExecutorResult evaluate(RuleExecutorContext context) {
        return evaluateConcomitanciaVigenciaOverlap(context);
      }
    };
  }

  private static RuleBindingExecutor frequenciaSobreposicaoJavaExecutor() {
    return new RuleBindingExecutor() {
      @Override
      public String implementationKey() {
        return FrequenciaLancamentoRuleSet.JAVA_EXECUTOR_SOBREPOSICAO_KEY;
      }

      @Override
      public String implementationVersion() {
        return FrequenciaLancamentoRuleSet.JAVA_EXECUTOR_VERSION;
      }

      @Override
      public RuleExecutorResult evaluate(RuleExecutorContext context) {
        return evaluateFrequenciaSobreposicaoOverlap(context);
      }
    };
  }

  private static RuleBindingExecutor afastamentoSobreposicaoJavaExecutor() {
    return new RuleBindingExecutor() {
      @Override
      public String implementationKey() {
        return AfastamentoConcessaoRuleSet.JAVA_EXECUTOR_SOBREPOSICAO_KEY;
      }

      @Override
      public String implementationVersion() {
        return AfastamentoConcessaoRuleSet.JAVA_EXECUTOR_VERSION;
      }

      @Override
      public RuleExecutorResult evaluate(RuleExecutorContext context) {
        return evaluateAfastamentoSobreposicaoOverlap(context);
      }
    };
  }

  private static RuleBindingExecutor dependenteCpfUnicidadeJavaExecutor() {
    return new RuleBindingExecutor() {
      @Override
      public String implementationKey() {
        return DependenteElegibilidadeRuleSet.JAVA_EXECUTOR_CPF_UNICIDADE_KEY;
      }

      @Override
      public String implementationVersion() {
        return DependenteElegibilidadeRuleSet.JAVA_EXECUTOR_VERSION;
      }

      @Override
      public RuleExecutorResult evaluate(RuleExecutorContext context) {
        return evaluateDependenteCpfUnicidade(context);
      }
    };
  }

  private static RuleBindingExecutor vinculoAcumulacaoConstitucionalJavaExecutor() {
    return new RuleBindingExecutor() {
      @Override
      public String implementationKey() {
        return VinculoProvimentoRuleSet.JAVA_EXECUTOR_ACUMULACAO_KEY;
      }

      @Override
      public String implementationVersion() {
        return VinculoProvimentoRuleSet.JAVA_EXECUTOR_VERSION;
      }

      @Override
      public RuleExecutorResult evaluate(RuleExecutorContext context) {
        return evaluateVinculoAcumulacaoConstitucional(context);
      }
    };
  }

  /**
   * Executor Java puro e determinístico para {@code ERG-08393}
   * ({@code PCK_REGRAS_FREQ.MAIN_POS} — {@code PACK_HADES.EH_CONCOMITANTE}).
   */
  private static RuleExecutorResult evaluateConcomitanciaVigenciaOverlap(
      RuleExecutorContext context) {
    JsonNode facts = context.facts();
    JsonNode draft = facts.path("draft");
    JsonNode existingOverlaps = facts.path("existingOverlaps");
    if (draft.isMissingNode() || draft.isNull() || !existingOverlaps.isArray()) {
      return RuleExecutorResult.allow();
    }

    LocalDate draftStart = parseIsoDate(draft.path("dtini"));
    if (draftStart == null) {
      return RuleExecutorResult.of(RuleDecision.INCONCLUSIVE, "MISSING_REQUIRED_FACT");
    }
    LocalDate draftEnd = parseIsoDateOrMax(draft.path("dtfim"));
    Long draftId = draft.path("id").isNumber() ? draft.path("id").longValue() : null;

    for (JsonNode candidate : existingOverlaps) {
      if (candidate == null || candidate.isNull() || !candidate.isObject()) {
        continue;
      }
      Long candidateId = candidate.path("id").isNumber() ? candidate.path("id").longValue() : null;
      if (draftId != null && Objects.equals(draftId, candidateId)) {
        continue;
      }
      if (!sameLogicalScope(draft, candidate)) {
        continue;
      }
      LocalDate candStart = parseIsoDate(candidate.path("dtini"));
      if (candStart == null) {
        continue;
      }
      LocalDate candEnd = parseIsoDateOrMax(candidate.path("dtfim"));
      if (!draftStart.isAfter(candEnd) && !candStart.isAfter(draftEnd)) {
        return RuleExecutorResult.of(RuleDecision.DENY, "ERG-08393");
      }
    }
    return RuleExecutorResult.allow();
  }

  /**
   * Executor Java puro e determinístico para {@code ERG-00424}
   * ({@code PCK_FREQUENCIAS.verifica_concomitancia_periodo}).
   */
  private static RuleExecutorResult evaluateFrequenciaSobreposicaoOverlap(
      RuleExecutorContext context) {
    JsonNode facts = context.facts();
    JsonNode draft = facts.path("draft");
    JsonNode existingFrequencias = facts.path("existingFrequencias");
    if (draft.isMissingNode() || draft.isNull() || !existingFrequencias.isArray()) {
      return RuleExecutorResult.allow();
    }
    String draftSituacao = textOrNull(draft, "situacao");
    if (isInactive(draftSituacao)) {
      return RuleExecutorResult.allow();
    }

    LocalDate draftStart = parseIsoDate(draft.path("dtini"));
    if (draftStart == null) {
      return RuleExecutorResult.of(RuleDecision.INCONCLUSIVE, "MISSING_REQUIRED_FACT");
    }
    LocalDate draftEnd = parseIsoDate(draft.path("dtfim"));
    if (draftEnd == null) {
      draftEnd = draftStart;
    }
    Long draftId = draft.path("id").isNumber() ? draft.path("id").longValue() : null;
    String draftTipofreq = textOrNull(draft, "tipofreq");

    for (JsonNode candidate : existingFrequencias) {
      if (candidate == null || candidate.isNull() || !candidate.isObject()) {
        continue;
      }
      Long candidateId = candidate.path("id").isNumber() ? candidate.path("id").longValue() : null;
      if (draftId != null && Objects.equals(draftId, candidateId)) {
        continue;
      }
      if (isInactive(textOrNull(candidate, "situacao"))) {
        continue;
      }
      String candTipofreq = textOrNull(candidate, "tipofreq");
      if (draftTipofreq != null
          && candTipofreq != null
          && !Objects.equals(draftTipofreq, candTipofreq)) {
        continue;
      }
      LocalDate candStart = parseIsoDate(candidate.path("dtini"));
      if (candStart == null) {
        continue;
      }
      LocalDate candEnd = parseIsoDate(candidate.path("dtfim"));
      if (candEnd == null) {
        candEnd = candStart;
      }
      if (!draftStart.isAfter(candEnd) && !candStart.isAfter(draftEnd)) {
        return RuleExecutorResult.of(RuleDecision.DENY, "ERG-00424");
      }
    }
    return RuleExecutorResult.allow();
  }

  /**
   * Executor Java puro e determinístico para {@code ERG-04300}
   * ({@code PCK_LIC_AFAST.MAIN_POS} — {@code PACK_HADES.EH_CONCOMITANTE}).
   */
  private static RuleExecutorResult evaluateAfastamentoSobreposicaoOverlap(
      RuleExecutorContext context) {
    JsonNode facts = context.facts();
    JsonNode draft = facts.path("draft");
    JsonNode existingAfastamentos = facts.path("existingAfastamentos");
    if (draft.isMissingNode() || draft.isNull() || !existingAfastamentos.isArray()) {
      return RuleExecutorResult.allow();
    }
    String draftSituacao = textOrNull(draft, "situacao");
    if (isInactive(draftSituacao)) {
      return RuleExecutorResult.allow();
    }

    LocalDate draftStart = parseIsoDate(draft.path("dtini"));
    if (draftStart == null) {
      return RuleExecutorResult.of(RuleDecision.INCONCLUSIVE, "MISSING_REQUIRED_FACT");
    }
    LocalDate draftEnd = firstNonNullDate(draft.path("dtfim"), draft.path("dtprevfim"));
    Long draftId = draft.path("id").isNumber() ? draft.path("id").longValue() : null;

    for (JsonNode candidate : existingAfastamentos) {
      if (candidate == null || candidate.isNull() || !candidate.isObject()) {
        continue;
      }
      Long candidateId = candidate.path("id").isNumber() ? candidate.path("id").longValue() : null;
      if (draftId != null && Objects.equals(draftId, candidateId)) {
        continue;
      }
      if (isInactive(textOrNull(candidate, "situacao"))) {
        continue;
      }
      LocalDate candStart = parseIsoDate(candidate.path("dtini"));
      if (candStart == null) {
        continue;
      }
      LocalDate candEnd = firstNonNullDate(candidate.path("dtfim"), candidate.path("dtprevfim"));
      if (!draftStart.isAfter(candEnd) && !candStart.isAfter(draftEnd)) {
        return RuleExecutorResult.of(RuleDecision.DENY, "ERG-04300");
      }
    }
    return RuleExecutorResult.allow();
  }

  /**
   * Executor Java puro e determinístico para {@code ERG-03193}
   * ({@code PCK_DEPENDENTES.PRC_VALID_CPF} — unicidade de CPF de dependente no servidor).
   */
  private static RuleExecutorResult evaluateDependenteCpfUnicidade(
      RuleExecutorContext context) {
    JsonNode facts = context.facts();
    JsonNode draft = facts.path("draft");
    JsonNode existingDependentes = facts.path("existingDependentes");
    if (draft.isMissingNode() || draft.isNull() || !existingDependentes.isArray()) {
      return RuleExecutorResult.allow();
    }
    String draftCpf = textOrNull(draft, "cpf");
    if (draftCpf == null || draftCpf.isBlank()) {
      return RuleExecutorResult.allow();
    }
    Long draftId = draft.path("id").isNumber() ? draft.path("id").longValue() : null;
    Integer draftNumero =
        draft.path("numero").isNumber() ? draft.path("numero").intValue() : null;
    LocalDate draftStart = parseIsoDate(draft.path("dtini"));
    LocalDate draftEnd = parseIsoDateOrMax(draft.path("dtfim"));
    LocalDate refDate = parseIsoDate(facts.path("context").path("referenceDate"));

    for (JsonNode candidate : existingDependentes) {
      if (candidate == null || candidate.isNull() || !candidate.isObject()) {
        continue;
      }
      Long candidateId = candidate.path("id").isNumber() ? candidate.path("id").longValue() : null;
      if (draftId != null && Objects.equals(draftId, candidateId)) {
        continue;
      }
      Integer candidateNumero =
          candidate.path("numero").isNumber() ? candidate.path("numero").intValue() : null;
      if (draftId == null && draftNumero != null && Objects.equals(draftNumero, candidateNumero)) {
        continue;
      }
      LocalDate candEnd = parseIsoDate(candidate.path("dtfim"));
      if (candEnd != null) {
        LocalDate candStart = parseIsoDate(candidate.path("dtini"));
        if ((draftStart != null && candEnd.isBefore(draftStart))
            || (candStart != null && draftEnd.isBefore(candStart))
            || (refDate != null && candEnd.isBefore(refDate))) {
          continue;
        }
      }
      String candCpf = textOrNull(candidate, "cpf");
      if (Objects.equals(draftCpf, candCpf)) {
        return RuleExecutorResult.of(RuleDecision.DENY, "ERG-03193");
      }
    }
    return RuleExecutorResult.allow();
  }

  /**
   * Executor Java puro e determinístico para {@code ERG-00300}
   * ({@code PCK_VINCULOS.VALID_VINC_CONCOMIT} — Acumulação Constitucional CF/88 Art. 37, XVI).
   */
  private static RuleExecutorResult evaluateVinculoAcumulacaoConstitucional(
      RuleExecutorContext context) {
    JsonNode facts = context.facts();
    JsonNode draft = facts.path("draft");
    String draftSituacao = textOrNull(draft, "situacao");
    if (draftSituacao == null || !VINCULO_EM_EXERCICIO_OU_AFASTADO.contains(draftSituacao)) {
      return RuleExecutorResult.allow();
    }
    JsonNode existingVinculos = facts.path("existingVinculos");
    if (!existingVinculos.isArray()) {
      return RuleExecutorResult.of(RuleDecision.INCONCLUSIVE, "MISSING_REQUIRED_FACT");
    }
    if (existingVinculos.isEmpty()) {
      return RuleExecutorResult.allow();
    }
    LocalDate draftStart = parseIsoDate(draft.path("dtexerc"));
    if (draftStart == null) draftStart = parseIsoDate(draft.path("dtposse"));
    if (draftStart == null) draftStart = parseIsoDate(draft.path("dtnom"));
    if (draftStart == null) {
      return RuleExecutorResult.of(RuleDecision.INCONCLUSIVE, "MISSING_REQUIRED_FACT");
    }
    LocalDate draftEnd = parseIsoDateOrMax(draft.path("dtvac"));
    String draftCategoria = textOrNull(draft, "categoria");
    List<LocalDate[]> overlapPeriods = new ArrayList<>();
    boolean missingEvidence = false;
    for (JsonNode candidate : existingVinculos) {
      LocalDate end = parseIsoDateOrMax(candidate.path("fim"));
      if (draftStart.isAfter(end)) continue;
      LocalDate start = parseIsoDate(candidate.path("inicio"));
      if (start == null || start.isAfter(end)) {
        missingEvidence = true;
        continue;
      }
      if (start.isAfter(draftEnd)) continue;
      LocalDate overlapStart = draftStart.isAfter(start) ? draftStart : start;
      LocalDate overlapEnd = draftEnd.isBefore(end) ? draftEnd : end;
      for (LocalDate[] period : overlapPeriods) {
        if (!overlapStart.isAfter(period[1]) && !period[0].isAfter(overlapEnd)) {
          return RuleExecutorResult.of(RuleDecision.DENY, "ERG-00300");
        }
      }
      overlapPeriods.add(new LocalDate[]{overlapStart, overlapEnd});
      String categoria = textOrNull(candidate, "categoria");
      if (draftCategoria != null && categoria != null
          && !isConstitutionalAccumulationPair(draftCategoria, categoria, overlapStart)) {
        return RuleExecutorResult.of(RuleDecision.DENY, "ERG-00300");
      }
      var horarios = ProvaHorariosAcumulacao.avaliar(candidate.path("horarios"), overlapStart, overlapEnd);
      if (horarios == RuleDecision.DENY) return RuleExecutorResult.of(RuleDecision.DENY, "ERG-00300");
      missingEvidence |= draftCategoria == null || categoria == null || horarios != RuleDecision.ALLOW;
    }
    if (missingEvidence) {
      return RuleExecutorResult.of(RuleDecision.INCONCLUSIVE, "MISSING_REQUIRED_FACT");
    }
    return RuleExecutorResult.allow();
  }

  private static boolean isConstitutionalAccumulationPair(String catA, String catB, LocalDate date) {
    boolean aProf = PROFESSOR_CATEGORIAS.contains(catA);
    boolean bProf = PROFESSOR_CATEGORIAS.contains(catB);
    boolean aSaude = SAUDE_CATEGORIAS.contains(catA);
    boolean bSaude = SAUDE_CATEGORIAS.contains(catB);
    if ((aProf && bProf) || (aSaude && bSaude)) return true;
    if (!date.isBefore(EC_138_VIGENCIA)) return aProf || bProf;
    return (aProf && (TECNICO_CIENTIFICO_CATEGORIAS.contains(catB) || bSaude))
        || (bProf && (TECNICO_CIENTIFICO_CATEGORIAS.contains(catA) || aSaude));
  }

  private static boolean sameLogicalScope(JsonNode a, JsonNode b) {
    return Objects.equals(textOrNull(a, "empCodigo"), textOrNull(b, "empCodigo"))
        && Objects.equals(textOrNull(a, "regimeJuridico"), textOrNull(b, "regimeJuridico"))
        && Objects.equals(textOrNull(a, "tipoVinculo"), textOrNull(b, "tipoVinculo"))
        && Objects.equals(textOrNull(a, "categoria"), textOrNull(b, "categoria"))
        && Objects.equals(textOrNull(a, "subcategoria"), textOrNull(b, "subcategoria"))
        && Objects.equals(textOrNull(a, "tipofreq"), textOrNull(b, "tipofreq"))
        && Objects.equals(textOrNull(a, "codfreq"), textOrNull(b, "codfreq"));
  }

  private static String textOrNull(JsonNode node, String field) {
    JsonNode v = node.path(field);
    return v.isMissingNode() || v.isNull() ? null : v.asText();
  }

  private static LocalDate parseIsoDate(JsonNode node) {
    if (node == null || node.isMissingNode() || node.isNull() || !node.isTextual()) {
      return null;
    }
    return LocalDate.parse(node.asText());
  }

  private static LocalDate parseIsoDateOrMax(JsonNode node) {
    LocalDate parsed = parseIsoDate(node);
    return parsed != null ? parsed : OPEN_ENDED_DATE;
  }

  private static LocalDate firstNonNullDate(JsonNode primary, JsonNode fallback) {
    LocalDate p = parseIsoDate(primary);
    if (p != null) {
      return p;
    }
    LocalDate f = parseIsoDate(fallback);
    return f != null ? f : OPEN_ENDED_DATE;
  }

  private static void putTextOrNull(ObjectNode node, String field, String value) {
    node.set(field, org.praxisplatform.rules.facts.PraxisFactTreeBuilder.toJsonNode(value));
  }

  private static void putIntOrNull(ObjectNode node, String field, Integer value) {
    node.set(field, org.praxisplatform.rules.facts.PraxisFactTreeBuilder.toJsonNode(value));
  }

  private static void putLongOrNull(ObjectNode node, String field, Long value) {
    node.set(field, org.praxisplatform.rules.facts.PraxisFactTreeBuilder.toJsonNode(value));
  }

  private static void putBooleanOrNull(ObjectNode node, String field, Boolean value) {
    node.set(field, org.praxisplatform.rules.facts.PraxisFactTreeBuilder.toJsonNode(value));
  }

  private static void putDateOrNull(ObjectNode node, String field, LocalDate value) {
    node.set(field, org.praxisplatform.rules.facts.PraxisFactTreeBuilder.toJsonNode(value));
  }

  // ============================================================================================
  // Slice 5 (Férias e Períodos Aquisitivos): Planos, Fatos e Executores Java Determinísticos
  // ============================================================================================

  /**
   * Retorna o {@link RuleDecisionPlan} compilado de parâmetros de contagem de férias para o órgão
   * informado.
   */
  public RuleDecisionPlan resolveParametroContagemFeriasPlan(Integer empCodigo) {
    return resolvePlan(defaultProductParametroContagemFeriasPlan.ref().ruleSetKey(), empCodigo);
  }

  /**
   * Registra ou atualiza em memória um plano customizado de parâmetros de contagem de férias para
   * um órgão específico.
   */
  public RuleDecisionPlan registerOrgaoParametroContagemFeriasPlan(
      Integer empCodigo, RuleSetDefinition definition) {
    return registerPlan(defaultProductParametroContagemFeriasPlan.ref().ruleSetKey(), empCodigo, definition);
  }

  /**
   * Retorna o {@link RuleDecisionPlan} compilado de períodos aquisitivos de férias para o órgão
   * informado.
   */
  public RuleDecisionPlan resolvePeriodoAquisitivoFeriasPlan(Integer empCodigo) {
    return resolvePlan(defaultProductPeriodoAquisitivoFeriasPlan.ref().ruleSetKey(), empCodigo);
  }

  /**
   * Registra ou atualiza em memória um plano customizado de períodos aquisitivos de férias para um
   * órgão específico.
   */
  public RuleDecisionPlan registerOrgaoPeriodoAquisitivoFeriasPlan(
      Integer empCodigo, RuleSetDefinition definition) {
    return registerPlan(defaultProductPeriodoAquisitivoFeriasPlan.ref().ruleSetKey(), empCodigo, definition);
  }

  /**
   * Retorna o {@link RuleDecisionPlan} compilado de programação e gozo de férias para o órgão
   * informado.
   */
  public RuleDecisionPlan resolveFeriasGozoPlan(Integer empCodigo) {
    return resolvePlan(defaultProductFeriasGozoPlan.ref().ruleSetKey(), empCodigo);
  }

  /**
   * Registra ou atualiza em memória um plano customizado de programação e gozo de férias para um
   * órgão específico.
   */
  public RuleDecisionPlan registerOrgaoFeriasGozoPlan(
      Integer empCodigo, RuleSetDefinition definition) {
    return registerPlan(defaultProductFeriasGozoPlan.ref().ruleSetKey(), empCodigo, definition);
  }

  /**
   * Avalia deterministicamente o cadastro/manutenção de {@link ParametroContagemFerias} contra o
   * plano do órgão.
   */
  public RuleEvaluationResult evaluateParametroContagemFerias(
      ParametroContagemFeriasRuleContext ctx) {
    Objects.requireNonNull(ctx, "ctx must not be null");
    Integer empCodigo =
        ctx.draft() != null && ctx.draft().getEmpCodigo() != null ? ctx.draft().getEmpCodigo() : 1;
    RuleDecisionPlan plan = resolveParametroContagemFeriasPlan(empCodigo);
    ObjectNode facts = buildParametroContagemFeriasFacts(ctx);
    return evaluate(plan, facts);
  }

  /**
   * Avalia o cadastro/manutenção de {@link ParametroContagemFerias} em modo <i>fail-closed</i>,
   * lançando {@link ErgonRuleViolationException} (HTTP 422) se o resultado não for
   * {@link RuleDecision#ALLOW}.
   */
  public RuleEvaluationResult enforceParametroContagemFerias(
      ParametroContagemFeriasRuleContext ctx) {
    Objects.requireNonNull(ctx, "ctx must not be null");
    Integer empCodigo =
        ctx.draft() != null && ctx.draft().getEmpCodigo() != null ? ctx.draft().getEmpCodigo() : 1;
    RuleDecisionPlan plan = resolveParametroContagemFeriasPlan(empCodigo);
    ObjectNode facts = buildParametroContagemFeriasFacts(ctx);
    return enforce(plan, facts);
  }

  /**
   * Avalia deterministicamente a abertura/manutenção de {@link PeriodoAquisitivoFerias} contra o
   * plano do órgão.
   */
  public RuleEvaluationResult evaluatePeriodoAquisitivoFerias(
      PeriodoAquisitivoFeriasRuleContext ctx) {
    Objects.requireNonNull(ctx, "ctx must not be null");
    Integer empCodigo =
        ctx.draft() != null && ctx.draft().getEmpCodigo() != null ? ctx.draft().getEmpCodigo() : 1;
    RuleDecisionPlan plan = resolvePeriodoAquisitivoFeriasPlan(empCodigo);
    ObjectNode facts = buildPeriodoAquisitivoFeriasFacts(ctx);
    return evaluate(plan, facts);
  }

  /**
   * Avalia a abertura/manutenção de {@link PeriodoAquisitivoFerias} em modo <i>fail-closed</i>,
   * lançando {@link ErgonRuleViolationException} (HTTP 422) se o resultado não for
   * {@link RuleDecision#ALLOW}.
   */
  public RuleEvaluationResult enforcePeriodoAquisitivoFerias(
      PeriodoAquisitivoFeriasRuleContext ctx) {
    Objects.requireNonNull(ctx, "ctx must not be null");
    Integer empCodigo =
        ctx.draft() != null && ctx.draft().getEmpCodigo() != null ? ctx.draft().getEmpCodigo() : 1;
    RuleDecisionPlan plan = resolvePeriodoAquisitivoFeriasPlan(empCodigo);
    ObjectNode facts = buildPeriodoAquisitivoFeriasFacts(ctx);
    return enforce(plan, facts);
  }

  /**
   * Avalia deterministicamente a programação/concessão de {@link FeriasGozo} contra o plano do
   * órgão.
   */
  public RuleEvaluationResult evaluateFeriasGozo(FeriasGozoRuleContext ctx) {
    Objects.requireNonNull(ctx, "ctx must not be null");
    Integer empCodigo =
        ctx.draft() != null && ctx.draft().getEmpCodigo() != null ? ctx.draft().getEmpCodigo() : 1;
    RuleDecisionPlan plan = resolveFeriasGozoPlan(empCodigo);
    ObjectNode facts = buildFeriasGozoFacts(ctx);
    return evaluate(plan, facts);
  }

  /**
   * Avalia a programação/concessão de {@link FeriasGozo} em modo <i>fail-closed</i>, lançando
   * {@link ErgonRuleViolationException} (HTTP 422) se o resultado não for {@link RuleDecision#ALLOW}.
   */
  public RuleEvaluationResult enforceFeriasGozo(FeriasGozoRuleContext ctx) {
    Objects.requireNonNull(ctx, "ctx must not be null");
    Integer empCodigo =
        ctx.draft() != null && ctx.draft().getEmpCodigo() != null ? ctx.draft().getEmpCodigo() : 1;
    RuleDecisionPlan plan = resolveFeriasGozoPlan(empCodigo);
    ObjectNode facts = buildFeriasGozoFacts(ctx);
    return enforce(plan, facts);
  }

  /**
   * Constrói o envelope canônico de fatos de {@link ParametroContagemFerias}
   * ({@code draft}, {@code scope}, {@code existingRules}, {@code derived}, {@code tenant},
   * {@code context}).
   */
  public ObjectNode buildParametroContagemFeriasFacts(ParametroContagemFeriasRuleContext ctx) {
    return FeriasFacts.ofParametroContagem(ctx);
  }

  /**
   * Constrói o envelope canônico de fatos de {@link PeriodoAquisitivoFerias}
   * ({@code draft}, {@code vinculo}, {@code effectiveParametroConta}, {@code derived},
   * {@code existingPeriodos}, {@code tenant}, {@code context}).
   */
  public ObjectNode buildPeriodoAquisitivoFeriasFacts(PeriodoAquisitivoFeriasRuleContext ctx) {
    return FeriasFacts.ofPeriodoAquisitivo(vacationParameterResolver, ctx);
  }

  /**
   * Constrói o envelope canônico de fatos de {@link FeriasGozo}
   * ({@code draft}, {@code vinculo}, {@code substituto}, {@code pa},
   * {@code effectiveParametroConta}, {@code derived}, {@code existingParcelas}, {@code tenant},
   * {@code context}).
   */
  public ObjectNode buildFeriasGozoFacts(FeriasGozoRuleContext ctx) {
    return FeriasFacts.ofFeriasGozo(vacationParameterResolver, ctx);
  }

  private static RuleBindingExecutor parametroContagemConcomitanciaJavaExecutor() {
    return new RuleBindingExecutor() {
      @Override
      public String implementationKey() {
        return ParametroContagemFeriasRuleSet.JAVA_EXECUTOR_CONCOMITANCIA_KEY;
      }

      @Override
      public String implementationVersion() {
        return ParametroContagemFeriasRuleSet.JAVA_EXECUTOR_VERSION;
      }

      @Override
      public RuleExecutorResult evaluate(RuleExecutorContext context) {
        JsonNode facts = context.facts();
        int overlaps = facts.path("scope").path("overlappingActiveCount").asInt(0);
        if (overlaps > 0) {
          return RuleExecutorResult.of(RuleDecision.DENY, "ERG-01850");
        }
        return RuleExecutorResult.allow();
      }
    };
  }

}
