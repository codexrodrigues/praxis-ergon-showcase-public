package br.com.praxis.ergonshowcase.mapper;

import br.com.praxis.ergonshowcase.domain.DesignacaoFuncao;
import br.com.praxis.ergonshowcase.dto.*;
import org.mapstruct.*;
import org.praxisplatform.uischema.mapper.base.ResourceMapper;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class DesignacaoFuncaoMapper implements ResourceMapper<DesignacaoFuncao, DesignacaoFuncaoResponseDTO, CreateDesignacaoFuncaoDTO, UpdateDesignacaoFuncaoDTO, Long> {
  @Override
  @Mapping(target = "titularPessoaId", source = "titularId.numfunc")
  @Mapping(target = "titularNome", source = "titular.pessoa.nome")
  public abstract DesignacaoFuncaoResponseDTO toResponse(DesignacaoFuncao entity);

  @Override
  public DesignacaoFuncao newEntity(CreateDesignacaoFuncaoDTO dto) {
    return fromDados(dto);
  }

  @BeanMapping(ignoreByDefault = true)
  @Mapping(target = "empCodigo", source = "empCodigo")
  @Mapping(target = "descricao", source = "descricao")
  @Mapping(target = "dtini", source = "dtini")
  @Mapping(target = "dtfim", source = "dtfim")
  @Mapping(target = "situacao", source = "situacao")
  @Mapping(target = "titularId", source = "titularId")
  @Mapping(target = "funcaoId", source = "funcaoId")
  @Mapping(target = "vagaFuncaoId", source = "vagaFuncaoId")
  @Mapping(target = "setorId", source = "setorId")
  @Mapping(target = "dtExercicio", source = "dtExercicio")
  @Mapping(target = "substituto1Id", source = "substituto1Id")
  @Mapping(target = "substituto2Id", source = "substituto2Id")
  @Mapping(target = "opcaoRemuneracao", source = "opcaoRemuneracao")
  @Mapping(target = "valorRemuneracao", source = "valorRemuneracao")
  @Mapping(target = "atoDispensaId", source = "atoDispensaId")
  @Mapping(target = "motivoDispensa", source = "motivoDispensa")
  @Mapping(target = "documentoLegalId", source = "documentoLegalId")
  @Mapping(target = "atoPublicacaoId", source = "atoPublicacaoId")
  @Mapping(target = "observacoes", source = "observacoes")
  @Mapping(target = "customFields", source = "customFields")
  protected abstract DesignacaoFuncao fromDados(DesignacaoFuncaoDados dto);

  @Override
  @InheritConfiguration(name = "fromDados")
  public abstract void applyUpdate(@MappingTarget DesignacaoFuncao entity, UpdateDesignacaoFuncaoDTO dto);

  @Override
  public Long extractId(DesignacaoFuncao entity) {
    return entity.getId();
  }
}
