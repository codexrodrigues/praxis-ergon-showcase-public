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
 * Definição canônica do {@link RuleSetDefinition} para validação determinística de Programação,
 * Concessão e Gozo de Férias e Abono Pecuniário ({@code PCK_FERIAS.MAIN_PRE}, {@code MAIN_POS},
 * {@code VALID_FERIAS} e {@code PCK_ERG_ABONOS_FERIAS.MAIN_PRE}) sobre o
 * {@code praxis-rules-engine:0.1.0-beta.17} (Contrato {@code 1.6}).
 *
 * <p>Consome o resultado do merge com precedência {@code PARAMETROS_CONTA (+) PERAQFER -> FERIAS}
 * ({@link EffectiveVacationParameterResolver}) através da raiz {@code effectiveParametroConta}.
 */
public final class FeriasGozoRuleSet {

  public static final String DOMAIN_SCOPE = "workforce.vacation-and-leave";
  public static final String RESOURCE_KEY = "administracao-pessoal.ferias-gozo";
  public static final String RULE_SET_KEY =
      "ergon.administracao-pessoal.ferias-gozo.concessao";
  public static final String OPERATION = "validate";
  public static final int VERSION = 1;

  public static final List<String> AVAILABLE_ROOTS =
      List.of(
          "draft",
          "vinculo",
          "substituto",
          "pa",
          "effectiveParametroConta",
          "derived",
          "existingParcelas",
          "tenant",
          "context");

  public static final String TEMPLATE_FRACIONAMENTO_AND_ABONO_BOUNDS_KEY =
      "ergon.ferias:fracionamento-and-abono-bounds";
  public static final String TEMPLATE_FRACIONAMENTO_AND_ABONO_BOUNDS_VERSION = "1.0";
  public static final String TEMPLATE_FRACIONAMENTO_AND_ABONO_BOUNDS =
      TEMPLATE_FRACIONAMENTO_AND_ABONO_BOUNDS_KEY
          + "@"
          + TEMPLATE_FRACIONAMENTO_AND_ABONO_BOUNDS_VERSION;

  public static final String JAVA_EXECUTOR_CONCOMITANCIA_KEY =
      "ergon.ferias:concomitancia-e-saldo-fracionamento";
  public static final String JAVA_EXECUTOR_VERSION = "1.0";
  public static final String EXECUTOR_CONCOMITANCIA_E_SALDO_FRACIONAMENTO =
      JAVA_EXECUTOR_CONCOMITANCIA_KEY + "@" + JAVA_EXECUTOR_VERSION;

  public static final String SLOT_CRONOLOGIA_E_INTERRUPCAO_VALIDA =
      "ferias-gozo.guard.cronologia-e-interrupcao-valida";
  public static final String SLOT_VINCULO_EXERCICIO_VACANCIA_FREQUENCIA =
      "ferias-gozo.guard.vinculo-exercicio-vacancia-frequencia";
  public static final String SLOT_HOOK_PRE_VALIDACAO =
      "ferias-gozo.hook.pre-validacao-orgao";
  public static final String SLOT_DIREITO_FERIAS_HABILITADO =
      "ferias-gozo.core.direito-ferias-habilitado";
  public static final String SLOT_SUBSTITUTO_FUNCIONAL_VALIDO =
      "ferias-gozo.core.substituto-funcional-valido";
  public static final String SLOT_BENEFICIOS_FINANCEIROS_ELEGIBILIDADE =
      "ferias-gozo.core.beneficios-financeiros-elegibilidade";
  public static final String SLOT_GOZO_ANTES_TERMINO_PA_POR_ORDEM =
      "ferias-gozo.core.gozo-antes-termino-pa-por-ordem";
  public static final String SLOT_GOZO_APOS_PRESCRICAO_PA =
      "ferias-gozo.core.gozo-apos-prescricao-pa";
  public static final String SLOT_FRACIONAMENTO_LIMITES_TEMPLATE =
      "ferias-gozo.core.fracionamento-limites-template";
  public static final String SLOT_ABONO_PECUNIARIO_ELEGIBILIDADE_E_LIMITES =
      "ferias-gozo.core.abono-pecuniario-elegibilidade-e-limites";
  public static final String SLOT_CONCOMITANCIA_E_SALDO_FRACIONAMENTO =
      "ferias-gozo.post.concomitancia-e-saldo-fracionamento";
  public static final String SLOT_HOOK_POS_VALIDACAO =
      "ferias-gozo.hook.pos-validacao-orgao";

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static final String ERG00519_TEMPLATE_EXPRESSION =
      """
      {
        "and": [
          {"<=": [{"var": "derived.totalParcelasNoPa"}, 6]},
          {"<=": [{"var": "draft.ordemParcela"}, 6]},
          {
            "or": [
              {"==": [{"var": "draft.situacao"}, "INTERROMPIDA"]},
              {
                "and": [
                  {">=": [{"var": "draft.diasGozo"}, 1]},
                  {
                    "or": [
                      {"==": [{"var": "effectiveParametroConta.minDiasPerFer"}, null]},
                      {">=": [{"var": "draft.diasGozo"}, {"var": "effectiveParametroConta.minDiasPerFer"}]}
                    ]
                  }
                ]
              }
            ]
          },
          {
            "or": [
              {"==": [{"var": "draft.abonoPecuniario"}, false]},
              {"==": [{"var": "draft.abonoPecuniario"}, null]},
              {"<=": [{"var": "draft.diasAbonoVendidos"}, 30]}
            ]
          },
          {
            "or": [
              {"==": [{"var": "derived.antecedenciaDias"}, null]},
              {">=": [{"var": "derived.antecedenciaDias"}, 0]}
            ]
          },
          {
            "or": [
              {"==": [{"var": "draft.situacao"}, "INTERROMPIDA"]},
              {">=": [{"var": "derived.maxParcelaDiasNoPa"}, 1]},
              {">=": [{"var": "derived.saldoRemanescentePa"}, 1]}
            ]
          }
        ]
      }
      """;

  private FeriasGozoRuleSet() {}

  public static RuleSetRef ruleSetRef() {
    return new RuleSetRef(DOMAIN_SCOPE, RESOURCE_KEY, RULE_SET_KEY, OPERATION, VERSION);
  }

  public static RuleParameterTemplate fracionamentoAndAbonoBoundsTemplate() {
    return new RuleParameterTemplate(
        TEMPLATE_FRACIONAMENTO_AND_ABONO_BOUNDS_KEY,
        TEMPLATE_FRACIONAMENTO_AND_ABONO_BOUNDS_VERSION,
        RuleExecutorRef.jsonLogic(parseJson(ERG00519_TEMPLATE_EXPRESSION)),
        RuleDecision.DENY,
        "ERG-00519",
        List.of("draft.dtini", "draft.dtprevfim", "draft.diasGozo", "draft.ordemParcela"),
        List.of(
            new RuleParameterDefinition(
                "maxParcelasPermitidas",
                RuleParameterType.NUMBER,
                true,
                false,
                false,
                "installments",
                MAPPER.getNodeFactory().numberNode(6),
                BigDecimal.ONE,
                new BigDecimal("6"),
                List.of(),
                List.of("/and/0/<=/1", "/and/1/<=/1")),
            new RuleParameterDefinition(
                "minDiasParcela",
                RuleParameterType.NUMBER,
                true,
                false,
                false,
                "days",
                MAPPER.getNodeFactory().numberNode(1),
                BigDecimal.ONE,
                new BigDecimal("30"),
                List.of(),
                List.of("/and/2/or/1/and/0/>=/1")),
            new RuleParameterDefinition(
                "minDiasParcelaPrincipal",
                RuleParameterType.NUMBER,
                true,
                false,
                false,
                "days",
                MAPPER.getNodeFactory().numberNode(1),
                BigDecimal.ONE,
                new BigDecimal("30"),
                List.of(),
                List.of("/and/5/or/1/>=/1", "/and/5/or/2/>=/1")),
            new RuleParameterDefinition(
                "maxDiasAbonoPecuniario",
                RuleParameterType.NUMBER,
                true,
                false,
                false,
                "days",
                MAPPER.getNodeFactory().numberNode(30),
                BigDecimal.ZERO,
                new BigDecimal("30"),
                List.of(),
                List.of("/and/3/or/2/<=/1")),
            new RuleParameterDefinition(
                "antecedenciaMinimaDias",
                RuleParameterType.NUMBER,
                true,
                false,
                false,
                "days",
                MAPPER.getNodeFactory().numberNode(0),
                BigDecimal.ZERO,
                new BigDecimal("180"),
                List.of(),
                List.of("/and/4/or/1/>=/1"))));
  }

  public static List<DecisionSlot> productSlots() {
    return List.of(
        new DecisionSlot(
            SLOT_CRONOLOGIA_E_INTERRUPCAO_VALIDA,
            DecisionStage.PROTECTED_GUARD,
            SlotCardinality.SINGLE,
            OverridePolicy.FORBIDDEN,
            DecisionAggregationPolicy.SINGLE_RESULT,
            null),
        new DecisionSlot(
            SLOT_VINCULO_EXERCICIO_VACANCIA_FREQUENCIA,
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
            SLOT_DIREITO_FERIAS_HABILITADO,
            DecisionStage.DOMAIN_DECISION,
            SlotCardinality.SINGLE,
            OverridePolicy.FORBIDDEN,
            DecisionAggregationPolicy.SINGLE_RESULT,
            null),
        new DecisionSlot(
            SLOT_SUBSTITUTO_FUNCIONAL_VALIDO,
            DecisionStage.DOMAIN_DECISION,
            SlotCardinality.MULTIPLE,
            OverridePolicy.RESTRICT_ONLY,
            DecisionAggregationPolicy.DENY_OVERRIDES,
            null),
        new DecisionSlot(
            SLOT_BENEFICIOS_FINANCEIROS_ELEGIBILIDADE,
            DecisionStage.DOMAIN_DECISION,
            SlotCardinality.MULTIPLE,
            OverridePolicy.RESTRICT_ONLY,
            DecisionAggregationPolicy.DENY_OVERRIDES,
            null),
        new DecisionSlot(
            SLOT_GOZO_ANTES_TERMINO_PA_POR_ORDEM,
            DecisionStage.DOMAIN_DECISION,
            SlotCardinality.MULTIPLE,
            OverridePolicy.RESTRICT_ONLY,
            DecisionAggregationPolicy.DENY_OVERRIDES,
            null),
        new DecisionSlot(
            SLOT_GOZO_APOS_PRESCRICAO_PA,
            DecisionStage.DOMAIN_DECISION,
            SlotCardinality.SINGLE,
            OverridePolicy.REPLACEABLE,
            DecisionAggregationPolicy.SINGLE_RESULT,
            null),
        new DecisionSlot(
            SLOT_FRACIONAMENTO_LIMITES_TEMPLATE,
            DecisionStage.DOMAIN_DECISION,
            SlotCardinality.SINGLE,
            OverridePolicy.PARAMETERIZABLE,
            DecisionAggregationPolicy.SINGLE_RESULT,
            fracionamentoAndAbonoBoundsTemplate()),
        new DecisionSlot(
            SLOT_ABONO_PECUNIARIO_ELEGIBILIDADE_E_LIMITES,
            DecisionStage.DOMAIN_DECISION,
            SlotCardinality.SINGLE,
            OverridePolicy.REPLACEABLE,
            DecisionAggregationPolicy.SINGLE_RESULT,
            null),
        new DecisionSlot(
            SLOT_CONCOMITANCIA_E_SALDO_FRACIONAMENTO,
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
    List<String> baseFacts = List.of("draft.dtini", "draft.dtprevfim", "draft.diasGozo");
    return List.of(
        // 1. VALID_FERIAS: cronologia dtini <= dtprevfim, dtini <= dtfim, dtInterrupcao em [dtini, dtprevfim]
        productJsonLogicBinding(
            "product.ferias-gozo.valid-ferias-cronologia",
            SLOT_CRONOLOGIA_E_INTERRUPCAO_VALIDA,
            10,
            """
            {
              "and": [
                {"!=": [{"var": "draft.dtini"}, null]},
                {"!=": [{"var": "draft.dtprevfim"}, null]},
                {">=": [{"date": [{"var": "draft.dtprevfim"}]}, {"date": [{"var": "draft.dtini"}]}]},
                {">": [{"var": "draft.diasGozo"}, 0]},
                {
                  "or": [
                    {"==": [{"var": "draft.dtfim"}, null]},
                    {">=": [{"date": [{"var": "draft.dtfim"}]}, {"date": [{"var": "draft.dtini"}]}]}
                  ]
                },
                {
                  "or": [
                    {"==": [{"var": "draft.dtInterrupcao"}, null]},
                    {
                      "and": [
                        {">=": [{"date": [{"var": "draft.dtInterrupcao"}]}, {"date": [{"var": "draft.dtini"}]}]},
                        {"<=": [{"date": [{"var": "draft.dtInterrupcao"}]}, {"date": [{"var": "draft.dtprevfim"}]}]}
                      ]
                    }
                  ]
                }
              ]
            }
            """,
            "VALID_FERIAS",
            baseFacts),
        // 2. VALID_FERIAS: vínculo em exercício, antes de vacância/aposentadoria e sem conflito de frequência/afastamento
        productJsonLogicBinding(
            "product.ferias-gozo.valid-ferias-vinculo-frequencia",
            SLOT_VINCULO_EXERCICIO_VACANCIA_FREQUENCIA,
            20,
            """
            {
              "and": [
                {
                  "or": [
                    {"==": [{"var": "vinculo.dtexerc"}, null]},
                    {">=": [{"date": [{"var": "draft.dtini"}]}, {"date": [{"var": "vinculo.dtexerc"}]}]}
                  ]
                },
                {
                  "or": [
                    {"==": [{"var": "vinculo.dtvac"}, null]},
                    {"<": [{"date": [{"var": "draft.dtfimEfetivo"}]}, {"date": [{"var": "vinculo.dtvac"}]}]}
                  ]
                },
                {
                  "or": [
                    {"==": [{"var": "vinculo.dtaposent"}, null]},
                    {"<": [{"date": [{"var": "draft.dtfimEfetivo"}]}, {"date": [{"var": "vinculo.dtaposent"}]}]}
                  ]
                },
                {"==": [{"var": "derived.overlappingNormalFrequenciasCount"}, 0]},
                {"==": [{"var": "derived.overlappingAfastamentosCount"}, 0]}
              ]
            }
            """,
            "VALID_FERIAS",
            baseFacts),
        // 3. Hook pré-validação
        productJsonLogicBinding(
            "product.ferias-gozo.hook-pre-baseline",
            SLOT_HOOK_PRE_VALIDACAO,
            30,
            "{\"==\": [1, 1]}",
            "CERG-FER-PRE-001",
            baseFacts),
        // 4. ERG-00506: direito a férias habilitado e PA apto
        productJsonLogicBinding(
            "product.ferias-gozo.erg-00506",
            SLOT_DIREITO_FERIAS_HABILITADO,
            40,
            """
            {
              "and": [
                {"==": [{"var": "effectiveParametroConta.temFerias"}, true]},
                {"==": [{"var": "pa.aptoConcessao"}, true]}
              ]
            }
            """,
            "ERG-00506",
            baseFacts),
        // 5a. ERG-00502: vedação de autodesignação como substituto
        productJsonLogicBinding(
            "product.ferias-gozo.erg-00502",
            SLOT_SUBSTITUTO_FUNCIONAL_VALIDO,
            50,
            """
            {"!=": [{"var": "substituto.selfSubstitution"}, true]}
            """,
            "ERG-00502",
            baseFacts),
        // 5b. ERG-00503: par (numfuncSubstituto, numvincSubstituto) completo e vínculo válido no órgão
        productJsonLogicBinding(
            "product.ferias-gozo.erg-00503",
            SLOT_SUBSTITUTO_FUNCIONAL_VALIDO,
            51,
            """
            {
              "and": [
                {"==": [{"var": "substituto.pairComplete"}, true]},
                {"==": [{"var": "substituto.vinculoValidoNoOrgao"}, true]}
              ]
            }
            """,
            "ERG-00503",
            baseFacts),
        // 5c. ERG-00504: proteção de substituição ativa no período
        productJsonLogicBinding(
            "product.ferias-gozo.erg-00504",
            SLOT_SUBSTITUTO_FUNCIONAL_VALIDO,
            52,
            """
            {"!=": [{"var": "substituto.activeSubstitutionConflict"}, true]}
            """,
            "ERG-00504",
            baseFacts),
        // 6a. ERG-00507: adiantamento de 13º salário permitido
        productJsonLogicBinding(
            "product.ferias-gozo.erg-00507",
            SLOT_BENEFICIOS_FINANCEIROS_ELEGIBILIDADE,
            60,
            """
            {
              "or": [
                {"!=": [{"var": "draft.adiantamento13"}, true]},
                {"==": [{"var": "effectiveParametroConta.adiant13Fer"}, true]}
              ]
            }
            """,
            "ERG-00507",
            baseFacts),
        // 6b. ERG-00508: adiantamento salarial permitido
        productJsonLogicBinding(
            "product.ferias-gozo.erg-00508",
            SLOT_BENEFICIOS_FINANCEIROS_ELEGIBILIDADE,
            61,
            """
            {
              "or": [
                {"!=": [{"var": "draft.adiantamentoSalario"}, true]},
                {"==": [{"var": "effectiveParametroConta.adiantSalFer"}, true]}
              ]
            }
            """,
            "ERG-00508",
            baseFacts),
        // 6c. ERG-00509: empréstimo de férias permitido
        productJsonLogicBinding(
            "product.ferias-gozo.erg-00509",
            SLOT_BENEFICIOS_FINANCEIROS_ELEGIBILIDADE,
            62,
            """
            {
              "or": [
                {"!=": [{"var": "draft.emprestimoFerias"}, true]},
                {"==": [{"var": "effectiveParametroConta.emprestFer"}, true]}
              ]
            }
            """,
            "ERG-00509",
            baseFacts),
        // 6d. ERG-00510: abono constitucional de 1/3 permitido
        productJsonLogicBinding(
            "product.ferias-gozo.erg-00510",
            SLOT_BENEFICIOS_FINANCEIROS_ELEGIBILIDADE,
            63,
            """
            {
              "or": [
                {"!=": [{"var": "draft.abonoConstitucional"}, true]},
                {"==": [{"var": "effectiveParametroConta.abonoConstFer"}, true]}
              ]
            }
            """,
            "ERG-00510",
            baseFacts),
        // 6e. ERG-00512: unicidade do abono constitucional de 1/3 no mesmo PA
        productJsonLogicBinding(
            "product.ferias-gozo.erg-00512",
            SLOT_BENEFICIOS_FINANCEIROS_ELEGIBILIDADE,
            64,
            """
            {
              "or": [
                {"!=": [{"var": "draft.abonoConstitucional"}, true]},
                {"==": [{"var": "derived.otherParcelasWithAbonoConstCount"}, 0]}
              ]
            }
            """,
            "ERG-00512",
            baseFacts),
        // 6f. ERG-00513: unicidade do adiantamento de 13º no mesmo ano civil
        productJsonLogicBinding(
            "product.ferias-gozo.erg-00513",
            SLOT_BENEFICIOS_FINANCEIROS_ELEGIBILIDADE,
            65,
            """
            {
              "or": [
                {"!=": [{"var": "draft.adiantamento13"}, true]},
                {"==": [{"var": "derived.otherParcelasWithAdiant13InYearCount"}, 0]}
              ]
            }
            """,
            "ERG-00513",
            baseFacts),
        // 6g. ERG-00521: vedação de abono pecuniário em mais de uma parcela quando permiteLancConcPa == false
        productJsonLogicBinding(
            "product.ferias-gozo.erg-00521",
            SLOT_BENEFICIOS_FINANCEIROS_ELEGIBILIDADE,
            66,
            """
            {
              "or": [
                {"!=": [{"var": "draft.abonoPecuniario"}, true]},
                {"==": [{"var": "effectiveParametroConta.permiteLancConcPa"}, true]},
                {"==": [{"var": "derived.otherParcelasWithAbonoPecuniarioInPaCount"}, 0]}
              ]
            }
            """,
            "ERG-00521",
            baseFacts),
        // 7a. ERG-00516: gozo antes da data de início do PA
        productJsonLogicBinding(
            "product.ferias-gozo.erg-00516",
            SLOT_GOZO_ANTES_TERMINO_PA_POR_ORDEM,
            70,
            """
            {
              "or": [
                {"==": [{"var": "effectiveParametroConta.permiteGozoAntesPaFer"}, true]},
                {"==": [{"var": "pa.dtini"}, null]},
                {">=": [{"date": [{"var": "draft.dtini"}]}, {"date": [{"var": "pa.dtini"}]}]}
              ]
            }
            """,
            "ERG-00516",
            baseFacts),
        // 7b. ERG-00527: gozo antes do fim do 1º PA do vínculo
        productJsonLogicBinding(
            "product.ferias-gozo.erg-00527",
            SLOT_GOZO_ANTES_TERMINO_PA_POR_ORDEM,
            71,
            """
            {
              "or": [
                {"==": [{"var": "effectiveParametroConta.permiteGozoAntesPaFer"}, true]},
                {"!=": [{"var": "pa.ordemNoVinculo"}, 1]},
                {"==": [{"var": "pa.dtfim"}, null]},
                {">=": [{"date": [{"var": "draft.dtini"}]}, {"date": [{"var": "pa.dtfim"}]}]},
                {"==": [{"var": "effectiveParametroConta.permiteFerPrimPer"}, true]}
              ]
            }
            """,
            "ERG-00527",
            baseFacts),
        // 7c. ERG-00528: gozo antes do fim do 2º PA do vínculo
        productJsonLogicBinding(
            "product.ferias-gozo.erg-00528",
            SLOT_GOZO_ANTES_TERMINO_PA_POR_ORDEM,
            72,
            """
            {
              "or": [
                {"==": [{"var": "effectiveParametroConta.permiteGozoAntesPaFer"}, true]},
                {"!=": [{"var": "pa.ordemNoVinculo"}, 2]},
                {"==": [{"var": "pa.dtfim"}, null]},
                {">=": [{"date": [{"var": "draft.dtini"}]}, {"date": [{"var": "pa.dtfim"}]}]},
                {"==": [{"var": "effectiveParametroConta.permiteFerSegunPer"}, true]}
              ]
            }
            """,
            "ERG-00528",
            baseFacts),
        // 7d. ERG-00530: gozo antes do fim do 3º PA do vínculo
        productJsonLogicBinding(
            "product.ferias-gozo.erg-00530",
            SLOT_GOZO_ANTES_TERMINO_PA_POR_ORDEM,
            73,
            """
            {
              "or": [
                {"==": [{"var": "effectiveParametroConta.permiteGozoAntesPaFer"}, true]},
                {"!=": [{"var": "pa.ordemNoVinculo"}, 3]},
                {"==": [{"var": "pa.dtfim"}, null]},
                {">=": [{"date": [{"var": "draft.dtini"}]}, {"date": [{"var": "pa.dtfim"}]}]},
                {"==": [{"var": "effectiveParametroConta.permiteFerTercPer"}, true]}
              ]
            }
            """,
            "ERG-00530",
            baseFacts),
        // 7e. ERG-00517: gozo antes do fim de PA incompleto (>3º PA do vínculo)
        productJsonLogicBinding(
            "product.ferias-gozo.erg-00517",
            SLOT_GOZO_ANTES_TERMINO_PA_POR_ORDEM,
            74,
            """
            {
              "or": [
                {"==": [{"var": "effectiveParametroConta.permiteGozoAntesPaFer"}, true]},
                {"<=": [{"var": "pa.ordemNoVinculo"}, 3]},
                {"==": [{"var": "pa.dtfim"}, null]},
                {">=": [{"date": [{"var": "draft.dtini"}]}, {"date": [{"var": "pa.dtfim"}]}]},
                {"==": [{"var": "effectiveParametroConta.permiteFerPerInc"}, true]}
              ]
            }
            """,
            "ERG-00517",
            baseFacts),
        // 8. ERG-01880: gozo após a data de prescrição do PA
        productJsonLogicBinding(
            "product.ferias-gozo.erg-01880",
            SLOT_GOZO_APOS_PRESCRICAO_PA,
            80,
            """
            {
              "or": [
                {"==": [{"var": "effectiveParametroConta.permiteGozoAposPrescricao"}, true]},
                {"==": [{"var": "pa.dtPrescricao"}, null]},
                {"!=": [{"var": "derived.violatesDtPrescricao"}, true]}
              ]
            }
            """,
            "ERG-01880",
            baseFacts),
        // 9. ERG-00519: template parametrizável de fracionamento e duração mínima
        productJsonLogicBinding(
            "product.ferias-gozo.erg-00519",
            SLOT_FRACIONAMENTO_LIMITES_TEMPLATE,
            90,
            ERG00519_TEMPLATE_EXPRESSION,
            "ERG-00519",
            baseFacts),
        // 10. ERG-00522 / ERG-03311..ERG-03315: elegibilidade e limites de abono pecuniário
        productJsonLogicBinding(
            "product.ferias-gozo.erg-00522",
            SLOT_ABONO_PECUNIARIO_ELEGIBILIDADE_E_LIMITES,
            100,
            """
            {
              "or": [
                {
                  "and": [
                    {"!=": [{"var": "draft.abonoPecuniario"}, true]},
                    {"or": [{"==": [{"var": "draft.diasAbonoVendidos"}, null]}, {"==": [{"var": "draft.diasAbonoVendidos"}, 0]}]}
                  ]
                },
                {
                  "and": [
                    {"==": [{"var": "draft.abonoPecuniario"}, true]},
                    {"==": [{"var": "effectiveParametroConta.permiteAbonoPecuniario"}, true]},
                    {"!=": [{"var": "draft.diasAbonoVendidos"}, null]},
                    {">": [{"var": "draft.diasAbonoVendidos"}, 0]},
                    {"in": [{"var": "draft.iniFimAbono"}, ["I", "F"]]},
                    {
                      "or": [
                        {"==": [{"var": "effectiveParametroConta.minDiasPvendFer"}, null]},
                        {">=": [{"var": "draft.diasAbonoVendidos"}, {"var": "effectiveParametroConta.minDiasPvendFer"}]}
                      ]
                    },
                    {
                      "or": [
                        {"==": [{"var": "effectiveParametroConta.maxDiasVendFer"}, null]},
                        {"<=": [{"var": "draft.diasAbonoVendidos"}, {"var": "effectiveParametroConta.maxDiasVendFer"}]}
                      ]
                    },
                    {
                      "or": [
                        {"==": [{"var": "derived.maxDiasAbonoPelaFracao"}, null]},
                        {"<=": [{"var": "draft.diasAbonoVendidos"}, {"var": "derived.maxDiasAbonoPelaFracao"}]}
                      ]
                    },
                    {
                      "or": [
                        {"==": [{"var": "draft.dtVendaAbono"}, null]},
                        {"==": [{"var": "draft.dtini"}, null]},
                        {"<=": [{"date": [{"var": "draft.dtVendaAbono"}]}, {"date": [{"var": "draft.dtini"}]}]}
                      ]
                    },
                    {
                      "or": [
                        {"==": [{"var": "draft.dtPagtoAbono"}, null]},
                        {"==": [{"var": "draft.dtVendaAbono"}, null]},
                        {">=": [{"date": [{"var": "draft.dtPagtoAbono"}]}, {"date": [{"var": "draft.dtVendaAbono"}]}]}
                      ]
                    }
                  ]
                }
              ]
            }
            """,
            "ERG-00522",
            baseFacts),
        // 11. ERG-00514, ERG-00518, ERG-00520, ERG-00523, ERG-00525, ERG-00531, ERG-00532, ERG-00526 (Executor Java)
        new DecisionBinding(
            "product.ferias-gozo.concomitancia-e-saldo",
            SLOT_CONCOMITANCIA_E_SALDO_FRACIONAMENTO,
            DecisionSource.PRODUCT,
            null,
            RuleExecutorRef.java(JAVA_EXECUTOR_CONCOMITANCIA_KEY, JAVA_EXECUTOR_VERSION),
            List.of(),
            110,
            true,
            null,
            null,
            List.of(
                "draft.diasGozo",
                "draft.ordemParcela",
                "pa.totDiasDireito",
                "derived.overlappingFeriasGozoCount",
                "derived.totalDiasConsumidosPa"),
            null),
        // 12. Hook pós-validação
        productJsonLogicBinding(
            "product.ferias-gozo.hook-pos-baseline",
            SLOT_HOOK_POS_VALIDACAO,
            120,
            "{\"==\": [1, 1]}",
            "CERG-FER-POST-001",
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
   * substituir o slot protegido de saldo/concomitância ({@code ERG-00523}).
   */
  public static RuleSetDefinition customerMode1ForbiddenOverrideAttempt() {
    DecisionBinding illegalOverride =
        new DecisionBinding(
            "customer.orgao2.ferias-gozo.forbidden-erg-00523",
            SLOT_CONCOMITANCIA_E_SALDO_FRACIONAMENTO,
            DecisionSource.CUSTOMER,
            CompositionPolicy.REPLACE_EXACT,
            RuleExecutorRef.jsonLogic(parseJson("{\"==\": [1, 1]}")),
            List.of(),
            115,
            true,
            RuleDecision.DENY,
            "ERG-00523",
            List.of("draft.diasGozo"),
            null);
    return withCustomerBindings(List.of(illegalOverride));
  }

  /**
   * <b>Modo 2 ({@link OverridePolicy#PARAMETERIZABLE} + {@link CompositionPolicy#PARAMETERIZE}):</b>
   * Órgão 2 parametriza o template {@code ergon.ferias:fracionamento-and-abono-bounds@1.0} exigindo
   * no máximo 2 parcelas, mínimo de 10 dias por parcela, mínimo de 15 dias na parcela principal,
   * máximo de 10 dias de abono pecuniário e antecedência mínima de 15 dias.
   */
  public static DecisionBinding customerMode2ParameterizeBinding() {
    return customerMode2ParameterizeBinding(2, 10, 15, 10, 15);
  }

  public static DecisionBinding customerMode2ParameterizeBinding(
      int maxParcelasPermitidas,
      int minDiasParcela,
      int maxDiasAbonoPecuniario,
      int antecedenciaMinimaDias) {
    return customerMode2ParameterizeBinding(
        maxParcelasPermitidas,
        minDiasParcela,
        15,
        maxDiasAbonoPecuniario,
        antecedenciaMinimaDias);
  }

  public static DecisionBinding customerMode2ParameterizeBinding(
      int maxParcelasPermitidas,
      int minDiasParcela,
      int minDiasParcelaPrincipal,
      int maxDiasAbonoPecuniario,
      int antecedenciaMinimaDias) {
    ObjectNode values = MAPPER.createObjectNode();
    values.put("maxParcelasPermitidas", maxParcelasPermitidas);
    values.put("minDiasParcela", minDiasParcela);
    values.put("minDiasParcelaPrincipal", minDiasParcelaPrincipal);
    values.put("maxDiasAbonoPecuniario", maxDiasAbonoPecuniario);
    values.put("antecedenciaMinimaDias", antecedenciaMinimaDias);
    return new DecisionBinding(
        "customer.orgao2.ferias-gozo.fracionamento-bounds",
        SLOT_FRACIONAMENTO_LIMITES_TEMPLATE,
        DecisionSource.CUSTOMER,
        CompositionPolicy.PARAMETERIZE,
        null,
        List.of(),
        95,
        true,
        null,
        null,
        List.of(),
        new RuleParameterization(
            TEMPLATE_FRACIONAMENTO_AND_ABONO_BOUNDS_KEY,
            TEMPLATE_FRACIONAMENTO_AND_ABONO_BOUNDS_VERSION,
            values));
  }

  /**
   * <b>Modo 3 ({@link OverridePolicy#RESTRICT_ONLY} + {@link CompositionPolicy#AUGMENT}):</b>
   * Órgão 2 exige portaria/documento legal ({@code documentoLegalId != null}) em toda concessão de
   * férias com abono pecuniário ou adiantamento de 13º salário.
   */
  public static DecisionBinding customerMode3AugmentBinding() {
    return new DecisionBinding(
        "customer.orgao2.ferias-gozo.require-documento-for-abono-or-13",
        SLOT_HOOK_PRE_VALIDACAO,
        DecisionSource.CUSTOMER,
        CompositionPolicy.AUGMENT,
        RuleExecutorRef.jsonLogic(
            parseJson(
                """
                {
                  "or": [
                    {
                      "and": [
                        {"!=": [{"var": "draft.abonoPecuniario"}, true]},
                        {"!=": [{"var": "draft.adiantamento13"}, true]}
                      ]
                    },
                    {"!=": [{"var": "draft.documentoLegalId"}, null]}
                  ]
                }
                """)),
        List.of(),
        35,
        true,
        RuleDecision.DENY,
        "CERG-FER-301",
        List.of("draft.dtini", "draft.dtprevfim", "draft.diasGozo"),
        null);
  }

  /**
   * <b>Modo 4 ({@link OverridePolicy#REPLACEABLE} + {@link CompositionPolicy#REPLACE_EXACT}):</b>
   * Órgão 2 substitui a política de abono pecuniário ({@code EP__TRATA_ABONO}) para permitir venda
   * de abono somente na 1ª parcela do período aquisitivo ({@code ordemParcela == 1}) e
   * exclusivamente no final das férias ({@code iniFimAbono == 'F'}).
   */
  public static DecisionBinding customerMode4ReplaceExactBinding() {
    return new DecisionBinding(
        "customer.orgao2.ferias-gozo.abono-somente-primeira-parcela-fim",
        SLOT_ABONO_PECUNIARIO_ELEGIBILIDADE_E_LIMITES,
        DecisionSource.CUSTOMER,
        CompositionPolicy.REPLACE_EXACT,
        RuleExecutorRef.jsonLogic(
            parseJson(
                """
                {
                  "or": [
                    {
                      "and": [
                        {"!=": [{"var": "draft.abonoPecuniario"}, true]},
                        {"or": [{"==": [{"var": "draft.diasAbonoVendidos"}, null]}, {"==": [{"var": "draft.diasAbonoVendidos"}, 0]}]}
                      ]
                    },
                    {
                      "and": [
                        {"==": [{"var": "draft.abonoPecuniario"}, true]},
                        {"==": [{"var": "effectiveParametroConta.permiteAbonoPecuniario"}, true]},
                        {"!=": [{"var": "draft.diasAbonoVendidos"}, null]},
                        {">": [{"var": "draft.diasAbonoVendidos"}, 0]},
                        {"==": [{"var": "draft.ordemParcela"}, 1]},
                        {"==": [{"var": "draft.iniFimAbono"}, "F"]},
                        {
                          "or": [
                            {"==": [{"var": "effectiveParametroConta.minDiasPvendFer"}, null]},
                            {">=": [{"var": "draft.diasAbonoVendidos"}, {"var": "effectiveParametroConta.minDiasPvendFer"}]}
                          ]
                        },
                        {
                          "or": [
                            {"==": [{"var": "effectiveParametroConta.maxDiasVendFer"}, null]},
                            {"<=": [{"var": "draft.diasAbonoVendidos"}, {"var": "effectiveParametroConta.maxDiasVendFer"}]}
                          ]
                        },
                        {
                          "or": [
                            {"==": [{"var": "derived.maxDiasAbonoPelaFracao"}, null]},
                            {"<=": [{"var": "draft.diasAbonoVendidos"}, {"var": "derived.maxDiasAbonoPelaFracao"}]}
                          ]
                        }
                      ]
                    }
                  ]
                }
                """)),
        List.of(),
        105,
        true,
        RuleDecision.DENY,
        "CERG-FER-401",
        List.of("draft.dtini", "draft.dtprevfim", "draft.diasGozo"),
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
      throw new IllegalStateException("Invalid static JSON Logic in FeriasGozoRuleSet", ex);
    }
  }
}
