package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.*;
import lombok.Getter;
import lombok.Setter;
import org.praxisplatform.uischema.*;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.customfields.CustomFields;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.filter.annotation.Filterable;
import org.praxisplatform.uischema.filter.annotation.Filterable.FilterOperation;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;

@Getter
@Setter
@Schema(
    description =
        "Matriz exaustiva de filtros operacionais, periciais e funcionais de licenças e afastamentos dos servidores públicos.")
public class AfastamentoFilterDTO implements GenericFilterDTO {
  @Schema(
      description =
          "Busca ampla unificada por fundamentação, nome ou CPF do servidor afastado, matrícula do vínculo, denominação ou sigla do código de frequência e número da portaria/publicação.")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Critério de pesquisa pode identificar o servidor ou fundamentação clínica.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @UISchema(label = "Busca ampla", group = "Identificação", order = 1, icon = "search")
  @Filterable(
      operation = FilterOperation.LIKE,
      relations = {
        "motivo",
        "pessoa.nome",
        "pessoa.cpf",
        "vinculo.matric",
        "vinculo.lotacao.nome",
        "codigoFrequenciaEntidade.nome",
        "codigoFrequenciaEntidade.mnemonico",
        "documentoLegal.numeroDocumento",
        "atoPublicacao.numeroDocumento"
      })
  private String buscaAmpla;

  @Schema(description = "Filtra pelo órgão público do vínculo funcional afastado.")
  @UISchema(
      label = "Órgão",
      group = "Identificação",
      order = 2,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "apartment",
      endpoint = ApiPaths.ORGAO_OPTIONS,
      valueField = "id",
      displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL, relation = "empCodigo")
  private Integer empCodigo;

  @Schema(description = "Filtra licenças e afastamentos de um servidor público específico.")
  @UISchema(
      label = "Servidor afastado",
      group = "Identificação",
      order = 3,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "person",
      endpoint = ApiPaths.PESSOA_OPTIONS,
      valueField = "id",
      displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL, relation = "numfunc")
  private Long numfunc;

  @Schema(description = "Filtra pelo número sequencial do vínculo funcional afastado.")
  @UISchema(
      label = "Sequencial do vínculo",
      group = "Identificação",
      order = 4,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "badge",
      endpoint = ApiPaths.VINCULO_OPTIONS,
      valueField = "id",
      displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL, relation = "numvinc")
  private Integer numvinc;

  @Schema(description = "Prefixo da matrícula institucional do vínculo afastado.")
  @UISchema(label = "Matrícula do vínculo", group = "Identificação", order = 5)
  @Filterable(operation = FilterOperation.STARTS_WITH, relation = "vinculo.matric")
  private String vinculoMatricula;

  @Schema(description = "Trecho do nome civil do servidor titular do afastamento.")
  @UISchema(label = "Nome do servidor", group = "Identificação", order = 6)
  @Filterable(operation = FilterOperation.LIKE, relation = "pessoa.nome")
  private String pessoaNome;

  @Schema(description = "Filtra pelo tipo macro de frequência da licença ou afastamento.")
  @UISchema(
      label = "Tipo de frequência",
      group = "Enquadramento",
      order = 10,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "category",
      endpoint = ApiPaths.TIPO_FREQUENCIA_OPTIONS,
      valueField = "id",
      displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL, relation = "tipofreq")
  private String tipofreq;

  @Schema(description = "Filtra pelo código numérico exato da modalidade de licença.")
  @UISchema(
      label = "Código da licença",
      group = "Enquadramento",
      order = 11,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "tag",
      endpoint = ApiPaths.CODIGO_FREQUENCIA_OPTIONS,
      valueField = "id",
      displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL, relation = "codfreq")
  private Integer codfreq;

  @Schema(description = "Situação exata do ciclo de vida da licença (ex.: HOMOLOGADO, PENDENTE_HOMOLOGACAO).")
  @UISchema(label = "Situação", group = "Enquadramento", order = 12)
  @Filterable(operation = FilterOperation.EQUAL, relation = "situacao")
  private String situacao;

  @Schema(description = "Conjunto de situações da licença a incluir na consulta.")
  @UISchema(
      label = "Situações incluídas",
      group = "Enquadramento",
      order = 13,
      controlType = FieldControlType.MULTI_SELECT)
  @Filterable(operation = FilterOperation.IN, relation = "situacao")
  private List<String> situacoes;

  @Schema(description = "Conjunto de situações da licença a excluir da consulta.")
  @UISchema(
      label = "Situações excluídas",
      group = "Enquadramento",
      order = 14,
      controlType = FieldControlType.MULTI_SELECT)
  @Filterable(operation = FilterOperation.NOT_IN, relation = "situacao")
  private List<String> situacoesExcluidas;

  @Schema(description = "Exclui uma situação específica da listagem.")
  @UISchema(label = "Exceto situação", group = "Enquadramento", order = 15)
  @Filterable(operation = FilterOperation.NOT_EQUAL, relation = "situacao")
  private String situacaoDiferenteDe;

  @Schema(description = "Trecho da fundamentação clínica ou administrativa do afastamento.")
  @UISchema(label = "Trecho da fundamentação", group = "Enquadramento", order = 16)
  @Filterable(operation = FilterOperation.LIKE, relation = "motivo")
  private String motivo;

  @Schema(description = "Intervalo de datas de início do afastamento.")
  @UISchema(
      label = "Período de início",
      group = "Datas e vigência",
      order = 20,
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_RANGE,
      icon = "date_range")
  @Filterable(operation = FilterOperation.BETWEEN, relation = "dtini")
  private List<LocalDate> dtiniEntre;

  @Schema(description = "Data mínima de início do afastamento.")
  @UISchema(
      label = "Iniciado a partir de",
      group = "Datas e vigência",
      order = 21,
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "event")
  @Filterable(operation = FilterOperation.GREATER_OR_EQUAL, relation = "dtini")
  private LocalDate dtiniMinima;

  @Schema(description = "Data máxima de início do afastamento.")
  @UISchema(
      label = "Iniciado até",
      group = "Datas e vigência",
      order = 22,
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "event")
  @Filterable(operation = FilterOperation.LESS_OR_EQUAL, relation = "dtini")
  private LocalDate dtiniMaxima;

  @Schema(description = "Data exata de início do afastamento.")
  @UISchema(
      label = "Início na data",
      group = "Datas e vigência",
      order = 23,
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "today")
  @Filterable(operation = FilterOperation.ON_DATE, relation = "dtini")
  private LocalDate dtiniNaData;

  @Schema(description = "Afastamentos iniciados nos últimos N dias.")
  @UISchema(
      label = "Iniciados nos últimos dias",
      group = "Datas e vigência",
      order = 24,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.INPUT,
      icon = "history")
  @Filterable(operation = FilterOperation.IN_LAST_DAYS, relation = "dtini")
  private Integer iniciadosUltimosDias;

  @Schema(description = "Afastamentos com término previsto nos próximos N dias (radar de retorno).")
  @UISchema(
      label = "Previstos para encerrar (dias)",
      group = "Datas e vigência",
      order = 25,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.INPUT,
      icon = "upcoming")
  @Filterable(operation = FilterOperation.IN_NEXT_DAYS, relation = "dtprevfim")
  private Integer retornoPrevistoProximosDias;

  @Schema(description = "Faixa de duração em dias corridos do afastamento.")
  @UISchema(
      label = "Faixa de duração (dias)",
      group = "Datas e vigência",
      order = 26,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.RANGE_SLIDER,
      icon = "timelapse")
  @Filterable(operation = FilterOperation.BETWEEN, relation = "quantidadeDias")
  private List<Integer> quantidadeDiasEntre;

  @Schema(description = "Restringe aos afastamentos sem data de término efetivo registrada (em aberto).")
  @UISchema(
      label = "Somente em aberto",
      group = "Datas e vigência",
      order = 27,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "play_circle")
  @Filterable(operation = FilterOperation.IS_NULL, relation = "dtfim")
  private Boolean somenteEmAberto;

  @Schema(description = "Restringe aos afastamentos já encerrados (com data de término efetivo).")
  @UISchema(
      label = "Somente encerrados",
      group = "Datas e vigência",
      order = 28,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "check_circle")
  @Filterable(operation = FilterOperation.IS_NOT_NULL, relation = "dtfim")
  private Boolean somenteEncerrados;

  @Schema(description = "Restringe aos lançamentos que configuram prorrogação de licença anterior.")
  @UISchema(
      label = "Somente prorrogações",
      group = "Prorrogação e substituição",
      order = 30,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "update")
  @Filterable(operation = FilterOperation.IS_TRUE, relation = "prorrogacao")
  private Boolean somenteProrrogacoes;

  @Schema(description = "Restringe às concessões iniciais (que não são prorrogação).")
  @UISchema(
      label = "Somente concessões iniciais",
      group = "Prorrogação e substituição",
      order = 31,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "fiber_new")
  @Filterable(operation = FilterOperation.IS_FALSE, relation = "prorrogacao")
  private Boolean somenteIniciais;

  @Schema(description = "Restringe aos afastamentos que possuem servidor substituto designado.")
  @UISchema(
      label = "Com substituto designado",
      group = "Prorrogação e substituição",
      order = 32,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "how_to_reg")
  @Filterable(operation = FilterOperation.IS_NOT_NULL, relation = "numfuncSubstituto")
  private Boolean comSubstituto;

  @Schema(description = "Restringe aos afastamentos cujo código de frequência implica ônus financeiro.")
  @UISchema(
      label = "Modalidade com ônus",
      group = "Enquadramento",
      order = 33,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "payments")
  @Filterable(operation = FilterOperation.IS_TRUE, relation = "codigoFrequenciaEntidade.onus")
  private Boolean somenteComOnus;

  @Schema(description = "Filtra pelo servidor designado como substituto.")
  @UISchema(
      label = "Servidor substituto",
      group = "Prorrogação e substituição",
      order = 34,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "person_add",
      endpoint = ApiPaths.PESSOA_OPTIONS,
      valueField = "id",
      displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL, relation = "numfuncSubstituto")
  private Long numfuncSubstituto;

  @Schema(description = "Filtra pela portaria de concessão ou laudo pericial associado.")
  @UISchema(
      label = "Portaria ou laudo legal",
      group = "Documentação",
      order = 40,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "description",
      endpoint = ApiPaths.DOCUMENTO_OPTIONS,
      valueField = "id",
      displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL, relation = "documentoLegalId")
  private Long documentoLegalId;

  @Schema(description = "Filtra pelo ato de publicação oficial em Diário Oficial.")
  @UISchema(
      label = "Ato de publicação oficial",
      group = "Documentação",
      order = 41,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "newspaper",
      endpoint = ApiPaths.DOCUMENTO_OPTIONS,
      valueField = "id",
      displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL, relation = "atoPublicacaoId")
  private Long atoPublicacaoId;

  @Schema(
      description =
          "Filtra licenças e afastamentos pela identidade composta do vínculo funcional (numfunc:numvinc).")
  @UISchema(
      label = "Vínculo funcional",
      group = "Identificação",
      order = 4,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "work",
      endpoint = ApiPaths.VINCULO_OPTIONS,
      valueField = "id",
      displayField = "label",
      helpText = "Selecione o vínculo funcional titular para listar seus afastamentos.")
  @Filterable(operation = FilterOperation.EQUAL, relation = "vinculo.id")
  private br.com.praxis.ergonshowcase.domain.VinculoId vinculoId;

  @Schema(
      description =
          "Restringe aos afastamentos sem servidor substituto designado, apoiando auditorias de descobertura de postos durante licenças.")
  @UISchema(
      label = "Sem substituto designado",
      group = "Prorrogação e substituição",
      order = 35,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "person_off")
  @Filterable(operation = FilterOperation.IS_NULL, relation = "numfuncSubstituto")
  private Boolean semSubstituto;

  @Schema(
      description =
          "Pesquisa textual pelo nome da unidade organizacional de lotação do vínculo afastado (ex.: UPA, Hospital, Secretaria de Saúde).",
      example = "Saúde")
  @UISchema(
      label = "Nome da lotação do vínculo",
      group = "Identificação",
      order = 5,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "account_tree",
      helpText = "Digite parte do nome do setor de lotação do servidor.")
  @Filterable(operation = FilterOperation.LIKE, relation = "vinculo.lotacao.nome")
  private String setorLotacaoNome;

  @Schema(
      description =
          "Filtra afastamentos pelo tipo de unidade organizacional de lotação do vínculo (ex.: SECRETARIA, DIRETORIA, COORDENADORIA, NUCLEO, UNIDADE_SAUDE, ESCOLA).")
  @UISchema(
      label = "Tipos de unidade de lotação",
      group = "Identificação",
      order = 6,
      type = FieldDataType.ARRAY,
      controlType = FieldControlType.MULTI_SELECT,
      icon = "category",
      options =
          "[{\"value\":\"SECRETARIA\",\"label\":\"Secretaria\"},{\"value\":\"DIRETORIA\",\"label\":\"Diretoria\"},{\"value\":\"COORDENADORIA\",\"label\":\"Coordenadoria\"},{\"value\":\"NUCLEO\",\"label\":\"Núcleo\"},{\"value\":\"UNIDADE_SAUDE\",\"label\":\"Unidade de Saúde\"},{\"value\":\"ESCOLA\",\"label\":\"Unidade Escolar\"}]")
  @Filterable(operation = FilterOperation.IN, relation = "vinculo.lotacao.tipoSetor")
  private List<String> tipoSetorLotacao;

  @Schema(
      description =
          "Filtra afastamentos pela categoria funcional do vínculo titular (ex.: ADMINISTRATIVO, MAGISTERIO, SAUDE, GUARDA_MUNICIPAL).")
  @UISchema(
      label = "Categorias funcionais",
      group = "Enquadramento",
      order = 36,
      type = FieldDataType.ARRAY,
      controlType = FieldControlType.MULTI_SELECT,
      icon = "badge",
      options =
          "[{\"value\":\"ADMINISTRATIVO\",\"label\":\"Administrativo\"},{\"value\":\"MAGISTERIO\",\"label\":\"Magistério\"},{\"value\":\"SAUDE\",\"label\":\"Saúde\"},{\"value\":\"GUARDA_MUNICIPAL\",\"label\":\"Guarda Municipal\"}]")
  @Filterable(operation = FilterOperation.IN, relation = "vinculo.categoria")
  private List<String> categoriaVinculo;

  @Schema(
      description =
          "Critérios adicionais sobre campos customizados do órgão definidos para licenças e afastamentos.")
  @UISchema(
      label = "Filtros complementares",
      group = "Extensões do órgão",
      order = 900,
      tableHidden = true)
  @CustomFields(resourceKey = "administracao-pessoal.afastamentos")
  private Map<String, Object> customFields = new LinkedHashMap<>();
}
