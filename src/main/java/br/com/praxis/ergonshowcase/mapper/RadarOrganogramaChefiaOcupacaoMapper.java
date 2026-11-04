package br.com.praxis.ergonshowcase.mapper;
import br.com.praxis.ergonshowcase.domain.RadarOrganogramaChefiaOcupacao;
import br.com.praxis.ergonshowcase.dto.RadarOrganogramaChefiaOcupacaoResponseDTO;
import org.mapstruct.Mapper;
import org.praxisplatform.uischema.mapper.base.ResourceMapper;
@Mapper(componentModel="spring")
public interface RadarOrganogramaChefiaOcupacaoMapper extends ResourceMapper<RadarOrganogramaChefiaOcupacao,RadarOrganogramaChefiaOcupacaoResponseDTO,Void,Void,String> {
 RadarOrganogramaChefiaOcupacaoResponseDTO toResponse(RadarOrganogramaChefiaOcupacao entity);
 default String extractId(RadarOrganogramaChefiaOcupacao entity) {return entity.getId();}
 default RadarOrganogramaChefiaOcupacao newEntity(Void dto) {throw new UnsupportedOperationException("Projeção de leitura");}
 default void applyUpdate(RadarOrganogramaChefiaOcupacao entity,Void dto) {throw new UnsupportedOperationException("Projeção de leitura");}
}
