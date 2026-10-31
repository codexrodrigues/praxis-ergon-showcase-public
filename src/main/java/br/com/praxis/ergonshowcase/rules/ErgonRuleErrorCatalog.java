package br.com.praxis.ergonshowcase.rules;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Catálogo oficial de códigos de razão e mensagens de domínio para a família de regras estatutárias
 * de frequência ({@code RN-013} — {@code PCK_REGRAS_FREQ.MAIN_PRE} e {@code PCK_REGRAS_FREQ.MAIN_POS}).
 *
 * <p>Preserva os códigos canônicos do legado Oracle ({@code ERG-08380} a {@code ERG-08393} e
 * {@code ERG-00019}) e suporta códigos governados de customização de órgão ({@code CERG-*}),
 * eliminando o acoplamento a exceções PL/SQL ({@code PACK_HADES.TRATA_ERRO}) sem perder a
 * rastreabilidade de auditoria funcional.
 */
public final class ErgonRuleErrorCatalog {

  /**
   * Descritor imutável de um código de razão determinístico da {@code RN-013}.
   *
   * @param code código canônico da decisão ({@code ERG-08380}..{@code ERG-08393}, {@code ERG-00019}, {@code CERG-*})
   * @param slotKey chave estável do {@code DecisionSlot} associado no plano da {@code RN-013}
   * @param targetField atributo principal do contrato {@code RegraFrequenciaEstatutaria} afetado pela violação
   * @param messagePtBr mensagem oficial de negócio em português do Brasil
   * @param plsqlOrigin rotina de origem no pacote legado Oracle ({@code PCK_REGRAS_FREQ})
   * @param plsqlLineage predicado original extraído de {@code pck_regras_freq.pkb}
   */
  public record RuleErrorDescriptor(
      String code,
      String slotKey,
      String targetField,
      String messagePtBr,
      String plsqlOrigin,
      String plsqlLineage) {}

  private static final Map<String, RuleErrorDescriptor> CATALOG;

  static {
    Map<String, RuleErrorDescriptor> map = new LinkedHashMap<>();
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-00019",
            Rn013RegrasFrequenciaRuleSet.SLOT_PERIODO_ORDEM_VALIDA,
            "dtfim",
            "A data de fim da vigência deve ser maior ou igual à data de início (DTFIM >= DTINI).",
            "PCK_REGRAS_FREQ.MAIN_PRE",
            "IF v_regras_freq.dtfim IS NOT NULL THEN IF v_regras_freq.dtfim < v_regras_freq.dtini THEN pack_hades.trata_erro(00019); END IF; END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-08380",
            Rn013RegrasFrequenciaRuleSet.SLOT_QTD_MAX_NON_NEGATIVE,
            "qtdMaxDias",
            "A quantidade máxima de dias por concessão não pode ser menor que zero ou fora da faixa parametrizada pelo órgão.",
            "PCK_REGRAS_FREQ.MAIN_PRE",
            "IF v_regras_freq.qtd_max < 0 THEN pack_hades.trata_erro(8380); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-08381",
            Rn013RegrasFrequenciaRuleSet.SLOT_QTD_MIN_NON_NEGATIVE,
            "qtdMinDias",
            "A quantidade mínima de dias por concessão não pode ser menor que zero.",
            "PCK_REGRAS_FREQ.MAIN_PRE",
            "IF v_regras_freq.qtd_min < 0 THEN pack_hades.trata_erro(8381); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-08382",
            Rn013RegrasFrequenciaRuleSet.SLOT_QTD_MAX_COHERENT_WITH_MIN,
            "qtdMaxDias",
            "A quantidade máxima de dias não pode ser menor que a quantidade mínima exigida.",
            "PCK_REGRAS_FREQ.MAIN_PRE",
            "IF v_regras_freq.qtd_max < v_regras_freq.qtd_min THEN pack_hades.trata_erro(8382); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-08383",
            Rn013RegrasFrequenciaRuleSet.SLOT_CONSEC_MES_NON_NEGATIVE,
            "qtdMaxConsecMes",
            "A quantidade máxima consecutiva no mês (ou o par prazo/unidade de prazo) não pode ser menor que zero ou incompleto.",
            "PCK_REGRAS_FREQ.MAIN_PRE",
            "IF v_regras_freq.qtd_max_consec_mes < 0 THEN pack_hades.trata_erro(8383); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-08384",
            Rn013RegrasFrequenciaRuleSet.SLOT_CONSEC_ANO_NON_NEGATIVE,
            "qtdMaxConsecAno",
            "A quantidade máxima consecutiva no ano não pode ser menor que zero.",
            "PCK_REGRAS_FREQ.MAIN_PRE",
            "IF v_regras_freq.qtd_max_consec_ano < 0 THEN pack_hades.trata_erro(8384); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-08385",
            Rn013RegrasFrequenciaRuleSet.SLOT_INTERP_MES_NON_NEGATIVE,
            "qtdMaxInterpMes",
            "A quantidade máxima interpolada no mês não pode ser menor que zero.",
            "PCK_REGRAS_FREQ.MAIN_PRE",
            "IF v_regras_freq.qtd_max_interp_mes < 0 THEN pack_hades.trata_erro(8385); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-08386",
            Rn013RegrasFrequenciaRuleSet.SLOT_INTERP_ANO_NON_NEGATIVE,
            "qtdMaxInterpAno",
            "A quantidade máxima interpolada no ano não pode ser menor que zero.",
            "PCK_REGRAS_FREQ.MAIN_PRE",
            "IF v_regras_freq.qtd_max_interp_ano < 0 THEN pack_hades.trata_erro(8386); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-08387",
            Rn013RegrasFrequenciaRuleSet.SLOT_INT_PRORROG_NON_NEGATIVE,
            "intProrrogDias",
            "O intervalo de prorrogação em dias não pode ser menor que zero, nem informado quando a prorrogação (S/N) estiver vedada ('N').",
            "PCK_REGRAS_FREQ.MAIN_PRE",
            "IF v_regras_freq.int_prorrog < 0 THEN pack_hades.trata_erro(8387); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-08388",
            Rn013RegrasFrequenciaRuleSet.SLOT_INTERV_MIN_NON_NEGATIVE,
            "intervMinDias",
            "O intervalo mínimo (interstício) entre ocorrências não pode ser menor que zero ou ter par valor/unidade incompleto.",
            "PCK_REGRAS_FREQ.MAIN_PRE",
            "IF v_regras_freq.interv_min < 0 THEN pack_hades.trata_erro(8388); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-08389",
            Rn013RegrasFrequenciaRuleSet.SLOT_CONSEC_MES_CALENDAR_LIMIT,
            "qtdMaxConsecMes",
            "A quantidade máxima consecutiva no mês não pode ser maior que 31 dias.",
            "PCK_REGRAS_FREQ.MAIN_PRE",
            "IF v_regras_freq.qtd_max_consec_mes > 31 THEN pack_hades.trata_erro(8389); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-08390",
            Rn013RegrasFrequenciaRuleSet.SLOT_INTERP_MES_CALENDAR_LIMIT,
            "qtdMaxInterpMes",
            "A quantidade máxima interpolada no mês não pode ser maior que 31 dias.",
            "PCK_REGRAS_FREQ.MAIN_PRE",
            "IF v_regras_freq.qtd_max_interp_mes > 31 THEN pack_hades.trata_erro(8390); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-08391",
            Rn013RegrasFrequenciaRuleSet.SLOT_CONSEC_ANO_CALENDAR_LIMIT,
            "qtdMaxConsecAno",
            "A quantidade máxima consecutiva no ano não pode ser maior que 366 dias.",
            "PCK_REGRAS_FREQ.MAIN_PRE",
            "IF v_regras_freq.qtd_max_consec_ano > 366 THEN pack_hades.trata_erro(8391); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-08392",
            Rn013RegrasFrequenciaRuleSet.SLOT_INTERP_ANO_CALENDAR_LIMIT,
            "qtdMaxInterpAno",
            "A quantidade máxima interpolada no ano não pode ser maior que 366 dias.",
            "PCK_REGRAS_FREQ.MAIN_PRE",
            "IF v_regras_freq.qtd_max_interp_ano > 366 THEN pack_hades.trata_erro(8392); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-08393",
            Rn013RegrasFrequenciaRuleSet.SLOT_CONCOMITANCIA_VIGENCIA_OVERLAP,
            "dtini",
            "Já existe uma regra estatutária cadastrada com vigência concomitante para a mesma chave de escopo funcional.",
            "PCK_REGRAS_FREQ.MAIN_POS",
            "PACK_HADES.EH_CONCOMITANTE(RF.DTINI, RF.DTFIM, v_regras_freq.DTINI, v_regras_freq.DTFIM) = 1 -> pack_hades.trata_erro(8393);"));

    // Códigos de customização de órgão (Customer Rule Customization Model - CERG-*)
    register(
        map,
        new RuleErrorDescriptor(
            "CERG-09001",
            Rn013RegrasFrequenciaRuleSet.SLOT_CONSEC_ANO_CALENDAR_LIMIT,
            "documentoLegalId",
            "Para este órgão, regras de frequência com teto superior a 30 dias exigem vínculo obrigatório com documento legal normativo.",
            "EPB__REGRAS_FREQ.MAIN_PRE (AUGMENT)",
            "Customização aditiva do órgão (OverridePolicy.RESTRICT_ONLY + CompositionPolicy.AUGMENT sob DENY_OVERRIDES)"));
    register(
        map,
        new RuleErrorDescriptor(
            "CERG-08385",
            Rn013RegrasFrequenciaRuleSet.SLOT_INTERP_MES_NON_NEGATIVE,
            "qtdMaxInterpMes",
            "Para este órgão, regras aplicáveis exigem limite mensal interpolado estritamente positivo (>= 1 dia).",
            "EPB__REGRAS_FREQ.MAIN_PRE (REPLACE_EXACT)",
            "Substituição atômica de slot autorizado (OverridePolicy.REPLACEABLE + CompositionPolicy.REPLACE_EXACT)"));

    CATALOG = Collections.unmodifiableMap(map);
  }

  private ErgonRuleErrorCatalog() {}

  private static void register(Map<String, RuleErrorDescriptor> map, RuleErrorDescriptor descriptor) {
    map.put(descriptor.code(), descriptor);
  }

  /** Retorna o mapa imutável com todos os descritores de códigos de razão registrados. */
  public static Map<String, RuleErrorDescriptor> all() {
    return CATALOG;
  }

  /** Busca o descritor canônico pelo código de razão ({@code ERG-*} ou {@code CERG-*}). */
  public static Optional<RuleErrorDescriptor> find(String reasonCode) {
    if (reasonCode == null || reasonCode.isBlank()) {
      return Optional.empty();
    }
    return Optional.ofNullable(CATALOG.get(reasonCode));
  }

  /** Resolve a mensagem de negócio em PT-BR para o código de razão informado. */
  public static String resolveMessagePtBr(String reasonCode) {
    if ("FACT_REQUIRED_MISSING".equals(reasonCode) || "MISSING_REQUIRED_FACT".equals(reasonCode)) {
      return "Fato obrigatório ausente para avaliação determinística da regra estatutária (ex.: dtini ou existingOverlaps).";
    }
    return find(reasonCode)
        .map(RuleErrorDescriptor::messagePtBr)
        .orElse("Violação determinística da política de regras estatutárias de frequência (" + reasonCode + ").");
  }
}
