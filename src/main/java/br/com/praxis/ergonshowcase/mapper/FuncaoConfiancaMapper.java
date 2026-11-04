package br.com.praxis.ergonshowcase.mapper;

import br.com.praxis.ergonshowcase.domain.FuncaoConfianca;
import br.com.praxis.ergonshowcase.dto.*;
import org.mapstruct.*;
import org.praxisplatform.uischema.mapper.base.ResourceMapper;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class FuncaoConfiancaMapper implements ResourceMapper<FuncaoConfianca, FuncaoConfiancaResponseDTO, CreateFuncaoConfiancaDTO, UpdateFuncaoConfiancaDTO, Long> {
  @Override
  public abstract FuncaoConfiancaResponseDTO toResponse(FuncaoConfianca entity);

  @Override
  public FuncaoConfianca newEntity(CreateFuncaoConfiancaDTO dto) {
    return fromDados(dto);
  }

  @BeanMapping(ignoreByDefault = true)
  @Mapping(target = "empCodigo", source = "empCodigo")
  @Mapping(target = "descricao", source = "descricao")
  @Mapping(target = "dtini", source = "dtini")
  @Mapping(target = "dtfim", source = "dtfim")
  @Mapping(target = "situacao", source = "situacao")
  @Mapping(target = "codigo", source = "codigo")
  @Mapping(target = "categoria", source = "categoria")
  @Mapping(target = "simbolo", source = "simbolo")
  @Mapping(target = "controleVaga", source = "controleVaga")
  @Mapping(target = "dtInicioContrVaga", source = "dtInicioContrVaga")
  @Mapping(target = "permiteSubstituicao", source = "permiteSubstituicao")
  @Mapping(target = "tipoSubstituicao", source = "tipoSubstituicao")
  @Mapping(target = "diasMinimosSubstituicao", source = "diasMinimosSubstituicao")
  @Mapping(target = "valorGratificacao", source = "valorGratificacao")
  @Mapping(target = "percentualOpcaoEfetivo", source = "percentualOpcaoEfetivo")
  @Mapping(target = "exigeVinculoEfetivo", source = "exigeVinculoEfetivo")
  @Mapping(target = "requisitos", source = "requisitos")
  @Mapping(target = "documentoLegalId", source = "documentoLegalId")
  @Mapping(target = "atoPublicacaoId", source = "atoPublicacaoId")
  @Mapping(target = "observacoes", source = "observacoes")
  @Mapping(target = "customFields", source = "customFields")
  protected abstract FuncaoConfianca fromDados(FuncaoConfiancaDados dto);

  @Override
  @InheritConfiguration(name = "fromDados")
  public abstract void applyUpdate(@MappingTarget FuncaoConfianca entity, UpdateFuncaoConfiancaDTO dto);

  @Override
  public Long extractId(FuncaoConfianca entity) {
    return entity.getId();
  }
}
