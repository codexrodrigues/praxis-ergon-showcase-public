package br.com.praxis.ergonshowcase;

import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.*;
import java.util.Map;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

/** Prova HTTP/TCP e ledger real; publicação e unidades possuem transações próprias. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BulkNeonIntegrationTest {
  @Autowired TestRestTemplate rest;
  @Autowired org.springframework.test.web.servlet.MockMvc mvc;
  @Value("${showcase.security.username}") String username;
  @Value("${showcase.security.password}") String password;
  boolean publicationReady;
  @BeforeAll void authenticate() { requireDisposableTarget();rest=rest.withBasicAuth(username,password); }

  @Test @Order(5)
  void homologatesDesignacaoAndReplaysWithoutReapplyingOnNeon() throws Exception {
    requireDisposableTarget();
    String operation = "ergon.designacoes.homologar-designacoes-lote";
    String path = "/api/ergon/designacoes";
    long generation;
    String intent;
    try (var c = ownerConnection(); var q = c.prepareStatement("""
        select generation,state from praxis_bulk.praxis_bulk_operation_control
        where namespace_id='ergon-showcase' and operation_id=?
        """)) {
      q.setString(1, operation);
      try (var rows = q.executeQuery()) {
        assertThat(rows.next()).as("Controle instalado para homologação").isTrue();
        generation = rows.getLong(1);
        intent = "READY".equals(rows.getString(2)) ? "RECONCILE" : "PUBLISH";
      }
    }
    var admin = rest.withBasicAuth(System.getenv("SHOWCASE_BULK_ADMIN_USERNAME"),
        System.getenv("SHOWCASE_BULK_ADMIN_PASSWORD"));
    var transport = new org.springframework.http.client.JdkClientHttpRequestFactory(
        java.net.http.HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(10)).build());
    transport.setReadTimeout(BulkPublicationProof.ADMIN_READ_TIMEOUT);
    admin.getRestTemplate().setRequestFactory(transport);
    rest.getRestTemplate().setRequestFactory(transport);
    String publicationPath = "/api/ergon/operations/bulk/publication";
    var publicationRequest = Map.of("operationId", operation, "expectedGeneration", generation, "intent", intent);
    var publication = admin.postForEntity(publicationPath, publicationRequest, JsonNode.class);
    if (publication.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE && "PUBLISH".equals(intent)) {
      // Retry cold OpenAPI generation only after proving the first attempt did not publish.
      try (var c = ownerConnection(); var q = c.prepareStatement("""
          select state, generation from praxis_bulk.praxis_bulk_operation_control
          where namespace_id='ergon-showcase' and operation_id=?
          """)) {
        q.setString(1, operation);
        try (var rows = q.executeQuery()) {
          assertThat(rows.next()).isTrue();
          assertThat(rows.getString(1)).isEqualTo("UNCOMPOSED");
          assertThat(rows.getLong(2)).isEqualTo(generation);
        }
      }
      System.out.println("S7 bulk proof: cold OpenAPI returned 503; control unchanged, retrying once");
      publication = admin.postForEntity(publicationPath, publicationRequest, JsonNode.class);
    }
    assertThat(publication.getStatusCode()).as("Publicação de %s: %s", operation, publication.getBody())
        .isEqualTo(HttpStatus.OK);
    assertThat(publication.getBody().path("data").path("state").asText()).isEqualTo("READY");

    long id;
    try (var c = ownerConnection(); var q = c.createStatement(); var rows = q.executeQuery("""
        with source as (
          select v.numfunc, v.numero, s.setor from vinculos v
          cross join lateral (select setor from setores where emp_codigo=1 order by setor limit 1) s
          where v.emp_codigo=1 and v.situacao='ATIVO' order by v.numfunc, v.numero limit 1
        ), funcao as (
          insert into funcoes_confianca(emp_codigo,codigo,descricao,categoria,simbolo,controle_vaga,
            permite_substituicao,valor_gratificacao,percentual_opcao_efetivo,exige_vinculo_efetivo,dtini,situacao)
          select 1, 'S7-' || left(md5(random()::text), 12), 'Função prova S7', 'CHEFIA', 'FG-1', true,
            true, 100, 100, false, current_date - 1, 'ATIVA' from source returning id
        ), vaga as (
          insert into vagas_funcao(emp_codigo,funcao_id,setor_id,numero_vaga,descricao,origem_vaga,dtini,situacao)
          select 1, f.id, s.setor, 1, 'Vaga prova S7', 'CRIACAO POR LEI', current_date - 1, 'LIVRE'
          from funcao f cross join source s returning id, funcao_id, setor_id
        )
        insert into designacoes_funcao(emp_codigo,descricao,dtini,situacao,funcao_id,vaga_funcao_id,
          setor_id,titular_numfunc,titular_numvinc,opcao_remuneracao)
        select 1,'Designação prova S7',current_date - 1,'DESIGNADA',v.funcao_id,v.id,v.setor_id,s.numfunc,s.numero,'INTEGRAL'
        from vaga v cross join source s returning id
        """)) {
      assertThat(rows.next()).as("Fixture de designação criada no órgão 1").isTrue();
      id = rows.getLong(1);
    }
    JsonNode before = designacaoState(id);
    var item = rest.getForEntity(path + "/" + id, JsonNode.class);
    assertThat(item.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(item.getHeaders().getETag()).isNotBlank();
    String date = java.time.LocalDate.now(java.time.ZoneId.of("America/Sao_Paulo")).toString();
    var proposal = rest.postForEntity(path + "/bulk/homologar-designacoes-lote/evaluation",
        Map.of("executionMode", "SYNC", "selection", Map.of("mode", "EXPLICIT", "targets",
            List.of(Map.of("id", Long.toString(id), "expectedVersion", item.getHeaders().getETag()))),
            "parameters", Map.of("dtExercicio", date)), JsonNode.class);
    assertThat(proposal.getStatusCode()).as("Avaliação: %s", proposal.getBody()).isEqualTo(HttpStatus.OK);
    var evaluation = proposal.getBody().path("data");
    assertThat(evaluation.path("totals").path("executable").asInt()).isEqualTo(1);
    assertThat(designacaoState(id)).isEqualTo(before);

    var headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Idempotency-Key", UUID.randomUUID().toString());
    var confirmation = new HttpEntity<>(Map.of("proposalId", evaluation.path("proposalId").asText()), headers);
    String action = path + "/actions/homologar-designacoes-lote";
    var first = rest.postForEntity(action, confirmation, JsonNode.class);
    assertThat(first.getStatusCode()).as("Confirmação: %s", first.getBody()).isEqualTo(HttpStatus.OK);
    var execution = first.getBody().path("data");
    assertThat(execution.path("status").asText()).isEqualTo("COMPLETED");
    String executionId = execution.path("executionId").asText();
    assertThat(executionId).isNotBlank();
    var receipts = rest.getForEntity(path + "/bulk/executions/" + executionId + "/results", JsonNode.class);
    assertThat(receipts.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(receipts.getBody().path("data").path("content")).hasSize(1);
    assertThat(receipts.getBody().path("data").path("content").get(0).path("status").asText()).isEqualTo("CONFIRMED");

    var after = designacaoState(id);
    assertThat(after.path("designacao").asText()).isEqualTo("EM_EXERCICIO");
    assertThat(after.path("exercicio").asText()).isEqualTo(date);
    assertThat(after.path("vaga").asText()).isEqualTo("OCUPADA");
    assertThat(after.path("ocupante")).isEqualTo(after.path("titular"));
    assertThat(after.path("ocupanteVinculo")).isEqualTo(after.path("titularVinculo"));
    assertThat(after.path("designacaoVersao").asLong()).isEqualTo(before.path("designacaoVersao").asLong() + 1);
    assertThat(after.path("vagaVersao").asLong()).isEqualTo(before.path("vagaVersao").asLong() + 1);

    var replay = rest.postForEntity(action, confirmation, JsonNode.class);
    assertThat(replay.getStatusCode()).as("Replay: %s", replay.getBody()).isEqualTo(HttpStatus.OK);
    assertThat(replay.getBody().path("data").path("executionId").asText()).isEqualTo(executionId);
    assertThat(designacaoState(id)).isEqualTo(after);
    System.out.printf("S7 bulk proof: branch=%s proposal=%s execution=%s designacaoVersion=%d vagaVersion=%d%n",
        System.getenv("PROOF_NEON_BRANCH"), evaluation.path("proposalId").asText(), executionId,
        after.path("designacaoVersao").asLong(), after.path("vagaVersao").asLong());
  }

  private JsonNode designacaoState(long id) throws Exception {
    try (var c = ownerConnection(); var q = c.prepareStatement("""
        select jsonb_build_object('designacao',d.situacao,'exercicio',d.dt_exercicio,
          'designacaoVersao',d.version,'vaga',v.situacao,'vagaVersao',v.version,
          'ocupante',v.titular_atual_numfunc,'ocupanteVinculo',v.titular_atual_numvinc,
          'titular',d.titular_numfunc,'titularVinculo',d.titular_numvinc)
        from designacoes_funcao d join vagas_funcao v on v.id=d.vaga_funcao_id and v.emp_codigo=d.emp_codigo
        where d.emp_codigo=1 and d.id=?
        """)) {
      q.setLong(1, id);
      try (var rows = q.executeQuery()) {
        assertThat(rows.next()).isTrue();
        return new com.fasterxml.jackson.databind.ObjectMapper().readTree(rows.getString(1));
      }
    }
  }
  private void requireDisposableTarget() {
    String expected=System.getenv("BULK_PROOF_DISPOSABLE_BRANCH");
    assertThat(expected).as("Identidade explícita da cópia descartável provisionada para esta execução").matches("br-[a-z0-9-]+").isNotEqualTo("br-super-sound-b7urizmd");
    assertThat(System.getenv("PROOF_NEON_BRANCH")).isEqualTo(expected);
    String host=System.getenv("BULK_PROOF_EXPECTED_HOST");
    assertThat(host).as("Endpoint da cópia confirmado no provisionamento").isNotBlank();
    assertThat(System.getenv("SPRING_DATASOURCE_URL")).contains("//"+host+"/");
  }

  @Test
  @org.springframework.security.test.context.support.WithMockUser(authorities="ERGON_READ")
  void readOnlyCannotEvaluateOrConfirmAnyBulkOperation() throws Exception {
    for(String resource:new String[]{"vinculos","servidor-pessoas","cargos"}) {
      for(String path:new String[]{"/bulk/update/evaluation","/bulk/update-items/evaluation","/bulk/update","/bulk/update-items"}) {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/ergon/"+resource+path)
            .contentType(MediaType.APPLICATION_JSON).header("Idempotency-Key","read-only-proof").content("{}"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
      }
    }
    for(String action:new String[]{"realocar-setor-lote","homologar-estagio-lote"}) {
      for(String path:new String[]{"/bulk/"+action+"/evaluation","/actions/"+action}) {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/ergon/vinculos"+path)
            .contentType(MediaType.APPLICATION_JSON).header("Idempotency-Key","read-only-proof").content("{}"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
      }
    }
  }

  @Test @Order(1) void publishesCanonicalBulkOperationsAndEditableFields() throws Exception {
    BulkPublicationProof.publish(rest,Boolean.parseBoolean(System.getenv().getOrDefault("BULK_PROOF_RESET_PUBLICATION","false")));
    var swagger=rest.getForEntity("/v3/api-docs/swagger-config",JsonNode.class);
    assertThat(swagger.getStatusCode()).isEqualTo(HttpStatus.OK);
    Files.createDirectories(Path.of("target/proof"));
    Files.writeString(Path.of("target/proof/swagger-config-bulk.json"),swagger.getBody().toString());
    for(String resource:new String[]{"vinculos","servidor-pessoas","cargos"}) {
      var response=rest.getForEntity("/api/ergon/"+resource+"/capabilities",JsonNode.class);
      assertThat(response.getStatusCode()).as(String.valueOf(response.getBody())).isEqualTo(HttpStatus.OK);
      JsonNode operations=response.getBody().path("operations");
      for(String id:new String[]{"bulk-update","bulk-update-items"}) {
        JsonNode bulk=operations.path(id).path("bulk");
        assertThat(bulk.path("editableFields").path("writableFields").isArray()).as(response.getBody().toString()).isTrue();
        assertThat(bulk.path("evaluationOperation").path("operation").path("operationId").asText()).isNotBlank();
        assertThat(bulk.path("confirmationOperation").path("operation").path("operationId").asText()).isNotBlank();
      }
      Files.createDirectories(Path.of("target/proof"));
      Files.writeString(Path.of("target/proof",resource+"-bulk-capabilities.json"),response.getBody().toString());
    }
    visibleColumns("setores",List.of("id","empCodigo","nome","sigla","tipoSetor","dtfim"));
    visibleColumns("documentos-legais",List.of("empCodigo","tipoDocumento","numeroDocumento","anoDocumento","dataPublicacao","ementa"));
    publicationReady=true;
  }

  @Test @Order(2)
  void evaluatesThenExecutesUniformItemsAndCollectiveCommands() throws Exception {
    assertThat(publicationReady).as("Publicação canônica deve passar antes de criar fixtures mutáveis").isTrue();
    requireDisposableTarget();
    String setor; long documento;
    try(var c=ownerConnection();var s=c.createStatement()) {
      try(var r=s.executeQuery("select setor from public.setores where emp_codigo=1 order by setor limit 1")) { assertThat(r.next()).isTrue();setor=r.getString(1); }
      try(var r=s.executeQuery("select min(id) from public.documentos_legais where emp_codigo=1")) { assertThat(r.next()).isTrue();documento=r.getLong(1);assertThat(r.wasNull()).isFalse(); }
    }
    try {
      createVinculo(9611); createVinculo(9612);
      var changes=List.of(set("setorExercicio",setor),set("jornadaSemanal",35));
      var original=readVinculo("9611:1");
      long originalVersion=storedVersion(9611);
      var proposal=bulkPost("/bulk/update/evaluation",Map.of("executionMode","SYNC","selection",selection("9611:1","9612:1"),"changes",changes),null,HttpStatus.OK);
      assertThat(proposal.path("status").asText()).isEqualTo("READY");
      assertThat(proposal.path("totals").path("executable").asInt()).isEqualTo(2);
      assertThat(readVinculo("9611:1").getHeaders().getETag()).isEqualTo(original.getHeaders().getETag());
      String proposalId=proposal.path("proposalId").asText();
      var first=bulkGet("/bulk/proposals/"+proposalId+"/results?size=1");
      assertThat(first.path("content")).hasSize(1);
      assertThat(first.path("content").get(0).path("decision").asText()).isEqualTo("EXECUTABLE");
      assertThat(first.path("next").asText()).isNotBlank();
      var next=bulkGet("/bulk/proposals/"+proposalId+"/results?size=1&after="+first.path("next").asText());
      assertThat(next.path("content")).hasSize(1);
      assertThat(next.path("content").get(0).path("id")).isNotEqualTo(first.path("content").get(0).path("id"));
      String key=UUID.randomUUID().toString();
      var execution=bulkPost("/bulk/update",Map.of("proposalId",proposalId),key,HttpStatus.OK);
      String executionId=execution.path("executionId").asText();
      var receipts=bulkGet("/bulk/executions/"+executionId+"/results?size=50");
      assertThat(receipts.path("content")).hasSize(2);
      receipts.path("content").forEach(item->assertThat(item.path("status").asText()).isEqualTo("CONFIRMED"));
      var changed=readVinculo("9611:1");
      assertThat(changed.getHeaders().getETag()).isNotEqualTo(original.getHeaders().getETag());
      assertThat(storedVersion(9611)).isGreaterThan(originalVersion);
      assertThat(changed.getBody().path("jornadaSemanal").asInt()).isEqualTo(35);
      assertThat(changed.getBody().path("setorExercicio").asText()).isEqualTo(setor);
      assertThat(changed.getBody().hasNonNull("valorDepFgts")).isTrue();
      assertThat(changed.getBody().path("valorDepFgts").decimalValue()).isEqualByComparingTo(java.math.BigDecimal.ZERO);
      assertThat(changed.getBody().path("correio").booleanValue()).isFalse();
      assertThat(changed.getBody().path("motivo").asText()).isEqualTo("Cadastro preservado da prova de lote");
      assertThat(bulkPost("/bulk/update",Map.of("proposalId",proposalId),key,HttpStatus.OK).path("executionId").asText()).isEqualTo(executionId);
      assertThat(readVinculo("9611:1").getHeaders().getETag()).isEqualTo(changed.getHeaders().getETag());

      var itemProposal=bulkPost("/bulk/update-items/evaluation",Map.of("executionMode","SYNC","items",List.of(
          Map.of("id","9611:1","expectedVersion",etag("9611:1"),"changes",List.of(set("jornadaSemanal",36))),
          Map.of("id","9612:1","expectedVersion",etag("9612:1"),"changes",List.of(set("jornadaSemanal",37))))),null,HttpStatus.OK);
      bulkPost("/bulk/update-items",Map.of("proposalId",itemProposal.path("proposalId").asText()),UUID.randomUUID().toString(),HttpStatus.OK);
      assertThat(readVinculo("9611:1").getBody().path("jornadaSemanal").asInt()).isEqualTo(36);
      assertThat(readVinculo("9612:1").getBody().path("jornadaSemanal").asInt()).isEqualTo(37);
      collectiveCommand("realocar-setor-lote",Map.of("setor",setor,"setorExercicio",setor,"documentoLegalId",documento,"observacao","Ato protegido de prova"));
      assertThat(readVinculo("9611:1").getBody().path("setor").asText()).isEqualTo(setor);
      assertThat(readVinculo("9611:1").getBody().path("motivo").asText()).isEqualTo("Cadastro preservado da prova de lote");
      collectiveCommand("homologar-estagio-lote",Map.of("dtHomologEstagio","2026-10-01","documentoLegalId",documento));
      assertThat(readVinculo("9612:1").getBody().path("dtHomologEstagio").asText()).isEqualTo("2026-10-01");

      var query=bulkPost("/bulk/update/evaluation",Map.of("executionMode","SYNC","selection",Map.of("mode","QUERY",
          "filter",Map.of("buscaAmpla","BULK-308-","jornadaRange",Map.of("min",35,"max",40)),"excludedIds",List.of("9612:1")),
          "changes",List.of(set("descontaIr",false))),null,HttpStatus.OK);
      assertThat(query.path("totals").path("targetCount").asInt()).isEqualTo(1);
      createVinculo(9613);
      bulkPost("/bulk/update",Map.of("proposalId",query.path("proposalId").asText()),UUID.randomUUID().toString(),HttpStatus.OK);
      assertThat(bulkGet("/bulk/proposals/"+query.path("proposalId").asText()).path("totals").path("targetCount").asInt()).isEqualTo(1);
      Files.writeString(Path.of("target/proof/bulk-protocol-receipts.json"),receipts.toString());
    } finally { removeFixtures(); }
  }


  @Test @Order(3)
  void blockedProposalExplainsStaleVersionAndInvalidItemWithoutMutation() throws Exception {
    assertThat(publicationReady).isTrue();
    requireDisposableTarget();
    try {
      createVinculo(9611);createVinculo(9612);
      String stale=etag("9611:1");
      var headers=new HttpHeaders();headers.setContentType(MediaType.APPLICATION_JSON);headers.set("If-Match",stale);
      var patch=rest.exchange("/api/ergon/vinculos/9611:1/lotacao-exercicio",HttpMethod.PATCH,
          new HttpEntity<>(Map.of("jornadaSemanal",37),headers),JsonNode.class);
      assertThat(patch.getStatusCode()).as(String.valueOf(patch.getBody())).isEqualTo(HttpStatus.OK);
      long beforeFirst=storedVersion(9611),beforeSecond=storedVersion(9612);
      var proposal=bulkPost("/bulk/update-items/evaluation",Map.of("executionMode","SYNC","items",List.of(
          Map.of("id","9611:1","expectedVersion",stale,"changes",List.of(set("jornadaSemanal",40))),
          Map.of("id","9612:1","expectedVersion",etag("9612:1"),"changes",List.of(set("jornadaSemanal",169))))),null,HttpStatus.OK);
      assertThat(proposal.path("status").asText()).isEqualTo("BLOCKED");
      assertThat(proposal.path("totals").path("blocked").asInt()).isEqualTo(2);
      String id=proposal.path("proposalId").asText();
      var preview=bulkGet("/bulk/proposals/"+id+"/results?size=50");
      assertThat(preview.path("content")).hasSize(2);
      assertThat(preview.toString()).contains("CONFLICT","JORNADA_FORA_INTERVALO","1 e 168","Atualize a seleção").doesNotContain(stale,"expectedVersion","blockedReason","orgao");
      bulkPost("/bulk/update-items",Map.of("proposalId",id),UUID.randomUUID().toString(),HttpStatus.UNPROCESSABLE_ENTITY);
      assertThat(storedVersion(9611)).isEqualTo(beforeFirst);assertThat(storedVersion(9612)).isEqualTo(beforeSecond);
      assertThat(readVinculo("9611:1").getBody().path("jornadaSemanal").asInt()).isEqualTo(37);
      assertThat(readVinculo("9612:1").getBody().path("jornadaSemanal").asInt()).isEqualTo(36);
      Files.writeString(Path.of("target/proof/bulk-blocked-preview.json"),preview.toString());
    } finally { removeFixtures(); }
  }

  @Test @Order(4)
  void cargosAndPessoasExecuteBothUpdateModesAndClear() throws Exception {
    assertThat(publicationReady).isTrue(); requireDisposableTarget();
    try {
      assertThat(rest.postForEntity("/api/ergon/cargos",Map.of("cargo",9621,"empCodigo",1,
          "nome","Cargo BULK-308-9621","categoria","SAUDE",
          "controleVaga",false,"porReferencia",false,"eAglutinador",false),JsonNode.class).getStatusCode()).isEqualTo(HttpStatus.CREATED);
      assertThat(rest.postForEntity("/api/ergon/servidor-pessoas",Map.of("numero",9621,"nome","Pessoa BULK-308-9621",
          "cpf","00000009621","dtnasc","1990-01-01"),JsonNode.class).getStatusCode()).isEqualTo(HttpStatus.CREATED);
      var evidence=new java.util.ArrayList<JsonNode>();
      for(String resource:List.of("cargos","servidor-pessoas")) {
        String base="/api/ergon/"+resource,field=resource.equals("cargos")?"subcategoria":"agencia";
        Object id=resource.equals("cargos")?(Object)Integer.valueOf(9621):"9621";
        for(String mode:List.of("update","update-items")) {
          var read=rest.getForEntity(base+"/9621",JsonNode.class);
          var changes=mode.equals("update")?List.of(set(field,"PROVA")):List.of(Map.of("field",field,"operator","CLEAR"));
          Object request=mode.equals("update")?Map.of("executionMode","SYNC","selection",Map.of("mode","EXPLICIT",
              "targets",List.of(Map.of("id",id,"expectedVersion",read.getHeaders().getETag()))),"changes",changes)
              :Map.of("executionMode","SYNC","items",List.of(Map.of("id",id,"expectedVersion",read.getHeaders().getETag(),"changes",changes)));
          var proposal=postResource(base+"/bulk/"+mode+"/evaluation",request,null);
          assertThat(proposal.path("status").asText()).isEqualTo("READY");
          var execution=postResource(base+"/bulk/"+mode,Map.of("proposalId",proposal.path("proposalId").asText()),UUID.randomUUID().toString());
          var receipts=rest.getForEntity(base+"/bulk/executions/"+execution.path("executionId").asText()+"/results",JsonNode.class);
          assertThat(receipts.getStatusCode()).isEqualTo(HttpStatus.OK);
          assertThat(receipts.getBody().path("data").path("content").get(0).path("status").asText()).isEqualTo("CONFIRMED");
          var changed=rest.getForEntity(base+"/9621",JsonNode.class);
          if(mode.equals("update")) assertThat(changed.getBody().path("data").path(field).asText()).isEqualTo("PROVA");
          else assertThat(changed.getBody().path("data").hasNonNull(field)).isFalse();
          assertThat(changed.getHeaders().getETag()).isNotEqualTo(read.getHeaders().getETag());
          evidence.add(receipts.getBody());
        }
      }
      Files.writeString(Path.of("target/proof/bulk-cargos-pessoas-receipts.json"),new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(evidence));
    } finally {
      try(var c=ownerConnection();var s=c.createStatement()) {
        s.executeUpdate("delete from cargos where cargo=9621 and nome='Cargo BULK-308-9621'");
        s.executeUpdate("delete from funcionarios where numero=9621 and nome='Pessoa BULK-308-9621'");
      }
    }
  }

  private JsonNode postResource(String path,Object body,String key) {
    var headers=new HttpHeaders();headers.setContentType(MediaType.APPLICATION_JSON);
    if(key!=null)headers.set("Idempotency-Key",key);
    var response=rest.exchange(path,HttpMethod.POST,new HttpEntity<>(body,headers),JsonNode.class);
    assertThat(response.getStatusCode()).as(path+": "+response.getBody()).isEqualTo(HttpStatus.OK);
    return response.getBody().path("data");
  }

  @Test @Order(5)
  void concurrentDiscoveryReturnsVerifiedSnapshotOrSafeUnavailable() throws Exception {
    assertThat(publicationReady).isTrue();
    var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
    var start=new java.util.concurrent.CountDownLatch(1);
    try {
      var calls=new java.util.ArrayList<java.util.concurrent.Future<ResponseEntity<JsonNode>>>();
      for(int i=0;i<2;i++) calls.add(pool.submit(()->{start.await();return rest.getForEntity("/api/ergon/cargos/capabilities",JsonNode.class);}));
      start.countDown();
      var statuses=new java.util.ArrayList<Integer>();
      for(var call:calls) {
        var response=call.get(100,java.util.concurrent.TimeUnit.SECONDS);
        assertThat(response.getStatusCode()).isIn(HttpStatus.OK,HttpStatus.SERVICE_UNAVAILABLE);
        if(response.getStatusCode()==HttpStatus.OK)
          assertThat(response.getBody().path("operations").path("bulk-update").path("bulk").isObject()).isTrue();
        else assertThat(response.getBody().toString()).contains("BULK_DISCOVERY_UNAVAILABLE").doesNotContain("jdbc","SQLException","TransactionTimedOut");
        statuses.add(response.getStatusCode().value());
      }
      Files.writeString(Path.of("target/proof/bulk-concurrent-discovery.json"),new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(Map.of("statuses",statuses,"partialSnapshots",0)));
    } finally {pool.shutdownNow();pool.awaitTermination(10,java.util.concurrent.TimeUnit.SECONDS);}
  }

  private void removeFixtures() throws Exception {
    try(var c=ownerConnection();var s=c.createStatement()) {
      s.executeUpdate("delete from public.vinculos where numfunc in (9611,9612,9613) and matric like 'BULK-308-%'");
      s.executeUpdate("delete from public.funcionarios where numero in (9611,9612,9613) and nome like 'Pessoa BULK-308-%'");
    }
  }

  private java.sql.Connection ownerConnection() throws Exception {
    return java.sql.DriverManager.getConnection(System.getenv("SPRING_DATASOURCE_URL"),System.getenv("BULK_OWNER_USERNAME"),System.getenv("BULK_OWNER_PASSWORD"));
  }
  private void visibleColumns(String resource,List<String> columns) throws Exception {
    String url=org.springframework.web.util.UriComponentsBuilder.fromPath("/schemas/filtered")
        .queryParam("resourcePath","/api/ergon/"+resource+"/all").queryParam("operation","get").queryParam("schemaType","response").build().toUriString();
    var response=rest.getForEntity(url,JsonNode.class);
    assertThat(response.getStatusCode()).as(String.valueOf(response.getBody())).isEqualTo(HttpStatus.OK);
    JsonNode properties=response.getBody().path("properties");
    if(!properties.has(columns.get(0))) properties=response.getBody().path("items").path("properties");
    for(String column:columns) {
      assertThat(properties.has(column)).as(resource+"."+column+" publicado").isTrue();
      assertThat(properties.path(column).path("x-ui").path("tableHidden").asBoolean()).as(resource+"."+column+" visível").isFalse();
    }
    assertThat(java.util.stream.StreamSupport.stream(properties.spliterator(),false)
        .anyMatch(field->field.path("x-ui").path("tableHidden").asBoolean())).as("Detalhes secundários continuam ocultos").isTrue();
    Files.writeString(Path.of("target/proof",resource+"-table-schema.json"),response.getBody().toString());
  }
  private long storedVersion(long pessoa) throws Exception {
    try(var c=ownerConnection();var s=c.prepareStatement("select version from public.vinculos where numfunc=? and numero=1")) {
      s.setLong(1,pessoa);try(var r=s.executeQuery()){assertThat(r.next()).isTrue();return r.getLong(1);}
    }
  }
  private Map<String,Object> set(String field,Object value) { return Map.of("field",field,"operator","SET","value",value); }
  private ResponseEntity<JsonNode> readVinculo(String id) {
    var response=rest.getForEntity("/api/ergon/vinculos/"+id,JsonNode.class);
    assertThat(response.getStatusCode()).as(String.valueOf(response.getBody())).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().hasNonNull("data")).as("GET canônico publica o registro em RestApiResponse.data").isTrue();
    return new ResponseEntity<>(response.getBody().path("data"),response.getHeaders(),response.getStatusCode());
  }
  private String etag(String id) { String value=readVinculo(id).getHeaders().getETag();assertThat(value).isNotBlank();return value; }
  private Map<String,Object> selection(String... ids) {
    return Map.of("mode","EXPLICIT","targets",java.util.Arrays.stream(ids).map(id->Map.of("id",id,"expectedVersion",etag(id))).toList());
  }
  private JsonNode bulkPost(String path,Object body,String key,HttpStatus expected) {
    var headers=new HttpHeaders();headers.setContentType(MediaType.APPLICATION_JSON);if(key!=null)headers.set("Idempotency-Key",key);
    var response=rest.exchange("/api/ergon/vinculos"+path,HttpMethod.POST,new HttpEntity<>(body,headers),JsonNode.class);
    assertThat(response.getStatusCode()).as(path+": "+response.getBody()).isEqualTo(expected);return response.getBody().path("data");
  }
  private JsonNode bulkGet(String path) {
    var response=rest.getForEntity("/api/ergon/vinculos"+path,JsonNode.class);
    assertThat(response.getStatusCode()).as(path+": "+response.getBody()).isEqualTo(HttpStatus.OK);return response.getBody().path("data");
  }
  private void createVinculo(long id) {
    var pessoa=rest.postForEntity("/api/ergon/servidor-pessoas",Map.of("numero",id,"nome","Pessoa BULK-308-"+id,"cpf",String.format("%011d",id),"dtnasc","1990-01-01"),JsonNode.class);
    assertThat(pessoa.getStatusCode()).as(String.valueOf(pessoa.getBody())).isEqualTo(HttpStatus.CREATED);
    var body=new java.util.LinkedHashMap<String,Object>(Map.of("numfunc",id,"numero",1,"empCodigo",1,"matric","BULK-308-"+id,"regimejur","ESTATUTARIO","tipovinc","EFETIVO","categoria","SAUDE","dtexerc","2020-01-01","jornadaSemanal",36,"customFields",Map.of("processo_sei_nomeacao",String.format("SEI-2026.%06d",id))));
    body.put("valorDepFgts",0);body.put("motivo","Cadastro preservado da prova de lote");
    var vinculo=rest.postForEntity("/api/ergon/vinculos",body,JsonNode.class);
    assertThat(vinculo.getStatusCode()).as(String.valueOf(vinculo.getBody())).isEqualTo(HttpStatus.CREATED);
  }
  private void collectiveCommand(String action,Map<String,Object> parameters) {
    var proposal=bulkPost("/bulk/"+action+"/evaluation",Map.of("executionMode","SYNC","selection",selection("9611:1","9612:1"),"parameters",parameters),null,HttpStatus.OK);
    assertThat(proposal.path("totals").path("executable").asInt()).isEqualTo(2);
    var execution=bulkPost("/actions/"+action,Map.of("proposalId",proposal.path("proposalId").asText()),UUID.randomUUID().toString(),HttpStatus.OK);
    var results=bulkGet("/bulk/executions/"+execution.path("executionId").asText()+"/results?size=50");
    assertThat(results.path("content")).hasSize(2);
    results.path("content").forEach(item->assertThat(item.path("status").asText()).isEqualTo("CONFIRMED"));
  }
}
