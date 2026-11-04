package br.com.praxis.ergonshowcase.service;
import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.domain.CargoSetor;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.mapper.CargoSetorMapper;
import br.com.praxis.ergonshowcase.repository.CargoSetorRepository;
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
public class CargoSetorService extends ErgonResourceService<CargoSetor,CargoSetorResponseDTO,Long,CargoSetorFilterDTO,CreateCargoSetorDTO,UpdateCargoSetorDTO> {
 private final CargoSetorMapper mapper;
  @Override
  public org.praxisplatform.uischema.filter.specification.GenericSpecificationsBuilder<CargoSetor> getSpecificationsBuilder() {
    return SetorFichaReference.relatedFilter(CargoSetorFilterDTO.class, CargoSetorFilterDTO::new,
        CargoSetorFilterDTO::getSetor, CargoSetorFilterDTO::setSetor, "setor");
  }

 public CargoSetorService(CargoSetorRepository repository,CargoSetorMapper mapper) {super(repository,CargoSetor.class,"id");this.mapper=mapper;}
 @Override protected CargoSetorMapper getResourceMapper() {return mapper;}
 @Override protected CargoSetorFilterDTO emptyFilter() {return new CargoSetorFilterDTO();}
 @Override protected String resolveResourceKey() {return ApiPaths.KEY_CARGOS_POR_SETOR;}
  @Override
  public StatsFieldRegistry getStatsFieldRegistry() {
    return StatsFieldRegistry.builder()
        .labeledGroupByBucket("id", "id", "cargoNome", Set.of(StatsMetric.COUNT))
        .categoricalGroupByBucket("tipoControleQuadro", "tipoControleQuadro")
        .numericHistogramMeasureField("lotacaoMaximaAutorizada", "lotacaoMaximaAutorizada")
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
        field("cargoNome", "Cargo", "string"),
        field("tipoControleQuadro", "Controle de quadro", "string"),
        field("lotacaoMaximaAutorizada", "Limite autorizado", "integer"),
        presentedField("abrangeSubordinados", "Abrange subordinados", "boolean", flag),
        field("lotacaoOcupadaAtual", "Lotação ocupada registrada", "integer"),
        presentedField("dtini", "Início de vigência", "date", date),
        presentedField("dtfim", "Término de vigência", "date", date));
  }
 @Override protected void beforeCreate(CreateCargoSetorDTO dto,CargoSetor entity) {throw blocked();}
 @Override protected void beforeUpdate(Long id,CargoSetor entity,UpdateCargoSetorDTO dto) {throw blocked();}
 @Override protected void beforeDelete(Long id,CargoSetor entity) {throw blocked();}
 private AccessDeniedException blocked() {return new AccessDeniedException("Edição organizacional indisponível até a validação dos comandos e regras de domínio.");}
}
