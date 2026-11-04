package br.com.praxis.ergonshowcase.config;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.policystudio.ErgonPolicyComposition;
import br.com.praxis.ergonshowcase.rules.ErgonRuleEngineFacade;
import br.com.praxis.ergonshowcase.rules.VagaFuncaoRuleSet;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.praxisplatform.config.service.DomainRuleSnapshotScopeLock;
import org.praxisplatform.config.tx.ConfigTransactionManagerNames;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@EnabledIfEnvironmentVariable(named = "PS3B_NEON_PROOF", matches = "true")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "showcase.security.username=ps3b-domain",
    "showcase.security.password=ps3b-test-only",
    "showcase.security.policy-read-orgs=2",
    "showcase.security.config-principals[0].username=ps3b-reader",
    "showcase.security.config-principals[0].password=ps3b-test-only",
    "showcase.security.config-principals[0].organization=2",
    "showcase.security.config-principals[0].environment=stage",
    "showcase.security.config-principals[0].roles=RULE_SNAPSHOT_READER",
    "showcase.security.config-principals[1].username=ps3c-author",
    "showcase.security.config-principals[1].password=ps3c-author-test-only",
    "showcase.security.config-principals[1].organization=2",
    "showcase.security.config-principals[1].environment=stage",
    "showcase.security.config-principals[1].roles=RULE_DEFINITION_AUTHOR,RULE_DEFINITION_APPROVER",
    "showcase.security.config-principals[2].username=ps3c-reviewer",
    "showcase.security.config-principals[2].password=ps3c-reviewer-test-only",
    "showcase.security.config-principals[2].organization=2",
    "showcase.security.config-principals[2].environment=stage",
    "showcase.security.config-principals[2].roles=RULE_DEFINITION_APPROVER",
    "showcase.security.config-principals[3].username=ps3c-publisher",
    "showcase.security.config-principals[3].password=ps3c-publisher-test-only",
    "showcase.security.config-principals[3].organization=2",
    "showcase.security.config-principals[3].environment=stage",
    "showcase.security.config-principals[3].roles=RULE_SNAPSHOT_PUBLISHER",
    "showcase.security.config-origins=https://studio.example.test"
})
class Ps3bConfigNeonTest {
  private static final String SNAPSHOTS = ApiPaths.PRAXIS_CONFIG + "/domain-rules/snapshots";
  @Autowired TestRestTemplate http;
  @Autowired ObjectMapper mapper;
  @Autowired ErgonRuleEngineFacade rules;
  @Autowired JdbcTemplate domain;
  @Autowired @Qualifier("configJdbcTemplate") JdbcTemplate config;
  @Autowired @Qualifier(ConfigTransactionManagerNames.CONFIG) PlatformTransactionManager transactions;
  @Autowired DomainRuleSnapshotScopeLock lock;

  @Test
  void configTransactionKeepsItsLockAndRollsBackWithoutTouchingDomain() {
    assertThat(config.queryForObject("select current_database()", String.class))
        .isNotEqualTo(domain.queryForObject("select current_database()", String.class));
    assertThat(config.queryForObject("select current_user", String.class))
        .isNotEqualTo(domain.queryForObject("select current_user", String.class));
    assertThat(config.queryForObject("select has_schema_privilege(current_user, current_schema(), 'CREATE')",
        Boolean.class)).isFalse();
    Long before = domain.queryForObject("select count(*) from empresas", Long.class);
    new TransactionTemplate(transactions).executeWithoutResult(status -> {
      lock.lock("2", "stage", "ps3b.gate");
      assertThat(config.queryForObject("""
          select count(*) from pg_locks where pid = pg_backend_pid() and locktype = 'advisory' and granted
          """, Long.class)).isPositive();
      config.update("insert into ps3b_tx_probe (id) values (1)");
      status.setRollbackOnly();
    });
    assertThat(config.queryForObject("select count(*) from ps3b_tx_probe", Long.class)).isZero();
    assertThat(domain.queryForObject("select count(*) from empresas", Long.class)).isEqualTo(before);
    assertThat(domain.queryForObject("select to_regclass('public.ps3b_tx_probe')::text", String.class)).isNull();
  }

  @Test
  void rejectsForgedScopeDisallowedOriginAndMissingConfigRoleOverHttp() {
    var reader = http.withBasicAuth("ps3b-reader", "ps3b-test-only");
    var headers = new HttpHeaders();
    headers.setOrigin("https://studio.example.test");
    String versions = SNAPSHOTS + "?ruleSetKey=ps3b.gate";
    assertThat(reader.exchange(versions, HttpMethod.GET, new HttpEntity<>(headers), String.class)
        .getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(http.withBasicAuth("ps3c-author", "ps3c-author-test-only")
        .exchange(versions, HttpMethod.GET, new HttpEntity<>(headers), String.class)
        .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(http.withBasicAuth("ps3b-domain", "ps3b-test-only")
        .exchange(versions, HttpMethod.GET, new HttpEntity<>(headers), String.class)
        .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    for (String header : List.of("X-Tenant-ID", "X-Env", "X-User-ID")) {
      headers.set(header, "forged");
      assertThat(reader.exchange(versions, HttpMethod.GET, new HttpEntity<>(headers), String.class)
          .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
      headers.remove(header);
    }
    assertThat(reader.exchange(SNAPSHOTS + "/missing/rollback", HttpMethod.POST,
        new HttpEntity<>(headers), String.class).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    headers.setOrigin("https://other.example.test");
    assertThat(reader.exchange(versions, HttpMethod.GET, new HttpEntity<>(headers), String.class)
        .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void persistsNativeDefinitionApprovalByADifferentAuthenticatedActor() throws Exception {
    String definitions = ApiPaths.PRAXIS_CONFIG + "/domain-rules/definitions";
    var headers = new HttpHeaders();
    headers.setOrigin("https://studio.example.test");
    headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
    var draft = mapper.createObjectNode()
        .put("ruleKey", "ps3c.iam-proof." + UUID.randomUUID())
        .put("version", 1).put("ruleType", "form_rule").put("status", "draft")
        .put("contextKey", "ps3c-iam-proof").put("resourceKey", "ps3c-iam-proof")
        .put("serviceKey", "praxis-ergon-showcase");
    draft.putObject("governance").putArray("requiredApprovals").add("ps3c-reviewer");
    var created = http.withBasicAuth("ps3c-author", "ps3c-author-test-only")
        .postForEntity(definitions, new HttpEntity<>(draft, headers), JsonNode.class);
    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    assertThat(created.getBody().path("createdBy").asText()).isEqualTo("ps3c-author");
    assertThat(created.getBody().path("status").asText()).isEqualTo("draft");
    String statusPath = definitions + "/" + created.getBody().path("id").asText() + "/status";

    var selfApproval = approve(statusPath, "ps3c-author", "ps3c-author-test-only");
    assertThat(selfApproval.statusCode()).isEqualTo(422);
    assertThat(mapper.readTree(selfApproval.body()).path("message").asText())
        .contains("approver must be different from its author");
    var reviewed = approve(statusPath, "ps3c-reviewer", "ps3c-reviewer-test-only");
    assertThat(reviewed.statusCode()).isEqualTo(200);
    var approved = mapper.readTree(reviewed.body());
    assertThat(approved.path("status").asText()).isEqualTo("approved");
    assertThat(approved.path("approvedBy").asText()).isEqualTo("ps3c-reviewer");
  }

  private HttpResponse<String> approve(String path, String actor, String password) throws Exception {
    String auth = Base64.getEncoder().encodeToString(
        (actor + ":" + password).getBytes(StandardCharsets.UTF_8));
    var request = HttpRequest.newBuilder(URI.create(http.getRootUri() + path))
        .header("Authorization", "Basic " + auth)
        .header("Origin", "https://studio.example.test")
        .header("Content-Type", "application/json")
        .method("PATCH", HttpRequest.BodyPublishers.ofString("{\"status\":\"approved\"}"))
        .build();
    try (var client = HttpClient.newHttpClient()) {
      return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
  }

  @Test
  void preparesCanonicalManifestFromApprovedErgonSourceWithoutChangingHead(
      org.junit.jupiter.api.TestReporter evidence) throws Exception {
    var product = VagaFuncaoRuleSet.productRuleSet();
    String key = product.ref().ruleSetKey();
    String slot = product.slots().stream()
        .filter(item -> item.overridePolicy() == org.praxisplatform.rules.contract.OverridePolicy.REPLACEABLE)
        .findFirst().orElseThrow().slotKey();
    String binding = "customer.orgao2.replace." + slot;
    int version = config.queryForObject("""
        select coalesce(max(version),0)+1 from domain_rule_definition
        where tenant_id='2' and environment='stage' and rule_key=?
        """, Integer.class, binding);
    var headers = new HttpHeaders();
    headers.setOrigin("https://studio.example.test");
    headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
    String definitions = ApiPaths.PRAXIS_CONFIG + "/domain-rules/definitions";
    var draft = mapper.createObjectNode()
        .put("ruleKey", binding).put("version", version).put("ruleType", "form_rule").put("status", "draft")
        .put("contextKey", product.ref().boundedContextKey()).put("resourceKey", key)
        .put("serviceKey", ErgonPolicyComposition.OWNER);
    var condition = mapper.readTree("{\"!=\":[{\"var\":\"draft.funcao\"},null]}");
    draft.set("condition", condition);
    draft.putObject("definition").putObject("ergonCustomization")
        .put("mode", "REPLACE_SLOT_EXPRESSION").put("slotKey", slot)
        .put("falseReasonCode", "CERG-PS3C-FUNCAO").put("messagePtBr", "Informe a função da vaga.")
        .set("expressionJson", condition);
    draft.putObject("parameters").put("hostContractVersion", ErgonPolicyComposition.HOST_CONTRACT);
    var governance = draft.putObject("governance").put("lifecycleBoundary", "REFERENCE_DRAFT_ONLY");
    governance.putArray("requiredApprovals").add("ps3c-reviewer");
    var created = http.withBasicAuth("ps3c-author", "ps3c-author-test-only")
        .postForEntity(definitions, new HttpEntity<>(draft, headers), JsonNode.class);
    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    String id = created.getBody().path("id").asText();
    String compose = ApiPaths.POLICY_STUDIO + ApiPaths.POLICY_STUDIO_COMPOSITION
        .replace("{ruleSetKey}", key).replace("{empCodigo}", "2");
    var request = mapper.createObjectNode().put("validFromUtc", "2026-11-03T00:00:00Z");
    request.putArray("sourceDefinitionIds").add(id);
    var publisher = http.withBasicAuth("ps3c-publisher", "ps3c-publisher-test-only");
    var reader = http.withBasicAuth("ps3b-reader", "ps3b-test-only");
    String scopeQuery = "?ruleSetKey=" + key;
    var headBefore = reader.exchange(SNAPSHOTS + "/head/status" + scopeQuery, HttpMethod.GET,
        new HttpEntity<>(headers), JsonNode.class);
    var versionsBefore = reader.exchange(SNAPSHOTS + scopeQuery, HttpMethod.GET,
        new HttpEntity<>(headers), JsonNode.class);
    String localTag = rules.activationTag(key, 2);
    String otherScope = compose.replace("/orgaos/2/", "/orgaos/7/");
    assertThat(publisher.postForEntity(otherScope, new HttpEntity<>(request, headers), JsonNode.class)
        .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(reader.postForEntity(compose, new HttpEntity<>(request, headers), JsonNode.class)
        .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(publisher.postForEntity(compose, new HttpEntity<>(request, headers), JsonNode.class)
        .getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(approve(definitions + "/" + id + "/status", "ps3c-reviewer",
        "ps3c-reviewer-test-only").statusCode()).isEqualTo(200);

    var result = publisher.postForEntity(compose, new HttpEntity<>(request, headers), JsonNode.class);
    assertThat(result.getStatusCode()).withFailMessage("Composição retornou %s", result.getBody())
        .isEqualTo(HttpStatus.OK);
    var manifest = result.getBody().path("manifest");
    assertThat(result.getBody().path("compositionDigest").asText())
        .isEqualTo(org.praxisplatform.rules.digest.PraxisCanonicalJson.sha256(manifest));
    assertThat(manifest.at("/sources/0/definitionId").asText()).isEqualTo(id);
    assertThat(manifest.path("ruleSet").path("errorDescriptors").path(binding).path("messagePtBr").asText())
        .isEqualTo("Informe a função da vaga.");
    assertThat(publisher.postForEntity(compose, new HttpEntity<>(request, headers), JsonNode.class).getBody())
        .isEqualTo(result.getBody());
    var headAfter = reader.exchange(SNAPSHOTS + "/head/status" + scopeQuery, HttpMethod.GET,
        new HttpEntity<>(headers), JsonNode.class);
    assertThat(headAfter.getStatusCode()).isEqualTo(headBefore.getStatusCode());
    assertThat(headAfter.getBody()).isEqualTo(headBefore.getBody());
    assertThat(reader.exchange(SNAPSHOTS + scopeQuery, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class)
        .getBody()).isEqualTo(versionsBefore.getBody());
    assertThat(rules.activationTag(key, 2)).isEqualTo(localTag);
    evidence.publishEntry("compositionDigest", result.getBody().path("compositionDigest").asText());
    evidence.publishEntry("candidateVersion", manifest.at("/ruleSet/ref/version").asText());
  }
}
