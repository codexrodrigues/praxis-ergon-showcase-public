package br.com.praxis.ergonshowcase.repository;

import br.com.praxis.ergonshowcase.domain.PeriodoAquisitivoFerias;
import java.util.List;
import java.util.Optional;
import org.praxisplatform.uischema.repository.base.BaseCrudRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;

/** Plano de leitura das relações necessárias à projeção de períodos aquisitivos de férias sem N+1. */
public interface PeriodoAquisitivoFeriasRepository
    extends BaseCrudRepository<PeriodoAquisitivoFerias, Long> {
  @Override
  @EntityGraph(
      attributePaths = {"orgao", "pessoa", "vinculo", "documentoLegal", "atoPublicacao"})
  Page<PeriodoAquisitivoFerias> findAll(
      Specification<PeriodoAquisitivoFerias> spec, Pageable pageable);

  @Override
  @EntityGraph(
      attributePaths = {"orgao", "pessoa", "vinculo", "documentoLegal", "atoPublicacao"})
  List<PeriodoAquisitivoFerias> findAll(Specification<PeriodoAquisitivoFerias> spec, Sort sort);

  @Override
  @EntityGraph(
      attributePaths = {"orgao", "pessoa", "vinculo", "documentoLegal", "atoPublicacao"})
  List<PeriodoAquisitivoFerias> findAll(Specification<PeriodoAquisitivoFerias> spec);

  @Override
  @EntityGraph(
      attributePaths = {"orgao", "pessoa", "vinculo", "documentoLegal", "atoPublicacao"})
  List<PeriodoAquisitivoFerias> findAll();

  @Override
  @EntityGraph(
      attributePaths = {"orgao", "pessoa", "vinculo", "documentoLegal", "atoPublicacao"})
  List<PeriodoAquisitivoFerias> findAllById(Iterable<Long> ids);

  @Override
  @EntityGraph(
      attributePaths = {"orgao", "pessoa", "vinculo", "documentoLegal", "atoPublicacao"})
  Optional<PeriodoAquisitivoFerias> findById(Long id);
}
