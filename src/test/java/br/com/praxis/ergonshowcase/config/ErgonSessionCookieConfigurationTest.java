package br.com.praxis.ergonshowcase.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.Cookie;
import org.apache.catalina.core.StandardContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.mock.env.MockEnvironment;
import org.apache.tomcat.util.http.Rfc6265CookieProcessor;

class ErgonSessionCookieConfigurationTest {
  @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
  @org.springframework.boot.autoconfigure.ImportAutoConfiguration({
      org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration.class,
      org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration.class,
      org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration.class})
  @org.springframework.context.annotation.Import(ErgonSessionCookieConfiguration.class)
  static class ActualCookieApp {
    @org.springframework.context.annotation.Bean
    org.springframework.boot.web.servlet.ServletRegistrationBean<jakarta.servlet.http.HttpServlet> cookieServlet() {
      return new org.springframework.boot.web.servlet.ServletRegistrationBean<>(new jakarta.servlet.http.HttpServlet() {
        @Override protected void doGet(jakarta.servlet.http.HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response) {
          request.getSession(true).setAttribute("proof", "session");
          response.setStatus(204);
        }
      }, "/cookie-proof");
    }
  }

  @Test void actualLazyBootServerEmitsPartitionedSessionCookie() throws Exception {
    try (var app = new org.springframework.boot.builder.SpringApplicationBuilder(ActualCookieApp.class)
        .web(org.springframework.boot.WebApplicationType.SERVLET)
        .properties(java.util.Map.of(
            "spring.config.location", "optional:file:./nonexistent-cookie-proof.properties",
            "spring.main.lazy-initialization", "true", "server.port", "8080",
            "server.servlet.session.cookie.name", "ERGONSESSION",
            "server.servlet.session.cookie.secure", "true",
            "server.servlet.session.cookie.http-only", "true",
            "server.servlet.session.cookie.same-site", "none"))
        .run()) {
      var response = java.net.http.HttpClient.newHttpClient().send(
          java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://127.0.0.1:8080/cookie-proof")).GET().build(),
          java.net.http.HttpResponse.BodyHandlers.discarding());
      assertThat(response.statusCode()).isEqualTo(204);
      assertThat(response.headers().firstValue("Set-Cookie").orElseThrow())
          .contains("Secure", "HttpOnly", "SameSite=None", "Partitioned");
    }
  }

  private String header(boolean secure, String sameSite, boolean deletion) {
    var factory = new TomcatServletWebServerFactory();
    var environment = new MockEnvironment().withProperty("server.servlet.session.cookie.secure", Boolean.toString(secure))
        .withProperty("server.servlet.session.cookie.same-site", sameSite);
    new ErgonSessionCookieConfiguration().ergonSessionCookiePolicy(environment).customize(factory);
    var context = new StandardContext();
    context.setCookieProcessor(new Rfc6265CookieProcessor());
    factory.getTomcatContextCustomizers().forEach(customizer -> customizer.customize(context));
    var cookie = new Cookie("ERGONSESSION", deletion ? "" : "synthetic-cookie-proof");
    cookie.setHttpOnly(true); cookie.setSecure(secure); cookie.setPath("/");
    if (deletion) cookie.setMaxAge(0);
    return context.getCookieProcessor().generateHeader(cookie, null);
  }
  @Test void crossSiteHttpsPartitionsTheSessionWithoutRemovingSecurityAttributes() {
    assertThat(header(true, "none", false)).contains("Secure", "HttpOnly", "SameSite=None", "Partitioned");
  }
  @Test void logoutDeletesTheSamePartitionedCookie() {
    assertThat(header(true, "none", true)).contains("Max-Age=0", "Secure", "SameSite=None", "Partitioned");
  }
  @Test void localHttpDoesNotIssueAnInvalidPartitionedCookie() {
    assertThat(header(false, "lax", false)).doesNotContain("Partitioned", "Secure");
  }
  @Test void sameSiteHttpsRetainsItsOrdinaryCookiePolicy() {
    assertThat(header(true, "lax", false)).contains("Secure").doesNotContain("Partitioned");
  }
}
