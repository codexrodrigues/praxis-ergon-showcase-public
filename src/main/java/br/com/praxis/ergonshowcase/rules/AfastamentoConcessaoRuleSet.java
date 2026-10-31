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
 * Definição canônica do {@link RuleSetDefinition} para validação determinística de concessão,
 * prorrogação e manutenção de licenças e afastamentos ({@code PCK_LIC_AFAST.MAIN_PRE},
 * {@code PCK_LIC_AFAST.MAIN_POS}, {@code PCK_LIC_AFAST.valid_licafast},
 * {@code PCK_LIC_AFAST.valid_reg_licafast}, {@code ERG_PM_GERALICAFAST} e user exits
 * {@code EPB__LIC_AFAST} / {@code EP__VERIFICA_LIC_AFAST}).
 *
 * <p>Conecta dinamicamente a entidade {@code RegraFrequenciaEstatutaria} vigente do servidor através
 * da raiz de fatos {@code effectiveStatutoryRule} e governa as customizações por órgão nos 4 modos
 * formais do {@code praxis-rules-engine:0.1.0-beta.17} (Contrato {@code 1.6}).
 */
public final class AfastamentoConcessaoRuleSet {

  public static final String DOMAIN_SCOPE = "workforce.frequency-and-absence";
  public static final String RESOURCE_KEY = "administracao-pessoal.afastamentos";
  public static final String RULESET_KEY = "ergon.administracao-pessoal.afastamentos.concessao";
  public static final String OPERATION = "validate";
  public static final int VERSION = 1;

  public static final List<String> AVAILABLE_ROOTS =
      List.of(
          "draft",
          "vinculo",
          "pessoa",
          "tipoFrequencia",
          "codigoFrequencia",
          "documentoLegal",
          "effectiveStatutoryRule",
          "existingAfastamentos",
          "activeFrequencias",
          "afastamentoOrigem",
          "substitutoVinculo",
          "tenant",
          "context");

  // DecisionSlot keys
  public static final String SLOT_PERIODO_ORDEM_VALIDA = "afast.periodo_ordem_valida";
  public static final String SLOT_VINCULO_EM_EXERCICIO_NO_PERIODO =
      "afast.vinculo_em_exercicio_no_periodo";
  public static final String SLOT_MODALIDADE_HABILITADA_AFASTAMENTO =
      "afast.modalidade_habilitada_afastamento";
  public static final String SLOT_COMPATIBILIDADE_SEXO_SERVIDOR =
      "afast.compatibilidade_sexo_servidor";
  public static final String SLOT_REGRA_ESTATUTARIA_OBRIGATORIA =
      "afast.regra_estatutaria_obrigatoria";
  public static final String SLOT_VEDACAO_NAO_SE_APLICA = "afast.vedacao_nao_se_aplica";
  public static final String SLOT_DURACAO_MINIMA_ESTATUTARIA = "afast.duracao_minima_estatutaria";
  public static final String SLOT_DURACAO_MAXIMA_ESTATUTARIA = "afast.duracao_maxima_estatutaria";
  public static final String SLOT_PRORROGACAO_ESTATUTARIA_VALIDA =
      "afast.prorrogacao_estatutaria_valida";
  public static final String SLOT_INTERSTICIO_MINIMO_ENTRE_CONCESSOES =
      "afast.intersticio_minimo_entre_concessoes";
  public static final String SLOT_LIMITES_ACUMULADOS_MES_ANO = "afast.limites_acumulados_mes_ano";
  public static final String SLOT_DISPENSA_PERICIA_DOCUMENTO_LEGAL =
      "afast.dispensa_pericia_documento_legal";
  public static final String SLOT_SUBSTITUTO_DESIGNADO_VALIDO = "afast.substituto_designado_valido";
  public static final String SLOT_CONCOMITANCIA_FREQUENCIA_ATIVA =
      "afast.concomitancia_frequencia_ativa";
  public static final String SLOT_SOBREPOSICAO_AFASTAMENTO_ATIVO =
      "afast.sobreposicao_afastamento_ativo";

  // Parameter template & Java executor keys
  public static final String TEMPLATE_PERICIA_WAIVER_BOUNDS_KEY =
      "ergon.afastamentos:pericia-waiver-bounds";
  public static final String TEMPLATE_PERICIA_WAIVER_BOUNDS_VERSION = "1.0";
  public static final String JAVA_EXECUTOR_SOBREPOSICAO_KEY =
      "ergon.afastamentos:sobreposicao-afastamento-overlap";
  public static final String JAVA_EXECUTOR_VERSION = "1.0";

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static final String ERG07513_TEMPLATE_EXPRESSION =
      """
      {
        "or": [
          {
            "and": [
              {"!=": [{"var": "draft.documentoLegalId"}, null]},
              {
                "or": [
                  {"!=": [{"var": "documentoLegal.present"}, true]},
                  {"==": [{"var": "documentoLegal.sameOrgao"}, true]}
                ]
              }
            ]
          },
          {
            "and": [
              {">=": [{"var": "draft.effectiveDurationDays"}, 1]},
              {"<=": [{"var": "draft.effectiveDurationDays"}, 15]},
              {"!=": [{"var": "effectiveStatutoryRule.exigeDocumentoLegal"}, true]}
            ]
          }
        ]
      }
      """;

  private AfastamentoConcessaoRuleSet() {}

  /** Constrói a referência canônica do {@link RuleSetDefinition} de concessão de afastamento. */
  public static RuleSetRef ruleSetRef() {
    return new RuleSetRef(DOMAIN_SCOPE, RESOURCE_KEY, RULESET_KEY, OPERATION, VERSION);
  }

  /**
   * Constrói o {@link RuleParameterTemplate} governado para dispensa de perícia médica / documento
   * legal comprobatório até {@code N} dias ({@code ERG-07513}) sob o Contrato 1.6.
   */
  public static RuleParameterTemplate periciaWaiverBoundsTemplate() {
    return new RuleParameterTemplate(
        TEMPLATE_PERICIA_WAIVER_BOUNDS_KEY,
        TEMPLATE_PERICIA_WAIVER_BOUNDS_VERSION,
        RuleExecutorRef.jsonLogic(parseJson(ERG07513_TEMPLATE_EXPRESSION)),
        RuleDecision.DENY,
        "ERG-07513",
        List.of("draft.dtini", "draft.tipofreq", "draft.codfreq"),
        List.of(
            new RuleParameterDefinition(
                "minDaysWithoutPericia",
                RuleParameterType.NUMBER,
                true,
                false,
                false,
                "days",
                MAPPER.getNodeFactory().numberNode(1),
                BigDecimal.ONE,
                new BigDecimal("30"),
                List.of(),
                List.of("/or/1/and/0/>=/1")),
            new RuleParameterDefinition(
                "maxDaysWithoutPericia",
                RuleParameterType.NUMBER,
                true,
                false,
                false,
                "days",
                MAPPER.getNodeFactory().numberNode(15),
                BigDecimal.ONE,
                new BigDecimal("180"),
                List.of(),
                List.of("/or/1/and/1/<=/1"))));
  }

  /** Retorna a lista ordenada dos 15 {@link DecisionSlot}s de concessão de afastamento. */
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
        new DecisionSlot(
            SLOT_VINCULO_EM_EXERCICIO_NO_PERIODO,
            DecisionStage.PROTECTED_GUARD,
            SlotCardinality.SINGLE,
            OverridePolicy.FORBIDDEN,
            DecisionAggregationPolicy.SINGLE_RESULT,
            null),
        new DecisionSlot(
            SLOT_MODALIDADE_HABILITADA_AFASTAMENTO,
            DecisionStage.PROTECTED_GUARD,
            SlotCardinality.SINGLE,
            OverridePolicy.FORBIDDEN,
            DecisionAggregationPolicy.SINGLE_RESULT,
            null),

        // Stage 2: DOMAIN_DECISION
        new DecisionSlot(
            SLOT_COMPATIBILIDADE_SEXO_SERVIDOR,
            DecisionStage.DOMAIN_DECISION,
            SlotCardinality.SINGLE,
            OverridePolicy.FORBIDDEN,
            DecisionAggregationPolicy.SINGLE_RESULT,
            null),
        new DecisionSlot(
            SLOT_REGRA_ESTATUTARIA_OBRIGATORIA,
            DecisionStage.DOMAIN_DECISION,
            SlotCardinality.SINGLE,
            OverridePolicy.FORBIDDEN,
            DecisionAggregationPolicy.SINGLE_RESULT,
            null),
        new DecisionSlot(
            SLOT_VEDACAO_NAO_SE_APLICA,
            DecisionStage.DOMAIN_DECISION,
            SlotCardinality.SINGLE,
            OverridePolicy.FORBIDDEN,
            DecisionAggregationPolicy.SINGLE_RESULT,
            null),
        new DecisionSlot(
            SLOT_DURACAO_MINIMA_ESTATUTARIA,
            DecisionStage.DOMAIN_DECISION,
            SlotCardinality.MULTIPLE,
            OverridePolicy.RESTRICT_ONLY,
            DecisionAggregationPolicy.DENY_OVERRIDES,
            null),
        new DecisionSlot(
            SLOT_DURACAO_MAXIMA_ESTATUTARIA,
            DecisionStage.DOMAIN_DECISION,
            SlotCardinality.MULTIPLE,
            OverridePolicy.RESTRICT_ONLY,
            DecisionAggregationPolicy.DENY_OVERRIDES,
            null),
        new DecisionSlot(
            SLOT_PRORROGACAO_ESTATUTARIA_VALIDA,
            DecisionStage.DOMAIN_DECISION,
            SlotCardinality.MULTIPLE,
            OverridePolicy.RESTRICT_ONLY,
            DecisionAggregationPolicy.DENY_OVERRIDES,
            null),
        new DecisionSlot(
            SLOT_INTERSTICIO_MINIMO_ENTRE_CONCESSOES,
            DecisionStage.DOMAIN_DECISION,
            SlotCardinality.MULTIPLE,
            OverridePolicy.RESTRICT_ONLY,
            DecisionAggregationPolicy.DENY_OVERRIDES,
            null),
        new DecisionSlot(
            SLOT_LIMITES_ACUMULADOS_MES_ANO,
            DecisionStage.DOMAIN_DECISION,
            SlotCardinality.MULTIPLE,
            OverridePolicy.RESTRICT_ONLY,
            DecisionAggregationPolicy.DENY_OVERRIDES,
            null),
        new DecisionSlot(
            SLOT_DISPENSA_PERICIA_DOCUMENTO_LEGAL,
            DecisionStage.DOMAIN_DECISION,
            SlotCardinality.SINGLE,
            OverridePolicy.PARAMETERIZABLE,
            DecisionAggregationPolicy.SINGLE_RESULT,
            periciaWaiverBoundsTemplate()),
        new DecisionSlot(
            SLOT_SUBSTITUTO_DESIGNADO_VALIDO,
            DecisionStage.DOMAIN_DECISION,
            SlotCardinality.MULTIPLE,
            OverridePolicy.RESTRICT_ONLY,
            DecisionAggregationPolicy.DENY_OVERRIDES,
            null),
        new DecisionSlot(
            SLOT_CONCOMITANCIA_FREQUENCIA_ATIVA,
            DecisionStage.DOMAIN_DECISION,
            SlotCardinality.SINGLE,
            OverridePolicy.REPLACEABLE,
            DecisionAggregationPolicy.SINGLE_RESULT,
            null),

        // Stage 3: POST_DECISION
        new DecisionSlot(
            SLOT_SOBREPOSICAO_AFASTAMENTO_ATIVO,
            DecisionStage.POST_DECISION,
            SlotCardinality.SINGLE,
            OverridePolicy.FORBIDDEN,
            DecisionAggregationPolicy.SINGLE_RESULT,
            null));
  }

  /** Retorna os 15 {@link DecisionBinding}s canônicos do produto Ergon ({@code DecisionSource.PRODUCT}). */
  public static List<DecisionBinding> productBindings() {
    List<String> baseRequiredFacts = List.of("draft.dtini", "draft.tipofreq", "draft.codfreq");
    return List.of(
        // 1. ERG-04312: coerência cronológica (dtfim >= dtini && dtprevfim >= dtini)
        productJsonLogicBinding(
            "product.ergon.afast.erg04312",
            SLOT_PERIODO_ORDEM_VALIDA,
            10,
            """
            {
              "and": [
                {
                  "or": [
                    {"==": [{"var": "draft.dtfim"}, null]},
                    {">=": [{"date": [{"var": "draft.dtfim"}]}, {"date": [{"var": "draft.dtini"}]}]}
                  ]
                },
                {
                  "or": [
                    {"==": [{"var": "draft.dtprevfim"}, null]},
                    {">=": [{"date": [{"var": "draft.dtprevfim"}]}, {"date": [{"var": "draft.dtini"}]}]}
                  ]
                }
              ]
            }
            """,
            "ERG-04312",
            baseRequiredFacts),

        // 2. ERG-04314: vínculo em exercício na data do afastamento e antes de vacância/aposentadoria
        productJsonLogicBinding(
            "product.ergon.afast.erg04314",
            SLOT_VINCULO_EM_EXERCICIO_NO_PERIODO,
            20,
            """
            {
              "and": [
                {"==": [{"var": "vinculo.present"}, true]},
                {"==": [{"var": "vinculo.sameOrgao"}, true]},
                {">=": [{"date": [{"var": "draft.dtini"}]}, {"date": [{"var": "vinculo.dtexerc"}]}]},
                {
                  "or": [
                    {"==": [{"var": "vinculo.dtvac"}, null]},
                    {
                      "and": [
                        {"<": [{"date": [{"var": "draft.dtini"}]}, {"date": [{"var": "vinculo.dtvac"}]}]},
                        {
                          "or": [
                            {"==": [{"var": "draft.dtprevfim"}, null]},
                            {"<": [{"date": [{"var": "draft.dtprevfim"}]}, {"date": [{"var": "vinculo.dtvac"}]}]}
                          ]
                        },
                        {
                          "or": [
                            {"==": [{"var": "draft.dtfim"}, null]},
                            {"<": [{"date": [{"var": "draft.dtfim"}]}, {"date": [{"var": "vinculo.dtvac"}]}]}
                          ]
                        }
                      ]
                    }
                  ]
                },
                {
                  "or": [
                    {"==": [{"var": "vinculo.dtaposent"}, null]},
                    {
                      "and": [
                        {"<": [{"date": [{"var": "draft.dtini"}]}, {"date": [{"var": "vinculo.dtaposent"}]}]},
                        {
                          "or": [
                            {"==": [{"var": "draft.dtprevfim"}, null]},
                            {"<": [{"date": [{"var": "draft.dtprevfim"}]}, {"date": [{"var": "vinculo.dtaposent"}]}]}
                          ]
                        },
                        {
                          "or": [
                            {"==": [{"var": "draft.dtfim"}, null]},
                            {"<": [{"date": [{"var": "draft.dtfim"}]}, {"date": [{"var": "vinculo.dtaposent"}]}]}
                          ]
                        }
                      ]
                    }
                  ]
                }
              ]
            }
            """,
            "ERG-04314",
            baseRequiredFacts),

        // 3. ERG-04306: código de frequência existente e habilitado para afastamento
        productJsonLogicBinding(
            "product.ergon.afast.erg04306",
            SLOT_MODALIDADE_HABILITADA_AFASTAMENTO,
            30,
            """
            {
              "and": [
                {"==": [{"var": "codigoFrequencia.present"}, true]},
                {"==": [{"var": "codigoFrequencia.afastamento"}, true]},
                {"==": [{"var": "codigoFrequencia.tipofreq"}, {"var": "draft.tipofreq"}]},
                {
                  "or": [
                    {"!=": [{"var": "tipoFrequencia.present"}, true]},
                    {"==": [{"var": "tipoFrequencia.tipo"}, {"var": "codigoFrequencia.tipofreq"}]}
                  ]
                }
              ]
            }
            """,
            "ERG-04306",
            baseRequiredFacts),

        // 4. ERG-04310: compatibilidade de sexo do servidor com a modalidade de licença
        productJsonLogicBinding(
            "product.ergon.afast.erg04310",
            SLOT_COMPATIBILIDADE_SEXO_SERVIDOR,
            40,
            """
            {
              "or": [
                {"==": [{"var": "codigoFrequencia.sexo"}, null]},
                {"==": [{"var": "codigoFrequencia.sexo"}, "A"]},
                {"==": [{"var": "codigoFrequencia.sexo"}, "I"]},
                {"==": [{"var": "codigoFrequencia.sexo"}, {"var": "pessoa.sexo"}]}
              ]
            }
            """,
            "ERG-04310",
            baseRequiredFacts),

        // 5. ERG-04305: exige regra estatutária vigente quando codigoFrequencia.regrasFreq == true
        productJsonLogicBinding(
            "product.ergon.afast.erg04305",
            SLOT_REGRA_ESTATUTARIA_OBRIGATORIA,
            50,
            """
            {
              "or": [
                {"!=": [{"var": "codigoFrequencia.regrasFreq"}, true]},
                {"==": [{"var": "effectiveStatutoryRule.present"}, true]}
              ]
            }
            """,
            "ERG-04305",
            baseRequiredFacts),

        // 6. ERG-04301: vedação estatutária expressa (effectiveStatutoryRule.naoSeAplica == true)
        productJsonLogicBinding(
            "product.ergon.afast.erg04301",
            SLOT_VEDACAO_NAO_SE_APLICA,
            60,
            """
            {
              "or": [
                {"!=": [{"var": "effectiveStatutoryRule.present"}, true]},
                {"!=": [{"var": "effectiveStatutoryRule.naoSeAplica"}, true]}
              ]
            }
            """,
            "ERG-04301",
            baseRequiredFacts),

        // 7. ERG-04304: duração mínima da modalidade e da regra estatutária
        productJsonLogicBinding(
            "product.ergon.afast.erg04304",
            SLOT_DURACAO_MINIMA_ESTATUTARIA,
            70,
            """
            {
              "and": [
                {
                  "or": [
                    {"==": [{"var": "codigoFrequencia.qtdmin"}, null]},
                    {"==": [{"var": "draft.effectiveDurationDays"}, null]},
                    {">=": [{"var": "draft.effectiveDurationDays"}, {"var": "codigoFrequencia.qtdmin"}]}
                  ]
                },
                {
                  "or": [
                    {"!=": [{"var": "effectiveStatutoryRule.present"}, true]},
                    {"==": [{"var": "effectiveStatutoryRule.qtdMinDias"}, null]},
                    {"==": [{"var": "draft.effectiveDurationDays"}, null]},
                    {">=": [{"var": "draft.effectiveDurationDays"}, {"var": "effectiveStatutoryRule.qtdMinDias"}]}
                  ]
                }
              ]
            }
            """,
            "ERG-04304",
            baseRequiredFacts),

        // 8. ERG-04303: duração máxima da modalidade e da regra estatutária
        productJsonLogicBinding(
            "product.ergon.afast.erg04303",
            SLOT_DURACAO_MAXIMA_ESTATUTARIA,
            80,
            """
            {
              "and": [
                {
                  "or": [
                    {"==": [{"var": "codigoFrequencia.qtdmax"}, null]},
                    {"==": [{"var": "draft.effectiveDurationDays"}, null]},
                    {"<=": [{"var": "draft.effectiveDurationDays"}, {"var": "codigoFrequencia.qtdmax"}]}
                  ]
                },
                {
                  "or": [
                    {"!=": [{"var": "effectiveStatutoryRule.present"}, true]},
                    {"==": [{"var": "effectiveStatutoryRule.qtdMaxDias"}, null]},
                    {"==": [{"var": "draft.effectiveDurationDays"}, null]},
                    {"<=": [{"var": "draft.effectiveDurationDays"}, {"var": "effectiveStatutoryRule.qtdMaxDias"}]}
                  ]
                }
              ]
            }
            """,
            "ERG-04303",
            baseRequiredFacts),

        // 9. ERG-04317: validação de prorrogação (afastamentoOrigem, janela intProrrogDias e acumulado <= qtdMaxDias)
        productJsonLogicBinding(
            "product.ergon.afast.erg04317",
            SLOT_PRORROGACAO_ESTATUTARIA_VALIDA,
            90,
            """
            {
              "or": [
                {"!=": [{"var": "draft.prorrogacao"}, true]},
                {
                  "and": [
                    {"!=": [{"var": "effectiveStatutoryRule.prorrogacaoPermitida"}, false]},
                    {
                      "or": [
                        {"==": [{"var": "draft.inPlaceProrrogacao"}, true]},
                        {
                          "and": [
                            {"!=": [{"var": "draft.afastamentoOrigemId"}, null]},
                            {"==": [{"var": "afastamentoOrigem.present"}, true]},
                            {"==": [{"var": "afastamentoOrigem.sameVinculo"}, true]},
                            {"!=": [{"var": "afastamentoOrigem.selfReference"}, true]},
                            {"==": [{"var": "afastamentoOrigem.activeOrigin"}, true]},
                            {"==": [{"var": "afastamentoOrigem.chronologicalAfterOrigin"}, true]}
                          ]
                        }
                      ]
                    },
                    {
                      "or": [
                        {"!=": [{"var": "effectiveStatutoryRule.present"}, true]},
                        {"==": [{"var": "effectiveStatutoryRule.intProrrogDias"}, null]},
                        {"==": [{"var": "afastamentoOrigem.gapDays"}, null]},
                        {"<=": [{"var": "afastamentoOrigem.gapDays"}, {"var": "effectiveStatutoryRule.intProrrogDias"}]}
                      ]
                    },
                    {
                      "or": [
                        {"!=": [{"var": "effectiveStatutoryRule.present"}, true]},
                        {"==": [{"var": "effectiveStatutoryRule.qtdMaxDias"}, null]},
                        {"<=": [{"var": "effectiveStatutoryRule.accumulatedProrrogDays"}, {"var": "effectiveStatutoryRule.qtdMaxDias"}]}
                      ]
                    }
                  ]
                }
              ]
            }
            """,
            "ERG-04317",
            baseRequiredFacts),

        // 10. ERG-04309: interstício mínimo (intervMinDias) entre concessões da mesma espécie
        productJsonLogicBinding(
            "product.ergon.afast.erg04309",
            SLOT_INTERSTICIO_MINIMO_ENTRE_CONCESSOES,
            100,
            """
            {
              "or": [
                {"==": [{"var": "draft.prorrogacao"}, true]},
                {"!=": [{"var": "effectiveStatutoryRule.present"}, true]},
                {"==": [{"var": "effectiveStatutoryRule.intervMinDias"}, null]},
                {"==": [{"var": "effectiveStatutoryRule.daysSincePreviousOccurrence"}, null]},
                {">=": [{"var": "effectiveStatutoryRule.daysSincePreviousOccurrence"}, {"var": "effectiveStatutoryRule.intervMinDias"}]}
              ]
            }
            """,
            "ERG-04309",
            baseRequiredFacts),

        // 11. ERG-04311: limites acumulados mensais e anuais (consecutivos e interpolados)
        productJsonLogicBinding(
            "product.ergon.afast.erg04311",
            SLOT_LIMITES_ACUMULADOS_MES_ANO,
            110,
            """
            {
              "or": [
                {"!=": [{"var": "effectiveStatutoryRule.present"}, true]},
                {
                  "and": [
                    {
                      "or": [
                        {"==": [{"var": "effectiveStatutoryRule.qtdMaxConsecMes"}, null]},
                        {"<=": [{"var": "effectiveStatutoryRule.projectedConsecMesDays"}, {"var": "effectiveStatutoryRule.qtdMaxConsecMes"}]}
                      ]
                    },
                    {
                      "or": [
                        {"==": [{"var": "effectiveStatutoryRule.qtdMaxInterpMes"}, null]},
                        {"<=": [{"var": "effectiveStatutoryRule.projectedInterpMesDays"}, {"var": "effectiveStatutoryRule.qtdMaxInterpMes"}]}
                      ]
                    },
                    {
                      "or": [
                        {"==": [{"var": "effectiveStatutoryRule.qtdMaxConsecAno"}, null]},
                        {"<=": [{"var": "effectiveStatutoryRule.projectedConsecAnoDays"}, {"var": "effectiveStatutoryRule.qtdMaxConsecAno"}]}
                      ]
                    },
                    {
                      "or": [
                        {"==": [{"var": "effectiveStatutoryRule.qtdMaxInterpAno"}, null]},
                        {"<=": [{"var": "effectiveStatutoryRule.projectedInterpAnoDays"}, {"var": "effectiveStatutoryRule.qtdMaxInterpAno"}]}
                      ]
                    }
                  ]
                }
              ]
            }
            """,
            "ERG-04311",
            baseRequiredFacts),

        // 12. ERG-07513: dispensa parametrizada de perícia médica / documento legal até N dias
        productJsonLogicBinding(
            "product.ergon.afast.erg07513",
            SLOT_DISPENSA_PERICIA_DOCUMENTO_LEGAL,
            120,
            ERG07513_TEMPLATE_EXPRESSION,
            "ERG-07513",
            baseRequiredFacts),

        // 13. ERG-04318: consistência do vínculo substituto designado
        productJsonLogicBinding(
            "product.ergon.afast.erg04318",
            SLOT_SUBSTITUTO_DESIGNADO_VALIDO,
            130,
            """
            {
              "or": [
                {
                  "and": [
                    {"==": [{"var": "draft.numfuncSubstituto"}, null]},
                    {"==": [{"var": "draft.numvincSubstituto"}, null]}
                  ]
                },
                {
                  "and": [
                    {"!=": [{"var": "draft.numfuncSubstituto"}, null]},
                    {"!=": [{"var": "draft.numvincSubstituto"}, null]},
                    {"!=": [{"var": "draft.numfuncSubstituto"}, {"var": "draft.numfunc"}]},
                    {"==": [{"var": "substitutoVinculo.present"}, true]},
                    {"==": [{"var": "substitutoVinculo.sameOrgao"}, true]},
                    {"==": [{"var": "substitutoVinculo.ativo"}, true]}
                  ]
                }
              ]
            }
            """,
            "ERG-04318",
            baseRequiredFacts),

        // 14. ERG-04316: concomitância cruzada com Frequencia ativa no período (REPLACEABLE)
        productJsonLogicBinding(
            "product.ergon.afast.erg04316",
            SLOT_CONCOMITANCIA_FREQUENCIA_ATIVA,
            140,
            """
            {
              "or": [
                {"==": [{"var": "effectiveStatutoryRule.overlappingFrequenciasCount"}, 0]},
                {"==": [{"var": "effectiveStatutoryRule.permiteConcomitancia"}, true]}
              ]
            }
            """,
            "ERG-04316",
            baseRequiredFacts),

        // 15. ERG-04300: sobreposição de afastamentos ativos no mesmo vínculo (POST_DECISION Java)
        new DecisionBinding(
            "product.ergon.afast.erg04300",
            SLOT_SOBREPOSICAO_AFASTAMENTO_ATIVO,
            DecisionSource.PRODUCT,
            null,
            RuleExecutorRef.java(JAVA_EXECUTOR_SOBREPOSICAO_KEY, JAVA_EXECUTOR_VERSION),
            List.of(),
            150,
            true,
            null,
            null,
            List.of("draft.dtini", "existingAfastamentos"),
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
   * cliente ao slot protegido {@code ERG-04312} ({@link DecisionStage#PROTECTED_GUARD}).
   */
  public static RuleSetDefinition customerMode1ForbiddenOverrideAttempt() {
    DecisionBinding illegalOverride =
        new DecisionBinding(
            "customer.emp2.afast.bypass_erg04312",
            SLOT_PERIODO_ORDEM_VALIDA,
            DecisionSource.CUSTOMER,
            CompositionPolicy.REPLACE_EXACT,
            RuleExecutorRef.jsonLogic(parseJson("{\"==\": [1, 1]}")),
            List.of(),
            15,
            true,
            RuleDecision.DENY,
            "ERG-04312",
            List.of("draft.dtini"),
            null);
    return withCustomerBindings(List.of(illegalOverride));
  }

  /**
   * <b>Modo 2 ({@link OverridePolicy#PARAMETERIZABLE} + {@link CompositionPolicy#PARAMETERIZE}):</b>
   * ajusta o limite de dias para dispensa de perícia médica / documento legal
   * ({@code maxDaysWithoutPericia}) para o órgão sem alterar a árvore JSON Logic de {@code ERG-07513}.
   */
  public static DecisionBinding customerMode2ParameterizeBinding(
      int minDaysWithoutPericia, int maxDaysWithoutPericia) {
    ObjectNode values = MAPPER.createObjectNode();
    values.put("minDaysWithoutPericia", minDaysWithoutPericia);
    values.put("maxDaysWithoutPericia", maxDaysWithoutPericia);
    return new DecisionBinding(
        "customer.emp2.afast.parameterize_erg07513",
        SLOT_DISPENSA_PERICIA_DOCUMENTO_LEGAL,
        DecisionSource.CUSTOMER,
        CompositionPolicy.PARAMETERIZE,
        null,
        List.of(),
        125,
        true,
        null,
        null,
        List.of(),
        new RuleParameterization(
            TEMPLATE_PERICIA_WAIVER_BOUNDS_KEY, TEMPLATE_PERICIA_WAIVER_BOUNDS_VERSION, values));
  }

  /**
   * <b>Modo 3 ({@link OverridePolicy#RESTRICT_ONLY} + {@link CompositionPolicy#AUGMENT}):</b>
   * adiciona a regra específica do órgão {@code empCodigo = 2} ({@code CERG-09201}) exigindo
   * {@code atoPublicacaoId != null} para afastamentos com duração superior a {@code 30} dias,
   * preservando os tetos mensais/anuais do produto ({@code ERG-04311}) ativos sob {@code DENY_OVERRIDES}.
   */
  public static DecisionBinding customerMode3AugmentBinding() {
    return new DecisionBinding(
        "customer.emp2.afast.augment_cerg09201",
        SLOT_LIMITES_ACUMULADOS_MES_ANO,
        DecisionSource.CUSTOMER,
        CompositionPolicy.AUGMENT,
        RuleExecutorRef.jsonLogic(
            parseJson(
                """
                {
                  "or": [
                    {"==": [{"var": "draft.effectiveDurationDays"}, null]},
                    {"<=": [{"var": "draft.effectiveDurationDays"}, 30]},
                    {"!=": [{"var": "draft.atoPublicacaoId"}, null]}
                  ]
                }
                """)),
        List.of(),
        115,
        true,
        RuleDecision.DENY,
        "CERG-09201",
        List.of("draft.dtini", "draft.tipofreq", "draft.codfreq"),
        null);
  }

  /**
   * <b>Modo 4 ({@link OverridePolicy#REPLACEABLE} + {@link CompositionPolicy#REPLACE_EXACT}):</b>
   * substitui exclusivamente a política de concomitância cruzada com frequência ativa
   * ({@link #SLOT_CONCOMITANCIA_FREQUENCIA_ATIVA} — {@code ERG-04316} -> {@code CERG-04316}) para o
   * órgão {@code empCodigo = 2}, bloqueando concomitância mesmo quando
   * {@code permiteConcomitancia == true}, sem desligar nenhuma das outras 14 decisões do produto.
   */
  public static DecisionBinding customerMode4ReplaceExactBinding() {
    return new DecisionBinding(
        "customer.emp2.afast.replace_erg04316",
        SLOT_CONCOMITANCIA_FREQUENCIA_ATIVA,
        DecisionSource.CUSTOMER,
        CompositionPolicy.REPLACE_EXACT,
        RuleExecutorRef.jsonLogic(
            parseJson(
                """
                {
                  "==": [{"var": "effectiveStatutoryRule.overlappingFrequenciasCount"}, 0]
                }
                """)),
        List.of(),
        145,
        true,
        RuleDecision.DENY,
        "CERG-04316",
        List.of("draft.dtini", "draft.tipofreq", "draft.codfreq"),
        null);
  }

  /** Constrói o {@link RuleSetDefinition} customizado do Órgão 2 ({@code empCodigo = 2}). */
  public static RuleSetDefinition orgao2CustomizedRuleSet() {
    return withCustomerBindings(
        List.of(
            customerMode2ParameterizeBinding(1, 5),
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
          "Invalid static JSON Logic in AfastamentoConcessaoRuleSet", ex);
    }
  }
}
