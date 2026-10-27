package br.com.praxis.ergonshowcase.mapper;

import br.com.praxis.ergonshowcase.domain.Orgao;
import br.com.praxis.ergonshowcase.dto.*;
import org.mapstruct.*;
import org.praxisplatform.uischema.mapper.base.ResourceMapper;

/** Projeção explícita gerada por MapStruct; a manutenção preserva identidade e auditoria. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public abstract class OrgaoMapper
    implements ResourceMapper<Orgao, OrgaoResponseDTO, CreateOrgaoDTO, UpdateOrgaoDTO, Integer> {
  @BeanMapping(ignoreByDefault = true)
  @Mapping(target = "id", source = "empresa")
  @Mapping(target = "nome", source = "nome")
  @Mapping(target = "fantasia", source = "fantasia")
  @Mapping(target = "razao", source = "razao")
  @Mapping(target = "cnpj", source = "cnpj")
  @Mapping(target = "ufSigla", source = "ufSigla")
  @Mapping(target = "municipio", source = "municipio")
  @Mapping(target = "cep", source = "cep")
  @Mapping(target = "logradouro", source = "logradouro")
  @Mapping(target = "telefone", source = "telefone")
  @Mapping(target = "email", source = "email")
  @Mapping(target = "web", source = "web")
  @Mapping(target = "ativa", source = "ativa")
  public abstract OrgaoResponseDTO toResponse(Orgao entity);

  @BeanMapping(ignoreByDefault = true)
  @Mapping(target = "empresa", source = "empresa")
  @Mapping(target = "nome", source = "nome")
  @Mapping(target = "fantasia", source = "fantasia")
  @Mapping(target = "razao", source = "razao")
  @Mapping(target = "cnpj", source = "cnpj")
  @Mapping(target = "ufSigla", source = "ufSigla")
  @Mapping(target = "municipio", source = "municipio")
  @Mapping(target = "cep", source = "cep")
  @Mapping(target = "logradouro", source = "logradouro")
  @Mapping(target = "telefone", source = "telefone")
  @Mapping(target = "email", source = "email")
  @Mapping(target = "web", source = "web")
  @Mapping(target = "ativa", source = "ativa")
  public abstract Orgao newEntity(CreateOrgaoDTO dto);

  @BeanMapping(ignoreByDefault = true)
  @Mapping(target = "nome", source = "nome")
  @Mapping(target = "fantasia", source = "fantasia")
  @Mapping(target = "razao", source = "razao")
  @Mapping(target = "cnpj", source = "cnpj")
  @Mapping(target = "ufSigla", source = "ufSigla")
  @Mapping(target = "municipio", source = "municipio")
  @Mapping(target = "cep", source = "cep")
  @Mapping(target = "logradouro", source = "logradouro")
  @Mapping(target = "telefone", source = "telefone")
  @Mapping(target = "email", source = "email")
  @Mapping(target = "web", source = "web")
  @Mapping(target = "ativa", source = "ativa")
  public abstract void applyUpdate(@MappingTarget Orgao entity, UpdateOrgaoDTO dto);

  public Integer extractId(Orgao entity) {
    return entity.getEmpresa();
  }
}
