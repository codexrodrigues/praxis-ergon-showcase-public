package br.com.praxis.ergonshowcase.mapper;

import br.com.praxis.ergonshowcase.domain.CodigoFrequenciaId;
import br.com.praxis.ergonshowcase.domain.RegraFrequenciaEstatutaria;
import br.com.praxis.ergonshowcase.dto.RegraFrequenciaEstatutariaResponseDTO;
import org.praxisplatform.uischema.mapper.base.ResourceMapper;
import org.springframework.stereotype.Component;

@Component
public class RegraFrequenciaEstatutariaMapper
    implements ResourceMapper<
        RegraFrequenciaEstatutaria, RegraFrequenciaEstatutariaResponseDTO, Void, Void, Long> {

  @Override
  public RegraFrequenciaEstatutariaResponseDTO toResponse(RegraFrequenciaEstatutaria entity) {
    RegraFrequenciaEstatutariaResponseDTO dto = new RegraFrequenciaEstatutariaResponseDTO();
    dto.setId(entity.getId());
    dto.setEmpCodigo(entity.getEmpCodigo());
    dto.setRegimeJuridico(entity.getRegimeJuridico());
    dto.setTipoVinculo(entity.getTipoVinculo());
    dto.setCategoria(entity.getCategoria());
    dto.setSubcategoria(entity.getSubcategoria());
    dto.setTipofreq(entity.getTipofreq());
    dto.setCodfreq(entity.getCodfreq());
    if (entity.getTipofreq() != null && entity.getCodfreq() != null) {
      dto.setCodigoFrequenciaId(new CodigoFrequenciaId(entity.getTipofreq(), entity.getCodfreq()));
    }
    dto.setDtini(entity.getDtini());
    dto.setDtfim(entity.getDtfim());
    dto.setQtdMinDias(entity.getQtdMinDias());
    dto.setQtdMaxDias(entity.getQtdMaxDias());
    dto.setIntervMinDias(entity.getIntervMinDias());
    dto.setIntProrrogDias(entity.getIntProrrogDias());
    dto.setQtdMaxConsecMes(entity.getQtdMaxConsecMes());
    dto.setQtdMaxInterpMes(entity.getQtdMaxInterpMes());
    dto.setQtdMaxConsecAno(entity.getQtdMaxConsecAno());
    dto.setQtdMaxInterpAno(entity.getQtdMaxInterpAno());
    dto.setExigeHomologacao(entity.getExigeHomologacao());
    dto.setPermiteConcomitancia(entity.getPermiteConcomitancia());
    dto.setNaoSeAplica(entity.getNaoSeAplica());
    dto.setRuleKey(entity.getRuleKey());
    dto.setRuleParams(entity.getRuleParams());
    dto.setDocumentoLegalId(entity.getDocumentoLegalId());
    dto.setAtiva(entity.getAtiva());
    if (entity.getOrgao() != null) {
      dto.setOrgaoNome(entity.getOrgao().getNome());
    }
    if (entity.getTipoFrequenciaEntidade() != null) {
      dto.setTipoFrequenciaDescricao(entity.getTipoFrequenciaEntidade().getNome());
    }
    if (entity.getCodigoFrequenciaEntidade() != null) {
      dto.setCodigoFrequenciaNome(entity.getCodigoFrequenciaEntidade().getNome());
    }
    if (entity.getDocumentoLegal() != null) {
      dto.setDocumentoLegalNumero(entity.getDocumentoLegal().getNumeroDocumento());
    }
    return dto;
  }

  @Override
  public RegraFrequenciaEstatutaria newEntity(Void dto) {
    throw new UnsupportedOperationException("Catálogo somente leitura");
  }

  @Override
  public void applyUpdate(RegraFrequenciaEstatutaria entity, Void dto) {
    throw new UnsupportedOperationException("Catálogo somente leitura");
  }

  @Override
  public Long extractId(RegraFrequenciaEstatutaria entity) {
    return entity.getId();
  }
}
