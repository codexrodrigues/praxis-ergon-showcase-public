package br.com.praxis.ergonshowcase.constants;

/**
 * Caminhos públicos relativos do host, compartilhados por controllers, DTOs e registry.
 * Constantes de anotação devem continuar sendo expressões constantes de compilação.
 * Origem, porta e contexto de implantação pertencem à configuração do ambiente.
 */
public final class ApiPaths {
  public static final String KEY_AVERBACOES_TEMPO_SERVICO = "administracao-pessoal.averbacoes-tempo-servico";
  public static final String KEY_PERIODOS_AQUISITIVOS_LICENCA_ESPECIAL = "administracao-pessoal.periodos-aquisitivos-licenca-especial";
  public static final String KEY_LICENCAS_ESPECIAIS_GOZO = "administracao-pessoal.licencas-especiais-gozo";
  public static final String KEY_VENDAS_LICENCA_ESPECIAL = "administracao-pessoal.vendas-licenca-especial";

  private ApiPaths() {}

  public static final String BASE = "/api/ergon";
  public static final String POLICY_STUDIO = BASE + "/policy-studio";
  public static final String POLICY_STUDIO_COMPOSITION = "/rule-sets/{ruleSetKey}/orgaos/{empCodigo}/composition-manifest";
  public static final String PRAXIS_CONFIG = "/api/praxis/config";
  public static final String POLICY_STUDIO_SIMULATE_SEGMENT = "/simulate";
  public static final String POLICY_STUDIO_SIMULATE = POLICY_STUDIO + POLICY_STUDIO_SIMULATE_SEGMENT;
  public static final String ORGAOS = BASE + "/orgaos";
  public static final String CARGOS = BASE + "/cargos";
  public static final String PESSOAS = BASE + "/servidor-pessoas";
  public static final String VINCULOS = BASE + "/vinculos";
  public static final String SETORES = BASE + "/setores";
  public static final String SETOR_ARVORE = "/arvore-organograma";
  public static final String SETORES_ARVORE = SETORES + SETOR_ARVORE;
  public static final String DOCUMENTOS_LEGAIS = BASE + "/documentos-legais";
  public static final String DEPENDENTES = BASE + "/dependentes";
  public static final String TIPOS_DEPENDENTE = BASE + "/tipos-dependente";
  public static final String TIPOS_FREQUENCIA = BASE + "/tipos-frequencia";
  public static final String CODIGOS_FREQUENCIA = BASE + "/codigos-frequencia";
  public static final String AFASTAMENTOS = BASE + "/afastamentos";
  public static final String REGRAS_FREQUENCIA_ESTATUTARIA = BASE + "/regras-frequencia-estatutaria";
  public static final String KEY_REGRAS_FREQUENCIA_ESTATUTARIA = "administracao-pessoal.regras-frequencia-estatutaria";
  public static final String FREQUENCIAS = BASE + "/frequencias";
  public static final String TABELAS = BASE + "/tabelas";
  public static final String ITENS_TABELA = BASE + "/itens-tabela";
  public static final String CAMPOS_CUSTOMIZADOS = BASE + "/campos-customizados";
  public static final String PARAMETROS_CONTAGEM_FERIAS = BASE + "/parametros-contagem-ferias";
  public static final String PERIODOS_AQUISITIVOS_FERIAS = BASE + "/periodos-aquisitivos-ferias";
  public static final String FERIAS_GOZO = BASE + "/ferias-gozo";
  public static final String RADAR_FERIAS_PRESCRICAO = BASE + "/radar-ferias-prescricao";
  public static final String AVERBACOES_TEMPO_SERVICO = BASE + "/averbacoes-tempo-servico";
  public static final String PERIODOS_AQUISITIVOS_LICENCA_ESPECIAL = BASE + "/periodos-aquisitivos-licenca-especial";
  public static final String LICENCAS_ESPECIAIS_GOZO = BASE + "/licencas-especiais-gozo";
  public static final String VENDAS_LICENCA_ESPECIAL = BASE + "/vendas-licenca-especial";
  public static final String RADAR_TEMPO_SERVICO_LICENCA_ESPECIAL = BASE + "/radar-tempo-servico-licenca-especial";
  public static final String AVERBACAO_TEMPO_SERVICO_SOURCE = "averbacaoTempoServico";
  public static final String PERIODO_AQUISITIVO_LICENCA_ESPECIAL_SOURCE = "periodoAquisitivoLicencaEspecial";
  public static final String LICENCA_ESPECIAL_GOZO_SOURCE = "licencaEspecialGozo";
  public static final String VENDA_LICENCA_ESPECIAL_SOURCE = "vendaLicencaEspecial";

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
  public static final String AVERBACAO_TEMPO_SERVICO_OPTIONS = AVERBACOES_TEMPO_SERVICO + OPTION_SOURCES + AVERBACAO_TEMPO_SERVICO_SOURCE + OPTIONS_FILTER;
  public static final String PERIODO_AQUISITIVO_LICENCA_ESPECIAL_OPTIONS = PERIODOS_AQUISITIVOS_LICENCA_ESPECIAL + OPTION_SOURCES + PERIODO_AQUISITIVO_LICENCA_ESPECIAL_SOURCE + OPTIONS_FILTER;
  public static final String LICENCA_ESPECIAL_GOZO_OPTIONS = LICENCAS_ESPECIAIS_GOZO + OPTION_SOURCES + LICENCA_ESPECIAL_GOZO_SOURCE + OPTIONS_FILTER;
  public static final String VENDA_LICENCA_ESPECIAL_OPTIONS = VENDAS_LICENCA_ESPECIAL + OPTION_SOURCES + VENDA_LICENCA_ESPECIAL_SOURCE + OPTIONS_FILTER;
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

  // Funções de confiança e exercício de chefias
  public static final String KEY_FUNCOES = "administracao-pessoal.funcoes";
  public static final String FUNCOES = BASE + "/funcoes";
  public static final String FUNCAO_SOURCE = "funcao";
  public static final String FUNCAO_OPTIONS = FUNCOES + OPTION_SOURCES + FUNCAO_SOURCE + OPTIONS_FILTER;

  public static final String KEY_VAGAS_FUNCAO = "administracao-pessoal.vagas-funcao";
  public static final String VAGAS_FUNCAO = BASE + "/vagas-funcao";
  public static final String VAGA_FUNCAO_SOURCE = "vagaFuncao";
  public static final String VAGA_FUNCAO_OPTIONS = VAGAS_FUNCAO + OPTION_SOURCES + VAGA_FUNCAO_SOURCE + OPTIONS_FILTER;

  public static final String KEY_DESIGNACOES = "administracao-pessoal.designacoes";
  public static final String DESIGNACOES = BASE + "/designacoes";
  public static final String DESIGNACAO_SOURCE = "designacao";
  public static final String DESIGNACAO_OPTIONS = DESIGNACOES + OPTION_SOURCES + DESIGNACAO_SOURCE + OPTIONS_FILTER;

  public static final String KEY_SUBSTITUICOES = "administracao-pessoal.substituicoes";
  public static final String SUBSTITUICOES = BASE + "/substituicoes";
  public static final String SUBSTITUICAO_SOURCE = "substituicao";
  public static final String SUBSTITUICAO_OPTIONS = SUBSTITUICOES + OPTION_SOURCES + SUBSTITUICAO_SOURCE + OPTIONS_FILTER;

  public static final String KEY_RADAR_CHEFIAS_SUBSTITUICOES = "administracao-pessoal.radar-chefias-substituicoes";
  public static final String RADAR_CHEFIAS_SUBSTITUICOES = BASE + "/radar-chefias-substituicoes";

  public static final String BULK_OPERATIONS = BASE + "/operations/bulk";
  public static final String BULK_PUBLICATION_SEGMENT = "/publication";
  public static final String BULK_PUBLICATION = BULK_OPERATIONS + BULK_PUBLICATION_SEGMENT;

  public static final String KEY_SETORES = "administracao-pessoal.setores";
  public static final String KEY_HISTORICO_SETORES = "administracao-pessoal.historico-setores";
  public static final String HISTORICO_SETORES = BASE + "/historico-setores";
  public static final String HISTORICO_SETOR_SOURCE = "historicoSetor";
  public static final String HISTORICO_SETOR_OPTIONS = HISTORICO_SETORES + OPTION_SOURCES + HISTORICO_SETOR_SOURCE + OPTIONS_FILTER;
  public static final String KEY_SETORES_ERGON = "administracao-pessoal.setores-ergon";
  public static final String SETORES_ERGON = BASE + "/setores-ergon";
  public static final String SETOR_ERGON_SOURCE = "setorErgon";
  public static final String SETOR_ERGON_OPTIONS = SETORES_ERGON + OPTION_SOURCES + SETOR_ERGON_SOURCE + OPTIONS_FILTER;
  public static final String KEY_CARGOS_POR_SETOR = "administracao-pessoal.cargos-por-setor";
  public static final String CARGOS_POR_SETOR = BASE + "/cargos-por-setor";
  public static final String CARGO_SETOR_SOURCE = "cargoSetor";
  public static final String CARGO_SETOR_OPTIONS = CARGOS_POR_SETOR + OPTION_SOURCES + CARGO_SETOR_SOURCE + OPTIONS_FILTER;
  public static final String KEY_SUBEMPRESAS_LOCALIDADES = "administracao-pessoal.subempresas-localidades";
  public static final String SUBEMPRESAS_LOCALIDADES = BASE + "/subempresas-localidades";
  public static final String SUBEMPRESA_LOCALIDADE_SOURCE = "subempresaLocalidade";
  public static final String SUBEMPRESA_LOCALIDADE_OPTIONS = SUBEMPRESAS_LOCALIDADES + OPTION_SOURCES + SUBEMPRESA_LOCALIDADE_SOURCE + OPTIONS_FILTER;
  public static final String KEY_GRUPOS_SETORES = "administracao-pessoal.grupos-setores";
  public static final String GRUPOS_SETORES = BASE + "/grupos-setores";
  public static final String GRUPO_SETOR_SOURCE = "grupoSetor";
  public static final String GRUPO_SETOR_OPTIONS = GRUPOS_SETORES + OPTION_SOURCES + GRUPO_SETOR_SOURCE + OPTIONS_FILTER;
  public static final String KEY_RADAR_ORGANOGRAMA_CHEFIAS_OCUPACAO = "administracao-pessoal.radar-organograma-chefias-ocupacao";
  public static final String RADAR_ORGANOGRAMA_CHEFIAS_OCUPACAO = BASE + "/radar-organograma-chefias-ocupacao";
}
