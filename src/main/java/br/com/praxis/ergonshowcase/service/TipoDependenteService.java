package br.com.praxis.ergonshowcase.service;

import br.com.praxis.ergonshowcase.domain.TipoDependente;
import br.com.praxis.ergonshowcase.dto.TipoDependenteFilterDTO;
import br.com.praxis.ergonshowcase.dto.TipoDependenteResponseDTO;
import br.com.praxis.ergonshowcase.mapper.TipoDependenteMapper;
import br.com.praxis.ergonshowcase.repository.TipoDependenteRepository;
import java.util.List;
import java.util.OptionalLong;
import java.util.Set;
import org.praxisplatform.uischema.exporting.CollectionExportField;
import org.praxisplatform.uischema.exporting.CollectionExportFieldPresentation;
import org.praxisplatform.uischema.stats.StatsFieldRegistry;
import org.praxisplatform.uischema.stats.StatsMetric;
import org.praxisplatform.uischema.stats.StatsSupportMode;
import org.springframework.stereotype.Service;

/** Serviço estritamente de leitura para o catálogo de tipos de dependente (TIPO_DEPEN). */
@Service
public class TipoDependenteService
    extends ErgonReadOnlyResourceService<
        TipoDependente, TipoDependenteResponseDTO, String, TipoDependenteFilterDTO> {
  private final TipoDependenteMapper mapper;

  public TipoDependenteService(
      TipoDependenteRepository repository, TipoDependenteMapper mapper) {
    super(repository, TipoDependente.class, "tipodepen");
    this.mapper = mapper;
  }

  @Override
  protected TipoDependenteMapper getResourceMapper() {
    return mapper;
  }

  @Override
  protected TipoDependenteFilterDTO emptyFilter() {
    return new TipoDependenteFilterDTO();
  }

  @Override
  protected OptionalLong getEntityResourceVersion(TipoDependente entity) {
    return OptionalLong.of(entity.getVersion());
  }

  @Override
  protected List<CollectionExportField> exportFields() {
    var boolPresentation =
        new CollectionExportFieldPresentation(
            "boolean", null, null, "pt-BR", null, "Sim", "Não", "Não informado");
    return List.of(
        field("id", "Código", "string"),
        field("descricao", "Descrição", "string"),
        presentedField("geraIr", "Gera IRRF", "boolean", boolPresentation),
        presentedField("geraSalarioFamilia", "Salário-família", "boolean", boolPresentation),
        field("idadeLimite", "Idade limite", "integer"));
  }

  @Override
  public StatsFieldRegistry getStatsFieldRegistry() {
    return StatsFieldRegistry.builder()
        .labeledGroupByBucket("id", "tipodepen", "descricao", Set.of(StatsMetric.COUNT))
        .categoricalGroupByBucket("geraIr", "geraIr")
        .categoricalGroupByBucket("geraSalarioFamilia", "geraSalarioFamilia")
        .numericHistogramMeasureField("idadeLimite", "idadeLimite")
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
