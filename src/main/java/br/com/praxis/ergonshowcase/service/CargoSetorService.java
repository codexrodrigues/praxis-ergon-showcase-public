package br.com.praxis.ergonshowcase.service;
import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.domain.CargoSetor;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.mapper.CargoSetorMapper;
import br.com.praxis.ergonshowcase.repository.CargoSetorRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Set;
import org.praxisplatform.uischema.exporting.CollectionExportField;
import org.praxisplatform.uischema.exporting.CollectionExportFieldPresentation;
import org.praxisplatform.uischema.stats.StatsFieldRegistry;
import org.praxisplatform.uischema.stats.StatsMetric;
import org.springframework.security.access.AccessDeniedException;
/** Contrato mutável preparado; execução exige os gates transacionais das Ondas 2–3. */
@Service
public class CargoSetorService extends ErgonResourceService<CargoSetor,CargoSetorResponseDTO,Long,CargoSetorFilterDTO,CreateCargoSetorDTO,UpdateCargoSetorDTO> {
 private final CargoSetorMapper mapper;
 public CargoSetorService(CargoSetorRepository repository,CargoSetorMapper mapper) {super(repository,CargoSetor.class,"id");this.mapper=mapper;}
 @Override protected CargoSetorMapper getResourceMapper() {return mapper;}
 @Override protected CargoSetorFilterDTO emptyFilter() {return new CargoSetorFilterDTO();}
 @Override protected String resolveResourceKey() {return ApiPaths.KEY_CARGOS_POR_SETOR;}
  @Override
  public StatsFieldRegistry getStatsFieldRegistry() {
    return StatsFieldRegistry.builder()
        .labeledGroupByBucket("id", "id", "cargoNome", Set.of(StatsMetric.COUNT))
        .categoricalGroupByBucket("tipoControleQuadro", "tipoControleQuadro")
        .numericHistogramMeasure