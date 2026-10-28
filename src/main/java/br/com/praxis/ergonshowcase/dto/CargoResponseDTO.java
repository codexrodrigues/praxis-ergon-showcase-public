package br.com.praxis.ergonshowcase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.*;
import lombok.Getter;
import lombok.Setter;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.extension.annotation.UISchemaPreset;

@Getter
@Setter
@Schema(description = "Cargos e funções integrantes dos quadros de pessoal dos órgãos públicos.")
@ResourceIdentity(
    keyField = "id",
    titleField = "nome",
    displayLabelField = "nome",
    subtitleField = "categoria",
    metadataFields = {"id", "orgaoNome", "tipoCargo", "saldoVagasDisponiveis"})
public class CargoResponseDTO extends CargoDados {
  @Schema(
      description = "Código imutável que identifica o cargo ou função no quadro de pessoal.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Identificação",
      preset = UISchemaPreset.ENTERPRISE_ID,
      readOnly = true,
      formHidden = true,
      group = "Identificação",
      order = 0)
  private Integer id;

  @Schema(
      description =
          "Órgão proprietário do cargo ou vínculo, preservado durante a manutenção cadastral.")
  @UISchema(
      label = "Código do órgão",
      readOnly = true,
      tableHidden = true,
      group = "Lotação",
      order = 101,
      type = org.praxisplatform.uischema.FieldDataType.TEXT,
      controlType = org.praxisplatform.uischema.FieldControlType.ENTITY_LOOKUP,
      icon = "apartment",
      endpoint = "/api/ergon/orgaos/option-sources/orgao/options/filter",
      valueField = "id",
      displayField = "label")
  private Integer empCodigo;

  @Schema(description = "Nome oficial do órgão de origem para leitura do assentamento.")
  @UISchema(
      label = "Órgão",
      readOnly = true,
      formHidden = true,
      group = "Lotação",
      order = 102,
      icon = "apartment",
      quickViewVisible = true,
      quickViewOrder = 4)
  @ResourceReference(
      resourceKey = "administracao-pessoal.empresas",
      valueField = "empCodigo",
      surfaceId = "ficha-institucional",
      previewSurfaceId = "ficha-institucional",
      detailPresentation = "drawer")
  private String orgaoNome;

  @Schema(
      description = "Data de extinção do cargo registrada pelo comando específico.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Extinção",
      group = "Quadro de Vagas e Vigência",
      tableHidden = true,
      readOnly = true,
      formHidden = true,
      controlType = org.praxisplatform.uischema.FieldControlType.DATE_PICKER,
      type = org.praxisplatform.uischema.FieldDataType.DATE,
      icon = "event")
  private LocalDate dtExtincao;

  @Schema(
      description = "Indica existência de extinção registrada para o cargo.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Cargo extinto",
      group = "Quadro de Vagas e Vigência",
      tableHidden = true,
      readOnly = true,
      formHidden = true)
  private Boolean cargoExtinto;

  @Schema(
      description =
          "Vagas autorizadas menos vínculos ativos, afastados ou cedidos; nulo quando não há quantitativo autorizado.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Saldo de vagas",
      group = "Quadro de Vagas e Vigência",
      tableHidden = true,
      readOnly = true,
      formHidden = true,
      quickViewVisible = true,
      quickViewOrder = 5)
  private Long saldoVagasDisponiveis;

  @Schema(
      description = "Denominação do cargo agregador do quadro ao qual este cargo está associado.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Cargo agregador",
      group = "Quadro de Vagas e Vigência",
      tableHidden = true,
      formHidden = true,
      readOnly = true)
  @ResourceReference(
      resourceKey = "administracao-pessoal.cargos",
      valueField = "cargoAglutinador",
      surfaceId = "ficha-cargo",
      previewSurfaceId = "ficha-cargo",
      detailPresentation = "drawer")
  private String cargoAglutinadorNome;
}
