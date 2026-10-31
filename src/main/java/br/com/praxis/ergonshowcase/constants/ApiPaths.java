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

  public static final String ORGAO_SOURCE = "orgao";
  public static final String CARGO_SOURCE = "cargo";
  public static final String PESSOA_SOURCE = "pessoa";
  public static final String VINCULO_SOURCE = "vinculo";
  public static final String SETOR_SOURCE = "setor";
  public static final String DOCUMENTO_SOURCE = "documento";

  private static final String OPTION_SOURCES = "/option-sources/";
  private static final String OPTIONS_FILTER = "/options/filter";
  public static final String ORGAO_OPTIONS = ORGAOS + OPTION_SOURCES + ORGAO_SOURCE + OPTIONS_FILTER;
  public static final String CARGO_OPTIONS = CARGOS + OPTION_SOURCES + CARGO_SOURCE + OPTIONS_FILTER;
  public static final String PESSOA_OPTIONS = PESSOAS + OPTION_SOURCES + PESSOA_SOURCE + OPTIONS_FILTER;
  public static final String SETOR_OPTIONS = SETORES + OPTION_SOURCES + SETOR_SOURCE + OPTIONS_FILTER;
  public static final String DOCUMENTO_OPTIONS = DOCUMENTOS_LEGAIS + OPTION_SOURCES + DOCUMENTO_SOURCE + OPTIONS_FILTER;

  public static final String BULK_OPERATIONS = BASE + "/operations/bulk";
  public static final String BULK_PUBLICATION_SEGMENT = "/publication";
  public static final String BULK_PUBLICATION = BULK_OPERATIONS + BULK_PUBLICATION_SEGMENT;
}
