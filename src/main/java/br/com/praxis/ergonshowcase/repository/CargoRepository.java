package br.com.praxis.ergonshowcase.repository;

import br.com.praxis.ergonshowcase.domain.*;
import java.util.*;
import org.praxisplatform.uischema.repository.base.BaseCrudRepository;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;

/** Plano de leitura das relações necessárias à projeção; o custo não cresce por linha. */
public interface CargoRepository extends BaseCrudRepository<Cargo, Integer> {
  @Override
  @EntityGraph(attributePaths = {"orgao"})
  Page<Cargo> findAll(Specification<Cargo> spec, Pageable pageable);

  @Override
  @EntityGraph(attributePaths = {"orgao"})
  List<Cargo> findAll(Specification<Cargo> spec, Sort sort);

  @Override
  @EntityGraph(attributePaths = {"orgao"})
  List<Cargo> findAll(Specification<Cargo> spec);

  @Override
  @EntityGraph(attributePaths = {"orgao"})
  List<Cargo> findAll();

  @Override
  @EntityGraph(attributePaths = {"orgao"})
  List<Cargo> findAllById(Iterable<Integer> ids);

  @Override
  @EntityGraph(attributePaths = {"orgao"})
  Optional<Cargo> findById(Integer id);
}
