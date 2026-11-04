package br.com.praxis.ergonshowcase;

import br.com.praxis.ergonshowcase.service.TempoLicencaCommandGateway;
import br.com.praxis.ergonshowcase.config.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.springframework.orm.jpa.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.praxisplatform.uischema.concurrency.ResourceRepresentationResult;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

/** Real production gateway with Hibernate/JpaTransactionManager and isolated PostgreSQL. */
public class GatewayPostgresProbe {
  public record Reply(String status) {}
  public static void main(String[] args) throws Exception {
    var root=Path.of("docs/evidence/slice6-phase2-corrections-review").toAbsolutePath().normalize();
    var data=root.resolve("pgdata-gateway").normalize();
    if(!data.startsWith(root))throw new IllegalStateException("Invalid data directory");
    try(var pg=EmbeddedPostgres.builder().setDataDirectory(data).setCleanDataDirectory(false)
        .setServerConfig("listen_addresses","127.0.0.1").setPort(0).start()) {
      var ds=pg.getPostgresDatabase();
      try(var c=ds.getConnection();var st=c.createStatement()) {
        st.execute("create table if not exists tempo_licenca_command_receipts(receipt_key varchar(64) primary key, fingerprint varchar(64), response_body text, response_version bigint)");
      }
      var registry=new StandardServiceRegistryBuilder()
          .applySetting("hibernate.connection.datasource",ds)
          .applySetting("hibernate.hbm2ddl.auto","none")
          .applySetting("hibernate.show_sql",false).build();
      try(var sf=new MetadataSources(registry).buildMetadata().buildSessionFactory()) {
        var em=SharedEntityManagerCreator.createSharedEntityManager(sf);
        var manager=new JpaTransactionManager(sf);manager.setDataSource(ds);manager.afterPropertiesSet();
        var tenant=mock(TenantContext.class);when(tenant.orgao()).thenReturn(1);
        var gateway=new TempoLicencaCommandGateway(new ObjectMapper().findAndRegisterModules(),tenant,manager);
        ReflectionTestUtils.setField(gateway,"em",em);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("test-operator","unused",
            List.of(new SimpleGrantedAuthority("ERGON_WRITE"))));
        var effects=new AtomicInteger();String key=UUID.randomUUID().toString();
        try {
          var result=gateway.execute("test.resource","/test",91L,"test-action",key,"test-etag",Map.of("reason","test"),Reply.class,
              ()->{effects.incrementAndGet();return ResourceRepresentationResult.versioned(new Reply("done"),1L);});
          assertThat(result.body().status()).isEqualTo("done");
          var replay=gateway.execute("test.resource","/test",91L,"test-action",key,"test-etag",Map.of("reason","test"),Reply.class,
              ()->{effects.incrementAndGet();throw new AssertionError("Duplicate effect");});
          assertThat(replay.body()).isEqualTo(result.body());assertThat(effects.get()).isEqualTo(1);
          System.out.println("GATEWAY_POSTGRES_CREATE_AND_REPLAY=PASS; effects=1");
        } catch(Throwable t) {
          System.out.println("GATEWAY_POSTGRES_FAILURE; effects="+effects.get());t.printStackTrace();throw t;
        } finally { SecurityContextHolder.clearContext(); }
      } finally { StandardServiceRegistryBuilder.destroy(registry); }
    }
  }
}
