package br.com.praxis.ergonshowcase.service;

import br.com.praxis.ergonshowcase.domain.Tabela;
import br.com.praxis.ergonshowcase.dto.TabelaFilterDTO;
import br.com.praxis.ergonshowcase.dto.TabelaResponseDTO;
import br.com.praxis.ergonshowcase.mapper.TabelaMapper;
import br.com.praxis.ergonshowcase.repository.TabelaRepository;
import java.util.List;
import java.util.OptionalLong;
import java.util.Set;
import org.praxisplatform.uischema.exporting.CollectionExportField;
import org.praxisplatform.uischema.exporting.CollectionExportFieldPresentation;
import org.praxisplatform.uischema.stats.StatsFieldRegistry;
import org.praxisplatform.uischema.stats.StatsMetric;
import org.praxisplatform.uischema.stats.StatsSupportMode;
import org.springframework.stereotype.Service;

/** Serviço estritamente de leitura para o catálogo mestre de tabelas gerais (HADES.TABELA). */
@Service
public class TabelaService
    extends ErgonReadOnlyResourceService<Tabela, TabelaResponseDTO, String, TabelaFilterDTO> {
  private final TabelaMapper mapper;

  public TabelaService(TabelaRepository repository, TabelaMapper mapper) {
    super(repository, Tabela.class, "tab");
    this.mapper = mapper;
  }

  @Override
  protected TabelaMapper getResourceMapper() {
    return mapper;
  }

  @Override
  protected TabelaFilterDTO emptyFilter() {
    return new TabelaFilterDTO();
  }

  @Override
  protected OptionalLong getEntityResourceVersion(Tabela entity) {
    return OptionalLong.of(entity.getVersion());
  }

  @Override
  protected List<CollectionExportField> exportFields() {
    var boolPresentation =
        new CollectionExportFieldPresentation(
            "boolean", null, null, "pt-BR", null, "Sim", "Não", "Não informado");
    return List.of(
        field("id", "Tabela", "string"),
        field("sis", "Sistema", "string"),
        field("descr", "Descrição", "string"),
        presentedField("multiEmp", "Multi-órgão", "boolean", boolPresentation),
        field("tipo", "Tipo de código", "string"),
        field("taminfo", "Tamanho máximo", "integer"));
  }

  @Override
  public StatsFieldRegistry getStatsFieldRegistry() {
    return StatsFieldRegistry.builder()
        .labeledGroupByBucket("id", "tab", "descr", Set.of(StatsMetric.COUNT))
        .categoricalGroupByBucket("sis", "sis")
        .categoricalGroupByBucket("multiEmp", "multiEmp")
        .categoricalGroupByBucket("tipo", "tipo")
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
