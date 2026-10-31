package br.com.praxis.ergonshowcase.service;

import br.com.praxis.ergonshowcase.domain.ItemTabela;
import br.com.praxis.ergonshowcase.domain.ItemTabelaId;
import br.com.praxis.ergonshowcase.dto.ItemTabelaFilterDTO;
import br.com.praxis.ergonshowcase.dto.ItemTabelaResponseDTO;
import br.com.praxis.ergonshowcase.mapper.ItemTabelaMapper;
import br.com.praxis.ergonshowcase.repository.ItemTabelaRepository;
import java.util.OptionalLong;
import org.praxisplatform.uischema.service.base.AbstractReadOnlyResourceService;
import org.springframework.stereotype.Service;

/** Serviço estritamente de leitura para os itens de tabelas gerais de domínio (HADES.ITEMTABELA_). */
@Service
public class ItemTabelaService
    extends AbstractReadOnlyResourceService<
        ItemTabela, ItemTabelaResponseDTO, ItemTabelaId, ItemTabelaFilterDTO> {
  private final ItemTabelaMapper mapper;

  public ItemTabelaService(ItemTabelaRepository repository, ItemTabelaMapper mapper) {
    super(repository, ItemTabela.class);
    this.mapper = mapper;
  }

  @Override
  protected ItemTabelaMapper getResourceMapper() {
    return mapper;
  }

  @Override
  protected OptionalLong getEntityResourceVersion(ItemTabela entity) {
    return OptionalLong.of(entity.getVersion());
  }
}
