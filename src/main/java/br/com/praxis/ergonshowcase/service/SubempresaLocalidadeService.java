package br.com.praxis.ergonshowcase.service;
import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.domain.SubempresaLocalidade;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.mapper.SubempresaLocalidadeMapper;
import br.com.praxis.ergonshowcase.repository.SubempresaLocalidadeRepository;
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
public class SubempresaLocalidadeService extends ErgonResourceService<SubempresaLocalidade,SubempresaLocalidadeResponseDTO,Long,SubempresaLocalidadeFilterDTO,CreateSubempresaLocalidadeDTO,UpdateSubempresaLocalidadeDTO> {
 private final SubempresaLocalidadeMapper mapper;
 public SubempresaLocalidadeService(SubempresaLocalidadeRepository repository,SubempresaLocalidadeMapper mapper) {super(repository,SubempresaLocalidade.class,"id");this.mapper=mapper;}
 @Override protected SubempresaLocalidadeMapper getResourceMapper() {return mapper;}
 @Override protected SubempresaLocalidadeFilterDTO emptyFilter() {return new SubempresaLocalidadeFilterDTO();}
 @Override protected String resolveResourceKey() {return ApiPaths.KEY_SUBEMPRESAS_LOCALIDADES;}
  @Override
  public StatsFieldRegistry getStatsFieldRegistry() {
    return StatsFieldRegistry.builder()
        .labeledGroupByBucket("id", "id", "nome", Set.of(StatsMetric.COUNT))
        .categoricalGroupByBucket("naturezaUnidade", "naturezaUnidade")
        .categoricalTermsBucket("uf", "uf")
        .temporalTimeSeriesField("dtini", "dtini