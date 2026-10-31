package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.*;
import lombok.Getter;
import lombok.Setter;
import org.praxisplatform.uischema.*;
import org.praxisplatform.uischema.customfields.CustomFields;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.filter.annotation.Filterable;
import org.praxisplatform.uischema.filter.annotation.Filterable.FilterOperation;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;

@Getter
@Setter
@Schema(
    description =
        "Matriz exaustiva de filtros operacionais, fiscais e cadastrais de dependentes vinculados aos servidores públicos.")
public class DependenteFilterDTO implements GenericFilterDTO {
  @Schema(
      description =
          "Busca ampla unificada por nome civil ou social do dependente, CPF, RG, matrícula da certidão ou nome/CPF do servidor titular.")
  @UISchema(label = "Busca ampla", group = "Identificação", order = 1, icon = "search")
  @Filterable(
      operation = FilterOperation.LIKE,
      relations = {
        "nome",
        "nomeSocial",
        "cpf",
        "numrg",
        "matriculaCert",
        "pessoa.nome",
        "pessoa.cpf"
      })
  private String buscaAmpla;

  @Schema(description = "Filtra dependentes vinculados a um servidor público específico.")
  @UISchema(
      label = "Servidor titular",
      group = "Identificação",
      order = 2,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "person",
      endpoint = ApiPaths.PESSOA_OPTIONS,
      valueField = "id",
      displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL, relation = "id.numfunc")
  private Long numfunc;

  @Schema(description = "Filtra pelo sequencial exato do dependente no prontuário do servidor.")
  @UISchema(
      label = "Sequencial",
      group = "Identificação",
      order = 3,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.INPUT,
      icon = "format_list_numbered")
  @Filterable(operation = FilterOperation.EQUAL, relation = "id.numdep")
  private Integer numdep;

  @Schema(description = "Trecho do nome civil completo do dependente.")
  @UISchema(label = "Nome do dependente", group = "Identificação", order = 4)
  @Filterable(operation = FilterOperation.LIKE, relation = "nome")
  private String nome;

  @Schema(description = "Prefixo do CPF do dependente sem formatação.")
  @UISchema(
      label = "CPF do dependente",
      group = "Identificação",
      order = 5,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.CPF_CNPJ_INPUT,
      icon = "badge")
  @Filterable(operation = FilterOperation.STARTS_WITH, relation = "cpf")
  private String cpf;

  @Schema(description = "Trecho do nome civil do servidor titular.")
  @UISchema(label = "Nome do servidor", group = "Identificação", order = 6)
  @Filterable(operation = FilterOperation.LIKE, relation = "pessoa.nome")
  private String pessoaNome;

  @Schema(description = "Grau de parentesco exato com o servidor titular.")
  @UISchema(label = "Parentesco", group = "Enquadramento", order = 10)
  @Filterable(operation = FilterOperation.EQUAL, relation = "parentesco")
  private String parentesco;

  @Schema(description = "Conjunto de graus de parentesco a incluir na consulta.")
  @UISchema(
      label = "Parentescos incluídos",
      group = "Enquadramento",
      order = 11,
      controlType = FieldControlType.MULTI_SELECT)
  @Filterable(operation = FilterOperation.IN, relation = "parentesco")
  private List<String> parentescos;

  @Schema(description = "Conjunto de graus de parentesco a excluir da consulta.")
  @UISchema(
      label = "Parentescos excluídos",
      group = "Enquadramento",
      order = 12,
      controlType = FieldControlType.MULTI_SELECT)
  @Filterable(operation = FilterOperation.NOT_IN, relation = "parentesco")
  private List<String> parentescosExcluidos;

  @Schema(description = "Exclui um grau de parentesco específico da listagem.")
  @UISchema(label = "Exceto parentesco", group = "Enquadramento", order = 13)
  @Filterable(operation = FilterOperation.NOT_EQUAL, relation = "parentesco")
  private String parentescoDiferenteDe;

  @Schema(description = "Filtra pela modalidade de dependência do catálogo TIPO_DEPEN.")
  @UISchema(
      label = "Tipo de dependência",
      group = "Enquadramento",
      order = 14,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "category",
      endpoint = ApiPaths.TIPO_DEPENDENTE_OPTIONS,
      valueField = "id",
      displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL, relation = "tipodepen")
  private String tipodepen;

  @Schema(description = "Sexo registral do dependente ([F] ou [M]).")
  @UISchema(label = "Sexo", group = "Identificação", order = 15)
  @Filterable(operation = FilterOperation.EQUAL, relation = "sexo")
  private String sexo;

  @Schema(description = "Intervalo de datas de nascimento do dependente.")
  @UISchema(
      label = "Período de nascimento",
      group = "Datas e vigência",
      order = 20,
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_RANGE,
      icon = "date_range")
  @Filterable(operation = FilterOperation.BETWEEN, relation = "dtnasc")
  private List<LocalDate> dtnascEntre;

  @Schema(description = "Data mínima de nascimento do dependente.")
  @UISchema(
      label = "Nascido a partir de",
      group = "Datas e vigência",
      order = 21,
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "event")
  @Filterable(operation = FilterOperation.GREATER_OR_EQUAL, relation = "dtnasc")
  private LocalDate dtnascMinima;

  @Schema(description = "Data máxima de nascimento do dependente.")
  @UISchema(
      label = "Nascido até",
      group = "Datas e vigência",
      order = 22,
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "event")
  @Filterable(operation = FilterOperation.LESS_OR_EQUAL, relation = "dtnasc")
  private LocalDate dtnascMaxima;

  @Schema(description = "Intervalo de início da vigência da dependência.")
  @UISchema(
      label = "Período de início",
      group = "Datas e vigência",
      order = 23,
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_RANGE,
      icon = "date_range")
  @Filterable(operation = FilterOperation.BETWEEN, relation = "dtini")
  private List<LocalDate> dtiniEntre;

  @Schema(description = "Data exata de início de vigência da dependência.")
  @UISchema(
      label = "Início na data",
      group = "Datas e vigência",
      order = 24,
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "today")
  @Filterable(operation = FilterOperation.ON_DATE, relation = "dtini")
  private LocalDate dtiniNaData;

  @Schema(description = "Dependências iniciadas nos últimos N dias.")
  @UISchema(
      label = "Iniciados nos últimos dias",
      group = "Datas e vigência",
      order = 25,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.INPUT,
      icon = "history")
  @Filterable(operation = FilterOperation.IN_LAST_DAYS, relation = "dtini")
  private Integer iniciadosUltimosDias;

  @Schema(description = "Dependências com encerramento programado nos próximos N dias.")
  @UISchema(
      label = "Encerrando nos próximos dias",
      group = "Datas e vigência",
      order = 26,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.INPUT,
      icon = "upcoming")
  @Filterable(operation = FilterOperation.IN_NEXT_DAYS, relation = "dtfim")
  private Integer encerrandoProximosDias;

  @Schema(description = "Restringe aos dependentes com vigência ativa (sem data de fim).")
  @UISchema(
      label = "Somente vigentes",
      group = "Datas e vigência",
      order = 27,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "check_circle")
  @Filterable(operation = FilterOperation.IS_NULL, relation = "dtfim")
  private Boolean somenteVigentes;

  @Schema(description = "Restringe aos dependentes com vigência encerrada (data de fim preenchida).")
  @UISchema(
      label = "Somente encerrados",
      group = "Datas e vigência",
      order = 28,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "cancel")
  @Filterable(operation = FilterOperation.IS_NOT_NULL, relation = "dtfim")
  private Boolean somenteEncerrados;

  @Schema(description = "Restringe aos dependentes com CPF ausente para auditoria cadastral do eSocial.")
  @UISchema(
      label = "Sem CPF cadastrado",
      group = "Auditoria cadastral",
      order = 29,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "warning")
  @Filterable(operation = FilterOperation.IS_NULL, relation = "cpf")
  private Boolean semCpf;

  @Schema(description = "Restringe aos dependentes ativos para dedução de IRRF.")
  @UISchema(
      label = "Elegível a IRRF",
      group = "Elegibilidade",
      order = 30,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "receipt_long")
  @Filterable(operation = FilterOperation.IS_TRUE, relation = "depIr")
  private Boolean somenteDepIr;

  @Schema(description = "Restringe aos dependentes que não deduzem IRRF.")
  @UISchema(
      label = "Sem dedução de IRRF",
      group = "Elegibilidade",
      order = 31,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "money_off")
  @Filterable(operation = FilterOperation.IS_FALSE, relation = "depIr")
  private Boolean semDepIr;

  @Schema(description = "Restringe aos dependentes que geram cota de salário-família.")
  @UISchema(
      label = "Com salário-família",
      group = "Elegibilidade",
      order = 32,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "savings")
  @Filterable(operation = FilterOperation.IS_TRUE, relation = "depSalarioFamilia")
  private Boolean somenteSalarioFamilia;

  @Schema(description = "Restringe aos dependentes vinculados ao plano de saúde suplementar.")
  @UISchema(
      label = "Com plano de saúde",
      group = "Elegibilidade",
      order = 33,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "health_and_safety")
  @Filterable(operation = FilterOperation.IS_TRUE, relation = "depPlanoSaude")
  private Boolean somentePlanoSaude;

  @Schema(description = "Restringe aos dependentes universitários.")
  @UISchema(
      label = "Somente universitários",
      group = "Elegibilidade",
      order = 34,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "school")
  @Filterable(operation = FilterOperation.IS_TRUE, relation = "universitario")
  private Boolean somenteUniversitarios;

  @Schema(description = "Restringe aos dependentes com invalidez permanente atestada.")
  @UISchema(
      label = "Com invalidez atestada",
      group = "Elegibilidade",
      order = 35,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "medical_information")
  @Filterable(operation = FilterOperation.IS_TRUE, relation = "invalido")
  private Boolean somenteInvalidos;

  @Schema(description = "Filtra pelo documento comprobatório ou ato legal associado.")
  @UISchema(
      label = "Documento comprobatório",
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

  @Schema(
      description =
          "Critérios adicionais sobre campos customizados do órgão definidos para dependentes.")
  @UISchema(
      label = "Filtros complementares",
      group = "Extensões do órgão",
      order = 900,
      tableHidden = true)
  @CustomFields(resourceKey = "administracao-pessoal.dependentes")
  private Map<String, Object> customFields = new LinkedHashMap<>();
}
