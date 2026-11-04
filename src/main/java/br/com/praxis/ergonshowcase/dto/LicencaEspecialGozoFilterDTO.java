package br.com.praxis.ergonshowcase.dto;
import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.*;
import lombok.*;
import org.praxisplatform.uischema.*;
import org.praxisplatform.uischema.customfields.CustomFields;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.filter.annotation.Filterable;
import org.praxisplatform.uischema.filter.annotation.Filterable.FilterOperation;
@Getter @Setter
@Schema(description = "Pesquisa de parcelas de licença especial por período, duração, abono e programação recente.")
public class LicencaEspecialGozoFilterDTO extends TempoLicencaFilterDTO {
  @Schema(description = "Pesquisa parcelas de licença pela descrição da fruição, nome do titular, matrícula ou observações.")
  @UISchema(label = "Busca ampla", helpText = "Pesquise a parcela ou seu titular.", controlType = FieldControlType.INPUT, icon = "search")
  @Filterable(operation = FilterOperation.LIKE, relations = {"descricao", "pessoa.nome", "vinculo.matric", "vinculo.lotacao.nome", "observacoes"}) private String searchTerm;
  @Schema(description = "Órgão responsável pelo vínculo da parcela, combinado com o escopo do serviço.")
  @UISchema(label = "Órgão", helpText = "Selecione o órgão da fruição.", type = FieldDataType.NUMBER, controlType = FieldControlType.ENTITY_LOOKUP, endpoint = br.com.praxis.ergonshowcase.constants.ApiPaths.ORGAO_OPTIONS, valueField = "id", displayField = "label", icon = "apartment")
  @Filterable(operation = FilterOperation.EQUAL) private Integer empCodigo;
  @Schema(description = "Número funcional da pessoa titular da parcela de licença especial.")
  @UISchema(label = "Servidor", helpText = "Selecione o titular da parcela.", type = FieldDataType.NUMBER, controlType = FieldControlType.ENTITY_LOOKUP, endpoint = br.com.praxis.ergonshowcase.constants.ApiPaths.PESSOA_OPTIONS, valueField = "id", displayField = "label", icon = "person")
  @Filterable(operation = FilterOperation.EQUAL) private Long numfunc;
  @Schema(description = "Sequencial do vínculo da fruição; combine com o número funcional.")
  @UISchema(label = "Sequencial do vínculo", helpText = "Informe o vínculo da parcela.", icon = "badge")
  @Filterable(operation = FilterOperation.EQUAL) private Integer numvinc;
  @Schema(description = "Prefixo da identificação da parcela de fruição.")
  @UISchema(label = "Descrição começa com", helpText = "Digite o início da identificação da parcela.", icon = "text_fields")
  @Filterable(operation = FilterOperation.STARTS_WITH, relation = "descricao") private String descricaoPrefixo;
  @Schema(description = "Estado operacional exato da parcela de fruição, da programação ao encerramento ou cancelamento.", allowableValues = {"PROGRAMADA", "HOMOLOGADA", "EM_GOZO", "GOZADA", "INTERROMPIDA", "CANCELADO"})
  @UISchema(label = "Situação cadastral", helpText = "Selecione a situação da parcela.", controlType = FieldControlType.SELECT, icon = "flag")
  @Filterable(operation = FilterOperation.EQUAL) private String situacao;
  @Schema(description = "Inclui parcelas em qualquer uma das situações informadas.", allowableValues = {"PROGRAMADA", "HOMOLOGADA", "EM_GOZO", "GOZADA", "INTERROMPIDA", "CANCELADO"})
  @UISchema(label = "Situações incluídas", helpText = "Escolha as situações pesquisadas.", icon = "filter_list")
  @Filterable(operation = FilterOperation.IN, relation = "situacao") private List<String> situacoes;
  @Schema(description = "Exclui parcelas nas situações informadas.", allowableValues = {"PROGRAMADA", "HOMOLOGADA", "EM_GOZO", "GOZADA", "INTERROMPIDA", "CANCELADO"})
  @UISchema(label = "Situações excluídas", helpText = "Escolha as situações que não devem aparecer.", icon = "filter_alt_off")
  @Filterable(operation = FilterOperation.NOT_IN, relation = "situacao") private List<String> situacoesExcluidas;
  @Schema(description = "Janela inclusiva de datas de início das parcelas de fruição.")
  @UISchema(label = "Gozo iniciado entre", helpText = "Informe os limites da janela de início do gozo.", type = FieldDataType.DATE, controlType = FieldControlType.DATE_RANGE, icon = "date_range")
  @Filterable(operation = FilterOperation.BETWEEN, relation = "dtini") private List<java.time.LocalDate> inicioEntre;
  @Schema(description = "Data mínima inclusiva de início do gozo da licença.")
  @UISchema(label = "Gozo iniciado desde", helpText = "Informe a primeira data de início desejada.", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, icon = "event")
  @Filterable(operation = FilterOperation.GREATER_OR_EQUAL, relation = "dtini") private java.time.LocalDate inicioMinimo;
  @Schema(description = "Data máxima inclusiva de encerramento efetivo da fruição.")
  @UISchema(label = "Gozo encerrado até", helpText = "Informe o último término efetivo desejado.", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, icon = "event_available")
  @Filterable(operation = FilterOperation.LESS_OR_EQUAL, relation = "dtfim") private java.time.LocalDate terminoMaximo;
  @Schema(description = "Seleciona as parcelas pertencentes ao período aquisitivo informado.")
  @UISchema(label = "Período aquisitivo", helpText = "Identidade do ciclo de licença especial.", type = FieldDataType.NUMBER, controlType = FieldControlType.ENTITY_LOOKUP, endpoint = br.com.praxis.ergonshowcase.constants.ApiPaths.PERIODO_AQUISITIVO_LICENCA_ESPECIAL_OPTIONS, valueField = "id", displayField = "label", icon = "date_range")
  @Filterable(operation = FilterOperation.EQUAL) private Long periodoAquisitivoLicencaId;
  @Schema(description = "Seleciona parcelas pelo espelho da chave de negócio do ciclo.")
  @UISchema(label = "Chave do período", helpText = "Combine com pessoa e vínculo para localizar o ciclo.", icon = "tag")
  @Filterable(operation = FilterOperation.EQUAL) private Integer chavePeraqlicesp;
  @Schema(description = "Seleciona parcelas com ou sem abono pecuniário associado.")
  @UISchema(label = "Com abono", helpText = "Informe se a parcela possui abono.", controlType = FieldControlType.CHECKBOX, icon = "payments")
  @Filterable(operation = FilterOperation.EQUAL) private Boolean abonoPecuniario;
  @Schema(description = "Seleciona parcelas cujo início ocorreu na janela recente informada em dias.")
  @UISchema(label = "Gozo iniciado recentemente", helpText = "Quantidade de dias da janela passada.", icon = "history")
  @Filterable(operation = FilterOperation.IN_LAST_DAYS, relation = "dtini") private Integer inicioUltimosDias;
  @Schema(description = "Seleciona parcelas sem data de encerramento efetivo.")
  @UISchema(label = "Sem término efetivo", helpText = "Inspecione parcelas ainda sem encerramento registrado.", controlType = FieldControlType.CHECKBOX, icon = "pending")
  @Filterable(operation = FilterOperation.IS_NULL, relation = "dtfim") private Boolean semTerminoEfetivo;
  @Schema(description = "Exclui uma ordem de parcela específica na pesquisa do ciclo.")
  @UISchema(label = "Excluir ordem da parcela", helpText = "Ordem que não deve aparecer na pesquisa.", icon = "filter_alt_off")
  @Filterable(operation = FilterOperation.NOT_EQUAL, relation = "ordemParcela") private Integer ordemDiferente;
  @CustomFields(resourceKey = "administracao-pessoal.licencas-especiais-gozo")
  @Schema(description = "Predicados sobre extensões institucionais das parcelas de licença especial.")
  @UISchema(label = "Campos customizados", helpText = "Pesquise atributos adicionais da parcela.", icon = "extension")
  private Map<String, Object> customFields = new LinkedHashMap<>();
  @Schema(description = "Identidade composta pessoa:número do vínculo; mantém a relação pai-filho completa nas superfícies 360°.")
  @UISchema(label = "Vínculo funcional", type = FieldDataType.TEXT, controlType = FieldControlType.ENTITY_LOOKUP, endpoint = ApiPaths.VINCULO_OPTIONS, valueField = "id", displayField = "label", icon = "badge", tableHidden = true)
  @Filterable(operation = FilterOperation.EQUAL, relation = "vinculo.id") private br.com.praxis.ergonshowcase.domain.VinculoId vinculoId;
  @Schema(description = "Trecho do nome da unidade organizacional de lotação do vínculo titular da parcela (ex.: Saúde, Educação, Fazenda).")
  @UISchema(label = "Nome da unidade", helpText = "Pesquise pelo nome da secretaria ou setor de lotação.", controlType = FieldControlType.INPUT, icon = "account_tree")
  @Filterable(operation = FilterOperation.LIKE, relation = "vinculo.lotacao.nome") private String setorLotacaoNome;
  @Schema(description = "Classificação estrutural da unidade de lotação do vínculo titular (ex.: SECRETARIA, DIRETORIA, HOSPITAL, UNIDADE_ENSINO).")
  @UISchema(label = "Tipo de unidade", helpText = "Filtra pela classificação da unidade de lotação.", controlType = FieldControlType.INPUT, icon = "domain")
  @Filterable(operation = FilterOperation.EQUAL, relation = "vinculo.lotacao.tipoSetor") private String tipoSetorLotacao;
  @Schema(description = "Categoria funcional do vínculo titular da parcela (ex.: SAUDE, MAGISTERIO, GERAL, GUARDA_MUNICIPAL).")
  @UISchema(label = "Categoria funcional", helpText = "Filtra pela carreira ou categoria do vínculo.", controlType = FieldControlType.INPUT, icon = "category")
  @Filterable(operation = FilterOperation.EQUAL, relation = "vinculo.categoria") private String categoriaVinculo;
}
