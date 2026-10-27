package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.util.*;
import lombok.Getter;
import lombok.Setter;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.NumericFormat;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.customfields.CustomFields;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.extension.annotation.UISchemaPreset;

/** Campos cadastrais do vínculo; transições funcionais pertencem aos comandos próprios. */
@Getter
@Setter
public abstract class VinculoDados {
  @Size(max = 20)
  @Schema(
      description =
          "Matrícula institucional atribuída ao vínculo funcional dentro do órgão; distingue assentamentos da mesma pessoa.")
  @UISchema(
      label = "Matrícula",
      preset = UISchemaPreset.ENTERPRISE_CODE,
      group = "Identificação funcional",
      order = 10,
      icon = "badge")
  private String matric;

  @NotBlank
  @Size(max = 20)
  @Schema(
      description =
          "Regime jurídico que fundamenta a relação funcional, como estatutário ou celetista.")
  @UISchema(label = "Regime jurídico", group = "Enquadramento", order = 20, icon = "gavel")
  private String regimejur;

  @NotBlank
  @Size(max = 20)
  @Schema(
      description =
          "Modalidade do vínculo funcional, como efetivo, comissão ou temporário, usada no enquadramento estatutário.")
  @UISchema(label = "Tipo de vínculo", group = "Enquadramento", order = 30, icon = "work")
  private String tipovinc;

  @NotBlank
  @Size(max = 20)
  @Schema(
      description =
          "Categoria funcional do assentamento, utilizada no recorte de regras de pessoal.")
  @UISchema(label = "Categoria", group = "Enquadramento", order = 40, icon = "category")
  private String categoria;

  @Schema(
      description =
          "Cargo de provimento ocupado pelo vínculo; deve pertencer ao mesmo órgão do assentamento.")
  @UISchema(
      label = "Cargo",
      controlType = FieldControlType.ENTITY_LOOKUP,
      endpoint = ApiPaths.CARGO_OPTIONS,
      valueField = "id",
      displayField = "label",
      tableHidden = true,
      group = "Lotação",
      order = 50,
      icon = "work")
  private Integer cargo;

  @Size(max = 20)
  @Schema(
      description = "Referência ou padrão salarial vigente na tabela de vencimentos do vínculo.")
  @UISchema(label = "Referência salarial", group = "Enquadramento", order = 60, icon = "payments")
  private String referencia;

  @Positive
  @Max(168)
  @Schema(
      description =
          "Carga horária semanal contratual ou estatutária do vínculo, expressa em horas.")
  @UISchema(
      label = "Jornada semanal",
      numericFormat = NumericFormat.INTEGER,
      group = "Enquadramento",
      order = 70,
      icon = "schedule")
  private Integer jornadaSemanal;

  @CustomFields(resourceKey = "administracao-pessoal.vinculos")
  @Schema(
      description =
          "Atributos funcionais definidos no catálogo do órgão, incluindo o processo SEI de nomeação e o regime de teletrabalho.")
  @UISchema(label = "Características do órgão", group = "Características adicionais", order = 80)
  private Map<String, Object> customFields = new LinkedHashMap<>();
}
