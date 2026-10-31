package br.com.praxis.ergonshowcase.mapper;

import br.com.praxis.ergonshowcase.domain.ItemTabela;
import br.com.praxis.ergonshowcase.domain.ItemTabelaId;
import br.com.praxis.ergonshowcase.dto.ItemTabelaResponseDTO;
import org.praxisplatform.uischema.mapper.base.ResourceMapper;
import org.springframework.stereotype.Component;

/** Projeção de leitura dos itens das tabelas gerais de domínio (HADES.ITEMTABELA_). */
@Component
public class ItemTabelaMapper
    implements ResourceMapper<ItemTabela, ItemTabelaResponseDTO, Void, Void, ItemTabelaId> {
  @Override
  public ItemTabelaResponseDTO toResponse(ItemTabela entity) {
    ItemTabelaResponseDTO dto = new ItemTabelaResponseDTO();
    dto.setId(entity.getId());
    if (entity.getId() != null) {
      dto.setTab(entity.getId().tab());
      dto.setItem(entity.getId().item());
    }
    if (entity.getTabelaEntidade() != null) {
      dto.setTabelaDescricao(entity.getTabelaEntidade().getDescr());
    }
    dto.setDescr(entity.getDescr());
    dto.setEmpCodigo(entity.getEmpCodigo());
    if (entity.getOrgao() != null) {
      dto.setOrgaoNome(entity.getOrgao().getNome());
    }
    dto.setOrdem(entity.getOrdem());
    dto.setAtivo(entity.getAtivo());
    dto.setItem1(entity.getItem1());
    dto.setItem2(entity.getItem2());
    dto.setItem3(entity.getItem3());
    dto.setItem4(entity.getItem4());
    if (entity.getAuditoria() != null) {
      dto.setUpdatedAt(entity.getAuditoria().getUpdatedAt());
    }
    return dto;
  }

  @Override
  public ItemTabelaId extractId(ItemTabela entity) {
    return entity.getId();
  }

  @Override
  public ItemTabela newEntity(Void unused) {
    throw new UnsupportedOperationException("Catálogo somente de leitura.");
  }

  @Override
  public void applyUpdate(ItemTabela entity, Void unused) {
    throw new UnsupportedOperationException("Catálogo somente de leitura.");
  }
}
