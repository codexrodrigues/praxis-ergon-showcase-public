package br.com.praxis.ergonshowcase.service;

import org.praxisplatform.uischema.stats.*;
import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.domain.*;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.mapper.VagaFuncaoMapper;
import br.com.praxis.ergonshowcase.repository.VagaFuncaoRepository;
import br.com.praxis.ergonshowcase.rules.ErgonRuleEngineFacade;
import java.util.Map;
import java.util.Optional;
import org.praxisplatform.uischema.capability.ResourceStateSnapshot;
import org.praxisplatform.uischema.service.base.VersionedCreateUpdateResourceService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class VagaFuncaoService extends ChefiaResourceService<VagaFuncao, VagaFuncaoResponseDTO, Long, VagaFuncaoFilterDTO, CreateVagaFuncaoDTO, UpdateVagaFuncaoDTO>
    implements VersionedCreateUpdateResourceService<VagaFuncaoResponseDTO, Long, VagaFuncaoFilterDTO, CreateVagaFuncaoDTO, UpdateVagaFuncaoDTO> {
  private final VagaFuncaoMapper mapper;
  @org.springframework.beans.factory.annotation.Autowired
  private ErgonRuleEngineFacade rules;

  public VagaFuncaoService(VagaFuncaoRepository repository, VagaFuncaoMapper mapper) {
    super(repository, VagaFuncao.class, "id");
    this.mapper = mapper;
  }

  @Override
  protected VagaFuncaoMapper getResourceMapper() { return mapper; }

  @Override
  protected VagaFuncaoFilterDTO emptyFilter() { return new VagaFuncaoFilterDTO(); }

  @Override
  protected String resolveResourceKey() { return ApiPaths.KEY_VAGAS_FUNCAO; }

  @Override
  protected Optional<ResourceStateSnapshot> toResourceStateSnapshot(VagaFuncao entity) {
    boolean disponivel = "LIVRE".equals(entity.getSituacao()) && entity.getTitularAtualId() == null;
    return Optional.of(ResourceStateSnapshot.of(entity.getSituacao(),
        Map.of("disponivelParaDesignacao", disponivel, "ocupada", entity.getTitularAtualId() != null)));
  }

  @Override
  protected void validateBusinessRules(VagaFuncao candidate, VagaFuncao previous) {
    var funcao = getEntityManager().find(FuncaoConfianca.class, candidate.getFuncaoId());
    rules.enforceVagaFuncao(candidate, previous, funcao);
  }

  @Override
  protected void beforeDelete(Long id, VagaFuncao entity) {
    assertEntityAccessAllowed(entity);
    var designacoes = getEntityManager().createQuery("""
        select d.id from DesignacaoFuncao d where d.vagaFuncaoId = :id
        """, Long.class).setParameter("id", id).setMaxResults(1).getResultList();
    if (entity.getTitularAtualId() != null || "OCUPADA".equals(entity.getSituacao()) || !designacoes.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "A vaga possui ocupação ou histórico de designação.");
    }
  }

  @Override
  protected void validateReferences(VagaFuncao candidate) {
    requireReference(candidate.getEmpCodigo(), Orgao.class, candidate.getEmpCodigo());
    requireReference(candidate.getEmpCodigo(), FuncaoConfianca.class, candidate.getFuncaoId());
    requireReference(candidate.getEmpCodigo(), Setor.class, candidate.getSetorId());
    requireReference(candidate.getEmpCodigo(), Vinculo.class, candidate.getTitularAtualId());
    requireReference(candidate.getEmpCodigo(), DocumentoLegal.class, candidate.getDocumentoLegalId());
    requireReference(candidate.getEmpCodigo(), DocumentoLegal.class, candidate.getAtoPublicacaoId());
  }

  @Override
  public StatsFieldRegistry getStatsFieldRegistry() {
    return StatsFieldRegistry.builder()
        .groupByBucket("situacao", "situacao", java.util.Set.of(StatsMetric.COUNT))
        .groupByBucket("origemVaga", "origemVaga", java.util.Set.of(StatsMetric.COUNT))
        .temporalTimeSeriesField("dtini", "dtini")
        .build();
  }

  @Override
  public StatsSupportMode getDistributionStatsSupportMode() {
    return StatsSupportMode.DISABLED;
  }
}
