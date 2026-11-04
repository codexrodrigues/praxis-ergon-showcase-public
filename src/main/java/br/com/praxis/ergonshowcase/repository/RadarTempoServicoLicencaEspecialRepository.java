package br.com.praxis.ergonshowcase.repository;
import br.com.praxis.ergonshowcase.domain.RadarTempoServicoLicencaEspecial;
import java.util.*;
import org.praxisplatform.uischema.repository.base.BaseCrudRepository;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;

public interface RadarTempoServicoLicencaEspecialRepository extends BaseCrudRepository<RadarTempoServicoLicencaEspecial, Long> {
  @Override @EntityGraph(attributePaths = {"pessoa", "vinculo", "orgao", "documentoLegal", "atoPublicacao"})
  Page<RadarTempoServicoLicencaEspecial> findAll(Specification<RadarTempoServicoLicencaEspecial> spec, Pageable pageable);
  @Override @EntityGraph(attributePaths = {"pessoa", "vinculo", "orgao", "documentoLegal", "atoPublicacao"})
  List<RadarTempoServicoLicencaEspecial> findAll(Specification<RadarTempoServicoLicencaEspecial> spec, Sort sort);
  @Override @EntityGraph(attributePaths = {"pessoa", "vinculo", "orgao", "documentoLegal", "atoPublicacao"})
  List<RadarTempoServicoLicencaEspecial> findAll(Specification<RadarTempoServicoLicencaEspecial> spec);
  @Override @EntityGraph(attributePaths = {"pessoa", "vinculo", "orgao", "documentoLegal", "atoPublicacao"})
  List<RadarTempoServicoLicencaEspecial> findAll();
  @Override @EntityGraph(attributePaths = {"pessoa", "vinculo", "orgao", "documentoLegal", "atoPublicacao"})
  Optional<RadarTempoServicoLicencaEspecial> findById(Long id);
  @Override @EntityGraph(attributePaths = {"pessoa", "vinculo", "orgao", "documentoLegal", "atoPublicacao"})
  List<RadarTempoServicoLicencaEspecial> findAllById(Iterable<Long> ids);
}
