package br.com.praxis.ergonshowcase.config;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.policystudio.PolicyStudioAccess;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.HashSet;
import java.util.Set;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Autenticação explícita do showcase; a identidade operacional recebe acesso ao domínio
 * demonstrativo, com suporte governado a CORS para o showcase web no Firebase Hosting e
 * modo opcional de leitura pública demonstrativa ({@code showcase.security.public-read-enabled}).
 */
@Configuration
@EnableConfigurationProperties(ErgonConfigPrincipals.class)
@org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
public class SecurityConfig {
  public static final String ACCUMULATION_CHECK = "ERGON_ACCUMULATION_CHECK";
  private static final List<String> DEFAULT_SHOWCASE_UI_ORIGINS = List.of(
      "https://praxis-ergon-showcase.web.app",
      "https://praxis-ergon-showcase.firebaseapp.com",
      "http://localhost:4200",
      "http://127.0.0.1:4200",
      "http://localhost:4303",
      "http://127.0.0.1:4303",
      "http://localhost:4310",
      "http://127.0.0.1:4310");
  private static final Set<String> CONFIG_ROLES = Set.of(
      "RULE_DEFINITION_READER", "RULE_DEFINITION_AUTHOR", "RULE_DEFINITION_APPROVER",
      "RULE_COMPOSITION_APPROVER", "RULE_SNAPSHOT_PUBLISHER", "RULE_SNAPSHOT_READER",
      "RULE_EXECUTION_OBSERVER", "RULE_SNAPSHOT_OPERATOR");

  @Bean
  UserDetailsService users(
      @Value("${showcase.security.username}") String username,
      @Value("${showcase.security.password}") String password,
      @Value("${SHOWCASE_BULK_ADMIN_USERNAME:}") String adminUsername,
      @Value("${SHOWCASE_BULK_ADMIN_PASSWORD:}") String adminPassword,
      @Value("${showcase.security.accumulation-orgs:}") String accumulationOrganizations,
      @Value("${showcase.security.policy-read-orgs:}") String policyRead,
      @Value("${showcase.security.policy-publish-orgs:}") String policyPublish,
      ErgonConfigPrincipals config) {
    var users = new java.util.ArrayList<UserDetails>();
    var encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
    var operatorAuthorities = new ArrayList<>(List.of("ERGON_READ", "ERGON_WRITE"));
    if (!accumulationOrganizations.isBlank()) operatorAuthorities.add(ACCUMULATION_CHECK);
    operatorAuthorities.addAll(PolicyStudioAccess.authorities(policyRead, policyPublish));
    users.add(User.withUsername(username)
            .password(encoder.encode(password))
            .authorities(operatorAuthorities.toArray(String[]::new))
            .build());
    if (adminUsername.isBlank() != adminPassword.isBlank() || adminUsername.equalsIgnoreCase(username))
      throw new IllegalArgumentException("Administração de lote exige identidade e senha próprias.");
    if (!adminUsername.isBlank())
      users.add(User.withUsername(adminUsername)
          .password(encoder.encode(adminPassword))
          .authorities("ERGON_READ", "ERGON_BULK_ADMIN").build());
    var usernames = new HashSet<String>();
    users.forEach(user -> usernames.add(user.getUsername().toLowerCase(Locale.ROOT)));
    for (var principal : config.configPrincipals()) {
      if (principal.username() == null || principal.username().isBlank()
          || principal.password() == null || principal.password().isBlank()
          || principal.organization() < 1 || principal.organization() > 999_999_999
          || principal.environment() == null || !principal.environment().matches("[a-z][a-z0-9-]{0,31}")
          || principal.roles() == null || principal.roles().isEmpty()
          || !CONFIG_ROLES.containsAll(principal.roles())
          || !usernames.add(principal.username().toLowerCase(Locale.ROOT))) {
        throw new IllegalArgumentException("Principal Config exige conta distinta, credenciais, escopo e papéis oficiais.");
      }
      var authorities = new ArrayList<String>();
      authorities.add("ERGON_CONFIG_SCOPE:" + principal.organization() + ":" + principal.environment());
      principal.roles().forEach(role -> authorities.add("ROLE_" + role));
      users.add(User.withUsername(principal.username()).password(encoder.encode(principal.password()))
          .authorities(authorities.toArray(String[]::new)).build());
    }
    return new InMemoryUserDetailsManager(users);
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource(
      @Value("${showcase.security.cors-origins:}") Set<String> configuredCorsOrigins,
      @Value("${showcase.security.config-origins:}") Set<String> configOrigins) {
    var allowedOrigins = new LinkedHashSet<String>(DEFAULT_SHOWCASE_UI_ORIGINS);
    for (String origin : configuredCorsOrigins) {
      if (origin != null && !origin.isBlank()) {
        allowedOrigins.add(origin.trim());
      }
    }
    for (String origin : configOrigins) {
      if (origin != null && !origin.isBlank()) {
        allowedOrigins.add(origin.trim());
      }
    }
    var cors = new CorsConfiguration();
    cors.setAllowedOrigins(List.copyOf(allowedOrigins));
    cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
    cors.setAllowedHeaders(List.of(
        "Authorization",
        "Content-Type",
        "Accept",
        "Origin",
        "X-Tenant",
        "X-Tenant-ID",
        "X-Ergon-Orgao",
        "X-Env",
        "X-User-ID",
        "If-Match",
        "If-None-Match",
        "Idempotency-Key",
        "X-Requested-With"));
    cors.setExposedHeaders(List.of(
        "ETag",
        "X-Schema-Hash",
        "X-Data-Version",
        "X-XSRF-TOKEN",
        "Location",
        "Content-Disposition"));
    cors.setAllowCredentials(true);
    cors.setMaxAge(3600L);
    var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", cors);
    return source;
  }

  @Bean
  SecurityFilterChain chain(HttpSecurity http,
      @Value("${showcase.security.config-origins:}") Set<String> configOrigins,
      @Value("${showcase.security.public-read-enabled:false}") boolean publicReadEnabled,
      @Value("${showcase.security.policy-read-orgs:}") String policyReadOrgs) throws Exception {
    if (publicReadEnabled) {
      var anonAuthorities = new ArrayList<String>(List.of("ROLE_ANONYMOUS", "ERGON_READ"));
      anonAuthorities.addAll(
          PolicyStudioAccess.authorities(policyReadOrgs.isBlank() ? "1,2,3" : policyReadOrgs, ""));
      http.anonymous(anon -> anon.authorities(anonAuthorities.toArray(String[]::new)));
    }
    return http.csrf(c -> c.disable())
        .cors(Customizer.withDefaults())
        .headers(headers -> headers
            .frameOptions(frame -> frame.deny())
            .contentTypeOptions(Customizer.withDefaults())
            .referrerPolicy(ref -> ref.policy(
                org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)))
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            a -> {
              a.requestMatchers(HttpMethod.OPTIONS, "/**")
                  .permitAll()
                  .requestMatchers("/actuator/health", "/v3/api-docs/**", "/error")
                  .permitAll()
                    .requestMatchers(ApiPaths.PRAXIS_CONFIG, ApiPaths.PRAXIS_CONFIG + "/**",
                        ApiPaths.POLICY_STUDIO + ApiPaths.POLICY_STUDIO_COMPOSITION)
                  .access((authentication, context) -> new AuthorizationDecision(
                      ErgonConfigAccess.authorize(authentication.get(), context.getRequest(), configOrigins)))
                  .requestMatchers(ApiPaths.BULK_PUBLICATION)
                  .hasAuthority("ERGON_BULK_ADMIN");
              if (publicReadEnabled) {
                a.requestMatchers("/praxis/cockpit/**", "/schemas/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/**")
                    .permitAll()
                    .requestMatchers(
                        HttpMethod.POST,
                        ApiPaths.POLICY_STUDIO_SIMULATE,
                        ApiPaths.BASE + "/*/determinations/*",
                        ApiPaths.BASE + "/*/filter",
                        ApiPaths.BASE + "/*/filter/cursor",
                        ApiPaths.BASE + "/*/locate",
                        ApiPaths.BASE + "/*/stats/*",
                        ApiPaths.BASE + "/*/options/**",
                        ApiPaths.BASE + "/*/option-sources/**",
                        ApiPaths.BASE + "/*/export")
                    .permitAll();
              } else {
                a.requestMatchers("/praxis/cockpit/**")
                    .hasAuthority("ERGON_READ")
                    .requestMatchers(HttpMethod.GET, "/**")
                    .hasAuthority("ERGON_READ")
                    .requestMatchers(
                        HttpMethod.POST,
                        ApiPaths.POLICY_STUDIO_SIMULATE,
                        ApiPaths.BASE + "/*/determinations/*",
                        ApiPaths.BASE + "/*/filter",
                        ApiPaths.BASE + "/*/filter/cursor",
                        ApiPaths.BASE + "/*/locate",
                        ApiPaths.BASE + "/*/stats/*",
                        ApiPaths.BASE + "/*/options/**",
                        ApiPaths.BASE + "/*/option-sources/**",
                        ApiPaths.BASE + "/*/export")
                    .hasAuthority("ERGON_READ");
              }
              a.anyRequest().hasAuthority("ERGON_WRITE");
            })
        .httpBasic(b -> {})
        .build();
  }

  /**
   * Garante que {@code DynamicSwaggerConfig} registre os {@code GroupedOpenApi} individuais
   * antes da inicialização de {@code MultipleOpenApiWebMvcResource}, inclusive quando
   * {@code spring.main.lazy-initialization=true} estiver habilitado em containers enxutos.
   */
  @Bean
  static org.springframework.beans.factory.config.BeanFactoryPostProcessor eagerDynamicSwaggerConfigPostProcessor() {
    return beanFactory -> {
      for (String name : beanFactory.getBeanDefinitionNames()) {
        if (name.toLowerCase(Locale.ROOT).contains("dynamicswaggerconfig")) {
          beanFactory.getBeanDefinition(name).setLazyInit(false);
        }
      }
      if (beanFactory.containsBeanDefinition("multipleOpenApiResource")
          && beanFactory.containsBeanDefinition("dynamicSwaggerConfig")) {
        beanFactory.getBeanDefinition("multipleOpenApiResource").setDependsOn("dynamicSwaggerConfig");
      }
    };
  }
}
