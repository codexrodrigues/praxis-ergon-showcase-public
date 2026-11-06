package br.com.praxis.ergonshowcase.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import jakarta.persistence.EntityManagerFactory;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.praxisplatform.config.controller.UserConfigController;
import org.praxisplatform.config.repository.UiUserConfigRepository;
import org.praxisplatform.config.service.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.*;
import org.springframework.boot.autoconfigure.web.servlet.*;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.praxisplatform.config.tx.ConfigTransactionManagerNames;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

/** Native HTTP + actual Config UI chain on disposable PostgreSQL; not full Config/domain installation. */
class ErgonBrowserConfigPostgresTest {
  @TempDir Path temporary;
  @Configuration(proxyBeanMethods = false)
  @ImportAutoConfiguration({ServletWebServerFactoryAutoConfiguration.class, DispatcherServletAutoConfiguration.class,
      WebMvcAutoConfiguration.class, JacksonAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class,
      SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class,
      org.praxisplatform.config.autoconfig.EnterpriseRuntimeAutoConfiguration.class})
  @Import({SecurityConfig.class, ErgonBrowserIdentity.class, ErgonSessionController.class,
      UserConfigController.class, UserConfigService.class, AiApiKeyProtectionService.class,
      AiApiKeyCryptoService.class, CanonicalJsonHashService.class})
  static class NativeApp {
    @Bean DataSource configDataSource(@Value("${proof.config.url}") String url,
        @Value("${proof.config.password}") String password) {
      return new DriverManagerDataSource(url, "auth_config_runtime", password);
    }
    @Bean NamedParameterJdbcTemplate configNamedParameterJdbcTemplate(DataSource source) { return new NamedParameterJdbcTemplate(source); }
    @Bean LocalContainerEntityManagerFactoryBean configEntityManagerFactory(DataSource source) {
      var result = new LocalContainerEntityManagerFactoryBean(); result.setDataSource(source);
      result.setManagedTypes(org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes.of(
          org.praxisplatform.config.domain.UiUserConfig.class.getName()));
      result.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
      result.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "validate")); return result;
    }
    @Bean(ConfigTransactionManagerNames.CONFIG)
    JpaTransactionManager configTransactionManager(EntityManagerFactory factory) {
      return new JpaTransactionManager(factory);
    }
    @Bean JpaRepositoryFactoryBean<UiUserConfigRepository, org.praxisplatform.config.domain.UiUserConfig, UUID>
        uiUserConfigRepository(EntityManagerFactory factory) {
      var repository = new JpaRepositoryFactoryBean<UiUserConfigRepository,
          org.praxisplatform.config.domain.UiUserConfig, UUID>(UiUserConfigRepository.class);
      repository.setEntityManager(SharedEntityManagerCreator.createSharedEntityManager(factory));
      repository.setTransactionManager(ConfigTransactionManagerNames.CONFIG);
      return repository;
    }
  }

  @Test void actualHttpSessionsKeepPersonalAndCommonConfigurationsSeparate() throws Exception {
    Path data = temporary.resolve("postgres").toAbsolutePath().normalize();
    assertThat(data.startsWith(temporary.toAbsolutePath().normalize())).isTrue();
    try (var pg = EmbeddedPostgres.builder().setDataDirectory(data).setCleanDataDirectory(true)
        .setPGStartupWait(Duration.ofMinutes(2)).setServerConfig("listen_addresses", "127.0.0.1").setPort(0).start()) {
      String dbPassword = UUID.randomUUID().toString();
      try (var connection = pg.getPostgresDatabase().getConnection(); var statement = connection.createStatement()) {
        statement.execute("create database auth_domain"); statement.execute("create database auth_config");
        statement.execute("create role auth_config_runtime login password '" + dbPassword + "'");
        statement.execute("create role auth_domain_runtime login password '" + UUID.randomUUID() + "'");
      }
      Path artifact = Path.of(UserConfigService.class.getProtectionDomain().getCodeSource().getLocation().toURI());
      Path migrations = temporary.resolve("migrations"); Files.createDirectories(migrations);
      try (var zip = new java.util.zip.ZipFile(artifact.toFile())) {
        // Exact UI migration subset, unchanged bytes. The complete chain requires pgvector.
        var uiMigrations = Set.of("V5__create_ui_user_config.sql", "V7__migrate_ui_user_config_component_type_selectors.sql",
            "V9__dedupe_ui_user_config_null_scope_uniques.sql", "V61__add_ui_user_config_authoring_source.sql");
        for (var entry : zip.stream().filter(e -> uiMigrations.contains(Path.of(e.getName()).getFileName().toString())).toList())
          try (var input = zip.getInputStream(entry)) { Files.copy(input, migrations.resolve(Path.of(entry.getName()).getFileName())); }
      }
      String url = "jdbc:postgresql://127.0.0.1:" + pg.getPort() + "/auth_config";
      Flyway.configure().dataSource(url, "postgres", "postgres").locations("filesystem:" + migrations.toString().replace('\\', '/'))
          .load().migrate();
      try (var connection = pg.getDatabase("postgres", "auth_config").getConnection(); var statement = connection.createStatement()) {
        statement.execute("grant usage on schema public to auth_config_runtime");
        statement.execute("grant select,insert,update,delete on ui_user_config to auth_config_runtime");
        statement.execute("grant usage,select on all sequences in schema public to auth_config_runtime");
      }
      var properties = new HashMap<String,Object>();
      properties.put("spring.config.location", "optional:file:./nonexistent-auth-proof.properties");
      properties.put("server.port", "8080"); properties.put("server.address", "127.0.0.1");
      properties.put("server.servlet.session.cookie.name", "ERGONSESSION");
      properties.put("server.servlet.session.cookie.http-only", "true");
      properties.put("server.servlet.session.cookie.same-site", "lax");
      properties.put("showcase.security.username", ""); properties.put("showcase.security.password", "");
      properties.put("showcase.security.domain-environment", "local");
      properties.put("showcase.security.config-origins", "http://127.0.0.1:4303");
      properties.put("proof.config.url", url); properties.put("proof.config.password", dbPassword);
      String password = UUID.randomUUID().toString();
      String hash = org.springframework.security.crypto.factory.PasswordEncoderFactories.createDelegatingPasswordEncoder().encode(password);
      int index = 0;
      for (String name : List.of("ana", "bia", "gestor", "consulta", "empresa")) {
        String prefix = "showcase.security.browser-accounts[" + index++ + "].";
        properties.put(prefix + "username", "prova." + name); properties.put(prefix + "password-hash", hash);
        properties.put(prefix + "display-name", "Prova " + name); properties.put(prefix + "organization", name.equals("empresa") ? 2 : 1);
        properties.put(prefix + "organization-label", name.equals("empresa") ? "Empresa sintética 2" : "Empresa sintética 1");
        properties.put(prefix + "environment", "local");
        properties.put(prefix + "profile", name.equals("gestor") ? "MANAGER" : name.equals("consulta") ? "READER" : "PERSONAL");
      }
      var passwords = new Properties();
      for (String name : List.of("ana", "bia", "gestor", "consulta", "empresa")) passwords.setProperty("prova." + name, password);
      String provisioned = System.getProperty("ergon.auth.accounts-directory");
      if (provisioned != null) {
        var configuration = new Properties();
        try (var input = Files.newBufferedReader(Path.of(provisioned, "accounts.properties"))) { configuration.load(input); }
        for (String key : configuration.stringPropertyNames()) properties.put(key, configuration.getProperty(key));
        try (var input = Files.newBufferedReader(Path.of(provisioned, "credentials.txt"))) { passwords.load(input); }
      }
      try (var app = new SpringApplicationBuilder(NativeApp.class).web(WebApplicationType.SERVLET).properties(properties).run()) {
        var ana = new Browser("prova.ana", passwords.getProperty("prova.ana")); var bia = new Browser("prova.bia", passwords.getProperty("prova.bia"));
        var manager = new Browser("prova.gestor", passwords.getProperty("prova.gestor")); var reader = new Browser("prova.consulta", passwords.getProperty("prova.consulta"));
        var company = new Browser("prova.empresa", passwords.getProperty("prova.empresa"));
        assertThat(ana.write("user", "ana").statusCode()).isEqualTo(200);
        assertThat(bia.write("user", "bia").statusCode()).isEqualTo(200);
        assertThat(manager.write("tenant", "common").statusCode()).isEqualTo(200);
        assertThat(ana.read().body()).contains("ana"); assertThat(bia.read().body()).contains("bia");
        assertThat(reader.read().body()).contains("common"); assertThat(company.read().statusCode()).isEqualTo(404);
        assertThat(reader.write("user", "forbidden").statusCode()).isEqualTo(403);
        assertThat(ana.write("tenant", "forbidden").statusCode()).isEqualTo(403);
        String anaEtag = ana.read().headers().firstValue("ETag").orElseThrow();
        assertThat(ana.writeConditional("ana-updated", anaEtag).statusCode()).isEqualTo(200);
        assertThat(ana.writeConditional("stale-must-not-win", anaEtag).statusCode()).isEqualTo(412);
        assertThat(ana.read().body()).contains("ana-updated").doesNotContain("stale-must-not-win");
        assertThat(company.write("user", "company2").statusCode()).isEqualTo(200);
        assertThat(ana.read().body()).contains("ana").doesNotContain("company2");
        assertThat(ana.send("/auth/logout", "POST", "", null).statusCode()).isEqualTo(204);
        assertThat(ana.send("/auth/session", "GET", null, null).statusCode()).isEqualTo(401);
        try (var connection = pg.getDatabase("postgres", "auth_config").getConnection(); var statement = connection.createStatement();
            var result = statement.executeQuery("select count(*) from ui_user_config where component_id='auth-native-proof'")) {
          assertThat(result.next()).isTrue(); assertThat(result.getInt(1)).isEqualTo(4);
        }
        String holdDirectory = System.getProperty("ergon.auth.hold-directory");
        if (holdDirectory != null) {
          Path hold = Path.of(holdDirectory).toAbsolutePath().normalize();
          Files.createDirectories(hold);
          Files.writeString(hold.resolve("native-ready.json"), "{\"port\":8080,\"pid\":" + ProcessHandle.current().pid() + "}");
          long deadline = System.nanoTime() + Duration.ofMinutes(10).toNanos();
          while (!Files.exists(hold.resolve("stop")) && System.nanoTime() < deadline) Thread.sleep(250);
        }
      }
    }
  }

  static class Browser {
    final HttpClient client = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
        .followRedirects(HttpClient.Redirect.NEVER).build();
    String csrf;
    Browser(String username, String password) throws Exception {
      var bootstrap = send("/auth/session", "GET", null, null); assertThat(bootstrap.statusCode()).isEqualTo(401);
      csrf = bootstrap.headers().firstValue("X-XSRF-TOKEN").orElseThrow();
      assertThat(send("/auth/login", "POST", "username=" + URLEncoder.encode(username, java.nio.charset.StandardCharsets.UTF_8)
          + "&password=" + URLEncoder.encode(password, java.nio.charset.StandardCharsets.UTF_8), "application/x-www-form-urlencoded").statusCode()).isEqualTo(204);
      var session = send("/auth/session", "GET", null, null); assertThat(session.statusCode()).isEqualTo(204);
      csrf = session.headers().firstValue("X-XSRF-TOKEN").orElseThrow();
      assertThat(send("/api/praxis/runtime/context", "GET", null, null).body()).contains(username);
    }
    HttpResponse<String> send(String path, String method, String body, String type) throws Exception {
      var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:8080" + path)).timeout(Duration.ofSeconds(20)).header("Origin", "http://127.0.0.1:4303");
      if (csrf != null && !method.equals("GET")) builder.header("X-XSRF-TOKEN", csrf);
      if (type != null) builder.header("Content-Type", type);
      return client.send(builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }
    HttpResponse<String> write(String scope, String value) throws Exception {
      return send("/api/praxis/config/ui?componentType=praxis-table&componentId=auth-native-proof&scope=" + scope,
          "PUT", "{\"payload\":{\"proof\":\"" + value + "\"}}", "application/json");
    }
    HttpResponse<String> read() throws Exception {
      return send("/api/praxis/config/ui?componentType=praxis-table&componentId=auth-native-proof", "GET", null, null);
    }
    HttpResponse<String> writeConditional(String value, String etag) throws Exception {
      var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:8080/api/praxis/config/ui"
          + "?componentType=praxis-table&componentId=auth-native-proof&scope=user"))
          .timeout(Duration.ofSeconds(20)).header("Origin", "http://127.0.0.1:4303")
          .header("X-XSRF-TOKEN", csrf).header("Content-Type", "application/json").header("If-Match", etag)
          .PUT(HttpRequest.BodyPublishers.ofString("{\"payload\":{\"proof\":\"" + value + "\"}}")) .build();
      return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
  }
}
