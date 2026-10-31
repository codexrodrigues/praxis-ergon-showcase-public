package br.com.praxis.ergonshowcase.mapper;

import br.com.praxis.ergonshowcase.domain.TipoFrequencia;
import br.com.praxis.ergonshowcase.dto.TipoFrequenciaResponseDTO;
import org.praxisplatform.uischema.mapper.base.ResourceMapper;
import org.springframework.stereotype.Component;

/** Projeção de leitura do catálogo de tipos macro de frequência sem acesso adicional ao banco. */
@Component
public class TipoFrequenciaMapper
    implements ResourceMapper<TipoFrequencia, TipoFrequenciaResponseDTO, Void, Void, String> {
  @Override
  public TipoFrequenciaResponseDTO toResponse(TipoFrequencia entity) {
    TipoFrequenciaResponseDTO dto = new TipoFrequenciaResponseDTO();
    dto.setId(entity.getTipo());
    dto.setTipo(entity.getTipo());
    dto.setNome(entity.getNome());
    dto.setInfluiConta(entity.getInfluiConta());
    dto.setHoraExtra(entity.getHoraExtra());
    dto.setPercentualExtra(entity.getPercentualExtra());
    dto.setDocumentoLegalId(entity.getDocumentoLegalId());
    if (entity.getDocumentoLegal() != null) {
      dto.setDocumentoLegalNumero(entity.getDocumentoLegal().getNumeroDocumento());
    }
    return dto;
  }

  @Override
  public String extractId(TipoFrequencia entity) {
    return entity.getTipo();
  }

  @Override
  public TipoFrequencia newEntity(Void unused) {
    throw new UnsupportedOperationException("Catálogo somente de leitura.");
  }

  @Override
  public void applyUpdate(TipoFrequencia entity, Void unused) {
    throw new UnsupportedOperationException("Catálogo somente de leitura.");
  }
}
