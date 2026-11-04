package br.com.praxis.ergonshowcase.mapper;

import br.com.praxis.ergonshowcase.domain.RadarChefiaSubstituicao;
import br.com.praxis.ergonshowcase.dto.*;
import org.mapstruct.*;
import org.praxisplatform.uischema.mapper.base.ResourceMapper;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class RadarChefiaSubstituicaoMapper implements ResourceMapper<RadarChefiaSubstituicao, RadarChefiaSubstituicaoResponseDTO, Void, Void, Long> {
  @Override
  @Mapping(target = "titularPessoaId", source = "titularId.numfunc")
  @Mapping(target = "titularNome", source = "titular.pessoa.nome")
  public abstract RadarChefiaSubstituicaoResponseDTO toResponse(RadarChefiaSubstituicao entity);

  @Override
  public RadarChefiaSubstituicao newEntity(Void dto) {
    throw new UnsupportedOperationException("A posição de cobertura é uma projeção somente leitura.");
  }

  @Override
  public void applyUpdate(RadarChefiaSubstituicao entity, Void dto) {
    throw new UnsupportedOperationException("A posição de cobertura é uma projeção somente leitura.");
  }

  @Override
  public Long extractId(RadarChefiaSubstituicao entity) {
    return entity.getId();
  }
}
