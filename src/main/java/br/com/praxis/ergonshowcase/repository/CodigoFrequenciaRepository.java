package br.com.praxis.ergonshowcase.repository;

import br.com.praxis.ergonshowcase.domain.CodigoFrequencia;
import br.com.praxis.ergonshowcase.domain.CodigoFrequenciaId;
import java.util.*;
import org.praxisplatform.uischema.repository.base.BaseCrudRepository;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;

/** Consulta do catálogo de códigos de ocorrência e modalidades de licença (CODIGOS_FREQ_) sem N+1. */
public interface CodigoFrequenciaRepository
    extends BaseCrudRepository<CodigoFrequencia, CodigoFrequenciaId> {
  @Override
  @EntityGraph(attributePaths = {"tipoFrequenciaEntidade", "documentoLegal"})
  Page<CodigoFrequencia> findAll(Specification<CodigoFrequencia> spec, Pageable pageable);

  @Override
  @EntityGraph(attributePaths = {"tipoFrequenciaEntidade", "documentoLegal"})
  List<CodigoFrequencia> findAll(Specification<CodigoFrequencia> spec, Sort sort);

  @Override
  @EntityGraph(attributePaths = {"tipoFrequenciaEntidade", "documentoLegal"})
  List<CodigoFrequencia> findAll(Specification<CodigoFrequencia> spec);

  @Override
  @EntityGraph(attributePaths = {"tipoFrequenciaEntidade", "documentoLegal"})
  List<CodigoFrequencia> findAll();

  @Override
  @EntityGraph(attributePaths = {"tipoFrequenciaEntidade", "documentoLegal"})
  List<CodigoFrequencia> findAllById(Iterable<CodigoFrequenciaId> ids);

  @Override
  @EntityGraph(attributePaths = {"tipoFrequenciaEntidade", "documentoLegal"})
  Optional<CodigoFrequencia> findById(CodigoFrequenciaId id);
}
