package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.*;
import lombok.Getter;
import lombok.Setter;
import org.praxisplatform.uischema.*;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.bulk.BulkMode;
import org.praxisplatform.uischema.customfields.CustomFields;
import org.praxisplatform.uischema.extension.annotation.UISchema;

/** Dados cadastrais e operacionais de licenças e afastamentos compartilhados entre leitura e escrita. */
@Getter
@Setter
public abstract class AfastamentoDados {
  @NotBlank
  @Size(max = 20)
  @Schema(
      description =
          "Tipo macro de frequência associado à modalidade de licença ou afastamento (ex.: LICENCA, AFASTAMENTO).")
  @UISchema(
      label = "Tipo de frequência",
      helpText = "Natureza macro da licença no catálogo de frequência.",
      group = "Enquadramento e fundamentação",
      order = 20,
      type = FieldDataType.TEXT,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "category",
      endpoint = ApiPaths.TIPO_FREQUENCIA_OPTIONS,
      valueField = "id",
      displayField = "label",
      tableHidden = true)
  @ResourceReference(
      resourceKey = "administracao-pessoal.tipos-frequencia",
      valueField = "tipofreq",
      surfaceId = "ficha-tipo-frequencia",
      previewSurfaceId = "ficha-tipo-frequencia",
      detailPresentation = "drawer")
  private String tipofreq;

  @NotNull
  @PositiveOrZero
  @Schema(
      description =
          "Código numérico específico da licença ou afastamento habilitado no catálogo de códigos de frequência.")
  @UISchema(
      label = "Código da licença",
      helpText = "Código numérico da modalidade de afastamento no catálogo.",
      group = "Enquadramento e fundamentação",
      order = 22,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "tag",
      endpoint = ApiPaths.CODIGO_FREQUENCIA_OPTIONS,
      valueField = "id",
      displayField = "label",
      tableHidden = true)
  private Integer codfreq;

  @NotNull
  @Schema(
      description =
          "Data de início efetivo do período de licença ou afastamento; não pode anteceder o início de exercício do vínculo.")
  @UISchema(
      label = "Início do afastamento",
      helpText = "Primeiro dia efetivo do período de afastamento.",
      group = "Período e vigência",
      order = 30,
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "event_available",
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 3)
  private LocalDate dtini;

  @Schema(
      nullable = true,
      description =
          "Data originalmente prevista para o término da licença ou afastamento (DTPREVFIM); não pode anteceder a data de início.")
  @UISchema(
      label = "Término previsto",
      helpText = "Data estimada para encerramento da licença ou retorno ao exercício.",
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      group = "Período e vigência",
      order = 40,
      icon = "event",
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 4)
  @BulkEditable(modes = {BulkMode.UNIFORM_UPDATE, BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  private LocalDate dtprevfim;

  @NotBlank
  @Size(max = 4000)
  @Schema(
      description =
          "Fundamentação administrativa, clínica ou jurídica detalhada do lançamento da licença ou afastamento (MOTIVO).")
  @DomainGovernance(
      kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.RESTRICTED,
      dataCategory = DomainDataCategory.SENSITIVE_PERSONAL,
      reason = "Fundamentação de licença pode conter diagnóstico clínico ou laudo pericial.",
      aiUsage =
          @AiUsagePolicy(visibility = AiVisibilityMode.MASK, trainingUse = AiTrainingUseMode.DENY))
  @UISchema(
      label = "Fundamentação do afastamento",
      helpText = "Síntese administrativa ou clínica que motiva a concessão da licença.",
      group = "Enquadramento e fundamentação",
      order = 50,
      icon = "notes",
      tableHidden = false)
  @ResourceReference(
      resourceKey = "administracao-pessoal.afastamentos",
      valueField = "id",
      surfaceId = "ficha-afastamento",
      previewSurfaceId = "ficha-afastamento",
      detailPresentation = "drawer")
  @BulkEditable(modes = {BulkMode.UNIFORM_UPDATE, BulkMode.PER_ITEM_UPDATE}, allowClear = false)
  private String motivo;

  @NotNull
  @Schema(
      description =
          "Indica se este lançamento representa prorrogação contínua de licença imediatamente anterior do mesmo vínculo.")
  @UISchema(
      label = "É prorrogação",
      helpText = "Marque quando a licença estender afastamento imediatamente anterior.",
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      group = "Prorrogação",
      order = 60,
      icon = "update",
      labelTrue = "Prorrogação",
      labelFalse = "Concessão inicial",
      toneTrue = "info",
      toneFalse = "neutral",
      tableHidden = true)
  @BulkEditable(modes = {BulkMode.UNIFORM_UPDATE, BulkMode.PER_ITEM_UPDATE}, allowClear = false)
  private Boolean prorrogacao = false;

  @Positive
  @Schema(
      nullable = true,
      description =
          "Referência ao identificador do afastamento original quando o registro atual configura prorrogação.")
  @UISchema(
      label = "Afastamento de origem",
      helpText = "Licença anterior prorrogada por este lançamento.",
      group = "Prorrogação",
      order = 70,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "history",
      endpoint = ApiPaths.AFASTAMENTO_OPTIONS,
      valueField = "id",
      displayField = "label",
      tableHidden = true)
  @ResourceReference(
      resourceKey = "administracao-pessoal.afastamentos",
      valueField = "afastamentoOrigemId",
      surfaceId = "ficha-afastamento",
      previewSurfaceId = "ficha-afastamento",
      detailPresentation = "drawer")
  @BulkEditable(modes = {BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  private Long afastamentoOrigemId;

  @Positive
  @Schema(
      nullable = true,
      description =
          "Número funcional do servidor público designado para substituição durante o período de afastamento.")
  @UISchema(
      label = "Servidor substituto",
      helpText = "Servidor designado para responder pelo posto durante a licença.",
      group = "Substituição funcional",
      order = 80,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "person_add",
      endpoint = ApiPaths.PESSOA_OPTIONS,
      valueField = "id",
      displayField = "label",
      tableHidden = true)
  @ResourceReference(
      resourceKey = "administracao-pessoal.funcionarios",
      valueField = "numfuncSubstituto",
      surfaceId = "ficha-cadastral",
      previewSurfaceId = "ficha-cadastral",
      detailPresentation = "drawer")
  @BulkEditable(modes = {BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  private Long numfuncSubstituto;

  @Positive
  @Schema(
      nullable = true,
      description =
          "Número sequencial do vínculo funcional do servidor substituto designado para cobertura.")
  @UISchema(
      label = "Vínculo do substituto",
      helpText = "Sequencial do vínculo ativo do servidor substituto.",
      group = "Substituição funcional",
      order = 90,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.INPUT,
      icon = "badge",
      tableHidden = true)
  @BulkEditable(modes = {BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  private Integer numvincSubstituto;

  @Positive
  @Schema(
      nullable = true,
      description =
          "Referência à portaria de concessão, despacho decisório ou laudo pericial oficial (substitui PONTLEI/RESULTPRONT).")
  @UISchema(
      label = "Portaria ou laudo legal",
      helpText = "Ato concessório ou laudo pericial que ampara o afastamento.",
      group = "Atos e publicação oficial",
      order = 100,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "description",
      endpoint = ApiPaths.DOCUMENTO_OPTIONS,
      valueField = "id",
      displayField = "label",
      tableHidden = true,
      quickViewVisible = true,
      quickViewOrder = 6)
  @ResourceReference(
      resourceKey = "administracao-pessoal.documentos-legais",
      valueField = "documentoLegalId",
      surfaceId = "ficha-documento",
      previewSurfaceId = "ficha-documento",
      detailPresentation = "drawer")
  @BulkEditable(modes = {BulkMode.UNIFORM_UPDATE, BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  private Long documentoLegalId;

  @Positive
  @Schema(
      nullable = true,
      description =
          "Referência ao ato de publicação oficial do afastamento em Diário Oficial ou boletim interno (substitui PONTPUBL/PONTPROC).")
  @UISchema(
      label = "Ato de publicação oficial",
      helpText = "Publicação no Diário Oficial vinculada à concessão ou homologação.",
      group = "Atos e publicação oficial",
      order = 110,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "newspaper",
      endpoint = ApiPaths.DOCUMENTO_OPTIONS,
      valueField = "id",
      displayField = "label",
      tableHidden = true)
  @ResourceReference(
      resourceKey = "administracao-pessoal.documentos-legais",
      valueField = "atoPublicacaoId",
      surfaceId = "ficha-documento",
      previewSurfaceId = "ficha-documento",
      detailPresentation = "drawer")
  @BulkEditable(modes = {BulkMode.UNIFORM_UPDATE, BulkMode.PER_ITEM_UPDATE}, allowClear = true)
  private Long atoPublicacaoId;

  @NotNull
  @Schema(
      description =
          "Atributos de negócio customizados por órgão em formato JSONB governado (ex.: cid_atestado, numero_laudo_pericial, processo_sei_licenca).")
  @UISchema(
      label = "Campos complementares",
      helpText = "Metadados clínicos, periciais ou processuais específicos do órgão.",
      group = "Extensões do órgão",
      order = 900,
      tableHidden = true)
  @CustomFields(resourceKey = "administracao-pessoal.afastamentos")
  private Map<String, Object> customFields = new LinkedHashMap<>();

  @Null
  @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
  @Schema(
      description =
          "Descrição oficial da modalidade de licença projetada em rascunho a partir do código de frequência; não pode ser enviada pelo cliente.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Modalidade prevista",
      helpText = "Denominação do código de frequência selecionada no rascunho.",
      group = "Enquadramento e fundamentação",
      order = 26,
      readOnly = true,
      tableHidden = true)
  private String codigoFrequenciaNomePrevisto;

  @Null
  @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
  @Schema(
      description =
          "Quantidade prevista de dias corridos entre o início e o término estimado em rascunho; não pode ser enviada pelo cliente.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Duração prevista (dias)",
      helpText = "Contagem preliminar de dias corridos do período informado.",
      group = "Período e vigência",
      order = 41,
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.INPUT,
      icon = "timelapse",
      readOnly = true,
      tableHidden = true)
  private Integer quantidadeDiasPrevista;

  @Null
  @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
  @Schema(
      description =
          "Indicador preliminar se a modalidade selecionada implica ônus financeiro ou desconto remuneratório; não pode ser enviado pelo cliente.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Implica ônus (previsto)",
      helpText = "Sinaliza reflexo financeiro conforme o código de frequência escolhido.",
      group = "Enquadramento e fundamentação",
      order = 27,
      type = FieldDataType.BOOLEAN,
      controlType = FieldControlType.CHECKBOX,
      icon = "payments",
      readOnly = true,
      tableHidden = true)
  private Boolean comOnusPrevisto;
}
