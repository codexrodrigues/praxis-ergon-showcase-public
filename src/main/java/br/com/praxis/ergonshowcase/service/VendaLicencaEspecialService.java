package br.com.praxis.ergonshowcase.service;
import br.com.praxis.ergonshowcase.config.TenantContext;
import br.com.praxis.ergonshowcase.domain.*;
import br.com.praxis.ergonshowcase.rules.ErgonRuleEngineFacade;
import br.com.praxis.ergonshowcase.rules.OperationalRuleSet;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.mapper.VendaLicencaEspecialMapper;
import br.com.praxis.ergonshowcase.repository.VendaLicencaEspecialRepository;
import jakarta.persistence.*;
import java.util.*;
import org.praxisplatform.uischema.exporting.*;
import org.praxisplatform.uischema.service.base.ResourceFilterAccessScope;
import org.praxisplatform.uischema.stats.*;
import org.springframework.stereotype.Service;

@Service
public class VendaLicencaEspecialService extends ErgonResourceService<VendaLicencaEspecial, VendaLicencaEspecialResponseDTO, Long, VendaLicencaEspecialFilterDTO, CreateVendaLicencaEspecialDTO, UpdateVendaLicencaEspecialDTO>
    implements org.praxisplatform.uischema.service.base.VersionedCreateUpdateResourceService<
        VendaLicencaEspecialResponseDTO, Long, VendaLicencaEspecialFilterDTO, CreateVendaLicencaEspecialDTO, UpdateVendaLicencaEspecialDTO>,
        org.praxisplatform.uischema.service.base.DuplicateDraftResourceService<VendaLicencaEspecialResponseDTO, Long, VendaLicencaEspecialFilterDTO, CreateVendaLicencaEspecialDTO, UpdateVendaLicencaEspecialDTO, VendaLicencaEspecialDuplicateDraftDTO> {
  private final VendaLicencaEspecialMapper mapper;
  private final TenantContext tenant;
  @org.springframework.beans.factory.annotation.Autowired private jakarta.validation.Validator beanValidator;
  @org.springframework.beans.factory.annotation.Autowired private ErgonRuleEngineFacade rules = new ErgonRuleEngineFacade();
  public VendaLicencaEspecialService(VendaLicencaEspecialRepository repository, VendaLicencaEspecialMapper mapper, TenantContext tenant) {
    super(repository, VendaLicencaEspecial.class, "id");
    this.mapper = mapper;
    this.tenant = tenant;
  }


  /** Executa as mesmas guardas e a sincronização da exclusão individual no lote. */
  @Override @org.springframework.transaction.annotation.Transactional
  public void deleteAllById(Collection<Long> ids) {
    if (ids == null) throw new IllegalArgumentException("ids must not be null");
    ids.stream().distinct().sorted().forEach(this::deleteById);
  }
  @Override protected VendaLicencaEspecialMapper getResourceMapper() { return mapper; }
  @Override protected VendaLicencaEspecialFilterDTO emptyFilter() { return new VendaLicencaEspecialFilterDTO(); }
  @Override public Optional<String> getDatasetVersion() { return Optional.empty(); }
  @Override protected VendaLicencaEspecialFilterDTO normalizeOptionSourceFilter(
      org.praxisplatform.uischema.options.OptionSourceDescriptor descriptor,
      org.praxisplatform.uischema.options.service.OptionSourceOperation operation,
      VendaLicencaEspecialFilterDTO filter) {
    return TempoLicencaValidation.scopeFilter(filter == null ? emptyFilter() : filter, tenant);
  }

  @Override protected Object prepareCreateValidationDraft(CreateVendaLicencaEspecialDTO dto, VendaLicencaEspecial candidate) {
    hydrateParentPeriodoAquisitivo(candidate);
    return mapper.toResponse(candidate);
  }
  @Override protected Object prepareUpdateValidationDraft(Long id, VendaLicencaEspecial candidate, UpdateVendaLicencaEspecialDTO dto) {
    mapper.applyUpdate(candidate, dto);
    hydrateParentPeriodoAquisitivo(candidate);
    return mapper.toResponse(candidate);
  }
  @Override protected void beforeCreate(CreateVendaLicencaEspecialDTO dto, VendaLicencaEspecial entity) {
    TempoLicencaLifecycle.requireState(entity, "SOLICITADA");
    rules.enforceOperational(OperationalRuleSet.VENDA_CRIAR_DATAS, tenant.orgao(), entity.getDtPagamento(), entity.getDtEstorno());
    rules.enforceOperational(OperationalRuleSet.VENDA_CRIAR_MODALIDADE, tenant.orgao(), entity.getLicencaEspecialGozoId(), entity.getModalidade());
    validate(entity);
  }
  @Override protected void beforeUpdate(Long id, VendaLicencaEspecial existing, UpdateVendaLicencaEspecialDTO dto) {
    TempoLicencaValidation.orgao(tenant, existing.getEmpCodigo());
    rules.enforceOperational(OperationalRuleSet.LICENCA_ESTADO_EDICAO, existing.getEmpCodigo(), existing.getSituacao(), dto.getSituacao());
    rules.enforceOperational(OperationalRuleSet.VENDA_DATAS_EDICAO, existing.getEmpCodigo(), existing.getDtPagamento(), dto.getDtPagamento(), existing.getDtEstorno(), dto.getDtEstorno());
    rules.enforceOperational(OperationalRuleSet.VENDA_ABONO_ORIGEM, existing.getEmpCodigo(), existing.getLicencaEspecialGozoId(), "editar", null, null, null, null, null, null, null, null);
    var candidate = mapper.candidate(dto);
    validateCadastralState(candidate,existing);
    rules.enforceOperational(OperationalRuleSet.LICENCA_PERIODO_EDICAO, existing.getEmpCodigo(), existing.getPeriodoAquisitivoLicencaId(), dto.getPeriodoAquisitivoLicencaId());
    candidate.setId(id);
    validate(candidate);
    dto.setEmpCodigo(candidate.getEmpCodigo());
    dto.setNumfunc(candidate.getNumfunc());
    dto.setNumvinc(candidate.getNumvinc());
    dto.setChavePeraqlicesp(candidate.getChavePeraqlicesp());
  }
  private void validate(VendaLicencaEspecial entity) {
    hydrateParentPeriodoAquisitivo(entity);
    var vinculo = TempoLicencaValidation.cadastro(entity, tenant, getEntityManager());
    if(entity.getLicencaEspecialGozoId()==null) TempoLicencaValidation.validarConsumo(entity, vinculo, tenant, getEntityManager(), rules);
    else {
      var gozo=getEntityManager().find(LicencaEspecialGozo.class,entity.getLicencaEspecialGozoId());
      if (gozo == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Origem do abono não encontrada.");
      ErgonTenantAccess.requireReferencesInOrganization(entity.getEmpCodigo(), gozo);
      rules.enforceOperational(OperationalRuleSet.VENDA_PERIODO_ORIGEM, entity.getEmpCodigo(), gozo.getPeriodoAquisitivoLicencaId(), entity.getPeriodoAquisitivoLicencaId());
      rules.enforceOperational(OperationalRuleSet.VENDA_MODALIDADE_ORIGEM, entity.getEmpCodigo(), entity.getModalidade());
    }

    // Vendas avulsas ativas são avaliadas pelo RuleSet; estes registros preservam a integridade histórica.
    rules.enforceOperational(OperationalRuleSet.VENDA_DIAS_VALOR, entity.getEmpCodigo(), entity.getLicencaEspecialGozoId(), entity.getSituacao(), entity.getDiasVendidos(), entity.getValorIndenizacao());
  }

  public void hydrateParentPeriodoAquisitivo(VendaLicencaEspecial entity) {
    TempoLicencaValidation.hydrate(entity, tenant, getEntityManager());
  }


  @Override protected VendaLicencaEspecial findEntityById(Long id) {
    var entity = super.findEntityById(id);
    TempoLicencaValidation.orgao(tenant, entity.getEmpCodigo());
    return entity;
  }
  @Override protected List<VendaLicencaEspecial> findEntitiesById(Collection<Long> ids) {
    var entities = super.findEntitiesById(ids);
    entities.forEach(entity -> TempoLicencaValidation.orgao(tenant, entity.getEmpCodigo()));
    return entities;
  }
  @Override protected List<VendaLicencaEspecial> findAllEntities() {
    return getRepository().findAll(resolveResourceFilterAccessScope().specification(), getDefaultSort());
  }

  @Override protected void beforeDelete(Long id, VendaLicencaEspecial entity) {
    TempoLicencaValidation.orgao(tenant, entity.getEmpCodigo());
    TempoLicencaLifecycle.requireState(entity,"SOLICITADA");
    rules.enforceOperational(OperationalRuleSet.VENDA_ABONO_ORIGEM, entity.getEmpCodigo(), entity.getLicencaEspecialGozoId(), "excluir", null, null, null, null, null, null, null, null);
    TempoLicencaValidation.parent(entity.getPeriodoAquisitivoLicencaId(), tenant, getEntityManager());
  }
  @Override protected void beforeDeleteAllById(Collection<Long> ids) {
    ids.forEach(this::findEntityById);
  }
  @Override protected void afterCreate(CreateVendaLicencaEspecialDTO dto, VendaLicencaEspecial saved) {
    TempoLicencaValidation.sincronizar(saved.getPeriodoAquisitivoLicencaId(), tenant, getEntityManager(), rules);
  }
  @Override protected void afterUpdate(Long id, VendaLicencaEspecial saved, UpdateVendaLicencaEspecialDTO dto) {
    TempoLicencaValidation.sincronizar(saved.getPeriodoAquisitivoLicencaId(), tenant, getEntityManager(), rules);
  }
  @Override protected void afterDelete(Long id, VendaLicencaEspecial deleted) {
    TempoLicencaValidation.sincronizar(deleted.getPeriodoAquisitivoLicencaId(), tenant, getEntityManager(), rules);
  }
  @Override protected List<CollectionExportField> exportFields() {
    var date = new CollectionExportFieldPresentation("date", "dd/MM/yyyy", null, "pt-BR", "America/Sao_Paulo", null, null, "Não informado");
    var number = new CollectionExportFieldPresentation("number", "0", null, "pt-BR", null, null, null, "Não informado");
    var bool = new CollectionExportFieldPresentation("boolean", null, null, "pt-BR", null, "Sim", "Não", "Não informado");
    return List.of(field("id", "Identidade", "integer"), field("descricao", "Assentamento", "string"),
        field("empCodigo", "Órgão", "integer"), field("numfunc", "Servidor", "integer"), field("numvinc", "Vínculo", "integer"),
        presentedField("dtini", "Início", "date", date), presentedField("dtfim", "Término", "date", date),
        field("situacao", "Situação", "string"), field("diasVendidos", "Dias vendidos", "integer"),
        field("modalidade", "Modalidade", "string"),
        presentedField("dtPagamento", "Pagamento", "date", date),
        presentedField("dtEstorno", "Estorno", "date", date),
        presentedField("mesAnoFolha", "Competência da folha", "date", date),
        presentedField("numFolha", "Folha", "integer", number));
  }
  @Override public StatsFieldRegistry getStatsFieldRegistry() {
    return StatsFieldRegistry.builder()
        .groupByBucket("id", "id", Set.of(StatsMetric.COUNT, StatsMetric.SUM))
        .groupByBucket("situacao", "situacao", Set.of(StatsMetric.COUNT, StatsMetric.SUM, StatsMetric.AVG, StatsMetric.MEDIAN, StatsMetric.PERCENTILE_90))
        .temporalTimeSeriesField("dtini", "dtini")
        .numericHistogramMeasureField("diasVendidos", "diasVendidos").build();
  }

  @org.springframework.transaction.annotation.Transactional
  public org.praxisplatform.uischema.concurrency.ResourceRepresentationResult<VendaLicencaEspecialResponseDTO> patch(Long id,
      org.praxisplatform.uischema.dto.PartialUpdateInput patch,
      org.praxisplatform.uischema.concurrency.ResourceVersionUpdatePrecondition<Long> precondition) {
    if(precondition==null) throw org.praxisplatform.uischema.concurrency.ResourceVersionPreconditionException.required();
    var entity = findEntityById(id);
    lockForWrite(entity);
    TempoLicencaValidation.orgao(tenant, entity.getEmpCodigo());
    precondition.requireMatch(entity.getVersion());
    var dto = new UpdateVendaLicencaEspecialDTO();
    org.springframework.beans.BeanUtils.copyProperties(mapper.toResponse(entity), dto);
    org.praxisplatform.uischema.service.base.ResourceStateCopySupport.copySuppliedPatchProperties(patch, dto, resourceObjectMapper);
    if(!beanValidator.validate(dto).isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dados do assentamento incompatíveis com o contrato.");
    if(entity.getLicencaEspecialGozoId()!=null) {
      var prepared = prepareCustomFields(dto, "request");
      validatePreparedCustomFieldsForUpdate(prepared, id, entity, dto);
      rules.enforceOperational(OperationalRuleSet.VENDA_PATCH_ESTORNO, entity.getEmpCodigo(), entity.getSituacao());
      rules.enforceOperational(OperationalRuleSet.VENDA_ABONO_ORIGEM, entity.getEmpCodigo(), entity.getLicencaEspecialGozoId(), patch instanceof VendaLicencaEspecialLiquidacaoPatchDTO ? "liquidacao" : "patch", entity.getMesAnoFolha(), dto.getMesAnoFolha(), entity.getNumFolha(), dto.getNumFolha(), entity.getDocumentoLegalId(), dto.getDocumentoLegalId(), entity.getAtoPublicacaoId(), dto.getAtoPublicacaoId());
      var candidate=mapper.candidate(dto);candidate.setId(id);
      validateCadastralState(candidate,entity);
      validate(candidate);
      // O PATCH financeiro não passa pelo merge cadastral que controla a origem do abono.
      entity.setValorIndenizacao(dto.getValorIndenizacao());entity.setObservacoes(dto.getObservacoes());
      var saved=getRepository().save(entity);afterUpdate(id,saved,dto);
      getEntityManager().flush();getEntityManager().refresh(saved);
      var result=toResourceRepresentation(saved);result.requirePersistedVersion();return result;
    }
    return update(id, dto, precondition);
  }

  /** Candidato isolado utilizado na avaliação e novamente sob os locks do executor durável. */
  public void validateBulkCandidate(VendaLicencaEspecial candidate, VendaLicencaEspecial previous) {
    if (previous != null) {
      rules.enforceOperational(OperationalRuleSet.VENDA_ABONO_ORIGEM, previous.getEmpCodigo(), previous.getLicencaEspecialGozoId(), "lote", null, null, null, null, null, null, null, null);
    }
    validate(candidate);
  }
  @org.springframework.transaction.annotation.Transactional
  public org.praxisplatform.uischema.concurrency.ResourceRepresentationResult<VendaLicencaEspecialResponseDTO> transition(Long id, String action,
      TempoLicencaTransicaoDTO dto, org.praxisplatform.uischema.concurrency.ResourceVersionUpdatePrecondition<Long> precondition) {
    if (precondition == null) throw org.praxisplatform.uischema.concurrency.ResourceVersionPreconditionException.required();
    if(getEntityManager().unwrap(org.hibernate.Session.class).isDirty()) throw new IllegalStateException("A transição exige contexto de persistência limpo.");
    var entity = findEntityById(id);
    lockForWrite(entity);
    var authorizationId = new java.util.concurrent.atomic.AtomicReference<Long>();
    var result = executeVersionedMutation(id, precondition, current -> {
    TempoLicencaValidation.orgao(tenant, current.getEmpCodigo());
    if("reconciliar-abono-vinculado".equals(action)) {
      rules.enforceOperational(OperationalRuleSet.VENDA_RECONCILIAR_ORIGEM, current.getEmpCodigo(), current.getLicencaEspecialGozoId());
      TempoLicencaLifecycle.requireState(current,"AUTORIZADA","PAGA","ESTORNADA");
      if(dto==null || dto.getDataEfeito()==null || dto.getJustificativa()==null || dto.getJustificativa().isBlank())
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data e justificativa do ato são obrigatórias.");
      rules.enforceOperational(OperationalRuleSet.VENDA_RECONCILIAR_DATA, current.getEmpCodigo(), current.getDtEstorno(), dto.getDataEfeito());
      // lockForWrite já bloqueou e atualizou o gozo antes do período e desta venda.
      var gozo=getEntityManager().find(LicencaEspecialGozo.class,current.getLicencaEspecialGozoId());
      if (gozo == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Origem do abono não encontrada.");
      ErgonTenantAccess.requireReferencesInOrganization(current.getEmpCodigo(), gozo);
      rules.enforceOperational(OperationalRuleSet.VENDA_PERIODO_ORIGEM, current.getEmpCodigo(), gozo.getPeriodoAquisitivoLicencaId(), current.getPeriodoAquisitivoLicencaId());
      TempoLicencaValidation.parent(gozo.getPeriodoAquisitivoLicencaId(),tenant,getEntityManager());
      rules.enforceOperational(OperationalRuleSet.VENDA_RECONCILIAR_GOZO, current.getEmpCodigo(), gozo.getSituacao(), gozo.getAbonoPecuniario());
      var autorizacao=TempoLicencaValidation.reconciliarAbono(gozo,getEntityManager(),rules);
      TempoLicencaValidation.sincronizar(gozo.getPeriodoAquisitivoLicencaId(),tenant,getEntityManager(), rules);
      getEntityManager().flush();getEntityManager().refresh(current);
      if(autorizacao!=null) authorizationId.set(autorizacao.getId());
      return;
    }
    var input = new UpdateVendaLicencaEspecialDTO(); org.springframework.beans.BeanUtils.copyProperties(mapper.toResponse(current), input);
    var candidate = mapper.candidate(input); candidate.setId(id);
    TempoLicencaLifecycle.transition(candidate, action, dto, tenant, getEntityManager(), rules);
    validate(candidate);
    org.springframework.beans.BeanUtils.copyProperties(candidate, current, "id", "version", "auditoria", "orgao", "pessoa", "vinculo", "documentoLegal", "atoPublicacao", "averbacaoOrigem", "periodoAquisitivoLicenca");
    getEntityManager().flush();
    TempoLicencaValidation.sincronizar(current.getPeriodoAquisitivoLicencaId(), tenant, getEntityManager(), rules);
    getEntityManager().refresh(current);
    });
    if (authorizationId.get() != null) result.body().setAutorizacaoAtualId(authorizationId.get());
    return result;
  }

  @Override
  @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ERGON_READ') and hasAuthority('ERGON_WRITE')")
  @org.springframework.transaction.annotation.Transactional(readOnly = true, propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
  public VendaLicencaEspecialDuplicateDraftDTO duplicateDraft(Long sourceId) {
    var source=findEntityById(sourceId);
    if (!canPrepareDuplicateDraft(source))
      throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN,"duplicate-draft-unavailable");
    var draft=new VendaLicencaEspecialDuplicateDraftDTO();
    draft.setDescricao(source.getDescricao());
    draft.setEmpCodigo(source.getEmpCodigo());
    draft.setDiasVendidos(source.getDiasVendidos());
    return draft;
  }
  private static boolean canPrepareDuplicateDraft(VendaLicencaEspecial source) {
    return source.getLicencaEspecialGozoId() == null
        && "PECUNIA".equals(source.getModalidade())
        && source.getSituacao() != null
        && Set.of("SOLICITADA", "AUTORIZADA", "PAGA").contains(source.getSituacao());
  }

  private void validateCadastralState(VendaLicencaEspecial candidate, VendaLicencaEspecial previous) {
    if (previous == null) return;
    rules.enforceOperational(OperationalRuleSet.VENDA_RETIFICACAO, previous.getEmpCodigo(), previous.getSituacao(), previous.getDiasVendidos(), candidate.getDiasVendidos(), previous.getValorIndenizacao(), candidate.getValorIndenizacao(), previous.getMesAnoFolha(), candidate.getMesAnoFolha(), previous.getNumFolha(), candidate.getNumFolha());
  }
  /** Mesma ordem do gozo: parcela de origem, período, abono. */
  private void lockForWrite(VendaLicencaEspecial entity) {
    Long gozoId=entity.getLicencaEspecialGozoId();
    if(gozoId!=null) {
      var gozo=getEntityManager().find(LicencaEspecialGozo.class,gozoId);
      if(gozo==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Origem do abono não encontrada.");
      TempoLicencaValidation.orgao(tenant,gozo.getEmpCodigo());
      getEntityManager().lock(gozo,LockModeType.PESSIMISTIC_WRITE,Map.of("jakarta.persistence.lock.timeout",1000));getEntityManager().refresh(gozo);
      TempoLicencaValidation.orgao(tenant,gozo.getEmpCodigo());
      if (gozo == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Origem do abono não encontrada.");
      ErgonTenantAccess.requireReferencesInOrganization(entity.getEmpCodigo(), gozo);
      rules.enforceOperational(OperationalRuleSet.VENDA_PERIODO_ORIGEM, entity.getEmpCodigo(), gozo.getPeriodoAquisitivoLicencaId(), entity.getPeriodoAquisitivoLicencaId());
      TempoLicencaValidation.parent(gozo.getPeriodoAquisitivoLicencaId(),tenant,getEntityManager());
    }
    getEntityManager().lock(entity,LockModeType.PESSIMISTIC_WRITE,Map.of("jakarta.persistence.lock.timeout",1000));getEntityManager().refresh(entity);
    TempoLicencaValidation.orgao(tenant,entity.getEmpCodigo());
    if(!Objects.equals(gozoId,entity.getLicencaEspecialGozoId()))
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Origem do abono alterada durante a operação.");
  }
  public void validateBulkUpdate(VendaLicencaEspecial candidate, VendaLicencaEspecial previous) { validateCadastralState(candidate,previous); validateBulkCandidate(candidate,previous); }
  @Override protected java.util.Optional<org.praxisplatform.uischema.capability.ResourceStateSnapshot> toResourceStateSnapshot(VendaLicencaEspecial e) {
    return java.util.Optional.of(snapshotState(e));
  }
  private org.praxisplatform.uischema.capability.ResourceStateSnapshot snapshotState(VendaLicencaEspecial e) {
    return org.praxisplatform.uischema.capability.ResourceStateSnapshot.of(e.getSituacao(),
        Map.of("duplicateDraftEligible", canPrepareDuplicateDraft(e)));
  }

  @Override protected String resolveResourceKey() { return "administracao-pessoal.vendas-licenca-especial"; }

}
