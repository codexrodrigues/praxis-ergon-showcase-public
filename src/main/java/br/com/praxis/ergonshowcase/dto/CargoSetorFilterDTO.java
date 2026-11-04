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
@Schema(description = "Pesquisa operacional de cargos e quadro por setor, por órgão, vigência e características administrativas.")
public class CargoSetorFilterDTO implements GenericFilterDTO {
  @Schema(description = "Órgão responsável pelo assentamento; o contexto autenticado limita as consultas independentemente deste valor.")
  @ResourceReference(resourceKey = "administracao-pessoal.empresas", valueField = "empCodigo", surfaceId = "ficha-institucional", previewSurfaceId = "ficha-institucional", detailPresentation = "drawer")
  @UISchema(label = "Órgão", group = "Vigência e fundamento", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, controlType = FieldControlType.ENTITY_LOOKUP, icon = "account_tree", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.ORGAO_OPTIONS, valueField = "id", displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL, relation = "empCodigo")
  private Integer empCodigo;
  @Schema(description = "Código da unidade titular da autorização de cargo e da reserva ou distribuição de vagas.")
  @ResourceReference(resourceKey = "administracao-pessoal.setores", valueField = "setor", surfaceId = "ficha-unidade", previewSurfaceId = "ficha-unidade", detailPresentation = "drawer")
  @UISchema(label = "Unidade", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, controlType = FieldControlType.ENTITY_LOOKUP, icon = "account_tree", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.SETOR_OPTIONS, valueField = "id", displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL, relation = "setor")
  private String setor;
  @Schema(description = "Denominação da unidade referenciada no período do assentamento.")
  @UISchema(label = "Nome da unidade", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  @Filterable(operation = FilterOperation.LIKE, relation = "setorNome")
  private String setorNome;
  @Schema(description = "Cargo do mesmo órgão autorizado a compor a força de trabalho desta unidade no período.")
  @ResourceReference(resourceKey = "administracao-pessoal.cargos", valueField = "cargoId", surfaceId = "ficha-cargo", previewSurfaceId = "ficha-cargo", detailPresentation = "drawer")
  @UISchema(label = "Cargo autorizado", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, controlType = FieldControlType.ENTITY_LOOKUP, icon = "account_tree", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.CARGO_OPTIONS, valueField = "id", displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL, relation = "cargoId")
  private Integer cargoId;
  @Schema(description = "Denominação do cargo autorizado no mesmo órgão.")
  @UISchema(label = "Nome do cargo", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  @Filterable(operation = FilterOperation.LIKE, relation = "cargoNome")
  private String cargoNome;
  @Schema(description = "Inclui a subárvore na reserva de quadro, equivalente ao SUBORD do legado; não soma cotas filhas automaticamente.")
  @UISchema(label = "Abrange subordinados", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.BOOLEAN, controlType = FieldControlType.CHECKBOX, icon = "check_circle", preset = UISchemaPreset.BOOLEAN_FLAG, labelTrue = "Sim", labelFalse = "Não", toneTrue = "info", toneFalse = "neutral")
  @Filterable(operation = FilterOperation.EQUAL, relation = "abrangeSubordinados")
  private Boolean abrangeSubordinados;
  @Schema(description = "Distingue reserva da unidade de distribuição derivada de uma reserva superior.")
  @UISchema(label = "Controle de quadro", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  @Filterable(operation = FilterOperation.EQUAL, relation = "tipoControleQuadro")
  private String tipoControleQuadro;
  @Schema(description = "Autorização superior do mesmo órgão da qual esta distribuição recebe sua cota.")
  @ResourceReference(resourceKey = "administracao-pessoal.cargos-por-setor", valueField = "quadroPaiId", surfaceId = "detail", previewSurfaceId = "detail", detailPresentation = "drawer")
  @UISchema(label = "Reserva de origem", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, controlType = FieldControlType.ENTITY_LOOKUP, icon = "account_tree", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.CARGO_SETOR_OPTIONS, valueField = "id", displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL, relation = "quadroPaiId")
  private Long quadroPaiId;
  @Schema(description = "Limite quantitativo de ocupação autorizado para o cargo no setor ou na subárvore abrangida.")
  @UISchema(label = "Vagas autorizadas", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.NUMBER, icon = "numbers")
  @Filterable(operation = FilterOperation.GREATER_OR_EQUAL, relation = "lotacaoMaximaAutorizada")
  private Integer lotacaoMaximaAutorizada;
  @Schema(description = "Ocupação calculada pela integração de provimentos; nulo significa ainda não apurada e não equivale a zero.")
  @UISchema(label = "Vagas ocupadas", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.NUMBER, icon = "numbers")
  @Filterable(operation = FilterOperation.GREATER_OR_EQUAL, relation = "lotacaoOcupadaAtual")
  private Integer lotacaoOcupadaAtual;
  @Schema(description = "Percentual de adicional de insalubridade da parametrização; não comprova concessão individual ou pagamento.")
  @UISchema(label = "Insalubridade (%)", controlType = FieldControlType.INPUT, group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.NUMBER, icon = "numbers")
  @Filterable(operation = FilterOperation.GREATER_OR_EQUAL, relation = "percInsalubridade")
  private BigDecimal percInsalubridade;
  @Schema(description = "Percentual de adicional de periculosidade da parametrização; sua incidência individual depende das regras de concessão.")
  @UISchema(label = "Periculosidade (%)", controlType = FieldControlType.INPUT, group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.NUMBER, icon = "numbers")
  @Filterable(operation = FilterOperation.GREATER_OR_EQUAL, relation = "percPericulosidade")
  private BigDecimal percPericulosidade;
  @Schema(description = "Indica que a ocupação desta autorização depende de habilitação profissional específica.")
  @UISchema(label = "Exige habilitação", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.BOOLEAN, controlType = FieldControlType.CHECKBOX, icon = "check_circle", preset = UISchemaPreset.BOOLEAN_FLAG, labelTrue = "Sim", labelFalse = "Não", toneTrue = "info", toneFalse = "neutral")
  @Filterable(operation = FilterOperation.EQUAL, relation = "exigeHabilitacaoEspecifica")
  private Boolean exigeHabilitacaoEspecifica;
  @Schema(description = "Seleciona os períodos que contêm esta data, incluindo os dois limites; término ausente permanece aberto.")
  @UISchema(label = "Em vigor na data", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, icon = "event", preset = UISchemaPreset.DATE_RANGE)
  @Filterable(operation = FilterOperation.EFFECTIVE_ON_DATE, relation = "dtini", endRelation = "dtfim")
  private LocalDate dataReferencia;
  @Schema(description = "Pesquisa conjunta por identificação e denominação administrativa, sem exigir o conhecimento de ids técnicos.")
  @UISchema(label = "Busca ampla", group = "Configuração organizacional", tableHidden = true, readOnly = false, type = FieldDataType.TEXT, icon = "description")
  @Filterable(operation = FilterOperation.LIKE, relations = {"cargoNome","setor","setorNome"})
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
  @org.praxisplatform.uischema.customfields.CustomFields(resourceKey=ApiPaths.KEY_CARGOS_POR_SETOR)
  @UISchema(label="Atributos do órgão",group="Pesquisa",icon="extension")
  private Map<String,Object> customFields;
}
