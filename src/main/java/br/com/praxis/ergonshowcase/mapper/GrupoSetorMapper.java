package br.com.praxis.ergonshowcase.mapper;
import br.com.praxis.ergonshowcase.domain.GrupoSetor;
import br.com.praxis.ergonshowcase.dto.*;
import org.mapstruct.*;
import org.praxisplatform.uischema.mapper.base.ResourceMapper;
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class GrupoSetorMapper implements ResourceMapper<GrupoSetor,GrupoSetorResponseDTO,CreateGrupoSetorDTO,UpdateGrupoSetorDTO,Long> {
 public abstract GrupoSetorResponseDTO toResponse(GrupoSetor entity);
 @BeanMapping(ignoreByDefault=true)
 @Mapping(target="empCodigo",source="empCodigo")
 @Mapping(target="dtini",source="dtini")
 @Mapping(target="dtfim",source="dtfim")
 @Mapping(target="documentoLegalId",source="documentoLegalId")
 @Mapping(target="observacao",source="observacao")
 @Mapping(target="customFields",source="customFields")
 @Mapping(target="sigla",source="sigla")
 @Mapping(target="nome",source="nome")
 @Mapping(target="tipoAgrupamento",source="tipoAgrupamento")
 @Mapping(target="setorSede",source="setorSede")
 @Mapping(target="subempCodigoVinculada",source="subempCodigoVinculada")
 @Mapping(target="ufAbrangencia",source="ufAbrangencia")
 @Mapping(target="municipioSede",source="municipioSede")
 @Mapping(target="ativo",source="ativo")
 public abstract GrupoSetor newEntity(CreateGrupoSetorDTO dto);
 @BeanMapping(ignoreByDefault=true)
 @Mapping(target="empCodigo",source="empCodigo")
 @Mapping(target="dtini",source="dtini")
 @Mapping(target="dtfim",source="dtfim")
 @Mapping(target="documentoLegalId",source="documentoLegalId")
 @Mapping(target="observacao",source="observacao")
 @Mapping(target="customFields",source="customFields")
 @Mapping(target="sigla",source="sigla")
 @Mapping(target="nome",source="nome")
 @Mapping(target="tipoAgrupamento",source="tipoAgrupamento")
 @Mapping(target="setorSede",source="setorSede")
 @Mapping(target="subempCodigoVinculada",source="subempCodigoVinculada")
 @Mapping(target="ufAbrangencia",source="ufAbrangencia")
 @Mapping(target="municipioSede",source="municipioSede")
 @Mapping(target="ativo",source="ativo")
 public abstract void applyUpdate(@MappingTarget GrupoSetor entity, UpdateGrupoSetorDTO dto);
 public Long extractId(GrupoSetor entity) {return entity.getId();}
}
