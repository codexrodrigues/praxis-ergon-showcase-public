package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.domain.VinculoId;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.filter.annotation.Filterable;
import org.praxisplatform.uischema.filter.annotation.Filterable.FilterOperation;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;

@Getter
@Setter
@NoArgsConstructor
@Schema(
    description =
        "Critérios analíticos de filtragem do radar de prescrição e saldo de férias por órgão, servidor, setor, cargo, regime, faixas de urgência, cobertura de programação e janelas temporais.")
public class RadarFeriasPrescricaoFilterDTO implements GenericFilterDTO {

  @Schema(
      description =
          "Busca ampla textual por nome do servidor, matrícula do vínculo, setor, cargo, situação ou faixa de urgência prescricional.")
  @UISchema(
      label = "Busca ampla",
      helpText = "Pesquise por servidor, matrícula, setor, cargo ou urgência.",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "search")
  @Filterable(
      operation = FilterOperation.LIKE,
      relations = {
        "pessoaNome",
        "vinculoMatricula",
        "setorNome",
        "cargoNome",
        "situacao",
        "faixaUrgenciaPrescricao"
      })
  private String searchTerm;

  @Schema(description = "Filtra registros do radar pelo órgão público de lotação do vínculo.")
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

  @Schema(description = "Filtra registros do radar pelo servidor público titular.")
  @UISchema(
      label = "Servidor",
      helpText = "Pessoa servidora titular do período aquisitivo.",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "person",
      endpoint = ApiPaths.PESSOA_OPTIONS,
      valueField = "id",
      displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL, relation = "numfunc")
  private Long numfunc;

  @Schema(description = "Filtra pelo número sequencial do vínculo funcional do servidor.")
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
      helpText = "Assentamento funcional titular do período aquisitivo.",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "badge",
      endpoint = ApiPaths.VINCULO_OPTIONS,
      valueField = "id",
      displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL, relation = "vinculo.id")
  private VinculoId vinculoId;

  @Schema(description = "Filtra pelo código da unidade organizacional (setor) de lotação.")
  @UISchema(
      label = "Setor de lotação",
      helpText = "Unidade organizacional de lotação do vínculo.",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "account_tree",
      endpoint = ApiPaths.SETOR_OPTIONS,
      valueField = "id",
      displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL, relation = "setorCodigo")
  private String setorCodigo;

  @Schema(description = "Filtra pelo código identificador do cargo público ocupado.")
  @UISchema(
      label = "Cargo público",
      helpText = "Cargo público ocupado pelo vínculo funcional.",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "work_outline",
      endpoint = ApiPaths.CARGO_OPTIONS,
      valueField = "id",
      displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL, relation = "cargoCodigo")
  private Integer cargoCodigo;

  @Schema(description = "Filtra por múltiplos regimes jurídicos do vínculo funcional.")
  @UISchema(
      label = "Regimes jurídicos",
      helpText = "Selecione um ou mais regimes jurídicos.",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.MULTI_SELECT,
      icon = "gavel")
  @Filterable(operation = FilterOperation.IN, relation = "regimeJuridico")
  private List<String> regimesJuridicos;

  @Schema(description = "Filtra por múltiplas categorias funcionais do vínculo.")
  @UISchema(
      label = "Categorias funcionais",
      helpText = "Selecione uma ou mais categorias funcionais.",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.MULTI_SELECT,
      icon = "category")
  @Filterable(operation = FilterOperation.IN, relation = "categoria")
  private List<String> categorias;

  @Schema(description = "Filtra pelo ano de exercício de referência do período aquisitivo.")
  @UISchema(
      label = "Ano de exercício",
      helpText = "Ano civil de referência das férias.",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "calendar_month")
  @Filterable(operation = FilterOperation.EQUAL, relation = "exercicio")
  private Integer exercicio;

  @Schema(description = "Filtra por múltiplas situações operacionais do período aquisitivo.")
  @UISchema(
      label = "Situações do PA",
      helpText = "Selecione uma ou mais situações operacionais.",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.MULTI_SELECT,
      icon = "flag")
  @Filterable(operation = FilterOperation.IN, relation = "situacao")
  private List<String> situacoes;

  @Schema(description = "Exclui períodos aquisitivos nas situações operacionais informadas.")
  @UISchema(
      label = "Excluir situações",
      helpText = "Oculta períodos aquisitivos nas situações selecionadas.",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.MULTI_SELECT,
      icon = "block")
  @Filterable(operation = FilterOperation.NOT_IN, relation = "situacao")
  private List<String> situacoesExcluidas;

  @Schema(
      description =
          "Filtra por uma ou mais faixas de urgência prescricional (VENCIDO_PRESCRITO, CRITICO_30D, ALERTA_60D, ATENCAO_90D, REGULAR).")
  @UISchema(
      label = "Faixas de urgência",
      helpText = "Selecione as faixas de risco prescricional desejadas.",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.MULTI_SELECT,
      icon = "notification_important")
  @Filterable(operation = FilterOperation.IN, relation = "faixaUrgenciaPrescricao")
  private List<String> faixasUrgenciaPrescricao;

  @Schema(description = "Filtra pela faixa de saldo disponível de dias no período aquisitivo.")
  @UISchema(
      label = "Faixa de saldo disponível",
      helpText = "Intervalo de dias disponíveis para gozo.",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.RANGE_SLIDER,
      icon = "hourglass_bottom")
  @Filterable(operation = FilterOperation.BETWEEN, relation = "saldoDiasDisponivel")
  private List<Integer> saldoDiasDisponivelBetween;

  @Schema(
      description =
          "Filtra períodos com saldo descoberto (sem programação futura) maior ou igual ao valor informado.")
  @UISchema(
      label = "Saldo mínimo sem programação",
      helpText = "Mínimo de dias disponíveis descobertos de programação.",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "event_repeat")
  @Filterable(operation = FilterOperation.GREATER_OR_EQUAL, relation = "saldoSemProgramacao")
  private Integer saldoSemProgramacaoMinimo;

  @Schema(
      description =
          "Filtra períodos cuja contagem regressiva para prescrição seja menor ou igual ao limite em dias.")
  @UISchema(
      label = "Máximo de dias até prescrição",
      helpText = "Teto de dias corridos restantes até a prescrição.",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "timer")
  @Filterable(operation = FilterOperation.LESS_OR_EQUAL, relation = "diasAtePrescricao")
  private Integer diasAtePrescricaoMaximo;

  @Schema(description = "Filtra pelo intervalo da data limite de prescrição.")
  @UISchema(
      label = "Intervalo de prescrição",
      helpText = "Período da data limite de prescrição.",
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_RANGE,
      icon = "warning")
  @Filterable(operation = FilterOperation.BETWEEN, relation = "dtPrescricao")
  private List<LocalDate> dtPrescricaoBetween;

  @Schema(description = "Filtra períodos com prescrição prevista nos próximos N dias.")
  @UISchema(
      label = "Prescrevendo nos próximos dias",
      helpText = "Janela futura em dias para prescrição iminente.",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "upcoming")
  @Filterable(operation = FilterOperation.IN_NEXT_DAYS, relation = "dtPrescricao")
  private Integer prescrevendoProximosDias;

  @Schema(description = "Filtra períodos aquisitivos encerrados nos últimos N dias.")
  @UISchema(
      label = "Encerrados nos últimos dias",
      helpText = "Períodos aquisitivos concluídos recentemente.",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "history")
  @Filterable(operation = FilterOperation.IN_LAST_DAYS, relation = "dtfim")
  private Integer encerradosUltimosDias;

  @Schema(
      description =
          "Filtra exclusivamente vínculos em risco de acúmulo de dois ou mais períodos aquisitivos abertos.")
  @UISchema(
      label = "Em risco de acúmulo de PAs",
      helpText = "Exibe vínculos com 2 ou mais períodos abertos com saldo.",
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "layers_clear")
  @Filterable(operation = FilterOperation.IS_TRUE, relation = "emRiscoAcumuloPeriodos")
  private Boolean emRiscoAcumuloPeriodos;

  @Schema(description = "Filtra pelo indicador de existência de parcela futura programada.")
  @UISchema(
      label = "Possui parcela programada",
      helpText = "Filtra períodos com ou sem parcela programada.",
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "event_upcoming")
  @Filterable(operation = FilterOperation.EQUAL, relation = "possuiParcelaProgramada")
  private Boolean possuiParcelaProgramada;

  @Schema(
      description =
          "Filtra períodos aquisitivos que não possuem data de início de próxima parcela agendada.")
  @UISchema(
      label = "Sem próxima parcela agendada",
      helpText = "Exibe períodos sem data futura de início de gozo.",
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "event_busy")
  @Filterable(operation = FilterOperation.IS_NULL, relation = "proximaParcelaInicio")
  private Boolean semProximaParcelaAgendada;

  @Schema(
      description =
          "Filtra períodos aquisitivos pela natureza operacional da unidade de lotação do vínculo (ex.: SECRETARIA, DIRETORIA, COORDENADORIA, NUCLEO, UNIDADE_SAUDE, ESCOLA).")
  @UISchema(
      label = "Tipos de unidade de lotação",
      helpText = "Selecione os tipos de unidade organizacional.",
      type = FieldDataType.ARRAY,
      controlType = FieldControlType.MULTI_SELECT,
      icon = "category",
      options =
          "[{\"value\":\"SECRETARIA\",\"label\":\"Secretaria\"},{\"value\":\"DIRETORIA\",\"label\":\"Diretoria\"},{\"value\":\"COORDENADORIA\",\"label\":\"Coordenadoria\"},{\"value\":\"NUCLEO\",\"label\":\"Núcleo\"},{\"value\":\"UNIDADE_SAUDE\",\"label\":\"Unidade de Saúde\"},{\"value\":\"ESCOLA\",\"label\":\"Unidade Escolar\"}]")
  @Filterable(operation = FilterOperation.IN, relation = "setor.tipoSetor")
  private List<String> tipoSetor;

  @Schema(
      description =
          "Filtra períodos aquisitivos por uma ou mais categorias funcionais do vínculo (ex.: ADMINISTRATIVO, MAGISTERIO, SAUDE, GUARDA_MUNICIPAL).")
  @UISchema(
      label = "Categorias funcionais",
      helpText = "Selecione uma ou mais categorias funcionais.",
      type = FieldDataType.ARRAY,
      controlType = FieldControlType.MULTI_SELECT,
      icon = "badge",
      options =
          "[{\"value\":\"ADMINISTRATIVO\",\"label\":\"Administrativo\"},{\"value\":\"MAGISTERIO\",\"label\":\"Magistério\"},{\"value\":\"SAUDE\",\"label\":\"Saúde\"},{\"value\":\"GUARDA_MUNICIPAL\",\"label\":\"Guarda Municipal\"}]")
  @Filterable(operation = FilterOperation.IN, relation = "categoria")
  private List<String> categoriasIn;

  @Schema(
      description =
          "Filtra períodos aquisitivos sem nenhuma parcela de férias futura programada ou homologada (possuiParcelaProgramada = false).")
  @UISchema(
      label = "Sem parcela programada",
      helpText = "Exibe períodos cujo saldo não possui parcela futura.",
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "event_busy")
  @Filterable(operation = FilterOperation.IS_FALSE, relation = "possuiParcelaProgramada")
  private Boolean semParcelaProgramada;
}
