package br.com.praxis.ergonshowcase.controller;

import org.praxisplatform.uischema.rest.response.RestApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

/** Published item endpoints return the representation directly; collection endpoints use envelopes. */
final class ErgonHttpContract {
  private ErgonHttpContract() { }
  @SuppressWarnings("unchecked")
  static <T> ResponseEntity<T> itemBody(ResponseEntity<?> response) {
    Object body = response.getBody();
    if (response.getStatusCode().is2xxSuccessful() && body instanceof RestApiResponse<?> envelope) body = envelope.getData();
    return new ResponseEntity<>((T) body, response.getHeaders(), response.getStatusCode());
  }
  static void childPage(int page, int size) {
    if (page < 0 || size < 1 || size > 200)
      throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Paginação fora do limite permitido.");
  }
}
