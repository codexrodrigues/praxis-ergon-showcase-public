package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.domain.CodigoFrequenciaId;
import br.com.praxis.ergonshowcase.domain.VinculoId;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.praxisplatform.uischema.*;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.extension.annotation.UISchemaPreset;

@Getter
@Setter
@Schema(
    description =
        "Assentamento completo de licença ou afastamento funcional do servidor por vínculo e período, com workflow de homologação, controle de substituição e atos legais (equivalente a LIC_AFAST no legado Ergon).")
@ResourceIdentity(
    keyField = "id",
    titleField = "motivo",
    displayLabelField = "motivo",
    subtitleField = "pessoaNome",
    metadataFields = {"orgaoNome", "codigoFrequenciaNome"})
public class AfastamentoResponseDTO extends AfastamentoDados {
  @Schema(
      description = "Identificador único surrogate imutável do registro de licença ou afastamento.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Código do afastamento",
      helpText = "Identificador único do assentamento de afastamento.",
      preset = UISchemaPreset.ENTERPRISE_ID,
      group = "Identificação funcional",
      order = 1,
      readOnly = true,
      tableHidden = false)
  private Long id;

  @Schema(
      description = "Código do órgão público ao qual pertence o vínculo funcional afastado.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Código do órgão",
      helpText = "Órgão público proprietário do vínculo afastado.",
      group = "Identificação funcional",
      order = 2,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "apartment",
      endpoint = ApiPaths.ORGAO_OPTIONS,
      valueField = "id",
      displayField = "label",
      readOnly = true,
      tableHidden = true)
  @ResourceReference(
      resourceKey = "administracao-pessoal.empresas",
      valueField = "empCodigo",
      surfaceId = "ficha-institucional",
      previewSurfaceId = "ficha-institucional",
      detailPresentation = "drawer")
  private Integer empCodigo;

  @Schema(
      description = "Denominação oficial do órgão público do vínculo funcional afastado.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Órgão", icon = "apartment",
      helpText = "Instituição pública de lotação do vínculo afastado.",
      group = "Identificação funcional",
      order = 3,
      readOnly = true,
      tableHidden = true,
      formHidden = true)
  @ResourceReference(
      resourceKey = "administracao-pessoal.empresas",
      valueField = "empCodigo",
      surfaceId = "ficha-institucional",
      previewSurfaceId = "ficha-institucional",
      detailPresentation = "drawer")
  private String orgaoNome;

  @Schema(
      description = "Número funcional do servidor afastado (componente 1 da chave do vínculo).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Número funcional",
      helpText = "Identificador funcional da pessoa servidora afastada.",
      group = "Identificação funcional",
      order = 4,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "person",
      endpoint = ApiPaths.PESSOA_OPTIONS,
      valueField = "id",
      displayField = "label",
      readOnly = true,
      tableHidden = true)
  @ResourceReference(
      resourceKey = "administracao-pessoal.funcionarios",
      valueField = "numfunc",
      surfaceId = "ficha-cadastral",
      previewSurfaceId = "ficha-cadastral",
      detailPresentation = "drawer")
  private Long numfunc;

  @Schema(
      description = "Número sequencial do vínculo funcional afastado (componente 2 da chave do vínculo).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Sequencial do vínculo",
      helpText = "Sequencial do vínculo funcional do servidor.",
      group = "Identificação funcional",
      order = 5,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "badge",
      endpoint = ApiPaths.VINCULO_OPTIONS,
      valueField = "id",
      displayField = "label",
      readOnly = true,
      tableHidden = true)
  private Integer numvinc;

  @Schema(
      description = "Identidade composta do vínculo funcional afastado no formato 'numfunc:numvinc'.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Identidade do vínculo",
      helpText = "Chave composta do vínculo funcional titular.",
      group = "Identificação funcional",
      order = 6,
      readOnly = true,
      tableHidden = true,
      formHidden = true)
  private VinculoId vinculoId;

  @Schema(
      description = "Matrícula institucional do vínculo funcional afastado.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Matrícula do vínculo", icon = "badge",
      helpText = "Matrícula funcional do vínculo afastado.",
      group = "Identificação funcional",
      order = 7,
      readOnly = true,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 1)
  @ResourceReference(
      resourceKey = "administracao-pessoal.vinculos",
      valueField = "vinculoId",
      surfaceId = "resumo-funcional",
      previewSurfaceId = "resumo-funcional",
      detailPresentation = "drawer")
  private String vinculoMatricula;

  @Schema(
      description = "Nome civil completo da pessoa servidora titular do afastamento.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Identificação civil do servidor afastado.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @UISchema(
      label = "Servidor afastado",
      helpText = "Nome civil do servidor titular da licença ou afastamento.",
      group = "Identificação funcional",
      order = 8,
      readOnly = true,
      tableHidden = false)
  @ResourceReference(
      resourceKey = "administracao-pessoal.funcionarios",
      valueField = "numfunc",
      surfaceId = "ficha-cadastral",
      previewSurfaceId = "ficha-cadastral",
      detailPresentation = "drawer")
  private String pessoaNome;

  @Schema(
      description = "Denominação oficial do tipo macro de frequência do afastamento.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Natureza da frequência",
      helpText = "Denominação do tipo macro de frequência.",
      group = "Enquadramento e fundamentação",
      order = 21,
      readOnly = true,
      tableHidden = true,
      formHidden = true)
  @ResourceReference(
      resourceKey = "administracao-pessoal.tipos-frequencia",
      valueField = "tipofreq",
      surfaceId = "ficha-tipo-frequencia",
      previewSurfaceId = "ficha-tipo-frequencia",
      detailPresentation = "drawer")
  private String tipoFrequenciaNome;

  @Schema(
      description = "Identidade composta do código de frequência no formato 'tipofreq:codfreq'.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Chave do código de frequência",
      helpText = "Identificador composto no catálogo de códigos de frequência.",
      group = "Enquadramento e fundamentação",
      order = 23,
      readOnly = true,
      tableHidden = true,
      formHidden = true)
  private CodigoFrequenciaId codigoFrequenciaId;

  @Schema(
      description = "Descrição oficial da modalidade de licença ou afastamento no catálogo de códigos.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Modalidade de licença", icon = "event_busy",
      helpText = "Denominação oficial da modalidade de afastamento.",
      group = "Enquadramento e fundamentação",
      order = 24,
      readOnly = true,
      tableHidden = false)
  @ResourceReference(
      resourceKey = "administracao-pessoal.codigos-frequencia",
      valueField = "codigoFrequenciaId",
      surfaceId = "ficha-codigo-frequencia",
      previewSurfaceId = "ficha-codigo-frequencia",
      detailPresentation = "drawer")
  private String codigoFrequenciaNome;

  @Schema(
      description =
          "Data de término efetivo ou retorno antecipado do afastamento, preenchida pelo comando de encerramento.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Término efetivo",
      helpText = "Último dia efetivo do afastamento após homologação ou retorno.",
      group = "Período e vigência",
      order = 45,
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "event_busy",
      readOnly = true,
      tableHidden = true)
  private LocalDate dtfim;

  @Schema(
      description = "Duração apurada em dias corridos do período de licença ou afastamento.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Duração (dias)", numericFormat = org.praxisplatform.uischema.NumericFormat.INTEGER,
      helpText = "Quantidade total de dias corridos apurada para o afastamento.",
      group = "Período e vigência",
      order = 48,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.INPUT,
      icon = "timelapse",
      readOnly = true,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 5)
  @MicroVisualization(
      kind = MicroVisualizationKind.BULLET,
      valueExpr = "quantidadeDias",
      min = 0,
      max = 365,
      total = 365,
      valueSuffix = " d",
      compactValue = true,
      fallbackText = "Duração ainda em aberto",
      ariaLabel = "Duração em dias corridos do afastamento em relação ao ciclo anual",
      thresholds = {
        @Threshold(min = 1, max = 15, tone = "success", label = "Curta duração (até 15d)"),
        @Threshold(min = 15, max = 90, tone = "info", label = "Média duração (16 a 90d)"),
        @Threshold(min = 90, max = 180, tone = "warning", label = "Longa duração (91 a 180d)"),
        @Threshold(min = 180, tone = "danger", label = "Afastamento prolongado (>180d)")
      })
  private Integer quantidadeDias;

  @Schema(
      description =
          "Estado do ciclo de vida e aprovação da licença (PENDENTE_HOMOLOGACAO, HOMOLOGADO, REJEITADO, CANCELADO).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Situação da licença", icon = "flag",
      helpText = "Estado atual de homologação e vigência do afastamento.",
      preset = UISchemaPreset.ENTERPRISE_STATUS,
      group = "Enquadramento e fundamentação",
      order = 28,
      readOnly = true,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 2)
  private String situacao;

  @Schema(
      description = "Nome civil do servidor designado como substituto durante o afastamento.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Identificação civil do servidor substituto.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @UISchema(
      label = "Nome do substituto",
      helpText = "Servidor público designado para substituição no período.",
      group = "Substituição funcional",
      order = 85,
      readOnly = true,
      tableHidden = true)
  @ResourceReference(
      resourceKey = "administracao-pessoal.funcionarios",
      valueField = "numfuncSubstituto",
      surfaceId = "ficha-cadastral",
      previewSurfaceId = "ficha-cadastral",
      detailPresentation = "drawer")
  private String substitutoNome;

  @Schema(
      description = "Número oficial da portaria de concessão ou laudo pericial associado.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Número da portaria/laudo", icon = "description",
      helpText = "Identificação oficial do documento legal de concessão.",
      group = "Atos e publicação oficial",
      order = 105,
      readOnly = true,
      tableHidden = true,
      quickViewVisible = true,
      quickViewOrder = 6)
  @ResourceReference(
      resourceKey = "administracao-pessoal.documentos-legais",
      valueField = "documentoLegalId",
      surfaceId = "ficha-documento",
      previewSurfaceId = "ficha-documento",
      detailPresentation = "drawer")
  private String documentoLegalNumero;

  @Schema(
      description = "Número oficial do ato de publicação em Diário Oficial associado ao afastamento.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Número da publicação",
      helpText = "Identificação oficial do ato de publicação no Diário Oficial.",
      group = "Atos e publicação oficial",
      order = 115,
      readOnly = true,
      tableHidden = true)
  @ResourceReference(
      resourceKey = "administracao-pessoal.documentos-legais",
      valueField = "atoPublicacaoId",
      surfaceId = "ficha-documento",
      previewSurfaceId = "ficha-documento",
      detailPresentation = "drawer")
  private String atoPublicacaoNumero;

  @Schema(
      description =
          "Fato derivado que indica se o afastamento encontra-se homologado e em curso na data atual.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Afastamento em curso",
      helpText = "Verdadeiro quando homologado e ainda não encerrado.",
      group = "Período e vigência",
      order = 49,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "play_circle",
      labelTrue = "Em curso",
      labelFalse = "Inativo / encerrado",
      toneTrue = "info",
      toneFalse = "neutral",
      readOnly = true,
      tableHidden = true)
  private Boolean afastamentoEmCurso;

  @Schema(
      description =
          "Fato derivado que indica se há servidor substituto formalmente designado para o período.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Com substituto designado",
      helpText = "Verdadeiro quando há servidor substituto vinculado.",
      group = "Substituição funcional",
      order = 95,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "how_to_reg",
      readOnly = true,
      tableHidden = true)
  private Boolean comSubstitutoDesignado;

  @Schema(
      description =
          "Fato derivado que indica se o registro aguarda deliberação ou homologação da chefia/junta médica.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Pendente de homologação",
      helpText = "Verdadeiro quando a licença aguarda despacho homologatório.",
      group = "Enquadramento e fundamentação",
      order = 29,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "pending_actions",
      readOnly = true,
      tableHidden = true)
  private Boolean pendenteHomologacao;
  @Override
  @Schema(
      description =
          "Fundamentação administrativa, clínica ou jurídica detalhada do lançamento da licença ou afastamento (MOTIVO).", accessMode = Schema.AccessMode.READ_ONLY)
  @org.praxisplatform.uischema.annotation.ResourceReference(
      resourceKey = "administracao-pessoal.afastamentos",
      valueField = "id",
      surfaceId = "ficha-afastamento",
      previewSurfaceId = "ficha-afastamento",
      detailPresentation = "drawer")
  public String getMotivo() { return super.getMotivo(); }

}
