package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.domain.VinculoId;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.customfields.CustomFields;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.filter.annotation.Filterable;
import org.praxisplatform.uischema.filter.annotation.Filterable.FilterOperation;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;

@Getter
@Setter
@NoArgsConstructor
@Schema(
    description =
        "Critérios ricos de filtragem de parcelas de programação e gozo de férias, abono pecuniário, competência de folha e substituição funcional.")
public class FeriasGozoFilterDTO implements GenericFilterDTO {

  @Schema(
      description =
          "Busca ampla textual por nome do servidor, CPF, matrícula do vínculo, substituto, motivo de interrupção ou observações.")
  @UISchema(
      label = "Busca ampla",
      helpText = "Pesquise por servidor, CPF, matrícula, substituto ou observação.",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "search")
  @Filterable(
      operation = FilterOperation.LIKE,
      relations = {
        "pessoa.nome",
        "pessoa.cpf",
        "vinculo.matric",
        "vinculo.lotacao.nome",
        "pessoaSubstituta.nome",
        "motivoInterrupcao",
        "observacoes"
      })
  private String searchTerm;

  @Schema(description = "Filtra parcelas de férias pelo órgão de lotação do vínculo.")
  @UISchema(
      label = "Órgão",
      helpText = "Órgão público titular do vínculo funcional.",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "apartment",
      endpoint = ApiPaths.ORGAO_OPTIONS,
      valueField = "id",
      displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL, relation = "empCodigo")
  private Integer empCodigo;

  @Schema(description = "Filtra parcelas pelo servidor titular.")
  @UISchema(
      label = "Servidor",
      helpText = "Pessoa servidora titular da parcela de férias.",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "person",
      endpoint = ApiPaths.PESSOA_OPTIONS,
      valueField = "id",
      displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL, relation = "numfunc")
  private Long numfunc;

  @Schema(description = "Filtra pelo sequencial do vínculo funcional.")
  @UISchema(
      label = "Sequencial do vínculo",
      helpText = "Número sequencial do vínculo funcional.",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "work",
      endpoint = ApiPaths.VINCULO_OPTIONS,
      valueField = "id",
      displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL, relation = "numvinc")
  private Integer numvinc;

  @Schema(description = "Filtra pela identidade composta pessoa:número do vínculo funcional.")
  @UISchema(
      label = "Vínculo funcional",
      helpText = "Assentamento funcional titular das férias.",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "badge",
      endpoint = ApiPaths.VINCULO_OPTIONS,
      valueField = "id",
      displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL, relation = "vinculo.id")
  private VinculoId vinculoId;

  @Schema(description = "Filtra pelo identificador do período aquisitivo vinculado.")
  @UISchema(
      label = "Período aquisitivo",
      helpText = "Período aquisitivo debitado pela parcela.",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "date_range",
      endpoint = ApiPaths.PERIODO_AQUISITIVO_FERIAS_OPTIONS,
      valueField = "id",
      displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL, relation = "periodoAquisitivoId")
  private Long periodoAquisitivoId;

  @Schema(description = "Filtra pela chave sequencial do período aquisitivo (CHAVEPERAQFER).")
  @UISchema(
      label = "Chave do PA",
      helpText = "Sequencial de negócio do período aquisitivo.",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "tag")
  @Filterable(operation = FilterOperation.EQUAL, relation = "chavePeraqfer")
  private Integer chavePeraqfer;

  @Schema(description = "Filtra pela ordem da parcela dentro do período aquisitivo.")
  @UISchema(
      label = "Ordem da parcela",
      helpText = "Número da parcela dentro do período aquisitivo.",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "splitscreen")
  @Filterable(operation = FilterOperation.EQUAL, relation = "ordemParcela")
  private Integer ordemParcela;

  @Schema(description = "Filtra por múltiplas situações operacionais da parcela de férias.")
  @UISchema(
      label = "Situações da parcela",
      helpText = "Selecione uma ou mais situações operacionais.",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.MULTI_SELECT,
      icon = "flag")
  @Filterable(operation = FilterOperation.IN, relation = "situacao")
  private List<String> situacoes;

  @Schema(description = "Exclui parcelas nas situações informadas.")
  @UISchema(
      label = "Excluir situações",
      helpText = "Oculta parcelas nas situações selecionadas.",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.MULTI_SELECT,
      icon = "block")
  @Filterable(operation = FilterOperation.NOT_IN, relation = "situacao")
  private List<String> situacoesExcluidas;

  @Schema(description = "Filtra pelo intervalo da data de início do gozo.")
  @UISchema(
      label = "Intervalo de início",
      helpText = "Período da data de início do gozo.",
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_RANGE,
      icon = "date_range")
  @Filterable(operation = FilterOperation.BETWEEN, relation = "dtini")
  private List<LocalDate> dtiniBetween;

  @Schema(description = "Filtra pelo intervalo da data de término previsto.")
  @UISchema(
      label = "Intervalo de término previsto",
      helpText = "Período da data de término previsto.",
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_RANGE,
      icon = "event_available")
  @Filterable(operation = FilterOperation.BETWEEN, relation = "dtprevfim")
  private List<LocalDate> dtprevfimBetween;

  @Schema(description = "Filtra pelo intervalo da competência da folha de pagamento.")
  @UISchema(
      label = "Intervalo de competência",
      helpText = "Período da competência mensal da folha.",
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_RANGE,
      icon = "receipt_long")
  @Filterable(operation = FilterOperation.BETWEEN, relation = "mesAnoFolha")
  private List<LocalDate> mesAnoFolhaBetween;

  @Schema(description = "Filtra pela faixa de dias de gozo da parcela.")
  @UISchema(
      label = "Faixa de dias de gozo",
      helpText = "Intervalo de dias corridos de gozo.",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.RANGE_SLIDER,
      icon = "beach_access")
  @Filterable(operation = FilterOperation.BETWEEN, relation = "diasGozo")
  private List<Integer> diasGozoBetween;

  @Schema(description = "Filtra parcelas com opção de abono pecuniário.")
  @UISchema(
      label = "Com abono pecuniário",
      helpText = "Filtra parcelas com venda de abono pecuniário.",
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "sell")
  @Filterable(operation = FilterOperation.EQUAL, relation = "abonoPecuniario")
  private Boolean abonoPecuniario;

  @Schema(description = "Filtra parcelas com adiantamento de 13º salário.")
  @UISchema(
      label = "Com adiantamento de 13º",
      helpText = "Filtra parcelas com antecipação de 13º salário.",
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "payments")
  @Filterable(operation = FilterOperation.EQUAL, relation = "adiantamento13")
  private Boolean adiantamento13;

  @Schema(description = "Filtra parcelas com início nos próximos N dias.")
  @UISchema(
      label = "Iniciando nos próximos dias",
      helpText = "Radar de férias programadas para iniciar nos próximos dias.",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "upcoming")
  @Filterable(operation = FilterOperation.IN_NEXT_DAYS, relation = "dtini")
  private Integer iniciandoProximosDias;

  @Schema(description = "Filtra parcelas iniciadas nos últimos N dias.")
  @UISchema(
      label = "Iniciadas nos últimos dias",
      helpText = "Parcelas de férias iniciadas recentemente.",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "history")
  @Filterable(operation = FilterOperation.IN_LAST_DAYS, relation = "dtini")
  private Integer iniciadasUltimosDias;

  @Schema(description = "Filtra pelo servidor substituto designado.")
  @UISchema(
      label = "Servidor substituto",
      helpText = "Servidor designado como substituto durante as férias.",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "supervisor_account",
      endpoint = ApiPaths.PESSOA_OPTIONS,
      valueField = "id",
      displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL, relation = "numfuncSubstituto")
  private Long numfuncSubstituto;

  @Schema(description = "Filtra parcelas que possuem substituto designado.")
  @UISchema(
      label = "Com substituto designado",
      helpText = "Exibe apenas parcelas com substituto formalizado.",
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "supervisor_account")
  @Filterable(operation = FilterOperation.IS_NOT_NULL, relation = "numfuncSubstituto")
  private Boolean comSubstituto;

  @Schema(description = "Filtra parcelas sem portaria de concessão associada.")
  @UISchema(
      label = "Sem documento legal",
      helpText = "Exibe parcelas sem portaria de concessão.",
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "find_in_page")
  @Filterable(operation = FilterOperation.IS_NULL, relation = "documentoLegalId")
  private Boolean semDocumentoLegal;

  @Schema(
      description =
          "Filtra parcelas de férias sem substituto designado para auditoria de cobertura operacional.")
  @UISchema(
      label = "Sem substituto",
      helpText = "Exibe parcelas sem substituto formalizado.",
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "person_off")
  @Filterable(operation = FilterOperation.IS_NULL, relation = "numfuncSubstituto")
  private Boolean semSubstituto;

  @Schema(
      description =
          "Trecho do nome da unidade organizacional de lotação do vínculo em gozo de férias (ex.: Saúde, Educação, Fazenda).")
  @UISchema(
      label = "Nome da unidade",
      helpText = "Pesquise férias pelo nome da secretaria ou setor de lotação.",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "account_tree")
  @Filterable(operation = FilterOperation.LIKE, relation = "vinculo.lotacao.nome")
  private String setorLotacaoNome;

  @Schema(
      description =
          "Classificação estrutural da unidade de lotação do vínculo titular das férias (ex.: SECRETARIA, DIRETORIA, HOSPITAL, UNIDADE_ENSINO).")
  @UISchema(
      label = "Tipo de unidade",
      helpText = "Filtra parcelas pela classificação da unidade de lotação.",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "domain")
  @Filterable(operation = FilterOperation.EQUAL, relation = "vinculo.lotacao.tipoSetor")
  private String tipoSetorLotacao;

  @Schema(
      description =
          "Categoria funcional do vínculo titular das férias (ex.: SAUDE, MAGISTERIO, GERAL, GUARDA_MUNICIPAL).")
  @UISchema(
      label = "Categoria funcional",
      helpText = "Filtra parcelas pela carreira ou categoria do vínculo.",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "category")
  @Filterable(operation = FilterOperation.EQUAL, relation = "vinculo.categoria")
  private String categoriaVinculo;

  @Schema(description = "Critérios de filtragem sobre atributos dinâmicos customizados.")
  @CustomFields(resourceKey = "administracao-pessoal.ferias-gozo")
  @UISchema(
      label = "Campos customizados",
      helpText = "Filtros adicionais sobre propriedades dinâmicas da parcela.",
      group = "Campos Customizados",
      order = 900)
  private Map<String, Object> customFields = new LinkedHashMap<>();
}
