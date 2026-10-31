package br.com.praxis.ergonshowcase.repository;

import br.com.praxis.ergonshowcase.domain.*;
import java.util.*;
import org.praxisplatform.uischema.repository.base.BaseCrudRepository;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;

/** Plano de leitura das relações necessárias à projeção de dependentes sem N+1 por linha. */
public interface DependenteRepository extends BaseCrudRepository<Dependente, DependenteId> {
  @Override
  @EntityGraph(attributePaths = {"pessoa", "tipoDependenteEntidade", "documentoLegal"})
  Page<Dependente> findAll(Specification<Dependente> spec, Pageable pageable);

  @Override
  @EntityGraph(attributePaths = {"pessoa", "tipoDependenteEntidade", "documentoLegal"})
  List<Dependente> findAll(Specification<Dependente> spec, Sort sort);

  @Override
  @EntityGraph(attributePaths = {"pessoa", "tipoDependenteEntidade", "documentoLegal"})
  List<Dependente> findAll(Specification<Dependente> spec);

  @Override
  @EntityGraph(attributePaths = {"pessoa", "tipoDependenteEntidade", "documentoLegal"})
  List<Dependente> findAll();

  @Override
  @EntityGraph(attributePaths = {"pessoa", "tipoDependenteEntidade", "documentoLegal"})
  List<Dependente> findAllById(Iterable<DependenteId> ids);

  @Override
  @EntityGraph(attributePaths = {"pessoa", "tipoDependenteEntidade", "documentoLegal"})
  Optional<Dependente> findById(DependenteId id);

  boolean existsByIdNumfuncAndCpfAndIdNumdepNot(Long numfunc, String cpf, Integer numdep);
}
