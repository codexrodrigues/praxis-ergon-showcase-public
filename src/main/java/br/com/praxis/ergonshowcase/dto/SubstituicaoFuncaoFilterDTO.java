package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.domain.VinculoId;
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
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.customfields.CustomFields;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.extension.annotation.UISchemaPreset;
import org.praxisplatform.uischema.filter.annotation.Filterable;
import org.praxisplatform.uischema.filter.annotation.Filterable.FilterOperation;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;

@Getter
@Setter
@Schema(description = "Pesquisa e recortes operacionais. Exercícios interinos vinculados à designação titular e ao impedimento, com vigências funcional e remuneratória independentes.")
public class SubstituicaoFuncaoFilterDTO implements GenericFilterDTO {
  @Schema(description = "Pesquisa substituições por identificação e dados vinculados publicados na consulta.")
  @UISchema(label = "Busca ampla", helpText = "Digite parte do nome ou da identificação.",
      type = FieldDataType.TEXT, controlType = FieldControlType.SEARCH_INPUT, icon = "search", preset = UISchemaPreset.NONE)
  @Filterable(operation = FilterOperation.LIKE, relations = {"descricao", "substituto.pessoa.nome", "designacao.titular.pessoa.nome", "designacao.funcao.descricao"})
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
      type = FieldDataType.ARRAY, controlType = FieldControlType.MULTI_SELECT, icon = "flag", preset = UISchemaPreset.ENTERPRISE_STATUS, options = "[{\"value\":\"PROGRAMADA\",\"label\":\"Programada\"},{\"value\":\"EM_EXERCICIO\",\"label\":\"Em exercício\"},{\"value\":\"PENDENTE_FOLHA\",\"label\":\"Pendente de folha\"},{\"value\":\"LIQUIDADA\",\"label\":\"Liquidada\"},{\"value\":\"CANCELADA\",\"label\":\"Cancelada\"}]")
  @Filterable(operation = FilterOperation.IN, relation = "situacao")
  private List<String> situacoes;

  @Schema(description = "Exclui os registros nas situações selecionadas.")
  @UISchema(label = "Situações excluídas", helpText = "Escolha os estados a retirar do resultado.",
      type = FieldDataType.ARRAY, controlType = FieldControlType.MULTI_SELECT, icon = "flag", preset = UISchemaPreset.ENTERPRISE_STATUS, options = "[{\"value\":\"PROGRAMADA\",\"label\":\"Programada\"},{\"value\":\"EM_EXERCICIO\",\"label\":\"Em exercício\"},{\"value\":\"PENDENTE_FOLHA\",\"label\":\"Pendente de folha\"},{\"value\":\"LIQUIDADA\",\"label\":\"Liquidada\"},{\"value\":\"CANCELADA\",\"label\":\"Cancelada\"}]")
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

  @Schema(description = "Seleciona exercícios interinos sem período remunerado registrado.")
  @UISchema(label = "Sem início remunerado", helpText = "Localize substituições sem data de início remunerado.",
      type = FieldDataType.BOOLEAN, controlType = FieldControlType.CHECKBOX, icon = "fact_check", preset = UISchemaPreset.BOOLEAN_FLAG,
      labelTrue = "Sim", labelFalse = "Não", toneTrue = "info", toneFalse = "neutral")
  @Filterable(operation = FilterOperation.IS_NULL, relation = "dtiniRemunerada")
  private Boolean semInicioRemunerado;

  @Schema(description = "Seleciona exercícios interinos com primeiro dia remunerado registrado.")
  @UISchema(label = "Com início remunerado", helpText = "Marque para inspecionar períodos pagos ou apurados.",
      type = FieldDataType.BOOLEAN, controlType = FieldControlType.CHECKBOX, icon = "fact_check", preset = UISchemaPreset.BOOLEAN_FLAG, labelTrue = "Sim", labelFalse = "Não", toneTrue = "info", toneFalse = "neutral")
  @Filterable(operation = FilterOperation.IS_NOT_NULL, relation = "dtiniRemunerada")
  private Boolean comInicioRemunerado;

  @Schema(description = "Órgão responsável pelo cadastro e pela gestão funcional deste registro.")
  @UISchema(label = "Órgão", helpText = "Selecione o órgão ao qual pertence o registro.",
      type = FieldDataType.NUMBER, controlType = FieldControlType.ENTITY_LOOKUP, icon = "apartment", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.ORGAO_OPTIONS, valueField = "id", displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL)
  private Integer empCodigo;

  @Schema(description = "Primeiro dia de exercício interino das atribuições do titular.")
  @UISchema(label = "Início da substituição", helpText = "Use a data inicial que consta no ato.",
      type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, icon = "event", preset = UISchemaPreset.DATE_RANGE)
  @Filterable(operation = FilterOperation.EQUAL)
  private LocalDate dtini;

  @Schema(description = "Último dia de exercício interino, inclusive.")
  @UISchema(label = "Fim da substituição", helpText = "Informe o último dia do exercício interino.",
      type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, icon = "event", preset = UISchemaPreset.DATE_RANGE)
  @Filterable(operation = FilterOperation.EQUAL)
  private LocalDate dtfim;

  @Schema(description = "Posição do exercício interino e do respectivo processamento em folha.")
  @UISchema(label = "Situação", helpText = "Selecione a posição cadastral documentada.",
      type = FieldDataType.TEXT, controlType = FieldControlType.SELECT, icon = "flag", preset = UISchemaPreset.ENTERPRISE_STATUS, options = "[{\"value\":\"PROGRAMADA\",\"label\":\"Programada\"},{\"value\":\"EM_EXERCICIO\",\"label\":\"Em exercício\"},{\"value\":\"PENDENTE_FOLHA\",\"label\":\"Pendente de folha\"},{\"value\":\"LIQUIDADA\",\"label\":\"Liquidada\"},{\"value\":\"CANCELADA\",\"label\":\"Cancelada\"}]")
  @Filterable(operation = FilterOperation.EQUAL)
  private String situacao;

  @Schema(description = "Evento precursor que identifica a função, o titular substituído e a vigência de sua designação.")
  @UISchema(label = "Designação titular", helpText = "Selecione o ato do titular impedido.",
      type = FieldDataType.NUMBER, controlType = FieldControlType.ENTITY_LOOKUP, icon = "link", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.DESIGNACAO_OPTIONS, valueField = "id", displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL)
  private Long designacaoId;

  @Schema(description = "Vínculo do servidor que assume temporariamente as atribuições da função.")
  @UISchema(label = "Vínculo substituto", helpText = "Escolha a matrícula de quem entra em exercício.",
      type = FieldDataType.TEXT, controlType = FieldControlType.ENTITY_LOOKUP, icon = "link", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.VINCULO_OPTIONS, valueField = "id", displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL)
  private VinculoId substitutoId;

  @Schema(description = "Natureza do impedimento do titular que originou o exercício interino.")
  @UISchema(label = "Motivo do exercício", helpText = "Selecione o evento que justifica a substituição.",
      type = FieldDataType.TEXT, controlType = FieldControlType.SELECT, icon = "flag", preset = UISchemaPreset.ENTERPRISE_STATUS, options = "[{\"value\":\"FERIAS\",\"label\":\"Férias\"},{\"value\":\"AFASTAMENTO\",\"label\":\"Afastamento\"},{\"value\":\"AUSENCIA_EVENTUAL\",\"label\":\"Ausência eventual\"}]")
  @Filterable(operation = FilterOperation.EQUAL)
  private String motivo;

  @Schema(description = "Evento funcional de afastamento que fundamenta o impedimento, quando esta é a origem da substituição.")
  @UISchema(label = "Afastamento do titular", helpText = "Vincule o afastamento registrado para o titular.",
      type = FieldDataType.NUMBER, controlType = FieldControlType.ENTITY_LOOKUP, icon = "link", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.AFASTAMENTO_OPTIONS, valueField = "id", displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL)
  private Long afastamentoId;

  @Schema(description = "Parcela de gozo de férias do titular que fundamenta o exercício interino.")
  @UISchema(label = "Férias do titular", helpText = "Vincule a parcela de férias correspondente.",
      type = FieldDataType.NUMBER, controlType = FieldControlType.ENTITY_LOOKUP, icon = "link", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.FERIAS_GOZO_OPTIONS, valueField = "id", displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL)
  private Long feriasGozoId;

  @Schema(description = "Primeiro dia remunerado do exercício interino, distinto do início funcional quando a regra de pagamento assim definir.")
  @UISchema(label = "Início remunerado", helpText = "Use o primeiro dia homologado para folha.",
      type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, icon = "event", preset = UISchemaPreset.DATE_RANGE)
  @Filterable(operation = FilterOperation.EQUAL)
  private LocalDate dtiniRemunerada;

  @Schema(description = "Último dia remunerado do exercício interino, contado inclusive no período pago.")
  @UISchema(label = "Fim remunerado", helpText = "Use o último dia homologado para folha.",
      type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, icon = "event", preset = UISchemaPreset.DATE_RANGE)
  @Filterable(operation = FilterOperation.EQUAL)
  private LocalDate dtfimRemunerada;

  @Schema(description = "Mês de referência em que o pagamento da substituição é processado, representado pelo primeiro dia do mês.")
  @UISchema(label = "Competência de folha", helpText = "Informe o primeiro dia da competência.",
      type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, icon = "event", preset = UISchemaPreset.DATE_RANGE)
  @Filterable(operation = FilterOperation.EQUAL)
  private LocalDate competenciaFolha;

  @Schema(description = "Valor bruto apurado para a substituição no período remunerado informado.")
  @UISchema(label = "Valor apurado", helpText = "Informe o valor apurado para a folha.",
      type = FieldDataType.NUMBER, controlType = FieldControlType.CURRENCY_INPUT, icon = "payments", preset = UISchemaPreset.MONETARY_AMOUNT)
  @Filterable(operation = FilterOperation.EQUAL)
  private BigDecimal valorSubstituicao;

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

  @Schema(description = "Pesquisa textual pelo nome oficial da unidade organizacional da designação substituída (ex.: UPA, Hospital, Secretaria de Saúde).", example = "Saúde")
  @UISchema(label = "Nome da unidade", helpText = "Digite parte do nome da unidade da função.",
      type = FieldDataType.TEXT, controlType = FieldControlType.INPUT, icon = "account_tree", preset = UISchemaPreset.NONE)
  @Filterable(operation = FilterOperation.LIKE, relation = "designacao.setor.nome")
  private String setorNome;

  @Schema(description = "Filtra substituições pela natureza operacional da unidade da função (ex.: SECRETARIA, DIRETORIA, COORDENADORIA, NUCLEO, UNIDADE_SAUDE, ESCOLA).")
  @UISchema(label = "Tipos de unidade", helpText = "Selecione os tipos de unidade organizacional.",
      type = FieldDataType.ARRAY, controlType = FieldControlType.MULTI_SELECT, icon = "category", preset = UISchemaPreset.ENTERPRISE_STATUS,
      options = "[{\"value\":\"SECRETARIA\",\"label\":\"Secretaria\"},{\"value\":\"DIRETORIA\",\"label\":\"Diretoria\"},{\"value\":\"COORDENADORIA\",\"label\":\"Coordenadoria\"},{\"value\":\"NUCLEO\",\"label\":\"Núcleo\"},{\"value\":\"UNIDADE_SAUDE\",\"label\":\"Unidade de Saúde\"},{\"value\":\"ESCOLA\",\"label\":\"Unidade Escolar\"}]")
  @Filterable(operation = FilterOperation.IN, relation = "designacao.setor.tipoSetor")
  private List<String> tipoSetor;

  @Schema(description = "Filtra substituições pela categoria funcional do vínculo substituto (ex.: ADMINISTRATIVO, MAGISTERIO, SAUDE, GUARDA_MUNICIPAL).")
  @UISchema(label = "Categorias do substituto", helpText = "Selecione as categorias funcionais do substituto.",
      type = FieldDataType.ARRAY, controlType = FieldControlType.MULTI_SELECT, icon = "badge", preset = UISchemaPreset.ENTERPRISE_STATUS,
      options = "[{\"value\":\"ADMINISTRATIVO\",\"label\":\"Administrativo\"},{\"value\":\"MAGISTERIO\",\"label\":\"Magistério\"},{\"value\":\"SAUDE\",\"label\":\"Saúde\"},{\"value\":\"GUARDA_MUNICIPAL\",\"label\":\"Guarda Municipal\"}]")
  @Filterable(operation = FilterOperation.IN, relation = "substituto.categoria")
  private List<String> categoriaSubstituto;

  @CustomFields(resourceKey = ApiPaths.KEY_SUBSTITUICOES)
  @Schema(description = "Critérios sobre atributos institucionais publicados para o cadastro de origem.")
  @UISchema(label = "Campos customizados", helpText = "Selecione os atributos do órgão usados como critérios.",
      type = FieldDataType.JSON, controlType = FieldControlType.INPUT, icon = "extension")
  private Map<String, Object> customFields = new LinkedHashMap<>();
}
