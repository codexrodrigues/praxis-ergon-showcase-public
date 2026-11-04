package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.filter.annotation.Filterable;
import org.praxisplatform.uischema.filter.annotation.Filterable.FilterOperation;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;

@Getter
@Setter
@Schema(
    description =
        "Critérios de consulta do cadastro auxiliar para inspeção de referências funcionais.")
public class SetorFilterDTO implements GenericFilterDTO {
  @Schema(description="Data da composição histórica procurada; a consulta seleciona historico_setores, não a linha corrente do diretório.")
  @UISchema(label="Data de referência",group="Organograma",type=org.praxisplatform.uischema.FieldDataType.DATE,controlType=org.praxisplatform.uischema.FieldControlType.DATE_PICKER,icon="event")
  @Filterable(operation=FilterOperation.EFFECTIVE_ON_DATE,relation="dtini",endRelation="dtfim")
  @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
  private java.time.LocalDate dataReferencia;
  @Schema(description="Zero para hierarquia oficial; um a quatro para hierarquias alternativas na data consultada.")
  @UISchema(label="Hierarquia",group="Organograma",icon="account_tree")
  private Integer tipoHierarquia;
  @Schema(description="Caminho delimitado do nó raiz na data e hierarquia consultadas; não aceita prefixo incompleto de código.")
  @UISchema(label="Subárvore",group="Organograma",icon="account_tree")
  @Filterable(operation=FilterOperation.STARTS_WITH,relation="caminhoHierarquico")
  private String caminhoSubarvore;
  @Schema(description="Inclui descendentes da subárvore selecionada; falso restringe ao nó raiz.")
  @UISchema(label="Incluir subordinados",group="Organograma")
  private Boolean incluirSubordinados;
  @Schema(description="Pesquisa conjunta pelo código, nome, sigla e município da unidade organizacional.")
  @UISchema(label="Busca ampla",group="Pesquisa")
  @Filterable(operation=FilterOperation.LIKE,relations={"setor","nome","sigla","municipioLotacao"})
  private String buscaAmpla;
  @Schema(description="Código administrativo exato da unidade dentro do órgão selecionado.")
  @UISchema(label="Código da unidade",group="Pesquisa")
  @Filterable(operation=FilterOperation.EQUAL,relation="setor")
  private String setor;
  @Schema(description="Código da unidade empregadora fiscal associada à lotação.")
  @UISchema(label="Subempresa",group="Pesquisa")
  @Filterable(operation=FilterOperation.EQUAL,relation="subempCodigo")
  private String subempCodigo;
  @Schema(description="Código da regionalização ou comarca associada à unidade.")
  @UISchema(label="Localidade",group="Pesquisa")
  @Filterable(operation=FilterOperation.EQUAL,relation="localidadeCodigo")
  private String localidadeCodigo;
  @Schema(description="Seleciona unidades administrativamente extintas ou habilitadas no período.")
  @UISchema(label="Extinto",group="Pesquisa")
  @Filterable(operation=FilterOperation.EQUAL,relation="extinto")
  private Boolean extinto;
  @Schema(description="Trecho do município de instalação da unidade no período.")
  @UISchema(label="Município",group="Pesquisa")
  @Filterable(operation=FilterOperation.LIKE,relation="municipioLotacao")
  private String municipioLotacao;
  @Schema(description="Unidade federativa da localização da unidade.")
  @UISchema(label="UF",group="Pesquisa")
  @Filterable(operation=FilterOperation.EQUAL,relation="ufLotacao")
  private String ufLotacao;
  @Schema(description="Profundidade mínima a investigar na hierarquia selecionada.")
  @UISchema(label="Nível mínimo",group="Organograma")
  @Filterable(operation=FilterOperation.GREATER_OR_EQUAL,relation="nivelHierarquico")
  private Integer nivelHierarquico;
  @Schema(description="Localiza unidades sem documento legal associado à sua configuração.")
  @UISchema(label="Sem fundamento legal",group="Pesquisa")
  @Filterable(operation=FilterOperation.IS_NULL,relation="documentoLegalId")
  private Boolean semFundamento;
  @Schema(description="Janela em dias para pesquisar configurações com término próximo.")
  @UISchema(label="Vencem em dias",group="Pesquisa")
  @Filterable(operation=FilterOperation.IN_NEXT_DAYS,relation="dtfim")
  private Integer encerramEmDias;
  @Schema(description = "Trecho da denominação oficial da unidade organizacional.")
  @UISchema(label = "Nome da unidade", group = "Pesquisa")
  @Filterable(operation = FilterOperation.LIKE, relation = "nome")
  private String nome;

  @Schema(description = "Prefixo da sigla administrativa da unidade.")
  @UISchema(label = "Sigla", group = "Pesquisa")
  @Filterable(operation = FilterOperation.STARTS_WITH, relation = "sigla")
  private String sigla;

  @Schema(description = "Órgão proprietário das unidades a consultar.")
  @UISchema(
      label = "Órgão",
      group = "Pesquisa",
      type = org.praxisplatform.uischema.FieldDataType.TEXT,
      controlType = org.praxisplatform.uischema.FieldControlType.ENTITY_LOOKUP,
      icon = "apartment",
      endpoint = ApiPaths.ORGAO_OPTIONS,
      valueField = "id",
      displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL, relation = "empCodigo")
  private Integer empCodigo;

  @Schema(description = "Unidade imediatamente superior no organograma pesquisado.")
  @UISchema(
      label = "Unidade superior",
      group = "Pesquisa",
      type = org.praxisplatform.uischema.FieldDataType.TEXT,
      controlType = org.praxisplatform.uischema.FieldControlType.ENTITY_LOOKUP,
      icon = "account_tree",
      endpoint = ApiPaths.SETOR_OPTIONS,
      valueField = "id",
      displayField = "label")
  @Filterable(operation = FilterOperation.EQUAL, relation = "setorPai")
  private String setorPai;

  @Schema(description = "Classificação administrativa da unidade organizacional.")
  @UISchema(label = "Tipo de unidade", group = "Pesquisa")
  @Filterable(operation = FilterOperation.EQUAL, relation = "tipoSetor")
  private String tipoSetor;

  @Schema(description = "Restringe a unidades sem data de encerramento registrada.")
  @UISchema(label = "Sem encerramento", group = "Pesquisa")
  @Filterable(operation = FilterOperation.IS_NULL, relation = "dtfim")
  private Boolean vigentes;

  @Schema(description = "Restringe a unidades com encerramento de vigência registrado.")
  @UISchema(label = "Com encerramento", group = "Pesquisa")
  @Filterable(operation = FilterOperation.IS_NOT_NULL, relation = "dtfim")
  private Boolean encerrados;
}
