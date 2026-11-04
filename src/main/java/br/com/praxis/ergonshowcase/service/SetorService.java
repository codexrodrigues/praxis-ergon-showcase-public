package br.com.praxis.ergonshowcase.service;

import br.com.praxis.ergonshowcase.config.TenantContext;
import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.domain.Setor;
import br.com.praxis.ergonshowcase.dto.HistoricoSetorFilterDTO;
import br.com.praxis.ergonshowcase.dto.HistoricoSetorResponseDTO;
import br.com.praxis.ergonshowcase.dto.RadarOrganogramaChefiaOcupacaoFilterDTO;
import br.com.praxis.ergonshowcase.dto.SetorFilterDTO;
import br.com.praxis.ergonshowcase.dto.SetorResponseDTO;
import br.com.praxis.ergonshowcase.mapper.SetorMapper;
import br.com.praxis.ergonshowcase.repository.SetorRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.OptionalLong;
import java.util.Set;
import org.praxisplatform.uischema.exporting.CollectionExportField;
import org.praxisplatform.uischema.exporting.CollectionExportFieldPresentation;
import org.praxisplatform.uischema.concurrency.ResourceRepresentationResult;
import org.praxisplatform.uischema.stats.StatsFieldRegistry;
import org.praxisplatform.uischema.stats.StatsMetric;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Fronteira estritamente de leitura para referências e inspeção contextual de unidades organizacionais. */
@Service
public class SetorService
    extends ErgonReadOnlyResourceService<Setor, SetorResponseDTO, String, SetorFilterDTO> {
  private final SetorMapper mapper;
  private final TenantContext tenant;
  @Autowired private HistoricoSetorService history;

  private boolean temporal(SetorFilterDTO f) {
    return f!=null&&(f.getDataReferencia()!=null||f.getTipoHierarquia()!=null||f.getCaminhoSubarvore()!=null||f.getNivelHierarquico()!=null);
  }
  private br.com.praxis.ergonshowcase.dto.HistoricoSetorFilterDTO historicalFilter(SetorFilterDTO f) {
    var result=new br.com.praxis.ergonshowcase.dto.HistoricoSetorFilterDTO();
    org.springframework.beans.BeanUtils.copyProperties(f,result);
    result.setSemEncerramento(f.getVigentes());result.setComEncerramento(f.getEncerrados());
    if(result.getDataReferencia()==null)result.setDataReferencia(java.time.LocalDate.now());
    return result;
  }
  private SetorResponseDTO historicalResponse(HistoricoSetorResponseDTO period, HistoricoSetorFilterDTO filter) {
    var dto = new SetorResponseDTO();
    org.springframework.beans.BeanUtils.copyProperties(period, dto, "id");
    dto.setId(period.getSetor());
    int hierarchy = filter.getTipoHierarquia() == null ? 0 : filter.getTipoHierarquia();
    dto.setReferenciaFicha(new SetorFichaReference(period.getId(), filter.getDataReferencia(), hierarchy).encode());
    return dto;
  }

  public void bindRadarSubtree(RadarOrganogramaChefiaOcupacaoFilterDTO filter, SetorResponseDTO parent) {
    filter.setRaizSubarvore(parent.getReferenciaFicha() == null ? parent.getId() : parent.getReferenciaFicha());
  }

  @Override
  @Transactional(readOnly = true)
  public ResourceRepresentationResult<SetorResponseDTO> findById(String id) {
    var reference = SetorFichaReference.parse(id);
    if (reference == null) {
      return super.findById(id);
    }
    LocalDate date = reference.date();
    var periods = getEntityManager().createQuery("""
        select h.id from HistoricoSetor h
        where h.id = :id and h.empCodigo = :org and h.dtini <= :date
          and (h.dtfim is null or h.dtfim >= :date)
        """, Long.class)
        .setParameter("id", reference.period())
        .setParameter("org", tenant.orgao())
        .setParameter("date", date)
        .getResultList();
    var historical = history.findById(periods.stream().findFirst().orElseThrow(this::getNotFoundException));
    var filter = new HistoricoSetorFilterDTO();
    filter.setDataReferencia(date);
    filter.setTipoHierarquia(reference.hierarchy());
    new OrganogramaTemporalQuery(getEntityManager(), tenant).enrich(List.of(historical.body()), filter);
    var dto = historicalResponse(historical.body(), filter);
    enrichHistorical(List.of(dto), date);
    // O corpo também depende dos períodos de ancestrais e da parametrização na data.
    return new ResourceRepresentationResult<>(dto, OptionalLong.empty());
  }
  @SuppressWarnings("unchecked")
  private void enrichHistorical(java.util.List<SetorResponseDTO> rows,java.time.LocalDate date) {
    if(rows.isEmpty())return;
    var byCode=rows.stream().collect(java.util.stream.Collectors.toMap(SetorResponseDTO::getId,java.util.function.Function.identity()));
    java.util.List<Object[]> facts=getEntityManager().createNativeQuery("""
      select h.setor,min(h.dtini),e.centro_custo,e.cargo_chefe_id,e.funcao_chefe_id
      from historico_setores h left join setores_ergon e on e.emp_codigo=h.emp_codigo and e.setor=h.setor
        and e.dtini<=:date and (e.dtfim is null or e.dtfim>=:date)
      where h.emp_codigo=:org and h.setor in (:codes)
      group by h.setor,e.centro_custo,e.cargo_chefe_id,e.funcao_chefe_id
      """).setParameter("org",tenant.orgao()).setParameter("date",date).setParameter("codes",byCode.keySet()).getResultList();
    for(Object[] fact:facts) {
      var dto=byCode.get((String)fact[0]);
      dto.setDataCriacaoOriginal(fact[1] instanceof java.sql.Date d?d.toLocalDate():(java.time.LocalDate)fact[1]);
      dto.setCentroCustoPadrao((String)fact[2]);
      dto.setCargoChefeId(fact[3]==null?null:((Number)fact[3]).intValue());
      dto.setFuncaoChefeId(fact[4]==null?null:((Number)fact[4]).longValue());
    }
  }
  private org.springframework.data.domain.Sort historicalSort(org.springframework.data.domain.Sort sort) {
    if(sort==null||sort.isUnsorted())return org.springframework.data.domain.Sort.by("setor");
    return org.springframework.data.domain.Sort.by(sort.stream().map(o->"id".equals(o.getProperty())?o.withProperty("setor"):o).toList());
  }
  private org.springframework.data.domain.Pageable historicalPage(org.springframework.data.domain.Pageable page) {
    var sort=historicalSort(page.getSort());
    return page.isPaged()?org.springframework.data.domain.PageRequest.of(page.getPageNumber(),page.getPageSize(),sort):org.springframework.data.domain.Pageable.unpaged(sort);
  }
  @Override @org.springframework.transaction.annotation.Transactional(readOnly=true)
  public org.springframework.data.domain.Page<org.praxisplatform.uischema.dto.OptionDTO<String>> filterOptions(SetorFilterDTO filter,org.springframework.data.domain.Pageable page) {
    if(!temporal(filter))return super.filterOptions(filter,page);
    return filter(filter,page,java.util.List.of()).map(d->new org.praxisplatform.uischema.dto.OptionDTO<>(d.getId(),d.getNome(),java.util.Map.of()));
  }
  /** Superfícies analíticas e option-source continuam correntes; nunca aceitam silenciosamente critérios históricos. */
  @Override public org.praxisplatform.uischema.filter.specification.GenericSpecificationsBuilder<Setor> getSpecificationsBuilder() {
    return new org.praxisplatform.uischema.filter.specification.GenericSpecificationsBuilder<>() {
      @Override public <F extends org.praxisplatform.uischema.filter.dto.GenericFilterDTO> org.praxisplatform.uischema.filter.specification.GenericSpecification<Setor> buildSpecification(F f,org.springframework.data.domain.Pageable p,
          org.praxisplatform.uischema.customfields.CustomFieldDefinitionProvider d,org.praxisplatform.uischema.customfields.ResourceTenantContextResolver t,String k) {
        if(f instanceof SetorFilterDTO sf && temporal(sf))throw new org.praxisplatform.uischema.rest.exceptionhandler.exception.InvalidFilterPayloadException("Esta superfície consulta o diretório corrente. Para análise histórica use historico-setores ou radar-organograma-chefias-ocupacao.");
        return super.buildSpecification(f,p,d,t,k);
      }
    };
  }
  private java.util.List<Long> historicalIds(java.util.Collection<String> codes,java.time.LocalDate date) {
    if(codes==null||codes.isEmpty())return java.util.List.of();
    if(codes.stream().anyMatch(java.util.Objects::isNull))throw invalidHistoricalIds();
    var result=getEntityManager().createQuery("select h.id from HistoricoSetor h where h.empCodigo=:org and h.setor in :codes and h.dtini<=:date and (h.dtfim is null or h.dtfim>=:date)",Long.class)
      .setParameter("org",tenant.orgao()).setParameter("codes",codes).setParameter("date",date).getResultList();
    return result;
  }
  private org.springframework.web.server.ResponseStatusException invalidHistoricalIds() {
    return new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Referências indisponíveis no órgão e na data consultados.");
  }
  @Override @org.springframework.transaction.annotation.Transactional(readOnly=true)
  public org.springframework.data.domain.Page<SetorResponseDTO> filter(SetorFilterDTO f,org.springframework.data.domain.Pageable page,java.util.Collection<String> includeIds) {
    if(!temporal(f))return super.filter(f,page,includeIds);
    var hf=historicalFilter(f);
    var ids=historicalIds(includeIds,hf.getDataReferencia());
    if(includeIds!=null&&!includeIds.isEmpty()&&ids.size()!=new java.util.HashSet<>(includeIds).size())throw invalidHistoricalIds();
    var result=history.filter(hf,historicalPage(page),ids).map(row -> historicalResponse(row, hf));
    enrichHistorical(result.getContent(),hf.getDataReferencia());return result;
  }
  @Override @org.springframework.transaction.annotation.Transactional(readOnly=true)
  public org.praxisplatform.uischema.dto.CursorPage<SetorResponseDTO> filterByCursor(SetorFilterDTO f,org.springframework.data.domain.Sort sort,String after,String before,int size) {
    if(!temporal(f))return super.filterByCursor(f,sort,after,before,size);
    var hf=historicalFilter(f);var result=history.filterByCursor(hf,historicalSort(sort),after,before,size);
    var rows=result.content().stream().map(row -> historicalResponse(row, hf)).toList();enrichHistorical(rows,hf.getDataReferencia());
    return new org.praxisplatform.uischema.dto.CursorPage<>(rows,result.next(),result.prev(),result.size());
  }
  @Override @org.springframework.transaction.annotation.Transactional(readOnly=true)
  public java.util.OptionalLong locate(SetorFilterDTO f,org.springframework.data.domain.Sort sort,String id) {
    if(!temporal(f))return super.locate(f,sort,id);
    var hf=historicalFilter(f);var ids=historicalIds(java.util.List.of(id),hf.getDataReferencia());
    return ids.isEmpty()?java.util.OptionalLong.empty():history.locate(hf,historicalSort(sort),ids.getFirst());
  }

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
        .numericHistogramMeasureField("quantidadeSubordinadosDiretos", "quantidadeSubordinadosDiretos")
        .temporalTimeSeriesField("dtini", "dtini")
        .build();
  }

  @Override protected String resolveResourceKey() { return ApiPaths.KEY_SETORES; }

}
