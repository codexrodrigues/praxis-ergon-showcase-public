package br.com.praxis.ergonshowcase.config;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

/** CSRF bootstrap and session confirmation. Login/logout are Spring Security filters. */
@RestController
public class ErgonSessionController {
  private final ErgonBrowserIdentity identities;
  public ErgonSessionController(ErgonBrowserIdentity identities) { this.identities = identities; }

  @GetMapping(ApiPaths.AUTH_SESSION)
  @Operation(summary = "Confirmar sessão do Showcase e obter token CSRF",
      description = "204 confirma sessão sintética; 401 indica ausência de sessão. Ambos retornam X-XSRF-TOKEN para o próximo comando. Nenhuma credencial é retornada.")
  public ResponseEntity<Void> session(Authentication authentication, HttpServletRequest request, CsrfToken csrf) {
    var active = identities.find(authentication) != null && request.getSession(false) != null
        && request.getSession(false).getAttribute("ergon.context.version") != null;
    return ResponseEntity.status(active ? 204 : 401).header("X-XSRF-TOKEN", csrf.getToken())
        .header("Cache-Control", "no-store").build();
  }
}
