package br.com.praxis.ergonshowcase.mapper;

import br.com.praxis.ergonshowcase.domain.*;
import br.com.praxis.ergonshowcase.dto.*;
import org.praxisplatform.uischema.mapper.base.ResourceMapper;
import org.springframework.stereotype.Component;

/** Fronteira explícita: somente campos cadastrais são copiados na manutenção. */
@Component
public class VinculoMapper
    implements ResourceMapper<
        Vinculo, VinculoResponseDTO, CreateVinculoDTO, UpdateVinculoDTO, VinculoId> {

  public VinculoResponseDTO toResponse(Vinculo entity) {
    var dto = new VinculoResponseDTO();
    dto.setMatric(entity.getMatric());
    dto.setRegimejur(entity.getRegimejur());
    dto.setTipovinc(entity.getTipovinc());
    dto.setCategoria(entity.getCategoria());
    dto.setCargo(entity.getCargo());
    dto.setReferencia(entity.getReferencia());
    dto.setJornadaSemanal(entity.getJornadaSemanal());
    dto.setCustomFields(entity.getCustomFields());
    dto.setId(entity.getId());
    dto.setEmpCodigo(entity.getEmpCodigo());
    dto.setOrgaoNome(entity.getOrgao() == null ? null : entity.getOrgao().getNome());
    dto.setNumfunc(entity.getId().numfunc());
    dto.setNumero(entity.getId().numero());
    dto.setPessoaNome(entity.getPessoa().getNome());
    dto.setCargoNome(
        entity.getCargoEntidade() == null ? null : entity.getCargoEntidade().getNome());
    dto.setSituacao(entity.getSituacao());
    dto.setDtexerc(entity.getDtexerc());
    dto.setDtvac(entity.getDtvac());
    dto.setDiasDesdeExercicio(
        java.time.temporal.ChronoUnit.DAYS.between(
            entity.getDtexerc(),
            entity.getDtvac() == null
                ? java.time.LocalDate.now(java.time.ZoneOffset.UTC)
                : entity.getDtvac()));
    dto.setMatriculaExterna(entity.getMatriculaExterna());
    dto.setSubcategoria(entity.getSubcategoria());
    dto.setSetor(entity.getSetor());
    dto.setSetorExercicio(entity.getSetorExercicio());
    dto.setCargoComissao(entity.getCargoComissao());
    dto.setDocumentoLegalId(entity.getDocumentoLegalId());
    dto.setAtoPublicacaoId(entity.getAtoPublicacaoId());
    dto.setDtnom(entity.getDtnom());
    dto.setDtposse(entity.getDtposse());
    dto.setDtadmRais(entity.getDtadmRais());
    dto.setClassifconc(entity.getClassifconc());
    dto.setDtconc(entity.getDtconc());
    dto.setNumvincant(entity.getNumvincant());
    dto.setNumvincpos(entity.getNumvincpos());
    dto.setMotivo(entity.getMotivo());
    dto.setDtinicontr(entity.getDtinicontr());
    dto.setDtfimcontr(entity.getDtfimcontr());
    dto.setDtprorrogcontr(entity.getDtprorrogcontr());
    dto.setRegimeprev(entity.getRegimeprev());
    dto.setPlanoprev(entity.getPlanoprev());
    dto.setDescontaIr(entity.getDescontaIr());
    dto.setCorreio(entity.getCorreio());
    dto.setInfoCota(entity.getInfoCota());
    dto.setBanco(entity.getBanco());
    dto.setAgencia(entity.getAgencia());
    dto.setConta(entity.getConta());
    dto.setTipopag(entity.getTipopag());
    dto.setDtopfgts(entity.getDtopfgts());
    dto.setDtretrfgts(entity.getDtretrfgts());
    dto.setBancofgts(entity.getBancofgts());
    dto.setAgenciafgts(entity.getAgenciafgts());
    dto.setContafgts(entity.getContafgts());
    dto.setValorDepFgts(entity.getValorDepFgts());
    dto.setDtHomologEstagio(entity.getDtHomologEstagio());
    dto.setTipoReq(entity.getTipoReq());
    dto.setOrgaoReq(entity.getOrgaoReq());
    dto.setFuncaoReq(entity.getFuncaoReq());
    dto.setTipoOnusReq(entity.getTipoOnusReq());
    dto.setTipoRessarcReq(entity.getTipoRessarcReq());
    dto.setDtiniCessao(entity.getDtiniCessao());
    dto.setDtfimCessao(entity.getDtfimCessao());
    dto.setDtaposent(entity.getDtaposent());
    dto.setTipoapos(entity.getTipoapos());
    dto.setFatorProporcAposent(entity.getFatorProporcAposent());
    dto.setFormavac(entity.getFormavac());
    dto.setMotivovac(entity.getMotivovac());
    dto.setDtPgtoAte(entity.getDtPgtoAte());
    dto.setSetorNome(entity.getLotacao() == null ? null : entity.getLotacao().getNome());
    dto.setSetorExercicioNome(
        entity.getExercicio() == null ? null : entity.getExercicio().getNome());
    dto.setEmEstagioProbatorio(
        "ATIVO".equals(entity.getSituacao())
            && entity.getDtHomologEstagio() == null
            && entity.getDtexerc() != null);
    dto.setPossuiCargoComissao(entity.getCargoComissao() != null);
    dto.setEmCessaoAtiva("CEDIDO".equals(entity.getSituacao()));
    return dto;
  }

  public Vinculo newEntity(CreateVinculoDTO dto) {
    var entity = new Vinculo();
    entity.setMatric(dto.getMatric());
    entity.setRegimejur(dto.getRegimejur());
    entity.setTipovinc(dto.getTipovinc());
    entity.setCategoria(dto.getCategoria());
    entity.setCargo(dto.getCargo());
    entity.setReferencia(dto.getReferencia());
    entity.setJornadaSemanal(dto.getJornadaSemanal());
    entity.setCustomFields(dto.getCustomFields());
    entity.setId(new VinculoId(dto.getNumfunc(), dto.getNumero()));
    entity.setEmpCodigo(dto.getEmpCodigo());
    entity.setDtexerc(dto.getDtexerc());
    entity.setSituacao("ATIVO");
    entity.setMatriculaExterna(dto.getMatriculaExterna());
    entity.setSubcategoria(dto.getSubcategoria());
    entity.setSetor(dto.getSetor());
    entity.setSetorExercicio(dto.getSetorExercicio());
    entity.setCargoComissao(dto.getCargoComissao());
    entity.setDocumentoLegalId(dto.getDocumentoLegalId());
    entity.setAtoPublicacaoId(dto.getAtoPublicacaoId());
    entity.setDtnom(dto.getDtnom());
    entity.setDtposse(dto.getDtposse());
    entity.setDtadmRais(dto.getDtadmRais());
    entity.setClassifconc(dto.getClassifconc());
    entity.setDtconc(dto.getDtconc());
    entity.setNumvincant(dto.getNumvincant());
    entity.setNumvincpos(dto.getNumvincpos());
    entity.setMotivo(dto.getMotivo());
    entity.setDtinicontr(dto.getDtinicontr());
    entity.setDtfimcontr(dto.getDtfimcontr());
    entity.setDtprorrogcontr(dto.getDtprorrogcontr());
    entity.setRegimeprev(dto.getRegimeprev());
    entity.setPlanoprev(dto.getPlanoprev());
    entity.setDescontaIr(dto.getDescontaIr());
    entity.setCorreio(dto.getCorreio());
    entity.setInfoCota(dto.getInfoCota());
    entity.setBanco(dto.getBanco());
    entity.setAgencia(dto.getAgencia());
    entity.setConta(dto.getConta());
    entity.setTipopag(dto.getTipopag());
    entity.setDtopfgts(dto.getDtopfgts());
    entity.setDtretrfgts(dto.getDtretrfgts());
    entity.setBancofgts(dto.getBancofgts());
    entity.setAgenciafgts(dto.getAgenciafgts());
    entity.setContafgts(dto.getContafgts());
    entity.setValorDepFgts(dto.getValorDepFgts());
    return entity;
  }

  public void applyUpdate(Vinculo entity, UpdateVinculoDTO dto) {
    entity.setMatric(dto.getMatric());
    entity.setRegimejur(dto.getRegimejur());
    entity.setTipovinc(dto.getTipovinc());
    entity.setCategoria(dto.getCategoria());
    entity.setCargo(dto.getCargo());
    entity.setReferencia(dto.getReferencia());
    entity.setJornadaSemanal(dto.getJornadaSemanal());
    entity.setCustomFields(dto.getCustomFields());
    entity.setMatriculaExterna(dto.getMatriculaExterna());
    entity.setSubcategoria(dto.getSubcategoria());
    entity.setSetor(dto.getSetor());
    entity.setSetorExercicio(dto.getSetorExercicio());
    entity.setCargoComissao(dto.getCargoComissao());
    entity.setDocumentoLegalId(dto.getDocumentoLegalId());
    entity.setAtoPublicacaoId(dto.getAtoPublicacaoId());
    entity.setDtnom(dto.getDtnom());
    entity.setDtposse(dto.getDtposse());
    entity.setDtadmRais(dto.getDtadmRais());
    entity.setClassifconc(dto.getClassifconc());
    entity.setDtconc(dto.getDtconc());
    entity.setNumvincant(dto.getNumvincant());
    entity.setNumvincpos(dto.getNumvincpos());
    entity.setMotivo(dto.getMotivo());
    entity.setDtinicontr(dto.getDtinicontr());
    entity.setDtfimcontr(dto.getDtfimcontr());
    entity.setDtprorrogcontr(dto.getDtprorrogcontr());
    entity.setRegimeprev(dto.getRegimeprev());
    entity.setPlanoprev(dto.getPlanoprev());
    entity.setDescontaIr(dto.getDescontaIr());
    entity.setCorreio(dto.getCorreio());
    entity.setInfoCota(dto.getInfoCota());
    entity.setBanco(dto.getBanco());
    entity.setAgencia(dto.getAgencia());
    entity.setConta(dto.getConta());
    entity.setTipopag(dto.getTipopag());
    entity.setDtopfgts(dto.getDtopfgts());
    entity.setDtretrfgts(dto.getDtretrfgts());
    entity.setBancofgts(dto.getBancofgts());
    entity.setAgenciafgts(dto.getAgenciafgts());
    entity.setContafgts(dto.getContafgts());
    entity.setValorDepFgts(dto.getValorDepFgts());
  }

  public VinculoId extractId(Vinculo entity) {
    return entity.getId();
  }
}
