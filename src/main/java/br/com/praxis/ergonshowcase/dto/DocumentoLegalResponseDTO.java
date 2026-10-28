package br.com.praxis.ergonshowcase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.*;
import org.praxisplatform.uischema.*;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.extension.annotation.UISchema;

/** Ficha de consulta auxiliar; não publica escrita sobre o cadastro de origem. */
@Getter
@Setter
@Schema(description = "Ato administrativo e referências de publicação e instrução documental.")
@ResourceIdentity(
    keyField = "id",
    titleField = "ementa",
    subtitleField = "tipoDocumento",
    metadataFields = {"empCodigo", "numeroDocumento"})
public class DocumentoLegalResponseDTO {
  @Schema(
      description = "Identidade estável do diploma legal, ato administrativo ou publicação.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Documento", group = "Identificação", readOnly = true, tableHidden = true)
  private Long id;

  @ResourceReference(
      resourceKey = "administracao-pessoal.empresas",
      valueField = "empCodigo",
      surfaceId = "ficha-institucional",
      previewSurfaceId = "ficha-institucional",
      detailPresentation = "drawer")
  @Schema(
      description = "Ente público emissor ou proprietário do documento administrativo.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Órgão emissor",
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

  @Schema(
      description = "Espécie jurídica do documento, como lei, decreto, portaria ou despacho.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Espécie documental",
      group = "Identificação",
      readOnly = true,
      tableHidden = true)
  private String tipoDocumento;

  @Schema(
      description = "Número oficial do ato, preservando sufixos e formatação da publicação.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Número oficial",
      group = "Identificação",
      readOnly = true,
      tableHidden = true,
      quickViewVisible = true,
      quickViewOrder = 2)
  private String numeroDocumento;

  @Schema(
      description = "Ano de emissão ou promulgação do documento legal.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Ano do ato",
      group = "Identificação",
      readOnly = true,
      tableHidden = true,
      quickViewVisible = true,
      quickViewOrder = 3)
  private Integer anoDocumento;

  @Schema(
      description = "Data em que o ato foi divulgado oficialmente.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Publicação",
      group = "Publicação",
      readOnly = true,
      tableHidden = true,
      quickViewVisible = true,
      quickViewOrder = 4,
      type = org.praxisplatform.uischema.FieldDataType.DATE,
      controlType = org.praxisplatform.uischema.FieldControlType.DATE_PICKER,
      icon = "event")
  private LocalDate dataPublicacao;

  @Schema(
      description = "Número da edição do diário ou boletim que veiculou o ato.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Edição oficial", group = "Publicação", readOnly = true, tableHidden = true)
  private String numeroPublicacao;

  @Schema(
      description = "Diário oficial, boletim ou outro veículo usado para a publicação do ato.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Veículo oficial",
      group = "Publicação",
      readOnly = true,
      tableHidden = true,
      quickViewVisible = true,
      quickViewOrder = 5)
  private String veiculoPublicacao;

  @ResourceReference(
      resourceKey = "administracao-pessoal.documentos-legais",
      valueField = "id",
      surfaceId = "ficha-documento",
      previewSurfaceId = "ficha-documento",
      detailPresentation = "drawer")
  @Schema(
      description =
          "Assunto jurídico ou administrativo que permite identificar a finalidade do ato.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Ementa", group = "Fundamentação", readOnly = true, tableHidden = true)
  private String ementa;

  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "A instrução do ato pode conter dados pessoais e referências documentais restritas.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @Schema(
      description = "Protocolo do processo que instruiu a emissão do ato ou documento.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Processo administrativo",
      group = "Fundamentação",
      readOnly = true,
      tableHidden = true)
  private String processoAdministrativo;

  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "A instrução do ato pode conter dados pessoais e referências documentais restritas.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @Schema(
      description =
          "URI ou chave de armazenamento do anexo digital, sem promessa de acesso público.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Documento digital", group = "Anexo", readOnly = true, tableHidden = true)
  private String arquivoUrl;

  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "A instrução do ato pode conter dados pessoais e referências documentais restritas.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @Schema(
      description = "Nome original do arquivo anexado ao registro do ato administrativo.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Nome do arquivo", group = "Anexo", readOnly = true, tableHidden = true)
  private String arquivoNome;

  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "A instrução do ato pode conter dados pessoais e referências documentais restritas.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @Schema(
      description = "Tipo MIME do anexo usado para selecionar um visualizador compatível.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Formato do arquivo", group = "Anexo", readOnly = true, tableHidden = true)
  private String arquivoMimeType;

  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "A instrução do ato pode conter dados pessoais e referências documentais restritas.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @Schema(
      description = "Anotações sobre vigência, retificações ou efeitos administrativos do ato.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Observações", group = "Fundamentação", readOnly = true, tableHidden = true)
  private String observacao;
}
