package br.com.praxis.ergonshowcase.config;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

final class ErgonConfigAccess {
  private ErgonConfigAccess() {}

  static boolean authorize(Authentication authentication, HttpServletRequest request, Set<String> allowedOrigins) {
    String origin = browserOrigin(request);
    if (authentication == null || !authentication.isAuthenticated()
        || origin.isBlank() || !allowedOrigins.contains(origin)) return false;
    var scopes = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority)
        .filter(authority -> authority.startsWith("ERGON_CONFIG_SCOPE:")).toList();
    if (scopes.size() != 1) return false;
    String[] scope = scopes.getFirst().split(":", -1);
    if (scope.length != 3 || !scope[1].matches("[1-9][0-9]{0,8}")
        || !scope[2].matches("[a-z][a-z0-9-]{0,31}")
        || !matchesHint(request.getHeader("X-Tenant-ID"), scope[1])
        || !matchesHint(request.getHeader("X-Env"), scope[2])
        || !matchesHint(request.getHeader("X-User-ID"), authentication.getName())) return false;
    request.setAttribute("tenantId", scope[1]);
    request.setAttribute("environment", scope[2]);
    return true;
  }

  private static boolean matchesHint(String hint, String expected) {
    return hint == null || expected.equals(hint);
  }

  private static String browserOrigin(HttpServletRequest request) {
    String origin = request.getHeader("Origin");
    if (origin != null) return origin;
    String referer = request.getHeader("Referer");
    if (referer == null) return "";
    try {
      URI uri = URI.create(referer);
      if (uri.getHost() != null && uri.getUserInfo() == null
          && ("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))) {
        return uri.getScheme() + "://" + uri.getRawAuthority();
      }
    } catch (IllegalArgumentException invalid) {
      return "";
    }
    return "";
  }
}
