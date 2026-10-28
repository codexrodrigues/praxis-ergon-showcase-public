package br.com.praxis.ergonshowcase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.Map;
import lombok.*;
import org.praxisplatform.uischema.*;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.extension.annotation.UISchema;

/** Ficha de consulta auxiliar; não publica escrita sobre o cadastro de origem. */
@Getter
@Setter
@Schema(description = "Unidade do organograma institucional e sua vigência.")
@ResourceIdentity(
    keyField = "id",
    titleField = "nome",
    subtitleField = "sigla",
    metadataFields = {"empCodigo", "tipoSetor"})
public class SetorResponseDTO {
  @Schema(
      description = "Código estruturado que identifica a unidade de lotação no organograma.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Código da unidade",
      group = "Identificação",
      readOnly = true,
      tableHidden = true)
  private String id;

  @ResourceReference(
      resourceKey = "administracao-pessoal.empresas",
      valueField = "empCodigo",
      surfaceId = "ficha-institucional",
      previewSurfaceId = "ficha-institucional",
      detailPresentation = "drawer")
  @Schema(
      description = "Ente público responsável pela unidade organizacional.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Órgão",
      group = "Identificação",
      readOnly = true,
      tableHidden = true,
      quickViewVisible = true,
      quickViewOrder = 1,
      type = org.praxisplatform.uischema.FieldDataType.TEXT,
      controlType = org.praxisplatform.uischema.FieldControlType.ENTITY_LOOKUP,
      icon = "apartment",
      endpoint = "/api/ergon/orgaos/option-sources/orgao/options/filter",
      valueField = "id",
      displayField = "label")
  private Integer empCodigo;

  @ResourceReference(
      resourceKey = "administracao-pessoal.setores",
      valueField = "id",
      surfaceId = "ficha-unidade",
      previewSurfaceId = "ficha-unidade",
      detailPresentation = "drawer")
  @Schema(
      description = "Denominação oficial da secretaria, diretoria ou unidade de lotação.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Unidade organizacional",
      group = "Identificação",
      readOnly = true,
      tableHidden = true)
  private String nome;

  @Schema(
      description = "Denominação abreviada usada para distinguir a unidade em organogramas.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Sigla", group = "Identificação", readOnly = true, tableHidden = true)
  private String sigla;

  @ResourceReference(
      resourceKey = "administracao-pessoal.setores",
      valueField = "setorPai",
      surfaceId = "ficha-unidade",
      previewSurfaceId = "ficha-unidade",
      detailPresentation = "drawer")
  @Schema(
      description = "Código da unidade imediatamente superior na hierarquia do mesmo órgão.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Unidade superior",
      group = "Organograma",
      readOnly = true,
      tableHidden = true,
      type = org.praxisplatform.uischema.FieldDataType.TEXT,
      controlType = org.praxisplatform.uischema.FieldControlType.ENTITY_LOOKUP,
      icon = "account_tree",
      endpoint = "/api/ergon/setores/option-sources/setor/options/filter",
      valueField = "id",
      displayField = "label")
  private String setorPai;

  @Schema(
      description = "Classificação administrativa da unidade, como secretaria ou diretoria.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Tipo de unidade",
      group = "Organograma",
      readOnly = true,
      tableHidden = true,
      quickViewVisible = true,
      quickViewOrder = 2)
  private String tipoSetor;

  @Schema(
      description = "Primeiro dia de vigência da unidade no organograma institucional.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Início da vigência",
      group = "Vigência",
      readOnly = true,
      tableHidden = true,
      quickViewVisible = true,
      quickViewOrder = 3,
      type = org.praxisplatform.uischema.FieldDataType.DATE,
      controlType = org.praxisplatform.uischema.FieldControlType.DATE_PICKER,
      icon = "event")
  private LocalDate dtini;

  @Schema(
      description = "Último dia de vigência; ausência indica unidade sem encerramento registrado.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Fim da vigência",
      group = "Vigência",
      readOnly = true,
      tableHidden = true,
      type = org.praxisplatform.uischema.FieldDataType.DATE,
      controlType = org.praxisplatform.uischema.FieldControlType.DATE_PICKER,
      icon = "event")
  private LocalDate dtfim;

  @Schema(
      description =
          "Inscrição da unidade tributária usada na apuração previdenciária e no eSocial.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "CNPJ da lotação", group = "Localização", readOnly = true, tableHidden = true)
  private String cgcLotacao;

  @Schema(
      description = "Município da localização física da unidade organizacional.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Município",
      group = "Localização",
      readOnly = true,
      tableHidden = true,
      quickViewVisible = true,
      quickViewOrder = 4)
  private String municipioLotacao;

  @Schema(
      description = "Unidade federativa da localização física da lotação.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "UF",
      group = "Localização",
      readOnly = true,
      tableHidden = true,
      quickViewVisible = true,
      quickViewOrder = 5)
  private String ufLotacao;

  @ResourceReference(
      resourceKey = "administracao-pessoal.documentos-legais",
      valueField = "documentoLegalId",
      surfaceId = "ficha-documento",
      previewSurfaceId = "ficha-documento",
      detailPresentation = "drawer")
  @Schema(
      description = "Documento legal que institui ou reestrutura a unidade organizacional.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Ato de criação",
      group = "Fundamentação",
      readOnly = true,
      tableHidden = true,
      type = org.praxisplatform.uischema.FieldDataType.TEXT,
      controlType = org.praxisplatform.uischema.FieldControlType.ENTITY_LOOKUP,
      icon = "gavel",
      endpoint = "/api/ergon/documentos-legais/option-sources/documento/options/filter",
      valueField = "id",
      displayField = "label")
  private Long documentoLegalId;

  @org.praxisplatform.uischema.customfields.CustomFields(
      resourceKey = "administracao-pessoal.setores")
  @Schema(
      description = "Atributos de lotação definidos pelo catálogo de campos customizados do órgão.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Características do órgão",
      group = "Características adicionais",
      readOnly = true,
      tableHidden = true)
  private Map<String, Object> customFields;
}
