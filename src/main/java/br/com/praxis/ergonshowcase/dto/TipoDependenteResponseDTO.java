package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.praxisplatform.uischema.*;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.extension.annotation.UISchema;

@Getter
@Setter
@Schema(
    description =
        "Classificação legal de dependência funcional e previdenciária utilizada para parametrização de elegibilidade a IRRF e cota de salário-família (equivalente a TIPO_DEPEN na tela ERGadm00001 do legado Ergon).")
@ResourceIdentity(
    keyField = "id",
    titleField = "descricao",
    displayLabelField = "descricao",
    subtitleField = "id",
    metadataFields = {"documentoLegalNumero"})
public class TipoDependenteResponseDTO {
  @Schema(
      description =
          "Código textual único da modalidade de dependência (chave estável do catálogo TIPO_DEPEN).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Código do tipo",
      helpText = "Identificador alfanumérico estável da modalidade de dependência.",
      group = "Identificação",
      order = 10,
      readOnly = true,
      tableHidden = false)
  private String id;

  @Schema(
      description =
          "Descrição oficial da modalidade de dependência e sua regra estatutária ou tributária de enquadramento.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Descrição da modalidade",
      helpText = "Denominação oficial da regra de dependência apresentada nos cadastros.",
      group = "Identificação",
      order = 20,
      readOnly = true,
      tableHidden = false)
  @ResourceReference(
      resourceKey = "administracao-pessoal.tipos-dependente",
      valueField = "id",
      surfaceId = "ficha-tipo-dependente",
      previewSurfaceId = "ficha-tipo-dependente",
      detailPresentation = "drawer")
  private String descricao;

  @Schema(
      description =
          "Indica se a modalidade habilita dedução legal na base de cálculo do Imposto de Renda Retido na Fonte (IRRF).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Elegível a IRRF",
      helpText = "Define se o tipo de dependência permite dedução tributária mensal de IRRF.",
      group = "Elegibilidade",
      order = 30,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "receipt_long",
      labelTrue = "Deduz IRRF",
      labelFalse = "Não deduz",
      toneTrue = "success",
      toneFalse = "neutral",
      readOnly = true,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 1)
  private Boolean geraIr;

  @Schema(
      description =
          "Indica se a modalidade habilita percepção de cota de salário-família ou benefício previdenciário correlato.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Gera salário-família",
      helpText = "Define se a modalidade confere direito à cota de salário-família.",
      group = "Elegibilidade",
      order = 40,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "family_restroom",
      labelTrue = "Gera cota",
      labelFalse = "Sem cota",
      toneTrue = "info",
      toneFalse = "neutral",
      readOnly = true,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 2)
  private Boolean geraSalarioFamilia;

  @Schema(
      description =
          "Idade limite padrão em anos completos para manutenção automática da dependência sem laudo de invalidez (ex.: 21 ou 24 anos).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Idade limite (anos)", numericFormat = org.praxisplatform.uischema.NumericFormat.INTEGER,
      helpText = "Teto etário padrão em anos para vigência sem invalidez permanente.",
      group = "Elegibilidade",
      order = 50,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.INPUT,
      icon = "cake",
      readOnly = true,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 3)
  private Integer idadeLimite;

  @Schema(
      description =
          "Referência ao ato normativo ou diploma legal que fundamenta a modalidade de dependência (substitui PONTLEI).",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Fundamento legal",
      helpText = "Diploma legal ou ato normativo que regulamenta esta modalidade.",
      group = "Normativo",
      order = 60,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "description",
      endpoint = ApiPaths.DOCUMENTO_OPTIONS,
      valueField = "id",
      displayField = "label",
      readOnly = true,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 4)
  @ResourceReference(
      resourceKey = "administracao-pessoal.documentos-legais",
      valueField = "documentoLegalId",
      surfaceId = "ficha-documento",
      previewSurfaceId = "ficha-documento",
      detailPresentation = "drawer")
  private Long documentoLegalId;

  @Schema(
      description =
          "Número oficial do ato ou diploma legal associado à modalidade de dependência.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Número do ato legal", icon = "description",
      helpText = "Identificação oficial do ato normativo vinculado.",
      group = "Normativo",
      order = 65,
      readOnly = true,
      tableHidden = false)
  @ResourceReference(
      resourceKey = "administracao-pessoal.documentos-legais",
      valueField = "documentoLegalId",
      surfaceId = "ficha-documento",
      previewSurfaceId = "ficha-documento",
      detailPresentation = "drawer")
  private String documentoLegalNumero;
}
