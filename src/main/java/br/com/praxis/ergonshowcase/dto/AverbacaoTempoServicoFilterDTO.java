package br.com.praxis.ergonshowcase.dto;
import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.*;
import lombok.*;
import org.praxisplatform.uischema.*;
import org.praxisplatform.uischema.customfields.CustomFields;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.filter.annotation.Filterable;
import org.praxisplatform.uischema.filter.annotation.Filterable.FilterOperation;
@Getter @Setter
@Schema(description = "Pesquisa de averbações por titular, certidão, finalidade, natureza de tempo e descontos parciais.")
public class AverbacaoTempoServicoFilterDTO extends TempoLicencaFilterDTO {
  @Schema(description = "Pesquisa averbações por descrição da certidão, titular, matrícula ou observações, combinadas por OU.")
  @UISchema(label = "Busca ampla", helpText = "Pesquise a certidão ou seu titular.", controlType = FieldControlType.INPUT, icon = "search")
  @Filterable(operation = FilterOperation.LIKE, relations = {"descricao", "pessoa.nome", "vinculo.matric", "vinculo.lotacao.nome", "observacoes"}) private String searchTerm;
  @Schema(description = "Órgão do vínculo ao qual pertence o tempo reconhecido, sempre combinado com o escopo do serviço.")
  @UISchema(label = "Órgão", helpText = "Selecione o órgão do assentamento.", type = FieldDataType.NUMBER, controlType = FieldControlType.ENTITY_LOOKUP, endpoint = br.com.praxis.ergonshowcase.constants.ApiPaths.ORGAO_OPTIONS, valueField = "id", displayField = "label", icon = "apartment")
  @Filterable(operation = FilterOperation.EQUAL) private Integer empCodigo;
  @Schema(description = "Número funcional da pessoa titular da certidão de tempo anterior.")
  @UISchema(label = "Servidor", helpText = "Selecione o titular da averbação.", type = FieldDataType.NUMBER, controlType = FieldControlType.ENTITY_LOOKUP, endpoint = br.com.praxis.ergonshowcase.constants.ApiPaths.PESSOA_OPTIONS, valueField = "id", displayField = "label", icon = "person")
  @Filterable(operation = FilterOperation.EQUAL) private Long numfunc;
  @Schema(description = "Sequencial do vínculo que recebe o tempo certificado; combine com o titular.")
  @UISchema(label = "Sequencial do vínculo", helpText = "Informe o vínculo dentro do cadastro do titular.", icon = "badge")
  @Filterable(operation = FilterOperation.EQUAL) private Integer numvinc;
  @Schema(description = "Localiza séries de certidões cujo texto de identificação começa pelo prefixo informado.")
  @UISchema(label = "Descrição começa com", helpText = "Digite o início da identificação da certidão.", icon = "text_fields")
  @Filterable(operation = FilterOperation.STARTS_WITH, relation = "descricao") private String descricaoPrefixo;
  @Schema(description = "Estado operacional exato do reconhecimento de tempo, incluindo homologação e desaverbação.", allowableValues = {"EM_ANALISE", "HOMOLOGADA", "PARCIALMENTE_DESAVERBADA", "DESAVERBADA_TOTAL", "CANCELADO"})
  @UISchema(label = "Situação cadastral", helpText = "Selecione a situação do assentamento.", controlType = FieldControlType.SELECT, icon = "flag")
  @Filterable(operation = FilterOperation.EQUAL) private String situacao;
  @Schema(description = "Inclui as averbações em qualquer uma das situações informadas.", allowableValues = {"EM_ANALISE", "HOMOLOGADA", "PARCIALMENTE_DESAVERBADA", "DESAVERBADA_TOTAL", "CANCELADO"})
  @UISchema(label = "Situações incluídas", helpText = "Escolha as situações que entram na pesquisa.", icon = "filter_list")
  @Filterable(operation = FilterOperation.IN, relation = "situacao") private List<String> situacoes;
  @Schema(description = "Exclui averbações nas situações informadas sem alterar o escopo de órgão.", allowableValues = {"EM_ANALISE", "HOMOLOGADA", "PARCIALMENTE_DESAVERBADA", "DESAVERBADA_TOTAL", "CANCELADO"})
  @UISchema(label = "Situações excluídas", helpText = "Escolha as situações que não devem aparecer.", icon = "filter_alt_off")
  @Filterable(operation = FilterOperation.NOT_IN, relation = "situacao") private List<String> situacoesExcluidas;
  @Schema(description = "Janela inclusiva das datas de início do serviço certificado.")
  @UISchema(label = "Início do serviço entre", helpText = "Informe os dois limites da janela de início.", type = FieldDataType.DATE, controlType = FieldControlType.DATE_RANGE, icon = "date_range")
  @Filterable(operation = FilterOperation.BETWEEN, relation = "dtini") private List<LocalDate> inicioEntre;
  @Schema(description = "Data mínima inclusiva de início do serviço certificado.")
  @UISchema(label = "Serviço iniciado desde", helpText = "Informe a data mínima de início.", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, icon = "event")
  @Filterable(operation = FilterOperation.GREATER_OR_EQUAL, relation = "dtini") private LocalDate inicioMinimo;
  @Schema(description = "Data máxima inclusiva de término do serviço certificado.")
  @UISchema(label = "Serviço encerrado até", helpText = "Informe a data máxima de término.", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, icon = "event_available")
  @Filterable(operation = FilterOperation.LESS_OR_EQUAL, relation = "dtfim") private LocalDate terminoMaximo;
  @Schema(description = "Natureza pública ou RGPS do tempo certificado.", allowableValues = {"PUBLICO", "RGPS"})
  @UISchema(label = "Natureza do tempo", helpText = "Selecione a origem do tempo reconhecido.", controlType = FieldControlType.SELECT, icon = "category")
  @Filterable(operation = FilterOperation.EQUAL) private String tipoTempo;
  @Schema(description = "Direito destinatário do tempo reconhecido.", allowableValues = {"APOSENTADORIA", "LICENCA_ESPECIAL", "TEMPO_SERVICO"})
  @UISchema(label = "Finalidade", helpText = "Selecione o direito que recebe a contagem.", controlType = FieldControlType.SELECT, icon = "rule")
  @Filterable(operation = FilterOperation.EQUAL) private String finalidade;
  @Schema(description = "Seleciona averbações com desconto parcial superior a zero dias.")
  @UISchema(label = "Com desaverbação parcial", helpText = "Informe zero para localizar qualquer desconto de dias.", icon = "remove_circle")
  @Filterable(operation = FilterOperation.GREATER_THAN, relation = "diasDesaverbados") private Integer diasDesaverbadosAcima;
  @Schema(description = "Localiza certidões que registram sobreposição de tempo.")
  @UISchema(label = "Com tempo sobreposto", helpText = "Ative para inspecionar coincidência de períodos.", controlType = FieldControlType.CHECKBOX, icon = "layers")
  @Filterable(operation = FilterOperation.IS_TRUE, relation = "sobrepoe") private Boolean comSobreposicao;
  @Schema(description = "Localiza registros ainda sem instituição de origem preenchida.")
  @UISchema(label = "Sem instituição", helpText = "Inspecione certidões com origem ainda não identificada.", controlType = FieldControlType.CHECKBOX, icon = "fact_check")
  @Filterable(operation = FilterOperation.IS_NULL, relation = "instituicao") private Boolean semInstituicao;
  @Schema(description = "Seleciona a data exata de efeito na contagem funcional.")
  @UISchema(label = "Efeito na data", helpText = "Data específica de início dos efeitos.", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, icon = "event")
  @Filterable(operation = FilterOperation.ON_DATE, relation = "dataAContar") private LocalDate efeitoEm;
  @CustomFields(resourceKey = "administracao-pessoal.averbacoes-tempo-servico")
  @Schema(description = "Predicados sobre extensões institucionais de averbações, segundo o catálogo do órgão.")
  @UISchema(label = "Campos customizados", helpText = "Pesquise atributos definidos pelo órgão.", icon = "extension")
  private Map<String, Object> customFields = new LinkedHashMap<>();
  @Schema(description = "Identidade composta pessoa:número do vínculo; mantém a relação pai-filho completa nas superfícies 360°.")
  @UISchema(label = "Vínculo funcional", type = FieldDataType.TEXT, controlType = FieldControlType.ENTITY_LOOKUP, endpoint = ApiPaths.VINCULO_OPTIONS, valueField = "id", displayField = "label", icon = "badge", tableHidden = true)
  @Filterable(operation = FilterOperation.EQUAL, relation = "vinculo.id") private br.com.praxis.ergonshowcase.domain.VinculoId vinculoId;
  @Schema(description = "Trecho do nome da unidade organizacional de lotação do vínculo titular da averbação (ex.: Saúde, Educação, Fazenda).")
  @UISchema(label = "Nome da unidade", helpText = "Pesquise pelo nome da secretaria ou setor de lotação.", controlType = FieldControlType.INPUT, icon = "account_tree")
  @Filterable(operation = FilterOperation.LIKE, relation = "vinculo.lotacao.nome") private String setorLotacaoNome;
  @Schema(description = "Classificação estrutural da unidade de lotação do vínculo titular (ex.: SECRETARIA, DIRETORIA, HOSPITAL, UNIDADE_ENSINO).")
  @UISchema(label = "Tipo de unidade", helpText = "Filtra pela classificação da unidade de lotação.", controlType = FieldControlType.INPUT, icon = "domain")
  @Filterable(operation = FilterOperation.EQUAL, relation = "vinculo.lotacao.tipoSetor") private String tipoSetorLotacao;
  @Schema(description = "Categoria funcional do vínculo titular da averbação (ex.: SAUDE, MAGISTERIO, GERAL, GUARDA_MUNICIPAL).")
  @UISchema(label = "Categoria funcional", helpText = "Filtra pela carreira ou categoria do vínculo.", controlType = FieldControlType.INPUT, icon = "category")
  @Filterable(operation = FilterOperation.EQUAL, relation = "vinculo.categoria") private String categoriaVinculo;
}
