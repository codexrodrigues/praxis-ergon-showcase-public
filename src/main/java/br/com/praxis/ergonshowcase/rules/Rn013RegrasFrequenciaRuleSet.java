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
 * Definição canônica do {@link RuleSetDefinition} para a família de regras estatutárias de
 * frequência {@code RN-013} ({@code PCK_REGRAS_FREQ.MAIN_PRE} e {@code PCK_REGRAS_FREQ.MAIN_POS}).
 *
 * <p>Decompõe o bloco monolítico legado de {@code PCK_REGRAS_FREQ} em 15 slots atômicos de decisão
 * ({@code ERG-00019} e {@code ERG-08380} a {@code ERG-08393}), eliminando a supressão grosseira por
 * {@code IF v_ep THEN} ({@code EPB__REGRAS_FREQ}) e governando a customização por órgão através dos
 * 4 modos formais do {@code praxis-rules-engine}:
 *
 * <ol>
 *   <li><b>Modo 1 ({@link OverridePolicy#FORBIDDEN}):</b> guardas estruturais e de integridade
 *       temporal ({@code ERG-00019}, {@code ERG-08382}, {@code ERG-08393}) imunes a bypass.
 *   <li><b>Modo 2 ({@link OverridePolicy#PARAMETERIZABLE} + {@link CompositionPolicy#PARAMETERIZE}):</b>
 *       parametrização tipada via {@link RuleParameterTemplate} (Contrato 1.6) sobre {@code ERG-08380}
 *       por RFC 6901 JSON Pointer sem reescrever a estrutura lógica.
 *   <li><b>Modo 3 ({@link OverridePolicy#RESTRICT_ONLY} + {@link CompositionPolicy#AUGMENT}):</b>
 *       extensão aditiva por órgão sob {@link DecisionAggregationPolicy#DENY_OVERRIDES}
 *       ({@code CERG-09001} somado a {@code ERG-08391}).
 *   <li><b>Modo 4 ({@link OverridePolicy#REPLACEABLE} + {@link CompositionPolicy#REPLACE_EXACT}):</b>
 *       substituição cirúrgica de um único slot autorizado ({@code ERG-08385} -> {@code CERG-08385})
 *       preservando todas as demais decisões do produto ativas.
 * </ol>
 */
public final class Rn013RegrasFrequenciaRuleSet {

  public static final String DOMAIN_SCOPE = "workforce.frequency-and-absence";
  public static final String RESOURCE_KEY = "administracao-pessoal.regras-frequencia";
  public static final String RULESET_KEY = "ergon.administracao-pessoal.regras-frequencia.rn013";
  public static final String OPERATION = "validate";
  public static final int VERSION = 1;

  public static final List<String> AVAILABLE_ROOTS =
      List.of("draft", "existingOverlaps", "tenant", "context");

  // DecisionSlot keys
  public static final String SLOT_PERIODO_ORDEM_VALIDA = "rn013.periodo_ordem_valida";
  public static final String SLOT_QTD_MAX_NON_NEGATIVE = "rn013.qtd_max_non_negative";
  public static final String SLOT_QTD_MIN_NON_NEGATIVE = "rn013.qtd_min_non_negative";
  public static final String SLOT_QTD_MAX_COHERENT_WITH_MIN = "rn013.qtd_max_coherent_with_min";
  public static final String SLOT_CONSEC_MES_NON_NEGATIVE = "rn013.consec_mes_non_negative";
  public static final String SLOT_CONSEC_ANO_NON_NEGATIVE = "rn013.consec_ano_non_negative";
  public static final String SLOT_INTERP_MES_NON_NEGATIVE = "rn013.interp_mes_non_negative";
  public static final String SLOT_INTERP_ANO_NON_NEGATIVE = "rn013.interp_ano_non_negative";
  public static final String SLOT_INT_PRORROG_NON_NEGATIVE = "rn013.int_prorrog_non_negative";
  public static final String SLOT_INTERV_MIN_NON_NEGATIVE = "rn013.interv_min_non_negative";
  public static final String SLOT_CONSEC_MES_CALENDAR_LIMIT = "rn013.consec_mes_calendar_limit";
  public static final String SLOT_INTERP_MES_CALENDAR_LIMIT = "rn013.interp_mes_calendar_limit";
  public static final String SLOT_CONSEC_ANO_CALENDAR_LIMIT = "rn013.consec_ano_calendar_limit";
  public static final String SLOT_INTERP_ANO_CALENDAR_LIMIT = "rn013.interp_ano_calendar_limit";
  public static final String SLOT_CONCOMITANCIA_VIGENCIA_OVERLAP =
      "rn013.concomitancia_vigencia_overlap";

  // Parameter template & Java executor keys
  public static final String TEMPLATE_QTD_MAX_BOUNDS_KEY = "ergon.rn013:qtd-max-bounds";
  public static final String TEMPLATE_QTD_MAX_BOUNDS_VERSION = "1.0";
  public static final String JAVA_EXECUTOR_CONCOMITANCIA_KEY =
      "ergon.rn013:concomitancia-vigencia-overlap";
  public static final String JAVA_EXECUTOR_VERSION = "1.0";

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static final String ERG08380_TEMPLATE_EXPRESSION =
      """
      {
        "or": [
          {"==": [{"var": "draft.qtdMaxDias"}, null]},
          {
            "and": [
              {">=": [{"var": "draft.qtdMaxDias"}, 0]},
              {"<=": [{"var": "draft.qtdMaxDias"}, 3650]}
            ]
          }
        ]
      }
      """;

  private Rn013RegrasFrequenciaRuleSet() {}

  /** Constrói a referência canônica do {@link RuleSetDefinition} da {@code RN-013}. */
  public static RuleSetRef ruleSetRef() {
    return new RuleSetRef(DOMAIN_SCOPE, RESOURCE_KEY, RULESET_KEY, OPERATION, VERSION);
  }

  /**
   * Constrói o {@link RuleParameterTemplate} governado para parametrização de limites de
   * {@code qtdMaxDias} ({@code ERG-08380}) sob o Contrato 1.6 do {@code praxis-rules-engine}.
   */
  public static RuleParameterTemplate qtdMaxBoundsTemplate() {
    return new RuleParameterTemplate(
        TEMPLATE_QTD_MAX_BOUNDS_KEY,
        TEMPLATE_QTD_MAX_BOUNDS_VERSION,
        RuleExecutorRef.jsonLogic(parseJson(ERG08380_TEMPLATE_EXPRESSION)),
        RuleDecision.DENY,
        "ERG-08380",
        List.of("draft.dtini"),
        List.of(
            new RuleParameterDefinition(
                "minAllowedQty",
                RuleParameterType.NUMBER,
                true,
                false,
                false,
                "days",
                MAPPER.getNodeFactory().numberNode(0),
                BigDecimal.ZERO,
                new BigDecimal("365"),
                List.of(),
                List.of("/or/1/and/0/>=/1")),
            new RuleParameterDefinition(
                "maxAllowedQty",
                RuleParameterType.NUMBER,
                true,
                false,
                false,
                "days",
                MAPPER.getNodeFactory().numberNode(3650),
                BigDecimal.ONE,
                new BigDecimal("3650"),
                List.of(),
                List.of("/or/1/and/1/<=/1"))));
  }

  /** Retorna a lista ordenada dos 15 {@link DecisionSlot}s da {@code RN-013}. */
  public static List<DecisionSlot> productSlots() {
    return List.of(
        // Stage 1: PROTECTED_GUARD (OverridePolicy.FORBIDDEN)
        new DecisionSlot(
            SLOT_PERIODO_ORDEM_VALIDA,
            DecisionStage.PROTECTED_GUARD,
            SlotCardinality.SINGLE,
            OverridePolicy.FORBIDDEN,
            DecisionAggregationPolicy.SINGLE_RESULT,
            null),

        // Stage 2: DOMAIN_DECISION (ERG-08380 a ERG-08392)
        new DecisionSlot(
            SLOT_QTD_MAX_NON_NEGATIVE,
            DecisionStage.DOMAIN_DECISION,
            SlotCardinality.SINGLE,
            OverridePolicy.PARAMETERIZABLE,
            DecisionAggregationPolicy.SINGLE_RESULT,
            qtdMaxBoundsTemplate()),
        new DecisionSlot(
            SLOT_QTD_MIN_NON_NEGATIVE,
            DecisionStage.DOMAIN_DECISION,
            SlotCardinality.MULTIPLE,
            OverridePolicy.RESTRICT_ONLY,
            DecisionAggregationPolicy.DENY_OVERRIDES,
            null),
        new DecisionSlot(
            SLOT_QTD_MAX_COHERENT_WITH_MIN,
            DecisionStage.DOMAIN_DECISION,
            SlotCardinality.SINGLE,
            OverridePolicy.FORBIDDEN,
            DecisionAggregationPolicy.SINGLE_RESULT,
            null),
        new DecisionSlot(
            SLOT_CONSEC_MES_NON_NEGATIVE,
            DecisionStage.DOMAIN_DECISION,
            SlotCardinality.MULTIPLE,
            OverridePolicy.RESTRICT_ONLY,
            DecisionAggregationPolicy.DENY_OVERRIDES,
            null),
        new DecisionSlot(
            SLOT_CONSEC_ANO_NON_NEGATIVE,
            DecisionStage.DOMAIN_DECISION,
            SlotCardinality.MULTIPLE,
            OverridePolicy.RESTRICT_ONLY,
            DecisionAggregationPolicy.DENY_OVERRIDES,
            null),
        new DecisionSlot(
            SLOT_INTERP_MES_NON_NEGATIVE,
            DecisionStage.DOMAIN_DECISION,
            SlotCardinality.SINGLE,
            OverridePolicy.REPLACEABLE,
            DecisionAggregationPolicy.SINGLE_RESULT,
            null),
        new DecisionSlot(
            SLOT_INTERP_ANO_NON_NEGATIVE,
            DecisionStage.DOMAIN_DECISION,
            SlotCardinality.MULTIPLE,
            OverridePolicy.RESTRICT_ONLY,
            DecisionAggregationPolicy.DENY_OVERRIDES,
            null),
        new DecisionSlot(
            SLOT_INT_PRORROG_NON_NEGATIVE,
            DecisionStage.DOMAIN_DECISION,
            SlotCardinality.MULTIPLE,
            OverridePolicy.RESTRICT_ONLY,
            DecisionAggregationPolicy.DENY_OVERRIDES,
            null),
        new DecisionSlot(
            SLOT_INTERV_MIN_NON_NEGATIVE,
            DecisionStage.DOMAIN_DECISION,
            SlotCardinality.MULTIPLE,
            OverridePolicy.RESTRICT_ONLY,
            DecisionAggregationPolicy.DENY_OVERRIDES,
            null),
        new DecisionSlot(
            SLOT_CONSEC_MES_CALENDAR_LIMIT,
            DecisionStage.DOMAIN_DECISION,
            SlotCardinality.MULTIPLE,
            OverridePolicy.RESTRICT_ONLY,
            DecisionAggregationPolicy.DENY_OVERRIDES,
            null),
        new DecisionSlot(
            SLOT_INTERP_MES_CALENDAR_LIMIT,
            DecisionStage.DOMAIN_DECISION,
            SlotCardinality.MULTIPLE,
            OverridePolicy.RESTRICT_ONLY,
            DecisionAggregationPolicy.DENY_OVERRIDES,
            null),
        new DecisionSlot(
            SLOT_CONSEC_ANO_CALENDAR_LIMIT,
            DecisionStage.DOMAIN_DECISION,
            SlotCardinality.MULTIPLE,
            OverridePolicy.RESTRICT_ONLY,
            DecisionAggregationPolicy.DENY_OVERRIDES,
            null),
        new DecisionSlot(
            SLOT_INTERP_ANO_CALENDAR_LIMIT,
            DecisionStage.DOMAIN_DECISION,
            SlotCardinality.MULTIPLE,
            OverridePolicy.RESTRICT_ONLY,
            DecisionAggregationPolicy.DENY_OVERRIDES,
            null),

        // Stage 3: POST_DECISION (ERG-08393 - Concomitância de vigência no escopo funcional)
        new DecisionSlot(
            SLOT_CONCOMITANCIA_VIGENCIA_OVERLAP,
            DecisionStage.POST_DECISION,
            SlotCardinality.SINGLE,
            OverridePolicy.FORBIDDEN,
            DecisionAggregationPolicy.SINGLE_RESULT,
            null));
  }

  /** Retorna os 15 {@link DecisionBinding}s padrão do produto Ergon ({@code DecisionSource.PRODUCT}). */
  public static List<DecisionBinding> productBindings() {
    List<String> baseRequiredFacts = List.of("draft.dtini");
    return List.of(
        // 1. ERG-00019: DTFIM == null || date(DTFIM) >= date(DTINI)
        productJsonLogicBinding(
            "product.ergon.rn013.erg00019",
            SLOT_PERIODO_ORDEM_VALIDA,
            10,
            """
            {
              "or": [
                {"==": [{"var": "draft.dtfim"}, null]},
                {">=": [{"date": [{"var": "draft.dtfim"}]}, {"date": [{"var": "draft.dtini"}]}]}
              ]
            }
            """,
            "ERG-00019",
            baseRequiredFacts),

        // 2. ERG-08380: QTD_MAX == null || (QTD_MAX >= 0 && QTD_MAX <= 3650)
        productJsonLogicBinding(
            "product.ergon.rn013.erg08380",
            SLOT_QTD_MAX_NON_NEGATIVE,
            20,
            ERG08380_TEMPLATE_EXPRESSION,
            "ERG-08380",
            baseRequiredFacts),

        // 3. ERG-08381: QTD_MIN == null || QTD_MIN >= 0
        productJsonLogicBinding(
            "product.ergon.rn013.erg08381",
            SLOT_QTD_MIN_NON_NEGATIVE,
            30,
            """
            {
              "or": [
                {"==": [{"var": "draft.qtdMinDias"}, null]},
                {">=": [{"var": "draft.qtdMinDias"}, 0]}
              ]
            }
            """,
            "ERG-08381",
            baseRequiredFacts),

        // 4. ERG-08382: QTD_MAX == null || QTD_MIN == null || QTD_MAX >= QTD_MIN
        productJsonLogicBinding(
            "product.ergon.rn013.erg08382",
            SLOT_QTD_MAX_COHERENT_WITH_MIN,
            40,
            """
            {
              "or": [
                {"==": [{"var": "draft.qtdMaxDias"}, null]},
                {"==": [{"var": "draft.qtdMinDias"}, null]},
                {">=": [{"var": "draft.qtdMaxDias"}, {"var": "draft.qtdMinDias"}]}
              ]
            }
            """,
            "ERG-08382",
            baseRequiredFacts),

        // 5. ERG-08383: QTD_MAX_CONSEC_MES == null || QTD_MAX_CONSEC_MES >= 0 (e par prazo/tipoUnidadePrazo consistente)
        productJsonLogicBinding(
            "product.ergon.rn013.erg08383",
            SLOT_CONSEC_MES_NON_NEGATIVE,
            50,
            """
            {
              "and": [
                {
                  "or": [
                    {"==": [{"var": "draft.qtdMaxConsecMes"}, null]},
                    {">=": [{"var": "draft.qtdMaxConsecMes"}, 0]}
                  ]
                },
                {
                  "or": [
                    {
                      "and": [
                        {"==": [{"var": "draft.ruleParams.prazo"}, null]},
                        {"==": [{"var": "draft.ruleParams.tipoUnidadePrazo"}, null]}
                      ]
                    },
                    {
                      "and": [
                        {"!=": [{"var": "draft.ruleParams.prazo"}, null]},
                        {"!=": [{"var": "draft.ruleParams.tipoUnidadePrazo"}, null]},
                        {">=": [{"var": "draft.ruleParams.prazo"}, 0]}
                      ]
                    }
                  ]
                }
              ]
            }
            """,
            "ERG-08383",
            baseRequiredFacts),

        // 6. ERG-08384: QTD_MAX_CONSEC_ANO == null || QTD_MAX_CONSEC_ANO >= 0
        productJsonLogicBinding(
            "product.ergon.rn013.erg08384",
            SLOT_CONSEC_ANO_NON_NEGATIVE,
            60,
            """
            {
              "or": [
                {"==": [{"var": "draft.qtdMaxConsecAno"}, null]},
                {">=": [{"var": "draft.qtdMaxConsecAno"}, 0]}
              ]
            }
            """,
            "ERG-08384",
            baseRequiredFacts),

        // 7. ERG-08385: QTD_MAX_INTERP_MES == null || QTD_MAX_INTERP_MES >= 0 (REPLACEABLE)
        productJsonLogicBinding(
            "product.ergon.rn013.erg08385",
            SLOT_INTERP_MES_NON_NEGATIVE,
            70,
            """
            {
              "or": [
                {"==": [{"var": "draft.qtdMaxInterpMes"}, null]},
                {">=": [{"var": "draft.qtdMaxInterpMes"}, 0]}
              ]
            }
            """,
            "ERG-08385",
            baseRequiredFacts),

        // 8. ERG-08386: QTD_MAX_INTERP_ANO == null || QTD_MAX_INTERP_ANO >= 0
        productJsonLogicBinding(
            "product.ergon.rn013.erg08386",
            SLOT_INTERP_ANO_NON_NEGATIVE,
            80,
            """
            {
              "or": [
                {"==": [{"var": "draft.qtdMaxInterpAno"}, null]},
                {">=": [{"var": "draft.qtdMaxInterpAno"}, 0]}
              ]
            }
            """,
            "ERG-08386",
            baseRequiredFacts),

        // 9. ERG-08387: INT_PRORROG == null || INT_PRORROG >= 0 (e coerência com ruleParams.prorrogacao S/N)
        productJsonLogicBinding(
            "product.ergon.rn013.erg08387",
            SLOT_INT_PRORROG_NON_NEGATIVE,
            90,
            """
            {
              "and": [
                {
                  "or": [
                    {"==": [{"var": "draft.intProrrogDias"}, null]},
                    {">=": [{"var": "draft.intProrrogDias"}, 0]}
                  ]
                },
                {
                  "or": [
                    {"==": [{"var": "draft.ruleParams.prorrogacao"}, null]},
                    {"==": [{"var": "draft.ruleParams.prorrogacao"}, "S"]},
                    {
                      "and": [
                        {"==": [{"var": "draft.ruleParams.prorrogacao"}, "N"]},
                        {
                          "or": [
                            {"==": [{"var": "draft.intProrrogDias"}, null]},
                            {"==": [{"var": "draft.intProrrogDias"}, 0]}
                          ]
                        }
                      ]
                    }
                  ]
                }
              ]
            }
            """,
            "ERG-08387",
            baseRequiredFacts),

        // 10. ERG-08388: INTERV_MIN == null || INTERV_MIN >= 0 (e par intersticio/tipoUnidadeInterst consistente)
        productJsonLogicBinding(
            "product.ergon.rn013.erg08388",
            SLOT_INTERV_MIN_NON_NEGATIVE,
            100,
            """
            {
              "and": [
                {
                  "or": [
                    {"==": [{"var": "draft.intervMinDias"}, null]},
                    {">=": [{"var": "draft.intervMinDias"}, 0]}
                  ]
                },
                {
                  "or": [
                    {
                      "and": [
                        {"==": [{"var": "draft.ruleParams.intersticio"}, null]},
                        {"==": [{"var": "draft.ruleParams.tipoUnidadeInterst"}, null]}
                      ]
                    },
                    {
                      "and": [
                        {"!=": [{"var": "draft.ruleParams.intersticio"}, null]},
                        {"!=": [{"var": "draft.ruleParams.tipoUnidadeInterst"}, null]},
                        {">=": [{"var": "draft.ruleParams.intersticio"}, 0]}
                      ]
                    }
                  ]
                }
              ]
            }
            """,
            "ERG-08388",
            baseRequiredFacts),

        // 11. ERG-08389: QTD_MAX_CONSEC_MES == null || QTD_MAX_CONSEC_MES <= 31
        productJsonLogicBinding(
            "product.ergon.rn013.erg08389",
            SLOT_CONSEC_MES_CALENDAR_LIMIT,
            110,
            """
            {
              "or": [
                {"==": [{"var": "draft.qtdMaxConsecMes"}, null]},
                {"<=": [{"var": "draft.qtdMaxConsecMes"}, 31]}
              ]
            }
            """,
            "ERG-08389",
            baseRequiredFacts),

        // 12. ERG-08390: QTD_MAX_INTERP_MES == null || QTD_MAX_INTERP_MES <= 31
        productJsonLogicBinding(
            "product.ergon.rn013.erg08390",
            SLOT_INTERP_MES_CALENDAR_LIMIT,
            120,
            """
            {
              "or": [
                {"==": [{"var": "draft.qtdMaxInterpMes"}, null]},
                {"<=": [{"var": "draft.qtdMaxInterpMes"}, 31]}
              ]
            }
            """,
            "ERG-08390",
            baseRequiredFacts),

        // 13. ERG-08391: QTD_MAX_CONSEC_ANO == null || QTD_MAX_CONSEC_ANO <= 366 (RESTRICT_ONLY / MULTIPLE)
        productJsonLogicBinding(
            "product.ergon.rn013.erg08391",
            SLOT_CONSEC_ANO_CALENDAR_LIMIT,
            130,
            """
            {
              "or": [
                {"==": [{"var": "draft.qtdMaxConsecAno"}, null]},
                {"<=": [{"var": "draft.qtdMaxConsecAno"}, 366]}
              ]
            }
            """,
            "ERG-08391",
            baseRequiredFacts),

        // 14. ERG-08392: QTD_MAX_INTERP_ANO == null || QTD_MAX_INTERP_ANO <= 366
        productJsonLogicBinding(
            "product.ergon.rn013.erg08392",
            SLOT_INTERP_ANO_CALENDAR_LIMIT,
            140,
            """
            {
              "or": [
                {"==": [{"var": "draft.qtdMaxInterpAno"}, null]},
                {"<=": [{"var": "draft.qtdMaxInterpAno"}, 366]}
              ]
            }
            """,
            "ERG-08392",
            baseRequiredFacts),

        // 15. ERG-08393: Concomitância de vigência no escopo funcional (Executor Java versionado)
        new DecisionBinding(
            "product.ergon.rn013.erg08393",
            SLOT_CONCOMITANCIA_VIGENCIA_OVERLAP,
            DecisionSource.PRODUCT,
            null,
            RuleExecutorRef.java(JAVA_EXECUTOR_CONCOMITANCIA_KEY, JAVA_EXECUTOR_VERSION),
            List.of(),
            150,
            true,
            null,
            null,
            List.of("draft.dtini", "existingOverlaps"),
            null));
  }

  /** Constrói o {@link RuleSetDefinition} baseline do produto Ergon (Órgão padrão {@code empCodigo = 1}). */
  public static RuleSetDefinition productRuleSet() {
    return new RuleSetDefinition(
        ruleSetRef(),
        AVAILABLE_ROOTS,
        productSlots(),
        productBindings(),
        RuleRuntimeCompatibility.current(),
        RuleFailPolicy.FAIL_CLOSED);
  }

  /**
   * Compõe o {@link RuleSetDefinition} do produto com bindings de customização de órgão
   * ({@link DecisionSource#CUSTOMER}).
   *
   * <p>Para slots unitários substituídos ou parametrizados ({@link CompositionPolicy#PARAMETERIZE}
   * ou {@link CompositionPolicy#REPLACE_EXACT}), desativa o binding default do produto naquele slot
   * para que apenas o binding derivado/substituto do órgão permaneça habilitado; para slots
   * múltiplos ({@link CompositionPolicy#AUGMENT}), mantém ambos habilitados sob
   * {@link DecisionAggregationPolicy#DENY_OVERRIDES}.
   */
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
   * <b>Modo 1 ({@link OverridePolicy#FORBIDDEN} — Prova Negativa):</b> tenta acoplar um binding de
   * cliente ({@link CompositionPolicy#REPLACE_EXACT}) ao slot protegido {@code ERG-00019}
   * ({@link DecisionStage#PROTECTED_GUARD}). O {@code PraxisRulePlanCompiler} deve rejeitar a
   * compilação do plano com {@code RulePlanIssueCode.PLAN_OVERRIDE_INVALID}.
   */
  public static RuleSetDefinition customerMode1ForbiddenOverrideAttempt() {
    DecisionBinding illegalOverride =
        new DecisionBinding(
            "customer.emp2.rn013.bypass_erg00019",
            SLOT_PERIODO_ORDEM_VALIDA,
            DecisionSource.CUSTOMER,
            CompositionPolicy.REPLACE_EXACT,
            RuleExecutorRef.jsonLogic(parseJson("{\"==\": [1, 1]}")),
            List.of(),
            15,
            true,
            RuleDecision.DENY,
            "ERG-00019",
            List.of("draft.dtini"),
            null);
    return withCustomerBindings(List.of(illegalOverride));
  }

  /**
   * <b>Modo 2 ({@link OverridePolicy#PARAMETERIZABLE} + {@link CompositionPolicy#PARAMETERIZE}):</b>
   * ajusta os limites escalares ({@code minAllowedQty}, {@code maxAllowedQty}) do slot
   * {@code ERG-08380} para o órgão {@code empCodigo = 2} sem alterar a expressão JSON Logic.
   */
  public static DecisionBinding customerMode2ParameterizeBinding(int minAllowedQty, int maxAllowedQty) {
    ObjectNode values = MAPPER.createObjectNode();
    values.put("minAllowedQty", minAllowedQty);
    values.put("maxAllowedQty", maxAllowedQty);
    return new DecisionBinding(
        "customer.emp2.rn013.parameterize_erg08380",
        SLOT_QTD_MAX_NON_NEGATIVE,
        DecisionSource.CUSTOMER,
        CompositionPolicy.PARAMETERIZE,
        null,
        List.of(),
        25,
        true,
        null,
        null,
        List.of(),
        new RuleParameterization(
            TEMPLATE_QTD_MAX_BOUNDS_KEY, TEMPLATE_QTD_MAX_BOUNDS_VERSION, values));
  }

  /**
   * <b>Modo 3 ({@link OverridePolicy#RESTRICT_ONLY} + {@link CompositionPolicy#AUGMENT}):</b>
   * adiciona a regra específica do órgão {@code empCodigo = 2} ({@code CERG-09001}) exigindo
   * {@code documentoLegalId != null} quando {@code qtdMaxConsecAno > 30}, preservando a regra do produto
   * {@code ERG-08391} ativa sob {@link DecisionAggregationPolicy#DENY_OVERRIDES}.
   */
  public static DecisionBinding customerMode3AugmentBinding() {
    return new DecisionBinding(
        "customer.emp2.rn013.augment_cerg09001",
        SLOT_CONSEC_ANO_CALENDAR_LIMIT,
        DecisionSource.CUSTOMER,
        CompositionPolicy.AUGMENT,
        RuleExecutorRef.jsonLogic(
            parseJson(
                """
                {
                  "or": [
                    {"==": [{"var": "draft.qtdMaxConsecAno"}, null]},
                    {"<=": [{"var": "draft.qtdMaxConsecAno"}, 30]},
                    {"!=": [{"var": "draft.documentoLegalId"}, null]}
                  ]
                }
                """)),
        List.of(),
        135,
        true,
        RuleDecision.DENY,
        "CERG-09001",
        List.of("draft.dtini"),
        null);
  }

  /**
   * <b>Modo 4 ({@link OverridePolicy#REPLACEABLE} + {@link CompositionPolicy#REPLACE_EXACT}):</b>
   * substitui exclusivamente o slot autorizado {@link #SLOT_INTERP_MES_NON_NEGATIVE}
   * ({@code ERG-08385}) para o órgão {@code empCodigo = 2} por {@code CERG-08385} (exigindo
   * {@code qtdMaxInterpMes >= 1} quando a regra for aplicável), sem desligar nenhuma das outras 14
   * decisões do produto.
   */
  public static DecisionBinding customerMode4ReplaceExactBinding() {
    return new DecisionBinding(
        "customer.emp2.rn013.replace_erg08385",
        SLOT_INTERP_MES_NON_NEGATIVE,
        DecisionSource.CUSTOMER,
        CompositionPolicy.REPLACE_EXACT,
        RuleExecutorRef.jsonLogic(
            parseJson(
                """
                {
                  "or": [
                    {"==": [{"var": "draft.naoSeAplica"}, true]},
                    {
                      "and": [
                        {"!=": [{"var": "draft.qtdMaxInterpMes"}, null]},
                        {">=": [{"var": "draft.qtdMaxInterpMes"}, 1]}
                      ]
                    }
                  ]
                }
                """)),
        List.of(),
        75,
        true,
        RuleDecision.DENY,
        "CERG-08385",
        List.of("draft.dtini"),
        null);
  }

  /**
   * Constrói o {@link RuleSetDefinition} customizado do Órgão 2 ({@code empCodigo = 2}) combinando
   * os 3 modos válidos de customização governada:
   * <ul>
   *   <li>Modo 2 ({@code PARAMETERIZE}): reduz o teto parametrizado de {@code qtdMaxDias} para {@code 12} dias;
   *   <li>Modo 3 ({@code AUGMENT}): adiciona {@code CERG-09001} sob {@code DENY_OVERRIDES};
   *   <li>Modo 4 ({@code REPLACE_EXACT}): substitui cirurgicamente apenas {@code ERG-08385} por {@code CERG-08385}.
   * </ul>
   */
  public static RuleSetDefinition orgao2CustomizedRuleSet() {
    return withCustomerBindings(
        List.of(
            customerMode2ParameterizeBinding(0, 12),
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
    JsonNode parsed = parseJson(jsonLogic);
    return new DecisionBinding(
        bindingKey,
        slotKey,
        DecisionSource.PRODUCT,
        null,
        RuleExecutorRef.jsonLogic(parsed),
        List.of(),
        order,
        true,
        RuleDecision.DENY,
        falseReasonCode,
        ErgonOntologyRuleSetSupport.mergeRequiredRoots(requiredFactPaths, parsed),
        null);
  }

  private static JsonNode parseJson(String rawJson) {
    try {
      return MAPPER.readTree(rawJson);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Invalid static JSON Logic in Rn013RegrasFrequenciaRuleSet", ex);
    }
  }
}
