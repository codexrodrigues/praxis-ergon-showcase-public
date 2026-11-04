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
@Schema(description = "Pesquisa operacional de subempresas e localidades, por órgão, vigência e características administrativas.")
public class SubempresaLocalidadeFilterDTO implements GenericFilterDTO {
  @Schema(description = "Órgão responsável pelo assentamento; o contexto autenticado limita as consultas independentemente deste valor.")
  @ResourceReference(resourceKey = "administracao-pessoal.empresas", valueField = "empCodigo", surfaceId = "ficha-institucional", previewSurfaceId = "ficha-institucional", detailPresentation = "drawer")
  @UISchema(label = "Órgão", group = "Vigência e fundamento", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, controlType = FieldControlType.ENTITY_LOOKUP, icon = "account_tree", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.ORGAO_OPTIONS, valueField = "id", displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL, relation = "empCodigo")
  private Integer empCodigo;
  @Schema(description = "Código administrativo da subempresa ou localidade; a identidade inclui órgão, natureza e vigência.")
  @UISchema(label = "Código da unidade", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  @Filterable(operation = FilterOperation.EQUAL, relation = "codigo")
  private String codigo;
  @Schema(description = "Nome administrativo da unidade empregadora ou da localidade regional.")
  @UISchema(label = "Nome", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  @Filterable(operation = FilterOperation.LIKE, relation = "nome")
  private String nome;
  @Schema(description = "Distingue empregador fiscal, localidade regional ou unidade mista no modelo do showcase.")
  @UISchema(label = "Natureza da unidade", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  @Filterable(operation = FilterOperation.EQUAL, relation = "naturezaUnidade")
  private String naturezaUnidade;
  @Schema(description = "Inscrição fiscal da unidade empregadora; localidades sem personalidade fiscal podem não possuir CNPJ.")
  @UISchema(label = "CNPJ", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  @Filterable(operation = FilterOperation.EQUAL, relation = "cnpj")
  private String cnpj;
  @Schema(description = "Inscrição responsável pela declaração das informações da unidade aos órgãos oficiais e ao eSocial.")
  @UISchema(label = "CNPJ declarante", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  @Filterable(operation = FilterOperation.EQUAL, relation = "cnpjDeclaranteEsocial")
  private String cnpjDeclaranteEsocial;
  @Schema(description = "Código do setor que sedia administrativamente esta subempresa ou localidade.")
  @ResourceReference(resourceKey = "administracao-pessoal.setores", valueField = "setorSede", surfaceId = "ficha-unidade", previewSurfaceId = "ficha-unidade", detailPresentation = "drawer")
  @UISchema(label = "Setor sede", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, controlType = FieldControlType.ENTITY_LOOKUP, icon = "account_tree", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.SETOR_OPTIONS, valueField = "id", displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL, relation = "setorSede")
  private String setorSede;
  @Schema(description = "Código da unidade empregadora superior dentro do mesmo órgão.")
  @UISchema(label = "Subempresa superior", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  @Filterable(operation = FilterOperation.EQUAL, relation = "subempresaPaiCodigo")
  private String subempresaPaiCodigo;
  @Schema(description = "Classificação da regionalização institucional, como comarca, distrito ou regional.")
  @UISchema(label = "Tipo de localidade", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  @Filterable(operation = FilterOperation.EQUAL, relation = "tipoLocalidade")
  private String tipoLocalidade;
  @Schema(description = "Região de abrangência territorial da localidade administrativa.")
  @UISchema(label = "Região", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  @Filterable(operation = FilterOperation.LIKE, relation = "regiaoGeografica")
  private String regiaoGeografica;
  @Schema(description = "Classificação da comarca na organização judiciária, quando aplicável.")
  @UISchema(label = "Entrância", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  @Filterable(operation = FilterOperation.EQUAL, relation = "entranciaComarca")
  private String entranciaComarca;
  @Schema(description = "Unidade federativa da sede da unidade empregadora ou localidade.")
  @UISchema(label = "UF", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  @Filterable(operation = FilterOperation.EQUAL, relation = "uf")
  private String uf;
  @Schema(description = "Município sede da subempresa ou localidade administrativa.")
  @UISchema(label = "Município", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  @Filterable(operation = FilterOperation.LIKE, relation = "municipio")
  private String municipio;
  @Schema(description = "Indica se a unidade está habilitada administrativamente, além da verificação de sua vigência.")
  @UISchema(label = "Ativa", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.BOOLEAN, controlType = FieldControlType.CHECKBOX, icon = "check_circle", preset = UISchemaPreset.BOOLEAN_FLAG, labelTrue = "Sim", labelFalse = "Não", toneTrue = "info", toneFalse = "neutral")
  @Filterable(operation = FilterOperation.EQUAL, relation = "ativo")
  private Boolean ativo;
  @Schema(description = "Seleciona os períodos que contêm esta data, incluindo os dois limites; término ausente permanece aberto.")
  @UISchema(label = "Em vigor na data", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, icon = "event", preset = UISchemaPreset.DATE_RANGE)
  @Filterable(operation = FilterOperation.EFFECTIVE_ON_DATE, relation = "dtini", endRelation = "dtfim")
  private LocalDate dataReferencia;
  @Schema(description = "Pesquisa conjunta por identificação e denominação administrativa, sem exigir o conhecimento de ids técnicos.")
  @UISchema(label = "Busca ampla", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  @Filterable(operation = FilterOperation.LIKE, relations = {"nome","codigo"})
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
  @Schema(description="Pesquisa atributos adicionais publicados no catálogo deste recurso e órgão; chaves desconhecidas ou sem permissão são rejeitadas pelo contrato canônico.")
  @org.praxisplatform.uischema.customfields.CustomFields(resourceKey=ApiPaths.KEY_SUBEMPRESAS_LOCALIDADES)
  @UISchema(label="Atributos do órgão",group="Pesquisa",icon="extension")
  private Map<String,Object> customFields;
}
