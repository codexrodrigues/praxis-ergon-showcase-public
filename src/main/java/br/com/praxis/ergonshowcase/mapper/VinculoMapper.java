package br.com.praxis.ergonshowcase.mapper;

import br.com.praxis.ergonshowcase.domain.*;
import br.com.praxis.ergonshowcase.dto.*;
import org.praxisplatform.uischema.mapper.base.ResourceMapper;
import org.springframework.stereotype.Component;

/** Fronteira explícita: somente campos cadastrais são copiados na manutenção. */
@Component
public class VinculoMapper
    implements ResourceMapper<
        Vinculo, VinculoResponseDTO, CreateVinculoDTO, UpdateVinculoDTO, VinculoId> {
  public VinculoResponseDTO toResponse(Vinculo entity) {
    var dto = new VinculoResponseDTO();
    dto.setMatric(entity.getMatric());
    dto.setRegimejur(entity.getRegimejur());
    dto.setTipovinc(entity.getTipovinc());
    dto.setCategoria(entity.getCategoria());
    dto.setCargo(entity.getCargo());
    dto.setReferencia(entity.getReferencia());
    dto.setJornadaSemanal(entity.getJornadaSemanal());
    dto.setCustomFields(entity.getCustomFields());
    dto.setId(entity.getId());
    dto.setEmpCodigo(entity.getEmpCodigo());
    dto.setOrgaoNome(entity.getOrgao() == null ? null : entity.getOrgao().getNome());
    dto.setNumfunc(entity.getId().numfunc());
    dto.setNumero(entity.getId().numero());
    dto.setPessoaNome(entity.getPessoa().getNome());
    dto.setCargoNome(
        entity.getCargoEntidade() == null ? null : entity.getCargoEntidade().getNome());
    dto.setSituacao(entity.getSituacao());
    dto.setDtexerc(entity.getDtexerc());
    dto.setDtvac(entity.getDtvac());
    dto.setDiasDesdeExercicio(
        java.time.temporal.ChronoUnit.DAYS.between(
            entity.getDtexerc(),
            entity.getDtvac() == null
                ? java.time.LocalDate.now(java.time.ZoneOffset.UTC)
                : entity.getDtvac()));
    return dto;
  }

  public Vinculo newEntity(CreateVinculoDTO dto) {
    var entity = new Vinculo();
    entity.setMatric(dto.getMatric());
    entity.setRegimejur(dto.getRegimejur());
    entity.setTipovinc(dto.getTipovinc());
    entity.setCategoria(dto.getCategoria());
    entity.setCargo(dto.getCargo());
    entity.setReferencia(dto.getReferencia());
    entity.setJornadaSemanal(dto.getJornadaSemanal());
    entity.setCustomFields(dto.getCustomFields());
    entity.setId(new VinculoId(dto.getNumfunc(), dto.getNumero()));
    entity.setEmpCodigo(dto.getEmpCodigo());
    entity.setDtexerc(dto.getDtexerc());
    entity.setSituacao("ATIVO");
    return entity;
  }

  public void applyUpdate(Vinculo entity, UpdateVinculoDTO dto) {
    entity.setMatric(dto.getMatric());
    entity.setRegimejur(dto.getRegimejur());
    entity.setTipovinc(dto.getTipovinc());
    entity.setCategoria(dto.getCategoria());
    entity.setCargo(dto.getCargo());
    entity.setReferencia(dto.getReferencia());
    entity.setJornadaSemanal(dto.getJornadaSemanal());
    entity.setCustomFields(dto.getCustomFields());
  }

  public VinculoId extractId(Vinculo entity) {
    return entity.getId();
  }
}
