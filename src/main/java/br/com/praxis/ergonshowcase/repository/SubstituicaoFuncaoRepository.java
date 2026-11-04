package br.com.praxis.ergonshowcase.repository;

import br.com.praxis.ergonshowcase.domain.SubstituicaoFuncao;
import java.util.List;
import org.praxisplatform.uischema.repository.base.BaseCrudRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;

public interface SubstituicaoFuncaoRepository extends BaseCrudRepository<SubstituicaoFuncao, Long> {
  @Override
  @EntityGraph(attributePaths = "substituto.pessoa")
  Page<SubstituicaoFuncao> findAll(Specification<SubstituicaoFuncao> spec, Pageable pageable);

  @Override
  @EntityGraph(attributePaths = "substituto.pessoa")
  List<SubstituicaoFuncao> findAll(Specification<SubstituicaoFuncao> spec, Sort sort);
}
