package br.com.praxis.ergonshowcase.service;
import br.com.praxis.ergonshowcase.config.TenantContext;
import br.com.praxis.ergonshowcase.domain.*;
import br.com.praxis.ergonshowcase.rules.ErgonRuleEngineFacade;
import br.com.praxis.ergonshowcase.rules.OperationalRuleSet;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import br.com.praxis.ergonshowcase.dto.*;
import br.com.praxis.ergonshowcase.mapper.AverbacaoTempoServicoMapper;
import br.com.praxis.ergonshowcase.repository.AverbacaoTempoServicoRepository;
import jakarta.persistence.*;
import java.util.*;
import org.praxisplatform.uischema.exporting.*;
import org.praxisplatform.uischema.service.base.ResourceFilterAccessScope;
import org.praxisplatform.uischema.stats.*;
import org.springframework.stereotype.Service;

@Service
public class AverbacaoTempoServicoService extends ErgonResourceService<AverbacaoTempoServico, AverbacaoTempoServicoResponseDTO, Long, AverbacaoTempoServicoFilterDTO, CreateAverbacaoTempoServicoDTO, UpdateAverbacaoTempoServicoDTO>
    implements org.praxisplatform.uischema.service.base.VersionedCreateUpdateResourceService<
        AverbacaoTempoServicoResponseDTO, Long, AverbacaoTempoServicoFilterDTO, CreateAverbacaoTempoServicoDTO, UpdateAverbacaoTempoServicoDTO>,
        org.praxisplatform.uischema.service.base.DuplicateDraftResourceService<AverbacaoTempoServicoResponseDTO, Long, AverbacaoTempoServicoFilterDTO, CreateAverbacaoTempoServicoDTO, UpdateAverbacaoTempoServicoDTO, AverbacaoTempoServicoDuplicateDraftDTO> {
  private final AverbacaoTempoServicoMapper mapper;
  private final TenantContext tenant;
  @org.springframework.beans.factory.annotation.Autowired private jakarta.validation.Validator beanValidator;
  @org.springframework.beans.factory.annotation.Autowired private ErgonRuleEngineFacade rules = new ErgonRuleEngineFacade();
  public AverbacaoTempoServicoService(AverbacaoTempoServicoRepository repository, AverbacaoTempoServicoMapper mapper, TenantContext tenant) {
    super(repository, AverbacaoTempoServico.class, "id");
    this.mapper = mapper;
    this.tenant = tenant;
  }


  /** Executa as mesmas guardas e a sincronização da exclusão individual no lote. */
  @Override @org.springframework.transaction.annotation.Transactional
  public void deleteAllById(Collection<Long> ids) {
    if (ids == null) throw new IllegalArgumentException("ids must not be null");
    ids.stream().distinct().sorted().forEach(this::deleteById);
  }
  @Override protected AverbacaoTempoServicoMapper getResourceMapper() { return mapper; }
  @Override protected AverbacaoTempoServicoFilterDTO emptyFilter() { return new AverbacaoTempoServicoFilterDTO(); }
  @Override public Optional<String> getDatasetVersion() { return Optional.empty(); }
  @Override protected AverbacaoTempoServicoFilterDTO normalizeOptionSourceFilter(
      org.praxisplatform.uischema.options.OptionSourceDescriptor descriptor,
      org.praxisplatform.uischema.options.service.OptionSourceOperation operation,
      AverbacaoTempoServicoFilterDTO filter) {
    return TempoLicencaValidation.scopeFilter(filter == null ? emptyFilter() : filter, tenant);
  }

  @Override protected void beforeCreate(CreateAverbacaoTempoServicoDTO dto, AverbacaoTempoServico entity) {
    TempoLicencaValidation.orgao(tenant, entity.getEmpCodigo());
    TempoLicencaLifecycle.requireState(entity, "EM_ANALISE");
    rules.enforceOperational(OperationalRuleSet.AVERBACAO_CRIAR, entity.getEmpCodigo(), entity.getDiasDesaverbados(), entity.getDataDesaverbacao(), entity.getDataRemocao(), entity.getMotivoRemocao(), entity.getMotivoDesaverbacao());
    validate(entity);
  }
  @Override protected void beforeUpdate(Long id, AverbacaoTempoServico existing, UpdateAverbacaoTempoServicoDTO dto) {
    TempoLicencaValidation.orgao(tenant, existing.getEmpCodigo());
    rules.enforceOperational(OperationalRuleSet.LICENCA_ESTADO_EDICAO, existing.getEmpCodigo(), existing.getSituacao(), dto.getSituacao());
    rules.enforceOperational(OperationalRuleSet.AVERBACAO_ATO_EDICAO, existing.getEmpCodigo(), existing.getDiasDesaverbados(), dto.getDiasDesaverbados(), existing.getDataRemocao(), dto.getDataRemocao(), existing.getDataDesaverbacao(), dto.getDataDesaverbacao(), existing.getMotivoDesaverbacao(), dto.getMotivoDesaverbacao(), existing.getMotivoRemocao(), dto.getMotivoRemocao());
    var candidate = mapper.candidate(dto);
    candidate.setId(id);
    if (candidate.getChave() == null) candidate.setChave(existing.getChave());
    validateCadastralState(candidate,existing);
    validate(candidate);
    dto.setChave(candidate.getChave());
    dto.setTotalDias(candidate.getTotalDias());
    dto.setDiasFinalidade(candidate.getDiasFinalidade());
  }
  private void validate(AverbacaoTempoServico entity) {

    var vinculo = TempoLicencaValidation.cadastro(entity, tenant, getEntityManager());
    getEntityManager().lock(vinculo, LockModeType.PESSIMISTIC_WRITE);
    if (entity.getChave() == null) {
      entity.setChave(TempoLicencaRuleFacts.proximaChave(
          AverbacaoTempoServico.class, entity, vinculo, getEntityManager()));
    }
    var dias = TempoLicencaRuleFacts.inclusive(entity.getDtini(), entity.getDtfim());
    if (entity.getTotalDias() == null && dias != null && dias > 0)
      entity.setTotalDias(Math.toIntExact(dias));
    if (entity.getDiasFinalidade() == null) entity.setDiasFinalidade(entity.getTotalDias());
    rules.enforceAverbacaoTempoServico(entity.getEmpCodigo(),
        TempoLicencaRuleFacts.averbacao(entity, vinculo, getEntityManager(), 0));

  }


  @Override protected AverbacaoTempoServico findEntityById(Long id) {
    var entity = super.findEntityById(id);
    TempoLicencaValidation.orgao(tenant, entity.getEmpCodigo());
    return entity;
  }
  @Override protected List<AverbacaoTempoServico> findEntitiesById(Collection<Long> ids) {
    var entities = super.findEntitiesById(ids);
    entities.forEach(entity -> TempoLicencaValidation.orgao(tenant, entity.getEmpCodigo()));
    return entities;
  }
  @Override protected List<AverbacaoTempoServico> findAllEntities() {
    return getRepository().findAll(resolveResourceFilterAccessScope().specification(), getDefaultSort());
  }

  @Override protected void beforeDelete(Long id, AverbacaoTempoServico entity) {
    TempoLicencaValidation.orgao(tenant, entity.getEmpCodigo());
    TempoLicencaLifecycle.requireState(entity,"EM_ANALISE");
    getEntityManager().lock(entity, LockModeType.PESSIMISTIC_WRITE, Map.of("jakarta.persistence.lock.timeout",1000));
    TempoLicencaValidation.semPeriodos(id, entity.getEmpCodigo(), getEntityManager());
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
        field("situacao", "Situação", "string"), field("totalDias", "Dias reconhecidos", "integer"),
        field("instituicao", "Instituição", "string"),
        field("tipoTempo", "Natureza", "string"),
        field("finalidade", "Finalidade", "string"),
        presentedField("diasFinalidade", "Dias por finalidade", "integer", number),
        presentedField("diasDesaverbados", "Dias desaverbados", "integer", number),
        presentedField("dataDesaverbacao", "Data de desaverbação", "date", date),
        presentedField("sobrepoe", "Sobreposição", "boolean", bool));
  }
  @Override public StatsFieldRegistry getStatsFieldRegistry() {
    return StatsFieldRegistry.builder()
        .groupByBucket("id", "id", Set.of(StatsMetric.COUNT, StatsMetric.SUM))
        .groupByBucket("situacao", "situacao", Set.of(StatsMetric.COUNT, StatsMetric.SUM, StatsMetric.AVG, StatsMetric.MEDIAN, StatsMetric.PERCENTILE_90))
        .temporalTimeSeriesField("dtini", "dtini")
        .numericHistogramMeasureField("totalDias", "totalDias").build();
  }

  @org.springframework.transaction.annotation.Transactional
  public org.praxisplatform.uischema.concurrency.ResourceRepresentationResult<AverbacaoTempoServicoResponseDTO> patch(Long id,
      org.praxisplatform.uischema.dto.PartialUpdateInput patch,
      org.praxisplatform.uischema.concurrency.ResourceVersionUpdatePrecondition<Long> precondition) {
    if(precondition==null) throw org.praxisplatform.uischema.concurrency.ResourceVersionPreconditionException.required();
    var entity = findEntityById(id);
    TempoLicencaValidation.orgao(tenant, entity.getEmpCodigo());
    var dto = new UpdateAverbacaoTempoServicoDTO();
    org.springframework.beans.BeanUtils.copyProperties(mapper.toResponse(entity), dto);
    org.praxisplatform.uischema.service.base.ResourceStateCopySupport.copySuppliedPatchProperties(patch, dto, resourceObjectMapper);
    if(!beanValidator.validate(dto).isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dados do assentamento incompatíveis com o contrato.");
    return update(id, dto, precondition);
  }

  /** Candidato isolado utilizado na avaliação e novamente sob os locks do executor durável. */
  public void validateBulkCandidate(AverbacaoTempoServico candidate, AverbacaoTempoServico previous) {
    validate(candidate);
  }
  @org.springframework.transaction.annotation.Transactional
  public org.praxisplatform.uischema.concurrency.ResourceRepresentationResult<AverbacaoTempoServicoResponseDTO> transition(Long id, String action,
      TempoLicencaTransicaoDTO dto, org.praxisplatform.uischema.concurrency.ResourceVersionUpdatePrecondition<Long> precondition) {
    return executeVersionedMutation(id, precondition, entity -> {
      TempoLicencaValidation.orgao(tenant, entity.getEmpCodigo());
      var input = new UpdateAverbacaoTempoServicoDTO(); org.springframework.beans.BeanUtils.copyProperties(mapper.toResponse(entity), input);
    var candidate = mapper.candidate(input); candidate.setId(id);
    TempoLicencaLifecycle.transition(candidate, action, dto, tenant, getEntityManager(), rules);
    validate(candidate);
    org.springframework.beans.BeanUtils.copyProperties(candidate, entity, "id", "version", "auditoria", "orgao", "pessoa", "vinculo", "documentoLegal", "atoPublicacao", "averbacaoOrigem", "periodoAquisitivoLicenca");
    getEntityManager().flush();

    TempoLicencaValidation.reconciliarOrigem(entity, getEntityManager(), rules);
    getEntityManager().flush();
    });
  }

  @Override
  @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ERGON_READ') and hasAuthority('ERGON_WRITE')")
  @org.springframework.transaction.annotation.Transactional(readOnly = true, propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
  public AverbacaoTempoServicoDuplicateDraftDTO duplicateDraft(Long sourceId) {
    var source=findEntityById(sourceId);
    if(Set.of("CANCELADO","ESTORNADA","PRESCRITO").contains(source.getSituacao()))
      throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN,"duplicate-draft-unavailable");
    var draft=new AverbacaoTempoServicoDuplicateDraftDTO();
    draft.setDescricao(source.getDescricao());
    draft.setInstituicao(source.getInstituicao());
    draft.setTipoTempo(source.getTipoTempo());
    draft.setRegimePrevidenciario(source.getRegimePrevidenciario());
    draft.setFinalidade(source.getFinalidade());
    return draft;
  }
  public void synchronizeOrigin(AverbacaoTempoServico entity) {
    TempoLicencaValidation.reconciliarOrigem(entity, getEntityManager(), rules);
  }
  private void validateCadastralState(AverbacaoTempoServico candidate, AverbacaoTempoServico previous) {
    if (previous == null) return;
    rules.enforceOperational(OperationalRuleSet.AVERBACAO_RETIFICACAO, previous.getEmpCodigo(), previous.getSituacao(), previous.getTotalDias(), candidate.getTotalDias(), previous.getDiasFinalidade(), candidate.getDiasFinalidade(), previous.getFinalidade(), candidate.getFinalidade(), previous.getTipoTempo(), candidate.getTipoTempo(), previous.getInstituicao(), candidate.getInstituicao(), previous.getDtini(), candidate.getDtini(), previous.getDtfim(), candidate.getDtfim());
  }
  public void validateBulkUpdate(AverbacaoTempoServico candidate, AverbacaoTempoServico previous) { validateCadastralState(candidate,previous); validateBulkCandidate(candidate,previous); }
  @Override protected java.util.Optional<org.praxisplatform.uischema.capability.ResourceStateSnapshot> toResourceStateSnapshot(AverbacaoTempoServico e) {
    return java.util.Optional.of(snapshotState(e));
  }
  private org.praxisplatform.uischema.capability.ResourceStateSnapshot snapshotState(AverbacaoTempoServico e) {
    return org.praxisplatform.uischema.capability.ResourceStateSnapshot.of(e.getSituacao());
  }

  @Override protected String resolveResourceKey() { return "administracao-pessoal.averbacoes-tempo-servico"; }

}
