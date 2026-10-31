package br.com.praxis.ergonshowcase.mapper;

import br.com.praxis.ergonshowcase.domain.DefinicaoCampoCustomizado;
import br.com.praxis.ergonshowcase.dto.CreateDefinicaoCampoCustomizadoDTO;
import br.com.praxis.ergonshowcase.dto.DefinicaoCampoCustomizadoDados;
import br.com.praxis.ergonshowcase.dto.DefinicaoCampoCustomizadoResponseDTO;
import br.com.praxis.ergonshowcase.dto.UpdateDefinicaoCampoCustomizadoDTO;
import java.util.LinkedHashMap;
import org.praxisplatform.uischema.mapper.base.ResourceMapper;
import org.springframework.stereotype.Component;

@Component
public class DefinicaoCampoCustomizadoMapper
    implements ResourceMapper<
        DefinicaoCampoCustomizado,
        DefinicaoCampoCustomizadoResponseDTO,
        CreateDefinicaoCampoCustomizadoDTO,
        UpdateDefinicaoCampoCustomizadoDTO,
        Long> {

  @Override
  public DefinicaoCampoCustomizadoResponseDTO toResponse(DefinicaoCampoCustomizado entity) {
    DefinicaoCampoCustomizadoResponseDTO dto = new DefinicaoCampoCustomizadoResponseDTO();
    dto.setId(entity.getId());
    dto.setEmpCodigo(entity.getEmpCodigo());
    dto.setResourceKey(entity.getResourceKey());
    dto.setFieldKey(entity.getFieldKey());
    dto.setLabel(entity.getLabel());
    dto.setHelpText(entity.getHelpText());
    dto.setGroupName(entity.getGroupName());
    dto.setFieldOrder(entity.getFieldOrder());
    dto.setFieldType(entity.getFieldType());
    dto.setControlType(entity.getControlType());
    dto.setRequired(entity.getRequired());
    dto.setOptionTabela(entity.getOptionTabela());
    dto.setValidationRules(
        entity.getValidationRules() != null
            ? new LinkedHashMap<>(entity.getValidationRules())
            : new LinkedHashMap<>());
    dto.setGovernancePolicy(
        entity.getGovernancePolicy() != null
            ? new LinkedHashMap<>(entity.getGovernancePolicy())
            : new LinkedHashMap<>());
    dto.setActive(entity.getActive());
    if (entity.getAuditoria() != null) {
      dto.setUpdatedAt(entity.getAuditoria().getUpdatedAt());
    }
    if (entity.getOrgao() != null) {
      dto.setOrgaoNome(entity.getOrgao().getNome());
    }
    if (entity.getOptionTabelaEntidade() != null) {
      dto.setOptionTabelaDescricao(entity.getOptionTabelaEntidade().getDescr());
    }
    dto.setPossuiTabelaOpcoes(
        entity.getOptionTabela() != null && !entity.getOptionTabela().isBlank());
    dto.setPossuiRegrasValidacao(
        entity.getValidationRules() != null && !entity.getValidationRules().isEmpty());
    dto.setObrigatorioAtivo(
        Boolean.TRUE.equals(entity.getRequired()) && Boolean.TRUE.equals(entity.getActive()));
    return dto;
  }

  @Override
  public DefinicaoCampoCustomizado newEntity(CreateDefinicaoCampoCustomizadoDTO dto) {
    DefinicaoCampoCustomizado entity = new DefinicaoCampoCustomizado();
    entity.setActive(true);
    applyShared(entity, dto);
    return entity;
  }

  @Override
  public void applyUpdate(DefinicaoCampoCustomizado entity, UpdateDefinicaoCampoCustomizadoDTO dto) {
    applyShared(entity, dto);
  }

  private void applyShared(DefinicaoCampoCustomizado entity, DefinicaoCampoCustomizadoDados dto) {
    entity.setEmpCodigo(dto.getEmpCodigo());
    entity.setResourceKey(dto.getResourceKey());
    entity.setFieldKey(dto.getFieldKey());
    entity.setLabel(dto.getLabel());
    entity.setHelpText(dto.getHelpText());
    entity.setGroupName(dto.getGroupName());
    entity.setFieldOrder(dto.getFieldOrder());
    entity.setFieldType(dto.getFieldType());
    entity.setControlType(dto.getControlType());
    entity.setRequired(dto.getRequired());
    entity.setOptionTabela(dto.getOptionTabela());
    entity.setValidationRules(
        dto.getValidationRules() != null
            ? new LinkedHashMap<>(dto.getValidationRules())
            : new LinkedHashMap<>());
    entity.setGovernancePolicy(
        dto.getGovernancePolicy() != null
            ? new LinkedHashMap<>(dto.getGovernancePolicy())
            : new LinkedHashMap<>());
  }

  @Override
  public Long extractId(DefinicaoCampoCustomizado entity) {
    return entity.getId();
  }
}
