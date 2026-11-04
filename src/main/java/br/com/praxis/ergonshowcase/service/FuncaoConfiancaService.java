package br.com.praxis.ergonshowcase.service;

import org.praxisplatform.uischema.stats.*;
import br.com.praxis.ergonshowcase.constants.ApiPaths;
import br.com.praxis.ergonshowcase.domain.*;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.mapper.FuncaoConfiancaMapper;
import br.com.praxis.ergonshowcase.repository.FuncaoConfiancaRepository;
import br.com.praxis.ergonshowcase.rules.ErgonRuleEngineFacade;
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
public class FuncaoConfiancaService extends ChefiaResourceService<FuncaoConfianca, FuncaoConfiancaResponseDTO, Long, FuncaoConfiancaFilterDTO, CreateFuncaoConfiancaDTO, UpdateFuncaoConfiancaDTO>
    implements VersionedCreateUpdateResourceService<FuncaoConfiancaResponseDTO, Long, FuncaoConfiancaFilterDTO, CreateFuncaoConfiancaDTO, UpdateFuncaoConfiancaDTO>,
        DuplicateDraftResourceService<FuncaoConfiancaResponseDTO, Long, FuncaoConfiancaFilterDTO, CreateFuncaoConfiancaDTO, UpdateFuncaoConfiancaDTO, FuncaoConfiancaDuplicateDraftDTO> {
  private final FuncaoConfiancaMapper mapper;
  @org.springframework.beans.factory.annotation.Autowired
  private ErgonRuleEngineFacade rules;

  public FuncaoConfiancaService(FuncaoConfiancaRepository repository, FuncaoConfiancaMapper mapper) {
    super(repository, FuncaoConfianca.class, "id");
    this.mapper = mapper;
  }

  @Override
  protected FuncaoConfiancaMapper getResourceMapper() { return mapper; }

  @Override
  protected FuncaoConfiancaFilterDTO emptyFilter() { return new FuncaoConfiancaFilterDTO(); }

  @Override
  protected String resolveResourceKey() { return ApiPaths.KEY_FUNCOES; }

  @Override
  protected void beforeDelete(Long id, FuncaoConfianca entity) {
    assertEntityAccessAllowed(entity);
    var dependencies = getEntityManager().createQuery("""
        select f.id from FuncaoConfianca f
        where f.id = :id and f.empCodigo = :orgao
          and (exists (select v.id from VagaFuncao v where v.funcaoId = f.id)
            or exists (select d.id from DesignacaoFuncao d where d.funcaoId = f.id))
        """, Long.class).setParameter("id", id).setParameter("orgao", entity.getEmpCodigo())
        .getResultList();
    if (!dependencies.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "A função possui vagas ou designações vinculadas.");
    }
  }

  @Override
  @PreAuthorize("hasAuthority('ERGON_READ') and hasAuthority('ERGON_WRITE')")
  @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
  public FuncaoConfiancaDuplicateDraftDTO duplicateDraft(Long id) {
    var source = findEntityById(id);
    if (!podePrepararRascunho(source.getSituacao())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "A situação da origem impede o preparo do rascunho.");
    }
    return new FuncaoConfiancaDuplicateDraftDTO(source.getCategoria());
  }

  @Override
  protected Optional<ResourceStateSnapshot> toResourceStateSnapshot(FuncaoConfianca entity) {
    return Optional.of(ResourceStateSnapshot.of(entity.getSituacao(),
        Map.of("duplicateDraftEligible", podePrepararRascunho(entity.getSituacao()))));
  }

  private static boolean podePrepararRascunho(String estado) {
    return "ATIVA".equals(estado);
  }

  @Override
  protected void validateReferences(FuncaoConfianca candidate) {
    requireReference(candidate.getEmpCodigo(), Orgao.class, candidate.getEmpCodigo());
    requireReference(candidate.getEmpCodigo(), DocumentoLegal.class, candidate.getDocumentoLegalId());
    requireReference(candidate.getEmpCodigo(), DocumentoLegal.class, candidate.getAtoPublicacaoId());
  }

  @Override
  protected void validateBusinessRules(FuncaoConfianca candidate) {
    rules.enforceFuncaoConfianca(candidate);
  }

  @Override
  public StatsFieldRegistry getStatsFieldRegistry() {
    return StatsFieldRegistry.builder()
        .groupByBucket("categoria", "categoria", java.util.Set.of(StatsMetric.COUNT))
        .groupByBucket("situacao", "situacao", java.util.Set.of(StatsMetric.COUNT))
        .temporalTimeSeriesField("dtini", "dtini")
        .numericHistogramMeasureField("diasMinimosSubstituicao", "diasMinimosSubstituicao")
        .build();
  }
}
