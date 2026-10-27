package br.com.praxis.ergonshowcase.constants;

/** Rotas públicas do cadastro civil e funcional. */
public final class ApiPaths {
  private ApiPaths() {}

  public static final String ORGAOS = "/api/ergon/orgaos";
  public static final String CARGOS = "/api/ergon/cargos";
  public static final String PESSOAS = "/api/ergon/servidor-pessoas";
  public static final String VINCULOS = "/api/ergon/vinculos";
  public static final String ORGAO_OPTIONS = ORGAOS + "/option-sources/orgao/options/filter";
  public static final String CARGO_OPTIONS = CARGOS + "/option-sources/cargo/options/filter";
  public static final String PESSOA_OPTIONS = PESSOAS + "/option-sources/pessoa/options/filter";
}
