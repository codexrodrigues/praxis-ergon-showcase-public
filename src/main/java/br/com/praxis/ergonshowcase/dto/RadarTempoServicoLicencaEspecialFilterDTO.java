package br.com.praxis.ergonshowcase.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.*;
import lombok.*;
import org.praxisplatform.uischema.*;
import org.praxisplatform.uischema.customfields.CustomFields;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.filter.annotation.Filterable;
import org.praxisplatform.uischema.filter.annotation.Filterable.FilterOperation;
@Getter @Setter
@Schema(description = "Recortes executivos de saldo, conversão, prescrição e cobertura de programação da licença especial.")
public class RadarTempoServicoLicencaEspecialFilterDTO extends TempoLicencaFilterDTO {
  @Schema(description = "Pesquisa ciclos monitorados por identificação do direito, nome do titular, matrícula, nome da lotação ou observações.")
  @UISchema(label = "Busca ampla", helpText = "Pesquise o ciclo monitorado, seu titular ou lotação.", controlType = FieldControlType.INPUT, icon = "search")
  @Filterable(operation = FilterOperation.LIKE, relations = {"descricao", "pessoa.nome", "vinculo.matric", "vinculo.lotacao.nome", "observacoes"}) private String searchTerm;
  @Schema(description = "Órgão responsável pelo vínculo monitorado, combinado com o escopo do serviço.")
  @UISchema(label = "Órgão", helpText = "Selecione o órgão dos ciclos monitorados.", type = FieldDataType.NUMBER, controlType = FieldControlType.ENTITY_LOOKUP, endpoint = br.com.praxis.ergonshowcase.constants.ApiPaths.ORGAO_OPTIONS, valueField = "id", displayField = "label", icon = "apartment")
  @Filterable(operation = FilterOperation.EQUAL) private Integer empCodigo;
  @Schema(description = "Número funcional da pessoa cujos direitos são monitorados.")
  @UISchema(label = "Servidor", helpText = "Selecione o titular dos ciclos.", type = FieldDataType.NUMBER, controlType = FieldControlType.ENTITY_LOOKUP, endpoint = br.com.praxis.ergonshowcase.constants.ApiPaths.PESSOA_OPTIONS, valueField = "id", displayField = "label", icon = "person")
  @Filterable(operation = FilterOperation.EQUAL) private Long numfunc;
  @Schema(description = "Sequencial do vínculo monitorado; combine com o número funcional.")
  @UISchema(label = "Sequencial do vínculo", helpText = "Informe o vínculo monitorado.", icon = "badge")
  @Filterable(operation = FilterOperation.EQUAL) private Integer numvinc;
  @Schema(description = "Prefixo da identificação do ciclo aquisitivo monitorado.")
  @UISchema(label = "Descrição começa com", helpText = "Digite o início da identificação do ciclo.", icon = "text_fields")
  @Filterable(operation = FilterOperation.STARTS_WITH, relation = "descricao") private String descricaoPrefixo;
  @Schema(description = "Estado operacional do período projetado no radar, com o mesmo vocabulário do ciclo de licença.", allowableValues = {"EM_AQUISICAO", "DISPONIVEL", "PARCIALMENTE_GOZADO", "CONVERTIDO_EM_DOBRO", "QUITADO", "SUSPENSO", "PRESCRITO", "CANCELADO"})
  @UISchema(label = "Situação cadastral", helpText = "Selecione a situação dos ciclos.", controlType = FieldControlType.SELECT, icon = "flag")
  @Filterable(operation = FilterOperation.EQUAL) private String situacao;
  @Schema(description = "Inclui ciclos monitorados em qualquer uma das situações informadas.", allowableValues = {"EM_AQUISICAO", "DISPONIVEL", "PARCIALMENTE_GOZADO", "CONVERTIDO_EM_DOBRO", "QUITADO", "SUSPENSO", "PRESCRITO", "CANCELADO"})
  @UISchema(label = "Situações incluídas", helpText = "Escolha as situações monitoradas.", icon = "filter_list")
  @Filterable(operation = FilterOperation.IN, relation = "situacao") private List<String> situacoes;
  @Schema(description = "Exclui ciclos nas situações informadas do conjunto monitorado.", allowableValues = {"EM_AQUISICAO", "DISPONIVEL", "PARCIALMENTE_GOZADO", "CONVERTIDO_EM_DOBRO", "QUITADO", "SUSPENSO", "PRESCRITO", "CANCELADO"})
  @UISchema(label = "Situações excluídas", helpText = "Escolha as situações que não devem aparecer.", icon = "filter_alt_off")
  @Filterable(operation = FilterOperation.NOT_IN, relation = "situacao") private List<String> situacoesExcluidas;
  @Schema(description = "Janela inclusiva de datas de início dos ciclos monitorados.")
  @UISchema(label = "Aquisição iniciada entre", helpText = "Informe os limites da janela de aquisição.", type = FieldDataType.DATE, controlType = FieldControlType.DATE_RANGE, icon = "date_range")
  @Filterable(operation = FilterOperation.BETWEEN, relation = "dtini") private List<java.time.LocalDate> inicioEntre;
  @Schema(description = "Data mínima inclusiva de início da aquisição nos ciclos monitorados.")
  @UISchema(label = "Aquisição iniciada desde", helpText = "Informe a primeira data de início desejada.", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, icon = "event")
  @Filterable(operation = FilterOperation.GREATER_OR_EQUAL, relation = "dtini") private java.time.LocalDate inicioMinimo;
  @Schema(description = "Data máxima inclusiva de encerramento da aquisição nos ciclos monitorados.")
  @UISchema(label = "Aquisição encerrada até", helpText = "Informe o último término de ciclo desejado.", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, icon = "event_available")
  @Filterable(operation = FilterOperation.LESS_OR_EQUAL, relation = "dtfim") private java.time.LocalDate terminoMaximo;
  @Schema(description = "Seleciona ciclos com saldo de licença maior que o valor informado.")
  @UISchema(label = "Saldo acima de", helpText = "Use zero para localizar ciclos com direito disponível.", icon = "donut_large")
  @Filterable(operation = FilterOperation.GREATER_THAN, relation = "saldoDias") private Integer saldoAcima;
  @Schema(description = "Seleciona ciclos com prescrição dentro da janela futura em dias.")
  @UISchema(label = "Prescrição próxima", helpText = "Informe a janela futura de atenção.", icon = "warning")
  @Filterable(operation = FilterOperation.IN_NEXT_DAYS, relation = "dtPrescricao") private Integer prescricaoProximosDias;
  @Schema(description = "Seleciona ciclos sem data de prescrição cadastrada.")
  @UISchema(label = "Sem prescrição informada", helpText = "Inspecione direitos cujo prazo ainda não está cadastrado.", controlType = FieldControlType.CHECKBOX, icon = "fact_check")
  @Filterable(operation = FilterOperation.IS_NULL, relation = "dtPrescricao") private Boolean semPrescricao;
  @Schema(description = "Seleciona ciclos que consumiram dias por conversão em dobro.")
  @UISchema(label = "Conversão acima de", helpText = "Use zero para localizar qualquer conversão em dobro.", icon = "more_time")
  @Filterable(operation = FilterOperation.GREATER_THAN, relation = "diasConvertidosDobro") private Integer conversaoAcima;
  @Schema(description = "Seleciona ciclos cuja programação futura possui mais dias que o valor informado.")
  @UISchema(label = "Gozo futuro acima de", helpText = "Use zero para localizar parcelas futuras registradas.", icon = "event")
  @Filterable(operation = FilterOperation.GREATER_THAN, relation = "diasProgramados") private Long programacaoAcima;
  @Schema(description = "Piso de tempo líquido averbado para licença no vínculo inteiro, repetido em seus ciclos.")
  @UISchema(label = "Tempo averbado mínimo", helpText = "Quantidade mínima de dias reconhecidos no vínculo.", icon = "history_edu")
  @Filterable(operation = FilterOperation.GREATER_OR_EQUAL, relation = "totalAverbadoVinculo") private Long tempoAverbadoMinimo;
  @Schema(description = "Teto de dias de gozo futuro programado para o ciclo; informe zero para localizar ciclos sem programação futura.")
  @UISchema(label = "Gozo futuro máximo", helpText = "Informe zero para isolar ciclos sem gozo futuro programado.", icon = "event_busy")
  @Filterable(operation = FilterOperation.LESS_OR_EQUAL, relation = "diasProgramados") private Long programacaoMaxima;
  @Schema(description = "Pesquisa textual pelo nome oficial da unidade de lotação do vínculo monitorado (ex.: UPA, Hospital, Secretaria de Saúde).", example = "Saúde")
  @UISchema(label = "Nome da lotação", helpText = "Digite parte do nome da lotação do vínculo.", controlType = FieldControlType.INPUT, icon = "account_tree")
  @Filterable(operation = FilterOperation.LIKE, relation = "vinculo.lotacao.nome") private String setorLotacaoNome;
  @Schema(description = "Filtra ciclos monitorados pelo tipo de unidade organizacional de lotação do vínculo (ex.: SECRETARIA, DIRETORIA, COORDENADORIA, NUCLEO, UNIDADE_SAUDE, ESCOLA).")
  @UISchema(label = "Tipos de unidade de lotação", helpText = "Selecione os tipos de unidade organizacional.", type = FieldDataType.ARRAY, controlType = FieldControlType.MULTI_SELECT, icon = "category", options = "[{\"value\":\"SECRETARIA\",\"label\":\"Secretaria\"},{\"value\":\"DIRETORIA\",\"label\":\"Diretoria\"},{\"value\":\"COORDENADORIA\",\"label\":\"Coordenadoria\"},{\"value\":\"NUCLEO\",\"label\":\"Núcleo\"},{\"value\":\"UNIDADE_SAUDE\",\"label\":\"Unidade de Saúde\"},{\"value\":\"ESCOLA\",\"label\":\"Unidade Escolar\"}]")
  @Filterable(operation = FilterOperation.IN, relation = "vinculo.lotacao.tipoSetor") private List<String> tipoSetorLotacao;
  @Schema(description = "Filtra ciclos monitorados pela categoria funcional do vínculo (ex.: ADMINISTRATIVO, MAGISTERIO, SAUDE, GUARDA_MUNICIPAL).")
  @UISchema(label = "Categorias funcionais", helpText = "Selecione uma ou mais categorias funcionais.", type = FieldDataType.ARRAY, controlType = FieldControlType.MULTI_SELECT, icon = "badge", options = "[{\"value\":\"ADMINISTRATIVO\",\"label\":\"Administrativo\"},{\"value\":\"MAGISTERIO\",\"label\":\"Magistério\"},{\"value\":\"SAUDE\",\"label\":\"Saúde\"},{\"value\":\"GUARDA_MUNICIPAL\",\"label\":\"Guarda Municipal\"}]")
  @Filterable(operation = FilterOperation.IN, relation = "vinculo.categoria") private List<String> categoriaVinculo;
  @CustomFields(resourceKey = "administracao-pessoal.periodos-aquisitivos-licenca-especial")
  @Schema(description = "Predicados sobre atributos institucionais dos ciclos projetados no radar.")
  @UISchema(label = "Campos customizados", helpText = "Pesquise atributos do ciclo de origem.", icon = "extension")
  private Map<String, Object> customFields = new LinkedHashMap<>();
}
