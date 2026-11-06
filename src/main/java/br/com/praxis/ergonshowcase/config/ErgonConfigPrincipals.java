package br.com.praxis.ergonshowcase.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Contas de laboratório com escopo explícito para o plano de controle Config. */
@ConfigurationProperties("showcase.security")
public record ErgonConfigPrincipals(List<Principal> configPrincipals, List<ErgonBrowserAccount> browserAccounts) {
  public ErgonConfigPrincipals(List<Principal> configPrincipals) { this(configPrincipals, List.of()); }
  @org.springframework.boot.context.properties.bind.ConstructorBinding
  public ErgonConfigPrincipals {
    configPrincipals = configPrincipals == null ? List.of() : List.copyOf(configPrincipals);
    browserAccounts = browserAccounts == null ? List.of() : List.copyOf(browserAccounts);
  }

  public record Principal(String username, String password, int organization, String environment,
      List<String> roles) {
    @Override
    public String toString() {
      return "Config principal " + username;
    }
  }
}
