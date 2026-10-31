package br.com.praxis.ergonshowcase.constants;

/**
 * Caminhos públicos relativos do host, compartilhados por controllers, DTOs e registry.
 * Constantes de anotação devem continuar sendo expressões constantes de compilação.
 * Origem, porta e contexto de implantação pertencem à configuração do ambiente.
 */
public final class ApiPaths {
  private ApiPaths() {}

  public static final String BASE = "/api/ergon";
  public static final String ORGAOS = BASE + "/orgaos";
  public static final String CARGOS = BASE + "/cargos";
  public static final String PESSOAS = BASE + "/servidor-pessoas";
  public static final String VINCULOS = BASE + "/vinculos";
  public static final String SETORES = BASE + "/setores";
  public static final String DOCUMENTOS_LEGAIS = BASE + "/documentos-legais";
  public static final String DEPENDENTES = BASE + "/dependentes";
  public static final String TIPOS_DEPENDENTE = BASE + "/tipos-dependente";
  public static final String TIPOS_FREQUENCIA = BASE + "/tipos-frequencia";
  public static final String CODIGOS_FREQUENCIA = BASE + "/codigos-frequencia";
  public static final String AFASTAMENTOS = BASE + "/afastamentos";
  public static final String REGRAS_FREQUENCIA_ESTATUTARIA = BASE + "/regras-frequencia-estatutaria";
  public static final String FREQUENCIAS = BASE + "/frequencias";
  public static final String TABELAS = BASE + "/tabelas";
  public static final String ITENS_TABELA = BASE + "/itens-tabela";
  public static final String CAMPOS_CUSTOMIZADOS = BASE + "/campos-customizados";
  public static final String PARAMETROS_CONTAGEM_FERIAS = BASE + "/parametros-contagem-ferias";
  public static final String PERIODOS_AQUISITIVOS_FERIAS = BASE + "/periodos-aquisitivos-ferias";
  public static final String FERIAS_GOZO = BASE + "/ferias-gozo";
  public static final String RADAR_FERIAS_PRESCRICAO = BASE + "/radar-ferias-prescricao";

  public static final String ORGAO_SOURCE = "orgao";
  public static final String CARGO_SOURCE = "cargo";
  public static final String PESSOA_SOURCE = "pessoa";
  public static final String VINCULO_SOURCE = "vinculo";
  public static final String SETOR_SOURCE = "setor";
  public static final String DOCUMENTO_SOURCE = "documento";
  public static final String DEPENDENTE_SOURCE = "dependente";
  public static final String TIPO_DEPENDENTE_SOURCE = "tipoDependente";
  public static final String TIPO_FREQUENCIA_SOURCE = "tipoFrequencia";
  public static final String CODIGO_FREQUENCIA_SOURCE = "codigoFrequencia";
  public static final String AFASTAMENTO_SOURCE = "afastamento";
  public static final String REGRA_FREQUENCIA_ESTATUTARIA_SOURCE = "regraFrequenciaEstatutaria";
  public static final String FREQUENCIA_SOURCE = "frequencia";
  public static final String TABELA_SOURCE = "tabela";
  public static final String ITEM_TABELA_SOURCE = "itemTabela";
  public static final String CAMPO_CUSTOMIZADO_SOURCE = "campoCustomizado";
  public static final String PARAMETRO_CONTAGEM_FERIAS_SOURCE = "parametroContagemFerias";
  public static final String PERIODO_AQUISITIVO_FERIAS_SOURCE = "periodoAquisitivoFerias";
  public static final String FERIAS_GOZO_SOURCE = "feriasGozo";

  private static final String OPTION_SOURCES = "/option-sources/";
  private static final String OPTIONS_FILTER = "/options/filter";
  public static final String ORGAO_OPTIONS = ORGAOS + OPTION_SOURCES + ORGAO_SOURCE + OPTIONS_FILTER;
  public static final String CARGO_OPTIONS = CARGOS + OPTION_SOURCES + CARGO_SOURCE + OPTIONS_FILTER;
  public static final String PESSOA_OPTIONS = PESSOAS + OPTION_SOURCES + PESSOA_SOURCE + OPTIONS_FILTER;
  public static final String VINCULO_OPTIONS = VINCULOS + OPTION_SOURCES + VINCULO_SOURCE + OPTIONS_FILTER;
  public static final String SETOR_OPTIONS = SETORES + OPTION_SOURCES + SETOR_SOURCE + OPTIONS_FILTER;
  public static final String DOCUMENTO_OPTIONS = DOCUMENTOS_LEGAIS + OPTION_SOURCES + DOCUMENTO_SOURCE + OPTIONS_FILTER;
  public static final String DEPENDENTE_OPTIONS = DEPENDENTES + OPTION_SOURCES + DEPENDENTE_SOURCE + OPTIONS_FILTER;
  public static final String TIPO_DEPENDENTE_OPTIONS = TIPOS_DEPENDENTE + OPTION_SOURCES + TIPO_DEPENDENTE_SOURCE + OPTIONS_FILTER;
  public static final String TIPO_FREQUENCIA_OPTIONS = TIPOS_FREQUENCIA + OPTION_SOURCES + TIPO_FREQUENCIA_SOURCE + OPTIONS_FILTER;
  public static final String CODIGO_FREQUENCIA_OPTIONS = CODIGOS_FREQUENCIA + OPTION_SOURCES + CODIGO_FREQUENCIA_SOURCE + OPTIONS_FILTER;
  public static final String AFASTAMENTO_OPTIONS = AFASTAMENTOS + OPTION_SOURCES + AFASTAMENTO_SOURCE + OPTIONS_FILTER;
  public static final String REGRA_FREQUENCIA_ESTATUTARIA_OPTIONS =
      REGRAS_FREQUENCIA_ESTATUTARIA + OPTION_SOURCES + REGRA_FREQUENCIA_ESTATUTARIA_SOURCE + OPTIONS_FILTER;
  public static final String FREQUENCIA_OPTIONS = FREQUENCIAS + OPTION_SOURCES + FREQUENCIA_SOURCE + OPTIONS_FILTER;
  public static final String TABELA_OPTIONS = TABELAS + OPTION_SOURCES + TABELA_SOURCE + OPTIONS_FILTER;
  public static final String ITEM_TABELA_OPTIONS = ITENS_TABELA + OPTION_SOURCES + ITEM_TABELA_SOURCE + OPTIONS_FILTER;
  public static final String CAMPO_CUSTOMIZADO_OPTIONS =
      CAMPOS_CUSTOMIZADOS + OPTION_SOURCES + CAMPO_CUSTOMIZADO_SOURCE + OPTIONS_FILTER;
  public static final String PARAMETRO_CONTAGEM_FERIAS_OPTIONS =
      PARAMETROS_CONTAGEM_FERIAS + OPTION_SOURCES + PARAMETRO_CONTAGEM_FERIAS_SOURCE + OPTIONS_FILTER;
  public static final String PERIODO_AQUISITIVO_FERIAS_OPTIONS =
      PERIODOS_AQUISITIVOS_FERIAS + OPTION_SOURCES + PERIODO_AQUISITIVO_FERIAS_SOURCE + OPTIONS_FILTER;
  public static final String FERIAS_GOZO_OPTIONS =
      FERIAS_GOZO + OPTION_SOURCES + FERIAS_GOZO_SOURCE + OPTIONS_FILTER;

  public static final String BULK_OPERATIONS = BASE + "/operations/bulk";
  public static final String BULK_PUBLICATION_SEGMENT = "/publication";
  public static final String BULK_PUBLICATION = BULK_OPERATIONS + BULK_PUBLICATION_SEGMENT;
}
