package br.com.praxis.ergonshowcase.service;

import br.com.praxis.ergonshowcase.config.TenantContext;
import br.com.praxis.ergonshowcase.domain.*;
import br.com.praxis.ergonshowcase.dto.*;
import jakarta.persistence.*;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Avaliações HOST não persistentes; não substituem o enforcement transacional. */
@Service
@Transactional(readOnly = true)
public class ErgonDeterminationService {
  @PersistenceContext private EntityManager em;
  private final TenantContext tenant;
  private final ErgonReadFacts facts;

  public ErgonDeterminationService(TenantContext tenant, ErgonReadFacts facts) {
    this.tenant = tenant;
    this.facts = facts;
  }

  public PcdPreviewDTO pcd(PcdDraftDTO draft) {
    return new PcdPreviewDTO(
        ErgonDerivedFacts.possuiDeficiencia(
            draft.deffisica(),
            draft.defvisual(),
            draft.defauditiva(),
            draft.defmental(),
            draft.defintelectual()));
  }

  private Cargo selected(Integer id) {
    Cargo cargo = em.find(Cargo.class, id);
    if (cargo == null || !java.util.Objects.equals(tenant.orgao(), cargo.getEmpCodigo()))
      ErgonInvariants.fail("Cargo inexistente ou de outro órgão.");
    return cargo;
  }

  public ProvimentoPreviewDTO provimento(CargoSelectionDTO draft) {
    if (draft.cargo() == null) return new ProvimentoPreviewDTO(null, null);
    Cargo cargo = selected(draft.cargo());
    var dto = new CargoResponseDTO();
    dto.setId(cargo.getCargo());
    dto.setVagasAutorizadas(cargo.getVagasAutorizadas());
    facts.enrich(List.of(dto));
    return new ProvimentoPreviewDTO(cargo.getNome(), dto.getSaldoVagasDisponiveis());
  }

  public AglutinadorPreviewDTO aglutinador(CargoSelectionDTO draft) {
    if (draft.cargo() == null) return new AglutinadorPreviewDTO(null);
    Cargo cargo = selected(draft.cargo());
    if (!Boolean.TRUE.equals(cargo.getEAglutinador()))
      ErgonInvariants.fail("Cargo selecionado não está habilitado como agregador.");
    return new AglutinadorPreviewDTO(cargo.getNome());
  }
}
