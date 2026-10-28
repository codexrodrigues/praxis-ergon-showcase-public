package br.com.praxis.ergonshowcase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.*;
import lombok.Getter;
import lombok.Setter;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.customfields.CustomFields;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.filter.annotation.Filterable;
import org.praxisplatform.uischema.filter.annotation.Filterable.FilterOperation;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;

@Getter
@Setter
@Schema(description = "Critérios de pesquisa de vínculos na administração de pessoal.")
public class VinculoFilterDTO implements GenericFilterDTO {
  @Schema(description = "Número funcional exato da pessoa cujos vínculos serão consultados.")
  @UISchema(
      label = "Pessoa servidora",
      controlType = org.praxisplatform.uischema.FieldControlType.ENTITY_LOOKUP,
      endpoint = "/api/ergon/servidor-pessoas/option-sources/pessoa/options/filter",
      valueField = "id",
      displayField = "label",
      group = "Pesquisa",
      order = 10,
      type = org.praxisplatform.uischema.FieldDataType.TEXT,
      icon = "person")
  @Filterable(operation = FilterOperation.EQUAL, relation = "id.numfunc")
  private Long numfunc;

  @Schema(description = "Número sequencial exato do vínculo dentro do cadastro da pessoa.")
  @UISchema(label = "Vínculo", group = "Pesquisa", order = 20)
  @Filterable(operation = FilterOperation.EQUAL, relation = "id.numero")
  private Integer numero;

  @Schema(description = "Órgão de origem dos assentamentos funcionais pesquisados.")
  @UISchema(
      label = "Órgão",
      controlType = org.praxisplatform.uischema.FieldControlType.ENTITY_LOOKUP,
      endpoint = "/api/ergon/orgaos/option-sources/orgao/options/filter",
      valueField = "id",
      displayField = "label",
      group = "Pesquisa",
      order = 30,
      type = org.praxisplatform.uischema.FieldDataType.TEXT,
      icon = "apartment")
  @Filterable(operation = FilterOperation.EQUAL)
  private Integer empCodigo;

  @Schema(
      description = "Estados funcionais incluídos no resultado, como ATIVO, AFASTADO ou CEDIDO.")
  @UISchema(label = "Situações", group = "Pesquisa", order = 40)
  @Filterable(operation = FilterOperation.IN, relation = "situacao")
  private List<String> situacoes;

  @Schema(description = "Intervalo inclusivo de início do efetivo exercício do vínculo.")
  @UISchema(
      label = "Período de exercício",
      controlType = org.praxisplatform.uischema.FieldControlType.DATE_RANGE,
      group = "Pesquisa",
      order = 50,
      type = org.praxisplatform.uischema.FieldDataType.DATE,
      icon = "event")
  @Filterable(operation = FilterOperation.BETWEEN, relation = "dtexerc")
  private List<LocalDate> exercicio;

  @Schema(
      description =
          "Janela móvel em dias para localizar vínculos que iniciaram exercício recentemente.")
  @UISchema(label = "Exercício nos últimos dias", group = "Pesquisa", order = 60)
  @Filterable(operation = FilterOperation.IN_LAST_DAYS, relation = "dtexerc")
  private Integer exercicioRecente;

  @Schema(description = "Trecho da matrícula institucional do vínculo.")
  @UISchema(label = "Matrícula", group = "Pesquisa", order = 70)
  @Filterable(operation = FilterOperation.LIKE)
  private String matric;

  @CustomFields(resourceKey = "administracao-pessoal.vinculos")
  @Schema(
      description =
          "Predicados sobre atributos funcionais dinâmicos, validados pelo catálogo do órgão.")
  @UISchema(label = "Características adicionais", group = "Pesquisa", order = 80)
  private Map<String, Object> customFields;

  @Schema(description = "Trecho do nome civil da pessoa titular do vínculo.")
  @UISchema(label = "Pessoa servidora", group = "Pesquisa avançada", order = 200)
  @Filterable(operation = FilterOperation.LIKE, relation = "pessoa.nome")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Critério de pesquisa sobre dado pessoal do cadastro civil.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private String pessoaNome;

  @Schema(description = "CPF exato da pessoa titular do vínculo.")
  @UISchema(label = "CPF da pessoa", group = "Pesquisa avançada", order = 201)
  @Filterable(operation = FilterOperation.EQUAL, relation = "pessoa.cpf")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Critério de pesquisa sobre dado pessoal do cadastro civil.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  private String pessoaCpf;

  @Schema(description = "Prefixo da matrícula do vínculo no sistema externo.")
  @UISchema(label = "Matrícula externa", group = "Pesquisa avançada", order = 202)
  @Filterable(operation = FilterOperation.STARTS_WITH, relation = "matriculaExterna")
  private String matriculaExterna;

  @Schema(description = "Estados funcionais excluídos da consulta.")
  @UISchema(label = "Excluir situações", group = "Pesquisa avançada", order = 203)
  @Filterable(operation = FilterOperation.NOT_IN, relation = "situacao")
  private List<String> excluirSituacoes;

  @Schema(description = "Regimes jurídicos dos vínculos incluídos.")
  @UISchema(label = "Regimes jurídicos", group = "Pesquisa avançada", order = 204)
  @Filterable(operation = FilterOperation.IN, relation = "regimejur")
  private List<String> regimesJuridicos;

  @Schema(description = "Modalidades de vínculo incluídas.")
  @UISchema(label = "Tipos de vínculo", group = "Pesquisa avançada", order = 205)
  @Filterable(operation = FilterOperation.IN, relation = "tipovinc")
  private List<String> tiposVinculo;

  @Schema(description = "Categorias funcionais incluídas.")
  @UISchema(label = "Categorias", group = "Pesquisa avançada", order = 206)
  @Filterable(operation = FilterOperation.IN, relation = "categoria")
  private List<String> categorias;

  @Schema(description = "Código do cargo ocupado pelo vínculo.")
  @UISchema(
      label = "Código do cargo ocupado pelo vínculo",
      group = "Pesquisa avançada",
      order = 207,
      type = org.praxisplatform.uischema.FieldDataType.TEXT,
      controlType = org.praxisplatform.uischema.FieldControlType.ENTITY_LOOKUP,
      icon = "badge",
      endpoint = "/api/ergon/cargos/option-sources/cargo/options/filter",
      valueField = "id",
      displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL, relation = "cargo")
  private Integer cargo;

  @Schema(description = "Trecho da nomenclatura do cargo ocupado.")
  @UISchema(label = "Cargo", group = "Pesquisa avançada", order = 208)
  @Filterable(operation = FilterOperation.LIKE, relation = "cargoEntidade.nome")
  private String cargoNome;

  @Schema(description = "Código exato do cargo em comissão.")
  @UISchema(
      label = "Cargo em comissão",
      group = "Pesquisa avançada",
      order = 209,
      type = org.praxisplatform.uischema.FieldDataType.TEXT,
      controlType = org.praxisplatform.uischema.FieldControlType.ENTITY_LOOKUP,
      icon = "badge",
      endpoint = "/api/ergon/cargos/option-sources/cargo/options/filter",
      valueField = "id",
      displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL, relation = "cargoComissao")
  private Integer cargoComissao;

  @Schema(description = "Seleciona vínculos com cargo em comissão.")
  @UISchema(label = "Com cargo em comissão", group = "Pesquisa avançada", order = 210)
  @Filterable(operation = FilterOperation.IS_NOT_NULL, relation = "cargoComissao")
  private Boolean possuiCargoComissao;

  @Schema(description = "Unidade de lotação do vínculo.")
  @UISchema(
      label = "Lotação",
      group = "Pesquisa avançada",
      order = 211,
      type = org.praxisplatform.uischema.FieldDataType.TEXT,
      controlType = org.praxisplatform.uischema.FieldControlType.ENTITY_LOOKUP,
      icon = "account_tree",
      endpoint = "/api/ergon/setores/option-sources/setor/options/filter",
      valueField = "id",
      displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL, relation = "setor")
  private String setor;

  @Schema(description = "Unidade de exercício efetivo do vínculo.")
  @UISchema(
      label = "Exercício",
      group = "Pesquisa avançada",
      order = 212,
      type = org.praxisplatform.uischema.FieldDataType.TEXT,
      controlType = org.praxisplatform.uischema.FieldControlType.ENTITY_LOOKUP,
      icon = "business",
      endpoint = "/api/ergon/setores/option-sources/setor/options/filter",
      valueField = "id",
      displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL, relation = "setorExercicio")
  private String setorExercicio;

  @Schema(description = "Intervalo inclusivo da jornada semanal em horas.")
  @UISchema(label = "Intervalo de jornada", group = "Pesquisa avançada", order = 213)
  @Filterable(operation = FilterOperation.BETWEEN, relation = "jornadaSemanal")
  private List<Integer> jornadaRange;

  @Schema(description = "Regimes previdenciários incluídos.")
  @UISchema(label = "Regimes previdenciários", group = "Pesquisa avançada", order = 214)
  @Filterable(operation = FilterOperation.IN, relation = "regimeprev")
  private List<String> regimesPrevidenciarios;

  @Schema(description = "Intervalo inclusivo da data de nomeação.")
  @UISchema(
      label = "Período de nomeação",
      group = "Pesquisa avançada",
      order = 215,
      controlType = org.praxisplatform.uischema.FieldControlType.DATE_RANGE,
      type = org.praxisplatform.uischema.FieldDataType.DATE,
      icon = "event")
  @Filterable(operation = FilterOperation.BETWEEN, relation = "dtnom")
  private List<LocalDate> nomeacaoRange;

  @Schema(description = "Intervalo inclusivo da data de posse.")
  @UISchema(
      label = "Período de posse",
      group = "Pesquisa avançada",
      order = 216,
      controlType = org.praxisplatform.uischema.FieldControlType.DATE_RANGE,
      type = org.praxisplatform.uischema.FieldDataType.DATE,
      icon = "event")
  @Filterable(operation = FilterOperation.BETWEEN, relation = "dtposse")
  private List<LocalDate> posseRange;

  @Schema(description = "Data exata de início do exercício.")
  @UISchema(
      label = "Exercício na data",
      group = "Pesquisa avançada",
      order = 217,
      controlType = org.praxisplatform.uischema.FieldControlType.DATE_PICKER,
      type = org.praxisplatform.uischema.FieldDataType.DATE,
      icon = "event")
  @Filterable(operation = FilterOperation.ON_DATE, relation = "dtexerc")
  private LocalDate exercicioNaData;

  @Schema(description = "Janela em dias até o fim do contrato temporário.")
  @UISchema(label = "Contrato termina nos próximos dias", group = "Pesquisa avançada", order = 218)
  @Filterable(operation = FilterOperation.IN_NEXT_DAYS, relation = "dtfimcontr")
  private Integer fimContratoNosProximosDias;

  @Schema(description = "Intervalo inclusivo de vacância.")
  @UISchema(
      label = "Período de vacância",
      group = "Pesquisa avançada",
      order = 219,
      controlType = org.praxisplatform.uischema.FieldControlType.DATE_RANGE,
      type = org.praxisplatform.uischema.FieldDataType.DATE,
      icon = "event")
  @Filterable(operation = FilterOperation.BETWEEN, relation = "dtvac")
  private List<LocalDate> vacanciaRange;

  @Schema(description = "Seleciona vínculos sem data de vacância.")
  @UISchema(label = "Sem vacância", group = "Pesquisa avançada", order = 220)
  @Filterable(operation = FilterOperation.IS_NULL, relation = "dtvac")
  private Boolean emExercicioSemVacancia;

  @Schema(description = "Seleciona vínculos com estágio homologado.")
  @UISchema(label = "Estágio homologado", group = "Pesquisa avançada", order = 221)
  @Filterable(operation = FilterOperation.IS_NOT_NULL, relation = "dtHomologEstagio")
  private Boolean estagioHomologado;

  @Schema(description = "Seleciona vínculos com aposentadoria registrada.")
  @UISchema(label = "Aposentado", group = "Pesquisa avançada", order = 222)
  @Filterable(operation = FilterOperation.IS_NOT_NULL, relation = "dtaposent")
  private Boolean aposentado;

  @Schema(description = "Seleciona vínculos com informação de cota.")
  @UISchema(label = "Informação de cota", group = "Pesquisa avançada", order = 223)
  @Filterable(operation = FilterOperation.IS_TRUE, relation = "infoCota")
  private Boolean infoCota;

  @Schema(description = "Pesquisa a incidência de desconto de imposto de renda.")
  @UISchema(label = "Desconta IR", group = "Pesquisa avançada", order = 224)
  @Filterable(operation = FilterOperation.EQUAL, relation = "descontaIr")
  private Boolean descontaIr;
}
