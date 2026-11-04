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
@Schema(description = "Pesquisa e recortes operacionais. Atos individuais de designação, exercício e dispensa em função de confiança, incluindo opção remuneratória e ordem de substitutos.")
public class DesignacaoFuncaoFilterDTO implements GenericFilterDTO {
  @Schema(description = "Pesquisa designações por identificação e dados vinculados publicados na consulta.")
  @UISchema(label = "Busca ampla", helpText = "Digite parte do nome ou da identificação.",
      type = FieldDataType.TEXT, controlType = FieldControlType.SEARCH_INPUT, icon = "search", preset = UISchemaPreset.NONE)
  @Filterable(operation = FilterOperation.LIKE, relations = {"descricao", "titular.pessoa.nome", "funcao.descricao", "funcao.codigo", "setor.nome"})
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
      type = FieldDataType.ARRAY, controlType = FieldControlType.MULTI_SELECT, icon = "flag", preset = UISchemaPreset.ENTERPRISE_STATUS, options = "[{\"value\":\"DESIGNADA\",\"label\":\"Designada\"},{\"value\":\"EM_EXERCICIO\",\"label\":\"Em exercício\"},{\"value\":\"SUSPENSA\",\"label\":\"Suspensa\"},{\"value\":\"DISPENSADA\",\"label\":\"Dispensada\"}]")
  @Filterable(operation = FilterOperation.IN, relation = "situacao")
  private List<String> situacoes;

  @Schema(description = "Exclui os registros nas situações selecionadas.")
  @UISchema(label = "Situações excluídas", helpText = "Escolha os estados a retirar do resultado.",
      type = FieldDataType.ARRAY, controlType = FieldControlType.MULTI_SELECT, icon = "flag", preset = UISchemaPreset.ENTERPRISE_STATUS, options = "[{\"value\":\"DESIGNADA\",\"label\":\"Designada\"},{\"value\":\"EM_EXERCICIO\",\"label\":\"Em exercício\"},{\"value\":\"SUSPENSA\",\"label\":\"Suspensa\"},{\"value\":\"DISPENSADA\",\"label\":\"Dispensada\"}]")
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

  @Schema(description = "Seleciona atos com data de exercício registrada.")
  @UISchema(label = "Com entrada em exercício", helpText = "Marque para exigir o registro da entrada.",
      type = FieldDataType.BOOLEAN, controlType = FieldControlType.CHECKBOX, icon = "fact_check", preset = UISchemaPreset.BOOLEAN_FLAG, labelTrue = "Sim", labelFalse = "Não", toneTrue = "info", toneFalse = "neutral")
  @Filterable(operation = FilterOperation.IS_NOT_NULL, relation = "dtExercicio")
  private Boolean comExercicio;

  @Schema(description = "Órgão responsável pelo cadastro e pela gestão funcional deste registro.")
  @UISchema(label = "Órgão", helpText = "Selecione o órgão ao qual pertence o registro.",
      type = FieldDataType.NUMBER, controlType = FieldControlType.ENTITY_LOOKUP, icon = "apartment", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.ORGAO_OPTIONS, valueField = "id", displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL)
  private Integer empCodigo;

  @Schema(description = "Data de início dos efeitos do ato de designação, independente da entrada em exercício.")
  @UISchema(label = "Início da vigência", helpText = "Use a data inicial que consta no ato.",
      type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, icon = "event", preset = UISchemaPreset.DATE_RANGE)
  @Filterable(operation = FilterOperation.EQUAL)
  private LocalDate dtini;

  @Schema(description = "Último dia abrangido pela designação, inclusive, quando houver dispensa ou prazo determinado.")
  @UISchema(label = "Fim da vigência", helpText = "Deixe em branco enquanto a vigência estiver aberta.",
      type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, icon = "event", preset = UISchemaPreset.DATE_RANGE)
  @Filterable(operation = FilterOperation.EQUAL)
  private LocalDate dtfim;

  @Schema(description = "Posição do ato entre designação publicada, exercício, suspensão e dispensa.")
  @UISchema(label = "Situação", helpText = "Selecione a posição cadastral documentada.",
      type = FieldDataType.TEXT, controlType = FieldControlType.SELECT, icon = "flag", preset = UISchemaPreset.ENTERPRISE_STATUS, options = "[{\"value\":\"DESIGNADA\",\"label\":\"Designada\"},{\"value\":\"EM_EXERCICIO\",\"label\":\"Em exercício\"},{\"value\":\"SUSPENSA\",\"label\":\"Suspensa\"},{\"value\":\"DISPENSADA\",\"label\":\"Dispensada\"}]")
  @Filterable(operation = FilterOperation.EQUAL)
  private String situacao;

  @Schema(description = "Vínculo em que o servidor recebeu o ato de designação para a função.")
  @UISchema(label = "Vínculo do titular", helpText = "Escolha a matrícula e o vínculo do titular.",
      type = FieldDataType.TEXT, controlType = FieldControlType.ENTITY_LOOKUP, icon = "link", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.VINCULO_OPTIONS, valueField = "id", displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL)
  private VinculoId titularId;

  @Schema(description = "Função de confiança objeto do ato de designação.")
  @UISchema(label = "Função designada", helpText = "Selecione a função indicada no ato.",
      type = FieldDataType.NUMBER, controlType = FieldControlType.ENTITY_LOOKUP, icon = "link", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.FUNCAO_OPTIONS, valueField = "id", displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL)
  private Long funcaoId;

  @Schema(description = "Posição numerada ocupada pelo ato quando a função usa controle de vagas.")
  @UISchema(label = "Vaga vinculada", helpText = "Filtre pela função e selecione a vaga do ato.",
      type = FieldDataType.NUMBER, controlType = FieldControlType.ENTITY_LOOKUP, icon = "link", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.VAGA_FUNCAO_OPTIONS, valueField = "id", displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL)
  private Long vagaFuncaoId;

  @Schema(description = "Setor onde o titular responde pelas atribuições da função designada.")
  @UISchema(label = "Unidade de exercício", helpText = "Selecione a unidade que recebe o titular.",
      type = FieldDataType.TEXT, controlType = FieldControlType.ENTITY_LOOKUP, icon = "link", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.SETOR_OPTIONS, valueField = "id", displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL)
  private String setorId;

  @Schema(description = "Primeiro dia de efetivo exercício na função, que pode ser posterior à publicação da designação.")
  @UISchema(label = "Início do exercício", helpText = "Informe a data do registro de entrada em exercício.",
      type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, icon = "event", preset = UISchemaPreset.DATE_RANGE)
  @Filterable(operation = FilterOperation.EQUAL)
  private LocalDate dtExercicio;

  @Schema(description = "Primeiro vínculo indicado no ato para responder durante impedimento do titular.")
  @UISchema(label = "Primeiro substituto", helpText = "Selecione o vínculo com precedência na substituição.",
      type = FieldDataType.TEXT, controlType = FieldControlType.ENTITY_LOOKUP, icon = "link", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.VINCULO_OPTIONS, valueField = "id", displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL)
  private VinculoId substituto1Id;

  @Schema(description = "Vínculo indicado em segunda ordem para responder na ausência do titular e do primeiro substituto.")
  @UISchema(label = "Segundo substituto", helpText = "Selecione o vínculo de segunda ordem.",
      type = FieldDataType.TEXT, controlType = FieldControlType.ENTITY_LOOKUP, icon = "link", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.VINCULO_OPTIONS, valueField = "id", displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL)
  private VinculoId substituto2Id;

  @Schema(description = "Escolha registrada entre remuneração integral da função e remuneração efetiva acrescida da opção percentual.")
  @UISchema(label = "Opção remuneratória", helpText = "Registre a escolha formal do servidor.",
      type = FieldDataType.TEXT, controlType = FieldControlType.SELECT, icon = "flag", preset = UISchemaPreset.ENTERPRISE_STATUS, options = "[{\"value\":\"INTEGRAL\",\"label\":\"Integral da função\"},{\"value\":\"VINCULO_EFETIVO\",\"label\":\"Opção pelo efetivo\"}]")
  @Filterable(operation = FilterOperation.EQUAL)
  private String opcaoRemuneracao;

  @Schema(description = "Valor mensal registrado para a função conforme a opção remuneratória do titular.")
  @UISchema(label = "Remuneração registrada", helpText = "Confira o valor que consta no assentamento.",
      type = FieldDataType.NUMBER, controlType = FieldControlType.CURRENCY_INPUT, icon = "payments", preset = UISchemaPreset.MONETARY_AMOUNT)
  @Filterable(operation = FilterOperation.EQUAL)
  private BigDecimal valorRemuneracao;

  @Schema(description = "Documento publicado que formaliza a dispensa do titular da função.")
  @UISchema(label = "Ato de dispensa", helpText = "Selecione o documento de encerramento.",
      type = FieldDataType.NUMBER, controlType = FieldControlType.ENTITY_LOOKUP, icon = "link", preset = UISchemaPreset.LEGAL_DOCUMENT_REFERENCE, endpoint = ApiPaths.DOCUMENTO_OPTIONS, valueField = "id", displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL)
  private Long atoDispensaId;

  @Schema(description = "Fundamento administrativo registrado para a cessação da designação.")
  @UISchema(label = "Motivo da dispensa", helpText = "Use o fundamento do ato de dispensa.",
      type = FieldDataType.TEXT, controlType = FieldControlType.INPUT, icon = "notes", preset = UISchemaPreset.NONE)
  @Filterable(operation = FilterOperation.EQUAL)
  private String motivoDispensa;

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

  @Schema(description = "Pesquisa textual pelo nome oficial da unidade de exercício da função (ex.: UPA, Hospital, Secretaria de Saúde).", example = "Saúde")
  @UISchema(label = "Nome da unidade", helpText = "Digite parte do nome da unidade de exercício.",
      type = FieldDataType.TEXT, controlType = FieldControlType.INPUT, icon = "account_tree", preset = UISchemaPreset.NONE)
  @Filterable(operation = FilterOperation.LIKE, relation = "setor.nome")
  private String setorNome;

  @Schema(description = "Filtra designações pela natureza operacional da unidade de exercício (ex.: SECRETARIA, DIRETORIA, COORDENADORIA, NUCLEO, UNIDADE_SAUDE, ESCOLA).")
  @UISchema(label = "Tipos de unidade", helpText = "Selecione os tipos de unidade organizacional.",
      type = FieldDataType.ARRAY, controlType = FieldControlType.MULTI_SELECT, icon = "category", preset = UISchemaPreset.ENTERPRISE_STATUS,
      options = "[{\"value\":\"SECRETARIA\",\"label\":\"Secretaria\"},{\"value\":\"DIRETORIA\",\"label\":\"Diretoria\"},{\"value\":\"COORDENADORIA\",\"label\":\"Coordenadoria\"},{\"value\":\"NUCLEO\",\"label\":\"Núcleo\"},{\"value\":\"UNIDADE_SAUDE\",\"label\":\"Unidade de Saúde\"},{\"value\":\"ESCOLA\",\"label\":\"Unidade Escolar\"}]")
  @Filterable(operation = FilterOperation.IN, relation = "setor.tipoSetor")
  private List<String> tipoSetor;

  @Schema(description = "Filtra designações pela categoria funcional do vínculo titular (ex.: ADMINISTRATIVO, MAGISTERIO, SAUDE, GUARDA_MUNICIPAL).")
  @UISchema(label = "Categorias do titular", helpText = "Selecione as categorias funcionais do titular.",
      type = FieldDataType.ARRAY, controlType = FieldControlType.MULTI_SELECT, icon = "badge", preset = UISchemaPreset.ENTERPRISE_STATUS,
      options = "[{\"value\":\"ADMINISTRATIVO\",\"label\":\"Administrativo\"},{\"value\":\"MAGISTERIO\",\"label\":\"Magistério\"},{\"value\":\"SAUDE\",\"label\":\"Saúde\"},{\"value\":\"GUARDA_MUNICIPAL\",\"label\":\"Guarda Municipal\"}]")
  @Filterable(operation = FilterOperation.IN, relation = "titular.categoria")
  private List<String> categoriaTitular;

  @CustomFields(resourceKey = ApiPaths.KEY_DESIGNACOES)
  @Schema(description = "Critérios sobre atributos institucionais publicados para o cadastro de origem.")
  @UISchema(label = "Campos customizados", helpText = "Selecione os atributos do órgão usados como critérios.",
      type = FieldDataType.JSON, controlType = FieldControlType.INPUT, icon = "extension")
  private Map<String, Object> customFields = new LinkedHashMap<>();
}
