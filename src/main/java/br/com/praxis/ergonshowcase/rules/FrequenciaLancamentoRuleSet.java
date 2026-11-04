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
 * Definição canônica do {@link RuleSetDefinition} para validação determinística de lançamento e
 * manutenção de ocorrências de frequência ({@code PCK_FREQUENCIAS.MAIN_PRE},
 * {@code PCK_FREQUENCIAS.MAIN_POS}, {@code PCK_FREQUENCIAS.valid_regras_freq} e user exits
 * {@code EPB__FREQUENCIAS} / {@code EP__VERIFICA_FREQUENCIAS}).
 *
 * <p>Conecta dinamicamente a entidade {@code RegraFrequenciaEstatutaria} vigente do servidor através
 * da raiz de fatos {@code effectiveStatutoryRule} e governa as customizações por órgão nos 4 modos
 * formais do {@code praxis-rules-engine:0.1.0-beta.17} (Contrato {@code 1.6}).
 */
public final class FrequenciaLancamentoRuleSet {

  public static final String DOMAIN_SCOPE = "workforce.frequency-and-absence";
  public static final String RESOURCE_KEY = "administracao-pessoal.frequencias";
  public static final String RULESET_KEY = "ergon.administracao-pessoal.frequencias.lancamento";
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
          "existingFrequencias",
          "activeAfastamentos",
          "tenant",
          "context");

  // DecisionSlot keys
  public static final String SLOT_PERIODO_ORDEM_VALIDA = "freq.periodo_ordem_valida";
  public static final String SLOT_CODIGO_HABILITADO_VIA_FREQUENCIA =
      "freq.codigo_habilitado_via_frequencia";
  public static final String SLOT_VINCULO_VIGENTE_NO_PERIODO = "freq.vinculo_vigente_no_periodo";
  public static final String SLOT_HORARIO_ENTRADA_SAIDA_VALIDO =
      "freq.horario_entrada_saida_valido";
  public static final String SLOT_COMPATIBILIDADE_SEXO_SERVIDOR =
      "freq.compatibilidade_sexo_servidor";
  public static final String SLOT_REGRA_ESTATUTARIA_OBRIGATORIA =
      "freq.regra_estatutaria_obrigatoria";
  public static final String SLOT_VEDACAO_NAO_SE_APLICA = "freq.vedacao_nao_se_aplica";
  public static final String SLOT_DURACAO_MINIMA_ESTATUTARIA = "freq.duracao_minima_estatutaria";
  public static final String SLOT_DURACAO_MAXIMA_ESTATUTARIA = "freq.duracao_maxima_estatutaria";
  public static final String SLOT_LIMITE_PRORROGACAO_ACUMULADA =
      "freq.limite_prorrogacao_acumulada";
  public static final String SLOT_INTERSTICIO_MINIMO_ENTRE_OCORRENCIAS =
      "freq.intersticio_minimo_entre_ocorrencias";
  public static final String SLOT_LIMITES_ACUMULADOS_MES_ANO = "freq.limites_acumulados_mes_ano";
  public static final String SLOT_EXIGENCIA_DOCUMENTO_LEGAL = "freq.exigencia_documento_legal";
  public static final String SLOT_LIMITE_RETROATIVIDADE_LANCAMENTO =
      "freq.limite_retroatividade_lancamento";
  public static final String SLOT_CONCOMITANCIA_AFASTAMENTO_ATIVO =
      "freq.concomitancia_afastamento_ativo";
  public static final String SLOT_SOBREPOSICAO_OCORRENCIA_MESMO_TIPO =
      "freq.sobreposicao_ocorrencia_mesmo_tipo";

  // Parameter template & Java executor keys
  public static final String TEMPLATE_RETROACTIVE_BOUNDS_KEY =
      "ergon.frequencias:retroactive-launch-bounds";
  public static final String TEMPLATE_RETROACTIVE_BOUNDS_VERSION = "1.0";
  public static final String JAVA_EXECUTOR_SOBREPOSICAO_KEY =
      "ergon.frequencias:sobreposicao-periodo-overlap";
  public static final String JAVA_EXECUTOR_VERSION = "1.0";

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static final String ERG00446_TEMPLATE_EXPRESSION =
      """
      {
        "or": [
          {"==": [{"var": "draft.retroactiveDays"}, null]},
          {
            "and": [
              {">=": [{"var": "draft.retroactiveDays"}, 0]},
              {"<=": [{"var": "draft.retroactiveDays"}, 60]}
            ]
          }
        ]
      }
      """;

  private FrequenciaLancamentoRuleSet() {}

  /** Constrói a referência canônica do {@link RuleSetDefinition} de lançamento de frequência. */
  public static RuleSetRef ruleSetRef() {
    return new RuleSetRef(DOMAIN_SCOPE, RESOURCE_KEY, RULESET_KEY, OPERATION, VERSION);
  }

  /**
   * Constrói o {@link RuleParameterTemplate} governado para parametrização da janela máxima de
   * lançamento/abono retroativo de frequência ({@code ERG-00446}) sob o Contrato 1.6.
   */
  public static RuleParameterTemplate retroactiveLaunchBoundsTemplate() {
    return new RuleParameterTemplate(
        TEMPLATE_RETROACTIVE_BOUNDS_KEY,
        TEMPLATE_RETROACTIVE_BOUNDS_VERSION,
        RuleExecutorRef.jsonLogic(parseJson(ERG00446_TEMPLATE_EXPRESSION)),
        RuleDecision.DENY,
        "ERG-00446",
        List.of("draft.dtini", "draft.tipofreq", "draft.codfreq"),
        List.of(
            new RuleParameterDefinition(
                "minRetroactiveDays",
                RuleParameterType.NUMBER,
                true,
                false,
                false,
                "days",
                MAPPER.getNodeFactory().numberNode(0),
                BigDecimal.ZERO,
                new BigDecimal("30"),
                List.of(),
                List.of("/or/1/and/0/>=/1")),
            new RuleParameterDefinition(
                "maxRetroactiveDays",
                RuleParameterType.NUMBER,
                true,
                false,
                false,
                "days",
                MAPPER.getNodeFactory().numberNode(60),
                BigDecimal.ONE,
                new BigDecimal("365"),
                List.of(),
                List.of("/or/1/and/1/<=/1"))));
  }

  /** Retorna a lista ordenada dos 16 {@link DecisionSlot}s de lançamento de frequência. */
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
            SLOT_CODIGO_HABILITADO_VIA_FREQUENCIA,
            DecisionStage.PROTECTED_GUARD,
            SlotCardinality.SINGLE,
            OverridePolicy.FORBIDDEN,
            DecisionAggregationPolicy.SINGLE_RESULT,
            null),
        new DecisionSlot(
            SLOT_VINCULO_VIGENTE_NO_PERIODO,
            DecisionStage.PROTECTED_GUARD,
            SlotCardinality.MULTIPLE,
            OverridePolicy.FORBIDDEN,
            DecisionAggregationPolicy.DENY_OVERRIDES,
            null),

        // Stage 2: DOMAIN_DECISION
        new DecisionSlot(
            SLOT_HORARIO_ENTRADA_SAIDA_VALIDO,
            DecisionStage.DOMAIN_DECISION,
            SlotCardinality.MULTIPLE,
            OverridePolicy.RESTRICT_ONLY,
            DecisionAggregationPolicy.DENY_OVERRIDES,
            null),
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
            SLOT_LIMITE_PRORROGACAO_ACUMULADA,
            DecisionStage.DOMAIN_DECISION,
            SlotCardinality.MULTIPLE,
            OverridePolicy.RESTRICT_ONLY,
            DecisionAggregationPolicy.DENY_OVERRIDES,
            null),
        new DecisionSlot(
            SLOT_INTERSTICIO_MINIMO_ENTRE_OCORRENCIAS,
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
            SLOT_EXIGENCIA_DOCUMENTO_LEGAL,
            DecisionStage.DOMAIN_DECISION,
            SlotCardinality.MULTIPLE,
            OverridePolicy.RESTRICT_ONLY,
            DecisionAggregationPolicy.DENY_OVERRIDES,
            null),
        new DecisionSlot(
            SLOT_LIMITE_RETROATIVIDADE_LANCAMENTO,
            DecisionStage.DOMAIN_DECISION,
            SlotCardinality.SINGLE,
            OverridePolicy.PARAMETERIZABLE,
            DecisionAggregationPolicy.SINGLE_RESULT,
            retroactiveLaunchBoundsTemplate()),
        new DecisionSlot(
            SLOT_CONCOMITANCIA_AFASTAMENTO_ATIVO,
            DecisionStage.DOMAIN_DECISION,
            SlotCardinality.SINGLE,
            OverridePolicy.REPLACEABLE,
            DecisionAggregationPolicy.SINGLE_RESULT,
            null),

        // Stage 3: POST_DECISION
        new DecisionSlot(
            SLOT_SOBREPOSICAO_OCORRENCIA_MESMO_TIPO,
            DecisionStage.POST_DECISION,
            SlotCardinality.SINGLE,
            OverridePolicy.FORBIDDEN,
            DecisionAggregationPolicy.SINGLE_RESULT,
            null));
  }

  /** Retorna os 16 {@link DecisionBinding}s canônicos do produto Ergon ({@code DecisionSource.PRODUCT}). */
  public static List<DecisionBinding> productBindings() {
    List<String> baseRequiredFacts = List.of("draft.dtini", "draft.tipofreq", "draft.codfreq");
    return List.of(
        // 1. ERG-00415: dtfim == null || date(dtfim) >= date(dtini)
        productJsonLogicBinding(
            "product.ergon.freq.erg00415",
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
            "ERG-00415",
            baseRequiredFacts),

        // 2. ERG-00400: código existente, pertencente ao tipofreq e com viaFrequencia == true
        productJsonLogicBinding(
            "product.ergon.freq.erg00400",
            SLOT_CODIGO_HABILITADO_VIA_FREQUENCIA,
            20,
            """
            {
              "and": [
                {"==": [{"var": "codigoFrequencia.present"}, true]},
                {"==": [{"var": "codigoFrequencia.viaFrequencia"}, true]},
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
            "ERG-00400",
            baseRequiredFacts),

        // 3. ERG-00410: início da frequência >= data de exercício do vínculo ativo
        productJsonLogicBinding(
            "product.ergon.freq.erg00410",
            SLOT_VINCULO_VIGENTE_NO_PERIODO,
            30,
            """
            {
              "and": [
                {"==": [{"var": "vinculo.present"}, true]},
                {"==": [{"var": "vinculo.sameOrgao"}, true]},
                {">=": [{"date": [{"var": "draft.dtini"}]}, {"date": [{"var": "vinculo.dtexerc"}]}]}
              ]
            }
            """,
            "ERG-00410",
            baseRequiredFacts),

        // 3b. ERG-00411: período da frequência anterior à vacância ou aposentadoria do vínculo
        productJsonLogicBinding(
            "product.ergon.freq.erg00411",
            SLOT_VINCULO_VIGENTE_NO_PERIODO,
            31,
            """
            {
              "and": [
                {
                  "or": [
                    {"==": [{"var": "vinculo.dtvac"}, null]},
                    {
                      "and": [
                        {"<": [{"date": [{"var": "draft.dtini"}]}, {"date": [{"var": "vinculo.dtvac"}]}]},
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
            "ERG-00411",
            baseRequiredFacts),

        // 4. ERG-00436: horário de entrada/saída obrigatório quando horaEntradaSaida == true e coerente
        productJsonLogicBinding(
            "product.ergon.freq.erg00436",
            SLOT_HORARIO_ENTRADA_SAIDA_VALIDO,
            40,
            """
            {
              "and": [
                {
                  "or": [
                    {"!=": [{"var": "codigoFrequencia.horaEntradaSaida"}, true]},
                    {
                      "and": [
                        {"!=": [{"var": "draft.horaEntradaMinutes"}, null]},
                        {"!=": [{"var": "draft.horaSaidaMinutes"}, null]}
                      ]
                    }
                  ]
                },
                {
                  "or": [
                    {
                      "and": [
                        {"==": [{"var": "draft.horaEntradaMinutes"}, null]},
                        {"==": [{"var": "draft.horaSaidaMinutes"}, null]}
                      ]
                    },
                    {
                      "and": [
                        {"!=": [{"var": "draft.horaEntradaMinutes"}, null]},
                        {"!=": [{"var": "draft.horaSaidaMinutes"}, null]},
                        {">=": [{"var": "draft.horaSaidaMinutes"}, {"var": "draft.horaEntradaMinutes"}]}
                      ]
                    }
                  ]
                }
              ]
            }
            """,
            "ERG-00436",
            baseRequiredFacts),

        // 5. ERG-00443: compatibilidade de sexo do servidor com codigoFrequencia.sexo
        productJsonLogicBinding(
            "product.ergon.freq.erg00443",
            SLOT_COMPATIBILIDADE_SEXO_SERVIDOR,
            50,
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
            "ERG-00443",
            baseRequiredFacts),

        // 6. ERG-00441: exige regra estatutária vigente quando codigoFrequencia.regrasFreq == true
        productJsonLogicBinding(
            "product.ergon.freq.erg00441",
            SLOT_REGRA_ESTATUTARIA_OBRIGATORIA,
            60,
            """
            {
              "or": [
                {"!=": [{"var": "codigoFrequencia.regrasFreq"}, true]},
                {"==": [{"var": "effectiveStatutoryRule.present"}, true]}
              ]
            }
            """,
            "ERG-00441",
            baseRequiredFacts),

        // 7. ERG-00430: vedação estatutária expressa (effectiveStatutoryRule.naoSeAplica == true)
        productJsonLogicBinding(
            "product.ergon.freq.erg00430",
            SLOT_VEDACAO_NAO_SE_APLICA,
            70,
            """
            {
              "or": [
                {"!=": [{"var": "effectiveStatutoryRule.present"}, true]},
                {"!=": [{"var": "effectiveStatutoryRule.naoSeAplica"}, true]}
              ]
            }
            """,
            "ERG-00430",
            baseRequiredFacts),

        // 8. ERG-00427: duração/quantidade mínima estatutária e da modalidade
        productJsonLogicBinding(
            "product.ergon.freq.erg00427",
            SLOT_DURACAO_MINIMA_ESTATUTARIA,
            80,
            """
            {
              "and": [
                {
                  "or": [
                    {"==": [{"var": "codigoFrequencia.qtdmin"}, null]},
                    {">=": [{"var": "draft.effectiveQuantity"}, {"var": "codigoFrequencia.qtdmin"}]}
                  ]
                },
                {
                  "or": [
                    {"!=": [{"var": "effectiveStatutoryRule.present"}, true]},
                    {"==": [{"var": "effectiveStatutoryRule.qtdMinDias"}, null]},
                    {">=": [{"var": "draft.effectiveDurationDays"}, {"var": "effectiveStatutoryRule.qtdMinDias"}]}
                  ]
                }
              ]
            }
            """,
            "ERG-00427",
            baseRequiredFacts),

        // 9. ERG-00428: duração/quantidade máxima estatutária e da modalidade
        productJsonLogicBinding(
            "product.ergon.freq.erg00428",
            SLOT_DURACAO_MAXIMA_ESTATUTARIA,
            90,
            """
            {
              "and": [
                {
                  "or": [
                    {"==": [{"var": "codigoFrequencia.qtdmax"}, null]},
                    {"<=": [{"var": "draft.effectiveQuantity"}, {"var": "codigoFrequencia.qtdmax"}]}
                  ]
                },
                {
                  "or": [
                    {"!=": [{"var": "effectiveStatutoryRule.present"}, true]},
                    {"==": [{"var": "effectiveStatutoryRule.qtdMaxDias"}, null]},
                    {"<=": [{"var": "draft.effectiveDurationDays"}, {"var": "effectiveStatutoryRule.qtdMaxDias"}]}
                  ]
                }
              ]
            }
            """,
            "ERG-00428",
            baseRequiredFacts),

        // 10. ERG-00429: soma acumulada na janela intProrrogDias <= qtdMaxDias
        productJsonLogicBinding(
            "product.ergon.freq.erg00429",
            SLOT_LIMITE_PRORROGACAO_ACUMULADA,
            100,
            """
            {
              "or": [
                {"!=": [{"var": "effectiveStatutoryRule.present"}, true]},
                {"==": [{"var": "effectiveStatutoryRule.intProrrogDias"}, null]},
                {"==": [{"var": "effectiveStatutoryRule.qtdMaxDias"}, null]},
                {"<=": [{"var": "effectiveStatutoryRule.accumulatedProrrogDays"}, {"var": "effectiveStatutoryRule.qtdMaxDias"}]}
              ]
            }
            """,
            "ERG-00429",
            baseRequiredFacts),

        // 11. ERG-00440: interstício mínimo (intervMinDias) desde a ocorrência anterior
        productJsonLogicBinding(
            "product.ergon.freq.erg00440",
            SLOT_INTERSTICIO_MINIMO_ENTRE_OCORRENCIAS,
            110,
            """
            {
              "or": [
                {"!=": [{"var": "effectiveStatutoryRule.present"}, true]},
                {"==": [{"var": "effectiveStatutoryRule.intervMinDias"}, null]},
                {"==": [{"var": "effectiveStatutoryRule.daysSincePreviousOccurrence"}, null]},
                {">=": [{"var": "effectiveStatutoryRule.daysSincePreviousOccurrence"}, {"var": "effectiveStatutoryRule.intervMinDias"}]}
              ]
            }
            """,
            "ERG-00440",
            baseRequiredFacts),

        // 12. ERG-00444: limites mensais e anuais (consecutivos e interpolados)
        productJsonLogicBinding(
            "product.ergon.freq.erg00444",
            SLOT_LIMITES_ACUMULADOS_MES_ANO,
            120,
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
            "ERG-00444",
            baseRequiredFacts),

        // 13. ERG-00445: exigência de documento legal comprobatório pela regra estatutária
        productJsonLogicBinding(
            "product.ergon.freq.erg00445",
            SLOT_EXIGENCIA_DOCUMENTO_LEGAL,
            130,
            """
            {
              "and": [
                {
                  "or": [
                    {"!=": [{"var": "effectiveStatutoryRule.present"}, true]},
                    {"!=": [{"var": "effectiveStatutoryRule.exigeDocumentoLegal"}, true]},
                    {"!=": [{"var": "draft.documentoLegalId"}, null]}
                  ]
                },
                {
                  "or": [
                    {"!=": [{"var": "documentoLegal.present"}, true]},
                    {"==": [{"var": "documentoLegal.sameOrgao"}, true]}
                  ]
                }
              ]
            }
            """,
            "ERG-00445",
            baseRequiredFacts),

        // 14. ERG-00446: limite parametrizado de dias para lançamento/abono retroativo
        productJsonLogicBinding(
            "product.ergon.freq.erg00446",
            SLOT_LIMITE_RETROATIVIDADE_LANCAMENTO,
            140,
            ERG00446_TEMPLATE_EXPRESSION,
            "ERG-00446",
            baseRequiredFacts),

        // 15. ERG-00417: concomitância cruzada com Afastamento ativo no período (REPLACEABLE)
        productJsonLogicBinding(
            "product.ergon.freq.erg00417",
            SLOT_CONCOMITANCIA_AFASTAMENTO_ATIVO,
            150,
            """
            {
              "or": [
                {"==": [{"var": "effectiveStatutoryRule.overlappingAfastamentosCount"}, 0]},
                {"==": [{"var": "effectiveStatutoryRule.permiteConcomitancia"}, true]}
              ]
            }
            """,
            "ERG-00417",
            baseRequiredFacts),

        // 16. ERG-00424: sobreposição de ocorrências de frequência ativas no mesmo período (POST_DECISION Java)
        new DecisionBinding(
            "product.ergon.freq.erg00424",
            SLOT_SOBREPOSICAO_OCORRENCIA_MESMO_TIPO,
            DecisionSource.PRODUCT,
            null,
            RuleExecutorRef.java(JAVA_EXECUTOR_SOBREPOSICAO_KEY, JAVA_EXECUTOR_VERSION),
            List.of(),
            160,
            true,
            null,
            null,
            List.of("draft.dtini", "existingFrequencias"),
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
   * cliente ao slot protegido {@code ERG-00415} ({@link DecisionStage#PROTECTED_GUARD}).
   */
  public static RuleSetDefinition customerMode1ForbiddenOverrideAttempt() {
    DecisionBinding illegalOverride =
        new DecisionBinding(
            "customer.emp2.freq.bypass_erg00415",
            SLOT_PERIODO_ORDEM_VALIDA,
            DecisionSource.CUSTOMER,
            CompositionPolicy.REPLACE_EXACT,
            RuleExecutorRef.jsonLogic(parseJson("{\"==\": [1, 1]}")),
            List.of(),
            15,
            true,
            RuleDecision.DENY,
            "ERG-00415",
            List.of("draft.dtini"),
            null);
    return withCustomerBindings(List.of(illegalOverride));
  }

  /**
   * <b>Modo 2 ({@link OverridePolicy#PARAMETERIZABLE} + {@link CompositionPolicy#PARAMETERIZE}):</b>
   * ajusta a janela máxima de lançamento retroativo ({@code maxRetroactiveDays}) para o órgão sem
   * alterar a árvore JSON Logic de {@code ERG-00446}.
   */
  public static DecisionBinding customerMode2ParameterizeBinding(
      int minRetroactiveDays, int maxRetroactiveDays) {
    ObjectNode values = MAPPER.createObjectNode();
    values.put("minRetroactiveDays", minRetroactiveDays);
    values.put("maxRetroactiveDays", maxRetroactiveDays);
    return new DecisionBinding(
        "customer.emp2.freq.parameterize_erg00446",
        SLOT_LIMITE_RETROATIVIDADE_LANCAMENTO,
        DecisionSource.CUSTOMER,
        CompositionPolicy.PARAMETERIZE,
        null,
        List.of(),
        145,
        true,
        null,
        null,
        List.of(),
        new RuleParameterization(
            TEMPLATE_RETROACTIVE_BOUNDS_KEY, TEMPLATE_RETROACTIVE_BOUNDS_VERSION, values));
  }

  /**
   * <b>Modo 3 ({@link OverridePolicy#RESTRICT_ONLY} + {@link CompositionPolicy#AUGMENT}):</b>
   * adiciona a regra específica do órgão {@code empCodigo = 2} ({@code CERG-09101}) exigindo
   * {@code documentoLegalId != null} quando a ocorrência superar {@code 3} dias, preservando os
   * tetos mensais/anuais do produto ({@code ERG-00444}) ativos sob {@code DENY_OVERRIDES}.
   */
  public static DecisionBinding customerMode3AugmentBinding() {
    return new DecisionBinding(
        "customer.emp2.freq.augment_cerg09101",
        SLOT_LIMITES_ACUMULADOS_MES_ANO,
        DecisionSource.CUSTOMER,
        CompositionPolicy.AUGMENT,
        RuleExecutorRef.jsonLogic(
            parseJson(
                """
                {
                  "or": [
                    {"<=": [{"var": "draft.effectiveDurationDays"}, 3]},
                    {"!=": [{"var": "draft.documentoLegalId"}, null]}
                  ]
                }
                """)),
        List.of(),
        125,
        true,
        RuleDecision.DENY,
        "CERG-09101",
        List.of("draft.dtini", "draft.tipofreq", "draft.codfreq"),
        null);
  }

  /**
   * <b>Modo 4 ({@link OverridePolicy#REPLACEABLE} + {@link CompositionPolicy#REPLACE_EXACT}):</b>
   * substitui exclusivamente a política de concomitância cruzada com afastamento ativo
   * ({@link #SLOT_CONCOMITANCIA_AFASTAMENTO_ATIVO} — {@code ERG-00417} -> {@code CERG-00417}) para
   * o órgão {@code empCodigo = 2}, bloqueando concomitância mesmo quando
   * {@code permiteConcomitancia == true}, sem desligar nenhuma das outras 15 decisões do produto.
   */
  public static DecisionBinding customerMode4ReplaceExactBinding() {
    return new DecisionBinding(
        "customer.emp2.freq.replace_erg00417",
        SLOT_CONCOMITANCIA_AFASTAMENTO_ATIVO,
        DecisionSource.CUSTOMER,
        CompositionPolicy.REPLACE_EXACT,
        RuleExecutorRef.jsonLogic(
            parseJson(
                """
                {
                  "==": [{"var": "effectiveStatutoryRule.overlappingAfastamentosCount"}, 0]
                }
                """)),
        List.of(),
        155,
        true,
        RuleDecision.DENY,
        "CERG-00417",
        List.of("draft.dtini", "draft.tipofreq", "draft.codfreq", "effectiveStatutoryRule"),
        null);
  }

  /** Constrói o {@link RuleSetDefinition} customizado do Órgão 2 ({@code empCodigo = 2}). */
  public static RuleSetDefinition orgao2CustomizedRuleSet() {
    return withCustomerBindings(
        List.of(
            customerMode2ParameterizeBinding(0, 15),
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
      throw new IllegalStateException(
          "Invalid static JSON Logic in FrequenciaLancamentoRuleSet", ex);
    }
  }
}
