package br.com.praxis.ergonshowcase.service;

import br.com.praxis.ergonshowcase.domain.VinculoId;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;
import org.praxisplatform.uischema.repository.base.BaseCrudRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Guardas estruturais comuns aos cadastros de chefia. */
public abstract class ChefiaResourceService<E, R, I, F extends GenericFilterDTO, C, U>
    extends ErgonResourceService<E, R, I, F, C, U> {
  protected ChefiaResourceService(BaseCrudRepository<E, I> repository, Class<E> type, String identityPath) {
    super(repository, type, identityPath);
  }

  @Override
  protected void beforeCreate(C dto, E candidate) {
    assertEntityAccessAllowed(candidate);
    validateReferences(candidate);
    validateBusinessRules(candidate, null);
  }

  @Override
  protected void beforeUpdate(I id, E existing, U dto) {
    E candidate = snapshotBeforeMutation(existing);
    getResourceMapper().applyUpdate(candidate, dto);
    assertEntityAccessAllowed(candidate);
    validateReferences(candidate);
    validateBusinessRules(candidate, existing);
  }

  protected abstract void validateReferences(E candidate);

  protected void validateBusinessRules(E candidate) {}

  protected void validateBusinessRules(E candidate, E previous) {
    validateBusinessRules(candidate);
  }

  protected void requireReference(Integer orgao, Class<?> type, Object id) {
    if (id == null) return;
    if (id instanceof VinculoId vinculo && (vinculo.numfunc() == null || vinculo.numero() == null)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe os dois componentes do vínculo.");
    }
    Object reference = getEntityManager().find(type, id);
    if (reference == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Referência funcional inexistente.");
    }
    ErgonTenantAccess.requireReferencesInOrganization(orgao, reference);
  }
}
