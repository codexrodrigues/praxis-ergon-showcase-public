package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.NumericFormat;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.customfields.CustomFields;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.extension.annotation.UISchemaPreset;
import org.praxisplatform.uischema.filter.annotation.Filterable;
import org.praxisplatform.uischema.filter.annotation.Filterable.FilterOperation;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;

@Getter
@Setter
@Schema(description = "Pesquisa e recortes operacionais. Catálogo por órgão de funções de direção, chefia e assessoramento, com vigência, gratificação e política de vagas e substituição.")
public class FuncaoConfiancaFilterDTO implements GenericFilterDTO {
  @Schema(description = "Pesquisa funções de confiança por identificação e dados vinculados publicados na consulta.")
  @UISchema(label = "Busca ampla", helpText = "Digite parte do nome ou da identificação.",
      type = FieldDataType.TEXT, controlType = FieldControlType.SEARCH_INPUT, icon = "search", preset = UISchemaPreset.NONE)
  @Filterable(operation = FilterOperation.LIKE, relations = {"descricao", "codigo", "simbolo", "orgao.nome"})
  private String buscaAmpla;

  @Schema(description = "Restringe a consulta à identificação de registro com o prefixo informado.")
  @UISchema(label = "Descrição começa com", helpText = "Digite o início da descrição.",
      type = FieldDataType.TEXT, controlType = FieldControlType.INPUT, icon = "notes", preset = UISchemaPreset.NONE)
  @Filterable(operation = FilterOperation.STARTS_WITH, relation = "descricao")
  private String descricaoPrefixo;

  @Schema(description = "Restringe a consulta à identificação integral registrada no cadastro.")
  @UISchema(label = "Descrição exata", helpText = "Informe a descrição completa.",
      type = FieldDataType.TEXT, controlType = FieldControlType.INPUT, icon = "notes", preset = UISchemaPreset.NONE)
  @Filterable(operation = FilterOperation.EQUAL)
  private String descricao;

  @Schema(description = "Inclui registros em qualquer uma das situações selecionadas.")
  @UISchema(label = "Situações incluídas", helpText = "Escolha os estados a consultar.",
      type = FieldDataType.ARRAY, controlType = FieldControlType.MULTI_SELECT, icon = "flag", preset = UISchemaPreset.ENTERPRISE_STATUS, options = "[{\"value\":\"ATIVA\",\"label\":\"Ativa\"},{\"value\":\"SUSPENSA\",\"label\":\"Suspensa\"},{\"value\":\"EXTINTA\",\"label\":\"Extinta\"}]")
  @Filterable(operation = FilterOperation.IN, relation = "situacao")
  private List<String> situacoes;

  @Schema(description = "Exclui os registros nas situações selecionadas.")
  @UISchema(label = "Situações excluídas", helpText = "Escolha os estados a retirar do resultado.",
      type = FieldDataType.ARRAY, controlType = FieldControlType.MULTI_SELECT, icon = "flag", preset = UISchemaPreset.ENTERPRISE_STATUS, options = "[{\"value\":\"ATIVA\",\"label\":\"Ativa\"},{\"value\":\"SUSPENSA\",\"label\":\"Suspensa\"},{\"value\":\"EXTINTA\",\"label\":\"Extinta\"}]")
  @Filterable(operation = FilterOperation.NOT_IN, relation = "situacao")
  private List<String> situacoesExcluidas;

  @Schema(description = "Janela inclusiva para a data de início registrada.")
  @UISchema(label = "Início entre datas", helpText = "Informe a primeira e a última data.",
      type = FieldDataType.DATE, controlType = FieldControlType.DATE_RANGE, icon = "date_range", preset = UISchemaPreset.DATE_RANGE)
  @Filterable(operation = FilterOperation.BETWEEN, relation = "dtini")
  private List<LocalDate> inicioEntre;

  @Schema(description = "Janela em dias civis até hoje para localizar registros iniciados recentemente.")
  @UISchema(label = "Inícios recentes", helpText = "Informe a quantidade de dias anteriores a hoje.",
      type = FieldDataType.NUMBER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "tag", preset = UISchemaPreset.NONE)
  @Filterable(operation = FilterOperation.IN_LAST_DAYS, relation = "dtini")
  private Integer inicioUltimosDias;

  @Schema(description = "Janela em dias civis a partir de hoje para localizar fins de vigência previstos.")
  @UISchema(label = "Términos próximos", helpText = "Informe a quantidade de dias futuros.",
      type = FieldDataType.NUMBER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "tag", preset = UISchemaPreset.NONE)
  @Filterable(operation = FilterOperation.IN_NEXT_DAYS, relation = "dtfim")
  private Integer terminoProximosDias;

  @Schema(description = "Seleciona registros cuja vigência termina em data ainda não registrada.")
  @UISchema(label = "Sem data de fim", helpText = "Marque para localizar períodos abertos.",
      type = FieldDataType.BOOLEAN, controlType = FieldControlType.CHECKBOX, icon = "fact_check", preset = UISchemaPreset.BOOLEAN_FLAG, labelTrue = "Sim", labelFalse = "Não", toneTrue = "info", toneFalse = "neutral")
  @Filterable(operation = FilterOperation.IS_NULL, relation = "dtfim")
  private Boolean semTermino;

  @Schema(description = "Seleciona registros que possuem data final registrada.")
  @UISchema(label = "Com data de fim", helpText = "Marque para localizar períodos com término definido.",
      type = FieldDataType.BOOLEAN, controlType = FieldControlType.CHECKBOX, icon = "fact_check", preset = UISchemaPreset.BOOLEAN_FLAG, labelTrue = "Sim", labelFalse = "Não", toneTrue = "info", toneFalse = "neutral")
  @Filterable(operation = FilterOperation.IS_NOT_NULL, relation = "dtfim")
  private Boolean comTermino;

  @Schema(description = "Código cadastrado a partir do prefixo informado.")
  @UISchema(label = "Código inicial", helpText = "Digite os primeiros caracteres do código.",
      type = FieldDataType.TEXT, controlType = FieldControlType.INPUT, icon = "notes", preset = UISchemaPreset.NONE)
  @Filterable(operation = FilterOperation.STARTS_WITH, relation = "codigo")
  private String codigoPrefixo;

  @Schema(description = "Órgão responsável pelo cadastro e pela gestão funcional deste registro.")
  @UISchema(label = "Órgão", helpText = "Selecione o órgão ao qual pertence o registro.",
      type = FieldDataType.NUMBER, controlType = FieldControlType.ENTITY_LOOKUP, icon = "apartment", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.ORGAO_OPTIONS, valueField = "id", displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL)
  private Integer empCodigo;

  @Schema(description = "Primeiro dia de validade do cadastro da função no órgão.")
  @UISchema(label = "Início da vigência", helpText = "Use a data inicial que consta no ato.",
      type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, icon = "event", preset = UISchemaPreset.DATE_RANGE)
  @Filterable(operation = FilterOperation.EQUAL)
  private LocalDate dtini;

  @Schema(description = "Último dia de vigência da função, inclusive, quando houver extinção ou término previsto.")
  @UISchema(label = "Fim da vigência", helpText = "Deixe em branco enquanto a vigência estiver aberta.",
      type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, icon = "event", preset = UISchemaPreset.DATE_RANGE)
  @Filterable(operation = FilterOperation.EQUAL)
  private LocalDate dtfim;

  @Schema(description = "Posição cadastral da função quanto à disponibilidade institucional para designação.")
  @UISchema(label = "Situação", helpText = "Selecione a posição cadastral documentada.",
      type = FieldDataType.TEXT, controlType = FieldControlType.SELECT, icon = "flag", preset = UISchemaPreset.ENTERPRISE_STATUS, options = "[{\"value\":\"ATIVA\",\"label\":\"Ativa\"},{\"value\":\"SUSPENSA\",\"label\":\"Suspensa\"},{\"value\":\"EXTINTA\",\"label\":\"Extinta\"}]")
  @Filterable(operation = FilterOperation.EQUAL)
  private String situacao;

  @Schema(description = "Identificador funcional usado nos atos de designação e no cadastro de vagas do órgão.")
  @UISchema(label = "Código da função", helpText = "Informe o código registrado no ato.",
      type = FieldDataType.TEXT, controlType = FieldControlType.INPUT, icon = "notes", preset = UISchemaPreset.NONE)
  @Filterable(operation = FilterOperation.EQUAL)
  private String codigo;

  @Schema(description = "Natureza da função comissionada conforme sua finalidade institucional de direção, chefia ou assessoramento.")
  @UISchema(label = "Categoria", helpText = "Selecione a finalidade da função.",
      type = FieldDataType.TEXT, controlType = FieldControlType.SELECT, icon = "flag", preset = UISchemaPreset.ENTERPRISE_STATUS, options = "[{\"value\":\"DIRECAO\",\"label\":\"Direção\"},{\"value\":\"CHEFIA\",\"label\":\"Chefia\"},{\"value\":\"ASSESSORAMENTO\",\"label\":\"Assessoramento\"}]")
  @Filterable(operation = FilterOperation.EQUAL)
  private String categoria;

  @Schema(description = "Símbolo da tabela remuneratória associado à função, como FG-1 ou DAS-3.")
  @UISchema(label = "Símbolo remuneratório", helpText = "Use o símbolo vigente no ato.",
      type = FieldDataType.TEXT, controlType = FieldControlType.INPUT, icon = "notes", preset = UISchemaPreset.NONE)
  @Filterable(operation = FilterOperation.EQUAL)
  private String simbolo;

  @Schema(description = "Indicador de que a designação passa a exigir vaga numerada a partir do marco de controle cadastrado.")
  @UISchema(label = "Controle de vagas", helpText = "Marque quando houver distribuição formal de vagas.",
      type = FieldDataType.BOOLEAN, controlType = FieldControlType.CHECKBOX, icon = "fact_check", preset = UISchemaPreset.BOOLEAN_FLAG, labelTrue = "Sim", labelFalse = "Não", toneTrue = "info", toneFalse = "neutral")
  @Filterable(operation = FilterOperation.EQUAL)
  private Boolean controleVaga;

  @Schema(description = "Primeiro dia em que a designação nesta função passa a exigir número de vaga.")
  @UISchema(label = "Início do controle", helpText = "Use a data definida para implantação do controle.",
      type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, icon = "event", preset = UISchemaPreset.DATE_RANGE)
  @Filterable(operation = FilterOperation.EQUAL)
  private LocalDate dtInicioContrVaga;

  @Schema(description = "Indicador da política da função que permite exercício interino durante impedimento do titular.")
  @UISchema(label = "Admite substituição", helpText = "Confira a previsão de substituição da função.",
      type = FieldDataType.BOOLEAN, controlType = FieldControlType.CHECKBOX, icon = "fact_check", preset = UISchemaPreset.BOOLEAN_FLAG, labelTrue = "Sim", labelFalse = "Não", toneTrue = "info", toneFalse = "neutral")
  @Filterable(operation = FilterOperation.EQUAL)
  private Boolean permiteSubstituicao;

  @Schema(description = "Modalidade de substituição prevista no histórico da função e utilizada na identificação do evento funcional.")
  @UISchema(label = "Tipo de substituição", helpText = "Use o tipo definido no cadastro da função.",
      type = FieldDataType.TEXT, controlType = FieldControlType.INPUT, icon = "notes", preset = UISchemaPreset.NONE)
  @Filterable(operation = FilterOperation.EQUAL)
  private String tipoSubstituicao;

  @Schema(description = "Quantidade mínima de dias de exercício interino para enquadramento na política de remuneração da função.")
  @UISchema(label = "Dias mínimos", helpText = "Informe o mínimo previsto para pagamento.",
      type = FieldDataType.NUMBER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "tag", preset = UISchemaPreset.NONE)
  @Filterable(operation = FilterOperation.EQUAL)
  private Integer diasMinimosSubstituicao;

  @Schema(description = "Valor mensal integral da gratificação correspondente ao símbolo da função.")
  @UISchema(label = "Valor da gratificação", helpText = "Informe o valor nominal mensal em reais.",
      type = FieldDataType.NUMBER, controlType = FieldControlType.CURRENCY_INPUT, icon = "payments", preset = UISchemaPreset.MONETARY_AMOUNT)
  @Filterable(operation = FilterOperation.EQUAL)
  private BigDecimal valorGratificacao;

  @Schema(description = "Parcela percentual da gratificação aplicável quando o servidor opta pela remuneração do vínculo efetivo.")
  @UISchema(label = "Percentual de opção", helpText = "Informe de 0 a 100.",
      type = FieldDataType.NUMBER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "percent", preset = UISchemaPreset.NONE, numericFormat = NumericFormat.PERCENT)
  @Filterable(operation = FilterOperation.EQUAL)
  private BigDecimal percentualOpcaoEfetivo;

  @Schema(description = "Indicação de reserva da função a ocupantes de vínculo efetivo, conforme o ato de instituição.")
  @UISchema(label = "Exige vínculo efetivo", helpText = "Confira os requisitos de provimento.",
      type = FieldDataType.BOOLEAN, controlType = FieldControlType.CHECKBOX, icon = "fact_check", preset = UISchemaPreset.BOOLEAN_FLAG, labelTrue = "Sim", labelFalse = "Não", toneTrue = "info", toneFalse = "neutral")
  @Filterable(operation = FilterOperation.EQUAL)
  private Boolean exigeVinculoEfetivo;

  @Schema(description = "Requisitos de carreira, formação ou experiência exigidos para exercício da função.")
  @UISchema(label = "Requisitos de provimento", helpText = "Registre os critérios do ato de instituição.",
      type = FieldDataType.TEXT, controlType = FieldControlType.INPUT, icon = "notes", preset = UISchemaPreset.NONE)
  @Filterable(operation = FilterOperation.EQUAL)
  private String requisitos;

  @Schema(description = "Documento que fornece o fundamento jurídico para o registro.")
  @UISchema(label = "Fundamento legal", helpText = "Selecione o ato legal de referência.",
      type = FieldDataType.NUMBER, controlType = FieldControlType.ENTITY_LOOKUP, icon = "link", preset = UISchemaPreset.LEGAL_DOCUMENT_REFERENCE, endpoint = ApiPaths.DOCUMENTO_OPTIONS, valueField = "id", displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL)
  private Long documentoLegalId;

  @Schema(description = "Documento que comprova a publicação oficial deste registro.")
  @UISchema(label = "Ato publicado", helpText = "Selecione a publicação correspondente.",
      type = FieldDataType.NUMBER, controlType = FieldControlType.ENTITY_LOOKUP, icon = "link", preset = UISchemaPreset.LEGAL_DOCUMENT_REFERENCE, endpoint = ApiPaths.DOCUMENTO_OPTIONS, valueField = "id", displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL)
  private Long atoPublicacaoId;

  @CustomFields(resourceKey = ApiPaths.KEY_FUNCOES)
  @Schema(description = "Critérios sobre atributos institucionais publicados para o cadastro de origem.")
  @UISchema(label = "Campos customizados", helpText = "Selecione os atributos do órgão usados como critérios.",
      type = FieldDataType.JSON, controlType = FieldControlType.INPUT, icon = "extension")
  private Map<String, Object> customFields = new LinkedHashMap<>();
}
