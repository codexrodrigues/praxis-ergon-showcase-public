package br.com.praxis.ergonshowcase.service;

import br.com.praxis.ergonshowcase.domain.TipoFrequencia;
import br.com.praxis.ergonshowcase.dto.TipoFrequenciaFilterDTO;
import br.com.praxis.ergonshowcase.dto.TipoFrequenciaResponseDTO;
import br.com.praxis.ergonshowcase.mapper.TipoFrequenciaMapper;
import br.com.praxis.ergonshowcase.repository.TipoFrequenciaRepository;
import java.util.OptionalLong;
import org.praxisplatform.uischema.service.base.AbstractReadOnlyResourceService;
import org.springframework.stereotype.Service;

/** Serviço estritamente de leitura para o catálogo de tipos macro de frequência (TIPO_FREQ_). */
@Service
public class TipoFrequenciaService
    extends AbstractReadOnlyResourceService<
        TipoFrequencia, TipoFrequenciaResponseDTO, String, TipoFrequenciaFilterDTO> {
  private final TipoFrequenciaMapper mapper;

  public TipoFrequenciaService(
      TipoFrequenciaRepository repository, TipoFrequenciaMapper mapper) {
    super(repository, TipoFrequencia.class);
    this.mapper = mapper;
  }

  @Override
  protected TipoFrequenciaMapper getResourceMapper() {
    return mapper;
  }

  @Override
  protected OptionalLong getEntityResourceVersion(TipoFrequencia entity) {
    return OptionalLong.of(entity.getVersion());
  }
}
