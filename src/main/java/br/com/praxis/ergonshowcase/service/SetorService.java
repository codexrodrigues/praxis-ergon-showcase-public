package br.com.praxis.ergonshowcase.service;

import br.com.praxis.ergonshowcase.domain.*;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.mapper.*;
import br.com.praxis.ergonshowcase.repository.*;
import java.util.OptionalLong;
import org.praxisplatform.uischema.service.base.AbstractReadOnlyResourceService;
import org.springframework.stereotype.Service;

/** Fronteira estritamente de leitura para referências e inspeção contextual. */
@Service
public class SetorService
    extends AbstractReadOnlyResourceService<Setor, SetorResponseDTO, String, SetorFilterDTO> {
  private final SetorMapper mapper;

  public SetorService(SetorRepository repository, SetorMapper mapper) {
    super(repository, Setor.class);
    this.mapper = mapper;
  }

  @Override
  protected SetorMapper getResourceMapper() {
    return mapper;
  }

  @Override
  protected OptionalLong getEntityResourceVersion(Setor entity) {
    return OptionalLong.of(entity.getVersion());
  }
}
