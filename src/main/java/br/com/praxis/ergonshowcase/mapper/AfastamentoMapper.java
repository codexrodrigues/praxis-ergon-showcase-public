package br.com.praxis.ergonshowcase.mapper;

import br.com.praxis.ergonshowcase.domain.*;
import br.com.praxis.ergonshowcase.dto.*;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import org.praxisplatform.uischema.mapper.base.ResourceMapper;
import org.springframework.stereotype.Component;

/** Mapeador canônico de Afastamento, preservando identidade, vínculo, modalidade e estado. */
@Component
public class AfastamentoMapper
    implements ResourceMapper<
        Afastamento, AfastamentoResponseDTO, CreateAfastamentoDTO, UpdateAfastamentoDTO, Long> {

  @Override
  public AfastamentoResponseDTO toResponse(Afastamento entity) {
    AfastamentoResponseDTO dto = new AfastamentoResponseDTO();
    dto.setId(entity.getId());
    dto.setEmpCodigo(entity.getEmpCodigo());
    if (entity.getOrgao() != null) {
      dto.setOrgaoNome(entity.getOrgao().getNome());
    }
    dto.setNumfunc(entity.getNumfunc());
    dto.setNumvinc(entity.getNumvinc());
    if (entity.getNumfunc() != null && entity.getNumvinc() != null) {
      dto.setVinculoId(new VinculoId(entity.getNumfunc(), entity.getNumvinc()));
    }
    if (entity.getVinculo() != null) {
      dto.setVinculoMatricula(entity.getVinculo().getMatric());
    }
    if (entity.getPessoa() != null) {
      dto.setPessoaNome(entity.getPessoa().getNome());
    }
    dto.setTipofreq(entity.getTipofreq());
    if (entity.getTipoFrequenciaEntidade() != null) {
      dto.setTipoFrequenciaNome(entity.getTipoFrequenciaEntidade().getNome());
    }
    dto.setCodfreq(entity.getCodfreq());
    if (entity.getTipofreq() != null && entity.getCodfreq() != null) {
      dto.setCodigoFrequenciaId(new CodigoFrequenciaId(entity.getTipofreq(), entity.getCodfreq()));
    }
    if (entity.getCodigoFrequenciaEntidade() != null) {
      dto.setCodigoFrequenciaNome(entity.getCodigoFrequenciaEntidade().getNome());
    }
    dto.setDtini(entity.getDtini());
    dto.setDtprevfim(entity.getDtprevfim());
    dto.setDtfim(entity.getDtfim());
    Integer dias = entity.getQuantidadeDias();
    if (dias == null && entity.getDtini() != null) {
      var refFim = entity.getDtfim() != null ? entity.getDtfim() : entity.getDtprevfim();
      if (refFim != null && !refFim.isBefore(entity.getDtini())) {
        dias = Math.toIntExact(ChronoUnit.DAYS.between(entity.getDtini(), refFim) + 1);
      }
    }
    dto.setQuantidadeDias(dias);
    dto.setSituacao(entity.getSituacao());
    dto.setMotivo(entity.getMotivo());
    dto.setProrrogacao(entity.getProrrogacao());
    dto.setAfastamentoOrigemId(entity.getAfastamentoOrigemId());
    dto.setNumfuncSubstituto(entity.getNumfuncSubstituto());
    dto.setNumvincSubstituto(entity.getNumvincSubstituto());
    if (entity.getPessoaSubstituta() != null) {
      dto.setSubstitutoNome(entity.getPessoaSubstituta().getNome());
    }
    dto.setDocumentoLegalId(entity.getDocumentoLegalId());
    if (entity.getDocumentoLegal() != null) {
      dto.setDocumentoLegalNumero(entity.getDocumentoLegal().getNumeroDocumento());
    }
    dto.setAtoPublicacaoId(entity.getAtoPublicacaoId());
    if (entity.getAtoPublicacao() != null) {
      dto.setAtoPublicacaoNumero(entity.getAtoPublicacao().getNumeroDocumento());
    }
    dto.setCustomFields(
        entity.getCustomFields() == null
            ? new LinkedHashMap<>()
            : new LinkedHashMap<>(entity.getCustomFields()));
    dto.setAfastamentoEmCurso(
        "HOMOLOGADO".equals(entity.getSituacao()) && entity.getDtfim() == null);
    dto.setComSubstitutoDesignado(entity.getNumfuncSubstituto() != null);
    dto.setPendenteHomologacao("PENDENTE_HOMOLOGACAO".equals(entity.getSituacao()));
    return dto;
  }

  @Override
  public Afastamento newEntity(CreateAfastamentoDTO dto) {
    Afastamento entity = new Afastamento();
    entity.setEmpCodigo(dto.getEmpCodigo());
    entity.setNumfunc(dto.getNumfunc());
    entity.setNumvinc(dto.getNumvinc());
    entity.setTipofreq(dto.getTipofreq());
    entity.setCodfreq(dto.getCodfreq());
    entity.setDtini(dto.getDtini());
    entity.setSituacao("PENDENTE_HOMOLOGACAO");
    applyUpdate(entity, dto);
    return entity;
  }

  @Override
  public void applyUpdate(Afastamento entity, UpdateAfastamentoDTO dto) {
    copyMutable(entity, dto);
  }

  private void applyUpdate(Afastamento entity, CreateAfastamentoDTO dto) {
    copyMutable(entity, dto);
  }

  private void copyMutable(Afastamento entity, AfastamentoDados dto) {
    entity.setTipofreq(dto.getTipofreq());
    entity.setCodfreq(dto.getCodfreq());
    entity.setDtini(dto.getDtini());
    entity.setDtprevfim(dto.getDtprevfim());
    entity.setMotivo(dto.getMotivo());
    entity.setProrrogacao(dto.getProrrogacao() == null
        ? dto.getAfastamentoOrigemId() != null
        : dto.getProrrogacao());
    entity.setAfastamentoOrigemId(dto.getAfastamentoOrigemId());
    entity.setNumfuncSubstituto(dto.getNumfuncSubstituto());
    entity.setNumvincSubstituto(dto.getNumvincSubstituto());
    entity.setDocumentoLegalId(dto.getDocumentoLegalId());
    entity.setAtoPublicacaoId(dto.getAtoPublicacaoId());
    entity.setCustomFields(dto.getCustomFields());
    if (entity.getCustomFields() == null) entity.setCustomFields(new LinkedHashMap<>());
    if (entity.getDtfim() == null && entity.getDtini() != null && entity.getDtprevfim() != null
        && !entity.getDtprevfim().isBefore(entity.getDtini())) {
      entity.setQuantidadeDias(
          Math.toIntExact(ChronoUnit.DAYS.between(entity.getDtini(), entity.getDtprevfim()) + 1));
    }
  }

  @Override
  public Long extractId(Afastamento entity) {
    return entity.getId();
  }
}
