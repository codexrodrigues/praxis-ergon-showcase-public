package br.com.praxis.ergonshowcase;

import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import java.sql.DriverManager;
import java.util.*;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;

/** Fixture administrativa explícita; somente o kernel realiza transições de controle. */
final class BulkPublicationProof {
  private BulkPublicationProof() {}
  static void publish(TestRestTemplate http, boolean reset) throws Exception {
    // Bound a broken network independently of the SDK's operational budgets.
    var transport=new org.springframework.http.client.SimpleClientHttpRequestFactory();
    transport.setConnectTimeout(java.time.Duration.ofSeconds(10));
    transport.setReadTimeout(java.time.Duration.ofSeconds(90));
    http.getRestTemplate().setRequestFactory(transport);
    var generations=new LinkedHashMap<String,Long>();
    var states=new HashMap<String,String>();
    readControls(generations,states);
    assertThat(generations).hasSize(8);
    // One explicit reconciliation installs the exact committed photograph locally.
    // The capability probes below still re-attest each operation's independent READY record.
    if(!reset && states.values().stream().allMatch("READY"::equals)) {
      var entry=generations.entrySet().iterator().next();
      var response=http.postForEntity("/api/ergon/operations/bulk/publication",Map.of("operationId",entry.getKey(),"expectedGeneration",entry.getValue(),"intent","RECONCILE"),JsonNode.class);
      assertThat(response.getStatusCode()).as(entry.getKey()+" explicit reconciliation: "+response.getBody()).isEqualTo(HttpStatus.OK);
      return;
    }
    if(reset) {
      var entry=generations.entrySet().iterator().next();
      var response=http.postForEntity("/api/ergon/operations/bulk/publication",Map.of("operationId",entry.getKey(),"expectedGeneration",entry.getValue(),"intent","SUSPEND"),JsonNode.class);
      assertThat(response.getStatusCode()).as(entry.getKey()+" suspension: "+response.getBody()).isEqualTo(HttpStatus.OK);
      // The global fence also suspends other READY operations and advances their generations.
      // Read the resulting committed CAS identities rather than reusing the pre-suspension map.
      generations.clear();states.clear();readControls(generations,states);
    }
    for(var entry:generations.entrySet()) {
      var response=http.postForEntity("/api/ergon/operations/bulk/publication",Map.of("operationId",entry.getKey(),"expectedGeneration",entry.getValue(),"intent",states.get(entry.getKey()).equals("READY") ? "RECONCILE" : "PUBLISH"),JsonNode.class);
      assertThat(response.getStatusCode()).as(entry.getKey()+" publication: "+response.getBody()).isEqualTo(HttpStatus.OK);
    }
  }
  private static void readControls(Map<String,Long> generations,Map<String,String> states) throws Exception {
    try(var connection=DriverManager.getConnection(System.getenv("SPRING_DATASOURCE_URL"),System.getenv("BULK_OWNER_USERNAME"),System.getenv("BULK_OWNER_PASSWORD"));
        var statement=connection.prepareStatement("select operation_id,generation,state from praxis_bulk.praxis_bulk_operation_control where namespace_id=? order by operation_id")) {
      statement.setString(1,"ergon-showcase");
      try(var rows=statement.executeQuery()) { while(rows.next()) { generations.put(rows.getString(1),rows.getLong(2));states.put(rows.getString(1),rows.getString(3)); } }
    }
  }
}
