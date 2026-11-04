package br.com.praxis.ergonshowcase.policystudio;

import static br.com.praxis.ergonshowcase.policystudio.PolicyStudioDtos.Organization;

import java.util.List;
import java.util.TreeSet;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

/** Autoridades por órgão emitidas pela identidade autenticada, sem depender de cabeçalhos do cliente. */
public final class PolicyStudioAccess {
  private static final String READ = "ERGON_POLICY_READ:";
  private static final String PUBLISH = "ERGON_POLICY_PUBLISH:";

  private PolicyStudioAccess() {}

  public static List<String> authorities(String readOrganizations, String publishOrganizations) {
    var result = new TreeSet<String>();
    add(result, READ, readOrganizations);
    add(result, READ, publishOrganizations);
    add(result, PUBLISH, publishOrganizations);
    return List.copyOf(result);
  }

  private static void add(TreeSet<String> authorities, String prefix, String organizations) {
    if (organizations.isBlank()) return;
    for (String token : organizations.split(",", -1)) {
      String code = token.trim();
      if (!code.matches("[1-9][0-9]{0,8}")) {
        throw new IllegalArgumentException("Código de órgão inválido nas autoridades do Policy Studio.");
      }
      authorities.add(prefix + code);
    }
  }

  static List<Organization> organizations() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) return List.of();
    return authentication.getAuthorities().stream().map(authority -> authority.getAuthority())
        .filter(name -> name.startsWith(READ)).map(name -> name.substring(READ.length()))
        .filter(code -> code.matches("[1-9][0-9]{0,8}")).map(Integer::parseInt).distinct().sorted()
        .map(code -> new Organization(code, has(PUBLISH, code) && has("ERGON_WRITE", null))).toList();
  }

  static void requireRead(int organization) {
    if (!has(READ, organization)) throw denied();
  }

  static void requirePublish(int organization) {
    requireRead(organization);
    if (!has(PUBLISH, organization)) throw denied();
  }

  private static boolean has(String prefix, Integer organization) {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    String authority = organization == null ? prefix : prefix + organization;
    return authentication != null && authentication.isAuthenticated()
        && authentication.getAuthorities().stream().anyMatch(grant -> authority.equals(grant.getAuthority()));
  }

  private static ResponseStatusException denied() {
    return new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso ao órgão indisponível no Policy Studio.");
  }
}
