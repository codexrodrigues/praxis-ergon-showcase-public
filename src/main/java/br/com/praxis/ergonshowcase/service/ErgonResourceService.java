package br.com.praxis.ergonshowcase.service;

import br.com.praxis.ergonshowcase.config.TenantContext;
import java.util.List;
import org.praxisplatform.uischema.exporting.*;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;
import org.praxisplatform.uischema.repository.base.BaseCrudRepository;
import org.praxisplatform.uischema.service.base.AbstractBaseResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Políticas do host; consulta, versão, snapshots e exportação são executados pelo starter. */
public abstract class ErgonResourceService<E, R, I, F extends GenericFilterDTO, C, U> extends AbstractBaseResourceService<E, R, I, F, C, U> {
  private final List<String> identityPaths;
  @Autowired(required = false) protected ErgonReadFacts readFacts;
  @Autowired private TenantContext resourceTenant = new TenantContext();
  @Autowired protected com.fasterxml.jackson.databind.ObjectMapper resourceObjectMapper;

  protected ErgonResourceService(BaseCrudRepository<E, I> repository, Class<E> type, String... identityPaths) {
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

  // As listas editoriais existentes permanecem como política de exportação do host.
  protected abstract F emptyFilter();
  protected static CollectionExportField field(String key, String label, String type) {
    return new CollectionExportField(key, label, true, true, type, key);
  }
  protected static CollectionExportField presentedField(String key, String label, String type,
      CollectionExportFieldPresentation presentation) {
    return new CollectionExportField(key, label, true, true, type, key, null, presentation);
  }
  /** Detached scalar state; mapped associations are not traversed or shared with a candidate. */
  @Override protected E snapshotBeforeMutation(E entity) {
    entity = unproxyEntity(entity);
    E copy = org.springframework.beans.BeanUtils.instantiateClass(getEntityClass());
    for (Class<?> type = getEntityClass(); type != null && type != Object.class; type = type.getSuperclass()) {
      for (var field : type.getDeclaredFields()) {
        if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) || field.isSynthetic()
            || field.isAnnotationPresent(jakarta.persistence.ManyToOne.class)
            || field.isAnnotationPresent(jakarta.persistence.OneToMany.class)
            || field.isAnnotationPresent(jakarta.persistence.OneToOne.class)
            || field.isAnnotationPresent(jakarta.persistence.ManyToMany.class)) continue;
        try {
          field.setAccessible(true);
          Object value = field.get(entity);
          if (value instanceof java.util.Map<?, ?> || value instanceof java.util.Collection<?>
              || value != null && field.isAnnotationPresent(jakarta.persistence.Embedded.class) && !value.getClass().isRecord()) {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules();
            value = mapper.convertValue(org.praxisplatform.rules.facts.PraxisFactTreeBuilder.toJsonNode(value),
                mapper.getTypeFactory().constructType(field.getGenericType()));
          }
          field.set(copy, value);
        } catch (IllegalAccessException ex) { throw new IllegalStateException("Não foi possível capturar o estado escalar.", ex); }
      }
    }
    return copy;
  }

  /** Internal bulk domain bridge; the ordinary lock, scope, version and snapshot boundary is reused. */
  @org.springframework.transaction.annotation.Transactional
  public void executeBulkDomainCommand(I id,
      org.praxisplatform.uischema.concurrency.ResourceVersionUpdatePrecondition<I> precondition,
      java.util.function.Consumer<E> mutation) {
    executeVersionedMutation(id, precondition, mutation);
  }

  /** Response-only previews are not inputs to the full update candidate. */
  @Override protected U toBulkUpdate(E entity, com.fasterxml.jackson.databind.ObjectMapper mapper,
      com.fasterxml.jackson.databind.JavaType updateType) {
    var response = mapper.<com.fasterxml.jackson.databind.node.ObjectNode>valueToTree(getResourceMapper().toResponse(entity));
    var input = mapper.createObjectNode();
    for (var property : mapper.getDeserializationConfig().introspect(updateType).findProperties()) {
      if (property.couldDeserialize() && response.has(property.getName())) input.set(property.getName(), response.get(property.getName()).deepCopy());
    }
    return mapper.convertValue(input, updateType);
  }

  /** Unavailable state is hidden from discovery; ordinary item reads retain their 403/404 contract. */
  @Override @org.springframework.transaction.annotation.Transactional(readOnly = true)
  public java.util.Optional<org.praxisplatform.uischema.capability.ResourceStateSnapshot> findResourceStateSnapshot(I id) {
    try { return super.findResourceStateSnapshot(id); }
    catch (jakarta.persistence.EntityNotFoundException absent) { return java.util.Optional.empty(); }
    catch (ResponseStatusException denied) {
      if (denied.getStatusCode().value() == 403 || denied.getStatusCode().value() == 404) return java.util.Optional.empty();
      throw denied;
    }
  }

  /** Bulk always freezes the current organization, including the default tenant when the header is omitted. */
  @Override protected org.praxisplatform.uischema.service.base.ResourceFilterAccessScope<E> resolveBulkResourceFilterAccessScope() {
    return resolveResourceFilterAccessScope();
  }
  @Override protected final org.praxisplatform.uischema.service.base.ResourceFilterAccessScope<E> resolveResourceFilterAccessScope() {
    return ErgonTenantAccess.scope(getEntityClass(), resourceTenant, globalOrganizationRecords());
  }
  protected boolean globalOrganizationRecords() { return false; }

  @Override public java.util.Optional<CollectionExportCapability> getCollectionExportCapability() {
    return ErgonExportPolicy.capability(supportsCollectionExport());
  }

}
