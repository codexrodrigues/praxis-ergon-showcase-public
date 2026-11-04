package br.com.praxis.ergonshowcase.service;
import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.domain.RadarOrganogramaChefiaOcupacao;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.mapper.RadarOrganogramaChefiaOcupacaoMapper;
import br.com.praxis.ergonshowcase.repository.RadarOrganogramaChefiaOcupacaoRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Set;
import org.praxisplatform.uischema.exporting.CollectionExportField;
import org.praxisplatform.uischema.exporting.CollectionExportFieldPresentation;
import org.praxisplatform.uischema.stats.StatsFieldRegistry;
import org.praxisplatform.uischema.stats.StatsMetric;
@Service public class RadarOrganogramaChefiaOcupacaoService extends ErgonReadOnlyResourceService<RadarOrganogramaChefiaOcupacao,RadarOrganogramaChefiaOcupacaoResponseDTO,String,RadarOrganogramaChefiaOcupacaoFilterDTO> {
 private final RadarOrganogramaChefiaOcupacaoMapper mapper;
 @Override public org.praxisplatform.uischema.filter.specification.GenericSpecificationsBuilder<RadarOrganogramaChefiaOcupacao> getSpecificationsBuilder() {return new OrganogramaRadarQuery();}
 public RadarOrganogramaChefiaOcupacaoService(RadarOrganogramaChefiaOcupacaoRepository repository,RadarOrganogramaChefiaOcupacaoMapper mapper) {super(repository,RadarOrganogramaChefiaOcupacao.class,"id");this.mapper=mapper;}
 @Override protected RadarOrganogramaChefiaOcupacaoMapper getResourceMapper() {return mapper;}
 @Override protected RadarOrganogramaChefiaOcupacaoFilterDTO emptyFilter() {return new RadarOrganogramaChefiaOcupacaoFilterDTO();}
 @Override protected String resolveResourceKey() {return ApiPaths.KEY_RADAR_ORGANOGRAMA_CHEFIAS_OCUPACAO;}
  @Override
  public StatsFieldRegistry getStatsFieldRegistry() {
    return StatsFieldRegistry.builder()
        .labeledGroupByBucket("id", "id", "nomeSetor", Set.of(StatsMetric.COUNT))
        .categoricalGroupByBucket("situacaoComandoSetor", "situacaoComandoSetor")
        .numericHistogramMeasureField("vagasAutorizadasSetor", "vagasAutorizadasSetor")
        .categoricalTermsBucket("tipoSetor", "tipoSetor")
        .temporalTimeSeriesField("dtini", "dtini")
        .build();
  }

  @Override
  protected List<CollectionExportField> exportFields() {
    var date = new CollectionExportFieldPresentation(
        "date", "dd/MM/yyyy", null, "pt-BR", "America/Sao_Paulo", null, null, "Não informado");
    return List.of(
        field("id", "Identificador", "string"),
        field("setor", "Unidade", "string"),
        field("nomeSetor", "Denominação", "string"),
        field("tipoHierarquia", "Hierarquia", "integer"),
        field("situacaoComandoSetor", "Situação do comando", "string"),
        field("vagasAutorizadasSetor", "Vagas diretas autorizadas", "integer"),
        field("vagasOcupadasSetor", "Vagas ocupadas", "integer"),
        field("escopoApuracao", "Escopo da apuração", "string"),
        presentedField("dtini", "Início de vigência", "date", date),
        presentedField("dtfim", "Término de vigência", "date", date));
  }
}
