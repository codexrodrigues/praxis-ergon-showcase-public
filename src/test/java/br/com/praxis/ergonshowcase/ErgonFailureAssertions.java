package br.com.praxis.ergonshowcase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.com.praxis.ergonshowcase.rules.ErgonRuleErrorCatalog;
import br.com.praxis.ergonshowcase.rules.ErgonRuleViolationException;
import org.praxisplatform.rules.contract.DecisionSource;
import org.praxisplatform.rules.violation.PraxisRuleViolationException;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;

/** Asserções do status HTTP para guardas estruturais e para a rejeição canônica de regras. */
final class ErgonFailureAssertions {
  private ErgonFailureAssertions() { }

  static void assertViolation(String code, DecisionSource source, Runnable call) {
    var error = assertThrows(ErgonRuleViolationException.class, call::run);
    var descriptor = ErgonRuleErrorCatalog.find(code).orElseThrow();
    assertThat(error.getHttpStatus()).isEqualTo(422);
    assertThat(error.getReasonCodes()).containsExactly(code);
    assertThat(error.getViolations()).singleElement().satisfies(violation -> {
      assertThat(violation.slotKey()).isNotBlank().isEqualTo(descriptor.slotKey());
      assertThat(violation.targetField()).isNotBlank().isEqualTo(descriptor.targetField());
      assertThat(violation.lineage()).isNotBlank()
          .isEqualTo(descriptor.plsqlOrigin() + ": " + descriptor.plsqlLineage());
      assertThat(descriptor.plsqlLineage()).isNotBlank();
      assertThat(violation.source()).isEqualTo(source);
    });
  }

  static HttpStatusCode status(Throwable failure) {
    if (failure instanceof PraxisRuleViolationException rule) return HttpStatusCode.valueOf(rule.getHttpStatus());
    if (failure instanceof ResponseStatusException structural) return structural.getStatusCode();
    throw new AssertionError("Esperava guarda HTTP ou rejeição canônica de regra.", failure);
  }
  static String reason(Throwable failure) {
    if (failure instanceof PraxisRuleViolationException rule) return rule.getMessage();
    if (failure instanceof ResponseStatusException structural) return structural.getReason();
    throw new AssertionError("Esperava guarda HTTP ou rejeição canônica de regra.", failure);
  }
}
