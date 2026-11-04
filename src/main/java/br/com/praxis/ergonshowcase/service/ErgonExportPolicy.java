package br.com.praxis.ergonshowcase.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.praxisplatform.uischema.exporting.*;

/** Host limits and formats; selection, governance, formatting and execution belong to the starter. */
final class ErgonExportPolicy {
  private ErgonExportPolicy() { }
  static Optional<CollectionExportCapability> capability(boolean configured) {
    return configured ? Optional.of(new CollectionExportCapability(
        List.of(CollectionExportFormat.CSV,CollectionExportFormat.JSON,CollectionExportFormat.EXCEL),
        List.of(CollectionExportScope.values()),Map.of("csv",1000,"json",1000,"excel",1000),false)) : Optional.empty();
  }
}
