package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.domain.VinculoId;
import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.customfields.CustomFields;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.extension.annotation.UISchemaPreset;

@Getter
@Setter
@ResourceIdentity(keyField = "id", titleField = "descricao", subtitleField = "titularNome",
    metadataFields = {"empCodigo", "dataReferencia"})
@Schema(description = "Posição de cobertura da chefia na data de referência. Distingue titular em exercício, impedimento coberto e chefia descoberta.")
public class RadarChefiaSubstituicaoResponseDTO {
  @Schema(description = "Identidade estável usada para abrir a ficha deste registro.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Identidade", helpText = "Identificador usado na abertura da ficha.", type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "tag", preset = UISchemaPreset.ENTERPRISE_ID,
      tableHidden = true, formHidden = true, readOnly = true)
  private Long id;

  @Schema(description = "Órgão responsável pelo cadastro e pela gestão funcional deste registro.", accessMode = Schema.AccessMode.READ_ONLY)
  @ResourceReference(resourceKey = "administracao-pessoal.empresas", valueField = "empCodigo",
      surfaceId = "ficha-institucional", previewSurfaceId = "ficha-institucional", detailPresentation = "drawer")
  @DomainGovernance(kind = DomainGovernanceKind.SECURITY,
      classification = DomainClassification.INTERNAL,
      dataCategory = DomainDataCategory.OPERATIONAL)
  @UISchema(label = "Órgão",
      helpText = "Selecione o órgão ao qual pertence o registro.",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "apartment",
      preset = UISchemaPreset.TENANT_LABEL,
      endpoint = ApiPaths.ORGAO_OPTIONS,
      valueField = "id",
      displayField = "label",
      readOnly = true,
      editable = false,
      group = "Identificação e situação",
      order = 10,
      tableHidden = true)
  private Integer empCodigo;

  @Schema(description = "Identificação da designação de chefia acompanhada no radar.", accessMode = Schema.AccessMode.READ_ONLY)
  @ResourceReference(resourceKey = ApiPaths.KEY_RADAR_CHEFIAS_SUBSTITUICOES, valueField = "id", surfaceId = "detail",
      previewSurfaceId = "detail", detailPresentation = "drawer")
  @DomainGovernance(kind = DomainGovernanceKind.SECURITY,
      classification = DomainClassification.INTERNAL,
      dataCategory = DomainDataCategory.OPERATIONAL)
  @UISchema(label = "Descrição",
      helpText = "Use uma identificação curta para localizar este registro.",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.INPUT,
      icon = "notes",
      preset = UISchemaPreset.NONE,
      readOnly = true,
      editable = false,
      group = "Identificação e situação",
      order = 20,
      tableHidden = false)
  private String descricao;

  @Schema(description = "Primeiro dia de exercício da designação titular.", accessMode = Schema.AccessMode.READ_ONLY)
  @DomainGovernance(kind = DomainGovernanceKind.SECURITY,
      classification = DomainClassification.INTERNAL,
      dataCategory = DomainDataCategory.OPERATIONAL)
  @UISchema(label = "Início da vigência",
      helpText = "Use a data inicial que consta no ato.",
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "event",
      preset = UISchemaPreset.DATE_RANGE,
      readOnly = true,
      editable = false,
      group = "Vigência",
      order = 30,
      tableHidden = true)
  private LocalDate dtini;

  @Schema(description = "Último dia abrangido pelo ato da chefia, inclusive, quando informado.", accessMode = Schema.AccessMode.READ_ONLY)
  @DomainGovernance(kind = DomainGovernanceKind.SECURITY,
      classification = DomainClassification.INTERNAL,
      dataCategory = DomainDataCategory.OPERATIONAL)
  @UISchema(label = "Fim da vigência",
      helpText = "Deixe em branco enquanto a vigência estiver aberta.",
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "event",
      preset = UISchemaPreset.DATE_RANGE,
      readOnly = true,
      editable = false,
      group = "Vigência",
      order = 40,
      tableHidden = true,
      quickViewVisible = true,
      quickViewOrder = 6)
  private LocalDate dtfim;

  @Schema(description = "Posição de cobertura na data de referência, obtida do impedimento do titular e dos exercícios interinos vigentes.", accessMode = Schema.AccessMode.READ_ONLY, allowableValues = {"TITULAR_EM_EXERCICIO", "COBERTA", "DESCOBERTA"})
  @DomainGovernance(kind = DomainGovernanceKind.SECURITY,
      classification = DomainClassification.INTERNAL,
      dataCategory = DomainDataCategory.OPERATIONAL)
  @UISchema(label = "Situação",
      helpText = "Selecione a posição cadastral documentada.",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.SELECT,
      icon = "flag",
      preset = UISchemaPreset.ENTERPRISE_STATUS,
      options = "[{\"value\":\"TITULAR_EM_EXERCICIO\",\"label\":\"Titular em exercício\"},{\"value\":\"COBERTA\",\"label\":\"Coberta\"},{\"value\":\"DESCOBERTA\",\"label\":\"Descoberta\"}]",
      readOnly = true,
      editable = false,
      group = "Identificação e situação",
      order = 50,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 3)
  private String situacao;

  @Schema(description = "Função de chefia ou direção exercida na designação acompanhada.", accessMode = Schema.AccessMode.READ_ONLY)
  @ResourceReference(resourceKey = ApiPaths.KEY_FUNCOES, valueField = "funcaoId",
      surfaceId = "detail", previewSurfaceId = "detail", detailPresentation = "drawer")
  @DomainGovernance(kind = DomainGovernanceKind.SECURITY,
      classification = DomainClassification.INTERNAL,
      dataCategory = DomainDataCategory.OPERATIONAL)
  @UISchema(label = "Função monitorada",
      helpText = "Abra o cadastro da função.",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "link",
      preset = UISchemaPreset.ENTERPRISE_ID,
      endpoint = ApiPaths.FUNCAO_OPTIONS,
      valueField = "id",
      displayField = "label",
      readOnly = true,
      editable = false,
      group = "Vínculos e unidades",
      order = 60,
      tableHidden = true,
      quickViewVisible = true,
      quickViewOrder = 1)
  private Long funcaoId;

  @Schema(description = "Setor pelo qual responde a chefia acompanhada.", accessMode = Schema.AccessMode.READ_ONLY)
  @ResourceReference(resourceKey = "administracao-pessoal.setores", valueField = "setorId",
      surfaceId = "ficha-unidade", previewSurfaceId = "ficha-unidade", detailPresentation = "drawer")
  @DomainGovernance(kind = DomainGovernanceKind.SECURITY,
      classification = DomainClassification.INTERNAL,
      dataCategory = DomainDataCategory.OPERATIONAL)
  @UISchema(label = "Unidade monitorada",
      helpText = "Abra a ficha da unidade.",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "link",
      preset = UISchemaPreset.ENTERPRISE_NAME,
      endpoint = ApiPaths.SETOR_OPTIONS,
      valueField = "id",
      displayField = "label",
      readOnly = true,
      editable = false,
      group = "Vínculos e unidades",
      order = 70,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 2)
  private String setorId;

  @Schema(description = "Ato em exercício usado para identificar a chefia na data de referência.", accessMode = Schema.AccessMode.READ_ONLY)
  @ResourceReference(resourceKey = ApiPaths.KEY_DESIGNACOES, valueField = "designacaoId",
      surfaceId = "detail", previewSurfaceId = "detail", detailPresentation = "drawer")
  @DomainGovernance(kind = DomainGovernanceKind.SECURITY,
      classification = DomainClassification.INTERNAL,
      dataCategory = DomainDataCategory.OPERATIONAL)
  @UISchema(label = "Designação titular",
      helpText = "Abra a designação de origem.",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "link",
      preset = UISchemaPreset.ENTERPRISE_ID,
      endpoint = ApiPaths.DESIGNACAO_OPTIONS,
      valueField = "id",
      displayField = "label",
      readOnly = true,
      editable = false,
      group = "Vínculos e unidades",
      order = 80,
      tableHidden = true)
  private Long designacaoId;

  @Schema(description = "Posição numerada vinculada à designação monitorada.", accessMode = Schema.AccessMode.READ_ONLY)
  @ResourceReference(resourceKey = ApiPaths.KEY_VAGAS_FUNCAO, valueField = "vagaFuncaoId",
      surfaceId = "detail", previewSurfaceId = "detail", detailPresentation = "drawer")
  @DomainGovernance(kind = DomainGovernanceKind.SECURITY,
      classification = DomainClassification.INTERNAL,
      dataCategory = DomainDataCategory.OPERATIONAL)
  @UISchema(label = "Vaga da chefia",
      helpText = "Abra a vaga registrada no ato.",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "link",
      preset = UISchemaPreset.ENTERPRISE_ID,
      endpoint = ApiPaths.VAGA_FUNCAO_OPTIONS,
      valueField = "id",
      displayField = "label",
      readOnly = true,
      editable = false,
      group = "Vínculos e unidades",
      order = 90,
      tableHidden = true)
  private Long vagaFuncaoId;

  @Schema(description = "Vínculo que detém a titularidade da função na data de referência.", accessMode = Schema.AccessMode.READ_ONLY)
  @ResourceReference(resourceKey = "administracao-pessoal.vinculos", valueField = "titularId",
      surfaceId = "resumo-funcional", previewSurfaceId = "resumo-funcional", detailPresentation = "drawer")
  @DomainGovernance(kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL, exportable = false)
  @UISchema(label = "Vínculo do titular",
      helpText = "Abra o resumo funcional do titular.",
      type = FieldDataType.TEXT,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "link",
      preset = UISchemaPreset.ENTERPRISE_ID,
      endpoint = ApiPaths.VINCULO_OPTIONS,
      valueField = "id",
      displayField = "label",
      readOnly = true,
      editable = false,
      group = "Vínculos e unidades",
      order = 100,
      tableHidden = true)
  private VinculoId titularId;

  @Schema(description = "Exercício interino vigente na data de referência. Quando há mais de um, exibe o que começou mais recentemente.", accessMode = Schema.AccessMode.READ_ONLY)
  @ResourceReference(resourceKey = ApiPaths.KEY_SUBSTITUICOES, valueField = "substituicaoId",
      surfaceId = "detail", previewSurfaceId = "detail", detailPresentation = "drawer")
  @DomainGovernance(kind = DomainGovernanceKind.SECURITY,
      classification = DomainClassification.INTERNAL,
      dataCategory = DomainDataCategory.OPERATIONAL)
  @UISchema(label = "Substituição vigente",
      helpText = "Abra o exercício interino apresentado.",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "link",
      preset = UISchemaPreset.ENTERPRISE_ID,
      endpoint = ApiPaths.SUBSTITUICAO_OPTIONS,
      valueField = "id",
      displayField = "label",
      readOnly = true,
      editable = false,
      group = "Vínculos e unidades",
      order = 110,
      tableHidden = true,
      quickViewVisible = true,
      quickViewOrder = 7)
  private Long substituicaoId;

  @Schema(description = "Afastamento vigente do vínculo titular na data de referência, com precedência para o mais recente.", accessMode = Schema.AccessMode.READ_ONLY)
  @ResourceReference(resourceKey = "administracao-pessoal.afastamentos", valueField = "afastamentoId",
      surfaceId = "ficha-afastamento", previewSurfaceId = "ficha-afastamento", detailPresentation = "drawer")
  @DomainGovernance(kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL, exportable = false)
  @UISchema(label = "Afastamento vigente",
      helpText = "Abra o registro de impedimento.",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "link",
      preset = UISchemaPreset.ENTERPRISE_ID,
      endpoint = ApiPaths.AFASTAMENTO_OPTIONS,
      valueField = "id",
      displayField = "label",
      readOnly = true,
      editable = false,
      group = "Vínculos e unidades",
      order = 120,
      tableHidden = true)
  private Long afastamentoId;

  @Schema(description = "Parcela de férias que cobre a data de referência para o vínculo titular.", accessMode = Schema.AccessMode.READ_ONLY)
  @ResourceReference(resourceKey = "administracao-pessoal.ferias-gozo", valueField = "feriasGozoId",
      surfaceId = "ficha-ferias-gozo", previewSurfaceId = "ficha-ferias-gozo", detailPresentation = "drawer")
  @DomainGovernance(kind = DomainGovernanceKind.SECURITY,
      classification = DomainClassification.INTERNAL,
      dataCategory = DomainDataCategory.OPERATIONAL)
  @UISchema(label = "Férias vigentes",
      helpText = "Abra a parcela de gozo de origem.",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.ENTITY_LOOKUP,
      icon = "link",
      preset = UISchemaPreset.ENTERPRISE_ID,
      endpoint = ApiPaths.FERIAS_GOZO_OPTIONS,
      valueField = "id",
      displayField = "label",
      readOnly = true,
      editable = false,
      group = "Vínculos e unidades",
      order = 130,
      tableHidden = true)
  private Long feriasGozoId;

  @Schema(description = "Data civil usada para avaliar vigência e impedimentos no radar.", accessMode = Schema.AccessMode.READ_ONLY)
  @DomainGovernance(kind = DomainGovernanceKind.SECURITY,
      classification = DomainClassification.INTERNAL,
      dataCategory = DomainDataCategory.OPERATIONAL)
  @UISchema(label = "Data de referência",
      helpText = "Confira a data da posição exibida.",
      type = FieldDataType.DATE,
      controlType = FieldControlType.DATE_PICKER,
      icon = "event",
      preset = UISchemaPreset.DATE_RANGE,
      readOnly = true,
      editable = false,
      group = "Vigência",
      order = 140,
      tableHidden = false)
  private LocalDate dataReferencia;

  @Schema(description = "Número de exercícios interinos registrados como vigentes na data de referência para a mesma designação.", accessMode = Schema.AccessMode.READ_ONLY)
  @DomainGovernance(kind = DomainGovernanceKind.SECURITY,
      classification = DomainClassification.INTERNAL,
      dataCategory = DomainDataCategory.OPERATIONAL)
  @MicroVisualization(kind = MicroVisualizationKind.BULLET, valueExpr = "=qtdCoberturas",
      targetExpr = "=1", ariaLabel = "Substituições vigentes", fallbackText = "Exercícios interinos", thresholds = {
          @Threshold(max = 0, tone = "warning", label = "Nenhuma"),
          @Threshold(min = 1, tone = "info", label = "Registrada")})
  @UISchema(label = "Substituições vigentes",
      helpText = "Verifique sobreposição quando houver mais de um exercício.",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "tag",
      preset = UISchemaPreset.NONE,
      readOnly = true,
      editable = false,
      group = "Identificação e situação",
      order = 150,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 4)
  private Integer qtdCoberturas;

  @Schema(description = "Quantidade de substituições da designação com apuração aguardando processamento em folha.", accessMode = Schema.AccessMode.READ_ONLY)
  @DomainGovernance(kind = DomainGovernanceKind.SECURITY,
      classification = DomainClassification.INTERNAL,
      dataCategory = DomainDataCategory.OPERATIONAL)
  @MicroVisualization(kind = MicroVisualizationKind.BULLET, valueExpr = "=pendenciasFolha",
      targetExpr = "=1", ariaLabel = "Pendências de folha", fallbackText = "Apurações pendentes", thresholds = {
          @Threshold(max = 0, tone = "neutral", label = "Nenhuma"),
          @Threshold(min = 1, tone = "warning", label = "Registrada")})
  @UISchema(label = "Pendências de folha",
      helpText = "Priorize o tratamento dos registros pendentes.",
      type = FieldDataType.NUMBER,
      controlType = FieldControlType.NUMERIC_TEXT_BOX,
      icon = "tag",
      preset = UISchemaPreset.NONE,
      readOnly = true,
      editable = false,
      group = "Remuneração",
      order = 160,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 5)
  private Integer pendenciasFolha;

  @Schema(description = "Atributos institucionais da designação que origina a posição de cobertura.", accessMode = Schema.AccessMode.READ_ONLY)
  @DomainGovernance(kind = DomainGovernanceKind.SECURITY,
      classification = DomainClassification.INTERNAL,
      dataCategory = DomainDataCategory.OPERATIONAL)
  @CustomFields(resourceKey = ApiPaths.KEY_DESIGNACOES)
  @UISchema(label = "Campos customizados",
      helpText = "Consulte as extensões da designação de origem.",
      type = FieldDataType.JSON,
      controlType = FieldControlType.INPUT,
      icon = "extension",
      preset = UISchemaPreset.NONE,
      readOnly = true,
      editable = false,
      group = "Extensões institucionais",
      order = 170,
      tableHidden = true)
  private Map<String, Object> customFields;

  @Schema(description = "Nome civil de quem detém a titularidade na designação monitorada.", accessMode = Schema.AccessMode.READ_ONLY)
  @ResourceReference(resourceKey = "administracao-pessoal.funcionarios", valueField = "titularPessoaId",
      surfaceId = "ficha-cadastral", previewSurfaceId = "ficha-cadastral", detailPresentation = "drawer")
  @DomainGovernance(kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL,
      dataCategory = DomainDataCategory.PERSONAL, exportable = false)
  @UISchema(label = "Nome do titular", helpText = "Abra o resumo funcional do titular.", group = "Identificação e situação",
      type = FieldDataType.TEXT, controlType = FieldControlType.INPUT, icon = "person",
      preset = UISchemaPreset.ENTERPRISE_NAME, tableHidden = false, readOnly = true, editable = false, formHidden = true)
  private String titularNome;

  @Schema(description = "Número funcional da pessoa identificada no vínculo de titular.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @DomainGovernance(kind = DomainGovernanceKind.PRIVACY,
      classification = DomainClassification.CONFIDENTIAL, dataCategory = DomainDataCategory.PERSONAL, exportable = false)
  @UISchema(label = "Número funcional", helpText = "Identifica a pessoa na ficha cadastral.",
      type = FieldDataType.NUMBER, controlType = FieldControlType.NUMERIC_TEXT_BOX, icon = "badge",
      preset = UISchemaPreset.ENTERPRISE_ID, tableHidden = true, formHidden = true, readOnly = true)
  private Long titularPessoaId;

}
