package br.com.praxis.ergonshowcase.mapper;

import br.com.praxis.ergonshowcase.domain.Tabela;
import br.com.praxis.ergonshowcase.dto.TabelaResponseDTO;
import org.praxisplatform.uischema.mapper.base.ResourceMapper;
import org.springframework.stereotype.Component;

/** Projeção de leitura do catálogo mestre de tabelas gerais (HADES.TABELA). */
@Component
public class TabelaMapper implements ResourceMapper<Tabela, TabelaResponseDTO, Void, Void, String> {
  @Override
  public TabelaResponseDTO toResponse(Tabela entity) {
    TabelaResponseDTO dto = new TabelaResponseDTO();
    dto.setId(entity.getTab());
    dto.setTab(entity.getTab());
    dto.setDescr(entity.getDescr());
    dto.setSis(entity.getSis());
    dto.setMultiEmp(entity.getMultiEmp());
    dto.setTipo(entity.getTipo());
    dto.setTaminfo(entity.getTaminfo());
    dto.setLabelItem1(entity.getLabelItem1());
    dto.setLabelItem2(entity.getLabelItem2());
    dto.setLabelItem3(entity.getLabelItem3());
    if (entity.getAuditoria() != null) {
      dto.setUpdatedAt(entity.getAuditoria().getUpdatedAt());
    }
    return dto;
  }

  @Override
  public String extractId(Tabela entity) {
    return entity.getTab();
  }

  @Override
  public Tabela newEntity(Void unused) {
    throw new UnsupportedOperationException("Catálogo somente de leitura.");
  }

  @Override
  public void applyUpdate(Tabela entity, Void unused) {
    throw new UnsupportedOperationException("Catálogo somente de leitura.");
  }
}
