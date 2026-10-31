package br.com.praxis.ergonshowcase.rules;

import br.com.praxis.ergonshowcase.domain.Afastamento;
import br.com.praxis.ergonshowcase.domain.Cargo;
import br.com.praxis.ergonshowcase.domain.CodigoFrequencia;
import br.com.praxis.ergonshowcase.domain.DefinicaoCampoCustomizado;
import br.com.praxis.ergonshowcase.domain.Dependente;
import br.com.praxis.ergonshowcase.domain.DocumentoLegal;
import br.com.praxis.ergonshowcase.domain.Frequencia;
import br.com.praxis.ergonshowcase.domain.Orgao;
import br.com.praxis.ergonshowcase.domain.RegraFrequenciaEstatutaria;
import br.com.praxis.ergonshowcase.domain.ServidorPessoa;
import br.com.praxis.ergonshowcase.domain.Setor;
import br.com.praxis.ergonshowcase.domain.TipoDependente;
import br.com.praxis.ergonshowcase.domain.TipoFrequencia;
import br.com.praxis.ergonshowcase.domain.Vinculo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
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
import org.praxisplatform.rules.contract.DecisionSource;
import org.praxisplatform.rules.contract.DecisionStage;
import org.praxisplatform.rules.contract.RuleDecision;
import org.praxisplatform.rules.contract.RuleEvaluationResult;
import org.praxisplatform.rules.contract.RuleExecutorResult;
import org.praxisplatform.rules.contract.RuleSetDefinition;
import org.praxisplatform.rules.jsonlogic.PraxisJsonLogicEngine;
import org.praxisplatform.rules.jsonlogic.PraxisJsonLogicException;
import org.praxisplatform.rules.jsonlogic.model.JsonLogicEvaluationOptions;
import org.praxisplatform.rules.jsonlogic.model.JsonLogicValidationOptions;
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
 * <p>Suporta as 5 famílias governadas do domínio do Ergon RH Público:
 * <ol>
 *   <li>{@link Rn013RegrasFrequenciaRuleSet} ({@code ergon.administracao-pessoal.regras-frequencia.rn013});
 *   <li>{@link FrequenciaLancamentoRuleSet} ({@code ergon.administracao-pessoal.frequencias.lancamento});
 *   <li>{@link AfastamentoConcessaoRuleSet} ({@code ergon.administracao-pessoal.afastamentos.concessao});
 *   <li>{@link DependenteElegibilidadeRuleSet} ({@code ergon.administracao-pessoal.dependentes.elegibilidade});
 *   <li>{@link VinculoProvimentoRuleSet} ({@code ergon.administracao-pessoal.vinculos.provimento}).
 * </ol>
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
      List<Vinculo> existingVinculos,
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
      boolean inPlaceProrrogacao) {

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

  private final ObjectMapper mapper;
  private final PraxisJsonLogicEngine jsonLogicEngine;
  private final RuleBindingExecutorRegistry executorRegistry;
  private final PraxisRulePlanCompiler compiler;
  private final PraxisRuleSetEngine engine;

  private final Map<Integer, RuleDecisionPlan> rn013PlansByOrgao = new ConcurrentHashMap<>();
  private final RuleDecisionPlan defaultProductRn013Plan;

  private final Map<Integer, RuleDecisionPlan> frequenciaPlansByOrgao = new ConcurrentHashMap<>();
  private final RuleDecisionPlan defaultProductFrequenciaPlan;

  private final Map<Integer, RuleDecisionPlan> afastamentoPlansByOrgao = new ConcurrentHashMap<>();
  private final RuleDecisionPlan defaultProductAfastamentoPlan;

  private final Map<Integer, RuleDecisionPlan> dependentePlansByOrgao = new ConcurrentHashMap<>();
  private final RuleDecisionPlan defaultProductDependentePlan;

  private final Map<Integer, RuleDecisionPlan> vinculoPlansByOrgao = new ConcurrentHashMap<>();
  private final RuleDecisionPlan defaultProductVinculoPlan;

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
                vinculoAcumulacaoConstitucionalJavaExecutor()));
    this.compiler = new PraxisRulePlanCompiler(this.executorRegistry);
    this.engine = new PraxisRuleSetEngine(this.executorRegistry);

    this.defaultProductRn013Plan =
        this.compiler.compile(Rn013RegrasFrequenciaRuleSet.productRuleSet());
    this.rn013PlansByOrgao.put(1, this.defaultProductRn013Plan);
    this.rn013PlansByOrgao.put(
        2, this.compiler.compile(Rn013RegrasFrequenciaRuleSet.orgao2CustomizedRuleSet()));

    this.defaultProductFrequenciaPlan =
        this.compiler.compile(FrequenciaLancamentoRuleSet.productRuleSet());
    this.frequenciaPlansByOrgao.put(1, this.defaultProductFrequenciaPlan);
    this.frequenciaPlansByOrgao.put(
        2, this.compiler.compile(FrequenciaLancamentoRuleSet.orgao2CustomizedRuleSet()));

    this.defaultProductAfastamentoPlan =
        this.compiler.compile(AfastamentoConcessaoRuleSet.productRuleSet());
    this.afastamentoPlansByOrgao.put(1, this.defaultProductAfastamentoPlan);
    this.afastamentoPlansByOrgao.put(
        2, this.compiler.compile(AfastamentoConcessaoRuleSet.orgao2CustomizedRuleSet()));

    this.defaultProductDependentePlan =
        this.compiler.compile(DependenteElegibilidadeRuleSet.productRuleSet());
    this.dependentePlansByOrgao.put(1, this.defaultProductDependentePlan);
    this.dependentePlansByOrgao.put(
        2, this.compiler.compile(DependenteElegibilidadeRuleSet.orgao2CustomizedRuleSet()));

    this.defaultProductVinculoPlan =
        this.compiler.compile(VinculoProvimentoRuleSet.productRuleSet());
    this.vinculoPlansByOrgao.put(1, this.defaultProductVinculoPlan);
    this.vinculoPlansByOrgao.put(
        2, this.compiler.compile(VinculoProvimentoRuleSet.orgao2CustomizedRuleSet()));
  }

  public PraxisJsonLogicEngine getJsonLogicEngine() {
    return jsonLogicEngine;
  }

  /** Compila e valida estaticamente um {@link RuleSetDefinition} contra o registro de executores. */
  public RuleDecisionPlan compilePlan(RuleSetDefinition definition) {
    Objects.requireNonNull(definition, "definition must not be null");
    return compiler.compile(definition);
  }

  /** Retorna o {@link RuleDecisionPlan} compilado da {@code RN-013} para o órgão informado. */
  public RuleDecisionPlan resolveRn013Plan(Integer empCodigo) {
    if (empCodigo == null) {
      return defaultProductRn013Plan;
    }
    return rn013PlansByOrgao.getOrDefault(empCodigo, defaultProductRn013Plan);
  }

  /** Registra ou atualiza em memória um plano customizado da {@code RN-013} para um órgão específico. */
  public RuleDecisionPlan registerOrgaoRn013Plan(Integer empCodigo, RuleSetDefinition definition) {
    Objects.requireNonNull(empCodigo, "empCodigo must not be null");
    RuleDecisionPlan compiled = compilePlan(definition);
    rn013PlansByOrgao.put(empCodigo, compiled);
    return compiled;
  }

  /** Retorna o {@link RuleDecisionPlan} compilado de lançamento de frequência para o órgão informado. */
  public RuleDecisionPlan resolveFrequenciaPlan(Integer empCodigo) {
    if (empCodigo == null) {
      return defaultProductFrequenciaPlan;
    }
    return frequenciaPlansByOrgao.getOrDefault(empCodigo, defaultProductFrequenciaPlan);
  }

  /** Registra ou atualiza em memória um plano customizado de frequência para um órgão específico. */
  public RuleDecisionPlan registerOrgaoFrequenciaPlan(
      Integer empCodigo, RuleSetDefinition definition) {
    Objects.requireNonNull(empCodigo, "empCodigo must not be null");
    RuleDecisionPlan compiled = compilePlan(definition);
    frequenciaPlansByOrgao.put(empCodigo, compiled);
    return compiled;
  }

  /** Retorna o {@link RuleDecisionPlan} compilado de concessão de afastamento para o órgão informado. */
  public RuleDecisionPlan resolveAfastamentoPlan(Integer empCodigo) {
    if (empCodigo == null) {
      return defaultProductAfastamentoPlan;
    }
    return afastamentoPlansByOrgao.getOrDefault(empCodigo, defaultProductAfastamentoPlan);
  }

  /** Registra ou atualiza em memória um plano customizado de afastamento para um órgão específico. */
  public RuleDecisionPlan registerOrgaoAfastamentoPlan(
      Integer empCodigo, RuleSetDefinition definition) {
    Objects.requireNonNull(empCodigo, "empCodigo must not be null");
    RuleDecisionPlan compiled = compilePlan(definition);
    afastamentoPlansByOrgao.put(empCodigo, compiled);
    return compiled;
  }

  /** Retorna o {@link RuleDecisionPlan} compilado de elegibilidade de dependente para o órgão informado. */
  public RuleDecisionPlan resolveDependentePlan(Integer empCodigo) {
    if (empCodigo == null) {
      return defaultProductDependentePlan;
    }
    return dependentePlansByOrgao.getOrDefault(empCodigo, defaultProductDependentePlan);
  }

  /** Registra ou atualiza em memória um plano customizado de dependente para um órgão específico. */
  public RuleDecisionPlan registerOrgaoDependentePlan(
      Integer empCodigo, RuleSetDefinition definition) {
    Objects.requireNonNull(empCodigo, "empCodigo must not be null");
    RuleDecisionPlan compiled = compilePlan(definition);
    dependentePlansByOrgao.put(empCodigo, compiled);
    return compiled;
  }

  /** Retorna o {@link RuleDecisionPlan} compilado de provimento de vínculo para o órgão informado. */
  public RuleDecisionPlan resolveVinculoPlan(Integer empCodigo) {
    if (empCodigo == null) {
      return defaultProductVinculoPlan;
    }
    return vinculoPlansByOrgao.getOrDefault(empCodigo, defaultProductVinculoPlan);
  }

  /** Registra ou atualiza em memória um plano customizado de vínculo para um órgão específico. */
  public RuleDecisionPlan registerOrgaoVinculoPlan(
      Integer empCodigo, RuleSetDefinition definition) {
    Objects.requireNonNull(empCodigo, "empCodigo must not be null");
    RuleDecisionPlan compiled = compilePlan(definition);
    vinculoPlansByOrgao.put(empCodigo, compiled);
    return compiled;
  }

  /** Avalia um {@link RuleDecisionPlan} compilado sobre os fatos normalizados. */
  public RuleEvaluationResult evaluate(RuleDecisionPlan plan, JsonNode facts) {
    return evaluate(plan, facts, Instant.now().toString());
  }

  /** Avalia um {@link RuleDecisionPlan} compilado com instante UTC explícito para reprodutibilidade. */
  public RuleEvaluationResult evaluate(RuleDecisionPlan plan, JsonNode facts, String nowUtc) {
    Objects.requireNonNull(plan, "plan must not be null");
    Objects.requireNonNull(facts, "facts must not be null");
    return engine.evaluate(plan, facts, nowUtc, DEFAULT_TIME_ZONE);
  }

  /**
   * Avalia um {@link RuleDecisionPlan} e impõe semântica <i>fail-closed</i>: qualquer decisão final
   * diferente de {@link RuleDecision#ALLOW} lança {@link ErgonRuleViolationException} (HTTP 422).
   */
  public RuleEvaluationResult enforce(RuleDecisionPlan plan, JsonNode facts) {
    RuleEvaluationResult result = evaluate(plan, facts);
    if (result.decision() != RuleDecision.ALLOW) {
      throw new ErgonRuleViolationException(plan, result);
    }
    return result;
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

    JsonNode exprNode = extractJsonLogicExpression(validationRules);
    if (exprNode != null) {
      JsonLogicValidationOptions options =
          new JsonLogicValidationOptions(
              CUSTOM_FIELD_JSON_LOGIC_ROOTS,
              "draft",
              false,
              Instant.now().toString(),
              DEFAULT_TIME_ZONE,
              true);
      try {
        jsonLogicEngine.validate(exprNode, options);
      } catch (PraxisJsonLogicException ex) {
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
    if (activeDefinitions == null || activeDefinitions.isEmpty()) {
      return;
    }
    Map<String, Object> safeCustomFields = customFields != null ? customFields : Map.of();
    JsonNode customFieldsNode = toSafeJsonValue(safeCustomFields);
    ObjectNode draftNode = toSafeEntityNode(draftEntity);
    String nowUtc = Instant.now().toString();
    JsonLogicEvaluationOptions evalOptions =
        new JsonLogicEvaluationOptions(
            CUSTOM_FIELD_JSON_LOGIC_ROOTS, "draft", false, nowUtc, DEFAULT_TIME_ZONE);

    Map<String, DefinicaoCampoCustomizado> effectiveDefs = new LinkedHashMap<>();
    for (DefinicaoCampoCustomizado candidate : activeDefinitions) {
      if (candidate == null
          || Boolean.FALSE.equals(candidate.getActive())
          || candidate.getFieldKey() == null) {
        continue;
      }
      if (candidate.getEmpCodigo() != null
          && empCodigo != null
          && !Objects.equals(candidate.getEmpCodigo(), empCodigo)) {
        continue;
      }
      DefinicaoCampoCustomizado prev = effectiveDefs.get(candidate.getFieldKey());
      if (prev == null || candidate.getEmpCodigo() != null) {
        effectiveDefs.put(candidate.getFieldKey(), candidate);
      }
    }

    for (DefinicaoCampoCustomizado def : effectiveDefs.values()) {
      String fieldKey = def.getFieldKey();
      if (def.getValidationRules() == null || def.getValidationRules().isEmpty()) {
        continue;
      }
      JsonNode exprNode = extractJsonLogicExpression(def.getValidationRules());
      if (exprNode == null) {
        continue;
      }
      boolean fieldPresent = safeCustomFields.containsKey(fieldKey);
      String exprText = exprNode.toString();
      boolean referencesDraftOrMap =
          exprText.contains("\"draft.")
              || exprText.contains("\"entity.")
              || exprText.contains("\"customFields.");
      if (!fieldPresent && !Boolean.TRUE.equals(def.getRequired()) && !referencesDraftOrMap) {
        continue;
      }
      Object fieldValue = safeCustomFields.get(fieldKey);
      ObjectNode contextData = mapper.createObjectNode();
      contextData.set("value", toSafeJsonValue(fieldValue));
      contextData.set("customFields", customFieldsNode);
      contextData.set("draft", draftNode);
      contextData.set("entity", draftNode);

      boolean allowed;
      try {
        Object result = jsonLogicEngine.evaluate(exprNode, contextData, evalOptions);
        allowed = jsonLogicEngine.truthy(result);
      } catch (PraxisJsonLogicException ex) {
        allowed = false;
      }
      if (!allowed) {
        Object customMsg = def.getValidationRules().get("customMessage");
        String msg =
            customMsg != null && !String.valueOf(customMsg).isBlank()
                ? String.valueOf(customMsg)
                : ErgonRuleErrorCatalog.ERG_21502.messagePtBr()
                    + " (recurso: "
                    + resourceKey
                    + ", campo: "
                    + fieldKey
                    + ").";
        throw new ErgonRuleViolationException(
            ErgonRuleErrorCatalog.ERG_21502.code(),
            ErgonRuleErrorCatalog.ERG_21502.slotKey(),
            "product.ergon.customField." + fieldKey,
            DecisionSource.PRODUCT,
            DecisionStage.DOMAIN_DECISION,
            "customFields." + fieldKey,
            msg);
      }
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
    List<ErgonRuleViolationException.SlotViolationDetail> violations =
        ErgonRuleViolationException.buildViolations(plan, result);
    Map<String, Object> payload = new java.util.LinkedHashMap<>();
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
            .map(ErgonRuleViolationException.SlotViolationDetail::slotKey)
            .distinct()
            .toList());
    payload.put(
        "decisionSources",
        violations.stream()
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
    ObjectNode root = mapper.createObjectNode();
    root.set("draft", toDraftNode(draft));

    ArrayNode overlapsArray = mapper.createArrayNode();
    if (existingOverlaps != null) {
      for (RegraFrequenciaEstatutaria existing : existingOverlaps) {
        if (existing != null) {
          overlapsArray.add(toDraftNode(existing));
        }
      }
    }
    root.set("existingOverlaps", overlapsArray);

    ObjectNode tenantNode = mapper.createObjectNode();
    tenantNode.put("empCodigo", empCodigo != null ? empCodigo : 1);
    root.set("tenant", tenantNode);

    ObjectNode contextNode = mapper.createObjectNode();
    contextNode.put("resourceKey", Rn013RegrasFrequenciaRuleSet.RESOURCE_KEY);
    contextNode.put("ruleSetKey", Rn013RegrasFrequenciaRuleSet.RULESET_KEY);
    root.set("context", contextNode);
    return root;
  }

  /**
   * Constrói a árvore JSON imutável de fatos para {@link FrequenciaLancamentoRuleSet}, incluindo a
   * ponte dinâmica {@code effectiveStatutoryRule} derivada de {@link RegraFrequenciaEstatutaria}.
   */
  public ObjectNode buildFrequenciaFacts(FrequenciaRuleContext ctx) {
    Objects.requireNonNull(ctx, "ctx must not be null");
    Frequencia draft = ctx.draft();
    Integer empCodigo = draft != null && draft.getEmpCodigo() != null ? draft.getEmpCodigo() : 1;

    ObjectNode root = mapper.createObjectNode();
    int durationDays = computeFrequenciaDurationDays(draft);
    BigDecimal effectiveQuantity =
        draft != null && draft.getQuantidade() != null
            ? draft.getQuantidade().stripTrailingZeros()
            : BigDecimal.valueOf(durationDays);

    ObjectNode draftNode = mapper.createObjectNode();
    if (draft != null) {
      putLongOrNull(draftNode, "id", draft.getId());
      putIntOrNull(draftNode, "empCodigo", draft.getEmpCodigo());
      putLongOrNull(draftNode, "numfunc", draft.getNumfunc());
      putIntOrNull(draftNode, "numvinc", draft.getNumvinc());
      putTextOrNull(draftNode, "tipofreq", draft.getTipofreq());
      putIntOrNull(draftNode, "codfreq", draft.getCodfreq());
      if (draft.getDtini() != null) {
        draftNode.put("dtini", draft.getDtini().toString());
      }
      putDateOrNull(draftNode, "dtfim", draft.getDtfim());
      if (draft.getHoraEntrada() != null) {
        draftNode.put(
            "horaEntradaMinutes",
            draft.getHoraEntrada().getHour() * 60 + draft.getHoraEntrada().getMinute());
      } else {
        draftNode.putNull("horaEntradaMinutes");
      }
      if (draft.getHoraSaida() != null) {
        draftNode.put(
            "horaSaidaMinutes",
            draft.getHoraSaida().getHour() * 60 + draft.getHoraSaida().getMinute());
      } else {
        draftNode.putNull("horaSaidaMinutes");
      }
      if (draft.getQuantidade() != null) {
        draftNode.put("quantidade", draft.getQuantidade().stripTrailingZeros());
      } else {
        draftNode.putNull("quantidade");
      }
      draftNode.put("effectiveQuantity", effectiveQuantity);
      draftNode.put("effectiveDurationDays", durationDays);
      putTextOrNull(draftNode, "situacao", draft.getSituacao());
      putLongOrNull(draftNode, "documentoLegalId", draft.getDocumentoLegalId());
      putIntOrNull(draftNode, "retroactiveDays", resolveRetroactiveDays(draft, ctx.referenceDate()));
    }
    root.set("draft", draftNode);

    root.set("vinculo", toVinculoNode(ctx.vinculo(), empCodigo));
    root.set("pessoa", toPessoaNode(ctx.pessoa()));
    root.set(
        "tipoFrequencia",
        toTipoFrequenciaNode(
            draft != null ? draft.getTipoFrequenciaEntidade() : null, ctx.codigoFrequencia()));
    root.set("codigoFrequencia", toCodigoFrequenciaNode(ctx.codigoFrequencia()));
    root.set(
        "documentoLegal",
        toDocumentoLegalNode(
            draft != null ? draft.getDocumentoLegal() : null,
            draft != null ? draft.getDocumentoLegalId() : null,
            empCodigo));

    LocalDate eventStart = draft != null ? draft.getDtini() : null;
    LocalDate eventEnd =
        draft != null ? (draft.getDtfim() != null ? draft.getDtfim() : draft.getDtini()) : null;

    root.set(
        "effectiveStatutoryRule",
        buildFrequenciaStatutoryRuleNode(
            ctx.effectiveStatutoryRule(),
            draft,
            durationDays,
            eventStart,
            eventEnd,
            ctx.existingFrequencias(),
            ctx.activeAfastamentos()));

    ArrayNode existingArray = mapper.createArrayNode();
    if (ctx.existingFrequencias() != null) {
      for (Frequencia existing : ctx.existingFrequencias()) {
        if (existing != null) {
          ObjectNode item = mapper.createObjectNode();
          putLongOrNull(item, "id", existing.getId());
          putLongOrNull(item, "numfunc", existing.getNumfunc());
          putIntOrNull(item, "numvinc", existing.getNumvinc());
          putTextOrNull(item, "tipofreq", existing.getTipofreq());
          putIntOrNull(item, "codfreq", existing.getCodfreq());
          putDateOrNull(item, "dtini", existing.getDtini());
          putDateOrNull(item, "dtfim", existing.getDtfim());
          putTextOrNull(item, "situacao", existing.getSituacao());
          existingArray.add(item);
        }
      }
    }
    root.set("existingFrequencias", existingArray);

    ArrayNode activeAfastArray = mapper.createArrayNode();
    if (ctx.activeAfastamentos() != null) {
      for (Afastamento afast : ctx.activeAfastamentos()) {
        if (afast != null) {
          activeAfastArray.add(toAfastamentoSummaryNode(afast));
        }
      }
    }
    root.set("activeAfastamentos", activeAfastArray);

    ObjectNode tenantNode = mapper.createObjectNode();
    tenantNode.put("empCodigo", empCodigo);
    root.set("tenant", tenantNode);

    ObjectNode contextNode = mapper.createObjectNode();
    contextNode.put("resourceKey", FrequenciaLancamentoRuleSet.RESOURCE_KEY);
    contextNode.put("ruleSetKey", FrequenciaLancamentoRuleSet.RULESET_KEY);
    root.set("context", contextNode);
    return root;
  }

  /**
   * Constrói a árvore JSON imutável de fatos para {@link AfastamentoConcessaoRuleSet}, incluindo a
   * ponte dinâmica {@code effectiveStatutoryRule} derivada de {@link RegraFrequenciaEstatutaria}.
   */
  public ObjectNode buildAfastamentoFacts(AfastamentoRuleContext ctx) {
    Objects.requireNonNull(ctx, "ctx must not be null");
    Afastamento draft = ctx.draft();
    Integer empCodigo = draft != null && draft.getEmpCodigo() != null ? draft.getEmpCodigo() : 1;

    ObjectNode root = mapper.createObjectNode();
    Integer durationDays = computeAfastamentoDurationDays(draft);

    ObjectNode draftNode = mapper.createObjectNode();
    if (draft != null) {
      putLongOrNull(draftNode, "id", draft.getId());
      putIntOrNull(draftNode, "empCodigo", draft.getEmpCodigo());
      putLongOrNull(draftNode, "numfunc", draft.getNumfunc());
      putIntOrNull(draftNode, "numvinc", draft.getNumvinc());
      putTextOrNull(draftNode, "tipofreq", draft.getTipofreq());
      putIntOrNull(draftNode, "codfreq", draft.getCodfreq());
      if (draft.getDtini() != null) {
        draftNode.put("dtini", draft.getDtini().toString());
      }
      putDateOrNull(draftNode, "dtprevfim", draft.getDtprevfim());
      putDateOrNull(draftNode, "dtfim", draft.getDtfim());
      putIntOrNull(draftNode, "quantidadeDias", draft.getQuantidadeDias());
      putIntOrNull(draftNode, "effectiveDurationDays", durationDays != null ? durationDays : 1);
      putBooleanOrNull(draftNode, "prorrogacao", draft.getProrrogacao());
      draftNode.put("inPlaceProrrogacao", ctx.inPlaceProrrogacao());
      putLongOrNull(draftNode, "afastamentoOrigemId", draft.getAfastamentoOrigemId());
      putLongOrNull(draftNode, "numfuncSubstituto", draft.getNumfuncSubstituto());
      putIntOrNull(draftNode, "numvincSubstituto", draft.getNumvincSubstituto());
      putLongOrNull(draftNode, "documentoLegalId", draft.getDocumentoLegalId());
      putLongOrNull(draftNode, "atoPublicacaoId", draft.getAtoPublicacaoId());
      putTextOrNull(draftNode, "situacao", draft.getSituacao());
    }
    root.set("draft", draftNode);

    root.set("vinculo", toVinculoNode(ctx.vinculo(), empCodigo));
    root.set("pessoa", toPessoaNode(ctx.pessoa()));
    root.set(
        "tipoFrequencia",
        toTipoFrequenciaNode(
            draft != null ? draft.getTipoFrequenciaEntidade() : null, ctx.codigoFrequencia()));
    root.set("codigoFrequencia", toCodigoFrequenciaNode(ctx.codigoFrequencia()));
    root.set(
        "documentoLegal",
        toDocumentoLegalNode(
            draft != null ? draft.getDocumentoLegal() : null,
            draft != null ? draft.getDocumentoLegalId() : null,
            empCodigo));
    root.set(
        "afastamentoOrigem",
        toAfastamentoOrigemNode(draft, ctx.afastamentoOrigem()));
    root.set(
        "substitutoVinculo",
        toSubstitutoVinculoNode(ctx.substitutoVinculo(), empCodigo));

    LocalDate eventStart = draft != null ? draft.getDtini() : null;
    LocalDate eventEnd = resolveAfastamentoEndDate(draft);

    root.set(
        "effectiveStatutoryRule",
        buildAfastamentoStatutoryRuleNode(
            ctx.effectiveStatutoryRule(),
            draft,
            durationDays != null ? durationDays : 1,
            eventStart,
            eventEnd,
            ctx.existingAfastamentos(),
            ctx.activeFrequencias(),
            ctx.afastamentoOrigem()));

    ArrayNode existingArray = mapper.createArrayNode();
    if (ctx.existingAfastamentos() != null) {
      for (Afastamento existing : ctx.existingAfastamentos()) {
        if (existing != null) {
          existingArray.add(toAfastamentoSummaryNode(existing));
        }
      }
    }
    root.set("existingAfastamentos", existingArray);

    ArrayNode activeFreqArray = mapper.createArrayNode();
    if (ctx.activeFrequencias() != null) {
      for (Frequencia freq : ctx.activeFrequencias()) {
        if (freq != null) {
          ObjectNode item = mapper.createObjectNode();
          putLongOrNull(item, "id", freq.getId());
          putLongOrNull(item, "numfunc", freq.getNumfunc());
          putIntOrNull(item, "numvinc", freq.getNumvinc());
          putTextOrNull(item, "tipofreq", freq.getTipofreq());
          putIntOrNull(item, "codfreq", freq.getCodfreq());
          putDateOrNull(item, "dtini", freq.getDtini());
          putDateOrNull(item, "dtfim", freq.getDtfim());
          putTextOrNull(item, "situacao", freq.getSituacao());
          activeFreqArray.add(item);
        }
      }
    }
    root.set("activeFrequencias", activeFreqArray);

    ObjectNode tenantNode = mapper.createObjectNode();
    tenantNode.put("empCodigo", empCodigo);
    root.set("tenant", tenantNode);

    ObjectNode contextNode = mapper.createObjectNode();
    contextNode.put("resourceKey", AfastamentoConcessaoRuleSet.RESOURCE_KEY);
    contextNode.put("ruleSetKey", AfastamentoConcessaoRuleSet.RULESET_KEY);
    root.set("context", contextNode);
    return root;
  }

  private record OccurrenceRange(Long id, LocalDate start, LocalDate end, int days) {}

  private ObjectNode buildFrequenciaStatutoryRuleNode(
      RegraFrequenciaEstatutaria rule,
      Frequencia draft,
      int draftDurationDays,
      LocalDate draftStart,
      LocalDate draftEnd,
      List<Frequencia> existingFrequencias,
      List<Afastamento> activeAfastamentos) {
    ObjectNode node = baseStatutoryRuleNode(rule);

    int prorrogWindowDays =
        rule != null && rule.getIntProrrogDias() != null ? rule.getIntProrrogDias() : 0;
    List<OccurrenceRange> peers = new ArrayList<>();
    if (draft != null && draftStart != null && existingFrequencias != null) {
      for (Frequencia f : existingFrequencias) {
        if (f == null || isInactive(f.getSituacao())) {
          continue;
        }
        if (draft.getId() != null && Objects.equals(draft.getId(), f.getId())) {
          continue;
        }
        if (!Objects.equals(draft.getTipofreq(), f.getTipofreq())
            || !Objects.equals(draft.getCodfreq(), f.getCodfreq())) {
          continue;
        }
        LocalDate fStart = f.getDtini();
        if (fStart == null) {
          continue;
        }
        LocalDate fEnd = f.getDtfim() != null ? f.getDtfim() : fStart;
        peers.add(new OccurrenceRange(f.getId(), fStart, fEnd, computeFrequenciaDurationDays(f)));
      }
    }

    LocalDate boundedDraftEnd =
        draftStart != null
            ? (draftEnd != null && !draftEnd.isBefore(draftStart)
                ? draftEnd
                : draftStart.plusDays(Math.max(1, draftDurationDays) - 1L))
            : null;
    populateStatutoryAccumulationMetrics(
        node,
        draft != null ? draft.getId() : null,
        draftStart,
        boundedDraftEnd,
        draftDurationDays,
        prorrogWindowDays,
        peers,
        null);

    int overlappingAfastamentosCount = 0;
    if (draftStart != null && activeAfastamentos != null) {
      LocalDate effectiveDraftEnd = draftEnd != null ? draftEnd : draftStart;
      for (Afastamento a : activeAfastamentos) {
        if (a == null || isInactive(a.getSituacao()) || a.getDtini() == null) {
          continue;
        }
        LocalDate aEnd = resolveAfastamentoEndDate(a);
        if (!draftStart.isAfter(aEnd) && !a.getDtini().isAfter(effectiveDraftEnd)) {
          overlappingAfastamentosCount++;
        }
      }
    }
    node.put("overlappingAfastamentosCount", overlappingAfastamentosCount);
    return node;
  }

  private ObjectNode buildAfastamentoStatutoryRuleNode(
      RegraFrequenciaEstatutaria rule,
      Afastamento draft,
      int draftDurationDays,
      LocalDate draftStart,
      LocalDate draftEnd,
      List<Afastamento> existingAfastamentos,
      List<Frequencia> activeFrequencias,
      Afastamento afastamentoOrigem) {
    ObjectNode node = baseStatutoryRuleNode(rule);

    int prorrogWindowDays =
        rule != null && rule.getIntProrrogDias() != null ? rule.getIntProrrogDias() : 0;
    List<OccurrenceRange> peers = new ArrayList<>();
    if (draft != null && draftStart != null && existingAfastamentos != null) {
      for (Afastamento a : existingAfastamentos) {
        if (a == null || isInactive(a.getSituacao())) {
          continue;
        }
        if (draft.getId() != null && Objects.equals(draft.getId(), a.getId())) {
          continue;
        }
        if (!Objects.equals(draft.getTipofreq(), a.getTipofreq())
            || !Objects.equals(draft.getCodfreq(), a.getCodfreq())) {
          continue;
        }
        LocalDate aStart = a.getDtini();
        if (aStart == null) {
          continue;
        }
        Integer aDaysObj = computeAfastamentoDurationDays(a);
        int aDays = aDaysObj != null ? aDaysObj : 1;
        LocalDate aEnd =
            a.getDtfim() != null
                ? a.getDtfim()
                : (a.getDtprevfim() != null
                    ? a.getDtprevfim()
                    : aStart.plusDays(Math.max(1, aDays) - 1L));
        peers.add(new OccurrenceRange(a.getId(), aStart, aEnd, aDays));
      }
    }

    OccurrenceRange explicitOriginRange = null;
    if (afastamentoOrigem != null
        && !isInactive(afastamentoOrigem.getSituacao())
        && afastamentoOrigem.getDtini() != null
        && (draft == null || !Objects.equals(draft.getId(), afastamentoOrigem.getId()))) {
      Integer origDaysObj = computeAfastamentoDurationDays(afastamentoOrigem);
      int origDays = origDaysObj != null ? origDaysObj : 1;
      LocalDate origEnd =
          afastamentoOrigem.getDtfim() != null
              ? afastamentoOrigem.getDtfim()
              : (afastamentoOrigem.getDtprevfim() != null
                  ? afastamentoOrigem.getDtprevfim()
                  : afastamentoOrigem.getDtini().plusDays(Math.max(1, origDays) - 1L));
      explicitOriginRange =
          new OccurrenceRange(
              afastamentoOrigem.getId(), afastamentoOrigem.getDtini(), origEnd, origDays);
    }

    LocalDate boundedDraftEnd = null;
    if (draftStart != null) {
      if (draftEnd != null
          && !draftEnd.equals(OPEN_ENDED_DATE)
          && !draftEnd.isBefore(draftStart)) {
        boundedDraftEnd = draftEnd;
      } else {
        boundedDraftEnd = draftStart.plusDays(Math.max(1, draftDurationDays) - 1L);
      }
    }

    populateStatutoryAccumulationMetrics(
        node,
        draft != null ? draft.getId() : null,
        draftStart,
        boundedDraftEnd,
        draftDurationDays,
        prorrogWindowDays,
        peers,
        explicitOriginRange);

    int overlappingFrequenciasCount = 0;
    if (draftStart != null && activeFrequencias != null) {
      LocalDate effectiveDraftEnd = draftEnd != null ? draftEnd : OPEN_ENDED_DATE;
      for (Frequencia f : activeFrequencias) {
        if (f == null || isInactive(f.getSituacao()) || f.getDtini() == null) {
          continue;
        }
        LocalDate fEnd = f.getDtfim() != null ? f.getDtfim() : f.getDtini();
        if (!draftStart.isAfter(fEnd) && !f.getDtini().isAfter(effectiveDraftEnd)) {
          overlappingFrequenciasCount++;
        }
      }
    }
    node.put("overlappingFrequenciasCount", overlappingFrequenciasCount);
    return node;
  }

  private static void populateStatutoryAccumulationMetrics(
      ObjectNode node,
      Long draftId,
      LocalDate draftStart,
      LocalDate boundedDraftEnd,
      int draftDurationDays,
      int prorrogWindowDays,
      List<OccurrenceRange> peers,
      OccurrenceRange explicitOriginRange) {
    if (draftStart == null || boundedDraftEnd == null) {
      node.putNull("daysSincePreviousOccurrence");
      node.put("accumulatedProrrogDays", draftDurationDays);
      node.put("projectedConsecMesDays", draftDurationDays);
      node.put("projectedInterpMesDays", draftDurationDays);
      node.put("projectedConsecAnoDays", draftDurationDays);
      node.put("projectedInterpAnoDays", draftDurationDays);
      return;
    }

    OccurrenceRange draftRange =
        new OccurrenceRange(draftId, draftStart, boundedDraftEnd, draftDurationDays);

    Integer daysSincePrevious = null;
    for (OccurrenceRange p : peers) {
      if (p.end().isBefore(draftStart)) {
        int gap = Math.max(0, Math.toIntExact(ChronoUnit.DAYS.between(p.end(), draftStart) - 1));
        if (daysSincePrevious == null || gap < daysSincePrevious) {
          daysSincePrevious = gap;
        }
      }
    }

    // 1. Cadeia multi-hop de prorrogação (accumulatedProrrogDays)
    List<OccurrenceRange> prorrogChain = new ArrayList<>();
    Set<OccurrenceRange> prorrogVisited = new HashSet<>();
    prorrogChain.add(draftRange);
    prorrogVisited.add(draftRange);
    if (explicitOriginRange != null) {
      OccurrenceRange matchedPeer =
          peers.stream()
              .filter(
                  p ->
                      (explicitOriginRange.id() != null
                              && Objects.equals(explicitOriginRange.id(), p.id()))
                          || (Objects.equals(explicitOriginRange.start(), p.start())
                              && Objects.equals(explicitOriginRange.end(), p.end())))
              .findFirst()
              .orElse(explicitOriginRange);
      if (prorrogVisited.add(matchedPeer)) {
        prorrogChain.add(matchedPeer);
      }
    }
    if (prorrogWindowDays > 0) {
      boolean expanded = true;
      while (expanded) {
        expanded = false;
        for (OccurrenceRange p : peers) {
          if (prorrogVisited.contains(p)) {
            continue;
          }
          for (OccurrenceRange member : List.copyOf(prorrogChain)) {
            if (p.end().isBefore(member.start())) {
              int gap =
                  Math.max(
                      0, Math.toIntExact(ChronoUnit.DAYS.between(p.end(), member.start()) - 1));
              if (gap <= prorrogWindowDays) {
                prorrogVisited.add(p);
                prorrogChain.add(p);
                expanded = true;
                break;
              }
            }
          }
        }
      }
    }
    int accumulatedProrrogDays =
        prorrogChain.stream().mapToInt(OccurrenceRange::days).sum();

    // 2. Cadeia multi-hop de ocorrências consecutivas (gap == 0 dias entre fim e início)
    List<OccurrenceRange> consecCluster = new ArrayList<>();
    Set<OccurrenceRange> consecVisited = new HashSet<>();
    consecCluster.add(draftRange);
    consecVisited.add(draftRange);
    boolean clusterExpanded = true;
    while (clusterExpanded) {
      clusterExpanded = false;
      for (OccurrenceRange p : peers) {
        if (consecVisited.contains(p)) {
          continue;
        }
        for (OccurrenceRange member : List.copyOf(consecCluster)) {
          if (areContiguousOrOverlapping(p, member)) {
            consecVisited.add(p);
            consecCluster.add(p);
            clusterExpanded = true;
            break;
          }
        }
      }
    }

    List<OccurrenceRange> allRanges = new ArrayList<>(peers.size() + 1);
    allRanges.add(draftRange);
    allRanges.addAll(peers);

    // 3. Limites mensais por mês-calendário tocado pelo rascunho
    YearMonth startMonth = YearMonth.from(draftStart);
    YearMonth endMonth = YearMonth.from(boundedDraftEnd);
    int maxConsecMes = 0;
    int maxInterpMes = 0;
    for (YearMonth ym = startMonth; !ym.isAfter(endMonth); ym = ym.plusMonths(1)) {
      int monthConsec = 0;
      for (OccurrenceRange r : consecCluster) {
        monthConsec += daysInYearMonth(r, ym);
      }
      int monthInterp = 0;
      for (OccurrenceRange r : allRanges) {
        monthInterp += daysInYearMonth(r, ym);
      }
      if (monthConsec > maxConsecMes) {
        maxConsecMes = monthConsec;
      }
      if (monthInterp > maxInterpMes) {
        maxInterpMes = monthInterp;
      }
    }

    // 4. Limites anuais por ano-calendário tocado pelo rascunho
    int startYear = draftStart.getYear();
    int endYear = boundedDraftEnd.getYear();
    int maxConsecAno = 0;
    int maxInterpAno = 0;
    for (int year = startYear; year <= endYear; year++) {
      int yearConsec = 0;
      for (OccurrenceRange r : consecCluster) {
        yearConsec += daysInYear(r, year);
      }
      int yearInterp = 0;
      for (OccurrenceRange r : allRanges) {
        yearInterp += daysInYear(r, year);
      }
      if (yearConsec > maxConsecAno) {
        maxConsecAno = yearConsec;
      }
      if (yearInterp > maxInterpAno) {
        maxInterpAno = yearInterp;
      }
    }

    putIntOrNull(node, "daysSincePreviousOccurrence", daysSincePrevious);
    node.put("accumulatedProrrogDays", accumulatedProrrogDays);
    node.put("projectedConsecMesDays", maxConsecMes);
    node.put("projectedInterpMesDays", maxInterpMes);
    node.put("projectedConsecAnoDays", maxConsecAno);
    node.put("projectedInterpAnoDays", maxInterpAno);
  }

  private static boolean areContiguousOrOverlapping(OccurrenceRange a, OccurrenceRange b) {
    if (a.end().isBefore(b.start())) {
      return ChronoUnit.DAYS.between(a.end(), b.start()) <= 1;
    }
    if (b.end().isBefore(a.start())) {
      return ChronoUnit.DAYS.between(b.end(), a.start()) <= 1;
    }
    return true;
  }

  private static int daysInYearMonth(OccurrenceRange r, YearMonth ym) {
    LocalDate mStart = ym.atDay(1);
    LocalDate mEnd = ym.atEndOfMonth();
    if (r.end().isBefore(mStart) || r.start().isAfter(mEnd)) {
      return 0;
    }
    if (!r.start().isBefore(mStart) && !r.end().isAfter(mEnd)) {
      return r.days();
    }
    LocalDate oStart = r.start().isAfter(mStart) ? r.start() : mStart;
    LocalDate oEnd = r.end().isBefore(mEnd) ? r.end() : mEnd;
    return Math.toIntExact(ChronoUnit.DAYS.between(oStart, oEnd) + 1);
  }

  private static int daysInYear(OccurrenceRange r, int year) {
    LocalDate yStart = LocalDate.of(year, 1, 1);
    LocalDate yEnd = LocalDate.of(year, 12, 31);
    if (r.end().isBefore(yStart) || r.start().isAfter(yEnd)) {
      return 0;
    }
    if (!r.start().isBefore(yStart) && !r.end().isAfter(yEnd)) {
      return r.days();
    }
    LocalDate oStart = r.start().isAfter(yStart) ? r.start() : yStart;
    LocalDate oEnd = r.end().isBefore(yEnd) ? r.end() : yEnd;
    return Math.toIntExact(ChronoUnit.DAYS.between(oStart, oEnd) + 1);
  }

  private ObjectNode baseStatutoryRuleNode(RegraFrequenciaEstatutaria rule) {
    ObjectNode node = mapper.createObjectNode();
    if (rule == null) {
      node.put("present", false);
      node.putNull("id");
      node.putNull("ruleKey");
      node.put("naoSeAplica", false);
      node.putNull("qtdMinDias");
      node.putNull("qtdMaxDias");
      node.putNull("intervMinDias");
      node.putNull("intProrrogDias");
      node.putNull("qtdMaxConsecMes");
      node.putNull("qtdMaxInterpMes");
      node.putNull("qtdMaxConsecAno");
      node.putNull("qtdMaxInterpAno");
      node.put("exigeHomologacao", false);
      node.put("permiteConcomitancia", false);
      node.put("exigeDocumentoLegal", false);
      node.put("prorrogacaoPermitida", true);
      node.putNull("documentoLegalId");
      return node;
    }

    node.put("present", true);
    putLongOrNull(node, "id", rule.getId());
    putTextOrNull(node, "ruleKey", rule.getRuleKey());
    node.put("naoSeAplica", Boolean.TRUE.equals(rule.getNaoSeAplica()));
    if (rule.getQtdMinDias() != null) {
      node.put("qtdMinDias", rule.getQtdMinDias().stripTrailingZeros());
    } else {
      node.putNull("qtdMinDias");
    }
    if (rule.getQtdMaxDias() != null) {
      node.put("qtdMaxDias", rule.getQtdMaxDias().stripTrailingZeros());
    } else {
      node.putNull("qtdMaxDias");
    }
    putIntOrNull(node, "intervMinDias", rule.getIntervMinDias());
    putIntOrNull(node, "intProrrogDias", rule.getIntProrrogDias());
    putIntOrNull(node, "qtdMaxConsecMes", rule.getQtdMaxConsecMes());
    putIntOrNull(node, "qtdMaxInterpMes", rule.getQtdMaxInterpMes());
    putIntOrNull(node, "qtdMaxConsecAno", rule.getQtdMaxConsecAno());
    putIntOrNull(node, "qtdMaxInterpAno", rule.getQtdMaxInterpAno());
    node.put("exigeHomologacao", Boolean.TRUE.equals(rule.getExigeHomologacao()));
    node.put("permiteConcomitancia", Boolean.TRUE.equals(rule.getPermiteConcomitancia()));

    boolean exigeDoc =
        rule.getDocumentoLegalId() != null
            || (rule.getRuleParams() != null
                && ("S".equalsIgnoreCase(String.valueOf(rule.getRuleParams().get("pontlei")))
                    || Boolean.TRUE.equals(rule.getRuleParams().get("exigeDocumentoLegal"))));
    node.put("exigeDocumentoLegal", exigeDoc);

    boolean prorrogPermitida =
        rule.getRuleParams() == null
            || !"N".equalsIgnoreCase(String.valueOf(rule.getRuleParams().get("prorrogacao")));
    node.put("prorrogacaoPermitida", prorrogPermitida);
    putLongOrNull(node, "documentoLegalId", rule.getDocumentoLegalId());
    return node;
  }

  private ObjectNode toVinculoNode(Vinculo vinculo, Integer expectedEmpCodigo) {
    ObjectNode node = mapper.createObjectNode();
    if (vinculo == null) {
      node.put("present", false);
      node.put("sameOrgao", false);
      node.putNull("dtexerc");
      node.putNull("dtvac");
      node.putNull("dtaposent");
      node.putNull("situacao");
      return node;
    }
    node.put("present", true);
    node.put(
        "sameOrgao",
        expectedEmpCodigo == null || Objects.equals(vinculo.getEmpCodigo(), expectedEmpCodigo));
    putDateOrNull(node, "dtexerc", vinculo.getDtexerc());
    putDateOrNull(node, "dtvac", vinculo.getDtvac());
    putDateOrNull(node, "dtaposent", vinculo.getDtaposent());
    putTextOrNull(node, "situacao", vinculo.getSituacao());
    putTextOrNull(node, "regimejur", vinculo.getRegimejur());
    putTextOrNull(node, "tipovinc", vinculo.getTipovinc());
    putTextOrNull(node, "categoria", vinculo.getCategoria());
    putTextOrNull(node, "subcategoria", vinculo.getSubcategoria());
    return node;
  }

  private ObjectNode toPessoaNode(ServidorPessoa pessoa) {
    ObjectNode node = mapper.createObjectNode();
    if (pessoa == null) {
      node.put("present", false);
      node.putNull("sexo");
      return node;
    }
    node.put("present", true);
    putTextOrNull(node, "sexo", pessoa.getSexo());
    return node;
  }

  private ObjectNode toTipoFrequenciaNode(TipoFrequencia tipoFreq, CodigoFrequencia codigo) {
    ObjectNode node = mapper.createObjectNode();
    TipoFrequencia resolved =
        tipoFreq != null
            ? tipoFreq
            : (codigo != null ? codigo.getTipoFrequenciaEntidade() : null);
    if (resolved == null) {
      node.put("present", false);
      node.putNull("tipo");
      node.putNull("nome");
      node.put("influiConta", false);
      node.put("horaExtra", false);
      return node;
    }
    node.put("present", true);
    putTextOrNull(node, "tipo", resolved.getTipo());
    putTextOrNull(node, "nome", resolved.getNome());
    node.put("influiConta", Boolean.TRUE.equals(resolved.getInfluiConta()));
    node.put("horaExtra", Boolean.TRUE.equals(resolved.getHoraExtra()));
    return node;
  }

  private ObjectNode toDocumentoLegalNode(
      DocumentoLegal doc, Long documentoLegalId, Integer expectedEmpCodigo) {
    ObjectNode node = mapper.createObjectNode();
    if (doc == null) {
      node.put("present", false);
      putLongOrNull(node, "id", documentoLegalId);
      node.putNull("empCodigo");
      node.put("sameOrgao", documentoLegalId != null);
      node.putNull("tipoDocumento");
      return node;
    }
    node.put("present", true);
    putLongOrNull(node, "id", doc.getId());
    putIntOrNull(node, "empCodigo", doc.getEmpCodigo());
    node.put(
        "sameOrgao",
        expectedEmpCodigo == null || Objects.equals(doc.getEmpCodigo(), expectedEmpCodigo));
    putTextOrNull(node, "tipoDocumento", doc.getTipoDocumento());
    return node;
  }

  private ObjectNode toCodigoFrequenciaNode(CodigoFrequencia codigo) {
    ObjectNode node = mapper.createObjectNode();
    if (codigo == null) {
      node.put("present", false);
      node.putNull("tipofreq");
      node.putNull("codigo");
      node.put("viaFrequencia", false);
      node.put("afastamento", false);
      node.put("horaEntradaSaida", false);
      node.put("regrasFreq", false);
      node.putNull("sexo");
      node.putNull("qtdmin");
      node.putNull("qtdmax");
      return node;
    }
    node.put("present", true);
    putTextOrNull(
        node, "tipofreq", codigo.getId() != null ? codigo.getId().tipofreq() : null);
    putIntOrNull(node, "codigo", codigo.getId() != null ? codigo.getId().codigo() : null);
    node.put("viaFrequencia", Boolean.TRUE.equals(codigo.getViaFrequencia()));
    node.put("afastamento", Boolean.TRUE.equals(codigo.getAfastamento()));
    node.put("horaEntradaSaida", Boolean.TRUE.equals(codigo.getHoraEntradaSaida()));
    node.put("regrasFreq", Boolean.TRUE.equals(codigo.getRegrasFreq()));
    putTextOrNull(node, "sexo", codigo.getSexo());
    if (codigo.getQtdmin() != null) {
      node.put("qtdmin", codigo.getQtdmin().stripTrailingZeros());
    } else {
      node.putNull("qtdmin");
    }
    if (codigo.getQtdmax() != null) {
      node.put("qtdmax", codigo.getQtdmax().stripTrailingZeros());
    } else {
      node.putNull("qtdmax");
    }
    return node;
  }

  private ObjectNode toAfastamentoOrigemNode(Afastamento draft, Afastamento origem) {
    ObjectNode node = mapper.createObjectNode();
    if (origem == null) {
      node.put("present", false);
      node.put("sameVinculo", false);
      node.put("selfReference", false);
      node.put("activeOrigin", false);
      node.put("chronologicalAfterOrigin", false);
      node.putNull("gapDays");
      return node;
    }
    node.put("present", true);
    boolean sameVinculo =
        draft != null
            && Objects.equals(origem.getNumfunc(), draft.getNumfunc())
            && Objects.equals(origem.getNumvinc(), draft.getNumvinc());
    boolean selfRef =
        draft != null
            && draft.getId() != null
            && Objects.equals(draft.getId(), origem.getId());
    boolean activeOrigin = !isInactive(origem.getSituacao());
    LocalDate origEnd =
        origem.getDtfim() != null
            ? origem.getDtfim()
            : (origem.getDtprevfim() != null ? origem.getDtprevfim() : origem.getDtini());
    boolean chronologicalAfterOrigin =
        draft != null
            && draft.getDtini() != null
            && origEnd != null
            && draft.getDtini().isAfter(origEnd);

    node.put("sameVinculo", sameVinculo);
    node.put("selfReference", selfRef);
    node.put("activeOrigin", activeOrigin);
    node.put("chronologicalAfterOrigin", chronologicalAfterOrigin);

    Integer gapDays = null;
    if (chronologicalAfterOrigin) {
      gapDays = Math.max(0, Math.toIntExact(ChronoUnit.DAYS.between(origEnd, draft.getDtini()) - 1));
    }
    putIntOrNull(node, "gapDays", gapDays);
    return node;
  }

  private ObjectNode toSubstitutoVinculoNode(Vinculo substituto, Integer expectedEmpCodigo) {
    ObjectNode node = mapper.createObjectNode();
    if (substituto == null) {
      node.put("present", false);
      node.put("sameOrgao", false);
      node.put("ativo", false);
      return node;
    }
    node.put("present", true);
    node.put(
        "sameOrgao",
        expectedEmpCodigo == null || Objects.equals(substituto.getEmpCodigo(), expectedEmpCodigo));
    node.put("ativo", "ATIVO".equals(substituto.getSituacao()));
    return node;
  }

  private ObjectNode toAfastamentoSummaryNode(Afastamento a) {
    ObjectNode item = mapper.createObjectNode();
    putLongOrNull(item, "id", a.getId());
    putLongOrNull(item, "numfunc", a.getNumfunc());
    putIntOrNull(item, "numvinc", a.getNumvinc());
    putTextOrNull(item, "tipofreq", a.getTipofreq());
    putIntOrNull(item, "codfreq", a.getCodfreq());
    putDateOrNull(item, "dtini", a.getDtini());
    putDateOrNull(item, "dtprevfim", a.getDtprevfim());
    putDateOrNull(item, "dtfim", a.getDtfim());
    putTextOrNull(item, "situacao", a.getSituacao());
    return item;
  }

  private ObjectNode toDraftNode(RegraFrequenciaEstatutaria entity) {
    ObjectNode node = mapper.createObjectNode();
    if (entity == null) {
      return node;
    }
    putLongOrNull(node, "id", entity.getId());
    putIntOrNull(node, "empCodigo", entity.getEmpCodigo());
    putTextOrNull(node, "regimeJuridico", entity.getRegimeJuridico());
    putTextOrNull(node, "tipoVinculo", entity.getTipoVinculo());
    putTextOrNull(node, "categoria", entity.getCategoria());
    putTextOrNull(node, "subcategoria", entity.getSubcategoria());
    putTextOrNull(node, "tipofreq", entity.getTipofreq());
    putIntOrNull(node, "codfreq", entity.getCodfreq());
    if (entity.getDtini() != null) {
      node.put("dtini", entity.getDtini().toString());
    }
    putDateOrNull(node, "dtfim", entity.getDtfim());

    if (entity.getQtdMinDias() != null) {
      node.put("qtdMinDias", entity.getQtdMinDias().stripTrailingZeros());
    } else {
      node.putNull("qtdMinDias");
    }
    if (entity.getQtdMaxDias() != null) {
      node.put("qtdMaxDias", entity.getQtdMaxDias().stripTrailingZeros());
    } else {
      node.putNull("qtdMaxDias");
    }

    putIntOrNull(node, "intervMinDias", entity.getIntervMinDias());
    putIntOrNull(node, "intProrrogDias", entity.getIntProrrogDias());
    putIntOrNull(node, "qtdMaxConsecMes", entity.getQtdMaxConsecMes());
    putIntOrNull(node, "qtdMaxInterpMes", entity.getQtdMaxInterpMes());
    putIntOrNull(node, "qtdMaxConsecAno", entity.getQtdMaxConsecAno());
    putIntOrNull(node, "qtdMaxInterpAno", entity.getQtdMaxInterpAno());
    putBooleanOrNull(node, "exigeHomologacao", entity.getExigeHomologacao());
    putBooleanOrNull(node, "permiteConcomitancia", entity.getPermiteConcomitancia());
    putBooleanOrNull(node, "naoSeAplica", entity.getNaoSeAplica());
    putBooleanOrNull(node, "ativa", entity.getAtiva());
    putLongOrNull(node, "documentoLegalId", entity.getDocumentoLegalId());
    putTextOrNull(node, "ruleKey", entity.getRuleKey());

    ObjectNode paramsNode = mapper.createObjectNode();
    paramsNode.putNull("prorrogacao");
    paramsNode.putNull("prazo");
    paramsNode.putNull("tipoUnidadePrazo");
    paramsNode.putNull("intersticio");
    paramsNode.putNull("tipoUnidadeInterst");
    paramsNode.putNull("tipoUnidade");
    paramsNode.putNull("pontlei");
    if (entity.getRuleParams() != null) {
      entity.getRuleParams().forEach((k, v) -> paramsNode.set(k, mapper.valueToTree(v)));
    }
    node.set("ruleParams", paramsNode);
    return node;
  }

  /**
   * Constrói a árvore JSON imutável de fatos para {@link DependenteElegibilidadeRuleSet}.
   */
  public ObjectNode buildDependenteFacts(DependenteRuleContext ctx) {
    Objects.requireNonNull(ctx, "ctx must not be null");
    Dependente draft = ctx.draft();
    Integer empCodigo = resolveDependenteEmpCodigo(ctx);
    LocalDate refDate = ctx.referenceDate() != null ? ctx.referenceDate() : LocalDate.now();

    ObjectNode root = mapper.createObjectNode();
    ObjectNode draftNode = mapper.createObjectNode();
    if (draft != null) {
      Long numfunc = draft.getId() != null ? draft.getId().numfunc() : null;
      Integer numdep = draft.getId() != null ? draft.getId().numdep() : null;
      putIntOrNull(draftNode, "id", numdep);
      putIntOrNull(draftNode, "empCodigo", empCodigo);
      putLongOrNull(draftNode, "numfunc", numfunc);
      putIntOrNull(draftNode, "numero", numdep);
      putIntOrNull(draftNode, "numdep", numdep);
      putTextOrNull(draftNode, "nome", draft.getNome());
      putDateOrNull(draftNode, "dtnasc", draft.getDtnasc());
      putTextOrNull(draftNode, "sexo", draft.getSexo());
      String cleanCpf = blankToNull(draft.getCpf());
      putTextOrNull(draftNode, "cpf", cleanCpf);
      draftNode.put("cpfValidFormat", cleanCpf != null && cleanCpf.matches("\\d{11}"));
      putTextOrNull(draftNode, "parentesco", blankToNull(draft.getParentesco()));
      putTextOrNull(draftNode, "tipodepen", blankToNull(draft.getTipodepen()));
      putDateOrNull(draftNode, "dtini", draft.getDtini());
      putDateOrNull(draftNode, "dtfim", draft.getDtfim());
      draftNode.put("depIr", Boolean.TRUE.equals(draft.getDepIr()));
      draftNode.put("depSalarioFamilia", Boolean.TRUE.equals(draft.getDepSalarioFamilia()));
      draftNode.put("depPlanoSaude", Boolean.TRUE.equals(draft.getDepPlanoSaude()));
      draftNode.put("estudante", Boolean.TRUE.equals(draft.getEstudante()));
      draftNode.put("universitario", Boolean.TRUE.equals(draft.getUniversitario()));
      draftNode.put("invalido", Boolean.TRUE.equals(draft.getInvalido()));
      draftNode.put("excepcional", Boolean.TRUE.equals(draft.getExcepcional()));
      draftNode.put("representanteLegal", Boolean.TRUE.equals(draft.getRepresentanteLegal()));
      putLongOrNull(draftNode, "documentoLegalId", draft.getDocumentoLegalId());
      putTextOrNull(draftNode, "numrg", blankToNull(draft.getNumrg()));
      putTextOrNull(draftNode, "tiporg", blankToNull(draft.getTiporg()));
      putTextOrNull(draftNode, "orgaorg", blankToNull(draft.getOrgaorg()));
      putTextOrNull(draftNode, "ufrg", blankToNull(draft.getUfrg()));
      putDateOrNull(draftNode, "expedrg", draft.getExpedrg());
      putTextOrNull(draftNode, "numCert", blankToNull(draft.getNumCert()));
      putTextOrNull(draftNode, "livroCert", blankToNull(draft.getLivroACert()));
      putTextOrNull(draftNode, "folhaCert", blankToNull(draft.getFolhaCert()));
      putTextOrNull(draftNode, "cartorioCert", blankToNull(draft.getCartorioCert()));
      putTextOrNull(draftNode, "tipodocCert", blankToNull(draft.getTipodocCert()));
      draftNode.put("referenceDate", refDate.toString());
      if (draft.getDtnasc() != null) {
        int age =
            draft.getDtnasc().isAfter(refDate)
                ? -1
                : Math.toIntExact(ChronoUnit.YEARS.between(draft.getDtnasc(), refDate));
        draftNode.put("ageYears", age);
      } else {
        draftNode.putNull("ageYears");
      }
    }
    root.set("draft", draftNode);

    ObjectNode pessoaNode = mapper.createObjectNode();
    if (ctx.pessoa() == null) {
      pessoaNode.put("present", false);
      pessoaNode.putNull("id");
      pessoaNode.putNull("cpf");
      pessoaNode.putNull("dtnasc");
      pessoaNode.putNull("sexo");
    } else {
      pessoaNode.put("present", true);
      putLongOrNull(pessoaNode, "id", ctx.pessoa().getNumero());
      putTextOrNull(pessoaNode, "cpf", blankToNull(ctx.pessoa().getCpf()));
      putDateOrNull(pessoaNode, "dtnasc", ctx.pessoa().getDtnasc());
      putTextOrNull(pessoaNode, "sexo", ctx.pessoa().getSexo());
    }
    root.set("pessoa", pessoaNode);

    ObjectNode tipoNode = mapper.createObjectNode();
    if (ctx.tipoDependente() == null) {
      tipoNode.put("present", false);
      tipoNode.putNull("codigo");
      tipoNode.put("geraIr", false);
      tipoNode.put("geraSalarioFamilia", false);
      tipoNode.putNull("effectiveMaxAge");
    } else {
      tipoNode.put("present", true);
      putTextOrNull(tipoNode, "codigo", ctx.tipoDependente().getTipodepen());
      tipoNode.put("geraIr", Boolean.TRUE.equals(ctx.tipoDependente().getGeraIr()));
      tipoNode.put(
          "geraSalarioFamilia", Boolean.TRUE.equals(ctx.tipoDependente().getGeraSalarioFamilia()));
      Integer effectiveMaxAge =
          draft != null
                  && Boolean.TRUE.equals(draft.getUniversitario())
                  && ctx.tipoDependente().getIdadeLimite() != null
                  && ctx.tipoDependente().getIdadeLimite() < 24
              ? Integer.valueOf(24)
              : ctx.tipoDependente().getIdadeLimite();
      putIntOrNull(tipoNode, "effectiveMaxAge", effectiveMaxAge);
    }
    root.set("tipoDependente", tipoNode);

    root.set(
        "documentoLegal",
        toDocumentoLegalNode(
            ctx.documentoLegal(),
            draft != null ? draft.getDocumentoLegalId() : null,
            empCodigo));

    ArrayNode existingArray = mapper.createArrayNode();
    if (ctx.existingDependentes() != null) {
      for (Dependente existing : ctx.existingDependentes()) {
        if (existing != null) {
          ObjectNode item = mapper.createObjectNode();
          Integer exNumdep = existing.getId() != null ? existing.getId().numdep() : null;
          Long exNumfunc = existing.getId() != null ? existing.getId().numfunc() : null;
          putIntOrNull(item, "id", exNumdep);
          putLongOrNull(item, "numfunc", exNumfunc);
          putIntOrNull(item, "numero", exNumdep);
          putTextOrNull(item, "cpf", blankToNull(existing.getCpf()));
          putDateOrNull(item, "dtini", existing.getDtini());
          putDateOrNull(item, "dtfim", existing.getDtfim());
          existingArray.add(item);
        }
      }
    }
    root.set("existingDependentes", existingArray);

    ObjectNode tenantNode = mapper.createObjectNode();
    tenantNode.put("empCodigo", empCodigo);
    root.set("tenant", tenantNode);

    ObjectNode contextNode = mapper.createObjectNode();
    contextNode.put("resourceKey", DependenteElegibilidadeRuleSet.RESOURCE_KEY);
    contextNode.put("ruleSetKey", DependenteElegibilidadeRuleSet.RULESET_KEY);
    contextNode.put("referenceDate", refDate.toString());
    root.set("context", contextNode);
    return root;
  }

  /**
   * Constrói a árvore JSON imutável de fatos para {@link VinculoProvimentoRuleSet}.
   */
  public ObjectNode buildVinculoFacts(VinculoRuleContext ctx) {
    Objects.requireNonNull(ctx, "ctx must not be null");
    Vinculo draft = ctx.draft();
    Integer empCodigo = draft != null && draft.getEmpCodigo() != null ? draft.getEmpCodigo() : 1;

    ObjectNode root = mapper.createObjectNode();
    ObjectNode draftNode = mapper.createObjectNode();
    if (draft != null) {
      Long numfunc = draft.getId() != null ? draft.getId().numfunc() : null;
      Integer numero = draft.getId() != null ? draft.getId().numero() : null;
      putIntOrNull(draftNode, "id", numero);
      putIntOrNull(draftNode, "empCodigo", draft.getEmpCodigo());
      putLongOrNull(draftNode, "numfunc", numfunc);
      putIntOrNull(draftNode, "numero", numero);
      putTextOrNull(draftNode, "regimejur", blankToNull(draft.getRegimejur()));
      putTextOrNull(draftNode, "tipovinc", blankToNull(draft.getTipovinc()));
      putTextOrNull(draftNode, "categoria", blankToNull(draft.getCategoria()));
      putTextOrNull(draftNode, "subcategoria", blankToNull(draft.getSubcategoria()));
      putTextOrNull(draftNode, "situacao", blankToNull(draft.getSituacao()));
      draftNode.put("creating", ctx.creating());
      draftNode.put(
          "occupyingPosition",
          draft.getSituacao() != null
              && Set.of("ATIVO", "AFASTADO", "CEDIDO").contains(draft.getSituacao()));

      putDateOrNull(draftNode, "dtnom", draft.getDtnom());
      putDateOrNull(draftNode, "dtposse", draft.getDtposse());
      putDateOrNull(draftNode, "dtexerc", draft.getDtexerc());
      putDateOrNull(draftNode, "dtvac", draft.getDtvac());
      putTextOrNull(draftNode, "formavac", blankToNull(draft.getFormavac()));
      putTextOrNull(draftNode, "motivovac", blankToNull(draft.getMotivovac()));
      putDateOrNull(draftNode, "dtaposent", draft.getDtaposent());
      putTextOrNull(draftNode, "tipoapos", blankToNull(draft.getTipoapos()));

      putDateOrNull(draftNode, "dtinicontr", draft.getDtinicontr());
      putDateOrNull(draftNode, "dtfimcontr", draft.getDtfimcontr());
      putDateOrNull(draftNode, "dtprorrogcontr", draft.getDtprorrogcontr());
      if (draft.getDtinicontr() != null && draft.getDtfimcontr() != null) {
        draftNode.put(
            "contractDurationDays",
            Math.toIntExact(ChronoUnit.DAYS.between(draft.getDtinicontr(), draft.getDtfimcontr())));
      } else {
        draftNode.putNull("contractDurationDays");
      }

      putDateOrNull(draftNode, "dtHomologEstagio", draft.getDtHomologEstagio());
      draftNode.putNull("dtinihomologestag");
      draftNode.putNull("dtfimhomologestag");

      putDateOrNull(draftNode, "dtiniCessao", draft.getDtiniCessao());
      putDateOrNull(draftNode, "dtfimCessao", draft.getDtfimCessao());
      putTextOrNull(draftNode, "tipoReq", blankToNull(draft.getTipoReq()));
      putTextOrNull(draftNode, "orgaoReq", blankToNull(draft.getOrgaoReq()));

      draftNode.putNull("dtinipagto");
      draftNode.putNull("dtfimpagto");
      putDateOrNull(draftNode, "dtPgtoAte", draft.getDtPgtoAte());

      putIntOrNull(draftNode, "jornadaSemanal", draft.getJornadaSemanal());
      if (draft.getDtposse() != null && draft.getDtexerc() != null) {
        draftNode.put(
            "daysPosseToExercicio",
            Math.toIntExact(ChronoUnit.DAYS.between(draft.getDtposse(), draft.getDtexerc())));
      } else {
        draftNode.putNull("daysPosseToExercicio");
      }

      LocalDate entryDate = draft.getDtposse() != null ? draft.getDtposse() : draft.getDtexerc();
      if (ctx.pessoa() != null && ctx.pessoa().getDtnasc() != null && entryDate != null) {
        int age =
            entryDate.isBefore(ctx.pessoa().getDtnasc())
                ? -1
                : Math.toIntExact(ChronoUnit.YEARS.between(ctx.pessoa().getDtnasc(), entryDate));
        draftNode.put("ageAtIngressoYears", age);
      } else {
        draftNode.putNull("ageAtIngressoYears");
      }

      putIntOrNull(draftNode, "cargo", draft.getCargo());
      putIntOrNull(draftNode, "cargoComissao", draft.getCargoComissao());
      putTextOrNull(draftNode, "setor", blankToNull(draft.getSetor()));
      putTextOrNull(draftNode, "setorExercicio", blankToNull(draft.getSetorExercicio()));
      putLongOrNull(draftNode, "documentoLegalId", draft.getDocumentoLegalId());
      draftNode.put("openAfastamentosCount", ctx.openAfastamentosCount());
    }
    root.set("draft", draftNode);

    ObjectNode orgaoNode = mapper.createObjectNode();
    if (ctx.orgao() == null) {
      orgaoNode.put("present", false);
      orgaoNode.put("ativa", false);
    } else {
      orgaoNode.put("present", true);
      orgaoNode.put("ativa", Boolean.TRUE.equals(ctx.orgao().getAtiva()));
    }
    root.set("orgao", orgaoNode);

    ObjectNode pessoaNode = mapper.createObjectNode();
    if (ctx.pessoa() == null) {
      pessoaNode.put("present", false);
      pessoaNode.putNull("id");
      pessoaNode.putNull("dtnasc");
    } else {
      pessoaNode.put("present", true);
      putLongOrNull(pessoaNode, "id", ctx.pessoa().getNumero());
      putDateOrNull(pessoaNode, "dtnasc", ctx.pessoa().getDtnasc());
    }
    root.set("pessoa", pessoaNode);

    ObjectNode setorNode = mapper.createObjectNode();
    setorNode.put("present", ctx.setor() != null);
    setorNode.put(
        "sameOrgao",
        ctx.setor() != null && Objects.equals(ctx.setor().getEmpCodigo(), empCodigo));
    root.set("setor", setorNode);

    ObjectNode setorExNode = mapper.createObjectNode();
    setorExNode.put("present", ctx.setorExercicio() != null);
    setorExNode.put(
        "sameOrgao",
        ctx.setorExercicio() != null
            && Objects.equals(ctx.setorExercicio().getEmpCodigo(), empCodigo));
    root.set("setorExercicio", setorExNode);

    root.set(
        "cargo",
        toCargoFactNode(
            ctx.cargo(), ctx.cargoOcupadas(), draft, empCodigo, ctx.creating(), false));
    root.set(
        "cargoComissao",
        toCargoFactNode(
            ctx.cargoComissao(),
            ctx.cargoComissaoOcupadas(),
            draft,
            empCodigo,
            ctx.creating(),
            true));

    root.set(
        "documentoLegal",
        toDocumentoLegalNode(
            ctx.documentoLegal(),
            draft != null ? draft.getDocumentoLegalId() : null,
            empCodigo));

    ArrayNode existingArray = mapper.createArrayNode();
    if (ctx.existingVinculos() != null) {
      for (Vinculo existing : ctx.existingVinculos()) {
        if (existing != null) {
          ObjectNode item = mapper.createObjectNode();
          Integer exNumero = existing.getId() != null ? existing.getId().numero() : null;
          Long exNumfunc = existing.getId() != null ? existing.getId().numfunc() : null;
          putIntOrNull(item, "id", exNumero);
          putLongOrNull(item, "numfunc", exNumfunc);
          putIntOrNull(item, "numero", exNumero);
          putTextOrNull(item, "situacao", existing.getSituacao());
          putTextOrNull(item, "categoria", existing.getCategoria());
          putDateOrNull(item, "dtexerc", existing.getDtexerc());
          putDateOrNull(item, "dtvac", existing.getDtvac());
          existingArray.add(item);
        }
      }
    }
    root.set("existingVinculos", existingArray);

    ObjectNode tenantNode = mapper.createObjectNode();
    tenantNode.put("empCodigo", empCodigo);
    root.set("tenant", tenantNode);

    ObjectNode contextNode = mapper.createObjectNode();
    contextNode.put("resourceKey", VinculoProvimentoRuleSet.RESOURCE_KEY);
    contextNode.put("ruleSetKey", VinculoProvimentoRuleSet.RULESET_KEY);
    contextNode.put("creating", ctx.creating());
    root.set("context", contextNode);
    return root;
  }

  private ObjectNode toCargoFactNode(
      Cargo cargo,
      long ocupadas,
      Vinculo draft,
      Integer empCodigo,
      boolean creating,
      boolean comissaoSlot) {
    ObjectNode node = mapper.createObjectNode();
    if (cargo == null) {
      node.put("present", false);
      node.put("sameOrgao", false);
      node.put("extinct", false);
      node.put("categoriaCompativel", false);
      node.put("tipoComissaoValido", false);
      node.put("hasVacancyBalance", false);
      return node;
    }
    node.put("present", true);
    node.put("sameOrgao", Objects.equals(cargo.getEmpCodigo(), empCodigo));
    node.put("extinct", cargo.getDtExtincao() != null);
    boolean categoriaCompativel = true;
    if (draft != null && draft.getTipovinc() != null && cargo.getTipoCargo() != null) {
      if ("EFETIVO".equals(draft.getTipovinc()) && "COMISSAO".equals(cargo.getTipoCargo())) {
        categoriaCompativel = false;
      } else if ("COMISSIONADO".equals(draft.getTipovinc())
          && "EFETIVO".equals(cargo.getTipoCargo())) {
        categoriaCompativel = false;
      }
    }
    boolean tipoComissaoValido =
        cargo.getTipoCargo() == null || !"EFETIVO".equals(cargo.getTipoCargo());
    node.put("categoriaCompativel", categoriaCompativel);
    node.put("tipoComissaoValido", tipoComissaoValido);

    boolean hasVacancyBalance = true;
    boolean occupying =
        draft != null
            && draft.getSituacao() != null
            && Set.of("ATIVO", "AFASTADO", "CEDIDO").contains(draft.getSituacao());
    if (Boolean.TRUE.equals(cargo.getControleVaga()) && occupying) {
      if (cargo.getVagasAutorizadas() == null || ocupadas >= cargo.getVagasAutorizadas()) {
        hasVacancyBalance = false;
      }
    }
    node.put("hasVacancyBalance", hasVacancyBalance);
    return node;
  }

  private JsonNode extractJsonLogicExpression(Map<String, Object> validationRules) {
    if (validationRules == null || validationRules.isEmpty()) {
      return null;
    }
    if (validationRules.containsKey("jsonLogic") || validationRules.containsKey("rule")) {
      Object raw =
          validationRules.containsKey("jsonLogic")
              ? validationRules.get("jsonLogic")
              : validationRules.get("rule");
      return toSafeJsonValue(raw);
    }
    boolean allScalar = true;
    for (Map.Entry<String, Object> entry : validationRules.entrySet()) {
      if (!isScalarValidationEntry(entry.getKey(), entry.getValue())) {
        allScalar = false;
        break;
      }
    }
    if (allScalar) {
      return null;
    }
    Map<String, Object> exprMap = new LinkedHashMap<>();
    for (Map.Entry<String, Object> entry : validationRules.entrySet()) {
      String k = entry.getKey();
      if ("customMessage".equals(k) && entry.getValue() instanceof String) {
        continue;
      }
      if ("required".equals(k) && entry.getValue() instanceof Boolean) {
        continue;
      }
      exprMap.put(k, entry.getValue());
    }
    return toSafeJsonValue(exprMap);
  }

  private static boolean isScalarValidationEntry(String key, Object value) {
    if (key == null) {
      return false;
    }
    return switch (key) {
      case "min", "max", "minLength", "maxLength" -> value instanceof Number;
      case "pattern", "customMessage" -> value instanceof String;
      case "required" -> value instanceof Boolean;
      default -> false;
    };
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

  private static int computeFrequenciaDurationDays(Frequencia f) {
    if (f == null || f.getDtini() == null) {
      return 1;
    }
    if (f.getDtfim() != null && !f.getDtfim().isBefore(f.getDtini())) {
      return Math.toIntExact(ChronoUnit.DAYS.between(f.getDtini(), f.getDtfim()) + 1);
    }
    return 1;
  }

  private static Integer computeAfastamentoDurationDays(Afastamento a) {
    if (a == null) {
      return null;
    }
    if (a.getQuantidadeDias() != null && a.getQuantidadeDias() > 0) {
      return a.getQuantidadeDias();
    }
    if (a.getDtini() == null) {
      return null;
    }
    LocalDate end = a.getDtfim() != null ? a.getDtfim() : a.getDtprevfim();
    if (end != null && !end.isBefore(a.getDtini())) {
      return Math.toIntExact(ChronoUnit.DAYS.between(a.getDtini(), end) + 1);
    }
    return null;
  }

  private static LocalDate resolveAfastamentoEndDate(Afastamento a) {
    if (a == null) {
      return OPEN_ENDED_DATE;
    }
    if (a.getDtfim() != null) {
      return a.getDtfim();
    }
    if (a.getDtprevfim() != null) {
      return a.getDtprevfim();
    }
    return OPEN_ENDED_DATE;
  }

  private static Integer resolveRetroactiveDays(Frequencia draft, LocalDate referenceDate) {
    if (draft == null) {
      return null;
    }
    if (draft.getCustomFields() != null && draft.getCustomFields().get("retroactiveDays") != null) {
      Object raw = draft.getCustomFields().get("retroactiveDays");
      if (raw instanceof Number n) {
        return n.intValue();
      }
      try {
        return Integer.parseInt(raw.toString());
      } catch (NumberFormatException ignored) {
        // fallback to referenceDate calculation
      }
    }
    if (referenceDate != null && draft.getDtini() != null) {
      return Math.max(0, Math.toIntExact(ChronoUnit.DAYS.between(draft.getDtini(), referenceDate)));
    }
    return null;
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
    JsonNode existingVinculos = facts.path("existingVinculos");
    if (draft.isMissingNode() || draft.isNull() || !existingVinculos.isArray()) {
      return RuleExecutorResult.allow();
    }
    String draftSituacao = textOrNull(draft, "situacao");
    if (!"ATIVO".equals(draftSituacao)) {
      return RuleExecutorResult.allow();
    }
    if (existingVinculos.isEmpty()) {
      return RuleExecutorResult.allow();
    }
    LocalDate draftStart = parseIsoDate(draft.path("dtexerc"));
    if (draftStart == null) {
      draftStart = parseIsoDate(draft.path("dtposse"));
    }
    if (draftStart == null) {
      draftStart = parseIsoDate(draft.path("dtnom"));
    }
    if (draftStart == null) {
      return RuleExecutorResult.of(RuleDecision.INCONCLUSIVE, "MISSING_REQUIRED_FACT");
    }
    LocalDate draftEnd = parseIsoDateOrMax(draft.path("dtvac"));
    Long draftId = draft.path("id").isNumber() ? draft.path("id").longValue() : null;
    Integer draftNumero =
        draft.path("numero").isNumber() ? draft.path("numero").intValue() : null;
    String draftCategoria = textOrNull(draft, "categoria");

    List<JsonNode> overlappingActive = new ArrayList<>();
    for (JsonNode candidate : existingVinculos) {
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
      if (!"ATIVO".equals(textOrNull(candidate, "situacao"))) {
        continue;
      }
      LocalDate candStart = parseIsoDate(candidate.path("dtexerc"));
      if (candStart == null) {
        candStart = parseIsoDate(candidate.path("dtposse"));
      }
      if (candStart == null) {
        candStart = parseIsoDate(candidate.path("dtnom"));
      }
      if (candStart == null) {
        continue;
      }
      LocalDate candEnd = parseIsoDateOrMax(candidate.path("dtvac"));
      if (!draftStart.isAfter(candEnd) && !candStart.isAfter(draftEnd)) {
        overlappingActive.add(candidate);
      }
    }

    if (overlappingActive.isEmpty()) {
      return RuleExecutorResult.allow();
    }
    if (overlappingActive.size() >= 2) {
      return RuleExecutorResult.of(RuleDecision.DENY, "ERG-00300");
    }
    JsonNode other = overlappingActive.get(0);
    String otherCategoria = textOrNull(other, "categoria");
    if (!isConstitutionalAccumulationPair(draftCategoria, otherCategoria)) {
      return RuleExecutorResult.of(RuleDecision.DENY, "ERG-00300");
    }
    return RuleExecutorResult.allow();
  }

  /**
   * Verifica se o par de categorias de vínculos concomitantes é constitucionalmente acumulável nos
   * termos do art. 37, XVI da CF/88:
   * <ul>
   *   <li>(a) dois cargos de professor ({@code MAGISTERIO} + {@code MAGISTERIO});
   *   <li>(b) um cargo de professor com outro técnico ou científico ({@code MAGISTERIO} + {@code TECNICO}/{@code TECNICO_CIENTIFICO}/{@code CIENTIFICO}/{@code SAUDE});
   *   <li>(c) dois cargos ou empregos privativos de profissionais de saúde ({@code SAUDE} + {@code SAUDE}).
   * </ul>
   */
  public static boolean isConstitutionalAccumulationPair(String catA, String catB) {
    if (catA == null || catB == null) {
      return false;
    }
    boolean aProf = PROFESSOR_CATEGORIAS.contains(catA);
    boolean bProf = PROFESSOR_CATEGORIAS.contains(catB);
    if (aProf && bProf) {
      return true;
    }
    boolean aSaude = SAUDE_CATEGORIAS.contains(catA);
    boolean bSaude = SAUDE_CATEGORIAS.contains(catB);
    if (aSaude && bSaude) {
      return true;
    }
    boolean aTec = TECNICO_CIENTIFICO_CATEGORIAS.contains(catA) || aSaude;
    boolean bTec = TECNICO_CIENTIFICO_CATEGORIAS.contains(catB) || bSaude;
    return (aProf && bTec) || (bProf && aTec);
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
    if (value != null) {
      node.put(field, value);
    } else {
      node.putNull(field);
    }
  }

  private static void putIntOrNull(ObjectNode node, String field, Integer value) {
    if (value != null) {
      node.put(field, value);
    } else {
      node.putNull(field);
    }
  }

  private static void putLongOrNull(ObjectNode node, String field, Long value) {
    if (value != null) {
      node.put(field, value);
    } else {
      node.putNull(field);
    }
  }

  private static void putBooleanOrNull(ObjectNode node, String field, Boolean value) {
    if (value != null) {
      node.put(field, value);
    } else {
      node.putNull(field);
    }
  }

  private static void putDateOrNull(ObjectNode node, String field, LocalDate value) {
    if (value != null) {
      node.put(field, value.toString());
    } else {
      node.putNull(field);
    }
  }
}
