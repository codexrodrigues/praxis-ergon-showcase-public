package br.com.praxis.ergonshowcase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.*;
import lombok.Getter;
import lombok.Setter;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.customfields.CustomFields;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.filter.annotation.Filterable;
import org.praxisplatform.uischema.filter.annotation.Filterable.FilterOperation;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;

@Getter
@Setter
@Schema(description = "Critérios de pesquisa de pessoas servidoras na administração de pessoal.")
public class ServidorPessoaFilterDTO implements GenericFilterDTO {
  @Schema(description = "Trecho do nome civil para localizar a pessoa no cadastro único.")
  @UISchema(label = "Nome", group = "Pesquisa", order = 10)
  @Filterable(operation = FilterOperation.LIKE)
  private String nome;

  @Schema(
      description = "Intervalo inclusivo de nascimento usado para localizar pessoas por período.")
  @UISchema(
      label = "Período de nascimento",
      controlType = FieldControlType.DATE_RANGE,
      group = "Pesquisa",
      order = 20)
  @Filterable(operation = FilterOperation.BETWEEN, relation = "dtnasc")
  private List<LocalDate> nascimento;

  @CustomFields(resourceKey = "administracao-pessoal.funcionarios")
  @Schema(description = "Predicados sobre os atributos civis adicionais publicados para o órgão.")
  @UISchema(label = "Características adicionais", group = "Pesquisa", order = 30)
  private Map<String, Object> customFields;
}
