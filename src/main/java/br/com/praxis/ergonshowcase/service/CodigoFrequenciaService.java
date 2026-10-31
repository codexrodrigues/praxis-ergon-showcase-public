package br.com.praxis.ergonshowcase.service;

import br.com.praxis.ergonshowcase.domain.CodigoFrequencia;
import br.com.praxis.ergonshowcase.domain.CodigoFrequenciaId;
import br.com.praxis.ergonshowcase.dto.CodigoFrequenciaFilterDTO;
import br.com.praxis.ergonshowcase.dto.CodigoFrequenciaResponseDTO;
import br.com.praxis.ergonshowcase.mapper.CodigoFrequenciaMapper;
import br.com.praxis.ergonshowcase.repository.CodigoFrequenciaRepository;
import java.util.OptionalLong;
import org.praxisplatform.uischema.service.base.AbstractReadOnlyResourceService;
import org.springframework.stereotype.Service;

/** Serviço estritamente de leitura para o catálogo de códigos de frequência (CODIGOS_FREQ_). */
@Service
public class CodigoFrequenciaService
    extends AbstractReadOnlyResourceService<
        CodigoFrequencia,
        CodigoFrequenciaResponseDTO,
        CodigoFrequenciaId,
        CodigoFrequenciaFilterDTO> {
  private final CodigoFrequenciaMapper mapper;

  public CodigoFrequenciaService(
      CodigoFrequenciaRepository repository, CodigoFrequenciaMapper mapper) {
    super(repository, CodigoFrequencia.class);
    this.mapper = mapper;
  }

  @Override
  protected CodigoFrequenciaMapper getResourceMapper() {
    return mapper;
  }

  @Override
  protected OptionalLong getEntityResourceVersion(CodigoFrequencia entity) {
    return OptionalLong.of(entity.getVersion());
  }
}
