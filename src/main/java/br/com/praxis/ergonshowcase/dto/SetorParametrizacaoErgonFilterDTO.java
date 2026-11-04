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
@Schema(description = "Pesquisa operacional de parâmetros funcionais de setores, por órgão, vigência e características administrativas.")
public class SetorParametrizacaoErgonFilterDTO implements GenericFilterDTO {
  @Schema(description = "Órgão responsável pelo assentamento; o contexto autenticado limita as consultas independentemente deste valor.")
  @ResourceReference(resourceKey = "administracao-pessoal.empresas", valueField = "empCodigo", surfaceId = "ficha-institucional", previewSurfaceId = "ficha-institucional", detailPresentation = "drawer")
  @UISchema(label = "Órgão", group = "Vigência e fundamento", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, controlType = FieldControlType.ENTITY_LOOKUP, icon = "account_tree", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.ORGAO_OPTIONS, valueField = "id", displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL, relation = "empCodigo")
  private Integer empCodigo;
  @Schema(description = "Código da unidade cujas regras funcionais e financeiras vigoram neste período.")
  @ResourceReference(resourceKey = "administracao-pessoal.setores", valueField = "setor", surfaceId = "ficha-unidade", previewSurfaceId = "ficha-unidade", detailPresentation = "drawer")
  @UISchema(label = "Unidade", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, controlType = FieldControlType.ENTITY_LOOKUP, icon = "account_tree", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.SETOR_OPTIONS, valueField = "id", displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL, relation = "setor")
  private String setor;
  @Schema(description = "Denominação da unidade referenciada no período do assentamento.")
  @UISchema(label = "Nome da unidade", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  @Filterable(operation = FilterOperation.LIKE, relation = "setorNome")
  private String setorNome;
  @Schema(description = "Cargo cujo ocupante pode exercer a titularidade do comando; alternativa exclusiva à função de chefia.")
  @ResourceReference(resourceKey = "administracao-pessoal.cargos", valueField = "cargoChefeId", surfaceId = "ficha-cargo", previewSurfaceId = "ficha-cargo", detailPresentation = "drawer")
  @UISchema(label = "Cargo de chefia", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, controlType = FieldControlType.ENTITY_LOOKUP, icon = "account_tree", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.CARGO_OPTIONS, valueField = "id", displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL, relation = "cargoChefeId")
  private Integer cargoChefeId;
  @Schema(description = "Função de confiança cuja designação identifica o titular; alternativa exclusiva ao cargo de chefia.")
  @ResourceReference(resourceKey = "administracao-pessoal.funcoes", valueField = "funcaoChefeId", surfaceId = "detail", previewSurfaceId = "detail", detailPresentation = "drawer")
  @UISchema(label = "Função de chefia", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, controlType = FieldControlType.ENTITY_LOOKUP, icon = "account_tree", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.FUNCAO_OPTIONS, valueField = "id", displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL, relation = "funcaoChefeId")
  private Long funcaoChefeId;
  @Schema(description = "Classificação funcional e financeira do setor utilizada pelas rotinas de pessoal.")
  @UISchema(label = "Tipo funcional", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  @Filterable(operation = FilterOperation.EQUAL, relation = "tipoSetor")
  private String tipoSetor;
  @Schema(description = "Classificação das condições de acesso ao local de trabalho, distinta de permissão de acesso ao sistema.")
  @UISchema(label = "Tipo de acesso", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  @Filterable(operation = FilterOperation.EQUAL, relation = "tipoAcesso")
  private String tipoAcesso;
  @Schema(description = "Centro de apropriação contábil associado ao setor nesta vigência; não substitui a resolução do setor de pagamento do vínculo.")
  @UISchema(label = "Centro de custo", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  @Filterable(operation = FilterOperation.LIKE, relation = "centroCusto")
  private String centroCusto;
  @Schema(description = "Natureza da classificação usada para organizar o processamento de folha do setor.")
  @UISchema(label = "Tipo de classificação", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  @Filterable(operation = FilterOperation.EQUAL, relation = "tipoClassifFolha")
  private String tipoClassifFolha;
  @Schema(description = "Código de enquadramento do setor dentro do tipo de classificação da folha.")
  @UISchema(label = "Classificação de folha", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  @Filterable(operation = FilterOperation.EQUAL, relation = "classifFolha")
  private String classifFolha;
  @Schema(description = "Classificação de risco ambiental adotada no showcase para inspeção de SST; não representa laudo individual.")
  @UISchema(label = "Grau de risco", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  @Filterable(operation = FilterOperation.EQUAL, relation = "grauRiscoAmbiental")
  private String grauRiscoAmbiental;
  @Schema(description = "Indica exigência administrativa de equipamento de proteção para atividades no ambiente.")
  @UISchema(label = "Exige EPI", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.BOOLEAN, controlType = FieldControlType.CHECKBOX, icon = "check_circle", preset = UISchemaPreset.BOOLEAN_FLAG, labelTrue = "Sim", labelFalse = "Não", toneTrue = "info", toneFalse = "neutral")
  @Filterable(operation = FilterOperation.EQUAL, relation = "exigeEpiObrigatorio")
  private Boolean exigeEpiObrigatorio;
  @Schema(description = "Entidade sindical de referência da parametrização funcional da unidade.")
  @UISchema(label = "Sindicato", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  @Filterable(operation = FilterOperation.LIKE, relation = "sindicato")
  private String sindicato;
  @Schema(description = "Seleciona os períodos que contêm esta data, incluindo os dois limites; término ausente permanece aberto.")
  @UISchema(label = "Em vigor na data", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, icon = "event", preset = UISchemaPreset.DATE_RANGE)
  @Filterable(operation = FilterOperation.EFFECTIVE_ON_DATE, relation = "dtini", endRelation = "dtfim")
  private LocalDate dataReferencia;
  @Schema(description = "Pesquisa conjunta por identificação e denominação administrativa, sem exigir o conhecimento de ids técnicos.")
  @UISchema(label = "Busca ampla", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  @Filterable(operation = FilterOperation.LIKE, relations = {"setorNome","setor"})
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
  @org.praxisplatform.uischema.customfields.CustomFields(resourceKey=ApiPaths.KEY_SETORES_ERGON)
  @UISchema(label="Atributos do órgão",group="Pesquisa",icon="extension")
  private Map<String,Object> customFields;
}
