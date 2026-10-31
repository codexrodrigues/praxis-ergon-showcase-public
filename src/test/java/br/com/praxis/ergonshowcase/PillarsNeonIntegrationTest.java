package br.com.praxis.ergonshowcase;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.praxis.ergonshowcase.domain.*;
import com.fasterxml.jackson.databind.*;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/** Provas dos contratos novos contra PostgreSQL real; toda alteração de fixture é revertida. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@WithMockUser(authorities = {"ERGON_READ", "ERGON_WRITE"})
class PillarsNeonIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired EntityManager em;
  @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
  @Autowired org.praxisplatform.uischema.openapi.OpenApiDocumentService documents;
  private NeonDomainFixture domainFixture;
  @org.junit.jupiter.api.BeforeEach void prepareDomainFixture() throws Exception {
    BulkPublicationProof.ensurePublished(documents, publicationHttp.withBasicAuth(username, password));
    domainFixture = new NeonDomainFixture();
  }
  @org.junit.jupiter.api.AfterEach void restoreDomainFixture() throws Exception {
    if (domainFixture != null) domainFixture.restore();
  }
  @Autowired org.springframework.boot.test.web.client.TestRestTemplate publicationHttp;
  @org.springframework.beans.factory.annotation.Value("${showcase.security.username}") String username;
  @org.springframework.beans.factory.annotation.Value("${showcase.security.password}") String password;

  private JsonNode postJson(String path, String body) throws Exception {
    return json.readTree(mvc.perform(post(path).contentType("application/json").content(body))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
  }

  @Test
  void broadSearchUsesTheSameCriterionForNameCpfAndParentage() throws Exception {
    jdbc.update("update funcionarios set mae=? where numero=1001", "Filiação exclusiva prova omnibox");
    var pessoa = em.find(ServidorPessoa.class, 1001L);
    for (String term : List.of(pessoa.getNome(), pessoa.getCpf(), pessoa.getMae())) {
      var result = postJson("/api/ergon/servidor-pessoas/filter",
          json.writeValueAsString(Map.of("buscaAmpla", term))).path("data").path("content");
      assertThat(result.size()).as("busca ampla por identidade ou filiação").isEqualTo(1);
      assertThat(result.get(0).path("id").asLong()).isEqualTo(1001L);
    }
    var absent = postJson("/api/ergon/servidor-pessoas/filter", "{\"buscaAmpla\":\"inexistente-omnibox-308\"}");
    assertThat(absent.path("data").path("content").size()).isZero();
    var vinculos = postJson("/api/ergon/vinculos/filter",
        json.writeValueAsString(Map.of("buscaAmpla", pessoa.getCpf()))).path("data").path("content");
    assertThat(vinculos.size()).isPositive();
    var identities = new HashSet<String>();
    for (var vinculo : vinculos) {
      assertThat(vinculo.path("id").asText()).startsWith("1001:");
      assertThat(identities.add(vinculo.path("id").asText())).isTrue();
    }
    var cargo = em.find(Cargo.class, 101);
    var cargos = postJson("/api/ergon/cargos/filter", json.writeValueAsString(Map.of("buscaAmpla", cargo.getNome())));
    assertThat(cargos.path("data").path("content").size()).isPositive();
  }

  @Test
  void sixRichDirectoriesFilterAndReloadActualIdentities() throws Exception {
    // Nome social é opcional; a massa original não o preenche. Prove o transporte
    // de um valor real sem exigir que campos ausentes produzam itens visuais vazios.
    var pessoa = em.createQuery("select p from ServidorPessoa p order by p.nome", ServidorPessoa.class)
        .setMaxResults(1).getSingleResult();
    jdbc.update("update funcionarios set nome_social=? where numero=?", "Nome social de prova", pessoa.getNumero());
    for (String[] resource : new String[][] {{"orgaos","orgao"},{"cargos","cargo"},
        {"servidor-pessoas","pessoa"},{"vinculos","vinculo"},{"setores","setor"},
        {"documentos-legais","documento"}}) {
      String base = "/api/ergon/" + resource[0] + "/option-sources/" + resource[1] + "/options";
      var page = postJson(base + "/filter", "{}").path("content");
      assertThat(page.isArray()).as(resource[0]).isTrue();
      assertThat(page.size()).isPositive();
      var id = page.get(0).get("id");
      var selected = postJson(base + "/by-ids", json.writeValueAsString(Map.of("ids", List.of(id))));
      assertThat(selected.size()).isEqualTo(1);
      assertThat(selected.get(0).path("id")).isEqualTo(id);
      assertThat(selected.get(0).path("extra").path("richFields").size()).isPositive();
      mvc.perform(post(base + "/filter").contentType("application/json")
          .content("{\"sort\":\"privateSqlColumn\"}"))
          .andExpect(status().isUnprocessableEntity());
    }
  }

  @Test
  void historicExtinctCargoIsRetainedButDisabledAndCompanyDependencyIsEffective() throws Exception {
    var cargo = em.createQuery("select c from Cargo c where c.empCodigo=1 order by c.cargo", Cargo.class)
        .setMaxResults(1).getSingleResult();
    jdbc.update("update cargos set dt_extincao=? where cargo=?", LocalDate.now().minusDays(1), cargo.getCargo());
    String base = "/api/ergon/cargos/option-sources/cargo/options";
    String ids = json.writeValueAsString(Map.of("ids", List.of(cargo.getCargo()), "filter", Map.of("empCodigo",1)));
    var selected = postJson(base + "/by-ids", ids);
    assertThat(selected.size()).isEqualTo(1);
    assertThat(selected.get(0).path("extra").path("selectable").asBoolean(true)).isFalse();
    assertThat(selected.get(0).path("extra").path("status").asText()).isEqualTo("EXTINTO");
    assertThat(postJson(base + "/by-ids", ids.replace("\"empCodigo\":1", "\"empCodigo\":2")).size()).isZero();
    for (var option : postJson(base + "/filter", "{\"filter\":{\"empCodigo\":2}}").path("content"))
      assertThat(option.path("id").asInt()).isNotEqualTo(cargo.getCargo());
  }

  @Test
  void nominalAnalyticsBucketsCarryRealIdsForThePublishedSurfaces() throws Exception {
    for (String[] resource : new String[][] {{"cargos","ficha-cargo"},
        {"servidor-pessoas","ficha-cadastral"},{"vinculos","resumo-funcional"}}) {
      String base = "/api/ergon/" + resource[0];
      var result = postJson(base + "/stats/group-by", "{\"field\":\"id\",\"metric\":{\"operation\":\"COUNT\"},\"filter\":{}}");
      var buckets = result.path("data").path("buckets");
      assertThat(buckets.size()).isPositive();
      assertThat(buckets.get(0).path("count").asLong()).isEqualTo(1L);
      String id = buckets.get(0).path("key").asText();
      assertThat(id).isNotBlank();
      mvc.perform(get(base + "/" + id + "/" + resource[1])).andExpect(status().isOk());
    }
  }

  @Test
  void analyticMeasuresUseRegisteredFieldsAndRejectDocumentSummation() throws Exception {
    var cargo = postJson("/api/ergon/cargos/stats/group-by",
        "{\"field\":\"id\",\"metric\":{\"operation\":\"SUM\",\"field\":\"vagasAutorizadas\"},\"filter\":{}}");
    assertThat(cargo.path("data").path("buckets").size()).isPositive();
    var vinculo = postJson("/api/ergon/vinculos/stats/group-by",
        "{\"field\":\"id\",\"metric\":{\"operation\":\"SUM\",\"field\":\"jornadaSemanal\"},\"filter\":{}}");
    assertThat(vinculo.path("data").path("buckets").size()).isPositive();
    mvc.perform(post("/api/ergon/servidor-pessoas/stats/group-by").contentType("application/json")
        .content("{\"field\":\"id\",\"metric\":{\"operation\":\"SUM\",\"field\":\"cpf\"},\"filter\":{}}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void publicSchemasActuallyPublishAnalyticsAndMicrovisualizations() throws Exception {
    // A publicação explícita do contexto precede a prova; não há transação externa.
    var openapi = json.readTree(mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString());
    for (String resource : List.of("cargos", "servidor-pessoas", "vinculos")) {
      var operation = openapi.path("paths").path("/api/ergon/" + resource + "/stats/group-by").path("post");
      assertThat(operation.toString()).contains("analytics", "recordOpen", "sourceIdentityField");
    }
    assertThat(openapi.path("components").path("schemas").toString())
        .contains("microVisualization", "canaisContatoPreenchidos", "vagasOcupadas");
  }
}
