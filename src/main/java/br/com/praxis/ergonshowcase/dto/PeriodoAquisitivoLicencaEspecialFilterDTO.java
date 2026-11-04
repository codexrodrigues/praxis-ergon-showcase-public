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
@Schema(description = "Pesquisa de ciclos de licença especial por saldo, prescrição, origem e parâmetros individuais.")
public class PeriodoAquisitivoLicencaEspecialFilterDTO extends TempoLicencaFilterDTO {
  @Schema(description = "Pesquisa ciclos aquisitivos pela identificação do período, nome do titular, matrícula ou observações.")
  @UISchema(label = "Busca ampla", helpText = "Pesquise o ciclo ou seu titular.", controlType = FieldControlType.INPUT, icon = "search")
  @Filterable(operation = FilterOperation.LIKE, relations = {"descricao", "pessoa.nome", "vinculo.matric", "vinculo.lotacao.nome", "observacoes"}) private String searchTerm;
  @Schema(description = "Órgão responsável pelo vínculo do ciclo de aquisição, combinado com o escopo do serviço.")
  @UISchema(label = "Órgão", helpText = "Selecione o órgão do ciclo.", type = FieldDataType.NUMBER, controlType = FieldControlType.ENTITY_LOOKUP, endpoint = br.com.praxis.ergonshowcase.constants.ApiPaths.ORGAO_OPTIONS, valueField = "id", displayField = "label", icon = "apartment")
  @Filterable(operation = FilterOperation.EQUAL) private Integer empCodigo;
  @Schema(description = "Número funcional da pessoa que adquire o direito à licença.")
  @UISchema(label = "Servidor", helpText = "Selecione o titular do ciclo aquisitivo.", type = FieldDataType.NUMBER, controlType = FieldControlType.ENTITY_LOOKUP, endpoint = br.com.praxis.ergonshowcase.constants.ApiPaths.PESSOA_OPTIONS, valueField = "id", displayField = "label", icon = "person")
  @Filterable(operation = FilterOperation.EQUAL) private Long numfunc;
  @Schema(description = "Sequencial do vínculo no qual o direito é adquirido; combine com o número funcional.")
  @UISchema(label = "Sequencial do vínculo", helpText = "Informe o vínculo do ciclo.", icon = "badge")
  @Filterable(operation = FilterOperation.EQUAL) private Integer numvinc;
  @Schema(description = "Prefixo da identificação do ciclo, útil para pesquisar séries de períodos aquisitivos.")
  @UISchema(label = "Descrição começa com", helpText = "Digite o início da identificação do ciclo.", icon = "text_fields")
  @Filterable(operation = FilterOperation.STARTS_WITH, relation = "descricao") private String descricaoPrefixo;
  @Schema(description = "Estado operacional exato do ciclo de aquisição e destinação do direito de licença.", allowableValues = {"EM_AQUISICAO", "DISPONIVEL", "PARCIALMENTE_GOZADO", "CONVERTIDO_EM_DOBRO", "QUITADO", "SUSPENSO", "PRESCRITO", "CANCELADO"})
  @UISchema(label = "Situação cadastral", helpText = "Selecione a situação do ciclo.", controlType = FieldControlType.SELECT, icon = "flag")
  @Filterable(operation = FilterOperation.EQUAL) private String situacao;
  @Schema(description = "Inclui os ciclos em qualquer uma das situações informadas.", allowableValues = {"EM_AQUISICAO", "DISPONIVEL", "PARCIALMENTE_GOZADO", "CONVERTIDO_EM_DOBRO", "QUITADO", "SUSPENSO", "PRESCRITO", "CANCELADO"})
  @UISchema(label = "Situações incluídas", helpText = "Escolha as situações pesquisadas.", icon = "filter_list")
  @Filterable(operation = FilterOperation.IN, relation = "situacao") private List<String> situacoes;
  @Schema(description = "Retira da pesquisa os ciclos nas situações informadas.", allowableValues = {"EM_AQUISICAO", "DISPONIVEL", "PARCIALMENTE_GOZADO", "CONVERTIDO_EM_DOBRO", "QUITADO", "SUSPENSO", "PRESCRITO", "CANCELADO"})
  @UISchema(label = "Situações excluídas", helpText = "Escolha as situações que não devem aparecer.", icon = "filter_alt_off")
  @Filterable(operation = FilterOperation.NOT_IN, relation = "situacao") private List<String> situacoesExcluidas;
  @Schema(description = "Janela inclusiva das datas de início dos ciclos de aquisição.")
  @UISchema(label = "Aquisição iniciada entre", helpText = "Informe os limites da janela de início.", type = FieldDataType.DATE, controlType = FieldControlType.DATE_RANGE, icon = "date_range")
  @Filterable(operation = FilterOperation.BETWEEN, relation = "dtini") private List<java.time.LocalDate> inicioEntre;
  @Schema(description = "Data mínima inclusiva de início da aquisição do direito.")
  @UISchema(label = "Aquisição iniciada desde", helpText = "Informe a data mínima de início do ciclo.", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, icon = "event")
  @Filterable(operation = FilterOperation.GREATER_OR_EQUAL, relation = "dtini") private java.time.LocalDate inicioMinimo;
  @Schema(description = "Data máxima inclusiva de encerramento da aquisição do direito.")
  @UISchema(label = "Aquisição encerrada até", helpText = "Informe o último término de ciclo desejado.", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, icon = "event_available")
  @Filterable(operation = FilterOperation.LESS_OR_EQUAL, relation = "dtfim") private java.time.LocalDate terminoMaximo;
  @Schema(description = "Seleciona o sequencial de aquisição dentro do vínculo.")
  @UISchema(label = "Chave do período", helpText = "Sequencial do ciclo no vínculo.", icon = "tag")
  @Filterable(operation = FilterOperation.EQUAL) private Integer chave;
  @Schema(description = "Seleciona ciclos com saldo estritamente superior à quantidade informada.")
  @UISchema(label = "Saldo acima de", helpText = "Use zero para localizar direitos ainda disponíveis.", icon = "donut_large")
  @Filterable(operation = FilterOperation.GREATER_THAN, relation = "saldoDias") private Integer saldoAcima;
  @Schema(description = "Localiza ciclos com prescrição nos próximos dias, conforme a data corrente de avaliação do filtro.")
  @UISchema(label = "Prescrição próxima", helpText = "Quantidade de dias da janela futura.", icon = "warning")
  @Filterable(operation = FilterOperation.IN_NEXT_DAYS, relation = "dtPrescricao") private Integer prescricaoProximosDias;
  @Schema(description = "Seleciona períodos cuja conversão em dobro tem data registrada.")
  @UISchema(label = "Com conversão datada", helpText = "Inspecione direitos com data de conversão preenchida.", controlType = FieldControlType.CHECKBOX, icon = "more_time")
  @Filterable(operation = FilterOperation.IS_NOT_NULL, relation = "dtCntDobro") private Boolean comContagemDobro;
  @Schema(description = "Seleciona períodos com vedação individual explícita de antecipação do gozo, distinta da herança nula.")
  @UISchema(label = "Antecipação vedada", helpText = "Inspecione overrides que vedam gozo antecipado.", controlType = FieldControlType.CHECKBOX, icon = "block")
  @Filterable(operation = FilterOperation.IS_FALSE, relation = "permGozoLicEspAntFimPe") private Boolean semGozoAntecipado;
  @Schema(description = "Seleciona períodos vinculados a uma averbação que originou o direito.")
  @UISchema(label = "Averbação de origem", helpText = "Identidade da averbação formadora do ciclo.", type = FieldDataType.NUMBER, controlType = FieldControlType.ENTITY_LOOKUP, endpoint = br.com.praxis.ergonshowcase.constants.ApiPaths.AVERBACAO_TEMPO_SERVICO_OPTIONS, valueField = "id", displayField = "label", icon = "history_edu")
  @Filterable(operation = FilterOperation.EQUAL) private Long averbacaoOrigemId;
  @CustomFields(resourceKey = "administracao-pessoal.periodos-aquisitivos-licenca-especial")
  @Schema(description = "Predicados sobre extensões institucionais dos ciclos aquisitivos, segundo o catálogo do órgão.")
  @UISchema(label = "Campos customizados", helpText = "Pesquise atributos institucionais do ciclo.", icon = "extension")
  private Map<String, Object> customFields = new LinkedHashMap<>();
  @Schema(description = "Identidade composta pessoa:número do vínculo; mantém a relação pai-filho completa nas superfícies 360°.")
  @UISchema(label = "Vínculo funcional", type = FieldDataType.TEXT, controlType = FieldControlType.ENTITY_LOOKUP, endpoint = ApiPaths.VINCULO_OPTIONS, valueField = "id", displayField = "label", icon = "badge", tableHidden = true)
  @Filterable(operation = FilterOperation.EQUAL, relation = "vinculo.id") private br.com.praxis.ergonshowcase.domain.VinculoId vinculoId;
  @Schema(description = "Trecho do nome da unidade organizacional de lotação do vínculo titular do ciclo (ex.: Saúde, Educação, Fazenda).")
  @UISchema(label = "Nome da unidade", helpText = "Pesquise pelo nome da secretaria ou setor de lotação.", controlType = FieldControlType.INPUT, icon = "account_tree")
  @Filterable(operation = FilterOperation.LIKE, relation = "vinculo.lotacao.nome") private String setorLotacaoNome;
  @Schema(description = "Classificação estrutural da unidade de lotação do vínculo titular (ex.: SECRETARIA, DIRETORIA, HOSPITAL, UNIDADE_ENSINO).")
  @UISchema(label = "Tipo de unidade", helpText = "Filtra pela classificação da unidade de lotação.", controlType = FieldControlType.INPUT, icon = "domain")
  @Filterable(operation = FilterOperation.EQUAL, relation = "vinculo.lotacao.tipoSetor") private String tipoSetorLotacao;
  @Schema(description = "Categoria funcional do vínculo titular do ciclo aquisitivo (ex.: SAUDE, MAGISTERIO, GERAL, GUARDA_MUNICIPAL).")
  @UISchema(label = "Categoria funcional", helpText = "Filtra pela carreira ou categoria do vínculo.", controlType = FieldControlType.INPUT, icon = "category")
  @Filterable(operation = FilterOperation.EQUAL, relation = "vinculo.categoria") private String categoriaVinculo;
}
