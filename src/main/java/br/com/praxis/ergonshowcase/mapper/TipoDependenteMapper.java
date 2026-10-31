package br.com.praxis.ergonshowcase.mapper;

import br.com.praxis.ergonshowcase.domain.TipoDependente;
import br.com.praxis.ergonshowcase.dto.TipoDependenteResponseDTO;
import org.praxisplatform.uischema.mapper.base.ResourceMapper;
import org.springframework.stereotype.Component;

/** Projeção de leitura do catálogo de tipos de dependente sem acesso adicional ao banco. */
@Component
public class TipoDependenteMapper
    implements ResourceMapper<TipoDependente, TipoDependenteResponseDTO, Void, Void, String> {

  @Override
  public TipoDependenteResponseDTO toResponse(TipoDependente entity) {
    var dto = new TipoDependenteResponseDTO();
    dto.setId(entity.getTipodepen());
    dto.setDescricao(entity.getDescricao());
    dto.setGeraIr(entity.getGeraIr());
    dto.setGeraSalarioFamilia(entity.getGeraSalarioFamilia());
    dto.setIdadeLimite(entity.getIdadeLimite());
    dto.setDocumentoLegalId(entity.getDocumentoLegalId());
    dto.setDocumentoLegalNumero(
        entity.getDocumentoLegal() == null ? null : entity.getDocumentoLegal().getNumeroDocumento());
    return dto;
  }

  @Override
  public String extractId(TipoDependente entity) {
    return entity.getTipodepen();
  }

  @Override
  public TipoDependente newEntity(Void unused) {
    throw new UnsupportedOperationException("Catálogo somente de leitura.");
  }

  @Override
  public void applyUpdate(TipoDependente entity, Void unused) {
    throw new UnsupportedOperationException("Catálogo somente de leitura.");
  }
}
