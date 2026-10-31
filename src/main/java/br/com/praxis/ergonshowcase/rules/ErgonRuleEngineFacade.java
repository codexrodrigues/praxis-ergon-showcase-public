package br.com.praxis.ergonshowcase.rules;

import br.com.praxis.ergonshowcase.domain.RegraFrequenciaEstatutaria;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.praxisplatform.rules.contract.RuleDecision;
import org.praxisplatform.rules.contract.RuleEvaluationResult;
import org.praxisplatform.rules.contract.RuleExecutorResult;
import org.praxisplatform.rules.contract.RuleSetDefinition;
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
 * <p>Toda consulta JPA/SQL ocorre na camada de serviço antes de invocar o motor; os executores
 * JSON Logic e Java registrados nesta fachada operam exclusivamente sobre a árvore imutável de
 * fatos ({@link JsonNode}), garantindo execução pura, determinística e auditável por slot.
 */
@Component
public class ErgonRuleEngineFacade {

  public static final String DEFAULT_TIME_ZONE = "America/Sao_Paulo";
  private static final LocalDate OPEN_ENDED_DATE = LocalDate.of(9999, 12, 31);

  private final ObjectMapper mapper;
  private final RuleBindingExecutorRegistry executorRegistry;
  private final PraxisRulePlanCompiler compiler;
  private final PraxisRuleSetEngine engine;
  private final Map<Integer, RuleDecisionPlan> rn013PlansByOrgao = new ConcurrentHashMap<>();
  private final RuleDecisionPlan defaultProductRn013Plan;

  public ErgonRuleEngineFacade() {
    this.mapper = new ObjectMapper();
    this.executorRegistry = new RuleBindingExecutorRegistry(List.of(concomitanciaJavaExecutor()));
    this.compiler = new PraxisRulePlanCompiler(this.executorRegistry);
    this.engine = new PraxisRuleSetEngine(this.executorRegistry);
    this.defaultProductRn013Plan =
        this.compiler.compile(Rn013RegrasFrequenciaRuleSet.productRuleSet());
    this.rn013PlansByOrgao.put(1, this.defaultProductRn013Plan);
    this.rn013PlansByOrgao.put(
        2, this.compiler.compile(Rn013RegrasFrequenciaRuleSet.orgao2CustomizedRuleSet()));
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

  /**
   * Constrói um payload estruturado de diagnóstico e auditoria para uma avaliação da {@code RN-013},
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
   * órgão informado, incluindo slots, bindings ativos ({@code PRODUCT} vs {@code CUSTOMER}) e o
   * catálogo de códigos de razão ({@link ErgonRuleErrorCatalog}).
   */
  public Map<String, Object> toPlanSummaryPayload(Integer empCodigo) {
    int resolvedOrgao = empCodigo != null ? empCodigo : 1;
    RuleDecisionPlan plan = resolveRn013Plan(resolvedOrgao);
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
   * Constrói a árvore JSON imutável de fatos ({@code draft}, {@code existingOverlaps},
   * {@code tenant}, {@code context}) sem {@code POJONode}s.
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

  /**
   * Executor Java puro e determinístico para {@code ERG-08393}
   * ({@code PCK_REGRAS_FREQ.MAIN_POS} — {@code PACK_HADES.EH_CONCOMITANTE}).
   *
   * <p>Verifica se existe na coleção pré-carregada {@code existingOverlaps} outra regra com a mesma
   * chave lógica ({@code empCodigo}, {@code regimeJuridico}, {@code tipoVinculo},
   * {@code categoria}, {@code subcategoria}, {@code tipofreq}, {@code codfreq}) cujo intervalo de
   * vigência {@code [dtini, dtfim]} se sobreponha ao intervalo do rascunho avaliado.
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
      // PACK_HADES.EH_CONCOMITANTE: intervalos [draftStart, draftEnd] e [candStart, candEnd] se interceptam
      if (!draftStart.isAfter(candEnd) && !candStart.isAfter(draftEnd)) {
        return RuleExecutorResult.of(RuleDecision.DENY, "ERG-08393");
      }
    }
    return RuleExecutorResult.allow();
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
