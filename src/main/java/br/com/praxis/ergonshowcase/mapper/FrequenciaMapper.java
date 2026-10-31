package br.com.praxis.ergonshowcase.mapper;

import br.com.praxis.ergonshowcase.domain.CodigoFrequenciaId;
import br.com.praxis.ergonshowcase.domain.Frequencia;
import br.com.praxis.ergonshowcase.domain.VinculoId;
import br.com.praxis.ergonshowcase.dto.CreateFrequenciaDTO;
import br.com.praxis.ergonshowcase.dto.FrequenciaDados;
import br.com.praxis.ergonshowcase.dto.FrequenciaResponseDTO;
import br.com.praxis.ergonshowcase.dto.UpdateFrequenciaDTO;
import java.util.LinkedHashMap;
import org.praxisplatform.uischema.mapper.base.ResourceMapper;
import org.springframework.stereotype.Component;

@Component
public class FrequenciaMapper
    implements ResourceMapper<
        Frequencia, FrequenciaResponseDTO, CreateFrequenciaDTO, UpdateFrequenciaDTO, Long> {

  @Override
  public FrequenciaResponseDTO toResponse(Frequencia entity) {
    FrequenciaResponseDTO dto = new FrequenciaResponseDTO();
    dto.setId(entity.getId());
    dto.setEmpCodigo(entity.getEmpCodigo());
    dto.setNumfunc(entity.getNumfunc());
    dto.setNumvinc(entity.getNumvinc());
    if (entity.getNumfunc() != null && entity.getNumvinc() != null) {
      dto.setVinculoId(new VinculoId(entity.getNumfunc(), entity.getNumvinc()));
    }
    dto.setTipofreq(entity.getTipofreq());
    dto.setCodfreq(entity.getCodfreq());
    if (entity.getTipofreq() != null && entity.getCodfreq() != null) {
      dto.setCodigoFrequenciaId(new CodigoFrequenciaId(entity.getTipofreq(), entity.getCodfreq()));
    }
    dto.setDtini(entity.getDtini());
    dto.setDtfim(entity.getDtfim());
    dto.setQuantidade(entity.getQuantidade());
    dto.setHoraEntrada(entity.getHoraEntrada());
    dto.setHoraSaida(entity.getHoraSaida());
    dto.setSituacao(entity.getSituacao());
    dto.setObs(entity.getObs());
    dto.setDocumentoLegalId(entity.getDocumentoLegalId());
    dto.setCustomFields(
        entity.getCustomFields() != null
            ? new LinkedHashMap<>(entity.getCustomFields())
            : new LinkedHashMap<>());
    if (entity.getOrgao() != null) {
      dto.setOrgaoNome(entity.getOrgao().getNome());
    }
    if (entity.getPessoa() != null) {
      dto.setPessoaNome(entity.getPessoa().getNome());
    }
    if (entity.getVinculo() != null) {
      dto.setVinculoMatricula(entity.getVinculo().getMatric());
    }
    if (entity.getTipoFrequenciaEntidade() != null) {
      dto.setTipoFrequenciaDescricao(entity.getTipoFrequenciaEntidade().getNome());
    }
    if (entity.getCodigoFrequenciaEntidade() != null) {
      dto.setCodigoFrequenciaNome(entity.getCodigoFrequenciaEntidade().getNome());
    }
    if (entity.getDocumentoLegal() != null) {
      dto.setDocumentoLegalNumero(entity.getDocumentoLegal().getNumeroDocumento());
    }
    dto.setPendenteHomologacao("PENDENTE_HOMOLOGACAO".equals(entity.getSituacao()));
    dto.setOcorrenciaHoraria(entity.getHoraEntrada() != null && entity.getHoraSaida() != null);
    dto.setComDocumentoComprobatorio(entity.getDocumentoLegalId() != null);
    return dto;
  }

  @Override
  public Frequencia newEntity(CreateFrequenciaDTO dto) {
    Frequencia entity = new Frequencia();
    entity.setEmpCodigo(dto.getEmpCodigo());
    entity.setNumfunc(dto.getNumfunc());
    entity.setNumvinc(dto.getNumvinc());
    entity.setSituacao("PENDENTE_HOMOLOGACAO");
    applyShared(entity, dto);
    return entity;
  }

  @Override
  public void applyUpdate(Frequencia entity, UpdateFrequenciaDTO dto) {
    applyShared(entity, dto);
  }

  private void applyShared(Frequencia entity, FrequenciaDados dto) {
    entity.setTipofreq(dto.getTipofreq());
    entity.setCodfreq(dto.getCodfreq());
    entity.setDtini(dto.getDtini());
    entity.setDtfim(dto.getDtfim());
    entity.setQuantidade(dto.getQuantidade());
    entity.setHoraEntrada(dto.getHoraEntrada());
    entity.setHoraSaida(dto.getHoraSaida());
    entity.setObs(dto.getObs());
    entity.setDocumentoLegalId(dto.getDocumentoLegalId());
    entity.setCustomFields(dto.getCustomFields());
    if (entity.getCustomFields() == null) {
      entity.setCustomFields(new LinkedHashMap<>());
    }
  }

  @Override
  public Long extractId(Frequencia entity) {
    return entity.getId();
  }
}
