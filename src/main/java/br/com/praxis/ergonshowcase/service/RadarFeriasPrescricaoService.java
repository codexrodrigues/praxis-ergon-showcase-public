package br.com.praxis.ergonshowcase.service;

import br.com.praxis.ergonshowcase.config.TenantContext;
import br.com.praxis.ergonshowcase.domain.RadarFeriasPrescricao;
import br.com.praxis.ergonshowcase.dto.RadarFeriasPrescricaoFilterDTO;
import br.com.praxis.ergonshowcase.dto.RadarFeriasPrescricaoResponseDTO;
import br.com.praxis.ergonshowcase.mapper.RadarFeriasPrescricaoMapper;
import br.com.praxis.ergonshowcase.repository.RadarFeriasPrescricaoRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.stream.Collectors;
import org.praxisplatform.uischema.dto.CursorPage;
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
import org.praxisplatform.uischema.util.CursorEncoder;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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
  public boolean supportsCursorPagination() {
    return true;
  }

  private Specification<RadarFeriasPrescricao> filtered(RadarFeriasPrescricaoFilterDTO filter) {
    return resolveResourceFilterAccessScope()
        .specification()
        .and(getSpecificationsBuilder().buildSpecification(filter, Pageable.unpaged()).spec());
  }

  private Sort identitySort(Sort sort) {
    if (sort == null || sort.isUnsorted() || sort.equals(getDefaultSort())) {
      return Sort.by(Sort.Order.asc("id"));
    }
    var orders = sort.toList();
    if (orders.size() == 1 && "id".equals(orders.getFirst().getProperty())) {
      return sort;
    }
    throw new ResponseStatusException(
        HttpStatus.UNPROCESSABLE_ENTITY, "Cursor e localização exigem ordenação pela identidade.");
  }

  private String token(RadarFeriasPrescricao e) {
    return CursorEncoder.BASE64_URL.encode(String.valueOf(e.getId()));
  }

  @Override
  protected CursorPage<RadarFeriasPrescricao> filterEntitiesByCursor(
      RadarFeriasPrescricaoFilterDTO filter,
      Sort requested,
      String after,
      String before,
      int size) {
    if (size < 1 || size > 200 || (after != null && before != null)) {
      throw new ResponseStatusException(
          HttpStatus.UNPROCESSABLE_ENTITY, "Tamanho inválido ou cursores conflitantes.");
    }
    Sort sort = identitySort(requested);
    boolean backwards = before != null;
    var spec = filtered(filter == null ? new RadarFeriasPrescricaoFilterDTO() : filter);
    String cursor = backwards ? before : after;
    if (cursor != null) {
      try {
        Long idValue = Long.valueOf(CursorEncoder.BASE64_URL.decode(cursor));
        boolean greater = sort.toList().getFirst().isAscending() != backwards;
        spec =
            spec.and(
                (root, q, cb) ->
                    greater
                        ? cb.greaterThan(root.get("id"), idValue)
                        : cb.lessThan(root.get("id"), idValue));
      } catch (IllegalArgumentException e) {
        throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Cursor inválido.");
      }
    }
    Sort querySort = backwards ? Sort.by(sort.stream().map(Sort.Order::reverse).toList()) : sort;
    List<RadarFeriasPrescricao> rows =
        new ArrayList<>(
            getRepository().findAll(spec, PageRequest.of(0, size + 1, querySort)).getContent());
    boolean more = rows.size() > size;
    if (more) rows.removeLast();
    if (backwards) Collections.reverse(rows);
    return new CursorPage<>(
        rows,
        rows.isEmpty()
            ? null
            : (!backwards && more || backwards && cursor != null ? token(rows.getLast()) : null),
        rows.isEmpty()
            ? null
            : (backwards && more || !backwards && cursor != null ? token(rows.getFirst()) : null),
        size);
  }

  @Override
  @Transactional(readOnly = true)
  public OptionalLong locate(RadarFeriasPrescricaoFilterDTO filter, Sort requested, Long id) {
    Sort sort = identitySort(requested);
    var entity = getRepository().findById(id);
    if (entity.isEmpty()) return OptionalLong.empty();
    var effectiveFilter = filter == null ? new RadarFeriasPrescricaoFilterDTO() : filter;
    Specification<RadarFeriasPrescricao> match = (root, q, cb) -> cb.equal(root.get("id"), id);
    if (!getRepository().exists(filtered(effectiveFilter).and(match))) return OptionalLong.empty();
    boolean greater = sort.toList().getFirst().isDescending();
    Specification<RadarFeriasPrescricao> bound =
        (root, q, cb) ->
            greater ? cb.greaterThan(root.get("id"), id) : cb.lessThan(root.get("id"), id);
    return OptionalLong.of(getRepository().count(filtered(effectiveFilter).and(bound)));
  }

  @Override
  protected ResourceFilterAccessScope<RadarFeriasPrescricao> resolveResourceFilterAccessScope() {
    var explicitOrgao = tenant.explicitOrgao();
    if (explicitOrgao.isEmpty()) {
      return ResourceFilterAccessScope.unrestricted();
    }
    int orgao = explicitOrgao.getAsInt();
    return ResourceFilterAccessScope.restricted(
        (root, query, cb) -> cb.equal(root.get("empCodigo"), orgao));
  }

  @Override
  public RadarFeriasPrescricao findEntityById(Long id) {
    RadarFeriasPrescricao entity = super.findEntityById(id);
    if (entity.getEmpCodigo() != null
        && !java.util.Objects.equals(entity.getEmpCodigo(), tenant.orgao())) {
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
    CollectionExportScope requestedScope =
        request.scope() != null ? request.scope() : CollectionExportScope.AUTO;
    boolean hasSelection =
        request.selection() != null
            && request.selection().selectedKeys() != null
            && !request.selection().selectedKeys().isEmpty();
    CollectionExportScope effectiveScope =
        requestedScope == CollectionExportScope.AUTO
            ? (hasSelection
                ? CollectionExportScope.SELECTED
                : (request.pagination() != null
                    ? CollectionExportScope.CURRENT_PAGE
                    : CollectionExportScope.FILTERED))
            : requestedScope;
    var filter =
        effectiveScope == CollectionExportScope.ALL || request.filters() == null
            ? new RadarFeriasPrescricaoFilterDTO()
            : request.filters();
    List<RadarFeriasPrescricaoResponseDTO> slice;
    boolean truncated;
    if (effectiveScope == CollectionExportScope.CURRENT_PAGE && request.pagination() != null) {
      int pageNum =
          request.pagination().pageIndex() != null
              ? Math.max(0, request.pagination().pageIndex())
              : (request.pagination().pageNumber() != null
                  ? Math.max(0, request.pagination().pageNumber())
                  : 0);
      int pageSize =
          request.pagination().pageSize() != null && request.pagination().pageSize() > 0
              ? Math.min(request.pagination().pageSize(), limit)
              : limit;
      var pageResult = filter(filter, PageRequest.of(pageNum, pageSize, getDefaultSort()), null);
      slice = pageResult.getContent();
      truncated = pageResult.hasNext() && slice.size() >= limit;
    } else {
      var rows = filter(filter, PageRequest.of(0, limit + 1, getDefaultSort()), null).getContent();
      if (effectiveScope == CollectionExportScope.SELECTED && hasSelection) {
        Set<String> selectedIds =
            request.selection().selectedKeys().stream()
                .map(String::valueOf)
                .collect(Collectors.toSet());
        String keyProp =
            request.selection().keyField() != null && !request.selection().keyField().isBlank()
                ? request.selection().keyField().trim()
                : "id";
        rows =
            rows.stream()
                .filter(
                    row -> {
                      var bw = new BeanWrapperImpl(row);
                      Object keyVal =
                          bw.isReadableProperty(keyProp)
                              ? bw.getPropertyValue(keyProp)
                              : (bw.isReadableProperty("id") ? bw.getPropertyValue("id") : null);
                      return keyVal != null && selectedIds.contains(String.valueOf(keyVal));
                    })
                .toList();
      }
      truncated = rows.size() > limit;
      slice = rows.subList(0, Math.min(rows.size(), limit));
    }
    String scopeName = effectiveScope.name().toLowerCase(Locale.ROOT);
    return exports.export(
        request,
        slice,
        exportFields(),
        CollectionExportValueResolver.reflective(),
        Map.of("effectiveMaxRows", limit, "truncated", truncated, "scope", scopeName));
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
