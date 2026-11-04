package br.com.praxis.ergonshowcase.repository;

import br.com.praxis.ergonshowcase.domain.RadarChefiaSubstituicao;
import java.util.List;
import org.praxisplatform.uischema.repository.base.BaseCrudRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;

public interface RadarChefiaSubstituicaoRepository extends BaseCrudRepository<RadarChefiaSubstituicao, Long> {
  @Override
  @EntityGraph(attributePaths = "titular.pessoa")
  Page<RadarChefiaSubstituicao> findAll(Specification<RadarChefiaSubstituicao> spec, Pageable pageable);

  @Override
  @EntityGraph(attributePaths = "titular.pessoa")
  List<RadarChefiaSubstituicao> findAll(Specification<RadarChefiaSubstituicao> spec, Sort sort);
}
