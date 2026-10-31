package br.com.praxis.ergonshowcase.repository;

import br.com.praxis.ergonshowcase.domain.DefinicaoCampoCustomizado;
import java.util.*;
import org.praxisplatform.uischema.repository.base.BaseCrudRepository;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;

/** Consulta e persistência de definições de campos customizados multi-órgão sem N+1. */
public interface DefinicaoCampoCustomizadoRepository
    extends BaseCrudRepository<DefinicaoCampoCustomizado, Long> {
  @Override
  @EntityGraph(attributePaths = {"orgao", "optionTabelaEntidade"})
  Page<DefinicaoCampoCustomizado> findAll(
      Specification<DefinicaoCampoCustomizado> spec, Pageable pageable);

  @Override
  @EntityGraph(attributePaths = {"orgao", "optionTabelaEntidade"})
  List<DefinicaoCampoCustomizado> findAll(Specification<DefinicaoCampoCustomizado> spec, Sort sort);

  @Override
  @EntityGraph(attributePaths = {"orgao", "optionTabelaEntidade"})
  List<DefinicaoCampoCustomizado> findAll(Specification<DefinicaoCampoCustomizado> spec);

  @Override
  @EntityGraph(attributePaths = {"orgao", "optionTabelaEntidade"})
  List<DefinicaoCampoCustomizado> findAll();

  @Override
  @EntityGraph(attributePaths = {"orgao", "optionTabelaEntidade"})
  List<DefinicaoCampoCustomizado> findAllById(Iterable<Long> ids);

  @Override
  @EntityGraph(attributePaths = {"orgao", "optionTabelaEntidade"})
  Optional<DefinicaoCampoCustomizado> findById(Long id);
}
