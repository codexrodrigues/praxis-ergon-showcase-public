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

  private BulkOperationDescriptorProvider provider(String resource, String suffix, BulkMode mode, BulkExecutionInfrastructure infrastructure) {
    return new BulkOperationDescriptorProvider() {
      public String confirmationOperationId() {
        var designacoes = ErgonBulkDomain.Resource.DESIGNACOES;
        return designacoes.name.equals(resource) && mode != BulkMode.DOMAIN_COMMAND
            ? ErgonBulkOperations.operation(designacoes, mode) : "ergon." + resource + "." + suffix;
      }
      public String providerId() { return "ergon-bulk-" + resource + "-" + suffix; }
      public String providerRevision() { return ErgonBulkOperations.REVISION; }
      public BulkIdentityCodec<?, ?> identityCodec() {
        return switch (resource) {
          case "cargos" -> BulkIdentityCodecs.integers();
          case "vinculos" -> BulkIdentityCodecs.composite(br.com.praxis.ergonshowcase.domain.VinculoId.class);
          case "dependentes" -> BulkIdentityCodecs.composite(br.com.praxis.ergonshowcase.domain.DependenteId.class);
          default -> BulkIdentityCodecs.longs();
        };
      }
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
  @Bean BulkOperationDescriptorProvider dependenteUniform(BulkExecutionInfrastructure i) { return provider("dependentes", "bulk.uniform", BulkMode.UNIFORM_UPDATE, i); }
  @Bean BulkOperationDescriptorProvider dependenteItems(BulkExecutionInfrastructure i) { return provider("dependentes", "bulk.items", BulkMode.PER_ITEM_UPDATE, i); }
  @Bean BulkOperationDescriptorProvider afastamentoUniform(BulkExecutionInfrastructure i) { return provider("afastamentos", "bulk.uniform", BulkMode.UNIFORM_UPDATE, i); }
  @Bean BulkOperationDescriptorProvider afastamentoItems(BulkExecutionInfrastructure i) { return provider("afastamentos", "bulk.items", BulkMode.PER_ITEM_UPDATE, i); }
  @Bean BulkOperationDescriptorProvider frequenciaUniform(BulkExecutionInfrastructure i) { return provider("frequencias", "bulk.uniform", BulkMode.UNIFORM_UPDATE, i); }
  @Bean BulkOperationDescriptorProvider frequenciaItems(BulkExecutionInfrastructure i) { return provider("frequencias", "bulk.items", BulkMode.PER_ITEM_UPDATE, i); }
  @Bean BulkOperationDescriptorProvider campoCustomizadoUniform(BulkExecutionInfrastructure i) { return provider("campos-customizados", "bulk.uniform", BulkMode.UNIFORM_UPDATE, i); }
  @Bean BulkOperationDescriptorProvider campoCustomizadoItems(BulkExecutionInfrastructure i) { return provider("campos-customizados", "bulk.items", BulkMode.PER_ITEM_UPDATE, i); }
  @Bean BulkOperationDescriptorProvider parametroContagemFeriasUniform(BulkExecutionInfrastructure i) { return provider("parametros-contagem-ferias", "bulk.uniform", BulkMode.UNIFORM_UPDATE, i); }
  @Bean BulkOperationDescriptorProvider parametroContagemFeriasItems(BulkExecutionInfrastructure i) { return provider("parametros-contagem-ferias", "bulk.items", BulkMode.PER_ITEM_UPDATE, i); }
  @Bean BulkOperationDescriptorProvider periodoAquisitivoFeriasUniform(BulkExecutionInfrastructure i) { return provider("periodos-aquisitivos-ferias", "bulk.uniform", BulkMode.UNIFORM_UPDATE, i); }
  @Bean BulkOperationDescriptorProvider periodoAquisitivoFeriasItems(BulkExecutionInfrastructure i) { return provider("periodos-aquisitivos-ferias", "bulk.items", BulkMode.PER_ITEM_UPDATE, i); }
  @Bean BulkOperationDescriptorProvider feriasGozoUniform(BulkExecutionInfrastructure i) { return provider("ferias-gozo", "bulk.uniform", BulkMode.UNIFORM_UPDATE, i); }
  @Bean BulkOperationDescriptorProvider feriasGozoItems(BulkExecutionInfrastructure i) { return provider("ferias-gozo", "bulk.items", BulkMode.PER_ITEM_UPDATE, i); }
  @Bean BulkOperationDescriptorProvider realocarSetor(BulkExecutionInfrastructure i) { return provider("vinculos", "realocar-setor-lote", BulkMode.DOMAIN_COMMAND, i); }
  @Bean BulkOperationDescriptorProvider homologarEstagio(BulkExecutionInfrastructure i) { return provider("vinculos", "homologar-estagio-lote", BulkMode.DOMAIN_COMMAND, i); }
  @Bean BulkOperationDescriptorProvider encerrarDependenciaLote(BulkExecutionInfrastructure i) { return provider("dependentes", "encerrar-dependencia-lote", BulkMode.DOMAIN_COMMAND, i); }
  @Bean BulkOperationDescriptorProvider homologarAfastamentoLote(BulkExecutionInfrastructure i) { return provider("afastamentos", "homologar-afastamento-lote", BulkMode.DOMAIN_COMMAND, i); }
  @Bean BulkOperationDescriptorProvider homologarFrequenciaLote(BulkExecutionInfrastructure i) { return provider("frequencias", "homologar-frequencia-lote", BulkMode.DOMAIN_COMMAND, i); }
  @Bean BulkOperationDescriptorProvider ativarCampoCustomizadoLote(BulkExecutionInfrastructure i) { return provider("campos-customizados", "ativar-campo-customizado-lote", BulkMode.DOMAIN_COMMAND, i); }
  @Bean BulkOperationDescriptorProvider ativarParametroFeriasLote(BulkExecutionInfrastructure i) { return provider("parametros-contagem-ferias", "ativar-parametro-ferias-lote", BulkMode.DOMAIN_COMMAND, i); }
  @Bean BulkOperationDescriptorProvider homologarPeriodoAquisitivoLote(BulkExecutionInfrastructure i) { return provider("periodos-aquisitivos-ferias", "homologar-periodo-aquisitivo-lote", BulkMode.DOMAIN_COMMAND, i); }
  @Bean BulkOperationDescriptorProvider homologarFeriasGozoLote(BulkExecutionInfrastructure i) { return provider("ferias-gozo", "homologar-ferias-gozo-lote", BulkMode.DOMAIN_COMMAND, i); }

  @Bean BulkOperationDescriptorProvider averbacaoTempoServicoUniform(BulkExecutionInfrastructure i) { return provider("averbacoes-tempo-servico","bulk.uniform",BulkMode.UNIFORM_UPDATE,i); }
  @Bean BulkOperationDescriptorProvider averbacaoTempoServicoItems(BulkExecutionInfrastructure i) { return provider("averbacoes-tempo-servico","bulk.items",BulkMode.PER_ITEM_UPDATE,i); }
  @Bean BulkOperationDescriptorProvider averbacaoTempoServicoCommand(BulkExecutionInfrastructure i) { return provider("averbacoes-tempo-servico","homologar-averbacoes-lote",BulkMode.DOMAIN_COMMAND,i); }

  @Bean BulkOperationDescriptorProvider periodoAquisitivoLicencaEspecialUniform(BulkExecutionInfrastructure i) { return provider("periodos-aquisitivos-licenca-especial","bulk.uniform",BulkMode.UNIFORM_UPDATE,i); }
  @Bean BulkOperationDescriptorProvider periodoAquisitivoLicencaEspecialItems(BulkExecutionInfrastructure i) { return provider("periodos-aquisitivos-licenca-especial","bulk.items",BulkMode.PER_ITEM_UPDATE,i); }
  @Bean BulkOperationDescriptorProvider periodoAquisitivoLicencaEspecialCommand(BulkExecutionInfrastructure i) { return provider("periodos-aquisitivos-licenca-especial","recalcular-saldos-licenca-especial-lote",BulkMode.DOMAIN_COMMAND,i); }

  @Bean BulkOperationDescriptorProvider licencaEspecialGozoUniform(BulkExecutionInfrastructure i) { return provider("licencas-especiais-gozo","bulk.uniform",BulkMode.UNIFORM_UPDATE,i); }
  @Bean BulkOperationDescriptorProvider licencaEspecialGozoItems(BulkExecutionInfrastructure i) { return provider("licencas-especiais-gozo","bulk.items",BulkMode.PER_ITEM_UPDATE,i); }
  @Bean BulkOperationDescriptorProvider licencaEspecialGozoCommand(BulkExecutionInfrastructure i) { return provider("licencas-especiais-gozo","homologar-licencas-especiais-lote",BulkMode.DOMAIN_COMMAND,i); }

  @Bean BulkOperationDescriptorProvider vendaLicencaEspecialUniform(BulkExecutionInfrastructure i) { return provider("vendas-licenca-especial","bulk.uniform",BulkMode.UNIFORM_UPDATE,i); }
  @Bean BulkOperationDescriptorProvider vendaLicencaEspecialItems(BulkExecutionInfrastructure i) { return provider("vendas-licenca-especial","bulk.items",BulkMode.PER_ITEM_UPDATE,i); }
  @Bean BulkOperationDescriptorProvider vendaLicencaEspecialCommand(BulkExecutionInfrastructure i) { return provider("vendas-licenca-especial","autorizar-vendas-licenca-especial-lote",BulkMode.DOMAIN_COMMAND,i); }

  @Bean BulkOperationDescriptorProvider designacaoUniform(BulkExecutionInfrastructure i) {
    return provider(ErgonBulkDomain.Resource.DESIGNACOES.name, "bulk.update", BulkMode.UNIFORM_UPDATE, i);
  }
  @Bean BulkOperationDescriptorProvider designacaoItems(BulkExecutionInfrastructure i) {
    return provider(ErgonBulkDomain.Resource.DESIGNACOES.name, "bulk.update-items", BulkMode.PER_ITEM_UPDATE, i);
  }
  @Bean BulkOperationDescriptorProvider designacaoCommand(BulkExecutionInfrastructure i) {
    return provider(ErgonBulkDomain.Resource.DESIGNACOES.name, "homologar-designacoes-lote", BulkMode.DOMAIN_COMMAND, i);
  }

  @Bean BulkOperationDescriptorProvider funcaoConfiancaUniform(BulkExecutionInfrastructure i) {
    return provider(ErgonBulkDomain.Resource.FUNCOES.name, "bulk.uniform", BulkMode.UNIFORM_UPDATE, i);
  }
  @Bean BulkOperationDescriptorProvider funcaoConfiancaItems(BulkExecutionInfrastructure i) {
    return provider(ErgonBulkDomain.Resource.FUNCOES.name, "bulk.items", BulkMode.PER_ITEM_UPDATE, i);
  }
  @Bean BulkOperationDescriptorProvider vagaFuncaoUniform(BulkExecutionInfrastructure i) {
    return provider(ErgonBulkDomain.Resource.VAGAS_FUNCAO.name, "bulk.uniform", BulkMode.UNIFORM_UPDATE, i);
  }
  @Bean BulkOperationDescriptorProvider vagaFuncaoItems(BulkExecutionInfrastructure i) {
    return provider(ErgonBulkDomain.Resource.VAGAS_FUNCAO.name, "bulk.items", BulkMode.PER_ITEM_UPDATE, i);
  }
  @Bean BulkOperationDescriptorProvider substituicaoFuncaoUniform(BulkExecutionInfrastructure i) {
    return provider(ErgonBulkDomain.Resource.SUBSTITUICOES.name, "bulk.uniform", BulkMode.UNIFORM_UPDATE, i);
  }
  @Bean BulkOperationDescriptorProvider substituicaoFuncaoItems(BulkExecutionInfrastructure i) {
    return provider(ErgonBulkDomain.Resource.SUBSTITUICOES.name, "bulk.items", BulkMode.PER_ITEM_UPDATE, i);
  }
}
