package br.com.praxis.ergonshowcase.config;

import java.util.Optional;
import java.util.OptionalInt;
import org.praxisplatform.uischema.customfields.ResourceTenantContextResolver;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.*;
import org.springframework.web.server.ResponseStatusException;

/** Órgão usado na projeção do catálogo customizável; não representa autorização multi-tenant. */
@Component
public class TenantContext implements ResourceTenantContextResolver {
  public Optional<String> resolveTenant() {
    var attrs = RequestContextHolder.getRequestAttributes();
    String value =
        attrs instanceof ServletRequestAttributes a ? a.getRequest().getHeader("X-Tenant") : null;
    if (value == null || value.isBlank()) return Optional.of("1");
    if (!value.matches("[1-9][0-9]{0,8}"))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Órgão inválido em X-Tenant.");
    return Optional.of(value);
  }

  /** Retorna o código do órgão apenas quando o cabeçalho {@code X-Tenant} foi explicitamente enviado. */
  public OptionalInt explicitOrgao() {
    var attrs = RequestContextHolder.getRequestAttributes();
    String value =
        attrs instanceof ServletRequestAttributes a ? a.getRequest().getHeader("X-Tenant") : null;
    if (value == null || value.isBlank()) return OptionalInt.empty();
    if (!value.matches("[1-9][0-9]{0,8}"))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Órgão inválido em X-Tenant.");
    return OptionalInt.of(Integer.parseInt(value));
  }

  public int orgao() {
    return Integer.parseInt(resolveTenant().orElse("1"));
  }
}
