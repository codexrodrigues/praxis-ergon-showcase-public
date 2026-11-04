package br.com.praxis.ergonshowcase.repository;

import br.com.praxis.ergonshowcase.domain.DesignacaoFuncao;
import java.util.List;
import org.praxisplatform.uischema.repository.base.BaseCrudRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;

public interface DesignacaoFuncaoRepository extends BaseCrudRepository<DesignacaoFuncao, Long> {
  @Override
  @EntityGraph(attributePaths = "titular.pessoa")
  Page<DesignacaoFuncao> findAll(Specification<DesignacaoFuncao> spec, Pageable pageable);

  @Override
  @EntityGraph(attributePaths = "titular.pessoa")
  List<DesignacaoFuncao> findAll(Specification<DesignacaoFuncao> spec, Sort sort);
}
