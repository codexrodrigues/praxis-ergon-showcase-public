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
import org.praxisplatform.uischema.filter.annotation.Filterable;
import org.praxisplatform.uischema.filter.annotation.Filterable.FilterOperation;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;
@Getter @Setter @Schema(description="Pesquisa temporal e hierárquica do organograma por órgão e composição válida na data.")
public class RadarOrganogramaChefiaOcupacaoFilterDTO implements GenericFilterDTO {
  @Schema(description = "Código da raiz do recorte 360°. Inclui a unidade e todos os descendentes na data e na hierarquia consultadas.")
  @UISchema(label = "Raiz da subárvore", group = "Organograma", type = FieldDataType.TEXT,
      controlType = FieldControlType.ENTITY_LOOKUP, icon = "account_tree",
      preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.SETOR_OPTIONS,
      valueField = "id", displayField = "label")
  private String raizSubarvore;
  @Schema(description="Inclui descendentes do caminho selecionado; falso restringe a inspeção ao próprio nó.")
  @UISchema(label="Incluir subordinados",group="Organograma",type=FieldDataType.BOOLEAN,controlType=FieldControlType.CHECKBOX,icon="account_tree",preset=UISchemaPreset.BOOLEAN_FLAG)
  private Boolean incluirSubordinados;
  @Schema(description = "Órgão ao qual pertencem todos os dados deste nó; nenhuma agregação cruza essa fronteira.")
  @ResourceReference(resourceKey = "administracao-pessoal.empresas", valueField = "empCodigo", surfaceId = "ficha-institucional", previewSurfaceId = "ficha-institucional", detailPresentation = "drawer")
  @UISchema(label = "Órgão", group = "Vigência e fundamento", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, controlType = FieldControlType.ENTITY_LOOKUP, icon = "account_tree", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.ORGAO_OPTIONS, valueField = "id", displayField = "label")
 @Filterable(operation=FilterOperation.EQUAL,relation="empCodigo")
 private Integer empCodigo;
  @Schema(description = "Código administrativo da unidade; o id da projeção distingue período e hierarquia.")
  @ResourceReference(resourceKey = "administracao-pessoal.setores", valueField = "setor", surfaceId = "ficha-unidade", previewSurfaceId = "ficha-unidade", detailPresentation = "drawer")
  @UISchema(label = "Código da unidade", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, controlType = FieldControlType.ENTITY_LOOKUP, icon = "account_tree", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.SETOR_OPTIONS, valueField = "id", displayField = "label")
 @Filterable(operation=FilterOperation.EQUAL,relation="setor")
 private String setor;
  @Schema(description = "Denominação da unidade na vigência histórica selecionada.")
  @UISchema(label = "Unidade", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
 @Filterable(operation=FilterOperation.LIKE,relation="nomeSetor")
 private String nomeSetor;
  @Schema(description = "Abreviação institucional da unidade no período selecionado.")
  @UISchema(label = "Sigla", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
 @Filterable(operation=FilterOperation.STARTS_WITH,relation="siglaSetor")
 private String siglaSetor;
  @Schema(description = "Classificação administrativa da unidade na data de referência.")
  @UISchema(label = "Tipo de unidade", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
 @Filterable(operation=FilterOperation.EQUAL,relation="tipoSetor")
 private String tipoSetor;
  @Schema(description = "Superior na hierarquia escolhida, resolvido na mesma data e no mesmo órgão.")
  @ResourceReference(resourceKey = "administracao-pessoal.setores", valueField = "setorPai", surfaceId = "ficha-unidade", previewSurfaceId = "ficha-unidade", detailPresentation = "drawer")
  @UISchema(label = "Superior selecionado", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, controlType = FieldControlType.ENTITY_LOOKUP, icon = "account_tree", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.SETOR_OPTIONS, valueField = "id", displayField = "label")
 @Filterable(operation=FilterOperation.EQUAL,relation="setorPai")
 private String setorPai;
  @Schema(description = "Zero seleciona a hierarquia oficial; um a quatro selecionam as hierarquias auxiliares.")
  @UISchema(label = "Hierarquia", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.NUMBER, icon = "numbers")
 @Filterable(operation=FilterOperation.EQUAL,relation="tipoHierarquia")
 private Integer tipoHierarquia = 0;
  @Schema(description = "Profundidade calculada na hierarquia escolhida; a raiz ocupa o nível um.")
  @UISchema(label = "Nível", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.NUMBER, icon = "numbers")
 @Filterable(operation=FilterOperation.EQUAL,relation="nivelHierarquico")
 private Integer nivelHierarquico;
  @Schema(description = "Indica desativação administrativa da unidade durante o período.")
  @UISchema(label = "Extinto", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.BOOLEAN, controlType = FieldControlType.CHECKBOX, icon = "check_circle", preset = UISchemaPreset.BOOLEAN_FLAG, labelTrue = "Sim", labelFalse = "Não", toneTrue = "info", toneFalse = "neutral")
 @Filterable(operation=FilterOperation.EQUAL,relation="extinto")
 private Boolean extinto;
  @Schema(description = "Código da unidade empregadora fiscal associada à lotação na vigência.")
  @UISchema(label = "Subempresa", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
 @Filterable(operation=FilterOperation.EQUAL,relation="subempCodigo")
 private String subempCodigo;
  @Schema(description = "Código da regionalização administrativa da unidade na vigência.")
  @UISchema(label = "Localidade", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
 @Filterable(operation=FilterOperation.EQUAL,relation="localidadeCodigo")
 private String localidadeCodigo;
  @Schema(description = "Centro contábil da parametrização funcional válida neste segmento.")
  @UISchema(label = "Centro de custo", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
 @Filterable(operation=FilterOperation.LIKE,relation="centroCusto")
 private String centroCusto;
  @Schema(description = "Diagnóstico do comando apurado na data; nulo significa apuração ainda indisponível, distinto de vacância ou descobertura.")
  @UISchema(label = "Situação do comando", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
 @Filterable(operation=FilterOperation.EQUAL,relation="situacaoComandoSetor")
 private String situacaoComandoSetor;
  @Schema(description = "Cargo parametrizado como origem da titularidade de comando da unidade.")
  @ResourceReference(resourceKey = "administracao-pessoal.cargos", valueField = "cargoChefeId", surfaceId = "ficha-cargo", previewSurfaceId = "ficha-cargo", detailPresentation = "drawer")
  @UISchema(label = "Cargo de comando", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, controlType = FieldControlType.ENTITY_LOOKUP, icon = "account_tree", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.CARGO_OPTIONS, valueField = "id", displayField = "label")
 @Filterable(operation=FilterOperation.EQUAL,relation="cargoChefeId")
 private Integer cargoChefeId;
  @Schema(description = "Função parametrizada como origem da titularidade de comando da unidade.")
  @ResourceReference(resourceKey = "administracao-pessoal.funcoes", valueField = "funcaoChefeId", surfaceId = "detail", previewSurfaceId = "detail", detailPresentation = "drawer")
  @UISchema(label = "Função de comando", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, controlType = FieldControlType.ENTITY_LOOKUP, icon = "account_tree", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.FUNCAO_OPTIONS, valueField = "id", displayField = "label")
 @Filterable(operation=FilterOperation.EQUAL,relation="funcaoChefeId")
 private Long funcaoChefeId;
  @Schema(description = "Data de inspeção da árvore; seleciona o segmento que contém a data inclusive nos limites.")
  @UISchema(label = "Data de referência", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, icon = "event", preset = UISchemaPreset.DATE_RANGE)
 @Filterable(operation=FilterOperation.EFFECTIVE_ON_DATE,relation="dtini",endRelation="dtfim")
 private LocalDate dataReferencia = LocalDate.now();
  @Schema(description = "Pesquisa simultânea pelo código, nome, sigla e centro de custo da unidade na vigência.")
  @UISchema(label = "Busca ampla", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
 @Filterable(operation=FilterOperation.LIKE,relations={"setor","nomeSetor","siglaSetor","centroCusto"})
 private String buscaAmpla;
  @Schema(description = "Caminho completo delimitado da raiz da subárvore; deve terminar em barra para não capturar prefixos irmãos.")
  @UISchema(label = "Subárvore", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
 @Filterable(operation=FilterOperation.STARTS_WITH,relation="caminhoHierarquico")
 private String caminhoSubarvore;
  @Schema(description = "Seleciona nós sem informação registrada nesta dimensão na data de referência.")
  @UISchema(label = "Comando não apurado", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.BOOLEAN, controlType = FieldControlType.CHECKBOX, icon = "check_circle", preset = UISchemaPreset.BOOLEAN_FLAG, labelTrue = "Sim", labelFalse = "Não", toneTrue = "info", toneFalse = "neutral")
 @Filterable(operation=FilterOperation.IS_NULL,relation="situacaoComandoSetor")
 private Boolean semComandoApurado;
  @Schema(description = "Seleciona nós sem informação registrada nesta dimensão na data de referência.")
  @UISchema(label = "Sem centro de custo", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.BOOLEAN, controlType = FieldControlType.CHECKBOX, icon = "check_circle", preset = UISchemaPreset.BOOLEAN_FLAG, labelTrue = "Sim", labelFalse = "Não", toneTrue = "info", toneFalse = "neutral")
 @Filterable(operation=FilterOperation.IS_NULL,relation="centroCusto")
 private Boolean semCentroCusto;
  @Schema(description = "Seleciona nós sem informação registrada nesta dimensão na data de referência.")
  @UISchema(label = "Raízes", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.BOOLEAN, controlType = FieldControlType.CHECKBOX, icon = "check_circle", preset = UISchemaPreset.BOOLEAN_FLAG, labelTrue = "Sim", labelFalse = "Não", toneTrue = "info", toneFalse = "neutral")
 @Filterable(operation=FilterOperation.IS_NULL,relation="setorPai")
 private Boolean semSuperior;
}
