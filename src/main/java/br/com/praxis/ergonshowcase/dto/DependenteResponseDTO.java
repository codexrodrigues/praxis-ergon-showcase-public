package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.domain.DependenteId;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.praxisplatform.uischema.*;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.extension.annotation.UISchema;

@Getter
@Setter
@Schema(
    description =
        "Assentamento completo do dependente vinculado ao servidor público, expondo elegibilidade tributária e previdenciária, registro civil e vigência (equivalente a DEPENDENTES na tela ERGadm00217 do legado Ergon).")
@ResourceIdentity(
    keyField = "id",
    titleField = "nome",
    displayLabelField = "nome",
    subtitleField = "pessoaNome",
    metadataFields = {"dtini", "dependenteVigente"})
public class DependenteResponseDTO extends DependenteDados {
  @Schema(
      description =
          "Identificador composto natural do dependente no formato canônico 'numfunc:numdep'.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Identificador do dependente",
      group = "Identificação do vínculo familiar",
      order = 1,
      readOnly = true,
      tableHidden = false)
  private DependenteId id;

  @Schema(
      description = "Número funcional do servidor público titular ao qual o dependente está vinculado.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Servidor titular",
      group = "Identificação do vínculo familiar",
      order = 2,
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
      description = "Sequencial numérico do dependente no prontuário funcional do servidor.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Sequencial do dependente",
      group = "Identificação do vínculo familiar",
      order = 3,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.INPUT,
      icon = "format_list_numbered",
      readOnly = true,
      tableHidden = true)
  private Integer numdep;

  @Schema(
      description = "Nome civil completo do servidor público titular da relação de dependência.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL,
      reason = "Identificação civil do servidor titular.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @UISchema(
      label = "Nome do servidor",
      group = "Identificação do vínculo familiar",
      order = 4,
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
      description =
          "Idade atual do dependente em anos completos, confrontada com as faixas de elegibilidade legal (18, 21 e 24 anos).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Idade (anos)",
      group = "Identificação civil",
      order = 45,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.INPUT,
      icon = "cake",
      readOnly = true,
      tableHidden = false)
  @MicroVisualization(
      kind = MicroVisualizationKind.RADIAL,
      valueExpr = "idadeAnos",
      min = 0,
      max = 24,
      compactValue = true,
      fallbackText = "Sem idade apurada",
      ariaLabel = "Idade em relação ao limite de dependência",
      thresholds = {
        @Threshold(min = 0, max = 18, tone = "success", label = "Menor"),
        @Threshold(min = 18, max = 21, tone = "info", label = "Até 21 anos"),
        @Threshold(min = 21, max = 24, tone = "warning", label = "Universitário"),
        @Threshold(min = 24, tone = "neutral", label = "Acima do limite")
      })
  private Long idadeAnos;

  @Schema(
      description =
          "Descrição oficial da modalidade de dependência vinculada ao código tipodepen.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Modalidade de dependência",
      group = "Enquadramento da dependência",
      order = 62,
      readOnly = true,
      tableHidden = false)
  @ResourceReference(
      resourceKey = "administracao-pessoal.tipos-dependente",
      valueField = "tipodepen",
      surfaceId = "ficha-tipo-dependente",
      previewSurfaceId = "ficha-tipo-dependente",
      detailPresentation = "drawer")
  private String tipoDependenteDescricao;

  @Schema(
      description = "Data de início da vigência da condição de dependência.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Início da vigência",
      group = "Vigência da dependência",
      order = 65,
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "event_available",
      readOnly = true,
      tableHidden = false)
  private LocalDate dtini;

  @Schema(
      description =
          "Data de encerramento da condição de dependência por maioridade, emancipação, divórcio ou óbito.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Fim da vigência",
      group = "Vigência da dependência",
      order = 66,
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "event_busy",
      readOnly = true,
      tableHidden = true)
  private LocalDate dtfim;

  @Schema(
      description = "Indica se a condição de dependência encontra-se vigente na data atual.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Dependência vigente",
      group = "Vigência da dependência",
      order = 67,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "verified",
      labelTrue = "Vigente",
      labelFalse = "Encerrada",
      toneTrue = "success",
      toneFalse = "neutral",
      readOnly = true,
      tableHidden = true)
  private Boolean dependenteVigente;

  @Schema(
      description =
          "Fato derivado que confirma elegibilidade ativa para dedução de IRRF considerando vigência, indicador depIr e limite etário.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Elegível a IRRF ativo",
      group = "Elegibilidade e benefícios",
      order = 135,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "verified_user",
      readOnly = true,
      tableHidden = true)
  private Boolean elegivelIrrf;

  @Schema(
      description = "Número oficial do ato ou documento comprobatório associado ao dependente.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Número do documento legal",
      group = "Registro civil",
      order = 295,
      readOnly = true,
      tableHidden = true)
  @ResourceReference(
      resourceKey = "administracao-pessoal.documentos-legais",
      valueField = "documentoLegalId",
      surfaceId = "ficha-documento",
      previewSurfaceId = "ficha-documento",
      detailPresentation = "drawer")
  private String documentoLegalNumero;
  @Override
  @Schema(
      description =
          "Nome civil completo do dependente registrado no assentamento funcional do servidor.", accessMode = Schema.AccessMode.READ_ONLY)
  @org.praxisplatform.uischema.annotation.ResourceReference(
      resourceKey = "administracao-pessoal.dependentes",
      valueField = "id",
      surfaceId = "ficha-dependente",
      previewSurfaceId = "ficha-dependente",
      detailPresentation = "drawer")
  public String getNome() { return super.getNome(); }

}
