package br.com.praxis.ergonshowcase.service;

import br.com.praxis.ergonshowcase.domain.Tabela;
import br.com.praxis.ergonshowcase.dto.TabelaFilterDTO;
import br.com.praxis.ergonshowcase.dto.TabelaResponseDTO;
import br.com.praxis.ergonshowcase.mapper.TabelaMapper;
import br.com.praxis.ergonshowcase.repository.TabelaRepository;
import java.util.OptionalLong;
import org.praxisplatform.uischema.service.base.AbstractReadOnlyResourceService;
import org.springframework.stereotype.Service;

/** Serviço estritamente de leitura para o catálogo mestre de tabelas gerais (HADES.TABELA). */
@Service
public class TabelaService
    extends AbstractReadOnlyResourceService<Tabela, TabelaResponseDTO, String, TabelaFilterDTO> {
  private final TabelaMapper mapper;

  public TabelaService(TabelaRepository repository, TabelaMapper mapper) {
    super(repository, Tabela.class);
    this.mapper = mapper;
  }

  @Override
  protected TabelaMapper getResourceMapper() {
    return mapper;
  }

  @Override
  protected OptionalLong getEntityResourceVersion(Tabela entity) {
    return OptionalLong.of(entity.getVersion());
  }
}
