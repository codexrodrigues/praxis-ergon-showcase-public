package br.com.praxis.ergonshowcase.controller;

import br.com.praxis.ergonshowcase.rules.ErgonCustomFieldErrors;
import org.praxisplatform.uischema.customfields.CustomFieldValidationException;
import org.praxisplatform.uischema.rest.exceptionhandler.GlobalExceptionHandler;
import org.praxisplatform.uischema.rest.response.RestApiResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

/** Retains ERG-21502/422 for JSON Logic while native primitive validation retains its public contract. */
@RestControllerAdvice @Order(Ordered.HIGHEST_PRECEDENCE)
public class ErgonCustomFieldExceptionHandler {
  private final GlobalExceptionHandler fallback;
  public ErgonCustomFieldExceptionHandler(GlobalExceptionHandler fallback) { this.fallback=fallback; }
  @ExceptionHandler(CustomFieldValidationException.class)
  public ResponseEntity<RestApiResponse<Object>> handle(CustomFieldValidationException exception,WebRequest request) {
    return ErgonCustomFieldErrors.isJsonLogic(exception)
        ? fallback.handleRuleViolation(ErgonCustomFieldErrors.violation(exception),request)
        : fallback.handleCustomFieldValidation(exception,request);
  }
}
