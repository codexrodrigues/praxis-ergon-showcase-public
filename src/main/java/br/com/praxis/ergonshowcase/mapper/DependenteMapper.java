package br.com.praxis.ergonshowcase.mapper;

import br.com.praxis.ergonshowcase.domain.*;
import br.com.praxis.ergonshowcase.dto.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import org.praxisplatform.uischema.mapper.base.ResourceMapper;
import org.springframework.stereotype.Component;

/** Fronteira explícita de mapeamento cadastral e derivação determinística de dependentes. */
@Component
public class DependenteMapper
    implements ResourceMapper<
        Dependente,
        DependenteResponseDTO,
        CreateDependenteDTO,
        UpdateDependenteDTO,
        DependenteId> {

  @Override
  public DependenteResponseDTO toResponse(Dependente entity) {
    var dto = new DependenteResponseDTO();
    dto.setId(entity.getId());
    dto.setNumfunc(entity.getId() == null ? null : entity.getId().numfunc());
    dto.setNumdep(entity.getId() == null ? null : entity.getId().numdep());
    dto.setPessoaNome(entity.getPessoa() == null ? null : entity.getPessoa().getNome());
    dto.setNome(entity.getNome());
    dto.setNomeSocial(entity.getNomeSocial());
    dto.setSexo(entity.getSexo());
    dto.setDtnasc(entity.getDtnasc());
    dto.setParentesco(entity.getParentesco());
    dto.setTipodepen(entity.getTipodepen());
    dto.setTipoDependenteDescricao(
        entity.getTipoDependenteEntidade() == null
            ? null
            : entity.getTipoDependenteEntidade().getDescricao());
    dto.setCpf(entity.getCpf());
    dto.setNumrg(entity.getNumrg());
    dto.setTiporg(entity.getTiporg());
    dto.setOrgaorg(entity.getOrgaorg());
    dto.setUfrg(entity.getUfrg());
    dto.setExpedrg(entity.getExpedrg());
    dto.setDtini(entity.getDtini());
    dto.setDtfim(entity.getDtfim());
    dto.setDepIr(entity.getDepIr());
    dto.setDepSalarioFamilia(entity.getDepSalarioFamilia());
    dto.setDepPlanoSaude(entity.getDepPlanoSaude());
    dto.setInvalido(entity.getInvalido());
    dto.setEstudante(entity.getEstudante());
    dto.setUniversitario(entity.getUniversitario());
    dto.setExcepcional(entity.getExcepcional());
    dto.setRepresentanteLegal(entity.getRepresentanteLegal());
    dto.setTipodocCert(entity.getTipodocCert());
    dto.setNumCert(entity.getNumCert());
    dto.setLivroACert(entity.getLivroACert());
    dto.setFolhaCert(entity.getFolhaCert());
    dto.setCartorioCert(entity.getCartorioCert());
    dto.setUfCart(entity.getUfCart());
    dto.setMunicipioCart(entity.getMunicipioCart());
    dto.setMatriculaCert(entity.getMatriculaCert());
    dto.setDocumentoLegalId(entity.getDocumentoLegalId());
    dto.setDocumentoLegalNumero(
        entity.getDocumentoLegal() == null ? null : entity.getDocumentoLegal().getNumeroDocumento());
    dto.setCustomFields(entity.getCustomFields());

    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    Long idade =
        entity.getDtnasc() == null ? null : ChronoUnit.YEARS.between(entity.getDtnasc(), today);
    dto.setIdadeAnos(idade);
    boolean vigente =
        entity.getDtini() != null
            && !entity.getDtini().isAfter(today)
            && entity.getDtfim() == null;
    dto.setDependenteVigente(vigente);
    int tetoPadrao =
        Boolean.TRUE.equals(entity.getUniversitario())
            ? 24
            : (entity.getTipoDependenteEntidade() != null
                    && entity.getTipoDependenteEntidade().getIdadeLimite() != null
                ? entity.getTipoDependenteEntidade().getIdadeLimite()
                : 21);
    boolean dentroDoLimite =
        Boolean.TRUE.equals(entity.getInvalido()) || (idade != null && idade <= tetoPadrao);
    dto.setElegivelIrrf(vigente && Boolean.TRUE.equals(entity.getDepIr()) && dentroDoLimite);
    return dto;
  }

  @Override
  public Dependente newEntity(CreateDependenteDTO dto) {
    var entity = new Dependente();
    entity.setId(new DependenteId(dto.getNumfunc(), dto.getNumdep()));
    entity.setDtini(dto.getDtini());
    entity.setNome(dto.getNome());
    entity.setNomeSocial(dto.getNomeSocial());
    entity.setSexo(dto.getSexo());
    entity.setDtnasc(dto.getDtnasc());
    entity.setParentesco(dto.getParentesco());
    entity.setTipodepen(dto.getTipodepen());
    entity.setCpf(dto.getCpf());
    entity.setNumrg(dto.getNumrg());
    entity.setTiporg(dto.getTiporg());
    entity.setOrgaorg(dto.getOrgaorg());
    entity.setUfrg(dto.getUfrg());
    entity.setExpedrg(dto.getExpedrg());
    entity.setDepIr(dto.getDepIr());
    entity.setDepSalarioFamilia(dto.getDepSalarioFamilia());
    entity.setDepPlanoSaude(dto.getDepPlanoSaude());
    entity.setInvalido(dto.getInvalido());
    entity.setEstudante(dto.getEstudante());
    entity.setUniversitario(dto.getUniversitario());
    entity.setExcepcional(dto.getExcepcional());
    entity.setRepresentanteLegal(dto.getRepresentanteLegal());
    entity.setTipodocCert(dto.getTipodocCert());
    entity.setNumCert(dto.getNumCert());
    entity.setLivroACert(dto.getLivroACert());
    entity.setFolhaCert(dto.getFolhaCert());
    entity.setCartorioCert(dto.getCartorioCert());
    entity.setUfCart(dto.getUfCart());
    entity.setMunicipioCart(dto.getMunicipioCart());
    entity.setMatriculaCert(dto.getMatriculaCert());
    entity.setDocumentoLegalId(dto.getDocumentoLegalId());
    entity.setCustomFields(dto.getCustomFields());
    return entity;
  }

  @Override
  public void applyUpdate(Dependente entity, UpdateDependenteDTO dto) {
    entity.setNome(dto.getNome());
    entity.setNomeSocial(dto.getNomeSocial());
    entity.setSexo(dto.getSexo());
    entity.setDtnasc(dto.getDtnasc());
    entity.setParentesco(dto.getParentesco());
    entity.setTipodepen(dto.getTipodepen());
    entity.setCpf(dto.getCpf());
    entity.setNumrg(dto.getNumrg());
    entity.setTiporg(dto.getTiporg());
    entity.setOrgaorg(dto.getOrgaorg());
    entity.setUfrg(dto.getUfrg());
    entity.setExpedrg(dto.getExpedrg());
    entity.setDepIr(dto.getDepIr());
    entity.setDepSalarioFamilia(dto.getDepSalarioFamilia());
    entity.setDepPlanoSaude(dto.getDepPlanoSaude());
    entity.setInvalido(dto.getInvalido());
    entity.setEstudante(dto.getEstudante());
    entity.setUniversitario(dto.getUniversitario());
    entity.setExcepcional(dto.getExcepcional());
    entity.setRepresentanteLegal(dto.getRepresentanteLegal());
    entity.setTipodocCert(dto.getTipodocCert());
    entity.setNumCert(dto.getNumCert());
    entity.setLivroACert(dto.getLivroACert());
    entity.setFolhaCert(dto.getFolhaCert());
    entity.setCartorioCert(dto.getCartorioCert());
    entity.setUfCart(dto.getUfCart());
    entity.setMunicipioCart(dto.getMunicipioCart());
    entity.setMatriculaCert(dto.getMatriculaCert());
    entity.setDocumentoLegalId(dto.getDocumentoLegalId());
    entity.setCustomFields(dto.getCustomFields());
  }

  @Override
  public DependenteId extractId(Dependente entity) {
    return entity.getId();
  }
}
