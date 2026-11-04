package br.com.praxis.ergonshowcase.service;

import br.com.praxis.ergonshowcase.config.TenantContext;
import java.util.List;
import org.praxisplatform.uischema.exporting.*;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;
import org.praxisplatform.uischema.repository.base.BaseCrudRepository;
import org.praxisplatform.uischema.service.base.AbstractReadOnlyResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;

/** Políticas do host; consulta, versão, snapshots e exportação são executados pelo starter. */
public abstract class ErgonReadOnlyResourceService<E, R, I, F extends GenericFilterDTO> extends AbstractReadOnlyResourceService<E, R, I, F> {
  private final List<String> identityPaths;
  @Autowired(required = false) protected ErgonReadFacts readFacts;
  @Autowired private TenantContext resourceTenant = new TenantContext();

  protected ErgonReadOnlyResourceService(BaseCrudRepository<E, I> repository, Class<E> type, String... identityPaths) {
    super(repository, type);
    this.identityPaths = List.of(identityPaths);
  }

  @Override protected List<String> getIdentityPaths() { return identityPaths; }
  @Override public Sort getDefaultSort() { return Sort.by(identityPaths.toArray(String[]::new)); }
  @Override public boolean supportsCursorPagination() { return true; }
  @Override protected int maxExportRows(CollectionExportFormat format) { return 1000; }

  @Override protected void enrichResponses(List<R> responses) {
    if (readFacts != null) readFacts.enrich(responses);
  }

  @Override protected void assertEntityAccessAllowed(E entity) {
    ErgonTenantAccess.assertAllowed(entity, resourceTenant, getEntityManager(), globalOrganizationRecords());
  }

  @Override protected void assertEntitiesAccessAllowed(java.util.Collection<E> entities) {
    ErgonTenantAccess.assertAllAllowed(entities, resourceTenant, getEntityManager(), globalOrganizationRecords());
  }

  @Override protected final org.praxisplatform.uischema.service.base.ResourceFilterAccessScope<E> resolveResourceFilterAccessScope() {
    return ErgonTenantAccess.scope(getEntityClass(), resourceTenant, globalOrganizationRecords());
  }
  protected boolean globalOrganizationRecords() { return false; }

  // As listas editoriais existentes permanecem como política de exportação do host.
  protected abstract F emptyFilter();
  protected static CollectionExportField field(String key, String label, String type) {
    return new CollectionExportField(key, label, true, true, type, key);
  }
  protected static CollectionExportField presentedField(String key, String label, String type,
      CollectionExportFieldPresentation presentation) {
    return new CollectionExportField(key, label, true, true, type, key, null, presentation);
  }
  @Override public java.util.Optional<CollectionExportCapability> getCollectionExportCapability() {
    return ErgonExportPolicy.capability(supportsCollectionExport());
  }

}
