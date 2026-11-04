package br.com.praxis.ergonshowcase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.praxis.ergonshowcase.controller.SetorController;
import br.com.praxis.ergonshowcase.service.SetorService;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.praxisplatform.uischema.controller.docs.OpenApiDocsSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;

/** Exercita a publicação Springdoc e o schema estrutural consumido pela Table. */
@SpringBootTest(classes = OrganogramaSlice8Phase2ContractTest.SchemaFixture.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"spring.ai.enabled=false", "praxis.ai.registry.health.enabled=false",
        "praxis.ai.registry.bootstrap.enabled=false", "praxis.domain-catalog.auto-sync.enabled=false",
        "praxis.resource-version.etag.secret=fixture-organograma-schema-local"})
class OrganogramaSlice8Phase2ContractTest {
  private static final String TREE = "/api/ergon/setores/arvore-organograma";
  @LocalServerPort int port;
  @Autowired TestRestTemplate rest;
  @Autowired OpenApiDocsSupport docs;

  @Test
  void treeDiscoveryFollowsTheRealGetSchemaAndKeepsStringIdentity() {
    String origin = "http://localhost:" + port;
    ReflectionTestUtils.setField(docs, "openApiInternalBaseUrl", origin);
    var catalog = rest.getForEntity("/schemas/surfaces?resource=administracao-pessoal.setores", JsonNode.class);
    assertThat(catalog.getStatusCode().value()).isEqualTo(200);
    var tree = catalog.getBody().path("surfaces").valueStream()
        .filter(surface -> surface.path("id").asText().equals("organograma-tree")).findFirst().orElseThrow();
    assertThat(tree.path("kind").asText()).isEqualTo("HIERARCHY_TREE");
    assertThat(tree.path("scope").asText()).isEqualTo("COLLECTION");
    assertThat(tree.path("method").asText()).isEqualTo("GET");
    assertThat(tree.path("path").asText()).isEqualTo(TREE);
    var operation = rest.getForObject("/v3/api-docs/ergon", JsonNode.class).path("paths").path(TREE).path("get");
    assertThat(operation.path("operationId").asText()).isEqualTo("consultarArvoreOrganograma");
    assertThat(operation.path("parameters").valueStream().map(p -> p.path("name").asText()))
        .contains("page", "size", "dataReferencia", "tipoHierarquia", "buscaAmpla", "includeIds");
    var response = rest.getForEntity(URI.create(origin + tree.path("schemaUrl").asText()), JsonNode.class);
    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getHeaders().getFirst("X-Schema-Hash")).isNotBlank();
    var schema = response.getBody();
    assertThat(schema.at("/properties/id/type").asText()).isEqualTo("string");
    assertThat(schema.at("/properties/nome/x-ui/resourceReference/valueField").asText()).isEqualTo("referenciaFicha");
    assertThat(schema.at("/properties/nome/x-ui/resourceReference/previewSurfaceId").asText()).isEqualTo("ficha-unidade");
    assertThat(schema.at("/x-ui/hierarchy/parentField").asText()).isEqualTo("setorPai");
  }

  @Configuration(proxyBeanMethods = false)
  @EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class,
      JpaRepositoriesAutoConfiguration.class, SecurityAutoConfiguration.class},
      excludeName = {"org.praxisplatform.config.autoconfig.ConfigSecurityAutoConfiguration",
          "org.praxisplatform.config.autoconfig.ConfigOpenApiAutoConfiguration"})
  static class SchemaFixture {
    @Bean
    SetorController setorController() {
      var service = mock(SetorService.class);
      when(service.getIdFieldName()).thenReturn("id");
      return new SetorController(service, null, null, null, null, null);
    }
  }
}
