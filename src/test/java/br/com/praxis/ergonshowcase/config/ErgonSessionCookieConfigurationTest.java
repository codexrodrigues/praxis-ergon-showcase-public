package br.com.praxis.ergonshowcase.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.Cookie;
import org.apache.catalina.core.StandardContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.mock.env.MockEnvironment;
import org.apache.tomcat.util.http.Rfc6265CookieProcessor;

class ErgonSessionCookieConfigurationTest {
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
