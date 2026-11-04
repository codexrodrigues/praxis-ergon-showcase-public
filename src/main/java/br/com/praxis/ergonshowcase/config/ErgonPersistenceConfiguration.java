package br.com.praxis.ergonshowcase.config;

import java.net.URI;
import jakarta.persistence.EntityManagerFactory;
import java.util.Map;
import javax.sql.DataSource;
import com.zaxxer.hikari.HikariDataSource;
import org.praxisplatform.config.tx.ConfigTransactionManagerNames;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

@Configuration(proxyBeanMethods = false)
@EnableJpaRepositories(basePackages = "br.com.praxis.ergonshowcase")
public class ErgonPersistenceConfiguration {
  @Bean
  @Primary
  JdbcTemplate jdbcTemplate(@Qualifier("dataSource") DataSource source) {
    return new JdbcTemplate(source);
  }

  @Bean
  @Primary
  NamedParameterJdbcTemplate namedParameterJdbcTemplate(@Qualifier("jdbcTemplate") JdbcTemplate jdbc) {
    return new NamedParameterJdbcTemplate(jdbc);
  }

  @Bean
  @Primary
  LocalContainerEntityManagerFactoryBean entityManagerFactory(EntityManagerFactoryBuilder builder,
      @Qualifier("dataSource") DataSource dataSource,
      @Value("${spring.jpa.hibernate.ddl-auto:validate}") String ddlAuto) {
    return builder.dataSource(dataSource).packages("br.com.praxis.ergonshowcase")
        .persistenceUnit("ergon").properties(Map.of("hibernate.hbm2ddl.auto", ddlAuto)).build();
  }

  @Configuration(proxyBeanMethods = false)
  @EnableJpaRepositories(basePackages = "org.praxisplatform.config.repository",
      entityManagerFactoryRef = "configEntityManagerFactory",
      transactionManagerRef = ConfigTransactionManagerNames.CONFIG)
  static class ConfigStore {
    @Bean
    HikariDataSource configDataSource(@Qualifier("dataSource") DataSource domain,
        @Value("${config.datasource.url}") String url,
        @Value("${config.datasource.username}") String username,
        @Value("${config.datasource.password}") String password,
        @Value("${config.datasource.schema}") String schema,
        @Value("${spring.datasource.username}") String domainUsername,
        @Value("${spring.datasource.url}") String domainUrl) {
      if (!url.startsWith("jdbc:postgresql://") || username.isBlank() || password.isBlank()) {
        throw new IllegalArgumentException("Config exige JDBC PostgreSQL e credenciais explícitas.");
      }
      if (!schema.matches("[a-z_][a-z0-9_]*")) {
        throw new IllegalArgumentException("Config exige schema PostgreSQL explícito.");
      }
      if (username.equals(domainUsername)
          || databaseName(url).equals(databaseName(domainUrl))) {
        throw new IllegalArgumentException("Config exige banco e usuário distintos do domínio.");
      }
      var source = new HikariDataSource();
      source.setJdbcUrl(url);
      source.setUsername(username);
      source.setPassword(password);
      source.setSchema(schema);
      source.setPoolName("ergon-config");
      source.setMaximumPoolSize(5);
      try (var domainConnection = domain.getConnection();
          var connection = source.getConnection();
          var statement = connection.createStatement();
          var actual = statement.executeQuery("select current_database(), current_user, current_schema()")) {
        if (actual.next() && databaseName(url).equals(actual.getString(1))
            && username.equals(actual.getString(2)) && schema.equals(actual.getString(3))
            && !actual.getString(1).equals(domainConnection.getCatalog())
            && !actual.getString(2).equals(domainConnection.getMetaData().getUserName())) {
          return source;
        }
      } catch (java.sql.SQLException failure) {
        source.close();
        throw new IllegalStateException("Conexão Config inválida. SQLSTATE=" + failure.getSQLState());
      }
      source.close();
      throw new IllegalStateException("Banco, usuário ou schema efetivo diverge da configuração Config.");
    }

    private static String databaseName(String url) {
      if (!url.startsWith("jdbc:postgresql:")) return url;
      if (!url.startsWith("jdbc:postgresql://")) {
        return url.substring("jdbc:postgresql:".length()).split("\\?", 2)[0];
      }
      String path = URI.create(url.substring("jdbc:".length())).getPath();
      if (path == null || !path.matches("/[^/]+")) {
        throw new IllegalArgumentException("Informe o nome do banco no JDBC PostgreSQL.");
      }
      return path.substring(1);
    }

    @Bean
    JdbcTemplate configJdbcTemplate(@Qualifier("configDataSource") DataSource source) {
      return new JdbcTemplate(source);
    }

    @Bean
    NamedParameterJdbcTemplate configNamedParameterJdbcTemplate(
        @Qualifier("configJdbcTemplate") JdbcTemplate jdbc) {
      return new NamedParameterJdbcTemplate(jdbc);
    }

    @Bean
    LocalContainerEntityManagerFactoryBean configEntityManagerFactory(EntityManagerFactoryBuilder builder,
        @Qualifier("configDataSource") DataSource dataSource,
        @Value("${config.datasource.schema}") String schema) {
      return builder.dataSource(dataSource).packages("org.praxisplatform.config.domain")
          .persistenceUnit("config").properties(Map.of(
              "hibernate.hbm2ddl.auto", "validate", "hibernate.default_schema", schema)).build();
    }

    @Bean(ConfigTransactionManagerNames.CONFIG)
    JpaTransactionManager configTransactionManager(
        @Qualifier("configEntityManagerFactory") EntityManagerFactory factory) {
      return new JpaTransactionManager(factory);
    }
  }
}
