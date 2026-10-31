package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.filter.annotation.Filterable;
import org.praxisplatform.uischema.filter.annotation.Filterable.FilterOperation;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;

@Getter
@Setter
@Schema(
    description =
        "Critérios de consulta do cadastro auxiliar para inspeção de referências funcionais.")
public class DocumentoLegalFilterDTO implements GenericFilterDTO {
  @Schema(description = "Trecho do assunto jurídico ou administrativo do ato.")
  @UISchema(label = "Ementa", group = "Pesquisa")
  @Filterable(operation = FilterOperation.LIKE, relation = "ementa")
  private String ementa;

  @Schema(description = "Prefixo do número oficial do documento, incluindo sufixos.")
  @UISchema(label = "Número oficial", group = "Pesquisa")
  @Filterable(operation = FilterOperation.STARTS_WITH, relation = "numeroDocumento")
  private String numeroDocumento;

  @Schema(description = "Ente público emissor ou proprietário do documento.")
  @UISchema(
      label = "Órgão emissor",
      group = "Pesquisa",
      type = org.praxisplatform.uischema.FieldDataType.TEXT,
      controlType = org.praxisplatform.uischema.FieldControlType.ENTITY_LOOKUP,
      icon = "apartment",
      endpoint = ApiPaths.ORGAO_OPTIONS,
      valueField = "id",
      displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL, relation = "empCodigo")
  private Integer empCodigo;

  @Schema(description = "Espécie jurídica do documento a consultar.")
  @UISchema(label = "Espécie documental", group = "Pesquisa")
  @Filterable(operation = FilterOperation.EQUAL, relation = "tipoDocumento")
  private String tipoDocumento;

  @Schema(description = "Ano de emissão ou promulgação do ato pesquisado.")
  @UISchema(label = "Ano do ato", group = "Pesquisa")
  @Filterable(operation = FilterOperation.EQUAL, relation = "anoDocumento")
  private Integer anoDocumento;

  @Schema(description = "Restringe aos atos com data de publicação registrada.")
  @UISchema(label = "Com publicação", group = "Pesquisa")
  @Filterable(operation = FilterOperation.IS_NOT_NULL, relation = "dataPublicacao")
  private Boolean publicados;

  @Schema(description = "Restringe aos registros sem URI ou chave de anexo digital.")
  @UISchema(label = "Sem anexo digital", group = "Pesquisa")
  @Filterable(operation = FilterOperation.IS_NULL, relation = "arquivoUrl")
  private Boolean semAnexo;
}
