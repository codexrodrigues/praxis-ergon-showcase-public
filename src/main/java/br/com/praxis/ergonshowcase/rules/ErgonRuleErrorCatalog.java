package br.com.praxis.ergonshowcase.rules;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Catálogo oficial de códigos de razão e mensagens de domínio para as famílias de regras
 * estatutárias e operacionais do Ergon RH Público:
 * <ul>
 *   <li>{@code RN-013} ({@code PCK_REGRAS_FREQ.MAIN_PRE} e {@code PCK_REGRAS_FREQ.MAIN_POS});
 *   <li>{@code PCK_FREQUENCIAS} ({@link FrequenciaLancamentoRuleSet});
 *   <li>{@code PCK_LIC_AFAST} / {@code ERG_PM_GERALICAFAST} ({@link AfastamentoConcessaoRuleSet});
 *   <li>{@code PCK_VINCULOS} e {@code PCK_DEPENDENTES} ({@link VinculoProvimentoRuleSet}, {@link DependenteElegibilidadeRuleSet});
 *   <li>{@code PCK_PARAMETROS_CONTA}, {@code PCK_PERAQFER}, {@code PCK_FERIAS} e {@code PCK_ERG_ABONOS_FERIAS}
 *       ({@link ParametroContagemFeriasRuleSet}, {@link PeriodoAquisitivoFeriasRuleSet}, {@link FeriasGozoRuleSet});
 *   <li>Códigos governados de customização de órgão ({@code CERG-*}).
 * </ul>
 *
 * <p>Preserva os códigos canônicos do legado Oracle ({@code ERG-NNNNN}) e suporta códigos
 * governados de customização de órgão ({@code CERG-*}), eliminando o acoplamento a exceções PL/SQL
 * ({@code PACK_HADES.TRATA_ERRO}) sem perder a rastreabilidade de auditoria funcional.
 */
public final class ErgonRuleErrorCatalog {

  /**
   * Descritor imutável de um código de razão determinístico do domínio Ergon.
   *
   * @param code código canônico da decisão ({@code ERG-NNNNN} ou {@code CERG-NNNNN})
   * @param slotKey chave estável do {@code DecisionSlot} associado no plano compilado
   * @param targetField atributo principal do contrato afetado pela violação
   * @param messagePtBr mensagem oficial de negócio em português do Brasil
   * @param plsqlOrigin rotina de origem no pacote legado Oracle
   * @param plsqlLineage predicado original extraído do código-fonte PL/SQL legado
   */
  public record RuleErrorDescriptor(
      String code,
      String slotKey,
      String targetField,
      String messagePtBr,
      String plsqlOrigin,
      String plsqlLineage) {
    public String message() {
      return messagePtBr;
    }
  }

  // ===========================================================================
  // Constantes Canônicas — Dependentes (PCK_DEPENDENTES / PCK_DEPENDENCIAS)
  // ===========================================================================
  public static final RuleErrorDescriptor ERG_03162 =
      new RuleErrorDescriptor(
          "ERG-03162",
          DependenteElegibilidadeRuleSet.SLOT_PERIODO_VIGENCIA,
          "dtfim",
          "A data de início da dependência ou de expedição do RG não pode anteceder a data de nascimento, e a data de término da dependência não pode anteceder a data de início.",
          "PCK_DEPENDENTES.MAIN_PRE",
          "IF v_row_new.dtini < v_row_new.dtnasc OR v_row_new.dtfim < v_row_new.dtini THEN pack_hades.trata_erro(3162); END IF;");

  public static final RuleErrorDescriptor ERG_03182 =
      new RuleErrorDescriptor(
          "ERG-03182",
          DependenteElegibilidadeRuleSet.SLOT_DATA_NASCIMENTO,
          "dtnasc",
          "A data de nascimento do dependente não pode ser futura nem ultrapassar o limite máximo de 120 anos de idade.",
          "PCK_DEPENDENTES.VALID_IDADE_LIMITE",
          "IF p_depen.dtnasc > sysdate THEN pack_hades.trata_erro(3182); ELSIF add_months(p_depen.dtnasc, 1440) < sysdate THEN pack_hades.trata_erro(3181); END IF;");

  public static final RuleErrorDescriptor ERG_03183 =
      new RuleErrorDescriptor(
          "ERG-03183",
          DependenteElegibilidadeRuleSet.SLOT_COERENCIA_PARENTESCO,
          "parentesco",
          "Inconsistência genealógica ou documental entre dependente e servidor titular: filhos/enteados devem nascer após o titular, pais/avós antes do titular e o CPF do dependente não pode coincidir com o CPF do servidor.",
          "PCK_DEPENDENTES.VALID_IDADE_LIMITE / PRC_VALID_CPF",
          "IF v_dtnasc_func > p_depen.dtnasc THEN pack_hades.trata_erro(3183); ELSIF v_dtnasc_func < p_depen.dtnasc THEN pack_hades.trata_erro(3184); END IF;");

  public static final RuleErrorDescriptor ERG_03185 =
      new RuleErrorDescriptor(
          "ERG-03185",
          DependenteElegibilidadeRuleSet.SLOT_CPF_OBRIGATORIO_IRRF,
          "cpf",
          "Dependente elegível para dedução de IRRF exige CPF válido informado.",
          "PCK_DEPENDENTES.VALIDA_OBRIGATORIEDADE_CPF",
          "IF p_depen.dep_ir = 'S' AND p_depen.cpf IS NULL THEN pack_hades.trata_erro(3185); END IF;");

  public static final RuleErrorDescriptor ERG_03171 =
      new RuleErrorDescriptor(
          "ERG-03171",
          DependenteElegibilidadeRuleSet.SLOT_COMPATIBILIDADE_TIPO,
          "tipodepen",
          "O tipo de dependente informado não existe ou não permite a concessão de dedução de IRRF ou cota de salário-família solicitada.",
          "PCK_DEPENDENTES.MAIN_PRE",
          "IF NOT pack_hades.valida_itemtab('ERG_TIPO_DEPEN', v_row_new.tipodepen) THEN pack_hades.trata_erro(3171); END IF;");

  public static final RuleErrorDescriptor ERG_03177 =
      new RuleErrorDescriptor(
          "ERG-03177",
          DependenteElegibilidadeRuleSet.SLOT_ELEGIBILIDADE_ETARIA,
          "dtnasc",
          "Dependente excede o limite etário regulamentar do benefício: Elegibilidade ao IRRF exige idade até 21 anos (ou até 24 anos quando universitário), Salário-família exige idade até 14 anos e Plano de saúde exige idade dentro do teto do órgão, salvo invalidez ou excepcionalidade.",
          "PCK_DEPENDENTES.VALID_IDADE_LIMITE / PCK_DEPENDENCIAS",
          "IF v_idade > v_teto_beneficio AND p_depen.invalido <> 'S' AND p_depen.excepcional <> 'S' THEN pack_hades.trata_erro(3177); END IF;");

  public static final RuleErrorDescriptor ERG_03188 =
      new RuleErrorDescriptor(
          "ERG-03188",
          DependenteElegibilidadeRuleSet.SLOT_ENCERRAMENTO_SEM_BENEFICIOS,
          "dtfim",
          "Dependência encerrada não pode manter benefícios ativos: Dependente com data de término informada não pode manter benefícios ativos (IRRF, salário-família ou plano de saúde).",
          "PCK_DEPENDENTES.MAIN_PRE",
          "IF v_row_new.dtfim IS NOT NULL AND (v_row_new.dep_ir = 'S' OR v_row_new.dep_sal_fam = 'S' OR v_row_new.dep_plano_saude = 'S') THEN pack_hades.trata_erro(3188); END IF;");

  public static final RuleErrorDescriptor ERG_21400 =
      new RuleErrorDescriptor(
          "ERG-21400",
          DependenteElegibilidadeRuleSet.SLOT_EXIGENCIA_DOCUMENTO,
          "documentoLegalId",
          "Condição de invalidez, excepcionalidade, guarda judicial, tutela/curatela ou representação legal exige documento legal comprobatório válido do órgão e dados completos de RG/certidão.",
          "PCK_DEPENDENTES.CONSISTE_DOC_DEP",
          "IF (v_row_new.invalido = 'S' OR v_row_new.excepcional = 'S' OR v_row_new.representante_legal = 'S') AND v_row_new.documento_legal_id IS NULL THEN pack_hades.trata_erro(21400); END IF;");

  public static final RuleErrorDescriptor ERG_03198 =
      new RuleErrorDescriptor(
          "ERG-03198",
          DependenteElegibilidadeRuleSet.SLOT_POLITICA_PLANO_SAUDE,
          "depPlanoSaude",
          "Inclusão de dependente agregado no plano de saúde institucional exige documento legal comprobatório.",
          "PCK_DEPENDENCIAS.MAIN_PRE",
          "IF v_row_new.dep_plano_saude = 'S' AND v_row_new.parentesco = 'AGREGADO' AND v_row_new.documento_legal_id IS NULL THEN pack_hades.trata_erro(3198); END IF;");

  public static final RuleErrorDescriptor ERG_03193 =
      new RuleErrorDescriptor(
          "ERG-03193",
          DependenteElegibilidadeRuleSet.SLOT_UNICIDADE_CPF,
          "cpf",
          "Já existe outro dependente cadastrado para este servidor com o mesmo CPF informado.",
          "PCK_DEPENDENTES.PRC_VALID_CPF",
          "SELECT COUNT(1) FROM dependentes WHERE numfunc = p_numfunc AND cpf = p_cpf AND numero <> p_numero -> pack_hades.trata_erro(3193);");

  public static final RuleErrorDescriptor CERG_09301 =
      new RuleErrorDescriptor(
          "CERG-09301",
          DependenteElegibilidadeRuleSet.SLOT_EXIGENCIA_DOCUMENTO,
          "documentoLegalId",
          "Customização do Órgão 2: dependente universitário exige documento legal comprobatório da matrícula em instituição de ensino superior.",
          "EPB__DEPENDENTES (AUGMENT)",
          "Customização aditiva do Órgão 2 em DependenteElegibilidadeRuleSet (OverridePolicy.RESTRICT_ONLY + SlotPatchMode.AUGMENT sob DENY_OVERRIDES)");

  public static final RuleErrorDescriptor CERG_03198 =
      new RuleErrorDescriptor(
          "CERG-03198",
          DependenteElegibilidadeRuleSet.SLOT_POLITICA_PLANO_SAUDE,
          "documentoLegalId",
          "Política substitutiva do Órgão 2: toda adesão de dependente ao plano de saúde institucional exige documento legal comprobatório.",
          "EPB__DEPENDENCIAS (REPLACE_EXACT)",
          "Substituição atômica de slot autorizado em DependenteElegibilidadeRuleSet (OverridePolicy.REPLACEABLE + SlotPatchMode.REPLACE_EXACT)");

  // ===========================================================================
  // Constantes Canônicas — Vínculos (PCK_VINCULOS / PCK_FUNCIONARIOS)
  // ===========================================================================
  public static final RuleErrorDescriptor ERG_00324 =
      new RuleErrorDescriptor(
          "ERG-00324",
          VinculoProvimentoRuleSet.SLOT_CADEIA_CRONOLOGICA,
          "dtexerc",
          "Inconsistência na cadeia cronológica do vínculo: Nomeação, posse, exercício, vacância, aposentadoria, pagamento, vigência contratual, prorrogação contratual, cessão e homologação de estágio devem respeitar a ordem temporal.",
          "PCK_VINCULOS.VALID_VINCULOS",
          "IF p_vinculos.dtnom > p_vinculos.dtposse OR p_vinculos.dtposse > p_vinculos.dtexerc OR p_vinculos.dtvac < p_vinculos.dtexerc THEN ergon_erro_pack.trata_erro(324); END IF;");

  public static final RuleErrorDescriptor ERG_00325 =
      new RuleErrorDescriptor(
          "ERG-00325",
          VinculoProvimentoRuleSet.SLOT_IDADE_MINIMA_INGRESSO,
          "dtposse",
          "Servidor titular não existe ou não possui a idade mínima constitucional de 18 anos na data de posse ou entrada em exercício.",
          "PCK_VINCULOS.VALID_VINCULOS / EP__VINCULOS_DTIDADE",
          "IF p_vinculos.dtposse < add_months(v_dtnasc, v_idade_minima * 12) THEN ergon_erro_pack.trata_erro(325); END IF;");

  public static final RuleErrorDescriptor ERG_00354 =
      new RuleErrorDescriptor(
          "ERG-00354",
          VinculoProvimentoRuleSet.SLOT_COMPATIBILIDADE_REGIME_CARGO,
          "regimejur",
          "Órgão inativo para admissão, incompatibilidade entre regime jurídico, tipo de vínculo e cargo, ou referência funcional (setor, cargo ou função) vinculada a outro órgão.",
          "PCK_VINCULOS.VERIFICA_TIPOVINC_VALID",
          "SELECT 'X' FROM erg_tipovinc_valid WHERE tipovinc = p_tipovinc AND categoria = p_categoria AND regimejur = p_regimejur -> ergon_erro_pack.trata_erro(354);");

  public static final RuleErrorDescriptor ERG_00359 =
      new RuleErrorDescriptor(
          "ERG-00359",
          VinculoProvimentoRuleSet.SLOT_VIGENCIA_CARGO_VAGAS,
          "cargo",
          "Provimento bloqueado no quadro de cargos: não é permitido vincular cargo ou função extinto em vínculo ativo, nem prover ocupação quando o Saldo de vagas disponíveis estiver esgotado.",
          "PCK_VINCULOS.VERIFICA_CATEG_CARGO_PROV",
          "Validação de vigência do cargo (dtExtincao IS NULL) e saldo positivo de vagas autorizadas em cargos com controleVaga = true.");

  public static final RuleErrorDescriptor ERG_00315 =
      new RuleErrorDescriptor(
          "ERG-00315",
          VinculoProvimentoRuleSet.SLOT_LIMITES_JORNADA_EXERCICIO,
          "jornadaSemanal",
          "A jornada semanal deve estar entre 1 e 168 horas (respeitando o teto operacional do órgão) e o prazo entre a posse e a entrada em exercício não pode exceder o limite regulamentar.",
          "PCK_VINCULOS.VALID_VINCULOS",
          "Validação parametrizável por órgão de carga horária máxima semanal (maxWeeklyHours) e prazo entre posse e exercício (maxDaysPosseToExercicio).");

  public static final RuleErrorDescriptor ERG_00342 =
      new RuleErrorDescriptor(
          "ERG-00342",
          VinculoProvimentoRuleSet.SLOT_VACANCIA_SEM_AFASTAMENTO,
          "dtvac",
          "Vacância exige forma de desligamento e inexistência de afastamento em aberto no vínculo funcional.",
          "PCK_VINCULOS.VALID_VINCULOS / VERIFICA_LICAFAST",
          "IF p_vinculos.formavac IS NULL AND p_vinculos.dtvac IS NOT NULL THEN ergon_erro_pack.trata_erro(342); END IF; VERIFICA_LICAFAST(numfunc, numero, dtvac, 'vacância');");

  public static final RuleErrorDescriptor ERG_00330 =
      new RuleErrorDescriptor(
          "ERG-00330",
          VinculoProvimentoRuleSet.SLOT_POLITICA_CONTRATO_TEMPORARIO,
          "dtinicontr",
          "Contrato temporário exige datas de início e término da vigência contratual, e requisição/cessão formalizada exige identificação do órgão requisitante.",
          "PCK_VINCULOS.VALID_VINCULOS",
          "IF p_vinculos.dtinicontr IS NOT NULL AND p_vinculos.dtfimcontr IS NULL THEN ergon_erro_pack.trata_erro(330); END IF;");

  public static final RuleErrorDescriptor ERG_00300 =
      new RuleErrorDescriptor(
          "ERG-00300",
          VinculoProvimentoRuleSet.SLOT_ACUMULACAO_CONSTITUCIONAL,
          "categoria",
          "Acumulação de vínculos ativos exige até dois cargos constitucionalmente acumuláveis (MAGISTERIO ou SAUDE) ou períodos de exercício não concomitantes.",
          "PCK_VINCULOS.VALID_VINC_CONCOMIT",
          "Verificação constitucional (CF/88 Art. 37, XVI) de acumulação de até 2 vínculos ativos concomitantes para o mesmo ServidorPessoa.");

  public static final RuleErrorDescriptor CERG_09401 =
      new RuleErrorDescriptor(
          "CERG-09401",
          VinculoProvimentoRuleSet.SLOT_VACANCIA_SEM_AFASTAMENTO,
          "documentoLegalId",
          "Customização do Órgão 2: designação para cargo em comissão ou registro de vacância exige documento legal comprobatório válido do órgão.",
          "EPB__VINCULOS (AUGMENT)",
          "Customização aditiva do Órgão 2 em VinculoProvimentoRuleSet (OverridePolicy.RESTRICT_ONLY + SlotPatchMode.AUGMENT sob DENY_OVERRIDES)");

  public static final RuleErrorDescriptor CERG_00330 =
      new RuleErrorDescriptor(
          "CERG-00330",
          VinculoProvimentoRuleSet.SLOT_POLITICA_CONTRATO_TEMPORARIO,
          "dtfimcontr",
          "Política substitutiva do Órgão 2: contrato temporário exige datas de início e fim, documento legal comprobatório e duração contratual máxima de 730 dias (2 anos).",
          "EPB__VINCULOS (REPLACE_EXACT)",
          "Substituição atômica de slot autorizado em VinculoProvimentoRuleSet (OverridePolicy.REPLACEABLE + SlotPatchMode.REPLACE_EXACT)");

  // ===========================================================================
  // Constantes Canônicas — Campos Customizados JSON Logic
  // ===========================================================================
  public static final RuleErrorDescriptor ERG_21501 =
      new RuleErrorDescriptor(
          "ERG-21501",
          "customField.validationRules.syntax",
          "validationRules",
          "A expressão JSON Logic informada em validationRules possui sintaxe, aridade, operador ou referência de variável inválida.",
          "PraxisJsonLogicEngine.validate",
          "Validação sintática e estrutural da árvore JSON Logic de DefinicaoCampoCustomizado.validationRules.");

  public static final RuleErrorDescriptor ERG_21502 =
      new RuleErrorDescriptor(
          "ERG-21502",
          "customField.validationRules.evaluation",
          "customFields",
          "O valor informado para o campo customizado violou a regra determinística JSON Logic configurada para o recurso.",
          "PraxisJsonLogicEngine.evaluate",
          "Avaliação determinística em runtime de DefinicaoCampoCustomizado.validationRules sobre {value, customFields, draft}.");

  private static final Map<String, RuleErrorDescriptor> CATALOG;

  static {
    Map<String, RuleErrorDescriptor> map = new LinkedHashMap<>();

    // =========================================================================
    // 1. Família RN-013 (PCK_REGRAS_FREQ)
    // =========================================================================
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

    // =========================================================================
    // 2. Família Lançamento de Frequência (PCK_FREQUENCIAS)
    // =========================================================================
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-00415",
            FrequenciaLancamentoRuleSet.SLOT_PERIODO_ORDEM_VALIDA,
            "dtfim",
            "A data final da ocorrência de frequência não pode anteceder a data de início.",
            "PCK_FREQUENCIAS.verifica_data",
            "IF p_dtini > p_dtfim AND p_dtfim IS NOT NULL THEN pack_hades.trata_erro(415); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-00400",
            FrequenciaLancamentoRuleSet.SLOT_CODIGO_HABILITADO_VIA_FREQUENCIA,
            "codfreq",
            "O código de frequência informado não existe ou não permite lançamento via frequência.",
            "PCK_FREQUENCIAS.MAIN_PRE",
            "IF v_cod_freq.via_frequencia = 'N' THEN pack_hades.trata_erro(400); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-00410",
            FrequenciaLancamentoRuleSet.SLOT_VINCULO_VIGENTE_NO_PERIODO,
            "dtini",
            "O período da ocorrência de frequência deve estar contido na vigência ativa do vínculo funcional (após o exercício e antes da vacância/aposentadoria).",
            "PCK_FREQUENCIAS.verifica_data_freq",
            "IF p_dtini < v_dtexerc THEN pack_hades.trata_erro(410); ELSIF p_dtini >= v_dtvac THEN pack_hades.trata_erro(411); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-00436",
            FrequenciaLancamentoRuleSet.SLOT_HORARIO_ENTRADA_SAIDA_VALIDO,
            "horaSaida",
            "O código de frequência exige o preenchimento de hora de entrada e hora de saída coerentes (horaSaida >= horaEntrada).",
            "PCK_FREQUENCIAS.valida_hora_entrada_saida",
            "IF v_cod_freq.hora_entrada_saida = 'S' AND (hora_entrada IS NULL OR hora_saida IS NULL) THEN pack_hades.trata_erro(436); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-00443",
            FrequenciaLancamentoRuleSet.SLOT_COMPATIBILIDADE_SEXO_SERVIDOR,
            "codfreq",
            "O código de frequência é exclusivo para servidores do sexo compatível com a modalidade cadastrada.",
            "PCK_FREQUENCIAS.MAIN_PRE",
            "IF v_cod_freq.sexo <> 'A' AND v_cod_freq.sexo <> v_sexo THEN pack_hades.trata_erro(443); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-00441",
            FrequenciaLancamentoRuleSet.SLOT_REGRA_ESTATUTARIA_OBRIGATORIA,
            "codfreq",
            "A modalidade de frequência exige regra estatutária vigente (REGRAS_FREQ) para o regime jurídico e vínculo do servidor.",
            "PCK_FREQUENCIAS.valid_regras_freq",
            "IF v_cod_freq.regras_freq = 'S' AND NOT found_regras_freq THEN pack_hades.trata_erro(441); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-00430",
            FrequenciaLancamentoRuleSet.SLOT_VEDACAO_NAO_SE_APLICA,
            "codfreq",
            "A regra estatutária vigente veda expressamente (naoSeAplica = true) esta modalidade de frequência para o escopo funcional do servidor.",
            "PCK_FREQUENCIAS.valid_regras_freq",
            "IF v_regras_freq.nao_se_aplica = 'S' THEN pack_hades.trata_erro(430); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-00427",
            FrequenciaLancamentoRuleSet.SLOT_DURACAO_MINIMA_ESTATUTARIA,
            "quantidade",
            "A quantidade ou duração da ocorrência é inferior ao limite mínimo permitido pela modalidade ou regra estatutária.",
            "PCK_FREQUENCIAS.valid_regras_freq",
            "IF v_qtd < v_regras_freq.qtd_min THEN pack_hades.trata_erro(427); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-00428",
            FrequenciaLancamentoRuleSet.SLOT_DURACAO_MAXIMA_ESTATUTARIA,
            "quantidade",
            "A quantidade ou duração da ocorrência excede o limite máximo permitido pela modalidade ou regra estatutária.",
            "PCK_FREQUENCIAS.valid_regras_freq",
            "IF v_qtd > v_regras_freq.qtd_max THEN pack_hades.trata_erro(428); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-00429",
            FrequenciaLancamentoRuleSet.SLOT_LIMITE_PRORROGACAO_ACUMULADA,
            "dtini",
            "A soma de dias acumulados dentro do intervalo de prorrogação (intProrrogDias) excede o limite máximo estatutário.",
            "PCK_FREQUENCIAS.valid_regras_freq",
            "IF v_qtd_acum_prorrog > v_regras_freq.qtd_max THEN pack_hades.trata_erro(429); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-00440",
            FrequenciaLancamentoRuleSet.SLOT_INTERSTICIO_MINIMO_ENTRE_OCORRENCIAS,
            "dtini",
            "Não foi cumprido o interstício mínimo em dias (intervMinDias) desde o término da ocorrência anterior da mesma espécie.",
            "PCK_FREQUENCIAS.valid_regras_freq",
            "IF (p_dtini - v_dtfim_ant - 1) < v_regras_freq.interv_min THEN pack_hades.trata_erro(440); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-00444",
            FrequenciaLancamentoRuleSet.SLOT_LIMITES_ACUMULADOS_MES_ANO,
            "quantidade",
            "O lançamento ultrapassa os limites acumulados mensais ou anuais (consecutivos ou interpolados) definidos na regra estatutária.",
            "PCK_FREQUENCIAS.valid_regras_freq",
            "IF v_acum_mes > qtd_max_consec_mes OR v_acum_ano > qtd_max_interp_ano THEN pack_hades.trata_erro(444); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-00445",
            FrequenciaLancamentoRuleSet.SLOT_EXIGENCIA_DOCUMENTO_LEGAL,
            "documentoLegalId",
            "A regra estatutária vigente exige a vinculação de um documento legal ou ato comprobatório para esta ocorrência de frequência.",
            "PCK_FREQUENCIAS.valid_regras_freq",
            "IF v_regras_freq.pontlei IS NOT NULL AND p_pontlei IS NULL THEN pack_hades.trata_erro(445); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-00446",
            FrequenciaLancamentoRuleSet.SLOT_LIMITE_RETROATIVIDADE_LANCAMENTO,
            "dtini",
            "O lançamento ou abono retroativo excede o limite máximo de dias retroativos parametrizado para o órgão.",
            "EP__VERIFICA_FREQUENCIAS",
            "IF (TRUNC(SYSDATE) - p_dtini) > v_max_dias_retroativos THEN pack_hades.trata_erro(10400); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-00417",
            FrequenciaLancamentoRuleSet.SLOT_CONCOMITANCIA_AFASTAMENTO_ATIVO,
            "dtini",
            "O servidor possui afastamento ou licença ativa no período e a regra estatutária não permite concomitância.",
            "PCK_FREQUENCIAS.verifica_concomitancia_afast",
            "IF existe_afastamento_no_periodo AND v_permite_concomitancia = 'N' THEN pack_hades.trata_erro(417); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-00424",
            FrequenciaLancamentoRuleSet.SLOT_SOBREPOSICAO_OCORRENCIA_MESMO_TIPO,
            "dtini",
            "Já existe ocorrência de frequência ativa para o mesmo vínculo e tipo de frequência sobreposta ao período informado.",
            "PCK_FREQUENCIAS.verifica_concomitancia_periodo",
            "PACK_HADES.EH_CONCOMITANTE(F.DTINI, F.DTFIM, v_freq.DTINI, v_freq.DTFIM) = 1 -> pack_hades.trata_erro(424);"));

    // =========================================================================
    // 3. Família Concessão de Licenças e Afastamentos (PCK_LIC_AFAST)
    // =========================================================================
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-04312",
            AfastamentoConcessaoRuleSet.SLOT_PERIODO_ORDEM_VALIDA,
            "dtprevfim",
            "A data de término previsto ou efetivo do afastamento não pode anteceder a data de início.",
            "PCK_LIC_AFAST.MAIN_PRE",
            "IF v_lic_afast.dtfim < v_lic_afast.dtini THEN pack_hades.trata_erro(4312); ELSIF v_lic_afast.dtprevfim < v_lic_afast.dtini THEN pack_hades.trata_erro(4313); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-04314",
            AfastamentoConcessaoRuleSet.SLOT_VINCULO_EM_EXERCICIO_NO_PERIODO,
            "dtini",
            "O início do afastamento deve ser igual ou posterior à entrada em exercício do vínculo e anterior à vacância ou aposentadoria.",
            "PCK_LIC_AFAST.valid_licafast",
            "IF p_dtini < v_dtexerc OR p_dtini >= v_dtvac OR p_dtini >= v_dtaposent THEN pack_hades.trata_erro(4314); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-04306",
            AfastamentoConcessaoRuleSet.SLOT_MODALIDADE_HABILITADA_AFASTAMENTO,
            "codfreq",
            "O código de frequência informado não existe ou não está habilitado para concessão de licenças e afastamentos.",
            "PCK_LIC_AFAST.MAIN_PRE",
            "IF v_cod_freq.afastamento <> 'S' THEN pack_hades.trata_erro(4306); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-04310",
            AfastamentoConcessaoRuleSet.SLOT_COMPATIBILIDADE_SEXO_SERVIDOR,
            "codfreq",
            "A modalidade de licença ou afastamento é exclusiva para servidores do sexo compatível com o código de frequência.",
            "PCK_LIC_AFAST.MAIN_PRE",
            "IF v_cod_freq.sexo <> 'A' AND v_cod_freq.sexo <> v_sexo THEN pack_hades.trata_erro(4310); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-04305",
            AfastamentoConcessaoRuleSet.SLOT_REGRA_ESTATUTARIA_OBRIGATORIA,
            "codfreq",
            "A modalidade de afastamento exige regra estatutária vigente (REGRAS_FREQ) para o escopo funcional do servidor.",
            "PCK_LIC_AFAST.valid_reg_licafast",
            "IF v_cod_freq.regras_freq = 'S' AND NOT found_regras_freq THEN pack_hades.trata_erro(4305); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-04301",
            AfastamentoConcessaoRuleSet.SLOT_VEDACAO_NAO_SE_APLICA,
            "codfreq",
            "A regra estatutária vigente veda expressamente (naoSeAplica = true) a concessão deste afastamento para o vínculo do servidor.",
            "PCK_LIC_AFAST.valid_reg_licafast",
            "IF v_regras_freq.nao_se_aplica = 'S' THEN pack_hades.trata_erro(4301); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-04304",
            AfastamentoConcessaoRuleSet.SLOT_DURACAO_MINIMA_ESTATUTARIA,
            "dtprevfim",
            "A duração em dias do afastamento é inferior ao mínimo regulamentar exigido pela modalidade ou regra estatutária.",
            "PCK_LIC_AFAST.valid_reg_licafast",
            "IF v_dias < v_regras_freq.qtd_min THEN pack_hades.trata_erro(4304); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-04303",
            AfastamentoConcessaoRuleSet.SLOT_DURACAO_MAXIMA_ESTATUTARIA,
            "dtprevfim",
            "A duração em dias do afastamento excede o limite máximo regulamentar permitido pela modalidade ou regra estatutária.",
            "PCK_LIC_AFAST.valid_reg_licafast",
            "IF v_dias > v_regras_freq.qtd_max THEN pack_hades.trata_erro(4303); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-04317",
            AfastamentoConcessaoRuleSet.SLOT_PRORROGACAO_ESTATUTARIA_VALIDA,
            "afastamentoOrigemId",
            "A prorrogação exige permissão na regra estatutária, afastamento de origem válido do mesmo vínculo, intervalo dentro de intProrrogDias e soma acumulada dentro de qtdMaxDias.",
            "PCK_LIC_AFAST.valid_reg_licafast",
            "IF v_prorrog = 'S' AND (v_int > int_prorrog OR v_soma_prorrog > qtd_max) THEN pack_hades.trata_erro(4303); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-04309",
            AfastamentoConcessaoRuleSet.SLOT_INTERSTICIO_MINIMO_ENTRE_CONCESSOES,
            "dtini",
            "Não foi cumprido o interstício mínimo em dias (intervMinDias) desde o encerramento da concessão anterior da mesma modalidade.",
            "PCK_LIC_AFAST.valid_reg_licafast",
            "IF (p_dtini - v_dtfim_ant - 1) < v_regras_freq.interv_min THEN pack_hades.trata_erro(4309); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-04311",
            AfastamentoConcessaoRuleSet.SLOT_LIMITES_ACUMULADOS_MES_ANO,
            "dtprevfim",
            "A concessão ultrapassa os limites acumulados mensais ou anuais (consecutivos ou interpolados) estabelecidos na regra estatutária.",
            "PCK_LIC_AFAST.valid_reg_licafast",
            "IF v_dias_mes > qtd_max_consec_mes OR v_dias_ano > qtd_max_interp_ano THEN pack_hades.trata_erro(4311); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-07513",
            AfastamentoConcessaoRuleSet.SLOT_DISPENSA_PERICIA_DOCUMENTO_LEGAL,
            "documentoLegalId",
            "O afastamento exige laudo de perícia médica oficial ou documento legal comprobatório por exceder a faixa de dispensa parametrizada pelo órgão.",
            "ERG_PM_GERALICAFAST",
            "IF v_dias > v_dispensa_pericia_dias AND p_pontlei IS NULL THEN pack_hades.trata_erro(7513); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-04318",
            AfastamentoConcessaoRuleSet.SLOT_SUBSTITUTO_DESIGNADO_VALIDO,
            "numfuncSubstituto",
            "O substituto designado deve informar servidor e vínculo ativos do mesmo órgão e não pode ser o próprio servidor afastado.",
            "EP__VERIFICA_AFAST_SUBSTITUTO",
            "IF p_numfunc_subst = p_numfunc OR NOT substituto_ativo_mesmo_orgao THEN pack_hades.trata_erro(4318); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-04316",
            AfastamentoConcessaoRuleSet.SLOT_CONCOMITANCIA_FREQUENCIA_ATIVA,
            "dtini",
            "Existe ocorrência de frequência ativa registrada no período do afastamento e a regra estatutária não permite concomitância.",
            "PCK_LIC_AFAST.valid_licafast",
            "IF existe_frequencia_no_periodo AND v_permite_concomitancia = 'N' THEN pack_hades.trata_erro(4316); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-04300",
            AfastamentoConcessaoRuleSet.SLOT_SOBREPOSICAO_AFASTAMENTO_ATIVO,
            "dtini",
            "O período informado conflita com outro afastamento ativo ou homologado existente para o mesmo vínculo.",
            "PCK_LIC_AFAST.MAIN_POS",
            "PACK_HADES.EH_CONCOMITANTE(LA.DTINI, LA.DTFIM, v_la.DTINI, v_la.DTFIM) = 1 -> pack_hades.trata_erro(4300);"));

    // =========================================================================
    // 4. Códigos de Customização de Órgão (Customer Rule Customization Model - CERG-*)
    // =========================================================================
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
    register(
        map,
        new RuleErrorDescriptor(
            "CERG-09101",
            FrequenciaLancamentoRuleSet.SLOT_LIMITES_ACUMULADOS_MES_ANO,
            "documentoLegalId",
            "Para este órgão, ocorrências de frequência com duração superior a 3 dias exigem documento legal comprobatório obrigatório.",
            "EPB__FREQUENCIAS.MAIN_PRE (AUGMENT)",
            "Customização aditiva do órgão em FrequenciaLancamentoRuleSet (OverridePolicy.RESTRICT_ONLY + CompositionPolicy.AUGMENT sob DENY_OVERRIDES)"));
    register(
        map,
        new RuleErrorDescriptor(
            "CERG-00417",
            FrequenciaLancamentoRuleSet.SLOT_CONCOMITANCIA_AFASTAMENTO_ATIVO,
            "dtini",
            "Para este órgão, é estritamente vedado lançar qualquer ocorrência de frequência durante período de afastamento ativo, mesmo quando a regra estatutária prevê concomitância.",
            "EPB__FREQUENCIAS.verifica_concomitancia_afast (REPLACE_EXACT)",
            "Substituição atômica de slot autorizado em FrequenciaLancamentoRuleSet (OverridePolicy.REPLACEABLE + CompositionPolicy.REPLACE_EXACT)"));
    register(
        map,
        new RuleErrorDescriptor(
            "CERG-09201",
            AfastamentoConcessaoRuleSet.SLOT_LIMITES_ACUMULADOS_MES_ANO,
            "atoPublicacaoId",
            "Para este órgão, concessões de licença ou afastamento superiores a 30 dias exigem ato formal de publicação em Diário Oficial.",
            "EPB__LIC_AFAST.MAIN_PRE (AUGMENT)",
            "Customização aditiva do órgão em AfastamentoConcessaoRuleSet (OverridePolicy.RESTRICT_ONLY + CompositionPolicy.AUGMENT sob DENY_OVERRIDES)"));
    register(
        map,
        new RuleErrorDescriptor(
            "CERG-04316",
            AfastamentoConcessaoRuleSet.SLOT_CONCOMITANCIA_FREQUENCIA_ATIVA,
            "dtini",
            "Para este órgão, é estritamente vedado conceder afastamento sobre período com frequência ativa registrada, mesmo quando a regra estatutária prevê concomitância.",
            "EPB__LIC_AFAST.valid_licafast (REPLACE_EXACT)",
            "Substituição atômica de slot autorizado em AfastamentoConcessaoRuleSet (OverridePolicy.REPLACEABLE + CompositionPolicy.REPLACE_EXACT)"));

    // =========================================================================
    // 5. Família Dependentes e Elegibilidade (PCK_DEPENDENTES / PCK_DEPENDENCIAS)
    // =========================================================================
    register(map, ERG_03162);
    register(map, ERG_03182);
    register(map, ERG_03183);
    register(map, ERG_03185);
    register(map, ERG_03171);
    register(map, ERG_03177);
    register(map, ERG_03188);
    register(map, ERG_21400);
    register(map, ERG_03198);
    register(map, ERG_03193);
    register(map, CERG_09301);
    register(map, CERG_03198);

    // =========================================================================
    // 6. Família Vínculos e Provimento (PCK_VINCULOS / PCK_FUNCIONARIOS)
    // =========================================================================
    register(map, ERG_00324);
    register(map, ERG_00325);
    register(map, ERG_00354);
    register(map, ERG_00359);
    register(map, ERG_00315);
    register(map, ERG_00342);
    register(map, ERG_00330);
    register(map, ERG_00300);
    register(map, CERG_09401);
    register(map, CERG_00330);

    // =========================================================================
    // 7. Família Campos Customizados JSON Logic
    // =========================================================================
    register(map, ERG_21501);
    register(map, ERG_21502);

    // =========================================================================
    // 8. Família Parâmetros de Contagem de Férias (PCK_PARAMETROS_CONTA)
    // =========================================================================
    registerFeriasDescriptors(map);

    CATALOG = Collections.unmodifiableMap(map);
  }

  private static void registerFeriasDescriptors(Map<String, RuleErrorDescriptor> map) {
    // PCK_PARAMETROS_CONTA
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-01873",
            ParametroContagemFeriasRuleSet.SLOT_VIGENCIA_ORDEM_VALIDA,
            "dtfim",
            "Data de início da vigência do parâmetro de contagem não pode ser posterior à data de término.",
            "PCK_PARAMETROS_CONTA.MAIN_PRE",
            "IF v_row_new.dtfim < v_row_new.dtini THEN pack_hades.trata_erro(1873); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-01870",
            ParametroContagemFeriasRuleSet.SLOT_PRESCRICAO_E_FLAGS_GOZO,
            "criterioPrescricao",
            "Critério de prescrição deve ser 'I' (início do gozo) ou 'F' (fim do gozo) e indicadores de prescrição/concomitância devem ser válidos.",
            "PCK_PARAMETROS_CONTA.MAIN_PRE",
            "IF v_row_new.criterio_prescr NOT IN ('I','F') THEN pack_hades.trata_erro(1870); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-01871",
            ParametroContagemFeriasRuleSet.SLOT_PRESCRICAO_E_FLAGS_GOZO,
            "permiteGozoAntesPaFer",
            "Indicador de permissão de gozo antes do fim do período aquisitivo inválido.",
            "PCK_PARAMETROS_CONTA.MAIN_PRE",
            "IF v_row_new.permite_gozo_antes_pa_fer NOT IN ('S','N') THEN pack_hades.trata_erro(1871); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-01872",
            ParametroContagemFeriasRuleSet.SLOT_PRESCRICAO_E_FLAGS_GOZO,
            "permiteGozoAposPrescricao",
            "Indicador de permissão de gozo após a prescrição inválido.",
            "PCK_PARAMETROS_CONTA.MAIN_PRE",
            "IF v_row_new.permite_gozo_pos_prescr NOT IN ('S','N') THEN pack_hades.trata_erro(1872); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-01800",
            ParametroContagemFeriasRuleSet.SLOT_FERIAS_HABILITADAS_OBRIGATORIOS,
            "totDiasFer",
            "Quando o regime possui direito a férias (temFerias = true), totDiasFer, maxPerFer, minPerFer, maxDiasPerFer e minDiasPerFer são obrigatórios e positivos.",
            "PCK_PARAMETROS_CONTA.MAIN_PRE",
            "IF v_row_new.tem_ferias = 'S' AND (v_row_new.tot_dias_fer IS NULL OR v_row_new.tot_dias_fer <= 0) THEN pack_hades.trata_erro(1800); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-01801",
            ParametroContagemFeriasRuleSet.SLOT_ABONO_PECUNIARIO_FRACAO_LIMITES,
            "abonoDirGozo",
            "Modalidade de abono pecuniário deve ser 'D' (sobre direito), 'G' (sobre gozo) ou 'N' (sem abono).",
            "PCK_PARAMETROS_CONTA.MAIN_PRE",
            "IF v_row_new.abono_dir_gozo NOT IN ('D','G','N') THEN pack_hades.trata_erro(1801); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-01802",
            ParametroContagemFeriasRuleSet.SLOT_ABONO_PECUNIARIO_FRACAO_LIMITES,
            "numDiasAbonoFer",
            "Fração e limites de abono pecuniário inválidos: numerador/denominador obrigatórios quando há abono (0 <= num <= den, den > 0) e minDiasPvendFer <= maxDiasVendFer <= totDiasFer.",
            "PCK_PARAMETROS_CONTA.MAIN_PRE",
            "IF v_row_new.abono_dir_gozo IN ('D','G') AND (v_row_new.num_dias_abono_fer IS NULL OR v_row_new.den_dias_abono_fer IS NULL) THEN pack_hades.trata_erro(1802); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-01803",
            ParametroContagemFeriasRuleSet.SLOT_ABONO_PECUNIARIO_FRACAO_LIMITES,
            "denDiasAbonoFer",
            "Denominador da fração de abono pecuniário deve ser maior que zero.",
            "PCK_PARAMETROS_CONTA.MAIN_PRE",
            "IF v_row_new.den_dias_abono_fer <= 0 THEN pack_hades.trata_erro(1803); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-01804",
            ParametroContagemFeriasRuleSet.SLOT_ABONO_PECUNIARIO_FRACAO_LIMITES,
            "numDiasAbonoFer",
            "Numerador da fração de abono pecuniário não pode ser negativo.",
            "PCK_PARAMETROS_CONTA.MAIN_PRE",
            "IF v_row_new.num_dias_abono_fer < 0 THEN pack_hades.trata_erro(1804); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-01805",
            ParametroContagemFeriasRuleSet.SLOT_ABONO_PECUNIARIO_FRACAO_LIMITES,
            "numDiasAbonoFer",
            "Numerador da fração de abono pecuniário não pode superar o denominador.",
            "PCK_PARAMETROS_CONTA.MAIN_PRE",
            "IF v_row_new.num_dias_abono_fer > v_row_new.den_dias_abono_fer THEN pack_hades.trata_erro(1805); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-01806",
            ParametroContagemFeriasRuleSet.SLOT_FERIAS_LIMITES_DIAS_TEMPLATE,
            "totDiasFer",
            "Limites de dias de férias e teto de parcelas fora dos parâmetros permitidos pelo órgão.",
            "PCK_PARAMETROS_CONTA.MAIN_PRE",
            "IF v_row_new.tot_dias_fer > 60 OR v_row_new.max_per_fer > 6 THEN pack_hades.trata_erro(1806); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-01807",
            ParametroContagemFeriasRuleSet.SLOT_ABONO_PECUNIARIO_FRACAO_LIMITES,
            "minDiasPvendFer",
            "Mínimo de dias para venda de abono não pode superar o máximo de dias vendáveis.",
            "PCK_PARAMETROS_CONTA.MAIN_PRE",
            "IF v_row_new.min_dias_pvend_fer > v_row_new.max_dias_vend_fer THEN pack_hades.trata_erro(1807); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-01808",
            ParametroContagemFeriasRuleSet.SLOT_ABONO_PECUNIARIO_FRACAO_LIMITES,
            "maxDiasVendFer",
            "Máximo de dias vendáveis em abono não pode superar o total de dias de férias.",
            "PCK_PARAMETROS_CONTA.MAIN_PRE",
            "IF v_row_new.max_dias_vend_fer > v_row_new.tot_dias_fer THEN pack_hades.trata_erro(1808); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-01809",
            ParametroContagemFeriasRuleSet.SLOT_PARCELAMENTO_COERENCIA,
            "minPerFer",
            "Mínimo de parcelas de férias não pode ser superior ao máximo de parcelas permitido.",
            "PCK_PARAMETROS_CONTA.MAIN_PRE",
            "IF v_row_new.min_per_fer > v_row_new.max_per_fer THEN pack_hades.trata_erro(1809); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-01810",
            ParametroContagemFeriasRuleSet.SLOT_PARCELAMENTO_COERENCIA,
            "minDiasPerFer",
            "Mínimo de dias por parcela não pode ser superior ao máximo de dias por parcela (e parcela principal deve estar dentro da faixa).",
            "PCK_PARAMETROS_CONTA.MAIN_PRE",
            "IF v_row_new.min_dias_per_fer > v_row_new.max_dias_per_fer THEN pack_hades.trata_erro(1810); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-01811",
            ParametroContagemFeriasRuleSet.SLOT_FERIAS_DESABILITADAS_SEM_LIMITES,
            "temFerias",
            "Quando o regime não possui direito a férias (temFerias = false), os limites numéricos de férias e abono devem permanecer nulos ou zerados.",
            "PCK_PARAMETROS_CONTA.MAIN_PRE",
            "IF v_row_new.tem_ferias = 'N' AND v_row_new.tot_dias_fer IS NOT NULL THEN pack_hades.trata_erro(1811); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-01812",
            ParametroContagemFeriasRuleSet.SLOT_FERIAS_DESABILITADAS_SEM_LIMITES,
            "maxDiasVendFer",
            "Quando o regime não possui direito a férias, os limites de venda de abono pecuniário não podem ser informados.",
            "PCK_PARAMETROS_CONTA.MAIN_PRE",
            "IF v_row_new.tem_ferias = 'N' AND (v_row_new.max_dias_vend_fer IS NOT NULL OR v_row_new.min_dias_pvend_fer IS NOT NULL) THEN pack_hades.trata_erro(1812); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-01813",
            ParametroContagemFeriasRuleSet.SLOT_LICENCA_ESPECIAL_PROGRESSAO_COERENCIA,
            "anosLicEsp",
            "Inconsistência nos parâmetros de Licença Especial, Progressão Funcional, Tempo de Serviço ou Sexta-Parte.",
            "PCK_PARAMETROS_CONTA.MAIN_PRE",
            "IF v_row_new.tem_lic_esp = 'S' AND (v_row_new.anos_lic_esp IS NULL OR v_row_new.meses_lic_esp_per IS NULL) THEN pack_hades.trata_erro(1813); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-01814",
            ParametroContagemFeriasRuleSet.SLOT_LICENCA_ESPECIAL_PROGRESSAO_COERENCIA,
            "abonoDirGozoLicEsp",
            "Venda de licença especial habilitada exige modalidade de abono válida ('D', 'G' ou 'N') e fração/limites coerentes.",
            "PCK_PARAMETROS_CONTA.MAIN_PRE",
            "IF v_row_new.vend_lic_esp = 'S' AND v_row_new.abono_dir_gozo_lic_esp NOT IN ('D','G','N') THEN pack_hades.trata_erro(1814); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-01815",
            ParametroContagemFeriasRuleSet.SLOT_LICENCA_ESPECIAL_PROGRESSAO_COERENCIA,
            "minDiasPvendLicEsp",
            "Quando a venda de licença especial está desabilitada, os limites mínimo e máximo de venda devem permanecer nulos ou zerados.",
            "PCK_PARAMETROS_CONTA.MAIN_PRE",
            "IF v_row_new.vend_lic_esp = 'N' AND (v_row_new.min_dias_pvend_lic_esp > 0 OR v_row_new.max_dias_vend_lic_esp > 0) THEN pack_hades.trata_erro(1815); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-01816",
            ParametroContagemFeriasRuleSet.SLOT_LICENCA_ESPECIAL_PROGRESSAO_COERENCIA,
            "minDiasPcontDobLicEsp",
            "Contagem em dobro de licença especial exige limites mínimo e máximo de dias coerentes (0 <= min <= max).",
            "PCK_PARAMETROS_CONTA.MAIN_PRE",
            "IF v_row_new.cont_dobro_lic_esp = 'S' AND (v_row_new.min_dias_pcont_dob_lic_esp IS NULL OR v_row_new.max_dias_cont_dob_lic_esp IS NULL) THEN pack_hades.trata_erro(1816); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-01817",
            ParametroContagemFeriasRuleSet.SLOT_LICENCA_ESPECIAL_PROGRESSAO_COERENCIA,
            "temLicEsp",
            "Quando o regime não possui direito a licença especial, os parâmetros de anos, meses, venda e contagem em dobro devem permanecer desabilitados.",
            "PCK_PARAMETROS_CONTA.MAIN_PRE",
            "IF v_row_new.tem_lic_esp = 'N' AND (v_row_new.anos_lic_esp > 0 OR v_row_new.meses_lic_esp_per > 0) THEN pack_hades.trata_erro(1817); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-01818",
            ParametroContagemFeriasRuleSet.SLOT_LICENCA_ESPECIAL_PROGRESSAO_COERENCIA,
            "anosProgressao",
            "Coerência entre o indicador de direito a progressão funcional (temProgressao) e o interstício em anos (anosProgressao).",
            "PCK_PARAMETROS_CONTA.MAIN_PRE",
            "IF (v_row_new.tem_progressao = 'S') <> (v_row_new.anos_progressao > 0) THEN pack_hades.trata_erro(1818); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-01819",
            ParametroContagemFeriasRuleSet.SLOT_LICENCA_ESPECIAL_PROGRESSAO_COERENCIA,
            "anos1TempoServ",
            "Coerência entre o indicador de adicional por tempo de serviço (temTempoServ) e o interstício em anos (anos1TempoServ).",
            "PCK_PARAMETROS_CONTA.MAIN_PRE",
            "IF (v_row_new.tem_tempo_serv = 'S') <> (v_row_new.anos_1_tempo_serv > 0) THEN pack_hades.trata_erro(1819); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-01820",
            ParametroContagemFeriasRuleSet.SLOT_LICENCA_ESPECIAL_PROGRESSAO_COERENCIA,
            "anosSextaParte",
            "Coerência entre o indicador de sexta-parte (temSextaParte) e o interstício em anos (anosSextaParte).",
            "PCK_PARAMETROS_CONTA.MAIN_PRE",
            "IF (v_row_new.tem_sexta_parte = 'S') <> (v_row_new.anos_sexta_parte > 0) THEN pack_hades.trata_erro(1820); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-01850",
            ParametroContagemFeriasRuleSet.SLOT_CONCOMITANCIA_VIGENCIA_ESCOPO,
            "dtini",
            "Conflito de vigência ativa na mesma chave de parâmetros de contagem ou data inicial do período aquisitivo incompatível com exercício/vacância do vínculo.",
            "PCK_PARAMETROS_CONTA.MAIN_POS / PCK_PERAQFER.MAIN_PRE",
            "IF v_concomitantes > 0 OR p_peraqfer.dtini < v_vinculo.dtexerc THEN pack_hades.trata_erro(1850); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-01851",
            ParametroContagemFeriasRuleSet.SLOT_PARCELAMENTO_COERENCIA,
            "dtfim",
            "Máximo de parcelas não pode superar o total de dias de férias ou cronologia de datas do período aquisitivo inválida (dtini <= dtfim <= dtPrescricao).",
            "PCK_PARAMETROS_CONTA.MAIN_PRE / PCK_PERAQFER.MAIN_PRE",
            "IF v_row_new.max_per_fer > v_row_new.tot_dias_fer OR p_peraqfer.dtfim < p_peraqfer.dtini THEN pack_hades.trata_erro(1851); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-01852",
            ParametroContagemFeriasRuleSet.SLOT_PARCELAMENTO_COERENCIA,
            "maxDiasPerFer",
            "Máximo de dias por parcela não pode superar o total de dias de férias.",
            "PCK_PARAMETROS_CONTA.MAIN_PRE",
            "IF v_row_new.max_dias_per_fer > v_row_new.tot_dias_fer THEN pack_hades.trata_erro(1852); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-01853",
            ParametroContagemFeriasRuleSet.SLOT_CONTAGEM_DOBRO_FERIAS_COERENCIA,
            "minDiasPeraqfer",
            "Quando a contagem em dobro de férias está habilitada, o mínimo de dias (minDiasPeraqfer) é obrigatório.",
            "PCK_PARAMETROS_CONTA.MAIN_PRE",
            "IF v_row_new.cont_dobro_ferias = 'S' AND v_row_new.min_dias_peraqfer IS NULL THEN pack_hades.trata_erro(1853); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-01854",
            ParametroContagemFeriasRuleSet.SLOT_CONTAGEM_DOBRO_FERIAS_COERENCIA,
            "maxDiasPeraqfer",
            "Quando a contagem em dobro de férias está habilitada, o máximo de dias (maxDiasPeraqfer) é obrigatório.",
            "PCK_PARAMETROS_CONTA.MAIN_PRE",
            "IF v_row_new.cont_dobro_ferias = 'S' AND v_row_new.max_dias_peraqfer IS NULL THEN pack_hades.trata_erro(1854); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-01855",
            ParametroContagemFeriasRuleSet.SLOT_CONTAGEM_DOBRO_FERIAS_COERENCIA,
            "minDiasPeraqfer",
            "Quando a contagem em dobro de férias está desabilitada, o mínimo de dias para contagem em dobro não deve ser informado.",
            "PCK_PARAMETROS_CONTA.MAIN_PRE",
            "IF v_row_new.cont_dobro_ferias = 'N' AND v_row_new.min_dias_peraqfer IS NOT NULL THEN pack_hades.trata_erro(1855); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-01856",
            ParametroContagemFeriasRuleSet.SLOT_CONTAGEM_DOBRO_FERIAS_COERENCIA,
            "maxDiasPeraqfer",
            "Quando a contagem em dobro de férias está desabilitada, o máximo de dias para contagem em dobro não deve ser informado.",
            "PCK_PARAMETROS_CONTA.MAIN_PRE",
            "IF v_row_new.cont_dobro_ferias = 'N' AND v_row_new.max_dias_peraqfer IS NOT NULL THEN pack_hades.trata_erro(1856); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-00604",
            PeriodoAquisitivoFeriasRuleSet.SLOT_OVERRIDES_CONTAGEM_DOBRO,
            "contDobroFerias",
            "Configuração de contagem em dobro de férias inconsistente com os limites mínimo e máximo de dias do período aquisitivo.",
            "PCK_PARAMETROS_CONTA.MAIN_PRE / PCK_PERAQFER.MAIN_PRE",
            "IF v_row_new.cont_dobro_ferias = 'S' AND (v_row_new.min_dias_peraqfer IS NULL OR v_row_new.max_dias_peraqfer IS NULL) THEN pack_hades.trata_erro(604); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-00605",
            PeriodoAquisitivoFeriasRuleSet.SLOT_OVERRIDES_CONTAGEM_DOBRO,
            "minDiasPeraqfer",
            "Limites de dias para contagem em dobro de férias não podem ser negativos.",
            "PCK_PARAMETROS_CONTA.MAIN_PRE / PCK_PERAQFER.MAIN_PRE",
            "IF v_row_new.min_dias_peraqfer < 0 OR v_row_new.max_dias_peraqfer < 0 THEN pack_hades.trata_erro(605); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-00606",
            ParametroContagemFeriasRuleSet.SLOT_CONTAGEM_DOBRO_FERIAS_COERENCIA,
            "minDiasPeraqfer",
            "O mínimo de dias para contagem em dobro não pode ser superior ao máximo de dias para contagem em dobro.",
            "PCK_PARAMETROS_CONTA.MAIN_PRE / PCK_PERAQFER.MAIN_PRE",
            "IF v_row_new.min_dias_peraqfer > v_row_new.max_dias_peraqfer THEN pack_hades.trata_erro(606); END IF;"));

    // PCK_PERAQFER
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-00099",
            PeriodoAquisitivoFeriasRuleSet.SLOT_BLOQUEIO_AVERBACAO_E_CRITERIO_PRESCRICAO,
            "geradoPorAverbacao",
            "Período aquisitivo gerado por averbação bloqueado para alteração manual ou critério de prescrição/multa fora do domínio permitido.",
            "PCK_PERAQFER.MAIN_PRE",
            "IF v_row_old.gerado_por_averbacao = 'S' THEN pack_hades.trata_erro(99); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-00600",
            PeriodoAquisitivoFeriasRuleSet.SLOT_DIREITO_TETO_E_REDUCAO_FALTAS,
            "totDiasDireito",
            "Total de dias de direito do período aquisitivo excede o limite do parâmetro/escala de redução por faltas ou é inferior aos dias já consumidos.",
            "PCK_PERAQFER.VALID_PERAQFER",
            "IF v_row_new.tot_dias_direito > v_max_direito OR v_row_new.tot_dias_direito < v_dias_consumidos THEN pack_hades.trata_erro(600); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-00601",
            PeriodoAquisitivoFeriasRuleSet.SLOT_SOBREPOSICAO_E_ACUMULO_PERIODOS,
            "dtini",
            "Sobreposição de datas ou duplicidade de chave entre períodos aquisitivos ativos do mesmo vínculo funcional.",
            "PCK_PERAQFER.MAIN_POS",
            "IF v_sobreposicao_pa > 0 THEN pack_hades.trata_erro(601); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-00603",
            PeriodoAquisitivoFeriasRuleSet.SLOT_BLOQUEIO_AVERBACAO_E_CRITERIO_PRESCRICAO,
            "geradoPorAverbacao",
            "Período aquisitivo gerado por averbação não pode sofrer manutenção manual direta nos campos nucleares.",
            "PCK_PERAQFER.MAIN_PRE",
            "IF v_row_new.gerado_por_averbacao = 'S' THEN pack_hades.trata_erro(603); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-00612",
            PeriodoAquisitivoFeriasRuleSet.SLOT_ACUMULO_E_FALTAS_LIMITES_TEMPLATE,
            "faltasApuradas",
            "Quantidade de períodos aquisitivos acumulados em aberto ou faltas apuradas excede o teto permitido pelo órgão.",
            "PCK_PERAQFER.VALID_PERAQFER",
            "IF v_periodos_abertos > v_max_acumulo THEN pack_hades.trata_erro(612); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-00615",
            PeriodoAquisitivoFeriasRuleSet.SLOT_PRESCRICAO_PARCELAS_ATIVAS,
            "dtPrescricao",
            "Existem parcelas de gozo de férias programadas ou usufruídas após a data de prescrição do período aquisitivo.",
            "PCK_PERAQFER.VERIFICA_DT_PRESCR",
            "IF v_parcelas_apos_prescr > 0 THEN pack_hades.trata_erro(615); END IF;"));

    // PCK_FERIAS & PCK_ERG_ABONOS_FERIAS
    register(
        map,
        new RuleErrorDescriptor(
            "VALID_FERIAS",
            FeriasGozoRuleSet.SLOT_CRONOLOGIA_E_INTERRUPCAO_VALIDA,
            "dtini",
            "Cronologia da parcela de férias inválida, fora da vigência de exercício/vacância do vínculo ou em conflito com frequência normal/afastamento.",
            "PCK_FERIAS.VALID_FERIAS",
            "IF p_ferias.dtprevfim < p_ferias.dtini OR v_conflito_freq_afast > 0 THEN pack_hades.trata_erro('VALID_FERIAS'); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-00502",
            FeriasGozoRuleSet.SLOT_SUBSTITUTO_FUNCIONAL_VALIDO,
            "numfuncSubstituto",
            "O servidor titular da parcela de férias não pode ser designado como seu próprio substituto.",
            "PCK_FERIAS.MAIN_PRE",
            "IF v_row_new.numfunc_subst = v_row_new.numfunc AND v_row_new.numvinc_subst = v_row_new.numvinc THEN pack_hades.trata_erro(502); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-00503",
            FeriasGozoRuleSet.SLOT_SUBSTITUTO_FUNCIONAL_VALIDO,
            "numfuncSubstituto",
            "Substituto funcional inválido: número funcional e vínculo do substituto devem ser informados em conjunto e pertencer a vínculo ativo no órgão.",
            "PCK_FERIAS.MAIN_PRE",
            "IF (v_row_new.numfunc_subst IS NULL) <> (v_row_new.numvinc_subst IS NULL) OR NOT vinculo_subst_valido THEN pack_hades.trata_erro(503); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-00504",
            FeriasGozoRuleSet.SLOT_SUBSTITUTO_FUNCIONAL_VALIDO,
            "numfuncSubstituto",
            "Existe substituição ativa vinculada ao período que impede o encolhimento ou exclusão da parcela de férias.",
            "PCK_FERIAS.MAIN_PRE",
            "IF v_substituicao_ativa_conflito > 0 THEN pack_hades.trata_erro(504); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-00506",
            FeriasGozoRuleSet.SLOT_DIREITO_FERIAS_HABILITADO,
            "periodoAquisitivoId",
            "A categoria/regime parametrizado não possui direito a férias habilitado ou o período aquisitivo encontra-se cancelado/prescrito.",
            "PCK_FERIAS.MAIN_PRE",
            "IF v_param.tem_ferias = 'N' OR v_pa.tot_dias_direito <= 0 THEN pack_hades.trata_erro(506); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-00507",
            FeriasGozoRuleSet.SLOT_BENEFICIOS_FINANCEIROS_ELEGIBILIDADE,
            "adiantamento13",
            "Adiantamento de 13º salário nas férias não permitido pelos parâmetros efetivos do período aquisitivo.",
            "PCK_FERIAS.MAIN_PRE",
            "IF v_row_new.adiant_13 = 'S' AND v_eff.adiant_13_fer = 'N' THEN pack_hades.trata_erro(507); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-00508",
            FeriasGozoRuleSet.SLOT_BENEFICIOS_FINANCEIROS_ELEGIBILIDADE,
            "adiantamentoSalario",
            "Adiantamento salarial nas férias não permitido pelos parâmetros efetivos do período aquisitivo.",
            "PCK_FERIAS.MAIN_PRE",
            "IF v_row_new.adiant_sal = 'S' AND v_eff.adiant_sal_fer = 'N' THEN pack_hades.trata_erro(508); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-00509",
            FeriasGozoRuleSet.SLOT_BENEFICIOS_FINANCEIROS_ELEGIBILIDADE,
            "emprestimoFerias",
            "Empréstimo de férias não permitido pelos parâmetros efetivos do período aquisitivo.",
            "PCK_FERIAS.MAIN_PRE",
            "IF v_row_new.emprest_fer = 'S' AND v_eff.emprest_fer = 'N' THEN pack_hades.trata_erro(509); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-00510",
            FeriasGozoRuleSet.SLOT_BENEFICIOS_FINANCEIROS_ELEGIBILIDADE,
            "abonoConstitucional",
            "Pagamento do terço constitucional de férias não permitido pelos parâmetros efetivos do período aquisitivo.",
            "PCK_FERIAS.MAIN_PRE",
            "IF v_row_new.abono_const = 'S' AND v_eff.abono_const_fer = 'N' THEN pack_hades.trata_erro(510); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-00512",
            FeriasGozoRuleSet.SLOT_BENEFICIOS_FINANCEIROS_ELEGIBILIDADE,
            "abonoConstitucional",
            "O terço constitucional de férias já foi concedido em outra parcela deste mesmo período aquisitivo.",
            "PCK_FERIAS.MAIN_PRE",
            "IF v_row_new.abono_const = 'S' AND v_outras_parcelas_abono_const > 0 THEN pack_hades.trata_erro(512); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-00513",
            FeriasGozoRuleSet.SLOT_BENEFICIOS_FINANCEIROS_ELEGIBILIDADE,
            "adiantamento13",
            "O adiantamento de 13º salário já foi concedido em outra parcela de férias no mesmo ano civil.",
            "PCK_FERIAS.MAIN_PRE",
            "IF v_row_new.adiant_13 = 'S' AND v_outras_parcelas_adiant13_ano > 0 THEN pack_hades.trata_erro(513); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-00514",
            FeriasGozoRuleSet.SLOT_CONCOMITANCIA_E_SALDO_FRACIONAMENTO,
            "dtini",
            "Conflito de datas ou ordem de parcela duplicada com outra parcela de férias ativa do vínculo.",
            "PCK_FERIAS.MAIN_POS",
            "IF v_concomitancia_ferias > 0 OR v_ordem_duplicada > 0 THEN pack_hades.trata_erro(514); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-00516",
            FeriasGozoRuleSet.SLOT_GOZO_ANTES_TERMINO_PA_POR_ORDEM,
            "dtini",
            "A data de início do gozo de férias não pode anteceder o início do período aquisitivo quando o gozo antecipado está vedado.",
            "PCK_FERIAS.MAIN_PRE",
            "IF v_row_new.dtini < v_pa.dtini AND v_eff.permite_gozo_antes_pa_fer = 'N' THEN pack_hades.trata_erro(516); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-00517",
            FeriasGozoRuleSet.SLOT_GOZO_ANTES_TERMINO_PA_POR_ORDEM,
            "dtini",
            "Não é permitido gozar férias antes do término de período aquisitivo incompleto (> 3º período do vínculo).",
            "PCK_FERIAS.MAIN_PRE",
            "IF v_ordem_pa > 3 AND v_row_new.dtini <= v_pa.dtfim AND v_eff.permite_fer_per_inc = 'N' THEN pack_hades.trata_erro(517); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-00518",
            FeriasGozoRuleSet.SLOT_CONCOMITANCIA_E_SALDO_FRACIONAMENTO,
            "diasGozo",
            "A duração da parcela de férias supera o máximo de dias por parcela permitido pelos parâmetros efetivos.",
            "PCK_FERIAS.MAIN_PRE",
            "IF v_row_new.dias_gozo > v_eff.max_dias_per_fer THEN pack_hades.trata_erro(518); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-00519",
            FeriasGozoRuleSet.SLOT_FRACIONAMENTO_LIMITES_TEMPLATE,
            "diasGozo",
            "A duração da parcela de férias é inferior ao mínimo de dias por parcela exigido pelos parâmetros efetivos.",
            "PCK_FERIAS.MAIN_PRE",
            "IF v_row_new.dias_gozo < v_eff.min_dias_per_fer THEN pack_hades.trata_erro(519); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-00520",
            FeriasGozoRuleSet.SLOT_CONCOMITANCIA_E_SALDO_FRACIONAMENTO,
            "ordemParcela",
            "A quantidade de parcelas de férias supera o máximo de parcelas permitido para o período aquisitivo.",
            "PCK_FERIAS.MAIN_PRE",
            "IF v_total_parcelas_pa > v_eff.max_per_fer THEN pack_hades.trata_erro(520); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-00521",
            FeriasGozoRuleSet.SLOT_BENEFICIOS_FINANCEIROS_ELEGIBILIDADE,
            "abonoPecuniario",
            "Não é permitido lançar abono pecuniário em mais de uma parcela do mesmo período aquisitivo quando permiteLancConcPa está desabilitado.",
            "PCK_FERIAS.MAIN_PRE",
            "IF v_row_new.abono_pecuniario = 'S' AND v_eff.permite_lanc_conc_pa = 'N' AND v_outros_abonos_pa > 0 THEN pack_hades.trata_erro(521); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-00522",
            FeriasGozoRuleSet.SLOT_ABONO_PECUNIARIO_ELEGIBILIDADE_E_LIMITES,
            "diasAbonoVendidos",
            "Venda de abono pecuniário vedada para o período aquisitivo ou quantidade/posição de dias vendidos fora dos limites parametrizados.",
            "PCK_FERIAS.MAIN_PRE / PCK_ERG_ABONOS_FERIAS.MAIN_PRE",
            "IF NOT abono_pecuniario_limites_validos THEN pack_hades.trata_erro(522); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-03311",
            FeriasGozoRuleSet.SLOT_ABONO_PECUNIARIO_ELEGIBILIDADE_E_LIMITES,
            "iniFimAbono",
            "Posição do abono pecuniário deve ser 'I' (início das férias) ou 'F' (final das férias) quando abonoPecuniario = true.",
            "PCK_ERG_ABONOS_FERIAS.MAIN_PRE",
            "IF v_row_new.abono_pecuniario = 'S' AND v_row_new.ini_fim_abono NOT IN ('I','F') THEN pack_hades.trata_erro(3311); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-03312",
            FeriasGozoRuleSet.SLOT_ABONO_PECUNIARIO_ELEGIBILIDADE_E_LIMITES,
            "diasAbonoVendidos",
            "Quantidade de dias vendidos em abono pecuniário inferior ao mínimo permitido pelos parâmetros efetivos (minDiasPvendFer).",
            "PCK_ERG_ABONOS_FERIAS.MAIN_PRE",
            "IF v_row_new.dias_abono_vendidos < v_eff.min_dias_pvend_fer THEN pack_hades.trata_erro(3312); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-03313",
            FeriasGozoRuleSet.SLOT_ABONO_PECUNIARIO_ELEGIBILIDADE_E_LIMITES,
            "diasAbonoVendidos",
            "Quantidade de dias vendidos em abono pecuniário superior ao máximo permitido pelos parâmetros efetivos (maxDiasVendFer).",
            "PCK_ERG_ABONOS_FERIAS.MAIN_PRE",
            "IF v_row_new.dias_abono_vendidos > v_eff.max_dias_vend_fer THEN pack_hades.trata_erro(3313); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-03314",
            FeriasGozoRuleSet.SLOT_ABONO_PECUNIARIO_ELEGIBILIDADE_E_LIMITES,
            "diasAbonoVendidos",
            "Quantidade de dias vendidos em abono pecuniário supera o teto da fração legal incidente sobre direito ou gozo (numDiasAbonoFer/denDiasAbonoFer).",
            "PCK_ERG_ABONOS_FERIAS.MAIN_PRE",
            "IF v_row_new.dias_abono_vendidos > v_max_fracao_abono THEN pack_hades.trata_erro(3314); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-03315",
            FeriasGozoRuleSet.SLOT_ABONO_PECUNIARIO_ELEGIBILIDADE_E_LIMITES,
            "dtVendaAbono",
            "Data de venda do abono pecuniário posterior ao início do gozo ou data de pagamento anterior à data de venda.",
            "PCK_ERG_ABONOS_FERIAS.MAIN_PRE",
            "IF v_row_new.dt_venda_abono > v_row_new.dtini OR v_row_new.dt_pagto_abono < v_row_new.dt_venda_abono THEN pack_hades.trata_erro(3315); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-00523",
            FeriasGozoRuleSet.SLOT_CONCOMITANCIA_E_SALDO_FRACIONAMENTO,
            "diasGozo",
            "A soma dos dias de gozo, abono pecuniário e contagem em dobro excede o total de dias de direito do período aquisitivo.",
            "PCK_FERIAS.MAIN_POS",
            "IF v_total_dias_consumidos_pa > v_pa.tot_dias_direito THEN pack_hades.trata_erro(523); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-00525",
            FeriasGozoRuleSet.SLOT_CONCOMITANCIA_E_SALDO_FRACIONAMENTO,
            "diasGozo",
            "O saldo remanescente do período aquisitivo não poderá ser esgotado nas parcelas restantes respeitando o teto máximo de dias por parcela.",
            "PCK_FERIAS.MAIN_POS",
            "IF v_saldo_remanescente > v_parcelas_restantes * v_eff.max_dias_per_fer THEN pack_hades.trata_erro(525); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-00526",
            FeriasGozoRuleSet.SLOT_CONCOMITANCIA_E_SALDO_FRACIONAMENTO,
            "diasGozo",
            "O saldo residual do período aquisitivo é inferior ao mínimo de dias permitido para uma futura parcela ou inviabiliza a parcela principal mínima.",
            "PCK_FERIAS.MAIN_POS",
            "IF v_saldo_remanescente < v_eff.min_dias_per_fer OR (NOT has_principal AND v_saldo_remanescente < v_eff.min_dias_per_princ_fer) THEN pack_hades.trata_erro(526); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-00527",
            FeriasGozoRuleSet.SLOT_GOZO_ANTES_TERMINO_PA_POR_ORDEM,
            "dtini",
            "Não é permitido gozar férias antes do término do 1º período aquisitivo do vínculo funcional.",
            "PCK_FERIAS.MAIN_PRE",
            "IF v_ordem_pa = 1 AND v_row_new.dtini <= v_pa.dtfim AND v_eff.permite_fer_prim_per = 'N' THEN pack_hades.trata_erro(527); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-00528",
            FeriasGozoRuleSet.SLOT_GOZO_ANTES_TERMINO_PA_POR_ORDEM,
            "dtini",
            "Não é permitido gozar férias antes do término do 2º período aquisitivo do vínculo funcional.",
            "PCK_FERIAS.MAIN_PRE",
            "IF v_ordem_pa = 2 AND v_row_new.dtini <= v_pa.dtfim AND v_eff.permite_fer_segun_per = 'N' THEN pack_hades.trata_erro(528); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-00530",
            FeriasGozoRuleSet.SLOT_GOZO_ANTES_TERMINO_PA_POR_ORDEM,
            "dtini",
            "Não é permitido gozar férias antes do término do 3º período aquisitivo do vínculo funcional.",
            "PCK_FERIAS.MAIN_PRE",
            "IF v_ordem_pa = 3 AND v_row_new.dtini <= v_pa.dtfim AND v_eff.permite_fer_terc_per = 'N' THEN pack_hades.trata_erro(530); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-00531",
            FeriasGozoRuleSet.SLOT_CONCOMITANCIA_E_SALDO_FRACIONAMENTO,
            "ordemParcela",
            "O período aquisitivo não pode ser integralmente quitado em quantidade de parcelas inferior ao mínimo exigido (minPerFer).",
            "PCK_FERIAS.MAIN_POS",
            "IF v_saldo_remanescente = 0 AND v_total_parcelas_pa < v_eff.min_per_fer THEN pack_hades.trata_erro(531); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-00532",
            FeriasGozoRuleSet.SLOT_CONCOMITANCIA_E_SALDO_FRACIONAMENTO,
            "diasGozo",
            "Ao atingir o número máximo de parcelas permitido (maxPerFer), não pode restar saldo positivo de dias no período aquisitivo.",
            "PCK_FERIAS.MAIN_POS",
            "IF v_total_parcelas_pa >= v_eff.max_per_fer AND v_saldo_remanescente > 0 THEN pack_hades.trata_erro(532); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "ERG-01880",
            FeriasGozoRuleSet.SLOT_GOZO_APOS_PRESCRICAO_PA,
            "dtini",
            "Gozo de férias após a data de prescrição do período aquisitivo não é permitido pelos parâmetros efetivos.",
            "PCK_FERIAS.MAIN_PRE",
            "IF v_viola_dt_prescr AND v_eff.permite_gozo_pos_prescr = 'N' THEN pack_hades.trata_erro(1880); END IF;"));

    // Códigos de customização de Órgão 2 (CERG-*)
    register(
        map,
        new RuleErrorDescriptor(
            "CERG-PARAM-PRE-001",
            ParametroContagemFeriasRuleSet.SLOT_HOOK_PRE_VALIDACAO,
            "documentoLegalId",
            "Violação de regra customizada pré-validação em parâmetros de contagem.",
            "EPB__PARAMETROS_CONTA",
            "EPB__PARAMETROS_CONTA(v_row_new, v_mens);"));
    register(
        map,
        new RuleErrorDescriptor(
            "CERG-PARAM-201",
            ParametroContagemFeriasRuleSet.SLOT_FERIAS_LIMITES_DIAS_TEMPLATE,
            "totDiasFer",
            "Órgão 2 exige direito de férias entre 20 e 30 dias e no máximo 2 parcelas.",
            "PARAMETROS_CONTA.TEMPLATE",
            "Template ergon.parametros-conta:ferias-bounds@1.0 parametrizado para Órgão 2."));
    register(
        map,
        new RuleErrorDescriptor(
            "CERG-PARAM-301",
            ParametroContagemFeriasRuleSet.SLOT_HOOK_PRE_VALIDACAO,
            "documentoLegalId",
            "Órgão 2 exige portaria/documento legal vinculado ao parâmetro de contagem de férias.",
            "EPB__PARAMETROS_CONTA",
            "IF v_row_new.tem_ferias = 'S' AND v_row_new.documento_legal_id IS NULL THEN pack_hades.trata_erro('CERG-PARAM-301'); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "CERG-PARAM-401",
            ParametroContagemFeriasRuleSet.SLOT_ABONO_PECUNIARIO_FRACAO_LIMITES,
            "abonoDirGozo",
            "Órgão 2 veda parametrização de abono pecuniário (exige abonoDirGozo = 'N').",
            "PCK_PARAMETROS_CONTA.MAIN_PRE",
            "IF v_row_new.abono_dir_gozo <> 'N' THEN pack_hades.trata_erro('CERG-PARAM-401'); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "CERG-PARAM-POST-001",
            ParametroContagemFeriasRuleSet.SLOT_HOOK_POS_VALIDACAO,
            "dtini",
            "Violação de regra customizada pós-validação em parâmetros de contagem.",
            "EPA__PARAMETROS_CONTA",
            "EPA__PARAMETROS_CONTA(v_row_new, v_mens);"));

    register(
        map,
        new RuleErrorDescriptor(
            "CERG-PA-PRE-001",
            PeriodoAquisitivoFeriasRuleSet.SLOT_HOOK_PRE_VALIDACAO,
            "documentoLegalId",
            "Violação de regra customizada pré-validação no período aquisitivo.",
            "EPB__PERAQFER",
            "EPB__PERAQFER(v_row_new, v_mens);"));
    register(
        map,
        new RuleErrorDescriptor(
            "CERG-PA-201",
            PeriodoAquisitivoFeriasRuleSet.SLOT_ACUMULO_E_FALTAS_LIMITES_TEMPLATE,
            "faltasApuradas",
            "Órgão 2 permite no máximo 1 período aquisitivo acumulado em aberto e até 20 faltas.",
            "PERAQFER.TEMPLATE",
            "Template ergon.peraqfer:accumulation-and-reduction-bounds@1.0 parametrizado para Órgão 2."));
    register(
        map,
        new RuleErrorDescriptor(
            "CERG-PA-301",
            PeriodoAquisitivoFeriasRuleSet.SLOT_HOOK_PRE_VALIDACAO,
            "documentoLegalId",
            "Órgão 2 exige documento legal formal para conversão em dobro no período aquisitivo.",
            "EPB__PERAQFER",
            "IF v_row_new.dias_convertidos_dobro > 0 AND v_row_new.documento_legal_id IS NULL THEN pack_hades.trata_erro('CERG-PA-301'); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "CERG-PA-401",
            PeriodoAquisitivoFeriasRuleSet.SLOT_PRESCRICAO_PARCELAS_ATIVAS,
            "dtPrescricao",
            "Órgão 2 exige documento legal expresso para autorizar parcelas após a data de prescrição.",
            "EP__VERIFICA_DT_PRESCR",
            "IF v_parcelas_apos_prescr > 0 AND v_row_new.documento_legal_id IS NULL THEN pack_hades.trata_erro('CERG-PA-401'); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "CERG-PA-POST-001",
            PeriodoAquisitivoFeriasRuleSet.SLOT_HOOK_POS_VALIDACAO,
            "dtini",
            "Violação de regra customizada pós-validação no período aquisitivo.",
            "EPA__PERAQFER",
            "EPA__PERAQFER(v_row_new, v_mens);"));

    register(
        map,
        new RuleErrorDescriptor(
            "CERG-FER-PRE-001",
            FeriasGozoRuleSet.SLOT_HOOK_PRE_VALIDACAO,
            "documentoLegalId",
            "Violação de regra customizada pré-validação na concessão de férias.",
            "EPB__FERIAS",
            "EPB__FERIAS(v_row_new, v_mens);"));
    register(
        map,
        new RuleErrorDescriptor(
            "CERG-FER-201",
            FeriasGozoRuleSet.SLOT_FRACIONAMENTO_LIMITES_TEMPLATE,
            "diasGozo",
            "Órgão 2 exige máximo de 2 parcelas, mínimo de 10 dias por parcela, 15 dias na parcela principal e antecedência mínima de 15 dias.",
            "FERIAS.TEMPLATE",
            "Template ergon.ferias:fracionamento-and-abono-bounds@1.0 parametrizado para Órgão 2."));
    register(
        map,
        new RuleErrorDescriptor(
            "CERG-FER-301",
            FeriasGozoRuleSet.SLOT_HOOK_PRE_VALIDACAO,
            "documentoLegalId",
            "Órgão 2 exige documento legal formal para concessão de férias com abono pecuniário ou adiantamento de 13º salário.",
            "EPB__FERIAS",
            "IF (v_row_new.abono_pecuniario = 'S' OR v_row_new.adiant_13 = 'S') AND v_row_new.documento_legal_id IS NULL THEN pack_hades.trata_erro('CERG-FER-301'); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "CERG-FER-401",
            FeriasGozoRuleSet.SLOT_ABONO_PECUNIARIO_ELEGIBILIDADE_E_LIMITES,
            "iniFimAbono",
            "Órgão 2 permite abono pecuniário exclusivamente na 1ª parcela e ao final do gozo (iniFimAbono = 'F').",
            "EP__TRATA_ABONO",
            "IF v_row_new.abono_pecuniario = 'S' AND (v_row_new.ordem_parcela <> 1 OR v_row_new.ini_fim_abono <> 'F') THEN pack_hades.trata_erro('CERG-FER-401'); END IF;"));
    register(
        map,
        new RuleErrorDescriptor(
            "CERG-FER-POST-001",
            FeriasGozoRuleSet.SLOT_HOOK_POS_VALIDACAO,
            "dtini",
            "Violação de regra customizada pós-validação na concessão de férias.",
            "EPA__FERIAS",
            "EPA__FERIAS(v_row_new, v_mens);"));
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
        .orElse("Violação determinística da política de regras de negócio Ergon (" + reasonCode + ").");
  }
}
