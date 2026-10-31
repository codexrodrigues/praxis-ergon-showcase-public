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
      String plsqlLineage) {}

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
