package br.com.praxis.ergonshowcase.service;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import org.praxisplatform.uischema.concurrency.ResourceRepresentationResult;
import org.praxisplatform.uischema.customfields.CustomFieldDefinitionProvider;
import org.praxisplatform.uischema.customfields.ResourceTenantContextResolver;
import org.praxisplatform.uischema.dto.CursorPage;
import org.praxisplatform.uischema.exporting.CollectionExportCapability;
import org.praxisplatform.uischema.exporting.CollectionExportExecutor;
import org.praxisplatform.uischema.exporting.CollectionExportField;
import org.praxisplatform.uischema.exporting.CollectionExportFieldPresentation;
import org.praxisplatform.uischema.exporting.CollectionExportFormat;
import org.praxisplatform.uischema.exporting.CollectionExportRequest;
import org.praxisplatform.uischema.exporting.CollectionExportResult;
import org.praxisplatform.uischema.exporting.CollectionExportScope;
import org.praxisplatform.uischema.exporting.CollectionExportSelection;
import org.praxisplatform.uischema.exporting.CollectionExportValueResolver;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;
import org.praxisplatform.uischema.repository.base.BaseCrudRepository;
import org.praxisplatform.uischema.service.base.AbstractReadOnlyResourceService;
import org.praxisplatform.uischema.util.CursorEncoder;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Base corporativa estritamente de leitura para catálogos e referências do Ergon Showcase,
 * provendo paginação por cursor determinístico (keyset), localização por identidade, exportação
 * governada nos 5 escopos ({@code AUTO}, {@code SELECTED}, {@code FILTERED}, {@code CURRENT_PAGE},
 * {@code ALL}) e enriquecimento em lote via {@link ErgonReadFacts}.
 */
public abstract class ErgonReadOnlyResourceService<E, R, I, F extends GenericFilterDTO>
    extends AbstractReadOnlyResourceService<E, R, I, F> {

  private final List<String> identityPaths;

  @Autowired(required = false)
  private CustomFieldDefinitionProvider definitions;

  @Autowired(required = false)
  private ResourceTenantContextResolver tenantContext;

  @Autowired(required = false)
  private CollectionExportExecutor exports;

  @Autowired(required = false)
  protected ErgonReadFacts readFacts;

  protected ErgonReadOnlyResourceService(
      BaseCrudRepository<E, I> repository, Class<E> type, String... identityPaths) {
    super(repository, type);
    this.identityPaths = List.of(identityPaths);
  }

  @Override
  public Sort getDefaultSort() {
    return Sort.by(identityPaths.toArray(String[]::new));
  }

  @Override
  public boolean supportsCursorPagination() {
    return true;
  }

  protected abstract List<CollectionExportField> exportFields();

  protected abstract F emptyFilter();

  protected static CollectionExportField field(String key, String label, String type) {
    return new CollectionExportField(key, label, true, true, type, key);
  }

  protected static CollectionExportField presentedField(
      String key, String label, String type, CollectionExportFieldPresentation presentation) {
    return new CollectionExportField(key, label, true, true, type, key, null, presentation);
  }

  protected Specification<E> filtered(F filter) {
    return resolveResourceFilterAccessScope()
        .specification()
        .and(
            getSpecificationsBuilder()
                .buildSpecification(
                    filter, Pageable.unpaged(), definitions, tenantContext, resolveResourceKey())
                .spec());
  }

  private Sort identitySort(Sort sort) {
    if (sort == null || sort.isUnsorted()) return getDefaultSort();
    var orders = sort.toList();
    if (orders.size() == 1 && orders.getFirst().getProperty().equals("id")) {
      return Sort.by(orders.getFirst().getDirection(), identityPaths.toArray(String[]::new));
    }
    if (!orders.stream().map(Sort.Order::getProperty).toList().equals(identityPaths)
        || orders.stream().anyMatch(o -> o.getDirection() != orders.getFirst().getDirection())) {
      throw bad("Cursor e localização exigem ordenação pela identidade.");
    }
    return sort;
  }

  private Path<?> path(Root<E> root, String name) {
    Path<?> p = root;
    for (String part : name.split("\\.")) p = p.get(part);
    return p;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private Specification<E> boundary(List<String> values, boolean greater) {
    return (root, q, cb) -> {
      List<Predicate> alternatives = new ArrayList<>(), equal = new ArrayList<>();
      for (int i = 0; i < identityPaths.size(); i++) {
        Path p = path(root, identityPaths.get(i));
        Comparable value =
            p.getJavaType() == Long.class
                ? Long.valueOf(values.get(i))
                : p.getJavaType() == Integer.class ? Integer.valueOf(values.get(i)) : values.get(i);
        List<Predicate> branch = new ArrayList<>(equal);
        branch.add(greater ? cb.greaterThan(p, value) : cb.lessThan(p, value));
        alternatives.add(cb.and(branch.toArray(Predicate[]::new)));
        equal.add(cb.equal(p, value));
      }
      return cb.or(alternatives.toArray(Predicate[]::new));
    };
  }

  private List<String> values(E e) {
    var bean = new BeanWrapperImpl(e);
    return identityPaths.stream().map(p -> Objects.toString(bean.getPropertyValue(p))).toList();
  }

  private String token(E e) {
    return CursorEncoder.BASE64_URL.encode(String.join(":", values(e)));
  }

  @Override
  protected CursorPage<E> filterEntitiesByCursor(
      F filter, Sort requested, String after, String before, int size) {
    if (size < 1 || size > 200 || after != null && before != null) {
      throw bad("Tamanho inválido ou cursores conflitantes.");
    }
    Sort sort = identitySort(requested);
    boolean backwards = before != null;
    var spec = filtered(filter);
    String cursor = backwards ? before : after;
    if (cursor != null) {
      try {
        List<String> values = List.of(CursorEncoder.BASE64_URL.decode(cursor).split(":"));
        if (values.size() != identityPaths.size() || values.stream().anyMatch(String::isBlank)) {
          throw bad("Cursor incompatível com a identidade.");
        }
        spec = spec.and(boundary(values, sort.toList().getFirst().isAscending() != backwards));
      } catch (IllegalArgumentException e) {
        throw bad("Cursor inválido.");
      }
    }
    Sort querySort = backwards ? Sort.by(sort.stream().map(Sort.Order::reverse).toList()) : sort;
    List<E> rows;
    try {
      rows =
          new ArrayList<>(
              getRepository().findAll(spec, PageRequest.of(0, size + 1, querySort)).getContent());
    } catch (IllegalArgumentException e) {
      throw bad("Cursor inválido.");
    }
    boolean more = rows.size() > size;
    if (more) rows.removeLast();
    if (backwards) Collections.reverse(rows);
    return new CursorPage<>(
        rows,
        rows.isEmpty()
            ? null
            : (!backwards && more || backwards && cursor != null ? token(rows.getLast()) : null),
        rows.isEmpty()
            ? null
            : (backwards && more || !backwards && cursor != null ? token(rows.getFirst()) : null),
        size);
  }

  @Override
  @Transactional(readOnly = true)
  public OptionalLong locate(F filter, Sort requested, I id) {
    Sort sort = identitySort(requested);
    var entity = getRepository().findById(id);
    if (entity.isEmpty()) return OptionalLong.empty();
    var target = values(entity.get());
    Specification<E> match =
        (root, q, cb) ->
            cb.and(
                java.util.stream.IntStream.range(0, identityPaths.size())
                    .mapToObj(
                        i -> {
                          var p = path(root, identityPaths.get(i));
                          Object val =
                              p.getJavaType() == Long.class
                                  ? Long.valueOf(target.get(i))
                                  : p.getJavaType() == Integer.class
                                      ? Integer.valueOf(target.get(i))
                                      : target.get(i);
                          return cb.equal(p, val);
                        })
                    .toArray(Predicate[]::new));
    if (!getRepository().exists(filtered(filter).and(match))) return OptionalLong.empty();
    return OptionalLong.of(
        getRepository()
            .count(
                filtered(filter).and(boundary(target, sort.toList().getFirst().isDescending()))));
  }

  @Override
  public boolean supportsCollectionExport() {
    return true;
  }

  @Override
  public Optional<CollectionExportCapability> getCollectionExportCapability() {
    return Optional.of(
        new CollectionExportCapability(
            List.of(
                CollectionExportFormat.CSV,
                CollectionExportFormat.JSON,
                CollectionExportFormat.EXCEL),
            List.of(
                CollectionExportScope.AUTO,
                CollectionExportScope.SELECTED,
                CollectionExportScope.FILTERED,
                CollectionExportScope.CURRENT_PAGE,
                CollectionExportScope.ALL),
            Map.of("csv", 1000, "json", 1000, "excel", 1000),
            false));
  }

  @Override
  @Transactional(readOnly = true)
  public CollectionExportResult exportCollection(CollectionExportRequest<F> request) {
    if (!getCollectionExportCapability().orElseThrow().formats().contains(request.format())) {
      throw bad("Formato não suportado.");
    }
    int limit = request.maxRows() == null ? 1000 : Math.min(request.maxRows(), 1000);
    if (limit < 1) throw bad("Limite de linhas inválido.");
    CollectionExportScope requestedScope =
        request.scope() != null ? request.scope() : CollectionExportScope.AUTO;
    boolean hasSelection =
        request.selection() != null
            && request.selection().selectedKeys() != null
            && !request.selection().selectedKeys().isEmpty();
    CollectionExportScope effectiveScope =
        requestedScope == CollectionExportScope.AUTO
            ? (hasSelection
                ? CollectionExportScope.SELECTED
                : (request.pagination() != null
                    ? CollectionExportScope.CURRENT_PAGE
                    : CollectionExportScope.FILTERED))
            : requestedScope;
    F effectiveFilter =
        effectiveScope == CollectionExportScope.ALL || request.filters() == null
            ? emptyFilter()
            : request.filters();
    Sort exportSort = resolveExportSort(request.sort());
    List<R> slice;
    boolean truncated;
    if (effectiveScope == CollectionExportScope.CURRENT_PAGE && request.pagination() != null) {
      int pageNum =
          request.pagination().pageIndex() != null
              ? Math.max(0, request.pagination().pageIndex())
              : (request.pagination().pageNumber() != null
                  ? Math.max(0, request.pagination().pageNumber())
                  : 0);
      int pageSize =
          request.pagination().pageSize() != null && request.pagination().pageSize() > 0
              ? Math.min(request.pagination().pageSize(), limit)
              : limit;
      var pageResult = filter(effectiveFilter, PageRequest.of(pageNum, pageSize, exportSort), null);
      slice = pageResult.getContent();
      truncated = pageResult.hasNext() && slice.size() >= limit;
    } else if (effectiveScope == CollectionExportScope.SELECTED && hasSelection) {
      var entities =
          getRepository()
              .findAll(
                  filtered(effectiveFilter).and(selectionSpecification(request.selection())),
                  PageRequest.of(0, limit + 1, exportSort))
              .getContent();
      var rows = new ArrayList<>(entities.stream().map(getResourceMapper()::toResponse).toList());
      if (readFacts != null) {
        readFacts.enrich(rows);
      }
      Set<String> selectedIds =
          request.selection().selectedKeys().stream()
              .map(String::valueOf)
              .collect(java.util.stream.Collectors.toSet());
      String keyProp =
          request.selection().keyField() != null && !request.selection().keyField().isBlank()
              ? request.selection().keyField().trim()
              : "id";
      List<R> matched =
          rows.stream()
              .filter(
                  row -> {
                    var bw = new BeanWrapperImpl(row);
                    Object keyVal =
                        bw.isReadableProperty(keyProp)
                            ? bw.getPropertyValue(keyProp)
                            : (bw.isReadableProperty("id") ? bw.getPropertyValue("id") : null);
                    return keyVal != null && selectedIds.contains(String.valueOf(keyVal));
                  })
              .toList();
      truncated = matched.size() > limit;
      slice = matched.subList(0, Math.min(matched.size(), limit));
    } else {
      var rows =
          filter(effectiveFilter, PageRequest.of(0, limit + 1, exportSort), null).getContent();
      truncated = rows.size() > limit;
      slice = rows.subList(0, Math.min(rows.size(), limit));
    }
    String scopeName = effectiveScope.name().toLowerCase(Locale.ROOT);
    return exports.export(
        request,
        slice,
        exportFields(),
        CollectionExportValueResolver.reflective(),
        Map.of("effectiveMaxRows", limit, "truncated", truncated, "scope", scopeName));
  }

  protected Sort resolveExportSort(Object rawSort) {
    if (rawSort instanceof Sort s && s.isSorted()) {
      return s;
    }
    if (rawSort instanceof String str && !str.isBlank()) {
      String[] parts = str.trim().split(",");
      String prop = parts[0].trim();
      Sort.Direction dir =
          parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim())
              ? Sort.Direction.DESC
              : Sort.Direction.ASC;
      if ("id".equals(prop)) {
        return Sort.by(dir, identityPaths.toArray(String[]::new));
      }
      return Sort.by(dir, prop);
    }
    if (rawSort instanceof Map<?, ?> map) {
      Object propObj =
          map.containsKey("property")
              ? map.get("property")
              : (map.containsKey("field") ? map.get("field") : map.get("column"));
      if (propObj instanceof String prop && !prop.isBlank()) {
        Object dirObj = map.containsKey("direction") ? map.get("direction") : map.get("order");
        Sort.Direction dir =
            dirObj != null && "desc".equalsIgnoreCase(dirObj.toString().trim())
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        if ("id".equals(prop.trim())) {
          return Sort.by(dir, identityPaths.toArray(String[]::new));
        }
        return Sort.by(dir, prop.trim());
      }
    }
    if (rawSort instanceof Collection<?> coll && !coll.isEmpty()) {
      Sort combined = Sort.unsorted();
      for (Object item : coll) {
        Sort next = resolveExportSort(item);
        if (next != null && next.isSorted()) {
          combined = combined.and(next);
        }
      }
      if (combined.isSorted()) {
        return combined;
      }
    }
    return getDefaultSort();
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  protected Specification<E> selectionSpecification(CollectionExportSelection selection) {
    if (selection == null
        || selection.selectedKeys() == null
        || selection.selectedKeys().isEmpty()) {
      return (root, q, cb) -> cb.conjunction();
    }
    List<String> rawKeys =
        selection.selectedKeys().stream()
            .filter(Objects::nonNull)
            .map(String::valueOf)
            .filter(s -> !s.isBlank())
            .toList();
    if (rawKeys.isEmpty()) {
      return (root, q, cb) -> cb.conjunction();
    }
    return (root, q, cb) -> {
      try {
        if (identityPaths.size() == 1) {
          Path p = path(root, identityPaths.getFirst());
          CriteriaBuilder.In<Object> in = cb.in(p);
          for (String raw : rawKeys) {
            Object val =
                p.getJavaType() == Long.class
                    ? Long.valueOf(raw)
                    : p.getJavaType() == Integer.class ? Integer.valueOf(raw) : raw;
            in.value(val);
          }
          return in;
        }
        List<Predicate> ors = new ArrayList<>();
        for (String raw : rawKeys) {
          String[] parts = raw.split(":");
          if (parts.length != identityPaths.size()) continue;
          List<Predicate> ands = new ArrayList<>();
          for (int i = 0; i < identityPaths.size(); i++) {
            Path p = path(root, identityPaths.get(i));
            Object val =
                p.getJavaType() == Long.class
                    ? Long.valueOf(parts[i])
                    : p.getJavaType() == Integer.class ? Integer.valueOf(parts[i]) : parts[i];
            ands.add(cb.equal(p, val));
          }
          ors.add(cb.and(ands.toArray(Predicate[]::new)));
        }
        return ors.isEmpty() ? cb.conjunction() : cb.or(ors.toArray(Predicate[]::new));
      } catch (RuntimeException ex) {
        return cb.conjunction();
      }
    };
  }

  protected ResourceRepresentationResult<R> enriched(ResourceRepresentationResult<R> result) {
    if (readFacts != null) {
      readFacts.enrichOne(result.body());
    }
    return result;
  }

  @Override
  @Transactional(readOnly = true)
  public ResourceRepresentationResult<R> findById(I id) {
    return enriched(super.findById(id));
  }

  @Override
  @Transactional(readOnly = true)
  public List<R> findAll() {
    var result = super.findAll();
    if (readFacts != null) {
      readFacts.enrich(result);
    }
    return result;
  }

  @Override
  @Transactional(readOnly = true)
  public List<R> findAllById(Collection<I> ids) {
    var result = super.findAllById(ids);
    if (readFacts != null) {
      readFacts.enrich(result);
    }
    return result;
  }

  @Override
  @Transactional(readOnly = true)
  public Page<R> filter(F filter, Pageable pageable, Collection<I> includeIds) {
    var result = super.filter(filter, pageable, includeIds);
    if (readFacts != null) {
      readFacts.enrich(result.getContent());
    }
    return result;
  }

  @Override
  @Transactional(readOnly = true)
  public CursorPage<R> filterByCursor(F filter, Sort sort, String after, String before, int size) {
    var result = super.filterByCursor(filter, sort, after, before, size);
    if (readFacts != null) {
      readFacts.enrich(result.content());
    }
    return result;
  }

  private static ResponseStatusException bad(String message) {
    return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, message);
  }
}
