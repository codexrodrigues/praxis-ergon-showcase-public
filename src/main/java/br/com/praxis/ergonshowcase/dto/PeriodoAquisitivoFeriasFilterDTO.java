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
        "Critérios ricos de filtragem de períodos aquisitivos de férias por vínculo, saldo, prescrição, faltas, suspensões e overrides individuais.")
public class PeriodoAquisitivoFeriasFilterDTO implements GenericFilterDTO {

  @Schema(
      description =
          "Busca ampla textual por nome do servidor, CPF, matrícula do vínculo, situação ou observações do período aquisitivo.")
  @UISchema(
      label = "Busca ampla",
      helpText = "Pesquise por servidor, CPF, matrícula, situação ou observação.",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "search")
  @Filterable(
      operation = FilterOperation.LIKE,
      relations = {
        "pessoa.nome",
        "pessoa.cpf",
        "vinculo.matric",
        "situacao",
        "observacoes"
      })
  private String searchTerm;

  @Schema(description = "Filtra períodos aquisitivos pelo órgão de lotação do vínculo.")
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

  @Schema(description = "Filtra períodos aquisitivos pelo servidor titular.")
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

  @Schema(description = "Filtra pelo número sequencial do vínculo funcional.")
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

  @Schema(description = "Filtra pelo sequencial do período aquisitivo no vínculo (CHAVE).")
  @UISchema(
      label = "Sequencial do PA",
      helpText = "Ordem sequencial do período aquisitivo.",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "tag")
  @Filterable(operation = FilterOperation.EQUAL, relation = "chave")
  private Integer chave;

  @Schema(description = "Filtra pelo ano de exercício de referência.")
  @UISchema(
      label = "Ano de exercício",
      helpText = "Ano civil de referência das férias.",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "calendar_month")
  @Filterable(operation = FilterOperation.EQUAL, relation = "exercicio")
  private Integer exercicio;

  @Schema(description = "Filtra pela faixa de anos de exercício.")
  @UISchema(
      label = "Faixa de exercícios",
      helpText = "Intervalo de anos de exercício.",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.RANGE_SLIDER,
      icon = "calendar_month")
  @Filterable(operation = FilterOperation.BETWEEN, relation = "exercicio")
  private List<Integer> exercicioBetween;

  @Schema(description = "Filtra por múltiplas situações operacionais do período aquisitivo.")
  @UISchema(
      label = "Situações do PA",
      helpText = "Selecione uma ou mais situações operacionais.",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.MULTI_SELECT,
      icon = "flag")
  @Filterable(operation = FilterOperation.IN, relation = "situacao")
  private List<String> situacoes;

  @Schema(description = "Exclui períodos aquisitivos nas situações informadas.")
  @UISchema(
      label = "Excluir situações",
      helpText = "Oculta períodos aquisitivos nas situações selecionadas.",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.MULTI_SELECT,
      icon = "block")
  @Filterable(operation = FilterOperation.NOT_IN, relation = "situacao")
  private List<String> situacoesExcluidas;

  @Schema(description = "Filtra pelo intervalo da data de início do período aquisitivo.")
  @UISchema(
      label = "Intervalo de início",
      helpText = "Período da data inicial de apuração.",
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_RANGE,
      icon = "date_range")
  @Filterable(operation = FilterOperation.BETWEEN, relation = "dtini")
  private List<LocalDate> dtiniBetween;

  @Schema(description = "Filtra pelo intervalo da data de término do período aquisitivo.")
  @UISchema(
      label = "Intervalo de término",
      helpText = "Período da data final de fechamento.",
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_RANGE,
      icon = "event_available")
  @Filterable(operation = FilterOperation.BETWEEN, relation = "dtfim")
  private List<LocalDate> dtfimBetween;

  @Schema(description = "Filtra pelo intervalo da data de prescrição.")
  @UISchema(
      label = "Intervalo de prescrição",
      helpText = "Período da data limite de prescrição.",
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_RANGE,
      icon = "warning")
  @Filterable(operation = FilterOperation.BETWEEN, relation = "dtPrescricao")
  private List<LocalDate> dtPrescricaoBetween;

  @Schema(description = "Filtra pela faixa de saldo de dias disponível para gozo.")
  @UISchema(
      label = "Faixa de saldo disponível",
      helpText = "Intervalo de dias disponíveis para gozo.",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.RANGE_SLIDER,
      icon = "hourglass_bottom")
  @Filterable(operation = FilterOperation.BETWEEN, relation = "saldoDiasDisponivel")
  private List<Integer> saldoDiasDisponivelBetween;

  @Schema(description = "Filtra períodos com saldo disponível maior ou igual ao valor informado.")
  @UISchema(
      label = "Saldo mínimo disponível",
      helpText = "Quantidade mínima de dias disponíveis no PA.",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "hourglass_bottom")
  @Filterable(operation = FilterOperation.GREATER_OR_EQUAL, relation = "saldoDiasDisponivel")
  private Integer saldoMinimoDisponivel;

  @Schema(description = "Filtra pela faixa de faltas apuradas no ciclo aquisitivo.")
  @UISchema(
      label = "Faixa de faltas apuradas",
      helpText = "Intervalo de faltas injustificadas no período.",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.RANGE_SLIDER,
      icon = "event_busy")
  @Filterable(operation = FilterOperation.BETWEEN, relation = "faltasApuradas")
  private List<Integer> faltasApuradasBetween;

  @Schema(description = "Filtra períodos aquisitivos com prescrição nos próximos N dias.")
  @UISchema(
      label = "Prescrevendo nos próximos dias",
      helpText = "Radar de períodos aquisitivos prestes a prescrever.",
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

  @Schema(description = "Filtra pelo indicador de período gerado por averbação.")
  @UISchema(
      label = "Gerado por averbação",
      helpText = "Filtra períodos originados por averbação.",
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "lock")
  @Filterable(operation = FilterOperation.EQUAL, relation = "geradoPorAverbacao")
  private Boolean geradoPorAverbacao;

  @Schema(description = "Filtra períodos aquisitivos sem documento legal comprobatório.")
  @UISchema(
      label = "Sem documento legal",
      helpText = "Exibe períodos aquisitivos sem portaria associada.",
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "find_in_page")
  @Filterable(operation = FilterOperation.IS_NULL, relation = "documentoLegalId")
  private Boolean semDocumentoLegal;

  @Schema(
      description =
          "Filtra períodos aquisitivos com quantidade mínima de dias convertidos em dobro.")
  @UISchema(
      label = "Dias em dobro mínimos",
      helpText = "Informe o mínimo de dias convertidos em dobro.",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "double_arrow")
  @Filterable(operation = FilterOperation.GREATER_OR_EQUAL, relation = "diasConvertidosDobro")
  private Integer diasConvertidosDobroMinimo;

  @Schema(description = "Critérios de filtragem sobre atributos dinâmicos customizados.")
  @CustomFields(resourceKey = "administracao-pessoal.periodos-aquisitivos-ferias")
  @UISchema(
      label = "Campos customizados",
      helpText = "Filtros adicionais sobre propriedades dinâmicas do período aquisitivo.",
      group = "Campos Customizados",
      order = 900)
  private Map<String, Object> customFields = new LinkedHashMap<>();
}
