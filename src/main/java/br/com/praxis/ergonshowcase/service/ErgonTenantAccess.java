package br.com.praxis.ergonshowcase.service;

import br.com.praxis.ergonshowcase.config.TenantContext;
import br.com.praxis.ergonshowcase.domain.Dependente;
import br.com.praxis.ergonshowcase.domain.Vinculo;
import jakarta.persistence.EntityManager;
import java.util.Objects;
import org.praxisplatform.uischema.service.base.ResourceFilterAccessScope;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Uma política de órgão para leituras, seleção de lotes e acesso individual do host. */
public final class ErgonTenantAccess {
  private static final java.util.List<String> ACTIVE_MEMBERSHIP = java.util.List.of("ATIVO", "AFASTADO", "CEDIDO");
  private static final String ACTIVE_MEMBERSHIP_JPQL = " and v.situacao in ('ATIVO','AFASTADO','CEDIDO')";
  private ErgonTenantAccess() { }

  /** Preloads memberships per bounded collection; no authorization survives this call or tenant. */
  public static void assertAllAllowed(java.util.Collection<?> entities, TenantContext tenant, EntityManager em, boolean allowGlobal) {
    var people = new java.util.LinkedHashSet<Long>();
    for (Object entity : entities) {
      if (entity instanceof Dependente dependent) {
        if (dependent.getId() == null || dependent.getId().numfunc() == null || em == null) throw denied();
        people.add(dependent.getId().numfunc());
      } else assertAllowed(entity, tenant, em, allowGlobal);
    }
    var ids = new java.util.ArrayList<>(people);
    for (int start = 0; start < ids.size(); start += 1000) {
      var batch = ids.subList(start, Math.min(start + 1000, ids.size()));
      var allowed = em.createQuery("select distinct v.id.numfunc from Vinculo v where v.id.numfunc in :people and v.empCodigo=:org" + ACTIVE_MEMBERSHIP_JPQL, Long.class)
          .setParameter("people", batch).setParameter("org", tenant.orgao()).getResultList();
      if (!new java.util.HashSet<>(allowed).containsAll(batch)) throw denied();
    }
  }

  public static void assertAllowed(Object entity, TenantContext tenant, EntityManager em, boolean allowGlobal) {
    if (entity instanceof Dependente dependent) {
      Long person = dependent.getId() == null ? null : dependent.getId().numfunc();
      if (person == null || em == null || em.createQuery(
          "select count(v) from Vinculo v where v.id.numfunc=:person and v.empCodigo=:org" + ACTIVE_MEMBERSHIP_JPQL, Long.class)
          .setParameter("person", person).setParameter("org", tenant.orgao()).getSingleResult() == 0)
        throw denied();
      return;
    }
    String property = organizationProperty(entity.getClass());
    if (property == null) return;
    Object organization = new BeanWrapperImpl(entity).getPropertyValue(property);
    if (!(organization == null && allowGlobal) && !Objects.equals(organization, tenant.orgao()))
      throw denied();
  }

  public static <E> ResourceFilterAccessScope<E> scope(Class<E> type, TenantContext tenant, boolean allowGlobal) {
    int organization = tenant.orgao();
    if (Dependente.class.isAssignableFrom(type)) {
      return ResourceFilterAccessScope.restricted((root, query, cb) -> {
        var membership = query.subquery(Integer.class);
        var vinculo = membership.from(Vinculo.class);
        membership.select(cb.literal(1)).where(
            cb.equal(vinculo.get("id").get("numfunc"), root.get("id").get("numfunc")),
            cb.equal(vinculo.get("empCodigo"), organization),
            vinculo.get("situacao").in(ACTIVE_MEMBERSHIP));
        return cb.exists(membership);
      });
    }
    String property = organizationProperty(type);
    if (property == null) return ResourceFilterAccessScope.unrestricted();
    return ResourceFilterAccessScope.restricted((root, query, cb) -> allowGlobal
        ? cb.or(cb.isNull(root.get(property)), cb.equal(root.get(property), organization))
        : cb.equal(root.get(property), organization));
  }

  private static String organizationProperty(Class<?> type) {
    if (BeanUtils.getPropertyDescriptor(type, "empCodigo") != null) return "empCodigo";
    if (BeanUtils.getPropertyDescriptor(type, "empresa") != null) return "empresa";
    return null;
  }

  /** A loaded reference must not become a rule fact if it belongs to another organization. */
  static void requireReferencesInOrganization(Integer org, Object... references) {
    for (Object reference : references) {
      if (reference == null) continue;
      String property = organizationProperty(reference.getClass());
      if (property != null) {
        Object owner = new BeanWrapperImpl(reference).getPropertyValue(property);
        if (owner != null && !Objects.equals(owner, org)) throw denied();
      }
    }
  }

  private static ResponseStatusException denied() {
    return new ResponseStatusException(HttpStatus.FORBIDDEN, "Registro fora do órgão corrente.");
  }
}
