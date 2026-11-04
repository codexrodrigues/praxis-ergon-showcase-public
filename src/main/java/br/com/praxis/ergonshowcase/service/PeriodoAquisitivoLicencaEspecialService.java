package br.com.praxis.ergonshowcase.service;
import br.com.praxis.ergonshowcase.config.TenantContext;
import br.com.praxis.ergonshowcase.domain.*;
import br.com.praxis.ergonshowcase.rules.ErgonRuleEngineFacade;
import br.com.praxis.ergonshowcase.rules.OperationalRuleSet;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.mapper.PeriodoAquisitivoLicencaEspecialMapper;
import br.com.praxis.ergonshowcase.repository.PeriodoAquisitivoLicencaEspecialRepository;
import jakarta.persistence.*;
import java.util.*;
import org.praxisplatform.uischema.exporting.*;
import org.praxisplatform.uischema.service.base.ResourceFilterAccessScope;
import org.praxisplatform.uischema.stats.*;
import org.springframework.stereotype.Service;

@Service
public class PeriodoAquisitivoLicencaEspecialService extends ErgonResourceService<PeriodoAquisitivoLicencaEspecial, PeriodoAquisitivoLicencaEspecialResponseDTO, Long, PeriodoAquisitivoLicencaEspecialFilterDTO, CreatePeriodoAquisitivoLicencaEspecialDTO, UpdatePeriodoAquisitivoLicencaEspecialDTO>
    implements org.praxisplatform.uischema.service.base.VersionedCreateUpdateResourceService<
        PeriodoAquisitivoLicencaEspecialResponseDTO, Long, PeriodoAquisitivoLicencaEspecialFilterDTO, CreatePeriodoAquisitivoLicencaEspecialDTO, UpdatePeriodoAquisitivoLicencaEspecialDTO>,
        org.praxisplatform.uischema.service.base.DuplicateDraftResourceService<PeriodoAquisitivoLicencaEspecialResponseDTO, Long, PeriodoAquisitivoLicencaEspecialFilterDTO, CreatePeriodoAquisitivoLicencaEspecialDTO, UpdatePeriodoAquisitivoLicencaEspecialDTO, PeriodoAquisitivoLicencaEspecialDuplicateDraftDTO> {
  private final PeriodoAquisitivoLicencaEspecialMapper mapper;
  private final TenantContext tenant;
  @org.springframework.beans.factory.annotation.Autowired private jakarta.validation.Validator beanValidator;
  @org.springframework.beans.factory.annotation.Autowired private ErgonRuleEngineFacade rules = new ErgonRuleEngineFacade();
  public PeriodoAquisitivoLicencaEspecialService(PeriodoAquisitivoLicencaEspecialRepository repository, PeriodoAquisitivoLicencaEspecialMapper mapper, TenantContext tenant) {
    super(repository, PeriodoAquisitivoLicencaEspecial.class, "id");
    this.mapper = mapper;
    this.tenant = tenant;
  }


  /** Executa as mesmas guardas e a sincronização da exclusão individual no lote. */
  @Override @org.springframework.transaction.annotation.Transactional
  public void deleteAllById(Collection<Long> ids) {
    if (ids == null) throw new IllegalArgumentException("ids must not be null");
    ids.stream().distinct().sorted().forEach(this::deleteById);
  }
  @Override protected PeriodoAquisitivoLicencaEspecialMapper getResourceMapper() { return mapper; }
  @Override protected PeriodoAquisitivoLicencaEspecialFilterDTO emptyFilter() { return new PeriodoAquisitivoLicencaEspecialFilterDTO(); }
  @Override public Optional<String> getDatasetVersion() { return Optional.empty(); }
  @Override protected PeriodoAquisitivoLicencaEspecialFilterDTO normalizeOptionSourceFilter(
      org.praxisplatform.uischema.options.OptionSourceDescriptor descriptor,
      org.praxisplatform.uischema.options.service.OptionSourceOperation operation,
      PeriodoAquisitivoLicencaEspecialFilterDTO filter) {
    return TempoLicencaValidation.scopeFilter(filter == null ? emptyFilter() : filter, tenant);
  }

  @Override protected void beforeCreate(CreatePeriodoAquisitivoLicencaEspecialDTO dto, PeriodoAquisitivoLicencaEspecial entity) {
    TempoLicencaValidation.orgao(tenant, entity.getEmpCodigo());
    TempoLicencaLifecycle.requireState(entity, "EM_AQUISICAO");
    rules.enforceOperational(OperationalRuleSet.PERIODO_CRIAR, entity.getEmpCodigo(), entity.getDiasConvertidosDobro(), entity.getDtCntDobro());
    validate(entity, null, false);
  }
  @Override protected void beforeUpdate(Long id, PeriodoAquisitivoLicencaEspecial existing, UpdatePeriodoAquisitivoLicencaEspecialDTO dto) {
    TempoLicencaValidation.orgao(tenant, existing.getEmpCodigo());
    rules.enforceOperational(OperationalRuleSet.LICENCA_ESTADO_EDICAO, existing.getEmpCodigo(), existing.getSituacao(), dto.getSituacao());
    rules.enforceOperational(OperationalRuleSet.PERIODO_CONVERSAO_EDICAO, existing.getEmpCodigo(), existing.getDiasConvertidosDobro(), dto.getDiasConvertidosDobro(), existing.getDtCntDobro(), dto.getDtCntDobro());
    var candidate = mapper.candidate(dto);
    candidate.setId(id);
    if (candidate.getChave() == null) candidate.setChave(existing.getChave());
    validate(candidate, existing, true);
    dto.setChave(candidate.getChave());
  }
  @Override protected void afterUpdate(Long id, PeriodoAquisitivoLicencaEspecial saved, UpdatePeriodoAquisitivoLicencaEspecialDTO dto) {
    TempoLicencaValidation.recalcularPeriodo(saved, getEntityManager(), rules);
  }
  private void validate(PeriodoAquisitivoLicencaEspecial entity,
      PeriodoAquisitivoLicencaEspecial anterior, boolean edicaoManual) {

    var vinculo = TempoLicencaValidation.cadastro(entity, tenant, getEntityManager());
    if (entity.getChave() == null) {
      entity.setChave(TempoLicencaRuleFacts.proximaChave(
          PeriodoAquisitivoLicencaEspecial.class, entity, vinculo, getEntityManager()));
    }
    rules.enforcePeriodoAquisitivoLicencaEspecial(entity.getEmpCodigo(),
        TempoLicencaRuleFacts.periodo(entity, anterior, edicaoManual, vinculo, getEntityManager()));
    TempoLicencaValidation.recalcularPeriodo(entity, getEntityManager(), rules);
    if (entity.getAverbacaoOrigemId() != null) {
      var origem = getEntityManager().find(AverbacaoTempoServico.class, entity.getAverbacaoOrigemId());
      if (origem == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Averbação de origem inexistente.");
      ErgonTenantAccess.requireReferencesInOrganization(entity.getEmpCodigo(), origem);
      rules.enforceOperational(OperationalRuleSet.PERIODO_VINCULO_ORIGEM, entity.getEmpCodigo(), origem.getNumfunc(), entity.getNumfunc(), origem.getNumvinc(), entity.getNumvinc());
      getEntityManager().lock(origem, LockModeType.PESSIMISTIC_WRITE);
      getEntityManager().refresh(origem);
      ErgonTenantAccess.requireReferencesInOrganization(entity.getEmpCodigo(), origem);
      rules.enforceOperational(OperationalRuleSet.PERIODO_VINCULO_ORIGEM, entity.getEmpCodigo(), origem.getNumfunc(), entity.getNumfunc(), origem.getNumvinc(), entity.getNumvinc());
      rules.enforceOperational(OperationalRuleSet.PERIODO_NOVA_ORIGEM, entity.getEmpCodigo(), anterior == null ? null : anterior.getAverbacaoOrigemId(), entity.getAverbacaoOrigemId(), origem.getSituacao(), origem.getDataRemocao(), origem.getFinalidade());
    }

  }


  @Override protected PeriodoAquisitivoLicencaEspecial findEntityById(Long id) {
    var entity = super.findEntityById(id);
    TempoLicencaValidation.orgao(tenant, entity.getEmpCodigo());
    return entity;
  }
  @Override protected List<PeriodoAquisitivoLicencaEspecial> findEntitiesById(Collection<Long> ids) {
    var entities = super.findEntitiesById(ids);
    entities.forEach(entity -> TempoLicencaValidation.orgao(tenant, entity.getEmpCodigo()));
    return entities;
  }
  @Override protected List<PeriodoAquisitivoLicencaEspecial> findAllEntities() {
    return getRepository().findAll(resolveResourceFilterAccessScope().specification(), getDefaultSort());
  }

  @Override protected void beforeDelete(Long id, PeriodoAquisitivoLicencaEspecial entity) {
    TempoLicencaValidation.orgao(tenant, entity.getEmpCodigo());
    TempoLicencaLifecycle.requireState(entity,"EM_AQUISICAO");
    getEntityManager().lock(entity, LockModeType.PESSIMISTIC_WRITE, Map.of("jakarta.persistence.lock.timeout",1000));
    TempoLicencaValidation.semParcelas(id, entity.getEmpCodigo(), getEntityManager());
  }
  @Override protected void beforeDeleteAllById(Collection<Long> ids) {
    ids.forEach(this::findEntityById);
  }
  @Override protected List<CollectionExportField> exportFields() {
    var date = new CollectionExportFieldPresentation("date", "dd/MM/yyyy", null, "pt-BR", "America/Sao_Paulo", null, null, "Não informado");
    var number = new CollectionExportFieldPresentation("number", "0", null, "pt-BR", null, null, null, "Não informado");
    var bool = new CollectionExportFieldPresentation("boolean", null, null, "pt-BR", null, "Sim", "Não", "Não informado");
    return List.of(field("id", "Identidade", "integer"), field("descricao", "Assentamento", "string"),
        field("empCodigo", "Órgão", "integer"), field("numfunc", "Servidor", "integer"), field("numvinc", "Vínculo", "integer"),
        presentedField("dtini", "Início", "date", date), presentedField("dtfim", "Término", "date", date),
        field("situacao", "Situação", "string"), field("saldoDias", "Saldo de dias", "integer"),
        presentedField("diasDireito", "Dias de direito", "integer", number),
        presentedField("diasGozados", "Dias de gozo", "integer", number),
        presentedField("diasVendidos", "Dias em pecúnia", "integer", number),
        presentedField("diasConvertidosDobro", "Dias convertidos", "integer", number),
        presentedField("dtPrescricao", "Prescrição", "date", date),
        presentedField("dtCntDobro", "Conversão em dobro", "date", date),
        field("vendLicEsp", "Política de venda", "string"),
        field("contDobroLicEsp", "Política de conversão", "string"));
  }
  @Override public StatsFieldRegistry getStatsFieldRegistry() {
    return StatsFieldRegistry.builder()
        .groupByBucket("id", "id", Set.of(StatsMetric.COUNT, StatsMetric.SUM))
        .groupByBucket("situacao", "situacao", Set.of(StatsMetric.COUNT, StatsMetric.SUM, StatsMetric.AVG, StatsMetric.MEDIAN, StatsMetric.PERCENTILE_90))
        .temporalTimeSeriesField("dtini", "dtini")
        .numericHistogramMeasureField("saldoDias", "saldoDias").build();
  }

  @org.springframework.transaction.annotation.Transactional
  public org.praxisplatform.uischema.concurrency.ResourceRepresentationResult<PeriodoAquisitivoLicencaEspecialResponseDTO> patch(Long id,
      org.praxisplatform.uischema.dto.PartialUpdateInput patch,
      org.praxisplatform.uischema.concurrency.ResourceVersionUpdatePrecondition<Long> precondition) {
    if(precondition==null) throw org.praxisplatform.uischema.concurrency.ResourceVersionPreconditionException.required();
    var entity = findEntityById(id);
    TempoLicencaValidation.orgao(tenant, entity.getEmpCodigo());
    var dto = new UpdatePeriodoAquisitivoLicencaEspecialDTO();
    org.springframework.beans.BeanUtils.copyProperties(mapper.toResponse(entity), dto);
    org.praxisplatform.uischema.service.base.ResourceStateCopySupport.copySuppliedPatchProperties(patch, dto, resourceObjectMapper);
    if(!beanValidator.validate(dto).isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dados do assentamento incompatíveis com o contrato.");
    return update(id, dto, precondition);
  }

  /** Candidato isolado utilizado na avaliação e novamente sob os locks do executor durável. */
  public void validateBulkCandidate(PeriodoAquisitivoLicencaEspecial candidate, PeriodoAquisitivoLicencaEspecial previous) {
    validate(candidate, previous, previous != null);
  }
  @org.springframework.transaction.annotation.Transactional
  public org.praxisplatform.uischema.concurrency.ResourceRepresentationResult<PeriodoAquisitivoLicencaEspecialResponseDTO> transition(Long id, String action,
      TempoLicencaTransicaoDTO dto, org.praxisplatform.uischema.concurrency.ResourceVersionUpdatePrecondition<Long> precondition) {
    return executeVersionedMutation(id, precondition, entity -> {
      TempoLicencaValidation.orgao(tenant, entity.getEmpCodigo());
      var input = new UpdatePeriodoAquisitivoLicencaEspecialDTO(); org.springframework.beans.BeanUtils.copyProperties(mapper.toResponse(entity), input);
    var candidate = mapper.candidate(input); candidate.setId(id);
    TempoLicencaLifecycle.transition(candidate, action, dto, tenant, getEntityManager(), rules);
    validate(candidate, entity, false);
    org.springframework.beans.BeanUtils.copyProperties(candidate, entity, "id", "version", "auditoria", "orgao", "pessoa", "vinculo", "documentoLegal", "atoPublicacao", "averbacaoOrigem", "periodoAquisitivoLicenca");
    getEntityManager().flush();
    });
  }
  public void synchronizeAccounting(Long id) { TempoLicencaValidation.sincronizar(id,tenant,getEntityManager(), rules); }

  @Override
  @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ERGON_READ') and hasAuthority('ERGON_WRITE')")
  @org.springframework.transaction.annotation.Transactional(readOnly = true, propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
  public PeriodoAquisitivoLicencaEspecialDuplicateDraftDTO duplicateDraft(Long sourceId) {
    var source=findEntityById(sourceId);
    if(Set.of("CANCELADO","ESTORNADA","PRESCRITO").contains(source.getSituacao()))
      throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN,"duplicate-draft-unavailable");
    var draft=new PeriodoAquisitivoLicencaEspecialDuplicateDraftDTO();
    draft.setDescricao(source.getDescricao());
    draft.setEmpCodigo(source.getEmpCodigo());
    draft.setDiasDireito(source.getDiasDireito());
    return draft;
  }
  @Override protected java.util.Optional<org.praxisplatform.uischema.capability.ResourceStateSnapshot> toResourceStateSnapshot(PeriodoAquisitivoLicencaEspecial e) {
    return java.util.Optional.of(snapshotState(e));
  }
  private org.praxisplatform.uischema.capability.ResourceStateSnapshot snapshotState(PeriodoAquisitivoLicencaEspecial e) {
    return org.praxisplatform.uischema.capability.ResourceStateSnapshot.of(e.getSituacao());
  }

  @Override protected String resolveResourceKey() { return "administracao-pessoal.periodos-aquisitivos-licenca-especial"; }

}
