package br.com.praxis.ergonshowcase.mapper;
import br.com.praxis.ergonshowcase.domain.CargoSetor;
import br.com.praxis.ergonshowcase.dto.*;
import org.mapstruct.*;
import org.praxisplatform.uischema.mapper.base.ResourceMapper;
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class CargoSetorMapper implements ResourceMapper<CargoSetor,CargoSetorResponseDTO,CreateCargoSetorDTO,UpdateCargoSetorDTO,Long> {
 public abstract CargoSetorResponseDTO toResponse(CargoSetor entity);
 @BeanMapping(ignoreByDefault=true)
 @Mapping(target="empCodigo",source="empCodigo")
 @Mapping(target="dtini",source="dtini")
 @Mapping(target="dtfim",source="dtfim")
 @Mapping(target="documentoLegalId",source="documentoLegalId")
 @Mapping(target="observacao",source="observacao")
 @Mapping(target="customFields",source="customFields")
 @Mapping(target="setor",source="setor")
 @Mapping(target="cargoId",source="cargoId")
 @Mapping(target="abrangeSubordinados",source="abrangeSubordinados")
 @Mapping(target="tipoControleQuadro",source="tipoControleQuadro")
 @Mapping(target="quadroPaiId",source="quadroPaiId")
 @Mapping(target="lotacaoMaximaAutorizada",source="lotacaoMaximaAutorizada")
 @Mapping(target="percInsalubridade",source="percInsalubridade")
 @Mapping(target="percPericulosidade",source="percPericulosidade")
 @Mapping(target="exigeHabilitacaoEspecifica",source="exigeHabilitacaoEspecifica")
 public abstract CargoSetor newEntity(CreateCargoSetorDTO dto);
 @BeanMapping(ignoreByDefault=true)
 @Mapping(target="empCodigo",source="empCodigo")
 @Mapping(target="dtini",source="dtini")
 @Mapping(target="dtfim",source="dtfim")
 @Mapping(target="documentoLegalId",source="documentoLegalId")
 @Mapping(target="observacao",source="observacao")
 @Mapping(target="customFields",source="customFields")
 @Mapping(target="setor",source="setor")
 @Mapping(target="cargoId",source="cargoId")
 @Mapping(target="abrangeSubordinados",source="abrangeSubordinados")
 @Mapping(target="tipoControleQuadro",source="tipoControleQuadro")
 @Mapping(target="quadroPaiId",source="quadroPaiId")
 @Mapping(target="lotacaoMaximaAutorizada",source="lotacaoMaximaAutorizada")
 @Mapping(target="percInsalubridade",source="percInsalubridade")
 @Mapping(target="percPericulosidade",source="percPericulosidade")
 @Mapping(target="exigeHabilitacaoEspecifica",source="exigeHabilitacaoEspecifica")
 public abstract void applyUpdate(@MappingTarget CargoSetor entity, UpdateCargoSetorDTO dto);
 public Long extractId(CargoSetor entity) {return entity.getId();}
}
