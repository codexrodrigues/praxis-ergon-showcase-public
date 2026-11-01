package br.com.praxis.ergonshowcase.service;

import br.com.praxis.ergonshowcase.domain.TipoFrequencia;
import br.com.praxis.ergonshowcase.dto.TipoFrequenciaFilterDTO;
import br.com.praxis.ergonshowcase.dto.TipoFrequenciaResponseDTO;
import br.com.praxis.ergonshowcase.mapper.TipoFrequenciaMapper;
import br.com.praxis.ergonshowcase.repository.TipoFrequenciaRepository;
import java.util.List;
import java.util.OptionalLong;
import java.util.Set;
import org.praxisplatform.uischema.exporting.CollectionExportField;
import org.praxisplatform.uischema.exporting.CollectionExportFieldPresentation;
import org.praxisplatform.uischema.stats.StatsFieldRegistry;
import org.praxisplatform.uischema.stats.StatsMetric;
import org.praxisplatform.uischema.stats.StatsSupportMode;
import org.springframework.stereotype.Service;

/** Serviço estritamente de leitura para o catálogo de tipos macro de frequência (TIPO_FREQ_). */
@Service
public class TipoFrequenciaService
    extends ErgonReadOnlyResourceService<
        TipoFrequencia, TipoFrequenciaResponseDTO, String, TipoFrequenciaFilterDTO> {
  private final TipoFrequenciaMapper mapper;

  public TipoFrequenciaService(
      TipoFrequenciaRepository repository, TipoFrequenciaMapper mapper) {
    super(repository, TipoFrequencia.class, "tipo");
    this.mapper = mapper;
  }

  @Override
  protected TipoFrequenciaMapper getResourceMapper() {
    return mapper;
  }

  @Override
  protected TipoFrequenciaFilterDTO emptyFilter() {
    return new TipoFrequenciaFilterDTO();
  }

  @Override
  protected OptionalLong getEntityResourceVersion(TipoFrequencia entity) {
    return OptionalLong.of(entity.getVersion());
  }

  @Override
  protected List<CollectionExportField> exportFields() {
    var boolPresentation =
        new CollectionExportFieldPresentation(
            "boolean", null, null, "pt-BR", null, "Sim", "Não", "Não informado");
    return List.of(
        field("id", "Tipo", "string"),
        field("nome", "Denominação", "string"),
        presentedField("influiConta", "Influi contagem", "boolean", boolPresentation),
        presentedField("horaExtra", "Hora extra", "boolean", boolPresentation),
        field("percentualExtra", "Percentual extra", "number"));
  }

  @Override
  public StatsFieldRegistry getStatsFieldRegistry() {
    return StatsFieldRegistry.builder()
        .labeledGroupByBucket("id", "tipo", "nome", Set.of(StatsMetric.COUNT))
        .categoricalGroupByBucket("influiConta", "influiConta")
        .categoricalGroupByBucket("horaExtra", "horaExtra")
        .build();
  }

  @Override
  public StatsSupportMode getGroupByStatsSupportMode() {
    return StatsSupportMode.AUTO;
  }

  @Override
  public StatsSupportMode getDistributionStatsSupportMode() {
    return StatsSupportMode.AUTO;
  }
}
