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
import org.springframework.transaction.annotation.Transactional;

/** Provas dos contratos novos contra PostgreSQL real; toda alteraÃ§Ã£o de fixture Ã© revertida. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
@WithMockUser(authorities = {"ERGON_READ", "ERGON_WRITE"})
class PillarsNeonIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired EntityManager em;
  @Autowired org.springframework.boot.test.web.client.TestRestTemplate publicationHttp;
  @org.springframework.beans.factory.annotation.Value("${showcase.security.username}") String username;
  @org.springframework.beans.factory.annotation.Value("${showcase.security.password}") String password;

  private JsonNode postJson(String path, String body) throws Exception {
    return json.readTree(mvc.perform(post(path).contentType("application/json").content(body))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
  }

  @Test
  void sixRichDirectoriesFilterAndReloadActualIdentities() throws Exception {
    // Nome social é opcional; a massa original não o preenche. Prove o transporte
    // de um valor real sem exigir que campos ausentes produzam itens visuais vazios.
    var pessoa = em.createQuery("select p from ServidorPessoa p order by p.nome", ServidorPessoa.class)
        .setMaxResults(1).getSingleResult();
    pessoa.setNomeSocial("Nome social de prova");
    em.flush();
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
    cargo.setDtExtincao(LocalDate.now().minusDays(1));
    em.flush(); em.clear();
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
    // PublicaÃ§Ã£o Ã© uma operaÃ§Ã£o administrativa explÃ­cita, fora da transaÃ§Ã£o de fixtures.
    org.springframework.test.context.transaction.TestTransaction.end();
    BulkPublicationProof.publish(publicationHttp.withBasicAuth(username, password), false);
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
