package br.com.praxis.ergonshowcase.repository;

import br.com.praxis.ergonshowcase.domain.*;
import java.util.*;
import org.praxisplatform.uischema.repository.base.BaseCrudRepository;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;

/** Plano de leitura das relações necessárias à projeção; o custo não cresce por linha. */
public interface VinculoRepository extends BaseCrudRepository<Vinculo, VinculoId> {
  @Override
  @EntityGraph(
      attributePaths = {
        "orgao",
        "pessoa",
        "cargoEntidade",
        "lotacao",
        "exercicio",
        "documentoLegal"
      })
  Page<Vinculo> findAll(Specification<Vinculo> spec, Pageable pageable);

  @Override
  @EntityGraph(
      attributePaths = {
        "orgao",
        "pessoa",
        "cargoEntidade",
        "lotacao",
        "exercicio",
        "documentoLegal"
      })
  List<Vinculo> findAll(Specification<Vinculo> spec, Sort sort);

  @Override
  @EntityGraph(
      attributePaths = {
        "orgao",
        "pessoa",
        "cargoEntidade",
        "lotacao",
        "exercicio",
        "documentoLegal"
      })
  List<Vinculo> findAll(Specification<Vinculo> spec);

  @Override
  @EntityGraph(
      attributePaths = {
        "orgao",
        "pessoa",
        "cargoEntidade",
        "lotacao",
        "exercicio",
        "documentoLegal"
      })
  List<Vinculo> findAll();

  @Override
  @EntityGraph(
      attributePaths = {
        "orgao",
        "pessoa",
        "cargoEntidade",
        "lotacao",
        "exercicio",
        "documentoLegal"
      })
  List<Vinculo> findAllById(Iterable<VinculoId> ids);

  @Override
  @EntityGraph(
      attributePaths = {
        "orgao",
        "pessoa",
        "cargoEntidade",
        "lotacao",
        "exercicio",
        "documentoLegal"
      })
  Optional<Vinculo> findById(VinculoId id);
}
