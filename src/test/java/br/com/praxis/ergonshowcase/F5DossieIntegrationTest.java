package br.com.praxis.ergonshowcase;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    // The in-memory fixture exercises HTTP, SpringDoc and JPA reads. Bulk publication needs PostgreSQL.
    "spring.autoconfigure.exclude=org.praxisplatform.uischema.configuration.BulkOperationLifecycleAutoConfiguration",
    "showcase.security.username=f5-test", "showcase.security.password=f5-test",
    "BULK_OWNER_USERNAME=sa", "BULK_CONTROL_PASSWORD=f5-test",
    "BULK_CURSOR_KEY=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
    "praxis.resource-version.etag.secret=f5-local-test-key-012345678901234567890"
})
@DirtiesContext
class F5DossieIntegrationTest {
  private static final String PARENT = "/api/ergon/vinculos/1001:1";

  @DynamicPropertySource
  static void isolatedDatabase(DynamicPropertyRegistry properties) throws SQLException {
    String url = "jdbc:h2:mem:f5-dossie;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
    try (var connection = DriverManager.getConnection(url, "sa", "");
        var statement = connection.createStatement()) {
      statement.execute("CREATE USER IF NOT EXISTS ergon_showcase_bulk_control PASSWORD 'f5-test' ADMIN");
    }
    properties.add("spring.datasource.url", () -> url);
    properties.add("config.datasource.url", () -> requiredConfig("CONFIG_DATASOURCE_URL"));
    properties.add("config.datasource.username", () -> requiredConfig("CONFIG_DATASOURCE_USERNAME"));
    properties.add("config.datasource.password", () -> requiredConfig("CONFIG_DATASOURCE_PASSWORD"));
    properties.add("config.datasource.schema", () -> requiredConfig("CONFIG_DATASOURCE_SCHEMA"));
  }

  private static String requiredConfig(String key) {
    String value = System.getenv(key);
    if (value == null || value.isBlank()) {
      throw new IllegalStateException("A fixture F5 exige a unidade Config de teste explícita: " + key);
    }
    return value;
  }

  @Autowired private TestRestTemplate rest;
  @Autowired private JdbcTemplate jdbc;

  @Test
  void followsContextualRelationsAndFiltersByTheFullVinculoIdentity() {
    seed();
    var operator = rest.withBasicAuth("f5-test", "f5-test");
    var headers = new HttpHeaders();
    headers.set("X-Tenant", "1");

    var item = operator.exchange(PARENT, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);
    assertThat(item.getStatusCode()).isEqualTo(HttpStatus.OK);
    var links = item.getBody().path("_links");
    if (links.isMissingNode()) links = item.getBody().path("data").path("_links");
    String href = links.path("surfaces").path("href").asText();
    assertThat(href).isNotBlank();

    var catalog = operator.exchange(href, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);
    assertThat(catalog.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertRelation(operator, headers, catalog.getBody(), "designacoes", "titularId", 10);
    assertRelation(operator, headers, catalog.getBody(), "substituicoes", "substitutoId", 20);

    assertThat(rest.getForEntity(PARENT + "/designacoes-vinculo", JsonNode.class).getStatusCode())
        .isEqualTo(HttpStatus.UNAUTHORIZED);
    headers.set("X-Tenant", "2");
    assertThat(operator.exchange(PARENT + "/surfaces", HttpMethod.GET,
        new HttpEntity<>(headers), JsonNode.class).getStatusCode()).isIn(HttpStatus.NOT_FOUND, HttpStatus.FORBIDDEN);
    assertThat(operator.exchange(PARENT + "/substituicoes-vinculo", HttpMethod.GET,
        new HttpEntity<>(headers), JsonNode.class).getStatusCode()).isIn(HttpStatus.NOT_FOUND, HttpStatus.FORBIDDEN);
  }

  private void assertRelation(TestRestTemplate operator, HttpHeaders headers, JsonNode catalog,
      String resource, String parentField, long expectedId) {
    var surface = StreamSupport.stream(catalog.path("surfaces").spliterator(), false)
        .filter(candidate -> candidate.path("id").asText().equals(resource + "-vinculo"))
        .findFirst().orElseThrow();
    var relation = surface.path("relatedResource");
    assertThat(surface.path("availability").path("allowed").asBoolean()).isTrue();
    assertThat(relation.path("childResourceKey").asText()).isEqualTo("administracao-pessoal." + resource);
    assertThat(relation.path("childParentField").asText()).isEqualTo(parentField);
    assertThat(relation.path("childOperations").toString()).contains("FILTER", "LIST");

    var direct = operator.exchange(PARENT + "/" + resource + "-vinculo", HttpMethod.GET,
        new HttpEntity<>(headers), JsonNode.class);
    assertThat(direct.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertSingle(direct.getBody(), expectedId);

    var filtered = operator.exchange("/api/ergon/" + resource + "/filter", HttpMethod.POST,
        new HttpEntity<>(Map.of(parentField, "1001:1", "empCodigo", 1), headers), JsonNode.class);
    assertThat(filtered.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertSingle(filtered.getBody(), expectedId);
  }

  private static void assertSingle(JsonNode body, long expectedId) {
    var records = body.path("data").path("content");
    assertThat(records.size()).isEqualTo(1);
    assertThat(records.get(0).path("id").asLong()).isEqualTo(expectedId);
  }

  private void seed() {
    jdbc.update("""
        insert into empresas (empresa,nome,razao,cnpj,uf_sigla,municipio,ativa,version,created_at,updated_at)
        values (1,'Órgão F5','Órgão F5','01234567000189','DF','Brasília',true,0,current_timestamp,current_timestamp)
        """);
    jdbc.update("""
        insert into funcionarios (numero,nome,dtnasc,cpf,uniao_estavel,
          deffisica,defvisual,defauditiva,defmental,defintelectual,custom_fields,version,created_at,updated_at)
        values (1001,'Pessoa F5',date '1985-01-01','01234567890',false,false,false,false,false,false,json '{}',0,current_timestamp,current_timestamp)
        """);
    for (int numero : new int[]{1, 2}) {
      jdbc.update("""
          insert into vinculos (numfunc,numero,emp_codigo,situacao,regimejur,tipovinc,categoria,dtexerc,
            desconta_ir,correio,info_cota,custom_fields,version,created_at,updated_at)
          values (1001,?,1,'ATIVO','ESTATUTARIO','EFETIVO','ATIVO',date '2024-01-01',
            false,false,false,json '{}',0,current_timestamp,current_timestamp)
          """, numero);
    }
    jdbc.update("""
        insert into setores (setor,emp_codigo,nome,dtini,custom_fields,version,created_at,updated_at)
        values ('F5',1,'Setor F5',date '2024-01-01',json '{}',0,current_timestamp,current_timestamp)
        """);
    jdbc.update("""
        insert into funcoes_confianca (id,emp_codigo,descricao,dtini,situacao,codigo,categoria,simbolo,
          controle_vaga,permite_substituicao,valor_gratificacao,exige_vinculo_efetivo,version,created_at,updated_at)
        values (1,1,'Função F5',date '2024-01-01','ATIVA','F5','CHEFIA','F5',false,true,0,false,0,current_timestamp,current_timestamp)
        """);
    for (int numero : new int[]{1, 2}) {
      jdbc.update("""
          insert into designacoes_funcao (id,emp_codigo,descricao,dtini,situacao,
            titular_numfunc,titular_numvinc,funcao_id,setor_id,opcao_remuneracao,version,created_at,updated_at)
          values (?,1,'Designação F5',date '2025-01-01','EM_EXERCICIO',1001,?,1,'F5','INTEGRAL',0,current_timestamp,current_timestamp)
          """, 9 + numero, numero);
      jdbc.update("""
          insert into substituicoes_funcao (id,emp_codigo,descricao,dtini,dtfim,situacao,
            designacao_id,substituto_numfunc,substituto_numvinc,motivo,version,created_at,updated_at)
          values (?,1,'Substituição F5',date '2025-01-10',date '2025-01-12','PROGRAMADA',
            ?,1001,?,'AUSENCIA_EVENTUAL',0,current_timestamp,current_timestamp)
          """, 19 + numero, 9 + numero, numero);
    }
  }
}
