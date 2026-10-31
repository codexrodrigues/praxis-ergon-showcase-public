package br.com.praxis.ergonshowcase.service;

import br.com.praxis.ergonshowcase.domain.TipoDependente;
import br.com.praxis.ergonshowcase.dto.TipoDependenteFilterDTO;
import br.com.praxis.ergonshowcase.dto.TipoDependenteResponseDTO;
import br.com.praxis.ergonshowcase.mapper.TipoDependenteMapper;
import br.com.praxis.ergonshowcase.repository.TipoDependenteRepository;
import java.util.OptionalLong;
import org.praxisplatform.uischema.service.base.AbstractReadOnlyResourceService;
import org.springframework.stereotype.Service;

/** Serviço estritamente de leitura para o catálogo de tipos de dependente (TIPO_DEPEN). */
@Service
public class TipoDependenteService
    extends AbstractReadOnlyResourceService<
        TipoDependente, TipoDependenteResponseDTO, String, TipoDependenteFilterDTO> {
  private final TipoDependenteMapper mapper;

  public TipoDependenteService(
      TipoDependenteRepository repository, TipoDependenteMapper mapper) {
    super(repository, TipoDependente.class);
    this.mapper = mapper;
  }

  @Override
  protected TipoDependenteMapper getResourceMapper() {
    return mapper;
  }

  @Override
  protected OptionalLong getEntityResourceVersion(TipoDependente entity) {
    return OptionalLong.of(entity.getVersion());
  }
}
