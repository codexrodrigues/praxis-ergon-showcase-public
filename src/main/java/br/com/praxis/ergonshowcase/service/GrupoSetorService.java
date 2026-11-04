package br.com.praxis.ergonshowcase.service;
import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.domain.GrupoSetor;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.mapper.GrupoSetorMapper;
import br.com.praxis.ergonshowcase.repository.GrupoSetorRepository;
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
public class GrupoSetorService extends ErgonResourceService<GrupoSetor,GrupoSetorResponseDTO,Long,GrupoSetorFilterDTO,CreateGrupoSetorDTO,UpdateGrupoSetorDTO> {
 private final GrupoSetorMapper mapper;
 public GrupoSetorService(GrupoSetorRepository repository,GrupoSetorMapper mapper) {super(repository,GrupoSetor.class,"id");this.mapper=mapper;}
 @Override protected GrupoSetorMapper getResourceMapper() {return mapper;}
 @Override protected GrupoSetorFilterDTO emptyFilter() {return new GrupoSetorFilterDTO();}
 @Override protected String resolveResourceKey() {return ApiPaths.KEY_GRUPOS_SETORES;}
  @Override
  public StatsFieldRegistry getStatsFieldRegistry() {
    return StatsFieldRegistry.builder()
        .labeledGroupByBucket("id", "id", "nome", Set.of(StatsMetric.COUNT))
        .categoricalGroupByBucket("tipoAgrupamento", "tipoAgrupamento")
        .categoricalTermsBucket("ufAbrangen