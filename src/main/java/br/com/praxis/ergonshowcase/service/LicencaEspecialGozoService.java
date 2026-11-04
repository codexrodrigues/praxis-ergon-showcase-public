package br.com.praxis.ergonshowcase.service;
import br.com.praxis.ergonshowcase.config.TenantContext;
import br.com.praxis.ergonshowcase.domain.*;
import br.com.praxis.ergonshowcase.rules.ErgonRuleEngineFacade;
import br.com.praxis.ergonshowcase.rules.OperationalRuleSet;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.mapper.LicencaEspecialGozoMapper;
import br.com.praxis.ergonshowcase.repository.LicencaEspecialGozoRepository;
import jakarta.persistence.*;
import java.util.*;
import org.praxisplatform.uischema.exporting.*;
import org.praxisplatform.uischema.service.base.ResourceFilterAccessScope;
import org.praxisplatform.uischema.stats.*;
import org.springframework.stereotype.Service;

@Service
public class LicencaEspecialGozoService extends ErgonResourceService<LicencaEspecialGozo, LicencaEspecialGozoResponseDTO, Long, LicencaEspecialGozoFilterDTO, CreateLicencaEspecialGozoDTO, UpdateLicencaEspecialGozoDTO>
    implements org.praxisplatform.uischema.service.base.VersionedCreateUpdateResourceService<
        LicencaEspecialGozoResponseDTO, Long, LicencaEspecialGozoFilterDTO, CreateLicencaEspecialGozoDTO, UpdateLicencaEspecialGozoDTO>,
        org.praxisplatform.uischema.service.base.DuplicateDraftResourceService<LicencaEspecialGozoResponseDTO, Long, LicencaEspecialGozoFilterDTO, CreateLicencaEspecialGozoDTO, UpdateLicencaEspecialGozoDTO, LicencaEspecialGozoDuplicateDraftDTO> {
  private final LicencaEspecialGozoMapper mapper;
  private final TenantContext tenant;
  @org.springframework.beans.factory.annotation.Autowired private jakarta.validation.Validator beanValidator;
  @org.springframework.beans.factory.annotation.Autowired private ErgonRuleEngineFacade rules = new ErgonRuleEngineFacade();
  public LicencaEspecialGozoService(LicencaEspecialGozoRepository repository, LicencaEspecialGozoMapper mapper, TenantContext tenant) {
    super(repository, LicencaEspecialGozo.class, "id");
    this.mapper = mapper;
    this.tenant = tenant;
  }


  /** Executa as mesmas guardas e a sincronização da exclusão individual no lote. */
  @Override @org.springframework.transaction.annotation.Transactional
  public void deleteAllById(Collection<Long> ids) {
    if (ids == null) throw new IllegalArgumentException("ids must not be null");
    ids.stream().distinct().sorted().forEach(this::deleteById);
  }
  @Override protected LicencaEspecialGozoMapper getResourceMapper() { return mapper; }
  @Override protected LicencaEspecialGozoFilterDTO emptyFilter() { return new LicencaEspecialGozoFilterDTO(); }
  @Override public Optional<String> getDatasetVersion() { return Optional.empty(); }
  @Override protected LicencaEspecialGozoFilterDTO normalizeOptionSourceFilter(
      org.praxisplatform.uischema.options.OptionSourceDescriptor descriptor,
      org.praxisplatform.uischema.options.service.OptionSourceOperation operation,
      LicencaEspecialGozoFilterDTO filter) {
    return TempoLicencaValidation.scopeFilter(filter == null ? emptyFilter() : filter, tenant);
  }

  @Override protected Object prepareCreateValidationDraft(CreateLicencaEspecialGozoDTO dto, LicencaEspecialGozo candidate) {
    hydrateParentPeriodoAquisitivo(candidate);
    return mapper.toResponse(candidate);
  }
  @Override protected Object prepareUpdateValidationDraft(Long id, LicencaEspecialGozo candidate, UpdateLicencaEspecialGozoDTO dto) {
    mapper.applyUpdate(candidate, dto);
    hydrateParentPeriodoAquisitivo(candidate);
    return mapper.toResponse(candidate);
  }
  @Override protected void beforeCreate(CreateLicencaEspecialGozoDTO dto, LicencaEspecialGozo entity) {
    TempoLicencaLifecycle.requireState(entity, "PROGRAMADA");
    rules.enforceOperational(OperationalRuleSet.GOZO_CRIAR, tenant.orgao(), entity.getDtfim());
    validate(entity);
  }
  @Override protected void beforeUpdate(Long id, LicencaEspecialGozo existing, UpdateLicencaEspecialGozoDTO dto) {
    TempoLicencaValidation.orgao(tenant, existing.getEmpCodigo());
    rules.enforceOperational(OperationalRuleSet.LICENCA_ESTADO_EDICAO, existing.getEmpCodigo(), existing.getSituacao(), dto.getSituacao());
    rules.enforceOperational(OperationalRuleSet.GOZO_ENCERRAMENTO_EDICAO, existing.getEmpCodigo(), existing.getDtfim(), dto.getDtfim());
    var candidate = mapper.candidate(dto);
    validateCadastralState(candidate,existing);
    rules.enforceOperational(OperationalRuleSet.LICENCA_PERIODO_EDICAO, existing.getEmpCodigo(), existing.getPeriodoAquisitivoLicencaId(), dto.getPeriodoAquisitivoLicencaId());
    candidate.setId(id);
    validate(candidate);
    dto.setEmpCodigo(candidate.getEmpCodigo());
    dto.setNumfunc(candidate.getNumfunc());
    dto.setNumvinc(candidate.getNumvinc());
    dto.setChavePeraqlicesp(candidate.getChavePeraqlicesp());
    dto.setDtprevfim(candidate.getDtprevfim());
    dto.setDiasGozo(candidate.getDiasGozo());
  }
  private void validate(LicencaEspecialGozo entity) {
    hydrateParentPeriodoAquisitivo(entity);
    var vinculo = TempoLicencaValidation.cadastro(entity, tenant, getEntityManager());
    if(entity.getNumfuncSubstituto()!=null && getEntityManager().find(ServidorPessoa.class,entity.getNumfuncSubstituto())==null)
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Servidor substituto inexistente.");
    TempoLicencaValidation.validarConsumo(entity, vinculo, tenant, getEntityManager(), rules);
    TempoLicencaValidation.validarAbonoLiquidado(entity, getEntityManager(), rules);


  }

  public void hydrateParentPeriodoAquisitivo(LicencaEspecialGozo entity) {
    TempoLicencaValidation.hydrate(entity, tenant, getEntityManager());
    if (entity.getDtini() != null && entity.getDtprevfim() == null
        && entity.getDiasGozo() != null && entity.getDiasGozo() > 0) {
      entity.setDtprevfim(entity.getDtini().plusDays(entity.getDiasGozo().longValue() - 1));
    }
    if (entity.getDiasGozo() == null) {
      var fim = entity.getDtfim() == null ? entity.getDtprevfim() : entity.getDtfim();
      var dias = TempoLicencaRuleFacts.inclusive(entity.getDtini(), fim);
      if (dias != null && dias > 0) entity.setDiasGozo(Math.toIntExact(dias));
    }
  }


  @Override protected LicencaEspecialGozo findEntityById(Long id) {
    var entity = super.findEntityById(id);
    TempoLicencaValidation.orgao(tenant, entity.getEmpCodigo());
    return entity;
  }
  @Override protected List<LicencaEspecialGozo> findEntitiesById(Collection<Long> ids) {
    var entities = super.findEntitiesById(ids);
    entities.forEach(entity -> TempoLicencaValidation.orgao(tenant, entity.getEmpCodigo()));
    return entities;
  }
  @Override protected List<LicencaEspecialGozo> findAllEntities() {
    return getRepository().findAll(resolveResourceFilterAccessScope().specification(), getDefaultSort());
  }

  @Override protected void beforeDelete(Long id, LicencaEspecialGozo entity) {
    TempoLicencaValidation.orgao(tenant, entity.getEmpCodigo());
    TempoLicencaValidation.parent(entity.getPeriodoAquisitivoLicencaId(), tenant, getEntityManager());
    TempoLicencaLifecycle.requireState(entity,"PROGRAMADA");
  }
  @Override protected void beforeDeleteAllById(Collection<Long> ids) {
    ids.forEach(this::findEntityById);
  }
  @Override protected void afterCreate(CreateLicencaEspecialGozoDTO dto, LicencaEspecialGozo saved) {
    TempoLicencaValidation.sincronizarAbono(saved,getEntityManager(),rules);
    TempoLicencaValidation.sincronizar(saved.getPeriodoAquisitivoLicencaId(), tenant, getEntityManager(), rules);
  }
  @Override protected void afterUpdate(Long id, LicencaEspecialGozo saved, UpdateLicencaEspecialGozoDTO dto) {
    TempoLicencaValidation.sincronizarAbono(saved,getEntityManager(),rules);
    TempoLicencaValidation.sincronizar(saved.getPeriodoAquisitivoLicencaId(), tenant, getEntityManager(), rules);
  }
  @Override protected void afterDelete(Long id, LicencaEspecialGozo deleted) {
    TempoLicencaValidation.sincronizar(deleted.getPeriodoAquisitivoLicencaId(), tenant, getEntityManager(), rules);
  }
  @Override protected List<CollectionExportField> exportFields() {
    var date = new CollectionExportFieldPresentation("date", "dd/MM/yyyy", null, "pt-BR", "America/Sao_Paulo", null, null, "Não informado");
    var number = new CollectionExportFieldPresentation("number", "0", null, "pt-BR", null, null, null, "Não informado");
    var bool = new CollectionExportFieldPresentation("boolean", null, null, "pt-BR", null, "Sim", "Não", "Não informado");
    return List.of(field("id", "Identidade", "integer"), field("descricao", "Assentamento", "string"),
        field("empCodigo", "Órgão", "integer"), field("numfunc", "Servidor", "integer"), field("numvinc", "Vínculo", "integer"),
        presentedField("dtini", "Início", "date", date), presentedField("dtfim", "Término", "date", date),
        field("situacao", "Situação", "string"), field("diasGozo", "Dias de gozo", "integer"),
        presentedField("dtprevfim", "Término previsto", "date", date),
        presentedField("diasAbono", "Dias de abono", "integer", number),
        presentedField("abonoPecuniario", "Com abono", "boolean", bool),
        presentedField("mesAnoFolha", "Competência da folha", "date", date),
        presentedField("numFolha", "Folha", "integer", number));
  }
  @Override public StatsFieldRegistry getStatsFieldRegistry() {
    return StatsFieldRegistry.builder()
        .groupByBucket("id", "id", Set.of(StatsMetric.COUNT, StatsMetric.SUM))
        .groupByBucket("situacao", "situacao", Set.of(StatsMetric.COUNT, StatsMetric.SUM, StatsMetric.AVG, StatsMetric.MEDIAN, StatsMetric.PERCENTILE_90))
        .temporalTimeSeriesField("dtini", "dtini")
        .numericHistogramMeasureField("diasGozo", "diasGozo").build();
  }

  @org.springframework.transaction.annotation.Transactional
  public org.praxisplatform.uischema.concurrency.ResourceRepresentationResult<LicencaEspecialGozoResponseDTO> patch(Long id,
      org.praxisplatform.uischema.dto.PartialUpdateInput patch,
      org.praxisplatform.uischema.concurrency.ResourceVersionUpdatePrecondition<Long> precondition) {
    if(precondition==null) throw org.praxisplatform.uischema.concurrency.ResourceVersionPreconditionException.required();
    var entity = findEntityById(id);
    TempoLicencaValidation.orgao(tenant, entity.getEmpCodigo());
    var dto = new UpdateLicencaEspecialGozoDTO();
    org.springframework.beans.BeanUtils.copyProperties(mapper.toResponse(entity), dto);
    org.praxisplatform.uischema.service.base.ResourceStateCopySupport.copySuppliedPatchProperties(patch, dto, resourceObjectMapper);
    if(!beanValidator.validate(dto).isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dados do assentamento incompatíveis com o contrato.");
    return update(id, dto, precondition);
  }

  /** Candidato isolado utilizado na avaliação e novamente sob os locks do executor durável. */
  public void validateBulkCandidate(LicencaEspecialGozo candidate, LicencaEspecialGozo previous) {
    validate(candidate);
  }
  @org.springframework.transaction.annotation.Transactional
  public org.praxisplatform.uischema.concurrency.ResourceRepresentationResult<LicencaEspecialGozoResponseDTO> transition(Long id, String action,
      TempoLicencaTransicaoDTO dto, org.praxisplatform.uischema.concurrency.ResourceVersionUpdatePrecondition<Long> precondition) {
    return executeVersionedMutation(id, precondition, entity -> {
      TempoLicencaValidation.orgao(tenant, entity.getEmpCodigo());
      var input = new UpdateLicencaEspecialGozoDTO(); org.springframework.beans.BeanUtils.copyProperties(mapper.toResponse(entity), input);
    var candidate = mapper.candidate(input); candidate.setId(id);
    TempoLicencaLifecycle.transition(candidate, action, dto, tenant, getEntityManager(), rules);
    validate(candidate);
    org.springframework.beans.BeanUtils.copyProperties(candidate, entity, "id", "version", "auditoria", "orgao", "pessoa", "vinculo", "documentoLegal", "atoPublicacao", "averbacaoOrigem", "periodoAquisitivoLicenca");
    getEntityManager().flush();
    TempoLicencaValidation.sincronizarAbono(entity,getEntityManager(),rules);
    TempoLicencaValidation.sincronizar(entity.getPeriodoAquisitivoLicencaId(), tenant, getEntityManager(), rules);
    });
  }

  @Override
  @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ERGON_READ') and hasAuthority('ERGON_WRITE')")
  @org.springframework.transaction.annotation.Transactional(readOnly = true, propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
  public LicencaEspecialGozoDuplicateDraftDTO duplicateDraft(Long sourceId) {
    var source=findEntityById(sourceId);
    if(Set.of("CANCELADO","ESTORNADA","PRESCRITO").contains(source.getSituacao()))
      throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN,"duplicate-draft-unavailable");
    var draft=new LicencaEspecialGozoDuplicateDraftDTO();
    draft.setDescricao(source.getDescricao());
    draft.setEmpCodigo(source.getEmpCodigo());
    draft.setDiasGozo(source.getDiasGozo());
    // Proposta de abono da licença (PCK_LIC_ESP), distinta do ABONO_CONST de férias.
    // Copiar o conjunto coerente não copia autorização; o novo create revalida a política.
    draft.setAbonoPecuniario(source.getAbonoPecuniario());
    draft.setDiasAbono(source.getDiasAbono());
    draft.setIniFimAbono(source.getIniFimAbono());
    return draft;
  }
  public void synchronizeAbono(LicencaEspecialGozo entity) { TempoLicencaValidation.sincronizarAbono(entity,getEntityManager(),rules); }
  private void validateCadastralState(LicencaEspecialGozo candidate, LicencaEspecialGozo previous) {
    if (previous == null) return;
    rules.enforceOperational(OperationalRuleSet.GOZO_RETIFICACAO, previous.getEmpCodigo(), previous.getSituacao(), previous.getDtini(), candidate.getDtini(), previous.getDtprevfim(), candidate.getDtprevfim(), previous.getDiasGozo(), candidate.getDiasGozo(), previous.getAbonoPecuniario(), candidate.getAbonoPecuniario(), previous.getDiasAbono(), candidate.getDiasAbono(), previous.getIniFimAbono(), candidate.getIniFimAbono());
  }
  public void validateBulkUpdate(LicencaEspecialGozo candidate, LicencaEspecialGozo previous) { validateCadastralState(candidate,previous); validateBulkCandidate(candidate,previous); }
  @Override protected java.util.Optional<org.praxisplatform.uischema.capability.ResourceStateSnapshot> toResourceStateSnapshot(LicencaEspecialGozo e) {
    return java.util.Optional.of(snapshotState(e));
  }
  private org.praxisplatform.uischema.capability.ResourceStateSnapshot snapshotState(LicencaEspecialGozo e) {
    return org.praxisplatform.uischema.capability.ResourceStateSnapshot.of(e.getSituacao());
  }

  @Override protected String resolveResourceKey() { return "administracao-pessoal.licencas-especiais-gozo"; }

}
