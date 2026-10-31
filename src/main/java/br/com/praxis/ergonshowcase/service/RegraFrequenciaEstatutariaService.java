package br.com.praxis.ergonshowcase.service;

import br.com.praxis.ergonshowcase.domain.RegraFrequenciaEstatutaria;
import br.com.praxis.ergonshowcase.dto.RegraFrequenciaEstatutariaFilterDTO;
import br.com.praxis.ergonshowcase.dto.RegraFrequenciaEstatutariaResponseDTO;
import br.com.praxis.ergonshowcase.mapper.RegraFrequenciaEstatutariaMapper;
import br.com.praxis.ergonshowcase.repository.RegraFrequenciaEstatutariaRepository;
import java.util.OptionalLong;
import org.praxisplatform.uischema.service.base.AbstractReadOnlyResourceService;
import org.springframework.stereotype.Service;

/** Serviço estritamente de leitura para o catálogo parametrizado de regras estatutárias de frequência (REGRAS_FREQ). */
@Service
public class RegraFrequenciaEstatutariaService
    extends AbstractReadOnlyResourceService<
        RegraFrequenciaEstatutaria,
        RegraFrequenciaEstatutariaResponseDTO,
        Long,
        RegraFrequenciaEstatutariaFilterDTO> {
  private final RegraFrequenciaEstatutariaMapper mapper;

  public RegraFrequenciaEstatutariaService(
      RegraFrequenciaEstatutariaRepository repository, RegraFrequenciaEstatutariaMapper mapper) {
    super(repository, RegraFrequenciaEstatutaria.class);
    this.mapper = mapper;
  }

  @Override
  protected RegraFrequenciaEstatutariaMapper getResourceMapper() {
    return mapper;
  }

  @Override
  protected OptionalLong getEntityResourceVersion(RegraFrequenciaEstatutaria entity) {
    return OptionalLong.of(entity.getVersion());
  }
}
