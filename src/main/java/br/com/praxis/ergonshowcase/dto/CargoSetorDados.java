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
@Schema(description = "Cargos e quadro por setor: dados do período organizacional e sua fundamentação.")
public class CargoSetorDados {
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
  @org.praxisplatform.uischema.customfields.CustomFields(resourceKey = ApiPaths.KEY_CARGOS_POR_SETOR)
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Atributos do órgão", group = "Vigência e fundamento", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  private Map<String, Object> customFields;

  @Schema(description = "Código da unidade titular da autorização de cargo e da reserva ou distribuição de vagas.")
  @ResourceReference(resourceKey = "administracao-pessoal.setores", valueField = "setor", surfaceId = "ficha-unidade", previewSurfaceId = "ficha-unidade", detailPresentation = "drawer")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Unidade", group = "Configuração organizacional", tableHidden = false, readOnly = false, type = FieldDataType.TEXT, controlType = FieldControlType.ENTITY_LOOKUP, icon = "account_tree", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.SETOR_OPTIONS, valueField = "id", displayField = "label")
  @jakarta.validation.constraints.NotBlank
  private String setor;

  @Schema(description = "Cargo do mesmo órgão autorizado a compor a força de trabalho desta unidade no período.")
  @ResourceReference(resourceKey = "administracao-pessoal.cargos", valueField = "cargoId", surfaceId = "ficha-cargo", previewSurfaceId = "ficha-cargo", detailPresentation = "drawer")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Cargo autorizado", group = "Configuração organizacional", tableHidden = false, readOnly = false, quickViewVisible = true, quickViewOrder = 5, type = FieldDataType.NUMBER, controlType = FieldControlType.ENTITY_LOOKUP, icon = "account_tree", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.CARGO_OPTIONS, valueField = "id", displayField = "label")
  @jakarta.validation.constraints.NotNull
  private Integer cargoId;

  @Schema(description = "Inclui a subárvore na reserva de quadro, equivalente ao SUBORD do legado; não soma cotas filhas automaticamente.")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(helpText="Inclua descendentes apenas quando o ato autorizar o quadro da subárvore.", visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Abrange subordinados", group = "Quadro autorizado", tableHidden = false, readOnly = false, quickViewVisible = true, quickViewOrder = 6, type = FieldDataType.BOOLEAN, controlType = FieldControlType.CHECKBOX, icon = "check_circle", preset = UISchemaPreset.BOOLEAN_FLAG, labelTrue = "Sim", labelFalse = "Não", toneTrue = "info", toneFalse = "neutral")
  @jakarta.validation.constraints.NotNull
  private Boolean abrangeSubordinados;

  @Schema(description = "Distingue reserva da unidade de distribuição derivada de uma reserva superior.")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Controle de quadro", group = "Quadro autorizado", tableHidden = false, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  @jakarta.validation.constraints.NotBlank
  private String tipoControleQuadro;

  @Schema(description = "Autorização superior do mesmo órgão da qual esta distribuição recebe sua cota.")
  @ResourceReference(resourceKey = "administracao-pessoal.cargos-por-setor", valueField = "quadroPaiId", surfaceId = "detail", previewSurfaceId = "detail", detailPresentation = "drawer")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(helpText="Relacione a reserva que sustenta esta distribuição de vagas.", visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Reserva de origem", group = "Quadro autorizado", tableHidden = false, readOnly = false, type = FieldDataType.NUMBER, controlType = FieldControlType.ENTITY_LOOKUP, icon = "account_tree", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.CARGO_SETOR_OPTIONS, valueField = "id", displayField = "label")
  private Long quadroPaiId;

  @Schema(description = "Limite quantitativo de ocupação autorizado para o cargo no setor ou na subárvore abrangida.")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Vagas autorizadas", group = "Quadro autorizado", tableHidden = true, readOnly = false, type = FieldDataType.NUMBER, icon = "numbers")
  @jakarta.validation.constraints.NotNull
  private Integer lotacaoMaximaAutorizada;

  @Schema(description = "Ocupação calculada pela integração de provimentos; nulo significa ainda não apurada e não equivale a zero.", accessMode = Schema.AccessMode.READ_ONLY)
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(helpText="Informação derivada; ausência de apuração não significa zero ocupantes.", visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Vagas ocupadas", group = "Quadro autorizado", tableHidden = true, readOnly = true, type = FieldDataType.NUMBER, icon = "numbers")
  @com.fasterxml.jackson.annotation.JsonProperty(access=com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
  private Integer lotacaoOcupadaAtual;

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

  @Schema(description = "Indica que a ocupação desta autorização depende de habilitação profissional específica.")
  @DomainGovernance(kind=DomainGovernanceKind.SECURITY,classification=DomainClassification.INTERNAL,dataCategory=DomainDataCategory.OPERATIONAL)
  @UISchema(visibleForAuthorities={"ERGON_READ"}, editableForAuthorities={"ERGON_WRITE"}, label = "Exige habilitação", group = "Saúde e segurança", tableHidden = true, readOnly = false, type = FieldDataType.BOOLEAN, controlType = FieldControlType.CHECKBOX, icon = "check_circle", preset = UISchemaPreset.BOOLEAN_FLAG, labelTrue = "Sim", labelFalse = "Não", toneTrue = "info", toneFalse = "neutral")
  @jakarta.validation.constraints.NotNull
  private Boolean exigeHabilitacaoEspecifica;

}
