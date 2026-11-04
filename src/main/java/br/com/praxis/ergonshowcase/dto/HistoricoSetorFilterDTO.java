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
@Schema(description = "Pesquisa operacional de históricos de setores, por órgão, vigência e características administrativas.")
public class HistoricoSetorFilterDTO implements GenericFilterDTO {
  @Schema(description="Trecho da denominação oficial da unidade durante o período histórico pesquisado.")
  @UISchema(label="Nome da unidade",group="Pesquisa",icon="apartment")
  @Filterable(operation=FilterOperation.LIKE,relation="nome")
  private String nome;
  @Schema(description="Caminho completo e delimitado da unidade raiz da subárvore na hierarquia e data selecionadas.")
  @UISchema(label="Subárvore",group="Organograma",icon="account_tree")
  @Filterable(operation=FilterOperation.STARTS_WITH,relation="caminhoHierarquico")
  private String caminhoSubarvore;
  @Schema(description="Zero seleciona a hierarquia legal; um a quatro selecionam as hierarquias funcionais alternativas.")
  @UISchema(label="Hierarquia",group="Organograma",icon="account_tree")
  private Integer tipoHierarquia;
  @Schema(description="Quando verdadeiro inclui descendentes do caminho escolhido; falso restringe ao nó raiz.")
  @UISchema(label="Incluir subordinados",group="Organograma",preset=UISchemaPreset.BOOLEAN_FLAG)
  private Boolean incluirSubordinados;
  @Schema(description = "Órgão responsável pelo assentamento; o contexto autenticado limita as consultas independentemente deste valor.")
  @ResourceReference(resourceKey = "administracao-pessoal.empresas", valueField = "empCodigo", surfaceId = "ficha-institucional", previewSurfaceId = "ficha-institucional", detailPresentation = "drawer")
  @UISchema(label = "Órgão", group = "Vigência e fundamento", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, controlType = FieldControlType.ENTITY_LOOKUP, icon = "account_tree", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.ORGAO_OPTIONS, valueField = "id", displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL, relation = "empCodigo")
  private Integer empCodigo;
  @Schema(description = "Identidade administrativa da unidade dentro do órgão; diferentes vigências conservam este código e possuem ids próprios.")
  @ResourceReference(resourceKey = "administracao-pessoal.setores", valueField = "setor", surfaceId = "ficha-unidade", previewSurfaceId = "ficha-unidade", detailPresentation = "drawer")
  @UISchema(label = "Código do setor", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, controlType = FieldControlType.ENTITY_LOOKUP, icon = "account_tree", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.SETOR_OPTIONS, valueField = "id", displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL, relation = "setor")
  private String setor;
  @Schema(description = "Abreviação institucional da unidade usada no organograma durante esta vigência.")
  @UISchema(label = "Sigla", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  @Filterable(operation = FilterOperation.STARTS_WITH, relation = "sigla")
  private String sigla;
  @Schema(description = "Natureza administrativa e atividade-fim da unidade, como diretoria, gabinete ou unidade de saúde.")
  @UISchema(label = "Tipo de unidade", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  @Filterable(operation = FilterOperation.EQUAL, relation = "tipoSetor")
  private String tipoSetor;
  @Schema(description = "Código da unidade imediatamente superior na hierarquia legal, resolvido no mesmo órgão e na data consultada.")
  @ResourceReference(resourceKey = "administracao-pessoal.setores", valueField = "setorPai", surfaceId = "ficha-unidade", previewSurfaceId = "ficha-unidade", detailPresentation = "drawer")
  @UISchema(label = "Superior oficial", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, controlType = FieldControlType.ENTITY_LOOKUP, icon = "account_tree", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.SETOR_OPTIONS, valueField = "id", displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL, relation = "setorPai")
  private String setorPai;
  @Schema(description = "Código da unidade empregadora fiscal da lotação; sua identidade inclui o órgão e a vigência.")
  @UISchema(label = "Subempresa", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  @Filterable(operation = FilterOperation.EQUAL, relation = "subempCodigo")
  private String subempCodigo;
  @Schema(description = "Código da regionalização, comarca ou distrito associado à unidade; distingue localização administrativa de endereço.")
  @UISchema(label = "Localidade", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  @Filterable(operation = FilterOperation.EQUAL, relation = "localidadeCodigo")
  private String localidadeCodigo;
  @Schema(description = "Período do agrupamento transversal ao qual esta unidade pertence, no mesmo órgão.")
  @ResourceReference(resourceKey = "administracao-pessoal.grupos-setores", valueField = "grupoSetorId", surfaceId = "detail", previewSurfaceId = "detail", detailPresentation = "drawer")
  @UISchema(label = "Grupo de setores", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, controlType = FieldControlType.ENTITY_LOOKUP, icon = "account_tree", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.GRUPO_SETOR_OPTIONS, valueField = "id", displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL, relation = "grupoSetorId")
  private Long grupoSetorId;
  @Schema(description = "Marca a unidade desativada para novas lotações; preserva referências e histórico anteriores à extinção.")
  @UISchema(label = "Extinto", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.BOOLEAN, controlType = FieldControlType.CHECKBOX, icon = "check_circle", preset = UISchemaPreset.BOOLEAN_FLAG, labelTrue = "Sim", labelFalse = "Não", toneTrue = "info", toneFalse = "neutral")
  @Filterable(operation = FilterOperation.EQUAL, relation = "extinto")
  private Boolean extinto;
  @Schema(description = "Município da instalação física da unidade; não corresponde ao domicílio dos servidores lotados.")
  @UISchema(label = "Município da unidade", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  @Filterable(operation = FilterOperation.LIKE, relation = "municipioLotacao")
  private String municipioLotacao;
  @Schema(description = "Unidade federativa do endereço da unidade durante a vigência.")
  @UISchema(label = "UF da unidade", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  @Filterable(operation = FilterOperation.EQUAL, relation = "ufLotacao")
  private String ufLotacao;
  @Schema(description = "Razão administrativa da abertura deste período, como criação, reorganização ou mudança de endereço.")
  @UISchema(label = "Motivo da alteração", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  @Filterable(operation = FilterOperation.EQUAL, relation = "motivoReestruturacao")
  private String motivoReestruturacao;
  @Schema(description = "Profundidade a partir da raiz, cujo nível é um; depende da hierarquia e da data de referência.")
  @UISchema(label = "Nível hierárquico", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.NUMBER, icon = "numbers")
  @Filterable(operation = FilterOperation.GREATER_OR_EQUAL, relation = "nivelHierarquico")
  private Integer nivelHierarquico;
  @Schema(description = "Seleciona os períodos que contêm esta data, incluindo os dois limites; término ausente permanece aberto.")
  @UISchema(label = "Em vigor na data", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, icon = "event", preset = UISchemaPreset.DATE_RANGE)
  @Filterable(operation = FilterOperation.EFFECTIVE_ON_DATE, relation = "dtini", endRelation = "dtfim")
  private LocalDate dataReferencia;
  @Schema(description = "Pesquisa conjunta por identificação e denominação administrativa, sem exigir o conhecimento de ids técnicos.")
  @UISchema(label = "Busca ampla", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  @Filterable(operation = FilterOperation.LIKE, relations = {"setor","nome","sigla","municipioLotacao"})
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
  @org.praxisplatform.uischema.customfields.CustomFields(resourceKey=ApiPaths.KEY_HISTORICO_SETORES)
  @UISchema(label="Atributos do órgão",group="Pesquisa",icon="extension")
  private Map<String,Object> customFields;
}
