package br.com.praxis.ergonshowcase.mapper;

import br.com.praxis.ergonshowcase.config.TenantContext;
import br.com.praxis.ergonshowcase.domain.ParametroContagemFerias;
import br.com.praxis.ergonshowcase.dto.*;
import java.util.LinkedHashMap;
import org.praxisplatform.uischema.mapper.base.ResourceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ParametroContagemFeriasMapper
    implements ResourceMapper<
        ParametroContagemFerias,
        ParametroContagemFeriasResponseDTO,
        CreateParametroContagemFeriasDTO,
        UpdateParametroContagemFeriasDTO,
        Long> {

  private final TenantContext tenant;

  public ParametroContagemFeriasMapper() {
    this(new TenantContext());
  }

  @Autowired
  public ParametroContagemFeriasMapper(TenantContext tenant) {
    this.tenant = tenant != null ? tenant : new TenantContext();
  }

  @Override
  public ParametroContagemFeriasResponseDTO toResponse(ParametroContagemFerias entity) {
    var dto = new ParametroContagemFeriasResponseDTO();
    dto.setId(entity.getId());
    dto.setEmpCodigo(entity.getEmpCodigo());
    dto.setOrgaoNome(entity.getOrgao() != null ? entity.getOrgao().getNome() : null);
    dto.setRegimeJuridico(entity.getRegimeJuridico());
    dto.setTipoVinculo(entity.getTipoVinculo());
    dto.setCategoria(entity.getCategoria());
    dto.setSubcategoria(entity.getSubcategoria());
    dto.setDtini(entity.getDtini());
    dto.setDtfim(entity.getDtfim());
    dto.setTemFerias(entity.getTemFerias());
    dto.setTotDiasFer(entity.getTotDiasFer());
    dto.setMinPerFer(entity.getMinPerFer());
    dto.setMaxPerFer(entity.getMaxPerFer());
    dto.setMinDiasPerFer(entity.getMinDiasPerFer());
    dto.setMaxDiasPerFer(entity.getMaxDiasPerFer());
    dto.setMinDiasPerPrincFer(entity.getMinDiasPerPrincFer());
    dto.setAbonoDirGozo(entity.getAbonoDirGozo());
    dto.setNumDiasAbonoFer(entity.getNumDiasAbonoFer());
    dto.setDenDiasAbonoFer(entity.getDenDiasAbonoFer());
    dto.setMinDiasPvendFer(entity.getMinDiasPvendFer());
    dto.setMaxDiasVendFer(entity.getMaxDiasVendFer());
    dto.setAbonoConstFer(entity.getAbonoConstFer());
    dto.setAdiant13Fer(entity.getAdiant13Fer());
    dto.setAdiantSalFer(entity.getAdiantSalFer());
    dto.setEmprestFer(entity.getEmprestFer());
    dto.setPermiteGozoAntesPaFer(entity.getPermiteGozoAntesPaFer());
    dto.setPermiteFerPrimPer(entity.getPermiteFerPrimPer());
    dto.setPermiteFerSegunPer(entity.getPermiteFerSegunPer());
    dto.setPermiteFerTercPer(entity.getPermiteFerTercPer());
    dto.setPermiteFerPerInc(entity.getPermiteFerPerInc());
    dto.setCriterioPrescricao(entity.getCriterioPrescricao());
    dto.setPermiteGozoAposPrescricao(entity.getPermiteGozoAposPrescricao());
    dto.setPermiteLancConcPa(entity.getPermiteLancConcPa());
    dto.setExigeExercicio(entity.getExigeExercicio());
    dto.setSemPgtoMulta(entity.getSemPgtoMulta());
    dto.setContDobroFerias(entity.getContDobroFerias());
    dto.setMinDiasPeraqfer(entity.getMinDiasPeraqfer());
    dto.setMaxDiasPeraqfer(entity.getMaxDiasPeraqfer());
    dto.setTemLicEsp(entity.getTemLicEsp());
    dto.setAnosLicEsp(entity.getAnosLicEsp());
    dto.setMesesLicEspPer(entity.getMesesLicEspPer());
    dto.setVendLicEsp(entity.getVendLicEsp());
    dto.setAbonoDirGozoLicEsp(entity.getAbonoDirGozoLicEsp());
    dto.setMinDiasPvendLicEsp(entity.getMinDiasPvendLicEsp());
    dto.setMaxDiasVendLicEsp(entity.getMaxDiasVendLicEsp());
    dto.setNumDiasAbonoLicEsp(entity.getNumDiasAbonoLicEsp());
    dto.setDenDiasAbonoLicEsp(entity.getDenDiasAbonoLicEsp());
    dto.setContDobroLicEsp(entity.getContDobroLicEsp());
    dto.setMinDiasPcontDobLicEsp(entity.getMinDiasPcontDobLicEsp());
    dto.setMaxDiasContDobLicEsp(entity.getMaxDiasContDobLicEsp());
    dto.setTemProgressao(entity.getTemProgressao());
    dto.setAnosProgressao(entity.getAnosProgressao());
    dto.setTemTempoServ(entity.getTemTempoServ());
    dto.setAnos1TempoServ(entity.getAnos1TempoServ());
    dto.setTemSextaParte(entity.getTemSextaParte());
    dto.setAnosSextaParte(entity.getAnosSextaParte());
    dto.setDocumentoLegalFeriasId(entity.getDocumentoLegalFeriasId());
    dto.setDocumentoLegalFeriasNumero(
        entity.getDocumentoLegalFerias() != null
            ? entity.getDocumentoLegalFerias().getNumeroDocumento()
            : null);
    dto.setDocumentoLegalProgressaoId(entity.getDocumentoLegalProgressaoId());
    dto.setDocumentoLegalProgressaoNumero(
        entity.getDocumentoLegalProgressao() != null
            ? entity.getDocumentoLegalProgressao().getNumeroDocumento()
            : null);
    dto.setObservacoes(entity.getObservacoes());
    dto.setActive(entity.getActive());
    dto.setEscopoGlobal(entity.getEmpCodigo() == null);
    dto.setPermiteParcelamento(entity.getMaxPerFer() != null && entity.getMaxPerFer() > 1);
    dto.setPermiteAbonoPecuniario(
        Boolean.TRUE.equals(entity.getTemFerias())
            && entity.getAbonoDirGozo() != null
            && !"N".equals(entity.getAbonoDirGozo())
            && (entity.getMaxDiasVendFer() == null || entity.getMaxDiasVendFer() > 0));
    dto.setCustomFields(
        entity.getCustomFields() != null
            ? new LinkedHashMap<>(entity.getCustomFields())
            : new LinkedHashMap<>());
    return dto;
  }

  @Override
  public ParametroContagemFerias newEntity(CreateParametroContagemFeriasDTO dto) {
    var entity = new ParametroContagemFerias();
    apply(dto, entity);
    entity.setActive(true);
    return entity;
  }

  @Override
  public void applyUpdate(
      ParametroContagemFerias entity, UpdateParametroContagemFeriasDTO dto) {
    apply(dto, entity);
  }

  @Override
  public Long extractId(ParametroContagemFerias entity) {
    return entity.getId();
  }

  private void apply(ParametroContagemFeriasDados dto, ParametroContagemFerias entity) {
    entity.setEmpCodigo(dto.getEmpCodigo());
    entity.setRegimeJuridico(dto.getRegimeJuridico());
    entity.setRegimeJuridico(normUpper(entity.getRegimeJuridico()));
    entity.setTipoVinculo(dto.getTipoVinculo());
    entity.setTipoVinculo(normUpper(entity.getTipoVinculo()));
    entity.setCategoria(dto.getCategoria());
    entity.setCategoria(normUpper(entity.getCategoria()));
    entity.setSubcategoria(dto.getSubcategoria());
    entity.setSubcategoria(normUpper(entity.getSubcategoria()));
    entity.setDtini(dto.getDtini());
    entity.setDtfim(dto.getDtfim());
    entity.setTemFerias(dto.getTemFerias());
    entity.setTemFerias(!Boolean.FALSE.equals(entity.getTemFerias()));
    entity.setTotDiasFer(dto.getTotDiasFer());
    entity.setMinPerFer(dto.getMinPerFer());
    entity.setMaxPerFer(dto.getMaxPerFer());
    entity.setMinDiasPerFer(dto.getMinDiasPerFer());
    entity.setMaxDiasPerFer(dto.getMaxDiasPerFer());
    entity.setMinDiasPerPrincFer(dto.getMinDiasPerPrincFer());
    entity.setAbonoDirGozo(dto.getAbonoDirGozo());
    entity.setAbonoDirGozo(normUpper(entity.getAbonoDirGozo()));
    entity.setNumDiasAbonoFer(dto.getNumDiasAbonoFer());
    entity.setDenDiasAbonoFer(dto.getDenDiasAbonoFer());
    entity.setMinDiasPvendFer(dto.getMinDiasPvendFer());
    entity.setMaxDiasVendFer(dto.getMaxDiasVendFer());
    entity.setAbonoConstFer(dto.getAbonoConstFer());
    entity.setAbonoConstFer(!Boolean.FALSE.equals(entity.getAbonoConstFer()));
    entity.setAdiant13Fer(dto.getAdiant13Fer());
    entity.setAdiant13Fer(!Boolean.FALSE.equals(entity.getAdiant13Fer()));
    entity.setAdiantSalFer(dto.getAdiantSalFer());
    entity.setAdiantSalFer(Boolean.TRUE.equals(entity.getAdiantSalFer()));
    entity.setEmprestFer(dto.getEmprestFer());
    entity.setEmprestFer(Boolean.TRUE.equals(entity.getEmprestFer()));
    entity.setPermiteGozoAntesPaFer(dto.getPermiteGozoAntesPaFer());
    entity.setPermiteGozoAntesPaFer(Boolean.TRUE.equals(entity.getPermiteGozoAntesPaFer()));
    entity.setPermiteFerPrimPer(dto.getPermiteFerPrimPer());
    entity.setPermiteFerPrimPer(Boolean.TRUE.equals(entity.getPermiteFerPrimPer()));
    entity.setPermiteFerSegunPer(dto.getPermiteFerSegunPer());
    entity.setPermiteFerSegunPer(Boolean.TRUE.equals(entity.getPermiteFerSegunPer()));
    entity.setPermiteFerTercPer(dto.getPermiteFerTercPer());
    entity.setPermiteFerTercPer(Boolean.TRUE.equals(entity.getPermiteFerTercPer()));
    entity.setPermiteFerPerInc(dto.getPermiteFerPerInc());
    entity.setPermiteFerPerInc(Boolean.TRUE.equals(entity.getPermiteFerPerInc()));
    entity.setCriterioPrescricao(dto.getCriterioPrescricao());
    entity.setCriterioPrescricao(
        entity.getCriterioPrescricao() != null ? normUpper(entity.getCriterioPrescricao()) : "F");
    entity.setPermiteGozoAposPrescricao(dto.getPermiteGozoAposPrescricao());
    entity.setPermiteGozoAposPrescricao(Boolean.TRUE.equals(entity.getPermiteGozoAposPrescricao()));
    entity.setPermiteLancConcPa(dto.getPermiteLancConcPa());
    entity.setPermiteLancConcPa(Boolean.TRUE.equals(entity.getPermiteLancConcPa()));
    entity.setExigeExercicio(dto.getExigeExercicio());
    entity.setExigeExercicio(Boolean.TRUE.equals(entity.getExigeExercicio()));
    entity.setSemPgtoMulta(dto.getSemPgtoMulta());
    entity.setSemPgtoMulta(Boolean.TRUE.equals(entity.getSemPgtoMulta()));
    entity.setContDobroFerias(dto.getContDobroFerias());
    entity.setContDobroFerias(Boolean.TRUE.equals(entity.getContDobroFerias()));
    entity.setMinDiasPeraqfer(dto.getMinDiasPeraqfer());
    entity.setMaxDiasPeraqfer(dto.getMaxDiasPeraqfer());
    entity.setTemLicEsp(dto.getTemLicEsp());
    entity.setTemLicEsp(Boolean.TRUE.equals(entity.getTemLicEsp()));
    entity.setAnosLicEsp(dto.getAnosLicEsp());
    entity.setMesesLicEspPer(dto.getMesesLicEspPer());
    entity.setVendLicEsp(dto.getVendLicEsp());
    entity.setVendLicEsp(Boolean.TRUE.equals(entity.getVendLicEsp()));
    entity.setAbonoDirGozoLicEsp(dto.getAbonoDirGozoLicEsp());
    entity.setAbonoDirGozoLicEsp(normUpper(entity.getAbonoDirGozoLicEsp()));
    entity.setMinDiasPvendLicEsp(dto.getMinDiasPvendLicEsp());
    entity.setMaxDiasVendLicEsp(dto.getMaxDiasVendLicEsp());
    entity.setNumDiasAbonoLicEsp(dto.getNumDiasAbonoLicEsp());
    entity.setDenDiasAbonoLicEsp(dto.getDenDiasAbonoLicEsp());
    entity.setContDobroLicEsp(dto.getContDobroLicEsp());
    entity.setContDobroLicEsp(Boolean.TRUE.equals(entity.getContDobroLicEsp()));
    entity.setMinDiasPcontDobLicEsp(dto.getMinDiasPcontDobLicEsp());
    entity.setMaxDiasContDobLicEsp(dto.getMaxDiasContDobLicEsp());
    entity.setTemProgressao(dto.getTemProgressao());
    entity.setTemProgressao(Boolean.TRUE.equals(entity.getTemProgressao()));
    entity.setAnosProgressao(dto.getAnosProgressao());
    entity.setTemTempoServ(dto.getTemTempoServ());
    entity.setTemTempoServ(Boolean.TRUE.equals(entity.getTemTempoServ()));
    entity.setAnos1TempoServ(dto.getAnos1TempoServ());
    entity.setTemSextaParte(dto.getTemSextaParte());
    entity.setTemSextaParte(Boolean.TRUE.equals(entity.getTemSextaParte()));
    entity.setAnosSextaParte(dto.getAnosSextaParte());
    entity.setDocumentoLegalFeriasId(dto.getDocumentoLegalFeriasId());
    entity.setDocumentoLegalProgressaoId(dto.getDocumentoLegalProgressaoId());
    entity.setObservacoes(dto.getObservacoes());
    entity.setObservacoes(norm(entity.getObservacoes()));
    entity.setCustomFields(dto.getCustomFields());
    entity.setCustomFields(
        entity.getCustomFields() != null
            ? new LinkedHashMap<>(entity.getCustomFields())
            : new LinkedHashMap<>());
  }

  private static String norm(String s) {
    return s == null || s.isBlank() ? null : s.trim();
  }

  private static String normUpper(String s) {
    return s == null || s.isBlank() ? null : s.trim().toUpperCase();
  }
}
