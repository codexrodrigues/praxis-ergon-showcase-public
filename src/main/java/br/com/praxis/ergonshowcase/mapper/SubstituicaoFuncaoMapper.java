package br.com.praxis.ergonshowcase.mapper;

import br.com.praxis.ergonshowcase.domain.SubstituicaoFuncao;
import br.com.praxis.ergonshowcase.dto.*;
import org.mapstruct.*;
import org.praxisplatform.uischema.mapper.base.ResourceMapper;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class SubstituicaoFuncaoMapper implements ResourceMapper<SubstituicaoFuncao, SubstituicaoFuncaoResponseDTO, CreateSubstituicaoFuncaoDTO, UpdateSubstituicaoFuncaoDTO, Long> {
  @Override
  @Mapping(target = "substitutoPessoaId", source = "substitutoId.numfunc")
  @Mapping(target = "substitutoNome", source = "substituto.pessoa.nome")
  public abstract SubstituicaoFuncaoResponseDTO toResponse(SubstituicaoFuncao entity);

  @Override
  public SubstituicaoFuncao newEntity(CreateSubstituicaoFuncaoDTO dto) {
    return fromDados(dto);
  }

  @BeanMapping(ignoreByDefault = true)
  @Mapping(target = "empCodigo", source = "empCodigo")
  @Mapping(target = "descricao", source = "descricao")
  @Mapping(target = "dtini", source = "dtini")
  @Mapping(target = "dtfim", source = "dtfim")
  @Mapping(target = "situacao", source = "situacao")
  @Mapping(target = "designacaoId", source = "designacaoId")
  @Mapping(target = "substitutoId", source = "substitutoId")
  @Mapping(target = "motivo", source = "motivo")
  @Mapping(target = "afastamentoId", source = "afastamentoId")
  @Mapping(target = "feriasGozoId", source = "feriasGozoId")
  @Mapping(target = "dtiniRemunerada", source = "dtiniRemunerada")
  @Mapping(target = "dtfimRemunerada", source = "dtfimRemunerada")
  @Mapping(target = "competenciaFolha", source = "competenciaFolha")
  @Mapping(target = "valorSubstituicao", source = "valorSubstituicao")
  @Mapping(target = "documentoLegalId", source = "documentoLegalId")
  @Mapping(target = "atoPublicacaoId", source = "atoPublicacaoId")
  @Mapping(target = "observacoes", source = "observacoes")
  @Mapping(target = "customFields", source = "customFields")
  protected abstract SubstituicaoFuncao fromDados(SubstituicaoFuncaoDados dto);

  @Override
  @InheritConfiguration(name = "fromDados")
  public abstract void applyUpdate(@MappingTarget SubstituicaoFuncao entity, UpdateSubstituicaoFuncaoDTO dto);

  @Override
  public Long extractId(SubstituicaoFuncao entity) {
    return entity.getId();
  }
}
