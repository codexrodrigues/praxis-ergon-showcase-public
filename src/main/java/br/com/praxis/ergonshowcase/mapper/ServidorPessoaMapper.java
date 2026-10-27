package br.com.praxis.ergonshowcase.mapper;

import br.com.praxis.ergonshowcase.domain.*;
import br.com.praxis.ergonshowcase.dto.*;
import org.praxisplatform.uischema.mapper.base.ResourceMapper;
import org.springframework.stereotype.Component;

/** Fronteira explícita: somente campos cadastrais são copiados na manutenção. */
@Component
public class ServidorPessoaMapper
    implements ResourceMapper<
        ServidorPessoa,
        ServidorPessoaResponseDTO,
        CreateServidorPessoaDTO,
        UpdateServidorPessoaDTO,
        Long> {
  public ServidorPessoaResponseDTO toResponse(ServidorPessoa entity) {
    var dto = new ServidorPessoaResponseDTO();
    dto.setNome(entity.getNome());
    dto.setNomeSocial(entity.getNomeSocial());
    dto.setCpf(entity.getCpf());
    dto.setDtnasc(entity.getDtnasc());
    dto.setEMail(entity.getEMail());
    dto.setTelefone(entity.getTelefone());
    dto.setNumtelCelular(entity.getNumtelCelular());
    dto.setPispasep(entity.getPispasep());
    dto.setCustomFields(entity.getCustomFields());
    dto.setId(entity.getNumero());
    return dto;
  }

  public ServidorPessoa newEntity(CreateServidorPessoaDTO dto) {
    var entity = new ServidorPessoa();
    entity.setNome(dto.getNome());
    entity.setNomeSocial(dto.getNomeSocial());
    entity.setCpf(dto.getCpf());
    entity.setDtnasc(dto.getDtnasc());
    entity.setEMail(dto.getEMail());
    entity.setTelefone(dto.getTelefone());
    entity.setNumtelCelular(dto.getNumtelCelular());
    entity.setPispasep(dto.getPispasep());
    entity.setCustomFields(dto.getCustomFields());
    entity.setNumero(dto.getNumero());
    return entity;
  }

  public void applyUpdate(ServidorPessoa entity, UpdateServidorPessoaDTO dto) {
    entity.setNome(dto.getNome());
    entity.setNomeSocial(dto.getNomeSocial());
    entity.setCpf(dto.getCpf());
    entity.setDtnasc(dto.getDtnasc());
    entity.setEMail(dto.getEMail());
    entity.setTelefone(dto.getTelefone());
    entity.setNumtelCelular(dto.getNumtelCelular());
    entity.setPispasep(dto.getPispasep());
    entity.setCustomFields(dto.getCustomFields());
  }

  public Long extractId(ServidorPessoa entity) {
    return entity.getNumero();
  }
}
