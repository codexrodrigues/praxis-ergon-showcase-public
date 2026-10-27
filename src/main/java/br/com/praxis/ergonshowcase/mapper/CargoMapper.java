package br.com.praxis.ergonshowcase.mapper;

import br.com.praxis.ergonshowcase.domain.*;
import br.com.praxis.ergonshowcase.dto.*;
import org.praxisplatform.uischema.mapper.base.ResourceMapper;
import org.springframework.stereotype.Component;

/** Fronteira explícita: somente campos cadastrais são copiados na manutenção. */
@Component
public class CargoMapper
    implements ResourceMapper<Cargo, CargoResponseDTO, CreateCargoDTO, UpdateCargoDTO, Integer> {
  public CargoResponseDTO toResponse(Cargo entity) {
    var dto = new CargoResponseDTO();
    dto.setNome(entity.getNome());
    dto.setCategoria(entity.getCategoria());
    dto.setSubcategoria(entity.getSubcategoria());
    dto.setTipoCargo(entity.getTipoCargo());
    dto.setCargoFuncao(entity.getCargoFuncao());
    dto.setEscolaridade(entity.getEscolaridade());
    dto.setCbo(entity.getCbo());
    dto.setVagasAutorizadas(entity.getVagasAutorizadas());
    dto.setControleVaga(entity.getControleVaga());
    dto.setPorReferencia(entity.getPorReferencia());
    dto.setCustomFields(entity.getCustomFields());
    dto.setId(entity.getCargo());
    dto.setEmpCodigo(entity.getEmpCodigo());
    dto.setOrgaoNome(entity.getOrgao() == null ? null : entity.getOrgao().getNome());
    return dto;
  }

  public Cargo newEntity(CreateCargoDTO dto) {
    var entity = new Cargo();
    entity.setNome(dto.getNome());
    entity.setCategoria(dto.getCategoria());
    entity.setSubcategoria(dto.getSubcategoria());
    entity.setTipoCargo(dto.getTipoCargo());
    entity.setCargoFuncao(dto.getCargoFuncao());
    entity.setEscolaridade(dto.getEscolaridade());
    entity.setCbo(dto.getCbo());
    entity.setVagasAutorizadas(dto.getVagasAutorizadas());
    entity.setControleVaga(dto.getControleVaga());
    entity.setPorReferencia(dto.getPorReferencia());
    entity.setCustomFields(dto.getCustomFields());
    entity.setCargo(dto.getCargo());
    entity.setEmpCodigo(dto.getEmpCodigo());
    return entity;
  }

  public void applyUpdate(Cargo entity, UpdateCargoDTO dto) {
    entity.setNome(dto.getNome());
    entity.setCategoria(dto.getCategoria());
    entity.setSubcategoria(dto.getSubcategoria());
    entity.setTipoCargo(dto.getTipoCargo());
    entity.setCargoFuncao(dto.getCargoFuncao());
    entity.setEscolaridade(dto.getEscolaridade());
    entity.setCbo(dto.getCbo());
    entity.setVagasAutorizadas(dto.getVagasAutorizadas());
    entity.setControleVaga(dto.getControleVaga());
    entity.setPorReferencia(dto.getPorReferencia());
    entity.setCustomFields(dto.getCustomFields());
  }

  public Integer extractId(Cargo entity) {
    return entity.getCargo();
  }
}
