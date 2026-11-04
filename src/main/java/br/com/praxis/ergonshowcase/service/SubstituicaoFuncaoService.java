package br.com.praxis.ergonshowcase.service;

import org.praxisplatform.uischema.stats.*;
import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.domain.*;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.mapper.SubstituicaoFuncaoMapper;
import br.com.praxis.ergonshowcase.repository.SubstituicaoFuncaoRepository;
import br.com.praxis.ergonshowcase.rules.ErgonRuleEngineFacade;
import br.com.praxis.ergonshowcase.rules.SubstituicaoFuncaoFacts;
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
public class SubstituicaoFuncaoService extends ChefiaResourceService<SubstituicaoFuncao, SubstituicaoFuncaoResponseDTO, Long, SubstituicaoFuncaoFilterDTO, CreateSubstituicaoFuncaoDTO, UpdateSubstituicaoFuncaoDTO>
    implements VersionedCreateUpdateResourceService<SubstituicaoFuncaoResponseDTO, Long, SubstituicaoFuncaoFilterDTO, CreateSubstituicaoFuncaoDTO, UpdateSubstituicaoFuncaoDTO>,
        DuplicateDraftResourceService<SubstituicaoFuncaoResponseDTO, Long, SubstituicaoFuncaoFilterDTO, CreateSubstituicaoFuncaoDTO, UpdateSubstituicaoFuncaoDTO, SubstituicaoFuncaoDuplicateDraftDTO> {
  private final SubstituicaoFuncaoMapper mapper;
  private final ErgonRuleEngineFacade rules;

  public SubstituicaoFuncaoService(SubstituicaoFuncaoRepository repository, SubstituicaoFuncaoMapper mapper,
      ErgonRuleEngineFacade rules) {
    super(repository, SubstituicaoFuncao.class, "id");
    this.mapper = mapper;
    this.rules = rules;
  }

  @Override
  protected SubstituicaoFuncaoMapper getResourceMapper() { return mapper; }

  @Override
  protected SubstituicaoFuncaoFilterDTO emptyFilter() { return new SubstituicaoFuncaoFilterDTO(); }

  @Override
  protected String resolveResourceKey() { return ApiPaths.KEY_SUBSTITUICOES; }

  @Override
  protected void validateBusinessRules(SubstituicaoFuncao candidate) {
    rules.enforceSubstituicaoFuncao(candidate.getEmpCodigo(),
        SubstituicaoFuncaoFacts.load(getEntityManager(), candidate));
  }

  @Override
  protected void beforeDelete(Long id, SubstituicaoFuncao entity) {
    assertEntityAccessAllowed(entity);
    if (!"PROGRAMADA".equals(entity.getSituacao()) || entity.getAtoPublicacaoId() != null) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "A substituição já integra o histórico funcional.");
    }
  }

  @Override
  @PreAuthorize("hasAuthority('ERGON_READ') and hasAuthority('ERGON_WRITE')")
  @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
  public SubstituicaoFuncaoDuplicateDraftDTO duplicateDraft(Long id) {
    var source = findEntityById(id);
    if (!podePrepararRascunho(source.getSituacao())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "A situação da origem impede o preparo do rascunho.");
    }
    return new SubstituicaoFuncaoDuplicateDraftDTO(source.getMotivo());
  }

  @Override
  protected Optional<ResourceStateSnapshot> toResourceStateSnapshot(SubstituicaoFuncao entity) {
    return Optional.of(ResourceStateSnapshot.of(entity.getSituacao(),
        Map.of(
            "duplicateDraftEligible", podePrepararRascunho(entity.getSituacao()),
            "excluivel", "PROGRAMADA".equals(entity.getSituacao()) && entity.getAtoPublicacaoId() == null)));
  }

  private static boolean podePrepararRascunho(String estado) {
    return "PROGRAMADA".equals(estado) || "EM_EXERCICIO".equals(estado);
  }

  @Override
  protected void validateReferences(SubstituicaoFuncao candidate) {
    requireReference(candidate.getEmpCodigo(), Orgao.class, candidate.getEmpCodigo());
    requireReference(candidate.getEmpCodigo(), DesignacaoFuncao.class, candidate.getDesignacaoId());
    requireReference(candidate.getEmpCodigo(), Vinculo.class, candidate.getSubstitutoId());
    requireReference(candidate.getEmpCodigo(), Afastamento.class, candidate.getAfastamentoId());
    requireReference(candidate.getEmpCodigo(), FeriasGozo.class, candidate.getFeriasGozoId());
    requireReference(candidate.getEmpCodigo(), DocumentoLegal.class, candidate.getDocumentoLegalId());
    requireReference(candidate.getEmpCodigo(), DocumentoLegal.class, candidate.getAtoPublicacaoId());
  }

  @Override
  public StatsFieldRegistry getStatsFieldRegistry() {
    return StatsFieldRegistry.builder()
        .groupByBucket("motivo", "motivo", java.util.Set.of(StatsMetric.COUNT))
        .groupByBucket("situacao", "situacao", java.util.Set.of(StatsMetric.COUNT))
        .temporalTimeSeriesField("dtini", "dtini")
        .build();
  }

  @Override
  public StatsSupportMode getDistributionStatsSupportMode() {
    return StatsSupportMode.DISABLED;
  }
}
