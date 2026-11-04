package br.com.praxis.ergonshowcase.rules;

import org.praxisplatform.uischema.customfields.CustomFieldValidationException;
import org.praxisplatform.rules.contract.*;

/** Maps the native declarative validation evidence to the existing Ergon atomic error contract. */
public final class ErgonCustomFieldErrors {
  private ErgonCustomFieldErrors() { }
  public static boolean isJsonLogic(CustomFieldValidationException exception) {
    return exception.getFieldErrors().stream().anyMatch(error -> "CUSTOM_FIELD_JSON_LOGIC".equals(error.getCode()));
  }
  public static ErgonRuleViolationException violation(CustomFieldValidationException exception) {
    return new ErgonRuleViolationException(exception.getFieldErrors().stream().map(error ->
        ErgonRuleViolationException.detail(
            "CUSTOM_FIELD_JSON_LOGIC".equals(error.getCode()) ? ErgonRuleErrorCatalog.ERG_21502.code()
                : error.getCode() == null ? "CUSTOM_FIELD_VALIDATION" : error.getCode(),
            ErgonRuleErrorCatalog.ERG_21502.slotKey(),
            "product.ergon.customField." + error.getField().replace("customFields.", ""),
            DecisionSource.PRODUCT, DecisionStage.DOMAIN_DECISION, error.getField(),
            error.getDefaultMessage() == null ? ErgonRuleErrorCatalog.ERG_21502.messagePtBr() : error.getDefaultMessage()))
        .toList());
  }
}
