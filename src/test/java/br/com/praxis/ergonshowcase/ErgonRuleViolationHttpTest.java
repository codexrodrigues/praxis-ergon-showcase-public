package br.com.praxis.ergonshowcase;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.praxis.ergonshowcase.controller.ErgonCustomFieldExceptionHandler;
import br.com.praxis.ergonshowcase.rules.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.praxisplatform.rules.contract.*;
import org.praxisplatform.rules.violation.PraxisRuleViolationException;
import org.praxisplatform.uischema.command.*;
import org.praxisplatform.uischema.customfields.CustomFieldValidationException;
import org.praxisplatform.uischema.rest.exceptionhandler.GlobalExceptionHandler;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

/** Fronteira HTTP real do handler e executor; a fixture não inicia JPA nem afirma prova de banco. */
class ErgonRuleViolationHttpTest {
  MockMvc mvc;
  @BeforeEach void setup() {
    var canonical = new GlobalExceptionHandler();
    mvc = MockMvcBuilders.standaloneSetup(new WriteBoundary())
        .setControllerAdvice(new ErgonCustomFieldExceptionHandler(canonical), canonical).build();
  }

  @ParameterizedTest
  @ValueSource(strings = {"ERG-08393", "ERG-00424", "ERG-04300", "ERG-03193", "ERG-00300", "ERG-01850"})
  void c01PreservesCanonicalErrorsAcrossRestIntentsActionsAndCollectionCommands(String code) throws Exception {
    int status = code.equals("ERG-01850") ? 422 : 409;
    var descriptor = ErgonRuleErrorCatalog.find(code).orElseThrow();
    for (var request : List.of(post("/writes/" + code), put("/writes/" + code),
        patch("/commands/" + code), post("/commands/" + code), post("/bulk/" + code))) {
      mvc.perform(request).andExpect(status().is(status))
          .andExpect(jsonPath("$.message").value(descriptor.messagePtBr()))
          .andExpect(jsonPath("$.errors[0].code").value(code))
          .andExpect(jsonPath("$.errors[0].status").value(status))
          .andExpect(jsonPath("$.errors[0].target").value(descriptor.targetField()))
          .andExpect(jsonPath("$.errors[0].targetField").value(descriptor.targetField()))
          .andExpect(jsonPath("$.errors[0].reasonCodes[0]").value(code))
          .andExpect(jsonPath("$.errors[0].slotKeys[0]").value(descriptor.slotKey()))
          .andExpect(jsonPath("$.errors[0].decisionSources[0]").value("PRODUCT"))
          .andExpect(jsonPath("$.errors[0].violations[0].bindingKey").value("product.regression"))
          .andExpect(jsonPath("$.errors[0].violations[0].targetField").value(descriptor.targetField()));
    }
    assertThat(rejection(code)).isInstanceOf(PraxisRuleViolationException.class);
    assertThat(rejection(code).getHttpStatus()).isEqualTo(status);
    assertThat(rejection(code).getStatusCode().value()).isEqualTo(status);
  }

  @Test void customFieldErrorsRetainEveryFailureAndItsExactTargetThroughTheCommandExecutor() throws Exception {
    mvc.perform(patch("/custom-fields")).andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.errors.length()").value(3))
        .andExpect(jsonPath("$.errors[0].code").value("ERG-21502"))
        .andExpect(jsonPath("$.errors[0].target").value("customFields.extra"))
        .andExpect(jsonPath("$.errors[1].target").value("customFields.second"))
        .andExpect(jsonPath("$.errors[2].target").value("customFields.note"))
        .andExpect(jsonPath("$.errors[0].violations.length()").value(3))
        .andExpect(jsonPath("$.errors[0].reasonCodes[1]").value("CUSTOM_FIELD_VALIDATION"));
  }

  @Test void primitiveOnlyCustomFieldFailureKeepsTheNative400Contract() throws Exception {
    mvc.perform(post("/custom-fields/primitive")).andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].target").value("customFields.note"));
  }

  static ErgonRuleViolationException rejection(String code) {
    var descriptor = ErgonRuleErrorCatalog.find(code).orElseThrow();
    return new ErgonRuleViolationException(code, descriptor.slotKey(), "product.regression", DecisionSource.PRODUCT,
        DecisionStage.DOMAIN_DECISION, descriptor.targetField(), descriptor.messagePtBr());
  }
  static FieldError jsonLogicError(String key) {
    return new FieldError("customFields", "customFields." + key, null, false,
        new String[]{"CUSTOM_FIELD_JSON_LOGIC"}, null, "Limite do campo excedido.");
  }
  static void command(RuntimeException rejection, boolean collection) {
    var executor = new GovernedResourceCommandExecutor(request -> { throw rejection; });
    executor.execute(collection
        ? ResourceCommandExecutionRequest.collection("regression", "/bulk", "validate", null, null)
        : ResourceCommandExecutionRequest.item("regression", "/writes", "validate", 1L, null, null));
  }
  @RestController
  static class WriteBoundary {
    @RequestMapping(path = "/writes/{code}", method = {RequestMethod.POST, RequestMethod.PUT})
    public void write(@PathVariable String code) { throw rejection(code); }
    @RequestMapping(path = "/commands/{code}", method = {RequestMethod.POST, RequestMethod.PATCH})
    public void governed(@PathVariable String code) { command(rejection(code), false); }
    @PostMapping("/bulk/{code}")
    public void bulk(@PathVariable String code) { command(rejection(code), true); }
    @PatchMapping("/custom-fields")
    public void fields() { command(new CustomFieldValidationException(List.of(jsonLogicError("extra"),
        jsonLogicError("second"), new FieldError("customFields", "customFields.note", "Tipo inválido."))), false); }
    @PostMapping("/custom-fields/primitive")
    public void primitive() { throw new CustomFieldValidationException(List.of(
        new FieldError("customFields", "customFields.note", "Tipo inválido."))); }
  }
}
