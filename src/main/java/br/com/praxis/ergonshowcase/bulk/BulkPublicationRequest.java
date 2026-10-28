package br.com.praxis.ergonshowcase.bulk;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/** CAS administrativo explícito; nunca é parâmetro de avaliação ou confirmação cadastral. */
@Schema(description="Transição administrativa de uma operação em lote previamente instalada, vinculada à geração esperada.")
public record BulkPublicationRequest(
    @NotBlank @Schema(description="OperationId exato de uma das oito confirmações declaradas pelo host.") String operationId,
    @Min(0) @Schema(description="Geração de controle conhecida pelo operador; divergência impede a transição sem repetição automática.") long expectedGeneration,
    @NotNull @Schema(description="Publicar, reconciliar a fotografia já publicada ou suspender explicitamente a operação.") Intent intent) {
  public enum Intent { PUBLISH, RECONCILE, SUSPEND }
}
