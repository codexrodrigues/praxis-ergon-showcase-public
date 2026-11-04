package br.com.praxis.ergonshowcase;

import br.com.praxis.ergonshowcase.bulk.*;
import br.com.praxis.ergonshowcase.controller.CargoController;
import br.com.praxis.ergonshowcase.controller.DesignacaoFuncaoController;
import br.com.praxis.ergonshowcase.service.DesignacaoFuncaoService;
import br.com.praxis.ergonshowcase.service.CargoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.praxisplatform.uischema.bulk.*;
import org.praxisplatform.uischema.action.ActionCollectionAtomicity;
import org.praxisplatform.uischema.action.ActionDefinition;
import org.praxisplatform.uischema.action.ActionDefinitionRegistry;
import org.praxisplatform.uischema.action.ActionScope;
import org.praxisplatform.uischema.openapi.*;
import org.praxisplatform.uischema.rest.exceptionhandler.GlobalExceptionHandler;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/** Real MVC/converter/controller proof. The execution engine is mocked; this is not database acceptance. */
@SuppressWarnings({"rawtypes","unchecked"})
class ErgonBulkTwoPhaseProtocolHttpTest {
  static final String PATH="/api/ergon/cargos";
  static final String PROPOSAL="92568a6a-9a8e-4d4d-8faa-2796289c202a";
  static final String EXECUTION="17bec724-a936-4316-93f7-a55b42463d5c";
  final ObjectMapper json=new ObjectMapper().findAndRegisterModules();
  final JpaResourceBulkEngine engine=mock(JpaResourceBulkEngine.class);
  MockMvc mvc;
  @BeforeEach void setup() {
    var controller=new CargoController(mock(CargoService.class));
    var bulk=mock(ErgonBulkOperations.class);when(bulk.engine(ErgonBulkDomain.Resource.CARGOS)).thenReturn(engine);
    ReflectionTestUtils.setField(controller,"bulk",bulk);
    var operations=mock(CanonicalOperationResolver.class);
    Map<String,String> paths=Map.of("ergon.cargos.bulk.proposal",PATH+"/bulk/proposals/{proposalId}",
        "ergon.cargos.bulk.proposal-results",PATH+"/bulk/proposals/{proposalId}/results",
        "ergon.cargos.bulk.execution",PATH+"/bulk/executions/{executionId}",
        "ergon.cargos.bulk.results",PATH+"/bulk/executions/{executionId}/results");
    when(operations.resolveByOperationId(anyString())).thenAnswer(i->{String id=i.getArgument(0);return Optional.of(new CanonicalOperationRef("ergon",id,paths.get(id),"GET"));});
    ReflectionTestUtils.setField(controller,"bulkOperations",operations);
    mvc=standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler())
        .setMessageConverters(new PraxisBulkProtocolHttpMessageConverter(json,List.of()),new MappingJackson2HttpMessageConverter(json)).build();
  }
  BulkProposal proposal(){var now=Instant.parse("2026-11-01T00:00:00Z");return new BulkProposal(PROPOSAL,
      new CanonicalOperationRef("ergon","ergon.cargos.bulk.uniform",PATH+"/bulk/update","POST"),BulkMode.UNIFORM_UPDATE,BulkExecutionMode.SYNC,
      ActionCollectionAtomicity.PER_ITEM,BulkProposalStatus.READY,now,now.plusSeconds(600),new BulkProposalTotals(1,1,1,0),json.createObjectNode(),List.of(),List.of());}
  BulkExecution execution(){var now=Instant.parse("2026-11-01T00:00:00Z");return new BulkExecution(EXECUTION,PROPOSAL,
      new CanonicalOperationRef("ergon","ergon.cargos.bulk.uniform",PATH+"/bulk/update","POST"),BulkMode.UNIFORM_UPDATE,BulkExecutionMode.SYNC,
      ActionCollectionAtomicity.PER_ITEM,BulkExecutionStatus.COMPLETED,now,now,now,new BulkExecutionTotals(1,0,1,0,0,0,0,0,0),List.of());}
  @Test void evaluatesThenConfirmsUsingPublishedOperationIdsAndSeparateBodies() throws Exception {
    when(engine.evaluateUniform(eq("ergon.cargos.bulk.uniform"),any())).thenReturn(proposal());
    when(engine.confirm(eq("ergon.cargos.bulk.uniform"),any(),eq("confirm-1"))).thenReturn(execution());
    mvc.perform(post(PATH+"/bulk/update/evaluation").contentType(MediaType.APPLICATION_JSON).content("""
        {"executionMode":"SYNC","selection":{"mode":"EXPLICIT","targets":[{"id":7,"expectedVersion":"v0"}]},
         "changes":[{"field":"nome","operator":"SET","value":"Revisado"}]}
        """)).andExpect(status().isOk()).andExpect(jsonPath("$.data.proposalId").value(PROPOSAL));
    verify(engine,never()).confirm(anyString(),any(),anyString());
    mvc.perform(post(PATH+"/bulk/update").header("Idempotency-Key","confirm-1").contentType(MediaType.APPLICATION_JSON)
        .content("{\"proposalId\":\""+PROPOSAL+"\"}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.data.executionId").value(EXECUTION));
    verify(engine).confirm(eq("ergon.cargos.bulk.uniform"),eq(new BulkConfirmationRequest(PROPOSAL)),eq("confirm-1"));
  }
  @Test void rejectsWrongIdentityTypesDuplicateKeysAndNewIntentOnConfirmationBeforeEngine() throws Exception {
    for(String body:List.of(
        "{\"executionMode\":\"SYNC\",\"selection\":{\"mode\":\"EXPLICIT\",\"targets\":[{\"id\":\"7\",\"expectedVersion\":\"v0\"}]},\"changes\":[{\"field\":\"nome\",\"operator\":\"CLEAR\"}]}",
        "{\"executionMode\":\"SYNC\",\"executionMode\":\"SYNC\"}"))
      mvc.perform(post(PATH+"/bulk/update/evaluation").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
    mvc.perform(post(PATH+"/bulk/update").header("Idempotency-Key","key").contentType(MediaType.APPLICATION_JSON)
        .content("{\"proposalId\":\""+PROPOSAL+"\",\"changes\":[]}")) .andExpect(status().isBadRequest());
    mvc.perform(post(PATH+"/bulk/update").contentType(MediaType.APPLICATION_JSON).content("{\"proposalId\":\""+PROPOSAL+"\"}"))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(engine);
  }

  @Test void homologacaoDesignacoesUsesTheRetainedProposalAndIdempotencyKey() throws Exception {
    String path = "/api/ergon/designacoes";
    String action = "homologar-designacoes-lote";
    String operation = "ergon.designacoes." + action;
    var reference = new CanonicalOperationRef("ergon", operation, path + "/actions/" + action, "POST");
    var bulk = mock(ErgonBulkOperations.class);
    when(bulk.engine(ErgonBulkDomain.Resource.DESIGNACOES)).thenReturn(engine);
    var controller = new DesignacaoFuncaoController(mock(DesignacaoFuncaoService.class), bulk, null);
    var actions = mock(ActionDefinitionRegistry.class);
    var definition = mock(ActionDefinition.class);
    when(definition.id()).thenReturn(action);
    when(definition.scope()).thenReturn(ActionScope.COLLECTION);
    when(definition.operation()).thenReturn(reference);
    when(actions.findByResourceKey("administracao-pessoal.designacoes")).thenReturn(List.of(definition));
    ReflectionTestUtils.setField(controller, "bulkActions", actions);
    var resolver = mock(CanonicalOperationResolver.class);
    Map<String, String> paths = Map.of("proposal", "/bulk/proposals/{proposalId}",
        "proposal-results", "/bulk/proposals/{proposalId}/results", "execution", "/bulk/executions/{executionId}",
        "results", "/bulk/executions/{executionId}/results");
    paths.forEach((suffix, route) -> {
      String id = "ergon.designacoes.bulk." + suffix;
      when(resolver.resolveByOperationId(id)).thenReturn(Optional.of(new CanonicalOperationRef("ergon", id, path + route, "GET")));
    });
    ReflectionTestUtils.setField(controller, "bulkOperations", resolver);
    var endpoint = standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler())
        .setMessageConverters(new PraxisBulkProtocolHttpMessageConverter(json, List.of()),
            new MappingJackson2HttpMessageConverter(json)).build();
    var now = Instant.parse("2026-11-01T00:00:00Z");
    when(engine.evaluateDomainCommand(eq(operation), any())).thenReturn(new BulkProposal(PROPOSAL,
        reference, BulkMode.DOMAIN_COMMAND, BulkExecutionMode.SYNC, ActionCollectionAtomicity.PER_ITEM,
        BulkProposalStatus.READY, now, now.plusSeconds(600), new BulkProposalTotals(1, 1, 1, 0),
        json.createObjectNode(), List.of(), List.of()));
    when(engine.confirm(eq(operation), any(), eq("homologacao-1"))).thenReturn(new BulkExecution(EXECUTION, PROPOSAL,
        reference, BulkMode.DOMAIN_COMMAND, BulkExecutionMode.SYNC, ActionCollectionAtomicity.PER_ITEM,
        BulkExecutionStatus.COMPLETED, now, now, now, new BulkExecutionTotals(1, 0, 1, 0, 0, 0, 0, 0, 0), List.of()));

    endpoint.perform(post(path + "/bulk/" + action + "/evaluation").contentType(MediaType.APPLICATION_JSON).content("""
        {"executionMode":"SYNC","selection":{"mode":"EXPLICIT","targets":[{"id":"7","expectedVersion":"v0"}]},
         "parameters":{"dtExercicio":"2026-11-01"}}
        """)).andExpect(status().isOk()).andExpect(jsonPath("$.data.proposalId").value(PROPOSAL));
    verify(engine, never()).confirm(anyString(), any(), anyString());

    endpoint.perform(post(path + "/actions/" + action).contentType(MediaType.APPLICATION_JSON)
        .header("Idempotency-Key", "homologacao-1")
        .content("{\"proposalId\":\"" + PROPOSAL + "\"}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.data.executionId").value(EXECUTION));
    verify(engine).confirm(operation, new BulkConfirmationRequest(PROPOSAL), "homologacao-1");
  }
}
