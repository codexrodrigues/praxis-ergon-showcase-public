package br.com.praxis.ergonshowcase.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Contas de laboratório com escopo explícito para o plano de controle Config. */
@ConfigurationProperties("showcase.security")
public record ErgonConfigPrincipals(List<Principal> configPrincipals) {
  public ErgonConfigPrincipals {
    configPrincipals = configPrincipals == null ? List.of() : List.copyOf(configPrincipals);
  }

  public record Principal(String username, String password, int organization, String environment,
      List<String> roles) {
    @Override
    public String toString() {
      return "Config principal " + username;
    }
  }
}
