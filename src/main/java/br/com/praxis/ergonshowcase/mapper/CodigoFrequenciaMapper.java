package br.com.praxis.ergonshowcase.mapper;

import br.com.praxis.ergonshowcase.domain.CodigoFrequencia;
import br.com.praxis.ergonshowcase.domain.CodigoFrequenciaId;
import br.com.praxis.ergonshowcase.dto.CodigoFrequenciaResponseDTO;
import org.praxisplatform.uischema.mapper.base.ResourceMapper;
import org.springframework.stereotype.Component;

/** Projeção de leitura do catálogo de códigos de frequência sem acesso adicional ao banco. */
@Component
public class CodigoFrequenciaMapper
    implements ResourceMapper<
        CodigoFrequencia, CodigoFrequenciaResponseDTO, Void, Void, CodigoFrequenciaId> {
  @Override
  public CodigoFrequenciaResponseDTO toResponse(CodigoFrequencia entity) {
    CodigoFrequenciaResponseDTO dto = new CodigoFrequenciaResponseDTO();
    dto.setId(entity.getId());
    if (entity.getId() != null) {
      dto.setTipofreq(entity.getId().tipofreq());
      dto.setCodigo(entity.getId().codigo());
    }
    if (entity.getTipoFrequenciaEntidade() != null) {
      dto.setTipoFrequenciaNome(entity.getTipoFrequenciaEntidade().getNome());
    }
    dto.setNome(entity.getNome());
    dto.setMnemonico(entity.getMnemonico());
    dto.setNomeAbrev(entity.getNomeAbrev());
    dto.setViaFrequencia(entity.getViaFrequencia());
    dto.setOnus(entity.getOnus());
    dto.setAfastamento(entity.getAfastamento());
    dto.setRegrasFreq(entity.getRegrasFreq());
    dto.setHoraEntradaSaida(entity.getHoraEntradaSaida());
    dto.setSexo(entity.getSexo());
    dto.setTipoPreenchimento(entity.getTipoPreenchimento());
    dto.setFormatoQtd(entity.getFormatoQtd());
    dto.setQtdFaltas(entity.getQtdFaltas());
    dto.setQtdmin(entity.getQtdmin());
    dto.setQtdmax(entity.getQtdmax());
    dto.setDocumentoLegalId(entity.getDocumentoLegalId());
    if (entity.getDocumentoLegal() != null) {
      dto.setDocumentoLegalNumero(entity.getDocumentoLegal().getNumeroDocumento());
    }
    return dto;
  }

  @Override
  public CodigoFrequenciaId extractId(CodigoFrequencia entity) {
    return entity.getId();
  }

  @Override
  public CodigoFrequencia newEntity(Void unused) {
    throw new UnsupportedOperationException("Catálogo somente de leitura.");
  }

  @Override
  public void applyUpdate(CodigoFrequencia entity, Void unused) {
    throw new UnsupportedOperationException("Catálogo somente de leitura.");
  }
}
