package br.com.praxis.ergonshowcase.rules;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.praxisplatform.rules.contract.*;

/** Guardas operacionais do host, com uma decisão e um descritor por regra. */
public enum OperationalRuleSet {
  CAMPO_SELECT("CERG-09051", "optionTabela", "Campo customizado SELECT exige uma tabela geral de opções.",
      "POLITICA_SHOWCASE.CAMPO_SELECT",
      List.of("draft.fieldType", "draft.optionTabela"),
      """
      {"or":[{"!=":[{"var":"draft.fieldType"},"SELECT"]},{"!":[{"or":[{"==":[{"var":"draft.optionTabela"},null]},{"==":[{"var":"draft.optionTabela"},""]}]}]}]}
      """),

  CAMPO_ORDEM("CERG-09002", "fieldOrder", "A ordem no grupo deve estar entre 1 e 999.",
      "POLITICA_SHOWCASE.CAMPO_ORDEM",
      List.of("draft.ordem"),
      """
      {"or":[{"==":[{"var":"draft.ordem"},null]},{"and":[{">=":[{"var":"draft.ordem"},1]},{"<=":[{"var":"draft.ordem"},999]}]}]}
      """),

  FERIAS_VINCULO("CERG-09003", "periodoAquisitivoId", "Período aquisitivo deve pertencer ao vínculo da parcela.",
      "POLITICA_SHOWCASE.FERIAS_VINCULO",
      List.of("periodoAquisitivo.pessoa", "draft.pessoa", "periodoAquisitivo.vinculo", "draft.vinculo"),
      """
      {"and":[{"==":[{"var":"periodoAquisitivo.pessoa"},{"var":"draft.pessoa"}]},{"==":[{"var":"periodoAquisitivo.vinculo"},{"var":"draft.vinculo"}]}]}
      """),

  PREVIA_FRACAO_ABONO("CERG-09009", "numDiasAbonoFer", "Numerador deve estar entre zero e o denominador da fração.",
      "POLITICA_SHOWCASE.PREVIA_FRACAO_ABONO",
      List.of("draft.numerador", "draft.denominador"),
      """
      {"and":[{">=":[{"var":"draft.numerador"},0]},{"<=":[{"var":"draft.numerador"},{"var":"draft.denominador"}]}]}
      """),

  LICENCA_ESTADO_EDICAO("CERG-09012", "situacao", "Use uma ação de ciclo de vida para alterar a situação.",
      "POLITICA_SHOWCASE.LICENCA_ESTADO_EDICAO",
      List.of("current.situacao", "draft.situacao"),
      """
      {"==":[{"var":"current.situacao"},{"var":"draft.situacao"}]}
      """),

  LICENCA_PERIODO_EDICAO("CERG-09013", "periodoAquisitivoLicencaId", "A parcela não pode ser transferida para outro período aquisitivo.",
      "POLITICA_SHOWCASE.LICENCA_PERIODO_EDICAO",
      List.of("current.periodo", "draft.periodo"),
      """
      {"==":[{"var":"current.periodo"},{"var":"draft.periodo"}]}
      """),

  AVERBACAO_CRIAR("CERG-09014", "diasDesaverbados", "Novo assentamento não pode nascer desaverbado ou removido.",
      "POLITICA_SHOWCASE.AVERBACAO_CRIAR",
      List.of("draft.dias", "draft.desaverbacao", "draft.remocao", "draft.motivoRemocao", "draft.motivoDesaverbacao"),
      """
      {"and":[{"==":[{"var":"draft.dias"},0]},{"==":[{"var":"draft.desaverbacao"},null]},{"==":[{"var":"draft.remocao"},null]},{"==":[{"var":"draft.motivoRemocao"},null]},{"==":[{"var":"draft.motivoDesaverbacao"},null]}]}
      """),

  AVERBACAO_ATO_EDICAO("CERG-09015", "diasDesaverbados", "Desaverbação e remoção exigem ação própria.",
      "POLITICA_SHOWCASE.AVERBACAO_ATO_EDICAO",
      List.of("current.diasDesaverbados", "draft.diasDesaverbados", "current.dataRemocao", "draft.dataRemocao", "current.dataDesaverbacao", "draft.dataDesaverbacao", "current.motivoDesaverbacao", "draft.motivoDesaverbacao", "current.motivoRemocao", "draft.motivoRemocao"),
      """
      {"and":[{"==":[{"var":"current.diasDesaverbados"},{"var":"draft.diasDesaverbados"}]},{"==":[{"var":"current.dataRemocao"},{"var":"draft.dataRemocao"}]},{"==":[{"var":"current.dataDesaverbacao"},{"var":"draft.dataDesaverbacao"}]},{"==":[{"var":"current.motivoDesaverbacao"},{"var":"draft.motivoDesaverbacao"}]},{"==":[{"var":"current.motivoRemocao"},{"var":"draft.motivoRemocao"}]}]}
      """),

  AVERBACAO_RETIFICACAO("CERG-09016", "situacao", "Campos operacionais não podem ser retificados neste estado do ciclo de vida.",
      "POLITICA_SHOWCASE.AVERBACAO_RETIFICACAO",
      List.of("current.situacao", "current.totalDias", "draft.totalDias", "current.diasFinalidade", "draft.diasFinalidade", "current.finalidade", "draft.finalidade", "current.tipoTempo", "draft.tipoTempo", "current.instituicao", "draft.instituicao", "current.dtini", "draft.dtini", "current.dtfim", "draft.dtfim"),
      """
      {"or":[{"in":[{"var":"current.situacao"},["EM_ANALISE"]]},{"and":[{"==":[{"var":"current.totalDias"},{"var":"draft.totalDias"}]},{"==":[{"var":"current.diasFinalidade"},{"var":"draft.diasFinalidade"}]},{"==":[{"var":"current.finalidade"},{"var":"draft.finalidade"}]},{"==":[{"var":"current.tipoTempo"},{"var":"draft.tipoTempo"}]},{"==":[{"var":"current.instituicao"},{"var":"draft.instituicao"}]},{"==":[{"var":"current.dtini"},{"var":"draft.dtini"}]},{"==":[{"var":"current.dtfim"},{"var":"draft.dtfim"}]}]}]}
      """),

  GOZO_CRIAR("CERG-09017", "dtfim", "Nova programação não pode nascer encerrada.",
      "POLITICA_SHOWCASE.GOZO_CRIAR",
      List.of("draft.fim"),
      """
      {"==":[{"var":"draft.fim"},null]}
      """),

  GOZO_ENCERRAMENTO_EDICAO("CERG-09018", "dtfim", "Encerramento exige ação de ciclo de vida.",
      "POLITICA_SHOWCASE.GOZO_ENCERRAMENTO_EDICAO",
      List.of("current.fim", "draft.fim"),
      """
      {"==":[{"var":"current.fim"},{"var":"draft.fim"}]}
      """),

  GOZO_RETIFICACAO("CERG-09019", "situacao", "Campos operacionais não podem ser retificados neste estado do ciclo de vida.",
      "POLITICA_SHOWCASE.GOZO_RETIFICACAO",
      List.of("current.situacao", "current.dtini", "draft.dtini", "current.dtprevfim", "draft.dtprevfim", "current.diasGozo", "draft.diasGozo", "current.abonoPecuniario", "draft.abonoPecuniario", "current.diasAbono", "draft.diasAbono", "current.iniFimAbono", "draft.iniFimAbono"),
      """
      {"or":[{"in":[{"var":"current.situacao"},["PROGRAMADA"]]},{"and":[{"==":[{"var":"current.dtini"},{"var":"draft.dtini"}]},{"==":[{"var":"current.dtprevfim"},{"var":"draft.dtprevfim"}]},{"==":[{"var":"current.diasGozo"},{"var":"draft.diasGozo"}]},{"==":[{"var":"current.abonoPecuniario"},{"var":"draft.abonoPecuniario"}]},{"==":[{"var":"current.diasAbono"},{"var":"draft.diasAbono"}]},{"==":[{"var":"current.iniFimAbono"},{"var":"draft.iniFimAbono"}]}]}]}
      """),

  PERIODO_CRIAR("CERG-09020", "diasConvertidosDobro", "Conversão em dobro exige período homologado e ação própria.",
      "POLITICA_SHOWCASE.PERIODO_CRIAR",
      List.of("draft.dias", "draft.data"),
      """
      {"and":[{"==":[{"var":"draft.dias"},0]},{"==":[{"var":"draft.data"},null]}]}
      """),

  PERIODO_CONVERSAO_EDICAO("CERG-09021", "diasConvertidosDobro", "Conversão em dobro exige ação própria.",
      "POLITICA_SHOWCASE.PERIODO_CONVERSAO_EDICAO",
      List.of("current.diasConvertidosDobro", "draft.diasConvertidosDobro", "current.dtCntDobro", "draft.dtCntDobro"),
      """
      {"and":[{"==":[{"var":"current.diasConvertidosDobro"},{"var":"draft.diasConvertidosDobro"}]},{"==":[{"var":"current.dtCntDobro"},{"var":"draft.dtCntDobro"}]}]}
      """),

  PERIODO_VINCULO_ORIGEM("CERG-09022", "averbacaoOrigemId", "Averbação de origem não pertence ao vínculo do período.",
      "POLITICA_SHOWCASE.PERIODO_VINCULO_ORIGEM",
      List.of("averbacaoOrigem.pessoa", "draft.pessoa", "averbacaoOrigem.vinculo", "draft.vinculo"),
      """
      {"and":[{"==":[{"var":"averbacaoOrigem.pessoa"},{"var":"draft.pessoa"}]},{"==":[{"var":"averbacaoOrigem.vinculo"},{"var":"draft.vinculo"}]}]}
      """),

  PERIODO_NOVA_ORIGEM("CERG-09023", "averbacaoOrigemId", "Nova origem exige averbação registrada, não removida e destinada à licença especial.",
      "POLITICA_SHOWCASE.PERIODO_NOVA_ORIGEM",
      List.of("current.origem", "draft.origem", "averbacaoOrigem.situacao", "averbacaoOrigem.dataRemocao", "averbacaoOrigem.finalidade"),
      """
      {"or":[{"==":[{"var":"current.origem"},{"var":"draft.origem"}]},{"and":[{"==":[{"var":"averbacaoOrigem.situacao"},"HOMOLOGADA"]},{"==":[{"var":"averbacaoOrigem.dataRemocao"},null]},{"==":[{"var":"averbacaoOrigem.finalidade"},"LICENCA_ESPECIAL"]}]}]}
      """),

  VENDA_CRIAR_DATAS("CERG-09024", "dtPagamento", "Nova solicitação não pode nascer liquidada ou estornada.",
      "POLITICA_SHOWCASE.VENDA_CRIAR_DATAS",
      List.of("draft.pagamento", "draft.estorno"),
      """
      {"and":[{"==":[{"var":"draft.pagamento"},null]},{"==":[{"var":"draft.estorno"},null]}]}
      """),

  VENDA_CRIAR_MODALIDADE("CERG-09025", "modalidade", "Abono vinculado é gerado exclusivamente pela homologação do gozo.",
      "POLITICA_SHOWCASE.VENDA_CRIAR_MODALIDADE",
      List.of("draft.gozo", "draft.modalidade"),
      """
      {"and":[{"==":[{"var":"draft.gozo"},null]},{"==":[{"var":"draft.modalidade"},"PECUNIA"]}]}
      """),

  VENDA_DATAS_EDICAO("CERG-09026", "dtPagamento", "Pagamento e estorno exigem ação própria.",
      "POLITICA_SHOWCASE.VENDA_DATAS_EDICAO",
      List.of("current.dtPagamento", "draft.dtPagamento", "current.dtEstorno", "draft.dtEstorno"),
      """
      {"and":[{"==":[{"var":"current.dtPagamento"},{"var":"draft.dtPagamento"}]},{"==":[{"var":"current.dtEstorno"},{"var":"draft.dtEstorno"}]}]}
      """),

  VENDA_ABONO_ORIGEM("ERG-03387", "licencaEspecialGozoId", "Mantenha o abono vinculado pela parcela de gozo. Na venda, apenas a liquidação própria pode alterar o abono.",
      "PCK_ERG_VENDA_LICESP.MAIN_PRE:99-103, trata_erro(3387) para alteração ou exclusão avulsa.",
      List.of("draft.gozo", "context.operacao", "current.mesAnoFolha", "draft.mesAnoFolha", "current.numFolha", "draft.numFolha", "current.documentoLegalId", "draft.documentoLegalId", "current.atoPublicacaoId", "draft.atoPublicacaoId"),
      """
      {"or":[{"==":[{"var":"draft.gozo"},null]},{"and":[{"==":[{"var":"context.operacao"},"liquidacao"]},{"and":[{"==":[{"var":"current.mesAnoFolha"},{"var":"draft.mesAnoFolha"}]},{"==":[{"var":"current.numFolha"},{"var":"draft.numFolha"}]},{"==":[{"var":"current.documentoLegalId"},{"var":"draft.documentoLegalId"}]},{"==":[{"var":"current.atoPublicacaoId"},{"var":"draft.atoPublicacaoId"}]}]}]}]}
      """),

  VENDA_PERIODO_ORIGEM("CERG-09027", "licencaEspecialGozoId", "Origem do abono incompatível com a venda.",
      "POLITICA_SHOWCASE.VENDA_PERIODO_ORIGEM",
      List.of("gozo.periodo", "draft.periodo"),
      """
      {"==":[{"var":"gozo.periodo"},{"var":"draft.periodo"}]}
      """),

  VENDA_MODALIDADE_ORIGEM("CERG-09028", "modalidade", "Origem do abono incompatível com a venda.",
      "POLITICA_SHOWCASE.VENDA_MODALIDADE_ORIGEM",
      List.of("draft.modalidade"),
      """
      {"==":[{"var":"draft.modalidade"},"ABONO_JUNTO_GOZO"]}
      """),

  VENDA_DIAS_VALOR("CERG-09029", "diasVendidos", "Dias ou valor da venda inválidos.",
      "POLITICA_SHOWCASE.VENDA_DIAS_VALOR",
      List.of("draft.gozo", "draft.situacao", "draft.dias", "draft.valor"),
      """
      {"or":[{"and":[{"==":[{"var":"draft.gozo"},null]},{"!=":[{"var":"draft.situacao"},"ESTORNADA"]}]},{"and":[{"!=":[{"var":"draft.dias"},null]},{">=":[{"var":"draft.dias"},1]},{"or":[{"==":[{"var":"draft.valor"},null]},{">=":[{"var":"draft.valor"},0]}]}]}]}
      """),

  VENDA_PATCH_ESTORNO("CERG-09030", "situacao", "Abono estornado é histórico; reconcilie a origem para emitir nova autorização.",
      "POLITICA_SHOWCASE.VENDA_PATCH_ESTORNO",
      List.of("current.situacao"),
      """
      {"!=":[{"var":"current.situacao"},"ESTORNADA"]}
      """),

  VENDA_RECONCILIAR_ORIGEM("CERG-09031", "licencaEspecialGozoId", "A venda não possui gozo de origem.",
      "POLITICA_SHOWCASE.VENDA_RECONCILIAR_ORIGEM",
      List.of("current.gozo"),
      """
      {"!=":[{"var":"current.gozo"},null]}
      """),

  VENDA_RECONCILIAR_DATA("CERG-09032", "dataEfeito", "Reconciliação não pode anteceder o estorno original.",
      "POLITICA_SHOWCASE.VENDA_RECONCILIAR_DATA",
      List.of("current.estorno", "draft.data"),
      """
      {"or":[{"==":[{"var":"current.estorno"},null]},{">=":[{"var":"draft.data"},{"var":"current.estorno"}]}]}
      """),

  VENDA_RECONCILIAR_GOZO("CERG-09033", "licencaEspecialGozoId", "Gozo de origem não possui abono homologado ativo para reconciliação.",
      "POLITICA_SHOWCASE.VENDA_RECONCILIAR_GOZO",
      List.of("gozo.situacao", "gozo.abono"),
      """
      {"and":[{"!=":[{"var":"gozo.situacao"},"PROGRAMADA"]},{"!=":[{"var":"gozo.situacao"},"CANCELADO"]},{"==":[{"var":"gozo.abono"},true]}]}
      """),

  VENDA_RETIFICACAO("CERG-09034", "situacao", "Campos operacionais não podem ser retificados neste estado do ciclo de vida.",
      "POLITICA_SHOWCASE.VENDA_RETIFICACAO",
      List.of("current.situacao", "current.diasVendidos", "draft.diasVendidos", "current.valorIndenizacao", "draft.valorIndenizacao", "current.mesAnoFolha", "draft.mesAnoFolha", "current.numFolha", "draft.numFolha"),
      """
      {"or":[{"in":[{"var":"current.situacao"},["SOLICITADA","AUTORIZADA"]]},{"and":[{"==":[{"var":"current.diasVendidos"},{"var":"draft.diasVendidos"}]},{"==":[{"var":"current.valorIndenizacao"},{"var":"draft.valorIndenizacao"}]},{"==":[{"var":"current.mesAnoFolha"},{"var":"draft.mesAnoFolha"}]},{"==":[{"var":"current.numFolha"},{"var":"draft.numFolha"}]}]}]}
      """),

  PERIODO_SALDO_CONTABIL("CERG-09035", "saldoDias", "Contabilidade do período excede os dias de direito.",
      "POLITICA_SHOWCASE.PERIODO_SALDO_CONTABIL",
      List.of("draft.direito", "draft.gozo", "draft.vendido", "draft.convertido"),
      """
      {"<=":[{"var":"draft.gozo"},{"-":[{"var":"draft.direito"},{"var":"draft.vendido"},{"var":"draft.convertido"}]}]}
      """),

  GOZO_ABONO_UNICO("CERG-09036", "abonoPecuniario", "Mais de um abono ativo vinculado à mesma parcela.",
      "POLITICA_SHOWCASE.GOZO_ABONO_UNICO",
      List.of("neighborhood.abonosAtivos"),
      """
      {"<=":[{"var":"neighborhood.abonosAtivos"},1]}
      """),

  AVERBACAO_REDISTRIBUICAO("CERG-09037", "diasDesaverbados", "Múltiplos períodos de origem exigem redistribuição individual antes da desaverbação.",
      "POLITICA_SHOWCASE.AVERBACAO_REDISTRIBUICAO",
      List.of("neighborhood.periodos"),
      """
      {"==":[{"var":"neighborhood.periodos"},1]}
      """),

  PREVIA_AVERBACAO("CERG-09038", "diasDesaverbados", "Desaverbação excede o tempo certificado.",
      "POLITICA_SHOWCASE.PREVIA_AVERBACAO",
      List.of("draft.total", "draft.dias"),
      """
      {"and":[{"!=":[{"var":"draft.total"},null]},{"!=":[{"var":"draft.dias"},null]},{">=":[{"var":"draft.total"},0]},{">=":[{"var":"draft.dias"},0]},{"<=":[{"var":"draft.dias"},{"var":"draft.total"}]}]}
      """),

  PREVIA_CONVERSAO("CERG-09039", "diasConvertidosDobro", "Conversão excede o direito inicial.",
      "POLITICA_SHOWCASE.PREVIA_CONVERSAO",
      List.of("draft.direito", "draft.dias"),
      """
      {"and":[{"!=":[{"var":"draft.direito"},null]},{"!=":[{"var":"draft.dias"},null]},{">=":[{"var":"draft.direito"},0]},{"<=":[{"var":"draft.direito"},999]},{">=":[{"var":"draft.dias"},0]},{"<=":[{"var":"draft.dias"},{"var":"draft.direito"}]}]}
      """),

  PREVIA_GOZO_INTERVALO("CERG-09040", "dtprevfim", "Intervalo previsto inválido.",
      "POLITICA_SHOWCASE.PREVIA_GOZO_INTERVALO",
      List.of("draft.inicio", "draft.fim"),
      """
      {"and":[{"!=":[{"var":"draft.inicio"},null]},{"!=":[{"var":"draft.fim"},null]},{"<=":[{"var":"draft.inicio"},{"var":"draft.fim"}]}]}
      """),

  PREVIA_GOZO_LIMITE("CERG-09041", "dtprevfim", "Parcela excede 999 dias.",
      "POLITICA_SHOWCASE.PREVIA_GOZO_LIMITE",
      List.of("draft.dias"),
      """
      {"<=":[{"var":"draft.dias"},999]}
      """),

  PREVIA_VENDA_SALDO_APURADO("CERG-09042", "periodoAquisitivoLicencaId", "Saldo do período não apurado.",
      "POLITICA_SHOWCASE.PREVIA_VENDA_SALDO_APURADO",
      List.of("periodoAquisitivo.saldo"),
      """
      {"!=":[{"var":"periodoAquisitivo.saldo"},null]}
      """),

  PREVIA_VENDA_CONSUMO("CERG-09043", "vendaId", "Consumo anterior da venda não apurado.",
      "POLITICA_SHOWCASE.PREVIA_VENDA_CONSUMO",
      List.of("current.dias"),
      """
      {"and":[{"!=":[{"var":"current.dias"},null]},{">=":[{"var":"current.dias"},1]}]}
      """),

  PREVIA_VENDA_LIMITE("CERG-09044", "diasVendidos", "Venda excede o saldo disponível.",
      "POLITICA_SHOWCASE.PREVIA_VENDA_LIMITE",
      List.of("draft.dias", "periodoAquisitivo.saldo"),
      """
      {"and":[{"!=":[{"var":"draft.dias"},null]},{">=":[{"var":"draft.dias"},1]},{"<=":[{"var":"draft.dias"},999]},{"<=":[{"var":"draft.dias"},{"var":"periodoAquisitivo.saldo"}]}]}
      """);

  private final String code;
  private final String field;
  private final String message;
  private final String origin;
  private final List<String> keys;
  private final String expression;

  OperationalRuleSet(String code, String field, String message, String origin,
      List<String> keys, String expression) {
    this.code = code;
    this.field = field;
    this.message = message;
    this.origin = origin;
    this.keys = keys;
    this.expression = expression;
  }

  private String slot() {
    return "operacao." + name().toLowerCase(Locale.ROOT).replace('_', '-');
  }

  ErgonRuleErrorCatalog.RuleErrorDescriptor descriptor() {
    return new ErgonRuleErrorCatalog.RuleErrorDescriptor(code, slot(), field, message,
        origin, this == VENDA_ABONO_ORIGEM ? origin : message);
  }

  public RuleSetDefinition productRuleSet(ObjectMapper mapper) {
    var slot = new DecisionSlot(slot(), DecisionStage.PROTECTED_GUARD, SlotCardinality.SINGLE,
        OverridePolicy.FORBIDDEN, DecisionAggregationPolicy.SINGLE_RESULT, null);
    try {
      var tree = mapper.readTree(expression);
      var binding = new DecisionBinding("product." + slot(), slot(), DecisionSource.PRODUCT, null,
          RuleExecutorRef.jsonLogic(tree), List.of(), 1, true,
          RuleDecision.DENY, code, ErgonOntologyRuleSetSupport.roots(tree), null);
      return RuleSetDefinition.withInferredRoots(
          new RuleSetRef("administracao-pessoal", "operacoes-showcase", slot(), "validate", 1),
          List.of(slot), List.of(binding), RuleRuntimeCompatibility.current(),
          RuleFailPolicy.FAIL_CLOSED);
    } catch (JsonProcessingException invalidExpression) {
      throw new IllegalStateException("Expressão inválida para " + code, invalidExpression);
    }
  }

  ObjectNode facts(ObjectMapper mapper, Object... values) {
    if (values.length != keys.size()) {
      throw new IllegalArgumentException("Fatos incompatíveis com " + code);
    }
    var root = mapper.createObjectNode();
    for (int i = 0; i < values.length; i++) {
      Object value = values[i];
      if (value instanceof LocalDate date) value = date.toEpochDay();
      // A retificação compara o valor cadastral com a escala persistida.
      if (this == VENDA_RETIFICACAO && value instanceof BigDecimal amount) value = amount.toString();
      String[] segments = keys.get(i).split("\\.", 2);
      if (segments.length != 2) {
        throw new IllegalStateException("Chave ontológica inválida em " + code + ": " + keys.get(i));
      }
      root.withObject("/" + segments[0]).set(segments[1], mapper.valueToTree(value));
    }
    return root;
  }

  public static String text(String value) {
    return value != null && value.isBlank() ? "" : value;
  }
}
