package br.com.praxis.ergonshowcase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.util.*;
import lombok.Getter;
import lombok.Setter;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.extension.annotation.UISchemaPreset;
import org.praxisplatform.uischema.filter.annotation.Filterable;
import org.praxisplatform.uischema.filter.annotation.Filterable.FilterOperation;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;

@Getter
@Setter
@Schema(description = "Critérios de pesquisa de órgãos na administração de pessoal.")
public class OrgaoFilterDTO implements GenericFilterDTO {
  @Schema(description = "Trecho do nome oficial do órgão para pesquisa textual.")
  @UISchema(label = "Nome", group = "Pesquisa", order = 10, icon = "search")
  @Filterable(operation = FilterOperation.LIKE)
  private String nome;

  @Schema(description = "Restringe aos órgãos habilitados ou desabilitados para novas admissões.")
  @UISchema(label = "Ativo", preset = UISchemaPreset.BOOLEAN_FLAG, group = "Pesquisa", order = 20)
  @Filterable(operation = FilterOperation.EQUAL)
  private Boolean ativa;

  @Schema(description = "Sigla exata da unidade federativa da sede do órgão.")
  @UISchema(label = "UF", group = "Pesquisa", order = 30)
  @Filterable(operation = FilterOperation.EQUAL)
  private String ufSigla;

  @Schema(description = "Trecho da sigla institucional do órgão.")
  @UISchema(label = "Sigla", group = "Pesquisa avançada", order = 200)
  @Filterable(operation = FilterOperation.LIKE, relation = "fantasia")
  private String fantasia;

  @Schema(description = "Trecho da denominação jurídica do ente.")
  @UISchema(label = "Razão social", group = "Pesquisa avançada", order = 201)
  @Filterable(operation = FilterOperation.LIKE, relation = "razao")
  private String razao;

  @Schema(description = "Prefixo da inscrição do ente no CNPJ.")
  @UISchema(
      label = "Prefixo da inscrição do ente no CNPJ",
      group = "Pesquisa avançada",
      order = 202)
  @Filterable(operation = FilterOperation.STARTS_WITH, relation = "cnpj")
  private String cnpj;

  @Schema(description = "Unidades federativas incluídas na pesquisa.")
  @UISchema(label = "Unidades federativas", group = "Pesquisa avançada", order = 203)
  @Filterable(operation = FilterOperation.IN, relation = "ufSigla")
  private List<String> ufs;

  @Schema(description = "Trecho do município sede do órgão.")
  @UISchema(label = "Trecho do município sede do órgão", group = "Pesquisa avançada", order = 204)
  @Filterable(operation = FilterOperation.LIKE, relation = "municipio")
  private String municipio;

  @Schema(description = "Seleciona somente órgãos habilitados.")
  @UISchema(
      label = "Seleciona somente órgãos habilitados",
      group = "Pesquisa avançada",
      order = 205)
  @Filterable(operation = FilterOperation.IS_TRUE, relation = "ativa")
  private Boolean somenteAtivos;

  @Schema(description = "Seleciona órgãos sem portal informado.")
  @UISchema(label = "Sem portal informado", group = "Pesquisa avançada", order = 206)
  @Filterable(operation = FilterOperation.IS_NULL, relation = "web")
  private Boolean semPortalWeb;

  @Schema(description = "Janela em dias desde a inclusão do órgão.")
  @UISchema(label = "Criado nos últimos dias", group = "Pesquisa avançada", order = 207)
  @Filterable(operation = FilterOperation.IN_LAST_DAYS, relation = "auditoria.createdAt")
  private Integer criadoNosUltimosDias;
}
