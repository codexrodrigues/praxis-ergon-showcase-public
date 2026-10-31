package br.com.praxis.ergonshowcase.bulk;

import br.com.praxis.ergonshowcase.bulk.ErgonBulkDomain.Resource;
import br.com.praxis.ergonshowcase.config.TenantContext;
import br.com.praxis.ergonshowcase.domain.*;
import br.com.praxis.ergonshowcase.operations.InstallBulkStorage;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.praxisplatform.uischema.action.ActionCollectionAtomicity;
import org.praxisplatform.uischema.bulk.*;
import org.praxisplatform.uischema.command.*;
import org.praxisplatform.uischema.concurrency.*;
import org.praxisplatform.uischema.dto.CursorPage;
import org.praxisplatform.uischema.openapi.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.*;
import org.springframework.web.server.ResponseStatusException;

/** Orquestração HTTP do kernel canônico; nenhum recibo ou executor paralelo pertence ao host. */
@Service
@SuppressWarnings({"rawtypes", "unchecked"})
public class ErgonBulkOperations {
  private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger(ErgonBulkOperations.class);
  static final String REVISION="ergon-bulk/2";
  @Autowired BulkExecutionInfrastructure infrastructure;
  @Autowired ObjectProvider<BulkOperationLifecycle> lifecycles;
  @Autowired PlatformTransactionManager transactionManager;
  @Autowired ObjectMapper json;
  @Autowired ErgonBulkDomain domain;
  @Autowired CanonicalOperationResolver operations;
  @Autowired OpenApiDocumentService documents;
  @Autowired ResourceVersionEtagService etags;
  @Autowired ResourceVersionScopeProvider scopes;
  @Autowired TenantContext tenant;
  @Autowired BulkReadCursorConfiguration cursors;

  private BulkOperationLifecycle lifecycle() { return lifecycles.getObject(); }
  private JdbcBulkProposalStore store() { return new JdbcBulkProposalStore(infrastructure); }
  private JdbcBulkDurableExecution executor() { return new JdbcBulkDurableExecution(infrastructure,lifecycle()); }
  private BulkOperationControlIdentity identity(String operation) { return new BulkOperationControlIdentity(infrastructure.namespace(),operation); }
  public static String operation(Resource r, BulkMode mode) { return "ergon."+r.name+".bulk."+(mode==BulkMode.UNIFORM_UPDATE ? "uniform" : "items"); }
  private void outside() {
    if (TransactionSynchronizationManager.isActualTransactionActive() || TransactionSynchronizationManager.isSynchronizationActive())
      throw new IllegalStateException("A operação em lote deve possuir suas próprias transações.");
  }
  private void authority(String name) {
    var auth=SecurityContextHolder.getContext().getAuthentication();
    if (auth==null || !auth.isAuthenticated() || auth.getAuthorities().stream().noneMatch(a -> name.equals(a.getAuthority())))
      throw new AccessDeniedException("Autoridade insuficiente para operação em lote.");
  }
  private String subject() {
    var auth=SecurityContextHolder.getContext().getAuthentication();
    if(auth==null || !auth.isAuthenticated()) throw new AccessDeniedException("Autenticação obrigatória.");
    return auth.getName()+"|orgao="+tenant.orgao();
  }
  private String fingerprint() {
    var authorities=SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().map(a->a.getAuthority()).sorted().toList();
    try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((REVISION+"\n"+subject()+"\n"+String.join("\n",authorities)).getBytes(StandardCharsets.UTF_8))); }
    catch(java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
  }
  private BulkFingerprintContext context(Resource resource, String operation) {
    return new BulkFingerprintContext(infrastructure.namespace(),subject(),resource.key,
        operations.requireResourceOperation(resource.key,operation,"POST"),REVISION,ActionCollectionAtomicity.PER_ITEM);
  }
  private <T> T transaction(int isolation, java.util.function.Supplier<T> work) {
    var template=new TransactionTemplate(transactionManager); template.setIsolationLevel(isolation);template.setTimeout(30);
    return template.execute(status -> work.get());
  }
  private BulkEditableFields editable(Resource resource) {
    return documents.withSchemaCacheReadLock(() -> {
      var binding=operations.requireResourceRequestBody(resource.key,resource.updateOperation,"PUT",json.getTypeFactory());
      var schema=CanonicalOpenApiGroupSnapshot.capture(documents,binding.operation().group()).requireRequestSchema(binding.operation());
      if (!binding.bodyType().hasRawClass(resource.update)) throw new IllegalStateException("Fonte PUT divergente.");
      return BulkEditableFields.compile(json,binding.bodyType(),schema.schema(),schema.specVersion(),resource.protectedFields());
    });
  }
  private void remaining(java.util.function.Supplier<Duration> budget) { if(budget.get().toMillis()<1) throw new IllegalStateException("Orçamento do lote esgotado."); }

  public BulkPublicationResult publication(BulkPublicationRequest request) {
    authority("ERGON_BULK_ADMIN");outside();domain.validate(request);
    var selected=InstallBulkStorage.identities().stream().filter(id->id.confirmationOperationId().equals(request.operationId()))
        .findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Operação não declarada pelo host."));
    if(request.intent()==BulkPublicationRequest.Intent.SUSPEND)
      return new BulkPublicationResult(request.operationId(),lifecycle().suspend(selected,request.expectedGeneration()),"SUSPENDED");
    var expectation=request.intent()==BulkPublicationRequest.Intent.PUBLISH ? lifecycle().publish(selected,request.expectedGeneration())
        : lifecycle().reconcilePublished(selected,request.expectedGeneration());
    return new BulkPublicationResult(request.operationId(),expectation.generation(),"READY");
  }

  public BulkProposal uniform(Resource resource, BulkUniformEvaluationRequest request) {
    return evaluate(resource,operation(resource,BulkMode.UNIFORM_UPDATE),request,null,null);
  }
  public BulkProposal items(Resource resource, BulkItemEvaluationRequest request) {
    return evaluate(resource,operation(resource,BulkMode.PER_ITEM_UPDATE),null,request,null);
  }
  public BulkProposal command(String action, BulkCommandEvaluationRequest request) {
    return evaluate(Resource.VINCULOS,"ergon.vinculos."+action,null,null,request);
  }
  public BulkProposal command(Resource resource, String action, BulkCommandEvaluationRequest request) {
    return evaluate(resource,"ergon."+resource.name+"."+action,null,null,request);
  }
  private BulkProposal evaluate(Resource resource,String operation,BulkUniformEvaluationRequest uniform,BulkItemEvaluationRequest items,BulkCommandEvaluationRequest command) {
    authority("ERGON_WRITE");outside();
    BulkMode mode=uniform!=null ? BulkMode.UNIFORM_UPDATE : items!=null ? BulkMode.PER_ITEM_UPDATE : BulkMode.DOMAIN_COMMAND;
    BulkExecutionMode execution=uniform!=null ? uniform.executionMode() : items!=null ? items.executionMode() : command.executionMode();
    BulkSelection selection=uniform!=null ? uniform.selection() : command!=null ? command.selection() : null;
    BulkSelectionMode selectionMode=selection==null ? BulkSelectionMode.EXPLICIT : selection.mode();
    int explicitCount=items!=null ? items.items().size() : selectionMode==BulkSelectionMode.EXPLICIT ? selection.targets().size() : -1;
    if(explicitCount==0 || explicitCount>200)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Seleção deve conter de 1 a 200 registros.");
    var admission=lifecycle().requireReady(identity(operation),execution,selectionMode);
    var context=context(resource,operation);var codec=domain.codec(resource);
    BulkIntentSnapshot intent=uniform!=null ? BulkIntentSnapshot.uniform(context,codec,uniform,v->json.valueToTree(v))
        : items!=null ? BulkIntentSnapshot.items(context,codec,items) : BulkIntentSnapshot.command(context,codec,command,v->json.valueToTree(v),v->json.valueToTree(v));
    BulkEditableFields fields=mode==BulkMode.DOMAIN_COMMAND ? null : editable(resource);
    Instant created=Instant.now();
    var proposal=new BulkStoredProposal(UUID.randomUUID(),created,created.plus(admission.profile().proposalLifetime()),intent,admission.expectation());
    long deadline=System.nanoTime()+Duration.ofSeconds(30).toNanos();
    var evidence=transaction(TransactionDefinition.ISOLATION_REPEATABLE_READ,()->store().captureAndInsertEvaluated(
        selectionMode==BulkSelectionMode.QUERY ? admission : null,proposal,(connection,budget)->{
          LOG.debug("Captura de avaliação iniciada: modalidade={}",mode);
          List<BulkTarget> targets=new ArrayList<>();
          if(items!=null) for(Object item:items.items()) { var change=(BulkItemChange)item;targets.add(new BulkTarget(change.id(),change.expectedVersion())); }
          else if(selectionMode==BulkSelectionMode.EXPLICIT) targets.addAll(selection.targets());
          else {
            var excluded=new HashSet<>(selection.excludedIds());
            for(Object id:domain.query(resource,selection.filter(),excluded.size())) {
              remaining(budget);if(excluded.contains(id)) continue;
              Object entity=domain.load(resource,id,false); if(entity==null) throw new IllegalStateException("Seleção alterada durante captura.");
              targets.add(new BulkTarget(id,etags.create(scopes.currentScope(),resource.key,domain.identity(resource,id),domain.version(entity))));
            }
          }
          if(targets.isEmpty() || targets.size()>admission.maxTargets()) throw new IllegalArgumentException("Seleção deve conter de 1 a 200 registros.");
          var all=new ArrayList<BulkTargetEvidence<?>>();
          for(var target:targets) {
            LOG.debug("Avaliando candidato ordinal={}",all.size());
            remaining(budget);
            List<BulkFieldChange> changes=items==null ? uniform==null ? List.of() : uniform.changes()
                : ((List<BulkItemChange>)items.items()).stream().filter(i->i.id().equals(target.id())).findFirst().orElseThrow().changes();
            ObjectNode plan=json.createObjectNode();
            if(command!=null) plan.set("parameters",json.valueToTree(command.parameters())); else plan.set("changes",json.valueToTree(changes));
            ObjectNode facts=json.createObjectNode().put("orgao",tenant.orgao());
            BulkTargetEligibility eligibility;String observed=target.expectedVersion();
            try {
              Object entity=domain.load(resource,target.id(),false);
              LOG.debug("Carga do candidato concluída: ordinal={}",all.size());
              if(entity==null) throw new IllegalArgumentException("Registro inexistente.");
              observed=etags.create(scopes.currentScope(),resource.key,domain.identity(resource,target.id()),domain.version(entity));
              if(!observed.equals(target.expectedVersion())) { plan.put("blockedReason","CONFLICT");throw new IllegalArgumentException("Versão defasada."); }
              if(command!=null) domain.commandCandidate(entity,operation,plan.path("parameters"));
              else domain.candidate(resource,entity,changes,fields,mode);
              LOG.debug("Validação do candidato concluída: ordinal={}",all.size());
              eligibility=BulkTargetEligibility.executable();
            } catch(SecurityException denied) { plan.put("blockedReason","DENIED");eligibility=blocked("DENIED"); }
            catch(BulkInputViolation invalid) { plan.put("blockedReason",invalid.code);eligibility=blocked(invalid.code); }
            catch(IllegalArgumentException | ResponseStatusException invalid) {
              if(!plan.has("blockedReason")) plan.put("blockedReason","INVALID");eligibility=blocked(plan.path("blockedReason").asText());
            }
            all.add(new BulkTargetEvidence(target,observed,facts,plan,eligibility));
          }
          LOG.debug("Captura de avaliação concluída: registros={}",all.size());
          return new BulkEvaluationSnapshot(proposal,Instant.now(),all,new BulkEvaluationGovernance(REVISION,fingerprint(),List.of()));
        },evaluation->new BulkPreviewProjection(evaluation,"1",publicDiagnostics()),()->Duration.ofNanos(Math.max(0,deadline-System.nanoTime()))));
    return proposal(resource,operation,evidence.proposal().id());
  }
  private List<BulkPreviewProjection.PublicDiagnostic> publicDiagnostics() {
    return List.of("INVALID","CONFLICT","DENIED","JORNADA_FORA_INTERVALO").stream().map(code->new BulkPreviewProjection.PublicDiagnostic(ResourceCommandErrorCategory.VALIDATION,code,diagnosticMessage(code))).toList();
  }
  private String diagnosticMessage(String code) {
    return switch(code) {
      case "JORNADA_FORA_INTERVALO" -> "Informe jornadaSemanal entre 1 e 168 horas e avalie novamente.";
      case "CONFLICT" -> "O registro mudou. Atualize a seleção e avalie novamente antes de confirmar.";
      case "DENIED" -> "Não foi possível autorizar este item. Revise o órgão selecionado e solicite acesso ao responsável.";
      default -> "Os valores não atendem às regras do cadastro. Revise os campos alterados e avalie novamente.";
    };
  }
  private BulkTargetEligibility blocked(String code) {
    return BulkTargetEligibility.blocked(List.of(new ResourceCommandMessage(ResourceCommandErrorCategory.VALIDATION,code,diagnosticMessage(code),null,Map.of())));
  }

  public BulkExecution confirm(Resource resource,String operation,BulkConfirmationRequest request,String key) {
    authority("ERGON_WRITE");outside();
    if(key==null || key.isBlank() || key.length()>128) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Idempotency-Key obrigatório, até 128 caracteres.");
    UUID proposalId=UUID.fromString(request.proposalId());
    var requestContext=context(resource,operation);
    BulkStoredProposal stored=transaction(TransactionDefinition.ISOLATION_READ_COMMITTED,()->store().find(requestContext,proposalId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND)));
    BulkEditableFields updateFields=stored.snapshot().mode()==BulkMode.DOMAIN_COMMAND ? null : editable(resource);
    var executor=executor();
    var reservation=executor.reserve(stored.snapshot().context(),proposalId,key,"ergon-http",stored.controlExpectation().structuralRevision(),Instant.now().plus(Duration.ofMinutes(5)));
    executor.advance(reservation,unit->{
      authority("ERGON_WRITE");remaining(unit::remainingBudget);
      var evidence=unit.targetEvidence(); var plan=evidence.plan();
      if(!evidence.eligibility().orElseThrow().isExecutable()) return switch(plan.path("blockedReason").asText()) {
        case "CONFLICT" -> BulkUnitAdmission.conflict(BulkUnitReasonCode.TARGET_VERSION_CONFLICT);
        case "DENIED" -> BulkUnitAdmission.denied(BulkUnitReasonCode.TARGET_DENIED);
        default -> BulkUnitAdmission.invalid(BulkUnitReasonCode.TARGET_INVALID);
      };
      try {
        Object entity=domain.load(resource,evidence.target().id(),true);
        if(entity==null) return BulkUnitAdmission.invalid(BulkUnitReasonCode.TARGET_NOT_FOUND);
        if(!etags.matches(evidence.target().expectedVersion(),scopes.currentScope(),resource.key,domain.identity(resource,evidence.target().id()),domain.version(entity)))
          return BulkUnitAdmission.conflict(BulkUnitReasonCode.TARGET_VERSION_CONFLICT);
        if(unit.originalIntent().mode()==BulkMode.DOMAIN_COMMAND) domain.commandCandidate(entity,operation,plan.path("parameters"));
        else domain.candidate(resource,entity,changes(plan),updateFields,unit.originalIntent().mode());
        remaining(unit::remainingBudget);return BulkUnitAdmission.admit();
      } catch(SecurityException denied) { return BulkUnitAdmission.denied(BulkUnitReasonCode.TARGET_DENIED); }
      catch(IllegalArgumentException | ResponseStatusException invalid) { return BulkUnitAdmission.invalid(BulkUnitReasonCode.TARGET_INVALID); }
    },unit->{
      remaining(unit::remainingBudget);Object entity=domain.load(resource,unit.targetEvidence().target().id(),true);
      boolean changed;
      if(unit.originalIntent().mode()==BulkMode.DOMAIN_COMMAND) changed=domain.applyCommand(entity,domain.commandCandidate(entity,operation,unit.targetEvidence().plan().path("parameters")));
      else changed=domain.apply(resource,entity,domain.candidate(resource,entity,changes(unit.targetEvidence().plan()),updateFields,unit.originalIntent().mode()));
      remaining(unit::remainingBudget);return changed ? BulkUnitMutationResult.confirmed() : BulkUnitMutationResult.unchanged();
    });
    return execution(resource,operation,reservation.executionId());
  }
  private List<BulkFieldChange> changes(JsonNode plan) {
    var result=new ArrayList<BulkFieldChange>();
    for(var value:plan.path("changes")) result.add(new BulkFieldChange(value.path("field").asText(),BulkChangeOperator.valueOf(value.path("operator").asText()),value.get("value")));
    return result;
  }

  private BulkReadAuthorizationProvider authorization(Resource resource,String operation) {
    return new BulkReadAuthorizationProvider() {
      public String confirmationOperationId() { return operation; }
      public BulkExecutionInfrastructure executionInfrastructure() { return infrastructure; }
      public GlobalDecision preAuthorize(Context c,Duration remaining) { try { authority("ERGON_READ");return c.requesterSubjectId().equals(subject()) ? GlobalDecision.ALLOWED : GlobalDecision.DENIED; } catch(AccessDeniedException denied) { return GlobalDecision.DENIED; } }
      public ScopeDecision authorize(Context context,String creator,List<Target> targets,Duration remaining) {
        if(!creator.equals(subject()) || remaining.toMillis()<1) return ScopeDecision.denied();
        if(targets.stream().anyMatch(t->t.facts().path("orgao").asInt()!=tenant.orgao())) return ScopeDecision.denied();
        String idExpression=resource==Resource.VINCULOS ? "numfunc::text || ':' || numero::text"
            : resource==Resource.DEPENDENTES ? "numfunc::text || ':' || numdep::text"
            : resource==Resource.PESSOAS ? "numero::text"
            : resource==Resource.CARGOS ? "cargo::text" : "id::text";
        String table=resource==Resource.CAMPOS_CUSTOMIZADOS ? "custom_field_definitions"
            : resource==Resource.PARAMETROS_CONTAGEM_FERIAS ? "parametros_contagem_ferias"
            : resource==Resource.PERIODOS_AQUISITIVOS_FERIAS ? "periodos_aquisitivos_ferias"
            : resource==Resource.FERIAS_GOZO ? "ferias_gozo"
            : resource.name;
        String placeholders=String.join(",",Collections.nCopies(targets.size(),"?"));
        boolean crossTenant=resource==Resource.PESSOAS || resource==Resource.DEPENDENTES;
        boolean nullableTenant=resource==Resource.CAMPOS_CUSTOMIZADOS || resource==Resource.PARAMETROS_CONTAGEM_FERIAS;
        String tenantClause=crossTenant ? "" : nullableTenant ? " and (emp_codigo is null or emp_codigo=?)" : " and emp_codigo=?";
        String sql="select count(*) from public."+table+" where ("+idExpression+") in ("+placeholders+")"+tenantClause;
        try {
          var holder=(org.springframework.jdbc.datasource.ConnectionHolder)TransactionSynchronizationManager.getResource(infrastructure.dataSource());
          if(holder==null) return ScopeDecision.unavailable();
          try(var statement=holder.getConnection().prepareStatement(sql)) {
            statement.setQueryTimeout(Math.max(1,(int)Math.min(2,remaining.toSeconds())));
            int index=1;for(var target:targets) statement.setString(index++,target.wireIdentity().toString());
            if(!crossTenant) statement.setInt(index,tenant.orgao());
            try(var rows=statement.executeQuery()) { if(!rows.next() || rows.getInt(1)!=targets.size()) return ScopeDecision.denied(); }
          }
          return ScopeDecision.authorized(HexFormat.of().parseHex(fingerprint()));
        } catch(SQLException failure) { return ScopeDecision.unavailable(); }
      }
    };
  }
  private BulkProposalProjectionProvider projection(Resource resource,String operation) {
    return new BulkProposalProjectionProvider() {
      public String resourceKey(){return resource.key;}
      public String confirmationOperationId(){return operation;}
      public String projectionRevision(){return "1";}
      public JsonNode projectRedactedIntent(BulkEvaluationSnapshot evaluation) {
        var result=json.createObjectNode().put("mode",evaluation.proposal().snapshot().mode().name()).put("targetCount",evaluation.targets().size());
        var fields=result.putArray("fields");var names=new TreeSet<String>();
        for(var target:evaluation.targets()) for(var change:target.plan().path("changes")) names.add(change.path("field").asText());
        names.forEach(fields::add); return result;
      }
    };
  }
  public BulkProposal proposal(Resource resource,String operation,UUID id) {
    authority("ERGON_READ");outside();
    var observation=new BulkAuthorizedProposalReader(infrastructure,resource.key,authorization(resource,operation),projection(resource,operation)).readProposal(subject(),id);
    readState(observation.state().name());return observation.proposal();
  }
  public CursorPage<BulkProposalItemResult<Object>> proposalResults(Resource resource,String operation,UUID id,int size,String after) {
    authority("ERGON_READ");outside();
    var observation=new BulkAuthorizedProposalResultsReader(infrastructure,resource.key,authorization(resource,operation),cursors).readProposalResults(subject(),id,size,after);
    readState(observation.state().name());return observation.page();
  }
  public BulkExecution execution(Resource resource,String operation,UUID id) {
    authority("ERGON_READ");outside();
    var observation=new BulkAuthorizedExecutionReader(infrastructure,resource.key,authorization(resource,operation)).readExecution(subject(),id);
    readState(observation.state().name());return observation.execution();
  }
  public CursorPage<BulkItemResult<Object>> results(Resource resource,String operation,UUID id,int size,String after) {
    authority("ERGON_READ");outside();
    var observation=new BulkAuthorizedExecutionResultsReader(infrastructure,resource.key,authorization(resource,operation),cursors).readExecutionResults(subject(),id,size,after);
    readState(observation.state().name());return observation.page();
  }
  public BulkExecution cancel(Resource resource,String operation,UUID id) {
    authority("ERGON_WRITE");outside();
    execution(resource,operation,id);executor().requestCancel(context(resource,operation),id);return execution(resource,operation,id);
  }
  public String locate(Resource resource,UUID id,boolean execution) {
    authority("ERGON_READ");outside();
    return transaction(TransactionDefinition.ISOLATION_READ_COMMITTED,()->infrastructure.withConnection(connection->{
      String sql="select operation_id from praxis_bulk."+(execution ? "praxis_bulk_execution where execution_id=?" : "praxis_bulk_proposal where proposal_id=?")+" and namespace_id=? and subject_id=? and resource_key=?";
      try(var statement=connection.prepareStatement(sql)) {
        statement.setObject(1,id);statement.setString(2,infrastructure.namespace());statement.setString(3,subject());statement.setString(4,resource.key);
        try(var rows=statement.executeQuery()) { if(!rows.next()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);String operation=rows.getString(1);operations.requireResourceOperation(resource.key,operation,"POST");return operation; }
      }
    }));
  }
  private void readState(String state) {
    if(state.equals("COMPLETE")) return;
    HttpStatus status=switch(state) { case "GLOBAL_DENIED" -> HttpStatus.FORBIDDEN;case "INVALID_CURSOR" -> HttpStatus.BAD_REQUEST;
      case "PRECONDITION_FAILED" -> HttpStatus.PRECONDITION_FAILED;case "GONE","TOMBSTONED" -> HttpStatus.GONE;
      case "NOT_FOUND_OR_DENIED" -> HttpStatus.NOT_FOUND;default -> HttpStatus.SERVICE_UNAVAILABLE; };
    throw new ResponseStatusException(status,"Consulta de lote indisponível para este contexto.");
  }
}
