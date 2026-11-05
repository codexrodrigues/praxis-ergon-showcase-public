package br.com.praxis.ergonshowcase.dto;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.Map;
import lombok.*;
import org.praxisplatform.uischema.*;
import org.praxisplatform.uischema.annotation.*;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.extension.annotation.UISchemaPreset;

/** Ficha de consulta auxiliar; não publica escrita sobre o cadastro de origem. */
@Getter
@Setter
@Schema(description = "Unidade do organograma institucional e sua vigência.")
@ResourceIdentity(
    keyField = "id",
    titleField = "nome",
    subtitleField = "sigla",
    metadataFields = {"setorPai", "dtfim"})
public class SetorResponseDTO {
  @Schema(
      description = "Código estruturado que identifica a unidade de lotação no organograma.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Código da unidade",
      group = "Identificação",
      readOnly = true,
      tableHidden = false)
  private String id;

  @Schema(description = "Identidade de navegação da ficha. Na leitura temporal identifica o período, a data de composição e a árvore selecionada.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Referência da ficha", group = "Identificação", readOnly = true, tableHidden = true, sortable = false)
  private String referenciaFicha;

  public String getReferenciaFicha() {
    return referenciaFicha == null ? id : referenciaFicha;
  }

  @ResourceReference(
      resourceKey = "administracao-pessoal.empresas",
      valueField = "empCodigo",
      surfaceId = "ficha-institucional",
      previewSurfaceId = "ficha-institucional",
      detailPresentation = "drawer")
  @Schema(
      description = "Ente público responsável pela unidade organizacional.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Órgão",
      group = "Identificação",
      readOnly = true,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 1,
      type = org.praxisplatform.uischema.FieldDataType.TEXT,
      controlType = org.praxisplatform.uischema.FieldControlType.ENTITY_LOOKUP,
      icon = "apartment",
      endpoint = ApiPaths.ORGAO_OPTIONS,
      valueField = "id",
      displayField = "label")
  private Integer empCodigo;

  @ResourceReference(
      resourceKey = "administracao-pessoal.setores",
      valueField = "referenciaFicha",
      surfaceId = "ficha-unidade",
      previewSurfaceId = "ficha-unidade",
      detailPresentation = "drawer")
  @Schema(
      description = "Denominação oficial da secretaria, diretoria ou unidade de lotação.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Unidade organizacional",
      group = "Identificação",
      readOnly = true,
      tableHidden = false)
  private String nome;

  @Schema(
      description = "Denominação abreviada usada para distinguir a unidade em organogramas.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Sigla", group = "Identificação", readOnly = true, tableHidden = false)
  private String sigla;

  @ResourceReference(
      resourceKey = "administracao-pessoal.setores",
      valueField = "setorPai",
      surfaceId = "ficha-unidade",
      previewSurfaceId = "ficha-unidade",
      detailPresentation = "drawer")
  @Schema(
      description = "Código da unidade imediatamente superior na hierarquia do mesmo órgão.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Unidade superior",
      group = "Organograma",
      readOnly = true,
      tableHidden = true,
      type = org.praxisplatform.uischema.FieldDataType.TEXT,
      controlType = org.praxisplatform.uischema.FieldControlType.ENTITY_LOOKUP,
      icon = "account_tree",
      endpoint = ApiPaths.SETOR_OPTIONS,
      valueField = "id",
      displayField = "label")
  private String setorPai;

  @Schema(
      description = "Classificação administrativa da unidade, como secretaria ou diretoria.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Tipo de unidade", icon = "account_tree",
      group = "Organograma",
      readOnly = true,
      tableHidden = false,
      quickViewVisible = true,
      quickViewOrder = 2)
  private String tipoSetor;

  @Schema(
      description = "Primeiro dia de vigência da unidade no organograma institucional.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Início da vigência",
      group = "Vigência e fundamento",
      readOnly = true,
      tableHidden = true,
      quickViewVisible = true,
      quickViewOrder = 3,
      type = org.praxisplatform.uischema.FieldDataType.DATE,
      controlType = org.praxisplatform.uischema.FieldControlType.DATE_PICKER,
      icon = "event")
  private LocalDate dtini;

  @Schema(
      description = "Último dia de vigência; ausência indica unidade sem encerramento registrado.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Fim da vigência",
      group = "Vigência e fundamento",
      readOnly = true,
      tableHidden = false,
      type = org.praxisplatform.uischema.FieldDataType.DATE,
      controlType = org.praxisplatform.uischema.FieldControlType.DATE_PICKER,
      icon = "event")
  private LocalDate dtfim;

  @Schema(
      description =
          "Inscrição da unidade tributária usada na apuração previdenciária e no eSocial.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "CNPJ da lotação", group = "Localização", readOnly = true, tableHidden = true)
  private String cgcLotacao;

  @Schema(
      description = "Município da localização física da unidade organizacional.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Município", icon = "location_city",
      group = "Localização",
      readOnly = true,
      tableHidden = true,
      quickViewVisible = true,
      quickViewOrder = 4)
  private String municipioLotacao;

  @Schema(
      description = "Unidade federativa da localização física da lotação.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "UF", icon = "map",
      group = "Localização",
      readOnly = true,
      tableHidden = true,
      quickViewVisible = true,
      quickViewOrder = 5)
  private String ufLotacao;

  @ResourceReference(
      resourceKey = "administracao-pessoal.documentos-legais",
      valueField = "documentoLegalId",
      surfaceId = "ficha-documento",
      previewSurfaceId = "ficha-documento",
      detailPresentation = "drawer")
  @Schema(
      description = "Documento legal que institui ou reestrutura a unidade organizacional.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Ato de criação",
      group = "Vigência e fundamento",
      readOnly = true,
      tableHidden = true,
      type = org.praxisplatform.uischema.FieldDataType.TEXT,
      controlType = org.praxisplatform.uischema.FieldControlType.ENTITY_LOOKUP,
      icon = "gavel",
      endpoint = ApiPaths.DOCUMENTO_OPTIONS,
      valueField = "id",
      displayField = "label")
  private Long documentoLegalId;

  @org.praxisplatform.uischema.customfields.CustomFields(
      resourceKey = "administracao-pessoal.setores")
  @Schema(
      description = "Atributos de lotação definidos pelo catálogo de campos customizados do órgão.",
      accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(
      label = "Características do órgão",
      group = "Características adicionais",
      readOnly = true,
      tableHidden = true)
  private Map<String, Object> customFields;
  @Schema(description = "Denominação completa da unidade quando o nome resumido exige abreviação.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Nome por extenso", group = "Configuração organizacional", tableHidden = true, readOnly = true, type = FieldDataType.TEXT, icon = "description")
  private String nomeLongo;
  @Schema(description = "Primeiro início conhecido da unidade, distinto do início do período corrente.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Criação original", group = "Configuração organizacional", tableHidden = true, readOnly = true, type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, icon = "event", preset = UISchemaPreset.DATE_RANGE)
  private LocalDate dataCriacaoOriginal;
  @Schema(description = "Quantidade de subordinados diretos na hierarquia e data da composição consultada.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Subordinados diretos", group = "Configuração organizacional", tableHidden = true, readOnly = true, type = FieldDataType.NUMBER, icon = "numbers")
  private Integer quantidadeSubordinadosDiretos;
  @Schema(description = "Centro de custo da parametrização funcional válida na data da composição consultada.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Centro de custo", group = "Configuração organizacional", tableHidden = true, readOnly = true, type = FieldDataType.TEXT, icon = "description")
  private String centroCustoPadrao;
  @Schema(description = "Cargo parametrizado para identificar o titular do comando da unidade.", accessMode = Schema.AccessMode.READ_ONLY)
  @ResourceReference(resourceKey = "administracao-pessoal.cargos", valueField = "cargoChefeId", surfaceId = "ficha-cargo", previewSurfaceId = "ficha-cargo", detailPresentation = "drawer")
  @UISchema(label = "Cargo de comando", group = "Configuração organizacional", tableHidden = true, readOnly = true, type = FieldDataType.TEXT, controlType = FieldControlType.ENTITY_LOOKUP, icon = "account_tree", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.CARGO_OPTIONS, valueField = "id", displayField = "label")
  private Integer cargoChefeId;
  @Schema(description = "Função de confiança parametrizada para identificar o titular da unidade.", accessMode = Schema.AccessMode.READ_ONLY)
  @ResourceReference(resourceKey = "administracao-pessoal.funcoes", valueField = "funcaoChefeId", surfaceId = "detail", previewSurfaceId = "detail", detailPresentation = "drawer")
  @UISchema(label = "Função de comando", group = "Configuração organizacional", tableHidden = true, readOnly = true, type = FieldDataType.TEXT, controlType = FieldControlType.ENTITY_LOOKUP, icon = "account_tree", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.FUNCAO_OPTIONS, valueField = "id", displayField = "label")
  private Long funcaoChefeId;
  @Schema(description = "Justificativa administrativa da configuração corrente da unidade.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Observação", group = "Vigência e fundamento", tableHidden = true, readOnly = true, type = FieldDataType.TEXT, icon = "description")
  private String observacao;
  @Schema(description = "Superior da primeira hierarquia funcional alternativa; não redefine a subordinação legal.", accessMode = Schema.AccessMode.READ_ONLY)
  @ResourceReference(resourceKey = "administracao-pessoal.setores", valueField = "setorPaiHier1", surfaceId = "ficha-unidade", previewSurfaceId = "ficha-unidade", detailPresentation = "drawer")
  @UISchema(label = "Superior auxiliar 1", group = "Configuração organizacional", tableHidden = true, readOnly = true, type = FieldDataType.TEXT, controlType = FieldControlType.ENTITY_LOOKUP, icon = "account_tree", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.SETOR_OPTIONS, valueField = "id", displayField = "label")
  private String setorPaiHier1;
  @Schema(description = "Superior da segunda hierarquia funcional alternativa na vigência do órgão.", accessMode = Schema.AccessMode.READ_ONLY)
  @ResourceReference(resourceKey = "administracao-pessoal.setores", valueField = "setorPaiHier2", surfaceId = "ficha-unidade", previewSurfaceId = "ficha-unidade", detailPresentation = "drawer")
  @UISchema(label = "Superior auxiliar 2", group = "Configuração organizacional", tableHidden = true, readOnly = true, type = FieldDataType.TEXT, controlType = FieldControlType.ENTITY_LOOKUP, icon = "account_tree", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.SETOR_OPTIONS, valueField = "id", displayField = "label")
  private String setorPaiHier2;
  @Schema(description = "Superior da terceira hierarquia funcional alternativa na vigência do órgão.", accessMode = Schema.AccessMode.READ_ONLY)
  @ResourceReference(resourceKey = "administracao-pessoal.setores", valueField = "setorPaiHier3", surfaceId = "ficha-unidade", previewSurfaceId = "ficha-unidade", detailPresentation = "drawer")
  @UISchema(label = "Superior auxiliar 3", group = "Configuração organizacional", tableHidden = true, readOnly = true, type = FieldDataType.TEXT, controlType = FieldControlType.ENTITY_LOOKUP, icon = "account_tree", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.SETOR_OPTIONS, valueField = "id", displayField = "label")
  private String setorPaiHier3;
  @Schema(description = "Superior da quarta hierarquia funcional alternativa na vigência do órgão.", accessMode = Schema.AccessMode.READ_ONLY)
  @ResourceReference(resourceKey = "administracao-pessoal.setores", valueField = "setorPaiHier4", surfaceId = "ficha-unidade", previewSurfaceId = "ficha-unidade", detailPresentation = "drawer")
  @UISchema(label = "Superior auxiliar 4", group = "Configuração organizacional", tableHidden = true, readOnly = true, type = FieldDataType.TEXT, controlType = FieldControlType.ENTITY_LOOKUP, icon = "account_tree", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.SETOR_OPTIONS, valueField = "id", displayField = "label")
  private String setorPaiHier4;
  @Schema(description = "Sequência de códigos codificados e delimitados desde a raiz; o prefixo completo delimita uma subárvore sem confundir códigos irmãos.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Caminho hierárquico", group = "Configuração organizacional", tableHidden = true, readOnly = true, type = FieldDataType.TEXT, icon = "description")
  private String caminhoHierarquico;
  @Schema(description = "Profundidade a partir da raiz, cujo nível é um; depende da hierarquia e da data de referência.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Nível hierárquico", group = "Configuração organizacional", tableHidden = true, readOnly = true, type = FieldDataType.NUMBER, icon = "numbers")
  private Integer nivelHierarquico;
  @Schema(description = "Código da unidade empregadora fiscal da lotação; sua identidade inclui o órgão e a vigência.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Subempresa", group = "Configuração organizacional", tableHidden = true, readOnly = true, type = FieldDataType.TEXT, icon = "description")
  private String subempCodigo;
  @Schema(description = "Código da regionalização, comarca ou distrito associado à unidade; distingue localização administrativa de endereço.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Localidade", group = "Configuração organizacional", tableHidden = true, readOnly = true, type = FieldDataType.TEXT, icon = "description")
  private String localidadeCodigo;
  @Schema(description = "Período do agrupamento transversal ao qual esta unidade pertence, no mesmo órgão.", accessMode = Schema.AccessMode.READ_ONLY)
  @ResourceReference(resourceKey = "administracao-pessoal.grupos-setores", valueField = "grupoSetorId", surfaceId = "detail", previewSurfaceId = "detail", detailPresentation = "drawer")
  @UISchema(label = "Grupo de setores", group = "Configuração organizacional", tableHidden = true, readOnly = true, type = FieldDataType.TEXT, controlType = FieldControlType.ENTITY_LOOKUP, icon = "account_tree", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.GRUPO_SETOR_OPTIONS, valueField = "id", displayField = "label")
  private Long grupoSetorId;
  @Schema(description = "Marca a unidade desativada para novas lotações; preserva referências e histórico anteriores à extinção.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Extinto", group = "Configuração organizacional", tableHidden = true, readOnly = true, type = FieldDataType.BOOLEAN, controlType = FieldControlType.CHECKBOX, icon = "check_circle", preset = UISchemaPreset.BOOLEAN_FLAG, labelTrue = "Sim", labelFalse = "Não", toneTrue = "info", toneFalse = "neutral")
  private Boolean extinto;
  @Schema(description = "Data de efeito da desativação administrativa; não é automaticamente o fim do histórico.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Data de extinção", group = "Configuração organizacional", tableHidden = true, readOnly = true, type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, icon = "event", preset = UISchemaPreset.DATE_RANGE)
  private LocalDate dataExtincao;
  @Schema(description = "Código da unidade destinada a receber dependências na migração de extinção; a transferência exige comando governado posterior.", accessMode = Schema.AccessMode.READ_ONLY)
  @ResourceReference(resourceKey = "administracao-pessoal.setores", valueField = "setorSubstituto", surfaceId = "ficha-unidade", previewSurfaceId = "ficha-unidade", detailPresentation = "drawer")
  @UISchema(label = "Unidade sucessora", group = "Configuração organizacional", tableHidden = true, readOnly = true, type = FieldDataType.TEXT, controlType = FieldControlType.ENTITY_LOOKUP, icon = "account_tree", preset = UISchemaPreset.ENTERPRISE_ID, endpoint = ApiPaths.SETOR_OPTIONS, valueField = "id", displayField = "label")
  private String setorSubstituto;
  @Schema(description = "Espécie do logradouro da unidade, como rua, avenida ou praça.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Tipo de logradouro", group = "Configuração organizacional", tableHidden = true, readOnly = true, type = FieldDataType.TEXT, icon = "description")
  private String tipoLogradouro;
  @Schema(description = "Nome oficial do logradouro onde a unidade organizacional está instalada.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Logradouro", group = "Configuração organizacional", tableHidden = true, readOnly = true, type = FieldDataType.TEXT, icon = "description")
  private String logradouro;
  @Schema(description = "Número do imóvel da unidade; admite identificação sem numeração oficial.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Número", group = "Configuração organizacional", tableHidden = true, readOnly = true, type = FieldDataType.TEXT, icon = "description")
  private String numeroEndereco;
  @Schema(description = "Identificação de bloco, sala ou referência interna do endereço da unidade.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Complemento", group = "Configuração organizacional", tableHidden = true, readOnly = true, type = FieldDataType.TEXT, icon = "description")
  private String complementoEndereco;
  @Schema(description = "Bairro da instalação física da unidade durante este período.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Bairro", group = "Configuração organizacional", tableHidden = true, readOnly = true, type = FieldDataType.TEXT, icon = "description")
  private String bairro;
  @Schema(description = "Código postal da instalação física para correspondência institucional.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "CEP", group = "Configuração organizacional", tableHidden = true, readOnly = true, type = FieldDataType.TEXT, icon = "description")
  private String cep;
  @Schema(description = "Número de contato institucional da unidade, incluindo código de área.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Telefone", group = "Configuração organizacional", tableHidden = true, readOnly = true, type = FieldDataType.TEXT, icon = "description")
  private String telefone;
  @Schema(description = "Número institucional de fac-símile preservado do cadastro histórico de endereço.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "Fax", group = "Configuração organizacional", tableHidden = true, readOnly = true, type = FieldDataType.TEXT, icon = "description")
  private String fax;
  @Schema(description = "Caixa de correio institucional da unidade para comunicação administrativa.", accessMode = Schema.AccessMode.READ_ONLY)
  @UISchema(label = "E-mail institucional", group = "Configuração organizacional", tableHidden = true, readOnly = true, type = FieldDataType.TEXT, icon = "description")
  private String emailUnidade;
  @MicroVisualization(kind=MicroVisualizationKind.HARVEY_BALL,valueExpr="=extinto ? 1 : 0",totalExpr="=1",min=0,max=1,thresholds=@Threshold(min=1,tone="neutral",label="Unidade extinta"),fallbackText="Estado administrativo não informado",ariaLabel="Extinção administrativa da unidade")
  public Boolean getExtinto() {return extinto;}
}
