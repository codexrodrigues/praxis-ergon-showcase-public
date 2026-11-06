package br.com.praxis.ergonshowcase.config;

import java.time.Instant;
import org.praxisplatform.config.dto.*;
import org.praxisplatform.config.service.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.*;
import org.springframework.web.server.ResponseStatusException;

/** Host adapters over Config's canonical context and persistence contracts. */
@Configuration
public class ErgonBrowserIdentity {
  private final ErgonConfigPrincipals principals;
  public ErgonBrowserIdentity(ErgonConfigPrincipals principals) { this.principals = principals; }

  public ErgonBrowserAccount find(Authentication auth) {
    if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) return null;
    return principals.browserAccounts().stream().filter(a -> a.username().equals(auth.getName())).findFirst().orElse(null);
  }
  public ErgonBrowserAccount require(Authentication auth) {
    var account = find(auth);
    if (account == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "SHOWCASE_SESSION_REQUIRED");
    return account;
  }

  @Bean EnterpriseRuntimeContextProvider ergonRuntimeContextProvider() {
    return request -> {
      var account = require(request.principal() instanceof Authentication a ? a : null);
      var servlet = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
      var session = servlet.getRequest().getSession(false);
      if (session == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "SHOWCASE_SESSION_REQUIRED");
      // Public version is random per issuance; never return the session identifier.
      var version = session.getAttribute("ergon.context.version");
      if (version == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "SHOWCASE_SESSION_REQUIRED");
      return new EnterpriseRuntimeContextResponse("praxis-enterprise-runtime-context.v1", "ready", version.toString(),
          new EnterpriseRuntimeUser(account.username(), account.displayName(), true),
          new EnterpriseRuntimeEffectiveContext(version.toString(),
              new EnterpriseRuntimeTenant(Integer.toString(account.organization()), account.organizationLabel(), true),
              Integer.toString(account.organization()), account.environment(), "pt-BR", "America/Sao_Paulo",
              "synthetic-" + account.profile().name().toLowerCase(java.util.Locale.ROOT), null,
              java.util.List.of("ERGON_READ"), account.capabilities()), null, Instant.now());
    };
  }

  @Bean UiConfigWriteAuthorizer ergonUiConfigWriteAuthorizer() {
    return request -> {
      var account = require(SecurityContextHolder.getContext().getAuthentication());
      if (!Integer.toString(account.organization()).equals(request.tenantId())
          || !account.environment().equals(request.environment())
          || !account.username().equals(request.userId())
          || request.updatedBy() != null && !account.username().equals(request.updatedBy())
          || !("user".equals(request.scope()) && account.capabilities().contains("ui.authoring.configure")
              || "tenant".equals(request.scope()) && account.capabilities().contains("ui.config.tenant.write")))
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "UI_CONFIG_WRITE_DENIED");
    };
  }
}
