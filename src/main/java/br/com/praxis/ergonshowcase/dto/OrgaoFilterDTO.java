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
}
