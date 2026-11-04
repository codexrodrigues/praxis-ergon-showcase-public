package br.com.praxis.ergonshowcase;

import br.com.praxis.ergonshowcase.controller.*;
import br.com.praxis.ergonshowcase.bulk.ErgonBulkDomain.Resource;
import br.com.praxis.ergonshowcase.bulk.ErgonBulkOperations;
import br.com.praxis.ergonshowcase.operations.InstallBulkStorage;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.praxisplatform.uischema.bulk.*;
import org.praxisplatform.uischema.controller.base.AbstractBulkGovernedResourceController;
import org.praxisplatform.uischema.extension.CustomOpenApiResolver;
import org.praxisplatform.uischema.openapi.*;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** All resource MVC declarations use the canonical compiler and schema synthesis without database I/O. */
class ShowcaseCanonicalConformanceGateTest {
  @Test void chefiaBulkSchemasPublishOnlyReviewedRetifications() {
    var host = new TempoLicencaBulkAnalyticsExportTest();
    var controllers = Map.of(Resource.FUNCOES, FuncaoConfiancaController.class,
        Resource.VAGAS_FUNCAO, VagaFuncaoController.class,
        Resource.SUBSTITUICOES, SubstituicaoFuncaoController.class);
    controllers.forEach((resource, controller) -> {
      var fields = host.fields(resource.update, controller);
      assertThat(controller.getAnnotation(BulkResourceOperations.class).protectedUpdateFields())
          .containsExactlyInAnyOrderElementsOf(resource.protectedFields());
      var uniform = resource == Resource.FUNCOES
          ? Set.of("categoria", "simbolo", "requisitos", "documentoLegalId", "observacoes")
          : Set.of("documentoLegalId", "observacoes");
      var perItem = new HashSet<>(uniform);
      perItem.add("descricao");
      assertThat(fields.writableFields(BulkMode.UNIFORM_UPDATE)).containsExactlyInAnyOrderElementsOf(uniform);
      assertThat(fields.writableFields(BulkMode.PER_ITEM_UPDATE)).containsExactlyInAnyOrderElementsOf(perItem);
      assertThat(InstallBulkStorage.identities()).extracting(BulkOperationControlIdentity::confirmationOperationId)
          .contains(ErgonBulkOperations.operation(resource, BulkMode.UNIFORM_UPDATE),
              ErgonBulkOperations.operation(resource, BulkMode.PER_ITEM_UPDATE));
    });
  }

  static final Map<Class<?>,String> RESOURCES=Map.ofEntries(
      Map.entry(DesignacaoFuncaoController.class,"designacoes"),
      Map.entry(FuncaoConfiancaController.class,"funcoes"),
      Map.entry(VagaFuncaoController.class,"vagas-funcao"),
      Map.entry(SubstituicaoFuncaoController.class,"substituicoes"),
      Map.entry(CargoController.class,"cargos"),Map.entry(ServidorPessoaController.class,"funcionarios"),
      Map.entry(VinculoController.class,"vinculos"),Map.entry(DependenteController.class,"dependentes"),
      Map.entry(AfastamentoController.class,"afastamentos"),Map.entry(FrequenciaController.class,"frequencias"),
      Map.entry(DefinicaoCampoCustomizadoController.class,"campos-customizados"),Map.entry(ParametroContagemFeriasController.class,"parametros-contagem-ferias"),
      Map.entry(PeriodoAquisitivoFeriasController.class,"periodos-aquisitivos-ferias"),Map.entry(FeriasGozoController.class,"ferias-gozo"),
      Map.entry(AverbacaoTempoServicoController.class,"averbacoes-tempo-servico"),Map.entry(PeriodoAquisitivoLicencaEspecialController.class,"periodos-aquisitivos-licenca-especial"),
      Map.entry(LicencaEspecialGozoController.class,"licencas-especiais-gozo"),Map.entry(VendaLicencaEspecialController.class,"vendas-licenca-especial"));

  @Test void protectedBulkFieldsExistInThePutBody() {
    var json = new ObjectMapper().findAndRegisterModules();
    var models = new io.swagger.v3.core.converter.ModelConverters();
    models.addConverter(new CustomOpenApiResolver(json));
    org.assertj.core.api.SoftAssertions.assertSoftly(checks -> RESOURCES.forEach((controller, name) -> {
      var resource = Arrays.stream(Resource.values())
          .filter(candidate -> candidate.name.equals(name))
          .findFirst().orElseThrow();
      var declared = controller.getAnnotation(BulkResourceOperations.class).protectedUpdateFields();
      var inputFields = json.getDeserializationConfig().introspect(json.constructType(resource.update))
          .findProperties().stream()
          .filter(com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition::couldDeserialize)
          .map(com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition::getName)
          .toList();
      var schema = models.resolveAsResolvedSchema(
          new io.swagger.v3.core.converter.AnnotatedType(resource.update).resolveAsRef(false)).schema;
      checks.assertThat(declared).as(name).containsExactlyInAnyOrderElementsOf(resource.protectedFields());
      checks.assertThat(inputFields).as(name + " PUT JSON").containsAll(Arrays.asList(declared));
      checks.assertThat(schema.getProperties().keySet()).as(name + " PUT OpenAPI").containsAll(Arrays.asList(declared));
      checks.assertThatCode(() -> BulkEditableFields.compile(
          json, json.constructType(resource.update), io.swagger.v3.core.util.Json.mapper().valueToTree(schema),
          io.swagger.v3.oas.models.SpecVersion.V30, Set.of(declared)))
          .as(name + " editable fields").doesNotThrowAnyException();
    }));
  }

  @Test void everyMutableBulkResourceRetainsIdsAndPublishesTypedSchemasAndReadOnlySetorStaysReadOnly() throws Exception {
    var json=new ObjectMapper().findAndRegisterModules();
    try(var context=new GenericWebApplicationContext()) {
      RESOURCES.forEach((type,name)->context.getBeanFactory().registerSingleton(name,mock(type,CALLS_REAL_METHODS)));
      context.refresh();
      var mvc=new RequestMappingHandlerMapping();mvc.setApplicationContext(context);mvc.afterPropertiesSet();
      var bindings=BulkResourceOperationBindings.from(mvc);
      assertThat(bindings.diagnostics()).isEmpty();
      var bulkBindings=mvc.getHandlerMethods().values().stream().map(bindings::bulkOperationFor)
          .flatMap(Optional::stream).distinct().toList();
      assertThat(bulkBindings).hasSize(50);
      var documents=mock(OpenApiDocumentService.class);when(documents.resolveGroupFromPath(anyString())).thenReturn("ergon");
      var resolver=new OpenApiCanonicalOperationResolver(documents,mvc,bindings,List.of("ergon"));
      var identities=new BulkOperationIdentityDiscovery(bindings,resolver).discover("ergon-showcase");
      for(var resource:RESOURCES.entrySet()) {
        assertThat(AbstractBulkGovernedResourceController.class).isAssignableFrom(resource.getKey());
        for(String mode:List.of("uniform","items")) {
          String operation = resource.getKey() == DesignacaoFuncaoController.class
              ? "administracao-pessoal.designacoes.bulk." + (mode.equals("uniform") ? "update" : "update-items")
              : "ergon." + resource.getValue() + ".bulk." + mode;
          assertThat(identities).anySatisfy(id->assertThat(id.confirmationOperationId()).isEqualTo(operation));
          var binding=bulkBindings.stream().filter(value->value.confirmationOperationId().equals(operation)).findFirst().orElseThrow();
          var mapping=mvc.getHandlerMethods().entrySet().stream().filter(entry->entry.getValue().equals(binding.confirmationHandler())).map(Map.Entry::getKey).findFirst().orElseThrow();
          assertThat(resolver.resolve(binding.confirmationHandler(),mapping).path()).isEqualTo("/api/ergon/"+(resource.getValue().equals("funcionarios") ? "servidor-pessoas" : resource.getValue())+
              (mode.equals("uniform")?"/bulk/update":"/bulk/update-items"));
        }
      }
      var document=new OpenAPI().paths(new Paths());
      for(var binding:bulkBindings) {
        var mapping=mvc.getHandlerMethods().entrySet().stream().filter(entry->entry.getValue().equals(binding.evaluationHandler())).map(Map.Entry::getKey).findFirst().orElseThrow();
        var ref=resolver.resolve(binding.evaluationHandler(),mapping);
        document.getPaths().addPathItem(ref.path(),new PathItem().post(new Operation().operationId(ref.operationId())));
      }
      new CustomOpenApiResolver(json).synthesizeBulkEvaluationSchemas(document,bindings);
      var schemas=document.getComponents().getSchemas();
      assertThat(json.valueToTree(schemas.get("CargoBulkUniformEvaluationRequest")).at("/properties/selection/properties/targets/items/properties/id/type").asText()).isEqualTo("integer");
      assertThat(json.valueToTree(schemas.get("FeriasGozoBulkUniformEvaluationRequest")).at("/properties/selection/properties/targets/items/properties/id/format").asText()).isEqualTo("int64-decimal");
      assertThat(json.valueToTree(schemas.get("VinculoBulkUniformEvaluationRequest")).at("/properties/selection/properties/targets/items/properties/id/type").asText()).isEqualTo("string");
      verify(documents,never()).getDocumentForGroupStrict(anyString());
      assertThat(AbstractBulkGovernedResourceController.class.isAssignableFrom(SetorController.class)).isFalse();
      assertThat(Arrays.stream(SetorController.class.getMethods()).map(java.lang.reflect.Method::getName)).doesNotContain("create","update","deleteById");
      assertThat(Arrays.stream(CargoController.class.getMethods()).map(java.lang.reflect.Method::getName)).contains("duplicateDraft");
    }
  }

  @Test void everyGovernedResourcePublishesOntologicalRuleSetInSemanticDomainCatalog() {
    var json = new ObjectMapper().findAndRegisterModules();
    var models = new io.swagger.v3.core.converter.ModelConverters();
    models.addConverter(new CustomOpenApiResolver(json));
    var ruleEngine = new br.com.praxis.ergonshowcase.rules.ErgonRuleEngineFacade();
    var ruleSetCatalogProvider = new br.com.praxis.ergonshowcase.policystudio.ErgonDomainRuleSetCatalogProvider(ruleEngine, json);

    var governedControllers = new ArrayList<Class<?>>(RESOURCES.keySet());
    governedControllers.remove(DefinicaoCampoCustomizadoController.class);
    governedControllers.add(RegraFrequenciaEstatutariaController.class);
    assertThat(governedControllers).hasSize(18);

    org.assertj.core.api.SoftAssertions.assertSoftly(checks -> {
      for (Class<?> controller : governedControllers) {
        var apiResource = controller.getAnnotation(org.praxisplatform.uischema.annotation.ApiResource.class);
        checks.assertThat(apiResource).as(controller.getSimpleName() + " @ApiResource").isNotNull();
        String resourceKey = apiResource.resourceKey();
        String resourcePath = apiResource.value()[0];
        var descriptors = ruleSetCatalogProvider.findRuleSets(resourceKey, "1");
        checks.assertThat(descriptors).as(resourceKey + " DomainRuleSetDescriptors").isNotEmpty();

        var responseType = (Class<?>) ((java.lang.reflect.ParameterizedType) controller.getGenericSuperclass())
            .getActualTypeArguments()[0];
        var schema = models.resolveAsResolvedSchema(
            new io.swagger.v3.core.converter.AnnotatedType(responseType).resolveAsRef(false)).schema;
        var openApiDoc = json.createObjectNode();
        var schemasNode = openApiDoc.putObject("components").putObject("schemas");
        schemasNode.set("ResourceResponse", io.swagger.v3.core.util.Json.mapper().valueToTree(schema));

        var actions = new org.praxisplatform.uischema.action.ActionDefinitionRegistry() {
          @Override public List<org.praxisplatform.uischema.action.ActionDefinition> findByResourceKey(String key) { return List.of(); }
          @Override public List<org.praxisplatform.uischema.action.ActionDefinition> findByGroup(String group) { return List.of(); }
        };
        var surfaces = new org.praxisplatform.uischema.surface.SurfaceDefinitionRegistry() {
          @Override
          public List<org.praxisplatform.uischema.surface.SurfaceDefinition> findByResourceKey(String key) {
            return List.of(new org.praxisplatform.uischema.surface.SurfaceDefinition(
                "detail",
                key,
                resourcePath,
                "ergon",
                org.praxisplatform.uischema.surface.SurfaceKind.READ_PROJECTION,
                org.praxisplatform.uischema.surface.SurfaceScope.ITEM,
                apiResource.title(),
                apiResource.description(),
                "detail",
                "response",
                org.praxisplatform.uischema.surface.SurfaceResponseCardinality.VOID,
                new org.praxisplatform.uischema.openapi.CanonicalOperationRef("ergon", "getById", resourcePath + "/{id}", "GET"),
                new org.praxisplatform.uischema.schema.CanonicalSchemaRef("ResourceResponse", "response", "/schemas/filtered?schemaType=response"),
                10,
                List.of(),
                List.of(),
                List.of()));
          }
          @Override
          public List<org.praxisplatform.uischema.surface.SurfaceDefinition> findByGroup(String group) {
            return findByResourceKey(resourceKey);
          }
        };
        var openApiDocs = mock(OpenApiDocumentService.class);
        when(openApiDocs.resolveGroupFromPath(anyString())).thenReturn("ergon");
        when(openApiDocs.getDocumentForGroup(anyString())).thenReturn(openApiDoc);
        var catalogService = new org.praxisplatform.uischema.domain.SemanticDomainCatalogService(
            actions,
            surfaces,
            org.praxisplatform.uischema.options.OptionSourceRegistry.empty(),
            openApiDocs,
            null,
            java.time.Clock.fixed(java.time.Instant.parse("2026-04-21T10:30:00Z"), java.time.ZoneOffset.UTC),
            "praxis-ergon-showcase",
            "Praxis Ergon Showcase",
            "test",
            null,
            null,
            () -> Optional.of("1"),
            ruleSetCatalogProvider);

        var domainCatalog = catalogService.findByResourceKey(resourceKey);
        var conceptNode = domainCatalog.nodes().stream()
            .filter(node -> "concept".equals(node.nodeType()))
            .findFirst()
            .orElse(null);
        checks.assertThat(conceptNode).as(resourceKey + " concept node").isNotNull();
        if (conceptNode != null) {
          @SuppressWarnings("unchecked")
          var ontologicalRoots = (List<String>) conceptNode.metadata().get("ontologicalFactRoots");
          checks.assertThat(ontologicalRoots)
              .as(resourceKey + " ontologicalFactRoots")
              .isNotEmpty()
              .contains("draft", "current", "context")
              .doesNotContain("facts");
        }

        var ruleSetNodes = domainCatalog.nodes().stream()
            .filter(node -> "policy_hint".equals(node.nodeType())
                && "domain_rule_set".equals(node.metadata().get("policyKind")))
            .toList();
        checks.assertThat(ruleSetNodes).as(resourceKey + " policy_hint domain_rule_set nodes").isNotEmpty();
        for (var ruleSetNode : ruleSetNodes) {
          checks.assertThat(domainCatalog.edges())
              .as(resourceKey + " governed_by edge")
              .anyMatch(edge -> "governed_by".equals(edge.edgeType())
                  && resourceKey.equals(edge.sourceNodeKey())
                  && ruleSetNode.nodeKey().equals(edge.targetNodeKey()));
          checks.assertThat(domainCatalog.bindings())
              .as(resourceKey + " rule_definition binding")
              .anyMatch(binding -> "rule_definition".equals(binding.bindingType())
                  && ruleSetNode.nodeKey().equals(binding.nodeKey()));
        }
      }
    });
  }
}
