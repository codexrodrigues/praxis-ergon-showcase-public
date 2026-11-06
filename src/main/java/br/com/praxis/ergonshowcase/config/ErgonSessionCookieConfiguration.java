package br.com.praxis.ergonshowcase.config;

import org.apache.tomcat.util.http.Rfc6265CookieProcessor;
import org.springframework.core.env.Environment;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Partition cross-site HTTPS sessions by the top-level application, including cookie removal. */
@Configuration(proxyBeanMethods = false)
public class ErgonSessionCookieConfiguration {
  @Bean
  WebServerFactoryCustomizer<TomcatServletWebServerFactory> ergonSessionCookiePolicy(
      Environment environment) {
    boolean secure = environment.getProperty("server.servlet.session.cookie.secure", Boolean.class, false);
    String sameSite = environment.getProperty("server.servlet.session.cookie.same-site", "lax");
    return factory -> factory.addContextCustomizers(context -> {
      if (secure && "none".equalsIgnoreCase(sameSite)) {
        var processor = new Rfc6265CookieProcessor();
        processor.setPartitioned(true);
        processor.setSameSiteCookies("none");
        context.setCookieProcessor(processor);
      }
    });
  }
}
