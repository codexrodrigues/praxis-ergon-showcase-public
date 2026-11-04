package br.com.praxis.ergonshowcase.error;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Invalid input to a host operation, distinct from a decision denied by a Praxis RuleSet. */
public final class ErgonInputValidationException extends ResponseStatusException {
  public ErgonInputValidationException(String reason) {
    super(HttpStatus.UNPROCESSABLE_ENTITY, reason);
  }
}
