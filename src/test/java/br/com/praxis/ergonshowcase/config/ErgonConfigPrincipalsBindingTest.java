package br.com.praxis.ergonshowcase.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import static org.assertj.core.api.Assertions.*;

/** Real configuration binding, without a hand-created accounts bean hiding constructor selection. */
class ErgonConfigPrincipalsBindingTest {
  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(ErgonConfigPrincipals.class)
  static class BindingConfiguration { }

  private final ApplicationContextRunner context = new ApplicationContextRunner()
      .withUserConfiguration(BindingConfiguration.class);

  @Test void startsWithoutOptionalAccounts() {
    context.run(app -> {
      assertThat(app).hasNotFailed();
      var accounts = app.getBean(ErgonConfigPrincipals.class);
      assertThat(accounts.configPrincipals()).isEmpty();
      assertThat(accounts.browserAccounts()).isEmpty();
    });
  }

  @Test void bindsTheDeclaredConfigScopeThroughTheCanonicalConstructor() {
    context.withPropertyValues(
        "showcase.security.config-principals[0].username=proof-admin",
        "showcase.security.config-principals[0].password=synthetic-not-a-credential",
        "showcase.security.config-principals[0].organization=2",
        "showcase.security.config-principals[0].environment=stage",
        "showcase.security.config-principals[0].roles[0]=RULE_SNAPSHOT_PUBLISHER")
        .run(app -> {
          assertThat(app).hasNotFailed();
          var accounts = app.getBean(ErgonConfigPrincipals.class);
          assertThat(accounts.configPrincipals()).containsExactly(new ErgonConfigPrincipals.Principal(
              "proof-admin", "synthetic-not-a-credential", 2, "stage", java.util.List.of("RULE_SNAPSHOT_PUBLISHER")));
          assertThat(accounts.browserAccounts()).isEmpty();
        });
  }
}
