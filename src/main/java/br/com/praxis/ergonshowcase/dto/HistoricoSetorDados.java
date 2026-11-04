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
@Schema(description = "Históricos de setores: dados do período organizacional e sua fundamentação.")
public class HistoricoSetorDados {
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
  @org.praxisplatform.uischema.customfields.CustomFields(resourceKey = ApiPaths.KEY_HISTORICO_SETORES)
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Atributos do órgão", group = "Vigência e fundamento", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  private Map<String, Object> customFields;

  @Schema(description = "Identidade administrativa da unidade dentro do órgão; diferentes vigências conservam este código e possuem ids próprios.")
  @ResourceReference(resourceKey = "administracao-pessoal.setores", valueField = "setor", surfaceId = "ficha-unidade", previewSurfaceId = "ficha-unidade", detailPresentation = "drawer")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Código do setor", group = "Configuração organizacional", tableHidden = false, readOnly = false, quickViewVisible = true, quickViewOrder = 5, type = FieldDataType.TEXT, controlType = FieldControlType.ENTITY_LOOKUP, icon = "account_tree", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.SETOR_OPTIONS, valueField = "id", displayField = "label")
  @jakarta.validation.constraints.NotBlank
  private String setor;

  @Schema(description = "Denominação oficial da unidade no período histórico selecionado.")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Nome da unidade", group = "Configuração organizacional", tableHidden = false, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  @jakarta.validation.constraints.NotBlank
  private String nome;

  @Schema(description = "Denominação completa da unidade quando a identificação resumida exige abreviação.")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Nome por extenso", group = "Configuração organizacional", tableHidden = false, readOnly = false, quickViewVisible = true, quickViewOrder = 6, type = FieldDataType.TEXT, icon = "description")
  private String nomeLongo;

  @Schema(description = "Abreviação institucional da unidade usada no organograma durante esta vigência.")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Sigla", group = "Configuração organizacional", tableHidden = false, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  private String sigla;

  @Schema(description = "Natureza administrativa e atividade-fim da unidade, como diretoria, gabinete ou unidade de saúde; ausência é preservada em períodos importados sem classificação.", nullable=true)
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Tipo de unidade", group = "Configuração organizacional", tableHidden = false, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  private String tipoSetor;

  @Schema(description = "Inscrição fiscal da unidade quando representa órgão da instituição; não é documento pessoal do servidor.")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "CNPJ da lotação", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  private String cgcLotacao;

  @Schema(description = "Razão administrativa da abertura deste período, como criação, reorganização ou mudança de endereço.")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Motivo da alteração", group = "Ciclo de vida", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  private String motivoReestruturacao;

  @Schema(description = "Código da unidade imediatamente superior na hierarquia legal, resolvido no mesmo órgão e na data consultada.")
  @ResourceReference(resourceKey = "administracao-pessoal.setores", valueField = "setorPai", surfaceId = "ficha-unidade", previewSurfaceId = "ficha-unidade", detailPresentation = "drawer")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(helpText="A unidade superior deve existir no mesmo órgão durante a vigência.", visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Superior oficial", group = "Organograma", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, controlType = FieldControlType.ENTITY_LOOKUP, icon = "account_tree", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.SETOR_OPTIONS, valueField = "id", displayField = "label")
  private String setorPai;

  @Schema(description = "Superior da primeira hierarquia funcional alternativa; não redefine a subordinação legal.")
  @ResourceReference(resourceKey = "administracao-pessoal.setores", valueField = "setorPaiHier1", surfaceId = "ficha-unidade", previewSurfaceId = "ficha-unidade", detailPresentation = "drawer")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Superior auxiliar 1", group = "Organograma", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, controlType = FieldControlType.ENTITY_LOOKUP, icon = "account_tree", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.SETOR_OPTIONS, valueField = "id", displayField = "label")
  private String setorPaiHier1;

  @Schema(description = "Superior da segunda hierarquia funcional alternativa na vigência do órgão.")
  @ResourceReference(resourceKey = "administracao-pessoal.setores", valueField = "setorPaiHier2", surfaceId = "ficha-unidade", previewSurfaceId = "ficha-unidade", detailPresentation = "drawer")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Superior auxiliar 2", group = "Organograma", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, controlType = FieldControlType.ENTITY_LOOKUP, icon = "account_tree", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.SETOR_OPTIONS, valueField = "id", displayField = "label")
  private String setorPaiHier2;

  @Schema(description = "Superior da terceira hierarquia funcional alternativa na vigência do órgão.")
  @ResourceReference(resourceKey = "administracao-pessoal.setores", valueField = "setorPaiHier3", surfaceId = "ficha-unidade", previewSurfaceId = "ficha-unidade", detailPresentation = "drawer")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Superior auxiliar 3", group = "Organograma", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, controlType = FieldControlType.ENTITY_LOOKUP, icon = "account_tree", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.SETOR_OPTIONS, valueField = "id", displayField = "label")
  private String setorPaiHier3;

  @Schema(description = "Superior da quarta hierarquia funcional alternativa na vigência do órgão.")
  @ResourceReference(resourceKey = "administracao-pessoal.setores", valueField = "setorPaiHier4", surfaceId = "ficha-unidade", previewSurfaceId = "ficha-unidade", detailPresentation = "drawer")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Superior auxiliar 4", group = "Organograma", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, controlType = FieldControlType.ENTITY_LOOKUP, icon = "account_tree", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.SETOR_OPTIONS, valueField = "id", displayField = "label")
  private String setorPaiHier4;

  @Schema(description = "Sequência de códigos codificados e delimitados desde a raiz; o prefixo completo delimita uma subárvore sem confundir códigos irmãos.", accessMode = Schema.AccessMode.READ_ONLY)
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Caminho hierárquico", group = "Organograma", tableHidden = true, readOnly = true, type = FieldDataType.TEXT, icon = "description")
  @com.fasterxml.jackson.annotation.JsonProperty(access=com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
  private String caminhoHierarquico;

  @Schema(description = "Profundidade a partir da raiz, cujo nível é um; depende da hierarquia e da data de referência.", accessMode = Schema.AccessMode.READ_ONLY)
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Nível hierárquico", group = "Organograma", tableHidden = true, readOnly = true, type = FieldDataType.NUMBER, icon = "numbers")
  @com.fasterxml.jackson.annotation.JsonProperty(access=com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
  private Integer nivelHierarquico;

  @Schema(description = "Código da unidade empregadora fiscal da lotação; sua identidade inclui o órgão e a vigência.")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(helpText="Use o código da unidade empregadora fiscal, não o identificador do período.", visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Subempresa", group = "Vinculação administrativa", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  private String subempCodigo;

  @Schema(description = "Código da regionalização, comarca ou distrito associado à unidade; distingue localização administrativa de endereço.")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(helpText="Use o código da regionalização administrativa.", visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Localidade", group = "Vinculação administrativa", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  private String localidadeCodigo;

  @Schema(description = "Período do agrupamento transversal ao qual esta unidade pertence, no mesmo órgão.")
  @ResourceReference(resourceKey = "administracao-pessoal.grupos-setores", valueField = "grupoSetorId", surfaceId = "detail", previewSurfaceId = "detail", detailPresentation = "drawer")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Grupo de setores", group = "Vinculação administrativa", tableHidden = true, readOnly = false, type = FieldDataType.NUMBER, controlType = FieldControlType.ENTITY_LOOKUP, icon = "account_tree", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.GRUPO_SETOR_OPTIONS, valueField = "id", displayField = "label")
  private Long grupoSetorId;

  @Schema(description = "Marca a unidade desativada para novas lotações; preserva referências e histórico anteriores à extinção.")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Extinto", group = "Ciclo de vida", tableHidden = true, readOnly = false, type = FieldDataType.BOOLEAN, controlType = FieldControlType.CHECKBOX, icon = "check_circle", preset = UISchemaPreset.BOOLEAN_FLAG, labelTrue = "Sim", labelFalse = "Não", toneTrue = "info", toneFalse = "neutral")
  @jakarta.validation.constraints.NotNull
  private Boolean extinto;

  @Schema(description = "Data de efeito da desativação administrativa; não é automaticamente o fim do histórico.")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Data de extinção", group = "Ciclo de vida", tableHidden = true, readOnly = false, type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, icon = "event", preset = UISchemaPreset.DATE_RANGE)
  private LocalDate dataExtincao;

  @Schema(description = "Código da unidade destinada a receber dependências na migração de extinção; a transferência exige comando governado posterior.")
  @ResourceReference(resourceKey = "administracao-pessoal.setores", valueField = "setorSubstituto", surfaceId = "ficha-unidade", previewSurfaceId = "ficha-unidade", detailPresentation = "drawer")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Unidade sucessora", group = "Ciclo de vida", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, controlType = FieldControlType.ENTITY_LOOKUP, icon = "account_tree", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.SETOR_OPTIONS, valueField = "id", displayField = "label")
  private String setorSubstituto;

  @Schema(description = "Espécie do logradouro da unidade, como rua, avenida ou praça.")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Tipo de logradouro", group = "Endereço e contato", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  private String tipoLogradouro;

  @Schema(description = "Nome oficial do logradouro onde a unidade organizacional está instalada.")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Logradouro", group = "Endereço e contato", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  private String logradouro;

  @Schema(description = "Número do imóvel da unidade; admite identificação sem numeração oficial.")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Número", group = "Endereço e contato", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  private String numeroEndereco;

  @Schema(description = "Identificação de bloco, sala ou referência interna do endereço da unidade.")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Complemento", group = "Endereço e contato", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  private String complementoEndereco;

  @Schema(description = "Bairro da instalação física da unidade durante este período.")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Bairro", group = "Endereço e contato", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  private String bairro;

  @Schema(description = "Município da instalação física da unidade; não corresponde ao domicílio dos servidores lotados.")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Município da unidade", group = "Endereço e contato", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  private String municipioLotacao;

  @Schema(description = "Unidade federativa do endereço da unidade durante a vigência.")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "UF da unidade", group = "Endereço e contato", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  private String ufLotacao;

  @Schema(description = "Código postal da instalação física para correspondência institucional.")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "CEP", group = "Endereço e contato", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  private String cep;

  @Schema(description = "Número de contato institucional da unidade, incluindo código de área.")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Telefone", group = "Endereço e contato", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  private String telefone;

  @Schema(description = "Número institucional de fac-símile preservado do cadastro histórico de endereço.")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Fax", group = "Endereço e contato", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  private String fax;

  @Schema(description = "Caixa de correio institucional da unidade para comunicação administrativa.")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "E-mail institucional", group = "Endereço e contato", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  private String emailUnidade;

}
