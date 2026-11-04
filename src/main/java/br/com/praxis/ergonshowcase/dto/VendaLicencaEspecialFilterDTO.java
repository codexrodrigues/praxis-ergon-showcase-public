package br.com.praxis.ergonshowcase.dto;
import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.*;
import lombok.*;
import org.praxisplatform.uischema.*;
import org.praxisplatform.uischema.customfields.CustomFields;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.extension.annotation.UISchemaPreset;
import org.praxisplatform.uischema.filter.annotation.Filterable;
import org.praxisplatform.uischema.filter.annotation.Filterable.FilterOperation;
@Getter @Setter
@Schema(description = "Pesquisa de conversões pecuniárias por titular, período, modalidade, dias vendidos e pagamento.")
public class VendaLicencaEspecialFilterDTO extends TempoLicencaFilterDTO {
  @Schema(description = "Pesquisa eventos de venda por descrição da conversão, nome do titular, matrícula ou observações.")
  @UISchema(label = "Busca ampla", helpText = "Pesquise a venda ou seu titular.", controlType = FieldControlType.INPUT, icon = "search")
  @Filterable(operation = FilterOperation.LIKE, relations = {"descricao", "pessoa.nome", "vinculo.matric", "vinculo.lotacao.nome", "observacoes"}) private String searchTerm;
  @Schema(description = "Órgão responsável pelo vínculo da venda, combinado com o escopo do serviço.")
  @UISchema(label = "Órgão", helpText = "Selecione o órgão da conversão.", type = FieldDataType.NUMBER, controlType = FieldControlType.ENTITY_LOOKUP, endpoint = br.com.praxis.ergonshowcase.constants.ApiPaths.ORGAO_OPTIONS, valueField = "id", displayField = "label", icon = "apartment")
  @Filterable(operation = FilterOperation.EQUAL) private Integer empCodigo;
  @Schema(description = "Número funcional da pessoa titular dos dias vendidos.")
  @UISchema(label = "Servidor", helpText = "Selecione o titular da venda.", type = FieldDataType.NUMBER, controlType = FieldControlType.ENTITY_LOOKUP, endpoint = br.com.praxis.ergonshowcase.constants.ApiPaths.PESSOA_OPTIONS, valueField = "id", displayField = "label", icon = "person")
  @Filterable(operation = FilterOperation.EQUAL) private Long numfunc;
  @Schema(description = "Sequencial do vínculo da conversão; combine com o número funcional.")
  @UISchema(label = "Sequencial do vínculo", helpText = "Informe o vínculo da venda.", icon = "badge")
  @Filterable(operation = FilterOperation.EQUAL) private Integer numvinc;
  @Schema(description = "Prefixo da identificação do evento de conversão pecuniária.")
  @UISchema(label = "Descrição começa com", helpText = "Digite o início da identificação da venda.", icon = "text_fields")
  @Filterable(operation = FilterOperation.STARTS_WITH, relation = "descricao") private String descricaoPrefixo;
  @Schema(description = "Estado operacional exato da venda ou do abono, incluindo o histórico compensado.", allowableValues = {"SOLICITADA", "AUTORIZADA", "PAGA", "ESTORNADA"})
  @UISchema(label = "Situação cadastral", helpText = "Selecione a situação da venda.", controlType = FieldControlType.SELECT, icon = "flag")
  @Filterable(operation = FilterOperation.EQUAL) private String situacao;
  @Schema(description = "Inclui vendas e abonos em qualquer um dos estados operacionais informados.", allowableValues = {"SOLICITADA", "AUTORIZADA", "PAGA", "ESTORNADA"})
  @UISchema(label = "Situações incluídas", helpText = "Escolha as situações pesquisadas.", icon = "filter_list")
  @Filterable(operation = FilterOperation.IN, relation = "situacao") private List<String> situacoes;
  @Schema(description = "Exclui vendas e abonos nos estados operacionais informados.", allowableValues = {"SOLICITADA", "AUTORIZADA", "PAGA", "ESTORNADA"})
  @UISchema(label = "Situações excluídas", helpText = "Escolha as situações que não devem aparecer.", icon = "filter_alt_off")
  @Filterable(operation = FilterOperation.NOT_IN, relation = "situacao") private List<String> situacoesExcluidas;
  @Schema(description = "Janela inclusiva das datas de registro dos eventos de venda.")
  @UISchema(label = "Venda registrada entre", helpText = "Informe os limites da janela de registro.", type = FieldDataType.DATE, controlType = FieldControlType.DATE_RANGE, icon = "date_range")
  @Filterable(operation = FilterOperation.BETWEEN, relation = "dtini") private List<LocalDate> inicioEntre;
  @Schema(description = "Data mínima inclusiva de registro da conversão pecuniária.")
  @UISchema(label = "Venda registrada desde", helpText = "Informe a primeira data de registro desejada.", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, icon = "event")
  @Filterable(operation = FilterOperation.GREATER_OR_EQUAL, relation = "dtini") private LocalDate inicioMinimo;
  @Schema(description = "Limite inclusivo de término cadastrado do evento, quando o assentamento possui intervalo.")
  @UISchema(label = "Término cadastrado até", helpText = "Use apenas para vendas com término cadastrado.", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, icon = "event_available")
  @Filterable(operation = FilterOperation.LESS_OR_EQUAL, relation = "dtfim") private LocalDate terminoMaximo;
  @Schema(description = "Seleciona vendas pertencentes ao ciclo aquisitivo informado.")
  @UISchema(label = "Período aquisitivo", helpText = "Identidade do ciclo de origem dos dias vendidos.", type = FieldDataType.NUMBER, controlType = FieldControlType.ENTITY_LOOKUP, endpoint = br.com.praxis.ergonshowcase.constants.ApiPaths.PERIODO_AQUISITIVO_LICENCA_ESPECIAL_OPTIONS, valueField = "id", displayField = "label", icon = "date_range")
  @Filterable(operation = FilterOperation.EQUAL) private Long periodoAquisitivoLicencaId;
  @Schema(description = "Seleciona vendas pelo espelho da chave de negócio do período.")
  @UISchema(label = "Chave do período", helpText = "Combine com pessoa e vínculo para localizar o ciclo.", icon = "tag")
  @Filterable(operation = FilterOperation.EQUAL) private Integer chavePeraqlicesp;
  @Schema(description = "Seleciona venda em pecúnia ou registro financeiro vinculado ao abono do gozo, inclusive reemissões e estornos.", allowableValues = {"PECUNIA", "ABONO_JUNTO_GOZO"})
  @UISchema(label = "Modalidade", helpText = "Escolha pecúnia ou abono.", controlType = FieldControlType.SELECT, icon = "category")
  @Filterable(operation = FilterOperation.EQUAL) private String modalidade;
  @Schema(description = "Seleciona conversões ainda sem data de pagamento registrada.")
  @UISchema(label = "Sem pagamento registrado", helpText = "Inspecione eventos sem data de liquidação.", controlType = FieldControlType.CHECKBOX, icon = "pending")
  @Filterable(operation = FilterOperation.IS_NULL, relation = "dtPagamento") private Boolean semPagamento;
  @Schema(description = "Seleciona eventos pagos na data civil informada.")
  @UISchema(label = "Pagamento na data", helpText = "Data exata da liquidação da indenização.", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, icon = "event")
  @Filterable(operation = FilterOperation.ON_DATE, relation = "dtPagamento") private LocalDate pagamentoEm;
  @Schema(description = "Quantidade mínima de dias vendidos no evento, inclusive.")
  @UISchema(label = "Mínimo de dias vendidos", helpText = "Piso de dias para selecionar as conversões.", icon = "sell")
  @Filterable(operation = FilterOperation.GREATER_OR_EQUAL, relation = "diasVendidos") private Integer diasVendidosMinimo;
  @Schema(description = "Janela inclusiva da data de efeito do estorno; não altera a pesquisa pela data do pagamento original.")
  @UISchema(label = "Estorno entre", helpText = "Informe o intervalo dos atos compensatórios.", type = FieldDataType.DATE, controlType = FieldControlType.DATE_RANGE, icon = "history", preset = UISchemaPreset.DATE_RANGE)
  @Filterable(operation = FilterOperation.BETWEEN, relation = "dtEstorno") private List<LocalDate> estornoEntre;
  @Schema(description = "Seleciona registros com data de estorno conhecida; estornos legados sem data não são incluídos neste recorte.")
  @UISchema(label = "Com estorno datado", helpText = "Exiba compensações com data de efeito registrada.", type = FieldDataType.BOOLEAN, controlType = FieldControlType.CHECKBOX, icon = "undo", preset = UISchemaPreset.BOOLEAN_FLAG)
  @Filterable(operation = FilterOperation.IS_NOT_NULL, relation = "dtEstorno") private Boolean comEstorno;
  @CustomFields(resourceKey = "administracao-pessoal.vendas-licenca-especial")
  @Schema(description = "Predicados sobre extensões institucionais dos eventos de venda de licença especial.")
  @UISchema(label = "Campos customizados", helpText = "Pesquise atributos adicionais da conversão.", icon = "extension")
  private Map<String, Object> customFields = new LinkedHashMap<>();
  @Schema(description = "Parcela de gozo que originou o abono vinculado; restringe a pecúnia ao evento de fruição selecionado.")
  @UISchema(label = "Gozo de origem", type = FieldDataType.NUMBER, controlType = FieldControlType.ENTITY_LOOKUP, endpoint = ApiPaths.LICENCA_ESPECIAL_GOZO_OPTIONS, valueField = "id", displayField = "label", icon = "beach_access", tableHidden = true)
  @Filterable(operation = FilterOperation.EQUAL) private Long licencaEspecialGozoId;
  @Schema(description = "Identidade composta pessoa:número do vínculo; mantém a relação pai-filho completa nas superfícies 360°.")
  @UISchema(label = "Vínculo funcional", type = FieldDataType.TEXT, controlType = FieldControlType.ENTITY_LOOKUP, endpoint = ApiPaths.VINCULO_OPTIONS, valueField = "id", displayField = "label", icon = "badge", tableHidden = true)
  @Filterable(operation = FilterOperation.EQUAL, relation = "vinculo.id") private br.com.praxis.ergonshowcase.domain.VinculoId vinculoId;
  @Schema(description = "Trecho do nome da unidade organizacional de lotação do vínculo titular da venda (ex.: Saúde, Educação, Fazenda).")
  @UISchema(label = "Nome da unidade", helpText = "Pesquise pelo nome da secretaria ou setor de lotação.", controlType = FieldControlType.INPUT, icon = "account_tree")
  @Filterable(operation = FilterOperation.LIKE, relation = "vinculo.lotacao.nome") private String setorLotacaoNome;
  @Schema(description = "Classificação estrutural da unidade de lotação do vínculo titular (ex.: SECRETARIA, DIRETORIA, HOSPITAL, UNIDADE_ENSINO).")
  @UISchema(label = "Tipo de unidade", helpText = "Filtra pela classificação da unidade de lotação.", controlType = FieldControlType.INPUT, icon = "domain")
  @Filterable(operation = FilterOperation.EQUAL, relation = "vinculo.lotacao.tipoSetor") private String tipoSetorLotacao;
  @Schema(description = "Categoria funcional do vínculo titular da venda (ex.: SAUDE, MAGISTERIO, GERAL, GUARDA_MUNICIPAL).")
  @UISchema(label = "Categoria funcional", helpText = "Filtra pela carreira ou categoria do vínculo.", controlType = FieldControlType.INPUT, icon = "category")
  @Filterable(operation = FilterOperation.EQUAL, relation = "vinculo.categoria") private String categoriaVinculo;
}
