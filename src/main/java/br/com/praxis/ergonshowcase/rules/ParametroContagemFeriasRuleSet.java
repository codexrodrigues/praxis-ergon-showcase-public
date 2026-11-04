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
 * Definição canônica do {@link RuleSetDefinition} para validação determinística de Parâmetros de
 * Contagem de Férias, Licença Especial e Progressão ({@code PCK_PARAMETROS_CONTA.MAIN_PRE} e
 * {@code MAIN_POS}) sobre o {@code praxis-rules-engine:0.1.0-beta.17} (Contrato {@code 1.6}).
 */
public final class ParametroContagemFeriasRuleSet {

  public static final String DOMAIN_SCOPE = "workforce.vacation-and-leave";
  public static final String RESOURCE_KEY = "administracao-pessoal.parametros-contagem-ferias";
  public static final String RULE_SET_KEY =
      "ergon.administracao-pessoal.parametros-contagem-ferias.matriz";
  public static final String OPERATION = "validate";
  public static final int VERSION = 1;

  public static final List<String> AVAILABLE_ROOTS =
      List.of("draft", "derived", "scope", "existingOverlaps", "tenant", "context");

  public static final String TEMPLATE_VACATION_DAYS_BOUNDS_KEY =
      "ergon.parametros-conta:vacation-days-bounds";
  public static final String TEMPLATE_VACATION_DAYS_BOUNDS_VERSION = "1.0";
  public static final String TEMPLATE_VACATION_DAYS_BOUNDS =
      TEMPLATE_VACATION_DAYS_BOUNDS_KEY + "@" + TEMPLATE_VACATION_DAYS_BOUNDS_VERSION;

  public static final String JAVA_EXECUTOR_CONCOMITANCIA_KEY =
      "ergon.parametros-conta:concomitancia-vigencia-overlap";
  public static final String JAVA_EXECUTOR_VERSION = "1.0";
  public static final String EXECUTOR_CONCOMITANCIA_VIGENCIA_OVERLAP =
      JAVA_EXECUTOR_CONCOMITANCIA_KEY + "@" + JAVA_EXECUTOR_VERSION;

  public static final String SLOT_VIGENCIA_ORDEM_VALIDA =
      "parametros-conta.guard.vigencia-ordem-valida";
  public static final String SLOT_HOOK_PRE_VALIDACAO =
      "parametros-conta.hook.pre-validacao-orgao";
  public static final String SLOT_PRESCRICAO_E_FLAGS_GOZO =
      "parametros-conta.core.prescricao-e-flags-gozo";
  public static final String SLOT_FERIAS_HABILITADAS_OBRIGATORIOS =
      "parametros-conta.core.ferias-habilitadas-obrigatorios";
  public static final String SLOT_FERIAS_LIMITES_DIAS_TEMPLATE =
      "parametros-conta.core.ferias-limites-dias-template";
  public static final String SLOT_PARCELAMENTO_COERENCIA =
      "parametros-conta.core.parcelamento-coerencia";
  public static final String SLOT_ABONO_PECUNIARIO_FRACAO_LIMITES =
      "parametros-conta.core.abono-pecuniario-fracao-limites";
  public static final String SLOT_FERIAS_DESABILITADAS_SEM_LIMITES =
      "parametros-conta.core.ferias-desabilitadas-sem-limites";
  public static final String SLOT_CONTAGEM_DOBRO_FERIAS_COERENCIA =
      "parametros-conta.core.contagem-dobro-ferias-coerencia";
  public static final String SLOT_LICENCA_ESPECIAL_PROGRESSAO_COERENCIA =
      "parametros-conta.core.licenca-especial-progressao-coerencia";
  public static final String SLOT_CONCOMITANCIA_VIGENCIA_ESCOPO =
      "parametros-conta.post.concomitancia-vigencia-escopo";
  public static final String SLOT_HOOK_POS_VALIDACAO =
      "parametros-conta.hook.pos-validacao-orgao";

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static final String ERG01806_TEMPLATE_EXPRESSION =
      """
      {
        "or": [
          {"!=": [{"var": "draft.temFerias"}, true]},
          {"==": [{"var": "draft.totDiasFer"}, null]},
          {"==": [{"var": "draft.maxPerFer"}, null]},
          {
            "and": [
              {">=": [{"var": "draft.totDiasFer"}, 1]},
              {"<=": [{"var": "draft.totDiasFer"}, 90]},
              {"<=": [{"var": "draft.maxPerFer"}, 6]}
            ]
          }
        ]
      }
      """;

  private ParametroContagemFeriasRuleSet() {}

  public static RuleSetRef ruleSetRef() {
    return new RuleSetRef(DOMAIN_SCOPE, RESOURCE_KEY, RULE_SET_KEY, OPERATION, VERSION);
  }

  /**
   * Template parametrizável para limites corporativos de dias de direito e teto de parcelas
   * ({@code ergon.parametros-conta:vacation-days-bounds@1.0}).
   */
  public static RuleParameterTemplate vacationDaysBoundsTemplate() {
    return new RuleParameterTemplate(
        TEMPLATE_VACATION_DAYS_BOUNDS_KEY,
        TEMPLATE_VACATION_DAYS_BOUNDS_VERSION,
        RuleExecutorRef.jsonLogic(parseJson(ERG01806_TEMPLATE_EXPRESSION)),
        RuleDecision.DENY,
        "ERG-01806",
        List.of("draft.dtini", "draft.temFerias"),
        List.of(
            new RuleParameterDefinition(
                "minDiasDireitoPadrao",
                RuleParameterType.NUMBER,
                true,
                false,
                false,
                "days",
                MAPPER.getNodeFactory().numberNode(1),
                BigDecimal.ONE,
                new BigDecimal("60"),
                List.of(),
                List.of("/or/3/and/0/>=/1")),
            new RuleParameterDefinition(
                "maxDiasDireitoPadrao",
                RuleParameterType.NUMBER,
                true,
                false,
                false,
                "days",
                MAPPER.getNodeFactory().numberNode(90),
                BigDecimal.ONE,
                new BigDecimal("120"),
                List.of(),
                List.of("/or/3/and/1/<=/1")),
            new RuleParameterDefinition(
                "maxParcelasTeto",
                RuleParameterType.NUMBER,
                true,
                false,
                false,
                "parcels",
                MAPPER.getNodeFactory().numberNode(6),
                BigDecimal.ONE,
                new BigDecimal("12"),
                List.of(),
                List.of("/or/3/and/2/<=/1"))));
  }

  public static List<DecisionSlot> productSlots() {
    return List.of(
        new DecisionSlot(
            SLOT_VIGENCIA_ORDEM_VALIDA,
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
            SLOT_PRESCRICAO_E_FLAGS_GOZO,
            DecisionStage.DOMAIN_DECISION,
            SlotCardinality.MULTIPLE,
            OverridePolicy.FORBIDDEN,
            DecisionAggregationPolicy.DENY_OVERRIDES,
            null),
        new DecisionSlot(
            SLOT_FERIAS_HABILITADAS_OBRIGATORIOS,
            DecisionStage.DOMAIN_DECISION,
            SlotCardinality.MULTIPLE,
            OverridePolicy.FORBIDDEN,
            DecisionAggregationPolicy.DENY_OVERRIDES,
            null),
        new DecisionSlot(
            SLOT_FERIAS_LIMITES_DIAS_TEMPLATE,
            DecisionStage.DOMAIN_DECISION,
            SlotCardinality.SINGLE,
            OverridePolicy.PARAMETERIZABLE,
            DecisionAggregationPolicy.SINGLE_RESULT,
            vacationDaysBoundsTemplate()),
        new DecisionSlot(
            SLOT_PARCELAMENTO_COERENCIA,
            DecisionStage.DOMAIN_DECISION,
            SlotCardinality.MULTIPLE,
            OverridePolicy.RESTRICT_ONLY,
            DecisionAggregationPolicy.DENY_OVERRIDES,
            null),
        new DecisionSlot(
            SLOT_ABONO_PECUNIARIO_FRACAO_LIMITES,
            DecisionStage.DOMAIN_DECISION,
            SlotCardinality.SINGLE,
            OverridePolicy.REPLACEABLE,
            DecisionAggregationPolicy.SINGLE_RESULT,
            null),
        new DecisionSlot(
            SLOT_FERIAS_DESABILITADAS_SEM_LIMITES,
            DecisionStage.DOMAIN_DECISION,
            SlotCardinality.MULTIPLE,
            OverridePolicy.FORBIDDEN,
            DecisionAggregationPolicy.DENY_OVERRIDES,
            null),
        new DecisionSlot(
            SLOT_CONTAGEM_DOBRO_FERIAS_COERENCIA,
            DecisionStage.DOMAIN_DECISION,
            SlotCardinality.MULTIPLE,
            OverridePolicy.RESTRICT_ONLY,
            DecisionAggregationPolicy.DENY_OVERRIDES,
            null),
        new DecisionSlot(
            SLOT_LICENCA_ESPECIAL_PROGRESSAO_COERENCIA,
            DecisionStage.DOMAIN_DECISION,
            SlotCardinality.MULTIPLE,
            OverridePolicy.RESTRICT_ONLY,
            DecisionAggregationPolicy.DENY_OVERRIDES,
            null),
        new DecisionSlot(
            SLOT_CONCOMITANCIA_VIGENCIA_ESCOPO,
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
    List<String> baseFacts = List.of("draft.dtini", "draft.temFerias");
    return List.of(
        // 1. ERG-01873: dtini <= dtfim
        productJsonLogicBinding(
            "product.parametros-conta.erg-01873",
            SLOT_VIGENCIA_ORDEM_VALIDA,
            10,
            """
            {
              "or": [
                {"==": [{"var": "draft.dtfim"}, null]},
                {">=": [{"date": [{"var": "draft.dtfim"}]}, {"date": [{"var": "draft.dtini"}]}]}
              ]
            }
            """,
            "ERG-01873",
            baseFacts),
        // 3a. ERG-01870: criterioPrescricao em ['I', 'F']
        productJsonLogicBinding(
            "product.parametros-conta.erg-01870",
            SLOT_PRESCRICAO_E_FLAGS_GOZO,
            30,
            """
            {
              "in": [{"var": "draft.criterioPrescricao"}, ["I", "F"]]
            }
            """,
            "ERG-01870",
            baseFacts),
        // 3b. ERG-01871: flags de gozo antes do término do PA e após prescrição obrigatórias
        productJsonLogicBinding(
            "product.parametros-conta.erg-01871",
            SLOT_PRESCRICAO_E_FLAGS_GOZO,
            31,
            """
            {
              "and": [
                {"!=": [{"var": "draft.permiteGozoAntesPaFer"}, null]},
                {"!=": [{"var": "draft.permiteGozoAposPrescricao"}, null]}
              ]
            }
            """,
            "ERG-01871",
            baseFacts),
        // 3c. ERG-01872: flag permiteLancConcPa obrigatória
        productJsonLogicBinding(
            "product.parametros-conta.erg-01872",
            SLOT_PRESCRICAO_E_FLAGS_GOZO,
            32,
            """
            {
              "!=": [{"var": "draft.permiteLancConcPa"}, null]
            }
            """,
            "ERG-01872",
            baseFacts),
        // 4a. ERG-01800: totDiasFer obrigatório quando temFerias == true
        productJsonLogicBinding(
            "product.parametros-conta.erg-01800",
            SLOT_FERIAS_HABILITADAS_OBRIGATORIOS,
            40,
            """
            {
              "or": [
                {"!=": [{"var": "draft.temFerias"}, true]},
                {
                  "and": [
                    {"!=": [{"var": "draft.totDiasFer"}, null]},
                    {">": [{"var": "draft.totDiasFer"}, 0]}
                  ]
                }
              ]
            }
            """,
            "ERG-01800",
            baseFacts),
        // 4b. ERG-01801: limites de períodos e dias por parcela obrigatórios quando temFerias == true
        productJsonLogicBinding(
            "product.parametros-conta.erg-01801",
            SLOT_FERIAS_HABILITADAS_OBRIGATORIOS,
            41,
            """
            {
              "or": [
                {"!=": [{"var": "draft.temFerias"}, true]},
                {
                  "and": [
                    {"!=": [{"var": "draft.maxPerFer"}, null]},
                    {">": [{"var": "draft.maxPerFer"}, 0]},
                    {"!=": [{"var": "draft.minPerFer"}, null]},
                    {">": [{"var": "draft.minPerFer"}, 0]},
                    {"!=": [{"var": "draft.maxDiasPerFer"}, null]},
                    {">": [{"var": "draft.maxDiasPerFer"}, 0]},
                    {"!=": [{"var": "draft.minDiasPerFer"}, null]},
                    {">": [{"var": "draft.minDiasPerFer"}, 0]}
                  ]
                }
              ]
            }
            """,
            "ERG-01801",
            baseFacts),
        // 5. ERG-01806: template parametrizável de limites de dias e parcelas
        productJsonLogicBinding(
            "product.parametros-conta.erg-01806",
            SLOT_FERIAS_LIMITES_DIAS_TEMPLATE,
            50,
            ERG01806_TEMPLATE_EXPRESSION,
            "ERG-01806",
            baseFacts),
        // 6a. ERG-01809: minPerFer <= maxPerFer
        productJsonLogicBinding(
            "product.parametros-conta.erg-01809",
            SLOT_PARCELAMENTO_COERENCIA,
            60,
            """
            {
              "or": [
                {"!=": [{"var": "draft.temFerias"}, true]},
                {"==": [{"var": "draft.minPerFer"}, null]},
                {"==": [{"var": "draft.maxPerFer"}, null]},
                {"<=": [{"var": "draft.minPerFer"}, {"var": "draft.maxPerFer"}]}
              ]
            }
            """,
            "ERG-01809",
            baseFacts),
        // 6b. ERG-01810: minDiasPerFer <= maxDiasPerFer e minDiasPerPrincFer coerente
        productJsonLogicBinding(
            "product.parametros-conta.erg-01810",
            SLOT_PARCELAMENTO_COERENCIA,
            61,
            """
            {
              "or": [
                {"!=": [{"var": "draft.temFerias"}, true]},
                {"==": [{"var": "draft.minDiasPerFer"}, null]},
                {"==": [{"var": "draft.maxDiasPerFer"}, null]},
                {
                  "and": [
                    {"<=": [{"var": "draft.minDiasPerFer"}, {"var": "draft.maxDiasPerFer"}]},
                    {
                      "or": [
                        {"==": [{"var": "draft.minDiasPerPrincFer"}, null]},
                        {
                          "and": [
                            {">=": [{"var": "draft.minDiasPerPrincFer"}, {"var": "draft.minDiasPerFer"}]},
                            {"<=": [{"var": "draft.minDiasPerPrincFer"}, {"var": "draft.maxDiasPerFer"}]}
                          ]
                        }
                      ]
                    }
                  ]
                }
              ]
            }
            """,
            "ERG-01810",
            baseFacts),
        // 6c. ERG-01851: maxPerFer <= totDiasFer
        productJsonLogicBinding(
            "product.parametros-conta.erg-01851",
            SLOT_PARCELAMENTO_COERENCIA,
            62,
            """
            {
              "or": [
                {"!=": [{"var": "draft.temFerias"}, true]},
                {"==": [{"var": "draft.maxPerFer"}, null]},
                {"==": [{"var": "draft.totDiasFer"}, null]},
                {"<=": [{"var": "draft.maxPerFer"}, {"var": "draft.totDiasFer"}]}
              ]
            }
            """,
            "ERG-01851",
            baseFacts),
        // 6d. ERG-01852: maxDiasPerFer <= totDiasFer
        productJsonLogicBinding(
            "product.parametros-conta.erg-01852",
            SLOT_PARCELAMENTO_COERENCIA,
            63,
            """
            {
              "or": [
                {"!=": [{"var": "draft.temFerias"}, true]},
                {"==": [{"var": "draft.maxDiasPerFer"}, null]},
                {"==": [{"var": "draft.totDiasFer"}, null]},
                {"<=": [{"var": "draft.maxDiasPerFer"}, {"var": "draft.totDiasFer"}]}
              ]
            }
            """,
            "ERG-01852",
            baseFacts),
        // 7. ERG-01801..ERG-01805 / ERG-01807 / ERG-01808: abono pecuniário e fração
        productJsonLogicBinding(
            "product.parametros-conta.erg-01802",
            SLOT_ABONO_PECUNIARIO_FRACAO_LIMITES,
            70,
            """
            {
              "or": [
                {"!=": [{"var": "draft.temFerias"}, true]},
                {
                  "and": [
                    {"in": [{"var": "draft.abonoDirGozo"}, ["D", "G", "N"]]},
                    {
                      "or": [
                        {"==": [{"var": "draft.abonoDirGozo"}, "N"]},
                        {
                          "and": [
                            {"!=": [{"var": "draft.numDiasAbonoFer"}, null]},
                            {"!=": [{"var": "draft.denDiasAbonoFer"}, null]},
                            {">=": [{"var": "draft.numDiasAbonoFer"}, 0]},
                            {">": [{"var": "draft.denDiasAbonoFer"}, 0]},
                            {"<=": [{"var": "draft.numDiasAbonoFer"}, {"var": "draft.denDiasAbonoFer"}]}
                          ]
                        }
                      ]
                    },
                    {
                      "or": [
                        {"==": [{"var": "draft.minDiasPvendFer"}, null]},
                        {">=": [{"var": "draft.minDiasPvendFer"}, 0]}
                      ]
                    },
                    {
                      "or": [
                        {"==": [{"var": "draft.maxDiasVendFer"}, null]},
                        {">=": [{"var": "draft.maxDiasVendFer"}, 0]}
                      ]
                    },
                    {
                      "or": [
                        {"==": [{"var": "draft.minDiasPvendFer"}, null]},
                        {"==": [{"var": "draft.maxDiasVendFer"}, null]},
                        {"<=": [{"var": "draft.minDiasPvendFer"}, {"var": "draft.maxDiasVendFer"}]}
                      ]
                    },
                    {
                      "or": [
                        {"==": [{"var": "draft.maxDiasVendFer"}, null]},
                        {"==": [{"var": "draft.totDiasFer"}, null]},
                        {"<=": [{"var": "draft.maxDiasVendFer"}, {"var": "draft.totDiasFer"}]}
                      ]
                    }
                  ]
                }
              ]
            }
            """,
            "ERG-01802",
            baseFacts),
        // 8a. ERG-01811: quando temFerias == false, veda parâmetros de férias
        productJsonLogicBinding(
            "product.parametros-conta.erg-01811",
            SLOT_FERIAS_DESABILITADAS_SEM_LIMITES,
            80,
            """
            {
              "or": [
                {"==": [{"var": "draft.temFerias"}, true]},
                {
                  "and": [
                    {"in": [{"var": "draft.totDiasFer"}, [null, 0]]},
                    {"in": [{"var": "draft.maxPerFer"}, [null, 0]]},
                    {"in": [{"var": "draft.minPerFer"}, [null, 0]]},
                    {"in": [{"var": "draft.maxDiasPerFer"}, [null, 0]]},
                    {"in": [{"var": "draft.minDiasPerFer"}, [null, 0]]},
                    {"in": [{"var": "draft.minDiasPerPrincFer"}, [null, 0]]}
                  ]
                }
              ]
            }
            """,
            "ERG-01811",
            baseFacts),
        // 8b. ERG-01812: quando temFerias == false, veda parâmetros de abono pecuniário
        productJsonLogicBinding(
            "product.parametros-conta.erg-01812",
            SLOT_FERIAS_DESABILITADAS_SEM_LIMITES,
            81,
            """
            {
              "or": [
                {"==": [{"var": "draft.temFerias"}, true]},
                {
                  "and": [
                    {"in": [{"var": "draft.minDiasPvendFer"}, [null, 0]]},
                    {"in": [{"var": "draft.maxDiasVendFer"}, [null, 0]]},
                    {"in": [{"var": "draft.numDiasAbonoFer"}, [null, 0]]},
                    {"in": [{"var": "draft.denDiasAbonoFer"}, [null, 0]]}
                  ]
                }
              ]
            }
            """,
            "ERG-01812",
            baseFacts),
        // 9a. ERG-01853: contDobroFerias == true exige minDiasPeraqfer != null
        productJsonLogicBinding(
            "product.parametros-conta.erg-01853",
            SLOT_CONTAGEM_DOBRO_FERIAS_COERENCIA,
            90,
            """
            {
              "or": [
                {"!=": [{"var": "draft.contDobroFerias"}, true]},
                {"!=": [{"var": "draft.minDiasPeraqfer"}, null]}
              ]
            }
            """,
            "ERG-01853",
            baseFacts),
        // 9b. ERG-01854: contDobroFerias == true exige maxDiasPeraqfer != null
        productJsonLogicBinding(
            "product.parametros-conta.erg-01854",
            SLOT_CONTAGEM_DOBRO_FERIAS_COERENCIA,
            91,
            """
            {
              "or": [
                {"!=": [{"var": "draft.contDobroFerias"}, true]},
                {"!=": [{"var": "draft.maxDiasPeraqfer"}, null]}
              ]
            }
            """,
            "ERG-01854",
            baseFacts),
        // 9c. ERG-01855: contDobroFerias == false veda minDiasPeraqfer preenchido
        productJsonLogicBinding(
            "product.parametros-conta.erg-01855",
            SLOT_CONTAGEM_DOBRO_FERIAS_COERENCIA,
            92,
            """
            {
              "or": [
                {"==": [{"var": "draft.contDobroFerias"}, true]},
                {"in": [{"var": "draft.minDiasPeraqfer"}, [null, 0]]}
              ]
            }
            """,
            "ERG-01855",
            baseFacts),
        // 9d. ERG-01856: contDobroFerias == false veda maxDiasPeraqfer preenchido
        productJsonLogicBinding(
            "product.parametros-conta.erg-01856",
            SLOT_CONTAGEM_DOBRO_FERIAS_COERENCIA,
            93,
            """
            {
              "or": [
                {"==": [{"var": "draft.contDobroFerias"}, true]},
                {"in": [{"var": "draft.maxDiasPeraqfer"}, [null, 0]]}
              ]
            }
            """,
            "ERG-01856",
            baseFacts),
        // 9e. ERG-00606: minDiasPeraqfer <= maxDiasPeraqfer
        productJsonLogicBinding(
            "product.parametros-conta.erg-00606",
            SLOT_CONTAGEM_DOBRO_FERIAS_COERENCIA,
            94,
            """
            {
              "or": [
                {"!=": [{"var": "draft.contDobroFerias"}, true]},
                {"==": [{"var": "draft.minDiasPeraqfer"}, null]},
                {"==": [{"var": "draft.maxDiasPeraqfer"}, null]},
                {
                  "and": [
                    {">=": [{"var": "draft.minDiasPeraqfer"}, 0]},
                    {">=": [{"var": "draft.maxDiasPeraqfer"}, 0]},
                    {"<=": [{"var": "draft.minDiasPeraqfer"}, {"var": "draft.maxDiasPeraqfer"}]}
                  ]
                }
              ]
            }
            """,
            "ERG-00606",
            baseFacts),
        // 10a. ERG-01813: temLicEsp == true exige anosLicEsp > 0 e mesesLicEspPer > 0
        productJsonLogicBinding(
            "product.parametros-conta.erg-01813",
            SLOT_LICENCA_ESPECIAL_PROGRESSAO_COERENCIA,
            100,
            """
            {
              "or": [
                {"!=": [{"var": "draft.temLicEsp"}, true]},
                {
                  "and": [
                    {"!=": [{"var": "draft.anosLicEsp"}, null]},
                    {">": [{"var": "draft.anosLicEsp"}, 0]},
                    {"!=": [{"var": "draft.mesesLicEspPer"}, null]},
                    {">": [{"var": "draft.mesesLicEspPer"}, 0]}
                  ]
                }
              ]
            }
            """,
            "ERG-01813",
            baseFacts),
        // 10b. ERG-01817: temLicEsp != true veda anosLicEsp, mesesLicEspPer, vendLicEsp e contDobroLicEsp
        productJsonLogicBinding(
            "product.parametros-conta.erg-01817",
            SLOT_LICENCA_ESPECIAL_PROGRESSAO_COERENCIA,
            101,
            """
            {
              "or": [
                {"==": [{"var": "draft.temLicEsp"}, true]},
                {
                  "and": [
                    {"or": [{"==": [{"var": "draft.anosLicEsp"}, null]}, {"<=": [{"var": "draft.anosLicEsp"}, 0]}]},
                    {"or": [{"==": [{"var": "draft.mesesLicEspPer"}, null]}, {"<=": [{"var": "draft.mesesLicEspPer"}, 0]}]},
                    {"!=": [{"var": "draft.vendLicEsp"}, true]},
                    {"!=": [{"var": "draft.contDobroLicEsp"}, true]}
                  ]
                }
              ]
            }
            """,
            "ERG-01817",
            baseFacts),
        // 10c. ERG-01814: vendLicEsp == true exige abonoDirGozoLicEsp válido ('D', 'G' ou 'N')
        productJsonLogicBinding(
            "product.parametros-conta.erg-01814",
            SLOT_LICENCA_ESPECIAL_PROGRESSAO_COERENCIA,
            102,
            """
            {
              "or": [
                {"!=": [{"var": "draft.temLicEsp"}, true]},
                {"!=": [{"var": "draft.vendLicEsp"}, true]},
                {"in": [{"var": "draft.abonoDirGozoLicEsp"}, ["D", "G", "N"]]}
              ]
            }
            """,
            "ERG-01814",
            baseFacts),
        // 10d. ERG-01821: vendLicEsp == true exige minDiasPvendLicEsp >= 0, maxDiasVendLicEsp >= 0 e minDiasPvendLicEsp <= maxDiasVendLicEsp
        productJsonLogicBinding(
            "product.parametros-conta.erg-01821",
            SLOT_LICENCA_ESPECIAL_PROGRESSAO_COERENCIA,
            103,
            """
            {
              "or": [
                {"!=": [{"var": "draft.temLicEsp"}, true]},
                {"!=": [{"var": "draft.vendLicEsp"}, true]},
                {
                  "and": [
                    {"or": [{"==": [{"var": "draft.minDiasPvendLicEsp"}, null]}, {">=": [{"var": "draft.minDiasPvendLicEsp"}, 0]}]},
                    {"or": [{"==": [{"var": "draft.maxDiasVendLicEsp"}, null]}, {">=": [{"var": "draft.maxDiasVendLicEsp"}, 0]}]},
                    {
                      "or": [
                        {"==": [{"var": "draft.minDiasPvendLicEsp"}, null]},
                        {"==": [{"var": "draft.maxDiasVendLicEsp"}, null]},
                        {"<=": [{"var": "draft.minDiasPvendLicEsp"}, {"var": "draft.maxDiasVendLicEsp"}]}
                      ]
                    }
                  ]
                }
              ]
            }
            """,
            "ERG-01821",
            baseFacts),
        // 10e. ERG-01822: vendLicEsp == true exige fração coerente 0 <= numDiasAbonoLicEsp <= denDiasAbonoLicEsp (obrigatória quando 'D' ou 'G')
        productJsonLogicBinding(
            "product.parametros-conta.erg-01822",
            SLOT_LICENCA_ESPECIAL_PROGRESSAO_COERENCIA,
            104,
            """
            {
              "or": [
                {"!=": [{"var": "draft.temLicEsp"}, true]},
                {"!=": [{"var": "draft.vendLicEsp"}, true]},
                {
                  "and": [
                    {"!=": [{"var": "draft.abonoDirGozoLicEsp"}, "D"]},
                    {"!=": [{"var": "draft.abonoDirGozoLicEsp"}, "G"]},
                    {"==": [{"var": "draft.numDiasAbonoLicEsp"}, null]},
                    {"==": [{"var": "draft.denDiasAbonoLicEsp"}, null]}
                  ]
                },
                {
                  "and": [
                    {"!=": [{"var": "draft.numDiasAbonoLicEsp"}, null]},
                    {"!=": [{"var": "draft.denDiasAbonoLicEsp"}, null]},
                    {">": [{"var": "draft.denDiasAbonoLicEsp"}, 0]},
                    {">=": [{"var": "draft.numDiasAbonoLicEsp"}, 0]},
                    {"<=": [{"var": "draft.numDiasAbonoLicEsp"}, {"var": "draft.denDiasAbonoLicEsp"}]}
                  ]
                }
              ]
            }
            """,
            "ERG-01822",
            baseFacts),
        // 10f. ERG-01815: vendLicEsp != true veda minDiasPvendLicEsp e maxDiasVendLicEsp positivos
        productJsonLogicBinding(
            "product.parametros-conta.erg-01815",
            SLOT_LICENCA_ESPECIAL_PROGRESSAO_COERENCIA,
            105,
            """
            {
              "or": [
                {"!=": [{"var": "draft.temLicEsp"}, true]},
                {"==": [{"var": "draft.vendLicEsp"}, true]},
                {
                  "and": [
                    {"or": [{"==": [{"var": "draft.minDiasPvendLicEsp"}, null]}, {"<=": [{"var": "draft.minDiasPvendLicEsp"}, 0]}]},
                    {"or": [{"==": [{"var": "draft.maxDiasVendLicEsp"}, null]}, {"<=": [{"var": "draft.maxDiasVendLicEsp"}, 0]}]}
                  ]
                }
              ]
            }
            """,
            "ERG-01815",
            baseFacts),
        // 10g. ERG-01816: contDobroLicEsp == true exige minDiasPcontDobLicEsp <= maxDiasContDobLicEsp informados
        productJsonLogicBinding(
            "product.parametros-conta.erg-01816",
            SLOT_LICENCA_ESPECIAL_PROGRESSAO_COERENCIA,
            106,
            """
            {
              "or": [
                {"!=": [{"var": "draft.temLicEsp"}, true]},
                {"!=": [{"var": "draft.contDobroLicEsp"}, true]},
                {
                  "and": [
                    {"!=": [{"var": "draft.minDiasPcontDobLicEsp"}, null]},
                    {"!=": [{"var": "draft.maxDiasContDobLicEsp"}, null]},
                    {"<=": [{"var": "draft.minDiasPcontDobLicEsp"}, {"var": "draft.maxDiasContDobLicEsp"}]}
                  ]
                }
              ]
            }
            """,
            "ERG-01816",
            baseFacts),
        // 10h. ERG-01823: contDobroLicEsp == true exige limites não negativos
        productJsonLogicBinding(
            "product.parametros-conta.erg-01823",
            SLOT_LICENCA_ESPECIAL_PROGRESSAO_COERENCIA,
            107,
            """
            {
              "or": [
                {"!=": [{"var": "draft.temLicEsp"}, true]},
                {"!=": [{"var": "draft.contDobroLicEsp"}, true]},
                {
                  "and": [
                    {"or": [{"==": [{"var": "draft.minDiasPcontDobLicEsp"}, null]}, {">=": [{"var": "draft.minDiasPcontDobLicEsp"}, 0]}]},
                    {"or": [{"==": [{"var": "draft.maxDiasContDobLicEsp"}, null]}, {">=": [{"var": "draft.maxDiasContDobLicEsp"}, 0]}]}
                  ]
                }
              ]
            }
            """,
            "ERG-01823",
            baseFacts),
        // 10i. ERG-01824: contDobroLicEsp != true veda limites de contagem em dobro de licença especial positivos
        productJsonLogicBinding(
            "product.parametros-conta.erg-01824",
            SLOT_LICENCA_ESPECIAL_PROGRESSAO_COERENCIA,
            108,
            """
            {
              "or": [
                {"!=": [{"var": "draft.temLicEsp"}, true]},
                {"==": [{"var": "draft.contDobroLicEsp"}, true]},
                {
                  "and": [
                    {"or": [{"==": [{"var": "draft.minDiasPcontDobLicEsp"}, null]}, {"<=": [{"var": "draft.minDiasPcontDobLicEsp"}, 0]}]},
                    {"or": [{"==": [{"var": "draft.maxDiasContDobLicEsp"}, null]}, {"<=": [{"var": "draft.maxDiasContDobLicEsp"}, 0]}]}
                  ]
                }
              ]
            }
            """,
            "ERG-01824",
            baseFacts),
        // 10j. ERG-01818: coerência entre temProgressao e anosProgressao
        productJsonLogicBinding(
            "product.parametros-conta.erg-01818",
            SLOT_LICENCA_ESPECIAL_PROGRESSAO_COERENCIA,
            109,
            """
            {
              "or": [
                {
                  "and": [
                    {"==": [{"var": "draft.temProgressao"}, true]},
                    {"!=": [{"var": "draft.anosProgressao"}, null]},
                    {">": [{"var": "draft.anosProgressao"}, 0]}
                  ]
                },
                {
                  "and": [
                    {"!=": [{"var": "draft.temProgressao"}, true]},
                    {"or": [{"==": [{"var": "draft.anosProgressao"}, null]}, {"<=": [{"var": "draft.anosProgressao"}, 0]}]}
                  ]
                }
              ]
            }
            """,
            "ERG-01818",
            baseFacts),
        // 10k. ERG-01819: coerência entre temTempoServ e anos1TempoServ
        productJsonLogicBinding(
            "product.parametros-conta.erg-01819",
            SLOT_LICENCA_ESPECIAL_PROGRESSAO_COERENCIA,
            110,
            """
            {
              "or": [
                {
                  "and": [
                    {"==": [{"var": "draft.temTempoServ"}, true]},
                    {"!=": [{"var": "draft.anos1TempoServ"}, null]},
                    {">": [{"var": "draft.anos1TempoServ"}, 0]}
                  ]
                },
                {
                  "and": [
                    {"!=": [{"var": "draft.temTempoServ"}, true]},
                    {"or": [{"==": [{"var": "draft.anos1TempoServ"}, null]}, {"<=": [{"var": "draft.anos1TempoServ"}, 0]}]}
                  ]
                }
              ]
            }
            """,
            "ERG-01819",
            baseFacts),
        // 10l. ERG-01820: coerência entre temSextaParte e anosSextaParte
        productJsonLogicBinding(
            "product.parametros-conta.erg-01820",
            SLOT_LICENCA_ESPECIAL_PROGRESSAO_COERENCIA,
            111,
            """
            {
              "or": [
                {
                  "and": [
                    {"==": [{"var": "draft.temSextaParte"}, true]},
                    {"!=": [{"var": "draft.anosSextaParte"}, null]},
                    {">": [{"var": "draft.anosSextaParte"}, 0]}
                  ]
                },
                {
                  "and": [
                    {"!=": [{"var": "draft.temSextaParte"}, true]},
                    {"or": [{"==": [{"var": "draft.anosSextaParte"}, null]}, {"<=": [{"var": "draft.anosSextaParte"}, 0]}]}
                  ]
                }
              ]
            }
            """,
            "ERG-01820",
            baseFacts),
        // 11. ERG-01850 / ERG-01874: concomitância de vigência no mesmo escopo lógico (Executor Java)
        new DecisionBinding(
            "product.parametros-conta.erg-01850",
            SLOT_CONCOMITANCIA_VIGENCIA_ESCOPO,
            DecisionSource.PRODUCT,
            null,
            RuleExecutorRef.java(JAVA_EXECUTOR_CONCOMITANCIA_KEY, JAVA_EXECUTOR_VERSION),
            List.of(),
            110,
            true,
            null,
            null,
            List.of("draft.dtini", "scope.overlappingActiveCount"),
            null));
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
   * substituir o guard protegido {@code ERG-01873}.
   */
  public static RuleSetDefinition customerMode1ForbiddenOverrideAttempt() {
    DecisionBinding illegalOverride =
        new DecisionBinding(
            "customer.orgao2.parametros-conta.forbidden-erg-01873",
            SLOT_VIGENCIA_ORDEM_VALIDA,
            DecisionSource.CUSTOMER,
            CompositionPolicy.REPLACE_EXACT,
            RuleExecutorRef.jsonLogic(parseJson("{\"==\": [1, 1]}")),
            List.of(),
            15,
            true,
            RuleDecision.DENY,
            "ERG-01873",
            List.of("draft.dtini"),
            null);
    return withCustomerBindings(List.of(illegalOverride));
  }

  /**
   * <b>Modo 2 ({@link OverridePolicy#PARAMETERIZABLE} + {@link CompositionPolicy#PARAMETERIZE}):</b>
   * Órgão 2 exige direito de férias entre 20 e 30 dias e no máximo 2 parcelas.
   */
  public static DecisionBinding customerMode2ParameterizeBinding() {
    return customerMode2ParameterizeBinding(20, 30, 2);
  }

  public static DecisionBinding customerMode2ParameterizeBinding(
      int minDiasDireitoPadrao, int maxDiasDireitoPadrao, int maxParcelasTeto) {
    ObjectNode values = MAPPER.createObjectNode();
    values.put("minDiasDireitoPadrao", minDiasDireitoPadrao);
    values.put("maxDiasDireitoPadrao", maxDiasDireitoPadrao);
    values.put("maxParcelasTeto", maxParcelasTeto);
    return new DecisionBinding(
        "customer.orgao2.parametros-conta.vacation-days-bounds",
        SLOT_FERIAS_LIMITES_DIAS_TEMPLATE,
        DecisionSource.CUSTOMER,
        CompositionPolicy.PARAMETERIZE,
        null,
        List.of(),
        55,
        true,
        null,
        null,
        List.of(),
        new RuleParameterization(
            TEMPLATE_VACATION_DAYS_BOUNDS_KEY, TEMPLATE_VACATION_DAYS_BOUNDS_VERSION, values));
  }

  /**
   * <b>Modo 3 ({@link OverridePolicy#RESTRICT_ONLY} + {@link CompositionPolicy#AUGMENT}):</b>
   * Órgão 2 exige portaria/documento legal ({@code documentoLegalFeriasId != null}) quando
   * {@code temFerias == true}.
   */
  public static DecisionBinding customerMode3AugmentBinding() {
    return new DecisionBinding(
        "customer.orgao2.parametros-conta.require-documento-legal",
        SLOT_HOOK_PRE_VALIDACAO,
        DecisionSource.CUSTOMER,
        CompositionPolicy.AUGMENT,
        RuleExecutorRef.jsonLogic(
            parseJson(
                """
                {
                  "or": [
                    {"!=": [{"var": "draft.temFerias"}, true]},
                    {"!=": [{"var": "draft.documentoLegalFeriasId"}, null]}
                  ]
                }
                """)),
        List.of(),
        25,
        true,
        RuleDecision.DENY,
        "CERG-PARAM-301",
        List.of("draft.dtini", "draft.temFerias"),
        null);
  }

  /**
   * <b>Modo 4 ({@link OverridePolicy#REPLACEABLE} + {@link CompositionPolicy#REPLACE_EXACT}):</b>
   * Órgão 2 substitui a política de abono pecuniário para vedar integralmente venda de abono
   * ({@code abonoDirGozo == 'N'}).
   */
  public static DecisionBinding customerMode4ReplaceExactBinding() {
    return new DecisionBinding(
        "customer.orgao2.parametros-conta.veda-abono-pecuniario",
        SLOT_ABONO_PECUNIARIO_FRACAO_LIMITES,
        DecisionSource.CUSTOMER,
        CompositionPolicy.REPLACE_EXACT,
        RuleExecutorRef.jsonLogic(
            parseJson(
                """
                {
                  "or": [
                    {"!=": [{"var": "draft.temFerias"}, true]},
                    {"==": [{"var": "draft.abonoDirGozo"}, "N"]}
                  ]
                }
                """)),
        List.of(),
        75,
        true,
        RuleDecision.DENY,
        "CERG-PARAM-401",
        List.of("draft.dtini", "draft.temFerias"),
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
          "Invalid static JSON Logic in ParametroContagemFeriasRuleSet", ex);
    }
  }
}
