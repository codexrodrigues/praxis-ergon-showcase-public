package br.com.praxis.ergonshowcase.bulk;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description="Resultado do CAS administrativo; não confere autorização para executar alterações no domínio.")
public record BulkPublicationResult(
    @Schema(description="Identidade da confirmação cuja publicação foi administrada.") String operationId,
    @Schema(description="Geração devolvida pelo controle durável após a transição.") long generation,
    @Schema(description="Estado publicado ou suspenso confirmado pelo SDK.") String state) {}
