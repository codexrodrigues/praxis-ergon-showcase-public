package br.com.praxis.ergonshowcase.service;
import br.com.praxis.ergonshowcase.config.TenantContext;
import br.com.praxis.ergonshowcase.domain.*;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.mapper.RadarTempoServicoLicencaEspecialMapper;
import br.com.praxis.ergonshowcase.repository.RadarTempoServicoLicencaEspecialRepository;
import jakarta.persistence.*;
import java.util.*;
import org.praxisplatform.uischema.exporting.*;
import org.praxisplatform.uischema.service.base.ResourceFilterAccessScope;
import org.praxisplatform.uischema.stats.*;
import org.springframework.stereotype.Service;

@Service
public class RadarTempoServicoLicencaEspecialService extends ErgonReadOnlyResourceService<RadarTempoServicoLicencaEspecial, RadarTempoServicoLicencaEspecialResponseDTO, Long, RadarTempoServicoLicencaEspecialFilterDTO> {
  private final RadarTempoServicoLicencaEspecialMapper mapper;
  private final TenantContext tenant;
  public RadarTempoServicoLicencaEspecialService(RadarTempoServicoLicencaEspecialRepository repository, RadarTempoServicoLicencaEspecialMapper mapper, TenantContext tenant) {
    super(repository, RadarTempoServicoLicencaEspecial.class, "id");
    this.mapper = mapper;
    this.tenant = tenant;
  }
  @Override protected RadarTempoServicoLicencaEspecialMapper getResourceMapper() { return mapper; }
  @Override protected RadarTempoServicoLicencaEspecialFilterDTO emptyFilter() { return new RadarTempoServicoLicencaEspecialFilterDTO(); }
  @Override public Optional<String> getDatasetVersion() { return Optional.empty(); }
  @Override protected OptionalLong getEntityResourceVersion(RadarTempoServicoLicencaEspecial entity) { return OptionalLong.empty(); }
  @Override protected RadarTempoServicoLicencaEspecialFilterDTO normalizeOptionSourceFilter(
      org.praxisplatform.uischema.options.OptionSourceDescriptor descriptor,
      org.praxisplatform.uischema.options.service.OptionSourceOperation operation,
      RadarTempoServicoLicencaEspecialFilterDTO filter) {
    return TempoLicencaValidation.scopeFilter(filter == null ? emptyFilter() : filter, tenant);
  }


  @Override protected RadarTempoServicoLicencaEspecial findEntityById(Long id) {
    var entity = super.findEntityById(id);
    TempoLicencaValidation.orgao(tenant, entity.getEmpCodigo());
    return entity;
  }
  @Override protected List<RadarTempoServicoLicencaEspecial> findEntitiesById(Collection<Long> ids) {
    var entities = super.findEntitiesById(ids);
    entities.forEach(entity -> TempoLicencaValidation.orgao(tenant, entity.getEmpCodigo()));
    return entities;
  }
  @Override protected List<RadarTempoServicoLicencaEspecial> findAllEntities() {
    return getRepository().findAll(resolveResourceFilterAccessScope().specification(), getDefaultSort());
  }
  @Override protected List<CollectionExportField> exportFields() {
    var date = new CollectionExportFieldPresentation("date", "dd/MM/yyyy", null, "pt-BR", "America/Sao_Paulo", null, null, "Não informado");
    var number = new CollectionExportFieldPresentation("number", "0", null, "pt-BR", null, null, null, "Não informado");
    var bool = new CollectionExportFieldPresentation("boolean", null, null, "pt-BR", null, "Sim", "Não", "Não informado");
    return List.of(field("id", "Identidade", "integer"), field("descricao", "Assentamento", "string"),
        field("empCodigo", "Órgão", "integer"), field("numfunc", "Servidor", "integer"), field("numvinc", "Vínculo", "integer"),
        presentedField("dtini", "Início", "date", date), presentedField("dtfim", "Término", "date", date),
        field("situacao", "Situação", "string"), field("saldoDias", "Saldo de licença", "integer"),
        presentedField("diasDireito", "Dias de direito", "integer", number),
        presentedField("diasConvertidosDobro", "Dias convertidos", "integer", number),
        presentedField("diasProgramados", "Dias programados", "integer", number),
        presentedField("diasVendidosRegistrados", "Venda avulsa em dias", "integer", number),
        presentedField("dtPrescricao", "Prescrição", "date", date),
        presentedField("diasAtePrescricao", "Dias até prescrição", "integer", number));
  }
  @Override public StatsFieldRegistry getStatsFieldRegistry() {
    return StatsFieldRegistry.builder()
        .groupByBucket("id", "id", Set.of(StatsMetric.COUNT, StatsMetric.SUM))
        .groupByBucket("situacao", "situacao", Set.of(StatsMetric.COUNT, StatsMetric.SUM, StatsMetric.AVG, StatsMetric.MEDIAN, StatsMetric.PERCENTILE_90))
        .temporalTimeSeriesField("dtini", "dtini")
        .numericHistogramMeasureField("saldoDias", "saldoDias").build();
  }
  @Override protected String resolveResourceKey() { return "administracao-pessoal.radar-tempo-servico-licenca-especial"; }

}
