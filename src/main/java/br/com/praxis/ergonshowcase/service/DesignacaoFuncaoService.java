package br.com.praxis.ergonshowcase.service;

import org.praxisplatform.uischema.stats.*;
import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.domain.*;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.mapper.DesignacaoFuncaoMapper;
import br.com.praxis.ergonshowcase.repository.DesignacaoFuncaoRepository;
import br.com.praxis.ergonshowcase.rules.ErgonRuleEngineFacade;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.Map;
import java.util.Optional;
import org.praxisplatform.uischema.capability.ResourceStateSnapshot;
import org.praxisplatform.uischema.service.base.DuplicateDraftResourceService;
import org.praxisplatform.uischema.service.base.VersionedCreateUpdateResourceService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DesignacaoFuncaoService extends ChefiaResourceService<DesignacaoFuncao, DesignacaoFuncaoResponseDTO, Long, DesignacaoFuncaoFilterDTO, CreateDesignacaoFuncaoDTO, UpdateDesignacaoFuncaoDTO>
    implements VersionedCreateUpdateResourceService<DesignacaoFuncaoResponseDTO, Long, DesignacaoFuncaoFilterDTO, CreateDesignacaoFuncaoDTO, UpdateDesignacaoFuncaoDTO>,
        DuplicateDraftResourceService<DesignacaoFuncaoResponseDTO, Long, DesignacaoFuncaoFilterDTO, CreateDesignacaoFuncaoDTO, UpdateDesignacaoFuncaoDTO, DesignacaoFuncaoDuplicateDraftDTO> {
  private final DesignacaoFuncaoMapper mapper;
  private final ErgonRuleEngineFacade rules;

  @Override
  public org.praxisplatform.uischema.filter.specification.GenericSpecificationsBuilder<DesignacaoFuncao> getSpecificationsBuilder() {
    return SetorFichaReference.relatedFilter(DesignacaoFuncaoFilterDTO.class, DesignacaoFuncaoFilterDTO::new,
        DesignacaoFuncaoFilterDTO::getSetorId, DesignacaoFuncaoFilterDTO::setSetorId, "setorId");
  }

  public DesignacaoFuncaoService(DesignacaoFuncaoRepository repository, DesignacaoFuncaoMapper mapper,
      ErgonRuleEngineFacade rules) {
    super(repository, DesignacaoFuncao.class, "id");
    this.mapper = mapper;
    this.rules = rules;
  }

  @Override
  protected DesignacaoFuncaoMapper getResourceMapper() { return mapper; }

  @Override
  protected DesignacaoFuncaoFilterDTO emptyFilter() { return new DesignacaoFuncaoFilterDTO(); }

  @Override
  protected String resolveResourceKey() { return ApiPaths.KEY_DESIGNACOES; }

  @Override
  protected void validateBusinessRules(DesignacaoFuncao candidate) {
    validateCandidate(candidate, null);
  }

  private void validateCandidate(DesignacaoFuncao candidate, String estadoAnterior) {
    assertEntityAccessAllowed(candidate);
    validateReferences(candidate);
    rules.enforceDesignacaoFuncao(candidate.getEmpCodigo(),
        br.com.praxis.ergonshowcase.rules.DesignacaoFuncaoFacts.load(
            getEntityManager(), candidate, estadoAnterior));
  }

  @Override
  protected void beforeCreate(CreateDesignacaoFuncaoDTO dto, DesignacaoFuncao candidate) {
    assertEntityAccessAllowed(candidate);
    lockVagas(candidate.getEmpCodigo(), candidate.getVagaFuncaoId());
    validateBusinessRules(candidate);
  }

  @Override
  protected void beforeUpdate(Long id, DesignacaoFuncao existing, UpdateDesignacaoFuncaoDTO dto) {
    DesignacaoFuncao candidate = snapshotBeforeMutation(existing);
    mapper.applyUpdate(candidate, dto);
    assertEntityAccessAllowed(candidate);
    boolean writing = getEntityManager().contains(existing);
    if (writing) lockVagas(existing.getEmpCodigo(), existing.getVagaFuncaoId(), candidate.getVagaFuncaoId());
    validateBusinessRules(candidate);
    if (writing && !Objects.equals(existing.getVagaFuncaoId(), candidate.getVagaFuncaoId())) {
      reconcileVaga(existing.getEmpCodigo(), existing.getVagaFuncaoId(), id);
    }
  }

  @Override
  protected void afterCreate(CreateDesignacaoFuncaoDTO dto, DesignacaoFuncao entity) {
    reconcileVaga(entity.getEmpCodigo(), entity.getVagaFuncaoId(), null);
  }

  @Override
  protected void afterUpdate(Long id, DesignacaoFuncao entity, UpdateDesignacaoFuncaoDTO dto) {
    reconcileVaga(entity.getEmpCodigo(), entity.getVagaFuncaoId(), null);
  }

  @Override
  protected void beforeDelete(Long id, DesignacaoFuncao entity) {
    assertEntityAccessAllowed(entity);
    lockVagas(entity.getEmpCodigo(), entity.getVagaFuncaoId());
    long dependencias = getEntityManager().createQuery("""
        select count(s) from SubstituicaoFuncao s where s.empCodigo = :orgao and s.designacaoId = :id
        """, Long.class).setParameter("orgao", entity.getEmpCodigo()).setParameter("id", id).getSingleResult();
    if (dependencias != 0) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "A designação possui substituições vinculadas.");
    }
  }

  @Override
  protected void afterDelete(Long id, DesignacaoFuncao entity) {
    reconcileVaga(entity.getEmpCodigo(), entity.getVagaFuncaoId(), id);
  }

  public DesignacaoFuncao previewHomologacao(DesignacaoFuncao source, HomologarDesignacaoLoteDTO input) {
    var candidate = snapshotBeforeMutation(source);
    candidate.setSituacao("EM_EXERCICIO");
    candidate.setDtExercicio(input.dtExercicio());
    validateCandidate(candidate, source.getSituacao());
    return candidate;
  }

  /** Called within the starter's locked, versioned domain-command transaction. */
  public void applyHomologacao(DesignacaoFuncao current, HomologarDesignacaoLoteDTO input) {
    assertEntityAccessAllowed(current);
    lockVagas(current.getEmpCodigo(), current.getVagaFuncaoId());
    DesignacaoFuncao candidate = previewHomologacao(current, input);
    current.setSituacao(candidate.getSituacao());
    current.setDtExercicio(candidate.getDtExercicio());
    reconcileVaga(current.getEmpCodigo(), current.getVagaFuncaoId(), null);
  }

  private void lockVagas(Integer orgao, Long... ids) {
    Stream.of(ids).filter(Objects::nonNull).distinct().sorted().forEach(id -> {
      requireReference(orgao, VagaFuncao.class, id);
      getEntityManager().refresh(getEntityManager().find(VagaFuncao.class, id), LockModeType.PESSIMISTIC_WRITE);
    });
  }

  private List<VinculoId> titulares(Integer orgao, Long vaga, Long excluir) {
    return getEntityManager().createQuery("""
        select d.titularId from DesignacaoFuncao d
        where d.empCodigo = :orgao and d.vagaFuncaoId = :vaga and d.situacao = 'EM_EXERCICIO'
          and d.id <> :excluir
        """, VinculoId.class).setParameter("orgao", orgao).setParameter("vaga", vaga)
        .setParameter("excluir", excluir == null ? -1L : excluir).setMaxResults(2).getResultList();
  }

  private void reconcileVaga(Integer orgao, Long id, Long excluir) {
    if (id == null) return;
    VagaFuncao vaga = getEntityManager().find(VagaFuncao.class, id);
    var titulares = titulares(orgao, id, excluir);
    if (titulares.size() > 1) throw new IllegalStateException("Vaga com titularidade inconsistente.");
    vaga.setTitularAtualId(titulares.isEmpty() ? null : titulares.getFirst());
    if ("LIVRE".equals(vaga.getSituacao()) || "OCUPADA".equals(vaga.getSituacao())) {
      vaga.setSituacao(titulares.isEmpty() ? "LIVRE" : "OCUPADA");
    }
  }

  @Override
  @PreAuthorize("hasAuthority('ERGON_READ') and hasAuthority('ERGON_WRITE')")
  @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
  public DesignacaoFuncaoDuplicateDraftDTO duplicateDraft(Long id) {
    var source = findEntityById(id);
    if (!podePrepararRascunho(source.getSituacao())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "A situação da origem impede o preparo do rascunho.");
    }
    return new DesignacaoFuncaoDuplicateDraftDTO(source.getFuncaoId());
  }

  @Override
  protected Optional<ResourceStateSnapshot> toResourceStateSnapshot(DesignacaoFuncao entity) {
    return Optional.of(ResourceStateSnapshot.of(entity.getSituacao(),
        Map.of(
            "duplicateDraftEligible", podePrepararRascunho(entity.getSituacao()),
            "homologacaoEligible", "DESIGNADA".equals(entity.getSituacao()))));
  }

  private static boolean podePrepararRascunho(String estado) {
    return "DESIGNADA".equals(estado) || "EM_EXERCICIO".equals(estado);
  }

  @Override
  protected void validateReferences(DesignacaoFuncao candidate) {
    requireReference(candidate.getEmpCodigo(), Orgao.class, candidate.getEmpCodigo());
    requireReference(candidate.getEmpCodigo(), Vinculo.class, candidate.getTitularId());
    requireReference(candidate.getEmpCodigo(), FuncaoConfianca.class, candidate.getFuncaoId());
    requireReference(candidate.getEmpCodigo(), VagaFuncao.class, candidate.getVagaFuncaoId());
    requireReference(candidate.getEmpCodigo(), Setor.class, candidate.getSetorId());
    requireReference(candidate.getEmpCodigo(), Vinculo.class, candidate.getSubstituto1Id());
    requireReference(candidate.getEmpCodigo(), Vinculo.class, candidate.getSubstituto2Id());
    requireReference(candidate.getEmpCodigo(), DocumentoLegal.class, candidate.getAtoDispensaId());
    requireReference(candidate.getEmpCodigo(), DocumentoLegal.class, candidate.getDocumentoLegalId());
    requireReference(candidate.getEmpCodigo(), DocumentoLegal.class, candidate.getAtoPublicacaoId());
  }

  @Override
  public StatsFieldRegistry getStatsFieldRegistry() {
    return StatsFieldRegistry.builder()
        .groupByBucket("situacao", "situacao", java.util.Set.of(StatsMetric.COUNT))
        .groupByBucket("opcaoRemuneracao", "opcaoRemuneracao", java.util.Set.of(StatsMetric.COUNT))
        .temporalTimeSeriesField("dtini", "dtini")
        .build();
  }

  @Override
  public StatsSupportMode getDistributionStatsSupportMode() {
    return StatsSupportMode.DISABLED;
  }
}
