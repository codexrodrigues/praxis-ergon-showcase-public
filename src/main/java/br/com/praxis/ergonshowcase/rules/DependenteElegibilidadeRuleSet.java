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
 * Definição canônica do {@link RuleSetDefinition} para elegibilidade cadastral e de benefícios de
 * {@code Dependente}, extraída de {@code PCK_DEPENDENTES}, {@code PCK_DEPENDENCIAS} e {@code
 * EPB__DEPENDENTES} via protocolo de 4 saltos.
 *
 * <p>Cobre guardas protegidas de vigência e genealogia, obrigatoriedade de CPF para IRRF,
 * compatibilidade com {@code TipoDependente}, tetos etários parametrizáveis por órgão (IRRF,
 * Salário-Família e Plano de Saúde) com isenção por invalidez/excepcionalidade, exigência de
 * documento legal para guarda/tutela/invalidez, política de plano de saúde e unicidade de CPF de
 * dependente no servidor.
 */
public final class DependenteElegibilidadeRuleSet {

  public static final String DOMAIN_SCOPE = "workforce.dependents-and-benefits";
  public static final String RESOURCE_KEY = "administracao-pessoal.dependentes";
  public static final String RULESET_KEY = "ergon.administracao-pessoal.dependentes.elegibilidade";
  public static final String OPERATION = "validate";
  public static final int VERSION = 1;

  public static final String SLOT_PERIODO_VIGENCIA = "dep.periodo_vigencia_valido";
  public static final String SLOT_DATA_NASCIMENTO = "dep.data_nascimento_valida";
  public static final String SLOT_COERENCIA_PARENTESCO = "dep.coerencia_cronologica_parentesco";
  public static final String SLOT_CPF_OBRIGATORIO_IRRF = "dep.cpf_obrigatorio_irrf";
  public static final String SLOT_COMPATIBILIDADE_TIPO = "dep.compatibilidade_tipo_dependente";
  public static final String SLOT_ELEGIBILIDADE_ETARIA = "dep.elegibilidade_etaria_beneficios";
  public static final String SLOT_ENCERRAMENTO_SEM_BENEFICIOS =
      "dep.encerramento_sem_beneficios_ativos";
  public static final String SLOT_EXIGENCIA_DOCUMENTO = "dep.exigencia_documento_invalidez_guarda";
  public static final String SLOT_POLITICA_PLANO_SAUDE = "dep.politica_conjuge_plano_saude";
  public static final String SLOT_UNICIDADE_CPF = "dep.unicidade_cpf_dependente_servidor";

  public static final String TEMPLATE_BENEFIT_AGE_CEILINGS_KEY =
      "ergon.dependentes:benefit-age-ceilings";
  public static final String TEMPLATE_BENEFIT_AGE_CEILINGS_VERSION = "1.0";
  public static final String JAVA_EXECUTOR_CPF_UNICIDADE_KEY =
      "ergon.dependentes:cpf-unicidade-servidor";
  public static final String JAVA_EXECUTOR_VERSION = "1.0";

  public static final List<String> AVAILABLE_ROOTS =
      List.of(
          "draft",
          "pessoa",
          "tipoDependente",
          "documentoLegal",
          "existingDependentes",
          "tenant",
          "context");

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static final String ERG03177_TEMPLATE_EXPRESSION =
      """
      {
        "and": [
          {
            "or": [
              {"!=": [{"var": "draft.depIr"}, true]},
              {"==": [{"var": "draft.invalido"}, true]},
              {"==": [{"var": "draft.excepcional"}, true]},
              {"in": [{"var": "draft.parentesco"}, ["CONJUGE", "COMPANHEIRO", "PAI", "MAE", "GENITOR", "AVO"]]},
              {"<=": [{"var": "draft.ageYears"}, 21]},
              {
                "and": [
                  {"==": [{"var": "draft.universitario"}, true]},
                  {"<=": [{"var": "draft.ageYears"}, 24]}
                ]
              }
            ]
          },
          {
            "or": [
              {"!=": [{"var": "draft.depSalarioFamilia"}, true]},
              {"==": [{"var": "draft.invalido"}, true]},
              {"==": [{"var": "draft.excepcional"}, true]},
              {"<=": [{"var": "draft.ageYears"}, 14]}
            ]
          },
          {
            "or": [
              {"!=": [{"var": "draft.depPlanoSaude"}, true]},
              {"==": [{"var": "draft.invalido"}, true]},
              {"==": [{"var": "draft.excepcional"}, true]},
              {"in": [{"var": "draft.parentesco"}, ["CONJUGE", "COMPANHEIRO", "PAI", "MAE", "GENITOR", "AVO"]]},
              {"<=": [{"var": "draft.ageYears"}, 24]}
            ]
          },
          {
            "or": [
              {"!=": [{"var": "draft.tipodepen"}, "PENSAO"]},
              {"==": [{"var": "draft.invalido"}, true]},
              {"==": [{"var": "draft.excepcional"}, true]},
              {"in": [{"var": "draft.parentesco"}, ["CONJUGE", "COMPANHEIRO", "PAI", "MAE", "GENITOR", "AVO"]]},
              {"<=": [{"var": "draft.ageYears"}, 21]}
            ]
          }
        ]
      }
      """;

  private DependenteElegibilidadeRuleSet() {}

  /** Constrói a referência canônica do {@link RuleSetDefinition} de elegibilidade de dependente. */
  public static RuleSetRef ruleSetRef() {
    return new RuleSetRef(DOMAIN_SCOPE, RESOURCE_KEY, RULESET_KEY, OPERATION, VERSION);
  }

  /**
   * Constrói o {@link RuleParameterTemplate} governado para os tetos etários de benefícios de
   * dependente ({@code ERG-03177}) sob o Contrato 1.6.
   */
  public static RuleParameterTemplate benefitAgeCeilingsTemplate() {
    return new RuleParameterTemplate(
        TEMPLATE_BENEFIT_AGE_CEILINGS_KEY,
        TEMPLATE_BENEFIT_AGE_CEILINGS_VERSION,
        RuleExecutorRef.jsonLogic(parseJson(ERG03177_TEMPLATE_EXPRESSION)),
        RuleDecision.DENY,
        ErgonRuleErrorCatalog.ERG_03177.code(),
        List.of("draft.dtnasc", "draft.parentesco"),
        List.of(
            new RuleParameterDefinition(
                "maxAgeIrrfStandard",
                RuleParameterType.NUMBER,
                true,
                false,
                false,
                "years",
                MAPPER.getNodeFactory().numberNode(21),
                new BigDecimal("14"),
                new BigDecimal("30"),
                List.of(),
                List.of("/and/0/or/4/<=/1")),
            new RuleParameterDefinition(
                "maxAgeIrrfUniversity",
                RuleParameterType.NUMBER,
                true,
                false,
                false,
                "years",
                MAPPER.getNodeFactory().numberNode(24),
                new BigDecimal("18"),
                new BigDecimal("35"),
                List.of(),
                List.of("/and/0/or/5/and/1/<=/1")),
            new RuleParameterDefinition(
                "maxAgeFamilyAllowance",
                RuleParameterType.NUMBER,
                true,
                false,
                false,
                "years",
                MAPPER.getNodeFactory().numberNode(14),
                new BigDecimal("10"),
                new BigDecimal("21"),
                List.of(),
                List.of("/and/1/or/3/<=/1")),
            new RuleParameterDefinition(
                "maxAgeHealthPlan",
                RuleParameterType.NUMBER,
                true,
                false,
                false,
                "years",
                MAPPER.getNodeFactory().numberNode(24),
                new BigDecimal("18"),
                new BigDecimal("35"),
                List.of(),
                List.of("/and/2/or/4/<=/1")),
            new RuleParameterDefinition(
                "maxAgePension",
                RuleParameterType.NUMBER,
                true,
                false,
                false,
                "years",
                MAPPER.getNodeFactory().numberNode(21),
                new BigDecimal("14"),
                new BigDecimal("30"),
                List.of(),
                List.of("/and/3/or/4/<=/1"))));
  }

  /** Retorna a lista ordenada dos 10 {@link DecisionSlot}s de elegibilidade de dependente. */
  public static List<DecisionSlot> productSlots() {
    return List.of(
        // Stage 1: PROTECTED_GUARD (OverridePolicy.FORBIDDEN)
        new DecisionSlot(
            SLOT_PERIODO_VIGENCIA,
            DecisionStage.PROTECTED_GUARD,
            SlotCardinality.SINGLE,
            OverridePolicy.FORBIDDEN,
            DecisionAggregationPolicy.SINGLE_RESULT,
            null),
        new DecisionSlot(
            SLOT_DATA_NASCIMENTO,
            DecisionStage.PROTECTED_GUARD,
            SlotCardinality.MULTIPLE,
            OverridePolicy.FORBIDDEN,
            DecisionAggregationPolicy.DENY_OVERRIDES,
            null),
        new DecisionSlot(
            SLOT_COERENCIA_PARENTESCO,
            DecisionStage.PROTECTED_GUARD,
            SlotCardinality.MULTIPLE,
            OverridePolicy.FORBIDDEN,
            DecisionAggregationPolicy.DENY_OVERRIDES,
            null),

        // Stage 2: DOMAIN_DECISION
        new DecisionSlot(
            SLOT_CPF_OBRIGATORIO_IRRF,
            DecisionStage.DOMAIN_DECISION,
            SlotCardinality.SINGLE,
            OverridePolicy.FORBIDDEN,
            DecisionAggregationPolicy.SINGLE_RESULT,
            null),
        new DecisionSlot(
            SLOT_COMPATIBILIDADE_TIPO,
            DecisionStage.DOMAIN_DECISION,
            SlotCardinality.SINGLE,
            OverridePolicy.FORBIDDEN,
            DecisionAggregationPolicy.SINGLE_RESULT,
            null),
        new DecisionSlot(
            SLOT_ELEGIBILIDADE_ETARIA,
            DecisionStage.DOMAIN_DECISION,
            SlotCardinality.SINGLE,
            OverridePolicy.PARAMETERIZABLE,
            DecisionAggregationPolicy.SINGLE_RESULT,
            benefitAgeCeilingsTemplate()),
        new DecisionSlot(
            SLOT_ENCERRAMENTO_SEM_BENEFICIOS,
            DecisionStage.DOMAIN_DECISION,
            SlotCardinality.SINGLE,
            OverridePolicy.FORBIDDEN,
            DecisionAggregationPolicy.SINGLE_RESULT,
            null),
        new DecisionSlot(
            SLOT_EXIGENCIA_DOCUMENTO,
            DecisionStage.DOMAIN_DECISION,
            SlotCardinality.MULTIPLE,
            OverridePolicy.RESTRICT_ONLY,
            DecisionAggregationPolicy.DENY_OVERRIDES,
            null),
        new DecisionSlot(
            SLOT_POLITICA_PLANO_SAUDE,
            DecisionStage.DOMAIN_DECISION,
            SlotCardinality.SINGLE,
            OverridePolicy.REPLACEABLE,
            DecisionAggregationPolicy.SINGLE_RESULT,
            null),

        // Stage 3: POST_DECISION (OverridePolicy.FORBIDDEN)
        new DecisionSlot(
            SLOT_UNICIDADE_CPF,
            DecisionStage.POST_DECISION,
            SlotCardinality.SINGLE,
            OverridePolicy.FORBIDDEN,
            DecisionAggregationPolicy.SINGLE_RESULT,
            null));
  }

  /** Retorna os 10 {@link DecisionBinding}s padrão do produto Ergon ({@link DecisionSource#PRODUCT}). */
  public static List<DecisionBinding> productBindings() {
    List<String> baseRequiredFacts = List.of("draft.dtnasc", "draft.dtini", "draft.parentesco");
    return List.of(
        // 1. ERG-03162: vigência válida (dtini >= dtnasc, dtfim >= dtini, expedrg >= dtnasc)
        productJsonLogicBinding(
            "product.ergon.dep.erg03162",
            SLOT_PERIODO_VIGENCIA,
            10,
            """
            {
              "and": [
                {"!=": [{"var": "draft.dtini"}, null]},
                {"!=": [{"var": "draft.dtnasc"}, null]},
                {">=": [{"var": "draft.dtini"}, {"var": "draft.dtnasc"}]},
                {
                  "or": [
                    {"==": [{"var": "draft.dtfim"}, null]},
                    {">=": [{"var": "draft.dtfim"}, {"var": "draft.dtini"}]}
                  ]
                },
                {
                  "or": [
                    {"==": [{"var": "draft.expedrg"}, null]},
                    {">=": [{"var": "draft.expedrg"}, {"var": "draft.dtnasc"}]}
                  ]
                }
              ]
            }
            """,
            ErgonRuleErrorCatalog.ERG_03162.code(),
            baseRequiredFacts),

        // 2. ERG-03182: data de nascimento não futura
        productJsonLogicBinding(
            "product.ergon.dep.erg03182",
            SLOT_DATA_NASCIMENTO,
            20,
            """
            {
              "and": [
                {"!=": [{"var": "draft.dtnasc"}, null]},
                {"<=": [{"var": "draft.dtnasc"}, {"var": "draft.referenceDate"}]}
              ]
            }
            """,
            ErgonRuleErrorCatalog.ERG_03182.code(),
            baseRequiredFacts),

        // 2b. ERG-03181: idade máxima do dependente <= 120 anos
        productJsonLogicBinding(
            "product.ergon.dep.erg03181",
            SLOT_DATA_NASCIMENTO,
            21,
            """
            {
              "and": [
                {"!=": [{"var": "draft.ageYears"}, null]},
                {"<=": [{"var": "draft.ageYears"}, 120]}
              ]
            }
            """,
            ErgonRuleErrorCatalog.ERG_03181.code(),
            baseRequiredFacts),

        // 3. ERG-03183: coerência cronológica do parentesco (filho/enteado/neto nascido após o titular) e CPF distinto do servidor titular
        productJsonLogicBinding(
            "product.ergon.dep.erg03183",
            SLOT_COERENCIA_PARENTESCO,
            30,
            """
            {
              "and": [
                {"==": [{"var": "pessoa.present"}, true]},
                {
                  "or": [
                    {"==": [{"var": "draft.cpf"}, null]},
                    {"==": [{"var": "draft.cpf"}, ""]},
                    {"==": [{"var": "pessoa.cpf"}, null]},
                    {"!=": [{"var": "draft.cpf"}, {"var": "pessoa.cpf"}]}
                  ]
                },
                {
                  "or": [
                    {"==": [{"var": "pessoa.dtnasc"}, null]},
                    {"==": [{"var": "draft.dtnasc"}, null]},
                    {"!": [{"in": [{"var": "draft.parentesco"}, ["FILHO", "FILHA", "FILHO_INV", "ENTEADO", "NETO"]]}]},
                    {">": [{"var": "draft.dtnasc"}, {"var": "pessoa.dtnasc"}]}
                  ]
                }
              ]
            }
            """,
            ErgonRuleErrorCatalog.ERG_03183.code(),
            baseRequiredFacts),

        // 3b. ERG-03184: coerência cronológica do parentesco (pais/mães/genitores/avós nascidos antes do titular)
        productJsonLogicBinding(
            "product.ergon.dep.erg03184",
            SLOT_COERENCIA_PARENTESCO,
            31,
            """
            {
              "and": [
                {"==": [{"var": "pessoa.present"}, true]},
                {
                  "or": [
                    {"==": [{"var": "pessoa.dtnasc"}, null]},
                    {"==": [{"var": "draft.dtnasc"}, null]},
                    {"!": [{"in": [{"var": "draft.parentesco"}, ["PAI", "MAE", "PAI_MAE", "GENITOR", "AVO"]]}]},
                    {"<": [{"var": "draft.dtnasc"}, {"var": "pessoa.dtnasc"}]}
                  ]
                }
              ]
            }
            """,
            ErgonRuleErrorCatalog.ERG_03184.code(),
            baseRequiredFacts),

        // 4. ERG-03185: CPF obrigatório para dedução de IRRF e formato válido de 11 dígitos
        productJsonLogicBinding(
            "product.ergon.dep.erg03185",
            SLOT_CPF_OBRIGATORIO_IRRF,
            40,
            """
            {
              "and": [
                {
                  "or": [
                    {"==": [{"var": "draft.cpf"}, null]},
                    {"==": [{"var": "draft.cpf"}, ""]},
                    {"==": [{"var": "draft.cpfValidFormat"}, true]}
                  ]
                },
                {
                  "or": [
                    {"!=": [{"var": "draft.depIr"}, true]},
                    {
                      "and": [
                        {"!=": [{"var": "draft.cpf"}, null]},
                        {"!=": [{"var": "draft.cpf"}, ""]},
                        {"==": [{"var": "draft.cpfValidFormat"}, true]}
                      ]
                    }
                  ]
                }
              ]
            }
            """,
            ErgonRuleErrorCatalog.ERG_03185.code(),
            baseRequiredFacts),

        // 5. ERG-03171: compatibilidade com TipoDependente
        productJsonLogicBinding(
            "product.ergon.dep.erg03171",
            SLOT_COMPATIBILIDADE_TIPO,
            50,
            """
            {
              "or": [
                {"==": [{"var": "draft.tipodepen"}, null]},
                {
                  "and": [
                    {"==": [{"var": "tipoDependente.present"}, true]},
                    {
                      "or": [
                        {"!=": [{"var": "draft.depIr"}, true]},
                        {"==": [{"var": "tipoDependente.geraIr"}, true]}
                      ]
                    },
                    {
                      "or": [
                        {"!=": [{"var": "draft.depSalarioFamilia"}, true]},
                        {"==": [{"var": "tipoDependente.geraSalarioFamilia"}, true]}
                      ]
                    }
                  ]
                }
              ]
            }
            """,
            ErgonRuleErrorCatalog.ERG_03171.code(),
            baseRequiredFacts),

        // 6. ERG-03177: tetos etários parametrizáveis por órgão (IRRF, Salário-Família, Plano de Saúde)
        productJsonLogicBinding(
            "product.ergon.dep.erg03177",
            SLOT_ELEGIBILIDADE_ETARIA,
            60,
            ERG03177_TEMPLATE_EXPRESSION,
            ErgonRuleErrorCatalog.ERG_03177.code(),
            baseRequiredFacts),

        // 7. ERG-03188: encerramento sem benefícios ativos
        productJsonLogicBinding(
            "product.ergon.dep.erg03188",
            SLOT_ENCERRAMENTO_SEM_BENEFICIOS,
            70,
            """
            {
              "or": [
                {"==": [{"var": "draft.dtfim"}, null]},
                {
                  "and": [
                    {"!=": [{"var": "draft.depIr"}, true]},
                    {"!=": [{"var": "draft.depSalarioFamilia"}, true]},
                    {"!=": [{"var": "draft.depPlanoSaude"}, true]}
                  ]
                }
              ]
            }
            """,
            ErgonRuleErrorCatalog.ERG_03188.code(),
            baseRequiredFacts),

        // 8. ERG-21400: exigência de documento legal (invalidez, guarda/tutela) e completude RG/Certidão
        productJsonLogicBinding(
            "product.ergon.dep.erg21400",
            SLOT_EXIGENCIA_DOCUMENTO,
            80,
            """
            {
              "and": [
                {
                  "or": [
                    {
                      "and": [
                        {"!=": [{"var": "draft.invalido"}, true]},
                        {"!=": [{"var": "draft.excepcional"}, true]},
                        {"!=": [{"var": "draft.representanteLegal"}, true]},
                        {"!": [{"in": [{"var": "draft.parentesco"}, ["MENOR_TUTELADO", "MENOR_SOB_GUARDA", "TUTELADO", "CURATELADO"]]}]}
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
                },
                {
                  "or": [
                    {"!": [{"in": [{"var": "draft.parentesco"}, ["MENOR_TUTELADO", "TUTELADO", "CURATELADO"]]}]},
                    {"==": [{"var": "draft.representanteLegal"}, true]}
                  ]
                },
                {
                  "or": [
                    {"==": [{"var": "draft.documentoLegalId"}, null]},
                    {
                      "and": [
                        {"==": [{"var": "documentoLegal.present"}, true]},
                        {"==": [{"var": "documentoLegal.sameOrgao"}, true]}
                      ]
                    }
                  ]
                },
                {
                  "or": [
                    {
                      "and": [
                        {"==": [{"var": "draft.numrg"}, null]},
                        {"==": [{"var": "draft.tiporg"}, null]},
                        {"==": [{"var": "draft.orgaorg"}, null]},
                        {"==": [{"var": "draft.ufrg"}, null]},
                        {"==": [{"var": "draft.expedrg"}, null]}
                      ]
                    },
                    {
                      "and": [
                        {"!=": [{"var": "draft.numrg"}, null]},
                        {"!=": [{"var": "draft.orgaorg"}, null]},
                        {"!=": [{"var": "draft.ufrg"}, null]}
                      ]
                    }
                  ]
                },
                {
                  "or": [
                    {
                      "and": [
                        {"==": [{"var": "draft.numCert"}, null]},
                        {"==": [{"var": "draft.livroCert"}, null]},
                        {"==": [{"var": "draft.folhaCert"}, null]},
                        {"==": [{"var": "draft.cartorioCert"}, null]}
                      ]
                    },
                    {
                      "and": [
                        {"!=": [{"var": "draft.numCert"}, null]},
                        {"!=": [{"var": "draft.tipodocCert"}, null]},
                        {"!=": [{"var": "draft.cartorioCert"}, null]}
                      ]
                    }
                  ]
                }
              ]
            }
            """,
            ErgonRuleErrorCatalog.ERG_21400.code(),
            baseRequiredFacts),

        // 9. ERG-03198: política de plano de saúde (REPLACEABLE)
        productJsonLogicBinding(
            "product.ergon.dep.erg03198",
            SLOT_POLITICA_PLANO_SAUDE,
            90,
            """
            {
              "or": [
                {"!=": [{"var": "draft.depPlanoSaude"}, true]},
                {"!=": [{"var": "draft.parentesco"}, "AGREGADO"]},
                {
                  "and": [
                    {"!=": [{"var": "draft.documentoLegalId"}, null]},
                    {"==": [{"var": "documentoLegal.present"}, true]},
                    {"==": [{"var": "documentoLegal.sameOrgao"}, true]}
                  ]
                }
              ]
            }
            """,
            ErgonRuleErrorCatalog.ERG_03198.code(),
            baseRequiredFacts),

        // 10. ERG-03193: unicidade de CPF de dependente no mesmo servidor (POST_DECISION Java)
        new DecisionBinding(
            "product.ergon.dep.erg03193",
            SLOT_UNICIDADE_CPF,
            DecisionSource.PRODUCT,
            null,
            RuleExecutorRef.java(JAVA_EXECUTOR_CPF_UNICIDADE_KEY, JAVA_EXECUTOR_VERSION),
            List.of(),
            100,
            true,
            null,
            null,
            List.of("draft.dtnasc", "existingDependentes"),
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
   * cliente ao slot protegido {@code ERG-03162} ({@link DecisionStage#PROTECTED_GUARD}).
   */
  public static RuleSetDefinition customerMode1ForbiddenOverrideAttempt() {
    DecisionBinding illegalOverride =
        new DecisionBinding(
            "customer.emp2.dep.bypass_erg03162",
            SLOT_PERIODO_VIGENCIA,
            DecisionSource.CUSTOMER,
            CompositionPolicy.REPLACE_EXACT,
            RuleExecutorRef.jsonLogic(parseJson("{\"==\": [1, 1]}")),
            List.of(),
            15,
            true,
            RuleDecision.DENY,
            ErgonRuleErrorCatalog.ERG_03162.code(),
            List.of("draft.dtini"),
            null);
    return withCustomerBindings(List.of(illegalOverride));
  }

  /**
   * <b>Modo 2 ({@link OverridePolicy#PARAMETERIZABLE} + {@link CompositionPolicy#PARAMETERIZE}):</b>
   * ajusta os tetos etários de benefícios por órgão sem alterar a árvore JSON Logic de {@code
   * ERG-03177}.
   */
  public static DecisionBinding customerMode2ParameterizeBinding(
      int maxAgeIrrfStandard,
      int maxAgeIrrfUniversity,
      int maxAgeFamilyAllowance,
      int maxAgeHealthPlan) {
    return customerMode2ParameterizeBinding(
        maxAgeIrrfStandard, maxAgeIrrfUniversity, maxAgeIrrfStandard, maxAgeFamilyAllowance, maxAgeHealthPlan);
  }

  /**
   * <b>Modo 2 ({@link OverridePolicy#PARAMETERIZABLE} + {@link CompositionPolicy#PARAMETERIZE}):</b>
   * ajusta todos os 5 tetos etários de benefícios por órgão ({@code maxAgeIrrfStandard}, {@code
   * maxAgeIrrfUniversity}, {@code maxAgePension}, {@code maxAgeFamilyAllowance}, {@code
   * maxAgeHealthPlan}) sem alterar a árvore JSON Logic de {@code ERG-03177}.
   */
  public static DecisionBinding customerMode2ParameterizeBinding(
      int maxAgeIrrfStandard,
      int maxAgeIrrfUniversity,
      int maxAgePension,
      int maxAgeFamilyAllowance,
      int maxAgeHealthPlan) {
    ObjectNode values = MAPPER.createObjectNode();
    values.put("maxAgeIrrfStandard", maxAgeIrrfStandard);
    values.put("maxAgeIrrfUniversity", maxAgeIrrfUniversity);
    values.put("maxAgePension", maxAgePension);
    values.put("maxAgeFamilyAllowance", maxAgeFamilyAllowance);
    values.put("maxAgeHealthPlan", maxAgeHealthPlan);
    return new DecisionBinding(
        "customer.emp2.dep.parameterize_erg03177",
        SLOT_ELEGIBILIDADE_ETARIA,
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
            TEMPLATE_BENEFIT_AGE_CEILINGS_KEY, TEMPLATE_BENEFIT_AGE_CEILINGS_VERSION, values));
  }

  /**
   * <b>Modo 3 ({@link OverridePolicy#RESTRICT_ONLY} + {@link CompositionPolicy#AUGMENT}):</b>
   * adiciona a regra específica do Órgão 2 ({@code CERG-09301}) exigindo {@code documentoLegalId !=
   * null} sempre que {@code universitario == true}, preservando a regra base {@code ERG-21400} sob
   * {@code DENY_OVERRIDES}.
   */
  public static DecisionBinding customerMode3AugmentBinding() {
    return new DecisionBinding(
        "customer.emp2.dep.augment_cerg09301",
        SLOT_EXIGENCIA_DOCUMENTO,
        DecisionSource.CUSTOMER,
        CompositionPolicy.AUGMENT,
        RuleExecutorRef.jsonLogic(
            parseJson(
                """
                {
                  "or": [
                    {"!=": [{"var": "draft.universitario"}, true]},
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
        85,
        true,
        RuleDecision.DENY,
        ErgonRuleErrorCatalog.CERG_09301.code(),
        List.of("draft.dtnasc", "draft.dtini", "documentoLegal"),
        null);
  }

  /**
   * <b>Modo 4 ({@link OverridePolicy#REPLACEABLE} + {@link CompositionPolicy#REPLACE_EXACT}):</b>
   * substitui exclusivamente a política de plano de saúde ({@link #SLOT_POLITICA_PLANO_SAUDE} —
   * {@code ERG-03198} -> {@code CERG-03198}) para o Órgão 2, exigindo documento legal para qualquer
   * dependente com {@code depPlanoSaude == true}.
   */
  public static DecisionBinding customerMode4ReplaceExactBinding() {
    return new DecisionBinding(
        "customer.emp2.dep.replace_erg03198",
        SLOT_POLITICA_PLANO_SAUDE,
        DecisionSource.CUSTOMER,
        CompositionPolicy.REPLACE_EXACT,
        RuleExecutorRef.jsonLogic(
            parseJson(
                """
                {
                  "or": [
                    {"!=": [{"var": "draft.depPlanoSaude"}, true]},
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
        95,
        true,
        RuleDecision.DENY,
        ErgonRuleErrorCatalog.CERG_03198.code(),
        List.of("draft.dtnasc", "draft.dtini", "documentoLegal"),
        null);
  }

  /** Constrói o {@link RuleSetDefinition} customizado do Órgão 2 ({@code empCodigo = 2}). */
  public static RuleSetDefinition orgao2CustomizedRuleSet() {
    return withCustomerBindings(
        List.of(
            customerMode2ParameterizeBinding(21, 24, 14, 21),
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
          "Invalid static JSON Logic in DependenteElegibilidadeRuleSet", ex);
    }
  }
}
