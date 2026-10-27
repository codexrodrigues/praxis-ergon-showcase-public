package br.com.praxis.ergonshowcase.mapper;

import br.com.praxis.ergonshowcase.domain.*;
import br.com.praxis.ergonshowcase.dto.*;
import org.praxisplatform.uischema.mapper.base.ResourceMapper;
import org.springframework.stereotype.Component;

/** Fronteira explícita: somente campos cadastrais são copiados na manutenção. */
@Component
public class CargoMapper
    implements ResourceMapper<Cargo, CargoResponseDTO, CreateCargoDTO, UpdateCargoDTO, Integer> {
  @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager em;

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
    dto.setEAglutinador(entity.getEAglutinador());
    dto.setCargoAglutinador(entity.getCargoAglutinador());
    dto.setDtInicioContrVaga(entity.getDtInicioContrVaga());
    dto.setDocumentoLegalId(entity.getDocumentoLegalId());
    dto.setDtExtincao(entity.getDtExtincao());
    dto.setCargoExtinto(entity.getDtExtincao() != null);
    long ocupadas =
        em.createQuery(
                "select count(v) from Vinculo v where v.cargo=:c and v.situacao in ('ATIVO','AFASTADO','CEDIDO')",
                Long.class)
            .setParameter("c", entity.getCargo())
            .getSingleResult();
    dto.setSaldoVagasDisponiveis(
        entity.getVagasAutorizadas() == null ? null : entity.getVagasAutorizadas() - ocupadas);
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
    entity.setEAglutinador(dto.getEAglutinador());
    entity.setCargoAglutinador(dto.getCargoAglutinador());
    entity.setDtInicioContrVaga(dto.getDtInicioContrVaga());
    entity.setDocumentoLegalId(dto.getDocumentoLegalId());
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
    entity.setEAglutinador(dto.getEAglutinador());
    entity.setCargoAglutinador(dto.getCargoAglutinador());
    entity.setDtInicioContrVaga(dto.getDtInicioContrVaga());
    entity.setDocumentoLegalId(dto.getDocumentoLegalId());
  }

  public Integer extractId(Cargo entity) {
    return entity.getCargo();
  }
}
