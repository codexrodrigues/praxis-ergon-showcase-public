package br.com.praxis.ergonshowcase.service;
import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.domain.HistoricoSetor;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.mapper.HistoricoSetorMapper;
import br.com.praxis.ergonshowcase.repository.HistoricoSetorRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Set;
import org.praxisplatform.uischema.exporting.CollectionExportField;
import org.praxisplatform.uischema.exporting.CollectionExportFieldPresentation;
import org.praxisplatform.uischema.stats.StatsFieldRegistry;
import org.praxisplatform.uischema.stats.StatsMetric;
import org.springframework.security.access.AccessDeniedException;
/** Contrato mutÃ¡vel preparado; execuÃ§Ã£o exige os gates transacionais das Ondas 2â€“3. */
@Service
public class HistoricoSetorService extends ErgonResourceService<HistoricoSetor,HistoricoSetorResponseDTO,Long,HistoricoSetorFilterDTO,CreateHistoricoSetorDTO,UpdateHistoricoSetorDTO> {
 private final HistoricoSetorMapper mapper;
 @org.springframework.beans.factory.annotation.Autowired
 private br.com.praxis.ergonshowcase.config.TenantContext temporalTenant;
 private OrganogramaTemporalQuery temporalQuery() {return new OrganogramaTemporalQuery(getEntityManager(),temporalTenant);}
 @Override protected void enrichResponses(java.util.List<HistoricoSetorResponseDTO> rows) {
   super.enrichResponses(rows);temporalQuery().enrich(rows,null);
 }
 @Override public org.praxisplatform.uischema.filter.specification.GenericSpecificationsBuilder<HistoricoSetor> getSpecificationsBuilder() {return temporalQuery();}
 @Override @org.springframework.transaction.annotation.Transactional(readOnly=true)
 public org.springframework.data.domain.Page<HistoricoSetorResponseDTO> filter(HistoricoSetorFilterDTO filter,org.springframework.data.domain.Pageable page,java.util.Collection<Long> includeIds) {
   var result=super.filter(filter,page,includeIds);temporalQuery().enrich(result.getContent(),filter);return result;
 }
 @Override @org.springframework.transaction.annotation.Transactional(readOnly=true)
 public org.praxisplatform.uischema.dto.CursorPage<HistoricoSetorResponseDTO> filterByCursor(HistoricoSetorFilterDTO filter,org.springframework.data.domain.Sort sort,String after,String before,int size) {
   var result=super.filterByCursor(filter,sort,after,before,size);temporalQuery().enrich(result.content(),filter);return result;
 }
 public HistoricoSetorService(HistoricoSetorRepository repository,HistoricoSetorMapper mapper) {super(repository,HistoricoSetor.class,"id");this.mapper=mapper;}
 @Override protected HistoricoSetorMapper getResourceMapper() {return mapper;}
 @Override protected HistoricoSetorFilterDTO emptyFilter() {return new HistoricoSetorFilterDTO();}
 @Override protected String resolveResourceKey() {return ApiPaths.KEY_HISTORICO_SETORES;}
  @Override
  public StatsFieldRegistry getStatsFieldRegistry() {
    return StatsFieldRegistry.builder()
        .labeledGroupByBucket("id", "id", "nome", Set.of(StatsMetric.COUNT))
        .categoricalGroupByBucket("tipoSetor", "tipoSetor")
        .categoricalTermsBucket("ufLotacao", "ufLotacao")
        .temporalTimeSeriesField("dtini", "dtini")
        .build();
  }

  @Override
  protected List<CollectionExportField> exportFields() {
    var date = new CollectionExportFieldPresentation(
        "date", "dd/MM/yyyy", null, "pt-BR", "America/Sao_Paulo", null, null, "Não informado");
    var flag = new CollectionExportFieldPresentation(
        "boolean", null, null, "pt-BR", null, "Sim", "Não", "Não informado");
    return List.of(
        field("id", "Identificador", "integer"),
        field("setor", "Unidade", "string"),
        field("nome", "Denominação", "string"),
        field("tipoSetor", "Tipo de unidade", "string"),
        field("setorPai", "Superior oficial", "string"),
        field("ufLotacao", "UF", "string"),
        presentedField("extinto", "Extinto", "boolean", flag),
        presentedField("dtini", "Início de vigência", "date", date),
        presentedField("dtfim", "Término de vigência", "date", date));
  }
 @Override protected void beforeCreate(CreateHistoricoSetorDTO dto,HistoricoSetor entity) {throw blocked();}
 @Override protected void beforeUpdate(Long id,HistoricoSetor entity,UpdateHistoricoSetorDTO dto) {throw blocked();}
 @Override protected void beforeDelete(Long id,HistoricoSetor entity) {throw blocked();}
 private AccessDeniedException blocked() {return new AccessDeniedException("EdiÃ§Ã£o organizacional indisponÃ­vel atÃ© a validaÃ§Ã£o dos comandos e regras de domÃ­nio.");}
}
