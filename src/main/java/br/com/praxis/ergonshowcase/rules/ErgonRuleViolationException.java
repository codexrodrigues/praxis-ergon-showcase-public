package br.com.praxis.ergonshowcase.rules;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.praxisplatform.rules.contract.DecisionBinding;
import org.praxisplatform.rules.contract.DecisionSource;
import org.praxisplatform.rules.contract.DecisionStage;
import org.praxisplatform.rules.contract.RuleBindingResult;
import org.praxisplatform.rules.contract.RuleDecision;
import org.praxisplatform.rules.contract.RuleEvaluationResult;
import org.praxisplatform.rules.plan.RuleDecisionPlan;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.server.ResponseStatusException;

/**
 * Exceção de domínio lançada quando a avaliação determinística de um {@code RuleDecisionPlan} no
 * {@code praxis-rules-engine} não resulta em {@code RuleDecision.ALLOW} (comportamento
 * <i>fail-closed</i> para {@code DENY}, {@code INCONCLUSIVE} e {@code TECHNICAL_ERROR}).
 *
 * <p>Preserva na mensagem e nas propriedades estruturadas RFC 7807 ({@link ProblemDetail}) todos os
 * códigos canônicos ({@code ERG-NNNNN} / {@code CERG-NNNNN}), os {@code slotKey}s, os
 * {@code bindingKey}s, a rastreabilidade da decisão ({@link DecisionSource#PRODUCT} vs
 * {@link DecisionSource#CUSTOMER}) e o campo alvo ({@code targetField}) para consumo por interfaces
 * tradicionais, Angular Dynamic Form e agentes de IA.
 */
public class ErgonRuleViolationException extends ResponseStatusException {

  /**
   * Detalhe estruturado e auditável de uma violação determinística por slot/binding.
   *
   * @param code código canônico da razão ({@code ERG-*}, {@code CERG-*} ou código técnico fail-closed)
   * @param slotKey chave estável do {@code DecisionSlot} no plano
   * @param bindingKey identificador do {@code DecisionBinding} que originou a decisão
   * @param source origem governada da decisão ({@link DecisionSource#PRODUCT} ou {@link DecisionSource#CUSTOMER})
   * @param stage estágio do pipeline ({@link DecisionStage#PROTECTED_GUARD}, {@code DOMAIN_DECISION}, {@code POST_DECISION})
   * @param decision estado determinístico retornado pelo binding ({@code DENY}, {@code INCONCLUSIVE}, {@code TECHNICAL_ERROR})
   * @param targetField atributo principal do contrato afetado
   * @param messagePtBr mensagem oficial de negócio em português do Brasil
   * @param plsqlOrigin rotina legada de origem ({@code PCK_*} ou {@code EPB__*})
   */
  public record SlotViolationDetail(
      String code,
      String slotKey,
      String bindingKey,
      DecisionSource source,
      DecisionStage stage,
      RuleDecision decision,
      String targetField,
      String messagePtBr,
      String plsqlOrigin) {}

  private final transient RuleDecisionPlan plan;
  private final transient RuleEvaluationResult evaluationResult;
  private final List<String> reasonCodes;
  private final transient List<ErgonRuleErrorCatalog.RuleErrorDescriptor> descriptors;
  private final transient List<SlotViolationDetail> violations;

  public ErgonRuleViolationException(RuleEvaluationResult evaluationResult) {
    this(null, evaluationResult);
  }

  public ErgonRuleViolationException(
      RuleDecisionPlan plan, RuleEvaluationResult evaluationResult) {
    super(
        HttpStatus.UNPROCESSABLE_ENTITY,
        formatMessage(
            plan, Objects.requireNonNull(evaluationResult, "evaluationResult must not be null")));
    this.plan = plan;
    this.evaluationResult = evaluationResult;
    this.reasonCodes = List.copyOf(evaluationResult.reasonCodes());
    this.descriptors =
        this.reasonCodes.stream()
            .map(ErgonRuleErrorCatalog::find)
            .flatMap(Optional::stream)
            .toList();
    this.violations = buildViolations(plan, evaluationResult);
    populateProblemDetailProperties();
  }

  public ErgonRuleViolationException(
      String code,
      String slotKey,
      String bindingKey,
      DecisionSource source,
      DecisionStage stage,
      String targetField,
      String messagePtBr) {
    super(
        HttpStatus.UNPROCESSABLE_ENTITY,
        "["
            + code
            + " | "
            + source
            + " | slot="
            + slotKey
            + " | field="
            + targetField
            + "] "
            + messagePtBr);
    this.plan = null;
    this.evaluationResult = null;
    this.reasonCodes = List.of(code);
    this.descriptors = ErgonRuleErrorCatalog.find(code).stream().toList();
    this.violations =
        List.of(
            new SlotViolationDetail(
                code,
                slotKey,
                bindingKey,
                source,
                stage,
                RuleDecision.DENY,
                targetField,
                messagePtBr,
                this.descriptors.isEmpty()
                    ? "PraxisJsonLogicEngine"
                    : this.descriptors.get(0).plsqlOrigin()));
    ProblemDetail body = getBody();
    body.setTitle("Violação determinística de regra de negócio");
    body.setProperty("decision", RuleDecision.DENY.name());
    body.setProperty("reasonCodes", this.reasonCodes);
    body.setProperty("slotKeys", getSlotKeys());
    body.setProperty(
        "decisionSources", getDecisionSources().stream().map(Enum::name).toList());
    body.setProperty("violations", this.violations);
  }

  public RuleDecisionPlan getPlan() {
    return plan;
  }

  public RuleEvaluationResult getEvaluationResult() {
    return evaluationResult;
  }

  public List<String> getReasonCodes() {
    return reasonCodes;
  }

  public List<ErgonRuleErrorCatalog.RuleErrorDescriptor> getDescriptors() {
    return descriptors;
  }

  public List<SlotViolationDetail> getViolations() {
    return violations;
  }

  /** Retorna os {@code slotKey}s distintos acionados na violação. */
  public List<String> getSlotKeys() {
    return violations.stream()
        .map(SlotViolationDetail::slotKey)
        .filter(Objects::nonNull)
        .distinct()
        .toList();
  }

  /** Retorna as origens ({@code PRODUCT} / {@code CUSTOMER}) distintas acionadas na violação. */
  public List<DecisionSource> getDecisionSources() {
    return violations.stream()
        .map(SlotViolationDetail::source)
        .filter(Objects::nonNull)
        .distinct()
        .toList();
  }

  private void populateProblemDetailProperties() {
    ProblemDetail body = getBody();
    String ruleSetKey =
        evaluationResult.ruleSetRef() != null
            ? evaluationResult.ruleSetRef().ruleSetKey()
            : null;
    body.setTitle(resolveProblemTitle(ruleSetKey));
    if (evaluationResult.ruleSetRef() != null) {
      body.setProperty("ruleSetKey", ruleSetKey);
      body.setProperty("resourceKey", evaluationResult.ruleSetRef().boundedContextKey());
    }
    body.setProperty("planDigest", evaluationResult.planDigest());
    body.setProperty("factsDigest", evaluationResult.factsDigest());
    body.setProperty("decision", evaluationResult.decision().name());
    body.setProperty("reasonCodes", reasonCodes);
    body.setProperty("slotKeys", getSlotKeys());
    body.setProperty(
        "decisionSources", getDecisionSources().stream().map(Enum::name).toList());
    body.setProperty("violations", violations);
  }

  public static List<SlotViolationDetail> buildViolations(
      RuleDecisionPlan plan, RuleEvaluationResult result) {
    String ruleSetKey =
        result.ruleSetRef() != null
            ? result.ruleSetRef().ruleSetKey()
            : null;
    Map<String, DecisionSource> sourceByBindingKey = new LinkedHashMap<>();
    if (plan != null && plan.orderedBindings() != null) {
      for (DecisionBinding binding : plan.orderedBindings()) {
        if (binding != null) {
          sourceByBindingKey.put(binding.bindingKey(), binding.source());
        }
      }
    }

    List<SlotViolationDetail> list = new ArrayList<>();
    if (result.bindingResults() != null) {
      for (RuleBindingResult br : result.bindingResults()) {
        if (br == null || br.decision() == RuleDecision.ALLOW) {
          continue;
        }
        DecisionSource source =
            sourceByBindingKey.getOrDefault(
                br.bindingKey(), inferSource(br.bindingKey(), br.reasonCodes()));
        List<String> codes =
            br.reasonCodes() == null || br.reasonCodes().isEmpty()
                ? List.of(br.decision().name())
                : br.reasonCodes();
        for (String code : codes) {
          Optional<ErgonRuleErrorCatalog.RuleErrorDescriptor> desc =
              ErgonRuleErrorCatalog.find(code);
          list.add(
              new SlotViolationDetail(
                  code,
                  br.slotKey(),
                  br.bindingKey(),
                  source,
                  br.stage(),
                  br.decision(),
                  desc.map(ErgonRuleErrorCatalog.RuleErrorDescriptor::targetField).orElse("dtini"),
                  ErgonRuleErrorCatalog.resolveMessagePtBr(code),
                  desc.map(ErgonRuleErrorCatalog.RuleErrorDescriptor::plsqlOrigin)
                      .orElseGet(() -> resolveDefaultPlsqlOrigin(ruleSetKey, source))));
        }
      }
    }

    if (list.isEmpty() && result.reasonCodes() != null) {
      for (String code : result.reasonCodes()) {
        Optional<ErgonRuleErrorCatalog.RuleErrorDescriptor> desc = ErgonRuleErrorCatalog.find(code);
        DecisionSource source = inferSource(null, List.of(code));
        list.add(
            new SlotViolationDetail(
                code,
                desc.map(ErgonRuleErrorCatalog.RuleErrorDescriptor::slotKey)
                    .orElseGet(() -> resolveDefaultSlotKey(ruleSetKey)),
                "unknown",
                source,
                DecisionStage.DOMAIN_DECISION,
                result.decision(),
                desc.map(ErgonRuleErrorCatalog.RuleErrorDescriptor::targetField).orElse("dtini"),
                ErgonRuleErrorCatalog.resolveMessagePtBr(code),
                desc.map(ErgonRuleErrorCatalog.RuleErrorDescriptor::plsqlOrigin)
                    .orElseGet(() -> resolveDefaultPlsqlOrigin(ruleSetKey, source))));
      }
    }
    return List.copyOf(list);
  }

  private static String resolveProblemTitle(String ruleSetKey) {
    if (ruleSetKey == null) {
      return "Violação determinística de regra estatutária (RN-013)";
    }
    if (ruleSetKey.contains("parametros-contagem-ferias")) {
      return "Violação determinística de regra de parâmetros de contagem de férias (PCK_PARAMETROS_CONTA)";
    }
    if (ruleSetKey.contains("periodos-aquisitivos-ferias")) {
      return "Violação determinística de regra de período aquisitivo de férias (PCK_PERAQFER)";
    }
    if (ruleSetKey.contains("ferias-gozo")) {
      return "Violação determinística de regra de concessão e gozo de férias (PCK_FERIAS)";
    }
    if (ruleSetKey.contains("frequencias") && !ruleSetKey.contains("regras-frequencia")) {
      return "Violação determinística de regra de lançamento de frequência (PCK_FREQUENCIAS)";
    }
    if (ruleSetKey.contains("afastamentos")) {
      return "Violação determinística de regra de concessão de afastamento (PCK_LIC_AFAST)";
    }
    if (ruleSetKey.contains("dependentes")) {
      return "Violação determinística de regra de elegibilidade de dependente (PCK_DEPENDENTES)";
    }
    if (ruleSetKey.contains("vinculos")) {
      return "Violação determinística de regra de provimento e movimentação funcional (PCK_VINCULOS)";
    }
    return "Violação determinística de regra estatutária (RN-013)";
  }

  private static String resolveDefaultPlsqlOrigin(String ruleSetKey, DecisionSource source) {
    boolean customer = source == DecisionSource.CUSTOMER;
    if (ruleSetKey == null) {
      return customer ? "EPB__REGRAS_FREQ" : "PCK_REGRAS_FREQ";
    }
    if (ruleSetKey.contains("parametros-contagem-ferias")) {
      return customer ? "EPB__PARAMETROS_CONTA" : "PCK_PARAMETROS_CONTA";
    }
    if (ruleSetKey.contains("periodos-aquisitivos-ferias")) {
      return customer ? "EPB__PERAQFER" : "PCK_PERAQFER";
    }
    if (ruleSetKey.contains("ferias-gozo")) {
      return customer ? "EPB__FERIAS" : "PCK_FERIAS";
    }
    if (ruleSetKey.contains("frequencias") && !ruleSetKey.contains("regras-frequencia")) {
      return customer ? "EPB__FREQUENCIAS" : "PCK_FREQUENCIAS";
    }
    if (ruleSetKey.contains("afastamentos")) {
      return customer ? "EPB__LIC_AFAST" : "PCK_LIC_AFAST";
    }
    if (ruleSetKey.contains("dependentes")) {
      return customer ? "EPB__DEPENDENTES" : "PCK_DEPENDENTES";
    }
    if (ruleSetKey.contains("vinculos")) {
      return customer ? "EPB__VINCULOS" : "PCK_VINCULOS";
    }
    return customer ? "EPB__REGRAS_FREQ" : "PCK_REGRAS_FREQ";
  }

  private static String resolveDefaultSlotKey(String ruleSetKey) {
    if (ruleSetKey == null) {
      return "rn013.unknown";
    }
    if (ruleSetKey.contains("parametros-contagem-ferias")) {
      return "parametros-conta.unknown";
    }
    if (ruleSetKey.contains("periodos-aquisitivos-ferias")) {
      return "peraqfer.unknown";
    }
    if (ruleSetKey.contains("ferias-gozo")) {
      return "ferias-gozo.unknown";
    }
    if (ruleSetKey.contains("frequencias") && !ruleSetKey.contains("regras-frequencia")) {
      return "frequencias.unknown";
    }
    if (ruleSetKey.contains("afastamentos")) {
      return "afastamentos.unknown";
    }
    if (ruleSetKey.contains("dependentes")) {
      return "dependentes.unknown";
    }
    if (ruleSetKey.contains("vinculos")) {
      return "vinculos.unknown";
    }
    return "rn013.unknown";
  }

  private static DecisionSource inferSource(String bindingKey, List<String> codes) {
    if (bindingKey != null && bindingKey.startsWith("customer.")) {
      return DecisionSource.CUSTOMER;
    }
    if (codes != null && codes.stream().anyMatch(c -> c != null && c.startsWith("CERG-"))) {
      return DecisionSource.CUSTOMER;
    }
    return DecisionSource.PRODUCT;
  }

  private static String formatMessage(RuleDecisionPlan plan, RuleEvaluationResult result) {
    List<SlotViolationDetail> details = buildViolations(plan, result);
    if (details.isEmpty()) {
      return "Decisão determinística bloqueada (fail-closed: " + result.decision() + ").";
    }
    return details.stream()
        .map(
            v ->
                "["
                    + v.code()
                    + " | "
                    + v.source()
                    + " | slot="
                    + v.slotKey()
                    + "] "
                    + v.messagePtBr())
        .collect(Collectors.joining(" | "));
  }
}
