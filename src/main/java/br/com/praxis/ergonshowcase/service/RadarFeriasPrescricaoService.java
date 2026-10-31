package br.com.praxis.ergonshowcase.service;

import br.com.praxis.ergonshowcase.config.TenantContext;
import br.com.praxis.ergonshowcase.domain.RadarFeriasPrescricao;
import br.com.praxis.ergonshowcase.dto.RadarFeriasPrescricaoFilterDTO;
import br.com.praxis.ergonshowcase.dto.RadarFeriasPrescricaoResponseDTO;
import br.com.praxis.ergonshowcase.mapper.RadarFeriasPrescricaoMapper;
import br.com.praxis.ergonshowcase.repository.RadarFeriasPrescricaoRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import org.praxisplatform.uischema.exporting.CollectionExportCapability;
import org.praxisplatform.uischema.exporting.CollectionExportExecutor;
import org.praxisplatform.uischema.exporting.CollectionExportField;
import org.praxisplatform.uischema.exporting.CollectionExportFieldPresentation;
import org.praxisplatform.uischema.exporting.CollectionExportFormat;
import org.praxisplatform.uischema.exporting.CollectionExportRequest;
import org.praxisplatform.uischema.exporting.CollectionExportResult;
import org.praxisplatform.uischema.exporting.CollectionExportScope;
import org.praxisplatform.uischema.exporting.CollectionExportValueResolver;
import org.praxisplatform.uischema.service.base.AbstractReadOnlyResourceService;
import org.praxisplatform.uischema.service.base.ResourceFilterAccessScope;
import org.praxisplatform.uischema.stats.StatsFieldRegistry;
import org.praxisplatform.uischema.stats.StatsMetric;
import org.praxisplatform.uischema.stats.StatsSupportMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Serviço somente-leitura para a projeção analítica executiva de prescrição, queima de saldo e
 * cobertura de programação futura de férias ({@code RadarFeriasPrescricao}).
 */
@Service
public class RadarFeriasPrescricaoService
    extends AbstractReadOnlyResourceService<
        RadarFeriasPrescricao,
        RadarFeriasPrescricaoResponseDTO,
        Long,
        RadarFeriasPrescricaoFilterDTO> {

  private final RadarFeriasPrescricaoMapper mapper;
  private final TenantContext tenant;

  @Autowired(required = false)
  private CollectionExportExecutor exports;

  public RadarFeriasPrescricaoService(
      RadarFeriasPrescricaoRepository repository, RadarFeriasPrescricaoMapper mapper) {
    this(repository, mapper, new TenantContext());
  }

  @Autowired
  public RadarFeriasPrescricaoService(
      RadarFeriasPrescricaoRepository repository,
      RadarFeriasPrescricaoMapper mapper,
      TenantContext tenant) {
    super(repository, RadarFeriasPrescricao.class);
    this.mapper = mapper;
    this.tenant = tenant != null ? tenant : new TenantContext();
  }

  @Override
  protected RadarFeriasPrescricaoMapper getResourceMapper() {
    return mapper;
  }

  @Override
  protected OptionalLong getEntityResourceVersion(RadarFeriasPrescricao entity) {
    return OptionalLong.of(entity.getVersion() != null ? entity.getVersion() : 0L);
  }

  @Override
  public Sort getDefaultSort() {
    return Sort.by(Sort.Order.asc("dtPrescricao"), Sort.Order.asc("id"));
  }

  @Override
  protected ResourceFilterAccessScope<RadarFeriasPrescricao> resolveResourceFilterAccessScope() {
    int orgao = tenant.orgao();
    return ResourceFilterAccessScope.restricted(
        (root, query, cb) -> cb.equal(root.get("empCodigo"), orgao));
  }

  @Override
  public RadarFeriasPrescricao findEntityById(Long id) {
    RadarFeriasPrescricao entity = super.findEntityById(id);
    if (entity.getEmpCodigo() != null && !java.util.Objects.equals(entity.getEmpCodigo(), tenant.orgao())) {
      throw new ResponseStatusException(
          HttpStatus.UNPROCESSABLE_ENTITY,
          "Órgão do registro no radar de prescrição diverge de X-Tenant.");
    }
    return entity;
  }

  public List<CollectionExportField> exportFields() {
    var datePresentation =
        new CollectionExportFieldPresentation(
            "date", "dd/MM/yyyy", null, "pt-BR", "America/Sao_Paulo", null, null, "Não informado");
    var boolPresentation =
        new CollectionExportFieldPresentation(
            "boolean", null, null, "pt-BR", null, "Sim", "Não", "Não informado");
    var intPresentation =
        new CollectionExportFieldPresentation(
            "number", "0", null, "pt-BR", null, null, null, "Não informado");
    return List.of(
        new CollectionExportField("id", "ID do período", true, true, "integer", "id"),
        new CollectionExportField("empCodigo", "Órgão", true, true, "integer", "empCodigo"),
        new CollectionExportField("orgaoNome", "Nome do órgão", true, true, "string", "orgaoNome"),
        new CollectionExportField("numfunc", "Servidor", true, true, "integer", "numfunc"),
        new CollectionExportField(
            "vinculoMatricula", "Matrícula", true, true, "string", "vinculoMatricula"),
        new CollectionExportField(
            "setorNome", "Setor de lotação", true, true, "string", "setorNome"),
        new CollectionExportField("cargoNome", "Cargo público", true, true, "string", "cargoNome"),
        new CollectionExportField(
            "exercicio",
            "Ano de exercício",
            true,
            true,
            "integer",
            "exercicio",
            null,
            intPresentation),
        new CollectionExportField(
            "dtini", "Início do PA", true, true, "date", "dtini", null, datePresentation),
        new CollectionExportField(
            "dtfim", "Término do PA", true, true, "date", "dtfim", null, datePresentation),
        new CollectionExportField("situacao", "Situação do PA", true, true, "string", "situacao"),
        new CollectionExportField(
            "saldoDiasDisponivel",
            "Saldo disponível",
            true,
            true,
            "integer",
            "saldoDiasDisponivel",
            null,
            intPresentation),
        new CollectionExportField(
            "saldoSemProgramacao",
            "Saldo sem programação",
            true,
            true,
            "integer",
            "saldoSemProgramacao",
            null,
            intPresentation),
        new CollectionExportField(
            "dtPrescricao",
            "Data de prescrição",
            true,
            true,
            "date",
            "dtPrescricao",
            null,
            datePresentation),
        new CollectionExportField(
            "diasAtePrescricao",
            "Dias até prescrição",
            true,
            true,
            "integer",
            "diasAtePrescricao",
            null,
            intPresentation),
        new CollectionExportField(
            "faixaUrgenciaPrescricao",
            "Faixa de urgência",
            true,
            true,
            "string",
            "faixaUrgenciaPrescricao"),
        new CollectionExportField(
            "emRiscoAcumuloPeriodos",
            "Risco acúmulo PAs",
            true,
            true,
            "boolean",
            "emRiscoAcumuloPeriodos",
            null,
            boolPresentation),
        new CollectionExportField(
            "possuiParcelaProgramada",
            "Parcela programada",
            true,
            true,
            "boolean",
            "possuiParcelaProgramada",
            null,
            boolPresentation));
  }

  @Override
  public boolean supportsCollectionExport() {
    return true;
  }

  @Override
  public Optional<CollectionExportCapability> getCollectionExportCapability() {
    return Optional.of(
        new CollectionExportCapability(
            List.of(
                CollectionExportFormat.CSV,
                CollectionExportFormat.JSON,
                CollectionExportFormat.EXCEL),
            List.of(
                CollectionExportScope.AUTO,
                CollectionExportScope.SELECTED,
                CollectionExportScope.FILTERED,
                CollectionExportScope.CURRENT_PAGE,
                CollectionExportScope.ALL),
            Map.of("csv", 1000, "json", 1000, "excel", 1000),
            false));
  }

  @Override
  @Transactional(readOnly = true)
  public CollectionExportResult exportCollection(
      CollectionExportRequest<RadarFeriasPrescricaoFilterDTO> request) {
    if (!getCollectionExportCapability().orElseThrow().formats().contains(request.format())) {
      throw new ResponseStatusException(
          HttpStatus.UNPROCESSABLE_ENTITY, "Formato de exportação não suportado.");
    }
    int limit = request.maxRows() == null ? 1000 : Math.min(request.maxRows(), 1000);
    if (limit < 1) {
      throw new ResponseStatusException(
          HttpStatus.UNPROCESSABLE_ENTITY, "Limite de linhas inválido.");
    }
    var filter = request.filters() == null ? new RadarFeriasPrescricaoFilterDTO() : request.filters();
    var rows = filter(filter, PageRequest.of(0, limit + 1, getDefaultSort()), null).getContent();
    boolean truncated = rows.size() > limit;
    var slice = rows.subList(0, Math.min(rows.size(), limit));
    String effectiveScope =
        request.scope() != null ? request.scope().name().toLowerCase() : "filtered";
    return exports.export(
        request,
        slice,
        exportFields(),
        CollectionExportValueResolver.reflective(),
        Map.of("effectiveMaxRows", limit, "truncated", truncated, "scope", effectiveScope));
  }

  @Override
  public StatsFieldRegistry getStatsFieldRegistry() {
    return StatsFieldRegistry.builder()
        .labeledGroupByBucket(
            "id",
            "id",
            "faixaUrgenciaPrescricao",
            Set.of(StatsMetric.COUNT, StatsMetric.SUM))
        .categoricalGroupByBucket("faixaUrgenciaPrescricao", "faixaUrgenciaPrescricao")
        .categoricalGroupByBucket("setorNome", "setorNome")
        .categoricalGroupByBucket("situacao", "situacao")
        .categoricalGroupByBucket("regimeJuridico", "regimeJuridico")
        .categoricalGroupByBucket("categoria", "categoria")
        .categoricalGroupByBucket("exercicio", "exercicio")
        .categoricalGroupByBucket("emRiscoAcumuloPeriodos", "emRiscoAcumuloPeriodos")
        .categoricalGroupByBucket("possuiParcelaProgramada", "possuiParcelaProgramada")
        .temporalTimeSeriesField("dtPrescricao", "dtPrescricao")
        .temporalTimeSeriesField("dtini", "dtini")
        .temporalTimeSeriesField("dtfim", "dtfim")
        .numericHistogramMeasureField("saldoDiasDisponivel", "saldoDiasDisponivel")
        .numericHistogramMeasureField("saldoSemProgramacao", "saldoSemProgramacao")
        .numericHistogramMeasureField("diasAtePrescricao", "diasAtePrescricao")
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
