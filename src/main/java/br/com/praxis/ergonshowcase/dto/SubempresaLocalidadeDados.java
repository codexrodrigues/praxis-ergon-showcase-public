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
@Schema(description = "Subempresas e localidades: dados do período organizacional e sua fundamentação.")
public class SubempresaLocalidadeDados {
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
  @org.praxisplatform.uischema.customfields.CustomFields(resourceKey = ApiPaths.KEY_SUBEMPRESAS_LOCALIDADES)
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Atributos do órgão", group = "Vigência e fundamento", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  private Map<String, Object> customFields;

  @Schema(description = "Código administrativo da subempresa ou localidade; a identidade inclui órgão, natureza e vigência.")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Código da unidade", group = "Configuração organizacional", tableHidden = false, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  @jakarta.validation.constraints.NotBlank
  private String codigo;

  @Schema(description = "Distingue empregador fiscal, localidade regional ou unidade mista no modelo do showcase.")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Natureza da unidade", group = "Configuração organizacional", tableHidden = false, readOnly = false, quickViewVisible = true, quickViewOrder = 5, type = FieldDataType.TEXT, icon = "description")
  @jakarta.validation.constraints.NotBlank
  private String naturezaUnidade;

  @Schema(description = "Nome administrativo da unidade empregadora ou da localidade regional.")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Nome", group = "Configuração organizacional", tableHidden = false, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  @jakarta.validation.constraints.NotBlank
  private String nome;

  @Schema(description = "Denominação abreviada utilizada na identificação institucional da unidade empregadora.")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Nome fantasia", group = "Identidade fiscal", tableHidden = false, readOnly = false, quickViewVisible = true, quickViewOrder = 6, type = FieldDataType.TEXT, icon = "description")
  private String nomeFantasia;

  @Schema(description = "Denominação jurídica da unidade empregadora responsável pelos registros fiscais.")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Razão social", group = "Identidade fiscal", tableHidden = false, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  private String razaoSocial;

  @Schema(description = "Inscrição fiscal da unidade empregadora; localidades sem personalidade fiscal podem não possuir CNPJ.")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "CNPJ", group = "Identidade fiscal", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  private String cnpj;

  @Schema(description = "Inscrição responsável pela declaração das informações da unidade aos órgãos oficiais e ao eSocial.")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(helpText="Identifique a pessoa jurídica declarante, que pode diferir da unidade de lotação.", visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "CNPJ declarante", group = "Identidade fiscal", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  private String cnpjDeclaranteEsocial;

  @Schema(description = "Código da atividade econômica principal informado para a unidade empregadora.")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "CNAE preponderante", group = "Identidade fiscal", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  private String cnaePreponderante;

  @Schema(description = "Código de classificação jurídica do empregador no catálogo fiscal.")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Natureza jurídica", group = "Identidade fiscal", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  private String naturezaJuridica;

  @Schema(description = "Código da unidade empregadora superior dentro do mesmo órgão.")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Subempresa superior", group = "Vinculação administrativa", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  private String subempresaPaiCodigo;

  @Schema(description = "Código do setor que sedia administrativamente esta subempresa ou localidade.")
  @ResourceReference(resourceKey = "administracao-pessoal.setores", valueField = "setorSede", surfaceId = "ficha-unidade", previewSurfaceId = "ficha-unidade", detailPresentation = "drawer")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Setor sede", group = "Vinculação administrativa", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, controlType = FieldControlType.ENTITY_LOOKUP, icon = "account_tree", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.SETOR_OPTIONS, valueField = "id", displayField = "label")
  private String setorSede;

  @Schema(description = "Classificação da regionalização institucional, como comarca, distrito ou regional.")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Tipo de localidade", group = "Regionalização", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  private String tipoLocalidade;

  @Schema(description = "Região de abrangência territorial da localidade administrativa.")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Região", group = "Regionalização", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  private String regiaoGeografica;

  @Schema(description = "Classificação da comarca na organização judiciária, quando aplicável.")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Entrância", group = "Regionalização", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  private String entranciaComarca;

  @Schema(description = "Unidade federativa da sede da unidade empregadora ou localidade.")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "UF", group = "Endereço e contato", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  private String uf;

  @Schema(description = "Município sede da subempresa ou localidade administrativa.")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Município", group = "Endereço e contato", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  private String municipio;

  @Schema(description = "Código postal da sede para correspondência institucional.")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "CEP", group = "Endereço e contato", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  private String cep;

  @Schema(description = "Logradouro da sede da unidade empregadora ou localidade.")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Logradouro", group = "Endereço e contato", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  private String logradouro;

  @Schema(description = "Indica se a unidade está habilitada administrativamente, além da verificação de sua vigência.")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Ativa", group = "Ciclo de vida", tableHidden = true, readOnly = false, type = FieldDataType.BOOLEAN, controlType = FieldControlType.CHECKBOX, icon = "check_circle", preset = UISchemaPreset.BOOLEAN_FLAG, labelTrue = "Sim", labelFalse = "Não", toneTrue = "info", toneFalse = "neutral")
  @jakarta.validation.constraints.NotNull
  private Boolean ativo;

}
