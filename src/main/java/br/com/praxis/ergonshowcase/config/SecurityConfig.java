package br.com.praxis.ergonshowcase.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Autenticação explícita do showcase; a identidade operacional recebe acesso ao domínio
 * demonstrativo.
 */
@Configuration
@org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
public class SecurityConfig {
  @Bean
  UserDetailsService users(
      @Value("${showcase.security.username}") String username,
      @Value("${showcase.security.password}") String password) {
    return new InMemoryUserDetailsManager(
        User.withUsername(username)
            .password(PasswordEncoderFactories.createDelegatingPasswordEncoder().encode(password))
            .authorities("ERGON_READ", "ERGON_WRITE")
            .build());
  }

  @Bean
  SecurityFilterChain chain(HttpSecurity http) throws Exception {
    return http.csrf(c -> c.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers("/actuator/health", "/v3/api-docs/**")
                    .permitAll()
                    .requestMatchers(org.springframework.http.HttpMethod.GET, "/**")
                    .hasAuthority("ERGON_READ")
                    .requestMatchers(
                        org.springframework.http.HttpMethod.POST,
                        "/api/ergon/*/determinations/*",
                        "/api/ergon/*/filter",
                        "/api/ergon/*/filter/cursor",
                        "/api/ergon/*/locate",
                        "/api/ergon/*/stats/*",
                        "/api/ergon/*/options/**",
                        "/api/ergon/*/option-sources/**",
                        "/api/ergon/*/export")
                    .hasAuthority("ERGON_READ")
                    .anyRequest()
                    .hasAuthority("ERGON_WRITE"))
        .httpBasic(b -> {})
        .build();
  }
}
