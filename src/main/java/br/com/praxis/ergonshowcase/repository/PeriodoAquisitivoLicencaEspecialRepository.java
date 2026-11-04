package br.com.praxis.ergonshowcase.repository;
import br.com.praxis.ergonshowcase.domain.PeriodoAquisitivoLicencaEspecial;
import java.util.*;
import org.praxisplatform.uischema.repository.base.BaseCrudRepository;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;

public interface PeriodoAquisitivoLicencaEspecialRepository extends BaseCrudRepository<PeriodoAquisitivoLicencaEspecial, Long> {
  @Override @EntityGraph(attributePaths = {"pessoa", "vinculo", "orgao", "documentoLegal", "atoPublicacao", "averbacaoOrigem"})
  Page<PeriodoAquisitivoLicencaEspecial> findAll(Specification<PeriodoAquisitivoLicencaEspecial> spec, Pageable pageable);
  @Override @EntityGraph(attributePaths = {"pessoa", "vinculo", "orgao", "documentoLegal", "atoPublicacao", "averbacaoOrigem"})
  List<PeriodoAquisitivoLicencaEspecial> findAll(Specification<PeriodoAquisitivoLicencaEspecial> spec, Sort sort);
  @Override @EntityGraph(attributePaths = {"pessoa", "vinculo", "orgao", "documentoLegal", "atoPublicacao", "averbacaoOrigem"})
  List<PeriodoAquisitivoLicencaEspecial> findAll(Specification<PeriodoAquisitivoLicencaEspecial> spec);
  @Override @EntityGraph(attributePaths = {"pessoa", "vinculo", "orgao", "documentoLegal", "atoPublicacao", "averbacaoOrigem"})
  List<PeriodoAquisitivoLicencaEspecial> findAll();
  @Override @EntityGraph(attributePaths = {"pessoa", "vinculo", "orgao", "documentoLegal", "atoPublicacao", "averbacaoOrigem"})
  Optional<PeriodoAquisitivoLicencaEspecial> findById(Long id);
  @Override @EntityGraph(attributePaths = {"pessoa", "vinculo", "orgao", "documentoLegal", "atoPublicacao", "averbacaoOrigem"})
  List<PeriodoAquisitivoLicencaEspecial> findAllById(Iterable<Long> ids);
}
