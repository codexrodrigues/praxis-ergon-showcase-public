package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.filter.annotation.Filterable;
import org.praxisplatform.uischema.filter.annotation.Filterable.FilterOperation;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;

@Getter
@Setter
@Schema(
    description =
        "Filtros avançados para pesquisa e auditoria de ocorrências operacionais de frequência, faltas, abonos e horas extraordinárias.")
public class FrequenciaFilterDTO implements GenericFilterDTO {

  @Filterable(
      operation = FilterOperation.LIKE,
      relations = {
        "pessoa.nome",
        "pessoa.cpf",
        "vinculo.matric",
        "codigoFrequenciaEntidade.nome",
        "obs",
        "documentoLegal.numeroDocumento"
      })
  @Schema(
      description =
          "Busca ampla por nome ou CPF do servidor, matrícula do vínculo, modalidade de frequência, justificativa ou documento.")
  @UISchema(
      label = "Busca ampla de frequência",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "search",
      group = "Pesquisa Rápida",
      order = 1,
      helpText = "Pesquise por servidor, CPF, matrícula, modalidade, justificativa ou documento.")
  private String searchTerm;

  @Filterable(operation = FilterOperation.EQUAL, relation = "empCodigo")
  @Schema(description = "Filtra ocorrências de frequência por órgão público de lotação.")
  @UISchema(
      label = "Órgão",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "apartment",
      group = "Vínculo Funcional",
      order = 10,
      endpoint = ApiPaths.ORGAO_OPTIONS,
      valueField = "id",
      displayField = "label",
      helpText = "Selecione o órgão público de lotação.")
  private Integer empCodigo;

  @Filterable(operation = FilterOperation.EQUAL, relation = "numfunc")
  @Schema(description = "Filtra ocorrências pelo número funcional do servidor.")
  @UISchema(
      label = "Servidor",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "person",
      group = "Vínculo Funcional",
      order = 20,
      endpoint = ApiPaths.PESSOA_OPTIONS,
      valueField = "id",
      displayField = "label",
      helpText = "Selecione o servidor titular.")
  private Long numfunc;

  @Filterable(operation = FilterOperation.EQUAL, relation = "numvinc")
  @Schema(description = "Filtra ocorrências pelo número sequencial do vínculo funcional.")
  @UISchema(
      label = "Vínculo funcional",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "work",
      group = "Vínculo Funcional",
      order = 30,
      endpoint = ApiPaths.VINCULO_OPTIONS,
      valueField = "id",
      displayField = "label",
      helpText = "Selecione o vínculo funcional.")
  private Integer numvinc;

  @Filterable(operation = FilterOperation.EQUAL, relation = "tipofreq")
  @Schema(description = "Filtra ocorrências pelo tipo macro de frequência.")
  @UISchema(
      label = "Tipo de frequência",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "category",
      group = "Tipificação e Situação",
      order = 40,
      endpoint = ApiPaths.TIPO_FREQUENCIA_OPTIONS,
      valueField = "id",
      displayField = "label",
      helpText = "Selecione o agrupador macro de frequência.")
  private String tipofreq;

  @Filterable(operation = FilterOperation.EQUAL, relation = "codfreq")
  @Schema(description = "Filtra ocorrências pelo código detalhado de frequência.")
  @UISchema(
      label = "Código de frequência",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "pin",
      group = "Tipificação e Situação",
      order = 50,
      endpoint = ApiPaths.CODIGO_FREQUENCIA_OPTIONS,
      valueField = "id",
      displayField = "label",
      helpText = "Selecione a modalidade específica de frequência.")
  private Integer codfreq;

  @Filterable(operation = FilterOperation.EQUAL, relation = "situacao")
  @Schema(description = "Filtra ocorrências pela situação atual no workflow de aprovação.")
  @UISchema(
      label = "Situação",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      icon = "verified_user",
      group = "Tipificação e Situação",
      order = 60,
      options =
          "[{\"id\":\"PENDENTE_HOMOLOGACAO\",\"label\":\"Pendente de Homologação\"},{\"id\":\"HOMOLOGADO\",\"label\":\"Homologado\"},{\"id\":\"REJEITADO\",\"label\":\"Rejeitado\"},{\"id\":\"CANCELADO\",\"label\":\"Cancelado\"}]",
      valueField = "id",
      displayField = "label",
      helpText = "Situação da ocorrência no ciclo de homologação.")
  private String situacao;

  @Filterable(operation = FilterOperation.IN, relation = "situacao")
  @Schema(description = "Filtra ocorrências por múltiplas situações simultaneamente.")
  @UISchema(
      label = "Múltiplas situações",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.MULTI_SELECT,
      icon = "checklist",
      group = "Tipificação e Situação",
      order = 65,
      options =
          "[{\"id\":\"PENDENTE_HOMOLOGACAO\",\"label\":\"Pendente de Homologação\"},{\"id\":\"HOMOLOGADO\",\"label\":\"Homologado\"},{\"id\":\"REJEITADO\",\"label\":\"Rejeitado\"},{\"id\":\"CANCELADO\",\"label\":\"Cancelado\"}]",
      valueField = "id",
      displayField = "label",
      helpText = "Selecione uma ou mais situações para consulta combinada.")
  private List<String> situacoes;

  @Filterable(operation = FilterOperation.BETWEEN, relation = "dtini")
  @Schema(description = "Intervalo de datas de início da ocorrência de frequência.")
  @UISchema(
      label = "Período de início",
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_RANGE,
      icon = "date_range",
      group = "Recortes Temporais",
      order = 70,
      helpText = "Faixa de datas de início da ocorrência.")
  private List<LocalDate> dtiniBetween;

  @Filterable(operation = FilterOperation.IN_LAST_DAYS, relation = "dtini")
  @Schema(description = "Filtra ocorrências iniciadas nos últimos N dias.")
  @UISchema(
      label = "Iniciadas nos últimos dias",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "history",
      group = "Recortes Temporais",
      order = 80,
      helpText = "Quantidade de dias retroativos a partir de hoje.")
  private Integer iniciadasUltimosDias;

  @Filterable(operation = FilterOperation.GREATER_OR_EQUAL, relation = "quantidade")
  @Schema(description = "Quantidade mínima apurada na ocorrência.")
  @UISchema(
      label = "Quantidade mínima",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "vertical_align_bottom",
      group = "Quantidades e Auditoria",
      order = 90,
      helpText = "Filtra ocorrências com quantidade igual ou superior ao valor.")
  private BigDecimal quantidadeMin;

  @Filterable(operation = FilterOperation.LESS_OR_EQUAL, relation = "quantidade")
  @Schema(description = "Quantidade máxima apurada na ocorrência.")
  @UISchema(
      label = "Quantidade máxima",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "vertical_align_top",
      group = "Quantidades e Auditoria",
      order = 100,
      helpText = "Filtra ocorrências com quantidade igual ou inferior ao valor.")
  private BigDecimal quantidadeMax;

  @Filterable(operation = FilterOperation.EQUAL, relation = "documentoLegalId")
  @Schema(description = "Filtra ocorrências pelo documento comprobatório vinculado.")
  @UISchema(
      label = "Documento comprobatório",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "menu_book",
      group = "Quantidades e Auditoria",
      order = 110,
      endpoint = ApiPaths.DOCUMENTO_OPTIONS,
      valueField = "id",
      displayField = "label",
      helpText = "Filtra pelo documento legal ou processo comprobatório.")
  private Long documentoLegalId;

  @Filterable(operation = FilterOperation.IS_NULL, relation = "documentoLegalId")
  @Schema(description = "Identifica ocorrências sem documento comprobatório vinculado.")
  @UISchema(
      label = "Sem documento comprobatório",
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "find_in_page",
      group = "Quantidades e Auditoria",
      order = 120,
      helpText = "Quando marcado, lista ocorrências pendentes de documento anexo.")
  private Boolean semDocumentoLegal;

  @Filterable(operation = FilterOperation.EQUAL, relation = "vinculo.id")
  @Schema(
      description =
          "Filtra ocorrências de frequência pela identidade composta do vínculo funcional (numfunc:numvinc).")
  @UISchema(
      label = "Vínculo funcional",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "work",
      group = "Vínculo e Servidor",
      order = 35,
      endpoint = ApiPaths.VINCULO_OPTIONS,
      valueField = "id",
      displayField = "label",
      helpText = "Selecione o vínculo funcional titular para listar suas frequências.")
  private br.com.praxis.ergonshowcase.domain.VinculoId vinculoId;

  @Schema(
      description =
          "Critérios adicionais sobre campos customizados do órgão definidos para ocorrências de frequência.")
  @UISchema(
      label = "Filtros complementares",
      group = "Extensões do órgão",
      order = 900,
      tableHidden = true)
  @org.praxisplatform.uischema.customfields.CustomFields(
      resourceKey = "administracao-pessoal.frequencias")
  private java.util.Map<String, Object> customFields = new java.util.LinkedHashMap<>();
}
