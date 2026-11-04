package br.com.praxis.ergonshowcase.config;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import java.util.Map;
import java.util.Set;
import org.praxisplatform.uischema.capability.*;
import org.springframework.stereotype.Component;

/** Discovery e execução permanecem fechados para comandos ainda sem regras transacionais. */
@Component
public class OrganogramaPhaseOneAvailability implements ResourceOperationAvailabilityProvider {
  public static final Set<String> RESOURCES = Set.of(ApiPaths.KEY_HISTORICO_SETORES,
      ApiPaths.KEY_SETORES_ERGON, ApiPaths.KEY_CARGOS_POR_SETOR,
      ApiPaths.KEY_SUBEMPRESAS_LOCALIDADES, ApiPaths.KEY_GRUPOS_SETORES);

  @Override public AvailabilityDecision evaluate(ResourceOperationAvailabilityContext context) {
    if (!RESOURCES.contains(context.resourceKey())) return AvailabilityDecision.allowAll();
    if (Set.of("create", "update", "delete", "delete-all", "duplicate-draft", "bulk-update")
        .contains(context.operationId()))
      return AvailabilityDecision.deny("organograma-comandos-ainda-indisponiveis", Map.of());
    return AvailabilityDecision.allowAll();
  }
}
