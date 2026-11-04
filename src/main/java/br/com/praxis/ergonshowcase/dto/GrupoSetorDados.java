package br.com.praxis.ergonshowcase.dto;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import org.praxisplatform.uischema.*;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.extension.annotation.*;

@Getter @Setter
@Schema(description = "Grupos de setores: dados do período organizacional e sua fundamentação.")
public class GrupoSetorDados {
  @Schema(description = "Órgão responsável pelo assentamento; o contexto autenticado limita as consultas independentemente deste valor.")
  @ResourceReference(resourceKey = "administracao-pessoal.empresas", valueField = "empCodigo", surfaceId = "ficha-institucional", previewSurfaceId = "ficha-institucional", detailPresentation = "drawer")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(helpText="O órgão da sessão limita as referências disponíveis.", visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Órgão", group = "Vigência e fundamento", tableHidden = false, readOnly = false, quickViewVisible = true, quickViewOrder = 1, type = FieldDataType.NUMBER, controlType = FieldControlType.ENTITY_LOOKUP, icon = "account_tree", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.ORGAO_OPTIONS, valueField = "id", displayField = "label")
  @jakarta.validation.constraints.NotNull
  private Integer empCodigo;

  @Schema(description = "Primeiro dia em que esta configuração organizacional produz efeitos; a data pertence ao intervalo consultado.")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.LEGAL)
  @UISchema(helpText="Informe o primeiro dia de efeitos do período.", visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Início da vigência", group = "Vigência e fundamento", tableHidden = false, readOnly = false, quickViewVisible = true, quickViewOrder = 2, type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, icon = "event", preset = UISchemaPreset.DATE_RANGE)
  @jakarta.validation.constraints.NotNull
  private LocalDate dtini;

  @Schema(description = "Último dia de validade, inclusive; ausência representa vigência sem encerramento definido.")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.LEGAL)
  @UISchema(helpText="Deixe vazio quando não houver encerramento definido; o último dia é incluído.", visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Fim da vigência", group = "Vigência e fundamento", tableHidden = true, readOnly = false, quickViewVisible = true, quickViewOrder = 3, type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, icon = "event", preset = UISchemaPreset.DATE_RANGE)
  private LocalDate dtfim;

  @Schema(description = "Ato administrativo que fundamenta a criação ou alteração desta configuração organizacional.")
  @ResourceReference(resourceKey = "administracao-pessoal.documentos-legais", valueField = "documentoLegalId", surfaceId = "ficha-documento", previewSurfaceId = "ficha-documento", detailPresentation = "drawer")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.LEGAL)
  @UISchema(helpText="Vincule o ato que fundamenta esta configuração.", visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Fundamento legal", group = "Vigência e fundamento", tableHidden = true, readOnly = false, quickViewVisible = true, quickViewOrder = 4, type = FieldDataType.NUMBER, controlType = FieldControlType.ENTITY_LOOKUP, icon = "account_tree", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.DOCUMENTO_OPTIONS, valueField = "id", displayField = "label")
  private Long documentoLegalId;

  @Schema(description = "Justificativa administrativa complementar do período; não substitui o ato legal nem altera seus efeitos.")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Observação", group = "Vigência e fundamento", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  private String observacao;

  @Schema(description = "Características adicionais governadas pelo catálogo de campos customizados deste recurso no órgão.")
  @org.praxisplatform.uischema.customfields.CustomFields(resourceKey = ApiPaths.KEY_GRUPOS_SETORES)
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Atributos do órgão", group = "Vigência e fundamento", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  private Map<String, Object> customFields;

  @Schema(description = "Identificador abreviado do agrupamento transversal no órgão e no período.")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Sigla do grupo", group = "Configuração organizacional", tableHidden = false, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  @jakarta.validation.constraints.NotBlank
  private String sigla;

  @Schema(description = "Descrição institucional do agrupamento de setores para uso administrativo coletivo.")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Nome do grupo", group = "Configuração organizacional", tableHidden = false, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  @jakarta.validation.constraints.NotBlank
  private String nome;

  @Schema(description = "Finalidade operacional do agrupamento, como cálculo de folha, regionalização, custo ou comissão de SST.")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Finalidade do grupo", group = "Configuração organizacional", tableHidden = false, readOnly = false, quickViewVisible = true, quickViewOrder = 5, type = FieldDataType.TEXT, icon = "description")
  @jakarta.validation.constraints.NotBlank
  private String tipoAgrupamento;

  @Schema(description = "Unidade que coordena o agrupamento dentro do órgão.")
  @ResourceReference(resourceKey = "administracao-pessoal.setores", valueField = "setorSede", surfaceId = "ficha-unidade", previewSurfaceId = "ficha-unidade", detailPresentation = "drawer")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Setor sede", group = "Vinculação administrativa", tableHidden = false, readOnly = false, quickViewVisible = true, quickViewOrder = 6, type = FieldDataType.TEXT, controlType = FieldControlType.ENTITY_LOOKUP, icon = "account_tree", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.SETOR_OPTIONS, valueField = "id", displayField = "label")
  private String setorSede;

  @Schema(description = "Código da unidade empregadora à qual o agrupamento está associado.")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Subempresa vinculada", group = "Vinculação administrativa", tableHidden = false, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  private String subempCodigoVinculada;

  @Schema(description = "Unidade federativa abrangida pelo grupo quando houver delimitação territorial.")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "UF de abrangência", group = "Regionalização", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  private String ufAbrangencia;

  @Schema(description = "Município da coordenação administrativa do agrupamento.")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Município sede", group = "Regionalização", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  private String municipioSede;

  @Schema(description = "Indica habilitação administrativa do grupo; sua aplicação também exige período vigente.")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Ativo", group = "Ciclo de vida", tableHidden = true, readOnly = false, type = FieldDataType.BOOLEAN, controlType = FieldControlType.CHECKBOX, icon = "check_circle", preset = UISchemaPreset.BOOLEAN_FLAG, labelTrue = "Sim", labelFalse = "Não", toneTrue = "info", toneFalse = "neutral")
  @jakarta.validation.constraints.NotNull
  private Boolean ativo;

}
