package br.com.praxis.ergonshowcase;

import static org.assertj.core.api.Assertions.*;

import br.com.praxis.ergonshowcase.service.CargoService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/** Prova TCP com commits reais na cópia Neon: preparo não é criação nem comando persistente. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CargoDuplicateDraftNeonTest {
  private static final int SOURCE = 979991;
  private static final int CREATED = 979992;
  private static final String ROOT = "/api/ergon/cargos";
  @Autowired TestRestTemplate rest;
  @Autowired JdbcTemplate jdbc;
  @Autowired ObjectMapper json;
  @Autowired CargoService service;
  @Autowired org.praxisplatform.uischema.openapi.OpenApiDocumentService documents;
  @Autowired jakarta.persistence.EntityManager entityManager;
  @Autowired org.springframework.transaction.PlatformTransactionManager transactions;
  @Value("${showcase.security.username}") String username;
  @Value("${showcase.security.password}") String password;
  TestRestTemplate writer;
  TestRestTemplate reader;
  JdbcTemplate observation;
  boolean fixtureOwned;

  @BeforeAll void prepare() throws Exception {
    String branch = System.getenv("BULK_PROOF_DISPOSABLE_BRANCH");
    assertThat(branch).matches("br-[a-z0-9-]+").isNotEqualTo("br-super-sound-b7urizmd");
    assertThat(System.getenv("PROOF_NEON_BRANCH")).isEqualTo(branch);
    assertThat(System.getenv("SPRING_DATASOURCE_URL"))
        .contains("//" + System.getenv("BULK_PROOF_EXPECTED_HOST") + "/");
    // Observação independente e somente leitura dos recibos protegidos; HTTP usa a identidade comum.
    var owner = new org.springframework.jdbc.datasource.DriverManagerDataSource(
        System.getenv("SPRING_DATASOURCE_URL"), System.getenv("BULK_OWNER_USERNAME"),
        System.getenv("BULK_OWNER_PASSWORD"));
    observation = new JdbcTemplate(owner);
    writer = rest.withBasicAuth(username, password);
    reader = rest.withBasicAuth(System.getenv("SHOWCASE_BULK_ADMIN_USERNAME"),
        System.getenv("SHOWCASE_BULK_ADMIN_PASSWORD")); // READ + BULK_ADMIN, sem WRITE.
    BulkPublicationProof.ensurePublished(documents, writer);
    assertThat(jdbc.queryForObject("select count(*) from cargos where cargo in (?,?)",
        Long.class, SOURCE, CREATED)).isZero();
    jdbc.update("insert into cargos(cargo,emp_codigo,nome,categoria,subcategoria,tipo_cargo,"
        + "cargo_funcao,escolaridade,cbo,controle_vaga,por_referencia,e_aglutinador,"
        + "vagas_autorizadas,dt_inicio_contr_vaga,dt_extincao,custom_fields) "
        + "values (?,1,'Cargo referência prova','MAGISTERIO','DOCENCIA','EFETIVO','C',"
        + "'SUPERIOR','231205',true,true,true,17,date '2010-01-01',null,"
        + "'{\"naoCopiar\":\"marcador privado da origem\"}'::jsonb)", SOURCE);
    fixtureOwned = true;
  }

  @AfterAll void cleanup() {
    if (fixtureOwned) jdbc.update("delete from cargos where cargo in (?,?)", CREATED, SOURCE);
    SecurityContextHolder.clearContext();
  }

  private ResponseEntity<JsonNode> call(TestRestTemplate client, HttpMethod method,
      String path, String tenant, Object body) {
    var headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    if (tenant != null) headers.set("X-Tenant", tenant);
    return client.exchange(path, method, new HttpEntity<>(body, headers), JsonNode.class);
  }

  private ObjectNode draft() {
    var response = call(writer, HttpMethod.POST, ROOT + "/" + SOURCE + "/duplicate-draft", "1", null);
    assertThat(response.getStatusCode()).as(String.valueOf(response.getBody())).isEqualTo(HttpStatus.OK);
    return (ObjectNode) response.getBody().path("data");
  }

  /** Hashes somente: não exportar conteúdo de domínio ou ledger como evidência. */
  private List<Map<String,Object>> snapshot() {
    var tables = observation.queryForList("select schemaname,tablename from pg_tables "
        + "where schemaname in ('public','praxis_bulk') order by 1,2");
    String union = tables.stream().map(t -> {
      String schema = t.get("schemaname").toString();
      String table = t.get("tablename").toString();
      String quoted = "\"" + schema.replace("\"", "\"\"") + "\".\""
          + table.replace("\"", "\"\"") + "\"";
      return "select '" + (schema+"."+table).replace("'", "''") + "' as tabela, count(*) as total, "
          + "md5(string_agg(to_jsonb(t)::text,'|' order by to_jsonb(t)::text)) as hash from " + quoted + " t";
    }).collect(java.util.stream.Collectors.joining(" union all "));
    return observation.queryForList("select * from (" + union + ") snapshot order by tabela");
  }

  @Test void preparesAllowlistedDraftWithoutAnyWriteOrVersionChangeAndCanBeDiscarded() throws Exception {
    var before = snapshot();
    var version = jdbc.queryForObject("select version from cargos where cargo=?", Long.class, SOURCE);
    var first = draft();
    assertThat(first.path("nome").asText()).isEqualTo("Cargo referência prova");
    assertThat(first.path("categoria").asText()).isEqualTo("MAGISTERIO");
    assertThat(first.path("subcategoria").asText()).isEqualTo("DOCENCIA");
    assertThat(first.path("tipoCargo").asText()).isEqualTo("EFETIVO");
    assertThat(first.path("cargoFuncao").asText()).isEqualTo("C");
    assertThat(first.path("escolaridade").asText()).isEqualTo("SUPERIOR");
    assertThat(first.path("cbo").asText()).isEqualTo("231205");
    for (String field : List.of("cargo","empCodigo","documentoLegalId","vagasAutorizadas",
        "cargoAglutinador","dtInicioContrVaga","dtExtincao","version","auditoria","id"))
      assertThat(first.path(field).isMissingNode() || first.path(field).isNull()).as(field).isTrue();
    for (String field : List.of("controleVaga","porReferencia","eAglutinador")) {
      assertThat(first.path(field).isNull()).as(field).isTrue();
    }
    assertThat(first.path("customFields").size()).isZero();
    assertThat(draft()).isEqualTo(first); // Preparar novamente após descartar não cria identidade.
    assertThat(snapshot()).isEqualTo(before);
    assertThat(jdbc.queryForObject("select version from cargos where cargo=?", Long.class, SOURCE)).isEqualTo(version);
    Files.createDirectories(Path.of("target/proof"));
    Files.writeString(Path.of("target/proof/cargo-duplicate-draft.json"), json.writeValueAsString(Map.of(
        "allowlistedFields", List.of("nome","categoria","subcategoria","tipoCargo","cargoFuncao","escolaridade","cbo"),
        "noDomainOrBulkWrite", true, "sourceVersionUnchanged", true, "discardHasNoEffect", true)));
  }

  @Test void explicitFlagsAreRequiredAndFalseRemainsValid() {
    var before = snapshot();
    var input = draft().put("cargo", CREATED).put("empCodigo", 1)
        .put("controleVaga", false).put("porReferencia", false).put("eAglutinador", false);
    for (String flag : List.of("controleVaga", "porReferencia", "eAglutinador")) {
      var omitted = input.deepCopy(); omitted.remove(flag);
      assertThat(call(writer,HttpMethod.POST,ROOT,"1",omitted).getStatusCode()).as(flag)
          .isEqualTo(HttpStatus.BAD_REQUEST);
      var nil = input.deepCopy(); nil.putNull(flag);
      assertThat(call(writer,HttpMethod.POST,ROOT,"1",nil).getStatusCode()).as(flag)
          .isEqualTo(HttpStatus.BAD_REQUEST);
    }
    assertThat(snapshot()).isEqualTo(before);
    try {
      assertThat(call(writer,HttpMethod.POST,ROOT,"1",input).getStatusCode()).isEqualTo(HttpStatus.CREATED);
      assertThat(jdbc.queryForObject("select not controle_vaga and not por_referencia and not e_aglutinador from cargos where cargo=?",Boolean.class,CREATED)).isTrue();
    } finally { jdbc.update("delete from cargos where cargo=?", CREATED); }
    assertThat(snapshot()).isEqualTo(before);
  }

  @Test void contextAndUntrustedProjectionInputsAreRevalidatedAtCreate() {
    var before = snapshot();
    assertThat(call(rest,HttpMethod.POST,ROOT+"/"+SOURCE+"/duplicate-draft","1",null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(call(writer,HttpMethod.POST,ROOT+"/"+SOURCE+"/duplicate-draft","invalid",null).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(call(writer,HttpMethod.POST,ROOT+"/"+SOURCE+"/duplicate-draft",null,null).getStatusCode()).isEqualTo(HttpStatus.OK);
    var input = draft().put("cargo",CREATED).put("empCodigo",1)
        .put("controleVaga",false).put("porReferencia",false).put("eAglutinador",false);
    var missingContextField = call(writer,HttpMethod.POST,ROOT,"2",input);
    assertThat(missingContextField.getStatusCode()).as(String.valueOf(missingContextField.getBody()))
        .isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(missingContextField.getBody().toString()).contains("exige_porte_arma");
    input.putObject("customFields").put("exige_porte_arma",false);
    var mismatchedContext = call(writer,HttpMethod.POST,ROOT,"2",input);
    assertThat(mismatchedContext.getStatusCode()).as(String.valueOf(mismatchedContext.getBody()))
        .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(mismatchedContext.getBody().toString()).contains("diverge de X-Tenant");
    input.putObject("customFields");
    input.put("cargoAglutinadorNomePrevisto","Prévia forjada");
    assertThat(call(writer,HttpMethod.POST,ROOT,"1",input).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    input.remove("cargoAglutinadorNomePrevisto");
    input.put("id",SOURCE);
    for (String property : List.of("version","dtExtincao","saldoVagasDisponiveis","auditoria")) {
      var forged = input.deepCopy();
      if (property.equals("auditoria"))
        forged.putObject(property).put("createdAt","1900-01-01T00:00:00Z").put("updatedAt","1900-01-01T00:00:00Z");
      else if (property.equals("dtExtincao")) forged.put(property,"2020-01-01");
      else forged.put(property,999);
      var rejected = call(writer,HttpMethod.POST,ROOT,"1",forged);
      assertThat(rejected.getStatusCode()).as(property + ": " + rejected.getBody()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
    assertThat(snapshot()).isEqualTo(before);
    try {
      // O host rejeita campos desconhecidos; apenas id é explicitamente ignorado no CREATE.
      var accepted = call(writer,HttpMethod.POST,ROOT,"1",input);
      assertThat(accepted.getStatusCode()).as(String.valueOf(accepted.getBody())).isEqualTo(HttpStatus.CREATED);
      assertThat(jdbc.queryForObject("select version=0 and dt_extincao is null and created_at > timestamp '2000-01-01' and updated_at > timestamp '2000-01-01' from cargos where cargo=?",Boolean.class,CREATED)).isTrue();
    } finally { jdbc.update("delete from cargos where cargo=?",CREATED); }
    assertThat(snapshot()).isEqualTo(before);
  }

  @Test void finalCreateUsesCurrentCustomFieldCatalogInsteadOfOriginValues() {
    var before = snapshot();
    var input = draft().put("cargo",CREATED).put("empCodigo",1)
        .put("controleVaga",false).put("porReferencia",false).put("eAglutinador",false);
    input.set("customFields",json.createObjectNode().put("unknown_draft_key",true));
    assertThat(call(writer,HttpMethod.POST,ROOT,"1",input).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    input.set("customFields",json.createObjectNode().put("conselho_classe_obrigatorio","ENUM_INEXISTENTE"));
    assertThat(call(writer,HttpMethod.POST,ROOT,"1",input).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    var definition = observation.queryForMap("select id,required from custom_field_definitions where active=true and resource_key='administracao-pessoal.cargos' and field_key='conselho_classe_obrigatorio' and (emp_codigo is null or emp_codigo=1) order by emp_codigo nulls last limit 1");
    try {
      // Somente na cópia descartável: a obrigatoriedade muda depois do preparo.
      observation.update("update custom_field_definitions set required=true where id=?",definition.get("id"));
      input.set("customFields",json.createObjectNode());
      assertThat(call(writer,HttpMethod.POST,ROOT,"1",input).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    } finally {
      observation.update("update custom_field_definitions set required=? where id=?",definition.get("required"),definition.get("id"));
    }
    assertThat(snapshot()).isEqualTo(before);
  }

  @Test void extinctOriginIncludingFutureDateIsDeniedInEveryBoundary() {
    try {
      SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
          "draft-writer", "unused", List.of(new SimpleGrantedAuthority("ERGON_READ"),new SimpleGrantedAuthority("ERGON_WRITE"))));
      for (var date : List.of(java.time.LocalDate.of(2020,1,1), java.time.LocalDate.now().plusYears(10))) {
        jdbc.update("update cargos set dt_extincao=? where cargo=?",date,SOURCE);
        var before = snapshot();
        assertThat(call(writer,HttpMethod.POST,ROOT+"/"+SOURCE+"/duplicate-draft","1",null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThatThrownBy(() -> service.duplicateDraft(SOURCE))
            .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
            .hasMessageContaining("duplicate-draft-unavailable");
        var caps = call(writer,HttpMethod.GET,ROOT+"/"+SOURCE+"/capabilities","1",null);
        assertThat(caps.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(caps.getBody().path("operations").path("duplicate-draft").path("availability").path("allowed")).isEqualTo(BooleanNode.FALSE);
        var item = call(writer,HttpMethod.GET,ROOT+"/"+SOURCE,"1",null);
        assertThat(item.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(item.getBody().path("data").isObject()).isTrue();
        assertThat(item.getBody().path("_links").isObject()).isTrue();
        assertThat(item.getBody().path("_links").has("duplicate-draft")).isFalse();
        var actions = call(writer,HttpMethod.GET,ROOT+"/"+SOURCE+"/actions","1",null);
        assertThat(actions.getStatusCode()).isEqualTo(HttpStatus.OK);
        var action = java.util.stream.StreamSupport.stream(actions.getBody().path("actions").spliterator(),false)
            .filter(a -> a.path("id").asText().equals("duplicate-draft")).findFirst().orElseThrow();
        assertThat(action.path("availability").path("allowed")).isEqualTo(BooleanNode.FALSE);
        assertThat(snapshot()).isEqualTo(before);
      }
    } finally {
      jdbc.update("update cargos set dt_extincao=null where cargo=?",SOURCE);
      SecurityContextHolder.clearContext();
    }
  }

  @Test void functionOptionalNullsAndTextualCboArePreserved() {
    try {
      jdbc.update("update cargos set cargo_funcao='F',subcategoria=null,tipo_cargo=null,escolaridade=null,cbo='001234' where cargo=?",SOURCE);
      var before = snapshot();
      var result = draft();
      assertThat(result.path("cargoFuncao").asText()).isEqualTo("F");
      assertThat(result.path("cbo").asText()).isEqualTo("001234");
      for (var field : List.of("subcategoria","tipoCargo","escolaridade"))
        assertThat(result.path(field).isNull()).as(field).isTrue();
      result.put("nome","Modificação exclusivamente do rascunho");
      assertThat(draft().path("nome").asText()).isEqualTo("Cargo referência prova");
      assertThat(snapshot()).isEqualTo(before);
    } finally {
      jdbc.update("update cargos set cargo_funcao='C',subcategoria='DOCENCIA',tipo_cargo='EFETIVO',escolaridade='SUPERIOR',cbo='231205' where cargo=?",SOURCE);
    }
  }

  @Test void assignedIdentityWithNullVersionIsNew() {
    var cargo = new br.com.praxis.ergonshowcase.domain.Cargo();
    cargo.setCargo(CREATED);
    var information = org.springframework.data.jpa.repository.support.JpaEntityInformationSupport
        .getEntityInformation(br.com.praxis.ergonshowcase.domain.Cargo.class, entityManager);
    assertThat(cargo.getVersion()).isNull();
    assertThat(information.isNew(cargo)).isTrue();
    cargo.setVersion(0L);
    assertThat(information.isNew(cargo)).as("Persisted version must not use the new-entity path").isFalse();
  }

  @Test void concurrentCreateCannotOverwriteTheWinner() throws Exception {
    var before = snapshot();
    var input = draft().put("cargo",CREATED).put("empCodigo",1)
        .put("controleVaga",false).put("porReferencia",false).put("eAglutinador",false);
    var ready = new java.util.concurrent.CountDownLatch(2);
    var start = new java.util.concurrent.CountDownLatch(1);
    try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      var calls = new ArrayList<java.util.concurrent.Future<ResponseEntity<JsonNode>>>();
      for (String name : List.of("Primeiro concorrente", "Segundo concorrente")) {
        var body = input.deepCopy().put("nome",name);
        calls.add(pool.submit(() -> { ready.countDown(); start.await(); return call(writer,HttpMethod.POST,ROOT,"1",body); }));
      }
      assertThat(ready.await(10,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
      start.countDown();
      var responses = List.of(calls.get(0).get(120,java.util.concurrent.TimeUnit.SECONDS),calls.get(1).get(120,java.util.concurrent.TimeUnit.SECONDS));
      assertThat(responses.stream().map(r -> r.getStatusCode().value()).toList()).containsExactlyInAnyOrder(201,409);
      String winner = responses.get(0).getStatusCode().value() == 201 ? "Primeiro concorrente" : "Segundo concorrente";
      assertThat(jdbc.queryForObject("select nome from cargos where cargo=?",String.class,CREATED)).isEqualTo(winner);
      assertThat(jdbc.queryForObject("select version from cargos where cargo=?",Long.class,CREATED)).isZero();
    } finally { start.countDown(); jdbc.update("delete from cargos where cargo=?",CREATED); }
    assertThat(snapshot()).isEqualTo(before);
  }

  @Test void readWithoutCreatePermissionCannotPrepareOrDiscoverExecutableAction() {
    var before = snapshot();
    var rejected = call(reader, HttpMethod.POST, ROOT+"/"+SOURCE+"/duplicate-draft", "1", null);
    assertThat(rejected.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    var caps = call(reader, HttpMethod.GET, ROOT+"/"+SOURCE+"/capabilities", "1", null);
    assertThat(caps.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(caps.getBody().path("operations").path("duplicate-draft").path("availability").path("allowed")).isEqualTo(BooleanNode.FALSE);
    var item = call(reader,HttpMethod.GET,ROOT+"/"+SOURCE,"1",null);
    assertThat(item.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(item.getBody().path("data").isObject()).isTrue();
    assertThat(item.getBody().path("_links").isObject()).isTrue();
    assertThat(item.getBody().path("_links").has("duplicate-draft")).isFalse();
    var catalog = call(reader,HttpMethod.GET,ROOT+"/"+SOURCE+"/actions","1",null);
    assertThat(catalog.getStatusCode()).isEqualTo(HttpStatus.OK);
    var denied = java.util.stream.StreamSupport.stream(catalog.getBody().path("actions").spliterator(),false)
        .filter(a -> a.path("id").asText().equals("duplicate-draft")).findFirst().orElseThrow();
    assertThat(denied.path("availability").path("allowed")).isEqualTo(BooleanNode.FALSE);
    assertThat(snapshot()).isEqualTo(before);
  }

  @Test void missingAndOtherOrganizationOriginsAreIndistinguishableAndDoNotLeakState() {
    var before = snapshot();
    var absent = call(writer,HttpMethod.POST,ROOT+"/979999/duplicate-draft","1",null);
    var foreign = call(writer,HttpMethod.POST,ROOT+"/"+SOURCE+"/duplicate-draft","2",null);
    assertThat(absent.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(foreign.getStatusCode()).isEqualTo(absent.getStatusCode());
    assertThat(absent.getBody().path("message").asText()).isNotBlank()
        .isEqualTo(foreign.getBody().path("message").asText());
    assertThat(absent.getBody().toString()).doesNotContain("Cargo referência", "marcador privado");
    assertThat(foreign.getBody().toString()).doesNotContain("Cargo referência", "marcador privado");
    var caps = call(writer,HttpMethod.GET,ROOT+"/"+SOURCE+"/capabilities","2",null);
    assertThat(caps.getStatusCode()).isEqualTo(HttpStatus.OK);
    var foreignAvailability = caps.getBody().path("operations").path("duplicate-draft").path("availability");
    assertThat(foreignAvailability.isObject()).isTrue();
    assertThat(foreignAvailability.path("allowed")).isEqualTo(BooleanNode.FALSE);
    assertThat(foreignAvailability.path("reason").asText()).isEqualTo("duplicate-draft-unavailable");
    assertThat(foreignAvailability.path("metadata").isObject()).isTrue();
    assertThat(foreignAvailability.path("metadata").isEmpty()).isTrue();
    var publishedAvailabilities = caps.getBody().findValues("availability");
    assertThat(publishedAvailabilities).isNotEmpty();
    for (var availability : publishedAvailabilities) {
      assertThat(availability.isObject()).isTrue();
      assertThat(availability.path("metadata").isObject()).isTrue();
      assertThat(availability.path("metadata").has("resourceState")).isFalse();
    }
    assertThat(snapshot()).isEqualTo(before);
  }

  @Test void finalCreateRevalidatesRequiredIdentityOrganizationVacanciesAndUniqueness() {
    var before = snapshot();
    try {
      var dto = draft();
      assertThat(call(writer,HttpMethod.POST,ROOT,"1",dto).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      dto.put("controleVaga",false).put("porReferencia",false).put("eAglutinador",false);
      dto.put("cargo",CREATED).put("empCodigo",2);
      assertThat(call(writer,HttpMethod.POST,ROOT,"1",dto).getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
      dto.put("empCodigo",1).put("controleVaga",true).put("vagasAutorizadas",0);
      assertThat(call(writer,HttpMethod.POST,ROOT,"1",dto).getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
      dto.put("vagasAutorizadas",3).put("cargoAglutinador",SOURCE);
      var foreignParent = jdbc.queryForObject("select min(cargo) from cargos where emp_codigo<>1",Integer.class);
      assertThat(foreignParent).isNotNull();
      dto.put("cargoAglutinador",foreignParent);
      assertThat(call(writer,HttpMethod.POST,ROOT,"1",dto).getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
      dto.remove("cargoAglutinador");dto.put("cargo",SOURCE);
      assertThat(call(writer,HttpMethod.POST,ROOT,"1",dto).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
      dto.put("cargo",CREATED);
      assertThat(call(reader,HttpMethod.POST,ROOT,"1",dto).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
      assertThat(snapshot()).isEqualTo(before);
      var created = call(writer,HttpMethod.POST,ROOT,"1",dto);
      assertThat(created.getStatusCode()).as(String.valueOf(created.getBody())).isEqualTo(HttpStatus.CREATED);
      assertThat(jdbc.queryForObject("select count(*) from cargos where cargo=? and emp_codigo=1 and dt_extincao is null",Long.class,CREATED)).isEqualTo(1);
    } finally { jdbc.update("delete from cargos where cargo=?",CREATED); }
    assertThat(snapshot()).isEqualTo(before);
  }

  @Test void directServiceAlsoRequiresPermissionsAndDoesNotExposeMissingOrigin() {
    var before = snapshot();
    try {
      SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
          "draft-reader", "unused", List.of(new SimpleGrantedAuthority("ERGON_READ"))));
      assertThatThrownBy(() -> service.duplicateDraft(SOURCE))
          .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
      SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
          "draft-writer-without-read", "unused", List.of(new SimpleGrantedAuthority("ERGON_WRITE"))));
      assertThatThrownBy(() -> service.duplicateDraft(SOURCE))
          .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
      SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
          "draft-writer", "unused", List.of(new SimpleGrantedAuthority("ERGON_READ"),new SimpleGrantedAuthority("ERGON_WRITE"))));
      assertThat(service.duplicateDraft(SOURCE).getCargo()).isNull();
      assertThatThrownBy(() -> service.duplicateDraft(979999))
          .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
          .hasMessageContaining("duplicate-draft-unavailable");
    } finally { SecurityContextHolder.clearContext(); }
    assertThat(snapshot()).isEqualTo(before);
  }

  @Test void publishesCanonicalSupportLinksActionAndCreateCompatibleSchema() throws Exception {
    var before = snapshot();
    var caps = call(writer,HttpMethod.GET,ROOT+"/"+SOURCE+"/capabilities","1",null);
    assertThat(caps.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(caps.getBody().path("canonicalOperations").path("duplicate-draft").asBoolean()).isTrue();
    var operation = caps.getBody().path("operations").path("duplicate-draft");
    assertThat(operation.path("preferredMethod").asText()).isEqualTo("POST");
    assertThat(operation.path("availability").path("allowed").asBoolean()).isTrue();
    var item = call(writer,HttpMethod.GET,ROOT+"/"+SOURCE,"1",null);
    assertThat(item.getBody().path("_links").has("duplicate-draft")).isTrue();
    var action = call(writer,HttpMethod.GET,ROOT+"/"+SOURCE+"/actions","1",null);
    assertThat(action.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(action.getBody().toString()).contains("duplicate-draft", "prepareErgonCargoDraft");
    var published = java.util.stream.StreamSupport.stream(action.getBody().path("actions").spliterator(),false)
        .filter(a -> a.path("id").asText().equals("duplicate-draft")).findFirst().orElseThrow();
    assertThat(published.path("availability").path("allowed").asBoolean()).isTrue();
    assertThat(published.path("responseSchemaUrl").asText()).contains("/schemas/filtered");
    var responseSchema = writer.getForEntity(
        "/schemas/filtered?resourcePath={path}&operation=post&schemaType=response",
        JsonNode.class,ROOT+"/{id}/duplicate-draft");
    var createSchema = writer.getForEntity(
        "/schemas/filtered?resourcePath={path}&operation=post&schemaType=request",JsonNode.class,ROOT);
    assertThat(responseSchema.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(createSchema.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(createSchema.getBody().path("properties").path("cargo").path("x-ui")
        .path("controlType").asText()).isEqualTo("input");
    assertThat(responseSchema.getBody().path("properties").has("cargo")).isTrue();
    assertThat(responseSchema.getBody().path("properties").has("dtExtincao")).isFalse();
    assertThat(responseSchema.getBody().path("required").size()).isZero();
    assertThat(responseSchema.getBody().path("properties").size()).isEqualTo(17);
    for (String flag : List.of("controleVaga","porReferencia","eAglutinador")) {
      assertThat(responseSchema.getBody().path("properties").path(flag).path("nullable").asBoolean()).as(flag).isTrue();
      assertThat(createSchema.getBody().path("required").toString()).contains('"'+flag+'"');
      assertThat(createSchema.getBody().path("properties").path(flag).path("x-ui").path("controlType").asText()).isEqualTo("select");
      assertThat(createSchema.getBody().path("properties").path(flag).hasNonNull("default")).isFalse();
      var draftProperty = responseSchema.getBody().path("properties").path(flag);
      assertThat(draftProperty.path("x-ui").path("controlType").asText()).as(flag).isEqualTo("select");
      assertThat(draftProperty.path("x-ui").path("options")).as(flag)
          .isEqualTo(createSchema.getBody().path("properties").path(flag).path("x-ui").path("options"));
      assertThat(draftProperty.hasNonNull("default")).as(flag).isFalse();
    }
    for (String field : List.of("empCodigo", "cargoAglutinador", "documentoLegalId")) {
      var draftUi = responseSchema.getBody().path("properties").path(field).path("x-ui");
      var createUi = createSchema.getBody().path("properties").path(field).path("x-ui");
      assertThat(draftUi.path("controlType").asText()).as(field).isEqualTo("entityLookup");
      assertThat(draftUi.path("endpoint").asText()).as(field).isNotBlank()
          .isEqualTo(createUi.path("endpoint").asText());
      for (String key : List.of("type", "valueField", "displayField"))
        assertThat(draftUi.path(key)).as(field + "." + key).isEqualTo(createUi.path(key));
    }
    for (String field : List.of("cargo","empCodigo","nome","categoria","subcategoria", "tipoCargo",
        "cargoFuncao","escolaridade","cbo")) {
      assertThat(responseSchema.getBody().path("properties").has(field)).as(field).isTrue();
      assertThat(createSchema.getBody().path("properties").has(field)).as(field).isTrue();
      assertThat(responseSchema.getBody().path("properties").path(field).path("type"))
          .isEqualTo(createSchema.getBody().path("properties").path(field).path("type"));
    }
    assertThat(snapshot()).isEqualTo(before);
    Files.createDirectories(Path.of("target/proof"));
    Files.writeString(Path.of("target/proof/cargo-duplicate-capabilities.json"),caps.getBody().toString());
    Files.writeString(Path.of("target/proof/cargo-duplicate-schema.json"),responseSchema.getBody().toString());
    Files.writeString(Path.of("target/proof/cargo-create-schema.json"),createSchema.getBody().toString());
    Files.writeString(Path.of("target/proof/cargo-duplicate-actions.json"),action.getBody().toString());
  }

  @Test void preparingDraftDoesNotFlushCallerOwnedDirtyPersistenceContext() {
    var before = snapshot();
    try {
      SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
          "draft-writer", "unused", List.of(new SimpleGrantedAuthority("ERGON_READ"),new SimpleGrantedAuthority("ERGON_WRITE"))));
      new org.springframework.transaction.support.TransactionTemplate(transactions).executeWithoutResult(status -> {
        var source = entityManager.find(br.com.praxis.ergonshowcase.domain.Cargo.class,SOURCE);
        source.setNome("Alteração pendente que não pertence ao preparo");
        var statistics = entityManager.getEntityManagerFactory().unwrap(org.hibernate.SessionFactory.class).getStatistics();
        boolean enabled = statistics.isStatisticsEnabled();
        statistics.setStatisticsEnabled(true);
        long flushes = statistics.getFlushCount();
        long inserts = statistics.getEntityInsertCount();
        long updates = statistics.getEntityUpdateCount();
        long deletes = statistics.getEntityDeleteCount();
        try {
        assertThat(service.duplicateDraft(SOURCE).getNome()).isEqualTo("Cargo referência prova");
        assertThatThrownBy(() -> service.duplicateDraft(979999))
            .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThat(observation.queryForObject("select nome from cargos where cargo=?",String.class,SOURCE))
            .isEqualTo("Cargo referência prova");
        assertThat(source.getNome()).isEqualTo("Alteração pendente que não pertence ao preparo");
        assertThat(statistics.getFlushCount()).isEqualTo(flushes);
        assertThat(statistics.getEntityInsertCount()).isEqualTo(inserts);
        assertThat(statistics.getEntityUpdateCount()).isEqualTo(updates);
        assertThat(statistics.getEntityDeleteCount()).isEqualTo(deletes);
        } finally { statistics.setStatisticsEnabled(enabled); }
        status.setRollbackOnly();
      });
    } finally { SecurityContextHolder.clearContext(); }
    assertThat(snapshot()).isEqualTo(before);
  }
}
