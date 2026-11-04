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
 * Definição canônica do {@link RuleSetDefinition} para provimento, acumulação constitucional e
 * ciclo de vida de {@code Vinculo}, extraída de {@code PCK_VINCULOS}, {@code PCK_FUNCIONARIOS},
 * {@code EP__VINCULOS_DTIDADE} e {@code EPB__VINCULOS} via protocolo de 4 saltos.
 *
 * <p>Cobre guardas protegidas de cadeia cronológica de provimento/vacância/contrato/cessão, idade
 * mínima constitucional de ingresso, compatibilidade entre regime jurídico/tipo de vínculo/cargo,
 * vigência de cargo e saldo de vagas, limites operacionais parametrizáveis por órgão (carga horária
 * semanal e prazo posse-exercício), vacância sem afastamento em aberto, política de contratos
 * temporários e executor Java de acumulação constitucional de vínculos ativos.
 */
public final class VinculoProvimentoRuleSet {

  public static final String DOMAIN_SCOPE = "workforce.employment-links";
  public static final String RESOURCE_KEY = "administracao-pessoal.vinculos";
  public static final String RULESET_KEY = "ergon.administracao-pessoal.vinculos.provimento";
  public static final String OPERATION = "validate";
  public static final int VERSION = 1;

  public static final String SLOT_CADEIA_CRONOLOGICA = "vinc.cadeia_cronologica_provimento";
  public static final String SLOT_IDADE_MINIMA_INGRESSO =
      "vinc.idade_minima_constitucional_ingresso";
  public static final String SLOT_COMPATIBILIDADE_REGIME_CARGO =
      "vinc.compatibilidade_regime_vinculo_cargo";
  public static final String SLOT_VIGENCIA_CARGO_VAGAS = "vinc.vigencia_cargo_e_saldo_vagas";
  public static final String SLOT_LIMITES_JORNADA_EXERCICIO =
      "vinc.limites_jornada_e_prazo_exercicio";
  public static final String SLOT_VACANCIA_SEM_AFASTAMENTO =
      "vinc.vacancia_sem_afastamento_aberto";
  public static final String SLOT_POLITICA_CONTRATO_TEMPORARIO =
      "vinc.politica_contrato_temporario_cessao";
  public static final String SLOT_ACUMULACAO_CONSTITUCIONAL =
      "vinc.acumulacao_constitucional_cargos";

  public static final String TEMPLATE_PROVIMENTO_BOUNDS_KEY =
      "ergon.vinculos:provimento-operational-bounds";
  public static final String TEMPLATE_PROVIMENTO_BOUNDS_VERSION = "1.0";
  public static final String JAVA_EXECUTOR_ACUMULACAO_KEY =
      "ergon.vinculos:acumulacao-constitucional-cargos";
  public static final String JAVA_EXECUTOR_VERSION = "2.1";

  public static final List<String> AVAILABLE_ROOTS =
      List.of(
          "draft",
          "orgao",
          "pessoa",
          "cargo",
          "cargoComissao",
          "setor",
          "setorExercicio",
          "documentoLegal",
          "existingVinculos",
          "tenant",
          "context");

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static final String ERG00315_TEMPLATE_EXPRESSION =
      """
      {
        "and": [
          {
            "or": [
              {"==": [{"var": "draft.jornadaSemanal"}, null]},
              {
                "and": [
                  {">=": [{"var": "draft.jornadaSemanal"}, 1]},
                  {"<=": [{"var": "draft.jornadaSemanal"}, 168]}
                ]
              }
            ]
          },
          {
            "or": [
              {"==": [{"var": "draft.daysPosseToExercicio"}, null]},
              {"<=": [{"var": "draft.daysPosseToExercicio"}, 30]}
            ]
          }
        ]
      }
      """;

  private VinculoProvimentoRuleSet() {}

  /** Constrói a referência canônica do {@link RuleSetDefinition} de provimento de vínculo. */
  public static RuleSetRef ruleSetRef() {
    return new RuleSetRef(DOMAIN_SCOPE, RESOURCE_KEY, RULESET_KEY, OPERATION, VERSION);
  }

  /**
   * Constrói o {@link RuleParameterTemplate} governado para os limites operacionais de carga
   * horária semanal e prazo posse-exercício ({@code ERG-00315}) sob o Contrato 1.6.
   */
  public static RuleParameterTemplate provimentoOperationalBoundsTemplate() {
    return new RuleParameterTemplate(
        TEMPLATE_PROVIMENTO_BOUNDS_KEY,
        TEMPLATE_PROVIMENTO_BOUNDS_VERSION,
        RuleExecutorRef.jsonLogic(parseJson(ERG00315_TEMPLATE_EXPRESSION)),
        RuleDecision.DENY,
        ErgonRuleErrorCatalog.ERG_00315.code(),
        List.of("draft.empCodigo", "draft.situacao"),
        List.of(
            new RuleParameterDefinition(
                "maxWeeklyHours",
                RuleParameterType.NUMBER,
                true,
                false,
                false,
                "hours",
                MAPPER.getNodeFactory().numberNode(168),
                new BigDecimal("10"),
                new BigDecimal("168"),
                List.of(),
                List.of("/and/0/or/1/and/1/<=/1")),
            new RuleParameterDefinition(
                "maxDaysPosseToExercicio",
                RuleParameterType.NUMBER,
                true,
                false,
                false,
                "days",
                MAPPER.getNodeFactory().numberNode(30),
                BigDecimal.ONE,
                new BigDecimal("90"),
                List.of(),
                List.of("/and/1/or/1/<=/1"))));
  }

  /** Retorna a lista ordenada dos 8 {@link DecisionSlot}s de provimento de vínculo. */
  public static List<DecisionSlot> productSlots() {
    return List.of(
        // Stage 1: PROTECTED_GUARD (OverridePolicy.FORBIDDEN)
        new DecisionSlot(
            SLOT_CADEIA_CRONOLOGICA,
            DecisionStage.PROTECTED_GUARD,
            SlotCardinality.SINGLE,
            OverridePolicy.FORBIDDEN,
            DecisionAggregationPolicy.SINGLE_RESULT,
            null),
        new DecisionSlot(
            SLOT_IDADE_MINIMA_INGRESSO,
            DecisionStage.PROTECTED_GUARD,
            SlotCardinality.SINGLE,
            OverridePolicy.FORBIDDEN,
            DecisionAggregationPolicy.SINGLE_RESULT,
            null),
        new DecisionSlot(
            SLOT_COMPATIBILIDADE_REGIME_CARGO,
            DecisionStage.PROTECTED_GUARD,
            SlotCardinality.SINGLE,
            OverridePolicy.FORBIDDEN,
            DecisionAggregationPolicy.SINGLE_RESULT,
            null),
        new DecisionSlot(
            SLOT_VIGENCIA_CARGO_VAGAS,
            DecisionStage.PROTECTED_GUARD,
            SlotCardinality.SINGLE,
            OverridePolicy.FORBIDDEN,
            DecisionAggregationPolicy.SINGLE_RESULT,
            null),

        // Stage 2: DOMAIN_DECISION
        new DecisionSlot(
            SLOT_LIMITES_JORNADA_EXERCICIO,
            DecisionStage.DOMAIN_DECISION,
            SlotCardinality.SINGLE,
            OverridePolicy.PARAMETERIZABLE,
            DecisionAggregationPolicy.SINGLE_RESULT,
            provimentoOperationalBoundsTemplate()),
        new DecisionSlot(
            SLOT_VACANCIA_SEM_AFASTAMENTO,
            DecisionStage.DOMAIN_DECISION,
            SlotCardinality.MULTIPLE,
            OverridePolicy.RESTRICT_ONLY,
            DecisionAggregationPolicy.DENY_OVERRIDES,
            null),
        new DecisionSlot(
            SLOT_POLITICA_CONTRATO_TEMPORARIO,
            DecisionStage.DOMAIN_DECISION,
            SlotCardinality.SINGLE,
            OverridePolicy.REPLACEABLE,
            DecisionAggregationPolicy.SINGLE_RESULT,
            null),

        // Stage 3: POST_DECISION (OverridePolicy.FORBIDDEN)
        new DecisionSlot(
            SLOT_ACUMULACAO_CONSTITUCIONAL,
            DecisionStage.POST_DECISION,
            SlotCardinality.SINGLE,
            OverridePolicy.FORBIDDEN,
            DecisionAggregationPolicy.SINGLE_RESULT,
            null));
  }

  /** Retorna os 8 {@link DecisionBinding}s padrão do produto Ergon ({@link DecisionSource#PRODUCT}). */
  public static List<DecisionBinding> productBindings() {
    List<String> baseRequiredFacts = List.of("draft.empCodigo", "draft.situacao");
    return List.of(
        // 1. ERG-00324: cadeia cronológica de provimento, vacância, contrato e cessão
        productJsonLogicBinding(
            "product.ergon.vinc.erg00324",
            SLOT_CADEIA_CRONOLOGICA,
            10,
            """
            {
              "and": [
                {
                  "or": [
                    {"==": [{"var": "draft.dtnom"}, null]},
                    {"==": [{"var": "draft.dtposse"}, null]},
                    {"<=": [{"var": "draft.dtnom"}, {"var": "draft.dtposse"}]}
                  ]
                },
                {
                  "or": [
                    {"==": [{"var": "draft.dtnom"}, null]},
                    {"==": [{"var": "draft.dtexerc"}, null]},
                    {"<=": [{"var": "draft.dtnom"}, {"var": "draft.dtexerc"}]}
                  ]
                },
                {
                  "or": [
                    {"==": [{"var": "draft.dtposse"}, null]},
                    {"==": [{"var": "draft.dtexerc"}, null]},
                    {"<=": [{"var": "draft.dtposse"}, {"var": "draft.dtexerc"}]}
                  ]
                },
                {
                  "or": [
                    {"==": [{"var": "draft.dtexerc"}, null]},
                    {"==": [{"var": "draft.dtvac"}, null]},
                    {"<=": [{"var": "draft.dtexerc"}, {"var": "draft.dtvac"}]}
                  ]
                },
                {
                  "or": [
                    {"==": [{"var": "draft.dtexerc"}, null]},
                    {"==": [{"var": "draft.dtaposent"}, null]},
                    {"<=": [{"var": "draft.dtexerc"}, {"var": "draft.dtaposent"}]}
                  ]
                },
                {
                  "or": [
                    {"==": [{"var": "draft.dtPgtoAte"}, null]},
                    {
                      "and": [
                        {"!=": [{"var": "draft.dtvac"}, null]},
                        {"<=": [{"var": "draft.dtvac"}, {"var": "draft.dtPgtoAte"}]}
                      ]
                    }
                  ]
                },
                {
                  "or": [
                    {"==": [{"var": "draft.dtinicontr"}, null]},
                    {"==": [{"var": "draft.dtfimcontr"}, null]},
                    {"<=": [{"var": "draft.dtinicontr"}, {"var": "draft.dtfimcontr"}]}
                  ]
                },
                {
                  "or": [
                    {"==": [{"var": "draft.dtinicontr"}, null]},
                    {"==": [{"var": "draft.dtprorrogcontr"}, null]},
                    {"<=": [{"var": "draft.dtinicontr"}, {"var": "draft.dtprorrogcontr"}]}
                  ]
                },
                {
                  "or": [
                    {"==": [{"var": "draft.dtfimcontr"}, null]},
                    {"==": [{"var": "draft.dtprorrogcontr"}, null]},
                    {"<=": [{"var": "draft.dtfimcontr"}, {"var": "draft.dtprorrogcontr"}]}
                  ]
                },
                {
                  "or": [
                    {"==": [{"var": "draft.dtiniCessao"}, null]},
                    {"==": [{"var": "draft.dtfimCessao"}, null]},
                    {"<=": [{"var": "draft.dtiniCessao"}, {"var": "draft.dtfimCessao"}]}
                  ]
                },
                {
                  "or": [
                    {"==": [{"var": "draft.dtexerc"}, null]},
                    {"==": [{"var": "draft.dtiniCessao"}, null]},
                    {"<=": [{"var": "draft.dtexerc"}, {"var": "draft.dtiniCessao"}]}
                  ]
                },
                {
                  "or": [
                    {"==": [{"var": "draft.dtexerc"}, null]},
                    {"==": [{"var": "draft.dtHomologEstagio"}, null]},
                    {"<=": [{"var": "draft.dtexerc"}, {"var": "draft.dtHomologEstagio"}]}
                  ]
                },
                {
                  "or": [
                    {"==": [{"var": "draft.dtinihomologestag"}, null]},
                    {"==": [{"var": "draft.dtfimhomologestag"}, null]},
                    {"<=": [{"var": "draft.dtinihomologestag"}, {"var": "draft.dtfimhomologestag"}]}
                  ]
                },
                {
                  "or": [
                    {"==": [{"var": "draft.dtinipagto"}, null]},
                    {"==": [{"var": "draft.dtfimpagto"}, null]},
                    {"<=": [{"var": "draft.dtinipagto"}, {"var": "draft.dtfimpagto"}]}
                  ]
                }
              ]
            }
            """,
            ErgonRuleErrorCatalog.ERG_00324.code(),
            baseRequiredFacts),

        // 2. ERG-00325: idade mínima constitucional de ingresso (>= 18 anos)
        productJsonLogicBinding(
            "product.ergon.vinc.erg00325",
            SLOT_IDADE_MINIMA_INGRESSO,
            20,
            """
            {
              "and": [
                {"==": [{"var": "pessoa.present"}, true]},
                {
                  "or": [
                    {"==": [{"var": "draft.ageAtIngressoYears"}, null]},
                    {">=": [{"var": "draft.ageAtIngressoYears"}, 18]}
                  ]
                }
              ]
            }
            """,
            ErgonRuleErrorCatalog.ERG_00325.code(),
            baseRequiredFacts),

        // 3. ERG-00354: isolamento de órgão, órgão ativo na admissão e compatibilidade regime/vínculo
        productJsonLogicBinding(
            "product.ergon.vinc.erg00354",
            SLOT_COMPATIBILIDADE_REGIME_CARGO,
            30,
            """
            {
              "and": [
                {"==": [{"var": "orgao.present"}, true]},
                {
                  "or": [
                    {"!=": [{"var": "draft.creating"}, true]},
                    {"==": [{"var": "orgao.ativa"}, true]}
                  ]
                },
                {
                  "or": [
                    {"==": [{"var": "draft.setor"}, null]},
                    {
                      "and": [
                        {"==": [{"var": "setor.present"}, true]},
                        {"==": [{"var": "setor.sameOrgao"}, true]}
                      ]
                    }
                  ]
                },
                {
                  "or": [
                    {"==": [{"var": "draft.setorExercicio"}, null]},
                    {
                      "and": [
                        {"==": [{"var": "setorExercicio.present"}, true]},
                        {"==": [{"var": "setorExercicio.sameOrgao"}, true]}
                      ]
                    }
                  ]
                },
                {
                  "or": [
                    {"==": [{"var": "draft.cargo"}, null]},
                    {
                      "and": [
                        {"==": [{"var": "cargo.present"}, true]},
                        {"==": [{"var": "cargo.sameOrgao"}, true]},
                        {
                          "or": [
                            {"==": [{"var": "draft.tipovinc"}, null]},
                            {"==": [{"var": "cargo.tipoCargo"}, null]},
                            {
                              "!": [
                                {
                                  "or": [
                                    {
                                      "and": [
                                        {"==": [{"var": "draft.tipovinc"}, "EFETIVO"]},
                                        {"==": [{"var": "cargo.tipoCargo"}, "COMISSAO"]}
                                      ]
                                    },
                                    {
                                      "and": [
                                        {"==": [{"var": "draft.tipovinc"}, "COMISSIONADO"]},
                                        {"==": [{"var": "cargo.tipoCargo"}, "EFETIVO"]}
                                      ]
                                    }
                                  ]
                                }
                              ]
                            }
                          ]
                        }
                      ]
                    }
                  ]
                },
                {
                  "or": [
                    {"==": [{"var": "draft.cargoComissao"}, null]},
                    {
                      "and": [
                        {"==": [{"var": "cargoComissao.present"}, true]},
                        {"==": [{"var": "cargoComissao.sameOrgao"}, true]},
                        {
                          "or": [
                            {"==": [{"var": "cargoComissao.tipoCargo"}, null]},
                            {"!=": [{"var": "cargoComissao.tipoCargo"}, "EFETIVO"]}
                          ]
                        }
                      ]
                    }
                  ]
                },
                {
                  "!": [
                    {
                      "and": [
                        {"==": [{"var": "draft.regimejur"}, "CLT"]},
                        {"==": [{"var": "draft.tipovinc"}, "EFETIVO"]}
                      ]
                    }
                  ]
                },
                {
                  "!": [
                    {
                      "and": [
                        {"==": [{"var": "draft.tipovinc"}, "TEMPORARIO"]},
                        {"!=": [{"var": "draft.dtHomologEstagio"}, null]}
                      ]
                    }
                  ]
                }
              ]
            }
            """,
            ErgonRuleErrorCatalog.ERG_00354.code(),
            baseRequiredFacts),

        // 4. ERG-00359: vigência de cargo e saldo de vagas disponíveis
        productJsonLogicBinding(
            "product.ergon.vinc.erg00359",
            SLOT_VIGENCIA_CARGO_VAGAS,
            40,
            """
            {
              "or": [
                {"!=": [{"var": "draft.occupyingPosition"}, true]},
                {
                  "and": [
                    {
                      "or": [
                        {"==": [{"var": "draft.cargo"}, null]},
                        {
                          "and": [
                            {"!=": [{"var": "cargo.extinct"}, true]},
                            {
                              "or": [
                                {"!=": [{"var": "cargo.controleVaga"}, true]},
                                {
                                  "and": [
                                    {"!=": [{"var": "cargo.vagasAutorizadas"}, null]},
                                    {"<": [{"var": "cargo.ocupadas"}, {"var": "cargo.vagasAutorizadas"}]}
                                  ]
                                }
                              ]
                            }
                          ]
                        }
                      ]
                    },
                    {
                      "or": [
                        {"==": [{"var": "draft.cargoComissao"}, null]},
                        {
                          "and": [
                            {"!=": [{"var": "cargoComissao.extinct"}, true]},
                            {
                              "or": [
                                {"!=": [{"var": "cargoComissao.controleVaga"}, true]},
                                {
                                  "and": [
                                    {"!=": [{"var": "cargoComissao.vagasAutorizadas"}, null]},
                                    {"<": [{"var": "cargoComissao.ocupadas"}, {"var": "cargoComissao.vagasAutorizadas"}]}
                                  ]
                                }
                              ]
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }
              ]
            }
            """,
            ErgonRuleErrorCatalog.ERG_00359.code(),
            baseRequiredFacts),

        // 5. ERG-00315: limites operacionais parametrizáveis por órgão (jornada e prazo posse-exercício)
        productJsonLogicBinding(
            "product.ergon.vinc.erg00315",
            SLOT_LIMITES_JORNADA_EXERCICIO,
            50,
            ERG00315_TEMPLATE_EXPRESSION,
            ErgonRuleErrorCatalog.ERG_00315.code(),
            baseRequiredFacts),

        // 6. ERG-00342: vacância sem afastamento aberto (RESTRICT_ONLY)
        productJsonLogicBinding(
            "product.ergon.vinc.erg00342",
            SLOT_VACANCIA_SEM_AFASTAMENTO,
            60,
            """
            {
              "and": [
                {
                  "or": [
                    {"==": [{"var": "draft.dtvac"}, null]},
                    {"==": [{"var": "draft.situacao"}, "APOSENTADO"]},
                    {
                      "and": [
                        {"!=": [{"var": "draft.formavac"}, null]},
                        {"!=": [{"var": "draft.formavac"}, ""]}
                      ]
                    }
                  ]
                },
                {
                  "or": [
                    {"==": [{"var": "draft.dtvac"}, null]},
                    {"==": [{"var": "draft.openAfastamentosCount"}, 0]}
                  ]
                },
                {
                  "or": [
                    {"==": [{"var": "draft.tipoapos"}, null]},
                    {"!=": [{"var": "draft.dtaposent"}, null]}
                  ]
                }
              ]
            }
            """,
            ErgonRuleErrorCatalog.ERG_00342.code(),
            baseRequiredFacts),

        // 7. ERG-00330: política de contratos temporários e cessão (REPLACEABLE)
        productJsonLogicBinding(
            "product.ergon.vinc.erg00330",
            SLOT_POLITICA_CONTRATO_TEMPORARIO,
            70,
            """
            {
              "and": [
                {
                  "or": [
                    {"!=": [{"var": "draft.tipovinc"}, "TEMPORARIO"]},
                    {
                      "and": [
                        {"!=": [{"var": "draft.dtinicontr"}, null]},
                        {"!=": [{"var": "draft.dtfimcontr"}, null]}
                      ]
                    }
                  ]
                },
                {
                  "or": [
                    {"==": [{"var": "draft.tipoReq"}, null]},
                    {
                      "and": [
                        {"!=": [{"var": "draft.orgaoReq"}, null]},
                        {"!=": [{"var": "draft.orgaoReq"}, ""]}
                      ]
                    }
                  ]
                }
              ]
            }
            """,
            ErgonRuleErrorCatalog.ERG_00330.code(),
            baseRequiredFacts),

        // 8. ERG-00300: acumulação constitucional de vínculos ativos (POST_DECISION Java)
        new DecisionBinding(
            "product.ergon.vinc.erg00300",
            SLOT_ACUMULACAO_CONSTITUCIONAL,
            DecisionSource.PRODUCT,
            null,
            RuleExecutorRef.java(JAVA_EXECUTOR_ACUMULACAO_KEY, JAVA_EXECUTOR_VERSION),
            List.of(),
            80,
            true,
            null,
            null,
            List.of("draft.situacao", "existingVinculos"),
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
   * Compõe o {@link RuleSetDefinition} do produto com bindings de customização de órgão ({@link
   * DecisionSource#CUSTOMER}).
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
   * cliente ao slot protegido {@code ERG-00324} ({@link DecisionStage#PROTECTED_GUARD}).
   */
  public static RuleSetDefinition customerMode1ForbiddenOverrideAttempt() {
    DecisionBinding illegalOverride =
        new DecisionBinding(
            "customer.emp2.vinc.bypass_erg00324",
            SLOT_CADEIA_CRONOLOGICA,
            DecisionSource.CUSTOMER,
            CompositionPolicy.REPLACE_EXACT,
            RuleExecutorRef.jsonLogic(parseJson("{\"==\": [1, 1]}")),
            List.of(),
            15,
            true,
            RuleDecision.DENY,
            ErgonRuleErrorCatalog.ERG_00324.code(),
            List.of("draft.empCodigo"),
            null);
    return withCustomerBindings(List.of(illegalOverride));
  }

  /**
   * <b>Modo 2 ({@link OverridePolicy#PARAMETERIZABLE} + {@link CompositionPolicy#PARAMETERIZE}):</b>
   * ajusta a carga horária semanal máxima ({@code maxWeeklyHours}) e o prazo posse-exercício
   * ({@code maxDaysPosseToExercicio}) para o órgão sem alterar a árvore JSON Logic de {@code
   * ERG-00315}.
   */
  public static DecisionBinding customerMode2ParameterizeBinding(
      int maxWeeklyHours, int maxDaysPosseToExercicio) {
    ObjectNode values = MAPPER.createObjectNode();
    values.put("maxWeeklyHours", maxWeeklyHours);
    values.put("maxDaysPosseToExercicio", maxDaysPosseToExercicio);
    return new DecisionBinding(
        "customer.emp2.vinc.parameterize_erg00315",
        SLOT_LIMITES_JORNADA_EXERCICIO,
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
            TEMPLATE_PROVIMENTO_BOUNDS_KEY, TEMPLATE_PROVIMENTO_BOUNDS_VERSION, values));
  }

  /**
   * <b>Modo 3 ({@link OverridePolicy#RESTRICT_ONLY} + {@link CompositionPolicy#AUGMENT}):</b>
   * adiciona a regra específica do Órgão 2 ({@code CERG-09401}) exigindo {@code documentoLegalId !=
   * null} sempre que houver designação para cargo em comissão ou registro de vacância, preservando
   * {@code ERG-00342} sob {@code DENY_OVERRIDES}.
   */
  public static DecisionBinding customerMode3AugmentBinding() {
    return new DecisionBinding(
        "customer.emp2.vinc.augment_cerg09401",
        SLOT_VACANCIA_SEM_AFASTAMENTO,
        DecisionSource.CUSTOMER,
        CompositionPolicy.AUGMENT,
        RuleExecutorRef.jsonLogic(
            parseJson(
                """
                {
                  "or": [
                    {
                      "and": [
                        {"==": [{"var": "draft.cargoComissao"}, null]},
                        {"==": [{"var": "draft.dtvac"}, null]}
                      ]
                    },
                    {
                      "and": [
                        {"!=": [{"var": "draft.documentoLegalId"}, null]},
                        {"==": [{"var": "documentoLegal.present"}, true]},
                        {"==": [{"var": "documentoLegal.sameOrgao"}, true]}
                      ]
                    }
                  ]
                }
                """)),
        List.of(),
        65,
        true,
        RuleDecision.DENY,
        ErgonRuleErrorCatalog.CERG_09401.code(),
        List.of("draft.empCodigo", "draft.situacao", "documentoLegal"),
        null);
  }

  /**
   * <b>Modo 4 ({@link OverridePolicy#REPLACEABLE} + {@link CompositionPolicy#REPLACE_EXACT}):</b>
   * substitui exclusivamente a política de contratos temporários ({@link
   * #SLOT_POLITICA_CONTRATO_TEMPORARIO} — {@code ERG-00330} -> {@code CERG-00330}) para o Órgão 2,
   * exigindo {@code documentoLegalId != null} e duração contratual máxima de {@code 730} dias.
   */
  public static DecisionBinding customerMode4ReplaceExactBinding() {
    return new DecisionBinding(
        "customer.emp2.vinc.replace_erg00330",
        SLOT_POLITICA_CONTRATO_TEMPORARIO,
        DecisionSource.CUSTOMER,
        CompositionPolicy.REPLACE_EXACT,
        RuleExecutorRef.jsonLogic(
            parseJson(
                """
                {
                  "or": [
                    {"!=": [{"var": "draft.tipovinc"}, "TEMPORARIO"]},
                    {
                      "and": [
                        {"!=": [{"var": "draft.dtinicontr"}, null]},
                        {"!=": [{"var": "draft.dtfimcontr"}, null]},
                        {"!=": [{"var": "draft.documentoLegalId"}, null]},
                        {"==": [{"var": "documentoLegal.present"}, true]},
                        {"==": [{"var": "documentoLegal.sameOrgao"}, true]},
                        {"!=": [{"var": "draft.contractDurationDays"}, null]},
                        {"<=": [{"var": "draft.contractDurationDays"}, 730]}
                      ]
                    }
                  ]
                }
                """)),
        List.of(),
        75,
        true,
        RuleDecision.DENY,
        ErgonRuleErrorCatalog.CERG_00330.code(),
        List.of("draft.empCodigo", "draft.situacao", "documentoLegal"),
        null);
  }

  /** Constrói o {@link RuleSetDefinition} customizado do Órgão 2 ({@code empCodigo = 2}). */
  public static RuleSetDefinition orgao2CustomizedRuleSet() {
    return withCustomerBindings(
        List.of(
            customerMode2ParameterizeBinding(44, 15),
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
          "Invalid static JSON Logic in VinculoProvimentoRuleSet", ex);
    }
  }
}
