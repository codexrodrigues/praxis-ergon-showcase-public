package br.com.praxis.ergonshowcase.service;
import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.domain.RadarOrganogramaChefiaOcupacao;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.mapper.RadarOrganogramaChefiaOcupacaoMapper;
import br.com.praxis.ergonshowcase.repository.RadarOrganogramaChefiaOcupacaoRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Set;
import org.praxisplatform.uischema.exporting.CollectionExportField;
import org.praxisplatform.uischema.exporting.CollectionExportFieldPresentation;
import org.praxisplatform.uischema.stats.StatsFieldRegistry;
import org.praxisplatform.uischema.stats.StatsMetric;
@Service public class RadarOrganogramaChefiaOcupacaoService extends ErgonReadOnlyResourceService<RadarOrganogramaChefiaOcupacao,RadarOrganogramaChefiaOcupacaoResponseDTO,String,RadarOrganogramaChefiaOcupacaoFilterDTO> {
 private final RadarOrganogramaChefiaOcupacaoMapper mapper;
 @Override public org.praxisplatform.uischema.filter.specification.GenericSpecificationsBuilder<RadarOrganogramaChefiaOcupacao> getSpecificationsBuilder() {return new OrganogramaRadarQuery();}
 public RadarOrganogramaChefiaOcupacaoService(RadarOrganogramaChefiaOcupacaoRepository repository,RadarOrganogramaChefiaOcupacaoMapper mapper) {super(repository,RadarOrganogramaChefiaOcupacao.class,"id");this.mapper=mapper;}
 @Override protected RadarOrganogramaChefiaOcupacaoMapper getResourceMapper() {return mapper;}
 @Override protected RadarOrganogramaChefiaOcupacaoFilterDTO emptyFilter