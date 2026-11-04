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
@Schema(description = "Parâmetros funcionais de setores: dados do período organizacional e sua fundamentação.")
public class SetorParametrizacaoErgonDados {
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
  @org.praxisplatform.uischema.customfields.CustomFields(resourceKey = ApiPaths.KEY_SETORES_ERGON)
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Atributos do órgão", group = "Vigência e fundamento", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  private Map<String, Object> customFields;

  @Schema(description = "Código da unidade cujas regras funcionais e financeiras vigoram neste período.")
  @ResourceReference(resourceKey = "administracao-pessoal.setores", valueField = "setor", surfaceId = "ficha-unidade", previewSurfaceId = "ficha-unidade", detailPresentation = "drawer")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Unidade", group = "Configuração organizacional", tableHidden = false, readOnly = false, type = FieldDataType.TEXT, controlType = FieldControlType.ENTITY_LOOKUP, icon = "account_tree", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.SETOR_OPTIONS, valueField = "id", displayField = "label")
  @jakarta.validation.constraints.NotBlank
  private String setor;

  @Schema(description = "Cargo cujo ocupante pode exercer a titularidade do comando; alternativa exclusiva à função de chefia.")
  @ResourceReference(resourceKey = "administracao-pessoal.cargos", valueField = "cargoChefeId", surfaceId = "ficha-cargo", previewSurfaceId = "ficha-cargo", detailPresentation = "drawer")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Cargo de chefia", group = "Comando", tableHidden = false, readOnly = false, quickViewVisible = true, quickViewOrder = 5, type = FieldDataType.NUMBER, controlType = FieldControlType.ENTITY_LOOKUP, icon = "account_tree", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.CARGO_OPTIONS, valueField = "id", displayField = "label")
  private Integer cargoChefeId;

  @Schema(description = "Função de confiança cuja designação identifica o titular; alternativa exclusiva ao cargo de chefia.")
  @ResourceReference(resourceKey = "administracao-pessoal.funcoes", valueField = "funcaoChefeId", surfaceId = "detail", previewSurfaceId = "detail", detailPresentation = "drawer")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Função de chefia", group = "Comando", tableHidden = false, readOnly = false, quickViewVisible = true, quickViewOrder = 6, type = FieldDataType.NUMBER, controlType = FieldControlType.ENTITY_LOOKUP, icon = "account_tree", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.FUNCAO_OPTIONS, valueField = "id", displayField = "label")
  private Long funcaoChefeId;

  @Schema(description = "Classificação funcional e financeira do setor utilizada pelas rotinas de pessoal.")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Tipo funcional", group = "Configuração organizacional", tableHidden = false, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  @jakarta.validation.constraints.NotBlank
  private String tipoSetor;

  @Schema(description = "Classificação das condições de acesso ao local de trabalho, distinta de permissão de acesso ao sistema.")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Tipo de acesso", group = "Configuração organizacional", tableHidden = false, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  @jakarta.validation.constraints.NotBlank
  private String tipoAcesso;

  @Schema(description = "Natureza da classificação usada para organizar o processamento de folha do setor.")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Tipo de classificação", group = "Folha e apropriação", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  private String tipoClassifFolha;

  @Schema(description = "Código de enquadramento do setor dentro do tipo de classificação da folha.")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Classificação de folha", group = "Folha e apropriação", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  private String classifFolha;

  @Schema(description = "Centro de apropriação contábil associado ao setor nesta vigência; não substitui a resolução do setor de pagamento do vínculo.")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Centro de custo", group = "Folha e apropriação", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  @jakarta.validation.constraints.NotBlank
  private String centroCusto;

  @Schema(description = "Entidade sindical de referência da parametrização funcional da unidade.")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Sindicato", group = "Folha e apropriação", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  private String sindicato;

  @Schema(description = "Código postal utilizado pela classificação funcional e financeira do local de trabalho.")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "CEP funcional", group = "Endereço e contato", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  private String cep;

  @Schema(description = "Referência ao catálogo de logradouros usado pela classificação funcional da unidade.")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Código do logradouro", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  private String codigoLogradouro;

  @Schema(description = "Percentual de adicional de insalubridade da parametrização; não comprova concessão individual ou pagamento.")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(helpText="Informe o percentual do ato; a concessão individual depende das regras aplicáveis.", visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Insalubridade (%)", group = "Saúde e segurança", tableHidden = true, readOnly = false, type = FieldDataType.NUMBER, controlType = FieldControlType.INPUT, icon = "numbers")
  @jakarta.validation.constraints.NotNull
  private BigDecimal percInsalubridade;

  @Schema(description = "Percentual de adicional de periculosidade da parametrização; sua incidência individual depende das regras de concessão.")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(helpText="Informe o percentual do ato; este parâmetro não comprova pagamento.", visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Periculosidade (%)", group = "Saúde e segurança", tableHidden = true, readOnly = false, type = FieldDataType.NUMBER, controlType = FieldControlType.INPUT, icon = "numbers")
  @jakarta.validation.constraints.NotNull
  private BigDecimal percPericulosidade;

  @Schema(description = "Classificação de risco ambiental adotada no showcase para inspeção de SST; não representa laudo individual.")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Grau de risco", group = "Saúde e segurança", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  @jakarta.validation.constraints.NotBlank
  private String grauRiscoAmbiental;

  @Schema(description = "Agente de exposição predominante documentado para o ambiente de trabalho.")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Agente nocivo", group = "Saúde e segurança", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  private String agenteNocivoPrincipal;

  @Schema(description = "Indica exigência administrativa de equipamento de proteção para atividades no ambiente.")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(helpText="Registra a obrigação da unidade, sem atestar entrega ou uso individual.", visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Exige EPI", group = "Saúde e segurança", tableHidden = true, readOnly = false, type = FieldDataType.BOOLEAN, controlType = FieldControlType.CHECKBOX, icon = "check_circle", preset = UISchemaPreset.BOOLEAN_FLAG, labelTrue = "Sim", labelFalse = "Não", toneTrue = "info", toneFalse = "neutral")
  @jakarta.validation.constraints.NotNull
  private Boolean exigeEpiObrigatorio;

  @Schema(description = "Referência do processo que instrui a alteração funcional ou financeira da unidade.")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.LEGAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Processo administrativo", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  private String numeroProcesso;

}
