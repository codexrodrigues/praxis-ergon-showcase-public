package br.com.praxis.ergonshowcase.repository;

import br.com.praxis.ergonshowcase.domain.ParametroContagemFerias;
import java.util.List;
import java.util.Optional;
import org.praxisplatform.uischema.repository.base.BaseCrudRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;

/** Plano de leitura das relações necessárias à projeção de parâmetros de contagem de férias sem N+1. */
public interface ParametroContagemFeriasRepository
    extends BaseCrudRepository<ParametroContagemFerias, Long> {
  @Override
  @EntityGraph(attributePaths = {"orgao", "documentoLegalFerias", "documentoLegalProgressao"})
  Page<ParametroContagemFerias> findAll(
      Specification<ParametroContagemFerias> spec, Pageable pageable);

  @Override
  @EntityGraph(attributePaths = {"orgao", "documentoLegalFerias", "documentoLegalProgressao"})
  List<ParametroContagemFerias> findAll(Specification<ParametroContagemFerias> spec, Sort sort);

  @Override
  @EntityGraph(attributePaths = {"orgao", "documentoLegalFerias", "documentoLegalProgressao"})
  List<ParametroContagemFerias> findAll(Specification<ParametroContagemFerias> spec);

  @Override
  @EntityGraph(attributePaths = {"orgao", "documentoLegalFerias", "documentoLegalProgressao"})
  List<ParametroContagemFerias> findAll();

  @Override
  @EntityGraph(attributePaths = {"orgao", "documentoLegalFerias", "documentoLegalProgressao"})
  List<ParametroContagemFerias> findAllById(Iterable<Long> ids);

  @Override
  @EntityGraph(attributePaths = {"orgao", "documentoLegalFerias", "documentoLegalProgressao"})
  Optional<ParametroContagemFerias> findById(Long id);
}
