package br.com.praxis.ergonshowcase.bulk;

import br.com.praxis.ergonshowcase.operations.InstallBulkStorage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.*;
import java.time.Duration;
import java.util.*;
import javax.crypto.spec.SecretKeySpec;
import javax.sql.DataSource;
import org.praxisplatform.uischema.bulk.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Composição explícita: runtime JPA/JDBC comum e control plane com identidade separada. */
@Configuration
@EnableConfigurationProperties(DataSourceProperties.class)
public class ErgonBulkConfiguration {
  @Bean @Primary
  HikariDataSource dataSource(DataSourceProperties properties) {
    var source = properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    source.setMaximumPoolSize(5); return source;
  }
  @Bean @Primary
  JpaTransactionManager transactionManager(jakarta.persistence.EntityManagerFactory factory) { return new JpaTransactionManager(factory); }
  @Bean
  BulkExecutionInfrastructure ergonBulkInfrastructure(DataSource dataSource, PlatformTransactionManager transactionManager,
      @Value("${BULK_OWNER_USERNAME}") String owner) {
    return new BulkExecutionInfrastructure(dataSource, transactionManager, InstallBulkStorage.NAMESPACE,
        InstallBulkStorage.DEPLOYMENT, new BulkExecutionRoleConfiguration(owner,
        Set.of(InstallBulkStorage.RUNTIME), Set.of(), Set.of(InstallBulkStorage.CONTROL)));
  }
  @Bean(destroyMethod = "close")
  HikariDataSource ergonBulkControlDataSource(DataSourceProperties properties, @Value("${BULK_CONTROL_PASSWORD}") String password) {
    var config = new HikariConfig(); config.setJdbcUrl(properties.getUrl());
    config.setUsername(InstallBulkStorage.CONTROL); config.setPassword(password); config.setMaximumPoolSize(2);
    config.setPoolName("ergon-bulk-control"); return new HikariDataSource(config);
  }
  @Bean
  BulkControlPlaneInfrastructure ergonBulkControl(@Qualifier("ergonBulkControlDataSource") DataSource source, BulkExecutionInfrastructure runtime) {
    return new BulkControlPlaneInfrastructure(source, new DataSourceTransactionManager(source),
        runtime.namespace(), runtime.deploymentId(), InstallBulkStorage.CONTROL, runtime);
  }
  @Bean
  BulkReadCursorConfiguration ergonBulkCursors(@Value("${BULK_CURSOR_KEY}") String key) {
    return new BulkReadCursorConfiguration("ergon-1", Map.of("ergon-1", new SecretKeySpec(Base64.getDecoder().decode(key), "AES")), Duration.ofMinutes(5));
  }
  @Bean
  WebMvcConfigurer ergonBulkTransport(ObjectMapper mapper, List<org.praxisplatform.uischema.filter.web.FilterPayloadNormalizer> normalizers) {
    return new WebMvcConfigurer() {
      @Override public void extendMessageConverters(List<org.springframework.http.converter.HttpMessageConverter<?>> converters) {
        converters.addFirst(new ErgonBulkMessageConverter(mapper,normalizers));
      }
    };
  }
  private BulkOperationDescriptorProvider provider(String resource, String suffix, BulkMode mode, BulkExecutionInfrastructure infrastructure) {
    return new BulkOperationDescriptorProvider() {
      public String confirmationOperationId() { return "ergon." + resource + "." + suffix; }
      public String providerId() { return "ergon-bulk-" + resource + "-" + suffix; }
      public String providerRevision() { return ErgonBulkOperations.REVISION; }
      public BulkIdentityCodec<?, ?> identityCodec() { return resource.equals("cargos") ? BulkIdentityCodecs.integers() : resource.equals("funcionarios") ? BulkIdentityCodecs.longs() : BulkIdentityCodecs.strings(); }
      public String identitySchemaPointer() { return mode == BulkMode.PER_ITEM_UPDATE ? "/properties/items/items/properties/id" : "/properties/selection/properties/targets/items/properties/id"; }
      public BulkOperationalProfile profile() {
        return new BulkOperationalProfile(Set.of(mode), Set.of(BulkExecutionMode.SYNC),
            mode == BulkMode.UNIFORM_UPDATE ? Set.of(BulkSelectionMode.EXPLICIT, BulkSelectionMode.QUERY) : Set.of(BulkSelectionMode.EXPLICIT),
            200, 131072, Duration.ofMinutes(10), Duration.ofSeconds(5));
      }
      public BulkExecutionInfrastructure executionInfrastructure() { return infrastructure; }
    };
  }
  @Bean BulkOperationDescriptorProvider vinculoUniform(BulkExecutionInfrastructure i) { return provider("vinculos", "bulk.uniform", BulkMode.UNIFORM_UPDATE, i); }
  @Bean BulkOperationDescriptorProvider vinculoItems(BulkExecutionInfrastructure i) { return provider("vinculos", "bulk.items", BulkMode.PER_ITEM_UPDATE, i); }
  @Bean BulkOperationDescriptorProvider pessoaUniform(BulkExecutionInfrastructure i) { return provider("funcionarios", "bulk.uniform", BulkMode.UNIFORM_UPDATE, i); }
  @Bean BulkOperationDescriptorProvider pessoaItems(BulkExecutionInfrastructure i) { return provider("funcionarios", "bulk.items", BulkMode.PER_ITEM_UPDATE, i); }
  @Bean BulkOperationDescriptorProvider cargoUniform(BulkExecutionInfrastructure i) { return provider("cargos", "bulk.uniform", BulkMode.UNIFORM_UPDATE, i); }
  @Bean BulkOperationDescriptorProvider cargoItems(BulkExecutionInfrastructure i) { return provider("cargos", "bulk.items", BulkMode.PER_ITEM_UPDATE, i); }
  @Bean BulkOperationDescriptorProvider realocarSetor(BulkExecutionInfrastructure i) { return provider("vinculos", "realocar-setor-lote", BulkMode.DOMAIN_COMMAND, i); }
  @Bean BulkOperationDescriptorProvider homologarEstagio(BulkExecutionInfrastructure i) { return provider("vinculos", "homologar-estagio-lote", BulkMode.DOMAIN_COMMAND, i); }
}
