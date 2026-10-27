package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.domain.VinculoId;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.*;
import lombok.Getter;
import lombok.Setter;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.extension.annotation.UISchemaPreset;

@Getter
@Setter
@Schema(
    description =
        "Assentamentos funcionais por pessoa e número sequencial, com enquadramento e exercício.")
@ResourceIdentity(keyField = "id", titleField = "matric", displayLabelField = "matric")
public class VinculoResponseDTO extends VinculoDados {
  @Schema(
      description =
          "Identidade imutável formada pelo número funcional e pelo número do vínculo, como 1001:1.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Identificação",
      preset = UISchemaPreset.ENTERPRISE_ID,
      readOnly = true,
      formHidden = true,
      group = "Identificação",
      order = 0)
  private VinculoId id;

  @Schema(
      description =
          "Órgão proprietário do cargo ou vínculo, preservado durante a manutenção cadastral.")
  @UISchema(
      label = "Código do órgão",
      readOnly = true,
      tableHidden = true,
      group = "Lotação",
      order = 101)
  private Integer empCodigo;

  @Schema(description = "Nome oficial do órgão de origem para leitura do assentamento.")
  @UISchema(
      label = "Órgão",
      readOnly = true,
      formHidden = true,
      group = "Lotação",
      order = 102,
      icon = "apartment")
  @ResourceReference(
      resourceKey = "administracao-pessoal.empresas",
      valueField = "empCodigo",
      surfaceId = "detail")
  private String orgaoNome;

  @Schema(
      description =
          "Número funcional da pessoa titular do vínculo, primeiro componente da identidade.")
  @UISchema(
      label = "Número funcional",
      readOnly = true,
      group = "Identificação funcional",
      order = 1)
  private Long numfunc;

  @Schema(description = "Número sequencial do vínculo da pessoa, segundo componente da identidade.")
  @UISchema(label = "Vínculo", readOnly = true, group = "Identificação funcional", order = 2)
  private Integer numero;

  @Schema(description = "Nome civil da pessoa titular para identificação humana do assentamento.")
  @UISchema(
      label = "Pessoa servidora",
      readOnly = true,
      formHidden = true,
      group = "Identificação funcional",
      order = 3)
  @ResourceReference(
      resourceKey = "administracao-pessoal.funcionarios",
      valueField = "numfunc",
      surfaceId = "detail")
  private String pessoaNome;

  @Schema(description = "Nomenclatura oficial do cargo de provimento associado ao vínculo.")
  @UISchema(label = "Cargo", readOnly = true, formHidden = true, group = "Lotação", order = 51)
  @ResourceReference(
      resourceKey = "administracao-pessoal.cargos",
      valueField = "cargo",
      surfaceId = "detail")
  private String cargoNome;

  @Schema(
      description =
          "Estado funcional atual: ativo, afastado, cedido, aposentado ou vacante; alterado apenas pela operação de negócio pertinente.")
  @UISchema(
      label = "Situação",
      preset = UISchemaPreset.ENTERPRISE_STATUS,
      readOnly = true,
      group = "Exercício",
      order = 110)
  private String situacao;

  @Schema(description = "Data de início do efetivo exercício neste vínculo.")
  @UISchema(
      label = "Início de exercício",
      readOnly = true,
      controlType = FieldControlType.DATE_PICKER,
      group = "Exercício",
      order = 120)
  private LocalDate dtexerc;

  @Schema(description = "Data do encerramento funcional por vacância, quando registrado.")
  @UISchema(
      label = "Vacância",
      readOnly = true,
      controlType = FieldControlType.DATE_PICKER,
      group = "Exercício",
      order = 130)
  private LocalDate dtvac;

  @Schema(
      description =
          "Quantidade de dias corridos desde o exercício até a vacância ou a data atual; não desconta afastamentos nem certifica tempo previdenciário.")
  @UISchema(label = "Dias desde o exercício", readOnly = true, group = "Exercício", order = 140)
  private Long diasDesdeExercicio;
}
