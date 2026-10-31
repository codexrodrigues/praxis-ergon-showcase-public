package br.com.praxis.ergonshowcase.repository;

import br.com.praxis.ergonshowcase.domain.TipoDependente;
import java.util.*;
import org.praxisplatform.uischema.repository.base.BaseCrudRepository;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;

/** Consulta do catálogo de classificações legais de dependência (TIPO_DEPEN) sem N+1. */
public interface TipoDependenteRepository extends BaseCrudRepository<TipoDependente, String> {
  @Override
  @EntityGraph(attributePaths = {"documentoLegal"})
  Page<TipoDependente> findAll(Specification<TipoDependente> spec, Pageable pageable);

  @Override
  @EntityGraph(attributePaths = {"documentoLegal"})
  List<TipoDependente> findAll(Specification<TipoDependente> spec, Sort sort);

  @Override
  @EntityGraph(attributePaths = {"documentoLegal"})
  List<TipoDependente> findAll(Specification<TipoDependente> spec);

  @Override
  @EntityGraph(attributePaths = {"documentoLegal"})
  List<TipoDependente> findAll();

  @Override
  @EntityGraph(attributePaths = {"documentoLegal"})
  List<TipoDependente> findAllById(Iterable<String> ids);

  @Override
  @EntityGraph(attributePaths = {"documentoLegal"})
  Optional<TipoDependente> findById(String id);
}
