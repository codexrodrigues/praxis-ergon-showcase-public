package br.com.praxis.ergonshowcase.bulk;

import br.com.praxis.ergonshowcase.bulk.ErgonBulkDomain.Resource;
import br.com.praxis.ergonshowcase.config.TenantContext;
import br.com.praxis.ergonshowcase.domain.*;
import br.com.praxis.ergonshowcase.operations.InstallBulkStorage;
import br.com.praxis.ergonshowcase.rules.ErgonRuleErrorCatalog;
import br.com.praxis.ergonshowcase.rules.ErgonRuleViolationException;
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
  @Autowired jakarta.validation.Validator validator;
  @Autowired BulkOperationIdentityDiscovery identities;

  private BulkOperationLifecycle lifecycle() { return lifecycles.getObject(); }

  private JdbcBulkProposalStore store() { return new JdbcBulkProposalStore(infrastructure); }

  private BulkOperationControlIdentity identity(String operation) { return new BulkOperationControlIdentity(infrastructure.namespace(),operation); }

  public static String operation(Resource r, BulkMode mode) {
    if (r == Resource.DESIGNACOES) {
      return r.key + (mode == BulkMode.UNIFORM_UPDATE ? ".bulk.update" : ".bulk.update-items");
    }
    return "ergon."+r.name+".bulk."+(mode==BulkMode.UNIFORM_UPDATE ? "uniform" : "items");
  }

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

  public BulkPublicationResult publication(BulkPublicationRequest request) {
    authority("ERGON_BULK_ADMIN");outside();domain.validate(request);
    var selected=identities.discover(infrastructure.namespace()).stream().filter(id->id.confirmationOperationId().equals(request.operationId()))
        .findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Operação não declarada pelo host."));
    if(request.intent()==BulkPublicationRequest.Intent.SUSPEND)
      return new BulkPublicationResult(request.operationId(),lifecycle().suspend(selected,request.expectedGeneration()),"SUSPENDED");
    var expectation=request.intent()==BulkPublicationRequest.Intent.PUBLISH ? lifecycle().publish(selected,request.expectedGeneration())
        : lifecycle().reconcilePublished(selected,request.expectedGeneration());
    return new BulkPublicationResult(request.operationId(),expectation.generation(),"READY");
  }

  private String diagnosticMessage(String code) {
    return switch(code) {
      case "JORNADA_FORA_INTERVALO" -> "Informe jornadaSemanal entre 1 e 168 horas e avalie novamente.";
      case "CONFLICT", "BULK_VERSION_CONFLICT" -> "O registro mudou. Atualize a seleção e avalie novamente antes de confirmar.";
      case "DENIED", "BULK_TARGET_UNAVAILABLE" -> "Não foi possível autorizar este item. Revise o órgão selecionado e solicite acesso ao responsável.";
      default -> ErgonRuleErrorCatalog.find(code)
          .map(ErgonRuleErrorCatalog.RuleErrorDescriptor::messagePtBr)
          .orElse("Os valores não atendem às regras do cadastro. Revise os campos alterados e avalie novamente.");
    };
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
        String table=resource.table;
        String placeholders=String.join(",",Collections.nCopies(targets.size(),"?"));
        boolean crossTenant=resource==Resource.PESSOAS;
        boolean nullableTenant=resource==Resource.CAMPOS_CUSTOMIZADOS || resource==Resource.PARAMETROS_CONTAGEM_FERIAS;
        String tenantClause=crossTenant ? "" : resource==Resource.DEPENDENTES
            ? " and exists (select 1 from public.vinculos v where v.numfunc=t.numfunc and v.emp_codigo=? and v.situacao in ('ATIVO','AFASTADO','CEDIDO'))"
            : nullableTenant ? " and (emp_codigo is null or emp_codigo=?)" : " and emp_codigo=?";
        String sql="select count(*) from public."+table+" t where ("+idExpression+") in ("+placeholders+")"+tenantClause;
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

  /** Composition only: capture, durable execution and authorized reads belong to the starter. */
  public JpaResourceBulkEngine engine(Resource resource) {
    BulkDomainResourceAdapter adapter = domain.service(resource).bulkDomainAdapter(json, validator, etags, scopes::currentScope);
    BulkDomainResourceAdapter governed = new BulkDomainResourceAdapter() {
      public Object readFilter(JsonNode node) { Object filter = adapter.readFilter(node); domain.validate(filter); return filter; }
      public List selectBulkIdentities(Object filter, Collection included, Collection excluded, int max) {
        return adapter.selectBulkIdentities(filter, included, excluded, max);
      }
      public String currentVersion(Object id) { return adapter.currentVersion(id); }
      public Candidate evaluate(Object id, String expected, List changes, BulkMode mode, BulkEditableFields fields) {
        try {
          var candidate = adapter.evaluate(id, expected, changes, mode, fields);
          return new Candidate(candidate.observedVersion(), json.createObjectNode().put("orgao",tenant.orgao())
              .set("resource",candidate.facts()), candidate.plan());
        } catch (org.praxisplatform.uischema.customfields.CustomFieldValidationException rejection) {
          if (br.com.praxis.ergonshowcase.rules.ErgonCustomFieldErrors.isJsonLogic(rejection))
            throw br.com.praxis.ergonshowcase.rules.ErgonCustomFieldErrors.violation(rejection);
          throw rejection;
        } catch (jakarta.validation.ConstraintViolationException invalid) {
          if (invalid.getConstraintViolations().stream().anyMatch(v -> v.getPropertyPath().toString().equals("jornadaSemanal")))
            throw new org.praxisplatform.rules.violation.PraxisRuleViolationException(new org.praxisplatform.rules.violation.SlotViolationDetail(
                "JORNADA_FORA_INTERVALO",null,null,null,null,org.praxisplatform.rules.contract.RuleDecision.DENY,"jornadaSemanal",
                diagnosticMessage("JORNADA_FORA_INTERVALO"),null));
          throw invalid;
        }
      }
      public void update(Object id, String expected, JsonNode plan) { adapter.update(id, expected, plan); }
    };
    BulkResourcePolicy policy = new BulkResourcePolicy() {
      public BulkFingerprintContext context(String operation) { return ErgonBulkOperations.this.context(resource, operation); }
      public void authorize(BulkFingerprintContext context) { authority("ERGON_WRITE"); }
      public BulkEvaluationGovernance evaluateGovernance(BulkFingerprintContext context) {
        return new BulkEvaluationGovernance(REVISION, fingerprint(), List.of());
      }
      public BulkUnitAdmission admit(BulkExecutionUnit unit) {
        try { authority("ERGON_WRITE"); return BulkUnitAdmission.admit(); }
        catch (AccessDeniedException denied) { return BulkUnitAdmission.denied(BulkUnitReasonCode.TARGET_DENIED); }
      }
      public BulkReadAuthorizationProvider readAuthorization(String operation) { return authorization(resource, operation); }
      public BulkProposalProjectionProvider proposalProjection(String operation) { return projection(resource, operation); }
      public List<BulkPreviewProjection.PublicDiagnostic> publicDiagnostics(String operation) {
        var codes = new LinkedHashSet<String>(List.of("JORNADA_FORA_INTERVALO"));
        ErgonRuleErrorCatalog.all().values().forEach(error -> codes.add(error.code()));
        var diagnostics = new ArrayList<BulkPreviewProjection.PublicDiagnostic>();
        codes.forEach(code -> diagnostics.add(new BulkPreviewProjection.PublicDiagnostic(ResourceCommandErrorCategory.VALIDATION,code,diagnosticMessage(code))));
        diagnostics.add(new BulkPreviewProjection.PublicDiagnostic(ResourceCommandErrorCategory.PRECONDITION,"BULK_VERSION_CONFLICT",diagnosticMessage("CONFLICT")));
        diagnostics.add(new BulkPreviewProjection.PublicDiagnostic(ResourceCommandErrorCategory.PERMISSION,"BULK_TARGET_UNAVAILABLE",diagnosticMessage("DENIED")));
        diagnostics.add(new BulkPreviewProjection.PublicDiagnostic(ResourceCommandErrorCategory.VALIDATION,"BULK_TARGET_INVALID",diagnosticMessage("INVALID")));
        return diagnostics;
      }
    };
    Map<String, BulkDomainCommandAdapter> commands = new LinkedHashMap<>();
    for (var identity : identities.discover(infrastructure.namespace())) {
      if (!identity.confirmationOperationId().startsWith("ergon." + resource.name + ".")
          || identity.confirmationOperationId().equals(operation(resource, BulkMode.UNIFORM_UPDATE))
          || identity.confirmationOperationId().equals(operation(resource, BulkMode.PER_ITEM_UPDATE))) continue;
      String operation = identity.confirmationOperationId();
      commands.put(operation, new BulkDomainCommandAdapter() {
        public BulkDomainResourceAdapter.Candidate evaluate(Object id, String expected, JsonNode parameters) {
          Object entity = domain.load(resource, domain.codec(resource).encode(id), false);
          if (entity == null) throw new IllegalArgumentException("Registro inexistente.");
          String observed = etags.create(scopes.currentScope(),resource.key,id,domain.version(entity));
          if (!observed.equals(expected)) throw ResourceVersionPreconditionException.stale();
          domain.commandCandidate(entity,operation,parameters);
          return new BulkDomainResourceAdapter.Candidate(observed, json.createObjectNode().put("orgao",tenant.orgao()),
              json.createObjectNode().set("parameters",parameters.deepCopy()));
        }
        public void execute(Object id, String expected, JsonNode plan) {
          var precondition = new ResourceVersionUpdatePrecondition(etags,expected,scopes.currentScope(),resource.key,id);
          domain.service(resource).executeBulkDomainCommand(id,precondition,entity ->
              domain.applyCommand(entity,domain.commandCandidate(entity,operation,plan.path("parameters"))));
        }
      });
    }
    return new JpaResourceBulkEngine(infrastructure,lifecycle(),governed,domain.codec(resource),editable(resource),json,policy,cursors,commands);
  }
}
