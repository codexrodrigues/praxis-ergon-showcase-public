package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.domain.VinculoId;
import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Pesquisa e recortes operacionais. Posições numeradas autorizadas por função, órgão e unidade, com origem e situação de ocupação.")
public class VagaFuncaoFilterDTO implements GenericFilterDTO {
  @Schema(description = "Pesquisa vagas de função por identificação e dados vinculados publicados na consulta.")
  @UISchema(label = "Busca ampla", helpText = "Digite parte do nome ou da identificação.",
      type = FieldDataType.TEXT, controlType = FieldControlType.SEARCH_INPUT, icon = "search", preset = UISchemaPreset.NONE)
  @Filterable(operation = FilterOperation.LIKE, relations = {"descricao", "origemVaga", "funcao.codigo", "funcao.descricao", "setor.nome", "orgao.nome"})
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
      type = FieldDataType.ARRAY, controlType = FieldControlType.MULTI_SELECT, icon = "flag", preset = UISchemaPreset.ENTERPRISE_STATUS, options = "[{\"value\":\"LIVRE\",\"label\":\"Livre\"},{\"value\":\"OCUPADA\",\"label\":\"Ocupada\"},{\"value\":\"BLOQUEADA\",\"label\":\"Bloqueada\"},{\"value\":\"EXTINTA\",\"label\":\"Extinta\"}]")
  @Filterable(operation = FilterOperation.IN, relation = "situacao")
  private List<String> situacoes;

  @Schema(description = "Exclui os registros nas situações selecionadas.")
  @UISchema(label = "Situações excluídas", helpText = "Escolha os estados a retirar do resultado.",
      type = FieldDataType.ARRAY, controlType = FieldControlType.MULTI_SELECT, icon = "flag", preset = UISchemaPreset.ENTERPRISE_STATUS, options = "[{\"value\":\"LIVRE\",\"label\":\"Livre\"},{\"value\":\"OCUPADA\",\"label\":\"Ocupada\"},{\"value\":\"BLOQUEADA\",\"label\":\"Bloqueada\"},{\"value\":\"EXTINTA\",\"label\":\"Extinta\"}]")
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

  @Schema(description = "Limite inferior inclusivo do número da vaga.")
  @UISchema(label = "Número mínimo", helpText = "Use para recortar uma série de vagas.",
      type = FieldDataType.NUMBER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "tag", preset = UISchemaPreset.NONE)
  @Filterable(operation = FilterOperation.GREATER_OR_EQUAL, relation = "numeroVaga")
  private Long numeroVagaMin;

  @Schema(description = "Órgão responsável pelo cadastro e pela gestão funcional deste registro.")
  @UISchema(label = "Órgão", helpText = "Selecione o órgão ao qual pertence o registro.",
      type = FieldDataType.NUMBER, controlType = FieldControlType.ENTITY_LOOKUP, icon = "apartment", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.ORGAO_OPTIONS, valueField = "id", displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL)
  private Integer empCodigo;

  @Schema(description = "Data em que a posição numerada passa a compor o quadro autorizado.")
  @UISchema(label = "Início da vigência", helpText = "Use a data inicial que consta no ato.",
      type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, icon = "event", preset = UISchemaPreset.DATE_RANGE)
  @Filterable(operation = FilterOperation.EQUAL)
  private LocalDate dtini;

  @Schema(description = "Último dia em que a posição integra o quadro da função, inclusive.")
  @UISchema(label = "Fim da vigência", helpText = "Deixe em branco enquanto a vigência estiver aberta.",
      type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, icon = "event", preset = UISchemaPreset.DATE_RANGE)
  @Filterable(operation = FilterOperation.EQUAL)
  private LocalDate dtfim;

  @Schema(description = "Posição cadastral da vaga quanto à ocupação, bloqueio ou extinção.")
  @UISchema(label = "Situação", helpText = "Selecione a posição cadastral documentada.",
      type = FieldDataType.TEXT, controlType = FieldControlType.SELECT, icon = "flag", preset = UISchemaPreset.ENTERPRISE_STATUS, options = "[{\"value\":\"LIVRE\",\"label\":\"Livre\"},{\"value\":\"OCUPADA\",\"label\":\"Ocupada\"},{\"value\":\"BLOQUEADA\",\"label\":\"Bloqueada\"},{\"value\":\"EXTINTA\",\"label\":\"Extinta\"}]")
  @Filterable(operation = FilterOperation.EQUAL)
  private String situacao;

  @Schema(description = "Número de até doze dígitos atribuído à posição autorizada para ocupação da função no órgão.")
  @UISchema(label = "Número da vaga", helpText = "Use o número publicado na distribuição de vagas.",
      type = FieldDataType.NUMBER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "tag", preset = UISchemaPreset.NONE)
  @Filterable(operation = FilterOperation.EQUAL)
  private Long numeroVaga;

  @Schema(description = "Função de confiança para a qual a vaga numerada foi autorizada.")
  @UISchema(label = "Função autorizada", helpText = "Escolha a função que consta no ato.",
      type = FieldDataType.NUMBER, controlType = FieldControlType.ENTITY_LOOKUP, icon = "link", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.FUNCAO_OPTIONS, valueField = "id", displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL)
  private Long funcaoId;

  @Schema(description = "Unidade organizacional à qual a posição foi distribuída.")
  @UISchema(label = "Unidade da vaga", helpText = "Selecione a unidade beneficiária da distribuição.",
      type = FieldDataType.TEXT, controlType = FieldControlType.ENTITY_LOOKUP, icon = "link", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.SETOR_OPTIONS, valueField = "id", displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL)
  private String setorId;

  @Schema(description = "Classificação administrativa da origem da vaga, como criação por lei ou redistribuição.")
  @UISchema(label = "Origem da vaga", helpText = "Identifique a origem prevista no ato.",
      type = FieldDataType.TEXT, controlType = FieldControlType.INPUT, icon = "notes", preset = UISchemaPreset.NONE)
  @Filterable(operation = FilterOperation.EQUAL)
  private String origemVaga;

  @Schema(description = "Vínculo registrado como ocupante atual da posição numerada.")
  @UISchema(label = "Titular atual", helpText = "Selecione o vínculo que ocupa a vaga.",
      type = FieldDataType.TEXT, controlType = FieldControlType.ENTITY_LOOKUP, icon = "link", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.VINCULO_OPTIONS, valueField = "id", displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL)
  private VinculoId titularAtualId;

  @Schema(description = "Fundamento administrativo que impede o uso de uma vaga ainda vigente.")
  @UISchema(label = "Motivo do bloqueio", helpText = "Detalhe a restrição de ocupação.",
      type = FieldDataType.TEXT, controlType = FieldControlType.INPUT, icon = "notes", preset = UISchemaPreset.NONE)
  @Filterable(operation = FilterOperation.EQUAL)
  private String motivoBloqueio;

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

  @Schema(description = "Trecho do nome da unidade organizacional à qual a vaga foi distribuída (ex.: Saúde, Educação, Fazenda).")
  @UISchema(label = "Nome da unidade", helpText = "Pesquise vagas pelo nome da secretaria ou setor.",
      type = FieldDataType.TEXT, controlType = FieldControlType.INPUT, icon = "account_tree", preset = UISchemaPreset.NONE)
  @Filterable(operation = FilterOperation.LIKE, relation = "setor.nome")
  private String setorNome;

  @Schema(description = "Classificação estrutural da unidade à qual a vaga foi distribuída (ex.: SECRETARIA, DIRETORIA, HOSPITAL, UNIDADE_ENSINO).")
  @UISchema(label = "Tipo de unidade", helpText = "Filtra vagas pela classificação da unidade.",
      type = FieldDataType.TEXT, controlType = FieldControlType.INPUT, icon = "domain", preset = UISchemaPreset.NONE)
  @Filterable(operation = FilterOperation.EQUAL, relation = "setor.tipoSetor")
  private String tipoSetor;

  @Schema(description = "Seleciona vagas que não possuem titular ocupante atual registrado.")
  @UISchema(label = "Sem titular atual", helpText = "Exibe vagas sem ocupante atual.",
      type = FieldDataType.BOOLEAN, controlType = FieldControlType.CHECKBOX, icon = "person_off", preset = UISchemaPreset.BOOLEAN_FLAG, labelTrue = "Sim", labelFalse = "Não", toneTrue = "info", toneFalse = "neutral")
  @Filterable(operation = FilterOperation.IS_NULL, relation = "titularAtualId.numfunc")
  private Boolean semTitularAtual;

  @CustomFields(resourceKey = ApiPaths.KEY_VAGAS_FUNCAO)
  @Schema(description = "Critérios sobre atributos institucionais publicados para o cadastro de origem.")
  @UISchema(label = "Campos customizados", helpText = "Selecione os atributos do órgão usados como critérios.",
      type = FieldDataType.JSON, controlType = FieldControlType.INPUT, icon = "extension")
  private Map<String, Object> customFields = new LinkedHashMap<>();
}
