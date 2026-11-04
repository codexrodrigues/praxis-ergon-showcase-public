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
@Getter @Setter
@Schema(description = "Pesquisa operacional de grupos de setores, por órgão, vigência e características administrativas.")
public class GrupoSetorFilterDTO implements GenericFilterDTO {
  @Schema(description = "Órgão responsável pelo assentamento; o contexto autenticado limita as consultas independentemente deste valor.")
  @ResourceReference(resourceKey = "administracao-pessoal.empresas", valueField = "empCodigo", surfaceId = "ficha-institucional", previewSurfaceId = "ficha-institucional", detailPresentation = "drawer")
  @UISchema(label = "Órgão", group = "Vigência e fundamento", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, controlType = FieldControlType.ENTITY_LOOKUP, icon = "account_tree", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.ORGAO_OPTIONS, valueField = "id", displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL, relation = "empCodigo")
  private Integer empCodigo;
  @Schema(description = "Identificador abreviado do agrupamento transversal no órgão e no período.")
  @UISchema(label = "Sigla do grupo", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  @Filterable(operation = FilterOperation.STARTS_WITH, relation = "sigla")
  private String sigla;
  @Schema(description = "Descrição institucional do agrupamento de setores para uso administrativo coletivo.")
  @UISchema(label = "Nome do grupo", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  @Filterable(operation = FilterOperation.LIKE, relation = "nome")
  private String nome;
  @Schema(description = "Finalidade operacional do agrupamento, como cálculo de folha, regionalização, custo ou comissão de SST.")
  @UISchema(label = "Finalidade do grupo", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  @Filterable(operation = FilterOperation.EQUAL, relation = "tipoAgrupamento")
  private String tipoAgrupamento;
  @Schema(description = "Unidade que coordena o agrupamento dentro do órgão.")
  @ResourceReference(resourceKey = "administracao-pessoal.setores", valueField = "setorSede", surfaceId = "ficha-unidade", previewSurfaceId = "ficha-unidade", detailPresentation = "drawer")
  @UISchema(label = "Setor sede", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, controlType = FieldControlType.ENTITY_LOOKUP, icon = "account_tree", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.SETOR_OPTIONS, valueField = "id", displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL, relation = "setorSede")
  private String setorSede;
  @Schema(description = "Código da unidade empregadora à qual o agrupamento está associado.")
  @UISchema(label = "Subempresa vinculada", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  @Filterable(operation = FilterOperation.EQUAL, relation = "subempCodigoVinculada")
  private String subempCodigoVinculada;
  @Schema(description = "Unidade federativa abrangida pelo grupo quando houver delimitação territorial.")
  @UISchema(label = "UF de abrangência", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  @Filterable(operation = FilterOperation.EQUAL, relation = "ufAbrangencia")
  private String ufAbrangencia;
  @Schema(description = "Município da coordenação administrativa do agrupamento.")
  @UISchema(label = "Município sede", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  @Filterable(operation = FilterOperation.LIKE, relation = "municipioSede")
  private String municipioSede;
  @Schema(description = "Indica habilitação administrativa do grupo; sua aplicação também exige período vigente.")
  @UISchema(label = "Ativo", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.BOOLEAN, controlType = FieldControlType.CHECKBOX, icon = "check_circle", preset = UISchemaPreset.BOOLEAN_FLAG, labelTrue = "Sim", labelFalse = "Não", toneTrue = "info", toneFalse = "neutral")
  @Filterable(operation = FilterOperation.EQUAL, relation = "ativo")
  private Boolean ativo;
  @Schema(description = "Seleciona os períodos que contêm esta data, incluindo os dois limites; término ausente permanece aberto.")
  @UISchema(label = "Em vigor na data", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, icon = "event", preset = UISchemaPreset.DATE_RANGE)
  @Filterable(operation = FilterOperation.EFFECTIVE_ON_DATE, relation = "dtini", endRelation = "dtfim")
  private LocalDate dataReferencia;
  @Schema(description = "Pesquisa conjunta por identificação e denominação administrativa, sem exigir o conhecimento de ids técnicos.")
  @UISchema(label = "Busca ampla", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  @Filterable(operation = FilterOperation.LIKE, relations = {"nome","sigla"})
  private String buscaAmpla;
  @Schema(description = "Seleciona períodos sem data final registrada; não implica vigência na data atual.")
  @UISchema(label = "Sem encerramento", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.BOOLEAN, controlType = FieldControlType.CHECKBOX, icon = "check_circle", preset = UISchemaPreset.BOOLEAN_FLAG, labelTrue = "Sim", labelFalse = "Não", toneTrue = "info", toneFalse = "neutral")
  @Filterable(operation = FilterOperation.IS_NULL, relation = "dtfim")
  private Boolean semEncerramento;
  @Schema(description = "Seleciona períodos com data final registrada, inclusive encerramentos programados.")
  @UISchema(label = "Com encerramento", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.BOOLEAN, controlType = FieldControlType.CHECKBOX, icon = "check_circle", preset = UISchemaPreset.BOOLEAN_FLAG, labelTrue = "Sim", labelFalse = "Não", toneTrue = "info", toneFalse = "neutral")
  @Filterable(operation = FilterOperation.IS_NOT_NULL, relation = "dtfim")
  private Boolean comEncerramento;
  @Schema(description = "Localiza configurações sem referência ao documento que fundamenta a alteração.")
  @UISchema(label = "Sem ato legal", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.BOOLEAN, controlType = FieldControlType.CHECKBOX, icon = "check_circle", preset = UISchemaPreset.BOOLEAN_FLAG, labelTrue = "Sim", labelFalse = "Não", toneTrue = "info", toneFalse = "neutral")
  @Filterable(operation = FilterOperation.IS_NULL, relation = "documentoLegalId")
  private Boolean semFundamento;
  @Schema(description = "Janela relativa de dias para identificar configurações com término próximo.")
  @UISchema(label = "Vencem em dias", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.NUMBER, icon = "numbers")
  @Filterable(operation = FilterOperation.IN_NEXT_DAYS, relation = "dtfim")
  private Integer encerramEmDias;
  @Schema(description = "Janela relativa de dias para investigar configurações recentemente iniciadas.")
  @UISchema(label = "Iniciados em dias", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.NUMBER, icon = "numbers")
  @Filterable(operation = FilterOperation.IN_LAST_DAYS, relation = "dtini")
  private Integer iniciadosEmDias;
  @Schema(description = "Localiza agrupamentos cuja referência administrativa ainda não foi informada.")
  @UISchema(label = "Sem setor sede", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.BOOLEAN, controlType = FieldControlType.CHECKBOX, icon = "check_circle", preset = UISchemaPreset.BOOLEAN_FLAG, labelTrue = "Sim", labelFalse = "Não", toneTrue = "info", toneFalse = "neutral")
  @Filterable(operation = FilterOperation.IS_NULL, relation = "setorSede")
  private Boolean semSede;
  @Schema(description = "Localiza agrupamentos cuja referência administrativa ainda não foi informada.")
  @UISchema(label = "Sem subempresa", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.BOOLEAN, controlType = FieldControlType.CHECKBOX, icon = "check_circle", preset = UISchemaPreset.BOOLEAN_FLAG, labelTrue = "Sim", labelFalse = "Não", toneTrue = "info", toneFalse = "neutral")
  @Filterable(operation = FilterOperation.IS_NULL, relation = "subempCodigoVinculada")
  private Boolean semSubempresa;
  @Schema(description="Pesquisa atributos adicionais publicados no catálogo deste recurso e órgão; chaves desconhecidas ou sem permissão são rejeitadas pelo contrato canônico.")
  @org.praxisplatform.uischema.customfields.CustomFields(resourceKey=ApiPaths.KEY_GRUPOS_SETORES)
  @UISchema(label="Atributos do órgão",group="Pesquisa",icon="extension")
  private Map<String,Object> customFields;
}
