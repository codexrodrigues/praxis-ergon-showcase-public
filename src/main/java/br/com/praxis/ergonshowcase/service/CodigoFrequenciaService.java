package br.com.praxis.ergonshowcase.service;

import br.com.praxis.ergonshowcase.domain.CodigoFrequencia;
import br.com.praxis.ergonshowcase.domain.CodigoFrequenciaId;
import br.com.praxis.ergonshowcase.dto.CodigoFrequenciaFilterDTO;
import br.com.praxis.ergonshowcase.dto.CodigoFrequenciaResponseDTO;
import br.com.praxis.ergonshowcase.mapper.CodigoFrequenciaMapper;
import br.com.praxis.ergonshowcase.repository.CodigoFrequenciaRepository;
import java.util.List;
import java.util.OptionalLong;
import java.util.Set;
import org.praxisplatform.uischema.exporting.CollectionExportField;
import org.praxisplatform.uischema.exporting.CollectionExportFieldPresentation;
import org.praxisplatform.uischema.stats.StatsFieldRegistry;
import org.praxisplatform.uischema.stats.StatsMetric;
import org.praxisplatform.uischema.stats.StatsSupportMode;
import org.springframework.stereotype.Service;

/** Serviço estritamente de leitura para o catálogo de códigos de frequência (CODIGOS_FREQ_). */
@Service
public class CodigoFrequenciaService
    extends ErgonReadOnlyResourceService<
        CodigoFrequencia,
        CodigoFrequenciaResponseDTO,
        CodigoFrequenciaId,
        CodigoFrequenciaFilterDTO> {
  private final CodigoFrequenciaMapper mapper;

  public CodigoFrequenciaService(
      CodigoFrequenciaRepository repository, CodigoFrequenciaMapper mapper) {
    super(repository, CodigoFrequencia.class, "id.tipofreq", "id.codigo");
    this.mapper = mapper;
  }

  @Override
  protected CodigoFrequenciaMapper getResourceMapper() {
    return mapper;
  }

  @Override
  protected CodigoFrequenciaFilterDTO emptyFilter() {
    return new CodigoFrequenciaFilterDTO();
  }


  @Override
  protected List<CollectionExportField> exportFields() {
    var boolPresentation =
        new CollectionExportFieldPresentation(
            "boolean", null, null, "pt-BR", null, "Sim", "Não", "Não informado");
    return List.of(
        field("id", "Identificador", "string"),
        field("tipofreq", "Tipo de frequência", "string"),
        field("codigo", "Código", "integer"),
        field("nome", "Denominação", "string"),
        field("mnemonico", "Mnemônico", "string"),
        presentedField("viaFrequencia", "Via frequência", "boolean", boolPresentation),
        presentedField("afastamento", "Afastamento", "boolean", boolPresentation),
        presentedField("onus", "Com ônus", "boolean", boolPresentation));
  }

  @Override
  public StatsFieldRegistry getStatsFieldRegistry() {
    return StatsFieldRegistry.builder()
        .labeledGroupByBucket("id", "id", "nome", Set.of(StatsMetric.COUNT))
        .categoricalGroupByBucket("tipofreq", "id.tipofreq")
        .categoricalGroupByBucket("viaFrequencia", "viaFrequencia")
        .categoricalGroupByBucket("afastamento", "afastamento")
        .categoricalGroupByBucket("onus", "onus")
        .build();
  }


  @Override protected String resolveResourceKey() { return "administracao-pessoal.codigos-frequencia"; }

}
