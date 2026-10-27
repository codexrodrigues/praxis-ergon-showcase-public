package br.com.praxis.ergonshowcase.repository;

import br.com.praxis.ergonshowcase.domain.*;
import org.praxisplatform.uischema.repository.base.BaseCrudRepository;

/** Persistência canônica do recurso cargos. */
public interface CargoRepository extends BaseCrudRepository<Cargo, Integer> {}
