package br.com.praxis.ergonshowcase.controller;

import br.com.praxis.ergonshowcase.bulk.ErgonBulkOperations;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import org.praxisplatform.uischema.annotation.ApiGroup;
import org.praxisplatform.uischema.rest.response.RestApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Publicação administrativa explícita do contrato já instalado; não executa DDL. */
@RestController
@RequestMapping("/api/ergon/operations/bulk")
@ApiGroup("ergon")
public class BulkPublicationController {
  private final ErgonBulkOperations operations;
  public BulkPublicationController(ErgonBulkOperations operations) { this.operations=operations; }
  @PostMapping("/publication")
  @PreAuthorize("hasAuthority('ERGON_BULK_ADMIN')")
  @Operation(operationId="publishErgonBulkOperation",summary="Administrar publicação do lote",description="Executa um único CAS explícito de publicação, reconciliação ou suspensão, sem repetir automaticamente uma geração divergente.")
  public RestApiResponse<br.com.praxis.ergonshowcase.bulk.BulkPublicationResult> transition(
      @jakarta.validation.Valid @RequestBody br.com.praxis.ergonshowcase.bulk.BulkPublicationRequest request) {
    return RestApiResponse.success(operations.publication(request),null);
  }
}
