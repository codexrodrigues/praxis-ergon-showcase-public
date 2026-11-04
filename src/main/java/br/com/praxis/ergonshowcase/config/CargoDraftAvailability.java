package br.com.praxis.ergonshowcase.config;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.repository.CargoRepository;
import java.util.Map;
import org.praxisplatform.uischema.capability.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Disponibilidade do preparo e seus pré-requisitos; a execução revalida a origem no serviço. */
@Component
public class CargoDraftAvailability implements ResourceOperationAvailabilityProvider {
  private static final java.util.Set<String> DRAFT_WITH_EXPLICIT_ELIGIBILITY = java.util.Set.of(
      ApiPaths.KEY_VENDAS_LICENCA_ESPECIAL, ApiPaths.KEY_FUNCOES, ApiPaths.KEY_DESIGNACOES, ApiPaths.KEY_SUBSTITUICOES);
  private final CargoRepository cargos;
  private final TenantContext tenant;

  public CargoDraftAvailability(CargoRepository cargos, TenantContext tenant) {
    this.cargos = cargos;
    this.tenant = tenant;
  }

  @Override
  public AvailabilityDecision evaluate(ResourceOperationAvailabilityContext context) {
    if(java.util.Set.of(br.com.praxis.ergonshowcase.constants.ApiPaths.KEY_AVERBACOES_TEMPO_SERVICO,
        br.com.praxis.ergonshowcase.constants.ApiPaths.KEY_PERIODOS_AQUISITIVOS_LICENCA_ESPECIAL,
        br.com.praxis.ergonshowcase.constants.ApiPaths.KEY_LICENCAS_ESPECIAIS_GOZO).contains(context.resourceKey())
        || DRAFT_WITH_EXPLICIT_ELIGIBILITY.contains(context.resourceKey())) return draftFromSnapshot(context);
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
  private AvailabilityDecision draftFromSnapshot(ResourceOperationAvailabilityContext context) {
    String op=context.operationId();
    if(!java.util.Set.of("view","create","duplicate-draft").contains(op)) return AvailabilityDecision.allowAll();
    var auth=SecurityContextHolder.getContext().getAuthentication();
    var roles=auth==null ? java.util.Set.<String>of() : auth.getAuthorities().stream().map(a->a.getAuthority()).collect(java.util.stream.Collectors.toSet());
    boolean allowed=auth!=null && auth.isAuthenticated() && roles.contains(op.equals("view")?"ERGON_READ":"ERGON_WRITE");
    if(!op.equals("create")) {
      var state = context.resourceState();
      allowed &= roles.contains("ERGON_READ") && state != null;
      if (allowed && op.equals("duplicate-draft")) {
        allowed = DRAFT_WITH_EXPLICIT_ELIGIBILITY.contains(context.resourceKey())
            ? Boolean.TRUE.equals(state.metadata().get("duplicateDraftEligible"))
            : state.state() != null && !java.util.Set.of("CANCELADO", "ESTORNADA", "PRESCRITO").contains(state.state());
      }
    }
    return allowed ? AvailabilityDecision.allowAll() : AvailabilityDecision.deny("duplicate-draft-unavailable",Map.of());
  }
}
