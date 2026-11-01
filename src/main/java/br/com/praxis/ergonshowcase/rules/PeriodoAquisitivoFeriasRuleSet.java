package br.com.praxis.ergonshowcase.rules;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.praxisplatform.rules.contract.CompositionPolicy;
import org.praxisplatform.rules.contract.DecisionAggregationPolicy;
import org.praxisplatform.rules.contract.DecisionBinding;
import org.praxisplatform.rules.contract.DecisionSlot;
import org.praxisplatform.rules.contract.DecisionSource;
import org.praxisplatform.rules.contract.DecisionStage;
import org.praxisplatform.rules.contract.OverridePolicy;
import org.praxisplatform.rules.contract.RuleDecision;
import org.praxisplatform.rules.contract.RuleExecutorRef;
import org.praxisplatform.rules.contract.RuleFailPolicy;
import org.praxisplatform.rules.contract.RuleParameterDefinition;
import org.praxisplatform.rules.contract.RuleParameterTemplate;
import org.praxisplatform.rules.contract.RuleParameterType;
import org.praxisplatform.rules.contract.RuleParameterization;
import org.praxisplatform.rules.contract.RuleRuntimeCompatibility;
import org.praxisplatform.rules.contract.RuleSetDefinition;
import org.praxisplatform.rules.contract.RuleSetRef;
import org.praxisplatform.rules.contract.SlotCardinality;

/**
 * Definição canônica do {@link RuleSetDefinition} para validação determinística de Períodos
 * Aquisitivos de Férias ({@code PCK_PERAQFER.MAIN_PRE}, {@code MAIN_POS}, {@code VALID_PERAQFER} e
 * {@code VERIFICA_DT_PRESCR}) sobre o {@code praxis-rules-engine:0.1.0-beta.17}
 * (Contrato {@code 1.6}).
 */
public final class PeriodoAquisitivoFeriasRuleSet {

  public static final String DOMAIN_SCOPE = "workforce.vacation-and-leave";
  public static final String RESOURCE_KEY = "administracao-pessoal.periodos-aquisitivos-ferias";
  public static final String RULE_SET_KEY =
      "ergon.administracao-pessoal.periodos-aquisitivos-ferias.aquisicao";
  public static final String OPERATION = "validate";
  public static final int VERSION = 1;

  public static final List<String> AVAILABLE_ROOTS =
      List.of(
          "draft",
          "vinculo",
          "effectiveParametroConta",
          "derived",
          "existingPeriodos",
          "tenant",
          "context");

  public static final String TEMPLATE_ACCUMULATION_AND_REDUCTION_BOUNDS_KEY =
      "ergon.peraqfer:accumulation-and-reduction-bounds";
  public static final String TEMPLATE_ACCUMULATION_AND_REDUCTION_BOUNDS_VERSION = "1.0";
  public static final String TEMPLATE_ACCUMULATION_AND_REDUCTION_BOUNDS =
      TEMPLATE_ACCUMULATION_AND_REDUCTION_BOUNDS_KEY
          + "@"
          + TEMPLATE_ACCUMULATION_AND_REDUCTION_BOUNDS_VERSION;

  public static final String JAVA_EXECUTOR_SOBREPOSICAO_KEY =
      "ergon.peraqfer:sobreposicao-e-acumulo-periodos";
  public static final String JAVA_EXECUTOR_VERSION = "1.0";
  public static final String EXECUTOR_SOBREPOSICAO_E_ACUMULO_PERIODOS =
      JAVA_EXECUTOR_SOBREPOSICAO_KEY + "@" + JAVA_EXECUTOR_VERSION;

  public static final String SLOT_CRONOLOGIA_DATAS_VALIDA =
      "peraqfer.guard.cronologia-datas-valida";
  public static final String SLOT_VINCULO_EXERCICIO_VACANCIA =
      "peraqfer.guard.vinculo-exercicio-vacancia";
  public static final String SLOT_HOOK_PRE_VALIDACAO =
      "peraqfer.hook.pre-validacao-orgao";
  public static final String SLOT_BLOQUEIO_AVERBACAO_E_CRITERIO_PRESCRICAO =
      "peraqfer.core.bloqueio-averbacao-e-criterio-prescricao";
  public static final String SLOT_DIREITO_TETO_E_REDUCAO_FALTAS =
      "peraqfer.core.direito-teto-e-reducao-faltas";
  public static final String SLOT_ACUMULO_E_FALTAS_LIMITES_TEMPLATE =
      "peraqfer.core.acumulo-e-faltas-limites-template";
  public static final String SLOT_OVERRIDES_CONTAGEM_DOBRO =
      "peraqfer.core.overrides-contagem-dobro";
  public static final String SLOT_OVERRIDES_PARCELAMENTO_E_ABONO =
      "peraqfer.core.overrides-parcelamento-e-abono";
  public static final String SLOT_PRESCRICAO_PARCELAS_ATIVAS =
      "peraqfer.core.prescricao-parcelas-ativas";
  public static final String SLOT_SOBREPOSICAO_E_ACUMULO_PERIODOS =
      "peraqfer.post.sobreposicao-e-acumulo-periodos";
  public static final String SLOT_HOOK_POS_VALIDACAO =
      "peraqfer.hook.pos-validacao-orgao";

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static final String ERG00612_TEMPLATE_EXPRESSION =
      """
      {
        "and": [
          {"<=": [{"var": "derived.openAccumulatedPeriodsCount"}, 2]},
          {
            "or": [
              {"<=": [{"var": "draft.totDiasDireito"}, 0]},
              {"<=": [{"var": "draft.faltasApuradas"}, 32]}
            ]
          }
        ]
      }
      """;

  private PeriodoAquisitivoFeriasRuleSet() {}

  public static RuleSetRef ruleSetRef() {
    return new RuleSetRef(DOMAIN_SCOPE, RESOURCE_KEY, RULE_SET_KEY, OPERATION, VERSION);
  }

  public static RuleParameterTemplate accumulationAndReductionBoundsTemplate() {
    return new RuleParameterTemplate(
        TEMPLATE_ACCUMULATION_AND_REDUCTION_BOUNDS_KEY,
        TEMPLATE_ACCUMULATION_AND_REDUCTION_BOUNDS_VERSION,
        RuleExecutorRef.jsonLogic(parseJson(ERG00612_TEMPLATE_EXPRESSION)),
        RuleDecision.DENY,
        "ERG-00612",
        List.of("draft.dtini", "draft.dtfim"),
        List.of(
            new RuleParameterDefinition(
                "maxPeriodosAcumuladosAbertos",
                RuleParameterType.NUMBER,
                true,
                false,
                false,
                "periods",
                MAPPER.getNodeFactory().numberNode(2),
                BigDecimal.ONE,
                new BigDecimal("10"),
                List.of(),
                List.of("/and/0/<=/1")),
            new RuleParameterDefinition(
                "maxFaltasSemPerdaTotal",
                RuleParameterType.NUMBER,
                true,
                false,
                false,
                "days",
                MAPPER.getNodeFactory().numberNode(32),
                BigDecimal.ZERO,
                new BigDecimal("365"),
                List.of(),
                List.of("/and/1/or/1/<=/1"))));
  }

  public static List<DecisionSlot> productSlots() {
    return List.of(
        new DecisionSlot(
            SLOT_CRONOLOGIA_DATAS_VALIDA,
            DecisionStage.PROTECTED_GUARD,
            SlotCardinality.SINGLE,
            OverridePolicy.FORBIDDEN,
            DecisionAggregationPolicy.SINGLE_RESULT,
            null),
        new DecisionSlot(
            SLOT_VINCULO_EXERCICIO_VACANCIA,
            DecisionStage.PROTECTED_GUARD,
            SlotCardinality.SINGLE,
            OverridePolicy.FORBIDDEN,
            DecisionAggregationPolicy.SINGLE_RESULT,
            null),
        new DecisionSlot(
            SLOT_HOOK_PRE_VALIDACAO,
            DecisionStage.DOMAIN_DECISION,
            SlotCardinality.MULTIPLE,
            OverridePolicy.RESTRICT_ONLY,
            DecisionAggregationPolicy.DENY_OVERRIDES,
            null),
        new DecisionSlot(
            SLOT_BLOQUEIO_AVERBACAO_E_CRITERIO_PRESCRICAO,
            DecisionStage.DOMAIN_DECISION,
            SlotCardinality.SINGLE,
            OverridePolicy.FORBIDDEN,
            DecisionAggregationPolicy.SINGLE_RESULT,
            null),
        new DecisionSlot(
            SLOT_DIREITO_TETO_E_REDUCAO_FALTAS,
            DecisionStage.DOMAIN_DECISION,
            SlotCardinality.MULTIPLE,
            OverridePolicy.RESTRICT_ONLY,
            DecisionAggregationPolicy.DENY_OVERRIDES,
            null),
        new DecisionSlot(
            SLOT_ACUMULO_E_FALTAS_LIMITES_TEMPLATE,
            DecisionStage.DOMAIN_DECISION,
            SlotCardinality.SINGLE,
            OverridePolicy.PARAMETERIZABLE,
            DecisionAggregationPolicy.SINGLE_RESULT,
            accumulationAndReductionBoundsTemplate()),
        new DecisionSlot(
            SLOT_OVERRIDES_CONTAGEM_DOBRO,
            DecisionStage.DOMAIN_DECISION,
            SlotCardinality.MULTIPLE,
            OverridePolicy.RESTRICT_ONLY,
            DecisionAggregationPolicy.DENY_OVERRIDES,
            null),
        new DecisionSlot(
            SLOT_OVERRIDES_PARCELAMENTO_E_ABONO,
            DecisionStage.DOMAIN_DECISION,
            SlotCardinality.MULTIPLE,
            OverridePolicy.RESTRICT_ONLY,
            DecisionAggregationPolicy.DENY_OVERRIDES,
            null),
        new DecisionSlot(
            SLOT_PRESCRICAO_PARCELAS_ATIVAS,
            DecisionStage.DOMAIN_DECISION,
            SlotCardinality.SINGLE,
            OverridePolicy.REPLACEABLE,
            DecisionAggregationPolicy.SINGLE_RESULT,
            null),
        new DecisionSlot(
            SLOT_SOBREPOSICAO_E_ACUMULO_PERIODOS,
            DecisionStage.POST_DECISION,
            SlotCardinality.SINGLE,
            OverridePolicy.FORBIDDEN,
            DecisionAggregationPolicy.SINGLE_RESULT,
            null),
        new DecisionSlot(
            SLOT_HOOK_POS_VALIDACAO,
            DecisionStage.POST_DECISION,
            SlotCardinality.MULTIPLE,
            OverridePolicy.RESTRICT_ONLY,
            DecisionAggregationPolicy.DENY_OVERRIDES,
            null));
  }

  public static List<DecisionBinding> productBindings() {
    List<String> baseFacts = List.of("draft.dtini", "draft.dtfim");
    return List.of(
        // 1. ERG-01851: dtini <= dtfim, dtPrescricao >= dtfim, dtPgtoComMulta >= dtfim
        productJsonLogicBinding(
            "product.peraqfer.erg-01851",
            SLOT_CRONOLOGIA_DATAS_VALIDA,
            10,
            """
            {
              "and": [
                {">=": [{"date": [{"var": "draft.dtfim"}]}, {"date": [{"var": "draft.dtini"}]}]},
                {
                  "or": [
                    {"==": [{"var": "draft.dtPrescricao"}, null]},
                    {">=": [{"date": [{"var": "draft.dtPrescricao"}]}, {"date": [{"var": "draft.dtfim"}]}]}
                  ]
                },
                {
                  "or": [
                    {"==": [{"var": "draft.dtPgtoComMulta"}, null]},
                    {">=": [{"date": [{"var": "draft.dtPgtoComMulta"}]}, {"date": [{"var": "draft.dtfim"}]}]}
                  ]
                }
              ]
            }
            """,
            "ERG-01851",
            baseFacts),
        // 2. ERG-01850: dtini compatível com dtexerc, dtvac e dtaposent do vínculo
        productJsonLogicBinding(
            "product.peraqfer.erg-01850",
            SLOT_VINCULO_EXERCICIO_VACANCIA,
            20,
            """
            {
              "and": [
                {
                  "or": [
                    {"==": [{"var": "draft.geradoPorAverbacao"}, true]},
                    {"==": [{"var": "vinculo.dtexerc"}, null]},
                    {">=": [{"date": [{"var": "draft.dtini"}]}, {"date": [{"var": "vinculo.dtexerc"}]}]}
                  ]
                },
                {
                  "or": [
                    {"==": [{"var": "vinculo.dtvac"}, null]},
                    {"<": [{"date": [{"var": "draft.dtini"}]}, {"date": [{"var": "vinculo.dtvac"}]}]}
                  ]
                },
                {
                  "or": [
                    {"==": [{"var": "vinculo.dtaposent"}, null]},
                    {"<": [{"date": [{"var": "draft.dtini"}]}, {"date": [{"var": "vinculo.dtaposent"}]}]}
                  ]
                }
              ]
            }
            """,
            "ERG-01850",
            baseFacts),
        // 3. Hook pré-validação
        productJsonLogicBinding(
            "product.peraqfer.hook-pre-baseline",
            SLOT_HOOK_PRE_VALIDACAO,
            30,
            "{\"==\": [1, 1]}",
            "CERG-PA-PRE-001",
            baseFacts),
        // 4. ERG-00099 / ERG-00603: proteção de averbação e critério de prescrição
        productJsonLogicBinding(
            "product.peraqfer.erg-00099",
            SLOT_BLOQUEIO_AVERBACAO_E_CRITERIO_PRESCRICAO,
            40,
            """
            {
              "and": [
                {"!=": [{"var": "draft.lockedAverbacaoMutation"}, true]},
                {"in": [{"var": "draft.criterioPrescricao"}, [null, "I", "F"]]},
                {"in": [{"var": "draft.semPgtoMulta"}, [null, "I", "S", "N"]]}
              ]
            }
            """,
            "ERG-00099",
            baseFacts),
        // 5. ERG-00600: totDiasDireito <= totDiasFer, escala de faltas e >= totalDiasConsumidos
        productJsonLogicBinding(
            "product.peraqfer.erg-00600",
            SLOT_DIREITO_TETO_E_REDUCAO_FALTAS,
            50,
            """
            {
              "and": [
                {">=": [{"var": "draft.totDiasDireito"}, 0]},
                {"<=": [{"var": "draft.totDiasDireito"}, {"var": "effectiveParametroConta.totDiasFer"}]},
                {"<=": [{"var": "draft.totDiasDireito"}, {"var": "derived.maxDiasDireitoPelaEscalaFaltas"}]},
                {">=": [{"var": "draft.totDiasDireito"}, {"var": "derived.totalDiasConsumidos"}]}
              ]
            }
            """,
            "ERG-00600",
            baseFacts),
        // 6. ERG-00612: template parametrizável de acúmulo de períodos e limite de faltas
        productJsonLogicBinding(
            "product.peraqfer.erg-00612",
            SLOT_ACUMULO_E_FALTAS_LIMITES_TEMPLATE,
            60,
            ERG00612_TEMPLATE_EXPRESSION,
            "ERG-00612",
            baseFacts),
        // 7. ERG-00604..ERG-00610: overrides de contagem em dobro
        productJsonLogicBinding(
            "product.peraqfer.erg-00604",
            SLOT_OVERRIDES_CONTAGEM_DOBRO,
            70,
            """
            {"==": [{"var": "derived.contagemDobroOverrideCoerente"}, true]}
            """,
            "ERG-00604",
            baseFacts),
        // 8. ERG-01801..ERG-01810: overrides de parcelamento e abono no PA
        productJsonLogicBinding(
            "product.peraqfer.erg-01809",
            SLOT_OVERRIDES_PARCELAMENTO_E_ABONO,
            80,
            """
            {"==": [{"var": "derived.overridesParcelamentoAbonoCoerentes"}, true]}
            """,
            "ERG-01809",
            baseFacts),
        // 9. ERG-00615: VERIFICA_DT_PRESCR
        productJsonLogicBinding(
            "product.peraqfer.erg-00615",
            SLOT_PRESCRICAO_PARCELAS_ATIVAS,
            90,
            """
            {
              "or": [
                {"==": [{"var": "effectiveParametroConta.permiteGozoAposPrescricao"}, true]},
                {"==": [{"var": "draft.dtPrescricao"}, null]},
                {"==": [{"var": "derived.parcelasViolandoPrescricaoCount"}, 0]}
              ]
            }
            """,
            "ERG-00615",
            baseFacts),
        // 10. ERG-00601: sobreposição cronológica ou duplicidade de chave no mesmo vínculo
        new DecisionBinding(
            "product.peraqfer.erg-00601",
            SLOT_SOBREPOSICAO_E_ACUMULO_PERIODOS,
            DecisionSource.PRODUCT,
            null,
            RuleExecutorRef.java(JAVA_EXECUTOR_SOBREPOSICAO_KEY, JAVA_EXECUTOR_VERSION),
            List.of(),
            100,
            true,
            null,
            null,
            List.of("draft.dtini", "draft.dtfim", "derived.overlappingPeriodsCount"),
            null),
        // 11. Hook pós-validação
        productJsonLogicBinding(
            "product.peraqfer.hook-pos-baseline",
            SLOT_HOOK_POS_VALIDACAO,
            110,
            "{\"==\": [1, 1]}",
            "CERG-PA-POST-001",
            baseFacts));
  }

  public static RuleSetDefinition productRuleSet() {
    return new RuleSetDefinition(
        ruleSetRef(),
        AVAILABLE_ROOTS,
        productSlots(),
        productBindings(),
        RuleRuntimeCompatibility.current(),
        RuleFailPolicy.FAIL_CLOSED);
  }

  public static RuleSetDefinition withCustomerBindings(List<DecisionBinding> customerBindings) {
    Set<String> singleReplacedSlots =
        customerBindings == null
            ? Set.of()
            : customerBindings.stream()
                .filter(
                    b ->
                        b.compositionPolicy() == CompositionPolicy.PARAMETERIZE
                            || b.compositionPolicy() == CompositionPolicy.REPLACE_EXACT)
                .map(DecisionBinding::slotKey)
                .collect(Collectors.toSet());

    List<DecisionBinding> merged = new ArrayList<>();
    for (DecisionBinding productBinding : productBindings()) {
      if (singleReplacedSlots.contains(productBinding.slotKey())) {
        merged.add(
            new DecisionBinding(
                productBinding.bindingKey(),
                productBinding.slotKey(),
                productBinding.source(),
                productBinding.compositionPolicy(),
                productBinding.executor(),
                productBinding.dependsOn(),
                productBinding.order(),
                false,
                productBinding.falseDecision(),
                productBinding.falseReasonCode(),
                productBinding.requiredFactPaths(),
                productBinding.parameterization()));
      } else {
        merged.add(productBinding);
      }
    }
    if (customerBindings != null) {
      merged.addAll(customerBindings);
    }
    return new RuleSetDefinition(
        ruleSetRef(),
        AVAILABLE_ROOTS,
        productSlots(),
        List.copyOf(merged),
        RuleRuntimeCompatibility.current(),
        RuleFailPolicy.FAIL_CLOSED);
  }

  /**
   * <b>Modo 1 ({@link OverridePolicy#FORBIDDEN} — Prova Negativa):</b> tentativa ilegal de
   * substituir o guard protegido {@code ERG-01851}.
   */
  public static RuleSetDefinition customerMode1ForbiddenOverrideAttempt() {
    DecisionBinding illegalOverride =
        new DecisionBinding(
            "customer.orgao2.peraqfer.forbidden-erg-01851",
            SLOT_CRONOLOGIA_DATAS_VALIDA,
            DecisionSource.CUSTOMER,
            CompositionPolicy.REPLACE_EXACT,
            RuleExecutorRef.jsonLogic(parseJson("{\"==\": [1, 1]}")),
            List.of(),
            15,
            true,
            RuleDecision.DENY,
            "ERG-01851",
            List.of("draft.dtini"),
            null);
    return withCustomerBindings(List.of(illegalOverride));
  }

  /**
   * <b>Modo 2 ({@link OverridePolicy#PARAMETERIZABLE} + {@link CompositionPolicy#PARAMETERIZE}):</b>
   * Órgão 2 reduz o limite máximo de períodos aquisitivos acumulados em aberto de {@code 2} para
   * {@code 1} e o teto de faltas sem perda total para {@code 20}.
   */
  public static DecisionBinding customerMode2ParameterizeBinding() {
    return customerMode2ParameterizeBinding(1, 20);
  }

  public static DecisionBinding customerMode2ParameterizeBinding(
      int maxPeriodosAcumuladosAbertos, int maxFaltasSemPerdaTotal) {
    ObjectNode values = MAPPER.createObjectNode();
    values.put("maxPeriodosAcumuladosAbertos", maxPeriodosAcumuladosAbertos);
    values.put("maxFaltasSemPerdaTotal", maxFaltasSemPerdaTotal);
    return new DecisionBinding(
        "customer.orgao2.peraqfer.accumulation-bounds",
        SLOT_ACUMULO_E_FALTAS_LIMITES_TEMPLATE,
        DecisionSource.CUSTOMER,
        CompositionPolicy.PARAMETERIZE,
        null,
        List.of(),
        65,
        true,
        null,
        null,
        List.of(),
        new RuleParameterization(
            TEMPLATE_ACCUMULATION_AND_REDUCTION_BOUNDS_KEY,
            TEMPLATE_ACCUMULATION_AND_REDUCTION_BOUNDS_VERSION,
            values));
  }

  /**
   * <b>Modo 3 ({@link OverridePolicy#RESTRICT_ONLY} + {@link CompositionPolicy#AUGMENT}):</b>
   * Órgão 2 exige portaria/documento legal ({@code documentoLegalId != null}) sempre que houver
   * conversão em dobro ({@code diasConvertidosDobro > 0}).
   */
  public static DecisionBinding customerMode3AugmentBinding() {
    return new DecisionBinding(
        "customer.orgao2.peraqfer.require-documento-for-dobro",
        SLOT_HOOK_PRE_VALIDACAO,
        DecisionSource.CUSTOMER,
        CompositionPolicy.AUGMENT,
        RuleExecutorRef.jsonLogic(
            parseJson(
                """
                {
                  "or": [
                    {"<=": [{"var": "draft.diasConvertidosDobro"}, 0]},
                    {"!=": [{"var": "draft.documentoLegalId"}, null]}
                  ]
                }
                """)),
        List.of(),
        35,
        true,
        RuleDecision.DENY,
        "CERG-PA-301",
        List.of("draft.dtini", "draft.dtfim"),
        null);
  }

  /**
   * <b>Modo 4 ({@link OverridePolicy#REPLACEABLE} + {@link CompositionPolicy#REPLACE_EXACT}):</b>
   * Órgão 2 substitui a política de verificação de prescrição ({@code EP__VERIFICA_DT_PRESCR})
   * exigindo documento legal formal para autorizar gozo após {@code dtPrescricao}.
   */
  public static DecisionBinding customerMode4ReplaceExactBinding() {
    return new DecisionBinding(
        "customer.orgao2.peraqfer.strict-prescricao-policy",
        SLOT_PRESCRICAO_PARCELAS_ATIVAS,
        DecisionSource.CUSTOMER,
        CompositionPolicy.REPLACE_EXACT,
        RuleExecutorRef.jsonLogic(
            parseJson(
                """
                {
                  "or": [
                    {"==": [{"var": "draft.dtPrescricao"}, null]},
                    {"==": [{"var": "derived.parcelasViolandoPrescricaoCount"}, 0]},
                    {
                      "and": [
                        {"==": [{"var": "effectiveParametroConta.permiteGozoAposPrescricao"}, true]},
                        {"!=": [{"var": "draft.documentoLegalId"}, null]}
                      ]
                    }
                  ]
                }
                """)),
        List.of(),
        95,
        true,
        RuleDecision.DENY,
        "CERG-PA-401",
        List.of("draft.dtini", "draft.dtfim"),
        null);
  }

  public static RuleSetDefinition orgao2CustomizedRuleSet() {
    return withCustomerBindings(
        List.of(
            customerMode2ParameterizeBinding(),
            customerMode3AugmentBinding(),
            customerMode4ReplaceExactBinding()));
  }

  private static DecisionBinding productJsonLogicBinding(
      String bindingKey,
      String slotKey,
      int order,
      String jsonLogic,
      String falseReasonCode,
      List<String> requiredFactPaths) {
    return new DecisionBinding(
        bindingKey,
        slotKey,
        DecisionSource.PRODUCT,
        null,
        RuleExecutorRef.jsonLogic(parseJson(jsonLogic)),
        List.of(),
        order,
        true,
        RuleDecision.DENY,
        falseReasonCode,
        requiredFactPaths,
        null);
  }

  private static JsonNode parseJson(String rawJson) {
    try {
      return MAPPER.readTree(rawJson);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException(
          "Invalid static JSON Logic in PeriodoAquisitivoFeriasRuleSet", ex);
    }
  }
}
