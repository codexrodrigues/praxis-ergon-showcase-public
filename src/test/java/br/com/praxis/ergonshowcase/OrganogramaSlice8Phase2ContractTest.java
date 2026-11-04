package br.com.praxis.ergonshowcase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import br.com.praxis.ergonshowcase.bulk.ErgonBulkOperations;
import br.com.praxis.ergonshowcase.controller.*;
import br.com.praxis.ergonshowcase.service.*;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.praxisplatform.uischema.configuration.OpenApiResponseGenerationAutoConfiguration;
import org.praxisplatform.uischema.configuration.OpenApiUiSchemaAutoConfiguration;
import org.praxisplatform.uischema.configuration.PraxisMetadataAutoConfiguration;
import org.praxisplatform.uischema.controller.docs.OpenApiDocsSupport;
import org.praxisplatform.uischema.fact.ResourceFactCatalogProvider;
import org.praxisplatform.uischema.fact.ResourceFactDescriptor;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.webmvc.core.configuration.MultipleOpenApiSupportConfiguration;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

/** Exercita a publicação Springdoc e o schema estrutural consumido pela Table. */
@SpringBootTest(classes = OrganogramaSlice8Phase2ContractTest.SchemaFixture.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"praxis.domain-catalog.auto-sync.enabled=false",
        "praxis.resource-version.etag.secret=fixture-organograma-schema-local"})
@MockitoBean(types = {HistoricoSetorService.class, SetorParametrizacaoErgonService.class,
    CargoSetorService.class, DesignacaoFuncaoService.class, RadarOrganogramaChefiaOcupacaoService.class,
    SubempresaLocalidadeService.class, GrupoSetorService.class, ErgonBulkOperations.class,
    ErgonDeterminationService.class})
class OrganogramaSlice8Phase2ContractTest {
  private static final String TREE = "/api/ergon/setores/arvore-organograma";
  @LocalServerPort int port;
  @Autowired TestRestTemplate rest;
  @Autowired OpenApiDocsSupport docs;
  @Autowired ResourceFactCatalogProvider facts;
  @MockitoBean SetorService sectors;

  @Test
  void treeDiscoveryFollowsTheRealGetSchemaAndKeepsStringIdentity() {
    String origin = "http://localhost:" + port;
    when(sectors.getIdFieldName()).thenReturn("id");
    ReflectionTestUtils.setField(docs, "openApiInternalBaseUrl", origin);
    var catalog = rest.getForEntity("/schemas/surfaces?resource=administracao-pessoal.setores", JsonNode.class);
    assertThat(catalog.getStatusCode().value()).isEqualTo(200);
    var tree = catalog.getBody().path("surfaces").valueStream()
        .filter(surface -> surface.path("id").asText().equals("organograma-tree")).findFirst().orElseThrow();
    assertThat(tree.path("kind").asText()).isEqualTo("HIERARCHY_TREE");
    assertThat(tree.path("scope").asText()).isEqualTo("COLLECTION");
    assertThat(tree.path("method").asText()).isEqualTo("GET");
    assertThat(tree.path("path").asText()).isEqualTo(TREE);
    var children = Map.of("historico-vigencias", "setor", "parametrizacoes-ergon", "setor",
        "cargos-autorizados", "setor", "designacoes-chefia", "setorId", "radar-subarvore", "raizSubarvore");
    children.forEach((id, field) -> {
      var related = catalog.getBody().path("surfaces").valueStream()
          .filter(surface -> surface.path("id").asText().equals(id)).findFirst().orElseThrow().path("relatedResource");
      assertThat(related.path("childParentField").asText()).isEqualTo(field);
      assertThat(related.path("childOperations").valueStream().map(JsonNode::asText)).containsExactly("FILTER");
    });
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
    Map.of(
        "setores", List.of("tipoSetor", "quantidadeSubordinadosDiretos"),
        "historico-setores", List.of("diagnosticoHierarquia"),
        "setores-ergon", List.of("centroCusto"),
        "cargos-por-setor", List.of("lotacaoMaximaAutorizada"),
        "subempresas-localidades", List.of("naturezaUnidade"),
        "grupos-setores", List.of("tipoAgrupamento"),
        "radar-organograma-chefias-ocupacao", List.of("situacaoComandoSetor", "vagasAutorizadasSetor", "escopoApuracao"))
        .forEach((resource, keys) -> assertThat(facts.findFacts("administracao-pessoal." + resource, "1"))
            .extracting(ResourceFactDescriptor::key).containsExactlyInAnyOrderElementsOf(keys));
  }

  @Configuration(proxyBeanMethods = false)
  @ImportAutoConfiguration({ServletWebServerFactoryAutoConfiguration.class, DispatcherServletAutoConfiguration.class,
      WebMvcAutoConfiguration.class, JacksonAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class,
      PraxisMetadataAutoConfiguration.class, OpenApiUiSchemaAutoConfiguration.class,
      OpenApiResponseGenerationAutoConfiguration.class, SpringDocConfiguration.class, SpringDocConfigProperties.class,
      SpringDocWebMvcConfiguration.class, MultipleOpenApiSupportConfiguration.class})
  @Import({SetorController.class, HistoricoSetorController.class, SetorParametrizacaoErgonController.class,
      CargoSetorController.class, DesignacaoFuncaoController.class, RadarOrganogramaChefiaOcupacaoController.class,
      SubempresaLocalidadeController.class, GrupoSetorController.class})
  static class SchemaFixture { }
}
