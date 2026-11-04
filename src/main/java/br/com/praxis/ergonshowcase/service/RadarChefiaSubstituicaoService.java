package br.com.praxis.ergonshowcase.service;

import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.domain.*;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.mapper.RadarChefiaSubstituicaoMapper;
import br.com.praxis.ergonshowcase.repository.RadarChefiaSubstituicaoRepository;
import java.util.EnumSet;
import java.util.OptionalLong;
import org.praxisplatform.uischema.stats.StatsFieldRegistry;
import org.praxisplatform.uischema.stats.StatsMetric;
import org.springframework.stereotype.Service;

@Service
public class RadarChefiaSubstituicaoService extends ErgonReadOnlyResourceService<RadarChefiaSubstituicao, RadarChefiaSubstituicaoResponseDTO, Long, RadarChefiaSubstituicaoFilterDTO> {
  private final RadarChefiaSubstituicaoMapper mapper;

  public RadarChefiaSubstituicaoService(RadarChefiaSubstituicaoRepository repository, RadarChefiaSubstituicaoMapper mapper) {
    super(repository, RadarChefiaSubstituicao.class, "id");
    this.mapper = mapper;
  }

  @Override
  protected RadarChefiaSubstituicaoMapper getResourceMapper() { return mapper; }

  @Override
  protected RadarChefiaSubstituicaoFilterDTO emptyFilter() { return new RadarChefiaSubstituicaoFilterDTO(); }

  @Override
  protected String resolveResourceKey() { return ApiPaths.KEY_RADAR_CHEFIAS_SUBSTITUICOES; }

  @Override
  protected OptionalLong getEntityResourceVersion(RadarChefiaSubstituicao entity) { return OptionalLong.empty(); }

  @Override
  public StatsFieldRegistry getStatsFieldRegistry() {
    var metrics =
        EnumSet.of(
            StatsMetric.COUNT,
            StatsMetric.DISTINCT_COUNT,
            StatsMetric.SUM,
            StatsMetric.AVG,
            StatsMetric.MIN,
            StatsMetric.MAX);
    return StatsFieldRegistry.builder()
        .groupByBucket("id", "id", metrics)
        .groupByBucket("situacao", "situacao", metrics)
        .groupByBucket("setorId", "setorId", metrics)
        .temporalTimeSeriesField("dtini", "dtini")
        .numericHistogramMeasureField("qtdCoberturas", "qtdCoberturas")
        .numericMeasureField("pendenciasFolha", "pendenciasFolha")
        .build();
  }
}
