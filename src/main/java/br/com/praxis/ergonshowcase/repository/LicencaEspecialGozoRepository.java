package br.com.praxis.ergonshowcase.repository;
import br.com.praxis.ergonshowcase.domain.LicencaEspecialGozo;
import java.util.*;
import org.praxisplatform.uischema.repository.base.BaseCrudRepository;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;

public interface LicencaEspecialGozoRepository extends BaseCrudRepository<LicencaEspecialGozo, Long> {
  @Override @EntityGraph(attributePaths = {"pessoa", "vinculo", "orgao", "documentoLegal", "atoPublicacao", "periodoAquisitivoLicenca"})
  Page<LicencaEspecialGozo> findAll(Specification<LicencaEspecialGozo> spec, Pageable pageable);
  @Override @EntityGraph(attributePaths = {"pessoa", "vinculo", "orgao", "documentoLegal", "atoPublicacao", "periodoAquisitivoLicenca"})
  List<LicencaEspecialGozo> findAll(Specification<LicencaEspecialGozo> spec, Sort sort);
  @Override @EntityGraph(attributePaths = {"pessoa", "vinculo", "orgao", "documentoLegal", "atoPublicacao", "periodoAquisitivoLicenca"})
  List<LicencaEspecialGozo> findAll(Specification<LicencaEspecialGozo> spec);
  @Override @EntityGraph(attributePaths = {"pessoa", "vinculo", "orgao", "documentoLegal", "atoPublicacao", "periodoAquisitivoLicenca"})
  List<LicencaEspecialGozo> findAll();
  @Override @EntityGraph(attributePaths = {"pessoa", "vinculo", "orgao", "documentoLegal", "atoPublicacao", "periodoAquisitivoLicenca"})
  Optional<LicencaEspecialGozo> findById(Long id);
  @Override @EntityGraph(attributePaths = {"pessoa", "vinculo", "orgao", "documentoLegal", "atoPublicacao", "periodoAquisitivoLicenca"})
  List<LicencaEspecialGozo> findAllById(Iterable<Long> ids);
}
