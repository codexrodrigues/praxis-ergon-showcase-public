package br.com.praxis.ergonshowcase.mapper;

import br.com.praxis.ergonshowcase.domain.Setor;
import br.com.praxis.ergonshowcase.dto.SetorResponseDTO;
import org.mapstruct.*;
import org.praxisplatform.uischema.mapper.base.ResourceMapper;

/** Projeção de leitura sem acesso ao banco dentro do mapper. */
@Mapper(componentModel = "spring")
public interface SetorMapper extends ResourceMapper<Setor, SetorResponseDTO, Void, Void, String> {
  @Mapping(target = "id", source = "setor")
  @Mapping(target = "referenciaFicha", ignore = true)
  SetorResponseDTO toResponse(Setor entity);

  default String extractId(Setor entity) {
    return entity.getSetor();
  }

  default Setor newEntity(Void unused) {
    throw new UnsupportedOperationException("Cadastro somente de leitura.");
  }

  default void applyUpdate(Setor entity, Void unused) {
    throw new UnsupportedOperationException("Cadastro somente de leitura.");
  }
}
