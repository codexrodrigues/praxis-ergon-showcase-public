package br.com.praxis.ergonshowcase.mapper;

import br.com.praxis.ergonshowcase.domain.VagaFuncao;
import br.com.praxis.ergonshowcase.dto.*;
import org.mapstruct.*;
import org.praxisplatform.uischema.mapper.base.ResourceMapper;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class VagaFuncaoMapper implements ResourceMapper<VagaFuncao, VagaFuncaoResponseDTO, CreateVagaFuncaoDTO, UpdateVagaFuncaoDTO, Long> {
  @Override
  public abstract VagaFuncaoResponseDTO toResponse(VagaFuncao entity);

  @Override
  public VagaFuncao newEntity(CreateVagaFuncaoDTO dto) {
    return fromDados(dto);
  }

  @BeanMapping(ignoreByDefault = true)
  @Mapping(target = "empCodigo", source = "empCodigo")
  @Mapping(target = "descricao", source = "descricao")
  @Mapping(target = "dtini", source = "dtini")
  @Mapping(target = "dtfim", source = "dtfim")
  @Mapping(target = "situacao", source = "situacao")
  @Mapping(target = "numeroVaga", source = "numeroVaga")
  @Mapping(target = "funcaoId", source = "funcaoId")
  @Mapping(target = "setorId", source = "setorId")
  @Mapping(target = "origemVaga", source = "origemVaga")
  @Mapping(target = "titularAtualId", ignore = true)
  @Mapping(target = "motivoBloqueio", source = "motivoBloqueio")
  @Mapping(target = "documentoLegalId", source = "documentoLegalId")
  @Mapping(target = "atoPublicacaoId", source = "atoPublicacaoId")
  @Mapping(target = "observacoes", source = "observacoes")
  @Mapping(target = "customFields", source = "customFields")
  protected abstract VagaFuncao fromDados(VagaFuncaoDados dto);

  @Override
  @InheritConfiguration(name = "fromDados")
  public abstract void applyUpdate(@MappingTarget VagaFuncao entity, UpdateVagaFuncaoDTO dto);

  @Override
  public Long extractId(VagaFuncao entity) {
    return entity.getId();
  }
}
