package br.com.praxis.ergonshowcase.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;

class ErgonConfigAccessTest {
  private static final String ORIGIN = "https://studio.example.test";
  private final UsernamePasswordAuthenticationToken operator =
      UsernamePasswordAuthenticationToken.authenticated("operator", null,
          AuthorityUtils.createAuthorityList("ERGON_CONFIG_SCOPE:2:stage", "ROLE_RULE_SNAPSHOT_READER"));

  @Test
  void publishesOnlyTheAuthenticatedScope() {
    var request = request();
    request.addHeader("X-Tenant-ID", "2");
    assertThat(ErgonConfigAccess.authorize(operator, request, Set.of(ORIGIN))).isTrue();
    assertThat(request.getAttribute("tenantId")).isEqualTo("2");
    assertThat(request.getAttribute("environment")).isEqualTo("stage");
  }

  @Test
  void rejectsForgedScopeAndDisallowedOrigin() {
    for (String header : Set.of("X-Tenant-ID", "X-Env", "X-User-ID")) {
      var request = request();
      request.addHeader(header, "forged");
      assertThat(ErgonConfigAccess.authorize(operator, request, Set.of(ORIGIN))).isFalse();
      assertThat(request.getAttribute("tenantId")).isNull();
    }
    assertThat(ErgonConfigAccess.authorize(operator, request(), Set.of("https://other.example.test")))
        .isFalse();
  }

  @Test
  void rejectsHeadersAsAnAlternativeToAGrant() {
    var request = request();
    request.addHeader("X-Tenant-ID", "2");
    request.addHeader("X-Env", "stage");
    var domainUser = UsernamePasswordAuthenticationToken.authenticated("operator", null,
        AuthorityUtils.createAuthorityList("ERGON_WRITE", "ERGON_POLICY_PUBLISH:2"));
    assertThat(ErgonConfigAccess.authorize(domainUser, request, Set.of(ORIGIN))).isFalse();
  }

  @Test
  void provisionsConfigAccountsWithoutDomainWriteOrInheritedRoles() {
    var users = new SecurityConfig().users("domain", "test", "", "", "", "2", "2",
        new ErgonConfigPrincipals(List.of(
            principal("author", "RULE_DEFINITION_AUTHOR"),
            principal("reviewer", "RULE_DEFINITION_APPROVER"))));
    assertThat(users.loadUserByUsername("author").getAuthorities()).extracting("authority")
        .containsExactlyInAnyOrder("ERGON_CONFIG_SCOPE:2:stage", "ROLE_RULE_DEFINITION_AUTHOR");
    assertThat(users.loadUserByUsername("reviewer").getAuthorities()).extracting("authority")
        .containsExactlyInAnyOrder("ERGON_CONFIG_SCOPE:2:stage", "ROLE_RULE_DEFINITION_APPROVER");
    assertThat(users.loadUserByUsername("domain").getAuthorities()).extracting("authority")
        .doesNotContain("ERGON_CONFIG_SCOPE:2:stage", "ROLE_RULE_DEFINITION_APPROVER");
  }

  @Test
  void rejectsCaseInsensitiveCollisionsWithDomainAccounts() {
    var config = new ErgonConfigPrincipals(List.of(principal("DOMAIN", "RULE_SNAPSHOT_PUBLISHER")));
    assertThatIllegalArgumentException().isThrownBy(
        () -> new SecurityConfig().users("domain", "test", "", "", "", "2", "2", config));
  }

  @Test
  void configuresCorsForFirebaseHostingAndLocalShowcase() {
    var source = new SecurityConfig().corsConfigurationSource(
        Set.of("https://custom-ergon.example.test"),
        Set.of(ORIGIN));
    var request = new MockHttpServletRequest("OPTIONS", "/api/ergon/vinculos/filter");
    var cors = source.getCorsConfiguration(request);
    assertThat(cors).isNotNull();
    assertThat(cors.getAllowedOrigins()).contains(
        "https://praxis-ergon-showcase.web.app",
        "https://praxis-ergon-showcase.firebaseapp.com",
        "http://localhost:4200",
        "http://localhost:4303",
        "http://localhost:4310",
        "https://custom-ergon.example.test",
        ORIGIN);
    assertThat(cors.getAllowedHeaders()).contains(
        "Authorization", "X-Tenant", "X-Ergon-Orgao", "If-Match", "Idempotency-Key");
    assertThat(cors.getExposedHeaders()).contains(
        "ETag", "X-Schema-Hash", "X-Data-Version", "X-XSRF-TOKEN", "Location");
    assertThat(cors.getAllowCredentials()).isTrue();
  }

  private static ErgonConfigPrincipals.Principal principal(String username, String role) {
    return new ErgonConfigPrincipals.Principal(username, "test", 2, "stage", List.of(role));
  }

  private static MockHttpServletRequest request() {
    var request = new MockHttpServletRequest();
    request.addHeader("Origin", ORIGIN);
    return request;
  }
}
