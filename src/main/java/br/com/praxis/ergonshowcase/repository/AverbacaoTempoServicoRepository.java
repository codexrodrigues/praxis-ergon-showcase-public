package br.com.praxis.ergonshowcase.repository;
import br.com.praxis.ergonshowcase.domain.AverbacaoTempoServico;
import java.util.*;
import org.praxisplatform.uischema.repository.base.BaseCrudRepository;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;

public interface AverbacaoTempoServicoRepository extends BaseCrudRepository<AverbacaoTempoServico, Long> {
  @Override @EntityGraph(attributePaths = {"pessoa", "vinculo", "orgao", "documentoLegal", "atoPublicacao"})
  Page<AverbacaoTempoServico> findAll(Specification<AverbacaoTempoServico> spec, Pageable pageable);
  @Override @EntityGraph(attributePaths = {"pessoa", "vinculo", "orgao", "documentoLegal", "atoPublicacao"})
  List<AverbacaoTempoServico> findAll(Specification<AverbacaoTempoServico> spec, Sort sort);
  @Override @EntityGraph(attributePaths = {"pessoa", "vinculo", "orgao", "documentoLegal", "atoPublicacao"})
  List<AverbacaoTempoServico> findAll(Specification<AverbacaoTempoServico> spec);
  @Override @EntityGraph(attributePaths = {"pessoa", "vinculo", "orgao", "documentoLegal", "atoPublicacao"})
  List<AverbacaoTempoServico> findAll();
  @Override @EntityGraph(attributePaths = {"pessoa", "vinculo", "orgao", "documentoLegal", "atoPublicacao"})
  Optional<AverbacaoTempoServico> findById(Long id);
  @Override @EntityGraph(attributePaths = {"pessoa", "vinculo", "orgao", "documentoLegal", "atoPublicacao"})
  List<AverbacaoTempoServico> findAllById(Iterable<Long> ids);
}
