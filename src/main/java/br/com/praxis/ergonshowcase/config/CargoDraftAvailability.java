package br.com.praxis.ergonshowcase.config;

import br.com.praxis.ergonshowcase.repository.CargoRepository;
import java.util.Map;
import org.praxisplatform.uischema.capability.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Disponibilidade do preparo e seus pré-requisitos; a execução revalida a origem no serviço. */
@Component
public class CargoDraftAvailability implements ResourceOperationAvailabilityProvider {
  private final CargoRepository cargos;
  private final TenantContext tenant;

  public CargoDraftAvailability(CargoRepository cargos, TenantContext tenant) {
    this.cargos = cargos;
    this.tenant = tenant;
  }

  @Override
  public AvailabilityDecision evaluate(ResourceOperationAvailabilityContext context) {
    // As demais operações conservam a política padrão de discovery do showcase;
    // filtros HTTP, method security e invariantes continuam sendo enforcement.
    if (!"administracao-pessoal.cargos".equals(context.resourceKey()))
      return AvailabilityDecision.allowAll();
    String operation = context.operationId();
    if (!java.util.Set.of("create", "view", "duplicate-draft").contains(operation))
      return AvailabilityDecision.allowAll();
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    var authorities = authentication == null ? java.util.Set.<String>of()
        : authentication.getAuthorities().stream().map(a -> a.getAuthority())
            .collect(java.util.stream.Collectors.toSet());
    boolean allowed = authentication != null && authentication.isAuthenticated();
    if ("create".equals(operation)) {
      allowed &= authorities.contains("ERGON_WRITE");
    } else {
      allowed &= authorities.contains("ERGON_READ");
      if ("duplicate-draft".equals(operation)) allowed &= authorities.contains("ERGON_WRITE");
      if (allowed) {
        try {
          allowed = context.resourceId() != null && cargos.existsByCargoAndEmpCodigo(
              Integer.valueOf(context.resourceId().toString()), tenant.orgao());
        } catch (IllegalArgumentException ex) {
          allowed = false;
        }
      }
    }
    if (allowed && "duplicate-draft".equals(operation))
      allowed = context.resourceState() != null
          && "VIGENTE".equals(context.resourceState().state());
    return allowed ? AvailabilityDecision.allowAll()
        : AvailabilityDecision.deny("duplicate-draft-unavailable", Map.of());
  }
}
