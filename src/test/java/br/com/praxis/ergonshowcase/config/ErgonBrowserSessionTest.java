package br.com.praxis.ergonshowcase.config;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.*;
import org.junit.jupiter.api.*;
import org.praxisplatform.config.controller.UserConfigController;
import org.praxisplatform.config.domain.UiUserConfig;
import org.praxisplatform.config.service.UserConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@SpringJUnitWebConfig(ErgonBrowserSessionTest.App.class)
@TestPropertySource(properties = {"showcase.security.username=", "showcase.security.password=",
    "showcase.security.domain-environment=local", "showcase.security.config-origins=http://127.0.0.1:4303",
    "showcase.security.public-read-enabled=true"})
class ErgonBrowserSessionTest {
  static final String ORIGIN = "http://127.0.0.1:4303";
  static final String PASSWORD = UUID.randomUUID().toString();
  static final String HASH = PasswordEncoderFactories.createDelegatingPasswordEncoder().encode(PASSWORD);
  @Configuration @EnableWebMvc @EnableWebSecurity
  @org.springframework.boot.autoconfigure.ImportAutoConfiguration(org.praxisplatform.config.autoconfig.EnterpriseRuntimeAutoConfiguration.class)
  @Import({SecurityConfig.class, ErgonBrowserIdentity.class, ErgonSessionController.class, UserConfigController.class, Probe.class,
      org.praxisplatform.config.service.AiApiKeyProtectionService.class, org.praxisplatform.config.service.AiApiKeyCryptoService.class})
  static class App {
    @Bean com.fasterxml.jackson.databind.ObjectMapper objectMapper() {
      return new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules();
    }
    @Bean @Primary ErgonConfigPrincipals accounts() {
      return new ErgonConfigPrincipals(List.of(), List.of(account("prova.ana", 1, ErgonBrowserAccount.Profile.PERSONAL),
          account("prova.bia", 1, ErgonBrowserAccount.Profile.PERSONAL),
          account("prova.gestor", 1, ErgonBrowserAccount.Profile.MANAGER),
          account("prova.consulta", 1, ErgonBrowserAccount.Profile.READER),
          account("prova.empresa", 2, ErgonBrowserAccount.Profile.PERSONAL)));
    }
    @Bean UserConfigService configService() { return mock(UserConfigService.class); }
  }
  @RestController static class Probe {
    @GetMapping("/api/ergon/funcoes/probe") Map<String,String> read(@RequestHeader("X-Tenant") String tenant) { return Map.of("tenant", tenant); }
  }
  static ErgonBrowserAccount account(String username, int org, ErgonBrowserAccount.Profile profile) {
    return new ErgonBrowserAccount(username, HASH, username, org, "Empresa sintética " + org, "local", profile, "atendimento");
  }
  @Autowired WebApplicationContext context;
  @Autowired UserConfigService configs;
  MockMvc mvc;
  @BeforeEach void setup() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    reset(configs);
    when(configs.getResolved(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(Optional.empty());
    when(configs.upsert(any(), anyString(), anyString(), anyString(), anyString(), anyString(), any(), any(), nullable(String.class), anyString()))
        .thenAnswer(invocation -> {
          var saved = new UiUserConfig(); saved.setPayload("{\"density\":\"compact\"}"); saved.setEtag(UUID.randomUUID());
          saved.setVersion(1L); return saved;
        });
  }
  record Session(MockHttpSession cookie, String csrf) {}
  Session login(String username) throws Exception {
    var bootstrap = mvc.perform(get("/auth/session")).andExpect(status().isUnauthorized()).andReturn();
    var session = (MockHttpSession) bootstrap.getRequest().getSession();
    mvc.perform(post("/auth/login").session(session).header("Origin", ORIGIN)
        .header("X-XSRF-TOKEN", bootstrap.getResponse().getHeader("X-XSRF-TOKEN"))
        .param("username", username).param("password", PASSWORD)).andExpect(status().isNoContent());
    var confirmed = mvc.perform(get("/auth/session").session(session)).andExpect(status().isNoContent()).andReturn();
    return new Session(session, confirmed.getResponse().getHeader("X-XSRF-TOKEN"));
  }
  MockHttpServletRequestBuilder configWrite(Session session, String scope) {
    return put("/api/praxis/config/ui").session(session.cookie()).header("Origin", ORIGIN)
        .header("X-XSRF-TOKEN", session.csrf()).param("componentType", "praxis-table").param("componentId", "auth-proof")
        .param("scope", scope).contentType("application/json").content("{\"payload\":{\"density\":\"compact\"}}");
  }
  @Test void invalidCredentialAndCsrfAreDenied() throws Exception {
    var bootstrap = mvc.perform(get("/auth/session")).andReturn();
    var session = (MockHttpSession) bootstrap.getRequest().getSession();
    mvc.perform(post("/auth/login").session(session).header("Origin", ORIGIN)
        .param("username", "prova.ana").param("password", PASSWORD)).andExpect(status().isForbidden());
    mvc.perform(post("/auth/login").session(session).header("Origin", ORIGIN)
        .header("X-XSRF-TOKEN", bootstrap.getResponse().getHeader("X-XSRF-TOKEN"))
        .param("username", "prova.ana").param("password", "invalid")).andExpect(status().isUnauthorized());
  }
  @Test void loginOriginMustBeExplicitlyAllowed() throws Exception {
    mvc.perform(post("/auth/login").header("Origin", "https://untrusted.example")).andExpect(status().isForbidden());
  }
  @Test void serverConfirmsIdentityCompanyEnvironmentAndSafeCapabilities() throws Exception {
    var session = login("prova.ana");
    mvc.perform(get("/api/praxis/runtime/context").session(session.cookie()))
        .andExpect(status().isOk()).andExpect(jsonPath("$.user.userId").value("prova.ana"))
        .andExpect(jsonPath("$.user.resolvedFromServerPrincipal").value(true))
        .andExpect(jsonPath("$.effectiveContext.activeTenant.tenantId").value("1"))
        .andExpect(jsonPath("$.effectiveContext.environment").value("local"))
        .andExpect(jsonPath("$.effectiveContext.capabilities").value(org.hamcrest.Matchers.hasItem("ui.authoring.configure")));
    mvc.perform(get("/api/ergon/funcoes/probe").session(session.cookie()))
        .andExpect(status().isOk()).andExpect(jsonPath("$.tenant").value("1"));
  }
  @Test void forgedAndRepeatedHeadersNeverGrantContext() throws Exception {
    var session = login("prova.ana");
    for (String header : List.of("X-Tenant", "X-Tenant-ID", "X-Ergon-Orgao", "X-Env", "X-User-ID", "X-Updated-By")) {
      mvc.perform(get("/api/praxis/runtime/context").session(session.cookie()).header(header, "forged"))
          .andExpect(status().isForbidden());
      mvc.perform(get("/api/praxis/runtime/context").session(session.cookie()).header(header, "1", "1"))
          .andExpect(status().isForbidden());
    }
  }
  @Test void personalConfigUsesActualPrincipalAndCannotWriteCommon() throws Exception {
    var session = login("prova.ana");
    mvc.perform(configWrite(session, "user")).andExpect(status().isOk());
    verify(configs).upsert(eq(UserConfigService.Scope.USER), eq("1"), eq("prova.ana"), eq("praxis-table"), eq("auth-proof"),
        eq("local"), any(), nullable(com.fasterxml.jackson.databind.JsonNode.class), nullable(String.class), eq("prova.ana"));
    mvc.perform(configWrite(session, "tenant")).andExpect(status().isForbidden());
    verifyNoMoreInteractions(configs);
  }
  @Test void anIssuedBrowserSessionCannotWritePersonalConfigWithoutCsrf() throws Exception {
    var session = login("prova.ana");
    mvc.perform(put("/api/praxis/config/ui").session(session.cookie()).header("Origin", ORIGIN)
        .param("componentType", "praxis-table").param("componentId", "csrf-proof").param("scope", "user")
        .contentType("application/json").content("{\"payload\":{\"density\":\"compact\"}}"))
        .andExpect(status().isForbidden());
    verifyNoInteractions(configs);
  }
  @Test void readerCannotCustomizeAndManagerCanWriteCommon() throws Exception {
    var reader = login("prova.consulta");
    mvc.perform(configWrite(reader, "user")).andExpect(status().isForbidden());
    mvc.perform(configWrite(reader, "tenant")).andExpect(status().isForbidden());
    verifyNoInteractions(configs);
    var manager = login("prova.gestor");
    mvc.perform(configWrite(manager, "tenant")).andExpect(status().isOk());
    verify(configs).upsert(eq(UserConfigService.Scope.TENANT), eq("1"), eq("prova.gestor"), anyString(), anyString(),
        eq("local"), any(), nullable(com.fasterxml.jackson.databind.JsonNode.class), nullable(String.class), eq("prova.gestor"));
  }
  @Test void sameCompanyUsersAndAnotherCompanyReachConfigSeparately() throws Exception {
    for (String username : List.of("prova.ana", "prova.bia", "prova.empresa")) {
      var session = login(username);
      mvc.perform(get("/api/praxis/config/ui").session(session.cookie()).header("Origin", ORIGIN)
          .param("componentType", "praxis-table").param("componentId", "auth-proof")).andExpect(status().isNotFound());
      verify(configs).getResolved(eq(username.equals("prova.empresa") ? "2" : "1"), eq(username), eq("praxis-table"), eq("auth-proof"), eq("local"));
    }
  }
  @Test void publicDemoSettingDoesNotBypassSyntheticPolicy() throws Exception {
    var session = login("prova.ana");
    mvc.perform(get("/api/ergon/servidor-pessoas").session(session.cookie())).andExpect(status().isForbidden());
    mvc.perform(post("/api/ergon/funcoes").session(session.cookie()).header("X-XSRF-TOKEN", session.csrf()))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/praxis/config/domain-rules/definitions").session(session.cookie())).andExpect(status().isForbidden());
  }
  @Test void logoutInvalidatesTheServerSession() throws Exception {
    var session = login("prova.ana");
    mvc.perform(post("/auth/logout").session(session.cookie()).header("Origin", ORIGIN).header("X-XSRF-TOKEN", session.csrf()))
        .andExpect(status().isNoContent());
    assertThat(session.cookie().isInvalid()).isTrue();
    mvc.perform(get("/auth/session")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/praxis/runtime/context")).andExpect(status().isUnauthorized());
  }
}
