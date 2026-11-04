package br.com.praxis.ergonshowcase.service;
import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.domain.GrupoSetor;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.mapper.GrupoSetorMapper;
import br.com.praxis.ergonshowcase.repository.GrupoSetorRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Set;
import org.praxisplatform.uischema.exporting.CollectionExportField;
import org.praxisplatform.uischema.exporting.CollectionExportFieldPresentation;
import org.praxisplatform.uischema.stats.StatsFieldRegistry;
import org.praxisplatform.uischema.stats.StatsMetric;
import org.springframework.security.access.AccessDeniedException;
/** Contrato mutável preparado; execução exige os gates transacionais das Ondas 2–3. */
@Service
public class GrupoSetorService extends ErgonResourceService<GrupoSetor,GrupoSetorResponseDTO,Long,GrupoSetorFilterDTO,CreateGrupoSetorDTO,UpdateGrupoSetorDTO> {
 private final GrupoSetorMapper mapper;
 public GrupoSetorService(GrupoSetorRepository repository,GrupoSetorMapper mapper) {super(repository,GrupoSetor.class,"id");this.mapper=mapper;}
 @Override protected GrupoSetorMapper getResourceMapper() {return mapper;}
 @Override protected GrupoSetorFilterDTO emptyFilter() {return new GrupoSetorFilterDTO();}
 @Override protected String resolveResourceKey() {return ApiPaths.KEY_GRUPOS_SETORES;}
  @Override
  public StatsFieldRegistry getStatsFieldRegistry() {
    return StatsFieldRegistry.builder()
        .labeledGroupByBucket("id", "id", "nome", Set.of(StatsMetric.COUNT))
        .categoricalGroupByBucket("tipoAgrupamento", "tipoAgrupamento")
        .categoricalTermsBucket("ufAbrangencia", "ufAbrangencia")
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
        field("sigla", "Sigla", "string"),
        field("nome", "Denominação", "string"),
        field("tipoAgrupamento", "Agrupamento", "string"),
        field("setorSede", "Setor sede", "string"),
        field("ufAbrangencia", "UF de abrangência", "string"),
        presentedField("ativo", "Ativo", "boolean", flag),
        presentedField("dtini", "Início de vigência", "date", date),
        presentedField("dtfim", "Término de vigência", "date", date));
  }
 @Override protected void beforeCreate(CreateGrupoSetorDTO dto,GrupoSetor entity) {throw blocked();}
 @Override protected void beforeUpdate(Long id,GrupoSetor entity,UpdateGrupoSetorDTO dto) {throw blocked();}
 @Override protected void beforeDelete(Long id,GrupoSetor entity) {throw blocked();}
 private AccessDeniedException blocked() {return new AccessDeniedException("Edição organizacional indisponível até a validação dos comandos e regras de domínio.");}
}
