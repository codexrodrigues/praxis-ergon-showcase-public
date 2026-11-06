package br.com.praxis.ergonshowcase.config;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Synthetic accounts have a deliberately bounded policy, independent of public demo access. */
final class ErgonBrowserAdmissionFilter extends OncePerRequestFilter {
  private final ErgonBrowserIdentity identities;
  private final Set<String> origins;
  ErgonBrowserAdmissionFilter(ErgonBrowserIdentity identities, Set<String> origins) {
    this.identities = identities; this.origins = origins;
  }
  static String requestPath(HttpServletRequest request) {
    var path = org.springframework.web.util.ServletRequestPathUtils.hasParsedRequestPath(request)
        ? org.springframework.web.util.ServletRequestPathUtils.getParsedRequestPath(request)
        : org.springframework.web.util.ServletRequestPathUtils.parseAndCache(request);
    return path.pathWithinApplication().value();
  }
  @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String path = requestPath(request);
    boolean safe = Set.of("GET", "HEAD", "OPTIONS").contains(request.getMethod());
    if (path.startsWith(ApiPaths.AUTH + "/") && !safe && !origins.contains(request.getHeader("Origin"))) {
      response.sendError(403); return;
    }
    var account = identities.find(SecurityContextHolder.getContext().getAuthentication());
    if (account == null || path.startsWith(ApiPaths.AUTH + "/") || "OPTIONS".equals(request.getMethod())) {
      chain.doFilter(request, response); return;
    }
    var headers = Map.of("X-Tenant", Integer.toString(account.organization()),
        "X-Tenant-ID", Integer.toString(account.organization()), "X-Ergon-Orgao", Integer.toString(account.organization()),
        "X-Env", account.environment(), "X-User-ID", account.username(), "X-Updated-By", account.username());
    for (var entry : headers.entrySet()) {
      var values = Collections.list(request.getHeaders(entry.getKey()));
      if (values.size() > 1 || values.size() == 1 && !entry.getValue().equals(values.getFirst())) {
        response.sendError(403); return;
      }
    }
    var versions = Collections.list(request.getHeaders("X-Praxis-Context-Version"));
    if (versions.size() > 1 || versions.size() == 1 && (request.getSession(false) == null
        || !versions.getFirst().equals(request.getSession(false).getAttribute("ergon.context.version")))) {
      response.sendError(403); return;
    }
    boolean config = path.equals(ApiPaths.CONFIG_UI) || path.startsWith(ApiPaths.CONFIG_UI + "/");
    boolean functions = path.equals(ApiPaths.FUNCOES) || path.startsWith(ApiPaths.FUNCOES + "/");
    boolean read = safe || "POST".equals(request.getMethod()) && functions
        && Set.of("/filter", "/filter/cursor", "/locate", "/options/filter").stream().anyMatch(path::endsWith);
    boolean context = path.equals(ApiPaths.RUNTIME_CONTEXT) && "GET".equals(request.getMethod());
    boolean schemas = path.startsWith("/schemas/") && safe;
    boolean configWrite = config && Set.of("PUT", "DELETE").contains(request.getMethod());
    if (!(context || schemas || functions && read || config && safe || configWrite)) {
      response.sendError(403); return;
    }
    if (configWrite) {
      var scopes = request.getParameterValues("scope");
      String scope = scopes == null ? "user" : scopes.length == 1 ? scopes[0] : "invalid";
      if (!("user".equals(scope) && account.capabilities().contains("ui.authoring.configure")
          || "tenant".equals(scope) && account.capabilities().contains("ui.config.tenant.write"))) {
        response.sendError(403); return;
      }
    }
    chain.doFilter(new HttpServletRequestWrapper(request) {
      @Override public String getHeader(String name) {
        return headers.entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase(name))
            .map(Map.Entry::getValue).findFirst().orElseGet(() -> super.getHeader(name));
      }
      @Override public Enumeration<String> getHeaders(String name) {
        String value = getHeader(name);
        return headers.keySet().stream().anyMatch(k -> k.equalsIgnoreCase(name))
            ? Collections.enumeration(List.of(value)) : super.getHeaders(name);
      }
      @Override public Enumeration<String> getHeaderNames() {
        var names = new LinkedHashSet<>(Collections.list(super.getHeaderNames()));
        names.addAll(headers.keySet()); return Collections.enumeration(names);
      }
    }, response);
  }
}
