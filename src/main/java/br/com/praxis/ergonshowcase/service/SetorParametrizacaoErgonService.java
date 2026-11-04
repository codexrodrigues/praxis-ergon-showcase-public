package br.com.praxis.ergonshowcase.service;
import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.domain.SetorParametrizacaoErgon;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.mapper.SetorParametrizacaoErgonMapper;
import br.com.praxis.ergonshowcase.repository.SetorParametrizacaoErgonRepository;
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
public class SetorParametrizacaoErgonService extends ErgonResourceService<SetorParametrizacaoErgon,SetorParametrizacaoErgonResponseDTO,Long,SetorParametrizacaoErgonFilterDTO,CreateSetorParametrizacaoErgonDTO,UpdateSetorParametrizacaoErgonDTO> {
 private final SetorParametrizacaoErgonMapper mapper;
 public SetorParametrizacaoErgonService(SetorParametrizacaoErgonRepository repository,SetorParametrizacaoErgonMapper mapper) {super(repository,SetorParametrizacaoErgon.class,"id");this.mapper=mapper;}
 @Override protected SetorParametrizacaoErgonMapper getResourceMapper() {return mapper;}
 @Override protected SetorParametrizacaoErgonFilterDTO emptyFilter() {return new SetorParametrizacaoErgonFilterDTO();}
 @Override protected String resolveResourceKey() {return ApiPaths.KEY_SETORES_ERGON;}
  @Override
  public StatsFieldRegistry getStatsFieldRegistry() {
    return StatsFieldRegistry.builder()
        .labeledGroupByBucket("id", "id", "setorNome", Set.of(StatsMetric.COUNT))
        .categoricalGroupByBucket("tipoAcesso", "tipoAcesso")
        .categoricalTermsBucket("grauRiscoAmbiental", "grauRiscoAmbiental")
        .temporalTimeSeriesField("dtini",