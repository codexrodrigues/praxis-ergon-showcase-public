package br.com.praxis.ergonshowcase.rules;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Catálogo oficial de códigos de razão e mensagens de domínio para as famílias de regras
 * estatutárias e operacionais de frequência e afastamentos do Ergon RH Público:
 * <ul>
 *   <li>{@code RN-013} ({@code PCK_REGRAS_FREQ.MAIN_PRE} e {@code PCK_REGRAS_FREQ.MAIN_POS});
 *   <li>{@code PCK_FREQUENCIAS} ({@link FrequenciaLancamentoRuleSet});
 *   <li>{@code PCK_LIC_AFAST} / {@code ERG_PM_GERALICAFAST} ({@link AfastamentoConcessaoRuleSet});
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
