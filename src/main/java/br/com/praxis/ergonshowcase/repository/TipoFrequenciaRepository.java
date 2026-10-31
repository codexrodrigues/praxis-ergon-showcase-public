package br.com.praxis.ergonshowcase.repository;

import br.com.praxis.ergonshowcase.domain.TipoFrequencia;
import java.util.*;
import org.praxisplatform.uischema.repository.base.BaseCrudRepository;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;

/** Consulta do catálogo de naturezas macro de frequência e afastamento (TIPO_FREQ_) sem N+1. */
public interface TipoFrequenciaRepository extends BaseCrudRepository<TipoFrequencia, String> {
  @Override
  @EntityGraph(attributePaths = {"documentoLegal"})
  Page<TipoFrequencia> findAll(Specification<TipoFrequencia> spec, Pageable pageable);

  @Override
  @EntityGraph(attributePaths = {"documentoLegal"})
  List<TipoFrequencia> findAll(Specification<TipoFrequencia> spec, Sort sort);

  @Override
  @EntityGraph(attributePaths = {"documentoLegal"})
  List<TipoFrequencia> findAll(Specification<TipoFrequencia> spec);

  @Override
  @EntityGraph(attributePaths = {"documentoLegal"})
  List<TipoFrequencia> findAll();

  @Override
  @EntityGraph(attributePaths = {"documentoLegal"})
  List<TipoFrequencia> findAllById(Iterable<String> ids);

  @Override
  @EntityGraph(attributePaths = {"documentoLegal"})
  Optional<TipoFrequencia> findById(String id);
}
