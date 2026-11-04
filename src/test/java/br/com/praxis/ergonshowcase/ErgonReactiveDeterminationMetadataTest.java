package br.com.praxis.ergonshowcase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import br.com.praxis.ergonshowcase.config.ErgonDeterminationConfig;
import br.com.praxis.ergonshowcase.config.TempoLicencaDeterminationConfig;
import br.com.praxis.ergonshowcase.controller.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.annotations.Operation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.praxisplatform.uischema.annotation.ApiResource;
import org.praxisplatform.uischema.determination.*;
import org.praxisplatform.uischema.extension.CustomOpenApiResolver;
import org.praxisplatform.uischema.openapi.*;
import org.praxisplatform.uischema.schema.FilteredSchemaReferenceResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Compila a projeção de /schemas/filtered com os controllers, DTOs e providers reais das Slices 1–7. */
class ErgonReactiveDeterminationMetadataTest {
  static final List<Class<?>> CONTROLLERS = List.of(ServidorPessoaController.class, VinculoController.class,
      CargoController.class, DependenteController.class, AfastamentoController.class, FrequenciaController.class,
      DefinicaoCampoCustomizadoController.class, ParametroContagemFeriasController.class,
      PeriodoAquisitivoFeriasController.class, FeriasGozoController.class, AverbacaoTempoServicoController.class,
      PeriodoAquisitivoLicencaEspecialController.class, LicencaEspecialGozoController.class, VendaLicencaEspecialController.class,
      FuncaoConfiancaController.class, VagaFuncaoController.class, DesignacaoFuncaoController.class, SubstituicaoFuncaoController.class);

  @Test void allDeterminationsCompileForBothCreateAndEditAgainstRealOperationSchemas() throws Exception {
    var definitions = new ArrayList<ReactiveDeterminationDefinition>();
    definitions.addAll(new ErgonDeterminationConfig().ergonDeterminations().definitions());
    definitions.addAll(new TempoLicencaDeterminationConfig().tempoLicencaDeterminations().definitions());
    assertThat(definitions).hasSize(18);
    var expected = new HashSet<String>();
    definitions.forEach(definition -> {
      expected.add(definition.operationId());
      assertThat(definition.scopes()).extracting(ReactiveDeterminationScope::formMode)
          .containsExactlyInAnyOrder(ReactiveDeterminationFormMode.CREATE, ReactiveDeterminationFormMode.EDIT);
      definition.scopes().forEach(scope -> expected.add(scope.schemaOperationId()));
    });
    var json = new ObjectMapper().findAndRegisterModules();
    var converters = new ModelConverters();
    converters.addConverter(new CustomOpenApiResolver(json));
    var document = json.createObjectNode();
    document.put("openapi", "3.0.3");
    var paths = document.putObject("paths");
    var schemas = document.putObject("components").putObject("schemas");
    var refs = new LinkedHashMap<String, CanonicalOperationRef>();
    var requests = new HashMap<String, JsonNode>();
    for (Class<?> controller : CONTROLLERS) {
      String base = org.springframework.core.annotation.AnnotatedElementUtils
          .findMergedAnnotation(controller, RequestMapping.class).path()[0];
      for (var method : controller.getDeclaredMethods()) {
        if (method.isBridge() || method.isSynthetic()) continue;
        var operation = method.getAnnotation(Operation.class);
        if (operation == null || !expected.contains(operation.operationId())) continue;
        var post = method.getAnnotation(PostMapping.class);
        var put = method.getAnnotation(PutMapping.class);
        assertThat(post != null || put != null).as("%s mapping", operation.operationId()).isTrue();
        String verb = post != null ? "POST" : "PUT";
        String[] mapping = post != null ? post.value() : put.value();
        String[] namedPath = post != null ? post.path() : put.path();
        String path = base + (mapping.length > 0 ? mapping[0] : namedPath.length > 0 ? namedPath[0] : "");
        var node = ((ObjectNode) paths.withObjectProperty(path)).putObject(verb.toLowerCase(Locale.ROOT));
        node.put("operationId", operation.operationId());
        assertThat(refs.put(operation.operationId(), new CanonicalOperationRef("ergon", operation.operationId(), path, verb))).isNull();
        var body = Arrays.stream(method.getParameters()).filter(parameter -> parameter.isAnnotationPresent(RequestBody.class))
            .findFirst().orElseThrow();
        JsonNode requestSchema = schema(converters, body.getParameterizedType(), schemas, json);
        requests.put(operation.operationId(), requestSchema);
        node.putObject("requestBody").putObject("content").putObject("application/json").set("schema", requestSchema);
        Type response = method.getGenericReturnType();
        if (response instanceof ParameterizedType generic && generic.getRawType() == ResponseEntity.class)
          response = generic.getActualTypeArguments()[0];
        node.putObject("responses").putObject("200").putObject("content").putObject("application/json")
            .set("schema", schema(converters, response, schemas, json));
      }
    }
    assertThat(refs.keySet()).containsExactlyInAnyOrderElementsOf(expected);
    java.nio.file.Files.writeString(java.nio.file.Path.of("target/p5-b-determination-openapi.json"), document.toPrettyString());
    var operations = mock(CanonicalOperationResolver.class);
    when(operations.resolveByOperationId(anyString())).thenAnswer(call -> Optional.ofNullable(refs.get(call.getArgument(0))));
    var documents = mock(OpenApiDocumentService.class, CALLS_REAL_METHODS);
    when(documents.getDocumentForGroup("ergon")).thenReturn(document);
    var registry = new DefaultReactiveDeterminationDefinitionRegistry(List.of(() -> definitions));
    var compiler = new ReactiveDeterminationMetadataCompiler(registry, operations, new FilteredSchemaReferenceResolver(), documents);
    int compiled = 0;
    for (var definition : definitions) {
      for (var scope : definition.scopes()) {
        String operation = scope.schemaOperationId();
        var metadata = compiler.compile(refs.get(operation), "request", document, requests.get(operation));
        assertThat(metadata).as("/schemas/filtered?operationId=%s&schemaType=request", operation).hasSize(1);
        assertThat(metadata.getFirst()).containsEntry("id", definition.id());
        assertThat(compiler.compile(refs.get(operation), "response", document, requests.get(operation))).isEmpty();
        compiled++;
      }
    }
    assertThat(compiled).isEqualTo(36);
  }

  private static JsonNode schema(ModelConverters converters, Type type, ObjectNode schemas, ObjectMapper json) {
    var resolved = converters.resolveAsResolvedSchema(new AnnotatedType(type).resolveAsRef(false));
    resolved.referencedSchemas.forEach((name, schema) -> schemas.set(name, io.swagger.v3.core.util.Json.mapper().valueToTree(schema)));
    return io.swagger.v3.core.util.Json.mapper().valueToTree(resolved.schema);
  }
}
