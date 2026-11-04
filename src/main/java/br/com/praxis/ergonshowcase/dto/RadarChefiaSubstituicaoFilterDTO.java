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
@Schema(description = "Pesquisa e recortes operacionais. Posição de cobertura da chefia na data de referência. Distingue titular em exercício, impedimento coberto e chefia descoberta.")
public class RadarChefiaSubstituicaoFilterDTO implements GenericFilterDTO {
  @Schema(description = "Pesquisa radar de chefias por identificação e dados vinculados publicados na consulta.")
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
      type = FieldDataType.ARRAY, controlType = FieldControlType.MULTI_SELECT, icon = "flag", preset = UISchemaPreset.ENTERPRISE_STATUS, options = "[{\"value\":\"TITULAR_EM_EXERCICIO\",\"label\":\"Titular em exercício\"},{\"value\":\"COBERTA\",\"label\":\"Coberta\"},{\"value\":\"DESCOBERTA\",\"label\":\"Descoberta\"}]")
  @Filterable(operation = FilterOperation.IN, relation = "situacao")
  private List<String> situacoes;

  @Schema(description = "Exclui os registros nas situações selecionadas.")
  @UISchema(label = "Situações excluídas", helpText = "Escolha os estados a retirar do resultado.",
      type = FieldDataType.ARRAY, controlType = FieldControlType.MULTI_SELECT, icon = "flag", preset = UISchemaPreset.ENTERPRISE_STATUS, options = "[{\"value\":\"TITULAR_EM_EXERCICIO\",\"label\":\"Titular em exercício\"},{\"value\":\"COBERTA\",\"label\":\"Coberta\"},{\"value\":\"DESCOBERTA\",\"label\":\"Descoberta\"}]")
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

  @Schema(description = "Quantidade mínima de exercícios interinos pendentes de folha por designação.")
  @UISchema(label = "Pendências mínimas", helpText = "Use 1 para localizar chefias com pendência.",
      type = FieldDataType.NUMBER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "tag", preset = UISchemaPreset.NONE)
  @Filterable(operation = FilterOperation.GREATER_OR_EQUAL, relation = "pendenciasFolha")
  private Integer pendenciasFolhaMin;

  @Schema(description = "Órgão responsável pelo cadastro e pela gestão funcional deste registro.")
  @UISchema(label = "Órgão", helpText = "Selecione o órgão ao qual pertence o registro.",
      type = FieldDataType.NUMBER, controlType = FieldControlType.ENTITY_LOOKUP, icon = "apartment", preset = UISchemaPreset.TENANT_LABEL, endpoint = ApiPaths.ORGAO_OPTIONS, valueField = "id", displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL)
  private Integer empCodigo;

  @Schema(description = "Primeiro dia de exercício da designação titular.")
  @UISchema(label = "Início da vigência", helpText = "Use a data inicial que consta no ato.",
      type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, icon = "event", preset = UISchemaPreset.DATE_RANGE)
  @Filterable(operation = FilterOperation.EQUAL)
  private LocalDate dtini;

  @Schema(description = "Último dia abrangido pelo ato da chefia, inclusive, quando informado.")
  @UISchema(label = "Fim da vigência", helpText = "Deixe em branco enquanto a vigência estiver aberta.",
      type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, icon = "event", preset = UISchemaPreset.DATE_RANGE)
  @Filterable(operation = FilterOperation.EQUAL)
  private LocalDate dtfim;

  @Schema(description = "Posição de cobertura na data de referência, obtida do impedimento do titular e dos exercícios interinos vigentes.")
  @UISchema(label = "Situação", helpText = "Selecione a posição cadastral documentada.",
      type = FieldDataType.TEXT, controlType = FieldControlType.SELECT, icon = "flag", preset = UISchemaPreset.ENTERPRISE_STATUS, options = "[{\"value\":\"TITULAR_EM_EXERCICIO\",\"label\":\"Titular em exercício\"},{\"value\":\"COBERTA\",\"label\":\"Coberta\"},{\"value\":\"DESCOBERTA\",\"label\":\"Descoberta\"}]")
  @Filterable(operation = FilterOperation.EQUAL)
  private String situacao;

  @Schema(description = "Função de chefia ou direção exercida na designação acompanhada.")
  @UISchema(label = "Função monitorada", helpText = "Abra o cadastro da função.",
      type = FieldDataType.NUMBER, controlType = FieldControlType.ENTITY_LOOKUP, icon = "link", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.FUNCAO_OPTIONS, valueField = "id", displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL)
  private Long funcaoId;

  @Schema(description = "Setor pelo qual responde a chefia acompanhada.")
  @UISchema(label = "Unidade monitorada", helpText = "Abra a ficha da unidade.",
      type = FieldDataType.TEXT, controlType = FieldControlType.ENTITY_LOOKUP, icon = "link", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.SETOR_OPTIONS, valueField = "id", displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL)
  private String setorId;

  @Schema(description = "Ato em exercício usado para identificar a chefia na data de referência.")
  @UISchema(label = "Designação titular", helpText = "Abra a designação de origem.",
      type = FieldDataType.NUMBER, controlType = FieldControlType.ENTITY_LOOKUP, icon = "link", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.DESIGNACAO_OPTIONS, valueField = "id", displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL)
  private Long designacaoId;

  @Schema(description = "Posição numerada vinculada à designação monitorada.")
  @UISchema(label = "Vaga da chefia", helpText = "Abra a vaga registrada no ato.",
      type = FieldDataType.NUMBER, controlType = FieldControlType.ENTITY_LOOKUP, icon = "link", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.VAGA_FUNCAO_OPTIONS, valueField = "id", displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL)
  private Long vagaFuncaoId;

  @Schema(description = "Vínculo que detém a titularidade da função na data de referência.")
  @UISchema(label = "Vínculo do titular", helpText = "Abra o resumo funcional do titular.",
      type = FieldDataType.TEXT, controlType = FieldControlType.ENTITY_LOOKUP, icon = "link", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.VINCULO_OPTIONS, valueField = "id", displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL)
  private VinculoId titularId;

  @Schema(description = "Exercício interino vigente na data de referência. Quando há mais de um, exibe o que começou mais recentemente.")
  @UISchema(label = "Substituição vigente", helpText = "Abra o exercício interino apresentado.",
      type = FieldDataType.NUMBER, controlType = FieldControlType.ENTITY_LOOKUP, icon = "link", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.SUBSTITUICAO_OPTIONS, valueField = "id", displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL)
  private Long substituicaoId;

  @Schema(description = "Afastamento vigente do vínculo titular na data de referência, com precedência para o mais recente.")
  @UISchema(label = "Afastamento vigente", helpText = "Abra o registro de impedimento.",
      type = FieldDataType.NUMBER, controlType = FieldControlType.ENTITY_LOOKUP, icon = "link", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.AFASTAMENTO_OPTIONS, valueField = "id", displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL)
  private Long afastamentoId;

  @Schema(description = "Parcela de férias que cobre a data de referência para o vínculo titular.")
  @UISchema(label = "Férias vigentes", helpText = "Abra a parcela de gozo de origem.",
      type = FieldDataType.NUMBER, controlType = FieldControlType.ENTITY_LOOKUP, icon = "link", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.FERIAS_GOZO_OPTIONS, valueField = "id", displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL)
  private Long feriasGozoId;

  @Schema(description = "Data civil usada para avaliar vigência e impedimentos no radar.")
  @UISchema(label = "Data de referência", helpText = "Confira a data da posição exibida.",
      type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, icon = "event", preset = UISchemaPreset.DATE_RANGE)
  @Filterable(operation = FilterOperation.EQUAL)
  private LocalDate dataReferencia;

  @Schema(description = "Número de exercícios interinos registrados como vigentes na data de referência para a mesma designação.")
  @UISchema(label = "Substituições vigentes", helpText = "Verifique sobreposição quando houver mais de um exercício.",
      type = FieldDataType.NUMBER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "tag", preset = UISchemaPreset.NONE)
  @Filterable(operation = FilterOperation.EQUAL)
  private Integer qtdCoberturas;

  @Schema(description = "Quantidade de substituições da designação com apuração aguardando processamento em folha.")
  @UISchema(label = "Pendências de folha", helpText = "Priorize o tratamento dos registros pendentes.",
      type = FieldDataType.NUMBER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "tag", preset = UISchemaPreset.NONE)
  @Filterable(operation = FilterOperation.EQUAL)
  private Integer pendenciasFolha;

  @Schema(description = "Pesquisa textual pelo nome oficial da unidade monitorada no radar de chefias (ex.: UPA, Hospital, Secretaria de Saúde).", example = "Saúde")
  @UISchema(label = "Nome da unidade", helpText = "Digite parte do nome da unidade monitorada.",
      type = FieldDataType.TEXT, controlType = FieldControlType.INPUT, icon = "account_tree", preset = UISchemaPreset.NONE)
  @Filterable(operation = FilterOperation.LIKE, relation = "setor.nome")
  private String setorNome;

  @Schema(description = "Filtra chefias monitoradas pela natureza operacional da unidade organizacional (ex.: SECRETARIA, DIRETORIA, COORDENADORIA, NUCLEO, UNIDADE_SAUDE, ESCOLA).")
  @UISchema(label = "Tipos de unidade", helpText = "Selecione os tipos de unidade organizacional.",
      type = FieldDataType.ARRAY, controlType = FieldControlType.MULTI_SELECT, icon = "category", preset = UISchemaPreset.ENTERPRISE_STATUS,
      options = "[{\"value\":\"SECRETARIA\",\"label\":\"Secretaria\"},{\"value\":\"DIRETORIA\",\"label\":\"Diretoria\"},{\"value\":\"COORDENADORIA\",\"label\":\"Coordenadoria\"},{\"value\":\"NUCLEO\",\"label\":\"Núcleo\"},{\"value\":\"UNIDADE_SAUDE\",\"label\":\"Unidade de Saúde\"},{\"value\":\"ESCOLA\",\"label\":\"Unidade Escolar\"}]")
  @Filterable(operation = FilterOperation.IN, relation = "setor.tipoSetor")
  private List<String> tipoSetor;

  @Schema(description = "Filtra chefias pela categoria funcional do vínculo titular (ex.: ADMINISTRATIVO, MAGISTERIO, SAUDE, GUARDA_MUNICIPAL).")
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
