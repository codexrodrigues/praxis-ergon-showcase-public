package br.com.praxis.ergonshowcase.service;
import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.domain.SetorParametrizacaoErgon;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.mapper.SetorParametrizacaoErgonMapper;
import br.com.praxis.ergonshowcase.repository.SetorParametrizacaoErgonRepository;
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
public class SetorParametrizacaoErgonService extends ErgonResourceService<SetorParametrizacaoErgon,SetorParametrizacaoErgonResponseDTO,Long,SetorParametrizacaoErgonFilterDTO,CreateSetorParametrizacaoErgonDTO,UpdateSetorParametrizacaoErgonDTO> {
 private final SetorParametrizacaoErgonMapper mapper;
  @Override
  public org.praxisplatform.uischema.filter.specification.GenericSpecificationsBuilder<SetorParametrizacaoErgon> getSpecificationsBuilder() {
    return SetorFichaReference.relatedFilter(SetorParametrizacaoErgonFilterDTO.class, SetorParametrizacaoErgonFilterDTO::new,
        SetorParametrizacaoErgonFilterDTO::getSetor, SetorParametrizacaoErgonFilterDTO::setSetor, "setor");
  }

 public SetorParametrizacaoErgonService(SetorParametrizacaoErgonRepository repository,SetorParametrizacaoErgonMapper mapper) {super(repository,SetorParametrizacaoErgon.class,"id");this.mapper=mapper;}
 @Override protected SetorParametrizacaoErgonMapper getResourceMapper() {return mapper;}
 @Override protected SetorParametrizacaoErgonFilterDTO emptyFilter() {return new SetorParametrizacaoErgonFilterDTO();}
 @Override protected String resolveResourceKey() {return ApiPaths.KEY_SETORES_ERGON;}
  @Override
  public StatsFieldRegistry getStatsFieldRegistry() {
    return StatsFieldRegistry.builder()
        .labeledGroupByBucket("id", "id", "setorNome", Set.of(StatsMetric.COUNT))
        .categoricalGroupByBucket("tipoAcesso", "tipoAcesso")
        .categoricalTermsBucket("grauRiscoAmbiental", "grauRiscoAmbiental")
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
        field("setorNome", "Denominação", "string"),
        field("centroCusto", "Centro de custo", "string"),
        field("tipoAcesso", "Tipo de acesso", "string"),
        field("grauRiscoAmbiental", "Grau de risco", "string"),
        presentedField("exigeEpiObrigatorio", "EPI obrigatório", "boolean", flag),
        presentedField("dtini", "Início de vigência", "date", date),
        presentedField("dtfim", "Término de vigência", "date", date));
  }
 @Override protected void beforeCreate(CreateSetorParametrizacaoErgonDTO dto,SetorParametrizacaoErgon entity) {throw blocked();}
 @Override protected void beforeUpdate(Long id,SetorParametrizacaoErgon entity,UpdateSetorParametrizacaoErgonDTO dto) {throw blocked();}
 @Override protected void beforeDelete(Long id,SetorParametrizacaoErgon entity) {throw blocked();}
 private AccessDeniedException blocked() {return new AccessDeniedException("Edição organizacional indisponível até a validação dos comandos e regras de domínio.");}
}
