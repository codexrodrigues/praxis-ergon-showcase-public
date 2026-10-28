package br.com.praxis.ergonshowcase.repository;

import br.com.praxis.ergonshowcase.domain.Setor;
import org.praxisplatform.uischema.repository.base.BaseCrudRepository;

/** Consulta do cadastro auxiliar existente, sem evolução de schema. */
public interface SetorRepository extends BaseCrudRepository<Setor, String> {}
