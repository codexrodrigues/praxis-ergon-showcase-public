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
@Schema(description="Organograma temporal com escopo explícito da apuração de comando e ocupação.")
@ResourceIdentity(keyField="id",titleField="nomeSetor",subtitleField="siglaSetor",metadataFields={"tipoHierarquia"})
public class RadarOrganogramaChefiaOcupacaoResponseDTO {
  @Schema(description = "Identidade composta pelo período histórico, início do segmento e hierarquia selecionada.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Segmento", group = "Configuração organizacional", tableHidden = true, readOnly = true, type = FieldDataType.TEXT, icon = "description")
 private String id;
  @Schema(description = "Identificador da vigência de historico_setores que sustenta este segmento temporal da projeção.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Período de origem", group = "Configuração organizacional", tableHidden = true, readOnly = true, type = FieldDataType.NUMBER, icon = "numbers")
 private Long periodoId;
  @Schema(description = "Órgão ao qual pertencem todos os dados deste nó; nenhuma agregação cruza essa fronteira.", accessMode = Schema.AccessMode.READ_ONLY)
  @ResourceReference(resourceKey = "administracao-pessoal.empresas", valueField = "empCodigo", surfaceId = "ficha-institucional", previewSurfaceId = "ficha-institucional", detailPresentation = "drawer")
  @UISchema(label = "Órgão", group = "Vigência e fundamento", tableHidden = true, readOnly = true, quickViewVisible = true, quickViewOrder = 1, type = FieldDataType.TEXT, controlType = FieldControlType.ENTITY_LOOKUP, icon = "account_tree", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.ORGAO_OPTIONS, valueField = "id", displayField = "label")
 private Integer empCodigo;
  @Schema(description = "Código administrativo da unidade; o id da projeção distingue período e hierarquia.", accessMode = Schema.AccessMode.READ_ONLY)
  @ResourceReference(resourceKey = "administracao-pessoal.setores", valueField = "setor", surfaceId = "ficha-unidade", previewSurfaceId = "ficha-unidade", detailPresentation = "drawer")
  @UISchema(label = "Código da unidade", group = "Configuração organizacional", tableHidden = true, readOnly = true, type = FieldDataType.TEXT, controlType = FieldControlType.ENTITY_LOOKUP, icon = "account_tree", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.SETOR_OPTIONS, valueField = "id", displayField = "label")
 private String setor;
  @Schema(description = "Denominação da unidade na vigência histórica selecionada.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Unidade", group = "Configuração organizacional", tableHidden = false, readOnly = true, type = FieldDataType.TEXT, icon = "description")
 @ResourceReference(resourceKey=ApiPaths.KEY_RADAR_ORGANOGRAMA_CHEFIAS_OCUPACAO,valueField="id",surfaceId="detail",previewSurfaceId="detail",detailPresentation="drawer")
 private String nomeSetor;
  @Schema(description = "Abreviação institucional da unidade no período selecionado.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Sigla", group = "Configuração organizacional", tableHidden = false, readOnly = true, type = FieldDataType.TEXT, icon = "description")
 private String siglaSetor;
  @Schema(description = "Classificação administrativa da unidade na data de referência.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Tipo de unidade", group = "Configuração organizacional", tableHidden = false, readOnly = true, quickViewVisible = true, quickViewOrder = 4, type = FieldDataType.TEXT, icon = "description")
 private String tipoSetor;
  @Schema(description = "Superior na hierarquia escolhida, resolvido na mesma data e no mesmo órgão.", accessMode = Schema.AccessMode.READ_ONLY)
  @ResourceReference(resourceKey = "administracao-pessoal.setores", valueField = "setorPai", surfaceId = "ficha-unidade", previewSurfaceId = "ficha-unidade", detailPresentation = "drawer")
  @UISchema(label = "Superior selecionado", group = "Configuração organizacional", tableHidden = true, readOnly = true, type = FieldDataType.TEXT, controlType = FieldControlType.ENTITY_LOOKUP, icon = "account_tree", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.SETOR_OPTIONS, valueField = "id", displayField = "label")
 private String setorPai;
  @Schema(description = "Superior da primeira hierarquia funcional alternativa na vigência.", accessMode = Schema.AccessMode.READ_ONLY)
  @ResourceReference(resourceKey = "administracao-pessoal.setores", valueField = "setorPaiHier1", surfaceId = "ficha-unidade", previewSurfaceId = "ficha-unidade", detailPresentation = "drawer")
  @UISchema(label = "Superior auxiliar 1", group = "Configuração organizacional", tableHidden = true, readOnly = true, type = FieldDataType.TEXT, controlType = FieldControlType.ENTITY_LOOKUP, icon = "account_tree", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.SETOR_OPTIONS, valueField = "id", displayField = "label")
 private String setorPaiHier1;
  @Schema(description = "Superior da segunda hierarquia funcional alternativa na vigência.", accessMode = Schema.AccessMode.READ_ONLY)
  @ResourceReference(resourceKey = "administracao-pessoal.setores", valueField = "setorPaiHier2", surfaceId = "ficha-unidade", previewSurfaceId = "ficha-unidade", detailPresentation = "drawer")
  @UISchema(label = "Superior auxiliar 2", group = "Configuração organizacional", tableHidden = true, readOnly = true, type = FieldDataType.TEXT, controlType = FieldControlType.ENTITY_LOOKUP, icon = "account_tree", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.SETOR_OPTIONS, valueField = "id", displayField = "label")
 private String setorPaiHier2;
  @Schema(description = "Superior da terceira hierarquia funcional alternativa na vigência.", accessMode = Schema.AccessMode.READ_ONLY)
  @ResourceReference(resourceKey = "administracao-pessoal.setores", valueField = "setorPaiHier3", surfaceId = "ficha-unidade", previewSurfaceId = "ficha-unidade", detailPresentation = "drawer")
  @UISchema(label = "Superior auxiliar 3", group = "Configuração organizacional", tableHidden = true, readOnly = true, type = FieldDataType.TEXT, controlType = FieldControlType.ENTITY_LOOKUP, icon = "account_tree", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.SETOR_OPTIONS, valueField = "id", displayField = "label")
 private String setorPaiHier3;
  @Schema(description = "Superior da quarta hierarquia funcional alternativa na vigência.", accessMode = Schema.AccessMode.READ_ONLY)
  @ResourceReference(resourceKey = "administracao-pessoal.setores", valueField = "setorPaiHier4", surfaceId = "ficha-unidade", previewSurfaceId = "ficha-unidade", detailPresentation = "drawer")
  @UISchema(label = "Superior auxiliar 4", group = "Configuração organizacional", tableHidden = true, readOnly = true, type = FieldDataType.TEXT, controlType = FieldControlType.ENTITY_LOOKUP, icon = "account_tree", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.SETOR_OPTIONS, valueField = "id", displayField = "label")
 private String setorPaiHier4;
  @Schema(description = "Caminho calculado por códigos exatos e segmentos delimitados na hierarquia e na data deste segmento.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Caminho", group = "Configuração organizacional", tableHidden = true, readOnly = true, type = FieldDataType.TEXT, icon = "description")
 private String caminhoHierarquico;
  @Schema(description = "Profundidade calculada na hierarquia escolhida; a raiz ocupa o nível um.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Nível", group = "Configuração organizacional", tableHidden = false, readOnly = true, type = FieldDataType.NUMBER, icon = "numbers")
 private Integer nivelHierarquico;
  @Schema(description = "Quantidade de filhos imediatos válidos no órgão, na data e na hierarquia selecionados.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Subordinados diretos", group = "Configuração organizacional", tableHidden = true, readOnly = true, type = FieldDataType.NUMBER, icon = "numbers")
 private Long quantidadeSubordinadosDiretos;
  @Schema(description = "Zero seleciona a hierarquia oficial; um a quatro selecionam as hierarquias auxiliares.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Hierarquia", group = "Configuração organizacional", tableHidden = true, readOnly = true, type = FieldDataType.NUMBER, icon = "numbers")
 private Integer tipoHierarquia;
  @Schema(description = "Primeiro dia em que esta composição de unidade, ancestrais e parâmetros permanece válida.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Início do segmento", group = "Vigência e fundamento", tableHidden = true, readOnly = true, quickViewVisible = true, quickViewOrder = 2, type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, icon = "event", preset = UISchemaPreset.DATE_RANGE)
 private LocalDate dtini;
  @Schema(description = "Último dia desta composição; uma alteração em qualquer ancestral pode encerrar o segmento sem encerrar a unidade.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Fim do segmento", group = "Vigência e fundamento", tableHidden = true, readOnly = true, quickViewVisible = true, quickViewOrder = 3, type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, icon = "event", preset = UISchemaPreset.DATE_RANGE)
 private LocalDate dtfim;
  @Schema(description = "Indica desativação administrativa da unidade durante o período.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Extinto", group = "Configuração organizacional", tableHidden = true, readOnly = true, type = FieldDataType.BOOLEAN, controlType = FieldControlType.CHECKBOX, icon = "check_circle", preset = UISchemaPreset.BOOLEAN_FLAG, labelTrue = "Sim", labelFalse = "Não", toneTrue = "info", toneFalse = "neutral")
 private Boolean extinto;
  @Schema(description = "Código da unidade indicada para receber dependências em extinção; não comprova transferência executada.", accessMode = Schema.AccessMode.READ_ONLY)
  @ResourceReference(resourceKey = "administracao-pessoal.setores", valueField = "setorSubstituto", surfaceId = "ficha-unidade", previewSurfaceId = "ficha-unidade", detailPresentation = "drawer")
  @UISchema(label = "Unidade sucessora", group = "Configuração organizacional", tableHidden = true, readOnly = true, type = FieldDataType.TEXT, controlType = FieldControlType.ENTITY_LOOKUP, icon = "account_tree", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.SETOR_OPTIONS, valueField = "id", displayField = "label")
 private String setorSubstituto;
  @Schema(description = "Código da unidade empregadora fiscal associada à lotação na vigência.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Subempresa", group = "Configuração organizacional", tableHidden = true, readOnly = true, type = FieldDataType.TEXT, icon = "description")
 private String subempCodigo;
  @Schema(description = "Código da regionalização administrativa da unidade na vigência.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Localidade", group = "Configuração organizacional", tableHidden = true, readOnly = true, type = FieldDataType.TEXT, icon = "description")
 private String localidadeCodigo;
  @Schema(description = "Centro contábil da parametrização funcional válida neste segmento.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Centro de custo", group = "Configuração organizacional", tableHidden = true, readOnly = true, type = FieldDataType.TEXT, icon = "description")
 private String centroCusto;
  @Schema(description = "Cargo parametrizado como origem da titularidade de comando da unidade.", accessMode = Schema.AccessMode.READ_ONLY)
  @ResourceReference(resourceKey = "administracao-pessoal.cargos", valueField = "cargoChefeId", surfaceId = "ficha-cargo", previewSurfaceId = "ficha-cargo", detailPresentation = "drawer")
  @UISchema(label = "Cargo de comando", group = "Configuração organizacional", tableHidden = true, readOnly = true, type = FieldDataType.TEXT, controlType = FieldControlType.ENTITY_LOOKUP, icon = "account_tree", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.CARGO_OPTIONS, valueField = "id", displayField = "label")
 private Integer cargoChefeId;
  @Schema(description = "Função parametrizada como origem da titularidade de comando da unidade.", accessMode = Schema.AccessMode.READ_ONLY)
  @ResourceReference(resourceKey = "administracao-pessoal.funcoes", valueField = "funcaoChefeId", surfaceId = "detail", previewSurfaceId = "detail", detailPresentation = "drawer")
  @UISchema(label = "Função de comando", group = "Configuração organizacional", tableHidden = true, readOnly = true, type = FieldDataType.TEXT, controlType = FieldControlType.ENTITY_LOOKUP, icon = "account_tree", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.FUNCAO_OPTIONS, valueField = "id", displayField = "label")
 private Long funcaoChefeId;
  @Schema(description = "Número funcional do titular apurado na data; nulo quando a integração não permite apuração.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Servidor titular", group = "Configuração organizacional", tableHidden = true, readOnly = true, type = FieldDataType.NUMBER, icon = "numbers")
 private Long chefeTitularNumfunc;
  @Schema(description = "Número do vínculo do titular; deve ser interpretado junto ao número funcional.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Vínculo do titular", group = "Configuração organizacional", tableHidden = true, readOnly = true, type = FieldDataType.NUMBER, icon = "numbers")
 private Integer chefeTitularNumvinc;
  @Schema(description = "Nome do titular apurado para o órgão e a data; não é preenchido por uma massa fictícia do radar.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Titular", group = "Configuração organizacional", tableHidden = true, readOnly = true, type = FieldDataType.TEXT, icon = "description")
 @ResourceReference(resourceKey="administracao-pessoal.vinculos",valueField="chefeTitularId",surfaceId="resumo-funcional",previewSurfaceId="resumo-funcional",detailPresentation="drawer")
 @DomainGovernance(kind=DomainGovernanceKind.PRIVACY,classification=DomainClassification.CONFIDENTIAL,dataCategory=DomainDataCategory.PERSONAL,exportable=false)
 private String chefeTitularNome;
  @Schema(description = "Número funcional de quem exerce o comando na data; pode diferir do titular por substituição.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Servidor em exercício", group = "Configuração organizacional", tableHidden = true, readOnly = true, type = FieldDataType.NUMBER, icon = "numbers")
 private Long chefeEmExercicioNumfunc;
  @Schema(description = "Número do vínculo de quem exerce o comando, pareado com o número funcional.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Vínculo em exercício", group = "Configuração organizacional", tableHidden = true, readOnly = true, type = FieldDataType.NUMBER, icon = "numbers")
 private Integer chefeEmExercicioNumvinc;
  @Schema(description = "Nome de quem efetivamente exerce o comando após análise das ausências e substituições.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Comando em exercício", group = "Configuração organizacional", tableHidden = true, readOnly = true, type = FieldDataType.TEXT, icon = "description")
 @ResourceReference(resourceKey="administracao-pessoal.vinculos",valueField="chefeEmExercicioId",surfaceId="resumo-funcional",previewSurfaceId="resumo-funcional",detailPresentation="drawer")
 @DomainGovernance(kind=DomainGovernanceKind.PRIVACY,classification=DomainClassification.CONFIDENTIAL,dataCategory=DomainDataCategory.PERSONAL,exportable=false)
 private String chefeEmExercicioNome;
  @Schema(description = "Família do impedimento vigente do titular; exige integração de férias, licença especial, afastamento e frequência.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Ausência do titular", group = "Configuração organizacional", tableHidden = true, readOnly = true, type = FieldDataType.TEXT, icon = "description")
 @DomainGovernance(kind=DomainGovernanceKind.PRIVACY,classification=DomainClassification.CONFIDENTIAL,dataCategory=DomainDataCategory.PERSONAL,exportable=false)
 private String motivoAusenciaTitular;
  @Schema(description = "Diagnóstico do comando apurado na data; nulo significa apuração ainda indisponível, distinto de vacância ou descobertura.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Situação do comando", group = "Configuração organizacional", tableHidden = false, readOnly = true, quickViewVisible = true, quickViewOrder = 5, type = FieldDataType.TEXT, icon = "description")
 private String situacaoComandoSetor;
  @Schema(description = "Quantidade de vínculos com provimento no setor na data; desconhecida sem histórico de provimentos.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Lotados por provimento", group = "Configuração organizacional", tableHidden = true, readOnly = true, type = FieldDataType.NUMBER, icon = "numbers")
 private Long lotadosProvimentoCount;
  @Schema(description = "Quantidade de vínculos trabalhando fisicamente na unidade; não é inferida da lotação de provimento.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Em exercício físico", group = "Configuração organizacional", tableHidden = true, readOnly = true, type = FieldDataType.NUMBER, icon = "numbers")
 private Long emExercicioFisicoNoSetorCount;
  @Schema(description = "Vínculos lotados nesta unidade cujo exercício físico ocorre em outra unidade na data.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Em outras unidades", group = "Configuração organizacional", tableHidden = true, readOnly = true, type = FieldDataType.NUMBER, icon = "numbers")
 private Long cedidosParaOutrosSetoresCount;
  @Schema(description = "Vínculos em exercício físico nesta unidade com provimento em outra unidade na data.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Recebidos de outras unidades", group = "Configuração organizacional", tableHidden = true, readOnly = true, type = FieldDataType.NUMBER, icon = "numbers")
 private Long recebidosDeOutrosSetoresCount;
  @Schema(description = "Vínculos cujo setor de pagamento resolve para esta unidade; não é substituído pela contagem de lotados.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Apropriados na folha", group = "Configuração organizacional", tableHidden = true, readOnly = true, type = FieldDataType.NUMBER, icon = "numbers")
 private Long apropriadosFolhaSetorCount;
  @Schema(description = "Soma das cotas exclusivas deste nó na data; reservas que abrangem subárvore não são somadas novamente.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Vagas do nó", group = "Configuração organizacional", tableHidden = false, readOnly = true, type = FieldDataType.NUMBER, icon = "numbers")
 private Long vagasAutorizadasSetor;
  @Schema(description = "Ocupação efetivamente apurada para as cotas do nó; nulo preserva a ausência da integração temporal.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Vagas ocupadas", group = "Configuração organizacional", tableHidden = false, readOnly = true, type = FieldDataType.NUMBER, icon = "numbers")
 private Long vagasOcupadasSetor;
  @Schema(description = "Percentual de unidades com comando coberto na subárvore; exige apuração completa dos nós na mesma data.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Cobertura de chefia (%)", group = "Configuração organizacional", tableHidden = true, readOnly = true, type = FieldDataType.NUMBER, controlType = FieldControlType.INPUT, icon = "numbers")
 private BigDecimal percentualCoberturaChefiaSubarvore;
  @Schema(description = "Declara quais famílias de indicadores estão disponíveis, evitando interpretar ausência de integração como zero.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Escopo da apuração", group = "Configuração organizacional", tableHidden = true, readOnly = true, quickViewVisible = true, quickViewOrder = 6, type = FieldDataType.TEXT, icon = "description")
 private String escopoApuracao;
  @MicroVisualization(kind=MicroVisualizationKind.BULLET,valueExpr="vagasOcupadasSetor",targetExpr="vagasAutorizadasSetor",totalExpr="vagasAutorizadasSetor",min=0,thresholds=@Threshold(min=0,tone="info",label="Ocupação apurada"),fallbackText="Ocupação temporal ainda não integrada",ariaLabel="Ocupação do quadro no período")
  public Long getVagasOcupadasSetor() {return vagasOcupadasSetor;}
  @MicroVisualization(kind=MicroVisualizationKind.RADIAL,valueExpr="percentualCoberturaChefiaSubarvore",totalExpr="=100",min=0,max=100,thresholds=@Threshold(min=100,tone="success",label="Comando integralmente coberto"),fallbackText="Cobertura ainda não apurada",ariaLabel="Cobertura de comando na subárvore")
  public BigDecimal getPercentualCoberturaChefiaSubarvore() {return percentualCoberturaChefiaSubarvore;}
  @Schema(description="Identidade composta do vínculo titular; preserva número funcional e número do vínculo sem concatenação própria do host.",accessMode=Schema.AccessMode.READ_ONLY)
  @UISchema(label="Identidade do titular",group="Comando",readOnly=true,tableHidden=true,type=FieldDataType.TEXT,controlType=FieldControlType.ENTITY_LOOKUP,icon="badge",preset=UISchemaPreset.ENTERPRISE_ID,endpoint=ApiPaths.VINCULO_OPTIONS,valueField="id",displayField="label")
  @ResourceReference(resourceKey="administracao-pessoal.vinculos",valueField="chefeTitularId",surfaceId="resumo-funcional",previewSurfaceId="resumo-funcional",detailPresentation="drawer")
  public br.com.praxis.ergonshowcase.domain.VinculoId getChefeTitularId() {
    return chefeTitularNumfunc==null||chefeTitularNumvinc==null?null:new br.com.praxis.ergonshowcase.domain.VinculoId(chefeTitularNumfunc,chefeTitularNumvinc);
  }
  @Schema(description="Identidade composta do vínculo que exerce o comando na data; pode diferir do vínculo titular.",accessMode=Schema.AccessMode.READ_ONLY)
  @UISchema(label="Identidade em exercício",group="Comando",readOnly=true,tableHidden=true,type=FieldDataType.TEXT,controlType=FieldControlType.ENTITY_LOOKUP,icon="badge",preset=UISchemaPreset.ENTERPRISE_ID,endpoint=ApiPaths.VINCULO_OPTIONS,valueField="id",displayField="label")
  @ResourceReference(resourceKey="administracao-pessoal.vinculos",valueField="chefeEmExercicioId",surfaceId="resumo-funcional",previewSurfaceId="resumo-funcional",detailPresentation="drawer")
  public br.com.praxis.ergonshowcase.domain.VinculoId getChefeEmExercicioId() {
    return chefeEmExercicioNumfunc==null||chefeEmExercicioNumvinc==null?null:new br.com.praxis.ergonshowcase.domain.VinculoId(chefeEmExercicioNumfunc,chefeEmExercicioNumvinc);
  }
}
