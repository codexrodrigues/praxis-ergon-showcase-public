package br.com.praxis.ergonshowcase.repository;
import br.com.praxis.ergonshowcase.domain.VendaLicencaEspecial;
import java.util.*;
import org.praxisplatform.uischema.repository.base.BaseCrudRepository;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;

public interface VendaLicencaEspecialRepository extends BaseCrudRepository<VendaLicencaEspecial, Long> {
  @Override @EntityGraph(attributePaths = {"pessoa", "vinculo", "orgao", "documentoLegal", "atoPublicacao", "periodoAquisitivoLicenca", "licencaEspecialGozo"})
  Page<VendaLicencaEspecial> findAll(Specification<VendaLicencaEspecial> spec, Pageable pageable);
  @Override @EntityGraph(attributePaths = {"pessoa", "vinculo", "orgao", "documentoLegal", "atoPublicacao", "periodoAquisitivoLicenca", "licencaEspecialGozo"})
  List<VendaLicencaEspecial> findAll(Specification<VendaLicencaEspecial> spec, Sort sort);
  @Override @EntityGraph(attributePaths = {"pessoa", "vinculo", "orgao", "documentoLegal", "atoPublicacao", "periodoAquisitivoLicenca", "licencaEspecialGozo"})
  List<VendaLicencaEspecial> findAll(Specification<VendaLicencaEspecial> spec);
  @Override @EntityGraph(attributePaths = {"pessoa", "vinculo", "orgao", "documentoLegal", "atoPublicacao", "periodoAquisitivoLicenca", "licencaEspecialGozo"})
  List<VendaLicencaEspecial> findAll();
  @Override @EntityGraph(attributePaths = {"pessoa", "vinculo", "orgao", "documentoLegal", "atoPublicacao", "periodoAquisitivoLicenca", "licencaEspecialGozo"})
  Optional<VendaLicencaEspecial> findById(Long id);
  @Override @EntityGraph(attributePaths = {"pessoa", "vinculo", "orgao", "documentoLegal", "atoPublicacao", "periodoAquisitivoLicenca", "licencaEspecialGozo"})
  List<VendaLicencaEspecial> findAllById(Iterable<Long> ids);
}
