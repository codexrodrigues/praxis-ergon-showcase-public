package br.com.praxis.ergonshowcase.service;

import br.com.praxis.ergonshowcase.config.TenantContext;
import br.com.praxis.ergonshowcase.domain.Setor;
import br.com.praxis.ergonshowcase.dto.SetorFilterDTO;
import br.com.praxis.ergonshowcase.dto.SetorResponseDTO;
import br.com.praxis.ergonshowcase.mapper.SetorMapper;
import br.com.praxis.ergonshowcase.repository.SetorRepository;
import java.util.List;
import java.util.OptionalLong;
import java.util.Set;
import org.praxisplatform.uischema.exporting.CollectionExportField;
import org.praxisplatform.uischema.exporting.CollectionExportFieldPresentation;
import org.praxisplatform.uischema.service.base.ResourceFilterAccessScope;
import org.praxisplatform.uischema.stats.StatsFieldRegistry;
import org.praxisplatform.uischema.stats.StatsMetric;
import org.praxisplatform.uischema.stats.StatsSupportMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Fronteira estritamente de leitura para referências e inspeção contextual de unidades organizacionais. */
@Service
public class SetorService
    extends ErgonReadOnlyResourceService<Setor, SetorResponseDTO, String, SetorFilterDTO> {
  private final SetorMapper mapper;
  private final TenantContext tenant;

  public SetorService(SetorRepository repository, SetorMapper mapper) {
    this(repository, mapper, new TenantContext());
  }

  @Autowired
  public SetorService(SetorRepository repository, SetorMapper mapper, TenantContext tenant) {
    super(repository, Setor.class, "setor");
    this.mapper = mapper;
    this.tenant = tenant != null ? tenant : new TenantContext();
  }

  @Override
  protected SetorMapper getResourceMapper() {
    return mapper;
  }

  @Override
  protected SetorFilterDTO emptyFilter() {
    return new SetorFilterDTO();
  }

  @Override
  protected OptionalLong getEntityResourceVersion(Setor entity) {
    return OptionalLong.of(entity.getVersion());
  }

  @Override
  protected ResourceFilterAccessScope<Setor> resolveResourceFilterAccessScope() {
    var explicit = tenant.explicitOrgao();
    if (explicit.isEmpty()) {
      return ResourceFilterAccessScope.unrestricted();
    }
    int orgao = explicit.getAsInt();
    return ResourceFilterAccessScope.restricted(
        (root, query, cb) -> cb.equal(root.get("empCodigo"), orgao));
  }

  @Override
  protected List<CollectionExportField> exportFields() {
    var datePresentation =
        new CollectionExportFieldPresentation(
            "date", "dd/MM/yyyy", null, "pt-BR", "America/Sao_Paulo", null, null, "Não informado");
    return List.of(
        field("id", "Código do setor", "string"),
        field("empCodigo", "Órgão", "integer"),
        field("nome", "Denominação", "string"),
        field("sigla", "Sigla", "string"),
        field("setorPai", "Setor superior", "string"),
        field("tipoSetor", "Tipo de setor", "string"),
        presentedField("dtini", "Início de vigência", "date", datePresentation),
        presentedField("dtfim", "Término de vigência", "date", datePresentation));
  }

  @Override
  public StatsFieldRegistry getStatsFieldRegistry() {
    return StatsFieldRegistry.builder()
        .labeledGroupByBucket("id", "setor", "nome", Set.of(StatsMetric.COUNT))
        .categoricalGroupByBucket("tipoSetor", "tipoSetor")
        .categoricalGroupByBucket("empCodigo", "empCodigo")
        .temporalTimeSeriesField("dtini", "dtini")
        .build();
  }

  @Override
  public StatsSupportMode getGroupByStatsSupportMode() {
    return StatsSupportMode.AUTO;
  }

  @Override
  public StatsSupportMode getTimeSeriesStatsSupportMode() {
    return StatsSupportMode.AUTO;
  }

  @Override
  public StatsSupportMode getDistributionStatsSupportMode() {
    return StatsSupportMode.AUTO;
  }

  @Override
  public StatsSupportMode getComparisonStatsSupportMode() {
    return StatsSupportMode.AUTO;
  }
}
