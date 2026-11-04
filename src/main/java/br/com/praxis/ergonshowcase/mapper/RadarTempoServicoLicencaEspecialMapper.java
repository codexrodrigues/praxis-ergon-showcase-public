package br.com.praxis.ergonshowcase.mapper;
import br.com.praxis.ergonshowcase.domain.*;
import br.com.praxis.ergonshowcase.dto.*;
import org.mapstruct.*;
import org.praxisplatform.uischema.mapper.base.ResourceMapper;
@Mapper(imports = VinculoId.class, componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface RadarTempoServicoLicencaEspecialMapper extends ResourceMapper<RadarTempoServicoLicencaEspecial, RadarTempoServicoLicencaEspecialResponseDTO, Void, Void, Long> {
  @Mapping(target = "orgaoNome", source = "orgao.nome")
  @Mapping(target = "pessoaNome", source = "pessoa.nome")
  @Mapping(target = "vinculoMatricula", source = "vinculo.matric")
  @Mapping(target = "documentoLegalNumero", source = "documentoLegal.numeroDocumento")
  @Mapping(target = "atoPublicacaoNumero", source = "atoPublicacao.numeroDocumento")
  @Mapping(target = "vinculoId", expression = "java(entity.getNumfunc() == null || entity.getNumvinc() == null ? null : new VinculoId(entity.getNumfunc(), entity.getNumvinc()))")
  RadarTempoServicoLicencaEspecialResponseDTO toResponse(RadarTempoServicoLicencaEspecial entity);
  default RadarTempoServicoLicencaEspecial newEntity(Void dto) { throw new UnsupportedOperationException("Radar somente leitura"); }
  default void applyUpdate(RadarTempoServicoLicencaEspecial entity, Void dto) { throw new UnsupportedOperationException("Radar somente leitura"); }
  default Long extractId(RadarTempoServicoLicencaEspecial entity) { return entity.getId(); }
}
